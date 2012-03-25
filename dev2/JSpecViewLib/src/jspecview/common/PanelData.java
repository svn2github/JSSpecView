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

import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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

public class PanelData {

  PanelData(JSVPanel owner) {
    this.owner = owner;
    System.out.println("new panelData for " + owner);
  }
  
  
  // Critical fields

  JSVPanel owner;
  private ArrayList<PanelListener> listeners = new ArrayList<PanelListener>();
  List<GraphSet> graphSets;
  GraphSet currentGraphSet;
  private Map<String, NumberFormat> htFormats = new Hashtable<String, NumberFormat>();
  PlotWidget thisWidget;
  Coordinate coordClicked;
  Coordinate[] coordsClicked;
  Hashtable<String, Object> options = new Hashtable<String, Object>();

  public void dispose() {
    owner = null;
    for (int i = 0; i < graphSets.size(); i++)
      graphSets.get(i).dispose();
    graphSets = null;
    currentGraphSet = null;
    htFormats = null;
    coordClicked = null;
    coordsClicked = null;
    thisWidget = null;
    options = null;
    listeners = null;
  }

  // plot parameters

  protected int defaultHeight = 450;
  protected int defaultWidth = 280;
  protected int leftPlotAreaPos = 80, topPlotAreaPos = 30;
  protected int plotAreaWidth, plotAreaHeight;
  protected int left = leftPlotAreaPos, right = 50, top = topPlotAreaPos,
      bottom = 50;

  // current values

  protected boolean drawXAxisLeftToRight;
  protected boolean xAxisLeftToRight = true;
  boolean isIntegralDrag;
  protected boolean isOverlaid;

  public boolean isOverlaid() {
    return isOverlaid;
  }

  protected int nSpectra;
  int thisWidth;
  int thisPlotHeight;

  String coordStr = "";
  String startupPinTip = "Click to set.";
  protected String title;

  public void setTitle(String title) {
    this.title = title;
  }

  //////// settable parameters //////////

  protected boolean coordinatesOn = true;
  boolean display1D = false; // with 2D
  boolean display2D = true;
  protected boolean enableZoom = true;
  protected boolean gridOn = false;
  protected boolean reversePlot = false;
  protected boolean titleBoldOn = false;
  protected boolean titleOn = true;
  protected boolean xScaleOn = true;
  protected boolean xUnitsOn = true;
  protected boolean yScaleOn = true;
  protected boolean yUnitsOn = true;

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
      sets.add(graphSets.get(i).getInfo(key,
          isSelected && graphSets.get(i) == currentGraphSet));
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

  private String displayFontName;
  private String titleFontName;


  public void setFontName(ScriptToken st, String fontName) {    
    switch (st) {
    case DISPLAYFONTNAME:
      displayFontName = fontName;
      break;
    case TITLEFONTNAME:
      titleFontName = fontName;
      break;
    }
    if (fontName != null)
      options.put(st.name(), fontName);
  }

  /////////// print parameters ///////////

  boolean isPrinting;
  protected boolean printGrid = gridOn;
  protected boolean printTitle = true;
  protected boolean printScale = true;
  String printingFont;
  protected String printGraphPosition = "default";


  // listeners to handle various events

  protected void initSingleSpectrum(Graph spectrum) {
    List<Graph> spectra = new ArrayList<Graph>();
    spectra.add(spectrum);
    initJSVPanel(spectra, 0, 0);
  }

  protected void initJSVPanel(List<Graph> spectra, int startIndex, int endIndex) {
    owner.setupPlatform();
    nSpectra = spectra.size();
    graphSets = GraphSet.getGraphSets(owner, spectra, startIndex, endIndex);
    currentGraphSet = graphSets.get(0);
    setTitle(getSpectrum().getTitleLabel());
  }

