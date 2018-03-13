package heuristic;

import org.coinor.opents.*;
import ts_free.CoIoTe_Free;
import ts_trim.CoIoTe_Trim;
import ts_taken.CoIoTe_Taken;

public class MyTabuSearchBuilder {

    public enum THREAD_METHOD {
        SINGLE,
        MULTI
    }

    private int numCells;
    private int numCustTypes;
    private int numTimeSteps;
    private Data data;

    private MyTabuSearch tabuSearch;
    private MyTabuSearch.TABU_METHOD tabuMethod;
    private Solution solution;
    private int iterationToGo;
    private int unimprovingMoves = 0;
    private int threads = 2;
    private THREAD_METHOD method;

    public MyTabuSearchBuilder(MyTabuSearch.TABU_METHOD tabuMethod) {
        this.tabuMethod = tabuMethod;
    }

    public MyTabuSearchBuilder init(int numCells, int numCustTypes, int numTimeSteps, Data data) {
        this.numCells = numCells;
        this.numCustTypes = numCustTypes;
        this.numTimeSteps = numTimeSteps;
        this.data = data;
        return this;
    }

    public MyTabuSearchBuilder initSingleThreadTabuSearch(Solution solution) {
        this.solution = solution;
        this.method = THREAD_METHOD.SINGLE;
        return this;
    }

    public MyTabuSearchBuilder initMultiThreadTabuSearch(Solution solution, int threads) {
        this.solution = solution;
        this.threads = threads;
        this.method = THREAD_METHOD.MULTI;
        return this;
    }

    public MyTabuSearchBuilder setIterationToGo(int n) {
        this.iterationToGo = n;
        return this;
    }

    public MyTabuSearchBuilder setUnimprovingMoves(int n) {
        unimprovingMoves = n;
        return this;
    }

    public MyTabuSearch build() {
    	
        switch (tabuMethod) {
            case FREE:
                tabuSearch = new CoIoTe_Free(numCells, numCustTypes, numTimeSteps, data);
                break;
            case TAKEN:
                tabuSearch = new CoIoTe_Taken(numCells, numCustTypes, numTimeSteps, data);
                break;
            case TRIM:
                tabuSearch = new CoIoTe_Trim(numCells, numCustTypes, numTimeSteps, data);
                break;
        }

        if (method == THREAD_METHOD.SINGLE)
            tabuSearch.initSingleThread(solution);
        else
            tabuSearch.initMultiThread(solution,threads);
        tabuSearch.setIterationToGo(iterationToGo);
        tabuSearch.setUnimprovingMoves(unimprovingMoves);
        
        return tabuSearch;
    }

}
