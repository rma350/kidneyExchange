package main;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.validators.PositiveInteger;

import exchangeGraph.SolverOption;

public class PerformanceCLI {

  @Parameter(names = { "--threads", "-t" }, description = "Number of threads per instance", validateWith = PositiveInteger.class)
  private int threads = 1;

  @Parameter(names = { "--instance", "-i" }, variableArity = true, description = "List the file names of the KEP instance to run.  If not specified, first ./kepLibInstances/*.csv will be checked, then *.csv from the working directory will be checked.")
  private List<String> instances = makeDefaultInstances();

  @Parameter(names = { "--chain", "-c" }, description = "Maximum chain length", validateWith = PositiveInteger.class)
  private int maxChainLength = Integer.MAX_VALUE;

  @Parameter(names = { "--timeLimit", "-l" }, description = "Maximum solve time seconds", validateWith = PositiveInteger.class)
  private int maxSolveTimeSeconds = Integer.MAX_VALUE;

  @Parameter(names = { "--heuristicAttempts", "-h" }, description = "Maximum attempts to construct integer solution via heuristics", validateWith = PositiveInteger.class)
  private int maxHeuristicAttempts = 5;

  @Parameter(names = { "--formulation", "-f" }, variableArity = true, description = "Integer Programming Formulation", validateWith = FormulationValidator.class)
  private List<String> formulations = Lists.newArrayList(
      SolverOption.edgeMode.toString(), SolverOption.cutsetMode.toString());

  @Parameter(names = { "--output", "-o" }, description = "File to write output")
  private String outFile = "output" + File.separator + "kepLib"
      + File.separator + "kepLibSummary.csv";

  @Parameter(names = { "--solutionEdgeCount", "-se" }, description = "Output includes the number of edges in the optimal solution")
  private boolean solutionEdgeCount = false;

  @Parameter(names = { "--solutionLongestChain", "-sl" }, description = "Ouput includes the length of the longest chain in the optimal solution")
  private boolean solutionLongestChain = false;

  private List<String> makeDefaultInstances() {
    List<String> ans = Lists.newArrayList();
    String path = "kepLibInstances";
    File dir = new File(path);
    if (!dir.isDirectory()) {
      System.out
          .println("Directory kepLibInstances not found, looking for files in working directory instead.");
      path = ".";
    }
    String fileName = ".*.csv";

    System.out.println("path:" + path);
    System.out.println("fileName:" + fileName);
    // fileName = fileName.replaceAll("\\*", "\\.\\*");
    // System.out.println("fileNameJavaregex:" + fileName);

    Collection<File> files = FileUtils.listFiles(new File(path),
        new RegexFileFilter(fileName), null);
    for (File file : files) {
      ans.add(file.getPath());
    }
    System.out.println("Found " + files.size() + " files.");
    return ans;

  }

  public int getThreads() {
    return threads;
  }

  public List<String> getInstances() {
    return instances;
  }

  public int getMaxChainLength() {
    return maxChainLength;
  }

  public int getMaxSolveTimeSeconds() {
    return maxSolveTimeSeconds;
  }

  public int getMaxHeuristicAttempts() {
    return this.maxHeuristicAttempts;
  }

  public List<SolverOption> getFormulations() {
    List<SolverOption> ans = Lists.newArrayList();
    for (String formName : formulations) {
      ans.add(SolverOption.valueOf(formName));
    }
    return ans;
  }

  public String getOutFile() {
    return outFile;
  }

  public boolean getSolutionEdgeCount() {
    return solutionEdgeCount;
  }

  public boolean getSolutionLongestChain() {
    return solutionLongestChain;
  }

  public static final class SolverOptionConverter implements
      IStringConverter<SolverOption> {

    @Override
    public SolverOption convert(String value) {
      return SolverOption.valueOf(value);
    }

  }

  public static final class FormulationValidator implements IParameterValidator {

    private String errorMessage(String name, String value) {
      return "Paramter " + name + " must be in " + SolverOption.constriantModes
          + " but was " + value;
    }

    @Override
    public void validate(String name, String value) throws ParameterException {
      try {
        SolverOption option = SolverOption.valueOf(value);
        if (SolverOption.constriantModes.contains(option)) {
          return;
        } else {
          throw new ParameterException(errorMessage(name, value));
        }
      } catch (IllegalArgumentException e) {
        throw new ParameterException(errorMessage(name, value));
      }

    }

  }

}
