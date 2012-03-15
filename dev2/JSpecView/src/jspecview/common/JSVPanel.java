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
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
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

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet; //import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.source.JDXSource;
import jspecview.util.Logger;
import jspecview.util.Parser;
import jspecview.util.TextFormat;

/*
// Batik SVG Generator
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.dom.GenericDOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;
*/

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
  private static final int MIN_DRAG_X_PIXELS = 5;// fewer than this means no zoom

  private PlotWidget zoomBox1D, zoomBox2D, 
      pin1Dx0, pin1Dx1, pin1Dy0, pin1Dy1, 
      pin2Dx0, pin2Dx1, pin2Dy;
  private PlotWidget thisWidget;
  private PlotWidget[] widgets;
  
  private int index;

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public void finalize() {
    System.out.println("JSVPanel " + this + " finalized");
  }

  // The list of spectra
  private Graph[] spectra;

  // The Number of Spectra
  private int nSpectra;

  // Contains information needed to draw spectra
  private MultiScaleData multiScaleData;

  // width and height of the JSVPanel
  //private int width, height;

  //The position of the plot area
  private int leftPlotAreaPos = 80, topPlotAreaPos = 30;

  // insets of the plot area
  private Insets plotAreaInsets = new Insets(topPlotAreaPos, leftPlotAreaPos, 50, 50);

  // width and height of the plot area
  private int plotAreaWidth, plotAreaHeight;

  // Positions of the borders of the plotArea
  private int rightPlotAreaPos, bottomPlotAreaPos;

  // Enables or disables zoom
  private boolean zoomEnabled = true;

  // turns on/off elements of the JSVPanel
  private boolean gridOn = false;
  private boolean coordsOn = true;
  private boolean highlightOn = false;
  private boolean titleOn = true;
  private boolean xScaleOn = true;
  private boolean yScaleOn = true;
  private boolean xUnitsOn = true;
  private boolean yUnitsOn = true;

  // export properties
  //export svg for inkscape
  private boolean svgExportForInscapeEnabled = false;

  private Color highlightColor = new Color(255, 0, 0, 200);

  private List<Highlight> highlights = new ArrayList<Highlight>();

  // The Current Coordinate to be drawn on the Panel
  private String coordStr = "";
  private Coordinate coordClicked;
  private boolean plotReversed;

  private boolean drawXAxisLeftToRight;

  // background color of plot area
  private Color plotAreaColor = Color.white;

  //plot line color
  private Color[] plotColors;

  // integral Color
  private Color integralPlotColor = Color.red;

  //integration ratio annotations
  private ArrayList<Annotation> integrationRatios;

  private ArrayList<Annotation> annotations;

  // scale color
  private Color scaleColor = Color.black;

  // titleColor
  private Color titleColor = Color.black;

  // units Color
  private Color unitsColor = Color.red;

  // grid Color
  private Color gridColor = Color.gray;

  // coordinate Color
  private Color coordinatesColor = Color.red;

  private Color zoomBoxColor = new Color(100, 100, 50, 130);

  /* PUT FONT FACE AND SIZE ATTRIBUTES HERE */
  private String displayFontName = null;
  private String titleFontName = null;
  private boolean titleBoldOn = false;

  // The scale factors
  private double xFactorForScale, yFactorForScale;
  
  private double userYFactor = 1;

  // List of Scale data for zoom
  private List<MultiScaleData> zoomInfoList;

  // Current index of view in zoomInfoList
  private int currentZoomIndex;

  // Determines if the xAxis should be displayed increasing
  private boolean xAxisLeftToRight = true;

  // Minimum number of points that are displayable in a zoom
  private int minNumOfPointsForZoom = 3;

  private String title;

  private boolean isPrinting;
  private boolean printGrid = gridOn;
  private boolean printTitle = true;
  private boolean printScale = true;
  private String printingFont;
  private String graphPosition = "default";
  private int defaultHeight = 450;
  private int defaultWidth = 280;
  private boolean allowYScale = true;
  

  // listeners to handle coordinatesClicked
  private ArrayList<PanelListener> listeners = new ArrayList<PanelListener>();

  /**
   * Constructs a new JSVPanel
   * 
   * @param spectrum
   *        the spectrum
   * @throws ScalesIncompatibleException
   */
  public JSVPanel(Graph spectrum) {
    super();
    // standard applet not overlaid and not showing range
    // standard application split spectra
    // removal of integration, taConvert
    // Preferences Dialog sample.jdx
    initJSVPanel(new Graph[] { spectrum }, null, null);
  }

  /**
   * Constructs a <code>JSVPanel</code> with a single <code>Spectrum</code> the
   * segment specified by startindex and endindex displayed
   * 
   * @param spectrum
   *        the spectrum
   * @param startIndex
   *        the index of coordinate at which the display should start
   * @param endIndex
   *        the index of the end coordinate
   * @throws JSpecViewException
   */
  public JSVPanel(Graph spectrum, int startIndex, int endIndex)
      throws ScalesIncompatibleException {
    super();
    // from applet not overlaid but showing range
      initJSVPanel(new Graph[] { spectrum }, new int[] { startIndex },
          new int[] { endIndex });
  }

  /**
   * Constructs a JSVPanel with an array of Spectra
   * 
   * @param spectra
   *        an array of spectra (<code>Spectrum</code>)
   * @throws ScalesIncompatibleException
   */
  public JSVPanel(Graph[] spectra) {
    super();
    // specifically for getIntegralPanel
    initJSVPanel(spectra, null, null);
  }

  /**
   * Constructs a JSVMultiPanel with a List of Spectra
   * 
   * @param spectra
   *
   *        a <code>List</code> of spectra
   * @throws ScalesIncompatibleException
   */
  public JSVPanel(List<JDXSpectrum> spectra) throws ScalesIncompatibleException {
    super();
    // applet overlay, no range
    // application overlay, no range
    if (!JDXSpectrum.areScalesCompatible(spectra))
      throw new ScalesIncompatibleException();
    initJSVPanel((Graph[]) spectra.toArray(new Graph[spectra.size()]), null, null);
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
  public JSVPanel(List<JDXSpectrum> spectra, int[] startIndices,
      int[] endIndices) throws ScalesIncompatibleException {
    // from applet only;  overlay with a set of spectra, with range.
    super();
    if (!JDXSpectrum.areScalesCompatible(spectra))
      throw new ScalesIncompatibleException();
    initJSVPanel((Graph[]) spectra.toArray(new Graph[spectra.size()]), startIndices,
        endIndices);
  }

  public static JSVPanel getIntegralPanel(JDXSpectrum spectrum, Color color) {
    Graph graph = spectrum.getIntegrationGraph();
    JSVPanel jsvp = new JSVPanel(new Graph[] { spectrum, graph });
    jsvp.setTitle(graph.getTitle());
    jsvp.setPlotColors(new Color[] { jsvp.getPlotColor(0), color });
    return jsvp;
  }

  /**
   * Initializes the JSVPanel
   * 
   * @param spectra
   *        the array of spectra
   * @param startIndices
   *        the index of the start data point
   * @param endIndices
   *        the index of the end data point
   * @throws JSpecViewException
   * @throws ScalesIncompatibleException
   */
  
  private void initJSVPanel(Graph[] spectra, int[] startIndices,
                            int[] endIndices) {
    this.spectra = spectra;
    nSpectra = spectra.length;
    if (nSpectra == 1)
      setTitle(getSpectrum().getTitleLabel());
    if (startIndices == null) {
      startIndices = new int[nSpectra];
      endIndices = new int[nSpectra];
      for (int i = 0; i < nSpectra; i++) {
        startIndices[i] = 0;
        endIndices[i] = spectra[i].getXYCoords().length - 1;
      }
    }
    allowYScale = true;
    for (int i = 0; i < nSpectra; i++) {
      allowYScale &= (spectra[i].getYUnits().equals(spectra[0].getYUnits()) 
          && spectra[i].getUserYFactor() == spectra[0].getUserYFactor());        
    }
    getMultiScaleData(0, 0, 0, 0, startIndices, endIndices);
    zoomInfoList = new ArrayList<MultiScaleData>();
    zoomInfoList.add(multiScaleData);
    setPlotColors(Parameters.defaultPlotColors);
    setBorder(BorderFactory.createLineBorder(Color.lightGray));
    addKeyListener(this);
    addMouseListener(this);
    addMouseMotionListener(this);
  }

  public void setPlotColors(Color[] colors) {
    if (colors.length > nSpectra) {
      Color[] tmpPlotColors = new Color[nSpectra];
      System.arraycopy(colors, 0, tmpPlotColors, 0, nSpectra);
      colors = tmpPlotColors;
    } else if (nSpectra > colors.length) {
      Color[] tmpPlotColors = new Color[nSpectra];
      int numAdditionColors = nSpectra - colors.length;
      System.arraycopy(colors, 0, tmpPlotColors, 0, colors.length);
      for (int i = 0, j = colors.length; i < numAdditionColors; i++, j++)
        tmpPlotColors[j] = generateRandomColor();
      colors = tmpPlotColors;
    }
    plotColors = colors;
  }

  private void getMultiScaleData(double x1, double x2, double y1, double y2,
                                 int[] startIndices, int[] endIndices) {
    Graph[] graphs = (graphsTemp[0] == null ? spectra : graphsTemp);
    List<JDXSpectrum> subspecs = getSpectrumAt(0).getSubSpectra();
    if (!getSpectrumAt(0).is1D() || subspecs == null && y1 == y2) {
      // 2D spectrum or startup
      graphs = spectra;
    } else if (y1 == y2) {
      //start up, forced subsets (too many spectra) 
      multiScaleData = new MultiScaleData(subspecs, y1, y2, 10, 10, getSpectrum()
          .isContinuous());
      return;
    }
    multiScaleData = new MultiScaleData(graphs, y1, y2, startIndices,
        endIndices, 10, 10, getSpectrumAt(0).isContinuous());
    if (x1 != x2)
      multiScaleData.setXRange(x1, x2, 10);
  }

  /**
   * Returns a randomly generated <code>Color</code>
   * 
   * @return a randomly generated <code>Color</code>
   */
  private Color generateRandomColor() {
    int red = (int) (Math.random() * 255);
    int green = (int) (Math.random() * 255);
    int blue = (int) (Math.random() * 255);

    Color randomColor = new Color(red, green, blue);

    if (!randomColor.equals(Color.blue))
      return randomColor;
    return generateRandomColor();
  }

  /* ------------------------- SETTER METHODS-------------------------------*/
  /**
   * Sets the insets of the plot area
   * 
   * @param top
   *        top inset
   * @param left
   *        left inset
   * @param bottom
   *        bottom inset
   * @param right
   *        right inset
   */
  public void setPlotAreaInsets(int top, int left, int bottom, int right) {
    leftPlotAreaPos = left;
    topPlotAreaPos = top;
    plotAreaInsets = new Insets(top, left, bottom, right);
  }

  /**
   * Sets the plot area insets
   * 
   * @param insets
   *        the insets of the plot area
   */
  public void setPlotAreaInsets(Insets insets) {
    leftPlotAreaPos = insets.left;
    topPlotAreaPos = insets.top;
    plotAreaInsets = insets;
  }

  /**
   * Sets the zoom enabled or disabled
   * 
   * @param val
   *        true or false
   */
  public void setZoomEnabled(boolean val) {
    zoomEnabled = val;
  }

  /**
   * Displays grid if val is true
   * 
   * @param val
   *        true or false
   */
  public void setGridOn(boolean val) {
    gridOn = val;
  }

  /**
   * Displays Coordinates if val is true
   * 
   * @param val
   *        true or false
   */
  public void setCoordinatesOn(boolean val) {
    coordsOn = val;
  }

  /**
   * Displays x scale if val is true
   * 
   * @param val
   *        true if x scale should be displayed, false otherwise
   */
  public void setXScaleOn(boolean val) {
    xScaleOn = val;
  }

  /**
   * Displays y scale if val is true
   * 
   * @param val
   *        true if y scale should be displayed, false otherwise
   */
  public void setYScaleOn(boolean val) {
    yScaleOn = val;
  }

  /**
   * Displays x units if val is true
   * 
   * @param val
   *        true if x units should be displayed, false otherwise
   */
  public void setXUnitsOn(boolean val) {
    xUnitsOn = val;
  }

  /**
   * Displays y units if val is true
   * 
   * @param val
   *        true if y units should be displayed, false otherwise
   */
  public void setYUnitsOn(boolean val) {
    yUnitsOn = val;
  }


  /**
   * Sets the Minimum number of points that may be displayed when the spectrum
   * is zoomed
   * 
   * @param num
   *        the number of points
   */
  public void setMinNumOfPointsForZoom(int num) {
    minNumOfPointsForZoom = (num >= minNumOfPointsForZoom) ? num
        : minNumOfPointsForZoom;
  }

  /**
   * Sets the color of the title displayed
   * 
   * @param color
   *        the color
   */
  public void setTitleColor(Color color) {
    titleColor = color;
  }

  /**
   * Sets the color of the plotArea;
   * 
   * @param color
   *        the color
   */
  public void setPlotAreaColor(Color color) {
    plotAreaColor = color;
  }

  public void setBackgroundColor(Color color) {
    //setBackground(backgroundColor);
  }
  
  /**
   * Sets the color of the plot
   * 
   * @param color
   *        the color
   */
  public void setPlotColor(Color color) {
    plotColors[0] = color;
  }

  /**
   * Sets the color of the integral plot
   * 
   * @param color
   *        the color
   */
  public void setIntegralPlotColor(Color color) {
    if (color != null)
      integralPlotColor = color;
  }

  /**
   * Sets the color of the scale
   * 
   * @param color
   *        the color
   */
  public void setScaleColor(Color color) {
    scaleColor = color;
  }

  /**
   * Sets the color of the units
   * 
   * @param color
   *        the color
   */
  public void setUnitsColor(Color color) {
    unitsColor = color;
  }

  /**
   * Sets the color of the grid
   * 
   * @param color
   *        the color
   */
  public void setGridColor(Color color) {
    gridColor = color;
  }

  /**
   * Sets the title that will be displayed on the panel
   * 
   * @param title
   *        the title that will be displayed on the panel
   */
  public void setTitle(String title) {
    this.title = title;
    setName(title);
  }

  /**
   * Sets the color of the Coordinates
   * 
   * @param color
   *        the color
   */
  public void setcoordinatesColor(Color color) {
    coordinatesColor = color;
  }

  /**
   * Sets highlighting enabled or disabled
   * 
   * @param val
   *        true or false
   */
  public void setHighlightOn(boolean val) {
    highlightOn = val;
  }

  /**
   * Allows the title to be displayed or not
   * 
   * @param val
   *        true if scale should be displayed, false otherwise
   */
  public void setTitleOn(boolean val) {
    titleOn = val;
  }

  /**
   * Determines whether the title is bold or not
   * 
   * @param val
   *        true if the title should be bold
   */
  public void setTitleBoldOn(boolean val) {
    titleBoldOn = val;
  }

  /**
   * Sets the title font
   * 
   * @param titleFont
   *        the name of the title font
   */
  public void setTitleFontName(String titleFont) {
    this.titleFontName = titleFont;
  }

  /**
   * Sets the display font name
   * 
   * @param displayFontName
   *        the name of the display font
   */
  public void setDisplayFontName(String displayFontName) {
    this.displayFontName = displayFontName;
  }

  /**
   * Sets the integration ratios that will be displayed
   * 
   * @param ratios
   *        array of the integration ratios
   */
  public void setIntegrationRatios(ArrayList<Annotation> ratios) {
    integrationRatios = ratios;
  }

  ColoredAnnotation lastAnnotation;

  private double minYScale;

  private boolean display2D = true;

  private double widthRatio = 1;

//  private double widthRatioMin;
  
  public void setDisplay2D(boolean TF) {
    display2D = TF;
  }
 
  /**
   * Sets the integration ratios that will be displayed
   * 
   * @param ratios
   *        array of the integration ratios
   */
  public void addAnnotation(List<String> args) {
    if (args.size() == 0 || args.size() == 1
        && args.get(0).equalsIgnoreCase("none")) {
      annotations = null;
      lastAnnotation = null;
      return;
    }
    if (args.size() < 4 && lastAnnotation == null)
      lastAnnotation = new ColoredAnnotation(
          (multiScaleData.maxXOnScale + multiScaleData.minXOnScale) / 2,
          (multiScaleData.maxYOnScale + multiScaleData.minYOnScale) / 2, title,
          Color.BLACK, false);
    ColoredAnnotation annotation = ColoredAnnotation.getAnnotation(args,
        lastAnnotation);
    if (annotation == null)
      return;
    if (annotations == null && args.size() == 1
        && args.get(0).charAt(0) == '\"') {
      String s = annotation.getText();
      setTitle(s);
      getSpectrum().setTitle(s);
      return;
    }
    lastAnnotation = annotation;
    if (annotations == null)
      annotations = new ArrayList<Annotation>();
    for (int i = annotations.size(); --i >= 0;)
      if (((Coordinate) annotations.get(i)).equals(annotation))
        annotations.remove(i);
    if (annotation.getText().length() > 0)
      annotations.add(annotation);
  }

  /*
   * sets whether svg export should support inkscape
   * @param true if inkscape svg export is enabled
   */
  public void setSvgExportForInscapeEnabled(boolean val) {
    svgExportForInscapeEnabled = val;
  }

  /*
   * Sets whether the x Axis should be displayed increasing
   */
  public void setXAxisDisplayedIncreasing(boolean val) {
    xAxisLeftToRight = val;
    setDrawXAxis();
  }

  /**
   * Displays plot in reverse if val is true
   * 
   * @param val
   *        true or false
   */
  public void setReversePlot(boolean val) {
    plotReversed = val;
    setDrawXAxis();
  }

  private void setDrawXAxis() {
    drawXAxisLeftToRight = xAxisLeftToRight ^ plotReversed;
    getSpectrum().setExportXAxisDirection(drawXAxisLeftToRight);
  }

  /* -------------------Other methods ------------------------------------*/

  /**
   * Add information about a region of the displayed spectrum to be highlighted
   * 
   * @param x1
   *        the x value of the coordinate where the highlight should start
   * @param x2
   *        the x value of the coordinate where the highlight should end
   * @param color
   *        the color of the highlight
   */
  public void addHighlight(double x1, double x2, Color color) {
    Highlight hl = new Highlight(x1, x2, (color == null ? highlightColor
        : color));
    if (!highlights.contains(hl))
      highlights.add(hl);
  }

  /**
   * Add information about a region of the displayed spectrum to be highlighted
   * 
   * @param x1
   *        the x value of the coordinate where the highlight should start
   * @param x2
   *        the x value of the coordinate where the highlight should end
   */
  public void addHighlight(double x1, double x2) {
    Highlight hl = new Highlight(x1, x2);
    if (!highlights.contains(hl)) {
      highlights.add(hl);
    }
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
    highlights.remove(index);
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
    Highlight hl = new Highlight(x1, x2);
    int index = highlights.indexOf(hl);
    if (index != -1) {
      highlights.remove(index);
    }
  }

  /**
   * Removes all highlights from the display
   */
  public void removeAllHighlights() {
    highlights.clear();
  }

  /* ------------------------- GET METHODS ----------------------------*/

  /**
   * Returns true if plot is left to right
   * 
   * @return true if plot is left to right 
   */
  public boolean isXAxisLeftToright() {
    return xAxisLeftToRight;
  }

  /**
   * Returns true if plot is reversed
   * 
   * @return true if plot is reversed
   */
  public boolean isPlotReversed() {
    return plotReversed;
  }

  /**
   * Returns true if zoom is enabled
   * 
   * @return true if zoom is enabled
   */
  public boolean isZoomEnabled() {
    return zoomEnabled;
  }

  /**
   * Returns true if coordinates are on
   * 
   * @return true if coordinates are displayed
   */
  public boolean isCoordinatesOn() {
    return coordsOn;
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
   * Returns the minimum number of points for zoom if plot is reversed
   * 
   * @return the minimum number of points for zoom if plot is reversed
   */
  public int getMinNumOfPointsForZoom() {
    return minNumOfPointsForZoom;
  }

  /**
   * Returns the <code>Spectrum</code> at the specified index
   * 
   * @param index
   *        the index of the <code>Spectrum</code>
   * @return the <code>Spectrum</code> at the specified index
   */
  public JDXSpectrum getSpectrumAt(int index) {
    return (JDXSpectrum) spectra[index];
  }

  /**
   * Returns the Number of Spectra
   * 
   * @return the Number of Spectra
   */
  public int getNumberOfSpectra() {
    return nSpectra;
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
   * Returns the insets of the plot area
   * 
   * @return the insets of the plot area
   */
  public Insets getPlotAreaInsets() {
    return plotAreaInsets;
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
    if (index >= plotColors.length)
      return null;
    return plotColors[index];
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
   * Returns the color of the Coodinates
   * 
   * @return the color of the Coodinates
   */
  public Color getcoordinatesColor() {
    return coordinatesColor;
  }

  /**
   * Returns the color of the highlighted Region
   * 
   * @return the color of the highlighted Region
   */
  public Color getHighlightColor() {
    return highlightColor;
  }

  /**
   * Returns whether highlighting is enabled
   * 
   * @return whether highlighting is enabled
   */
  public boolean getHighlightOn() {
    return highlightOn;
  }

  /**
   * Return the start indices of the Scaledata
   * 
   * @return the start indices of the Scaledata
   */
  public int[] getStartDataPointIndices() {
    return multiScaleData.startDataPointIndices;
  }

  /**
   * Return the end indices of the Scaledata
   * 
   * @return the end indices of the Scaledata
   */
  public int[] getEndDataPointIndices() {
    return multiScaleData.endDataPointIndices;
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

  /*
   * Determines whether svg export should support inkscape
   * @return true if inkscape svg export is enabled
   */
  public boolean isSvgExportForInkscapeEnabled() {
    return svgExportForInscapeEnabled;
  }

  /*
   * Returns true is the X Axis is displayed increasing
   */
  public boolean isXAxisDisplayedIncreasing() {
    return xAxisLeftToRight;
  }

  @Override
  public void setEnabled(boolean TF) {
    super.setEnabled(TF);
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
    if (!isEnabled())
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

    JDXSpectrum spec0 = getSpectrumAt(0);
    plotAreaWidth = width - (plotAreaInsets.right + plotAreaInsets.left);
    plotAreaHeight = height - (plotAreaInsets.top + plotAreaInsets.bottom);
    boolean isResized = (thisWidth != width || thisPlotHeight != plotAreaHeight);
    if (isResized)
      isd = null;
    thisWidth = width;
    thisPlotHeight = plotAreaHeight;
    bottomPlotAreaPos = plotAreaHeight + topPlotAreaPos;
    rightPlotAreaPos = plotAreaWidth + leftPlotAreaPos;
    userYFactor = getSpectrum().getUserYFactor();
    setScaleFactors(multiScaleData);
    if (!spec0.is1D() && display2D && (isd != null || get2DImage(width))) {
      width = (int) Math.floor(widthRatio * width);
      plotAreaWidth = width - (plotAreaInsets.right + plotAreaInsets.left);
      rightPlotAreaPos = plotAreaWidth + leftPlotAreaPos;
      setScaleFactors(multiScaleData);
    }

    if (isResized)
      setWidgets();

    if (isd != null)
      draw2DImage(g);

    // fill plot area color
    g.setColor(plotAreaColor);
    g.fillRect(leftPlotAreaPos, topPlotAreaPos, plotAreaWidth, plotAreaHeight);

    // fill highlight color

    if (highlightOn) {
      if (Logger.debugging)
        System.out.println();
    }

    for (int i = 0; i < highlights.size(); i++) {
      Highlight hl = highlights.get(i);
      drawBar(g, hl.getStartX(), hl.getEndX(), highlightColor = hl.getColor(),
          true);
    }

    int subIndex = spec0.getSubIndex();
    if (subIndex >= 0 && isd != null) {
      g.setColor(plotColors[0]);
      drawUnits(g, width, spec0.nucleusX, getWidth() - 30, bottomPlotAreaPos,
          1, 1.0);
      drawUnits(g, width, spec0.nucleusY, isd.xPixel0 - 5, topPlotAreaPos, 1, 0);
    }
    ArrayList<PeakInfo> list = (nSpectra == 1
        || getSpectrum().getIntegrationGraph() != null ? getSpectrum()
        .getPeakList() : null);
    if (list != null && list.size() > 0) {
      for (int i = list.size(); --i >= 0;) {
        PeakInfo pi = list.get(i);
        double xMin = pi.getXMin();
        double xMax = pi.getXMax();
        if (xMin != xMax) {
          drawBar(g, xMin, xMax, highlightColor, false);
        }
      }
    }

    boolean grid, title, xunits, yunits, coords, xscale, yscale;

    if (isPrinting) {
      grid = printGrid;
      xscale = printScale;
      yscale = printScale;
      title = printTitle;
      xunits = printScale;
      yunits = printScale;
      coords = false;
    } else {
      grid = gridOn;
      title = titleOn;
      xunits = xUnitsOn;
      yunits = yUnitsOn;
      coords = coordsOn;
      xscale = xScaleOn;
      yscale = yScaleOn;
    }

    boolean xAxis = true;//!spectra[0].getXUnits().equalsIgnoreCase("Arbitrary Units");
    boolean yAxis = true;//!spectra[0].getYUnits().equalsIgnoreCase("Arbitrary Units");

    drawZoomBoxes(g);
    if (grid)
      drawGrid(g, height, width);
    for (int i = nSpectra; --i >= 0;)
      drawPlot(g, i, height, width);
    if (xscale && xAxis)
      drawXScale(g, height, width);
    if (yscale && yAxis && allowYScale)
      drawYScale(g, height, width);
    if (title)
      drawTitle(g, height, width);
    if (xunits && xAxis)
      drawXUnits(g, width);
    if (yunits && yAxis)
      drawYUnits(g, width);
    if (coords)
      drawCoordinates(g, height, width);
    if (integrationRatios != null)
      drawAnnotations(g, height, width, integrationRatios, integralPlotColor);
    if (annotations != null)
      drawAnnotations(g, height, width, annotations, null);
    drawIntegralValue(g, width);
    drawPins(g, subIndex);
  }

  private void setWidgets() {
    if (zoomBox1D == null) {
      zoomBox1D = new PlotWidget(false);
      pin1Dx0 = new PlotWidget(true);
      pin1Dx1 = new PlotWidget(true);
      pin1Dy0 = new PlotWidget(true);
      pin1Dy1 = new PlotWidget(true);
      reset1DPins();
      if (isd != null) {
        zoomBox2D = new PlotWidget(false);
        pin2Dx0 = new PlotWidget(true);
        pin2Dx1 = new PlotWidget(true);
        pin2Dy = new PlotWidget(true);
        pin2Dx0.setX(isd.toX(isd.xPixel0), isd.xPixel0);
        pin2Dx1.setX(isd.toX(isd.xPixel1), isd.xPixel1);
      }
      widgets = new PlotWidget[] { zoomBox1D, zoomBox2D, pin1Dx0, pin1Dx1,
          pin1Dy0, pin1Dy1, pin2Dx0, pin2Dx1, pin2Dy };
    } else {
      pin1Dx0.setX(pin1Dx0.x, toPixelX0(pin1Dx0.x));
      pin1Dx1.setX(pin1Dx1.x, toPixelX0(pin1Dx1.x));
      pin1Dy0.setY(pin1Dy0.y, toPixelY0(pin1Dy0.y));
      pin1Dy1.setY(pin1Dy1.y, toPixelY0(pin1Dy1.y));
      if (isd != null) {
        pin2Dx0.setX(pin1Dx0.x, isd.toPixelX(pin2Dx0.x));
        pin2Dx1.setX(pin2Dx1.x, isd.toPixelX(pin2Dx1.x));
      }
    }
    pin1Dx0.yPixel0 = pin1Dx1.yPixel0 = topPlotAreaPos - 5;
    pin1Dx0.yPixel1 = pin1Dx1.yPixel1 = topPlotAreaPos;
    pin1Dy0.xPixel0 = pin1Dy1.xPixel0 = rightPlotAreaPos + 5;
    pin1Dy0.xPixel1 = pin1Dy1.xPixel1 = rightPlotAreaPos;
    if (isd != null) {
      pin2Dx0.yPixel0 = pin2Dx1.yPixel0 = bottomPlotAreaPos + 15;
      pin2Dx0.yPixel1 = pin2Dx1.yPixel1 = topPlotAreaPos - 5;
      pin2Dx0.yPixel0 = pin2Dx1.yPixel0 = bottomPlotAreaPos + 15;
      pin2Dx1.yPixel1 = pin2Dx1.yPixel1 = topPlotAreaPos - 5;
      pin2Dy.xPixel0 = (rightPlotAreaPos + isd.xPixel0) / 2;
      pin2Dy.xPixel1 = getWidth() - 10;
    }
  }

  private void reset1DPins() {
    pin1Dx0.setX(multiScaleData.minX, toPixelX0(multiScaleData.minX));
    pin1Dx1.setX(multiScaleData.maxX, toPixelX0(multiScaleData.maxX));
    pin1Dy0.setY(multiScaleData.minY, toPixelY0(multiScaleData.minY));
    pin1Dy1.setY(multiScaleData.maxY, toPixelY0(multiScaleData.maxY));
  }

  private void drawZoomBoxes(Graphics g) {
    if (zoomEnabled) {
      drawWidget(g, zoomBox1D);
      drawWidget(g, zoomBox2D);
    }
  }

  private void drawPins(Graphics g, int subIndex) {
    // topbar
    g.setColor(gridColor);
    fillBox(g, leftPlotAreaPos, pin1Dx0.yPixel1, rightPlotAreaPos, pin1Dx1.yPixel1 + 2);    
    fillBox(g, pin1Dy0.xPixel1, bottomPlotAreaPos, pin1Dy1.xPixel1 + 2, topPlotAreaPos);    
    g.setColor(plotColors[0]);
    fillBox(g, pin1Dx0.xPixel0, pin1Dx0.yPixel1, pin1Dx1.xPixel0, pin1Dx1.yPixel1 + 2);
    fillBox(g, pin1Dy0.xPixel1, pin1Dy0.yPixel1, pin1Dy1.xPixel1 + 2, pin1Dy1.yPixel0);

    if (subIndex >= 0 && isd != null) {
      pin2Dy.yPixel0 = pin2Dy.yPixel1 = isd.toPixelY(subIndex);
      drawWidget(g, pin2Dx0);
      drawWidget(g, pin2Dx1);
      drawWidget(g, pin2Dy);
    }
    drawWidget(g, pin1Dx0);
    drawWidget(g, pin1Dx1);
    drawWidget(g, pin1Dy0);
    drawWidget(g, pin1Dy1);
  }

  private boolean isInTopBar(int xPixel, int yPixel) {
    return (xPixel == fixX(xPixel) && yPixel > pin1Dx0.yPixel0 && yPixel < pin1Dx0.yPixel1);
  }

  private boolean isInRightBar(int xPixel, int yPixel) {
    return (yPixel == fixY(yPixel) && xPixel > pin1Dy0.xPixel1 && xPixel < pin1Dy0.xPixel0);
  }

  private void drawWidget(Graphics g, PlotWidget pw) {
    if (pw == null)
      return;
    if (pw.isPin) {
      g.setColor(plotColors[0]);
      g.drawLine(pw.xPixel0, pw.yPixel0, pw.xPixel1, pw.yPixel1);
      drawHandle(g, pw.xPixel0, pw.yPixel0);
    } else if (pw.xPixel1 != pw.xPixel0) {
      g.setColor(zoomBoxColor);
      fillBox(g, pw.xPixel0, pw.yPixel0, pw.xPixel1, pw.yPixel1);
    }
  }

  private void fillBox(Graphics g, int x0, int y0, int x1, int y1) {
    g.fillRect(Math.min(x0, x1), Math.min(y0, y1), 
        Math.abs(x0 - x1), Math.abs(y0 - y1));
  }

  private void drawHandle(Graphics g, int x, int y) {
    g.fillRect(x - 2, y - 2, 5, 5);
  }

  /**
   * draw a bar, but not necessarily full height
   * 
   * @param g
   * @param startX
   *        units
   * @param endX
   *        units
   * @param color
   * @param isFullHeight
   */

  private void drawBar(Graphics g, double startX, double endX, Color color,
                       boolean isFullHeight) {
    int x1 = toPixelX(startX);
    int x2 = toPixelX(endX);
    if (x1 > x2) {
      int tmp = x1;
      x1 = x2;
      x2 = tmp;
    }
    // if either pixel is outside of plot area
    x1 = fixX(x1);
    x2 = fixX(x2);
    if (x1 == x2)
      return;
    g.setColor(color);
    fillBox(g, x1, topPlotAreaPos, x2, topPlotAreaPos
        + (isFullHeight ? plotAreaHeight : 5));
  }

  /**
   * Draws the plot on the Panel
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param index
   *        the index of the Spectrum to draw
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  private void drawPlot(Graphics g, int index, int height, int width) {
    // Check if specInfo in null or xyCoords is null
    //Coordinate[] xyCoords = spectra[index].getXYCoords();
    Coordinate[] xyCoords = spectra[index].getXYCoords();

    // Draw a border
    if (!gridOn) {
      g.setColor(gridColor);
      g.drawRect(leftPlotAreaPos, topPlotAreaPos, plotAreaWidth, plotAreaHeight);
    }

    Color color = plotColors[index];
    if (index == 1 && getSpectrum().getIntegrationGraph() != null)
      color = integralPlotColor;
    g.setColor(color);

    // Check if revPLot on

    userYFactor = spectra[index].getUserYFactor();
    if (spectra[index].isContinuous()) {
      for (int i = multiScaleData.startDataPointIndices[index]; i < multiScaleData.endDataPointIndices[index]; i++) {
        Coordinate point1 = xyCoords[i];
        Coordinate point2 = xyCoords[i + 1];
        int x1 = toPixelX(point1.getXVal());
        int y1 = toPixelY(point1.getYVal());
        int x2 = toPixelX(point2.getXVal());
        int y2 = toPixelY(point2.getYVal());
        if (y1 == Integer.MIN_VALUE || y2 == Integer.MIN_VALUE)
          continue;
        y1 = fixY(y1);
        y2 = fixY(y2);
        if (y1 == y2 && (y1 == topPlotAreaPos || y1 == bottomPlotAreaPos))
          continue;
        g.drawLine(x1, y1, x2, y2);
      }
    } else {
      for (int i = multiScaleData.startDataPointIndices[index]; i <= multiScaleData.endDataPointIndices[index]; i++) {
        Coordinate point = xyCoords[i];
        int x1 = toPixelX(point.getXVal());
        int y1 = toPixelY(Math.max(multiScaleData.minYOnScale, 0));
        int y2 = toPixelY(point.getYVal());
        if (y2 == Integer.MIN_VALUE)
          continue;
        y1 = fixY(y1);
        y2 = fixY(y2);
        if (y1 == y2 && (y1 == topPlotAreaPos || y1 == bottomPlotAreaPos))
          continue;
        g.drawLine(x1, y1, x1, y2);
      }
      if (multiScaleData.isYZeroOnScale()) {
        int y = toPixelY(0);
        g.drawLine(rightPlotAreaPos, y, leftPlotAreaPos, y);
      }
    }
  } 
  
  /**
   * Draws the grid on the Panel
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  private void drawGrid(Graphics g, int height, int width) {
    g.setColor(gridColor);

    double lastX;
    if (Double.isNaN(multiScaleData.firstX)) {
      lastX = multiScaleData.maxXOnScale + multiScaleData.xStep / 2;
      for (double val = multiScaleData.minXOnScale; val < lastX; val += multiScaleData.xStep) {
        int x = toPixelX(val);
        g.drawLine(x, topPlotAreaPos, x, bottomPlotAreaPos);
      }
    } else {
      lastX = multiScaleData.maxXOnScale * 1.0001;
      for (double val = multiScaleData.firstX; val <= lastX; val += multiScaleData.xStep) {
        int x = toPixelX(val);
        g.drawLine(x, topPlotAreaPos, x, bottomPlotAreaPos);
      }
    }
    for (double val = multiScaleData.minYOnScale; val < multiScaleData.maxYOnScale
        + multiScaleData.yStep / 2; val += multiScaleData.yStep) {
      int y = toPixelY(val);
      if (y == fixY(y))
        g.drawLine(leftPlotAreaPos, y, rightPlotAreaPos, y);
    }
  }

  private void drawIntegralValue(Graphics g, int width) {
    List<Integral> integrals = getSpectrum().getIntegrals();
    if (integrals == null)
      return;
    setFont(g, width, Font.BOLD, 12, false);
    FontMetrics fm = g.getFontMetrics();
    NumberFormat formatter = getFormatter("#0.0");
    g.setColor(integralPlotColor);

    for (int i = integrals.size(); --i >= 0;) {
      Integral in = integrals.get(i);
      if (in.value == 0)
        continue;
      String s = "  " + formatter.format(Math.abs(in.value));
      int x = toPixelX(in.x2);
      int y1 = toPixelY(in.y1);
      int y2 = toPixelY(in.y2);
      g.drawLine(x, y1, x, y2);
      g.drawLine(x + 1, y1, x + 1, y2);
      g.drawString(s, x, (y1 + y2) / 2  + fm.getHeight() / 3);
    }
  }

  private Map<String, NumberFormat> htFormats = new Hashtable<String, NumberFormat>();

  private Graph[] graphsTemp = new Graph[1];

  private NumberFormat getFormatter(String hash) {
    NumberFormat formatter = htFormats.get(hash);
    if (formatter == null)
      htFormats.put(hash, formatter = TextFormat.getDecimalFormat(hash));
    return formatter;
  }

  /**
   * Draws the x Scale
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  private void drawXScale(Graphics g, int height, int width) {

    String hashX = "#";
    String hash1 = "0.00000000";

    if (multiScaleData.hashNums[0] <= 0)
      hashX = hash1.substring(0, Math.abs(multiScaleData.hashNums[0]) + 3);

    NumberFormat formatter = getFormatter(hashX);
    setFont(g, width, Font.PLAIN, 12, false);
    FontMetrics fm = g.getFontMetrics();    
    int y1 = bottomPlotAreaPos;
    int y2 = bottomPlotAreaPos + 3;
    double maxWidth = Math.abs((toPixelX(multiScaleData.xStep) - toPixelX(0)) * 0.95);
    double lastX;
    if (Double.isNaN(multiScaleData.firstX)) {
      lastX = multiScaleData.maxXOnScale + multiScaleData.xStep / 2;
      for (double val = multiScaleData.minXOnScale, vald = multiScaleData.maxXOnScale; val < lastX; val += multiScaleData.xStep, vald -= multiScaleData.xStep) {
        int x = (int) (leftPlotAreaPos + (((drawXAxisLeftToRight ? val : vald) - multiScaleData.minXOnScale) / xFactorForScale));
        g.setColor(gridColor);
        g.drawLine(x, y1, x, y2);
        g.setColor(scaleColor);
        String s = formatter.format(val);
        int w = fm.stringWidth(s);
        g.drawString(s, x - w / 2, y2 + fm.getHeight());
      }
    } else {
      lastX = multiScaleData.maxXOnScale * 1.0001;
      for (double val = multiScaleData.firstX; val <= lastX; val += multiScaleData.xStep) {
        int x = toPixelX(val);
        g.setColor(gridColor);
        g.drawLine(x, y1, x, y2);
        g.setColor(scaleColor);
        String s = formatter.format(val);
        int w = fm.stringWidth(s);
        g.drawString(s, x - w / 2, y2 + fm.getHeight());
        val += Math.floor(w/maxWidth) * multiScaleData.xStep;
      }
    }
  }

  private void setFont(Graphics g, int width, int mode, int size, boolean isLabel) {
    g.setFont(new Font((isPrinting ? printingFont : displayFontName),
        mode, calculateFontSize(width, size, true)));
  }

  /**
   * Draws the y Scale
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  private void drawYScale(Graphics g, int height, int width) {

    //String hashX = "#";
    String hashY = "#";
    String hash1 = "0.00000000";
    if (multiScaleData.hashNums[1] <= 0)
      hashY = hash1.substring(0, Math.abs(multiScaleData.hashNums[1]) + 3);
    NumberFormat formatter = getFormatter(hashY);
    setFont(g, width, Font.PLAIN, 12, false); 
    FontMetrics fm = g.getFontMetrics();
    double max = multiScaleData.maxYOnScale + multiScaleData.yStep / 2;
    for (double val = multiScaleData.minYOnScale; val < max; val += multiScaleData.yStep) {
      int x1 = (int) leftPlotAreaPos;
      int y = toPixelY(val * userYFactor);
      g.setColor(gridColor);
      g.drawLine(x1, y, x1 - 3, y);
      g.setColor(scaleColor);
      String s = formatter.format(val);
      g.drawString(s, (x1 - 4 - fm.stringWidth(s)), y + fm.getHeight() / 3);
    }
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
  private void drawTitle(Graphics g, int height, int width) {
    setFont(g, width, isPrinting || titleBoldOn ? Font.BOLD : Font.PLAIN, 
        14, true); 
    FontMetrics fm = g.getFontMetrics();
    g.setColor(titleColor);
    g.drawString(getSpectrum().getPeakTitle(), 5, (int) (height - fm
        .getHeight() / 2));
  }

  /**
   * Draws the X Units
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param imageHeight
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  private void drawXUnits(Graphics g, int width) {
    drawUnits(g, width, spectra[0].getXUnits(), rightPlotAreaPos, bottomPlotAreaPos, 1, 2.5);
  }

  private void drawUnits(Graphics g, int width, String s,
                         int x, int y, double hOff, double vOff) {
    g.setColor(unitsColor);
    setFont(g, width, Font.ITALIC, 10, false);
    FontMetrics fm = g.getFontMetrics();
    g.drawString(s, (int) (x - fm.stringWidth(s) * hOff),
        (int) (y + fm.getHeight() * vOff));
  }

  /**
   * Draws the Y Units
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param imageHeight
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  private void drawYUnits(Graphics g, int width) {
    drawUnits(g, width, spectra[0].getYUnits(), 5, topPlotAreaPos, 0, -1);
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

  // determine whether there are any ratio annotations to draw
  private void drawAnnotations(Graphics g, int height, int width,
                               ArrayList<Annotation> annotations, Color color) {
    setFont(g, width, Font.BOLD, 12, false);
    for (int i = annotations.size(); --i >= 0;) {
      Annotation note = annotations.get(i);
      color = (note instanceof ColoredAnnotation ? ((ColoredAnnotation) note).getColor()
          : color);
      if (color == null)
        color = Color.BLACK;
      g.setColor(color);
      int x = toPixelX(note.getXVal());
      int y = (note.isPixels() ? (int) (topPlotAreaPos + 10 - note.getYVal()) 
          : toPixelY(note.getYVal()));
      g.drawString(note.getText(), x, y);
    }
  }

  /**
   * Calculates the size of the font to display based on the window size
   * 
   * @param length
   *        ??
   * @param initSize
   *        the intial size of the font
   * @param isLabel
   *        true if the text lies along the width else false
   * @return the size of the font
   */
  private int calculateFontSize(double length, int initSize, boolean isLabel) {
    int size = initSize;
    // TODO THIS METHOD NEEDS REWORKING!!!
    if (isLabel) {
      if (length < 400)
        size = (int) ((length * initSize) / 400);
    } else {
      if (length < 250)
        size = (int) ((length * initSize) / 250);
    }
    return size;
  }

  /*-------------------- METHODS FOR SCALING AND ZOOM --------------------------*/

  public void setZoom(double x1, double y1, double x2, double y2) {
    setZoomTo(0);
    if (Double.isNaN(x1)) {
      // yzoom only
      x1 = multiScaleData.minX;
      x2 = multiScaleData.maxX;
      isd = null;
    }
    if (x1 != 0 || x2 != 0) {
      doZoom(x1, y1, x2, y2, false, true, false);
      return;
    }
    isd = null;
    thisWidth = 0;
    notifyZoomListeners(0, 0, 0, 0);
  }

  private void setScaleFactors(MultiScaleData multiScaleData) {
    xFactorForScale = (multiScaleData.maxXOnScale - multiScaleData.minXOnScale)
        / plotAreaWidth;
    yFactorForScale = (multiScaleData.maxYOnScale - multiScaleData.minYOnScale)
        / plotAreaHeight;
    minYScale = multiScaleData.minYOnScale;
  }

  private int fixX(int xPixel) {
    return Math.max(Math.min(xPixel, rightPlotAreaPos), leftPlotAreaPos);
  }

  private int toPixelX(double dx) {
    int x = (int) ((dx - multiScaleData.minXOnScale) / xFactorForScale);
    return (int) (drawXAxisLeftToRight ? leftPlotAreaPos + x
        : rightPlotAreaPos - x);
  }

  private int toPixelX0(double x) {
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxXOnScale - multiScaleData.minXOnScale) / plotAreaWidth;
    return (int) (drawXAxisLeftToRight ? rightPlotAreaPos - (multiScaleData.maxXOnScale - x) / factor
        : rightPlotAreaPos - (x - multiScaleData.minXOnScale) / factor);
  }
  
  private double toX(int xPixel) {
    if (isd != null && isd.isXWithinRange(xPixel))
      return isd.toX(xPixel);
    xPixel = fixX(xPixel);
    return (drawXAxisLeftToRight 
         ?  multiScaleData.maxXOnScale - (rightPlotAreaPos - xPixel) * xFactorForScale 
         :  multiScaleData.minXOnScale + (rightPlotAreaPos - xPixel) * xFactorForScale);
  }

  private double toX0(int xPixel) {
    xPixel = fixX(xPixel);
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxXOnScale - multiScaleData.minXOnScale) / plotAreaWidth;
    return (drawXAxisLeftToRight ? multiScaleData.maxXOnScale
        - (rightPlotAreaPos - xPixel) * factor : multiScaleData.minXOnScale
        + (rightPlotAreaPos - xPixel) * factor);
  }

  private int fixY(int yPixel) {
    return Math.max(Math.min(yPixel, bottomPlotAreaPos), topPlotAreaPos);
  }

  private int toPixelY(double yVal) {
    return (Double.isNaN(yVal) ? Integer.MIN_VALUE 
        : bottomPlotAreaPos - (int) ((yVal * userYFactor - minYScale) / yFactorForScale));
  }

  private int toPixelY0(double y) {
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxYOnScale - multiScaleData.minYOnScale) / plotAreaHeight;
    return (int) (topPlotAreaPos + (multiScaleData.maxYOnScale - y) / factor);
  }
  
  private double toY(int yPixel) {
    return multiScaleData.maxYOnScale + (topPlotAreaPos - yPixel) * yFactorForScale;
  }
  
  private double toY0(int yPixel) {
    yPixel = fixY(yPixel);
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxYOnScale - multiScaleData.minYOnScale) / plotAreaHeight;
    double y = multiScaleData.maxYOnScale + (topPlotAreaPos - yPixel) * factor;
    return Math.max(multiScaleData.minY, Math.min(y, multiScaleData.maxY));
  }

  /**
   * Zooms the spectrum between two coordinates
   * 
   * @param initX
   *        TODO
   * @param initY
   *        TODO
   * @param finalX
   *        TODO
   * @param finalY
   *        TODO
   * @param initX
   *        the X start coordinate of the zoom area
   * @param initY
   *        the Y start coordinate of the zoom area
   * @param finalX
   *        the X end coordinate of the zoom area
   * @param finalY
   *        the Y end coordinate of the zoom area
   */
  private void doZoom(double initX, double initY, double finalX, double finalY,
                      boolean doRepaint, boolean addZoom, boolean checkRange) {
    if (!zoomEnabled)
      return;

    // swap points if init value > final value
    if (initX > finalX) {
      double tempX = initX;
      initX = finalX;
      finalX = tempX;
    }

    if (initY > finalY) {
      double tempY = initY;
      initY = finalY;
      finalY = tempY;
    }

    // determine if the range of the area selected for zooming is within the plot
    // Area and if not ensure that it is

    if (checkRange) {
      if (!ScaleData.isWithinRange(initX, multiScaleData)
          && !ScaleData.isWithinRange(finalX, multiScaleData))
        return;
      if (!ScaleData.isWithinRange(initX, multiScaleData)) {
        initX = multiScaleData.minX;
      } else if (!ScaleData.isWithinRange(finalX, multiScaleData)) {
        finalX = multiScaleData.maxX;
      }
    } else {
      multiScaleData = zoomInfoList.get(0);
    }
    int[] startIndices = new int[nSpectra];
    int[] endIndices = new int[nSpectra];
    if (!doZoomWithoutRepaint(initX, finalX, initY, finalY, startIndices,
        endIndices, addZoom))
      return;
    notifyZoomListeners(initX, finalX, initY, finalY);
    if (doRepaint)
      repaint();
  }

  /**
   * Zooms the spectrum but does not repaint so that it is not visible
   * 
   * @param xPt1
   *        TODO
   * @param xPt2
   *        TODO
   * @param startIndices
   *        the start indices
   * @param endIndices
   *        the end indices
   * 
   * @return true if successful
   * @throws JSpecViewException
   */
  private boolean doZoomWithoutRepaint(double xPt1, double xPt2,
                                       double yPt1, double yPt2,
                                       int[] startIndices, int[] endIndices, 
                                       boolean addZoom) {
    if (!zoomEnabled)
      return false;
    if (getSpectrumAt(0).is1D() && getSpectrumAt(0).getSubSpectra() != null) {
      graphsTemp[0] = getSpectrum();
      if (!multiScaleData.setDataPointIndices(graphsTemp, xPt1, xPt2,
          minNumOfPointsForZoom, startIndices, endIndices, false))
        return false;
    } else {
      if (!multiScaleData.setDataPointIndices(spectra, xPt1, xPt2,
          minNumOfPointsForZoom, startIndices, endIndices, true))
        return false;
    }
    getMultiScaleData(xPt1, xPt2, yPt1, yPt2, startIndices, endIndices);
    pin1Dx0.setX(xPt1, toPixelX0(xPt1));
    pin1Dx1.setX(xPt2, toPixelX0(xPt2));
    pin1Dy0.setY(yPt1, toPixelY0(yPt1));
    pin1Dy1.setY(yPt2, toPixelY0(yPt2));
    if (isd != null) {
      int isub = getSpectrumAt(0).getSubIndex();
      int ifix = isd.fixSubIndex(isub);
      if (ifix != isub)
        setCurrentSubSpectrum(ifix);
    }
    if (addZoom)
      addCurrentZoom();
    return true;
  }

  private void addCurrentZoom() {
    // add to and clean the zoom list
    if (zoomInfoList.size() > currentZoomIndex + 1)
      for (int i = zoomInfoList.size() - 1; i > currentZoomIndex; i--)
        zoomInfoList.remove(i);
    zoomInfoList.add(multiScaleData);
    currentZoomIndex++;
  }

  /**
   * Resets the spectrum to it's original view
   */
  public void reset() {
    setZoomTo(0);
  }

  private void setZoomTo(int i) {
    isd = null;
    currentZoomIndex = i;
    multiScaleData = zoomInfoList.get(i);
    pin1Dx0.setX(multiScaleData.minXOnScale, toPixelX0(multiScaleData.minXOnScale));
    pin1Dx1.setX(multiScaleData.maxXOnScale, toPixelX0(multiScaleData.maxXOnScale));
    pin1Dy0.setY(multiScaleData.minY, toPixelY0(multiScaleData.minY));
    pin1Dy1.setY(multiScaleData.maxY, toPixelY0(multiScaleData.maxY));
    thisWidth = 0;    
    repaint();
  }

  /**
   * Clears all views in the zoom list
   */
  public void clearViews() {
    reset();
    // leave first zoom
    for (int i = zoomInfoList.size(); --i >= 1; ) 
      zoomInfoList.remove(i);
  }

  /**
   * Displays the previous view zoomed
   */
  public void previousView() {
    if (currentZoomIndex > 0)
      setZoomTo(currentZoomIndex - 1);
  }

  /**
   * Displays the next view zoomed
   */
  public void nextView() {
    if (currentZoomIndex + 1 < zoomInfoList.size())
      setZoomTo(currentZoomIndex + 1);
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

      if (graphPosition.equals("default")) {
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        if (pf.getOrientation() == PageFormat.PORTRAIT) {
          height = defaultHeight;
          width = defaultWidth;
        } else {
          height = defaultWidth;
          width = defaultHeight;
        }
      } else if (graphPosition.equals("fit to page")) {
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
    graphPosition = pl.position;

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

  /*--------------------------------------------------------------------------*/

  /**
   * Private class to represent a Highlighed region of the spectrum display
   * <p>
   * Title: JSpecView
   * </p>
   * <p>
   * Description: JSpecView is a graphical viewer for chemical spectra specified
   * in the JCAMP-DX format
   * </p>
   * <p>
   * Copyright: Copyright (c) 2002
   * </p>
   * <p>
   * Company: Dept. of Chemistry, University of the West Indies, Mona Campus,
   * Jamaica
   * </p>
   * 
   * @author Debbie-Ann Facey
   * @author Khari A. Bryan
   * @author Prof Robert.J. Lancashire
   * @version 1.0.017032006
   */
  private class Highlight {
    private double x1;
    private double x2;
    private Color color = new Color(255, 255, 0, 100);

    /**
     * Constructor
     * 
     * @param x1
     *        starting x coordinate
     * @param x2
     *        ending x coordinate
     */
    public Highlight(double x1, double x2) {
      this.x1 = x1;
      this.x2 = x2;
    }

    /**
     * Constructor
     * 
     * @param x1
     *        starting x coordinate
     * @param x2
     *        ending x coordinate
     * @param color
     *        the color of the highlighted region
     */
    public Highlight(double x1, double x2, Color color) {
      this(x1, x2);
      this.color = color;
    }

    /**
     * Returns the x coordinate where the highlighted region starts
     * 
     * @return the x coordinate where the highlighted region starts
     */
    public double getStartX() {
      return x1;
    }

    /**
     * Returns the x coordinate where the highlighted region ends
     * 
     * @return the x coordinate where the highlighted region ends
     */
    public double getEndX() {
      return x2;
    }

    /**
     * Returns the color of the highlighted region
     * 
     * @return the color of the highlighted region
     */
    public Color getColor() {
      return color;
    }

    /**
     * Overides the equals method in class <code>Object</code>
     * 
     * @param obj
     *        the object that this <code>Highlight<code> is compared to
     * @return true if equal
     */
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Highlight))
        return false;
      Highlight hl = (Highlight) obj;

      return ((hl.x1 == this.x1) && (hl.x2 == this.x2));
    }
  }

  public void destroy() {
    removeKeyListener(this);
    removeMouseListener(this);
    removeMouseMotionListener(this);
  }

  //  private static DecimalFormat coordFormatter = JSpecViewUtils.getDecimalFormat("0.000000");

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
    coord.setXVal(coordClicked.getXVal());
    coord.setYVal(coordClicked.getYVal());
    if (actualCoord == null)
      return true;
    int store = 0;
    double xPt = coord.getXVal();
    JDXSpectrum spectrum = getSpectrum();
    Coordinate[] coords = spectrum.getXYCoords();
    for (int i = 0; i < coords.length; i++) {
      if (coords[i].getXVal() > xPt) {
        store = i;
        // I guess I don't see why only the starting point is important here
        break;
      }
    }

    double actualXPt = spectrum.getXYCoords()[store].getXVal();
    double actualYPt = spectrum.getXYCoords()[store].getYVal();

    actualCoord.setXVal(actualXPt);//Double.parseDouble(coordFormatter.format(actualXPt)));
    actualCoord.setYVal(actualYPt);//Double.parseDouble(coordFormatter.format(actualYPt)));
    return true;
  }

  public void processPeakSelect(String peak) {
    if (getSpectrum().getPeakList() != null)
      removeAllHighlights();
    if (peak == null)
      return;
    String xMin = Parser.getQuotedAttribute(peak, "xMin");
    String xMax = Parser.getQuotedAttribute(peak, "xMax");
    if (xMin == null || xMax == null)
      return;
    float x1 = Parser.parseFloat(xMin);
    float x2 = Parser.parseFloat(xMax);
    if (Float.isNaN(x1) || Float.isNaN(x2))
      return;
    setHighlightOn(true);
    addHighlight(x1, x2, null);
    if (ScaleData.isWithinRange(x1, multiScaleData)
        && ScaleData.isWithinRange(x2, multiScaleData))
      repaint();
    else
      reset();
  }

  private double[] zoom = new double[4];
  /**
   * Notifies CoordinatePickedListeners
   * 
   * @param coord
   */
  public void notifyZoomListeners(double x1, double y1, double x2, double y2) {
    zoom[0] = x1;
    zoom[1] = y1;
    zoom[2] = x2;
    zoom[3] = y2;
    notifyListeners(zoom);
  }

  /**
   * Adds a CoordinatePickedListener
   * 
   * @param listener
   */
  public void addListener(PanelListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /**
   * Notifies CoordinatePickedListeners
   * 
   * @param coord
   */
  public void notifyPeakPickedListeners(Coordinate coord) {
    // TODO: Currently Aassumes spectra are not overlaid
    notifyListeners(new PeakPickedEvent(this, coord, getSpectrum()
        .getAssociatedPeakInfo(coord)));
  }

  private void notifyListeners(Object eventObj) {
    for (int i = 0; i < listeners.size(); i++) {
      PanelListener listener = listeners.get(i);
      if (listener != null) {
        listener.panelEvent(eventObj);
      }
    }
  }

  private JSVPanelPopupMenu popup;
  private JDXSource source;

  public void setPopup(JSVPanelPopupMenu appletPopupMenu) {
    this.popup = appletPopupMenu;
  }

  public void setSource(JDXSource source) {
    this.source = source;
  }

  /*--------------METHODS IN INTERFACE MouseListener-----------------------*/

  private boolean isIntegralDrag;

  /**
   * Implements mousePressed in interface MouseListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mousePressed(MouseEvent e) {
    if (e.getButton() != MouseEvent.BUTTON1)
      return;
    isIntegralDrag = (e.isControlDown() && getSpectrum().getIntegrationGraph() != null);
    checkWidgetEvent(e.getX(), e.getY(), true);
  }
  
  private boolean checkWidgetEvent(int xPixel, int yPixel, boolean isPress) {
    if (isPress) {
      thisWidget = null;
      if (!checkPinSelected(xPixel, yPixel)) {
        yPixel = fixY(yPixel);
        if (xPixel < rightPlotAreaPos) {
          xPixel = fixX(xPixel);
          zoomBox1D.setX(toX(xPixel), xPixel);
          zoomBox1D.yPixel0 = (isIntegralDrag ? topPlotAreaPos : yPixel);
          thisWidget = zoomBox1D;
        } else if (isd != null && xPixel < isd.xPixel1) {
          zoomBox2D.setX(isd.toX(xPixel), isd.fixX(xPixel));
          zoomBox2D.yPixel0 = yPixel;
          thisWidget = zoomBox2D;
        }
      }
    } else if (thisWidget != null) {
      // mouse drag with widget
      if (thisWidget.isPin) {
        if (thisWidget == pin2Dy) {
          yPixel = fixY(yPixel);
          pin2Dy.yPixel0 = pin2Dy.yPixel1 = yPixel;
          int iSpec = isd.toSubSpectrumIndex(yPixel);
          getSpectrumAt(0).setCurrentSubSpectrum(iSpec);
          return true;
        }
        if (thisWidget == pin2Dx0 || thisWidget == pin2Dx1) {
          xPixel = isd.fixX(xPixel);
          thisWidget.setX(isd.toX(xPixel), xPixel);
          doZoom(pin2Dx0.x, multiScaleData.minY, pin2Dx1.x, multiScaleData.maxY, false, false, false);
          return true;
        }
        if (thisWidget == pin1Dx0 || thisWidget == pin1Dx1) {
          xPixel = fixX(xPixel);
          thisWidget.setX(toX0(xPixel), xPixel);
          doZoom(pin1Dx0.x, multiScaleData.minY,
              pin1Dx1.x, multiScaleData.maxY, false, false, false);
          return true;
        }
        if (thisWidget == pin1Dy0 || thisWidget == pin1Dy1) {
          yPixel = fixY(yPixel);
          thisWidget.setY(toY0(yPixel), yPixel);
          doZoom(multiScaleData.minXOnScale, pin1Dy0.y, 
              multiScaleData.maxXOnScale, pin1Dy1.y, false, false, false);
          return true;
        }
      } else if (thisWidget == zoomBox1D) {
        zoomBox1D.xPixel1 = fixX(xPixel);
        zoomBox1D.yPixel1 = (isIntegralDrag ? bottomPlotAreaPos : fixY(yPixel));
        if (isIntegralDrag && zoomBox1D.xPixel0 != zoomBox1D.xPixel1)
          checkIntegral(zoomBox1D.x, toX(zoomBox1D.xPixel1), false);
        return true;
      } else if (thisWidget == zoomBox2D) {
        zoomBox2D.xPixel1 = isd.fixX(xPixel);
        zoomBox2D.yPixel1 = fixY(yPixel);
        return true;
      }
    }
    return false;
  }
  
  private boolean checkPinSelected(int xPixel, int yPixel) {
    for (int i = 0; i < widgets.length; i++)
      if (widgets[i] != null && widgets[i].isPin && widgets[i].selected(xPixel, yPixel)) {
        thisWidget = widgets[i];
        return true;
      }
    return false;
  }

  /**
   * Implements mouseMoved in interface MouseMotionListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mouseMoved(MouseEvent e) {
    setToolTipForPixels(e.getX(), e.getY());
    repaint();
  }

  public void mouseDragged(MouseEvent e) {
    checkWidgetEvent(e.getX(), e.getY(), false);
    mouseMoved(e);
  }

  /**
   * Implements mouseReleased in interface MouseListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mouseReleased(MouseEvent e) {

    if (thisWidget == null || e.getButton() != MouseEvent.BUTTON1)
      return;

    if (thisWidget == zoomBox2D) {
      if (Math.abs(zoomBox2D.xPixel1 - zoomBox2D.xPixel0) <= MIN_DRAG_X_PIXELS)
        return;
      isd.setZoom(zoomBox2D.xPixel0, zoomBox2D.yPixel0, zoomBox2D.xPixel1,
          zoomBox2D.yPixel1);
      zoomBox2D.xPixel1 = zoomBox2D.xPixel0;
      pin2Dx0.setX(isd.toX(isd.xPixel0), isd.xPixel0);
      int xPixel = isd.xPixel0 + isd.xPixels - 1;
      pin2Dx1.setX(isd.toX(xPixel), xPixel);
      doZoom(isd.toX(pin2Dx0.xPixel0), multiScaleData.minY, isd
          .toX(pin2Dx1.xPixel0), multiScaleData.maxY, true, true, false);
    } else if (thisWidget == zoomBox1D) {
      if (Math.abs(zoomBox1D.xPixel1 - zoomBox1D.xPixel0) <= MIN_DRAG_X_PIXELS)
        return;
      int x1 = zoomBox1D.xPixel1;
      zoomBox1D.xPixel1 = zoomBox1D.xPixel0;
      doZoom(toX(zoomBox1D.xPixel0), toY(zoomBox1D.yPixel0), toX(x1),
          toY(zoomBox1D.yPixel1), true, true, false);
    } else if (thisWidget == pin1Dx0 || thisWidget == pin1Dx1 || thisWidget == pin2Dx0 || thisWidget == pin2Dx1) {
      addCurrentZoom();
    }
    thisWidget = null;
    return;
  }

  private double lastClickX = Double.MAX_VALUE;

  /**
   * Implements mouseClicked in interface MouseListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mouseClicked(MouseEvent e) {
    requestFocusInWindow();
    int xPixel = e.getX();
    int yPixel = e.getY();
    if (e.getButton() == MouseEvent.BUTTON3) {
      popup.setSelectedJSVPanel(this);
      popup.setSource(source);
      popup.gridCheckBoxMenuItem.setSelected(isGridOn());
      popup.coordsCheckBoxMenuItem.setSelected(isCoordinatesOn());
      popup.reversePlotCheckBoxMenuItem.setSelected(isPlotReversed());
      popup.show(this, xPixel, yPixel);
      return;
    }
    if (e.isControlDown())
      clearIntegrals();
    else if (e.getClickCount() == 2) {
      if (isInTopBar(xPixel, yPixel))
        doZoom(toX0(leftPlotAreaPos), multiScaleData.minY, toX0(rightPlotAreaPos), multiScaleData.maxY, true, true, false);
      else if (isInRightBar(xPixel, yPixel))
        doZoom(multiScaleData.minXOnScale, zoomInfoList.get(0).minY, multiScaleData.maxXOnScale, zoomInfoList.get(0).maxY, true, true, false);
      return;
    }
    if (xPixel != fixX(xPixel) || yPixel != fixY(yPixel)) {
      coordClicked = null;
      return;
    }
    coordClicked = new Coordinate(toX(xPixel), toY(yPixel));
    lastClickX = coordClicked.getXVal();
    notifyPeakPickedListeners(coordClicked);
  }

  private void clearIntegrals() {
    checkIntegral(Double.NaN, 0, false);
    repaint();
  }
  
  private void checkIntegral(double x1, double x2, boolean isFinal) {
    IntegralGraph ig = getSpectrum().getIntegrationGraph();
    if (ig == null)
      return;
    ig.addIntegral(x1, x2, isFinal);
  }


  private void setToolTipForPixels(int xPixel, int yPixel) {
    
    String hashX = "#";
    String hash1 = "0.00000000";
    if (multiScaleData.hashNums[0] <= 0)
      hashX = hash1.substring(0, Math.abs(multiScaleData.hashNums[0]) + 3);
    NumberFormat formatter = getFormatter(hashX);

    if (isd != null && isd.fixX(xPixel) == xPixel && fixY(yPixel) == yPixel) {
      String s = formatter.format(isd.toX(xPixel));
      int isub = isd.toSubSpectrumIndex(yPixel);
      JDXSpectrum spec = getSpectrumAt(0).getSubSpectra().get(isub);
      double d = spec.getY2D();
      s += "," + formatter.format(d);
      String units = spec.getY2DUnits();
      if (units.equals("HZ"))
        s += " HZ (" + formatter.format(spec.getY2DPPM()) + " PPM)";
      setToolTipText(s);
      return;
    }
    
    double xPt = toX(fixX(xPixel));
    String xx = formatter.format(xPt);

    double yPt = (isd != null && isd.isXWithinRange(xPixel) ? 
        isd.toSubSpectrumIndex(fixY(yPixel)) : toY(fixY(yPixel)));
    String hashY = "#";
    if (multiScaleData.hashNums[1] <= 0)
      hashY = hash1.substring(0, Math.abs(multiScaleData.hashNums[1]) + 3);
    formatter = getFormatter(hashY);
    coordStr = "(" + xx + ", " + formatter.format(yPt) + ")";

    if (xPixel != fixX(xPixel) || yPixel != fixY(yPixel)) {
      yPt = Double.NaN;
    } else if (nSpectra == 1) {
      if (!getSpectrum().isHNMR()) {
        yPt = spectra[0].getPercentYValueAt(xPt);
        xx += ", " + formatter.format(yPt);
      }
    } else if (getSpectrum().getIntegrationGraph() != null) {
      yPt = spectra[1].getPercentYValueAt(xPt);
      xx += ", " + getFormatter("#0.0").format(yPt);
    }
    setToolTipText(Double.isNaN(yPt) ? null : xx);
  }

  public void mouseEntered(MouseEvent e) {
  }

  
  public void mouseExited(MouseEvent e) {
  }

  public void keyPressed(KeyEvent e) {
    if (e.getModifiers() != 0) {
      if (e.isControlDown()) {
        switch (e.getKeyCode()) {
        case 45: //'-'
          scaleYBy(0.5);
          e.consume();
          break;
        case 61: //'='
          scaleYBy(2);
          e.consume();
          break;
        }
      }

      return;
    }
    switch (e.getKeyCode()) {
    case KeyEvent.VK_LEFT:
      toPeak(-1);
      e.consume();
      break;
    case KeyEvent.VK_RIGHT:
      toPeak(1);
      e.consume();
      break;
    case KeyEvent.VK_DOWN:
    case KeyEvent.VK_UP:
      int dir = (e.getKeyCode() == KeyEvent.VK_DOWN ? 1 : -1);
      if (getSpectrumAt(0).getSubSpectra() == null)
        notifyZoomListeners(dir, Double.NaN, Double.NaN, Double.NaN);
      else {
        advanceSubSpectrum(-dir);
        notifyListeners(getSpectrum().getTitleLabel());        
        repaint();
      }
      e.consume();
      break;
    }
  }

  public void toPeak(int istep) {
    istep *= (drawXAxisLeftToRight ? 1 : -1);
    coordClicked = new Coordinate(lastClickX, 0);
    JDXSpectrum spec = getSpectrum();
    int iPeak = spec.setNextPeak(coordClicked, istep);
    if (iPeak < 0)
      return;
    PeakInfo peak = spec.getPeakList().get(iPeak);
    spec.setSelectedPeak(peak);
    coordClicked.setXVal(lastClickX = peak.getX());
    notifyListeners(new PeakPickedEvent(this, coordClicked, peak
        .getStringInfo()));
  }

  public void advanceSubSpectrum(int i) {
    if (getSpectrumAt(0).advanceSubSpectrum(i))
      multiScaleData.setXRange(getSpectrum());
  }

  public void setCurrentSubSpectrum(int i) {
    if (getSpectrumAt(0).setCurrentSubSpectrum(i))
      multiScaleData.setXRange(getSpectrum());
  }
  
  private int[] tempi4 = new int[4];

  private void scaleYBy(double factor) {
    if (!allowYScale)
      return;
    double factor1 = factor;
    double factor2 = factor;
    switch (getSpectrum().getYScaleType()) {
    case JDXSpectrum.SCALE_NONE:
      return;
    case JDXSpectrum.SCALE_TOP:
      factor1 = 1;
      break;
    case JDXSpectrum.SCALE_BOTTOM:
      factor2 = 1;
      break;
    }
    if (isd != null)
      isd.getView(tempi4);
    doZoom(multiScaleData.minX, multiScaleData.minY / factor1,
        multiScaleData.maxX, multiScaleData.maxY / factor2, true, true, false);
    if (isd == null)
      return;
    update2dImage(true);
    isd.setView(tempi4);
    repaint();
  }

  public void keyReleased(KeyEvent e) {
  }

  public void keyTyped(KeyEvent e) {
    if (e.getKeyChar() == 'z') {
      previousView();
      return;
    }
    if (e.getKeyChar() == 'y') {
      nextView();
      return;
    }
  }

  public static JSVPanel getPanel0(JInternalFrame frame) {
    return ((JSVPanel) frame.getContentPane().getComponent(0));
  }

  public void setParam(DisplayScheme ds, ScriptToken st) {
    if (st == null || st == ScriptToken.TITLEFONTNAME)
      setTitleFontName(ds.getTitleFont());
    if (st == null || st == ScriptToken.DISPLAYFONTNAME)
      setDisplayFontName(ds.getDisplayFont());
    if (st == null || st == ScriptToken.TITLECOLOR)
      setTitleColor(ds.getColor("title"));
    if (st == null || st == ScriptToken.UNITSCOLOR)
      setUnitsColor(ds.getColor("units"));
    if (st == null || st == ScriptToken.SCALECOLOR)
      setScaleColor(ds.getColor("scale"));
    if (st == null || st == ScriptToken.COORDINATESCOLOR)
      setcoordinatesColor(ds.getColor("coordinates"));
    if (st == null || st == ScriptToken.GRIDCOLOR)
      setGridColor(ds.getColor("grid"));
    if (st == null || st == ScriptToken.PLOTCOLOR)
      setPlotColor(ds.getColor("plot"));
    if (st == null || st == ScriptToken.PLOTAREACOLOR)
      setPlotAreaColor(ds.getColor("plotarea"));
    if (st == null || st == ScriptToken.BACKGROUNDCOLOR)
      setBackgroundColor(ds.getColor("background"));
    if (st == null || st == ScriptToken.INTEGRALPLOTCOLOR)
      setIntegralPlotColor(ds.getColor("integral"));
  }

  public void setBoolean(Parameters parameters, ScriptToken st) {
    if (st == null || st == ScriptToken.GRIDON)
      setGridOn(parameters.gridOn);
    if (st == null || st == ScriptToken.COORDINATESON)
      setCoordinatesOn(parameters.coordinatesOn);
    if (st == null || st == ScriptToken.XSCALEON)
      setXScaleOn(parameters.xScaleOn);
    if (st == null || st == ScriptToken.YSCALEON)
      setYScaleOn(parameters.yScaleOn);
    if (st == null || st == ScriptToken.XUNITSON)
      setXUnitsOn(parameters.xUnitsOn);
    if (st == null || st == ScriptToken.YUNITSON)
      setYUnitsOn(parameters.yUnitsOn);
    if (st == null || st == ScriptToken.REVERSEPLOT)
      setReversePlot(parameters.reversePlot);
    if (st == null || st == ScriptToken.DISPLAY2D)
      setDisplay2D(parameters.display2D);
    if (st == null || st == ScriptToken.TITLEON)
      setTitleOn(parameters.titleOn);
    if (st == null || st == ScriptToken.TITLEBOLDON)
      setTitleBoldOn(parameters.titleBoldOn);
  }

  public JDXSpectrum getSpectrum() {
    return getSpectrumAt(0).getCurrentSubSpectrum();
  }

  public static JSVPanel taConvert(JSVPanel jsvp, int mode) {
    if (jsvp.getNumberOfSpectra() > 1)
      return null;
    JDXSpectrum spectrum = JDXSpectrum.taConvert(jsvp.getSpectrum(), mode);
    return (spectrum == jsvp.getSpectrum() ? null : new JSVPanel(spectrum));
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

  /////////////// 2D image /////////////////
  
  private BufferedImage image2D;
  private int thisWidth, thisPlotHeight;
  
  private void draw2DImage(Graphics g) {
    if (isd != null) {
      g.drawImage(image2D, 
          isd.xPixel0, isd.yPixel0,  // destination 
          isd.xPixel0 + isd.xPixels - 1, // destination 
          isd.yPixel0 + isd.yPixels - 1, // destination 
          isd.xView1, isd.yView1, isd.xView2, isd.yView2, null); // source
    }
  }

  private ImageScaleData isd;

  private boolean get2DImage(int width) {
    isd = new ImageScaleData();
    isd.setScale(zoomInfoList.get(0));
    if (!update2dImage(false))
      return false;
    widthRatio = 1.0 * (width - isd.xPixels) / width;
    isd.setXY0((int) Math.floor(width - isd.xPixels) - 20, topPlotAreaPos);
    isd.resetZoom();
    return true;
  }

  private boolean update2dImage(boolean forceNew) {    
    isd.setScale(multiScaleData);
    int[] buffer = getSpectrumAt(0).get2dBuffer(thisWidth, thisPlotHeight, isd, forceNew);
    if (buffer == null) {
      image2D = null;
      isd = null;
      return false;
    }
    image2D = new BufferedImage(isd.imageWidth, isd.imageHeight, BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster raster = image2D.getRaster();
    raster.setSamples(0, 0, isd.imageWidth, isd.imageHeight, 0, buffer);
    return true;
  }
}
