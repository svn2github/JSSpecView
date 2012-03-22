package jspecview.common;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.source.JDXSource;
import jspecview.util.Logger;
import jspecview.util.Parser;

abstract class GraphSet {

  abstract void addHighlight(double x1, double x2, Graph spec, Object oColor);
  abstract protected void disposeImage();
  abstract protected void draw2DImage(Object g);
  abstract protected void drawHandle(Object g, int x, int y);
  abstract protected void drawHighlights(Object g, Graph spec);
  abstract protected void drawLine(Object g, int x0, int y0, int x1, int y1);
  abstract protected void drawRect(Object g, int xPixel02, int yPixel02,
                                   int xPixels2, int yPixels2);
  abstract protected void drawString(Object g, String s, int x, int y);
  abstract protected void drawTitle(Object g, int height, int width,
                                    String title);
  abstract protected void fillBox(Object g, int x0, int y0, int x1, int y1,
                                  ScriptToken whatColor);
  abstract Annotation getAnnotation(double x, double y, String text,
                                    boolean isPixels, boolean is2D,
                                    int offsetX, int offsetY);
  abstract Annotation getAnnotation(List<String> args, Annotation lastAnnotation);
  abstract protected boolean get2DImage();
  abstract protected String getCoordString();
  abstract protected int getFontHeight(Object g);
  abstract protected NumberFormat getFormatter(String hash);
  abstract protected GraphSet getGraphSet(Object jsvp);
  abstract protected String getInput(String message, String title, String sval);
  abstract protected PlotWidget getThisWidget();
  abstract protected int getStringWidth(Object g, String s);
  abstract protected boolean isCurrentGraphSet();
  abstract protected boolean isIntegralDrag();
  abstract protected void notifyPeakPickedListeners();
  abstract protected void notifyPeakListeners(PeakInfo peak);
  abstract protected void notifySubSpectrumChange(int i, JDXSpectrum spectrum);
  abstract void refresh();
  abstract void removeAllHighlights(Graph graph);
  abstract void removeHighlight(int index);
  abstract void removeHighlight(double x1, double x2);
  abstract void repaint();
  abstract protected void setAnnotationColor(Object g, Annotation note, ScriptToken whatColor);
  abstract protected void setColor(Object g, ScriptToken plotcolor);
  abstract protected Coordinate setCoordClicked(double x, double y);
  abstract protected void setCoordStr(String string);
  abstract protected void setCurrentBoxColor(Object g);
  abstract protected void setFont(Object g, int width, int face, int size,
                                  boolean b);
  abstract protected void setIntegralDrag(boolean b);
  abstract protected void setPlotColor(Object g, int i);
  abstract void setPlotColors(Object oColors);
  abstract void setPlotColor0(Object oColor);
  abstract protected boolean setStartupPinTip();  
  abstract protected void setThisWidget(PlotWidget widget);
  abstract protected void setToolTipText(String s);
  abstract boolean update2dImage(boolean b);



  protected List<Graph> spectra = new ArrayList<Graph>(2);

  int getNumberOfSpectra() {
    return nSpectra;
  }

  MultiScaleData multiScaleData;
  protected List<MultiScaleData> zoomInfoList;
  protected ArrayList<Annotation> integrationRatios;
  protected ArrayList<Annotation> annotations;
  protected Annotation lastAnnotation;
  protected JDXSource source;
  protected ImageScaleData isd;
  private List<Graph> graphsTemp = new ArrayList<Graph>();
  protected PlotWidget[] widgets;


  void dispose() {
    for (int i = 0; i < spectra.size(); i++)
      spectra.get(i).dispose();
    spectra = null;
    multiScaleData = null;
    zoomInfoList = null;
    integrationRatios = null;
    annotations = null;
    lastAnnotation = null;
    source = null;
    isd = null;
    graphsTemp = null;
    widgets = null;
    disposeImage();
  }

  protected double fracX = 1, fracY = 1, fX0 = 0, fY0 = 0; // take up full screen

  protected PlotWidget zoomBox1D, zoomBox2D, pin1Dx0, pin1Dx1, // ppm range -- horizontal bar on 1D spectrum 
      pin1Dy0, pin1Dy1, // y-scaling -- vertical bar on 1D spectrum and left of 2D when no 1D
      pin1Dx01, pin1Dy01, // center pins for those
      pin2Dx0, pin2Dx1, // ppm range -- horizontal bar on 2D spectrum 
      pin2Dy0, pin2Dy1, // subspectrum range -- vertical bar on 2D spectrum
      pin2Dx01, pin2Dy01, // center pins for those
      cur2Dx0, cur2Dx1, // 2D x cursors -- derived from pin1Dx0 and pin1Dx1 values
      cur2Dy; // 2D y cursor -- points to currently displayed 1D slice


  // for the 1D plot area:
  protected int xPixel0, yPixel0, xPixel1, yPixel1;
  // for the overall panel section:
  protected int xPixel00, yPixel00, xPixel11, yPixel11;
  protected int xPixels, yPixels;

  protected boolean allowYScale = true;
  protected boolean drawXAxisLeftToRight;
  protected boolean xAxisLeftToRight = true;
  protected boolean doDraw1DObjects = true;
  protected boolean sticky2Dcursor;

  protected boolean reversePlot;
  protected boolean enableZoom;

  protected int currentZoomIndex;
  protected int nSpectra;
  protected int iThisSpectrum = -1;
  private double lastClickX;

  double userYFactor = 1;
  protected double xFactorForScale, yFactorForScale;
  protected double minYScale;
  protected double widthRatio;

  PeakInfo findPeak(String filePath, String index) {
    // for now we are only checking the base spectrum, but
    // there might be a need to check subspectra at some point?
    return getSpectrumAt(0).findPeakByFileIndex(filePath, index);
  }

