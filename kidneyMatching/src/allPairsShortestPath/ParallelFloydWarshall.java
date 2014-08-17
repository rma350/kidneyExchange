package allPairsShortestPath;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import threading.FixedThreadPool;

import com.google.common.base.Optional;

public class ParallelFloydWarshall {
	
	private Optional<FixedThreadPool> threadPool;
	
	private double[] current;
	private double[] next;
	
	private int[] maxIndex;
	private int numNodes;
	private boolean solved;
	
	private int getIndex(int i, int j){
		return i*numNodes+j;
	}
	
	private int getI(int index){
		return index / numNodes;
	}
	
	private int getJ(int index){
		return index % numNodes;
	}
	
	public ParallelFloydWarshall(int numNodes, double[][] distances){
		this(numNodes,distances,Optional.<FixedThreadPool>absent());
	}
	
	public ParallelFloydWarshall(int numNodes, double[][] distances, Optional<FixedThreadPool> threadPool){
		this.numNodes = numNodes;
		this.current = new double[numNodes*numNodes];
		this.next = new double[numNodes*numNodes];
		this.maxIndex = new int[numNodes*numNodes];
		this.threadPool = threadPool;
		Arrays.fill(maxIndex, -1);
		for(int i = 0; i < numNodes; i++){
			for(int j = 0; j < numNodes; j++){
				current[getIndex(i,j)] = distances[i][j];
			}
		}
		this.solved = false;
	}
	
	
	private void updateMatrixSingleThreaded(int k){
		update(0,current.length,k);
	}
	
	private void updateMatrixMultiThreaded(int k){
		List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
		if(current.length < threadPool.get().getNumThreads()){
			for(int i = 0; i < current.length; i++){
				tasks.add(new FloydJob(i,i+1,k));
			}
		}
		else{
			for(int t = 0; t < threadPool.get().getNumThreads(); t++){
				int lo = t*current.length/threadPool.get().getNumThreads();
				int hi = (t+1)*current.length/threadPool.get().getNumThreads();
				tasks.add(new FloydJob(lo,hi,k));
			}
		}
		try {
			List<Future<Boolean>> results = threadPool.get().getExec().invokeAll(tasks);
			for(Future<Boolean> result : results){
				if(!result.get().booleanValue()){
					throw new RuntimeException();
				}
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void solve(){
		if(solved){
			throw new RuntimeException("Already solved");
		}
		for(int k = 0; k < numNodes; k++){
			if(this.threadPool.isPresent()){
				updateMatrixMultiThreaded(k);
			}
			else{
				updateMatrixSingleThreaded(k);
			}
			double[] temp = current;
			current = next;
			next = temp;			
		}
		next = null;
		solved = true;
	}
	
	/**
	 * 
	 * @param i must lie in in [0,numNodes)
	 * @param j must lie in in [0,numNodes)
	 * @return the length of the shortest directed path from node i to node j.  If i == j, gives the shortest directed cycle starting at node i (note that the graph may contain nodes with self loops).  Returns Double.POSITIVE_INFINITY if there is no path from i to j.   
	 */
	public double shorestPathLength(int i, int j){
		if(!solved){
			throw new RuntimeException("Must solve first");
		}
		return this.current[getIndex(i,j)];
	}
	/**
	 * Example: If the path from node 2 to node 5 is an edge from 2 to 3 and then an edge from 3 to 5, the return value will be Arrays.asList(Integer.valueOf(2),Integer.valueOf(3),Integer.valueOf(5));
	 * 
	 * @param i the start of the directed path
	 * @param j the end of the directed path
	 * @return The shortest path starting at node i and ending at node j, or null if no such path exists.  
	 */
	public List<Integer> shortestPath(int i, int j){		
		if(current[getIndex(i,j)] == Double.POSITIVE_INFINITY){
			return null;
		}
		else{
			List<Integer> ans = new ArrayList<Integer>();
			ans.add(Integer.valueOf(i));
			ArrayDeque<ToEvaluate> stack = new ArrayDeque<ToEvaluate>();
			stack.push(new ToEvaluate(i,j));
			while(!stack.isEmpty()){
				ToEvaluate eval = stack.pop();
				
				int index = getIndex(eval.getSource(),eval.getSink());
				if(stack.size()>100){
					System.err.println("big stack " + stack.size() + " source " + eval.getSource() 
							+ " sink " + eval.getSink() + " index " + index + " max index " + maxIndex[index]);
					
				}
				if(this.maxIndex[index] < 0){
					ans.add(Integer.valueOf(eval.getSink()));
				}
				else{
					stack.push(new ToEvaluate(this.maxIndex[index],eval.getSink()));
					stack.push(new ToEvaluate(eval.getSource(),this.maxIndex[index]));
				}
			}
			return ans;			
		}
	}
	
	private static class ToEvaluate{
		private int source;
		private int sink;
		public ToEvaluate(int source, int sink) {
			super();
			this.source = source;
			this.sink = sink;
		}
		public int getSource() {
			return source;
		}
		public int getSink() {
			return sink;
		}
		
	}
	

	
	private class FloydJob implements Callable<Boolean>{
		
		private final int lo;
		private final int hi;
		private final int k;
		
		public FloydJob(int lo, int hi, int k){
			this.lo = lo;
			this.hi = hi;
			this.k = k;
		}
	

		@Override
		public Boolean call() throws Exception {
			return update(this.lo,this.hi,this.k);
		}
	}
	
	private double tolerance = .00001;
	
	private boolean update(int lo, int hi, int k){
		for(int index = lo; index < hi; index++){
			int i = getI(index);
			int j = getJ(index);
			double alternatePathValue = current[getIndex(i,k)] + current[getIndex(k,j)];
			 
			if(alternatePathValue + tolerance < current[index]){
				next[index] = alternatePathValue;
				/*if(k == j){
					System.err.println("alt = " + alternatePathValue +  " = " + current[getIndex(i,k)] + " + " + current[getIndex(k,j)]);
					System.err.println("current best is " + current[getIndex(i,j)]);
					throw new RuntimeException();
				}*/
				maxIndex[index] = k;
			}
			else{
				next[index] = current[index];
			}
		}
		return true;
	}

}
