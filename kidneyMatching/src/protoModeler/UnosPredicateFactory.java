package protoModeler;

import kepModeler.ModelerInputs;
import protoModeler.UnosPredicateBuilder.UnosHistoricData;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;

public class UnosPredicateFactory implements
    PredicateFactory<UnosExchangeUnit, UnosDonorEdge> {

  private UnosHistoricData unosHistoricData;

  public UnosPredicateFactory(UnosHistoricData unosHistoricData) {
    this.unosHistoricData = unosHistoricData;
  }

  @Override
  public PredicateBuilder<UnosExchangeUnit, UnosDonorEdge> createPredicateBuilder(
      ModelerInputs<UnosExchangeUnit, UnosDonorEdge> modelerInputs) {
    return new UnosPredicateBuilder(modelerInputs, unosHistoricData);
  }
}
