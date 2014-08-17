package inputOutput.core;

import multiPeriod.TimeInstant;

public interface TimeDifferenceCalc<T extends Comparable<T>,D> {

	public D getDifference(TimeInstant<T> timeLo, TimeInstant<T> timeHi);
	
	public TimeInstant<T> add(TimeInstant<T> timeLo, D timeToAdd);
	
}
