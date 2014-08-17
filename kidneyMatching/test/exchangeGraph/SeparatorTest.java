package exchangeGraph;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CutsetSeparator.Cutset;
import exchangeGraph.CycleSeparator.CycleCut;
import exchangeGraph.SubtourSeparator.Subtour;
import graphUtil.EdgeCycle;

public class SeparatorTest {

  private static enum Node {
    a, b, c, d, e, r;
  }

  private static enum Edge {
    ab(Node.a, Node.b, 1), bc(Node.b, Node.c, 1), cd(Node.c, Node.d, 1), da(
        Node.d, Node.a, 1), re(Node.r, Node.e, 1), db(Node.d, Node.b, 0), ea(
        Node.e, Node.a, 0);

    private Edge(Node source, Node sink, double weight) {
      this.source = source;
      this.sink = sink;
      this.weight = weight;
    }

    private final Node source;
    private final Node sink;
    private double weight;

    public Node getSource() {
      return source;
    }

    public Node getSink() {
      return sink;
    }

    public double getWeight() {
      return weight;
    }
  }

  public static Map<Edge, Double> makeEdgeWeights(Set<Edge> edges) {
    Map<Edge, Double> ans = new EnumMap<Edge, Double>(Edge.class);
    for (Edge edge : edges) {
      if (edge.getWeight() > 0) {
        ans.put(edge, edge.getWeight());
      }
    }
    return ans;
  }

  public static DirectedSparseMultigraph<Node, Edge> makeTestGraph(
      Set<Node> nodes, Set<Edge> edges) {
    DirectedSparseMultigraph<Node, Edge> graph = new DirectedSparseMultigraph<Node, Edge>();
    for (Node node : nodes) {
      graph.addVertex(node);
    }
    for (Edge edge : edges) {
      graph.addEdge(edge, edge.getSource(), edge.getSink());
    }
    return graph;
  }

