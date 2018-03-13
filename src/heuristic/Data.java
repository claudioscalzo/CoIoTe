package heuristic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Data {
	
	// ATTRIBUTES
	private int[][][][] costs; 			// costs matrix (i,j,m,t)
	private int[] numActivities; 		// number of activities for a specific user (m)
	private int[] activities; 			// activities to do in a specific cell (i), in ANY time
	private int[][][] usersCell; 		// number of users of type m in cell i during t (i,m,t)
    private int[] surplusActivities;	// surplus of activities in a sp

	private int numCells;
	private int numTimeSteps;
	private int numCustTypes;

	private Object lock;
	private ArrayList<ArrayList<Arch>> arches;
	private HashMap<Integer,Integer> distribution;
	
	private Boolean[] alreadySorted;
	
	// CONSTRUCTOR
	public Data(int numCells, int numTimeSteps, int numCustTypes) {
		super();
		costs = new int[numCells][numCells][numCustTypes][numTimeSteps];
		numActivities = new int[numCustTypes];
		activities = new int[numCells];
		usersCell = new int[numCells][numCustTypes][numTimeSteps];
        surplusActivities = new int[numCells];

		this.numCells = numCells;
		this.numTimeSteps = numTimeSteps;
		this.numCustTypes = numCustTypes;

		lock = new Object();

		arches = new ArrayList<ArrayList<Arch>>();
		for(int i=0; i < numCells; i++)
			arches.add(new ArrayList<Arch>());
		distribution = new HashMap<>();
		
		alreadySorted = new Boolean[numCells];
		Arrays.fill(alreadySorted, Boolean.FALSE);
		
	}
	
	// GETTERS
	public int getCosts(int i, int j, int m, int t) {
		return costs[i][j][m][t];
	}

	public int getNumActivities(int m) {
		return numActivities[m];
	}

	public int getActivities(int i) {
		return activities[i];
	}

	public int getUsersCell(int i, int m, int t) {
		return usersCell[i][m][t];
	}
	
	public int getSurplusActivites(int j) {
		return surplusActivities[j];
	}

	// NEW GETTERS FOR COPY
	public int[][][][] getCosts() { return costs; }

	public int[] getNumActivities() { return numActivities; }

	public int[] getActivities() { return activities; }

	// because usersCell will be modified by solution --> the method returns another object
	public int[][][] getUsersCell() {
		int[][][] copy = new int[numCells][numCustTypes][numTimeSteps];

		for(int i = 0; i < numCells; i++)
			for(int j = 0; j < numCustTypes; j++)
				for(int k = 0; k < numTimeSteps; k++)
					copy[i][j][k] = usersCell[i][j][k];

		return copy;
	}

	public Object getLock() { return lock; }

	public ArrayList<ArrayList<Arch>> getArches() { return arches; }


	// SETTERS
	public void setCosts(int i, int j, int m, int t, int value) {
		costs[i][j][m][t] = value;
	}
	
	public void setNumActivities(int m, int value) {
		numActivities[m] = value;
	}
	
	public void setActivities(int i, int value) {
		activities[i] = value;
	}
	
	public void setUsersCell(int i, int m, int t, int value) {
		usersCell[i][m][t] = value;
	}
	
	public void setSurplusActivities(int j, int value) {
		surplusActivities[j] = value;
    }

	// NEW SETTERS FOR COPY
	public void setCosts(int[][][][] costs) { this.costs = costs; }

	public void setNumActivities(int[] numActivities) { this.numActivities = numActivities; }

	public void setActivities(int[] activities) { this.activities = activities; }

	public void setUsersCell(int[][][] usersCell) { this.usersCell = usersCell; }

	public void setLock(Object lock) { this.lock = lock; }

	public void setArches(ArrayList<ArrayList<Arch>> arches) { this.arches = arches; }


	// DECREMENT
	public void decrementUsersCell(int i, int m, int t, int value) {
		usersCell[i][m][t] -= value;
	}

	// INCREMENT
	public void incrementUsersCell(int i, int m, int t, int value) {
		usersCell[i][m][t] += value;
	}

	// METHODS FOR ARCHES
	public List<Arch> getArches(int dst) {
		return arches.get(dst);
	}
	
	public void setArches(int dst, ArrayList<Arch> list) {
		arches.set(dst, list);
	}
	
	public void addArch(int j, Arch a) {
		arches.get(j).add(a);
	}
	
	public void incrementDistribution(int i, int m, int t) {
        String s = i + "" + m + "" + t;
		int key = Integer.parseInt(s);
        int count = (distribution.containsKey(key)) ? distribution.get(key) : 0;
        distribution.put(key, count+1);
	}

	public void decrementDistribution(int i, int m, int t) {
		String s = i + "" + m + "" + t;
		int key = Integer.parseInt(s);
		int count = (distribution.containsKey(key)) ? distribution.get(key) : 0;
		distribution.put(key, count-1);
	}

	public int getDistribution(int i, int m, int t) {
        String s = i + "" + m + "" + t;
        int key = Integer.parseInt(s);
        return (distribution.containsKey(key)) ? distribution.get(key) : 0;
    }
	
	public Boolean getAlreadySorted(int j) {
		return alreadySorted[j];
	}
	
	public void setAlreadySorted(int j) {
		alreadySorted[j] = Boolean.TRUE;
	}
	
	public Boolean[] getAlreadySorted() {
		return alreadySorted;
	}
	
	public void setAlreadySorted(Boolean[] alreadySorted) {
		this.alreadySorted = alreadySorted;
	}
	
	

}
