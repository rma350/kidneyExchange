package ui;

import java.util.ArrayList;

import multiPeriod.MultiPeriodCyclePacking.EffectiveNodeType;

public class EffectiveNodeTypePartition<V> extends Partition<EffectiveNodeType,V>{
	
	public EffectiveNodeTypePartition(){
		this.partitionKeys = new ArrayList<PartitionKey>();
		this.partitionKeys.add(new PartitionKey(EffectiveNodeType.chainRoot,"Chain Root"));
		this.partitionKeys.add(new PartitionKey(EffectiveNodeType.paired,"Paired"));
		this.partitionKeys.add(new PartitionKey(EffectiveNodeType.terminal,"Terminal"));
	}

}
