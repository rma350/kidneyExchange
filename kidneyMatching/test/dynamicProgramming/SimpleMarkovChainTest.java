package dynamicProgramming;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import dynamicProgramming.LeastSquaresPolicyEvaluation;

public class SimpleMarkovChainTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int state = 0;
		RealVector phiInit = new ArrayRealVector(new double[]{state});
		double alpha = .9;
		LeastSquaresPolicyEvaluation lspe = new LeastSquaresPolicyEvaluation(phiInit,1,alpha);
		for(int i = 0; i < 1000; i++){
			double cost = state;
			int newState = Math.random() > .5 ?  1 : 0;
			
			RealVector phiNext = new ArrayRealVector(new double[]{newState});
			lspe.iterate(phiNext, cost);
			state = newState;
		}
		double valueOne = 1 + alpha/(2*(1-alpha));
		double valueZero = alpha/(2*(1-alpha));
		
		ArrayRealVector calcOne = new ArrayRealVector(new double[]{1,1});
		ArrayRealVector calcZero = new ArrayRealVector(new double[]{1,0});
		
		
		double estimateOne = calcOne.dotProduct(lspe.getR());
		double estimateZero = calcZero.dotProduct(lspe.getR());
		
		System.out.println("True for state one: " + valueOne + ", estimate: " + estimateOne);
		System.out.println("True for state zero: " + valueZero + ", estimate: " + estimateZero);

	}

}
