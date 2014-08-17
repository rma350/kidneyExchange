package unosData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import data.BloodType;
import data.Genotype;
import data.HlaType;
import data.ProblemData;
import data.Race;
import data.SpecialHla;
import data.TissueType;
import data.TissueTypeSensitivity;

public class UnosParseData {

  private final ImmutableBiMap<UnosDataHeader, String> dataHeaderNames;
  private final ImmutableBiMap<UnosDataHeader, Integer> headerColumns;
  private final Set<Long> observedIds;
  private final ImmutableSet<UnosDonor> altruisticDonors;
  private final ImmutableMap<UnosDonor, Long> donorToPatientId;
  private final ImmutableMap<Long, UnosPatient> patientIdToPatient;
  private final boolean printWarnings;

  public UnosParseData(String fileName, boolean printWarnings) {
    this(fileName, printWarnings, UnosDataHeader.ALL_DEFAULT_NAMES);
  }

  private void printWarning(String message) {
    if (printWarnings) {
      System.err.println(message);
    }
  }

  public UnosParseData(String fileName, boolean printWarnings,
      ImmutableBiMap<UnosDataHeader, String> dataHeaderNames) {
    this.dataHeaderNames = dataHeaderNames;
    this.printWarnings = printWarnings;
    ImmutableMap.Builder<UnosDonor, Long> donorToPatientIdBuilder = ImmutableMap
        .builder();
    ImmutableMap.Builder<Long, UnosPatient> patientIdToPatientBuilder = ImmutableMap
        .builder();
    ImmutableSet.Builder<UnosDonor> altruisticDonorsBuilder = ImmutableSet
        .builder();
    observedIds = Sets.newHashSet();
    try {
      CSVParser parser = new CSVParser(new BufferedReader(new FileReader(
          fileName)), CSVFormat.EXCEL);
      List<CSVRecord> records = parser.getRecords();
      if (records.size() < 1) {
        throw new RuntimeException(
            "Error: file must have at least one line, but had "
                + records.size());
      }
      headerColumns = getHeaderColumns(records.get(0));
      for (int i = 1; i < records.size(); i++) {
        CSVRecord record = records.get(i);
        long id = Long.parseLong(read(UnosDataHeader.personId, record));
        if (observedIds.contains(id)) {
          printWarning("Warning: found duplicate entry for id: " + id);
          continue;
        } else {
          observedIds.add(id);
        }
        if (isDonor(read(UnosDataHeader.isDonor, record))) {
          UnosDonor donor = readDonor(record);
          String relatedPatientString = read(UnosDataHeader.relatedPatientId,
              record).trim();
          if (relatedPatientString.isEmpty()) {
            altruisticDonorsBuilder.add(donor);
          } else {
            long relatedPatient = Long.parseLong(relatedPatientString);
            donorToPatientIdBuilder.put(donor, relatedPatient);
          }
        } else {
          UnosPatient patient = readPatient(record);
          patientIdToPatientBuilder.put(patient.getId(), patient);
        }
      }
      this.altruisticDonors = altruisticDonorsBuilder.build();
      this.patientIdToPatient = patientIdToPatientBuilder.build();
      this.donorToPatientId = donorToPatientIdBuilder.build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ImmutableMap<UnosDonor, Long> getDonorToPatientId() {
    return donorToPatientId;
  }

  public ImmutableMap<Long, UnosPatient> getPatientIdToPatient() {
    return patientIdToPatient;
  }

  public ImmutableSet<UnosDonor> getAltruisticDonors() {
    return this.altruisticDonors;
  }

  private String read(UnosDataHeader header, CSVRecord record) {
    return record.get(headerColumns.get(header));
  }

  private UnosPatient readPatient(CSVRecord record) {
    long id = Long.parseLong(read(UnosDataHeader.personId, record));
    DateTime arrivalTime = DateTime.parse(
        read(UnosDataHeader.arrivalTime, record), dateTimeFormatter);
    BloodType bloodType = parseBloodType(read(UnosDataHeader.bloodType, record));
    int maxAgeDonorYears = Integer.parseInt(read(UnosDataHeader.maxAgeForDonor,
        record));
    TissueTypeSensitivity tissueTypeSens = parseTissueTypeSensitivity(record);
    Race race = parseRace(read(UnosDataHeader.race, record));
    int center = Integer.parseInt(read(UnosDataHeader.center, record));
    int age = Integer.parseInt(read(UnosDataHeader.age, record));
    double cPra = Double.parseDouble(read(UnosDataHeader.cPra, record));
    return new UnosPatient(id, arrivalTime, bloodType, maxAgeDonorYears,
        tissueTypeSens, race, center, cPra, age);
  }

  private static DateTimeFormatter dateTimeFormatter = DateTimeFormat
      .forPattern("MM/dd/yyyy");

  private UnosDonor readDonor(CSVRecord record) {
    long id = Long.parseLong(read(UnosDataHeader.personId, record));
    DateTime arrivalTime = DateTime.parse(
        read(UnosDataHeader.arrivalTime, record), dateTimeFormatter);
    int age = Integer.parseInt(read(UnosDataHeader.age, record));
    BloodType bloodType = parseBloodType(read(UnosDataHeader.bloodType, record));
    TissueType tissueType = parseTissueType(record);
    Race race = parseRace(read(UnosDataHeader.race, record));
    int center = Integer.parseInt(read(UnosDataHeader.center, record));
    return new UnosDonor(id, arrivalTime, age, bloodType, tissueType, race,
        center);
  }

  private Race parseRace(String raceName) {
    String trimmed = raceName.trim();
    if (trimmed.equalsIgnoreCase("Caucasian")) {
      return Race.WHITE;
    } else if (trimmed.equalsIgnoreCase("Black")) {
      return Race.BLACK;
    } else if (trimmed.equalsIgnoreCase("Latino")) {
      return Race.HISPANIC;
    } else if (trimmed.equalsIgnoreCase("Asian")) {
      return Race.ASIAN;
    } else if (trimmed.equalsIgnoreCase("Amer Ind")) {
      return Race.NATIVE_AMERICAN;
    } else if (trimmed.equalsIgnoreCase("Pacific")) {
      return Race.PACIFIC_ISLANDER;
    } else if (trimmed.equalsIgnoreCase("Multiracial")) {
      return Race.MULTIRACIAL;
    } else if (trimmed.isEmpty()) {
      return Race.UNKNOWN;
    } else {
      printWarning("Unrecognized race: " + trimmed);
      return Race.OTHER;
    }
  }

  private TissueTypeSensitivity parseTissueTypeSensitivity(CSVRecord record) {
    TissueTypeSensitivity ans = new TissueTypeSensitivity();
    EnumMap<HlaType, int[]> hlaAntibodies = ans.getAntibodies();
    for (HlaParseStruct parseStruct : parseStructs) {
      String listAnti = read(parseStruct.getAntibodies(), record).trim();
      String[] anitArray = ProblemData.properBarSplit(listAnti);
      hlaAntibodies.put(parseStruct.getHlaType(),
          ProblemData.convertToInts(anitArray));
    }
    EnumMap<SpecialHla, Boolean> specialAntibodies = ans
        .getAvoidsSpecialAntibodies();
    for (HlaSpecialParseStruct special : specialHlaStructs) {
      specialAntibodies.put(special.getHla(),
          readZeroOneToBoolean(read(special.getImmunity(), record)));
    }
    return ans;
  }

  private TissueType parseTissueType(CSVRecord record) {
    TissueType ans = new TissueType();
    EnumMap<HlaType, Genotype> hlaTypes = ans.getHlaTypes();

    for (HlaParseStruct parseStruct : parseStructs) {
      String firstColumn = read(parseStruct.getFirstColumn(), record).trim();
      if (!firstColumn.isEmpty()) {
        int hla1 = Integer.parseInt(firstColumn);
        String hla2String = read(parseStruct.getSecondColumn(), record).trim();
        int hla2;
        if (hla2String.isEmpty()) {
          hla2 = hla1;
        } else {
          hla2 = Integer.parseInt(hla2String);
          if (hla2 == -1) {
            hla2 = hla1;
          }
        }
        Genotype genotype = new Genotype(hla1, hla2);
        hlaTypes.put(parseStruct.getHlaType(), genotype);
      } else {
        printWarning("Was missing HLA " + parseStruct.getHlaType()
            + "1 on record: " + record);
        Genotype genotype = new Genotype(-100, -100);
        hlaTypes.put(parseStruct.getHlaType(), genotype);
      }
    }
    EnumMap<SpecialHla, Boolean> specialHla = ans.getSpecialHla();
    for (HlaSpecialParseStruct special : specialHlaStructs) {
      specialHla.put(special.getHla(),
          ProblemData.parseOneEmpty(read(special.getColumn(), record)));
      // TODO this isn't quite right, we actually don't have data probably.
    }
    return ans;
  }

  private static enum UnosDataHeader {
    personId("id"), relatedPatientId("famid"), arrivalTime("date"), age("age"), race(
        "race"), isDonor("isDonor"), bloodType("bloodType"), center("center"), maxAgeForDonor(
        "maxAgeForDonor"), a1("A1"), a2("A2"), b1("B1"), b2("B2"), dr1("DR1"), dr2(
        "DR2"), bw4("Bw4"), bw6("Bw6"), cw1("Cw1"), cw2("Cw2"), dp1("DP1"), dp2(
        "DP2"), dq1("DQ1"), dq2("DQ2"), dr51("DR51"), dr52("DR52"), dr53("DR53"), antiA(
        "antiA"), antiB("antiB"), antiDR("antiDR"), antiCW("antiCW"), antiDQ(
        "antiDQ"), antiDP("antiDP"), antiBw("antiBw"), antiDR51("antiDR51"), antiDR52(
        "antiDR52"), antiDR53("antiDR53"), antiBw4("antiBw4"), antiBw6(
        "antiBw6"), cPra("cPRA");

    private UnosDataHeader(String defaultName) {
      this.defaultName = defaultName;
    }

    public String getDefaultName() {
      return this.defaultName;
    }

    private String defaultName;

    public static final ImmutableBiMap<UnosDataHeader, String> ALL_DEFAULT_NAMES = createAllDefaultNames();

    private static ImmutableBiMap<UnosDataHeader, String> createAllDefaultNames() {
      ImmutableBiMap.Builder<UnosDataHeader, String> ans = ImmutableBiMap
          .builder();
      for (UnosDataHeader header : UnosDataHeader.values()) {
        ans.put(header, header.defaultName);
      }
      return ans.build();
    }
  }

  private ImmutableBiMap<UnosDataHeader, Integer> getHeaderColumns(
      CSVRecord headerRow) {
    ImmutableBiMap.Builder<UnosDataHeader, Integer> ans = ImmutableBiMap
        .builder();
    for (int i = 0; i < headerRow.size(); i++) {
      String header = headerRow.get(i);
      if (dataHeaderNames.inverse().containsKey(header)) {
        ans.put(dataHeaderNames.inverse().get(header), i);
      }
    }
    ImmutableBiMap<UnosDataHeader, Integer> ansBuilt = ans.build();
    if (ansBuilt.size() != dataHeaderNames.size()) {
      throw new RuntimeException("Expected "
          + dataHeaderNames.size()
          + " headers but found: "
          + ansBuilt.size()
          + ".  Values: "
          + ansBuilt
          + ", Missing: "
          + Sets.difference(EnumSet.allOf(UnosDataHeader.class),
              ansBuilt.keySet()));
    }
    return ansBuilt;
  }

  private static boolean isDonor(String value) {
    return readZeroOneToBoolean(value);
  }

  private static boolean readZeroOneToBoolean(String value) {
    String readVal = value.trim();
    if (readVal.equals("1")) {
      return true;
    }
    if (readVal.equals("0")) {
      return false;
    }
    throw new RuntimeException("Expected 0 or 1, but found: " + readVal);
  }

  private static BloodType parseBloodType(String string) {
    int typeInd = Integer.parseInt(string);
    if (typeInd == 3) {
      return BloodType.O;
    }
    if (typeInd == 2) {
      return BloodType.A;
    }
    if (typeInd == 1) {
      return BloodType.B;
    }
    if (typeInd == 0) {
      return BloodType.AB;
    }
    throw new RuntimeException(
        "Unexpected blood type index (should be in {0,1,2,3}): " + typeInd);
  }

  private static class HlaParseStruct {
    private UnosDataHeader firstColumn;
    private UnosDataHeader secondColumn;
    private UnosDataHeader antibodies;
    public HlaType hlaType;

    public HlaParseStruct(UnosDataHeader firstColumn,
        UnosDataHeader secondColumn, UnosDataHeader antibodies, HlaType hlaType) {
      super();
      this.firstColumn = firstColumn;
      this.secondColumn = secondColumn;
      this.antibodies = antibodies;
      this.hlaType = hlaType;
    }

    public UnosDataHeader getFirstColumn() {
      return firstColumn;
    }

    public UnosDataHeader getSecondColumn() {
      return secondColumn;
    }

    public UnosDataHeader getAntibodies() {
      return antibodies;
    }

    public HlaType getHlaType() {
      return hlaType;
    }

  }

  private static HlaParseStruct[] parseStructs = new HlaParseStruct[] {
      new HlaParseStruct(UnosDataHeader.a1, UnosDataHeader.a2,
          UnosDataHeader.antiA, HlaType.A),
      new HlaParseStruct(UnosDataHeader.b1, UnosDataHeader.b2,
          UnosDataHeader.antiB, HlaType.B),
      new HlaParseStruct(UnosDataHeader.dr1, UnosDataHeader.dr2,
          UnosDataHeader.antiDR, HlaType.DR),

      new HlaParseStruct(UnosDataHeader.cw1, UnosDataHeader.cw2,
          UnosDataHeader.antiCW, HlaType.Cw),
      new HlaParseStruct(UnosDataHeader.dq1, UnosDataHeader.dq2,
          UnosDataHeader.antiDQ, HlaType.DQ),
      new HlaParseStruct(UnosDataHeader.dp1, UnosDataHeader.dp2,
          UnosDataHeader.antiDP, HlaType.DP) };

  private static class HlaSpecialParseStruct {
    private UnosDataHeader column;
    private UnosDataHeader immunity;
    private SpecialHla hla;

    public HlaSpecialParseStruct(UnosDataHeader column,
        UnosDataHeader immunity, SpecialHla hla) {
      this.column = column;
      this.immunity = immunity;
      this.hla = hla;
    }

    public UnosDataHeader getColumn() {
      return column;
    }

    public UnosDataHeader getImmunity() {
      return immunity;
    }

    public SpecialHla getHla() {
      return hla;
    }

  }

  private static HlaSpecialParseStruct[] specialHlaStructs = new HlaSpecialParseStruct[] {
      new HlaSpecialParseStruct(UnosDataHeader.bw4, UnosDataHeader.antiBw4,
          SpecialHla.Bw4),
      new HlaSpecialParseStruct(UnosDataHeader.bw6, UnosDataHeader.antiBw6,
          SpecialHla.Bw6),
      new HlaSpecialParseStruct(UnosDataHeader.dr51, UnosDataHeader.antiDR51,
          SpecialHla.DR51),
      new HlaSpecialParseStruct(UnosDataHeader.dr52, UnosDataHeader.antiDR52,
          SpecialHla.DR52),
      new HlaSpecialParseStruct(UnosDataHeader.dr53, UnosDataHeader.antiDR53,
          SpecialHla.DR53) };

}
