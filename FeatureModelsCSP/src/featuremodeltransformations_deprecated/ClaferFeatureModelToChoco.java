/**
 * 
 */
package featuremodeltransformations_deprecated;

/**
 * @author rafaelolaechea
 *
 */
import java.io.File;
import java.text.NumberFormat;
import java.util.Arrays;


import choco.Choco;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.kernel.model.Model;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.Solver;

import com.lexicalscope.jewel.cli.CommandLineInterface;
import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;



public class ClaferFeatureModelToChoco {
	/**
	 * @param args
	 */
	public static void main(String[] args) throws java.io.IOException, java.lang.InterruptedException {
		final FeatureModelToCSPOptions parsedOptions = CliFactory.parseArguments(FeatureModelToCSPOptions.class, args);
		

		String claferTranslator = parsedOptions.getClaferTranslator();
		String claferFile = parsedOptions.getClaferModel();
		
		java.lang.Runtime rt = java.lang.Runtime.getRuntime();
		java.lang.Process p = rt.exec(claferTranslator + " --mode=xml " + claferFile);
		p.waitFor();
		
		System.out.println("Finishing Building XML file for " + claferFile);
		
		FeatureModelHandler claferHandler = new FeatureModelHandler();

		CreateParser parser = new CreateParser(claferHandler);

		String  xmlClaferFeatureModel = claferFile.replace(".cfr", ".xml");
		parser.parse(xmlClaferFeatureModel);
			// TODO Auto-generated method stub
		
		System.out.println("Top-Level Clafers");
		claferHandler.printDeclarations();
		
		parser.parse(xmlClaferFeatureModel);
		
		System.out.println("Finished Original Parsing");

		ParentChildRelationExtractor childRelationsExtractor = new  ParentChildRelationExtractor(claferHandler.getFeatureNames(), claferHandler.getTopLevelDeclarations(), claferHandler.getObjectiveNames());
		
		parser = new CreateParser(childRelationsExtractor);
		
		parser.parse(xmlClaferFeatureModel);
		
		//childRelationsExtractor.printParentChild();
		childRelationsExtractor.printMandatoryChildrenConstraints();

		CreateConstraintSolvingProblem(childRelationsExtractor);
	}

