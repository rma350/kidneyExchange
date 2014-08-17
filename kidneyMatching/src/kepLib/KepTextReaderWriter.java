package kepLib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kepLib.KepInstance.BridgeConstraint;
import kepLib.KepInstance.EdgeConstraint;
import kepLib.KepInstance.NodeConstraint;
import kepLib.KepInstance.NodeFlowInConstraint;
import kepLib.KepInstance.NodeFlowOutConstraint;
import kepLib.KepInstance.RelationType;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CycleChainPackingPolytope.CycleChainPolytopeFractionalSolution;
import exchangeGraph.minWaitingTime.MinWaitingTimeProblemData;
import exchangeGraph.stochasticOpt.EdgeFailureScenario;
import graphUtil.CycleChainDecomposition;
import graphUtil.Edge;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import graphUtil.Node;

public class KepTextReaderWriter extends KepIOBase {

  public static final KepTextReaderWriter INSTANCE = new KepTextReaderWriter();

  private KepTextReaderWriter() {
    super();
  }

  private static String startComment = "#";
  private static String delimeter = ",";

  private static String problemData = "problemData";
  private static String endProblemData = "endProblemData";
  private static String maxChainLength = "maxChainLength";
  private static String infinity = "Infinity";
  private static String maxCycleLength = "maxCycleLength";
  private static String cycleBonus = "cycleBonus";
  private static String rootNodes = "rootNodes";
  private static String endRootNodes = "endRootNodes";
  private static String pairedNodes = "pairedNodes";
  private static String endPairedNodes = "endPairedNodes";
  private static String terminalNodes = "terminalNodes";
  private static String endTerminalNodes = "endTerminalNodes";
  private static String edges = "edges";
  private static String endEdges = "endEdges";
  private static String bridgeConstraint = "bridgeConstraint";
  private static String endBridgeConstraint = "endBridgeConstraint";
  private static String nodeFlowInConstraint = "nodeFlowInConstraint";
  private static String endNodeFlowInConstraint = "endNodeFlowInConstraint";
  private static String nodeFlowOutConstraint = "nodeFlowOutConstraint";
  private static String endNodeFlowOutConstraint = "endNodeFlowOutConstraint";
  private static String edgeConstraint = "edgeConstraint";
  private static String endEdgeConstraint = "endEdgeConstraint";

  private static String edgeFailureProbabilities = "edgeFailureProbability";
  private static String endEdgeFailureProbabilities = "endEdgeFailureProbability";

  private static String cycleOut = "cycle";
  private static String endCycleOut = "endCycle";

  private static String chainOut = "chain";
  private static String endChainOut = "endChain";

  private static String edgeFailureScenarios = "edgeFailureScenarios";
  private static String endEdgeFailureScenarios = "endEdgeFailureScenarios";
  private static String numScenarios = "numScenarios";
  private static String edgeSet = "edgeSet";
  private static String endEdgeSet = "endEdgeSet";

  private static String variableValue = "variableValue";

  private static String terminationTime = "terminationTime";
  private static String startNodeArrivalTimes = "nodeArrivalTimes";
  private static String endNodeArrivalTimes = "endNodeArrivalTimes";

  private static String startNodeMatchTimes = "nodeMatchTimes";
  private static String endNodeMatchTimes = "endNodeMatchTimes";

  private static Splitter lineSplitter = Splitter.on(delimeter)
      .omitEmptyStrings().trimResults();

  private static Joiner lineJoiner = Joiner.on(delimeter);

  // TODO fill this in
  @Override
  public <E> void writeKepFractionalSolution(
      CycleChainPolytopeFractionalSolution<E> fractionalSolution,
      Function<? super E, String> edgeNames, String fileName) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

