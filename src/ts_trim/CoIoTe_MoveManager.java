package ts_trim;

import heuristic.Data;
import org.coinor.opents.Move;
import org.coinor.opents.MoveManager;
import org.coinor.opents.Solution;

@SuppressWarnings("serial")
class CoIoTe_MoveManager implements MoveManager {

    private enum CASE {
        CUSTOMER1_CHEAPER,
        CUSTOMER2_CHEAPER,
        NO_CHANGE
    }

    // ATTRIBUTES
    private int numCells;
    private int numCustTypes;
    private int numTimeSteps;

    // CONSTRUCTOR
    CoIoTe_MoveManager(int numCells, int numCustTypes, int numTimeSteps) {
        super();
        this.numCells = numCells;
        this.numCustTypes = numCustTypes;
        this.numTimeSteps = numTimeSteps;
    }

    @Override
    public Move[] getAllMoves(Solution sol) {
        CoIoTe_Solution solution = (CoIoTe_Solution)sol;
        Data data = solution.getData();
        int bufferSize = numCells * numCells * numCustTypes * numTimeSteps;
        if (bufferSize > 6000)
            bufferSize = 6000;
        Move[] buffer = new Move[bufferSize];
        int count = 0;
        int neighborhoodSize = (numCells < 300) ? 20 : 10;

        for (int j = 0; j < numCells; j++) {
            for (int i = 0; i < numCells; i++) {
                for (int m = 0; m < numCustTypes; m++) {
                    for (int t = 0; t < numTimeSteps; t++) {

                        if(solution.getCell(i, j, m, t) == 0)
                            continue;

                        double cost0 = data.getCosts(i,j,m,t) / data.getNumActivities(m);

                        int i1 = (i > neighborhoodSize) ? i - neighborhoodSize: 0;
                        for(; i1 < numCells && i1 < (i + neighborhoodSize); i1++) {

                            if (j == i1)
                                continue;

                            for (int t1 = 0; t1 < numTimeSteps; t1++) {
                                for (int m1 = 0; m1 < numCustTypes; m1++) {
                                    if (((i != i1) || (t1 != t) || (m1 != m)) && data.getUsersCell(i1,m1,t1) != 0) {

                                        if (count >= buffer.length)
                                            break;

                                        double cost1 = data.getCosts(i1,j,m1,t1) / data.getNumActivities(m1);

                                        if (cost1 > cost0 && solution.getCell(i1,j,m1,t1) == 0 && data.getSurplusActivites(j) == 0)
                                            continue;

                                        int surplusTasks = data.getSurplusActivites(j);
                                        int activities1 = data.getNumActivities(m);     // get number of tasks that could be perfomed by customer #1
                                        int activities2 = data.getNumActivities(m1);    // get number of tasks that could be perfomed by customer #2
                                        int currentTasks = solution.getCell(i,j,m,t) * activities1 + solution.getCell(i1,j,m1,t1) * activities2;  // total number of tasks currently performed
                                        int todoTasks = currentTasks + surplusTasks;    // number of tasks that has to be performed (surplus should be <= 0)
                                        int nCustomers1 = 0, nCustomers2 = 0;
                                        double ratio1 = (double)data.getCosts(i,j,m,t) / activities1;   // ratio cost per task for customer #1
                                        double ratio2 = (double)data.getCosts(i1,j,m1,t1) / activities2;    // ratio cost per task for customer #2

                                        CASE scenario = CASE.NO_CHANGE;
                                        if (ratio1 < ratio2)
                                            scenario = CASE.CUSTOMER1_CHEAPER;
                                        else if (ratio1 > ratio2)
                                            scenario = CASE.CUSTOMER2_CHEAPER;
                                        else if (ratio1 == ratio2 && surplusTasks != 0)      // if #1 and #2 cost the same but there is a surplus of tasks
                                            if (activities1 > activities2)
                                                scenario = CASE.CUSTOMER1_CHEAPER;
                                            else if (activities1 < activities2)
                                                scenario = CASE.CUSTOMER2_CHEAPER;

                                        int currentCellTasks;
                                        switch (scenario) {
                                            case CUSTOMER1_CHEAPER:
                                                currentCellTasks = solution.getCell(i1,j,m1,t1);
                                                while (todoTasks % activities1 != 0 && todoTasks > currentCellTasks)
                                                    todoTasks -= activities2;     // get how many tasks should be performed by customer #1 (trying to assign as many task as possible to #1)
                                                if (todoTasks < currentCellTasks)
                                                    continue;
                                                nCustomers1 = todoTasks / activities1;     // # of necessary customer #1
                                                if (nCustomers1 > solution.getCell(i,j,m,t) + data.getUsersCell(i,m,t))
                                                    nCustomers1 = solution.getCell(i,j,m,t) + data.getUsersCell(i,m,t);
                                                nCustomers2 = (currentTasks + surplusTasks - nCustomers1 * activities1) / activities2;  // calculate #2
                                                break;
                                            case CUSTOMER2_CHEAPER:
                                                currentCellTasks = solution.getCell(i1,j,m1,t1);
                                                while (todoTasks % activities2 != 0 && todoTasks > currentCellTasks)
                                                    todoTasks -= activities1;     // get how many tasks should be performed by customer #2 (trying to assign as many task as possible to #2)
                                                if (todoTasks < currentCellTasks)
                                                    continue;
                                                nCustomers2 = todoTasks / activities2;     // # of necessary customer #2
                                                if (nCustomers2 > solution.getCell(i1,j,m1,t1) + data.getUsersCell(i1,m1,t1))
                                                    nCustomers2 = solution.getCell(i1,j,m1,t1) + data.getUsersCell(i1,m1,t1);
                                                nCustomers1 = (currentTasks + surplusTasks - nCustomers2 * activities2) / activities1;
                                                break;
                                            case NO_CHANGE:
                                                continue;
                                        }

                                        if ((nCustomers1 * activities1 + nCustomers2 * activities2) >= currentTasks + surplusTasks) {   // if the tasks which have to be done are satisfied
                                            nCustomers1 -= solution.getCell(i, j, m, t);   // get the number of customer #1 that has to be added
                                            nCustomers2 -= solution.getCell(i1, j, m1, t1);    // get the number of customer #2 that has to be added
                                            if (nCustomers1 <= data.getUsersCell(i, m, t) && nCustomers2 <= data.getUsersCell(i1, m1, t1))   // if there are enough available customer
                                                buffer[count++] = new CoIoTe_Move(i, j, m, t, i1, t1, m1, nCustomers1, nCustomers2);
                                        }

                                    }
                                }

                            }

                        }   // end for #1

                    }
                }
            }
        }   // end for #0

        Move[] moves = new Move[count];
        System.arraycopy(buffer, 0, moves, 0, count);

        return moves;
    }

}