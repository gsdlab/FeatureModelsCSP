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

/**
 * @author Rafael Olaechea
 *
 */
public class ExampleCSPMagicCircles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 *  Magic Circles 3x3.
		 *  A magic square of order n is an arrangement of n2 numbers, usually distinct integers, in a
		 *   square, such that the n numbers in all rows, all columns, and both diagonals sum to the
		 *    same constant. A normal magic square contains the integers from 1 to n2.
		 */

		int n = 3;
		int SumToConstant = n * (n * n + 1) / 2;
		
		Model m = new CPModel();

		IntegerVariable[][] cells = new IntegerVariable[n][n];
		for (int i = 0; i < n; i++) {
		    for (int j = 0; j < n; j++) {
		       cells[i][j] = Choco.makeIntVar("cell" + j, 1, n * n);
		    } 
	    }
		
		// Adding Constraints over rows.
		 Constraint[] rows = new Constraint[n];
         for (int i = 0; i < n; i++) {
             rows[i] = Choco.eq(Choco.sum(cells[i]), SumToConstant);
         }
         

 		// Adding Constraints over columns.

         // first, get the columns, with a temporary array
         IntegerVariable[][] cellsDual = new IntegerVariable[n][n];
         for (int i = 0; i < n; i++) {
             for (int j = 0; j < n; j++) {
                 cellsDual[i][j] = cells[j][i];
             }
         }

         Constraint[] cols = new Constraint[n];
         for (int i = 0; i < n; i++) {
             cols[i] = Choco.eq(Choco.sum(cellsDual[i]), SumToConstant);
         }
         m.addConstraints(cols);
         
         // Adding Constraints over diagonals.
         IntegerVariable[][] diags = new IntegerVariable[2][n];
         for (int i = 0; i < n; i++) {
             diags[0][i] = cells[i][i];
             diags[1][i] = cells[i][(n - 1) - i];
         }
         m.addConstraint(Choco.eq(Choco.sum(diags[0]), SumToConstant));
         m.addConstraint(Choco.eq(Choco.sum(diags[1]), SumToConstant));
         
       //All cells are different from each other
        IntegerVariable[] allVars = new IntegerVariable[n * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                allVars[i * n + j] = cells[i][j];
            } 
        }
        m.addConstraint(Choco.allDifferent(allVars));         
         
		System.out.println("Hello World A	");
		
		//Our solver
		Solver s = new CPSolver();
		//read the model
		s.read(m);
		s.solve();
		
		//Print the values.
		for (int i = 0; i < n; i++) {
		    for (int j = 0; j < n; j++) {
		       System.out.print(s.getVar(cells[i][j]).getVal() + "_");
		    }
		    System.out.println();
		}
		
	}

}
