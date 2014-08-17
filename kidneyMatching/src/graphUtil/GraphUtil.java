package graphUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import multiPeriod.TimeInstant;

import org.apache.commons.collections15.Predicate;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class GraphUtil {

  public static <V, E> DirectedSparseMultigraph<V, E> makeSubgraph(
      DirectedSparseMultigraph<V, E> graph, final Set<E> includedEdges) {
    EdgePredicateFilter<V, E> filter = new EdgePredicateFilter<V, E>(
        new Predicate<E>() {
          @Override
          public boolean evaluate(E edge) {
            return includedEdges.contains(edge);
          }
        });
    return (DirectedSparseMultigraph<V, E>) filter.transform(graph);
  }

  public static <V, E> Set<E> allInternalEdges(
      DirectedSparseMultigraph<V, E> graph, Set<V> vertices) {
    Set<E> ans = new HashSet<E>();
    for (V source : vertices) {
      for (V sink : vertices) {
        ans.addAll(graph.findEdgeSet(source, sink));
      }
    }
    return ans;
  }

  public static <V, E> Set<V> allContactedVertices(
      DirectedSparseMultigraph<V, E> graph, Set<E> edges) {
    Set<V> ans = new HashSet<V>();
    for (E edge : edges) {
      ans.addAll(graph.getEndpoints(edge));
    }
    return ans;
  }

  public static <T> double doubleSumDefaultZero(Map<T, ? extends Number> map,
      Iterable<T> iterable) {
    return doubleSum(Functions.forMap(map, Double.valueOf(0)), iterable);
  }

  public static <T> double doubleSum(Function<T, ? extends Number> func,
      Iterable<T> iterable) {
    double ans = 0;
    for (T t : iterable) {
      ans += func.apply(t).doubleValue();
    }
    return ans;
  }

  /**
   * It is assumed that connectedComponent is a connected component of the
   * graph, and that every node in the graph has in degree at most one and out
   * degree at most one
   * 
   * @param connectedComponent
   * @param integerOnlyGraph
   * @return
   */
  public static <V, E> boolean testSetIsCycle(Set<V> connectedComponent,
      DirectedSparseMultigraph<V, E> degreeTwoGraph) {
    for (V node : connectedComponent) {
      if (degreeTwoGraph.inDegree(node) != 1) {
        return false;
      }
      if (degreeTwoGraph.outDegree(node) != 1) {
        return false;
      }
    }
    return true;
  }

  /**
   * It is assumed that connectedComponent is a connected component of the
   * graph, and that every node in the graph has in degree at most one and out
   * degree at most one
   * 
   * @param connectedComponent
   * @param degreeTwoGraph
   * @return
   */
  public static <V, E> EdgeCycle<E> makeCycle(Set<V> connectedComponent,
      DirectedSparseMultigraph<V, E> degreeTwoGraph) {
    List<E> ans = new ArrayList<E>();
    Set<V> visited = new HashSet<V>();
    if (!connectedComponent.isEmpty()) {
      V first = connectedComponent.iterator().next();
      V next = first;
      for (int i = 0; i < connectedComponent.size(); i++) {
        if (!visited.add(next)) {
          throw new RuntimeException("Visiting node : " + next.toString()
              + " twice.");
        }
        Collection<E> outEdges = degreeTwoGraph.getOutEdges(next);
        if (outEdges.size() != 1) {
          throw new RuntimeException(
              "each node must have out degree one, but found node "
                  + next.toString() + " with out degree " + outEdges.size());
        }
        E edge = outEdges.iterator().next();
        ans.add(edge);
        next = degreeTwoGraph.getDest(edge);
      }
      if (next != first) {
        throw new RuntimeException("Cycle should end where it starts...");
      }
    }
    return new EdgeCycle<E>(ans);
  }

  /**
   * It is assumed that connectedComponent is a connected component of the
   * graph, and that every node in the graph has in degree at most one and out
   * degree at most one
   * 
   * @param start
   * @param connectedComponent
   *          should contain start
   * @param degreeTwoGraph
   * @return
   */
  public static <V, E> EdgeChain<E> makeChain(V start,
      Set<V> connectedComponent, DirectedSparseMultigraph<V, E> degreeTwoGraph) {
    List<E> ans = new ArrayList<E>();
    Set<V> visited = new HashSet<V>();
    if (!connectedComponent.isEmpty()) {
      visited.add(start);
      V next = start;
      for (int i = 0; i < connectedComponent.size() - 1; i++) {
        Collection<E> outEdges = degreeTwoGraph.getOutEdges(next);
        if (outEdges.size() != 1) {
          throw new RuntimeException(
              "each node must have out degree one, but found node "
                  + next.toString() + " with out degree " + outEdges.size());
        }
        E edge = outEdges.iterator().next();
        ans.add(edge);
        next = degreeTwoGraph.getDest(edge);
        if (!visited.add(next)) {
          throw new RuntimeException("Visiting node : " + next.toString()
              + " twice.");
        }
      }
      if (degreeTwoGraph.outDegree(next) != 0) {
        throw new RuntimeException(
            "Chain should end on node with zero out degree.");
      }
    }
    return new EdgeChain<E>(ans);
  }

  public static <V, E> List<V> getNodesInOrder(EdgeCycle<E> orderedEdgeSet,
      DirectedSparseMultigraph<V, E> graph) {
    List<V> ans = new ArrayList<V>();
    for (E edge : orderedEdgeSet.getEdgesInOrder()) {
      ans.add(graph.getSource(edge));
    }
    return ans;
  }

  public static <V, E> List<V> getNodesInOrder(EdgeChain<E> orderedEdgeSet,
      DirectedSparseMultigraph<V, E> graph) {
    List<V> ans = new ArrayList<V>();
    List<E> edgesInOrder = orderedEdgeSet.getEdgesInOrder();
    for (E edge : edgesInOrder) {
      ans.add(graph.getSource(edge));
    }
    if (edgesInOrder.size() > 0) {
      ans.add(graph.getDest(edgesInOrder.get(edgesInOrder.size() - 1)));
    }
    return ans;
  }

  public static <V, E> ImmutableSet<E> edgesIntoCut(
      DirectedSparseMultigraph<V, E> graph, Set<V> component) {
    ImmutableSet.Builder<E> builder = ImmutableSet.builder();
    for (V outNode : graph.getVertices()) {
      if (!component.contains(outNode)) {
        for (V inNode : component) {
          builder.addAll(graph.findEdgeSet(outNode, inNode));
        }
      }
    }
    return builder.build();

  }

  public static <V, E, T extends Comparable<T>> ImmutableMap<E, TimeInstant<T>> inferEdgeArrivalTimes(
      ImmutableMap<V, TimeInstant<T>> nodeArrivalTimes,
      DirectedSparseMultigraph<V, E> feasibleTransplants) {
    ImmutableMap.Builder<E, TimeInstant<T>> edgeArrivalTimes = ImmutableMap
        .builder();
    for (E edge : feasibleTransplants.getEdges()) {
      try {
        TimeInstant<T> sourceTime = nodeArrivalTimes.get(feasibleTransplants
            .getSource(edge));
        TimeInstant<T> destTime = nodeArrivalTimes.get(feasibleTransplants
            .getDest(edge));
        if (sourceTime.compareTo(destTime) <= 0) {
          edgeArrivalTimes.put(edge, destTime);
        } else {
          edgeArrivalTimes.put(edge, sourceTime);
        }
      } catch (RuntimeException e) {
        System.out.println("edge: " + edge);
        System.out.println("source: " + feasibleTransplants.getSource(edge));
        System.out.println("sink: " + feasibleTransplants.getDest(edge));
        System.out.println(nodeArrivalTimes);
        throw e;
      }
    }
    return edgeArrivalTimes.build();
  }

}
