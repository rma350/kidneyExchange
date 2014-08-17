package exchangeGraph;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloRange;

import java.util.List;

import com.google.common.collect.Lists;



public class NoUserCuts implements UserCutGenerator{
	
	public static final NoUserCuts INSTANCE = new NoUserCuts();
	
	private NoUserCuts(){}

	@Override
	public List<IloRange> quickUserCut()
			throws IloException {
		return Lists.newArrayList();
	}

	@Override
	public List<IloRange> fullUserCut()
			throws IloException {
		return Lists.newArrayList();
	}

}
