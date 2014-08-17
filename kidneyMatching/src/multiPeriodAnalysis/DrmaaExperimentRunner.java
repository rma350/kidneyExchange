package multiPeriodAnalysis;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepProtos.KepProtos.RandomTrial;
import kepProtos.KepProtos.Simulation;
import kepProtos.KepProtos.SimulationInputs;
import kepProtos.KepProtos.SimulationJob;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

public class DrmaaExperimentRunner<V, E, T extends Comparable<T>> implements
    ExperimentRunner<V, E, T> {

  private Environment<V, E, T> environment;
  private Session session;
  private JobTemplate jt;
  private SecureRandom secureRandom;

  private String cplexLocation = "/opt/ibm/ILOG/CPLEX_Studio125/cplex/bin/x86-64_sles10_4.1/";
  private String drmaaLocation = "/opt/uge816/lib/lx-amd64/";
  private String workingDir;
  private String tmpDir;
  private String programLocation;

  public DrmaaExperimentRunner(Environment<V, E, T> environment) {
    this.environment = environment;
    this.workingDir = java.lang.System.getProperty("user.dir");
    tmpDir = workingDir + File.separator + "tmp" + File.separator;
    // = System.getProperty("java.io.tmpdir");
    safeMakeDir(tmpDir);
    this.programLocation = workingDir + "/unosAnalysis125.jar";

    SessionFactory factory = SessionFactory.getFactory();
    session = factory.getSession();
    secureRandom = new SecureRandom();
    try {
      session.init("");
      jt = session.createJobTemplate();
      jt.setRemoteCommand("java");
      jt.setWorkingDirectory(workingDir);
    } catch (DrmaaException e) {
      System.out.println("Error: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @Override
  public Simulation runExperiment(SimulationInputs simulationInputs,
      List<RandomTrial> randomTrials) {
    List<SimulationDrmaaJobInfo> jobs = makeJobs(simulationInputs, randomTrials);
    List<String> jobIds = submitJobs(jobs);
    try {
      session.synchronize(jobIds, Session.TIMEOUT_WAIT_FOREVER, true);
    } catch (DrmaaException e) {
      throw new RuntimeException(e);
    }
    System.out.println("All jobs finished");
    return mergeResults(jobs, 0);
  }

  private Simulation mergeResults(List<SimulationDrmaaJobInfo> completedJobs,
      int expectedStartInRandomTrials) {
    Simulation.Builder ans = Simulation.newBuilder();
    for (SimulationDrmaaJobInfo job : completedJobs) {
      String resultsFile = getSimResultNameForId(job.getOutFileId());
      Simulation result = safeRead(resultsFile);
      if (!ans.hasSimulationInputs()) {
        ans.setSimulationInputs(result.getSimulationInputs());
      }
      if (result.hasHistoricDynamicMatching()) {
        if (ans.hasHistoricDynamicMatching()) {
          throw new RuntimeException("Historic matching found twice");
        } else {
          ans.setHistoricDynamicMatching(result.getHistoricDynamicMatching());
        }
      }
      if (result.getRandomTrialDynamicMatchingCount() > 0) {
        if (ans.getRandomTrialDynamicMatchingCount()
            + expectedStartInRandomTrials != result.getIndexWithinAllTrials()) {
          throw new RuntimeException("Next random trial to add was at index "
              + result.getIndexWithinAllTrials()
              + " but had only found to index: "
              + ans.getRandomTrialDynamicMatchingCount() + " so far");
        }
        ans.addAllRandomTrial(result.getRandomTrialList());
        ans.addAllRandomTrialDynamicMatching(result
            .getRandomTrialDynamicMatchingList());
      }
    }
    return ans.build();
  }

  private Simulation safeRead(String simulationFile) {
    long msToWait = 2000;
    File file = new File(simulationFile);
    while (!file.exists()) {
      System.out.println("waiting for file system to show " + simulationFile
          + ".  Sleeping " + msToWait + "ms");
      try {
        Thread.sleep(msToWait);
      } catch (InterruptedException e) {
      }
      msToWait = 2 * msToWait;
    }
    if (file.exists()) {
      try {
        return Simulation.parseFrom(Files.toByteArray(file));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException();
    }
  }

  @Override
  public ImmutableMap<SimulationInputs, Simulation> runExperiments(
      Set<SimulationInputs> simulationInputsSet, List<RandomTrial> randomTrials) {
    Map<SimulationInputs, List<SimulationDrmaaJobInfo>> jobsByInput = Maps
        .newHashMap();
    List<String> allJobIds = Lists.newArrayList();
    for (SimulationInputs experiment : simulationInputsSet) {
      List<SimulationDrmaaJobInfo> jobs = makeJobs(experiment, randomTrials);
      jobsByInput.put(experiment, jobs);
      allJobIds.addAll(submitJobs(jobs));
    }
    try {
      session.synchronize(allJobIds, Session.TIMEOUT_WAIT_FOREVER, true);
    } catch (DrmaaException e) {
      throw new RuntimeException(e);
    }
    System.out.println("All jobs finished");
    ImmutableMap.Builder<SimulationInputs, Simulation> ans = ImmutableMap
        .builder();
    for (Map.Entry<SimulationInputs, List<SimulationDrmaaJobInfo>> entry : jobsByInput
        .entrySet()) {
      ans.put(entry.getKey(), mergeResults(entry.getValue(), 0));
    }
    return ans.build();
  }

  private List<SimulationDrmaaJobInfo> makeJobs(
      SimulationInputs simulationInputs, List<RandomTrial> randomTrials) {
    List<SimulationDrmaaJobInfo> ans = Lists.newArrayList();
    ans.add(new SimulationDrmaaJobInfo(getSimJobName(), SimulationJob
        .newBuilder().setSimulationInputs(simulationInputs)
        .setRunHistoric(true).build()));
    for (int i = 0; i < randomTrials.size(); i++) {
      ans.add(new SimulationDrmaaJobInfo(getSimJobName(), SimulationJob
          .newBuilder().setSimulationInputs(simulationInputs)
          .setRunHistoric(false).addRandomTrial(randomTrials.get(i))
          .setIndexWithinAllTrials(i).build()));
    }
    return ans;
  }

  private List<SimulationDrmaaJobInfo> splitInputJob(SimulationJob simulationJob) {
    List<SimulationDrmaaJobInfo> ans = Lists.newArrayList();
    if (simulationJob.getRunHistoric()) {
      new SimulationDrmaaJobInfo(getSimJobName(), SimulationJob.newBuilder()
          .setSimulationInputs(simulationJob.getSimulationInputs())
          .setRunHistoric(true).build());
    }
    for (int i = 0; i < simulationJob.getRandomTrialCount(); i++) {
      ans.add(new SimulationDrmaaJobInfo(getSimJobName(), SimulationJob
          .newBuilder()
          .setSimulationInputs(simulationJob.getSimulationInputs())
          .setRunHistoric(false)
          .addRandomTrial(simulationJob.getRandomTrial(i))
          .setIndexWithinAllTrials(i + simulationJob.getIndexWithinAllTrials())
          .build()));
    }
    return ans;
  }

  private List<String> submitJobs(List<SimulationDrmaaJobInfo> jobs) {
    List<String> ans = Lists.newArrayList();
    for (SimulationDrmaaJobInfo job : jobs) {
      String jobFileName = getSimJobNameForId(job.getOutFileId());
      try {
        Files
            .write(job.getSimulationJob().toByteArray(), new File(jobFileName));
      } catch (IOException e1) {
        throw new RuntimeException(e1);
      }
      try {
        jt.setErrorPath(":" + getStdErrorNameForId(job.getOutFileId()));
        jt.setOutputPath(":" + getStdOutNameForId(job.getOutFileId()));
        List<String> args = Lists.newArrayList("-Djava.library.path="
            + drmaaLocation + ":" + cplexLocation, "-jar", programLocation,
            "-m", "worker", "-t", Integer.toString(environment.getThreads()),
            "-s", jobFileName, "-o", getSimResultNameForId(job.getOutFileId()));
        jt.setArgs(args);
        String id = session.runJob(jt);
        ans.add(id);
        job.setDrmaaId(id);
      } catch (DrmaaException e) {
        System.out.println("Error: " + e.getMessage());
        throw new RuntimeException(e);
      }
    }
    return ans;
  }

  private long getSimJobName() {
    return getUniqueFileName(simJobPrefix, simJobSuffix, 5);
  }

  private static final String simJobPrefix = "simJob";
  private static final String simJobSuffix = ".buf";

  private String getSimJobNameForId(long id) {
    return getFileName(simJobPrefix, id, simJobSuffix);
  }

  private static final String simResultPrefix = "simResult";
  private static final String simResultSuffix = ".buf";

  private String getSimResultNameForId(long id) {
    return getFileName(simResultPrefix, id, simResultSuffix);
  }

  private String getStdErrorNameForId(long id) {
    return getFileName("stderror", id, ".txt");
  }

  private String getStdOutNameForId(long id) {
    return getFileName("stdout", id, ".txt");
  }

  private String getFileName(String prefix, long id, String suffix) {
    return tmpDir + prefix + Long.toString(id) + suffix;
  }

  private long getUniqueFileName(String prefix, String suffix, int maxAttempt) {
    for (int i = 0; i < maxAttempt; i++) {
      long n = secureRandom.nextLong();
      if (n == Long.MIN_VALUE) {
        n = 0; // corner case
      } else {
        n = Math.abs(n);
      }
      String proposedName = getFileName(prefix, n, suffix);
      if (!new File(proposedName).exists()) {
        return n;
      } else {
        System.err
            .println("Warning: randomly generated file name was not unique, this is very unlikely: "
                + proposedName);
      }
    }
    throw new RuntimeException("Made " + maxAttempt
        + " attempts to create a temporary file but failed.");
  }

  @Override
  public void terminate() {
    try {
      session.deleteJobTemplate(jt);
      session.exit();
    } catch (DrmaaException e) {
      System.out.println("Error: " + e.getMessage());
      throw new RuntimeException(e);
    }

  }

  private static class SimulationDrmaaJobInfo {
    private String drmaaId;
    private long outFileId;
    private SimulationJob simulationJob;

    public String getDrmaaId() {
      return drmaaId;
    }

    public void setDrmaaId(String drmaaId) {
      this.drmaaId = drmaaId;
    }

    public long getOutFileId() {
      return outFileId;
    }

    public SimulationJob getSimulationJob() {
      return simulationJob;
    }

    public SimulationDrmaaJobInfo(long outFileId, SimulationJob simulationJob) {
      super();
      this.outFileId = outFileId;
      this.simulationJob = simulationJob;
    }

  }

  private static void safeMakeDir(String dirName) {
    File dir = new File(dirName);
    if (!dir.isDirectory()) {
      dir.mkdirs();
    }
  }

  @Override
  public Simulation runSimulationJob(SimulationJob simulationJob) {
    List<SimulationDrmaaJobInfo> jobs = splitInputJob(simulationJob);
    List<String> jobIds = submitJobs(jobs);
    try {
      session.synchronize(jobIds, Session.TIMEOUT_WAIT_FOREVER, true);
    } catch (DrmaaException e) {
      throw new RuntimeException(e);
    }
    System.out.println("All jobs finished");
    return mergeResults(jobs, simulationJob.getIndexWithinAllTrials());
  }

  @Override
  public ImmutableMap<SimulationJob, Simulation> runSimulationJobs(
      Set<SimulationJob> simulationJobs) {
    Map<SimulationJob, List<SimulationDrmaaJobInfo>> jobsByInput = Maps
        .newHashMap();
    List<String> allJobIds = Lists.newArrayList();

    for (SimulationJob experiment : simulationJobs) {
      List<SimulationDrmaaJobInfo> jobs = splitInputJob(experiment);
      jobsByInput.put(experiment, jobs);
      allJobIds.addAll(submitJobs(jobs));
    }
    try {
      session.synchronize(allJobIds, Session.TIMEOUT_WAIT_FOREVER, true);
    } catch (DrmaaException e) {
      throw new RuntimeException(e);
    }
    System.out.println("All jobs finished");
    ImmutableMap.Builder<SimulationJob, Simulation> ans = ImmutableMap
        .builder();
    for (Map.Entry<SimulationJob, List<SimulationDrmaaJobInfo>> entry : jobsByInput
        .entrySet()) {
      ans.put(
          entry.getKey(),
          mergeResults(entry.getValue(), entry.getKey()
              .getIndexWithinAllTrials()));
    }
    return ans.build();
  }

}
