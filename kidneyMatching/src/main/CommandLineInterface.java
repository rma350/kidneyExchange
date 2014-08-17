package main;

import ilog.concert.IloException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepLib.KepInstance;
import kepLib.KepParseData;
import kepLib.KepTextReaderWriter;
import randomInstances.RandomArrivalTimes;
import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import exchangeGraph.CycleChainPackingSubtourElimination;
import exchangeGraph.SolverOption;
import exchangeGraph.minWaitingTime.MinWaitingTimeKepSolver;
import exchangeGraph.minWaitingTime.MinWaitingTimeProblemData;
import exchangeGraph.stochasticOpt.EdgeFailureScenario;
import exchangeGraph.stochasticOpt.TwoStageEdgeFailureSolver;
import graphUtil.CycleChainDecomposition;
import graphUtil.Edge;
import graphUtil.Node;

public class CommandLineInterface {

  public static final String deterministicMode = "deterministicMode";
  public static final String twoStageMode = "twoStageMode";
  public static final String minWaitingMode = "minWaitingMode";

  private static long extractOrDefault(EnumMap<CLIOption, String> options,
      CLIOption option, long defaultValue) {
    if (options.containsKey(option)) {
      return Integer.parseInt(options.get(option));
    } else {
      return defaultValue;
    }
  }

  private static boolean extractOrDefault(EnumMap<CLIOption, String> options,
      CLIOption option, boolean defaultValue) {
    if (options.containsKey(option)) {
      return Boolean.parseBoolean(options.get(option));
    } else {
      return defaultValue;
    }
  }

  private static <T extends Enum<T>> T extractOrDefault(
      EnumMap<CLIOption, String> options, CLIOption option, T defaultValue,
      Iterable<T> acceptableValues) {
    if (options.containsKey(option)) {
      String optionName = options.get(option);
      for (T val : acceptableValues) {
        if (val.name().equalsIgnoreCase(optionName)) {
          return val;
        }
      }
    }
    return defaultValue;
  }

  private static <T extends Enum<T>> T extractOrDefault(
      EnumMap<CLIOption, String> options, CLIOption option, T defaultValue,
      Class<T> clazz) {
    return extractOrDefault(options, option, defaultValue, EnumSet.allOf(clazz));
  }

  private static <T> Optional<Double> extractOptional(
      EnumMap<CLIOption, String> options, CLIOption option) {
    if (options.containsKey(option)) {
      return Optional.of(Double.parseDouble(options.get(option)));
    }
    return Optional.<Double> absent();
  }

