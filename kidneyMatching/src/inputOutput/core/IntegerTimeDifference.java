package inputOutput.core;

import multiPeriod.TimeInstant;

public class IntegerTimeDifference implements
    TimeDifferenceCalc<Integer, Integer> {

  public static final IntegerTimeDifference instance = new IntegerTimeDifference();

  private IntegerTimeDifference() {
  }

  @Override
  public Integer getDifference(TimeInstant<Integer> timeLo,
      TimeInstant<Integer> timeHi) {
    return timeHi.getValue().intValue() - timeLo.getValue().intValue();
  }

  @Override
  public TimeInstant<Integer> add(TimeInstant<Integer> timeLo, Integer timeToAdd) {
    return new TimeInstant<Integer>(Integer.valueOf(timeLo.getValue()
        + timeToAdd));
  }

}
