package protoModeler;

import java.util.List;
import java.util.Map;

import kepProtos.KepProtos;
import kepProtos.KepProtos.BloodType;
import kepProtos.KepProtos.DonorPredicate;
import kepProtos.KepProtos.EdgePredicate;
import kepProtos.KepProtos.EdgeStep;
import kepProtos.KepProtos.HLA;
import kepProtos.KepProtos.NodePredicate;
import kepProtos.KepProtos.ObjectiveFunction;
import kepProtos.KepProtos.PatientPredicate;
import kepProtos.KepProtos.Range;
import kepProtos.KepProtos.Relation;
import cern.colt.Arrays;

import com.google.common.collect.Lists;

public class ProtoObjectives {

  public static ObjectiveFunction maximumCardinality() {
    return ObjectiveFunction.newBuilder().setConstant(1).build();
  }

  public static ObjectiveFunction patientPairMatchPower(List<Range> steps,
      double[] values) {
    validateSize(steps, values);
    validateBounds(0, steps, 1);
    ObjectiveFunction.Builder ans = ObjectiveFunction.newBuilder().setConstant(
        1);
    for (int i = 0; i < values.length; i++) {
      ans.addEdgeStepBuilder().setScore(values[i]).getEdgeConjunctionBuilder()
          .addEdgePredicateBuilder().getTargetBuilder()
          .setHistoricPatientPower(steps.get(i));
    }
    return ans.build();
  }

  public static List<EdgeStep> patientBloodType(
      Map<BloodType, Double> bloodTypeWeights) {
    List<EdgeStep> ans = Lists.newArrayList();
    EdgeStep.Builder edgeStep = EdgeStep.newBuilder();
    PatientPredicate.Builder patient = edgeStep.getEdgeConjunctionBuilder()
        .addEdgePredicateBuilder().getTargetBuilder()
        .getPatientPredicateBuilder();
    for (Map.Entry<KepProtos.BloodType, Double> bloodTypeEntry : bloodTypeWeights
        .entrySet()) {
      edgeStep.setScore(bloodTypeEntry.getValue());
      patient.clearBloodType();
      patient.addBloodType(bloodTypeEntry.getKey());
      ans.add(edgeStep.build());
    }
    return ans;
  }

  /**
   * Score will be (baseScore + bloodTypeWeight.get(bloodType))*(1-sum_{h in
   * hla}(I(homozygous in h)*homozygousPenalty.get(h)))
   * 
   * @param homozygousPenalty
   *          sum of penalties should be less than one, otherwise edge weights
   *          might be negative.
   * @param bloodTypeWeights
   * @return
   */
  public static List<EdgeStep> donorHomozygousHlaPenalty(
      Map<HLA, Double> homozygousPenalty,
      Map<BloodType, Double> bloodTypeWeights, double baseScore) {
    List<EdgeStep> ans = Lists.newArrayList();

    // contributes bloodTypeWeight.get(bloodType)*1
    List<EdgeStep> donorEdgeBloodTypeScores = donorEdgeBloodType(bloodTypeWeights);
    ans.addAll(donorEdgeBloodTypeScores);

    for (Map.Entry<HLA, Double> hlaEntry : homozygousPenalty.entrySet()) {
      for (int i = 0; i < donorEdgeBloodTypeScores.size(); i++) {
        // contributes -(baseScore + bloodScore)*hlaEntry.value*I(homozygous in
        // hlaEntry.key)
        EdgeStep.Builder edgeStep = donorEdgeBloodTypeScores.get(i).toBuilder();
        edgeStep.setScore(-(baseScore + edgeStep.getScore())
            * hlaEntry.getValue());
        edgeStep.getEdgeConjunctionBuilder().setRelation(Relation.AND);
        DonorPredicate.Builder donor = edgeStep.getEdgeConjunctionBuilder()
            .addEdgePredicateBuilder().getEdgeDonorPredicateBuilder();
        donor.addHomozygous(hlaEntry.getKey());
        ans.add(edgeStep.build());
      }
    }
    return ans;
  }

  /**
   * Warning: this doesn't really treat the case of multiple donors properly. We
   * really should be using the minimum score of any donor at the node, not the
   * donor used for the edge.
   * */
  public static List<EdgeStep> donorEdgeBloodType(
      Map<BloodType, Double> bloodTypeWeights) {
    List<EdgeStep> ans = Lists.newArrayList();
    EdgeStep.Builder edgeStep = EdgeStep.newBuilder();
    DonorPredicate.Builder donor = edgeStep.getEdgeConjunctionBuilder()
        .addEdgePredicateBuilder().getEdgeDonorPredicateBuilder();
    for (Map.Entry<KepProtos.BloodType, Double> bloodTypeEntry : bloodTypeWeights
        .entrySet()) {
      edgeStep.setScore(bloodTypeEntry.getValue());
      donor.clearBloodType();
      donor.addBloodType(bloodTypeEntry.getKey());
      ans.add(edgeStep.build());
    }
    return ans;
  }

