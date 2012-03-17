package jspecview.common;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import jspecview.util.Logger;

public class Parameters extends DisplayScheme {

  public Parameters(String name) {
    super(name);
    setDefaults();
  }

  protected void setDefaults() {
    super.setDefaults();
    setBoolean(ScriptToken.TITLEON, !name.equals("application"));
    setBoolean(ScriptToken.ENABLEZOOM, true);
    setBoolean(ScriptToken.DISPLAY2D, true);
    setBoolean(ScriptToken.COORDINATESON, true);
    setBoolean(ScriptToken.GRIDON, true);
    setBoolean(ScriptToken.XSCALEON, true);
    setBoolean(ScriptToken.YSCALEON, true);
    setBoolean(ScriptToken.XUNITSON, true);
    setBoolean(ScriptToken.YUNITSON, true);

    plotColors = new Color[defaultPlotColors.length];
    System.arraycopy(defaultPlotColors, 0, plotColors, 0, plotColors.length);
  }
  public final static Color[] defaultPlotColors = { Color.blue, Color.green, Color.yellow,
      Color.orange, Color.red, Color.magenta, Color.pink, Color.cyan,
      Color.darkGray };

  private Color[] plotColors;  
  private String plotColorsStr;

  public double integralMinY = IntegralGraph.DEFAULT_MINY;
  public double integralFactor = IntegralGraph.DEFAULT_FACTOR;
  public double integralOffset = IntegralGraph.DEFAULT_OFFSET;

  public void setFor(JSVPanel jsvp, DisplayScheme ds, boolean includeMeasures) {

    if (ds == null)
      ds = this;

    // measures -- not in displayScheme?

    if (includeMeasures)
      jsvp.setBoolean(this, null);

    // colors and fonts

    if (jsvp.getPlotColor(1) != null) // integration
      jsvp.setPlotColors(plotColors);
    jsvp.setParam(ds, null);
 }

  public void set(JSVPanel jsvp, ScriptToken st, String value) {
    Object param = null;
    List<String> tokens;
    switch (st) {
    default:
      return;
    case PLOTCOLORS:
      plotColorsStr = value;
      if (jsvp == null)
        getPlotColors();
      else
        jsvp.setPlotColors(getPlotColors(value));
      return;
    case ZOOM:
      if (jsvp != null) {
        tokens = ScriptToken.getTokens(value);
        switch (tokens.size()) {
        case 1:
          if (tokens.get(0).equalsIgnoreCase("out"))
            jsvp.setZoom(0, 0, 0, 0);
          break;
        case 2:
          jsvp.setZoom(Double.parseDouble(tokens.get(0)), 0, Double
              .parseDouble(tokens.get(1)), 0);
          break;
        case 4:
          jsvp.setZoom(
              Double.parseDouble(tokens.get(0)), 
              Double.parseDouble(tokens.get(1)), 
              Double.parseDouble(tokens.get(2)), 
              Double.parseDouble(tokens.get(3))); 
        }
      }
      break;
    case DEBUG:
      Logger.debugging = isTrue(value);
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
      setBoolean(st, value);
      break;
    case BACKGROUNDCOLOR:
    case COORDINATESCOLOR:
    case GRIDCOLOR:
    case INTEGRALPLOTCOLOR:
    case PLOTAREACOLOR:
    case PLOTCOLOR:
    case SCALECOLOR:
    case TITLECOLOR:
    case UNITSCOLOR:
      param = setColor(st, value);
      break;
    case TITLEFONTNAME:
      GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
      List<String> fontList = Arrays.asList(g.getAvailableFontFamilyNames());
      for (String s : fontList)
        if (value.equalsIgnoreCase(s)) {
          titleFont = value;
          break;
        }
      param = titleFont;
      break;
    case DISPLAYFONTNAME:
      GraphicsEnvironment g2 = GraphicsEnvironment
          .getLocalGraphicsEnvironment();
      List<String> fontList2 = Arrays.asList(g2.getAvailableFontFamilyNames());
      for (String s2 : fontList2)
        if (value.equalsIgnoreCase(s2)) {
          displayFont = value;
          break;
        }
      param = displayFont;
      break;
    }
    if (jsvp == null)
      return;
    if (param != null)
      jsvp.setParam(this, st);
    else
      jsvp.setBoolean(this, st);
  }

  private Map<ScriptToken, Boolean> htBooleans = new Hashtable<ScriptToken, Boolean>();
  
  public static boolean isTrue(String value) {
    return (value.length() == 0 || Boolean.parseBoolean(value)); 
  }
  
  public boolean setBoolean(ScriptToken st, String value) {
    return setBoolean(st, isTrue(value));
  }

  public boolean setBoolean(ScriptToken st, boolean val) {
    if (val)
      htBooleans.put(st, Boolean.TRUE);
    else
      htBooleans.remove(st);
    return val;
  }

  public boolean getBoolean(ScriptToken t) {
    return htBooleans.containsKey(t);
  }
    
  private Color setColor(ScriptToken st, String value) {
    return setColor(st, AppUtils.getColorFromString(value));
  }

  /**
   * Intialises the <code>plotColors</code> array from the <i>plotColorsStr</i>
   * variable
   */
  public void getPlotColors() {
    if (plotColorsStr != null) {
      plotColors = getPlotColors(plotColorsStr);
    } else {
      plotColors[0] = getColor(ScriptToken.PLOTCOLOR);
    }
  }

  private Color[] getPlotColors(String plotColorsStr) {
    StringTokenizer st = new StringTokenizer(plotColorsStr, ",;.- ");
    List<Color> colors = new ArrayList<Color>();
    try {
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        colors.add(AppUtils.getColorFromString(token));
      }
    } catch (NoSuchElementException nsee) {
      return null;
    } catch (NumberFormatException nfe) {
      return null;
    }
    return (Color[]) colors.toArray(new Color[colors.size()]);
  }

}
