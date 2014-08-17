package main;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

import graphUtil.Edge;
import graphUtil.Node;
import kepLib.KepInstance;
import kepLib.KepProblemData;
import kepLib.KepTextReaderWriter;

public class RemoveAltruistsAtRandom {

	/**
	 * Note that if there are any auxiliary constraints, this program will fail to copy them over.
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 3){
			System.out.println("Input must have three arguments, the input file name," + "the output file name, then the probability each altruist is kept.  Exiting...");
			return;
		}
		String inFile = args[0];
		String outFile = args[1];
		double probKeep = Double.parseDouble(args[2]);
		KepInstance<Node,Edge> input = KepTextReaderWriter.INSTANCE.read(inFile);
		
		ImmutableSet.Builder<Node> retainedRootsBuilder = ImmutableSet.builder();
		for(Node root: input.getRootNodes()){
			if(Math.random() < probKeep){
				retainedRootsBuilder.add(root);
			}
		}
		ImmutableSet<Node> retainedRoots = retainedRootsBuilder.build();
		Set<Node> deleltedRoots = Sets.difference(input.getRootNodes(), retainedRoots);
		DirectedSparseMultigraph<Node,Edge> retainedGraph = new DirectedSparseMultigraph<Node,Edge>();
		for(Node node : Sets.union(retainedRoots, input.nonRootNodes())){
			retainedGraph.addVertex(node);
		}
		for(Edge edge: input.getGraph().getEdges()){
			if(!deleltedRoots.contains(input.getGraph().getSource(edge))){
				retainedGraph.addEdge(edge, input.getGraph().getSource(edge), input.getGraph().getDest(edge));
			}
		}
		KepProblemData<Node,Edge> outProblemData = new KepProblemData<Node,Edge>(retainedGraph,retainedRoots,input.getPairedNodes(),input.getTerminalNodes());
		KepInstance<Node,Edge> output = new KepInstance<Node,Edge>(outProblemData, input.getEdgeWeights(), input.getMaxChainLength(), input.getMaxCycleLength(), input.getCycleBonus());
		KepTextReaderWriter.INSTANCE.writeWithToString(output, outFile);
		
	}
	
	

}
