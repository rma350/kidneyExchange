package exchangeGraph;

import java.util.Set;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import com.google.common.collect.ImmutableBiMap;

public class NumVariableSet<T> extends VariableSet<T>{
	
	private IloNumVar[] vars;
	private ImmutableBiMap<T,IloNumVar> varMap;

	public NumVariableSet(Set<T> variableObjects, IloCplex cplex)
			throws IloException {
		this(variableObjects, 0, Double.MAX_VALUE, cplex);
	}

	public NumVariableSet(Set<T> variableObjects, double lowerBound,
			double upperBound, IloCplex cplex) throws IloException {
		this(cplex.numVarArray(variableObjects.size(), lowerBound, upperBound),
				variableObjects, cplex);
	}
	
	private NumVariableSet(IloNumVar[] variables, Iterable<T> variableObjects, IloCplex cplex){
		super(cplex);
		this.vars = variables;
		ImmutableBiMap.Builder<T,IloNumVar> ans = ImmutableBiMap.builder();
		int i = 0;
		for(T t: variableObjects){
			ans.put(t, vars[i++]);
		}
		varMap = ans.build();
	}
	
	@Override
	protected ImmutableBiMap<T, IloNumVar> getVarMap() {
		return varMap;
	}

	@Override
	protected IloNumVar[] getVarsInOrder() {
		return vars;
	}



}
