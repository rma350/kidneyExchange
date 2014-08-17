package exchangeGraph;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.CycleChainDecomposition;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import graphUtil.GraphUtil;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepLib.KepInstance;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class AllHeavyEdgesConstructionHeuristic<V, E> {

  private KepInstance<V, E> kepInstance;
  private Map<E, Double> nonZeroEdgeValues;
  private Map<EdgeCycle<E>, Double> nonZeroCycleValues;
  private double minWeightToInclude;
  private CycleChainDecomposition<V, E> solution;
  private CycleChainDecomposition<V, E> abandoned;

  public AllHeavyEdgesConstructionHeuristic(KepInstance<V, E> kepInstance,
      Map<E, Double> nonZeroEdgeValues,
      Map<EdgeCycle<E>, Double> nonZeroCycleValues, double minWeightToInclude) {
    if (minWeightToInclude < .505) {
      throw new RuntimeException(
          "All heavy edges construction heuristic must use a minimum weight of at least "
              + ".505 for each edge to ensure that all nodes have in degree at most one "
              + "and out degree at most one.");
    }
    this.kepInstance = kepInstance;
    this.nonZeroCycleValues = nonZeroCycleValues;
    this.nonZeroEdgeValues = nonZeroEdgeValues;
    this.minWeightToInclude = minWeightToInclude;
    /*
     * (List<Double> edgeVals = Lists.newArrayList(Maps.filterEntries(
     * nonZeroEdgeValues, AllHeavyEdgesConstructionHeuristic.<E> atLeast(.1))
     * .values()); Collections.sort(edgeVals);
     * System.out.println(doublesToString(edgeVals));
     */
    double totalEdgeWeight = sum(nonZeroEdgeValues.values());
    Map<E, Double> filteredEdgeValues = Maps.filterEntries(nonZeroEdgeValues,
        edgeGreaterThanHalf);
    double totalEdgeWeightFiltered = sum(filteredEdgeValues.values());

    DirectedSparseMultigraph<V, E> onlyHeavyEdges = GraphUtil.makeSubgraph(
        kepInstance.getGraph(), Sets.newHashSet(filteredEdgeValues.keySet()));
    CycleChainDecomposition<V, E> heavyEdgesDecomp = new CycleChainDecomposition<V, E>(
        onlyHeavyEdges);
    // we now keep any chains that begin at a root node, and any cycles where
    // the LP had weight at least .5
    List<EdgeCycle<E>> edgeCyclesFeasible = Lists.newArrayList();
    List<EdgeChain<E>> edgeChainsFeasible = Lists.newArrayList();
    // we abandon any chains that begin at a node that is not the root node, or
    // any cycles found in heavyEdgesDecomp

    List<EdgeChain<E>> edgeChainsAbandoned = Lists.newArrayList();
    for (EdgeChain<E> edgeChain : heavyEdgesDecomp.getEdgeChains()) {
      if (kepInstance.getRootNodes().contains(
          onlyHeavyEdges.getSource(edgeChain.getEdgesInOrder().get(0)))) {
        edgeChainsFeasible.add(edgeChain);
      } else {
        edgeChainsAbandoned.add(edgeChain);
      }
    }
    List<EdgeCycle<E>> edgeCyclesAbandoned = Lists.newArrayList();
    for (EdgeCycle<E> cycle : heavyEdgesDecomp.getEdgeCycles()) {
      if (cycle.size() > kepInstance.getMaxCycleLength()) {
        edgeCyclesAbandoned.add(cycle);
      } else {
        System.out.println("max cycle length: "
            + kepInstance.getMaxCycleLength());
        System.out.println("adding a cycle of size: " + cycle.size());
        edgeCyclesFeasible.add(cycle);
      }
    }
    abandoned = new CycleChainDecomposition<V, E>(onlyHeavyEdges,
        edgeCyclesAbandoned, edgeChainsAbandoned);
    System.out.println("lp edge weight: "
        + shortFormat.format(totalEdgeWeight)
        + ", heavy edge weight: "
        + shortFormat.format(totalEdgeWeightFiltered)
        + ", chain edge weight: "
        + shortFormat.format((computeWeight(edgeChainsFeasible,
            filteredEdgeValues))) + ", chain edge count: "
        + edgeCount(edgeChainsFeasible));

    double totalCycleWeight = sum(nonZeroCycleValues.values());
    Map<EdgeCycle<E>, Double> filteredCycles = Maps.filterEntries(
        nonZeroCycleValues, edgeCycleGreaterThanHalf);
    double filteredCycleWeight = sum(filteredCycles.values());
    Set<EdgeCycle<E>> edgeCyclesInSolution = filteredCycles.keySet();
    System.out.println("lp cycle weight: "
        + shortFormat.format(totalCycleWeight) + ", select cycle weight: "
        + shortFormat.format(filteredCycleWeight) + ", cycle count: "
        + edgeCyclesInSolution.size());
    edgeCyclesFeasible.addAll(edgeCyclesInSolution);
    solution = new CycleChainDecomposition<V, E>(kepInstance.getGraph(),
        edgeCyclesFeasible, edgeChainsFeasible);
    /*
     * Set<E> selectedEdgesDeprecated = getSelectedEdges(filteredEdgeValues);
     * Set<E> allEdges = Sets.newHashSet(); for (EdgeChain<E> edgeChain :
     * edgeChainsFeasible) { allEdges.addAll(edgeChain.getEdges()); }
     * System.out.println("deprecated edges: " + selectedEdgesDeprecated.size()
     * + " standard edges: " + allEdges + ", sets are equal: " +
     * allEdges.equals(selectedEdgesDeprecated));
     */

  }

  public CycleChainDecomposition<V, E> getSolution() {
    return this.solution;
  }

  public CycleChainDecomposition<V, E> getAbandoned() {
    return this.abandoned;
  }

  public static <T> Predicate<Map.Entry<T, Double>> atLeast(
      final double minValue) {
    return new Predicate<Map.Entry<T, Double>>() {
      public boolean apply(Map.Entry<T, Double> entry) {
        return entry.getValue() >= minValue;
      }
    };
  }

  private final Predicate<Map.Entry<E, Double>> edgeGreaterThanHalf = atLeast(.51);
  private final Predicate<Map.Entry<EdgeCycle<E>, Double>> edgeCycleGreaterThanHalf = atLeast(.51);

  private static double sum(Iterable<Double> summands) {
    double ans = 0;
    for (Double summand : summands) {
      ans += summand;
    }
    return ans;
  }

  private static NumberFormat shortFormat = new DecimalFormat("0.00");

  private static String doublesToString(List<Double> doubles) {
    StringBuilder ans = new StringBuilder();
    for (int i = 0; i < doubles.size(); i++) {
      ans.append(shortFormat.format(doubles.get(i)));
      if (i < doubles.size() - 1) {
        ans.append(',');
      }
    }
    return ans.toString();
  }

  private int edgeCount(List<EdgeChain<E>> edgeChains) {
    int ans = 0;
    for (EdgeChain<E> chain : edgeChains) {
      ans += chain.size();
    }
    return ans;
  }

  private double computeWeight(List<EdgeChain<E>> edgeChains,
      Map<E, Double> edgeVals) {
    double ans = 0;
    for (EdgeChain<E> chain : edgeChains) {
      for (E edge : chain) {
        ans += edgeVals.get(edge);
      }
    }
    return ans;
  }

  @Deprecated
  private Set<E> getSelectedEdges(Map<E, Double> filteredEdgeValues) {
    Set<E> potentialEdges = filteredEdgeValues.keySet();
    Set<E> actualEdges = Sets.newHashSet();
    for (V root : kepInstance.getRootNodes()) {
      V head = root;
      while (head != null) {
        SetView<E> outEdge = Sets.intersection(potentialEdges,
            Sets.newHashSet(kepInstance.getGraph().getOutEdges(head)));
        if (outEdge.isEmpty()) {
          head = null;
        } else {
          E inUse = outEdge.iterator().next();
          actualEdges.add(inUse);
          head = kepInstance.getGraph().getDest(inUse);
        }
      }
    }
    return actualEdges;
  }

}
