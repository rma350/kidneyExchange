package exchangeGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;


import org.apache.commons.collections15.Predicate;

import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.EdgeCycle;
import graphUtil.GraphUtil;

public class CycleSeparator<V,E> extends APSPSeparator<V,E>{
	

	public static class CycleCut<E>{
		private EdgeCycle<E> cycle;
		private double weight;
		private double violated;
		public EdgeCycle<E> getCycle() {
			return cycle;
		}
		public double getWeight() {
			return weight;
		}
		public double getViolated() {
			return violated;
		}
		public CycleCut(EdgeCycle<E> cycle, double weight, double violated) {
			super();
			this.cycle = cycle;
			this.weight = weight;
			this.violated = violated;
		}
	}
	
	//Will only say a cycle is invalid in findConnectedComponentCuts if size of cycle >= this quantity
	private int minCycleEdgesToSeparate;
	
	public CycleSeparator(DirectedSparseMultigraph<V,E> graph, Set<V> chainRoots, final Map<E,Double> nonZeroEdgeWeights){
		this(graph,chainRoots,nonZeroEdgeWeights,0);
	}
	
	public CycleSeparator(DirectedSparseMultigraph<V,E> graph, Set<V> chainRoots, final Map<E,Double> nonZeroEdgeWeights, int minCycleEdgesToSeparate){
		super(graph,chainRoots,nonZeroEdgeWeights);
		this.minCycleEdgesToSeparate = minCycleEdgesToSeparate;
	}
	
	public List<CycleCut<E>> findConnectedComponentCuts(double minimumViolation){
		EdgePredicateFilter<V,E> filter = new EdgePredicateFilter<V,E>(new Predicate<E>(){
			@Override
			public boolean evaluate(E edge) {
				if(nonZeroEdgeWeights.containsKey(edge)){
					return Math.abs(1-nonZeroEdgeWeights.get(edge)) < CplexUtil.epsilon;
				}
				return false;				
			}});
		DirectedSparseMultigraph<V,E> integerOnlyGraph = (DirectedSparseMultigraph<V,E>)filter.transform(graph);
		WeakComponentClusterer<V,E> clusterer = new WeakComponentClusterer<V,E>();
		Set<Set<V>> connectedComponents = clusterer.transform(integerOnlyGraph);
		List<CycleCut<E>> ans = new ArrayList<CycleCut<E>>();
		for(Set<V> component: connectedComponents){
			if(GraphUtil.testSetIsCycle(component,integerOnlyGraph) && component.size() >= this.minCycleEdgesToSeparate){
				EdgeCycle<E> cycle = GraphUtil.makeCycle(component, integerOnlyGraph);
				ans.add(new CycleCut<E>(cycle,cycle.size(),1));
			}
		}
		return ans;		
	}
	
	public Set<CycleCut<E>> findCycleCuts(double minimumViolation, Optional<FixedThreadPool> threadPool){
		Set<CycleCut<E>> ans = new HashSet<CycleCut<E>>();
		WeakComponentClusterer<V,E> clusterer = new WeakComponentClusterer<V,E>();
		Set<Set<V>> connectedComponents = clusterer.transform(this.subgraphNonZeroEdgeWeights());
		for(Set<V> component: connectedComponents){
			Map<EdgeCycle<E>,Double> cycleWeights = violatedCyclesInComponent(component,minimumViolation,threadPool); 
			for(EdgeCycle<E> cycle: cycleWeights.keySet() ){
				double weight = cycleWeights.get(cycle);
				double violation = weight - (cycle.size() - 1);
				ans.add(new CycleCut<E>(cycle,weight,violation));
			}			
		}
		return ans;
	}
	


}
