package main;

import inputOutput.Reports.UnosOutputAttributeSet;
import inputOutput.aggregate.Agg;
import inputOutput.aggregate.AggregateTrialReport.UnosAggregateTrialReport;
import inputOutput.aggregate.ChainCycleAnalysis.TotalChainEdges;
import inputOutput.aggregate.ChainCycleAnalysis.TotalCycleEdges;
import inputOutput.aggregate.TrialReport;
import inputOutput.aggregate.TrialReport.NumericTrialAttribute;
import inputOutput.aggregate.TrialReport.UnosTrialOutcome;
import inputOutput.aggregate.UnosAgg;
import inputOutput.aggregate.UnosAgg.UnosCountPairedMatchedPatients;
import inputOutput.core.DoubleTimeDifferenceCalculator;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepProtos.KepProtos;
import kepProtos.KepProtos.EdgePredicate;
import kepProtos.KepProtos.EdgeStep;
import kepProtos.KepProtos.KepModelerInputs;
import kepProtos.KepProtos.MatchingMode;
import kepProtos.KepProtos.NeighborhoodPredicate;
import kepProtos.KepProtos.ObjectiveFunction;
import kepProtos.KepProtos.ObjectiveMetric;
import kepProtos.KepProtos.PrioritizationLevel;
import kepProtos.KepProtos.Relation;
import kepProtos.KepProtos.Simulation;
import kepProtos.KepProtos.SimulationInputs;
import main.UnosAnalysisCLI.ExecutionMode;
import multiPeriod.DynamicMatching;
import multiPeriodAnalysis.DrmaaExperimentRunner;
import multiPeriodAnalysis.Environment;
import multiPeriodAnalysis.ExperimentRunner;
import multiPeriodAnalysis.LocalExperimentRunner;
import multiPeriodAnalysis.ResultCache;

import org.apache.commons.csv.CSVFormat;

