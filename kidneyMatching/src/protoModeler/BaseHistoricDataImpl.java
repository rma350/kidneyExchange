package protoModeler;

import kepLib.KepProblemData;
import kepModeler.AuxiliaryInputStatistics;
import kepProtos.KepProtos.NodeType;
import protoModeler.BasePredicateBuilder.HistoricData;

public class BaseHistoricDataImpl<V, E> implements HistoricData<V, E> {

  private KepProblemData<V, E> baseProblemData;
  private AuxiliaryInputStatistics<V, E> auxiliaryInputStatistics;

  public BaseHistoricDataImpl(KepProblemData<V, E> baseProblemData,
      AuxiliaryInputStatistics<V, E> auxiliaryInputStatistics) {
    super();
    this.baseProblemData = baseProblemData;
    this.auxiliaryInputStatistics = auxiliaryInputStatistics;
  }

  @Override
  public double historicDonorPower(V node) {
    return this.auxiliaryInputStatistics.getDonorPowerPostPreference()
        .get(node);
  }

  @Override
  public double historicPatientPower(V node) {
    return this.auxiliaryInputStatistics.getReceiverPowerPostPreference().get(
        node);
  }

  @Override
  public NodeType rawNodeType(V node) {
    return ProtoUtil.getCurrentNodeType(node, baseProblemData);
  }

}
