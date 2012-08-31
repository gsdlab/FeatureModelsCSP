package featuremodeltransformations_deprecated;

import java.util.List;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



public class ParentChildRelationExtractor extends DefaultHandler {
	  private String clNamesapce = "http://gsd.uwaterloo.ca/clafer";
	  private java.util.Stack<String> FeatureNames = new java.util.Stack<String>();
	  private java.util.Stack<String> ObjectiveNames = new java.util.Stack<String>();
	  private List<String> TopLevelDeclarations;	 
	  
	  final int  AbstractFeatureModel_index = 1;
	  final int ConcreteFeatureModel_index = 2;
	  
	  private boolean inUniqueID = false;
	  private boolean inConstraint = false;

	  private int indentation_level = 0;
	  private boolean inAbstractFeatureModel = false;
	  
	  private java.util.Stack<String> FeaturesPathFromRoot = new java.util.Stack<String>();
	  private java.util.Stack<String> FeaturesWithXorOrChildren = new java.util.Stack<String>();
	  private java.util.Stack<String> FeaturesWithXor = new java.util.Stack<String>();	  
	  private java.util.Stack<String> FeaturesWithOr =  new java.util.Stack<String>();	
	  private java.util.Stack<java.util.Stack<String>> ParentChild = new java.util.Stack<java.util.Stack<String>>();
	  private java.util.Map<String, java.util.Set<String>> ParentChildrenMap = new java.util.HashMap<String, java.util.Set<String>>();
	  
	  private boolean haveKeywordCardinalityConstraint = false;	  
	  private boolean inCardinality = false;
	  private boolean inMinCardinality = false;
	  private boolean inMinCardinalityIntLiteral = false;
	  
	  private int inKeywordCardinalityConstraintMin = -1;
	  private int inKeywordCardinalityConstraintMax = -1;
	  
	  private java.util.Map<String, Integer> FeatureMinCardinality = new java.util.HashMap<String, Integer>();

	  
	  private boolean inGroupCardinality;
	  private boolean inGroupCardinalityIsKeyword;
	  private boolean inMinGroupCardinality;
	  private boolean inMinGroupCardinalityIntLiteral;
	  private boolean inMaxGroupCardinality;
	  private boolean inMaxGroupCardinalityIntLiteral;

	  
	  public  ParentChildRelationExtractor(java.util.Stack<String> FeatureNames, List<String> TopLevelDeclarations, java.util.Stack<String> ObjectiveNames) {
		  super();
		  this.FeatureNames = FeatureNames;
		  this.TopLevelDeclarations = TopLevelDeclarations;
		  this.ObjectiveNames = ObjectiveNames;
	  }

	  /**
	   * Receive notification of the start of an element.
	   * @param namespaceURI - The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
	   * @param localName - The local name (without prefix), or the empty string if Namespace processing is not being performed.
	   * @param qName - The qualified name (with prefix), or the empty string if qualified names are not available.
	   * @param atts - The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
	   * @throws SAXException - Any SAX exception, possibly wrapping another exception.
	   */
	  public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
	      throws SAXException {		  
		  if (namespaceURI == clNamesapce  && localName.equals("Declaration")){
			this.indentation_level++;
			  
		  	if (atts.getValue(atts.getQName(0)).equals("cl:IConstraint")){
		  		this.inConstraint = true;			  		
		  	}
		  }else if (namespaceURI == clNamesapce  && localName.equals("UniqueId")){
			  this.inUniqueID = true;
		  }else if (namespaceURI == clNamesapce  && localName.equals("GroupCard")){
			  this.inGroupCardinality = true;			
		  }else if (namespaceURI == clNamesapce  && localName.equals("Min") && this.inGroupCardinality == true){
			  this.inMinGroupCardinality = true;
		  }else if (namespaceURI == clNamesapce  && localName.equals("Max") && this.inGroupCardinality == true){
			  this.inMaxGroupCardinality = true;
		  }else if (namespaceURI == clNamesapce  && localName.equals("IntLiteral") && this.inMinGroupCardinality == true ){
			  this.inMinGroupCardinalityIntLiteral = true;
		  } else if (namespaceURI == clNamesapce  && localName.equals("IntLiteral") && this.inMaxGroupCardinality == true){
			  this.inMaxGroupCardinalityIntLiteral = true;
		  }else if (namespaceURI == clNamesapce  && localName.equals("IsKeyword")){
			  this.inGroupCardinalityIsKeyword = true;			
		  }else if (namespaceURI == clNamesapce  && localName.equals("Card")){
			  this.inCardinality = true;
		  } else if (namespaceURI == clNamesapce  && localName.equals("Min") && this.inCardinality == true){
			  this.inMinCardinality = true;
		  } else if (namespaceURI == clNamesapce  && localName.equals("IntLiteral") && this.inMinCardinality == true ){
			  this.inMinCardinalityIntLiteral = true;
		  }		  
	  }

