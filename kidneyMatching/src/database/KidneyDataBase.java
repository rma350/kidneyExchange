package database;

import kepModeler.ModelerInputs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import replicator.DonorEdge;
import data.ExchangeUnit;

public interface KidneyDataBase {
	
	public ImmutableList<String> availableDatasets();
	
	public ModelerInputs<ExchangeUnit,DonorEdge> loadInputs(String datasetName);

}
