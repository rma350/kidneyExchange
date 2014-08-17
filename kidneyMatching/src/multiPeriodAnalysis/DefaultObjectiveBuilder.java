package multiPeriodAnalysis;

import java.util.Arrays;

import kepModeler.MaximumWeightedPacking.PairMatchPowerBonus;
import kepModeler.MaximumWeightedPacking.PairMatchPowerThesholdBonus;
import kepModeler.ObjectiveMode;
import kepModeler.ObjectiveMode.ApacheStepFunction;
import kepModeler.ObjectiveMode.MaximumCardinalityMode;
import kepModeler.ObjectiveMode.MaximumWeightedPackingMode;
import kepModeler.ObjectiveMode.VpraWeightObjectiveMode;
import kepProtos.KepProtos.ObjectiveMetric;
import kepProtos.KepProtos.PrioritizationLevel;

import org.apache.commons.math3.analysis.function.StepFunction;

import com.google.common.base.Optional;

public class DefaultObjectiveBuilder extends ObjectiveBuilder {

  public static final DefaultObjectiveBuilder INSTANCE = new DefaultObjectiveBuilder();

  private DefaultObjectiveBuilder() {
  }

  private static final double[] pmpHistBuckets = new double[] { 0, 0.1, 5, 20,
      60, Double.POSITIVE_INFINITY };
  private static final double[] pmpObjectiveBuckets = Arrays.copyOfRange(
      pmpHistBuckets, 1, pmpHistBuckets.length - 1);
  private static final double[] vpraHistBuckets = new double[] { 0, 60, 80, 95,
      99.5, 100 };
  // private static final double[] vpraObjectiveBuckets =
  // Arrays.copyOfRange(vpraHistBuckets,1, vpraHistBuckets.length-1);
  private static final double[] vpraApacheObjectiveBuckets = Arrays
      .copyOfRange(vpraHistBuckets, 0, vpraHistBuckets.length - 1);

  private static final double defaultEdgeWeight = 1;

  @Override
  public ObjectiveMode createObjectiveMode(ObjectiveMetric metric,
      Optional<PrioritizationLevel> prioritization, double cycleBonus) {
    if (metric == ObjectiveMetric.MAX_CARDINALITY) {
      return new MaximumCardinalityMode(cycleBonus);
    } else if (metric == ObjectiveMetric.PAIR_MATCH_POWER) {
      PrioritizationLevel priority = safeGetPrioritization(metric,
          prioritization);
      // must be strictly increasing and nonnegative
      double[] thresholds = pmpObjectiveBuckets;
      // must be strictly decreasing, non-negative, and of one longer length
      // than thresholds
      double[] bonuses;
      String name;
      if (priority == PrioritizationLevel.TIE_BREAKER) {
        bonuses = new double[] { .05, .01, .005, .001, .0005 };
        name = "tieBreaker";
      } else if (priority == PrioritizationLevel.MODERATE) {
        bonuses = new double[] { 3, 1, .6, .1, .01 };
        name = "moderate";
      } else if (priority == PrioritizationLevel.EXTREME) {
        bonuses = new double[] { 10, 5, 3, 1, .01 };
        name = "extreme";
      } else if (priority == PrioritizationLevel.LEXICOGRAPHIC) {
        bonuses = new double[] { 200, 50, 10, 3, .01 };
        name = "lexicographic";
      } else if (priority == PrioritizationLevel.NEG_MATCH_MODERATE) {
        bonuses = new double[] { 3, 1, .6, -.9, -1.15 };
        name = "negativeMatchModerate";
      } else if (priority == PrioritizationLevel.NEG_MATCH_EXTREME) {
        bonuses = new double[] { 3, 1, .6, -.5, -1.1 };
        name = "negativeMatchExtreme";
      } else {
        throw new RuntimeException("Unexpected prioritization level: "
            + priority);
      }
      PairMatchPowerBonus pmpBonus = new PairMatchPowerThesholdBonus(
          thresholds, bonuses);
      MaximumWeightedPackingMode ans = new ObjectiveMode.MaximumWeightedPackingMode(
          cycleBonus, defaultEdgeWeight, pmpBonus);
      ans.setName(name);
      return ans;
    } else if (metric == ObjectiveMetric.PRA) {
      PrioritizationLevel priority = safeGetPrioritization(metric,
          prioritization);
      double[] x = vpraApacheObjectiveBuckets;
      double[] y;
      String name;
      if (priority == PrioritizationLevel.TIE_BREAKER) {
        y = new double[] { .0005, .001, .005, .01, .05 };
        name = "vpraTieBreaker";
      } else if (priority == PrioritizationLevel.MODERATE) {
        y = new double[] { .01, .1, .6, 1, 3 };
        name = "vpraModerate";
      } else if (priority == PrioritizationLevel.EXTREME) {
        y = new double[] { .01, 1, 3, 5, 10 };
        name = "vpraExtreme";
      } else if (priority == PrioritizationLevel.LEXICOGRAPHIC) {
        y = new double[] { .01, 3, 10, 50, 200 };
        name = "vpraLexicographic";
      } else {
        throw new RuntimeException("Unexpected prioritization level: "
            + priority);
      }

      ApacheStepFunction stepFunction = new ApacheStepFunction(
          new StepFunction(x, y));
      VpraWeightObjectiveMode ans = new VpraWeightObjectiveMode(cycleBonus,
          defaultEdgeWeight, stepFunction);
      ans.setName(name);
      return ans;

    } else {
      throw new RuntimeException("unknown objective metric: " + metric);
    }
  }

  private static PrioritizationLevel safeGetPrioritization(
      ObjectiveMetric metric, Optional<PrioritizationLevel> prioritization) {
    if (!prioritization.isPresent()) {
      throw new RuntimeException("Objective metric " + metric
          + " requires a prioritization level, but none found");
    }
    return prioritization.get();
  }

}
