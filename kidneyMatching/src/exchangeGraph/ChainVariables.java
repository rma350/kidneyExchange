package exchangeGraph;

import graphUtil.CycleGenerator;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.collections15.Transformer;

import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class ChainVariables<V,E> {
	
	private ImmutableBiMap<List<E>,IloIntVar> chainVars;
	private DirectedSparseMultigraph<V,E> graph;
	
	
	public ChainVariables(DirectedSparseMultigraph<V,E> graph, List<V> chainRoots, int maxChainLength,
			Optional<FixedThreadPool> threadPool, IloCplex cplex) throws IloException{
		List<List<E>> allChains = CycleGenerator.generateAllChains(threadPool, 
				graph, maxChainLength,chainRoots);
		chainVars = CplexUtil.makeBinaryVariables(cplex, allChains);
		this.graph = graph;
	}
	
	public void addNodeUsage(BiMap<V,IloLinearIntExpr> nodeUsage) throws IloException{
		for(List<E> chain: chainVars.keySet()){
			if(chain.size()>0){
			IloIntVar chainVar = chainVars.get(chain);
			V start = graph.getSource(chain.get(0));
			nodeUsage.get(start).addTerm(chainVar, 1);
			for(E edge: chain){
				nodeUsage.get(graph.getDest(edge)).addTerm(chainVar, 1);
			}
			}
		}
	}


	public ImmutableBiMap<List<E>, IloIntVar> getChainVars() {
		return chainVars;
	}



}
