package exchangeGraph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections15.Transformer;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import com.google.common.collect.ImmutableBiMap;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class FlowVariables<V, E> extends FlowInterface<V,E> {
	
	
	private IntVariableSet<V> flowInVars;
	
	private IntVariableSet<V> flowOutVars;
	
	private DirectedEdgeVariables<V,E> edgeVariables;
	
	private IloCplex cplex;
	
	
	
	public FlowVariables(DirectedSparseMultigraph<V,E> graph, Set<V> chainRoots, Set<V> pairedNodes, Set<V> terminalNodes,
			DirectedEdgeVariables<V,E> edgeVariables, IloCplex cplex) throws IloException{
		super(cplex);
		this.edgeVariables = edgeVariables;
		this.cplex = cplex;
		Set<V> nonChainRootNodes = new HashSet<V>(pairedNodes);
		nonChainRootNodes.addAll(terminalNodes);
		flowInVars = new IntVariableSet<V>(nonChainRootNodes,cplex);
		for(V nonChainRoot: nonChainRootNodes){
			cplex.addEq(edgeVariables.integerSum(graph.getInEdges(nonChainRoot)),
					flowInVars.get(nonChainRoot));
		}
		Set<V> nonTerminalNodes = new HashSet<V>(pairedNodes);
		nonTerminalNodes.addAll(chainRoots);
		flowOutVars = new IntVariableSet<V>(nonTerminalNodes,cplex);
		for(V nonTerminalNode: nonTerminalNodes){
			cplex.addEq(edgeVariables.integerSum(graph.getOutEdges(nonTerminalNode)),
					flowOutVars.get(nonTerminalNode));
		}
	}

	public IntVariableSet<V> getFlowInVars() {
		return flowInVars;
	}



	public IntVariableSet<V> getFlowOutVars() {
		return flowOutVars;
	}



	public DirectedEdgeVariables<V, E> getEdgeVariables() {
		return edgeVariables;
	}

	@Override
	public void addFlowInIntScaled(V vertex,IloLinearIntExpr expr, int scale)
			throws IloException {
		expr.addTerm(flowInVars.get(vertex), scale);
	}

	@Override
	public void addFlowOutIntScaled(V vertex,IloLinearIntExpr expr, int scale)
			throws IloException {
		expr.addTerm(flowOutVars.get(vertex), scale);
	}

	@Override
	public void addFlowInDoubleScaled(V vertex,IloLinearNumExpr expr, double d)
			throws IloException {
		expr.addTerm(flowInVars.get(vertex), d);
	}

	@Override
	public void addFlowOutDoubleScaled(V vertex,IloLinearNumExpr expr, double d)
			throws IloException {
		expr.addTerm(flowOutVars.get(vertex), d);
	}

	@Override
	public void relaxIntegrality() throws IloException {
		this.flowInVars.relaxIntegrality();
		this.flowOutVars.relaxIntegrality();
	}

	@Override
	public void restateIntegrality() throws IloException {
		this.flowInVars.restateIntegrality();
		this.flowOutVars.restateIntegrality();
	}

	

	

}
