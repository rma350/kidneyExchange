package graphUtil;

import static org.junit.Assert.*;

import graphUtil.EdgeCycle;

import java.util.Arrays;


import org.junit.Test;

public class EdgeCycleTest {
	
	
	public EdgeCycle<String> makeCycleOne(){
		return new EdgeCycle<String>(Arrays.asList("a","b","c","d"));
	}
	
	public EdgeCycle<String> makeCycleTwo(){
		return new EdgeCycle<String>(Arrays.asList("c","d","a","b"));
	}
	
	public EdgeCycle<String> makeCycleThree(){
		return new EdgeCycle<String>(Arrays.asList("a","b","c"));
	}
	
	
	@Test
	public void testEqual(){
		EdgeCycle<String> cycleOne = makeCycleOne();
		EdgeCycle<String> cycleTwo = makeCycleTwo();
		assertEquals(cycleOne,cycleOne);
		assertEquals(cycleOne,cycleTwo);
		assertFalse(cycleOne.equals(makeCycleThree()));
	}
	
	

	@Test(expected=RuntimeException.class)
	public void testValidateEqualsNotEqual() {
		EdgeCycle<String> cycleOne = makeCycleOne();
		EdgeCycle<String> cycleThree = makeCycleThree();
		EdgeCycle.validateEqual(cycleOne, cycleThree);
	}
	
	@Test(expected=RuntimeException.class)
	public void testValidateEqualsCycleOneSize() {
		EdgeCycle<String> cycleOne = makeCycleOne();
		cycleOne.getEdgesInOrder().add("a");
		EdgeCycle<String> cycleTwo = makeCycleTwo();
		EdgeCycle.validateEqual(cycleOne, cycleTwo);
	}
	
	@Test(expected=RuntimeException.class)
	public void testValidateEqualsCycleTwoSize() {
		EdgeCycle<String> cycleOne = makeCycleOne();		
		EdgeCycle<String> cycleTwo = makeCycleTwo();
		cycleOne.getEdgesInOrder().add("e");
		EdgeCycle.validateEqual(cycleOne, cycleTwo);
	}
	
	@Test(expected=RuntimeException.class)
	public void testValidateEqualsCycleOneEdgesEqual() {
		EdgeCycle<String> cycleOne = makeCycleOne();
		cycleOne.getEdgesInOrder().remove(3);
		cycleOne.getEdgesInOrder().add("a");
		EdgeCycle<String> cycleTwo = makeCycleTwo();
		EdgeCycle.validateEqual(cycleOne, cycleTwo);
	}
	
	@Test(expected=RuntimeException.class)
	public void testValidateEqualsCycleTwoEdgesEqual() {
		EdgeCycle<String> cycleOne = makeCycleOne();
		EdgeCycle<String> cycleTwo = makeCycleTwo();
		cycleTwo.getEdgesInOrder().remove(2);
		cycleTwo.getEdgesInOrder().add("e");
		EdgeCycle.validateEqual(cycleOne, cycleTwo);
	}
	
	@Test(expected=RuntimeException.class)
	public void testValidateEqualsOrdering() {
		EdgeCycle<String> cycleOne = makeCycleOne();
		EdgeCycle<String> cycleTwo = makeCycleTwo();
		cycleTwo.getEdgesInOrder().clear();
		cycleTwo.getEdgesInOrder().add("d");
		cycleTwo.getEdgesInOrder().add("c");
		cycleTwo.getEdgesInOrder().add("b");
		cycleTwo.getEdgesInOrder().add("a");
		EdgeCycle.validateEqual(cycleOne, cycleTwo);
	}
	

}
