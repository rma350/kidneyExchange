package inputOutput.aggregate;

import inputOutput.ExchangePredicates.InputPredicates;
import inputOutput.ExchangePredicates.OutputPredicates;
import inputOutput.aggregate.TrialReport.ExchangeUnitTrialOutcome;
import inputOutput.aggregate.TrialReport.NodeAggregation;
import inputOutput.aggregate.TrialReport.TrialOutcome;
import inputOutput.core.Attribute;
import inputOutput.core.SumStatVal;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import multiPeriod.MultiPeriodCyclePacking.EffectiveNodeType;

import org.joda.time.Interval;
import org.joda.time.PeriodType;
import org.joda.time.ReadableInstant;

import replicator.DonorEdge;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Range;
import com.google.common.math.DoubleMath;

import data.ExchangeUnit;
import data.Receiver;

public class Agg {

  public static abstract class HistMaker<V, E, T extends Comparable<T>, D> {
    private List<NodeAggregation<V, E, T, D>> histBins;

    public List<NodeAggregation<V, E, T, D>> getHistBins() {
      return histBins;
    }

    protected HistMaker(String base, double[] bounds) {
      histBins = new ArrayList<NodeAggregation<V, E, T, D>>();
      for (int i = 0; i < bounds.length - 1; i++) {
        final Range<Double> range = Range.closedOpen(bounds[i], bounds[i + 1]);
        String binName = base + "CountIn[" + range.lowerEndpoint() + "-"
            + range.upperEndpoint() + ")";
        histBins
            .add(new NodeAggregation<V, E, T, D>(binName, SumStatVal.COUNT) {
              @Override
              protected Attribute<V, ? extends Number> getAttribute(
                  TrialOutcome<V, E, T, D> trialOutcome) {
                return getAttributeHist(trialOutcome);
              }

              @Override
              protected Predicate<V> getPredicate(
                  TrialOutcome<V, E, T, D> trialOutcome) {
                return Predicates.and(getPredicateHist(trialOutcome),
                    Predicates.compose(range,
                        wrapDouble(getAttribute(trialOutcome))));
              }
            });
      }
    }

    protected abstract Attribute<V, ? extends Number> getAttributeHist(
        TrialOutcome<V, E, T, D> trialOutcome);

    protected abstract Predicate<V> getPredicateHist(
        TrialOutcome<V, E, T, D> trialOutcome);
  }

  public static abstract class ExchangeUnitHistMaker extends
      HistMaker<ExchangeUnit, DonorEdge, ReadableInstant, Interval> {

    protected ExchangeUnitHistMaker(String base, double[] bounds) {
      super(base, bounds);
    }

    // TODO this really is not the right way to do this... not obvious how to
    // fix it easily
    @Override
    protected Attribute<ExchangeUnit, ? extends Number> getAttributeHist(
        TrialOutcome<ExchangeUnit, DonorEdge, ReadableInstant, Interval> trialOutcome) {
      return getAttributeHistExchangeUnit((ExchangeUnitTrialOutcome) trialOutcome);
    }

    protected abstract Attribute<ExchangeUnit, ? extends Number> getAttributeHistExchangeUnit(
        ExchangeUnitTrialOutcome trialOutcome);

  }

  public static <V, E, T extends Comparable<T>, D> List<NodeAggregation<V, E, T, D>> pairMatchPowerHist(
      String name, double[] bounds, final boolean matched) {
    HistMaker<V, E, T, D> hist = new HistMaker<V, E, T, D>(name, bounds) {
      @Override
      protected Attribute<V, ? extends Number> getAttributeHist(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return trialOutcome.getAttributes().getNodeInputAttributes()
            .getPairMatchPowerPostPreferences();
      }

      @Override
      protected Predicate<V> getPredicateHist(
          TrialOutcome<V, E, T, D> trialOutcome) {
        OutputPredicates<V, E, T, D> outPred = trialOutcome
            .getOutputPredicates();
        Predicate<V> paired = trialOutcome.getInputPredicates()
            .effectiveNodeTypeIs(EffectiveNodeType.paired);
        Predicate<V> matchStatus = outPred.receiverIsMatched();
        if (!matched) {
          matchStatus = Predicates.not(matchStatus);
        }
        return Predicates.and(paired, matchStatus);
      }
    };
    return hist.getHistBins();
  }

