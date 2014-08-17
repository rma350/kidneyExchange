package kepLib;

import java.util.HashSet;
import java.util.Set;

import statistics.Queries.KepProblemDataInterface;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class KepProblemData<V,E> implements KepProblemDataInterface<V,E> {
	
	private DirectedSparseMultigraph<V,E> graph;
	private ImmutableSet<V> rootNodes;
	private ImmutableSet<V> pairedNodes;
	private ImmutableSet<V> terminalNodes;
	
	public KepProblemData(DirectedSparseMultigraph<V, E> graph,
			Set<V> rootNodes, Set<V> pairedNodes,
			Set<V> terminalNodes){
		this.graph = graph;
		this.rootNodes = ImmutableSet.copyOf(rootNodes);
		this.pairedNodes = ImmutableSet.copyOf(pairedNodes);
		this.terminalNodes = ImmutableSet.copyOf(terminalNodes);
		validateInput();
	}
	
	public Sets.SetView<V> nonRootNodes(){
		return Sets.union(terminalNodes, pairedNodes);
	}
	
	public Sets.SetView<V> nonTerminalNodes(){
		return Sets.union(rootNodes,pairedNodes);
	}
	
	public DirectedSparseMultigraph<V, E> getGraph() {
		return graph;
	}



	public ImmutableSet<V> getRootNodes() {
		return rootNodes;
	}



	public ImmutableSet<V> getPairedNodes() {
		return pairedNodes;
	}



	public ImmutableSet<V> getTerminalNodes() {
		return terminalNodes;
	}



	private void validateInput(){
		{
			Set<V> rootPaired = Sets.intersection(rootNodes, pairedNodes);
			if(rootPaired.size()!= 0){
				throw new RuntimeException(rootPaired.size() + " duplicate elements found in root nodes and paired nodes: " + rootPaired);
			}
		}
		{
			Set<V> rootTerminal = Sets.intersection(rootNodes, terminalNodes);
			if(rootTerminal.size()!= 0){
				throw new RuntimeException(rootTerminal.size() + " duplicate elements found in root nodes and terminal nodes: " + rootTerminal);
			}
		}
		{
			Set<V> terminalPaired = Sets.intersection(terminalNodes, pairedNodes);
			if(terminalPaired.size()!= 0){
				throw new RuntimeException(terminalPaired.size() + " duplicate elements found in terminal nodes and paired nodes: " + terminalPaired);
			}
		}
		{
			Set<V> graphNodes = new HashSet<V>(graph.getVertices());
			SetView<V> all = Sets.union(Sets.union(rootNodes, terminalNodes),pairedNodes);
			SetView<V> graphNodesButNotAll = Sets.difference(graphNodes, all);
			if(graphNodesButNotAll.size() != 0 ){
				throw new RuntimeException(graphNodesButNotAll.size() + " found in graph but not in root, terminal, or paired: " + graphNodesButNotAll);
			}
			SetView<V> allButNotGraphNodes = Sets.difference(all,graphNodes);
			if(allButNotGraphNodes.size() != 0 ){
				throw new RuntimeException(allButNotGraphNodes.size() + " found in root, terminal, or paired but not graph: " + allButNotGraphNodes);
			}
		
		}
	}

}
