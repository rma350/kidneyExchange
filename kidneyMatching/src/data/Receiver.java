package data;



import org.joda.time.DateTime;
import org.joda.time.Period;

public class Receiver extends Person{
	
	
	private TissueTypeSensitivity tissueTypeSensitivity;
	
	private Period minDonorAgeYr;
	private Period maxDonorAgeYr;
	private int minHlaScore;
	private int minDonorWeightKg;
	
	private boolean acceptShippedKidney;
	private boolean willingToTravel;
	
	
	
	public Receiver(DateTime yearBorn, String id, DateTime registered, Gender gender,
			Race race, BloodType bloodType,
			TissueTypeSensitivity tissueTypeSensitivity, TissueType tissueType,
			Integer heightCm, Integer weightKg, Period minDonorAgeYr, Period maxDonorAgeYr,
			int minHlaScore, int minDonorWeightKg, boolean acceptShippedKidney,
			boolean willingToTravel, Hospital hospital) {
		super(yearBorn, id, registered, gender,
				race, bloodType, tissueType,
				heightCm, weightKg, hospital);
		this.tissueTypeSensitivity = tissueTypeSensitivity;
		
		this.minDonorAgeYr = minDonorAgeYr;
		this.maxDonorAgeYr = maxDonorAgeYr;
		this.minHlaScore = minHlaScore;
		this.minDonorWeightKg = minDonorWeightKg;
		this.acceptShippedKidney = acceptShippedKidney;
		this.willingToTravel = willingToTravel;
		
	}
	
	public int getMinDonorWeightKg() {
		return minDonorWeightKg;
	}
	
	
	public TissueTypeSensitivity getTissueTypeSensitivity() {
		return tissueTypeSensitivity;
	}
	
	public Period getMinDonorAgeYr() {
		return minDonorAgeYr;
	}
	public Period getMaxDonorAgeYr() {
		return maxDonorAgeYr;
	}
	public int getMinHlaScore() {
		return minHlaScore;
	}
	public boolean isAcceptShippedKidney() {
		return acceptShippedKidney;
	}
	public boolean isWillingToTravel() {
		return willingToTravel;
	}
	
	
	

}
