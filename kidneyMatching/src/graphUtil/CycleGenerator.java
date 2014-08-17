package graphUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import threading.FixedThreadPool;




import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;






public class CycleGenerator {
	
	private CycleGenerator(){}
	
	public static <V,E> List<List<E>> allChainPaths(DirectedSparseMultigraph<V,E> graph, V root, int maxChainLengthEdges){
		List<List<E>> ans = new ArrayList<List<E>>();
		List<E> currentList = new ArrayList<E>();
		Set<V> visited = new HashSet<V>();
		visited.add(root);
		allChainPaths(graph,ans,currentList,visited,root,maxChainLengthEdges);
		return ans;
	}
	/**
	 * 
	 * @param graph
	 * @param solutions
	 * @param appendTo
	 * @param remainingDepth 
	 */
	public static <V, E> void allChainPaths(DirectedSparseMultigraph<V,E> graph, List<List<E>> solutions,
			List<E> appendTo, Set<V> visited, V currentTail, int remainingNumNodes){
		if(remainingNumNodes > 0){
			
			for(E edge : graph.getOutEdges(currentTail)){
				V target = graph.getDest(edge);
				if(!visited.contains(target)){
					List<E> newList = new ArrayList<E>(appendTo);
					newList.add(edge);
					solutions.add(newList);
					visited.add(target);
					allChainPaths(graph, solutions,newList,visited,target,remainingNumNodes-1);
					visited.remove(target);
				}			
			}
		}		
	}
	
	public static <V,E> List<EdgeCycle<E>> allCycles(Map<V,Integer> vertexToIndex, DirectedSparseMultigraph<V,E> graph, V root, int length){
		
		List<EdgeCycle<E>> ans = new ArrayList<EdgeCycle<E>>();
		if(length > 1){
			List<E> appendTo = new ArrayList<E>();
			Set<V> visited = new HashSet<V>();
			visited.add(root);
			
			allCycles(vertexToIndex,graph,ans,appendTo,visited,root,root,length);
		}
		return ans;
	}
	
	/**
	 * 
	 * @param graph
	 * @param solutions
	 * @param appendTo
	 * @param remainingDepth Assumes that remaining depth >= 1
	 */
	public static <V,E> void allCycles(Map<V,Integer> vertexToIndex, DirectedSparseMultigraph<V,E> graph, 
			List<EdgeCycle<E>> solutions, List<E> appendTo, Set<V> visited,V head, V currentTail, int remainingDepth){
		for(E edge : graph.getOutEdges(currentTail)){			
			V target = graph.getDest(edge);
			if(target == head){
				List<E> newList = new ArrayList<E>(appendTo);
				newList.add(edge);
				/*if(appendTo.size() == 0){
					System.err.println("begin: " + head.toString());
					System.err.println("base: " + currentTail.toString());
					System.err.println("target: " +target.toString());
					System.err.println("outgoing of base: " + graph.outgoingEdgesOf(currentTail));
					throw new RuntimeException();
				}*/
				solutions.add(new EdgeCycle<E>(newList));
			}
			else if(remainingDepth > 1 &&
					vertexToIndex.get(target).intValue() > vertexToIndex.get(head).intValue()
					&& !visited.contains(target)){
				List<E> newList = new ArrayList<E>(appendTo);
				newList.add(edge);
				visited.add(target);
				allCycles(vertexToIndex,graph,solutions,newList,visited,head,target,remainingDepth-1);
				visited.remove(target);
			}
		}
		return;
	}
	
	private static class CycleJob<V,E> implements Callable<List<EdgeCycle<E>>>{

		private int lo;
		private int hi;
		private DirectedSparseMultigraph<V,E> graph;
		private Map<V,Integer> vertexToIndex;
		private List<V> vertices;
		private int cycleMaxLength;
		
		public CycleJob(int lo, int hi, DirectedSparseMultigraph<V,E> graph, List<V> vertices, int cycleMaxLength){
			this.lo = lo;
			this.hi = hi;
			this.graph = graph;
			this.vertices = vertices;
			this.cycleMaxLength = cycleMaxLength;
			vertexToIndex = new HashMap<V,Integer>();
			{
				int i = 0;
				for(V vertex: graph.getVertices() ){
					vertexToIndex.put(vertex, Integer.valueOf(i++));
				}
			}
			
		}
		@Override
		public List<EdgeCycle<E>> call() throws Exception {
			List<EdgeCycle<E>> ans = new ArrayList<EdgeCycle<E>>();
			for(int j = lo; j < hi; j++){
				V vertex = vertices.get(j);
				ans.addAll(CycleGenerator.allCycles(vertexToIndex,graph, vertex, cycleMaxLength));
			}
			return ans;
		}		
	}
	
	public static <V,E> List<EdgeCycle<E>> generateAllCycles(Optional<FixedThreadPool> threadPool, 
			DirectedSparseMultigraph<V,E> graph, int cycleMaxLength,List<V> nonRootNonTerminalVertices){
		if(threadPool.isPresent()){
			return generateAllCycles(threadPool.get().getExec(),threadPool.get().getNumThreads(),
					graph,cycleMaxLength,nonRootNonTerminalVertices);
		}
		else{
			List<EdgeCycle<E>> ans = new ArrayList<EdgeCycle<E>>();
			Map<V,Integer> vertexToIndex = new HashMap<V,Integer>();
			{
				int i = 0;
				for(V vertex: graph.getVertices() ){
					vertexToIndex.put(vertex, Integer.valueOf(i++));
				}
			}
			for(V vertex: nonRootNonTerminalVertices){
				ans.addAll(CycleGenerator.allCycles(vertexToIndex,graph, vertex, cycleMaxLength));
			}
			return ans;
		}
	}
	
