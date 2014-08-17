package main;

import java.io.File;

import kepLib.KepInstance;
import kepLib.KepParseData;
import kepLib.KepTextReaderWriter;
import unosData.UnosData;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;

import com.google.common.base.Function;
import com.google.common.base.Functions;

import exchangeGraph.minWaitingTime.MinWaitingTimeProblemData;

public class ConvertUnosData {

  public static void main(String[] args) {
    UnosData data = UnosData.readUnosData("data" + File.separator
        + "unosDataItaiCorrected.csv", false, true);
    KepInstance<UnosExchangeUnit, UnosDonorEdge> instance = new KepInstance<UnosExchangeUnit, UnosDonorEdge>(
        data.exportKepProblemData(), Functions.constant(1), Integer.MAX_VALUE,
        3, 0);
    Function<UnosExchangeUnit, String> nodeNames = KepParseData
        .anonymousNodeNames(instance);
    Function<UnosDonorEdge, String> edgeNames = KepParseData
        .anonymousEdgeNames(instance);
    KepTextReaderWriter.INSTANCE.write(instance, nodeNames, edgeNames,
        "kepLibInstances" + File.separator + "big" + File.separator
            + "unos.csv");
    MinWaitingTimeProblemData<UnosExchangeUnit> minWaitingTimeProblemData = data
        .exportMinWaitingTimeProblemData();
    KepTextReaderWriter.INSTANCE.writeNodeArrivalTimes("kepLibInstances"
        + File.separator + "big" + File.separator + "unosArrivalTimes.csv",
        minWaitingTimeProblemData, nodeNames);

  }

}
