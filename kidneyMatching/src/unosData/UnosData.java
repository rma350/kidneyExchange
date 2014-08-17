package unosData;

import java.util.Map;
import java.util.Set;

import kepLib.KepProblemData;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;
import multiPeriod.TimeInstant;
import multiPeriod.TimeWriter;

import org.joda.time.DateTime;
import org.joda.time.Days;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.minWaitingTime.MinWaitingTimeProblemData;
import graphUtil.GraphUtil;

public class UnosData {

  private ImmutableSet<UnosExchangeUnit> altruisticDonors;
  private ImmutableSet<UnosExchangeUnit> pairs;
  private DirectedSparseMultigraph<UnosExchangeUnit, UnosDonorEdge> feasibleTransplants;
  private ImmutableBiMap<String, UnosExchangeUnit> nodeIds;
  private ImmutableBiMap<String, UnosDonorEdge> edgeIds;

  public KepProblemData<UnosExchangeUnit, UnosDonorEdge> exportKepProblemData() {
    return new KepProblemData<UnosExchangeUnit, UnosDonorEdge>(
        feasibleTransplants, altruisticDonors, pairs,
        ImmutableSet.<UnosExchangeUnit> of());
  }

  public MinWaitingTimeProblemData<UnosExchangeUnit> exportMinWaitingTimeProblemData() {
    Map<UnosExchangeUnit, DateTime> arrivalTimes = Maps.newHashMap();
    UnosExchangeUnit first = !this.pairs.isEmpty() ? this.pairs.iterator()
        .next() : this.altruisticDonors.iterator().next();
    DateTime min = first.getPatient().getArrivalDate();
    DateTime max = min;
    for (UnosExchangeUnit altruist : this.altruisticDonors) {
      DateTime arrivalTime = altruist.getDonors().get(0).getArrivalDate();
      arrivalTimes.put(altruist, arrivalTime);
      if (min.compareTo(arrivalTime) > 0) {
        min = arrivalTime;
      }
      if (max.compareTo(arrivalTime) < 0) {
        max = arrivalTime;
      }
    }
    for (UnosExchangeUnit paired : pairs) {
      DateTime arrivalTime = paired.getPatient().getArrivalDate();
      arrivalTimes.put(paired, arrivalTime);
      if (min.compareTo(arrivalTime) > 0) {
        min = arrivalTime;
      }
      if (max.compareTo(arrivalTime) < 0) {
        max = arrivalTime;
      }
    }
    System.out.println("Min date: " + min);
    System.out.println("Max date: " + max);
    ImmutableMap.Builder<UnosExchangeUnit, Integer> normalizedTime = ImmutableMap
        .builder();
    for (Map.Entry<UnosExchangeUnit, DateTime> entry : arrivalTimes.entrySet()) {
      int ellpasedDays = Days.daysBetween(min.withTimeAtStartOfDay(),
          entry.getValue().withTimeAtStartOfDay()).getDays();
      normalizedTime.put(entry.getKey(), ellpasedDays);
    }
    int normalizedMax = Days.daysBetween(min.withTimeAtStartOfDay(),
        max.withTimeAtStartOfDay()).getDays() + 1;
    System.out.println("Normalized max: " + normalizedMax);
    return new MinWaitingTimeProblemData<UnosExchangeUnit>(
        normalizedTime.build(), (double) normalizedMax);
  }

  private ImmutableMap<UnosExchangeUnit, TimeInstant<Double>> buildNodeArrivalTimesFromWaingTimeProblemDataData(
      MinWaitingTimeProblemData<UnosExchangeUnit> minWaitingTime) {
    ImmutableMap.Builder<UnosExchangeUnit, TimeInstant<Double>> nodeArrivalTimes = ImmutableMap
        .builder();
    for (Map.Entry<UnosExchangeUnit, ? extends Number> entry : minWaitingTime
        .getNodeArrivalTime().entrySet()) {
      nodeArrivalTimes.put(entry.getKey(), new TimeInstant<Double>(entry
          .getValue().doubleValue()));
    }
    return nodeArrivalTimes.build();
  }

  public MultiPeriodCyclePackingInputs<UnosExchangeUnit, UnosDonorEdge, Double> exportMultiPeriodCyclePackingInputs() {
    MinWaitingTimeProblemData<UnosExchangeUnit> minWaitingTime = this
        .exportMinWaitingTimeProblemData();
    ImmutableMap<UnosExchangeUnit, TimeInstant<Double>> nodeArrivalTimes = buildNodeArrivalTimesFromWaingTimeProblemDataData(minWaitingTime);
    ImmutableMap<UnosDonorEdge, TimeInstant<Double>> edgeArrivalTimes = GraphUtil
        .inferEdgeArrivalTimes(nodeArrivalTimes, this.feasibleTransplants);
    return new MultiPeriodCyclePackingInputs<UnosExchangeUnit, UnosDonorEdge, Double>(
        exportKepProblemData(), new TimeInstant<Double>(0.0),
        new TimeInstant<Double>(minWaitingTime.getTerminationTime()),
        nodeArrivalTimes, edgeArrivalTimes,
        TimeWriter.DoubleTimeWriter.INSTANCE);
  }

  public ImmutableSet<UnosExchangeUnit> getAltruisticDonors() {
    return altruisticDonors;
  }

  public ImmutableSet<UnosExchangeUnit> getPairs() {
    return pairs;
  }

  public DirectedSparseMultigraph<UnosExchangeUnit, UnosDonorEdge> getFeasibleTransplants() {
    return feasibleTransplants;
  }

  public ImmutableBiMap<String, UnosExchangeUnit> getNodeIds() {
    return nodeIds;
  }

