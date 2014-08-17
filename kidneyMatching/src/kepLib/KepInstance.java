package kepLib;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ui.DemoFrame;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.CycleChainDecomposition;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import graphUtil.GraphUtil;

public class KepInstance<V, E> {

  private KepProblemData<V, E> kepProblemData;
  private Function<? super E, ? extends Number> edgeWeights;
  private int maxChainLength;
  private int maxCycleLength;
  private double cycleBonus;

  private List<NodeFlowInConstraint<V>> nodeFlowInConstraints;
  private List<NodeFlowOutConstraint<V>> nodeFlowOutConstraints;
  private List<EdgeConstraint<E>> edgeUsageConstraints;
  private List<BridgeConstraint<V>> bridgeConstraints;

  public KepInstance(KepProblemData<V, E> kepProblemData,
      Function<? super E, ? extends Number> edgeWeights, int maxChainLength,
      int maxCycleLength, double cycleBonus) {
    this.kepProblemData = kepProblemData;
    this.edgeWeights = edgeWeights;
    this.maxChainLength = maxChainLength;
    this.maxCycleLength = maxCycleLength;
    this.cycleBonus = cycleBonus;
    this.nodeFlowInConstraints = new ArrayList<NodeFlowInConstraint<V>>();
    this.nodeFlowOutConstraints = new ArrayList<NodeFlowOutConstraint<V>>();
    this.edgeUsageConstraints = new ArrayList<EdgeConstraint<E>>();
    this.bridgeConstraints = new ArrayList<BridgeConstraint<V>>();
  }

  public KepInstance(DirectedSparseMultigraph<V, E> graph, Set<V> rootNodes,
      Set<V> pairedNodes, Set<V> terminalNodes,
      Function<? super E, ? extends Number> edgeWeights, int maxChainLength,
      int maxCycleLength, double cycleBonus) {
    this(
        new KepProblemData<V, E>(graph, rootNodes, pairedNodes, terminalNodes),
        edgeWeights, maxChainLength, maxCycleLength, cycleBonus);

  }

  public KepProblemData<V, E> getKepProblemData() {
    return this.kepProblemData;
  }

  public Sets.SetView<V> nonRootNodes() {
    return this.kepProblemData.nonRootNodes();
  }

  public Sets.SetView<V> nonTerminalNodes() {
    return this.kepProblemData.nonTerminalNodes();
  }

  public DirectedSparseMultigraph<V, E> getGraph() {
    return this.kepProblemData.getGraph();
  }

  public ImmutableSet<V> getRootNodes() {
    return this.kepProblemData.getRootNodes();
  }

  public ImmutableSet<V> getPairedNodes() {
    return this.kepProblemData.getPairedNodes();
  }

  public ImmutableSet<V> getTerminalNodes() {
    return this.kepProblemData.getTerminalNodes();
  }

  public Function<? super E, ? extends Number> getEdgeWeights() {
    return edgeWeights;
  }

  public int getMaxChainLength() {
    return maxChainLength;
  }

  public int getMaxCycleLength() {
    return maxCycleLength;
  }

  public List<NodeFlowInConstraint<V>> getNodeFlowInConstraints() {
    return this.nodeFlowInConstraints;
  }

  public List<NodeFlowOutConstraint<V>> getNodeFlowOutConstraints() {
    return this.nodeFlowOutConstraints;
  }

  public List<EdgeConstraint<E>> getEdgeUsageConstraints() {
    return edgeUsageConstraints;
  }

  public List<BridgeConstraint<V>> getBridgeConstraints() {
    return bridgeConstraints;
  }

  public double getCycleBonus() {
    return this.cycleBonus;
  }

  public Function<EdgeCycle<E>, Double> makeCycleWeight(
      final Function<? super E, ? extends Number> edgeWeight,
      final double rescale) {
    return new Function<EdgeCycle<E>, Double>() {
      @Override
      public Double apply(EdgeCycle<E> arg0) {
        double ans = 0;
        for (E edge : arg0.getEdgesInOrder()) {
          ans += edgeWeight.apply(edge).doubleValue();
        }
        return rescale * ans;
      }
    };
  }

  public Function<EdgeCycle<E>, Double> makeCycleWeight(
      final Function<? super V, ? extends Number> nodeWeight) {
    return new Function<EdgeCycle<E>, Double>() {
      @Override
      public Double apply(EdgeCycle<E> edgeCycle) {
        double ans = 0;
        for (E edge : edgeCycle.getEdgesInOrder()) {
          V source = kepProblemData.getGraph().getSource(edge);
          ans += nodeWeight.apply(source).doubleValue();
        }
        return ans;
      }
    };
  }

  public static abstract class SideConstraint {
    private RelationType relationType;
    private double rhs;

    public RelationType getRelationType() {
      return relationType;
    }

    public double getRhs() {
      return rhs;
    }

    protected SideConstraint(RelationType relationType, double rhs) {
      this.relationType = relationType;
      this.rhs = rhs;
    }
  }

  public static abstract class NodeConstraint<U> extends SideConstraint {
    private Set<U> nodes;
    private Function<? super U, ? extends Number> coefficients;

    protected NodeConstraint(Set<U> nodes,
        Function<? super U, ? extends Number> coefficients,
        RelationType relationType, double rhs) {
      super(relationType, rhs);
      this.nodes = nodes;
      this.coefficients = coefficients;
    }

