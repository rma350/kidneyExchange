package multiPeriodAnalysis;

import java.io.File;
import java.util.List;

import kepModeler.AuxiliaryInputStatistics;
import kepModeler.ChainsForcedRemainOpenOptions;
import kepModeler.KepModeler;
import kepProtos.KepProtos.Cycle;
import kepProtos.KepProtos.CycleValue;
import kepProtos.KepProtos.DynamicMatching;
import kepProtos.KepProtos.KepModelerInputs;
import kepProtos.KepProtos.NodeArrivalTime;
import kepProtos.KepProtos.RandomTrial;
import kepProtos.KepProtos.Segment;
import kepProtos.KepProtos.SegmentedChain;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;
import multiPeriod.TimeInstant;
import multiPeriod.TimeWriter;
import protoModeler.BasePredicateBuilder.HistoricData;
import protoModeler.PredicateFactory;
import protoModeler.UnosHistoricDataImpl;
import protoModeler.UnosPredicateBuilder.UnosHistoricData;
import protoModeler.UnosPredicateFactory;
import statistics.Queries;
import threading.FixedThreadPool;
import unosData.UnosData;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import exchangeGraph.SolverOption;
import graphUtil.GraphUtil;

public class Environment<V, E, T extends Comparable<T>> {

  private static UnosData data = UnosData.readUnosData("data" + File.separator
      + "unosDataItaiCorrected.csv", false, false);

  private Optional<FixedThreadPool> threadPool;
  private int threads;
  private Optional<Double> maxSolveTimeSeconds;
  private MultiPeriodCyclePackingInputs<V, E, T> multiPeriodInputs;
  private ImmutableSet<SolverOption> solverOptions;
  private TimeWriter<T> timeWriter;
  private ObjectiveBuilder objectiveBuilder;
  private ImmutableBiMap<String, V> nodeIds;
  private ImmutableBiMap<String, E> edgeIds;
  private int replications;
  private HistoricData<V, E> historicData;
  private PredicateFactory<V, E> predicateFactory;

  public static Environment<UnosExchangeUnit, UnosDonorEdge, Double> unosEnvironment(
      int threads, int replications) {
    MultiPeriodCyclePackingInputs<UnosExchangeUnit, UnosDonorEdge, Double> inputs = data
        .exportMultiPeriodCyclePackingInputs();
    AuxiliaryInputStatistics<UnosExchangeUnit, UnosDonorEdge> auxiliary = Queries
        .getAuxiliaryStatistics(inputs);
    inputs.setAuxiliaryInputStatistics(auxiliary);
    UnosHistoricData historic = new UnosHistoricDataImpl(
        inputs.getProblemData(), auxiliary);
    PredicateFactory<UnosExchangeUnit, UnosDonorEdge> predicateFactory = new UnosPredicateFactory(
        historic);
    return new Environment<UnosExchangeUnit, UnosDonorEdge, Double>(
        data.getNodeIds(), data.getEdgeIds(), threads, inputs,
        TimeWriter.DoubleTimeWriter.INSTANCE, DefaultObjectiveBuilder.INSTANCE,
        replications, predicateFactory, historic);
  }

  public Environment(ImmutableBiMap<String, V> nodeIds,
      ImmutableBiMap<String, E> edgeIds, int threads,
      MultiPeriodCyclePackingInputs<V, E, T> multiPeriodInputs,
      TimeWriter<T> timeWriter, ObjectiveBuilder objectiveBuilder,
      int replications, PredicateFactory<V, E> predicateFactory,
      HistoricData<V, E> historicData) {
    this.nodeIds = nodeIds;
    this.edgeIds = edgeIds;
    this.multiPeriodInputs = multiPeriodInputs;
    this.threads = threads;
    threadPool = FixedThreadPool.makePool(threads);
    maxSolveTimeSeconds = Optional.of(60.0);
    solverOptions = SolverOption.defaultOptions;
    this.timeWriter = timeWriter;
    this.objectiveBuilder = objectiveBuilder;
    this.replications = replications;
    this.predicateFactory = predicateFactory;
    this.historicData = historicData;
  }

  public HistoricData<V, E> getHistoricData() {
    return this.historicData;
  }

  public PredicateFactory<V, E> getPredicateFactory() {
    return this.predicateFactory;
  }

  public int getThreads() {
    return this.threads;
  }

  public int getReplications() {
    return this.replications;
  }

  public Optional<FixedThreadPool> getThreadPool() {
    return threadPool;
  }

  public Optional<Double> getMaxSolveTimeSeconds() {
    return maxSolveTimeSeconds;
  }

  public UnosData getData() {
    return data;
  }

  public ImmutableSet<SolverOption> getSolverOptions() {
    return solverOptions;
  }

  public MultiPeriodCyclePackingInputs<V, E, T> getMultiPeriodInputs() {
    return multiPeriodInputs;
  }

  public TimeWriter<T> getTimeWriter() {
    return this.timeWriter;
  }

  public void shutDown() {
    FixedThreadPool.shutDown(threadPool);
  }

