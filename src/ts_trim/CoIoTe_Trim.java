package ts_trim;

import heuristic.Data;
import heuristic.MyTabuSearch;
import org.coinor.opents.*;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CoIoTe_Trim implements MyTabuSearch {

    private int numCells;
    private int numCustTypes;
    private int numTimeSteps;
    private Data data;

    private CoIoTe_ObjectiveFunction objectiveFunction;
    private CoIoTe_Solution solution;
    private MoveManager moveManager;
    private TabuList tabuList;
    private org.coinor.opents.TabuSearch tabuSearch;
    private BestEverAspirationCriteria bestEverAspirationCriteria;
    private CoIoTe_SearchListener searchListener;

    private CoIoTe_Solution bestSolution;
    private int finalObjectiveFunction;

    public CoIoTe_Trim(int numCells, int numCustTypes, int numTimeSteps, Data data) {
        this.numCells = numCells;
        this.numCustTypes = numCustTypes;
        this.numTimeSteps = numTimeSteps;
        this.data = data;

        objectiveFunction = new CoIoTe_ObjectiveFunction(data, numCells, numCustTypes, numTimeSteps);
        moveManager = new CoIoTe_MoveManager(numCells, numCustTypes, numTimeSteps);
        tabuList = new SimpleTabuList();
        bestEverAspirationCriteria = new BestEverAspirationCriteria();
    }

    @SuppressWarnings("unchecked")
	@Override
    public CoIoTe_Trim initSingleThread(Solution sol) {
        solution = (CoIoTe_Solution) sol;

        tabuSearch = new SingleThreadedTabuSearch(
                solution,
                moveManager,
                objectiveFunction,
                tabuList,
                bestEverAspirationCriteria,
                false);

        return this;
    }

    @SuppressWarnings("unchecked")
	@Override
    public CoIoTe_Trim initMultiThread(Solution sol, int threads) {
        solution = (CoIoTe_Solution) sol;

        tabuSearch = new MultiThreadedTabuSearch(
                solution,
                moveManager,
                objectiveFunction,
                tabuList,
                bestEverAspirationCriteria,
                false);
        ((MultiThreadedTabuSearch)tabuSearch).setThreads(threads);

        return this;
    }

    @SuppressWarnings("unchecked")
	@Override
    public CoIoTe_Trim setIterationToGo(int n) {
        tabuSearch.setIterationsToGo(n);
        return this;
    }

    @SuppressWarnings("unchecked")
	@Override
    public CoIoTe_Trim startSolving() {
        tabuSearch.startSolving();
        bestSolution = (CoIoTe_Solution) tabuSearch.getBestSolution();
        finalObjectiveFunction = (int)tabuSearch.getObjectiveFunction().evaluate(bestSolution, null)[0];
        return this;
    }

    @SuppressWarnings("unchecked")
	@Override
    public CoIoTe_Trim setUnimprovingMoves(int n) {
        searchListener = new CoIoTe_SearchListener(n);
        tabuSearch.addTabuSearchListener(searchListener);

        return this;
    }

    @Override
    public int getFinalObjectiveFunction() {
        return finalObjectiveFunction;
    }

    @Override
    public boolean hasBestSolution() {
        return (bestSolution != null);
    }

    @SuppressWarnings("unchecked")
	@Override
    public CoIoTe_Solution getBestSolution() {
        return bestSolution;
    }

    @Override
    public double getObjectiveFunctionGap(int optimalObjFun) {
        DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
        df.setRoundingMode(RoundingMode.FLOOR);
        return new Double(df.format(Math.abs(finalObjectiveFunction - optimalObjFun) / optimalObjFun * 100));
    }

    @Override
    public String printBestSolution() {
        StringBuilder stringBuilder = new StringBuilder();
        int objF = 0, doneTasks = 0, todoTasks = 0, surplus = 0;
        for(int j = 0; j < numCells; j++) {
            for (int i = 0; i < numCells; i++)
                for (int m = 0; m < numCustTypes; m++)
                    for (int t = 0; t < numTimeSteps; t++)
                        if (bestSolution.getCell(i, j, m, t) > 0) {
                            //stringBuilder.append(i + ";" + j + ";" + m + ";" + t + ";" + bestSolution.getCell(i, j, m, t) + " - " + data.getCosts(i,j,m,t) + "\n");
                            objF += bestSolution.getCell(i, j, m, t) * data.getCosts(i, j, m, t);
                            doneTasks += bestSolution.getCell(i, j, m, t) * data.getNumActivities(m);
                        }
            if (bestSolution.getSurplusActivities(j) != 0)
                surplus += bestSolution.getSurplusActivities(j);
            todoTasks += data.getActivities(j);
        }
        return stringBuilder
                .append("TRIM Surplus tasks: " + surplus + "\n")
                .append("Objective function: " + objF + "\n")
                .append("Tasks done: " + doneTasks + "; Todo: " + todoTasks + "\n")
                .toString();
    }

}
