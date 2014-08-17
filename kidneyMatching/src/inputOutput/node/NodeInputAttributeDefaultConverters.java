package inputOutput.node;

import inputOutput.Attributes;
import inputOutput.Attributes.NodeIn;
import inputOutput.core.Attribute;
import inputOutput.core.AttributeConverter;
import inputOutput.core.CompositeAttributeConverter;
import inputOutput.core.Converters;
import inputOutput.core.CsvFormat;
import inputOutput.core.CsvFormatUtil;


import java.util.EnumMap;

import multiPeriod.TimeInstant;



public class NodeInputAttributeDefaultConverters<V,E,T extends Comparable<T>> implements Converters<NodeIn,V>{
	
	private EnumMap<NodeIn,AttributeConverter<V>> attributeMap;
	private NodeInputAttributes<V,E,T> inputAttributes;
	private CsvFormat<TimeInstant<T>> timeFormatter;
	
	public NodeInputAttributeDefaultConverters(NodeInputAttributes<V,E,T> inputAttributes,CsvFormat<TimeInstant<T>> timeFormatter){
		this.inputAttributes = inputAttributes;
		this.attributeMap = new EnumMap<NodeIn,AttributeConverter<V>>(NodeIn.class);
		add(NodeIn.donorPowerPostPreferences, inputAttributes.getDonorPowerPostPreferences(),CsvFormatUtil.toStringNullBlank);
		add(NodeIn.pairMatchPowerPostPreferences, inputAttributes.getPairMatchPowerPostPreferences(),CsvFormatUtil.toStringNullBlank);
		add(NodeIn.receiverPowerPostPreferences, inputAttributes.getReceiverPowerPostPreferences(),CsvFormatUtil.toStringNullBlank);
		add(NodeIn.timeNodeArrived,inputAttributes.getTimeNodeArrived(),timeFormatter);//CsvFormatUtil.mmddyyyyFormatTimeInstant);		
	}
	
	private <A> void add(NodeIn name, Attribute<V,A> attribute, CsvFormat<? super A> formatter  ){
		attributeMap.put(name, new CompositeAttributeConverter<V,A>(attribute,formatter));
	}

	public EnumMap<NodeIn, AttributeConverter<V>> getAttributeMap() {
		return attributeMap;
	}

	@Override
	public AttributeConverter<V> get(NodeIn attributeName) {
		if(!this.attributeMap.containsKey(attributeName)){
			throw new UnsupportedOperationException();
		}
		return this.attributeMap.get(attributeName);		
	}
	
	

}
