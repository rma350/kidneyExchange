package exchangeGraph.minWaitingTime;
import java.util.Map;

import kepLib.KepInstance;

import org.apache.commons.collections15.TransformerUtils;

import threading.FixedThreadPool;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import allPairsShortestPath.ParallelFloydWarshallJung;
import exchangeGraph.minWaitingTime.MinWaitingTimeKepSolver.PreProcessor;

public class APSPPreProcessor<V,E>  implements PreProcessor<V>{
	
	private ParallelFloydWarshallJung<V, E> allPairsShortestPath;
	private Map<V,Double> minDistToAnyRoot;
	private KepInstance<V,E> kepInstance;
	
	
	
	public APSPPreProcessor(KepInstance<V, E> kepInstance,
			Optional<FixedThreadPool> threadPool) {
		this.kepInstance = kepInstance;
		System.out.println("Starting all pairs shortest path");
		long time = System.currentTimeMillis();
		allPairsShortestPath = new ParallelFloydWarshallJung<V, E>(
				kepInstance.getGraph(),
				TransformerUtils.<Integer> constantTransformer(Integer
						.valueOf(1)), threadPool);
		allPairsShortestPath.solve();
		long elapsed = System.currentTimeMillis() - time;
		System.out
				.println("Finished all pairs shortest path, elapsed time ms: "
						+ elapsed);
		minDistToAnyRoot = Maps.newHashMap();
		for (V vertex : kepInstance.getGraph().getVertices()) {

			if (kepInstance.getRootNodes().contains(vertex)) {
				minDistToAnyRoot.put(vertex, 0.0);
			} else {
				double bestDist = Double.POSITIVE_INFINITY;
				for (V root : kepInstance.getRootNodes()) {
					bestDist = Math.min(bestDist, allPairsShortestPath
							.getShortestPathLength(root, vertex));
				}
				minDistToAnyRoot.put(vertex, bestDist);
			}
		}
	}

	@Override
	public boolean createVariable(V first, V second) {
		double minDistFromAnyRoot = this.minDistToAnyRoot.get(first);
		if (minDistFromAnyRoot == Double.POSITIVE_INFINITY) {
			return false;
		}
		if (minDistFromAnyRoot > kepInstance.getMaxChainLength()) {
			return false;
		}
		double lengthFirstToSecond = allPairsShortestPath
				.getShortestPathLength(first, second);
		if (lengthFirstToSecond == Double.POSITIVE_INFINITY) {
			return false;
		}
		if (minDistFromAnyRoot + lengthFirstToSecond > kepInstance
				.getMaxChainLength()) {
			return false;
		}
		return true;
	}

}
