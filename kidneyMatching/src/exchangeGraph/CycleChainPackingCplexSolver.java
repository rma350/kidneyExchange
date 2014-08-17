package exchangeGraph;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import kepLib.KepInstance;

import com.google.common.base.Optional;

public abstract class CycleChainPackingCplexSolver<V, E> extends
    CycleChainPackingIp<V, E> {

  protected IloCplex cplex;
  protected boolean solveLpRelaxationFirst = false;
  protected double lpRelaxationValue;
  private boolean displayOutput;
  private double solveTimeSeconds;
  private boolean throwExceptionNoSolution;

  protected CycleChainPackingCplexSolver(KepInstance<V, E> kepInstance,
      boolean displayOutput, Optional<Double> maxTimeSeconds,
      boolean throwExceptionNoSolution) {
    super(kepInstance, displayOutput, maxTimeSeconds);
    try {
      cplex = new IloCplex();
      this.displayOutput = displayOutput;
      if (maxTimeSeconds.isPresent()) {
        cplex.setParam(DoubleParam.TiLim, maxTimeSeconds.get().doubleValue());
      }
      if (!displayOutput) {
        cplex.setOut(null);
        cplex.setWarning(null);
      }
    } catch (IloException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract void relaxAllIntegerVariables() throws IloException;

  protected abstract void restateAllIntegerVariables() throws IloException;

  public void solve() {
    try {
      long startms = System.currentTimeMillis();
      if (this.solveLpRelaxationFirst) {
        this.relaxAllIntegerVariables();
        cplex.solve();
        this.lpRelaxationValue = cplex.getObjValue();
        if (displayOutput) {
          System.out.println("LP relaxation: " + this.lpRelaxationValue);
        }
        restateAllIntegerVariables();
      }
      cplex.solve();
      long endms = System.currentTimeMillis();
      this.solveTimeSeconds = (endms - startms) / 1000.0;
      if (this.displayOutput) {
        System.out.println("solve time seconds: " + this.solveTimeSeconds);
        System.out.println("objective: " + cplex.getObjValue());
      }
    } catch (IloException e) {
      if (throwExceptionNoSolution) {
        throw new RuntimeException(e);
      } else {
        System.err.print(e);
        return;
      }
    }
    this.solution = recoverSolution();
  }

  public IloCplex getCplex() {
    return cplex;
  }

  public void cleanUp() {
    cplex.end();
  }

  @Override
  public double getSolveTimeSeconds() {
    return this.solveTimeSeconds;
  }

}
