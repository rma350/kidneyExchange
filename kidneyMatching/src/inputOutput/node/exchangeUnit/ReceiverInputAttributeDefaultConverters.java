package inputOutput.node.exchangeUnit;

import inputOutput.Attributes.ReceiverIn;
import inputOutput.core.AttributeConverter;
import inputOutput.core.CompositeAttributeConverter;
import inputOutput.core.Converters;
import inputOutput.core.CsvFormat;
import inputOutput.core.CsvFormatUtil;

import java.util.EnumMap;
import java.util.List;

import org.joda.time.Period;

import data.HlaType;
import data.Receiver;
import data.SpecialHla;

public class ReceiverInputAttributeDefaultConverters implements
    Converters<ReceiverIn, Receiver> {

  private ReceiverInputAttributes inputAttributes;
  private EnumMap<ReceiverIn, AttributeConverter<Receiver>> converters;

  public ReceiverInputAttributeDefaultConverters(
      ReceiverInputAttributes inputAttributes) {
    this.inputAttributes = inputAttributes;
    converters = new EnumMap<ReceiverIn, AttributeConverter<Receiver>>(
        ReceiverIn.class);

    converters.put(ReceiverIn.antibodiesA, makeHlaSensitivity(HlaType.A));
    converters.put(ReceiverIn.antibodiesB, makeHlaSensitivity(HlaType.B));
    converters.put(ReceiverIn.antibodiesDQ, makeHlaSensitivity(HlaType.DQ));
    converters.put(ReceiverIn.antibodiesDP, makeHlaSensitivity(HlaType.DP));
    converters.put(ReceiverIn.antibodiesDR, makeHlaSensitivity(HlaType.DR));

    converters.put(ReceiverIn.antibodiesCw, makeHlaSensitivity(HlaType.Cw));
    converters.put(ReceiverIn.antibodySpecialBw4,
        makeSpecialHla(SpecialHla.Bw4));
    converters.put(ReceiverIn.antibodySpecialBw6,
        makeSpecialHla(SpecialHla.Bw6));

    converters.put(ReceiverIn.antibodySpecialDR51,
        makeSpecialHla(SpecialHla.DR51));
    converters.put(ReceiverIn.antibodySpecialDR52,
        makeSpecialHla(SpecialHla.DR52));

    converters.put(ReceiverIn.antibodySpecialDR53,
        makeSpecialHla(SpecialHla.DR53));
    converters.put(
        ReceiverIn.minDonorAge,
        new CompositeAttributeConverter<Receiver, Period>(inputAttributes
            .getMinDonorAgeYr(), CsvFormatUtil.getYears));
    converters.put(
        ReceiverIn.maxDonorAge,
        new CompositeAttributeConverter<Receiver, Period>(inputAttributes
            .getMaxDonorAgeYr(), CsvFormatUtil.getYears));
    converters.put(
        ReceiverIn.maxDonorAge,
        new CompositeAttributeConverter<Receiver, Double>(inputAttributes
            .getVpra(), CsvFormatUtil.toStringNullBlank));
    converters.put(
        ReceiverIn.minHlaMatch,
        new CompositeAttributeConverter<Receiver, Integer>(inputAttributes
            .getMinHlaScore(), CsvFormatUtil.toStringNullBlank));
    converters.put(
        ReceiverIn.minDonorWeight,
        new CompositeAttributeConverter<Receiver, Integer>(inputAttributes
            .getMinDonorWeightKg(), CsvFormatUtil.toStringNullBlank));
    converters.put(
        ReceiverIn.acceptShippedKidney,
        new CompositeAttributeConverter<Receiver, Boolean>(inputAttributes
            .getAcceptShippedKidney(), CsvFormatUtil.yesNo));
    converters.put(
        ReceiverIn.willingToTravel,
        new CompositeAttributeConverter<Receiver, Boolean>(inputAttributes
            .getWillingToTravel(), CsvFormatUtil.yesNo));
  }

  private static CsvFormat<List<? extends Object>> defaultListFormat = CsvFormatUtil
      .makeListFormat(CsvFormatUtil.toStringFormat, "|");

  private AttributeConverter<Receiver> makeHlaSensitivity(HlaType hlaType) {
    return new CompositeAttributeConverter<Receiver, List<Integer>>(
        inputAttributes.getHlaSensitivityAttributes().get(hlaType),
        defaultListFormat);
  }

  private AttributeConverter<Receiver> makeSpecialHla(SpecialHla specialHla) {
    return new CompositeAttributeConverter<Receiver, Boolean>(inputAttributes
        .getSpecialHlaSensitivityAttributes().get(specialHla),
        CsvFormatUtil.specialHlaFormat);
  }

  @Override
  public AttributeConverter<Receiver> get(ReceiverIn attributeName) {
    if (!this.converters.containsKey(attributeName)) {
      throw new UnsupportedOperationException(attributeName.getDefaultName());
    }
    return this.converters.get(attributeName);
  }

}
