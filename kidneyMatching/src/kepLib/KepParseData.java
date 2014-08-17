package kepLib;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Functions;

public class KepParseData<V,E> {
	public KepInstance<V, E> instance;
	public Map<String, V> nodeNames;
	public Map<String, E> edgeNames;

	public KepParseData(KepInstance<V, E> instance,
			Map<String, V> nodeNames, Map<String, E> edgeNames) {
		super();
		this.instance = instance;
		this.nodeNames = nodeNames;
		this.edgeNames = edgeNames;
	}
	public KepInstance<V, E> getInstance() {
		return this.instance;
	}
	public Map<String, V> getNodeNames() {
		return this.nodeNames;
	}
	public Map<String, E> getEdgeNames() {
		return this.edgeNames;
	}	
	
	public static final Function<Object,String> toStringFunction = new Function<Object,String>(){
		@Override
		public String apply(Object input){
			return input.toString();
		}
	};
	
	public static <V,E> Function<V,String> anonymousNodeNames(KepInstance<V,E> instance){
		Map<V,String> vertexName = new HashMap<V,String>();
		int rootIndex = 1;
		for(V vertex: instance.getRootNodes()){
			vertexName.put(vertex, "r" + Integer.toString(rootIndex++));
		}
		int pairedIndex = 1;
		for(V vertex: instance.getPairedNodes()){
			vertexName.put(vertex, "p" + Integer.toString(pairedIndex++));
		}
		int terminalIndex = 1;
		for(V vertex: instance.getTerminalNodes()){
			vertexName.put(vertex, "t" + Integer.toString(terminalIndex++));
		}		
		return Functions.forMap(vertexName);
	}
	
	public static <V,E> Function<E,String> anonymousEdgeNames(KepInstance<V,E> instance){
		Map<E,String> edgeName = new HashMap<E,String>();
		int edgeIndex = 1;
		for(E edge : instance.getGraph().getEdges()){
			edgeName.put(edge, "e"+Integer.toString(edgeIndex++));
		}
		return Functions.forMap(edgeName);
	}
}