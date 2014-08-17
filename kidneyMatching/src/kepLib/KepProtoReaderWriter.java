package kepLib;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CycleChainPackingPolytope.CycleChainPolytopeFractionalSolution;
import exchangeGraph.minWaitingTime.MinWaitingTimeProblemData;
import exchangeGraph.stochasticOpt.EdgeFailureScenario;
import graphUtil.CycleChainDecomposition;
import graphUtil.Edge;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import graphUtil.Node;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepProtos.KepProtos;
import kepProtos.KepProtos.Chain;
import kepProtos.KepProtos.Cycle;
import kepProtos.KepProtos.CycleValue;
import kepProtos.KepProtos.EdgeFailureScenarios;
import kepProtos.KepProtos.EdgeValue;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.google.protobuf.TextFormat;

/**
 * Class will be thread safe if Parser/TextFormat objects are thread safe.
 * Presumably they are?
 */
public class KepProtoReaderWriter extends KepIOBase {

  public KepProtoReaderWriter TEXT_INSTANCE = new KepProtoReaderWriter(
      ProtoMode.TEXT);
  public KepProtoReaderWriter COMPRESSED_INSTANCE = new KepProtoReaderWriter(
      ProtoMode.COMPRESSED);

  public static enum ProtoMode {
    TEXT, COMPRESSED;
  }

  private final ProtoMode protoMode;

  public KepProtoReaderWriter(ProtoMode protoMode) {
    this.protoMode = protoMode;
  }

  private Set<Node> makeNodes(List<String> nodeNames,
      Map<String, Node> nodeMap, DirectedSparseMultigraph<Node, Edge> graph) {
    Set<Node> ans = new HashSet<Node>();
    for (String nodeName : nodeNames) {
      if (nodeMap.containsKey(nodeName)) {
        throw new RuntimeException("Found name twice while parsing protobuf: "
            + nodeNames);
      }
      Node next = new Node(nodeName);
      ans.add(next);
      nodeMap.put(nodeName, next);
      graph.addVertex(next);
    }
    return ans;
  }

  @Override
  public KepParseData<Node, Edge> readParseData(String file) {
    KepProtos.KepInstance in = readProtobuf(file,
        KepProtos.KepInstance.getDefaultInstance());
    Map<String, Node> nodes = new HashMap<String, Node>();
    DirectedSparseMultigraph<Node, Edge> graph = new DirectedSparseMultigraph<Node, Edge>();
    Set<Node> rootNodes = makeNodes(in.getRootNodeList(), nodes, graph);
    Set<Node> pairedNodes = makeNodes(in.getPairedNodeList(), nodes, graph);
    Set<Node> terminalNodes = makeNodes(in.getTerminalNodeList(), nodes, graph);
    Map<String, Edge> edges = new HashMap<String, Edge>();
    Map<Edge, Double> edgeWeight = new HashMap<Edge, Double>();
    for (KepProtos.Edge protoEdge : in.getEdgeList()) {
      Edge edge = new Edge(protoEdge.getName());
      edgeWeight.put(edge, protoEdge.getWeight());
      graph.addEdge(edge, nodes.get(protoEdge.getSource()),
          nodes.get(protoEdge.getDest()));
    }
    KepInstance<Node, Edge> instance = new KepInstance<Node, Edge>(graph,
        rootNodes, pairedNodes, terminalNodes, Functions.forMap(edgeWeight),
        in.getMaxChainLength(), in.getMaxCycleLength(), in.getCycleBonus());
    return new KepParseData<Node, Edge>(instance, nodes, edges);
  }

  @Override
  public <V, E> ImmutableMap<E, Double> readEdgeFailureProbability(
      String fileName, KepParseData<V, E> kepParseData) {
    KepProtos.EdgeFailureProbabilities in = readProtobuf(fileName,
        KepProtos.EdgeFailureProbabilities.getDefaultInstance());
    Builder<E, Double> builder = ImmutableMap.builder();
    for (KepProtos.EdgeValue edgeValue : in.getEdgeFailureProbabilityList()) {
      builder.put(kepParseData.getEdgeNames().get(edgeValue.getEdgeName()),
          edgeValue.getValue());
    }
    return builder.build();
  }

  @Override
  public <V, E> List<EdgeFailureScenario<V, E>> readEdgeFailureScenarios(
      String fileName, KepParseData<V, E> kepParseData) {
    KepProtos.EdgeFailureScenarios in = readProtobuf(fileName,
        KepProtos.EdgeFailureScenarios.getDefaultInstance());
    List<EdgeFailureScenario<V, E>> ans = Lists.newArrayList();
    for (KepProtos.EdgeFailureScenario scenario : in.getFailureScenarioList()) {
      ans.add(new EdgeFailureScenario<V, E>(kepParseData.getInstance(),
          ImmutableSet.copyOf(Lists.transform(scenario.getFailedEdgeNameList(),
              Functions.forMap(kepParseData.getEdgeNames())))));
    }
    return ans;
  }

