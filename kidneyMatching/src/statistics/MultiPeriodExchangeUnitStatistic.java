package statistics;

import java.util.HashMap;
import java.util.Map;

import multiPeriod.MultiPeriodCyclePacking;
import data.Donor;
import data.ExchangeUnit.ExchangeUnitType;
import data.Receiver;


import org.joda.time.ReadableInstant;

import replicator.DonorEdge;

import data.ExchangeUnit;

public class MultiPeriodExchangeUnitStatistic extends MultiPeriodCyclePackingStatistic<ExchangeUnit,DonorEdge,ReadableInstant>{
	
	

	public MultiPeriodExchangeUnitStatistic(
			MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> multiPeriodPacking) {
		super(multiPeriodPacking);	
	}

}
