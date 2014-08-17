package multiPeriodAnalysis;

import java.util.List;

import kepModeler.KepModeler;
import kepProtos.KepProtos;
import kepProtos.KepProtos.RandomTrial;
import kepProtos.KepProtos.Simulation;
import kepProtos.KepProtos.SimulationInputs;
import multiPeriod.CycleChainPackingFactory;
import multiPeriod.CycleChainPackingSubtourEliminationFactory;
import multiPeriod.DynamicMatching;
import multiPeriod.MultiPeriodCyclePacking;
import multiPeriod.MultiPeriodCyclePackingIpSolver;
import multiPeriod.TimeInstant;
import multiPeriod.TimeWriter;

@Deprecated
public class RunExperiment {

  @Deprecated
  public static <V, E, T extends Comparable<T>> Simulation run(
      SimulationInputs simulationInputs, List<RandomTrial> randomTrials,
      Environment<V, E, T> environment) {
    Simulation.Builder ans = Simulation.newBuilder()
        .setSimulationInputs(simulationInputs).addAllRandomTrial(randomTrials);
    KepModeler modeler = environment.createModeler(simulationInputs
        .getKepModelerInputs());
    CycleChainPackingFactory<V, E> factory = new CycleChainPackingSubtourEliminationFactory<V, E>(
        modeler, false, environment.getMaxSolveTimeSeconds(),
        environment.getThreadPool(), environment.getMultiPeriodInputs()
            .getAuxiliaryInputStatistics(), environment.getSolverOptions(),
        environment.getPredicateFactory());
    TimeWriter<T> time = environment.getTimeWriter();
    List<TimeInstant<T>> matchingTimes = time.makeMatchingTimes(
        time.read(simulationInputs.getSimulationStartTime()),
        time.read(simulationInputs.getSimulationEndTime()),
        simulationInputs.getMatchingFrequency());
    // run historic
    {
      MultiPeriodCyclePacking<V, E, T> packing = new MultiPeriodCyclePackingIpSolver<V, E, T>(
          environment.getMultiPeriodInputs(), matchingTimes, factory, null);
      packing.computeAllMatchings();
      DynamicMatching<V, E, T> out = packing.getDynamicMatching();
      KepProtos.DynamicMatching protoMatching = environment
          .buildProtoDynamicMatching(out);
      // DynamicMatching<V, E, T> restored = new DynamicMatching<V, E, T>(
      // environment.getMultiPeriodInputs().getGraph(), protoMatching, time,
      // environment.getNodeIds(), environment.getEdgeIds());

      ans.setHistoricDynamicMatching(protoMatching);
    }
    // run shuffled
    int i = 0;
    for (RandomTrial randomTrial : randomTrials) {
      System.out.println("Trial " + ++i + " of " + randomTrials.size());
      ans.addRandomTrial(randomTrial);
      MultiPeriodCyclePacking<V, E, T> packing = new MultiPeriodCyclePackingIpSolver<V, E, T>(
          environment.alternateArrivalTimes(randomTrial), matchingTimes,
          factory, null);
      packing.computeAllMatchings();
      ans.addRandomTrialDynamicMatching(environment
          .buildProtoDynamicMatching(packing.getDynamicMatching()));
    }
    return ans.build();
  }

}
