package inputOutput;

import inputOutput.Attributes.AttributeName;
import inputOutput.Attributes.ExchangeIn;
import inputOutput.Attributes.ExchangeOut;
import inputOutput.Attributes.NodeAttributeName;
import inputOutput.Attributes.NodeIn;
import inputOutput.Attributes.NodeOut;
import inputOutput.Attributes.PersonAttributeName;
import inputOutput.Attributes.PersonIn;
import inputOutput.Attributes.ReceiverIn;
import inputOutput.core.Attribute;
import inputOutput.core.AttributeConverter;
import inputOutput.core.Converters;
import inputOutput.core.CsvFormatUtil;
import inputOutput.core.DoubleTimeDifferenceCalculator;
import inputOutput.core.JodaTimeDifference;
import inputOutput.core.Report;
import inputOutput.core.TimeDifferenceCalc;
import inputOutput.node.NodeInputAttributeDefaultConverters;
import inputOutput.node.NodeInputAttributes;
import inputOutput.node.NodeOutputAttributeDefaultConverters;
import inputOutput.node.NodeOutputAttributes;
import inputOutput.node.exchangeUnit.ExchangeInputAttributeDefaultConverters;
import inputOutput.node.exchangeUnit.ExchangeInputAttributes;
import inputOutput.node.exchangeUnit.ExchangeOutputAttributeDefaultConverters;
import inputOutput.node.exchangeUnit.ExchangeOutputAttributes;
import inputOutput.node.exchangeUnit.PersonInputAttributeDefaultConverters;
import inputOutput.node.exchangeUnit.PersonInputAttributes;
import inputOutput.node.exchangeUnit.ReceiverInputAttributeDefaultConverters;
import inputOutput.node.exchangeUnit.ReceiverInputAttributes;
import inputOutput.node.unosExchangeUnit.UnosDonorInputAttributes;
import inputOutput.node.unosExchangeUnit.UnosNodeInputAttributes;
import inputOutput.node.unosExchangeUnit.UnosPatientInputAttributes;

import java.util.ArrayList;
import java.util.List;

import kepModeler.ExchangeUnitAuxiliaryInputStatistics;
import multiPeriod.DynamicMatching;
import multiPeriod.MultiPeriodCyclePacking;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;

import org.joda.time.Interval;
import org.joda.time.ReadableInstant;

import protoModeler.UnosPredicateBuilder.UnosHistoricData;
import replicator.DonorEdge;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;
import data.Donor;
import data.ExchangeUnit;
import data.Receiver;

public class Reports {

  public static enum DonorOptions {
    matchedDonorOnly, matchedDonorOrFirst, donorsInOrder;
  }

  public static interface NodeColumn {
    public String getName();

    public void setName(String name);

    public NodeAttributeName getAttributeName();
  }

  public static class PersonColumn implements NodeColumn {
    private boolean isDonor;
    private DonorOptions donorOption;
    private int donorIndex;
    private PersonAttributeName personAttributeName;
    private String name;

    public static PersonColumn receiverColumn(PersonIn personIn) {
      return new PersonColumn(personIn);
    }

    public static PersonColumn receiverColumn(ReceiverIn receiverIn) {
      return new PersonColumn(receiverIn);
    }

    public static PersonColumn donorColumn(PersonIn personIn,
        DonorOptions donorOption, int donorNumber) {
      String prefix;
      if (donorOption == null) {
        throw new NullPointerException();
      } else if (donorOption == DonorOptions.donorsInOrder) {
        prefix = "Donor " + donorNumber + " ";
      } else if (donorOption == DonorOptions.matchedDonorOnly) {
        prefix = "Matched Donor ";
      } else if (donorOption == DonorOptions.matchedDonorOrFirst) {
        prefix = "Matched/First Donor ";
      } else {
        throw new UnsupportedOperationException();
      }
      return new PersonColumn(true, donorOption, donorNumber, personIn, prefix
          + personIn.getDefaultName());
    }