  public ImmutableBiMap<String, UnosDonorEdge> getEdgeIds() {
    return edgeIds;
  }

  public UnosData(
      ImmutableSet<UnosExchangeUnit> altruisticDonors,
      ImmutableSet<UnosExchangeUnit> pairs,
      DirectedSparseMultigraph<UnosExchangeUnit, UnosDonorEdge> feasibleTransplants) {
    super();
    this.altruisticDonors = altruisticDonors;
    this.pairs = pairs;
    this.feasibleTransplants = feasibleTransplants;
    ImmutableBiMap.Builder<String, UnosExchangeUnit> nodeIdBuilder = ImmutableBiMap
        .builder();
    for (UnosExchangeUnit alt : altruisticDonors) {
      nodeIdBuilder.put("a" + alt.getDonors().get(0).getId(), alt);
    }
    for (UnosExchangeUnit paired : pairs) {
      nodeIdBuilder.put("p" + paired.getPatient().getId(), paired);
    }
    this.nodeIds = nodeIdBuilder.build();
    ImmutableBiMap.Builder<String, UnosDonorEdge> edgeIdBuilder = ImmutableBiMap
        .builder();
    for (UnosDonorEdge edge : feasibleTransplants.getEdges()) {
      String id = "e" + edge.getDonor().getId() + "-"
          + feasibleTransplants.getDest(edge).getPatient().getId();
      edgeIdBuilder.put(id, edge);
    }
    this.edgeIds = edgeIdBuilder.build();
  }

  public static UnosData readUnosData(String file, boolean allowSelfLoops,
      boolean printWarnings) {

    UnosParseData parseData = new UnosParseData(file, printWarnings);
    // validate(parseData);

    ImmutableSet.Builder<UnosExchangeUnit> altruistsBuilder = ImmutableSet
        .builder();
    for (UnosDonor donor : parseData.getAltruisticDonors()) {
      altruistsBuilder.add(UnosExchangeUnit.makeAltruistic(donor));
    }
    ImmutableSet<UnosExchangeUnit> altruists = altruistsBuilder.build();
    Multimap<UnosPatient, UnosDonor> patientDonorMap = HashMultimap.create();
    for (Map.Entry<UnosDonor, Long> donorEntry : parseData
        .getDonorToPatientId().entrySet()) {
      patientDonorMap.put(
          parseData.getPatientIdToPatient().get(donorEntry.getValue()),
          donorEntry.getKey());
    }

    ImmutableSet.Builder<UnosExchangeUnit> pairsBuilder = ImmutableSet
        .builder();
    for (UnosPatient patient : parseData.getPatientIdToPatient().values()) {
      if (patientDonorMap.containsKey(patient)) {
        pairsBuilder.add(UnosExchangeUnit.makePaired(patient,
            patientDonorMap.get(patient)));
      } else {
        if (printWarnings) {
          System.err.println("warning: no donors for patient "
              + patient.getId());
        }
      }
    }

    ImmutableSet<UnosExchangeUnit> pairs = pairsBuilder.build();
    DirectedSparseMultigraph<UnosExchangeUnit, UnosDonorEdge> feasibleTransplants = new DirectedSparseMultigraph<UnosExchangeUnit, UnosDonorEdge>();
    for (UnosExchangeUnit unit : Sets.union(altruists, pairs)) {
      feasibleTransplants.addVertex(unit);
    }
    for (UnosExchangeUnit start : Sets.union(altruists, pairs)) {
      for (UnosDonor donor : start.getDonors()) {
        for (UnosExchangeUnit target : pairs) {
          if (allowSelfLoops || start != target) {
            UnosPatient patient = target.getPatient();
            if (UnosMedicalMatch.INSTANCE.match(donor, patient).isEmpty()) {
              feasibleTransplants.addEdge(new UnosDonorEdge(donor), start,
                  target);
            }
          }
        }
      }
    }
    return new UnosData(altruists, pairs, feasibleTransplants);

  }

  // Ids are now checked when ParseData is being constructed for uniqueness,
  // this is no longer necessary
  private static void validate(UnosParseData parseData) {
    Set<Long> usedIds = Sets.newHashSet();
    Set<UnosDonor> altToDelete = Sets.newHashSet();
    for (UnosDonor donor : parseData.getAltruisticDonors()) {
      boolean isUnique = usedIds.add(donor.getId());
      if (!isUnique) {
        System.err.println("Multiple individuals with id: " + donor.getId());
        altToDelete.add(donor);
      }
    }
    parseData.getAltruisticDonors().removeAll(altToDelete);

    Set<UnosDonor> pairedDonorsToDelete = Sets.newHashSet();
    for (Map.Entry<UnosDonor, Long> entry : parseData.getDonorToPatientId()
        .entrySet()) {
      boolean isUnique = usedIds.add(entry.getKey().getId());
      if (!isUnique) {
        System.err.println("Multiple individuals with id: "
            + entry.getKey().getId());
        pairedDonorsToDelete.add(entry.getKey());
      }
      if (!parseData.getPatientIdToPatient().containsKey(entry.getValue())) {
        throw new RuntimeException("Donor: " + entry.getKey().getId()
            + " listed related donor as: " + entry.getValue()
            + ", but no such patient exists");
      }
    }
    for (UnosDonor unosDonor : pairedDonorsToDelete) {
      parseData.getDonorToPatientId().remove(unosDonor);
    }
    for (UnosPatient patient : parseData.getPatientIdToPatient().values()) {
      boolean isUnique = usedIds.add(patient.getId());
      if (!isUnique) {
        throw new RuntimeException("Multiple individuals with id: "
            + patient.getId());
      }
    }
  }

}