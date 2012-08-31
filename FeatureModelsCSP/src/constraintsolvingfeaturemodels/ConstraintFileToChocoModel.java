/**
 * 
 */
package constraintsolvingfeaturemodels;

/**
 * @author rafaelolaechea
 *
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import choco.Choco;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.kernel.model.Model;
import choco.kernel.model.variables.Variable;
import choco.kernel.model.variables.integer.IntegerExpressionVariable;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.Solver;
import choco.kernel.model.constraints.Constraint;
import featuremodeltransformations_deprecated.ParentChildRelationExtractor;
import au.com.bytecode.opencsv.CSVReader;



public class ConstraintFileToChocoModel {

	private String claferFile;
	private String outputFile;
	
	private java.util.Map<String, IntegerVariable> FeatureVariables = new java.util.HashMap<String, IntegerVariable>();
	private java.util.Map<String, IntegerVariable> ProductLevelNFPVariables = new java.util.HashMap<String, IntegerVariable>();
	
	private java.util.Set<Constraint> MandatoryConstraints = new java.util.HashSet<Constraint>();
	private java.util.Set<Constraint> ChildrenImpliesParentConstraints = new java.util.HashSet<Constraint>();
	private java.util.Set<Constraint> MandatoryChildEqualParentConstraints = new java.util.HashSet<Constraint>();
	private java.util.Set<Constraint> ExclusiveOrConstraints = new java.util.HashSet<Constraint>();
	private java.util.Set<Constraint> OrConstraints = new java.util.HashSet<Constraint>();
	private java.util.Set<Constraint> ProductLevelNFPConstraints = new java.util.HashSet<Constraint>();
	private java.util.Set<Constraint> CrossTreeConstraints = new java.util.HashSet<Constraint>();
	
	
	private java.util.Set<String> ObjectiveVariable= new java.util.HashSet<String>();
	private java.util.Map<String, Map<String, Integer>> ProductLevelNfpAttributesWithContributions = new java.util.HashMap<String, Map<String, Integer>>();
	
	private String ObjectiveSense = "";
	

	private static final String ObjectiveSenseMinimize = "False";
	private static final String ObjectiveSenseMaximize = "True";	
	
	private  final  String  FeatureVariableIdentifier = "BinaryVariable";
	private final String ConstraintPrefixIdentifier = "Constraint";
	private final String ProductLevelNfpAttributeIdenitifer = "ProductLevelNfpAttribute";
	private  final String ProductLevelNfpFeatureContributionIdentifier = "ProductLevelNfpFeatureContribution";	
	
	private final String ConstraintMandatoryFeaturesIdentifier = "ConstraintMandatoryFeatures";
	private final String ConstraintChildrenImpliesParentIdentifier = "ConstraintChildrenImpliesParent";
	private final String ConstraintExclusiveOrIdentifier = "ConstraintExclusiveOr";
	private final String ConstraintOrIdentifier= "ConstraintOr";
	private final String ConstraintMandatoryChildEqualParentIdentifier = "ConstraintMandatoryChildEqualParent";	
	private final String ConstraintCrossTree = "ConstraintCrossTree";
	
	private final String ObjectiveSenseIdentifier = "ObjectiveSense";
	
	private final String  ChocoEqualConstraint = "Choco.eq";
	private final String ChocoImpliesConstraint = "Choco.implies";
	private final String ChocoIfAndOlnyIfConstraint = "Choco.ifOnlyIf";
	private final String ChocAndConstraint = "Choco.and";
	private final String ChocoNotConstraint = "Choco.not";
	private final String ChocoOrConstraint = "Choco.or" ;

	private final String keywordGroupVars = "GROUPVARS";
	private final char CSVDelimiter = ':';

	
	public ConstraintFileToChocoModel(String filename, String outputfilename)throws java.io.IOException{
		this.claferFile = filename;
		this.outputFile = outputfilename;
		
		this.parseFeatureVariables();
		this.parseConstraints();
		
		this.parseProductLevelNFP();
		this.parseProductLevelNFPFeatureContributions();
		this.createProductLevelNFPVariables();
		this.createProductLevelNFPConstraints();	
		this.parseObjectiveSense();
	}

	public static void main(String[] args) throws java.io.IOException, java.lang.InterruptedException {
		
		String claferFile = args[0];
		String outputFile = args[0].substring(0, args[0].lastIndexOf(".")) + ".chocosolution";
		
		ConstraintFileToChocoModel constraintParser = new ConstraintFileToChocoModel(claferFile, outputFile);
		
		constraintParser.CreateConstraintSolvingProblem();
	}
	
	private boolean isNumericString(String s){
		boolean ret = false;
		
		try{
			Integer.parseInt(s);
			ret = true;
		}catch(NumberFormatException e){
			ret = false;
		}

		return ret;
	}
	
	private Constraint extractEqualityFeatureToEitherFeatureOrNumberConstraint(String content){
		/*
		 *Parsing one of :
		 * 	Choco.eq(c17_ElementB, 1)
		 * Choco.eq(c17_ElementB, c17_ElementX)
		 */		
		Constraint ret_val;
		
		content= content.replace("(", "");
		content= content.replace(")", "");
		content = content.replace("Choco.eq", "");
		String []EqualityArguments = content.split(",");

		// First Argument should be a string,  second one can  be a an Integer or String.
		assert(!this.isNumericString(EqualityArguments[0].trim()));					

		assert(this.isNumericString(EqualityArguments[1].trim()));

		String nameFeatureEquality = EqualityArguments[0].trim();
		if (this.isNumericString(EqualityArguments[1].trim())){
			//System.out.println("FeatureToNumberConstrEq");
			int FeatureEqualityNum = Integer.parseInt(EqualityArguments[1].trim());		
			//System.out.println("Got back " + this.FeatureVariables.get(nameFeatureEquality) +  " by calling on get" + nameFeatureEquality );
			ret_val = Choco.eq(this.FeatureVariables.get(nameFeatureEquality), FeatureEqualityNum);
		} else {
			String nameSeconFeatureEquality = EqualityArguments[1].trim();
			ret_val = Choco.eq(this.FeatureVariables.get(nameFeatureEquality), 
								this.FeatureVariables.get(nameSeconFeatureEquality));
		}


		//System.out.println("Adding Constraint " + nameFeatureEquality + " = " + FeatureEqualityNum);
		return ret_val;
	}
	
	private Constraint extractNotEqualityFeatureToNumberConstraint(String content){
		/*
		 *Parsing :
		 * Choco.not(Choco.eq(c17_ElementB, 1))
		 */
		assert(content.startsWith(this.ChocoNotConstraint));
		Integer FirstParamStartIndex =this.ChocoNotConstraint.length() + 1;
		Integer FirstParamEndIndex =content.lastIndexOf("))") + 1;
		String FirstParam = content.substring(FirstParamStartIndex, FirstParamEndIndex);

		return Choco.not(this.extractEqualityFeatureToEitherFeatureOrNumberConstraint(FirstParam));
		
	}
	private Constraint extractAndNotEqualityFeatureToNumberWithFinalEqualConstraint(String content){
		/*
		 * 
		 * Parsing 
		 * Choco.and(Choco.not(Choco.eq(c17_ElementB, 1)),Choco.not(Choco.eq(c23_ElementC, 1)),Choco.eq(c4_AbstractElement, 1))
		 */
		//System.out.println("Adding AndNot Constraint receieving " + content );
		java.util.ArrayList<Constraint> AndConstraintsMembers = new java.util.ArrayList<Constraint>();
		assert(content.startsWith(this.ChocAndConstraint));
		while(content.indexOf(this.ChocoNotConstraint)!=-1){
			Integer ParamIndexStart = content.indexOf(this.ChocoNotConstraint);
			Integer ParamIndexStop = content.indexOf("))")+2;			
			String Param = content.substring(ParamIndexStart, ParamIndexStop);
			content = content.substring(ParamIndexStop);
			AndConstraintsMembers.add(this.extractNotEqualityFeatureToNumberConstraint(Param));
		}

		
		Integer ParamEqIndexStart = content.indexOf(this.ChocoEqualConstraint);
		Integer ParamEqIndexEnd = content.indexOf("))") + 1;
		String ParamEq = content.substring(ParamEqIndexStart, ParamEqIndexEnd);
		
		AndConstraintsMembers.add(this.extractEqualityFeatureToEitherFeatureOrNumberConstraint(ParamEq));
		Constraint []ArrayAndConstraintsMembers = new Constraint[AndConstraintsMembers.size()];
		
		ArrayAndConstraintsMembers = AndConstraintsMembers.toArray(ArrayAndConstraintsMembers);
		return Choco.and(ArrayAndConstraintsMembers);
	}

	private Constraint extractOrOnGroupVars(String content){
		/*
		 * Parsing 
		 *	Choco.or(GROUPVARS(c103_Performance,c109_MemorySize))
		 * 
		 */
		Integer innerGroupVarsStartIndex = content.indexOf(this.keywordGroupVars) + this.keywordGroupVars.length() + 1;
		Integer innerGroupVarsStopIndex = content.lastIndexOf("))");
		
		String innerGroupVars =  content.substring(innerGroupVarsStartIndex, innerGroupVarsStopIndex);
		
		java.util.ArrayList<IntegerVariable> OrVariablesMembers = new java.util.ArrayList<IntegerVariable>();
		for(String varName : innerGroupVars.split(",")){
			OrVariablesMembers.add(this.FeatureVariables.get(varName));
		}
		IntegerVariable []ArrayOrVariablesMembers = new IntegerVariable[OrVariablesMembers.size()];
		ArrayOrVariablesMembers = OrVariablesMembers.toArray(ArrayOrVariablesMembers);
		
		return Choco.or(ArrayOrVariablesMembers);
	}

	private void handleMandatoryConstraint(String constraintIdentifier, String constraintContent){
		/*
		 * 
		 * Parsing
		 * 	Choco.eq(c3_LinkedList, 1)
		 */

		if (constraintContent.startsWith(this.ChocoEqualConstraint)){
			Constraint CSPConstraint = this.extractEqualityFeatureToEitherFeatureOrNumberConstraint(constraintContent);
//			System.out.println("In " + constraintContent);
			//this.printConstraint(CSPConstraint, "Inserting MandatoryConstraint");			
			this.MandatoryConstraints.add(CSPConstraint);					
		}						
	}
	private void printConstraint(Constraint CSPConstraint, String prefix){
		System.out.println(prefix + " " + "Constraint " + CSPConstraint.getConstraintType() + " : " + CSPConstraint.getNbVars());

		for (Variable innerVarCSP  : CSPConstraint.getVariables() ){
			System.out.println("\t" +innerVarCSP );
		}

		
	}
	private void handleOrConstraints(String constraintIdentifier, String constraintContent){
		/*
		 * 
		 * Parsing
		 * ConstraintOr:c34:Choco.ifOnlyIf(Choco.or(GROUPVARS(c103_Performance,c109_MemorySize)), Choco.eq(c97_SyntheticPerformanceOrMemorySize, 1))
		 */
		if (constraintContent.startsWith(this.ChocoIfAndOlnyIfConstraint)){
			Integer FirstParameterStartIndex = this.ChocoIfAndOlnyIfConstraint.length() + 1;			
			Integer FirstParameterStopIndex = constraintContent.indexOf("),") + 1;

			String FirstParameter = constraintContent.substring(FirstParameterStartIndex, FirstParameterStopIndex);			
			String rest_constraintContent = constraintContent.substring(FirstParameterStopIndex);
			
			// , Choco.eq(c97_SyntheticPerformanceOrMemorySize, 1))
			Integer SecondParamaterStartIndex = rest_constraintContent.indexOf(this.ChocoEqualConstraint);			
			Integer SecondParameterStopIndex = rest_constraintContent.lastIndexOf("))") + 1;
			String SecondParameter =  rest_constraintContent.substring(SecondParamaterStartIndex, SecondParameterStopIndex);		
					
			OrConstraints.add(Choco.ifOnlyIf(
					this.extractOrOnGroupVars(FirstParameter),
					this.extractEqualityFeatureToEitherFeatureOrNumberConstraint(SecondParameter)));
		
			
			//System.out.println("OR <" +  FirstParameter + "> ,  <" + SecondParameter + ">");
		}		
	}
	private void hanldeExclusiveOrConstraints(String constraintIdentifier, String constraintContent){
		/*
		 * Parsing
		 * Choco.ifOnlyIf(Choco.eq(c11_ElementA, 1), Choco.and(Choco.not(Choco.eq(c17_ElementB, 1)),Choco.not(Choco.eq(c23_ElementC, 1)),Choco.eq(c4_AbstractElement, 1)))
		 *
		 *
		 */
		if (constraintContent.startsWith(this.ChocoIfAndOlnyIfConstraint)){
			Integer FirstParameterStartIndex = this.ChocoIfAndOlnyIfConstraint.length() + 1;			
			Integer FirstParameterStopIndex = constraintContent.indexOf("),") + 1;

			String FirstParameter = constraintContent.substring(FirstParameterStartIndex, FirstParameterStopIndex);			
			String rest_constraintContent = constraintContent.substring(FirstParameterStopIndex);
			
			// Choco.and(Choco.not(Choco.eq(c17_ElementB, 1)),Choco.not(Choco.eq(c23_ElementC, 1)),Choco.eq(c4_AbstractElement, 1))
			Integer SecondParamaterStartIndex = rest_constraintContent.indexOf(this.ChocAndConstraint);			
			Integer SecondParameterStopIndex = rest_constraintContent.lastIndexOf("))") + 1;
			String SecondParameter =  rest_constraintContent.substring(SecondParamaterStartIndex, SecondParameterStopIndex);		
			
			ExclusiveOrConstraints.add(Choco.ifOnlyIf(
					this.extractEqualityFeatureToEitherFeatureOrNumberConstraint(FirstParameter),
					this.extractAndNotEqualityFeatureToNumberWithFinalEqualConstraint(SecondParameter)));
			
			//System.out.println("Xor <" +  FirstParameter + "> ,  <" + SecondParameter + ">");
		}
	}

	private Constraint extractImpliesConstraintedBasedOnVarValues(String constraintContent){
		/*
		 *  Parsing Choco.implies(Choco.eq(%s, %s), Choco.eq(%s, %s))\n"
		 * 
		 */		
		Integer FirstParameterStartIndex = this.ChocoImpliesConstraint.length() + 1;
		Integer FirstParameterStopIndex = constraintContent.indexOf("),") + 1;
		String FirstParameter = constraintContent.substring(FirstParameterStartIndex, FirstParameterStopIndex);

		String rest_constraintContent = constraintContent.substring(FirstParameterStopIndex);
		
		
		Integer SecondParamaterStartIndex = rest_constraintContent.indexOf(this.ChocoEqualConstraint);			
		Integer SecondParameterStopIndex = rest_constraintContent.indexOf(")") + 1;
		String SecondParameter = rest_constraintContent.substring(SecondParamaterStartIndex, SecondParameterStopIndex);
		
		//System.out.println("Extracting Constraint out of <<" +FirstParameter + ">>");
		Constraint n1 = this.extractEqualityFeatureToEitherFeatureOrNumberConstraint(FirstParameter);
		Constraint n2 = this.extractEqualityFeatureToEitherFeatureOrNumberConstraint(SecondParameter);
		
		return Choco.implies(n1, n2);
	}
	private void handleChildrenImpliesParentConstraints(String constraintIdentifier, String constraintContent){
		/*
		 *  Parsing Choco.implies(Choco.eq(%s, 1), Choco.eq(%s, 1))\n" % (child_id, parent_id)
		 * 
		 */
		if (constraintContent.startsWith(this.ChocoImpliesConstraint)){
			this.ChildrenImpliesParentConstraints.add(this.extractImpliesConstraintedBasedOnVarValues(constraintContent));
		} 
	}

	private void handleCrossTreeConstraints(String constraintIdentifier,
			String constraintContent) {
		/*
		 *  Parsing Choco.implies(Choco.eq(%s, %s), Choco.eq(%s, %s))\n" 
		 * 
		 */
		if (constraintContent.startsWith(this.ChocoImpliesConstraint)){
			this.CrossTreeConstraints.add(this.extractImpliesConstraintedBasedOnVarValues(constraintContent));
		} 		
	}
	
	private void handleMandatoryChildEqualParent(String constraintIdentifier,
			String constraintContent) {
		/*
		 * Parsing
		 *	Choco.eq(c115_Base, c3_LinkedList)
		 * 
		 */
		this.MandatoryChildEqualParentConstraints.add(this.extractEqualityFeatureToEitherFeatureOrNumberConstraint(constraintContent));
		
	}

	private void parseConstraints() throws java.io.IOException {
		CSVReader reader = new CSVReader(new FileReader(this.claferFile), this.CSVDelimiter);
		String [] nextLine;
		while ((nextLine = reader.readNext()) != null) {
			if( nextLine[0].startsWith(this.ConstraintPrefixIdentifier)){
				String constraintIdentifier = nextLine[1];
				String constraintContent = nextLine[2];				
				if (nextLine[0].equals(this.ConstraintMandatoryFeaturesIdentifier)){
					this.handleMandatoryConstraint(constraintIdentifier, constraintContent);
				}else if (nextLine[0].equals(ConstraintChildrenImpliesParentIdentifier)){
					//System.out.println("Handling ConstraintChildrenImpliesParentIdentifier");
					this.handleChildrenImpliesParentConstraints(constraintIdentifier, constraintContent);
				}else if (nextLine[0].equals(this.ConstraintExclusiveOrIdentifier)){
					this.hanldeExclusiveOrConstraints(constraintIdentifier, constraintContent);
				} else if (nextLine[0].equals(this.ConstraintOrIdentifier)){
					this.handleOrConstraints(constraintIdentifier, constraintContent);
				}else if (nextLine[0].equals(this.ConstraintMandatoryChildEqualParentIdentifier)){
					this.handleMandatoryChildEqualParent(constraintIdentifier, constraintContent);
				} else if (nextLine[0].equals(this.ConstraintCrossTree)){
					this.handleCrossTreeConstraints(constraintIdentifier, constraintContent);					
				}
			}
		}
		reader.close();
		
	}


	private void parseFeatureVariables() throws java.io.IOException {
		CSVReader reader = new CSVReader(new FileReader(this.claferFile), this.CSVDelimiter);
		String [] nextLine;
		while ((nextLine = reader.readNext()) != null) {
			if( nextLine[0].equals(this.FeatureVariableIdentifier)){
				this.FeatureVariables.put(nextLine[1], 	Choco.makeIntVar(nextLine[1], 0, 1));
			}
		}
		reader.close();
	}
	
	private void parseProductLevelNFP() throws java.io.IOException {
		
		CSVReader reader = new CSVReader(new FileReader(this.claferFile), this.CSVDelimiter);
		String [] nextLine;
		while ((nextLine = reader.readNext()) != null) {
			if( nextLine[0].equals(this.ProductLevelNfpAttributeIdenitifer)){
				this.ProductLevelNfpAttributesWithContributions.put(nextLine[1], new HashMap<String, Integer>());
			}
		}
		reader.close();
	}
	
	private void parseProductLevelNFPFeatureContributions() throws java.io.IOException {		
		CSVReader reader = new CSVReader(new FileReader(this.claferFile), this.CSVDelimiter);
		String [] nextLine;
		while ((nextLine = reader.readNext()) != null) {
			if( nextLine[0].equals(this.ProductLevelNfpFeatureContributionIdentifier)){				
				String []Contents = nextLine[1].split(",");
				assert(Contents.length >= 3);
				String NFPAttribute = Contents[0];
				String Feature = Contents[1];
				Integer FeatureContribution = Integer.parseInt(Contents[2]);
				this.ProductLevelNfpAttributesWithContributions.get(NFPAttribute).put(Feature, FeatureContribution);
			}
		}
		reader.close();
	}

	private void parseObjectiveSense() throws java.io.IOException {		
		CSVReader reader = new CSVReader(new FileReader(this.claferFile), this.CSVDelimiter);
		String [] nextLine;
		while ((nextLine = reader.readNext()) != null) {
			if( nextLine[0].equals(this.ObjectiveSenseIdentifier)){		
				this.ObjectiveSense = nextLine[1];
			}
		}
		reader.close();
	}
	
	
	private void createProductLevelNFPConstraints() {
		// TODO Auto-generated method stub
		for (String NFPProductLevel : this.ProductLevelNfpAttributesWithContributions.keySet()){
						
			int NumberFeaturesContributing = this.ProductLevelNfpAttributesWithContributions.get(NFPProductLevel).size();
			
			
			IntegerVariable features[] = new IntegerVariable[NumberFeaturesContributing];
			int[] featureContributions = new int[NumberFeaturesContributing];

			int i=0;
			for(String featureName : this.ProductLevelNfpAttributesWithContributions.get(NFPProductLevel).keySet()){
				features[i] = this.FeatureVariables.get(featureName);
				featureContributions[i] = this.ProductLevelNfpAttributesWithContributions.get(NFPProductLevel).get(featureName);
				i++;
			}

			System.out.println("Setting Equality for " + this.ProductLevelNFPVariables.get(NFPProductLevel));
			this.ProductLevelNFPConstraints.add(Choco.eq(this.ProductLevelNFPVariables.get(NFPProductLevel), 
					Choco.scalar(featureContributions, features)));
			
		}		
	}

	private void createProductLevelNFPVariables() {
		// TODO Auto-generated method stub
		for (String NFPProductLevel : this.ProductLevelNfpAttributesWithContributions.keySet()){
			int max_goal = 0;
			int min_goal = 0;
			
			for (String Feature: this.ProductLevelNfpAttributesWithContributions.get(NFPProductLevel).keySet()){
				int contribution = this.ProductLevelNfpAttributesWithContributions.get(NFPProductLevel).get(Feature);
				if (contribution > 0 )
					max_goal += contribution;
				else  {
					min_goal += contribution;
				}
			}			
			this.ProductLevelNFPVariables.put(NFPProductLevel, Choco.makeIntVar(NFPProductLevel, min_goal, max_goal, "cp:bound"));
		}
		
	}
	
	private  void CreateConstraintSolvingProblem() throws FileNotFoundException {
		// TODO Auto-generated method stub
		
		Model m = new CPModel();

		for (String CSPVarName : this.FeatureVariables.keySet()){
			m.addVariable(this.FeatureVariables.get(CSPVarName));
		}
		
		for (String CSPVarName : this.ProductLevelNFPVariables.keySet()){
			m.addVariable(this.ProductLevelNFPVariables.get(CSPVarName));
		}		
		
		for (Constraint CSPConstraint : this.MandatoryConstraints){
			//this.printConstraint(CSPConstraint, "Adding MandatoryConstraint");	
			m.addConstraint(CSPConstraint);
		}

		for (Constraint CSPConstraint : this.ChildrenImpliesParentConstraints){
			//this.printConstraint(CSPConstraint, "Adding ChildrenImpliesParentConstraints");	
			m.addConstraint(CSPConstraint);
		}
		
		for (Constraint CSPConstraint : this.MandatoryChildEqualParentConstraints){
			m.addConstraint(CSPConstraint);
		}

		for (Constraint CSPConstraint : this.ExclusiveOrConstraints){
			m.addConstraint(CSPConstraint);
		}

		for (Constraint CSPConstraint : this.OrConstraints){
			m.addConstraint(CSPConstraint);
		}

		for (Constraint CSPConstraint : this.ProductLevelNFPConstraints){
			System.out.println("Adding ProductLevel Constraint");
			m.addConstraint(CSPConstraint);
		}

		for (Constraint CSPConstraint : this.CrossTreeConstraints){
			//this.printConstraint(CSPConstraint, "Adding ChildrenImpliesParentConstraints");	
			m.addConstraint(CSPConstraint);
		}		
		
		
		Solver s = new CPSolver();
		
		
		
		assert(this.ProductLevelNFPVariables.size()<=1 && this.ProductLevelNFPVariables.size() >0);
		
		IntegerVariable Objective = null;
		 
		for (String ProductLevelNFP :  this.ProductLevelNFPVariables.keySet()){
			Objective = this.ProductLevelNFPVariables.get(ProductLevelNFP);
		}
		
		System.out.println("Objective " +Objective);
		
		s.read(m);

		System.out.println("Checking "+ this.ObjectiveSense + " , versus " +ObjectiveSenseMinimize );
		
		if(this.ObjectiveSense.equals(ObjectiveSenseMinimize)){
			s.minimize(s.getVar(Objective), false);			
		} else {
			s.maximize(s.getVar(Objective), false);			
		}

			
		System.out.println("Configuration");
		for (String varName : this.FeatureVariables.keySet()){
			if (s.getVar(this.FeatureVariables.get(varName)).getVal() > 0){
				System.out.println("\t" + (s.getVar(this.FeatureVariables.get(varName)).getName() ));
			}
		}
		System.out.println("\t" + s.getVar(Objective).getName()  + " = " + s.getVar(Objective).getVal() );
		
		PrintWriter out= new PrintWriter(this.outputFile);
		System.out.println("Will print solution to " + this.outputFile);
		
		out.print("<solution>\n");
		out.print("\t<instance>\n");
		for (String varName : this.FeatureVariables.keySet()){
			out.print("\t\t<variable id='"+ varName + "'>\n");
			out.print("\t\t\t<value>" + s.getVar(this.FeatureVariables.get(varName)).getVal() +"</value>\n");			
			out.print("\t\t</variable>\n");
		}	
		for (String ProductLevelNFP :  this.ProductLevelNFPVariables.keySet()){
			out.print("\t\t<variable id='"+ ProductLevelNFP + "'>\n");
			out.print("\t\t\t<value>" + s.getVar(this.ProductLevelNFPVariables.get(ProductLevelNFP)).getVal() +"</value>\n");
			out.print("\t\t</variable>\n");
		}
		
		out.print("\t</instance>\n");
		out.print("</solution>\n");
		out.close();		
	}

}



