package exchangeGraph.stochasticOpt;

import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CplexUtil;
import exchangeGraph.CycleChainPackingPolytope;
import exchangeGraph.CycleChainPackingSubtourElimination;
import exchangeGraph.KepPolytope;
import exchangeGraph.SolverOption;
import exchangeGraph.UserCutGenerator;
import graphUtil.CycleChainDecomposition;
import graphUtil.CycleGenerator;
import graphUtil.EdgeCycle;
import graphUtil.GraphUtil;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Lists;

import exchangeGraph.VariableSet;
import exchangeGraph.IntVariableSet;

import kepLib.KepInstance;
import kepLib.KepInstance.EdgeConstraint;

public class TwoStageEdgeFailureSolver<V,E> {
	
	private KepInstance<V,E> kepInstance;
	private List<EdgeFailureScenario<V,E>> edgeFailureScenarios;
	private List<EdgeCycle<E>> cycles;
	private IloCplex cplex;
	private KepPolytope<V,E> phaseOneProblem;
	
	
	private List<CycleChainPackingPolytope<V,E>> phaseTwoProblems;
	
	private int numUserCutCallback = 0;
	private int numLazyConstraintCallback = 0;
	
	private double solveTimeSeconds;
	private boolean displayOutput;
	private ImmutableSet<E> edgesInSolution;
	
	public static <V,E> TwoStageEdgeFailureSolver<V,E> truncationSolver(KepInstance<V,E> kepInstance, 
			List<EdgeFailureScenario<V,E>> edgeFailureScenarios, Optional<FixedThreadPool> threadPool, 
			boolean displayOutput, Optional<Double> maxTimeSeconds, ImmutableSet<SolverOption> solverOptions) throws IloException{
		int maxCycleLengthToCreateVariables = kepInstance.getMaxCycleLength();
		IloCplex cplex = new IloCplex();
		if(maxTimeSeconds.isPresent()){
			cplex.setParam(DoubleParam.TiLim, maxTimeSeconds.get().doubleValue());
		}
		if(threadPool.isPresent()){
			cplex.setParam(IntParam.Threads, threadPool.get().getNumThreads());
		}
		List<EdgeCycle<E>> cycles = CycleGenerator.generateAllCycles(threadPool, 
				kepInstance.getGraph(), maxCycleLengthToCreateVariables,new ArrayList<V>(kepInstance.getPairedNodes()));
		CycleChainPackingPolytope<V,E> phaseOneProblem = new CycleChainPackingPolytope<V,E>(kepInstance, cycles, cplex, threadPool,solverOptions);
		return new TwoStageEdgeFailureSolver<V,E>(kepInstance,edgeFailureScenarios,cplex,
				phaseOneProblem,cycles,threadPool,displayOutput,
				SolverOption.phaseTwoTrunctationOptions(solverOptions));
	}
	
	public TwoStageEdgeFailureSolver(KepInstance<V,E> kepInstance,List<EdgeFailureScenario<V,E>> edgeFailureScenarios, IloCplex cplex,
			KepPolytope<V,E> phaseOneProblem, List<EdgeCycle<E>> cycles, 
			Optional<FixedThreadPool> threadPool, boolean displayOutput, ImmutableSet<SolverOption> phaseTwoSolverOptions) throws IloException{
		this.displayOutput = displayOutput;
		this.kepInstance = kepInstance;
		this.edgeFailureScenarios = edgeFailureScenarios;
		this.phaseOneProblem = phaseOneProblem;
		this.cplex = cplex;
		if(!displayOutput){
			cplex.setOut(null);
			cplex.setWarning(null);
		}
		this.cycles = cycles;
		this.phaseTwoProblems = Lists.newArrayList();
		IloLinearNumExpr obj = cplex.linearNumExpr();
		for(int i = 0 ; i < edgeFailureScenarios.size(); i++){
			
			CycleChainPackingPolytope<V,E> phaseTwoProb = new CycleChainPackingPolytope<V,E>(kepInstance,
					cycles, cplex, threadPool, phaseTwoSolverOptions);
			phaseTwoProblems.add(phaseTwoProb);
			for(E edge: edgeFailureScenarios.get(i).getFailedEdges()){
				cplex.addEq(phaseTwoProb.indicatorEdgeSelected(edge), 0);
			}
			for(E edge: kepInstance.getGraph().getEdges()){
				cplex.addLe(phaseTwoProb.indicatorEdgeSelected(edge), phaseOneProblem.indicatorEdgeSelected(edge));
			}
			IloLinearNumExpr objTerm = phaseTwoProb.createObjective();
			CplexUtil.rescale(objTerm,1.0/edgeFailureScenarios.size());
			obj.add(objTerm);
		}
		cplex.addMaximize(obj);
		cplex.use(new TwoStageLazyConstraintCallback());
		cplex.use(new TwoStageUserCutCallback());		
	}
	
