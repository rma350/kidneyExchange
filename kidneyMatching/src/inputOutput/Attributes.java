package inputOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Attributes {

  public static interface AttributeName {
    public String getDefaultName();
  }

  public static enum ExchangeIn implements ExchangeUnitInputAttributeName {
    exchangeUnitType("Exchange Unit Type"), numDonors("Number Donors"), isChip(
        "Is Chip"), donors("Donors");

    private String defaultName;

    private ExchangeIn(String defaultName) {
      this.defaultName = defaultName;
    }

    public String getDefaultName() {
      return defaultName;
    }
  }

  public static enum ExchangeOut implements ExchangeUnitOutputAttributeName {
    matchedDonor("Matched Donor"), matchedOrFirstDonor("Matched/First Donor");

    private String defaultName;

    private ExchangeOut(String defaultName) {
      this.defaultName = defaultName;
    }

    public String getDefaultName() {
      return defaultName;
    }
  }

  public static enum NodeIn implements NodeInputAttributeName {
    timeNodeArrived("Time Node Arrived"), donorPowerPostPreferences(
        "Node Donor Power Post Preferences"), receiverPowerPostPreferences(
        "Node Receiver Power Post Preferences"), pairMatchPowerPostPreferences(
        "Node Pair Match Power Post Preferences"), effectiveNodeType(
        "Effective Node Type");

    private String defaultName;

    private NodeIn(String defaultName) {
      this.defaultName = defaultName;

    }

    public String getDefaultName() {
      return this.defaultName;
    }
  }

  public static enum NodeOut implements NodeOutputAttributeName {
    donorWasMatched("Donor Matched"), timeDonorMatched("Time Donor Matched"),

    donorWaitingTime("Donor Waiting Time (Matched)"), totalDonorWaitingTime(
        "Donor Waiting Time (All)"), receiverWasMatched("Receiver Matched"), timeReceiverMatched(
        "Time Receiver Matched"),

    receiverWaitingTime("Receiver Waiting Time (Matched)"), totalReceiverWaitingTime(
        "Receiver Waiting Time (All)");

    private String defaultName;

    private NodeOut(String defaultName) {
      this.defaultName = defaultName;
    }

    public String getDefaultName() {
      return this.defaultName;
    }

  }

  public static enum PersonIn implements PersonInputAttributeName {
    yearBorn("Year born"), id("Id"), registered("Date Registered"), gender(
        "Gender"), race("Race"), bloodType("Blood Type"), hlaA1("HLA A1"), hlaA2(
        "HLA A2"), hlaB1("HLA B1"), hlaB2("HLA B2"), hlaDR1("HLA DR1"), hlaDR2(
        "HLA DR2"), hlaBw4("HLA Bw4"), hlaBw6("HLA Bw6"), hlaCw1("HLA Cw1"), hlaCw2(
        "HLA Cw2"), hlaDQ1("HLA DQ1"), hlaDQ2("HLA DQ2"), hlaDP1("HLA DP1"), hlaDP2(
        "HLA DP2"), hlaDR51("HLA DR 51"), hlaDR52("HLA DR 52"), hlaDR53(
        "HLA DR 53"), heightCm("Height (cm)"), weightKg("Weight (kg)"), hospital(
        "Hospital");

    private String defaultName;

    private PersonIn(String defaultName) {
      this.defaultName = defaultName;

    }

    public String getDefaultName() {
      return this.defaultName;
    }
  }

  public static enum ReceiverIn implements ReceiverInputAttributeName {
    antibodiesA("A antibodies"), antibodiesB("B antibodies"), antibodiesDP(
        "DP antibodies"), antibodiesDQ("DQ antibodies"), antibodiesDR(
        "DR antibodies"), antibodiesCw("Cw antibodies"), antibodySpecialBw4(
        "Bw 4 antibody"), antibodySpecialBw6("Bw 6 antibody"), antibodySpecialDR51(
        "DR 51 antibody"), antibodySpecialDR52("DR 52 antibody"), antibodySpecialDR53(
        "DR 53 antibody"), minDonorAge("Min Donor Age"), maxDonorAge(
        "Max Donor Age"), minHlaMatch("Min Hla Match"), minDonorWeight(
        "Min Donor Weight"), acceptShippedKidney("Accept shipped kidney"), willingToTravel(
        "Willing to travel"), vpra("Virtual PRA");

    @Override
    public String getDefaultName() {
      return this.defaultName;
    }

    private String defaultName;

    private ReceiverIn(String defaultName) {
      this.defaultName = defaultName;
    }

  }

  public static Map<AttributeName, String> getAttributesToDefaultName(
      Set<AttributeName> attributes) {
    Map<AttributeName, String> ans = new HashMap<AttributeName, String>();
    for (AttributeName name : attributes) {
      ans.put(name, name.getDefaultName());
    }
    return ans;
  }

  public static interface InputAttributeName extends AttributeName {
  }

  public static interface OutputAttributeName extends AttributeName {
  }

  public static interface NodeAttributeName extends AttributeName {
  }

  public static interface ExchangeUnitAttributeName extends NodeAttributeName {
  }

  public static interface NodeInputAttributeName extends NodeAttributeName,
      InputAttributeName {
  }

  public static interface NodeOutputAttributeName extends NodeAttributeName,
      OutputAttributeName {
  }

  public static interface ExchangeUnitInputAttributeName extends
      ExchangeUnitAttributeName, NodeInputAttributeName {
  }

  public static interface ExchangeUnitOutputAttributeName extends
      ExchangeUnitAttributeName, NodeOutputAttributeName {
  }

  public static interface PersonAttributeName extends ExchangeUnitAttributeName {
  }

  public static interface PersonInputAttributeName extends PersonAttributeName,
      ExchangeUnitInputAttributeName {
  }

  public static interface PersonOutputAttributeName extends
      PersonAttributeName, ExchangeUnitOutputAttributeName {
  }

  public static interface DonorAttributeName extends PersonAttributeName {
  }

  public static interface DonorInputAttributeName extends DonorAttributeName,
      PersonInputAttributeName {
  }

  public static interface DonorOutputAttributeName extends DonorAttributeName,
      PersonOutputAttributeName {
  }

  public static interface ReceiverAttributeName extends PersonAttributeName {
  }

  public static interface ReceiverInputAttributeName extends
      ReceiverAttributeName, PersonInputAttributeName {
  }

  public static interface ReceiverOutputAttributeName extends
      ReceiverAttributeName, PersonOutputAttributeName {
  }

}
