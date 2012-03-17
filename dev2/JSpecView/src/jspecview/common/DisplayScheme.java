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
 * @see jspecview.common.JSVPanel
 */
public class DisplayScheme {

  /** The name of the DisplayScheme */
  protected String name;
  /** The name of the title font */
  protected String titleFont;
  /** The name of the display font */
  protected String displayFont;
  /** A map of the name of the elements and their colors*/
  protected Map<ScriptToken, Color> elementColors = new Hashtable<ScriptToken, Color>();
  /** Specifies if the display scheme is the default */
  protected boolean isDefault = false;

  /**
   * Intialises a DisplayScheme with the given name
   * @param name the name of the <code>DisplayScheme</code>
   */
  public DisplayScheme(String name){
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
  public Color getColor(ScriptToken st){
    return elementColors.get(st);
  }

  /**
   * Sets the color of an element
   * @param element the name of the element
   * @param color the color the element should have
   */
  public Color setColor(ScriptToken st, Color color){
    System.out.println(name + " " + st + " " + color);
    if (color != null)
      elementColors.put(st, color);
    return color;
  }

  /**
   * Returns a copy of this <code>DisplayScheme</code> with a new name
   * @param newName the new name
   * @return a copy of this <code>DisplayScheme</code> with a new name
   */
  public DisplayScheme copy(String newName){
    DisplayScheme ds = new DisplayScheme(newName);
    ds.setDisplayFont(getDisplayFont());
    for(Map.Entry<ScriptToken, Color> entry: elementColors.entrySet())
      ds.setColor(entry.getKey(), entry.getValue());
    return ds;
  }

  /**
   * Returns a copy of this <code>DisplayScheme</code>
   * @return a copy of this <code>DisplayScheme</code>
   */
  public DisplayScheme copy(){
    return copy(getName());
  }

  protected void setDefaults() {
    setColor(ScriptToken.TITLECOLOR, Color.BLACK);
    setColor(ScriptToken.UNITSCOLOR, Color.RED);
    setColor(ScriptToken.SCALECOLOR, Color.BLACK);
    setColor(ScriptToken.COORDINATESCOLOR, Color.RED);
    setColor(ScriptToken.GRIDCOLOR, Color.LIGHT_GRAY);
    setColor(ScriptToken.PLOTCOLOR, Color.BLUE);
    setColor(ScriptToken.PLOTAREACOLOR, Color.WHITE);
    setColor(ScriptToken.BACKGROUNDCOLOR, new Color(192, 192, 192));
    setColor(ScriptToken.INTEGRALPLOTCOLOR, Color.red);
  }


}
