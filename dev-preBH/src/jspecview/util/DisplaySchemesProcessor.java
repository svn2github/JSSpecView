/* Copyright (c) 2002-2007 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.util;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <code>DisplaySchemesProcessor</code> loads and saves the display schemes of
 * Jspecview. The Schemes are loaded from an XML file and saved in a TreeMap.
 * Also saves the schemes out to XML file after modification
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class DisplaySchemesProcessor {

  /** The Name of the XML file that contains the display schemes */
  private String fileName;

  /** The list of displaySchemes that is loaded from file */
  private TreeMap<String,DisplayScheme> displaySchemes;


  /**
   * Initialises the <code>DisplaySchemesProcessor</code>
   */
  public DisplaySchemesProcessor() {
    displaySchemes = new TreeMap<String,DisplayScheme>();
  }

  /**
   * Load a default DisplayScheme if xml file not found
   * @param dispSchemeFileName String
   * @return boolean
   * @throws Exception
   */

public boolean loadDefault(String dispSchemeFileName) {
    Color black = new Color(0,0,0);
    Color white = new Color(255,255,255);

    DisplayScheme dsdef = new DisplayScheme("Default");
    dsdef.setFont("default");
    dsdef.setColor("title", black);
    dsdef.setColor("coordinates", black);
    dsdef.setColor("scale", black);
    dsdef.setColor("units", black);
    dsdef.setColor("grid", black);
    dsdef.setColor("plot", black);
    dsdef.setColor("plotarea", white);
    dsdef.setColor("background", white);
    displaySchemes.put("Default", dsdef);
    return true;
  }



  /**
   * Loads the display schemes into memory and stores them in a <code>Vector</code>
   * @param dispSchemeFileName the name of the file to load
   * @throws Exception
   * @return true if loaded successfully
   */
  public boolean load(String dispSchemeFileName) throws Exception{
    this.fileName = dispSchemeFileName;

    DOMParser parser = new DOMParser();

    // Get the DOM tree as a Document object
    parser.parse(dispSchemeFileName);
    Document doc = parser.getDocument();
    return documentToDisplaySchemes(doc);
  }

  /**
   * Saves the display schemes to file in XML format
   * @throws IOException
   */
  public void store() throws IOException{
    serializeDisplaySchemes(new FileWriter(fileName));
  }

  /**
   * Returns the list of <code>DisplayScheme</code>s that were loaded
   * @return the list of <code>DisplayScheme</code>s that were loaded
   */
  public TreeMap getDisplaySchemes(){
    return displaySchemes;
  }

  /**
   * Extacts the information from the Document tree and create a list of
   * <code>DisplayScheme</code>s. Method returns true on success
   * @param doc the DOM Document
   * @return true if the conversion was a success
   */
  private boolean documentToDisplaySchemes(Document doc){
    // Get root element
    Element rootElement = doc.getDocumentElement();
    if(!rootElement.getNodeName().toLowerCase().equals("displayschemes"))
      return false;

    NamedNodeMap rootAttrs = rootElement.getAttributes();
    String defaultDS = rootAttrs.getNamedItem("default").getNodeValue();

    NodeList nodes = rootElement.getChildNodes();

    if(nodes == null)
      return false;

    // Get all displayScheme elements
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if(node.getNodeType() != Node.ELEMENT_NODE)
        continue;

      NamedNodeMap dsAttr = node.getAttributes();

      String name = dsAttr.getNamedItem("name").getNodeValue();
      DisplayScheme ds = new DisplayScheme(name);
      if(name.equals(defaultDS))
        ds.setDefault(true);

      NodeList childNodes = node.getChildNodes();
      if(childNodes == null)
        continue;

      for(int j = 0; j < childNodes.getLength(); j++){
        Node childNode = childNodes.item(j);

        // get attribues of each element
        NamedNodeMap attrs = childNode.getAttributes();
        if(attrs == null)
          continue;

        String nodeName = childNode.getNodeName();

        if(nodeName.toLowerCase().equals("font")){
          String font;
          Node attrNode = attrs.getNamedItem("face");
          if(attrNode != null){
            font = attrNode.getNodeValue();
            if(font == null)
              font = "default"; // or some actual font name
          }else{
            font = "default";
          }
          ds.setFont(font);
        }else if(nodeName.toLowerCase().equals("titlecolor")){
          Color color = getColorFromAtrr(attrs);
          if(color == null)
               color= color.decode("#0000ff");
            ds.setColor("title", color);
        }else if(nodeName.toLowerCase().equals("coordinatecolor")){
          Color color = getColorFromAtrr(attrs);
          if(color == null)
            color=color.decode("#ff0000");
            ds.setColor("coordinates", color);
        }else if(nodeName.toLowerCase().equals("scalecolor")){
          Color color = getColorFromAtrr(attrs);
          if(color == null)
            color=color.decode("#660000");
            ds.setColor("scale", color);
        }else if(nodeName.toLowerCase().equals("unitscolor")){
          Color color = getColorFromAtrr(attrs);
          if(color == null)
            color=color.decode("#ff0000");
            ds.setColor("units", color);
        }else if(nodeName.toLowerCase().equals("gridcolor")){
          Color color = getColorFromAtrr(attrs);
          if(color == null)
            color=color.decode("#4e4c4c");
            ds.setColor("grid", color);
        }else if(nodeName.toLowerCase().equals("plotcolor")){
          Color color = getColorFromAtrr(attrs);
          if(color == null)
             color.decode("#ff9900");
            ds.setColor("plot", color);
        }else if(nodeName.toLowerCase().equals("plotareacolor")){
          Color color = getColorFromAtrr(attrs);
          if(color == null)
            color=color.decode("#333333");
            ds.setColor("plotarea", color);
        }else if(nodeName.toLowerCase().equals("backgroundcolor")){
          Color color = getColorFromAtrr(attrs);
          if(color == null)
            color=color.decode("#c0c0c0");
            ds.setColor("background", color);
        }
      }
      displaySchemes.put(ds.getName(), ds);
    }
    return true;
  }

  /**
   * Serializes the display schemes to the given writer
   * @param writer the writer for the output
   * @throws IOException
   */
  public void serializeDisplaySchemes(Writer writer) throws IOException{
    if(displaySchemes.size() == 0){
      return;
    }

    // find the default scheme
    // set default attr
    StringWriter sw = new StringWriter();
    BufferedWriter buffer = new BufferedWriter(sw);
    String defaultDSName = "";

    Iterator interator = displaySchemes.keySet().iterator();
    while (interator.hasNext()){
      DisplayScheme ds = (DisplayScheme)displaySchemes.get(interator.next());
      if(ds.isDefault())
        defaultDSName = ds.getName();

      buffer.write("\t<displayScheme name=\"" + ds.getName() + "\">");
      buffer.newLine();
      buffer.write("\t\t<font face = \"" + ds.getFont() + "\"/>");
      buffer.newLine();
      buffer.write("\t\t<titleColor hex = \"" +
                   JSpecViewUtils.colorToHexString(ds.getColor("title")) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<scaleColor hex = \"" +
                   JSpecViewUtils.colorToHexString(ds.getColor("scale")) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<unitsColor hex = \"" +
                   JSpecViewUtils.colorToHexString(ds.getColor("units")) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<coordinateColor hex = \"" +
                   JSpecViewUtils.colorToHexString(ds.getColor("coordinates")) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<gridColor hex = \"" +
                   JSpecViewUtils.colorToHexString(ds.getColor("grid")) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<plotColor hex = \"" +
                   JSpecViewUtils.colorToHexString(ds.getColor("plot")) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<plotAreaColor hex = \"" +
                   JSpecViewUtils.colorToHexString(ds.getColor("plotarea")) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t\t<backgroundColor hex = \"" +
                   JSpecViewUtils.colorToHexString(ds.getColor("background")) +
                   "\"/>");
      buffer.newLine();
      buffer.write("\t</displayScheme>");
      buffer.newLine();
    }

    buffer.write("</displaySchemes>");
    buffer.flush();

    StringBuffer outBuffer = new StringBuffer();
    outBuffer.append("<?xml version=\"1.0\"?>" + JSpecViewUtils.newLine);
    outBuffer.append("<displaySchemes default=\""+ defaultDSName +"\">" + JSpecViewUtils.newLine);
    outBuffer.append(sw.getBuffer());

    writer.write(outBuffer.toString());
    writer.flush();
    writer.close();
  }

  /**
   * Gets a hex color value from the attribute of a tag and returns a
   * <code>Color</code>
   * @param attrs the collection of attributes
   * @return Returns a <code>Color</code> from the attribute
   */
  private Color getColorFromAtrr(NamedNodeMap attrs){
    Color color = null;
    String value;
    Node attrNode = attrs.getNamedItem("hex");
    if(attrNode != null){
      value = attrNode.getNodeValue();
      if(value != null && !value.toLowerCase().equals("default"))
        color = JSpecViewUtils.getColorFromString(value);
    }
    return color;
  }
}
