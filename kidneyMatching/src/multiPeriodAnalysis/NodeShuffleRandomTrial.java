package multiPeriodAnalysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import kepProtos.KepProtos.NodeArrivalTime;
import kepProtos.KepProtos.RandomTrial;
import multiPeriod.TimeInstant;

import com.google.common.collect.Lists;

public class NodeShuffleRandomTrial {

  public static <V, E, T extends Comparable<T>> List<RandomTrial> makeRandomTrials(
      Environment<V, E, T> environment, int numTrials) {
    List<String> nodeIds = Lists.newArrayList();
    List<TimeInstant<T>> arrivalTimes = Lists.newArrayList();
    for (Map.Entry<V, TimeInstant<T>> entry : environment
        .getMultiPeriodInputs().getNodeArrivalTimes().entrySet()) {
      String nodeId = environment.getNodeIds().inverse().get(entry.getKey());
      if (nodeId == null) {
        System.out.println("No id found for node: " + entry.getKey());
        System.out.println("Node id map: ");
        System.out.println(environment.getNodeIds());
        throw new RuntimeException();
      }

      nodeIds.add(nodeId);
      arrivalTimes.add(entry.getValue());
    }
    List<RandomTrial> ans = Lists.newArrayList();
    for (int i = 0; i < numTrials; i++) {
      Collections.shuffle(arrivalTimes);
      RandomTrial.Builder trial = RandomTrial.newBuilder();
      for (int j = 0; j < nodeIds.size(); j++) {
        trial.addNodeArrivalTime(NodeArrivalTime
            .newBuilder()
            .setNodeId(nodeIds.get(j))
            .setArrivalTime(
                environment.getTimeWriter().writeTime(arrivalTimes.get(j))));
      }
      ans.add(trial.build());
    }
    return ans;
  }

}
