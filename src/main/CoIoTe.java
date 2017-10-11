package main;

import heuristic.*;
import java.nio.file.Paths;

public class CoIoTe {
	
	public enum Option {
		SOLVE,
		TEST
	}
	
	public static void main(String[] args) {
		
		Heuristic h = new Heuristic();
		Option option = Option.SOLVE;
		String inPath = null;
		String kpiPath = null;
		String solPath = null;
		
		
		// COMMAND LINE CHECK		
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("-test")) {
				option = Option.TEST;
			} else if(args[i].equals("-i")) {
				if(i+1 < args.length)
					inPath = args[++i];
			} else if(args[i].equals("-o")) {
				if(i+1 < args.length)
					kpiPath = args[++i];
			} else if(args[i].equals("-s")) {
				if(i+1 < args.length)
					solPath = args[++i];
			}
		}
		
		if((option == Option.SOLVE && (inPath == null || kpiPath == null))
		   ||
		   (option == Option.TEST && (inPath == null || solPath == null))) {
			
			System.err.println("Error in command line arguments! Usage:\n");
			System.err.println("To find a solution:");
			System.err.println("-solve -i <input_txt_file> -o <output_kpi_csv> -s <output_sol_txt>\n");
			System.err.println("To test a solution:");
			System.err.println("-test -i <input_txt_file> -s <sol_txt_file>");
			return;
		}

		
		// PROGRAM STARTS
		if(option == Option.SOLVE) {
			
			String inFileName = Paths.get(inPath).getFileName().toString();
			// if we don't want the file extension: inFileName.substring(0, inFileName.lastIndexOf("."));
			
			// LOAD THE INSTANCE
			h.readFile(inPath);

			
			// START SOLVING
			long startTime = System.currentTimeMillis();
			h.solve();
			long endTime = System.currentTimeMillis();
	        float time = ((float)(endTime - startTime))/1000;
	        
	        
	        // WRITE OUTPUT AND SOLUTION FILES
			h.makeKpi();
			if(solPath != null)
				h.writeSol(solPath);
			h.writeKpi(kpiPath, inFileName, time);
			
			
			// PRINT OBJ FUNCTION VALUES AND TIMES
			h.printTimings(inFileName, time);
			
		}
		else if (option == Option.TEST) {

			String inFileName = Paths.get(inPath).getFileName().toString();
			// if we don't want the file extension: inFileName.substring(0, inFileName.lastIndexOf("."));
			
			// LOAD THE INSTANCE
			h.readFile(inPath);
			
			
			// CHECK THE FEASIBILITY
			Heuristic.FeasibleState fs = h.isFeasible(solPath);
			
			System.out.println("Instance: " + inFileName);
			switch(fs) {
			case FEASIBLE:
				System.out.println("The solution is feasible!\n");
				break;
			case NOT_FEASIBLE_DEMAND:
				System.out.println("The solution is not feasible: demand not satisfied.\n");
				break;
			case NOT_FEASIBLE_USERS:
				System.out.println("The solution is not feasible: exceeded number of available users.\n");
				break;	
			}
		}

	}
	
}
