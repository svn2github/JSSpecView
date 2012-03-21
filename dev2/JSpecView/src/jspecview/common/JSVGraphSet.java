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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


package jspecview.common;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JOptionPane;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.source.JDXSource;

/**
 * JSVGraphSet class represents a set of overlaid spectra within the same rectangle within a JSVPanel
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

class JSVGraphSet {

  
  private JSVPanel jsvp;
  private static final long serialVersionUID = 1L;
  private List<Graph> spectra = new ArrayList<Graph>(2);
  MultiScaleData multiScaleData;
  private List<Highlight> highlights = new ArrayList<Highlight>();
  private List<MultiScaleData> zoomInfoList;
  private ArrayList<Annotation> integrationRatios;
  private ArrayList<Annotation> annotations;
  private ColoredAnnotation lastAnnotation;
  protected JDXSource source;
  private ImageScaleData isd;
  private BufferedImage image2D;
  private PlotWidget zoomBox1D, zoomBox2D, 
      pin1Dx0, pin1Dx1, // ppm range -- horizontal bar on 1D spectrum 
      pin1Dy0, pin1Dy1, // y-scaling -- vertical bar on 1D spectrum and left of 2D when no 1D
      pin1Dx01, pin1Dy01, // center pins for those
      pin2Dx0, pin2Dx1, // ppm range -- horizontal bar on 2D spectrum 
      pin2Dy0, pin2Dy1, // subspectrum range -- vertical bar on 2D spectrum
      pin2Dx01, pin2Dy01, // center pins for those
      cur2Dx0, cur2Dx1, // 2D x cursors -- derived from pin1Dx0 and pin1Dx1 values
      cur2Dy;           // 2D y cursor -- points to currently displayed 1D slice
      
  private PlotWidget[] widgets;

  // for the 1D plot area:
  private int xPixel0, yPixel0, xPixel1, yPixel1;
  // for the overall panel section:
  private int xPixel00, yPixel00, xPixel11, yPixel11;
  private int xPixels, yPixels;
  
  private boolean allowYScale = true;
  private boolean drawXAxisLeftToRight;
  private boolean xAxisLeftToRight = true;  
  private boolean doDraw1DObjects = true;
  private boolean sticky2Dcursor;

  private boolean reversePlot;
  private boolean enableZoom;

  private int currentZoomIndex;  
  private int nSpectra;

  double userYFactor = 1;
  private double xFactorForScale, yFactorForScale;
  private double minYScale;
  private double widthRatio;
  

  private List<Graph> graphsTemp = new ArrayList<Graph>();

  //////// settable parameters //////////

  private Color[] plotColors;

  public JSVGraphSet(JSVPanel jsvp) {
    this.jsvp = jsvp;
  }

  void addSpec(Graph spec) {
    spectra.add(spec);
    nSpectra++;
  }
  
  void setPlotColors(Color[] colors) {
    if (colors.length > nSpectra) {
      Color[] tmpPlotColors = new Color[nSpectra];
      System.arraycopy(colors, 0, tmpPlotColors, 0, nSpectra);
      colors = tmpPlotColors;
    } else if (nSpectra > colors.length) {
      Color[] tmpPlotColors = new Color[nSpectra];
      int numAdditionColors = nSpectra - colors.length;
      System.arraycopy(colors, 0, tmpPlotColors, 0, colors.length);
      for (int i = 0, j = colors.length; i < numAdditionColors; i++, j++)
        tmpPlotColors[j] = AppUtils.generateRandomColor();
      colors = tmpPlotColors;
    }
    plotColors = colors;
  }


  JDXSpectrum getSpectrum() {
    return getSpectrumAt(0).getCurrentSubSpectrum();
  }

  /**
   * Initializes the graph set
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
  
  protected void initGraphSet(int startIndex, int endIndex) {
    xAxisLeftToRight = getSpectrumAt(0).shouldDisplayXAxisIncreasing();
    setDrawXAxis();
    int[] startIndices = new int[nSpectra];
    int[] endIndices = new int[nSpectra];
    allowYScale = true;
    if (endIndex == 0)
      endIndex = Integer.MAX_VALUE;
    for (int i = 0; i < nSpectra; i++) {
      int iLast = spectra.get(i).getXYCoords().length - 1;
      startIndices[i] = Coordinate.intoRange(startIndex, 0, iLast);
      endIndices[i] = Coordinate.intoRange(endIndex, 0, iLast);
      allowYScale &= (spectra.get(i).getYUnits().equals(spectra.get(0).getYUnits()) 
          && spectra.get(i).getUserYFactor() == spectra.get(0).getUserYFactor());        
    }
    getMultiScaleData(0, 0, 0, 0, startIndices, endIndices);
    System.out.println("JSVGraphSet nSpectra = " + nSpectra);
    zoomInfoList = new ArrayList<MultiScaleData>();
    zoomInfoList.add(multiScaleData);
    setPlotColors(Parameters.defaultPlotColors);
  }
  
  private void getMultiScaleData(double x1, double x2, double y1, double y2,
                                 int[] startIndices, int[] endIndices) {
    List<Graph>graphs = (graphsTemp.size() == 0 ? spectra : graphsTemp);
    List<JDXSpectrum> subspecs = getSpectrumAt(0).getSubSpectra();
    boolean dontUseSubspecs = (subspecs == null || subspecs.size() == 2);
    //NMR real/imaginary
    boolean is2D = !getSpectrumAt(0).is1D(); 
    if (is2D || dontUseSubspecs && y1 == y2) {
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
   * Sets the integration ratios that will be displayed
   * 
   * @param ratios
   *        array of the integration ratios
   */
  void setIntegrationRatios(ArrayList<Annotation> ratios) {
    integrationRatios = ratios;
  }

  /**
   * Sets the integration ratios that will be displayed
   * 
   * @param ratios
   *        array of the integration ratios
   */
  void addAnnotation(List<String> args) {
    if (args.size() == 0 || args.size() == 1
        && args.get(0).equalsIgnoreCase("none")) {
      annotations = null;
      lastAnnotation = null;
      return;
    }
    if (args.size() < 4 && lastAnnotation == null)
      lastAnnotation = new ColoredAnnotation(
          (multiScaleData.maxXOnScale + multiScaleData.minXOnScale) / 2,
          (multiScaleData.maxYOnScale + multiScaleData.minYOnScale) / 2, jsvp.getTitle(),
          Color.BLACK, false, false, 0, 0);
    ColoredAnnotation annotation = ColoredAnnotation.getAnnotation(args,
        lastAnnotation);
    if (annotation == null)
      return;
    if (annotations == null && args.size() == 1
        && args.get(0).charAt(0) == '\"') {
      String s = annotation.getText();
      jsvp.setTitle(s);
      getSpectrum().setTitle(s);
      return;
    }
    lastAnnotation = annotation;
    addAnnotation(annotation, false);
  }

  Annotation findAnnotation2D(Coordinate xy) {
    for (int i = annotations.size(); --i >= 0;) {
      Annotation a = annotations.get(i);
      if (isNearby2D(a, xy))
        return a;
    }
    return null;    
  }
  private void addAnnotation(ColoredAnnotation annotation, boolean isToggle) {
    if (annotations == null)
      annotations = new ArrayList<Annotation>();
    boolean removed = false;
    for (int i = annotations.size(); --i >= 0;)
      if (annotation.is2D? isNearby2D(annotations.get(i), annotation) : annotation.equals(annotations.get(i))) {
        removed = true;
        annotations.remove(i);
      }
    if (annotation.getText().length() > 0 && (!removed || !isToggle))
      annotations.add(annotation);
  }

  boolean isNearby2D(Coordinate a1, Coordinate a2) {
    int xp1 = isd.toPixelX(a1.getXVal());
    int yp1 = toPixelY(a1.getYVal());
    int xp2 = isd.toPixelX(a2.getXVal());
    int yp2 = toPixelY(a2.getYVal());
    return (Math.abs(xp1 - xp2) + Math.abs(yp1 - yp2) < 10);
  }

  /**
   * Displays plot in reverse if val is true
   * 
   * @param val
   *        true or false
   */
  void setReversePlot(boolean val) {
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
   * 
   * @param x1
   *        the x value of the coordinate where the highlight should start
   * @param x2
   *        the x value of the coordinate where the highlight should end
   * @param color
   *        the color of the highlight
   */
  void addHighlight(double x1, double x2, Color color) {
    Highlight hl = new Highlight(x1, x2, (color == null ? jsvp.getHighlightColor()
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
  void addHighlight(double x1, double x2) {
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
  void removeHighlight(int index) {
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
  void removeHighlight(double x1, double x2) {
    Highlight hl = new Highlight(x1, x2);
    int index = highlights.indexOf(hl);
    if (index != -1) {
      highlights.remove(index);
    }
  }

  /**
   * Removes all highlights from the display
   */
  void removeAllHighlights() {
    highlights.clear();
  }

  /**
   * Returns the <code>Spectrum</code> at the specified index
   * 
   * @param index
   *        the index of the <code>Spectrum</code>
   * @return the <code>Spectrum</code> at the specified index
   */
  JDXSpectrum getSpectrumAt(int index) {
    return (JDXSpectrum) spectra.get(index);
  }

  /**
   * Returns the Number of Spectra
   * 
   * @return the Number of Spectra
   */
  int getNumberOfSpectra() {
    return nSpectra;
  }

  /**
   * Returns the color of the plot at a certain index
   * 
   * @param index
   *        the index
   * @return the color of the plot
   */
  Color getPlotColor(int index) {
    if (index >= plotColors.length)
      return null;
    return plotColors[index];
  }


  /*----------------------- PAINTING METHODS ---------------------*/

  
  private double fracX = 1, fracY = 1, fX0 = 0, fY0 = 0; // take up full screen
  
  public static void setFractionalPositions(List<JSVGraphSet> graphSets) {
    // for now, just a vertical stack
    int n = graphSets.size();
    double f = 0;
    int n2d = 1;
    for (int i = 0; i < n; i++)
      f += (graphSets.get(i).getSpectrumAt(0).is1D() ? 1 : n2d);
    f = 1 / f;
    double x = 0;
    for (int i = 0; i < n; i++) {
      JSVGraphSet gs = graphSets.get(i);
      double g = (gs.getSpectrumAt(0).is1D() ? f : n2d * f); 
      gs.fracY = g;
      gs.fY0 = x;
      x += g;
    }
  }

  void setPositionForFrame(int width, int height, Insets insets) {
    xPixel00 = (int) (width * fX0) ;
    xPixel11 = (int) (width * (fX0 + fracX)) - 1;
    yPixel00 = (int) (height * fY0) ;
    yPixel11 = (int) (height * (fY0 + fracY)) - 1;
    xPixel0 = xPixel00 + insets.left / (xPixel00 == 0 ? 1 : 2);
    xPixel1 = xPixel11 - insets.right / (xPixel11 > width - 2 ? 1 : 2);
    yPixel0 = yPixel00 + insets.top / (yPixel00 == 0 ? 1 : 2);
    yPixel1 = yPixel11 - insets.bottom / (yPixel11 > height - 2 ? 1 : 2);
    xPixels = xPixel1 - xPixel0 + 1;
    yPixels = yPixel1 - yPixel0 + 1;
    allowYScale &= (fY0 == 1 && fX0 == 1);

  }
  
  public boolean hasPoint(int xPixel, int yPixel) {
    return (xPixel >= xPixel00 && xPixel <= xPixel11 && yPixel >= yPixel00 && yPixel <= yPixel11);
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
  void drawGraph(Graphics g, boolean grid, boolean xunits, boolean yunits,
                 boolean xscale,
                 boolean yscale,
                 boolean drawY0, //!isIntegralDrag
                 int height, int width, 
                 Insets plotAreaInsets, boolean isResized, boolean enableZoom) {
    // for now, at least, we only allow one 2D image
    this.enableZoom = enableZoom;
    setPositionForFrame(width, height, plotAreaInsets);
    JDXSpectrum spec0 = getSpectrumAt(0);
    userYFactor = getSpectrum().getUserYFactor();  
    setScaleFactors(multiScaleData);
    if (!getSpectrumAt(0).is1D() && jsvp.display2D && (isd != null || get2DImage())) {
      setImageWindow();
      width = (int) Math.floor(widthRatio * xPixels * 0.8);
      if (jsvp.display1D) {
        xPixels = width;
        xPixel1 = xPixel0 + xPixels - 1;
      } else {
        xPixels = 0;
        xPixel1 = isd.xPixel0 - 30;
      }
      setScaleFactors(multiScaleData);
    }

    setScaleFactors(multiScaleData);
    int subIndex = spec0.getSubIndex();
    setWidgets(isResized, subIndex);

    if (isd != null)
      draw2DImage(g);
    draw2DUnits(g, width, subIndex, spec0);
    doDraw1DObjects = (isd == null || jsvp.display1D);
    if (doDraw1DObjects) {
      // background stuff
      fillBox(g, xPixel0, yPixel0, xPixel1, yPixel1, jsvp.getPlotAreaColor());
      drawWidgets(g, subIndex);
      if (grid) {
        drawGrid(g, height, width);
      } else {
        g.setColor(jsvp.getGridColor());
        g.drawRect(xPixel0, yPixel0, xPixels, yPixels);
      }
      drawHighlights(g);
      drawPeakTabs(g);
      // scale-related, title, and coordinates
      if (xscale)
        drawXScale(g, height, width);
      if (yscale && allowYScale)
        drawYScale(g, height, width);
      if (xunits)
        drawXUnits(g, width);
      if (yunits && allowYScale)
        drawYUnits(g, width);

      // the graphs
      for (int i = nSpectra; --i >= 0;)
        drawPlot(g, i, height, width, grid, drawY0);

      // over-spectrum stuff    
      if (integrationRatios != null)
        drawAnnotations(g, height, width, integrationRatios, jsvp
            .getIntegralPlotColor());
      drawIntegralValue(g, width);
    } else {
      drawWidgets(g, subIndex);
    }
    if (annotations != null)
      drawAnnotations(g, height, width, annotations, null);

  }

  private void draw2DUnits(Graphics g, int width, int subIndex,
                           JDXSpectrum spec0) {
    if (subIndex >= 0 && isd != null) {
      g.setColor(plotColors[0]);
      drawUnits(g, width, spec0.nucleusX, isd.xPixel1 + 5, yPixel1,
          1, 1.0);
      drawUnits(g, width, spec0.nucleusY, isd.xPixel0 - 5, yPixel0, 1, 0);
    }
  }

  private void drawHighlights(Graphics g) {
    for (int i = 0; i < highlights.size(); i++) {
      Highlight hl = highlights.get(i);
      drawBar(g, hl.getStartX(), hl.getEndX(), jsvp.setHighlightColor(hl.getColor()),
          true);
    }
  }

  private void drawPeakTabs(Graphics g) {
    ArrayList<PeakInfo> list = (nSpectra == 1
        || getSpectrum().getIntegrationGraph() != null ? getSpectrum()
        .getPeakList() : null);
    if (list != null && list.size() > 0) {
      Color highlightColor = jsvp.getHighlightColor();
      for (int i = list.size(); --i >= 0;) {
        PeakInfo pi = list.get(i);
        double xMin = pi.getXMin();
        double xMax = pi.getXMax();
        if (xMin != xMax) {
          drawBar(g, xMin, xMax, highlightColor, false);
        }
      }
    }
  }

  /**
   * PlotWidgets are zoom boxes and slider points that are draggable.
   * Some are derived from others (center points and the 2D subIndex pointer).
   * The first time through, we have to create new pins.
   * When the frame is resized, we need to reset their positions along the slider
   * based on their values, and we need to also move the sliders to the right place.
   * 
   * @param isResized
   * @param subIndex
   */
  private void setWidgets(boolean isResized, int subIndex) {
    if (isResized) {
      if (zoomBox1D == null)
        newPins();
      else
        resetPinPositions();
    }
    setDerivedPins(subIndex);
    setPinSliderPositions();
  }

  /**
   * Create new pins and set their default values. 
   * Note that we are making a distinction between multiScaleData.minY and
   * multiScaleData.minYOnScale. For X these are now the same, but for Y they
   * are not. This produces a nicer grid, but also an odd jumpiness in the Y slider
   * that is not totally predictable.
   * 
   * @param subIndex
   */
  private void newPins() {
    zoomBox1D = new PlotWidget("zoomBox1D");
    pin1Dx0 = new PlotWidget("pin1Dx0");
    pin1Dx1 = new PlotWidget("pin1Dx1");
    pin1Dy0 = new PlotWidget("pin1Dy0");
    pin1Dy1 = new PlotWidget("pin1Dy1");
    pin1Dx01 = new PlotWidget("pin1Dx01");
    pin1Dy01 = new PlotWidget("pin1Dy01");
    if (isd != null) {
      zoomBox2D = new PlotWidget("zoomBox2D");
      // these pins only present when no 1D is present
      pin2Dx0 = new PlotWidget("pin2Dx0");
      pin2Dx1 = new PlotWidget("pin2Dx1");
      pin2Dy0 = new PlotWidget("pin2Dy0");
      pin2Dy1 = new PlotWidget("pin2Dy1");
      pin2Dx01 = new PlotWidget("pin2Dx01");
      pin2Dy01 = new PlotWidget("pin2Dy01");
      // these pins only present when 1D and 2D
      cur2Dx0 = new PlotWidget("cur2Dx0");
      // these pins only present whenever 2D present
      cur2Dx1 = new PlotWidget("cur2Dx1");
      cur2Dy = new PlotWidget("cur2Dy");
      pin2Dy0.setY(0, isd.toPixelY0(0));
      int n = getSpectrumAt(0).getSubSpectra().size();
      pin2Dy1.setY(n, isd.toPixelY0(n));
    }
    pin1Dx0.setX(multiScaleData.minX, toPixelX0(multiScaleData.minX));
    pin1Dx1.setX(multiScaleData.maxX, toPixelX0(multiScaleData.maxX));
    pin1Dy0.setY(multiScaleData.minY, toPixelY0(multiScaleData.minY));
    pin1Dy1.setY(multiScaleData.maxY, toPixelY0(multiScaleData.maxY));
    widgets = new PlotWidget[] { zoomBox1D, zoomBox2D, 
        pin1Dx0, pin1Dx01, pin1Dx1,
        pin1Dy0, pin1Dy01, pin1Dy1, 
        pin2Dx0, pin2Dx01, pin2Dx1,
        pin2Dy0, pin2Dy01, pin2Dy1, 
        cur2Dx0, cur2Dx1, cur2Dy };
  }

  /**
   * use the pin values to find their positions along the slider
   * 
   */
  private void resetPinPositions() {
    pin1Dx0.setX(pin1Dx0.getXVal(), toPixelX0(pin1Dx0.getXVal())); 
    pin1Dx1.setX(pin1Dx1.getXVal(), toPixelX0(pin1Dx1.getXVal()));
    pin1Dy0.setY(pin1Dy0.getYVal(), toPixelY0(pin1Dy0.getYVal()));
    pin1Dy1.setY(pin1Dy1.getYVal(), toPixelY0(pin1Dy1.getYVal()));
    if (isd != null) {
      pin2Dy0.setY(pin2Dy0.getYVal(), isd.toPixelY0(pin2Dy0.getYVal()));
      pin2Dy1.setY(pin2Dy1.getYVal(), isd.toPixelY0(pin2Dy1.getYVal()));
    }
  }

  /**
   * realign sliders to proper locations after resizing
   * 
   */
  private void setPinSliderPositions() {
    pin1Dx0.yPixel0 = pin1Dx1.yPixel0 = pin1Dx01.yPixel0 = yPixel0 - 5;
    pin1Dx0.yPixel1 = pin1Dx1.yPixel1 = pin1Dx01.yPixel1 = yPixel0;
    pin1Dy0.xPixel0 = pin1Dy1.xPixel0 = pin1Dy01.xPixel0 = xPixel1 + 5;
    pin1Dy0.xPixel1 = pin1Dy1.xPixel1 = pin1Dy01.xPixel1 = xPixel1;
    if (isd != null) {
      pin2Dx0.yPixel0 = pin2Dx1.yPixel0 = pin2Dx01.yPixel0 = yPixel0 - 5;
      pin2Dx0.yPixel1 = pin2Dx1.yPixel1 = pin2Dx01.yPixel1 = yPixel0;
      pin2Dy0.xPixel0 = pin2Dy1.xPixel0 = pin2Dy01.xPixel0 = isd.xPixel1 + 5;
      pin2Dy0.xPixel1 = pin2Dy1.xPixel1 = pin2Dy01.xPixel1 = isd.xPixel1;
      cur2Dx0.yPixel0 = cur2Dx1.yPixel0 = yPixel1 + 15;
      cur2Dx0.yPixel1 = cur2Dx1.yPixel1 = yPixel0 - 5;
      cur2Dx0.yPixel0 = cur2Dx1.yPixel0 = yPixel1 + 15;
      cur2Dx1.yPixel1 = cur2Dx1.yPixel1 = yPixel0 - 5;
      cur2Dy.xPixel0 = (doDraw1DObjects ? (xPixel1 + isd.xPixel0) / 2 : isd.xPixel0 - 15);
      cur2Dy.xPixel1 = isd.xPixel1 + 5;
    }
  }

  /**
   * The center pins and the 2D subspectrum slider values are derived from other
   * data
   * 
   * @param subIndex
   */
  private void setDerivedPins(int subIndex) {
    pin1Dx01.setX(0, (pin1Dx0.xPixel0 + pin1Dx1.xPixel0) / 2);
    pin1Dy01.setY(0, (pin1Dy0.yPixel0 + pin1Dy1.yPixel0) / 2);
    if (isd == null)
      return;
    
    double x = pin1Dx0.getXVal();
    cur2Dx0.setX(x, isd.toPixelX(x));
    x = pin1Dx1.getXVal();
    cur2Dx1.setX(x, isd.toPixelX(x));
    
    x = isd.toX(isd.xPixel0);
    pin2Dx0.setX(x, isd.toPixelX0(x));
    x = isd.toX(isd.xPixel1);
    pin2Dx1.setX(x, isd.toPixelX0(x));
    pin2Dx01.setX(0, (pin2Dx0.xPixel0 + pin2Dx1.xPixel0) / 2);

    double y = isd.imageHeight - 1 - isd.yView1;
    pin2Dy0.setY(y, isd.toPixelY0(y));
    y = isd.imageHeight - 1 - isd.yView2;
    pin2Dy1.setY(y, isd.toPixelY0(y));
    pin2Dy01.setY(0, (pin2Dy0.yPixel0 + pin2Dy1.yPixel0) / 2);

    cur2Dy.yPixel0 = cur2Dy.yPixel1 = isd.toPixelY(subIndex);
  }

  /**
   * 
   * Draw sliders, pins, and zoom boxes (only one of which would ever be drawn)
   * 
   * @param g
   * @param subIndex
   */
  private void drawWidgets(Graphics g, int subIndex) {
    // top/side slider bar backgrounds
    Color gridColor = jsvp.getGridColor();
    if (doDraw1DObjects) {
      fillBox(g, xPixel0, pin1Dx0.yPixel1, xPixel1,
          pin1Dx1.yPixel1 + 2, gridColor);
      fillBox(g, pin1Dx0.xPixel0, pin1Dx0.yPixel1, pin1Dx1.xPixel0,
          pin1Dx1.yPixel1 + 2, plotColors[0]);
    } else {
      fillBox(g, isd.xPixel0, pin2Dx0.yPixel1, isd.xPixel1,
          pin2Dx0.yPixel1 + 2, gridColor);
      fillBox(g, pin2Dx0.xPixel0, pin2Dx0.yPixel1, pin2Dx1.xPixel0,
          pin2Dx1.yPixel1 + 2, plotColors[0]);
      fillBox(g, pin2Dy0.xPixel1, yPixel1, pin2Dy1.xPixel1 + 2,
          yPixel0, gridColor);
      fillBox(g, pin2Dy0.xPixel1, pin2Dy0.yPixel1, pin2Dy1.xPixel1 + 2,
          pin2Dy1.yPixel0, plotColors[0]);
    }
    fillBox(g, pin1Dy0.xPixel1, yPixel1, pin1Dy1.xPixel1 + 2,
        yPixel0, gridColor);
    fillBox(g, pin1Dy0.xPixel1, pin1Dy0.yPixel1, pin1Dy1.xPixel1 + 2,
        pin1Dy1.yPixel0, plotColors[0]);

    for (int i = 0; i < widgets.length; i++) {
      PlotWidget pw = widgets[i];
      if (pw == null)
        continue;
      if (!pw.isPinOrCursor && !enableZoom)
        continue;
      if (pw.is2D) {
        if (pw == cur2Dx0 && !doDraw1DObjects)// || pw.is2Donly && doDraw1DObjects)
          continue;
      } else if (!doDraw1DObjects && pw != pin1Dy0 && pw != pin1Dy1
          && pw != pin1Dy01) {
        continue;
      }
      g.setColor(pw.isPinOrCursor ? plotColors[0] : jsvp.getZoomBoxColor());
      drawWidget(g, pw);
    }
  }

  private boolean isInTopBar(int xPixel, int yPixel) {
    return (xPixel == fixX(xPixel) && yPixel > pin1Dx0.yPixel0 - 2 && yPixel < pin1Dx0.yPixel1);
  }

  private boolean isInTopBar2D(int xPixel, int yPixel) {
    return (isd != null && xPixel == isd.fixX(xPixel) && yPixel > pin2Dx0.yPixel0 - 2 && yPixel < pin2Dx0.yPixel1);
  }

  private boolean isInRightBar(int xPixel, int yPixel) {
    return (yPixel == fixY(yPixel) && xPixel > pin1Dy0.xPixel1 && xPixel < pin1Dy0.xPixel0 + 2);
  }

  private boolean isInRightBar2D(int xPixel, int yPixel) {
    return (isd != null && yPixel == fixY(yPixel) && xPixel > pin2Dy0.xPixel1 && xPixel < pin2Dy0.xPixel0 + 2);
  }

  private void drawWidget(Graphics g, PlotWidget pw) {
    if (pw == null)
      return;
    if (pw.isPinOrCursor) {
      g.setColor(plotColors[0]);
      g.drawLine(pw.xPixel0, pw.yPixel0, pw.xPixel1, pw.yPixel1);
      drawHandle(g, pw.xPixel0, pw.yPixel0);
    } else if (pw.xPixel1 != pw.xPixel0) {
      fillBox(g, pw.xPixel0, pw.yPixel0, pw.xPixel1, pw.yPixel1, jsvp.getZoomBoxColor());
    }
  }

  private void fillBox(Graphics g, int x0, int y0, int x1, int y1, Color color) {
    if (color != null)
      g.setColor(color);
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
    fillBox(g, x1, yPixel0, x2, yPixel0
        + (isFullHeight ? yPixels : 5), color);
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
  private void drawPlot(Graphics g, int index, int height, int width,
                        boolean gridOn, boolean drawY0) {
    // Check if specInfo in null or xyCoords is null
    //Coordinate[] xyCoords = spectra[index].getXYCoords();
    Coordinate[] xyCoords = spectra.get(index).getXYCoords();

    g.setColor(index == 1 && getSpectrum().getIntegrationGraph() != null
        ? jsvp.getIntegralPlotColor() : plotColors[index]);

    // Check if revPLot on
    int y0 = toPixelY(0);
    if (drawY0 || index != 0 || y0 != fixY(y0))
      y0 = -1;
    userYFactor = spectra.get(index).getUserYFactor();
    if (spectra.get(index).isContinuous()) {
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
        if (y0 >= 0 &&  x1 >= zoomBox1D.xPixel0 && x1 <= zoomBox1D.xPixel1) {
          g.setColor(jsvp.getIntegralPlotColor());
          g.drawLine(x1, y0, x1, y1);
          g.setColor(plotColors[0]);
          continue;
        }
        if (y1 == y2 && (y1 == yPixel0 || y1 == yPixel1))
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
        if (y1 == y2 && (y1 == yPixel0 || y1 == yPixel1))
          continue;
        g.drawLine(x1, y1, x1, y2);
      }
      if (multiScaleData.isYZeroOnScale()) {
        int y = toPixelY(0);
        g.drawLine(xPixel1, y, xPixel0, y);
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
    g.setColor(jsvp.getGridColor());
    double lastX;
    if (Double.isNaN(multiScaleData.firstX)) {
      lastX = multiScaleData.maxXOnScale + multiScaleData.xStep / 2;
      for (double val = multiScaleData.minXOnScale; val < lastX; val += multiScaleData.xStep) {
        int x = toPixelX(val);
        g.drawLine(x, yPixel0, x, yPixel1);
      }
    } else {
      lastX = multiScaleData.maxXOnScale * 1.0001;
      for (double val = multiScaleData.firstX; val <= lastX; val += multiScaleData.xStep) {
        int x = toPixelX(val);
        g.drawLine(x, yPixel0, x, yPixel1);
      }
    }
    for (double val = multiScaleData.minYOnScale; val < multiScaleData.maxYOnScale
        + multiScaleData.yStep / 2; val += multiScaleData.yStep) {
      int y = toPixelY(val);
      if (y == fixY(y))
        g.drawLine(xPixel0, y, xPixel1, y);
    }
  }

  private void drawIntegralValue(Graphics g, int width) {
    List<Integral> integrals = getSpectrum().getIntegrals();
    if (integrals == null)
      return;
    jsvp.setFont(g, width, Font.BOLD, 12, false);
    FontMetrics fm = g.getFontMetrics();
    NumberFormat formatter = jsvp.getFormatter("#0.0");
    g.setColor(jsvp.getIntegralPlotColor());

    for (int i = integrals.size(); --i >= 0;) {
      Integral in = integrals.get(i);
      if (in.value == 0)
        continue;
      String s = "  " + formatter.format(Math.abs(in.value));
      int x = toPixelX(in.x2);
      int y1 = toPixelY(in.y1);
      int y2 = toPixelY(in.y2);
      if (x != fixX(x))
        continue;
      g.drawLine(x, y1, x, y2);
      g.drawLine(x + 1, y1, x + 1, y2);
      g.drawString(s, x, (y1 + y2) / 2  + fm.getHeight() / 3);
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

    if (multiScaleData.hashNums[0] <= 0)
      hashX = hash1.substring(0, Math.abs(multiScaleData.hashNums[0]) + 3);

    NumberFormat formatter = jsvp.getFormatter(hashX);
    jsvp.setFont(g, width, Font.PLAIN, 12, false);
    FontMetrics fm = g.getFontMetrics();    
    int y1 = yPixel1;
    int y2 = yPixel1 + 3;
    double maxWidth = Math.abs((toPixelX(multiScaleData.xStep) - toPixelX(0)) * 0.95);
    double lastX;
    if (Double.isNaN(multiScaleData.firstX)) {
      lastX = multiScaleData.maxXOnScale + multiScaleData.xStep / 2;
      for (double val = multiScaleData.minXOnScale, vald = multiScaleData.maxXOnScale; val < lastX; val += multiScaleData.xStep, vald -= multiScaleData.xStep) {
        int x = (int) (xPixel0 + (((drawXAxisLeftToRight ? val : vald) - multiScaleData.minXOnScale) / xFactorForScale));
        g.setColor(jsvp.getGridColor());
        g.drawLine(x, y1, x, y2);
        g.setColor(jsvp.getScaleColor());
        String s = formatter.format(val);
        int w = fm.stringWidth(s);
        g.drawString(s, x - w / 2, y2 + fm.getHeight());
      }
    } else {
      lastX = multiScaleData.maxXOnScale * 1.0001;
      for (double val = multiScaleData.firstX; val <= lastX; val += multiScaleData.xStep) {
        int x = toPixelX(val);
        g.setColor(jsvp.getGridColor());
        g.drawLine(x, y1, x, y2);
        g.setColor(jsvp.getScaleColor());
        String s = formatter.format(val);
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
    NumberFormat formatter = jsvp.getFormatter(hashY);
    jsvp.setFont(g, width, Font.PLAIN, 12, false); 
    FontMetrics fm = g.getFontMetrics();
    double max = multiScaleData.maxYOnScale + multiScaleData.yStep / 2;
    for (double val = multiScaleData.minYOnScale; val < max; val += multiScaleData.yStep) {
      int x1 = (int) xPixel0;
      int y = toPixelY(val * userYFactor);
      g.setColor(jsvp.getGridColor());
      g.drawLine(x1, y, x1 - 3, y);
      g.setColor(jsvp.getScaleColor());
      String s = formatter.format(val);
      g.drawString(s, (x1 - 4 - fm.stringWidth(s)), y + fm.getHeight() / 3);
    }
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
    drawUnits(g, width, spectra.get(0).getXUnits(), xPixel1, yPixel1 + 5, 0, 1);
  }

  private void drawUnits(Graphics g, int width, String s,
                         int x, int y, double hOff, double vOff) {
    g.setColor(jsvp.getUnitsColor());
    jsvp.setFont(g, width, Font.ITALIC, 10, false);
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
    drawUnits(g, width, spectra.get(0).getYUnits(), 5, yPixel0, 0, -1);
  }

  // determine whether there are any ratio annotations to draw
  private void drawAnnotations(Graphics g, int height, int width,
                               ArrayList<Annotation> annotations, Color color) {
    jsvp.setFont(g, width, Font.BOLD, 12, false);
    for (int i = annotations.size(); --i >= 0;) {
      Annotation note = annotations.get(i);
      color = (note instanceof ColoredAnnotation ? ((ColoredAnnotation) note).getColor()
          : color);
      if (color == null)
        color = Color.BLACK;
      g.setColor(color);
      int x = (note.is2D ? isd.toPixelX(note.getXVal()) : toPixelX(note.getXVal()));
      int y = (note.isPixels() ? (int) (yPixel0 + 10 - note.getYVal()) 
          : note.is2D ? isd.toPixelY((int) note.getYVal()) : toPixelY(note.getYVal()));
      g.drawString(note.getText(), x + note.offsetX, y - note.offsetY);
    }
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
    if (x1 == 0 && x2 == 0) {
      newPins();
    } else {
      doZoom(x1, y1, x2, y2, false, true, false);
      return;
    }
    isd = null;
  }

  private void setScaleFactors(MultiScaleData multiScaleData) {
    xFactorForScale = (multiScaleData.maxXOnScale - multiScaleData.minXOnScale)
        / xPixels;
    yFactorForScale = (multiScaleData.maxYOnScale - multiScaleData.minYOnScale)
        / yPixels;
    minYScale = multiScaleData.minYOnScale;
  }

  private int fixX(int xPixel) {
    return Coordinate.intoRange(xPixel, xPixel0, xPixel1);
  }

  private int toPixelX(double dx) {
    int x = (int) ((dx - multiScaleData.minXOnScale) / xFactorForScale);
    return (int) (drawXAxisLeftToRight ? xPixel0 + x
        : xPixel1 - x);
  }

  private int toPixelX0(double x) {
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxXOnScale - multiScaleData.minXOnScale) / xPixels;
    return (int) (!drawXAxisLeftToRight ? xPixel1 - (multiScaleData.maxXOnScale - x) / factor
        : xPixel1 - (x - multiScaleData.minXOnScale) / factor);
  }
  
  private double toX(int xPixel) {
    if (isd != null && isd.isXWithinRange(xPixel))
      return isd.toX(xPixel);
    xPixel = fixX(xPixel);
    return (drawXAxisLeftToRight 
         ?  multiScaleData.maxXOnScale - (xPixel1 - xPixel) * xFactorForScale 
         :  multiScaleData.minXOnScale + (xPixel1 - xPixel) * xFactorForScale);
  }

  private double toX0(int xPixel) {
    xPixel = fixX(xPixel);
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxXOnScale - multiScaleData.minXOnScale) / xPixels;
    return (!drawXAxisLeftToRight ? multiScaleData.maxXOnScale
        - (xPixel1 - xPixel) * factor : multiScaleData.minXOnScale
        + (xPixel1 - xPixel) * factor);
  }

  private int fixY(int yPixel) {
    return Coordinate.intoRange(yPixel, yPixel0, yPixel1);
  }

  private int toPixelY(double yVal) {
    return (Double.isNaN(yVal) ? Integer.MIN_VALUE 
        : yPixel1 - (int) ((yVal * userYFactor - minYScale) / yFactorForScale));
  }

  private int toPixelY0(double y) {
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxYOnScale - multiScaleData.minYOnScale) / yPixels;
    return fixY((int) (yPixel0 + (multiScaleData.maxYOnScale - y) / factor));
  }
  
  private double toY(int yPixel) {
    return multiScaleData.maxYOnScale + (yPixel0 - yPixel) * yFactorForScale;
  }
  
  private double toY0(int yPixel) {
    yPixel = fixY(yPixel);
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxYOnScale - multiScaleData.minYOnScale) / yPixels;
    double y = multiScaleData.maxYOnScale + (yPixel0 - yPixel) * factor;
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
  private synchronized void doZoom(double initX, double initY, double finalX, double finalY,
                      boolean doRepaint, boolean addZoom, boolean checkRange) {

    if (!enableZoom)
      return;
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

    boolean is2DYScaleChange = (isd != null && (isd.minZ != initY || isd.maxZ != finalY));
    
    // determine if the range of the area selected for zooming is within the plot
    // area and if not ensure that it is

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
    graphsTemp.clear();
    List<JDXSpectrum> subspecs = getSpectrumAt(0).getSubSpectra();
    boolean dontUseSubspecs = (subspecs == null || subspecs.size() == 2);
    //NMR real/imaginary
    boolean is2D = !getSpectrumAt(0).is1D(); 
    if (!is2D && !dontUseSubspecs) {
      graphsTemp.add(getSpectrum());
      if (!multiScaleData.setDataPointIndices(graphsTemp, initX, finalX,
          JSVPanel.minNumOfPointsForZoom, startIndices, endIndices, false))
        return;
    } else {
      if (!multiScaleData.setDataPointIndices(spectra, initX, finalX,
          JSVPanel.minNumOfPointsForZoom, startIndices, endIndices, false))
        return;
    }
    
    getMultiScaleData(initX, finalX, initY, finalY, startIndices, endIndices);
    pin1Dx0.setX(initX, toPixelX0(initX));
    pin1Dx1.setX(finalX, toPixelX0(finalX));
    pin1Dy0.setY(initY, toPixelY0(initY));
    pin1Dy1.setY(finalY, toPixelY0(finalY));
    if (isd != null) {
      int isub = getSpectrumAt(0).getSubIndex();
      int ifix = isd.fixSubIndex(isub);
      if (ifix != isub)
        setCurrentSubSpectrum(ifix);
      if (is2DYScaleChange)
        update2dImage(true);
    }
    if (addZoom)
      addCurrentZoom();
    if (doRepaint)
      jsvp.repaint();
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
  void reset() {
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
    jsvp.refresh();
  }

  /**
   * Clears all views in the zoom list
   */
  void clearViews() {
    reset();
    // leave first zoom
    for (int i = zoomInfoList.size(); --i >= 1; ) 
      zoomInfoList.remove(i);
  }

  /**
   * Displays the previous view zoomed
   */
  void previousView() {
    if (currentZoomIndex > 0)
      setZoomTo(currentZoomIndex - 1);
  }

  /**
   * Displays the next view zoomed
   */
  void nextView() {
    if (currentZoomIndex + 1 < zoomInfoList.size())
      setZoomTo(currentZoomIndex + 1);
  }


  /**
   * Private class to represent a Highlighted region of the spectrum display
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
    Highlight(double x1, double x2) {
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
    Highlight(double x1, double x2, Color color) {
      this(x1, x2);
      this.color = color;
    }

    /**
     * Returns the x coordinate where the highlighted region starts
     * 
     * @return the x coordinate where the highlighted region starts
     */
    double getStartX() {
      return x1;
    }

    /**
     * Returns the x coordinate where the highlighted region ends
     * 
     * @return the x coordinate where the highlighted region ends
     */
    double getEndX() {
      return x2;
    }

    /**
     * Returns the color of the highlighted region
     * 
     * @return the color of the highlighted region
     */
    Color getColor() {
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

  PlotWidget getPinSelected(int xPixel, int yPixel) {
    if (widgets != null)
      for (int i = 0; i < widgets.length; i++)
        if (widgets[i] != null && widgets[i].isPinOrCursor
            && widgets[i].selected(xPixel, yPixel)) {
          return widgets[i];
        }
    return null;
  }

  private void set2DCrossHairs(int xPixel, int yPixel) {
    if (xPixel == isd.fixX(xPixel) && yPixel == fixY(yPixel)) {
      pin1Dx1.setX(isd.toX(xPixel), toPixelX(isd.toX(xPixel)));
      cur2Dx1.setX(isd.toX(xPixel), xPixel);
      setCurrentSubSpectrum(isd.toSubspectrumIndex(yPixel));
    }      
  }

  private void reset2D(boolean isX) {
    if (isX) {
      isd.setView0(isd.xPixel0, pin2Dy0.yPixel0, isd.xPixel1, pin2Dy1.yPixel0);
      doZoom(isd.minX, multiScaleData.minY, isd.maxX, multiScaleData.maxY,
          true, true, false);
    } else {
      isd.setView0(pin2Dx0.xPixel0, isd.yPixel0, pin2Dx1.xPixel0, isd.yPixel1);
      jsvp.repaint();
    }
  }

  private boolean setAnnotationText(Annotation a) {
    String sval = (String) JOptionPane.showInputDialog(null, "New text?",
        "Set Label", JOptionPane.PLAIN_MESSAGE, null, null, a.getText());
    if (sval == null)
      return false;
    if (sval.length() == 0)
      annotations.remove(a);
    else
      a.setText(sval);
    return true;
  }

  private void setWidgetValueByUser(PlotWidget pw) {
    String sval;
    if (pw == cur2Dy)
      sval = "" + isd.toSubspectrumIndex(pw.yPixel0);
    else if (pw == pin1Dx01)
      sval = "" + Math.min(pin1Dx0.getXVal(), pin1Dx1.getXVal()) + " - "
          + Math.max(pin1Dx0.getXVal(), pin1Dx1.getXVal());
    else if (pw == pin1Dy01)
      sval = "" + Math.min(pin1Dy0.getYVal(), pin1Dy1.getYVal()) + " - "
          + Math.max(pin1Dy0.getYVal(), pin1Dy1.getYVal());
    else if (pw == pin2Dx01)
      sval = "" + Math.min(pin2Dx0.getXVal(), pin2Dx1.getXVal()) + " - "
          + Math.max(pin2Dx0.getXVal(), pin2Dx1.getXVal());
    else if (pw == pin2Dy01)
      sval = "" + (int) Math.min(pin2Dy0.getYVal(), pin2Dy1.getYVal()) + " - "
          + (int) Math.max(pin2Dy0.getYVal(), pin2Dy1.getYVal());
    else
      sval = "" + pw.getValue();
    sval = (String) JOptionPane.showInputDialog(null, "New value?",
        "Set Slider", JOptionPane.PLAIN_MESSAGE, null, null, sval);
    if (sval == null)
      return;
    sval = sval.trim();
    try {
      if (pw == pin1Dx01 || pw == pin1Dy01 || pw == pin2Dx01 || pw == pin2Dy01) {
        int pt = sval.indexOf("-", 1);
        if (pt < 0)
          return;
        double val1 = Double.valueOf(sval.substring(0, pt));
        double val2 = Double.valueOf(sval.substring(pt + 1));
        if (pw == pin1Dx01) {
          doZoom(val1, pin1Dy0.getYVal(), val2, pin1Dy1.getYVal(), true, true,
              false);
        } else if (pw == pin1Dy01) {
          doZoom(pin1Dx0.getXVal(), val1, pin1Dx1.getXVal(), val2, true, true,
              false);
        } else if (pw == pin2Dx01) {
          isd.setView0(isd.toPixelX0(val1), pin2Dy0.yPixel0, isd.toPixelX0(val2), pin2Dy1.yPixel0);
          doZoom(val1, pin1Dy0.getYVal(), val2, pin1Dy1.getYVal(), true, true,
              false);
        } else if (pw == pin2Dy01) {
          isd.setView0(pin2Dx0.xPixel0, isd.toPixelY0(val1), pin2Dx1.xPixel0, isd.toPixelY0(val2));
          doZoom(isd.toX(isd.xPixel0), multiScaleData.minY, isd.toX(isd.xPixel0 + isd.xPixels - 1), multiScaleData.maxY, true, true, false);
        }
      } else {
        double val = Double.valueOf(sval);
        if (pw.isXtype) {
          double val2 = (pw == pin1Dx0 || pw == cur2Dx0 || pw == pin2Dx0 ? pin1Dx1
              .getXVal()
              : pin1Dx0.getXVal());
          doZoom(val, pin1Dy0.getYVal(), val2, pin1Dy1.getYVal(), true, true,
              false);
        } else if (pw == cur2Dy) {
          setCurrentSubSpectrum((int) val);
          jsvp.repaint();
        } else if (pw == pin2Dy0 || pw == pin2Dy1) {
          int val2 = (pw == pin2Dy0 ? pin2Dy1.yPixel0 : pin2Dy0.yPixel0);
          isd.setView0(pin2Dx0.xPixel0, isd.toPixelY((int) val), pin2Dx1.xPixel0, val2);
          jsvp.repaint();
        } else {
          double val2 = (pw == pin1Dy0 ? pin1Dy1.getYVal() : pin1Dy0.getYVal());
          doZoom(pin1Dx0.getXVal(), val, pin1Dx1.getXVal(), val2, true, true,
              false);
        }
      }
    } catch (Exception e) {
    }
  }

  private void clearIntegrals() {
    checkIntegral(Double.NaN, 0, false);
    jsvp.repaint();
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
    NumberFormat formatterX = jsvp.getFormatter(hashX);
    String hashY = "#";
    if (multiScaleData.hashNums[1] <= 0)
      hashY = hash1.substring(0, Math.abs(multiScaleData.hashNums[1]) + 3);
    NumberFormat formatterY = jsvp.getFormatter(hashY);

    PlotWidget pw = getPinSelected(xPixel, yPixel);
    if (pw != null) {
      if (jsvp.startupPinTip != null) {
        jsvp.setToolTipText(jsvp.startupPinTip);
        jsvp.startupPinTip = null;
        return;
      }
      String s;
      if (pw == pin1Dx01 || pw == pin2Dx01) {
        s = formatterX.format(Math.min(pin1Dx0.getXVal(), pin1Dx1.getXVal())) + " - "
            + formatterX.format(Math.max(pin1Dx0.getXVal(), pin1Dx1.getXVal()));
      } else if (pw == pin1Dy01) {
        s = formatterY.format(Math.min(pin1Dy0.getYVal(), pin1Dy1.getYVal())) + " - "
            + formatterX.format(Math.max(pin1Dy0.getYVal(), pin1Dy1.getYVal()));
      } else if (pw == cur2Dy) {
        int isub = isd.toSubspectrumIndex(pw.yPixel0);
        s = get2DYLabel(isub, formatterX);
      } else if (pw == pin2Dy01) {
        s = "" + (int) Math.min(pin2Dy0.getYVal(), pin2Dy1.getYVal()) + " - "
        + (int) Math.max(pin2Dy0.getYVal(), pin2Dy1.getYVal());
      } else if (pw.isXtype) {
        s = formatterX.format(pw.getXVal());
      } else if (pw.is2D) {
        s = "" + (int) pw.getYVal();
      } else {
        s = formatterY.format(pw.getYVal());
      }
      jsvp.setToolTipText(s);
      return;
    }

    if (isd != null && isd.fixX(xPixel) == xPixel && fixY(yPixel) == yPixel) {
      int isub = isd.toSubspectrumIndex(yPixel);
      String s = formatterX.format(isd.toX(xPixel)) + " " + getSpectrum().getXUnits() + ",  "
          + get2DYLabel(isub, formatterX);
      jsvp.setToolTipText(jsvp.display1D ? s : "");
      jsvp.coordStr = s;
      return;
    }

    if (isd != null && !jsvp.display1D) {
      jsvp.setToolTipText("");
      jsvp.coordStr = "";
      return;      
    }
    double xPt = toX(fixX(xPixel));
    String xx = formatterX.format(xPt);

    double yPt = (isd != null && isd.isXWithinRange(xPixel) ? isd
        .toSubspectrumIndex(fixY(yPixel)) : toY(fixY(yPixel)));
    formatterY = jsvp.getFormatter(hashY);
    jsvp.coordStr = "(" + xx + ", " + formatterY.format(yPt) + ")";
    if (xPixel != fixX(xPixel) || yPixel != fixY(yPixel)) {
      yPt = Double.NaN;
    } else if (nSpectra == 1) {
// I have no idea what I was thinking here...
//      if (!getSpectrum().isHNMR()) {
//        yPt = spectra[0].getPercentYValueAt(xPt);
//        xx += ", " + formatterY.format(yPt);
//      }
    } else if (getSpectrum().getIntegrationGraph() != null) {
      yPt = spectra.get(1).getPercentYValueAt(xPt);
      xx += ", " + jsvp.getFormatter("#0.0").format(yPt);
    }
    jsvp.setToolTipText(Double.isNaN(yPt) ? null : xx);
  }

  private String get2DYLabel(int isub, NumberFormat formatterX) {
    JDXSpectrum spec = getSpectrumAt(0).getSubSpectra().get(isub);
    return formatterX.format(spec.getY2D()) + (spec.y2DUnits.equals("HZ") ?  
        " HZ (" + formatterX.format(spec.getY2DPPM()) + " PPM)" : "");
  }

 void toPeak(int istep) {
    istep *= (drawXAxisLeftToRight ? 1 : -1);
    JDXSpectrum spec = getSpectrum();
    jsvp.coordClicked = new Coordinate(jsvp.lastClickX, 0);
    jsvp.coordsClicked = spec.getXYCoords();
    int iPeak = spec.setNextPeak(jsvp.coordClicked, istep);
    if (iPeak < 0)
      return;
    PeakInfo peak = spec.getPeakList().get(iPeak);
    spec.setSelectedPeak(peak);
    jsvp.coordClicked.setXVal(jsvp.lastClickX = peak.getX());
    jsvp.notifyListeners(new PeakPickedEvent(jsvp, jsvp.coordClicked, peak
        .getStringInfo()));
  }

  void advanceSubSpectrum(int i) {
    getSpectrumAt(0).advanceSubSpectrum(i);
    if (getSpectrumAt(0).isForcedSubset())
      multiScaleData.setXRange(getSpectrum());
    notifySubSpectrumChange();
  }

  private void notifySubSpectrumChange() {
    jsvp.notifyListeners(getSpectrum().getTitleLabel());        
  }

  void setCurrentSubSpectrum(int i) {
    getSpectrumAt(0).setCurrentSubSpectrum(i);
    if (getSpectrumAt(0).isForcedSubset())
      multiScaleData.setXRange(getSpectrum());
    notifySubSpectrumChange();
  }
  
  void scaleYBy(double factor) {
    if (!allowYScale)
      return;
    double factor1 = factor;
    double factor2 = factor;
    switch (getSpectrum().getYScaleType()) {
    case JDXDataObject.SCALE_NONE:
      return;
    case JDXDataObject.SCALE_TOP:
      factor1 = 1;
      break;
    case JDXDataObject.SCALE_BOTTOM:
      factor2 = 1;
      break;
    }
    doZoom(multiScaleData.minX, multiScaleData.minY / factor1,
        multiScaleData.maxX, multiScaleData.maxY / factor2, true, true, false);
  }

  static JSVPanel taConvert(JSVPanel jsvp, int mode) {
    if (jsvp.getNumberOfSpectraTotal() != 1)
      return null;
    JDXSpectrum spectrum = JDXSpectrum.taConvert(jsvp.getSpectrum(), mode);
    return (spectrum == jsvp.getSpectrum() ? null : new JSVPanel(spectrum, jsvp.popup));
  }

  static void showSolutionColor(Component component, String sltnclr) {
    JOptionPane.showMessageDialog(component, "<HTML><body bgcolor=rgb("
        + sltnclr + ")><br />Predicted Solution Colour- RGB(" + sltnclr
        + ")<br /><br /></body></HTML>", "Predicted Colour",
        JOptionPane.INFORMATION_MESSAGE);
  }

  String getSolutionColor() {
    Graph spectrum = getSpectrum();
    String Yunits = spectrum.getYUnits();
    return Visible.Colour(spectrum.getXYCoords(), Yunits);
  }

  /////////////// 2D image /////////////////
  
  private void draw2DImage(Graphics g) {
    if (isd != null) {
      g.drawImage(image2D, 
          isd.xPixel0, isd.yPixel0,  // destination 
          isd.xPixel0 + isd.xPixels - 1, // destination 
          isd.yPixel0 + isd.yPixels - 1, // destination 
          isd.xView1, isd.yView1, isd.xView2, isd.yView2, null); // source
    }
  }

  private boolean get2DImage() {
    isd = new ImageScaleData();
    isd.setScale(zoomInfoList.get(0));
    if (!update2dImage(false))
      return false;
    isd.resetZoom();
    sticky2Dcursor = true;
    return true;
  }

  private boolean update2dImage(boolean forceNew) {
    isd.setScale(multiScaleData);
    JDXSpectrum spec0 = getSpectrumAt(0);
    int[] buffer = spec0.get2dBuffer(jsvp.thisWidth, jsvp.thisPlotHeight, isd, forceNew);
    if (buffer == null) {
      image2D = null;
      isd = null;
      return false;
    }
    isd.setImageSize(spec0.getXYCoords().length, spec0.getSubSpectra().size(), !forceNew);
    image2D = new BufferedImage(isd.imageWidth, isd.imageHeight, BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster raster = image2D.getRaster();
    raster.setSamples(0, 0, isd.imageWidth, isd.imageHeight, 0, buffer);
    setImageWindow();
    return true;
  }

  private void setImageWindow() {
    isd.setPixelWidthHeight((int) ((jsvp.display1D ? 0.6 : 1) * xPixels), yPixels);
    widthRatio = (jsvp.display1D ? 1.0 * (xPixels - isd.xPixels) / xPixels : 1);
    isd.setXY0((int) Math.floor(xPixel1 - isd.xPixels), yPixel0);
  }


  void dispose() {
    spectra = null;
    zoomInfoList = null;
    image2D = null;
    isd = null;
  }
  
  Map<String, Object> getInfo(String key, boolean isSelected) {
    Map<String, Object> spectraInfo = new Hashtable<String, Object>();
    List<Map<String, Object>> specInfo = new ArrayList<Map<String, Object>>();   
    spectraInfo.put("spectra", specInfo);
    for (int i = 0; i < nSpectra; i++) {
      Map<String, Object> info = spectra.get(i).getInfo(key);
      info.put("selected", Boolean.valueOf(isSelected));
      specInfo.add(info);
    }
    return spectraInfo;
  }

  public void setPlotColor0(Color color) {
    plotColors[0] = color;
  }

  public void mouseClickEvent(int xPixel, int yPixel, int clickCount, boolean isControlDown) {
    PlotWidget pw = getPinSelected(xPixel, yPixel);
    if (pw != null) {
      setWidgetValueByUser(pw);
      return;
    }
    boolean is2D = (isd != null && xPixel == isd.fixX(xPixel) && yPixel == fixY(yPixel));
    if (isControlDown)
      clearIntegrals();
    else if (clickCount == 2) {
      if (is2D) {
        if (sticky2Dcursor) {
          addAnnotation(new ColoredAnnotation(isd.toX(xPixel), isd
              .toSubspectrumIndex(yPixel), jsvp.coordStr, Color.BLACK, false, true,
              5, 5), true);
        }
        sticky2Dcursor = !sticky2Dcursor;
        set2DCrossHairs(xPixel, yPixel);
        jsvp.repaint();
        return;
      }
      if (isInTopBar(xPixel, yPixel)) {
        doZoom(toX0(xPixel0), multiScaleData.minY,
            toX0(xPixel1), multiScaleData.maxY, true, true, false);
      } else if (isInRightBar(xPixel, yPixel)) {
        doZoom(multiScaleData.minXOnScale, zoomInfoList.get(0).minY,
            multiScaleData.maxXOnScale, zoomInfoList.get(0).maxY, true, true,
            false);
      } else if (isInTopBar2D(xPixel, yPixel)) {
        reset2D(true);
      } else if (isInRightBar2D(xPixel, yPixel)) {
        reset2D(false);
      }

      return;
    }
    if (is2D && annotations != null) {
      Coordinate xy = new Coordinate(isd.toX(xPixel), isd
          .toSubspectrumIndex(yPixel));
      Annotation a = findAnnotation2D(xy);
      if (a != null && setAnnotationText(a)) {
        jsvp.repaint();
        return;
      }
    }
    if (is2D) {
      sticky2Dcursor = false;
      set2DCrossHairs(xPixel, yPixel);
      jsvp.repaint();
      return;
    }
    if (xPixel != fixX(xPixel) || yPixel != fixY(yPixel)) {
      jsvp.coordClicked = null;
      jsvp.coordsClicked = null;      
      return;
    }
    jsvp.coordClicked = new Coordinate(toX(xPixel), toY(yPixel));
    jsvp.lastClickX = jsvp.coordClicked.getXVal();
    jsvp.coordsClicked = getSpectrum().getXYCoords();
    jsvp.notifyPeakPickedListeners(jsvp.coordClicked);
  }

  public void mouseReleasedEvent() {
    if (jsvp.isIntegralDrag) {
      if (zoomBox1D.xPixel0 != zoomBox1D.xPixel1) {
        checkIntegral(toX(zoomBox1D.xPixel0), toX(zoomBox1D.xPixel1), true);
        zoomBox1D.xPixel1 = zoomBox1D.xPixel0;
        jsvp.repaint();
      }
      jsvp.isIntegralDrag = false;
      return;
    }

    if (jsvp.thisWidget == zoomBox2D) {
      if (!isGoodEvent(zoomBox2D, true, true))
        return;
      isd.setZoom(zoomBox2D.xPixel0, zoomBox2D.yPixel0, zoomBox2D.xPixel1,
          zoomBox2D.yPixel1);
      zoomBox2D.xPixel1 = zoomBox2D.xPixel0;
      doZoom(isd.toX(isd.xPixel0), multiScaleData.minY, isd.toX(isd.xPixel0
          + isd.xPixels - 1), multiScaleData.maxY, true, true, false);
    } else if (jsvp.thisWidget == zoomBox1D) {
      if (!isGoodEvent(zoomBox1D, true, true))
        return;
      int x1 = zoomBox1D.xPixel1;
      doZoom(toX(zoomBox1D.xPixel0), toY(zoomBox1D.yPixel0), toX(x1),
          toY(zoomBox1D.yPixel1), true, true, false);
      zoomBox1D.xPixel1 = zoomBox1D.xPixel0;
    } else if (jsvp.thisWidget == pin1Dx0 || jsvp.thisWidget == pin1Dx1
        || jsvp.thisWidget == cur2Dx0 || jsvp.thisWidget == cur2Dx1) {
      addCurrentZoom();
    }
  }

  private static final int MIN_DRAG_X_PIXELS = 5;// fewer than this means no zoom

  private static boolean isGoodEvent(PlotWidget zb, boolean asX, boolean asY) {
    return (!asX || (Math.abs(zb.xPixel1 - zb.xPixel0) > MIN_DRAG_X_PIXELS)
    && (!asY || Math.abs(zb.yPixel1 - zb.yPixel0) > MIN_DRAG_X_PIXELS));
  }

  public void mouseMovedEvent(int xPixel, int yPixel) {
    setToolTipForPixels(xPixel, yPixel);
    if (isd != null && !jsvp.display1D && sticky2Dcursor)
      set2DCrossHairs(xPixel, yPixel);
    jsvp.repaint();
  }

  boolean checkWidgetEvent(int xPixel, int yPixel, boolean isPress) {
    if (!enableZoom)
      return false;
    PlotWidget widget = jsvp.thisWidget;
    if (isPress) {
      widget = getPinSelected(xPixel, yPixel);
      if (widget == null) {
        yPixel = fixY(yPixel);
        if (xPixel < xPixel1) {
          xPixel = fixX(xPixel);
          zoomBox1D.setX(toX(xPixel), xPixel);
          zoomBox1D.yPixel0 = (jsvp.isIntegralDrag ? yPixel0 : yPixel);
          widget = zoomBox1D;
        } else if (isd != null && xPixel < isd.xPixel1) {
          zoomBox2D.setX(isd.toX(xPixel), isd.fixX(xPixel));
          zoomBox2D.yPixel0 = yPixel;
          widget = zoomBox2D;
        }
      }
      jsvp.thisWidget = widget;
      return true;
    }
    if (widget == null)
      return false;

    // mouse drag with widget
    if (widget == zoomBox1D) {
      zoomBox1D.xPixel1 = fixX(xPixel);
      zoomBox1D.yPixel1 = (jsvp.isIntegralDrag ? yPixel1 : fixY(yPixel));
      if (jsvp.isIntegralDrag && zoomBox1D.xPixel0 != zoomBox1D.xPixel1)
        checkIntegral(zoomBox1D.getXVal(), toX(zoomBox1D.xPixel1), false);
      return true;
    }
    if (widget == zoomBox2D) {
      zoomBox2D.xPixel1 = isd.fixX(xPixel);
      zoomBox2D.yPixel1 = fixY(yPixel);
      return true;
    }
    if (widget == cur2Dy) {
      yPixel = fixY(yPixel);
      cur2Dy.yPixel0 = cur2Dy.yPixel1 = yPixel;
      setCurrentSubSpectrum(isd.toSubspectrumIndex(yPixel));
      return true;
    }
    if (widget == cur2Dx0 || widget == cur2Dx1) {
      xPixel = isd.fixX(xPixel);
      widget.setX(isd.toX(xPixel), xPixel);
      doZoom(cur2Dx0.getXVal(), multiScaleData.minY, cur2Dx1.getXVal(),
          multiScaleData.maxY, false, false, false);
      return true;
    }
    if (widget == pin1Dx0 || widget == pin1Dx1
        || widget == pin1Dx01) {
      xPixel = fixX(xPixel);
      widget.setX(toX0(xPixel), xPixel);
      if (widget == pin1Dx01) {
        int dp = xPixel - (pin1Dx0.xPixel0 + pin1Dx1.xPixel0) / 2 + 1;
        xPixel = pin1Dx0.xPixel0 + dp;
        pin1Dx0.setX(toX0(xPixel), xPixel);
        xPixel = pin1Dx1.xPixel0 + dp;
        pin1Dx1.setX(toX0(xPixel), xPixel);
      }
      doZoom(pin1Dx0.getXVal(), multiScaleData.minY, pin1Dx1.getXVal(),
          multiScaleData.maxY, false, false, false);
      return true;
    }
    if (widget == pin1Dy0 || widget == pin1Dy1
        || widget == pin1Dy01) {
      yPixel = fixY(yPixel);
      widget.setY(toY0(yPixel), yPixel);
      if (widget == pin1Dy01) {
        int dp = yPixel - (pin1Dy0.yPixel0 + pin1Dy1.yPixel0) / 2 + 1;
        yPixel = pin1Dy0.yPixel0 + dp;
        pin1Dy0.setY(toY0(yPixel), yPixel);
        yPixel = pin1Dy1.yPixel0 + dp;
        pin1Dy1.setY(toY0(yPixel), yPixel);
      }
      doZoom(multiScaleData.minXOnScale, pin1Dy0.getYVal(),
          multiScaleData.maxXOnScale, pin1Dy1.getYVal(), false, false, false);
      return true;
    }
    if (widget == pin2Dx0 || widget == pin2Dx1
        || widget == pin2Dx01) {
      xPixel = isd.fixX(xPixel);
      widget.setX(isd.toX0(xPixel), xPixel);
      if (widget == pin2Dx01) {
        int dp = xPixel - (pin2Dx0.xPixel0 + pin2Dx1.xPixel0) / 2 + 1;
        xPixel = pin2Dx0.xPixel0 + dp;
        pin2Dx0.setX(isd.toX0(xPixel), xPixel);
        xPixel = pin2Dx1.xPixel0 + dp;
        pin2Dx1.setX(isd.toX0(xPixel), xPixel);
      }
      if (!isGoodEvent(pin2Dx0, true, false)) {
        reset2D(true);
        return false;
      }
      isd.setView0(pin2Dx0.xPixel0, pin2Dy0.yPixel0, pin2Dx1.xPixel0,
          pin2Dy1.yPixel0);
      doZoom(pin2Dx0.getXVal(), multiScaleData.minY, pin2Dx1.getXVal(),
          multiScaleData.maxY, false, false, false);
      return true;
    }
    if (widget == pin2Dy0 || widget == pin2Dy1
        || widget == pin2Dy01) {
      yPixel = fixY(yPixel);
      widget.setY(isd.toSubspectrumIndex(yPixel), yPixel);
      if (widget == pin2Dy01) {
        int dp = yPixel - (pin2Dy0.yPixel0 + pin2Dy1.yPixel0) / 2 + 1;
        yPixel = pin2Dy0.yPixel0 + dp;
        pin2Dy0.setY(isd.toSubspectrumIndex(yPixel), yPixel);
        yPixel = pin2Dy1.yPixel0 + dp;
        pin2Dy1.setY(isd.toSubspectrumIndex(yPixel), yPixel);
      }
      if (!isGoodEvent(pin2Dy0, false, true)) {
        reset2D(false);
        return false;
      }
      isd.setView0(pin2Dx0.xPixel0, pin2Dy0.yPixel0, pin2Dx1.xPixel1,
          pin2Dy1.yPixel1);
      return true;
    }
    return false;
  }

}
