package statistics;


import multiPeriod.MultiPeriodCyclePacking;


import org.joda.time.ReadableInstant;

import data.ExchangeUnit;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class NodeStatistic<V,E,T extends Comparable<T>> extends DynamicCycleChainPackingStatistic<V,E,T> {
	
	private V vertex;

	public NodeStatistic(MultiPeriodCyclePacking<V, E, T> multiPeriodPacking, V vertex) {
		super(multiPeriodPacking);
		this.vertex = vertex;
	}
	
	public int inDegree(){
		return this.getMultiPeriodPacking().getInputs().getGraph().inDegree(vertex);
	}
	
	public int outDegree(){
		return this.getMultiPeriodPacking().getInputs().getGraph().outDegree(vertex);
	}
	
	public boolean receivedEdgeInMatching(){
		return this.getMultiPeriodPacking().getDynamicMatching().getNodeToReceiverMatching().containsKey(vertex);
	}
	
	public int outDegreeExcludeTerminalNodes(){
		DirectedSparseMultigraph<V,E> graph = this.getMultiPeriodPacking().getInputs().getGraph();
		int ans = 0;
		for(E e: graph.getOutEdges(vertex)){
			V target = graph.getDest(e);
			if(!this.getMultiPeriodPacking().getInputs().getTerminalNodes().contains(target)){
				ans++;
			}
		}
		return ans;
	}
	
	

}
