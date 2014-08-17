package kepModeler;

import java.util.Map;

import kepLib.KepProblemData;
import protoModeler.PredicateBuilder;
import replicator.DonorEdge;
import statistics.Queries;

import com.google.common.collect.Maps;

import data.ExchangeUnit;

public class ModelerInputs<V, E> {

  private KepProblemData<V, E> kepProblemData;
  private AuxiliaryInputStatistics<V, E> auxiliaryInputStatistics;
  private Map<V, Double> timeWaitedDays;
  private PredicateBuilder<V, E> predicateBuilder;

  public AuxiliaryInputStatistics<V, E> getAuxiliaryInputStatistics() {
    return auxiliaryInputStatistics;
  }

  public KepProblemData<V, E> getKepProblemData() {
    return this.kepProblemData;
  }

  public ModelerInputs(KepProblemData<V, E> kepProblemData) {
    this(kepProblemData, Queries.<V, E> getAuxiliaryStatistics(kepProblemData));
  }

  public static ModelerInputs<ExchangeUnit, DonorEdge> newModelerInputs(
      KepProblemData<ExchangeUnit, DonorEdge> kepProblemData) {
    return new ModelerInputs<ExchangeUnit, DonorEdge>(kepProblemData,
        Queries.getAuxiliaryStatisticsWithVPra(kepProblemData));
  }

  public ModelerInputs(KepProblemData<V, E> kepProblemData,
      AuxiliaryInputStatistics<V, E> auxiliaryInputStatistics) {
    this(kepProblemData, auxiliaryInputStatistics, Maps
        .<V, Double> newHashMap());

  }

  public ModelerInputs(KepProblemData<V, E> kepProblemData,
      AuxiliaryInputStatistics<V, E> auxiliaryInputStatistics,
      Map<V, Double> timeWaitedDays) {
    this.kepProblemData = kepProblemData;
    this.auxiliaryInputStatistics = auxiliaryInputStatistics;
    this.timeWaitedDays = timeWaitedDays;
  }

  public Map<V, Double> getTimeWaitedDays() {
    return this.timeWaitedDays;
  }

}