    public Set<U> getNodes() {
      return nodes;
    }

    public Function<? super U, ? extends Number> getCoefficients() {
      return this.coefficients;
    }
  }

  public static class NodeFlowInConstraint<U> extends NodeConstraint<U> {
    public NodeFlowInConstraint(Set<U> nodes,
        Function<? super U, ? extends Number> coefficients,
        RelationType relationType, double rhs) {
      super(nodes, coefficients, relationType, rhs);
    }
  }

  public static class NodeFlowOutConstraint<U> extends NodeConstraint<U> {
    public NodeFlowOutConstraint(Set<U> nodes,
        Function<? super U, ? extends Number> coefficients,
        RelationType relationType, double rhs) {
      super(nodes, coefficients, relationType, rhs);
    }
  }

  public static class EdgeConstraint<F> extends SideConstraint {
    private Set<F> edges;
    private Function<? super F, ? extends Number> coefficients;

    public EdgeConstraint(Set<F> edges,
        Function<? super F, ? extends Number> coefficients,
        RelationType relationType, double rhs) {
      super(relationType, rhs);
      this.edges = edges;
      this.coefficients = coefficients;
    }

    public Set<F> getEdges() {
      return this.edges;
    }

    public Function<? super F, ? extends Number> getCoefficients() {
      return this.coefficients;
    }
  }

  public static class BridgeConstraint<U> extends NodeConstraint<U> {
    public BridgeConstraint(Set<U> nodes,
        Function<? super U, ? extends Number> coefficients,
        RelationType relationType, double rhs) {
      super(nodes, coefficients, relationType, rhs);
    }
  }

  public static RelationType parseRelationType(String s) {
    String trimmed = s.trim().toLowerCase();
    if (trimmed.equals("eq")) {
      return RelationType.eq;
    }
    if (trimmed.equals("geq")) {
      return RelationType.geq;
    }
    if (trimmed.equals("leq")) {
      return RelationType.leq;
    }
    throw new RuntimeException("Unrecognized Input: " + s);
  }

  public static enum RelationType {
    eq, geq, leq;
  }

  public void printToTikz(BufferedWriter writer,
      CycleChainDecomposition<V, E> solution, boolean fullTex) {
    Set<E> edgesUsed = new HashSet<E>();
    for (EdgeCycle<E> cycle : solution.getEdgeCycles()) {
      edgesUsed.addAll(cycle.getEdges());
    }
    for (EdgeChain<E> chain : solution.getEdgeChains()) {
      edgesUsed.addAll(chain.getEdges());
    }
    printToTikz(writer, edgesUsed, fullTex);
  }

  public void printToTikz(BufferedWriter writer, Set<E> matchedEdges,
      boolean fullTex) {
    printToTikz(writer, matchedEdges, fullTex, false);
  }

  public void printToTikz(BufferedWriter writer, Set<E> matchedEdges,
      boolean fullTex, boolean printNodeNames) {
    DemoFrame<V, E> frame = new DemoFrame<V, E>(this.getGraph());
    frame.setVisible(true);
    frame.getGraphPanel().printGraphManual(writer, matchedEdges,
        this.kepProblemData.getRootNodes(),
        this.kepProblemData.getTerminalNodes(), fullTex, printNodeNames);
  }

  public KepInstance<V, E> createRestrictedProblem(Set<E> retainedEdges) {
    DirectedSparseMultigraph<V, E> subgraph = GraphUtil.makeSubgraph(
        this.getGraph(), retainedEdges);

    KepInstance<V, E> ans = new KepInstance<V, E>(subgraph,
        this.kepProblemData.getRootNodes(),
        this.kepProblemData.getPairedNodes(),
        this.kepProblemData.getTerminalNodes(), edgeWeights, maxChainLength,
        maxCycleLength, cycleBonus);
    ans.getBridgeConstraints().addAll(bridgeConstraints);
    ans.getNodeFlowInConstraints().addAll(nodeFlowInConstraints);
    ans.getNodeFlowOutConstraints().addAll(nodeFlowOutConstraints);
    for (EdgeConstraint<E> edgeConstraint : edgeUsageConstraints) {
      ans.getEdgeUsageConstraints().add(
          new EdgeConstraint<E>(Sets.intersection(edgeConstraint.getEdges(),
              retainedEdges), edgeConstraint.getCoefficients(), edgeConstraint
              .getRelationType(), edgeConstraint.getRhs()));
    }
    return ans;
  }

  public KepInstance<V, E> changeObjectiveFunction(
      Function<? super E, ? extends Number> newEdgeWeights, double newCycleBonus) {
    KepInstance<V, E> ans = new KepInstance<V, E>(
        this.kepProblemData.getGraph(), this.kepProblemData.getRootNodes(),
        this.kepProblemData.getPairedNodes(),
        this.kepProblemData.getTerminalNodes(), newEdgeWeights, maxChainLength,
        maxCycleLength, newCycleBonus);
    ans.getBridgeConstraints().addAll(bridgeConstraints);
    ans.getNodeFlowInConstraints().addAll(nodeFlowInConstraints);
    ans.getNodeFlowOutConstraints().addAll(nodeFlowOutConstraints);
    ans.getEdgeUsageConstraints().addAll(edgeUsageConstraints);
    return ans;
  }

}
