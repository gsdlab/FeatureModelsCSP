/**
 * 
 */
package featuremodeltransformations_deprecated;

/**
 * @author rafaelolaechea
 *
 */
import java.text.NumberFormat;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

/**
 * OrderHandler extends DefaultHandler to calculate price, number of items,
 * and number of orders.
 */
public class OrderHandler extends DefaultHandler {
  private float fOrderPrice = 0;
  private String priceElement = "";
  private final String PRICE = "PRICE";
  private final String ORDER = "Order";
  private final String ORDERS = "Orders";
  private final String ITEM = "Item";
  private int numberOrders = 0;
  private int numberItems = 0;

  /**
   * Returns Total for current XML Order
   * @return sOrderPrice - String
   */
  public String getOrderPrice(){
    return NumberFormat.getCurrencyInstance().format(fOrderPrice);
  }

  /**
   * Returns numbers of Orders found in XML file
   * @return sOrderPrice - String
   */
  public int getNumberOrders(){
    return numberOrders;
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
    if (PRICE.equals(localName)){
      priceElement = PRICE;
    }
    if (ITEM.equals(localName)){
      numberItems++;
    }
    if (ORDER.equals(localName)){
      // Reset Order's values
      fOrderPrice = 0;
      numberItems = 0;
      numberOrders++;
    }
  }

  /**
   * Receive notification of character data inside an element.
   * @param ch - The characters.
   * @param start - The start position in the character array.
   * @param length - The number of characters to use from the character array.
   * @throws SAXException - Any SAX exception, possibly wrapping another exception.
   */
  public void characters(char[] ch, int start, int length)
      throws SAXException {
    float floatValue = 0;
    if ( PRICE.equals(priceElement) ){
      String strValue = new String(ch, start, length);
      try{
        floatValue = NumberFormat.getCurrencyInstance().parse(strValue).floatValue();
      } catch(java.text.ParseException e) {
        System.out.println("Can't parse the PRICE element - " + e);
      }
      fOrderPrice += floatValue;
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
    if (ORDER.equals(localName)){
      System.out.println("Order number: " + numberOrders + " Items: " + numberItems + " Total: " + getOrderPrice());
    }
    if (PRICE.equals(localName)){
      priceElement = "";
    }
    if (ORDERS.equals(localName)){
      System.out.println("Orders found in XML file: " + numberOrders);
    }
  }

}


