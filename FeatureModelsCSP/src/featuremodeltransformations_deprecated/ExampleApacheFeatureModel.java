/**
 * 
 */
package featuremodeltransformations_deprecated;

import static choco.Choco.*;

import choco.kernel.solver.Solver;
import choco.cp.solver.CPSolver;

import choco.Choco;
import choco.cp.model.CPModel;
import choco.kernel.model.Model;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.model.constraints.Constraint;

import java.util.*;

/**
 * @author Rafael Olaechea
 *
 */
public class ExampleApacheFeatureModel {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 *  Apache Feature model.
		 *  	Finding the optimal configuration for Apache.
		 */

		int FeaturesCount = 9;
		int ObjectivesCount = 1;
		String[] FeatureNames = {"Base", "HostnameLookups", "KeepAlive", "EnableSendfile", "FollowSymLinks",
				"AccessLog", "ExtendedStatus", "InMemory", "Handle"};
		int[] FeaturePerformanceContributions =  {1200, -210, 840, 120, 0,-120, -90, 210, 30};
			//{150, -26, 105, 15, 0, -15, -11, 26, 4};
		//(scaled down as in clafer to multi-objective alloy
		
		String [] ObjectiveNames = {"total_performance"};
		int Base_index = 0;
		int InMemory_Index = 7;
		int Handle_Index = 8;
		
		Model m = new CPModel();

		IntegerVariable [] variables = new IntegerVariable[FeaturesCount + ObjectivesCount];
		for (int i = 0; i < FeaturesCount; i++) {
			variables[i] = Choco.makeIntVar(FeatureNames[i], 0, 1);
	    }

		// Adding total_performance variables
		int min_goal = 0; 
		int max_goal = 0 ; 
		for (int contribution: FeaturePerformanceContributions){
			if (contribution > 0 )
				max_goal += contribution;
			else 
				min_goal += contribution;
		}
		
		variables[9] = makeIntVar(ObjectiveNames[0], min_goal, max_goal, "cp:bound");

		
		// Adding Mandatory Constraints.
		m.addConstraint(Choco.eq(variables[Base_index], 1));		
		
		// Adding cross-tree Constraints.
		// InMemory
		// 		[! Handle ]
		m.addConstraint(Choco.implies(Choco.eq(variables[InMemory_Index], 1), Choco.eq(variables[Handle_Index], 0)));
	
				
		//Adding Constraint for total performance.
		// [total_performance = sum IMeasurement.performance ]
		m.addConstraint(Choco.eq(variables[9], Choco.scalar(FeaturePerformanceContributions, Arrays.copyOfRange(variables, 0, 9))));

		
		//Solve and print answer.
		Solver s = new CPSolver();
		
		s.read(m);
		s.maximize(s.getVar(variables[9]), false);
		
		System.out.println("ApacheConfiguration");
		for (int i = 0; i < FeaturesCount; i++) {
			if ( s.getVar(variables[i]).getVal() > 0 ){
				System.out.println("\t" + s.getVar(variables[i]).getName());								
			}
	    }
		System.out.println("\t" + s.getVar(variables[9]).getName() + " = " + s.getVar(variables[9]).getVal() );		
	}	
}
