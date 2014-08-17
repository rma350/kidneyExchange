package inputOutput.node.exchangeUnit;

import inputOutput.Attributes.AttributeName;
import inputOutput.core.Attribute;
import inputOutput.core.JodaTimeDifference;
import inputOutput.node.NodeOutputAttributes;

import java.util.ArrayList;
import java.util.List;

import multiPeriod.DynamicMatching.SimultaneousMatch;
import multiPeriod.MultiPeriodCyclePacking;

import org.joda.time.Interval;
import org.joda.time.ReadableInstant;

import replicator.DonorEdge;
import data.Donor;
import data.ExchangeUnit;
import data.ExchangeUnit.ExchangeUnitType;
import data.Receiver;



public class ExchangeOutputAttributes{
	
	
	private final Attribute<ExchangeUnit,Donor> matchedDonor;
	private final Attribute<ExchangeUnit,Donor> matchedOrFirstDonor;
	private final MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> cyclePacking;
	private final ExchangeInputAttributes inputAttributes;

	
	
	public ExchangeOutputAttributes(MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> cyclePacking){
		this(cyclePacking,new ExchangeInputAttributes(cyclePacking.getInputs()));
	}

	public ExchangeOutputAttributes(final MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> cyclePacking,
			final ExchangeInputAttributes inputAttributes) {
		this.cyclePacking = cyclePacking;
		this.inputAttributes = inputAttributes;
		matchedDonor = new Attribute<ExchangeUnit,Donor>(){
		
			@Override
			public Donor apply(ExchangeUnit input) {
				if(cyclePacking.getDynamicMatching().getNodeToDonorMatching().containsKey(input)){
					List<DonorEdge> edgesInMatch = cyclePacking.getDynamicMatching().getNodeToDonorMatching().get(input).getEdges();
					//TODO this is pretty sloppy, eliminate quadratic run time
					for(DonorEdge edge: edgesInMatch){
						for(Donor donor: input.getDonor()){
							if(edge.getDonor() == donor){
								return donor;
							}	
						}
					}
					throw new RuntimeException("Did not find any donor in the simultaneous match but should have");
				}
				return null;
				
			}
			
		};
		matchedOrFirstDonor = new Attribute<ExchangeUnit,Donor>(){

			@Override
			public Donor apply(ExchangeUnit input) {
				if(input.getDonor().size() > 0){
					Donor matched = matchedDonor.apply(input);
					if(matched != null){
						return matched;
					}
					else{
						return input.getDonor().get(0);
					}
				}
				return null;
			}
			
		};
		
		
	}
	
	
	
	
	
	public Attribute<ExchangeUnit, Donor> getMatchedDonor() {
		return matchedDonor;
	}

	public Attribute<ExchangeUnit, Donor> getMatchedOrFirstDonor() {
		return matchedOrFirstDonor;
	}
	
	

	public <T> Attribute<ExchangeUnit,T> makeReceiverNodeAttribute(final Attribute<? super Receiver,T> receiverAttribute){
		return new Attribute<ExchangeUnit,T>(){
			@Override
			public T apply(ExchangeUnit person) {
				return receiverAttribute.apply(person.getReceiver());
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
	
	public <T> Attribute<ExchangeUnit,T> makeCompositeDonorNodeAttribute(final Attribute<? super Donor,T> donorAttribute, final Attribute<ExchangeUnit,Donor> makeDonor){
		return new Attribute<ExchangeUnit,T>(){
			@Override
			public T apply(ExchangeUnit input) {
				Donor donor = makeDonor.apply(input);
				return donor == null?  null : donorAttribute.apply(donor);
			}			
		};
	}

}
