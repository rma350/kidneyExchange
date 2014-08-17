package exchangeGraph.minWaitingTime;

import graphUtil.CycleChainDecomposition;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import graphUtil.GraphUtil;
import graphUtil.SubgraphUtil;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;

import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepLib.KepInstance;
import threading.FixedThreadPool;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Table.Cell;
import com.google.common.math.DoubleMath;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CplexUtil;
import exchangeGraph.CycleChainPackingPolytope;
import exchangeGraph.CycleChainPackingSubtourElimination;
import exchangeGraph.DisjointPathsImprovementHeuristic;
import exchangeGraph.KepSolutionPolytope.UserSolution;
import exchangeGraph.NumVariableSet;
import exchangeGraph.SolverOption;
import exchangeGraph.UserCutGenerator;
import exchangeGraph.VariableSet;

public class MinWaitingTimeKepSolver<V, E> {

  private boolean displayOutput;
  private Optional<FixedThreadPool> threadPool;
  private ImmutableSet<SolverOption> solverOptions;
  private double solveTimeSeconds;

  private volatile int numUserCutCallback;
  private volatile int numLazyConstraintCallback;

  private KepInstance<V, E> kepInstance;
  private final Map<V, ? extends Number> nodeArrivalTimes;
  private double terminationTime;
  // private List<EdgeCycle<E>> cycles;
  private IloCplex cplex;
  private CycleChainPackingPolytope<V, E> kepPolytope;

  private ImmutableTable<V, V, IloNumVar> precedenceVariables;
  private NumVariableSet<V> chainTime;// s_v
  private NumVariableSet<V> matchedTime;// t_v
  private NumVariableSet<V> indicatorNotMatched;// 1 - f^i_v - \sum_{C \in
                                                // C_k(v)} z_C

  private CycleChainDecomposition<V, E> cycleChainPackingSolution;
  private Set<V> unmatchedVertices;
  private Map<V, Double> matchingTimes;

  private PreProcessor<V> preProcesser;
  private boolean addConsistencyConstraintsLazily;
  private PrecedenceConsistencyConstraintGenerator<V, E> precedenceConsistencyConstraintGenerator;

  private boolean warmStart;

