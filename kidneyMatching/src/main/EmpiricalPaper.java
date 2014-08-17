package main;

import inputOutput.Reports.ExchangeUnitOutputAttributeSet;
import inputOutput.aggregate.Agg;
import inputOutput.aggregate.ChainCycleAnalysis.ChainAttribute;
import inputOutput.aggregate.ChainCycleAnalysis.CycleAttribute;
import inputOutput.aggregate.ChainCycleAnalysis.TotalChainEdges;
import inputOutput.aggregate.ChainCycleAnalysis.TotalCycleEdges;
import inputOutput.aggregate.TrialReport;
import inputOutput.aggregate.TrialReport.NkrTrialReport;
import inputOutput.core.CsvFormatUtil;
import inputOutput.core.JodaTimeDifference;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import kepModeler.ChainsForcedRemainOpenOptions;
import kepModeler.ExchangeUnitAuxiliaryInputStatistics;
import kepModeler.KepModeler;
import kepModeler.MaximumWeightedPacking.PairMatchPowerBonus;
import kepModeler.MaximumWeightedPacking.PairMatchPowerThesholdBonus;
import kepModeler.ObjectiveMode;
import kepModeler.ObjectiveMode.ApacheStepFunction;
import kepModeler.ObjectiveMode.VpraWeightObjectiveMode;
import multiPeriod.CycleChainPackingFactory;
import multiPeriod.CycleChainPackingSubtourEliminationFactory;
import multiPeriod.MultiPeriodCyclePacking;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;
import multiPeriod.MultiPeriodCyclePackingIpSolver;
import multiPeriod.TimeInstant;

import org.apache.commons.math3.analysis.function.StepFunction;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;

import replicator.DataMultiPeriodConverter;
import replicator.DonorEdge;
import replicator.TimePeriodUtil;
import statistics.Queries;
import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.Range;

import data.DataCleaner;
import data.DataCleaner.ReasonToCleanDonor;
import data.DataCleaner.ReasonToCleanReceiver;
import data.DataCleaner.ReasonsInvalid;
import data.ExchangeUnit;
import data.ProblemData;

public class EmpiricalPaper {

  public static void main(String[] args) {
    experimentOne();
    // experimentOneRobust(false);
    // experimentOneRobust(true);
    // experimentTwo();
    // experimentTwoRobust();
    // experimentTwoVpra();
    // experimentThree();
    // experimentThreeChainsOpenLonger();
    // experimentCycleLength();
    // experimentOpenChain();
    // matchEveryNArrivals();
  }

  private static String outDir = "output" + File.separator + "empiricalPaper"
      + File.separator + "dataV3" + File.separator;

  private static String dateString = "20120524";// data to load
  private static DateTime start = new DateTime(2010, 5, 24, 0, 0);
  private static DateTime end = new DateTime(2012, 5, 24, 0, 0);
  private static int numThreads = 4;
  private static int maxChainLength = Integer.MAX_VALUE;
  private static int defaultMaxCycleLength = 3;
  private static int[] maxCycleLengths = new int[] { 0, 2, 3, 4 };

  private static List<ObjectiveMode> objectiveModes = new ArrayList<ObjectiveMode>();
  private static final ObjectiveMode tieBreaker;
  private static final ObjectiveMode moderate;
  private static final ObjectiveMode extreme;
  private static final ObjectiveMode lexicographic;

  private static final double[] pmpHistBuckets = new double[] { 0, 0.1, 5, 20,
      60, Double.POSITIVE_INFINITY };
  private static final double[] pmpObjectiveBuckets = Arrays.copyOfRange(
      pmpHistBuckets, 1, pmpHistBuckets.length - 1);
  private static final double[] vpraHistBuckets = new double[] { 0, 60, 80, 95,
      99.5, 100 };
  // private static final double[] vpraObjectiveBuckets =
  // Arrays.copyOfRange(vpraHistBuckets,1, vpraHistBuckets.length-1);
  private static final double[] vpraApacheObjectiveBuckets = Arrays
      .copyOfRange(vpraHistBuckets, 0, vpraHistBuckets.length - 1);

