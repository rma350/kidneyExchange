package inputOutput.nkrFormat;

import inputOutput.core.AttributeConverter;
import inputOutput.core.CompositeAttributeConverter;
import inputOutput.core.CsvFormatUtil;


import java.util.EnumMap;
import java.util.List;

import data.Donor;
import data.ExchangeUnit;
import data.ExchangeUnit.ExchangeUnitType;
import data.ProblemData.ColumnName;

public class NkrExchangeInputAttributeConverters {
	
	public static EnumMap<ColumnName,AttributeConverter<ExchangeUnit>> receiverExchangeUnitAttributeDefault;
	static{
		receiverExchangeUnitAttributeDefault = new EnumMap<ColumnName,AttributeConverter<ExchangeUnit>>(ColumnName.class);
		//receiverExchangeUnitAttributeDefault.put(ColumnName.TYPE, 
				//new CompositeAttributeConverter<ExchangeUnit,ExchangeUnitType>(
						//ExchangeUnitAttribute.exchangeUnitType,CsvFormatUtil.receiverType));
		//receiverExchangeUnitAttributeDefault.put(ColumnName.RELATED_DONORS, 
				//new CompositeAttributeConverter<ExchangeUnit, List<? extends Donor>>(
				//ExchangeUnitAttribute.donors,CsvFormatUtil.personListNameFormat));
		//receiverExchangeUnitAttributeDefault.put(ColumnName.CHIP, new CompositeAttributeConverter<ExchangeUnit,Boolean>(
				//ExchangeUnitAttribute.isChip,CsvFormatUtil.yesNo));
	}

}
