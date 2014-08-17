package inputOutput;

import inputOutput.Reports.ExchangeUnitInputAttributeSet;
import inputOutput.Reports.NodeInputAttributeSet;
import inputOutput.Reports.NodeOutputAttributeSet;
import inputOutput.Reports.UnosNodeAttributeSet;
import inputOutput.Reports.UnosOutputAttributeSet;
import inputOutput.core.Attribute;
import multiPeriod.MultiPeriodCyclePacking.EffectiveNodeType;

import org.joda.time.Interval;
import org.joda.time.ReadableInstant;

import replicator.DonorEdge;
import unosData.UnosDonor;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;
import unosData.UnosPatient;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Range;

import data.BloodType;
import data.ExchangeUnit;
import data.Race;
import data.Receiver;

public class ExchangePredicates {

  public static class ExchangeUnitInputPredicates extends
      InputPredicates<ExchangeUnit, DonorEdge, ReadableInstant, Interval> {

    public ExchangeUnitInputPredicates(
        ExchangeUnitInputAttributeSet attributeSet) {
      super(attributeSet);
      this.attributeSet = attributeSet;
    }

    private ExchangeUnitInputAttributeSet attributeSet;

    @Override
    public ExchangeUnitInputAttributeSet getNodeAttributeSet() {
      return this.attributeSet;
    }

    public Predicate<ExchangeUnit> virtualPraIn(Range<Double> range) {
      Attribute<Receiver, Double> vpra = this.attributeSet
          .getReceiverInputAttributes().getVpra();
      Attribute<ExchangeUnit, Double> vpraEx = attributeSet
          .getExchangeInputAttributes().makeReceiverNodeAttribute(vpra);
      return Predicates.compose(range, vpraEx);
    }

  }

  public static class UnosNodeInputPredicates extends
      InputPredicates<UnosExchangeUnit, UnosDonorEdge, Double, Double> {

    private UnosNodeAttributeSet unosNodeAttributeSet;

    public UnosNodeInputPredicates(UnosNodeAttributeSet unosNodeAttributeSet) {
      super(unosNodeAttributeSet);
      this.unosNodeAttributeSet = unosNodeAttributeSet;
    }

    @Override
    public UnosNodeAttributeSet getNodeAttributeSet() {
      return this.unosNodeAttributeSet;
    }

    public Predicate<UnosExchangeUnit> firstDonorBloodTypeIs(BloodType bloodType) {
      return fromFirstDonorAttribute(unosNodeAttributeSet
          .getUnosDonorInputAttributes().getBloodType(),
          Predicates.equalTo(bloodType));
    }

    private <T> Predicate<UnosExchangeUnit> fromFirstDonorAttribute(
        Attribute<UnosDonor, T> donorAttribute, Predicate<T> predicate) {
      return Predicates.compose(predicate,
          unosNodeAttributeSet.getUnosNodeInputAttributes()
              .makeDonorNodeAttribute(donorAttribute, 0));
    }

    private <T> Predicate<UnosExchangeUnit> fromPatientAttribute(
        Attribute<UnosPatient, T> patientAttribute, Predicate<T> predicate) {
      return Predicates.compose(predicate,
          unosNodeAttributeSet.getUnosNodeInputAttributes()
              .makePatientNodeAttribute(patientAttribute));
    }

    public Predicate<UnosExchangeUnit> exactlyOneDonor() {
      return Predicates.compose(Predicates.equalTo(Integer.valueOf(1)),
          unosNodeAttributeSet.getUnosNodeInputAttributes().getNumDonors());
    }

    public Predicate<UnosExchangeUnit> derivedPatientHistoricPraIn(
        Range<Double> range) {
      return fromPatientAttribute(unosNodeAttributeSet
          .getUnosPatientInputAttributes().getDerivedPra(), range);
    }

    public Predicate<UnosExchangeUnit> unosCalcPatientPraIn(Range<Double> range) {
      return fromPatientAttribute(unosNodeAttributeSet
          .getUnosPatientInputAttributes().getPra(), range);
    }

    public Predicate<UnosExchangeUnit> patientBloodTypeIs(BloodType bloodType) {
      return fromPatientAttribute(unosNodeAttributeSet
          .getUnosPatientInputAttributes().getBloodType(),
          Predicates.equalTo(bloodType));
    }

    public Predicate<UnosExchangeUnit> patientRaceIs(Race race) {
      return fromPatientAttribute(unosNodeAttributeSet
          .getUnosPatientInputAttributes().getRace(), Predicates.equalTo(race));
    }

    /**
     * Predicate will return true iff the patients age is strictly less than
     * maxAge
     */
    public Predicate<UnosExchangeUnit> patientIsPediatric(int maxAge) {
      return fromPatientAttribute(unosNodeAttributeSet
          .getUnosPatientInputAttributes().getAgeYears(),
          Range.lessThan(maxAge));
    }

  }

