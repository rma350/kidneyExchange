package main;

import ilog.concert.IloException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepLib.KepInstance;
import kepLib.KepTextReaderWriter;
import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CycleChainPackingSubtourElimination;
import exchangeGraph.SolverOption;
import exchangeGraph.stochasticOpt.EdgeFailureScenario;
import exchangeGraph.stochasticOpt.TwoStageEdgeFailureSolver;
import graphUtil.CycleChainDecomposition;
import graphUtil.Edge;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import graphUtil.Node;

public class Debug {

  public static void main(String[] args) {
    regularKep();
  }

  private static void regularKep() {
    Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(8);
    CycleChainPackingSubtourElimination<Node, Edge> solver = null;
    try {
      // String problemName = "kepLibInstances/BinputData186950";
      // String problemName = "kepLibInstances/BinputData164750";
      // String problemName = "kepLibInstances/BinputData62818";
      // String problemName = "kepLibInstances/BinputData60084";
      // String problemName = "kepLibInstances/BinputData59364";
      // String problemName = "kepLibInstances/BinputData40282";

      String problemName = "kepLibInstances/r0p134t0e1990time196.0";
      // String problemName = "kepLibInstances/BinputData149794";
      // String problemName = "kepLibInstances/BinputData90049";
      // String problemName = "kepLibInstances/BinputData184527";

      // String problemName = "kepLibInstances/inputData174350";
      // String problemName =
      // "kepLibInstances/r75p473t244e60457time2012-05-24T00:00:00.000-04:00";
      // String problemName =
      // "kepLibInstances/r1p161t149e4463time2011-09-24T00:00:00.000-04:00";
      // String problemName =
      // "kepLibInstances/r2p213t176e8346time2012-02-24T00:00:00.000-05:00";
      // String problemName = "kepLibInstances/r10p138t118e2411";
      // String problemName = "kepLibInstances/r10p137t118e2550";
      // String problemName = "kepLibInstances/r10p93t59e1125";
      // String problemName = "kepLibInstances/r10p138t118e2347";
      // String problemName = "kepLibInstances/r10p97t59e1109";
      String suffix = ".csv";
      KepInstance<Node, Edge> kepInstance = KepTextReaderWriter.INSTANCE
          .read(problemName + suffix);// "error.txt");
      /*
       * Set<Edge> retainedEdges = new HashSet<Edge>(); Set<Node> retainedRoots
       * = new
       * HashSet<Node>(Lists.newArrayList(kepInstanceFull.getRootNodes()).subList
       * (50,51)); Set<Node> discardedRoots =
       * Sets.difference(kepInstanceFull.getRootNodes(),
       * retainedRoots).immutableCopy(); for(Edge edge:
       * kepInstanceFull.getGraph().getEdges()){ if(Sets.intersection(new
       * HashSet
       * <Node>(kepInstanceFull.getGraph().getEndpoints(edge)),discardedRoots
       * ).isEmpty()){ retainedEdges.add(edge); } } KepInstance<Node, Edge>
       * kepInstance = kepInstanceFull.createRestrictedProblem(retainedEdges);
       */
      boolean displayOutput = true;
      Optional<Double> maxTimeSeconds = Optional.<Double> absent();

      ImmutableSet<SolverOption> solverOptions = SolverOption
          .makeCheckedOptions(SolverOption.cutsetMode,
              SolverOption.lazyConstraintCallback,
              SolverOption.userCutCallback, SolverOption.expandedFormulation);

      boolean printTikz = true;

      solver = new CycleChainPackingSubtourElimination<Node, Edge>(kepInstance,
          displayOutput, maxTimeSeconds, threadPool, solverOptions);
      solver.solve();
      CycleChainDecomposition<Node, Edge> ans = solver.getSolution();
      DirectedSparseMultigraph<Node, Edge> graph = kepInstance.getGraph();
      System.out.println("chains: " + ans.getEdgeChains().size());
      for (EdgeChain<Edge> chain : ans.getEdgeChains()) {
        System.out.println("chain: "
            + convertListFormat(chain.getEdgesInOrder(), graph));
      }
      System.out.println("cycles: " + ans.getEdgeCycles().size());
      for (EdgeCycle<Edge> cycle : ans.getEdgeCycles()) {
        System.out.println("cycle: "
            + convertListFormat(cycle.getEdgesInOrder(), graph));
      }
      if (printTikz) {
        Set<Edge> matchEdges = new HashSet<Edge>();
        for (EdgeChain<Edge> chain : ans.getEdgeChains()) {
          matchEdges.addAll(chain.getEdgesInOrder());
        }
        for (EdgeCycle<Edge> cycle : ans.getEdgeCycles()) {
          matchEdges.addAll(cycle.getEdgesInOrder());
        }
        String tikzOut = problemName + ".tex";
        BufferedWriter writer = new BufferedWriter(new FileWriter(tikzOut));
        kepInstance.printToTikz(writer, matchEdges, true, true);
        // writer.flush();
        // writer.close();
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (IloException e) {
      throw new RuntimeException(e);
    } finally {
      if (solver != null) {
        solver.cleanUp();
      }
      FixedThreadPool.shutDown(threadPool);

    }
  }

  private static void twoStageKep() {
    Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(4);
    TwoStageEdgeFailureSolver<Node, Edge> solver = null;
    try {
      // String problemName =
      // "kepLibInstances/r75p473t244e60457time2012-05-24T00:00:00.000-04:00";
      // String problemName =
      // "kepLibInstances/r1p161t149e4463time2011-09-24T00:00:00.000-04:00";
      // String problemName =
      // "kepLibInstances/r2p213t176e8346time2012-02-24T00:00:00.000-05:00";
      String problemName = "kepLibInstances/r10p138t118e2411";
      // String problemName = "kepLibInstances/r10p137t118e2550";
      // String problemName = "kepLibInstances/r10p93t59e1125";
      // String problemName = "kepLibInstances/r10p138t118e2347";
      // String problemName = "kepLibInstances/r10p97t59e1109";
      String suffix = ".csv";
      KepInstance<Node, Edge> kepInstance = KepTextReaderWriter.INSTANCE
          .read(problemName + suffix);// "error.txt");
      Map<Edge, Double> edgeFailures = new HashMap<Edge, Double>();
      for (Edge e : kepInstance.getGraph().getEdges()) {
        edgeFailures.put(e, .1);
      }
      boolean displayOutput = true;
      Optional<Double> maxTimeMs = Optional.<Double> absent();
      ImmutableSet<SolverOption> solverOptions = SolverOption
          .makeCheckedOptions(SolverOption.cutsetMode,
              SolverOption.lazyConstraintCallback, SolverOption.userCutCallback);

      boolean printTikz = false;
      List<EdgeFailureScenario<Node, Edge>> edgeFailureScenarios = EdgeFailureScenario
          .generateScenarios(kepInstance, edgeFailures, 10);
      solver = TwoStageEdgeFailureSolver.truncationSolver(kepInstance,
          edgeFailureScenarios, threadPool, displayOutput, maxTimeMs,
          SolverOption.defaultOptions);
      solver.solve();
      EdgeFailureScenario<Node, Edge> actual = EdgeFailureScenario
          .generateScenarios(kepInstance, edgeFailures, 1).get(0);
      CycleChainDecomposition<Node, Edge> ans = solver
          .applySolutionToRealization(actual, false, null, threadPool,
              solverOptions);
      DirectedSparseMultigraph<Node, Edge> graph = kepInstance.getGraph();
      System.out.println("chains: " + ans.getEdgeChains().size());
      for (EdgeChain<Edge> chain : ans.getEdgeChains()) {
        System.out.println("chain: "
            + convertListFormat(chain.getEdgesInOrder(), graph));
      }
      System.out.println("cycles: " + ans.getEdgeCycles().size());
      for (EdgeCycle<Edge> cycle : ans.getEdgeCycles()) {
        System.out.println("cycle: "
            + convertListFormat(cycle.getEdgesInOrder(), graph));
      }
      if (printTikz) {
        Set<Edge> matchEdges = new HashSet<Edge>();
        for (EdgeChain<Edge> chain : ans.getEdgeChains()) {
          matchEdges.addAll(chain.getEdgesInOrder());
        }
        for (EdgeCycle<Edge> cycle : ans.getEdgeCycles()) {
          matchEdges.addAll(cycle.getEdgesInOrder());
        }
        String tikzOut = problemName + ".tex";
        BufferedWriter writer = new BufferedWriter(new FileWriter(tikzOut));
        kepInstance.printToTikz(writer, matchEdges, true);
        writer.flush();
        writer.close();
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (IloException e) {
      throw new RuntimeException(e);
    } finally {
      FixedThreadPool.shutDown(threadPool);
    }
  }

  private static List<Node> convertListFormat(List<Edge> edges,
      DirectedSparseMultigraph<Node, Edge> graph) {
    List<Node> ans = new ArrayList<Node>();
    for (Edge edge : edges) {
      ans.add(graph.getSource(edge));
      ans.add(graph.getDest(edge));
    }
    return ans;
  }

}
