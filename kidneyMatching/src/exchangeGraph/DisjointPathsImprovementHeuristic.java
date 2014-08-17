package exchangeGraph;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.CycleChainDecomposition;
import graphUtil.Edge;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import graphUtil.GraphUtil;
import graphUtil.Node;

import java.util.List;
import java.util.Map;
import java.util.Set;

import kepLib.KepInstance;
import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;

public class DisjointPathsImprovementHeuristic<V, E> {

  private KepInstance<V, E> kepInstance;
  private Map<E, Double> nonZeroEdgeValues;
  private Map<EdgeCycle<E>, Double> nonZeroCycleValues;
  private CycleChainDecomposition<V, E> inSolution;
  private CycleChainDecomposition<V, E> inAbandoned;
  private Optional<FixedThreadPool> threadPool;

  private DirectedSparseMultigraph<Node, Edge> disjointPathsGraph;
  private Map<Edge, Integer> disjointPathsNonZeroEdgeWeights;

  private BiMap<Node, V> connectingNodes;
  private BiMap<Node, EdgeCycle<E>> targetCycleNodes;
  private BiMap<Node, EdgeChain<E>> targetChainNodes;
  private BiMap<Node, EdgeChain<E>> sourceNodes;
  private Map<V, EdgeChain<E>> repNodeSources;
  private Map<V, EdgeChain<E>> repNodeTargetChains;
  private Map<V, EdgeCycle<E>> repNodeTargetCycles;

  private boolean improved;
  private CycleChainDecomposition<V, E> outSolution;
  private CycleChainDecomposition<V, E> outAbandoned;

  public static <V, E> DisjointPathsImprovementHeuristic<V, E> constructImproveRepeat(
      KepInstance<V, E> kepInstance, Map<E, Double> nonZeroEdgeValues,
      Map<EdgeCycle<E>, Double> nonZeroCycleValues,
      double minWeightForceInclusion, double minWeightInConnection,
      Optional<FixedThreadPool> threadPool) {
    DisjointPathsImprovementHeuristic<V, E> next = constructAndImprove(
        kepInstance, nonZeroEdgeValues, nonZeroCycleValues,
        minWeightForceInclusion, minWeightInConnection, threadPool);
    while (next.didImprove()) {
      next = new DisjointPathsImprovementHeuristic<V, E>(kepInstance,
          nonZeroEdgeValues, nonZeroCycleValues, next.getOutSolution(),
          next.getOutAbandoned(), minWeightInConnection, threadPool);
    }
    return next;
  }

  public static <V, E> DisjointPathsImprovementHeuristic<V, E> constructAndImprove(
      KepInstance<V, E> kepInstance, Map<E, Double> nonZeroEdgeValues,
      Map<EdgeCycle<E>, Double> nonZeroCycleValues,
      double minWeightForceInclusion, double minWeightInConnection,
      Optional<FixedThreadPool> threadPool) {
    AllHeavyEdgesConstructionHeuristic<V, E> allHeavyEdges = new AllHeavyEdgesConstructionHeuristic<V, E>(
        kepInstance, nonZeroEdgeValues, nonZeroCycleValues,
        minWeightForceInclusion);
    return new DisjointPathsImprovementHeuristic<V, E>(kepInstance,
        nonZeroEdgeValues, nonZeroCycleValues, allHeavyEdges.getSolution(),
        allHeavyEdges.getAbandoned(), minWeightInConnection, threadPool);
  }

