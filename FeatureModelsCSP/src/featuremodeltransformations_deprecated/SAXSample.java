/**
 * 
 */
package featuremodeltransformations_deprecated;

/**
 * Main class for SAX Sample
 */
public class SAXSample {

  /**
   * Main method
   * @param args - String[] arguments
   */
  public static void main(String[] args) {

    SAXSample jfs = new SAXSample();

    // Create Order's Handler
    OrderHandler oHandler = new OrderHandler();

    // Create the parser
    CreateParser parser = new CreateParser(oHandler);

    // Parse the XML file, handler generates the output
    parser.parse("Order.xml");

    System.out.println("\n\n The Order.xml parsed - found Orders: " + oHandler.getNumberOrders());
  }
}