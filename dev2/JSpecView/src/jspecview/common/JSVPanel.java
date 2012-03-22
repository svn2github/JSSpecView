/* Copyright (c) 2002-2012 The University of the West Indies
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

// CHANGES to 'JSVPanel.java'
// University of the West Indies, Mona Campus
//
// 25-06-2007 rjl - bug in ReversePlot for non-continuous spectra fixed
//                - previously, one point less than npoints was displayed
// 25-06-2007 cw  - show/hide/close modified
// 10-02-2009 cw  - adjust for non zero baseline in North South plots
// 24-08-2010 rjl - check coord output is not Internationalised and uses decimal point not comma
// 31-10-2010 rjl - bug fix for drawZoomBox suggested by Tim te Beek
// 01-11-2010 rjl - bug fix for drawZoomBox
// 05-11-2010 rjl - colour the drawZoomBox area suggested by Valery Tkachenko
// 23-07-2011 jak - Added feature to draw the x scale, y scale, x units and y units
//					independently of each other. Added independent controls for the font,
//					title font, title bold, and integral plot color.
// 24-09-2011 jak - Altered drawGraph to fix bug related to reversed highlights. Added code to
//					draw integration ratio annotations
// 03-06-2012 rmh - Full overhaul; code simplification; added support for Jcamp 6 nD spectra

package jspecview.common;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet; //import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.util.Logger;
import jspecview.util.TextFormat;

/**
 * JSVPanel class draws a plot from the data contained a instance of a
 * <code>Graph</code>.
 * 
 * @see jspecview.common.Graph
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class JSVPanel extends JPanel implements Printable, MouseListener,
    MouseMotionListener, KeyListener {

  private static final long serialVersionUID = 1L;

  @Override
  public void finalize() {
    Logger.info("JSVPanel " + this + " finalized");
  }

  public void dispose() {
    for (int i = 0; i < graphSets.size(); i++)
      graphSets.get(i).dispose();
    graphSets = null;
    if (popup != null) {
      popup.dispose();
      popup = null;
    }
    removeKeyListener(this);
    removeMouseListener(this);
    removeMouseMotionListener(this);
  }

  // Critical fields

  private List<JSVGraphSet> graphSets;
  JSVGraphSet currentGraphSet;
  protected JSVPanelPopupMenu popup;

  public JSVPanelPopupMenu getPopup() {
    return popup;
  }

  private Map<String, NumberFormat> htFormats = new Hashtable<String, NumberFormat>();

  // sliders and zoom boxes

  // plot parameters

  private int defaultHeight = 450;
  private int defaultWidth = 280;
  private int leftPlotAreaPos = 80, topPlotAreaPos = 30;
  private int plotAreaWidth, plotAreaHeight;
  private Insets plotAreaInsets = new Insets(topPlotAreaPos, leftPlotAreaPos,
      50, 50);

  // static parameters
  static int minNumOfPointsForZoom = 3;

  // current values

  Coordinate coordClicked;
  Coordinate[] coordsClicked;

  private boolean drawXAxisLeftToRight;
  private boolean xAxisLeftToRight = true;
  boolean isIntegralDrag;
  protected boolean isOverlaid;

  public boolean isOverlaid() {
    return isOverlaid;
  }

  private int nSpectra;
  int thisWidth;
  int thisPlotHeight;
  PlotWidget thisWidget;

  double lastClickX = Double.MAX_VALUE;

  String coordStr = "";
  String startupPinTip = "Click to set.";
  private String title;

  public void setTitle(String title) {
    this.title = title;
    setName(title);
  }

  //////// settable parameters //////////

  private boolean coordinatesOn = true;
  boolean display1D = false; // with 2D
  boolean display2D = true;
  private boolean enableZoom = true;
  private boolean gridOn = false;
  private boolean reversePlot = false;
  private boolean titleBoldOn = false;
  private boolean titleOn = true;
  private boolean xScaleOn = true;
  private boolean xUnitsOn = true;
  private boolean yScaleOn = true;
  private boolean yUnitsOn = true;

  Hashtable<String, Object> options = new Hashtable<String, Object>();

  public Map<String, Object> getInfo(boolean isSelected, String key) {
    Map<String, Object> info = new Hashtable<String, Object>();
    Set<Entry<String, Object>> entries = options.entrySet();
    for (Entry<String, Object> entry : entries)
      JDXSpectrum.putInfo(key, info, entry.getKey(), entry.getValue());
    JDXSpectrum.putInfo(key, info, "selected", Boolean.valueOf(isSelected));
    JDXSpectrum.putInfo(key, info, "type", getSpectrumAt(0).getDataType());
    JDXSpectrum.putInfo(key, info, "title", title);
    JDXSpectrum.putInfo(key, info, "nSets", Integer.valueOf(graphSets.size()));
    JDXSpectrum.putInfo(key, info, "userYFactor", Double
        .valueOf(currentGraphSet.userYFactor));
    List<Map<String, Object>> sets = new ArrayList<Map<String, Object>>();
    for (int i = graphSets.size(); --i >= 0;)
      sets.add(graphSets.get(i).getInfo(key, isSelected && graphSets.get(i) == currentGraphSet));
    info.put("sets", sets);
    return info;
  }

  public void setBoolean(Parameters parameters, ScriptToken st) {
    if (st == null) {
      Map<ScriptToken, Boolean> booleans = parameters.getBooleans();
      for (Map.Entry<ScriptToken, Boolean> entry : booleans.entrySet())
        setBoolean(parameters, entry.getKey());
      return;
    }
    boolean isTrue = parameters.getBoolean(st);
    options.put(st.name(), (isTrue ? Boolean.TRUE : Boolean.FALSE));
    switch (st) {
    case COORDINATESON:
      coordinatesOn = isTrue;
      break;
    case DISPLAY1D:
      display1D = isTrue;
      thisWidth = 0;
      break;
    case DISPLAY2D:
      display2D = isTrue;
      thisWidth = 0;
      break;
    case ENABLEZOOM:
      enableZoom = isTrue;
      break;
    case GRIDON:
      gridOn = isTrue;
      break;
    case REVERSEPLOT:
      reversePlot = isTrue;
      break;
    case TITLEBOLDON:
      titleBoldOn = isTrue;
      break;
    case TITLEON:
      titleOn = isTrue;
      break;
    case XSCALEON:
      xScaleOn = isTrue;
      break;
    case XUNITSON:
      xUnitsOn = isTrue;
      break;
    case YSCALEON:
      yScaleOn = isTrue;
      break;
    case YUNITSON:
      yUnitsOn = isTrue;
      break;
    default:
      Logger.warn("JSVPanel --- unrecognized Parameter boolean: " + st);
      break;
    }
  }

  ////////// settable colors and fonts //////////

  String displayFontName;
  private String titleFontName;
  private Color coordinatesColor;
  private Color gridColor;
  private Color integralPlotColor;
  private Color plotAreaColor;
  private Color scaleColor;
  private Color titleColor;
  private Color unitsColor;

  public void setPlotColors(Color[] colors) {
    for (int i = graphSets.size(); --i >= 0;)
      graphSets.get(i).setPlotColors(colors);
  }

  // potentially settable; 

  private Color highlightColor = new Color(255, 0, 0, 200);

  public Color setHighlightColor(Color color) {
    return (highlightColor = color);
  }

  public Color getHighlightColor() {
    return highlightColor;
  }

  private Color zoomBoxColor = new Color(100, 100, 50, 130);

  public Color getZoomBoxColor() {
    return zoomBoxColor;
  }

  public void setColorOrFont(DisplayScheme ds, ScriptToken st) {
    if (st == null) {
      Map<ScriptToken, Color> colors = ds.getColors();
      for (Map.Entry<ScriptToken, Color> entry : colors.entrySet())
        setColorOrFont(ds, entry.getKey());
      setColorOrFont(ds, ScriptToken.DISPLAYFONTNAME);
      setColorOrFont(ds, ScriptToken.TITLEFONTNAME);
      return;
    }
    switch (st) {
    case DISPLAYFONTNAME:
      displayFontName = ds.getDisplayFont();
      if (displayFontName != null)
        options.put(st.name(), displayFontName);
      return;
    case TITLEFONTNAME:
      titleFontName = ds.getTitleFont();
      if (titleFontName != null)
        options.put(st.name(), titleFontName);
      return;
    }
    Color color = ds.getColor(st);
    if (color != null)
      options.put(st.name(), AppUtils.colorToHexString(color));
    switch (st) {
    case BACKGROUNDCOLOR:
      setBackground(color);
      break;
    case COORDINATESCOLOR:
      coordinatesColor = color;
      break;
    case GRIDCOLOR:
      gridColor = color;
      break;
    case INTEGRALPLOTCOLOR:
      integralPlotColor = color;
      break;
    case PLOTCOLOR:
      for (int i = graphSets.size(); --i >= 0;)
        graphSets.get(i).setPlotColor0(color);
      break;
    case PLOTAREACOLOR:
      plotAreaColor = color;
      break;
    case SCALECOLOR:
      scaleColor = color;
      break;
    case TITLECOLOR:
      titleColor = color;
      break;
    case UNITSCOLOR:
      unitsColor = color;
      break;
    default:
      Logger.warn("JSVPanel --- unrecognized DisplayScheme color: " + st);
      break;
    }
  }

  /////////// print parameters ///////////

  boolean isPrinting;
  private boolean printGrid = gridOn;
  private boolean printTitle = true;
  private boolean printScale = true;
  String printingFont;
  private String printGraphPosition = "default";

  // listeners to handle various events

  private ArrayList<PanelListener> listeners = new ArrayList<PanelListener>();
  private List<Graph> spectra;

  /**
   * Constructs a new JSVPanel
   * 
   * @param spectrum
   *        the spectrum
   * @throws ScalesIncompatibleException
   */
  public JSVPanel(Graph spectrum, JSVPanelPopupMenu popup) {
    // standard applet not overlaid and not showing range
    // standard application split spectra
    // removal of integration, taConvert
    // Preferences Dialog sample.jdx

    this.popup = popup;
    List<Graph> spectra = new ArrayList<Graph>();
    spectra.add(spectrum);
    initJSVPanel(spectra, 0, 0);
  }

  public static JSVPanel getJSVPanel(List<JDXSpectrum> specs, int startIndex, int endIndex, JSVPanelPopupMenu popup) {
    List<Graph> graphs = new ArrayList<Graph>(specs.size());
    for (int i = 0; i < specs.size(); i++)
      graphs.add(specs.get(i));
    JSVPanel jsvp = new JSVPanel(graphs, startIndex, endIndex, popup);
    jsvp.isOverlaid = (specs.size() > 1);
    return jsvp;
  }

  /**
   * Constructs a <code>JSVPanel</code> with List of spectra and corresponding
   * start and end indices of data points that should be displayed
   * 
   * @param spectra
   *        the List of <code>Graph</code> instances
   * @param startIndices
   *        the start indices
   * @param endIndices
   *        the end indices
   * @throws JSpecViewException
   * @throws ScalesIncompatibleException
   */
  private JSVPanel(List<Graph> spectra, int startIndex,
      int endIndex, JSVPanelPopupMenu popup) {
    this.popup = popup;
    initJSVPanel(spectra, startIndex, endIndex);
  }

  public static JSVPanel getIntegralPanel(JDXSpectrum spectrum, Color color,
                                          JSVPanelPopupMenu popup) {
    Graph graph = spectrum.getIntegrationGraph();
    List<Graph> graphs = new ArrayList<Graph>();
    graphs.add(spectrum);
    graphs.add(graph);
    JSVPanel jsvp = new JSVPanel(graphs, 0, 0, popup);
    jsvp.setTitle(graph.getTitle());
    jsvp.setPlotColors(new Color[] { jsvp.getPlotColor(0), color });
    return jsvp;
  }

  private void initJSVPanel(List<Graph> spectra, int startIndex, int endIndex) {
    setBorder(BorderFactory.createLineBorder(Color.lightGray));
    nSpectra = spectra.size();
    this.spectra = spectra;
    graphSets = JSVGraphSet.getGraphSets(this, spectra, startIndex, endIndex);
    currentGraphSet = graphSets.get(0);
    setTitle(getSpectrum().getTitleLabel());
    if (popup == null) {
      // preferences dialog
      coordStr = "(0,0)";
    } else {
      addKeyListener(this);
      addMouseListener(this);
      addMouseMotionListener(this);
    }
  }

  private void setCurrentGraphSet(JSVGraphSet gs) {
    if (currentGraphSet == gs)
      return;
    currentGraphSet = gs;
    notifySubSpectrumChange(gs.getSpectrum().getSubIndex(), gs.getSpectrum());
  }

  public JDXSpectrum getSpectrum() {
    return currentGraphSet.getSpectrum().getCurrentSubSpectrum();
  }

  /**
   * Returns the <code>Spectrum</code> at the specified index
   * 
   * @param index
   *        the index of the <code>Spectrum</code>
   * @return the <code>Spectrum</code> at the specified index
   */
  public JDXSpectrum getSpectrumAt(int index) {
    return currentGraphSet.getSpectrumAt(index);
  }

  /**
   * Sets the integration ratios that will be displayed
   * 
   * @param ratios
   *        array of the integration ratios
   */
  public void setIntegrationRatios(ArrayList<Annotation> ratios) {
    currentGraphSet.setIntegrationRatios(ratios);
  }

  public void setDisplay1Dwith2D(boolean TF) {
    display1D = TF;
    thisWidth = 0;
  }

  public void setDisplay2D(boolean TF) {
    display2D = TF;
    thisWidth = 0;
  }

  /**
   * Displays plot in reverse if val is true
   * 
   * @param val
   *        true or false
   */
  public void setReversePlot(boolean val) {
    reversePlot = val;
    setDrawXAxis();
  }

  private void setDrawXAxis() {
    drawXAxisLeftToRight = xAxisLeftToRight ^ reversePlot;
    getSpectrum().setExportXAxisDirection(drawXAxisLeftToRight);
  }

  /* -------------------Other methods ------------------------------------*/

  /**
   * Add information about a region of the displayed spectrum to be highlighted
   * applet only right now
   * 
   * @param x1
   *        the x value of the coordinate where the highlight should start
   * @param x2
   *        the x value of the coordinate where the highlight should end
   * @param color
   *        the color of the highlight
   */
  public void addHighlight(double x1, double x2, Color color) {
    currentGraphSet.addHighlight(x1, x2, null, color);
  }

  /**
   * Remove the highlight at the specified index in the internal list of
   * highlights The index depends on the order in which the highlights were
   * added
   * 
   * @param index
   *        the index of the highlight in the list
   */
  public void removeHighlight(int index) {
    currentGraphSet.removeHighlight(index);
  }

  /**
   * Remove the highlight specified by the starting and ending x value
   * 
   * @param x1
   *        the x value of the coordinate where the highlight started
   * @param x2
   *        the x value of the coordinate where the highlight ended
   */
  public void removeHighlight(double x1, double x2) {
    currentGraphSet.removeHighlight(x1, x2);
  }

  /**
   * Removes all highlights from the display
   */
  public void removeAllHighlights() {
    currentGraphSet.removeAllHighlights();
  }

  /**
   * Returns true if plot is reversed
   * 
   * @return true if plot is reversed
   */
  public boolean isPlotReversed() {
    return reversePlot;
  }

  /**
   * Returns true if zoom is enabled
   * 
   * @return true if zoom is enabled
   */
  public boolean isZoomEnabled() {
    return enableZoom;
  }

  /**
   * Returns true if coordinates are on
   * 
   * @return true if coordinates are displayed
   */
  public boolean isCoordinatesOn() {
    return coordinatesOn;
  }

  /**
   * Returns true if grid is on
   * 
   * @return true if the grid is displayed
   */
  public boolean isGridOn() {
    return gridOn;
  }

  /**
   * Returns true if x Scale is on
   * 
   * @return true if x Scale is displayed
   */
  public boolean isXScaleOn() {
    return xScaleOn;
  }

  /**
   * Returns true if y Scale is on
   * 
   * @return true if the y Scale is displayed
   */
  public boolean isYScaleOn() {
    return yScaleOn;
  }

   /**
   * Returns the Number of Graph sets
   * 
   * @return the Number of graph sets
   */
  public int getNumberOfSpectraTotal() {
    return nSpectra;
  }

  /**
   * Returns the Number of Graph sets
   * 
   * @return the Number of graph sets
   */
  public int getNumberOfGraphSets() {
    return graphSets.size();
  }

  public int getNumberOfSpectraInCurrentSet() {
    return currentGraphSet.getNumberOfSpectra();
  }

  /**
   * Returns the title displayed on the graph
   * 
   * @return the title displayed on the graph
   */
  public String getTitle() {
    return title;
  }

  /**
   * Returns the color of the plotArea
   * 
   * @return the color of the plotArea
   */
  public Color getPlotAreaColor() {
    return plotAreaColor;
  }

  /**
   * Returns the color of the plot at a certain index
   * 
   * @param index
   *        the index
   * @return the color of the plot
   */
  public Color getPlotColor(int index) {
    //TODO -- what about this?
    return currentGraphSet.getPlotColor(index);
  }

  /**
   * Returns the color of the integral plot
   * 
   * @return the color of the integral plot
   */
  public Color getIntegralPlotColor() {
    return integralPlotColor;
  }

  /**
   * Returns the color of the scale
   * 
   * @return the color of the scale
   */
  public Color getScaleColor() {
    return scaleColor;
  }

  /**
   * Returns the color of the units
   * 
   * @return the color of the units
   */
  public Color getUnitsColor() {
    return unitsColor;
  }

  /**
   * Returns the color of the title
   * 
   * @return the color of the title
   */
  public Color getTitleColor() {
    return titleColor;
  }

  /**
   * Returns the color of the grid
   * 
   * @return the color of the grid
   */
  public Color getGridColor() {
    return gridColor;
  }

  /**
   * Return the start indices of the Scaledata
   * 
   * @return the start indices of the Scaledata
   */
  public int[] getStartDataPointIndices() {
    return currentGraphSet.multiScaleData.startDataPointIndices;
  }

  /**
   * Return the end indices of the Scaledata
   * 
   * @return the end indices of the Scaledata
   */
  public int[] getEndDataPointIndices() {
    return currentGraphSet.multiScaleData.endDataPointIndices;
  }

  /**
   * Returns the name of the font used in the display
   * 
   * @return the name of the font used in the display
   */
  public String getDisplayFontName() {
    return displayFontName;
  }

  /**
   * Returns the font of the title
   * 
   * @return the font of the title
   */
  public String getTitleFontName() {
    return titleFontName;
  }

  /*----------------------- JSVPanel PAINTING METHODS ---------------------*/

  /**
   * Overides paintComponent in class JPanel in order to draw the spectrum
   * 
   * @param g
   *        the <code>Graphics</code> object
   */
  @Override
  public void paintComponent(Graphics g) {
    if (!isEnabled() || graphSets == null)
      return;
    super.paintComponent(g);
    drawGraph(g, getHeight(), getWidth());
  }

  /**
   * Draws the Spectrum to the panel
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  private void drawGraph(Graphics g, int height, int width) {

    boolean withGrid, withTitle, withXUnits, withYUnits, 
        withCoords, withXScale, withYScale, withSliders;

    if (isPrinting) {
      withCoords = false;
      withTitle = printTitle;
      withGrid = printGrid;
      withXScale = printScale;
      withYScale = printScale;
      withXUnits = printScale;
      withYUnits = printScale;
      withSliders = false;
    } else {
      withCoords = coordinatesOn;
      withTitle = titleOn;
      withGrid = gridOn;
      withXUnits = xUnitsOn;
      withYUnits = yUnitsOn;
      withXScale = xScaleOn;
      withYScale = yScaleOn;
      withSliders = true;
    }
    plotAreaWidth = width - (plotAreaInsets.right + plotAreaInsets.left);
    plotAreaHeight = height - (plotAreaInsets.top + plotAreaInsets.bottom);
    boolean isResized = (thisWidth != width || thisPlotHeight != plotAreaHeight);
    if (isResized) {
      requestFocusInWindow();
    }
    thisWidth = width;
    thisPlotHeight = plotAreaHeight;

    for (int i = graphSets.size(); --i >= 0;)
      graphSets.get(i).drawGraph(g, withGrid, withXUnits, withYUnits,
          withXScale, withYScale, withSliders, !isIntegralDrag, height, width,
          plotAreaInsets, isResized, enableZoom);
    if (withTitle)
      drawTitle(g, height, width, getSpectrum().getPeakTitle());
    if (withCoords)
      drawCoordinates(g, height, width);
  }

  NumberFormat getFormatter(String hash) {
    NumberFormat formatter = htFormats.get(hash);
    if (formatter == null)
      htFormats.put(hash, formatter = TextFormat.getDecimalFormat(hash));
    return formatter;
  }

  void setFont(Graphics g, int width, int mode, int size, boolean isLabel) {
    if (isLabel) {
      if (width < 400)
        size = (int) ((width * size) / 400);
    } else {
      if (width < 250)
        size = (int) ((width * size) / 250);
    }
    g.setFont(new Font((isPrinting ? printingFont : displayFontName), mode, size));
  }

  /**
   * Draws Title
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  void drawTitle(Graphics g, int height, int width, String title) {
    setFont(g, width, isPrinting || titleBoldOn ? Font.BOLD : Font.PLAIN, 14,
        true);
    FontMetrics fm = g.getFontMetrics();
    g.setColor(titleColor);
    g.drawString(title, 5, (int) (height - fm
        .getHeight() / 2));
  }

  /**
   * Draws the Coordinates
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  private void drawCoordinates(Graphics g, int height, int width) {
    g.setColor(coordinatesColor);
    setFont(g, width, Font.PLAIN, 12, true);
    g.drawString(coordStr, (int) ((plotAreaWidth + leftPlotAreaPos) * 0.85),
        (int) (topPlotAreaPos - 10));
  }

  /*-------------------- METHODS FOR SCALING AND ZOOM --------------------------*/

  public void setZoom(double x1, double y1, double x2, double y2) {
    currentGraphSet.setZoom(x1, y1, x2, y2);
    thisWidth = 0;
    notifyZoomListeners(x1, y1, x2, y2);
  }

  /**
   * Resets the spectrum to it's original view
   */
  public void reset() {
    currentGraphSet.reset();
  }

  /**
   * Clears all views in the zoom list
   */
  public void clearViews() {
    for (int i = graphSets.size(); --i >= 0; )
      graphSets.get(i).clearViews();
  }

  /**
   * Displays the previous view zoomed
   */
  public void previousView() {
    currentGraphSet.previousView();
  }

  /**
   * Displays the next view zoomed
   */
  public void nextView() {
    currentGraphSet.nextView();
  }

  /*----------------- METHODS IN INTERFACE Printable ---------------------- */

  /**
   * Implements method print in interface printable
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param pf
   *        the <code>PageFormat</code> object
   * @param pi
   *        the page index
   * @return an int that depends on whether a print was successful
   * @throws PrinterException
   */
  public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
    if (pi == 0) {
      Graphics2D g2D = (Graphics2D) g;
      isPrinting = true;

      double height, width;

      if (printGraphPosition.equals("default")) {
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        if (pf.getOrientation() == PageFormat.PORTRAIT) {
          height = defaultHeight;
          width = defaultWidth;
        } else {
          height = defaultWidth;
          width = defaultHeight;
        }
      } else if (printGraphPosition.equals("fit to page")) {
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        height = pf.getImageableHeight();
        width = pf.getImageableWidth();
      } else { // center
        Paper paper = pf.getPaper();
        double paperHeight = paper.getHeight();
        double paperWidth = paper.getWidth();
        int x, y;

        if (pf.getOrientation() == PageFormat.PORTRAIT) {
          height = defaultHeight;
          width = defaultWidth;
          x = (int) (paperWidth - width) / 2;
          y = (int) (paperHeight - height) / 2;
        } else {
          height = defaultWidth;
          width = defaultHeight;
          y = (int) (paperWidth - defaultWidth) / 2;
          x = (int) (paperHeight - defaultHeight) / 2;
        }
        g2D.translate(x, y);
      }

      drawGraph(g2D, (int) height, (int) width);

      isPrinting = false;
      return Printable.PAGE_EXISTS;
    }
    isPrinting = false;
    return Printable.NO_SUCH_PAGE;
  }

  /*--------------------------------------------------------------------------*/

  /**
   * Send a print job of the spectrum to the default printer on the system
   * 
   * @param pl
   *        the layout of the print job
   */
  public void printSpectrum(PrintLayoutDialog.PrintLayout pl) {

    PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();

    if (pl.layout.equals("landscape"))
      aset.add(OrientationRequested.LANDSCAPE);
    else
      aset.add(OrientationRequested.PORTRAIT);

    aset.add(pl.paper);

    //MediaSize size = MediaSize.getMediaSizeForName(pl.paper);

    // Set Graph Properties
    printingFont = pl.font;
    printGrid = pl.showGrid;
    printTitle = pl.showTitle;
    printGraphPosition = pl.position;

    /* Create a print job */
    PrinterJob pj = PrinterJob.getPrinterJob();
    //    PageFormat pf = pj.defaultPage();
    //    pf.setOrientation(PageFormat.LANDSCAPE);
    //    pf = pj.pageDialog(pf);
    //
    //    PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
    //
    //    if(pf.getOrientation() == pf.LANDSCAPE){
    //      aset.add(OrientationRequested.LANDSCAPE);
    //    }else{
    //      aset.add(OrientationRequested.PORTRAIT);
    //    }

    pj.setPrintable(this);

    if (pj.printDialog()) {
      try {
        pj.print(aset);
      } catch (PrinterException ex) {
        ex.printStackTrace();
      }
    }

  }

  /*------------------------- Javascript call functions ---------------------*/

  /**
   * Returns the spectrum coordinate of the point on the display that was
   * clicked
   * 
   * @return the spectrum coordinate of the point on the display that was
   *         clicked
   */
  public Coordinate getClickedCoordinate() {
    return coordClicked;
  }

  /**
   * moving panel click event processing to JSVPanel from applet
   * 
   * @param coord
   * @param actualCoord
   * @return
   */
  public boolean getPickedCoordinates(Coordinate coord, Coordinate actualCoord) {
    if (coordClicked == null)
      return false;
    double x = coordClicked.getXVal();
    coord.setXVal(x);
    coord.setYVal(coordClicked.getYVal());
    if (actualCoord == null)
      return true;
    int pt = Coordinate.getNearestIndexForX(coordsClicked, x);
    actualCoord.setXVal(coordsClicked[pt].getXVal());
    actualCoord.setYVal(coordsClicked[pt].getYVal());
    return true;
  }

  public static JSVPanel taConvert(JSVPanel jsvp, int mode) {
    if (jsvp.getNumberOfSpectraTotal() > 1)
      return null;
    JDXSpectrum spectrum = JDXSpectrum.taConvert(jsvp.getSpectrum(), mode);
    return (spectrum == jsvp.getSpectrum() ? null : new JSVPanel(spectrum,
        jsvp.popup));
  }

  public static void showSolutionColor(Component component, String sltnclr) {
    JOptionPane.showMessageDialog(component, "<HTML><body bgcolor=rgb("
        + sltnclr + ")><br />Predicted Solution Colour- RGB(" + sltnclr
        + ")<br /><br /></body></HTML>", "Predicted Colour",
        JOptionPane.INFORMATION_MESSAGE);
  }

  public String getSolutionColor() {
    Graph spectrum = getSpectrum();
    String Yunits = spectrum.getYUnits();
    return Visible.Colour(spectrum.getXYCoords(), Yunits);
  }

  public void refresh() {
    thisWidth = 0;
    repaint();
  }

  public void addAnnotation(List<String> tokens) {
    currentGraphSet.addAnnotation(tokens);
    // TODO Auto-generated method stub
    
  }

  public void processPeakSelect(PeakInfo peakInfo) {
    System.out.println("JSVPANEL PROCESS " + peakInfo);
    // note that we could have multiple matches? 
    for (int i = 0; i < graphSets.size(); i++)
      graphSets.get(i).processPeakSelect(peakInfo);
  }

  public PeakInfo findPeak(String fileName, String index) {
    PeakInfo pi;
    System.out.println("JSVPanel " + this + "\n looking for " + fileName + " " + index);
    for (int i = graphSets.size(); --i >= 0; )
      if ((pi = graphSets.get(i).findPeak(fileName, index)) != null) {
        System.out.println(" found " + pi);
        return pi;
      }
    return null;
  }

  public void selectSpectrum(String fileName, String type, String model) {
    for (int i = 0; i < graphSets.size(); i+= 1)
      graphSets.get(i).selectSpectrum(fileName, type, model);
  }

  /**
   * Adds a PanelListener
   * 
   * @param listener
   */
  public void addListener(PanelListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /**
   * 
   * @param isub -1 indicates direction if no subspectra or subspectrum index if subspectra
   * @param spec null indicates no subspectra
   */
  public void notifySubSpectrumChange(int isub, JDXSpectrum spec) {
    notifyListeners(new SubSpecChangeEvent(isub, (spec == null ? null : spec.getTitleLabel())));
  }
  
  /**
   * Notifies CoordinatePickedListeners
   * 
   * @param coord
   */
  public void notifyZoomListeners(double x1, double y1, double x2, double y2) {
    notifyListeners(new ZoomEvent(x1, y1, x2, y2));
  }

  /**
   * Notifies CoordinatePickedListeners
   * 
   * @param coord
   */
  public void notifyPeakPickedListeners(Coordinate coord) {
    notifyListeners(new PeakPickEvent(this, coord, getSpectrum()
        .getAssociatedPeakInfo(coord)));
  }

  void notifyListeners(Object eventObj) {
    for (int i = 0; i < listeners.size(); i++)
      if (listeners.get(i) != null)
        listeners.get(i).panelEvent(eventObj);
  }

  /*--------------METHODS IN INTERFACE MouseListener-----------------------*/

  public void mousePressed(MouseEvent e) {
    if (e.getButton() != MouseEvent.BUTTON1)
      return;
    int xPixel = e.getX();
    int yPixel = e.getY();
    JSVGraphSet gs = JSVGraphSet.findGraphSet(graphSets, xPixel, yPixel);
    if (gs == null)
      return;
    isIntegralDrag = (e.isControlDown() && gs.getSpectrum().getIntegrationGraph() != null);
    setCurrentGraphSet(gs);
    gs.checkWidgetEvent(xPixel, yPixel, true);
  }

  public void mouseMoved(MouseEvent e) {
    int xPixel = e.getX();
    int yPixel = e.getY();
    JSVGraphSet gs = JSVGraphSet.findGraphSet(graphSets, xPixel, yPixel);
    if (gs == null)
      return;
    gs.mouseMovedEvent(xPixel, yPixel);
  }

  public void mouseDragged(MouseEvent e) {
    int xPixel = e.getX();
    int yPixel = e.getY();
    if (JSVGraphSet.findGraphSet(graphSets, xPixel, yPixel) != currentGraphSet)
      return;
    currentGraphSet.checkWidgetEvent(xPixel, yPixel, false);
    currentGraphSet.mouseMovedEvent(xPixel, yPixel);
  }

  public void mouseReleased(MouseEvent e) {
    if (thisWidget == null || e.getButton() != MouseEvent.BUTTON1)
      return;
    currentGraphSet.mouseReleasedEvent();
    thisWidget = null;
  }

  public void mouseClicked(MouseEvent e) {
    requestFocusInWindow();
    int xPixel = e.getX();
    int yPixel = e.getY();
    if (e.getButton() == MouseEvent.BUTTON3) {
      popup.show(this, xPixel, yPixel);
      return;
    }
    JSVGraphSet gs = JSVGraphSet.findGraphSet(graphSets, xPixel, yPixel);
    if (gs == null)
      return;
    setCurrentGraphSet(gs);
    gs.mouseClickEvent(xPixel, yPixel, e.getClickCount(), e.isControlDown());
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
      currentGraphSet.escape();
      isIntegralDrag = false;
      repaint();
      e.consume();
      return;
    }
    if (e.getModifiers() != 0) {
      if (e.isControlDown()) {
        switch (e.getKeyCode()) {
        case 45: //'-'
          currentGraphSet.scaleYBy(0.5);
          e.consume();
          break;
        case 61: //'='
          currentGraphSet.scaleYBy(2);
          e.consume();
          break;
        }
      }
      return;
    }
    switch (e.getKeyCode()) {
    case KeyEvent.VK_LEFT:
      currentGraphSet.toPeak(-1);
      e.consume();
      break;
    case KeyEvent.VK_RIGHT:
      currentGraphSet.toPeak(1);
      e.consume();
      break;
    case KeyEvent.VK_DOWN:
    case KeyEvent.VK_UP:
      int dir = (e.getKeyCode() == KeyEvent.VK_DOWN ? -1 : 1);
      if (getSpectrumAt(0).getSubSpectra() == null) {
        notifySubSpectrumChange(dir, null);
      } else {
        currentGraphSet.advanceSubSpectrum(dir);
        repaint();
      }
      e.consume();
      break;
    }
  }

  public void keyReleased(KeyEvent e) {
  }

  public void keyTyped(KeyEvent e) {
    if (e.getKeyChar() == 'z') {
      currentGraphSet.previousView();
      return;
    }
    if (e.getKeyChar() == 'y') {
      currentGraphSet.nextView();
      return;
    }
  }

  public boolean hasFileLoaded(String filePath) {
    for (int i = spectra.size(); --i >= 0;) {
       System.out.println(i + " JSVPanel hasFileloaded? look for " + filePath + " -- fouund " + spectra.get(i).getFilePathForwardSlash());
      if (spectra.get(i).getFilePathForwardSlash().equals(filePath))
        return  true;
    }
    return false;
  }

}
