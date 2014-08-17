package graphUtil;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;


import org.apache.commons.collections15.Predicate;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;



import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.CycleChainDecomposition;
import graphUtil.EdgeChain;
import graphUtil.EdgeCycle;
import graphUtil.GraphUtil;

public class GraphUtilTest {
	
	private static enum Node{
		a,b,c,d,e,f,g,h;
	}
	
	private static enum Edge{
		ab(Node.a,Node.b),ac(Node.a,Node.c),ad(Node.a,Node.d),
		ba(Node.b,Node.a),bc(Node.b,Node.c),bd(Node.b,Node.d),
		ca(Node.c,Node.a),cb(Node.c,Node.b),cd(Node.c,Node.d),
		da(Node.d,Node.a),db(Node.d,Node.b),dc(Node.d,Node.c),
		
		ef(Node.e,Node.f),eg(Node.e,Node.g),
		fg(Node.f,Node.g),fh(Node.f,Node.h),
		gf(Node.g,Node.f),gh(Node.g,Node.h),
		he(Node.h,Node.e),
		
		be(Node.b,Node.e),hc(Node.h,Node.c);
		
		
		
		
		private Edge(Node source,Node dest){
			this.source = source;
			this.dest = dest;
		}
		
		
		public Node getSource() {
			return source;
		}
		public Node getDest() {
			return dest;
		}


		private Node source;
		private Node dest;
	}
	
	private static DirectedSparseMultigraph<Node,Edge> makeTestGraph(){
		DirectedSparseMultigraph<Node,Edge> ans = new DirectedSparseMultigraph<Node,Edge>();
		for(Node node: Node.values()){
			ans.addVertex(node);
		}
		for(Edge edge: Edge.values()){
			ans.addEdge(edge, edge.getSource(), edge.getDest());
		}
		return ans;
	}

	@Test
	public void testAllInternalEdgesComplete() {
		DirectedSparseMultigraph<Node,Edge> graph = makeTestGraph();
		EnumSet<Node> subset = EnumSet.of(Node.a, Node.b,Node.c,Node.d);
		EnumSet<Edge> expected = EnumSet.of(Edge.ab,Edge.ac,Edge.ad,Edge.ba,Edge.bc,Edge.bd,Edge.ca,Edge.cb,Edge.cd,Edge.da,Edge.db,Edge.dc);
		assertEquals(expected,GraphUtil.allInternalEdges(graph, subset));
	}
	
	@Test
	public void testAllInternalEdgesSparse() {
		DirectedSparseMultigraph<Node,Edge> graph = makeTestGraph();
		EnumSet<Node> subset = EnumSet.of(Node.e, Node.f,Node.g,Node.h);
		EnumSet<Edge> expected = EnumSet.of(Edge.ef,Edge.eg,Edge.fg,Edge.fh,Edge.gf,Edge.gh,Edge.he);
		assertEquals(expected,GraphUtil.allInternalEdges(graph, subset));
	}
	
	@Test
	public void testAllInternalEdgesDisconnected() {
		DirectedSparseMultigraph<Node,Edge> graph = makeTestGraph();
		EnumSet<Node> subset = EnumSet.of(Node.a, Node.f);
		EnumSet<Edge> expected = EnumSet.noneOf(Edge.class);
		assertEquals(expected,GraphUtil.allInternalEdges(graph, subset));
	}
	
	@Test
	public void testAllInternalEdgesEmpty() {
		DirectedSparseMultigraph<Node,Edge> graph = makeTestGraph();
		EnumSet<Node> subset = EnumSet.noneOf(Node.class);
		EnumSet<Edge> expected = EnumSet.noneOf(Edge.class);
		assertEquals(expected,GraphUtil.allInternalEdges(graph, subset));
	}
	
