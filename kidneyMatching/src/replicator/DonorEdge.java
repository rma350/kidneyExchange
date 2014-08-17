package replicator;

import data.Donor;

public class DonorEdge {
	
	private Donor donor;
	
	public DonorEdge(Donor donor){
		this.donor = donor;
	}
	
	public Donor getDonor(){
		return this.donor;
	}
	
	public String toString(){
		return "e_"+this.donor.toString() + "_" + super.toString();
	}

}