import protoModeler.ProtoObjectives;
import protoModeler.ProtoObjectives.StepFunctionBuilder;
import protoModeler.ProtoUtil;
import protoModeler.UnosPredicateBuilder.UnosHistoricData;
import protoModeler.UnosProtoObjectives;
import protoModeler.UnosProtoObjectives.ObjFactor;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;
import cern.colt.Arrays;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class UnosAnalysis {

  /**
   * @param args
   */
  public static void main(String[] args) {
    UnosAnalysisCLI unosCli = new UnosAnalysisCLI();
    new JCommander(unosCli, args);
    if (unosCli.getExecutionMode() == ExecutionMode.DRMAA_WORKER) {
      Environment<UnosExchangeUnit, UnosDonorEdge, Double> environment = Environment
          .unosEnvironment(unosCli.getThreads(), unosCli.getReplications());
      LocalExperimentRunner<UnosExchangeUnit, UnosDonorEdge, Double> runner = new LocalExperimentRunner<UnosExchangeUnit, UnosDonorEdge, Double>(
          environment);

      Simulation simulation = runner.runSimulationJob(runner.safeRead(unosCli
          .getSimulationJob()));
      environment.shutDown();
      runner.terminate();
      try {
        Files.write(simulation.toByteArray(),
            new File(unosCli.getOutputSimulation()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      Environment<UnosExchangeUnit, UnosDonorEdge, Double> environment = Environment
          .unosEnvironment(unosCli.getThreads(), unosCli.getReplications());
      ExperimentRunner<UnosExchangeUnit, UnosDonorEdge, Double> runner;
      if (unosCli.getExecutionMode() == ExecutionMode.LOCAL) {
        runner = new LocalExperimentRunner<UnosExchangeUnit, UnosDonorEdge, Double>(
            environment);
      } else if (unosCli.getExecutionMode() == ExecutionMode.DRMAA_MASTER) {
        runner = new DrmaaExperimentRunner<UnosExchangeUnit, UnosDonorEdge, Double>(
            environment);
      } else {
        throw new RuntimeException("Unexpected execution mode: "
            + unosCli.getExecutionMode());
      }
      UnosAnalysis analysis = new UnosAnalysis(environment, runner);
      Set<String> experimentNames = Sets.newHashSet(unosCli.getExperiments());
      analysis.executeExperiments(experimentNames);
      analysis.environment.shutDown();
      analysis.experimentRunner.terminate();
    }

  }

  private void executeExperiments(Set<String> experimentNames) {
    if (experimentNames.size() > 0) {
      if (experimentNames.contains("hardToMatch")) {
        experimentPrioritizeHardToMatch();
      }
      if (experimentNames.contains("basic")) {
        experimentBasic();
      }
      if (experimentNames.contains("deleteUnos")) {
        experimentDeleteUnosCriteria();
      }
      if (experimentNames.contains("waiting")) {
        experimentWaiting();
      }
      if (experimentNames.contains("protoObj")) {
        experimentProtoObjPriority();
      }
      if (experimentNames.contains("priority")) {
        experimentPriority();
      }
      if (experimentNames.contains("chainLength")) {
        experimentChainLength();
      }
      if (experimentNames.contains("matchFreq")) {
        experimentMatchingFrequency();
      }
      if (experimentNames.contains("noCycles")) {
        experimentNoCycles();
      }
      if (experimentNames.contains("unosPraSensitivity")) {
        experimentUnosPraBonusSensitivity();
      }
      if (experimentNames.contains("patientPraObjectives")) {
        experimentPatientPraObjectives();
      }
      if (experimentNames.contains("singlePraThreshold")) {
        experimentSinglePraThreshold();
      }
      if (experimentNames.contains("praVsWaiting")) {
        experimentPraVsWaitingTime();
      }
      if (experimentNames.contains("prioritizeByPowerShort")) {
        experimentPrioritizeByPowerShort();
      }
      if (experimentNames.contains("negativeWeights")) {
        experimentNegativeWeights();
      }
    } else {
      experimentBasic();
      // analysis.experimentPrioritizeHardToMatch();
      // analysis.experimentDeleteUnosCriteria();
      // analysis.experimentProtoObjPriority();
      // analysis.experimentPriority();
      // analysis.experimentChainLength();
      // analysis.experimentMatchingFrequency();
      // analysis.experimentNoCycles();
    }
  }

  private static int maxChainLength = Integer.MAX_VALUE;
  private static int defaultMaxCycleLength = 3;

  private static int defaultNumThreads = 4;
  private static int defaultNumReps = 2;
  private static String defaultRandomTrialCache = "output" + File.separator
      + "unosCache" + File.separator + "randomTrials" + File.separator;
  private static String defaultSimulationsCache = "output" + File.separator
      + "unosCache" + File.separator + "simulations" + File.separator;
  private static String defaultOutput = "output" + File.separator + "unosOut"
      + File.separator;

  // private UnosData unosData;
  private Environment<UnosExchangeUnit, UnosDonorEdge, Double> environment;
  private ResultCache<UnosExchangeUnit, UnosDonorEdge, Double> resultCache;
  private ExperimentRunner<UnosExchangeUnit, UnosDonorEdge, Double> experimentRunner;

  public UnosAnalysis(
      Environment<UnosExchangeUnit, UnosDonorEdge, Double> environment,
      ExperimentRunner<UnosExchangeUnit, UnosDonorEdge, Double> experimentRunner) {
    this.environment = environment;
    this.experimentRunner = experimentRunner;
    resultCache = new ResultCache<UnosExchangeUnit, UnosDonorEdge, Double>(
        environment, defaultRandomTrialCache, defaultSimulationsCache,
        experimentRunner);
  }

  private static SimulationInputs.Builder defaultInputBuilderWithObjective(
      double simulationStartTime, double simulationEndTime) {
    SimulationInputs.Builder inputBuilder = defaultInputBuilder(
        simulationStartTime, simulationEndTime);
    inputBuilder.getKepModelerInputsBuilder().getObjectiveBuilder()
        .setMetric(ObjectiveMetric.PAIR_MATCH_POWER)
        .setPrioritization(PrioritizationLevel.MODERATE);
    return inputBuilder;
  }

  private static SimulationInputs.Builder defaultInputBuilder(
      double simulationStartTime, double simulationEndTime) {
    SimulationInputs.Builder inputBuilder = SimulationInputs.newBuilder()
        .setMatchingMode(MatchingMode.DAYS).setMatchingFrequency(7)
        .setSimulationStartTime(simulationStartTime)
        .setSimulationEndTime(simulationEndTime);
    inputBuilder.getKepModelerInputsBuilder()
        .setMaxChainLength(Integer.MAX_VALUE).setMaxCycleLength(3)
        .setCycleBonus(.01);
    return inputBuilder;

  }

  private static <V, E> List<NumericTrialAttribute<V, E, Double, Double>> donorPatientPowerShortAgg() {
    return donorPatientPowerAgg(patientPowerBucketsShort,
        donorPowerBucketsShort);
  }

  private static <V, E> List<NumericTrialAttribute<V, E, Double, Double>> donorPatientPowerLongAgg() {
    return donorPatientPowerAgg(patientPowerBucketsFull, donorPowerBucketsFull);
  }

  private static <V, E> List<NumericTrialAttribute<V, E, Double, Double>> donorPatientPowerAgg(
      List<KepProtos.Range> patientPowerBuckets,
      List<KepProtos.Range> donorPowerRanges) {
    List<NumericTrialAttribute<V, E, Double, Double>> agg = Lists
        .newArrayList();
    for (Range<Double> patientPower : ProtoUtil
        .createRanges(patientPowerBuckets)) {
      agg.add(Agg
          .<V, E, Double, Double> patientPowerPairedNodesMatched(patientPower));
      agg.add(Agg
          .<V, E> patientPowerPairedTotalWaitingTimeDoubleTime(patientPower));
    }
    for (Range<Double> donorPower : ProtoUtil.createRanges(donorPowerRanges)) {
      agg.add(Agg
          .<V, E, Double, Double> donorPowerPairedNodesMatched(donorPower));
    }
    return agg;
  }

  private static <V, E> List<NumericTrialAttribute<V, E, Double, Double>> pmpAgg() {
    List<NumericTrialAttribute<V, E, Double, Double>> agg = Lists
        .newArrayList();
    agg.add(Agg.<V, E, Double, Double> sensitizedPairedNodesMatched(Range
        .closed(0.0, 20.0)));
    agg.add(Agg.<V, E> sensitizedPairedTotalWaitingTimeDoubleTime(Range.closed(
        0.0, 20.0)));
    agg.add(Agg.<V, E, Double, Double> sensitizedPairedNodesMatched(Range
        .greaterThan(20.0)));
    agg.add(Agg.<V, E> sensitizedPairedTotalWaitingTimeDoubleTime(Range
        .greaterThan(20.0)));
    return agg;
  }

  private static <V, E> List<NumericTrialAttribute<V, E, Double, Double>> basicAgg() {
    List<NumericTrialAttribute<V, E, Double, Double>> agg = Lists
        .newArrayList();
    agg.add(Agg.<V, E, Double, Double> pairedNodesMatched());
    agg.add(Agg.<V, E> pairedTotalWaitingTimeDoubleTime());
    agg.addAll(UnosAnalysis.<V, E> pmpAgg());
    return agg;
  }

  private static List<UnosCountPairedMatchedPatients> aggDerivedPraProtos(
      List<KepProtos.Range> protoRanges) {
    return aggDerivedPra(ProtoUtil.createRanges(protoRanges));
  }

  private static List<UnosCountPairedMatchedPatients> aggDerivedPra(
      List<Range<Double>> ranges) {
    List<UnosCountPairedMatchedPatients> ans = Lists.newArrayList();
    for (Range<Double> range : ranges) {
      ans.add(UnosAgg.derivedPraInMatched(range));
    }
    return ans;
  }

  private static List<UnosCountPairedMatchedPatients> aggUnosPraProtos(
      List<KepProtos.Range> protoRanges) {
    return aggUnosPra(ProtoUtil.createRanges(protoRanges));
  }

  private static List<UnosCountPairedMatchedPatients> aggUnosPra(
      List<Range<Double>> ranges) {
    List<UnosCountPairedMatchedPatients> ans = Lists.newArrayList();
    for (Range<Double> range : ranges) {
      ans.add(UnosAgg.unosPraInMatched(range));
    }
    return ans;
  }

  private static List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> unosObjectiveAgg() {
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = Lists
        .newArrayList();
    agg.add(UnosAgg.pediatricsMatched());
    agg.add(UnosAgg.unosPraInMatched(Range.atLeast(80.0)));
    agg.add(UnosAgg.wasSameCenter());
    agg.add(Agg
        .<UnosExchangeUnit, UnosDonorEdge> totalDoubleWaitingTimeAtLeast(500));
    return agg;
  }

  private static List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> demographicAgg() {
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = Lists
        .newArrayList();
    agg.addAll(UnosAgg.bloodTypePairedNodesMatched(true, "bloodType"));
    agg.addAll(UnosAgg.racePairedNodesMatched(true));
    // agg.add(UnosAgg.pediatricsMatched());
    // agg.addAll(aggUnosPraProtos(patientPraBuckets));
    // agg.addAll(aggDerivedPraProtos(patientPraBuckets));
    return agg;
  }

  private static <V, E> List<NumericTrialAttribute<V, E, Double, Double>> cycleChainBreakDown() {
    List<NumericTrialAttribute<V, E, Double, Double>> agg = Lists
        .newArrayList();
    agg.add(new TotalCycleEdges<V, E, Double, Double>(Range.singleton(2)));
    agg.add(new TotalCycleEdges<V, E, Double, Double>(Range.singleton(3)));
    agg.add(new TotalChainEdges<V, E, Double, Double>(true, Range.atMost(5)));
    agg.add(new TotalChainEdges<V, E, Double, Double>(true, Range.openClosed(5,
        15)));
    agg.add(new TotalChainEdges<V, E, Double, Double>(true, Range
        .greaterThan(15)));
    return agg;
  }

  private static double[] patientPraBucketDividers = new double[] { 50, 80, 90,
      95, 99 };
  private static List<KepProtos.Range> patientPraBuckets = ProtoObjectives
      .makeSteps(patientPraBucketDividers);
  private static double[] patientPraCoefficients = new double[] { 0, .2, .5, 1,
      2, 4 };
  private static ImmutableMap<KepProtos.BloodType, Double> patientBloodTypeBonus = ImmutableMap
      .of(KepProtos.BloodType.A, 1.0, KepProtos.BloodType.B, 1.0,
          KepProtos.BloodType.O, 4.0, KepProtos.BloodType.AB, 0.0);

  private static double[] patientPowerDividersShort = new double[] { .01, .1 };

  // private static double[] donorPowerDividers = new double[] { .05, .2, .4, .7
  // };
  private static List<KepProtos.Range> patientPowerBucketsShort = ProtoObjectives
      .makeSteps(patientPowerDividersShort);

  private static double[] patientPowerDividersFull = new double[] { .003, .01,
      .05, .1, .3, .5 };

  // private static double[] patientPowerDividers = new double[] { .01, .05, .2,
  // .4 };
  private static List<KepProtos.Range> patientPowerBucketsFull = ProtoObjectives
      .makeSteps(patientPowerDividersFull);
  private static double[] patientPowerCoefficients = new double[] { 15, 10, 5,
      1, .3, .1, 0 };

  private static double[] donorPraBucketDividers = new double[] { 55, 60 };
  private static List<KepProtos.Range> donorPraBuckets = ProtoObjectives
      .makeSteps(donorPraBucketDividers);
  private static double[] donorPraCoefficients = new double[] { 0, .3, 1 };

  private static ImmutableMap<KepProtos.BloodType, Double> donorBloodTypeBonus = ImmutableMap
      .of(KepProtos.BloodType.A, 1.0, KepProtos.BloodType.B, 1.0,
          KepProtos.BloodType.O, 0.0, KepProtos.BloodType.AB, 4.0);

  private static ImmutableMap<KepProtos.HLA, Double> donorHlaPenalties = ImmutableMap
      .<KepProtos.HLA, Double> builder().put(KepProtos.HLA.HLA_A, 0.15)
      .put(KepProtos.HLA.HLA_B, 0.15).put(KepProtos.HLA.DR, 0.15)
      .put(KepProtos.HLA.Cw, 0.15).put(KepProtos.HLA.DP, 0.15)
      .put(KepProtos.HLA.DQ, 0.25).build();

  private static double[] donorPowerDividersShort = new double[] { .05, .15 };

  // private static double[] donorPowerDividers = new double[] { .05, .2, .4, .7
  // };
  private static List<KepProtos.Range> donorPowerBucketsShort = ProtoObjectives
      .makeSteps(donorPowerDividersShort);

  private static double[] donorPowerDividersFull = new double[] { .005, .02,
      .05, .1, .3 };

  // private static double[] donorPowerDividers = new double[] { .05, .2, .4, .7
  // };
  private static List<KepProtos.Range> donorPowerBucketsFull = ProtoObjectives
      .makeSteps(donorPowerDividersFull);
  private static double[] donorPowerCoefficientsFull = new double[] { 10, 5,
      1.5, .5, .1, 0 };

  private static double[] pairPowerDividersShort = new double[] { 5, 20 };
  private static List<KepProtos.Range> pairPowerBucketsShort = ProtoObjectives
      .makeSteps(pairPowerDividersShort);

  private static double[] pairPowerDividers = new double[] { 1, 5, 20, 40, 100 };
  private static List<KepProtos.Range> pairPowerBuckets = ProtoObjectives
      .makeSteps(pairPowerDividers);
  private static double[] pairPowerCoefficients = new double[] { 20, 10, 5, 1,
      .3, 0 };

  private void experimentPrioritizeByPowerShort() {

    String outDirName = defaultOutput + "prioritizeByPowerShort"
        + File.separator;
    List<ObjectiveFunction> objectives = Lists.newArrayList();
    List<String> names = Lists.newArrayList();

    List<double[]> points = Lists.newArrayList();
    points.add(new double[] { 0.5, 0.2, 0.0 });
    points.add(new double[] { 1, .5, 0.0 });
    points.add(new double[] { 3, 1, 0.0 });
    points.add(new double[] { 5, 2, 0.0 });

    // prioritize edges by historic target patient match power
    for (double[] pointVals : points) {
      ObjectiveFunction.Builder patientHistoricMatchPower = ObjectiveFunction
          .newBuilder().setConstant(1.0);
      StepFunctionBuilder stepFunction = new StepFunctionBuilder();
      stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
          .getTargetBuilder().getHistoricPatientPowerBuilder());
      patientHistoricMatchPower.addAllEdgeStep(stepFunction.build(pointVals,
          patientPowerBucketsShort));
      objectives.add(patientHistoricMatchPower.build());
      names.add("historicPatientMatchPower" + Arrays.toString(pointVals));
    }

    // prioritize edges by pool target patient match power
    for (double[] pointVals : points) {
      ObjectiveFunction.Builder patientPoolMatchPower = ObjectiveFunction
          .newBuilder().setConstant(1.0);
      StepFunctionBuilder stepFunction = new StepFunctionBuilder();
      stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
          .getTargetBuilder().getPoolPatientPowerBuilder());
      patientPoolMatchPower.addAllEdgeStep(stepFunction.build(pointVals,
          patientPowerBucketsShort));
      objectives.add(patientPoolMatchPower.build());
      names.add("poolPatientMatchPower" + Arrays.toString(pointVals));
    }

    // prioritize edges by historic source node donor power + historic target
    // node patient power
    for (double[] patientPoints : points) {
      for (double[] donorPoints : points) {
        ObjectiveFunction.Builder obj = ObjectiveFunction.newBuilder()
            .setConstant(1.0);
        {
          StepFunctionBuilder stepFunction = new StepFunctionBuilder();
          stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
              .getTargetBuilder().getHistoricPatientPowerBuilder());
          obj.addAllEdgeStep(stepFunction.build(patientPoints,
              patientPowerBucketsShort));
        }
        {
          StepFunctionBuilder stepFunction = new StepFunctionBuilder();
          stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
              .getSourceBuilder().getHistoricDonorPowerBuilder());
          obj.addAllEdgeStep(stepFunction.build(donorPoints,
              donorPowerBucketsShort));
        }
        objectives.add(obj.build());
        names.add("historicDonorMatch" + Arrays.toString(donorPoints)
            + "PatientMatch" + Arrays.toString(patientPoints) + "Power");
      }
    }
    // prioritize edges by pool source node donor power + pool target
    // node patient power
    for (double[] patientPoints : points) {
      for (double[] donorPoints : points) {
        ObjectiveFunction.Builder obj = ObjectiveFunction.newBuilder()
            .setConstant(1.0);
        {
          StepFunctionBuilder stepFunction = new StepFunctionBuilder();
          stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
              .getTargetBuilder().getPoolPatientPowerBuilder());
          obj.addAllEdgeStep(stepFunction.build(patientPoints,
              patientPowerBucketsShort));
        }
        {
          StepFunctionBuilder stepFunction = new StepFunctionBuilder();
          stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
              .getSourceBuilder().getPoolDonorPowerBuilder());
          obj.addAllEdgeStep(stepFunction.build(donorPoints,
              donorPowerBucketsShort));
        }
        objectives.add(obj.build());
        names.add("poolDonorMatch" + Arrays.toString(donorPoints)
            + "PatientMatch" + Arrays.toString(patientPoints) + "Power");
      }
    }

    // prioritize edges by historic target pair match power
    for (double[] pointVals : points) {
      ObjectiveFunction.Builder obj = ObjectiveFunction.newBuilder()
          .setConstant(1.0);
      StepFunctionBuilder stepFunction = new StepFunctionBuilder();
      stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
          .getTargetBuilder().getHistoricPairMatchPowerBuilder());
      obj.addAllEdgeStep(stepFunction.build(pointVals, pairPowerBucketsShort));
      objectives.add(obj.build());
      names.add("historicPairMatchPower" + Arrays.toString(pointVals));
    }

    // prioritize edges by pool target pair match power
    for (double[] pointVals : points) {
      ObjectiveFunction.Builder obj = ObjectiveFunction.newBuilder()
          .setConstant(1.0);
      StepFunctionBuilder stepFunction = new StepFunctionBuilder();
      stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
          .getTargetBuilder().getPoolPairMatchPowerBuilder());
      obj.addAllEdgeStep(stepFunction.build(pointVals, pairPowerBucketsShort));
      objectives.add(obj.build());
      names.add("poolPairMatchPower" + Arrays.toString(pointVals));
    }
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    runExperiment(objectives, names, agg, outDirName);
  }

  private void experimentPrioritizeHardToMatch() {
    String outDirName = defaultOutput + "prioritizeHardToMatch"
        + File.separator;
    List<ObjectiveFunction> objectives = Lists.newArrayList();
    List<String> names = Lists.newArrayList();
    {
      // baseline: no prioritization
      ObjectiveFunction.Builder baseline = ObjectiveFunction.newBuilder()
          .setConstant(1.0);
      objectives.add(baseline.build());
      names.add("none");

    }
    // prioritize edges by historic target patient PRA
    {
      ObjectiveFunction.Builder patientHistoricPra = ObjectiveFunction
          .newBuilder().setConstant(1.0);
      StepFunctionBuilder stepFunction = new StepFunctionBuilder();
      stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
          .getTargetBuilder().getPatientPredicateBuilder()
          .getHistoricPatientPraBuilder());
      patientHistoricPra.addAllEdgeStep(stepFunction.build(
          patientPraCoefficients, patientPraBuckets));
      objectives.add(patientHistoricPra.build());
      names.add("historicPatientPra");
    }
    // prioritize edges by pool target patient PRA
    {
      ObjectiveFunction.Builder patientPoolPra = ObjectiveFunction.newBuilder()
          .setConstant(1.0);
      StepFunctionBuilder stepFunction = new StepFunctionBuilder();
      stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
          .getTargetBuilder().getPatientPredicateBuilder()
          .getPoolPatientPraBuilder());
      patientPoolPra.addAllEdgeStep(stepFunction.build(patientPraCoefficients,
          patientPraBuckets));
      objectives.add(patientPoolPra.build());
      names.add("poolPatientPra");
    }
    // prioritize edges by historic target patient match power
    {
      ObjectiveFunction.Builder patientHistoricMatchPower = ObjectiveFunction
          .newBuilder().setConstant(1.0);
      StepFunctionBuilder stepFunction = new StepFunctionBuilder();
      stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
          .getTargetBuilder().getHistoricPatientPowerBuilder());
      patientHistoricMatchPower.addAllEdgeStep(stepFunction.build(
          patientPowerCoefficients, patientPowerBucketsFull));
      objectives.add(patientHistoricMatchPower.build());
      names.add("historicPatientMatchPower");
    }
    // prioritize edges by pool target patient match power
    {
      ObjectiveFunction.Builder patientPoolMatchPower = ObjectiveFunction
          .newBuilder().setConstant(1.0);
      StepFunctionBuilder stepFunction = new StepFunctionBuilder();
      stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
          .getTargetBuilder().getPoolPatientPowerBuilder());
      patientPoolMatchPower.addAllEdgeStep(stepFunction.build(
          patientPowerCoefficients, patientPowerBucketsFull));
      objectives.add(patientPoolMatchPower.build());
      names.add("poolPatientMatchPower");
    }
    // prioritize edges by historic source node donor power + historic target
    // node patient power
    {
      ObjectiveFunction.Builder patientHistoricMatchPower = ObjectiveFunction
          .newBuilder().setConstant(1.0);
      {
        StepFunctionBuilder stepFunction = new StepFunctionBuilder();
        stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
            .getTargetBuilder().getHistoricPatientPowerBuilder());
        patientHistoricMatchPower.addAllEdgeStep(stepFunction.build(
            patientPowerCoefficients, patientPowerBucketsFull));
      }
      {
        StepFunctionBuilder stepFunction = new StepFunctionBuilder();
        stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
            .getSourceBuilder().getHistoricDonorPowerBuilder());
        patientHistoricMatchPower.addAllEdgeStep(stepFunction.build(
            patientPowerCoefficients, patientPowerBucketsFull));
      }
      objectives.add(patientHistoricMatchPower.build());
      names.add("historicSourceDonorMatchPowerPlusTargetPatientMatchPower");
    }
    // prioritize only on patient blood type
    {
      ObjectiveFunction.Builder patientBloodType = ObjectiveFunction
          .newBuilder().setConstant(1.0);
      patientBloodType.addAllEdgeStep(ProtoObjectives
          .patientBloodType(patientBloodTypeBonus));
      objectives.add(patientBloodType.build());
      names.add("patientBloodType");
    }
    // prioritize only patient & donor blood type
    {
      ObjectiveFunction.Builder patientDonorBloodType = ObjectiveFunction
          .newBuilder().setConstant(1.0);
      patientDonorBloodType.addAllEdgeStep(ProtoObjectives
          .patientBloodType(patientBloodTypeBonus));
      patientDonorBloodType.addAllEdgeStep(ProtoObjectives
          .donorEdgeBloodType(donorBloodTypeBonus));
      objectives.add(patientDonorBloodType.build());
      names.add("patient&DonorBloodType");
    }
    // prioritize patient & donor blood type with homozygous penalty on donors
    {
      double baseScore = 1.0;
      ObjectiveFunction.Builder patientDonorBloodTypeHomozygous = ObjectiveFunction
          .newBuilder().setConstant(baseScore);
      patientDonorBloodTypeHomozygous.addAllEdgeStep(ProtoObjectives
          .patientBloodType(patientBloodTypeBonus));
      patientDonorBloodTypeHomozygous.addAllEdgeStep(ProtoObjectives
          .donorHomozygousHlaPenalty(donorHlaPenalties, donorBloodTypeBonus,
              baseScore));
      objectives.add(patientDonorBloodTypeHomozygous.build());
      names.add("patient&DonorBloodTypeHomozygousPenalty");
    }
    // prioritize edges by historic target patient PRA and BloodType combined
    {
      ObjectiveFunction.Builder patientHistoricPraAndBlood = ObjectiveFunction
          .newBuilder().setConstant(1.0);
      patientHistoricPraAndBlood.addAllEdgeStep(ProtoObjectives
          .patientBloodType(patientBloodTypeBonus));
      StepFunctionBuilder stepFunction = new StepFunctionBuilder();
      stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
          .getTargetBuilder().getPatientPredicateBuilder()
          .getHistoricPatientPraBuilder());
      patientHistoricPraAndBlood.addAllEdgeStep(stepFunction.build(
          patientPraCoefficients, patientPraBuckets));
      objectives.add(patientHistoricPraAndBlood.build());
      names.add("historicPatientPra+BloodType");
    }
    // prioritize edges by historic source donor and target patient PRA and
    // BloodType combined
    {
      ObjectiveFunction.Builder patientDonorHistoricPraAndBlood = ObjectiveFunction
          .newBuilder().setConstant(1.0);
      patientDonorHistoricPraAndBlood.addAllEdgeStep(ProtoObjectives
          .patientBloodType(patientBloodTypeBonus));
      patientDonorHistoricPraAndBlood.addAllEdgeStep(ProtoObjectives
          .donorEdgeBloodType(donorBloodTypeBonus));
      {
        StepFunctionBuilder stepFunction = new StepFunctionBuilder();
        stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
            .getTargetBuilder().getPatientPredicateBuilder()
            .getHistoricPatientPraBuilder());
        patientDonorHistoricPraAndBlood.addAllEdgeStep(stepFunction.build(
            patientPraCoefficients, patientPraBuckets));
      }
      {
        StepFunctionBuilder stepFunction = new StepFunctionBuilder();
        stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
            .getEdgeDonorPredicateBuilder().getHistoricPraBuilder());
        patientDonorHistoricPraAndBlood.addAllEdgeStep(stepFunction.build(
            donorPraCoefficients, donorPraBuckets));
      }
      objectives.add(patientDonorHistoricPraAndBlood.build());
      names.add("historicPatient+DonorPra+BloodType");
    }
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    runExperiment(objectives, names, agg, outDirName);
  }

  private void experimentUnosPraBonusSensitivity() {
    String outDirName = defaultOutput + "unosPraSensitivity" + File.separator;
    List<ObjectiveFunction> objectives = Lists.newArrayList();
    List<String> names = Lists.newArrayList();

    objectives.add(UnosProtoObjectives.unosProposedObjective(EnumSet
        .complementOf(EnumSet.of(ObjFactor.PRA))));
    names.add("pra bonus 0 (none)");

    objectives.add(UnosProtoObjectives.unosProposedObjective());
    names.add("pra bonus 125 (base)");
    for (int bonus : new int[] { 250, 500, 750, 1000, 1250, 1500 }) {
      objectives.add(UnosProtoObjectives.unosProposedObjective(
          ImmutableMap.of(ObjFactor.PRA, (double) bonus), true));
      names.add("pra bonus " + bonus);
    }

    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    runExperiment(objectives, names, agg, outDirName);
  }

  private void experimentPraVsWaitingTime() {
    String outDirName = defaultOutput + "praVsWaiting" + File.separator;
    List<ObjectiveFunction> objectives = Lists.newArrayList();
    List<String> names = Lists.newArrayList();

    objectives.add(ObjectiveFunction.newBuilder().setConstant(1.0).build());
    names.add("constant 1.0");
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = Lists
        .newArrayList();
    for (Range<Double> patientPower : ProtoUtil
        .createRanges(patientPowerBucketsShort)) {
      agg.add(Agg
          .<UnosExchangeUnit, UnosDonorEdge> patientPowerPairedTotalWaitingTimeDoubleTime(patientPower));
    }
    for (Range<Double> patientPra : ProtoUtil.createRanges(ProtoObjectives
        .makeSteps(new double[] { 80, 95 }))) {
      agg.add(UnosAgg.waitingTimeUnosPatientPraIn(patientPra));
    }

    for (Range<Double> pairPower : ProtoUtil.createRanges(pairPowerBuckets)) {
      agg.add(Agg
          .<UnosExchangeUnit, UnosDonorEdge> sensitizedPairedTotalWaitingTimeDoubleTime(pairPower));
    }
    runExperiment(objectives, names, agg, outDirName);
  }

  private void experimentPatientPraObjectives() {
    String outDirName = defaultOutput + "patientPraObjectives" + File.separator;
    List<ObjectiveFunction> objectives = Lists.newArrayList();
    List<String> names = Lists.newArrayList();

    objectives.add(ObjectiveFunction.newBuilder().setConstant(1.0).build());
    names.add("constant 1.0");
    List<double[]> objectiveCoefs = Lists.newArrayList();
    objectiveCoefs.add(new double[] { 0, .1, .2, .3, .4, .5 });
    objectiveCoefs.add(new double[] { 0, .1, .3, .5, .8, 1.2 });
    objectiveCoefs.add(new double[] { 0, .1, .3, .6, 1, 2 });
    objectiveCoefs.add(new double[] { 0, .2, .4, .8, 1.5, 3 });
    objectiveCoefs.add(new double[] { 0, .2, .5, 1, 2, 4 });
    objectiveCoefs.add(new double[] { 0, .2, .6, 1.2, 3, 6 });
    objectiveCoefs.add(new double[] { 0, .3, 1, 2, 4, 8 });
    for (double[] objectiveCoef : objectiveCoefs) {
      ObjectiveFunction.Builder objectiveBuilder = ObjectiveFunction
          .newBuilder().setConstant(1.0);
      StepFunctionBuilder stepFunction = new StepFunctionBuilder();
      stepFunction.setRangeBuilder(stepFunction.getEdgePredicateBuilder()
          .getTargetBuilder().getPatientPredicateBuilder()
          .getHistoricPatientPraBuilder());
      objectiveBuilder.addAllEdgeStep(stepFunction.build(objectiveCoef,
          patientPraBuckets));
      objectives.add(objectiveBuilder.build());
      names.add("pra bonus " + Arrays.toString(objectiveCoef));
    }
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    double[] patientPraDiv = new double[] { 80, 90, 95, 99 };
    List<KepProtos.Range> patientPraBuck = ProtoObjectives
        .makeSteps(patientPraDiv);
    agg.addAll(UnosAnalysis.aggUnosPraProtos(patientPraBuck));
    runExperiment(objectives, names, agg, outDirName);
  }

  private void experimentSinglePraThreshold() {
    String outDirName = defaultOutput + "singlePraThreshold" + File.separator;
    List<ObjectiveFunction> objectives = Lists.newArrayList();
    List<String> names = Lists.newArrayList();
    ObjectiveFunction.Builder objBuilder = UnosProtoObjectives
        .unosProposedObjective(EnumSet.complementOf(EnumSet.of(ObjFactor.PRA)))
        .toBuilder();
    EdgeStep.Builder praStep = objBuilder.addEdgeStepBuilder();
    KepProtos.Range.Builder praRange = praStep.getEdgeConjunctionBuilder()
        .addEdgePredicateBuilder().getTargetBuilder()
        .getPatientPredicateBuilder().getHistoricPatientPraBuilder();
    for (double threshold : new double[] { 80, 90, 95, 99 }) {
      for (double bonus : new double[] { 125, 250, 500, 1000, 1500 }) {
        praStep.setScore(bonus);
        praRange.setLowerBound(threshold);
        objectives.add(objBuilder.build());
        names.add("threshold" + threshold + "bonus" + bonus);
      }
    }
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    double[] patientPraDiv = new double[] { 80, 90, 95, 99 };
    List<KepProtos.Range> patientPraBuck = ProtoObjectives
        .makeSteps(patientPraDiv);
    agg.addAll(UnosAnalysis.aggUnosPraProtos(patientPraBuck));
    runExperiment(objectives, names, agg, outDirName);
  }

  private void experimentWaiting() {
    String outDirName = defaultOutput + "experimentWaiting" + File.separator;
    List<ObjectiveFunction> objectives = Lists.newArrayList();
    List<String> names = Lists.newArrayList();
    for (double weight : new double[] { 0, 3, 7, 20, 50, 100, 200 }) {
      objectives.add(UnosProtoObjectives.unosProposedObjective(
          ImmutableMap.of(ObjFactor.WAITING, weight), true));
      names.add(weight + " points per 100 days");
    }
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    for (int days : new int[] { 50, 100, 150, 200, 250, 300, 350, 400, 450, 500 }) {
      agg.add(Agg
          .<UnosExchangeUnit, UnosDonorEdge> totalDoubleWaitingTimeAtLeast(days));
    }
    runExperiment(objectives, names, agg, outDirName);
  }

  private void experimentDeleteUnosCriteria() {
    String outDirName = defaultOutput + "experimentUnosFactors"
        + File.separator;
    List<ObjectiveFunction> objectives = Lists.newArrayList();
    List<String> names = Lists.newArrayList();
    objectives.add(UnosProtoObjectives.unosProposedObjective());
    names.add("all");

    objectives.add(UnosProtoObjectives.unosProposedObjective(EnumSet
        .noneOf(ObjFactor.class)));
    names.add("none");

    for (ObjFactor factor : ObjFactor.values()) {
      objectives.add(UnosProtoObjectives.unosProposedObjective(EnumSet
          .complementOf(EnumSet.of(factor))));
      names.add("no-" + factor);
    }

    // make versions of objectives that are more extreme in points awarded.
    EnumMap<ObjFactor, Double> extremeFactors = Maps
        .newEnumMap(ObjFactor.class);
    extremeFactors.put(ObjFactor.CENTER, 600.0);
    extremeFactors.put(ObjFactor.PEDIATRIC, 600.0);
    extremeFactors.put(ObjFactor.PRA, 15000.0);
    extremeFactors.put(ObjFactor.WAITING, 200.0);
    for (ObjFactor factor : ObjFactor.values()) {
      objectives.add(UnosProtoObjectives.unosProposedObjective(
          ImmutableMap.of(factor, extremeFactors.get(factor)), true));
      names.add("extreme-" + factor);
    }
    objectives.add(UnosProtoObjectives.unosProposedObjective(extremeFactors,
        true));
    names.add("all-extreme");

    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    agg.addAll(unosObjectiveAgg());
    agg.addAll(demographicAgg());
    /*
     * agg.add(Agg .<UnosExchangeUnit, UnosDonorEdge>
     * pairedTotalWaitingTimeDoubleTime(SumStatVal.ST_DEV)); agg.add(Agg
     * .<UnosExchangeUnit, UnosDonorEdge>
     * sensitizedPairedTotalWaitingTimeDoubleTime( Range.closed(0.0, 20.0),
     * SumStatVal.ST_DEV)); agg.add(Agg .<UnosExchangeUnit, UnosDonorEdge>
     * sensitizedPairedTotalWaitingTimeDoubleTime( Range.greaterThan(20.0),
     * SumStatVal.ST_DEV));
     */
    runExperiment(objectives, names, agg, outDirName);
  }

  private void experimentPriority() {
    String outDirName = defaultOutput + "experimentPriority" + File.separator;
    SimulationInputs.Builder inputBuilder = defaultInputBuilderWithObjective(
        environment.getSimulationStart(), environment.getSimulationEnd());
    LinkedHashMap<SimulationInputs, String> inputs = new LinkedHashMap<SimulationInputs, String>();
    for (PrioritizationLevel level : PrioritizationLevel.values()) {
      inputBuilder.getKepModelerInputsBuilder().getObjectiveBuilder()
          .setPrioritization(level);
      inputs.put(inputBuilder.build(), level.toString());
    }
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    runExperiment(inputs, agg, outDirName);
  }

  private void experimentProtoObjPriority() {
    String outDirName = defaultOutput + "experimentProtoObjPriority"
        + File.separator;
    SimulationInputs.Builder inputBuilder = defaultInputBuilder(
        environment.getSimulationStart(), environment.getSimulationEnd());
    KepModelerInputs.Builder modelerBuilder = inputBuilder
        .getKepModelerInputsBuilder();
    LinkedHashMap<SimulationInputs, String> simInputs = new LinkedHashMap<SimulationInputs, String>();
    {
      modelerBuilder.setObjectiveFunction(ProtoObjectives.maximumCardinality());
      simInputs.put(inputBuilder.build(), "maxCard");
    }
    {
      modelerBuilder.setObjectiveFunction(ProtoObjectives
          .patientPairMatchPower(
              ProtoObjectives.makeSteps(new double[] { .1, .5 }), new double[] {
                  5.0, 2.0, 1.0 }));
      simInputs.put(inputBuilder.build(), "mildPatientPower");
    }
    {
      modelerBuilder.setObjectiveFunction(UnosProtoObjectives
          .unosProposedObjective());
      simInputs.put(inputBuilder.build(), "unosDefault");
    }
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    runExperiment(simInputs, agg, outDirName);
  }

  private void experimentChainLength() {
    String outDirName = defaultOutput + "experimentChainLength"
        + File.separator;
    SimulationInputs.Builder inputBuilder = defaultInputBuilder(
        environment.getSimulationStart(), environment.getSimulationEnd());
    inputBuilder.getKepModelerInputsBuilder().setObjectiveFunction(
        UnosProtoObjectives.unosProposedObjective());
    LinkedHashMap<SimulationInputs, String> inputs = new LinkedHashMap<SimulationInputs, String>();
    for (int chainLength : new int[] { 3, 4, 5, 6, 7, Integer.MAX_VALUE }) {
      inputBuilder.getKepModelerInputsBuilder().setMaxChainLength(chainLength);
      inputs.put(inputBuilder.build(), Integer.toString(chainLength));
    }
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    agg.addAll(UnosAnalysis
        .<UnosExchangeUnit, UnosDonorEdge> cycleChainBreakDown());
    runExperiment(inputs, agg, outDirName);
  }

  public void experimentNegativeWeights() {
    String outDirName = defaultOutput + "experimentNegativeWeights"
        + File.separator;
    List<ObjectiveFunction> objectives = Lists.newArrayList();
    List<String> names = Lists.newArrayList();

    ObjectiveFunction.Builder obj = ObjectiveFunction.newBuilder().setConstant(
        1);
    for (double penalty : new double[] { -3 }) {// , -2.5, -2, -1.5 }) {
      for (double easyPatient : new double[] { .2 }) {// .15, .2, .3, .5 }) {
        for (double easyDonor : new double[] { .2 }) {// .15, .2, .3, .5 }) {
          for (double hardDonor : new double[] { .1 }) {// .05, .1 }) {
            for (int otherEasyDonorsLessThan : new int[] { 3 }) {// 2, 3 }) {

              EdgePredicate.Builder edgePredicate = obj.addEdgeStepBuilder()
                  .setScore(penalty).getEdgeConjunctionBuilder()
                  .setRelation(Relation.AND).addEdgePredicateBuilder();
              // only apply on edges to easy to match patients
              edgePredicate.getTargetBuilder().getHistoricPatientPowerBuilder()
                  .setLowerBound(easyPatient);
              // only apply on easy edges from easy to match donors
              edgePredicate.getSourceBuilder().getHistoricDonorPowerBuilder()
                  .setLowerBound(easyDonor);

              // look at the neighborhood of the source.
              NeighborhoodPredicate.Builder neighborhood = edgePredicate
                  .getSourceBuilder().addOutNeighborhoodPredicateBuilder();
              // apply only if there is at least one neighboring node satisfying
              // the below
              // criteria.
              neighborhood.getNeighborCountBuilder().setLowerBound(1);
              // nodes where the patient is hard to match and has no other
              // options
              neighborhood.getNeighborPredicateBuilder()
                  .getHistoricPatientPowerBuilder().setUpperBound(hardDonor);
              neighborhood.getNeighborPredicateBuilder().getInDegreeBuilder()
                  .setUpperBound(otherEasyDonorsLessThan);
              objectives.add(obj.build());
              names.add("pen" + penalty + " easyPat" + easyPatient + " easyDon"
                  + easyDonor + " hardDon" + hardDonor + " otherEasyLessThan"
                  + otherEasyDonorsLessThan);
            }
          }
        }
      }
    }

    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    runExperiment(objectives, names, agg, outDirName);

  }

  public void experimentBasic() {
    String outDirName = defaultOutput + "basic" + File.separator;
    SimulationInputs.Builder inputBuilder = defaultInputBuilderWithObjective(
        environment.getSimulationStart(), environment.getSimulationEnd());
    LinkedHashMap<SimulationInputs, String> inputs = new LinkedHashMap<SimulationInputs, String>();
    inputs.put(inputBuilder.build(), "basic");
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    agg.addAll(UnosAnalysis
        .<UnosExchangeUnit, UnosDonorEdge> cycleChainBreakDown());
    runExperiment(inputs, agg, outDirName);
  }

  private void experimentNoCycles() {
    String outDirName = defaultOutput + "experimentNoCycles" + File.separator;
    SimulationInputs.Builder inputBuilder = defaultInputBuilderWithObjective(
        environment.getSimulationStart(), environment.getSimulationEnd());
    LinkedHashMap<SimulationInputs, String> inputs = new LinkedHashMap<SimulationInputs, String>();
    for (int cycleLength : new int[] { 0, 3 }) {
      inputBuilder.getKepModelerInputsBuilder().setMaxCycleLength(cycleLength);
      inputs.put(inputBuilder.build(), Integer.toString(cycleLength));
    }
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    agg.addAll(UnosAnalysis
        .<UnosExchangeUnit, UnosDonorEdge> cycleChainBreakDown());
    runExperiment(inputs, agg, outDirName);
  }

  private void experimentMatchingFrequency() {
    String outDirName = defaultOutput + "experimentMatchingFrequency"
        + File.separator;
    SimulationInputs.Builder inputBuilder = defaultInputBuilderWithObjective(
        environment.getSimulationStart(), environment.getSimulationEnd());
    LinkedHashMap<SimulationInputs, String> inputs = new LinkedHashMap<SimulationInputs, String>();
    for (int matchingFrequency : new int[] { 1, 7, 14, 30, 90, 365, 1141 }) {
      inputBuilder.setMatchingFrequency(matchingFrequency);
      inputs.put(inputBuilder.build(), Integer.toString(matchingFrequency));
    }
    List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg = basicAgg();
    runExperiment(inputs, agg, outDirName);
  }

  private void runExperiment(
      List<ObjectiveFunction> protoObjectives,
      List<String> objectiveNames,
      List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> agg,
      String outDirName) {
    if (protoObjectives.size() != objectiveNames.size()) {
      throw new RuntimeException("number of objectives = "
          + protoObjectives.size() + " and number of names = "
          + objectiveNames.size() + " but should have been equal.");
    }
    SimulationInputs.Builder inputBuilder = defaultInputBuilder(
        environment.getSimulationStart(), environment.getSimulationEnd());
    KepModelerInputs.Builder modelerBuilder = inputBuilder
        .getKepModelerInputsBuilder();
    LinkedHashMap<SimulationInputs, String> simInputs = new LinkedHashMap<SimulationInputs, String>();
    for (int i = 0; i < protoObjectives.size(); i++) {
      modelerBuilder.setObjectiveFunction(protoObjectives.get(i));
      simInputs.put(inputBuilder.build(), objectiveNames.get(i));
    }
    runExperiment(simInputs, agg, outDirName);

  }

  private void runExperiment(
      LinkedHashMap<SimulationInputs, String> simulationInputs,
      List<NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double>> nodeAggregationAttributes,
      String outDirName) {

    File outDirFile = new File(outDirName);
    if (!outDirFile.isDirectory()) {
      outDirFile.mkdirs();
    }

    Map<SimulationInputs, Simulation> simulationResults = resultCache
        .getAllSimulationResults(Sets.newHashSet(simulationInputs.keySet()));

    TrialReport<UnosExchangeUnit, UnosDonorEdge, Double, Double> trialReport = new TrialReport<UnosExchangeUnit, UnosDonorEdge, Double, Double>(
        true, true, true);
    UnosAggregateTrialReport randomTrialsReport = new UnosAggregateTrialReport(
        true, true, true, Lists.newArrayList(simulationInputs.keySet()),
        environment.getReplications(),
        (UnosHistoricData) environment.getHistoricData());
    for (NumericTrialAttribute<UnosExchangeUnit, UnosDonorEdge, Double, Double> agg : nodeAggregationAttributes) {
      trialReport.addAttribute(agg);
      randomTrialsReport.addNodeAggregationAttribute(agg);
    }
    // need to do use inputs instead of results to get correct iteration order
    for (SimulationInputs simInputs : simulationInputs.keySet()) {
      Simulation simResults = simulationResults.get(simInputs);
      DynamicMatching<UnosExchangeUnit, UnosDonorEdge, Double> dynamicMatching = environment
          .restoreDynamicMatching(simResults.getHistoricDynamicMatching());
      UnosOutputAttributeSet attributeSet = new UnosOutputAttributeSet(
          environment.getMultiPeriodInputs(), dynamicMatching,
          (UnosHistoricData) environment.getHistoricData());
      String trialName = simulationInputs.get(simInputs);
      trialReport.addOutcome(new UnosTrialOutcome(trialName, attributeSet));
      randomTrialsReport.addAllTrialOutcomes(trialName, simResults,
          environment, DoubleTimeDifferenceCalculator.instance);

    }

    trialReport.writeReport(outDirName + "report.csv", CSVFormat.EXCEL);
    randomTrialsReport.writeReport(outDirName + "experiment1aggregate.csv",
        CSVFormat.EXCEL);

  }
}
