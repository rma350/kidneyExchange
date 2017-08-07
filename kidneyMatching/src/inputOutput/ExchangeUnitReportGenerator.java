package inputOutput;

import inputOutput.Attributes.AttributeName;
import inputOutput.Attributes.ExchangeIn;
import inputOutput.Attributes.NodeOut;
import inputOutput.Attributes.NodeIn;
import inputOutput.core.AttributeConverter;
import inputOutput.node.exchangeUnit.ExchangeOutputAttributeDefaultConverters;
import inputOutput.node.exchangeUnit.ExchangeOutputAttributes;
import inputOutput.node.exchangeUnit.PersonInputAttributeDefaultConverters;
import inputOutput.node.exchangeUnit.ReceiverInputAttributeDefaultConverters;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.joda.time.Interval;
import org.joda.time.ReadableInstant;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import multiPeriod.MultiPeriodCyclePacking;

import data.ProblemData;
import data.ProblemData.ColumnName;
import data.ExchangeUnit;
import replicator.DonorEdge;

/*
private ListMultimap<ColumnName,AttributeConverter<ExchangeUnit>> donorAttributes;
	private EnumMap<ColumnName,AttributeConverter<ExchangeUnit>> receiverAttributes;
	
	donorAttributes = ArrayListMultimap.create();
		receiverAttributes = new EnumMap<ColumnName,AttributeConverter<ExchangeUnit>>(ColumnName.class);
		
		for(ColumnName donorColumn: donorSelected){			
			if(PersonOut.columnToAttributeDefault.containsKey(donorColumn)){
				for(int i = 0; i < this.numDonors; i++){
					donorAttributes.put(donorColumn, donorToUnit(PersonOut.columnToAttributeDefault.get(donorColumn),i));
				}
			}
			else if(DonorOut.donorExchangeUnitAttributeDefault.containsKey(donorColumn)){
				for(int i = 0; i < this.numDonors; i++){
					donorAttributes.put(donorColumn, donorToUnit(DonorOut.donorExchangeUnitAttributeDefault.get(donorColumn),i));
				}
			}
			else{
				throw new RuntimeException();
			}
		}
		for(ColumnName receiverColumn: receiverSelected){
			if(PersonOut.columnToAttributeDefault.containsKey(receiverColumn)){
				receiverAttributes.put(receiverColumn, receiverToUnit(PersonOut.columnToAttributeDefault.get(receiverColumn)));
			}
			else if(ReceiverOut.columnToReceiverAttributeDefault.containsKey(receiverColumn)){
				receiverAttributes.put(receiverColumn, receiverToUnit(ReceiverOut.columnToReceiverAttributeDefault.get(receiverColumn)));
			}
			else{
				throw new RuntimeException("cannot find attribute: " + receiverColumn);
			}
		}
	
	
 * 
 * 
 */
@Deprecated
public class ExchangeUnitReportGenerator {
	
	private List<ColumnName> inputColumnsDonor;
	private List<ColumnName> inputColumnsReceiver;
	private List<Attributes.AttributeName> attributeNames;
	
	private int numDonorsPerNode;
	private int numColumns;
	
	private ExchangeOutputAttributeDefaultConverters exchangeUnitAttributeOut;
	private List<ExchangeUnit> exchangeUnits;
	
	
	/*public ExchangeUnitReportGenerator(List<ColumnName> inputColumnsDonor,
			List<ColumnName> inputColumnsReceiver,
			List<Attributes.AttributeName> attributeNames, int numDonorsPerNode,
			MultiPeriodCyclePacking<ExchangeUnit,DonorEdge,ReadableInstant> mpCyclePacking) {
		this(inputColumnsDonor,inputColumnsReceiver,attributeNames,numDonorsPerNode,new ExchangeUnitMmpAttribute(mpCyclePacking,numDonorsPerNode),
				new ArrayList<ExchangeUnit>(mpCyclePacking.getInputs().getGraph().getVertices()));
		
	}*/
	
	public static List<Attributes.AttributeName> allNames(){
		List<Attributes.AttributeName> ans = new ArrayList<Attributes.AttributeName>();
		ans.addAll(Arrays.asList(Attributes.ExchangeIn.values()));
		ans.addAll(Arrays.asList(Attributes.NodeOut.values()));
		return ans;
	}
	
	
	/*public ExchangeUnitReportGenerator(List<ColumnName> inputColumnsDonor,
			List<ColumnName> inputColumnsReceiver,
			List<Attributes.AttributeName> attributeNames, int numDonorsPerNode,
			MultiPeriodCyclePacking<ExchangeUnit,DonorEdge,ReadableInstant> mpCyclePacking,List<ExchangeUnit> exchangeUnits) {
		this(inputColumnsDonor,inputColumnsReceiver,attributeNames,numDonorsPerNode,new ExchangeUnitMmpAttribute(mpCyclePacking,numDonorsPerNode),exchangeUnits);
		
	}*/
	
