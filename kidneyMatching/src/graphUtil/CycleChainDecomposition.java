package graphUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class CycleChainDecomposition<V, E> {

  private List<EdgeCycle<E>> edgeCycles;
  private List<EdgeChain<E>> edgeChains;
  private DirectedSparseMultigraph<V, E> degreeTwoGraph;

  public CycleChainDecomposition(DirectedSparseMultigraph<V, E> graph,
      List<EdgeCycle<E>> edgeCycles, List<EdgeChain<E>> edgeChains) {
    this.edgeChains = edgeChains;
    this.edgeCycles = edgeCycles;
    // TODO it isn't clear why we are storing the degree two graph...?
    Set<E> includedEdges = new HashSet<E>();
    for (EdgeCycle<E> cycle : edgeCycles) {
      includedEdges.addAll(cycle.getEdges());
    }
    for (EdgeChain<E> chain : edgeChains) {
      includedEdges.addAll(chain.getEdges());
    }
    this.degreeTwoGraph = GraphUtil.makeSubgraph(graph, includedEdges);
  }

  public CycleChainDecomposition(DirectedSparseMultigraph<V, E> graph,
      Set<E> includedEdges, Collection<EdgeCycle<E>> identifiedCycles) {
    this(GraphUtil.makeSubgraph(graph, includedEdges));
    this.edgeCycles.addAll(identifiedCycles);
  }

  /**
   * 
   * @param graph
   *          every node in graph must have in degree at most one and out degree
   *          at most one, otherwise throws error
   */
  public CycleChainDecomposition(DirectedSparseMultigraph<V, E> degreeTwoGraph) {
    this.degreeTwoGraph = degreeTwoGraph;
    this.edgeChains = Lists.newArrayList();
    this.edgeCycles = Lists.newArrayList();
    validateInput();
    WeakComponentClusterer<V, E> clusterer = new WeakComponentClusterer<V, E>();
    Set<Set<V>> connectedComponents = clusterer.transform(degreeTwoGraph);
    for (Set<V> connectedComponent : connectedComponents) {
      if (connectedComponent.size() > 1 ||
      // this condition allows for self loops
          (connectedComponent.size() == 1 && degreeTwoGraph
              .outDegree(connectedComponent.iterator().next()) > 0)) {
        Optional<V> start = findStart(connectedComponent);
        if (start.isPresent()) {
          EdgeChain<E> chain = GraphUtil.makeChain(start.get(),
              connectedComponent, degreeTwoGraph);
          edgeChains.add(chain);
        } else {
          EdgeCycle<E> cycle = GraphUtil.makeCycle(connectedComponent,
              degreeTwoGraph);
          edgeCycles.add(cycle);
        }
      }
    }
  }

  public List<EdgeCycle<E>> getEdgeCycles() {
    return edgeCycles;
  }

  public List<EdgeChain<E>> getEdgeChains() {
    return edgeChains;
  }

  // is absent if the connected component is a loop, otherwise returns the
  // starting node of the chain
  private Optional<V> findStart(Set<V> connectedComponent) {
    for (V vertex : connectedComponent) {
      if (this.degreeTwoGraph.inDegree(vertex) == 0) {
        return Optional.of(vertex);
      }
    }
    return Optional.absent();
  }

  private void validateInput() {
    for (V vertex : degreeTwoGraph.getVertices()) {
      if (degreeTwoGraph.inDegree(vertex) > 1) {
        throw new RuntimeException(
            "Illegal input, in degree greater than one for node "
                + vertex.toString() + " of " + degreeTwoGraph.inDegree(vertex));
      }
      if (degreeTwoGraph.outDegree(vertex) > 1) {
        throw new RuntimeException(
            "Illegal input, out degree greater than one for node "
                + vertex.toString() + " of " + degreeTwoGraph.outDegree(vertex));
      }
    }
  }

  public int totalEdges() {
    int ans = 0;
    for (EdgeCycle<E> cycle : this.edgeCycles) {
      ans += cycle.size();
    }
    for (EdgeChain<E> chain : this.edgeChains) {
      ans += chain.size();
    }
    return ans;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((edgeChains == null) ? 0 : edgeChains.hashCode());
    result = prime * result
        + ((edgeCycles == null) ? 0 : edgeCycles.hashCode());
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
    CycleChainDecomposition other = (CycleChainDecomposition) obj;
    if (edgeChains == null) {
      if (other.edgeChains != null)
        return false;
    } else if (!Sets.newHashSet(edgeChains).equals(
        Sets.newHashSet(other.edgeChains)))
      return false;
    if (edgeCycles == null) {
      if (other.edgeCycles != null)
        return false;
    } else if (!Sets.newHashSet(edgeCycles).equals(
        Sets.newHashSet(other.edgeCycles)))
      return false;
    return true;
  }

  public String toString() {
    return "Chains: " + this.edgeChains.toString() + ", Cycles: "
        + this.edgeCycles.toString();
  }

}
