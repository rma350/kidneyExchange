package exchangeGraph;



import ilog.concert.IloException;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

public abstract class FlowInterface<V,E> {
	
	private IloCplex cplex;
	
	protected FlowInterface(IloCplex cplex){
		this.cplex = cplex;
	}
	
	public abstract void addFlowInIntScaled(V vertex, IloLinearIntExpr expr, int scale) throws IloException;
	public abstract void addFlowOutIntScaled(V vertex, IloLinearIntExpr expr, int scale) throws IloException;
	
	
	
	public abstract void addFlowInDoubleScaled(V vertex,IloLinearNumExpr expr, double d) throws IloException;
	public abstract void addFlowOutDoubleScaled(V vertex,IloLinearNumExpr expr, double d) throws IloException;
	
	
	public abstract void relaxIntegrality() throws IloException;
	public abstract void restateIntegrality() throws IloException;
	
	
	/**
	 * creates a new linear int expr, calls addFlowInIntScaled, and returns the result.
	 * @param vertex
	 * @param scale
	 * @return
	 * @throws IloException
	 */
	
	public IloLinearIntExpr flowInIntScaled(V vertex, int scale)
			throws IloException {
		IloLinearIntExpr ans = cplex.linearIntExpr();
		this.addFlowInIntScaled(vertex, ans, scale);
		return ans;
	}

	
	public IloLinearIntExpr flowOutIntScaled(V vertex, int scale)
			throws IloException {
		IloLinearIntExpr ans = cplex.linearIntExpr();
		this.addFlowOutIntScaled(vertex, ans, scale);
		return ans;
	}

	
	public IloLinearNumExpr flowInDoubleScaled(V vertex, double d)
			throws IloException {
		IloLinearNumExpr ans = cplex.linearNumExpr();
		this.addFlowInDoubleScaled(vertex, ans, d);
		return ans;
	}

	
	public IloLinearNumExpr flowOutDoubleScaled(V vertex, double d)
			throws IloException {
		IloLinearNumExpr ans = cplex.linearNumExpr();
		this.addFlowOutDoubleScaled(vertex, ans, d);
		return ans;
	}
	
	
	

}
