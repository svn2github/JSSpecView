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
import java.util.HashMap;
import java.util.Iterator;

/**
 * This a representation of the Display Scheme for the spectral display.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.common.JSVPanel
 */
public class DisplayScheme {

  /** The name of the DisplayScheme */
  private String name;
  /** The name of the font */
  private String font;
  /** A map of the name of the elements and their colors*/
  private HashMap<String, Color> elementColors;
  /** Specifies if the display scheme is the default */
  private boolean isDefault = false;

  /**
   * Intialises a DisplayScheme with the given name
   * @param name the name of the <code>DisplayScheme</code>
   */
  public DisplayScheme(String name){
    this.name = name;
    elementColors = new HashMap<String, Color>();
    font = null;
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
   * Returns the font name used in this <code>DisplayScheme</code>
   * @return the font name used in this <code>DisplayScheme</code>
   */
  public String getFont(){
    return font;
  }

  /**
   * Sets the font name
   * @param fontName the name of the font
   */
  public void setFont(String fontName){
    font = fontName;
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
  public Color getColor(String element){
    Color color = ((Color)elementColors.get(element));
    return color;
  }

  /**
   * Sets the color of an element
   * @param element the name of the element
   * @param color the color the element should have
   */
  public void setColor(String element, Color color){
    elementColors.put(element, color);
  }

  /**
   * Returns a copy of this <code>DisplayScheme</code> with a new name
   * @param newName the new name
   * @return a copy of this <code>DisplayScheme</code> with a new name
   */
  public DisplayScheme copy(String newName){
    DisplayScheme ds = new DisplayScheme(newName);
    ds.setFont(getFont());
    for(Iterator<String> iter = elementColors.keySet().iterator(); iter.hasNext();){
      String element = (String)iter.next();
      Color color = (Color) elementColors.get(element);
      Color newColor = new Color(color.getRGB());
      ds.setColor(element, newColor);
    }

    return ds;
  }

  /**
   * Returns a copy of this <code>DisplayScheme</code>
   * @return a copy of this <code>DisplayScheme</code>
   */
  public DisplayScheme copy(){
    return copy(getName());
  }

}