/*		
// TODO Make Automatic.
 final String[] FeatureNames = {"c4_AbstractElement"
		 ,"c11_ElementA"
		 ,"c17_ElementB"
		 ,"c23_ElementC"
		 ,"c29_AbstractIterator"
		 ,"c35_ForwardIterator"
		 ,"c41_BackwardIterator"
		 ,"c47_AbstractSort"
		 ,"c53_BubbleSort"
		 ,"c59_MergeSort"
		 ,"c65_InsertionSort"
		 ,"c71_QuickSort"
		 ,"c77_print"
		 ,"c83_Measurement"
		 ,"c91_TCP_IP"
		 ,"c97_SyntheticPerformanceOrMemorySize"
		 ,"c103_Performance"
		 ,"c109_MemorySize"
		 ,"c115_Base"};
	
 final int[] FeatureContributions =  {-12, 12, 0, 0, 0,
		0, 1, 57, 17, 32,
		0, 22, 44, 484, 0,
		0, 37, 36, 455};

	int min_goal = 0; 
	int max_goal = 0 ; 
	for (int contribution: FeatureContributions){
		if (contribution > 0 )
			max_goal += contribution;
		else 
			min_goal += contribution;
	}
	
	// TODO Automate
	IntegerVariable total_footprint = Choco.makeIntVar("total_footprint", min_goal, max_goal, "cp:bound");
	m.addVariable(total_footprint);
	
	// TODO Automate

	//Adding Constraint for total footprint.
	// [total_footprint = sum IMeasurement.footprint ]
	IntegerVariable features[] = new IntegerVariable[FeatureNames.length];
	int i =0;
	for(String name : FeatureNames){
		features[i] = this.FeatureVariables.get(name);
		i++;
	}
	
	m.addConstraint(Choco.eq(total_footprint, Choco.scalar(FeatureContributions, features)));
*/
