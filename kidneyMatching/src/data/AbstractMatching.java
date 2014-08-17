package data;

public abstract class AbstractMatching {
	
	private int id;

	public int getId() {
		return id;
	}

	public AbstractMatching(int id) {
		super();
		this.id = id;
	}
	
	public abstract void checkWellFormed();

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		AbstractMatching other = (AbstractMatching) obj;
		if (id != other.id)
			return false;
		return true;
	}

	

}