      writeLine(writer, edges,
          Double.toString(fractionalSolution.getNonZeroEdgeValues().size()));
      for (E edge : fractionalSolution.getNonZeroEdgeValues().keySet()) {
        writeLine(writer, edgeNames.apply(edge),
            Double
                .toString(fractionalSolution.getNonZeroEdgeValues().get(edge)));
      }
      writer.write(endEdges);
      for (EdgeCycle<E> cycle : fractionalSolution.getNonZeroCycleValues()
          .keySet()) {
        writeLine(
            writer,
            cycleOut,
            Integer.toString(cycle.size()),
            variableValue,
            Double.toString(fractionalSolution.getNonZeroCycleValues().get(
                cycle)));
        for (E edge : cycle) {
          writeLine(writer, edgeNames.apply(edge));
        }
        writeLine(writer, endCycleOut);
      }
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  // TODO fill this in
  @Override
  public <V, E> CycleChainPolytopeFractionalSolution<E> readFractionalSolution(
      String fileName, KepParseData<V, E> kepParseData) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <V, E> void writeEdgeFailureScenarios(KepInstance<V, E> instance,
      List<EdgeFailureScenario<V, E>> scenarios,
      Function<? super E, String> edgeNames, String fileName) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      writeLine(writer, edgeFailureScenarios);
      writeLine(writer, numScenarios, Integer.toString(scenarios.size()));
      writeLine(writer, endEdgeFailureScenarios);
      for (EdgeFailureScenario<V, E> scenario : scenarios) {
        writeEdgeSet(scenario.getFailedEdges(), edgeNames, writer);
      }
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  @Override
  public <V, E> void writePhaseOneSolution(Set<E> edges,
      Function<? super E, String> edgeNames, String fileName) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      writeEdgeSet(edges, edgeNames, writer);
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  private static <E> void writeEdgeSet(Set<E> edges,
      Function<? super E, String> edgeNames, BufferedWriter writer)
      throws IOException {
    writeLine(writer, edgeSet, Integer.toString(edges.size()));
    for (E edge : edges) {
      writeLine(writer, edgeNames.apply(edge));
    }
    writeLine(writer, endEdgeSet);
  }

