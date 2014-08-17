package jFreeChart;

import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;

public class DegreeHistogram {
	
	private static String pdf = ".pdf";
	
	public static enum HistType{
		InDegree,OutDegree
	}
	
	public static void makeInAndOutDegreeHistogram(String baseFileName, String titleSuffix, double[] inDegrees, double[] outDegrees){
		makeDegreeHistogram(baseFileName + "InDegree" + pdf, "In Degree of " + titleSuffix, inDegrees,HistType.InDegree);
		makeDegreeHistogram(baseFileName + "OutDegree" + pdf, "Out Degree of " + titleSuffix, outDegrees,HistType.OutDegree);
	}
	
	public static void makeDegreeHistogram(String qualifiedFileName, String title, double[] data, HistType histType){
		HistogramDataset dataset = new HistogramDataset();
		dataset.addSeries("h1", data, 10);
		String xLabel;
		String yLabel;
		if(histType == HistType.InDegree){
			xLabel = "Edge probability";
			yLabel = "Nodes";
		}
		else if(histType == HistType.OutDegree){
			xLabel = "Edge probability";
			yLabel = "Nodes";
		}
		else{
			throw new RuntimeException();
		}
		JFreeChart chart = ChartFactory.createHistogram(
				title,
	            xLabel,
	            yLabel,
	            dataset,
	            PlotOrientation.VERTICAL,
	            false,
	            false,
	            false);
		
		File file = new File(qualifiedFileName);
		try {
			JFreeChartUtil.saveChartAsPDF(file, chart, 1000, 800);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
