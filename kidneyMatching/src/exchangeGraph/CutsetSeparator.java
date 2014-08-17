package exchangeGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.flows.EdmondsKarpMaxFlow;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.GraphUtil;

public class CutsetSeparator<V, E> {

  private DirectedSparseMultigraph<V, E> graph;
  private Set<V> chainRoots;
  private Set<V> pairedNodes;
  private Set<V> activeVertices;
  private Map<E, Double> nonZeroEdgeWeights;
  private DirectedSparseMultigraph<WrapperNode, WrapperEdge> subgraph;
  private Map<V, WrapperNode> nodeMap;
  private Map<E, WrapperEdge> edgeMap;
  private Set<WrapperEdge> superSourceToRoot;
  private WrapperNode superSource;
  private int rescaling;
  private Transformer<WrapperEdge, Number> scaledEdgeWeight;

  private WrapperEdgeFactory edgeFactory;

  private static int defaultRescaling = 50;

  public CutsetSeparator(DirectedSparseMultigraph<V, E> graph,
      Set<V> chainRoots, Set<V> pairedNodes,
      final Map<E, Double> nonZeroEdgeWeights) {
    this(graph, chainRoots, pairedNodes, nonZeroEdgeWeights, defaultRescaling);
  }

  public CutsetSeparator(DirectedSparseMultigraph<V, E> graph,
      Set<V> chainRoots, Set<V> pairedNodes,
      final Map<E, Double> nonZeroEdgeWeights, final int rescaling) {
    this.graph = graph;
    this.chainRoots = chainRoots;
    this.pairedNodes = pairedNodes;
    this.nonZeroEdgeWeights = nonZeroEdgeWeights;
    this.rescaling = rescaling;
    this.subgraph = new DirectedSparseMultigraph<WrapperNode, WrapperEdge>();
    activeVertices = new HashSet<V>();
    for (E edge : nonZeroEdgeWeights.keySet()) {
      activeVertices.addAll(graph.getEndpoints(edge));
    }
    nodeMap = new HashMap<V, WrapperNode>();
    edgeMap = new HashMap<E, WrapperEdge>();
    for (V vert : activeVertices) {
      WrapperNode node = new WrapperNode(vert);
      nodeMap.put(vert, node);
      subgraph.addVertex(node);
    }
    for (E edge : nonZeroEdgeWeights.keySet()) {
      // lets be a little forgiving, in case of programmer error...
      if (nonZeroEdgeWeights.get(edge) > CplexUtil.epsilon) {
        WrapperEdge wEdge = new WrapperEdge(edge);
        edgeMap.put(edge, wEdge);
        subgraph.addEdge(wEdge, nodeMap.get(graph.getSource(edge)),
            nodeMap.get(graph.getDest(edge)));
      }
    }
    this.superSource = new WrapperNode(null);
    this.superSourceToRoot = new HashSet<WrapperEdge>();
    this.subgraph.addVertex(superSource);
    for (V vertex : Sets.intersection(this.chainRoots, this.activeVertices)) {
      WrapperEdge edge = new WrapperEdge(null);
      this.superSourceToRoot.add(edge);
      this.subgraph.addEdge(edge, superSource, nodeMap.get(vertex));
    }
    this.scaledEdgeWeight = new Transformer<WrapperEdge, Number>() {
      @Override
      public Number transform(WrapperEdge arg0) {
        if (superSourceToRoot.contains(arg0)) {
          return Integer.valueOf(1 * rescaling);
        } else if (arg0.getEdge() != null) {
          if (!nonZeroEdgeWeights.containsKey(arg0.getEdge())) {
            throw new RuntimeException();
          }
          return Math.round(nonZeroEdgeWeights.get(arg0.getEdge()) * rescaling);
        } else {
          // IMPORTANT. The edmonds karp implementation creates back edges and
          // for some reason,
          // calls transform on them, expecting the result to be null
          return null;
        }
      }
    };
    this.edgeFactory = new WrapperEdgeFactory();
  }

  public static class Cutset<V, E> {
    private final ImmutableSet<E> cutEdges;
    private final ImmutableSet<V> component;
    private final V rhs;

    private final double violation;

    public ImmutableSet<E> getCutEdges() {
      return cutEdges;
    }

    public ImmutableSet<V> getComponent() {
      return component;
    }

    public V getRhs() {
      return rhs;
    }

    public double getViolation() {
      return this.violation;
    }

    public Cutset(ImmutableSet<E> cutEdges, ImmutableSet<V> component, V rhs,
        double violation) {
      super();
      this.cutEdges = cutEdges;
      this.component = component;
      this.rhs = rhs;
      this.violation = violation;
    }

    public String toString() {
      return "Violation: " + violation + ", rhs: " + rhs.toString()
          + " Component: " + component.toString();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + ((component == null) ? 0 : component.hashCode());
      result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Cutset other = (Cutset) obj;
      if (component == null) {
        if (other.component != null)
          return false;
      } else if (!component.equals(other.component))
        return false;
      if (rhs == null) {
        if (other.rhs != null)
          return false;
      } else if (!rhs.equals(other.rhs))
        return false;
      return true;
    }

  }

