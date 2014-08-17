package inputOutput.aggregate;

import inputOutput.ExchangePredicates.ExchangeUnitInputPredicates;
import inputOutput.ExchangePredicates.InputPredicates;
import inputOutput.ExchangePredicates.OutputPredicates;
import inputOutput.ExchangePredicates.UnosNodeInputPredicates;
import inputOutput.ExchangePredicates.UnosOutputPredicates;
import inputOutput.Reports.ExchangeUnitOutputAttributeSet;
import inputOutput.Reports.NodeOutputAttributeSet;
import inputOutput.Reports.UnosOutputAttributeSet;
import inputOutput.core.Attribute;
import inputOutput.core.CsvFormat;
import inputOutput.core.CsvFormatUtil;
import inputOutput.core.SumStatVal;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import multiPeriod.DynamicMatching;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.joda.time.Interval;
import org.joda.time.ReadableInstant;

import replicator.DonorEdge;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import data.ExchangeUnit;

public class TrialReport<V, E, T extends Comparable<T>, D> {

  private List<TrialOutcome<V, E, T, D>> trialOutcomes;
  private List<TrialAttribute<V, E, T, D>> trialAttributes;
  private boolean showOutcomeNames;
  private boolean showAttributeNames;
  private boolean outcomesAreRows;

  public static class NkrTrialReport extends
      TrialReport<ExchangeUnit, DonorEdge, ReadableInstant, Interval> {

    public NkrTrialReport(boolean showOutcomeNames, boolean showAttributeNames,
        boolean outcomesAreRows) {
      super(showOutcomeNames, showAttributeNames, outcomesAreRows);
      // TODO Auto-generated constructor stub
    }

    @Override
    protected ExchangeUnitTrialOutcome makeTrialOutcome(
        String name,
        NodeOutputAttributeSet<ExchangeUnit, DonorEdge, ReadableInstant, Interval> attributes) {
      return new ExchangeUnitTrialOutcome(name,
          (ExchangeUnitOutputAttributeSet) attributes);
    }

  }

  public TrialReport(boolean showOutcomeNames, boolean showAttributeNames,
      boolean outcomesAreRows) {
    this.trialOutcomes = Lists.newArrayList();
    this.trialAttributes = Lists.newArrayList();
    this.showOutcomeNames = showOutcomeNames;
    this.showAttributeNames = showAttributeNames;
    this.outcomesAreRows = outcomesAreRows;
  }

  public void addOutcome(String name,
      NodeOutputAttributeSet<V, E, T, D> attributes) {
    this.trialOutcomes.add(makeTrialOutcome(name, attributes));
  }

  public void addOutcome(TrialOutcome<V, E, T, D> trialOutcome) {
    this.trialOutcomes.add(trialOutcome);
  }

  protected TrialOutcome<V, E, T, D> makeTrialOutcome(String name,
      NodeOutputAttributeSet<V, E, T, D> attributes) {
    return new TrialOutcome<V, E, T, D>(name, attributes);
  }

  public void addAttribute(TrialAttribute<V, E, T, D> attribute) {
    this.trialAttributes.add(attribute);
  }