    private PersonColumn(PersonAttributeName personAttributeName) {
      this(false, null, 0, personAttributeName, "Receiver "
          + personAttributeName.getDefaultName());
    }

    private PersonColumn(boolean isDonor, DonorOptions donorOption,
        int donorIndex, PersonAttributeName personAttributeName, String name) {
      super();
      this.isDonor = isDonor;
      this.donorOption = donorOption;
      this.donorIndex = donorIndex;
      this.personAttributeName = personAttributeName;
      this.name = name;
    }

    @Override
    public String getName() {
      return this.name;
    }

    @Override
    public PersonAttributeName getAttributeName() {
      return personAttributeName;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    public boolean isDonor() {
      return isDonor;
    }

    public DonorOptions getDonorOption() {
      return donorOption;
    }

    public int getDonorIndex() {
      return donorIndex;
    }

  }

  public static class AttributeColumn implements NodeColumn {
    private NodeAttributeName attributeName;
    private String name;

    public AttributeColumn(ExchangeIn exchangeIn) {
      this(exchangeIn, exchangeIn.getDefaultName());
    }

    public AttributeColumn(ExchangeOut exchangeOut) {
      this(exchangeOut, exchangeOut.getDefaultName());
    }

    public AttributeColumn(NodeIn nodeIn) {
      this(nodeIn, nodeIn.getDefaultName());
    }

    public AttributeColumn(NodeOut nodeOut) {
      this(nodeOut, nodeOut.getDefaultName());
    }

    private AttributeColumn(NodeAttributeName nodeAttributeName, String name) {
      this.attributeName = nodeAttributeName;
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public NodeAttributeName getAttributeName() {
      return this.attributeName;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }
  }

  public static Report<ExchangeUnit> makeOutputReport(
      Iterable<? extends NodeAttributeName> attributes, TitleMode titleMode,
      ExchangeOutputConverterSet converterSet, DonorOptions donorOptions,
      int numDonors) {
    List<NodeAttributeName> exchangeUnitAttributes = new ArrayList<NodeAttributeName>();
    List<PersonAttributeName> receiverAttributes = new ArrayList<PersonAttributeName>();
    List<PersonAttributeName> donorAttributes = new ArrayList<PersonAttributeName>();
    for (NodeAttributeName attributeName : attributes) {
      if (attributeName instanceof ExchangeIn
          || attributeName instanceof ExchangeOut
          || attributeName instanceof NodeIn
          || attributeName instanceof NodeOut) {
        exchangeUnitAttributes.add(attributeName);
      } else if (attributeName instanceof PersonIn) {
        receiverAttributes.add((PersonIn) attributeName);
        donorAttributes.add((PersonIn) attributeName);
      } else if (attributeName instanceof ReceiverIn) {
        receiverAttributes.add((ReceiverIn) attributeName);
      } else {
        throw new UnsupportedOperationException("attribute "
            + attributeName.getDefaultName() + " is of unsupported Type "
            + AttributeName.class.getName());
      }
    }
    return makeOutputReport(exchangeUnitAttributes, receiverAttributes,
        donorAttributes, titleMode, converterSet, donorOptions, numDonors);
  }

  private static <T extends NodeAttributeName> void processAttribute(
      List<AttributeConverter<ExchangeUnit>> converters,
      Converters<T, ExchangeUnit> converterMap, T attributeName) {
    converters.add(converterMap.get(attributeName));
  }