  static {
    {
      double cycleBonus = .01;
      double defaultEdgeWeight = 1;
      double[] thresholds = pmpObjectiveBuckets; // must be strictly increasing
                                                 // and nonnegative
      double[] bonuses = new double[] { .05, .01, .005, .001, .0005 }; // must
                                                                       // be
                                                                       // strictly
                                                                       // decreasing,
                                                                       // non-negative,
                                                                       // and of
                                                                       // one
                                                                       // longer
                                                                       // length
                                                                       // than
                                                                       // thresholds
      PairMatchPowerBonus pmpBonus = new PairMatchPowerThesholdBonus(
          thresholds, bonuses);
      tieBreaker = new ObjectiveMode.MaximumWeightedPackingMode(cycleBonus,
          defaultEdgeWeight, pmpBonus);
      tieBreaker.setName("tieBreaker");
      objectiveModes.add(tieBreaker);
    }
    {
      double cycleBonus = .01;
      double defaultEdgeWeight = 1;
      double[] thresholds = pmpObjectiveBuckets; // must be strictly increasing
                                                 // and nonnegative
      double[] bonuses = new double[] { 3, 1, .6, .1, .01 }; // must be strictly
                                                             // decreasing,
                                                             // non-negative,
                                                             // and of one
                                                             // longer length
                                                             // than thresholds
      PairMatchPowerBonus pmpBonus = new PairMatchPowerThesholdBonus(
          thresholds, bonuses);
      moderate = new ObjectiveMode.MaximumWeightedPackingMode(cycleBonus,
          defaultEdgeWeight, pmpBonus);
      moderate.setName("moderate");
      objectiveModes.add(moderate);
    }
    {
      double cycleBonus = .01;
      double defaultEdgeWeight = 1;
      double[] thresholds = pmpObjectiveBuckets; // must be strictly increasing
                                                 // and nonnegative
      double[] bonuses = new double[] { 10, 5, 3, 1, .01 }; // must be strictly
                                                            // decreasing,
                                                            // non-negative, and
                                                            // of one longer
                                                            // length than
                                                            // thresholds
      PairMatchPowerBonus pmpBonus = new PairMatchPowerThesholdBonus(
          thresholds, bonuses);
      extreme = new ObjectiveMode.MaximumWeightedPackingMode(cycleBonus,
          defaultEdgeWeight, pmpBonus);
      extreme.setName("extreme");
      objectiveModes.add(extreme);
    }
    {
      double cycleBonus = .01;
      double defaultEdgeWeight = 1;
      double[] thresholds = pmpObjectiveBuckets; // must be strictly increasing
                                                 // and nonnegative
      double[] bonuses = new double[] { 200, 50, 10, 3, .01 }; // must be
                                                               // strictly
                                                               // decreasing,
                                                               // non-negative,
                                                               // and of one
                                                               // longer length
                                                               // than
                                                               // thresholds
      PairMatchPowerBonus pmpBonus = new PairMatchPowerThesholdBonus(
          thresholds, bonuses);
      lexicographic = new ObjectiveMode.MaximumWeightedPackingMode(cycleBonus,
          defaultEdgeWeight, pmpBonus);
      lexicographic.setName("lexicographic");
      objectiveModes.add(lexicographic);
    }
  }
  private static final List<ObjectiveMode> vpraObjectiveModes = new ArrayList<ObjectiveMode>();
  private static final ObjectiveMode vpraTieBreaker;
  private static final ObjectiveMode vpraModerate;
  private static final ObjectiveMode vpraExtreme;
  private static final ObjectiveMode vpraLexicographic;
  static {
    {
      double[] x = vpraApacheObjectiveBuckets;
      double[] y = new double[] { .0005, .001, .005, .01, .05 };
      ApacheStepFunction stepFunction = new ApacheStepFunction(
          new StepFunction(x, y));
      vpraTieBreaker = new VpraWeightObjectiveMode(stepFunction);
      vpraTieBreaker.setName("vpraTieBreaker");
      vpraObjectiveModes.add(vpraTieBreaker);
    }
    {
      double[] x = vpraApacheObjectiveBuckets;
      double[] y = new double[] { .01, .1, .6, 1, 3 };
      ApacheStepFunction stepFunction = new ApacheStepFunction(
          new StepFunction(x, y));
      vpraModerate = new VpraWeightObjectiveMode(stepFunction);
      vpraModerate.setName("vpraModerate");
      vpraObjectiveModes.add(vpraModerate);
    }
    {
      double[] x = vpraApacheObjectiveBuckets;
      double[] y = new double[] { .01, 1, 3, 5, 10 };
      ApacheStepFunction stepFunction = new ApacheStepFunction(
          new StepFunction(x, y));
      vpraExtreme = new VpraWeightObjectiveMode(stepFunction);
      vpraExtreme.setName("vpraExtreme");
      vpraObjectiveModes.add(vpraExtreme);
    }
    {
      double[] x = vpraApacheObjectiveBuckets;
      double[] y = new double[] { .01, 3, 10, 50, 200 };
      ApacheStepFunction stepFunction = new ApacheStepFunction(
          new StepFunction(x, y));
      vpraLexicographic = new VpraWeightObjectiveMode(stepFunction);
      vpraLexicographic.setName("vpraLexicographic");
      vpraObjectiveModes.add(vpraLexicographic);
    }
  }

  private static List<ChainsForcedRemainOpenOptions> openChainOptions = new ArrayList<ChainsForcedRemainOpenOptions>();
  private static ChainsForcedRemainOpenOptions none;
  private static ChainsForcedRemainOpenOptions atLeastOne;
  private static ChainsForcedRemainOpenOptions atLeastOneGood;
  static {
    none = ChainsForcedRemainOpenOptions.none;
    none.setName("none");
    atLeastOne = new ChainsForcedRemainOpenOptions.AtLeastCountChains(1);
    atLeastOne.setName("atLeastOne");
    atLeastOneGood = new ChainsForcedRemainOpenOptions.AtLeastCountOfQualityChains(
        1, .2);
    atLeastOneGood.setName("atLeastOneGood");
    openChainOptions.add(none);
    openChainOptions.add(atLeastOne);
    openChainOptions.add(atLeastOneGood);
  }
  // we will run the simulation computing a matching every period, for each
  // period in the below list.
  private static List<Period> defaultPeriods = Arrays.asList(Period.days(1),
      Period.weeks(1), Period.weeks(2), Period.months(1), Period.months(3),
      Period.months(6), Period.years(1), Period.years(2));
  private static int[] defaultArrivalsBetweenMatches = new int[] { 1, 4, 8, 17,
      51, 103, 205 };

  private static MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> generateDefaultInputs() {
    ProblemData problemData = new ProblemData(dateString);
    Map<ExchangeUnit, ReasonsInvalid> removed = DataCleaner.cleanProblemData(
        problemData, EnumSet.allOf(ReasonToCleanDonor.class),
        EnumSet.allOf(ReasonToCleanReceiver.class));
    DataCleaner.assessHistoricMatching(problemData, removed);
    // the system begins in the preexisting state at time start, i.e. those that
    // arrive
    // before start but have not yet been matched are waiting.
    DataMultiPeriodConverter converterOld = new DataMultiPeriodConverter(
        problemData, start, end);
    // the system begins empty at time start, but then has the same arrivals as
    // above.
    // DataMultiPeriodConverter converterFresh = new
    // DataMultiPeriodConverter(problemData,start,end,true);
    MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = converterOld
        .getMultiPeriodPackingInputs();
    // contains pair match power info.
    ExchangeUnitAuxiliaryInputStatistics auxiliaryInput = Queries
        .getAuxiliaryStatisticsWithVPra(inputs);
    inputs.setAuxiliaryInputStatistics(auxiliaryInput);
    return inputs;
  }

