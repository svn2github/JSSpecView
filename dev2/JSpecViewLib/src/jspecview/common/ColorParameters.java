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

import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.jmol.util.JmolList;

import jspecview.api.JSVPanel;
import jspecview.util.JSVColor;
import jspecview.util.JSVColorUtil;

/**
 * This a representation of the Display Scheme for the spectral display.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public abstract class ColorParameters extends Parameters {

  abstract protected JSVColor getColor3(int r, int g, int b);
  abstract protected JSVColor getColor1(int rgb);
	abstract protected boolean isValidFontName(String value);


  public String titleFontName;
  public String displayFontName;
  public Map<ScriptToken, JSVColor> elementColors; 
  public JSVColor[] plotColors;    
  public boolean isDefault;

  public static JSVColor BLACK;
	public static JSVColor RED;
	public static JSVColor LIGHT_GRAY;
	public static JSVColor DARK_GRAY;
	public static JSVColor BLUE;
	public static JSVColor WHITE;

  public final static JSVColor[] defaultPlotColors = new JSVColor[8];  
  public final static String[] defaultPlotColorNames = new String[] {
      "black",                                                      
      "darkGreen",
      "darkred",
      "orange",
      "magenta",
      "cyan",
      "maroon",
      "darkGray"
  };
  
  /**
   * Intialises a DisplayScheme with the given name
   * @param name the name of the <code>DisplayScheme</code>
   */
  public ColorParameters(String name){
  	super(name);
    BLACK = getColor3(0, 0, 0);
    RED = getColor3(255, 0, 0);
    LIGHT_GRAY = getColor3(200, 200, 200);
    DARK_GRAY = getColor3(80, 80, 80);
    BLUE = getColor3(0, 0, 255);
    WHITE = getColor3(255, 255, 255);
    elementColors = new Hashtable<ScriptToken, JSVColor>();
    setColor(ScriptToken.TITLECOLOR, BLACK);
    setColor(ScriptToken.UNITSCOLOR, RED);
    setColor(ScriptToken.SCALECOLOR, BLACK);
    setColor(ScriptToken.COORDINATESCOLOR, RED);
    setColor(ScriptToken.GRIDCOLOR, LIGHT_GRAY);
    setColor(ScriptToken.PLOTCOLOR, BLUE);
    setColor(ScriptToken.PLOTAREACOLOR, WHITE);
    setColor(ScriptToken.BACKGROUNDCOLOR, getColor3(192, 192, 192));
    setColor(ScriptToken.INTEGRALPLOTCOLOR, RED);
    setColor(ScriptToken.PEAKTABCOLOR, RED);
    setColor(ScriptToken.HIGHLIGHTCOLOR, DARK_GRAY);
    for (int i = 0; i < 8; i++)
    	defaultPlotColors[i] = getColorFromString(defaultPlotColorNames[i]);
    plotColors = new JSVColor[8];
    System.arraycopy(defaultPlotColors, 0, plotColors, 0, 8);
  }
    
  public void setFor(JSVPanel jsvp, ColorParameters ds, boolean includeMeasures) {

    if (ds == null)
      ds = this;

    // measures -- not in displayScheme?

    if (includeMeasures)
      jsvp.getPanelData().setBooleans(ds, null);

    // colors and fonts

    PanelData pd = jsvp.getPanelData();
    if (pd.getCurrentPlotColor(1) != null) // integration
      pd.setPlotColors(plotColors);
    pd.setColorOrFont(ds, null);
 }

	public void set(PanelData pd, ScriptToken st, String value) {
		Object param = null;
		switch (st) {
		default:
			setP(pd, st, value);
			return;
		case PLOTCOLORS:
			if (pd == null)
				getPlotColors(value);
			else
				pd.setPlotColors(getPlotColors(value));
			return;
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
			break;
		}
		if (pd == null)
			return;
		if (param != null)
			pd.setColorOrFont(this, st);
	}

//////////////////////////// COLORS /////////////////////////
	
  /**
   * Gets the color of an element in the scheme
   * @param st 
   * @return the <code>Color</code> of the element
   */
  public JSVColor getElementColor(ScriptToken st){
    return elementColors.get(st);
  }


  /**
   * Sets the color of an element
   * @param st 
   * @param color the color the element should have
   * @return color object 
   */
  public JSVColor setColor(ScriptToken st, JSVColor color){
    if (color != null)
      elementColors.put(st, color);
    return color;
  }

  /**
   * Returns a copy of this <code>DisplayScheme</code> with a new name
   * @param newName the new name
   * @return a copy of this <code>DisplayScheme</code> with a new name
   */
  abstract public ColorParameters copy(String newName);

  /**
   * Returns a copy of this <code>DisplayScheme</code>
   * @return a copy of this <code>DisplayScheme</code>
   */
  public ColorParameters copy(){
    return copy(name);
  }

	protected ColorParameters setElementColors(ColorParameters p) {
    displayFontName = p.displayFontName;
    for(Map.Entry<ScriptToken, JSVColor> entry: p.elementColors.entrySet())
      setColor(entry.getKey(), entry.getValue());
    return this;
	}

  public JSVColor getColorFromString(String name) {
  	return getColor1(JSVColorUtil.getArgbFromString(name));
	}
  
	/**
	 * @param plotColorsStr  
   * @return Color[]
	 */
  protected JSVColor[] getPlotColors(String plotColorsStr) {
    if (plotColorsStr == null) {
      plotColors[0] = getElementColor(ScriptToken.PLOTCOLOR);
      return plotColors;
    }
    StringTokenizer st = new StringTokenizer(plotColorsStr, ",;.- ");
    JmolList<JSVColor> colors = new JmolList<JSVColor>();
    try {
      while (st.hasMoreTokens())
        colors.addLast(getColorFromString(st.nextToken()));
    } catch (Exception e) {
      return null;
    }
    return colors.toArray(new JSVColor[colors.size()]);
  }

  /**
	 * @param st  
   * @param value 
   * @return color object
	 */
  protected Object setColorFromString(ScriptToken st, String value) {
    // overridden in AwtParameters
    return null;
  }
  
////////////////// fonts /////////////////  

  /**
	 * @param st 
   * @param value  
   * @return font object
	 */
  @SuppressWarnings("incomplete-switch")
  public String getFontName(ScriptToken st, String value) {
  	boolean isValid = isValidFontName(value);
    switch (st) {
    case TITLEFONTNAME:
      return (isValid ? titleFontName = value : titleFontName);
    case DISPLAYFONTNAME:
      return (isValid ? displayFontName = value : displayFontName);
    }
    return null;
  }

}