	  public void characters (char ch[], int start, int length)
				throws SAXException {
		  String content = new String(java.util.Arrays.copyOfRange(ch, start, start+length));

		  if (this.inUniqueID && content.equals(this.TopLevelDeclarations.get(AbstractFeatureModel_index))) {
			  this.FeaturesPathFromRoot.add(this.TopLevelDeclarations.get(ConcreteFeatureModel_index));			  
			  this.inAbstractFeatureModel = true;
			  
		  } else if (this.inUniqueID && this.inAbstractFeatureModel){
			  
			  java.util.Stack<String> parentChildTuple = new java.util.Stack<String>();
			  
			  parentChildTuple.add(this.FeaturesPathFromRoot.peek());
			  parentChildTuple.add(content);

			  this.ParentChild.add(parentChildTuple);
			  
			 if ( this.ParentChildrenMap.containsKey(this.FeaturesPathFromRoot.peek()) ){
				java.util.Set<String> childrenSet =  this.ParentChildrenMap.get(this.FeaturesPathFromRoot.peek());
				childrenSet.add(content);
			 } else {
				 java.util.Set<String> childrenSet = new java.util.HashSet<String>();
				 childrenSet.add(content);
				 this.ParentChildrenMap.put(this.FeaturesPathFromRoot.peek(), childrenSet);
			 }
			 
			  //this.ParentChildrenMap.get(key)
			  this.FeaturesPathFromRoot.add(content);
			  
			  if (this.haveKeywordCardinalityConstraint == true){
				  this.FeaturesWithXorOrChildren.push(content);
				  
				  if ( this.inKeywordCardinalityConstraintMax == 1  && this.inKeywordCardinalityConstraintMin == 1 ){
					  this.FeaturesWithXor.push(content);
				  } else if ( this.inKeywordCardinalityConstraintMax == -1  && this.inKeywordCardinalityConstraintMin == 1 ){
					  this.FeaturesWithOr.push(content);
				  }
				  this.inKeywordCardinalityConstraintMax = -1;
				  this.inKeywordCardinalityConstraintMin = -1;
			  }
			  
			  this.haveKeywordCardinalityConstraint = false;

		  } else if (this.inMinCardinalityIntLiteral == true && this.inAbstractFeatureModel ){
			  if (this.TopLevelDeclarations.get(ConcreteFeatureModel_index).equals(this.FeaturesPathFromRoot.peek())){				  
				  this.FeatureMinCardinality.put(this.FeaturesPathFromRoot.peek(), 1); 				  
			  } else {
				  this.FeatureMinCardinality.put(this.FeaturesPathFromRoot.peek(), Integer.parseInt(content)); 				  
			  }
		  } else if (this.inGroupCardinalityIsKeyword == true && this.inAbstractFeatureModel && content.equals("true")){
			  //System.out.println("Found Keyword for xor/or");
			  this.haveKeywordCardinalityConstraint = true;
		  } else if (this.inMinGroupCardinalityIntLiteral == true && this.inAbstractFeatureModel && this.haveKeywordCardinalityConstraint == true){
			  this.inKeywordCardinalityConstraintMin = Integer.parseInt(content);
		  } else if  (this.inMaxGroupCardinalityIntLiteral == true && this.inAbstractFeatureModel && this.haveKeywordCardinalityConstraint == true){
			  this.inKeywordCardinalityConstraintMax = Integer.parseInt(content);
		  }
	  }
	  

