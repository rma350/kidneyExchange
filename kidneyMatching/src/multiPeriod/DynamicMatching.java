package multiPeriod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepProtos.KepProtos;
import kepProtos.KepProtos.CycleValue;
import kepProtos.KepProtos.Segment;
import kepProtos.KepProtos.SegmentedChain;

import com.google.common.base.Functions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;

public class DynamicMatching<V, E, T extends Comparable<T>> {

  private DirectedSparseMultigraph<V, E> graph;

  private List<SimultaneousMatch> cycles;
  private Multimap<TimeInstant<T>, SimultaneousMatch> matchesByTimeInstant;

  // maps the node to the match in which the node received
  private Map<V, SimultaneousMatch> nodeToDonorMatching;
  private Map<V, SimultaneousMatch> nodeToReceiverMatching;
  private Map<V, Chain> chainRootToChain;
  private Map<V, Chain> chainTailToChain;
  private List<TimeInstant<T>> timeInstantsOfMatches;

  public int size() {
    int ans = 0;
    for (TimeInstant<T> time : matchesByTimeInstant.keySet()) {
      Collection<SimultaneousMatch> value = this.matchesByTimeInstant.get(time);
      for (SimultaneousMatch match : value) {
        ans += match.size();
      }

    }
    return ans;
  }

  // TODO: this creates a bad cycle in package dependency of this package on
  // multiperiodanalysis through TimeWriter. Maybe move TimeWriter?
  public DynamicMatching(DirectedSparseMultigraph<V, E> graph,
      KepProtos.DynamicMatching protoMatching, TimeWriter<T> timeWriter,
      ImmutableBiMap<String, V> nodeById, ImmutableBiMap<String, E> edgeById) {
    this(graph);
    for (CycleValue cycleTime : protoMatching.getCycleTimeList()) {
      SimultaneousMatch cycleMatch = new SimultaneousMatch(
          ImmutableList.copyOf(Lists.transform(cycleTime.getCycle()
              .getEdgeNameOrderedList(), Functions.forMap(edgeById))),
          timeWriter.readTime(cycleTime.getValue()));
      cycles.add(cycleMatch);
      matchesByTimeInstant.put(cycleMatch.getTimeMatched(), cycleMatch);
      updateNodeToMatching(cycleMatch);
    }
    for (SegmentedChain protoChain : protoMatching.getChainTimeList()) {
      Chain chain = new Chain();
      for (Segment protoSegment : protoChain.getSegmentList()) {
        SimultaneousMatch chainMatch = new SimultaneousMatch(
            ImmutableList.copyOf(Lists.transform(protoSegment.getEdgeIdList(),
                Functions.forMap(edgeById))), timeWriter.readTime(protoSegment
                .getTime()));
        chain.getClusters().add(chainMatch);
        matchesByTimeInstant.put(chainMatch.getTimeMatched(), chainMatch);
        updateNodeToMatching(chainMatch);
      }
      List<SimultaneousMatch> clusters = chain.getClusters();
      V source = graph.getSource(clusters.get(0).getEdges().get(0));
      chainRootToChain.put(source, chain);
      List<E> edgesLastCluster = clusters.get(clusters.size() - 1).getEdges();
      V tail = graph.getDest(edgesLastCluster.get(edgesLastCluster.size() - 1));
      chainTailToChain.put(tail, chain);
      if (protoChain.getCompleted()) {
        chain.setTerminated(chain.getClusters()
            .get(chain.getClusters().size() - 1).getTimeMatched());
      } else {
        chain.setTerminated(null);
      }
    }
    timeInstantsOfMatches.addAll(matchesByTimeInstant.keySet());
    Collections.sort(timeInstantsOfMatches);
  }

  public DynamicMatching(DirectedSparseMultigraph<V, E> graph) {
    this.graph = graph;
    this.cycles = new ArrayList<SimultaneousMatch>();
    this.matchesByTimeInstant = HashMultimap.create();
    this.nodeToDonorMatching = new HashMap<V, SimultaneousMatch>();
    this.nodeToReceiverMatching = new HashMap<V, SimultaneousMatch>();
    chainRootToChain = new HashMap<V, Chain>();
    timeInstantsOfMatches = new ArrayList<TimeInstant<T>>();
    this.chainTailToChain = new HashMap<V, Chain>();
  }

  public DirectedSparseMultigraph<V, E> getGraph() {
    return graph;
  }

  public List<SimultaneousMatch> getCycles() {
    return cycles;
  }

  public Multimap<TimeInstant<T>, SimultaneousMatch> getMatchesByTimeInstant() {
    return matchesByTimeInstant;
  }

  public Map<V, SimultaneousMatch> getNodeToDonorMatching() {
    return nodeToDonorMatching;
  }

