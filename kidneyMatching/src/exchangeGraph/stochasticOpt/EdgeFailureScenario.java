package exchangeGraph.stochasticOpt;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import exchangeGraph.CplexUtil;

import kepLib.KepInstance;

public class EdgeFailureScenario<V,E> {
	
	
	private KepInstance<V,E> kepInstance;
	private ImmutableSet<E> failedEdges;
	
	public EdgeFailureScenario(KepInstance<V, E> kepInstance,
			ImmutableSet<E> failedEdges) {
		super();
		this.kepInstance = kepInstance;
		this.failedEdges = failedEdges;
	}
	public KepInstance<V, E> getKepInstance() {
		return kepInstance;
	}
	public ImmutableSet<E> getFailedEdges() {
		return failedEdges;
	}
	
	public static <V,E> List<EdgeFailureScenario<V,E>> generateScenarios(KepInstance<V,E> kepInstance, Map<E,Double> failureProbabilities, int numScenarios){
		List<EdgeFailureScenario<V,E>> scenarios = Lists.newArrayList();
		if(!new HashSet<E>(kepInstance.getGraph().getEdges()).equals(failureProbabilities.keySet())){
			throw new RuntimeException("edges in failure probabilities were not equal to kepInstance edges");
		}
		for(E edge: failureProbabilities.keySet()){
			if(failureProbabilities.get(edge) < -CplexUtil.epsilon || failureProbabilities.get(edge) > 1 + CplexUtil.epsilon){
				throw new RuntimeException("edges failure probabilty should be in in [0,1] but found: " + edge.toString() + ", " + failureProbabilities.get(edge));
			}
		}
		for(int i = 0; i < numScenarios;i++){
			ImmutableSet.Builder<E> failedEdges = ImmutableSet.builder();
			for(E edge: failureProbabilities.keySet()){
				if(Math.random() < failureProbabilities.get(edge)){
					failedEdges.add(edge);
				}
			}
			scenarios.add(new EdgeFailureScenario<V,E>(kepInstance,failedEdges.build()));
		}
		return scenarios;
	}
	
	

}