  JDXSpectrum getSpectrum() {
    return getSpectrumAt(
        iThisSpectrum < 0 || iThisSpectrum == Integer.MAX_VALUE ? 0
            : iThisSpectrum).getCurrentSubSpectrum();
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

  static List<GraphSet> getGraphSets(Panel jsvp, List<Graph> spectra,
                                     int startIndex, int endIndex) {
    List<GraphSet> graphSets = new ArrayList<GraphSet>();
    GraphSet graphSet = null;
    Graph specLast = null;
    for (int i = 0; i < spectra.size(); i++) {
      Graph spec = spectra.get(i);
      if (specLast == null
          || !JDXSpectrum.areScalesCompatible(spec, specLast, false)) {
        graphSet = jsvp.newGraphSet();
        graphSets.add(graphSet);
      }
      graphSet.addSpec(specLast = spec);
    }
    GraphSet.setFractionalPositions(graphSets);
    for (int i = graphSets.size(); --i >= 0;) {
      graphSets.get(i).initGraphSet(startIndex, endIndex);
      Logger.info("JSVGraphSet " + (i + 1) + " nSpectra = "
          + graphSets.get(i).nSpectra);
    }
    return graphSets;
  }

  protected void addSpec(Graph spec) {
    spectra.add(spec);
    nSpectra++;
  }

  static void setFractionalPositions(List<GraphSet> graphSets) {
    // for now, just a vertical stack
    int n = graphSets.size();
    double f = 0;
    int n2d = 1;
    for (int i = 0; i < n; i++)
      f += (graphSets.get(i).getSpectrumAt(0).is1D() ? 1 : n2d);
    f = 1 / f;
    double x = 0;
    for (int i = 0; i < n; i++) {
      GraphSet gs = graphSets.get(i);
      double g = (gs.getSpectrumAt(0).is1D() ? f : n2d * f);
      gs.fracY = g;
      gs.fY0 = x;
      x += g;
    }
  }

  protected void setPositionForFrame(int width, int height, int left,
                                     int right, int top, int bottom) {
    xPixel00 = (int) (width * fX0);
    xPixel11 = (int) (width * (fX0 + fracX)) - 1;
    yPixel00 = (int) (height * fY0);
    yPixel11 = (int) (height * (fY0 + fracY)) - 1;
    xPixel0 = xPixel00 + left / (xPixel00 == 0 ? 1 : 2);
    xPixel1 = xPixel11 - right / (xPixel11 > width - 2 ? 1 : 2);
    yPixel0 = yPixel00 + top / (yPixel00 == 0 ? 1 : 2);
    yPixel1 = yPixel11 - bottom / (yPixel11 > height - 2 ? 1 : 2);
    xPixels = xPixel1 - xPixel0 + 1;
    yPixels = yPixel1 - yPixel0 + 1;

  }

  protected boolean hasPoint(int xPixel, int yPixel) {
    return (xPixel >= xPixel00 && xPixel <= xPixel11 && yPixel >= yPixel00 && yPixel <= yPixel11);
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

  void initGraphSet(int startIndex, int endIndex) {
    xAxisLeftToRight = getSpectrumAt(0).shouldDisplayXAxisIncreasing();
    setDrawXAxis();
    int[] startIndices = new int[nSpectra];
    int[] endIndices = new int[nSpectra];
    allowYScale = true;
    if (endIndex <= 0)
      endIndex = Integer.MAX_VALUE;
    for (int i = 0; i < nSpectra; i++) {
      int iLast = spectra.get(i).getXYCoords().length - 1;
      startIndices[i] = Coordinate.intoRange(startIndex, 0, iLast);
      endIndices[i] = Coordinate.intoRange(endIndex, 0, iLast);
      allowYScale &= (spectra.get(i).getYUnits().equals(
          spectra.get(0).getYUnits()) && spectra.get(i).getUserYFactor() == spectra
          .get(0).getUserYFactor());
    }
    allowYScale &= (fracY == 1 && fracX == 1);
    getMultiScaleData(0, 0, 0, 0, startIndices, endIndices);
    zoomInfoList = new ArrayList<MultiScaleData>();
    zoomInfoList.add(multiScaleData);
  }

  protected void getMultiScaleData(double x1, double x2, double y1, double y2,
                                   int[] startIndices, int[] endIndices) {
    List<Graph> graphs = (graphsTemp.size() == 0 ? spectra : graphsTemp);
    List<JDXSpectrum> subspecs = getSpectrumAt(0).getSubSpectra();
    boolean dontUseSubspecs = (subspecs == null || subspecs.size() == 2);
    //NMR real/imaginary
    boolean is2D = !getSpectrumAt(0).is1D();
    if (is2D || dontUseSubspecs && y1 == y2) {
      // 2D spectrum or startup
      graphs = spectra;
    } else if (y1 == y2) {
      //start up, forced subsets (too many spectra) 
      multiScaleData = new MultiScaleData(subspecs, y1, y2, 10, 10,
          getSpectrum().isContinuous());
      return;
    }
    multiScaleData = new MultiScaleData(graphs, y1, y2, startIndices,
        endIndices, 10, 10, getSpectrumAt(0).isContinuous());
    if (x1 != x2)
      multiScaleData.setXRange(x1, x2, 10);
  }

  static GraphSet findGraphSet(List<GraphSet> graphSets, int xPixel, int yPixel) {
    for (int i = graphSets.size(); --i >= 0;)
      if (graphSets.get(i).hasPoint(xPixel, yPixel))
        return graphSets.get(i);
    return null;
  }

  protected boolean isNearby2D(Coordinate a1, Coordinate a2) {
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

  protected void setDrawXAxis() {
    drawXAxisLeftToRight = xAxisLeftToRight ^ reversePlot;
    getSpectrum().setExportXAxisDirection(drawXAxisLeftToRight);
  }

  protected void setScaleFactors(MultiScaleData multiScaleData) {
    xFactorForScale = (multiScaleData.maxXOnScale - multiScaleData.minXOnScale)
        / xPixels;
    yFactorForScale = (multiScaleData.maxYOnScale - multiScaleData.minYOnScale)
        / yPixels;
    minYScale = multiScaleData.minYOnScale;
  }

  protected int fixX(int xPixel) {
    return Coordinate.intoRange(xPixel, xPixel0, xPixel1);
  }

  protected int toPixelX(double dx) {
    int x = (int) ((dx - multiScaleData.minXOnScale) / xFactorForScale);
    return (int) (drawXAxisLeftToRight ? xPixel0 + x : xPixel1 - x);
  }

  protected boolean isInTopBar(int xPixel, int yPixel) {
    return (xPixel == fixX(xPixel) && yPixel > pin1Dx0.yPixel0 - 2 && yPixel < pin1Dx0.yPixel1);
  }

  protected boolean isInTopBar2D(int xPixel, int yPixel) {
    return (isd != null && xPixel == isd.fixX(xPixel)
        && yPixel > pin2Dx0.yPixel0 - 2 && yPixel < pin2Dx0.yPixel1);
  }

  protected boolean isInRightBar(int xPixel, int yPixel) {
    return (yPixel == fixY(yPixel) && xPixel > pin1Dy0.xPixel1 && xPixel < pin1Dy0.xPixel0 + 2);
  }

  protected boolean isInRightBar2D(int xPixel, int yPixel) {
    return (isd != null && yPixel == fixY(yPixel) && xPixel > pin2Dy0.xPixel1 && xPixel < pin2Dy0.xPixel0 + 2);
  }

  protected double toX(int xPixel) {
    if (isd != null && isd.isXWithinRange(xPixel))
      return isd.toX(xPixel);
    xPixel = fixX(xPixel);
    return (drawXAxisLeftToRight ? multiScaleData.maxXOnScale
        - (xPixel1 - xPixel) * xFactorForScale : multiScaleData.minXOnScale
        + (xPixel1 - xPixel) * xFactorForScale);
  }

  protected double toX0(int xPixel) {
    xPixel = fixX(xPixel);
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxXOnScale - multiScaleData.minXOnScale)
        / xPixels;
    return (drawXAxisLeftToRight ? multiScaleData.maxXOnScale
        - (xPixel1 - xPixel) * factor : multiScaleData.minXOnScale
        + (xPixel1 - xPixel) * factor);
  }

  protected int toPixelX0(double x) {
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxXOnScale - multiScaleData.minXOnScale)
        / xPixels;
    return (int) (drawXAxisLeftToRight ? xPixel1
        - (multiScaleData.maxXOnScale - x) / factor : xPixel1
        - (x - multiScaleData.minXOnScale) / factor);
  }

  protected int fixY(int yPixel) {
    return Coordinate.intoRange(yPixel, yPixel0, yPixel1);
  }

  protected int toPixelY(double yVal) {
    return (Double.isNaN(yVal) ? Integer.MIN_VALUE : yPixel1
        - (int) ((yVal * userYFactor - minYScale) / yFactorForScale));
  }

  protected int toPixelY0(double y) {
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxYOnScale - multiScaleData.minYOnScale)
        / yPixels;
    return fixY((int) (yPixel0 + (multiScaleData.maxYOnScale - y) / factor));
  }

