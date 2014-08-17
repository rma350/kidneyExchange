package inputOutput.node;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import multiPeriod.TimeInstant;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.ReadableInstant;

import inputOutput.Attributes;
import inputOutput.Attributes.AttributeName;
import inputOutput.Attributes.NodeOut;
import inputOutput.Attributes.NodeIn;
import inputOutput.core.Attribute;
import inputOutput.core.AttributeConverter;
import inputOutput.core.CompositeAttributeConverter;
import inputOutput.core.Converters;
import inputOutput.core.CsvFormat;
import inputOutput.core.CsvFormatUtil;

public class NodeOutputAttributeDefaultConverters<V,E,T extends Comparable<T>,D> implements Converters<NodeOut,V> {
	
	
	private EnumMap<NodeOut,AttributeConverter<V>> attributeMap;
	private NodeOutputAttributes<V,E,T,D> mpMatchingAttributes;
	private NodeInputAttributes<V,E,T> inputAttributes;
	private CsvFormat<TimeInstant<T>> timeFormat;
	private CsvFormat<D> timeDifferenceFormat;
	
	
	public NodeOutputAttributeDefaultConverters(NodeOutputAttributes<V,E,T,D> mpMatchingAttributes, 
			CsvFormat<TimeInstant<T>> timeFormat, CsvFormat<D> timeDifferenceFormat){
		this.attributeMap = new EnumMap<NodeOut,AttributeConverter<V>>(NodeOut.class);
		this.timeFormat = timeFormat;
		this.timeDifferenceFormat = timeDifferenceFormat;
		this.mpMatchingAttributes = mpMatchingAttributes;
		this.inputAttributes = mpMatchingAttributes.getInputAttributes();
		add(NodeOut.donorWasMatched, mpMatchingAttributes.getDonorWasMatched(),CsvFormatUtil.excelTrueFalse);
		add(NodeOut.donorWaitingTime,mpMatchingAttributes.getDonorWaitingTime(),timeDifferenceFormat);//CsvFormatUtil.numDays);
		add(NodeOut.receiverWaitingTime, mpMatchingAttributes.getReceiverWaitingTime(),timeDifferenceFormat);
		add(NodeOut.receiverWasMatched, mpMatchingAttributes.getReceiverWasMatched(),CsvFormatUtil.excelTrueFalse);
		add(NodeOut.timeDonorMatched, mpMatchingAttributes.getTimeDonorMatched(),timeFormat);//CsvFormatUtil.mmddyyyyFormatTimeInstant);
		add(NodeOut.timeReceiverMatched, mpMatchingAttributes.getTimeReceiverMatched(),timeFormat);
		add(NodeOut.totalDonorWaitingTime,mpMatchingAttributes.getTotalDonorWaitingTime(),timeDifferenceFormat);
		add(NodeOut.totalReceiverWaitingTime,mpMatchingAttributes.getTotalReceiverWaitingTime(),timeDifferenceFormat);
		
	}
	
	private <A> void add(NodeOut name, Attribute<V,A> attribute, CsvFormat<A> format){
		this.attributeMap.put(name, new CompositeAttributeConverter<V,A>(attribute,format));
	}
	
	public EnumMap<NodeOut, AttributeConverter<V>> getAttributeMap() {
		return attributeMap;
	}

	public NodeOutputAttributes<V, E, T, D> getMpMatchingAttributes() {
		return mpMatchingAttributes;
	}

	@Override
	public AttributeConverter<V> get(NodeOut attributeName) {
		if(!this.attributeMap.containsKey(attributeName)){
			throw new UnsupportedOperationException();
		}
		return this.attributeMap.get(attributeName);	
	}
	
	

}
