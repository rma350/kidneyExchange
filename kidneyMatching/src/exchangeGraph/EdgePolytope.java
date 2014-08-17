package exchangeGraph;

import graphUtil.CycleChainDecomposition;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import graphUtil.GraphUtil;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.Arrays;
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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CycleSeparator.CycleCut;
import exchangeGraph.VariableSet.VariableExtractor;

public class EdgePolytope<V, E> implements KepSolutionPolytope<V, E> {

  private KepInstance<V, E> kepInstance;
  private IloCplex cplex;
  private ImmutableSet<SolverOption> solverOptions;

  private Optional<FixedThreadPool> threadPool;

  private DirectedEdgeVariables<V, E> edgeVariables;
  private FlowInterface<V, E> flowInterface;
  private IntegerFlowNetwork<V, E> flowNetwork;

  private boolean enforceMaximumChainLength;

  public EdgePolytope(KepInstance<V, E> kepInstance, IloCplex cplex,
      Optional<FixedThreadPool> threadPool,
      ImmutableSet<SolverOption> solverOptions) throws IloException {
    this.kepInstance = kepInstance;
    this.threadPool = threadPool;
    this.cplex = cplex;
    this.enforceMaximumChainLength = kepInstance.getMaxChainLength() < kepInstance
        .getPairedNodes().size() + 2
        && !solverOptions.contains(SolverOption.ignoreMaxChainLength);
    this.solverOptions = solverOptions;
    this.flowNetwork = new IntegerFlowNetwork<V, E>(kepInstance.getGraph(),
        kepInstance.getRootNodes(), kepInstance.getPairedNodes(),
        kepInstance.getTerminalNodes(), cplex,
        solverOptions.contains(SolverOption.expandedFormulation));
    this.edgeVariables = flowNetwork.getEdgeVariables();
    this.flowInterface = flowNetwork.getFlowInterface();
    if (enforceMaximumChainLength) {
      MaxChainLength.constrainMaxLength(kepInstance, cplex, solverOptions,
          edgeVariables, false);
    }
    addAuxiliaryConstraints();
  }

  @Override
  public IloLinearIntExpr indicatorEdgeSelected(E edge) throws IloException {
    IloLinearIntExpr ans = cplex.linearIntExpr();
    ans.addTerm(this.edgeVariables.get(edge), 1);
    return ans;
  }

  @Override
  public UserCutGenerator makeUserCutGenerator(
      VariableExtractor variableExtractor) throws IloException {
    return NoUserCuts.INSTANCE;
  }

  @Override
  public List<IloRange> lazyConstraint(VariableExtractor variableExtractor)
      throws IloException {
    if (this.solverOptions.contains(SolverOption.lazyConstraintCallback)) {
      return (new LongCycleCutGenerator(variableExtractor, CplexUtil.epsilon,
          kepInstance.getMaxCycleLength() + 1)).quickUserCut();
    } else {
      return Lists.newArrayList();
    }
  }

  @Override
  public IloLinearNumExpr createObjective() throws IloException {
    return this.edgeVariables.doubleSum(edgeVariables,
        kepInstance.getEdgeWeights());
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

  private class LongCycleCutGenerator implements UserCutGenerator {
    private Map<E, Double> nonZeroEdgeWeights;
    private CycleSeparator<V, E> sep;
    private double minimumViolationForCut;

    public LongCycleCutGenerator(VariableExtractor variableExtractor,
        double minimumViolationForCut, int minCycleEdgesToSeparate)
        throws IloException {
      this.minimumViolationForCut = minimumViolationForCut;
      nonZeroEdgeWeights = edgeVariables
          .getNonZeroVariableValues(variableExtractor);
      sep = new CycleSeparator<V, E>(kepInstance.getGraph(),
          kepInstance.getRootNodes(), nonZeroEdgeWeights,
          minCycleEdgesToSeparate);
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
      throw new UnsupportedOperationException();
    }

  }

  private void addAuxiliaryConstraints() throws IloException {
    for (NodeFlowInConstraint<V> flowInConstraint : kepInstance
        .getNodeFlowInConstraints()) {
      IloLinearNumExpr sum = cplex.linearNumExpr();
      for (V vertex : flowInConstraint.getNodes()) {
        flowInterface.addFlowInDoubleScaled(vertex, sum, flowInConstraint
            .getCoefficients().apply(vertex).doubleValue());
      }
      CplexUtil.addConstraint(sum, flowInConstraint.getRelationType(),
          flowInConstraint.getRhs(), cplex);
    }
    for (NodeFlowOutConstraint<V> flowOutConstraint : kepInstance
        .getNodeFlowOutConstraints()) {
      IloLinearNumExpr sum = cplex.linearNumExpr();
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
      IloLinearNumExpr sum = edgeVariables.doubleSum(edgeConstraint.getEdges(),
          edgeConstraint.getCoefficients());
      CplexUtil.addConstraint(sum, edgeConstraint.getRelationType(),
          edgeConstraint.getRhs(), cplex);
    }
  }

  @Override
  public CycleChainDecomposition<V, E> recoverSolution() throws IloException {
    return new CycleChainDecomposition<V, E>(GraphUtil.makeSubgraph(
        this.kepInstance.getGraph(), this.edgeVariables.getNonZeroVariables()));
  }

  @Override
  public void relaxAllIntegerVariables() throws IloException {
    this.edgeVariables.relaxIntegrality();
  }

  @Override
  public void restateAllIntegerVariables() throws IloException {
    this.edgeVariables.restateIntegrality();
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
    Set<E> allEdges = Sets.newHashSet(edges);
    for (EdgeCycle<E> cycle : cycles) {
      allEdges.addAll(cycle.getEdges());
    }
    IloIntVar[] variables = Arrays.copyOf(edgeVariables.getVars(),
        edgeVariables.getVars().length);
    double[] values = new double[edgeVariables.size()];
    int ansPos = 0;
    for (IloIntVar edgeVar : edgeVariables.getVars()) {
      if (edges.contains(edgeVariables.getInverse(edgeVar))) {
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
    throw new UnsupportedOperationException();
  }
}
