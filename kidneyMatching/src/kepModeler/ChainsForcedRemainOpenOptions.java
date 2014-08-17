package kepModeler;

import kepLib.KepInstance;
import kepLib.KepInstance.BridgeConstraint;
import kepLib.KepInstance.RelationType;

public abstract class ChainsForcedRemainOpenOptions {
	
	private OptionName optionName;
	private String name;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	protected ChainsForcedRemainOpenOptions(OptionName optionName){
		this.optionName = optionName;
	}
	public OptionName getOptionName() {
		return optionName;
	}
	public static enum OptionName{
		none, atLeastCount, atLeastCountOfQuality;
	}
	
	//public abstract <V,E> void addConstraint(KepInstance<V,E> kepInstance);
	
	public static final ChainsForcedRemainOpenOptions none = new ChainsForcedRemainOpenOptions(OptionName.none){};
	
	
	public static class AtLeastCountChains extends ChainsForcedRemainOpenOptions{		
		private int minNumChains;
		public AtLeastCountChains(int minNumChains) {
			super(OptionName.atLeastCount);
			this.minNumChains = minNumChains;
		}		
		public int getMinNumChains(){
			return this.minNumChains;
		}
		
	}
	
	public static class AtLeastCountOfQualityChains extends ChainsForcedRemainOpenOptions{		
		private int minNumChains;
		private double minimumChainRootQuality;
		
		
		public AtLeastCountOfQualityChains(int minNumChains, double minimumChainRootQuality) {
			super(OptionName.atLeastCountOfQuality);
			this.minNumChains = minNumChains;
			this.minimumChainRootQuality = minimumChainRootQuality;
		}		
		public int getMinNumChains(){
			return this.minNumChains;
		}
		public double getMinimumChainRootQuality() {
			return minimumChainRootQuality;
		}
		

	}
	
	
	
}
