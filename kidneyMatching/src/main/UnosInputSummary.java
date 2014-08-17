package main;

import inputOutput.ExchangePredicates.UnosNodeInputPredicates;
import inputOutput.Reports.UnosNodeAttributeSet;
import inputOutput.core.Attribute;
import inputOutput.core.AttributeConverter;
import inputOutput.core.CompositeAttributeConverter;
import inputOutput.core.CsvFormatUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import kepLib.KepInstance;
import kepLib.KepParseData;
import kepLib.KepTextReaderWriter;
import multiPeriod.MultiPeriodCyclePacking.EffectiveNodeType;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;
import multiPeriod.TimeInstant;
import multiPeriodAnalysis.Environment;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import protoModeler.UnosPredicateBuilder.UnosHistoricData;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import data.BloodType;
import exchangeGraph.minWaitingTime.MinWaitingTimeProblemData;

public class UnosInputSummary {

  /**
   * @param args
   */
  public static void main(String[] args) {
    new File(outDir).mkdirs();
    Environment<UnosExchangeUnit, UnosDonorEdge, Double> env = Environment
        .unosEnvironment(1, 0);
    MultiPeriodCyclePackingInputs<UnosExchangeUnit, UnosDonorEdge, Double> multiPeriodInputs = env
        .getMultiPeriodInputs();
    UnosNodeAttributeSet attributeSet = new UnosNodeAttributeSet(
        multiPeriodInputs, (UnosHistoricData) env.getHistoricData());
    UnosNodeInputPredicates inputPredicates = new UnosNodeInputPredicates(
        attributeSet);
    ExperimentInputs experimentInputs = new ExperimentInputs(attributeSet,
        inputPredicates, ImmutableList.copyOf(multiPeriodInputs.getGraph()
            .getVertices()));
    experimentPairMatchPowerDistribution(experimentInputs);
    experimentDonorPowerByBloodType(experimentInputs);
    experiementNodeBloodTypes(experimentInputs);
    experimentPatientPraByDonorBloodType(experimentInputs);
    experimentPatientPraVsDonorPower(experimentInputs);
    experimentPairSummaryInfo(experimentInputs);
    experimentMakeKEP(multiPeriodInputs);
    env.shutDown();
  }

  private static String outDir = "output" + File.separator + "unosSummary"
      + File.separator;

  private static void experiementNodeBloodTypes(ExperimentInputs in) {
    UnosInputSummary summary = new UnosInputSummary(in.getExchangeUnits(),
        Predicates.and(
            in.getInputPredicates().effectiveNodeTypeIs(
                EffectiveNodeType.paired), in.getInputPredicates()
                .exactlyOneDonor()));
    UnosNodeAttributeSet att = in.getAttributeSet();
    summary.addAttribute(
        string(att.getUnosNodeInputAttributes().makePatientNodeAttribute(
            att.getUnosPatientInputAttributes().getBloodType())),
        "patientBlood");
    summary
        .addAttribute(
            string(att.getUnosNodeInputAttributes().makeDonorNodeAttribute(
                att.getUnosDonorInputAttributes().getBloodType(), 0)),
            "donorBlood");
    summary.print(outDir + "patientDonorPairBlood.csv");
  }

  private static void experimentDonorPowerByBloodType(ExperimentInputs in) {
    for (BloodType bloodType : BloodType.values()) {
      UnosInputSummary summary = new UnosInputSummary(in.getExchangeUnits(),
          Predicates.and(
              in.getInputPredicates().effectiveNodeTypeIs(
                  EffectiveNodeType.paired), in.getInputPredicates()
                  .exactlyOneDonor(), in.getInputPredicates()
                  .firstDonorBloodTypeIs(bloodType)));
      summary.addAttribute(threeDecimals(in.getAttributeSet()
          .getNodeInputAttributes().getDonorPowerPostPreferences()),
          "donorPower");
      summary.print(outDir + "donorPowerBlood" + bloodType + ".csv");
    }
  }

