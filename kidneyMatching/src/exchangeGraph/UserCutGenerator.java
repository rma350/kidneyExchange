package exchangeGraph;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloRange;

import java.util.List;


public interface UserCutGenerator {
	
	public List<IloRange> quickUserCut() throws IloException;
	
	public List<IloRange> fullUserCut() throws IloException;

}
