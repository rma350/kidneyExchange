package inputOutput.core;

import multiPeriod.TimeInstant;

import org.joda.time.Interval;
import org.joda.time.ReadableInstant;

public class JodaTimeDifference implements
    TimeDifferenceCalc<ReadableInstant, Interval> {

  public static final JodaTimeDifference instance = new JodaTimeDifference();

  private JodaTimeDifference() {
  }

  @Override
  public Interval getDifference(TimeInstant<ReadableInstant> timeLo,
      TimeInstant<ReadableInstant> timeHi) {
    return new Interval(timeLo.getValue(), timeHi.getValue());
  }

  @Override
  public TimeInstant<ReadableInstant> add(TimeInstant<ReadableInstant> timeLo,
      Interval timeToAdd) {
    return new TimeInstant<ReadableInstant>(timeToAdd.toDuration()
        .toIntervalFrom(timeLo.getValue()).getEnd());
  }

}