  private <V, E> EdgeCycle<E> createEdgeCycle(Cycle cycle,
      KepParseData<V, E> kepParseData) {
    return new EdgeCycle<E>(Lists.transform(cycle.getEdgeNameOrderedList(),
        Functions.forMap(kepParseData.getEdgeNames())));
  }

  @Override
  public <V, E> CycleChainPolytopeFractionalSolution<E> readFractionalSolution(
      String fileName, KepParseData<V, E> kepParseData) {
    KepProtos.FractionalSolution solution = readProtobuf(fileName,
        KepProtos.FractionalSolution.getDefaultInstance());
    Map<E, Double> nonZeroEdgeValues = new HashMap<E, Double>();
    for (EdgeValue edgeValue : solution.getNonZeroEdgeList()) {
      nonZeroEdgeValues.put(
          kepParseData.getEdgeNames().get(edgeValue.getEdgeName()),
          edgeValue.getValue());
    }
    Map<EdgeCycle<E>, Double> nonZeroCycleValues = new HashMap<EdgeCycle<E>, Double>();
    for (CycleValue cycleValue : solution.getNonZeroCycleList()) {
      nonZeroCycleValues.put(
          this.createEdgeCycle(cycleValue.getCycle(), kepParseData),
          cycleValue.getValue());
    }
    return new CycleChainPolytopeFractionalSolution<E>(nonZeroEdgeValues,
        nonZeroCycleValues);
  }

  @Override
  public <V, E> ImmutableSet<E> readPhaseOneSolution(String fileName,
      KepParseData<V, E> kepParseData) {
    KepProtos.PhaseOneSolution in = readProtobuf(fileName,
        KepProtos.PhaseOneSolution.getDefaultInstance());
    return ImmutableSet.copyOf(Lists.transform(in.getEdgeNameSelectedList(),
        Functions.forMap(kepParseData.getEdgeNames())));
  }

  @Override
  public <V, E> CycleChainDecomposition<V, E> readSolution(
      KepParseData<V, E> parseData, String solutionFileName) {
    KepProtos.IntegerSolution in = readProtobuf(solutionFileName,
        KepProtos.IntegerSolution.getDefaultInstance());
    List<EdgeChain<E>> chains = Lists.newArrayList();
    for (KepProtos.Chain chain : in.getChainSelectedList()) {
      chains.add(new EdgeChain<E>(Lists.transform(
          chain.getEdgeNameOrderedList(),
          Functions.forMap(parseData.getEdgeNames()))));
    }
    List<EdgeCycle<E>> cycles = Lists.newArrayList();

    for (KepProtos.Cycle cycle : in.getCycleSelectedList()) {
      cycles.add(createEdgeCycle(cycle, parseData));
    }
    return new CycleChainDecomposition<V, E>(
        parseData.getInstance().getGraph(), cycles, chains);
  }

  @Override
  public <V, E> void write(KepInstance<V, E> instance,
      Function<? super V, String> nodeNames,
      Function<? super E, String> edgeNames, String fileName) {
    List<KepProtos.Edge> edges = Lists.newArrayList();
    for (E edge : instance.getGraph().getEdges()) {
      edges.add(KepProtos.Edge.newBuilder().setName(edgeNames.apply(edge))
          .setSource(nodeNames.apply(instance.getGraph().getSource(edge)))
          .setDest(nodeNames.apply(instance.getGraph().getDest(edge)))
          .setWeight(instance.getEdgeWeights().apply(edge).doubleValue())
          .build());
    }
    KepProtos.KepInstance out = KepProtos.KepInstance
        .newBuilder()
        .addAllRootNode(Iterables.transform(instance.getRootNodes(), nodeNames))
        .addAllPairedNode(
            Iterables.transform(instance.getPairedNodes(), nodeNames))
        .addAllTerminalNode(
            Iterables.transform(instance.getTerminalNodes(), nodeNames))
        .addAllEdge(edges).setCycleBonus(instance.getCycleBonus())
        .setMaxChainLength(instance.getMaxChainLength())
        .setMaxCycleLength(instance.getMaxCycleLength()).build();
    writeProtobuf(fileName, out);

  }

  @Override
  public <V, E> void writeEdgeFailureScenarios(KepInstance<V, E> instance,
      List<EdgeFailureScenario<V, E>> scenarios,
      Function<? super E, String> edgeNames, String fileName) {
    EdgeFailureScenarios.Builder scenariosBuilder = EdgeFailureScenarios
        .newBuilder();
    for (EdgeFailureScenario<V, E> scenario : scenarios) {
      KepProtos.EdgeFailureScenario.Builder scenarioBuilder = KepProtos.EdgeFailureScenario
          .newBuilder();
      scenarioBuilder.addAllFailedEdgeName(Iterables.transform(
          scenario.getFailedEdges(), edgeNames));
      scenariosBuilder.addFailureScenario(scenarioBuilder.build());
    }
    writeProtobuf(fileName, scenariosBuilder.build());

  }

