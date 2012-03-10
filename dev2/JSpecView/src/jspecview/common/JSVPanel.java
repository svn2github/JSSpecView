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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

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
 * @author Bob Hanson hansonr@stolaf.edu
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
  
  private double userYFactor = 1;

  // List of Scale data for zoom
  private List<MultiScaleData> zoomInfoList;

  // Current index of view in zoomInfoList
  private int currentZoomIndex;

  // Determines if the xAxis should be displayed increasing
  private boolean xAxisLeftToRight = true;

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
    doZoomWithoutRepaint(initX, finalX, 0, 0, startIndices, endIndices);

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
    int[] startIndices = new int[nSpectra];
    int[] endIndices = new int[nSpectra];
    //boolean[] plotIncreasing = new boolean[numOfSpectra];

    // test if all have same x and y units
    checkUnits();

    allowYScale = true;
    for (int i = 0; i < nSpectra; i++) {
      startIndices[i] = 0;
      endIndices[i] = spectra[i].getXYCoords().length - 1;
      allowYScale &= (!spectra[i].getYUnits().equals(spectra[0].getYUnits()) 
          || spectra[i].getUserYFactor() != spectra[0].getUserYFactor());        
    }
    allowYScale = true;

    setPlotColors(Parameters.defaultPlotColors);

    getMultiScaleData(0, 0, 0, 0, startIndices, endIndices);
    //add data to zoomInfoList
    zoomInfoList = new ArrayList<MultiScaleData>();
    zoomInfoList.add(multiScaleData);

    setBorder(BorderFactory.createLineBorder(Color.lightGray));
  }

  private void getMultiScaleData(double x1, double x2, double y1, double y2,
                                 int[] startIndices, int[] endIndices) {
    List<JDXSpectrum> subspecs = getSpectrumAt(0).getSubSpectra();
    if (!getSpectrumAt(0).is1D() || subspecs == null && y1 == y2) {
      // 2D spectrum 
      multiScaleData = new MultiScaleData(spectra, y1, y2, startIndices,
          endIndices, 10, 10, getSpectrumAt(0).isContinuous());
      return;
    }
    if (y1 == y2) {
      //start up, forced subsets (too many spectra) 
      multiScaleData = new MultiScaleData(subspecs, y1, y2, 10, 10, getSpectrum()
          .isContinuous());
      return;
    }
    // from a zoom measurement
    multiScaleData = new MultiScaleData(graphsTemp, y1, y2, startIndices,
        endIndices, 10, 10, getSpectrumAt(0).isContinuous());
    multiScaleData.setXRange(x1, x2, 10);
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
    leftPlotAreaPos = plotAreaX;
    topPlotAreaPos = plotAreaY;
    bottomPlotAreaPos = plotAreaHeight + plotAreaY;
    rightPlotAreaPos = plotAreaWidth + plotAreaX;
    userYFactor = getSpectrum().getUserYFactor();
    setScaleFactors(multiScaleData);

    //widthRatio = (getSpectrumAt(0).is1D() ? 1 : 0.3);
    draw1D = true;
    if (!spec0.is1D()
        && display2D && draw2DImage(g, width, height)) {
      if (!draw1D)
        return;
      width = (int) Math.floor(widthRatio * width);
      plotAreaWidth = width - (plotAreaInsets.right + plotAreaInsets.left);
      rightPlotAreaPos = plotAreaWidth + plotAreaX;
      setScaleFactors(multiScaleData);
    }
    
    ///System.out.println("JSVPANEL "  + width + " " + height + " " + plotAreaWidth + " " + plotAreaHeight + " " + title);


    
    
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

    int subIndex = spec0.getSubIndex();
    if (subIndex >= 0 && !spec0.is1D()) {
      g.setColor(plotColors[0]);
      int y = ((int) (plotAreaHeight * 1.0 * (subIndex + 1) / getSpectrumAt(0).getSubSpectra().size()));
      g.fillRect(rightPlotAreaPos, bottomPlotAreaPos - y, 3, y);
      if (image2D != null) {
        g.drawLine(rightPlotAreaPos, bottomPlotAreaPos - y, getWidth() - 10, bottomPlotAreaPos - y);
        g.drawLine(xLine0,topPlotAreaPos - 10, xLine0, bottomPlotAreaPos + 10);
        g.drawLine(xLine1, bottomPlotAreaPos + 10, xLine1, topPlotAreaPos - 10);
        drawUnits(g, width, spec0.nucleusX, getWidth() - 30, bottomPlotAreaPos, 1, 1.0);
        drawUnits(g, width, spec0.nucleusY, bwidthLeft - 5, topPlotAreaPos, 1, 0);
      }
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
    for (int i = nSpectra; --i >= 0; )
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
    if (zoomEnabled)
      drawZoomBox(g);
    if (integrationRatios != null)
      drawAnnotations(g, height, width, integrationRatios, integralPlotColor);
    if (annotations != null)
      drawAnnotations(g, height, width, annotations, null);
    drawIntegralValue(g, width);
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

////    if (Logger.debugging) {
////      System.out.println("x1: " + x1);
////      System.out.println("x2: " + x2);
//    }

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
    return (int) (drawXAxisLeftToRight ? leftPlotAreaPos + x
        : rightPlotAreaPos - x);
  }

  private int yPixels(double yVal) {
    return (Double.isNaN(yVal) ? Integer.MIN_VALUE : invertY((int) (topPlotAreaPos + (yVal * userYFactor - minYScale)
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
    Coordinate[] xyCoords = spectra[index].getXYCoords();

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

    userYFactor = spectra[index].getUserYFactor();
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
        y1 = fixY(y1);
        y2 = fixY(y2);
        if (y1 == y2 && isOffScale(y1))
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
        y1 = fixY(y1);
        y2 = fixY(y2);
        if (y1 == y2 && isOffScale(y1))
          continue;
        g.drawLine(x1, y1, x1, y2);
      }
      if (multiScaleData.isYZeroOnScale()) {
        int y = yPixels(0);
        g.drawLine(rightPlotAreaPos, y, leftPlotAreaPos, y);
      }
    }
  } // End drawPlot

  private boolean isOffScale(int y) {
    return (y == topPlotAreaPos || y == bottomPlotAreaPos);
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

  private void drawIntegralValue(Graphics g, int width) {
    List<Integral> integrals = getSpectrum().getIntegrals();
    if (integrals == null)
      return;
    setFont(g, width, Font.BOLD, 12);
    FontMetrics fm = g.getFontMetrics();
    NumberFormat formatter = getFormatter("#0.0");
    g.setColor(integralPlotColor);

    for (int i = integrals.size(); --i >= 0;) {
      Integral in = integrals.get(i);
      if (in.value == 0)
        continue;
      String s = "  " + formatter.format(Math.abs(in.value));
      int x = xPixels(in.x2);
      int y1 = yPixels(in.y1);
      int y2 = yPixels(in.y2);
      g.drawLine(x, y1, x, y2);
      g.drawLine(x + 1, y1, x + 1, y2);
      g.drawString(s, x, (y1 + y2) / 2  + fm.getHeight() / 3);
    }
  }

  private Map<String, NumberFormat> htFormats = new Hashtable<String, NumberFormat>();

  private int zoomBoxY;

  private int currZoomBoxY;

  private Graph[] graphsTemp = new Graph[1];

  private double zPt;
  
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
    setFont(g, width, Font.PLAIN, 12);
    FontMetrics fm = g.getFontMetrics();
    int y1 = invertY((int) topPlotAreaPos);
    int y2 = invertY((int) (topPlotAreaPos - 3));
    double maxWidth = Math.abs((xPixels(multiScaleData.xStep) - xPixels(0)) * 0.95);
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
        int x = xPixels(val);
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

  private void setFont(Graphics g, int width, int mode, int size) {
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
    setFont(g, width, Font.PLAIN, 12); 
    FontMetrics fm = g.getFontMetrics();
    double max = multiScaleData.maxYOnScale + multiScaleData.yStep / 2;
    for (double val = multiScaleData.minYOnScale; val < max; val += multiScaleData.yStep) {
      int x1 = (int) leftPlotAreaPos;
      int y = yPixels(val * userYFactor);
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
        14); 
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
  private void drawXUnits(Graphics g, int width) {
    drawUnits(g, width, spectra[0].getXUnits(), rightPlotAreaPos, bottomPlotAreaPos, 1, 2.5);
  }

  private void drawUnits(Graphics g, int width, String s,
                         int x, int y, double hOff, double vOff) {
    g.setColor(unitsColor);
    setFont(g, width, Font.ITALIC, 10);
    FontMetrics fm = g.getFontMetrics();
    g.drawString(s, (int) (x - fm.stringWidth(s) * hOff),
        (int) (y + fm.getHeight() * vOff));
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
    setFont(g, width, Font.PLAIN, 12);
    g.drawString(coordStr, (int) ((plotAreaWidth + leftPlotAreaPos) * 0.85), 
        (int) (topPlotAreaPos - 10));
  }

  // determine whether there are any ratio annotations to draw
  private void drawAnnotations(Graphics g, int height, int width,
                               ArrayList<Annotation> annotations, Color color) {
    setFont(g, width, Font.BOLD, 12);
    for (int i = annotations.size(); --i >= 0;) {
      Annotation note = annotations.get(i);
      color = (note instanceof ColoredAnnotation ? ((ColoredAnnotation) note).getColor()
          : color);
      if (color == null)
        color = Color.BLACK;
      g.setColor(color);
      int x = xPixels(note.getXVal());
      int y = (note.isPixels() ? invertY((int) (bottomPlotAreaPos
          + note.getYVal() - 10)) : yPixels(note.getYVal()));
      g.drawString(note.getText(), x, y);
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
      g.fillRect(
          Math.min(zoomBoxX, currZoomBoxX), 
          Math.min(zoomBoxY, currZoomBoxY), 
          Math.abs(currZoomBoxX - zoomBoxX), 
          Math.abs(currZoomBoxY - zoomBoxY));
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

  private void setScaleFactors(MultiScaleData multiScaleData) {
    xFactorForScale = (multiScaleData.maxXOnScale - multiScaleData.minXOnScale)
        / plotAreaWidth;
    yFactorForScale = (multiScaleData.maxYOnScale - multiScaleData.minYOnScale)
        / plotAreaHeight;
    minYScale = multiScaleData.minYOnScale;
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

    zPt = Double.NaN;
    
    if (image2D != null && xPixel >= bwidthLeft - 5 && xPixel < getWidth() - 15) {

      xPt = 1.0 * (fixX2D(xPixel) - bwidthLeft) / imageWidthHeight[0];
      //System.out.println("getCOord shoulddrawXInc " + shouldDrawXAxisIncreasing);
      //if (shouldDrawXAxisIncreasing)
        xPt = 1 - xPt;
      Coordinate[] xyCoords = getSpectrum().getXYCoords();
      xPt = xPt * (xyCoords[xyCoords.length - 1].getXVal() - xyCoords[0].getXVal()) + xyCoords[0].getXVal(); 
      int n = getSpectrumAt(0).getSubSpectra().size();
      int y = Math.min(n - 1, Math.max(0, (int)((bottomPlotAreaPos - yPixel) * 1.0 / imageWidthHeight[1] * n)));
      yPt = y;
      JDXSpectrum spec = getSpectrumAt(0).getSubSpectra().get(y);
      //zPt = spec.getYValueAt(spec.getXYCoords(), xPt);
      zPt = spec.getY2D();
    } else {
      xPixel = fixX(xPixel);
      yPixel = fixY(yPixel);
      xPt = (drawXAxisLeftToRight 
         ?  multiScaleData.maxXOnScale - (rightPlotAreaPos - xPixel) * xFactorForScale 
         :  multiScaleData.minXOnScale + (rightPlotAreaPos - xPixel) * xFactorForScale);
      yPt = multiScaleData.maxYOnScale + (topPlotAreaPos - yPixel) * yFactorForScale;
    }
    
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
  private void doZoom(double initX, double initY, double finalX, double finalY, boolean isMouseEvent) {
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
    if (doZoomWithoutRepaint(initX, finalX, initY, finalY, startIndices,
        endIndices)) {
      notifyZoomListeners(initX, finalX, initY, finalY);
      if (isMouseEvent)
        repaint();
    }
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
                                       double yPt1, double yPt2,
                                       int[] startIndices, int[] endIndices) {
    if (!zoomEnabled)
      return false;
    if (getSpectrumAt(0).is1D()) {
      graphsTemp[0] = getSpectrum();
      if (!multiScaleData.setDataPointIndices(graphsTemp, initX, finalX,
          minNumOfPointsForZoom, startIndices, endIndices, false))
        return false;
    } else {
      if (!multiScaleData.setDataPointIndices(spectra, initX, finalX,
          minNumOfPointsForZoom, startIndices, endIndices, true))
        return false;
    }
    getMultiScaleData(initX, finalX, yPt1, yPt2, startIndices, endIndices);
    // add to and clean the zoom list
    if (zoomInfoList.size() > currentZoomIndex + 1)
      for (int i = zoomInfoList.size() - 1; i > currentZoomIndex; i--)
        zoomInfoList.remove(i);
    zoomInfoList.add(multiScaleData);
    currentZoomIndex++;
    clear2DImage();
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
    // leave first zoom
    for (int i = zoomInfoList.size(); --i >= 1; ) 
      zoomInfoList.remove(i);
  }

  /**
   * Displays the previous view zoomed
   */
  public void previousView() {
    if (currentZoomIndex > 0) {
      multiScaleData = zoomInfoList.get(--currentZoomIndex);
      bwidth = 0;
      repaint();
    }
  }

  /**
   * Displays the next view zoomed
   */
  public void nextView() {
    if (currentZoomIndex + 1 < zoomInfoList.size()) {
      multiScaleData = zoomInfoList.get(++currentZoomIndex);
      bwidth = 0;
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
   * @param mode
   *        the format to export in
   * @param index
   *        the index of the spectrum
   * @param recentFileName
   * @param dirLastExported
   * @return dirLastExported
   */
  public String exportSpectrum(JDXSpectrum spec, JFileChooser fc, String mode,
                               int index, String recentFileName,
                               String dirLastExported) {
    JSpecViewFileFilter filter = new JSpecViewFileFilter();
    //TODO: This is flawed. It assumes the file name has one and only one "." in it. 
    String name = TextFormat.split(recentFileName, ".")[0];
    if ("XY FIX PAC SQZ DIF DIFDUP".indexOf(mode) >= 0) {
      filter.addExtension("jdx");
      filter.addExtension("dx");
      filter.setDescription("JCAMP-DX Files");
      name += ".jdx";
    } else {
      if (mode.toLowerCase().indexOf("iml") >= 0
          || mode.toLowerCase().indexOf("aml") >= 0)
        mode = "XML";
      filter.addExtension(mode);
      filter.setDescription(mode + " Files");
      name += "." + mode.toLowerCase();
    }
    fc.setFileFilter(filter);
    fc.setSelectedFile(new File(name));
    int returnVal = fc.showSaveDialog(this);
    if (returnVal != JFileChooser.APPROVE_OPTION)
      return dirLastExported;
    File file = fc.getSelectedFile();
    this.dirLastExported = file.getParent();
    if (file.exists()) {
      int option = JOptionPane.showConfirmDialog(this, "Overwrite file?",
          "Confirm Overwrite Existing File", JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);
      if (option == JOptionPane.NO_OPTION)
        return exportSpectrum(spec, fc, mode, index, recentFileName,
            this.dirLastExported);
    }
    export(mode, file, spec, getStartDataPointIndices()[index], getEndDataPointIndices()[index]);
    return this.dirLastExported;
  }

  /**
   * from EXPORT command
   * 
   * @param tokens
   * 
   * @return message for status line
   */
  public String export(List<String> tokens) {
    String mode = "XY";
    String fileName = null;
    switch (tokens.size()) {
    default:
      return "EXPORT what?";
    case 1:
      fileName = TextFormat.trimQuotes(tokens.get(0));
      int pt = fileName.indexOf(".");
      if (pt < 0)
        return "EXPORT mode?";
      break;
    case 2:
      mode = tokens.get(0).toUpperCase();
      fileName = TextFormat.trimQuotes(tokens.get(1));
      break;
    }
    String ext = fileName.substring(fileName.lastIndexOf(".") + 1)
        .toUpperCase();
    if (ext.equals("JDX")) {
      if (mode == null)
        mode = "XY";
    } else if (Exporter.isExportMode(ext)) {
      mode = ext;
    } else if (Exporter.isExportMode(mode)){
      fileName += "."  + mode;
    }
    return export(mode, new File(fileName), getSpectrum(),
        getStartDataPointIndices()[0], getEndDataPointIndices()[0]);
  }
    
  private String export(String mode, File file, JDXSpectrum spec, int startIndex, int endIndex) {
    String msg = " OK";
    try {
      if (mode.equals("PNG") || mode.equals("JPG")) {
        try {
          Image image = createImage(getWidth(), getHeight());
          paint(image.getGraphics());
          ImageIO.write((RenderedImage) image, mode.toLowerCase(), new File(file
              .getAbsolutePath()));
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      } else if (mode.equals("SVG")) {
        msg = (new SVGExporter()).exportAsSVG(file.getAbsolutePath(), spec.getXYCoords(), spec.getTitle(), startIndex,
            endIndex, spec.getXUnits(), spec.getYUnits(), spec.isContinuous(),
            spec.isIncreasing(), getPlotAreaColor(), getBackground(),
            getPlotColor(0), getGridColor(), getTitleColor(), 
            getScaleColor(), getUnitsColor(), isSvgExportForInkscapeEnabled());
      } else {
        msg = Exporter.export(mode, file.getAbsolutePath(), spec, startIndex,
            endIndex);
      }
      return "Exported " + mode + ": " + file.getAbsolutePath() + msg;
    } catch (IOException ioe) {
      return "Error exporting " + file.getAbsolutePath() + ": " + ioe.getMessage();
    }
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
    addMenuItem(saveAsJDXMenu, "XY", actionListener);
    addMenuItem(saveAsJDXMenu, "DIF", actionListener);
    addMenuItem(saveAsJDXMenu, "DIFDUP", actionListener);
    addMenuItem(saveAsJDXMenu, "FIX", actionListener);
    addMenuItem(saveAsJDXMenu, "PAC", actionListener);
    addMenuItem(saveAsJDXMenu, "SQZ", actionListener);
    saveAsMenu.add(saveAsJDXMenu);
    addMenuItem(saveAsMenu, "CML", actionListener);
    addMenuItem(saveAsMenu, "XML (AnIML)", actionListener);
    if (exportAsMenu != null) {
      exportAsMenu.setText("Export As");
      addMenuItem(exportAsMenu, "JPG", actionListener);
      addMenuItem(exportAsMenu, "PNG", actionListener);
      addMenuItem(exportAsMenu, "SVG", actionListener);
    }
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
    // TODO: Currently Aassumes spectra are not overlayed
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

  /**
   * Implements mouseClicked in interface MouseListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mouseClicked(MouseEvent e) {
    requestFocusInWindow();
    if (e.isControlDown())
      clearIntegrals();
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

  
  int mouseClickCount;
  
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
    if (e.getButton() != MouseEvent.BUTTON1)
      return;

    int xPixel = e.getX();
    int yPixel = fixY(e.getY());

    if (checkImageDrag(xPixel, yPixel, true))
      return;

    xPixel = fixX(xPixel);

    mouseClickCount = e.getClickCount();
    isIntegralDrag = (e.isControlDown() && getSpectrum().getIntegrationGraph() != null);

    zoomBoxX = xPixel;
    zoomBoxY = (isIntegralDrag ? topPlotAreaPos : yPixel);

    Coordinate coord = getCoordFromPoint(xPixel, yPixel);

    initX = coord.getXVal();
    initY = coord.getYVal();
    initXpixel = xPixel;
    repaint();
  }

  
  boolean triggerNewImage2DZoom;
  boolean isLine0, isLine1;
  
  private boolean checkImageDrag(int xPixel, int yPixel, boolean isPress) {
    if (image2D == null || xPixel < rightPlotAreaPos + 10)
      return false;
    if (isPress) {
      isLine0 = (Math.abs(xPixel - xLine0) < 5);
      isLine1 = (!isLine0 && Math.abs(xPixel - xLine1) < 5);
    }
    if (isLine0 || isLine1) {
      if (isLine0)
        xLine0 = fixX2D(xPixel);
      else if (isLine1)
        xLine1 = fixX2D(xPixel);
      triggerNewImage2DZoom = true;
      isMouseDraggedEvent = true;
      repaint();
      return true;
    }

    int iSpec = (int) (1.0 * (bottomPlotAreaPos - yPixel) / plotAreaHeight * getSpectrumAt(
        0).getSubSpectra().size());
    getSpectrumAt(0).setCurrentSubSpectrum(iSpec);
    repaint();
    isMouseDraggedEvent = false;
    return true;
  }

  private int fixX2D(int xPixel) {
    return (xPixel < bwidthLeft ? bwidthLeft
        : xPixel >= bwidthLeft + imageWidthHeight[0] ? xPixel = bwidthLeft + imageWidthHeight[0] - 1
            : xPixel);
  }

  
  /**
   * Implements mouseReleased in interface MouseListener
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  public void mouseReleased(MouseEvent e) {
    // Maybe use a fireMouseReleased Method

    if (e.getButton() != MouseEvent.BUTTON1 || !isMouseDraggedEvent)
      return;

    isMouseReleased = true;

    int xPixel = fixX(e.getX());
    int yPixel = fixY(e.getY());

    if (triggerNewImage2DZoom) {
      triggerNewImage2DZoom = false;
      clearViews();
      doZoom(getX2DFromPixel(xLine1), multiScaleData.minY, getX2DFromPixel(xLine0), multiScaleData.maxY, true);
      return;
    }
    setMouseFinalXY(xPixel, yPixel);
    if (Math.abs(xPixel - initXpixel) <= MIN_DRAG_X_PIXELS)
      return;
    if (isIntegralDrag)
      checkIntegral(initX, finalX, true);
    else
      doZoom(initX, initY, finalX, finalY, true);
  }

  private double getX2DFromPixel(int x) {
    return getCoordFromPoint(x, 0).getXVal();
  }

  private boolean checkXY(int xPixel, int yPixel) {
    return (xPixel >= leftPlotAreaPos && xPixel <= rightPlotAreaPos
        && yPixel >= topPlotAreaPos && yPixel <= bottomPlotAreaPos);
  }

  private void setMouseFinalXY(int x, int y) {
    Coordinate coord = getCoordFromPoint(x, y);
    finalX = coord.getXVal();
    finalY = coord.getYVal();
  }

  private void clearIntegrals() {
    checkIntegral(Double.NaN, 0, false);
  }
  
  private void checkIntegral(double x1, double x2, boolean isFinal) {
    IntegralGraph ig = getSpectrum().getIntegrationGraph();
    if (ig == null)
      return;
    ig.addIntegral(x1, x2, isFinal);
    repaint();
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
    repaint();
  }

  private double lastClickX = Double.MAX_VALUE;

  private boolean isIntegralDrag;

  /**
   * Called by the mouseClicked Method
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  private void fireMouseClicked(MouseEvent e) {
    int xPixel = e.getX();
    int yPixel = e.getY();
    if (!checkXY(xPixel, yPixel)) {
      coordClicked = null;
      return;
    }
    coordClicked = getCoordFromPoint(xPixel, yPixel);
    lastClickX = coordClicked.getXVal();
    notifyPeakPickedListeners(coordClicked);
  }

  /**
   * Carries out the function of the MouseMoved Event
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  private void fireMouseMoved(MouseEvent e) {
    int xPixel = fixX(e.getX());
    int yPixel = fixY(e.getY());

    if (isMouseDraggedEvent) {
      isMouseDragged = true;
      currZoomBoxX = xPixel;
      currZoomBoxY = (isIntegralDrag ? bottomPlotAreaPos : yPixel);
    }

    Coordinate coord = getCoordFromPoint(e.getX(), e.getY());

    double xPt = coord.getXVal();
    double yPt = coord.getYVal();
    //if (image2D != null && e.getX() > bwidthLeft)
      //yPt = zPt;

    String hashX = "#";
    String hashY = "#";
    String hash1 = "0.00000000";

    if (multiScaleData.hashNums[0] <= 0)
      hashX = hash1.substring(0, Math.abs(multiScaleData.hashNums[0]) + 3);
    String xx = getFormatter(hashX).format(xPt);

    if (multiScaleData.hashNums[1] <= 0)
      hashY = hash1.substring(0, Math.abs(multiScaleData.hashNums[1]) + 3);
    NumberFormat formatter = getFormatter(hashY);
    coordStr = "(" + xx + ", " + formatter.format(yPt) + ")";

    if (nSpectra == 1) {
      /*if (yPt == zPt) {
        xx += ", " + formatter.format(yPt);        
      } else */if (!getSpectrum().isHNMR()) {
        yPt = spectra[0].getPercentYValueAt(xPt);
        xx += ", " + formatter.format(yPt);
      }
    } else if (getSpectrum().getIntegrationGraph() != null) {
      yPt = spectra[1].getPercentYValueAt(xPt);
      xx += ", " + getFormatter("#0.0").format(yPt);
    }
    setToolTipText(Double.isNaN(yPt) ? null : xx);
  }

  /**
   * Carries out the function of the MouseDragged Event
   * 
   * @param e
   *        the <code>MouseEvent</code>
   */
  private void fireMouseDragged(MouseEvent e) {

    int xPixel = e.getX();
    int yPixel = fixY(e.getY());

    if (checkImageDrag(xPixel, yPixel, false))
      return;
    
    isMouseDraggedEvent = true; // testing   
    fireMouseMoved(e);
    if (isIntegralDrag) {
      setMouseFinalXY(fixX(xPixel), yPixel);
      checkIntegral(initX, finalX, false);      
      return;
    }
    repaint();
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

  public void advanceSubSpectrum(int i) {
    if (getSpectrumAt(0).advanceSubSpectrum(i))
      multiScaleData.setXRange(getSpectrum());
  }

  public void setCurrentSubSpectrum(int i) {
    if (getSpectrumAt(0).setCurrentSubSpectrum(i))
      multiScaleData.setXRange(getSpectrum());
  }
  
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
    doZoom(multiScaleData.minX, multiScaleData.minY / factor1, 
        multiScaleData.maxX, multiScaleData.maxY / factor2, true);
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
  }

  public JDXSpectrum getSpectrum() {
    return getSpectrumAt(0).getCurrentSubSpectrum();
  }

  public void setZoom(double x1, double y1, double x2, double y2) {
    multiScaleData = zoomInfoList.get(0);
    if (Double.isNaN(x1)) {
      x1 = multiScaleData.minX;
      x2 = multiScaleData.maxX;
    }
    if (x1 != 0 || x2 != 0)
      doZoom(x1, y1, x2, y2, false);
    else
      notifyZoomListeners(0, 0, 0, 0);
  }

  private int fixY(int yPixels) {
    return Math.max(Math.min(yPixels, bottomPlotAreaPos), topPlotAreaPos);
  }

  private int fixX(int xPixels) {
    return Math.max(Math.min(xPixels, rightPlotAreaPos), leftPlotAreaPos);
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
  
  private BufferedImage image2D;
  private int bwidth, bheight;
  private int bwidthLeft;
  private boolean draw1D = true;
  
  private void clear2DImage() {
    bwidth = 0;
  }
  
  private int xLine0, xLine1;
  private int[] imageWidthHeight = new int[2];
  private boolean draw2DImage(Graphics g, int width, int height) {
    //widthRatio = 1;
    if (bwidth != width || bheight != plotAreaHeight) {
      int[] buffer = getSpectrumAt(0).get2dBuffer(bwidth = width, bheight = plotAreaHeight, imageWidthHeight, multiScaleData.minY, multiScaleData.maxY);
      if (buffer == null) {
        image2D = null;
        draw1D = true;
        return false;
      }
      image2D = new BufferedImage(imageWidthHeight[0], imageWidthHeight[1], BufferedImage.TYPE_BYTE_GRAY);
      WritableRaster raster = image2D.getRaster();
      raster.setSamples(0, 0, imageWidthHeight[0], imageWidthHeight[1], 0, buffer);
      widthRatio = 1.0 * (width - imageWidthHeight[0]) / width;
      bwidthLeft = (int) Math.floor(bwidth - imageWidthHeight[0]) - 20;
      xLine0 = bwidthLeft + (int) (imageWidthHeight[0] *(1 - 1.0 *  multiScaleData.startDataPointIndices[0]/ getSpectrum().getXYCoords().length));
      xLine1 = bwidthLeft + (int) (imageWidthHeight[0] *(1 - 1.0 *  multiScaleData.endDataPointIndices[0]/ getSpectrum().getXYCoords().length));
      draw1D = true;// || (widthRatio >= widthRatioMin);
    }
    if (image2D != null) {
      g.drawImage(image2D, bwidthLeft, topPlotAreaPos, null);
    }
    return true;    
  }
}
