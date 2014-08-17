package inputOutput.node.unosExchangeUnit;

import inputOutput.core.Attribute;
import protoModeler.UnosPredicateBuilder.UnosHistoricData;
import unosData.UnosPatient;
import data.BloodType;
import data.Race;

public class UnosPatientInputAttributes {

  private final UnosHistoricData unosHistoricalData;

  private final Attribute<UnosPatient, Long> id;
  private final Attribute<UnosPatient, BloodType> bloodType;
  private final Attribute<UnosPatient, Race> race;
  private final Attribute<UnosPatient, Integer> ageYears;
  private final Attribute<UnosPatient, Integer> maxDonorAgeYr;
  public final Attribute<UnosPatient, Integer> centerId;

  private final Attribute<UnosPatient, Double> pra;
  private final Attribute<UnosPatient, Double> derivedPra;

  public UnosPatientInputAttributes(final UnosHistoricData unosHistoricalData) {
    this.unosHistoricalData = unosHistoricalData;
    id = new Attribute<UnosPatient, Long>() {
      @Override
      public Long apply(UnosPatient patient) {
        return patient.getId();
      }
    };
    bloodType = new Attribute<UnosPatient, BloodType>() {
      @Override
      public BloodType apply(UnosPatient patient) {
        return patient.getBloodType();
      }
    };
    race = new Attribute<UnosPatient, Race>() {
      @Override
      public Race apply(UnosPatient patient) {
        return patient.getRace();
      }
    };
    ageYears = new Attribute<UnosPatient, Integer>() {
      @Override
      public Integer apply(UnosPatient patient) {
        return patient.getAgeYears();
      }
    };
    maxDonorAgeYr = new Attribute<UnosPatient, Integer>() {
      @Override
      public Integer apply(UnosPatient patient) {
        return patient.getMaxDonorAgeYears();
      }
    };
    pra = new Attribute<UnosPatient, Double>() {
      @Override
      public Double apply(UnosPatient patient) {
        return patient.getcPra();
      }
    };
    derivedPra = new Attribute<UnosPatient, Double>() {
      @Override
      public Double apply(UnosPatient patient) {
        return unosHistoricalData.historicPatientPra(patient);
      }
    };
    centerId = new Attribute<UnosPatient, Integer>() {
      @Override
      public Integer apply(UnosPatient patient) {
        return patient.getCenter();
      }
    };
  }

  public UnosHistoricData getUnosHistoricalData() {
    return unosHistoricalData;
  }

  public Attribute<UnosPatient, Long> getId() {
    return id;
  }

  public Attribute<UnosPatient, BloodType> getBloodType() {
    return bloodType;
  }

  public Attribute<UnosPatient, Race> getRace() {
    return race;
  }

  public Attribute<UnosPatient, Integer> getAgeYears() {
    return ageYears;
  }

  public Attribute<UnosPatient, Integer> getMaxDonorAgeYr() {
    return maxDonorAgeYr;
  }

  public Attribute<UnosPatient, Double> getPra() {
    return pra;
  }

  public Attribute<UnosPatient, Double> getDerivedPra() {
    return derivedPra;
  }

  public Attribute<UnosPatient, Integer> getCenterId() {
    return centerId;
  }

}
