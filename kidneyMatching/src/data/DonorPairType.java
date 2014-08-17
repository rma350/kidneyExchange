package data;

public enum DonorPairType {
	dArA(BloodType.A, BloodType.A),
	dArB(BloodType.A, BloodType.B),
	dArAB(BloodType.A, BloodType.AB),
	dArO(BloodType.A, BloodType.O),
	
	dBrA(BloodType.B, BloodType.A),
	dBrB(BloodType.B, BloodType.B),
	dBrAB(BloodType.B, BloodType.AB),
	dBrO(BloodType.B, BloodType.O),
	
	dABrA(BloodType.AB, BloodType.A),
	dABrB(BloodType.AB, BloodType.B),
	dABrAB(BloodType.AB, BloodType.AB),
	dABrO(BloodType.AB, BloodType.O),
	
	dOrA(BloodType.O, BloodType.A),
	dOrB(BloodType.O, BloodType.B),
	dOrAB(BloodType.O, BloodType.AB),
	dOrO(BloodType.O, BloodType.O);
	
	private BloodType donorType;
	private BloodType receiverType;
	
	private DonorPairType(BloodType donorType, BloodType receiverType){
		this.donorType = donorType;
		this.receiverType = receiverType;
	}

	public BloodType getDonorType() {
		return donorType;
	}

	public BloodType getReceiverType() {
		return receiverType;
	}
	
	

}
