package multiPeriod;

import java.util.List;


import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import com.google.common.collect.Lists;

public abstract class TimeWriter<T extends Comparable<T>> {
  public double writeTime(TimeInstant<T> timeInstant) {
    return writeTime(timeInstant.getValue());
  }

  public TimeInstant<T> readTime(double time) {
    return new TimeInstant<T>(read(time));
  }

  public abstract double writeTime(T time);

  public abstract T read(double time);

  public abstract List<TimeInstant<T>> makeMatchingTimes(T start, T end,
      int frequency);

  public static class DoubleTimeWriter extends TimeWriter<Double> {

    public static final DoubleTimeWriter INSTANCE = new DoubleTimeWriter();

    private DoubleTimeWriter() {
    }

    @Override
    public double writeTime(Double time) {
      return time.doubleValue();
    }

    @Override
    public Double read(double time) {
      return time;
    }

    @Override
    public List<TimeInstant<Double>> makeMatchingTimes(Double start,
        Double end, int frequency) {
      List<TimeInstant<Double>> ans = Lists.newArrayList();
      double head = start;
      while (head < end) {
        ans.add(new TimeInstant<Double>(head));
        head += frequency;
      }
      double lastVal = ans.get(ans.size() - 1).getValue();
      if (lastVal + .01 < end) {
        ans.add(new TimeInstant<Double>(end));
      }
      return ans;
    }
  }

  public static class DateTimeWriter extends TimeWriter<ReadableInstant> {

    public static final DateTimeWriter INSTANCE = new DateTimeWriter();

    private DateTimeWriter() {
    }

    @Override
    public double writeTime(ReadableInstant time) {
      return time.getMillis();
    }

    @Override
    public ReadableInstant read(double time) {
      return new DateTime((long) time);
    }

    @Override
    public List<TimeInstant<ReadableInstant>> makeMatchingTimes(
        ReadableInstant start, ReadableInstant end, int frequency) {
      throw new UnsupportedOperationException("not yet implemented");
    }
  }
}