  public static Report<ExchangeUnit> makeOutputReport(
      Iterable<? extends NodeColumn> columns, TitleMode titleMode,
      ExchangeOutputConverterSet converterSet) {
    List<String> titles = new ArrayList<String>();
    List<AttributeConverter<ExchangeUnit>> converters = new ArrayList<AttributeConverter<ExchangeUnit>>();
    for (NodeColumn column : columns) {
      if (column instanceof AttributeColumn) {
        if (titleMode == TitleMode.CAMEL_CASE) {
          column.setName(column.getAttributeName().toString());
        }
        titles.add(column.getName());
        NodeAttributeName attributeName = column.getAttributeName();
        if (attributeName instanceof ExchangeIn) {
          processAttribute(converters,
              converterSet.getExchangeInputDefaultConverter(),
              (ExchangeIn) attributeName);
        } else if (attributeName instanceof ExchangeOut) {
          processAttribute(converters,
              converterSet.getExchangeOutputDefaultConverter(),
              (ExchangeOut) attributeName);
        } else if (attributeName instanceof NodeIn) {
          processAttribute(converters, converterSet.getNodeInputConverters(),
              (NodeIn) attributeName);
        } else if (attributeName instanceof NodeOut) {
          processAttribute(converters, converterSet.getNodeOutputConverters(),
              (NodeOut) attributeName);
        } else {
          throw new UnsupportedOperationException("attribute "
              + attributeName.getDefaultName() + " is of unsupported Type "
              + AttributeName.class.getName());
        }
      } else if (column instanceof PersonColumn) {
        PersonColumn personColumn = (PersonColumn) column;
        if (personColumn.isDonor()) {

          titles.add(personColumn.getName());
          AttributeConverter<? super Donor> converter;
          if (personColumn.getAttributeName() instanceof PersonIn) {
            converter = converterSet.getPersonInputAttributeDefaultConverters()
                .get((PersonIn) personColumn.getAttributeName());
          } else {
            throw new UnsupportedOperationException();
          }
          Attribute<ExchangeUnit, Donor> donor;
          if (personColumn.getDonorOption() == DonorOptions.matchedDonorOnly) {
            if (titleMode == TitleMode.CAMEL_CASE) {
              column.setName("matchedDonor-"
                  + column.getAttributeName().toString());
            }
            donor = converterSet.getExchangeOutputDefaultConverter()
                .getExchangeOutAttributes().getMatchedDonor();
          } else if (personColumn.getDonorOption() == DonorOptions.matchedDonorOrFirst) {
            if (titleMode == TitleMode.CAMEL_CASE) {
              column.setName("matchedOrFirstDonor-"
                  + column.getAttributeName().toString());
            }
            donor = converterSet.getExchangeOutputDefaultConverter()
                .getExchangeOutAttributes().getMatchedOrFirstDonor();
          } else if (personColumn.getDonorOption() == DonorOptions.donorsInOrder) {
            if (titleMode == TitleMode.CAMEL_CASE) {
              column.setName("donor" + personColumn.getDonorIndex() + "-"
                  + column.getAttributeName().toString());
            }
            donor = converterSet.getExchangeInputDefaultConverter()
                .getInputAttributes().getIthDonor(personColumn.getDonorIndex());
          } else {
            throw new UnsupportedOperationException();
          }
          converters.add(ExchangeOutputAttributeDefaultConverters
              .makeCompositeDonorNodeAttribute(converter, donor));
        } else {
          if (titleMode == TitleMode.CAMEL_CASE) {
            column.setName("receiver-" + column.getAttributeName().toString());
          }
          titles.add(personColumn.getName());
          AttributeConverter<? super Receiver> converter;
          if (personColumn.getAttributeName() instanceof PersonIn) {
            converter = converterSet.getPersonInputAttributeDefaultConverters()
                .get((PersonIn) personColumn.getAttributeName());
          } else if (personColumn.getAttributeName() instanceof ReceiverIn) {
            converter = converterSet
                .getReceiverInputAttributeDefaultConverters().get(
                    (ReceiverIn) personColumn.getAttributeName());
          } else {
            throw new UnsupportedOperationException();
          }
          converters.add(ExchangeOutputAttributeDefaultConverters
              .receiverToUnit(converter));
        }
      } else {
        throw new UnsupportedOperationException(
            "node column of unrecognized type: " + column.getClass().getName());
      }
    }
    return new Report<ExchangeUnit>(titleMode == TitleMode.NO_TITLES ? null
        : titles, converters);
  }

  public static enum TitleMode {
    SPACES, CAMEL_CASE, NO_TITLES;
  }