  public Map<V, SimultaneousMatch> getNodeToReceiverMatching() {
    return nodeToReceiverMatching;
  }

  public Map<V, Chain> getChainRootToChain() {
    return chainRootToChain;
  }

  public Map<V, Chain> getChainTailToChain() {
    return chainTailToChain;
  }

  public List<TimeInstant<T>> getTimeInstantsOfMatches() {
    return timeInstantsOfMatches;
  }

  private void updateNodeToMatching(SimultaneousMatch cycleOrChainSegment) {
    for (E edge : cycleOrChainSegment.edges) {
      V donor = graph.getSource(edge);
      V receiver = graph.getDest(edge);
      if (nodeToDonorMatching.containsKey(donor)) {
        throw new RuntimeException();
      }
      if (nodeToReceiverMatching.containsKey(receiver)) {
        throw new RuntimeException();
      }
      nodeToDonorMatching.put(donor, cycleOrChainSegment);
      nodeToReceiverMatching.put(receiver, cycleOrChainSegment);
    }
  }

  public void augmentMatchings(List<EdgeCycle<E>> cyclesByEdges,
      Map<EdgeChain<E>, Boolean> chainByEdgeToTerminate,
      TimeInstant<T> matchingTime) {
    Set<SimultaneousMatch> currentMatching = new HashSet<SimultaneousMatch>();
    for (EdgeCycle<E> cycleEdges : cyclesByEdges) {

      SimultaneousMatch cycle = new SimultaneousMatch(
          cycleEdges.getEdgesInOrder(), matchingTime);
      currentMatching.add(cycle);
      cycles.add(cycle);
      updateNodeToMatching(cycle);
    }
    for (EdgeChain<E> chainEdges : chainByEdgeToTerminate.keySet()) {
      boolean isTerminal = chainByEdgeToTerminate.get(chainEdges)
          .booleanValue();
      SimultaneousMatch cluster = new SimultaneousMatch(
          chainEdges.getEdgesInOrder(), matchingTime);
      currentMatching.add(cluster);
      updateNodeToMatching(cluster);
      V source = graph.getSource(chainEdges.getEdgesInOrder().get(0));
      V sink = graph.getDest(chainEdges.getEdgesInOrder().get(
          chainEdges.size() - 1));
      Chain chain;
      if (this.chainTailToChain.containsKey(source)) {

        chain = this.chainTailToChain.get(source);
        this.chainTailToChain.remove(source);
      } else {
        chain = new Chain();
        this.chainRootToChain.put(source, chain);
      }
      chain.getClusters().add(cluster);
      this.chainTailToChain.put(sink, chain);
      if (chain.getTerminated() != null) {
        throw new RuntimeException("Chain was already terminated");
      }
      if (isTerminal) {
        chain.setTerminated(matchingTime);
      }
    }
    if (currentMatching.size() > 0) {
      this.timeInstantsOfMatches.add(matchingTime);
      this.matchesByTimeInstant.putAll(matchingTime, currentMatching);
    }

  }

  public void validate(Set<V> chainRoots, Set<V> terminalNodes,
      Map<V, TimeInstant<T>> nodeArrivals, Map<E, TimeInstant<T>> edgeArrivals) {
    for (Chain chain : this.chainRootToChain.values()) {
      chain.validate(chainRoots, terminalNodes, nodeArrivals, edgeArrivals);
    }
    for (SimultaneousMatch cycle : this.cycles) {
      cycle.validate(nodeArrivals, edgeArrivals);
      V start = graph.getSource(cycle.getEdges().get(0));
      V end = graph.getDest(cycle.getEdges().get(cycle.getEdges().size() - 1));
      if (start != end) {
        throw new RuntimeException("Cycle start " + start
            + "was not not equal to cycle end" + end);
      }
    }
  }

  public class Chain {
    private List<SimultaneousMatch> clusters;
    private TimeInstant<T> terminated;

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Chain other = (Chain) obj;
      if (clusters == null) {
        if (other.clusters != null)
          return false;
      } else if (!clusters.equals(other.clusters))
        return false;
      if (terminated == null) {
        if (other.terminated != null)
          return false;
      } else if (!terminated.equals(other.terminated))
        return false;
      return true;
    }

