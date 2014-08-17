package kepModeler;

import java.util.List;
import java.util.Set;

import kepModeler.MaximumWeightedPacking.PairMatchPowerBonus;

import com.google.common.base.Function;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

public interface Objective<V,E> {
	
	public Function<? super E,? extends Number> getEdgeWeights();
	
	public double getCycleBonus();
	
	//go
	//public Function<List<E>, ? extends Number> getCycleWeights();
	
	/*public static interface ObjectiveBuilder<V,E>{
		public Objective<V,E> build(IloCplex cplex, AuxiliaryInputStatistics<V,E> auxilaryInputStatistics, 
				Set<V> chainRoots,
				Set<V> terminalNodes, Set<V> pairedNodes,
				DirectedSparseMultigraph<V, E> graph,
				DirectedEdgeVariables<V, E> edgeVariables,
				CycleVariables<V, E> cycleVariables);
	}*/

}
