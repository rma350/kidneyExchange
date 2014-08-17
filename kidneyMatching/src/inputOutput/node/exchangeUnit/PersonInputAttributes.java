package inputOutput.node.exchangeUnit;



import inputOutput.core.Attribute;

import java.util.EnumMap;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import replicator.DonorEdge;
import data.BloodType;
import data.ExchangeUnit;
import data.Gender;
import data.Genotype;
import data.HlaType;
import data.Hospital;
import data.Person;
import data.Race;
import data.SpecialHla;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;

public class PersonInputAttributes {

	private MultiPeriodCyclePackingInputs<ExchangeUnit,DonorEdge,ReadableInstant> inputs;
	private final Attribute<Person,DateTime> yearBorn;
	private final Attribute<Person,String> id;
	private final Attribute<Person,DateTime>  registered;
	private final Attribute<Person,Gender> gender;
	private final Attribute<Person,Race> race;
	private final Attribute<Person,BloodType> bloodType;
	private final EnumMap<HlaType,HlaTypeAttribute> hlaLow;
	private final EnumMap<HlaType,HlaTypeAttribute> hlaHigh;
	private final EnumMap<SpecialHla,SpecialHlaAttribute> specialHla;
	private final Attribute<Person,Integer> heightCm;
	private final Attribute<Person,Integer> weightKg;
	private final Attribute<Person,Hospital> hospital;

	public PersonInputAttributes(MultiPeriodCyclePackingInputs<ExchangeUnit,DonorEdge,ReadableInstant> inputs){
		this.inputs = inputs;
		yearBorn = new Attribute<Person,DateTime>(){		
			@Override
			public DateTime apply(Person person){
				return person.getYearBorn();
			}		
		};	
		id = new Attribute<Person,String>(){
			@Override
			public String apply(Person person){
				return person.getId();
			}
		};

		registered = new Attribute<Person,DateTime>(){		
			@Override
			public DateTime apply(Person person){
				return person.getRegistered();
			}		
		};
		gender = new Attribute<Person,Gender>(){

			@Override
			public Gender apply(Person person) {
				return person.getGender();
			}

		};
		race = new Attribute<Person,Race>(){
			@Override
			public Race apply(Person person) {
				return person.getRace();
			}		
		};
		bloodType = new Attribute<Person,BloodType>(){
			@Override
			public BloodType apply(Person person) {
				return person.getBloodType();
			}

		};

		hlaLow = new EnumMap<HlaType,HlaTypeAttribute>(HlaType.class);
		for(HlaType hlaType: HlaType.values()){
			hlaLow.put(hlaType, new HlaTypeAttribute(hlaType,false));
		}



		hlaHigh = new EnumMap<HlaType,HlaTypeAttribute>(HlaType.class);
		for(HlaType hlaType: HlaType.values()){
			hlaHigh.put(hlaType, new HlaTypeAttribute(hlaType,true));
		}




		specialHla = new EnumMap<SpecialHla,SpecialHlaAttribute>(SpecialHla.class);
		for(SpecialHla special: SpecialHla.values()){
			specialHla.put(special, new SpecialHlaAttribute(special));
		}


		heightCm = new Attribute<Person,Integer>(){
			@Override
			public Integer apply(Person person) {
				return person.getHeightCm();
			}};		

			weightKg = new Attribute<Person,Integer>(){

				@Override
				public Integer apply(Person person) {
					return person.getWeightKg();
				}

			}; 


			hospital = new Attribute<Person,Hospital>(){
				@Override
				public Hospital apply(Person person) {
					return person.getHospital();
				}

			};
	}
	
	


	public Attribute<Person,DateTime> getYearBorn() {
		return yearBorn;
	}

	public Attribute<Person,String> getId() {
		return id;
	}

	public Attribute<Person,DateTime> getRegistered() {
		return registered;
	}

	public Attribute<Person,Gender> getGender() {
		return gender;
	}

	public Attribute<Person,Race> getRace() {
		return race;
	}

	public Attribute<Person,BloodType> getBloodType() {
		return bloodType;
	}

	public EnumMap<HlaType, HlaTypeAttribute> getHlaLow() {
		return hlaLow;
	}

	public EnumMap<HlaType, HlaTypeAttribute> getHlaHigh() {
		return hlaHigh;
	}

	public EnumMap<SpecialHla, SpecialHlaAttribute> getSpecialHla() {
		return specialHla;
	}

	public Attribute<Person,Integer> getHeightCm() {
		return heightCm;
	}

	public Attribute<Person,Integer> getWeightKg() {
		return weightKg;
	}

	public Attribute<Person,Hospital> getHospital() {
		return hospital;
	}




	public static class SpecialHlaAttribute extends Attribute<Person,Boolean>{

		private SpecialHla specialHla;

		public SpecialHlaAttribute(SpecialHla specialHla) {
			super();
			this.specialHla = specialHla;
		}

		@Override
		public Boolean apply(Person person) {
			if(person.getTissueType().getSpecialHla().containsKey(specialHla)){
				return person.getTissueType().getSpecialHla().get(specialHla);
			}
			return null;
		}

	}

	public static class HlaTypeAttribute extends Attribute<Person,Integer>{

		private HlaType hlaType;
		private boolean low;

		public HlaTypeAttribute(HlaType hlaType, boolean low){
			this.hlaType = hlaType;
			this.low = low;
		}

		@Override
		public Integer apply(Person person) {
			if(person.getTissueType().getHlaTypes().containsKey(hlaType)){
				Genotype geno =  person.getTissueType().getHlaTypes().get(hlaType);
				return low? geno.getAlleleLo() : geno.getAlleleHi();
			}
			return null;
		}

	}

}