  public MinWaitingTimeKepSolver(final KepInstance<V, E> kepInstance,
      final Map<V, ? extends Number> nodeArrivalTimes,
      final double terminationTime, boolean displayOutput,
      Optional<Double> maxTimeSeconds, Optional<FixedThreadPool> threadPool,
      ImmutableSet<SolverOption> solverOptions) {

    addConsistencyConstraintsLazily = true;
    warmStart = true;
    if (addConsistencyConstraintsLazily) {
      precedenceConsistencyConstraintGenerator = new PrecedenceConsistencyConstraintGenerator<V, E>(
          kepInstance, nodeArrivalTimes);
    } else {
      precedenceConsistencyConstraintGenerator = null;
    }
    this.kepInstance = kepInstance;
    this.nodeArrivalTimes = nodeArrivalTimes;
    this.threadPool = threadPool;
    this.solverOptions = solverOptions;
    this.terminationTime = terminationTime;
    validateTimes();
    try {
      this.cplex = new IloCplex();
      this.displayOutput = displayOutput;
      if (maxTimeSeconds.isPresent()) {
        cplex.setParam(DoubleParam.TiLim, maxTimeSeconds.get().doubleValue());
      }
      if (!displayOutput) {
        cplex.setOut(null);
        cplex.setWarning(null);
      }
      if (threadPool.isPresent()) {
        cplex.setParam(IntParam.Threads, threadPool.get().getNumThreads());
      }
      // cplex.setParam(IntParam.MIPEmphasis, 1);
      // cycles = CycleGenerator.generateAllCycles(threadPool,
      // kepInstance.getGraph(), kepInstance.getMaxCycleLength(),new
      // ArrayList<V>(kepInstance.getPairedNodes()));
      if (solverOptions.contains(SolverOption.edgeMode)) {
        throw new RuntimeException(
            "Min wiating time solver only works with formlations: cycles, subtour, and cutset, but you selected edges");
      } else {
        kepPolytope = new CycleChainPackingPolytope<V, E>(kepInstance, cplex,
            threadPool, solverOptions);
      }
      preProcesser = new APSPPreProcessor<V, E>(kepInstance, threadPool);
      long variablesEliminatedByPreprocessor = 0;
      ImmutableTable.Builder<V, V, IloNumVar> precedenceVariablesBuilder = ImmutableTable
          .builder();
      for (V first : this.kepInstance.nonTerminalNodes()) {
        for (V second : this.kepInstance.nonRootNodes()) {
          if (first != second) {
            if (preProcesser.createVariable(first, second)) {
              IloIntVar var = cplex.boolVar();
              precedenceVariablesBuilder.put(first, second, var);
              Collection<E> edgesFromFirstToSecond = kepInstance.getGraph()
                  .findEdgeSet(first, second);
              if (!edgesFromFirstToSecond.isEmpty()) {
                // delta_uv >= y_uv
                cplex.addGe(
                    var,
                    kepPolytope.getEdgeVariables().doubleSum(
                        edgesFromFirstToSecond, Functions.constant(1)));
              }
            } else {
              variablesEliminatedByPreprocessor++;
            }
          }
        }

      }
      precedenceVariables = precedenceVariablesBuilder.build();
      System.out.println("number of retained precedence variables: "
          + precedenceVariables.size());
      System.out
          .println("number of precedence variables eliminated by preprocessor: "
              + variablesEliminatedByPreprocessor);

      List<V> verticesInOrder = Lists.newArrayList(kepInstance.getGraph()
          .getVertices());
      if (!addConsistencyConstraintsLazily) {
        this.addAllPrecedenceConsistencyConstraints(verticesInOrder);
      }
      this.chainTime = new NumVariableSet<V>(this.kepInstance.nonRootNodes(),
          cplex);
      for (V vertex : this.kepInstance.nonRootNodes()) {
        // s_v >= f^i_v * a_v
        cplex.addGe(
            chainTime.get(vertex),
            this.kepPolytope.getFlowInterface().flowInDoubleScaled(vertex,
                this.nodeArrivalTimes.get(vertex).doubleValue()));
        for (V u : verticesInOrder) {
          if (precedenceVariables.contains(u, vertex)) {
            double arrivalU = this.nodeArrivalTimes.get(u).doubleValue();
            // s_v >= a_u * delta_{uv}
            cplex.addGe(chainTime.get(vertex),
                cplex.prod(arrivalU, precedenceVariables.get(u, vertex)));
          }
        }
      }
      indicatorNotMatched = new NumVariableSet<V>(
          this.kepInstance.nonRootNodes(), 0, 1, cplex);
      for (V vertex : this.kepInstance.nonRootNodes()) {
        IloLinearNumExpr varUsed = this.kepPolytope.getCycleVariables()
            .doubleSum(
                kepPolytope.getCycleVariables().getNodeToCycles().get(vertex),
                CplexUtil.unity);
        kepPolytope.getFlowInterface()
            .addFlowInDoubleScaled(vertex, varUsed, 1);
        varUsed.addTerm(indicatorNotMatched.get(vertex), 1);
        cplex.addEq(varUsed, 1);
      }
      this.matchedTime = new NumVariableSet<V>(this.kepInstance.nonRootNodes(),
          cplex);
      for (V vertex : this.kepInstance.nonRootNodes()) {
        IloLinearNumExpr sum = this.kepPolytope.getCycleVariables().doubleSum(
            kepPolytope.getCycleVariables().getNodeToCycles().get(vertex),
            cycleArrivalTime);
        sum.addTerm(chainTime.get(vertex), 1);
        sum.addTerm(indicatorNotMatched.get(vertex), this.terminationTime);
        cplex.addGe(this.matchedTime.get(vertex), sum);
      }
      cplex.addMinimize(this.matchedTime.doubleSum(this.matchedTime,
          Functions.constant(1)));
      cplex.use(new KepUserCutCallback());
      cplex.use(new KepLazyConstraintCallback());
      // cplex.use(new SolveKepHeuristic());
      cplex.use(new SimpleRoundingCallback(10));
      if (warmStart) {
        print("Attempting to warm start by solving standard KEP");
        Function<E, Double> maximumWaitingTime = new Function<E, Double>() {
          public Double apply(E edge) {
            return terminationTime
                - nodeArrivalTimes.get(kepInstance.getGraph().getDest(edge))
                    .doubleValue();
          }
        };
        KepInstance<V, E> kepObjMinWait = kepInstance.changeObjectiveFunction(
            maximumWaitingTime, 0);
        CycleChainPackingSubtourElimination<V, E> solver = new CycleChainPackingSubtourElimination<V, E>(
            kepObjMinWait, displayOutput, maxTimeSeconds, threadPool,
            solverOptions);
        solver.solve();
        CycleChainDecomposition<V, E> solution = solver.getSolution();
        UserSolution userSolution = this.createUserSolution(solution);
        cplex
            .addMIPStart(userSolution.getVariables(), userSolution.getValues());
        print("Warm start complete");
      }

    } catch (IloException e) {
      throw new RuntimeException(e);
    }

  }

