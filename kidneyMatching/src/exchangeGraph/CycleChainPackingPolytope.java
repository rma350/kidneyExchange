package exchangeGraph;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CutsetSeparator.Cutset;
import exchangeGraph.CycleSeparator.CycleCut;
import exchangeGraph.SubtourSeparator.Subtour;
import exchangeGraph.VariableSet.VariableExtractor;
import graphUtil.CycleChainDecomposition;
import graphUtil.CycleGenerator;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepLib.KepInstance;
import kepLib.KepInstance.BridgeConstraint;
import kepLib.KepInstance.EdgeConstraint;
import kepLib.KepInstance.NodeFlowInConstraint;
import kepLib.KepInstance.NodeFlowOutConstraint;
import threading.FixedThreadPool;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;

public class CycleChainPackingPolytope<V, E> implements
    KepSolutionPolytope<V, E> {

  private static double defaultMinUserConstraintViolation = .05;

  private KepInstance<V, E> kepInstance;
  private IloCplex cplex;
  private ImmutableSet<SolverOption> solverOptions;

  private Optional<FixedThreadPool> threadPool;

  private CycleVariables<V, E> cycleVariables;
  private DirectedEdgeVariables<V, E> edgeVariables;
  private FlowInterface<V, E> flowInterface;
  private IntegerFlowNetwork<V, E> flowNetwork;

  private boolean enforceMaximumChainLength;

  public CycleChainPackingPolytope(KepInstance<V, E> kepInstance,
      IloCplex cplex, Optional<FixedThreadPool> threadPool,
      ImmutableSet<SolverOption> solverOptions) throws IloException {
    this(kepInstance, CycleGenerator.generateAllCycles(threadPool,
        kepInstance.getGraph(), kepInstance.getMaxCycleLength(),
        Lists.newArrayList(kepInstance.getPairedNodes())), cplex, threadPool,
        solverOptions);
  }

  public CycleChainPackingPolytope(KepInstance<V, E> kepInstance,
      List<EdgeCycle<E>> cycles, IloCplex cplex,
      Optional<FixedThreadPool> threadPool,
      ImmutableSet<SolverOption> solverOptions) throws IloException {
    this.kepInstance = kepInstance;
    this.threadPool = threadPool;
    this.cplex = cplex;
    this.enforceMaximumChainLength = kepInstance.getMaxChainLength() < kepInstance
        .getPairedNodes().size() + 2
        && !solverOptions.contains(SolverOption.ignoreMaxChainLength);
    this.solverOptions = solverOptions;
    this.cycleVariables = new CycleVariables<V, E>(kepInstance.getGraph(),
        cycles, cplex);
    this.flowNetwork = new IntegerFlowNetwork<V, E>(kepInstance.getGraph(),
        kepInstance.getRootNodes(), kepInstance.getPairedNodes(),
        kepInstance.getTerminalNodes(), cplex,
        solverOptions.contains(SolverOption.expandedFormulation));
    this.edgeVariables = flowNetwork.getEdgeVariables();
    this.flowInterface = flowNetwork.getFlowInterface();
    /*
     * for(EdgeCycle<E> cycle: cycles){ for(IloRange cycleConstraint:
     * this.makeConstraintForCycle(cycle)){ cplex.add(cycleConstraint); } }
     */
    for (V vertex : kepInstance.getPairedNodes()) {
      IloLinearIntExpr nodeUsage = cycleVariables.integerSum(cycleVariables
          .getNodeToCycles().get(vertex));
      flowInterface.addFlowInIntScaled(vertex, nodeUsage, 1);
      cplex.addLe(nodeUsage, 1);
    }
    if (enforceMaximumChainLength) {
      MaxChainLength.constrainMaxLength(kepInstance, cplex, solverOptions,
          edgeVariables, true);
    }
    addAuxiliaryConstraints();
  }

  public IloLinearNumExpr createObjective() throws IloException {
    double cycleBonus = kepInstance.getCycleBonus();
    IloLinearNumExpr obj = this.edgeVariables.doubleSum(edgeVariables,
        kepInstance.getEdgeWeights());
    // int negativeCount = 0;gm
    // int nonNeg = 0;
    // for (E edge : this.getEdgeVariables()) {
    // if (kepInstance.getEdgeWeights().apply(edge).doubleValue() < 0) {
    // negativeCount++;
    // } else {
    // nonNeg++;
    // }
    // }
    // System.out.println("negative count: " + negativeCount
    // + ", positive count: " + nonNeg);
    Function<EdgeCycle<E>, Double> cycleWeights = kepInstance.makeCycleWeight(
        kepInstance.getEdgeWeights(), 1 + cycleBonus);
    obj.add(this.cycleVariables.doubleSum(cycleVariables, cycleWeights));
    return obj;
  }

  private IloRange makeCutsetConstraint(Cutset<V, E> cutset)
      throws IloException {
    /*
     * IloLinearIntExpr cut = edgeVariables.integerSum(cutset.getCutEdges());
     * this.flowInterface.addFlowInIntScaled(cutset.getRhs(), cut, -1); return
     * cplex.ge(cut, 0);
     */

    IloLinearIntExpr flowInRhs = cplex.linearIntExpr();
    this.flowInterface.addFlowInIntScaled(cutset.getRhs(), flowInRhs, 1);
    return (IloRange) cplex.ge(edgeVariables.integerSum(cutset.getCutEdges()),
        flowInRhs);
  }

  private IloRange makeSubtourConstraint(Subtour<V, E> subtour)
      throws IloException {
    return cplex.le(edgeVariables.integerSum(subtour.getInternalEdges()),
        subtour.getNodes().size() - 1);
  }

  private IloRange makeCycleConstraint(CycleCut<E> cycleCut)
      throws IloException {
    DirectedSparseMultigraph<V, E> graph = kepInstance.getGraph();
    Set<E> constraintEdges = new HashSet<E>();
    for (E edge : cycleCut.getCycle().getEdgesInOrder()) {
      constraintEdges.addAll(graph.findEdgeSet(graph.getSource(edge),
          graph.getDest(edge)));
    }
    return cplex.le(edgeVariables.integerSum(constraintEdges), cycleCut
        .getCycle().size() - 1);
  }

  /*
   * private List<IloRange> makeConstraintForCycle(EdgeCycle<E> cycle) throws
   * IloException{ ImmutableSet<V> nodes =
   * ImmutableSet.copyOf(GraphUtil.getNodesInOrder
   * (cycle,kepInstance.getGraph())); ImmutableSet<E> cutEdges =
   * GraphUtil.edgesIntoCut(kepInstance.getGraph(),nodes); List<IloRange> ans =
   * new ArrayList<IloRange>(); for(V node: nodes){
   * ans.add(makeCutsetConstraint(new Cutset<V,E>(cutEdges,nodes,node,1))); }
   * return ans; }
   */

  private void addAuxiliaryConstraints() throws IloException {
    for (NodeFlowInConstraint<V> flowInConstraint : kepInstance
        .getNodeFlowInConstraints()) {
      IloLinearNumExpr sum = cycleVariables.doubleSum(cycleVariables
          .getCyclesTouchingAllVertices(flowInConstraint.getNodes()),
          kepInstance.makeCycleWeight(flowInConstraint.getCoefficients()));
      for (V vertex : flowInConstraint.getNodes()) {
        flowInterface.addFlowInDoubleScaled(vertex, sum, flowInConstraint
            .getCoefficients().apply(vertex).doubleValue());
      }
      CplexUtil.addConstraint(sum, flowInConstraint.getRelationType(),
          flowInConstraint.getRhs(), cplex);
    }
    for (NodeFlowOutConstraint<V> flowOutConstraint : kepInstance
        .getNodeFlowOutConstraints()) {
      IloLinearNumExpr sum = cycleVariables.doubleSum(cycleVariables
          .getCyclesTouchingAllVertices(flowOutConstraint.getNodes()),
          kepInstance.makeCycleWeight(flowOutConstraint.getCoefficients()));

      for (V vertex : flowOutConstraint.getNodes()) {
        flowInterface.addFlowOutDoubleScaled(vertex, sum, flowOutConstraint
            .getCoefficients().apply(vertex).doubleValue());
      }

      CplexUtil.addConstraint(sum, flowOutConstraint.getRelationType(),
          flowOutConstraint.getRhs(), cplex);
    }

    for (BridgeConstraint<V> bridgeConstraint : kepInstance
        .getBridgeConstraints()) {
      IloLinearNumExpr sum = cplex.linearNumExpr();
      for (V node : bridgeConstraint.getNodes()) {
        double coef = bridgeConstraint.getCoefficients().apply(node)
            .doubleValue();
        if (kepInstance.getRootNodes().contains(node)) {
          sum.setConstant(sum.getConstant() + coef);
        } else {
          flowInterface.addFlowInDoubleScaled(node, sum, coef);
        }
        if (kepInstance.nonTerminalNodes().contains(node)) {
          flowInterface.addFlowOutDoubleScaled(node, sum, -coef);
        }

      }
      CplexUtil.addConstraint(sum, bridgeConstraint.getRelationType(),
          bridgeConstraint.getRhs(), cplex);
    }

    for (EdgeConstraint<E> edgeConstraint : kepInstance
        .getEdgeUsageConstraints()) {
      IloLinearNumExpr sum = cycleVariables
          .doubleSum(cycleVariables.getCyclesContainingAllEdges(edgeConstraint
              .getEdges()), kepInstance.makeCycleWeight(
              edgeConstraint.getCoefficients(), 1));

      sum.add(edgeVariables.doubleSum(edgeConstraint.getEdges(),
          edgeConstraint.getCoefficients()));

      CplexUtil.addConstraint(sum, edgeConstraint.getRelationType(),
          edgeConstraint.getRhs(), cplex);
    }
  }

  @Override
  public IloLinearIntExpr indicatorEdgeSelected(E edge) throws IloException {
    IloLinearIntExpr ans = cycleVariables.integerSum(cycleVariables
        .getEdgeToCycles().get(edge));
    ans.addTerm(edgeVariables.get(edge), 1);
    return ans;
  }

  private UserCutGenerator makeCutGenerator(VariableExtractor extractor,
      double minCutViolation) throws IloException {
    if (this.solverOptions.contains(SolverOption.cycleMode)) {
      return new CycleCutGenerator(extractor, minCutViolation);
    } else if (this.solverOptions.contains(SolverOption.subsetMode)) {
      return new SubtourCutGenerator(extractor, minCutViolation);
    } else if (this.solverOptions.contains(SolverOption.cutsetMode)) {
      return new CutsetCutGenerator(extractor, minCutViolation);
    } else {
      throw new RuntimeException(
          "Solver option for cut generation type not set");
    }
  }

  @Override
  public List<IloRange> lazyConstraint(VariableExtractor variableExtractor)
      throws IloException {
    if (this.solverOptions.contains(SolverOption.lazyConstraintCallback)) {
      return makeCutGenerator(variableExtractor, CplexUtil.epsilon)
          .quickUserCut();
    } else {
      return Lists.newArrayList();
    }

  }

  @Override
  public UserCutGenerator makeUserCutGenerator(
      VariableExtractor variableExtractor) throws IloException {
    if (this.solverOptions.contains(SolverOption.userCutCallback)) {
      return this.makeCutGenerator(variableExtractor,
          defaultMinUserConstraintViolation);
    } else {
      return NoUserCuts.INSTANCE;
    }
  }

  private class CycleCutGenerator implements UserCutGenerator {
    private Map<E, Double> nonZeroEdgeWeights;
    private CycleSeparator<V, E> sep;
    private double minimumViolationForCut;

    public CycleCutGenerator(VariableExtractor variableExtractor,
        double minimumViolationForCut) throws IloException {
      this.minimumViolationForCut = minimumViolationForCut;
      nonZeroEdgeWeights = edgeVariables
          .getNonZeroVariableValues(variableExtractor);
      sep = new CycleSeparator<V, E>(kepInstance.getGraph(),
          kepInstance.getRootNodes(), nonZeroEdgeWeights);
    }

    private List<IloRange> convertToIloConstraints(
        Iterable<CycleCut<E>> cycleCuts) throws IloException {
      List<IloRange> ans = Lists.newArrayList();
      for (CycleCut<E> cycleCut : cycleCuts) {
        ans.add(makeCycleConstraint(cycleCut));
      }
      return ans;
    }

    @Override
    public List<IloRange> quickUserCut() throws IloException {
      return convertToIloConstraints(sep
          .findConnectedComponentCuts(minimumViolationForCut));
    }

    @Override
    public List<IloRange> fullUserCut() throws IloException {
      return convertToIloConstraints(sep.findCycleCuts(minimumViolationForCut,
          threadPool));
    }

  }

  private class SubtourCutGenerator implements UserCutGenerator {
    private Map<E, Double> nonZeroEdgeWeights;
    private SubtourSeparator<V, E> sep;
    private double minimumViolationForCut;

    public SubtourCutGenerator(VariableExtractor variableExtractor,
        double minimumViolationForCut) throws IloException {
      this.minimumViolationForCut = minimumViolationForCut;
      nonZeroEdgeWeights = edgeVariables
          .getNonZeroVariableValues(variableExtractor);
      sep = new SubtourSeparator<V, E>(kepInstance.getGraph(),
          kepInstance.getRootNodes(), nonZeroEdgeWeights);
    }

    private List<IloRange> convertToIloConstraints(List<Subtour<V, E>> subtours)
        throws IloException {
      List<IloRange> ans = Lists.newArrayList();
      for (Subtour<V, E> subtour : subtours) {
        ans.add(makeSubtourConstraint(subtour));
      }
      return ans;
    }

    @Override
    public List<IloRange> quickUserCut() throws IloException {
      return convertToIloConstraints(sep
          .findConnectedComponentCuts(minimumViolationForCut));
    }

    @Override
    public List<IloRange> fullUserCut() throws IloException {
      return convertToIloConstraints(sep.findCycleCuts(minimumViolationForCut,
          threadPool));
    }
  }

  private class CutsetCutGenerator implements UserCutGenerator {

    private Map<E, Double> nonZeroEdgeWeights;
    private CutsetSeparator<V, E> sep;
    private double minimumViolationForCut;

    public CutsetCutGenerator(VariableExtractor variableExtractor,
        double minimumViolationForCut) throws IloException {
      this.minimumViolationForCut = minimumViolationForCut;
      nonZeroEdgeWeights = edgeVariables
          .getNonZeroVariableValues(variableExtractor);
      sep = new CutsetSeparator<V, E>(kepInstance.getGraph(),
          kepInstance.getRootNodes(), kepInstance.getPairedNodes(),
          nonZeroEdgeWeights);
    }

    private List<IloRange> convertToIloConstraints(
        Iterable<Cutset<V, E>> cutsets) throws IloException {
      List<IloRange> ans = Lists.newArrayList();
      double best = 0;
      Cutset<V, E> bestCut = null;
      IloRange bestConstraint = null;
      for (Cutset<V, E> cut : cutsets) {
        IloRange constraint = makeCutsetConstraint(cut);
        if (cut.getViolation() > best) {
          bestCut = cut;
          best = cut.getViolation();
          bestConstraint = constraint;
        }
        ans.add(constraint);
      }
      // System.out.println(bestCut == null ? "0" : bestCut.getViolation());
      // return bestCut == null ? Lists.<IloConstraint>newArrayList():
      // Lists.<IloConstraint>newArrayList(bestConstraint);
      return ans;
    }

    @Override
    public List<IloRange> quickUserCut() throws IloException {
      // System.out.print("quick ");
      return convertToIloConstraints(sep.findEasyCuts(minimumViolationForCut));
    }

    @Override
    public List<IloRange> fullUserCut() throws IloException {
      // System.out.print("full ");
      return convertToIloConstraints(sep
          .findHardCuts(this.minimumViolationForCut));
    }
  }

  public CycleVariables<V, E> getCycleVariables() {
    return cycleVariables;
  }

  public DirectedEdgeVariables<V, E> getEdgeVariables() {
    return edgeVariables;
  }

  public FlowInterface<V, E> getFlowInterface() {
    return flowInterface;
  }

  public static class CycleChainPolytopeFractionalSolution<E> {
    private Map<E, Double> nonZeroEdgeValues;
    private Map<EdgeCycle<E>, Double> nonZeroCycleValues;

    public CycleChainPolytopeFractionalSolution(
        Map<E, Double> nonZeroEdgeValues,
        Map<EdgeCycle<E>, Double> nonZeroCycleValues) {
      super();
      this.nonZeroEdgeValues = nonZeroEdgeValues;
      this.nonZeroCycleValues = nonZeroCycleValues;
    }

    public Map<E, Double> getNonZeroEdgeValues() {
      return nonZeroEdgeValues;
    }

    public Map<EdgeCycle<E>, Double> getNonZeroCycleValues() {
      return nonZeroCycleValues;
    }
  }

  @Override
  public CycleChainDecomposition<V, E> recoverSolution() throws IloException {
    Set<E> edgesUsed = this.edgeVariables.getNonZeroVariables();
    Set<EdgeCycle<E>> cyclesUsed = this.cycleVariables.getNonZeroVariables();
    return new CycleChainDecomposition<V, E>(kepInstance.getGraph(), edgesUsed,
        cyclesUsed);
  }

  // TODO(rander): this needs to be unified with the above method.
  public CycleChainDecomposition<V, E> recoverSolution(
      VariableExtractor variableExtractor) throws IloException {
    Set<E> edgesUsed = this.edgeVariables
        .getNonZeroVariables(variableExtractor);
    Set<EdgeCycle<E>> cyclesUsed = this.cycleVariables
        .getNonZeroVariables(variableExtractor);
    return new CycleChainDecomposition<V, E>(kepInstance.getGraph(), edgesUsed,
        cyclesUsed);
  }

  @Override
  public void relaxAllIntegerVariables() throws IloException {
    this.cycleVariables.relaxIntegrality();
    this.edgeVariables.relaxIntegrality();
    this.flowInterface.relaxIntegrality();
  }

  @Override
  public void restateAllIntegerVariables() throws IloException {
    this.cycleVariables.restateIntegrality();
    this.edgeVariables.restateIntegrality();
    this.flowInterface.restateIntegrality();
  }

  @Override
  public UserSolution createUserSolution(CycleChainDecomposition<V, E> solution) {
    Set<E> edgesInSolution = Sets.newHashSet();
    for (EdgeChain<E> chain : solution.getEdgeChains()) {
      edgesInSolution.addAll(chain.getEdges());
    }
    Set<EdgeCycle<E>> cyclesInSolution = Sets.newHashSet(solution
        .getEdgeCycles());
    return createUserSolution(edgesInSolution, cyclesInSolution);
  }

  @Override
  public UserSolution createUserSolution(Set<E> edges, Set<EdgeCycle<E>> cycles) {
    IloIntVar[] variables = ObjectArrays.concat(edgeVariables.getVars(),
        cycleVariables.getVars(), IloIntVar.class);

    double[] values = new double[edgeVariables.size() + cycleVariables.size()];
    int ansPos = 0;
    for (IloIntVar edgeVar : edgeVariables.getVars()) {
      if (edges.contains(edgeVariables.getInverse(edgeVar))) {
        values[ansPos] = 1.0;
      } else {
        values[ansPos] = 0.0;
      }
      ansPos++;
    }
    for (IloIntVar cycleVar : cycleVariables.getVars()) {
      if (cycles.contains(cycleVariables.getInverse(cycleVar))) {
        values[ansPos] = 1.0;
      } else {
        values[ansPos] = 0.0;
      }
      ansPos++;
    }
    return new UserSolution(variables, values);
  }

  @Override
  public UserSolution roundFractionalSolution(
      VariableExtractor variableExtractor) {
    try {
      Map<E, Double> nonZeroEdgeValues = this.edgeVariables
          .getNonZeroVariableValues(variableExtractor);
      Map<EdgeCycle<E>, Double> nonZeroCycleValues = this.cycleVariables
          .getNonZeroVariableValues(variableExtractor);
      DisjointPathsImprovementHeuristic<V, E> disjointPaths = DisjointPathsImprovementHeuristic
          .constructImproveRepeat(kepInstance, nonZeroEdgeValues,
              nonZeroCycleValues, .51, 0.0, threadPool);
      return createUserSolution(disjointPaths.getOutSolution());
    } catch (IloException e) {
      throw new RuntimeException(e);
    }

  }
}
