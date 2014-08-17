package multiPeriod;

import ilog.concert.IloException;

import java.util.Map;

import kepLib.KepProblemData;
import kepModeler.AuxiliaryInputStatistics;
import kepModeler.KepModeler;
import protoModeler.PredicateFactory;

import com.google.common.base.Optional;

import exchangeGraph.CycleChainPackingIp;

public abstract class CycleChainPackingFactory<U, F> {

  private KepModeler kepModeler;

  protected boolean displayOutput;
  protected Optional<Double> maxTimeSeconds;
  private String name;
  private AuxiliaryInputStatistics<U, F> auxiliaryInputStatistics;
  private PredicateFactory<U, F> edgePredicateFactory;

  protected CycleChainPackingFactory(KepModeler kepModeler,
      boolean displayOutput, Optional<Double> maxTimeSeconds,
      AuxiliaryInputStatistics<U, F> auxiliaryInputStatistics,
      PredicateFactory<U, F> edgePredicateFactory) {
    super();
    this.kepModeler = kepModeler;
    this.displayOutput = displayOutput;
    this.maxTimeSeconds = maxTimeSeconds;
    this.auxiliaryInputStatistics = auxiliaryInputStatistics;
    this.edgePredicateFactory = edgePredicateFactory;
  }

  protected PredicateFactory<U, F> getEdgePredicateFactory() {
    return this.edgePredicateFactory;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public boolean isDisplayOutput() {
    return displayOutput;
  }

  public Optional<Double> getMaxTimeSeconds() {
    return maxTimeSeconds;
  }

  public AuxiliaryInputStatistics<U, F> getAuxiliaryInputStatistics() {
    return this.auxiliaryInputStatistics;
  }

  public KepModeler getKepModeler() {
    return this.kepModeler;
  }

  public abstract CycleChainPackingIp<U, F> makeCycleChainPackingIP(
      KepProblemData<U, F> kepProblemData, Map<U, Double> waitingTimes)
      throws IloException;

  public abstract void cleanUp();

}
