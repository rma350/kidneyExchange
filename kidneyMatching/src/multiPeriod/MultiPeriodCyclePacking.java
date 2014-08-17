package multiPeriod;

import inputOutput.core.TimeDifferenceCalc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepLib.KepProblemData;
import kepModeler.AuxiliaryInputStatistics;
import statistics.Queries.KepProblemDataInterface;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public abstract class MultiPeriodCyclePacking<V, E, T extends Comparable<T>> {

  public enum MatchingMode {
    EVERY_N_DAYS, EVERY_N_ARRIVALS;
  }

  public static class MultiPeriodCyclePackingInputs<VV, EE, TT extends Comparable<TT>>
      implements KepProblemDataInterface<VV, EE> {
    private DirectedSparseMultigraph<VV, EE> graph;
    private Set<VV> chainRoots;
    private Set<VV> terminalNodes;
    private Set<VV> paired;

    private TimeInstant<TT> startTime;
    private TimeInstant<TT> endTime;

    private ImmutableMap<VV, TimeInstant<TT>> nodeArrivalTimes;
    private ImmutableMap<EE, TimeInstant<TT>> edgeArrivalTimes;

    private TimeWriter<TT> timeWriter;

    private AuxiliaryInputStatistics<VV, EE> auxiliaryInputStatistics;

    public int killRootsRandom(double removalProbability) {
      Map<VV, TimeInstant<TT>> mutableNodeArrivalTimes = Maps
          .newHashMap(nodeArrivalTimes);
      Map<EE, TimeInstant<TT>> mutableEdgeArrivalTimes = Maps
          .newHashMap(edgeArrivalTimes);

      Iterator<VV> it = chainRoots.iterator();
      int removed = 0;
      while (it.hasNext()) {
        VV root = it.next();
        if (Math.random() < removalProbability) {
          removed++;
          it.remove();
          for (EE edge : graph.getOutEdges(root)) {
            mutableEdgeArrivalTimes.remove(edge);
          }
          mutableNodeArrivalTimes.remove(root);
          this.graph.removeVertex(root);
        }
      }
      nodeArrivalTimes = ImmutableMap.copyOf(mutableNodeArrivalTimes);
      edgeArrivalTimes = ImmutableMap.copyOf(mutableEdgeArrivalTimes);
      return removed;
    }

    public MultiPeriodCyclePackingInputs(KepProblemData<VV, EE> problemData,
        TimeInstant<TT> startTime, TimeInstant<TT> endTime,
        ImmutableMap<VV, TimeInstant<TT>> nodeArrivalTimes,
        ImmutableMap<EE, TimeInstant<TT>> edgeArrivalTimes,
        TimeWriter<TT> timeWriter) {
      this(problemData.getGraph(), problemData.getRootNodes(), problemData
          .getTerminalNodes(), startTime, endTime, nodeArrivalTimes,
          edgeArrivalTimes, timeWriter);
    }

    public MultiPeriodCyclePackingInputs(
        DirectedSparseMultigraph<VV, EE> graph, Set<VV> chainRoots,
        Set<VV> terminalNodes, TimeInstant<TT> startTime,
        TimeInstant<TT> endTime,
        ImmutableMap<VV, TimeInstant<TT>> nodeArrivalTimes,
        ImmutableMap<EE, TimeInstant<TT>> edgeArrivalTimes,
        TimeWriter<TT> timeWriter) {
      super();
      this.graph = graph;
      this.chainRoots = chainRoots;
      this.terminalNodes = terminalNodes;
      this.paired = new HashSet<VV>(graph.getVertices());
      paired.removeAll(chainRoots);
      paired.removeAll(terminalNodes);
      this.startTime = startTime;
      this.endTime = endTime;
      this.nodeArrivalTimes = nodeArrivalTimes;
      this.edgeArrivalTimes = edgeArrivalTimes;
      this.timeWriter = timeWriter;
    }

    public <D> void shuffleArrivalTimes(TimeDifferenceCalc<TT, D> calc) {
      List<TimeInstant<TT>> arrivalTimes = new ArrayList<TimeInstant<TT>>();
      List<VV> nodes = new ArrayList<VV>();
      Map<EE, D> delay = new HashMap<EE, D>();

      ImmutableMap.Builder<VV, TimeInstant<TT>> shuffledNodeArrivalTimes = ImmutableMap
          .builder();
      ImmutableMap.Builder<EE, TimeInstant<TT>> shuffledEdgeArrivalTimes = ImmutableMap
          .builder();

      for (VV node : this.nodeArrivalTimes.keySet()) {
        // hack...
        if (this.getEffectiveNodeType(node) == EffectiveNodeType.paired
            && this.nodeArrivalTimes.get(node).compareTo(startTime) > 0) {
          nodes.add(node);
          arrivalTimes.add(this.nodeArrivalTimes.get(node));
          for (EE edge : graph.getOutEdges(node)) {
            delay.put(
                edge,
                calc.getDifference(nodeArrivalTimes.get(node),
                    edgeArrivalTimes.get(edge)));
          }
        } else {
          shuffledNodeArrivalTimes.put(node, this.nodeArrivalTimes.get(node));
          for (EE edge : graph.getOutEdges(node)) {
            shuffledEdgeArrivalTimes.put(edge, this.edgeArrivalTimes.get(edge));
          }
        }
      }
      Collections.shuffle(nodes);

      for (int i = 0; i < nodes.size(); i++) {
        shuffledNodeArrivalTimes.put(nodes.get(i), arrivalTimes.get(i));
        for (EE edge : graph.getOutEdges(nodes.get(i))) {
          shuffledEdgeArrivalTimes.put(edge, arrivalTimes.get(i));// calc.add(arrivalTimes.get(i),
                                                                  // delay.get(edge)));
        }
      }

      this.nodeArrivalTimes = shuffledNodeArrivalTimes.build();
      this.edgeArrivalTimes = shuffledEdgeArrivalTimes.build();

    }

    public EffectiveNodeType getEffectiveNodeType(VV node) {
      if (this.chainRoots.contains(node)) {
        return EffectiveNodeType.chainRoot;
      } else if (this.terminalNodes.contains(node)) {
        return EffectiveNodeType.terminal;
      } else if (this.paired.contains(node)) {
        return EffectiveNodeType.paired;
      } else {
        throw new RuntimeException("node was not in inputs");
      }
    }

    public TimeInstant<TT> getStartTime() {
      return startTime;
    }

    public TimeInstant<TT> getEndTime() {
      return endTime;
    }

    public DirectedSparseMultigraph<VV, EE> getGraph() {
      return graph;
    }

    public Set<VV> getRootNodes() {
      return chainRoots;
    }

    public Set<VV> getTerminalNodes() {
      return terminalNodes;
    }

    public ImmutableMap<VV, TimeInstant<TT>> getNodeArrivalTimes() {
      return nodeArrivalTimes;
    }

    public ImmutableMap<EE, TimeInstant<TT>> getEdgeArrivalTimes() {
      return edgeArrivalTimes;
    }

    public AuxiliaryInputStatistics<VV, EE> getAuxiliaryInputStatistics() {
      return auxiliaryInputStatistics;
    }

    public void setAuxiliaryInputStatistics(
        AuxiliaryInputStatistics<VV, EE> auxiliaryInputStatistics) {
      this.auxiliaryInputStatistics = auxiliaryInputStatistics;
    }

    public TimeWriter<TT> getTimeWriter() {
      return this.timeWriter;
    }

    public KepProblemData<VV, EE> getProblemData() {
      return new KepProblemData<VV, EE>(graph, chainRoots, paired,
          terminalNodes);
    }

  }

  public static enum EffectiveNodeType {
    paired, chainRoot, terminal;
  }

  protected MatchingMode matchingMode;

  protected MultiPeriodCyclePackingInputs<V, E, T> inputs;

  protected List<TimeInstant<T>> augmentMatchingTimes;// it is assumed that this
                                                      // list is in order low to
                                                      // hi.

  protected DynamicMatching<V, E, T> dynamicMatching;

  protected int currentMatchingTime;

  public DynamicMatching<V, E, T> getDynamicMatching() {
    return dynamicMatching;
  }

  public List<TimeInstant<T>> getAugmentMatchingTimes() {
    return augmentMatchingTimes;
  }

  public int getCurrentMatchingTime() {
    return currentMatchingTime;
  }

  public MultiPeriodCyclePackingInputs<V, E, T> getInputs() {
    return this.inputs;
  }

  protected MultiPeriodCyclePacking(
      MultiPeriodCyclePackingInputs<V, E, T> inputs,
      List<TimeInstant<T>> augmentMatchingTimes) {
    this.inputs = inputs;
    this.augmentMatchingTimes = augmentMatchingTimes;
    this.dynamicMatching = new DynamicMatching<V, E, T>(inputs.getGraph());
    this.currentMatchingTime = 0;
    this.matchingMode = MatchingMode.EVERY_N_DAYS;
  }

  protected MultiPeriodCyclePacking(
      MultiPeriodCyclePackingInputs<V, E, T> inputs,
      int minArrivalsBetweenMatchings) {
    this.inputs = inputs;
    this.augmentMatchingTimes = new ArrayList<TimeInstant<T>>();
    this.dynamicMatching = new DynamicMatching<V, E, T>(inputs.getGraph());
    this.currentMatchingTime = 0;
    this.matchingMode = MatchingMode.EVERY_N_ARRIVALS;
    List<TimeInstant<T>> arrivalTimes = new ArrayList<TimeInstant<T>>();
    for (V vertex : inputs.getNodeArrivalTimes().keySet()) {
      if (inputs.getEffectiveNodeType(vertex) == EffectiveNodeType.paired
          && inputs.getNodeArrivalTimes().get(vertex)
              .compareTo(inputs.getStartTime()) > 0) {
        arrivalTimes.add(inputs.getNodeArrivalTimes().get(vertex));
      }
    }
    Collections.sort(arrivalTimes);
    int foundSinceLastMatching = 0;
    for (int i = 0; i < arrivalTimes.size(); i++) {
      if (foundSinceLastMatching < minArrivalsBetweenMatchings) {
        foundSinceLastMatching++;
      } else if (foundSinceLastMatching == minArrivalsBetweenMatchings) {
        augmentMatchingTimes.add(arrivalTimes.get(i));
        while (i + 1 < arrivalTimes.size()
            && arrivalTimes.get(i + 1).compareTo(arrivalTimes.get(i)) == 0) {
          i++;
        }
        foundSinceLastMatching = 0;
      }
    }
    augmentMatchingTimes.add(arrivalTimes.get(arrivalTimes.size() - 1));

  }

  public void computeNextMatching() {
    if (this.currentMatchingTime >= augmentMatchingTimes.size()) {
      throw new ArrayIndexOutOfBoundsException();
    }
    augmentMatching();

    this.currentMatchingTime++;
  }

  public void computeAllMatchings() {
    int position = 0;
    System.out.println("Matching Times: " + augmentMatchingTimes.size());
    System.out.print("Completed: ");
    while (currentMatchingTime < augmentMatchingTimes.size()) {
      computeNextMatching();
      position++;
      if (position % 10 == 0) {
        System.out.print(position + ", ");
      }
    }
    System.out.println();
    this.dynamicMatching.validate(inputs.getRootNodes(),
        inputs.getTerminalNodes(), inputs.getNodeArrivalTimes(),
        inputs.getEdgeArrivalTimes());
    System.out
        .println("Size of total matching: " + this.dynamicMatching.size());
  }

  protected abstract void augmentMatching();

}
