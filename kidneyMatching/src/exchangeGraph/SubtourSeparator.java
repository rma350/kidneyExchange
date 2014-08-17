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
import com.google.common.collect.Sets;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.EdgeCycle;
import graphUtil.GraphUtil;

public class SubtourSeparator<V,E> extends APSPSeparator<V,E>{
	
	private DirectedSparseMultigraph<V,E> subgraph;
	private Set<Set<V>> connectedComponents; 
	
	
	public SubtourSeparator(DirectedSparseMultigraph<V,E> graph, Set<V> chainRoots, final Map<E,Double> nonZeroEdgeWeights){
		super(graph,chainRoots,nonZeroEdgeWeights);
		this.subgraph = subgraphNonZeroEdgeWeights();
		WeakComponentClusterer<V,E> clusterer = new WeakComponentClusterer<V,E>(); 
		this.connectedComponents =  clusterer.transform(subgraph);
	}
	
	public static class Subtour<V,E>{
		private Set<V> nodes;
		private Set<E> internalEdges;
		private double totalWeight;
		private double violation;
		public Set<V> getNodes() {
			return nodes;
		}
		public Set<E> getInternalEdges() {
			return internalEdges;
		}
		public double getTotalWeight() {
			return totalWeight;
		}
		public double getViolation() {
			return violation;
		}
		public Subtour(Set<V> nodes, Set<E> internalEdges, double totalWeight, double violation) {
			super();
			this.nodes = nodes;
			this.internalEdges = internalEdges;
			this.totalWeight = totalWeight;
			this.violation = violation;
		}
		
		
	}
	
	public List<Subtour<V,E>> findConnectedComponentCuts(double minimumViolation){
		List<Subtour<V,E>> ans = new ArrayList<Subtour<V,E>>();
		for(Set<V> component: connectedComponents){
			if(Sets.intersection(this.chainRoots, component).isEmpty()){
				Set<E> internalEdges = GraphUtil.allInternalEdges(graph, component);
				double weight = 0;
				for(E edge: internalEdges){
					weight+= this.nonZeroEdgeWeights.containsKey(edge) ? this.nonZeroEdgeWeights.get(edge) : 0; 
				}
				double violation = weight - (component.size()-1);
				if(violation >= minimumViolation + CplexUtil.epsilon){
					ans.add(new Subtour<V,E>(component,internalEdges,weight,violation));
				}
			}
		}
		return ans;
	}
	
	public List<Subtour<V,E>> findCycleCuts(double minimumViolation, Optional<FixedThreadPool> threadPool){
		List<Subtour<V,E>> ans = Lists.newArrayList();
		for(Set<V> component: this.connectedComponents){
			Map<EdgeCycle<E>,Double> cycles = violatedCyclesInComponent(component,minimumViolation,threadPool);
			for(EdgeCycle<E> cycle: cycles.keySet()){
				Set<V> nodesInCycle = GraphUtil.allContactedVertices(graph,cycle.getEdges());
				Set<E> subtourEdges = GraphUtil.allInternalEdges(graph, nodesInCycle);
				double weight = cycles.get(cycle);
				double violation = weight - (nodesInCycle.size()-1);
				ans.add(new Subtour<V,E>(nodesInCycle,subtourEdges,weight,violation));
			}
		}
		return ans;		
	}
	
	
	

}
