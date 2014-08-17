package inputOutput.node;

import inputOutput.core.Attribute;
import inputOutput.core.TimeDifferenceCalc;
import multiPeriod.DynamicMatching;
import multiPeriod.MultiPeriodCyclePacking;
import multiPeriod.MultiPeriodCyclePacking.MultiPeriodCyclePackingInputs;
import multiPeriod.TimeInstant;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class NodeOutputAttributes<V, E, T extends Comparable<T>, D> {

  private NodeInputAttributes<V, E, T> inputAttributes;

  private final MultiPeriodCyclePackingInputs<V, E, T> cyclePackingInputs;
  private final DynamicMatching<V, E, T> dynamicMatching;
  private final TimeDifferenceCalc<T, D> timeDiffCalc;
  private final Attribute<V, Boolean> donorWasMatched;
  private final Attribute<V, TimeInstant<T>> timeDonorMatched;

  private final Attribute<V, D> donorWaitingTime;
  private final Attribute<V, D> totalDonorWaitingTime;
  private final Attribute<V, Boolean> receiverWasMatched;
  private final Attribute<V, TimeInstant<T>> timeReceiverMatched;

  private final Attribute<V, D> receiverWaitingTime;
  private final Attribute<V, D> totalReceiverWaitingTime;

  private final Attribute<V, V> donatingNode;

  public NodeOutputAttributes(MultiPeriodCyclePacking<V, E, T> cyclePacking,
      TimeDifferenceCalc<T, D> timeDiffCalc,
      NodeInputAttributes<V, E, T> inputAttributes) {
    this(cyclePacking.getInputs(), cyclePacking.getDynamicMatching(),
        timeDiffCalc, inputAttributes);
  }

  public NodeOutputAttributes(
      MultiPeriodCyclePackingInputs<V, E, T> cyclePackingInputs,
      DynamicMatching<V, E, T> dynamicMatching,
      TimeDifferenceCalc<T, D> timeDiffCalc,
      NodeInputAttributes<V, E, T> inputAttributes) {
    super();
    this.inputAttributes = inputAttributes;
    this.cyclePackingInputs = cyclePackingInputs;
    this.dynamicMatching = dynamicMatching;
    this.timeDiffCalc = timeDiffCalc;
    this.donorWasMatched = new Attribute<V, Boolean>() {
      @Override
      public Boolean apply(V node) {
        return Boolean.valueOf(NodeOutputAttributes.this.dynamicMatching
            .getNodeToDonorMatching().containsKey(node));
      }
    };
    this.timeDonorMatched = new Attribute<V, TimeInstant<T>>() {
      @Override
      public TimeInstant<T> apply(V node) {
        return NodeOutputAttributes.this.donorWasMatched.apply(node) ? NodeOutputAttributes.this.dynamicMatching
            .getNodeToDonorMatching().get(node).getTimeMatched()
            : null;
      }
    };

    this.donorWaitingTime = new Attribute<V, D>() {
      @Override
      public D apply(V node) {
        if (!NodeOutputAttributes.this.donorWasMatched.apply(node)) {
          return null;
        }
        TimeInstant<T> timeLo = NodeOutputAttributes.this.inputAttributes
            .getTimeNodeArrived().apply(node);
        TimeInstant<T> timeHi = timeDonorMatched.apply(node);
        return NodeOutputAttributes.this.timeDiffCalc.getDifference(timeLo,
            timeHi);
      }
    };
    this.totalDonorWaitingTime = new Attribute<V, D>() {
      @Override
      public D apply(V node) {
        TimeInstant<T> timeLo = NodeOutputAttributes.this.inputAttributes
            .getTimeNodeArrived().apply(node);
        TimeInstant<T> timeHi;
        if (!NodeOutputAttributes.this.donorWasMatched.apply(node)) {
          timeHi = NodeOutputAttributes.this.cyclePackingInputs.getEndTime();
        } else {
          timeHi = timeDonorMatched.apply(node);
        }

        return NodeOutputAttributes.this.timeDiffCalc.getDifference(timeLo,
            timeHi);
      }
    };

    this.receiverWasMatched = new Attribute<V, Boolean>() {
      @Override
      public Boolean apply(V node) {
        return Boolean.valueOf(NodeOutputAttributes.this.dynamicMatching
            .getNodeToReceiverMatching().containsKey(node));
      }
    };
    this.timeReceiverMatched = new Attribute<V, TimeInstant<T>>() {
      @Override
      public TimeInstant<T> apply(V node) {
        return NodeOutputAttributes.this.receiverWasMatched.apply(node) ? NodeOutputAttributes.this.dynamicMatching
            .getNodeToReceiverMatching().get(node).getTimeMatched()
            : null;
      }
    };

    this.receiverWaitingTime = new Attribute<V, D>() {
      @Override
      public D apply(V node) {
        if (!NodeOutputAttributes.this.receiverWasMatched.apply(node)) {
          return null;
        }
        TimeInstant<T> timeLo = NodeOutputAttributes.this.inputAttributes
            .getTimeNodeArrived().apply(node);
        TimeInstant<T> timeHi = timeReceiverMatched.apply(node);
        return NodeOutputAttributes.this.timeDiffCalc.getDifference(timeLo,
            timeHi);
      }
    };
    this.totalReceiverWaitingTime = new Attribute<V, D>() {
      @Override
      public D apply(V node) {
        TimeInstant<T> timeLo = NodeOutputAttributes.this.inputAttributes
            .getTimeNodeArrived().apply(node);
        TimeInstant<T> timeHi;
        if (!NodeOutputAttributes.this.receiverWasMatched.apply(node)) {
          timeHi = NodeOutputAttributes.this.cyclePackingInputs.getEndTime();
        } else {
          timeHi = timeReceiverMatched.apply(node);
        }
        return NodeOutputAttributes.this.timeDiffCalc.getDifference(timeLo,
            timeHi);
      }
    };
    this.donatingNode = new Attribute<V, V>() {
      @Override
      public V apply(V input) {
        if (!receiverWasMatched.apply(input)) {
          return null;
        }
        DirectedSparseMultigraph<V, E> graph = NodeOutputAttributes.this.dynamicMatching
            .getGraph();
        for (E edge : NodeOutputAttributes.this.dynamicMatching
            .getNodeToReceiverMatching().get(input).getEdges()) {
          if (graph.getDest(edge) == input) {
            return graph.getSource(edge);
          }
        }
        throw new RuntimeException("Could not find node preceeding " + input
            + " in matching: " + NodeOutputAttributes.this.dynamicMatching);
      }
    };
  }

  public MultiPeriodCyclePackingInputs<V, E, T> getCyclePackingInputs() {
    return cyclePackingInputs;
  }

  public DynamicMatching<V, E, T> getDynamicMatching() {
    return this.dynamicMatching;
  }

  public TimeDifferenceCalc<T, D> getTimeDiffCalc() {
    return timeDiffCalc;
  }

  public Attribute<V, Boolean> getDonorWasMatched() {
    return donorWasMatched;
  }

  public Attribute<V, TimeInstant<T>> getTimeDonorMatched() {
    return timeDonorMatched;
  }

  public Attribute<V, D> getDonorWaitingTime() {
    return donorWaitingTime;
  }

  public Attribute<V, D> getTotalDonorWaitingTime() {
    return totalDonorWaitingTime;
  }

  public Attribute<V, Boolean> getReceiverWasMatched() {
    return receiverWasMatched;
  }

  public Attribute<V, TimeInstant<T>> getTimeReceiverMatched() {
    return timeReceiverMatched;
  }

  public Attribute<V, D> getReceiverWaitingTime() {
    return receiverWaitingTime;
  }

  public Attribute<V, D> getTotalReceiverWaitingTime() {
    return totalReceiverWaitingTime;
  }

  public NodeInputAttributes<V, E, T> getInputAttributes() {
    return inputAttributes;
  }

  public Attribute<V, V> getDonatingNode() {
    return donatingNode;
  }

}
