package protoModeler;

import java.util.List;

import kepProtos.KepProtos.EdgeConjunction;
import kepProtos.KepProtos.EdgePredicate;
import kepProtos.KepProtos.EdgeStep;
import kepProtos.KepProtos.ObjectiveFunction;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class ProtoObjectiveBuilder<V, E> {

  private PredicateBuilder<V, E> predicateBuilder;

  private final List<Step<E>> steps;

  private Function<E, Double> objectiveFunction;

  public ProtoObjectiveBuilder(PredicateBuilder<V, E> predicateBuilder,
      final ObjectiveFunction protoObjective) {
    this.predicateBuilder = predicateBuilder;
    this.steps = Lists.newArrayList();
    for (EdgeStep protoStep : protoObjective.getEdgeStepList()) {
      steps.add(new Step<E>(createConjunction(protoStep.getEdgeConjunction()),
          protoStep.getScore()));
    }
    this.objectiveFunction = new Function<E, Double>() {
      public Double apply(E edge) {
        double ans = protoObjective.getConstant();
        for (Step<E> step : steps) {
          if (step.getPredicate().apply(edge)) {
            ans += step.getScore();
          }
        }
        return ans;
      }
    };
  }

  public Function<E, Double> getObjectiveFunction() {
    return this.objectiveFunction;
  }

  public Predicate<E> createConjunction(EdgeConjunction edgeConjunction) {
    List<Predicate<E>> toMerge = Lists.newArrayList();
    for (EdgePredicate edgePredicate : edgeConjunction.getEdgePredicateList()) {
      toMerge.addAll(predicateBuilder.makeEdgePredicate(edgePredicate));
    }
    for (EdgeConjunction conjunction : edgeConjunction.getEdgeConjunctionList()) {
      toMerge.add(createConjunction(conjunction));
    }
    return ProtoUtil.combinePredicates(toMerge, edgeConjunction.getRelation());
  }

  private static class Step<E> {
    private Predicate<E> predicate;
    private double score;

    public Step(Predicate<E> predicate, double score) {
      super();
      this.score = score;
      this.predicate = predicate;
    }

    public Predicate<E> getPredicate() {
      return predicate;
    }

    public double getScore() {
      return score;
    }
  }

}
