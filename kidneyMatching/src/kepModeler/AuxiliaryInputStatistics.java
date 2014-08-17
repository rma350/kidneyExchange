package kepModeler;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class AuxiliaryInputStatistics<V,E> {
	
	private ImmutableMap<V,Double> donorPowerPostPreference;
	private ImmutableMap<V,Double> receiverPowerPostPreference;
	
	
	
	public AuxiliaryInputStatistics(ImmutableMap<V,Double> donorPowerPostPreference, ImmutableMap<V,Double> receiverPowerPostPreference){
		this.donorPowerPostPreference = donorPowerPostPreference;
		this.receiverPowerPostPreference = receiverPowerPostPreference;
	}
	
	


	public ImmutableMap<V, Double> getDonorPowerPostPreference() {
		return donorPowerPostPreference;
	}


	public void setDonorPowerPostPreference(ImmutableMap<V, Double> donorPowerPostPreference) {
		this.donorPowerPostPreference = donorPowerPostPreference;
	}


	public ImmutableMap<V, Double> getReceiverPowerPostPreference() {
		return receiverPowerPostPreference;
	}


	public void setReceiverPowerPostPreference(
			ImmutableMap<V, Double> receiverPowerPostPreference) {
		this.receiverPowerPostPreference = receiverPowerPostPreference;
	}

	
	
	
	
	
	
	

}
