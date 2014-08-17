package multiPeriod;

import ilog.concert.IloException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepLib.KepInstance;
import kepLib.KepProblemData;
import kepLib.KepTextReaderWriter;

import com.google.common.collect.Maps;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CycleChainPackingIp;
import exchangeGraph.CycleChainPackingPolytope;
import exchangeGraph.CycleChainPackingSubtourElimination;
import graphUtil.CycleChainDecomposition;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import graphUtil.SubgraphUtil;

public class MultiPeriodCyclePackingIpSolver<V, E, T extends Comparable<T>>
    extends MultiPeriodCyclePacking<V, E, T> {

  private CycleChainPackingFactory<V, E> solverFactory;

  private Long minSolveTimeWriteToKepLib;
  private boolean failFast = true;

  public MultiPeriodCyclePackingIpSolver(
      MultiPeriodCyclePackingInputs<V, E, T> inputs,
      List<TimeInstant<T>> augmentMatchingTimes,
      CycleChainPackingFactory<V, E> solverFactory) {
    this(inputs, augmentMatchingTimes, solverFactory, null);
  }

  public MultiPeriodCyclePackingIpSolver(
      MultiPeriodCyclePackingInputs<V, E, T> inputs,
      List<TimeInstant<T>> augmentMatchingTimes,
      CycleChainPackingFactory<V, E> solverFactory,
      Long minSolveTimeWriteToKepLib) {
    super(inputs, augmentMatchingTimes);
    this.solverFactory = solverFactory;
    this.minSolveTimeWriteToKepLib = minSolveTimeWriteToKepLib;
    System.out.println("Total number of vertices: "
        + inputs.getGraph().getVertexCount());
    System.out.println("Number of chips: " + inputs.getTerminalNodes().size());
    System.out.println("Number of paired: "
        + (inputs.getGraph().getVertexCount()
            - inputs.getTerminalNodes().size() - inputs.getRootNodes().size()));
    System.out.println("Number of altruist/bridge: "
        + inputs.getRootNodes().size());
  }

  public MultiPeriodCyclePackingIpSolver(
      MultiPeriodCyclePackingInputs<V, E, T> inputs,
      int minDaysBetweenMatchings, CycleChainPackingFactory<V, E> solverFactory) {
    super(inputs, minDaysBetweenMatchings);
    this.solverFactory = solverFactory;
    System.out.println("Total number of vertices: "
        + inputs.getGraph().getVertexCount());
    System.out.println("Number of chips: " + inputs.getTerminalNodes().size());
    System.out.println("Number of paired: "
        + (inputs.getGraph().getVertexCount()
            - inputs.getTerminalNodes().size() - inputs.getRootNodes().size()));
    System.out.println("Number of altruist/bridge: "
        + inputs.getRootNodes().size());
  }

  // TODO a lot of this should move up into the super class with kepModeler!!!
  @Override
  protected void augmentMatching() {
    TimeInstant<T> currentTime = this.augmentMatchingTimes
        .get(this.currentMatchingTime);

    Set<V> pairedDonorReceivers = new HashSet<V>();
    Set<V> chips = new HashSet<V>();
    Set<V> altruistsAndBridge = new HashSet<V>();
    for (V v : inputs.getGraph().getVertices()) {
      if (currentTime.compareTo(inputs.getNodeArrivalTimes().get(v)) >= 0) {
        if (this.dynamicMatching.getNodeToReceiverMatching().containsKey(v)) {
          if (this.dynamicMatching.getChainTailToChain().containsKey(v)) {
            if (this.dynamicMatching.getChainTailToChain().get(v)
                .getTerminated() == null) {
              altruistsAndBridge.add(v);
            }
          }
        } else {
          if (inputs.getRootNodes().contains(v)) {
            if (!this.dynamicMatching.getNodeToDonorMatching().containsKey(v)) {
              altruistsAndBridge.add(v);
            }
          } else if (inputs.getTerminalNodes().contains(v)) {
            chips.add(v);
          } else {
            pairedDonorReceivers.add(v);
          }

        }
      }
    }
    Set<V> subgraphNodes = new HashSet<V>();

    subgraphNodes.addAll(chips);
    subgraphNodes.addAll(pairedDonorReceivers);
    subgraphNodes.addAll(altruistsAndBridge);

    Set<E> subgraphEdges = new HashSet<E>();
    for (E e : inputs.getGraph().getEdges()) {
      if (currentTime.compareTo(inputs.getEdgeArrivalTimes().get(e)) >= 0) {
        V source = inputs.getGraph().getSource(e);
        V sink = inputs.getGraph().getDest(e);
        if (subgraphNodes.contains(source) && subgraphNodes.contains(sink)) {
          if (!altruistsAndBridge.contains(sink) && !chips.contains(source)) {
            subgraphEdges.add(e);
          }
        }
      }
    }
    // System.out.println("Number of edges this iteration: " +
    // subgraphEdges.size());
    DirectedSparseMultigraph<V, E> subgraph = SubgraphUtil.subgraph(
        inputs.getGraph(), subgraphNodes, subgraphEdges);
    CycleChainPackingIp<V, E> cyclePackingIp = null;
    CycleChainDecomposition<V, E> ans = null;
    try {
      KepProblemData<V, E> problemData = new KepProblemData<V, E>(subgraph,
          altruistsAndBridge, pairedDonorReceivers, chips);
      Map<V, Double> waitingTimes = Maps.newHashMap();
      for (V vertex : subgraph.getVertices()) {
        waitingTimes.put(
            vertex,
            inputs.getTimeWriter().writeTime(
                inputs.getNodeArrivalTimes().get(vertex)));
      }
      cyclePackingIp = solverFactory.makeCycleChainPackingIP(problemData,
          waitingTimes);
      cyclePackingIp.solve();
      if (this.minSolveTimeWriteToKepLib != null
          && cyclePackingIp.getSolveTimeSeconds() > minSolveTimeWriteToKepLib) {
        System.err.println("min time to write: " + minSolveTimeWriteToKepLib
            + ", actual time: " + cyclePackingIp.getSolveTimeSeconds());
        KepInstance<V, E> instance = cyclePackingIp.getKepInstance();
        String fileName = "r" + instance.getRootNodes().size() + "p"
            + instance.getPairedNodes().size() + "t"
            + instance.getTerminalNodes().size() + "e"
            + instance.getGraph().getEdgeCount() + "time"
            + currentTime.getValue().toString();
        System.err.println("writing to keplib: " + fileName);
        KepTextReaderWriter.INSTANCE.writeAnonymous(instance, "kepLibInstances"
            + File.separator + fileName + ".csv");
      }
      ans = cyclePackingIp.getSolution();
      if (ans.totalEdges() > 0) {
        // System.out.print("Current time: " + currentTime.getValue());
        // System.out.println(", solution size: " + ans.totalEdges());
      }
      cyclePackingIp.cleanUp();
      List<EdgeCycle<E>> cyclesByEdges = ans.getEdgeCycles();
      Map<EdgeChain<E>, Boolean> chainByEdgeToTerminate = new HashMap<EdgeChain<E>, Boolean>();
      for (EdgeChain<E> chainByEdges : ans.getEdgeChains()) {
        if (chainByEdges.size() > 0) {
          V lastNode = inputs.getGraph().getDest(
              chainByEdges.getEdgesInOrder().get(chainByEdges.size() - 1));
          boolean isTerminal = chips.contains(lastNode);
          chainByEdgeToTerminate.put(chainByEdges, Boolean.valueOf(isTerminal));
        }
      }
      this.dynamicMatching.augmentMatchings(cyclesByEdges,
          chainByEdgeToTerminate, currentTime);

    } catch (RuntimeException e) {
      onFatalError(e, cyclePackingIp, ans);
    } catch (IloException e) {
      onFatalError(e, cyclePackingIp, ans);
    }
  }

  private void onFatalError(Exception e,
      CycleChainPackingIp<V, E> cyclePackingIp,
      CycleChainDecomposition<V, E> ans) {
    if (failFast) {
      throw new RuntimeException(e);
    }
    try {
      BufferedWriter errorGraph = new BufferedWriter(new FileWriter(
          "errorGraph.tex"));
      Set<E> edgesUsed = new HashSet<E>();
      if (ans != null) {
        for (EdgeChain<E> chain : ans.getEdgeChains()) {
          edgesUsed.addAll(chain.getEdges());
        }
        for (EdgeCycle<E> cycle : ans.getEdgeCycles()) {
          edgesUsed.addAll(cycle.getEdges());
        }
      } else if (cyclePackingIp instanceof CycleChainPackingSubtourElimination<?, ?>) {
        try {
          CycleChainPackingSubtourElimination<V, E> subtour = (CycleChainPackingSubtourElimination<V, E>) cyclePackingIp;
          if (subtour.getPolytope() instanceof CycleChainPackingPolytope) {
            CycleChainPackingPolytope<V, E> polytope = (CycleChainPackingPolytope<V, E>) subtour
                .getPolytope();
            edgesUsed.addAll(polytope.getEdgeVariables().getNonZeroVariables());
            for (EdgeCycle<E> cycle : polytope.getCycleVariables()
                .getNonZeroVariables()) {
              edgesUsed.addAll(cycle.getEdges());
            }
          }
        } catch (IloException e1) {
          System.err
              .println("cplex exception while attempting to write out error: "
                  + e1.getMessage());
        }
      } else {
        System.err.println("error while generating solution...");
      }
      cyclePackingIp.getKepInstance().printToTikz(errorGraph, edgesUsed, true);
      errorGraph.flush();
      errorGraph.close();
      KepTextReaderWriter.INSTANCE.writeWithToString(
          cyclePackingIp.getKepInstance(), "error.txt");
    } catch (IOException e1) {
      System.err.println("failed to write out error do to io error: "
          + e1.getMessage());
    }
    throw new RuntimeException(e);
  }

}
