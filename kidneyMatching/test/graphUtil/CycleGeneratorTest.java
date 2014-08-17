package graphUtil;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;






import org.junit.Test;

import com.google.common.collect.Lists;



import graphUtil.CycleGenerator;
import graphUtil.Edge;
import graphUtil.EdgeCycle;


public class CycleGeneratorTest {

	@Test
	public void testLengthTwoCycles() {
		TestGraph g = new TestGraph();
		{
			EdgeCycle<Edge> cycle = new EdgeCycle<Edge>(Lists.newArrayList(g.e02,g.e20));
			List<EdgeCycle<Edge>> expectedN1 = Lists.<EdgeCycle<Edge>>newArrayList(cycle);
			List<EdgeCycle<Edge>> actualN1 = CycleGenerator.allCycles(g.nodeMap,g.graph, g.n0, 2);			
			assertEquals(expectedN1,actualN1);
			for(int i = 0; i < actualN1.size(); i++){
				EdgeCycle.validateEqual(expectedN1.get(i),actualN1.get(i));
			}
			
		}
		{
			List<EdgeCycle<Edge>> expectedN2 = Lists.newArrayList();			
			List<EdgeCycle<Edge>> actualN2 = CycleGenerator.allCycles(g.nodeMap,g.graph, g.n1, 2); 
			assertEquals(expectedN2, actualN2);
		}
		{
			List<EdgeCycle<Edge>> expectedN3 = Lists.newArrayList();
			expectedN3.add(new EdgeCycle<Edge>(Lists.newArrayList(g.e23,g.e32)));
			List<EdgeCycle<Edge>> actualN3 = CycleGenerator.allCycles(g.nodeMap,g.graph, g.n2, 2); 
			assertEquals(expectedN3,actualN3);
			for(int i = 0; i < actualN3.size(); i++){
				EdgeCycle.validateEqual(expectedN3.get(i),actualN3.get(i));
			}
		}
		{
			List<EdgeCycle<Edge>> expectedN4 = new ArrayList<EdgeCycle<Edge>>();			
			List<EdgeCycle<Edge>> actualN4 = CycleGenerator.allCycles(g.nodeMap,g.graph, g.n3, 2); 
			assertEquals(expectedN4,actualN4);
		}
		
		{
			List<EdgeCycle<Edge>> expectedN5 = new ArrayList<EdgeCycle<Edge>>();			
			List<EdgeCycle<Edge>> actualN5 = CycleGenerator.allCycles(g.nodeMap,g.graph, g.n4, 2); 
			assertEquals(expectedN5,actualN5);
		}
		
	}
	
	
	@Test
	public void testLength5Cycles() {
		TestGraph g = new TestGraph();
		{
			Set<EdgeCycle<Edge>> expectedN1 = new HashSet<EdgeCycle<Edge>>();
			expectedN1.add(new EdgeCycle<Edge>(Arrays.asList(new Edge[]{g.e02, g.e20})));
			expectedN1.add(new EdgeCycle<Edge>(Arrays.asList(new Edge[]{g.e01,g.e14,g.e40})));
			expectedN1.add(new EdgeCycle<Edge>(Arrays.asList(new Edge[]{g.e02,g.e23,g.e34,g.e40})));
			expectedN1.add(new EdgeCycle<Edge>(Arrays.asList(new Edge[]{g.e02,g.e21,g.e14,g.e40})));
			List<EdgeCycle<Edge>> actualN1List = CycleGenerator.allCycles(g.nodeMap,g.graph, g.n0, 5); 
			Set<EdgeCycle<Edge>> actualN1 = new HashSet<EdgeCycle<Edge>>(actualN1List);
			assertEquals(0,actualN1List.size() - actualN1.size());
			assertEquals(expectedN1,actualN1);
			for(EdgeCycle<Edge> ex: expectedN1){
				for(EdgeCycle<Edge> act: actualN1){
					if(ex.equals(act)){
						EdgeCycle.validateEqual(ex, act);
					}
				}
			}
		}
		
		{
			Set<EdgeCycle<Edge>> expectedN2 = new HashSet<EdgeCycle<Edge>>();			
			List<EdgeCycle<Edge>> actualN2List = CycleGenerator.allCycles(g.nodeMap,g.graph, g.n1, 2);
			Set<EdgeCycle<Edge>> actualN2 = new HashSet<EdgeCycle<Edge>>(actualN2List);
			assertEquals(0,actualN2List.size() - actualN2.size());
			assertEquals(expectedN2,actualN2);
		}
		{
			Set<EdgeCycle<Edge>> expectedN3 = new HashSet<EdgeCycle<Edge>>();
			expectedN3.add(new EdgeCycle<Edge>(Arrays.asList(new Edge[]{g.e23,g.e32})));
			List<EdgeCycle<Edge>> actualN3List = CycleGenerator.allCycles(g.nodeMap,g.graph, g.n2, 2);
			Set<EdgeCycle<Edge>> actualN3 = new HashSet<EdgeCycle<Edge>>(actualN3List);
			assertEquals(0,actualN3List.size() - actualN3.size());
			assertEquals(expectedN3,actualN3);
			EdgeCycle.validateEqual(expectedN3.iterator().next(),actualN3.iterator().next());
		}
		{
			Set<EdgeCycle<Edge>> expectedN4 = new HashSet<EdgeCycle<Edge>>();			
			List<EdgeCycle<Edge>> actualN4List = CycleGenerator.allCycles(g.nodeMap,g.graph, g.n3, 2);
			Set<EdgeCycle<Edge>> actualN4 = new HashSet<EdgeCycle<Edge>>(actualN4List);
			assertEquals(0,actualN4List.size() - actualN4.size());
			assertEquals(expectedN4,actualN4);
		}
		
		{
			Set<EdgeCycle<Edge>> expectedN5 = new HashSet<EdgeCycle<Edge>>();			
			List<EdgeCycle<Edge>> actualN5List = CycleGenerator.allCycles(g.nodeMap,g.graph, g.n4, 2);
			Set<EdgeCycle<Edge>> actualN5 = new HashSet<EdgeCycle<Edge>>(actualN5List);
			assertEquals(0,actualN5List.size() - actualN5.size());
			assertEquals(expectedN5,actualN5);
			
		}
		
		
	}
	