  public void addAttributes(
      Iterable<? extends TrialAttribute<V, E, T, D>> attributes) {
    for (TrialAttribute<V, E, T, D> attribute : attributes) {
      this.addAttribute(attribute);
    }
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
          for (int i = 0; i < trialAttributes.size(); i++) {
            printer.print(this.trialAttributes.get(i).getAttributeName());
          }
          printer.println();
        }
        for (TrialOutcome<V, E, T, D> outcome : this.trialOutcomes) {
          if (this.showOutcomeNames) {
            printer.print(outcome.getName());
          }
          for (int i = 0; i < trialAttributes.size(); i++) {
            String result = trialAttributes.get(i).apply(outcome);
            printer.print(result);
          }
          printer.println();
        }
      } else {
        if (this.showOutcomeNames) {
          if (this.showAttributeNames) {
            printer.print("attributes");
          }
          for (int i = 0; i < this.trialOutcomes.size(); i++) {
            printer.print(this.trialOutcomes.get(i).getName());
          }
          printer.println();
        }
        for (TrialAttribute<V, E, T, D> attribute : this.trialAttributes) {
          if (this.showAttributeNames) {
            printer.print(attribute.getAttributeName());
          }
          for (int i = 0; i < this.trialOutcomes.size(); i++) {
            printer.print(attribute.apply(trialOutcomes.get(i)));
          }
          printer.println();
        }
      }
      printer.flush();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  public static class ExchangeUnitTrialOutcome extends
      TrialOutcome<ExchangeUnit, DonorEdge, ReadableInstant, Interval> {
    private ExchangeUnitOutputAttributeSet attributes;
    private ExchangeUnitInputPredicates inputPredicates;

    // private OutputPredicates<ExchangeUnit, DonorEdge, ReadableInstant,
    // Interval> outputPredicates;

    public ExchangeUnitTrialOutcome(String name,
        ExchangeUnitOutputAttributeSet attributes) {
      super(name, attributes);
      this.attributes = attributes;
      this.inputPredicates = (ExchangeUnitInputPredicates) super
          .getInputPredicates();
    }

    @Override
    public ExchangeUnitInputPredicates getInputPredicates() {
      return this.inputPredicates;
    }

    @Override
    public ExchangeUnitOutputAttributeSet getAttributes() {
      return attributes;
    }

    @Override
    protected ExchangeUnitInputPredicates createInputPredicates(
        NodeOutputAttributeSet<ExchangeUnit, DonorEdge, ReadableInstant, Interval> attributeSet) {
      ExchangeUnitOutputAttributeSet attributes = (ExchangeUnitOutputAttributeSet) attributeSet;
      return new ExchangeUnitInputPredicates(
          attributes.deriveInputAttributeSet());
    }

  }

  public static class UnosTrialOutcome extends
      TrialOutcome<UnosExchangeUnit, UnosDonorEdge, Double, Double> {
    private UnosOutputAttributeSet attributes;
    private UnosNodeInputPredicates inputPredicates;
    private UnosOutputPredicates unosOutputPredicates;

    // private OutputPredicates<ExchangeUnit, DonorEdge, ReadableInstant,
    // Interval> outputPredicates;

    public UnosTrialOutcome(String name, UnosOutputAttributeSet attributes) {
      super(name, attributes, new UnosOutputPredicates(attributes));
      this.attributes = attributes;
      this.inputPredicates = (UnosNodeInputPredicates) super
          .getInputPredicates();
      this.unosOutputPredicates = (UnosOutputPredicates) super
          .getOutputPredicates();
    }

    @Override
    public UnosNodeInputPredicates getInputPredicates() {
      return this.inputPredicates;
    }

    @Override
    public UnosOutputPredicates getOutputPredicates() {
      return this.unosOutputPredicates;
    }

    @Override
    protected UnosNodeInputPredicates createInputPredicates(
        NodeOutputAttributeSet<UnosExchangeUnit, UnosDonorEdge, Double, Double> attributeSet) {
      UnosOutputAttributeSet attributes = (UnosOutputAttributeSet) attributeSet;
      return new UnosNodeInputPredicates(attributes.deriveInputAttributeSet());
    }

  }

  public static class TrialOutcome<V, E, T extends Comparable<T>, D> {
    private NodeOutputAttributeSet<V, E, T, D> attributes;
    private InputPredicates<V, E, T, D> inputPredicates;
    private OutputPredicates<V, E, T, D> outputPredicates;
    private MultiPeriodCyclePackingInputs<V, E, T> packingInputs;
    private DynamicMatching<V, E, T> dynamicMatching;
    private String name;

    public NodeOutputAttributeSet<V, E, T, D> getAttributes() {
      return attributes;
    }

    public MultiPeriodCyclePackingInputs<V, E, T> getPackingInputs() {
      return packingInputs;
    }

    public DynamicMatching<V, E, T> getDynamicMatching() {
      return this.dynamicMatching;
    }

    public OutputPredicates<V, E, T, D> getOutputPredicates() {
      return outputPredicates;
    }

    public String getName() {
      return this.name;
    }

    public InputPredicates<V, E, T, D> getInputPredicates() {
      return this.inputPredicates;
    }

    protected InputPredicates<V, E, T, D> createInputPredicates(
        NodeOutputAttributeSet<V, E, T, D> attributeSet) {
      return new InputPredicates<V, E, T, D>(
          attributeSet.deriveInputAttributeSet());
    }

    public TrialOutcome(String name,
        NodeOutputAttributeSet<V, E, T, D> attributes) {
      this(name, attributes, new OutputPredicates<V, E, T, D>(attributes));
    }

    public TrialOutcome(String name,
        NodeOutputAttributeSet<V, E, T, D> attributes,
        OutputPredicates<V, E, T, D> outputPredicates) {
      super();
      this.attributes = attributes;
      this.dynamicMatching = attributes.getNodeOutputAttributes()
          .getDynamicMatching();
      this.packingInputs = attributes.getNodeOutputAttributes()
          .getCyclePackingInputs();
      this.inputPredicates = createInputPredicates(attributes);
      this.outputPredicates = outputPredicates;
      this.name = name;
    }
  }

  public static interface TrialAttribute<V, E, T extends Comparable<T>, D>
      extends Function<TrialOutcome<V, E, T, D>, String> {
    public String getAttributeName();
  }

  public static interface NumericTrialAttribute<V, E, T extends Comparable<T>, D>
      extends TrialAttribute<V, E, T, D> {
    public double applyForDouble(TrialOutcome<V, E, T, D> trialOutcome);
  }

  public static abstract class BaseNumericTrialAttribute<V, E, T extends Comparable<T>, D>
      implements NumericTrialAttribute<V, E, T, D> {

    @Override
    public String apply(TrialOutcome<V, E, T, D> trialOutcome) {
      return Double.toString(applyForDouble(trialOutcome));
    }
  }

  public abstract static class NodeAggregation<V, E, T extends Comparable<T>, D>
      implements NumericTrialAttribute<V, E, T, D> {

    protected abstract Attribute<? super V, ? extends Number> getAttribute(
        TrialOutcome<V, E, T, D> trialOutcome);

    protected abstract Predicate<V> getPredicate(
        TrialOutcome<V, E, T, D> trialOutcome);

    private String attributeName;
    private SumStatVal sumStatVal;
    private CsvFormat<StatisticalSummary> csvFormat;

    protected NodeAggregation(String attributeName, SumStatVal sumStatVal) {
      this.attributeName = attributeName;
      this.sumStatVal = sumStatVal;
      this.csvFormat = CsvFormatUtil.statisticalSummaryFormatters
          .get(sumStatVal);
    }

    protected NodeAggregation(String attributeName) {
      this(attributeName, SumStatVal.MEAN);
    }

    private SummaryStatistics computeStatistics(
        TrialOutcome<V, E, T, D> trialOutcome) {
      Attribute<? super V, ? extends Number> attribute = getAttribute(trialOutcome);
      Iterable<V> units = trialOutcome.getPackingInputs().getGraph()
          .getVertices();
      Predicate<V> pred = getPredicate(trialOutcome);
      SummaryStatistics stats = new SummaryStatistics();
      for (Number value : Iterables.transform(Iterables.filter(units, pred),
          attribute)) {
        stats.addValue(value.doubleValue());
      }
      return stats;
    }

    public double applyForDouble(TrialOutcome<V, E, T, D> trialOutcome) {
      return sumStatVal.getFunction().apply(computeStatistics(trialOutcome));
    }

    public String apply(TrialOutcome<V, E, T, D> trialOutcome) {
      SummaryStatistics stats = computeStatistics(trialOutcome);
      return csvFormat.apply(stats);
    }

    public String getAttributeName() {
      return this.attributeName;
    }

  }

}
