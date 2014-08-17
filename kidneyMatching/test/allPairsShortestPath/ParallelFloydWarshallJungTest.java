package allPairsShortestPath;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections15.Transformer;
import org.junit.Test;

import threading.FixedThreadPool;

import com.google.common.base.Optional;

import allPairsShortestPath.ParallelFloydWarshallJung;



import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class ParallelFloydWarshallJungTest {
	
	private enum Node{
		a,b,c,d
	}
	
	private enum Edge{
		ab1(Node.a,Node.b,1),
		ab2(Node.a,Node.b,2),
		bc(Node.b,Node.c,3),
		ac(Node.a,Node.c,5),
		aa(Node.a,Node.a,3),
		cc(Node.c,Node.c,5),
		dd(Node.d,Node.d,1);
		
		private Node source;
		private Node sink;
		private double weight;
		
		private Edge(Node source, Node sink, double weight){
			this.source = source;
			this.sink = sink;
			this.weight= weight;
		}
		
		
		public Node getSource() {
			return source;
		}


		public Node getSink() {
			return sink;
		}


		public double getWeight() {
			return weight;
		}


		public static final Transformer<Edge,Double> EDGE_WEIGHT = new Transformer<Edge,Double>(){

			@Override
			public Double transform(Edge arg0) {
				return arg0.weight;
			}
			
		};
	}
	
	public static DirectedSparseMultigraph<Node,Edge> makeGraph(){
		DirectedSparseMultigraph<Node,Edge> graph = new DirectedSparseMultigraph<Node,Edge>();
		for(Node node: Node.values()){
			graph.addVertex(node);
		}
		for(Edge edge: Edge.values()){
			graph.addEdge(edge, edge.getSource(), edge.getSink());
		}
		return graph;
	}
	
	private static double eps = .0000001;
	private static double inf = Double.POSITIVE_INFINITY;

	@Test
	public void test() {
		DirectedSparseMultigraph<Node,Edge> graph = makeGraph();
		int numThreads = 3;
		Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
		ParallelFloydWarshallJung<Node,Edge> apspParallel = new ParallelFloydWarshallJung<Node,Edge>(graph,Edge.EDGE_WEIGHT,threadPool);
		ParallelFloydWarshallJung<Node,Edge> apspSingle = new ParallelFloydWarshallJung<Node,Edge>(graph,Edge.EDGE_WEIGHT);
		List<ParallelFloydWarshallJung<Node,Edge>> apsps = new ArrayList<ParallelFloydWarshallJung<Node,Edge>>();
		apsps.add(apspParallel);
		apsps.add(apspSingle);
		for(ParallelFloydWarshallJung<Node,Edge> apsp: apsps){
		apsp.solve();
		assertEquals(3,apsp.getShortestPathLength(Node.a, Node.a),eps);
		assertEquals(Arrays.asList(Edge.aa),apsp.getShortestPath(Node.a, Node.a));
		assertEquals(1,apsp.getShortestPathLength(Node.a, Node.b),eps);
		assertEquals(Arrays.asList(Edge.ab1),apsp.getShortestPath(Node.a, Node.b));
		assertEquals(4,apsp.getShortestPathLength(Node.a, Node.c),eps);
		assertEquals(Arrays.asList(Edge.ab1,Edge.bc),apsp.getShortestPath(Node.a, Node.c));
		assertEquals(inf,apsp.getShortestPathLength(Node.a, Node.d),eps);
		assertNull(apsp.getShortestPath(Node.a, Node.d));
		
		assertEquals(inf,apsp.getShortestPathLength(Node.b, Node.a),eps);
		assertNull(apsp.getShortestPath(Node.b, Node.a));
		assertEquals(inf,apsp.getShortestPathLength(Node.b, Node.b),eps);
		assertNull(apsp.getShortestPath(Node.b, Node.b));
		assertEquals(3,apsp.getShortestPathLength(Node.b, Node.c),eps);
		assertEquals(Arrays.asList(Edge.bc),apsp.getShortestPath(Node.b, Node.c));
		assertEquals(inf,apsp.getShortestPathLength(Node.b, Node.d),eps);
		assertNull(apsp.getShortestPath(Node.b, Node.d));
		
		assertEquals(inf,apsp.getShortestPathLength(Node.c, Node.a),eps);
		assertNull(apsp.getShortestPath(Node.c, Node.a));
		assertEquals(inf,apsp.getShortestPathLength(Node.c, Node.b),eps);
		assertNull(apsp.getShortestPath(Node.c, Node.b));
		assertEquals(5,apsp.getShortestPathLength(Node.c, Node.c),eps);
		assertEquals(Arrays.asList(Edge.cc),apsp.getShortestPath(Node.c, Node.c));
		assertEquals(inf,apsp.getShortestPathLength(Node.c, Node.d),eps);
		assertNull(apsp.getShortestPath(Node.c, Node.d));
		
		assertEquals(inf,apsp.getShortestPathLength(Node.d, Node.a),eps);
		assertNull(apsp.getShortestPath(Node.d, Node.a));
		assertEquals(inf,apsp.getShortestPathLength(Node.d, Node.b),eps);
		assertNull(apsp.getShortestPath(Node.d, Node.b));
		assertEquals(inf,apsp.getShortestPathLength(Node.d, Node.c),eps);
		assertNull(apsp.getShortestPath(Node.d, Node.c));
		assertEquals(1,apsp.getShortestPathLength(Node.d, Node.d),eps);
		assertEquals(Arrays.asList(Edge.dd),apsp.getShortestPath(Node.d, Node.d));
		}
	}

}
