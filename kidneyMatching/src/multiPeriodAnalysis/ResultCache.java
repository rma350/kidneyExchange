package multiPeriodAnalysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepProtos.KepProtos.RandomTrial;
import kepProtos.KepProtos.Simulation;
import kepProtos.KepProtos.SimulationInputs;
import kepProtos.KepProtos.SimulationJob;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class ResultCache<V, E, T extends Comparable<T>> {

  private Environment<V, E, T> environment;
  private Map<Integer, RandomTrial> randomTrialCache;
  private Map<SimulationInputs, Simulation> simulationCache;
  private Map<SimulationInputs, String> simulationFiles;
  private String trialCacheDir;
  private String simulationCacheDir;
  private String cacheFileExtension = "buf";
  private ExperimentRunner<V, E, T> experimentRunner;

  public ResultCache(Environment<V, E, T> environment,
      String randomTrialCacheDir, String simulationCacheDir,
      ExperimentRunner<V, E, T> experimentRunner) {
    this.simulationFiles = Maps.newHashMap();
    this.trialCacheDir = randomTrialCacheDir;
    this.simulationCacheDir = simulationCacheDir;
    this.environment = environment;
    randomTrialCache = loadTrialCache(randomTrialCacheDir);
    simulationCache = loadSimulationCache(simulationCacheDir);
    this.experimentRunner = experimentRunner;
  }

  private RandomTrial getRandomTrial(int index) {
    if (randomTrialCache.containsKey(index)) {
      System.out.println("trial hit" + index);
      return randomTrialCache.get(index);
    } else {
      System.out.println("trial miss " + index);
      RandomTrial ans = NodeShuffleRandomTrial.makeRandomTrials(
          this.environment, 1).get(0);
      randomTrialCache.put(index, ans);
      try {
        OutputStream out = new FileOutputStream(this.trialCacheDir + index
            + "." + cacheFileExtension);
        ans.writeTo(out);
        out.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      return ans;
    }
  }

  private List<RandomTrial> getRandomTrials(int numTrials) {
    List<RandomTrial> ans = Lists.newArrayList();
    for (int i = 0; i < numTrials; i++) {
      ans.add(getRandomTrial(i));
    }
    return ans;
  }

  public ImmutableMap<SimulationInputs, Simulation> getAllSimulationResults(
      Set<SimulationInputs> simulationInputs) {
    // Set<SimulationInputs> simulationsToRun = Sets.newHashSet();
    Set<SimulationJob> simulationsToRun = Sets.newHashSet();
    ImmutableMap.Builder<SimulationInputs, Simulation> ans = ImmutableMap
        .builder();
    for (SimulationInputs inputs : simulationInputs) {
      int maxTrial = environment.getReplications();
      if (simulationCache.containsKey(inputs)) {
        int trialsAlreadyRun = simulationCache.get(inputs)
            .getRandomTrialCount();
        if (trialsAlreadyRun >= maxTrial) {
          System.out.println("Cache hit:");
          System.out.println(inputs.toString());
          ans.put(inputs, simulationCache.get(inputs));
        } else {
          System.out.println("Partial cache hit, found " + trialsAlreadyRun
              + " trials but requested " + maxTrial);
          System.out.println(inputs.toString());

          List<RandomTrial> trialsToRun = Lists.newArrayList(getRandomTrials(
              maxTrial).subList(trialsAlreadyRun, maxTrial));
          simulationsToRun.add(SimulationJob.newBuilder()
              .setSimulationInputs(inputs).setRunHistoric(false)
              .addAllRandomTrial(trialsToRun)
              .setIndexWithinAllTrials(trialsAlreadyRun).build());
        }
      } else {
        System.out.println("Cache miss:");
        System.out.println(inputs.toString());
        simulationsToRun.add(SimulationJob.newBuilder()
            .setSimulationInputs(inputs).setRunHistoric(true)
            .addAllRandomTrial(getRandomTrials(maxTrial))
            .setIndexWithinAllTrials(0).build());
      }
    }
    System.out.println("Must run " + simulationsToRun.size() + " out of "
        + simulationInputs.size() + " simulations, others cached");
    ImmutableMap<SimulationJob, Simulation> experimentResults = this.experimentRunner
        .runSimulationJobs(simulationsToRun);
    for (Map.Entry<SimulationJob, Simulation> result : experimentResults
        .entrySet()) {

      if (this.simulationCache.containsKey(result.getKey()
          .getSimulationInputs())) {
        // we need to merge the new results with the old results
        Simulation newSim = result.getValue();
        Simulation oldSim = simulationCache.get(result.getKey()
            .getSimulationInputs());
        Simulation merged = oldSim
            .toBuilder()
            .addAllRandomTrial(newSim.getRandomTrialList())
            .addAllRandomTrialDynamicMatching(
                newSim.getRandomTrialDynamicMatchingList()).build();
        try {
          Files.write(merged.toByteArray(),
              new File(simulationFiles.get(merged.getSimulationInputs())));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        this.simulationCache.put(result.getKey().getSimulationInputs(), merged);
        ans.put(result.getKey().getSimulationInputs(), merged);

      } else {
        // we can just write a new file to the cache
        String uniqueName = Integer.toString(new File(this.simulationCacheDir)
            .listFiles().length);
        String fullName = this.simulationCacheDir + uniqueName + "."
            + cacheFileExtension;
        try {
          Files.write(result.getValue().toByteArray(), new File(fullName));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        this.simulationCache.put(result.getKey().getSimulationInputs(),
            result.getValue());
        this.simulationFiles.put(result.getKey().getSimulationInputs(),
            fullName);
        ans.put(result.getKey().getSimulationInputs(), result.getValue());
      }

    }
    return ans.build();

  }

  /** Will block on each input. */
  @Deprecated
  public Simulation getSimulationResults(SimulationInputs inputs) {
    if (simulationCache.containsKey(inputs)) {
      System.out.println("simulation hit");
      return simulationCache.get(inputs);
    } else {
      System.out.println("simulation miss");
      System.out.println("requested: ");
      System.out.println(inputs.toString());
      System.out.println("cached: ");
      Simulation ans = this.experimentRunner.runExperiment(inputs,
          getRandomTrials(environment.getReplications()));
      // Simulation ans = RunExperiment.run(inputs,
      // getRandomTrials(environment.getReplications()), environment);
      simulationCache.put(inputs, ans);
      try {
        String uniqueName = Integer.toString(new File(this.simulationCacheDir)
            .listFiles().length);
        OutputStream out = new FileOutputStream(this.simulationCacheDir
            + uniqueName + "." + cacheFileExtension);
        ans.writeTo(out);
        out.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return ans;
    }
  }

  private Map<SimulationInputs, Simulation> loadSimulationCache(String dir) {
    safeMakeDir(dir);
    File cache = new File(dir);
    Map<SimulationInputs, Simulation> inCache = Maps.newHashMap();
    for (File file : cache.listFiles()) {
      String simpleFileName = file.getName();
      if (file.isFile()
          && Files.getFileExtension(simpleFileName)
              .endsWith(cacheFileExtension)) {
        try {
          InputStream stream = new FileInputStream(file);
          Simulation sim = Simulation.parseFrom(stream);
          stream.close();
          inCache.put(sim.getSimulationInputs(), sim);
          simulationFiles.put(sim.getSimulationInputs(), file.getPath());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return inCache;
  }

  private Map<Integer, RandomTrial> loadTrialCache(String dir) {
    safeMakeDir(dir);
    File cache = new File(dir);
    Map<Integer, RandomTrial> inCache = Maps.newHashMap();
    for (File file : cache.listFiles()) {
      String simpleFileName = file.getName();
      if (file.isFile()
          && Files.getFileExtension(simpleFileName)
              .endsWith(cacheFileExtension)) {
        int index = Integer.parseInt(Files
            .getNameWithoutExtension(simpleFileName));
        try {
          InputStream stream = new FileInputStream(file);
          inCache.put(index, RandomTrial.parseFrom(stream));
          stream.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return inCache;
  }

  private static void safeMakeDir(String dirName) {
    File dir = new File(dirName);
    if (!dir.isDirectory()) {
      dir.mkdirs();
    }
  }

}
