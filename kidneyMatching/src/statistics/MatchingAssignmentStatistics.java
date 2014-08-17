package statistics;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;

import data.AbstractMatching;
import data.MatchingAssignment;
import data.ProblemData;
import data.Receiver;

public class MatchingAssignmentStatistics {
	
	private MatchingAssignment matchingAssignment;
	private DateTime now;
	private DescriptiveStatistics receiverNoChipWaitingTimeStatistics;
	private DescriptiveStatistics chipWaitingTimeStatistics;
	private int numOpenChains;
	private int totalNumMatches;
	
	public MatchingAssignmentStatistics(ProblemData problemData, MatchingAssignment matchingAssignment, DateTime now){
		this.matchingAssignment = matchingAssignment;
		this.now = now;
		this.numOpenChains = matchingAssignment.getActiveChains().size();
		this.receiverNoChipWaitingTimeStatistics = new DescriptiveStatistics();
		Set<Receiver> transplanted = new HashSet<Receiver>();
		for(AbstractMatching matching: matchingAssignment.getMatchings()){
			
		}
		for(Receiver rec: problemData.getPairedReceivers().keySet()){
			
		}
		this.chipWaitingTimeStatistics = new DescriptiveStatistics();
	}
	
	
	
	public int getNumOpenChains(){
		return numOpenChains;
	}

	
	

}