  public DisjointPathsImprovementHeuristic(KepInstance<V, E> kepInstance,
      Map<E, Double> nonZeroEdgeValues,
      Map<EdgeCycle<E>, Double> nonZeroCycleValues,
      CycleChainDecomposition<V, E> inSolution,
      CycleChainDecomposition<V, E> inAbandoned, double minWeightInConnection,
      Optional<FixedThreadPool> threadPool) {
    this.kepInstance = kepInstance;
    this.nonZeroCycleValues = nonZeroCycleValues;
    this.nonZeroEdgeValues = nonZeroEdgeValues;
    this.inSolution = inSolution;
    this.inAbandoned = inAbandoned;
    this.threadPool = threadPool;
    disjointPathsGraph = new DirectedSparseMultigraph<Node, Edge>();
    Node superSource = new Node("ss");
    disjointPathsGraph.addVertex(superSource);

    // build a node for each chain in the solution from the original
    // graph. Connect that node to the super source. Designate the
    // Representative vertex from the original graph as the final vertex in the
    // chain.
    sourceNodes = makeNodesFor(inSolution.getEdgeChains(), "s");
    repNodeSources = Maps.newHashMap();
    for (EdgeChain<E> sourceChain : sourceNodes.values()) {
      List<V> nodesInOrder = GraphUtil.getNodesInOrder(sourceChain,
          kepInstance.getGraph());
      V last = nodesInOrder.get(nodesInOrder.size() - 1);
      repNodeSources.put(last, sourceChain);
    }
    Set<Edge> ssEdges = Sets.newHashSet();
    for (Node node : sourceNodes.keySet()) {
      Edge e = new Edge("ss->" + node.getName());
      ssEdges.add(e);
      disjointPathsGraph.addEdge(e, superSource, node);
    }

    Node superSink = new Node("tt");
    disjointPathsGraph.addVertex(superSink);

    disjointPathsNonZeroEdgeWeights = Maps.newHashMap();

    // Build a node for each chain that was abandoned in the original graph.
    // Connect that node to the super sink with an edge. Set the objective
    // weight for this edge to be the number of edges in the chain. Designate
    // the representative vertex from the original graph as the first node in
    // the chain.
    Set<Edge> ttChainEdges = Sets.newHashSet();
    targetChainNodes = makeNodesFor(inAbandoned.getEdgeChains(), "tCha");
    repNodeTargetChains = Maps.newHashMap();
    for (EdgeChain<E> targetChain : targetChainNodes.values()) {
      List<V> nodesInOrder = GraphUtil.getNodesInOrder(targetChain,
          kepInstance.getGraph());
      V first = nodesInOrder.get(0);
      repNodeTargetChains.put(first, targetChain);
    }
    for (Map.Entry<Node, EdgeChain<E>> entry : targetChainNodes.entrySet()) {
      Edge e = new Edge(entry.getKey().getName() + "->tt");
      ttChainEdges.add(e);
      disjointPathsGraph.addEdge(e, entry.getKey(), superSink);
      disjointPathsNonZeroEdgeWeights.put(e, entry.getValue().size());
    }

    // Build a node for each cycle that was abandoned in the original graph.
    // Connect that node to the super sink with an edge. Set the objective
    // weight for this edge to be the number of edges in the cycle minus one.
    // Designate every vertex from the cycle
    // as a representative vertex for the new node.
    Set<Edge> ttCycleEdges = Sets.newHashSet();
    targetCycleNodes = makeNodesFor(inAbandoned.getEdgeCycles(), "tCyc");
    repNodeTargetCycles = Maps.newHashMap();
    for (EdgeCycle<E> targetCycle : targetCycleNodes.values()) {
      for (V v : GraphUtil.getNodesInOrder(targetCycle, kepInstance.getGraph())) {
        repNodeTargetCycles.put(v, targetCycle);
      }
    }
    for (Map.Entry<Node, EdgeCycle<E>> entry : targetCycleNodes.entrySet()) {
      Edge e = new Edge(entry.getKey().getName() + "->tt");
      ttCycleEdges.add(e);
      disjointPathsGraph.addEdge(e, entry.getKey(), superSink);
      disjointPathsNonZeroEdgeWeights.put(e, entry.getValue().size() - 1);
    }

    Set<V> alreadyUsedVertices = Sets.newHashSet();
    alreadyUsedVertices.addAll(nodesUsedChains(kepInstance.getGraph(),
        inSolution.getEdgeChains()));
    alreadyUsedVertices.addAll(nodesUsedChains(kepInstance.getGraph(),
        inAbandoned.getEdgeChains()));
    alreadyUsedVertices.addAll(nodesUsedCycles(kepInstance.getGraph(),
        inSolution.getEdgeCycles()));
    alreadyUsedVertices.addAll(nodesUsedCycles(kepInstance.getGraph(),
        inAbandoned.getEdgeCycles()));

    Set<V> alreadyUsedLegalEdgeSource = Sets
        .newHashSet(repNodeSources.keySet());
    ImmutableSet<V> alreadyUsedLegalEdgeTarget = Sets.union(
        repNodeTargetCycles.keySet(), repNodeTargetChains.keySet())
        .immutableCopy();

    ImmutableSet<V> networkNodes = Sets.difference(
        Sets.newHashSet(kepInstance.getPairedNodes()), alreadyUsedVertices)
        .immutableCopy();
    connectingNodes = makeNodesFor(networkNodes, "n");

    ImmutableSet<V> legalEdgeSources = Sets.union(networkNodes,
        alreadyUsedLegalEdgeSource).immutableCopy();
    ImmutableSet<V> legalEdgeTargets = Sets.union(networkNodes,
        alreadyUsedLegalEdgeTarget).immutableCopy();

    BiMap<Edge, E> networkEdges = HashBiMap.create();
    Iterable<E> edgesToTest = minWeightInConnection <= 0 ? this.kepInstance
        .getGraph().getEdges() : Maps.filterValues(nonZeroEdgeValues,
        Range.atLeast(minWeightInConnection)).keySet();
    int edgeCount = 0;
    for (E e : edgesToTest) {
      V start = kepInstance.getGraph().getSource(e);
      V dest = kepInstance.getGraph().getDest(e);
      if (legalEdgeSources.contains(start) && legalEdgeTargets.contains(dest)) {
        Edge edge = new Edge("e" + (edgeCount++));
        networkEdges.put(edge, e);
        disjointPathsGraph.addEdge(edge, getNodeFor(start), getNodeFor(dest));
      }
    }
    UnitNodeCapacityMaxFlowSolver<Node, Edge> maxFlow = new UnitNodeCapacityMaxFlowSolver<Node, Edge>(
        disjointPathsGraph, superSource, superSink,
        disjointPathsNonZeroEdgeWeights, false, threadPool);
    System.out.println("Max flow objective: " + maxFlow.getObjValue());
    this.improved = maxFlow.getObjValue() > 0;
    if (improved) {
      Set<E> allChainEdges = Sets.newHashSet();
      for (EdgeChain<E> edgeChain : this.inSolution.getEdgeChains()) {
        allChainEdges.addAll(edgeChain.getEdges());
      }
      Map<EdgeCycle<E>, E> excludedEdges = Maps.newHashMap();
      for (Edge edge : maxFlow.getEdgesInSolution()) {
        Node target = this.disjointPathsGraph.getDest(edge);
        if (this.targetCycleNodes.containsKey(target)) {
          E trueEdge = networkEdges.get(edge);
          V cycleToChainStart = kepInstance.getGraph().getDest(trueEdge);
          EdgeCycle<E> edgeCycle = targetCycleNodes.get(target);
          E deletedEdge = getEdgeInto(cycleToChainStart, edgeCycle);
          if (deletedEdge == null) {
            throw new RuntimeException("no edge to delete for " + edgeCycle);
          }
          excludedEdges.put(edgeCycle, deletedEdge);
        }
      }
      Set<EdgeCycle<E>> abandonedOutCycles = Sets.newHashSet(this.inAbandoned
          .getEdgeCycles());
      Set<EdgeChain<E>> abandonedOutChains = Sets.newHashSet(this.inAbandoned
          .getEdgeChains());
      for (Edge edge : maxFlow.getEdgesInSolution()) {
        if (networkEdges.containsKey(edge)) {

          allChainEdges.add(networkEdges.get(edge));
        } else if (ttChainEdges.contains(edge)) {
          EdgeChain<E> edgeChain = targetChainNodes.get(this.disjointPathsGraph
              .getSource(edge));
          abandonedOutChains.remove(edgeChain);
          allChainEdges.addAll(edgeChain.getEdges());
        } else if (ttCycleEdges.contains(edge)) {
          EdgeCycle<E> edgeCycle = targetCycleNodes.get(this.disjointPathsGraph
              .getSource(edge));
          abandonedOutCycles.remove(edgeCycle);
          E excluded = excludedEdges.get(edgeCycle);
          for (E e : edgeCycle) {
            if (e != excluded) {
              allChainEdges.add(e);
            }
          }
        }

      }
      this.outAbandoned = new CycleChainDecomposition<V, E>(
          this.kepInstance.getGraph(), Lists.newArrayList(abandonedOutCycles),
          Lists.newArrayList(abandonedOutChains));

      this.outSolution = new CycleChainDecomposition<V, E>(
          kepInstance.getGraph(), allChainEdges,
          this.inSolution.getEdgeCycles());
      if (outSolution.getEdgeCycles().size() > 0) {
        System.out.println("in solution edge cycles: "
            + this.inSolution.getEdgeCycles());
        System.out.println("out solution edge cycles: "
            + this.outSolution.getEdgeCycles());
      }
      for (EdgeCycle<E> edgeCycle : outSolution.getEdgeCycles()) {
        for (E edge : edgeCycle) {
          if (networkEdges.inverse().containsKey(edge)) {
            System.out.println(edge + " is a network edge");
          }
        }
      }

    } else {
      this.outSolution = inSolution;
      this.outAbandoned = inAbandoned;
    }

  }

