package unosData;

import java.util.List;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;

import data.ExchangeUnit.ExchangeUnitType;

public class UnosExchangeUnit {

  private ExchangeUnitType exchangeUnitType;
  private List<UnosDonor> donors;
  private UnosPatient patient;

  public ExchangeUnitType getExchangeUnitType() {
    return exchangeUnitType;
  }

  public static UnosExchangeUnit makeAltruistic(UnosDonor donor) {
    UnosExchangeUnit ans = new UnosExchangeUnit(ExchangeUnitType.altruistic,
        null);
    ans.getDonors().add(donor);
    return ans;
  }

  public static UnosExchangeUnit makePaired(UnosPatient patient,
      Iterable<UnosDonor> donors) {
    UnosExchangeUnit ans = new UnosExchangeUnit(ExchangeUnitType.paired,
        patient);
    for (UnosDonor donor : donors) {
      ans.getDonors().add(donor);
    }
    return ans;
  }

  private UnosExchangeUnit(ExchangeUnitType exchangeUnitType,
      UnosPatient patient) {
    super();
    this.exchangeUnitType = exchangeUnitType;
    this.donors = Lists.newArrayList();
    this.patient = patient;
  }

  public List<UnosDonor> getDonors() {
    return donors;
  }

  public UnosPatient getPatient() {
    return patient;
  }

  public String toString() {
    String ans;
    if (this.exchangeUnitType == ExchangeUnitType.altruistic) {
      ans = "{" + donors.toString() + "}";
    } else {
      ans = "{" + patient.toString() + ", " + donors.toString() + "}";
    }
    return CharMatcher.is(',').replaceFrom(ans, '-');

  }

}