  private void print(String message) {
    if (displayOutput) {
      System.out.println(message);
    }
  }

  private void addAllPrecedenceConsistencyConstraints(List<V> verticesInOrder)
      throws IloException {
    long startTimeAddPrecConstraints = System.currentTimeMillis();
    int constraintCount = 0;
    /*
     * for(int i = 0; i < verticesInOrder.size(); i++){ V vi =
     * verticesInOrder.get(i); for(int j = 0; j <i; j++){ V vj =
     * verticesInOrder.get(j); if(precedenceVariables.contains(vi, vj) &&
     * precedenceVariables.contains(vj, vi)){ //delta_uv + delta_vu <= 1
     * cplex.addLe(cplex.sum(precedenceVariables.get(vi, vj),
     * precedenceVariables.get(vj, vi)), 1.0); constraintCount++; } } }
     */
    for (V u : verticesInOrder) {
      for (V v : verticesInOrder) {
        if (v != u) {
          for (V w : verticesInOrder) {
            if (w != u && w != v) {
              if (precedenceVariables.contains(u, v)
                  && precedenceVariables.contains(v, w)
                  && precedenceVariables.contains(u, w)) {
                // delta_uv + delta_vw <= delta_uw + 1
                cplex.add(createPrecedenceConsistencyConstraint(u, v, w));
                constraintCount++;
              }
            }
          }
        }
      }
    }
    long elapsedTimePrecConstraint = System.currentTimeMillis()
        - startTimeAddPrecConstraints;
    System.out
        .println("Added " + constraintCount
            + " precedence constraints, elapsed time: "
            + elapsedTimePrecConstraint);
  }

  private Function<EdgeCycle<E>, Double> cycleArrivalTime = new Function<EdgeCycle<E>, Double>() {
    public Double apply(EdgeCycle<E> cycle) {
      double ans = Double.NEGATIVE_INFINITY;
      for (V v : GraphUtil.allContactedVertices(kepInstance.getGraph(),
          cycle.getEdges())) {
        ans = Math.max(ans, nodeArrivalTimes.get(v).doubleValue());
      }
      return ans;
    }
  };

  public void solve() throws IloException {
    long startms = System.currentTimeMillis();
    cplex.solve();
    long endms = System.currentTimeMillis();
    this.solveTimeSeconds = (endms - startms) / 1000.0;
    if (this.displayOutput) {
      System.out.println("objective: " + cplex.getObjValue());
    }
    this.cycleChainPackingSolution = kepPolytope.recoverSolution();
    this.matchingTimes = this.matchedTime.getVariableValues();
    this.unmatchedVertices = this.indicatorNotMatched.getNonZeroVariables();
  }

  public CycleChainDecomposition<V, E> getSolution() {
    return this.cycleChainPackingSolution;
  }

  public Map<V, Double> getMatchingTimes() {
    return this.matchingTimes;
  }

  public Set<V> getUnmatchedVertices() {
    return this.unmatchedVertices;
  }

  /**
   * Requires: solve has already been called and finished successfully.
   * 
   * @return
   */
  public double getSolveTimeSeconds() {
    return this.solveTimeSeconds;
  }

