package inputOutput.aggregate;

import inputOutput.aggregate.TrialReport.NodeAggregation;
import inputOutput.aggregate.TrialReport.TrialOutcome;
import inputOutput.aggregate.TrialReport.UnosTrialOutcome;
import inputOutput.core.Attribute;
import inputOutput.core.SumStatVal;

import java.util.List;

import multiPeriod.MultiPeriodCyclePacking.EffectiveNodeType;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import data.BloodType;
import data.Race;

public class UnosAgg {

  public static List<UnosCountPairedMatchedPatients> bloodTypePairedNodesMatched(
      final boolean isMatched, String name) {
    List<UnosCountPairedMatchedPatients> ans = Lists.newArrayList();
    for (final BloodType bloodType : BloodType.values()) {
      ans.add(new UnosCountPairedMatchedPatients(name + "=" + bloodType) {
        @Override
        protected Predicate<UnosExchangeUnit> getPatientPredicate(
            UnosTrialOutcome trialOutcome) {
          return trialOutcome.getInputPredicates()
              .patientBloodTypeIs(bloodType);
        }
      });
    }
    return ans;
  }

  private final static ImmutableList<Race> unosRaceClassifications = ImmutableList
      .of(Race.ASIAN, Race.BLACK, Race.HISPANIC, Race.WHITE, Race.MULTIRACIAL,
          Race.NATIVE_AMERICAN, Race.PACIFIC_ISLANDER);

  public static List<UnosCountPairedMatchedPatients> racePairedNodesMatched(
      final boolean isMatched) {
    List<UnosCountPairedMatchedPatients> ans = Lists.newArrayList();
    for (final Race race : unosRaceClassifications) {
      ans.add(new UnosCountPairedMatchedPatients("race=" + race) {
        @Override
        protected Predicate<UnosExchangeUnit> getPatientPredicate(
            UnosTrialOutcome trialOutcome) {
          return trialOutcome.getInputPredicates().patientRaceIs(race);
        }
      });
    }
    return ans;
  }

  public static UnosCountPairedMatchedPatients pediatricsMatched() {
    return new UnosCountPairedMatchedPatients("pediatricsMatched") {
      @Override
      protected Predicate<UnosExchangeUnit> getPatientPredicate(
          UnosTrialOutcome trialOutcome) {
        return trialOutcome.getInputPredicates().patientIsPediatric(18);
      }
    };
  }

  public static UnosCountPairedMatchedPatients unosPraInMatched(
      final Range<Double> range) {
    return new UnosCountPairedMatchedPatients("pairedMatchedUnosPraIn"
        + range.toString()) {
      @Override
      protected Predicate<UnosExchangeUnit> getPatientPredicate(
          UnosTrialOutcome trialOutcome) {
        return trialOutcome.getInputPredicates().unosCalcPatientPraIn(range);
      }
    };
  }

  public static UnosCountPairedMatchedPatients wasSameCenter() {
    return new UnosCountPairedMatchedPatients("sameCenter") {
      @Override
      protected Predicate<UnosExchangeUnit> getPatientPredicate(
          UnosTrialOutcome trialOutcome) {
        return trialOutcome.getOutputPredicates().matchDonorIsSameCenter();
      }
    };
  }

  public static UnosCountPairedMatchedPatients derivedPraInMatched(
      final Range<Double> range) {
    return new UnosCountPairedMatchedPatients("pairedMatchedDerivedPraIn"
        + range.toString()) {
      @Override
      protected Predicate<UnosExchangeUnit> getPatientPredicate(
          UnosTrialOutcome trialOutcome) {
        return trialOutcome.getInputPredicates().derivedPatientHistoricPraIn(
            range);
      }
    };
  }

  public static UnosTotalWaitingTime waitingTimeUnosPatientPraIn(
      final Range<Double> range) {
    return new UnosTotalWaitingTime("waitingTimePatientPraIn" + range) {

      @Override
      protected Predicate<UnosExchangeUnit> getExtraPredicates(
          UnosTrialOutcome unosTrialOutcome) {
        return unosTrialOutcome.getInputPredicates()
            .unosCalcPatientPraIn(range);
      }
    };
  }

  public static abstract class UnosNodeAggregation extends
      NodeAggregation<UnosExchangeUnit, UnosDonorEdge, Double, Double> {

    protected UnosNodeAggregation(String attributeName, SumStatVal sumStatVal) {
      super(attributeName, sumStatVal);
    }

    @Override
    protected Attribute<? super UnosExchangeUnit, ? extends Number> getAttribute(
        TrialOutcome<UnosExchangeUnit, UnosDonorEdge, Double, Double> trialOutcome) {
      return getAttribute((UnosTrialOutcome) trialOutcome);
    }

    @Override
    protected Predicate<UnosExchangeUnit> getPredicate(
        TrialOutcome<UnosExchangeUnit, UnosDonorEdge, Double, Double> trialOutcome) {
      return getPredicate((UnosTrialOutcome) trialOutcome);
    }

    protected abstract Attribute<? super UnosExchangeUnit, ? extends Number> getAttribute(
        UnosTrialOutcome unosTrialOutcome);

    protected abstract Predicate<UnosExchangeUnit> getPredicate(
        UnosTrialOutcome unosTrialOutcome);
  }

  public static abstract class UnosTotalWaitingTime extends UnosNodeAggregation {

    protected UnosTotalWaitingTime(String attributeName) {
      super(attributeName, SumStatVal.MEAN);
    }

    @Override
    protected Attribute<? super UnosExchangeUnit, Integer> getAttribute(
        UnosTrialOutcome unosTrialOutcome) {
      return Agg.wrapDays(unosTrialOutcome.getAttributes()
          .getNodeOutputAttributes().getTotalReceiverWaitingTime(),
          Agg.doubleDaysToDays);
    }

    @Override
    protected Predicate<UnosExchangeUnit> getPredicate(
        UnosTrialOutcome unosTrialOutcome) {
      return Predicates.and(unosTrialOutcome.getInputPredicates()
          .effectiveNodeTypeIs(EffectiveNodeType.paired),
          getExtraPredicates(unosTrialOutcome));
    }

    protected abstract Predicate<UnosExchangeUnit> getExtraPredicates(
        UnosTrialOutcome unosTrialOutcome);

  }

  public abstract static class UnosCountPairedMatchedPatients extends
      UnosNodeAggregation {

    protected UnosCountPairedMatchedPatients(String attributeName) {
      super(attributeName, SumStatVal.COUNT);
    }

    @Override
    protected Attribute<? super UnosExchangeUnit, ? extends Number> getAttribute(
        UnosTrialOutcome unosTrialOutcome) {
      return Agg.count;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Predicate<UnosExchangeUnit> getPredicate(
        UnosTrialOutcome unosTrialOutcome) {
      return Predicates.and(unosTrialOutcome.getInputPredicates()
          .effectiveNodeTypeIs(EffectiveNodeType.paired), unosTrialOutcome
          .getOutputPredicates().receiverIsMatched(),
          getPatientPredicate(unosTrialOutcome));
    }

    protected abstract Predicate<UnosExchangeUnit> getPatientPredicate(
        UnosTrialOutcome trialOutcome);

  }

}
