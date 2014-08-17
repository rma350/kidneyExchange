package exchangeGraph;

import graphUtil.CycleChainDecomposition;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.Status;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import data.ExchangeUnit;

import kepLib.KepInstance;
import kepModeler.ChainsForcedRemainOpenOptions.AtLeastCountChains;
import kepModeler.ChainsForcedRemainOpenOptions.AtLeastCountOfQualityChains;
import kepModeler.ChainsForcedRemainOpenOptions.OptionName;
import kepModeler.ObjectiveMode.MaximumCardinalityMode;
import kepModeler.ObjectiveMode.MaximumWeightedPackingMode;
import kepModeler.ObjectiveMode.VpraWeightObjectiveMode;

import ui.DemoFrame;





import edu.uci.ics.jung.graph.DirectedSparseMultigraph;



public abstract class CycleChainPackingIp<V,E> {
		
	protected final KepInstance<V,E> kepInstance;
	
	protected final Optional<Double> maxTimeSeconds;	
	
	protected CycleChainDecomposition<V,E> solution;
	
	public KepInstance<V,E> getKepInstance(){
		return this.kepInstance;
	}
	
	protected CycleChainPackingIp(KepInstance<V,E> kepInstance, boolean displayOutput, Optional<Double> maxTimeSeconds){
		this.kepInstance = kepInstance;
		this.maxTimeSeconds = maxTimeSeconds;		
		if(displayOutput){
			System.out.println("Chain Roots: " + kepInstance.getRootNodes().size());
			System.out.println("Terminal Verticies: " +kepInstance.getTerminalNodes().size());
			System.out.println("Paired Nodes: " + kepInstance.getPairedNodes().size());
		}
	}
	
	public abstract double getSolveTimeSeconds();
		
	public abstract void solve();
	protected abstract CycleChainDecomposition<V,E> recoverSolution();
	public abstract void cleanUp();
	
	public CycleChainDecomposition<V,E> getSolution(){
		return this.solution;
		
	}
}
