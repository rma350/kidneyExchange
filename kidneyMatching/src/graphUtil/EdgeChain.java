package graphUtil;


import java.util.List;


public class EdgeChain<E> extends OrderedEdgeSet<E>{	
	public EdgeChain(List<E> edgesInOrder) {
		super(edgesInOrder);
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof EdgeChain){
			return ((EdgeChain<?>)other).getEdgesInOrder().equals(this.getEdgesInOrder());
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return this.getEdgesInOrder().hashCode();
	}
}
