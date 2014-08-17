package data;


import java.util.EnumMap;
import java.util.EnumSet;

import org.joda.time.DateTime;


public class MedicalMatch {
	
	public static EnumMap<HlaType,Integer> defaultHlaPoints;
	static{
		defaultHlaPoints = new EnumMap<HlaType,Integer>(HlaType.class);
		defaultHlaPoints.put(HlaType.A, 10);
		defaultHlaPoints.put(HlaType.B, 15);
		defaultHlaPoints.put(HlaType.DR, 25);
	}
	
	/**
	 * If your weight is below the threshold, we assume your weight is the assumed weight.  There seem to be many people in the data with the weight 18kg.
	 */
	public static int assumedWeight = 60;
	public static int assumedWeightThreshold = 20;
	
	public static final MedicalMatch instance = new MedicalMatch(defaultHlaPoints);
	
	public MedicalMatch(EnumMap<HlaType,Integer> hlaPoints){
		this.hlaPoints = hlaPoints;
	}
	
	private EnumMap<HlaType,Integer> hlaPoints;
	
	public static enum Incompatability{
		BLOOD_TYPE,AGE_YOUNG,AGE_OLD,WEIGHT_LOW,NO_TRAVEL,HLA_LOW,TISSUE_TYPE;
	}
	
	
	
	public EnumSet<Incompatability> match(Donor donor, Receiver receiver, DateTime dateTime){
		EnumSet<Incompatability> ans = EnumSet.noneOf(Incompatability.class);
		if(!bloodTypeCompatible(donor.getBloodType(),receiver.getBloodType())){
			ans.add(Incompatability.BLOOD_TYPE);
		}
		
		if(donor.getYearBorn().plus(receiver.getMinDonorAgeYr()).isAfter(dateTime)){
			ans.add(Incompatability.AGE_YOUNG);
		}
		if(donor.getYearBorn().plus(receiver.getMaxDonorAgeYr()).isBefore(dateTime)){
			ans.add(Incompatability.AGE_OLD);
		}
		int weight = donor.getWeightKg();
		if(weight <= assumedWeightThreshold){
			weight = assumedWeight;
		}
		if(weight < receiver.getMinDonorWeightKg()){
			ans.add(Incompatability.WEIGHT_LOW);
		}
		if(!receiver.isWillingToTravel() && !receiver.isAcceptShippedKidney() && !receiver.getHospital().getCity().equals(donor.getHospital().getCity())){
			ans.add(Incompatability.NO_TRAVEL);
		}
		if(hlaScore(donor.getTissueType(),receiver.getTissueType(),this.hlaPoints) < receiver.getMinHlaScore()){
			ans.add(Incompatability.HLA_LOW);
		}
		if(!receiver.getTissueTypeSensitivity().isCompatible(donor.getTissueType())){
			ans.add(Incompatability.TISSUE_TYPE);
		}
		
		return ans;
	}
	
	private static EnumMap<BloodType,EnumSet<BloodType>> canDonateTo;
	static{
		canDonateTo = new EnumMap<BloodType,EnumSet<BloodType>>(BloodType.class);
		canDonateTo.put(BloodType.O, EnumSet.of(BloodType.A, BloodType.B, BloodType.AB, BloodType.O));
		canDonateTo.put(BloodType.A, EnumSet.of(BloodType.A, BloodType.AB));
		canDonateTo.put(BloodType.B, EnumSet.of(BloodType.B, BloodType.AB));
		canDonateTo.put(BloodType.AB, EnumSet.of(BloodType.AB));
	}
	
	public static boolean bloodTypeCompatible(BloodType donor, BloodType receiver){
		return canDonateTo.get(donor).contains(receiver);
	}
	
	
	
	public static int hlaScore(TissueType donor, TissueType receiver, EnumMap<HlaType,Integer> hlaPoints){
		int ans = 0;
		for(HlaType hla : EnumSet.of(HlaType.A, HlaType.B, HlaType.DR)){
			ans += hlaPoints.get(hla).intValue()* receiver.getHlaTypes().get(hla).containsCount(donor.getHlaTypes().get(hla));
		}
		return ans;
	}
	
	

}
