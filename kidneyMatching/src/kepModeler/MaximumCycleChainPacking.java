package kepModeler;

import java.util.List;
import java.util.Set;

import com.google.common.base.Function;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CplexUtil;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

public class MaximumCycleChainPacking<V, E> implements Objective<V, E> {
	
	public static final double defaultCycleBonus = .01;
	
	
	private double cycleBonus;
	private Function<Object,Integer> edgeWeight;

	public MaximumCycleChainPacking(){
		this(defaultCycleBonus);
	}
	public MaximumCycleChainPacking(double cycleBonus) {
		this.cycleBonus = cycleBonus;		
		this.edgeWeight = CplexUtil.unity;
	}


	@Override
	public Function<? super E, Integer> getEdgeWeights() {
		return edgeWeight;
	}
	@Override
	public double getCycleBonus() {
		return cycleBonus;
	}
	
	

}
