package main;

import java.io.File;
import java.io.IOException;
import java.util.List;

import kepProtos.KepProtos.SimulationJob;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

public class UnosAnalysisCLI {

  @Parameter(names = { "--mode", "-m" }, description = "Execution Mode", converter = ExecutionModeConverter.class, validateWith = ExecutionModeValidator.class)
  private ExecutionMode executionMode = ExecutionMode.LOCAL;

  @Parameter(names = { "--threads", "-t" }, description = "Number of threads per instance", validateWith = PositiveInteger.class)
  private int threads = 1;

  @Parameter(names = { "--replications", "-r" }, description = "Random Trials per simulation (master and local mode only)", validateWith = PositiveInteger.class)
  private int replications = 1;

  @Parameter(names = { "--experiment", "-e" }, description = "List of experiments to run (master and local mode only)", validateWith = ExperimentValidator.class)
  private List<String> experiments = Lists.newArrayList();

  // , converter = SimulationJobConverter.class, validateWith =
  // FileValidator.class)
  // leave as a file, read later, in case file isn't present on network file
  // system yet.
  @Parameter(names = { "--simulationJob", "-s" }, description = "(Worker mode only) SimulationJob protocol buffer file to run")
  private String simulationJob = null;

  @Parameter(names = { "--outputSimulation", "-o" }, description = "(Worker mode only) The solution where the output will be written to", validateWith = NoFileExistsValidator.class)
  private String outputSimulation = null;

  public ExecutionMode getExecutionMode() {
    return executionMode;
  }

  public int getThreads() {
    return threads;
  }

  public int getReplications() {
    return replications;
  }

  public String getSimulationJob() {
    return simulationJob;
  }

  public List<String> getExperiments() {
    return experiments;
  }

  public String getOutputSimulation() {
    return this.outputSimulation;
  }

  public static ImmutableSet<String> getExperimentNames() {
    return experimentNames;
  }

  public static enum ExecutionMode {
    LOCAL, DRMAA_MASTER, DRMAA_WORKER;
  }

  public static final ImmutableBiMap<String, ExecutionMode> executionModes = ImmutableBiMap
      .of("local", ExecutionMode.LOCAL, "master", ExecutionMode.DRMAA_MASTER,
          "worker", ExecutionMode.DRMAA_WORKER);

  public static final class ExecutionModeConverter implements
      IStringConverter<ExecutionMode> {
    @Override
    public ExecutionMode convert(String value) {
      return executionModes.get(value);
    }
  }

  public static final class ExecutionModeValidator implements
      IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
      if (!executionModes.containsKey(value)) {
        throw new ParameterException("Parameter " + name + "must be in "
            + executionModes.keySet() + " but was " + value);
      }
    }
  }

  public static final class SimulationJobConverter implements
      IStringConverter<SimulationJob> {
    @Override
    public SimulationJob convert(String value) {
      try {
        return SimulationJob.parseFrom(Files.toByteArray(new File(value)));
      } catch (IOException e) {
        throw new ParameterException(e);
      }
    }
  }

  public static final class FileValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
      File file = new File(value);
      if (!file.isFile()) {
        throw new ParameterException("Parameter " + name
            + "must be a file name, but no file was found at: " + value);
      }
    }
  }

  public static final class NoFileExistsValidator implements
      IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
      File file = new File(value);
      if (file.isFile()) {
        throw new ParameterException(
            "Parameter "
                + name
                + "must reference a location that does currently contain a file, but a file was found at: "
                + value);
      }
    }
  }

  private static ImmutableSet<String> experimentNames = ImmutableSet
      .<String> builder().add("hardToMatch").add("deleteUnos").add("protoObj")
      .add("priority").add("chainLength").add("matchFreq").add("noCycles")
      .add("unosPraSensitivity").add("patientPraObjectives")
      .add("singlePraThreshold").add("praVsWaiting").add("negativeWeights")
      .add("prioritizeByPowerShort").add("waiting").add("basic").build();

  public static final class ExperimentValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
      if (!experimentNames.contains(value)) {
        throw new ParameterException("Parameter " + name + " must in "
            + experimentNames + " but was: " + value);
      }
    }
  }

  public static final class NonnegativeValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
      try {
        int val = Integer.parseInt(value);
        if (val < 0) {
          throw new ParameterException("Parameter " + name
              + "must be a non-negative integer, but value was negative: "
              + value);
        }
      } catch (NumberFormatException e) {
        throw new ParameterException("Parameter " + name
            + "must be a non-negative integer, but could not parse to number: "
            + value);
      }
    }
  }

}
