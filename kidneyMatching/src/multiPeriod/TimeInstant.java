package multiPeriod;

public class TimeInstant<T extends Comparable<T>> implements
    Comparable<TimeInstant<T>> {

  private T value;

  public T getValue() {
    return value;
  }

  public TimeInstant(T value) {
    super();
    this.value = value;
  }

  public String toString() {
    return value.toString();
  }

  @Override
  public int compareTo(TimeInstant<T> o) {
    return this.value.compareTo(o.getValue());
  }

  // ublic abstract double difference(TimeInstant<T> other);

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TimeInstant other = (TimeInstant) obj;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
      return false;
    return true;
  }

}
