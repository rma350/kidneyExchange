package statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import jFreeChart.DegreeHistogram;
import jFreeChart.DegreeHistogram.HistType;
import kepLib.KepProblemData;
import kepModeler.AuxiliaryInputStatistics;
import kepModeler.ExchangeUnitAuxiliaryInputStatistics;


import multiPeriod.MultiPeriodCyclePacking;
import multiPeriod.MultiPeriodCyclePacking.EffectiveNodeType;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.joda.time.ReadableInstant;

import replicator.DonorEdge;

import com.google.common.collect.ImmutableClassToInstanceMap.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


import data.Donor;
import data.ExchangeUnit;
import data.Receiver;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import graphUtil.SubgraphUtil;

/**
 * Use input attributes instead.  TODO: convert everything in this class so it is calculated as an input attribute
 * @author ross
 *
 */
@Deprecated
public class Queries {
	
	private static String dir = "output" + File.separator + "queries" + File.separator; 
	private static String inputDir = "input";
	
	private static String csv = ".csv";
	
	
	
	
	private Queries(){}
	
	// TODO(ross): kill this, its just a hack, only need kepLib.kepProblemData, 
	// this is just a hack due to legacy issues from multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs
	@Deprecated
	public static interface KepProblemDataInterface<V,E>{
		public DirectedSparseMultigraph<V, E> getGraph();
		public Set<V> getRootNodes();
		public Set<V> getTerminalNodes();
	}
	
	
	public static <V,E> AuxiliaryInputStatistics<V,E> getAuxiliaryStatistics(KepProblemDataInterface<V,E> inputs){
		return new AuxiliaryInputStatistics<V,E>(getOutDegreeForObjective(inputs),getInDegreeForObjective(inputs));
	}
	
	// TODO(ross): this should take in a list of who we are comparing againt, not just use the current pool...
	public static ExchangeUnitAuxiliaryInputStatistics getAuxiliaryStatisticsWithVPra(KepProblemData<ExchangeUnit,DonorEdge> kepProblemData){
		AuxiliaryInputStatistics<ExchangeUnit,DonorEdge> regular = getAuxiliaryStatistics(kepProblemData);
		List<Donor> donors = new ArrayList<Donor>();
		List<Receiver> receivers = new ArrayList<Receiver>();
		for(ExchangeUnit unit: kepProblemData.getGraph().getVertices()){
			for(Donor donor: unit.getDonor()){
				donors.add(donor);
			}
			if(!kepProblemData.getRootNodes().contains(unit)){
				receivers.add(unit.getReceiver());
			}
		}		
		ImmutableMap.Builder<Receiver,Double> builder = ImmutableMap.<Receiver,Double>builder();
		for(Receiver receiver: receivers){
			int count = 0;
			for(Donor donor: donors){
				if(!receiver.getTissueTypeSensitivity().isCompatible(donor.getTissueType())){
					count++;
				}
			}
			builder.put(receiver, 100*count/(double)donors.size());
		}
		return new ExchangeUnitAuxiliaryInputStatistics(
				regular.getDonorPowerPostPreference(), 
				regular.getReceiverPowerPostPreference(),
				builder.build()) ;
	}
	
	public static ExchangeUnitAuxiliaryInputStatistics getAuxiliaryStatisticsWithVPra(MultiPeriodCyclePackingInputs<ExchangeUnit,DonorEdge,ReadableInstant> inputs){
		AuxiliaryInputStatistics<ExchangeUnit,DonorEdge> regular = getAuxiliaryStatistics(inputs);
		List<Donor> donors = new ArrayList<Donor>();
		List<Receiver> receivers = new ArrayList<Receiver>();
		for(ExchangeUnit unit: inputs.getGraph().getVertices()){
			for(Donor donor: unit.getDonor()){
				if(donor.getRegistered().isAfter(inputs.getStartTime().getValue())){
					donors.add(donor);
				}
			}
			if(inputs.getEffectiveNodeType(unit) != EffectiveNodeType.chainRoot){
				receivers.add(unit.getReceiver());
			}
		}		
		ImmutableMap.Builder<Receiver,Double> builder = ImmutableMap.<Receiver,Double>builder();
		for(Receiver receiver: receivers){
			int count = 0;
			for(Donor donor: donors){
				if(!receiver.getTissueTypeSensitivity().isCompatible(donor.getTissueType())){
					count++;
				}
			}
			builder.put(receiver, 100*count/(double)donors.size());
		}
		return new ExchangeUnitAuxiliaryInputStatistics(
				regular.getDonorPowerPostPreference(), 
				regular.getReceiverPowerPostPreference(),
				builder.build()) ;
	}
	