	private static <V,E> List<EdgeCycle<E>> generateAllCycles(ExecutorService exec, int numThreads, 
			DirectedSparseMultigraph<V,E> graph, int cycleMaxLength,List<V> nonRootNonTerminalVertices){
		List<EdgeCycle<E>> cycles = new ArrayList<EdgeCycle<E>>();
		int numStarts = nonRootNonTerminalVertices.size();
			List<Callable<List<EdgeCycle<E>>>> jobs = new ArrayList<Callable<List<EdgeCycle<E>>>>();
			if(numStarts < numThreads){
				for(int i = 0; i <numStarts; i++){
					jobs.add(new CycleJob<V,E>(i,i+1,graph, nonRootNonTerminalVertices, cycleMaxLength));
				}
			}
			else{
				
				for(int i = 0; i < numThreads; i++){
					jobs.add(new CycleJob<V,E>(numStarts*i/numThreads,numStarts*(i+1)/numThreads,graph,
							nonRootNonTerminalVertices, cycleMaxLength ));
				}
			}
			try {
				List<Future<List<EdgeCycle<E>>>> results = exec.invokeAll(jobs);
				for(Future<List<EdgeCycle<E>>> result : results){
					try {
						cycles.addAll(result.get());
					} catch (ExecutionException e) {
						throw new RuntimeException(e);
					}
				}
			} catch (InterruptedException e1) {
				throw new RuntimeException(e1);
			}
		return cycles;
	}
	
	private static class ChainJob<V,E> implements Callable<List<List<E>>>{
		
		private int lo;
		private int hi;
		private DirectedSparseMultigraph<V,E> graph;
		private int chainMaxLengthInEdges;
		private List<V> chainRoots;
		
		public ChainJob(int lo, int hi,DirectedSparseMultigraph<V,E> graph, List<V> chainRoots, int chainMaxLengthInEdges ){
			this.lo = lo;
			this.hi = hi;
			this.graph = graph;
			this.chainMaxLengthInEdges = chainMaxLengthInEdges;
			this.chainRoots = chainRoots;
		}
		
		@Override
		public List<List<E>> call() throws Exception {
			List<List<E>> ans = new ArrayList<List<E>>();
			for(int j = lo; j < hi; j++){
				V vertex = chainRoots.get(j);
				ans.addAll(CycleGenerator.allChainPaths(graph, vertex, chainMaxLengthInEdges));
			}
			return ans;
		}
	}
	
	public static <V,E> List<List<E>> generateAllChains(Optional<FixedThreadPool> threadPool,
			DirectedSparseMultigraph<V,E> graph,
			int chainMaxLengthInEdges, List<V> chainRoots){
		if(threadPool.isPresent()){
			return generateAllChains(threadPool.get().getExec(),threadPool.get().getNumThreads(),
					graph,chainMaxLengthInEdges,chainRoots);
		}
		else{
			List<List<E>> ans = new ArrayList<List<E>>();
			for(V vertex: chainRoots){
				ans.addAll(CycleGenerator.allChainPaths(graph, vertex, chainMaxLengthInEdges));
			}
			return ans;
		}
	}
	
	private static <V,E> List<List<E>> generateAllChains(ExecutorService exec, int numThreads,
			DirectedSparseMultigraph<V,E> graph,
			int chainMaxLengthInEdges, List<V> chainRoots ){
		List<List<E>> chains = new ArrayList<List<E>>();
		{
			List<Callable<List<List<E>>>> jobs = new ArrayList<Callable<List<List<E>>>>();
			if(chainRoots.size() < numThreads){
				for(int i = 0; i < chainRoots.size(); i++){
					jobs.add(new ChainJob<V,E>(i,i+1,graph,chainRoots, chainMaxLengthInEdges));
				}
			}
			else{
				int numChainNodes = chainRoots.size();
				for(int i = 0; i < numThreads; i++){
					jobs.add(new ChainJob<V,E>(numChainNodes*i/numThreads,numChainNodes*(i+1)/numThreads,graph,chainRoots, chainMaxLengthInEdges ));
				}
			}
			try {
				List<Future<List<List<E>>>> results = exec.invokeAll(jobs);
				for(Future<List<List<E>>> result : results){
					try {
						chains.addAll(result.get());
					} catch (ExecutionException e) {
						throw new RuntimeException(e);
					}
				}
			} catch (InterruptedException e1) {
				throw new RuntimeException(e1);
			}
		}
		
		return chains;
	}
	
	
	
	
	/*
	public static <V extends HasId,E> void allFixedLengthUndirectedCycles(DefaultDirectedWeightedGraph<V,E> graph, 
			 NodeMap<V> solution, List<V> appendTo, int remainingDepth){
		V begin = appendTo.get(0);
		V base = appendTo.get(appendTo.size()-1);
		for(E edge : graph.edgesOf(base)){
			V target;
			if(graph.getEdgeSource(edge).equals(base)){
				target= graph.getEdgeTarget(edge);
			}
			else{
				target= graph.getEdgeSource(edge);
			}
			if(remainingDepth == 1 && target.getId() == begin.getId()){
				//List<V> newList = new ArrayList<V>(appendTo);
				//newList.add(target);
				solution.addTriple(appendTo);
			}
			else if(remainingDepth > 1 && target.getId() > begin.getId()  && !appendTo.contains(target)){
				List<V> newList = new ArrayList<V>(appendTo);
				newList.add(target);
				allFixedLengthUndirectedCycles(graph,solution,newList,remainingDepth-1);
			}
		}
		return;
	}
	*/

}
