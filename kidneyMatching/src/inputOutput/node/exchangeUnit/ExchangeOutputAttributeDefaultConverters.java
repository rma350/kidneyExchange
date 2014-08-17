package inputOutput.node.exchangeUnit;

import inputOutput.Attributes;
import inputOutput.Attributes.AttributeName;
import inputOutput.Attributes.ExchangeIn;
import inputOutput.Attributes.ExchangeOut;
import inputOutput.Attributes.NodeOut;
import inputOutput.core.Attribute;
import inputOutput.core.AttributeConverter;
import inputOutput.core.CompositeAttributeConverter;
import inputOutput.core.Converters;
import inputOutput.core.CsvFormat;
import inputOutput.core.CsvFormatUtil;
import inputOutput.node.NodeOutputAttributeDefaultConverters;


import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.joda.time.Interval;
import org.joda.time.ReadableInstant;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import data.Donor;
import data.ExchangeUnit;
import data.ExchangeUnit.ExchangeUnitType;
import data.ProblemData.ColumnName;
import data.Receiver;
import replicator.DonorEdge;

public class ExchangeOutputAttributeDefaultConverters 
	//extends NodeOutputAttributeDefaultConverters<ExchangeUnit,DonorEdge,ReadableInstant,Interval>
	implements Converters<ExchangeOut,ExchangeUnit>{
	
	
	private ExchangeOutputAttributes exchangeOutAttributes;
	private EnumMap<ExchangeOut,AttributeConverter<ExchangeUnit>> exchangeUnitAttributes;
	
	private <T> void add(ExchangeOut exchangeOut, Attribute<ExchangeUnit,T> attribute, CsvFormat<? super T> formatter ){
		this.exchangeUnitAttributes.put(exchangeOut, new CompositeAttributeConverter<ExchangeUnit,T>(attribute,formatter));
	}
	
	public ExchangeOutputAttributeDefaultConverters(ExchangeOutputAttributes exchangeOutAttributes) {
		this.exchangeOutAttributes = exchangeOutAttributes;
		exchangeUnitAttributes = new EnumMap<ExchangeOut,AttributeConverter<ExchangeUnit>>(ExchangeOut.class);
		add(ExchangeOut.matchedDonor, exchangeOutAttributes.getMatchedDonor(), CsvFormatUtil.idFormat);
		add(ExchangeOut.matchedOrFirstDonor, exchangeOutAttributes.getMatchedDonor(), CsvFormatUtil.idFormat);

	}
	
	public static AttributeConverter<ExchangeUnit> donorToUnit(final AttributeConverter<? super Donor> donorConverter, final int donorIndex){
		return new AttributeConverter<ExchangeUnit>(){
			@Override
			public String apply(ExchangeUnit input) {
				return donorConverter.apply(input.getDonor().get(donorIndex));
			}			
		};
	}
	
	public static AttributeConverter<ExchangeUnit> makeCompositeDonorNodeAttribute(final AttributeConverter<? super Donor> donorConverter, final Attribute<ExchangeUnit,Donor> makeDonor){
		return new AttributeConverter<ExchangeUnit>(){
			@Override
			public String apply(ExchangeUnit input) {
				Donor donor = makeDonor.apply(input);
				return donor == null?  null : donorConverter.apply(donor);
			}			
		};
	}
	
	public static AttributeConverter<ExchangeUnit> receiverToUnit(final AttributeConverter<? super Receiver> receiverConverter){
		return new AttributeConverter<ExchangeUnit>(){
			@Override
			public String apply(ExchangeUnit input) {
				return input.getReceiver() == null ? "" : receiverConverter.apply(input.getReceiver());
			}
		};
	}
	
	

	
	public ExchangeOutputAttributes getExchangeOutAttributes() {
		return exchangeOutAttributes;
	}


	@Override
	public AttributeConverter<ExchangeUnit> get(ExchangeOut attributeName) {
		if(!this.exchangeUnitAttributes.containsKey(attributeName)){
			throw new UnsupportedOperationException();
		}
		return this.exchangeUnitAttributes.get(attributeName);
	}

	
	
}
