package exchangeGraph;

import exchangeGraph.VariableSet.VariableExtractor;
import graphUtil.CycleChainDecomposition;
import graphUtil.EdgeCycle;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;

import java.util.Set;

/**
 * Variables and constraints are sufficient to produce a solution to the
 * deterministic KEP.
 */
public interface KepSolutionPolytope<V, E> extends KepPolytope<V, E> {

  public IloLinearNumExpr createObjective() throws IloException;

  public CycleChainDecomposition<V, E> recoverSolution() throws IloException;

  public UserSolution createUserSolution(CycleChainDecomposition<V, E> solution);

  public UserSolution createUserSolution(Set<E> edges, Set<EdgeCycle<E>> cycles);

  public UserSolution roundFractionalSolution(
      VariableExtractor variableExtractor);

  public static class UserSolution {
    private IloNumVar[] variables;
    private double[] values;

    public IloNumVar[] getVariables() {
      return variables;
    }

    public double[] getValues() {
      return values;
    }

    public UserSolution(IloNumVar[] variables, double[] values) {
      super();
      this.variables = variables;
      this.values = values;
    }
  }

}
