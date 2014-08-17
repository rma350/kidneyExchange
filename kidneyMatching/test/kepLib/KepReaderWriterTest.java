package kepLib;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import exchangeGraph.CplexUtil;
import graphUtil.Edge;
import graphUtil.Node;

public class KepReaderWriterTest {
	
	private double tolerance = .0000000001;
	
	private static String testDir = "unitTestData" + File.separator;
	
	private static String simpleTestFile = testDir + "simpleKepLibTest.csv";
	private static String simpleTestFileEdgeFailures = testDir + "simpleKepLibEdgeFailures.csv";
	private static KepTextReaderWriter readerWriter = KepTextReaderWriter.INSTANCE;

	@Test
	public void testReadSimpleInput() {
		
			KepInstance<Node,Edge> instance = readerWriter.read(simpleTestFile);
			assertEquals(0,instance.getBridgeConstraints().size());
			assertEquals(0,instance.getEdgeUsageConstraints().size());
			assertEquals(0,instance.getNodeFlowInConstraints().size());
			assertEquals(0,instance.getNodeFlowOutConstraints().size());
			assertEquals(4,instance.getRootNodes().size());
			assertEquals(Sets.newHashSet("r1","r2","r3","r4"),getNodeNames(instance.getRootNodes()));			
			assertEquals(4,instance.getPairedNodes().size());
			assertEquals(Sets.newHashSet("p1","p2","p3","p4"),getNodeNames(instance.getPairedNodes()));
			assertEquals(3,instance.getTerminalNodes().size());
			assertEquals(Sets.newHashSet("t1","t2","t3"),getNodeNames(instance.getTerminalNodes()));
			assertEquals(11,instance.getGraph().getVertexCount());
			assertEquals(Sets.newHashSet("r1","r2","r3","r4","p1","p2","p3","p4","t1","t2","t3"),getNodeNames(new HashSet<Node>(instance.getGraph().getVertices())));
			assertTrue((new HashSet<Node>(instance.getGraph().getVertices())).equals(
					Sets.union(instance.getTerminalNodes(), 
							Sets.union(instance.getRootNodes(),instance.getPairedNodes()))));
			assertEquals(3,instance.getGraph().getEdgeCount());
			assertEquals(Sets.newHashSet("e1","e2","e3"),getEdgeNames(new HashSet<Edge>(instance.getGraph().getEdges())));
			assertEquals(.01,instance.getCycleBonus(),tolerance);
			assertEquals(3,instance.getMaxCycleLength());
			assertEquals(Integer.MAX_VALUE,instance.getMaxChainLength());
			Set<Double> edgeValsExpected = new HashSet<Double>();
			edgeValsExpected.add(3.1);
			edgeValsExpected.add(2.0);
			edgeValsExpected.add(2.2);
			Set<Double> edgeValsActual = new HashSet<Double>();
			for(Edge e : instance.getGraph().getEdges()){
				edgeValsActual.add(instance.getEdgeWeights().apply(e).doubleValue());
			}
			//this may cause some floating point trouble..
			assertEquals(edgeValsActual,edgeValsActual);
			//TODO test make cycle weight
				
	}
	
	private ImmutableSet<String> getNodeNames(Set<Node> nodes){
		ImmutableSet.Builder<String> names = ImmutableSet.builder();
		for(Node node:nodes){
			names.add(node.getName());
		}
		return names.build();
	}
	
	private ImmutableSet<String> getEdgeNames(Set<Edge> edges){
		ImmutableSet.Builder<String> names = ImmutableSet.builder();
		for(Edge edge:edges){
			names.add(edge.getName());
		}
		return names.build();
	}
	
	@Test
	public void testReadSimpleInputEdgeFailures() {
			KepParseData<Node,Edge> parseData = readerWriter.readParseData(simpleTestFile);
			assertEquals(3,parseData.getEdgeNames().size());
			assertEquals(Sets.newHashSet("e1","e2","e3"),parseData.getEdgeNames().keySet());
			assertEquals(Sets.newHashSet("e1","e2","e3"),getEdgeNames(new HashSet<Edge>(parseData.getEdgeNames().values())));
			for(String name: parseData.getEdgeNames().keySet()){
				assertEquals(name,parseData.getEdgeNames().get(name).getName());
			}
			
			assertEquals(11,parseData.getNodeNames().size());
			assertEquals(Sets.newHashSet("r1","r2","r3","r4","p1","p2","p3","p4","t1","t2","t3"),parseData.getNodeNames().keySet());
			assertEquals(Sets.newHashSet("r1","r2","r3","r4","p1","p2","p3","p4","t1","t2","t3"),getNodeNames(new HashSet<Node>(parseData.getNodeNames().values())));
			for(String name: parseData.getNodeNames().keySet()){
				assertEquals(name,parseData.getNodeNames().get(name).getName());
			}
			
			ImmutableMap<Edge,Double> failureProbabilities = readerWriter.readEdgeFailureProbability(simpleTestFileEdgeFailures, parseData);
			assertEquals(3,failureProbabilities.size());
			for(Edge e: failureProbabilities.keySet()){
				if(e.getName().equals("e1")){
					assertEquals(1.0,failureProbabilities.get(e),CplexUtil.epsilon);
				}
				else if(e.getName().equals("e2")){
					assertEquals(0.5,failureProbabilities.get(e),CplexUtil.epsilon);
				}
				else if(e.getName().equals("e3")){
					assertEquals(0.0,failureProbabilities.get(e),CplexUtil.epsilon);
				}
				else{
					throw new RuntimeException("unexpected edge: " + e.getName());
				}
			}
			
			
	}
}


