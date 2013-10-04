/* Copyright (c) 2002-2008 The University of the West Indies
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

package jspecview.application;

import jspecview.util.JSVColor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.TreeMap;

import org.jmol.util.SB;

import jspecview.common.ColorParameters;
import jspecview.common.ScriptToken;
import jspecview.java.AwtParameters;
import jspecview.util.JSVColorUtil;
import jspecview.util.JSVFileManager;
import jspecview.util.JSVTxt;
import jspecview.util.JXVXmlReader;

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
  private String fileName = "displaySchemes.xml";

  /** The list of displaySchemes that is loaded from file */
  private TreeMap<String, ColorParameters> displaySchemes;

  private JXVXmlReader reader;

  /**
   * Initialises the <code>DisplaySchemesProcessor</code>
   */
  public DisplaySchemesProcessor() {
    displaySchemes = new TreeMap<String, ColorParameters>();
  }

  /**
   * Load a default DisplayScheme if xml file not found
   * @return boolean
   */

  public ColorParameters loadDefault() {
    AwtParameters dsdef = new AwtParameters("Default");
    dsdef.displayFontName = "default";
    dsdef.setColor(ScriptToken.TITLECOLOR, ColorParameters.BLACK);
    dsdef.setColor(ScriptToken.UNITSCOLOR, ColorParameters.BLACK);
    dsdef.setColor(ScriptToken.SCALECOLOR, ColorParameters.BLACK);
    dsdef.setColor(ScriptToken.COORDINATESCOLOR, ColorParameters.BLACK);
    dsdef.setColor(ScriptToken.PEAKTABCOLOR, ColorParameters.RED);
    dsdef.setColor(ScriptToken.HIGHLIGHTCOLOR, ColorParameters.DARK_GRAY);
    dsdef.setColor(ScriptToken.GRIDCOLOR, ColorParameters.BLACK);
    dsdef.setColor(ScriptToken.PLOTCOLOR, ColorParameters.BLACK);
    dsdef.setColor(ScriptToken.PLOTAREACOLOR, ColorParameters.WHITE);
    dsdef.setColor(ScriptToken.BACKGROUNDCOLOR, ColorParameters.WHITE);
    dsdef.setColor(ScriptToken.INTEGRALPLOTCOLOR, ColorParameters.RED);
    displaySchemes.put("Default", dsdef);
    
    return dsdef;
  }
  
  public ColorParameters getDefaultScheme(){
	  ColorParameters ds = displaySchemes.get("Default");
	  if(ds == null){
		  ds = loadDefault();
	  }
	  return ds;
  }



  /**
   * Saves the display schemes to file in XML format
   * @throws Exception
   */
  public void store() throws Exception{
    serializeDisplaySchemes(new BufferedWriter(new FileWriter(fileName)));
  }

  /**
   * Returns the list of <code>DisplayScheme</code>s that were loaded
   * @return the list of <code>DisplayScheme</code>s that were loaded
   */
  public TreeMap<String, ColorParameters> getDisplaySchemes(){
    return displaySchemes;
  }

  public boolean load(InputStream stream) {
    try {
      return load(JSVFileManager.getBufferedReaderForInputStream(stream));
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Loads the display schemes into memory and stores them in a <code>Vector</code>
   * @param dispSchemeFileName the name of the file to load
   * @return true if loaded successfully
   */
  public boolean load(String dispSchemeFileName){
    fileName = dispSchemeFileName;        
    try{
      BufferedReader br = JSVFileManager.getBufferedReaderFromName(fileName, null, "##TITLE");
    	return load(br);
    }
    catch(Exception e){
    	return false;
    }
    
  }

  /**
   * Loads the display schemes into memory and stores them in a
   * <code>Vector</code>
   * @param br 
   * @return true if loaded successfully
   * @throws Exception 
   */
  public boolean load(BufferedReader br) throws Exception {

    reader = new JXVXmlReader(br);
    String defaultDS = "Default";
    ColorParameters ds = null;
    String attr;
    try {
      while (reader.hasNext()) {
        if (reader.nextEvent() != JXVXmlReader.START_ELEMENT)
          continue;
        String theTag = reader.getTagName();
        if (theTag.equals("displayschemes")) {
          defaultDS = reader.getAttrValue("default");
        }
        if (theTag.equals("displayscheme")) {
          String name = reader.getAttrValue("name");
          ds = new AwtParameters(name);
          if (name.equals(defaultDS))
            ds.isDefault = true;
          displaySchemes.put(name, ds);
        }
        if (ds == null)
          continue;
        if (theTag.equals("font")) {
          attr = reader.getAttrValue("face");
          if (attr.length() > 0)
            ds.displayFontName = attr;
        } else {
          if (theTag.equals("coordinateColor"))
            theTag = "coordinatesColor";
          ScriptToken st = ScriptToken.getScriptToken(theTag);
          if (st != ScriptToken.UNKNOWN) {
            JSVColor color = getColor(ds);
            if (color == null) {
              String def;
              switch (st) {
              default:
                def = null;
                break;
              case TITLECOLOR:
                def = "#0000ff";
                break;
              case COORDINATESCOLOR:
                def = "#ff0000";
                break;
              case PEAKTABCOLOR:
                def = "#ff0000";
                break;
              case HIGHLIGHTCOLOR:
                def = "#808080";
                break;
              case SCALECOLOR:
                def = "#660000";
                break;
              case UNITSCOLOR:
                def = "#ff0000";
                break;
              case GRIDCOLOR:
                def = "#4e4c4c";
                break;
              case PLOTCOLOR:
                def = "#ff9900";
                break;
              case PLOTAREACOLOR:
                def = "#333333";
                break;
              case BACKGROUNDCOLOR:
                def = "#c0c0c0";
                break;
              }
              if (def != null)
                color = ds.getColorFromString(def);
            }
            if (color != null)
              ds.setColor(st, color);
          }
        }
      }
    } catch (Exception e) {
      return false;
    }
    return true;
  }
  
  /**
   * Gets a hex color value from the attribute of a tag and returns a
   * <code>Color</code>
   * @param p 
   * @return Returns a <code>Color</code> from the attribute
   */
  private JSVColor getColor(ColorParameters p){
    String value = reader.getAttrValueLC("hex");
    return (value.length() == 0 || value.equals("default") ? null
        : p.getColorFromString(value));
  }

	/**
	 * Serializes the display schemes to the given writer
	 * 
	 * @param writer
	 *          the writer for the output
	 * @throws Exception
	 */
	public void serializeDisplaySchemes(Writer writer) throws Exception {
		if (displaySchemes.size() == 0) {
			return;
		}

		// find the default scheme
		// set default attr
		SB buffer = new SB();
		String defaultDSName = "";

		for (ColorParameters ds : displaySchemes.values()) {
			if (ds.isDefault)
				defaultDSName = ds.name;
			buffer.append("\t<displayScheme name=\"" + ds.name + "\">").append(JSVTxt.newLine);
			buffer.append("\t\t<font face=\"" + ds.displayFontName + "\"/>").append(JSVTxt.newLine);
			writeColor(buffer, ds, "titleColor", ScriptToken.TITLECOLOR);
			writeColor(buffer, ds, "scaleColor", ScriptToken.SCALECOLOR);
			writeColor(buffer, ds, "unitsColor", ScriptToken.UNITSCOLOR);
			writeColor(buffer, ds, "coordinatesColor", ScriptToken.COORDINATESCOLOR);
			writeColor(buffer, ds, "highlightColor", ScriptToken.HIGHLIGHTCOLOR);
			writeColor(buffer, ds, "peakTabColor", ScriptToken.PEAKTABCOLOR);
			writeColor(buffer, ds, "gridColor", ScriptToken.GRIDCOLOR);
			writeColor(buffer, ds, "plotColor", ScriptToken.PLOTCOLOR);
			writeColor(buffer, ds, "plotAreaColor", ScriptToken.PLOTAREACOLOR);
			writeColor(buffer, ds, "backgroundColor", ScriptToken.BACKGROUNDCOLOR);
			buffer.append("\t</displayScheme>").append(JSVTxt.newLine);
		}
		buffer.append("</displaySchemes>");

		SB outBuffer = new SB();
		outBuffer.append("<?xml version=\"1.0\"?>" + JSVTxt.newLine);
		outBuffer.append("<displaySchemes default=\"" + defaultDSName + "\">"
				+ JSVTxt.newLine);
		outBuffer.append(buffer.toString());
		writer.write(outBuffer.toString());
		writer.flush();
		writer.close();
	}

	private void writeColor(SB buffer, ColorParameters ds, String name,
			ScriptToken t) {
		buffer.append(
				"\t\t<" + name + " hex=\""
						+ JSVColorUtil.colorToHexString(ds.getElementColor(t)) + "\"/>")
				.append(JSVTxt.newLine);
	}

}
