package exchangeGraph;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumExpr;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import kepLib.KepInstance.RelationType;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.google.common.collect.ImmutableMap;

public class CplexUtil {

  public static final double epsilon = .0001;

  public static boolean doubleToBoolean(double value) {
    if (Math.abs(1 - value) < epsilon) {
      return true;
    } else if (Math.abs(value) < epsilon) {
      return false;
    } else
      throw new RuntimeException(
          "Failed to convert to boolean, not near zero or one: " + value);
  }

  /**
   * 
   * @param cplex
   * @param set
   *          must contain no duplicates according to T.equals(), a duplicate
   *          causes an illegal argument exception
   * @return
   * @throws IloException
   */
  public static <T> ImmutableBiMap<T, IloIntVar> makeBinaryVariables(
      IloCplex cplex, Iterable<T> set) throws IloException {
    Builder<T, IloIntVar> ans = ImmutableBiMap.builder();
    for (T t : set) {
      ans.put(t, cplex.boolVar());
    }
    return ans.build();
  }

  /**
   * 
   * @param cplex
   * @param set
   *          must contain no duplicates according to T.equals(), a duplicate
   *          causes an illegal argument exception
   * @return
   * @throws IloException
   */
  public static <T> ImmutableMap<T, IloLinearIntExpr> makeLinearIntExpr(
      IloCplex cplex, Iterable<T> set) throws IloException {
    ImmutableMap.Builder<T, IloLinearIntExpr> ans = ImmutableMap.builder();
    for (T t : set) {
      ans.put(t, cplex.linearIntExpr());
    }
    return ans.build();
  }

  public static Function<Object, Integer> unity = makeConstantIntegerFunction(1);

  public static Function<Object, Double> makeConstantDoubleFunction(
      final double constant) {
    return new Function<Object, Double>() {
      public Double apply(Object obj) {
        return constant;
      }
    };
  }

  public static Function<Object, Integer> makeConstantIntegerFunction(
      final int constant) {
    return new Function<Object, Integer>() {
      public Integer apply(Object obj) {
        return constant;
      }
    };
  }

  public static boolean doubleIsZero(double value) {
    return Math.abs(value) < epsilon;
  }

  public static boolean doubleIsInteger(double value) {
    return Math.abs(Math.round(value) - value) < epsilon;
  }

  public static IloRange addConstraint(IloNumExpr expr,
      RelationType relationType, double rhs, IloCplex cplex)
      throws IloException {
    if (relationType.equals(RelationType.eq)) {
      return cplex.addEq(expr, rhs);
    } else if (relationType.equals(RelationType.geq)) {
      return cplex.addGe(expr, rhs);
    } else if (relationType.equals(RelationType.leq)) {
      return cplex.addLe(expr, rhs);
    } else {
      throw new RuntimeException(
          "Unidentified relation type defining equation " + relationType);
    }
  }

  public static void rescale(IloLinearNumExpr sum, double multiplier) {
    IloLinearNumExprIterator it = sum.linearIterator();
    while (it.hasNext()) {
      it.nextNumVar();
      it.setValue(it.getValue() * multiplier);
    }
  }

}
