package multiPeriod;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



import multiPeriod.DynamicMatching;
import multiPeriod.DynamicMatching.SimultaneousMatch;
import multiPeriod.MultiPeriodCyclePackingIpSolver;
import multiPeriod.TimeInstant;


import org.junit.Test;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;





public class MultiPeriodCyclePackingIpSolverTest {
	
}
/*
	
	private static class SingleChainTestInput{
		public DirectedSparseMultigraph<TestNode,DefaultWeightedEdge> graph;
		public TestNode chainRoot;
		public TestNode n1;
		public TestNode terminal;
		public TestNode n3;
		public TestNode n4;
		public TestNode n5;
		public TestNode n6;
		public TestNode n7;
		public TestNode n8;
		public TestNode n9;
		public TestNode n10;
		public TestNode n11;		
		public Map<TestNode,Integer> nodeMap;
		
		public Map<TestNode,TimeInstant<Integer>> nodeArrivalTimes;
		public Map<DefaultWeightedEdge,TimeInstant<Integer>> edgeArrivalTimes;
		
		public Set<TestNode> chainRoots;
		public Set<TestNode> terminals;

		
		public SingleChainTestInput(){
			chainRoots = new HashSet<TestNode>();
			terminals = new HashSet<TestNode>();
			graph = new DefaultDirectedWeightedGraphExt<TestNode,DefaultWeightedEdge>(DefaultWeightedEdge.class);
			chainRoot = new TestNode(0);
			chainRoots.add(chainRoot);
			n1 = new TestNode(1);
			terminal = new TestNode(2);
			terminals.add(terminal);
			n3 = new TestNode(3);
			n4 = new TestNode(4);
			n5 = new TestNode(5);
			n6 = new TestNode(6);
			n7 = new TestNode(7);
			n8 = new TestNode(8);
			n9 = new TestNode(9);
			n10 = new TestNode(10);
			n11 = new TestNode(11);
			
			graph.addVertex(chainRoot);
			graph.addVertex(n1);
			graph.addVertex(terminal);
			graph.addVertex(n3);
			graph.addVertex(n4);
			graph.addVertex(n5);
			graph.addVertex(n6);
			graph.addVertex(n7);
			graph.addVertex(n8);
			graph.addVertex(n9);
			graph.addVertex(n10);
			graph.addVertex(n11);
			
			graph.addEdge(chainRoot, n1);
			graph.addEdge(n1, terminal);
			
			graph.addEdge(chainRoot, n3);
			graph.addEdge(n3, n4);
			graph.addEdge(n4, n5);
			
			graph.addEdge(n1, n6);
			graph.addEdge(n6, n7);
			graph.addEdge(n7, n8);
			
			graph.addEdge(n9, n10);
			graph.addEdge(n10, n11);
			graph.addEdge(n11, n9);
			for(DefaultWeightedEdge e: graph.edgeSet()){
				graph.setEdgeWeight(e, 1);
			}
			
			
			
			nodeMap = new HashMap<TestNode,Integer>();
			
			
			nodeMap.put(chainRoot, 0);
			nodeMap.put(n1, 1);
			nodeMap.put(terminal, 2);
			nodeMap.put(n3, 3);
			nodeMap.put(n4, 4);
			nodeMap.put(n5, 5);
			nodeMap.put(n6, 6);
			nodeMap.put(n7, 7);
			nodeMap.put(n8, 8);
			nodeMap.put(n9, 9);
			nodeMap.put(n10, 10);
			nodeMap.put(n11, 11);
			
			nodeArrivalTimes = new HashMap<TestNode,TimeInstant<Integer>>();
			nodeArrivalTimes.put(chainRoot, new TimeInstant<Integer>(0));
			nodeArrivalTimes.put(n1, new TimeInstant<Integer>(0));
			nodeArrivalTimes.put(terminal, new TimeInstant<Integer>(0));
			nodeArrivalTimes.put(n3, new TimeInstant<Integer>(1));
			nodeArrivalTimes.put(n4, new TimeInstant<Integer>(1));
			nodeArrivalTimes.put(n5, new TimeInstant<Integer>(1));
			nodeArrivalTimes.put(n6, new TimeInstant<Integer>(2));
			nodeArrivalTimes.put(n7, new TimeInstant<Integer>(2));
			nodeArrivalTimes.put(n8, new TimeInstant<Integer>(2));
			nodeArrivalTimes.put(n9, new TimeInstant<Integer>(1));
			nodeArrivalTimes.put(n10, new TimeInstant<Integer>(1));
			nodeArrivalTimes.put(n11, new TimeInstant<Integer>(1));
			
			edgeArrivalTimes = new HashMap<DefaultWeightedEdge,TimeInstant<Integer>>();
			for(DefaultWeightedEdge e: graph.edgeSet()){
				edgeArrivalTimes.put(e, nodeArrivalTimes.get(graph.getEdgeTarget(e)));
			}
			
			
			
		}
	}
	
	private static List<TimeInstant<Integer>> makeSchedule(int... times){
		List<TimeInstant<Integer>> ans = new ArrayList<TimeInstant<Integer>>();
		for(int time: times){
			ans.add(new TimeInstant<Integer>(time));
		}
		return ans;
	}

	@Test
	public void testOneShot() {
		SingleChainTestInput input = new SingleChainTestInput();
		//GraphPanel<TestNode,DefaultWeightedEdge> panel = new GraphPanel<TestNode,DefaultWeightedEdge>(input.graph);
				//panel.setVisible(true);
		
		CycleChainPackingFactory<TestNode,DefaultWeightedEdge> factory;
		{
			int chainMaxLength = Integer.MAX_VALUE;
			int cycleMaxLength = 3;
			int minChainsRemainOpen = 0;
			boolean displayOutput = false;
			Long maxTimeMs = null;
			int numThreads = 2;
			int maxCycleLengthToCreateVariables = cycleMaxLength;
			ExecutorService exec = Executors.newFixedThreadPool(numThreads);
			factory = new CycleChainPackingSubtourEliminationFactory<TestNode,DefaultWeightedEdge>(
					chainMaxLength,	cycleMaxLength,minChainsRemainOpen,
					displayOutput, maxTimeMs,exec,
					numThreads, maxCycleLengthToCreateVariables);
		}
		List<TimeInstant<Integer>> schedule = makeSchedule(2);
		*/
		
		/*MultiPeriodCyclePackingIpSolver<TestNode,DefaultWeightedEdge,Integer> solver =
				new MultiPeriodCyclePackingIpSolver<TestNode,DefaultWeightedEdge,Integer>(
						input,		schedule, factory);
		solver.computeAllMatchings();
		DynamicMatching<TestNode,DefaultWeightedEdge,Integer> matching = solver.getDynamicMatching();
		assertEquals(1,matching.getChainRootToChain().size());
		assertTrue(matching.getChainRootToChain().containsKey(input.chainRoot));
		assertEquals(1,matching.getChainTailToChain().size());
		assertTrue(matching.getChainTailToChain().containsKey(input.n8));
		assertEquals(1,matching.getCycles().size());
		Set<DefaultWeightedEdge> expectedCycle = new HashSet<DefaultWeightedEdge>();
		expectedCycle.add(input.graph.getEdge(input.n9, input.n10));
		expectedCycle.add(input.graph.getEdge(input.n10, input.n11));
		expectedCycle.add(input.graph.getEdge(input.n11, input.n9));
		SimultaneousMatch actualCycle = matching.getCycles().iterator().next();
		assertEquals(expectedCycle,new HashSet<TestNode>(actualCycle.getEdges()));
		
		factory.cleanUp();*/
		
		
		
	//}

//}