  protected double toY(int yPixel) {
    return multiScaleData.maxYOnScale + (yPixel0 - yPixel) * yFactorForScale;
  }

  protected double toY0(int yPixel) {
    yPixel = fixY(yPixel);
    MultiScaleData multiScaleData = zoomInfoList.get(0);
    double factor = (multiScaleData.maxYOnScale - multiScaleData.minYOnScale)
        / yPixels;
    double y = multiScaleData.maxYOnScale + (yPixel0 - yPixel) * factor;
    return Math.max(multiScaleData.minY, Math.min(y, multiScaleData.maxY));
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



  String addAnnotation(List<String> args, String title) {
    if (args.size() == 0 || args.size() == 1
        && args.get(0).equalsIgnoreCase("none")) {
      annotations = null;
      lastAnnotation = null;
      return null;
    }
    if (args.size() < 4 && lastAnnotation == null)
      lastAnnotation = getAnnotation(
          (multiScaleData.maxXOnScale + multiScaleData.minXOnScale) / 2,
          (multiScaleData.maxYOnScale + multiScaleData.minYOnScale) / 2, title,
          false, false, 0, 0);
    Annotation annotation = getAnnotation(args, lastAnnotation);
    if (annotation == null)
      return null;
    if (annotations == null && args.size() == 1
        && args.get(0).charAt(0) == '\"') {
      String s = annotation.getText();
      getSpectrum().setTitle(s);
      return s;
    }
    lastAnnotation = annotation;
    addAnnotation(annotation, false);
    return null;
  }


  protected Annotation findAnnotation2D(Coordinate xy) {
    for (int i = annotations.size(); --i >= 0;) {
      Annotation a = annotations.get(i);
      if (isNearby2D(a, xy))
        return a;
    }
    return null;
  }

  protected void addAnnotation(Annotation annotation, boolean isToggle) {
    if (annotations == null)
      annotations = new ArrayList<Annotation>();
    boolean removed = false;
    for (int i = annotations.size(); --i >= 0;)
      if (annotation.is2D ? isNearby2D(annotations.get(i), annotation)
          : annotation.equals(annotations.get(i))) {
        removed = true;
        annotations.remove(i);
      }
    if (annotation.getText().length() > 0 && (!removed || !isToggle))
      annotations.add(annotation);
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
   * 
   * @param g
   * @param withGrid
   * @param withXUnits
   * @param withYUnits
   * @param withXScale
   * @param withYScale
   * @param isInteractive
   * @param drawY0
   * @param height
   * @param width
   * @param plotAreaInsets
   * @param isResized
   * @param enableZoom
   */
  void drawGraph(Object og, boolean withGrid, boolean withXUnits,
                 boolean withYUnits, boolean withXScale,
                 boolean withYScale,
                 boolean isInteractive,
                 boolean drawY0, //!isIntegralDrag
                 int height, int width, int left, int right, int top,
                 int bottom, boolean isResized, boolean enableZoom,
                 boolean display1D, boolean display2D) {
    // for now, at least, we only allow one 2D image
    this.enableZoom = enableZoom;
    setPositionForFrame(width, height, left, right, top, bottom);
    JDXSpectrum spec0 = getSpectrumAt(0);
    userYFactor = getSpectrum().getUserYFactor();
    setScaleFactors(multiScaleData);
    if (!getSpectrumAt(0).is1D() && display2D && (isd != null || get2DImage())) {
      setImageWindow(display1D);
      width = (int) Math.floor(widthRatio * xPixels * 0.8);
      if (display1D) {
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
    drawAll(og, height, width, subIndex, spec0, isInteractive, withGrid,
        withXScale, withYScale, withXUnits, withYUnits, drawY0, display1D);
  }

  protected boolean doPlot(int i) {
    return (iThisSpectrum < 0 || iThisSpectrum == i || spectra.get(i) instanceof IntegralGraph
        && iThisSpectrum == i - 1);
  }

  protected void setImageWindow(boolean display1D) {
    isd.setPixelWidthHeight((int) ((display1D ? 0.6 : 1) * xPixels), yPixels);
    widthRatio = (display1D ? 1.0 * (xPixels - isd.xPixels) / xPixels : 1);
    isd.setXY0((int) Math.floor(xPixel1 - isd.xPixels), yPixel0);
  }

  void mouseClickEvent(int xPixel, int yPixel, int clickCount,
                       boolean isControlDown) {
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
          addAnnotation(
              getAnnotation(isd.toX(xPixel), isd.toSubspectrumIndex(yPixel),
                  getCoordString(), false, true, 5, 5), true);
        }
        sticky2Dcursor = !sticky2Dcursor;
        set2DCrossHairs(xPixel, yPixel);
        repaint();
        return;
      }
      if (isInTopBar(xPixel, yPixel)) {
        doZoom(toX0(xPixel0), multiScaleData.minY, toX0(xPixel1),
            multiScaleData.maxY, true, true, false);
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
        repaint();
        return;
      }
    }
    if (is2D) {
      sticky2Dcursor = false;
      set2DCrossHairs(xPixel, yPixel);
      repaint();
      return;
    }
    if (xPixel != fixX(xPixel) || yPixel != fixY(yPixel)) {
      setCoordClicked(Double.NaN, 0);
    } else {
      lastClickX = toX(xPixel);
      setCoordClicked(lastClickX, toY(yPixel));
    }
    notifyPeakPickedListeners();
  }

  void mouseReleasedEvent() {
    PlotWidget thisWidget = getThisWidget();
    if (isIntegralDrag()) {
      if (!isGoodEvent(zoomBox1D, null, true)) {
        checkIntegral(toX(zoomBox1D.xPixel0), toX(zoomBox1D.xPixel1), true);
        zoomBox1D.xPixel1 = zoomBox1D.xPixel0;
        repaint();
      }
      setIntegralDrag(false);
    } else if (thisWidget == zoomBox2D) {
      if (!isGoodEvent(zoomBox2D, null, true))
        return;
      isd.setZoom(zoomBox2D.xPixel0, zoomBox2D.yPixel0, zoomBox2D.xPixel1,
          zoomBox2D.yPixel1);
      zoomBox2D.xPixel1 = zoomBox2D.xPixel0;
      doZoom(isd.toX(isd.xPixel0), multiScaleData.minY, isd.toX(isd.xPixel0
          + isd.xPixels - 1), multiScaleData.maxY, true, true, false);
    } else if (thisWidget == zoomBox1D) {
      if (!isGoodEvent(zoomBox1D, null, true))
        return;
      int x1 = zoomBox1D.xPixel1;
      doZoom(toX(zoomBox1D.xPixel0), toY(zoomBox1D.yPixel0), toX(x1),
          toY(zoomBox1D.yPixel1), true, true, false);
      zoomBox1D.xPixel1 = zoomBox1D.xPixel0;
    } else if (thisWidget == pin1Dx0 || thisWidget == pin1Dx1
        || thisWidget == cur2Dx0 || thisWidget == cur2Dx1) {
      addCurrentZoom();
    }
  }

  private static final int MIN_DRAG_PIXELS = 5;// fewer than this means no zoom or reset

  private static boolean isGoodEvent(PlotWidget zOrP, PlotWidget p, boolean asX) {
    return (p == null ? (Math.abs(zOrP.xPixel1 - zOrP.xPixel0) > MIN_DRAG_PIXELS && Math
        .abs(zOrP.yPixel1 - zOrP.yPixel0) > MIN_DRAG_PIXELS)
        : asX ? Math.abs(zOrP.xPixel0 - p.xPixel0) > MIN_DRAG_PIXELS : Math
            .abs(zOrP.yPixel0 - p.yPixel0) > MIN_DRAG_PIXELS);
  }

  void mouseMovedEvent(int xPixel, int yPixel, boolean display1D) {
    setToolTipForPixels(xPixel, yPixel, display1D);
    if (isd != null && !display1D && sticky2Dcursor)
      set2DCrossHairs(xPixel, yPixel);
    repaint();
  }

  boolean checkWidgetEvent(int xPixel, int yPixel, boolean isPress) {
    if (!enableZoom)
      return false;
    PlotWidget widget = getThisWidget();
    if (isPress) {
      widget = getPinSelected(xPixel, yPixel);
      if (widget == null) {
        yPixel = fixY(yPixel);
        if (xPixel < xPixel1) {
          xPixel = fixX(xPixel);
          zoomBox1D.setX(toX(xPixel), xPixel);
          zoomBox1D.yPixel0 = (isIntegralDrag() ? yPixel0 : yPixel);
          widget = zoomBox1D;
        } else if (isd != null && xPixel < isd.xPixel1) {
          zoomBox2D.setX(isd.toX(xPixel), isd.fixX(xPixel));
          zoomBox2D.yPixel0 = yPixel;
          widget = zoomBox2D;
        }
      }
      setThisWidget(widget);
      return true;
    }
    if (widget == null)
      return false;

    // mouse drag with widget
    if (widget == zoomBox1D) {
      zoomBox1D.xPixel1 = fixX(xPixel);
      zoomBox1D.yPixel1 = (isIntegralDrag() ? yPixel1 : fixY(yPixel));
      if (isIntegralDrag() && zoomBox1D.xPixel0 != zoomBox1D.xPixel1)
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
    if (widget == pin1Dx0 || widget == pin1Dx1 || widget == pin1Dx01) {
      xPixel = fixX(xPixel);
      widget.setX(toX0(xPixel), xPixel);
      if (widget == pin1Dx01) {
        int dp = xPixel - (pin1Dx0.xPixel0 + pin1Dx1.xPixel0) / 2 + 1;
        xPixel = pin1Dx0.xPixel0 + dp;
        int xPixel1 = pin1Dx1.xPixel0 + dp;
        if (fixX(xPixel) != xPixel || fixX(xPixel1) != xPixel1)
          return true;
        pin1Dx0.setX(toX0(xPixel), xPixel);
        pin1Dx1.setX(toX0(xPixel1), xPixel1);
      }
      doZoom(pin1Dx0.getXVal(), multiScaleData.minY, pin1Dx1.getXVal(),
          multiScaleData.maxY, false, false, false);
      return true;
    }
    if (widget == pin1Dy0 || widget == pin1Dy1 || widget == pin1Dy01) {
      yPixel = fixY(yPixel);
      widget.setY(toY0(yPixel), yPixel);
      if (widget == pin1Dy01) {
        int dp = yPixel - (pin1Dy0.yPixel0 + pin1Dy1.yPixel0) / 2 + 1;
        yPixel = pin1Dy0.yPixel0 + dp;
        int yPixel1 = pin1Dy1.yPixel0 + dp;
        double y0 = toY0(yPixel);
        double y1 = toY0(yPixel1);
        if (Math.min(y0, y1) == multiScaleData.minY
            || Math.max(y0, y1) == multiScaleData.maxY)
          return true;
        pin1Dy0.setY(y0, yPixel);
        pin1Dy1.setY(y1, yPixel1);
      }
      doZoom(multiScaleData.minXOnScale, pin1Dy0.getYVal(),
          multiScaleData.maxXOnScale, pin1Dy1.getYVal(), false, false, false);
      return true;
    }
    if (widget == pin2Dx0 || widget == pin2Dx1 || widget == pin2Dx01) {
      xPixel = isd.fixX(xPixel);
      widget.setX(isd.toX0(xPixel), xPixel);
      if (widget == pin2Dx01) {
        int dp = xPixel - (pin2Dx0.xPixel0 + pin2Dx1.xPixel0) / 2 + 1;
        xPixel = pin2Dx0.xPixel0 + dp;
        int xPixel1 = pin2Dx1.xPixel0 + dp;
        if (isd.fixX(xPixel) != xPixel || isd.fixX(xPixel1) != xPixel1)
          return true;
        pin2Dx0.setX(isd.toX0(xPixel), xPixel);
        pin2Dx1.setX(isd.toX0(xPixel1), xPixel1);
      }
      if (!isGoodEvent(pin2Dx0, pin2Dx1, true)) {
        reset2D(true);
        return false;
      }
      isd.setView0(pin2Dx0.xPixel0, pin2Dy0.yPixel0, pin2Dx1.xPixel0,
          pin2Dy1.yPixel0);
      doZoom(pin2Dx0.getXVal(), multiScaleData.minY, pin2Dx1.getXVal(),
          multiScaleData.maxY, false, false, false);
      return true;
    }
    if (widget == pin2Dy0 || widget == pin2Dy1 || widget == pin2Dy01) {
      yPixel = fixY(yPixel);
      widget.setY(isd.toSubspectrumIndex(yPixel), yPixel);
      if (widget == pin2Dy01) {
        int dp = yPixel - (pin2Dy0.yPixel0 + pin2Dy1.yPixel0) / 2 + 1;
        yPixel = pin2Dy0.yPixel0 + dp;
        int yPixel1 = pin2Dy1.yPixel0 + dp;
        if (yPixel != fixY(yPixel) || yPixel1 != fixY(yPixel1))
          return true;
        pin2Dy0.setY(isd.toSubspectrumIndex(yPixel), yPixel);
        pin2Dy1.setY(isd.toSubspectrumIndex(yPixel1), yPixel1);
      }
      if (!isGoodEvent(pin2Dy0, pin2Dy1, false)) {
        reset2D(false);
        return false;
      }
      isd.setView0(pin2Dx0.xPixel0, pin2Dy0.yPixel0, pin2Dx1.xPixel1,
          pin2Dy1.yPixel1);
      return true;
    }
    return false;
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
    sval = getInput("New value?", "Set Slider", sval);
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
          isd.setView0(isd.toPixelX0(val1), pin2Dy0.yPixel0, isd
              .toPixelX0(val2), pin2Dy1.yPixel0);
          doZoom(val1, pin1Dy0.getYVal(), val2, pin1Dy1.getYVal(), true, true,
              false);
        } else if (pw == pin2Dy01) {
          isd.setView0(pin2Dx0.xPixel0, isd.toPixelY0(val1), pin2Dx1.xPixel0,
              isd.toPixelY0(val2));
          doZoom(isd.toX(isd.xPixel0), multiScaleData.minY, isd.toX(isd.xPixel0
              + isd.xPixels - 1), multiScaleData.maxY, true, true, false);
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
          repaint();
        } else if (pw == pin2Dy0 || pw == pin2Dy1) {
          int val2 = (pw == pin2Dy0 ? pin2Dy1.yPixel0 : pin2Dy0.yPixel0);
          isd.setView0(pin2Dx0.xPixel0, isd.toPixelY((int) val),
              pin2Dx1.xPixel0, val2);
          repaint();
        } else {
          double val2 = (pw == pin1Dy0 ? pin1Dy1.getYVal() : pin1Dy0.getYVal());
          doZoom(pin1Dx0.getXVal(), val, pin1Dx1.getXVal(), val2, true, true,
              false);
        }
      }
    } catch (Exception e) {
    }
  }

  void processPeakSelect(PeakInfo peakInfo) {
    for (int i = spectra.size(); --i >= 0;) {
      Graph spec = spectra.get(i);
      if (!peakInfo.isClearAll() && spec != peakInfo.spectrum)
        continue;
      String peak = peakInfo.toString();
      removeAllHighlights(spectra.get(i));
      if (peak == null) {
        continue;
      }
      String xMin = Parser.getQuotedAttribute(peak, "xMin");
      String xMax = Parser.getQuotedAttribute(peak, "xMax");
      if (xMin == null || xMax == null)
        return;
      float x1 = Parser.parseFloat(xMin);
      float x2 = Parser.parseFloat(xMax);
      if (Float.isNaN(x1) || Float.isNaN(x2))
        return;
      addHighlight(x1, x2, spec, null);
      if (ScaleData.isWithinRange(x1, multiScaleData)
          && ScaleData.isWithinRange(x2, multiScaleData))
        repaint();
      else
        reset();

    }
  }

  void selectSpectrum(String filePath, String type, String model) {
    System.out.println("jsvgraphset selectSpectrum " + filePath + " " + type
        + " " + model);
    if (nSpectra == 1) {
      iThisSpectrum = -1;
      return;
    }
    boolean haveFound = false;
    for (int i = spectra.size(); --i >= 0;) {
      if ((filePath == null || getSpectrumAt(i).getFilePathForwardSlash()
          .equals(filePath))
          && (getSpectrumAt(i).matchesPeakTypeModel(type, model))) {
        iThisSpectrum = i;
        System.out.println("have found it -- " + i);
        haveFound = true;
      }
    }
    if (!haveFound && iThisSpectrum >= 0)
      iThisSpectrum = Integer.MAX_VALUE; // no plots in that case
  }

  void toPeak(int istep) {
    istep *= (drawXAxisLeftToRight ? 1 : -1);
    JDXSpectrum spec = getSpectrum();
    Coordinate coord = setCoordClicked(lastClickX, 0);
    int iPeak = spec.setNextPeak(coord, istep);
    if (iPeak < 0)
      return;
    PeakInfo peak = spec.getPeakList().get(iPeak);
    spec.setSelectedPeak(peak);
    setCoordClicked(peak.getX(), 0);
    notifyPeakListeners(peak);
  }

  void escape() {
    setThisWidget(null);
    zoomBox1D.xPixel0 = zoomBox1D.xPixel1 = zoomBox2D.xPixel0 = zoomBox2D.xPixel1 = 0;
  }

  /**
   * PlotWidgets are zoom boxes and slider points that are draggable. Some are
   * derived from others (center points and the 2D subIndex pointer). The first
   * time through, we have to create new pins. When the frame is resized, we
   * need to reset their positions along the slider based on their values, and
   * we need to also move the sliders to the right place.
   * 
   * @param isResized
   * @param subIndex
   */
  protected void setWidgets(boolean isResized, int subIndex) {
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
   * Create new pins and set their default values. Note that we are making a
   * distinction between multiScaleData.minY and multiScaleData.minYOnScale. For
   * X these are now the same, but for Y they are not. This produces a nicer
   * grid, but also an odd jumpiness in the Y slider that is not totally
   * predictable.
   * 
   * @param subIndex
   */
  protected void newPins() {
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
    widgets = new PlotWidget[] { zoomBox1D, zoomBox2D, pin1Dx0, pin1Dx01,
        pin1Dx1, pin1Dy0, pin1Dy01, pin1Dy1, pin2Dx0, pin2Dx01, pin2Dx1,
        pin2Dy0, pin2Dy01, pin2Dy1, cur2Dx0, cur2Dx1, cur2Dy };
  }

  protected void resetPinsFromMultiScaleData() {
    if (pin1Dx0 == null)
      return;
    pin1Dx0.setX(multiScaleData.minXOnScale,
        toPixelX0(multiScaleData.minXOnScale));
    pin1Dx1.setX(multiScaleData.maxXOnScale,
        toPixelX0(multiScaleData.maxXOnScale));
    pin1Dy0.setY(multiScaleData.minY, toPixelY0(multiScaleData.minY));
    pin1Dy1.setY(multiScaleData.maxY, toPixelY0(multiScaleData.maxY));
  }

  /**
   * use the pin values to find their positions along the slider
   * 
   */
  protected void resetPinPositions() {
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
  protected void setPinSliderPositions() {
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
      cur2Dy.xPixel0 = (doDraw1DObjects ? (xPixel1 + isd.xPixel0) / 2
          : isd.xPixel0 - 15);
      cur2Dy.xPixel1 = isd.xPixel1 + 5;
    }
  }

  /**
   * The center pins and the 2D subspectrum slider values are derived from other
   * data
   * 
   * @param subIndex
   */
  protected void setDerivedPins(int subIndex) {
    pin1Dx01.setX(0, (pin1Dx0.xPixel0 + pin1Dx1.xPixel0) / 2);
    pin1Dy01.setY(0, (pin1Dy0.yPixel0 + pin1Dy1.yPixel0) / 2);
    pin1Dx01.setEnabled(Math.min(pin1Dx0.xPixel0, pin1Dx1.xPixel0) != xPixel0
        || Math.max(pin1Dx0.xPixel0, pin1Dx1.xPixel0) != xPixel1);
    pin1Dy01.setEnabled(Math.min(pin1Dy0.yPixel0, pin1Dy1.yPixel0) != Math.min(
        toPixelY(multiScaleData.minY), toPixelY(multiScaleData.maxY))
        || Math.max(pin1Dy0.yPixel0, pin1Dy1.yPixel0) != Math.max(
            toPixelY(multiScaleData.minY), toPixelY(multiScaleData.maxY)));
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

    pin2Dx01
        .setEnabled(Math.min(pin2Dx0.xPixel0, pin2Dx1.xPixel0) != isd.xPixel0
            || Math.max(pin2Dx0.xPixel0, pin2Dx1.xPixel1) != isd.xPixel1);
    pin2Dy01.setEnabled(Math.min(pin2Dy0.yPixel0, pin2Dy1.yPixel0) != yPixel0
        || Math.max(pin2Dy0.yPixel0, pin2Dy1.yPixel1) != yPixel1);

  }

  /*-------------------- METHODS FOR SCALING AND ZOOM --------------------------*/

  // static parameters
  final static int minNumOfPointsForZoom = 3;


  void setZoom(double x1, double y1, double x2, double y2) {
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
  protected synchronized void doZoom(double initX, double initY, double finalX,
                                     double finalY, boolean doRepaint,
                                     boolean addZoom, boolean checkRange) {

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
          minNumOfPointsForZoom, startIndices, endIndices, false))
        return;
    } else {
      if (!multiScaleData.setDataPointIndices(spectra, initX, finalX,
          minNumOfPointsForZoom, startIndices, endIndices, false))
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
      repaint();
  }

  void setCurrentSubSpectrum(int i) {
    JDXSpectrum spec0 = getSpectrumAt(0);
    i = spec0.setCurrentSubSpectrum(i);
    if (spec0.isForcedSubset())
      multiScaleData.setXRange(getSpectrum());
    notifySubSpectrumChange(i, getSpectrum());
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

  protected void addCurrentZoom() {
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

  protected void setZoomTo(int i) {
    isd = null;
    currentZoomIndex = i;
    multiScaleData = zoomInfoList.get(i);
    resetPinsFromMultiScaleData();
    refresh();
  }

  /**
   * Clears all views in the zoom list
   */
  void clearViews() {
    reset();
    // leave first zoom
    for (int i = zoomInfoList.size(); --i >= 1;)
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

  protected void drawAll(Object g, int height, int width, int subIndex,
                         JDXSpectrum spec0, boolean isInteractive,
                         boolean withGrid, boolean withXScale,
                         boolean withYScale, boolean withXUnits,
                         boolean withYUnits, boolean drawY0, boolean display1D) {
    if (isd != null)
      draw2DImage(g);
    draw2DUnits(g, width, subIndex, spec0);
    doDraw1DObjects = (isd == null || display1D);
    if (isInteractive)
      drawFrame(g, height, width, withGrid);
    if (doDraw1DObjects) {
      // background stuff
      fillBox(g, xPixel0, yPixel0, xPixel1, yPixel1, ScriptToken.PLOTAREACOLOR);
      drawWidgets(g, subIndex, isInteractive);
      if (withGrid)
        drawGrid(g, height, width);
      drawHighlights(g);
      drawPeakTabs(g);
      // scale-related, title, and coordinates
      if (withXScale)
        drawXScale(g, height, width);
      if (withYScale && allowYScale)
        drawYScale(g, height, width);
      if (withXUnits)
        drawXUnits(g, width);
      if (withYUnits && allowYScale)
        drawYUnits(g, width);

      // the graphs
      for (int i = nSpectra; --i >= 0;)
        if (doPlot(i)) {
          drawPlot(g, i, height, width, withGrid, drawY0);
          if (iThisSpectrum == i)
            drawTitle(g, height, width, spectra.get(i).getTitle());
        }

      // over-spectrum stuff    
      if (integrationRatios != null)
        drawAnnotations(g, height, width, integrationRatios,
            ScriptToken.INTEGRALPLOTCOLOR);
      drawIntegralValue(g, width);
    } else {
      drawWidgets(g, subIndex, isInteractive);
    }
    if (annotations != null)
      drawAnnotations(g, height, width, annotations, null);
  }


  private void draw2DUnits(Object g, int width, int subIndex, JDXSpectrum spec0) {
    if (subIndex >= 0 && isd != null) {
      setColor(g, ScriptToken.PLOTCOLOR);
      drawUnits(g, width, spec0.nucleusX, isd.xPixel1 + 5, yPixel1, 1, 1.0);
      drawUnits(g, width, spec0.nucleusY, isd.xPixel0 - 5, yPixel0, 1, 0);
    }
  }

  private void drawHighlights(Object g) {
    if (iThisSpectrum == Integer.MAX_VALUE)
      return;
    Graph spec = spectra.get(Math.max(iThisSpectrum, 0));
    drawHighlights(g, spec);
  }

  private void drawPeakTabs(Object g) {
    if (iThisSpectrum == Integer.MAX_VALUE)
      return;
    Graph spec = spectra.get(Math.max(iThisSpectrum, 0));
    ArrayList<PeakInfo> list = (nSpectra == 1
        || getSpectrum().getIntegrationGraph() != null || iThisSpectrum >= 0 ? ((JDXSpectrum) spec)
        .getPeakList()
        : null);
    if (list != null && list.size() > 0) {
      for (int i = list.size(); --i >= 0;) {
        PeakInfo pi = list.get(i);
        double xMin = pi.getXMin();
        double xMax = pi.getXMax();
        if (xMin != xMax) {
          drawBar(g, xMin, xMax, null, false);
        }
      }
    }
  }

  /**
   * 
   * Draw sliders, pins, and zoom boxes (only one of which would ever be drawn)
   * 
   * @param g
   * @param subIndex
   */
  private void drawWidgets(Object g, int subIndex, boolean withSliders) {
    // top/side slider bar backgrounds
    if (withSliders) {
      if (doDraw1DObjects) {
        fillBox(g, xPixel0, pin1Dx0.yPixel1, xPixel1, pin1Dx1.yPixel1 + 2,
            ScriptToken.GRIDCOLOR);
        fillBox(g, pin1Dx0.xPixel0, pin1Dx0.yPixel1, pin1Dx1.xPixel0,
            pin1Dx1.yPixel1 + 2, ScriptToken.PLOTCOLOR);
      } else {
        fillBox(g, isd.xPixel0, pin2Dx0.yPixel1, isd.xPixel1,
            pin2Dx0.yPixel1 + 2, ScriptToken.GRIDCOLOR);
        fillBox(g, pin2Dx0.xPixel0, pin2Dx0.yPixel1, pin2Dx1.xPixel0,
            pin2Dx1.yPixel1 + 2, ScriptToken.PLOTCOLOR);
        fillBox(g, pin2Dy0.xPixel1, yPixel1, pin2Dy1.xPixel1 + 2, yPixel0,
            ScriptToken.GRIDCOLOR);
        fillBox(g, pin2Dy0.xPixel1, pin2Dy0.yPixel1, pin2Dy1.xPixel1 + 2,
            pin2Dy1.yPixel0, ScriptToken.PLOTCOLOR);
      }
    }
    fillBox(g, pin1Dy0.xPixel1, yPixel1, pin1Dy1.xPixel1 + 2, yPixel0,
        ScriptToken.GRIDCOLOR);
    fillBox(g, pin1Dy0.xPixel1, pin1Dy0.yPixel1, pin1Dy1.xPixel1 + 2,
        pin1Dy1.yPixel0, ScriptToken.PLOTCOLOR);
    for (int i = 0; i < widgets.length; i++) {
      PlotWidget pw = widgets[i];
      if (pw == null || !pw.isEnabled || !pw.isPinOrCursor && !enableZoom
          || pw.isPin && !withSliders)
        continue;
      if (pw.is2D) {
        if (pw == cur2Dx0 && !doDraw1DObjects)// || pw.is2Donly && doDraw1DObjects)
          continue;
      } else if (!doDraw1DObjects && pw != pin1Dy0 && pw != pin1Dy1
          && pw != pin1Dy01) {
        continue;
      }
      drawWidget(g, pw);
    }
  }

  private void drawWidget(Object g, PlotWidget pw) {
    if (pw == null)
      return;
    if (pw.isPinOrCursor) {
      setColor(g, ScriptToken.PLOTCOLOR);
      drawLine(g, pw.xPixel0, pw.yPixel0, pw.xPixel1, pw.yPixel1);
      drawHandle(g, pw.xPixel0, pw.yPixel0);
    } else if (pw.xPixel1 != pw.xPixel0) {
      fillBox(g, pw.xPixel0, pw.yPixel0, pw.xPixel1, pw.yPixel1,
          ScriptToken.ZOOMBOXCOLOR);
    }
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

  protected void drawBar(Object g, double startX, double endX,
                         ScriptToken whatColor, boolean isFullHeight) {
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
    fillBox(g, x1, yPixel0, x2, yPixel0 + (isFullHeight ? yPixels : 5),
        ScriptToken.HIGHLIGHTCOLOR);
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
  private void drawPlot(Object g, int index, int height, int width,
                        boolean gridOn, boolean drawY0) {
    // Check if specInfo in null or xyCoords is null
    //Coordinate[] xyCoords = spectra[index].getXYCoords();
    Coordinate[] xyCoords = spectra.get(index).getXYCoords();

    setPlotColor(g,
        index == 1 && getSpectrum().getIntegrationGraph() != null ? -1
            : iThisSpectrum == index ? 0 : index);

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
        if (y0 >= 0 && x1 >= zoomBox1D.xPixel0 && x1 <= zoomBox1D.xPixel1) {
          setColor(g, ScriptToken.INTEGRALPLOTCOLOR);
          drawLine(g, x1, y0, x1, y1);
          setColor(g, ScriptToken.PLOTCOLOR);
          continue;
        }
        if (y1 == y2 && (y1 == yPixel0 || y1 == yPixel1))
          continue;
        drawLine(g, x1, y1, x2, y2);
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
        drawLine(g, x1, y1, x1, y2);
      }
      if (multiScaleData.isYZeroOnScale()) {
        int y = toPixelY(0);
        drawLine(g, xPixel1, y, xPixel0, y);
      }
    }
  }

  /**
   * 
   * @param g
   * @param height
   * @param width
   */
  private void drawFrame(Object g, int height, int width, boolean withGrid) {
    if (!withGrid) {
      setColor(g, ScriptToken.GRIDCOLOR);
      drawRect(g, xPixel0, yPixel0, xPixels, yPixels);
    }
    if (isCurrentGraphSet() && fracY != 1) {
      setCurrentBoxColor(g);
      drawRect(g, xPixel00 + 10, yPixel00 + 1, xPixel11 - xPixel00 - 20,
          yPixel11 - yPixel00 - 2);
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
  private void drawGrid(Object g, int height, int width) {
    setColor(g, ScriptToken.GRIDCOLOR);
    double lastX;
    if (Double.isNaN(multiScaleData.firstX)) {
      lastX = multiScaleData.maxXOnScale + multiScaleData.xStep / 2;
      for (double val = multiScaleData.minXOnScale; val < lastX; val += multiScaleData.xStep) {
        int x = toPixelX(val);
        drawLine(g, x, yPixel0, x, yPixel1);
      }
    } else {
      lastX = multiScaleData.maxXOnScale * 1.0001;
      for (double val = multiScaleData.firstX; val <= lastX; val += multiScaleData.xStep) {
        int x = toPixelX(val);
        drawLine(g, x, yPixel0, x, yPixel1);
      }
    }
    for (double val = multiScaleData.minYOnScale; val < multiScaleData.maxYOnScale
        + multiScaleData.yStep / 2; val += multiScaleData.yStep) {
      int y = toPixelY(val);
      if (y == fixY(y))
        drawLine(g, xPixel0, y, xPixel1, y);
    }
  }

  final protected int FONT_PLAIN = 0;
  final protected int FONT_BOLD = 1;
  final protected int FONT_ITALIC = 2;

  private void drawIntegralValue(Object g, int width) {
    List<Integral> integrals = getSpectrum().getIntegrals();
    if (integrals == null)
      return;
    setFont(g, width, FONT_BOLD, 12, false);
    NumberFormat formatter = getFormatter("#0.0");
    setColor(g, ScriptToken.INTEGRALPLOTCOLOR);

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
      drawLine(g, x, y1, x, y2);
      drawLine(g, x + 1, y1, x + 1, y2);
      drawString(g, s, x, (y1 + y2) / 2 + getFontHeight(g) / 3);
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
  private void drawXScale(Object g, int height, int width) {

    String hashX = "#";
    String hash1 = "0.00000000";

    if (multiScaleData.hashNums[0] <= 0)
      hashX = hash1.substring(0, Math.abs(multiScaleData.hashNums[0]) + 3);

    NumberFormat formatter = getFormatter(hashX);
    setFont(g, width, FONT_PLAIN, 12, false);
    int y1 = yPixel1;
    int y2 = yPixel1 + 3;
    double maxWidth = Math
        .abs((toPixelX(multiScaleData.xStep) - toPixelX(0)) * 0.95);
    double lastX;
    if (Double.isNaN(multiScaleData.firstX)) {
      lastX = multiScaleData.maxXOnScale + multiScaleData.xStep / 2;
      for (double val = multiScaleData.minXOnScale, vald = multiScaleData.maxXOnScale; val < lastX; val += multiScaleData.xStep, vald -= multiScaleData.xStep) {
        int x = (int) (xPixel0 + (((drawXAxisLeftToRight ? val : vald) - multiScaleData.minXOnScale) / xFactorForScale));
        setColor(g, ScriptToken.GRIDCOLOR);
        drawLine(g, x, y1, x, y2);
        setColor(g, ScriptToken.SCALECOLOR);
        String s = formatter.format(val);
        int w = getStringWidth(g, s);
        drawString(g, s, x - w / 2, y2 + getFontHeight(g));
      }
    } else {
      lastX = multiScaleData.maxXOnScale * 1.0001;
      for (double val = multiScaleData.firstX; val <= lastX; val += multiScaleData.xStep) {
        int x = toPixelX(val);
        setColor(g, ScriptToken.GRIDCOLOR);
        drawLine(g, x, y1, x, y2);
        setColor(g, ScriptToken.SCALECOLOR);
        String s = formatter.format(val);
        int w = getStringWidth(g, s);
        drawString(g, s, x - w / 2, y2 + getFontHeight(g));
        val += Math.floor(w / maxWidth) * multiScaleData.xStep;
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
  private void drawYScale(Object g, int height, int width) {

    //String hashX = "#";
    String hashY = "#";
    String hash1 = "0.00000000";
    if (multiScaleData.hashNums[1] <= 0)
      hashY = hash1.substring(0, Math.abs(multiScaleData.hashNums[1]) + 3);
    NumberFormat formatter = getFormatter(hashY);
    setFont(g, width, FONT_PLAIN, 12, false);
    double max = multiScaleData.maxYOnScale + multiScaleData.yStep / 2;
    for (double val = multiScaleData.minYOnScale; val < max; val += multiScaleData.yStep) {
      int x1 = (int) xPixel0;
      int y = toPixelY(val * userYFactor);
      setColor(g, ScriptToken.GRIDCOLOR);
      drawLine(g, x1, y, x1 - 3, y);
      setColor(g, ScriptToken.SCALECOLOR);
      String s = formatter.format(val);
      drawString(g, s, (x1 - 4 - getStringWidth(g, s)), y + getFontHeight(g)
          / 3);
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
  private void drawXUnits(Object g, int width) {
    drawUnits(g, width, spectra.get(0).getXUnits(), xPixel1, yPixel1 + 5, 0, 1);
  }

  private void drawUnits(Object g, int width, String s, int x, int y,
                         double hOff, double vOff) {
    setColor(g, ScriptToken.UNITSCOLOR);
    setFont(g, width, FONT_ITALIC, 10, false);
    drawString(g, s, (int) (x - getStringWidth(g, s) * hOff),
        (int) (y + getFontHeight(g) * vOff));
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
  private void drawYUnits(Object g, int width) {
    drawUnits(g, width, spectra.get(0).getYUnits(), 5, yPixel0, 0, -1);
  }

  // determine whether there are any ratio annotations to draw
  private void drawAnnotations(Object g, int height, int width,
                               ArrayList<Annotation> annotations,
                               ScriptToken whatColor) {
    setFont(g, width, FONT_BOLD, 12, false);
    for (int i = annotations.size(); --i >= 0;) {
      Annotation note = annotations.get(i);
      setAnnotationColor(g, note, whatColor);
      int x = (note.is2D ? isd.toPixelX(note.getXVal()) : toPixelX(note
          .getXVal()));
      int y = (note.isPixels() ? (int) (yPixel0 + 10 - note.getYVal())
          : note.is2D ? isd.toPixelY((int) note.getYVal()) : toPixelY(note
              .getYVal()));
      drawString(g, note.getText(), x + note.offsetX, y - note.offsetY);
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
      repaint();
    }
  }

  private boolean setAnnotationText(Annotation a) {
    String sval = getInput("New text?", "Set Label", a.getText());
    if (sval == null)
      return false;
    if (sval.length() == 0)
      annotations.remove(a);
    else
      a.setText(sval);
    return true;
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

  private void setToolTipForPixels(int xPixel, int yPixel, boolean display1D) {
    String hashX = "#";
    String hash1 = "0.00000000";
    if (multiScaleData.hashNums[0] <= 0)
      hashX = hash1.substring(0, Math.abs(multiScaleData.hashNums[0]) + 3);
    NumberFormat formatterX = getFormatter(hashX);
    String hashY = "#";
    if (multiScaleData.hashNums[1] <= 0)
      hashY = hash1.substring(0, Math.abs(multiScaleData.hashNums[1]) + 3);
    NumberFormat formatterY = getFormatter(hashY);

    PlotWidget pw = getPinSelected(xPixel, yPixel);
    if (pw != null) {
      if (setStartupPinTip())
        return;
      String s;
      if (pw == pin1Dx01 || pw == pin2Dx01) {
        s = formatterX.format(Math.min(pin1Dx0.getXVal(), pin1Dx1.getXVal()))
            + " - "
            + formatterX.format(Math.max(pin1Dx0.getXVal(), pin1Dx1.getXVal()));
      } else if (pw == pin1Dy01) {
        s = formatterY.format(Math.min(pin1Dy0.getYVal(), pin1Dy1.getYVal()))
            + " - "
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
      setToolTipText(s);
      return;
    }

    if (isd != null && isd.fixX(xPixel) == xPixel && fixY(yPixel) == yPixel) {
      int isub = isd.toSubspectrumIndex(yPixel);
      String s = formatterX.format(isd.toX(xPixel)) + " "
          + getSpectrum().getXUnits() + ",  " + get2DYLabel(isub, formatterX);
      setToolTipText(display1D ? s : "");
      setCoordStr(s);
      return;
    }

    if (isd != null && !display1D) {
      setToolTipText("");
      setCoordStr("");
      return;
    }
    double xPt = toX(fixX(xPixel));
    String xx = formatterX.format(xPt);

    double yPt = (isd != null && isd.isXWithinRange(xPixel) ? isd
        .toSubspectrumIndex(fixY(yPixel)) : toY(fixY(yPixel)));
    formatterY = getFormatter(hashY);
    setCoordStr("(" + xx + ", " + formatterY.format(yPt) + ")");
    if (xPixel != fixX(xPixel) || yPixel != fixY(yPixel)) {
      yPt = Double.NaN;
    } else if (nSpectra == 1) {
      // I have no idea what I was thinking here...
      //        if (!getSpectrum().isHNMR()) {
      //          yPt = spectra[0].getPercentYValueAt(xPt);
      //          xx += ", " + formatterY.format(yPt);
      //        }
    } else if (getSpectrum().getIntegrationGraph() != null) {
      yPt = spectra.get(1).getPercentYValueAt(xPt);
      xx += ", " + getFormatter("#0.0").format(yPt);
    }
    setToolTipText(Double.isNaN(yPt) ? null : xx);
  }
  
  private String get2DYLabel(int isub, NumberFormat formatterX) {
    JDXSpectrum spec = getSpectrumAt(0).getSubSpectra().get(isub);
    return formatterX.format(spec.getY2D())
        + (spec.y2DUnits.equals("HZ") ? " HZ ("
            + formatterX.format(spec.getY2DPPM()) + " PPM)" : "");
  }

  void advanceSubSpectrum(int dir) {
    JDXSpectrum spec0 = getSpectrumAt(0);
    int i = spec0.advanceSubSpectrum(dir);
    if (spec0.isForcedSubset())
      multiScaleData.setXRange(getSpectrum());
    notifySubSpectrumChange(i, getSpectrum());
  }

  String getSolutionColor() {
    Graph spectrum = getSpectrum();
    String Yunits = spectrum.getYUnits();
    return Visible.Colour(spectrum.getXYCoords(), Yunits);
  }

  static boolean getPickedCoordinates(Coordinate[] coordsClicked,
                                             Coordinate coordClicked,
                                             Coordinate coord,
                                             Coordinate actualCoord) {
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

  static PeakInfo findPeak(List<GraphSet> graphSets, String fileName,
                                  String index) {
    PeakInfo pi;
    for (int i = graphSets.size(); --i >= 0; )
      if ((pi = graphSets.get(i).findPeak(fileName, index)) != null) {
        System.out.println(" found " + pi);
        return pi;
      }
    return null;
  }
  public boolean hasFileLoaded(String filePath) {
    for (int i = spectra.size(); --i >= 0;)
      if (spectra.get(i).getFilePathForwardSlash().equals(filePath))
        return true;
    return false;
  }
}
