package web;

import graphUtil.CycleChainDecomposition;
import graphUtil.EdgeChain;
import ilog.concert.IloException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kepLib.KepInstance;
import kepLib.KepProblemData;
import kepModeler.ChainsForcedRemainOpenOptions;
import kepModeler.KepModeler;
import kepModeler.ModelerInputs;
import kepModeler.ObjectiveMode;
import replicator.DonorEdge;
import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import data.Donor;
import data.ExchangeUnit;
import database.KidneyDataBase;
import exchangeGraph.CycleChainPackingSubtourElimination;
import exchangeGraph.SolverOption;

public class KidneyServerSolver {

  private KidneyDataBase database;
  private Map<String, ModelerInputs<ExchangeUnit, DonorEdge>> dataCache = new HashMap<String, ModelerInputs<ExchangeUnit, DonorEdge>>();

  private Optional<FixedThreadPool> threadPool;
  Optional<Double> maxSolveTimeMs = Optional.of(100.0);

  public KidneyServerSolver(KidneyDataBase database,
      Optional<FixedThreadPool> threadPool) {
    this.database = database;
    this.threadPool = threadPool;
  }

  public ImmutableList<String> availableDatasets() {
    return database.availableDatasets();
  }

  public Map<Object, Object> getInputs(String databaseName) {
    return flattenModelerInputs(getModelerInputs(databaseName));
  }

  public Map<Object, Object> getSolution(String databaseName)
      throws IloException {
    ModelerInputs<ExchangeUnit, DonorEdge> inputs = getModelerInputs(databaseName);
    KepModeler modeler = new KepModeler(3, Integer.MAX_VALUE,
        ChainsForcedRemainOpenOptions.none,
        new ObjectiveMode.MaximumCardinalityMode());
    KepInstance<ExchangeUnit, DonorEdge> instance = modeler.makeKepInstance(
        inputs, null);
    CycleChainPackingSubtourElimination<ExchangeUnit, DonorEdge> solver = new CycleChainPackingSubtourElimination<ExchangeUnit, DonorEdge>(
        instance, true, maxSolveTimeMs, threadPool,
        SolverOption.makeCheckedOptions(SolverOption.cutsetMode,
            SolverOption.lazyConstraintCallback, SolverOption.userCutCallback));
    solver.solve();
    CycleChainDecomposition<ExchangeUnit, DonorEdge> solution = solver
        .getSolution();
    solver.cleanUp();
    return flattenSolution(inputs.getKepProblemData(), solution);
  }

  private ModelerInputs<ExchangeUnit, DonorEdge> getModelerInputs(
      String databaseName) {
    if (this.dataCache.containsKey(databaseName)) {
      return this.dataCache.get(databaseName);
    } else {
      ModelerInputs<ExchangeUnit, DonorEdge> inputs = database
          .loadInputs(databaseName);
      this.dataCache.put(databaseName, inputs);
      return inputs;
    }
  }

  public static Map<Object, Object> flattenModelerInputs(
      ModelerInputs<ExchangeUnit, DonorEdge> inputs) {
    Map<Object, Object> ans = new HashMap<Object, Object>();
    List<Map<Object, Object>> flatUnits = Lists.newArrayList();
    List<Map<Object, Object>> flatEdges = Lists.newArrayList();
    for (ExchangeUnit unit : inputs.getKepProblemData().getGraph()
        .getVertices()) {
      flatUnits.add(flattenExchangeUnit(inputs, unit));
    }
    for (DonorEdge edge : inputs.getKepProblemData().getGraph().getEdges()) {
      flatEdges.add(flattenDonorEdge(inputs.getKepProblemData(), edge));
    }
    ans.put("nodes", flatUnits);
    ans.put("links", flatEdges);
    return ans;
  }

  public static Map<Object, Object> flattenSolution(
      KepProblemData<ExchangeUnit, DonorEdge> problemData,
      CycleChainDecomposition<ExchangeUnit, DonorEdge> solution) {
    Map<Object, Object> ans = new HashMap<Object, Object>();
    List<Map<Object, Object>> flatEdges = Lists.newArrayList();
    for (EdgeChain<DonorEdge> edgeChain : solution.getEdgeChains()) {
      for (DonorEdge edge : edgeChain) {
        flatEdges.add(flattenDonorEdge(problemData, edge));
      }
    }
    ans.put("links", flatEdges);
    return ans;
  }

