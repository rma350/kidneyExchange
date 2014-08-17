package graphUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public abstract class OrderedEdgeSet<E> implements Iterable<E> {

	private ImmutableList<E> edgesInOrder;
	private ImmutableSet<E> edges;
	
	public ImmutableSet<E> getEdges() {
		return edges;
	}
	public ImmutableList<E> getEdgesInOrder(){
		return this.edgesInOrder;
	}
	
	protected OrderedEdgeSet(List<E> edgesInOrder) {
		super();
		this.edgesInOrder = ImmutableList.copyOf(edgesInOrder);
		this.edges = ImmutableSet.copyOf(this.edgesInOrder);
		if(edges.size() != this.edgesInOrder.size()){
			throw new RuntimeException("duplicate elements in input: " + edgesInOrder.toString());
		}
	}
	
	
	
	@Override
	public String toString(){
		return edgesInOrder.toString();
	}
	
	public int size(){
		return this.edges.size();
	}
	
	@Override
	public Iterator<E> iterator(){
		return this.edgesInOrder.iterator();
	}


}