  private static String expOneDir = outDir + "expOne" + File.separator;

  private static void experimentOne() {
    File dir = new File(expOneDir);
    dir.mkdirs();
    MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = generateDefaultInputs();
    Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
    Optional<Double> maxTime = Optional.<Double> absent();
    ObjectiveMode objectiveMode = tieBreaker;
    ChainsForcedRemainOpenOptions openChainOptions = none;
    boolean displayOutput = false;// kills cplex solver output

    KepModeler modeler = new KepModeler(maxChainLength, defaultMaxCycleLength,
        openChainOptions, objectiveMode);
    CycleChainPackingFactory<ExchangeUnit, DonorEdge> factory = new CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge>(
        modeler, displayOutput, maxTime, threadPool,
        inputs.getAuxiliaryInputStatistics());

    NkrTrialReport trailReport = new NkrTrialReport(true, true, true);
    trailReport
        .addAttribute(Agg
            .<ExchangeUnit, DonorEdge, ReadableInstant, Interval> pairedNodesMatched());
    trailReport.addAttribute(Agg
        .<ExchangeUnit, DonorEdge> pairedTotalWaitingTimeJoda());
    trailReport
        .addAttribute(Agg
            .<ExchangeUnit, DonorEdge, ReadableInstant, Interval> sensitizedPairedNodesMatched(Range
                .closed(0.0, 20.0)));
    trailReport.addAttribute(Agg
        .<ExchangeUnit, DonorEdge> sensitizedPairedTotalWaitingTimeJoda(Range
            .closed(Double.valueOf(0), Double.valueOf(20))));
    trailReport.addAttribute(Agg
        .<ExchangeUnit, DonorEdge> sensitizedPairedTotalWaitingTimeJoda(Range
            .greaterThan(Double.valueOf(20))));
    trailReport.addAttribute(Agg.vPraPairedNodesMatched(Range.closed(80.0,
        100.0)));
    trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range.closed(80.0,
        100.0)));
    trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range.closedOpen(
        0.0, 80.0)));
    NkrTrialReport chainCycleReport4 = new NkrTrialReport(true, true, false);
    chainCycleReport4.addAttribute(new CycleAttribute(2));
    chainCycleReport4.addAttribute(new CycleAttribute(3));
    chainCycleReport4.addAttribute(new CycleAttribute(4));
    chainCycleReport4.addAttribute(new ChainAttribute(Range.atMost(5)));
    chainCycleReport4.addAttribute(new ChainAttribute(Range.openClosed(5, 15)));
    chainCycleReport4.addAttribute(new ChainAttribute(Range.greaterThan(15)));
    NkrTrialReport chainCycleReport3 = new NkrTrialReport(true, true, false);
    chainCycleReport3.addAttribute(new TotalCycleEdges(Range.singleton(2)));
    chainCycleReport3.addAttribute(new TotalCycleEdges(Range.singleton(3)));
    chainCycleReport3.addAttribute(new TotalChainEdges(true, Range.atMost(5)));
    chainCycleReport3.addAttribute(new TotalChainEdges(true, Range.openClosed(
        5, 15)));
    chainCycleReport3.addAttribute(new TotalChainEdges(true, Range
        .greaterThan(15)));
    List<Period> periods = defaultPeriods;
    for (Period period : periods) {
      NkrTrialReport matchedHistorgramPmp = new NkrTrialReport(true, true,
          false);
      matchedHistorgramPmp
          .addAttributes(Agg
              .<ExchangeUnit, DonorEdge, ReadableInstant, Interval> pairMatchPowerHist(
                  "pmp", pmpHistBuckets, true));
      String matchedHistPmpName = expOneDir + period.toString()
          + "HistMatchedReport.csv";
      NkrTrialReport unMatchedHistorgram = new NkrTrialReport(true, true, false);
      unMatchedHistorgram
          .addAttributes(Agg
              .<ExchangeUnit, DonorEdge, ReadableInstant, Interval> pairMatchPowerHist(
                  "pmp", pmpHistBuckets, false));
      String unMatchedHistName = expOneDir + period.toString()
          + "HistUnMatchedReport.csv";

      NkrTrialReport matchedHistorgramVpra = new NkrTrialReport(true, true,
          false);
      matchedHistorgramVpra.addAttributes(Agg.virtualPraHist("vpra",
          vpraHistBuckets, true));
      String matchedHistVpraName = expOneDir + period.toString()
          + "HistMatchedReportVPra.csv";

      List<TimeInstant<ReadableInstant>> matchingTimes = TimePeriodUtil
          .generateScheduleTimeInstants(start, period, end, true);
      MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> packing = new MultiPeriodCyclePackingIpSolver<ExchangeUnit, DonorEdge, ReadableInstant>(
          inputs, matchingTimes, factory);
      packing.computeAllMatchings();
      String solutionName = period.toString();
      ExchangeUnitOutputAttributeSet attributeSet = new ExchangeUnitOutputAttributeSet(
          packing);
      trailReport.addOutcome(solutionName, attributeSet);
      chainCycleReport3.addOutcome(solutionName, attributeSet);
      chainCycleReport4.addOutcome(solutionName, attributeSet);
      matchedHistorgramPmp.addOutcome(solutionName, attributeSet);
      matchedHistorgramPmp.writeReport(matchedHistPmpName,
          CsvFormatUtil.pgfPlotTableFormat);
      unMatchedHistorgram.addOutcome(solutionName, attributeSet);
      unMatchedHistorgram.writeReport(unMatchedHistName,
          CsvFormatUtil.pgfPlotTableFormat);
      matchedHistorgramVpra.addOutcome(solutionName, attributeSet);
      matchedHistorgramVpra.writeReport(matchedHistVpraName,
          CsvFormatUtil.pgfPlotTableFormat);
    }
    trailReport.writeReport(expOneDir + "matchesAndWaitingTime.csv",
        CsvFormatUtil.pgfPlotTableFormat);
    chainCycleReport3.writeReport(expOneDir + "chainsAndCycles3.csv",
        CsvFormatUtil.pgfPlotTableFormat);
    chainCycleReport4.writeReport(expOneDir + "chainsAndCycles4.csv",
        CsvFormatUtil.pgfPlotTableFormat);
    factory.cleanUp();
    FixedThreadPool.shutDown(threadPool);

  }

  private static String expOneRobustDir = outDir + "expOneRobust"
      + File.separator;
  private static String expOneRobustNoShuffleDir = outDir
      + "expOneRobustNoShuffle" + File.separator;

  private static void experimentOneRobust(boolean shuffle) {
    Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
    Optional<Double> maxTime = Optional.of(60.0);
    String directory = shuffle ? expOneRobustDir : expOneRobustNoShuffleDir;
    File dir = new File(directory);
    dir.mkdirs();
    int numTrials = 50;
    List<MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant>> inputList = new ArrayList<MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant>>();
    for (int i = 0; i < numTrials; i++) {
      MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = generateDefaultInputs();
      if (shuffle && i != 0) {
        inputs.shuffleArrivalTimes(JodaTimeDifference.instance);
      }
      inputList.add(inputs);
    }
    List<Period> periods = defaultPeriods;
    List<ObjectiveMode> robustObjectiveModes = objectiveModes;// Arrays.asList(tieBreaker);
    for (ObjectiveMode objectiveMode : robustObjectiveModes) {
      for (Period period : periods) {

        TrialReport trailReport = new TrialReport(true, true, true);
        trailReport.addAttribute(Agg.pairedNodesMatched());
        trailReport.addAttribute(Agg.pairedTotalWaitingTimeJoda());
        trailReport.addAttribute(Agg.sensitizedPairedNodesMatched(Range.closed(
            0.0, 20.0)));
        trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
            .closed(Double.valueOf(0), Double.valueOf(20))));
        trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
            .greaterThan(Double.valueOf(20))));
        trailReport.addAttribute(Agg.vPraPairedNodesMatched(Range.closed(80.0,
            100.0)));
        trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range.closed(
            80.0, 100.0)));
        trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range
            .closedOpen(0.0, 80.0)));

        for (int i = 0; i < numTrials; i++) {
          MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = inputList
              .get(i);
          // ObjectiveMode objectiveMode = tieBreaker;
          ChainsForcedRemainOpenOptions openChainOptions = none;
          boolean displayOutput = false;// kills cplex solver output

          KepModeler modeler = new KepModeler(maxChainLength,
              defaultMaxCycleLength, openChainOptions, objectiveMode);
          CycleChainPackingFactory<ExchangeUnit, DonorEdge> factory = new CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge>(
              modeler, displayOutput, maxTime, threadPool,
              inputs.getAuxiliaryInputStatistics());
          List<TimeInstant<ReadableInstant>> matchingTimes = TimePeriodUtil
              .generateScheduleTimeInstants(start, period, end, true);
          MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> packing = new MultiPeriodCyclePackingIpSolver<ExchangeUnit, DonorEdge, ReadableInstant>(
              inputs, matchingTimes, factory);
          packing.computeAllMatchings();
          ExchangeUnitOutputAttributeSet attributeSet = new ExchangeUnitOutputAttributeSet(
              packing);
          trailReport.addOutcome("trial" + i, attributeSet);
          factory.cleanUp();

        }
        trailReport.writeReport(
            directory + objectiveMode.getName() + period.toString()
                + "-matchesAndWaitingTime.csv",
            CsvFormatUtil.pgfPlotTableFormat);
      }
    }
    FixedThreadPool.shutDown(threadPool);

  }

  /*
   * private static String expTwoModerateDir = outDir + "expTwo" +
   * File.separator; private static String expTwoExtremeDir = outDir +
   * "expTwoExtreme" + File.separator; private static String[] expTwoDirs = new
   * String[]{expTwoModerateDir,expTwoExtremeDir};
   */

  private static void experimentTwo() {

    for (ObjectiveMode objectiveMode : objectiveModes) {
      if (objectiveMode != tieBreaker) {
        String objDir = outDir + "expTwo" + objectiveMode.getName()
            + File.separator;
        File dir = new File(objDir);
        dir.mkdirs();
        MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = generateDefaultInputs();
        Optional<FixedThreadPool> threadPool = FixedThreadPool
            .makePool(numThreads);
        Optional<Double> maxTime = Optional.of(60.0);
        ChainsForcedRemainOpenOptions openChainOptions = none;
        boolean displayOutput = false;// kills cplex solver output

        KepModeler modeler = new KepModeler(maxChainLength,
            defaultMaxCycleLength, openChainOptions, objectiveMode);
        CycleChainPackingFactory<ExchangeUnit, DonorEdge> factory = new CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge>(
            modeler, displayOutput, maxTime, threadPool,
            inputs.getAuxiliaryInputStatistics());

        List<Period> periods = defaultPeriods;
        TrialReport trailReport = new TrialReport(true, true, true);
        trailReport.addAttribute(Agg.pairedNodesMatched());
        trailReport.addAttribute(Agg.pairedTotalWaitingTimeJoda());
        trailReport.addAttribute(Agg.sensitizedPairedNodesMatched(Range.closed(
            0.0, 20.0)));
        trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
            .closed(Double.valueOf(0), Double.valueOf(20))));
        trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
            .greaterThan(Double.valueOf(20))));
        trailReport.addAttribute(Agg.vPraPairedNodesMatched(Range.closed(80.0,
            100.0)));
        trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range.closed(
            80.0, 100.0)));
        trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range
            .closedOpen(0.0, 80.0)));
        for (Period period : periods) {
          List<TimeInstant<ReadableInstant>> matchingTimes = TimePeriodUtil
              .generateScheduleTimeInstants(start, period, end, true);
          MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> packing = new MultiPeriodCyclePackingIpSolver<ExchangeUnit, DonorEdge, ReadableInstant>(
              inputs, matchingTimes, factory);
          packing.computeAllMatchings();
          String solutionName = period.toString();
          ExchangeUnitOutputAttributeSet attributeSet = new ExchangeUnitOutputAttributeSet(
              packing);
          trailReport.addOutcome(solutionName, attributeSet);
        }
        trailReport.writeReport(objDir + "matchesAndWaitingTime.csv",
            CsvFormatUtil.pgfPlotTableFormat);
        factory.cleanUp();
        FixedThreadPool.shutDown(threadPool);
      }
    }

  }

  private static void experimentTwoRobust() {

    int numTrials = 20;
    List<MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant>> inputList = new ArrayList<MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant>>();
    for (int i = 0; i < numTrials; i++) {
      MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = generateDefaultInputs();
      inputs.shuffleArrivalTimes(JodaTimeDifference.instance);
      inputList.add(inputs);
    }
    for (ObjectiveMode objectiveMode : objectiveModes) {
      String objDir = outDir + "expTwoRobust" + objectiveMode.getName()
          + File.separator;
      File dir = new File(objDir);
      dir.mkdirs();
      TrialReport trailReport = new TrialReport(true, true, true);
      trailReport.addAttribute(Agg.pairedNodesMatched());
      trailReport.addAttribute(Agg.sensitizedPairedNodesMatched(Range.closed(
          0.0, 20.0)));

      for (int i = 0; i < numTrials; i++) {
        MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = inputList
            .get(i);
        Optional<FixedThreadPool> threadPool = FixedThreadPool
            .makePool(numThreads);
        Optional<Double> maxTime = Optional.of(60.0);
        ChainsForcedRemainOpenOptions openChainOptions = none;
        boolean displayOutput = false;// kills cplex solver output
        KepModeler modeler = new KepModeler(maxChainLength,
            defaultMaxCycleLength, openChainOptions, objectiveMode);
        CycleChainPackingFactory<ExchangeUnit, DonorEdge> factory = new CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge>(
            modeler, displayOutput, maxTime, threadPool,
            inputs.getAuxiliaryInputStatistics());
        Period period = defaultPeriods.get(0);
        List<TimeInstant<ReadableInstant>> matchingTimes = TimePeriodUtil
            .generateScheduleTimeInstants(start, period, end, true);
        MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> packing = new MultiPeriodCyclePackingIpSolver<ExchangeUnit, DonorEdge, ReadableInstant>(
            inputs, matchingTimes, factory);
        packing.computeAllMatchings();
        String solutionName = "trail" + i;
        ExchangeUnitOutputAttributeSet attributeSet = new ExchangeUnitOutputAttributeSet(
            packing);
        trailReport.addOutcome(solutionName, attributeSet);

        factory.cleanUp();
        FixedThreadPool.shutDown(threadPool);
      }
      trailReport.writeReport(objDir + "matchesAndWaitingTime.csv",
          CsvFormatUtil.pgfPlotTableFormat);
    }

  }

  private static void experimentTwoVpra() {

    for (ObjectiveMode objectiveMode : vpraObjectiveModes) {

      String objDir = outDir + "expTwo" + objectiveMode.getName()
          + File.separator;
      File dir = new File(objDir);
      dir.mkdirs();
      MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = generateDefaultInputs();

      Optional<FixedThreadPool> threadPool = FixedThreadPool
          .makePool(numThreads);
      Optional<Double> maxTime = Optional.of(60.0);
      ChainsForcedRemainOpenOptions openChainOptions = none;
      boolean displayOutput = false;// kills cplex solver output

      KepModeler modeler = new KepModeler(maxChainLength,
          defaultMaxCycleLength, openChainOptions, objectiveMode);
      CycleChainPackingFactory<ExchangeUnit, DonorEdge> factory = new CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge>(
          modeler, displayOutput, maxTime, threadPool,
          inputs.getAuxiliaryInputStatistics());

      List<Period> periods = defaultPeriods;
      TrialReport trailReport = new TrialReport(true, true, true);
      trailReport.addAttribute(Agg.pairedNodesMatched());
      trailReport.addAttribute(Agg.pairedTotalWaitingTimeJoda());
      trailReport.addAttribute(Agg.sensitizedPairedNodesMatched(Range.closed(
          0.0, 20.0)));
      trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
          .closed(Double.valueOf(0), Double.valueOf(20))));
      trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
          .greaterThan(Double.valueOf(20))));
      trailReport.addAttribute(Agg.vPraPairedNodesMatched(Range.closed(80.0,
          100.0)));
      trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range.closed(
          80.0, 100.0)));
      trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range.closedOpen(
          0.0, 80.0)));

      for (Period period : periods) {
        List<TimeInstant<ReadableInstant>> matchingTimes = TimePeriodUtil
            .generateScheduleTimeInstants(start, period, end, true);
        MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> packing = new MultiPeriodCyclePackingIpSolver<ExchangeUnit, DonorEdge, ReadableInstant>(
            inputs, matchingTimes, factory);
        packing.computeAllMatchings();
        String solutionName = period.toString();
        ExchangeUnitOutputAttributeSet attributeSet = new ExchangeUnitOutputAttributeSet(
            packing);
        trailReport.addOutcome(solutionName, attributeSet);
      }
      trailReport.writeReport(objDir + "matchesAndWaitingTime.csv",
          CsvFormatUtil.pgfPlotTableFormat);
      factory.cleanUp();
      FixedThreadPool.shutDown(threadPool);
    }
  }

  private static String expThreeDirBase = outDir + "expThree";
  private static String[] expThreeDirs = new String[] {
      expThreeDirBase + "25" + File.separator,
      expThreeDirBase + "50" + File.separator,
      expThreeDirBase + "75" + File.separator,
      expThreeDirBase + "100" + File.separator };

  private static void experimentThree() {

    double[] deletionProbabilities = new double[] { .25, .5, .75, 1 };

    for (int deleteIndex = 0; deleteIndex < deletionProbabilities.length; deleteIndex++) {
      String deletionDirName = expThreeDirs[deleteIndex];
      File dir = new File(deletionDirName);
      dir.mkdirs();
      double deletionProbability = deletionProbabilities[deleteIndex];

      MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = generateDefaultInputs();
      Optional<FixedThreadPool> threadPool = FixedThreadPool
          .makePool(numThreads);
      Optional<Double> maxTime = Optional.of(60.0);
      boolean displayOutput = false;// kills cplex solver output

      inputs.killRootsRandom(deletionProbability);

      KepModeler modeler = new KepModeler(maxChainLength,
          defaultMaxCycleLength, none, tieBreaker);
      CycleChainPackingFactory<ExchangeUnit, DonorEdge> factory = new CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge>(
          modeler, displayOutput, maxTime, threadPool,
          inputs.getAuxiliaryInputStatistics());

      List<Period> periods = defaultPeriods;
      NkrTrialReport trailReport = new NkrTrialReport(true, true, true);
      trailReport
          .addAttribute(Agg
              .<ExchangeUnit, DonorEdge, ReadableInstant, Interval> pairedNodesMatched());
      trailReport.addAttribute(Agg
          .<ExchangeUnit, DonorEdge> pairedTotalWaitingTimeJoda());
      trailReport
          .addAttribute(Agg
              .<ExchangeUnit, DonorEdge, ReadableInstant, Interval> sensitizedPairedNodesMatched(Range
                  .closed(0.0, 20.0)));
      trailReport.addAttribute(Agg
          .<ExchangeUnit, DonorEdge> sensitizedPairedTotalWaitingTimeJoda(Range
              .closed(Double.valueOf(0), Double.valueOf(20))));
      trailReport.addAttribute(Agg
          .<ExchangeUnit, DonorEdge> sensitizedPairedTotalWaitingTimeJoda(Range
              .greaterThan(Double.valueOf(20))));
      trailReport.addAttribute(Agg.vPraPairedNodesMatched(Range.closed(80.0,
          100.0)));
      trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range.closed(
          80.0, 100.0)));
      trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range.closedOpen(
          0.0, 80.0)));

      NkrTrialReport chainCycleReport = new NkrTrialReport(true, true, false);
      chainCycleReport.addAttribute(new CycleAttribute(2));
      chainCycleReport.addAttribute(new CycleAttribute(3));
      chainCycleReport.addAttribute(new ChainAttribute(Range.atMost(5)));
      chainCycleReport
          .addAttribute(new ChainAttribute(Range.openClosed(5, 15)));
      chainCycleReport.addAttribute(new ChainAttribute(Range.greaterThan(15)));

      for (Period period : periods) {

        List<TimeInstant<ReadableInstant>> matchingTimes = TimePeriodUtil
            .generateScheduleTimeInstants(start, period, end, true);
        MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> packing = new MultiPeriodCyclePackingIpSolver<ExchangeUnit, DonorEdge, ReadableInstant>(
            inputs, matchingTimes, factory);
        packing.computeAllMatchings();
        String solutionName = period.toString();
        ExchangeUnitOutputAttributeSet attributeSet = new ExchangeUnitOutputAttributeSet(
            packing);
        trailReport.addOutcome(solutionName, attributeSet);
        chainCycleReport.addOutcome(solutionName, attributeSet);

      }
      trailReport.writeReport(deletionDirName + "matchesAndWaitingTime.csv",
          CsvFormatUtil.pgfPlotTableFormat);
      chainCycleReport.writeReport(deletionDirName + "chainsAndCycles.csv",
          CsvFormatUtil.pgfPlotTableFormat);
      factory.cleanUp();
      FixedThreadPool.shutDown(threadPool);
    }

  }

  private static void experimentThreeChainsOpenLonger() {
    for (ChainsForcedRemainOpenOptions openChainOption : openChainOptions) {
      if (openChainOption != none) {
        String openDirName = expThreeDirBase + "75-"
            + openChainOption.getName() + File.separator;

        File dir = new File(openDirName);
        dir.mkdirs();
        double deletionProbability = .75;
        MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = generateDefaultInputs();
        inputs.killRootsRandom(deletionProbability);

        Optional<FixedThreadPool> threadPool = FixedThreadPool
            .makePool(numThreads);
        Optional<Double> maxTime = Optional.of(60.0);
        boolean displayOutput = false;// kills cplex solver output

        KepModeler modeler = new KepModeler(maxChainLength,
            defaultMaxCycleLength, openChainOption, tieBreaker);
        CycleChainPackingFactory<ExchangeUnit, DonorEdge> factory = new CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge>(
            modeler, displayOutput, maxTime, threadPool,
            inputs.getAuxiliaryInputStatistics());

        List<Period> periods = defaultPeriods;
        TrialReport trailReport = new TrialReport(true, true, true);
        trailReport.addAttribute(Agg.pairedNodesMatched());
        trailReport.addAttribute(Agg.pairedTotalWaitingTimeJoda());
        trailReport.addAttribute(Agg.sensitizedPairedNodesMatched(Range.closed(
            0.0, 20.0)));
        trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
            .closed(Double.valueOf(0), Double.valueOf(20))));
        trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
            .greaterThan(Double.valueOf(20))));
        trailReport.addAttribute(Agg.vPraPairedNodesMatched(Range.closed(80.0,
            100.0)));
        trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range.closed(
            80.0, 100.0)));
        trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range
            .closedOpen(0.0, 80.0)));
        TrialReport chainCycleReport = new TrialReport(true, true, false);
        chainCycleReport.addAttribute(new CycleAttribute(2));
        chainCycleReport.addAttribute(new CycleAttribute(3));
        chainCycleReport.addAttribute(new ChainAttribute(Range.atMost(5)));
        chainCycleReport.addAttribute(new ChainAttribute(Range
            .openClosed(5, 15)));
        chainCycleReport
            .addAttribute(new ChainAttribute(Range.greaterThan(15)));

        for (Period period : periods) {

          List<TimeInstant<ReadableInstant>> matchingTimes = TimePeriodUtil
              .generateScheduleTimeInstants(start, period, end, true);
          MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> packing = new MultiPeriodCyclePackingIpSolver<ExchangeUnit, DonorEdge, ReadableInstant>(
              inputs, matchingTimes, factory);
          packing.computeAllMatchings();
          String solutionName = period.toString();
          ExchangeUnitOutputAttributeSet attributeSet = new ExchangeUnitOutputAttributeSet(
              packing);
          trailReport.addOutcome(solutionName, attributeSet);
          chainCycleReport.addOutcome(solutionName, attributeSet);
        }
        trailReport.writeReport(openDirName + "matchesAndWaitingTime.csv",
            CsvFormatUtil.pgfPlotTableFormat);
        chainCycleReport.writeReport(openDirName + "chainsAndCycles.csv",
            CsvFormatUtil.pgfPlotTableFormat);
        factory.cleanUp();
        FixedThreadPool.shutDown(threadPool);
      }
    }

  }

  // cycle length
  private static void experimentCycleLength() {

    for (int maxCycleLength : maxCycleLengths) {
      if (maxCycleLength != defaultMaxCycleLength) {
        String cycleDirName = outDir + "cycleLength" + maxCycleLength
            + File.separator;
        File dir = new File(cycleDirName);
        dir.mkdirs();
        MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = generateDefaultInputs();
        Optional<FixedThreadPool> threadPool = FixedThreadPool
            .makePool(numThreads);
        Optional<Double> maxTime = Optional.of(60.0);
        boolean displayOutput = false;// kills cplex solver output
        KepModeler modeler = new KepModeler(maxChainLength, maxCycleLength,
            none, tieBreaker);
        CycleChainPackingFactory<ExchangeUnit, DonorEdge> factory = new CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge>(
            modeler, displayOutput, maxTime, threadPool,
            inputs.getAuxiliaryInputStatistics());

        List<Period> periods = defaultPeriods;
        TrialReport trailReport = new TrialReport(true, true, true);
        trailReport.addAttribute(Agg.pairedNodesMatched());
        trailReport.addAttribute(Agg.pairedTotalWaitingTimeJoda());
        trailReport.addAttribute(Agg.sensitizedPairedNodesMatched(Range.closed(
            0.0, 20.0)));
        trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
            .atMost(Double.valueOf(20))));
        TrialReport chainCycleReport = new TrialReport(true, true, false);
        chainCycleReport.addAttribute(new CycleAttribute(2));
        chainCycleReport.addAttribute(new CycleAttribute(3));
        chainCycleReport.addAttribute(new CycleAttribute(4));
        chainCycleReport.addAttribute(new ChainAttribute(Range.atMost(5)));
        chainCycleReport.addAttribute(new ChainAttribute(Range
            .openClosed(5, 15)));
        chainCycleReport
            .addAttribute(new ChainAttribute(Range.greaterThan(15)));
        for (Period period : periods) {
          List<TimeInstant<ReadableInstant>> matchingTimes = TimePeriodUtil
              .generateScheduleTimeInstants(start, period, end, true);
          MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> packing = new MultiPeriodCyclePackingIpSolver<ExchangeUnit, DonorEdge, ReadableInstant>(
              inputs, matchingTimes, factory);
          packing.computeAllMatchings();
          String solutionName = period.toString();
          ExchangeUnitOutputAttributeSet attributeSet = new ExchangeUnitOutputAttributeSet(
              packing);
          trailReport.addOutcome(solutionName, attributeSet);
          chainCycleReport.addOutcome(solutionName, attributeSet);
        }
        trailReport.writeReport(cycleDirName + "matchesAndWaitingTime.csv",
            CsvFormatUtil.pgfPlotTableFormat);
        chainCycleReport.writeReport(cycleDirName + "chainsAndCycles.csv",
            CsvFormatUtil.pgfPlotTableFormat);
        factory.cleanUp();
        FixedThreadPool.shutDown(threadPool);
      }
    }

  }

  private static void experimentOpenChain() {

    for (ChainsForcedRemainOpenOptions openChainOption : openChainOptions) {

      String openDirName = outDir + openChainOption.getName() + File.separator;
      File dir = new File(openDirName);
      dir.mkdirs();
      MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = generateDefaultInputs();

      Optional<FixedThreadPool> threadPool = FixedThreadPool
          .makePool(numThreads);
      Optional<Double> maxTime = Optional.of(60.0);

      boolean displayOutput = false;// kills cplex solver output

      KepModeler modeler = new KepModeler(maxChainLength,
          defaultMaxCycleLength, openChainOption, tieBreaker);
      CycleChainPackingFactory<ExchangeUnit, DonorEdge> factory = new CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge>(
          modeler, displayOutput, maxTime, threadPool,
          inputs.getAuxiliaryInputStatistics());

      List<Period> periods = defaultPeriods;
      TrialReport trailReport = new TrialReport(true, true, true);
      trailReport.addAttribute(Agg.pairedNodesMatched());
      trailReport.addAttribute(Agg.pairedTotalWaitingTimeJoda());
      trailReport.addAttribute(Agg.sensitizedPairedNodesMatched(Range.closed(
          0.0, 20.0)));
      trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
          .atMost(Double.valueOf(20))));
      trailReport.addAttribute(Agg.pairedNodesInSimulationArrivedAfterStart());
      TrialReport chainCycleReport = new TrialReport(true, true, false);
      chainCycleReport.addAttribute(new TotalCycleEdges(Range.singleton(2)));
      chainCycleReport.addAttribute(new TotalCycleEdges(Range.singleton(3)));
      chainCycleReport.addAttribute(new TotalChainEdges(true, Range.atMost(5)));
      chainCycleReport.addAttribute(new TotalChainEdges(true, Range.openClosed(
          5, 15)));
      chainCycleReport.addAttribute(new TotalChainEdges(true, Range
          .greaterThan(15)));
      /*
       * chainCycleReport.addAttribute(new CycleAttribute(2));
       * chainCycleReport.addAttribute(new CycleAttribute(3));
       * chainCycleReport.addAttribute(new ChainAttribute(Range.atMost(5)));
       * chainCycleReport.addAttribute(new
       * ChainAttribute(Range.openClosed(5,15)));
       * chainCycleReport.addAttribute(new
       * ChainAttribute(Range.greaterThan(15)));
       */
      for (Period period : periods) {
        List<TimeInstant<ReadableInstant>> matchingTimes = TimePeriodUtil
            .generateScheduleTimeInstants(start, period, end, true);
        MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> packing = new MultiPeriodCyclePackingIpSolver<ExchangeUnit, DonorEdge, ReadableInstant>(
            inputs, matchingTimes, factory);
        packing.computeAllMatchings();
        String solutionName = period.toString();
        ExchangeUnitOutputAttributeSet attributeSet = new ExchangeUnitOutputAttributeSet(
            packing);
        trailReport.addOutcome(solutionName, attributeSet);
        chainCycleReport.addOutcome(solutionName, attributeSet);
      }
      trailReport.writeReport(openDirName + "matchesAndWaitingTime.csv",
          CsvFormatUtil.pgfPlotTableFormat);
      chainCycleReport.writeReport(openDirName + "chainsAndCycles.csv",
          CsvFormatUtil.pgfPlotTableFormat);
      factory.cleanUp();
      FixedThreadPool.shutDown(threadPool);
    }

  }

  private static String everyNDir = outDir + "everyNarrivals" + File.separator;

  private static void matchEveryNArrivals() {
    File dir = new File(everyNDir);
    dir.mkdirs();
    MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = generateDefaultInputs();
    Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
    Optional<Double> maxTime = Optional.of(60.0);
    ObjectiveMode objectiveMode = tieBreaker;
    ChainsForcedRemainOpenOptions openChainOptions = none;
    boolean displayOutput = false;// kills cplex solver output

    KepModeler modeler = new KepModeler(maxChainLength, defaultMaxCycleLength,
        openChainOptions, objectiveMode);
    CycleChainPackingFactory<ExchangeUnit, DonorEdge> factory = new CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge>(
        modeler, displayOutput, maxTime, threadPool,
        inputs.getAuxiliaryInputStatistics());

    TrialReport trailReport = new TrialReport(true, true, true);
    trailReport.addAttribute(Agg.pairedNodesMatched());
    trailReport.addAttribute(Agg.pairedTotalWaitingTimeJoda());
    trailReport.addAttribute(Agg.sensitizedPairedNodesMatched(Range.closed(0.0,
        20.0)));
    trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
        .closed(Double.valueOf(0), Double.valueOf(20))));
    trailReport.addAttribute(Agg.sensitizedPairedTotalWaitingTimeJoda(Range
        .greaterThan(Double.valueOf(20))));
    trailReport.addAttribute(Agg.vPraPairedNodesMatched(Range.closed(80.0,
        100.0)));
    trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range.closed(80.0,
        100.0)));
    trailReport.addAttribute(Agg.vpraPairedTotalWaitingTime(Range.closedOpen(
        0.0, 80.0)));

    int[] arrivalsPerMatching = new int[] { 1, 4, 8, 17, 51, 103, 205, 5, 10,
        20, 30, 50, 100, 200 };// EmpiricalPaper.defaultArrivalsBetweenMatches;
    for (int arrivals : arrivalsPerMatching) {
      TrialReport matchedHistorgramPmp = new TrialReport(true, true, false);
      matchedHistorgramPmp.addAttributes(Agg.pairMatchPowerHist("pmp",
          pmpHistBuckets, true));
      String matchedHistPmpName = everyNDir + arrivals
          + "HistMatchedReport.csv";
      TrialReport unMatchedHistorgram = new TrialReport(true, true, false);
      unMatchedHistorgram.addAttributes(Agg.pairMatchPowerHist("pmp",
          pmpHistBuckets, false));
      String unMatchedHistName = everyNDir + arrivals
          + "HistUnMatchedReport.csv";

      TrialReport matchedHistorgramVpra = new TrialReport(true, true, false);
      matchedHistorgramVpra.addAttributes(Agg.virtualPraHist("vpra",
          vpraHistBuckets, true));
      String matchedHistVpraName = everyNDir + arrivals
          + "HistMatchedReportVPra.csv";

      MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> packing = new MultiPeriodCyclePackingIpSolver<ExchangeUnit, DonorEdge, ReadableInstant>(
          inputs, arrivals, factory);
      packing.computeAllMatchings();
      String solutionName = "" + arrivals;
      ExchangeUnitOutputAttributeSet attributeSet = new ExchangeUnitOutputAttributeSet(
          packing);
      trailReport.addOutcome(solutionName, attributeSet);
      matchedHistorgramPmp.addOutcome(solutionName, attributeSet);
      matchedHistorgramPmp.writeReport(matchedHistPmpName,
          CsvFormatUtil.pgfPlotTableFormat);
      unMatchedHistorgram.addOutcome(solutionName, attributeSet);
      unMatchedHistorgram.writeReport(unMatchedHistName,
          CsvFormatUtil.pgfPlotTableFormat);
      matchedHistorgramVpra.addOutcome(solutionName, attributeSet);
      matchedHistorgramVpra.writeReport(matchedHistVpraName,
          CsvFormatUtil.pgfPlotTableFormat);
    }
    trailReport.writeReport(everyNDir + "matchesAndWaitingTime.csv",
        CsvFormatUtil.pgfPlotTableFormat);
    factory.cleanUp();
    FixedThreadPool.shutDown(threadPool);

  }

}
