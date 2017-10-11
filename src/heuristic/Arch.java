package heuristic;

public class Arch implements Comparable<Arch> {
	private int src;
	private int dst;
	private int type;
	private int numActivities;
	private int time;
	private int cost;
	
	
	// CONSTRUCTOR
	public Arch(int src, int dst, int type, int numActivities, int time, int cost) {
		this.src = src;
		this.dst = dst;
		this.type = type;
		this.numActivities = numActivities;
		this.time = time;
		this.cost = cost;
	}
	

	//GETTERS
	public int getSrc() {
		return src;
	}

	public int getDst() {
		return dst;
	}

	public int getType() {
		return type;
	}
	
	public int getNumActivities() {
		return numActivities;
	}

	public int getTime() {
		return time;
	}

	public int getCost() {
		return cost;
	}

	@Override
	public int compareTo(Arch b) {
		double res = ((double)cost / numActivities ) - ((double)b.getCost() / b.getNumActivities() );

		if(res == 0) {
			if(type < b.getType() )
				return -1;

			if(type > b.getType() )
				return 1;

			if(time < b.getTime() )
				return -1;

			if(time > b.getTime() )
				return 1;

			if(src < b.getSrc() )
				return -1;

			if(src > b.getSrc() )
				return 1;

			if(dst < b.getDst() )
				return -1;

			if(dst > b.getDst() )
				return 1;

			return 0;
		}

		if(res > 0)
			return 1;

		return -1;
	}
}
