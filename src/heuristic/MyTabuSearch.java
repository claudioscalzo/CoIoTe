package heuristic;

import org.coinor.opents.Solution;

public interface MyTabuSearch {

    enum TABU_METHOD {
        FREE,
        TAKEN,
        TRIM
    }

    <T> T initSingleThread(Solution solution);

    <T> T initMultiThread(Solution solution, int threads);

    <T> T setIterationToGo(int n);

    <T> T setUnimprovingMoves(int n);

    <T> T startSolving();

    int getFinalObjectiveFunction();

    boolean hasBestSolution();

    <T> T getBestSolution();

    double getObjectiveFunctionGap(int optimalObjFun);

    String printBestSolution();

}