	  /**
	   * Receive notification of the end of an element.
	   * @param namespaceURI - The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
	   * @param localName - The local name (without prefix), or the empty string if Namespace processing is not being performed.
	   * @param qName - The qualified name (with prefix), or the empty string if qualified names are not available.
	   * @throws SAXException - Any SAX exception, possibly wrapping another exception.
	   */
	  public void endElement(String namespaceURI, String localName, String qName)
	      throws SAXException {
		  
		  if (namespaceURI == clNamesapce  && localName.equals("Declaration")){
			  this.indentation_level--;

			  if (this.inAbstractFeatureModel &&  this.inConstraint == false){
				  this.FeaturesPathFromRoot.pop();
			  }
			  
			  if (this.inConstraint){
				  this.inConstraint = false;
			  }
		  }else if (namespaceURI == clNamesapce  && localName.equals("UniqueId")){
			  this.inUniqueID = false;
		  }	else if (namespaceURI == clNamesapce  && localName.equals("Card")){
			  this.inCardinality = false;			  
		  }else if (namespaceURI == clNamesapce  && localName.equals("Min")){
			  this.inMinCardinality = false;
			  this.inMinGroupCardinality = false;
		  }else if (namespaceURI == clNamesapce  && localName.equals("Max")){			  
				  this.inMaxGroupCardinality = false;			  
		  } else if (namespaceURI == clNamesapce  && localName.equals("IntLiteral")){
			  this.inMinCardinalityIntLiteral = false;
			  this.inMinGroupCardinalityIntLiteral = false;
			  this.inMaxGroupCardinalityIntLiteral = false;
		  }else if (namespaceURI == clNamesapce  && localName.equals("GroupCard")){
			  this.inGroupCardinality = false;			
		  }else if (namespaceURI == clNamesapce  && localName.equals("IsKeyword")){
			  this.inGroupCardinalityIsKeyword = false;			
		  }

		  
		  if (this.inAbstractFeatureModel && this.indentation_level == 0){
			  // Exited the Abstract Feature Model.
			  this.inAbstractFeatureModel = false;
		  }
		  		    
	  }

	  public void endDocument ()
		throws SAXException
	    {

	    }
	  
	  public void printParentChild(){
		  for (java.util.Stack<String> parentChildTuple : this.ParentChild){
			  System.out.println("Parent is  " + parentChildTuple.get(0) + " , child is " + parentChildTuple.get(1) );
		  }	  	  
	  }
	  
	  public java.util.Set<String> getFeatures(){
		  return new java.util.HashSet<String>(this.FeatureNames);
	  }	  
	  public java.util.Set<String> getObjectiveNames(){
		  return new java.util.HashSet<String>(this.ObjectiveNames);
	  }	  

	  
	  public java.util.Set<java.util.Stack<String>> getMandatoryChildren(){
		  java.util.Set<java.util.Stack<String>>  parentMandatoryChildren  = new java.util.HashSet<java.util.Stack<String>>();
		  for (java.util.Stack<String> parentChildTuple : this.ParentChild){
			  if (this.FeatureMinCardinality.get(parentChildTuple.get(1)) == 1){
				  parentMandatoryChildren.add(parentChildTuple);
			  }
		  }
		  return parentMandatoryChildren;
	  }


	  public java.util.Set<java.util.Stack<String>> getOptionalChildren(){
		  java.util.Set<java.util.Stack<String>>  parentOptionalChildren  = new java.util.HashSet<java.util.Stack<String>>();
		  for (java.util.Stack<String> parentChildTuple : this.ParentChild){
			  if (this.FeatureMinCardinality.get(parentChildTuple.get(1)) == 0 && this.FeaturesWithXorOrChildren.contains(parentChildTuple.get(0))== false){
				  parentOptionalChildren.add(parentChildTuple);
			  }
		  }
		  return parentOptionalChildren;
	  }
	  
	  public java.util.Map<String, java.util.Set<String>> getOrFeatures(){
		  java.util.Map<String, java.util.Set<String>> orFeaturesMap = new java.util.HashMap<String, java.util.Set<String>>();		  

		  for (String  ParentsWithKeyword : this.FeaturesWithOr){
			  orFeaturesMap.put(ParentsWithKeyword, this.ParentChildrenMap.get(ParentsWithKeyword));
		  }
		  return orFeaturesMap;

		  
	  }

	  public java.util.Map<String, java.util.Set<String>> getXorFeatures(){
		  java.util.Map<String, java.util.Set<String>> xorFeaturesMap = new java.util.HashMap<String, java.util.Set<String>>();		  

		  for (String  ParentsWithKeyword : this.FeaturesWithXor){
			  xorFeaturesMap.put(ParentsWithKeyword, this.ParentChildrenMap.get(ParentsWithKeyword));
		  }
		  return xorFeaturesMap;
	  }
	  
	  public String getConfigurationUniqueName(){
		  return this.TopLevelDeclarations.get(2);
	  }

