package multiPeriodAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import kepModeler.KepModeler;
import kepProtos.KepProtos;
import kepProtos.KepProtos.RandomTrial;
import kepProtos.KepProtos.Simulation;
import kepProtos.KepProtos.SimulationInputs;
import kepProtos.KepProtos.SimulationJob;
import multiPeriod.CycleChainPackingFactory;
import multiPeriod.CycleChainPackingSubtourEliminationFactory;
import multiPeriod.DynamicMatching;
import multiPeriod.MultiPeriodCyclePacking;
import multiPeriod.MultiPeriodCyclePackingIpSolver;
import multiPeriod.TimeInstant;
import multiPeriod.TimeWriter;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

public class LocalExperimentRunner<V, E, T extends Comparable<T>> implements
    ExperimentRunner<V, E, T> {

  private Environment<V, E, T> environment;

  public LocalExperimentRunner(Environment<V, E, T> environment) {
    this.environment = environment;
  }

  @Override
  public Simulation runExperiment(SimulationInputs simulationInputs,
      List<RandomTrial> randomTrials) {
    Simulation.Builder ans = Simulation.newBuilder()
        .setSimulationInputs(simulationInputs).addAllRandomTrial(randomTrials);
    ans.setHistoricDynamicMatching(runHistoric(simulationInputs));
    for (RandomTrial randomTrial : randomTrials) {
      ans.addRandomTrialDynamicMatching(runRandomTrial(simulationInputs,
          randomTrial));
    }
    ans.setIndexWithinAllTrials(0);
    return ans.build();
  }

  @Override
  public ImmutableMap<SimulationInputs, Simulation> runExperiments(
      Set<SimulationInputs> simulationInputsSet, List<RandomTrial> randomTrials) {
    ImmutableMap.Builder<SimulationInputs, Simulation> ans = ImmutableMap
        .builder();
    for (SimulationInputs inputs : simulationInputsSet) {
      ans.put(inputs, runExperiment(inputs, randomTrials));
    }
    return ans.build();
  }

  public Simulation runSimulationJob(SimulationJob simulationJob) {
    SimulationInputs simulationInputs = simulationJob.getSimulationInputs();
    Simulation.Builder ans = Simulation.newBuilder()
        .setSimulationInputs(simulationInputs)
        .addAllRandomTrial(simulationJob.getRandomTrialList());
    if (simulationJob.getRunHistoric()) {
      ans.setHistoricDynamicMatching(runHistoric(simulationInputs));
    }
    for (RandomTrial randomTrial : simulationJob.getRandomTrialList()) {
      ans.addRandomTrialDynamicMatching(runRandomTrial(simulationInputs,
          randomTrial));
    }
    ans.setIndexWithinAllTrials(simulationJob.getIndexWithinAllTrials());
    return ans.build();
  }

  public SimulationJob safeRead(String simulationJobFile) {
    long msToWait = 2000;
    File file = new File(simulationJobFile);
    while (!file.exists()) {
      System.out.println("waiting for file system to show " + simulationJobFile
          + ".  Sleeping " + msToWait + "ms");
      try {
        Thread.sleep(msToWait);
      } catch (InterruptedException e) {
      }
      msToWait = 2 * msToWait;
    }
    if (file.exists()) {
      try {
        return SimulationJob.parseFrom(Files.toByteArray(file));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException();
    }
  }

  public KepProtos.DynamicMatching runHistoric(SimulationInputs simulationInputs) {
    MultiPeriodCyclePacking<V, E, T> packing = new MultiPeriodCyclePackingIpSolver<V, E, T>(
        environment.getMultiPeriodInputs(),
        makeMatchingTimes(simulationInputs), makeFactory(simulationInputs),
        null);
    return runMultiPeriod(packing);
  }

  private KepProtos.DynamicMatching runMultiPeriod(
      MultiPeriodCyclePacking<V, E, T> packing) {
    packing.computeAllMatchings();
    DynamicMatching<V, E, T> out = packing.getDynamicMatching();
    KepProtos.DynamicMatching protoMatching = environment
        .buildProtoDynamicMatching(out);
    return protoMatching;
  }

  public KepProtos.DynamicMatching runRandomTrial(
      SimulationInputs simulationInputs, RandomTrial randomTrial) {
    MultiPeriodCyclePacking<V, E, T> packing = new MultiPeriodCyclePackingIpSolver<V, E, T>(
        environment.alternateArrivalTimes(randomTrial),
        makeMatchingTimes(simulationInputs), makeFactory(simulationInputs),
        null);
    return runMultiPeriod(packing);
  }

  private List<TimeInstant<T>> makeMatchingTimes(
      SimulationInputs simulationInputs) {
    TimeWriter<T> time = environment.getTimeWriter();
    List<TimeInstant<T>> matchingTimes = time.makeMatchingTimes(
        time.read(simulationInputs.getSimulationStartTime()),
        time.read(simulationInputs.getSimulationEndTime()),
        simulationInputs.getMatchingFrequency());
    return matchingTimes;
  }

  private CycleChainPackingFactory<V, E> makeFactory(
      SimulationInputs simulationInputs) {
    KepModeler modeler = environment.createModeler(simulationInputs
        .getKepModelerInputs());
    CycleChainPackingFactory<V, E> factory = new CycleChainPackingSubtourEliminationFactory<V, E>(
        modeler, false, environment.getMaxSolveTimeSeconds(),
        environment.getThreadPool(), environment.getMultiPeriodInputs()
            .getAuxiliaryInputStatistics(), environment.getSolverOptions(),
        environment.getPredicateFactory());
    return factory;
  }

  @Override
  public void terminate() {
  }

  @Override
  public ImmutableMap<SimulationJob, Simulation> runSimulationJobs(
      Set<SimulationJob> simulationJobs) {
    ImmutableMap.Builder<SimulationJob, Simulation> ans = ImmutableMap
        .builder();
    for (SimulationJob job : simulationJobs) {
      ans.put(job, this.runSimulationJob(job));
    }
    return ans.build();
  }

}
