package featuremodeltransformations_deprecated;

import java.util.List;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



public class FeatureModelHandler extends DefaultHandler {
	  private String clNamesapce = "http://gsd.uwaterloo.ca/clafer";
	  private List<String> TopLevelDeclarations  = new Vector<String>();
	  private java.util.Stack<String> XMLElements = new java.util.Stack<String>();
	  
	  private int numberItems = 0;
	  private boolean inUniqueID = false;
	  private boolean firstPass = true;
	  private boolean inFeatureModelSecondPass = false;
	  private boolean inConstraint = false;
	  private int currentTab = 0;
	  
	  private java.util.Stack<String> FeatureNames = new java.util.Stack<String>();	  
	  private java.util.Stack<String> ObjectiveNames = new java.util.Stack<String>();	  
	  private java.util.Stack<String> FeaturesPathFromRoot = new java.util.Stack<String>();

	  
	  //new IntegerVariable[featuresCount + objectivesCount];
	  

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
			  if (firstPass)			  
			  {
			  	for (int i = 0; i < currentTab; i++){
			  		System.out.print("\t");  					  
			  	}
			  	System.out.println(atts.getValue(atts.getQName(0)));
			  	
			  }

			  	if (atts.getValue(atts.getQName(0)).equals("cl:IConstraint")){
			  		this.inConstraint = true;			  		
			  	}
			  	
			  currentTab++;
			  XMLElements.push(localName);
		  } else if (namespaceURI == clNamesapce  && localName.equals("UniqueId")){
			  if (firstPass) {
				  for (int i = 0; i < currentTab; i++){
					  System.out.print("\t");  					  
				  }
				  System.out.print(localName);
			  }
			  inUniqueID = true;
		  }
	  }

	  public void characters (char ch[], int start, int length)
				throws SAXException {
		  if (inUniqueID && firstPass){
			  String UniqueIDContent = new String(java.util.Arrays.copyOfRange(ch, start, start+length));
			  System.out.println(": " + UniqueIDContent );
			  
			  if (XMLElements.size() == 1) {
				  // This is the name of a toplevel declaration.
				  TopLevelDeclarations.add(UniqueIDContent);
				  
			  }
		  } else if (inUniqueID) {
			  String UniqueIDContent = new String(java.util.Arrays.copyOfRange(ch, start, start+length));
			  
			  if ( UniqueIDContent.equals(this.TopLevelDeclarations.get(1)) ){
				  this.inFeatureModelSecondPass = true;
			  } else if ( this.inFeatureModelSecondPass ){
				  if (! UniqueIDContent.contains("_total_")){
					  this.FeatureNames.add(UniqueIDContent);				  					  					  
				  } else {
					  this.ObjectiveNames.add(UniqueIDContent);
				  }
			  }
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
			  currentTab--;
			  XMLElements.pop();
			  
			  if (this.inFeatureModelSecondPass && this.FeaturesPathFromRoot.size() > 0 && this.inConstraint == false){
				  this.FeaturesPathFromRoot.pop();
			  }			 
			  if (this.inConstraint){
				  this.inConstraint = false;
			  }
		  }else if (namespaceURI == clNamesapce  && localName.equals("UniqueId")){
			  inUniqueID = false;
		  }	 

		  
		  if (this.inFeatureModelSecondPass && XMLElements.size() == 0){
			  // Exited the Abstract Feature Model.
			  this.inFeatureModelSecondPass = false;
		  }
		  
	  }

	  public void endDocument ()
		throws SAXException
	    {
		  this.firstPass = false;
		  System.out.println("Finished Document");
	    }

	  
	  public void printDeclarations(){
		  for (String  ClaferDeclaration : TopLevelDeclarations){
			  System.out.println(ClaferDeclaration);
		  }			  
	  }

	public java.util.Stack<String> getFeatureNames() {
		return FeatureNames;
	}

	public List<String> getTopLevelDeclarations() {
		return TopLevelDeclarations;
	}

	public java.util.Stack<String> getObjectiveNames() {
		return ObjectiveNames;
	}
	
}