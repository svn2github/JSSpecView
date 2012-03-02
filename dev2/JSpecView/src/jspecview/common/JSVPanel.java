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

package jspecview.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.RenderedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet; //import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.export.Exporter;
import jspecview.export.SVGExporter;
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
 */
public class JSVPanel extends JPanel implements Printable, MouseListener,
    MouseMotionListener, KeyListener {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private static final int MIN_DRAG_X_PIXELS = 5;// fewer than this means no zoom

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

  // The list of Coordinate arrays
  private Coordinate[][] xyCoordsList;

  // width and height of the JSVPanel
  //private int width, height;

  //The position of the plot area
  private int plotAreaX = 80, plotAreaY = 30;

  // insets of the plot area
  private Insets plotAreaInsets = new Insets(plotAreaY, plotAreaX, 50, 50);

  // width and height of the plot area
  private int plotAreaWidth, plotAreaHeight;

  // Positions of the borders of the plotArea
  private int leftPlotAreaPos, rightPlotAreaPos, topPlotAreaPos,
      bottomPlotAreaPos;

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

  private boolean shouldDrawXAxisIncreasing;

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

  //private boolean isMousePressed; 
  private boolean isMouseDragged, isMouseReleased;
  private int zoomBoxX, currZoomBoxX;
  private boolean isMouseDraggedEvent;

  // whether to draw an overlayed plot increasing or decreasing
  private boolean overlayIncreasing = false;

  /* PUT FONT FACE AND SIZE ATTRIBUTES HERE */
  private String displayFontName = null;
  private String titleFontName = null;
  private boolean titleBoldOn = false;

  // The scale factors
  private double xFactorForScale, yFactorForScale;

  // List of Scale data for zoom
  private List<MultiScaleData> zoomInfoList;

  // Current index of view in zoomInfoList
  private int currentZoomIndex;

  // Determines if the xAxis should be displayed increasing
  private boolean isXAxisDisplayedIncreasing = true;

  // Used to identify whether the mouse was pressed within
  // the plot area;
  private boolean mousePressedInPlotArea;

  private boolean mouseMovedOk = false;

  // The initial coordinate and final coordinates of zoom area
  private double initX, initY, finalX, finalY, initXpixel;

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

  // listeners to handle coordinatesClicked
  private ArrayList<PeakPickedListener> peakPickedListeners = new ArrayList<PeakPickedListener>();

  /**
   * Constructs a new JSVPanel
   * 
   * @param spectrum
   *        the spectrum
   * @throws ScalesIncompatibleException
   */
  public JSVPanel(Graph spectrum) {
    super();
    try {
      initJSVPanel(new Graph[] { spectrum });
    } catch (ScalesIncompatibleException e) {
      // impossible
    }
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
      throws JSpecViewException {
    super();
    try {
      initJSVPanel(new Graph[] { spectrum }, new int[] { startIndex },
          new int[] { endIndex });
    } catch (ScalesIncompatibleException sie) {
    }
  }

  /**
   * Constructs a JSVPanel with an array of Spectra
   * 
   * @param spectra
   *        an array of spectra (<code>Spectrum</code>)
   * @throws ScalesIncompatibleException
   */
  public JSVPanel(Graph[] spectra) throws ScalesIncompatibleException {
    super();
    initJSVPanel(spectra);
  }

  public static JSVPanel getIntegralPanel(JDXSpectrum spectrum, Color color) {
    try {
      Graph graph = spectrum.getIntegrationGraph();
      JSVPanel jsvp = new JSVPanel(new Graph[] { spectrum, graph });
      jsvp.setTitle(graph.getTitle());
      jsvp.setPlotColors(new Color[] { jsvp.getPlotColor(0), color });
      return jsvp;
    } catch (ScalesIncompatibleException e) {
      return null;
    }
  }

  /**
   * Constructs a <code>JSVPanel</code> with an array of spectra and
   * corresponding start and end indices of data points that should be displayed
   * 
   * @param spectra
   *        the array of spectra
   * @param startIndices
   *        the indices of coordinates at which the display should start
   * @param endIndices
   *        the indices of the end coordinates
   * @throws JSpecViewException
   * @throws ScalesIncompatibleException
   */
  public JSVPanel(Graph[] spectra, int[] startIndices, int[] endIndices)
      throws JSpecViewException, ScalesIncompatibleException {
    super();
    initJSVPanel(spectra, startIndices, endIndices);
  }

  /**
   * Constructs a JSVMultiPanel with a List of Spectra
   * 
   * @param spectra
   *        a <code>List</code> of spectra
   * @throws ScalesIncompatibleException
   */
  public JSVPanel(List<JDXSpectrum> spectra) throws ScalesIncompatibleException {
    this((Graph[]) spectra.toArray(new Graph[spectra.size()]));
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
      int[] endIndices) throws JSpecViewException, ScalesIncompatibleException {
    this((Graph[]) spectra.toArray(new Graph[spectra.size()]), startIndices,
        endIndices);
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
                            int[] endIndices) throws JSpecViewException,
      ScalesIncompatibleException {
    this.spectra = spectra;
    nSpectra = spectra.length;
    if (nSpectra == 1)
      setTitle(getSpectrum().getTitleLabel());

    checkUnits();

    xyCoordsList = new Coordinate[nSpectra][];

    for (int i = 0; i < nSpectra; i++) {
      xyCoordsList[i] = spectra[i].getXYCoords();
    }

    if (nSpectra > plotColors.length) {
      Color[] tmpPlotColors = new Color[nSpectra];
      int numAdditionColors = nSpectra - plotColors.length;
      System.arraycopy(plotColors, 0, tmpPlotColors, 0, plotColors.length);

      for (int i = 0, j = plotColors.length; i < numAdditionColors; i++, j++) {
        tmpPlotColors[j] = generateRandomColor();
      }

      plotColors = tmpPlotColors;
    }

    zoomInfoList = new ArrayList<MultiScaleData>();
    doZoomWithoutRepaint(initX, finalX, startIndices, endIndices);

    setBorder(BorderFactory.createLineBorder(Color.lightGray));
  }

  // throw exception when units are not the same on both axes

  /**
   * Initializes the JSVPanel
   * 
   * @param spectra
   *        the array of spectra
   * @throws ScalesIncompatibleException
   */
  private void initJSVPanel(Graph[] spectra) throws ScalesIncompatibleException {
    setDefaultMouseListener();
    this.spectra = spectra;
    nSpectra = spectra.length;
    if (nSpectra == 1)
      setTitle(getSpectrum().getTitleLabel());
    xyCoordsList = new Coordinate[nSpectra][];

    int[] startIndices = new int[nSpectra];
    int[] endIndices = new int[nSpectra];
    //boolean[] plotIncreasing = new boolean[numOfSpectra];

    // test if all have same x and y units
    checkUnits();

    for (int i = 0; i < nSpectra; i++) {
      xyCoordsList[i] = spectra[i].getXYCoords();
      startIndices[i] = 0;
      endIndices[i] = xyCoordsList[i].length - 1;
    }

    setPlotColors(Parameters.defaultPlotColors);

    multiScaleData = new MultiScaleData(xyCoordsList, startIndices, endIndices,
        10, 10, getSpectrum().isContinuous());

    //add data to zoomInfoList
    zoomInfoList = new ArrayList<MultiScaleData>();
    zoomInfoList.add(multiScaleData);

    setBorder(BorderFactory.createLineBorder(Color.lightGray));
  }

  /**
   * 
   * @throws ScalesIncompatibleException
   */
  private void checkUnits() throws ScalesIncompatibleException {
    if (!JDXSpectrum.areScalesCompatible(spectra)) {
      throw new ScalesIncompatibleException();
    }
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

  /* ------------------------------LISTENER METHODS-------------------------*/

  /**
   * Sets default MouseListener
   */
  public void setDefaultMouseListener() {
    addMouseListener(this);
    addMouseMotionListener(this);
    addKeyListener(this);
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
    plotAreaX = left;
    plotAreaY = top;
    plotAreaInsets = new Insets(top, left, bottom, right);
  }

  /**
   * Sets the plot area insets
   * 
   * @param insets
   *        the insets of the plot area
   */
  public void setPlotAreaInsets(Insets insets) {
    plotAreaX = insets.left;
    plotAreaY = insets.top;
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
   * Displays plot in reverse if val is true
   * 
   * @param val
   *        true or false
   */
  public void setReversePlot(boolean val) {
    plotReversed = val;
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
   * Sets the color of the plots
   * 
   * @param colors
   *        an array of <code>Color</code>
   */
  public void setPlotColors(Color[] colors) {
    Color[] tmpPlotColors;
    if (colors.length < nSpectra) {
      tmpPlotColors = new Color[nSpectra];
      int numAdditionColors = nSpectra - colors.length;
      System.arraycopy(colors, 0, tmpPlotColors, 0, colors.length);

      for (int i = 0, j = colors.length; i < numAdditionColors; i++, j++) {
        tmpPlotColors[j] = generateRandomColor();
      }

      plotColors = tmpPlotColors;
    } else if (colors.length > nSpectra) {
      plotColors = new Color[nSpectra];
      System.arraycopy(colors, 0, plotColors, 0, nSpectra);
    } else
      plotColors = colors;

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

  /**
   * Sets the integration ratios that will be displayed
   * 
   * @param ratios
   *        array of the integration ratios
   */
  public void addAnnotation(String value) {
    if (value.equalsIgnoreCase("none")) {
      annotations = null;
      return;
    }
    ColoredAnnotation annotation = ColoredAnnotation.getAnnotation(value);
    if (annotation == null)
      return;
    if (annotations == null)
      annotations = new ArrayList<Annotation>();
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
    isXAxisDisplayedIncreasing = val;
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
   * Returns true if plot is reversed
   * 
   * @return true if plot is reversed
   */
  public boolean isPlotReversed() {
    return plotReversed;
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
   * Returns the list of coordinates of all the Graphs
   * 
   * @return an array of arrays of <code>Coordinate</code>
   */
  public Coordinate[][] getXYCoordsList() {
    return xyCoordsList;
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
    return isXAxisDisplayedIncreasing;
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
    shouldDrawXAxisIncreasing = isXAxisDisplayedIncreasing ^ plotReversed;
    plotAreaWidth = width - (plotAreaInsets.right + plotAreaInsets.left);
    plotAreaHeight = height - (plotAreaInsets.top + plotAreaInsets.bottom);

    ///System.out.println("JSVPANEL "  + width + " " + height + " " + plotAreaWidth + " " + plotAreaHeight + " " + title);

    leftPlotAreaPos = plotAreaX;
    rightPlotAreaPos = plotAreaWidth + plotAreaX;
    topPlotAreaPos = plotAreaY;
    bottomPlotAreaPos = plotAreaHeight + plotAreaY;

    xFactorForScale = (multiScaleData.maxXOnScale - multiScaleData.minXOnScale)
        / plotAreaWidth;
    yFactorForScale = (multiScaleData.maxYOnScale - multiScaleData.minYOnScale)
        / plotAreaHeight;

    // fill plot area color
    g.setColor(plotAreaColor);
    g.fillRect(plotAreaX, plotAreaY, plotAreaWidth, plotAreaHeight);

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

    if (grid)
      drawGrid(g, height, width);
    for (int i = 0; i < nSpectra; i++)
      drawPlot(g, i, height, width);
    if (xscale && xAxis)
      drawXScale(g, height, width);
    if (yscale && yAxis)
      drawYScale(g, height, width);
    if (title)
      drawTitle(g, height, width);
    if (xunits && xAxis)
      drawXUnits(g, height, width);
    if (yunits && yAxis)
      drawYUnits(g, height, width);
    if (coords)
      drawCoordinates(g, height, width);
    if (zoomEnabled)
      drawZoomBox(g);
    if (integrationRatios != null)
      drawAnnotations(g, height, width, integrationRatios, integralPlotColor);
    if (annotations != null)
      drawAnnotations(g, height, width, annotations, null);
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

    int x1 = xPixels(startX);
    int x2 = xPixels(endX);

    if (x1 > x2) {
      int tmp = x1;
      x1 = x2;
      x2 = tmp;
    }

    if (Logger.debugging) {
      System.out.println("x1: " + x1);
      System.out.println("x2: " + x2);
    }

    // if either pixel is outside of plot area
    if (!isPixelWithinPlotArea(x1) || !isPixelWithinPlotArea(x2)) {
      // if both are ouside of plot area
      if (!isPixelWithinPlotArea(x1) && !isPixelWithinPlotArea(x2)) {
        // check if leftareapos and rightareapos both lie between
        // x1 and x2
        if (leftPlotAreaPos >= x1 && rightPlotAreaPos <= x2) {
          x1 = leftPlotAreaPos;
          x2 = rightPlotAreaPos;
        } else {
          return;
        }
      } else if (isPixelWithinPlotArea(x1) && !isPixelWithinPlotArea(x2)) {
        x2 = rightPlotAreaPos;
      } else if (!isPixelWithinPlotArea(x1) && isPixelWithinPlotArea(x2)) {
        x1 = leftPlotAreaPos;
      }
    }
    g.setColor(color);
    g.fillRect(x1, plotAreaY, Math.abs(x2 - x1), (isFullHeight ? plotAreaHeight
        : 5));
  }

  private int xPixels(double dx) {
    int x = (int) ((dx - multiScaleData.minXOnScale) / xFactorForScale);
    return (int) (shouldDrawXAxisIncreasing ? leftPlotAreaPos + x
        : rightPlotAreaPos - x);
  }

  private int yPixels(double yVal) {
    return (Double.isNaN(yVal) ? Integer.MIN_VALUE : invertY((int) (topPlotAreaPos + (yVal - multiScaleData.minYOnScale)
        / yFactorForScale)));
  }

  private boolean isPixelWithinPlotArea(int pix) {
    if (pix >= leftPlotAreaPos && pix <= rightPlotAreaPos) {
      return true;
    }
    return false;
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
    Coordinate[] xyCoords = xyCoordsList[index];

    // Draw a border
    if (!gridOn) {
      g.setColor(gridColor);
      g.drawRect(plotAreaX, plotAreaY, plotAreaWidth, plotAreaHeight);
    }

    Color color = plotColors[index];
    if (index == 1 && getSpectrum().getIntegrationGraph() != null)
      color = integralPlotColor;
    g.setColor(color);

    // Check if revPLot on

    if (spectra[index].isContinuous()) {
      for (int i = multiScaleData.startDataPointIndices[index]; i < multiScaleData.endDataPointIndices[index]; i++) {
        Coordinate point1 = xyCoords[i];
        Coordinate point2 = xyCoords[i + 1];
        int x1 = xPixels(point1.getXVal());
        int y1 = yPixels(point1.getYVal());
        int x2 = xPixels(point2.getXVal());
        int y2 = yPixels(point2.getYVal());
        if (y1 == Integer.MIN_VALUE || y2 == Integer.MIN_VALUE)
          continue;
        g.drawLine(x1, y1, x2, y2);
      }
    } else {
      for (int i = multiScaleData.startDataPointIndices[index]; i <= multiScaleData.endDataPointIndices[index]; i++) {
        Coordinate point = xyCoords[i];
        int x1 = xPixels(point.getXVal());
        int y1 = yPixels(Math.max(multiScaleData.minYOnScale, 0));
        int y2 = yPixels(point.getYVal());
        if (y2 == Integer.MIN_VALUE)
          continue;
        g.drawLine(x1, y1, x1, y2);
      }
      if (multiScaleData.minYOnScale < 0) {
        int y = yPixels(0);
        g.drawLine(rightPlotAreaPos, invertY(y), leftPlotAreaPos, invertY(y));
      }
    }
  } // End drawPlot

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
        int x = xPixels(val);
        int y1 = yPixels(multiScaleData.minYOnScale);
        int y2 = yPixels(multiScaleData.maxYOnScale);
        g.drawLine(x, y1, x, y2);
      }
    } else {
      lastX = multiScaleData.maxXOnScale * 1.0001;
      for (double val = multiScaleData.firstX; val <= lastX; val += multiScaleData.xStep) {
        int x = xPixels(val);
        int y1 = yPixels(multiScaleData.minYOnScale);
        int y2 = yPixels(multiScaleData.maxYOnScale);
        g.drawLine(x, y1, x, y2);
      }
    }

    for (double val = multiScaleData.minYOnScale; val < multiScaleData.maxYOnScale
        + multiScaleData.yStep / 2; val += multiScaleData.yStep) {
      int x1 = xPixels(multiScaleData.minXOnScale);
      int x2 = xPixels(multiScaleData.maxXOnScale);
      int y = yPixels(val);
      g.drawLine(x1, y, x2, y);
    }
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

    boolean drawScaleIncreasing = shouldDrawXAxisIncreasing;

    if (multiScaleData.hashNums[0] <= 0)
      hashX = hash1.substring(0, Math.abs(multiScaleData.hashNums[0]) + 3);

    DecimalFormat displayXFormatter = TextFormat.getDecimalFormat(hashX);

    g.setFont(new Font((isPrinting ? printingFont : displayFontName),
        Font.PLAIN, calculateFontSize(width, 12, true)));
    FontMetrics fm = g.getFontMetrics();
    int y1 = invertY((int) topPlotAreaPos);
    int y2 = invertY((int) (topPlotAreaPos - 3));
    double maxWidth = (xPixels(multiScaleData.xStep) - xPixels(0)) * 0.95;
    double lastX;
    if (Double.isNaN(multiScaleData.firstX)) {
      lastX = multiScaleData.maxXOnScale + multiScaleData.xStep / 2;
      for (double val = multiScaleData.minXOnScale, vald = multiScaleData.maxXOnScale; val < lastX; val += multiScaleData.xStep, vald -= multiScaleData.xStep) {
        int x = (int) (leftPlotAreaPos + (((drawScaleIncreasing ? val : vald) - multiScaleData.minXOnScale) / xFactorForScale));
        g.setColor(gridColor);
        g.drawLine(x, y1, x, y2);
        g.setColor(scaleColor);
        String s = displayXFormatter.format(val);
        int w = fm.stringWidth(s);
        g.drawString(s, x - w / 2, y2 + fm.getHeight());
      }
    } else {
      lastX = multiScaleData.maxXOnScale * 1.0001;
      for (double val = multiScaleData.firstX; val <= lastX; val += multiScaleData.xStep) {
        int x = xPixels(val);
        g.setColor(gridColor);
        g.drawLine(x, y1, x, y2);
        g.setColor(scaleColor);
        String s = displayXFormatter.format(val);
        int w = fm.stringWidth(s);
        g.drawString(s, x - w / 2, y2 + fm.getHeight());
        val += Math.floor(w/maxWidth) * multiScaleData.xStep;
      }
    }
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
    DecimalFormat displayYFormatter = TextFormat.getDecimalFormat(hashY);
    g.setFont(new Font((isPrinting ? printingFont : displayFontName),
        Font.PLAIN, calculateFontSize(width, 12, true)));
    FontMetrics fm = g.getFontMetrics();
    double max = multiScaleData.maxYOnScale + multiScaleData.yStep / 2;
    for (double val = multiScaleData.minYOnScale; val < max; val += multiScaleData.yStep) {
      int x1 = (int) leftPlotAreaPos;
      int y = yPixels(val);
      g.setColor(gridColor);
      g.drawLine(x1, y, x1 - 3, y);
      g.setColor(scaleColor);
      String s = displayYFormatter.format(val);
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
    int style = (isPrinting || titleBoldOn ? Font.BOLD : Font.PLAIN);
    g.setFont(new Font((isPrinting ? printingFont : titleFontName), style,
        calculateFontSize(width, 14, true)));
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
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  private void drawXUnits(Graphics g, int height, int width) {
    g.setColor(unitsColor);
    g.setFont(new Font((isPrinting ? printingFont : displayFontName),
        Font.ITALIC, calculateFontSize(width, 10, true)));
    FontMetrics fm = g.getFontMetrics();
    String s = spectra[0].getXUnits();
    g.drawString(s, (int) (rightPlotAreaPos - fm.stringWidth(s)),
        (int) (bottomPlotAreaPos + fm.getHeight() * 2.5));
  }

  /**
   * Draws the Y Units
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  private void drawYUnits(Graphics g, int height, int width) {
    g.setColor(unitsColor);
    g.setFont(new Font((isPrinting ? printingFont : displayFontName),
        Font.ITALIC, calculateFontSize(width, 10, true)));
    FontMetrics fm = g.getFontMetrics();
    g.drawString(spectra[0].getYUnits(), 5, (int) (topPlotAreaPos - fm
        .getHeight()));
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
    Font font;

    if (isPrinting)
      font = new Font(printingFont, Font.PLAIN, calculateFontSize(width, 12,
          true));
    else
      font = new Font(displayFontName, Font.PLAIN, calculateFontSize(width, 12,
          true));
    g.setFont(font);
    int x = (int) ((plotAreaWidth + leftPlotAreaPos) * 0.85);
    g.drawString(coordStr, x, (int) (topPlotAreaPos - 10));
  }

  // determine whether there are any ratio annotations to draw
  private void drawAnnotations(Graphics g, int height, int width,
                               ArrayList<Annotation> annotations, Color color) {
    g.setFont(new Font(isPrinting ? printingFont : displayFontName, Font.BOLD,
        calculateFontSize(width, 12, true)));
    for (int i = annotations.size(); --i >= 0;) {
      Annotation note = annotations.get(i);
      color = (note instanceof ColoredAnnotation ? ((ColoredAnnotation) note).color
          : color);
      if (color == null)
        color = Color.BLACK;
      g.setColor(color);
      int x = xPixels(note.getXVal());
      int y = (note.isPixels ? invertY((int) (bottomPlotAreaPos
          + note.getYVal() - 10)) : yPixels(note.getYVal()));
      g.drawString(note.text, x, y);
    }
  }

  /**
   * Draws a light grey rectangle over the portion of the spectrum to be zoomed
   * 
   * @param g
   *        the Graphics object
   */
  private void drawZoomBox(Graphics g) {
    //  adapted from suggestion by Valery Tkachenko 5 Nov 2010 and previously implemented for ChemSpider
    if (isMouseDragged) {
      g.setColor(zoomBoxColor);
      g.fillRect(Math.min(zoomBoxX, currZoomBoxX), topPlotAreaPos, Math
          .abs(currZoomBoxX - zoomBoxX), bottomPlotAreaPos - topPlotAreaPos);
      isMouseDragged = false;
    }
    if (isMouseReleased) {
      isMouseReleased = false; // bug fix suggested by Tim te Beek 29 Oct 2010    
      repaint();
    }
  }

  /**
   * Calculates the size of the font to display based on the window size
   * 
   * @param length
   *        ??
   * @param initSize
   *        the intial size of the font
   * @param isWidth
   *        true if the text lies along the width esle false
   * @return the size of the font
   */
  private int calculateFontSize(double length, int initSize, boolean isWidth) {
    int size = initSize;
    // TODO THIS METHOD NEEDS REWORKING!!!

    if (isWidth) {
      if (length < 400)
        size = (int) ((length * initSize) / 400);
    } else {
      if (length < 250)
        size = (int) ((length * initSize) / 250);
    }

    return size;
  } // End calculateFontSize

  /**
   * Utility used to invert the value of y in the default coordinate system
   * 
   * @param y
   *        the y pixel value
   * @param height
   *        the height to be drawn in pixels
   * @return the inverted y pixel value
   */
  private int invertY(int y) {
    return (plotAreaHeight - y + (2 * plotAreaInsets.top));
  }

  /**
   * Calculates spectrum coordinates from system Coordinates
   * 
   * @param xPixel
   *        the x pixel value of the coordinate
   * @param yPixel
   *        the y pixel value of the coordinate
   * @return the coordinate
   */
  private Coordinate getCoordFromPoint(int xPixel, int yPixel) {
    double xPt, yPt;

    boolean plotIncreasing;
    plotIncreasing = shouldDrawXAxisIncreasing;

    if (!plotIncreasing)
      xPt = (((rightPlotAreaPos - xPixel) * xFactorForScale) + multiScaleData.minXOnScale);
    else
      xPt = multiScaleData.maxXOnScale
          - (((rightPlotAreaPos - xPixel) * xFactorForScale));

    yPt = multiScaleData.maxYOnScale
        + (((topPlotAreaPos - yPixel) * yFactorForScale));

    return new Coordinate(xPt, yPt);
  }

  /*-------------------- METHODS FOR ZOOM SUPPORT--------------------------*/

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
  public void doZoom(double initX, double initY, double finalX, double finalY) {
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

    // Determine if the range of the area selected for zooming is within the plot
    // Area and if not ensure that it is

    if (!ScaleData.isWithinRange(initX, multiScaleData)
        && !ScaleData.isWithinRange(finalX, multiScaleData))
      return;
    if (!ScaleData.isWithinRange(initX, multiScaleData)) {
      initX = multiScaleData.minX;
    } else if (!ScaleData.isWithinRange(finalX, multiScaleData)) {
      finalX = multiScaleData.maxX;
    }
    int[] startIndices = new int[nSpectra];
    int[] endIndices = new int[nSpectra];
    if (doZoomWithoutRepaint(initX, finalX, startIndices, endIndices))
      repaint();
  }

  /**
   * Zooms the spectrum but does not repaint so that it is not visible
   * 
   * @param initX
   *        TODO
   * @param finalX
   *        TODO
   * @param startIndices
   *        the start indices
   * @param endIndices
   *        the end indices
   * 
   * @return true if successful
   * @throws JSpecViewException
   */
  private boolean doZoomWithoutRepaint(double initX, double finalX,
                                       int[] startIndices, int[] endIndices) {
    if (!zoomEnabled)
      return false;
    if (!multiScaleData.setDataPointIndices(spectra, initX, finalX,
        minNumOfPointsForZoom, startIndices, endIndices))
      return false;
    multiScaleData = new MultiScaleData(xyCoordsList, startIndices, endIndices,
        10, 10, getSpectrum().isContinuous());
    // add to and clean the zoom list
    if (zoomInfoList.size() > currentZoomIndex + 1)
      for (int i = zoomInfoList.size() - 1; i > currentZoomIndex; i--)
        zoomInfoList.remove(i);
    zoomInfoList.add(multiScaleData);
    currentZoomIndex++;
    return true;
  }

  /**
   * Resets the spectrum to it's original view
   */
  public void reset() {
    multiScaleData = zoomInfoList.get(0);
    currentZoomIndex = 0;
    repaint();
  }

  /**
   * Clears all views in the zoom list
   */
  public void clearViews() {
    reset();
    int loopNum = zoomInfoList.size();
    for (int i = 1; i < loopNum; i++) {
      zoomInfoList.remove(1);
    }
  }

  /**
   * Displays the previous view zoomed
   */
  public void previousView() {
    if (currentZoomIndex > 0) {
      multiScaleData = zoomInfoList.get(--currentZoomIndex);
      repaint();
    }
  }

  /**
   * Displays the next view zoomed
   */
  public void nextView() {
    if (currentZoomIndex + 1 < zoomInfoList.size()) {
      multiScaleData = zoomInfoList.get(++currentZoomIndex);
      repaint();
    }
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

  /**
   * Sets whether plots that are overlayed should be drawn in increasing of
   * decreasing
   * 
   * @param val
   *        true if increasing, false otherwise
   */
  public void setOverlayIncreasing(boolean val) {
    overlayIncreasing = val;
  }

  /**
   * Returns whether overlayed plots are drawn increasing
   * 
   * @return true is increasing, false otherwise
   */
  public boolean isOverlayIncreasing() {
    return overlayIncreasing;
  }

  /*
   public void generateSVG(){
   // Get a DOMImplementation
   DOMImplementation domImpl =
   GenericDOMImplementation.getDOMImplementation();

   // Create an instance of org.w3c.dom.Document
   Document document = domImpl.createDocument(null, "svg", null);

   // Create an instance of the SVG Generator
   SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

   // Ask the test to render into the SVG Graphics2D implementation
   this.paintComponent(svgGenerator);

   // Finally, stream out SVG to the standard output using UTF-8
   // character to byte encoding
   boolean useCSS = true; // we want to use CSS style attribute
   try{
   Writer out = new FileWriter("testSvgGen.svg");
   svgGenerator.stream(out, useCSS);
   out.close();
   }
   catch(Exception e){
   }
   }
   */

  private String dirLastExported;

  /**
   * Auxiliary Export method
   * 
   * @param spec
   *        the spectrum to export
   * @param fc
   *        file chooser to use
   * @param comm
   *        the format to export in
   * @param index
   *        the index of the spectrum
   * @param recentFileName
   * @param dirLastExported
   * @return dirLastExported
   */
  public String exportSpectrum(JDXSpectrum spec, JFileChooser fc, String comm,
                               int index, String recentFileName,
                               String dirLastExported) {
    JSpecViewFileFilter filter = new JSpecViewFileFilter();
    //TODO: This is flawed. It assumes the file name has one and only one "." in it. 
    String name = TextFormat.split(recentFileName, ".")[0];
    if ("XY FIX PAC SQZ DIF".indexOf(comm) >= 0) {
      filter.addExtension("jdx");
      filter.addExtension("dx");
      filter.setDescription("JCAMP-DX Files");
      name += ".jdx";
    } else {
      if (comm.toLowerCase().indexOf("iml") >= 0
          || comm.toLowerCase().indexOf("aml") >= 0)
        comm = "XML";
      filter.addExtension(comm);
      filter.setDescription(comm + " Files");
      name += "." + comm.toLowerCase();
    }
    fc.setFileFilter(filter);
    fc.setSelectedFile(new File(name));
    int returnVal = fc.showSaveDialog(this);
    if (returnVal != JFileChooser.APPROVE_OPTION)
      return dirLastExported;
    File file = fc.getSelectedFile();
    this.dirLastExported = file.getParent();
    int option = -1;
    int startIndex, endIndex;
    if (file.exists()) {
      option = JOptionPane.showConfirmDialog(this, "Overwrite file?",
          "Confirm Overwrite Existing File", JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);
    }

    if (option != -1) {
      if (option == JOptionPane.NO_OPTION) {
        return exportSpectrum(spec, fc, comm, index, recentFileName,
            this.dirLastExported);
      }
    }

    startIndex = getStartDataPointIndices()[index];
    endIndex = getEndDataPointIndices()[index];

    try {
      if (comm.equals("PNG")) {
        try {
          Rectangle r = getBounds();
          Image image = createImage(r.width, r.height);
          Graphics g = image.getGraphics();
          paint(g);
          ImageIO.write((RenderedImage) image, "png", new File(file
              .getAbsolutePath()));
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      } else if (comm.equals("JPG")) {
        try {
          Rectangle r = getBounds();
          Image image = createImage(r.width, r.height);
          Graphics g = image.getGraphics();
          paint(g);
          ImageIO.write((RenderedImage) image, "jpg", new File(file
              .getAbsolutePath()));
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      } else if (comm.equals("SVG")) {
        (new SVGExporter()).exportAsSVG(file.getAbsolutePath(), this, index,
            true);
      } else {
        Exporter.export(comm, file.getAbsolutePath(), spec, startIndex,
            endIndex);
      }
    } catch (IOException ioe) {
      // STATUS --> "Error writing: " + file.getName()
    }
    return this.dirLastExported;
  }

  public String exportSpectra(JFrame frame, JFileChooser fc, String type,
                              String recentFileName, String dirLastExported) {
    // if JSVPanel has more than one spectrum...Choose which one to export
    int numOfSpectra = getNumberOfSpectra();
    if (numOfSpectra == 1 || type.equals("JPG") || type.equals("PNG")) {

      JDXSpectrum spec = getSpectrum();
      return exportSpectrum(spec, fc, type, 0, recentFileName, dirLastExported);
    }

    String[] items = new String[numOfSpectra];
    for (int i = 0; i < numOfSpectra; i++) {
      JDXSpectrum spectrum = getSpectrumAt(i);
      items[i] = spectrum.getTitle();
    }

    final JDialog dialog = new JDialog(frame, "Choose Spectrum", true);
    dialog.setResizable(false);
    dialog.setSize(200, 100);
    dialog.setLocation((getLocation().x + getSize().width) / 2,
        (getLocation().y + getSize().height) / 2);
    final JComboBox cb = new JComboBox(items);
    Dimension d = new Dimension(120, 25);
    cb.setPreferredSize(d);
    cb.setMaximumSize(d);
    cb.setMinimumSize(d);
    JPanel panel = new JPanel(new FlowLayout());
    JButton button = new JButton("OK");
    panel.add(cb);
    panel.add(button);
    dialog.getContentPane().setLayout(new BorderLayout());
    dialog.getContentPane().add(
        new JLabel("Choose Spectrum to export", SwingConstants.CENTER),
        BorderLayout.NORTH);
    dialog.getContentPane().add(panel);
    this.dirLastExported = dirLastExported;
    final String dl = dirLastExported;
    final String t = type;
    final String rfn = recentFileName;
    final JFileChooser f = fc;
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int index = cb.getSelectedIndex();
        JDXSpectrum spec = getSpectrumAt(index);
        dialog.dispose();
        exportSpectrum(spec, f, t, index, rfn, dl);
      }
    });
    dialog.setVisible(true);
    return this.dirLastExported;
  }

  public static void setMenus(JMenu saveAsMenu, JMenu saveAsJDXMenu,
                              JMenu exportAsMenu, ActionListener actionListener) {
    saveAsMenu.setText("Save As");
    saveAsJDXMenu.setText("JDX");
    exportAsMenu.setText("Export As");
    addMenuItem(saveAsJDXMenu, "XY", actionListener);
    addMenuItem(saveAsJDXMenu, "DIF", actionListener);
    addMenuItem(saveAsJDXMenu, "DIFDUP", actionListener);
    addMenuItem(saveAsJDXMenu, "FIX", actionListener);
    addMenuItem(saveAsJDXMenu, "PAC", actionListener);
    addMenuItem(saveAsJDXMenu, "SQZ", actionListener);
    saveAsMenu.add(saveAsJDXMenu);
    addMenuItem(saveAsMenu, "CML", actionListener);
    addMenuItem(saveAsMenu, "XML (AnIML)", actionListener);
    addMenuItem(exportAsMenu, "JPG", actionListener);
    addMenuItem(exportAsMenu, "PNG", actionListener);
    addMenuItem(exportAsMenu, "SVG", actionListener);
  }

  private static void addMenuItem(JMenu m, String key,
                                  ActionListener actionListener) {
    JMenuItem jmi = new JMenuItem();
    jmi.setMnemonic(key.charAt(0));
    jmi.setText(key);
    jmi.addActionListener(actionListener);
    m.add(jmi);
  }

  public void destroy() {
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

  /**
   * Adds a CoordinatePickedListener
   * 
   * @param listener
   */
  public void addPeakPickedListener(PeakPickedListener listener) {
    if (!peakPickedListeners.contains(listener)) {
      peakPickedListeners.add(listener);
    }
  }

  /**
   * Notifies CoordinatePickedListeners
   * 
   * @param coord
   */
  public void notifyPeakPickedListeners(Coordinate coord) {
    // TODO: Currently Aassumes spectra are not overlayed
    notifyPeakPickedListeners(new PeakPickedEvent(this, coord, getSpectrum()
        .getAssociatedPeakInfo(coord)));
  }

  private void notifyPeakPickedListeners(PeakPickedEvent eventObj) {
    for (int i = 0; i < peakPickedListeners.size(); i++) {
      PeakPickedListener listener = peakPickedListeners.get(i);
      if (listener != null) {
        listener.peakPicked(eventObj);
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

  /**
   * Implements mouseClicked in interface MouseListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mouseClicked(MouseEvent e) {
    requestFocusInWindow();
    if (e.getButton() == MouseEvent.BUTTON3) {
      popup.setSelectedJSVPanel(this);
      popup.setSource(source);
      popup.gridCheckBoxMenuItem.setSelected(isGridOn());
      popup.coordsCheckBoxMenuItem.setSelected(isCoordinatesOn());
      popup.reversePlotCheckBoxMenuItem.setSelected(isPlotReversed());
      popup.show(this, e.getX(), e.getY());
      return;
    }
    fireMouseClicked(e);
  }

  /**
   * Implements mouseEntered in interface MouseListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mouseEntered(MouseEvent e) {
  }

  /**
   * Implements mouseExited in interface MouseListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mouseExited(MouseEvent e) {
  }

  /**
   * Implements mousePressed in interface MouseListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mousePressed(MouseEvent e) {
    // Maybe put this in a fireMousePressed() method
    if (e.getButton() != MouseEvent.BUTTON1)
      return;

    int x = e.getX();
    int y = e.getY();

    int xPixel = x /*- getX()*/;
    int yPixel = y /*- getY()*/;

    if (xPixel >= leftPlotAreaPos && xPixel <= rightPlotAreaPos
        && yPixel >= topPlotAreaPos && yPixel <= bottomPlotAreaPos) {

      //isMousePressed = true;
      zoomBoxX = xPixel;

      double xPt, yPt;

      Coordinate coord = getCoordFromPoint(xPixel, yPixel);

      xPt = coord.getXVal();
      yPt = coord.getYVal();

      initXpixel = xPixel;
      initX = xPt;
      initY = yPt;
      mousePressedInPlotArea = true;
      repaint();

    } else
      mousePressedInPlotArea = false;
  }

  /**
   * Implements mouseReleased in interface MouseListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mouseReleased(MouseEvent e) {
    // Maybe use a fireMouseReleased Method

    if (e.getButton() != MouseEvent.BUTTON1)
      return;

    int x = e.getX();
    int y = e.getY();

    int xPixel = x /*- getX()*/;
    int yPixel = y /*- getY()*/;

    if (xPixel >= leftPlotAreaPos && xPixel <= rightPlotAreaPos
        && yPixel >= topPlotAreaPos && yPixel <= bottomPlotAreaPos) {

      isMouseReleased = true;

      double xPt, yPt;

      Coordinate coord = getCoordFromPoint(xPixel, yPixel);

      xPt = coord.getXVal();
      yPt = coord.getYVal();

      finalX = xPt;
      finalY = yPt;

      if (mousePressedInPlotArea
          && Math.abs(xPixel - initXpixel) > MIN_DRAG_X_PIXELS) {
        doZoom(initX, initY, finalX, finalY);
      }

    }

  }

  public void mouseDragged(MouseEvent e) {
    isMouseDraggedEvent = true;
    fireMouseDragged(e);
  }

  /**
   * Implements mouseMoved in interface MouseMotionListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mouseMoved(MouseEvent e) {
    isMouseDraggedEvent = false;
    fireMouseMoved(e);
    if (mouseMovedOk)
      repaint();
  }

  private double lastClickX = Double.MAX_VALUE;

  /**
   * Called by the mouseClicked Method
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  private void fireMouseClicked(MouseEvent e) {
    if (e.getButton() != MouseEvent.BUTTON1)
      return;

    int xPixel = e.getX();
    int yPixel = e.getY();

    if (xPixel >= leftPlotAreaPos && xPixel <= rightPlotAreaPos
        && yPixel >= topPlotAreaPos && yPixel <= bottomPlotAreaPos) {

      double xPt, yPt;

      Coordinate coord = getCoordFromPoint(xPixel, yPixel);

      lastClickX = xPt = coord.getXVal();
      yPt = coord.getYVal();

      String hashX = "#";
      String hashY = "#";
      String hash1 = "0.00000000";

      if (multiScaleData.hashNums[0] <= 0)
        hashX = hash1.substring(0, Math.abs(multiScaleData.hashNums[0]) + 3);

      DecimalFormat displayXFormatter = TextFormat.getDecimalFormat(hashX);

      if (multiScaleData.hashNums[1] <= 0)
        hashY = hash1.substring(0, Math.abs(multiScaleData.hashNums[1]) + 3);

      DecimalFormat displayYFormatter = TextFormat.getDecimalFormat(hashY);

      String xStr, yStr;

      xStr = displayXFormatter.format(xPt);
      yStr = displayYFormatter.format(yPt);

      coordClicked = new Coordinate(Double.parseDouble(xStr), Double
          .parseDouble(yStr));
      notifyPeakPickedListeners(coordClicked);
    } else
      coordClicked = null;
  }

  /**
   * Carries out the function of the MouseMoved Event
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  private void fireMouseMoved(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();

    int xPixel = x /*- getX()*/;
    int yPixel = y /*- getY()*/;

    if (xPixel >= leftPlotAreaPos && xPixel <= rightPlotAreaPos
        && yPixel >= topPlotAreaPos && yPixel <= bottomPlotAreaPos) {

      if (isMouseDraggedEvent) {
        isMouseDragged = true;
        currZoomBoxX = xPixel;
      }

      double xPt, yPt;

      Coordinate coord = getCoordFromPoint(xPixel, yPixel);

      xPt = coord.getXVal();
      yPt = coord.getYVal();

      String hashX = "#";
      String hashY = "#";
      String hash1 = "0.00000000";

      if (multiScaleData.hashNums[0] <= 0)
        hashX = hash1.substring(0, Math.abs(multiScaleData.hashNums[0]) + 3);

      DecimalFormat formatter = TextFormat.getDecimalFormat(hashX);

      if (multiScaleData.hashNums[1] <= 0)
        hashY = hash1.substring(0, Math.abs(multiScaleData.hashNums[1]) + 3);

      String xx = formatter.format(xPt);
      formatter = TextFormat.getDecimalFormat(hashY);
      coordStr = "(" + xx + ", " + formatter.format(yPt) + ")";

      if (nSpectra == 1) {
        if (!getSpectrum().isHNMR()) {
          yPt = spectra[0].getYValueAt(xPt);
          xx += ", " + formatter.format(yPt);
        }
      } else if (getSpectrum().getIntegrationGraph() != null) {
        formatter = TextFormat.getDecimalFormat("#0.0");
        yPt = spectra[1].getYValueAt(xPt);
        xx += ", " + formatter.format(yPt);
      }

      setToolTipText(Double.isNaN(yPt) ? null : xx);
    }
    mouseMovedOk = true;
  }

  /**
   * Carries out the function of the MouseDragged Event
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  private void fireMouseDragged(MouseEvent e) {
    isMouseDraggedEvent = true; // testing   
    fireMouseMoved(e);
    if (mouseMovedOk)
      repaint();
  }

  public void toPeak(int istep) {
    istep *= (shouldDrawXAxisIncreasing ? 1 : -1);
    coordClicked = new Coordinate(lastClickX, 0);
    JDXSpectrum spec = getSpectrum();
    int iPeak = spec.setNextPeak(coordClicked, istep);
    if (iPeak < 0)
      return;
    PeakInfo peak = spec.getPeakList().get(iPeak);
    spec.setSelectedPeak(peak);
    coordClicked.setXVal(lastClickX = peak.getX());
    notifyPeakPickedListeners(new PeakPickedEvent(this, coordClicked, peak
        .getStringInfo()));
  }

  public void keyPressed(KeyEvent e) {
    if (e.getModifiers() != 0)
      return;
    switch (e.getKeyCode()) {
    case KeyEvent.VK_LEFT:
      toPeak(-1);
      break;
    case KeyEvent.VK_RIGHT:
      toPeak(1);
      break;
    }
  }

  public void keyReleased(KeyEvent e) {
  }

  public void keyTyped(KeyEvent e) {
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
      setBackground(ds.getColor("background"));
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
  }

  public JDXSpectrum getSpectrum() {
    return getSpectrumAt(0);
  }

  public void setZoom(double x1, double x2) {
    Coordinate z0 = getCoordFromPoint(xPixels(x1), topPlotAreaPos);
    Coordinate z1 = getCoordFromPoint(xPixels(x2), bottomPlotAreaPos);
    multiScaleData = zoomInfoList.get(0);
    double initX = z0.getXVal();
    double initY = z0.getYVal();
    double finalX = z1.getXVal();
    double finalY = z1.getYVal();
    if (initX == 0 && finalX == 0)
      multiScaleData = zoomInfoList.get(0);
    else
      doZoom(initX, initY, finalX, finalY);
    z0 = z1 = null;
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

}
