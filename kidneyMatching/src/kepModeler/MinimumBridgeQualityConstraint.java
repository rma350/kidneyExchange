package kepModeler;

import ilog.concert.IloException;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import kepLib.KepInstance;
import kepLib.KepInstance.BridgeConstraint;
import kepLib.KepInstance.RelationType;
import kepModeler.ChainsForcedRemainOpenOptions.AtLeastCountChains;
import kepModeler.ChainsForcedRemainOpenOptions.AtLeastCountOfQualityChains;
import kepModeler.ChainsForcedRemainOpenOptions.OptionName;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import exchangeGraph.CplexUtil;

public class MinimumBridgeQualityConstraint {
	

	
	public static <V,E> void addConstraint(AtLeastCountChains atleastCountChains, KepInstance<V,E> kepInstance){
		int effectiveMinimumChainsOpen = Math.min(kepInstance.getRootNodes().size(), atleastCountChains.getMinNumChains());
		int maximumTerminalNodesHit = kepInstance.getRootNodes().size() - effectiveMinimumChainsOpen;
		BridgeConstraint<V> constraint = new BridgeConstraint<V>(kepInstance.getTerminalNodes(),CplexUtil.unity,RelationType.leq,maximumTerminalNodesHit);
		kepInstance.getBridgeConstraints().add(constraint);
	}
	
	public static <V,E> void addConstraint(AtLeastCountOfQualityChains atleastCountQuality, KepInstance<V,E> kepInstance, AuxiliaryInputStatistics<V,E> auxiliaryStats ){
		Set<V> aboveThreshold = new HashSet<V>();
		for(V root: kepInstance.getRootNodes()){
			if(isAboveThreshold(atleastCountQuality,root,auxiliaryStats)){
				aboveThreshold.add(root);
			}
		}
		int rootsAboveThresh = aboveThreshold.size();
		int effectiveMinimumChainsOpen = Math.min(rootsAboveThresh, atleastCountQuality.getMinNumChains());
		for(V paired : kepInstance.getPairedNodes()){
			if(isAboveThreshold(atleastCountQuality,paired,auxiliaryStats)){
				aboveThreshold.add(paired);
			}
		}
		BridgeConstraint<V> constraint = new BridgeConstraint<V>(aboveThreshold,CplexUtil.unity,RelationType.geq,effectiveMinimumChainsOpen);
		kepInstance.getBridgeConstraints().add(constraint);
	}
	
	private static <V,E> boolean isAboveThreshold(AtLeastCountOfQualityChains atleastCountQuality, V vertex, AuxiliaryInputStatistics<V,E> auxiliaryStats ){
		return auxiliaryStats.getDonorPowerPostPreference().get(vertex).doubleValue() >= 
				atleastCountQuality.getMinimumChainRootQuality();
	}
	

	
	
}
