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


// CHANGES to 'JSVPanel.java'
// University of the West Indies, Mona Campus
//
// 25-06-2007 rjl - bug in ReversePlot for non-continuous spectra fixed
//                - previously, one point less than npoints was displayed
// 25-06-2007 cw  - show/hide/close modified

package jspecview.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
//import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jspecview.common.JSpecViewUtils.MultiScaleData;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.export.Exporter;
import jspecview.export.SVGExporter;
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
 * @see jspecview.common.Graph
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class JSVPanel extends JPanel implements Printable, MouseListener, MouseMotionListener{

  // The list of spectra
  private Graph[] spectra;

  // The Number of Spectra
  private int numOfSpectra;

  // Contains information needed to draw spectra
  private JSpecViewUtils.MultiScaleData scaleData;

  // The list of Coordinate arrays
  private Coordinate[][] xyCoordsList;

  // Default Listeners
  private MouseListener defaultMouseListener;
  private MouseMotionListener defaultMouseMotionListener;

  // width and height of the JSVPanel
  //protected int width, height;

  //The postion of the plot area
  protected int plotAreaX = 80, plotAreaY = 30;

    // insets of the plot area
  protected Insets plotAreaInsets = new Insets(plotAreaY, plotAreaX, 50, 50);

  // width and height of the plot area
  protected int plotAreaWidth, plotAreaHeight;

  // Postions of the borders of the plotArea
  protected int leftPlotAreaPos, rightPlotAreaPos, topPlotAreaPos, bottomPlotAreaPos;

  // Enables or disables zoom
  protected boolean zoomEnabled = true;

  // turns on/off elements of the JSVPanel
  protected boolean gridOn = false;
  protected boolean coordsOn = true;
  protected boolean highlightOn = false;
  protected boolean titleOn = true;
  protected boolean scaleOn = true;
  protected boolean unitsOn = true;


  // highlight range
  protected double hlStart, hlEnd;

  protected Color highlightColor = new Color(255, 255, 0, 100);

  Vector<Highlight> highlights = new Vector<Highlight>();

  // The Current Coordinate to be drawn on the Panel
  private String coordStr = "";

  //private String coordClickedStr = null;
  private Coordinate coordClicked;

  // Is true if plot is reversed
  protected boolean plotReversed;


  // background color of plot area
  protected Color plotAreaColor = Color.white;

  //plot line color
  protected Color[] plotColors = {Color.blue, Color.green,
                                  Color.yellow, Color.orange,
                                  Color.red, Color.magenta,
                                  Color.pink, Color.cyan,
                                  Color.darkGray
                                  };

  // integral Color
  protected Color integralPlotColor = Color.red;

  // scale color
  protected Color scaleColor = Color.black;

  // titleColor
  private Color titleColor = Color.black;

  // units Color
  protected Color unitsColor = Color.red;

  // grid Color
  protected Color gridColor = Color.gray;

  // coordinate Color
  protected Color coordinatesColor = Color.red;

  protected Color zoomBoxColor = Color.gray;

  private boolean isMousePressed, isMouseDragged, isMouseReleased;
  private int zoomBoxX, currZoomBoxX;
  private boolean isMouseDraggedEvent;

  // whether to draw an overlayed plot increasing or decreasing
  private boolean overlayIncreasing = false;

  // the file path
  String filename = null;


  /* PUT FONT FACE AND SIZE ATTRIBUTES HERE */
  String fontName = null;

  // The scale factors
  protected double xFactorForScale, yFactorForScale;

  // List of Scale data for zoom
  protected Vector<MultiScaleData> zoomInfoList;

  // Current index of view in zoomInfoList
  protected int currentZoomIndex;

  // Used to identify whether the mouse was pressed within
  // the plot area;
  private boolean mousePressedInPlotArea;

  private boolean mouseMovedOk = false;

  // The initial coordinate and final coordinates of zoom area
  private double initX, initY, finalX, finalY;

  // Minimum number of points that is displayable in a zoom
  private int minNumOfPointsForZoom = 3;

  private String title = null;

  private boolean isPrinting = false;
  private boolean printGrid = gridOn;
  private boolean printScale = true;
  private boolean printTitle = true;
  private String printingFont = null;
  private String graphPosition = "default";
  private int defaultHeight = 450;
  private int defaultWidth = 280;


  //private MediaSize printMediaSize;
//  private double letterSize_width = 8.5;
//  private double letterSize_length = 11.0;
//  private int letter_pix_length = 648;
//  private int letter_pix_width = 468;

  public JSVPanel() {
    setDefaultMouseListener(this);
    setDefaultMouseMotionListener(this);
  }

  /**
   * Constructs a new JSVPanel
   * @param spectrum the spectrum
   * @throws ScalesIncompatibleException 
   */
  public JSVPanel(Graph spectrum) throws ScalesIncompatibleException {
    super();
    setDefaultMouseListener(this);
    setDefaultMouseMotionListener(this);
    initJSVPanel(new Graph[]{spectrum});
  }

  /**
   * Constructs a <code>JSVPanel</code> with a single <code>Spectrum</code>
   * the segment specified by startindex and endindex displayed
   * @param spectrum the spectrum
   * @param startIndex the index of coordinate at which the display should start
   * @param endIndex the index of the end coordinate
   * @throws JSpecViewException
   */
  public JSVPanel(Graph spectrum, int startIndex, int endIndex) throws JSpecViewException{
    super();
    setDefaultMouseListener(this);
    setDefaultMouseMotionListener(this);
    try{
      initJSVPanel(new Graph[]{spectrum}, new int[]{startIndex}, new int[]{endIndex});
    }
    catch(ScalesIncompatibleException sie){
    }
  }

  /**
   * Contructs a JSVPanel with an array of Spectra
   * @param spectra an array of spectra (<code>Spectrum</code>)
   * @throws ScalesIncompatibleException
   */
  public JSVPanel(Graph[] spectra)throws ScalesIncompatibleException{
    super();
    setDefaultMouseListener(this);
    setDefaultMouseMotionListener(this);
    initJSVPanel(spectra);
  }

  /**
   * Constructs a <code>JSVPanel</code> with an array of spectra and corresponding
   * start and end indices of data points that should be displayed
   * @param spectra the array of spectra
   * @param startIndices the indices of coordinates at which the display should start
   * @param endIndices the indices of the end coordinates
   * @throws JSpecViewException
   * @throws ScalesIncompatibleException
   */
  public JSVPanel(Graph[] spectra, int[] startIndices, int[] endIndices) throws JSpecViewException, ScalesIncompatibleException{
    super();
    setDefaultMouseListener(this);
    setDefaultMouseMotionListener(this);
    initJSVPanel(spectra, startIndices, endIndices);
  }

  /**
   * Contructs a JSVMultiPanel with a vector of Spectra
   * @param spectra a <code>Vector</code> of spectra
   * @throws ScalesIncompatibleException
   */
  public JSVPanel(Vector<JDXSpectrum> spectra)throws ScalesIncompatibleException{
    this((Graph[])spectra.toArray(new Graph[spectra.size()]));
  }

 /**
  * Constructs a <code>JSVPanel</code> with vector of spectra and corresponding
  * start and end indices of data points that should be displayed
  * @param spectra the vector of <code>Graph</code> instances
  * @param startIndices the start indices
  * @param endIndices the end indices
  * @throws JSpecViewException
  * @throws ScalesIncompatibleException
  */
  public JSVPanel(Vector<JDXSpectrum> spectra, int[] startIndices, int[] endIndices) throws JSpecViewException, ScalesIncompatibleException{
    this((Graph[])spectra.toArray(new Graph[spectra.size()]), startIndices, endIndices);
  }

  /**
   * Initises the JSVPanel
   * @param spectra the array of spectra
   * @param startIndices the index of the start data point
   * @param endIndices the index of the end data point
   * @throws JSpecViewException
   * @throws ScalesIncompatibleException
   */
  private void initJSVPanel(Graph[] spectra, int[] startIndices, int[] endIndices) throws JSpecViewException, ScalesIncompatibleException{
    this.spectra = spectra;
    numOfSpectra = spectra.length;

    checkUnits();

    xyCoordsList = new Coordinate[numOfSpectra][];

    for(int i = 0; i < numOfSpectra; i++){
      xyCoordsList[i] = spectra[i].getXYCoords();
    }

    if(numOfSpectra > plotColors.length){
      Color[] tmpPlotColors = new Color[numOfSpectra];
      int numAdditionColors = numOfSpectra - plotColors.length;
      System.arraycopy(plotColors, 0, tmpPlotColors, 0, plotColors.length);

      for(int i = 0, j = plotColors.length; i < numAdditionColors; i++, j++){
        tmpPlotColors[j] = generateRandomColor();
      }

      plotColors = tmpPlotColors;
    }

    zoomInfoList = new Vector<MultiScaleData>();
    doZoomWithoutRepaint(startIndices, endIndices);

    setBorder(BorderFactory.createLineBorder(Color.lightGray));
  }
// throw exception when units are not the same on both axes

  /**
   * Intialises the JSVPanel
   * @param spectra the array of spectra
   * @throws ScalesIncompatibleException
   */
  private void initJSVPanel(Graph[] spectra)throws ScalesIncompatibleException{
    this.spectra = spectra;
    numOfSpectra = spectra.length;

    xyCoordsList = new Coordinate[numOfSpectra][];

    int[] startIndices = new int[numOfSpectra];
    int[] endIndices = new int[numOfSpectra];
    //boolean[] plotIncreasing = new boolean[numOfSpectra];

    // test if all have same x and y units
    checkUnits();


    for(int i = 0; i < numOfSpectra; i++){
      xyCoordsList[i] = spectra[i].getXYCoords();
      startIndices[i] = 0;
      endIndices[i] = xyCoordsList[i].length - 1;
    }

    if(numOfSpectra > plotColors.length){
      Color[] tmpPlotColors = new Color[numOfSpectra];
      int numAdditionColors = numOfSpectra - plotColors.length;
      System.arraycopy(plotColors, 0, tmpPlotColors, 0, plotColors.length);

      for(int i = 0, j = plotColors.length; i < numAdditionColors; i++, j++){
        tmpPlotColors[j] = generateRandomColor();
      }

      plotColors = tmpPlotColors;
    }

    scaleData = JSpecViewUtils.generateScaleData(xyCoordsList, startIndices, endIndices, 10, 10);

    //add data to zoomInfoList
    zoomInfoList = new Vector<MultiScaleData>();
    zoomInfoList.addElement(scaleData);

    setBorder(BorderFactory.createLineBorder(Color.lightGray));
  }

  /**
   *
   * @throws ScalesIncompatibleException
   */
  private void checkUnits() throws ScalesIncompatibleException{
    String xUnit = spectra[0].getXUnits();
    String yUnit = spectra[0].getYUnits();

    for(int i = 1; i < numOfSpectra; i++){
        String tempXUnit, tempYUnit;
        tempXUnit = spectra[i].getXUnits();
        tempYUnit = spectra[i].getYUnits();
        if(!xUnit.equals(tempXUnit) ||
           !yUnit.equals(tempYUnit)){
          throw new ScalesIncompatibleException();
        }
        xUnit = tempXUnit;
        yUnit = tempYUnit;
    }
  }

  /**
   * Returns a randomly generated <code>Color</code>
   * @return a randomly generated <code>Color</code>
   */
  private Color generateRandomColor(){
    int red = (int)(Math.random() * 255);
    int green = (int)(Math.random() * 255);
    int blue = (int)(Math.random() * 255);

    Color randomColor = new Color(red, green, blue);

    if(!randomColor.equals(Color.blue))
      return randomColor;
    return generateRandomColor();
  }

  /* ------------------------------LISTENER METHODS-------------------------*/

  /**
   * Sets default MouseListener
   * @param listener the <code>MouseListener</code>
   */
  public void setDefaultMouseListener(MouseListener listener){
    if (defaultMouseListener != null)
      removeMouseListener(defaultMouseListener);
    addMouseListener(listener);
    defaultMouseListener = listener;
  }

  /**
   * Sets default MouseMotionListener
   * @param listener the <code>MouseMotionListener</code>
   */
  public void setDefaultMouseMotionListener(MouseMotionListener listener){
    if (defaultMouseMotionListener != null)
      removeMouseMotionListener(defaultMouseMotionListener);
    addMouseMotionListener(listener);
    defaultMouseMotionListener = listener;
  }

  /**
   * Removes default MouseListener
   */
  public void removeDefaultMouseListener(){
    if (defaultMouseListener != null)
      removeMouseListener(defaultMouseListener);
  }

  /**
   * Removes default MouseMotionListener
   */
  public void removeDefaultMouseMotionListener(){
    if (defaultMouseMotionListener != null)
      removeMouseMotionListener(defaultMouseMotionListener);
  }


  /* ------------------------- SETTER METHODS-------------------------------*/
  /**
   * Sets the insets of the plot area
   * @param top top inset
   * @param left left inset
   * @param bottom bottom inset
   * @param right right inset
   */
  public void setPlotAreaInsets(int top, int left, int bottom, int right){
    plotAreaX = left;
    plotAreaY = top;
    plotAreaInsets = new Insets(top, left, bottom, right);
  }

  /**
   * Sets the plot area insets
   * @param insets the insets of the plot area
   */
  public void setPlotAreaInsets(Insets insets){
    plotAreaX = insets.left;
    plotAreaY = insets.top;
    plotAreaInsets = insets;
  }

  /**
   * Sets the zoom enabled or disabled
   * @param val true or false
   */
  public void setZoomEnabled(boolean val){
    zoomEnabled = val;
  }

  /**
   * Displays grid if val is true
   * @param val true or false
   */
  public void setGridOn(boolean val){
    gridOn = val;
  }

  /**
   * Displays Coordinates if val is true
   * @param val true or false
   */
  public void setCoordinatesOn(boolean val){
    coordsOn = val;
  }

  /**
   * Allows the scale to be displayed or not
   * @param val true if scale should be displayed, false otherwise
   */
  public void setScaleOn(boolean val){
    scaleOn = val;
  }

  /**
   * Allows the units are to be displayed or not
   * @param val true if scale should be displayed, false otherwise
   */
  public void setUnitsOn(boolean val){
    unitsOn = val;
  }

  /**
   * Displays plot in reverse if val is true
   * @param val true or false
   */
  public void setReversePlot(boolean val){
    plotReversed = val;
  }

  /**
   * Sets the Minimum number of points that may be displayed when the spectrum
   * is zoomed
   * @param num the number of points
   */
   public void setMinNumOfPointsForZoom(int num){
      minNumOfPointsForZoom = (num >= minNumOfPointsForZoom) ? num : minNumOfPointsForZoom;
   }

   /**
    * Sets the color of the title displayed
    * @param color the color
    */
   public void setTitleColor(Color color){
    titleColor = color;
   }

   /**
    * Sets the color of the plotArea;
    * @param color the color
    */
   public void setPlotAreaColor(Color color){
    plotAreaColor = color;
   }

   /**
    * Sets the color of the plot
    * @param color the color
    */
  public void setPlotColor(Color color){
    plotColors[7] = color;
  }

  /**
     * Sets the color of the integral plot
     * @param color the color
    */
  public void setIntegralPlotColor(Color color) {
    integralPlotColor = color;
  }

  /**
   * Sets the color of the plots
   * @param colors an array of <code>Color</code>
   */
  public void setPlotColors(Color[] colors){
    Color[] tmpPlotColors;
    if(colors.length < numOfSpectra){
      tmpPlotColors = new Color[numOfSpectra];
      int numAdditionColors = numOfSpectra - colors.length;
      System.arraycopy(colors, 0, tmpPlotColors, 0, colors.length);

      for(int i = 0, j = colors.length; i < numAdditionColors; i++, j++){
        tmpPlotColors[j] = generateRandomColor();
      }

      plotColors = tmpPlotColors;
    }
    else if(colors.length > numOfSpectra){
      plotColors = new Color[numOfSpectra];
      System.arraycopy(colors, 0, plotColors, 0, numOfSpectra);
    }
    else
      plotColors = colors;

  }

  /**
   * Sets the color of the scale
   * @param color the color
   */
  public void setScaleColor(Color color){
    scaleColor = color;
  }

  /**
   * Sets the color of the units
   * @param color the color
   */
  public void setUnitsColor(Color color){
    unitsColor = color;
  }

  /**
   * Sets the color of the grid
   * @param color the color
   */
  public void setGridColor(Color color){
    gridColor = color;
  }

  /**
   * Sets the title that will be displayed on the panel
   * @param title the title that will be diplayed on the panel
   */
  public void setTitle(String title){
    this.title = title;
  }

  /**
   * Sets the color of the Coodinates
   * @param color the color
   */
  public void setcoordinatesColor(Color color){
    coordinatesColor = color;
  }


  /**
   * Sets highlighting enabled or disabled
   * @param val true or false
   */
  public void setHighlightOn(boolean val){
    highlightOn = val;
  }

  /**
   * Allows the title to be displayed or not
   * @param val true if scale should be displayed, false otherwise
   */
  public void setTitleOn(boolean val){
    titleOn = val;
  }


  /**
   * Sets the display font name
   * @param fontName the name of the display font
   */
  public void setDisplayFontName(String fontName){
    this.fontName = fontName;
  }

  /* -------------------Other methods ------------------------------------*/

  /**
   * Add information about a region of the displayed spectrum to be highlighted
   * @param x1 the x value of the coordinate where the highlight should start
   * @param x2 the x value of the coordinate where the highlight should end
   * @param color the color of the highlight
   */
  public void addHighlight(double x1, double x2, Color color){
    Highlight hl = new Highlight(x1, x2, color);
    if(!highlights.contains(hl)){
      highlights.addElement(hl);
    }
  }

  /**
   * Add information about a region of the displayed spectrum to be highlighted
   * @param x1 the x value of the coordinate where the highlight should start
   * @param x2 the x value of the coordinate where the highlight should end
   */
  public void addHighlight(double x1, double x2){
    Highlight hl = new Highlight(x1, x2);
    if(!highlights.contains(hl)){
      highlights.addElement(hl);
    }
  }

  /**
   * Remove the highlight at the specfied index in the internal list of highlights
   * The index depends on the order in which the highlights were added
   * @param index the index of the highlight in the list
   */
  public void removeHighlight(int index){
    highlights.removeElementAt(index);
  }

  /**
   * Remove the highlight specified by the starting and ending x value
   * @param x1 the x value of the coordinate where the highlight started
   * @param x2 the x value of the coordinate where the highlight ended
   */
  public void removeHighlight(double x1, double x2){
    Highlight hl = new Highlight(x1, x2);
    int index = highlights.indexOf(hl);
    if(index != -1){
      highlights.removeElementAt(index);
    }
  }

  /**
   * Removes all highlights from the display
   */
  public void removeAllHighlights(){
    highlights.removeAllElements();
  }

  /* ------------------------- GET METHODS ----------------------------*/

  /**
   * Returns true if zoom is enabled
   * @return true if zoom is enabled
   */
  public boolean isZoomEnabled(){
    return zoomEnabled;
  }

  /**
   * Returns true if coordinates are on
   * @return true if coordinates are displayed
   */
  public boolean isCoordinatesOn(){
    return coordsOn;
  }

  /**
   * Returns true if grid is on
   * @return true if if the grid is displayed
   */
  public boolean isGridOn(){
    return gridOn;
  }

  /**
   * Returns true if plot is reversed
   * @return true if plot is reversed
   */
  public boolean isPlotReversed(){
    return plotReversed;
  }

  /**
   * Returns the minimum number of points for zoom
   * if plot is reversed
   * @return the minimum number of points for zoom
   * if plot is reversed
   */
  public int getMinNumOfPointsForZoom(){
    return minNumOfPointsForZoom;
  }

  /**
   * Returns the <code>Spectrum</code> at the specified index
   * @param index the index of the <code>Spectrum</code>
   * @return the <code>Spectrum</code> at the specified index
   */
  public Graph getSpectrumAt(int index){
    return spectra[index];
  }

  /**
   * Returns the Number of Spectra
   * @return the Number of Spectra
   */
  public int getNumberOfSpectra(){
    return numOfSpectra;
  }

  /**
   * Returns the title displayed on the graph
   * @return the title displayed on the graph
   */
  public String getTitle(){
    return title;
  }

  /**
   * Returns the insets of the plot area
   * @return the insets of the plot area
   */
  public Insets getPlotAreaInsets(){
    return plotAreaInsets;
  }

  /**
    * Returns the color of the plotArea
    * @return the color of the plotArea
    */
  public Color getPlotAreaColor(){
    return plotAreaColor;
  }

   /**
    * Returns the color of the plot at a certain index
    * @param index  the index
    * @return the color of the plot
    */
  public Color getPlotColor(int index){
    return plotColors[index];
  }

  /**
   * Returns the color of the scale
   * @return the color of the scale
   */
  public Color getScaleColor(){
    return scaleColor;
  }

  /**
   * Returns the color of the units
   * @return the color of the units
   */
  public Color getUnitsColor(){
    return unitsColor;
  }

  /**
   * Returns the color of the title
   * @return the color of the title
   */
  public Color getTitleColor(){
    return titleColor;
  }

  /**
   * Returns the color of the grid
   * @return the color of the grid
   */
  public Color getGridColor(){
    return gridColor;
  }

  /**
   * Returns the color of the Coodinates
   * @return the color of the Coodinates
   */
  public Color getcoordinatesColor(){
    return coordinatesColor;
  }

  /**
   * Returns the color of the highlighted Region
   * @return the color of the highlighted Region
   */
  public Color getHighlightColor(){
    return highlightColor;
  }

  /**
   * Returns whether highlighting is enabled
   * @return whether highlighting is enabled
   */
  public boolean getHighlightOn(){
    return highlightOn;
  }

  /**
   * Returns the list of coordinates of all the Graphs
   * @return an array of arrays of <code>Coordinate</code>
   */
  public Coordinate[][] getXYCoordsList(){
    return xyCoordsList;
  }

  /**
   * Return the start indices of the Scaledata
   * @return the start indices of the Scaledata
   */
  public int[] getStartDataPointIndices(){
    return scaleData.startDataPointIndices;
  }

  /**
   * Return the end indices of the Scaledata
   * @return the end indices of the Scaledata
   */
  public int[] getEndDataPointIndices(){
    return scaleData.endDataPointIndices;
  }

  /**
   * Returns the name of the font used in the display
   * @return the name of the font used in the display
   */
  public String getDisplayFontName(){
    return fontName;
  }

  /*----------------------- JSVPanel PAINTING METHODS ---------------------*/

  /**
   * Overides paintComponent in class JPanel inorder to draw the spectrum
   * @param g the <code>Graphics</code> object
   */
   @Override
  public void paintComponent(Graphics g){
      super.paintComponent(g);
      int width = getWidth();
      int height = getHeight();

      drawGraph(g, height, width);
   }

   /**
    * Draws the Spectrum to the panel
    * @param g the <code>Graphics</code> object
    * @param height the height to be drawn in pixels
    * @param width the width to be drawn in pixels
    */
   protected void drawGraph(Graphics g, int height, int width){
    plotAreaWidth = width - (plotAreaInsets.right + plotAreaInsets.left);
    plotAreaHeight = height - (plotAreaInsets.top + plotAreaInsets.bottom);

    leftPlotAreaPos = plotAreaX;
    rightPlotAreaPos = plotAreaWidth + plotAreaX;
    topPlotAreaPos = plotAreaY;
    bottomPlotAreaPos = plotAreaHeight + plotAreaY;

    xFactorForScale = (scaleData.maxXOnScale-scaleData.minXOnScale)/plotAreaWidth;
    yFactorForScale = (scaleData.maxYOnScale-scaleData.minYOnScale)/plotAreaHeight;

    // fill plot area color
    g.setColor(plotAreaColor);
    g.fillRect(plotAreaX, plotAreaY, plotAreaWidth, plotAreaHeight);

    boolean plotIncreasing;
    if(numOfSpectra == 1)
      plotIncreasing = spectra[0].isIncreasing() ^ plotReversed;
    else
      plotIncreasing = !plotReversed;

    // fill highlight color

    if(highlightOn){
      int x1, x2;

      if(JSpecViewUtils.DEBUG)
          System.out.println();

      for(int i = 0; i < highlights.size(); i++){
        Highlight hl = (Highlight)highlights.elementAt(i);
        if(plotIncreasing){
          x1 = (int) (leftPlotAreaPos + ((hl.getStartX() -scaleData.minXOnScale)/xFactorForScale));
          x2 = (int) (leftPlotAreaPos + ((hl.getEndX() -scaleData.minXOnScale)/xFactorForScale));
        }
        else{
          x2 = (int) (rightPlotAreaPos - ((hl.getStartX()-scaleData.minXOnScale)/xFactorForScale));
          x1 = (int) (rightPlotAreaPos - ((hl.getEndX()-scaleData.minXOnScale)/xFactorForScale));
        }

        if(x1 > x2){
          int tmp = x1;
          x1 = x2;
          x2 = tmp;
        }

        if(JSpecViewUtils.DEBUG){
          System.out.println("x1: " + x1);
          System.out.println("x2: " + x2);
        }

        // if either pixel is outside of plot area
        if(!isPixelWithinPlotArea(x1) || !isPixelWithinPlotArea(x2)){
          // if both are ouside of plot area
          if(!isPixelWithinPlotArea(x1) && !isPixelWithinPlotArea(x2)){
            // check if leftareapos and rightareapos both lie between
            // x1 and x2
            if(leftPlotAreaPos >= x1 && rightPlotAreaPos <= x2){
               x1 = leftPlotAreaPos;
               x2 = rightPlotAreaPos;
            }
            else{
              continue;
            }
          }
          else if(isPixelWithinPlotArea(x1) && !isPixelWithinPlotArea(x2)){
            x2 = rightPlotAreaPos;
          }
          else if(!isPixelWithinPlotArea(x1) && isPixelWithinPlotArea(x2)){
            x1 = leftPlotAreaPos;
          }
        }

        g.setColor(hl.getColor());

        g.fillRect(x1, plotAreaY, Math.abs(x2 - x1), plotAreaHeight);
      }
    }

    boolean grid, scale, title, units, coords;

    if(isPrinting){
      grid = printGrid;
      scale = printScale;
      title = printTitle;
      units = printScale;
      coords = false;
    }
    else{
      grid = gridOn;
      scale = scaleOn;
      title = titleOn;
      units = unitsOn;
      coords = coordsOn;
    }

    if(grid)
      drawGrid(g, height, width);
    for(int i = 0; i < numOfSpectra; i++)
      drawPlot(g, i, height, width);
    if(scale)
      drawScale(g, height, width);
    if(title)
      drawTitle(g, height, width);
    if(units)
      drawUnits(g, height, width);
    if(coords)
      drawCoordinates(g, height, width);
    /*if(zoomEnabled)
     drawZoomBox(g);*/
  }


  private boolean isPixelWithinPlotArea(int pix){
    if(pix >= leftPlotAreaPos && pix <= rightPlotAreaPos){
      return true;
    }
    return false;
  }

  /**
   * Draws the plot on the Panel
   * @param g the <code>Graphics</code> object
   * @param index the index of the Spectrum to draw
   * @param height the height to be drawn in pixels
    * @param width the width to be drawn in pixels
   */
  protected void drawPlot(Graphics g, int index, int height, int width){
    // Check if specInfo in null or xyCoords is null
    //Coordinate[] xyCoords = spectra[index].getXYCoords();
    Coordinate[] xyCoords = xyCoordsList[index];

    boolean drawPlotIncreasing;

    if(numOfSpectra == 1)
      drawPlotIncreasing = spectra[index].isIncreasing() ^ plotReversed;
    else
      //drawPlotIncreasing = !plotReversed;
      drawPlotIncreasing = overlayIncreasing ^ plotReversed;

    // Draw a border
    if(!gridOn){
      g.setColor(gridColor);
      g.drawRect(plotAreaX, plotAreaY, plotAreaWidth, plotAreaHeight);
    }

    g.setColor(plotColors[index]);

    // Check if revPLot on

    if(drawPlotIncreasing){
     if(!spectra[index].isContinuous()){
        for(int i = scaleData.startDataPointIndices[index]; i <= scaleData.endDataPointIndices[index]; i++){
          Coordinate point = xyCoords[i];
          int x1 = (int) (leftPlotAreaPos + (((point.getXVal())-scaleData.minXOnScale)/xFactorForScale));
          int y1 = (int) (topPlotAreaPos);
          int x2 = (int) (leftPlotAreaPos + (((point.getXVal())-scaleData.minXOnScale)/xFactorForScale));
          int y2 = (int) (topPlotAreaPos + (((point.getYVal())-scaleData.minYOnScale)/yFactorForScale));
          g.drawLine(x1, invertY(height, y1), x2, invertY(height, y2));
        }
      }
      else{
        for(int i = scaleData.startDataPointIndices[index]; i < scaleData.endDataPointIndices[index]; i++){
          Coordinate point1 = xyCoords[i];
          Coordinate point2 = xyCoords[i+1];
          int x1 = (int) (leftPlotAreaPos + (((point1.getXVal())-scaleData.minXOnScale)/xFactorForScale));
          int y1 = (int) (topPlotAreaPos + (((point1.getYVal())-scaleData.minYOnScale)/yFactorForScale));
          int x2 = (int) (leftPlotAreaPos + (((point2.getXVal())-scaleData.minXOnScale)/xFactorForScale));
          int y2 = (int) (topPlotAreaPos + (((point2.getYVal())-scaleData.minYOnScale)/yFactorForScale));
          g.drawLine(x1, invertY(height, y1), x2, invertY(height, y2));
        }
      }
    }
    else{
      if(!spectra[index].isContinuous()){
        for(int i = scaleData.endDataPointIndices[index]; i >= scaleData.startDataPointIndices[index]; i--){
          Coordinate point = xyCoords[i];
          int x1 = (int) (rightPlotAreaPos - (((point.getXVal())-scaleData.minXOnScale)/xFactorForScale));
          int y1 = (int) (topPlotAreaPos);
          int x2 = (int) (rightPlotAreaPos - (((point.getXVal())-scaleData.minXOnScale)/xFactorForScale));
          int y2 = (int) (topPlotAreaPos + (((point.getYVal())-scaleData.minYOnScale)/yFactorForScale));
          g.drawLine(x1, invertY(height, y1), x2, invertY(height, y2));
        }
      }
      else{
        for(int i = scaleData.endDataPointIndices[index]; i > scaleData.startDataPointIndices[index] ; i--){
          Coordinate point1 = xyCoords[i];
          Coordinate point2 = xyCoords[i-1];
          int x1 = (int) (rightPlotAreaPos -  (((point1.getXVal())-scaleData.minXOnScale)/xFactorForScale));
          int y1 = (int) (topPlotAreaPos + (((point1.getYVal())-scaleData.minYOnScale)/yFactorForScale));
          int x2 = (int) (rightPlotAreaPos - (((point2.getXVal())-scaleData.minXOnScale)/xFactorForScale));
          int y2 = (int)(topPlotAreaPos + (((point2.getYVal())-scaleData.minYOnScale)/yFactorForScale));
          g.drawLine(x1, invertY(height, y1), x2, invertY(height, y2));
        }
      }
    }
  } // End drawPlot


  /**
   * Draws the grid on the Panel
   * @param g the <code>Graphics</code> object
   * @param height the height to be drawn in pixels
   * @param width the width to be drawn in pixels
   */
  protected void drawGrid(Graphics g, int height, int width){
    g.setColor(gridColor);

    for(double i = scaleData.minXOnScale; i < scaleData.maxXOnScale + scaleData.xStep/2 ; i += scaleData.xStep){
      int x = (int)(leftPlotAreaPos + ((i - scaleData.minXOnScale)/xFactorForScale));
      int y1 = (int) topPlotAreaPos;
      int y2 = (int) (topPlotAreaPos + ((scaleData.maxYOnScale - scaleData.minYOnScale)/yFactorForScale));
      g.drawLine(x, invertY(height, y1), x, invertY(height, y2));
    }

    for(double i = scaleData.minYOnScale; i < scaleData.maxYOnScale + scaleData.yStep/2; i += scaleData.yStep){
      int x1 = (int) leftPlotAreaPos;
      int x2 = (int)(leftPlotAreaPos + ((scaleData.maxXOnScale - scaleData.minXOnScale)/xFactorForScale));
      int y = (int) (topPlotAreaPos + ((i - scaleData.minYOnScale)/yFactorForScale));
      g.drawLine(x1, invertY(height, y), x2, invertY(height, y));
    }
  } // End drawGrid

  /**
   * Draws the Scale
   * @param g the <code>Graphics</code> object
   * @param height the height to be drawn in pixels
   * @param width the width to be drawn in pixels
   */
  protected void drawScale(Graphics g, int height, int width){

    String hashX = "#";
    String hashY = "#";
    String hash1 = "0.00000000";

    boolean drawScaleIncreasing;

    if(numOfSpectra == 1)
      drawScaleIncreasing = spectra[0].isIncreasing() ^ plotReversed;
    else
      //drawScaleIncreasing = !plotReversed;
      drawScaleIncreasing = overlayIncreasing ^ plotReversed;

    if (scaleData.hashNumX <= 0)
      hashX = hash1.substring(0,Math.abs(scaleData.hashNumX)+3);

    DecimalFormat displayXFormatter = new DecimalFormat(hashX);

    if (scaleData.hashNumY <= 0)
      hashY = hash1.substring(0,Math.abs(scaleData.hashNumY)+3);


    DecimalFormat displayYFormatter = new DecimalFormat(hashY);

    Font font;

    if(isPrinting)
      font = new Font(printingFont, Font.PLAIN, calculateFontSize(width, 12, true));
    else
      font = new Font(fontName, Font.PLAIN, calculateFontSize(width, 12, true));
    g.setFont(font);

    if (drawScaleIncreasing){
      for(double i = scaleData.minXOnScale; i < (scaleData.maxXOnScale + scaleData.xStep/2); i += scaleData.xStep){
        int x = (int)(leftPlotAreaPos + ((i - scaleData.minXOnScale)/xFactorForScale));
        int y1 = (int) topPlotAreaPos;
        int y2 = (int) (topPlotAreaPos - 3);
        g.setColor(gridColor);
        g.drawLine(x, invertY(height, y1), x, invertY(height, y2));
        g.setColor(scaleColor);
        g.drawString(displayXFormatter.format(i), x - 10, (invertY(height, y2) + 15));
      }
    }
    else{
      for(double i = scaleData.minXOnScale, j = scaleData.maxXOnScale; i < (scaleData.maxXOnScale + scaleData.xStep/2); i += scaleData.xStep, j -= scaleData.xStep){
        int x = (int)(leftPlotAreaPos + ((j - scaleData.minXOnScale)/xFactorForScale));
        int y1 = (int) topPlotAreaPos;
        int y2 = (int) (topPlotAreaPos - 3);
        g.setColor(gridColor);
        g.drawLine(x, invertY(height, y1), x, invertY(height, y2));
        g.setColor(scaleColor);
        g.drawString(displayXFormatter.format(i), x - 5, (invertY(height, y2) + 15));
      }
    }

    if(isPrinting)
      font = new Font(printingFont, Font.PLAIN, calculateFontSize(width, 12, false));
    else
      font = new Font(fontName, Font.PLAIN, calculateFontSize(width, 12, false));
    g.setFont(font);

    for(double i = scaleData.minYOnScale; (i < scaleData.maxYOnScale + scaleData.yStep/2); i += scaleData.yStep){
      int x1 = (int) leftPlotAreaPos;
      int x2 = (int)(leftPlotAreaPos - 3);
      int y = (int) (topPlotAreaPos + ((i - scaleData.minYOnScale)/yFactorForScale));
      g.setColor(gridColor);
      g.drawLine(x1, invertY(height, y), x2, invertY(height, y));
      g.setColor(scaleColor);
      g.drawString(displayYFormatter.format(i), (x2 - 60), invertY(height, y));
    }
  } // End drawScale


  /**
   * Draws Title
   * @param g the <code>Graphics</code> object
   * @param height the height to be drawn in pixels
   * @param width the width to be drawn in pixels
   */
  protected void drawTitle(Graphics g, int height, int width){
    Font font;

    g.setColor(titleColor);

    if(isPrinting)
      font = new Font(printingFont, Font.BOLD, calculateFontSize(width, 14, true));
    else
      font = new Font(fontName, Font.BOLD, calculateFontSize(width, 14, true));


    g.setFont(font);
    if(numOfSpectra == 1)
      title = spectra[0].getTitle().substring(0, 1).toUpperCase() + spectra[0].getTitle().substring(1);
    g.drawString(title, (int)(leftPlotAreaPos) , (int)(bottomPlotAreaPos + 45));

  } // End Draw Title

  /**
   * Draws the Units
   * @param g the <code>Graphics</code> object
   * @param height the height to be drawn in pixels
   * @param width the width to be drawn in pixels
   */
   protected void drawUnits(Graphics g, int height, int width){

    g.setColor(unitsColor);
    Font font;
    if(isPrinting)
      font = new Font(printingFont, Font.ITALIC, calculateFontSize(width, 10, true));
    else
      font = new Font(fontName, Font.ITALIC, calculateFontSize(width, 10, true));
    g.setFont(font);
    g.drawString(spectra[0].getYUnits(), (int)(leftPlotAreaPos - 60) , (int)(topPlotAreaPos - 15));
    g.drawString(spectra[0].getXUnits(), (int)(rightPlotAreaPos - (spectra[0].getXUnits().length() * 10)) , (int)(bottomPlotAreaPos + 35));
  } // End DrawUnits


  /**
   * Draws the Coordinates
   * @param g the <code>Graphics</code> object
   * @param height the height to be drawn in pixels
   * @param width the width to be drawn in pixels
   */
  protected void drawCoordinates(Graphics g, int height, int width){
    g.setColor(coordinatesColor);
    Font font;

    if(isPrinting)
      font = new Font(printingFont, Font.PLAIN, calculateFontSize(width, 12, true));
    else
      font = new Font(fontName, Font.PLAIN, calculateFontSize(width, 12, true));
    g.setFont(font);
    int x = (int) ((plotAreaWidth + leftPlotAreaPos) * 0.85);
    g.drawString(coordStr, x, (int)(topPlotAreaPos - 10));
  }

  /**
   * Draws a box around the portion of the plot are to be zoomed
   * @param g the Graphics object
   */
  protected void drawZoomBox(Graphics g){
    g.setColor(zoomBoxColor);

    if(isMousePressed){
      g.drawLine(zoomBoxX, topPlotAreaPos, zoomBoxX, bottomPlotAreaPos);
      isMousePressed = false;
    }
    if(isMouseDragged){
      g.drawLine(zoomBoxX, topPlotAreaPos, zoomBoxX, bottomPlotAreaPos);
      g.drawLine(zoomBoxX, topPlotAreaPos, currZoomBoxX, topPlotAreaPos);
      g.drawLine(zoomBoxX, bottomPlotAreaPos, currZoomBoxX, bottomPlotAreaPos);
      g.drawLine(currZoomBoxX, topPlotAreaPos, currZoomBoxX, bottomPlotAreaPos);
      isMouseDragged = false;
    }
    if(isMouseReleased){
      repaint();
    }
  }


  // THIS METHOD NEEDS REWORKING!!!
  /**
   * Calculates the size of the font to display based on the window size
   * @param length ??
   * @param initSize the intial size of the font
   * @param isWidth true if the text lies along the width esle false
   * @return the size of the font
   */
  protected int calculateFontSize(double length, int initSize, boolean isWidth){
    int size = initSize;

    if(isWidth){
      if (length < 400)
        size =  (int)((length * initSize) / 400);
    }
    else{
      if(length < 250)
        size = (int)((length * initSize) / 250);
    }

    return size;
  } // End calculateFontSize

  /**
   * Utility used to invert the value of y in the default coordinate system
   * @param y the y pixel value
   * @param height the height to be drawn in pixels
   * @return the inverted y pixel value
   */
  protected int invertY(int height, int y){
    return (plotAreaHeight - y + (2 * plotAreaInsets.top));
  }

  /**
   * Calculates spectrum coordinates from system Coordinates
   * @param xPixel the x pixel value of the coordinate
   * @param yPixel the y pixel value of the coordinate
   * @return the coordinate
   */
  public Coordinate getCoordFromPoint(int xPixel, int yPixel){
    double xPt, yPt;

    boolean plotIncreasing;
    if(numOfSpectra == 1)
      plotIncreasing = spectra[0].isIncreasing() ^ plotReversed;
    else
      //plotIncreasing = !plotReversed;
      plotIncreasing = overlayIncreasing ^ plotReversed;

    if(!plotIncreasing)
      xPt = (((rightPlotAreaPos - xPixel) * xFactorForScale) + scaleData.minXOnScale);
    else
      xPt = scaleData.maxXOnScale - (((rightPlotAreaPos - xPixel) * xFactorForScale));


    yPt = scaleData.maxYOnScale + (((topPlotAreaPos - yPixel) * yFactorForScale));

    return new Coordinate(xPt, yPt);
  }

   /*--------------METHODS IN INTERFACE MouseListener-----------------------*/

   /**
    * Implements mouseClicked in interface MouseListener
    * @param e the <code>MouseEvent</code>
    */
    public void mouseClicked(MouseEvent e){
      fireMouseClicked(e);
    }

    /**
     * Implements mouseEntered in interface MouseListener
     * @param e the <code>MouseEvent</code>
     */
    public void mouseEntered(MouseEvent e){
    }

    /**
     * Implements mouseExited in interface MouseListener
     * @param e the <code>MouseEvent</code>
     */
    public void mouseExited(MouseEvent e){
    }

    /**
     * Implements mousePressed in interface MouseListener
     * @param e the <code>MouseEvent</code>
     */
    public void mousePressed(MouseEvent e){
      // Maybe put this in a fireMousePressed() method
      if(e.getButton() != MouseEvent.BUTTON1)
        return;

      int x = e.getX();
      int y = e.getY();

      int xPixel = x /*- getX()*/;
      int yPixel = y /*- getY()*/;

      if( xPixel >= leftPlotAreaPos && xPixel <= rightPlotAreaPos &&
          yPixel >= topPlotAreaPos && yPixel <= bottomPlotAreaPos){

        isMousePressed = true;
        zoomBoxX = xPixel;

        double xPt, yPt;

        Coordinate coord = getCoordFromPoint(xPixel, yPixel);

        xPt = coord.getXVal();
        yPt = coord.getYVal();

        initX = xPt;
        initY = yPt;

        mousePressedInPlotArea = true;
        repaint();

      }
      else
        mousePressedInPlotArea = false;
    }

    /**
     * Implements mouseReleased in interface MouseListener
     * @param e the <code>MouseEvent</code>
     */
    public void mouseReleased(MouseEvent e){
      // Maybe use a fireMouseReleased Method

      if(e.getButton() != MouseEvent.BUTTON1)
        return;

      int x = e.getX();
      int y = e.getY();

      int xPixel = x /*- getX()*/;
      int yPixel = y /*- getY()*/;

      if( xPixel >= leftPlotAreaPos && xPixel <= rightPlotAreaPos &&
          yPixel >= topPlotAreaPos && yPixel <= bottomPlotAreaPos){


        isMouseReleased = true;

        double xPt, yPt;

        Coordinate coord = getCoordFromPoint(xPixel, yPixel);

        xPt = coord.getXVal();
        yPt = coord.getYVal();

        finalX = xPt;
        finalY = yPt;

        if(mousePressedInPlotArea){
          doZoom(initX, initY, finalX, finalY);
        }

      }

    }


   /*--------------METHODS IN INTERFACE MouseMotionListener------------------*/

   /**
    * Implements mouseDragged in interface MouseMotionListener
    * @param e the <code>MouseEvent</code>
    */
   public void mouseDragged(MouseEvent e){
    isMouseDraggedEvent = true;
    fireMouseDragged(e);
   }

   /**
    * Implements mouseMoved in interface MouseMotionListener
    * @param e the <code>MouseEvent</code>
    */
  public void mouseMoved(MouseEvent e){
    isMouseDraggedEvent = false;
    fireMouseMoved(e);
    if(mouseMovedOk)
      repaint();
  }

  /**
   * Called by the mouseClicked Method
   * @param e the <code>MouseEvent</code>
   */
  protected void fireMouseClicked(MouseEvent e){
    if(e.getButton() != MouseEvent.BUTTON1)
        return;

      int x = e.getX();
      int y = e.getY();

      int xPixel = x /*- getX()*/;
      int yPixel = y /*- getY()*/;

      if( xPixel >= leftPlotAreaPos && xPixel <= rightPlotAreaPos &&
          yPixel >= topPlotAreaPos && yPixel <= bottomPlotAreaPos){

        double xPt, yPt;

        Coordinate coord = getCoordFromPoint(xPixel, yPixel);

        xPt = coord.getXVal();
        yPt = coord.getYVal();

        String hashX = "#";
        String hashY = "#";
        String hash1 = "0.00000000";

        if (scaleData.hashNumX <= 0)
          hashX = hash1.substring(0,Math.abs(scaleData.hashNumX)+3);

        DecimalFormat displayXFormatter = new DecimalFormat(hashX);

        if (scaleData.hashNumY <= 0)
        hashY = hash1.substring(0,Math.abs(scaleData.hashNumY)+3);

        DecimalFormat displayYFormatter = new DecimalFormat(hashY);

        String xStr, yStr;

        xStr = displayXFormatter.format(xPt);
        yStr = displayYFormatter.format(yPt);

        //coordClickedStr = xStr + " "+ yStr;

        coordClicked = new Coordinate(Double.parseDouble(xStr), Double.parseDouble(yStr));
      }
      else
        coordClicked = null;
  }

  /**
   * Carries out the function of the MouseMoved Event
   * @param e the <code>MouseEvent</code>
   */
  protected void fireMouseMoved(MouseEvent e){
    int x = e.getX();
    int y = e.getY();

    int xPixel = x /*- getX()*/;
    int yPixel = y /*- getY()*/;

    if( xPixel >= leftPlotAreaPos && xPixel <= rightPlotAreaPos &&
        yPixel >= topPlotAreaPos && yPixel <= bottomPlotAreaPos &&
        coordsOn){

      if(isMouseDraggedEvent){
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

      if (scaleData.hashNumX <= 0)
        hashX = hash1.substring(0,Math.abs(scaleData.hashNumX)+3);

      DecimalFormat displayXFormatter = new DecimalFormat(hashX);

      if (scaleData.hashNumY <= 0)
      hashY = hash1.substring(0,Math.abs(scaleData.hashNumY)+3);

      DecimalFormat displayYFormatter = new DecimalFormat(hashY);

      coordStr = "(" +displayXFormatter.format(xPt) + ", " + displayYFormatter.format(yPt) + ")";

      mouseMovedOk = true;
    }
  }

  /**
   * Carries out the function of the MouseDragged Event
   * @param e the <code>MouseEvent</code>
   */
  protected void fireMouseDragged(MouseEvent e){
    fireMouseMoved(e);
    if(mouseMovedOk)
      repaint();
  }


   /*-------------------- METHODS FOR ZOOM SUPPORT--------------------------*/

   /**
    * Zooms the spectrum between two coordinates
    * @param initX the X start coordinate of the zoom area
    * @param initY the Y start coordinate of the zoom area
    * @param finalX the X end coordinate of the zoom area
    * @param finalY the Y end coordinate of the zoom area
    */
   public void doZoom(double initX, double initY, double finalX, double finalY){
    if(zoomEnabled){

      //int tempStartDataPointIndex = 0;
      //int tempEndDataPointIndex;
      int[] tempStartDataPointIndices = new int[numOfSpectra];
      int[] tempEndDataPointIndices = new int[numOfSpectra];

      int ptCount = 0;
      int numSpectraWithMinPointsForZoom = 0;

      // swap points if init value > final value
      if(initX > finalX){
        double tempX = initX;
        initX = finalX;
        finalX = tempX;
      }

      if(initY > finalY){
        double tempY = initY;
        initY = finalY;
        finalY = tempY;
      }


      if(isWithinRangeEnsured()){
      //determine startDataPointIndex
        int index = 0;
        for(int i = 0; i < numOfSpectra; i++){
          Coordinate[] xyCoords = spectra[i].getXYCoords();

          for(index = scaleData.startDataPointIndices[i]; index <= scaleData.endDataPointIndices[i]; index++){
            double x = xyCoords[index].getXVal();
            if (x >= initX){
              tempStartDataPointIndices[i] = index;
              break;
            }
          }


          // determine endDataPointIndex
          for(;index <= scaleData.endDataPointIndices[i]; index++){
            double x = xyCoords[index].getXVal();
            ptCount++;
            if(x >= finalX){
              break;
            }
          }


          if(ptCount >= minNumOfPointsForZoom){
            numSpectraWithMinPointsForZoom++;
          }
          ptCount = 0;
          tempEndDataPointIndices[i] = index - 1;
        }

        // need to have a pt count list
       if(numSpectraWithMinPointsForZoom == numOfSpectra){
          // Save scale data
          scaleData = JSpecViewUtils.generateScaleData(xyCoordsList, tempStartDataPointIndices, tempEndDataPointIndices, 10, 10);
          if(zoomInfoList.size() > currentZoomIndex+1){
            for(int i = zoomInfoList.size()-1; i >= currentZoomIndex+1; i--){
              zoomInfoList.removeElementAt(i);
            }
          }
          zoomInfoList.addElement(scaleData);
          currentZoomIndex++;
          repaint(); // just repaint the plot Area
        }
      }
    }
   }


   /**
    * Zooms the spectrum between two indices in the list of coordinates
    * @param startIndices the start indices
    * @param endIndices the end indices
    * @throws JSpecViewException
    */
  public void doZoom(int[] startIndices, int[] endIndices) throws JSpecViewException{
    if(doZoomWithoutRepaint(startIndices, endIndices))
      repaint(); // just repaint the plot Area
  }

  /**
   * Zooms the spectrum but does not repaint so that it is not visible
   * @param startIndices the start indices
   * @param endIndices the end indices
   * @return true if successful
   * @throws JSpecViewException
   */
  private boolean doZoomWithoutRepaint(int[] startIndices, int[] endIndices) throws JSpecViewException{
    if(zoomEnabled){
      // Ensure range
      int countHasMinNumOfPointsForZoom = 0;
      if(startIndices.length != numOfSpectra || endIndices.length != numOfSpectra)
        throw new JSpecViewException("Invalid start or endIndices");
        // throw invalid argument exception

      for(int i = 0; i < numOfSpectra; i++){
        if(startIndices[i] < 0)
          startIndices[i] = 0;
        if(endIndices[i] >= spectra[i].getNumberOfPoints())
          endIndices[i] = spectra[i].getNumberOfPoints() - 1;
        if((endIndices[i] - startIndices[i] + 1) >= minNumOfPointsForZoom)
          countHasMinNumOfPointsForZoom++;
      }

      if(countHasMinNumOfPointsForZoom == numOfSpectra){
        scaleData = JSpecViewUtils.generateScaleData(xyCoordsList, startIndices, endIndices, 10, 10);
        // Not sure if I should do this
        // ie. add the info to the zoomInfoList
        if(zoomInfoList.size() > currentZoomIndex+1){
          for(int i = zoomInfoList.size()-1; i >= currentZoomIndex+1; i--){
            zoomInfoList.removeElementAt(i);
          }
        }
        zoomInfoList.addElement(scaleData);
        currentZoomIndex++;
        return true;
      }
    }

    return false;
  }

  /**
   * Zooms the spectrum between two data points in the list of coordinates
   * specified by startindex and endindex
   * @param startIndex the index of the starting data point
   * @param endIndex the index of the ending data point
   * @throws JSpecViewException
   */
  public void doZoom(int startIndex, int endIndex) throws JSpecViewException{
    if(numOfSpectra != 1){
      throw new JSpecViewException("Can't Zoom");
      // throw invalid argument exception
    }

    doZoom(new int[]{startIndex}, new int[]{endIndex});
  }

  /**
   * Resets the spectrum to it's original view
   */
  public void reset(){
    scaleData = (JSpecViewUtils.MultiScaleData)zoomInfoList.firstElement();
    currentZoomIndex = 0;
    repaint();
  }

  /**
   * Clears all views in the zoom list
   */
  public void clearViews(){
    reset();
    int loopNum = zoomInfoList.size();
    for(int i = 1; i < loopNum; i++){
      zoomInfoList.removeElementAt(1);
    }
  }

  /**
   * Displays the previous view zoomed
   */
  public void previousView(){
    if(currentZoomIndex - 1 >= 0){
      scaleData = (JSpecViewUtils.MultiScaleData)zoomInfoList.elementAt(--currentZoomIndex);
      repaint();
    }
  }

  /**
   * Displays the next view zoomed
   */
  public void nextView(){
    if(currentZoomIndex + 1 < zoomInfoList.size()){
      scaleData = (JSpecViewUtils.MultiScaleData)zoomInfoList.elementAt(++currentZoomIndex);
      repaint();
    }
  }

  /**
   * Determines if the range of the area selected for zooming
   * is within the plot Area and if not ensures that it is
   * @return true if is within range or can be set to within range
   */
  protected  boolean isWithinRangeEnsured(){
    boolean rangeOK = true;

    if(!isWithinRange(initX) && !isWithinRange(finalX))
      rangeOK = false;
    else if(!isWithinRange(initX)){
      initX = scaleData.minX;
    }
    else if(!isWithinRange(finalX)){
      finalX = scaleData.maxX;
    }

    return rangeOK;
  }

  /**
   * Determines if the x coordinate is within the range of coordinates
   * in the coordinate list
   * @param x the x coodinate
   * @return true if within range
   */
  protected boolean isWithinRange(double x){
    if(x >= scaleData.minX && x <= scaleData.maxX)
      return true;
    return false;
  }


   /*----------------- METHODS IN INTERFACE Printable ---------------------- */

   /**
    * Implements method print in interface printable
    * @param g the <code>Graphics</code> object
    * @param pf the <code>PageFormat</code> object
    * @param pi the page index
    * @return an int that depends on whether a print was successful
    * @throws PrinterException
    */
  public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
    if (pi == 0) {
      Graphics2D g2D = (Graphics2D)g;
      isPrinting = true;

      double height, width;

      if(graphPosition.equals("default")){
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        if(pf.getOrientation() == PageFormat.PORTRAIT){
          height = defaultHeight;
          width = defaultWidth;
        }else{
          height = defaultWidth;
          width = defaultHeight;
        }
      } else if(graphPosition.equals("fit to page")){
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        height = pf.getImageableHeight();
        width = pf.getImageableWidth();
      } else{ // center
        Paper paper = pf.getPaper();
        double paperHeight = paper.getHeight();
        double paperWidth = paper.getWidth();
        int x, y;

        if(pf.getOrientation() == PageFormat.PORTRAIT){
          height = defaultHeight;
          width = defaultWidth;
          x = (int)(paperWidth - width)/2;
          y = (int)(paperHeight - height)/2;
        }else{
          height = defaultWidth;
          width = defaultHeight;
          y = (int)(paperWidth - defaultWidth)/2;
          x = (int)(paperHeight - defaultHeight)/2;
        }
        g2D.translate(x, y);
      }

      drawGraph(g2D, (int)height, (int)width);

      isPrinting = false;
      return Printable.PAGE_EXISTS;
    }
    isPrinting = false;
    return Printable.NO_SUCH_PAGE;
  }
  /*--------------------------------------------------------------------------*/

  /**
   * Send a print job of the spectrum to the default printer on the system
   * @param pl the layout of the print job
   */
  public void printSpectrum(PrintLayoutDialog.PrintLayout pl){


    PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();

    if(pl.layout.equals("landscape"))
      aset.add(OrientationRequested.LANDSCAPE);
    else
      aset.add(OrientationRequested.PORTRAIT);

    aset.add(pl.paper);

    //MediaSize size = MediaSize.getMediaSizeForName(pl.paper);

    // Set Graph Properties
    printingFont = pl.font;
    printGrid = pl.showGrid;
    printScale = pl.showScale;
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
      }
      catch (PrinterException ex) {
        ex.printStackTrace();
      }
    }

  }

  /*------------------------- Javascript call functions ---------------------*/

  /**
   * Returns the spectrum coordinate of the point on the display that was clicked
   * @return the spectrum coordinate of the point on the display that was clicked
   */
  public Coordinate getClickedCoordinate(){
    return coordClicked;
  }

  /*--------------------------------------------------------------------------*/

  /**
   * Private class to represent a Highlighed region of the spectrum display
   * <p>Title: JSpecView</p>
   * <p>Description: JSpecView is a graphical viewer for chemical spectra specified in the JCAMP-DX format</p>
   * <p>Copyright: Copyright (c) 2002</p>
   * <p>Company: Dept. of Chemistry, University of the West Indies, Mona Campus, Jamaica</p>
   * @author Debbie-Ann Facey
   * @author Khari A. Bryan
   * @author Prof Robert.J. Lancashire
   * @version 1.0.017032006
   */
  private class Highlight{
    private double x1;
    private double x2;
    private Color color = new Color(255, 255, 0, 100);

    /**
     * Constructor
     * @param x1 starting x coordinate
     * @param x2 ending x coordinate
     */
    public Highlight(double x1, double x2){
      this.x1 = x1;
      this.x2 = x2;
    }

    /**
     * Constructor
     * @param x1 starting x coordinate
     * @param x2 ending x coordinate
     * @param color the color of the highlighted region
     */
    public Highlight(double x1, double x2, Color color){
      this(x1, x2);
      this.color = color;
    }

    /**
     * Returns the x coordinate where the highlighted region starts
     * @return the x coordinate where the highlighted region starts
     */
    public double getStartX(){
      return x1;
    }

    /**
     * Returns the x coordinate where the highlighted region ends
     * @return the x coordinate where the highlighted region ends
     */
    public double getEndX(){
      return x2;
    }

    /**
     * Returns the color of the highlighted region
     * @return the color of the highlighted region
     */
    public Color getColor(){
      return color;
    }

    /**
     * Overides the equals method in class <code>Object</code>
     * @param obj the object that this <code>Hightlight<code> is compared to
     * @return true if equal
     */
    @Override
    public boolean equals(Object obj){
      if(!(obj instanceof Highlight))
        return false;
      Highlight hl = (Highlight)obj;

      return ((hl.x1 == this.x1) && (hl.x2 == this.x2));
    }
  }

  /**
   * Sets whether plots that are overlayed should be drawn in increasing of decreasing
   * @param val true if increasing, false otherwise
   */
  public void setOverlayIncreasing(boolean val){
    overlayIncreasing = val;
  }

  /**
   * Returns whether overlayed plots are drawn increasing
   * @return true is increasing, false otherwise
   */
  public boolean isOverlayIncreasing(){
    return overlayIncreasing;
  }

  public JSVPanel copy() {
    JSVPanel newJSVPanel = new JSVPanel();

    newJSVPanel.setGridOn(isGridOn());
    newJSVPanel.setReversePlot(isPlotReversed());
    newJSVPanel.setCoordinatesOn(isCoordinatesOn());
    newJSVPanel.setOverlayIncreasing(isOverlayIncreasing());
    newJSVPanel.setZoomEnabled(isZoomEnabled());
//    newJSVPanel.setZoomEnabled(true);


    return newJSVPanel;
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
   * @param spec the spectrum to export
   * @param fc file chooser to use
   * @param comm the format to export in
   * @param index the index of the spectrum
   * @param recentFileName 
   * @param dirLastExported 
   * @return dirLastExported
   */
  public String exportSpectrum(JDXSpectrum spec, JFileChooser fc, String comm,
                            int index, String recentFileName,
                            String dirLastExported) {
    JSpecViewFileFilter filter = new JSpecViewFileFilter();
    String name = TextFormat.split(recentFileName, ".")[0];
    if ("XY FIX PAC SQZ DIF".indexOf(comm) >= 0) {
      filter.addExtension("jdx");
      filter.addExtension("dx");
      filter.setDescription("JCAMP-DX Files");
      name += ".jdx";
    } else {
      if (comm.equals("AnIML"))
        comm = "AML";
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
        (new Exporter()).export(comm, file.getAbsolutePath(), spec, startIndex,
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

      JDXSpectrum spec = (JDXSpectrum) getSpectrumAt(0);
      return exportSpectrum(spec, fc, type, 0, recentFileName, dirLastExported);
    }

    String[] items = new String[numOfSpectra];
    for (int i = 0; i < numOfSpectra; i++) {
      JDXSpectrum spectrum = (JDXSpectrum) getSpectrumAt(i);
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
        JDXSpectrum spec = (JDXSpectrum) getSpectrumAt(index);
        dialog.dispose();
        exportSpectrum(spec, f, t, index, rfn, dl);
      }
    });
    dialog.setVisible(true);
    return this.dirLastExported;
  }

  public static void setMenus(JMenu saveAsMenu, JMenu saveAsJDXMenu, JMenu exportAsMenu, ActionListener actionListener) {
    addMenuItem(saveAsJDXMenu, "XY", actionListener);
    addMenuItem(saveAsJDXMenu, "FIX", actionListener);
    addMenuItem(saveAsJDXMenu, "PAC", actionListener);
    addMenuItem(saveAsJDXMenu, "SQZ", actionListener);
    addMenuItem(saveAsJDXMenu, "DIF", actionListener);
    addMenuItem(saveAsMenu, "AnIML", actionListener);
    addMenuItem(saveAsMenu, "CML", actionListener);
    addMenuItem(exportAsMenu, "JPG", actionListener);
    addMenuItem(exportAsMenu, "PNG", actionListener);
    addMenuItem(exportAsMenu, "SVG", actionListener);
  }
  
  private static void addMenuItem(JMenu m, String key, ActionListener actionListener) {
    JMenuItem jmi = new JMenuItem();
    jmi.setMnemonic(key.charAt(0));
    jmi.setText(key);
    jmi.addActionListener(actionListener);
    m.add(jmi);
  }


}
