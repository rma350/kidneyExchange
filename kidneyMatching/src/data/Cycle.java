package data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;




public class Cycle extends AbstractMatching {
	
	private List<Transplant> transplants;
	private List<ExchangeUnit> exchangeUnits;

	/**
	 * For transplant i, a donor in exchange unit i gives to the receiver in exchange unit i+1, wrapping around.
	 * @param id unique across all cyles and chains
	 * @param transplants
	 * @param exchangeUnits
	 */
	public Cycle(int id, List<Transplant> transplants, List<ExchangeUnit> exchangeUnits) {
		super(id);
		this.transplants = transplants;
		this.exchangeUnits = exchangeUnits;
	}
	
	

	public List<Transplant> getTransplants() {
		return transplants;
	}
	
	



	public List<ExchangeUnit> getExchangeUnits() {
		return exchangeUnits;
	}



	@Override
	public void checkWellFormed() {
		if(transplants.size() != exchangeUnits.size()){
			throw new RuntimeException("For cycle " + this.getId() + ", transplants and exchangeUnits should be the same size, but were "
					+ transplants.size() + " and " + exchangeUnits.size() + ", respectively.");
		}
		if(transplants.size() < 2){
			throw new RuntimeException("A cycle must have at least two transplants, but only found " + transplants.size());
		}
		for(int i = 0; i < transplants.size(); i++){
			if(!exchangeUnits.get(i).getDonor().contains(transplants.get(i).getDonor())){
				throw new RuntimeException("Error validating cycle " + this.getId()+ 
						". Could not find donor in exchange unit " + i + " for transplant " + i);
			}
			if(!exchangeUnits.get((i+1)%exchangeUnits.size()).getReceiver().equals(transplants.get(i).getReceiver())){
				throw new RuntimeException("Error validating cycle " + this.getId()+ 
						". Could not find receiver in exchange unit " + (i+1) + " for transplant " + i);
			}
		}
		
		return;
		
	}
	
	
	
	

}
