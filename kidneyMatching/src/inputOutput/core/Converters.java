package inputOutput.core;

import inputOutput.Attributes.AttributeName;

public interface Converters<A extends AttributeName,B> {
	
	public AttributeConverter<B> get(A attributeName);
	

}
