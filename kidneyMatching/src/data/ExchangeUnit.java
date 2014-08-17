package data;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import com.google.common.base.CharMatcher;

public class ExchangeUnit {
	
	public static enum ExchangeUnitType{
		altruistic,paired,chip;
	}

	private ExchangeUnitType exchangeUnitType;
	private List<Donor> donor;
	private Receiver receiver;
	

	
	public ExchangeUnitType getExchangeUnitType() {
		return exchangeUnitType;
	}

	public static ExchangeUnit makeAltruistic(Donor donor){
		ExchangeUnit ans = new ExchangeUnit(ExchangeUnitType.altruistic,null);
		ans.getDonor().add(donor);
		return ans;
	}
	
	public static ExchangeUnit makeChip(Receiver receiver){
		ExchangeUnit ans = new ExchangeUnit(ExchangeUnitType.chip,receiver);
		return ans;
	}
	
	public static ExchangeUnit makePaired(Receiver receiver, Donor...donors){
		ExchangeUnit ans = new ExchangeUnit(ExchangeUnitType.paired,receiver);
		for(Donor donor: donors){
			ans.getDonor().add(donor);
		}
		return ans;
	}
	
	private ExchangeUnit(ExchangeUnitType exchangeUnitType, Receiver receiver) {
		super();
		this.exchangeUnitType = exchangeUnitType;
		this.donor = new ArrayList<Donor>();
		this.receiver = receiver;
	}
	public List<Donor> getDonor() {
		return donor;
	}
	public Receiver getReceiver() {
		return receiver;
	}
	
	public String toString(){
		String ans;
		if(this.exchangeUnitType == ExchangeUnitType.altruistic){
			ans = "{" + donor.toString() + "}";
		}
		else{
			ans = "{" + receiver.toString() + ", " + donor.toString() + "}";
		}
		return CharMatcher.is(',').replaceFrom(ans, '-');
		
		
	}
	
	
}
