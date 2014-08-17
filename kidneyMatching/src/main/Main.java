package main;

import graphUtil.CycleChainDecomposition;
import graphUtil.Edge;
import graphUtil.Node;
import ilog.concert.IloException;
import inputOutput.Attributes.ExchangeIn;
import inputOutput.Attributes.ExchangeOut;
import inputOutput.Attributes.NodeAttributeName;
import inputOutput.Attributes.NodeIn;
import inputOutput.Attributes.NodeOut;
import inputOutput.Attributes.PersonIn;
import inputOutput.Attributes.ReceiverIn;
import inputOutput.ExchangePredicates.InputPredicates;
import inputOutput.ExchangePredicates.OutputPredicates;
import inputOutput.Reports;
import inputOutput.Reports.DonorOptions;
import inputOutput.Reports.ExchangeOutputConverterSet;
import inputOutput.Reports.ExchangeUnitOutputAttributeSet;
import inputOutput.Reports.TitleMode;
import inputOutput.core.Report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import kepLib.KepInstance;
import kepLib.KepProblemData;
import kepLib.KepTextReaderWriter;
import kepModeler.AuxiliaryInputStatistics;
import kepModeler.ChainsForcedRemainOpenOptions;
import kepModeler.KepModeler;
import kepModeler.ObjectiveMode;
import multiPeriod.CycleChainPackingSubtourEliminationFactory;
import multiPeriod.MultiPeriodCyclePacking;
import multiPeriod.MultiPeriodCyclePacking.EffectiveNodeType;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;
import multiPeriod.MultiPeriodCyclePackingIpSolver;
import multiPeriod.TimeInstant;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;

import randomInstances.RandomGraphFactory;
import randomInstances.RobustCycleGenerator;
import replicator.DataMultiPeriodConverter;
import replicator.DonorEdge;
import replicator.TimePeriodUtil;
import statistics.Queries;
import statistics.Results;
import threading.FixedThreadPool;
import ui.DemoFrame;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

import data.DataCleaner;
import data.DataCleaner.ReasonToCleanDonor;
import data.DataCleaner.ReasonToCleanReceiver;
import data.DataCleaner.ReasonsInvalid;
import data.ExchangeUnit;
import data.ProblemData;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CycleChainPackingIp;

public class Main {

