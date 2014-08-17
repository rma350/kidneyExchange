package unosData;

import java.util.EnumSet;

import data.MedicalMatch;
import data.MedicalMatch.Incompatability;

public class UnosMedicalMatch {

  private UnosMedicalMatch() {
  }

  public static UnosMedicalMatch INSTANCE = new UnosMedicalMatch();

  public EnumSet<Incompatability> match(UnosDonor donor, UnosPatient patient) {
    EnumSet<Incompatability> ans = EnumSet.noneOf(Incompatability.class);
    if (!MedicalMatch.bloodTypeCompatible(donor.getBloodType(),
        patient.getBloodType())) {
      ans.add(Incompatability.BLOOD_TYPE);
    }

    if (donor.getAgeYears() > patient.getMaxDonorAgeYears()) {
      ans.add(Incompatability.AGE_OLD);
    }
    if (!patient.getTissueTypeSensitivity().isCompatible(donor.getTissueType())) {
      ans.add(Incompatability.TISSUE_TYPE);
    }
    return ans;
  }

}
