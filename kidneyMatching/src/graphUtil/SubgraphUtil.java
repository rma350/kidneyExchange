package graphUtil;

import java.util.Set;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class SubgraphUtil {
	
	public static <V,E> DirectedSparseMultigraph<V,E> subgraph(DirectedSparseMultigraph<V,E> graph, 
			Set<V> subgraphNodes, Set<E> subgraphEdges){
		DirectedSparseMultigraph<V,E> subgraph = new DirectedSparseMultigraph<V,E>();
		for(V node: subgraphNodes){
			subgraph.addVertex(node);
		}
		if(subgraphEdges == null){
			for(E edge: graph.getEdges()){
				V source = graph.getSource(edge);
				V dest =  graph.getDest(edge);
				if(subgraphNodes.contains(source) &&
						subgraphNodes.contains(dest)){
					subgraph.addEdge(edge, source,dest);
				}
			}
		}
		else{
			for(E edge: subgraphEdges){
				subgraph.addEdge(edge,graph.getSource(edge) , graph.getDest(edge));
			}
		}
		return subgraph;
	}

}
