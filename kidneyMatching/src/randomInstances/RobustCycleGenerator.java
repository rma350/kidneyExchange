package randomInstances;

import ilog.concert.IloException;

import java.util.HashSet;
import java.util.List;

import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import exchangeGraph.CplexUtil;
import exchangeGraph.CycleChainPackingSubtourElimination;
import exchangeGraph.SolverOption;

import graphUtil.Edge;
import graphUtil.Node;
import kepLib.KepInstance;

public class RobustCycleGenerator {
	
	public static void main(String[] args){
		KepInstance<Node,Edge> instance =  makeHardInstance(30);
		Optional<Double> maxSolveTimeSeconds = Optional.absent();
		Optional<FixedThreadPool> threading = FixedThreadPool.makePool(8);
		ImmutableSet<SolverOption> solverOptions = SolverOption
				.makeCheckedOptions(SolverOption.cutsetMode,
						SolverOption.expandedFormulation,
						SolverOption.userCutCallback,
						SolverOption.lazyConstraintCallback);
		/*ImmutableSet<SolverOption> solverOptions = SolverOption
				.makeCheckedOptions(SolverOption.edgeMode,
						SolverOption.expandedFormulation,
						SolverOption.lazyConstraintCallback);*/
		double runTimeSeconds = 0;
		try{
			CycleChainPackingSubtourElimination<Node,Edge> solver = 
					new CycleChainPackingSubtourElimination<Node,Edge>(instance, true, maxSolveTimeSeconds,threading,
							solverOptions);
			solver.solve();
			runTimeSeconds = solver.getSolveTimeSeconds();
			
			solver.cleanUp();
			}
			catch(IloException e){
				System.err.println(e);
				runTimeSeconds = maxSolveTimeSeconds.get().doubleValue();
			}
		System.out.println("Total Running time: " + runTimeSeconds);
		FixedThreadPool.shutDown(threading);
	}

	public static KepInstance<Node,Edge> makeHardInstance(int size){
		DirectedSparseMultigraph<Node,Edge> graph = new DirectedSparseMultigraph<Node,Edge>();
		List<Node> nodes = Lists.newArrayList();
		for(int i = 0; i < size; i++){
			nodes.add(new Node("p"+i));
			graph.addVertex(nodes.get(i));
		}
		for(int i = 0; i < nodes.size(); i++){
			int third = nodes.size()/3;
			Node source = nodes.get(i);
			Node oneAhead = nodes.get((i+1) %nodes.size());
			Node twoAhead = nodes.get((i+2) %nodes.size());
			Node thirdAhead = nodes.get((i+third+2) %nodes.size());
			Node twoThirdsAhead = nodes.get((i+2*third+2) %nodes.size());
			List<Node> targets = Lists.newArrayList(oneAhead,twoAhead,thirdAhead,twoThirdsAhead);
			for(Node dest: targets){
				graph.addEdge(new Edge(source.getName() + dest.getName()), source, dest);
			}
		}
		return new KepInstance<Node, Edge>(graph, new HashSet<Node>(),
				new HashSet<Node>(nodes), new HashSet<Node>(), CplexUtil.unity,
				Integer.MAX_VALUE, 3, 0);
	}

}