  private static void experimentPatientPraByDonorBloodType(ExperimentInputs in) {
    for (BloodType bloodType : BloodType.values()) {
      UnosInputSummary summary = new UnosInputSummary(in.getExchangeUnits(),
          Predicates.and(
              in.getInputPredicates().effectiveNodeTypeIs(
                  EffectiveNodeType.paired), in.getInputPredicates()
                  .exactlyOneDonor(), in.getInputPredicates()
                  .firstDonorBloodTypeIs(bloodType)));
      summary
          .addAttribute(
              threeDecimals(in
                  .getAttributeSet()
                  .getUnosNodeInputAttributes()
                  .makePatientNodeAttribute(
                      in.getAttributeSet().getUnosPatientInputAttributes()
                          .getPra())), "patientPra");
      summary.print(outDir + "patientPraForDonorBlood" + bloodType + ".csv");
    }
  }

  private static void experimentPatientPraVsDonorPower(ExperimentInputs in) {
    UnosInputSummary summary = new UnosInputSummary(in.getExchangeUnits(), in
        .getInputPredicates().effectiveNodeTypeIs(EffectiveNodeType.paired));
    summary
        .addAttribute(threeDecimals(in.getAttributeSet()
            .getNodeInputAttributes().getDonorPowerPostPreferences()),
            "donorPower");
    summary
        .addAttribute(
            threeDecimals(in
                .getAttributeSet()
                .getUnosNodeInputAttributes()
                .makePatientNodeAttribute(
                    in.getAttributeSet().getUnosPatientInputAttributes()
                        .getPra())), "patientPra");
    summary.print(outDir + "patientPraVsDonorPower.csv");

  }

  private static void experimentPairMatchPowerDistribution(ExperimentInputs in) {
    UnosInputSummary summary = new UnosInputSummary(in.getExchangeUnits(), in
        .getInputPredicates().effectiveNodeTypeIs(EffectiveNodeType.paired));
    summary
        .addAttribute(threeDecimals(in.getAttributeSet()
            .getNodeInputAttributes().getDonorPowerPostPreferences()),
            "donorPower");
    summary.addAttribute(threeDecimals(in.getAttributeSet()
        .getNodeInputAttributes().getReceiverPowerPostPreferences()),
        "patientPower");
    summary.addAttribute(noDecimals(in.getAttributeSet()
        .getNodeInputAttributes().getPairMatchPowerPostPreferences()),
        "pairMatchPower");
    summary.print(outDir + "pairMatchPower.csv");
  }

  private static void experimentMakeKEP(
      MultiPeriodCyclePackingInputs<UnosExchangeUnit, UnosDonorEdge, Double> multiPeriod) {
    final KepInstance<UnosExchangeUnit, UnosDonorEdge> kepInstance = new KepInstance<UnosExchangeUnit, UnosDonorEdge>(
        multiPeriod.getProblemData(), Functions.constant(1.0),
        Integer.MAX_VALUE, 3, 0);
    Function<UnosExchangeUnit, String> nodeNames = new Function<UnosExchangeUnit, String>() {
      public String apply(UnosExchangeUnit unit) {
        return kepInstance.getRootNodes().contains(unit) ? "a"
            + unit.getDonors().get(0).getId() : "p" + unit.getPatient().getId();
      }
    };
    Function<UnosDonorEdge, String> edgeNames = KepParseData
        .anonymousEdgeNames(kepInstance);
    KepTextReaderWriter.INSTANCE.write(kepInstance, nodeNames, edgeNames,
        outDir + "allUnosKep.csv");
    ImmutableMap.Builder<UnosExchangeUnit, Double> nodeArrivalTimes = ImmutableMap
        .builder();
    for (Map.Entry<UnosExchangeUnit, TimeInstant<Double>> entry : multiPeriod
        .getNodeArrivalTimes().entrySet()) {
      nodeArrivalTimes.put(entry.getKey(), entry.getValue().getValue());
    }
    KepTextReaderWriter.INSTANCE.writeNodeArrivalTimes(
        outDir + "allUnosNodeArrivals.csv",
        new MinWaitingTimeProblemData<UnosExchangeUnit>(nodeArrivalTimes
            .build(), multiPeriod.getEndTime().getValue()), nodeNames);
  }

