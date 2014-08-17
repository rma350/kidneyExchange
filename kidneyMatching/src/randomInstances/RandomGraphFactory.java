package randomInstances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;




import org.apache.commons.math3.random.RandomDataImpl;


import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.Edge;
import graphUtil.Node;


public class RandomGraphFactory {
	
	private static RandomDataImpl sRandom =   new RandomDataImpl();
	
	public static <F> List<Set<F>> generateCrossMatchFailureScenarios(List<F> edges, int numScenarios, double p){
		return generateCrossMatchFailureScenarios(edges,numScenarios,p,sRandom);
	}
	public static <F> List<Set<F>> generateCrossMatchFailureScenarios(List<F> edges, int numScenarios, double p, RandomDataImpl rand){
		List<Set<F>> crossMatchFailureScenarios = new ArrayList<Set<F>>();
		for(int i = 0; i < numScenarios; i++){
			Set<F> crossMatchFailures = new HashSet<F>();
			int numFailures = rand.nextBinomial(edges.size(),p);
			if(numFailures> 0){
				Object[] failures = rand.nextSample(edges, numFailures);
				for(Object failure: failures){
					crossMatchFailures.add((F)failure);
				}
			}
			crossMatchFailureScenarios.add(crossMatchFailures);
		}
		return crossMatchFailureScenarios;
	}
	
	public static DirectedSparseMultigraph<Node,Edge> randomERGraph(int n, double p){
		return randomERGraph(n,p,sRandom);
	}
	
	public static List<Node> addRoots(DirectedSparseMultigraph<Node,Edge> graph, int numRoots, double p){
		return addRoots(graph,numRoots,p,sRandom);
	}
	
	public static List<Node> addRoots(DirectedSparseMultigraph<Node,Edge> graph, 
			int numRoots, double p, RandomDataImpl rand){
		List<Node> roots = new ArrayList<Node>();
		ArrayList<Node> oldNodes = new ArrayList<Node>(graph.getVertices());
		for(int i = 0; i < numRoots; i++){
			Node node = new Node(""+(i + oldNodes.size()));
			roots.add(node);
			graph.addVertex(node);
			int numNeighbors = rand.nextBinomial(oldNodes.size(),p);
			if(numNeighbors> 0){
			Object[] neighborNodes = rand.nextSample(oldNodes, numNeighbors);
			for(Object neighbor: neighborNodes){
				Node target = (Node)neighbor;
				graph.addEdge(new Edge(node.getName()+"-"+target.getName()),node, target);
				
			}
			}
		}
		return roots;
	}
	
	public static DirectedSparseMultigraph<Node,Edge> randomERGraph(int n, double p, RandomDataImpl rand){
		DirectedSparseMultigraph<Node,Edge> ans = new DirectedSparseMultigraph<Node,Edge>();
		Node[] nodes = new Node[n];
		for(int i =0; i < n; i++){
			nodes[i] = new Node(""+i);
			ans.addVertex(nodes[i]);
		}
		for(int i = 0; i < n; i++){
			int numNeighbors = rand.nextBinomial(n,p);
			if(numNeighbors> 0){
				Object[] neighborNodes = rand.nextSample(Arrays.asList(nodes), numNeighbors);
				for(Object neighborNodeO: neighborNodes){
					Node neighborNode = (Node)neighborNodeO;
					if(nodes[i] != neighborNode){
						ans.addEdge(new Edge(nodes[i].toString()+"-"+neighborNode.toString()),nodes[i],neighborNode);					
					}				
				}
			}
		}
		return ans;
	}

}