  private static Map<Object, Object> flattenDonorEdge(
      KepProblemData<ExchangeUnit, DonorEdge> kepProblemData, DonorEdge edge) {
    Map<Object, Object> ans = new HashMap<Object, Object>();
    ExchangeUnit source = kepProblemData.getGraph().getSource(edge);
    ExchangeUnit dest = kepProblemData.getGraph().getDest(edge);
    String sourceId = makeNodeId(kepProblemData, source);
    String destId = makeNodeId(kepProblemData, dest);
    ans.put("sourceId", sourceId);
    ans.put("targetId", destId);
    ans.put("id", sourceId + destId);
    return ans;
  }

  private static Map<Object, Object> flattenExchangeUnit(
      ModelerInputs<ExchangeUnit, DonorEdge> inputs, ExchangeUnit unit) {
    Map<Object, Object> ans = new HashMap<Object, Object>();
    ans.put("id", makeNodeId(inputs.getKepProblemData(), unit));
    ans.put("type", makeType(inputs.getKepProblemData(), unit));
    ans.put("reachable", true);
    ans.put("sensitized", computeSensitization(inputs, unit));
    return ans;
  }

  private static String makeNodeId(
      KepProblemData<ExchangeUnit, DonorEdge> kepProblemData, ExchangeUnit unit) {
    if (kepProblemData.getRootNodes().contains(unit)) {
      return unit.getDonor().get(0).getId();
    } else {
      return unit.getReceiver().getId();
    }
  }

  private static String makeType(
      KepProblemData<ExchangeUnit, DonorEdge> kepProblemData, ExchangeUnit unit) {
    if (kepProblemData.getRootNodes().contains(unit)) {
      return "root";
    } else if (kepProblemData.getPairedNodes().contains(unit)) {
      return "paired";
    } else if (kepProblemData.getTerminalNodes().contains(unit)) {
      return "terminal";
    } else {
      throw new RuntimeException();
    }
  }

  private static int computeSensitization(
      ModelerInputs<ExchangeUnit, DonorEdge> inputs, ExchangeUnit unit) {
    Map<ExchangeUnit, Double> donorPower = inputs.getAuxiliaryInputStatistics()
        .getDonorPowerPostPreference();
    Map<ExchangeUnit, Double> receiverPower = inputs
        .getAuxiliaryInputStatistics().getReceiverPowerPostPreference();
    // System.out.println(donorPower);
    // System.out.println(receiverPower);

    if (inputs.getKepProblemData().getRootNodes().contains(unit)) {
      if (donorPower.containsKey(unit.getDonor().get(0))) {
        return singlePersonSensitization(donorPower.get(unit.getDonor().get(0)));
      } else {
        // System.err.println("missing donor power data for: " + unit);
        return 0;
      }

    } else if (inputs.getKepProblemData().getPairedNodes().contains(unit)) {
      double unitDonorPower = 0;
      for (Donor donor : unit.getDonor()) {
        if (donorPower.containsKey(donor)) {
          unitDonorPower += donorPower.get(donor);
        } else {
          // System.err.println("missing donor power data for: " + unit);
          return 0;
        }

      }
      if (receiverPower.containsKey(unit.getReceiver())) {
        return twoPersonSensitization(unitDonorPower,
            receiverPower.get(unit.getReceiver()));
      } else {
        // System.err.println("missing receiver power for: " + unit);
        return 0;
      }

    } else if (inputs.getKepProblemData().getTerminalNodes().contains(unit)) {
      if (receiverPower.containsKey(unit.getReceiver())) {
        return singlePersonSensitization(receiverPower.get(unit.getReceiver()));
      } else {
        // System.err.println("missing receiver power for: " + unit);
        return 0;
      }

    } else {
      throw new RuntimeException();
    }
  }

  private static int singlePersonSensitization(double matchPower) {
    if (matchPower < .01) {
      return 3;
    } else if (matchPower < .08) {
      return 2;
    } else if (matchPower < .2) {
      return 1;
    } else {
      return 0;
    }
  }

  private static int twoPersonSensitization(double donorMatchPower,
      double receiverMatchPower) {
    double pmp = 10000 * donorMatchPower * receiverMatchPower;
    if (pmp < .1) {
      return 4;
    } else if (pmp < 5) {
      return 3;
    } else if (pmp < 20) {
      return 2;
    } else if (pmp < 60) {
      return 1;
    } else {
      return 0;
    }
  }

}
