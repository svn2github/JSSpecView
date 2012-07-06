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

package jspecview.common;

import java.awt.Color;
import java.util.Hashtable;
import java.util.Map;

/**
 * This a representation of the Display Scheme for the spectral display.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class Parameters {

  /** The name of the DisplayScheme */
  protected String name;
  /** The name of the title font */
  protected String titleFont;
  /** The name of the display font */
  protected String displayFont;
  /** A map of the name of the elements and their colors*/
  protected Map<ScriptToken, Object> elementColors = new Hashtable<ScriptToken, Object>();
  public Map<ScriptToken, Object> getColors() {
    return elementColors;
  }
  
  /** Specifies if the display scheme is the default */
  protected boolean isDefault = false;

  /**
   * Intialises a DisplayScheme with the given name
   * @param name the name of the <code>DisplayScheme</code>
   */
  public Parameters(String name){
    this.name = name;
  }

  /**
   * Returns the name of the <code>DisplayScheme</code>
   * @return the name of the <code>DisplayScheme</code>
   */
  public String getName(){
    return name;
  }

  /**
   * Sets the name of the <code>DisplayScheme</code>
   * @param name the name
   */
  public void setName(String name){
    this.name = name;
  }

  /**
   * @return the title font name used in this <code>DisplayScheme</code>
   */
  public String getTitleFont(){
    return titleFont;
  }

  /**
   * Sets the title font name
   * @param fontName the name of the font
   */
  public void setTitleFont(String fontName){
    titleFont = fontName;
  }

  /**
   * Returns the font name used in this <code>DisplayScheme</code>
   * @return the font name used in this <code>DisplayScheme</code>
   */
  public String getDisplayFont(){
    return displayFont;
  }

  /**
   * Sets the font name
   * @param fontName the name of the font
   */
  public void setDisplayFont(String fontName){
    displayFont = fontName;
  }

  /**
   * Sets whether the <code>DisplayScheme</code> is the default
   * @param val is true if default, otherwise false
   */
  public void setDefault(boolean val){
    isDefault = val;
  }

  /**
   * Returns whether or not the <code>DisplayScheme</code> is the default
   * @return true if default, false otherwise
   */
  public boolean isDefault(){
    return isDefault;
  }

  /**
   * Gets the color of an element in the scheme
   * @param element the name of the element
   * @return the <code>Color</code> of the element
   */
  public Object getColor(ScriptToken st){
    return elementColors.get(st);
  }

  protected Object plotColors;    

  /**
   * Sets the color of an element
   * @param element the name of the element
   * @param color the color the element should have
   */
  public Object setColor(ScriptToken st, Object color){
    if (color != null)
      elementColors.put(st, color);
    return color;
  }

  /**
   * Returns a copy of this <code>DisplayScheme</code> with a new name
   * @param newName the new name
   * @return a copy of this <code>DisplayScheme</code> with a new name
   */
  public Parameters copy(String newName){
    Parameters ds = new Parameters(newName);
    ds.setDisplayFont(getDisplayFont());
    for(Map.Entry<ScriptToken, Object> entry: elementColors.entrySet())
      ds.setColor(entry.getKey(), entry.getValue());
    return ds;
  }

  /**
   * Returns a copy of this <code>DisplayScheme</code>
   * @return a copy of this <code>DisplayScheme</code>
   */
  public Parameters copy(){
    return copy(getName());
  }

  protected void setDefaultColors(Object[] colors) {
    
    setColor(ScriptToken.TITLECOLOR, colors[0]);
    setColor(ScriptToken.UNITSCOLOR, colors[1]);
    setColor(ScriptToken.SCALECOLOR, colors[2]);
    setColor(ScriptToken.COORDINATESCOLOR, colors[3]);
    setColor(ScriptToken.GRIDCOLOR, colors[4]);
    setColor(ScriptToken.PLOTCOLOR, colors[5]);
    setColor(ScriptToken.PLOTAREACOLOR, colors[6]);
    setColor(ScriptToken.BACKGROUNDCOLOR, colors[7]);
    setColor(ScriptToken.INTEGRALPLOTCOLOR, colors[8]);
    if (colors.length < 9) {
    	setColor(ScriptToken.PEAKTABCOLOR, Color.RED);
    	setColor(ScriptToken.HIGHLIGHTCOLOR, Color.DARK_GRAY);
    } else {
    	setColor(ScriptToken.PEAKTABCOLOR, colors[9]);
    	setColor(ScriptToken.HIGHLIGHTCOLOR, colors[10]);
    }
  }

  public double integralMinY = IntegralData.DEFAULT_MINY;
  public double integralFactor = IntegralData.DEFAULT_FACTOR;
  public double integralOffset = IntegralData.DEFAULT_OFFSET;

  protected void setParamDefaults() {
    setBoolean(ScriptToken.TITLEON, true);
    setBoolean(ScriptToken.ENABLEZOOM, true);
    setBoolean(ScriptToken.DISPLAY2D, true);
    setBoolean(ScriptToken.COORDINATESON, true);
    setBoolean(ScriptToken.GRIDON, true);
    setBoolean(ScriptToken.XSCALEON, true);
    setBoolean(ScriptToken.YSCALEON, true);
    setBoolean(ScriptToken.XUNITSON, true);
    setBoolean(ScriptToken.YUNITSON, true);
  }

  public static boolean isTrue(String value) {
    return (value.length() == 0 || Boolean.parseBoolean(value)); 
  }
  
  private Map<ScriptToken, Boolean> htBooleans = new Hashtable<ScriptToken, Boolean>();

  public Map<ScriptToken, Boolean> getBooleans() {
    return htBooleans;
  }

  public boolean setBoolean(ScriptToken st, String value) {
    return setBoolean(st, isTrue(value));
  }

  public boolean setBoolean(ScriptToken st, boolean val) {
    htBooleans.put(st, Boolean.valueOf(val));
    return val;
  }

  public boolean getBoolean(ScriptToken t) {
    return Boolean.TRUE == htBooleans.get(t);
  }
    
  public void setFor(JSVPanel jsvp, Parameters ds, boolean includeMeasures) {

    if (ds == null)
      ds = this;

    // measures -- not in displayScheme?

    if (includeMeasures)
      jsvp.getPanelData().setBoolean(this, null);

    // colors and fonts

    if (jsvp.getPlotColor(1) != null) // integration
      jsvp.setPlotColors(plotColors);
    jsvp.setColorOrFont(ds, null);
 }

  public void set(JSVPanel jsvp, ScriptToken st, String value) {
    Object param = null;
    switch (st) {
    default:
      return;
    case PLOTCOLORS:
      if (jsvp == null)
        getPlotColors(value);
      else
        jsvp.setPlotColors(getPlotColors(value));
      return;
    case COORDINATESON:
    case DISPLAY1D:
    case DISPLAY2D:
    case ENABLEZOOM:
    case GRIDON:
    case REVERSEPLOT:
    case TITLEON:
    case TITLEBOLDON:
    case XSCALEON:
    case XUNITSON:
    case YSCALEON:
    case YUNITSON:
      if (value.equalsIgnoreCase("TOGGLE")) {
        if (jsvp == null)
          return;
        boolean b = !jsvp.getPanelData().getBoolean(st);
        value = (b ? "TRUE" : "FALSE");
        switch (st) {
        case XSCALEON:
          setBoolean(ScriptToken.XUNITSON, b);
          jsvp.getPanelData().setBoolean(ScriptToken.XUNITSON, b);
          break;
        case YSCALEON:
          setBoolean(ScriptToken.YUNITSON, b);
          jsvp.getPanelData().setBoolean(ScriptToken.YUNITSON, b);
          break;
        }
      }
      setBoolean(st, value);
      break;
    case BACKGROUNDCOLOR:
    case COORDINATESCOLOR:
    case GRIDCOLOR:
    case HIGHLIGHTCOLOR:
    case INTEGRALPLOTCOLOR:
    case PEAKTABCOLOR:
    case PLOTAREACOLOR:
    case PLOTCOLOR:
    case SCALECOLOR:
    case TITLECOLOR:
    case UNITSCOLOR:
      param = setColorFromString(st, value);
      break;
    case TITLEFONTNAME:
    case DISPLAYFONTNAME:
      param = getFontName(st, value);
    }
    if (jsvp == null)
      return;
    if (param != null)
      jsvp.setColorOrFont(this, st);
    else
      jsvp.getPanelData().setBoolean(this, st);
  }

  protected Object getPlotColors(String value) {
    // overridden in AwtParameters
    return null;
  }

  protected Object setColorFromString(ScriptToken st, String value) {
    // overridden in AwtParameters
    return null;
  }

  protected Object getFontName(ScriptToken st, String value) {
    // overridden in AwtParameters
    return null;
  }

}
