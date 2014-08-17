package multiPeriodAnalysis;

import java.util.List;
import java.util.Set;

import kepProtos.KepProtos.RandomTrial;
import kepProtos.KepProtos.Simulation;
import kepProtos.KepProtos.SimulationInputs;
import kepProtos.KepProtos.SimulationJob;

import com.google.common.collect.ImmutableMap;

public interface ExperimentRunner<V, E, T extends Comparable<T>> {

  /** Will block until job completes. */
  public Simulation runExperiment(SimulationInputs simulationInputs,
      List<RandomTrial> randomTrials);

  /** Will block until job completes. */
  public ImmutableMap<SimulationInputs, Simulation> runExperiments(
      Set<SimulationInputs> simulationInputsSet, List<RandomTrial> randomTrials);

  /** Will block until job completes. */
  public Simulation runSimulationJob(SimulationJob simulationJob);

  /** Will block until job completes. */
  public ImmutableMap<SimulationJob, Simulation> runSimulationJobs(
      Set<SimulationJob> simulationJobs);

  public void terminate();

}