  public static List<NodeAggregation<ExchangeUnit, DonorEdge, ReadableInstant, Interval>> virtualPraHist(
      String name, double[] bounds, final boolean matched) {
    ExchangeUnitHistMaker hist = new ExchangeUnitHistMaker(name, bounds) {
      @Override
      protected Attribute<ExchangeUnit, ? extends Number> getAttributeHistExchangeUnit(
          ExchangeUnitTrialOutcome trialOutcome) {
        Attribute<Receiver, Double> vpra = trialOutcome.getAttributes()
            .getReceiverInputAttributes().getVpra();
        return trialOutcome.getAttributes().getExchangeInputAttributes()
            .makeReceiverNodeAttribute(vpra);
      }

      @Override
      protected Predicate<ExchangeUnit> getPredicateHist(
          TrialOutcome<ExchangeUnit, DonorEdge, ReadableInstant, Interval> trialOutcome) {
        InputPredicates<ExchangeUnit, DonorEdge, ReadableInstant, Interval> outPred = trialOutcome
            .getInputPredicates();
        Predicate<ExchangeUnit> paired = outPred
            .effectiveNodeTypeIs(EffectiveNodeType.paired);
        Predicate<ExchangeUnit> matchStatus = trialOutcome
            .getOutputPredicates().receiverIsMatched();
        if (!matched) {
          matchStatus = Predicates.not(matchStatus);
        }
        return Predicates.and(paired, matchStatus);
      }
    };
    return hist.getHistBins();
  }

  public static Attribute<Object, Integer> count = new Attribute<Object, Integer>() {
    @Override
    public Integer apply(Object input) {
      return 1;
    }
  };