	@Test
	public void testLengthTwoChains(){
		TestGraph g = new TestGraph();
		
			Set<List<Edge>> expected = new HashSet<List<Edge>>();
			expected.add(Arrays.asList(new Edge[]{g.echainRoot0}));
			expected.add(Arrays.asList(new Edge[]{g.echainRoot0, g.e01}));
			expected.add(Arrays.asList(new Edge[]{g.echainRoot0, g.e02}));
			expected.add(Arrays.asList(new Edge[]{g.echainRoot3}));
			expected.add(Arrays.asList(new Edge[]{g.echainRoot3, g.e34}));
			expected.add(Arrays.asList(new Edge[]{g.echainRoot3, g.e32}));
			List<List<Edge>> actualList = CycleGenerator.allChainPaths(g.graph, g.chainRoot, 2); 
			Set<List<Edge>> actual = new HashSet<List<Edge>>(actualList);
			assertEquals(0,actualList.size() - actual.size());
			assertEquals(expected,actual);
		
	}
	
	@Test
	public void testLengthFourChains(){
		TestGraph g = new TestGraph();
		
			Set<List<Object>> expected = new HashSet<List<Object>>();
			expected.add(Arrays.asList(new Object[]{g.echainRoot0}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot0, g.e01}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot0, g.e01,g.e14}));
			
			expected.add(Arrays.asList(new Object[]{g.echainRoot0, g.e02}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot0, g.e02,g.e21}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot0, g.e02,g.e21,g.e14}));
			
			expected.add(Arrays.asList(new Object[]{g.echainRoot0, g.e02,g.e23}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot0, g.e02,g.e23,g.e34}));
			
			expected.add(Arrays.asList(new Object[]{g.echainRoot3}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot3, g.e34}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot3, g.e34,g.e40}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot3, g.e34,g.e40,g.e01}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot3, g.e34,g.e40,g.e02}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot3, g.e32}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot3, g.e32,g.e21}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot3, g.e32,g.e21,g.e14}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot3, g.e32,g.e20}));
			expected.add(Arrays.asList(new Object[]{g.echainRoot3, g.e32,g.e20,g.e01}));
			List<List<Edge>> actualList = CycleGenerator.allChainPaths(g.graph, g.chainRoot, 4); 
			Set<List<Edge>> actual = new HashSet<List<Edge>>(actualList);
			assertEquals(0,actualList.size() - actual.size());
			assertEquals(expected,actual);
		
	}
	
	


	
	

}
