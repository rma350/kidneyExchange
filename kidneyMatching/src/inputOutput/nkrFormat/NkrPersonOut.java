package inputOutput.nkrFormat;

import data.Genotype;
import data.HlaType;
import data.Person;
import inputOutput.core.AttributeConverter;
import inputOutput.core.CompositeAttributeConverter;
import inputOutput.core.CsvFormat;
import inputOutput.core.CsvFormatUtil;
import inputOutput.node.exchangeUnit.PersonInputAttributes;
import inputOutput.node.exchangeUnit.PersonInputAttributeDefaultConverters;
import inputOutput.node.exchangeUnit.PersonInputAttributes.HlaTypeAttribute;

public class NkrPersonOut extends PersonInputAttributeDefaultConverters{

	public NkrPersonOut(PersonInputAttributes personInputAttributes) {
		super(personInputAttributes);		
	}
	
	@Override
	protected CsvFormat<? super Boolean> getSpecialHlaFormat(){
		return CsvFormatUtil.specialHlaFormat;
	}
	
	@Override
	protected AttributeConverter<Person> getHlaConverter(HlaType type, boolean isLow){		 
		if(isLow){
			HlaTypeAttribute attribute = getInputAttributes().getHlaLow().get(type);
			return new CompositeAttributeConverter<Person,Integer>(attribute,CsvFormatUtil.toStringNullBlank);
		}
		else{
			HlaTypeAttribute attribute = getInputAttributes().getHlaHigh().get(type);
			return makeHlaHighConverter(type);
		}
		
		
	}
	
	private AttributeConverter<Person> makeHlaHighConverter(final HlaType type){
		return new AttributeConverter<Person>(){
			@Override
			public String apply(Person input) {
				Integer hlaLo = getInputAttributes().getHlaLow().get(type).apply(input);
				Integer hlaHi = getInputAttributes().getHlaHigh().get(type).apply(input);
				if(hlaLo != null && hlaHi != null){
					if(hlaLo.equals(hlaHi)){
						return "-1";
					}
					else{
						return Integer.toString(hlaHi);
					}
				}
				else{
					return "";
				}				
			}
			
		};
			
	}

}
