package inputOutput.node;



import inputOutput.core.Attribute;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;

import multiPeriod.MultiPeriodCyclePacking.EffectiveNodeType;
import multiPeriod.TimeInstant;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;


public class NodeInputAttributes<V,E,T extends Comparable<T>> {
	
	private MultiPeriodCyclePackingInputs<V,E,T> problemInput;
	
	private final Attribute<V,Double> donorPowerPostPreferences;
	private final Attribute<V,Double> receiverPowerPostPreferences;
	private final Attribute<V,Double> pairMatchPowerPostPreferences;
	
	private final Attribute<V,TimeInstant<T>> timeNodeArrived;
	private final Attribute<V,EffectiveNodeType> effectiveNodeType;
	

	
	public NodeInputAttributes(final MultiPeriodCyclePackingInputs<V,E,T> problemInput){
		this.problemInput = problemInput;
		this.timeNodeArrived =  new Attribute<V,TimeInstant<T>>(){
			@Override
			public TimeInstant<T> apply(V node) {
				return problemInput.getNodeArrivalTimes().get(node);
			}			
		};
		this.donorPowerPostPreferences = new Attribute<V,Double>(){
			@Override
			public Double apply(V node) {
				ImmutableMap<V,Double> donorPower = problemInput.getAuxiliaryInputStatistics().getDonorPowerPostPreference(); 
				if(donorPower.containsKey(node)){
					return donorPower.get(node);
				}
				else{
					return null;
				}
			}
		};
		this.receiverPowerPostPreferences = new Attribute<V,Double>(){
			@Override
			public Double apply(V node) {
				ImmutableMap<V,Double> receiverPower = problemInput.getAuxiliaryInputStatistics().getReceiverPowerPostPreference(); 
				if(receiverPower.containsKey(node)){
					return receiverPower.get(node);
				}
				else{
					return null;
				}
			}
		};
		this.pairMatchPowerPostPreferences = new Attribute<V,Double>(){
			@Override
			public Double apply(V node) {
				Double donorP = donorPowerPostPreferences.apply(node);
				if(donorP != null){
					
					Double receiverP = receiverPowerPostPreferences.apply(node);
					if(receiverP != null){
						
						return donorP.doubleValue()*receiverP.doubleValue()*10000;
					}
				}
				return null;				
			}
		};
		this.effectiveNodeType = new Attribute<V,EffectiveNodeType>(){
			@Override
			public EffectiveNodeType apply(V input) {
				return problemInput.getEffectiveNodeType(input);
			}			
		};
	}
	
	public Attribute<V, EffectiveNodeType> getEffectiveNodeType() {
		return effectiveNodeType;
	}

	public Attribute<V,TimeInstant<T>> getTimeNodeArrived() {
		return timeNodeArrived;
	}
	public Attribute<V,Double> getDonorPowerPostPreferences() {
		return donorPowerPostPreferences;
	}


	public Attribute<V,Double> getReceiverPowerPostPreferences() {
		return receiverPowerPostPreferences;
	}


	public Attribute<V,Double> getPairMatchPowerPostPreferences() {
		return pairMatchPowerPostPreferences;
	}

	public MultiPeriodCyclePackingInputs<V, E, T> getProblemInput() {
		return problemInput;
	}
	
	
	
	

}
