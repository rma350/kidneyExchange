package allPairsShortestPath;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import threading.FixedThreadPool;

import com.google.common.base.Optional;

import allPairsShortestPath.ParallelFloydWarshall;




public class ParallelFloydWarshallTest {
	
	private static class TestGraph{
		public final Integer a = 0;
		public final Integer b = 1;
		public final Integer c = 2;
		public final Integer d = 3;
		public final Integer e = 4;
		public final Integer f = 5;
		public final Integer g = 6;
		public final int numNodes = 7;
		
		public final double[][] costs;
		
		public TestGraph(){
			costs = new double[numNodes][];
			costs[a] = new double[]{inf,4,inf,1,3,inf,inf};
			costs[b] = new double[]{2,inf,1,inf,inf,inf,inf};
			costs[c] = new double[]{inf,inf,inf,inf,5,inf,inf};
			costs[d] = new double[]{inf,2,2,inf,inf,inf,inf};
			costs[e] = new double[]{inf,inf,inf,2,inf,inf,inf};
			costs[f] = new double[]{inf,inf,inf,inf,inf,inf,1};
			costs[g] = new double[]{inf,inf,inf,inf,inf,inf,inf};
		}
		
	}
	private static double inf = Double.POSITIVE_INFINITY;
	private static double eps = .00000001; 

	@Test
	public void test() {
		TestGraph graph = new TestGraph();
		int numThreads = 3;
		Optional<FixedThreadPool> threadPool = FixedThreadPool.makePool(numThreads);
		ParallelFloydWarshall apspParallel = new ParallelFloydWarshall(graph.numNodes, graph.costs, threadPool);
		ParallelFloydWarshall apspSingle = new ParallelFloydWarshall(graph.numNodes, graph.costs);
		List<ParallelFloydWarshall> apsps = new ArrayList<ParallelFloydWarshall>();
		apsps.add(apspParallel);
		apsps.add(apspSingle);
		for(ParallelFloydWarshall apsp: apsps){
			apsp.solve();
			assertEquals(5,apsp.shorestPathLength(graph.a, graph.a),eps);
			assertEquals(Arrays.asList(graph.a,graph.d,graph.b,graph.a), apsp.shortestPath(graph.a, graph.a));
			assertEquals(3,apsp.shorestPathLength(graph.a, graph.b),eps);
			assertEquals(Arrays.asList(graph.a,graph.d,graph.b), apsp.shortestPath(graph.a, graph.b));
			assertEquals(3,apsp.shorestPathLength(graph.a, graph.c),eps);
			assertEquals(Arrays.asList(graph.a,graph.d,graph.c), apsp.shortestPath(graph.a, graph.c));
			assertEquals(1,apsp.shorestPathLength(graph.a, graph.d),eps);
			assertEquals(Arrays.asList(graph.a,graph.d), apsp.shortestPath(graph.a, graph.d));
			assertEquals(3,apsp.shorestPathLength(graph.a, graph.e),eps);
			assertEquals(Arrays.asList(graph.a,graph.e), apsp.shortestPath(graph.a, graph.e));
			assertEquals(inf,apsp.shorestPathLength(graph.a, graph.f),eps);
			assertNull(apsp.shortestPath(graph.a, graph.f));
			assertEquals(inf,apsp.shorestPathLength(graph.a, graph.g),eps);
			assertNull(apsp.shortestPath(graph.a, graph.g));

			assertEquals(2,apsp.shorestPathLength(graph.b, graph.a),eps);
			assertEquals(Arrays.asList(graph.b,graph.a), apsp.shortestPath(graph.b, graph.a));
			assertEquals(5,apsp.shorestPathLength(graph.b, graph.b),eps);
			assertEquals(Arrays.asList(graph.b,graph.a,graph.d,graph.b), apsp.shortestPath(graph.b, graph.b));
			assertEquals(1,apsp.shorestPathLength(graph.b, graph.c),eps);
			assertEquals(Arrays.asList(graph.b,graph.c), apsp.shortestPath(graph.b, graph.c));
			assertEquals(3,apsp.shorestPathLength(graph.b, graph.d),eps);
			assertEquals(Arrays.asList(graph.b,graph.a,graph.d), apsp.shortestPath(graph.b, graph.d));
			assertEquals(5,apsp.shorestPathLength(graph.b, graph.e),eps);
			assertEquals(Arrays.asList(graph.b,graph.a,graph.e), apsp.shortestPath(graph.b, graph.e));
			assertEquals(inf,apsp.shorestPathLength(graph.b, graph.f),eps);
			assertNull(apsp.shortestPath(graph.b, graph.f));
			assertEquals(inf,apsp.shorestPathLength(graph.b, graph.g),eps);
			assertNull(apsp.shortestPath(graph.b, graph.g));

			//TODO paths starting at c,d,e




			assertEquals(inf,apsp.shorestPathLength(graph.f, graph.a),eps);
			assertNull(apsp.shortestPath(graph.f, graph.a));
			assertEquals(inf,apsp.shorestPathLength(graph.f, graph.b),eps);
			assertNull(apsp.shortestPath(graph.f, graph.b));
			assertEquals(inf,apsp.shorestPathLength(graph.f, graph.c),eps);
			assertNull(apsp.shortestPath(graph.f, graph.c));
			assertEquals(inf,apsp.shorestPathLength(graph.f, graph.d),eps);
			assertNull(apsp.shortestPath(graph.f, graph.d));
			assertEquals(inf,apsp.shorestPathLength(graph.f, graph.e),eps);
			assertNull(apsp.shortestPath(graph.f, graph.e));
			assertEquals(inf,apsp.shorestPathLength(graph.f, graph.f),eps);
			assertNull(apsp.shortestPath(graph.f, graph.f));
			assertEquals(1,apsp.shorestPathLength(graph.f, graph.g),eps);
			assertEquals(Arrays.asList(graph.f,graph.g),apsp.shortestPath(graph.f, graph.g));

			assertEquals(inf,apsp.shorestPathLength(graph.g, graph.a),eps);
			assertNull(apsp.shortestPath(graph.g, graph.a));
			assertEquals(inf,apsp.shorestPathLength(graph.g, graph.b),eps);
			assertNull(apsp.shortestPath(graph.g, graph.b));
			assertEquals(inf,apsp.shorestPathLength(graph.g, graph.c),eps);
			assertNull(apsp.shortestPath(graph.g, graph.c));
			assertEquals(inf,apsp.shorestPathLength(graph.g, graph.d),eps);
			assertNull(apsp.shortestPath(graph.g, graph.d));
			assertEquals(inf,apsp.shorestPathLength(graph.g, graph.e),eps);
			assertNull(apsp.shortestPath(graph.g, graph.e));
			assertEquals(inf,apsp.shorestPathLength(graph.g, graph.f),eps);
			assertNull(apsp.shortestPath(graph.g, graph.f));
			assertEquals(inf,apsp.shorestPathLength(graph.g, graph.g),eps);
			assertNull(apsp.shortestPath(graph.g, graph.g));

		}
	}

}