    public void validate(Set<V> chainRoots, Set<V> terminalNodes,
        Map<V, TimeInstant<T>> nodeArrivals, Map<E, TimeInstant<T>> edgeArrivals) {
      List<E> edges = clusters.get(0).getEdges();
      V source = graph.getSource(edges.get(0));
      if (!chainRoots.contains(source)) {
        throw new RuntimeException(
            "Found a chain that does begin at a chain root, begins at "
                + source);
      }
      V sink = graph.getDest(edges.get(edges.size() - 1));
      clusters.get(0).validate(nodeArrivals, edgeArrivals);
      TimeInstant<T> clusterTime = clusters.get(0).getTimeMatched();
      for (int i = 1; i < clusters.size(); i++) {
        TimeInstant<T> nextClusterTime = clusters.get(i).getTimeMatched();
        if (nextClusterTime.compareTo(clusterTime) <= 0) {
          throw new RuntimeException("Cluster times must be increasing.");
        }
        clusterTime = nextClusterTime;
        List<E> nextEdges = clusters.get(i).getEdges();
        V nextSource = graph.getSource(nextEdges.get(0));
        if (nextSource != sink) {
          throw new RuntimeException("Cluster ended with " + sink
              + " but next cluster began with " + nextSource);
        }
        source = nextSource;
        sink = graph.getDest(nextEdges.get(nextEdges.size() - 1));
        clusters.get(i).validate(nodeArrivals, edgeArrivals);
      }
      if (this.terminated != null) {
        if (!clusterTime.equals(terminated)) {
          throw new RuntimeException();
        }
        if (!terminalNodes.contains(sink)) {
          throw new RuntimeException("Chain was terminated on node " + sink
              + " which was not a terminal node");
        }
      }

    }

    public List<SimultaneousMatch> getClusters() {
      return clusters;
    }

    public Chain() {
      this.clusters = new ArrayList<SimultaneousMatch>();
      this.terminated = null;
    }

    public TimeInstant<T> getTerminated() {
      return terminated;
    }

    public void setTerminated(TimeInstant<T> terminated) {
      this.terminated = terminated;
    }

    public int getNumEdges() {
      int ans = 0;
      for (SimultaneousMatch match : this.clusters) {
        ans += match.size();
      }
      return ans;
    }

  }

  public class SimultaneousMatch {
    private List<E> edges;
    private TimeInstant<T> timeMatched;

    public void validate(Map<V, TimeInstant<T>> nodeArrivals,
        Map<E, TimeInstant<T>> edgeArrivals) {
      for (int i = 0; i < edges.size(); i++) {
        if (timeMatched.compareTo(edgeArrivals.get(edges.get(i))) < 0) {
          throw new RuntimeException("Edge was matched before it arrived: "
              + edges.get(i));
        }
        V source = graph.getSource(edges.get(i));
        if (timeMatched.compareTo(nodeArrivals.get(source)) < 0) {
          throw new RuntimeException(
              "Edge was matched before source node arrived: " + source);
        }
        V target = graph.getDest(edges.get(i));
        if (timeMatched.compareTo(nodeArrivals.get(target)) < 0) {
          throw new RuntimeException(
              "Edge was matched before target node arrived: " + target);
        }
        if (i < edges.size() - 1) {
          V nextSource = graph.getSource(edges.get(i + 1));
          if (target != nextSource) {
            throw new RuntimeException("Edge ends on " + target
                + " but following edge begins elsewhere on " + nextSource);
          }
        }

      }
    }

    public int size() {
      return this.edges.size();
    }

    public List<E> getEdges() {
      return edges;
    }

    public TimeInstant<T> getTimeMatched() {
      return timeMatched;
    }

    public SimultaneousMatch(List<E> edges, TimeInstant<T> timeMatched) {
      super();

      this.edges = edges;
      this.timeMatched = timeMatched;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((edges == null) ? 0 : edges.hashCode());
      result = prime * result
          + ((timeMatched == null) ? 0 : timeMatched.hashCode());
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
      SimultaneousMatch other = (SimultaneousMatch) obj;
      if (edges == null) {
        if (other.edges != null)
          return false;
      } else if (!edges.equals(other.edges))
        return false;
      if (timeMatched == null) {
        if (other.timeMatched != null)
          return false;
      } else if (!timeMatched.equals(other.timeMatched))
        return false;
      return true;
    }

  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DynamicMatching other = (DynamicMatching) obj;
    if (!cycles.equals(other.cycles)) {
      return false;
    }
    if (!matchesByTimeInstant.equals(other.matchesByTimeInstant)) {
      return false;
    }
    if (!nodeToDonorMatching.equals(other.nodeToDonorMatching)) {
      return false;
    }
    if (!nodeToReceiverMatching.equals(other.nodeToReceiverMatching)) {
      return false;
    }
    if (!chainRootToChain.equals(other.chainRootToChain)) {
      return false;
    }
    if (!chainTailToChain.equals(other.chainTailToChain)) {
      return false;
    }
    if (!timeInstantsOfMatches.equals(other.timeInstantsOfMatches)) {
      return false;
    }
    return true;
  }
}