  protected void setCurrentGraphSet(GraphSet gs) {
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

  protected void setDrawXAxis() {
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
  public void addHighlight(double x1, double x2, Object color) {
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
    currentGraphSet.removeAllHighlights(null);
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
   * Draws the Spectrum to the panel
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  protected void drawGraph(Object g, int height, int width) {

    boolean withGrid, withTitle, withXUnits, withYUnits, withCoords, withXScale, withYScale, withSliders;

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
    plotAreaWidth = width - (right + left);
    plotAreaHeight = height - (top + bottom);
    boolean isResized = (thisWidth != width || thisPlotHeight != plotAreaHeight);
    if (isResized) {
      // this seems unnecessary, and it prevents focus returning to the script command line
      //owner.doRequestFocusInWindow();
    }
    thisWidth = width;
    thisPlotHeight = plotAreaHeight;

    for (int i = graphSets.size(); --i >= 0;)
      graphSets.get(i)
          .drawGraph(g, withGrid, withXUnits, withYUnits, withXScale,
              withYScale, withSliders, !isIntegralDrag, height, width, left,
              right, top, bottom, isResized, enableZoom, display1D, display2D);
    if (withTitle)
      owner.drawTitle(g, height, width, getSpectrum().getPeakTitle());
    if (withCoords)
      owner.drawCoordinates(g, height, width);
  }

  NumberFormat getFormatter(String hash) {
    NumberFormat formatter = htFormats.get(hash);
    if (formatter == null)
      htFormats.put(hash, formatter = TextFormat.getDecimalFormat(hash));
    return formatter;
  }

  void setFont(Object g, int width, int mode, int size, boolean isLabel) {
    if (isLabel) {
      if (width < 400)
        size = (int) ((width * size) / 400);
    } else {
      if (width < 250)
        size = (int) ((width * size) / 250);
    }
    owner.setFont(g, (isPrinting ? printingFont : displayFontName), mode, size);
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
    for (int i = graphSets.size(); --i >= 0;)
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
    return GraphSet.getPickedCoordinates(coordsClicked, coordClicked, coord,
        actualCoord);
  }

  public String getSolutionColorHtml() {
    String color = currentGraphSet.getSolutionColor();
    return "<HTML><body bgcolor=rgb(" + color
        + ")><br />Predicted Solution Colour- RGB(" + color
        + ")<br /><br /></body></HTML>";
  }



  public void refresh() {
    thisWidth = 0;
    owner.repaint();
  }

  public void addAnnotation(List<String> tokens) {
    String title = currentGraphSet.addAnnotation(tokens, getTitle());
    if (title != null)
      setTitle(title);
  }

  public void addPeakHighlight(PeakInfo peakInfo) {
    System.out.println("PD checking pd.owner " + owner + "\n with " + owner.getSpectrum() + " for " + peakInfo);
    for (int i = 0; i < graphSets.size(); i++)
      graphSets.get(i).addPeakHighlight(peakInfo);
  }

  public PeakInfo findPeak(String fileName, String index) {
    return GraphSet.findPeak(graphSets, fileName, index);
  }

  public void selectSpectrum(String fileName, String type, String model) {
    for (int i = 0; i < graphSets.size(); i += 1)
      graphSets.get(i).selectSpectrum(fileName, type, model);
  }

  public boolean hasFileLoaded(String filePath) {
    for (int i = graphSets.size(); --i >= 0;)
      if (graphSets.get(i).hasFileLoaded(filePath))
        return true;
    return false;
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
   * @param isub
   *        -1 indicates direction if no subspectra or subspectrum index if
   *        subspectra
   * @param spec
   *        null indicates no subspectra
   */
  public void notifySubSpectrumChange(int isub, JDXSpectrum spec) {
    notifyListeners(new SubSpecChangeEvent(isub, (spec == null ? null : spec
        .getTitleLabel())));
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
  public void notifyPeakPickedListeners() {
    notifyListeners(new PeakPickEvent(owner, coordClicked, getSpectrum()
        .getAssociatedPeakInfo(coordClicked)));
  }

  void notifyListeners(Object eventObj) {
    for (int i = 0; i < listeners.size(); i++)
      if (listeners.get(i) != null)
        listeners.get(i).panelEvent(eventObj);
  }

  /*--------------the rest are all mouse and keyboard interface -----------------------*/

  protected void doMousePressed(int xPixel, int yPixel, boolean isControlDown) {
    GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
    if (gs == null)
      return;
    isIntegralDrag = (isControlDown && gs.getSpectrum().getIntegrationGraph() != null);
    if (isControlDown && !isIntegralDrag)
      return;
    setCurrentGraphSet(gs);
    gs.checkWidgetEvent(xPixel, yPixel, true);
  }

  protected void doMouseMoved(int xPixel, int yPixel) {
    GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
    if (gs == null)
      return;
    gs.mouseMovedEvent(xPixel, yPixel, display1D);
  }

  protected void doMouseDragged(int xPixel, int yPixel) {
    if (GraphSet.findGraphSet(graphSets, xPixel, yPixel) != currentGraphSet)
      return;
    currentGraphSet.checkWidgetEvent(xPixel, yPixel, false);
    currentGraphSet.mouseMovedEvent(xPixel, yPixel, display1D);
  }

  protected void doMouseReleased(boolean isButton1) {
    if (thisWidget == null || !isButton1)
      return;
    currentGraphSet.mouseReleasedEvent();
    thisWidget = null;
  }

  protected void doMouseClicked(int xPixel, int yPixel, int clickCount,
                                boolean isControlDown) {
    GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
    if (gs == null)
      return;
    setCurrentGraphSet(gs);
    gs.mouseClickEvent(xPixel, yPixel, clickCount, isControlDown);
  }
  
  public static JSVPanel checkIntegral(JSVPanel jsvp, 
                                    Parameters parameters, String value) {
    IntegralGraph graph = jsvp.getSpectrum().getIntegrationGraph();
    boolean showMessage = false;//value.equals("?");
    int mode = IntegralGraph.getMode(value);
    if (mode == IntegralGraph.INTEGRATE_MARK) {
      if (graph == null) {
        jsvp = checkIntegral(jsvp, parameters, "ON");
        graph = jsvp.getSpectrum().getIntegrationGraph();
      }
      if (graph != null)
        graph.addMarks(value.substring(5).trim());
      return jsvp;
    }
    return (mode == IntegralGraph.INTEGRATE_OFF
        || mode != IntegralGraph.INTEGRATE_ON && graph != null ? removeIntegration(jsvp)
        : integrate(jsvp, showMessage, parameters));
  }

  public static JSVPanel removeIntegration(JSVPanel jsvp) {
    JDXSpectrum spectrum = jsvp.getSpectrum();
    if (spectrum.getIntegrationGraph() == null)
      return jsvp;
    spectrum.setIntegrationGraph(null);
    return jsvp.getNewPanel(spectrum);
  }

  /**
     * Integrates an HNMR spectrum
     * 
     * @param frameOrPanel
     *        the selected frame
     * @param showDialog
     *        if true then dialog is shown, otherwise spectrum is integrated with
     *        default values
     * @param integrationRatios
     * 
     * 
     * @return the panel containing the HNMR spectrum with integral displayed
     */
    public static JSVPanel integrate(JSVPanel jsvp, boolean showDialog,
                                     Parameters parameters) {
      JDXSpectrum spectrum = jsvp.getSpectrum();
      IntegralGraph graph = spectrum.getIntegrationGraph();
      spectrum.setIntegrationGraph(null);
      if (graph != null || spectrum.canIntegrate()
          && jsvp.getPanelData().getNumberOfSpectraInCurrentSet() == 1) {
  /*      if (showDialog) {
          IntegrateDialog integDialog;
          if (graph != null) {
            integDialog = new IntegrateDialog(jp, "Integration Parameters", true,
                graph.getPercentMinimumY(), graph.getPercentOffset(), graph
                    .getIntegralFactor());
          } else {
            integDialog = new IntegrateDialog(jp, "Integration Parameters", true,
                parameters.integralMinY, parameters.integralOffset,
                parameters.integralFactor);
          }
          parameters.integralMinY = integDialog.getMinimumY();
          parameters.integralOffset = integDialog.getOffset();
          parameters.integralFactor = integDialog.getFactor();
        }
  */      graph = spectrum.integrate(parameters.integralMinY,
            parameters.integralOffset, parameters.integralFactor);
  
        if (graph != null)
          jsvp = jsvp.getIntegralPanel(spectrum);
      }
      return jsvp;
    }

  public static JSVPanel taConvert(JSVPanel jsvp, int mode) {
    if (jsvp.getPanelData().getNumberOfSpectraTotal() > 1)
      return null;
    JDXSpectrum spectrum = JDXSpectrum.taConvert(jsvp.getSpectrum(), mode);
    return (spectrum == jsvp.getSpectrum() ? null : jsvp.getNewPanel(spectrum));
  }

  public void repaint() {
    owner.repaint();
  }

  public void setToolTipText(String s) {
    owner.setToolTipText(s);
  }

  public Object getHighlightColor() {
    return owner.getColor(ScriptToken.HIGHLIGHTCOLOR);
  }

  public void setHighlightColor(Object color) {
    owner.setColor(ScriptToken.HIGHLIGHTCOLOR, color);
  }

  public String getSolutionColor() {
    return currentGraphSet.getSolutionColor();
  }

}