  private class SolveKepHeuristic extends IloCplex.HeuristicCallback implements
      VariableSet.VariableExtractor {

    public SolveKepHeuristic() {
    }

    @Override
    public double[] getValuesVE(IloNumVar[] variables) throws IloException {
      return variables.length == 0 ? new double[0] : this.getValues(variables);
    }

    @Override
    protected void main() throws IloException {
      // System.out.println("attempting heuristic callback");

      Map<E, Double> nonZeroEdgeValues = kepPolytope.getEdgeVariables()
          .getNonZeroVariableValues(this);

      Map<EdgeCycle<E>, Double> nonZeroCycleValues = kepPolytope
          .getCycleVariables().getNonZeroVariableValues(this);
      final Map<E, Double> totalEdgeWeights = Maps
          .newHashMap(nonZeroEdgeValues);
      for (Map.Entry<EdgeCycle<E>, Double> entry : nonZeroCycleValues
          .entrySet()) {
        for (E edge : entry.getKey()) {
          totalEdgeWeights.put(
              edge,
              entry.getValue()
                  + (totalEdgeWeights.containsKey(edge) ? totalEdgeWeights
                      .get(edge) : 0));
        }
      }
      Function<E, Integer> newObj = new Function<E, Integer>() {
        public Integer apply(E edge) {
          if (totalEdgeWeights.containsKey(edge)) {
            return 1 + DoubleMath.roundToInt(100 * totalEdgeWeights.get(edge),
                RoundingMode.HALF_DOWN);
          } else {
            return 1;
          }
        }
      };
      KepInstance<V, E> fractionalObjective = kepInstance
          .changeObjectiveFunction(newObj, 0);
      CycleChainPackingSubtourElimination<V, E> solver = new CycleChainPackingSubtourElimination<V, E>(
          fractionalObjective, displayOutput, Optional.<Double> absent(),
          threadPool, solverOptions);
      solver.solve();
      CycleChainDecomposition<V, E> decomp = solver.getSolution();
      solver.cleanUp();

      validate(decomp);

      UserSolution userSolution = createUserSolution(decomp);
      this.setSolution(userSolution.getVariables(), userSolution.getValues());
    }
  }

  private class SimpleRoundingCallback extends IloCplex.HeuristicCallback
      implements VariableSet.VariableExtractor {

    private int currentAttempt;
    private int maxAttempts;

    public SimpleRoundingCallback(int maxAttempts) {
      this.maxAttempts = maxAttempts;
      this.currentAttempt = 0;
    }

    @Override
    public double[] getValuesVE(IloNumVar[] variables) throws IloException {
      return variables.length == 0 ? new double[0] : this.getValues(variables);
    }

    private UserSolution extractSolution() throws IloException {
      Map<E, Double> nonZeroEdgeValues = kepPolytope.getEdgeVariables()
          .getNonZeroVariableValues(this);

      Map<EdgeCycle<E>, Double> nonZeroCycleValues = kepPolytope
          .getCycleVariables().getNonZeroVariableValues(this);
      DisjointPathsImprovementHeuristic<V, E> disjointPaths = DisjointPathsImprovementHeuristic
          .constructImproveRepeat(kepInstance, nonZeroEdgeValues,
              nonZeroCycleValues, .53, 0.0, threadPool);
      CycleChainDecomposition<V, E> decomp = disjointPaths.getOutSolution();
      // validate(decomp);
      return createUserSolution(decomp);
    }

    @Override
    protected void main() throws IloException {
      if (currentAttempt < maxAttempts) {
        // System.out.println("attempting heuristic callback");

        UserSolution userSolution = extractSolution();
        this.setSolution(userSolution.getVariables(), userSolution.getValues());
        currentAttempt++;

      }
    }
  }

  private void validate(CycleChainDecomposition<V, E> decomp) {
    for (EdgeCycle<E> cycle : decomp.getEdgeCycles()) {
      if (cycle.size() > kepInstance.getMaxCycleLength()) {
        throw new RuntimeException("cycle is too long: " + cycle.toString());
      }
      if (!kepPolytope.getCycleVariables().contains(cycle)) {
        throw new RuntimeException("cycle variable does not exist for cycle: "
            + cycle);
      }
    }
    for (EdgeChain<E> chain : decomp.getEdgeChains()) {
      List<E> edgesInOrder = chain.getEdgesInOrder();
      for (E edge : edgesInOrder) {
        if (!kepPolytope.getEdgeVariables().contains(edge)) {
          throw new RuntimeException("edge var missing for: " + edge);
        }
        if (!precedenceVariables.contains(kepInstance.getGraph()
            .getSource(edge), kepInstance.getGraph().getDest(edge))) {
          throw new RuntimeException("precedence var missing for: " + edge);
        }
      }
    }
  }

