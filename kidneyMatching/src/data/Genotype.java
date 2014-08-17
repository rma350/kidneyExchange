package data;

public class Genotype {
	private int alleleLo;
	private int alleleHi;
	public Genotype(int alleleOne, int alleleTwo) {
		super();
		if(alleleOne <= alleleTwo){
			this.alleleLo = alleleOne;
			this.alleleHi = alleleTwo;
		}
		else{
			this.alleleLo = alleleTwo;
			this.alleleHi = alleleOne;
		}		
	}
	public int getAlleleLo() {
		return alleleLo;
	}
	public int getAlleleHi() {
		return alleleHi;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + alleleHi;
		result = prime * result + alleleLo;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Genotype other = (Genotype) obj;
		if (alleleHi != other.alleleHi)
			return false;
		if (alleleLo != other.alleleLo)
			return false;
		return true;
	}
	
	/**
	 * 
	 * @param other
	 * @return
	 */
	public int containsCount(Genotype other){
		int ans = 0;
		if(contains(other.getAlleleHi())){
			ans++;
		}
		if(contains(other.alleleLo)){
			ans++;
		}
		return ans;
	}
	
	public boolean contains(int other){
		return this.alleleHi == other  || this.alleleLo == other;
	}
	
	
	
	
}
