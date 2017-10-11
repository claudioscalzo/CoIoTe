package ts_taken;

import heuristic.Data;
import org.coinor.opents.Move;
import org.coinor.opents.MoveManager;
import org.coinor.opents.Solution;
import ts_trim.CoIoTe_Solution;

@SuppressWarnings("serial")
class CoIoTe_MoveManagerTaken implements MoveManager {

    // ATTRIBUTES
    private int numCells;
    private int numCustTypes;
    private int numTimeSteps;

    // CONSTRUCTOR
    CoIoTe_MoveManagerTaken(int numCells, int numCustTypes, int numTimeSteps) {
        super();
        this.numCells = numCells;
        this.numCustTypes = numCustTypes;
        this.numTimeSteps = numTimeSteps;
    }


    //METHOD SWAP TAKEN USERS
    @Override
    public Move[] getAllMoves(Solution sol) {
        CoIoTe_Solution solution = (CoIoTe_Solution)sol;
        Data data = solution.getData();
        int offsetJ = (numCells < 100) ? numCells/2 : 20;
        int offsetI = 20;

        int bufferSize = numCells * offsetJ * numCustTypes * numTimeSteps;
        if (bufferSize > 3000)
            bufferSize = 3000;
        Move[] buffer = new Move[bufferSize];
        int count = 0;

        for(int j = 0; j < numCells; j++) {
            for(int i = 0; i < numCells; i++) {
                for(int m = 0; m < numCustTypes; m++) {
                    for(int t = 0; t < numTimeSteps; t++) {

                        if(solution.getCell(i, j, m, t) == 0)
                            continue;

                        double cost0 = data.getCosts(i,j,m,t) / data.getNumActivities(m);

                        int j1 = (j > offsetJ) ? j - offsetJ : 0;
                        for(; j1 < numCells && j1 <= (j + offsetJ); j1++) {
                            if (j1 == j || j1 == i)
                                continue;

                            int i1 = (i > offsetI) ? i - offsetI : 0;
                            for(; i1 < numCells && i1 <= (i + offsetI); i1++) {
                                if (i1 == j1 || i1 == j || i1 == i)
                                    continue;

                                for (int t1 = 0; t1 < numTimeSteps; t1++) {
                                    for (int m1 = 0; m1 < numCustTypes; m1++) {
                                        if(solution.getCell(i1, j1, m1, t1) == 0)
                                            continue;

                                        if(count >= buffer.length)
                                            break;

                                        double cost1 = data.getCosts(i1,j1,m1,t1) / data.getNumActivities(m1);

                                        if (cost1 > cost0)
                                            continue;

                                        //check the best number of people to swap
                                        int quantityTask = solution.getCell(i, j, m, t) * data.getNumActivities(m);

                                        if(solution.getCell(i1, j1, m1, t1) * data.getNumActivities(m1) < quantityTask)
                                            quantityTask = solution.getCell(i1, j1, m1, t1) * data.getNumActivities(m1);

                                        //check for better algorithm... Task1 and Task2 MUST be equal to have feasible solution
                                        while (quantityTask % data.getNumActivities(m) != 0 || quantityTask % data.getNumActivities(m1) != 0)
                                            quantityTask--;

                                        if(quantityTask == 0)
                                            continue;

                                        int quantity1 = quantityTask / data.getNumActivities(m);
                                        int quantity2 = quantityTask / data.getNumActivities(m1);

                                        buffer[count++] = new CoIoTe_MoveTaken(i, j, m, t, j1, i1, t1, m1, quantity1, quantity2);

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
