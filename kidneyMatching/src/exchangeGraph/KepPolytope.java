package exchangeGraph;

import ilog.concert.IloException;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloRange;

import java.util.List;

import exchangeGraph.VariableSet.VariableExtractor;

public interface KepPolytope<V, E> {

  public IloLinearIntExpr indicatorEdgeSelected(E edge) throws IloException;

  public UserCutGenerator makeUserCutGenerator(
      VariableExtractor variableExtractor) throws IloException;

  public List<IloRange> lazyConstraint(VariableExtractor variableExtractor)
      throws IloException;

  public void relaxAllIntegerVariables() throws IloException;

  public void restateAllIntegerVariables() throws IloException;

}
