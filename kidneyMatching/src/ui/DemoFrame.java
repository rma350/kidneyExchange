package ui;

import javax.swing.JFrame;
import javax.swing.JPanel;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

public class DemoFrame<V,E> extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1039700460519384454L;
	private JPanel panel;
	private GraphPanel<V,E> graphPanel;
	
	public DemoFrame(DirectedSparseMultigraph<V,E> graph){
		this.panel = new JPanel();
		this.graphPanel = new GraphPanel<V,E>(graph);
		this.setContentPane(panel);
		this.setSize(1000, 800);
		panel.add(graphPanel);		
	}
	
	public GraphPanel<V,E> getGraphPanel(){
		return this.graphPanel;
	}

}
