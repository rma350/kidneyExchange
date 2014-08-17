package allPairsShortestPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.commons.collections15.Transformer;

import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.util.Pair;

public class ParallelFloydWarshallJung<V,E> {
	
	private ParallelFloydWarshall floydWarshall;
	private DirectedGraph<V,E> graph;
	private Transformer<? super E,? extends Number> edgeWeights;
	private ImmutableBiMap<Pair<V>,E> edgesUsed;
	private ImmutableBiMap<V,Integer> nodeIndex;
	private Optional<FixedThreadPool> threadPool;
	
	public ParallelFloydWarshallJung(DirectedGraph<V,E> graph, Transformer<E,? extends Number> edgeWeights){
		this(graph,edgeWeights,Optional.<FixedThreadPool>absent());
	}
	
	public ParallelFloydWarshallJung(DirectedGraph<V,E> graph, Transformer<? super E,? extends Number> edgeWeights,
			Optional<FixedThreadPool> threadPool){
		this.threadPool = threadPool;
		this.graph = graph;
		this.edgeWeights = edgeWeights;
		int numNodes = graph.getVertexCount();
		double[][] costs = new double[numNodes][numNodes];
		Builder<Pair<V>,E> edgesUsedBuilder = ImmutableBiMap.builder();
		
		{
			Builder<V,Integer> nodeIndexBuilder = ImmutableBiMap.builder();
			int i = 0;
			for(V v: graph.getVertices()){
				nodeIndexBuilder.put(v,i++);
			}
			this.nodeIndex = nodeIndexBuilder.build();

		}

		for(V source: nodeIndex.keySet()){
			for(V sink : nodeIndex.keySet()){
				Collection<E> edges = graph.findEdgeSet(source, sink); 				
				double bestEdgeVal = Double.POSITIVE_INFINITY;
				E bestEdge = null;
				for(E edge: edges){
					double newEdgeVal = edgeWeights.transform(edge).doubleValue();
					if(newEdgeVal < bestEdgeVal){
						bestEdgeVal = newEdgeVal;
						bestEdge = edge;
					}
				}
				if(bestEdge != null){
					edgesUsedBuilder.put(new Pair<V>(source,sink),bestEdge);
					costs[nodeIndex.get(source)][nodeIndex.get(sink)] = bestEdgeVal;
				}
				else{
					costs[nodeIndex.get(source)][nodeIndex.get(sink)] = Double.POSITIVE_INFINITY;
				}
			}
		}
		this.edgesUsed = edgesUsedBuilder.build();
		this.floydWarshall = new ParallelFloydWarshall(numNodes,costs,threadPool);
	}
	
	public void solve(){
		this.floydWarshall.solve();
	}
	
	public double getShortestPathLength(V source, V sink){
		return this.floydWarshall.shorestPathLength(this.nodeIndex.get(source), this.nodeIndex.get(sink));
	}
	
	public List<E> getShortestPath(V source, V sink){
		List<Integer> nodes = this.floydWarshall.shortestPath(this.nodeIndex.get(source), this.nodeIndex.get(sink));
		if(nodes == null){
			return null;
		}
		else if(nodes.size() > 1){
			List<E> ans = new ArrayList<E>();
			for(int i = 1; i < nodes.size(); i++){
				ans.add(this.edgesUsed.get(new Pair<V>(this.nodeIndex.inverse().get(nodes.get(i-1)), this.nodeIndex.inverse().get(nodes.get(i)))));
			}
			return ans;
		}
		else{
			throw new RuntimeException();
		}
	}
	

}
