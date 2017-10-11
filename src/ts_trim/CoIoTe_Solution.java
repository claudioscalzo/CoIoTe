package ts_trim;

import heuristic.Arch;
import heuristic.Data;
import org.coinor.opents.SolutionAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class CoIoTe_Solution extends SolutionAdapter {

	// ATTRIBUTES
	private int[][][][] matrix;
	private Data data;
	private int numCells;
	private int numCustTypes;
	private int numTimeSteps;

    private int maxCost = 0;

	// NULL CONSTRUCTOR (to appease the clone() method)
	public CoIoTe_Solution() {

	}

	// REAL CONSTRUCTOR
	public CoIoTe_Solution(Data data, int numCells, int numCustTypes, int numTimeSteps) {
		super();
		matrix = new int[numCells][numCells][numCustTypes][numTimeSteps];
		this.numCells = numCells;
		this.numCustTypes = numCustTypes;
		this.numTimeSteps = numTimeSteps;

		//this.data is a new object
		this.data = new Data(numCells, numTimeSteps, numCustTypes);
		this.data.setCosts(data.getCosts() );
		this.data.setNumActivities(data.getNumActivities() );
		this.data.setActivities(data.getActivities() );
		this.data.setUsersCell(data.getUsersCell() ); //this.data.usersCell is a copy of the original
		this.data.setLock(data.getLock());
		this.data.setArches(data.getArches() );
		this.data.setAlreadySorted(data.getAlreadySorted() );
	}

	// CLONE METHOD
	@Override
	public CoIoTe_Solution clone() {
		CoIoTe_Solution copy = (CoIoTe_Solution)super.clone();
		copy.matrix = (int[][][][])this.matrix.clone();

		return copy;
	}

	// GETTER
	public int getCell(int i, int j, int m, int t) {
		return matrix[i][j][m][t];
	}

	public Data getData() {
		return data;
	}

	//SETTER
	public void incrementCell(int i, int j, int m, int t, int value) {
		matrix[i][j][m][t] += value;
	}

    public void decrementCell(int i, int j, int m, int t, int value) {
        matrix[i][j][m][t] -= value;
    }

    private class Pair {
        private final int index;
        private final int value;

        Pair(int index, int value) {
            this.index = index;
            this.value = value;
        }

        int getIndex() {
            return index;
        }

        int getValue() {
            return value;
        }
    }

    public void solveOrderedH(int coefficient1, int coefficient2, boolean reversedFirst) {
        HashMap<String, List<Arch>> map = new HashMap<>();
        ArrayList<Pair> activitiesTodo = new ArrayList<>();

        double totalDemand = 0;
        int countDemand = 0;

        for (int j = 0; j < numCells; j++) {
            totalDemand += data.getActivities(j);
            if (data.getActivities(j) > 0)
                countDemand++;

            activitiesTodo.add(j,new Pair(j,data.getActivities(j)));

            List<Arch> arches = data.getArches(j);
            arches.stream()
                    .filter(a -> data.getUsersCell(a.getSrc(), a.getType(), a.getTime()) > 0)
                    .sorted( Arch::compareTo )
                    .forEach((a) -> {
                        List<Arch> archList = map.get(a.getNumActivities() + "-" + a.getDst());
                        if (archList == null) {
                            archList = new ArrayList<>();
                            archList.add(a);
                            map.put(a.getNumActivities() + "-" + a.getDst(), archList);
                        } else
                            archList.add(a);
                    });
        }

        int[] users = new int[numCustTypes];
        for (int m = 0; m < numCustTypes; m++)
            for (int i = 0; i < numCells; i++)
                for (int t = 0; t < numTimeSteps; t++)
                    users[m] += data.getUsersCell(i, m, t);

        int tasksMean = (int)totalDemand / countDemand;
        int tasksLastType = data.getNumActivities(numCustTypes-1);

        for (int m = 0; m < numCustTypes; m++) {

            if (m < numCustTypes - 1) {     // if m it's not the last

                // count represent how many iteration of j has been done (in order to use all users m < 2)
                for (int count = 0; users[m] > 0; count++) {

                    ArrayList<Pair> ordered;
                    if (count == 0) {
                        ordered = new ArrayList<>(activitiesTodo);
                        Collections.sort(ordered, (a,b) -> coefficient1 * (a.getValue() - b.getValue()));
                        if (reversedFirst)
                            Collections.reverse(ordered);
                    } else {
                        ordered = new ArrayList<>(activitiesTodo);
                        Collections.sort(ordered, (a,b) -> coefficient2 * (a.getValue() - b.getValue()));
                        if (!reversedFirst)
                            Collections.shuffle(ordered, new Random(18));
                    }

                    for (int j = 0; j < numCells; j++) {

                        if (ordered.get(j).getValue() <= 0    // if solved
                                || (count > 0 && data.getActivities(j) < tasksMean))
                            continue;

                        List<Arch> arches = map.get(data.getNumActivities(m) + "-" + j);
                        boolean satisfied = false;

                        int todo = ordered.get(j).getValue();
                        if (todo > data.getNumActivities(m) && todo <= data.getNumActivities(m+1)
                                || (todo % tasksLastType == 0 && todo < tasksMean))
                            satisfied = true;

                        while (!satisfied) {

                            if (users[m] == 0)
                                break;

                            int i = 0, t = 0;
                            for (int k = 0; k < arches.size(); k++) {  // not safe
                                Arch arch = arches.get(k);
                                i = arch.getSrc();
                                t = arch.getTime();
                                if (data.getUsersCell(i, m, t) > 0)
                                    break;
                            }

                            int indexJ = ordered.get(j).getIndex();
                            matrix[i][indexJ][m][t]++;
                            data.decrementUsersCell(i, m, t, 1);
                            users[m]--;
                            todo -= data.getNumActivities(m);
                            totalDemand -= data.getNumActivities(m);

                            // check if the number of remain tasks can be substracted by a multiple of m+1 so it can be completed in the end
                            int m2 = (m + 1 == numCustTypes - 1) ? tasksLastType : data.getNumActivities(m+2);
                            for (int m1 = data.getNumActivities(m+1); m1 <= todo && !satisfied; m1 += data.getNumActivities(m+1))
                                if (m1 % m2 == todo % m2) {
                                    activitiesTodo.set(indexJ, new Pair(indexJ,todo));
                                    satisfied = true;
                                }

                            // check if the number of task is between m and m+1 or if tasks can be done by last customers
                            if (todo > data.getNumActivities(m) && todo <= data.getNumActivities(m+1)
                                    || todo % m2 == 0) {
                                activitiesTodo.set(indexJ, new Pair(indexJ,todo));
                                satisfied = true;
                            }

                            // if solved
                            if (todo <= 0) {
                                data.setSurplusActivities(indexJ, todo);
                                activitiesTodo.set(indexJ, new Pair(indexJ,todo));
                                satisfied = true;
                            }

                        }


                    }

                }

            } else {    // if it is the last m then finish the job

                for (int j = 0; j < numCells; j++) {

                    int todo = activitiesTodo.get(j).getValue();
                    if (todo <= 0)
                        continue;

                    List<Arch> arches = map.get(data.getNumActivities(m) + "-" + j);
                    int i = 0, t = 0;

                    while (todo > 0) {
                        for (int k = 0; k < arches.size(); k++) {   // not safe?
                            Arch arch = arches.get(k);
                            i = arch.getSrc();
                            t = arch.getTime();
                            if (data.getUsersCell(i, m, t) > 0)
                                break;
                        }

                        matrix[i][j][m][t]++;
                        data.decrementUsersCell(i, m, t, 1);
                        todo -= data.getNumActivities(m);
                        totalDemand -= data.getNumActivities(m);

                        if (todo <= 0) {
                            data.setSurplusActivities(j, todo);
                            activitiesTodo.set(j, new Pair(j, todo));
                        }
                    }

                }

            }

        }
    }

    public void takenSwap() {

        for(int j = 0; j < numCells; j++) {
            for(int i = 0; i < numCells; i++) {
                for(int m = 0; m < numCustTypes; m++) {
                    for(int t = 0; t < numTimeSteps; t++) {
                        if(matrix[i][j][m][t] == 0)
                            continue;

                        for(int j1 = 0; j1 < numCells; j1++) {
                            if (j1 == i)
                                continue;

                            for(int i1 = 0; i1 < numCells; i1++) {
                                if (j1 == i1 || i1 == j)
                                    continue;

                                for(int t1 = 0; t1 < numTimeSteps; t1++) {
                                    for(int m1 = 0; m1 < numCustTypes; m1++) {
                                        if(matrix[i1][j1][m1][t1] == 0)
                                            continue;

                                        //check the best number of people to swap
                                        int quantityTask = matrix[i][j][m][t] * data.getNumActivities(m);

                                        if(matrix[i1][j1][m1][t1] * data.getNumActivities(m1) < quantityTask)
                                            quantityTask = matrix[i1][j1][m1][t1] * data.getNumActivities(m1);

                                        //check for better algorithm... Task1 and Task2 MUST be equal to have feasible solution
                                        while (quantityTask % data.getNumActivities(m) != 0 || quantityTask % data.getNumActivities(m1) != 0)
                                            quantityTask--;

                                        if(quantityTask == 0)
                                            continue;

                                        int quantity1 = quantityTask / data.getNumActivities(m);
                                        int quantity2 = quantityTask / data.getNumActivities(m1);

                                        //METHOD SWAP TAKEN USERS
                                        double obj1 = quantity1 * data.getCosts(i, j, m, t);
                                        double obj2 = quantity2 * data.getCosts(i1, j, m1, t1);
                                        double obj3 = quantity2 * data.getCosts(i1, j1, m1, t1);
                                        double obj4 = quantity1 * data.getCosts(i, j1, m, t);
                                        double objDiff = obj2 - obj1 + obj4 - obj3;

                                        if (objDiff < 0) {
                                            matrix[i][j][m][t] -= quantity1;
                                            matrix[i1][j][m1][t1] += quantity2;

                                            matrix[i1][j1][m1][t1] -= quantity2;
                                            matrix[i][j1][m][t] += quantity1;
                                        }

                                    }
                                }

                            }

                        }   // end for #1

                    }
                }
            }
        }   // end for #0

    }

	public boolean solveOrdered(int varietyCoefficient, int n, int seed) {
		// variety coefficient is a global attribute, passed by the multi threaded solve method in Heuristic
        boolean isSolved = false;

        ArrayList<Pair> ordered = new ArrayList<>();
        if (seed != 0) {
            for (int j = 0; j < numCells; j++)
                ordered.add(new Pair(j, data.getActivities(j)));
            Collections.shuffle(ordered, new Random(seed));
        }
        
		for (int indexJ = -n; indexJ < numCells - n; indexJ ++) {
            int j = (seed != 0) ? ordered.get(Math.abs(indexJ)).getIndex() : Math.abs(indexJ);
		    int demand = data.getActivities(j);
		    if(demand == 0)
		        continue;

		    synchronized(data.getAlreadySorted(j) ) {
			    if(data.getAlreadySorted(j) == Boolean.FALSE) {
			    	data.setArches(j, data.getArches(j).stream().sorted(Arch::compareTo).collect(Collectors.toCollection(ArrayList::new) ));
			    	data.setAlreadySorted(j);
			    }
		    }
		    List<Arch> arches = data.getArches(j);

			boolean notSatisfied = true;
			for (int k = 0; k < arches.size() && notSatisfied; k++) {
				Arch arch = arches.get(k);
				int i = arch.getSrc();

				if (j != i) {
					int m = arch.getType();
					int t = arch.getTime();

					if (demand > data.getNumActivities(m) * data.getUsersCell(i, m, t) ) {
						matrix[i][j][m][t] =  data.getUsersCell(i, m, t) / varietyCoefficient;
						data.decrementUsersCell(i, m, t, matrix[i][j][m][t] );
						data.incrementDistribution(i,m,t);
					} else {
						matrix[i][j][m][t] += (int)Math.ceil((double)demand / data.getNumActivities(m));
						data.decrementUsersCell(i, m, t, (int)Math.ceil((double)demand / data.getNumActivities(m)));
						data.incrementDistribution(i,m,t);
						notSatisfied = false;
					}

                    if (data.getCosts(i,j,m,t) / data.getNumActivities(m) > maxCost)
                        maxCost = data.getCosts(i,j,m,t) / data.getNumActivities(m);

	                demand -= data.getNumActivities(m) * matrix[i][j][m][t];
		    	}
			}

			if (demand > 0) {
                isSolved = true;
                break;
            }

            if (!notSatisfied)
                setSurplusActivities(j, demand);
		}

		return !isSolved;
	}

    public void threeWaySwap(int neighborhoodSize) {

        // locate the customer #0 (i0,j,m0,t0) that could be incremented
        for (int j = 0; j < numCells; j++) {
            for (int i0 = 0; i0 < numCells; i0++) {
                for (int m0 = 0; m0 < numCustTypes; m0++) {
                    for (int t0 = 0; t0 < numTimeSteps; t0++) {

                        if (matrix[i0][j][m0][t0] == 0)
                            continue;

                        // locate the customer #1 (i1,j,m1,t1) that could be decremented (in order to balance the increment of #0 and #2)
                        int i1 = (j > neighborhoodSize) ? j - neighborhoodSize : 0;
                        for (; i1 < numCells && i1 < j + neighborhoodSize; i1++) {
                            for (int m1 = 0; m1 < numCustTypes; m1++) {
                                for (int t1 = 0; t1 < numTimeSteps; t1++) {

                                    if (matrix[i1][j][m1][t1] == 0 || (i0 == i1 && m0 == m1 && t0 == t1))
                                        continue;

                                    // locate the customer #2 (i2,j,m2,t2) that could be incremented
                                    int i2 = (j > neighborhoodSize) ? j - neighborhoodSize : 0;
                                    for (; i2 < numCells && i2 < j + neighborhoodSize; i2++) {
                                        for (int m2 = 0; m2 < numCustTypes; m2++) {
                                            for (int t2 = 0; t2 < numTimeSteps; t2++) {

                                                int tasks0 = data.getNumActivities(m0);
                                                int tasks1 = data.getNumActivities(m1);
                                                int tasks2 = data.getNumActivities(m2);
                                                int customersToMoveFrom1 = 0;
                                                int customersToMoveTo02 = 0;

                                                // the number of tasks of #1 should be equal to the sum of tasks of #0 and #2 (otherwise the balancement won't be respected)
                                                if (data.getUsersCell(i2, m2, t2) == 0
                                                        || (i1 == i2 && m1 == m2 && t1 == t2)
                                                        || (i2 == i0 && m2 == m0 && t2 == t0)
                                                        || tasks1 != (tasks0 + tasks2))
                                                    continue;

                                                int tasksCustomer1IsDoing = matrix[i1][j][m1][t1] * tasks1;
                                                int tasksCustomer02CanDo = data.getUsersCell(i0,m0,t0) * tasks0 + data.getUsersCell(i2,m2,t2) * tasks2;

                                                boolean found = false;

                                                double objFunStart = data.getCosts(i0, j, m0, t0) * matrix[i0][j][m0][t0]
                                                        + data.getCosts(i1, j, m1, t1) * matrix[i1][j][m1][t1]
                                                        + data.getCosts(i2, j, m2, t2) * matrix[i2][j][m2][t2];
                                                double objFunEnd;

                                                int tasksToTransfer;    // count how many loop has been done
                                                // trying to find how many tasks could be transfered from #0 and #2 to #1 (#1 activites = #0 activities + #2 activities)
                                                for (customersToMoveTo02 = 0, tasksToTransfer = 0;
                                                     tasksToTransfer < tasksCustomer02CanDo || tasksToTransfer < tasksCustomer1IsDoing;
                                                     tasksToTransfer += tasks1, customersToMoveTo02++) {

                                                    customersToMoveFrom1 = tasksToTransfer / tasks1;

                                                    objFunEnd = ((matrix[i1][j][m1][t1] - customersToMoveFrom1) * data.getCosts(i1, j, m1, t1))
                                                            + ((matrix[i0][j][m0][t0] + customersToMoveTo02) * data.getCosts(i0, j, m0, t0))
                                                            + ((matrix[i2][j][m2][t2] + customersToMoveTo02) * data.getCosts(i2, j, m2, t2));

                                                    if (objFunEnd < objFunStart) {
                                                        if (customersToMoveTo02 <= data.getUsersCell(i2, m2, t2)
                                                                && customersToMoveTo02 <= data.getUsersCell(i0, m0, t0)
                                                                && customersToMoveFrom1 <= matrix[i1][j][m1][t1]) {
                                                            found = true;
                                                            break;
                                                        }
                                                    }

                                                }

                                                if (found) {
                                                    // transfering from #0
                                                    matrix[i0][j][m0][t0] += customersToMoveTo02;
                                                    data.decrementUsersCell(i0, m0, t0, customersToMoveTo02);

                                                    // balancing the transfering with #1
                                                    matrix[i1][j][m1][t1] -= customersToMoveFrom1;
                                                    data.incrementUsersCell(i1, m1, t1, customersToMoveFrom1);

                                                    // incrementing the new cell #2
                                                    matrix[i2][j][m2][t2] += customersToMoveTo02;
                                                    data.decrementUsersCell(i2, m2, t2, customersToMoveTo02);
                                                }

                                            }
                                        }
                                    }   // end for #2

                                }
                            }
                        }   // end for #1

                    }
                }
            }
        } // end for #0

    }

    public void fourWaySwap(int neighborhoodSize) {

        // locate the first cell (i0,j0,m0,t0) that could be incremented #0
        for (int j0 = 0; j0 < numCells; j0++) {
            for (int i0 = 0; i0 < numCells; i0++) {
                for (int m0 = 0; m0 < numCustTypes; m0++) {
                    for (int t0 = 0; t0 < numTimeSteps; t0++) {

                        if (data.getDistribution(i0,m0,t0) < 2 || matrix[i0][j0][m0][t0] == 0)
                            continue;

                        // locate the second cell (i0,j1,m0,t0) that could be decremented in order to increment #0 (this #1 is causing local minimum stuck)
                        for (int j1 = 0; j1 < numCells; j1++) {

                            // discard if there are no users taken
                            if (matrix[i0][j1][m0][t0] == 0)
                                continue;

                            // locate the third cell (i2,j1,m2,t2) in order to balance the decrement of #2
                            int i2 = (j1 > neighborhoodSize) ? j1 - neighborhoodSize: 0;
                            for (; i2 < numCells && i2 < j1 + neighborhoodSize; i2++) {
                                for (int m2 = 0; m2 < numCustTypes; m2++) {
                                    for (int t2 = 0; t2 < numTimeSteps; t2++) {

                                        // discard if there is no availability or if it costs too much
                                        if (data.getUsersCell(i2,m2,t2) == 0 || (data.getCosts(i2,j1,m2,t2) / data.getNumActivities(m2)) > maxCost
                                                || (i2 == i0 && m2 == m0 && t2 == t0))
                                            continue;

                                        // locate the forth cell (i3,m3,t3), this is used for the balancement of #3 (this cell should be decremented)
                                        int i3 = (j0 > neighborhoodSize) ? j0 - neighborhoodSize: 0;
                                        for (; i3 < numCells && i3 < j0 + neighborhoodSize; i3++) {
                                            for (int m3 = 0; m3 < numCustTypes; m3++) {
                                                for (int t3 = 0; t3 < numTimeSteps; t3++) {

                                                    if (matrix[i3][j0][m3][t3] == 0 || (i3 == i0 && m3 == m0 && t3 == t0))
                                                        continue;

                                                    int tasksToTransfer;
                                                    int tasks0 = data.getNumActivities(m0);
                                                    int tasks2 = data.getNumActivities(m2);
                                                    int tasks3 = data.getNumActivities(m3);
                                                    int tasksCustomer1IsDoing = matrix[i0][j1][m0][t0] * tasks0;    // how many tasks is #1 currently doing

                                                    boolean found = false;

                                                    // compute the total cost of these four cells
                                                    double objFunStart = data.getCosts(i0,j0,m0,t0) * matrix[i0][j0][m0][t0]
                                                            + data.getCosts(i0,j1,m0,t0) * matrix[i0][j1][m0][t0]
                                                            + data.getCosts(i2,j1,m2,t2) * matrix[i2][j1][m2][t2]
                                                            + data.getCosts(i3,j0,m3,t3) * matrix[i3][j0][m3][t3];
                                                    double objFunEnd;

                                                    int beforeTasks0 = (matrix[i0][j0][m0][t0] * tasks0) + (matrix[i3][j0][m3][t3] * tasks3);
                                                    int beforeTasks1 = (matrix[i0][j1][m0][t0] * tasks0) + (matrix[i2][j1][m2][t2] * tasks2);

                                                    // trying to find how many tasks could be transfered from #1 to #0 (by not violating the constraints)
                                                    for (tasksToTransfer = tasksCustomer1IsDoing;
                                                         tasksToTransfer > 0;
                                                         tasksToTransfer -= tasks0) {

                                                        // checking if it is possible to balance the tasks decremented in #1 with #2 (by keeping the same number of tasks done)
                                                        if (tasksToTransfer % tasks2 == 0) {
                                                            // compute the total cost of the new combination
                                                            objFunEnd = ((matrix[i2][j1][m2][t2] + (tasksToTransfer / tasks2)) * data.getCosts(i2, j1, m2, t2))
                                                                    + ((matrix[i0][j1][m0][t0] - (tasksToTransfer / tasks0)) * data.getCosts(i0, j1, m0, t0))
                                                                    + ((matrix[i0][j0][m0][t0] + (tasksToTransfer / tasks0)) * data.getCosts(i0, j0, m0, t0))
                                                                    + ((matrix[i3][j0][m3][t3] - (tasksToTransfer / tasks3)) * data.getCosts(i3, j0, m3, t3));

                                                            if (objFunEnd < objFunStart) {
                                                                // checking for constraints violation
                                                                if (tasksToTransfer / tasks2 <= data.getUsersCell(i2,m2,t2)
                                                                        && tasksToTransfer / tasks0 <= matrix[i0][j1][m0][t0]
                                                                        && tasksToTransfer / tasks3 <= matrix[i3][j0][m3][t3]) {
                                                                    found = true;
                                                                    break;
                                                                }
                                                            }
                                                        }

                                                    }

                                                    if (found) {
                                                        // transfering from #1 (to #0)
                                                        matrix[i0][j1][m0][t0] -= tasksToTransfer / tasks0;
                                                        data.incrementUsersCell(i0, m0, t0, tasksToTransfer / tasks0);

                                                        // adding to #0 (from #1)
                                                        matrix[i0][j0][m0][t0] += tasksToTransfer / tasks0;
                                                        data.decrementUsersCell(i0, m0, t0, tasksToTransfer / tasks0);

                                                        // balancing #2 (because #1 has been decremented)
                                                        matrix[i2][j1][m2][t2] += tasksToTransfer / tasks2;
                                                        data.decrementUsersCell(i2, m2, t2, tasksToTransfer / tasks2);

                                                        // balancing #3 (because #0 has been incremented)
                                                        matrix[i3][j0][m3][t3] -= tasksToTransfer / tasks3;
                                                        data.incrementUsersCell(i3, m3, t3, tasksToTransfer / tasks3);

                                                        int afterTasks0 = (matrix[i0][j0][m0][t0] * tasks0) + (matrix[i3][j0][m3][t3] * tasks3);
                                                        int afterTasks1 = (matrix[i0][j1][m0][t0] * tasks0) + (matrix[i2][j1][m2][t2] * tasks2);

                                                        // update the surplus tasks for j0 and j1
                                                        if (j0 == j1) {
                                                            int surplus = getSurplusActivities(j0) + ((beforeTasks0 + beforeTasks1) - (afterTasks0 + afterTasks1));
                                                            setSurplusActivities(j0,surplus);
                                                        } else {
                                                            int surplus0 = getSurplusActivities(j0) + (beforeTasks0 - afterTasks0);
                                                            int surplus1 = getSurplusActivities(j1) + (beforeTasks1 - afterTasks1);
                                                            setSurplusActivities(j0,surplus0);
                                                            setSurplusActivities(j1,surplus1);
                                                        }

                                                    }


                                                }
                                            }
                                        }   // end for #3


                                    }
                                }
                            } // end for #2

                        } // end for #1

                    }
                }
            }
        } // end for #0

    }

    public void decrementDataUsersCell(int i, int m, int t, int value) {
		data.decrementUsersCell(i,m,t,value);
	}

	public void setSurplusActivities(int j, int value) {
        data.setSurplusActivities(j, value);
    }

    public int getSurplusActivities(int j) {
        return data.getSurplusActivites(j);
    }

}
