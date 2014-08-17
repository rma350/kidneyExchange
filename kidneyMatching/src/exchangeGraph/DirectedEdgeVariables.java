package exchangeGraph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;


import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;


public class DirectedEdgeVariables<V, E> extends IntVariableSet<E>{
	
	private DirectedSparseMultigraph<V,E> graph;
	
	public DirectedEdgeVariables(DirectedSparseMultigraph<V,E> graph, IloCplex cplex) throws IloException{
		super(new HashSet<E>(graph.getEdges()),cplex);
		this.graph = graph;
	}

}