	private static EnumSet<Attributes.NodeOut> makeNodeAttributeNames(List<Attributes.AttributeName> attributeNameList){
		EnumSet<Attributes.NodeOut> ans = EnumSet.noneOf(Attributes.NodeOut.class);
		for(Attributes.AttributeName attribute: attributeNameList){
			if(attribute instanceof Attributes.NodeOut){
				ans.add((Attributes.NodeOut)attribute);
			}
		}
		return ans;
	}
	
	private static EnumSet<Attributes.NodeIn> makeNodeInputAttributeNames(List<Attributes.AttributeName> attributeNameList){
		EnumSet<Attributes.NodeIn> ans = EnumSet.noneOf(Attributes.NodeIn.class);
		for(Attributes.AttributeName attribute: attributeNameList){
			if(attribute instanceof Attributes.NodeIn){
				ans.add((Attributes.NodeIn)attribute);
			}
		}
		return ans;
	}
	
	private static EnumSet<Attributes.ExchangeIn> makeExchangeUnitAttributeNames(List<Attributes.AttributeName> attributeNameList){
		EnumSet<Attributes.ExchangeIn> ans = EnumSet.noneOf(Attributes.ExchangeIn.class);
		for(Attributes.AttributeName attribute: attributeNameList){
			if(attribute instanceof Attributes.ExchangeIn){
				ans.add((Attributes.ExchangeIn)attribute);
			}
		}
		return ans;
	}
	
	/*public ExchangeUnitReportGenerator(List<ColumnName> inputColumnsDonor,
			List<ColumnName> inputColumnsReceiver,
			List<Attributes.AttributeName> attributeNames, int numDonorsPerNode,
			ExchangeUnitMmpAttribute exchangeUnitMmpAttribute,List<ExchangeUnit> exchangeUnits) {
		this(inputColumnsDonor,inputColumnsReceiver,attributeNames,numDonorsPerNode,
				new ExchangeUnitAttributeOut(exchangeUnitMmpAttribute,
						new HashSet<Attributes.AttributeName>(attributeNames),
						EnumSet.copyOf(inputColumnsDonor),
						EnumSet.copyOf(inputColumnsReceiver),makeExchangeUnitAttributeNames(attributeNames),
						numDonorsPerNode),
						exchangeUnits);
	}*/
	
	
	
	public ExchangeUnitReportGenerator(List<ColumnName> inputColumnsDonor,
			List<ColumnName> inputColumnsReceiver,
			List<Attributes.AttributeName> attributeNames, int numDonorsPerNode,
			ExchangeOutputAttributeDefaultConverters exchangeUnitAttributeOut, List<ExchangeUnit> exchangeUnits) {
		this.inputColumnsDonor = inputColumnsDonor;
		this.inputColumnsReceiver = inputColumnsReceiver;
		this.attributeNames = attributeNames;
		this.numDonorsPerNode = numDonorsPerNode;
		this.numColumns = attributeNames.size() + inputColumnsReceiver.size() + inputColumnsDonor.size()*numDonorsPerNode;
		this.exchangeUnitAttributeOut = exchangeUnitAttributeOut;
		this.exchangeUnits = exchangeUnits;
	}
	
	public void writeReport(String fileName, boolean printFirstRow){
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL);
			if(printFirstRow){
				String[] firstRow = new String[this.numColumns];			
				int col = 0;
				for(Attributes.AttributeName nodeAttribute: attributeNames){
					firstRow[col++] = nodeAttribute.getDefaultName();
				}
				for(ColumnName columnName: this.inputColumnsReceiver){
					firstRow[col++] = "R " + ProblemData.defaultColumnNamesHistoricReversed.get(columnName);
				}
				for(int i = 0; i < this.numDonorsPerNode; i++){
					for(ColumnName columnName : this.inputColumnsDonor){
						firstRow[col++] = "D"+i +" " + ProblemData.defaultColumnNamesHistoricReversed.get(columnName);
					}				
				}
				if(col != this.numColumns){
					throw new RuntimeException();
				}
				printer.printRecord(firstRow);
			}
			for(ExchangeUnit unit: this.exchangeUnits){
				String[] nextRow = new String[this.numColumns];			
				int col = 0;
				for(Attributes.AttributeName attributeName: attributeNames){
					//nextRow[col++] = this.exchangeUnitAttributeOut.getAttributeConverter(attributeName).getValueFormatted(unit);
				}
				for(ColumnName columnName: this.inputColumnsReceiver){
					//nextRow[col++] = this.exchangeUnitAttributeOut.getReceiverAttributes().get(columnName).getValueFormatted(unit);
				}
				for(int i = 0; i < Math.min(this.numDonorsPerNode,unit.getDonor().size()); i++){
					for(ColumnName columnName : this.inputColumnsDonor){
						//nextRow[col++] = this.exchangeUnitAttributeOut.getDonorAttributes().get(columnName).get(i).getValueFormatted(unit);
					}				
				}
				printer.printRecord(nextRow);
			}
			printer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
}
