package replicator;

import java.util.ArrayList;
import java.util.List;

import multiPeriod.TimeInstant;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;

public class TimePeriodUtil {
	
	public static List<ReadableInstant> generateSchedule(DateTime firstMatching, Period periodBetweenMatchings, DateTime end){
		List<ReadableInstant> ans = new ArrayList<ReadableInstant>();
		DateTime next = firstMatching;
		while(!next.isAfter(end)){
			ans.add(next);
			next = next.plus(periodBetweenMatchings);
		}
		return ans;
	}
	
	public static List<TimeInstant<ReadableInstant>> generateScheduleTimeInstants(DateTime firstMatching, Period periodBetweenMatchings, DateTime end, boolean forceEnd){
		List<TimeInstant<ReadableInstant>> ans = new ArrayList<TimeInstant<ReadableInstant>>();
		for(ReadableInstant instant: generateSchedule(firstMatching,periodBetweenMatchings,end)){
			ans.add(new TimeInstant<ReadableInstant>(instant));
		}
		if(forceEnd && !ans.get(ans.size()-1).getValue().equals(end)){
			ans.add(new TimeInstant<ReadableInstant>(end));
		}
		return ans;
	}
	
	public static List<TimeInstant<ReadableInstant>> generateScheduleTimeInstantsExludeFirst(DateTime firstMatching, Period periodBetweenMatchings, DateTime end, boolean forceEnd){
		List<TimeInstant<ReadableInstant>> ans = generateScheduleTimeInstants(firstMatching,periodBetweenMatchings,end,forceEnd);
		if(ans.size()>0){
		ans.remove(0);
		}
		return ans;
	}

}
