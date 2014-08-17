package exchangeGraph;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;

import ilog.concert.IloConversion;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

public class IntVariableSet<T> extends VariableSet<T> {
	
	private IloIntVar[] vars;
	private ImmutableBiMap<T,IloIntVar> varMap;
	private IloCplex cplex;
	
	private IloConversion relaxation;
	private boolean relaxed;
	
	public IntVariableSet(Set<T> variableObjects, IloCplex cplex) throws IloException{
		this(cplex.boolVarArray(variableObjects.size()),variableObjects,cplex);
	}
	
	@Override
	public IloIntVar get(T variableObject){
		return varMap.get(variableObject);
	}
	
	
	public T getInverse(IloIntVar variable){
		return super.getInverse(variable);
	}
	
	IloIntVar[] getVars(){
		return this.vars;
	}
	
	
	private IntVariableSet(IloIntVar[] variables, Iterable<T> variableObjects, IloCplex cplex){
		super(cplex);
		this.cplex = cplex;
		this.vars = variables;
		ImmutableBiMap.Builder<T,IloIntVar> ans = ImmutableBiMap.builder();
		int i = 0;
		for(T t: variableObjects){
			ans.put(t, vars[i++]);
		}
		varMap = ans.build();
		this.relaxed = false;
	}
	
	public IloLinearIntExpr integerSum(Iterable<T> set, 
			Function<? super T,Integer> coefficients) throws IloException{
		IloLinearIntExpr sum = cplex.linearIntExpr();
		for(T t: set){
			sum.addTerm(varMap.get(t), coefficients.apply(t));
		}
		return sum;
	}
	
	public IloLinearIntExpr integerSum(Iterable<T> set) throws IloException{
		return integerSum(set,CplexUtil.unity);
	}
	
	
	
	public void setPriorities(int priorityLevel) throws IloException{
		int[] priority = new int[this.vars.length];
		Arrays.fill(priority, priorityLevel);
		cplex.setPriorities(this.vars, priority);
	}
	
	public void relaxIntegrality() throws IloException{
		if(relaxed){
			throw new RuntimeException("Already relaxed... cannot relax again");
		}
		else{
			this.relaxation = cplex.conversion(this.vars, IloNumVarType.Float);
			cplex.add(relaxation);
		}
	}
	
	public void restateIntegrality() throws IloException{
		if(!relaxed){
			throw new RuntimeException("Can only restate integrality if variables have already been relaxed.");
		}
		else{
			cplex.remove(relaxation);
			relaxation = null;
		}
	}

	@Override
	protected ImmutableBiMap<T, IloIntVar> getVarMap() {
		return this.varMap;
	}

	@Override
	protected IloIntVar[] getVarsInOrder() {
		return this.vars;
	}
	
	

}