  @Override
  public <V, E> ImmutableSet<E> readPhaseOneSolution(String fileName,
      KepParseData<V, E> kepParseData) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(fileName));
      ImmutableSet<E> ans = readEdgeSet(reader, kepParseData);
      reader.close();
      return ans;
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  private static <V, E> ImmutableSet<E> readEdgeSet(BufferedReader reader,
      KepParseData<V, E> kepParseData) throws IOException {
    int numEdges = advanceToExtractInt(edgeSet, reader);
    List<String> failureEdges = extractListAndTerminate(numEdges, endEdgeSet,
        reader);
    ImmutableSet.Builder<E> edges = ImmutableSet.builder();
    for (String failure : failureEdges) {
      edges.add(kepParseData.getEdgeNames().get(failure));
    }
    return edges.build();
  }

  @Override
  public <V, E> List<EdgeFailureScenario<V, E>> readEdgeFailureScenarios(
      String fileName, KepParseData<V, E> kepParseData) {
    try {
      List<EdgeFailureScenario<V, E>> ans = Lists.newArrayList();
      BufferedReader reader = new BufferedReader(new FileReader(fileName));
      advanceTo(edgeFailureScenarios, 1, reader);
      int scenarioCount = advanceToExtractInt(numScenarios, reader);
      advanceTo(endEdgeFailureScenarios, 1, reader);
      for (int i = 0; i < scenarioCount; i++) {
        ans.add(new EdgeFailureScenario<V, E>(kepParseData.getInstance(),
            readEdgeSet(reader, kepParseData)));
      }
      reader.close();
      return ans;
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  @Override
  public <V, E> CycleChainDecomposition<V, E> readSolution(
      KepParseData<V, E> parseData, String solutionFileName) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(
          solutionFileName));
      String nextLine = getNextLine(reader);
      List<EdgeCycle<E>> cycles = Lists.newArrayList();
      List<EdgeChain<E>> chains = Lists.newArrayList();
      while (nextLine != null) {
        List<String> header = Lists.newArrayList(lineSplitter.split(nextLine));
        if (header.size() != 2) {
          throw new RuntimeException("Expceted 2"
              + " tokens in first non comment line, but found " + header.size()
              + ", line was " + nextLine + ", tokenized to "
              + header.toString());
        }
        int numEdges = Integer.parseInt(header.get(1));
        if (header.get(0).equalsIgnoreCase(cycleOut)) {
          List<String> edges = extractListAndTerminate(numEdges, endCycleOut,
              reader);
          cycles.add(new EdgeCycle<E>(edgeLookupByName(parseData, edges)));
        } else if (header.get(0).equalsIgnoreCase(chainOut)) {
          List<String> edges = extractListAndTerminate(numEdges, endChainOut,
              reader);
          chains.add(new EdgeChain<E>(edgeLookupByName(parseData, edges)));
        } else {
          throw new RuntimeException("Unexpected line header for first token: "
              + header);
        }
        nextLine = getNextLine(reader);
      }
      return new CycleChainDecomposition<V, E>(parseData.getInstance()
          .getGraph(), cycles, chains);
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  private static <V, E> List<E> edgeLookupByName(KepParseData<V, E> parseData,
      List<String> edgeList) {
    List<E> ans = Lists.newArrayList();
    for (String edgeName : edgeList) {
      E edge = parseData.getEdgeNames().get(edgeName);
      if (edge == null) {
        throw new RuntimeException("Unrecognized edge name: " + edgeName);
      }
      ans.add(edge);
    }
    return ans;
  }

  @Override
  public <V, E> void writeSolution(KepInstance<V, E> instance,
      CycleChainDecomposition<V, E> solution,
      Function<? super E, String> edgeNames, String fileName) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      for (EdgeChain<E> chain : solution.getEdgeChains()) {
        writeLine(writer, chainOut, Integer.toString(chain.size()));
        for (E edge : chain.getEdgesInOrder()) {
          writeLine(writer, edgeNames.apply(edge));
        }
        writeLine(writer, endChainOut);
      }
      for (EdgeCycle<E> cycle : solution.getEdgeCycles()) {
        writeLine(writer, cycleOut, Integer.toString(cycle.size()));
        for (E edge : cycle.getEdgesInOrder()) {
          writeLine(writer, edgeNames.apply(edge));
        }
        writeLine(writer, endCycleOut);
      }
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <V, E> void write(KepInstance<V, E> instance,
      Function<? super V, String> nodeNames,
      Function<? super E, String> edgeNames, String fileName) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      writeLine(writer, problemData);
      writeLine(writer, maxChainLength,
          intToString(instance.getMaxChainLength()));
      writeLine(writer, maxCycleLength,
          intToString(instance.getMaxCycleLength()));
      writeLine(writer, cycleBonus, Double.toString(instance.getCycleBonus()));
      writeLine(writer, endProblemData);
      writeLine(writer, rootNodes, intToString(instance.getRootNodes().size()));
      for (V vertex : instance.getRootNodes()) {
        writeLine(writer, nodeNames.apply(vertex));
      }
      writeLine(writer, endRootNodes);

      writeLine(writer, pairedNodes, intToString(instance.getPairedNodes()
          .size()));
      for (V vertex : instance.getPairedNodes()) {
        writeLine(writer, nodeNames.apply(vertex));
      }
      writeLine(writer, endPairedNodes);

      writeLine(writer, terminalNodes, intToString(instance.getTerminalNodes()
          .size()));
      for (V vertex : instance.getTerminalNodes()) {
        writeLine(writer, nodeNames.apply(vertex));
      }
      writeLine(writer, endTerminalNodes);

      writeLine(writer, edges, intToString(instance.getGraph().getEdgeCount()));
      for (E edge : instance.getGraph().getEdges()) {
        writeLine(writer, edgeNames.apply(edge),
            nodeNames.apply(instance.getGraph().getSource(edge)),
            nodeNames.apply(instance.getGraph().getDest(edge)), instance
                .getEdgeWeights().apply(edge).toString());
      }
      writeLine(writer, endEdges);
      for (NodeConstraint<V> nodeFlowIn : instance.getNodeFlowInConstraints()) {
        writeNodeConstraint(nodeFlowInConstraint, endNodeFlowInConstraint,
            nodeFlowIn, nodeNames, writer);
      }
      for (NodeConstraint<V> nodeFlowOut : instance.getNodeFlowOutConstraints()) {
        writeNodeConstraint(nodeFlowOutConstraint, endNodeFlowOutConstraint,
            nodeFlowOut, nodeNames, writer);
      }
      for (EdgeConstraint<E> edgeUsageConstraint : instance
          .getEdgeUsageConstraints()) {
        writeLine(writer, edgeConstraint, intToString(edgeUsageConstraint
            .getEdges().size()), edgeUsageConstraint.getRelationType()
            .toString(), Double.toString(edgeUsageConstraint.getRhs()));
        for (E edge : edgeUsageConstraint.getEdges()) {
          writeLine(
              writer,
              edgeNames.apply(edge),
              Double.toString(edgeUsageConstraint.getCoefficients().apply(edge)
                  .doubleValue()));
        }
        writeLine(writer, endEdgeConstraint);
      }

      for (BridgeConstraint<V> bridgeUsageConstraint : instance
          .getBridgeConstraints()) {
        writeNodeConstraint(bridgeConstraint, endBridgeConstraint,
            bridgeUsageConstraint, nodeNames, writer);
      }
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static <V> void writeNodeConstraint(String start, String end,
      NodeConstraint<V> nodeConstraint, Function<? super V, String> nodeNames,
      BufferedWriter writer) throws IOException {
    writeLine(writer, start, intToString(nodeConstraint.getNodes().size()),
        nodeConstraint.getRelationType().toString(),
        Double.toString(nodeConstraint.getRhs()));
    for (V node : nodeConstraint.getNodes()) {
      writeLine(
          writer,
          nodeNames.apply(node),
          Double.toString(nodeConstraint.getCoefficients().apply(node)
              .doubleValue()));
    }
    writeLine(writer, end);
  }

  private static void writeLine(BufferedWriter writer, String... items)
      throws IOException {
    String joined = lineJoiner.join(items);
    writer.write(joined);
    writer.newLine();
  }

  @Override
  public <V, E> ImmutableMap<E, Double> readEdgeFailureProbability(
      String fileName, KepParseData<V, E> kepParseData) {
    try {
      ImmutableMap.Builder<E, Double> ans = ImmutableMap.builder();
      Map<String, E> edgeNames = kepParseData.getEdgeNames();
      BufferedReader reader = new BufferedReader(new FileReader(fileName));
      int numEdges = advanceToExtractInt(edgeFailureProbabilities, reader);
      Map<String, Double> failureProbs = parseMap(edgeNames.keySet(),
          edgeNames.size(), endEdgeFailureProbabilities, reader);
      for (String edgeName : edgeNames.keySet()) {
        ans.put(edgeNames.get(edgeName), failureProbs.get(edgeName));
      }
      reader.close();
      return ans.build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public KepParseData<Node, Edge> readParseData(String file) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      advanceTo(problemData, 1, reader);
      int chainLength = advanceToExtractInt(maxChainLength, reader);
      int cycleLength = advanceToExtractInt(maxCycleLength, reader);
      double cycleBonusVal = advanceToExtractDouble(cycleBonus, reader);
      advanceTo(endProblemData, 1, reader);
      Set<String> checkDuplicateNodeNames = new HashSet<String>();
      Map<String, Node> rootNodesMap = makeNodeMap(rootNodes, endRootNodes,
          checkDuplicateNodeNames, reader);
      Map<String, Node> pairedNodesMap = makeNodeMap(pairedNodes,
          endPairedNodes, checkDuplicateNodeNames, reader);
      Map<String, Node> terminalNodeMap = makeNodeMap(terminalNodes,
          endTerminalNodes, checkDuplicateNodeNames, reader);
      Map<String, Node> allNodes = new HashMap<String, Node>();
      allNodes.putAll(rootNodesMap);
      allNodes.putAll(pairedNodesMap);
      allNodes.putAll(terminalNodeMap);
      DirectedSparseMultigraph<Node, Edge> graph = new DirectedSparseMultigraph<Node, Edge>();
      for (Node node : allNodes.values()) {
        graph.addVertex(node);
      }
      Map<String, Edge> edgeMap = new HashMap<String, Edge>();
      Function<Edge, Double> edgeWeights = extractAllEdges(allNodes, edgeMap,
          graph, reader);
      KepInstance<Node, Edge> ans = new KepInstance<Node, Edge>(graph,
          new HashSet<Node>(rootNodesMap.values()), new HashSet<Node>(
              pairedNodesMap.values()), new HashSet<Node>(
              terminalNodeMap.values()), edgeWeights, chainLength, cycleLength,
          cycleBonusVal);

      String sideConstraintTitle = getNextLine(reader);
      while (sideConstraintTitle != null) {
        List<String> tokens = Lists.newArrayList(lineSplitter
            .split(sideConstraintTitle));
        if (tokens.size() != 4) {
          throw new RuntimeException("Expected 4 tokens but found "
              + tokens.toString());
        }
        String type = tokens.get(0);
        int numEls = Integer.parseInt(tokens.get(1));
        RelationType relation = KepInstance.parseRelationType(tokens.get(2));
        double rhs = Double.parseDouble(tokens.get(3));
        if (type.equalsIgnoreCase(nodeFlowInConstraint)) {
          ans.getNodeFlowInConstraints()
              .add(
                  parseNodeFlowInConstraint(allNodes, numEls, relation, rhs,
                      reader));
        } else if (type.equalsIgnoreCase(nodeFlowOutConstraint)) {
          ans.getNodeFlowOutConstraints().add(
              parseNodeFlowOutConstraint(allNodes, numEls, relation, rhs,
                  reader));
        } else if (type.equalsIgnoreCase(edgeConstraint)) {
          ans.getEdgeUsageConstraints().add(
              parseEdgeConstraint(edgeMap, numEls, relation, rhs, reader));
        } else if (type.equalsIgnoreCase(bridgeConstraint)) {
          ans.getBridgeConstraints().add(
              parseBridgeConstraint(allNodes, numEls, relation, rhs, reader));
        } else {
          throw new RuntimeException(
              "Side constraint type did not match any known types, tokens: "
                  + tokens.toString());
        }
        sideConstraintTitle = getNextLine(reader);
      }
      reader.close();
      return new KepParseData<Node, Edge>(ans, allNodes, edgeMap);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static NodeFlowInConstraint<Node> parseNodeFlowInConstraint(
      Map<String, Node> nodeMap, int numNodes, RelationType constraintType,
      double rhs, BufferedReader reader) throws IOException {
    Map<String, Double> nodeVals = parseMap(nodeMap.keySet(), numNodes,
        endNodeFlowInConstraint, reader);
    Map<Node, Double> weights = new HashMap<Node, Double>();
    for (String key : nodeVals.keySet()) {
      weights.put(nodeMap.get(key), nodeVals.get(key));
    }
    Function<Node, Double> weightFunction = Functions.forMap(weights);
    return new NodeFlowInConstraint<Node>(new HashSet<Node>(weights.keySet()),
        weightFunction, constraintType, rhs);
  }

  private static NodeFlowOutConstraint<Node> parseNodeFlowOutConstraint(
      Map<String, Node> nodeMap, int numNodes, RelationType constraintType,
      double rhs, BufferedReader reader) throws IOException {
    Map<String, Double> nodeVals = parseMap(nodeMap.keySet(), numNodes,
        endNodeFlowOutConstraint, reader);
    Map<Node, Double> weights = new HashMap<Node, Double>();
    for (String key : nodeVals.keySet()) {
      weights.put(nodeMap.get(key), nodeVals.get(key));
    }
    Function<Node, Double> weightFunction = Functions.forMap(weights);
    return new NodeFlowOutConstraint<Node>(new HashSet<Node>(weights.keySet()),
        weightFunction, constraintType, rhs);
  }

  private static EdgeConstraint<Edge> parseEdgeConstraint(
      Map<String, Edge> edgeMap, int numEdges, RelationType constraintType,
      double rhs, BufferedReader reader) throws IOException {
    Map<Edge, Double> weights = new HashMap<Edge, Double>();
    Map<String, Double> edgeVals = parseMap(edgeMap.keySet(), numEdges,
        endEdgeConstraint, reader);
    for (String key : edgeVals.keySet()) {
      weights.put(edgeMap.get(key), edgeVals.get(key));
    }
    return new EdgeConstraint<Edge>(new HashSet<Edge>(weights.keySet()),
        Functions.forMap(weights), constraintType, rhs);
  }

  private static BridgeConstraint<Node> parseBridgeConstraint(
      Map<String, Node> nodeMap, int numNodes, RelationType constraintType,
      double rhs, BufferedReader reader) throws IOException {
    Map<String, Double> nodeVals = parseMap(nodeMap.keySet(), numNodes,
        endBridgeConstraint, reader);
    Map<Node, Double> weights = new HashMap<Node, Double>();
    for (String key : nodeVals.keySet()) {
      weights.put(nodeMap.get(key), nodeVals.get(key));
    }
    Function<Node, Double> weightFunction = Functions.forMap(weights);
    return new BridgeConstraint<Node>(new HashSet<Node>(weights.keySet()),
        weightFunction, constraintType, rhs);
  }

  private static Map<String, Double> parseMap(Set<String> possibleKeys,
      int numMembers, String terminationString, BufferedReader reader)
      throws IOException {
    Map<String, Double> ans = new HashMap<String, Double>();
    for (int i = 0; i < numMembers; i++) {
      String nextLine = getNextLineNullImpossible(reader);
      List<String> tokens = Lists.newArrayList(lineSplitter.split(nextLine));
      if (tokens.size() != 2) {
        throw new RuntimeException("Expected two tokens but found: "
            + tokens.toString());
      }
      if (!possibleKeys.contains(tokens.get(0))) {
        throw new RuntimeException("Unexpected Key: " + tokens.get(0));
      }
      double val = Double.parseDouble(tokens.get(1));
      ans.put(tokens.get(0), val);
    }
    advanceTo(terminationString, 1, reader);
    return ans;
  }

  // this function populates edgeMap
  private static Function<Edge, Double> extractAllEdges(
      Map<String, Node> nodeMap, Map<String, Edge> edgeMap,
      DirectedSparseMultigraph<Node, Edge> graph, BufferedReader reader)
      throws IOException {
    int numEdges = advanceToExtractInt(edges, reader);

    Map<Edge, Double> edgeWeights = new HashMap<Edge, Double>();
    for (int i = 0; i < numEdges; i++) {
      String nextLine = getNextLineNullImpossible(reader);
      List<String> tokens = Lists.newArrayList(lineSplitter.split(nextLine));
      if (tokens.size() != 4) {
        throw new RuntimeException("Expected four tokens but found " + tokens);
      }
      String edgeName = tokens.get(0);
      if (edgeMap.containsKey(edgeName)) {
        throw new RuntimeException("Found duplicate edge name: " + edgeName);
      } else {

        Edge edge = new Edge(edgeName);
        edgeMap.put(edgeName, edge);
        Node source = nodeMap.get(tokens.get(1));
        if (source == null) {
          throw new RuntimeException("for edge " + edgeName
              + " could not find source node " + tokens.get(1));
        }
        Node sink = nodeMap.get(tokens.get(2));
        if (sink == null) {
          throw new RuntimeException("for edge " + edgeName
              + " could not find sink node " + tokens.get(2));
        }
        double weight = Double.parseDouble(tokens.get(3));
        graph.addEdge(edge, source, sink);
        edgeWeights.put(edge, weight);
      }
    }
    advanceTo(endEdges, 1, reader);
    return Functions.forMap(edgeWeights);
  }

  private static Map<String, Node> makeNodeMap(String startWord,
      String endWord, Set<String> checkDuplicates, BufferedReader reader)
      throws IOException {
    Map<String, Node> nodeMap = new HashMap<String, Node>();
    {
      int numRootNodes = advanceToExtractInt(startWord, reader);
      List<String> rootNodeNames = extractListAndTerminate(numRootNodes,
          endWord, reader);
      safeAddNode(rootNodeNames, checkDuplicates, nodeMap);
    }
    return nodeMap;
  }

  private static void safeAddNode(List<String> nodeNames,
      Set<String> checkDuplicates, Map<String, Node> nodeMap) {
    for (String nodeName : nodeNames) {
      if (checkDuplicates.contains(nodeName)) {
        throw new RuntimeException("Found two nodes named: " + nodeName);
      } else {
        checkDuplicates.add(nodeName);
        nodeMap.put(nodeName, new Node(nodeName));
      }
    }
  }

  private static List<String> extractListAndTerminate(int expectedElements,
      String expectedTermination, BufferedReader reader) throws IOException {
    List<String> ans = new ArrayList<String>();
    for (int i = 0; i < expectedElements; i++) {
      String token = extractSingleToken(reader);
      if (token.equalsIgnoreCase(expectedTermination)) {
        throw new RuntimeException("Found " + expectedTermination + " on line "
            + i + " lines but should have been " + expectedElements);
      } else {
        ans.add(token);
      }
    }
    advanceTo(expectedTermination, 1, reader);
    return ans;
  }

  private static String extractSingleToken(BufferedReader reader)
      throws IOException {
    String nextLine = getNextLineNullImpossible(reader);
    List<String> tokens = Lists.newArrayList(lineSplitter.split(nextLine));
    if (tokens.size() == 1) {
      return tokens.get(0);
    }
    throw new RuntimeException("Expected only one token but found: "
        + tokens.toString());
  }

  private static int stringToInt(String intString) {
    if (intString.equalsIgnoreCase(infinity)) {
      return Integer.MAX_VALUE;
    } else {
      return Integer.parseInt(intString);
    }
  }

  private static String intToString(int intVal) {
    if (intVal == Integer.MAX_VALUE) {
      return infinity;
    } else {
      return Integer.toString(intVal);
    }
  }

  private static double advanceToExtractDouble(String expectedFirstToken,
      BufferedReader reader) throws IOException {
    String doubleString = advanceTo(expectedFirstToken, 2, reader).get(1);
    return Double.parseDouble(doubleString);
  }

  private static int advanceToExtractInt(String expectedFirstToken,
      BufferedReader reader) throws IOException {
    String intString = advanceTo(expectedFirstToken, 2, reader).get(1);
    return stringToInt(intString);
  }

  private static List<String> advanceTo(String expectedFirstToken,
      int expectedNumTokens, BufferedReader reader) throws IOException {
    List<String> ans = advanceTo(expectedNumTokens, reader);
    if (!ans.get(0).equalsIgnoreCase(expectedFirstToken)) {
      throw new RuntimeException("Expected first non comment to be: "
          + expectedFirstToken + ", but was:" + ans.get(0) + " in line " + ans);
    }
    return ans;
  }

  private static List<String> advanceTo(int expectedNumTokens,
      BufferedReader reader) throws IOException {
    if (expectedNumTokens <= 0) {
      throw new RuntimeException("Must expect a positive number of tokens");
    }
    String nextLine = getNextLineNullImpossible(reader);
    List<String> ans = Lists.newArrayList(lineSplitter.split(nextLine));
    if (ans.size() != expectedNumTokens) {
      throw new RuntimeException("Expceted " + expectedNumTokens
          + " tokens in first non comment line, but found " + ans.size()
          + ", line was " + nextLine + ", tokenized to " + ans.toString());
    }
    return ans;
  }

  private static String getNextLineNullImpossible(BufferedReader reader)
      throws IOException {
    String ans = getNextLine(reader);
    if (ans == null) {
      throw new RuntimeException("Reached end of file unexpectedly");
    }
    return ans;
  }

  private static String getNextLine(BufferedReader reader) throws IOException {
    String nextLine = reader.readLine();
    while (nextLine != null) {
      if (nextLine.startsWith(startComment) || nextLine.trim().isEmpty()) {
        nextLine = reader.readLine();
      } else {
        return nextLine;
      }
    }
    return null;
  }

  @Override
  public MinWaitingTimeProblemData<Node> readNodeArrivalTimes(String fileName,
      KepParseData<Node, Edge> parseData) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(fileName));
      double endTime = advanceToExtractDouble(terminationTime, reader);
      ImmutableMap<Node, Double> ans = readNodeValues(reader,
          startNodeArrivalTimes, endNodeArrivalTimes, parseData);
      reader.close();
      return new MinWaitingTimeProblemData<Node>(ans, endTime);
    } catch (IOException e) {
      throw new RuntimeException();
    }

  }

  public ImmutableMap<Node, Double> readNodeValues(BufferedReader reader,
      String start, String end, KepParseData<Node, Edge> kepParseData) {
    try {
      int nodes = advanceToExtractInt(start, reader);
      ImmutableMap.Builder<Node, Double> ans = ImmutableMap.builder();
      Map<String, Double> nodeVals = parseMap(kepParseData.nodeNames.keySet(),
          nodes, end, reader);
      for (String key : nodeVals.keySet()) {
        ans.put(kepParseData.getNodeNames().get(key), nodeVals.get(key));
      }
      return ans.build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> void writeMap(BufferedWriter writer, Map<T, ? extends Number> map,
      String start, String end, Function<? super T, String> keyNames)
      throws IOException {
    writeLine(writer, start, Integer.toString(map.size()));
    for (Map.Entry<T, ? extends Number> entry : map.entrySet()) {
      writeLine(writer, keyNames.apply(entry.getKey()),
          Double.toString(entry.getValue().doubleValue()));
    }
    writeLine(writer, end);
  }

  @Override
  public Map<Node, Double> readNodeMatchingTimes(String fileName,
      KepParseData<Node, Edge> parseData) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(fileName));
      ImmutableMap<Node, Double> ans = readNodeValues(reader,
          startNodeMatchTimes, endNodeMatchTimes, parseData);
      reader.close();
      return ans;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <V, E> void writeNodeMatchingTimes(String fileName,
      Map<V, Double> nodeMatchingTimes, Function<? super V, String> nodeNames) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      writeMap(writer, nodeMatchingTimes, startNodeMatchTimes,
          endNodeMatchTimes, nodeNames);
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public <V, E> void writeNodeArrivalTimes(String fileName,
      MinWaitingTimeProblemData<V> minWaitingTimeProblemData,
      Function<? super V, String> nodeNames) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      writeLine(writer, terminationTime,
          Double.toString(minWaitingTimeProblemData.getTerminationTime()));

      writeMap(writer, minWaitingTimeProblemData.getNodeArrivalTime(),
          startNodeArrivalTimes, endNodeArrivalTimes, nodeNames);
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