  public static Report<ExchangeUnit> makeOutputReport(
      Iterable<? extends NodeAttributeName> exchangeUnitAttributes,
      Iterable<? extends PersonAttributeName> receiverAttributes,
      Iterable<? extends PersonAttributeName> donorAttributes,
      TitleMode titleMode, ExchangeOutputConverterSet converterSet,
      DonorOptions donorOptions, int numDonors) {
    List<NodeColumn> nodeColumns = new ArrayList<NodeColumn>();
    for (NodeAttributeName attributeName : exchangeUnitAttributes) {
      if (attributeName instanceof ExchangeIn) {
        nodeColumns.add(new AttributeColumn((ExchangeIn) attributeName));
      } else if (attributeName instanceof ExchangeOut) {
        nodeColumns.add(new AttributeColumn((ExchangeOut) attributeName));
      } else if (attributeName instanceof NodeIn) {
        nodeColumns.add(new AttributeColumn((NodeIn) attributeName));
      } else if (attributeName instanceof NodeOut) {
        nodeColumns.add(new AttributeColumn((NodeOut) attributeName));
      } else {
        throw new UnsupportedOperationException();
      }
    }
    for (PersonAttributeName receiverName : receiverAttributes) {
      if (receiverName instanceof PersonIn) {
        nodeColumns.add(PersonColumn.receiverColumn((PersonIn) receiverName));
      } else if (receiverName instanceof ReceiverIn) {
        nodeColumns.add(PersonColumn.receiverColumn((ReceiverIn) receiverName));
      } else {
        throw new UnsupportedOperationException();
      }
    }
    for (PersonAttributeName donorName : donorAttributes) {
      if (donorOptions == DonorOptions.donorsInOrder) {
        if (numDonors < 0) {
          throw new RuntimeException();
        }
        for (int i = 0; i < numDonors; i++) {
          if (donorName instanceof PersonIn) {
            nodeColumns.add(PersonColumn.donorColumn((PersonIn) donorName,
                donorOptions, i));
          } else {
            throw new UnsupportedOperationException();
          }
        }
      } else if (donorOptions == DonorOptions.matchedDonorOnly
          || donorOptions == DonorOptions.matchedDonorOrFirst) {
        if (donorName instanceof PersonIn) {
          nodeColumns.add(PersonColumn.donorColumn((PersonIn) donorName,
              donorOptions, 0));
        } else {
          throw new UnsupportedOperationException();
        }
      } else {
        throw new UnsupportedOperationException();
      }

    }
    return Reports.makeOutputReport(nodeColumns, titleMode, converterSet);
  }

  public static class ExchangeOutputConverterSet {

    private NodeInputAttributeDefaultConverters<ExchangeUnit, DonorEdge, ReadableInstant> nodeInputConverters;
    private NodeOutputAttributeDefaultConverters<ExchangeUnit, DonorEdge, ReadableInstant, Interval> nodeOutputConverters;
    private ExchangeInputAttributeDefaultConverters exchangeInputDefaultConverter;
    private ExchangeOutputAttributeDefaultConverters exchangeOutputDefaultConverter;
    private PersonInputAttributeDefaultConverters personInputAttributeDefaultConverters;
    private ReceiverInputAttributeDefaultConverters receiverInputAttributeDefaultConverters;

    public ExchangeOutputConverterSet(
        ExchangeUnitOutputAttributeSet attributeSet) {

      nodeInputConverters = new NodeInputAttributeDefaultConverters<ExchangeUnit, DonorEdge, ReadableInstant>(
          attributeSet.getNodeInputAttributes(),
          CsvFormatUtil.mmddyyyyFormatTimeInstant);
      nodeOutputConverters = new NodeOutputAttributeDefaultConverters<ExchangeUnit, DonorEdge, ReadableInstant, Interval>(
          attributeSet.getNodeOutputAttributes(),
          CsvFormatUtil.mmddyyyyFormatTimeInstant, CsvFormatUtil.numDays);
      exchangeInputDefaultConverter = new ExchangeInputAttributeDefaultConverters(
          attributeSet.getExchangeInputAttributes());
      exchangeOutputDefaultConverter = new ExchangeOutputAttributeDefaultConverters(
          attributeSet.getExchangeOutputAttributes());
      personInputAttributeDefaultConverters = new PersonInputAttributeDefaultConverters(
          attributeSet.getPersonInputAttributes());
      receiverInputAttributeDefaultConverters = new ReceiverInputAttributeDefaultConverters(
          attributeSet.getReceiverInputAttributes());
    }

