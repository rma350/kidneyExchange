package exchangeGraph;

import ilog.concert.IloException;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;

import com.google.common.collect.ImmutableSet;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class IntegerFlowNetwork<V, E> {
	
	private IloCplex cplex;
	
	private ImmutableSet<V> roots;
	private ImmutableSet<V> paired;
	private ImmutableSet<V> terminal;
	
	private DirectedSparseMultigraph<V,E> graph;
	private DirectedEdgeVariables<V,E> edgeVariables;
	private FlowInterface<V,E> flowInterface;
	
	private boolean expandedFormulation;
	
	public IntegerFlowNetwork(DirectedSparseMultigraph<V,E> graph,ImmutableSet<V> roots,
			ImmutableSet<V> paired,ImmutableSet<V> terminal, IloCplex cplex, boolean expandedFormulation) throws IloException{
		this.graph = graph;
		this.roots = roots;
		this.paired = paired;
		this.terminal = terminal;
		this.cplex = cplex;
		this.expandedFormulation = expandedFormulation;
		this.edgeVariables = new DirectedEdgeVariables<V,E>(graph, cplex);
		if(expandedFormulation){
			this.flowInterface = new FlowVariables<V,E>(graph, roots,paired,terminal,edgeVariables,cplex);
		}
		else{
			this.flowInterface = new FlowVariablesCompact<V,E>(graph,edgeVariables,cplex);
		}
		for(V vertex: roots){
			cplex.addLe(flowInterface.flowOutIntScaled(vertex, 1), 1);
		}
		for(V vertex: paired){
			IloLinearIntExpr flowIn = flowInterface.flowInIntScaled(vertex, 1);
			IloLinearIntExpr flowOut = flowInterface.flowOutIntScaled(vertex, 1);
			cplex.addLe(flowOut, flowIn);
			if(!expandedFormulation){
				cplex.addLe(flowIn, 1);
			}
		}
		for(V vertex: terminal){
			cplex.addLe(flowInterface.flowInIntScaled(vertex, 1), 1);
		}
	}

	public IloCplex getCplex() {
		return cplex;
	}

	public ImmutableSet<V> getRoots() {
		return roots;
	}

	public ImmutableSet<V> getPaired() {
		return paired;
	}

	public ImmutableSet<V> getTerminal() {
		return terminal;
	}

	public DirectedSparseMultigraph<V, E> getGraph() {
		return graph;
	}

	public DirectedEdgeVariables<V, E> getEdgeVariables() {
		return edgeVariables;
	}

	public FlowInterface<V, E> getFlowInterface() {
		return flowInterface;
	}

	public boolean isExpandedFormulation() {
		return expandedFormulation;
	}
	
	
	
}

