package exchangeGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;


import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.PredicateUtils;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.TransformerUtils;

import threading.FixedThreadPool;

import com.google.common.base.Optional;

import allPairsShortestPath.ParallelFloydWarshallJung;

import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.algorithms.filters.VertexPredicateFilter;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.EdgeCycle;
import graphUtil.GraphUtil;

public abstract class APSPSeparator<V,E> {
	
	protected DirectedSparseMultigraph<V,E> graph;
	protected Set<V> chainRoots;
	protected Map<E,Double> nonZeroEdgeWeights;
	private Predicate<E> edgeIsNonZero;
	private Transformer<E,Double> inverseEdgeWeights;
	
	
	
	public APSPSeparator(DirectedSparseMultigraph<V,E> graph, Set<V> chainRoots, final Map<E,Double> nonZeroEdgeWeights){
		this.graph = graph;
		this.chainRoots = chainRoots;
		this.nonZeroEdgeWeights = nonZeroEdgeWeights;
		this.edgeIsNonZero = new Predicate<E>(){
			@Override
			public boolean evaluate(E edge) {
				return nonZeroEdgeWeights.containsKey(edge) && nonZeroEdgeWeights.get(edge).doubleValue() > CplexUtil.epsilon;
			}};
		this.inverseEdgeWeights = inverseEdgeWeights();
		
	}
	
	protected DirectedSparseMultigraph<V,E> subgraphNonZeroEdgeWeights(){
		EdgePredicateFilter<V,E> filter = new EdgePredicateFilter<V,E>(edgeIsNonZero);
		return (DirectedSparseMultigraph<V,E>)filter.transform(graph);
	}
	
	private Transformer<E,Double> inverseEdgeWeights() {
		Map<E,Double> ans = new HashMap<E,Double>();
		for(E edge: this.nonZeroEdgeWeights.keySet()){
			if(nonZeroEdgeWeights.get(edge).doubleValue() > CplexUtil.epsilon){
				ans.put(edge, Math.max(1-nonZeroEdgeWeights.get(edge),0));
			}			
		}			
		return TransformerUtils.switchTransformer(edgeIsNonZero, 
				TransformerUtils.mapTransformer(ans), 
				TransformerUtils.constantTransformer(Double.valueOf(1.0)));
	}
	

	/**
	 * returns a map from cycles to their weight
	 * @param nodesInSubgraph
	 * @param minimumViolation
	 * @param exec
	 * @param numThreads
	 * @return
	 */
	protected Map<EdgeCycle<E>,Double> violatedCyclesInComponent(final Set<V> nodesInSubgraph, 
			double minimumViolation, Optional<FixedThreadPool> threadPool){
		Predicate<V> inSubgraph = new Predicate<V>(){
			@Override
			public boolean evaluate(V vertex) {
				return nodesInSubgraph.contains(vertex);
			}};
		VertexPredicateFilter<V,E> filter = new VertexPredicateFilter<V,E>(inSubgraph);
		DirectedSparseMultigraph<V,E> problemGraph = (DirectedSparseMultigraph<V,E>)filter.transform(graph);
		ParallelFloydWarshallJung<V,E> apsp = new ParallelFloydWarshallJung<V,E>(
				problemGraph,inverseEdgeWeights,threadPool);
		apsp.solve();
		Map<EdgeCycle<E>,Double> ans = new HashMap<EdgeCycle<E>,Double>();
		for(V vertex: problemGraph.getVertices()){
			double loopWeight = apsp.getShortestPathLength(vertex, vertex);
			if(loopWeight <= 1-CplexUtil.epsilon-minimumViolation){
				List<E> cycleInOrder = apsp.getShortestPath(vertex, vertex);
				EdgeCycle<E> cycle = new EdgeCycle<E>(cycleInOrder);
				ans.put(cycle,GraphUtil.doubleSumDefaultZero(this.nonZeroEdgeWeights, cycleInOrder));
			}
		}
		return ans;			
	}

}