	@Test
	public void testAllInternalEdgesFull() {
		DirectedSparseMultigraph<Node,Edge> graph = makeTestGraph();
		EnumSet<Node> subset = EnumSet.allOf(Node.class);
		EnumSet<Edge> expected = EnumSet.allOf(Edge.class);
		assertEquals(expected,GraphUtil.allInternalEdges(graph, subset));
	}
	
	
	@Test
	public void testAllContactedVerticesFull() {
		DirectedSparseMultigraph<Node,Edge> graph = makeTestGraph();		
		EnumSet<Edge> contacted = EnumSet.allOf(Edge.class);		
		EnumSet<Node> expected = EnumSet.allOf(Node.class);
		assertEquals(expected,GraphUtil.allContactedVertices(graph, contacted));
	}
	
	@Test
	public void testAllContactedVerticesPath() {
		DirectedSparseMultigraph<Node,Edge> graph = makeTestGraph();		
		EnumSet<Edge> contacted = EnumSet.of(Edge.ab,Edge.bc,Edge.cd,Edge.da);		
		EnumSet<Node> expected = EnumSet.of(Node.a,Node.b,Node.c,Node.d);
		assertEquals(expected,GraphUtil.allContactedVertices(graph, contacted));
	}
	
	//returns a graph with all the original vertices but only these edges
	private static DirectedSparseMultigraph<Node,Edge>  subgraph(DirectedSparseMultigraph<Node,Edge> graph,final EnumSet<Edge> edgeSet){
		EdgePredicateFilter<Node,Edge> filter = new EdgePredicateFilter<Node,Edge>(new Predicate<Edge>(){
			@Override
			public boolean evaluate(Edge edge) {
				return edgeSet.contains(edge);
			}});
		return (DirectedSparseMultigraph<Node,Edge>)filter.transform(graph);
	}
	
	@Test
	public void testTestSetIsCycle(){
		DirectedSparseMultigraph<Node,Edge> graph =  makeTestGraph();
		DirectedSparseMultigraph<Node,Edge> subgraph = subgraph(graph,EnumSet.of(Edge.ef,Edge.fh,Edge.he,Edge.ab,Edge.bc));
		EnumSet<Node> connectedComponentCycle = EnumSet.of(Node.e,Node.f,Node.h);
		assertTrue(GraphUtil.testSetIsCycle(connectedComponentCycle, subgraph));
		
		EnumSet<Node> connectedComponentChain = EnumSet.of(Node.a,Node.b,Node.c);
		assertFalse(GraphUtil.testSetIsCycle(connectedComponentChain, subgraph));
		
	}
	
	@Test
	public void testMakeCycleShort(){
		DirectedSparseMultigraph<Node,Edge> graph =  makeTestGraph();
		DirectedSparseMultigraph<Node,Edge> subgraph = subgraph(graph,EnumSet.of(Edge.ef,Edge.fh,Edge.he,Edge.ab,Edge.bc));
		EnumSet<Node> connectedComponentCycle = EnumSet.of(Node.e,Node.f,Node.h);
		assertTrue(GraphUtil.testSetIsCycle(connectedComponentCycle, subgraph));
		EdgeCycle<Edge> cycle = GraphUtil.makeCycle(connectedComponentCycle, subgraph);
		EdgeCycle<Edge> expected = new EdgeCycle<Edge>(Lists.newArrayList(Edge.ef,Edge.fh,Edge.he));
		assertEquals(expected,cycle);
		EdgeCycle.validateEqual(expected, cycle);		
	}
	
	@Test(expected=RuntimeException.class)
	public void testMakeCycleFail(){
		DirectedSparseMultigraph<Node,Edge> graph =  makeTestGraph();
		DirectedSparseMultigraph<Node,Edge> subgraph = subgraph(graph,EnumSet.of(Edge.ef,Edge.fh,Edge.he,Edge.ab,Edge.bc));
		EnumSet<Node> connectedComponentChain = EnumSet.of(Node.a,Node.b,Node.c);
		assertFalse(GraphUtil.testSetIsCycle(connectedComponentChain, subgraph));
		EdgeCycle<Edge> cycle = GraphUtil.makeCycle(connectedComponentChain, subgraph);
	}
	
