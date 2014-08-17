package inputOutput.core;

import multiPeriod.TimeInstant;

public class DoubleTimeDifferenceCalculator implements
    TimeDifferenceCalc<Double, Double> {

  public static final DoubleTimeDifferenceCalculator instance = new DoubleTimeDifferenceCalculator();

  private DoubleTimeDifferenceCalculator() {
  }

  @Override
  public Double getDifference(TimeInstant<Double> timeLo,
      TimeInstant<Double> timeHi) {
    return timeHi.getValue().doubleValue() - timeLo.getValue().doubleValue();
  }

  @Override
  public TimeInstant<Double> add(TimeInstant<Double> timeLo, Double timeToAdd) {
    return new TimeInstant<Double>(
        Double.valueOf(timeLo.getValue() + timeToAdd));
  }

}