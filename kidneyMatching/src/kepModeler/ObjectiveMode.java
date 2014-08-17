package kepModeler;

import kepModeler.MaximumWeightedPacking.PairMatchPowerBonus;

import org.apache.commons.math3.analysis.function.StepFunction;

import com.google.common.base.Function;

public abstract class ObjectiveMode {

  private String name;

  private double cycleBonus;

  protected ObjectiveMode(final double cycleBonus) {
    this.cycleBonus = cycleBonus;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getCycleBonus() {
    return this.cycleBonus;
  }

  public static class ApacheStepFunction implements Function<Double, Double> {

    private StepFunction stepFunc;

    public ApacheStepFunction(StepFunction stepFunc) {
      this.stepFunc = stepFunc;
    }

    @Override
    public Double apply(Double arg0) {
      return stepFunc.value(arg0.doubleValue());
    }

  }

  public static class VpraWeightObjectiveMode extends ObjectiveMode {
    private double cycleBonus;
    private double defaultEdgeWeight;
    private Function<Double, Double> vpraBonus;

    public VpraWeightObjectiveMode(double cycleBonus, double defaultEdgeWeight,
        Function<Double, Double> vpraBonus) {
      super(cycleBonus);
      this.cycleBonus = cycleBonus;
      this.defaultEdgeWeight = defaultEdgeWeight;
      this.vpraBonus = vpraBonus;
    }

    public VpraWeightObjectiveMode(Function<Double, Double> vpraBonus) {
      this(MaximumWeightedPacking.defaultCycleBonus,
          MaximumWeightedPacking.defaultDefaultEdgeWeight, vpraBonus);
    }

    public void setCycleBonus(double cycleBonus) {
      this.cycleBonus = cycleBonus;
    }

    public void setDefaultEdgeWeight(double defaultEdgeWeight) {
      this.defaultEdgeWeight = defaultEdgeWeight;
    }

    public double getCycleBonus() {
      return cycleBonus;
    }

    public double getDefaultEdgeWeight() {
      return defaultEdgeWeight;
    }

    public Function<Double, Double> getVpraBonus() {
      return vpraBonus;
    }

    public void setVpraBonus(Function<Double, Double> vpraBonus) {
      this.vpraBonus = vpraBonus;
    }

  }

  public static class MaximumCardinalityMode extends ObjectiveMode {

    public MaximumCardinalityMode(double cycleBonus) {
      super(cycleBonus);
    }

    public MaximumCardinalityMode() {
      this(MaximumCycleChainPacking.defaultCycleBonus);
    }
  }

  public static class MaximumWeightedPackingMode extends ObjectiveMode {
    private double defaultEdgeWeight;
    private PairMatchPowerBonus pairMatchPowerBonus;

    public MaximumWeightedPackingMode(double cycleBonus,
        double defaultEdgeWeight, PairMatchPowerBonus pairMatchPowerBonus) {
      super(cycleBonus);
      this.defaultEdgeWeight = defaultEdgeWeight;
      this.pairMatchPowerBonus = pairMatchPowerBonus;
    }

    public MaximumWeightedPackingMode() {
      this(MaximumWeightedPacking.defaultCycleBonus,
          MaximumWeightedPacking.defaultDefaultEdgeWeight,
          MaximumWeightedPacking.defaultPairMatchPowerBonus);
    }

    public void setDefaultEdgeWeight(double defaultEdgeWeight) {
      this.defaultEdgeWeight = defaultEdgeWeight;
    }

    public void setPairMatchPowerBonus(PairMatchPowerBonus pairMatchPowerBonus) {
      this.pairMatchPowerBonus = pairMatchPowerBonus;
    }

    public double getDefaultEdgeWeight() {
      return defaultEdgeWeight;
    }

    public PairMatchPowerBonus getPairMatchPowerBonus() {
      return pairMatchPowerBonus;
    }
  }
}