  @Test
  public void testCutsetEasyCutsOnEdgeSimple() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeEdgeWeights(EnumSet.allOf(Edge.class));
    Set<Node> roots = EnumSet.of(Node.r);
    Set<Node> paired = EnumSet.of(Node.a, Node.b, Node.c, Node.d, Node.e);
    CutsetSeparator<Node, Edge> cutsetSep = new CutsetSeparator<Node, Edge>(
        graph, roots, paired, weights);
    Set<Cutset<Node, Edge>> cuts = cutsetSep.findEasyCuts(.1);
    assertEquals(4, cuts.size());
    EnumSet<Node> cutComponent = EnumSet.of(Node.a, Node.b, Node.c, Node.d);
    EnumSet<Edge> cutEdges = EnumSet.of(Edge.ea);
    EnumSet<Node> sources = EnumSet.noneOf(Node.class);
    for (Cutset<Node, Edge> cut : cuts) {
      assertEquals(cutComponent, cut.getComponent());
      assertEquals(cutEdges, cut.getCutEdges());
      sources.add(cut.getRhs());
      assertEquals(1, cut.getViolation(), CplexUtil.epsilon);

    }
    assertEquals(cutComponent, sources);
  }

  @Test
  public void testCutsetSepHardCutsOnEdgeSimple() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeEdgeWeights(EnumSet.allOf(Edge.class));
    Set<Node> roots = EnumSet.of(Node.r);
    Set<Node> paired = EnumSet.of(Node.a, Node.b, Node.c, Node.d, Node.e);
    CutsetSeparator<Node, Edge> cutsetSep = new CutsetSeparator<Node, Edge>(
        graph, roots, paired, weights);
    Set<Cutset<Node, Edge>> cuts = cutsetSep.findHardCuts(0);
    assertEquals(4, cuts.size());
    EnumSet<Node> cutComponent = EnumSet.of(Node.a, Node.b, Node.c, Node.d);
    EnumSet<Edge> cutEdges = EnumSet.of(Edge.ea);
    EnumSet<Node> sources = EnumSet.noneOf(Node.class);
    for (Cutset<Node, Edge> cut : cuts) {
      assertEquals(cutComponent, cut.getComponent());
      assertEquals(cutEdges, cut.getCutEdges());
      sources.add(cut.getRhs());
      assertEquals(1, cut.getViolation(), CplexUtil.epsilon);

    }
    assertEquals(cutComponent, sources);
  }

  @Test
  public void testCycleSepEasyCutsOnEdgeSimple() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeEdgeWeights(EnumSet.allOf(Edge.class));
    Set<Node> roots = EnumSet.of(Node.r);

    CycleSeparator<Node, Edge> cycleSep = new CycleSeparator<Node, Edge>(graph,
        roots, weights);
    List<CycleCut<Edge>> cycles = cycleSep.findConnectedComponentCuts(.1);
    assertEquals(1, cycles.size());
    CycleCut<Edge> actual = cycles.get(0);
    assertEquals(4, actual.getWeight(), CplexUtil.epsilon);
    assertEquals(1, actual.getViolated(), CplexUtil.epsilon);
    EdgeCycle<Edge> expected = new EdgeCycle<Edge>(Arrays.asList(Edge.ab,
        Edge.bc, Edge.cd, Edge.da));
    assertEquals(expected, actual.getCycle());
    EdgeCycle.validateEqual(expected, actual.getCycle());
  }

  @Test
  public void testCycleSepHardCutsOnEdgeSimple() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeEdgeWeights(EnumSet.allOf(Edge.class));
    Set<Node> roots = EnumSet.of(Node.r);
    CycleSeparator<Node, Edge> cycleSep = new CycleSeparator<Node, Edge>(graph,
        roots, weights);
    int numThreads = 2;
    Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
    Set<CycleCut<Edge>> cycles = cycleSep.findCycleCuts(.1, threadPool);
    FixedThreadPool.shutDown(threadPool);
    assertEquals(1, cycles.size());

    CycleCut<Edge> cut = cycles.iterator().next();
    EdgeCycle<Edge> actual = cut.getCycle();
    EdgeCycle<Edge> expected = new EdgeCycle<Edge>(Arrays.asList(Edge.ab,
        Edge.bc, Edge.cd, Edge.da));
    assertEquals(expected, actual);
    EdgeCycle.validateEqual(expected, actual);
    assertEquals(4, cut.getWeight(), CplexUtil.epsilon);
    assertEquals(1, cut.getViolated(), CplexUtil.epsilon);
  }

  @Test
  public void testSubtourSepEasyCutsOnEdgeSimple() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeEdgeWeights(EnumSet.allOf(Edge.class));
    Set<Node> roots = EnumSet.of(Node.r);
    SubtourSeparator<Node, Edge> subtourSep = new SubtourSeparator<Node, Edge>(
        graph, roots, weights);
    List<Subtour<Node, Edge>> subtours = subtourSep
        .findConnectedComponentCuts(.1);
    assertEquals(1, subtours.size());
    EnumSet<Node> subtourNodes = EnumSet.of(Node.a, Node.b, Node.c, Node.d);
    EnumSet<Edge> subtourEdges = EnumSet.of(Edge.ab, Edge.bc, Edge.cd, Edge.da,
        Edge.db);
    Subtour<Node, Edge> subtour = subtours.get(0);
    assertEquals(subtourEdges, subtour.getInternalEdges());
    assertEquals(4, subtour.getTotalWeight(), CplexUtil.epsilon);
    assertEquals(1, subtour.getViolation(), CplexUtil.epsilon);
    assertEquals(subtourNodes, subtour.getNodes());
  }

  @Test
  public void testSubtourSepHardCutsOnEdgeSimple() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeEdgeWeights(EnumSet.allOf(Edge.class));
    Set<Node> roots = EnumSet.of(Node.r);
    SubtourSeparator<Node, Edge> subtourSep = new SubtourSeparator<Node, Edge>(
        graph, roots, weights);
    int numThreads = 2;
    Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
    List<Subtour<Node, Edge>> subtours = subtourSep.findCycleCuts(.1,
        threadPool);
    FixedThreadPool.shutDown(threadPool);
    assertEquals(1, subtours.size());
    EnumSet<Node> subtourNodes = EnumSet.of(Node.a, Node.b, Node.c, Node.d);
    EnumSet<Edge> subtourEdges = EnumSet.of(Edge.ab, Edge.bc, Edge.cd, Edge.da,
        Edge.db);
    Subtour<Node, Edge> subtour = subtours.get(0);
    assertEquals(subtourEdges, subtour.getInternalEdges());
    assertEquals(4, subtour.getTotalWeight(), CplexUtil.epsilon);
    assertEquals(1, subtour.getViolation(), CplexUtil.epsilon);
    assertEquals(subtourNodes, subtour.getNodes());
  }

  private static Map<Edge, Double> makeAlternateWeights() {
    Map<Edge, Double> ans = new EnumMap<Edge, Double>(Edge.class);
    ans.put(Edge.re, .5);
    ans.put(Edge.ea, .5);
    ans.put(Edge.ab, .5);
    ans.put(Edge.bc, 1.0);
    ans.put(Edge.cd, 1.0);
    ans.put(Edge.db, .5);
    return ans;
  }

  @Test
  public void testCutsetEasyCutsOnAlternate() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeAlternateWeights();
    Set<Node> roots = EnumSet.of(Node.r);
    Set<Node> paired = EnumSet.of(Node.a, Node.b, Node.c, Node.d, Node.e);
    CutsetSeparator<Node, Edge> cutsetSep = new CutsetSeparator<Node, Edge>(
        graph, roots, paired, weights);
    Set<Cutset<Node, Edge>> cuts = cutsetSep.findEasyCuts(.1);
    assertEquals(0, cuts.size());
  }

  @Test
  public void testCycleEasyCutsOnAlternate() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeAlternateWeights();
    Set<Node> roots = EnumSet.of(Node.r);

    CycleSeparator<Node, Edge> cycleSep = new CycleSeparator<Node, Edge>(graph,
        roots, weights);
    List<CycleCut<Edge>> cuts = cycleSep.findConnectedComponentCuts(.1);
    assertEquals(0, cuts.size());
  }

  @Test
  public void testSubtourEasyCutsOnAlternate() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeAlternateWeights();
    Set<Node> roots = EnumSet.of(Node.r);
    SubtourSeparator<Node, Edge> subtourSep = new SubtourSeparator<Node, Edge>(
        graph, roots, weights);
    List<Subtour<Node, Edge>> cuts = subtourSep.findConnectedComponentCuts(.1);
    assertEquals(0, cuts.size());
  }

  @Test
  public void testCutsetHardCutsOnAlternate() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeAlternateWeights();
    Set<Node> roots = EnumSet.of(Node.r);
    Set<Node> paired = EnumSet.of(Node.a, Node.b, Node.c, Node.d, Node.e);
    CutsetSeparator<Node, Edge> cutsetSep = new CutsetSeparator<Node, Edge>(
        graph, roots, paired, weights);
    Set<Cutset<Node, Edge>> cuts = cutsetSep.findHardCuts(0);
    assertEquals(3, cuts.size());
    EnumSet<Node> cutComponent = EnumSet.of(Node.b, Node.c, Node.d);
    EnumSet<Node> sources = EnumSet.noneOf(Node.class);
    for (Cutset<Node, Edge> cut : cuts) {
      assertEquals(3, Sets.intersection(cutComponent, cut.getComponent())
          .size());
      sources.add(cut.getRhs());
      assertEquals(.5, cut.getViolation(), CplexUtil.epsilon);

    }
    assertEquals(cutComponent, sources);
  }

  @Test
  public void testCycleHardCutsOnAlternate() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeAlternateWeights();
    Set<Node> roots = EnumSet.of(Node.r);
    CycleSeparator<Node, Edge> cycleSep = new CycleSeparator<Node, Edge>(graph,
        roots, weights);
    int numThreads = 2;
    Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
    Set<CycleCut<Edge>> cycles = cycleSep.findCycleCuts(0, threadPool);
    FixedThreadPool.shutDown(threadPool);
    assertEquals(1, cycles.size());
    CycleCut<Edge> cycleCut = cycles.iterator().next();
    EdgeCycle<Edge> expected = new EdgeCycle<Edge>(Arrays.asList(Edge.bc,
        Edge.cd, Edge.db));
    assertEquals(expected, cycleCut.getCycle());
    EdgeCycle.validateEqual(expected, cycleCut.getCycle());
    assertEquals(.5, cycleCut.getViolated(), CplexUtil.epsilon);
    assertEquals(2.5, cycleCut.getWeight(), CplexUtil.epsilon);
  }

  @Test
  public void testSubtourHardCutsOnAlternate() {
    DirectedSparseMultigraph<Node, Edge> graph = makeTestGraph(
        EnumSet.allOf(Node.class), EnumSet.allOf(Edge.class));
    Map<Edge, Double> weights = makeAlternateWeights();
    Set<Node> roots = EnumSet.of(Node.r);
    SubtourSeparator<Node, Edge> subtourSep = new SubtourSeparator<Node, Edge>(
        graph, roots, weights);
    int numThreads = 2;
    Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
    List<Subtour<Node, Edge>> subtours = subtourSep
        .findCycleCuts(0, threadPool);
    FixedThreadPool.shutDown(threadPool);
    assertEquals(1, subtours.size());
    EnumSet<Edge> cycleEdges = EnumSet.of(Edge.bc, Edge.cd, Edge.db);
    Subtour<Node, Edge> subtour = subtours.iterator().next();
    assertEquals(cycleEdges, subtour.getInternalEdges());
    assertEquals(EnumSet.of(Node.b, Node.c, Node.d), subtour.getNodes());
    assertEquals(2.5, subtour.getTotalWeight(), CplexUtil.epsilon);
    assertEquals(.5, subtour.getViolation(), CplexUtil.epsilon);
  }

}
