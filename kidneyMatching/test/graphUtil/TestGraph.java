package graphUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;




import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.Edge;
import graphUtil.Node;

public class TestGraph {
	public DirectedSparseMultigraph<Node,Edge> graph;
	public Node n0 = new Node("0");
	public Node n1 = new Node("1");
	public Node n2 = new Node("2");
	public Node n3 = new Node("3");
	public Node n4 = new Node("4");
	public Node chainRoot = new Node("root");
	public Map<Node,Integer> nodeMap;
	public Edge e01 = new Edge("0-1");
	public Edge e02= new Edge("0-2");
	public Edge e14= new Edge("1-4");
	public Edge e20= new Edge("2-0");
	public Edge e21= new Edge("2-1");
	public Edge e23= new Edge("2-3");
	public Edge e32= new Edge("3-2");
	public Edge e34= new Edge("3-4");
	public Edge e40= new Edge("4-0");
	public Edge echainRoot0= new Edge("root-0");
	public Edge echainRoot3= new Edge("root-3");
	
	public ImmutableSet<Node> rootNodes;
	public ImmutableSet<Node> pairedNodes;
	public ImmutableSet<Node> terminalNodes;
	
	public TestGraph(){
		rootNodes = ImmutableSet.of(chainRoot);
		pairedNodes = ImmutableSet.of(n0, n1, n2, n3, n4);
		terminalNodes = ImmutableSet.of();
		graph =  new DirectedSparseMultigraph<Node,Edge>();
		nodeMap = new HashMap<Node,Integer>();
		nodeMap.put(n0, 0);
		nodeMap.put(n1, 1);
		nodeMap.put(n2, 2);
		nodeMap.put(n3, 3);
		nodeMap.put(n4, 4);
		nodeMap.put(chainRoot, 5);
		graph.addVertex(n0);
		graph.addVertex(n1);
		graph.addVertex(n2);
		graph.addVertex(n3);
		graph.addVertex(n4);
		graph.addVertex(chainRoot);
		graph.addEdge(e01,n0,n1);
		graph.addEdge(e02,n0,n2);
		graph.addEdge(e14,n1,n4);
		graph.addEdge(e20,n2,n0);
		graph.addEdge(e21,n2,n1);
		graph.addEdge(e23,n2,n3);
		graph.addEdge(e32,n3,n2);
		graph.addEdge(e34,n3,n4);
		graph.addEdge(e40,n4,n0);
		graph.addEdge(echainRoot0,chainRoot, n0);
		graph.addEdge(echainRoot3,chainRoot, n3);
	}
	
	
}
