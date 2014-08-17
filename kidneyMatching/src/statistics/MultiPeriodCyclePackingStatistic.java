package statistics;

import multiPeriod.MultiPeriodCyclePacking;

public class MultiPeriodCyclePackingStatistic<V,E,T extends Comparable<T>> extends DynamicCycleChainPackingStatistic<V,E,T> {
	
	
	public MultiPeriodCyclePackingStatistic(
			MultiPeriodCyclePacking<V, E, T> multiPeriodPacking) {
		super(multiPeriodPacking);
		
	}
	
	public int getNumNodesWithDonor(){
		return this.getMultiPeriodPacking().getInputs().getGraph().getVertexCount() - this.getMultiPeriodPacking().getInputs().getTerminalNodes().size();
	}
	
	public int getNumNodesWithReceiver(){
		return this.getMultiPeriodPacking().getInputs().getGraph().getVertexCount() - this.getMultiPeriodPacking().getInputs().getRootNodes().size();
	}

}
