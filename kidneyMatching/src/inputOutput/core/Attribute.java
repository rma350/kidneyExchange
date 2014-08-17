package inputOutput.core;

import com.google.common.base.Function;



public abstract class Attribute<T,V> implements Function<T,V>{
	
	public abstract V apply(T input);
	
	public boolean equals(Object other){
		return other != null && this == other;
	}

}
