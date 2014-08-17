package kepModeler;

import java.util.Map;
import java.util.Set;

import kepLib.KepInstance;
import kepLib.KepProblemData;
import kepModeler.ChainsForcedRemainOpenOptions.AtLeastCountChains;
import kepModeler.ChainsForcedRemainOpenOptions.AtLeastCountOfQualityChains;
import kepModeler.ChainsForcedRemainOpenOptions.OptionName;
import kepModeler.ObjectiveMode.MaximumCardinalityMode;
import kepModeler.ObjectiveMode.MaximumWeightedPackingMode;
import kepModeler.ObjectiveMode.VpraWeightObjectiveMode;
import kepProtos.KepProtos;
import protoModeler.PredicateBuilder;
import protoModeler.ProtoObjectiveBuilder;

import com.google.common.base.Function;

import data.ExchangeUnit;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class KepModeler {

  private int chainMaxLength;
  private int cycleMaxLength;
  private ChainsForcedRemainOpenOptions openChainOptions;
  private ObjectiveMode objectiveMode;

  private KepProtos.ObjectiveFunction protoObjective;
  private final double protoCycleBonus;

  public KepModeler(int chainMaxLength, int cycleMaxLength,
      ChainsForcedRemainOpenOptions openChainOptions,
      ObjectiveMode objectiveMode) {
    super();
    this.chainMaxLength = chainMaxLength;
    this.cycleMaxLength = cycleMaxLength;
    this.openChainOptions = openChainOptions;
    this.objectiveMode = objectiveMode;
    this.protoObjective = null;
    protoCycleBonus = 0;
  }

  public KepModeler(int chainMaxLength, int cycleMaxLength,
      ChainsForcedRemainOpenOptions openChainOptions,
      KepProtos.ObjectiveFunction protoObjective, double protoCycleBonus) {
    super();
    System.out.println("proto objective: ");
    System.out.println(protoObjective.toString());
    this.chainMaxLength = chainMaxLength;
    this.cycleMaxLength = cycleMaxLength;
    this.openChainOptions = openChainOptions;
    this.protoObjective = protoObjective;
    this.protoCycleBonus = protoCycleBonus;
    this.objectiveMode = null;
  }

  public int getChainMaxLength() {
    return chainMaxLength;
  }

  public int getCycleMaxLength() {
    return cycleMaxLength;
  }

  public ChainsForcedRemainOpenOptions getOpenChainOptions() {
    return openChainOptions;
  }

  public ObjectiveMode getObjectiveMode() {
    return objectiveMode;
  }

  public <V, E> KepInstance<V, E> makeKepInstance(
      ModelerInputs<V, E> modelerInputs, PredicateBuilder<V, E> predicateBuilder) {
    KepProblemData<V, E> problemData = modelerInputs.getKepProblemData();
    Objective<V, E> obj = makeObjectiveFunction(modelerInputs, predicateBuilder);
    KepInstance<V, E> ans = new KepInstance<V, E>(problemData,
        obj.getEdgeWeights(), chainMaxLength, cycleMaxLength,
        obj.getCycleBonus());

    if (openChainOptions.getOptionName().equals(OptionName.atLeastCount)) {
      AtLeastCountChains chainOption = (AtLeastCountChains) this.openChainOptions;
      MinimumBridgeQualityConstraint.addConstraint(chainOption, ans);
    } else if (openChainOptions.getOptionName().equals(
        OptionName.atLeastCountOfQuality)) {
      AtLeastCountOfQualityChains chainOption = (AtLeastCountOfQualityChains) this.openChainOptions;
      MinimumBridgeQualityConstraint.addConstraint(chainOption, ans,
          modelerInputs.getAuxiliaryInputStatistics());
    } else if (openChainOptions.getOptionName().equals(OptionName.none)) {

    } else {
      throw new RuntimeException("unexpected open chain option: "
          + openChainOptions.getName());
    }
    return ans;
  }

  @Deprecated
  public <V, E> KepInstance<V, E> makeKepInstance(
      DirectedSparseMultigraph<V, E> graph, Set<V> rootNodes,
      Set<V> pairedNodes, Set<V> terminalNodes,
      AuxiliaryInputStatistics<V, E> auxiliaryInputStatistics,
      PredicateBuilder<V, E> predicateBuilder) {
    return makeKepInstance(
        new ModelerInputs<V, E>(new KepProblemData<V, E>(graph, rootNodes,
            pairedNodes, terminalNodes), auxiliaryInputStatistics),
        predicateBuilder);
  }

  @Deprecated
  public <V, E> KepInstance<V, E> makeKepInstance(
      DirectedSparseMultigraph<V, E> graph, Set<V> rootNodes,
      Set<V> pairedNodes, Set<V> terminalNodes,
      AuxiliaryInputStatistics<V, E> auxiliaryInputStatistics,
      Map<V, Double> timeWaitedDays, PredicateBuilder<V, E> predicateBuilder) {
    return makeKepInstance(new ModelerInputs<V, E>(new KepProblemData<V, E>(
        graph, rootNodes, pairedNodes, terminalNodes),
        auxiliaryInputStatistics, timeWaitedDays), predicateBuilder);
  }

  private <V, E> Objective<V, E> makeProtoObjective(
      ModelerInputs<V, E> modelerInputs, PredicateBuilder<V, E> predicateBuilder) {
    ProtoObjectiveBuilder<V, E> builder = new ProtoObjectiveBuilder<V, E>(
        predicateBuilder, this.protoObjective);
    final Function<E, Double> edgeWeights = builder.getObjectiveFunction();
    return new Objective<V, E>() {

      @Override
      public Function<? super E, ? extends Number> getEdgeWeights() {
        return edgeWeights;
      }

      @Override
      public double getCycleBonus() {
        return protoCycleBonus;
      }
    };

  }

  private <V, E> Objective<V, E> makeObjectiveFunction(
      ModelerInputs<V, E> modelerInputs, PredicateBuilder<V, E> predicateBuilder) {
    if (this.protoObjective != null) {
      return makeProtoObjective(modelerInputs, predicateBuilder);
    } else {
      if (this.objectiveMode instanceof MaximumCardinalityMode) {
        return new MaximumCycleChainPacking<V, E>(objectiveMode.getCycleBonus());
      } else if (objectiveMode instanceof MaximumWeightedPackingMode) {
        MaximumWeightedPackingMode maxWeight = (MaximumWeightedPackingMode) objectiveMode;
        return new MaximumWeightedPacking<V, E>(
            modelerInputs.getAuxiliaryInputStatistics(), modelerInputs
                .getKepProblemData().getPairedNodes(), modelerInputs
                .getKepProblemData().getGraph(), maxWeight.getCycleBonus(),
            maxWeight.getDefaultEdgeWeight(),
            maxWeight.getPairMatchPowerBonus());
      } else if (objectiveMode instanceof VpraWeightObjectiveMode) {
        VpraWeightObjectiveMode maxWeight = (VpraWeightObjectiveMode) objectiveMode;
        ExchangeUnitAuxiliaryInputStatistics exStats = (ExchangeUnitAuxiliaryInputStatistics) modelerInputs
            .getAuxiliaryInputStatistics();
        Set<ExchangeUnit> pairedNodesEx = (Set<ExchangeUnit>) modelerInputs
            .getKepProblemData().getPairedNodes();
        VPraWeightedObjective obj = new VPraWeightedObjective(exStats,
            pairedNodesEx, maxWeight);
        return (Objective<V, E>) obj;
      } else {
        throw new RuntimeException("Unrecoginzied Objective Mode.  Name: "
            + objectiveMode.getName() + ", toString(): "
            + this.objectiveMode.toString());
      }
    }
  }

}
