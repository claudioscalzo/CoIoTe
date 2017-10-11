package ts_trim;

import heuristic.Data;
import org.coinor.opents.Move;
import org.coinor.opents.ObjectiveFunction;
import org.coinor.opents.Solution;

@SuppressWarnings("serial")
class CoIoTe_ObjectiveFunction implements ObjectiveFunction {

	// ATTRIBUTES
	private Data data;
	private int numCells, numCustTypes, numTimeSteps;
	
	// CONSTRUCTOR
	CoIoTe_ObjectiveFunction(Data data, int numCells, int numCustTypes, int numTimeSteps) {
		super();
		this.data = data;
		this.numCells = numCells;
		this.numCustTypes = numCustTypes;
		this.numTimeSteps = numTimeSteps;
	}
	
	// METHODS
	@Override
	public double[] evaluate(Solution sol, Move mov) {
		CoIoTe_Solution solution = (CoIoTe_Solution)sol;
		int objFunctionValue = 0;
		
		if(mov == null) {	
			// Objective function evaluation from scratch
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

		}
		else {
			// Incremental objective function evaluation
			CoIoTe_Move move = (CoIoTe_Move)mov;
			
			int i = move.getI();
			int j = move.getJ();
			int m = move.getM();
			int t = move.getT();
			int i1 = move.getI1();
			int t1 = move.getT1();
			int m1 = move.getM1();
			int q1 = move.getQ1();
			int q2 = move.getQ2();
			
			// METHOD TRIM
			int obj1 = q1 * data.getCosts(i, j, m, t);
			int obj2 = q2 * data.getCosts(i1, j, m1, t1);
			int objDiff = obj1 + obj2;

			objFunctionValue = (int)sol.getObjectiveValue()[0] + objDiff;
		}
		
		return new double[] {(double)objFunctionValue};
	}
	
}
