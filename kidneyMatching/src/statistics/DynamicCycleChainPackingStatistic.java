package statistics;

import multiPeriod.MultiPeriodCyclePacking;

public abstract class DynamicCycleChainPackingStatistic<V,E,T extends Comparable<T>> {
	
	private MultiPeriodCyclePacking<V,E,T> multiPeriodPacking;
	
	
	protected DynamicCycleChainPackingStatistic(MultiPeriodCyclePacking<V,E,T> multiPeriodPacking){
		this.multiPeriodPacking = multiPeriodPacking;
	}
	
	protected MultiPeriodCyclePacking<V,E,T> getMultiPeriodPacking(){
		return this.multiPeriodPacking;
	}

}
