package data;

import org.joda.time.DateTime;

public abstract class Person {
	
	private DateTime yearBorn;
	private String id;
	
	private DateTime registered;
	private Gender gender;
	private Race race;
	private BloodType bloodType;
	private TissueType tissueType;
	private Integer heightCm;
	private Integer weightKg;
	
	private Hospital hospital;
	
	public String toString(){
		return id;
	}
	
	
	protected Person(DateTime yearBorn, String id, DateTime registered, Gender gender,
			Race race, BloodType bloodType, TissueType tissueType,
			Integer heightCm, Integer weightKg, Hospital hospital) {
		super();
		this.yearBorn = yearBorn;
		this.id = id;
		this.registered = registered;
		this.gender = gender;
		this.race = race;
		this.bloodType = bloodType;
		this.tissueType = tissueType;
		this.heightCm = heightCm;
		this.weightKg = weightKg;
		this.hospital = hospital;
	}
	public DateTime getYearBorn() {
		return yearBorn;
	}
	public String getId() {
		return id;
	}
	public DateTime getRegistered() {
		return registered;
	}
	public Gender getGender() {
		return gender;
	}
	public Race getRace() {
		return race;
	}
	public BloodType getBloodType() {
		return bloodType;
	}
	public TissueType getTissueType() {
		return tissueType;
	}
	public Integer getHeightCm() {
		return heightCm;
	}
	public Integer getWeightKg() {
		return weightKg;
	}
	public Hospital getHospital() {
		return hospital;
	}

}
