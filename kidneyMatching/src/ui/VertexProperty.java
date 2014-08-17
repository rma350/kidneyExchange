package ui;

import java.util.List;

public abstract class VertexProperty<T> {
	
	public abstract List<VertexPropertyValue> getValues();
	
	
	
	
	public class VertexPropertyValue{
		private T value;
		private String getValueName;
		public T getValue() {
			return value;
		}
		public String getGetValueName() {
			return getValueName;
		}
		public VertexPropertyValue(T value, String getValueName) {
			super();
			this.value = value;
			this.getValueName = getValueName;
		}
		
		
	}

}
