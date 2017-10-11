package heuristic;

import ts_trim.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Heuristic {

    // FEASIBILITY ENUM
    public enum FeasibleState {
        FEASIBLE,
        NOT_FEASIBLE_DEMAND,
        NOT_FEASIBLE_USERS
    }

    // ATTRIBUTES
	/* input data variables */
    private Data data;
    private int numCells;
    private int numTimeSteps;
    private int numCustTypes;

    /* solution variables */
    private boolean hasSolution = false;
    private CoIoTe_Solution bestSol;

    /* kpi variables */
    private int objFunctionValue = Integer.MAX_VALUE;
    private int[] numCustomers;


    // CONSTRUCTOR
    public Heuristic() {
        super();
    }


    // METHODS
    public void readFile(String path) {
        String line;
        String[] word;

        try(BufferedReader br = new BufferedReader(new FileReader(path))) {

            // FIRST LINE (cells number, time steps, customer types)
            line = br.readLine();
            word = line.split(" ");
            numCells = Integer.parseInt(word[0]);
            numTimeSteps = Integer.parseInt(word[1]);
            numCustTypes = Integer.parseInt(word[2]);

            // DATA STRUCTURE CREATION
            data = new Data(numCells, numTimeSteps, numCustTypes);

            // THIRD LINE (number of activities for a specific user)
            br.readLine(); // second line is empty
            line = br.readLine();
            word = line.split(" ");
            for(int m = 0; m < numCustTypes; m++)
                data.setNumActivities(m, Integer.parseInt(word[m]));

            // NEXT LINEs (costs)
            br.readLine(); // fourth line is empty
            for(int m = 0; m < numCustTypes; m++) {
                for(int t = 0; t < numTimeSteps; t++) {
                    br.readLine(); // line with m and t (useless)
                    for(int i = 0; i < numCells; i++) {
                        line = br.readLine();
                        word = line.split(" ");
                        for(int j = 0; j < numCells; j++) {
                            int cost = (int)Double.parseDouble(word[j]);
                            data.setCosts(i, j, m, t, cost);
                            data.addArch(j, new Arch(i, j, m, data.getNumActivities(m), t, cost));
                        }
                    }
                }
            }

            // NEXT LINEs (activities in a specific cell)
            br.readLine(); // this line is empty
            line = br.readLine();
            word = line.split(" ");
            for(int i = 0; i < numCells; i++)
                data.setActivities(i, Integer.parseInt(word[i]));

            // NEXT LINEs (number of users of type m, in cell i, during t)
            br.readLine(); // this line is empty
            for(int m = 0; m < numCustTypes; m++) {
                for(int t = 0; t < numTimeSteps; t++) {
                    br.readLine(); // line with m and t (useless)
                    line = br.readLine();
                    word = line.split(" ");
                    for(int i = 0; i < numCells; i++)
                        data.setUsersCell(i, m, t, Integer.parseInt(word[i]));
                }
            }

        }
        catch(FileNotFoundException fnfe) {
            System.err.println(fnfe.getLocalizedMessage());
            System.exit(1);
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
            System.exit(1);
        }
    }


    private class ThreadMultiStartSimple implements Runnable {

        // ATTRIBUTES
        private int index;	// thread number = index
        private CoIoTe_Solution[] solutionThread;	// used to save all solutions
        private int[] objFunThread;	// used to save all objective functions
        private int n;

        private int iterationsTrim1, iterationsTrim2, neighborhoodSize, unimprovingMoves, varietyCoefficient;

        // CONSTRUCTOR
        ThreadMultiStartSimple(CoIoTe_Solution[] solutionThread,
                                      int[] objFunThread, int index, int n, int vc) {
            this.solutionThread = solutionThread;
            this.n = n;
            this.objFunThread = objFunThread;
            this.index = index;
            this.varietyCoefficient = vc;

            initParameters();
        }

        private void initParameters() {
            iterationsTrim1 = 35;
            iterationsTrim2 = 2;
            neighborhoodSize = 10;
            unimprovingMoves = 1;
        }

        @Override
        public void run() {

            CoIoTe_Solution greedySolution = new CoIoTe_Solution(data, numCells, numCustTypes, numTimeSteps);
            if (!greedySolution.solveOrdered(varietyCoefficient,n,0))
                return;

            MyTabuSearch tabuSearch = new MyTabuSearchBuilder(MyTabuSearch.TABU_METHOD.TRIM)
                    .init(numCells, numCustTypes, numTimeSteps, data)
                    .initSingleThreadTabuSearch(greedySolution)
                    .setUnimprovingMoves(unimprovingMoves)
                    .setIterationToGo(iterationsTrim1)
                    .build()
                    .startSolving();

            if (!(hasSolution = tabuSearch.hasBestSolution()))
                return;

            solutionThread[index] = tabuSearch.getBestSolution();
            solutionThread[index].fourWaySwap(neighborhoodSize);

            tabuSearch = new MyTabuSearchBuilder(MyTabuSearch.TABU_METHOD.TRIM)
                    .init(numCells, numCustTypes, numTimeSteps, data)
                    .initSingleThreadTabuSearch(solutionThread[index])
                    .setIterationToGo(iterationsTrim2)
                    .build()
                    .startSolving();

            solutionThread[index] = tabuSearch.getBestSolution();
            objFunThread[index] = evaluateObjFunction(solutionThread[index]);
            hasSolution = true;
        }
    }

    private void solveSimpleMultiStart(int NT) {

        CoIoTe_Solution[] solutionThread = new CoIoTe_Solution[NT];
        int[] objFunThread = new int[NT];

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        executor.execute(new ThreadMultiStartSimple(solutionThread, objFunThread, 0, 0, 1));
        executor.execute(new ThreadMultiStartSimple(solutionThread, objFunThread, 1, numCells - 1, 1));
        if (NT > 2)
            executor.execute(new ThreadMultiStartSimple(solutionThread, objFunThread, 2, 0, 7));


        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            if (hasSolution) {
                for (int i = 0; i < NT; i++) {
                	if(solutionThread[i] != null) {
                		if (objFunThread[i] < this.objFunctionValue) {
                            this.objFunctionValue = objFunThread[i];
                            this.bestSol = solutionThread[i];
                        }
                	}
                    
                }
            }

            if (!hasSolution)
                solveMultiStartH();

        } catch (InterruptedException e) {
            System.out.println("Error in a thread!");
            e.printStackTrace();
        }

    }

    private class ThreadMultiStartH implements Runnable {

        // ATTRIBUTES
        private int index;	// thread number = index
        private CoIoTe_Solution greedySolution;
        private CoIoTe_Solution[] solutionThread;	// used to save all solutions
        private int[] objFunThread;	// used to save all objective functions

        private int iterationsTaken, iterationsTrim;

        // CONSTRUCTOR
        ThreadMultiStartH(CoIoTe_Solution greedySolution, CoIoTe_Solution[] solutionThread,
                                    int[] objFunThread, int index) {
            this.solutionThread = solutionThread;
            this.greedySolution = greedySolution;
            this.objFunThread = objFunThread;
            this.index = index;

            initParameters();
        }

        private void initParameters() {
            if (numCells >= 100) {
                iterationsTaken = 5;
                iterationsTrim = 2;
            } else {
                iterationsTaken = 5;
                iterationsTrim = 0;
            }
        }

        @Override
        public void run() {

            for (int i = 0; i < iterationsTaken; i++)
                greedySolution.takenSwap();

            MyTabuSearch tabuSearch = new MyTabuSearchBuilder(MyTabuSearch.TABU_METHOD.TRIM)
                    .init(numCells, numCustTypes, numTimeSteps, data)
                    .initSingleThreadTabuSearch(greedySolution)
                    .setIterationToGo(iterationsTrim)
                    .build()
                    .startSolving();

            solutionThread[index] = tabuSearch.getBestSolution();
            objFunThread[index] = tabuSearch.getFinalObjectiveFunction();
            hasSolution = true;
        }
    }

    private void solveMultiStartH() {
        int NT = 8;
        Runnable[] thread = new ThreadMultiStartH[NT];
        CoIoTe_Solution[] solutionThread = new CoIoTe_Solution[NT];
        int[] objFunThread = new int[NT];
        int[] coefficient = {1, 1, -1, -1, 1, -1, -1, 1};

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < NT/2; i++) {

            int iThreadTrue = i * 2;
            int iThreadFalse = i * 2 + 1;

            CoIoTe_Solution greedySolution = new CoIoTe_Solution(data, numCells, numCustTypes, numTimeSteps);
            greedySolution.solveOrderedH(coefficient[i*2],coefficient[i*2+1],true);
            thread[iThreadTrue] = new ThreadMultiStartH(greedySolution, solutionThread, objFunThread, iThreadTrue);

            greedySolution = new CoIoTe_Solution(data, numCells, numCustTypes, numTimeSteps);
            greedySolution.solveOrderedH(coefficient[i*2],coefficient[i*2+1],false);
            thread[iThreadFalse] = new ThreadMultiStartH(greedySolution, solutionThread, objFunThread, iThreadFalse);

            executor.execute(thread[iThreadTrue]);
            executor.execute(thread[iThreadFalse]);
        }

        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            if (hasSolution) {
                for (int i = 0; i < NT; i++) {
                	if(solutionThread[i] != null) {
                		if (objFunThread[i] < this.objFunctionValue) {
                            this.objFunctionValue = objFunThread[i];
                            this.bestSol = solutionThread[i];
                        }
                	}
                    
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Error in a thread!");
            e.printStackTrace();
        }

    }

    private class ThreadMultiStartVariety implements Runnable {

        // ATTRIBUTES
        private int index;	// thread number = index
        private CoIoTe_Solution[] solutionThread;	// used to save all solutions
        private int[] objFunThread;	// used to save all objective functions

        private int neighboorhoodSize, unimprovingMoves1, unimprovingMoves2;
        private int iterationsFree1, iterationsTaken1, iterationsTrim1, iterationsFree2, iterationsTrim2;
        private int n, varietyCoefficient, seed;

        // CONSTRUCTOR
        ThreadMultiStartVariety(int n, int varietyCoefficient, CoIoTe_Solution[] solutionThread, int[] objFunThread, int index, int seed) {
            this.solutionThread = solutionThread;
            this.objFunThread = objFunThread;
            this.index = index;
            this.varietyCoefficient = varietyCoefficient;
            this.n = n;
            this.seed = seed;

            initParameters();
        }

        private void initParameters() {

            if(numCells <= 30) {
                iterationsFree1 = 1;
                iterationsTaken1 = 3;
                iterationsTrim1 = 50;
                iterationsFree2 = 40;
                iterationsTrim2 = 10;
                neighboorhoodSize = 30;
                unimprovingMoves1 = 15;
                unimprovingMoves2 = 5;
            }
            else {
                iterationsFree1 = 55;
                iterationsTaken1 = 40;
                iterationsTrim1 = 30;
                iterationsFree2 = 4;
                iterationsTrim2 = 10;
                neighboorhoodSize = 20;
                unimprovingMoves1 = 10;
                unimprovingMoves2 = 5;
            }

        }

        // METHODS
        @Override
        public void run() {

            CoIoTe_Solution greedySolution = new CoIoTe_Solution(data, numCells, numCustTypes, numTimeSteps);
            if (!greedySolution.solveOrdered(varietyCoefficient,n, seed))
                return;

            MyTabuSearch tabuSearch = new MyTabuSearchBuilder(MyTabuSearch.TABU_METHOD.FREE)
                    .init(numCells, numCustTypes, numTimeSteps, data)
                    .initSingleThreadTabuSearch(greedySolution)
                    .setUnimprovingMoves(unimprovingMoves1)
                    .setIterationToGo(iterationsFree1)
                    .build()
                    .startSolving();

            solutionThread[index] = tabuSearch.getBestSolution();

            tabuSearch = new MyTabuSearchBuilder(MyTabuSearch.TABU_METHOD.TAKEN)
                    .init(numCells, numCustTypes, numTimeSteps, data)
                    .initSingleThreadTabuSearch(solutionThread[index])
                    .setUnimprovingMoves(unimprovingMoves1)
                    .setIterationToGo(iterationsTaken1)
                    .build()
                    .startSolving();

            solutionThread[index] = tabuSearch.getBestSolution();

            tabuSearch = new MyTabuSearchBuilder(MyTabuSearch.TABU_METHOD.TRIM)
                    .init(numCells, numCustTypes, numTimeSteps, data)
                    .initSingleThreadTabuSearch(solutionThread[index])
                    .setUnimprovingMoves(unimprovingMoves1)
                    .setIterationToGo(iterationsTrim1)
                    .build()
                    .startSolving();

            solutionThread[index] = tabuSearch.getBestSolution();

            solutionThread[index].fourWaySwap(neighboorhoodSize);
            solutionThread[index].threeWaySwap(neighboorhoodSize);
            solutionThread[index].fourWaySwap(neighboorhoodSize);
            solutionThread[index].threeWaySwap(neighboorhoodSize);

            tabuSearch = new MyTabuSearchBuilder(MyTabuSearch.TABU_METHOD.FREE)
                    .init(numCells, numCustTypes, numTimeSteps, data)
                    .initSingleThreadTabuSearch(solutionThread[index])
                    .setUnimprovingMoves(unimprovingMoves1)
                    .setIterationToGo(iterationsFree2)
                    .build()
                    .startSolving();

            solutionThread[index] = tabuSearch.getBestSolution();

            tabuSearch = new MyTabuSearchBuilder(MyTabuSearch.TABU_METHOD.TRIM)
                    .init(numCells, numCustTypes, numTimeSteps, data)
                    .initSingleThreadTabuSearch(solutionThread[index])
                    .setUnimprovingMoves(unimprovingMoves2)
                    .setIterationToGo(iterationsTrim2)
                    .build()
                    .startSolving();

            solutionThread[index] = tabuSearch.getBestSolution();
            solutionThread[index].fourWaySwap(neighboorhoodSize);

            tabuSearch = new MyTabuSearchBuilder(MyTabuSearch.TABU_METHOD.TRIM)
                    .init(numCells, numCustTypes, numTimeSteps, data)
                    .initSingleThreadTabuSearch(solutionThread[index])
                    .setUnimprovingMoves(unimprovingMoves2)
                    .setIterationToGo(iterationsTrim2)
                    .build()
                    .startSolving();

            solutionThread[index] = tabuSearch.getBestSolution();
            objFunThread[index] = tabuSearch.getFinalObjectiveFunction();
            hasSolution = true;
        }
    }

    private void solveMultiStartVariety(int[] bestVC, int[][] bestSeed) {

        int NT = bestVC.length;
        int c = 10;
        int rTH = bestSeed.length * c;
        CoIoTe_Solution[] solutionThread = new CoIoTe_Solution[NT + rTH];
        int[] objFunThread = new int[NT + rTH];
        Runnable[] thread = new ThreadMultiStartVariety[NT + rTH];

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < NT; i++) {
            int n = (i < NT / 2) ? 0 : numCells - 1;
            thread[i] = new ThreadMultiStartVariety(n, bestVC[i], solutionThread, objFunThread, i, 0);
            executor.execute(thread[i]);
        }

        for (int i = 0; i < bestSeed.length; i++) {
            for (int j = 0; j < bestSeed[i].length; j++) {
                thread[NT + (i * c) + j] = new ThreadMultiStartVariety(0, i + 1, solutionThread, objFunThread, (NT + (i * c) + j), bestSeed[i][j]);
                executor.execute(thread[NT + (i * c) + j]);
            }
        }

        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            if (hasSolution) {
                for (int i = 0; i < NT + rTH; i++) {
                    if(solutionThread[i] != null) {
                        if (objFunThread[i] <= this.objFunctionValue) {
                            this.objFunctionValue = objFunThread[i];
                            this.bestSol = solutionThread[i];
                        }
                    }

                }
            }

            if (!hasSolution) {
                solveMultiStartH();
            }

        } catch (InterruptedException e) {
            System.out.println("Error in a thread!");
            e.printStackTrace();
        }

    }

    public void solve() {

        int[] bestVCs = {1, 5, 6, 14, /* mid */ 1, 3, 5, 6};
        int[] bestVCl = {3, 4, 10, /* mid */ 2, 3, 18};
        int[][] bestSeeds = {
                {2, 63, 101, 262},
                {3, 17},
                {17, 220}
        };
        int[][] bestSeedl = {
                {16, 123},
                {16, 29, 33, 45, 47, 60, 118},
                {38, 40, 52, 91, 192, 260, 269}
        };

        if (numCells <= 30 && numTimeSteps == 1)
            solveMultiStartVariety(bestVCs,bestSeeds);
        else if (numCells <= 100 && numCells > 30 && numTimeSteps == 1)
            solveMultiStartVariety(bestVCl,bestSeedl);
        else if (numCells <= 100 && numCells > 30 && numTimeSteps <= 20 && numTimeSteps > 1)
            solveSimpleMultiStart(3);
        else
            solveSimpleMultiStart(2);

    }

    public void printTimings(String inFileName, float time) {
        System.out.println("Instance: " + inFileName);
        System.out.println("Time: " + time + " seconds");
        System.out.println("Obj function: " + evaluateObjFunction(bestSol) + "\n");
    }

    public void makeKpi() {

        if (!hasSolution)
            return;

        numCustomers = new int[numCustTypes];

        for (int i = 0; i < numCells; i++)
            for (int j = 0; j < numCells; j++)
                for (int t = 0; t < numTimeSteps; t++)
                    for (int m = 0; m < numCustTypes; m++)
                        if (bestSol.getCell(i, j, m, t) > 0)
                            numCustomers[m] += bestSol.getCell(i, j, m, t);

    }

    public void writeSol(String path) {

        if(!hasSolution)
            return;

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {

            bw.write(numCells + ";" + numTimeSteps + ";" + numCustTypes);
            bw.newLine();
            for(int j = 0; j < numCells; j++)
                for(int i = 0; i < numCells; i++)
                    for(int m = 0; m < numCustTypes; m++)
                        for(int t = 0; t < numTimeSteps; t++)
                            if(bestSol.getCell(i, j, m, t) > 0) {
                                bw.write(i + ";" + j + ";" + m + ";" + t + ";" + String.valueOf(bestSol.getCell(i, j, m, t)));
                                bw.newLine();
                            }

        }
        catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    public void writeKpi(String path, String inputFileName, float time) {

        if(!hasSolution)
            return;

        // the 'true' value in the FileWriter constructor means "append"
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(path, true))) {
            bw.write(inputFileName + ";" + String.valueOf(time) + ";" + String.valueOf(Math.round(objFunctionValue)) + ";");
            for(int m = 0; m < numCustTypes; m++) {
                bw.write(String.valueOf(numCustomers[m]));
                if(m != numCustTypes - 1) {
                    bw.write(";");
                }
            }
            bw.newLine();

        }
        catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
    }

    private int evaluateObjFunction(CoIoTe_Solution solution) {
        int objFunctionValue = 0;

        for(int j = 0; j < numCells; j++) {
            for(int i = 0; i < numCells; i++) {
                for(int m = 0; m < numCustTypes; m++) {
                    for(int t = 0; t < numTimeSteps; t++) {
                        if(solution.getCell(i, j, m, t) != 0) {
                            objFunctionValue += solution.getCell(i, j, m, t) * data.getCosts(i, j, m, t);
                        }
                    }
                }
            }
        }

        return objFunctionValue;
    }

    public FeasibleState isFeasible(String path) {
        int[][][][] solution;
        String line;
        String[] word;
        int expr;
        FeasibleState fs = FeasibleState.FEASIBLE;

        try(BufferedReader br = new BufferedReader(new FileReader(path))) {

            // FIRST LINE (cells number, time steps, customer types)
            line = br.readLine();
            word = line.split("\\s*\\t*;\\s*\\t*");
            numCells = Integer.parseInt(word[0]);
            numTimeSteps = Integer.parseInt(word[1]);
            numCustTypes = Integer.parseInt(word[2]);

            // SOLUTION STRUCTURE CREATION (0-init not needed in Java)
            solution = new int[numCells][numCells][numCustTypes][numTimeSteps];

            // NEXT LINEs (solution)
            while((line = br.readLine()) != null) {
                word = line.split("\\s*\\t*;\\s*\\t*");
                int i = Integer.parseInt(word[0]);
                int j = Integer.parseInt(word[1]);
                int m = Integer.parseInt(word[2]);
                int t = Integer.parseInt(word[3]);
                solution[i][j][m][t] = Integer.parseInt(word[4]);
            }

            // DEMAND CHECK
            for (int i = 0; i < numCells; i++) {
                expr = 0;
                for (int j = 0; j < numCells; j++) {
                    for (int m = 0; m < numCustTypes; m++) {
                        for (int t = 0; t < numTimeSteps; t++) {
                            expr += data.getNumActivities(m) * solution[j][i][m][t];
                        }
                    }
                }

                if(expr < data.getActivities(i))
                    return FeasibleState.NOT_FEASIBLE_DEMAND;
            }

            // MAX NUMBER OF USERS CHECK
            for (int i = 0; i < numCells; i++) {
                for (int m = 0; m < numCustTypes; m++) {
                    for (int t = 0; t < numTimeSteps; t++) {
                        expr = 0;
                        for (int j = 0; j < numCells; j++) {
                            expr += solution[i][j][m][t];
                        }

                        if (expr > data.getUsersCell(i,m,t))
                            return FeasibleState.NOT_FEASIBLE_USERS;
                    }
                }
            }

            return FeasibleState.FEASIBLE;

        }
        catch(FileNotFoundException fnfe) {
            System.err.println(fnfe.getLocalizedMessage());
            System.exit(1);
        }
        catch(IOException ioe) {
            System.err.println(ioe.getMessage());
            System.exit(1);
        }

        return fs;
    }

}
