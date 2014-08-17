package inputOutput.node.exchangeUnit;

import java.util.ArrayList;
import java.util.List;

import inputOutput.core.Attribute;
import inputOutput.node.NodeInputAttributes;

import multiPeriod.MultiPeriodCyclePacking;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;

import org.joda.time.Interval;
import org.joda.time.ReadableInstant;

import replicator.DonorEdge;

import data.Donor;
import data.ExchangeUnit;
import data.Receiver;
import data.ExchangeUnit.ExchangeUnitType;

public class ExchangeInputAttributes {
	
	private final MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> cyclePackingInputs;
	private final Attribute<ExchangeUnit,Receiver> receiver;
	private final Attribute<ExchangeUnit,List<Donor>> donors;
	private final Attribute<ExchangeUnit,Integer> numDonors;
	private final Attribute<ExchangeUnit,Boolean> isChip;
	private final Attribute<ExchangeUnit,ExchangeUnitType> exchangeUnitType;

	public ExchangeInputAttributes(
			MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> cyclePackingInputs) {
		this.cyclePackingInputs = cyclePackingInputs;
		
		this.receiver = new Attribute<ExchangeUnit,Receiver>(){
			@Override
			public Receiver apply(ExchangeUnit unit) {
				return unit.getReceiver();
			}			
		};
		
		donors = new Attribute<ExchangeUnit,List<Donor>>(){
			@Override
			public List<Donor> apply(ExchangeUnit person) {
				return person.getDonor();
			}
		};
		exchangeUnitType = new Attribute<ExchangeUnit,ExchangeUnitType>(){

			@Override
			public ExchangeUnitType apply(ExchangeUnit exchangeUnit) {
				return exchangeUnit.getExchangeUnitType();
			}
			
		};
		isChip = new Attribute<ExchangeUnit,Boolean>(){

			@Override
			public Boolean apply(ExchangeUnit exchangeUnit) {
				return exchangeUnit.getExchangeUnitType() == ExchangeUnitType.chip;
			}
			
		};
		numDonors = new Attribute<ExchangeUnit,Integer>(){
			@Override
			public Integer apply(ExchangeUnit unit) {
				return unit.getDonor().size();
			}			
		};
	}
	
	public Attribute<ExchangeUnit,Donor> getIthDonor(final int i){
		if(i < 0){
			throw new ArrayIndexOutOfBoundsException();
		}
		return new Attribute<ExchangeUnit,Donor>(){

			@Override
			public Donor apply(ExchangeUnit input) {
				
				return input.getDonor().size() >= i ? null : input.getDonor().get(i);
			}
			
		};
	}
	
	
	public Attribute<ExchangeUnit,Receiver> getReceiver() {
		return receiver;
	}



	public Attribute<ExchangeUnit,List<Donor>> getDonors() {
		return donors;
	}





	public Attribute<ExchangeUnit,Integer> getNumDonors() {
		return numDonors;
	}



	public Attribute<ExchangeUnit,Boolean> getIsChip() {
		return isChip;
	}



	public Attribute<ExchangeUnit,ExchangeUnitType> getExchangeUnitType() {
		return exchangeUnitType;
	}



	public <T> Attribute<ExchangeUnit,T> makeReceiverNodeAttribute(final Attribute<? super Receiver,T> receiverAttribute){
		return new Attribute<ExchangeUnit,T>(){
			@Override
			public T apply(ExchangeUnit person) {
				return receiverAttribute.apply(receiver.apply(person));
			}			
		};
	}
	
	public <T> Attribute<ExchangeUnit,T> makeDonorNodeAttribute(final Attribute<? super Donor,T> donorAttribute, final int donorIndex){
		return new Attribute<ExchangeUnit,T>(){
			@Override
			public T apply(ExchangeUnit person) {
				return donorAttribute.apply(person.getDonor().get(donorIndex));
			}			
		};
	}

}
