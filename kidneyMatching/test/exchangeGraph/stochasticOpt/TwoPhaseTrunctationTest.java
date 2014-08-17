package exchangeGraph.stochasticOpt;

import static org.junit.Assert.*;

import exchangeGraph.SolverOption;
import graphUtil.CycleChainDecomposition;
import graphUtil.Edge;
import graphUtil.Node;

import ilog.concert.IloException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import kepLib.KepInstance;
import kepLib.KepTextReaderWriter;
import kepLib.KepParseData;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import threading.FixedThreadPool;

@RunWith(Theories.class)
public class TwoPhaseTrunctationTest {

	public static final String unitTestDir = "unitTestData"+File.separator + "twoStageTruncationIn" + File.separator;
	public static final String inputKepBase = "e";
	public static final String inputFailuresBase = "failures";
	public static final String phaseOneOutBase= "phaseOneSol";
	public static final String realizationBase = "realization";
	public static final String realizedSolutionBase = "realizedSol";
	public static final String suffix = ".csv";
	
	@AfterClass
	public static void cleanUp(){
		FixedThreadPool.shutDown(singleThreaded);
		FixedThreadPool.shutDown(multiThreaded);
	}
	
	public static final int numTests = 5;
	
	public static int[] oneToN(int n){
		int[] ans = new int[n];
		for(int j = 0; j < n; j++){
			ans[j] = j+1;
		}
		return ans;
	}
	
	@DataPoints
	public static int[] tests = oneToN(numTests);
	
	@DataPoint
	public static Optional<FixedThreadPool> singleThreaded = FixedThreadPool.makePool(1);
	
	@DataPoint
	public static Optional<FixedThreadPool> multiThreaded = FixedThreadPool.makePool(2);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o1 = SolverOption.makeCheckedOptions(SolverOption.cutsetMode,
			SolverOption.expandedFormulation,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o2 = SolverOption.makeCheckedOptions(SolverOption.cycleMode,
			SolverOption.expandedFormulation,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o3 = SolverOption.makeCheckedOptions(SolverOption.subsetMode,
			SolverOption.expandedFormulation,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o4 = SolverOption.makeCheckedOptions(SolverOption.cutsetMode,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o5 = SolverOption.makeCheckedOptions(SolverOption.cycleMode,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o6 = SolverOption.makeCheckedOptions(SolverOption.subsetMode,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	
	
	public static class TruncationInputOutput{
		private KepInstance<Node,Edge> kepInstance;
		private List<EdgeFailureScenario<Node,Edge>> failureScenarios;
		private ImmutableSet<Edge> phaseOneSolution;
		private EdgeFailureScenario<Node,Edge> realization;
		private CycleChainDecomposition<Node,Edge> realizedSolution;
		public TruncationInputOutput(KepInstance<Node, Edge> kepInstance,
				List<EdgeFailureScenario<Node, Edge>> failureScenarios,
				ImmutableSet<Edge> phaseOneSolution, EdgeFailureScenario<Node,Edge> realization,
				CycleChainDecomposition<Node,Edge> realizedSolution) {
			super();
			this.kepInstance = kepInstance;
			this.failureScenarios = failureScenarios;
			this.phaseOneSolution = phaseOneSolution;
			this.realization = realization;
			this.realizedSolution = realizedSolution;
		}
		public KepInstance<Node, Edge> getKepInstance() {
			return kepInstance;
		}
		public List<EdgeFailureScenario<Node, Edge>> getFailureScenarios() {
			return failureScenarios;
		}
		public ImmutableSet<Edge> getPhaseOneSolution() {
			return phaseOneSolution;
		}
		public EdgeFailureScenario<Node, Edge> getRealization() {
			return realization;
		}
		public CycleChainDecomposition<Node, Edge> getRealizedSolution() {
			return realizedSolution;
		}
	}
	
	public static TruncationInputOutput makeTruncationInputOutput(int trialNum) throws IOException{
		if(trialNum < 1 || trialNum > numTests){
			throw new RuntimeException("Trial number must lie in [1," + trialNum+ "] but was " + trialNum);
		}
		String kepInName = unitTestDir + inputKepBase + trialNum + suffix;
		KepParseData<Node,Edge> parseData = KepTextReaderWriter.INSTANCE.readParseData(kepInName);
		String scenarioName = unitTestDir + inputFailuresBase+ trialNum + suffix;
		List<EdgeFailureScenario<Node,Edge>> scenarios =  KepTextReaderWriter.INSTANCE.readEdgeFailureScenarios(scenarioName, parseData);
		String phaseOneName = unitTestDir + phaseOneOutBase+ trialNum + suffix;
		ImmutableSet<Edge> phaseOneSol = KepTextReaderWriter.INSTANCE.readPhaseOneSolution(phaseOneName, parseData);
		String realizationName = unitTestDir + realizationBase+ trialNum + suffix;
		EdgeFailureScenario<Node,Edge> realization = KepTextReaderWriter.INSTANCE.readEdgeFailureScenarios(realizationName, parseData).get(0);
		String realizedSolutionName = unitTestDir + realizedSolutionBase+ trialNum + suffix;
		CycleChainDecomposition<Node,Edge> solution = KepTextReaderWriter.INSTANCE.readSolution(parseData, realizedSolutionName);
		return new TruncationInputOutput(parseData.getInstance(),scenarios,phaseOneSol,realization,solution);		
	}
	
	@Theory
	public void testCutsetSingleThreaded(int trial, Optional<FixedThreadPool> threadPool, 
			ImmutableSet<SolverOption> solverOptions) throws IOException, IloException{
		String identification ="Trial " + trial + ", Threads " + (threadPool.isPresent() ? threadPool.get().getNumThreads() : 1)  + ", Solver Options:" + solverOptions; 
		System.out.println(identification);
		TruncationInputOutput inOut = makeTruncationInputOutput(trial);
		TwoStageEdgeFailureSolver<Node,Edge> solver = TwoStageEdgeFailureSolver.truncationSolver(inOut.getKepInstance(), 
				inOut.getFailureScenarios(), threadPool, false, Optional.<Double>absent(), solverOptions);
		solver.solve();
		assertEquals(inOut.getPhaseOneSolution(),solver.getEdgesInSolution());
		CycleChainDecomposition<Node,Edge> realizedSol = solver.applySolutionToRealization(inOut.getRealization(), false, Optional.<Double>absent(), threadPool, solverOptions);
		assertEquals(inOut.getRealizedSolution(),realizedSol);
		
	}

}