  private class KepUserCutCallback extends IloCplex.UserCutCallback implements
      VariableSet.VariableExtractor {

    private volatile long userConsistencyConstraintThreshold = 1;

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
      UserCutGenerator userCutGen = kepPolytope.makeUserCutGenerator(this);
      List<IloRange> violatedConstraints = userCutGen.quickUserCut();
      if (!solverOptions.contains(SolverOption.disableFullUserCut)
          && violatedConstraints.size() == 0 && this.getNnodes64() == 0) {
        violatedConstraints = userCutGen.fullUserCut();
      }
      if (!violatedConstraints.isEmpty()) {
        for (IloRange constraint : violatedConstraints) {
          this.add(constraint);
        }
      } else if (addConsistencyConstraintsLazily) {
        ImmutableTable<V, V, Double> precVarVals = getPrecedenceVariableValues(this);
        for (List<V> constraint : precedenceConsistencyConstraintGenerator
            .checkFractionalSolution(kepPolytope.getEdgeVariables()
                .getNonZeroVariableValues(this), precVarVals)) {
          this.add(createPrecedenceConsistencyConstraint(constraint.get(0),
              constraint.get(1), constraint.get(2)));
        }
        long totalFractionalCuts = precedenceConsistencyConstraintGenerator
            .getFractionalCutsAdded();
        if (totalFractionalCuts > userConsistencyConstraintThreshold) {
          System.out.println("User Consistency Constraints Added: "
              + totalFractionalCuts);
          while (totalFractionalCuts > userConsistencyConstraintThreshold) {
            userConsistencyConstraintThreshold = 2 * userConsistencyConstraintThreshold;
          }
        }
      }
    }
  }

  private class KepLazyConstraintCallback extends
      IloCplex.LazyConstraintCallback implements VariableSet.VariableExtractor {

    private volatile long lazyConsistencyConstraintThreshold = 1;

    public KepLazyConstraintCallback() {
    }

    @Override
    public double[] getValuesVE(IloNumVar[] variables) throws IloException {
      return variables.length == 0 ? new double[0] : this.getValues(variables);
    }

    @Override
    protected void main() throws IloException {
      int time = numLazyConstraintCallback++;
      if (time % 500 == 0 && time != 0) {
        System.err.println("completed about: " + time
            + " lazy cut callbacks.  Integrality gap: "
            + this.getMIPRelativeGap());
      }
      List<IloRange> kepPolytopeLazy = kepPolytope.lazyConstraint(this);
      if (!kepPolytopeLazy.isEmpty()) {
        for (IloRange constraint : kepPolytopeLazy) {
          this.add(constraint);
        }
      } else if (addConsistencyConstraintsLazily) {
        CycleChainDecomposition<V, E> cycleChain = kepPolytope
            .recoverSolution(this);
        ImmutableTable<V, V, Double> precVarVals = getPrecedenceVariableValues(this);
        for (List<V> constraint : precedenceConsistencyConstraintGenerator
            .checkIntegerSolution(cycleChain, precVarVals)) {
          this.add(createPrecedenceConsistencyConstraint(constraint.get(0),
              constraint.get(1), constraint.get(2)));
        }
        long totalIntegerCuts = precedenceConsistencyConstraintGenerator
            .getIntegercutsAdded();
        if (totalIntegerCuts > lazyConsistencyConstraintThreshold) {
          System.out.println("Lazy Consistency Constraints Added: "
              + totalIntegerCuts);
          while (totalIntegerCuts > lazyConsistencyConstraintThreshold) {
            lazyConsistencyConstraintThreshold = 2 * lazyConsistencyConstraintThreshold;
          }
        }

      }

    }
  }

  private ImmutableTable<V, V, Double> getPrecedenceVariableValues(
      VariableSet.VariableExtractor variableExtractor) {
    ImmutableTable.Builder<V, V, Double> ans = ImmutableTable.builder();
    List<Cell<V, V, IloNumVar>> varsAsList = Lists
        .newArrayList(this.precedenceVariables.cellSet());
    IloNumVar[] varArray = new IloNumVar[varsAsList.size()];
    int i = 0;
    for (Cell<V, V, IloNumVar> cell : varsAsList) {
      varArray[i++] = cell.getValue();
    }
    double[] varVals;
    try {
      varVals = variableExtractor.getValuesVE(varArray);
    } catch (IloException e) {
      throw new RuntimeException(e);
    }
    for (int j = 0; j < varsAsList.size(); j++) {
      ans.put(varsAsList.get(j).getRowKey(), varsAsList.get(j).getColumnKey(),
          varVals[j]);
    }
    return ans.build();
  }

  private void printDeltaVars(VariableSet.VariableExtractor variableExtractor) {

    List<Cell<V, V, IloNumVar>> varsAsList = Lists
        .newArrayList(this.precedenceVariables.cellSet());
    IloNumVar[] varArray = new IloNumVar[varsAsList.size()];
    int i = 0;
    for (Cell<V, V, IloNumVar> cell : varsAsList) {
      varArray[i++] = cell.getValue();
    }
    double[] varVals;
    try {
      varVals = variableExtractor.getValuesVE(varArray);
    } catch (IloException e) {
      throw new RuntimeException(e);
    }
    for (int j = 0; j < varsAsList.size(); j++) {
      System.out.println(varsAsList.get(j).getRowKey() + ", "
          + varsAsList.get(j).getColumnKey() + " = " + varVals[j]);
    }
  }

  // creates the constraint delta_uv + delta vw <= 1 + delta uw
  private IloRange createPrecedenceConsistencyConstraint(V u, V v, V w) {
    try {
      return (IloRange) cplex.le(
          cplex.sum(precedenceVariables.get(u, v),
              precedenceVariables.get(v, w)),
          cplex.sum(precedenceVariables.get(u, w), 1.0));
    } catch (IloException e) {
      throw new RuntimeException(e);
    }
  }

  // this requirement is here because our variables chain time and matched
  // time are assumed to be nonnegative.
  private void validateTimes() {
    double maximumArrivalTime = 0;
    for (Map.Entry<V, ? extends Number> arrivalEntry : nodeArrivalTimes
        .entrySet()) {
      if (arrivalEntry.getValue().doubleValue() < 0) {
        throw new RuntimeException(
            "All arrival times must be nonnegative, but found a negative arrival time of "
                + arrivalEntry.getValue() + " for vertex "
                + arrivalEntry.getKey());
      }
      maximumArrivalTime = Math.max(maximumArrivalTime, arrivalEntry.getValue()
          .doubleValue());
    }
    if (terminationTime < maximumArrivalTime) {
      throw new RuntimeException(
          "Termination time must be nonnegative and larger than the maximum node arrival time, but but termination time was "
              + terminationTime);
    }

  }

  public void cleanUp() {
    this.cplex.end();
  }

  public static interface PreProcessor<V> {

    public boolean createVariable(V first, V second);
  }

  private static class PrecedenceConsistencyConstraintGenerator<V, E> {

    private KepInstance<V, E> kepInstance;
    private Set<List<V>> generatedConstraints;
    private volatile long fractionalCutsAdded;
    private volatile long integerCutsAdded;
    private boolean checkChains = true;
    private final Map<V, ? extends Number> nodeArrivalTimes;

    public PrecedenceConsistencyConstraintGenerator(
        KepInstance<V, E> kepInstance, Map<V, ? extends Number> nodeArrivalTimes) {
      this.kepInstance = kepInstance;
      this.nodeArrivalTimes = nodeArrivalTimes;
      this.generatedConstraints = Sets.newHashSet();
      fractionalCutsAdded = 0;
      integerCutsAdded = 0;

    }

    public long getFractionalCutsAdded() {
      return this.fractionalCutsAdded;
    }

    public long getIntegercutsAdded() {
      return this.integerCutsAdded;
    }

    /**
     * 
     * @param nonZeroEdgeValues
     * @return A list of lists of length 3. A returned list of [u,v,w] indicates
     *         that the constraint delta_uv + delta_vw <= delta_uw + 1 should be
     *         added.
     */
    public List<List<V>> checkFractionalSolution(
        Map<E, Double> nonZeroEdgeValues,
        ImmutableTable<V, V, Double> precVarVals) {
      Set<V> retainedVertices = Sets.newHashSet(kepInstance.getGraph()
          .getVertices());
      Set<E> retainedEdges = Maps.filterValues(nonZeroEdgeValues,
          Range.<Double> atLeast(.51)).keySet();
      // In the below graph, each node will have in degree and out degree
      // at most one
      DirectedSparseMultigraph<V, E> inUseEdges = SubgraphUtil.subgraph(
          kepInstance.getGraph(), retainedVertices, retainedEdges);
      CycleChainDecomposition<V, E> decomposition = new CycleChainDecomposition<V, E>(
          inUseEdges);
      List<List<V>> ans = checkCycleChainDecomposition(decomposition,
          precVarVals, checkChains);
      fractionalCutsAdded += ans.size();
      return ans;
    }

    private synchronized List<List<V>> checkCycleChainDecomposition(
        CycleChainDecomposition<V, E> cycleChainDecomposition,
        ImmutableTable<V, V, Double> precVarVals, boolean checkCycles) {
      List<List<V>> violatedConstraints = Lists.newArrayList();
      List<List<V>> notViolatedNotYetGeneratedConstraints = Lists
          .newArrayList();
      for (EdgeChain<E> chain : cycleChainDecomposition.getEdgeChains()) {
        List<V> vertices = GraphUtil.getNodesInOrder(chain,
            kepInstance.getGraph());
        V lastArrival = vertices.get(0);
        double lastArrivalTime = nodeArrivalTimes.get(lastArrival)
            .doubleValue();
        boolean lastArrivalIsPrecedingNode = true;

        for (int i = 1; i < vertices.size(); i++) {
          V w = vertices.get(i);
          double wTime = nodeArrivalTimes.get(w).doubleValue();
          if (wTime < lastArrivalTime) {
            if (!lastArrivalIsPrecedingNode) {
              V v = vertices.get(i - 1);
              List<V> proposedConstraint = Lists
                  .newArrayList(lastArrival, v, w);
              if (!generatedConstraints.contains(proposedConstraint)) {
                boolean constraintIsViolated = precVarVals.get(lastArrival, v)
                    + precVarVals.get(v, w) > precVarVals.get(lastArrival, w)
                    + 1 + CplexUtil.epsilon;
                if (constraintIsViolated) {
                  generatedConstraints.add(proposedConstraint);
                  violatedConstraints.add(proposedConstraint);
                } else {
                  notViolatedNotYetGeneratedConstraints.add(proposedConstraint);
                }
              }
            }
            lastArrivalIsPrecedingNode = false;
          } else {
            lastArrival = w;
            lastArrivalTime = wTime;
            lastArrivalIsPrecedingNode = true;
          }
        }
      }
      if (checkCycles) {
        for (EdgeCycle<E> cycle : cycleChainDecomposition.getEdgeCycles()) {
          if (cycle.size() > this.kepInstance.getMaxCycleLength()
              && cycle.size() > 2) {
            List<V> verticesWrap = GraphUtil.getNodesInOrder(cycle,
                kepInstance.getGraph());
            verticesWrap.add(verticesWrap.get(0));
            verticesWrap.add(verticesWrap.get(1));

            V lastArrival = verticesWrap.get(0);
            double lastArrivalTime = nodeArrivalTimes.get(lastArrival)
                .doubleValue();
            boolean lastArrivalIsPrecedingNode = true;

            for (int i = 1; i < verticesWrap.size(); i++) {
              V w = verticesWrap.get(i);
              if (w == lastArrival) {
                // this will only occur if the first or second node in the loop
                // is the node with the maximum arrival time, and we have gone
                // all the way around the loop.
                break;
              }
              double wTime = nodeArrivalTimes.get(w).doubleValue();
              if (wTime < lastArrivalTime) {
                if (!lastArrivalIsPrecedingNode) {
                  V v = verticesWrap.get(i - 1);
                  List<V> proposedConstraint = Lists.newArrayList(lastArrival,
                      v, w);

                  if (!generatedConstraints.contains(proposedConstraint)) {
                    boolean constraintIsViolated = precVarVals.get(lastArrival,
                        v) + precVarVals.get(v, w) > precVarVals.get(
                        lastArrival, w) + 1 + CplexUtil.epsilon;
                    if (constraintIsViolated) {
                      generatedConstraints.add(proposedConstraint);
                      violatedConstraints.add(proposedConstraint);
                    } else {
                      notViolatedNotYetGeneratedConstraints
                          .add(proposedConstraint);
                    }
                  }
                }
                lastArrivalIsPrecedingNode = false;
              } else {
                lastArrival = w;
                lastArrivalTime = wTime;
                lastArrivalIsPrecedingNode = true;
              }
            }
          }

        }
      }
      // adding a constraint that is not yet violated if there are no violated
      // constraints will result in an error
      if (violatedConstraints.isEmpty()) {
        return violatedConstraints;
      } else {
        List<List<V>> ans = Lists.newArrayList();
        ans.addAll(violatedConstraints);
        for (List<V> notYetViolated : notViolatedNotYetGeneratedConstraints) {
          generatedConstraints.add(notYetViolated);
          ans.add(notYetViolated);
        }
        return ans;
      }

    }

    /**
     * 
     * @param cycleChainDecomposition
     * @return A list of lists of length 3. A returned list of [u,v,w] indicates
     *         that the constraint delta_uv + delta_vw <= delta_uw + 1 should be
     *         added.
     */
    public List<List<V>> checkIntegerSolution(
        CycleChainDecomposition<V, E> cycleChainDecomposition,
        ImmutableTable<V, V, Double> precVarVals) {
      List<List<V>> ans = checkCycleChainDecomposition(cycleChainDecomposition,
          precVarVals, false);
      integerCutsAdded += ans.size();
      return ans;
    }

  }

  private void zeroOutUnassigned(Map<IloNumVar, Double> assignedVarVals,
      VariableSet<?> variableSet) {
    zeroOutUnassigned(assignedVarVals, variableSet.values());
  }

  private void zeroOutUnassigned(Map<IloNumVar, Double> assignedVarVals,
      Iterable<? extends IloNumVar> variables) {
    for (IloNumVar var : variables) {
      if (!assignedVarVals.containsKey(var)) {
        assignedVarVals.put(var, 0.0);
      }
    }
  }

  public UserSolution createUserSolution(
      CycleChainDecomposition<V, E> cycleChainDecomposition) {
    Map<IloNumVar, Double> varVals = Maps.newHashMap();
    for (EdgeChain<E> chain : cycleChainDecomposition.getEdgeChains()) {
      for (E edge : chain) {
        varVals.put(kepPolytope.getEdgeVariables().get(edge), 1.0);
      }
      List<V> verticesInOrder = GraphUtil.getNodesInOrder(chain,
          kepInstance.getGraph());
      for (int i = 0; i < verticesInOrder.size(); i++) {
        for (int j = i + 1; j < verticesInOrder.size(); j++) {
          varVals.put(this.precedenceVariables.get(verticesInOrder.get(i),
              verticesInOrder.get(j)), 1.0);
        }
      }
      double maxArrivalTime = this.nodeArrivalTimes.get(verticesInOrder.get(0))
          .doubleValue();
      for (int i = 1; i < verticesInOrder.size(); i++) {
        V node = verticesInOrder.get(i);
        maxArrivalTime = Math.max(maxArrivalTime,
            this.nodeArrivalTimes.get(node).doubleValue());
        varVals.put(chainTime.get(node), maxArrivalTime);
        varVals.put(matchedTime.get(node), maxArrivalTime);
      }
    }
    zeroOutUnassigned(varVals, kepPolytope.getEdgeVariables());
    zeroOutUnassigned(varVals, chainTime);
    zeroOutUnassigned(varVals, precedenceVariables.values());

    for (EdgeCycle<E> cycle : cycleChainDecomposition.getEdgeCycles()) {
      varVals.put(kepPolytope.getCycleVariables().get(cycle), 1.0);
      List<V> verticesInOrder = GraphUtil.getNodesInOrder(cycle,
          kepInstance.getGraph());
      double arrivalTime = this.cycleArrivalTime.apply(cycle);
      for (V v : verticesInOrder) {
        varVals.put(matchedTime.get(v), arrivalTime);
      }
    }
    zeroOutUnassigned(varVals, kepPolytope.getCycleVariables());
    for (V vertex : matchedTime.keySet()) {
      IloNumVar var = matchedTime.get(vertex);
      if (!varVals.containsKey(var)) {
        varVals.put(var, this.terminationTime);
        varVals.put(indicatorNotMatched.get(vertex), 1.0);
      }
    }
    zeroOutUnassigned(varVals, indicatorNotMatched);
    double objVal = 0;
    for (V v : kepInstance.nonRootNodes()) {
      objVal += varVals.get(matchedTime.get(v));
    }
    System.out.println("Heuristically generated solution objective: " + objVal);

    IloNumVar[] vars = new IloNumVar[varVals.size()];
    double[] vals = new double[varVals.size()];
    int i = 0;
    for (Map.Entry<IloNumVar, Double> entry : varVals.entrySet()) {
      vars[i] = entry.getKey();
      vals[i] = entry.getValue();
      i++;
    }
    return new UserSolution(vars, vals);
  }

}
