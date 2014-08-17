package kepModeler;

import java.util.List;
import java.util.Set;

import kepModeler.MaximumWeightedPacking.PairMatchPowerBonus;
import kepModeler.MaximumWeightedPacking.PairMatchPowerThesholdBonus;
import kepModeler.ObjectiveMode.VpraWeightObjectiveMode;

import com.google.common.base.Function;

import replicator.DonorEdge;
import data.ExchangeUnit;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

public class VPraWeightedObjective implements Objective<ExchangeUnit,DonorEdge> {
	
	private Function<DonorEdge,Double> edgeWeight;	
	private double cycleBonus;
	private double defaultEdgeWeight;
	
	private ExchangeUnitAuxiliaryInputStatistics auxiliaryInputStatistics;
	private Set<ExchangeUnit> pairedNodes;
	private DirectedSparseMultigraph<ExchangeUnit,DonorEdge> graph;
	
	
	private Function<Double,Double> weightByVpra;//input, a vpra, output, a weight.
	
	public VPraWeightedObjective( 
			final ExchangeUnitAuxiliaryInputStatistics auxilaryInputStatistics, 
			final Set<ExchangeUnit> pairedNodes,
			final VpraWeightObjectiveMode vpraWeightObjectiveMode){
		
		this.auxiliaryInputStatistics = auxilaryInputStatistics;
		
		this.pairedNodes = pairedNodes;
		this.weightByVpra = vpraWeightObjectiveMode.getVpraBonus();
		this.cycleBonus = vpraWeightObjectiveMode.getCycleBonus();
		this.edgeWeight = new Function<DonorEdge,Double>(){
			
			@Override
			public Double apply(DonorEdge edge){
				double ans = defaultEdgeWeight;
				if(pairedNodes.contains(graph.getDest(edge))){
					double vpra = auxiliaryInputStatistics.getVirtualPra().get(graph.getDest(edge).getReceiver());
					ans+=  weightByVpra.apply(vpra);
				}
				return ans;
			}
		};
	}

	@Override
	public Function<DonorEdge, Double> getEdgeWeights() {
		return this.edgeWeight;
	}

	@Override
	public double getCycleBonus() {
		return this.cycleBonus;
	}

	
	

}