  public static void main(String[] args) {

    EnumMap<CLIOption, String> options = parseInput(args);
    System.out.println(options);
    boolean fullUserCuts = extractOrDefault(options, CLIOption.fullUserCut,
        true);
    int numThreads = (int) extractOrDefault(options, CLIOption.numThreads, 1);
    SolverOption lpFormulation = extractOrDefault(options,
        CLIOption.formulation, SolverOption.cutsetMode,
        SolverOption.constriantModes);
    Optional<Double> maxSolveTimeMs = extractOptional(options,
        CLIOption.maxTimeSeconds);
    Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
    ensureOptionsContain(options,
        EnumSet.of(CLIOption.mode, CLIOption.kepIn, CLIOption.optPackingOut));
    try {
      String modeName = options.get(CLIOption.mode);
      String kepInFile = options.get(CLIOption.kepIn);
      String optPackingOutFile = options.get(CLIOption.optPackingOut);

      if (modeName.equalsIgnoreCase(deterministicMode)) {
        try {
          KepInstance<Node, Edge> kep = KepTextReaderWriter.INSTANCE
              .read(kepInFile);
          ImmutableSet<SolverOption> solverOptions;
          if (fullUserCuts) {
            solverOptions = SolverOption.makeCheckedOptions(lpFormulation,
                SolverOption.lazyConstraintCallback,
                SolverOption.userCutCallback);
          } else {
            solverOptions = SolverOption.makeCheckedOptions(lpFormulation,
                SolverOption.lazyConstraintCallback,
                SolverOption.userCutCallback, SolverOption.disableFullUserCut);
          }
          CycleChainPackingSubtourElimination<Node, Edge> solver = new CycleChainPackingSubtourElimination<Node, Edge>(
              kep, true, maxSolveTimeMs, threadPool, solverOptions);
          solver.solve();
          CycleChainDecomposition<Node, Edge> solution = solver.getSolution();
          solver.cleanUp();
          KepTextReaderWriter.INSTANCE.writeSolution(kep, solution,
              KepParseData.toStringFunction, optPackingOutFile);
        } catch (IloException e) {
          throw new RuntimeException(e);
        }

      } else if (modeName.equalsIgnoreCase(twoStageMode)) {
        ensureOptionsContain(options, EnumSet.of(CLIOption.phaseOneOut));
        CLIOption edgeRealizationMode = ensureExactlyOne(options, EnumSet.of(
            CLIOption.edgeRealizationIn, CLIOption.edgeRealizationOut));
        boolean edgeRealizationIsInput = edgeRealizationMode == CLIOption.edgeRealizationIn;
        CLIOption scenarioMode = ensureExactlyOne(options, EnumSet.of(
            CLIOption.edgeFailureScenariosIn, CLIOption.edgeFailureIn));
        if (scenarioMode == CLIOption.edgeFailureIn && !edgeRealizationIsInput) {
          throw new RuntimeException("Options " + CLIOption.edgeFailureIn
              + " and " + CLIOption.edgeRealizationOut + " are incompatiable");
        }
        String phaseOneOutFile = options.get(CLIOption.phaseOneOut);

        KepParseData<Node, Edge> kepParseData = KepTextReaderWriter.INSTANCE
            .readParseData(kepInFile);
        KepInstance<Node, Edge> kepInstance = kepParseData.getInstance();

        List<EdgeFailureScenario<Node, Edge>> edgeFailureScenarios;
        Map<Edge, Double> edgeFailureProb = null;
        if (scenarioMode == CLIOption.edgeFailureIn) {
          int numScenarios = (int) extractOrDefault(options,
              CLIOption.numScenarios, 20);
          String edgeFailureInFile = options.get(CLIOption.edgeFailureIn);
          edgeFailureProb = KepTextReaderWriter.INSTANCE
              .readEdgeFailureProbability(edgeFailureInFile, kepParseData);
          edgeFailureScenarios = EdgeFailureScenario.generateScenarios(
              kepInstance, edgeFailureProb, numScenarios);
        } else {// scenarioMode = CLIOption.edgeFailureScenariosIn
          String edgeFailureScenarioFile = options
              .get(CLIOption.edgeFailureScenariosIn);
          edgeFailureScenarios = KepTextReaderWriter.INSTANCE
              .readEdgeFailureScenarios(edgeFailureScenarioFile, kepParseData);
        }
        TwoStageEdgeFailureSolver<Node, Edge> solver = TwoStageEdgeFailureSolver
            .truncationSolver(kepInstance, edgeFailureScenarios, threadPool,
                true, maxSolveTimeMs, SolverOption.defaultOptions);
        solver.solve();
        Set<Edge> stageOneSolution = solver.getEdgesInSolution();
        KepTextReaderWriter.INSTANCE.writePhaseOneSolution(stageOneSolution,
            KepParseData.toStringFunction, phaseOneOutFile);
        EdgeFailureScenario<Node, Edge> realization;
        if (edgeRealizationIsInput) {
          String edgeRealizationInFile = options
              .get(CLIOption.edgeRealizationIn);
          List<EdgeFailureScenario<Node, Edge>> realizationAsList = KepTextReaderWriter.INSTANCE
              .readEdgeFailureScenarios(edgeRealizationInFile, kepParseData);
          realization = realizationAsList.get(0);
        } else {
          String edgeRealizationOutFile = options
              .get(CLIOption.edgeRealizationOut);
          List<EdgeFailureScenario<Node, Edge>> realizationAsList = EdgeFailureScenario
              .generateScenarios(kepInstance, edgeFailureProb, 1);
          KepTextReaderWriter.INSTANCE.writeEdgeFailureScenarios(kepInstance,
              realizationAsList, KepParseData.toStringFunction,
              edgeRealizationOutFile);
          realization = realizationAsList.get(0);
        }
        CycleChainDecomposition<Node, Edge> decomposition = solver
            .applySolutionToRealization(realization, true, maxSolveTimeMs,
                threadPool, SolverOption.defaultOptions);
        KepTextReaderWriter.INSTANCE.writeSolution(kepInstance, decomposition,
            KepParseData.toStringFunction, optPackingOutFile);
        solver.cleanUp();

      } else if (modeName.equalsIgnoreCase(minWaitingMode)) {
        ensureOptionsContain(options,
            EnumSet.of(CLIOption.arrivalTimesIn, CLIOption.matchingTimesOut));
        String arrivalTimesInFile = options.get(CLIOption.arrivalTimesIn);

        String matchingTimesOutFile = options.get(CLIOption.matchingTimesOut);
        try {
          KepParseData<Node, Edge> kepParseData = KepTextReaderWriter.INSTANCE
              .readParseData(kepInFile);
          KepInstance<Node, Edge> kep = kepParseData.getInstance();

          MinWaitingTimeProblemData<Node> minWaitingTimeProblemData;
          if (arrivalTimesInFile.equalsIgnoreCase("random")) {
            int maxTime = 100;
            minWaitingTimeProblemData = new MinWaitingTimeProblemData<Node>(
                RandomArrivalTimes.randomArrivalTimes(kep, maxTime),
                (double) maxTime);
          } else {
            minWaitingTimeProblemData = KepTextReaderWriter.INSTANCE
                .readNodeArrivalTimes(arrivalTimesInFile, kepParseData);
          }
          ImmutableSet<SolverOption> solverOptions;
          if (fullUserCuts) {
            solverOptions = SolverOption.makeCheckedOptions(lpFormulation,
                SolverOption.lazyConstraintCallback,
                SolverOption.userCutCallback);
          } else {
            solverOptions = SolverOption.makeCheckedOptions(lpFormulation,
                SolverOption.lazyConstraintCallback,
                SolverOption.userCutCallback, SolverOption.disableFullUserCut);
          }
          MinWaitingTimeKepSolver<Node, Edge> solver = new MinWaitingTimeKepSolver<Node, Edge>(
              kep, minWaitingTimeProblemData.getNodeArrivalTime(),
              minWaitingTimeProblemData.getTerminationTime(), true,
              maxSolveTimeMs, threadPool, solverOptions);
          solver.solve();
          CycleChainDecomposition<Node, Edge> solution = solver.getSolution();
          Map<Node, Double> matchingTimes = solver.getMatchingTimes();
          solver.cleanUp();
          KepTextReaderWriter.INSTANCE.writeSolution(kep, solution,
              KepParseData.toStringFunction, optPackingOutFile);
          KepTextReaderWriter.INSTANCE.writeNodeMatchingTimes(
              matchingTimesOutFile, matchingTimes,
              KepParseData.toStringFunction);
        } catch (IloException e) {
          throw new RuntimeException(e);
        }
      } else {
        throw new RuntimeException("Unrecognized mode, should be either "
            + deterministicMode + " or " + twoStageMode + " but was" + modeName);
      }
    } catch (IloException e) {
      throw new RuntimeException(e);
    } finally {
      FixedThreadPool.shutDown(threadPool);
    }
  }

