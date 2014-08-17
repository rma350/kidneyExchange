package statistics;

import java.util.HashMap;
import java.util.Map;

import multiPeriod.MultiPeriodCyclePacking;

import org.joda.time.ReadableInstant;

import replicator.DonorEdge;

import data.Donor;
import data.ExchangeUnit;
import data.Receiver;

public class ExchangeUnitResults {
	
	private MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> multiPeriodPacking;
	
	private Map<Donor,DonorStatistic> donorStats;
	private Map<Receiver,ReceiverStatistic> receiverStats;
	private MultiPeriodExchangeUnitStatistic multiPeriodExchangeUnitStatistic;
	
	
	public ExchangeUnitResults(
			MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> multiPeriodPacking) {
		this.multiPeriodPacking = multiPeriodPacking;
		this.multiPeriodExchangeUnitStatistic = new MultiPeriodExchangeUnitStatistic(multiPeriodPacking);
		donorStats = new HashMap<Donor,DonorStatistic>();
		receiverStats = new HashMap<Receiver,ReceiverStatistic>();
		for(ExchangeUnit unit: multiPeriodPacking.getInputs().getGraph().getVertices()){
			if(!this.multiPeriodPacking.getInputs().getRootNodes().contains(unit)){
				receiverStats.put(unit.getReceiver(), new ReceiverStatistic(multiPeriodPacking,
						unit, unit.getReceiver()) );
			}
			for(Donor donor: unit.getDonor()){
				donorStats.put(donor, new DonorStatistic(multiPeriodPacking,unit,donor));
			}
		}
	}


	public MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> getMultiPeriodPacking() {
		return multiPeriodPacking;
	}


	public Map<Donor, DonorStatistic> getDonorStats() {
		return donorStats;
	}


	public Map<Receiver, ReceiverStatistic> getReceiverStats() {
		return receiverStats;
	}


	public MultiPeriodExchangeUnitStatistic getMultiPeriodExchangeUnitStatistic() {
		return multiPeriodExchangeUnitStatistic;
	}
	
	

}
