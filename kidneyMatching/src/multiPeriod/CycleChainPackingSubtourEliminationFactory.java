package multiPeriod;

import ilog.concert.IloException;

import java.util.Map;

import kepLib.KepInstance;
import kepLib.KepProblemData;
import kepModeler.AuxiliaryInputStatistics;
import kepModeler.KepModeler;
import kepModeler.ModelerInputs;
import protoModeler.PredicateBuilder;
import protoModeler.PredicateFactory;
import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import exchangeGraph.CycleChainPackingSubtourElimination;
import exchangeGraph.SolverOption;

public class CycleChainPackingSubtourEliminationFactory<U, F> extends
    CycleChainPackingFactory<U, F> {

  private Optional<FixedThreadPool> threadPool;

  private ImmutableSet<SolverOption> solverOptions;

  @Deprecated
  public CycleChainPackingSubtourEliminationFactory(KepModeler kepModeler,
      boolean displayOutput, Optional<Double> maxTimeSeconds,
      Optional<FixedThreadPool> threadPool) {
    this(kepModeler, displayOutput, maxTimeSeconds, threadPool, null);
  }

  @Deprecated
  public CycleChainPackingSubtourEliminationFactory(KepModeler kepModeler,
      boolean displayOutput, Optional<Double> maxTimeSeconds,
      Optional<FixedThreadPool> threadPool,
      AuxiliaryInputStatistics<U, F> auxiliaryInputStatistics) {
    this(kepModeler, displayOutput, maxTimeSeconds, threadPool,
        auxiliaryInputStatistics, SolverOption.defaultOptions, null);
  }

  @Deprecated
  public CycleChainPackingSubtourEliminationFactory(KepModeler kepModeler,
      boolean displayOutput, Optional<Double> maxTimeSeconds,
      Optional<FixedThreadPool> threadPool,
      AuxiliaryInputStatistics<U, F> auxiliaryInputStatistics,
      ImmutableSet<SolverOption> solverOptions) {
    this(kepModeler, displayOutput, maxTimeSeconds, threadPool,
        auxiliaryInputStatistics, solverOptions, null);
  }

  public CycleChainPackingSubtourEliminationFactory(KepModeler kepModeler,
      boolean displayOutput, Optional<Double> maxTimeSeconds,
      Optional<FixedThreadPool> threadPool,
      AuxiliaryInputStatistics<U, F> auxiliaryInputStatistics,
      ImmutableSet<SolverOption> solverOptions,
      PredicateFactory<U, F> predicateFactory) {
    super(kepModeler, displayOutput, maxTimeSeconds, auxiliaryInputStatistics,
        predicateFactory);
    this.solverOptions = solverOptions;
    this.threadPool = threadPool;

  }

  @Override
  public CycleChainPackingSubtourElimination<U, F> makeCycleChainPackingIP(
      KepProblemData<U, F> kepProblemData, Map<U, Double> waitingTimes)
      throws IloException {

    ModelerInputs<U, F> modelerInputs = new ModelerInputs<U, F>(kepProblemData,
        super.getAuxiliaryInputStatistics(), waitingTimes);
    PredicateFactory<U, F> factory = getEdgePredicateFactory();
    PredicateBuilder<U, F> predicateBuilder = factory == null ? null : factory
        .createPredicateBuilder(modelerInputs);

    KepInstance<U, F> kepInstance = super.getKepModeler().makeKepInstance(
        modelerInputs, predicateBuilder);
    return new CycleChainPackingSubtourElimination<U, F>(kepInstance,
        displayOutput, maxTimeSeconds, threadPool, solverOptions);
  }

  @Override
  public void cleanUp() {

  }

}
