package main;

import ilog.concert.IloException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import kepLib.KepInstance;
import kepLib.KepTextReaderWriter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import threading.FixedThreadPool;

import com.beust.jcommander.JCommander;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import exchangeGraph.CycleChainPackingSubtourElimination;
import exchangeGraph.SolverOption;
import graphUtil.CycleChainDecomposition;
import graphUtil.Edge;
import graphUtil.EdgeChain;
import graphUtil.Node;

public class Performance {

  public static void main(String[] args) throws IOException {
    PerformanceCLI performanceCli = new PerformanceCLI();
    JCommander parser = new JCommander(performanceCli);
    try {
      parser.parse(args);
    } catch (RuntimeException e) {
      parser.usage();
      throw e;
    }
    if (performanceCli.getInstances().isEmpty()) {
      System.out.println("No files found!!! Existing Program.");
      parser.usage();
      return;
    }
    Performance perf = new Performance(performanceCli);

  }

  private Optional<FixedThreadPool> threadPool;

  public Performance(PerformanceCLI performanceCli) throws IOException {

    int numThreads = performanceCli.getThreads();
    threadPool = FixedThreadPool.makePool(numThreads);

    ImmutableList.Builder<SolverConfiguration> solverConfigBuilder = ImmutableList
        .builder();
    for (SolverOption constraintMode : performanceCli.getFormulations()) {
      ImmutableSet.Builder<SolverOption> options = ImmutableSet
          .<SolverOption> builder().add(constraintMode)
          .add(SolverOption.expandedFormulation)
          .add(SolverOption.lazyConstraintCallback)
          .add(SolverOption.userCutCallback);
      if (performanceCli.getMaxHeuristicAttempts() > 0) {
        options.add(SolverOption.heuristicCallback);
      }
      solverConfigBuilder.add(new SolverConfiguration(options.build(),
          threadPool, constraintMode + "-user-" + numThreads));

    }
    ImmutableList<SolverConfiguration> solverConfigurations = solverConfigBuilder
        .build();

    ImmutableList<InputProperty> inputProperties = ImmutableList.of(ALTRUISTS,
        NON_ALTRUISTS, EDGES);
    String outFile = performanceCli.getOutFile();
    File outDir = new File(outFile).getParentFile();
    outDir.mkdirs();

    BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
    CSVPrinter output = new CSVPrinter(writer, CSVFormat.DEFAULT);

    output.print("instance");
    for (InputProperty inputProperty : inputProperties) {
      output.print(inputProperty.getName());
    }
    for (SolverConfiguration solverConfiguration : solverConfigurations) {
      output.print(solverConfiguration.getName());
      if (performanceCli.getSolutionEdgeCount()) {
        output.print("solution edges");
      }
      if (performanceCli.getSolutionEdgeCount()) {
        output.print("solution longest chain");
      }
    }
    output.println();

    Optional<Double> maxSolveTimeSeconds = Optional.of((double) performanceCli
        .getMaxSolveTimeSeconds());

    ImmutableList<String> instanceFiles = ImmutableList.copyOf(performanceCli
        .getInstances());

    for (String instanceFile : instanceFiles) {
      String instanceName = Files
          .getNameWithoutExtension(new File(instanceFile).getName());
      System.out.println(instanceName);
      KepInstance<Node, Edge> kep = KepTextReaderWriter.INSTANCE
          .read(instanceFile);
      if (kep.getMaxChainLength() != performanceCli.getMaxChainLength()) {
        kep = changeMaxChainLength(kep, performanceCli.getMaxChainLength());
      }
      output.print(instanceName);
      for (InputProperty inputProperty : inputProperties) {
        output.print(inputProperty.apply(kep));
      }
      for (SolverConfiguration config : solverConfigurations) {

        double runTimeSeconds;
        int solutionEdgeCount = 0;
        int solutionLongestChain = 0;
        try {
          CycleChainPackingSubtourElimination<Node, Edge> solver = new CycleChainPackingSubtourElimination<Node, Edge>(
              kep, true, maxSolveTimeSeconds, config.getThreadingOptions(),
              config.getSovlerOptions(),
              performanceCli.getMaxHeuristicAttempts());
          System.gc();
          solver.solve();
          runTimeSeconds = solver.getSolveTimeSeconds();
          CycleChainDecomposition<Node, Edge> solution = solver.getSolution();
          solutionEdgeCount = solution.totalEdges();
          for (EdgeChain<Edge> chain : solution.getEdgeChains()) {
            solutionLongestChain = Math.max(solutionLongestChain, chain.size());
          }

          solver.cleanUp();
        } catch (IloException e) {
          System.err.println(e);
          runTimeSeconds = maxSolveTimeSeconds.get().doubleValue();
        }
        output.print(Double.toString(runTimeSeconds));
        if (performanceCli.getSolutionEdgeCount()) {
          output.print(Integer.toString(solutionEdgeCount));
        }
        if (performanceCli.getSolutionEdgeCount()) {
          output.print(Integer.toString(solutionLongestChain));
        }
      }
      output.println();
    }
    output.flush();
    writer.close();
    FixedThreadPool.shutDown(threadPool);

  }

  public static <V, E> KepInstance<V, E> changeMaxChainLength(
      KepInstance<V, E> instance, int newMaxChainLength) {
    KepInstance<V, E> ans = new KepInstance<V, E>(instance.getGraph(),
        instance.getRootNodes(), instance.getPairedNodes(),
        instance.getTerminalNodes(), instance.getEdgeWeights(),
        newMaxChainLength, instance.getMaxCycleLength(),
        instance.getCycleBonus());
    ans.getBridgeConstraints().addAll(instance.getBridgeConstraints());
    ans.getNodeFlowInConstraints().addAll(instance.getNodeFlowInConstraints());
    ans.getNodeFlowOutConstraints()
        .addAll(instance.getNodeFlowOutConstraints());
    ans.getEdgeUsageConstraints().addAll(instance.getEdgeUsageConstraints());
    return ans;
  }

  private static class SolverConfiguration {
    private ImmutableSet<SolverOption> sovlerOptions;
    private Optional<FixedThreadPool> threadingOptions;
    private String name;

    public SolverConfiguration(ImmutableSet<SolverOption> sovlerOptions,
        Optional<FixedThreadPool> threadingOptions, String name) {
      super();
      this.sovlerOptions = sovlerOptions;
      this.threadingOptions = threadingOptions;
      this.name = name;
    }

    public ImmutableSet<SolverOption> getSovlerOptions() {
      return sovlerOptions;
    }

    public Optional<FixedThreadPool> getThreadingOptions() {
      return threadingOptions;
    }

    public String getName() {
      return name;
    }
  }

  private static abstract class InputProperty implements
      Function<KepInstance<Node, Edge>, String> {
    private String name;

    public InputProperty(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

  private static InputProperty ALTRUISTS = new InputProperty("altruists") {
    @Override
    public String apply(KepInstance<Node, Edge> instance) {
      return Integer.toString(instance.getRootNodes().size());
    }
  };

  private static InputProperty NON_ALTRUISTS = new InputProperty(
      "non-altruists") {
    @Override
    public String apply(KepInstance<Node, Edge> instance) {
      return Integer.toString(instance.nonRootNodes().size());
    }
  };

  private static InputProperty EDGES = new InputProperty("edges") {
    @Override
    public String apply(KepInstance<Node, Edge> instance) {
      return Integer.toString(instance.getGraph().getEdgeCount());
    }
  };

}