    public NodeInputAttributeDefaultConverters<ExchangeUnit, DonorEdge, ReadableInstant> getNodeInputConverters() {
      return nodeInputConverters;
    }

    public NodeOutputAttributeDefaultConverters<ExchangeUnit, DonorEdge, ReadableInstant, Interval> getNodeOutputConverters() {
      return nodeOutputConverters;
    }

    public ExchangeInputAttributeDefaultConverters getExchangeInputDefaultConverter() {
      return exchangeInputDefaultConverter;
    }

    public ExchangeOutputAttributeDefaultConverters getExchangeOutputDefaultConverter() {
      return exchangeOutputDefaultConverter;
    }

    public PersonInputAttributeDefaultConverters getPersonInputAttributeDefaultConverters() {
      return personInputAttributeDefaultConverters;
    }

    public ReceiverInputAttributeDefaultConverters getReceiverInputAttributeDefaultConverters() {
      return receiverInputAttributeDefaultConverters;
    }

  }

  public static class ExchangeInputConverterSet {
    private NodeInputAttributeDefaultConverters<ExchangeUnit, DonorEdge, ReadableInstant> nodeInputConverters;
    private ExchangeInputAttributeDefaultConverters exchangeInputDefaultConverter;
    private PersonInputAttributeDefaultConverters personInputAttributeDefaultConverters;
    private ReceiverInputAttributeDefaultConverters receiverInputAttributeDefaultConverters;

    public ExchangeInputConverterSet(ExchangeUnitInputAttributeSet attributeSet) {
      nodeInputConverters = new NodeInputAttributeDefaultConverters<ExchangeUnit, DonorEdge, ReadableInstant>(
          attributeSet.getNodeInputAttributes(),
          CsvFormatUtil.mmddyyyyFormatTimeInstant);
      exchangeInputDefaultConverter = new ExchangeInputAttributeDefaultConverters(
          attributeSet.getExchangeInputAttributes());
      personInputAttributeDefaultConverters = new PersonInputAttributeDefaultConverters(
          attributeSet.getPersonInputAttributes());
      receiverInputAttributeDefaultConverters = new ReceiverInputAttributeDefaultConverters(
          attributeSet.getReceiverInputAttributes());
    }

    public NodeInputAttributeDefaultConverters<ExchangeUnit, DonorEdge, ReadableInstant> getNodeInputConverters() {
      return nodeInputConverters;
    }

    public ExchangeInputAttributeDefaultConverters getExchangeInputDefaultConverter() {
      return exchangeInputDefaultConverter;
    }

    public PersonInputAttributeDefaultConverters getPersonInputAttributeDefaultConverters() {
      return personInputAttributeDefaultConverters;
    }

    public ReceiverInputAttributeDefaultConverters getReceiverInputAttributeDefaultConverters() {
      return receiverInputAttributeDefaultConverters;
    }
  }

  // TODO(rander): this whole class is just kind of a hack so
  // ExchangeUnitOutputAttributeSet has something to extend. It really contains
  // no new information from NodeOutputAttributes<V, E, T, D>. Maybe there is a
  // way to get rid of this class.
  public static class NodeOutputAttributeSet<V, E, T extends Comparable<T>, D> {
    protected NodeInputAttributes<V, E, T> nodeInputAttributes;
    protected NodeOutputAttributes<V, E, T, D> nodeOutputAttributes;

    public NodeOutputAttributeSet(
        MultiPeriodCyclePacking<V, E, T> cyclePacking,
        TimeDifferenceCalc<T, D> timeDifferenceCalc) {
      this(cyclePacking.getInputs(), cyclePacking.getDynamicMatching(),
          timeDifferenceCalc);
    }

