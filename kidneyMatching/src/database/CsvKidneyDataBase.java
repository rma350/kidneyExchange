package database;

import java.io.File;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import multiPeriod.TimeInstant;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import kepLib.KepProblemData;
import kepModeler.AuxiliaryInputStatistics;
import kepModeler.ModelerInputs;

import replicator.DonorEdge;
import data.DataCleaner;
import data.Donor;
import data.ExchangeUnit;
import data.MedicalMatch;
import data.ProblemData;
import data.Receiver;
import data.DataCleaner.ReasonToCleanDonor;
import data.DataCleaner.ReasonToCleanReceiver;
import data.DataCleaner.ReasonsInvalid;
import data.MedicalMatch.Incompatability;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class CsvKidneyDataBase implements KidneyDataBase{
	
	
	
	private static ImmutableList<String> feasibleDateString = ImmutableList.of("20120524","20120402","testDatabase1");
	private static ImmutableSet<String> feasibleDateStringSet = ImmutableSet.copyOf(feasibleDateString);
	private static ImmutableSet<String> testCases = ImmutableSet.of("testDatabase1");
	
	
	private boolean allowSelfMatches = true;
	
	
	public CsvKidneyDataBase(){	}
	
	private static DateTimeFormatter jodaFormat = DateTimeFormat.forPattern("yyyyMMdd");

	@Override
	public ModelerInputs<ExchangeUnit, DonorEdge> loadInputs(String datasetName) {
		ProblemData problemData;
		DateTime currentTime;
		if(testCases.contains(datasetName)){
			String fileName = "unitTestData" + File.separator + "csvDatabase" + File.separator + datasetName+".csv";
			currentTime = jodaFormat.parseDateTime("20130327");
			problemData = new ProblemData(fileName, currentTime);
		}
		else if(feasibleDateStringSet.contains(datasetName)){
			String dateString = datasetName;
			problemData = new ProblemData(dateString,false);
			currentTime = problemData.getDataDate();
			Map<ExchangeUnit,ReasonsInvalid> removed = DataCleaner.cleanProblemData(problemData, EnumSet.allOf(ReasonToCleanDonor.class), EnumSet.allOf(ReasonToCleanReceiver.class));
		}
		else{
			throw new RuntimeException("Ilegal input, dataset not found: " + datasetName + " in " + feasibleDateString);
		}
		
		//DataCleaner.assessHistoricMatching(problemData,removed);
		DirectedSparseMultigraph<ExchangeUnit,DonorEdge> graph = new DirectedSparseMultigraph<ExchangeUnit,DonorEdge>();
		
		Set<ExchangeUnit> roots = new HashSet<ExchangeUnit>(problemData.getAltruisticDonors().values());
		Set<ExchangeUnit> paired = new HashSet<ExchangeUnit>(problemData.getPairedReceivers().values());
		Set<ExchangeUnit> terminal = new HashSet<ExchangeUnit>(problemData.getChips().values());
		
		for(ExchangeUnit unit: problemData.getExchangeUnits()){
			graph.addVertex(unit);
		}
		for(ExchangeUnit donorNode: problemData.getExchangeUnits()){
			if(!terminal.contains(donorNode)){
				for(ExchangeUnit receiverNode: problemData.getExchangeUnits()){
					if(donorNode != receiverNode || this.allowSelfMatches){
						if(!roots.contains(receiverNode)){						
							Receiver receiver = receiverNode.getReceiver();
							for(Donor donor: donorNode.getDonor()){
								if(!problemData.getHardBlockedExchanges().get(receiver).contains(donor) &&
										isMatchFeasible(donor,receiver,currentTime)){
									DonorEdge edge = new DonorEdge(donor);
									graph.addEdge(edge, donorNode, receiverNode);									
									break;
								}
							}							
						}
					}
				}
			}
		}
		KepProblemData<ExchangeUnit,DonorEdge> kepProblemData = new KepProblemData<ExchangeUnit,DonorEdge>(graph,roots,paired,terminal);
		return ModelerInputs.newModelerInputs(kepProblemData);
	}
	
	
	private static boolean isMatchFeasible(Donor donor, Receiver receiver, DateTime date){
		EnumSet<Incompatability> transplantIncompatabilities = MedicalMatch.instance.match(donor,receiver,date);
		return transplantIncompatabilities.size() == 0;
	}


	@Override
	public ImmutableList<String> availableDatasets() {
		return feasibleDateString;
	}

}