	public static <V,E,T extends Comparable<T>> ImmutableMap<V,Double> getOutDegreeForObjective(KepProblemDataInterface<V,E> inputs){
		Set<V> noTerminal = Queries.verticesNoTerminal(inputs);
		DirectedSparseMultigraph<V,E> noTerminalSubgraph = SubgraphUtil.subgraph(inputs.getGraph(),noTerminal,null);
		return Queries.outDegreeAsProbabilityOnSubgraphToMap(inputs, noTerminal, noTerminalSubgraph);
	}
	
	public static <V,E,T extends Comparable<T>> ImmutableMap<V,Double> getInDegreeForObjective(KepProblemDataInterface<V,E> inputs){
		//Set<V> all = new HashSet<V>(inputs.getGraph().getVertices());
		//Set<V> noRoots = new HashSet<V>(all);
		//noRoots.removeAll(inputs.getChainRoots());
		Set<V> paired = Queries.verticesPaired(inputs);
		return Queries.inDegreeAsProbabilityOnSubgraphToMap(inputs, paired,inputs.getGraph());
	}
	
	
	public static void writeData(String path, String fileName, Iterable<Double> values){
		String fullFileName = path + fileName + csv;
		
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fullFileName));
			CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL);
			for(Double value: values){
				printer.print(value.toString());
				printer.println();
			}
			printer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public static <V,E,T extends Comparable<T>> void allInputQueries(String inputName, MultiPeriodCyclePackingInputs<V,E,T> inputs){
		String inputDir = dir + inputName + File.separator + Queries.inputDir + File.separator;
		File file = new File(inputDir);
		file.mkdirs();
		
		Set<V> paired = Queries.verticesPaired(inputs);
		DirectedSparseMultigraph<V,E> pairedOnlySubgraph = SubgraphUtil.subgraph(inputs.getGraph(),paired,null);
		{
			double[] inDegreeDist = inDegreeAsProbabilityOnSubgraph(inputs,paired,pairedOnlySubgraph);
			double[] outDegreeDist = outDegreeAsProbabilityOnSubgraph(inputs,paired,pairedOnlySubgraph);
			String fileName = inputDir+ "paired";
			String titleSuffix = "paired nodes";			
			DegreeHistogram.makeInAndOutDegreeHistogram(fileName, titleSuffix, inDegreeDist, outDegreeDist);
		}
		Set<V> verticesNew = verticesArrivedAfterStart(inputs);
		verticesNew.retainAll(paired);
		{
			double[] inDegreeDist = inDegreeAsProbabilityOnSubgraph(inputs,verticesNew,pairedOnlySubgraph);
			double[] outDegreeDist = outDegreeAsProbabilityOnSubgraph(inputs,verticesNew,pairedOnlySubgraph);
			String fileName = inputDir+ "pairedAndNew";
			String titleSuffix = "paired nodes arriving after start";			
			DegreeHistogram.makeInAndOutDegreeHistogram(fileName, titleSuffix, inDegreeDist, outDegreeDist);
		}
		
		Set<V> pairedOld = new HashSet<V>(paired);
		pairedOld.removeAll(verticesNew);
		{
			double[] inDegreeDist =inDegreeAsProbabilityOnSubgraph(inputs,pairedOld,pairedOnlySubgraph);
			double[] outDegreeDist = outDegreeAsProbabilityOnSubgraph(inputs,pairedOld,pairedOnlySubgraph);
			String fileName = inputDir+ "pairedAndOld";
			String titleSuffix = "paired nodes arriving before start";			
			DegreeHistogram.makeInAndOutDegreeHistogram(fileName, titleSuffix, inDegreeDist, outDegreeDist);
		}
		
		Map<V,Double> receiverPower = Queries.getInDegreeForObjective(inputs);
		Map<V,Double> donorPower = Queries.getOutDegreeForObjective(inputs);
		Map<V,Double> matchingPower = new HashMap<V,Double>();
		for(V vertex: paired){
			matchingPower.put(vertex, receiverPower.get(vertex).doubleValue()*donorPower.get(vertex).doubleValue()*10000);
		}
		writeData(inputDir,"receiverPower",receiverPower.values());
		writeData(inputDir,"donorPower",donorPower.values());
		writeData(inputDir,"matchingPower",matchingPower.values());
		
		
	}
	
	public static <V,E,T extends Comparable<T>> void queryAll(String inputName, String solutionName, Results<V,E,T> results){
		String inputDir = dir + inputName + File.separator + solutionName + File.separator;
		MultiPeriodCyclePackingInputs<V,E,T> inputs = results.getMultiPeriodPacking().getInputs();
		File file = new File(inputDir);
		file.mkdirs();
		Set<V> paired = Queries.verticesPaired(inputs);
		Set<V> matchedPaired = Queries.verticesReceivedEdgeInMatching(results);
		DirectedSparseMultigraph<V,E> pairedOnlySubgraph = SubgraphUtil.subgraph(inputs.getGraph(),paired,null);
		matchedPaired.retainAll(paired);
		{
			double[] inDegreeDist = inDegreeAsProbabilityOnSubgraph(inputs,matchedPaired,pairedOnlySubgraph);
			double[] outDegreeDist = outDegreeAsProbabilityOnSubgraph(inputs,matchedPaired,pairedOnlySubgraph);
			String fileName = inputDir+ "pairedAndMatched";
			String titleSuffix = "paired nodes that were matched";			
			DegreeHistogram.makeInAndOutDegreeHistogram(fileName, titleSuffix, inDegreeDist, outDegreeDist);
		}
		
		
		
	}
	
	private static <V,E,T extends Comparable<T>> int potentialEdgeSources(KepProblemDataInterface<V,E> multiPeriodCyclePacking){
		return multiPeriodCyclePacking.getGraph().getVertexCount() - multiPeriodCyclePacking.getTerminalNodes().size();
	}
	
	private static <V,E,T extends Comparable<T>> int potentialEdgeTargets(KepProblemDataInterface<V,E> multiPeriodCyclePacking){
		return multiPeriodCyclePacking.getGraph().getVertexCount() - multiPeriodCyclePacking.getRootNodes().size();
	}
	
	private static <V,E,T extends Comparable<T>> int potentialEdgeSources(KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			DirectedSparseMultigraph<V,E> subgraph){
		int count = 0;
		for(V v: subgraph.getVertices()){
			if(!multiPeriodCyclePacking.getTerminalNodes().contains(v)){
				count++;
			}
		}
		return count;
	}
	
	private static <V,E,T extends Comparable<T>> int potentialEdgeTargets(KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			DirectedSparseMultigraph<V,E> subgraph){

		int count = 0;
		for(V v: subgraph.getVertices()){
			if(!multiPeriodCyclePacking.getRootNodes().contains(v)){
				count++;
			}
		}
		return count;
	}
	
	private static double[] normalizeToProbability(int[] data, int denominator){
		double[] ans = new double[data.length];
		double denomDouble = denominator;
		for(int i = 0; i < ans.length; i++){
			ans[i] = data[i]/denomDouble;
		}
		return ans;
	}
	
	private static <V> ImmutableMap<V,Double> normalizeMapToProbability(Map<V,Integer> data, int denominator){
		ImmutableMap.Builder<V,Double> ans = ImmutableMap.builder();
		double denomDouble = denominator;
		for(V vertex: data.keySet()){
			ans.put(vertex, Double.valueOf(data.get(vertex).intValue()/denomDouble));
		}
		return ans.build();
	}
	
	public static <V,E,T extends Comparable<T>> int[] inDegree(KepProblemDataInterface<V,E> multiPeriodCyclePacking, Set<V> vertices){
		int[] ans = new int[vertices.size()];
		int i = 0;
		for(V vertex: vertices){
				ans[i++] = multiPeriodCyclePacking.getGraph().inDegree(vertex);				
		}
		return ans;
	}
	
	
	public static <V,E,T extends Comparable<T>> double[] inDegreeAsProbability(KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			Set<V> vertices){
		return normalizeToProbability(inDegree(multiPeriodCyclePacking,vertices),potentialEdgeSources(multiPeriodCyclePacking));
	}
	
	
	public static <V,E,T extends Comparable<T>> int[] inDegreeOnSubgraph(KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			Set<V> vertices, DirectedSparseMultigraph<V,E> subgraph){
		int[] ans = new int[vertices.size()];
		int i = 0;
		for(V vertex: vertices){
			if(!subgraph.containsVertex(vertex)){
				throw new RuntimeException();
			}
			ans[i++] = subgraph.inDegree(vertex);				
		}
		return ans;
	}
	
	public static <V,E,T extends Comparable<T>> Map<V,Integer> inDegreeOnSubgraphToMap(
			KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			Set<V> vertices, DirectedSparseMultigraph<V,E> subgraph){
		Map<V,Integer> ans = new HashMap<V,Integer>();
		for(V vertex: vertices){
			if(!subgraph.containsVertex(vertex)){
				throw new RuntimeException();
			}
			ans.put(vertex,subgraph.inDegree(vertex));				
		}
		return ans;
	}
	
	public static <V,E,T extends Comparable<T>> ImmutableMap<V,Double> inDegreeAsProbabilityOnSubgraphToMap(
			KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			Set<V> vertices, DirectedSparseMultigraph<V,E> subgraph){
		return normalizeMapToProbability(inDegreeOnSubgraphToMap(multiPeriodCyclePacking,vertices,subgraph),
				potentialEdgeSources(multiPeriodCyclePacking,subgraph));
	}
	
	public static <V,E,T extends Comparable<T>> double[] inDegreeAsProbabilityOnSubgraph(KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			Set<V> vertices, DirectedSparseMultigraph<V,E> subgraph){
		return normalizeToProbability(inDegreeOnSubgraph(multiPeriodCyclePacking,vertices,subgraph),potentialEdgeSources(multiPeriodCyclePacking,subgraph));
	}
	
	
	
	
	
	public static <V,E,T extends Comparable<T>> int[] outDegree(KepProblemDataInterface<V,E> multiPeriodCyclePacking, Set<V> vertices){
		int[] ans = new int[vertices.size()];
		int i = 0;
		for(V vertex: vertices){
				ans[i++] = multiPeriodCyclePacking.getGraph().outDegree(vertex);				
		}
		return ans;
	}
	
	public static <V,E,T extends Comparable<T>> double[] outDegreeAsProbability(KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			Set<V> vertices){
		return normalizeToProbability(outDegree(multiPeriodCyclePacking,vertices),potentialEdgeTargets(multiPeriodCyclePacking));
	}
	
	public static <V,E,T extends Comparable<T>> int[] outDegreeOnSubgraph(KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			Set<V> vertices, DirectedSparseMultigraph<V,E> subgraph){

		int[] ans = new int[vertices.size()];
		int i = 0;
		for(V vertex: vertices){
			if(!subgraph.containsVertex(vertex)){
				throw new RuntimeException();
			}
			ans[i++] = subgraph.outDegree(vertex);				
		}
		return ans;
	}
	
	public static <V,E,T extends Comparable<T>> double[] outDegreeAsProbabilityOnSubgraph(KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			Set<V> vertices, DirectedSparseMultigraph<V,E> subgraph){
		return normalizeToProbability(outDegreeOnSubgraph(multiPeriodCyclePacking,vertices,subgraph),potentialEdgeTargets(multiPeriodCyclePacking,subgraph));
	}
	
	public static <V,E,T extends Comparable<T>> Map<V,Integer> outDegreeOnSubgraphToMap(
			KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			Set<V> vertices, DirectedSparseMultigraph<V,E> subgraph){
		Map<V,Integer> ans = new HashMap<V,Integer>();
		for(V vertex: vertices){
			if(!subgraph.containsVertex(vertex)){
				throw new RuntimeException();
			}
			ans.put(vertex,subgraph.outDegree(vertex));				
		}
		return ans;
	}
	
	public static <V,E,T extends Comparable<T>> ImmutableMap<V,Double> outDegreeAsProbabilityOnSubgraphToMap(
			KepProblemDataInterface<V,E> multiPeriodCyclePacking,
			Set<V> vertices, DirectedSparseMultigraph<V,E> subgraph){
		return normalizeMapToProbability(outDegreeOnSubgraphToMap(multiPeriodCyclePacking,vertices,subgraph),
				potentialEdgeSources(multiPeriodCyclePacking,subgraph));
	}
	
	public static <V,E,T extends Comparable<T>> Set<V> verticesPaired(KepProblemDataInterface<V,E> input){
		Set<V> vertices = new HashSet<V>(input.getGraph().getVertices());
		vertices.removeAll(input.getTerminalNodes());
		vertices.removeAll(input.getRootNodes());
		return vertices;
	}
	
	public static <V,E,T extends Comparable<T>> Set<V> verticesNoTerminal(KepProblemDataInterface<V,E> input){
		Set<V> vertices = new HashSet<V>(input.getGraph().getVertices());
		vertices.removeAll(input.getTerminalNodes());
		return vertices;
	}
	
	
	

	
	public static <V,E,T extends Comparable<T>> Set<V> verticesArrivedAfterStart(MultiPeriodCyclePackingInputs<V,E,T> multiPeriodCyclePacking){
		Set<V> vertices = new HashSet<V>(multiPeriodCyclePacking.getGraph().getVertices());
		Iterator<V> it = vertices.iterator();
		while(it.hasNext()){
			V next = it.next();
			if(multiPeriodCyclePacking.getNodeArrivalTimes().get(next).compareTo(multiPeriodCyclePacking.getStartTime()) <= 0){
				it.remove();
			}
		}
		return vertices;
	}
	
	public static <V,E,T extends Comparable<T>> Set<V> verticesReceivedEdgeInMatching(Results<V,E,T> results){
		Set<V> vertices = new HashSet<V>(results.getMultiPeriodPacking().getInputs().getGraph().getVertices());
		Iterator<V> it = vertices.iterator();
		while(it.hasNext()){
			V next = it.next();
			if(!results.getNodeStatistics().get(next).receivedEdgeInMatching()){
				it.remove();
			}
		}
		return vertices;
	}
	
	

}
