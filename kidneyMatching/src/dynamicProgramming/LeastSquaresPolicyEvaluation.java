package dynamicProgramming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.RealVectorFormat;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class LeastSquaresPolicyEvaluation {
	
	//private List<Feature<T>> features;
	
	private static RealVectorFormat vectorFormat = new RealVectorFormat();
	
	private double regularizer = .00001;
	
	private RealVector rOld;
	//private RealVector rInterOld;
	private RealVector r;
	//private RealVector rInter;
	
	private int k;
	
	private List<RealVector> phis;
	private RealVector costs;
	
	private double alpha;
	private int dim;
	
	public LeastSquaresPolicyEvaluation(RealVector rInit, RealVector phiInit, int dim, double alpha){
		if(phiInit.getDimension() != dim){
			throw new RuntimeException();
		}
		if(rInit.getDimension() != dim+1){
			throw new RuntimeException();
		}
		this.r = rInit;
		this.k = 0;
		this.phis = new ArrayList<RealVector>();
		phis.add(phiInit);
		this.alpha = alpha;
		this.rOld = null;
		this.costs = new ArrayRealVector();
		this.dim = dim;
		if(rInit.getDimension() != dim+1){
			throw new RuntimeException("Diminsion of r was " + rInit.getDimension() 
					+ " and dimension of problem was " + dim + " but r must have dimension one greater than dim");
		}
		//this.rInterOld = null;
		//this.rInter = rInterceptInit;
	}
	
	public LeastSquaresPolicyEvaluation(RealVector phiInit, int dim, double alpha){
		this(new ArrayRealVector(dim+1),phiInit,dim,alpha);
	}
	
	public void iterate(RealVector phiNext, double cost){
		
		double[][] phiDataZero = new double[phis.size()+dim+1][this.dim];
		for(int i = 0; i < dim; i++){
			phiDataZero[i+1][i] = regularizer;
		}
		for(int i = 0; i < phis.size(); i++){
			System.arraycopy(phis.get(i).toArray(), 0, phiDataZero[i+dim+1], 0, dim);
		}
		Array2DRowRealMatrix x = new Array2DRowRealMatrix(phiDataZero,false);
		
		
		phis.add(phiNext);
		this.costs = costs.append(cost);
		double[][] phiDataOne = new double[phis.size()-1][this.dim+1];
		for(int i = 0; i < phis.size()-1; i++){
			phiDataOne[i][0] =  1;
			System.arraycopy(phis.get(i+1).toArray(), 0, phiDataOne[i], 1, dim);
		}
		Array2DRowRealMatrix phiOne = new Array2DRowRealMatrix(phiDataOne,false);
		
		
		
		//System.out.println(costs.getDimension());
		//System.out.println(phiOne.getRowDimension());
		RealVector y = costs.add( phiOne.operate(this.r).mapMultiplyToSelf(alpha) );
		RealVector yRegularize = new ArrayRealVector(dim+1);
		y = yRegularize.append(y);
		
		//if(k+1 > dim+1){
			//System.out.println(k);
			OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();

			regression.newSampleData(y.toArray(), x.getDataRef());
			double[] result = regression.estimateRegressionParameters();
			/*System.out.println("costs:" + vectorFormat.format(costs));
			for(int i = 0; i < phis.size(); i++){
				System.out.println(i + ": " + vectorFormat.format(phis.get(i)));
			}
			System.out.println("y:" + vectorFormat.format(y));
			System.out.println(Arrays.toString(result));*/
			

			rOld = r;

			r = new ArrayRealVector(result);
		//}
		k++;
	}

	public RealVector getrOld() {
		return rOld;
	}

	public RealVector getR() {
		return r;
	}

	public int getK() {
		return k;
	}

	public double getAlpha() {
		return alpha;
	}

	public int getDim() {
		return dim;
	}
	
	
	
	
	

}
