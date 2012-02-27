package jspecview.common;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import jspecview.common.JSpecViewUtils;
import jspecview.util.ColorUtil;
import jspecview.util.TextFormat;

public class Parameters extends DisplayScheme {

  public Parameters(String name) {
    super(name);
    setDefaults();
  }

  protected void setDefaults() {
    super.setDefaults();
    plotColors = new Color[defaultPlotColors.length];
    System.arraycopy(defaultPlotColors, 0, plotColors, 0, plotColors.length);
  }
  public final static Color[] defaultPlotColors = { Color.blue, Color.green, Color.yellow,
      Color.orange, Color.red, Color.magenta, Color.pink, Color.cyan,
      Color.darkGray };

  private Color[] plotColors;  
  private String plotColorsStr;
  boolean reversePlot = false;
  private boolean enableZoom = true;
  private boolean titleBoldOn = false;


  // specific to parmaters?
  public boolean coordinatesOn = true;
  public boolean gridOn = true;
  public boolean xScaleOn = true;
  public boolean yScaleOn = true;
  boolean xUnitsOn = true;
  boolean yUnitsOn = true;
  

  public void setFor(JSVPanel jsvp, DisplayScheme ds, boolean includeMeasures) {
    
    if (ds == null)
      ds = this;

    // measures -- not in displayScheme?
    
    if (includeMeasures)
      jsvp.setBoolean(this, null);

    // colors and fonts
    
    if(jsvp.getPlotColor(1) != null) // integration
    jsvp.setPlotColors(plotColors);
    jsvp.setParam(ds, null);

    // misc
    
    jsvp.setZoomEnabled(enableZoom);
    jsvp.setTitleBoldOn(titleBoldOn);
  }

  public void set(JSVPanel jsvp, ScriptToken st, String value) {
    Object param = null;
    
    switch (st) {
    default:
      return;
    case DEBUG:
      JSpecViewUtils.DEBUG = parseBoolean(value);
      return;
    case PLOTCOLORS:
      plotColorsStr = value;
      if (jsvp == null)
        getPlotColors();
      else
        jsvp.setPlotColors(getPlotColors(value));
      return;
    case REVERSEPLOT:
      reversePlot = parseBoolean(value);
      break;
    case COORDINATESON:
      coordinatesOn = parseBoolean(value);
      break;
    case GRIDON:
      gridOn = parseBoolean(value);
      break;
    case XSCALEON:
      xScaleOn = parseBoolean(value);
      break;
    case YSCALEON:
      yScaleOn = parseBoolean(value);
      break;
    case XUNITSON:
      xUnitsOn = parseBoolean(value);
      break;
    case YUNITSON:
      yUnitsOn = parseBoolean(value);
      break;
    case TITLEBOLDON:
      titleBoldOn = parseBoolean(value);
      break;
    case BACKGROUNDCOLOR:
      param = setColor("background", value);
      break;
    case COORDINATESCOLOR:
      param = setColor("coordinates", value);
      break;
    case GRIDCOLOR:
      param = setColor("grid", value);
      break;
    case PLOTAREACOLOR:
      param = setColor("plotarea", value);
      break;
    case PLOTCOLOR:
      param = setColor("plot", value);
      break;
    case SCALECOLOR:
      param = setColor("scale", value);
      break;
    case TITLECOLOR:
      param = setColor("title", value);
      break;
    case UNITSCOLOR:
      param = setColor("units", value);
      break;
    case INTEGRALPLOTCOLOR:
      param = setColor("integral", value);
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
    case ZOOM:
      value = TextFormat.replaceAllCharacters(value, " ,",' ').trim();
      String[] minmax = TextFormat.split(value, ' ');
      if (jsvp != null)
        jsvp.setZoom(Double.parseDouble(minmax[0]), Double.parseDouble(minmax[1]));
      break;
    }
    if (jsvp == null)
      return;
    if (param != null)
      jsvp.setParam(this, st);
    else
      jsvp.setBoolean(this, st);
  }

  private static boolean parseBoolean(String value) {
    if (value.length() == 0)
      value = "true";
    return Boolean.valueOf(value);
  }

  private Color setColor(String element, String value) {
    return setColor(element, ColorUtil.getColorFromString(value));
  }

  /**
   * Intialises the <code>plotColors</code> array from the <i>plotColorsStr</i>
   * variable
   */
  public void getPlotColors() {
    if (plotColorsStr != null) {
      plotColors = getPlotColors(plotColorsStr);
    } else {
      plotColors[0] = getColor("plot");
    }
  }

  private Color[] getPlotColors(String plotColorsStr) {
    StringTokenizer st = new StringTokenizer(plotColorsStr, ",;.- ");
    int r, g, b;
    List<Color> colors = new ArrayList<Color>();
    try {
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        if (token.startsWith("#")) {
          colors.add(new Color(Integer
              .parseInt(token.substring(1), 16)));
        } else {
          r = Integer.parseInt(token.trim());
          g = Integer.parseInt(st.nextToken().trim());
          b = Integer.parseInt(st.nextToken().trim());
          colors.add(new Color(r, g, b));
        }
      }
    } catch (NoSuchElementException nsee) {
      return null;
    } catch (NumberFormatException nfe) {
      return null;
    }
    return (Color[]) colors.toArray(new Color[colors.size()]);
  }
}
