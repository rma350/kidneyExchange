package exchangeGraph;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import threading.FixedThreadPool;


import graphUtil.CycleGenerator;
import graphUtil.EdgeCycle;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class CycleVariables<V, E> extends IntVariableSet<EdgeCycle<E>>{
	
	
	private DirectedSparseMultigraph<V,E> graph;
	private ImmutableListMultimap<V,EdgeCycle<E>> nodeToCycles;
	private ImmutableListMultimap<E,EdgeCycle<E>> edgeToCycles;
	
	
	
	public CycleVariables(DirectedSparseMultigraph<V,E> graph, 
			List<EdgeCycle<E>> cycles, IloCplex cplex) throws IloException{
		super(new HashSet<EdgeCycle<E>>(cycles),cplex);
		if(super.size() != cycles.size()){
			throw new RuntimeException("cycles contained a duplicate");
		}
		this.graph = graph;
		
		ImmutableListMultimap.Builder<E,EdgeCycle<E>> edgeBuilder = ImmutableListMultimap.builder();
		ImmutableListMultimap.Builder<V,EdgeCycle<E>> nodeBuilder = ImmutableListMultimap.builder();
		for(EdgeCycle<E> cycle: cycles){
			for(E edge: cycle.getEdgesInOrder()){
				edgeBuilder.put(edge, cycle);
				nodeBuilder.put(graph.getSource(edge),cycle);
			}
		}
		nodeToCycles =nodeBuilder.build();
		edgeToCycles = edgeBuilder.build();
	}
	
	public CycleVariables(DirectedSparseMultigraph<V,E> graph, List<V> nonRootNonTerminalVertices, 
			int maxCycleLength, Optional<FixedThreadPool> threadPool, IloCplex cplex) throws IloException{
		this(graph,CycleGenerator.generateAllCycles(threadPool, 
				graph, maxCycleLength,nonRootNonTerminalVertices),cplex);
	}
	
	public ImmutableListMultimap<V,EdgeCycle<E>> getNodeToCycles(){
		return this.nodeToCycles;
	}
	
	public ImmutableListMultimap<E,EdgeCycle<E>> getEdgeToCycles(){
		return this.edgeToCycles;
	}
	
	public Set<EdgeCycle<E>> getCyclesTouchingAllVertices(Set<V> vertices){
		//TODO this is a little wasteful...
		Set<EdgeCycle<E>> cycles = new HashSet<EdgeCycle<E>>();
		for(V v : vertices){
			cycles.addAll(this.nodeToCycles.get(v));
		}
		return cycles;
	}
	
	public Set<EdgeCycle<E>> getCyclesContainingAllEdges(Set<E> edges){
		//TODO this is a little wasteful...
		Set<EdgeCycle<E>> cycles = new HashSet<EdgeCycle<E>>();
		for(E e : edges){
			cycles.addAll(this.edgeToCycles.get(e));
		}
		return cycles;
	}
	
	

	
	
	

}