  // TODO: create an input aggreation class and move this there, as this is only
  // a function of the input.
  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> pairedNodesInSimulationArrivedAfterStart() {
    return pairedNodesInSimulationArrivedAfterStart("numPairedArrivals");
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> pairedNodesInSimulationArrivedAfterStart(
      String name) {
    return new NodeAggregation<V, E, T, D>(name, SumStatVal.COUNT) {
      @Override
      protected Attribute<? super V, ? extends Number> getAttribute(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return count;
      }

      @Override
      protected Predicate<V> getPredicate(TrialOutcome<V, E, T, D> trialOutcome) {
        return Predicates.and(trialOutcome.getInputPredicates()
            .effectiveNodeTypeIs(EffectiveNodeType.paired), trialOutcome
            .getInputPredicates().arrivedAfterSimulationStart());
      }
    };
  }

  public static <V, E, T extends Comparable<T>, D> CountPairedMatchedPatients<V, E, T, D> pairedNodesMatched() {
    return pairedNodesMatched("pairedMatched");
  }

  public static <V, E, T extends Comparable<T>, D> CountPairedMatchedPatients<V, E, T, D> pairedNodesMatched(
      String name) {
    return new CountPairedMatchedPatients<V, E, T, D>(name) {
      @Override
      protected Predicate<V> getExtraPredicate(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return Predicates.<V> alwaysTrue();
      }
    };
  }

  public static CountPairedMatchedPatients<ExchangeUnit, DonorEdge, ReadableInstant, Interval> vPraPairedNodesMatched(
      final Range<Double> vPraIn) {
    return vPraPairedNodesMatched("virtualPraIn" + vPraIn.toString()
        + "PairedMatched", vPraIn);
  }

  public static CountPairedMatchedPatients<ExchangeUnit, DonorEdge, ReadableInstant, Interval> vPraPairedNodesMatched(
      String name, final Range<Double> vPraIn) {
    return new CountPairedMatchedPatients<ExchangeUnit, DonorEdge, ReadableInstant, Interval>(
        name) {
      @Override
      protected Predicate<ExchangeUnit> getExtraPredicate(
          TrialOutcome<ExchangeUnit, DonorEdge, ReadableInstant, Interval> trialOutcome) {
        return ((ExchangeUnitTrialOutcome) trialOutcome).getInputPredicates()
            .virtualPraIn(vPraIn);
      }
    };
  }

  public static <V, E, T extends Comparable<T>, D> CountPairedMatchedPatients<V, E, T, D> sensitizedPairedNodesMatched(
      Range<Double> pmpIn) {
    return sensitizedPairedNodesMatched(pmpIn, "pmpIn" + pmpIn.toString()
        + "PairedMatched");
  }

  public static <V, E, T extends Comparable<T>, D> CountPairedMatchedPatients<V, E, T, D> sensitizedPairedNodesMatched(
      final Range<Double> pmpIn, String name) {
    return new CountPairedMatchedPatients<V, E, T, D>(name) {
      @Override
      protected Predicate<V> getExtraPredicate(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return trialOutcome.getInputPredicates()
            .postPreferencePairMatchPowerIn(pmpIn);
      }
    };
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> donorPowerPairedNodesMatched(
      Range<Double> donorPowerIn) {
    return donorPowerPairedNodesMatched(donorPowerIn, "donorPowerIn"
        + donorPowerIn.toString() + "PairedMatched");
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> donorPowerPairedNodesMatched(
      final Range<Double> donorPowerIn, String name) {
    return new NodeAggregation<V, E, T, D>(name, SumStatVal.COUNT) {
      @Override
      protected Attribute<? super V, ? extends Number> getAttribute(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return Agg.count;
      }

      @SuppressWarnings("unchecked")
      @Override
      protected Predicate<V> getPredicate(TrialOutcome<V, E, T, D> trialOutcome) {
        return Predicates.and(trialOutcome.getInputPredicates()
            .effectiveNodeTypeIs(EffectiveNodeType.paired), trialOutcome
            .getOutputPredicates().donorIsMatched(), trialOutcome
            .getInputPredicates().postPreferenceDonorPowerIn(donorPowerIn));
      }

    };
  }

  public static <V, E, T extends Comparable<T>, D> CountPairedMatchedPatients<V, E, T, D> patientPowerPairedNodesMatched(
      Range<Double> patientPowerIn) {
    return patientPowerPairedNodesMatched(patientPowerIn,
        "pairedMatchedPatientPowerIn" + patientPowerIn.toString());
  }

  public static <V, E, T extends Comparable<T>, D> CountPairedMatchedPatients<V, E, T, D> patientPowerPairedNodesMatched(
      final Range<Double> patientPowerIn, String name) {
    return new CountPairedMatchedPatients<V, E, T, D>(name) {
      @Override
      protected Predicate<V> getExtraPredicate(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return trialOutcome.getInputPredicates().postPreferenceReceiverPowerIn(
            patientPowerIn);
      }
    };
  }

  public static <V, E> NodeAggregation<V, E, Double, Double> totalDoubleWaitingTimeAtLeast(
      int minWaitingTimeDays) {
    return totalWaitingTimeAtLeast(minWaitingTimeDays, Agg.doubleDaysToDays);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> totalWaitingTimeAtLeast(
      final int minWaitingTimeDays, final Function<D, Integer> ellapsedDays) {
    return new NodeAggregation<V, E, T, D>("pairedTotalWaitAtLeast"
        + minWaitingTimeDays + "Days", SumStatVal.COUNT) {

      @Override
      protected Attribute<? super V, ? extends Number> getAttribute(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return Agg.count;
      }

      @Override
      protected Predicate<V> getPredicate(TrialOutcome<V, E, T, D> trialOutcome) {
        Predicate<V> waitingTimeDays = Predicates.compose(
            Range.<Integer> atLeast(minWaitingTimeDays),
            Functions.compose(ellapsedDays, trialOutcome.getAttributes()
                .getNodeOutputAttributes().getTotalReceiverWaitingTime()));
        return Predicates
            .and(waitingTimeDays, trialOutcome.getInputPredicates()
                .effectiveNodeTypeIs(EffectiveNodeType.paired));
      }
    };
  }

  public static <V, E> NodeAggregation<V, E, ReadableInstant, Interval> pairedTotalWaitingTimeJoda() {
    return pairedTotalWaitingTime(Agg.intervalToDays);
  }

  public static <V, E> NodeAggregation<V, E, Double, Double> pairedTotalWaitingTimeDoubleTime() {
    return pairedTotalWaitingTime(Agg.doubleDaysToDays);
  }

  public static <V, E> NodeAggregation<V, E, Double, Double> pairedTotalWaitingTimeDoubleTime(
      SumStatVal sumStatVal) {
    return pairedTotalWaitingTime("pairedTotalWaiting", Agg.doubleDaysToDays,
        sumStatVal);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> pairedTotalWaitingTime(
      Function<D, Integer> computeDays) {
    return pairedTotalWaitingTime("pairedTotalWaiting", computeDays);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> pairedTotalWaitingTime(
      String name, final Function<D, Integer> computeDays) {
    return pairedTotalWaitingTime(name, computeDays, SumStatVal.MEAN);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> pairedTotalWaitingTime(
      String name, final Function<D, Integer> computeDays, SumStatVal sumStatVal) {
    return new NodeAggregation<V, E, T, D>(name, sumStatVal) {
      @Override
      protected Attribute<V, ? extends Number> getAttribute(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return wrapDays(trialOutcome.getAttributes().getNodeOutputAttributes()
            .getTotalReceiverWaitingTime(), computeDays);
      }

      @Override
      protected Predicate<V> getPredicate(TrialOutcome<V, E, T, D> trialOutcome) {
        return trialOutcome.getInputPredicates().effectiveNodeTypeIs(
            EffectiveNodeType.paired);
      }
    };
  }

  public static <V, E> NodeAggregation<V, E, ReadableInstant, Interval> sensitizedPairedTotalWaitingTimeJoda(
      Range<Double> pmpIn) {
    return sensitizedPairedTotalWaitingTime(pmpIn, Agg.intervalToDays);
  }

  public static <V, E> NodeAggregation<V, E, Double, Double> sensitizedPairedTotalWaitingTimeDoubleTime(
      Range<Double> pmpIn) {
    return sensitizedPairedTotalWaitingTime(pmpIn, Agg.doubleDaysToDays);
  }

  public static <V, E> NodeAggregation<V, E, Double, Double> sensitizedPairedTotalWaitingTimeDoubleTime(
      Range<Double> pmpIn, SumStatVal sumStatVal) {
    return sensitizedPairedTotalWaitingTime(pmpIn, "pmp" + sumStatVal + "In"
        + pmpIn, Agg.doubleDaysToDays, sumStatVal);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> sensitizedPairedTotalWaitingTime(
      Range<Double> pmpIn, final Function<D, Integer> computeDays) {
    return sensitizedPairedTotalWaitingTime(pmpIn, "pmpIn" + pmpIn.toString()
        + "PairedTotalWaiting", computeDays);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> sensitizedPairedTotalWaitingTime(
      final Range<Double> pmpIn, String name,
      final Function<D, Integer> computeDays) {
    return sensitizedPairedTotalWaitingTime(pmpIn, name, computeDays,
        SumStatVal.MEAN);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> sensitizedPairedTotalWaitingTime(
      final Range<Double> pmpIn, String name,
      final Function<D, Integer> computeDays, SumStatVal sumStatVal) {
    return new NodeAggregation<V, E, T, D>(name, sumStatVal) {
      @Override
      protected Attribute<V, ? extends Number> getAttribute(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return wrapDays(trialOutcome.getAttributes().getNodeOutputAttributes()
            .getTotalReceiverWaitingTime(), computeDays);
      }

      @Override
      protected Predicate<V> getPredicate(TrialOutcome<V, E, T, D> trialOutcome) {
        return Predicates.and(trialOutcome.getInputPredicates()
            .effectiveNodeTypeIs(EffectiveNodeType.paired), trialOutcome
            .getInputPredicates().postPreferencePairMatchPowerIn(pmpIn));
      }
    };
  }

  public static <V, E> NodeAggregation<V, E, Double, Double> patientPowerPairedTotalWaitingTimeDoubleTime(
      Range<Double> pmpIn) {
    return patientPowerPairedTotalWaitingTime(pmpIn,
        "totalWaitingPatientPowerIn" + pmpIn, Agg.doubleDaysToDays,
        SumStatVal.MEAN);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> patientPowerPairedTotalWaitingTime(
      final Range<Double> patientPowerIn, String name,
      final Function<D, Integer> computeDays, SumStatVal sumStatVal) {
    return new NodeAggregation<V, E, T, D>(name, sumStatVal) {
      @Override
      protected Attribute<V, ? extends Number> getAttribute(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return wrapDays(trialOutcome.getAttributes().getNodeOutputAttributes()
            .getTotalReceiverWaitingTime(), computeDays);
      }

      @Override
      protected Predicate<V> getPredicate(TrialOutcome<V, E, T, D> trialOutcome) {
        return Predicates
            .and(
                trialOutcome.getInputPredicates().effectiveNodeTypeIs(
                    EffectiveNodeType.paired),
                trialOutcome.getInputPredicates()
                    .postPreferenceReceiverPowerIn(patientPowerIn));
      }
    };
  }

  public static NodeAggregation<ExchangeUnit, DonorEdge, ReadableInstant, Interval> vpraPairedTotalWaitingTime(
      Range<Double> vpraIn) {
    return vpraPairedTotalWaitingTime(vpraIn, "vpraIn" + vpraIn.toString()
        + "PairedTotalWaiting");
  }

  public static NodeAggregation<ExchangeUnit, DonorEdge, ReadableInstant, Interval> vpraPairedTotalWaitingTime(
      final Range<Double> vpraIn, String name) {
    return new NodeAggregation<ExchangeUnit, DonorEdge, ReadableInstant, Interval>(
        name) {
      @Override
      protected Attribute<ExchangeUnit, ? extends Number> getAttribute(
          TrialOutcome<ExchangeUnit, DonorEdge, ReadableInstant, Interval> trialOutcome) {
        return wrapDays(trialOutcome.getAttributes().getNodeOutputAttributes()
            .getTotalReceiverWaitingTime(), Agg.intervalToDays);
      }

      @Override
      protected Predicate<ExchangeUnit> getPredicate(
          TrialOutcome<ExchangeUnit, DonorEdge, ReadableInstant, Interval> trialOutcome) {
        return Predicates.and(trialOutcome.getInputPredicates()
            .effectiveNodeTypeIs(EffectiveNodeType.paired),
            ((ExchangeUnitTrialOutcome) trialOutcome).getInputPredicates()
                .virtualPraIn(vpraIn));
      }
    };
  }

  public static <V, E> NodeAggregation<V, E, ReadableInstant, Interval> pairedMatchedNodesWaitingJoda() {
    return pairedMatchedNodesWaiting(Agg.intervalToDays);
  }

  public static <V, E> NodeAggregation<V, E, Double, Double> pairedMatchedNodesWaitingDoubleDays() {
    return pairedMatchedNodesWaiting(Agg.doubleDaysToDays);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> pairedMatchedNodesWaiting(
      Function<D, Integer> days) {
    return pairedMatchedNodesWaiting("pairedAndMatchedWaiting", days);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> pairedMatchedNodesWaiting(
      String name, final Function<D, Integer> days) {
    return new NodeAggregation<V, E, T, D>(name) {
      @Override
      protected Attribute<V, ? extends Number> getAttribute(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return wrapDays(trialOutcome.getAttributes().getNodeOutputAttributes()
            .getReceiverWaitingTime(), days);
      }

      @Override
      protected Predicate<V> getPredicate(TrialOutcome<V, E, T, D> trialOutcome) {
        return Predicates.and(trialOutcome.getInputPredicates()
            .effectiveNodeTypeIs(EffectiveNodeType.paired), trialOutcome
            .getOutputPredicates().receiverIsMatched());
      }
    };
  }

  public static <V, E> NodeAggregation<V, E, ReadableInstant, Interval> sensitizedPairedMatchedNodesWaitingJoda(
      Range<Double> pmpIn, boolean isMatched) {
    return sensitizedPairedMatchedNodesWaiting(pmpIn, isMatched,
        Agg.intervalToDays);
  }

  public static <V, E> NodeAggregation<V, E, Double, Double> sensitizedPairedMatchedNodesWaitingDoubleTime(
      Range<Double> pmpIn, boolean isMatched) {
    return sensitizedPairedMatchedNodesWaiting(pmpIn, isMatched,
        Agg.doubleDaysToDays);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> sensitizedPairedMatchedNodesWaiting(
      Range<Double> pmpIn, boolean isMatched, Function<D, Integer> dayEllpased) {
    return sensitizedPairedMatchedNodesWaiting(pmpIn, isMatched, "pmpIn"
        + pmpIn.toString() + "PairedAnd" + (isMatched ? "" : "Un")
        + "MatchedWaiting", dayEllpased);
  }

  public static <V, E, T extends Comparable<T>, D> NodeAggregation<V, E, T, D> sensitizedPairedMatchedNodesWaiting(
      final Range<Double> pmpIn, final boolean isMatched, String name,
      final Function<D, Integer> daysEllpased) {
    return new NodeAggregation<V, E, T, D>(name) {
      @Override
      protected Attribute<V, ? extends Number> getAttribute(
          TrialOutcome<V, E, T, D> trialOutcome) {
        return wrapDays(trialOutcome.getAttributes().getNodeOutputAttributes()
            .getReceiverWaitingTime(), daysEllpased);
      }

      @SuppressWarnings("unchecked")
      @Override
      protected Predicate<V> getPredicate(TrialOutcome<V, E, T, D> trialOutcome) {
        Predicate<V> matchPred = trialOutcome.getOutputPredicates()
            .receiverIsMatched();
        if (!isMatched) {
          matchPred = Predicates.not(matchPred);
        }
        return Predicates.and(
            trialOutcome.getInputPredicates().effectiveNodeTypeIs(
                EffectiveNodeType.paired),
            matchPred,
            trialOutcome.getInputPredicates().postPreferencePairMatchPowerIn(
                pmpIn));
      }
    };
  }

  public static NodeAggregation<ExchangeUnit, DonorEdge, ReadableInstant, Interval> vPraPairedMatchedNodesWaiting(
      Range<Double> vpraIn, boolean isMatched) {
    return vPraPairedMatchedNodesWaiting(vpraIn, isMatched,
        "vpraIn" + vpraIn.toString() + "PairedAnd" + (isMatched ? "" : "Un")
            + "MatchedWaiting");
  }

  public static NodeAggregation<ExchangeUnit, DonorEdge, ReadableInstant, Interval> vPraPairedMatchedNodesWaiting(
      final Range<Double> vpraIn, final boolean isMatched, String name) {
    return new NodeAggregation<ExchangeUnit, DonorEdge, ReadableInstant, Interval>(
        name) {
      @Override
      protected Attribute<ExchangeUnit, ? extends Number> getAttribute(
          TrialOutcome<ExchangeUnit, DonorEdge, ReadableInstant, Interval> trialOutcome) {
        return wrapDays(trialOutcome.getAttributes().getNodeOutputAttributes()
            .getReceiverWaitingTime(), Agg.intervalToDays);
      }

      @SuppressWarnings("unchecked")
      @Override
      protected Predicate<ExchangeUnit> getPredicate(
          TrialOutcome<ExchangeUnit, DonorEdge, ReadableInstant, Interval> trialOutcome) {
        Predicate<ExchangeUnit> matchPred = trialOutcome.getOutputPredicates()
            .receiverIsMatched();
        if (!isMatched) {
          matchPred = Predicates.not(matchPred);
        }
        return Predicates.and(trialOutcome.getInputPredicates()
            .effectiveNodeTypeIs(EffectiveNodeType.paired), matchPred,
            ((ExchangeUnitTrialOutcome) trialOutcome).getInputPredicates()
                .virtualPraIn(vpraIn));
      }
    };
  }

  // Util
  // section/////////////////////////////////////////////////////////////////////////////

  public static <V> Attribute<V, Integer> wrap(
      final Attribute<V, Boolean> attribute) {
    return wrap(attribute, true);
  }

  public static <V> Attribute<V, Integer> wrap(
      final Attribute<V, Boolean> attribute, final boolean failFast) {
    return new Attribute<V, Integer>() {

      @Override
      public Integer apply(V input) {
        Boolean val = attribute.apply(input);
        if (failFast && val == null) {
          throw new RuntimeException();
        } else if (val != null && val.booleanValue()) {
          return Integer.valueOf(1);
        } else {
          return Integer.valueOf(0);
        }
      }

    };
  }

  public static <V> Attribute<V, Double> wrapDouble(
      final Attribute<V, ? extends Number> attribute) {
    return wrapDouble(attribute, true);
  }

  public static <V> Attribute<V, Double> wrapDouble(
      final Attribute<V, ? extends Number> attribute, final boolean failFast) {
    return new Attribute<V, Double>() {

      @Override
      public Double apply(V input) {
        Number val = attribute.apply(input);
        if (failFast && val == null) {
          System.err.println(((ExchangeUnit) input).getExchangeUnitType());
          // throw new RuntimeException();
          return 0.0;
        } else if (val == null) {
          return null;
        } else {
          return val.doubleValue();
        }
      }

    };
  }

  public static <V, D> Attribute<V, Integer> wrapDays(
      final Attribute<V, D> attribute, final Function<D, Integer> computeDays) {
    return new Attribute<V, Integer>() {
      @Override
      public Integer apply(V input) {
        D days = attribute.apply(input);
        if (days == null) {
          throw new RuntimeException();
        } else {
          return computeDays.apply(days);
        }
      }
    };
  }

  public static final Function<Interval, Integer> intervalToDays = new Function<Interval, Integer>() {
    public Integer apply(Interval interval) {
      return interval.toPeriod(PeriodType.days()).getDays();
    }
  };

  public static final Function<Double, Integer> doubleDaysToDays = new Function<Double, Integer>() {
    public Integer apply(Double days) {
      return DoubleMath.roundToInt(days, RoundingMode.HALF_UP);
    }
  };

  /*
   * public static <V> Attribute<V, Integer> wrapDays( final Attribute<V,
   * Interval> attribute) { return wrapDays(attribute, true); }
   * 
   * public static <V> Attribute<V, Integer> wrapDays( final Attribute<V,
   * Interval> attribute, final boolean failFast) { return new Attribute<V,
   * Integer>() {
   * 
   * @Override public Integer apply(V input) { Interval val =
   * attribute.apply(input); if (val == null) { if (failFast) { throw new
   * RuntimeException(); } else { return 0; } } else { return
   * val.toPeriod(PeriodType.days()).getDays(); } }
   * 
   * }; }
   */

  public abstract static class CountPairedMatchedPatients<V, E, T extends Comparable<T>, D>
      extends NodeAggregation<V, E, T, D> {

    protected CountPairedMatchedPatients(String attributeName) {
      super(attributeName, SumStatVal.COUNT);
    }

    @Override
    protected Attribute<? super V, ? extends Number> getAttribute(
        TrialOutcome<V, E, T, D> trialOutcome) {
      return Agg.count;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Predicate<V> getPredicate(TrialOutcome<V, E, T, D> trialOutcome) {
      return Predicates.and(trialOutcome.getInputPredicates()
          .effectiveNodeTypeIs(EffectiveNodeType.paired), trialOutcome
          .getOutputPredicates().receiverIsMatched(),
          getExtraPredicate(trialOutcome));
    }

    protected abstract Predicate<V> getExtraPredicate(
        TrialOutcome<V, E, T, D> trialOutcome);
  }
}
