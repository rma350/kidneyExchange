package inputOutput.node.unosExchangeUnit;

import inputOutput.core.Attribute;

import java.util.EnumMap;

import protoModeler.UnosPredicateBuilder.UnosHistoricData;
import unosData.UnosDonor;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import data.BloodType;
import data.Genotype;
import data.HlaType;
import data.Race;

public class UnosDonorInputAttributes {

  private final UnosHistoricData unosHistoricalData;

  private final Attribute<UnosDonor, BloodType> bloodType;
  private final Attribute<UnosDonor, Race> race;
  private final Attribute<UnosDonor, Integer> ageYears;
  public final Attribute<UnosDonor, Integer> centerId;

  private final Attribute<UnosDonor, Double> derivedPra;
  private final Attribute<UnosDonor, Double> historicalDonorPower;
  private final ImmutableMap<HlaType, Attribute<UnosDonor, Genotype>> hlaType;

  public UnosDonorInputAttributes(final UnosHistoricData unosHistoricalData) {
    this.unosHistoricalData = unosHistoricalData;
    bloodType = new Attribute<UnosDonor, BloodType>() {
      @Override
      public BloodType apply(UnosDonor donor) {
        return donor.getBloodType();
      }
    };
    race = new Attribute<UnosDonor, Race>() {
      @Override
      public Race apply(UnosDonor donor) {
        return donor.getRace();
      }
    };
    ageYears = new Attribute<UnosDonor, Integer>() {
      @Override
      public Integer apply(UnosDonor donor) {
        return donor.getAgeYears();
      }
    };
    derivedPra = new Attribute<UnosDonor, Double>() {
      @Override
      public Double apply(UnosDonor donor) {
        return unosHistoricalData.historicDonorPra(donor);
      }
    };
    historicalDonorPower = new Attribute<UnosDonor, Double>() {
      @Override
      public Double apply(UnosDonor donor) {
        return unosHistoricalData.historicDonorPower(donor);
      }
    };
    centerId = new Attribute<UnosDonor, Integer>() {
      @Override
      public Integer apply(UnosDonor donor) {
        return donor.getCenter();
      }
    };
    EnumMap<HlaType, Attribute<UnosDonor, Genotype>> hlaBuilder = Maps
        .newEnumMap(HlaType.class);
    for (final HlaType hlaType : HlaType.values()) {
      hlaBuilder.put(hlaType, new Attribute<UnosDonor, Genotype>() {

        @Override
        public Genotype apply(UnosDonor input) {
          return input.getTissueType().getHlaTypes().get(hlaType);
        }
      });
    }
    hlaType = Maps.immutableEnumMap(hlaBuilder);
  }

  public UnosHistoricData getUnosHistoricalData() {
    return unosHistoricalData;
  }

  public Attribute<UnosDonor, BloodType> getBloodType() {
    return bloodType;
  }

  public Attribute<UnosDonor, Race> getRace() {
    return race;
  }

  public Attribute<UnosDonor, Integer> getAgeYears() {
    return ageYears;
  }

  public Attribute<UnosDonor, Integer> getCenterId() {
    return centerId;
  }

  public Attribute<UnosDonor, Double> getDerivedPra() {
    return derivedPra;
  }

  public Attribute<UnosDonor, Double> getHistoricalDonorPower() {
    return historicalDonorPower;
  }

  public ImmutableMap<HlaType, Attribute<UnosDonor, Genotype>> getHlaType() {
    return hlaType;
  }

}
