package data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import data.Chain.Cluster;

public class MatchingAssignment {
	
	private List<AbstractMatching> matchings;
	private Set<Chain> activeChains;
	
	public MatchingAssignment(){
		this.matchings = new ArrayList<AbstractMatching>();
		this.activeChains = new HashSet<Chain>();
	}

	public List<AbstractMatching> getMatchings() {
		return matchings;
	}

	public void setMatchings(List<AbstractMatching> matchings) {
		this.matchings = matchings;
	}

	public Set<Chain> getActiveChains() {
		return activeChains;
	}
	
	public Set<Donor> getMatchedDonors(){
		Set<Donor> ans = new HashSet<Donor>();
		for(AbstractMatching matching: this.matchings){
			if(matching instanceof Cycle){
				for(ExchangeUnit unit: ((Cycle)matching).getExchangeUnits()){
					ans.addAll(unit.getDonor());
				}
			}
			else if(matching instanceof Chain){
				List<Cluster> clusters = ((Chain)matching).getClusters();
				for(int cluster = 0; cluster< clusters.size(); cluster++){
					List<ExchangeUnit> exchanges = clusters.get(cluster).getExchangeUnits();
					for(int exchange = 0; exchange< exchanges.size();exchange++){
						if(cluster< clusters.size()-1 || exchange < exchanges.size()-1){
							ans.addAll(exchanges.get(exchange).getDonor());
						}
					}
				}
			}
			else{
				throw new RuntimeException();
			}
		}
		return ans;
	}
	
	public Set<Receiver> getMatchedReceivers(){
		Set<Receiver> ans = new HashSet<Receiver>();
		for(AbstractMatching matching: this.matchings){
			if(matching instanceof Cycle){
				for(ExchangeUnit unit: ((Cycle)matching).getExchangeUnits()){
					ans.add(unit.getReceiver());
				}
			}
			else if(matching instanceof Chain){
				List<Cluster> clusters = ((Chain)matching).getClusters();
				for(int cluster = 0; cluster< clusters.size(); cluster++){
					List<ExchangeUnit> exchanges = clusters.get(cluster).getExchangeUnits();
					for(int exchange = 0; exchange< exchanges.size();exchange++){
						if(cluster>0  || exchange >0){
							ans.add(exchanges.get(exchange).getReceiver());
						}
					}
				}
			}
			else{
				throw new RuntimeException();
			}
		}
		return ans;
	}


	
	

}