    public NodeOutputAttributeSet(
        MultiPeriodCyclePackingInputs<V, E, T> cyclePackingInputs,
        DynamicMatching<V, E, T> dynamicMatching,
        TimeDifferenceCalc<T, D> timeDifferenceCalc) {
      this.nodeInputAttributes = new NodeInputAttributes<V, E, T>(
          cyclePackingInputs);
      this.nodeOutputAttributes = new NodeOutputAttributes<V, E, T, D>(
          cyclePackingInputs, dynamicMatching, timeDifferenceCalc,
          this.nodeInputAttributes);
    }

    public NodeInputAttributes<V, E, T> getNodeInputAttributes() {
      return nodeInputAttributes;
    }

    public NodeOutputAttributes<V, E, T, D> getNodeOutputAttributes() {
      return nodeOutputAttributes;
    }

    public NodeInputAttributeSet<V, E, T> deriveInputAttributeSet() {
      return new NodeInputAttributeSet<V, E, T>(this.nodeInputAttributes);

    }
  }

  public static class UnosOutputAttributeSet extends
      NodeOutputAttributeSet<UnosExchangeUnit, UnosDonorEdge, Double, Double> {

    private UnosHistoricData unosHistoricData;
    private MultiPeriodCyclePackingInputs<UnosExchangeUnit, UnosDonorEdge, Double> cyclePacking;

    public UnosOutputAttributeSet(
        MultiPeriodCyclePackingInputs<UnosExchangeUnit, UnosDonorEdge, Double> cyclePacking,
        DynamicMatching<UnosExchangeUnit, UnosDonorEdge, Double> dynamicMatching,
        UnosHistoricData unosHistoricData) {
      super(cyclePacking, dynamicMatching,
          DoubleTimeDifferenceCalculator.instance);
      this.unosHistoricData = unosHistoricData;
      this.cyclePacking = cyclePacking;
    }

    @Override
    public UnosNodeAttributeSet deriveInputAttributeSet() {
      return new UnosNodeAttributeSet(cyclePacking, unosHistoricData);
    }

  }

  public static class ExchangeUnitOutputAttributeSet
      extends
      NodeOutputAttributeSet<ExchangeUnit, DonorEdge, ReadableInstant, Interval> {

    private ExchangeInputAttributes exchangeInputAttributes;
    private ExchangeOutputAttributes exchangeOutputAttributes;
    private PersonInputAttributes personInputAttributes;
    // private PersonOutputAttributes personOutputAttributes;
    private ReceiverInputAttributes receiverInputAttributes;

    // private ReceiverOutputAttributes receiverOutputAttributes;

    public ExchangeUnitOutputAttributeSet(
        MultiPeriodCyclePacking<ExchangeUnit, DonorEdge, ReadableInstant> cyclePacking) {
      super(cyclePacking, JodaTimeDifference.instance);
      MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs = cyclePacking
          .getInputs();

      this.exchangeInputAttributes = new ExchangeInputAttributes(inputs);
      this.exchangeOutputAttributes = new ExchangeOutputAttributes(cyclePacking);
      this.personInputAttributes = new PersonInputAttributes(inputs);
      this.receiverInputAttributes = new ReceiverInputAttributes(
          (ExchangeUnitAuxiliaryInputStatistics) inputs
              .getAuxiliaryInputStatistics());
    }

    public ExchangeInputAttributes getExchangeInputAttributes() {
      return exchangeInputAttributes;
    }

    public ExchangeOutputAttributes getExchangeOutputAttributes() {
      return exchangeOutputAttributes;
    }

    public PersonInputAttributes getPersonInputAttributes() {
      return personInputAttributes;
    }

    public ReceiverInputAttributes getReceiverInputAttributes() {
      return receiverInputAttributes;
    }

    @Override
    public ExchangeUnitInputAttributeSet deriveInputAttributeSet() {
      return new ExchangeUnitInputAttributeSet(nodeInputAttributes,
          exchangeInputAttributes, personInputAttributes,
          receiverInputAttributes);
    }
  }

