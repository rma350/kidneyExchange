package exchangeGraph;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import graphUtil.CycleChainDecomposition;
import graphUtil.Edge;
import graphUtil.Node;
import graphUtil.TestGraph;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.Status;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kepLib.KepInstance;
import kepLib.KepParseData;
import kepLib.KepTextReaderWriter;
import kepModeler.ChainsForcedRemainOpenOptions;
import kepModeler.ObjectiveMode;
import kepModeler.ObjectiveMode.MaximumCardinalityMode;


import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;


import threading.FixedThreadPool;



import exchangeGraph.CycleChainPackingIp;

import exchangeGraph.CycleChainPackingSubtourElimination;

@RunWith(Theories.class)
public class CyclePackingIpTest {
	
	private static double tolerance = .00001;
	
	public static final String unitTestDir = "unitTestData"+File.separator;
	public static final String inputDir = unitTestDir + "solverIn" + File.separator;
	public static final String expectedOutputDir = unitTestDir + "solverOut" + File.separator;
	public static final String inputFileNameBase = "e";
	public static final String outputFileNameBase = "sol";
	public static final String suffix = ".csv";
	
	public static final int numTests = 10;
	
	public static int[] oneToN(int n){
		int[] ans = new int[n];
		for(int j = 0; j < n; j++){
			ans[j] = j+1;
		}
		return ans;
	}
	
	@AfterClass
	public static void cleanUp(){
		FixedThreadPool.shutDown(singleThreaded);
		FixedThreadPool.shutDown(multiThreaded);
	}
	
	@DataPoints
	public static int[] tests = oneToN(numTests);
	
	@DataPoint
	public static Optional<FixedThreadPool> singleThreaded = FixedThreadPool.makePool(1);
	
	@DataPoint
	public static Optional<FixedThreadPool> multiThreaded = FixedThreadPool.makePool(2);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o1 = SolverOption.makeCheckedOptions(SolverOption.cutsetMode,
			SolverOption.expandedFormulation,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o2 = SolverOption.makeCheckedOptions(SolverOption.cycleMode,
			SolverOption.expandedFormulation,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o3 = SolverOption.makeCheckedOptions(SolverOption.subsetMode,
			SolverOption.expandedFormulation,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o4 = SolverOption.makeCheckedOptions(SolverOption.edgeMode,
			SolverOption.expandedFormulation,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o5 = SolverOption.makeCheckedOptions(SolverOption.cutsetMode,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o6 = SolverOption.makeCheckedOptions(SolverOption.cycleMode,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o7 = SolverOption.makeCheckedOptions(SolverOption.subsetMode,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	@DataPoint
	public static ImmutableSet<SolverOption> o8 = SolverOption.makeCheckedOptions(SolverOption.edgeMode,
			SolverOption.lazyConstraintCallback,
			SolverOption.userCutCallback);
	
	
	
	
	
	
	static class InputOutput{
		private KepInstance<Node,Edge> kepInstance;
		private CycleChainDecomposition<Node,Edge> solution;
		public InputOutput(KepInstance<Node, Edge> kepInstance,
				CycleChainDecomposition<Node, Edge> solution) {
			super();
			this.kepInstance = kepInstance;
			this.solution = solution;
		}
		public KepInstance<Node, Edge> getKepInstance() {
			return kepInstance;
		}
		public CycleChainDecomposition<Node, Edge> getSolution() {
			return solution;
		}
	}
	
	
	
	public static InputOutput readInputOutput(int trialNum) throws IOException{
		if(trialNum < 1 || trialNum > numTests){
			throw new RuntimeException("Trial number must lie in [1," + trialNum+ "] but was " + trialNum);
		}
		String inputName = inputDir + inputFileNameBase + trialNum + suffix;
		KepParseData<Node,Edge> parseData = KepTextReaderWriter.INSTANCE.readParseData(inputName);
		String outputName = expectedOutputDir + outputFileNameBase + trialNum + suffix;
		CycleChainDecomposition<Node,Edge> output = KepTextReaderWriter.INSTANCE.readSolution(parseData, outputName);
		return new InputOutput(parseData.getInstance(),output);
	}

	
	@Theory
	public void testCutsetSingleThreaded(int trial, Optional<FixedThreadPool> threadPool, ImmutableSet<SolverOption> solverOptions) throws IOException, IloException{
		String identification ="Trial " + trial + ", Threads " + (threadPool.isPresent() ? threadPool.get().getNumThreads() : 1)  + ", Solver Options:" + solverOptions; 
		System.out.println(identification);
		//Edge mode does not support adding additional weight to cycles, the answer in trial 9 requires the additional cycle weight.
		assumeTrue(!solverOptions.contains(SolverOption.edgeMode) || trial != 9);
		InputOutput data = readInputOutput(trial);
		CycleChainPackingSubtourElimination<Node,Edge> solver= new CycleChainPackingSubtourElimination<Node,Edge>(
							data.getKepInstance(), false,  Optional.<Double>absent(), 
							threadPool, solverOptions);
		solver.solve();
		solver.cleanUp();		
		assertEquals(identification,data.getSolution(),solver.getSolution());
	}
}
