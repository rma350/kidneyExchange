package statistics;


import org.joda.time.ReadableInstant;

import replicator.DonorEdge;

import data.Donor;
import data.ExchangeUnit;
import data.ExchangeUnit.ExchangeUnitType;
import data.Person;
import multiPeriod.DynamicMatching;
import multiPeriod.DynamicMatching.SimultaneousMatch;
import multiPeriod.MultiPeriodCyclePacking;

public class DonorStatistic extends PersonStatistic{
	
	private Donor donor;

	protected DonorStatistic(
			MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> multiPeriodPacking,
			ExchangeUnit exchangeUnit, Donor donor) {
		super(multiPeriodPacking, exchangeUnit, donor);
		this.donor = donor;
	}

	@Override
	public boolean wasMatched() {
		return this.getMultiPeriodPacking().getDynamicMatching().getNodeToDonorMatching().containsKey(this.getExchangeUnit());
	}

	@Override
	protected DynamicMatching<ExchangeUnit,DonorEdge,ReadableInstant>.SimultaneousMatch getMatching() {
		return this.getMultiPeriodPacking().getDynamicMatching().getNodeToDonorMatching().get(this.getExchangeUnit());
	}
	
	public boolean isChainRoot(){
		return this.getMultiPeriodPacking().getInputs().getRootNodes().contains(this.getExchangeUnit());
	}
	
	public boolean isAltruistic(){
		return this.getExchangeUnit().getExchangeUnitType() == ExchangeUnitType.altruistic;
	}
	
	public int getNumReceiversCompatible(){
		throw new UnsupportedOperationException();
		//DirectedWeightedGraph<ExchangeUnit,DefaultWeightedEdge> graph = this.getMultiPeriodPacking().getGraph();
		//return graph.outDegreeOf(this.getExchangeUnit());
	}

}