  public static void main(String[] args) {
    try {
      // Sets the graphics to use the operating system's native look and feel.
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (UnsupportedLookAndFeelException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    testUi();
  }

  private static void testLoad() {
    String dateString = "20120524";
    ProblemData problemData = new ProblemData(dateString);

    // this eliminates some of the inconsistent data
    Map<ExchangeUnit, ReasonsInvalid> removed = DataCleaner.cleanProblemData(
        problemData, EnumSet.allOf(ReasonToCleanDonor.class),
        EnumSet.allOf(ReasonToCleanReceiver.class));

    // these statistics are for the entire data set, generally spanning before
    // the start of the simulation.
    System.out.println("Altruistic Donors: "
        + problemData.getAltruisticDonors().size());
    System.out.println("Chip Receivers: " + problemData.getChips().size());
    System.out
        .println("Paired Donors: " + problemData.getPairedDonors().size());
    System.out.println("Paired Receivers: "
        + problemData.getPairedReceivers().size());
    System.out.println("Exchange Units: "
        + problemData.getExchangeUnits().size());
    System.out.println("Blocks: "
        + problemData.getHardBlockedExchanges().size());
    DataCleaner.assessHistoricMatching(problemData, removed);
    // ProblemDataWriter writer = new ProblemDataWriter(problemData, dateString
    // + "combined.csv", DataFormat.SNAPSHOT,
    // DataFormat.SNAPSHOT.getColumnNames().keySet());

    int numThreads = 4;
    Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
    Optional<Double> maxTime = Optional.<Double> absent();
    int maxChainLength = Integer.MAX_VALUE;
    int maxCycleLength = 3;

    // the maximum cardinality objective, with a small cycle bonus.
    ObjectiveMode objectiveMode = // new ObjectiveMode.MaximumCardinalityMode();

    // the maximum total edge weight objective. By default gives bias to low pmp
    // nodes.
    new ObjectiveMode.MaximumWeightedPackingMode();

    // Below, how to customize the weight function for the objective. See
    // MaximumWeightedPacking for documentation.
    // These are the default values.
    /*
     * double cycleBonus = .01; double defaultEdgeWeight = 1; double[]
     * thresholds = new double[]{1,10,100}; //must be strictly increasing and
     * nonnegative double[] bonuses = new double[]{3,.6,.1,0}; //must be
     * strictly decreasing, non-negative, and of one longer length than
     * thresholds PairMatchPowerBonus pmpBonus = new
     * PairMatchPowerThesholdBonus(thresholds,bonuses); objectiveMode = new
     * ObjectiveMode.MaximumWeightedPackingMode(cycleBonus, defaultEdgeWeight,
     * pmpBonus);
     */

    // no chains are held open
    ChainsForcedRemainOpenOptions openChainOptions =
    // ChainsForcedRemainOpenOptions.none;

    // always at least 3 chains open
    // new ChainsForcedRemainOpenOptions.AtLeastCountChains(1);

    // always at least 3 chains open where chain root has donor power >= .2.
    new ChainsForcedRemainOpenOptions.AtLeastCountOfQualityChains(1, .2);

    boolean displayOutput = true;// kills cplex solver output

    DateTime start = new DateTime(2009, 5, 24, 0, 0);
    DateTime end = new DateTime(2012, 5, 24, 0, 0);

    // the system begins in the preexisting state at time start, i.e. those that
    // arrive
    // before start but have not yet been matched are waiting.
    DataMultiPeriodConverter converterOld = new DataMultiPeriodConverter(
        problemData, start, end);

    // the system begins empty at time start, but then has the same arrivals as
    // above.
    // DataMultiPeriodConverter converterFresh = new
    // DataMultiPeriodConverter(problemData,start,end,true);

    MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = converterOld
        .getMultiPeriodPackingInputs();

    // contains pair match power info.
    AuxiliaryInputStatistics<ExchangeUnit, DonorEdge> auxiliaryInput = Queries
        .getAuxiliaryStatistics(inputs);
    inputs.setAuxiliaryInputStatistics(auxiliaryInput);

    KepModeler modeler = new KepModeler(maxChainLength, maxCycleLength,
        openChainOptions, objectiveMode);
    CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge> factory = new CycleChainPackingSubtourEliminationFactory<ExchangeUnit, DonorEdge>(
        modeler, displayOutput, maxTime, threadPool, auxiliaryInput);

    String inputName = dateString;

    // outputs statistics gathered on the inputs to output/queries/dateString
    Queries.allInputQueries(inputName, inputs);

    // we will run the simulation computing a matching every period, for each
    // period in the below list.
    List<Period> periods = Arrays.asList(Period.months(1));// Period.days(1),Period.weeks(1),
                                                           // Period.weeks(2),
                                                           // Period.months(1),
                                                           // Period.months(3),
                                                           // Period.months(6),
                                                           // Period.years(1));
    for (Period period : periods) {
      List<TimeInstant<ReadableInstant>> matchingTimes = TimePeriodUtil
          .generateScheduleTimeInstants(start, period, end, true);
      MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> packing = new MultiPeriodCyclePackingIpSolver<ExchangeUnit, DonorEdge, ReadableInstant>(
          inputs, matchingTimes, factory);

      packing.computeAllMatchings();

      String solutionName = "solveEvery" + period.toString();

      Results<ExchangeUnit, DonorEdge, ReadableInstant> result = new Results<ExchangeUnit, DonorEdge, ReadableInstant>(
          packing);
      // writes some data on the output to the same directory
      // output/queries/dateString
      Queries.queryAll(inputName, solutionName, result);

      ExchangeUnitOutputAttributeSet attributeSet = new ExchangeUnitOutputAttributeSet(
          packing);
      ExchangeOutputConverterSet converterSet = new ExchangeOutputConverterSet(
          attributeSet);
      List<NodeAttributeName> attributes = new ArrayList<NodeAttributeName>();
      attributes.addAll(Arrays.asList(NodeIn.values()));
      attributes.addAll(Arrays.asList(NodeOut.values()));
      attributes.addAll(Arrays.asList(ExchangeIn.values()));
      attributes.addAll(Arrays.asList(ExchangeOut.values()));
      attributes.addAll(Arrays.asList(ReceiverIn.values()));
      attributes.addAll(Arrays.asList(PersonIn.values()));

      Report<ExchangeUnit> report = Reports.makeOutputReport(attributes,
          TitleMode.SPACES, converterSet, DonorOptions.matchedDonorOrFirst, 0);// number
                                                                               // of
                                                                               // donors
                                                                               // to
                                                                               // display,
                                                                               // only
                                                                               // relevant
                                                                               // under
                                                                               // DonorOptions.donorsInOrder
      OutputPredicates outPredicates = new OutputPredicates(attributeSet);
      Predicate<ExchangeUnit> matched = outPredicates.receiverIsMatched();
      InputPredicates inPredicates = new InputPredicates(
          attributeSet.deriveInputAttributeSet());
      Predicate<ExchangeUnit> paired = inPredicates
          .effectiveNodeTypeIs(EffectiveNodeType.paired);
      {
        Predicate<ExchangeUnit> pairedAndMatched = Predicates.and(matched,
            paired);
        String reportHome = "output" + File.separator + "reports"
            + File.separator + dateString + File.separator;
        File reportDir = new File(reportHome);
        reportDir.mkdirs();
        String reportName = reportHome + period.toString()
            + "MatchedReport.csv";
        report.writeReport(inputs.getGraph().getVertices(), pairedAndMatched,
            reportName);
      }

    }
    factory.cleanUp();
    FixedThreadPool.shutDown(threadPool);

  }

  private static void testRandomGraph() {
    int n = 1000;
    double p = 10 / (double) n;// Math.log(n)/(double)n;
    System.out.println("Starting...");
    DirectedSparseMultigraph<Node, Edge> graph = RandomGraphFactory
        .randomERGraph(n, p);
    Set<Node> pairedNodes = new HashSet<Node>(graph.getVertices());
    Set<Node> chainRoots = new HashSet<Node>(RandomGraphFactory.addRoots(graph,
        3, p));

    System.out.println("chain roots: " + chainRoots.size());
    System.out.println("Random Graph created...");
    try {
      System.out.println("Without valid");
      // List<TestNode> chainRoots = new ArrayList<TestNode>();
      int numThreads = 4;
      Optional<FixedThreadPool> threadPool = FixedThreadPool
          .makePool(numThreads);
      Optional<Double> maxTime = Optional.<Double> absent();
      Set<Node> terminal = new HashSet<Node>();
      int maxChainLength = Integer.MAX_VALUE;
      int maxCycleLength = 3;
      ObjectiveMode objectiveMode = new ObjectiveMode.MaximumCardinalityMode();
      ChainsForcedRemainOpenOptions openChainOptions = ChainsForcedRemainOpenOptions.none;
      KepModeler modeler = new KepModeler(maxChainLength, maxCycleLength,
          openChainOptions, objectiveMode);
      CycleChainPackingSubtourEliminationFactory<Node, Edge> factory = new CycleChainPackingSubtourEliminationFactory<Node, Edge>(
          modeler, true, maxTime, threadPool);
      KepProblemData<Node, Edge> problemData = new KepProblemData<Node, Edge>(
          graph, chainRoots, pairedNodes, terminal);
      CycleChainPackingIp<Node, Edge> ip = factory.makeCycleChainPackingIP(
          problemData, Maps.<Node, Double> newHashMap());
      factory.cleanUp();

      ip.solve();
      CycleChainDecomposition<Node, Edge> ans = ip.getSolution();
      System.out.println("Size of matching: " + ans.totalEdges());
      ip.cleanUp();

    } catch (IloException e) {
      throw new RuntimeException(e);
    }
  }

  private static void testKepLibIo() {

    String input = "kepLibInstances/kepLibExample.txt";
    KepInstance<Node, Edge> instance = KepTextReaderWriter.INSTANCE.read(input);
    String output = "kepLibInstances/kepLibExampleCopy.txt";
    KepTextReaderWriter.INSTANCE.writeWithToString(instance, output);
    String copyOutput = "kepLibInstances/kepLibExampleCopyCopy.txt";
    KepInstance<Node, Edge> copyInstance = KepTextReaderWriter.INSTANCE
        .read(output);
    KepTextReaderWriter.INSTANCE.writeWithToString(copyInstance, copyOutput);

  }

  private static void testUi() {

    try {
      // Set System L&F
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (UnsupportedLookAndFeelException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    int n = 20;
    double p = 2 / (double) n;// Math.log(n)/(double)n;
    KepInstance<Node, Edge> hard = RobustCycleGenerator.makeHardInstance(30);
    DirectedSparseMultigraph<Node, Edge> graph = hard.getGraph();// RandomGraphFactory.randomERGraph(n,
                                                                 // p);
    DemoFrame<Node, Edge> frame = new DemoFrame<Node, Edge>(graph);
    frame.setVisible(true);
    String pathName = "output" + File.separator + "tikz" + File.separator
        + "graph.tex";
    // String fileName = new File(pathName).getAbsolutePath();
    // System.out.println(fileName);
    BufferedWriter writer;
    try {
      writer = new BufferedWriter(new FileWriter(pathName));
      // frame.getGraphPanel().printGraphManual(writer);
      writer.flush();
      writer.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }
  /*
   * private static void testRandomGraphAdapative(){ int n = 100; double p =
   * 2/(double)n;//Math.log(n)/(double)n; System.out.println("Starting...");
   * double failureProbability = .1; int numScenarios = 10; int maxPhaseOneEdges
   * = 50;//(int)(n*n*p/4); DirectedWeightedGraph<TestNode,DefaultWeightedEdge>
   * graph = RandomGraphFactory.randomERGraph(n, p); List<TestNode> chainRoots =
   * RandomGraphFactory.addRoots(graph, 5, p); List<Set<DefaultWeightedEdge>>
   * scenarios = RandomGraphFactory.generateCrossMatchFailureScenarios(new
   * ArrayList
   * <DefaultWeightedEdge>(graph.edgeSet()),numScenarios,failureProbability);
   * 
   * System.out.println("chain roots: " + chainRoots.size());
   * System.out.println("Random Graph created..."); int numThreads = 8;
   * ExecutorService exec = Executors.newFixedThreadPool(numThreads);
   * Set<TestNode> terminal = new HashSet<TestNode>(); int maxChainLength =
   * Integer.MAX_VALUE; int maxCycleLength = 3; int
   * maxCycleLengthToCreateVariables = 3; int minChainsOpen = 0;
   * 
   * AdapativeCycleChainPackingIp<TestNode,DefaultWeightedEdge> ip = new
   * AdaptiveCycleChainPackingSubtourElimination<TestNode,DefaultWeightedEdge>(
   * graph, chainRoots, terminal,scenarios, maxPhaseOneEdges, maxChainLength,
   * maxCycleLength, true, minChainsOpen, maxCycleLengthToCreateVariables, null,
   * exec, numThreads);
   * 
   * 
   * ip.solve(); AdaptiveCycleChainPacking<TestNode,DefaultWeightedEdge> ans =
   * ip.getSolution(); //System.out.println("Size of matching: " + ans.size());
   * ip.cleanUp(); //ans.validate(maxCycleLength, maxChainLength, new
   * HashSet<TestNode>(chainRoots), terminal, minTerminalOpen);
   * 
   * 
   * }
   */

}