  // This presumably isn't done correctly... can't figure out the "typesafe"
  // way...
  public <T extends GeneratedMessage> T readProtobuf(String fileName,
      T defaultInstance) {
    FileInputStream stream = null;
    try {
      stream = new FileInputStream(fileName);
      if (this.protoMode == ProtoMode.COMPRESSED) {
        return ((Parser<T>) defaultInstance.getParserForType())
            .parseFrom(stream);
      } else if (this.protoMode == ProtoMode.TEXT) {
        Message.Builder builder = defaultInstance.newBuilderForType();
        TextFormat.merge(new InputStreamReader(stream), builder);
        return (T) builder.build();
      } else {
        throw new RuntimeException("Unexpected protomode: " + protoMode);
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (stream != null) {
          stream.close();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  // GROSSSSSS probably a guava way to do this better...
  private void writeProtobuf(String fileName, GeneratedMessage generatedMessage) {
    FileOutputStream output = null;
    try {
      output = new FileOutputStream(fileName);
      if (this.protoMode == ProtoMode.COMPRESSED) {
        generatedMessage.writeTo(output);
      } else if (this.protoMode == ProtoMode.TEXT) {
        TextFormat.print(generatedMessage, new OutputStreamWriter(output));
      } else {
        throw new RuntimeException("Unexpected Protomode: " + protoMode);
      }

    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (output != null) {
          output.close();
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

  }

  private <E> KepProtos.Cycle makeCycle(EdgeCycle<E> edgeCycle,
      Function<? super E, String> edgeNames) {
    return KepProtos.Cycle
        .newBuilder()
        .addAllEdgeNameOrdered(
            Iterables.transform(edgeCycle.getEdgesInOrder(), edgeNames))
        .build();
  }

  @Override
  public <E> void writeKepFractionalSolution(
      CycleChainPolytopeFractionalSolution<E> fractionalSolution,
      Function<? super E, String> edgeNames, String fileName) {
    KepProtos.FractionalSolution.Builder fractionalBuilder = KepProtos.FractionalSolution
        .newBuilder();
    for (E edge : fractionalSolution.getNonZeroEdgeValues().keySet()) {
      KepProtos.EdgeValue edgeVal = KepProtos.EdgeValue.newBuilder()
          .setEdgeName(edgeNames.apply(edge))
          .setValue(fractionalSolution.getNonZeroEdgeValues().get(edge))
          .build();
      fractionalBuilder.addNonZeroEdge(edgeVal);
    }
    for (EdgeCycle<E> edgeCycle : fractionalSolution.getNonZeroCycleValues()
        .keySet()) {
      fractionalBuilder.addNonZeroCycle(KepProtos.CycleValue.newBuilder()
          .setCycle(makeCycle(edgeCycle, edgeNames))
          .setValue(fractionalSolution.getNonZeroCycleValues().get(edgeCycle))
          .build());
    }
    writeProtobuf(fileName, fractionalBuilder.build());
  }

  @Override
  public <V, E> void writePhaseOneSolution(Set<E> edges,
      Function<? super E, String> edgeNames, String fileName) {
    writeProtobuf(fileName, KepProtos.PhaseOneSolution.newBuilder()
        .addAllEdgeNameSelected(Iterables.transform(edges, edgeNames)).build());
  }

  @Override
  public <V, E> void writeSolution(KepInstance<V, E> instance,
      CycleChainDecomposition<V, E> solution,
      Function<? super E, String> edgeNames, String fileName) {
    KepProtos.IntegerSolution.Builder builder = KepProtos.IntegerSolution
        .newBuilder();
    for (EdgeChain<E> edgeChain : solution.getEdgeChains()) {
      builder.addChainSelected(Chain
          .newBuilder()
          .addAllEdgeNameOrdered(
              Lists.transform(edgeChain.getEdgesInOrder(), edgeNames)).build());
    }
    for (EdgeCycle<E> edgeCycle : solution.getEdgeCycles()) {
      builder.addCycleSelected(this.makeCycle(edgeCycle, edgeNames));
    }
    writeProtobuf(fileName, builder.build());
  }

  @Override
  public MinWaitingTimeProblemData<Node> readNodeArrivalTimes(String fileName,
      KepParseData<Node, Edge> parseData) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Map<Node, Double> readNodeMatchingTimes(String fileName,
      KepParseData<Node, Edge> parseData) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public <V, E> void writeNodeMatchingTimes(String fileName,
      Map<V, Double> nodeMatchingTimes, Function<? super V, String> nodeNames) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public <V, E> void writeNodeArrivalTimes(String fileName,
      MinWaitingTimeProblemData<V> minWaitingTimeProblemData,
      Function<? super V, String> nodeNames) {
    throw new UnsupportedOperationException("Not implemented yet");

  }

}
