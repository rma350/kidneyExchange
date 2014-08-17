package data;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import data.Transplant.TransplantStatus;

public class CycleClusterBuilder {
	
	private Map<Integer,TransplantBuilder> transplants;
	
	private int cycleChainId;
	private int clusterIndex;
	
	
	public CycleClusterBuilder(int cycleChainId, int clusterIndex){
		transplants = new HashMap<Integer,TransplantBuilder>();
		this.cycleChainId = cycleChainId;
		this.clusterIndex = clusterIndex;
	}
	
	
	
	
	
	
	public Map<Integer, TransplantBuilder> getTransplants() {
		return transplants;
	}






	public int getCycleChainId() {
		return cycleChainId;
	}






	public int getClusterIndex() {
		return clusterIndex;
	}






	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + clusterIndex;
		result = prime * result + cycleChainId;
		return result;
	}




	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CycleClusterBuilder other = (CycleClusterBuilder) obj;
		if (clusterIndex != other.clusterIndex)
			return false;
		if (cycleChainId != other.cycleChainId)
			return false;
		return true;
	}




	public static class TransplantBuilder{
		private String donorId;
		private String receiverId;
		private TransplantStatus transplantStatus;
		private DateTime date;
		public String getDonorId() {
			return donorId;
		}
		public void setDonorId(String donorId) {
			this.donorId = donorId;
		}
		public String getReceiverId() {
			return receiverId;
		}
		public void setReceiverId(String receiverId) {
			this.receiverId = receiverId;
		}
		public TransplantStatus getTransplantStatus() {
			return transplantStatus;
		}
		public void setTransplantStatus(TransplantStatus transplantStatus) {
			this.transplantStatus = transplantStatus;
		}
		public DateTime getDate() {
			return date;
		}
		public void setDate(DateTime date) {
			this.date = date;
		}
		public TransplantBuilder() {
			super();
		}
		
		
	}
	

}
