package randomInstances;

import graphUtil.Edge;
import graphUtil.Node;

import java.util.Map;

import kepLib.KepInstance;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class RandomArrivalTimes {

	public static ImmutableMap<Node,Double> randomArrivalTimes(KepInstance<Node,Edge> instance, int maxArrivalTime){
		ImmutableMap.Builder<Node,Double> ans = ImmutableMap.builder();
		for(Node node: instance.getGraph().getVertices()){
			ans.put(node, Math.floor(Math.random()*maxArrivalTime));
		}
		return ans.build();
	}

}
