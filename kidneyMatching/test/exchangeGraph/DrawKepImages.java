package exchangeGraph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import exchangeGraph.CyclePackingIpTest.InputOutput;
import graphUtil.Edge;

public class DrawKepImages {
	
	private static final String imageIn = CyclePackingIpTest.unitTestDir + "imageIn" + File.separator;
	private static final String imageOut = CyclePackingIpTest.unitTestDir + "imageOut" + File.separator;
	
	private static final String fileNameInputBase = "e";
	private static final String fileNameOutputBase = "sol";
	private static final String suffix = ".tex";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
		int numTestInstances = CyclePackingIpTest.numTests;
		for(int i = 1; i <= numTestInstances; i++){
			InputOutput inOut = CyclePackingIpTest.readInputOutput(i);
			String imageInName = imageIn + fileNameInputBase + i + suffix;
			BufferedWriter writerInput = new BufferedWriter(new FileWriter(imageInName));
			inOut.getKepInstance().printToTikz(writerInput, new HashSet<Edge>(),true);
			String imageOutName = imageOut + fileNameOutputBase + i + suffix;
			BufferedWriter writerOutput = new BufferedWriter(new FileWriter(imageOutName));
			inOut.getKepInstance().printToTikz(writerOutput, inOut.getSolution(),true);
			Runtime.getRuntime().exec("pdflatex -output-directory " + imageIn + " " + imageInName);
			Runtime.getRuntime().exec("pdflatex -output-directory " + imageOut + " " + imageOutName);
		}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

}
