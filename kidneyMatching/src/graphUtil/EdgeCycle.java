package graphUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;



public class EdgeCycle<E> extends OrderedEdgeSet<E>{
	
	
	public EdgeCycle(List<E> edgesInOrder) {
		super(edgesInOrder);
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof EdgeCycle){
			return ((EdgeCycle<?>)other).getEdges().equals(this.getEdges());
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return this.getEdges().hashCode();
	}
	
	/**
	 * The class asssumes that cycleOne.equals(cycleTwo) if and only if cycleOne.getEdges().equals(cycleTwo.getEdges()).  Here,
	 * we make the additional test that the orderings of cycleOne.getEdgesInOrder() and cycleTwo.getEdgesInOrder() are consistent
	 * @param cycleOne
	 * @param cycleTwo
	 * @return
	 */
	public static <E> void validateEqual(EdgeCycle<E> cycleOne, EdgeCycle<E> cycleTwo){
		if(!cycleOne.equals(cycleTwo)){
			throw new RuntimeException("Two cycles do not satisfy .equal(), i.e. they do not contain the same edge sets");
		}
		if(cycleOne.getEdges().size() != cycleOne.getEdgesInOrder().size()){
			throw new RuntimeException("set view: " + cycleOne.getEdges() + " of size: " + cycleOne.getEdges().size() 
					+ ", but list view: " + cycleOne.getEdgesInOrder().size() + " of size: " + cycleOne.getEdgesInOrder().size());
		}
		if(cycleTwo.getEdges().size() != cycleTwo.getEdgesInOrder().size()){
			throw new RuntimeException("set view: " + cycleTwo.getEdges() + " of size: " + cycleTwo.getEdges().size() 
					+ ", but list view: " + cycleTwo.getEdgesInOrder().size() + " of size: " + cycleTwo.getEdgesInOrder().size());
		}
		if(!cycleOne.getEdges().equals(new HashSet<E>(cycleOne.getEdgesInOrder()))){
			throw new RuntimeException("Cycle one set view: "+  cycleOne.getEdges().toString() +
					" is not equal to cyle one list view contents: " + cycleOne.getEdgesInOrder().toString());
		}
		if(!cycleTwo.getEdges().equals(new HashSet<E>(cycleTwo.getEdgesInOrder()))){
			throw new RuntimeException("Cycle one set view: "+  cycleTwo.getEdges().toString() +
					" is not equal to cyle one list view: " + cycleTwo.getEdgesInOrder().toString());
		}		
		if(cycleOne.size() == 0 ){
			return;
		}
		E first = cycleOne.getEdgesInOrder().get(0);
		int offset = cycleTwo.getEdgesInOrder().indexOf(first);
		if(offset < 0){
			throw new RuntimeException("could not find first element of cycle one: " + first + ", not present in cycleTwo: " + cycleTwo.toString());
		}
		int n = cycleOne.getEdgesInOrder().size();
		for(int i = 0; i < n ; i++){
			if(!cycleOne.getEdgesInOrder().get(i).equals(cycleTwo.getEdgesInOrder().get((i + offset)%n))){
				throw new RuntimeException("The " + i + "th element of cycle one did not match the " 
			+ ((i + offset)%n) + " element of cycle two.  Cycle One: " 
						+ cycleOne.getEdgesInOrder() + ", Cycle two: " + cycleTwo.getEdgesInOrder());
			}
		}
	}
}
