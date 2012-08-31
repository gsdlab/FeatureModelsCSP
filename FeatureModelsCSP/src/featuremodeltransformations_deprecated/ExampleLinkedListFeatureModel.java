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
public class ExampleLinkedListFeatureModel {

	static final String[] FeatureNames = {"AbstractElement", "ElementA", "ElementB", "ElementC", "AbstractIterator",
			"ForwardIterator", "BackwardIterator", "AbstractSort", "BubbleSort", "MergeSort",
			"InsertionSort", "QuickSort", "print", "Measurement", "TCP_IP", 
			"SyntheticPerformanceOrMeasuruement", "Performance", "MemorySize","Base"};
	
	static final String [] ObjectiveNames = {"total_footprint"};;
	static final int[] FeatureContributions =  {-12, 12, 0, 0, 0,
			0, 1, 57, 17, 32,
			0, 22, 44, 484, 0,
			0, 37, 36, 455};
		
	private static int getIndex(String Name){
		int index = -1;
		for (int i = 0; i < FeatureNames.length; i++){
			if (FeatureNames[i] == Name)
					index = i;
		}
		return index;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 *  Apache Feature model.
		 *  	Finding the optimal configuration for Apache.
		 */

		int featuresCount = FeatureNames.length;
		int objectivesCount = ObjectiveNames.length;
						
		
		Model m = new CPModel();

		IntegerVariable [] variables = new IntegerVariable[featuresCount + objectivesCount];

		for (int i = 0; i < featuresCount; i++) {
			variables[i] = Choco.makeIntVar(FeatureNames[i], 0, 1);
	    }

		// Adding total_performance variables
		int min_goal = 0; 
		int max_goal = 0 ; 
		for (int contribution: FeatureContributions){
			if (contribution > 0 )
				max_goal += contribution;
			else 
				min_goal += contribution;
		}
		
		variables[featuresCount] = makeIntVar(ObjectiveNames[0], min_goal, max_goal, "cp:bound");

		
		// Adding Top-Level Mandatory Constraints.
		String [] TopLevelMandatory = {"AbstractElement", "AbstractIterator", "Base"};
		for (String featureName : TopLevelMandatory){
			m.addConstraint(Choco.eq(variables[getIndex(featureName)], 1));		
		}

		
		//Adding Mandatory Relations.
		String [][] MandatoryChildren = {{"Measurement", "TCP_IP"},{"Measurement", "SyntheticPerformanceOrMeasuruement"}};
		for (String [] ParentChildren: MandatoryChildren){		
			System.out.println("Adding Constraint " + ParentChildren[0] + " = " + ParentChildren[1]);
			m.addConstraint(Choco.eq(variables[getIndex(ParentChildren[0])], variables[getIndex(ParentChildren[1])] ));		
		}
		

		// Adding OR 
        //or SyntheticPerformanceOrMeasuruement
        //		Performance
		//		MemorySize
		IntegerVariable [] PerformanceOrMeasurement = {variables[getIndex("Performance")], variables[getIndex("MemorySize")]};
		m.addConstraint(Choco.ifOnlyIf(Choco.or(PerformanceOrMeasurement), Choco.eq(variables[getIndex("SyntheticPerformanceOrMeasuruement")], 1)));
		
		
		
		//Adding Exclusive 
		/*
		 * 
		 * 
		 * xor AbstractElement
                ElementA
                ElementB
                ElementC
		 * 
		 */		
		Choco.and(
				Choco.ifOnlyIf(	
							Choco.eq(variables[getIndex("ElementA")], 1)	, 
							Choco.and(
									Choco.not(Choco.eq(variables[getIndex("ElementB")], 1)),
									Choco.not(Choco.eq(variables[getIndex("ElementC")], 1)),
									Choco.eq(variables[getIndex("AbstractElement")], 1)
						)),
				Choco.ifOnlyIf(	
						Choco.eq(variables[getIndex("ElementB")], 1)	, 
						Choco.and(
								Choco.not(Choco.eq(variables[getIndex("ElementA")], 1)),
								Choco.not(Choco.eq(variables[getIndex("ElementC")], 1)),
								Choco.eq(variables[getIndex("AbstractElement")], 1)
					)),
				Choco.ifOnlyIf(	
						Choco.eq(variables[getIndex("ElementC")], 1)	, 
						Choco.and(
								Choco.not(Choco.eq(variables[getIndex("ElementA")], 1)),
								Choco.not(Choco.eq(variables[getIndex("ElementB")], 1)),
								Choco.eq(variables[getIndex("AbstractElement")], 1)
					))						
		);
		
		//Adding xor AbstractIterator.
		/*
		 	xor AbstractIterator
		 			ForwardIterator
		 			BackwardIterator
		 */
		Choco.and(
				Choco.ifOnlyIf(	
							Choco.eq(variables[getIndex("ForwardIterator")], 1)	, 
							Choco.and(
									Choco.not(Choco.eq(variables[getIndex("BackwardIterator")], 1)),
									Choco.eq(variables[getIndex("AbstractIterator")], 1)
						)),
				Choco.ifOnlyIf(	
						Choco.eq(variables[getIndex("BackwardIterator")], 1)	, 
						Choco.and(
								Choco.not(Choco.eq(variables[getIndex("ForwardIterator")], 1)),
								Choco.eq(variables[getIndex("AbstractIterator")], 1)
					))					
		);

		//Adding xor AbstractSort.
		/*
		 	xor AbstractSort
		 			BubbleSort
		 			MergeSort
		 			InsertionSort
		 			QuickSort
		 */
		Choco.and(
				Choco.ifOnlyIf(	
							Choco.eq(variables[getIndex("BubbleSort")], 1)	, 
							Choco.and(
									Choco.not(Choco.eq(variables[getIndex("MergeSort")], 1)),
									Choco.not(Choco.eq(variables[getIndex("InsertionSort")], 1)),
									Choco.not(Choco.eq(variables[getIndex("QuickSort")], 1)),									
									Choco.eq(variables[getIndex("AbstractSort")], 1)
						)),
				Choco.ifOnlyIf(	
						Choco.eq(variables[getIndex("MergeSort")], 1)	, 
						Choco.and(
								Choco.not(Choco.eq(variables[getIndex("BubbleSort")], 1)),
								Choco.not(Choco.eq(variables[getIndex("InsertionSort")], 1)),
								Choco.not(Choco.eq(variables[getIndex("QuickSort")], 1)),									
								Choco.eq(variables[getIndex("AbstractSort")], 1)

					)),
				Choco.ifOnlyIf(	
						Choco.eq(variables[getIndex("InsertionSort")], 1)	, 
						Choco.and(
								Choco.not(Choco.eq(variables[getIndex("BubbleSort")], 1)),
								Choco.not(Choco.eq(variables[getIndex("MergeSort")], 1)),
								Choco.not(Choco.eq(variables[getIndex("QuickSort")], 1)),									
								Choco.eq(variables[getIndex("AbstractSort")], 1)

					)),					
				Choco.ifOnlyIf(	
						Choco.eq(variables[getIndex("QuickSort")], 1)	, 
						Choco.and(
								Choco.not(Choco.eq(variables[getIndex("BubbleSort")], 1)),
								Choco.not(Choco.eq(variables[getIndex("MergeSort")], 1)),
								Choco.not(Choco.eq(variables[getIndex("InsertionSort")], 1)),									
								Choco.eq(variables[getIndex("AbstractSort")], 1)

					))					
					
		);
		
		
		// Adding cross-tree Constraints.
		// Measurement
		// 		[AbstractSort]
		m.addConstraint(Choco.implies(Choco.eq(variables[getIndex("Measurement")], 1), Choco.eq(variables[getIndex("AbstractSort")], 1)));
	
				
		//Adding Constraint for total footprint.
		// [total_footprint = sum IMeasurement.footprint ]
		m.addConstraint(Choco.eq(variables[featuresCount], Choco.scalar(FeatureContributions, Arrays.copyOfRange(variables, 0, featuresCount))));


		//m.addConstraint(Choco.eq(variables[getIndex("print")], 1));
		//m.addConstraint(Choco.eq(variables[getIndex("Measurement")], 1));
		
		//Solve and print answer.
		Solver s = new CPSolver();
		
		s.read(m);
		s.minimize(s.getVar(variables[featuresCount]), false);
		
		System.out.println("ApacheConfiguration");
		for (int i = 0; i < featuresCount; i++) {
			if ( s.getVar(variables[i]).getVal() > 0 ){
				System.out.println("\t" + s.getVar(variables[i]).getName());								
			}
	    }
		System.out.println("\t" + s.getVar(variables[featuresCount]).getName() + " = " + s.getVar(variables[featuresCount]).getVal() );		
	}	
}
