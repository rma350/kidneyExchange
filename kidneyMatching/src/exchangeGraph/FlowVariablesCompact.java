package exchangeGraph;

import java.util.HashSet;
import java.util.Set;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableListMultimap.Builder;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class FlowVariablesCompact<V,E> extends FlowInterface<V,E>{

	private DirectedSparseMultigraph<V,E> graph;
	private DirectedEdgeVariables<V,E> edgeVariables;
	private IloCplex cplex;


	public FlowVariablesCompact(DirectedSparseMultigraph<V,E> graph,
			DirectedEdgeVariables<V,E> edgeVariables, IloCplex cplex) throws IloException{
		super(cplex);
		this.cplex = cplex;
		this.edgeVariables = edgeVariables;
		this.graph = graph;
		/*for(V vertex: pairedNodes){
			cplex.addGe(count(flowIn.get(vertex),cplex),count(flowOut.get(vertex),cplex) );			
		}*/
		
	}
	
	/*private static IloLinearIntExpr count(Iterable<IloIntVar> vars, IloCplex cplex) throws IloException{
		IloLinearIntExpr ans = cplex.linearIntExpr();
		for(IloIntVar var: vars){
			ans.addTerm(var, 1);
		}
		return ans;
	}*/

	@Override
	public void addFlowInIntScaled(V vertex,IloLinearIntExpr expr, int scale)
			throws IloException {
		expr.add(edgeVariables.integerSum(graph.getInEdges(vertex), CplexUtil.makeConstantIntegerFunction(scale)));
	}

	@Override
	public void addFlowOutIntScaled(V vertex,IloLinearIntExpr expr, int scale)
			throws IloException {
		expr.add(edgeVariables.integerSum(graph.getOutEdges(vertex), CplexUtil.makeConstantIntegerFunction(scale)));
	}

	@Override
	public void addFlowInDoubleScaled(V vertex, IloLinearNumExpr expr, double d)
			throws IloException {
		expr.add(edgeVariables.doubleSum(graph.getInEdges(vertex), CplexUtil.makeConstantDoubleFunction(d)));
	}

	@Override
	public void addFlowOutDoubleScaled(V vertex,IloLinearNumExpr expr, double d)
			throws IloException {
		expr.add(edgeVariables.doubleSum(graph.getOutEdges(vertex), CplexUtil.makeConstantDoubleFunction(d)));
	}

	@Override
	public void relaxIntegrality() throws IloException {}

	@Override
	public void restateIntegrality() throws IloException {}
}
