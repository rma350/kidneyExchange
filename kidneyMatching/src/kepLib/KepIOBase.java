package kepLib;

import java.io.IOException;

import graphUtil.Edge;
import graphUtil.Node;



public abstract class KepIOBase implements KepIO{
	
	public KepIOBase(){	}


	@Override
	public KepInstance<Node,Edge> read(String file){
		return readParseData(file).getInstance();
	}
	@Override
	public <V,E> void writeWithToString(KepInstance<V,E> instance,String fileName) {
		write(instance,KepParseData.toStringFunction,KepParseData.toStringFunction,fileName);
	}
	@Override
	public <V,E> void writeAnonymous(KepInstance<V,E> instance,String fileName) {
		write(instance,KepParseData.anonymousNodeNames(instance),KepParseData.anonymousEdgeNames(instance),fileName);
	}

}
