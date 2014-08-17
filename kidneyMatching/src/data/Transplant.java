package data;

import org.joda.time.DateTime;

public class Transplant {
	
	public static enum TransplantStatus{
		NEED_A_MATCH(1), OFFER_MADE(2), PENDING_CROSSMATCH(3),CROSSMATCHED(4), TRANSPLANTED(5);		
		private int code;
		
		private TransplantStatus(int code){
			this.code = code;
		}
		
		public int getCode(){
			return this.code;
		}
		
		public static TransplantStatus maximum(TransplantStatus stat1, TransplantStatus stat2){
			if(stat1.getCode() > stat2.getCode()){
				return stat1;
			}
			return stat2;
		}
		
		/**
		 * 
		 * @param code
		 * @return null if code does not correspond to any TransplantStatus, the corresponding TransplantStatus otherwise.
		 */
		public static TransplantStatus getStatusByCode(int code){
			for(TransplantStatus status : TransplantStatus.values()){
				if(status.getCode() == code){
					return status;
				}
			}
			return null;
		}
	}

	private Donor donor;
	private Receiver receiver;
	private TransplantStatus status;	
	private DateTime dateTransplanted;
	public Donor getDonor() {
		return donor;
	}
	public Receiver getReceiver() {
		return receiver;
	}
	public TransplantStatus getStatus() {
		return status;
	}
	public DateTime getDateTransplanted() {
		return dateTransplanted;
	}
	public Transplant(Donor donor, Receiver receiver, TransplantStatus status,
			DateTime dateTransplanted) {
		super();
		this.donor = donor;
		this.receiver = receiver;
		this.status = status;
		this.dateTransplanted = dateTransplanted;
	}
	
	
	
	
}
