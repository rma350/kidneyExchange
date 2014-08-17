package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chain extends AbstractMatching {
	
	public static class Cluster{
		
		
		private List<Transplant> transplants;
		private List<ExchangeUnit> exchangeUnits;
		
		

		
		/**
		 * There are n+1 exchange units and n transplants.
		 * @param chain
		 * @param transplants
		 * @param exchangeUnits
		 */
		public Cluster(List<Transplant> transplants, List<ExchangeUnit> exchangeUnits){

			this.transplants = transplants;
			this.exchangeUnits = exchangeUnits;
			
		}
		
		
		
		
		public List<Transplant> getTransplants() {
			return transplants;
		}




		public List<ExchangeUnit> getExchangeUnits() {
			return exchangeUnits;
		}






		public void checkWellFormed(boolean isLastCluster, Chain chain){
			if(transplants.size()+1 != exchangeUnits.size()){
				throw new RuntimeException("Must have one more exchange unit than transplant, but found trans: " 
						+ transplants.size() + " and exchange: " +exchangeUnits.size() );
				
			}
			for(int i = 0; i < exchangeUnits.size()-1; i++){
				if(!exchangeUnits.get(i).getDonor().contains(transplants.get(i).getDonor())){
					System.err.println("In transplant: " + transplants.get(i).getDonor().getId());
					for(Donor donor: exchangeUnits.get(i).getDonor()){
						System.err.println("In exchange unit: " + donor.getId());
					}
					throw new RuntimeException("Chain: " + chain.getId() +  " cluster : " + (i+1) + " Must have the " +i+ " donor in chain in the " + i + " exchange unit, but was not found." );
				}
				if(!(isLastCluster && i == exchangeUnits.size()-2)){
					//System.err.println("Chain: " + chain.getId() + " with " + 1 + " clusters");
					for(int k = 0; k < chain.clusters.size(); k++){
						if(chain.clusters.get(k) == this){
							//System.err.println("Error in cluster " + (k+1));
						}
					}
					//System.err.println("Location: " + (i+1));
					//System.err.println("Donor: " + transplants.get(i).getDonor());
					if(!exchangeUnits.get(i+1).getReceiver().equals(transplants.get(i).getReceiver())){

						throw new RuntimeException("Must have the " +i+ " receiver in chain in the " + (i+1) + " exchange unit, but was not found." );
					}
				}
			}

			
		}
	}
	
	private List<Cluster> clusters;

	public Chain(int id, List<Cluster> clusters) {
		super(id);
		this.clusters = clusters;
	}

	@Override
	public void checkWellFormed() {
		for(int i = 0; i < clusters.size(); i++){
			clusters.get(i).checkWellFormed(i == clusters.size()-1,this);
		}
		for(int i = 0; i < clusters.size()-1; i++){
			List<ExchangeUnit> clusterR = clusters.get(i).getExchangeUnits();
			List<ExchangeUnit> clusterD = clusters.get(i+1).getExchangeUnits();
			if(!clusterR.get(clusterR.size()-1).equals(clusterD.get(0))){
				throw new RuntimeException("Error in validating chain " + this.getId() + 
						", last member of cluster " + i + " not equal to first member of cluster " + (i+1));
			}
		}
	}

	public List<Cluster> getClusters() {
		return clusters;
	}
	
	

}