	  public java.util.Map<String, Integer> getGoalSum(){
		  java.util.Map<String, Integer> PropertyMap = new java.util.HashMap<String, Integer>();

		  if ( this.TopLevelDeclarations.get(1).contains("LinkedList")){
			  PropertyMap.put("c4_AbstractElement" , -12);
			  PropertyMap.put("c83_Measurement" , 484);
			  PropertyMap.put("c59_MergeSort" , 32);
			  PropertyMap.put("c23_ElementC" , 0);
			  PropertyMap.put("c17_ElementB" , 0);
			  PropertyMap.put("c103_Performance" , 37);
			  PropertyMap.put("c65_InsertionSort" , 0);
			  PropertyMap.put("c115_Base" , 455);
			  PropertyMap.put("c97_SyntheticPerformanceOrMemorySize" , 0);
			  PropertyMap.put("c53_BubbleSort" , 17);
			  PropertyMap.put("c47_AbstractSort" , 57);
			  PropertyMap.put("c35_ForwardIterator" , 0);
			  PropertyMap.put("c91_TCP_IP" , 0);
			  PropertyMap.put("c71_QuickSort" , 22);
			  PropertyMap.put("c11_ElementA" , 12);
			  PropertyMap.put("c77_print" , 44);
			  PropertyMap.put("c41_BackwardIterator" , 1);
			  PropertyMap.put("c29_AbstractIterator" , 0);
			  PropertyMap.put("c109_MemorySize", 36);		
			  
		  }else if (this.TopLevelDeclarations.get(1).contains("Apache")){
			  PropertyMap.put("c10_HostnameLookups", -26);
			  PropertyMap.put("c49_InMemory", 26);
			  PropertyMap.put("c4_Base", 150);
			  PropertyMap.put("c23_EnableSendfile", 15);
			  PropertyMap.put("c57_Handle", 4);
			  PropertyMap.put("c42_ExtendedStatus", 0);
			  PropertyMap.put("c35_AccessLog", -15);
			  PropertyMap.put("c17_KeepAlive", 105);
			  PropertyMap.put("c29_FollowSymLinks", 0);
		  }				  				  
		  return PropertyMap;
		  
	  }
	  
	  public java.util.Map<String, String> getCrossTreeImplications(){
		  java.util.Map<String, String> PropertyMap = new java.util.HashMap<String, String>();

		  if ( this.TopLevelDeclarations.get(1).contains("LinkedList")){
			  PropertyMap.put("c83_Measurement" , "c47_AbstractSort"); 			  			  
		  } else if (this.TopLevelDeclarations.get(1).contains("Apache")){
			  PropertyMap.put("c49_InMemory", "!c57_Handle");
		  }
		  

		  
		  return PropertyMap;		  
	  }
	  
	  
	  public void printMandatoryChildrenConstraints() {
		  for (java.util.Stack<String> parentChildTuple : this.ParentChild){
//			  System.out.println("parentChildTuple size : " + parentChildTuple.size() + " ; this.FeatureMinCardinality: " + this.FeatureMinCardinality.size());
			  if (this.FeatureMinCardinality.get(parentChildTuple.get(1)) == 1){
				  System.out.println("Mandatory Children: " + parentChildTuple.get(1) + "  of " + parentChildTuple.get(0)  );
				  //System.out.println(parentChildTuple.get(0) + " = 1" + " <-> " + parentChildTuple.get(1) + " = 1");				  
			  }
		  }
		  
		  for (java.util.Stack<String> parentChildTuple : this.ParentChild){
//			  System.out.println("parentChildTuple size : " + parentChildTuple.size() + " ; this.FeatureMinCardinality: " + this.FeatureMinCardinality.size());
			  if (this.FeatureMinCardinality.get(parentChildTuple.get(1)) == 0 && this.FeaturesWithXorOrChildren.contains(parentChildTuple.get(0))== false){
				  System.out.println("Optional Children: " + parentChildTuple.get(1));				  
			  }
		  }
		  

		  for (String  ParentsWithKeyword : this.FeaturesWithXorOrChildren){
			 // System.out.println("FeaturesXorOr: " + ParentsWithKeyword);	
			  for (String Children : this.ParentChildrenMap.get(ParentsWithKeyword)){
				//  System.out.println("\t\t Child: " + Children);
			  }
		  }	

		  
		  for (String  ParentsWithKeyword : this.FeaturesWithXor){
			  System.out.println("FeaturesXor: " + ParentsWithKeyword);
			  for (String Children : this.ParentChildrenMap.get(ParentsWithKeyword)){
				  System.out.println("\t\t Child: " + Children);
			  }
		  }

		  for (String  ParentsWithKeyword : this.FeaturesWithOr){
			  System.out.println("FeaturesOr: " + ParentsWithKeyword);
			  for (String Children : this.ParentChildrenMap.get(ParentsWithKeyword)){
				  System.out.println("\t\t Child: " + Children);
			  }			  
		  }

	  }
	  
}