  private static void ensureOptionsContain(
      EnumMap<CLIOption, String> optionMap, EnumSet<CLIOption> mandatoryOptions) {
    for (CLIOption mand : mandatoryOptions) {
      if (!optionMap.containsKey(mand)) {
        throw new RuntimeException("Option: " + mand
            + " is required but was not set.");
      }
    }
  }

  private static CLIOption ensureExactlyOne(
      EnumMap<CLIOption, String> optionMap, EnumSet<CLIOption> orSet) {
    Set<CLIOption> intersection = Sets.intersection(orSet, optionMap.keySet());
    if (intersection.size() != 1) {
      throw new RuntimeException("Expected exactly one of " + orSet
          + " in keys of " + optionMap + ", but found " + intersection);
    } else {
      return intersection.iterator().next();
    }
  }

  public static enum CLIOption {
    mode, kepIn, edgeFailureIn, edgeFailureScenariosIn, optPackingOut, phaseOneOut, edgeRealizationIn, edgeRealizationOut, arrivalTimesIn, matchingTimesOut, numThreads, maxTimeSeconds, numScenarios, formulation, fullUserCut;
  }

  private static CLIOption parseOption(String optionText) {
    if (optionText.length() == 0) {
      throw new RuntimeException("Option names must be non empty");
    }
    if (optionText.charAt(0) != '-') {
      throw new RuntimeException(
          "Option name must begin with a -, but input was: " + optionText);
    }
    String toMatch = optionText.substring(1);
    for (CLIOption option : CLIOption.values()) {
      if (option.toString().equalsIgnoreCase(toMatch)) {
        return option;
      }
    }
    throw new RuntimeException("Could not find a match for option: "
        + optionText);
  }

  private static EnumMap<CLIOption, String> parseInput(String[] args) {
    if (args.length % 2 != 0) {
      throw new RuntimeException("Must have an even number of arugments");
    }
    EnumMap<CLIOption, String> ans = new EnumMap<CLIOption, String>(
        CLIOption.class);
    for (int i = 0; i < args.length; i += 2) {
      String optionName = args[i];
      CLIOption opt = parseOption(optionName);
      String optionValue = args[i + 1];
      ans.put(opt, optionValue);
    }
    return ans;
  }

}
