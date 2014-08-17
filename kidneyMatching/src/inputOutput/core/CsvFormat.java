package inputOutput.core;

import com.google.common.base.Function;

public abstract class CsvFormat<T> implements Function<T,String>{

	public abstract String apply(T value);
	
	public boolean equals(Object other){
		return other != null && this == other;
	}
}