  private E getEdgeInto(V v, EdgeCycle<E> edgeCycle) {
    for (E edge : edgeCycle) {
      if (this.kepInstance.getGraph().getDest(edge) == v) {
        return edge;
      }
    }
    throw new RuntimeException("Found no edge into: " + v
        + ", searching in edge cycle: " + edgeCycle);
  }

  public CycleChainDecomposition<V, E> getOutSolution() {
    return this.outSolution;
  }

  public CycleChainDecomposition<V, E> getOutAbandoned() {
    return this.outAbandoned;
  }

  public boolean didImprove() {
    return this.improved;
  }

  private Node getNodeFor(V v) {
    if (this.connectingNodes.containsValue(v)) {
      return this.connectingNodes.inverse().get(v);
    } else if (this.repNodeSources.containsKey(v)) {
      return this.sourceNodes.inverse().get(this.repNodeSources.get(v));
    } else if (this.repNodeTargetChains.containsKey(v)) {
      return this.targetChainNodes.inverse().get(
          this.repNodeTargetChains.get(v));
    } else if (this.repNodeTargetCycles.containsKey(v)) {
      return this.targetCycleNodes.inverse().get(
          this.repNodeTargetCycles.get(v));
    } else {
      throw new RuntimeException("Failed to find node: " + v);
    }
  }

