package threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Optional;

public class FixedThreadPool {
	
	private ExecutorService exec;
	private int numThreads;
	
	public FixedThreadPool(int numThreads){
		this.numThreads = numThreads;
		this.exec = Executors.newFixedThreadPool(numThreads);
	}

	public ExecutorService getExec() {
		return exec;
	}

	public int getNumThreads() {
		return numThreads;
	}
	
	public static Optional<FixedThreadPool> makePool(int numThreads){
		if(numThreads > 1){
			return Optional.of(new FixedThreadPool(numThreads));
		}
		else{
			if(numThreads < 1){
				System.err.println("Warning: numThreads should be at least one...");				
			}
			return Optional.<FixedThreadPool>absent();
		}
	}
	
	public static void shutDown(Optional<FixedThreadPool> threadPool){
		if(threadPool.isPresent()){
			threadPool.get().getExec().shutdown();
		}
	}

}
