package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.collect.Sets;

import data.Chain.Cluster;
import data.CycleClusterBuilder.TransplantBuilder;
import data.ExchangeUnit.ExchangeUnitType;
import data.MedicalMatch.Incompatability;
import data.Transplant.TransplantStatus;

public class ProblemData {

  private static String dataDir = "data" + File.separator;
  private static DateTimeFormatter dirNameFormat = DateTimeFormat
      .forPattern("yyyyMMdd");
  private static String fileNameSnapshotOld = "bestmatch_export.csv";
  private static String fileNameSnapshot = "bm1-0.csv";
  private static String fileNameHistoric = "bm1-0xplanted.csv";
  private static String fileNameSnapShotRelated = "best_match_pairs.csv";
  private static String fileNameSnapShotPotentialMatchesDirected = "best_match_one_ways_dd.csv";
  private static String fileNameSnapShotPotentialMatchesNonDirected = "best_match_one_ways_ndd.csv";

  public static DateTimeFormatter registeredTime = DateTimeFormat
      .forPattern("MM/dd/yyyy HH:mm");
  public static DateTimeFormatter bornTime = DateTimeFormat
      .forPattern("MM/dd/yyyy");

  private Set<ExchangeUnit> exchangeUnits;
  private Map<Receiver, ExchangeUnit> chips;
  private Map<Receiver, ExchangeUnit> pairedReceivers;
  private Map<Donor, ExchangeUnit> pairedDonors;
  private Map<Donor, ExchangeUnit> altruisticDonors;
  private Set<Hospital> hospitals;
  private Map<Receiver, Set<Donor>> hardBlockedExchanges;
  private Map<Receiver, Set<Donor>> snapshotFeasibleExchanges;
  private MatchingAssignment historicMatchingAssignment;

  private DateTime dataDate;

  private Set<Donor> looseDonors;
  private Set<Receiver> looseReceivers;

  // entries because of april 10: 3,9,19,21,144,150
  // entries because of may 24: 147 156 159 161 164
  private static int[] cycleChainIndexblackList = new int[] { 3, 9, 19, 21,
      144, 147, 150, 156, 159, 161, 164 };

  public ProblemData(String fileName, DateTime testDate) {
    this.dataDate = testDate;
    ProblemDataBuilder dataBuilder = new ProblemDataBuilder();
    readFile(fileName, false, dataBuilder);
    convertBuilderToDataStructures(dataBuilder, false);
    validateDataStuctures(dataBuilder);
  }

  /**
   * assumes format yyyMMdd
   * 
   * @param start
   * @param end
   */
  public ProblemData(String date) {
    this(dirNameFormat.parseDateTime(date));
  }

  public ProblemData(String date, boolean includeHistoric) {
    this(dirNameFormat.parseDateTime(date), includeHistoric);
  }

  public ProblemData(DateTime date) {
    this(date, true);

  }

  /**
   * 
   * @param start
   *          inclusive
   * @param end
   *          inclusive
   */
  public ProblemData(DateTime date, boolean includeHistoric) {
    dataDate = date;
    ProblemDataBuilder dataBuilder = new ProblemDataBuilder();
    String dir = dataDir + dirNameFormat.print(date) + File.separator;
    File dirFile = new File(dir);
    if (dirFile.isDirectory()) {
      if (includeHistoric) {
        String csvHistoric = dir + fileNameHistoric;
        System.err.println("Reading historic data");
        readFile(csvHistoric, true, dataBuilder);
      }
      String csvCurrent = dir + fileNameSnapshot;
      System.err.println("Reading current data");
      readFile(csvCurrent, false, dataBuilder);
      String csvQualifiedFileNameSnapShotRelated = dir
          + fileNameSnapShotRelated;
      System.err.println("Reading relationship current data");
      addRels(csvQualifiedFileNameSnapShotRelated, dataBuilder);
      System.err.println("Reading current feasible match data");
      addSnapshotFeasibleMatches(dir
          + ProblemData.fileNameSnapShotPotentialMatchesDirected, dir
          + ProblemData.fileNameSnapShotPotentialMatchesNonDirected,
          dataBuilder);
    }
    convertBuilderToDataStructures(dataBuilder, true);
    validateDataStuctures(dataBuilder);

  }

  public DateTime getDataDate() {
    return this.dataDate;
  }

  public Set<ExchangeUnit> getExchangeUnits() {
    return exchangeUnits;
  }

  public Map<Receiver, ExchangeUnit> getChips() {
    return chips;
  }

  public Map<Receiver, ExchangeUnit> getPairedReceivers() {
    return pairedReceivers;
  }

  public Map<Donor, ExchangeUnit> getPairedDonors() {
    return pairedDonors;
  }

  public Map<Donor, ExchangeUnit> getAltruisticDonors() {
    return altruisticDonors;
  }

  public Set<Hospital> getHospitals() {
    return hospitals;
  }

  public Map<Receiver, Set<Donor>> getHardBlockedExchanges() {
    return hardBlockedExchanges;
  }

  public MatchingAssignment getHistoricMatchingAssignment() {
    return historicMatchingAssignment;
  }

  public Set<Donor> getLooseDonors() {
    return looseDonors;
  }

  public Set<Receiver> getLooseReceivers() {
    return looseReceivers;
  }

  private int idToInt(String id) {
    return Integer.parseInt(id.substring(1, id.indexOf("_")));
  }