	@Test
	public void testMakeCycleMedium(){
		DirectedSparseMultigraph<Node,Edge> graph =  makeTestGraph();
		DirectedSparseMultigraph<Node,Edge> subgraph = subgraph(graph,EnumSet.of(Edge.be,Edge.eg,Edge.gh,Edge.hc,Edge.cb));
		EnumSet<Node> connectedComponentCycle = EnumSet.of(Node.b,Node.e,Node.g,Node.h,Node.c);
		assertTrue(GraphUtil.testSetIsCycle(connectedComponentCycle, subgraph));
		EdgeCycle<Edge> cycle = GraphUtil.makeCycle(connectedComponentCycle, subgraph);
		EdgeCycle<Edge> expected = new EdgeCycle<Edge>(Lists.newArrayList(Edge.be,Edge.eg,Edge.gh,Edge.hc,Edge.cb));
		assertEquals(expected,cycle);
		EdgeCycle.validateEqual(expected, cycle);		
	}
	
	@Test
	public void testMakeCycleFull(){
		DirectedSparseMultigraph<Node,Edge> graph =  makeTestGraph();
		DirectedSparseMultigraph<Node,Edge> subgraph = subgraph(graph,EnumSet.of(Edge.ab,Edge.be,Edge.ef,Edge.fg,Edge.gh,Edge.hc,Edge.cd,Edge.da));
		EnumSet<Node> connectedComponentCycle = EnumSet.allOf(Node.class);
		assertTrue(GraphUtil.testSetIsCycle(connectedComponentCycle, subgraph));
		EdgeCycle<Edge> cycle = GraphUtil.makeCycle(connectedComponentCycle, subgraph);
		EdgeCycle<Edge> expected = new EdgeCycle<Edge>(Lists.newArrayList(Edge.ab,Edge.be,Edge.ef,Edge.fg,Edge.gh,Edge.hc,Edge.cd,Edge.da));
		assertEquals(expected,cycle);
		EdgeCycle.validateEqual(expected, cycle);		
	}
	@Test	
	public void testMakeChainShort(){
		DirectedSparseMultigraph<Node,Edge> graph =  makeTestGraph();
		DirectedSparseMultigraph<Node,Edge> subgraph = subgraph(graph,EnumSet.of(Edge.bd));
		EnumSet<Node> connectedComponentChain = EnumSet.of(Node.b,Node.d);
		assertFalse(GraphUtil.testSetIsCycle(connectedComponentChain, subgraph));
		EdgeChain<Edge> chain = GraphUtil.makeChain(Node.b,connectedComponentChain, subgraph);
		EdgeChain<Edge> expected = new EdgeChain<Edge>(Lists.newArrayList(Edge.bd));
		assertEquals(expected,chain);
	}
	
	@Test	
	public void testMakeChainMedium(){
		DirectedSparseMultigraph<Node,Edge> graph =  makeTestGraph();
		DirectedSparseMultigraph<Node,Edge> subgraph = subgraph(graph,EnumSet.of(Edge.ab,Edge.be,Edge.eg,Edge.gh));
		EnumSet<Node> connectedComponentChain = EnumSet.of(Node.a,Node.b,Node.e,Node.g,Node.h);
		assertFalse(GraphUtil.testSetIsCycle(connectedComponentChain, subgraph));
		EdgeChain<Edge> chain = GraphUtil.makeChain(Node.a,connectedComponentChain, subgraph);
		EdgeChain<Edge> expected = new EdgeChain<Edge>(Lists.newArrayList(Edge.ab,Edge.be,Edge.eg,Edge.gh));
		assertEquals(expected,chain);
	}
	
