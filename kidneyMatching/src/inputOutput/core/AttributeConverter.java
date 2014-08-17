package inputOutput.core;

import com.google.common.base.Function;

public interface AttributeConverter<T> extends Function<T,String>{

	public String apply(T input);
}
