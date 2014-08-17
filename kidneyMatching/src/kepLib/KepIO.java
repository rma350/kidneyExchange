package kepLib;

import exchangeGraph.CycleChainPackingPolytope.CycleChainPolytopeFractionalSolution;
import exchangeGraph.minWaitingTime.MinWaitingTimeProblemData;
import exchangeGraph.stochasticOpt.EdgeFailureScenario;
import graphUtil.CycleChainDecomposition;
import graphUtil.Edge;
import graphUtil.Node;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public interface KepIO {

  public KepParseData<Node, Edge> readParseData(String file);

  public KepInstance<Node, Edge> read(String file);

  public <V, E> ImmutableMap<E, Double> readEdgeFailureProbability(
      String fileName, KepParseData<V, E> kepParseData);

  public <V, E> List<EdgeFailureScenario<V, E>> readEdgeFailureScenarios(
      String fileName, KepParseData<V, E> kepParseData);

  public <V, E> CycleChainPolytopeFractionalSolution<E> readFractionalSolution(
      String fileName, KepParseData<V, E> kepParseData);

  public <V, E> ImmutableSet<E> readPhaseOneSolution(String fileName,
      KepParseData<V, E> kepParseData);

  public <V, E> CycleChainDecomposition<V, E> readSolution(
      KepParseData<V, E> parseData, String solutionFileName);

  // For the MinWaitingTimeKepSolver
  public MinWaitingTimeProblemData<Node> readNodeArrivalTimes(String fileName,
      KepParseData<Node, Edge> parseData);

  public Map<Node, Double> readNodeMatchingTimes(String fileName,
      KepParseData<Node, Edge> parseData);

  public <V, E> void write(KepInstance<V, E> instance,
      Function<? super V, String> nodeNames,
      Function<? super E, String> edgeNames, String fileName);

  public <V, E> void writeAnonymous(KepInstance<V, E> instance, String fileName);

  public <V, E> void writeWithToString(KepInstance<V, E> instance,
      String fileName);

  public <V, E> void writeEdgeFailureScenarios(KepInstance<V, E> instance,
      List<EdgeFailureScenario<V, E>> scenarios,
      Function<? super E, String> edgeNames, String fileName);

  public <E> void writeKepFractionalSolution(
      CycleChainPolytopeFractionalSolution<E> fractionalSolution,
      Function<? super E, String> edgeNames, String fileName);

  public <V, E> void writePhaseOneSolution(Set<E> edges,
      Function<? super E, String> edgeNames, String fileName);

  public <V, E> void writeSolution(KepInstance<V, E> instance,
      CycleChainDecomposition<V, E> solution,
      Function<? super E, String> edgeNames, String fileName);

  // For the MinWaitingTimeKepSolver
  public <V, E> void writeNodeMatchingTimes(String fileName,
      Map<V, Double> nodeMatchingTimes, Function<? super V, String> nodeNames);

  public <V, E> void writeNodeArrivalTimes(String fileName,
      MinWaitingTimeProblemData<V> minWaitingTimeProblemData,
      Function<? super V, String> nodeNames);

}
