package inputOutput.node.exchangeUnit;

import inputOutput.core.Attribute;


import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import kepModeler.ExchangeUnitAuxiliaryInputStatistics;

import org.joda.time.Period;
import org.joda.time.ReadableInstant;

import replicator.DonorEdge;
import data.ExchangeUnit;
import data.HlaType;
import data.Receiver;
import data.SpecialHla;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;

public class ReceiverInputAttributes {
	
	public ReceiverInputAttributes(ExchangeUnitAuxiliaryInputStatistics auxiliaryStatistics){
		this.auxiliaryStatistics = auxiliaryStatistics;
		minDonorAgeYr = new Attribute<Receiver,Period>(){
			@Override
			public Period apply(Receiver person) {
				return person.getMinDonorAgeYr();
			}
		};
		maxDonorAgeYr = new Attribute<Receiver,Period>(){
			@Override
			public Period apply(Receiver person) {
				return person.getMaxDonorAgeYr();
			}
		};
		minHlaScore = new Attribute<Receiver,Integer>(){
			@Override
			public Integer apply(Receiver person) {
				return Integer.valueOf(person.getMinHlaScore());
			}
		};
		minDonorWeightKg = new Attribute<Receiver,Integer>(){
			@Override
			public Integer apply(Receiver person) {
				return Integer.valueOf(person.getMinDonorWeightKg());
			}
		};
		acceptShippedKidney = new Attribute<Receiver,Boolean>(){
			@Override
			public Boolean apply(Receiver person) {
				return Boolean.valueOf(person.isAcceptShippedKidney());
			}
		};
		willingToTravel = new Attribute<Receiver,Boolean>(){
			@Override
			public Boolean apply(Receiver person) {
				return Boolean.valueOf(person.isWillingToTravel());
			}
		};
		vpra = new Attribute<Receiver,Double>(){
			@Override
			public Double apply(Receiver input) {
				return ReceiverInputAttributes.this.auxiliaryStatistics.getVirtualPra().get(input);
			}			
		};
		hlaSensitivityAttributes = new EnumMap<HlaType,HlaSensitivityAttribute>(HlaType.class);
		for(HlaType type: HlaType.values()){
			hlaSensitivityAttributes.put(type, new HlaSensitivityAttribute(type));
		}
		specialHlaSensitivityAttributes =
				new EnumMap<SpecialHla,SpecialHlaSensitivityAttribute>(SpecialHla.class);
		for(SpecialHla specialType: SpecialHla.values()){
			specialHlaSensitivityAttributes.put(specialType, new SpecialHlaSensitivityAttribute(specialType));
		}
	}
	
	//private MultiPeriodCyclePackingInputs<ExchangeUnit,DonorEdge,ReadableInstant> problemInput;
	private final ExchangeUnitAuxiliaryInputStatistics auxiliaryStatistics;
	private final Attribute<Receiver,Period> minDonorAgeYr;
	private final Attribute<Receiver,Period> maxDonorAgeYr;
	private final Attribute<Receiver,Integer> minHlaScore;
	private final Attribute<Receiver,Integer> minDonorWeightKg;
	private final Attribute<Receiver,Boolean> acceptShippedKidney;
	private final Attribute<Receiver,Boolean> willingToTravel;
	private final Attribute<Receiver,Double> vpra;
	private final EnumMap<HlaType,HlaSensitivityAttribute> hlaSensitivityAttributes;
	private final EnumMap<SpecialHla,SpecialHlaSensitivityAttribute> specialHlaSensitivityAttributes;
	
	
	public Attribute<Receiver, Period> getMinDonorAgeYr() {
		return minDonorAgeYr;
	}

	public Attribute<Receiver, Period> getMaxDonorAgeYr() {
		return maxDonorAgeYr;
	}

	public Attribute<Receiver, Integer> getMinHlaScore() {
		return minHlaScore;
	}

	public Attribute<Receiver, Integer> getMinDonorWeightKg() {
		return minDonorWeightKg;
	}

	public Attribute<Receiver, Boolean> getAcceptShippedKidney() {
		return acceptShippedKidney;
	}

	public Attribute<Receiver, Boolean> getWillingToTravel() {
		return willingToTravel;
	}

	public Attribute<Receiver, Double> getVpra() {
		return vpra;
	}

	public EnumMap<HlaType, HlaSensitivityAttribute> getHlaSensitivityAttributes() {
		return hlaSensitivityAttributes;
	}

	public EnumMap<SpecialHla, SpecialHlaSensitivityAttribute> getSpecialHlaSensitivityAttributes() {
		return specialHlaSensitivityAttributes;
	}

	public static class HlaSensitivityAttribute extends Attribute<Receiver,List<Integer>>{		
		private HlaType hlaType;		
		public HlaSensitivityAttribute(HlaType hlaType){
			this.hlaType = hlaType;
		}
		@Override
		public List<Integer> apply(Receiver person) {
			List<Integer> ans = new ArrayList<Integer>();
			if(person.getTissueTypeSensitivity().getAntibodies().containsKey(hlaType)){
				for(int i: person.getTissueTypeSensitivity().getAntibodies().get(hlaType)){
					ans.add(Integer.valueOf(i));
				}
			}
			return ans;
		}		
	}
	
	public static class SpecialHlaSensitivityAttribute extends Attribute<Receiver,Boolean>{		
		private SpecialHla hlaType;		
		public SpecialHlaSensitivityAttribute(SpecialHla hlaType){
			this.hlaType = hlaType;
		}
		@Override
		public Boolean apply(Receiver person) {
			if(person.getTissueTypeSensitivity().getAvoidsSpecialAntibodies().containsKey(hlaType)){
				return person.getTissueTypeSensitivity().getAvoidsSpecialAntibodies().get(hlaType);				
			}
			return null;
		}		
	}
	
	
}
