package statistics;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class StatUtil {

	public static SummaryStatistics asStatistic(int[] values){
		SummaryStatistics ans = new SummaryStatistics();
		for(int val: values){
			ans.addValue(val);
		}
		return ans;
	}
	
	public static SummaryStatistics asStatistic(double[] values){
		SummaryStatistics ans = new SummaryStatistics();
		for(double val: values){
			ans.addValue(val);
		}
		return ans;
	}
	
}
