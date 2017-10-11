package ts_free;

import org.coinor.opents.*;
import heuristic.Data;
import ts_trim.CoIoTe_Solution;

@SuppressWarnings("serial")
class CoIoTe_MoveManagerFree implements MoveManager {

    // ATTRIBUTES
    private int numCells;
    private int numCustTypes;
    private int numTimeSteps;

    // CONSTRUCTOR
    CoIoTe_MoveManagerFree(int numCells, int numCustTypes, int numTimeSteps) {
        super();
        this.numCells = numCells;
        this.numCustTypes = numCustTypes;
        this.numTimeSteps = numTimeSteps;
    }

    //METHOD SWAP FREE USERS
    @Override
    public Move[] getAllMoves(Solution sol) {
        CoIoTe_Solution solution = (CoIoTe_Solution)sol;
        int offsetI = 20;

        Data data = solution.getData();
        int bufferSize = numCells * offsetI * numCustTypes * numTimeSteps;
        if(bufferSize > 1000)
        	bufferSize = 1000;
        Move[] buffer = new Move[bufferSize];
        int count = 0;

        for(int j = 0; j < numCells; j++) {
            for(int i = 0; i < numCells; i++) {
                for(int m = 0; m < numCustTypes; m++) {
                    for(int t = 0; t < numTimeSteps; t++) {
                        if (solution.getCell(i,j,m,t) == 0)
                            continue;

                        double cost0 = data.getCosts(i,j,m,t) / data.getNumActivities(m);

                        //different cells
                        int i1 = (i > offsetI) ? i - offsetI: 0;
                        for(; i1 < numCells && i1 <= (i + offsetI); i1++) {
                            if (i1 == j)
                                continue;

                            for(int t1 = 0; t1 < numTimeSteps; t1++) {
                                for(int m1 = 0; m1 < numCustTypes; m1++) {
                                    if(data.getUsersCell(i1, m1, t1) == 0)
                                        continue;

                                    if(count >= buffer.length)
                                        break;

                                    double cost1 = data.getCosts(i1,j,m1,t1) / data.getNumActivities(m1);

                                    if (cost1 > cost0)
                                        continue;

                                    //check the best number of people to swap
                                    int quantityTask = solution.getCell(i, j, m, t) * data.getNumActivities(m);

                                    if(data.getUsersCell(i1, m1, t1) * data.getNumActivities(m1) < quantityTask)
                                        quantityTask = data.getUsersCell(i1, m1, t1) * data.getNumActivities(m1);

                                    //check for better algorithm... Task1 and Task2 MUST be equal
                                    while (quantityTask % data.getNumActivities(m)  != 0 || quantityTask % data.getNumActivities(m1) != 0 )
                                        quantityTask--;

                                    int quantity1 = quantityTask / data.getNumActivities(m);
                                    int quantity2 = quantityTask / data.getNumActivities(m1);

                                    if(quantityTask == 0)
                                        continue;

                                    buffer[count++] = new CoIoTe_MoveFree(i, j, m, t, i1, t1, m1, quantity1, quantity2);
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