  public KepModeler createModeler(KepModelerInputs kepModelerInputs) {
    if (kepModelerInputs.hasObjectiveFunction()) {
      return new KepModeler(kepModelerInputs.getMaxChainLength(),
          kepModelerInputs.getMaxCycleLength(),
          ChainsForcedRemainOpenOptions.none,
          kepModelerInputs.getObjectiveFunction(),
          kepModelerInputs.getCycleBonus());
    } else {
      return new KepModeler(kepModelerInputs.getMaxChainLength(),
          kepModelerInputs.getMaxCycleLength(),
          ChainsForcedRemainOpenOptions.none,
          objectiveBuilder.createObjectiveMode(kepModelerInputs.getObjective(),
              kepModelerInputs.getCycleBonus()));
    }
  }

  public multiPeriod.DynamicMatching<V, E, T> restoreDynamicMatching(
      DynamicMatching protoMatching) {
    return new multiPeriod.DynamicMatching<V, E, T>(
        this.multiPeriodInputs.getGraph(), protoMatching, this.timeWriter,
        this.nodeIds, this.edgeIds);
  }

  public DynamicMatching buildProtoDynamicMatching(
      multiPeriod.DynamicMatching<V, E, T> matching) {
    DynamicMatching.Builder ans = DynamicMatching.newBuilder();
    for (multiPeriod.DynamicMatching<V, E, T>.SimultaneousMatch cycle : matching
        .getCycles()) {
      Cycle protoCycle = Cycle.newBuilder()
          .addAllEdgeNameOrdered(getEdgeIds(cycle.getEdges())).build();
      ans.addCycleTime(CycleValue.newBuilder()
          .setValue(timeWriter.writeTime(cycle.getTimeMatched()))
          .setCycle(protoCycle).build());
    }
    for (multiPeriod.DynamicMatching<V, E, T>.Chain chain : matching
        .getChainRootToChain().values()) {
      SegmentedChain.Builder protoChain = SegmentedChain.newBuilder();
      for (multiPeriod.DynamicMatching<V, E, T>.SimultaneousMatch segment : chain
          .getClusters()) {
        protoChain.addSegment(Segment.newBuilder()
            .setTime(timeWriter.writeTime(segment.getTimeMatched()))
            .addAllEdgeId(getEdgeIds(segment.getEdges())));
      }
      protoChain.setCompleted(chain.getTerminated() != null);
      ans.addChainTime(protoChain);
    }
    return ans.build();
  }

  public double getSimulationStart() {
    return this.timeWriter.writeTime(this.multiPeriodInputs.getStartTime());
  }

  public double getSimulationEnd() {
    return this.timeWriter.writeTime(this.multiPeriodInputs.getEndTime());
  }

  private List<String> getEdgeIds(List<E> edges) {
    return Lists.transform(edges, Functions.forMap(this.edgeIds.inverse()));
  }

  public ImmutableBiMap<String, V> getNodeIds() {
    return this.nodeIds;
  }

  public ImmutableBiMap<String, E> getEdgeIds() {
    return this.edgeIds;
  }

  public ImmutableMap<V, TimeInstant<T>> createNodeArrivalTimes(
      RandomTrial randomTrial) {
    if (randomTrial.getNodeArrivalTimeCount() != nodeIds.size()) {
      throw new RuntimeException(
          "Random trial should have arrival time data for all nodes, but random trial # is "
              + randomTrial.getNodeArrivalTimeCount()
              + " and # of nodes is "
              + nodeIds.size());
    }

    ImmutableMap.Builder<V, TimeInstant<T>> nodeArrivalTimes = ImmutableMap
        .builder();
    for (NodeArrivalTime arrivalTime : randomTrial.getNodeArrivalTimeList()) {
      try {
        nodeArrivalTimes.put(nodeIds.get(arrivalTime.getNodeId()),
            timeWriter.readTime(arrivalTime.getArrivalTime()));
      } catch (RuntimeException e) {
        System.out.println("error adding node arrival time: "
            + arrivalTime.toString());
        System.out.println("nodeIds:");
        System.out.println(nodeIds.toString());
        System.out.println("Arrival time list:");
        System.out.println(randomTrial.getNodeArrivalTimeList());
        throw e;
      }
    }
    return nodeArrivalTimes.build();

  }

  public MultiPeriodCyclePackingInputs<V, E, T> alternateArrivalTimes(
      RandomTrial randomTrial) {
    ImmutableMap<V, TimeInstant<T>> nodeArrivalTimes = createNodeArrivalTimes(randomTrial);
    // System.out.println("node arrival times: " + nodeArrivalTimes);
    ImmutableMap<E, TimeInstant<T>> edgeArrivalTimes = GraphUtil
        .inferEdgeArrivalTimes(nodeArrivalTimes,
            this.multiPeriodInputs.getGraph());
    MultiPeriodCyclePackingInputs<V, E, T> ans = new MultiPeriodCyclePackingInputs<V, E, T>(
        multiPeriodInputs.getGraph(), multiPeriodInputs.getRootNodes(),
        multiPeriodInputs.getTerminalNodes(), multiPeriodInputs.getStartTime(),
        multiPeriodInputs.getEndTime(), nodeArrivalTimes, edgeArrivalTimes,
        multiPeriodInputs.getTimeWriter());
    ans.setAuxiliaryInputStatistics(multiPeriodInputs
        .getAuxiliaryInputStatistics());
    return ans;
  }

}
