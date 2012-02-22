package jspecview.application.common;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

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
  private boolean reversePlot = false;
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
    
    if (includeMeasures) {
      jsvp.setGridOn(gridOn);
      jsvp.setCoordinatesOn(coordinatesOn);
      jsvp.setXScaleOn(xScaleOn);
      jsvp.setYScaleOn(yScaleOn);
      jsvp.setXUnitsOn(xUnitsOn);
      jsvp.setYUnitsOn(yUnitsOn);
    }
   
    // colors
    
    jsvp.setPlotColors(plotColors);

    jsvp.setTitleColor(ds.getColor("title"));
    jsvp.setUnitsColor(ds.getColor("units"));
    jsvp.setScaleColor(ds.getColor("scale"));
    jsvp.setcoordinatesColor(ds.getColor("coordinates"));
    jsvp.setGridColor(ds.getColor("grid"));
    jsvp.setPlotColor(ds.getColor("plot"));
    jsvp.setPlotAreaColor(ds.getColor("plotarea"));
    jsvp.setBackground(ds.getColor("background"));
    jsvp.setIntegralPlotColor(ds.getColor("integral"));

    // fonts
    
    jsvp.setTitleFontName(ds.getTitleFont());
    jsvp.setDisplayFontName(ds.getDisplayFont());

    // misc
    
    jsvp.setReversePlot(reversePlot);
    jsvp.setZoomEnabled(enableZoom);
    jsvp.setTitleBoldOn(titleBoldOn);
  }

  public void set(ScriptParser.ScriptToken st, String value) {
    switch (st) {
    default:
      return;
    case PLOTCOLORS:
      plotColorsStr = value;
      break;
    case REVERSEPLOT:
      reversePlot = Boolean.parseBoolean(value);
      break;
    case COORDINATESON:
      coordinatesOn = Boolean.parseBoolean(value);
      break;
    case GRIDON:
      gridOn = Boolean.parseBoolean(value);
      break;
    case XSCALEON:
      xScaleOn = Boolean.parseBoolean(value);
      break;
    case YSCALEON:
      yScaleOn = Boolean.parseBoolean(value);
      break;
    case XUNITSON:
      xUnitsOn = Boolean.parseBoolean(value);
      break;
    case YUNITSON:
      yUnitsOn = Boolean.parseBoolean(value);
      break;      
    case BACKGROUNDCOLOR:
      setColor("background", value);
      break;
    case COORDINATESCOLOR:
      setColor("coordinates", value);
      break;
    case GRIDCOLOR:
      setColor("grid", value);
      break;
    case PLOTAREACOLOR:
      setColor("plotArea", value);
      break;
    case PLOTCOLOR:
      setColor("plot", value);
      break;
    case SCALECOLOR:
      setColor("scale", value);
      break;
    case TITLECOLOR:
      setColor("title", value);
      break;
    case UNITSCOLOR:
      setColor("units", value);
      break;
    case INTEGRALPLOTCOLOR:
      setColor("integral", value);
      break;
    case TITLEBOLDON:
      titleBoldOn = Boolean.parseBoolean(value);
      break;
    case TITLEFONTNAME:
      GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
      List<String> fontList = Arrays.asList(g.getAvailableFontFamilyNames());
      for (String s : fontList)
        if (value.equalsIgnoreCase(s)) {
          titleFont = value;
          break;
        }
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
      break;
    }
  }

  private void setColor(String element, String value) {
    setColor(element, AppUtils.getColorFromString(value));
  }

  /**
   * Intialises the <code>plotColors</code> array from the <i>plotColorsStr</i>
   * variable
   */
  public void getPlotColors() {
    if (plotColorsStr != null) {
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
        return;
      } catch (NumberFormatException nfe) {
        return;
      }

      plotColors = (Color[]) colors.toArray(new Color[colors.size()]);
    } else {
      //      plotColors = new Color[specs.size()];
      //      for(int i = 0; i < specs.size(); i++){
      plotColors[0] = getColor("plot");
      //        System.out.println(i+" "+plotColors[i]);
    }
    //    }
  }


}