	@Test	
	public void testMakeChainLong(){
		DirectedSparseMultigraph<Node,Edge> graph =  makeTestGraph();
		DirectedSparseMultigraph<Node,Edge> subgraph = subgraph(graph,EnumSet.of(Edge.ab,Edge.be,Edge.ef,Edge.fg,Edge.gh,Edge.hc,Edge.cd));
		EnumSet<Node> connectedComponentChain = EnumSet.allOf(Node.class);
		assertFalse(GraphUtil.testSetIsCycle(connectedComponentChain, subgraph));
		EdgeChain<Edge> chain = GraphUtil.makeChain(Node.a,connectedComponentChain, subgraph);
		EdgeChain<Edge> expected = new EdgeChain<Edge>(Lists.newArrayList(Edge.ab,Edge.be,Edge.ef,Edge.fg,Edge.gh,Edge.hc,Edge.cd));
		assertEquals(expected,chain);
	}
	
	@Test
	public void testCycleChainDecompositionFull(){
		DirectedSparseMultigraph<Node,Edge> graph =  makeTestGraph();
		DirectedSparseMultigraph<Node,Edge> subgraph = subgraph(graph,EnumSet.of(Edge.ab,Edge.bc,Edge.cd,Edge.da, Edge.ef,Edge.fg,Edge.gh));
		CycleChainDecomposition<Node,Edge> decomp = new CycleChainDecomposition<Node,Edge>(subgraph);
		assertEquals(7,decomp.totalEdges());
		assertEquals(1,decomp.getEdgeChains().size());
		assertEquals(new EdgeChain<Edge>(Arrays.asList(Edge.ef,Edge.fg,Edge.gh)),decomp.getEdgeChains().get(0));
		assertEquals(1,decomp.getEdgeCycles().size());
		assertEquals(new EdgeCycle<Edge>(Arrays.asList(Edge.ab,Edge.bc,Edge.cd,Edge.da)),decomp.getEdgeCycles().get(0));		
	}
	
	@Test
	public void testCycleChainDecompositionPartial(){
		DirectedSparseMultigraph<Node,Edge> graph =  makeTestGraph();
		DirectedSparseMultigraph<Node,Edge> subgraph = subgraph(graph,EnumSet.of(Edge.ab,Edge.bc,Edge.ca, Edge.fg,Edge.he));
		CycleChainDecomposition<Node,Edge> decomp = new CycleChainDecomposition<Node,Edge>(subgraph);
		assertEquals(5,decomp.totalEdges());
		assertEquals(2,decomp.getEdgeChains().size());
		Set<EdgeChain<Edge>> expected = new HashSet<EdgeChain<Edge>>();
		expected.add(new EdgeChain<Edge>(Arrays.asList(Edge.fg)));
		expected.add(new EdgeChain<Edge>(Arrays.asList(Edge.he)));
		assertEquals(expected,new HashSet<EdgeChain<Edge>>(decomp.getEdgeChains()));
		assertEquals(1,decomp.getEdgeCycles().size());
		assertEquals(new EdgeCycle<Edge>(Arrays.asList(Edge.ab,Edge.bc,Edge.ca)),decomp.getEdgeCycles().get(0));		
	}
	
	@Test
	public void testCycleChainDecompositionByEdgeCycleList(){
		DirectedSparseMultigraph<Node,Edge> graph =  makeTestGraph();
		Set<Edge> edges = EnumSet.of(Edge.fh,Edge.he);
		Set<EdgeCycle<Edge>> edgeCycles = new HashSet<EdgeCycle<Edge>>();
		edgeCycles.add(new EdgeCycle<Edge>(Arrays.asList(Edge.cd,Edge.db,Edge.bc)));
		CycleChainDecomposition<Node,Edge> decomp = new CycleChainDecomposition<Node,Edge>(graph,edges,edgeCycles);
		assertEquals(5,decomp.totalEdges());
		assertEquals(1,decomp.getEdgeChains().size());
		assertEquals(new EdgeChain<Edge>(Arrays.asList(Edge.fh,Edge.he)),decomp.getEdgeChains().get(0));
		assertEquals(1,decomp.getEdgeCycles().size());
		assertEquals(new EdgeCycle<Edge>(Arrays.asList(Edge.cd,Edge.db,Edge.bc)),decomp.getEdgeCycles().get(0));		
	}
	
	

}
