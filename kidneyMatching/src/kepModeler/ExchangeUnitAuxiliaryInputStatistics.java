package kepModeler;

import replicator.DonorEdge;

import com.google.common.collect.ImmutableMap;

import data.ExchangeUnit;
import data.Receiver;

public class ExchangeUnitAuxiliaryInputStatistics extends
		AuxiliaryInputStatistics<ExchangeUnit,DonorEdge> {

	private ImmutableMap<Receiver,Double> virtualPra;
	
	public ExchangeUnitAuxiliaryInputStatistics(
			ImmutableMap<ExchangeUnit,Double> donorPowerPostPreference,
			ImmutableMap<ExchangeUnit,Double> receiverPowerPostPreference,
			ImmutableMap<Receiver,Double> virtualPra) {
		super(donorPowerPostPreference, receiverPowerPostPreference);
		this.virtualPra = virtualPra;
	}
	
	public ImmutableMap<Receiver, Double> getVirtualPra() {
		return virtualPra;
	}

	
	
	

}
