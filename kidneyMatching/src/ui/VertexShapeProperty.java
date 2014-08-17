package ui;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

public class VertexShapeProperty extends VertexProperty<Shape> {
	
	private List<VertexPropertyValue> values;
	
	public VertexShapeProperty(){
		this.values = new ArrayList<VertexPropertyValue>();
		values.add(new VertexPropertyValue(new Rectangle(-20,-10,40,20),"Rectangle"));
		values.add(new VertexPropertyValue(new Rectangle(-10,-10,20,20),"Square"));
		values.add(new VertexPropertyValue(new Ellipse2D.Double(-10,-10,20,20),"Circle"));
	}

	@Override
	public List<VertexPropertyValue> getValues() {
		return this.values;
	}

}
