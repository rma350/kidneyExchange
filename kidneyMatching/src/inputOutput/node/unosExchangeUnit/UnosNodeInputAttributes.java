package inputOutput.node.unosExchangeUnit;

import inputOutput.core.Attribute;

import java.util.List;

import unosData.UnosDonor;
import unosData.UnosExchangeUnit;
import unosData.UnosPatient;
import data.ExchangeUnit.ExchangeUnitType;

public class UnosNodeInputAttributes {

  private final Attribute<UnosExchangeUnit, UnosPatient> patient;
  private final Attribute<UnosExchangeUnit, List<UnosDonor>> donors;
  private final Attribute<UnosExchangeUnit, Integer> numDonors;
  private final Attribute<UnosExchangeUnit, Boolean> isChip;
  private final Attribute<UnosExchangeUnit, ExchangeUnitType> exchangeUnitType;

  public UnosNodeInputAttributes() {

    this.patient = new Attribute<UnosExchangeUnit, UnosPatient>() {
      @Override
      public UnosPatient apply(UnosExchangeUnit unit) {
        return unit.getPatient();
      }
    };

    donors = new Attribute<UnosExchangeUnit, List<UnosDonor>>() {
      @Override
      public List<UnosDonor> apply(UnosExchangeUnit node) {
        return node.getDonors();
      }
    };
    exchangeUnitType = new Attribute<UnosExchangeUnit, ExchangeUnitType>() {

      @Override
      public ExchangeUnitType apply(UnosExchangeUnit exchangeUnit) {
        return exchangeUnit.getExchangeUnitType();
      }

    };
    isChip = new Attribute<UnosExchangeUnit, Boolean>() {

      @Override
      public Boolean apply(UnosExchangeUnit exchangeUnit) {
        return exchangeUnit.getExchangeUnitType() == ExchangeUnitType.chip;
      }

    };
    numDonors = new Attribute<UnosExchangeUnit, Integer>() {
      @Override
      public Integer apply(UnosExchangeUnit unit) {
        return unit.getDonors().size();
      }
    };
  }

  public Attribute<UnosExchangeUnit, UnosDonor> getIthDonor(final int i) {
    if (i < 0) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return new Attribute<UnosExchangeUnit, UnosDonor>() {

      @Override
      public UnosDonor apply(UnosExchangeUnit input) {

        return input.getDonors().size() >= i ? null : input.getDonors().get(i);
      }

    };
  }

  public Attribute<UnosExchangeUnit, UnosPatient> getPatient() {
    return patient;
  }

  public Attribute<UnosExchangeUnit, List<UnosDonor>> getDonors() {
    return donors;
  }

  public Attribute<UnosExchangeUnit, Integer> getNumDonors() {
    return numDonors;
  }

  public Attribute<UnosExchangeUnit, Boolean> getIsChip() {
    return isChip;
  }

  public Attribute<UnosExchangeUnit, ExchangeUnitType> getExchangeUnitType() {
    return exchangeUnitType;
  }

  public <T> Attribute<UnosExchangeUnit, T> makePatientNodeAttribute(
      final Attribute<? super UnosPatient, T> patientAttribute) {
    return new Attribute<UnosExchangeUnit, T>() {
      @Override
      public T apply(UnosExchangeUnit unit) {
        return patientAttribute.apply(unit.getPatient());
      }
    };
  }

  public <T> Attribute<UnosExchangeUnit, T> makeDonorNodeAttribute(
      final Attribute<? super UnosDonor, T> donorAttribute, final int donorIndex) {
    return new Attribute<UnosExchangeUnit, T>() {
      @Override
      public T apply(UnosExchangeUnit node) {
        return donorAttribute.apply(node.getDonors().get(donorIndex));
      }
    };
  }

}
