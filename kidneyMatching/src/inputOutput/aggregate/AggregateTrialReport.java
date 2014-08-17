package inputOutput.aggregate;

import inputOutput.Reports.NodeOutputAttributeSet;
import inputOutput.Reports.UnosOutputAttributeSet;
import inputOutput.aggregate.TrialReport.NumericTrialAttribute;
import inputOutput.aggregate.TrialReport.TrialOutcome;
import inputOutput.aggregate.TrialReport.UnosTrialOutcome;
import inputOutput.core.TimeDifferenceCalc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import kepProtos.KepProtos;
import kepProtos.KepProtos.Simulation;
import kepProtos.KepProtos.SimulationInputs;
import multiPeriod.DynamicMatching;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;
import multiPeriodAnalysis.Environment;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import protoModeler.UnosPredicateBuilder.UnosHistoricData;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.Lists;

public class AggregateTrialReport<V, E, T extends Comparable<T>, D> {

  // The outer list corresponds to different simulations, the inner list
  // corresponds to different random trials for the same simulation.
  private ArrayTable<SimulationInputs, Integer, TrialOutcome<V, E, T, D>> trialOutcomes;
  private List<NumericTrialAttribute<V, E, T, D>> nodeAggregationAttributes;
  private boolean showOutcomeNames;
  private boolean showAttributeNames;
  private boolean outcomesAreRows;

  public AggregateTrialReport(boolean showOutcomeNames,
      boolean showAttributeNames, boolean outcomesAreRows,
      List<SimulationInputs> simulationInputs, int numTrialsPerSimulation) {
    this.showAttributeNames = showAttributeNames;
    this.showOutcomeNames = showOutcomeNames;
    this.outcomesAreRows = outcomesAreRows;
    List<Integer> columns = Lists.newArrayList();
    for (int i = 0; i < numTrialsPerSimulation; i++) {
      columns.add(i);
    }
    trialOutcomes = ArrayTable.create(simulationInputs, columns);
    nodeAggregationAttributes = Lists.newArrayList();
  }

  public void addAllTrialOutcomes(String name, Simulation simulation,
      Environment<V, E, T> environment, TimeDifferenceCalc<T, D> timeDifference) {
    int trials = environment.getReplications();
    // using the below instead will cause an error if a cached simulation has
    // more trails than the simulation desired.
    // simulation.getRandomTrialDynamicMatchingCount();
    for (int i = 0; i < trials; i++) {
      KepProtos.DynamicMatching protoMatching = simulation
          .getRandomTrialDynamicMatching(i);
      DynamicMatching<V, E, T> dynamicMatching = environment
          .restoreDynamicMatching(protoMatching);
      TrialOutcome<V, E, T, D> trialOutcome = createTrialOutcome(name,
          environment.alternateArrivalTimes(simulation.getRandomTrial(i)),
          dynamicMatching, timeDifference);
      trialOutcomes.put(simulation.getSimulationInputs(), i, trialOutcome);
    }
  }

  protected TrialOutcome<V, E, T, D> createTrialOutcome(String name,
      MultiPeriodCyclePackingInputs<V, E, T> inputs,
      DynamicMatching<V, E, T> dynamicMatching,
      TimeDifferenceCalc<T, D> timeDifference) {
    NodeOutputAttributeSet<V, E, T, D> attributeSet = new NodeOutputAttributeSet<V, E, T, D>(
        inputs, dynamicMatching, timeDifference);
    return new TrialOutcome<V, E, T, D>(name, attributeSet);
  }

  public void addTrialOutcome(SimulationInputs simulation, int randomTrial,
      TrialOutcome<V, E, T, D> trialOutcome) {
    trialOutcomes.put(simulation, randomTrial, trialOutcome);
  }

  public void addNodeAggregationAttribute(
      NumericTrialAttribute<V, E, T, D> nodeAggregationAttribute) {
    nodeAggregationAttributes.add(nodeAggregationAttribute);
  }

  private String getValue(SimulationInputs simulation,
      NumericTrialAttribute<V, E, T, D> nodeAggregation) {
    SummaryStatistics summaryStatistics = new SummaryStatistics();
    for (TrialOutcome<V, E, T, D> outcome : this.trialOutcomes.row(simulation)
        .values()) {
      summaryStatistics.addValue(nodeAggregation.applyForDouble(outcome));
    }
    return Double.toString(summaryStatistics.getMean());
  }

  public void writeReport(String fileName, CSVFormat format) {

    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
      CSVPrinter printer = new CSVPrinter(writer, format);
      if (this.outcomesAreRows) {
        if (this.showAttributeNames) {
          if (showOutcomeNames) {
            printer.print("trials");
          }
          for (int i = 0; i < nodeAggregationAttributes.size(); i++) {
            printer.print(this.nodeAggregationAttributes.get(i)
                .getAttributeName());
          }
          printer.println();
        }
        for (SimulationInputs simulationInput : this.trialOutcomes.rowKeyList()) {
          if (this.showOutcomeNames) {
            printer.print(trialOutcomes
                .get(simulationInput, Integer.valueOf(0)).getName());
          }
          for (NumericTrialAttribute<V, E, T, D> nodeAggregation : this.nodeAggregationAttributes) {
            String result = getValue(simulationInput, nodeAggregation);
            printer.print(result);
          }
          printer.println();
        }
      } else {
        if (this.showOutcomeNames) {
          if (this.showAttributeNames) {
            printer.print("attributes");
          }
          for (SimulationInputs simulationInput : this.trialOutcomes
              .rowKeyList()) {
            printer.print(trialOutcomes
                .get(simulationInput, Integer.valueOf(0)).getName());
          }
          printer.println();
        }
        for (NumericTrialAttribute<V, E, T, D> nodeAggregation : this.nodeAggregationAttributes) {
          if (this.showAttributeNames) {
            printer.print(nodeAggregation.getAttributeName());
          }
          for (SimulationInputs simulationInput : this.trialOutcomes
              .rowKeyList()) {
            String result = getValue(simulationInput, nodeAggregation);
            printer.print(result);
          }
          printer.println();
        }
      }
      printer.flush();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  public static class UnosAggregateTrialReport extends
      AggregateTrialReport<UnosExchangeUnit, UnosDonorEdge, Double, Double> {

    private UnosHistoricData unosHistoricData;

    public UnosAggregateTrialReport(boolean showOutcomeNames,
        boolean showAttributeNames, boolean outcomesAreRows,
        List<SimulationInputs> simulationInputs, int numTrialsPerSimulation,
        UnosHistoricData unosHistoricData) {
      super(showOutcomeNames, showAttributeNames, outcomesAreRows,
          simulationInputs, numTrialsPerSimulation);
      this.unosHistoricData = unosHistoricData;

    }

    @Override
    protected UnosTrialOutcome createTrialOutcome(
        String name,
        MultiPeriodCyclePackingInputs<UnosExchangeUnit, UnosDonorEdge, Double> inputs,
        DynamicMatching<UnosExchangeUnit, UnosDonorEdge, Double> dynamicMatching,
        TimeDifferenceCalc<Double, Double> timeDifference) {
      UnosOutputAttributeSet attributeSet = new UnosOutputAttributeSet(inputs,
          dynamicMatching, unosHistoricData);
      return new UnosTrialOutcome(name, attributeSet);
    }

  }
}
