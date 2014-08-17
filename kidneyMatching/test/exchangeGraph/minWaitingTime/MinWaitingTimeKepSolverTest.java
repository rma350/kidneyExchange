package exchangeGraph.minWaitingTime;

import static org.junit.Assert.assertEquals;
import ilog.concert.IloException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import kepLib.KepInstance;
import kepLib.KepParseData;
import kepLib.KepTextReaderWriter;

import org.junit.AfterClass;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import exchangeGraph.SolverOption;
import graphUtil.CycleChainDecomposition;
import graphUtil.Edge;
import graphUtil.Node;

@RunWith(Theories.class)
public class MinWaitingTimeKepSolverTest {

  private static double tolerance = .00001;

  public static final String unitTestDir = "unitTestData" + File.separator
      + "minWaitingTime" + File.separator;
  public static final String inputFileNameBase = "e";
  public static final String inputArrivalTimeFileNameBase = "a";
  public static final String outputFileNameBase = "sol";
  public static final String outputMatchingTimeFileNameBase = "times";
  public static final String suffix = ".csv";

  public static final int numTests = 6;

  public static int[] oneToN(int n) {
    int[] ans = new int[n];
    for (int j = 0; j < n; j++) {
      ans[j] = j + 1;
    }
    return ans;
  }

  @AfterClass
  public static void cleanUp() {
    FixedThreadPool.shutDown(singleThreaded);
    FixedThreadPool.shutDown(multiThreaded);
  }

  @DataPoints
  public static int[] tests = oneToN(numTests);

  @DataPoint
  public static Optional<FixedThreadPool> singleThreaded = FixedThreadPool
      .makePool(1);

  // @DataPoint
  public static Optional<FixedThreadPool> multiThreaded = FixedThreadPool
      .makePool(2);

  @DataPoint
  public static ImmutableSet<SolverOption> o1 = SolverOption
      .makeCheckedOptions(SolverOption.cutsetMode,
          SolverOption.expandedFormulation,
          SolverOption.lazyConstraintCallback, SolverOption.userCutCallback);

  static class InputOutput {
    private KepInstance<Node, Edge> kepInstance;
    private MinWaitingTimeProblemData<Node> minWaitingTimeProblemData;
    private CycleChainDecomposition<Node, Edge> solution;
    private Map<Node, Double> matchingTimes;

    public InputOutput(KepInstance<Node, Edge> kepInstance,
        MinWaitingTimeProblemData<Node> minWaitingTimeProblemData,
        CycleChainDecomposition<Node, Edge> solution,
        Map<Node, Double> matchingTimes) {
      super();
      this.kepInstance = kepInstance;
      this.minWaitingTimeProblemData = minWaitingTimeProblemData;
      this.solution = solution;
      this.matchingTimes = matchingTimes;
    }

    public KepInstance<Node, Edge> getKepInstance() {
      return kepInstance;
    }

    public MinWaitingTimeProblemData<Node> getMinWaitingTimeProblemData() {
      return minWaitingTimeProblemData;
    }

    public CycleChainDecomposition<Node, Edge> getSolution() {
      return solution;
    }

    public Map<Node, Double> getMatchingTimes() {
      return matchingTimes;
    }
  }

  private static String mkFileName(String base, int trialNum) {
    return unitTestDir + base + trialNum + suffix;
  }

  public static InputOutput readInputOutput(int trialNum) throws IOException {
    if (trialNum < 1 || trialNum > numTests) {
      throw new RuntimeException("Trial number must lie in [1," + trialNum
          + "] but was " + trialNum);
    }
    String inputName = mkFileName(inputFileNameBase, trialNum);
    KepParseData<Node, Edge> parseData = KepTextReaderWriter.INSTANCE
        .readParseData(inputName);
    String arrivalTimeName = mkFileName(inputArrivalTimeFileNameBase, trialNum);
    MinWaitingTimeProblemData<Node> minWaitingTimeProblemData = KepTextReaderWriter.INSTANCE
        .readNodeArrivalTimes(arrivalTimeName, parseData);

    String outputName = mkFileName(outputFileNameBase, trialNum);
    CycleChainDecomposition<Node, Edge> output = KepTextReaderWriter.INSTANCE
        .readSolution(parseData, outputName);
    String matchingTimesName = mkFileName(outputMatchingTimeFileNameBase,
        trialNum);
    Map<Node, Double> matchingTimes = KepTextReaderWriter.INSTANCE
        .readNodeMatchingTimes(matchingTimesName, parseData);
    return new InputOutput(parseData.getInstance(), minWaitingTimeProblemData,
        output, matchingTimes);
  }

  @Theory
  public void test(int trial, Optional<FixedThreadPool> threadPool,
      ImmutableSet<SolverOption> solverOptions) throws IOException,
      IloException {
    String identification = "Trial " + trial + ", Threads "
        + (threadPool.isPresent() ? threadPool.get().getNumThreads() : 1)
        + ", Solver Options:" + solverOptions;
    System.out.println(identification);
    InputOutput data = readInputOutput(trial);
    boolean displayOutput = false;
    MinWaitingTimeKepSolver<Node, Edge> solver = new MinWaitingTimeKepSolver<Node, Edge>(
        data.getKepInstance(), data.getMinWaitingTimeProblemData()
            .getNodeArrivalTime(), data.getMinWaitingTimeProblemData()
            .getTerminationTime(), displayOutput, Optional.<Double> absent(),
        threadPool, solverOptions);
    solver.solve();
    System.out.println(solver.getMatchingTimes());
    assertEquals(identification, data.getSolution(), solver.getSolution());

    assertEquals(identification, data.getMatchingTimes().keySet(), solver
        .getMatchingTimes().keySet());
    for (Map.Entry<Node, Double> entry : data.getMatchingTimes().entrySet()) {
      assertEquals(
          identification + " on matching time for node: " + entry.getKey(),
          entry.getValue().doubleValue(),
          solver.getMatchingTimes().get(entry.getKey()).doubleValue(),
          tolerance);
    }
    solver.cleanUp();
  }

}
