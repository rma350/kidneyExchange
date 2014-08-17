package inputOutput.core;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import com.google.common.base.Function;

public enum SumStatVal {
  MEAN(new Function<StatisticalSummary, Double>() {
    public Double apply(StatisticalSummary statisticalSummary) {
      return statisticalSummary.getMean();
    }
  }), SUM(new Function<StatisticalSummary, Double>() {
    public Double apply(StatisticalSummary statisticalSummary) {
      return statisticalSummary.getSum();
    }
  }), COUNT(new Function<StatisticalSummary, Double>() {
    public Double apply(StatisticalSummary statisticalSummary) {
      return (double) statisticalSummary.getN();
    }
  }), ST_DEV(new Function<StatisticalSummary, Double>() {
    public Double apply(StatisticalSummary statisticalSummary) {
      return statisticalSummary.getStandardDeviation();
    }
  }), MAX(new Function<StatisticalSummary, Double>() {
    public Double apply(StatisticalSummary statisticalSummary) {
      return statisticalSummary.getMax();
    }
  }), MIN(new Function<StatisticalSummary, Double>() {
    public Double apply(StatisticalSummary statisticalSummary) {
      return statisticalSummary.getMin();
    }
  });

  private Function<StatisticalSummary, Double> function;

  private SumStatVal(Function<StatisticalSummary, Double> function) {
    this.function = function;
  }

  public Function<StatisticalSummary, Double> getFunction() {
    return this.function;
  }

}