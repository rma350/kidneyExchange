package protoModeler;

import java.util.Map;

import kepLib.KepProblemData;
import kepModeler.AuxiliaryInputStatistics;
import protoModeler.UnosPredicateBuilder.UnosHistoricData;
import unosData.UnosDonor;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;
import unosData.UnosPatient;

import com.google.common.collect.Maps;

public class UnosHistoricDataImpl extends
    BaseHistoricDataImpl<UnosExchangeUnit, UnosDonorEdge> implements
    UnosHistoricData {

  private Map<UnosDonor, Double> historicDonorPraValues;
  private Map<UnosPatient, Double> historicPatientPraValues;
  private Map<UnosDonor, Double> historicDonorPairMatchPowerValues;

  public UnosHistoricDataImpl(
      KepProblemData<UnosExchangeUnit, UnosDonorEdge> baseProblemData,
      AuxiliaryInputStatistics<UnosExchangeUnit, UnosDonorEdge> auxiliaryInputStatistics) {
    super(baseProblemData, auxiliaryInputStatistics);
    historicDonorPraValues = Maps.newHashMap();
    historicDonorPairMatchPowerValues = Maps.newHashMap();

    // System.out.println("donor pras:");
    for (UnosExchangeUnit unit : baseProblemData.getGraph().getVertices()) {
      for (UnosDonor donor : unit.getDonors()) {
        historicDonorPairMatchPowerValues.put(donor,
            ProtoUtil.computeDonorPower(donor, unit, baseProblemData));
        double donorPra = ProtoUtil.computeDonorPra(donor, baseProblemData);
        // System.out.println(donorPra);
        historicDonorPraValues.put(donor, donorPra);
      }
    }
    historicPatientPraValues = Maps.newHashMap();
    // System.out.println("patient pras: ");
    // System.out.println("whole pool, spreadsheet");
    for (UnosExchangeUnit unit : baseProblemData.getGraph().getVertices()) {
      if (unit.getPatient() != null) {
        UnosPatient patient = unit.getPatient();
        double patientPra = ProtoUtil.computePatientPra(patient,
            baseProblemData);
        // System.out.println(patientPra + ", " + patient.getcPra());
        historicPatientPraValues.put(patient, patientPra);
      }
    }

  }

  @Override
  public double historicDonorPower(UnosDonor donor) {
    return historicDonorPairMatchPowerValues.get(donor);
  }

  @Override
  public double historicDonorPra(UnosDonor donor) {
    return this.historicDonorPraValues.get(donor);
  }

  @Override
  public double historicPatientPra(UnosPatient patient) {
    return this.historicPatientPraValues.get(patient);
  }

}
