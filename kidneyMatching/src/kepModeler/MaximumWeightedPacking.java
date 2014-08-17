package kepModeler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class MaximumWeightedPacking<V, E> implements Objective<V, E> {

  public static final double defaultCycleBonus = .01;
  public static final double defaultDefaultEdgeWeight = 1;
  public static final PairMatchPowerBonus defaultPairMatchPowerBonus = new PairMatchPowerThesholdBonus();

  private final double cycleBonus;
  private final double defaultEdgeWeight;
  private PairMatchPowerBonus pairMatchPowerBonus;

  private AuxiliaryInputStatistics<V, E> auxiliaryInputStatistics;
  private Set<V> pairedNodes;
  private DirectedSparseMultigraph<V, E> graph;

  private Function<E, Double> edgeWeights;

  public MaximumWeightedPacking(
      AuxiliaryInputStatistics<V, E> auxilaryInputStatistics,
      Set<V> pairedNodes, DirectedSparseMultigraph<V, E> graph) {
    this(auxilaryInputStatistics, pairedNodes, graph, defaultCycleBonus,
        defaultDefaultEdgeWeight, defaultPairMatchPowerBonus);
  }

  public MaximumWeightedPacking(
      AuxiliaryInputStatistics<V, E> auxilaryInputStatistics,
      final Set<V> pairedNodes, final DirectedSparseMultigraph<V, E> graph,
      double cycleBonus, final double defaultEdgeWeight,
      final PairMatchPowerBonus pairMatchPowerBonus) {
    super();

    this.auxiliaryInputStatistics = auxilaryInputStatistics;
    this.pairedNodes = pairedNodes;
    this.graph = graph;
    this.cycleBonus = cycleBonus;
    this.defaultEdgeWeight = defaultEdgeWeight;
    this.pairMatchPowerBonus = pairMatchPowerBonus;
    this.edgeWeights = new Function<E, Double>() {
      @Override
      public Double apply(E edge) {
        double val = defaultEdgeWeight;
        if (pairedNodes.contains(graph.getDest(edge))) {
          val += pairMatchPowerBonus
              .edgeBonusForReceiverPairMatchPower(getPairMatchPower(graph
                  .getDest(edge)));
        }
        return val;
      }
    };
  }

  private double getPairMatchPower(V vertex) {
    if (!this.pairedNodes.contains(vertex)) {
      throw new RuntimeException();
    }
    double donorPower = this.auxiliaryInputStatistics
        .getDonorPowerPostPreference().get(vertex).doubleValue();
    double receiverPower = this.auxiliaryInputStatistics
        .getReceiverPowerPostPreference().get(vertex).doubleValue();
    return donorPower * receiverPower * 10000;
  }

  public static interface PairMatchPowerBonus {
    public double edgeBonusForReceiverPairMatchPower(double pairMatchPower);
  }

  public static class PairMatchPowerThesholdBonus implements
      PairMatchPowerBonus {

    private List<BonusThreshold> bonusStepFunction;
    private double bonusForHighPmp;

    /**
     * Builds a step function f(x) = y such that if thresholds[i-1] < x <=
     * thresholds[i], then f(x) = bonuses[i]. The step function takes a pair
     * match power x, and assigns bonus f(x) to the value of a matching. If x <=
     * thresholds[0], then (naturally) f(x) = bonuses[0], and x >
     * thresholds[thresholds.length-1] implies that f(x) =
     * bonuses[thresholds.length]. Requires: thresholds.length = bonuses.length
     * - 1.
     * 
     * @param thresholds
     *          the breaking points for the step function, must have
     *          thresholds[i] < thresholds[i+1] for all i
     * @param bonuses
     *          the values of the step function, must have bonus[i] > bonus[i+1]
     *          for all i.
     */
    public PairMatchPowerThesholdBonus(double[] thresholds, double[] bonuses) {
      this(makeBonusThresholdList(thresholds, bonuses),
          bonuses[bonuses.length - 1]);
    }

    private static List<BonusThreshold> makeBonusThresholdList(
        double[] thresholds, double[] bonuses) {
      List<BonusThreshold> bonusThreshList = new ArrayList<BonusThreshold>();
      for (int i = 0; i < thresholds.length; i++) {
        bonusThreshList.add(new BonusThreshold(thresholds[i], bonuses[i]));
      }
      return bonusThreshList;
    }

    /**
     * Produces a function taking a pair match power x to a bonus f(x) such that
     * if bonusStepFunction.get(i-1).threshold() < x <=
     * bonusStepFunction.get(i).threshold(), then f(x) =
     * bonusStepFunction.get(i).bonus(). Naturally, the edge cases are: If x <=
     * bonusStepFunction.get(0).threshold(), then f(x) =
     * bonusStepFunction.get(0).bonus(), and If x >
     * bonusStepFunction.get(bonusStepFunction.size()-1).threshold(), then f(x)
     * = bonusForHighPmp. Requires: The sequence
     * bonusStepFunction.get(i).threshold() must be strictly increasing.
     * Requires: The sequence bonusStepFunction.get(i).bonus() must be strictly
     * decreasing. Requires: bonusForHighPmp must be less than
     * bonusStepFunction.get(bonusStepFunction.size()-1).bonus()
     * 
     * @param bonusStepFunction
     *          a sequence of break point function value pairs where the break
     *          points are increasing and bonus values are decreasing.
     * @param bonusForHighPmp
     *          The bonus value when the pair match power is above all threhold
     *          values in bonusStepFunction.
     */
    public PairMatchPowerThesholdBonus(List<BonusThreshold> bonusStepFunction,
        double bonusForHighPmp) {
      this.bonusStepFunction = bonusStepFunction;
      this.bonusForHighPmp = bonusForHighPmp;
      this.validateBonusStepFunction();
    }

    /**
     * Uses default values for bonus function.
     */
    public PairMatchPowerThesholdBonus() {
      this(new double[] { 1, 10, 100 }, new double[] { 3, .6, .1, .01 });
    }

    private void validateBonusStepFunction() {
      for (BonusThreshold bonus : bonusStepFunction) {
        if (bonus.getPairMatchPower() < 0) {
          throw new RuntimeException("Pair match power was "
              + bonus.getPairMatchPower() + " but must be non-negative");
        }
        if (bonus.getBonus() < 0) {
          System.err.println("warning: bound negative bonus: "
              + bonus.getBonus());
          // throw new RuntimeException("Bonus was " + bonus.getBonus() +
          // " but must be non-negative");
        }
      }
      for (int i = 1; i < bonusStepFunction.size(); i++) {
        if (bonusStepFunction.get(i).getPairMatchPower() <= bonusStepFunction
            .get(i - 1).getPairMatchPower()) {
          throw new RuntimeException(
              "Pair match power must be strictly increasing, but threshold "
                  + i + " was " + bonusStepFunction.get(i).getPairMatchPower()
                  + " and threshold " + (i - 1) + " was "
                  + bonusStepFunction.get(i - 1).getPairMatchPower());
        }
        if (bonusStepFunction.get(i).getBonus() >= bonusStepFunction.get(i - 1)
            .getBonus()) {
          throw new RuntimeException(
              "Bonuses must be strictly decreasing, but bonus " + i + " was "
                  + bonusStepFunction.get(i).getBonus() + " and bonus "
                  + (i - 1) + " was " + bonusStepFunction.get(i - 1).getBonus());
        }
      }
      if (bonusStepFunction.size() > 0
          && bonusForHighPmp >= bonusStepFunction.get(
              bonusStepFunction.size() - 1).getBonus()) {
        throw new RuntimeException(
            "Bonuses must be strictly decreasing, but final bonus for high pmp was "
                + bonusForHighPmp
                + " and next to last bonus (last in list) was "
                + bonusStepFunction.get(bonusStepFunction.size() - 1)
                    .getBonus());
      }
    }

    @Override
    public double edgeBonusForReceiverPairMatchPower(double pairMatchPower) {
      for (BonusThreshold threshold : this.bonusStepFunction) {
        if (pairMatchPower <= threshold.getPairMatchPower()) {
          return threshold.getBonus();
        }
      }
      return bonusForHighPmp;
    }

    public static class BonusThreshold {
      private double pairMatchPower;
      private double bonus;

      public BonusThreshold(double pairMatchPower, double bonus) {
        super();
        this.pairMatchPower = pairMatchPower;
        this.bonus = bonus;
      }

      public double getPairMatchPower() {
        return pairMatchPower;
      }

      public double getBonus() {
        return bonus;
      }
    }

  }

  @Override
  public Function<? super E, ? extends Number> getEdgeWeights() {
    return this.edgeWeights;
  }

  @Override
  public double getCycleBonus() {
    return this.cycleBonus;
  }

}
