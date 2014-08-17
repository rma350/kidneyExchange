package multiPeriodAnalysis;

import kepModeler.ObjectiveMode;
import kepProtos.KepProtos.Objective;
import kepProtos.KepProtos.ObjectiveMetric;
import kepProtos.KepProtos.PrioritizationLevel;

import com.google.common.base.Optional;

public abstract class ObjectiveBuilder {

  public abstract ObjectiveMode createObjectiveMode(ObjectiveMetric metric,
      Optional<PrioritizationLevel> prioritization, double cycleBonus);

  public ObjectiveMode createObjectiveMode(Objective objective,
      double cycleBonus) {
    return createObjectiveMode(
        objective.getMetric(),
        objective.hasPrioritization() ? Optional.of(objective
            .getPrioritization()) : Optional.<PrioritizationLevel> absent(),
        cycleBonus);

  }

}
