package exchangeGraph.stochasticOpt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.DirectedEdgeVariables;
import exchangeGraph.KepPolytope;
import exchangeGraph.NoUserCuts;
import exchangeGraph.UserCutGenerator;
import exchangeGraph.VariableSet.VariableExtractor;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class BoundedDegree<V,E> implements KepPolytope<V,E>{
	
	private DirectedSparseMultigraph<V,E> graph;
	private IloCplex cplex;
	private DirectedEdgeVariables<V,E> edgeVariables;
	private int inDegreeMaximum;
	private int outDegreeMaximum;
	
	private Map<V,IloRange> inDegreeConstraints;
	private Map<V,IloRange> outDegreeConstraints;
	
	
	public BoundedDegree(DirectedSparseMultigraph<V,E> graph,IloCplex cplex, int inDegreeMaximum, int outDegreeMaximum) throws IloException{
		this.graph = graph;
		this.cplex = cplex;
		this.edgeVariables = new DirectedEdgeVariables<V,E>(graph,cplex);
		this.inDegreeConstraints = new HashMap<V,IloRange>();
		this.outDegreeConstraints = new HashMap<V,IloRange>();
		for(V vertex: graph.getVertices()){
			IloRange inConstraint = cplex.addLe(edgeVariables.integerSum(graph.getInEdges(vertex)), inDegreeMaximum);
			inDegreeConstraints.put(vertex, inConstraint);
			IloRange outConstraint = cplex.addLe(edgeVariables.integerSum(graph.getOutEdges(vertex)), outDegreeMaximum);
			outDegreeConstraints.put(vertex, outConstraint);
		}
		
	}

	@Override
	public IloLinearIntExpr indicatorEdgeSelected(E edge) throws IloException {
		IloLinearIntExpr ans = cplex.linearIntExpr();
		ans.addTerm(this.edgeVariables.get(edge), 1);
		return ans;
	}

	

	@Override
	public List<IloRange> lazyConstraint(
			VariableExtractor variableExtractor) throws IloException {
		return new ArrayList<IloRange>();
	}

	@Override
	public UserCutGenerator makeUserCutGenerator(
			VariableExtractor variableExtractor) throws IloException {
		return NoUserCuts.INSTANCE;
	}

	@Override
	public void relaxAllIntegerVariables() throws IloException {
		this.edgeVariables.relaxIntegrality();
	}

	@Override
	public void restateAllIntegerVariables() throws IloException {
		this.edgeVariables.restateIntegrality();
	}

}
