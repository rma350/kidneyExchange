package statistics;
import java.util.List;
import java.util.Map;
import java.util.Set;


import data.ExchangeUnit;
import data.Person;
import multiPeriod.DynamicMatching;
import multiPeriod.DynamicMatching.SimultaneousMatch;
import multiPeriod.MultiPeriodCyclePacking;
import multiPeriod.TimeInstant;


import org.joda.time.Duration;
import org.joda.time.ReadableInstant;

import replicator.DonorEdge;


public abstract class PersonStatistic extends DynamicCycleChainPackingStatistic<ExchangeUnit,DonorEdge,ReadableInstant>{
	
	private Person person;
	private ExchangeUnit exchangeUnit;

	protected PersonStatistic(
			MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> multiPeriodPacking,
			ExchangeUnit exchangeUnit, Person person) {
		super(multiPeriodPacking);
		this.person = person;
		this.exchangeUnit = exchangeUnit;
	}

	
	
	public boolean arrivedBeforeSimulationStart(){
		return this.person.getRegistered().isBefore(this.getMultiPeriodPacking().getInputs().getStartTime().getValue());
	}
	
	public abstract boolean wasMatched();
	
	protected abstract DynamicMatching<ExchangeUnit,DonorEdge,ReadableInstant>.SimultaneousMatch getMatching();
	
	public Duration timeUntilMatched(){
		if(!wasMatched()){
			throw new RuntimeException();
		}
		else{
			return timeUntilMatchedChecked();
		}
		
	}
	
	private Duration timeUntilMatchedChecked(){
		return new Duration(person.getRegistered(),getMatching().getTimeMatched().getValue());
	}
	
	public Duration timeUntilMatchedIgnorePreSimulation(){
		if(!wasMatched()){
			throw new RuntimeException();
		}
		if(arrivedBeforeSimulationStart()){
			return new Duration(getMultiPeriodPacking().getInputs().getStartTime().getValue(),getMatching().getTimeMatched().getValue());
		}
		else{
			return timeUntilMatchedChecked();
		}
	}
	
	protected ExchangeUnit getExchangeUnit(){
		return this.exchangeUnit;
	}
	
	 

}
