package exchangeGraph;

import graphUtil.CycleChainDecomposition;
import graphUtil.EdgeCycle;
import graphUtil.SubgraphUtil;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Algorithm;
import ilog.cplex.IloCplex.IntParam;

import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

import threading.FixedThreadPool;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.math.DoubleMath;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

/**
 * Solves the max flow min cut problem under the additional assumption that
 * every node except the source and sink have a flow capacity of one.
 * 
 * Algorithm used is linear programming via CPLEX.
 * 
 * @author ross
 * 
 * @param <V>
 * @param <E>
 */
public class UnitNodeCapacityMaxFlowSolver<V, E> {

  private DirectedSparseMultigraph<V, E> graph;
  private V source;
  private V sink;
  private Map<E, Integer> nonZeroEdgeWeights;
  private Optional<FixedThreadPool> threadPool;

  private IloCplex cplex;
  private NumVariableSet<E> edgeVars;

  private int objValue;
  private Set<E> edgesInSolution;

  public UnitNodeCapacityMaxFlowSolver(DirectedSparseMultigraph<V, E> graph,
      V source, V sink, Map<E, Integer> nonZeroEdgeWeights,
      boolean displayOutput, Optional<FixedThreadPool> threadPool) {
    super();
    this.graph = graph;
    this.source = source;
    this.sink = sink;
    this.nonZeroEdgeWeights = nonZeroEdgeWeights;
    this.threadPool = threadPool;

    try {
      this.cplex = new IloCplex();
      if (!displayOutput) {
        cplex.setOut(null);
      }
      cplex.setParam(IntParam.RootAlg, Algorithm.Network);
      if (threadPool.isPresent()) {
        cplex.setParam(IntParam.Threads, threadPool.get().getNumThreads());
      }
      this.edgeVars = new NumVariableSet<E>(Sets.newHashSet(graph.getEdges()),
          0.0, 1.0, cplex);
      cplex.addMaximize(edgeVars.doubleSum(nonZeroEdgeWeights.keySet(),
          Functions.forMap(nonZeroEdgeWeights)));
      for (V v : graph.getVertices()) {
        if (v != source && v != sink) {
          IloLinearNumExpr flowIn = edgeVars.doubleSum(graph.getInEdges(v),
              CplexUtil.unity);
          IloLinearNumExpr flowOut = edgeVars.doubleSum(graph.getOutEdges(v),
              CplexUtil.unity);
          cplex.addEq(flowIn, flowOut);
          cplex.addLe(flowIn, 1);
        }
      }
      cplex.solve();
      edgesInSolution = Sets.newHashSet();
      for (Map.Entry<E, Double> entry : edgeVars.getVariableValues().entrySet()) {
        if (CplexUtil.doubleToBoolean(entry.getValue())) {
          edgesInSolution.add(entry.getKey());
        }
      }
      removeCycles();
      double cplexSol = cplex.getObjValue();
      if (!CplexUtil.doubleIsInteger(cplexSol)) {
        throw new RuntimeException(
            "Expected an integer optimal solution, but found: " + cplexSol);
      }
      this.objValue = DoubleMath.roundToInt(cplexSol, RoundingMode.HALF_UP);
      cplex.end();
    } catch (IloException e) {
      throw new RuntimeException(e);
    }
  }

  private void removeCycles() {
    Set<V> noSourceSink = Sets.newHashSet(graph.getVertices());
    noSourceSink.remove(this.source);
    noSourceSink.remove(sink);
    Set<E> restrictedEdges = Sets.newHashSet();
    for (E edge : this.edgesInSolution) {
      V start = graph.getSource(edge);
      V end = graph.getDest(edge);
      if (start != this.source && end != this.sink) {
        restrictedEdges.add(edge);
      }
    }
    DirectedSparseMultigraph<V, E> degreeTwoGraph = SubgraphUtil.subgraph(
        graph, noSourceSink, restrictedEdges);
    CycleChainDecomposition<V, E> cycleChainDecomp = new CycleChainDecomposition<V, E>(
        degreeTwoGraph);
    for (EdgeCycle<E> edgeCycle : cycleChainDecomp.getEdgeCycles()) {
      this.edgesInSolution.removeAll(edgeCycle.getEdges());
    }
  }

  public double getObjValue() {
    return this.objValue;
  }

  public Set<E> getEdgesInSolution() {
    return this.edgesInSolution;
  }

}