  public static class InputPredicates<V, E, T extends Comparable<T>, D> {
    private NodeInputAttributeSet<V, E, T> attributeSet;

    public InputPredicates(NodeInputAttributeSet<V, E, T> attributeSet) {
      this.attributeSet = attributeSet;
    }

    public NodeInputAttributeSet<V, E, T> getNodeAttributeSet() {
      return this.attributeSet;
    }

    // DON'T USE THIS! You can have a "unit type" of PAIRED but for the purposes
    // of the simulation, be
    // an altruistic donor, if you are a bridge donor with one end of the bridge
    // before the simulation
    // starts and the other after.
    /*
     * public Predicate<ExchangeUnit> unitTypeIs(ExchangeUnitType
     * exchangeUnitType){ return
     * Predicates.compose(Predicates.equalTo(exchangeUnitType),
     * attributeSet.getExchangeInputAttributes().getExchangeUnitType()); }
     */

    public Predicate<V> arrivedAfterSimulationStart() {
      final T simulationStart = attributeSet.getInputs().getStartTime()
          .getValue();
      return new Predicate<V>() {

        @Override
        public boolean apply(V arg0) {
          return simulationStart.compareTo(attributeSet
              .getNodeInputAttributes().getTimeNodeArrived().apply(arg0)
              .getValue()) < 0;
        }
      };
    }

    public Predicate<V> effectiveNodeTypeIs(EffectiveNodeType effectiveNodeType) {
      return Predicates.compose(Predicates.equalTo(effectiveNodeType),
          attributeSet.getNodeInputAttributes().getEffectiveNodeType());
    }

    public Predicate<V> postPreferencePairMatchPowerIn(Range<Double> range) {
      return Predicates.compose(range, attributeSet.getNodeInputAttributes()
          .getPairMatchPowerPostPreferences());
    }

    public Predicate<V> postPreferenceDonorPowerIn(Range<Double> range) {
      return Predicates.compose(range, attributeSet.getNodeInputAttributes()
          .getDonorPowerPostPreferences());
    }

    public Predicate<V> postPreferenceReceiverPowerIn(Range<Double> range) {
      return Predicates.compose(range, attributeSet.getNodeInputAttributes()
          .getReceiverPowerPostPreferences());
    }
  }

  public static class OutputPredicates<V, E, T extends Comparable<T>, D> {
    private NodeOutputAttributeSet<V, E, T, D> attributeSet;

    public OutputPredicates(NodeOutputAttributeSet<V, E, T, D> attributeSet) {
      this.attributeSet = attributeSet;
    }

    public Predicate<V> receiverIsMatched() {
      return Predicates.compose(identity, attributeSet
          .getNodeOutputAttributes().getReceiverWasMatched());
    }

    public Predicate<V> donorIsMatched() {
      return Predicates.compose(identity, attributeSet
          .getNodeOutputAttributes().getDonorWasMatched());
    }
  }

  public static class UnosOutputPredicates extends
      OutputPredicates<UnosExchangeUnit, UnosDonorEdge, Double, Double> {

    private UnosOutputAttributeSet attributeSet;

    public UnosOutputPredicates(UnosOutputAttributeSet attributeSet) {
      super(attributeSet);
      this.attributeSet = attributeSet;
    }

    public Predicate<UnosExchangeUnit> matchDonorIsSameCenter() {
      return new Predicate<UnosExchangeUnit>() {
        public boolean apply(UnosExchangeUnit unit) {
          if (!attributeSet.getNodeOutputAttributes().getReceiverWasMatched()
              .apply(unit)) {
            return false;
          }
          UnosExchangeUnit donorUnit = attributeSet.getNodeOutputAttributes()
              .getDonatingNode().apply(unit);
          if (donorUnit.getPatient() != null) {
            return unit.getPatient().getCenter() == donorUnit.getPatient()
                .getCenter();
          } else {
            return unit.getPatient().getCenter() == donorUnit.getDonors()
                .get(0).getCenter();
          }
        }
      };
    }

  }

  /*
   * public static class ExchangeUnitOutputPredicates extends
   * OutputPredicates<ExchangeUnit,DonorEdge,ReadableInstant,Period>{
   * 
   * private ExchangeUnitInputPredicates inputPredicates;
   * 
   * public
   * ExchangeUnitOutputPredicates(NodeOutputAttributeSet<ExchangeUnit,DonorEdge
   * ,ReadableInstant,Period> attributeSet) { super(attributeSet);
   * this.inputPredicates = } }
   */

  private static Predicate<Boolean> identity = Predicates.equalTo(Boolean.TRUE);

}
