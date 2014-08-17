package protoModeler;

import java.util.List;

import kepLib.KepProblemData;
import kepProtos.KepProtos;
import unosData.UnosDonor;
import unosData.UnosDonorEdge;
import unosData.UnosExchangeUnit;
import unosData.UnosPatient;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

public class ProtoUtil {

  public static <T> Predicate<T> combinePredicates(
      Iterable<Predicate<T>> predicates, KepProtos.Relation relation) {
    if (relation == KepProtos.Relation.AND) {
      return Predicates.and(predicates);
    } else if (relation == KepProtos.Relation.OR) {
      return Predicates.or(predicates);
    } else {
      throw new RuntimeException("Unexpected Predicate relation: " + relation);
    }
  }

  public static Range<Double> createRange(KepProtos.Range protoRange) {
    if (protoRange.hasLowerBound()) {
      if (protoRange.hasUpperBound()) {
        return Range.closedOpen(protoRange.getLowerBound(),
            protoRange.getUpperBound());
      } else {
        return Range.atLeast(protoRange.getLowerBound());
      }
    } else if (protoRange.hasUpperBound()) {
      return Range.lessThan(protoRange.getUpperBound());
    } else {
      throw new RuntimeException(
          "Found range with no lower bound and no upper bound.");
    }
  }

  public static List<Range<Double>> createRanges(
      Iterable<KepProtos.Range> protoRanges) {
    List<Range<Double>> ans = Lists.newArrayList();
    for (KepProtos.Range protoRange : protoRanges) {
      ans.add(createRange(protoRange));
    }
    return ans;
  }

  public static <V, E> KepProtos.NodeType getCurrentNodeType(V v,
      KepProblemData<V, E> kepProblemData) {
    if (kepProblemData.getRootNodes().contains(v)) {
      return KepProtos.NodeType.NDD;
    } else if (kepProblemData.getPairedNodes().contains(v)) {
      return KepProtos.NodeType.PAIRED;
    } else if (kepProblemData.getTerminalNodes().contains(v)) {
      return KepProtos.NodeType.TERMINAL;
    } else {
      throw new RuntimeException("Unrecognized node type for node: " + v);
    }
  }

  public static double computePatientPra(UnosPatient patient,
      KepProblemData<UnosExchangeUnit, UnosDonorEdge> currentPool) {
    if (currentPool.getPairedNodes().size() == 0) {
      return 0;
    }
    int matchCount = 0;
    int donorCount = 0;
    for (UnosExchangeUnit unit : currentPool.getPairedNodes()) {
      for (UnosDonor donor : unit.getDonors()) {
        donorCount++;
        if (patient.getTissueTypeSensitivity().isCompatible(
            donor.getTissueType())) {
          matchCount++;
        }
      }
    }
    return 100 * (1 - matchCount / (double) donorCount);
  }

  public static double computeDonorPra(UnosDonor donor,
      KepProblemData<UnosExchangeUnit, UnosDonorEdge> currentPool) {
    if (currentPool.getPairedNodes().size() == 0) {
      return 0;
    }
    int matchCount = 0;
    for (UnosExchangeUnit unit : currentPool.getPairedNodes()) {
      if (unit.getPatient().getTissueTypeSensitivity()
          .isCompatible(donor.getTissueType())) {
        matchCount++;
      }
    }
    return 100 * (1 - matchCount / (double) currentPool.getPairedNodes().size());
  }

  public static double computeDonorPower(UnosDonor donor,
      UnosExchangeUnit donorNode,
      KepProblemData<UnosExchangeUnit, UnosDonorEdge> currentPool) {
    if (currentPool.getPairedNodes().size() == 0) {
      return 0;
    }
    int useCount = 0;
    for (UnosDonorEdge edge : currentPool.getGraph().getOutEdges(donorNode)) {
      if (edge.getDonor() == donor
          && !currentPool.getTerminalNodes().contains(
              currentPool.getGraph().getDest(edge))) {
        useCount++;
      }
    }
    return useCount / (double) currentPool.getPairedNodes().size();
  }

}
