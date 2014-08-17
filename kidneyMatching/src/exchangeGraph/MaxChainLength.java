package exchangeGraph;

import ilog.concert.IloException;
import ilog.concert.IloLinearIntExpr;
import ilog.cplex.IloCplex;

import java.util.HashMap;
import java.util.Map;

import kepLib.KepInstance;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import edu.uci.ics.jung.algorithms.filters.KNeighborhoodFilter;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class MaxChainLength{
	
	private MaxChainLength(){}
	


	public static <V,E> void constrainMaxLength(KepInstance<V, E> kepInstance, IloCplex cplex,
			ImmutableSet<SolverOption> solverOptions,
			DirectedEdgeVariables<V, E> edgeVariables, boolean allFlowIsFromChains) throws IloException {
	//create a separate set of edge flow variables for each chain root
		Map<V,IntegerFlowNetwork<V,E>> flowNetworksByRoot = new HashMap<V,IntegerFlowNetwork<V,E>>();
		for(V root: kepInstance.getRootNodes()){
			KNeighborhoodFilter<V,E> filter = new  KNeighborhoodFilter<V,E>(root,kepInstance.getMaxChainLength(),KNeighborhoodFilter.EdgeType.OUT);				
			DirectedSparseMultigraph<V,E> subgraph = (DirectedSparseMultigraph<V,E>)filter.transform(kepInstance.getGraph());
			ImmutableSet<V> subgraphNodes = ImmutableSet.copyOf(subgraph.getVertices());
			ImmutableSet<V> rootSet = ImmutableSet.of(root);
			ImmutableSet<V> pairedFromRoot = Sets.intersection(subgraphNodes,kepInstance.getPairedNodes()).immutableCopy();
			ImmutableSet<V> terminalFromRoot = Sets.intersection(subgraphNodes,kepInstance.getTerminalNodes()).immutableCopy();
			if(subgraph.getEdgeCount()>0){
				flowNetworksByRoot.put(root, new IntegerFlowNetwork<V,E>(subgraph,rootSet, 
						pairedFromRoot, terminalFromRoot, cplex, solverOptions.contains(SolverOption.expandedFormulation)));
				DirectedEdgeVariables<V,E> edgesUsedByRoot = flowNetworksByRoot.get(root).getEdgeVariables();
				//use at most max chain length of these variables.
				cplex.addLe(edgesUsedByRoot.integerSum(edgesUsedByRoot), kepInstance.getMaxChainLength());
			}				
		}
		
		DirectedEdgeVariables<V, E> cyclesOnlyEdgeVariables = null;
		if (!allFlowIsFromChains) {
			cyclesOnlyEdgeVariables = cyclesOnlyEdgeVariables(kepInstance,
					cplex, solverOptions);
		}
		
		//the total edge flow is equal to the sum of the edge flows initiated at each root
		for(E edge: kepInstance.getGraph().getEdges()){
			IloLinearIntExpr edgeUsage = cplex.linearIntExpr();
			for(V root: flowNetworksByRoot.keySet()){
				DirectedEdgeVariables<V,E> edgeVarsForRoot = flowNetworksByRoot.get(root).getEdgeVariables();
				if(edgeVarsForRoot.contains(edge)){
					edgeUsage.addTerm(edgeVarsForRoot.get(edge), 1);
				}
			}
			if(!allFlowIsFromChains){			
				edgeUsage.addTerm(cyclesOnlyEdgeVariables.get(edge),1);
			}
			cplex.addEq(edgeUsage, edgeVariables.get(edge));			
		}
	}
	
	// TODO(rander): this was hacked out of Integer flow network. Maybe refactor
	// integer flow network so the flow into and out of each kind of node can be
	// parameterized, so this can be eliminated.
	private static <V, E> DirectedEdgeVariables<V, E> cyclesOnlyEdgeVariables(
			KepInstance<V, E> kepInstance, IloCplex cplex,
			ImmutableSet<SolverOption> solverOptions) {
		try {
			boolean expandedFormulation = solverOptions
					.contains(SolverOption.expandedFormulation);
			DirectedEdgeVariables<V, E> edgeVars = new DirectedEdgeVariables<V, E>(
					kepInstance.getGraph(), cplex);
			FlowInterface<V, E> flowInterface;
			if (expandedFormulation) {
				flowInterface = new FlowVariables<V, E>(kepInstance.getGraph(),
						kepInstance.getRootNodes(),
						kepInstance.getPairedNodes(),
						kepInstance.getTerminalNodes(), edgeVars, cplex);
			} else {
				flowInterface = new FlowVariablesCompact<V, E>(
						kepInstance.getGraph(), edgeVars, cplex);
			}
			for (V vertex : kepInstance.getRootNodes()) {
				cplex.addEq(flowInterface.flowOutIntScaled(vertex, 1), 0);
			}
			for (V vertex : kepInstance.getPairedNodes()) {
				IloLinearIntExpr flowIn = flowInterface.flowInIntScaled(vertex,
						1);
				IloLinearIntExpr flowOut = flowInterface.flowOutIntScaled(
						vertex, 1);
				cplex.addEq(flowOut, flowIn);
				if (!expandedFormulation) {
					cplex.addLe(flowIn, 1);
				}
			}
			for (V vertex : kepInstance.getTerminalNodes()) {
				cplex.addEq(flowInterface.flowInIntScaled(vertex, 1), 0);
			}
			return edgeVars;
		} catch (IloException e) {
			throw new RuntimeException(e);
		}

	}

}
