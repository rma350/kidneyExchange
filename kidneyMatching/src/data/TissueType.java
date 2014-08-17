package data;

import java.util.EnumMap;

public class TissueType {
	
	private EnumMap<HlaType,Genotype> hlaTypes;
	private EnumMap<SpecialHla,Boolean> specialHla;
	
	public TissueType(){
		hlaTypes = new EnumMap<HlaType,Genotype>(HlaType.class);
		specialHla = new EnumMap<SpecialHla,Boolean>(SpecialHla.class);
	}

	public EnumMap<HlaType, Genotype> getHlaTypes() {
		return hlaTypes;
	}

	public EnumMap<SpecialHla, Boolean> getSpecialHla() {
		return specialHla;
	}

}
