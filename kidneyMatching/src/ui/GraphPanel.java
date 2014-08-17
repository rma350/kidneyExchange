package ui;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;

import javax.swing.JPanel;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;

public class GraphPanel<V, E> extends JPanel {

  private DirectedSparseMultigraph<V, E> graph;
  private Layout<V, E> layout;

  public GraphPanel(DirectedSparseMultigraph<V, E> graph) {
    // this.setLayout(new FlowLayout());
    DirectedSparseMultigraph<V, E> graphTemp = new DirectedSparseMultigraph<V, E>();
    for (V vertex : graph.getVertices()) {
      if (graph.inDegree(vertex) > 0 || graph.outDegree(vertex) > 0) {
        graphTemp.addVertex(vertex);
      }
    }
    for (E edge : graph.getEdges()) {
      if (graphTemp.containsVertex(graph.getSource(edge))
          && graphTemp.containsVertex(graph.getDest(edge))) {
        graphTemp.addEdge(edge, graph.getSource(edge), graph.getDest(edge));
      }
    }
    this.graph = graphTemp;
    layout = new KKLayout<V, E>(this.graph);
    layout.setSize(new Dimension(1000, 800)); // sets the initial size of the
                                              // space
    // The BasicVisualizationServer<V,E> is parameterized by the edge types
    BasicVisualizationServer<V, E> server = new BasicVisualizationServer<V, E>(
        layout);
    server.setPreferredSize(new Dimension(1000, 800));
    this.add(server);

  }

  private static String backslash = "\\";

  private static String pointToString(Point2D point) {
    return "(" + point.getX() + "pt," + point.getY() + "pt)";
  }

  private String nodeString(V node, int index, String shape, String color,
      String text, Point2D location, boolean printName) {
    return backslash + "node[draw, " + shape + "," + "fill = " + color + "] ("
        + index + ") at " + pointToString(location) + "{"
        + (printName ? node.toString() : "") + "};";
  }

  private String printEdge(E edge, BiMap<V, Integer> nodeIndex,
      Set<E> matchedEdges) {
    V source = graph.getSource(edge);
    V dest = graph.getDest(edge);
    String bend = "";
    String thickness = "";
    if (matchedEdges.contains(edge)) {
      thickness = "line width = .08em, ";
    } else {
      thickness = "line width = .05em, loosely dotted, ";
    }
    if (graph.findEdge(dest, source) != null) {
      bend = "bend right,";
    }
    return backslash + "path[" + thickness + "-latex]  ("
        + nodeIndex.get(source).toString() + ") edge[" + bend + "] ("
        + nodeIndex.get(dest).toString() + ");";
  }

  // if full tex is true, will produce an image that will compile, otherwise,
  // will give a fragment
  public void printGraphManual(BufferedWriter writer, Set<E> matchedEdges,
      Set<V> chainRoots, Set<V> terminalNodes, boolean fullTex,
      boolean printNodeNames) {
    try {
      if (fullTex) {
        writer.write(backslash + "documentclass{standalone}");
        writer.newLine();
        writer.write(backslash + "usepackage{tikz}");
        writer.newLine();
        writer.write(backslash + "begin{document}");
        writer.newLine();
      }
      writer.write(backslash + "begin{tikzpicture}[yscale=-1]");
      writer.newLine();
      writer.write(backslash + "tikzstyle{every node}=[font=" + backslash
          + "tiny])");
      writer.newLine();
      String nodeShape = "circle";

      BiMap<V, Integer> nodeIndex = HashBiMap.create();
      {
        int i = 0;
        for (V vertex : graph.getVertices()) {
          if (graph.inDegree(vertex) > 0 || graph.outDegree(vertex) > 0) {
            String nodeColor;
            if (chainRoots.contains(vertex)) {
              nodeColor = "green!20";
            } else if (terminalNodes.contains(vertex)) {
              nodeColor = "red!20";
            } else {
              nodeColor = "blue!20";
            }
            nodeIndex.put(vertex, i++);
            writer.write(nodeString(vertex, nodeIndex.get(vertex), nodeShape,
                nodeColor, nodeIndex.get(vertex).toString(),
                layout.transform(vertex), printNodeNames));
            writer.newLine();
          }
        }
      }
      for (E edge : graph.getEdges()) {
        writer.write(printEdge(edge, nodeIndex, matchedEdges));
        writer.newLine();

      }
      writer.write(backslash + "end{tikzpicture}");
      writer.newLine();
      if (fullTex) {
        writer.write(backslash + "end{document}");
        writer.newLine();
      }
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  /*
   * public void printGraphJTikz(OutputStream tikzOut){ TikzGraphics2D
   * tikzGraphics = new TikzGraphics2D(tikzOut);
   * tikzGraphics.paintComponent(this);
   * 
   * //this.paint(tikzGraphics); }
   */

}
