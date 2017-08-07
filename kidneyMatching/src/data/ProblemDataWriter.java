package data;

import inputOutput.core.AttributeConverter;
import inputOutput.node.exchangeUnit.PersonInputAttributeDefaultConverters;
import inputOutput.node.exchangeUnit.ReceiverInputAttributeDefaultConverters;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;

import data.ExchangeUnit.ExchangeUnitType;
import data.ProblemData.ColumnName;

public class ProblemDataWriter {
	
	private ProblemData data;
	private DataFormat format;
	private CSVPrinter printer;
	private Set<ColumnName> columnsToWrite;
	private EnumMap<ColumnName,AttributeConverter<Receiver>> receiverAttributeConverter;
	private EnumMap<ColumnName,AttributeConverter<Donor>> donorAttributeConverter;
	private EnumMap<ColumnName,AttributeConverter<Person>> personAttributeConverter;
	
	private EnumMap<ColumnName,AttributeConverter<ExchangeUnit>> donorExchangeUnitAttributes;
	
	private EnumMap<ColumnName,AttributeConverter<ExchangeUnit>> receiverExchangeUnitAttributes;
	
	

	
	public static enum DataFormat{
		SNAPSHOT(ProblemData.defaultColumnNamesHistoricReversed, ProblemData.defaultColumnNamesHistoricToIndex);
		
		private int numColumns;
		private Map<ColumnName,Integer> columnPositions;
		private Map<ColumnName,String> columnNames;
		
		
		private DataFormat(Map<ColumnName,String> columnNames, Map<ColumnName,Integer> columnPositions){
			this.columnNames = columnNames;
			this.columnPositions = columnPositions;
			this.numColumns = columnPositions.size();
		}


		public int getNumColumns() {
			return numColumns;
		}


		public Map<ColumnName, Integer> getColumnPositions() {
			return columnPositions;
		}


		public Map<ColumnName, String> getColumnNames() {
			return columnNames;
		}
		
		
	}
	
	//public ProblemDataWriter(ProblemData data, String fileName, DataFormat format, Set<ColumnName> columnsToWrite){
	//	this(data,fileName,format,columnsToWrite,ReceiverOut.columnToReceiverAttributeDefault,
	//			PersonOut.columnToAttributeDefault, DonorOut.donorExchangeUnitAttributeDefault,
	//			ReceiverOut.receiverExchangeUnitAttributeDefault);
	//}
	
