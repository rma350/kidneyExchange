package inputOutput.aggregate;

import inputOutput.aggregate.TrialReport.BaseNumericTrialAttribute;
import inputOutput.aggregate.TrialReport.TrialOutcome;
import multiPeriod.DynamicMatching;

import com.google.common.collect.Range;

public class ChainCycleAnalysis {

  public static class CycleAttribute<V, E, T extends Comparable<T>, D> extends
      BaseNumericTrialAttribute<V, E, T, D> {

    private int cycleLength;
    private String name;

    public CycleAttribute(int cycleLength) {
      this(cycleLength, "cyclesOfLength" + cycleLength);
    }

    public CycleAttribute(int cycleLength, String name) {
      super();
      this.cycleLength = cycleLength;
      this.name = name;
    }

    @Override
    public String getAttributeName() {
      return name;
    }

    @Override
    public double applyForDouble(TrialOutcome<V, E, T, D> trialOutcome) {
      int ans = 0;
      for (DynamicMatching<V, E, T>.SimultaneousMatch cycle : trialOutcome
          .getDynamicMatching().getCycles()) {
        if (cycle.size() == cycleLength) {
          ans++;
        }
      }
      return ans;
    }
  }

  public static class TotalCycleEdges<V, E, T extends Comparable<T>, D> extends
      BaseNumericTrialAttribute<V, E, T, D> {

    private Range<Integer> cycleLength;
    private String name;

    public TotalCycleEdges() {
      this(Range.<Integer> all());
    }

    public TotalCycleEdges(Range<Integer> cycleLength) {
      this(cycleLength, "totalEdgesCycleLengthIn" + cycleLength.toString());
    }

    public TotalCycleEdges(Range<Integer> cycleLength, String name) {
      super();
      this.cycleLength = cycleLength;
      this.name = name;
    }

    @Override
    public String getAttributeName() {
      return name;
    }

    @Override
    public double applyForDouble(TrialOutcome<V, E, T, D> trialOutcome) {
      int ans = 0;
      for (DynamicMatching<V, E, T>.SimultaneousMatch cycle : trialOutcome
          .getDynamicMatching().getCycles()) {
        if (this.cycleLength.contains(cycle.size())) {
          ans += cycle.size();
        }
      }
      return ans;
    }

  }

  public static class ChainAttribute<V, E, T extends Comparable<T>, D> extends
      BaseNumericTrialAttribute<V, E, T, D> {

    private Range<Integer> chainLength;
    private String name;

    public ChainAttribute(Range<Integer> range) {
      this(range, "chainLengthIn" + range.toString());
    }

    public ChainAttribute(Range<Integer> chainLength, String name) {
      super();
      this.chainLength = chainLength;
      this.name = name;
    }

    @Override
    public String getAttributeName() {
      return name;
    }

    @Override
    public double applyForDouble(TrialOutcome<V, E, T, D> trialOutcome) {
      int ans = 0;
      for (DynamicMatching<V, E, T>.Chain chain : trialOutcome
          .getDynamicMatching().getChainRootToChain().values()) {
        if (this.chainLength.contains(Integer.valueOf(chain.getNumEdges()))) {
          ans++;
        }
      }
      return ans;
    }

  }

  public static class TotalChainEdges<V, E, T extends Comparable<T>, D> extends
      BaseNumericTrialAttribute<V, E, T, D> {

    private Range<Integer> chainLength;
    private String name;
    private boolean adjustToPairedNodeCount;

    public TotalChainEdges(boolean adjustToPairedNodeCount) {
      this(adjustToPairedNodeCount, Range.<Integer> all());
    }

    public TotalChainEdges(boolean adjustToPairedNodeCount,
        Range<Integer> chainLength) {
      this(adjustToPairedNodeCount, chainLength, "totalEdgesChainLengthIn"
          + chainLength.toString());
    }

    public TotalChainEdges(boolean adjustToPairedNodeCount,
        Range<Integer> chainLength, String name) {
      super();
      this.chainLength = chainLength;
      this.name = name;
      this.adjustToPairedNodeCount = adjustToPairedNodeCount;
    }

    @Override
    public String getAttributeName() {
      return name;
    }

    @Override
    public double applyForDouble(TrialOutcome<V, E, T, D> trialOutcome) {
      int ans = 0;
      for (DynamicMatching<V, E, T>.Chain chain : trialOutcome
          .getDynamicMatching().getChainRootToChain().values()) {
        int chainSize = adjustToPairedNodeCount ? chain.getNumEdges() - 1
            : chain.getNumEdges();
        if (this.chainLength.contains(chainSize)) {
          ans += chainSize;
        }
      }
      return ans;
    }

  }

}
