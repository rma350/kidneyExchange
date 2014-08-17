package inputOutput.nkrFormat;

import inputOutput.Attributes.ExchangeIn;
import inputOutput.Attributes.PersonIn;
import inputOutput.Attributes.ReceiverIn;

import java.util.EnumMap;

import data.ProblemData;
import data.ProblemData.ColumnName;

public class ColumnNameToAttribute {

  public static EnumMap<ColumnName, ExchangeIn> columnToNodeIn;
  static {
    columnToNodeIn = new EnumMap<ColumnName, ExchangeIn>(ColumnName.class);
    columnToNodeIn.put(ColumnName.RELATED_DONORS, ExchangeIn.donors);
    columnToNodeIn.put(ColumnName.CHIP, ExchangeIn.isChip);
    columnToNodeIn.put(ColumnName.TYPE, ExchangeIn.exchangeUnitType);

  }

  public static EnumMap<ColumnName, PersonIn> columnToAttributeDefault;
  static {
    columnToAttributeDefault = new EnumMap<ProblemData.ColumnName, PersonIn>(
        ProblemData.ColumnName.class);
    columnToAttributeDefault.put(ColumnName.BIRTH_YEAR, PersonIn.yearBorn);
    columnToAttributeDefault.put(ColumnName.PERSON_ID, PersonIn.id);
    columnToAttributeDefault.put(ColumnName.DATE_REGISTERED,
        PersonIn.registered);
    columnToAttributeDefault.put(ColumnName.GENDER, PersonIn.gender);
    columnToAttributeDefault.put(ColumnName.RACE, PersonIn.race);
    columnToAttributeDefault.put(ColumnName.BLOOD_TYPE, PersonIn.bloodType);
    columnToAttributeDefault.put(ColumnName.WEIGHT_KG, PersonIn.weightKg);
    columnToAttributeDefault.put(ColumnName.HEIGHT_CM, PersonIn.heightCm);
    columnToAttributeDefault.put(ColumnName.CENTER_ID, PersonIn.hospital);
    columnToAttributeDefault.put(ColumnName.HLA_A1, PersonIn.hlaA1);
    columnToAttributeDefault.put(ColumnName.HLA_A2, PersonIn.hlaA2);
    columnToAttributeDefault.put(ColumnName.HLA_B1, PersonIn.hlaB1);
    columnToAttributeDefault.put(ColumnName.HLA_B2, PersonIn.hlaB2);
    columnToAttributeDefault.put(ColumnName.HLA_DQ1, PersonIn.hlaDQ1);
    columnToAttributeDefault.put(ColumnName.HLA_DQ2, PersonIn.hlaDQ2);
    columnToAttributeDefault.put(ColumnName.HLA_DR1, PersonIn.hlaDR1);
    columnToAttributeDefault.put(ColumnName.HLA_DR2, PersonIn.hlaDR2);
    columnToAttributeDefault.put(ColumnName.HLA_Bw1, PersonIn.hlaBw4);
    columnToAttributeDefault.put(ColumnName.HLA_Bw2, PersonIn.hlaBw6);
    columnToAttributeDefault.put(ColumnName.HLA_Cw1, PersonIn.hlaCw1);
    columnToAttributeDefault.put(ColumnName.HLA_Cw2, PersonIn.hlaCw2);
    columnToAttributeDefault.put(ColumnName.HLA_DP1, PersonIn.hlaDP1);
    columnToAttributeDefault.put(ColumnName.HLA_DP2, PersonIn.hlaDP2);
    columnToAttributeDefault.put(ColumnName.HLA_DR_51, PersonIn.hlaDR51);
    columnToAttributeDefault.put(ColumnName.HLA_DR_52, PersonIn.hlaDR52);
    columnToAttributeDefault.put(ColumnName.HLA_DR_53, PersonIn.hlaDR53);
  }

  static {
    EnumMap<ColumnName, ReceiverIn> columnToReceiverIn = new EnumMap<ColumnName, ReceiverIn>(
        ColumnName.class);
    columnToReceiverIn.put(ColumnName.AVOIDS_A, ReceiverIn.antibodiesA);
    columnToReceiverIn.put(ColumnName.AVOIDS_B, ReceiverIn.antibodiesB);
    columnToReceiverIn.put(ColumnName.AVOIDS_DP, ReceiverIn.antibodiesDP);
    columnToReceiverIn.put(ColumnName.AVOIDS_DQ, ReceiverIn.antibodiesDQ);
    columnToReceiverIn.put(ColumnName.AVOIDS_DR, ReceiverIn.antibodiesDR);
    // TODO this is broken, bw6 is not covered.
    columnToReceiverIn.put(ColumnName.AVOIDS_BW, ReceiverIn.antibodySpecialBw4);
    columnToReceiverIn.put(ColumnName.AVOIDS_CW, ReceiverIn.antibodiesCw);
    columnToReceiverIn.put(ColumnName.AVOIDS_DR_51,
        ReceiverIn.antibodySpecialDR51);
    columnToReceiverIn.put(ColumnName.AVOIDS_DR_52,
        ReceiverIn.antibodySpecialDR52);
    columnToReceiverIn.put(ColumnName.AVOIDS_DR_53,
        ReceiverIn.antibodySpecialDR53);
    columnToReceiverIn.put(ColumnName.MIN_DONOR_AGE, ReceiverIn.minDonorAge);
    columnToReceiverIn.put(ColumnName.MAX_DONOR_AGE, ReceiverIn.maxDonorAge);
    columnToReceiverIn.put(ColumnName.MIN_HLA_MATCH, ReceiverIn.minHlaMatch);
    columnToReceiverIn.put(ColumnName.MIN_DONOR_WEIGHT,
        ReceiverIn.minDonorWeight);
    columnToReceiverIn.put(ColumnName.ACCEPT_SHIPPED_KIDNEY,
        ReceiverIn.acceptShippedKidney);
    columnToReceiverIn.put(ColumnName.WILLING_TO_TRAVEL,
        ReceiverIn.willingToTravel);
  }

}
