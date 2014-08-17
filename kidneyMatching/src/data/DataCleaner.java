package data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import data.Chain.Cluster;
import data.ExchangeUnit.ExchangeUnitType;
import data.MedicalMatch.Incompatability;

public class DataCleaner {
	
	public static enum ReasonToCleanDonor{
		NO_BLOOD_TYPE {
			@Override
			public boolean valid(Donor donor) {
				return donor.getBloodType() != null;
			}
		}, NO_HLA_MAJOR {
			@Override
			public boolean valid(Donor donor) {
				EnumMap<HlaType,Genotype> hla = donor.getTissueType().getHlaTypes();
				return hla.containsKey(HlaType.A) && hla.containsKey(HlaType.B) && hla.containsKey(HlaType.DR);
			}
		};
		
		public abstract boolean valid(Donor donor);
	}
	
	public static enum ReasonToCleanReceiver{
		NO_BLOOD_TYPE {
			@Override
			public boolean valid(Receiver receiver) {
				return receiver.getBloodType() != null;
			}
		}, NO_HLA_MAJOR {
			@Override
			public boolean valid(Receiver receiver) {
				EnumMap<HlaType,Genotype> hla = receiver.getTissueType().getHlaTypes();
				return hla.containsKey(HlaType.A) && hla.containsKey(HlaType.B) && hla.containsKey(HlaType.DR);
			}
		};
		
		public abstract boolean valid(Receiver receiver);
	}
	
	public static class ReasonsInvalid{
		private ExchangeUnit exchangeUnit;
		private Map<Donor,EnumSet<ReasonToCleanDonor>> invalidDonors;
		private EnumSet<ReasonToCleanReceiver> invalidReceiver;
		
		public ReasonsInvalid(ExchangeUnit exchangeUnit, EnumSet<ReasonToCleanDonor> reasonsToCleanDonors, EnumSet<ReasonToCleanReceiver> reasonsToCleanReceivers){
			this(exchangeUnit);
			for(Donor donor: invalidDonors.keySet()){
				EnumSet<ReasonToCleanDonor> failures = invalidDonors.get(donor);
				for(ReasonToCleanDonor donorReason: reasonsToCleanDonors){
					if(!donorReason.valid(donor)){
						failures.add(donorReason);
					}
				}
			}
			
			if(exchangeUnit.getExchangeUnitType() != ExchangeUnitType.altruistic){
				for(ReasonToCleanReceiver receiverReason : reasonsToCleanReceivers){
					if(!receiverReason.valid(exchangeUnit.getReceiver())){
						invalidReceiver.add(receiverReason);
					}
				}
			}
			
		}
		
		public ReasonsInvalid(ExchangeUnit exchangeUnit) {
			super();
			this.exchangeUnit = exchangeUnit;
			this.invalidDonors = new HashMap<Donor,EnumSet<ReasonToCleanDonor>>();
			for(Donor donor : exchangeUnit.getDonor()){
				this.invalidDonors.put(donor, EnumSet.noneOf(ReasonToCleanDonor.class));
			}
			this.invalidReceiver = EnumSet.noneOf(ReasonToCleanReceiver.class);
		}
		public ExchangeUnit getExchangeUnit() {
			return exchangeUnit;
		}
		public Map<Donor, EnumSet<ReasonToCleanDonor>> getInvalidDonors() {
			return invalidDonors;
		}
		public EnumSet<ReasonToCleanReceiver> getInvalidReceiver() {
			return invalidReceiver;
		}
		
		public int getNumReasons(){
			int ans = 0;
			for(Donor donor : invalidDonors.keySet()){
				ans+= invalidDonors.get(donor).size();				
			}
			ans += invalidReceiver.size();
			return ans;
		}
		
		public String toString(){
			String ans = "" + getNumReasons() + " errors: ";
			for(Donor donor :invalidDonors.keySet() ){
				EnumSet<ReasonToCleanDonor> reasons = invalidDonors.get(donor);
				if(reasons.size() > 0){
					ans += donor.toString() + " " + reasons.toString() + ", ";
				}
			}
			if(invalidReceiver.size() > 0){
				ans+= exchangeUnit.getReceiver().toString() +" " +  invalidReceiver.toString();
			}
			return ans;
		}
		
	}
	
