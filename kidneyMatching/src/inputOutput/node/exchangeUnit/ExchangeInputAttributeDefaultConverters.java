package inputOutput.node.exchangeUnit;

import java.util.EnumMap;

import data.ExchangeUnit;
import data.ExchangeUnit.ExchangeUnitType;
import inputOutput.Attributes.ExchangeIn;
import inputOutput.core.Attribute;
import inputOutput.core.AttributeConverter;
import inputOutput.core.CompositeAttributeConverter;
import inputOutput.core.Converters;
import inputOutput.core.CsvFormat;
import inputOutput.core.CsvFormatUtil;
import inputOutput.node.NodeInputAttributes;
import inputOutput.node.NodeInputAttributeDefaultConverters;
import multiPeriod.TimeInstant;

import org.joda.time.Interval;
import org.joda.time.ReadableInstant;


import replicator.DonorEdge;

public class ExchangeInputAttributeDefaultConverters implements Converters<ExchangeIn,ExchangeUnit>{
	
	private EnumMap<ExchangeIn,AttributeConverter<ExchangeUnit>> attributes;
	private ExchangeInputAttributes inputAttributes;
	
	private <A> void add(ExchangeIn name, Attribute<ExchangeUnit,A> attribute, CsvFormat<? super A> format){
		attributes.put(name, new CompositeAttributeConverter<ExchangeUnit,A>(attribute,format));
	}
	
	

	public ExchangeInputAttributes getInputAttributes() {
		return inputAttributes;
	}



	public ExchangeInputAttributeDefaultConverters(
			ExchangeInputAttributes inputAttributes) {
		//super(inputAttributes, CsvFormatUtil.mmddyyyyFormatTimeInstant);
		attributes = new EnumMap<ExchangeIn,AttributeConverter<ExchangeUnit>>(ExchangeIn.class);
		add(ExchangeIn.exchangeUnitType, inputAttributes.getExchangeUnitType(),CsvFormatUtil.exchangeUnitType);
		add(ExchangeIn.donors, inputAttributes.getDonors(),CsvFormatUtil.personListNameFormat);
		add(ExchangeIn.isChip, inputAttributes.getIsChip(),CsvFormatUtil.yesNo);
		add(ExchangeIn.numDonors, inputAttributes.getNumDonors(),CsvFormatUtil.toStringNullBlank);
	}

	public EnumMap<ExchangeIn, AttributeConverter<ExchangeUnit>> getAttributes() {
		return attributes;
	}

	@Override
	public AttributeConverter<ExchangeUnit> get(ExchangeIn attributeName) {
		if(!this.attributes.containsKey(attributeName)){
			throw new UnsupportedOperationException();
		}
		return this.attributes.get(attributeName);
	}
	
	

}