  /**
   * DANGER! If rangeBuilder isn't a sub-builder of edgeStepBuilder, this won't
   * work. If you don't know what a sub-builder is, it is probably best to stay
   * away from this method to avoid incorrect behavior.
   * 
   * @param edgeStepBuilder
   *          A
   * @param rangeBuilder
   *          Must be a nested builder inside edgeStepBuilder, corresponding to
   *          the attribute that you want to test.
   * @param scores
   *          The value of assigned to edges falling in the ith bucket.
   * @param ranges
   *          A list of buckets to see
   * @return
   */
  public static class StepFunctionBuilder {
    private EdgeStep.Builder edgeStepBuilder;
    private KepProtos.Range.Builder rangeBuilder;

    public StepFunctionBuilder() {
      this.edgeStepBuilder = EdgeStep.newBuilder();
    }

    public EdgeStep.Builder getStepBuilder() {
      return this.edgeStepBuilder;
    }

    public EdgePredicate.Builder getEdgePredicateBuilder() {
      return this.edgeStepBuilder.getEdgeConjunctionBuilder()
          .addEdgePredicateBuilder();
    }

    public void setRangeBuilder(KepProtos.Range.Builder rangeBuilder) {
      this.rangeBuilder = rangeBuilder;
    }

    public List<EdgeStep> build(double[] scores, List<KepProtos.Range> ranges) {
      ProtoObjectives.validateSize(ranges, scores);
      List<EdgeStep> ans = Lists.newArrayList();
      for (int i = 0; i < scores.length; i++) {
        edgeStepBuilder.setScore(scores[i]);
        rangeBuilder.clear().mergeFrom(ranges.get(i));
        ans.add(edgeStepBuilder.build());
      }
      return ans;
    }
  }

  public static List<EdgeStep> makeLinearWaitingTimeScore(int daysPerBucket,
      double pointsPerBucket, int numBuckets) {
    List<EdgeStep> ans = Lists.newArrayList();
    double[] dividers = makeDividers(daysPerBucket, daysPerBucket,
        numBuckets - 1);
    List<Range> steps = makeSteps(dividers);
    EdgeStep.Builder edgeStepBuilder = EdgeStep.newBuilder();
    NodePredicate.Builder patientNodeBuilder = edgeStepBuilder
        .getEdgeConjunctionBuilder().addEdgePredicateBuilder()
        .getTargetBuilder();
    for (int i = 0; i < steps.size(); i++) {
      edgeStepBuilder.setScore(i * pointsPerBucket);
      patientNodeBuilder.setWaitingTime(steps.get(i));
      ans.add(edgeStepBuilder.build());
    }
    return ans;
  }

  /**
   * 
   * @param bucketDividers
   *          an array of n dividers
   * @return n+1 ranges, for values [-infty, dividers[0]),
   *         [dividers[0],dividers[1]),...,[dividers[n-2], dividers[n-1]),
   *         [dividers[n-1],+infty)
   */
  public static List<Range> makeSteps(double[] bucketDividers) {
    validateIncreasing(bucketDividers);
    List<Range> ans = Lists.newArrayList();
    for (int i = 0; i <= bucketDividers.length; i++) {
      Range.Builder range = Range.newBuilder();
      if (i != 0) {
        range.setLowerBound(bucketDividers[i - 1]);
      }
      if (i != bucketDividers.length) {
        range.setUpperBound(bucketDividers[i]);
      }
      ans.add(range.build());
    }
    return ans;
  }

  private static double[] makeDividers(double low, double increment, int count) {
    double[] ans = new double[count];
    for (int i = 0; i < ans.length; i++) {
      ans[i] = low + i * increment;
    }
    return ans;
  }

  private static void validateBounds(double lower, List<Range> steps,
      double upper) {
    if (steps.get(0).getUpperBound() <= lower) {
      throw new RuntimeException("First bucket must be greater than " + lower
          + ", but was " + steps.get(0).getUpperBound());
    }
    if (steps.get(steps.size() - 1).getLowerBound() >= upper) {
      throw new RuntimeException("Last bucket must be less than " + upper
          + ", but was " + steps.get(steps.size() - 1).getLowerBound());
    }
  }

  public static void validateSize(List<Range> steps, double[] values) {
    if (steps.size() != values.length) {
      throw new RuntimeException(
          "Number of steps must be equal to number of values, but found "
              + steps.size() + " steps and " + values.length + " values");
    }
  }

  private static void validateSize(double[] bucketDividers, double[] values) {
    if (bucketDividers.length == 0) {
      throw new RuntimeException("Must have at least one bucket divider");
    }
    if (bucketDividers.length != values.length - 1) {
      throw new RuntimeException(
          "Must have exacty one more value than bucket, but have "
              + bucketDividers.length + " buckets and " + values.length
              + " values.");
    }
  }

  private static void validateDecreasing(double[] data) {
    for (int i = 0; i < data.length - 1; i++) {
      if (data[i] <= data[i + 1]) {
        throw new RuntimeException(
            "Expected input to have decreasing values, but failed at " + i
                + " to " + (i + 1) + " on array: " + Arrays.toString(data));
      }
    }
  }

  private static void validateIncreasing(double[] data) {
    for (int i = 0; i < data.length - 1; i++) {
      if (data[i] >= data[i + 1]) {
        throw new RuntimeException(
            "Expected input to have increasing values, but failed at " + i
                + " to " + (i + 1) + " on array: " + Arrays.toString(data));
      }
    }
  }

}
