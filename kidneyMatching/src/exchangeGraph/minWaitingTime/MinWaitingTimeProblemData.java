package exchangeGraph.minWaitingTime;

import com.google.common.collect.ImmutableMap;

public class MinWaitingTimeProblemData<V> {

  private ImmutableMap<V, ? extends Number> nodeArrivalTime;
  private double terminationTime;

  public ImmutableMap<V, ? extends Number> getNodeArrivalTime() {
    return nodeArrivalTime;
  }

  public double getTerminationTime() {
    return terminationTime;
  }

  public MinWaitingTimeProblemData(
      ImmutableMap<V, ? extends Number> nodeArrivalTime, double terminationTime) {
    super();
    this.nodeArrivalTime = nodeArrivalTime;
    this.terminationTime = terminationTime;
  }

}
