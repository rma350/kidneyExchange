package exchangeGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;





public abstract class VariableSet<T> implements Iterable<T> {
	
	
	private IloCplex cplex;
	private CplexVariableExtractor cplexExtractor;
	
	
	public VariableSet(IloCplex cplex){
		this.cplex = cplex;
		this.cplexExtractor = new CplexVariableExtractor();
	}
	
	protected abstract ImmutableBiMap<T,? extends IloNumVar> getVarMap();
	
	protected abstract IloNumVar[] getVarsInOrder();
	
	public int size(){
		return getVarsInOrder().length;
	}
	
	public ImmutableSet<T> keySet(){
		return getVarMap().keySet();
	}
	
	public ImmutableSet<? extends IloNumVar> values(){
		return getVarMap().values();
	}
	
	public IloNumVar get(T variableObject){
		return getVarMap().get(variableObject);
	}
	
	public T getInverse(IloNumVar variable){
		return getVarMap().inverse().get(variable);
	}
	
	public boolean contains(T variableObject){
		return getVarMap().containsKey(variableObject);
	}
	
	public IloLinearNumExpr doubleSum(Iterable<T> set, 
			Function<? super T,? extends Number> coefficients) throws IloException{
		IloLinearNumExpr sum = cplex.linearNumExpr();
		for(T t: set){
			sum.addTerm(getVarMap().get(t), coefficients.apply(t).doubleValue());
		}
		return sum;
	}
	

	public Map<T,Double> getVariableValues() throws IloException{
		return getVariableValues(this.cplexExtractor);
	}
	
	public Map<T,Double> getVariableValues(VariableExtractor extractor) throws IloException{
		IloNumVar[] vars = this.getVarsInOrder();
		double[] values = extractor.getValuesVE(vars);
		Map<T,Double> ans = new HashMap<T,Double>();
		for(int i = 0; i < vars.length; i++){
			ans.put(this.getVarMap().inverse().get(vars[i]),values[i]);
		}
		return ans;
	}
	
	public Map<T,Double> getNonZeroVariableValues() throws IloException{
		return getNonZeroVariableValues(this.cplexExtractor);
	}
	
	public Map<T,Double> getNonZeroVariableValues(VariableExtractor extractor) throws IloException{
		IloNumVar[] vars = this.getVarsInOrder();
		double[] values = extractor.getValuesVE(vars);
		Map<T,Double> ans = new HashMap<T,Double>();
		for(int i = 0; i < vars.length; i++){
			if(!CplexUtil.doubleIsZero(values[i])){
				ans.put(this.getVarMap().inverse().get(vars[i]),values[i]);
			}
		}
		return ans;
	}
	
	public Set<T> getNonZeroVariables() throws IloException{
		return getNonZeroVariables(this.cplexExtractor);
	}
	
	public Set<T> getNonZeroVariables(VariableExtractor extractor) throws IloException{
		IloNumVar[] vars = this.getVarsInOrder();
		double[] values = extractor.getValuesVE(vars);
		Set<T> ans = new HashSet<T>();
		for(int i = 0; i < vars.length; i++){
			if(!CplexUtil.doubleIsZero(values[i])){
				ans.add(this.getVarMap().inverse().get(vars[i]));
			}
		}
		return ans;
	}
	
	public interface VariableExtractor{
		public double[] getValuesVE(IloNumVar[] variables) throws IloException;
	}
	private class CplexVariableExtractor implements VariableExtractor{
		public CplexVariableExtractor(){}
		@Override
		public double[] getValuesVE(IloNumVar[] variables) throws IloException {
			return variables.length == 0 ? new double[0] : cplex.getValues(variables);
		}		
	}
	@Override
	public UnmodifiableIterator<T> iterator() {
		return this.getVarMap().keySet().iterator();
	}

	

}