  private void addSnapshotFeasibleMatches(
      String qualifiedFileNameSnapShotPotentialMatchesDirected,
      String qualifiedFileNameSnapShotPotentialMatchesNonDirected,
      ProblemDataBuilder builder) {
    try {
      Map<Integer, String> donorIdNum = new HashMap<Integer, String>();
      Map<Integer, String> receiverIdNum = new HashMap<Integer, String>();
      for (String donorId : builder.getAltruisticOrBridgeDonors().keySet()) {
        donorIdNum.put(Integer.valueOf(idToInt(donorId)), donorId);
      }
      for (String donorId : builder.getPairedDonorsNoBridge().keySet()) {
        donorIdNum.put(Integer.valueOf(idToInt(donorId)), donorId);
      }
      for (String recId : builder.getPairedReceivers().keySet()) {
        receiverIdNum.put(Integer.valueOf(idToInt(recId)), recId);
      }
      for (String recId : builder.getChipReceivers().keySet()) {
        receiverIdNum.put(Integer.valueOf(idToInt(recId)), recId);
      }
      for (String fileName : new String[] {
          qualifiedFileNameSnapShotPotentialMatchesDirected,
          qualifiedFileNameSnapShotPotentialMatchesNonDirected }) {
        CSVParser parser = new CSVParser(new BufferedReader(new FileReader(
            fileName)), CSVFormat.EXCEL);
        List<CSVRecord> records = parser.getRecords();
        for (int i = 1; i < records.size(); i++) {
          int receiver = Integer.parseInt(records.get(i).get(1));
          String receiverS = receiverIdNum.get(Integer.valueOf(receiver));
          if (receiverS == null) {
            System.err.println("Warning: Could not find receiver " + receiver);
            continue;
          }
          int donor = Integer.parseInt(records.get(i).get(0));
          String donorS = donorIdNum.get(Integer.valueOf(donor));
          if (donorS == null) {
            System.err.println("Warning: Could not find donor " + donor);
            continue;
          }
          Set<String> donors;
          if (!builder.getSnapshotFeasibleExchangeIds().containsKey(receiverS)) {
            donors = new HashSet<String>();
            builder.getSnapshotFeasibleExchangeIds().put(receiverS, donors);
          } else {
            donors = builder.getSnapshotFeasibleExchangeIds().get(receiverS);
          }
          donors.add(donorS);
        }
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private void addRels(String qualifiedFileNameSnapShotRelated,
      ProblemDataBuilder builder) {
    try {
      Map<Integer, String> donorIdNum = new HashMap<Integer, String>();
      Map<Integer, String> receiverIdNum = new HashMap<Integer, String>();
      for (String donorId : builder.getAltruisticOrBridgeDonors().keySet()) {
        donorIdNum.put(Integer.valueOf(idToInt(donorId)), donorId);
      }
      for (String donorId : builder.getPairedDonorsNoBridge().keySet()) {
        donorIdNum.put(Integer.valueOf(idToInt(donorId)), donorId);
      }
      for (String recId : builder.getPairedReceivers().keySet()) {
        receiverIdNum.put(Integer.valueOf(idToInt(recId)), recId);
      }
      for (String recId : builder.getChipReceivers().keySet()) {
        receiverIdNum.put(Integer.valueOf(idToInt(recId)), recId);
      }
      CSVParser parser = new CSVParser(new BufferedReader(new FileReader(
          qualifiedFileNameSnapShotRelated)), CSVFormat.EXCEL);
      List<CSVRecord> records = parser.getRecords();
      for (int i = 1; i < records.size(); i++) {
        int receiver = Integer.parseInt(records.get(i).get(0));
        String receiverS = receiverIdNum.get(Integer.valueOf(receiver));
        if (receiverS == null) {
          System.err.println("Warning: Could not find receiver " + receiver);
          continue;
        }
        int donor = Integer.parseInt(records.get(i).get(1));
        String donorS = donorIdNum.get(Integer.valueOf(donor));
        if (donorS == null) {
          System.err.println("Warning: Could not find donor " + donor);
          continue;
        }
        if (builder.getDonorToReceiverId().containsKey(donorS)
            && !builder.getDonorToReceiverId().get(donorS).equals(receiverS)) {
          throw new RuntimeException("For donor " + donorS
              + " wanted to add receiver " + receiverS + " but already found "
              + builder.getDonorToReceiverId().get(donorS));
        } else {
          builder.getDonorToReceiverId().put(donorS, receiverS);
        }
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private static enum PersonType {
    AltruisticOrBridgeDonor, PairedNoBridgeDonor, PairedReceiver, Chip;
  }

  private void readFile(String csvFile, boolean isHistoric,
      ProblemDataBuilder builder) {
    try {
      CSVParser parser = new CSVParser(new BufferedReader(new FileReader(
          csvFile)), CSVFormat.EXCEL);
      List<CSVRecord> records = parser.getRecords();
      if (records.size() == 0) {
        throw new RuntimeException("Error: file had no lines");
      } else {
        EnumMap<ColumnName, Integer> columnIndicies = getColumnIndicies(
            records.get(0), isHistoric ? defaultColumnNamesHistoric
                : defaultColumnNames);
        verify(columnIndicies, defaultMandatoryColumns);
        int i = 1;
        while (!records.get(i).get(0).equals("--YMXX_o_XXMY--")) {
          try {
            CSVRecord record = records.get(i);
            String personId = record.get(
                columnIndicies.get(ColumnName.PERSON_ID)).trim();
            if (isHistoric) {
              builder.getAlreadyTransplanted().add(personId);
            }
            PersonType recordType = parsePersonType(record, columnIndicies,
                isHistoric);
            if (builder.patientAlreadyFound(personId, recordType)) {
              System.err.println("Warning, found patient " + personId
                  + " more than once");
              continue;
            }
            String hospitalId = record.get(
                columnIndicies.get(ColumnName.CENTER_ID)).trim();
            Hospital hospital;
            if (!builder.getHospitals().containsKey(hospitalId)) {
              hospital = new Hospital(hospitalId);
              builder.getHospitals().put(hospitalId, hospital);
            } else {
              hospital = builder.getHospitals().get(hospitalId);
            }
            // System.err.println(record.get(columnIndicies.get(ColumnName.DATE_REGISTERED)).trim());
            DateTime enteredSystem = registeredTime.parseDateTime(record.get(
                columnIndicies.get(ColumnName.DATE_REGISTERED)).trim());
            // boolean active =
            // parseYesNo(record.get(columnIndicies.get(ColumnName.ACTIVE)));
            DateTime born = bornTime.parseDateTime(record.get(
                columnIndicies.get(ColumnName.BIRTH_YEAR)).trim());
            Gender gender = parseGender(record.get(columnIndicies
                .get(ColumnName.GENDER)));
            Race race = parseRace(record.get(columnIndicies
                .get(ColumnName.RACE)));
            // System.err.println(personId);
            BloodType bloodType = parseBloodType(record.get(columnIndicies
                .get(ColumnName.BLOOD_TYPE)));
            if (bloodType == null) {
              if (recordType.equals(PersonType.AltruisticOrBridgeDonor)
                  || recordType.equals(PersonType.PairedNoBridgeDonor)) {
                bloodType = BloodType.AB;
              } else {
                bloodType = BloodType.O;
              }
            }
            Integer heightCm = parseInteger(record.get(columnIndicies
                .get(ColumnName.HEIGHT_CM)));
            Integer weightKg = parseInteger(record.get(columnIndicies
                .get(ColumnName.WEIGHT_KG)));
            TissueType tissueType = parseTissueType(record, columnIndicies);

            if (recordType.equals(PersonType.AltruisticOrBridgeDonor)) {

              parseMatching(builder, record, columnIndicies, personId, true);

              Donor donor = new Donor(born, personId, enteredSystem, gender,
                  race, bloodType, tissueType, heightCm, weightKg, hospital);
              builder.getAltruisticOrBridgeDonors().put(personId, donor);
            } else if (recordType.equals(PersonType.PairedNoBridgeDonor)) {
              parseMatching(builder, record, columnIndicies, personId, true);
              Donor donor = new Donor(born, personId, enteredSystem, gender,
                  race, bloodType, tissueType, heightCm, weightKg, hospital);
              builder.getPairedDonorsNoBridge().put(personId, donor);
            } else if (recordType.equals(PersonType.Chip)
                || recordType.equals(PersonType.PairedReceiver)) {
              // System.err.println(record.toString());
              parseMatching(builder, record, columnIndicies, personId, false);
              TissueTypeSensitivity tissueTypeSensitivity = parseTissueTypeSensitivity(
                  record, columnIndicies);
              Period minDonorAgeYr = Period.years(parseIntOrEmpty(record
                  .get(columnIndicies.get(ColumnName.MIN_DONOR_AGE))));
              Period maxDonorAgeYr = Period.years(parseIntOrEmptyOrZero(
                  record.get(columnIndicies.get(ColumnName.MAX_DONOR_AGE)),
                  defaultMaximumAgeOfDonorInYears));
              int minHlaMatch = Integer.parseInt(record.get(
                  columnIndicies.get(ColumnName.MIN_HLA_MATCH)).trim());
              int minDonorWeightKg = (int) Double.parseDouble(record.get(
                  columnIndicies.get(ColumnName.MIN_DONOR_WEIGHT)).trim());
              boolean acceptsShipped = parseYesNo(record.get(columnIndicies
                  .get(ColumnName.ACCEPT_SHIPPED_KIDNEY)));
              boolean willingToTravel = parseYesNo(record.get(columnIndicies
                  .get(ColumnName.WILLING_TO_TRAVEL)));

              String[] hardBlockedDonors = properBarSplit(record
                  .get(columnIndicies.get(ColumnName.HARD_BLOCKED_DONORS)));

              builder.getHardBlockedIds().put(personId,
                  new HashSet<String>(Arrays.asList(hardBlockedDonors)));
              Receiver receiver = new Receiver(born, personId, enteredSystem,
                  gender, race, bloodType, tissueTypeSensitivity, tissueType,
                  heightCm, weightKg, minDonorAgeYr, maxDonorAgeYr,
                  minHlaMatch, minDonorWeightKg, acceptsShipped,
                  willingToTravel, hospital);

              if (recordType.equals(PersonType.Chip)) {
                builder.getChipReceivers().put(personId, receiver);
              } else {
                builder.getPairedReceivers().put(personId, receiver);
                String[] donors = properBarSplit(record.get(columnIndicies
                    .get(ColumnName.RELATED_DONORS)));
                for (String donorId : donors) {
                  builder.getDonorToReceiverId().put(donorId, personId);
                }

              }
            } else {
              throw new RuntimeException("Type of record is unknown.");
            }
          } catch (BlackListException e) {
            System.err.println("Warning: black listed entry from chain "
                + e.getChainIndex());
          }
          i++;

        }
        i++;
        EnumMap<HospitalColumn, Integer> hospitalColumnIndicies = getHospitalColumnIndicies(
            records.get(i), ProblemData.defaultHosptialColumnNames);
        verifyHospital(hospitalColumnIndicies, defaultMandatoryHospitalColumns);
        i++;
        while (!records.get(i).get(0).trim().equals("--XXMY_o_YMXX--")) {
          CSVRecord record = records.get(i);
          String id = record.get(hospitalColumnIndicies.get(HospitalColumn.ID))
              .trim();
          String city = record.get(
              hospitalColumnIndicies.get(HospitalColumn.CITY)).trim();
          String state = record.get(
              hospitalColumnIndicies.get(HospitalColumn.STATE)).trim();
          if (builder.getHospitals().containsKey(id)) {
            Hospital hospital = builder.getHospitals().get(id);
            hospital.setCity(city);
            hospital.setState(state);
          }
          i++;
        }

      }

    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void convertBuilderToDataStructures(ProblemDataBuilder data,
      boolean doComplicatedNkrRebuild) {
    exchangeUnits = new HashSet<ExchangeUnit>();
    chips = new HashMap<Receiver, ExchangeUnit>();
    pairedReceivers = new HashMap<Receiver, ExchangeUnit>();
    pairedDonors = new HashMap<Donor, ExchangeUnit>();
    altruisticDonors = new HashMap<Donor, ExchangeUnit>();
    hospitals = new HashSet<Hospital>();
    hardBlockedExchanges = new HashMap<Receiver, Set<Donor>>();
    historicMatchingAssignment = new MatchingAssignment();
    looseDonors = new HashSet<Donor>();
    looseReceivers = new HashSet<Receiver>();
    hospitals.addAll(data.getHospitals().values());
    snapshotFeasibleExchanges = new HashMap<Receiver, Set<Donor>>();
    for (String donorId : data.getDonorToReceiverId().keySet()) {
      Donor donor = data.getPairedDonorsNoBridge().get(donorId);
      if (donor == null) {
        String recId = data.getDonorToReceiverId().get(donorId);
        System.err.println("Warning: donor " + donorId
            + " was supposed to be related to receiver " + recId
            + " but did not find  entry for donor, deleting donor.");
      } else {
        String receiverId = data.getDonorToReceiverId().get(donorId);
        Receiver receiver = data.getPairedReceivers().get(receiverId);
        if (receiver == null) {
          System.err.println("Warning: discarding donor: " + donorId
              + " as paired receiver " + receiverId
              + " was missing (possibly a chip)");
          continue;
        }
        ExchangeUnit ex;
        if (!pairedReceivers.containsKey(receiver)) {

          ex = ExchangeUnit.makePaired(receiver, donor);
          this.pairedReceivers.put(receiver, ex);
          // exchangeUnits.add(ex);
        } else {
          ex = pairedReceivers.get(receiver);
          ex.getDonor().add(donor);
        }
        this.pairedDonors.put(donor, ex);
      }
    }
    for (String receiverId : data.pairedReceivers.keySet()) {
      Receiver receiver = data.getPairedReceivers().get(receiverId);

      if (!this.pairedReceivers.containsKey(receiver)) {
        this.pairedReceivers.put(receiver, ExchangeUnit.makePaired(receiver));
      }
    }
    for (String donorId : data.pairedDonorsNoBridge.keySet()) {
      Donor donor = data.pairedDonorsNoBridge.get(donorId);
      if (!pairedDonors.containsKey(donor)) {
        this.looseDonors.add(donor);
        System.err
            .println("Warning: found no related receiver for directed donor:  "
                + donorId);
      }
    }
    if (looseDonors.size() > 0) {
      System.err.println("Warning: found " + looseDonors.size()
          + " directed donors without receivers.");
    }

    for (String chipId : data.getChipReceivers().keySet()) {
      Receiver rec = data.getChipReceivers().get(chipId);
      if (rec == null) {
        throw new RuntimeException("Could not find chip receiver for id: "
            + chipId);
      }
      ExchangeUnit ex = ExchangeUnit.makeChip(rec);
      chips.put(rec, ex);
      // exchangeUnits.add(ex);
    }
    if (doComplicatedNkrRebuild) {
      complicatedNkrRebuild(data);
    } else {
      for (String altruist : data.getAltruisticOrBridgeDonors().keySet()) {
        Donor altruistDonor = data.getAltruisticOrBridgeDonors().get(altruist);
        altruisticDonors.put(altruistDonor,
            ExchangeUnit.makeAltruistic(altruistDonor));
      }
    }
    finishBuildAfterAltruisticDetermined(data);
  }

  public void complicatedNkrRebuild(ProblemDataBuilder data) {
    for (Integer chainCycleIndex : data.getCycleClusterBuilders().keySet()) {
      try {

        Map<Integer, CycleClusterBuilder> chainCycle = data
            .getCycleClusterBuilders().get(chainCycleIndex);

        CycleClusterBuilder firstCycleBuilder = chainCycle.get(Integer
            .valueOf(1));
        if (firstCycleBuilder == null) {
          throw new RuntimeException("Chain/Cycle " + chainCycleIndex
              + " did not have a cluster 1, but had clusters "
              + chainCycle.keySet().toString());
        }
        List<Transplant> firstTransplants = new ArrayList<Transplant>();

        for (int i = 1; i <= firstCycleBuilder.getTransplants().size(); i++) {

          TransplantBuilder transplantBuilder = firstCycleBuilder
              .getTransplants().get(Integer.valueOf(i));
          if (transplantBuilder.getReceiverId() == null
              && (i < firstCycleBuilder.getTransplants().size() || chainCycle
                  .size() > 1)) {

            System.err.println(chainCycle.keySet());
            throw new RuntimeException("Error in chain " + chainCycleIndex
                + " cluster 1 transplant " + i
                + " receiver was null but not final transplant in chain");
          }
          if (transplantBuilder.getDonorId() == null) {
            throw new RuntimeException("Error in chain " + chainCycleIndex
                + " cluster 1 transplant " + i + " donor was null");
          }
          Transplant trans = new Transplant(data.getDonorById(transplantBuilder
              .getDonorId()), transplantBuilder.getReceiverId() == null ? null
              : data.getReceiverById(transplantBuilder.getReceiverId()),
              transplantBuilder.getTransplantStatus(),
              transplantBuilder.getDate());
          firstTransplants.add(trans);

        }

        List<ExchangeUnit> firstExchangeUnits = new ArrayList<ExchangeUnit>();
        boolean isCycle = correctOrdering(firstTransplants, firstExchangeUnits,
            data, null, firstCycleBuilder);
        if (isCycle) {
          if (chainCycle.size() > 1) {
            throw new RuntimeException("Chain cycle " + chainCycleIndex
                + " appeared to be a cycle but had more than one cluster");
          }
          Cycle cycle = new Cycle(chainCycleIndex.intValue(), firstTransplants,
              firstExchangeUnits);
          this.historicMatchingAssignment.getMatchings().add(cycle);
        } else {
          List<Cluster> clusters = new ArrayList<Cluster>();
          // if(firstTransplants.get(firstTransplants.size()-1))
          Cluster firstCluster = new Cluster(firstTransplants,
              firstExchangeUnits);
          clusters.add(firstCluster);
          /*
           * if(firstCycleBuilder.getCycleChainId() == 121 &&
           * firstCycleBuilder.getClusterIndex() == 1){ throw new
           * RuntimeException("hello3\n\n\n\n\n\n\n"); }
           */
          ExchangeUnit bridge = firstExchangeUnits.get(firstExchangeUnits
              .size() - 1);
          for (int i = 2; i <= chainCycle.size(); i++) {
            CycleClusterBuilder iCycleBuilder = chainCycle.get(Integer
                .valueOf(i));
            if (iCycleBuilder == null) {
              throw new RuntimeException("For chain " + chainCycleIndex
                  + " could not find cluster " + i + " only had clusters "
                  + chainCycle.keySet());
            }
            List<Transplant> iTransplants = new ArrayList<Transplant>();

            for (int j = 1; j <= iCycleBuilder.getTransplants().size(); j++) {
              TransplantBuilder transplantBuilder = iCycleBuilder
                  .getTransplants().get(Integer.valueOf(j));
              if (transplantBuilder.getReceiverId() == null
                  && (j < iCycleBuilder.getTransplants().size() || i < chainCycle
                      .size())) {
                throw new RuntimeException("Error in chain " + chainCycleIndex
                    + " cluster " + i + "transplant " + j
                    + " receiver was null but not final transplant in chain");
              }
              Transplant trans = new Transplant(
                  data.getDonorById(transplantBuilder.getDonorId()),
                  transplantBuilder.getReceiverId() == null ? null : data
                      .getReceiverById(transplantBuilder.getReceiverId()),
                  transplantBuilder.getTransplantStatus(),
                  transplantBuilder.getDate());
              iTransplants.add(trans);

            }
            List<ExchangeUnit> iExchangeUnits = new ArrayList<ExchangeUnit>();
            boolean iIsCycle = correctOrdering(iTransplants, iExchangeUnits,
                data, bridge, iCycleBuilder);
            if (iIsCycle) {
              throw new RuntimeException("Found a cycle in cluster " + i
                  + " of chain " + chainCycleIndex);
            }
            Cluster iCluster = new Cluster(iTransplants, iExchangeUnits);
            clusters.add(iCluster);
            ExchangeUnit finalExchange = iExchangeUnits.get(iExchangeUnits
                .size() - 1);
            if (finalExchange == null) {
              throw new RuntimeException("Error in chain " + chainCycleIndex
                  + " cluster " + i + " final exchange null ");
            }
            if (finalExchange.getExchangeUnitType() == ExchangeUnitType.chip) {
              bridge = null;
              if (i != chainCycle.size()) {
                throw new RuntimeException("Error in chain " + chainCycleIndex
                    + " cluster " + i + " ends with a chip but there are  "
                    + chainCycle.size() + " clusters.");
              }
            } else {
              bridge = finalExchange;
            }
          }
          Chain chain = new Chain(chainCycleIndex, clusters);
          this.historicMatchingAssignment.getMatchings().add(chain);

        }
      } catch (RuntimeException e) {
        System.err.println(e);
        System.err.println(Arrays.toString(e.getStackTrace()));
      }
    }

  }

  private void finishBuildAfterAltruisticDetermined(ProblemDataBuilder data) {
    for (String receiverId : data.getHardBlockedIds().keySet()) {
      Receiver receiver = data.getReceiverById(receiverId);
      if (!this.hardBlockedExchanges.containsKey(receiver)) {
        this.hardBlockedExchanges.put(receiver, new HashSet<Donor>());
      }
      Set<Donor> blocked = this.hardBlockedExchanges.get(receiver);
      Set<String> donorIds = data.getHardBlockedIds().get(receiverId);
      for (String donorId : donorIds) {
        if (data.donorExists(donorId)) {
          blocked.add(data.getDonorById(donorId));
        } else {
          // System.err.println("Warning: could not find blocked donor " +
          // donorId);
        }
      }
    }
    for (String receiverId : data.getSnapshotFeasibleExchangeIds().keySet()) {
      Receiver receiver = data.getReceiverById(receiverId);
      Set<Donor> donors = new HashSet<Donor>();
      this.snapshotFeasibleExchanges.put(receiver, donors);
      for (String donor : data.getSnapshotFeasibleExchangeIds().get(receiverId)) {
        donors.add(data.getDonorById(donor));
      }
    }
    for (Receiver rec : this.pairedReceivers.keySet()) {
      if (this.pairedReceivers.get(rec).getDonor().size() == 0) {
        this.looseReceivers.add(rec);
        System.err.println("Warning: found no related donors for receiver "
            + rec.getId());
      }
    }
    if (this.looseReceivers.size() > 0) {
      System.err.println("Warning: found " + looseReceivers.size()
          + " loose receivers, these will not be included.");
      for (Receiver rec : looseReceivers) {
        this.pairedReceivers.remove(rec);
      }
    }
    if (this.looseDonors.size() > 0) {
      System.err.println("Warning: " + this.looseDonors.size()
          + " loose donors remaining, these will not be included.");
    }
    for (Receiver rec : this.pairedReceivers.keySet()) {
      ExchangeUnit unit = this.pairedReceivers.get(rec);
      this.exchangeUnits.add(unit);
    }
    for (Receiver rec : this.chips.keySet()) {
      this.exchangeUnits.add(this.chips.get(rec));
    }
    for (Donor donor : this.altruisticDonors.keySet()) {
      this.exchangeUnits.add(this.altruisticDonors.get(donor));
    }
  }

  /**
   * As a side effect, transplants will be properly reordered and exchange units
   * will be populated as per the constructors of Cycle and Chain.Cluster Also
   * as a side effect, the bridge donors will be populated into the map paired
   * donors and the altruistic donors will be populated into their map. Finally,
   * the exchange units for bridge and altruistic donors, which previously only
   * contained the receiver and potentially unused donors, will now have the the
   * bridge/altruistic donors added.
   * 
   * @param transplants
   *          the actual list of transplants for this cycle/chain as specified
   *          by cycleClusterBuilder
   * @param exchangeUnitsInCluster
   *          an empty list, to be filled
   * @param data
   * @param incomingBridgeUnit
   *          if this is a chain and not the first cluster, pass in the final
   *          exchange unit from the previous cluster, otherwise null
   * @param cycleClusterBuilder
   * @return true if this should be a cycle, false if it should be a chain
   */
  private boolean correctOrdering(List<Transplant> transplants,
      List<ExchangeUnit> exchangeUnitsInCluster, ProblemDataBuilder data,
      ExchangeUnit incomingBridgeUnit, CycleClusterBuilder cycleClusterBuilder) {

    Map<Donor, Transplant> components = new HashMap<Donor, Transplant>();
    List<ExchangeUnit> exchangeUnitsOutOfOrder = new ArrayList<ExchangeUnit>();
    Donor head = null;
    int headIndex = -1;
    for (int i = 0; i < transplants.size(); i++) {
      Transplant trans = transplants.get(i);
      if (components.containsKey(trans.getDonor())) {
        throw new RuntimeException("Cycle/chain "
            + cycleClusterBuilder.getCycleChainId() + " cluster "
            + cycleClusterBuilder.getClusterIndex() + " Donor "
            + trans.getDonor().getId() + " is donating for two transplants");
      }
      components.put(trans.getDonor(), trans);
      boolean matchesIncoming = false;
      if (incomingBridgeUnit != null
          && incomingBridgeUnit.getDonor().contains(trans.getDonor())) {

        matchesIncoming = true;
        if (head == null) {
          head = trans.getDonor();
          headIndex = i;
          exchangeUnitsOutOfOrder.add(incomingBridgeUnit);
        } else {
          throw new RuntimeException(
              "Warning: Cycle/chain "
                  + cycleClusterBuilder.getCycleChainId()
                  + " cluster "
                  + cycleClusterBuilder.getClusterIndex()
                  + "Found two donors matching incoming receiver from previous chain link");

        }
      }
      boolean wasInAltruistic = false;
      if (data.getAltruisticOrBridgeDonors().containsKey(
          trans.getDonor().getId())) {
        if (matchesIncoming) {
          throw new RuntimeException(
              "Cycle/chain "
                  + cycleClusterBuilder.getCycleChainId()
                  + " cluster "
                  + cycleClusterBuilder.getClusterIndex()
                  + " Error: donor "
                  + trans.getDonor().getId()
                  + " was in in altrustic/paired-bridge but also was in paired-no-bridge...");
        }

        if (head == null) {
          wasInAltruistic = true;
          head = trans.getDonor();
          headIndex = i;
          if (incomingBridgeUnit == null) {
            ExchangeUnit ex = ExchangeUnit.makeAltruistic(trans.getDonor());
            this.altruisticDonors.put(trans.getDonor(), ex);
            exchangeUnitsOutOfOrder.add(ex);
          } else {
            incomingBridgeUnit.getDonor().add(trans.getDonor());
            this.pairedDonors.put(head, incomingBridgeUnit);
            // if(!this.pairedReceivers.containsKey(incomingBridgeUnit)){
            // this.pairedReceivers.put(incomingBridgeUnit.getReceiver(),
            // incomingBridgeUnit);//TODO this is a hack it should not be
            // necessary
            // }
            exchangeUnitsOutOfOrder.add(incomingBridgeUnit);
          }
        } else {
          Receiver previousRec = transplants.get(i - 1).getReceiver();
          System.err.println("Warning: Cycle/chain "
              + cycleClusterBuilder.getCycleChainId() + " cluster "
              + cycleClusterBuilder.getClusterIndex()
              + " Found a chain/cycle with multiple heads: " + head.getId()
              + " and " + trans.getDonor().getId() + ", guessing the pair ("
              + trans.getDonor().getId() + ", " + previousRec.getId() + ")");
          this.pairedReceivers.get(previousRec).getDonor()
              .add(trans.getDonor());
          this.pairedDonors.put(trans.getDonor(),
              this.pairedReceivers.get(previousRec));

          data.getAltruisticOrBridgeDonors().remove(trans.getDonor().getId());
        }
      }
      boolean wasPaired = false;
      if (!matchesIncoming && !wasInAltruistic
          && this.pairedDonors.containsKey(trans.getDonor())) {
        wasPaired = true;
        exchangeUnitsOutOfOrder.add(this.pairedDonors.get(trans.getDonor()));
      }
      if (!matchesIncoming && !wasInAltruistic
          && this.looseDonors.contains(trans.getDonor())) {
        if (exchangeUnitsOutOfOrder.size() > 0) {
          wasPaired = true;
          Receiver previousRec = transplants.get(i - 1).getReceiver();
          ExchangeUnit probablyPrevious = this.pairedReceivers.get(previousRec);
          probablyPrevious.getDonor().add(trans.getDonor());
          exchangeUnitsOutOfOrder.add(probablyPrevious);
          System.err.println("Warning: guessing the receiver for loose donor "
              + trans.getDonor().getId() + " is " + previousRec.getId());
          looseDonors.remove(trans.getDonor());
        } else {
          throw new RuntimeException("Consider blacklisting Cycle/chain "
              + cycleClusterBuilder.getCycleChainId() + " cluster "
              + cycleClusterBuilder.getClusterIndex()
              + " could not determine the receiver for donor "
              + trans.getDonor().getId());
        }

      }

      if (!wasPaired && !wasInAltruistic && !matchesIncoming) {
        throw new RuntimeException("Cycle/chain "
            + cycleClusterBuilder.getCycleChainId() + " cluster "
            + cycleClusterBuilder.getClusterIndex() + " Could not find donor: "
            + trans.getDonor().getId());
      }
    }
    boolean isCycle = head == null;
    if (cycleClusterBuilder.getClusterIndex() > 1 && isCycle) {
      Transplant firstTrans = transplants.get(0);
      System.err.println("Warning: Cycle/chain "
          + cycleClusterBuilder.getCycleChainId() + " cluster "
          + cycleClusterBuilder.getClusterIndex()
          + " there was no head, guessing that the head is: "
          + firstTrans.getDonor().getId());
      ExchangeUnit old = this.pairedDonors.get(firstTrans.getDonor());
      if (old != null) {
        System.err
            .println("Warning, guessed donor was previously matched as a "
                + old.getExchangeUnitType()
                + "  to "
                + old.getReceiver()
                + ", deleting this relationship, check data manually to see if this is a mistake");
        old.getDonor().remove(firstTrans.getDonor());
        this.pairedDonors.remove(firstTrans.getDonor());
      }
      incomingBridgeUnit.getDonor().add(firstTrans.getDonor());
      this.pairedDonors.put(firstTrans.getDonor(), incomingBridgeUnit);
      exchangeUnitsOutOfOrder.add(incomingBridgeUnit);
      head = firstTrans.getDonor();
      headIndex = 0;
      isCycle = false;

    }
    if (isCycle) {
      head = transplants.get(0).getDonor();
      headIndex = 0;
    }

    Donor nextDonor = head;
    int nextDonorIndex = headIndex;
    int numSorted = 0;
    int[] correct = new int[transplants.size()];
    boolean deleteLastTransplant = false;
    for (int i = 0; i < transplants.size(); i++) {
      correct[i] = nextDonorIndex;
      numSorted++;
      components.remove(nextDonor);
      Receiver rec = transplants.get(nextDonorIndex).getReceiver();

      if (rec != null) {
        if (rec.getId().equals("R1055_RMC")) {
          System.out.println("found ya");
        }
        ExchangeUnit recUnit;
        if (this.pairedReceivers.containsKey(rec)) {
          recUnit = this.pairedReceivers.get(rec);
        } else if (this.chips.containsKey(rec)) {
          recUnit = this.chips.get(rec);
          if (numSorted != transplants.size()) {
            throw new RuntimeException("Cycle/chain "
                + cycleClusterBuilder.getCycleChainId() + " cluster "
                + cycleClusterBuilder.getClusterIndex()
                + " Found a chip before the end of a chain");
          }
        } else {
          throw new RuntimeException("Cycle/chain "
              + cycleClusterBuilder.getCycleChainId() + " cluster "
              + cycleClusterBuilder.getClusterIndex()
              + " No exchange unit for receiver: " + rec.getId());
        }
        if (i < transplants.size() - 1) {
          Transplant nextTransplant = null;
          for (Donor potentialDonor : recUnit.getDonor()) {
            if (components.containsKey(potentialDonor)) {
              if (nextTransplant != null) {
                throw new RuntimeException("Cycle/chain "
                    + cycleClusterBuilder.getCycleChainId() + " cluster "
                    + cycleClusterBuilder.getClusterIndex()
                    + " Found two related donors for a single receiver.");
              } else {
                nextTransplant = components.get(potentialDonor);
              }
            }
          }
          if (nextTransplant == null) {
            int nextTransplantIndexGuess = nextDonorIndex + 1;
            nextTransplant = transplants.get(nextTransplantIndexGuess);
            System.err.println("Warning: Cycle/chain "
                + cycleClusterBuilder.getCycleChainId() + " cluster "
                + cycleClusterBuilder.getClusterIndex()
                + " Could not find a transplant to follow that of "
                + nextDonor.getId() + ", guessing that transplant of "
                + nextTransplant.getDonor().getId() + " should follow");
            recUnit.getDonor().add(nextTransplant.getDonor());
            if (this.pairedDonors.containsKey(nextTransplant.getDonor())) {
              throw new RuntimeException("Cycle/chain "
                  + cycleClusterBuilder.getCycleChainId() + " cluster "
                  + cycleClusterBuilder.getClusterIndex() + " this is bad");
            }
            if (this.altruisticDonors.containsKey(nextTransplant.getDonor())) {
              throw new RuntimeException("Cycle/chain "
                  + cycleClusterBuilder.getCycleChainId() + " cluster "
                  + cycleClusterBuilder.getClusterIndex()
                  + " this is bad altruistic");
            }
            this.pairedDonors.put(nextTransplant.getDonor(), recUnit);

          }
          nextDonor = nextTransplant.getDonor();
          nextDonorIndex = transplants.indexOf(nextTransplant);
        } else {
          if (isCycle) {
            boolean verifiedCycle = false;
            for (Donor potentialDonor : recUnit.getDonor()) {
              if (head == potentialDonor) {
                verifiedCycle = true;
                break;
              }
            }
            if (!verifiedCycle) {
              throw new RuntimeException(
                  "cycle/chain "
                      + cycleClusterBuilder.getCycleChainId()
                      + " cluster "
                      + cycleClusterBuilder.getClusterIndex()
                      + " Should have been a cycle but did not actually for a cycle");
            }
          }
        }
      } else {
        deleteLastTransplant = true;
      }

    }
    List<Transplant> transplantCopy = new ArrayList<Transplant>(transplants);
    transplants.clear();
    int numToCopy = deleteLastTransplant ? correct.length - 1 : correct.length;
    for (int i = 0; i < numToCopy; i++) {
      if (correct[i] != i) {
        System.err.println("Warning: cycle/chain "
            + cycleClusterBuilder.getCycleChainId() + " cluster "
            + cycleClusterBuilder.getClusterIndex() + " is out of order");
        break;
      }
    }
    for (int i = 0; i < numToCopy; i++) {
      transplants.add(transplantCopy.get(correct[i]));
      exchangeUnitsInCluster.add(exchangeUnitsOutOfOrder.get(correct[i]));
    }

    if (!isCycle) {
      if (transplants.size() > 0) {

        Receiver lastReceiver = transplants.get(transplants.size() - 1)
            .getReceiver();

        ExchangeUnit lastUnit;
        if (this.pairedReceivers.containsKey(lastReceiver)) {
          lastUnit = this.pairedReceivers.get(lastReceiver);
        } else if (this.chips.containsKey(lastReceiver)) {
          lastUnit = this.chips.get(lastReceiver);
        } else {
          throw new RuntimeException(
              "Could not find exchange unit for receiver: "
                  + lastReceiver.getId());
        }
        exchangeUnitsInCluster.add(lastUnit);
      } else if (deleteLastTransplant) {
        exchangeUnitsInCluster.add(incomingBridgeUnit);
      } else {
        throw new RuntimeException("Cycle/chain "
            + cycleClusterBuilder.getCycleChainId() + " cluster "
            + cycleClusterBuilder.getClusterIndex() + " had no transplants");
      }

    }
    return isCycle;

  }

  /*
   * private boolean isCycle(Map<Integer,CycleClusterBuilder> chainCycle,
   * ProblemDataBuilder dataBuilder){ if(!(chainCycle.size() == 1 &&
   * chainCycle.containsKey(Integer.valueOf(1)))){ return false; }
   * CycleClusterBuilder builder = chainCycle.get(Integer.valueOf(1));
   * Map<Integer,TransplantBuilder> transplants = builder.getTransplants();
   * if(transplants.size() < 2 || !transplants.containsKey(Integer.valueOf(1))
   * || !transplants.containsKey(Integer.valueOf(transplants.size()))){ return
   * false; } TransplantBuilder finalTransplant =
   * transplants.get(Integer.valueOf(transplants.size()));
   * if(finalTransplant.getReceiverId() == null){ return false; } Donor head =
   * dataBuilder.getDonorById(transplants.get(Integer.valueOf(1)).getDonorId());
   * Receiver tail =
   * dataBuilder.getReceiverById(finalTransplant.getReceiverId());
   * if(!this.pairedDonors.containsKey(head) ||
   * !this.pairedReceivers.containsKey(tail)){ return false; }
   * if(!this.pairedDonors.get(head).equals(this.pairedReceivers.get(tail))){
   * return false; } return true; }
   */

  private void validateDataStuctures(ProblemDataBuilder builder) {
    for (AbstractMatching matching : this.historicMatchingAssignment
        .getMatchings()) {
      matching.checkWellFormed();
    }
    int numPredictedMatches = 0;
    int numDisagreements = 0;
    for (Receiver rec : this.snapshotFeasibleExchanges.keySet()) {

      for (Donor donor : this.snapshotFeasibleExchanges.get(rec)) {
        numPredictedMatches++;
        EnumSet<Incompatability> reasonsIncompatible = MedicalMatch.instance
            .match(donor, rec, this.dataDate);
        if (reasonsIncompatible.size() > 0) {
          numDisagreements++;
          System.err.println("Data said that " + donor + " and " + rec
              + " should be compatible but found: " + reasonsIncompatible);
        }
      }
    }
    System.err.println("Disagreed on " + numDisagreements + " out of "
        + numPredictedMatches + " predicted matches");

    for (AbstractMatching matching : this.historicMatchingAssignment
        .getMatchings()) {
      matching.checkWellFormed();
    }
    Set<Donor> allDonorsNotHistoric = new HashSet<Donor>();
    Set<Receiver> allReceiversNotHistoric = new HashSet<Receiver>();
    for (Donor donor : this.pairedDonors.keySet()) {
      if (!builder.alreadyTransplanted.contains(donor.getId())) {
        allDonorsNotHistoric.add(donor);
      }
    }
    for (Donor donor : this.altruisticDonors.keySet()) {
      if (!builder.alreadyTransplanted.contains(donor.getId())) {
        allDonorsNotHistoric.add(donor);
      }
    }
    for (Donor donor : this.looseDonors) {
      if (!builder.alreadyTransplanted.contains(donor.getId())) {
        // allDonorsNotHistoric.add(donor);
      }
    }

    for (Receiver rec : this.pairedReceivers.keySet()) {
      if (!builder.alreadyTransplanted.contains(rec.getId())) {
        allReceiversNotHistoric.add(rec);
      }
    }
    for (Receiver rec : this.chips.keySet()) {

      if (!builder.alreadyTransplanted.contains(rec.getId())) {
        // allReceiversNotHistoric.add(rec);
      }
    }
    for (Receiver rec : this.looseReceivers) {
      if (!builder.alreadyTransplanted.contains(rec.getId())) {
        // allReceiversNotHistoric.add(rec);
      }
    }
    int numExtraEdgesForUs = 0;
    Set<Donor> matchedDonors = this.historicMatchingAssignment
        .getMatchedDonors();
    Set<Receiver> matchedReceivers = this.historicMatchingAssignment
        .getMatchedReceivers();
    for (Receiver rec : allReceiversNotHistoric) {
      if (!matchedReceivers.contains(rec)) {
        Set<Donor> hardBlocked = this.getHardBlockedExchanges().get(rec);
        Set<Donor> predictedCompatible = this.snapshotFeasibleExchanges
            .get(rec);
        if (predictedCompatible == null) {
          predictedCompatible = new HashSet<Donor>();
        }
        for (Donor donor : allDonorsNotHistoric) {
          if (!matchedDonors.contains(donor)) {
            EnumSet<Incompatability> reasonsIncompatible = MedicalMatch.instance
                .match(donor, rec, this.dataDate);
            if (!hardBlocked.contains(donor) && reasonsIncompatible.size() == 0
                && !predictedCompatible.contains(donor)) {
              numExtraEdgesForUs++;
              System.err.println("Data said that " + donor + " and " + rec
                  + " should not be compatible but found they were.");
            }
          }
        }
      }
    }
    System.out.println("We predicted " + numExtraEdgesForUs
        + " organ donations as valid that data said were not.");

  }

  private static String[] unmatched = new String[] { "", "", "1", "1" };

  private static boolean intContains(int[] data, int value) {
    for (int datum : data) {
      if (datum == value) {
        return true;
      }
    }
    return false;
  }

  private static class BlackListException extends Exception {

    /**
		 * 
		 */
    private static final long serialVersionUID = 3267867444124253964L;
    private int chainIndex;

    public BlackListException(int chainIndex) {
      this.chainIndex = chainIndex;
    }

    public int getChainIndex() {
      return chainIndex;
    }
  }

  private static void parseMatching(ProblemDataBuilder builder,
      CSVRecord record, EnumMap<ColumnName, Integer> columnIndicies, String id,
      boolean isDonor) throws BlackListException {
    String[] chainClusterPositionStatus = properBarSplit(record
        .get(columnIndicies.get(ColumnName.CHAIN_CLUSTER_POSITION_STATUS)));
    Map<Integer, Map<Integer, CycleClusterBuilder>> cycleClusterBuilders = builder
        .getCycleClusterBuilders();
    if (!Arrays.equals(unmatched, chainClusterPositionStatus)) {
      int[] chainClusterPositionStatusInt = convertToInts(chainClusterPositionStatus);
      int chain = chainClusterPositionStatusInt[0];
      if (intContains(ProblemData.cycleChainIndexblackList, chain)) {
        throw new BlackListException(chain);
      }
      int cluster = chainClusterPositionStatusInt[1];
      int position = chainClusterPositionStatusInt[2];
      TransplantStatus status = TransplantStatus
          .getStatusByCode(chainClusterPositionStatusInt[3]);
      if (!cycleClusterBuilders.containsKey(chain)) {
        cycleClusterBuilders.put(chain,
            new HashMap<Integer, CycleClusterBuilder>());
      }
      Map<Integer, CycleClusterBuilder> chainCycle = cycleClusterBuilders
          .get(chain);
      if (!chainCycle.containsKey(cluster)) {
        chainCycle.put(cluster, new CycleClusterBuilder(chain, cluster));
      }
      CycleClusterBuilder clusterOb = chainCycle.get(cluster);
      TransplantBuilder transplant;
      if (!clusterOb.getTransplants().containsKey(position)) {
        transplant = new TransplantBuilder();
        transplant.setTransplantStatus(status);
        clusterOb.getTransplants().put(position, transplant);
      } else {
        transplant = clusterOb.getTransplants().get(position);
        if (transplant.getTransplantStatus() != status) {
          TransplantStatus maximum = TransplantStatus.maximum(status,
              transplant.getTransplantStatus());
          System.err
              .println("Warning: Conflicting status for transplant chain/cycle: "
                  + chain
                  + ", cluster "
                  + cluster
                  + ", position: "
                  + position
                  + " status one: "
                  + status
                  + ", status two: "
                  + transplant.getTransplantStatus()
                  + ", using status: "
                  + maximum);
          transplant.setTransplantStatus(maximum);
        }
      }
      if (isDonor) {
        if (transplant.getDonorId() != null) {
          throw new RuntimeException("Found two donors "
              + transplant.getDonorId() + ", " + id + " for same transplant");
        }
        transplant.setDonorId(id);
      } else {
        if (transplant.getReceiverId() != null) {
          throw new RuntimeException("Found two receivers "
              + transplant.getReceiverId() + ", " + id + " for same transplant");
        }
        transplant.setReceiverId(id);
        if (transplant.getTransplantStatus() == TransplantStatus.TRANSPLANTED) {
          transplant.setDate(bornTime.parseDateTime(record.get(columnIndicies
              .get(ColumnName.TRANSPLANTED_DATE))));
        }

      }

    }
  }

  public static String[] properBarSplit(String s) {
    String trimmed = s.trim();
    if (trimmed.equals("")) {
      return new String[0];
    } else {
      return trimmed.split("\\|");
    }
  }

  private static TissueTypeSensitivity parseTissueTypeSensitivity(
      CSVRecord record, EnumMap<ColumnName, Integer> columnIndicies) {
    TissueTypeSensitivity ans = new TissueTypeSensitivity();
    EnumMap<HlaType, int[]> hlaAntibodies = ans.getAntibodies();
    for (HlaParseStruct parseStruct : parseStructs) {
      String listAnti = record.get(columnIndicies.get(parseStruct.antibodies))
          .trim();
      String[] anitArray = properBarSplit(listAnti);
      hlaAntibodies.put(parseStruct.hlaType, convertToInts(anitArray));
    }
    EnumMap<SpecialHla, Boolean> specialAntibodies = ans
        .getAvoidsSpecialAntibodies();
    for (HlaSpecialParseStruct special : specialHlaStructs) {
      if (special.hla == SpecialHla.Bw4 || special.hla == SpecialHla.Bw6) {
        String bwImmune = record.get(columnIndicies.get(special.immunity))
            .trim();
        if (bwImmune.equals("4")) {
          specialAntibodies.put(SpecialHla.Bw4, true);
          specialAntibodies.put(SpecialHla.Bw6, false);
        } else if (bwImmune.equals("6")) {
          specialAntibodies.put(SpecialHla.Bw4, false);
          specialAntibodies.put(SpecialHla.Bw6, true);
        } else if (bwImmune.isEmpty()) {
          specialAntibodies.put(SpecialHla.Bw4, false);
          specialAntibodies.put(SpecialHla.Bw6, false);
        } else {
          throw new RuntimeException("Expected 4, 6 or blank, but found: "
              + bwImmune);
        }
      } else if (parseOneEmpty(record.get(columnIndicies.get(special.immunity)))) {
        specialAntibodies.put(special.hla, true);
      } else {
        // TODO this isn't quite right, we actually don't have data probably.
        specialAntibodies.put(special.hla, false);
      }
    }

    return ans;
  }

  public static int[] convertToInts(String[] strings) {
    int[] ans = new int[strings.length];
    for (int i = 0; i < strings.length; i++) {
      try {
        ans[i] = Integer.parseInt(strings[i]);
      } catch (NumberFormatException e) {
        System.err.println("Number format error on: " + strings[i]);
        // TODO this whole catch clause is a hack, delete it when data is fixed
        ans[i] = -50;
      }
    }
    return ans;
  }

  private static class HlaParseStruct {
    public ColumnName firstColumn;
    public ColumnName secondColumn;
    public ColumnName antibodies;
    public HlaType hlaType;

    public HlaParseStruct(ColumnName firstColumn, ColumnName secondColumn,
        ColumnName antibodies, HlaType hlaType) {
      super();
      this.firstColumn = firstColumn;
      this.secondColumn = secondColumn;
      this.antibodies = antibodies;
      this.hlaType = hlaType;
    }
  }

  private static HlaParseStruct[] parseStructs = new HlaParseStruct[] {
      new HlaParseStruct(ColumnName.HLA_A1, ColumnName.HLA_A2,
          ColumnName.AVOIDS_A, HlaType.A),
      new HlaParseStruct(ColumnName.HLA_B1, ColumnName.HLA_B2,
          ColumnName.AVOIDS_B, HlaType.B),
      new HlaParseStruct(ColumnName.HLA_DR1, ColumnName.HLA_DR2,
          ColumnName.AVOIDS_DR, HlaType.DR),
      new HlaParseStruct(ColumnName.HLA_Cw1, ColumnName.HLA_Cw2,
          ColumnName.AVOIDS_CW, HlaType.Cw),
      new HlaParseStruct(ColumnName.HLA_DQ1, ColumnName.HLA_DQ2,
          ColumnName.AVOIDS_DQ, HlaType.DQ),
      new HlaParseStruct(ColumnName.HLA_DP1, ColumnName.HLA_DP2,
          ColumnName.AVOIDS_DP, HlaType.DP) };

  private static class HlaSpecialParseStruct {
    public ColumnName column;
    public ColumnName immunity;
    public SpecialHla hla;

    public HlaSpecialParseStruct(ColumnName column, ColumnName immunity,
        SpecialHla hla) {
      this.column = column;
      this.immunity = immunity;
      this.hla = hla;
    }
  }

  private static HlaSpecialParseStruct[] specialHlaStructs = new HlaSpecialParseStruct[] {
      new HlaSpecialParseStruct(ColumnName.HLA_Bw1, ColumnName.AVOIDS_BW,
          SpecialHla.Bw4),
      new HlaSpecialParseStruct(ColumnName.HLA_Bw2, ColumnName.AVOIDS_BW,
          SpecialHla.Bw6),
      new HlaSpecialParseStruct(ColumnName.HLA_DR_51, ColumnName.AVOIDS_DR_51,
          SpecialHla.DR51),
      new HlaSpecialParseStruct(ColumnName.HLA_DR_52, ColumnName.AVOIDS_DR_52,
          SpecialHla.DR52),
      new HlaSpecialParseStruct(ColumnName.HLA_DR_53, ColumnName.AVOIDS_DR_53,
          SpecialHla.DR53) };

  private static TissueType parseTissueType(CSVRecord record,
      EnumMap<ColumnName, Integer> columnIndicies) {
    TissueType ans = new TissueType();
    EnumMap<HlaType, Genotype> hlaTypes = ans.getHlaTypes();

    for (HlaParseStruct parseStruct : parseStructs) {
      String firstColumn = record.get(
          columnIndicies.get(parseStruct.firstColumn)).trim();
      if (!firstColumn.equals("")) {
        int hla1 = Integer.parseInt(firstColumn);
        int hla2 = Integer.parseInt(record.get(
            columnIndicies.get(parseStruct.secondColumn)).trim());
        Genotype genotype = new Genotype(hla1, hla2 == -1 ? hla1 : hla2);
        hlaTypes.put(parseStruct.hlaType, genotype);
      }
    }
    EnumMap<SpecialHla, Boolean> specialHla = ans.getSpecialHla();
    {
      String firstColumn = record.get(columnIndicies.get(ColumnName.HLA_Bw1))
          .trim();
      String secondColumn = record.get(columnIndicies.get(ColumnName.HLA_Bw2))
          .trim();
      Set<SpecialHla> found = Sets.newHashSet();
      if (!firstColumn.isEmpty()) {
        int value = Integer.parseInt(firstColumn);
        if (value == 4) {
          found.add(SpecialHla.Bw4);
        }
        if (value == 6) {
          found.add(SpecialHla.Bw6);
        }
      }
      if (!secondColumn.isEmpty()) {
        int value = Integer.parseInt(secondColumn);
        if (value == 4) {
          found.add(SpecialHla.Bw4);
        }
        if (value == 6) {
          found.add(SpecialHla.Bw6);
        }
      }
      specialHla.put(SpecialHla.Bw4, found.contains(SpecialHla.Bw4));
      specialHla.put(SpecialHla.Bw6, found.contains(SpecialHla.Bw6));
    }
    for (HlaSpecialParseStruct special : specialHlaStructs) {
      if (special.hla != SpecialHla.Bw4 && special.hla != SpecialHla.Bw6) {
        if (parseOneEmpty(record.get(columnIndicies.get(special.column)))) {
          specialHla.put(special.hla, true);
        } else {
          // TODO this isn't quite right, we actually don't have data probably.
          specialHla.put(special.hla, false);
        }
      }
    }
    return ans;
  }

  public static boolean parseOneEmpty(String value) {
    String trimmed = value.trim();
    if (trimmed.equals("1")) {
      return true;
    } else if (trimmed.equals("")) {
      return false;
    } else {
      throw new RuntimeException(
          "Expected either \"1\" or empty string, found: \"" + value + "\"");
    }
  }

  private static boolean parseOneZero(String value) {
    String trimmed = value.trim();
    if (trimmed.equals("1")) {
      return true;
    } else if (trimmed.equals("0")) {
      return false;
    } else {
      throw new RuntimeException("Was not 1 or 0 was: " + trimmed);
    }
  }

  private static boolean parseYesNo(String value) {
    String trimmed = value.trim();
    if (trimmed.equals("Yes")) {
      return true;
    } else if (trimmed.equals("No")) {
      return false;
    } else {
      throw new RuntimeException("Expected yes or no, found: " + trimmed);
    }
  }

  private static Integer parseInteger(String string) {
    String trimmed = string.trim();
    if (trimmed.equals("")) {
      return null;
    } else {
      return Integer.parseInt(trimmed);
    }
  }

  private static int parseIntOrEmpty(String string) {
    String trimmed = string.trim();
    if (trimmed.equals("")) {
      return 0;
    } else {
      return Integer.parseInt(trimmed);
    }
  }

  private static int defaultMaximumAgeOfDonorInYears = 500;

  private static int parseIntOrEmptyOrZero(String string, int defaultValue) {
    String trimmed = string.trim();
    if (trimmed.equals("")) {
      return defaultValue;
    } else {
      int ans = Integer.parseInt(trimmed);
      if (ans == 0) {
        return defaultValue;
      } else {
        return ans;
      }
    }
  }

  private static Gender parseGender(String genderValue) {
    String trimmed = genderValue.trim();
    if (trimmed.equals("M")) {
      return Gender.MALE;
    } else if (trimmed.equals("F")) {
      return Gender.FEMALE;
    } else if (trimmed.equals("")) {
      return Gender.UNKNOWN;
    } else {
      throw new RuntimeException("Unrecognized Gender: " + genderValue);
    }

  }

  private static BloodType parseBloodType(String bloodType) {
    String trimmed = bloodType.trim();
    if (trimmed.equals("A")) {
      return BloodType.A;
    } else if (trimmed.equals("B")) {
      return BloodType.B;
    } else if (trimmed.equals("AB")) {
      return BloodType.AB;
    } else if (trimmed.equals("O")) {
      return BloodType.O;
    } else {
      System.err.println("Warning: Unrecognized blood type for patient: "
          + bloodType);
      return null;
    }
  }

  private static PersonType parsePersonType(CSVRecord record,
      EnumMap<ColumnName, Integer> columnIndicies, boolean isHistoric) {
    String recordType = record.get(columnIndicies.get(ColumnName.TYPE)).trim();
    if (recordType.equals("Donor Non Directed")) {
      return PersonType.AltruisticOrBridgeDonor;
    } else if (recordType.equals("Donor Incompatible")) {
      return PersonType.PairedNoBridgeDonor;
    } else if (recordType.equals("Recipient")) {
      String chipString = record.get(columnIndicies.get(ColumnName.CHIP))
          .trim();
      boolean chip;
      if (isHistoric) {
        chip = parseOneZero(chipString);

      } else {
        chip = parseYesNo(chipString);
      }
      if (chip) {
        return PersonType.Chip;
      } else {
        return PersonType.PairedReceiver;
      }
    } else {
      throw new RuntimeException("Unrecognized Record type: " + recordType);
    }
  }

  private static Race parseRace(String raceValue) {
    String trimmed = raceValue.trim();
    if (trimmed.equals("Caucasian")) {
      return Race.WHITE;
    } else if (trimmed.equals("Black")) {
      return Race.BLACK;
    } else if (trimmed.equals("Asian")) {
      return Race.ASIAN;
    } else if (trimmed.equals("Latino")) {
      return Race.HISPANIC;
    } else if (trimmed.equals("Not Disclosed")) {
      return Race.NOT_DISCLOSED;
    } else if (trimmed.equals("Other")) {
      return Race.OTHER;
    } else if (trimmed.equals("")) {
      return Race.UNKNOWN;
    } else {
      throw new RuntimeException("Unrecognized Gender: " + raceValue);
    }

  }

  private static enum HospitalColumn {
    ID, CITY, STATE
  }

  private static EnumSet<HospitalColumn> defaultMandatoryHospitalColumns;
  static {
    defaultMandatoryHospitalColumns = EnumSet.allOf(HospitalColumn.class);
  }

  private static Map<String, HospitalColumn> defaultHosptialColumnNames;
  static {
    defaultHosptialColumnNames = new HashMap<String, HospitalColumn>();
    defaultHosptialColumnNames.put("Center Alias/Code", HospitalColumn.ID);
    defaultHosptialColumnNames.put("City", HospitalColumn.CITY);
    defaultHosptialColumnNames.put("State", HospitalColumn.STATE);
  }

  public static enum ColumnName {
    CENTER_ID, PERSON_ID, DATE_REGISTERED, ACTIVE, TYPE, RELATED_DONORS, BIRTH_YEAR, GENDER, RACE, BLOOD_TYPE, HEIGHT_CM, HEIGHT_FT, HEIGHT_IN, WEIGHT_LBS, WEIGHT_KG, URINE_PROTEIN, CREATINE, BLOOD_PRESSURE, EBV, CMV, HLA_A1, HLA_A2, HLA_B1, HLA_B2, HLA_DR1, HLA_DR2, AVOIDS_A, AVOIDS_B, AVOIDS_DR, MIN_DONOR_AGE, MAX_DONOR_AGE, MIN_HLA_MATCH, MIN_CREATINE, SPEED_COMPATIBILITY, HLA_Bw1, HLA_Bw2, HLA_Cw1, HLA_Cw2, HLA_DQ1, HLA_DQ2, AVOIDS_BW, AVOIDS_CW, AVOIDS_DQ, CENTER_PRA, REGISTRY_PRA, RECIPIENT_TRANSPLANT_CENTER, CENTER_IF_UNLISTED, ACCEPT_SHIPPED_KIDNEY, WILLING_TO_TRAVEL, REASON_KIDNEY_FAIL, MEDICAL_ISSUES, DONOR_TRAVEL_PREF, DONOR_TRAVEL_CENTERS_IF_RESTRICTED, DONOR_TRAVEL_CENTERS_IF_RESTRICTED_AND_UNLISTED, DONOR_NEEDS_AID, DONOR_TRAVEL_NOTES, HARD_BLOCKED_DONORS, CHAIN_CLUSTER_POSITION_STATUS, HLA_DP1, HLA_DP2, AVOIDS_DP, HLA_DR_51, DR3_2_DONOR_ANTIGEN, AVOIDS_DR_51, HLA_DR_52, DR4_2_DONOR_ANTIGEN, AVOIDS_DR_52, HLA_DR_53, DR5_2_DONOR_ANTIGEN, AVOIDS_DR_53, STATE, CENTER_STATE, CHAIN_CLUSTER_ASSIGNMENT, DONOR_WORKED_UP, CHAIN_STATUS, DONOR_A_SUBTYPE, RECIPIENT_NON_A1_TITER, PRA, RECIPIENT_INSURANCE, SUBSCRIBER_IF_NOT_PATIENT, GLOBAL_CASE_RATE, DATE_INSURANCE_LAST_UPDATED, PRECERT_PHONE, PROFESSIONAL_CASE_RATE, BENEFIT_PHONE, CASE_MANAGER_NAME, CASE_MANAGER_PHONE, MIN_DONOR_WEIGHT, CHIP, TRANSPLANTED_DATE;

  }

  public static Map<String, ColumnName> defaultColumnNames;
  static {
    defaultColumnNames = new HashMap<String, ColumnName>();
    defaultColumnNames.put("Center", ColumnName.CENTER_ID);
    defaultColumnNames.put("ID", ColumnName.PERSON_ID);
    defaultColumnNames.put("Registered", ColumnName.DATE_REGISTERED);
    defaultColumnNames.put("Active", ColumnName.ACTIVE);
    defaultColumnNames.put("Type", ColumnName.TYPE);
    defaultColumnNames.put("Related Donors", ColumnName.RELATED_DONORS);
    defaultColumnNames.put("Birth Year", ColumnName.BIRTH_YEAR);
    defaultColumnNames.put("Gender", ColumnName.GENDER);
    defaultColumnNames.put("Race", ColumnName.RACE);
    defaultColumnNames.put("Blood Type", ColumnName.BLOOD_TYPE);
    defaultColumnNames.put("Height (cm)", ColumnName.HEIGHT_CM);
    defaultColumnNames.put("Height (feet)", ColumnName.HEIGHT_FT);
    defaultColumnNames.put("Height (inches)", ColumnName.HEIGHT_IN);
    defaultColumnNames.put("Weight (lbs)", ColumnName.WEIGHT_LBS);
    defaultColumnNames.put("Weight (kilos)", ColumnName.WEIGHT_KG);
    defaultColumnNames.put("24h Urine Protein", ColumnName.URINE_PROTEIN);
    defaultColumnNames.put("Creatinine Clearance", ColumnName.CREATINE);
    defaultColumnNames.put("Blood Pressure", ColumnName.BLOOD_PRESSURE);
    defaultColumnNames.put("EBV", ColumnName.EBV);
    defaultColumnNames.put("CMV", ColumnName.CMV);
    defaultColumnNames.put("HLA A1", ColumnName.HLA_A1);
    defaultColumnNames.put("HLA A2", ColumnName.HLA_A2);
    defaultColumnNames.put("HLA B1", ColumnName.HLA_B1);
    defaultColumnNames.put("HLA B2", ColumnName.HLA_B2);
    defaultColumnNames.put("HLA DR1", ColumnName.HLA_DR1);
    defaultColumnNames.put("HLA DR2", ColumnName.HLA_DR2);
    defaultColumnNames.put("Avoids A", ColumnName.AVOIDS_A);
    defaultColumnNames.put("Avoids B", ColumnName.AVOIDS_B);
    defaultColumnNames.put("Avoids DR", ColumnName.AVOIDS_DR);
    defaultColumnNames.put("Min Donor Age", ColumnName.MIN_DONOR_AGE);
    defaultColumnNames.put("Max Donor Age", ColumnName.MAX_DONOR_AGE);
    defaultColumnNames
        .put("Minimum HLA Match Points", ColumnName.MIN_HLA_MATCH);
    defaultColumnNames.put("Min Creatinine Clearance", ColumnName.MIN_CREATINE);
    defaultColumnNames.put("Speed or Compatibility",
        ColumnName.SPEED_COMPATIBILITY);
    defaultColumnNames.put("HLA Bw1", ColumnName.HLA_Bw1);
    defaultColumnNames.put("HLA Bw2", ColumnName.HLA_Bw2);
    defaultColumnNames.put("HLA Cw1", ColumnName.HLA_Cw1);
    defaultColumnNames.put("HLA Cw2", ColumnName.HLA_Cw2);
    defaultColumnNames.put("HLA DQ1", ColumnName.HLA_DQ1);
    defaultColumnNames.put("HLA DQ2", ColumnName.HLA_DQ2);
    defaultColumnNames.put("Avoids Bw", ColumnName.AVOIDS_BW);
    defaultColumnNames.put("Avoids Cw", ColumnName.AVOIDS_CW);
    defaultColumnNames.put("Avoids DQ", ColumnName.AVOIDS_DQ);
    defaultColumnNames.put("Center PRA", ColumnName.CENTER_PRA);
    defaultColumnNames.put("Registry PRA", ColumnName.REGISTRY_PRA);
    defaultColumnNames.put("Recp Transplant Center",
        ColumnName.RECIPIENT_TRANSPLANT_CENTER);
    defaultColumnNames.put("Center (if unlisted)",
        ColumnName.CENTER_IF_UNLISTED);
    defaultColumnNames.put("Accept Shipped Kidney",
        ColumnName.ACCEPT_SHIPPED_KIDNEY);
    defaultColumnNames.put("R Will Travel", ColumnName.WILLING_TO_TRAVEL);
    defaultColumnNames.put("Reason for Kidnay Failure",
        ColumnName.REASON_KIDNEY_FAIL);
    defaultColumnNames.put("Notable Medical Issues", ColumnName.MEDICAL_ISSUES);
    defaultColumnNames.put("Donor Travel Preference",
        ColumnName.DONOR_TRAVEL_PREF);
    defaultColumnNames.put("Donor Travel Centers (if restricted)",
        ColumnName.DONOR_TRAVEL_CENTERS_IF_RESTRICTED);
    defaultColumnNames.put("Donor Travel Centers (if restricted and unlisted)",
        ColumnName.DONOR_TRAVEL_CENTERS_IF_RESTRICTED_AND_UNLISTED);
    defaultColumnNames.put("Donor Needs Aid", ColumnName.DONOR_NEEDS_AID);
    defaultColumnNames.put("Additional Donor Travel Notes",
        ColumnName.DONOR_TRAVEL_NOTES);
    defaultColumnNames.put("Hard Blocked Donors",
        ColumnName.HARD_BLOCKED_DONORS);
    defaultColumnNames.put("Chain|Cluster|Position|Status",
        ColumnName.CHAIN_CLUSTER_POSITION_STATUS);
    defaultColumnNames.put("HLA DP1", ColumnName.HLA_DP1);
    defaultColumnNames.put("HLA DP2", ColumnName.HLA_DP2);
    defaultColumnNames.put("Avoids DP", ColumnName.AVOIDS_DP);
    defaultColumnNames.put("HLA DR-51", ColumnName.HLA_DR_51);
    defaultColumnNames.put("DR3-2 Donor Antigen",
        ColumnName.DR3_2_DONOR_ANTIGEN);
    defaultColumnNames.put("Avoids DR-51", ColumnName.AVOIDS_DR_51);
    defaultColumnNames.put("HLA DR-52", ColumnName.HLA_DR_52);
    defaultColumnNames.put("DR4-2 Donor Antigen",
        ColumnName.DR4_2_DONOR_ANTIGEN);
    defaultColumnNames.put("Avoids DR-52", ColumnName.AVOIDS_DR_52);
    defaultColumnNames.put("HLA DR-53", ColumnName.HLA_DR_53);
    defaultColumnNames.put("DR5-2 Donor Antigen",
        ColumnName.DR5_2_DONOR_ANTIGEN);
    defaultColumnNames.put("Avoids DR-53", ColumnName.AVOIDS_DR_53);
    defaultColumnNames.put("State", ColumnName.STATE);
    defaultColumnNames.put("Center State", ColumnName.CENTER_STATE);
    defaultColumnNames.put("Chain/Cluster Assignments",
        ColumnName.CHAIN_CLUSTER_ASSIGNMENT);
    defaultColumnNames.put("Donor Worked Up", ColumnName.DONOR_WORKED_UP);
    defaultColumnNames.put("Chain Status", ColumnName.CHAIN_STATUS);
    defaultColumnNames.put("Donor A Subtype", ColumnName.DONOR_A_SUBTYPE);
    defaultColumnNames.put("Recipient Non-A1 Titer",
        ColumnName.RECIPIENT_NON_A1_TITER);
    defaultColumnNames.put("PRA", ColumnName.PRA);
    defaultColumnNames.put("Recipient Insurance",
        ColumnName.RECIPIENT_INSURANCE);
    defaultColumnNames.put("Subscriber if not patient",
        ColumnName.SUBSCRIBER_IF_NOT_PATIENT);
    defaultColumnNames.put("Global case rate", ColumnName.GLOBAL_CASE_RATE);
    defaultColumnNames.put("Date Insurance Last Updated",
        ColumnName.DATE_INSURANCE_LAST_UPDATED);
    defaultColumnNames.put("Precert phone", ColumnName.PRECERT_PHONE);
    defaultColumnNames.put("Professional case rate",
        ColumnName.PROFESSIONAL_CASE_RATE);
    defaultColumnNames.put("Benefit phone", ColumnName.BENEFIT_PHONE);
    defaultColumnNames.put("Case manager name", ColumnName.CASE_MANAGER_NAME);
    defaultColumnNames.put("Case manager phone", ColumnName.CASE_MANAGER_PHONE);
    defaultColumnNames.put("Minimum Donor Weight", ColumnName.MIN_DONOR_WEIGHT);
    defaultColumnNames.put("CHIP", ColumnName.CHIP);

  }

  private static Map<String, ColumnName> defaultColumnNamesHistoric;
  public static Map<ColumnName, String> defaultColumnNamesHistoricReversed;
  public static Map<ColumnName, Integer> defaultColumnNamesHistoricToIndex;
  static {
    defaultColumnNamesHistoric = new LinkedHashMap<String, ColumnName>();// It
                                                                         // is
                                                                         // important
                                                                         // that
                                                                         // everything
                                                                         // stays
                                                                         // in
                                                                         // order
                                                                         // and
                                                                         // that
                                                                         // this
                                                                         // remains
                                                                         // a
                                                                         // linked
                                                                         // hash
                                                                         // map.
    defaultColumnNamesHistoricReversed = new HashMap<ColumnName, String>();
    defaultColumnNamesHistoric.put("Center", ColumnName.CENTER_ID);
    defaultColumnNamesHistoric.put("ID", ColumnName.PERSON_ID);
    defaultColumnNamesHistoric.put("Registered", ColumnName.DATE_REGISTERED);
    defaultColumnNamesHistoric.put("Active", ColumnName.ACTIVE);
    defaultColumnNamesHistoric.put("Type", ColumnName.TYPE);
    defaultColumnNamesHistoric.put("Related Donors", ColumnName.RELATED_DONORS);
    defaultColumnNamesHistoric.put("Birthdate", ColumnName.BIRTH_YEAR);
    defaultColumnNamesHistoric.put("Gender", ColumnName.GENDER);
    defaultColumnNamesHistoric.put("Race", ColumnName.RACE);
    defaultColumnNamesHistoric.put("Blood Type", ColumnName.BLOOD_TYPE);
    defaultColumnNamesHistoric.put("Height (cm)", ColumnName.HEIGHT_CM);
    defaultColumnNamesHistoric.put("Height (feet)", ColumnName.HEIGHT_FT);
    defaultColumnNamesHistoric.put("Height (inches)", ColumnName.HEIGHT_IN);
    defaultColumnNamesHistoric.put("Weight (lbs)", ColumnName.WEIGHT_LBS);
    defaultColumnNamesHistoric.put("Weight (kilos)", ColumnName.WEIGHT_KG);
    defaultColumnNamesHistoric.put("24h Urine Protein",
        ColumnName.URINE_PROTEIN);
    defaultColumnNamesHistoric.put("Creatinine Clearance", ColumnName.CREATINE);
    defaultColumnNamesHistoric.put("Blood Pressure", ColumnName.BLOOD_PRESSURE);
    defaultColumnNamesHistoric.put("EBV", ColumnName.EBV);
    defaultColumnNamesHistoric.put("CMV", ColumnName.CMV);
    defaultColumnNamesHistoric.put("HLA A1", ColumnName.HLA_A1);
    defaultColumnNamesHistoric.put("HLA A2", ColumnName.HLA_A2);
    defaultColumnNamesHistoric.put("HLA B1", ColumnName.HLA_B1);
    defaultColumnNamesHistoric.put("HLA B2", ColumnName.HLA_B2);
    defaultColumnNamesHistoric.put("HLA DR1", ColumnName.HLA_DR1);
    defaultColumnNamesHistoric.put("HLA DR2", ColumnName.HLA_DR2);
    defaultColumnNamesHistoric.put("A Antibodies", ColumnName.AVOIDS_A);
    defaultColumnNamesHistoric.put("B Antibodies", ColumnName.AVOIDS_B);
    defaultColumnNamesHistoric.put("DR Antibodies", ColumnName.AVOIDS_DR);
    defaultColumnNamesHistoric.put("Min Donor Age", ColumnName.MIN_DONOR_AGE);
    defaultColumnNamesHistoric.put("Max Donor Age", ColumnName.MAX_DONOR_AGE);
    defaultColumnNamesHistoric
        .put("Min Match Points", ColumnName.MIN_HLA_MATCH);
    defaultColumnNamesHistoric.put("Min Creatinine Clearance",
        ColumnName.MIN_CREATINE);
    defaultColumnNamesHistoric.put("Speed or Compatibility",
        ColumnName.SPEED_COMPATIBILITY);
    defaultColumnNamesHistoric.put("Bw1 Donor Antigen", ColumnName.HLA_Bw1);
    defaultColumnNamesHistoric.put("Bw2 Donor Antigen", ColumnName.HLA_Bw2);
    defaultColumnNamesHistoric.put("Cw1 Donor Antigen", ColumnName.HLA_Cw1);
    defaultColumnNamesHistoric.put("Cw2 Donor Antigen", ColumnName.HLA_Cw2);
    defaultColumnNamesHistoric.put("DQ1 Donor Antigen", ColumnName.HLA_DQ1);
    defaultColumnNamesHistoric.put("DQ2 Donor Antigen", ColumnName.HLA_DQ2);
    defaultColumnNamesHistoric.put("Bw Antibodies", ColumnName.AVOIDS_BW);
    defaultColumnNamesHistoric.put("Cw Antibodies", ColumnName.AVOIDS_CW);
    defaultColumnNamesHistoric.put("DQ Antibodies", ColumnName.AVOIDS_DQ);
    defaultColumnNamesHistoric.put("Center PRA", ColumnName.CENTER_PRA);
    defaultColumnNamesHistoric.put("Registry PRA", ColumnName.REGISTRY_PRA);
    defaultColumnNamesHistoric.put("Recp Transplant Center",
        ColumnName.RECIPIENT_TRANSPLANT_CENTER);
    defaultColumnNamesHistoric.put("Center (if unlisted)",
        ColumnName.CENTER_IF_UNLISTED);
    defaultColumnNamesHistoric.put("Shipped Kidney Okay",
        ColumnName.ACCEPT_SHIPPED_KIDNEY);
    defaultColumnNamesHistoric.put("Recp Will Transfer",
        ColumnName.WILLING_TO_TRAVEL);
    defaultColumnNamesHistoric.put("Reason for Kidnay Failure",
        ColumnName.REASON_KIDNEY_FAIL);
    defaultColumnNamesHistoric.put("Notable Medical Issues",
        ColumnName.MEDICAL_ISSUES);
    defaultColumnNamesHistoric.put("Donor Travel Preference",
        ColumnName.DONOR_TRAVEL_PREF);
    defaultColumnNamesHistoric.put("Donor Travel Centers (if restricted)",
        ColumnName.DONOR_TRAVEL_CENTERS_IF_RESTRICTED);
    defaultColumnNamesHistoric.put(
        "Donor Travel Centers (if restricted and unlisted)",
        ColumnName.DONOR_TRAVEL_CENTERS_IF_RESTRICTED_AND_UNLISTED);
    defaultColumnNamesHistoric.put("Donor Needs Aid",
        ColumnName.DONOR_NEEDS_AID);
    defaultColumnNamesHistoric.put("Additional Donor Travel Notes",
        ColumnName.DONOR_TRAVEL_NOTES);
    defaultColumnNamesHistoric.put("Failed Crossmatch",
        ColumnName.HARD_BLOCKED_DONORS);
    defaultColumnNamesHistoric.put("Cross Match in Progress",
        ColumnName.CHAIN_CLUSTER_POSITION_STATUS);
    defaultColumnNamesHistoric.put("DP1 Donor Antigen", ColumnName.HLA_DP1);
    defaultColumnNamesHistoric.put("DP2 Donor Antigen", ColumnName.HLA_DP2);
    defaultColumnNamesHistoric.put("DP Antibodies", ColumnName.AVOIDS_DP);
    defaultColumnNamesHistoric.put("DR3-1 Donor Antigen", ColumnName.HLA_DR_51);
    defaultColumnNamesHistoric.put("DR3-2 Donor Antigen",
        ColumnName.DR3_2_DONOR_ANTIGEN);
    defaultColumnNamesHistoric.put("DR3 Antibodies", ColumnName.AVOIDS_DR_51);
    defaultColumnNamesHistoric.put("DR4-1 Donor Antigen", ColumnName.HLA_DR_52);
    defaultColumnNamesHistoric.put("DR4-2 Donor Antigen",
        ColumnName.DR4_2_DONOR_ANTIGEN);
    defaultColumnNamesHistoric.put("DR4 Antibodies", ColumnName.AVOIDS_DR_52);
    defaultColumnNamesHistoric.put("DR5-1 Donor Antigen", ColumnName.HLA_DR_53);
    defaultColumnNamesHistoric.put("DR5-2 Donor Antigen",
        ColumnName.DR5_2_DONOR_ANTIGEN);
    defaultColumnNamesHistoric.put("DR5 Antibodies", ColumnName.AVOIDS_DR_53);
    defaultColumnNamesHistoric.put("State", ColumnName.STATE);
    defaultColumnNamesHistoric.put("Center State", ColumnName.CENTER_STATE);
    defaultColumnNamesHistoric.put("Chain/Cluster Assignments",
        ColumnName.CHAIN_CLUSTER_ASSIGNMENT);
    // defaultColumnNamesHistoric.put("Donor Worked Up",
    // ColumnName.DONOR_WORKED_UP);
    // Fastpool?
    defaultColumnNamesHistoric.put("Chain Status", ColumnName.CHAIN_STATUS);
    defaultColumnNamesHistoric.put("A1", ColumnName.DONOR_A_SUBTYPE);
    defaultColumnNamesHistoric
        .put("Anti-A1", ColumnName.RECIPIENT_NON_A1_TITER);
    defaultColumnNamesHistoric.put("PRA", ColumnName.PRA);
    // new chain status
    defaultColumnNamesHistoric.put("Transplanted Date",
        ColumnName.TRANSPLANTED_DATE);
    // Kidney Shipped
    // CIT
    // Graft Survival
    // swap type
    // admit creatinine
    // admit creatinine date
    // 1 week creatinine
    // 1 week creatinine date
    // 1 month creatinine
    // 1 month creatinine date
    // 6 month creatinine
    // 6 month creatinine date
    // 1 year creatinine
    // 1 year creatinine date
    // Desensitized
    // unos id
    // unos list date
    // pre transplant pra
    // peak pra
    // dialysis start date
    // donated by
    // insurance

    defaultColumnNamesHistoric.put("Insurance", ColumnName.RECIPIENT_INSURANCE);
    defaultColumnNamesHistoric.put("Min. Donor Weight",
        ColumnName.MIN_DONOR_WEIGHT);
    // worked up center
    // shipping method
    defaultColumnNamesHistoric.put("IsInCHIP", ColumnName.CHIP);
    for (String key : defaultColumnNamesHistoric.keySet()) {
      defaultColumnNamesHistoricReversed.put(
          defaultColumnNamesHistoric.get(key), key);
    }
    defaultColumnNamesHistoricToIndex = new HashMap<ColumnName, Integer>();
    int i = 0;
    for (String key : defaultColumnNamesHistoric.keySet()) {
      defaultColumnNamesHistoricToIndex.put(
          defaultColumnNamesHistoric.get(key), Integer.valueOf(i++));
    }

  }

  private static EnumSet<ColumnName> defaultMandatoryColumns;
  static {
    defaultMandatoryColumns = EnumSet.of(
        ColumnName.CENTER_ID,
        ColumnName.PERSON_ID,
        ColumnName.DATE_REGISTERED,
        ColumnName.ACTIVE,
        ColumnName.TYPE,
        // ColumnName.RELATED_DONORS,
        ColumnName.BIRTH_YEAR,
        ColumnName.GENDER,
        ColumnName.RACE,
        ColumnName.BLOOD_TYPE,
        ColumnName.HEIGHT_CM,
        // ColumnName.HEIGHT_FT,
        // ColumnName.HEIGHT_IN,
        // ColumnName.WEIGHT_LBS,
        ColumnName.WEIGHT_KG,
        // ColumnName.URINE_PROTEIN,
        // ColumnName.CREATINE,
        ColumnName.BLOOD_PRESSURE,
        // ColumnName.EBV,
        // ColumnName.CMV,
        ColumnName.HLA_A1,
        ColumnName.HLA_A2,
        ColumnName.HLA_B1,
        ColumnName.HLA_B2,
        ColumnName.HLA_DR1,
        ColumnName.HLA_DR2,
        ColumnName.AVOIDS_A,
        ColumnName.AVOIDS_B,
        ColumnName.AVOIDS_DR,
        ColumnName.MIN_DONOR_AGE,
        ColumnName.MAX_DONOR_AGE,
        ColumnName.MIN_HLA_MATCH,
        // ColumnName.MIN_CREATINE,
        // ColumnName.SPEED_COMPATIBILITY,
        ColumnName.HLA_Bw1,
        ColumnName.HLA_Bw2,
        ColumnName.HLA_Cw1,
        ColumnName.HLA_Cw2,
        ColumnName.HLA_DQ1,
        ColumnName.HLA_DQ2,
        ColumnName.AVOIDS_BW,
        ColumnName.AVOIDS_CW,
        ColumnName.AVOIDS_DQ,
        // ColumnName.CENTER_PRA,
        // ColumnName.REGISTRY_PRA,
        ColumnName.RECIPIENT_TRANSPLANT_CENTER,
        ColumnName.CENTER_IF_UNLISTED,
        ColumnName.ACCEPT_SHIPPED_KIDNEY,
        ColumnName.WILLING_TO_TRAVEL,
        // ColumnName.REASON_KIDNEY_FAIL,
        // ColumnName.MEDICAL_ISSUES,
        // ColumnName.DONOR_TRAVEL_PREF,
        // ColumnName.DONOR_TRAVEL_CENTERS_IF_RESTRICTED,
        // ColumnName.DONOR_TRAVEL_CENTERS_IF_RESTRICTED_AND_UNLISTED,
        // ColumnName.DONOR_NEEDS_AID,
        // ColumnName.DONOR_TRAVEL_NOTES,
        ColumnName.HARD_BLOCKED_DONORS,
        ColumnName.CHAIN_CLUSTER_POSITION_STATUS, ColumnName.HLA_DP1,
        ColumnName.HLA_DP2, ColumnName.AVOIDS_DP, ColumnName.HLA_DR_51,
        // ColumnName.DR3_2_DONOR_ANTIGEN,
        ColumnName.AVOIDS_DR_51, ColumnName.HLA_DR_52,
        // ColumnName.DR4_2_DONOR_ANTIGEN,
        ColumnName.AVOIDS_DR_52, ColumnName.HLA_DR_53,
        // ColumnName.DR5_2_DONOR_ANTIGEN,
        ColumnName.AVOIDS_DR_53,
        // ColumnName.STATE,
        // ColumnName.CENTER_STATE,
        // ColumnName.CHAIN_CLUSTER_ASSIGNMENT,
        // ColumnName.DONOR_WORKED_UP,
        // ColumnName.CHAIN_STATUS,
        // ColumnName.DONOR_A_SUBTYPE,
        // ColumnName.RECIPIENT_NON_A1_TITER,
        // ColumnName.PRA,
        // ColumnName.RECIPIENT_INSURANCE,
        // ColumnName.SUBSCRIBER_IF_NOT_PATIENT,
        // ColumnName.GLOBAL_CASE_RATE,
        // ColumnName.DATE_INSURANCE_LAST_UPDATED,
        // ColumnName.PRECERT_PHONE,
        // ColumnName.PROFESSIONAL_CASE_RATE,
        // ColumnName.BENEFIT_PHONE,
        // ColumnName.CASE_MANAGER_NAME,
        // ColumnName.CASE_MANAGER_PHONE,
        ColumnName.MIN_DONOR_WEIGHT, ColumnName.CHIP);
  }

  private static void verify(EnumMap<ColumnName, Integer> columnNames,
      EnumSet<ColumnName> mandatoryColumns) {
    for (ColumnName name : mandatoryColumns) {
      if (!columnNames.containsKey(name)) {
        throw new RuntimeException("Could not find column: " + name);
      }
    }
  }

  private static void verifyHospital(
      EnumMap<HospitalColumn, Integer> columnNames,
      EnumSet<HospitalColumn> mandatoryColumns) {
    for (HospitalColumn name : mandatoryColumns) {
      if (!columnNames.containsKey(name)) {
        throw new RuntimeException("Could not find column: " + name);
      }
    }
  }

  private static EnumMap<ColumnName, Integer> getColumnIndicies(
      CSVRecord firstRow, Map<String, ColumnName> columnNames) {
    EnumMap<ColumnName, Integer> ans = new EnumMap<ColumnName, Integer>(
        ColumnName.class);
    for (int i = 0; i < firstRow.size(); i++) {
      String val = firstRow.get(i).trim();
      ColumnName column = columnNames.get(val);
      if (column == null) {
        System.err.println("Unknown column: " + val);
        // throw new RuntimeException("Unknown column: " + val);
      } else if (ans.containsKey(column)) {
        throw new RuntimeException("Repeated column: " + val);
      } else {
        ans.put(column, i);
      }
    }
    return ans;
  }

  private static EnumMap<HospitalColumn, Integer> getHospitalColumnIndicies(
      CSVRecord firstRow, Map<String, HospitalColumn> columnNames) {
    EnumMap<HospitalColumn, Integer> ans = new EnumMap<HospitalColumn, Integer>(
        HospitalColumn.class);
    for (int i = 0; i < firstRow.size(); i++) {
      String val = firstRow.get(i).trim();
      HospitalColumn column = columnNames.get(val);
      if (column == null) {
        System.err.println("Unknown column: " + val);
        // throw new RuntimeException("Unknown column: " + val);
      } else if (ans.containsKey(column)) {
        throw new RuntimeException("Repeated column: " + val);
      } else {
        ans.put(column, i);
      }
    }
    return ans;
  }

  /**
   * The data is read in three steps. First, all the strings are read and
   * objects for the donors and receivers are made, but the relationships
   * between donors and receivers are organized as data structures on the String
   * ids. The role of ProblemDataBuilder is to organize these Strings. Then the
   * next step is convert these strings into data structures that directly
   * reference our newly created objects. Finally we validate to make sure that
   * our newly created objects are consistent.
   * 
   * @author ross
   * 
   */
  private static class ProblemDataBuilder {
    private Map<String, Receiver> chipReceivers;
    private Map<String, Receiver> pairedReceivers;
    private Map<String, Donor> pairedDonorsNoBridge;
    private Map<String, Donor> altruisticOrBridgeDonors;
    private Map<String, Hospital> hospitals;
    private Map<String, String> donorToReceiverId;
    private Map<String, Set<String>> hardBlockedIds;// receiver to donors
    private Map<Integer, Map<Integer, CycleClusterBuilder>> cycleClusterBuilders;
    private Set<String> alreadyTransplanted;
    private Map<String, Set<String>> snapshotFeasibleExchangeIds;// receiver to
                                                                 // donors

    public ProblemDataBuilder() {
      chipReceivers = new HashMap<String, Receiver>();
      pairedReceivers = new HashMap<String, Receiver>();
      pairedDonorsNoBridge = new HashMap<String, Donor>();
      altruisticOrBridgeDonors = new HashMap<String, Donor>();

      alreadyTransplanted = new HashSet<String>();

      donorToReceiverId = new HashMap<String, String>();
      hardBlockedIds = new HashMap<String, Set<String>>();
      snapshotFeasibleExchangeIds = new HashMap<String, Set<String>>();
      cycleClusterBuilders = new HashMap<Integer, Map<Integer, CycleClusterBuilder>>();
      hospitals = new HashMap<String, Hospital>();
    }

    public Map<String, Receiver> getChipReceivers() {
      return chipReceivers;
    }

    public Map<String, Receiver> getPairedReceivers() {
      return pairedReceivers;
    }

    public Map<String, Donor> getPairedDonorsNoBridge() {
      return pairedDonorsNoBridge;
    }

    public Map<String, Donor> getAltruisticOrBridgeDonors() {
      return altruisticOrBridgeDonors;
    }

    public Map<String, Hospital> getHospitals() {
      return hospitals;
    }

    public Map<String, String> getDonorToReceiverId() {
      return donorToReceiverId;
    }

    public Map<String, Set<String>> getHardBlockedIds() {
      return hardBlockedIds;
    }

    public Map<Integer, Map<Integer, CycleClusterBuilder>> getCycleClusterBuilders() {
      return cycleClusterBuilders;
    }

    public Set<String> getAlreadyTransplanted() {
      return alreadyTransplanted;
    }

    public Map<String, Set<String>> getSnapshotFeasibleExchangeIds() {
      return snapshotFeasibleExchangeIds;
    }

    public boolean donorExists(String donorId) {
      return this.altruisticOrBridgeDonors.containsKey(donorId)
          || this.pairedDonorsNoBridge.containsKey(donorId);
    }

    public Donor getDonorById(String donorId) {
      if (this.altruisticOrBridgeDonors.containsKey(donorId)) {
        return this.altruisticOrBridgeDonors.get(donorId);
      } else if (this.pairedDonorsNoBridge.containsKey(donorId)) {
        return this.pairedDonorsNoBridge.get(donorId);
      } else {
        throw new RuntimeException("No donor by id: " + donorId);
      }
    }

    public Receiver getReceiverById(String receiverId) {
      if (this.pairedReceivers.containsKey(receiverId)) {
        return this.pairedReceivers.get(receiverId);
      } else if (this.chipReceivers.containsKey(receiverId)) {
        return this.chipReceivers.get(receiverId);
      } else {

        throw new RuntimeException("No receiver by id: " + receiverId);
      }
    }

    private boolean patientAlreadyFound(String personId, PersonType recordType) {
      if (recordType.equals(PersonType.AltruisticOrBridgeDonor)) {
        return this.altruisticOrBridgeDonors.containsKey(personId);
      } else if (recordType.equals(PersonType.PairedReceiver)) {
        return this.pairedReceivers.containsKey(personId);
      } else if (recordType.equals(PersonType.Chip)) {
        return this.chipReceivers.containsKey(personId);
      } else if (recordType.equals(PersonType.PairedNoBridgeDonor)) {
        return this.pairedDonorsNoBridge.containsKey(personId);
      } else {
        throw new RuntimeException("Type of record is unknown.");
      }
    }

  }

}