	public void solve() throws IloException{
		long startms = System.currentTimeMillis();
		cplex.solve();
		long endms = System.currentTimeMillis();
		this.solveTimeSeconds = (endms - startms)/1000.0;
		if(this.displayOutput){
			System.out.println("objective: " + cplex.getObjValue());
		}
		Builder<E> setBuilder = ImmutableSet.builder();
		for(E edge: kepInstance.getGraph().getEdges()){
			if(CplexUtil.doubleToBoolean(cplex.getValue(this.phaseOneProblem.indicatorEdgeSelected(edge)))){
				setBuilder.add(edge);
			}
		}
		this.edgesInSolution = setBuilder.build();
	}
	
	/**
	 * Requires: solve has already been called and finished successfully.
	 * @return
	 */
	public double getSolveTimeSeconds(){
		return this.solveTimeSeconds;
	}
	
	public ImmutableSet<E> getEdgesInSolution(){
		return this.edgesInSolution;
	}
	
	public CycleChainDecomposition<V,E> applySolutionToRealization(EdgeFailureScenario<V,E> scenario, boolean displayOutput,
			Optional<Double> maxTimeSeconds, Optional<FixedThreadPool> threadPool, ImmutableSet<SolverOption> solverOptions) throws IloException{
		Set<E> availableEdges = Sets.difference(edgesInSolution, scenario.getFailedEdges());
		KepInstance<V,E> restricted = this.kepInstance.createRestrictedProblem(availableEdges);
		CycleChainPackingSubtourElimination<V,E> solver = new CycleChainPackingSubtourElimination<V,E>(restricted,
				displayOutput,maxTimeSeconds,threadPool,solverOptions);
		solver.solve();
		return solver.getSolution();
	}
	
	
	public void cleanUp(){
		cplex.end();
	}
	
	
	private class TwoStageUserCutCallback extends IloCplex.UserCutCallback implements VariableSet.VariableExtractor{
		
		
		public TwoStageUserCutCallback(){}
		
		@Override
		public double[] getValuesVE(IloNumVar[] variables) throws IloException {
			return variables.length == 0 ? new double[0] : this.getValues(variables);
		}
		
		@Override
		protected void main() throws IloException {
			if(!this.isAfterCutLoop()){
				return;
			}
			int time = numUserCutCallback++;
			if(time %500 == 0 && time != 0){
				System.err.println("completed about: " + time + " user cut callbacks.  Integrality gap: " + this.getMIPRelativeGap());
			}
			List<UserCutGenerator> userCutGens = Lists.newArrayList();
			userCutGens.add(phaseOneProblem.makeUserCutGenerator(this));
			for(CycleChainPackingPolytope<V,E> phaseTwo : phaseTwoProblems){
				userCutGens.add(phaseTwo.makeUserCutGenerator(this));
			}
			List<IloRange> violatedConstraints = Lists.newArrayList();
			for(UserCutGenerator userCutGen: userCutGens){
				violatedConstraints.addAll(userCutGen.quickUserCut());
			}
			if(violatedConstraints.size() == 0 && this.getNnodes64() ==0){
				for(UserCutGenerator userCutGen: userCutGens){
					violatedConstraints.addAll(userCutGen.fullUserCut());
				}
			}
			for(IloRange constraint: violatedConstraints){
				this.add(constraint);
			}
		}		
	}
	
	private class TwoStageLazyConstraintCallback extends IloCplex.LazyConstraintCallback implements VariableSet.VariableExtractor{
		
		
		public TwoStageLazyConstraintCallback(){}
		
		@Override
		public double[] getValuesVE(IloNumVar[] variables) throws IloException {
			return variables.length == 0 ? new double[0] : this.getValues(variables);
		}
		
		@Override
		protected void main() throws IloException {
			int time = numLazyConstraintCallback++;
			if(time %500 == 0 && time != 0){
				System.err.println("completed about: " + time + " lazy cut callbacks.  Integrality gap: " + this.getMIPRelativeGap());
			}
			
			List<IloRange> violatedConstraints = Lists.newArrayList();
			violatedConstraints.addAll(phaseOneProblem.lazyConstraint(this));
			for(CycleChainPackingPolytope<V,E> phaseTwoProbem: phaseTwoProblems){
				violatedConstraints.addAll(phaseTwoProbem.lazyConstraint(this));
			}
			for(IloRange constraint: violatedConstraints){
				this.add(constraint);
			}
		}		
	}
	
	 

}
