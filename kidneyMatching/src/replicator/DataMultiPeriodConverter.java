package replicator;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;
import multiPeriod.TimeInstant;
import multiPeriod.TimeWriter;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import com.google.common.collect.ImmutableMap;

import data.AbstractMatching;
import data.Chain;
import data.Chain.Cluster;
import data.Cycle;
import data.Donor;
import data.ExchangeUnit;
import data.ExchangeUnit.ExchangeUnitType;
import data.MedicalMatch;
import data.MedicalMatch.Incompatability;
import data.ProblemData;
import data.Receiver;
import data.Transplant;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class DataMultiPeriodConverter {

  private ProblemData problemData;
  // private CycleChainPackingFactory<ExchangeUnit,DefaultWeightedEdge>
  // cycleChainPackingFactory;
  // private List<TimeInstant<ReadableInstant>> matchingTimes;
  private ReadableInstant startTime;
  private ReadableInstant endTime;
  private MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> multiPeriodPackingInputs;

  private boolean allowSelfMatches = false;

  public ProblemData getProblemData() {
    return problemData;
  }

  /*
   * public CycleChainPackingFactory<ExchangeUnit, DefaultWeightedEdge>
   * getCycleChainPackingFactory() { return cycleChainPackingFactory; }
   * 
   * public List<TimeInstant<ReadableInstant>> getMatchingTimes() { return
   * matchingTimes; }
   */

  public ReadableInstant getStartTime() {
    return startTime;
  }

  public ReadableInstant getEndTime() {
    return endTime;
  }

  public MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant> getMultiPeriodPackingInputs() {
    return multiPeriodPackingInputs;
  }

  private ReadableInstant maxTime(DateTime time) {
    if (this.startTime != null && this.startTime.isAfter(time)) {
      return this.startTime;
    } else {
      return time;
    }
  }

  private static DateTime maximum(DateTime first, DateTime second) {
    if (first.isAfter(second)) {
      return first;
    } else {
      return second;
    }
  }

  private static DateTime minimum(DateTime first, DateTime second) {
    if (first.isBefore(second)) {
      return first;
    } else {
      return second;
    }
  }

  private static DateTime minTime(List<Transplant> transplants) {
    if (transplants.size() == 0) {
      return null;
    } else {
      DateTime best = transplants.get(0).getDateTransplanted();
      if (best == null) {
        return null;// TODO this is a hack
      }
      for (int i = 1; i < transplants.size(); i++) {
        best = minimum(best, transplants.get(i).getDateTransplanted());
      }
      return best;
    }
  }

  public DataMultiPeriodConverter(ProblemData problemData,
      ReadableInstant startTime, ReadableInstant endTime) {
    super();
    this.problemData = problemData;
    this.startTime = startTime;
    this.endTime = endTime;
    DirectedSparseMultigraph<ExchangeUnit, DonorEdge> graph = new DirectedSparseMultigraph<ExchangeUnit, DonorEdge>();
    ImmutableMap.Builder<ExchangeUnit, TimeInstant<ReadableInstant>> nodeArrivalTimes = ImmutableMap
        .builder();
    ImmutableMap.Builder<DonorEdge, TimeInstant<ReadableInstant>> edgeArrivalTimes = ImmutableMap
        .builder();
    Set<ExchangeUnit> chainRoots = new HashSet<ExchangeUnit>();
    Set<ExchangeUnit> terminalNodes = new HashSet<ExchangeUnit>();

    // these sets are determined by the start time, they will be empty if the
    // start time is null
    Set<ExchangeUnit> unitsAlreadyMatched = new HashSet<ExchangeUnit>();
    Set<ExchangeUnit> activeChainsTails = new HashSet<ExchangeUnit>();
    if (startTime != null) {
      int numUnitsHistoricMatched = 0;
      for (AbstractMatching matching : problemData
          .getHistoricMatchingAssignment().getMatchings()) {
        if (matching instanceof Cycle) {
          Cycle cycle = (Cycle) matching;
          DateTime minTime = minTime(cycle.getTransplants());
          /*
           * if(minTime == null){ throw new RuntimeException(); }
           */
          if (minTime != null && minTime.isBefore(this.startTime)) {
            unitsAlreadyMatched.addAll(cycle.getExchangeUnits());
          } else {
            numUnitsHistoricMatched += cycle.getTransplants().size();
          }
        } else if (matching instanceof Chain) {
          Chain chain = (Chain) matching;
          boolean chainStopped = false;
          for (Cluster cluster : chain.getClusters()) {
            if (!chainStopped) {
              DateTime clusterTime = minTime(cluster.getTransplants());

              if (clusterTime == null || !clusterTime.isBefore(this.startTime)) {
                activeChainsTails.add(cluster.getExchangeUnits().get(0));
                chainStopped = true;
              } else {
                unitsAlreadyMatched.addAll(cluster.getExchangeUnits());
              }
            }
            if (chainStopped) {
              numUnitsHistoricMatched += cluster.getTransplants().size();
            }
          }

        }
      }
      System.out
          .println("Number of units that were matched historically beyond simulation start: "
              + numUnitsHistoricMatched);
    } else {
      // System.out.println("Number of units that were matched historically beyond simulation start: ");
    }
    for (ExchangeUnit unit : this.problemData.getExchangeUnits()) {
      if (!unitsAlreadyMatched.contains(unit)) {
        graph.addVertex(unit);
        boolean isAltruist = unit.getExchangeUnitType() == ExchangeUnitType.altruistic;
        DateTime arrival = isAltruist ? unit.getDonor().get(0).getRegistered()
            : unit.getReceiver().getRegistered();
        nodeArrivalTimes.put(unit, new TimeInstant<ReadableInstant>(
            maxTime(arrival)));

        if (isAltruist) {
          chainRoots.add(unit);
        } else if (unit.getExchangeUnitType() == ExchangeUnitType.chip) {
          terminalNodes.add(unit);
        }
      } else if (activeChainsTails.contains(unit)) {
        graph.addVertex(unit);
        DateTime arrival = unit.getReceiver().getRegistered();
        nodeArrivalTimes.put(unit, new TimeInstant<ReadableInstant>(
            maxTime(arrival)));
        chainRoots.add(unit);
      }
    }
    Set<ExchangeUnit> includedUnits = new HashSet<ExchangeUnit>(
        graph.getVertices());
    for (ExchangeUnit donorNode : includedUnits) {
      if (!terminalNodes.contains(donorNode)) {
        for (ExchangeUnit receiverNode : includedUnits) {
          if (donorNode != receiverNode || this.allowSelfMatches) {
            if (!chainRoots.contains(receiverNode)) {
              Receiver receiver = receiverNode.getReceiver();
              // TODO the feasibility of a match may change over time due to age
              // restrictions. For now, we just compute
              // feasibility at the moment with the donor and receiver have both
              // arrived.
              for (Donor donor : donorNode.getDonor()) {
                DateTime testDate = maximum(donor.getRegistered(),
                    receiver.getRegistered());
                if (!problemData.getHardBlockedExchanges().get(receiver)
                    .contains(donor)
                    && isMatchFeasible(donor, receiver, testDate)) {
                  ReadableInstant edgeTime = maxTime(testDate);
                  DonorEdge edge = new DonorEdge(donor);
                  graph.addEdge(edge, donorNode, receiverNode);
                  edgeArrivalTimes.put(edge, new TimeInstant<ReadableInstant>(
                      edgeTime));
                  break;
                }
              }
            }
          }
        }
      }
    }
    multiPeriodPackingInputs = new MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant>(
        graph, chainRoots, terminalNodes, new TimeInstant<ReadableInstant>(
            this.startTime), new TimeInstant<ReadableInstant>(this.endTime),
        nodeArrivalTimes.build(), edgeArrivalTimes.build(),
        TimeWriter.DateTimeWriter.INSTANCE);

  }

  public DataMultiPeriodConverter(ProblemData problemData,
      ReadableInstant startTime, ReadableInstant endTime, boolean freshStart) {
    if (!freshStart) {
      throw new UnsupportedOperationException();
    }
    this.problemData = problemData;
    this.endTime = endTime;
    this.startTime = startTime;
    DirectedSparseMultigraph<ExchangeUnit, DonorEdge> graph = new DirectedSparseMultigraph<ExchangeUnit, DonorEdge>();
    ImmutableMap.Builder<ExchangeUnit, TimeInstant<ReadableInstant>> nodeArrivalTimes = ImmutableMap
        .builder();
    ImmutableMap.Builder<DonorEdge, TimeInstant<ReadableInstant>> edgeArrivalTimes = ImmutableMap
        .builder();
    Set<ExchangeUnit> chainRoots = new HashSet<ExchangeUnit>();
    Set<ExchangeUnit> terminalNodes = new HashSet<ExchangeUnit>();
    for (ExchangeUnit unit : this.problemData.getExchangeUnits()) {
      boolean isAltruist = unit.getExchangeUnitType() == ExchangeUnitType.altruistic;
      DateTime arrival = isAltruist ? unit.getDonor().get(0).getRegistered()
          : unit.getReceiver().getRegistered();
      if (startTime.compareTo(arrival) <= 0) {
        // System.out.println(arrival);
        graph.addVertex(unit);
        nodeArrivalTimes.put(unit, new TimeInstant<ReadableInstant>(arrival));
        if (isAltruist) {
          chainRoots.add(unit);
        } else if (unit.getExchangeUnitType() == ExchangeUnitType.chip) {
          terminalNodes.add(unit);
        }
      }
    }
    Set<ExchangeUnit> includedUnits = new HashSet<ExchangeUnit>(
        graph.getVertices());
    for (ExchangeUnit donorNode : includedUnits) {
      if (!terminalNodes.contains(donorNode)) {
        for (ExchangeUnit receiverNode : includedUnits) {
          if (donorNode != receiverNode || this.allowSelfMatches) {
            if (!chainRoots.contains(receiverNode)) {
              Receiver receiver = receiverNode.getReceiver();
              // TODO the feasibility of a match may change over time due to age
              // restrictions. For now, we just compute
              // feasibility at the moment with the donor and receiver have both
              // arrived.
              for (Donor donor : donorNode.getDonor()) {
                DateTime testDate = maximum(donor.getRegistered(),
                    receiver.getRegistered());
                if (!problemData.getHardBlockedExchanges().get(receiver)
                    .contains(donor)
                    && isMatchFeasible(donor, receiver, testDate)) {
                  ReadableInstant edgeTime = maxTime(testDate);
                  DonorEdge edge = new DonorEdge(donor);
                  graph.addEdge(edge, donorNode, receiverNode);
                  edgeArrivalTimes.put(edge, new TimeInstant<ReadableInstant>(
                      edgeTime));
                  break;
                }
              }
            }
          }
        }
      }
    }
    multiPeriodPackingInputs = new MultiPeriodCyclePackingInputs<ExchangeUnit, DonorEdge, ReadableInstant>(
        graph, chainRoots, terminalNodes, new TimeInstant<ReadableInstant>(
            this.startTime), new TimeInstant<ReadableInstant>(this.endTime),
        nodeArrivalTimes.build(), edgeArrivalTimes.build(),
        TimeWriter.DateTimeWriter.INSTANCE);

  }

  private boolean isMatchFeasible(Donor donor, Receiver receiver, DateTime date) {
    EnumSet<Incompatability> transplantIncompatabilities = MedicalMatch.instance
        .match(donor, receiver, date);
    return transplantIncompatabilities.size() == 0;
  }

}