	public ProblemDataWriter(ProblemData data, String fileName, DataFormat format, Set<ColumnName> columnsToWrite,
			EnumMap<ColumnName,AttributeConverter<Receiver>> receiverAttributeConverter, 
			EnumMap<ColumnName,AttributeConverter<Person>> personAttributeConverter,
			EnumMap<ColumnName,AttributeConverter<ExchangeUnit>> donorExchangeUnitAttributes,
			EnumMap<ColumnName,AttributeConverter<ExchangeUnit>> receiverExchangeUnitAttributes){
		this.data = data;
		this.format = format;
		this.columnsToWrite = columnsToWrite;
		this.receiverAttributeConverter = receiverAttributeConverter;
		this.personAttributeConverter = personAttributeConverter;
		this.donorExchangeUnitAttributes = donorExchangeUnitAttributes;
		this.receiverExchangeUnitAttributes = receiverExchangeUnitAttributes;
		try {
			
			printer = new CSVPrinter(new BufferedWriter(new FileWriter(fileName)),CSVFormat.EXCEL);
			String[] firstRow = new String[format.getNumColumns()];
			Map<Integer,ColumnName> columnNameToPositionReversed = new HashMap<Integer,ColumnName>();
			for(ColumnName name: format.getColumnPositions().keySet()){
				columnNameToPositionReversed.put(format.getColumnPositions().get(name), name);
			}
			for(int i = 0; i < format.getNumColumns(); i++){
				firstRow[i]=format.getColumnNames().get(columnNameToPositionReversed.get(i));
			}
			printer.printRecord(firstRow);
			for(Donor donor: data.getAltruisticDonors().keySet()){
				printDonor(donor,data.getAltruisticDonors().get(donor));
			}
			for(Donor donor: data.getPairedDonors().keySet()){
				printDonor(donor,data.getPairedDonors().get(donor));
			}
			for(Receiver receiver: data.getPairedReceivers().keySet()){
				printReceiver(receiver,data.getPairedReceivers().get(receiver));
			}
			for(Receiver receiver: data.getChips().keySet()){
				printReceiver(receiver,data.getChips().get(receiver));
			}
			printer.printRecord("--XXMY_o_YMXX--");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private void printDonor(Donor donor, ExchangeUnit exchangeUnit){
		String[] row = new String[this.format.getNumColumns()];
		for(ColumnName name: this.columnsToWrite){
			int position = this.format.getColumnPositions().get(name).intValue();
			String value = getDonorValueOf(donor,exchangeUnit,name);
			row[position] = value;
		}
		try {
			this.printer.printRecord(row);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/*private static String formatSpecialHlaTissueType(Person person, SpecialHla speicalHlaType){
		if(person.getTissueType().getSpecialHla().containsKey(speicalHlaType)){
			return person.getTissueType().getSpecialHla().get(speicalHlaType).booleanValue() ? "" + 1 : ""; 
		}
		else{
			return "";
		}
	}
	
	private static String formatAlleleLo(Person person, HlaType hlaType){
		if(person.getTissueType().getHlaTypes().containsKey(hlaType)){
			return ""+person.getTissueType().getHlaTypes().get(hlaType).getAlleleLo();
		}
		else{
			return "";
		}
	}
	
	private static String formatAlleleHi(Person person, HlaType hlaType){
		if(person.getTissueType().getHlaTypes().containsKey(hlaType)){
			Genotype genotype = person.getTissueType().getHlaTypes().get(hlaType);
			if(genotype.getAlleleHi()== genotype.getAlleleLo()){
				return ""+ (-1);
			}
			else{
				return "" + genotype.getAlleleHi();
			}
		}
		else{
			return "";
		}
	}*/
	
	private String getDonorValueOf(Donor donor, ExchangeUnit exchangeUnit, ColumnName columnName){
		if(receiversOnly.contains(columnName)){
			return "";
		}
		else if(personPrint.contains(columnName)){
			return getPersonValueOf(donor,exchangeUnit,columnName);
		}
		else if(columnName == ColumnName.TYPE){
			return this.donorExchangeUnitAttributes.get(columnName).apply(exchangeUnit);
		}
		else if(donorsOnly.contains(columnName)){
			if(this.personAttributeConverter.containsKey(columnName)){
				return this.personAttributeConverter.get(columnName).apply(donor);
			}
			//else if(this)
			if(columnName == ColumnName.CREATINE){
				 return "";
			}
			/*else if(columnName == ColumnName.HLA_Bw1){
				 return formatAlleleLo(donor,HlaType.Bw);
			}
			else if(columnName == ColumnName.HLA_Bw2){
				return formatAlleleHi(donor,HlaType.Bw);
			}
			else if(columnName == ColumnName.HLA_Cw1){
				return formatAlleleLo(donor,HlaType.Cw);
			}
			else if(columnName == ColumnName.HLA_Cw2){
				return formatAlleleHi(donor,HlaType.Cw);
			}
			else if(columnName == ColumnName.HLA_DQ1){
				return formatAlleleLo(donor,HlaType.DQ);
			}
			else if(columnName == ColumnName.HLA_DQ2){
				return formatAlleleHi(donor,HlaType.DQ);
			}
			
			else if(columnName == ColumnName.HLA_DP1){
				return formatAlleleLo(donor,HlaType.DP);
			}
			else if(columnName == ColumnName.HLA_DP2){
				return formatAlleleHi(donor,HlaType.DP); 
			}
			else if(columnName == ColumnName.HLA_DR_51){
				return formatSpecialHlaTissueType(donor,SpecialHla.DR51);
			}
			else if(columnName == ColumnName.HLA_DR_52){
				return formatSpecialHlaTissueType(donor,SpecialHla.DR52); 
			}
			else if(columnName == ColumnName.HLA_DR_53){
				return formatSpecialHlaTissueType(donor,SpecialHla.DR53);
			}*/
			else if(columnName == ColumnName.DONOR_TRAVEL_CENTERS_IF_RESTRICTED){
				 return "";
			}
			else if(columnName == ColumnName.DONOR_TRAVEL_CENTERS_IF_RESTRICTED_AND_UNLISTED){
				return "";
			}
			else if(columnName == ColumnName.DONOR_WORKED_UP){
				 return "";
			}
			else if(columnName == ColumnName.DONOR_A_SUBTYPE){
				 return "";
			}
			else {
				throw new RuntimeException();
			}
		}
		else{
			return "";
		}
	}
	
	private void printReceiver(Receiver receiver, ExchangeUnit exchangeUnit){
		String[] row = new String[this.format.getNumColumns()];
		for(ColumnName name: this.columnsToWrite){
			int position = this.format.getColumnPositions().get(name).intValue();
			String value = getReceiverValueOf(receiver,exchangeUnit,name);
			row[position] = value;
		}
		try {
			this.printer.printRecord(row);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	/*
	private String printAnitbodies(Receiver rec, HlaType hla){
		String ans = "";
		if(rec.getTissueTypeSensitivity().getAntibodies().containsKey(hla)){
			int[] antibodies = rec.getTissueTypeSensitivity().getAntibodies().get(hla);
			for(int i = 0; i < antibodies.length; i++){
				ans+=antibodies[i];
				if(i < antibodies.length-1){
					ans+= "|";
				}
			}
		}
		return ans;
	}
	
	private String printSpecialAntibodies(Receiver rec,SpecialHla specialHla){
		String ans ="";
		if(rec.getTissueTypeSensitivity().getAvoidsSpecialAntibodies().containsKey(specialHla)){
			if(rec.getTissueTypeSensitivity().getAvoidsSpecialAntibodies().get(specialHla).booleanValue()){
				ans+=1;
			}
		}
		return ans;
	}
	*/
	/*private static String peopleToString(Iterable<? extends Person> people){
		String ans ="";
		Iterator<? extends Person> iterator = people.iterator();
		while(iterator.hasNext()){
			ans+= iterator.next().getId();
			if(iterator.hasNext()){
				ans+="|";
			}
		}
		return ans;
	}*/
	
	private String getReceiverValueOf(Receiver receiver, ExchangeUnit exchangeUnit, ColumnName columnName){
		if(donorsOnly.contains(columnName)){
			return "";
		}
		else if(personPrint.contains(columnName)){
			return getPersonValueOf(receiver,exchangeUnit,columnName);
		}
		else if(receiversOnly.contains(columnName)){
			if(this.receiverExchangeUnitAttributes.containsKey(columnName)){
				return this.receiverExchangeUnitAttributes.get(columnName).apply(exchangeUnit);
			}
			else if(this.receiverAttributeConverter.containsKey(columnName)){
				return this.receiverAttributeConverter.get(columnName).apply(receiver);
			}
			else if(columnName == ColumnName.RECIPIENT_NON_A1_TITER){
				return "";
			}
			else if(columnName == ColumnName.MIN_CREATINE){
				return "";
			}
			else if(columnName == ColumnName.HARD_BLOCKED_DONORS){
				return "";//peopleToString(data.getHardBlockedExchanges().get(receiver));
			}
			else{
				throw new RuntimeException();
			}
		}
		else if(columnName == ColumnName.TYPE){
			return this.receiverExchangeUnitAttributes.get(columnName).apply(exchangeUnit);
		}
		else{
			return "";
		}
	}
	
	private String getPersonValueOf(Person person,ExchangeUnit exchangeUnit,ColumnName columnName){
		if(this.personAttributeConverter.containsKey(columnName)){
			return this.personAttributeConverter.get(columnName).apply(person);
		}
		else if(columnName == ColumnName.CHAIN_CLUSTER_POSITION_STATUS){
			return "";
		}
		else if(columnName == ColumnName.CHAIN_STATUS){
			return "";
		}
		else{
			throw new RuntimeException();
		}
	}
	

	
	private static EnumSet<ColumnName> donorsOnly = EnumSet.of(ColumnName.CREATINE, ColumnName.HLA_Bw1, ColumnName.HLA_Bw2, 
			ColumnName.HLA_Cw1, ColumnName.HLA_Cw2, ColumnName.HLA_DQ1,	ColumnName.HLA_DQ2, ColumnName.DONOR_TRAVEL_CENTERS_IF_RESTRICTED,
			ColumnName.DONOR_TRAVEL_CENTERS_IF_RESTRICTED_AND_UNLISTED,ColumnName.HLA_DP1,ColumnName.HLA_DP2,ColumnName.HLA_DR_51,ColumnName.HLA_DR_52, ColumnName.HLA_DR_53,
			ColumnName.DONOR_WORKED_UP, ColumnName.DONOR_A_SUBTYPE);
	private static EnumSet<ColumnName> receiversOnly = EnumSet.of(ColumnName.RELATED_DONORS,ColumnName.AVOIDS_A, ColumnName.AVOIDS_B, 
			ColumnName.AVOIDS_DR, ColumnName.MIN_DONOR_AGE, ColumnName.MAX_DONOR_AGE, ColumnName.MIN_HLA_MATCH,ColumnName.MIN_CREATINE,
			ColumnName.AVOIDS_BW, ColumnName.AVOIDS_CW, ColumnName.AVOIDS_DQ,ColumnName.ACCEPT_SHIPPED_KIDNEY,ColumnName.WILLING_TO_TRAVEL,
			ColumnName.HARD_BLOCKED_DONORS,ColumnName.AVOIDS_DP, ColumnName.AVOIDS_DR_51, ColumnName.AVOIDS_DR_52, ColumnName.AVOIDS_DR_53,
			ColumnName.RECIPIENT_NON_A1_TITER, ColumnName.MIN_DONOR_WEIGHT,ColumnName.CHIP );
	
	private static EnumSet<ColumnName> personPrint = EnumSet.of(ColumnName.CENTER_ID,
			ColumnName.PERSON_ID, ColumnName.DATE_REGISTERED,ColumnName.ACTIVE, ColumnName.BIRTH_YEAR, ColumnName.GENDER,
			ColumnName.RACE, ColumnName.BLOOD_TYPE, ColumnName.HEIGHT_CM, ColumnName.WEIGHT_KG, ColumnName.HLA_A1, ColumnName.HLA_A2,
			ColumnName.HLA_B1, ColumnName.HLA_B2, ColumnName.HLA_DR1, ColumnName.HLA_DR2,ColumnName.CHAIN_CLUSTER_POSITION_STATUS, ColumnName.CHAIN_STATUS);

}