  // TODO(rander): the two below methods could be unified if OrderedEdgeSet
  // conatined an abstract method getNodesInOrder that took in the graph
  private List<V> nodesUsedCycles(DirectedSparseMultigraph<V, E> graph,
      Iterable<EdgeCycle<E>> edgeCycles) {
    List<V> ans = Lists.newArrayList();
    for (EdgeCycle<E> edgeCycle : edgeCycles) {
      ans.addAll(GraphUtil.getNodesInOrder(edgeCycle, graph));
    }
    return ans;
  }

  private List<V> nodesUsedChains(DirectedSparseMultigraph<V, E> graph,
      Iterable<EdgeChain<E>> edgeChains) {
    List<V> ans = Lists.newArrayList();
    for (EdgeChain<E> edgeChain : edgeChains) {
      ans.addAll(GraphUtil.getNodesInOrder(edgeChain, graph));
    }
    return ans;
  }

  private <T> BiMap<Node, T> makeNodesFor(Iterable<T> objects, String baseName) {
    BiMap<Node, T> ans = HashBiMap.create();
    int nodeCount = 0;
    for (T ob : objects) {
      Node node = new Node(baseName + (nodeCount++));
      disjointPathsGraph.addVertex(node);
      ans.put(node, ob);
    }
    return ans;
  }

}
