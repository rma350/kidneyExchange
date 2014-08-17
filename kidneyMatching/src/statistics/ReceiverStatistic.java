package statistics;


import org.joda.time.ReadableInstant;

import replicator.DonorEdge;

import data.ExchangeUnit;
import data.ExchangeUnit.ExchangeUnitType;
import data.Person;
import data.Receiver;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import multiPeriod.DynamicMatching;
import multiPeriod.DynamicMatching.SimultaneousMatch;
import multiPeriod.MultiPeriodCyclePacking;

public class ReceiverStatistic extends PersonStatistic {
	
	private Receiver receiver;

	protected ReceiverStatistic(
			MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> multiPeriodPacking,
			ExchangeUnit exchangeUnit, Receiver receiver) {
		super(multiPeriodPacking, exchangeUnit, receiver);
		this.receiver = receiver;
	}

	@Override
	public boolean wasMatched() {
		return this.getMultiPeriodPacking().getDynamicMatching().getNodeToReceiverMatching().containsKey(this.getExchangeUnit());
	}

	@Override
	protected DynamicMatching<ExchangeUnit,DonorEdge,ReadableInstant>.SimultaneousMatch getMatching() {		
		return this.getMultiPeriodPacking().getDynamicMatching().getNodeToReceiverMatching().get(this.getExchangeUnit());
	}
	
	public boolean isChip(){
		return this.getExchangeUnit().getExchangeUnitType() == ExchangeUnitType.chip;
	}
	
	public int getNumDonorsCompatible(){
		DirectedSparseMultigraph<ExchangeUnit,DonorEdge> graph = this.getMultiPeriodPacking().getInputs().getGraph();
		return graph.inDegree(this.getExchangeUnit());
	}

}