  // TODO(rander) this class is a hack so ExchangeUnitInputAttributeSet has
  // something to extend, it has no context beyond NodeInputAttributes, it
  // should be eliminated.
  public static class NodeInputAttributeSet<V, E, T extends Comparable<T>> {
    protected NodeInputAttributes<V, E, T> nodeInputAttributes;
    protected MultiPeriodCyclePackingInputs<V, E, T> inputs;

    public NodeInputAttributeSet(
        NodeInputAttributes<V, E, T> nodeInputAttributes) {
      this.inputs = nodeInputAttributes.getProblemInput();
      this.nodeInputAttributes = nodeInputAttributes;
    }

    public NodeInputAttributeSet(MultiPeriodCyclePackingInputs<V, E, T> inputs) {
      this.inputs = inputs;
      this.nodeInputAttributes = new NodeInputAttributes<V, E, T>(inputs);
    }

    public MultiPeriodCyclePackingInputs<V, E, T> getInputs() {
      return inputs;
    }

    public NodeInputAttributes<V, E, T> getNodeInputAttributes() {
      return nodeInputAttributes;
    }
  }

  public static class UnosNodeAttributeSet extends
      NodeInputAttributeSet<UnosExchangeUnit, UnosDonorEdge, Double> {
    public UnosNodeAttributeSet(
        MultiPeriodCyclePackingInputs<UnosExchangeUnit, UnosDonorEdge, Double> inputs,
        UnosHistoricData unosHistoricData) {
      super(inputs);
      this.unosDonorInputAttributes = new UnosDonorInputAttributes(
          unosHistoricData);
      this.unosPatientInputAttributes = new UnosPatientInputAttributes(
          unosHistoricData);
      this.unosNodeInputAttributes = new UnosNodeInputAttributes();
    }

    public UnosDonorInputAttributes getUnosDonorInputAttributes() {
      return unosDonorInputAttributes;
    }

    public UnosPatientInputAttributes getUnosPatientInputAttributes() {
      return unosPatientInputAttributes;
    }

    public UnosNodeInputAttributes getUnosNodeInputAttributes() {
      return unosNodeInputAttributes;
    }

    private UnosDonorInputAttributes unosDonorInputAttributes;
    private UnosPatientInputAttributes unosPatientInputAttributes;
    private UnosNodeInputAttributes unosNodeInputAttributes;

  }

  public static class ExchangeUnitInputAttributeSet extends
      NodeInputAttributeSet<ExchangeUnit, DonorEdge, ReadableInstant> {

    private ExchangeInputAttributes exchangeInputAttributes;
    private PersonInputAttributes personInputAttributes;
    private ReceiverInputAttributes receiverInputAttributes;

    ExchangeUnitInputAttributeSet(
        NodeInputAttributes<ExchangeUnit, DonorEdge, ReadableInstant> nodeInputAttributes,
        ExchangeInputAttributes exchangeInputAttributes,
        PersonInputAttributes personInputAttributes,
        ReceiverInputAttributes receiverInputAttributes) {
      super(nodeInputAttributes);

      this.exchangeInputAttributes = exchangeInputAttributes;
      this.personInputAttributes = personInputAttributes;
      this.receiverInputAttributes = receiverInputAttributes;
    }

    public ExchangeUnitInputAttributeSet(
        MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs) {
      super(inputs);
      this.exchangeInputAttributes = new ExchangeInputAttributes(inputs);
      this.personInputAttributes = new PersonInputAttributes(inputs);
      this.receiverInputAttributes = new ReceiverInputAttributes(
          (ExchangeUnitAuxiliaryInputStatistics) inputs
              .getAuxiliaryInputStatistics());
    }

    public ExchangeInputAttributes getExchangeInputAttributes() {
      return exchangeInputAttributes;
    }

    public PersonInputAttributes getPersonInputAttributes() {
      return personInputAttributes;
    }

    public ReceiverInputAttributes getReceiverInputAttributes() {
      return receiverInputAttributes;
    }

  }

}