	public static void assessHistoricMatching(ProblemData problemData, Map<ExchangeUnit,ReasonsInvalid> exchangeUnitsCleaned){
		Set<Donor> donorsRemoved = new HashSet<Donor>();
		Set<Receiver> receiversRemoved = new HashSet<Receiver>();
		for(ExchangeUnit unit: exchangeUnitsCleaned.keySet()){
			donorsRemoved.addAll(unit.getDonor());
			receiversRemoved.add(unit.getReceiver());
		}
		int goodMatching = 0;
		int badMatching = 0;
		int erasedMatchings = 0;
		int errors = 0;
		int chains = 0;
		int cycles = 0;
		int clusters = 0;
		for(AbstractMatching matching: problemData.getHistoricMatchingAssignment().getMatchings()){
			if(matching instanceof Cycle){
				cycles++;
				Cycle cycle = (Cycle)matching;
				for(Transplant transplant: cycle.getTransplants()){
					if(problemData.getLooseReceivers().contains(transplant.getReceiver())||
							problemData.getLooseDonors().contains(transplant.getDonor()) ||
							donorsRemoved.contains(transplant.getDonor()) || receiversRemoved.contains(transplant.getReceiver())){
						erasedMatchings++;
					}
					else{
						try{
							EnumSet<Incompatability> transplantCompatability = MedicalMatch.instance.match(transplant.getDonor(), transplant.getReceiver(),transplant.getDateTransplanted());
							if( transplantCompatability.size()== 0){
								goodMatching++;
							}
							else{
								System.err.println("Historic transplant invalid? " + transplant.getDonor().getId() + ", " + transplant.getReceiver().getId());
								System.err.println(transplantCompatability);
								badMatching++;
							}
						}
						catch(RuntimeException e){
							System.err.println("Error while trying to compute feasiblity of matching donor " + transplant.getDonor() 
									+ " with receiver " + transplant.getReceiver().getId());
							throw e;
							//System.err.println(e.getMessage());
							//System.err.println(e.getStackTrace().toString());
							//	errors++;
						}
					}
				}
			}
			else if(matching instanceof Chain){
				chains++;
				Chain chain = (Chain)matching;
				for(Cluster cluster: chain.getClusters()){
					clusters++;
					for(Transplant transplant:cluster.getTransplants()){
						if(problemData.getLooseReceivers().contains(transplant.getReceiver())||
								problemData.getLooseDonors().contains(transplant.getDonor()) ||
								donorsRemoved.contains(transplant.getDonor()) || receiversRemoved.contains(transplant.getReceiver())){
							erasedMatchings++;
						}
						else{
							try{
								EnumSet<Incompatability> transplantCompatability = MedicalMatch.instance.match(transplant.getDonor(), transplant.getReceiver(),transplant.getDateTransplanted());
								if(transplantCompatability.size()==0){
									goodMatching++;
								}
								else{
									System.err.println("Historic transplant invalid? " + transplant.getDonor().getId() + ", " + transplant.getReceiver().getId());
									System.err.println(transplantCompatability);
									badMatching++;
								}
							}
							catch(RuntimeException e){
								System.err.println("Error while trying to compute feasiblity of matching donor " + transplant.getDonor() 
										+ " with receiver " + transplant.getReceiver().getId());
								throw e;
								//System.err.println(e.getMessage());
								//System.err.println(e.getStackTrace().toString());
								//errors++;
							}
						}
					}
				}
			}
		}
		System.err.println("Chains " + chains + ", Cycles " + cycles + " Clusters " + clusters);
		System.err.println("Good Matchings: " + goodMatching  + ", Bad matchings: " + badMatching +", Erased matchings:  " + erasedMatchings + ", Errors: " + errors);
	}
	
	public static Map<ExchangeUnit,ReasonsInvalid> cleanProblemData(ProblemData data, EnumSet<ReasonToCleanDonor> reasonsToCleanDonors, EnumSet<ReasonToCleanReceiver> reasonsToCleanReceivers){
		Map<ExchangeUnit,ReasonsInvalid> invalidExchangeUnits = new HashMap<ExchangeUnit,ReasonsInvalid>();
		for(ExchangeUnit exchangeUnit : data.getExchangeUnits()){
			ReasonsInvalid reasons = new ReasonsInvalid(exchangeUnit,reasonsToCleanDonors,reasonsToCleanReceivers);
			if(reasons.getNumReasons() > 0){
				invalidExchangeUnits.put(exchangeUnit,reasons);
				String error = "Warning: exchange unit " + exchangeUnit.toString() + " was invalid because: " + reasons.toString();
				System.err.println(error);
			}
		}
		for(ExchangeUnit unit: invalidExchangeUnits.keySet()){
			remove(data,unit);
		}
		return invalidExchangeUnits;
	}
	
	private static void remove(ProblemData data, ExchangeUnit exchangeUnit){
		if(exchangeUnit.getExchangeUnitType() == ExchangeUnitType.altruistic){
			data.getAltruisticDonors().remove(exchangeUnit.getDonor().get(0));			
		}
		else if(exchangeUnit.getExchangeUnitType() == ExchangeUnitType.chip){
			data.getChips().remove(exchangeUnit.getReceiver());
			for(Donor donor: exchangeUnit.getDonor()){
				data.getPairedDonors().remove(donor);
			}			
		}
		else if(exchangeUnit.getExchangeUnitType() == ExchangeUnitType.paired){
			for(Donor donor: exchangeUnit.getDonor()){
				data.getPairedDonors().remove(donor);
			}
			data.getPairedReceivers().remove(exchangeUnit);
		}
		data.getExchangeUnits().remove(exchangeUnit);
	}
	
	

}
