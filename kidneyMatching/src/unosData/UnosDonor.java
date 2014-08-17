package unosData;

import org.joda.time.DateTime;

import data.BloodType;
import data.Race;
import data.TissueType;

public class UnosDonor {

  private long id;
  private DateTime arrivalDate;
  private int ageYears;
  private BloodType bloodType;
  private TissueType tissueType;

  private Race race;
  private int center;

  public Race getRace() {
    return race;
  }

  public int getCenter() {
    return center;
  }

  public long getId() {
    return id;
  }

  public DateTime getArrivalDate() {
    return arrivalDate;
  }

  public int getAgeYears() {
    return ageYears;
  }

  public BloodType getBloodType() {
    return bloodType;
  }

  public TissueType getTissueType() {
    return tissueType;
  }

  public UnosDonor(long id, DateTime arrivalDate, int ageYears,
      BloodType bloodType, TissueType tissueType, Race race, int center) {
    super();
    this.id = id;
    this.arrivalDate = arrivalDate;
    this.ageYears = ageYears;
    this.bloodType = bloodType;
    this.tissueType = tissueType;
    this.race = race;
    this.center = center;
  }

  @Override
  public String toString() {
    return "d" + id;
  }

}