  private static void experimentPairSummaryInfo(ExperimentInputs in) {
    UnosInputSummary summary = new UnosInputSummary(in.getExchangeUnits(), in
        .getInputPredicates().effectiveNodeTypeIs(EffectiveNodeType.paired));
    summary.addAttribute(
        string(in
            .getAttributeSet()
            .getUnosNodeInputAttributes()
            .makePatientNodeAttribute(
                in.getAttributeSet().getUnosPatientInputAttributes().getId())),
        "patientId");
    summary
        .addAttribute(threeDecimals(in.getAttributeSet()
            .getNodeInputAttributes().getDonorPowerPostPreferences()),
            "donorPower");
    summary.addAttribute(threeDecimals(in.getAttributeSet()
        .getNodeInputAttributes().getReceiverPowerPostPreferences()),
        "patientPower");
    summary.addAttribute(noDecimals(in.getAttributeSet()
        .getNodeInputAttributes().getPairMatchPowerPostPreferences()),
        "pairMatchPower");
    UnosNodeAttributeSet att = in.getAttributeSet();
    summary.addAttribute(
        string(att.getUnosNodeInputAttributes().makePatientNodeAttribute(
            att.getUnosPatientInputAttributes().getBloodType())),
        "patientBlood");
    summary
        .addAttribute(
            string(att.getUnosNodeInputAttributes().makeDonorNodeAttribute(
                att.getUnosDonorInputAttributes().getBloodType(), 0)),
            "donorBlood");
    summary
        .addAttribute(
            threeDecimals(in
                .getAttributeSet()
                .getUnosNodeInputAttributes()
                .makePatientNodeAttribute(
                    in.getAttributeSet().getUnosPatientInputAttributes()
                        .getPra())), "patientPra");
    summary.print(outDir + "pairSummaryInfo.csv");
  }

  private Predicate<UnosExchangeUnit> nodeFilter;
  private List<String> attributeNames;
  private List<CompositeAttributeConverter<UnosExchangeUnit, ?>> attributeConverters;
  private ImmutableList<UnosExchangeUnit> exchangeUnits;

  public UnosInputSummary(ImmutableList<UnosExchangeUnit> exchangeUnits,
      Predicate<UnosExchangeUnit> nodeFilter) {

    this.nodeFilter = nodeFilter;
    this.attributeConverters = Lists.newArrayList();
    attributeNames = Lists.newArrayList();
    this.exchangeUnits = exchangeUnits;
  }

  public void addAttribute(
      CompositeAttributeConverter<UnosExchangeUnit, ?> converter, String name) {
    this.attributeConverters.add(converter);
    this.attributeNames.add(name);
  }

  public void print(String fileName) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL);
      // print headers
      for (int i = 0; i < attributeConverters.size(); i++) {
        printer.print(this.attributeNames.get(i));
      }
      printer.println();
      // print rows
      for (UnosExchangeUnit unit : this.exchangeUnits) {

        if (nodeFilter.apply(unit)) {

          for (AttributeConverter<UnosExchangeUnit> converter : this.attributeConverters) {
            printer.print(converter.apply(unit));
          }
          printer.println();
        }
      }
      printer.flush();
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static class ExperimentInputs {
    private UnosNodeAttributeSet attributeSet;
    private UnosNodeInputPredicates inputPredicates;
    private ImmutableList<UnosExchangeUnit> exchangeUnits;

    public UnosNodeAttributeSet getAttributeSet() {
      return attributeSet;
    }

    public UnosNodeInputPredicates getInputPredicates() {
      return inputPredicates;
    }

    public ImmutableList<UnosExchangeUnit> getExchangeUnits() {
      return exchangeUnits;
    }

    public ExperimentInputs(UnosNodeAttributeSet attributeSet,
        UnosNodeInputPredicates inputPredicates,
        ImmutableList<UnosExchangeUnit> exchangeUnits) {
      super();
      this.attributeSet = attributeSet;
      this.inputPredicates = inputPredicates;
      this.exchangeUnits = exchangeUnits;
    }
  }

  private static CompositeAttributeConverter<UnosExchangeUnit, Double> noDecimals(
      Attribute<UnosExchangeUnit, Double> attribute) {
    return new CompositeAttributeConverter<UnosExchangeUnit, Double>(attribute,
        CsvFormatUtil.noDecimals);
  }

  private static CompositeAttributeConverter<UnosExchangeUnit, Double> threeDecimals(
      Attribute<UnosExchangeUnit, Double> attribute) {
    return new CompositeAttributeConverter<UnosExchangeUnit, Double>(attribute,
        CsvFormatUtil.threeDecimals);
  }

  private static <T> CompositeAttributeConverter<UnosExchangeUnit, T> string(
      Attribute<UnosExchangeUnit, T> attribute) {
    return new CompositeAttributeConverter<UnosExchangeUnit, T>(attribute,
        CsvFormatUtil.toStringFormat);
  }

}
