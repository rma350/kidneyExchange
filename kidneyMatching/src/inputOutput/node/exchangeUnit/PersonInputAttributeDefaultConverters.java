package inputOutput.node.exchangeUnit;

import inputOutput.Attributes.PersonIn;
import inputOutput.core.Attribute;
import inputOutput.core.AttributeConverter;
import inputOutput.core.CompositeAttributeConverter;
import inputOutput.core.Converters;
import inputOutput.core.CsvFormat;
import inputOutput.core.CsvFormatUtil;
import inputOutput.node.exchangeUnit.PersonInputAttributes.HlaTypeAttribute;
import inputOutput.node.exchangeUnit.PersonInputAttributes.SpecialHlaAttribute;

import java.util.EnumMap;

import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;

import org.joda.time.ReadableInstant;

import replicator.DonorEdge;
import data.ExchangeUnit;
import data.HlaType;
import data.Person;
import data.SpecialHla;

public class PersonInputAttributeDefaultConverters implements
    Converters<PersonIn, Person> {

  private EnumMap<PersonIn, AttributeConverter<Person>> converters;
  private PersonInputAttributes inputAttributes;

  public PersonInputAttributeDefaultConverters(
      MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> inputs) {
    this(new PersonInputAttributes(inputs));
  }

  private <T> void add(PersonIn attributeName, Attribute<Person, T> attribute,
      CsvFormat<? super T> format) {
    converters.put(attributeName, new CompositeAttributeConverter<Person, T>(
        attribute, format));
  }

  protected AttributeConverter<Person> getHlaConverter(HlaType type,
      boolean isLow) {
    HlaTypeAttribute attribute = isLow ? inputAttributes.getHlaLow().get(type)
        : inputAttributes.getHlaHigh().get(type);
    return new CompositeAttributeConverter<Person, Integer>(attribute,
        CsvFormatUtil.toStringNullBlank);
  }

  private void addHla(PersonIn attributeName, HlaType hlaType, boolean isLow) {
    converters.put(attributeName, getHlaConverter(hlaType, isLow));
  }

  protected CsvFormat<? super Boolean> getSpecialHlaFormat() {
    return CsvFormatUtil.toStringNullBlank;
  }

  private void addHlaSpecial(PersonIn attributeName, SpecialHla specialHla) {
    SpecialHlaAttribute attribute = inputAttributes.getSpecialHla().get(
        specialHla);
    add(attributeName, attribute, getSpecialHlaFormat());
  }

  public PersonInputAttributeDefaultConverters(
      PersonInputAttributes personInputAttributes) {
    this.inputAttributes = personInputAttributes;
    this.converters = new EnumMap<PersonIn, AttributeConverter<Person>>(
        PersonIn.class);
    add(PersonIn.yearBorn, inputAttributes.getYearBorn(), CsvFormatUtil.year);
    add(PersonIn.id, inputAttributes.getId(), CsvFormatUtil.toStringFormat);
    add(PersonIn.registered, inputAttributes.getRegistered(),
        CsvFormatUtil.mmddyyyyFormat);
    add(PersonIn.gender, inputAttributes.getGender(),
        CsvFormatUtil.genderFormat);
    add(PersonIn.race, inputAttributes.getRace(), CsvFormatUtil.raceFormat);
    add(PersonIn.bloodType, inputAttributes.getBloodType(),
        CsvFormatUtil.bloodTypeFormat);

    addHla(PersonIn.hlaA1, HlaType.A, true);
    addHla(PersonIn.hlaA2, HlaType.A, false);
    addHla(PersonIn.hlaB1, HlaType.B, true);
    addHla(PersonIn.hlaB2, HlaType.B, false);
    addHla(PersonIn.hlaCw1, HlaType.Cw, true);
    addHla(PersonIn.hlaCw2, HlaType.Cw, false);
    addHla(PersonIn.hlaDP1, HlaType.DP, true);
    addHla(PersonIn.hlaDP2, HlaType.DP, false);
    addHla(PersonIn.hlaDQ1, HlaType.DQ, true);
    addHla(PersonIn.hlaDQ2, HlaType.DQ, false);
    addHla(PersonIn.hlaDR1, HlaType.DR, true);
    addHla(PersonIn.hlaDR2, HlaType.DR, false);

    addHlaSpecial(PersonIn.hlaBw4, SpecialHla.Bw4);
    addHlaSpecial(PersonIn.hlaBw6, SpecialHla.Bw6);

    addHlaSpecial(PersonIn.hlaDR51, SpecialHla.DR51);
    addHlaSpecial(PersonIn.hlaDR52, SpecialHla.DR52);
    addHlaSpecial(PersonIn.hlaDR53, SpecialHla.DR53);

    add(PersonIn.heightCm, inputAttributes.getHeightCm(),
        CsvFormatUtil.toStringNullBlank);
    add(PersonIn.weightKg, inputAttributes.getWeightKg(),
        CsvFormatUtil.toStringNullBlank);
    add(PersonIn.hospital, inputAttributes.getHospital(),
        CsvFormatUtil.hospitalFormat);
  }

  private CompositeAttributeConverter<Person, Integer> makeHlaLowConverter(
      HlaType hlaType) {
    return new CompositeAttributeConverter<Person, Integer>(inputAttributes
        .getHlaLow().get(hlaType), CsvFormatUtil.toStringNullBlank);
  }

  protected EnumMap<PersonIn, AttributeConverter<Person>> getConverters() {
    return this.converters;
  }

  protected PersonInputAttributes getInputAttributes() {
    return this.inputAttributes;
  }

  @Override
  public AttributeConverter<Person> get(PersonIn attributeName) {
    if (!this.converters.containsKey(attributeName)) {
      throw new UnsupportedOperationException();
    }
    return this.converters.get(attributeName);
  }

}