	private static void CreateConstraintSolvingProblem(
			ParentChildRelationExtractor childRelationsExtractor) {
		// TODO Auto-generated method stub
		
		java.util.Set<String> featureNames = childRelationsExtractor.getFeatures();
		java.util.Set<String> objectiveNames = childRelationsExtractor.getObjectiveNames();
		String rootName = childRelationsExtractor.getConfigurationUniqueName();
		
		java.util.Map<String, Integer> variableIndex  = new java.util.HashMap<String, Integer>();
		
		Model m = new CPModel();

		// Start of Creating Choco Variables.		
		IntegerVariable [] variables = new IntegerVariable[featureNames.size() + objectiveNames.size() + 1];
		int i = 0;
		
		for (String feature: featureNames){
			//System.out.println("PropertyMap.put(\""+ feature + ", 0);");
			variables[i] = Choco.makeIntVar(feature, 0, 1);
			variableIndex.put(feature, i);
			i++;
		}

		for (String objective: objectiveNames){
			// Adding total_performance variables
			int min_goal = 0; 
			int max_goal = 0 ; 
			for (int contribution: childRelationsExtractor.getGoalSum().values()){
				if (contribution > 0 )
					max_goal += contribution;
				else 
					min_goal += contribution;
			}
			
			variables[i] = Choco.makeIntVar(objective,  min_goal, max_goal, "cp:bound");
			variableIndex.put(objective, i);
			i++;
		}
		
		variables[i] =  Choco.makeIntVar(rootName, 0, 1);
		variableIndex.put(rootName, i);		
		// End of Creating Variables
		
		
		//Mandatory Constraints
		for (java.util.Stack<String> parentChildMandatory : childRelationsExtractor.getMandatoryChildren()){
			if ( ! objectiveNames.contains(parentChildMandatory.get(1))){
				m.addConstraint(Choco.eq(variables[variableIndex.get(parentChildMandatory.get(0))],
										 variables[variableIndex.get(parentChildMandatory.get(1))]));
			}
		}

		// Optinal Constraints.
		for (java.util.Stack<String> parentChildOptional : childRelationsExtractor.getOptionalChildren()){
			m.addConstraint(Choco.implies(Choco.eq(variables[variableIndex.get(parentChildOptional.get(1))],1),
					Choco.eq(variables[variableIndex.get(parentChildOptional.get(0))],1)));
		}

		
		// Xor Constraints.
		for (String xorParent : childRelationsExtractor.getXorFeatures().keySet()){
			choco.kernel.model.constraints.Constraint []IfOnlyIfConstraints = new choco.kernel.model.constraints.Constraint[childRelationsExtractor.getXorFeatures().get(xorParent).size()];
			int j = 0;
			for (String children : childRelationsExtractor.getXorFeatures().get(xorParent)){
				int k = 0;
				int number_other_children = childRelationsExtractor.getXorFeatures().get(xorParent).size()  - 1;
				choco.kernel.model.constraints.Constraint [] AndAllOtherNotAndParentTrue = new choco.kernel.model.constraints.Constraint[number_other_children + 1];
				for (String otherChildren : childRelationsExtractor.getXorFeatures().get(xorParent)){
					if (! otherChildren.equals(children)){
						AndAllOtherNotAndParentTrue[k] = Choco.not(Choco.eq(variables[variableIndex.get(otherChildren)], 1));
						k++;
					}
				}
				AndAllOtherNotAndParentTrue[k] = Choco.eq(variables[variableIndex.get(xorParent)], 1);
				IfOnlyIfConstraints[j] = 	Choco.ifOnlyIf(	
						Choco.eq(variables[variableIndex.get(children)], 1)	, 
						Choco.and(AndAllOtherNotAndParentTrue));
				j++;
			}
			Choco.and(IfOnlyIfConstraints);
		}

		
		// Or Constraints.
		for (String orParent : childRelationsExtractor.getOrFeatures().keySet()){
			IntegerVariable []OrBetweenChildren = new IntegerVariable[childRelationsExtractor.getOrFeatures().get(orParent).size()];
			int j = 0;
			for (String children : childRelationsExtractor.getOrFeatures().get(orParent)){
				OrBetweenChildren[j] = variables[variableIndex.get(children)];
				j++;
			}
			m.addConstraint(Choco.ifOnlyIf(Choco.or(OrBetweenChildren), Choco.eq(variables[variableIndex.get(orParent)], 1)));
			
		}
		
		// Cross-Tree Constraints.
		for (String CroosTreeOriginator : childRelationsExtractor.getCrossTreeImplications().keySet()){
			String CrossTreeImplied = childRelationsExtractor.getCrossTreeImplications().get(CroosTreeOriginator);
			if (CrossTreeImplied.startsWith("!")){
				int offset_skip_logicalnot = 1;				
				m.addConstraint(Choco.implies(Choco.eq(variables[variableIndex.get(CroosTreeOriginator)], 1), Choco.eq(variables[variableIndex.get(CrossTreeImplied.substring(offset_skip_logicalnot))], 0)));				
			} else {
				m.addConstraint(Choco.implies(Choco.eq(variables[variableIndex.get(CroosTreeOriginator)], 1), Choco.eq(variables[variableIndex.get(CrossTreeImplied)], 1)));				
			}
			
		}
		


		// Has to be a root.
		m.addConstraint(Choco.eq(variables[variableIndex.get(rootName)], 1));
		
		// Objective Constraints.
		for (String objective : objectiveNames){
			int FeatureContributions [] = new int[featureNames.size()];
			for (String featureName : featureNames){
				FeatureContributions[variableIndex.get(featureName)] =  childRelationsExtractor.getGoalSum().get(featureName);
			}
			m.addConstraint(Choco.eq(variables[variableIndex.get(objective)], Choco.scalar(FeatureContributions, Arrays.copyOfRange(variables, 0, featureNames.size()))));
		}
		

		//Solve and print answer.
		Solver s = new CPSolver();		
		s.read(m);
		
		for (String objective : objectiveNames){
			s.maximize(s.getVar(variables[variableIndex.get(objective)]), false);
		}
		
		System.out.println("Configuration");
		
		for (String featureName : featureNames ) {
			if ( s.getVar(variables[ variableIndex.get(featureName)]).getVal() > 0 ){
				System.out.println("\t" + s.getVar(variables[variableIndex.get(featureName)]).getName());								
			}
	    }

		for (String objective : objectiveNames){
			System.out.println("\t" + s.getVar(variables[variableIndex.get(objective)]).getName() + " = " + s.getVar(variables[variableIndex.get(objective)]).getVal() );	
		}					
		

	}

}


