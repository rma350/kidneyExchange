package protoModeler;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import kepLib.KepProblemData;
import kepModeler.ModelerInputs;
import kepProtos.KepProtos;
import kepProtos.KepProtos.EdgePredicate;
import kepProtos.KepProtos.NeighborhoodPredicate;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;

public abstract class BasePredicateBuilder<V, E> implements
    PredicateBuilder<V, E> {

  private ModelerInputs<V, E> inputs;
  private HistoricData<V, E> historicData;

  protected HistoricData<V, E> getHistoricData() {
    return this.historicData;
  }

  public static interface HistoricData<V, E> {

    public double historicDonorPower(V node);

    public double historicPatientPower(V node);

    public KepProtos.NodeType rawNodeType(V node);
  }

  protected ModelerInputs<V, E> getInputs() {
    return this.inputs;
  }

  public BasePredicateBuilder(ModelerInputs<V, E> inputs,
      HistoricData<V, E> historicData) {
    this.inputs = inputs;
  }

  @Override
  public List<Predicate<E>> makeEdgePredicate(EdgePredicate protoEdgePredicate) {
    List<Predicate<E>> ans = Lists.newArrayList();
    if (protoEdgePredicate.hasSource()) {
      for (Predicate<V> nodePredicate : makeNodePredicate(protoEdgePredicate
          .getSource())) {
        ans.add(edgeSource(nodePredicate));
      }
    }
    if (protoEdgePredicate.hasTarget()) {
      for (Predicate<V> nodePredicate : makeNodePredicate(protoEdgePredicate
          .getTarget())) {
        ans.add(edgeTarget(nodePredicate));
      }
    }
    if (protoEdgePredicate.hasCheckSameCenter()
        && protoEdgePredicate.getCheckSameCenter()) {
      ans.add(checkSameCenter());
    }
    if (protoEdgePredicate.hasEdgeDonorPredicate()) {
      ans.addAll(donorEdgePredicate(protoEdgePredicate.getEdgeDonorPredicate()));
    }
    return ans;
  }

  public Predicate<E> edgeSource(final Predicate<V> nodePredicate) {
    return new Predicate<E>() {
      public boolean apply(E edge) {
        return nodePredicate.apply(inputs.getKepProblemData().getGraph()
            .getSource(edge));
      }
    };
  }

  public Predicate<E> edgeTarget(final Predicate<V> nodePredicate) {
    return new Predicate<E>() {
      public boolean apply(E edge) {
        return nodePredicate.apply(inputs.getKepProblemData().getGraph()
            .getDest(edge));
      }
    };
  }

  public abstract Predicate<E> checkSameCenter();

  public abstract List<Predicate<E>> donorEdgePredicate(
      KepProtos.DonorPredicate donorPredicate);

  public List<Predicate<V>> makeNodePredicate(
      KepProtos.NodePredicate protoNodePredicate) {
    List<Predicate<V>> ans = Lists.newArrayList();
    if (protoNodePredicate.hasDonorPredicate()) {
      if (protoNodePredicate.getRawNodeTypeList().contains(
          KepProtos.NodeType.TERMINAL)
          || protoNodePredicate.getCurrentNodeTypeList().contains(
              KepProtos.NodeType.TERMINAL)) {
        throw new RuntimeException(
            "Cannot filter on donor type when query could select terminal nodes that contain no donor");
      }
      ans.add(donorPredicate(protoNodePredicate.getDonorPredicate(),
          protoNodePredicate.getCombinePredicatesForDonor(),
          protoNodePredicate.getCombineDonorsForNode()));
    }

    if (protoNodePredicate.hasPatientPredicate()) {
      if (protoNodePredicate.getRawNodeTypeList().contains(
          KepProtos.NodeType.NDD)
          || protoNodePredicate.getCurrentNodeTypeList().contains(
              KepProtos.NodeType.NDD)) {
        throw new RuntimeException(
            "Cannot filter on patient type when query could select NDD nodes that contain no patient");
      }
      ans.addAll(patientPredicate(protoNodePredicate.getPatientPredicate()));
    }
    if (protoNodePredicate.hasInDegree()) {
      ans.add(inDegree(protoNodePredicate.getInDegree()));
    }
    if (protoNodePredicate.hasOutDegree()) {
      ans.add(outDegree(protoNodePredicate.getOutDegree()));
    }
    if (protoNodePredicate.getInNeighborhoodPredicateCount() > 0) {
      for (NeighborhoodPredicate inPred : protoNodePredicate
          .getInNeighborhoodPredicateList()) {
        ans.add(inNeighborhood(inPred));
      }
    }
    if (protoNodePredicate.getOutNeighborhoodPredicateCount() > 0) {
      for (NeighborhoodPredicate outPred : protoNodePredicate
          .getOutNeighborhoodPredicateList()) {
        ans.add(outNeighborhood(outPred));
      }
    }
    if (protoNodePredicate.getRawNodeTypeCount() > 0) {
      ans.add(rawNodeTypeIn(protoNodePredicate.getRawNodeTypeList()));
    }
    if (protoNodePredicate.getCurrentNodeTypeCount() > 0) {
      ans.add(currentNodeTypeIn(protoNodePredicate.getCurrentNodeTypeList()));
    }
    if (protoNodePredicate.hasWaitingTime()) {
      ans.add(waitingTime(protoNodePredicate.getWaitingTime()));
    }
    if (protoNodePredicate.hasPoolPatientPower()) {
      ans.add(poolPatientPower(protoNodePredicate.getPoolPatientPower()));
    }
    if (protoNodePredicate.hasHistoricPatientPower()) {
      ans.add(historicPatientPower(protoNodePredicate.getHistoricPatientPower()));
    }

    if (protoNodePredicate.hasPoolDonorPower()) {
      ans.add(poolDonorPower(protoNodePredicate.getPoolDonorPower()));
    }
    if (protoNodePredicate.hasHistoricDonorPower()) {
      ans.add(historicDonorPower(protoNodePredicate.getHistoricDonorPower()));
    }

    if (protoNodePredicate.hasPoolPairMatchPower()) {
      ans.add(poolPairMatchPower(protoNodePredicate.getPoolPairMatchPower()));
    }
    if (protoNodePredicate.hasHistoricPairMatchPower()) {
      ans.add(historicPairMatchPower(protoNodePredicate
          .getHistoricPairMatchPower()));
    }

    return ans;
  }

  public abstract Predicate<V> donorPredicate(
      KepProtos.DonorPredicate protoDonorPredicate,
      KepProtos.Relation combinePredicatesForDonor,
      KepProtos.Relation combineDonorsForNode);

  public abstract List<Predicate<V>> patientPredicate(
      KepProtos.PatientPredicate protoPatientPredicate);

  public Predicate<V> rawNodeTypeIn(final List<KepProtos.NodeType> nodeTypes) {
    return new Predicate<V>() {
      public boolean apply(V v) {
        return nodeTypes.contains(getHistoricData().rawNodeType(v));
      }
    };
  }

  public Predicate<V> currentNodeTypeIn(final List<KepProtos.NodeType> nodeTypes) {
    return new Predicate<V>() {
      public boolean apply(V v) {
        return nodeTypes.contains(ProtoUtil.getCurrentNodeType(v, getInputs()
            .getKepProblemData()));
      }
    };
  }

  public Predicate<V> inDegree(KepProtos.Range protoRange) {
    final Range<Double> range = ProtoUtil.createRange(protoRange);
    return new Predicate<V>() {
      public boolean apply(V v) {
        return range.contains((double) getInputs().getKepProblemData()
            .getGraph().inDegree(v));
      }
    };
  }

  public Predicate<V> inNeighborhood(
      KepProtos.NeighborhoodPredicate inNeighborhood) {
    final Range<Double> range = ProtoUtil.createRange(inNeighborhood
        .getNeighborCount());
    final Predicate<V> neighborPredicate = Predicates
        .and(makeNodePredicate(inNeighborhood.getNeighborPredicate()));
    return new Predicate<V>() {
      public boolean apply(V v) {
        int count = 0;
        for (E edge : getInputs().getKepProblemData().getGraph().getInEdges(v)) {
          if (neighborPredicate.apply(getInputs().getKepProblemData()
              .getGraph().getSource(edge))) {
            count++;
          }
        }
        return range.contains((double) count);
      }
    };
  }

  public Predicate<V> outNeighborhood(
      KepProtos.NeighborhoodPredicate outNeighborhood) {
    final Range<Double> range = ProtoUtil.createRange(outNeighborhood
        .getNeighborCount());
    final Predicate<V> neighborPredicate = Predicates
        .and(makeNodePredicate(outNeighborhood.getNeighborPredicate()));
    return new Predicate<V>() {
      public boolean apply(V v) {
        int count = 0;
        for (E edge : getInputs().getKepProblemData().getGraph().getOutEdges(v)) {
          if (neighborPredicate.apply(getInputs().getKepProblemData()
              .getGraph().getDest(edge))) {
            count++;
          }
        }
        return range.contains((double) count);
      }
    };
  }

  public Predicate<V> outDegree(KepProtos.Range protoRange) {
    final Range<Double> range = ProtoUtil.createRange(protoRange);
    return new Predicate<V>() {
      public boolean apply(V v) {
        return range.contains((double) getInputs().getKepProblemData()
            .getGraph().outDegree(v));
      }
    };
  }

  public Predicate<V> waitingTime(KepProtos.Range protoRange) {
    final Range<Double> range = ProtoUtil.createRange(protoRange);
    return new Predicate<V>() {
      public boolean apply(V v) {
        return range.contains(getInputs().getTimeWaitedDays().get(v));
      }
    };
  }

  private double computePoolPatientPower(V v) {
    int pairedNodes = getInputs().getKepProblemData().getPairedNodes().size();
    if (pairedNodes == 0) {
      return 0;
    }
    KepProblemData<V, E> data = getInputs().getKepProblemData();
    Collection<E> inEdges = data.getGraph().getInEdges(v);
    Set<V> inNodes = Sets.newHashSet();
    for (E edge : inEdges) {
      V source = data.getGraph().getSource(edge);
      if (data.getPairedNodes().contains(source)) {
        inNodes.add(source);
      }
    }
    return inNodes.size() / (double) pairedNodes;
  }

  public final Predicate<V> poolPatientPower(KepProtos.Range protoRange) {
    final Range<Double> range = ProtoUtil.createRange(protoRange);
    return new Predicate<V>() {
      public boolean apply(V v) {
        return range.contains(computePoolPatientPower(v));
      }
    };
  }

  private double computePoolDonorPower(V v) {
    int pairedNodes = getInputs().getKepProblemData().getPairedNodes().size();
    if (pairedNodes == 0) {
      return 0;
    }
    KepProblemData<V, E> data = getInputs().getKepProblemData();
    Collection<E> outEdges = data.getGraph().getOutEdges(v);
    Set<V> outNodes = Sets.newHashSet();
    for (E edge : outEdges) {
      V target = data.getGraph().getDest(edge);
      if (data.getPairedNodes().contains(target)) {
        outNodes.add(target);
      }
    }
    return outNodes.size() / (double) pairedNodes;
  }

  public final Predicate<V> poolDonorPower(KepProtos.Range protoRange) {
    final Range<Double> range = ProtoUtil.createRange(protoRange);
    return new Predicate<V>() {
      public boolean apply(V v) {
        return range.contains(computePoolDonorPower(v));
      }
    };
  }

  public final Predicate<V> historicPatientPower(KepProtos.Range protoRange) {
    final Range<Double> range = ProtoUtil.createRange(protoRange);
    return new Predicate<V>() {
      public boolean apply(V v) {
        return range.contains(getHistoricData().historicPatientPower(v));
      }
    };
  }

  public final Predicate<V> historicDonorPower(KepProtos.Range protoRange) {
    final Range<Double> range = ProtoUtil.createRange(protoRange);
    return new Predicate<V>() {
      public boolean apply(V v) {
        return range.contains(getHistoricData().historicDonorPower(v));
      }
    };
  }

  public final Predicate<V> poolPairMatchPower(KepProtos.Range protoRange) {
    final Range<Double> range = ProtoUtil.createRange(protoRange);
    return new Predicate<V>() {
      public boolean apply(V v) {
        double pairMatchPower = 100 * 100 * computePoolDonorPower(v)
            * computePoolPatientPower(v);
        return range.contains(pairMatchPower);
      }
    };
  }

  public final Predicate<V> historicPairMatchPower(KepProtos.Range protoRange) {
    final Range<Double> range = ProtoUtil.createRange(protoRange);
    return new Predicate<V>() {
      public boolean apply(V v) {
        double pairMatchPower = 100 * 100
            * getHistoricData().historicDonorPower(v)
            * getHistoricData().historicPatientPower(v);
        return range.contains(pairMatchPower);
      }
    };
  }

}
