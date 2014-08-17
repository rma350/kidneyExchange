package protoModeler;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import kepProtos.KepProtos.EdgeStep;
import kepProtos.KepProtos.ObjectiveFunction;

import com.google.common.collect.ImmutableList;

public class UnosProtoObjectives {

  public static enum ObjFactor {
    // NOT DONE: Zero antigen mismatch gives 200 points

    /** PRA >= 80% gives 125 points. */
    PRA(makePRA()),
    // NOT DONE: Prior living donors get 150 points

    /** Patient age < 18 gives 100 points. */
    PEDIATRIC(makePediatric()),
    /**
     * Give 7 points for every 100 days of waiting (approximation of .07 points
     * per day of waiting. Code assumes that maximum waiting time is less than
     * 2000 days.
     */
    WAITING(makeWaiting()),
    // NOT DONE: 25 points for same OPTN region
    // NOT DONE: 50 points for same Donation Service Area

    /** Add 75 points to patients in the same center. */
    CENTER(makeCenter());
    // Should the OPTN/Donation service area/same center points be cummulative??
    // Presumably you only get one of the three bonuses.

    private ObjFactor(ImmutableList<EdgeStep> steps) {
      this.defaultEdgeSteps = steps;
    }

    private ImmutableList<EdgeStep> defaultEdgeSteps;

    public ImmutableList<EdgeStep> getDefaultSteps() {
      return this.defaultEdgeSteps;
    }
  }

  private static double baseEdgeValue = 200;

  public static ObjectiveFunction unosProposedObjective(
      Set<ObjFactor> objFactors) {
    // Base of 200 points
    ObjectiveFunction.Builder ans = ObjectiveFunction.newBuilder().setConstant(
        baseEdgeValue);
    for (ObjFactor factor : objFactors) {
      ans.addAllEdgeStep(factor.getDefaultSteps());
    }
    return ans.build();
  }

  public static ObjectiveFunction unosProposedObjective(
      Map<ObjFactor, Double> factorValues, boolean defaultOthers) {
    ObjectiveFunction.Builder ans = ObjectiveFunction.newBuilder().setConstant(
        baseEdgeValue);
    if (factorValues.containsKey(ObjFactor.CENTER)) {
      ans.addAllEdgeStep(makeCenter(factorValues.get(ObjFactor.CENTER)));
    }
    if (factorValues.containsKey(ObjFactor.PEDIATRIC)) {
      ans.addAllEdgeStep(makePediatric(factorValues.get(ObjFactor.PEDIATRIC)));
    }
    if (factorValues.containsKey(ObjFactor.PRA)) {
      ans.addAllEdgeStep(makePRA(factorValues.get(ObjFactor.PRA)));
    }
    if (factorValues.containsKey(ObjFactor.WAITING)) {
      ans.addAllEdgeStep(makeWaiting(factorValues.get(ObjFactor.WAITING)));
    }
    if (defaultOthers) {
      for (ObjFactor factor : ObjFactor.values()) {
        if (!factorValues.containsKey(factor)) {
          ans.addAllEdgeStep(factor.getDefaultSteps());
        }
      }
    }
    return ans.build();
  }

  public static ObjectiveFunction unosProposedObjective() {
    return unosProposedObjective(EnumSet.of(ObjFactor.PRA, ObjFactor.WAITING,
        ObjFactor.PEDIATRIC, ObjFactor.CENTER));
  }

  public static ImmutableList<EdgeStep> makePRA(double pointsPraAbove80) {
    EdgeStep.Builder ans = EdgeStep.newBuilder().setScore(pointsPraAbove80);
    ans.getEdgeConjunctionBuilder().addEdgePredicateBuilder()
        .getTargetBuilder().getPatientPredicateBuilder()
        .getHistoricPatientPraBuilder().setLowerBound(80);
    return ImmutableList.of(ans.build());
  }

  public static ImmutableList<EdgeStep> makePRA() {
    return makePRA(125);
  }

  public static ImmutableList<EdgeStep> makePediatric(double pointsUnder18) {
    EdgeStep.Builder ans = EdgeStep.newBuilder().setScore(pointsUnder18);
    ans.getEdgeConjunctionBuilder().addEdgePredicateBuilder()
        .getTargetBuilder().getPatientPredicateBuilder().getPatientAgeBuilder()
        .setUpperBound(18);
    return ImmutableList.of(ans.build());
  }

  public static ImmutableList<EdgeStep> makePediatric() {
    return makePediatric(100);
  }

  public static ImmutableList<EdgeStep> makeWaiting(double pointsPerHundredDays) {
    return ImmutableList.copyOf(ProtoObjectives.makeLinearWaitingTimeScore(100,
        pointsPerHundredDays, 20));
  }

  public static ImmutableList<EdgeStep> makeWaiting() {
    return makeWaiting(7);
  }

  public static ImmutableList<EdgeStep> makeCenter(double pointsCenterMatches) {
    EdgeStep.Builder ans = EdgeStep.newBuilder().setScore(pointsCenterMatches);
    ans.getEdgeConjunctionBuilder().addEdgePredicateBuilder()
        .setCheckSameCenter(true);
    return ImmutableList.of(ans.build());
  }

  public static ImmutableList<EdgeStep> makeCenter() {
    return makeCenter(75);
  }

}
