package unosData;

import org.joda.time.DateTime;

import data.BloodType;
import data.Race;
import data.TissueTypeSensitivity;

public class UnosPatient {

  private long id;
  private DateTime arrivalDate;
  private BloodType bloodType;
  private int maxDonorAgeYears;
  private TissueTypeSensitivity tissueTypeSensitivity;
  private Race race;
  private int center;
  private double cPra;
  private int ageYears;

  public long getId() {
    return id;
  }

  public DateTime getArrivalDate() {
    return arrivalDate;
  }

  public BloodType getBloodType() {
    return bloodType;
  }

  public int getMaxDonorAgeYears() {
    return maxDonorAgeYears;
  }

  public TissueTypeSensitivity getTissueTypeSensitivity() {
    return tissueTypeSensitivity;
  }

  public double getcPra() {
    return cPra;
  }

  public Race getRace() {
    return race;
  }

  public int getCenter() {
    return center;
  }

  public UnosPatient(long id, DateTime arrivalDate, BloodType bloodType,
      int maxDonorAgeYears, TissueTypeSensitivity tissueTypeSensitivity,
      Race race, int center, double cPra, int ageYears) {
    super();
    this.id = id;
    this.arrivalDate = arrivalDate;
    this.bloodType = bloodType;
    this.maxDonorAgeYears = maxDonorAgeYears;
    this.tissueTypeSensitivity = tissueTypeSensitivity;
    this.race = race;
    this.center = center;
    this.cPra = cPra;
    this.ageYears = ageYears;
  }

  public int getAgeYears() {
    return this.ageYears;
  }

  @Override
  public String toString() {
    return "p" + id;
  }

  public String longString() {
    return "id: " + this.id + ", arrivalDate: " + arrivalDate + ", bloodType: "
        + bloodType + ", maxDonorAgeYears: " + maxDonorAgeYears
        + ", tissueTypeSensitivity: " + tissueTypeSensitivity + ", race: "
        + race + ", center: " + center + ", cPra: " + cPra + ", ageYears: "
        + ageYears;
  }

}
