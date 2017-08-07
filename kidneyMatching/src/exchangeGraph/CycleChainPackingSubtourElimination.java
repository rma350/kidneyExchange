package exchangeGraph;

import exchangeGraph.KepSolutionPolytope.UserSolution;
import graphUtil.CycleChainDecomposition;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kepLib.KepInstance;
import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class CycleChainPackingSubtourElimination<V, E> extends
    CycleChainPackingCplexSolver<V, E> {

  private Optional<FixedThreadPool> threadPool;

  private KepSolutionPolytope<V, E> polytope;

  private ImmutableSet<SolverOption> solverOptions;

  private volatile int numUserCutCallback = 0;
  // this variable is not synchronized, as a result, the count will only be
  // approximate...
  private volatile int numLazyCallbacks = 0;

  public CycleChainPackingSubtourElimination(KepInstance<V, E> kepInstance,
      boolean displayOutput, Optional<Double> maxTimeSeconds,
      Optional<FixedThreadPool> threadPool) throws IloException {
    this(kepInstance, displayOutput, maxTimeSeconds, threadPool,
        SolverOption.defaultOptions);
  }

  public CycleChainPackingSubtourElimination(KepInstance<V, E> kepInstance,
      boolean displayOutput, Optional<Double> maxTimeSeconds,
      Optional<FixedThreadPool> threadPool,
      ImmutableSet<SolverOption> solverOptions) throws IloException {
    this(kepInstance, displayOutput, maxTimeSeconds, threadPool, solverOptions,
        5);
  }

  public CycleChainPackingSubtourElimination(KepInstance<V, E> kepInstance,
      boolean displayOutput, Optional<Double> maxTimeSeconds,
      Optional<FixedThreadPool> threadPool,
      ImmutableSet<SolverOption> solverOptions, int maxHeuristicAttempts)
      throws IloException {
    super(kepInstance, displayOutput, maxTimeSeconds, !solverOptions
        .contains(SolverOption.silentNoSolutionFound));
    this.threadPool = threadPool;
    this.solverOptions = solverOptions;
    if (solverOptions.contains(SolverOption.edgeMode)) {
      this.polytope = new EdgePolytope<V, E>(kepInstance, cplex, threadPool,
          solverOptions);
    } else {
      this.polytope = new CycleChainPackingPolytope<V, E>(kepInstance, cplex,
          threadPool, solverOptions);
    }
    if (this.solverOptions.contains(SolverOption.lazyConstraintCallback)) {
      cplex.use(new KepLazyConstraintCallback());
    } else {
      System.err
          .println("Warning: No lazy constraint added because lazyConstraintCallback option not selected, solution may be incorrect!");
    }
    if (this.solverOptions.contains(SolverOption.heuristicCallback)
        && maxHeuristicAttempts > 0) {
      if (solverOptions.contains(SolverOption.edgeMode)) {
        System.err
            .println("Warning: primal heuristics not implemented for edge formulation, not adding to formultion.");
      } else {
        cplex.use(new SimpleRoundingCallback(maxHeuristicAttempts));
      }
    }
    // user cut callbacks are not yet implemented for edge mode.
    if (this.solverOptions.contains(SolverOption.userCutCallback)
        && !solverOptions.contains(SolverOption.edgeMode)) {
      cplex.use(new KepUserCutCallback());
    }

    cplex.addMaximize(this.polytope.createObjective());

    // TODO(rander) add a method to extract this value from the polytope, or
    // reorgnize in some way so that this isn't duplicated.
    boolean enforceMaximumChainLength = kepInstance.getMaxChainLength() < kepInstance
        .getPairedNodes().size() + 2
        && !solverOptions.contains(SolverOption.ignoreMaxChainLength);

    Set<E> initEdges = new HashSet<E>();
    Set<EdgeCycle<E>> initCycles = Sets.newHashSet();
    if (enforceMaximumChainLength
        && !solverOptions
            .contains(SolverOption.disableBoundedChainsAdvancedStart)) {
      if (displayOutput) {
        System.out
            .println("Beginning subproblem with unbounded chain length to generate initial solution");
      }
      KepInstance<V, E> subProblemInstance = kepInstance;
      Optional<Double> maxTimeSecondsSubProblem = maxTimeSeconds.isPresent() ? Optional
          .of(maxTimeSeconds.get() * .75) : Optional.<Double> absent();
      EnumSet<SolverOption> subProblemOptionsBuilder = EnumSet
          .copyOf(solverOptions);
      subProblemOptionsBuilder.add(SolverOption.ignoreMaxChainLength);
      ImmutableSet<SolverOption> subProblemOptions = Sets
          .immutableEnumSet(subProblemOptionsBuilder);
      CycleChainPackingSubtourElimination<V, E> subProblemUnboundedChains = new CycleChainPackingSubtourElimination<V, E>(
          subProblemInstance, displayOutput, maxTimeSecondsSubProblem,
          threadPool, subProblemOptions);
      subProblemUnboundedChains.solve();
      if (displayOutput) {
        System.out
            .println("Successfully terminated on subproblem to generate initial solution");
      }
      CycleChainDecomposition<V, E> rawSubSolution = subProblemUnboundedChains
          .getSolution();
      subProblemUnboundedChains.cleanUp();
      initCycles.addAll(rawSubSolution.getEdgeCycles());
      int truncationLoss = 0;
      for (EdgeChain<E> chain : rawSubSolution.getEdgeChains()) {
        if (chain.size() <= kepInstance.getMaxChainLength()) {
          initEdges.addAll(chain.getEdges());
        } else {
          int loss = chain.size() - kepInstance.getMaxChainLength();
          truncationLoss += loss;
          initEdges.addAll(chain.getEdgesInOrder().subList(0,
              kepInstance.getMaxChainLength()));
        }
      }
      if (displayOutput) {
        System.out.println("Truncation loss from sub problem was "
            + truncationLoss + " edges.");
      }

    }
    UserSolution variableValues = this.polytope.createUserSolution(initEdges,
        initCycles);
    cplex
        .addMIPStart(variableValues.getVariables(), variableValues.getValues());

    if (this.threadPool.isPresent()) {
      cplex.setParam(IloCplex.IntParam.Threads, threadPool.get()
          .getNumThreads());
    }
    if (solverOptions.contains(SolverOption.useStrongBranching)) {
      int strongBranching = 3;
      cplex.setParam(IloCplex.IntParam.VarSel, strongBranching);
    }
  }

  @Override
  protected CycleChainDecomposition<V, E> recoverSolution() {
    try {
      if (!cplex.getStatus().equals(Status.Optimal)) {
        System.err.println("Warning, IP was not solved, but status was: "
            + cplex.getStatus());
      }
      return this.polytope.recoverSolution();
    } catch (IloException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean debugFractional = true;
  private static String logDir = "log" + File.separator;

  private void writeCurrentSolution(String shortFileName,
      IntVariableSet.VariableExtractor extractor) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(logDir
          + shortFileName));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }/*
      * catch (IloException e) { throw new RuntimeException(e); }
      */
  }

  private class KepUserCutCallback extends IloCplex.UserCutCallback implements
      VariableSet.VariableExtractor {

    public KepUserCutCallback() {
    }

    @Override
    public double[] getValuesVE(IloNumVar[] variables) throws IloException {
      return variables.length == 0 ? new double[0] : this.getValues(variables);
    }

    @Override
    protected void main() throws IloException {
      if (!this.isAfterCutLoop()) {
        return;
      }
      int time = numUserCutCallback++;
      if (time % 500 == 0 && time != 0) {
        System.err.println("completed about: " + time
            + " user cut callbacks.  MIP gap: " + this.getMIPRelativeGap());
      }
      UserCutGenerator userCutGen = polytope.makeUserCutGenerator(this);
      List<IloRange> violatedConstraints = userCutGen.quickUserCut();
      if (!solverOptions.contains(SolverOption.disableFullUserCut)
          && violatedConstraints.size() == 0 && this.getNnodes64() == 0) {
        violatedConstraints = userCutGen.fullUserCut();
      }
      for (IloRange constraint : violatedConstraints) {
        this.addLocal(constraint);
      }
    }
  }

  private class KepLazyConstraintCallback extends
      IloCplex.LazyConstraintCallback implements VariableSet.VariableExtractor {

    public KepLazyConstraintCallback() {
    }

    @Override
    public double[] getValuesVE(IloNumVar[] variables) throws IloException {
      return variables.length == 0 ? new double[0] : this.getValues(variables);
    }

    @Override
    protected void main() throws IloException {
      int time = numLazyCallbacks++;
      if (time % 500 == 0 && time != 0) {
        System.err.println("completed about: " + time
            + " lazy cut callbacks.  Integrality gap: "
            + this.getMIPRelativeGap());
      }
      for (IloRange constraint : polytope.lazyConstraint(this)) {
        this.add(constraint);
      }
    }
  }

  private class SimpleRoundingCallback extends IloCplex.HeuristicCallback
      implements VariableSet.VariableExtractor {

    private final int maxAttempts;
    private int currentAttempts;

    public SimpleRoundingCallback(int maxAttempts) {
      this.maxAttempts = maxAttempts;
      currentAttempts = 0;
    }

    @Override
    public double[] getValuesVE(IloNumVar[] variables) throws IloException {
      return variables.length == 0 ? new double[0] : this.getValues(variables);
    }

    @Override
    protected void main() throws IloException {
      if (currentAttempts < maxAttempts) {
        // System.out.println("attempting heuristic callback");
        UserSolution userSolution = polytope.roundFractionalSolution(this);
        this.setSolution(userSolution.getVariables(), userSolution.getValues());
        currentAttempts++;
      }
    }
  }

  @Override
  protected void relaxAllIntegerVariables() throws IloException {
    polytope.relaxAllIntegerVariables();
  }

  @Override
  protected void restateAllIntegerVariables() throws IloException {
    polytope.restateAllIntegerVariables();
  }

  /**
   * Warning: your code should not depend on this, it may be eliminated in
   * future releases!!! Do not consider it part of the public API, it is here
   * for debugging purposes.
   * 
   * @return
   */
  public KepSolutionPolytope<V, E> getPolytope() {
    return polytope;
  }

  /*
   * public int getNumCycles(){ return polytope.getCycleVariables().size(); }
   */

  public int getNumLazyCallbacks() {
    return this.numLazyCallbacks;
  }

  /*
   * private class ImprovedBranchingCallback extends IloCplex.BranchCallback{
   * 
   * @Override protected void main() throws IloException { int numBranches =
   * this.getNbranches(); if(numBranches == 0){ return; }
   * if(!createEdgeVariables){ return; } else{ IloNumVar[][] branchedVariables =
   * new IloNumVar[numBranches][]; double[][] bounds = new
   * double[numBranches][]; IloCplex.BranchDirection[][] direction = new
   * IloCplex.BranchDirection[numBranches][]; double[]
   * cplexBranchingObjetiveGuess = this.getBranches(branchedVariables, bounds,
   * direction); boolean hitCycleVar = false; for(int i = 0; i < numBranches;
   * i++){ for(int v = 0; v < branchedVariables[i].length; v++){
   * 
   * if(cycleVariables.getC.inverse().containsKey(branchedVariables[i][v])){
   * hitCycleVar = true; break; } } } if(!hitCycleVar){ return; } else{ for(int
   * i = 0; i < numBranches; i++){ List<IloRange> constraints = new
   * ArrayList<IloRange>(); for(int v = 0; v < branchedVariables[i].length;
   * v++){ IloNumVar branchVar = branchedVariables[i][v]; if(direction[i][v] ==
   * IloCplex.BranchDirection.Up){ constraints.add(cplex.le(branchVar,
   * bounds[i][v]));
   * if(cycleVariables.getCycleVars().inverse().containsKey(branchVar)){
   * constraints.add(makeConstraintForCycle(
   * cycleVariables.getCycleVars().inverse().get(branchVar),
   * SubtourMode.CYCLE_CONSTRAINT)); } } else if(direction[i][v] ==
   * IloCplex.BranchDirection.Down){
   * constraints.add(cplex.ge(branchedVariables[i][v], bounds[i][v])); } else{
   * throw new RuntimeException("Unexpected Branch direction: " +
   * direction[i][v]); } } this.makeBranch(constraints.toArray(new
   * IloRange[constraints.size()]),cplexBranchingObjetiveGuess[i] ); } } }
   * 
   * 
   * }
   * 
   * }
   */

}