  private ImmutableSet<V> getNodesFromWrapperNodes(
      Set<WrapperNode> wrapperNodes, boolean allowNullNodes) {
    ImmutableSet.Builder<V> builder = ImmutableSet.builder();
    for (WrapperNode node : wrapperNodes) {
      if (node.getNode() == null) {
        if (!allowNullNodes) {
          throw new RuntimeException();
        }
      } else {
        builder.add(node.getNode());
      }
    }
    return builder.build();
  }

  public Set<Cutset<V, E>> findEasyCuts(double minimumViolation) {
    Set<Cutset<V, E>> ans = Sets.newHashSet();
    WeakComponentClusterer<WrapperNode, WrapperEdge> clusterer = new WeakComponentClusterer<WrapperNode, WrapperEdge>();
    Set<Set<WrapperNode>> components = clusterer.transform(subgraph);

    ImmutableSet<V> bigCutComponent = null;
    ImmutableSet<E> bigCutEdges = null;
    for (Set<WrapperNode> component : components) {
      if (component.contains(this.superSource)) {
        bigCutComponent = Sets.difference(pairedNodes,
            getNodesFromWrapperNodes(component, true)).immutableCopy();
        bigCutEdges = GraphUtil.edgesIntoCut(graph, bigCutComponent);
      }
    }
    if (bigCutComponent == null || bigCutEdges == null) {
      throw new RuntimeException(
          "Could not find component containing super source");
    }

    for (Set<WrapperNode> component : components) {
      if (!component.contains(this.superSource)) {

        ImmutableSet<V> cutComponent = getNodesFromWrapperNodes(component,
            false);
        ImmutableSet<E> cutEdges = GraphUtil.edgesIntoCut(graph, cutComponent);

        for (V rhs : cutComponent) {
          ImmutableSet<E> flowInToRhs = ImmutableSet.copyOf(graph
              .getInEdges(rhs));
          double violation = realEdgeWeight(flowInToRhs);
          if (violation >= minimumViolation) {
            ans.add(new Cutset<V, E>(cutEdges, cutComponent, rhs, violation));
            ans.add(new Cutset<V, E>(bigCutEdges, bigCutComponent, rhs,
                violation));
          }
        }
      }
    }
    return ans;
  }

  private double realEdgeWeight(Iterable<E> edges) {
    double ans = 0;
    for (E edge : edges) {
      if (this.nonZeroEdgeWeights.containsKey(edge)) {
        ans += this.nonZeroEdgeWeights.get(edge);
      }
    }
    return ans;
  }

  public Set<Cutset<V, E>> findHardCuts(double minimumViolation) {
    Set<Cutset<V, E>> ans = Sets.newHashSet();
    for (V sink : this.nodeMap.keySet()) {
      if (!this.chainRoots.contains(sink)) {
        WrapperNode sinkNode = this.nodeMap.get(sink);
        Map<WrapperEdge, Number> flowQuantities = new HashMap<WrapperEdge, Number>();
        EdmondsKarpMaxFlow<WrapperNode, WrapperEdge> maxFlow = new EdmondsKarpMaxFlow<WrapperNode, WrapperEdge>(
            subgraph, this.superSource, sinkNode, this.scaledEdgeWeight,
            flowQuantities, edgeFactory);
        maxFlow.evaluate();
        Set<WrapperEdge> minCutWrapperEdges = maxFlow.getMinCutEdges();
        if (Sets.intersection(minCutWrapperEdges, this.superSourceToRoot)
            .isEmpty()) {
          Set<E> minCutEdges = new HashSet<E>();
          for (WrapperEdge edge : minCutWrapperEdges) {
            minCutEdges.add(edge.getEdge());
          }
          double actualCutVal = realEdgeWeight(minCutEdges);
          ImmutableSet<E> flowInToRhs = ImmutableSet.copyOf(graph
              .getInEdges(sink));
          double violation = realEdgeWeight(flowInToRhs) - actualCutVal;
          if (violation >= minimumViolation + CplexUtil.epsilon) {
            ImmutableSet.Builder<V> builder = ImmutableSet.builder();
            for (WrapperNode node : maxFlow.getNodesInSinkPartition()) {
              builder.add(node.getNode());
            }
            ImmutableSet<V> cutComponent = builder.build();
            ImmutableSet<E> cutEdges = GraphUtil.edgesIntoCut(graph,
                cutComponent);
            ans.add(new Cutset<V, E>(cutEdges, cutComponent, sink, violation));
          }
        }
      }
    }
    return ans;
  }

  private class WrapperNode {
    private V node;

    public WrapperNode(V node) {
      this.node = node;
    }

    public V getNode() {
      return node;
    }

    public String toString() {
      return node == null ? "superSource" : node.toString();
    }
  }

  private class WrapperEdge {
    private E edge;

    public WrapperEdge(E edge) {
      this.edge = edge;
    }

    public E getEdge() {
      return edge;
    }

    public String toString() {
      return edge == null ? "fromSuperSource or factory" : edge.toString();
    }
  }

  private class WrapperEdgeFactory implements Factory<WrapperEdge> {

    public WrapperEdgeFactory() {
    }

    @Override
    public WrapperEdge create() {
      return new WrapperEdge(null);
    }

  }

}
