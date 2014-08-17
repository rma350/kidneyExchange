package statistics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import multiPeriod.MultiPeriodCyclePacking;

public class Results<V,E,T extends Comparable<T>> {
	
	private MultiPeriodCyclePacking<V,E,T> multiPeriodPacking;
	private Map<V,NodeStatistic<V,E,T>> nodeStatistics;
	private Set<V> pairedNodes;
	
	
	public Results(MultiPeriodCyclePacking<V,E,T> multiPeriodPacking){
		this.multiPeriodPacking = multiPeriodPacking;
		this.nodeStatistics = new HashMap<V,NodeStatistic<V,E,T>>();
		this.pairedNodes = new HashSet<V>();
		for(V v: multiPeriodPacking.getInputs().getGraph().getVertices()){
			nodeStatistics.put(v,new NodeStatistic<V,E,T>(multiPeriodPacking,v));
			if((!multiPeriodPacking.getInputs().getRootNodes().contains(v)) 
					&& (!multiPeriodPacking.getInputs().getTerminalNodes().contains(v))){
				pairedNodes.add(v);
			}
		}
	}

	public MultiPeriodCyclePacking<V, E, T> getMultiPeriodPacking() {
		return multiPeriodPacking;
	}

	public Map<V, NodeStatistic<V, E, T>> getNodeStatistics() {
		return nodeStatistics;
	}

	public Set<V> getPairedNodes() {
		return pairedNodes;
	}
	
	
	
	

}
