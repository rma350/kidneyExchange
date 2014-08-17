package data;


import org.joda.time.DateTime;

public class Donor extends Person{
	
	

	
	
	public Donor(DateTime yearBorn, String id, DateTime registered, Gender gender,
			Race race, BloodType bloodType, TissueType tissueType,
			Integer heightCm, Integer weightKg, Hospital hospital) {
		super(yearBorn, id, registered, gender,
			race, bloodType, tissueType,
			heightCm, weightKg, hospital);
	}
		
	
	
}
