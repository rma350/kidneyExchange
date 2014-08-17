package ui;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Multimap;

public abstract class Partition<K,V> {
	
	protected Multimap<K,V> partition;
	protected List<PartitionKey> partitionKeys;
	
	//protected abstract Multimap<K,V> makePartition(Set<V> set);
	
	protected Partition(){}
	
	public List<PartitionKey> getPartitionKeys(){
		return this.partitionKeys;
	}
	
	public class PartitionKey{
		
		private K key;
		private String keyName;
		
		public PartitionKey(K key, String keyName){
			this.key = key;
			this.keyName = keyName;
		}
		
		public K getKey(){
			return key;
		}
		
		public String getKeyName(){
			return this.keyName;
		}
		
		
	}

}
