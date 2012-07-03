package jspecview.common;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.util.Logger;
import jspecview.util.Parser;

abstract class GraphSet {

	protected enum ArrowType { LEFT, RIGHT, UP, DOWN, RESET, HOME };
	
  abstract protected void disposeImage();
  abstract protected void draw2DImage(Object g);
  abstract protected void drawHandle(Object g, int x, int y, boolean outlineOnly);
  abstract protected void drawLine(Object g, int x0, int y0, int x1, int y1);
  abstract protected void drawRect(Object g, int xPixel02, int yPixel02,
                                   int xPixels2, int yPixels2);
  abstract protected void drawString(Object g, String s, int x, int y);
  abstract protected void drawTitle(Object g, int height, int width,
                                    String title);
  abstract protected void fillArrow(Object g, ArrowType type, int x, int y, boolean doFill);
  abstract protected void fillBox(Object g, int x0, int y0, int x1, int y1,
                                  ScriptToken whatColor);
	abstract protected void fillCircle(Object g, int x, int y);
  abstract Annotation getAnnotation(double x, double y, String text,
                                    boolean isPixels, boolean is2D,
                                    int offsetX, int offsetY);
  abstract Annotation getAnnotation(List<String> args, Annotation lastAnnotation);
  abstract protected boolean get2DImage();
  abstract protected int getFontHeight(Object g);
  abstract protected String getInput(String message, String title, String sval);
  abstract protected int getStringWidth(Object g, String s);
  abstract protected void setAnnotationColor(Object g, Annotation note, ScriptToken whatColor);
  abstract protected void setColor(Object g, ScriptToken plotcolor);
  abstract protected void setCurrentBoxColor(Object g);
  abstract protected void setPlotColor(Object g, int i);
  abstract protected void setColor(Object g, int red, int green, int blue);
  abstract protected void setStrokeBold(Object g, boolean tf);
  
  abstract void setPlotColors(Object oColors);
  abstract void setPlotColor0(Object oColor);
  abstract boolean update2dImage(boolean b);


  protected List<Highlight> highlights = new ArrayList<Highlight>();
  protected List<JDXSpectrum> spectra = new ArrayList<JDXSpectrum>(2);

  private boolean isSplittable = true;
	private boolean allowStacking = true;
  private int[] splitSpectrumPointers = new int[] {0};

  private List<Annotation> annotations;
  private List<Measurement> measurements;
  private Annotation lastAnnotation;
  private Measurement pendingMeasurement;
  private List<JDXSpectrum> graphsTemp = new ArrayList<JDXSpectrum>();
  private PlotWidget[] widgets;
  private double userYFactor = 1;
  private double yRef = 0;
  
  private final double RT2 = Math.sqrt(2.0);
	private boolean haveSingleYScale;

	/**
	 * iSpectrumMovedTo
	 * 
	 * -- indicates spectrum moved to by user
	 * 
	 * -- originally 0
	 * 
	 * -- set in mouseMovedEvent 
	 *    only when nSpectra > 1:
	 *      to iSpecBold if iSpecBold >= 0
	 *      or to -1     if showAllStacked
	 *      or to getSplitPoint(yPixel)
   * 
	 * -- used in doZoom to set spectrum number for the new View object
	 *     int iSpec = (iSpecBold >= 0 ? iSpecBold 
	 *     							: iSpectrumMovedTo);
	 * 
	 * -- used in drawAll to set the frame with the purple boundary
	 * 		int iSpec = (nSpectra == 1 ? 0 
	 * 									: !showAllStacked ? iSpectrumMovedTo 
	 * 									: iSpecBold >= 0 ? iSpecBold 
	 * 									: iSpectrumSelected);
	 * 
	 */
	/*very*/private int iSpectrumMovedTo;
	private int setSpectrumMovedTo(int i) {
		return iSpectrumMovedTo = i;
	}
	
	/**
	 * iSpectrumClicked
	 * 
	 * -- indicates spectrum clicked on by user
	 * -- when set T/F, also sets iSpectrumSelected T/F
	 *    
	 * -- initially 0
	 * 
	 * -- set in checkSpectrumClickEvent
	 *      from PanelData.setCurrentGraphSet
	 *      when nSplit == 1 && showAllStacked && isClick
	 *    to spectrum number if on spectrum 
	 *    to -1 if click is not on a spectrum 
	 *  
	 * -- set in MouseClickEvent
	 *    to previous spectrum clicked if it is a double click 
	 *      and the previous click was on a spectrum  
	 *      (also sets iSpectrumSelected in that case)
	 *   
	 * -- set in processPendingMeasurement
	 *    to index of previous pendingMeasurement when clickCount == 1
	 *
	 * -- used in mouseClickEvent
	 *    if (iSpectrumClicked >= 0) {
   *    	processPendingMeasurement(xPixel, yPixel, 2);
   *    } 
	 * 
	 * -- used in processPendingMeasurement
	 *    pendingMeasurement = new Measurement(this, spectra.get(iSpectrumClicked)...
	 *    
	 * 
	 */
	/*very*/private int iSpectrumClicked;
	private void setSpectrumClicked(int i) {
		iSpectrumClicked = setSpectrumSelected(i);
	}
  
  /**
   * iSpectrumSelected
   * 
   * -- indicates current spectrum index selected
   *    -- by clicking Left/Right arrow
   *    -- by clicking on a spectrum
   *    -- 
   *  
   * -- originally -1
   * -- [0,nSpectra) indicates selected by clicking or peak picking
   * -- Integer.MIN_VALUE -- none selected (and display none)
   *
   * -- set in PanelData.setCurrentGraphSet
   *    to currentSplitPoint when gs.nSplit > 1 && !gs.showAllStacked
   *
   * -- set in checkArrowLeftRightClick
   *    to selected spectrum if LEFT or RIGHT, or
   *    to -1 if HOME circle
   *    
   * -- set in checkSpectrumClickEvent
   *    to spectrum clicked on, or
   *    to -1 if clicked off-spectrum
   * 
   * -- set in mouseClickEvent along with iSpectrumClicked
   *    to the previously clicked spectrum when there is a double click. 
   *    
   * -- set in selectSpectrum based on filePath, type, and model
   *    to -1 if nSpectra == 1, or
   *    to the selected spectrum index if there is a match, or
   *    to Integer.MIN_VALUE if this isn't the current graph set
   *       and there is a selected spectrum already ?? 
   * 
   * -- used all over the place, in checkArrowLeftRightClick,
   *    checkArrowUpDownClick, checkSpectrum, doPlot, drawAll,
   *    drawPeakTabs, drawPlot, drawSpectrum, 
   *    getFixedSelectedSpectrumIndex, isDrawNoSpectra,
   *    and selectSpectrum,
   *    
   * -- used in doPlot
   *    to return true when
   *       a split is to be shown, or
   *       when showAllStacked is true, or
   *       when no spectrum is selected, or
   *       when this is the spectrum selected
   *       
   */
  
	/*very*/private int iSpectrumSelected = -1;
	int setSpectrumSelected(int i) {
		return iSpectrumSelected = i;
	}

	/** iSpectrumBold
	 * 
	 * -- indicates the selected spectrum that is bold when showAllStacked
	 * 
	 * -- initially -1
	 * 
	 * -- set in drawAll
   *		int iSelected = (stackSelected || !showAllStacked ? iSpectrumSelected : -1);
	 *    boolean doYScale = (!showAllStacked || nSpectra == 1 || iSelected >= 0);
	 *    setSpectrumBold(stackSelected && iSpectrumSelected >= 0 ? iSpectrumSelected : -1);
	 * 
	 * -- used in doZoom
	 *     int iSpec = (iSpectrumBold >= 0 ? iSpectrumBold : iSpectrumMovedTo);
    getView(initX, finalX, initY, finalY, startIndices, endIndices, view, iSpec);

	 * 
	 * -- used in drawAll to set the frame with the purple boundary
	 * 		int iSpec = (nSpectra == 1 ? 0 
	 * 									: !showAllStacked ? iSpectrumMovedTo 
	 * 									: iSpecBold >= 0 ? iSpecBold 
	 * 									: iSpectrumSelected);

	  if (
	  		this == pd.currentGraphSet  // is current set
	  		&& iSplit == pd.currentSplitPoint
	  		&& (
	  		  n < 2                    // just one spectrum to show
	  		  || iSpectrumBold >= 0         // stacked and selected
	  		))
	  	haveSelectedSpectrum = true; 

	 */
	/*very*/private int iSpectrumBold = -1;
	private void setSpectrumBold(int i) {
		iSpectrumBold = i;
	}

	private boolean stackSelected = true;
	
	
	//needed by PanelData
  
  View view; 
  boolean reversePlot;
  int nSplit = 1;
  
  /**
   * if nSplit > 1, then showAllStacked is false,
	 * but if nSplit == 1, then showAllStacked may be true or false
	 */
  boolean showAllStacked = true;

  // needed by AwtGraphSet
  
  protected List<View> viewList;
  protected ImageView imageView;
  protected PanelData pd;
  protected boolean sticky2Dcursor;
  protected int nSpectra; // also needed by PanelData
  

  void dispose() {
    for (int i = 0; i < spectra.size(); i++)
      spectra.get(i).dispose();
    spectra = null;
    view = null;
    viewList = null;
    annotations = null;
    lastAnnotation = null;
    pendingMeasurement = null;
    imageView = null;
    graphsTemp = null;
    widgets = null;
    disposeImage();
  }

  private double fracY = 1, fX0 = 0, fY0 = 0; // take up full screen

  private PlotWidget zoomBox1D, zoomBox2D, pin1Dx0, pin1Dx1, // ppm range -- horizontal bar on 1D spectrum 
      pin1Dy0, pin1Dy1, // y-scaling -- vertical bar on 1D spectrum and left of 2D when no 1D
      pin1Dx01, pin1Dy01, // center pins for those
      pin2Dx0, pin2Dx1, // ppm range -- horizontal bar on 2D spectrum 
      pin2Dy0, pin2Dy1, // subspectrum range -- vertical bar on 2D spectrum
      pin2Dx01, pin2Dy01, // center pins for those
      cur2Dx0, cur2Dx1, // 2D x cursors -- derived from pin1Dx0 and pin1Dx1 values
      cur2Dy; // 2D y cursor -- points to currently displayed 1D slice


	// for the 1D plot area:
  private int xPixel0, yPixel0, xPixel1, yPixel1;
  // for the overall panel section:
  private int xPixel00, yPixel00, xPixel11, yPixel11, yPixel000;
  private int xPixels, yPixels;
	private int xPixel10, xPixels0;

  private boolean allowStackedYScale = true;
  private boolean drawXAxisLeftToRight;
  private boolean xAxisLeftToRight = true;
  private boolean doDraw1DObjects = true;
	private int iPreviousSpectrumClicked = -1;
	private boolean haveSelectedSpectrum;
	

  private boolean enableZoom;
  private int currentZoomIndex;
  
  private double lastClickX;

	private boolean isDrawNoSpectra() {
		return (iSpectrumSelected == Integer.MIN_VALUE);
	}

	/**
	 * 
	 * @return spectrum index selected by user from a peak pick, a spectrum pick
	 *         with showAllStacked, but set to 0 if out of range
	 */

	private int getFixedSelectedSpectrumIndex() {
		return Math.max(iSpectrumSelected, 0);
	}

  private int height;
	private int width;
	private int right;
	private int top;
	private int left;
	private int bottom;

  private PeakInfo piMouseOver;
  private final Coordinate coordTemp = new Coordinate();
	private String nucleusX;
	private String nucleusY;
  private final static int minNumOfPointsForZoom = 3;

  final private int FONT_PLAIN = 0;
  final private int FONT_BOLD = 1;
  final private int FONT_ITALIC = 2;

	private boolean is1D2DSplit;

  PeakInfo selectPeakByFileIndex(String filePath, String index) {
  	PeakInfo pi;
  	for (int i = spectra.size(); --i >= 0;)
  		if ((pi = getSpectrumAt(i).selectPeakByFileIndex(filePath, index)) != null)
  			return pi;
  	return null;
  }

  JDXSpectrum getSpectrum() {
    // could be a 2D spectrum or a set of mass spectra
    return getSpectrumAt(getFixedSelectedSpectrumIndex()).getCurrentSubSpectrum();
  }

  void setSpectrum(JDXSpectrum spec) {
  	// T/A conversion for IR
  	clearViews();
    int pt = getFixedSelectedSpectrumIndex();
    spectra.remove(pt);
    spectra.add(pt, spec);
  	view.setSpectrumYRef(pt, spec.getYRef());
  	pendingMeasurement = null;
  }

  /**
   * Returns the <code>Spectrum</code> at the specified index
   * 
   * @param index
   *        the index of the <code>Spectrum</code>
   * @return the <code>Spectrum</code> at the specified index
   */
  JDXSpectrum getSpectrumAt(int index) {
    return spectra.get(index);
  }

  private int getSpectrumIndex(JDXSpectrum spec) {
  	for (int i = spectra.size(); --i >= 0;)
  		if (spectra.get(i) == spec)
  			return i;
  	return -1;
  }
  
  static List<GraphSet> getGraphSets(JSVPanel jsvp, List<JDXSpectrum> spectra,
                                     int startIndex, int endIndex) {
    List<GraphSet> graphSets = new ArrayList<GraphSet>();
    for (int i = 0; i < spectra.size(); i++) {
      JDXSpectrum spec = spectra.get(i);
      GraphSet graphSet = findCompatibleGraphSet(graphSets, spec);
      if (graphSet == null)
        graphSets.add(graphSet = jsvp.getNewGraphSet(null));
      graphSet.addSpec(spec);
    }
    setFractionalPositions(graphSets);
    for (int i = graphSets.size(); --i >= 0;) {
      graphSets.get(i).initGraphSet(startIndex, endIndex);
      Logger.info("JSVGraphSet " + (i + 1) + " nSpectra = "
          + graphSets.get(i).nSpectra);
    }
    return graphSets;
  }

  private static GraphSet findCompatibleGraphSet(List<GraphSet> graphSets, JDXSpectrum spec) {
    for (int i = 0; i < graphSets.size(); i++)
    	if (JDXSpectrum.areScalesCompatible(spec, graphSets.get(i).getSpectrum(), false))
    		return graphSets.get(i);
    return null;
	}
  
	private void addSpec(JDXSpectrum spec) {
    spectra.add(spec);
    nSpectra++;
  }

	void splitStack(List<GraphSet> graphSets, boolean doSplit) {
		if (doSplit && isSplittable) {
			nSplit = 0;
			splitSpectrumPointers = new int[nSpectra];
			for (int i = 0; i < nSpectra; i++) {
				splitSpectrumPointers[nSplit] = i;
				nSplit++;
			}
			showAllStacked = false;
		} else {
			nSplit = 1;
			splitSpectrumPointers[0] = 0;
			showAllStacked = allowStacking && !doSplit;
		}
		stackSelected = false;
		setFractionalPositions(graphSets);
	}
  
  static void setFractionalPositions(List<GraphSet> graphSets) {
    // for now, just a vertical stack
    int n = graphSets.size();
    double f = 0;
    int n2d = 1;
    GraphSet gs;
    for (int i = 0; i < n; i++) {
    	gs = graphSets.get(i);
      f += (gs.getSpectrumAt(0).is1D() ? 1 : n2d) * gs.nSplit;
    }
    f = 1 / f;
    double y = 0;
    for (int i = 0; i < n; i++) {
      gs = graphSets.get(i);
      double g = (gs.getSpectrumAt(0).is1D() ? f : n2d * f);
      gs.fracY = g;
      gs.fY0 = y;
      y += g * gs.nSplit;
    }
  }

	private void setPositionForFrame(int iSplit) {

		int marginalHeight = height - 40;
		xPixel00 = (int) (width * fX0);
		xPixel11 = xPixel00 + width - 1;
		yPixel000 = (int) (height * fY0);
		yPixel00 = yPixel000 + (int) (marginalHeight * fracY * iSplit);
		yPixel11 = yPixel00 + (int) (marginalHeight * fracY) - 1;
		xPixel0 = xPixel00 + left / (xPixel00 == 0 ? 1 : 2);
		xPixel10 = xPixel1 = xPixel11 - right / (xPixel11 > width - 2 ? 1 : 2);
		yPixel0 = yPixel00 + top / (yPixel00 == 0 ? 1 : 2);
		yPixel1 = yPixel11 - bottom / (yPixel11 > height - 2 ? 1 : 2);
		xPixels0 = xPixels = xPixel1 - xPixel0 + 1;
		yPixels = yPixel1 - yPixel0 + 1;		
		if (is1D2DSplit) {
			setImageWindow();
			if (pd.display1D) {
				double widthRatio = (pd.display1D ? 1.0 * (xPixels0 - imageView.xPixels)
						/ xPixels0 : 1);
				xPixels = (int) Math.floor(widthRatio * xPixels0 * 0.8);
				xPixel1 = xPixel0 + xPixels - 1;
			} else {
				xPixels = 0;
				xPixel1 = imageView.xPixel0 - 30;
			}
		}
	}

  private boolean hasPoint(int xPixel, int yPixel) {
    return (xPixel >= xPixel00 && xPixel <= xPixel11 && yPixel >= yPixel000 && yPixel <= yPixel11 * nSplit);
  }

  private boolean isInPlotRegion(int xPixel, int yPixel) {
    return (xPixel >= xPixel0 && xPixel <= xPixel1 && yPixel >= yPixel0 && yPixel <= yPixel1);
  }

	int getSplitPoint(int yPixel) {
		return Math.min((int)((yPixel - yPixel000) / (yPixel11 - yPixel00)), nSplit - 1);
	}

	boolean isSplitWidget(int xPixel, int yPixel) {
		return (isSplittable && xPixel >= xPixel11 - 20 && yPixel >= yPixel00 + 1
				&& xPixel <= xPixel11 - 10 && yPixel <= yPixel00 + 11);
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
    //null means use standard offset spectrumOffsets = new int[nSpectra];
    allowStackedYScale = true;
    if (endIndex <= 0)
      endIndex = Integer.MAX_VALUE;
    isSplittable = (nSpectra > 1);// for now, could be: getSpectrumAt(0).isSplitable();
		allowStacking = (spectra.get(0).isStackable());
    showAllStacked = allowStacking && (nSpectra > 1);
    for (int i = 0; i < nSpectra; i++) {
      int iLast = spectra.get(i).getXYCoords().length - 1;
      startIndices[i] = Coordinate.intoRange(startIndex, 0, iLast);
      endIndices[i] = Coordinate.intoRange(endIndex, 0, iLast);
      allowStackedYScale &= (spectra.get(i).getYUnits().equals(
          spectra.get(0).getYUnits()) && spectra.get(i).getUserYFactor() == spectra
          .get(0).getUserYFactor());
    }
    getView(0, 0, 0, 0, startIndices, endIndices, null, -1);
    viewList = new ArrayList<View>();
    viewList.add(view);
  }

	/**
	 * here we are selecting a spectrum based on a message from Jmol matching type
	 * and model
	 * 
	 * @param filePath
	 * @param type
	 * @param model
	 */
	boolean selectSpectrum(String filePath, String type, String model) {
		//System.out.println("selectSpec checking " + type + " " + model + " "
			//	+ filePath + "\n in " + this.spectra);
		if (nSpectra == 1) {
			setSpectrumSelected(-1);
			return false; // doesn't seem right. Why not OK if only one spectrum?
		}
		boolean haveFound = false;
		for (int i = spectra.size(); --i >= 0;)
			if ((filePath == null || getSpectrumAt(i).getFilePathForwardSlash()
					.equals(filePath))
					&& (getSpectrumAt(i).matchesPeakTypeModel(type, model))) {
				setSpectrumSelected(i);
				if (nSplit > 1)
					splitStack(pd.graphSets, true);
				//System.out.println("found: " + spectra.get(i));
				haveFound = true;
			}
		if (!haveFound && iSpectrumSelected >= 0 && this != pd.currentGraphSet)
			setSpectrumSelected(Integer.MIN_VALUE); // no plots in that case
		return haveFound;
	}

	private void getView(double x1, double x2, double y1, double y2,
			int[] startIndices, int[] endIndices, View msd, int iSpec) {
		List<JDXSpectrum> graphs = (graphsTemp.size() == 0 ? spectra : graphsTemp);
		List<JDXSpectrum> subspecs = getSpectrumAt(0).getSubSpectra();
		boolean dontUseSubspecs = (subspecs == null || subspecs.size() == 2);
		// NMR real/imaginary
		boolean is2D = !getSpectrumAt(0).is1D();
		boolean useFirstSubSpecOnly = false;
		if (is2D && useFirstSubSpecOnly || dontUseSubspecs && y1 == y2) {
			// 2D spectrum or startup
			graphs = spectra;
		} else if (y1 == y2) {
			// start up, forced subsets (too many spectra)
			view = new View(subspecs, y1, y2, 10, 10,
					getSpectrum().isContinuous());
			graphs = null;
		}
		if (graphs != null) {
			view = new View(graphs, y1, y2, startIndices,
					endIndices, 10, 10, getSpectrumAt(0).isContinuous(), iSpec);
			if (x1 != x2)
				view.setXRange(x1, x2, 10);
		}
		if (msd != null)
			view.copyScaleFactors(msd);
	}

  static GraphSet findGraphSet(List<GraphSet> graphSets, int xPixel, int yPixel) {
    for (int i = graphSets.size(); --i >= 0;)
      if (graphSets.get(i).hasPoint(xPixel, yPixel))
        return graphSets.get(i);
    return null;
  }

  private boolean isNearby(Coordinate a1, Coordinate a2, ImageView imageView, int range) {
  	double x = a1.getXVal();
    int xp1 = (imageView == null ? toPixelX(x) : imageView.toPixelX(x));
    int yp1 = toPixelY(a1.getYVal());
    x = a2.getXVal();
    int xp2 = (imageView == null ? toPixelX(x) : imageView.toPixelX(x));
    int yp2 = toPixelY(a2.getYVal());
    return (Math.abs(xp1 - xp2) + Math.abs(yp1 - yp2) < range);
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

  boolean getReversePlot() {
    return  reversePlot;
  }
  
  private void setDrawXAxis() {
    drawXAxisLeftToRight = xAxisLeftToRight ^ reversePlot;
    for (int i = 0; i < spectra.size(); i++)
      if (spectra.get(i) instanceof JDXSpectrum)
      (spectra.get(i)).setExportXAxisDirection(drawXAxisLeftToRight);
  }

  private int fixX(int xPixel) {
    return Coordinate.intoRange(xPixel, xPixel0, xPixel1);
  }

  private int toPixelX(double dx) {
    int x = (int) ((dx - view.minXOnScale) / view.xFactorForScale);
    return (int) (drawXAxisLeftToRight ? xPixel0 + x : xPixel1 - x);
  }

  private boolean isInTopBar(int xPixel, int yPixel) {
    return (xPixel == fixX(xPixel) && yPixel > pin1Dx0.yPixel0 - 2 && yPixel < pin1Dx0.yPixel1);
  }

  private boolean isInTopBar2D(int xPixel, int yPixel) {
    return (imageView != null && xPixel == imageView.fixX(xPixel)
        && yPixel > pin2Dx0.yPixel0 - 2 && yPixel < pin2Dx0.yPixel1);
  }

  private boolean isInRightBar(int xPixel, int yPixel) {
    return (yPixel == fixY(yPixel) && xPixel > pin1Dy0.xPixel1 && xPixel < pin1Dy0.xPixel0 + 2);
  }

  private boolean isInRightBar2D(int xPixel, int yPixel) {
    return (imageView != null && yPixel == fixY(yPixel) && xPixel > pin2Dy0.xPixel1 && xPixel < pin2Dy0.xPixel0 + 2);
  }

  private double toX(int xPixel) {
    if (imageView != null && imageView.isXWithinRange(xPixel))
      return imageView.toX(xPixel);
    xPixel = fixX(xPixel);
    return (drawXAxisLeftToRight ? view.maxXOnScale
        - (xPixel1 - xPixel) * view.xFactorForScale : view.minXOnScale
        + (xPixel1 - xPixel) * view.xFactorForScale);
  }

  private double toX0(int xPixel) {
    xPixel = fixX(xPixel);
    View view = viewList.get(0);
    double factor = (view.maxXOnScale - view.minXOnScale)
        / xPixels;
    return (drawXAxisLeftToRight ? view.maxXOnScale
        - (xPixel1 - xPixel) * factor : view.minXOnScale
        + (xPixel1 - xPixel) * factor);
  }

  private int toPixelX0(double x) {
    View view = viewList.get(0);
    double factor = (view.maxXOnScale - view.minXOnScale)
        / xPixels;
    return (int) (drawXAxisLeftToRight ? xPixel1
        - (view.maxXOnScale - x) / factor : xPixel1
        - (x - view.minXOnScale) / factor);
  }

  private int fixY(int yPixel) {
    return Coordinate.intoRange(yPixel, yPixel0, yPixel1);
  }

	private int toPixelY(double yVal) {
		return (Double.isNaN(yVal) ? Integer.MIN_VALUE
				: yPixel1	- (int) (((yVal - yRef) * userYFactor + yRef 
						- view.minYOnScale) / view.yFactorForScale));
	}

  private int toPixelYint(double yVal) {
    return yPixel1 - (int) (Double.isNaN(yVal) ? Integer.MIN_VALUE : yPixels * yVal);
  }

  private int toPixelY0(double y) {
    View view = viewList.get(0);
    double factor = (view.maxYOnScale - view.minYOnScale)
        / yPixels;
    return fixY((int) (yPixel0 + (view.maxYOnScale - y) / factor));
  }

  private double toY(int yPixel) {
    return view.maxYOnScale + (yPixel0 - yPixel) * view.yFactorForScale;
  }

  private double toY0(int yPixel) {
    yPixel = fixY(yPixel);
    View view = viewList.get(0);
    double factor = (view.maxYOnScale - view.minYOnScale)
        / yPixels;
    double y = view.maxYOnScale + (yPixel0 - yPixel) * factor;
    return Math.max(view.minY, Math.min(y, view.maxY));
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
          (view.maxXOnScale + view.minXOnScale) / 2,
          (view.maxYOnScale + view.minYOnScale) / 2, title,
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


  private Annotation findAnnotation2D(Coordinate xy) {
    for (int i = annotations.size(); --i >= 0;) {
      Annotation a = annotations.get(i);
      if (isNearby(a, xy, imageView, 10))
        return a;
    }
    return null;
  }

  private void addAnnotation(Annotation annotation, boolean isToggle) {
    if (annotations == null)
      annotations = new ArrayList<Annotation>();
    boolean removed = false;
    for (int i = annotations.size(); --i >= 0;)
      if (annotation.is2D ? isNearby(annotations.get(i), annotation, imageView, 10)
          : annotation.equals(annotations.get(i))) {
        removed = true;
        annotations.remove(i);
      }
    if (annotation.getText().length() > 0 && (!removed || !isToggle))
      annotations.add(annotation);
  }

	/**
	 * 
	 * @param g
	 * @param withGrid
	 * @param withXUnits
	 * @param withYUnits
	 * @param withXScale
	 * @param withYScale
	 * @param drawY0
	 * @param height
	 * @param width
	 * @param plotAreaInsets
	 * @param isResized
	 * @param enableZoom
	 */
	synchronized void drawGraph(Object og, int height, int width, int left,
			int right, int top, int bottom, boolean isResized) {
		JDXSpectrum spec0 = getSpectrumAt(0);
		int subIndex = spec0.getSubIndex();
		nucleusX = spec0.nucleusX;
		nucleusY = spec0.nucleusY;
		enableZoom = pd.getBoolean(ScriptToken.ENABLEZOOM);
		this.height = height;
		this.width = width;
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
		yValueMovedTo = Double.MIN_VALUE;
		setFormatters();
    is1D2DSplit = (!spec0.is1D() && pd.getBoolean(ScriptToken.DISPLAY2D)
		&& (imageView != null || get2DImage()));
    haveSelectedSpectrum = false;
		for (int iSplit = 0; iSplit < nSplit; iSplit++) {
			// for now, at least, we only allow one 2D image
			setPositionForFrame(iSplit);
			drawAll(og, subIndex, iSplit, isResized || nSplit > 1);
		}
		setPositionForFrame(nSplit > 1 ? pd.currentSplitPoint : 0);
	}

  protected void setImageWindow() {
    imageView.setPixelWidthHeight((int) ((pd.display1D ? 0.6 : 1) * xPixels0), yPixels);
    imageView.setXY0((int) Math.floor(xPixel10 - imageView.xPixels), yPixel0);
	}

  private int iSelectedMeasurement = -1;
  synchronized void mouseClickEvent(int xPixel, int yPixel, int clickCount,
                       boolean isControlDown) {
  	iSelectedMeasurement = -1;
  	if (clickCount == 2 && iSpectrumClicked == -1 && iPreviousSpectrumClicked >= 0) {
  		setSpectrumClicked(iPreviousSpectrumClicked);
  		stackSelected = showAllStacked;
  	}
  	if (isSplitWidget(xPixel, yPixel)) {
  		splitStack(pd.graphSets, nSplit == 1);
      pd.refresh();
  		return;
  	}
    PlotWidget pw = getPinSelected(xPixel, yPixel);
    if (pw != null) {
      setWidgetValueByUser(pw);
      return;
    }
    if (checkArrowUpDownClick(xPixel, yPixel))
    	return;
    
    boolean is2D = (imageView != null && xPixel == imageView.fixX(xPixel) && yPixel == fixY(yPixel));
    
    if (isControlDown)
      clearIntegrals();
    else if (clickCount == 2) {
      if (is2D) {
        if (sticky2Dcursor) {
          addAnnotation(
              getAnnotation(imageView.toX(xPixel), imageView.toSubspectrumIndex(yPixel),
                  pd.coordStr, false, true, 5, 5), true);
        }
        sticky2Dcursor = !sticky2Dcursor;
        set2DCrossHairs(xPixel, yPixel);
        pd.repaint();
        return;
      }
    
      // 1D double-click
      
      if (isInTopBar(xPixel, yPixel)) {
        doZoom(toX0(xPixel0), view.minY, toX0(xPixel1),
            view.maxY, true, true, false);
      } else if (isInRightBar(xPixel, yPixel)) {
        doZoom(view.minXOnScale, viewList.get(0).minY,
            view.maxXOnScale, viewList.get(0).maxY, true, true,
            false);
      } else if (isInTopBar2D(xPixel, yPixel)) {
        reset2D(true);
      } else if (isInRightBar2D(xPixel, yPixel)) {
        reset2D(false);
      } else if (pendingMeasurement != null) {
      	processPendingMeasurement(xPixel, yPixel, -2);
    	} else if (iSpectrumClicked >= 0) {
      	processPendingMeasurement(xPixel, yPixel, 2);
      } 
      return;
    }     
    
    // single click
    
    if (is2D) {
      if (annotations != null) {
        Coordinate xy = new Coordinate(imageView.toX(xPixel), imageView
            .toSubspectrumIndex(yPixel));
        Annotation a = findAnnotation2D(xy);
        if (a != null && setAnnotationText(a)) {
          pd.repaint();
          return;
        }
      }
      sticky2Dcursor = false;
      set2DCrossHairs(xPixel, yPixel);
      pd.repaint();
      return;
    }
    
    // 1D single click

    if (isInPlotRegion(xPixel, yPixel)) {
    	if (pendingMeasurement != null) {
    		processPendingMeasurement(xPixel, yPixel, 1);
    		return;
    	} 
      setCoordClicked(toX(xPixel), toY(yPixel));
    } else {
      setCoordClicked(Double.NaN, 0);
    }
    pd.notifyPeakPickedListeners();
  }

	private void processPendingMeasurement(int xPixel, int yPixel,
			int clickCount) {
		if (!isInPlotRegion(xPixel, yPixel)) {
			pendingMeasurement = null;
			return;
		}
		double x, y;
		Measurement m;
		switch (clickCount) {
		case 0: // move
    		pendingMeasurement.setPt2(this, toX(xPixel), toY(yPixel));
    	break;
		case 2: // first double click
			int i = findMeasurement(xPixel, yPixel, 1);
			if (i >= 0) {
				m = measurements.get(i);
				xPixel = toPixelX(m.getXVal());
				yPixel = toPixelY(m.getYVal());
			} else if ((i = findMeasurement(xPixel, yPixel, 2)) >= 0) {
					m = measurements.get(i);
					xPixel = toPixelX(m.getPt2().getXVal());
					yPixel = toPixelY(m.getPt2().getYVal());
			}
    	x = toX(xPixel);
    	y = toY(yPixel);
      pendingMeasurement = new Measurement(this, spectra.get(iSpectrumClicked), x, y, "", x, y);
      break;
		case 1: // single click -- save and continue
		case -2: // second double click -- save and quit
			x = toX(xPixel);
			y = toY(yPixel);
			pendingMeasurement.setPt2(this, x, y);
			if (pendingMeasurement.text.length() == 0) {
  			pendingMeasurement = null;
			} else {
  			setMeasurement(pendingMeasurement);
  			if (clickCount == 1) {
	  			setSpectrumClicked(getSpectrumIndex(pendingMeasurement.spec));
		  		pendingMeasurement = new Measurement(this, pendingMeasurement.spec, x, y, "", x, y);
  			} else {
    			pendingMeasurement = null;
  			}
			}
			pd.refresh();
			break;
		}
	}
	
	Coordinate ptTemp = new Coordinate();

	private int findMeasurement(int xPixel, int yPixel, int iPt) {
		if (measurements == null)
			return -1;
		if (iPt == 0) {
			int i = findMeasurement(xPixel, yPixel, 1);
			if (i >= 0)
				return i;
			i = findMeasurement(xPixel, yPixel, 2);
			if (i >= 0)
				return i;
			i = findMeasurement(xPixel, yPixel, -2);
			if (i >= 0)
				return i;
			return findMeasurement(xPixel, yPixel, -3);
		}
		for (int i = measurements.size(); --i >= 0;) {
			Measurement m = measurements.get(i);
			int x = toPixelX(iPt == -2 || iPt == -3 ? (m.getXVal() + m.getPt2().getXVal()) / 2 
					: iPt == 1 ? m.getXVal() : m.getPt2().getXVal());
			int y = (iPt == -3 ? yPixel1 - 2 : toPixelY(m.getYVal()));
			if (Math.abs(xPixel - x) + Math.abs(yPixel - y) < 4)
				return i;
		}
		return -1;
	}

	private void setMeasurement(Measurement m) {
		if (measurements == null)
			measurements = new ArrayList<Measurement>();
    measurements.add(new Measurement(this, m));
	}
	
	private boolean checkArrowUpDownClick(int xPixel, int yPixel) {
		boolean ok = false;
		double f = (isArrowClick(xPixel, yPixel, ArrowType.UP) ? RT2 : isArrowClick(
				xPixel, yPixel, ArrowType.DOWN) ? 1 / RT2 : 0);
		if (f != 0) {		
			if ((nSpectra == 1 || iSpectrumSelected >= 0) 
					&& spectra.get(getFixedSelectedSpectrumIndex()).isTransmittance())
				f = 1/f;
			view.scaleSpectrum(iSpectrumSelected, f);
			ok = true;
		} else if (isArrowClick(xPixel, yPixel, ArrowType.RESET)) {
			clearViews();
			view.setScaleFactor(-1, 1);
			// did not work: view.setScaleFactor(iSpectrumSelected, 1);
			ok = true;
		}

		if (ok)
			pd.refresh();
		return ok;
	}

  private boolean checkArrowLeftRightClick(int xPixel, int yPixel) {
  	if (nSplit == 1 && showAllStacked) {
  		int dx = (isArrowClick(xPixel, yPixel, ArrowType.LEFT) ? -1 
  				: isArrowClick(xPixel, yPixel, ArrowType.RIGHT) ? 1 : 0);
  		if (dx != 0) {
  			if (iSpectrumSelected + dx >= 0 && iSpectrumSelected + dx < nSpectra)
    			setSpectrumSelected(iSpectrumSelected + dx);
      	return true;
      } 
  		if (isArrowClick(xPixel, yPixel, ArrowType.HOME)) {
  			setSpectrumSelected(-1);
  			return true;
      }
  	}
		return false;
	}

	private boolean isArrowClick(int xPixel, int yPixel, ArrowType type) {
		int pt;
		switch (type) {
		case UP:
		case DOWN:
		case RESET:
			pt = (yPixel00 + yPixel11) / 2 + (type == ArrowType.UP ? -1 : type == ArrowType.DOWN ? 1 : 0) * 15;
			return (Math.abs(xPixel11 - 25 - xPixel) < 10 && Math.abs(pt - yPixel) < 10);
		case LEFT:
		case RIGHT:
		case HOME:
			pt = xPixel00 + 32 + (type == ArrowType.LEFT ? -1 : type == ArrowType.RIGHT ? 1 : 0) * 15;
			return (Math.abs(pt - xPixel) < 10 && Math.abs(yPixel11 - 10 - yPixel) < 10);
		}
		return false;
	}
	synchronized void mouseReleasedEvent() {
    PlotWidget thisWidget = pd.thisWidget;
    if (pd.isIntegralDrag) {
      if (isGoodEvent(zoomBox1D, null, true)) {
        checkIntegral(toX(zoomBox1D.xPixel0), toX(zoomBox1D.xPixel1), true);
        zoomBox1D.xPixel1 = zoomBox1D.xPixel0;
        pd.repaint();
      }
      pd.isIntegralDrag = false;
    } else if (thisWidget == zoomBox2D) {
      if (!isGoodEvent(zoomBox2D, null, true))
        return;
      imageView.setZoom(zoomBox2D.xPixel0, zoomBox2D.yPixel0, zoomBox2D.xPixel1,
          zoomBox2D.yPixel1);
      zoomBox2D.xPixel1 = zoomBox2D.xPixel0;
      doZoom(imageView.toX(imageView.xPixel0), view.minY, imageView.toX(imageView.xPixel0
          + imageView.xPixels - 1), view.maxY, true, true, false);
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
  
  private boolean inPlotMove;
  private int xPixelMovedTo = -1;
	private double yValueMovedTo;
	private double xValueMovedTo;
	private NumberFormat formatterX;
	private NumberFormat formatterY;
	private boolean showIntegration = true;
  
  synchronized void mouseMovedEvent(int xPixel, int yPixel) {
  	inPlotMove = isInPlotRegion(xPixel, yPixel);
  	xPixelMovedTo = (inPlotMove ? xPixel: -1);
  	if (nSpectra > 1) {
  		int iFrame = getSplitPoint(yPixel);
  		setPositionForFrame(iFrame);
  		setSpectrumMovedTo(iSpectrumBold >= 0 ? iSpectrumBold : showAllStacked ? -1 : iFrame);
    	if (iSpectrumMovedTo >= 0)
    		setUserYFactor(iSpectrumMovedTo);
  	}
    if (pd.isIntegralDrag) {
    } else if (pendingMeasurement != null) {
    	processPendingMeasurement(xPixel, yPixel, 0);
      setToolTipForPixels(xPixel, yPixel);
    } else {
    	iSelectedMeasurement = (inPlotMove ? findMeasurement(xPixel, yPixel, 0) : -1);
      setToolTipForPixels(xPixel, yPixel);
      if (imageView == null) {
        if (!isDrawNoSpectra()) {
          JDXSpectrum spec = getSpectrum();
          if (spec.getPeakList() != null) {
            coordTemp.setXVal(toX(xPixel));
            coordTemp.setYVal(toY(yPixel));
            piMouseOver = pd.getSpectrum().findPeakByCoord(coordTemp);
          }
        }
      } else {
        if (!pd.display1D && sticky2Dcursor)
          set2DCrossHairs(xPixel, yPixel);
      }
    }
    pd.repaint();
  }

  boolean checkWidgetEvent(int xPixel, int yPixel, boolean isPress, int clickCount) {
    if (!enableZoom)
      return false;
    PlotWidget widget = pd.thisWidget;
    if (isPress) {
      widget = getPinSelected(xPixel, yPixel);
      if (widget == null) {
        yPixel = fixY(yPixel);
        if (xPixel < xPixel1) {
          xPixel = fixX(xPixel);
          zoomBox1D.setX(toX(xPixel), xPixel);
          zoomBox1D.yPixel0 = (pd.isIntegralDrag ? yPixel0 : yPixel);
          widget = zoomBox1D;
        } else if (imageView != null && xPixel < imageView.xPixel1) {
          zoomBox2D.setX(imageView.toX(xPixel), imageView.fixX(xPixel));
          zoomBox2D.yPixel0 = yPixel;
          widget = zoomBox2D;
        }
      }
      pd.thisWidget = widget;
      return true;
    }
    if (widget == null)
      return false;

    // mouse drag with widget
    if (widget == zoomBox1D) {
      zoomBox1D.xPixel1 = fixX(xPixel);
      zoomBox1D.yPixel1 = (pd.isIntegralDrag ? yPixel1 : fixY(yPixel));
      if (pd.isIntegralDrag && zoomBox1D.xPixel0 != zoomBox1D.xPixel1)
        checkIntegral(zoomBox1D.getXVal(), toX(zoomBox1D.xPixel1), false);
      return true;
    }
    if (widget == zoomBox2D) {
      zoomBox2D.xPixel1 = imageView.fixX(xPixel);
      zoomBox2D.yPixel1 = fixY(yPixel);
      return true;
    }
    if (widget == cur2Dy) {
      yPixel = fixY(yPixel);
      cur2Dy.yPixel0 = cur2Dy.yPixel1 = yPixel;
      setCurrentSubSpectrum(imageView.toSubspectrumIndex(yPixel));
      return true;
    }
    if (widget == cur2Dx0 || widget == cur2Dx1) {
      xPixel = imageView.fixX(xPixel);
      widget.setX(imageView.toX(xPixel), xPixel);
      doZoom(cur2Dx0.getXVal(), view.minY, cur2Dx1.getXVal(),
          view.maxY, false, false, false);
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
      doZoom(pin1Dx0.getXVal(), view.minY, pin1Dx1.getXVal(),
          view.maxY, false, false, false);
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
        if (Math.min(y0, y1) == view.minY
            || Math.max(y0, y1) == view.maxY)
          return true;
        pin1Dy0.setY(y0, yPixel);
        pin1Dy1.setY(y1, yPixel1);
      }
      doZoom(view.minXOnScale, pin1Dy0.getYVal(),
          view.maxXOnScale, pin1Dy1.getYVal(), false, false, false);
      return true;
    }
    if (widget == pin2Dx0 || widget == pin2Dx1 || widget == pin2Dx01) {
      xPixel = imageView.fixX(xPixel);
      widget.setX(imageView.toX0(xPixel), xPixel);
      if (widget == pin2Dx01) {
        int dp = xPixel - (pin2Dx0.xPixel0 + pin2Dx1.xPixel0) / 2 + 1;
        xPixel = pin2Dx0.xPixel0 + dp;
        int xPixel1 = pin2Dx1.xPixel0 + dp;
        if (imageView.fixX(xPixel) != xPixel || imageView.fixX(xPixel1) != xPixel1)
          return true;
        pin2Dx0.setX(imageView.toX0(xPixel), xPixel);
        pin2Dx1.setX(imageView.toX0(xPixel1), xPixel1);
      }
      if (!isGoodEvent(pin2Dx0, pin2Dx1, true)) {
        reset2D(true);
        return false;
      }
      imageView.setView0(pin2Dx0.xPixel0, pin2Dy0.yPixel0, pin2Dx1.xPixel0,
          pin2Dy1.yPixel0);
      doZoom(pin2Dx0.getXVal(), view.minY, pin2Dx1.getXVal(),
          view.maxY, false, false, false);
      return true;
    }
    if (widget == pin2Dy0 || widget == pin2Dy1 || widget == pin2Dy01) {
      yPixel = fixY(yPixel);
      widget.setY(imageView.toSubspectrumIndex(yPixel), yPixel);
      if (widget == pin2Dy01) {
        int dp = yPixel - (pin2Dy0.yPixel0 + pin2Dy1.yPixel0) / 2 + 1;
        yPixel = pin2Dy0.yPixel0 + dp;
        int yPixel1 = pin2Dy1.yPixel0 + dp;
        if (yPixel != fixY(yPixel) || yPixel1 != fixY(yPixel1))
          return true;
        pin2Dy0.setY(imageView.toSubspectrumIndex(yPixel), yPixel);
        pin2Dy1.setY(imageView.toSubspectrumIndex(yPixel1), yPixel1);
      }
      if (!isGoodEvent(pin2Dy0, pin2Dy1, false)) {
        reset2D(false);
        return false;
      }
      imageView.setView0(pin2Dx0.xPixel0, pin2Dy0.yPixel0, pin2Dx1.xPixel1,
          pin2Dy1.yPixel1);
      return true;
    }
    return false;
  }

  private void setWidgetValueByUser(PlotWidget pw) {
    String sval;
    if (pw == cur2Dy)
      sval = "" + imageView.toSubspectrumIndex(pw.yPixel0);
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
          imageView.setView0(imageView.toPixelX0(val1), pin2Dy0.yPixel0, imageView
              .toPixelX0(val2), pin2Dy1.yPixel0);
          doZoom(val1, pin1Dy0.getYVal(), val2, pin1Dy1.getYVal(), true, true,
              false);
        } else if (pw == pin2Dy01) {
          imageView.setView0(pin2Dx0.xPixel0, imageView.toPixelY0(val1), pin2Dx1.xPixel0,
              imageView.toPixelY0(val2));
          doZoom(imageView.toX(imageView.xPixel0), view.minY, imageView.toX(imageView.xPixel0
              + imageView.xPixels - 1), view.maxY, true, true, false);
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
          pd.repaint();
        } else if (pw == pin2Dy0 || pw == pin2Dy1) {
          int val2 = (pw == pin2Dy0 ? pin2Dy1.yPixel0 : pin2Dy0.yPixel0);
          imageView.setView0(pin2Dx0.xPixel0, imageView.toPixelY((int) val),
              pin2Dx1.xPixel0, val2);
          pd.repaint();
        } else {
          double val2 = (pw == pin1Dy0 ? pin1Dy1.getYVal() : pin1Dy0.getYVal());
          doZoom(pin1Dx0.getXVal(), val, pin1Dx1.getXVal(), val2, true, true,
              false);
        }
      }
    } catch (Exception e) {
    }
  }

  void addPeakHighlight(PeakInfo peakInfo) {
    for (int i = spectra.size(); --i >= 0;) {
      JDXSpectrum spec = spectra.get(i);
      removeAllHighlights(spec);
      if (peakInfo == null || peakInfo.isClearAll() || spec != peakInfo.spectrum)
        continue;
      String peak = peakInfo.toString();
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
      pd.addHighlight(x1, x2, spec, 200, 200, 200, 200);
      if (ScaleData.isWithinRange(x1, view)
          && ScaleData.isWithinRange(x2, view)) {
        pd.repaint();
      } else {
        reset();
      }
    }
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
    pd.notifyListeners(new PeakPickEvent(pd.owner, pd.coordClicked, peak));
  }

  private Coordinate setCoordClicked(double x, double y) {
    if (Double.isNaN(x)) {
      pd.coordClicked = null;
      pd.coordsClicked = null;
      return null;
    }
    pd.coordClicked = new Coordinate(lastClickX = x, y);
    pd.coordsClicked = getSpectrum().getXYCoords();
    return pd.coordClicked;
  }

	synchronized void escapeKeyPressed() {
		if (!inPlotMove)
			return;
		if (pendingMeasurement != null) {
			pendingMeasurement = null;
			return;
		}
		pd.thisWidget = null;
		pendingMeasurement = null;
		if (zoomBox1D != null)
			zoomBox1D.xPixel0 = zoomBox1D.xPixel1 = 0;
		if (zoomBox2D != null)
			zoomBox2D.xPixel0 = zoomBox2D.xPixel1 = 0;
		if (measurements != null && iSelectedMeasurement >= 0
				&& measurements.size() >= iSelectedMeasurement) {
			measurements.remove(iSelectedMeasurement);
			iSelectedMeasurement = -1;
		}
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
  private void setWidgets(boolean needNewPins, int subIndex) {
    if (needNewPins) {
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
   * distinction between view.minY and view.minYOnScale. For
   * X these are now the same, but for Y they are not. This produces a nicer
   * grid, but also an odd jumpiness in the Y slider that is not totally
   * predictable.
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
    if (imageView != null) {
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
      pin2Dy0.setY(0, imageView.toPixelY0(0));
      int n = getSpectrumAt(0).getSubSpectra().size();
      pin2Dy1.setY(n, imageView.toPixelY0(n));
    }
    pin1Dx0.setX(view.minX, toPixelX0(view.minX));
    pin1Dx1.setX(view.maxX, toPixelX0(view.maxX));
    pin1Dy0.setY(view.minY, toPixelY0(view.minY));
    pin1Dy1.setY(view.maxY, toPixelY0(view.maxY));
    widgets = new PlotWidget[] { zoomBox1D, zoomBox2D, pin1Dx0, pin1Dx01,
        pin1Dx1, pin1Dy0, pin1Dy01, pin1Dy1, pin2Dx0, pin2Dx01, pin2Dx1,
        pin2Dy0, pin2Dy01, pin2Dy1, cur2Dx0, cur2Dx1, cur2Dy };
  }

  private void resetPinsFromView() {
    if (pin1Dx0 == null)
      return;
    pin1Dx0.setX(view.minXOnScale,
        toPixelX0(view.minXOnScale));
    pin1Dx1.setX(view.maxXOnScale,
        toPixelX0(view.maxXOnScale));
    pin1Dy0.setY(view.minY, toPixelY0(view.minY));
    pin1Dy1.setY(view.maxY, toPixelY0(view.maxY));
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
    if (imageView != null) {
      pin2Dy0.setY(pin2Dy0.getYVal(), imageView.toPixelY0(pin2Dy0.getYVal()));
      pin2Dy1.setY(pin2Dy1.getYVal(), imageView.toPixelY0(pin2Dy1.getYVal()));
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
    if (imageView != null) {
      pin2Dx0.yPixel0 = pin2Dx1.yPixel0 = pin2Dx01.yPixel0 = yPixel0 - 5;
      pin2Dx0.yPixel1 = pin2Dx1.yPixel1 = pin2Dx01.yPixel1 = yPixel0;
      pin2Dy0.xPixel0 = pin2Dy1.xPixel0 = pin2Dy01.xPixel0 = imageView.xPixel1 + 5;
      pin2Dy0.xPixel1 = pin2Dy1.xPixel1 = pin2Dy01.xPixel1 = imageView.xPixel1;
      cur2Dx0.yPixel0 = cur2Dx1.yPixel0 = yPixel1 + 15;
      cur2Dx0.yPixel1 = cur2Dx1.yPixel1 = yPixel0 - 5;
      cur2Dx0.yPixel0 = cur2Dx1.yPixel0 = yPixel1 + 15;
      cur2Dx1.yPixel1 = cur2Dx1.yPixel1 = yPixel0 - 5;
      cur2Dy.xPixel0 = (doDraw1DObjects ? (xPixel1 + imageView.xPixel0) / 2
          : imageView.xPixel0 - 15);
      cur2Dy.xPixel1 = imageView.xPixel1 + 5;
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
    pin1Dx01.setEnabled(Math.min(pin1Dx0.xPixel0, pin1Dx1.xPixel0) > xPixel0
        || Math.max(pin1Dx0.xPixel0, pin1Dx1.xPixel0) < xPixel1);
    // note that toPixelY uses userYFactor, which is spectrum-dependent. 
    // in a stacked set, this will be wrong. Perhaps no showing this pin1Dy01 then?
    pin1Dy01.setEnabled(Math.min(pin1Dy0.yPixel0, pin1Dy1.yPixel0) > Math.min(
        toPixelY(view.minY), toPixelY(view.maxY))
        || Math.max(pin1Dy0.yPixel0, pin1Dy1.yPixel0) < Math.max(
            toPixelY(view.minY), toPixelY(view.maxY)));
    if (imageView == null)
      return;
    double x = pin1Dx0.getXVal();
    cur2Dx0.setX(x, imageView.toPixelX(x));
    x = pin1Dx1.getXVal();
    cur2Dx1.setX(x, imageView.toPixelX(x));

    x = imageView.toX(imageView.xPixel0);
    pin2Dx0.setX(x, imageView.toPixelX0(x));
    x = imageView.toX(imageView.xPixel1);
    pin2Dx1.setX(x, imageView.toPixelX0(x));
    pin2Dx01.setX(0, (pin2Dx0.xPixel0 + pin2Dx1.xPixel0) / 2);

    double y = imageView.imageHeight - 1 - imageView.yView1;
    pin2Dy0.setY(y, imageView.toPixelY0(y));
    y = imageView.imageHeight - 1 - imageView.yView2;
    pin2Dy1.setY(y, imageView.toPixelY0(y));
    pin2Dy01.setY(0, (pin2Dy0.yPixel0 + pin2Dy1.yPixel0) / 2);

    cur2Dy.yPixel0 = cur2Dy.yPixel1 = imageView.toPixelY(subIndex);

    pin2Dx01
        .setEnabled(Math.min(pin2Dx0.xPixel0, pin2Dx1.xPixel0) != imageView.xPixel0
            || Math.max(pin2Dx0.xPixel0, pin2Dx1.xPixel1) != imageView.xPixel1);
    pin2Dy01.setEnabled(Math.min(pin2Dy0.yPixel0, pin2Dy1.yPixel0) != yPixel0
        || Math.max(pin2Dy0.yPixel0, pin2Dy1.yPixel1) != yPixel1);

  }

  /*-------------------- METHODS FOR SCALING AND ZOOM --------------------------*/


  void setZoom(double x1, double y1, double x2, double y2) {
    setZoomTo(0);
    if (Double.isNaN(x1)) {
      // yzoom only
      x1 = view.minX;
      x2 = view.maxX;
      imageView = null;
    }
    if (x1 == 0 && x2 == 0) {
      newPins();
    } else {
      doZoom(x1, y1, x2, y2, false, true, false);
      return;
    }
    imageView = null;
  }

  /**
   * Zooms the spectrum between two coordinates
   * 
   * @param initX
   *        TODO
   * @param initX
   *        the X start coordinate of the zoom area
   * @param initY
   *        TODO
   * @param initY
   *        the Y start coordinate of the zoom area
   * @param finalX
   *        TODO
   * @param finalX
   *        the X end coordinate of the zoom area
   * @param finalY
   *        TODO
   * @param finalY
   *        the Y end coordinate of the zoom area
   */
  private synchronized void doZoom(double initX, double initY, double finalX,
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

    boolean is2DYScaleChange = (imageView != null && (imageView.minZ != initY || imageView.maxZ != finalY));

    // determine if the range of the area selected for zooming is within the plot
    // area and if not ensure that it is

    if (checkRange) {
      if (!ScaleData.isWithinRange(initX, view)
          && !ScaleData.isWithinRange(finalX, view))
        return;
      if (!ScaleData.isWithinRange(initX, view)) {
        initX = view.minX;
      } else if (!ScaleData.isWithinRange(finalX, view)) {
        finalX = view.maxX;
      }
    } else {
      view = viewList.get(0);
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
      if (!view.setDataPointIndices(graphsTemp, initX, finalX,
          minNumOfPointsForZoom, startIndices, endIndices, false))
        return;
    } else {
      if (!view.setDataPointIndices(spectra, initX, finalX,
          minNumOfPointsForZoom, startIndices, endIndices, false))
        return;
    }

    int iSpec = (iSpectrumBold >= 0 ? iSpectrumBold : iSpectrumMovedTo);
    getView(initX, finalX, initY, finalY, startIndices, endIndices, view, iSpec);
    xPixelMovedTo = -1;
    pin1Dx0.setX(initX, toPixelX0(initX));
    pin1Dx1.setX(finalX, toPixelX0(finalX));
    pin1Dy0.setY(initY, toPixelY0(initY));
    pin1Dy1.setY(finalY, toPixelY0(finalY));
    if (imageView != null) {
      int isub = getSpectrumAt(0).getSubIndex();
      int ifix = imageView.fixSubIndex(isub);
      if (ifix != isub)
        setCurrentSubSpectrum(ifix);
      if (is2DYScaleChange)
        update2dImage(true);
    }
    if (addZoom)
      addCurrentZoom();
    if (doRepaint)
      pd.repaint();
  }

  void setCurrentSubSpectrum(int i) {
    JDXSpectrum spec0 = getSpectrumAt(0);
    i = spec0.setCurrentSubSpectrum(i);
    if (spec0.isForcedSubset())
      view.setXRange(getSpectrum());
    pd.notifySubSpectrumChange(i, getSpectrum());
  }


  void scaleYBy(double factor) {
  	view.scaleSpectrum(-1, factor);
   // doZoom(view.minX, view.minY / factor1,
     //   view.maxX, view.maxY / factor2, true, true, false);
  }

  private void addCurrentZoom() {
    // add to and clean the zoom list
    if (viewList.size() > currentZoomIndex + 1)
      for (int i = viewList.size() - 1; i > currentZoomIndex; i--)
        viewList.remove(i);
    viewList.add(view);
    currentZoomIndex++;
  }

  /**
   * Resets the spectrum to it's original view
   */
  void reset() {
    setZoomTo(0);
  }

  void setZoomTo(int i) {
    imageView = null;
    currentZoomIndex = i;
    view = viewList.get(i);
    resetPinsFromView();
    pd.refresh();
  }

  /**
   * Clears all views in the zoom list
   */
  void clearViews() {
    reset();
    // leave first zoom
    for (int i = viewList.size(); --i >= 1;)
      viewList.remove(i);
  }

	private void clearMeasurements() {
		measurements = null;
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
    if (currentZoomIndex + 1 < viewList.size())
      setZoomTo(currentZoomIndex + 1);
  }

	private void drawAll(Object g, int subIndex,
			int iSplit, boolean needNewPins) {
		if (imageView != null)
			draw2DImage(g);
		draw2DUnits(g, width, subIndex);
		doDraw1DObjects = (imageView == null || pd.display1D);
		
		int iSelected = (stackSelected || !showAllStacked ? iSpectrumSelected : -1);
	  boolean doYScale = (!showAllStacked || nSpectra == 1 || iSelected >= 0);
	  setSpectrumBold(stackSelected && iSpectrumSelected >= 0 ? iSpectrumSelected : -1);
		int n = 0;
		if (doDraw1DObjects) {
			fillBox(g, xPixel0, yPixel0, xPixel1, yPixel1, ScriptToken.PLOTAREACOLOR);
			if (iSelected < 0) {
				doYScale = true;
				for (int i = 0; i < nSpectra; i++)
					if (doPlot(i, iSplit)) {
					  if (stackSelected && iSelected == i)
					  	setSpectrumBold(i);
						if (n++ == 0)
							continue;
						if (doYScale && !view.areYScalesSame(i-1, i))
							doYScale = false;
					}
			}
		}
	  if (!pd.isPrinting) {
			int iSpec = (nSpectra == 1 ? 0 : !showAllStacked ? iSpectrumMovedTo : iSpectrumBold >= 0 ? iSpectrumBold : iSpectrumSelected);
			drawFrame(g, iSplit, iSpec);
		}

	  if (
	  		this == pd.currentGraphSet  // is current set
	  		&& iSplit == pd.currentSplitPoint
	  		&& (
	  		  n < 2                    // just one spectrum to show
	  		  || iSpectrumBold >= 0         // stacked and selected
	  		))
	  	haveSelectedSpectrum = true; 
	  haveSingleYScale = (showAllStacked && nSpectra > 1 ? allowStackedYScale && doYScale : true);
		if (haveSingleYScale) {
			setUserYFactor(iSplit);
			if (pd.gridOn && imageView == null)
				drawGrid(g);
		}
	  setWidgets(needNewPins, subIndex);
		if (doDraw1DObjects) {
			if (nSplit == 1 || showAllStacked || iSpectrumSelected == iSplit) {
				drawWidgets(g, subIndex);
				drawHighlights(g);
				drawPeakTabs(g);
			}
			// grid and YScale only if scaling is common to all spectra
			// scale-related, title, and coordinates
			if (pd.getBoolean(ScriptToken.XSCALEON))
				drawXScale(g);
			if (pd.getBoolean(ScriptToken.XUNITSON))
				drawXUnits(g);

			// the graphs
			int offset = 0;
			int yOffsetPixels = (int) (yPixels * (pd.yStackOffsetPercent / 100f));
			for (int i = 0; i < nSpectra; i++)
				if (doPlot(i, iSplit)) {
					boolean isBold = (iSpectrumBold == i);
					setUserYFactor(i);
					if (n == 1 || iSpectrumSelected == i && this == pd.currentGraphSet) {
						if (!pd.isPrinting && xPixelMovedTo >= 0)
							drawSpectrumPointer(g, i);
						if (pd.titleOn) {
							drawTitle(g, height, width, spectra.get(i).getPeakTitle());
							pd.titleDrawn = true;
						}
					}
					drawSpectrum(g, i, view.spectrumOffsets == null ? offset
							: view.spectrumOffsets[i], isBold);
					if (iSelected == i && !pd.isPrinting && this == pd.currentGraphSet) {
						setUserYFactor(iSelected);
						setCurrentBoxColor(g);
						if (iSelected > 0)
							fillArrow(g, ArrowType.LEFT, yPixel11 - 10, xPixel00 + 23, true);
						fillCircle(g, xPixel00 + 32, yPixel11 - 10);
						if (iSelected < nSpectra - 1)
							fillArrow(g, ArrowType.RIGHT, yPixel11 - 10, xPixel00 + 41, true);

					}
					offset -= yOffsetPixels;
				}

			if (haveSingleYScale) {
				if (pd.getBoolean(ScriptToken.YSCALEON))
					drawYScale(g);
				if (pd.getBoolean(ScriptToken.YUNITSON))
					drawYUnits(g);
			}
		} else {
			drawWidgets(g, subIndex);
		}
		if (annotations != null)
			drawAnnotations(g, annotations, null);
	}

	private void drawSpectrumPointer(Object g, int i) {
		setColor(g, ScriptToken.PEAKTABCOLOR);
		xValueMovedTo = toX(xPixelMovedTo);
		yValueMovedTo = spectra.get(i).getYValueAt(xValueMovedTo);
		setCoordStr(xValueMovedTo, yValueMovedTo);
		int y = toPixelY(yValueMovedTo);
		if (y == fixY(y))
			drawLine(g, xPixelMovedTo, y - 10, xPixelMovedTo, y + 10);							
	}
	
	private void setUserYFactor(int i) {
    userYFactor = view.userYFactors[i];
    yRef = view.spectrumYRefs[i];
		view.setAxisScaling(i, xPixels, yPixels);			
	}
  
	private boolean doPlot(int i, int iSplit) {
  	if (nSplit > 1)
  		return (i == splitSpectrumPointers[iSplit]);
    return (showAllStacked || iSpectrumSelected == -1 || iSpectrumSelected == i);
  }

  private void draw2DUnits(Object g, int width, int subIndex) {
    if (subIndex >= 0 && imageView != null) {
      setColor(g, ScriptToken.PLOTCOLOR);
      drawUnits(g, width, nucleusX, imageView.xPixel1 + 5, yPixel1, 1, 1.0);
      drawUnits(g, width, nucleusY, imageView.xPixel0 - 5, yPixel0, 1, 0);
    }
  }

  private void drawHighlights(Object g) {
    if (isDrawNoSpectra() || pd.isPrinting)
      return;
    JDXSpectrum spec = spectra.get(getFixedSelectedSpectrumIndex());
    drawHighlights(g, spec);
  }

  private void drawPeakTabs(Object g) {
    if (isDrawNoSpectra() || pd.isPrinting)
      return;
    JDXSpectrum spec = spectra.get(getFixedSelectedSpectrumIndex());
    List<PeakInfo> list = (
    		nSpectra == 1 || iSpectrumSelected >= 0 ? spec.getPeakList()
        : null);
    if (list != null && list.size() > 0) {
      if (piMouseOver != null && pd.isMouseUp()) {
        setColor(g, 240, 240, 240); // very faint gray box
        drawPeak(g, piMouseOver, true);
      }
      setColor(g, ScriptToken.PEAKTABCOLOR);
      for (int i = list.size(); --i >= 0;) {
        drawPeak(g, list.get(i), false);
      }
    }
  }

  private void drawPeak(Object g, PeakInfo pi, boolean isFull) {
  	if (pd.isPrinting)
  		return;
    double xMin = pi.getXMin();
    double xMax = pi.getXMax();
    if (xMin != xMax) {
      drawBar(g, xMin, xMax, null, isFull);
    }
  }

	/**
	 * 
	 * Draw sliders, pins, and zoom boxes (only one of which would ever be drawn)
	 * 
	 * @param g
	 * @param subIndex
	 */
	private void drawWidgets(Object g, int subIndex) {
		if (pd.isPrinting)
			return;
		// top/side slider bar backgrounds
		if (doDraw1DObjects) {
			fillBox(g, xPixel0, pin1Dx0.yPixel1, xPixel1, pin1Dx1.yPixel1 + 2,
					ScriptToken.GRIDCOLOR);
			fillBox(g, pin1Dx0.xPixel0, pin1Dx0.yPixel1, pin1Dx1.xPixel0,
					pin1Dx1.yPixel1 + 2, ScriptToken.PLOTCOLOR);
		} else {
			fillBox(g, imageView.xPixel0, pin2Dx0.yPixel1, imageView.xPixel1,
					pin2Dx0.yPixel1 + 2, ScriptToken.GRIDCOLOR);
			fillBox(g, pin2Dx0.xPixel0, pin2Dx0.yPixel1, pin2Dx1.xPixel0,
					pin2Dx1.yPixel1 + 2, ScriptToken.PLOTCOLOR);
			fillBox(g, pin2Dy0.xPixel1, yPixel1, pin2Dy1.xPixel1 + 2, yPixel0,
					ScriptToken.GRIDCOLOR);
			fillBox(g, pin2Dy0.xPixel1, pin2Dy0.yPixel1, pin2Dy1.xPixel1 + 2,
					pin2Dy1.yPixel0, ScriptToken.PLOTCOLOR);
		}
		fillBox(g, pin1Dy0.xPixel1, yPixel1, pin1Dy1.xPixel1 + 2, yPixel0,
				ScriptToken.GRIDCOLOR);
		if (!doDraw1DObjects) {
			fillBox(g, pin1Dy0.xPixel1, pin1Dy0.yPixel1, pin1Dy1.xPixel1 + 2,
					pin1Dy1.yPixel0, ScriptToken.PLOTCOLOR);
		}
		for (int i = 0; i < widgets.length; i++) {
			PlotWidget pw = widgets[i];
			if (pw == null || !pw.isPinOrCursor && !enableZoom)
				continue;
			if (pw.is2D) {
				if (pw == cur2Dx0 && !doDraw1DObjects)// || pw.is2Donly && doDraw1DObjects)
					continue;
			} else if (doDraw1DObjects == (pw == pin1Dy0 || pw == pin1Dy1 || pw == pin1Dy01)) {
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
      drawHandle(g, pw.xPixel0, pw.yPixel0, !pw.isEnabled);
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

  private void drawBar(Object g, double startX, double endX,
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
        whatColor);
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
  private void drawSpectrum(Object g, int index, 
  		int yOffset, boolean isBold) {
    // Check if specInfo in null or xyCoords is null
    JDXSpectrum spec = spectra.get(index);    
    drawPlot(g, index, spec, true, 
    		spec.isContinuous(), yOffset, isBold);
    if (showIntegration && (!showAllStacked || iSpectrumSelected == index)) {
    	if (spec.hasIntegral())
        drawPlot(g, index, spec.getIntegrationGraph(), false, true, yOffset, false);
      if (spec.getIntegralRegions() != null)
    		drawIntegralValue(g, spec, yOffset);
    }
    if (spec.integrationRatios != null)
      drawAnnotations(g, spec.integrationRatios,
          ScriptToken.INTEGRALPLOTCOLOR);
  	if (pendingMeasurement != null && pendingMeasurement.spec == spec)
  		drawMeasurement(g, pendingMeasurement);
		if (measurements != null)
			drawMeasurements(g, spec);

  }

	private void drawPlot(Object g, int index, Graph spec, 
			boolean drawY0, boolean isContinuous, int yOffset, boolean isBold) {
		if (isBold)
			setStrokeBold(g, true);
		Coordinate[] xyCoords = spec.getXYCoords();
		boolean isIntegral = (spec instanceof IntegralGraph);
		if (isIntegral && !showIntegration)
			return;
		boolean fillPeaks = (!isIntegral && pd.isIntegralDrag && spectra.get(index)
				.hasIntegral());
		setPlotColor(g, isIntegral ? -1 : iSpectrumSelected == index && nSplit == 1 && !showAllStacked ? 0 : index);
		int y0 = toPixelY(0);
		if (!drawY0 || index != 0 || y0 != fixY(y0))
			y0 = -1;
		if (isContinuous) {
			for (int i = view.startDataPointIndices[index]; i < view.endDataPointIndices[index]; i++) {
				Coordinate point1 = xyCoords[i];
				Coordinate point2 = xyCoords[i + 1];
				int y1 = (isIntegral ? toPixelYint(point1.getYVal()) : toPixelY(point1
						.getYVal()));
				if (y1 == Integer.MIN_VALUE)
					continue;
				int y2 = (isIntegral ? toPixelYint(point2.getYVal()) : toPixelY(point2
						.getYVal()));
				if (y2 == Integer.MIN_VALUE)
					continue;
				int x1 = toPixelX(point1.getXVal());
				int x2 = toPixelX(point2.getXVal());
				y1 = yOffset + fixY(y1);
				y2 = yOffset + fixY(y2);
				if (fillPeaks && y0 > 0 && x1 >= zoomBox1D.xPixel0
						&& x1 <= zoomBox1D.xPixel1) {
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
			for (int i = view.startDataPointIndices[index]; i <= view.endDataPointIndices[index]; i++) {
				Coordinate point = xyCoords[i];
				int y2 = toPixelY(point.getYVal());
				if (y2 == Integer.MIN_VALUE)
					continue;
				int x1 = toPixelX(point.getXVal());
				int y1 = toPixelY(Math.max(view.minYOnScale, 0));
				y1 = fixY(y1);
				y2 = fixY(y2);
				if (y1 == y2 && (y1 == yPixel0 || y1 == yPixel1))
					continue;
				drawLine(g, x1, y1, x1, y2);
			}
			if (view.isYZeroOnScale()) {
				int y = toPixelY(0);
				drawLine(g, xPixel1, y, xPixel0, y);
			}
		}
		if (isBold)
			setStrokeBold(g, false);
	}

	/**
	 * 
	 * @param g
	 * @param height
	 * @param width
	 */
	private void drawFrame(Object g, int iSplit, int iSpec) {
		if (!pd.gridOn) {
			setColor(g, ScriptToken.GRIDCOLOR);
			drawRect(g, xPixel0, yPixel0, xPixels, yPixels);
		}
		setCurrentBoxColor(g);
		if (this != pd.currentGraphSet || imageView != null)
			return;
		boolean addCurrentBox = ((fracY != 1 || isSplittable) && (!isSplittable
				|| nSplit == 1 || pd.currentSplitPoint == iSplit));
		if (addCurrentBox) {
			drawRect(g, xPixel00 + 10, yPixel00 + 1, xPixel11 - 20 - xPixel00,
					yPixel11 - 2 - yPixel00);
			if (isSplittable) {
				fillBox(g, xPixel11 - 20, yPixel00 + 1, xPixel11 - 10, yPixel00 + 11,
						null);
			}
		}
		if (spectra.get(0).isScalable() && (addCurrentBox || nSpectra == 1)) {
			if (iSpec >= 0 && !isDrawNoSpectra()) {
				setPlotColor(g, iSpec);
				fillArrow(g, ArrowType.UP, xPixel11 - 25, (yPixel00 + yPixel11) / 2 - 9, true);
				fillArrow(g, ArrowType.DOWN, xPixel11 - 25, (yPixel00 + yPixel11) / 2 + 9, true);
				setCurrentBoxColor(g);
			}
			fillArrow(g, ArrowType.UP, xPixel11 - 25, (yPixel00 + yPixel11) / 2 - 9, false);
			fillCircle(g, xPixel11 - 25, (yPixel00 + yPixel11) / 2);
			fillArrow(g, ArrowType.DOWN, xPixel11 - 25, (yPixel00 + yPixel11) / 2 + 9, false);
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
  private void drawGrid(Object g) {
    setColor(g, ScriptToken.GRIDCOLOR);
    double lastX;
    if (Double.isNaN(view.firstX)) {
      lastX = view.maxXOnScale + view.xStep / 2;
      for (double val = view.minXOnScale; val < lastX; val += view.xStep) {
        int x = toPixelX(val);
        drawLine(g, x, yPixel0, x, yPixel1);
      }
    } else {
      lastX = view.maxXOnScale * 1.0001;
      for (double val = view.firstX; val <= lastX; val += view.xStep) {
        int x = toPixelX(val);
        drawLine(g, x, yPixel0, x, yPixel1);
      }
    }
    for (double val = view.firstY; val < view.maxYOnScale
        + view.yStep / 2; val += view.yStep) {
      int y = toPixelY(val);
      if (y == fixY(y))
        drawLine(g, xPixel0, y, xPixel1, y);
    }
  }

  private void drawIntegralValue(Object g, JDXSpectrum spec, int yOffset) {
    List<Integral> integrals = spec.getIntegralRegions();
    pd.setFont(g, width, FONT_BOLD, 12, false);
    NumberFormat formatter = pd.getFormatter("#0.0");
    setColor(g, ScriptToken.INTEGRALPLOTCOLOR);

    for (int i = integrals.size(); --i >= 0;) {
      Integral in = integrals.get(i);
      if (in.value == 0)
        continue;
      String s = "  " + formatter.format(Math.abs(in.value));
      int x = toPixelX(in.x2);
      int y1 = yOffset + toPixelYint(in.y1);
      int y2 = yOffset + toPixelYint(in.y2);
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
  private void drawXScale(Object g) {

    String hashX = getNumberFormat(view.hashNums[0]);
    NumberFormat formatter = pd.getFormatter(hashX);
    pd.setFont(g, width, FONT_PLAIN, 12, false);
    int y1 = yPixel1;
    int y2 = yPixel1 + 3;
    double maxWidth = Math
        .abs((toPixelX(view.xStep) - toPixelX(0)) * 0.95);
    double lastX;
    if (Double.isNaN(view.firstX)) {
      lastX = view.maxXOnScale + view.xStep / 2;
      for (double val = view.minXOnScale, vald = view.maxXOnScale; val < lastX; val += view.xStep, vald -= view.xStep) {
        int x = (int) (xPixel0 + (((drawXAxisLeftToRight ? val : vald) - view.minXOnScale) / view.xFactorForScale));
        setColor(g, ScriptToken.SCALECOLOR);
        drawLine(g, x, y1, x, y2);
        setColor(g, ScriptToken.SCALECOLOR);
        String s = formatter.format(val);
        int w = getStringWidth(g, s);
        drawString(g, s, x - w / 2, y2 + getFontHeight(g));
      }
    } else {
      lastX = view.maxXOnScale * 1.0001;
      for (double val = view.firstX; val <= lastX; val += view.xStep) {
        int x = toPixelX(val);
        setColor(g, ScriptToken.SCALECOLOR);
        drawLine(g, x, y1, x, y2);
        setColor(g, ScriptToken.SCALECOLOR);
        String s = formatter.format(val);
        int w = getStringWidth(g, s);
        int n = (x + w/2 == fixX(x + w/2) ? 2 : 0);
        if (n > 0)
          drawString(g, s, x - w / n, y2 + getFontHeight(g));
        val += Math.floor(w / maxWidth) * view.xStep;
      }
    }
  }

	private static String getNumberFormat(int n) {
    String hash1 = "0.00000000";
    String hash = "#";
    if (n <= 0)
      hash = hash1.substring(0, Math.abs(n) + 3);
    else if (n > 3)
    	hash = "";
		return hash;
	}
	
	String getFormattedNumber(double x, String hash) {
	  return pd.getFormatter(hash).format(x);
	}
	
	/**
	 * Draws the y Scale
	 * 
	 * @param g
	 *          the <code>Graphics</code> object
	 * @param height
	 *          the height to be drawn in pixels
	 * @param width
	 *          the width to be drawn in pixels
	 */
	private void drawYScale(Object g) {

		String hashY = getNumberFormat(view.hashNums[1]);
		NumberFormat formatter = pd.getFormatter(hashY);
		pd.setFont(g, width, FONT_PLAIN, 12, false);
		double max = view.maxYOnScale + view.yStep / 2;
		for (double val = view.firstY; val < max; val += view.yStep) {
			int x1 = (int) xPixel0;
			int y = toPixelY(val);// was * userYFactor);
			if (y == fixY(y)) {
				setColor(g, ScriptToken.SCALECOLOR);
				drawLine(g, x1, y, x1 - 3, y);
				setColor(g, ScriptToken.SCALECOLOR);
				String s = formatter.format(val);
				if (s.startsWith("0") && s.contains("E"))
					s = "0";
				drawString(g, s, (x1 - 4 - getStringWidth(g, s)), y + getFontHeight(g)
						/ 3);
			}
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
  private void drawXUnits(Object g) {
    String units = spectra.get(0).getAxisLabel(true);
    if (units != null)
      drawUnits(g, width, units, xPixel1, yPixel1 + 5, 0, 1);
  }

  private void drawUnits(Object g, int width, String s, int x, int y,
                         double hOff, double vOff) {
    setColor(g, ScriptToken.UNITSCOLOR);
    pd.setFont(g, width, FONT_ITALIC, 10, false);
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
   */
  private void drawYUnits(Object g) {
    String units = spectra.get(0).getAxisLabel(false);
    if (units != null)
      drawUnits(g, width, units, 5, yPixel0, 0, -1);
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
    double x1;
    double x2;
    Object color;
    JDXSpectrum spectrum;

    @Override
    public String toString() {
      return "highlight " + x1 + " " + x2 + " " + spectrum;
    }

    Highlight(double x1, double x2, JDXSpectrum spec, Object color) {
      this.x1 = x1;
      this.x2 = x2;
      this.color = color;
      spectrum = spec;
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
   * Add information about a region of the displayed spectrum to be highlighted
   * 
   * @param x1
   *        the x value of the coordinate where the highlight should start
   * @param x2
   *        the x value of the coordinate where the highlight should end
   * @param spec
   * @param color
   *        the color of the highlight
   */
  void addHighlight(double x1, double x2, JDXSpectrum spec, Object oColor) {
    if (spec == null)
      spec = getSpectrumAt(0);
    Highlight hl = new Highlight(x1, x2, spec, (oColor == null ? 
        pd.getHighlightColor() : oColor));
    if (!highlights.contains(hl))
      highlights.add(hl);
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
    for (int i = highlights.size(); --i >= 0;) {
      Highlight h = highlights.get(i);
      if (h.x1 == x1 && h.x2 == x2)
        highlights.remove(i);
    }
  }

  void removeAllHighlights(JDXSpectrum spec) {
    if (spec == null) 
      highlights.clear();
    else
      for (int i = highlights.size(); --i >= 0;)
        if (highlights.get(i).spectrum == spec) {
          highlights.remove(i);
        }
  }

  void drawHighlights(Object g, JDXSpectrum spec) {
    for (int i = 0; i < highlights.size(); i++) {
      Highlight hl = highlights.get(i);
      if (hl.spectrum == spec) {
        pd.setHighlightColor(hl.color);
        drawBar(g, hl.x1, hl.x2, ScriptToken.HIGHLIGHTCOLOR,
            true);
      }
    }
  }

  // determine whether there are any ratio annotations to draw
  private void drawAnnotations(Object g,
                               List<Annotation> annotations,
                               ScriptToken whatColor) {
    pd.setFont(g, width, FONT_BOLD, 12, false);
    for (int i = annotations.size(); --i >= 0;) {
      Annotation note = annotations.get(i);
      setAnnotationColor(g, note, whatColor);
      int x = (note.is2D ? imageView.toPixelX(note.getXVal()) : toPixelX(note
          .getXVal()));
      int y = (note.isPixels() ? (int) (yPixel0 + 10 - note.getYVal())
          : note.is2D ? imageView.toPixelY((int) note.getYVal()) : toPixelY(note
              .getYVal()));
      drawString(g, note.getText(), x + note.offsetX, y - note.offsetY);
    }
  }

	private void drawMeasurements(Object g,
			JDXSpectrum spec) {
		for (int i = measurements.size(); --i >= 0;)
			if (measurements.get(i).spec == spec)
				drawMeasurement(g, measurements.get(i));
	}
	
  private void drawMeasurement(Object g, Measurement m) {
  	if (m.text.length() == 0)
  		return;
    pd.setFont(g, width, FONT_BOLD, 12, false);
    setColor(g, 0, 0, 0); // black
    int x1 = toPixelX(m.getXVal());
    int y1 = toPixelY(m.getYVal());
    int x2 = toPixelX(m.getPt2().getXVal());
    boolean drawString = (Math.abs((m.getXVal() - m.getPt2().getXVal()) / view.xFactorForScale) >= 2);
    int x = (x1 + x2)/2;
    setStrokeBold(g, true);
    if (drawString)
      drawLine(g, x1, y1, x2, y1);
    drawLine(g, x1 + 1, yPixel1 - 1, x2, yPixel1 - 1);
    setStrokeBold(g, false);
    if (drawString)
      drawString(g, m.getText(), x + m.offsetX, y1 - m.offsetY);
    drawLine(g, x1, yPixel1, x1, yPixel1 - 6);
    drawLine(g, x2, yPixel1, x2, yPixel1 - 6);
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
    if (xPixel == imageView.fixX(xPixel) && yPixel == fixY(yPixel)) {
      pin1Dx1.setX(imageView.toX(xPixel), toPixelX(imageView.toX(xPixel)));
      cur2Dx1.setX(imageView.toX(xPixel), xPixel);
      setCurrentSubSpectrum(imageView.toSubspectrumIndex(yPixel));
    }
  }

  private void reset2D(boolean isX) {
    if (isX) {
      imageView.setView0(imageView.xPixel0, pin2Dy0.yPixel0, imageView.xPixel1, pin2Dy1.yPixel0);
      doZoom(imageView.minX, view.minY, imageView.maxX, view.maxY,
          true, true, false);
    } else {
      imageView.setView0(pin2Dx0.xPixel0, imageView.yPixel0, pin2Dx1.xPixel0, imageView.yPixel1);
      pd.repaint();
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
    pd.repaint();
  }

  private void checkIntegral(double x1, double x2, boolean isFinal) {
    IntegralGraph ig = getSpectrum().getIntegrationGraph();
    if (ig == null)
      return;
    ig.addIntegralRegion(x1, x2, isFinal);
  }

  private void setFormatters() {
    formatterX = pd.getFormatter(getNumberFormat(view.hashNums[0]));
    String hashY = getNumberFormat(view.hashNums[1]);
    formatterY = pd.getFormatter(hashY);
  }
  private void setToolTipForPixels(int xPixel, int yPixel) {
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
        int isub = imageView.toSubspectrumIndex(pw.yPixel0);
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
      pd.setToolTipText(s);
      return;
    }

    if (imageView != null && imageView.fixX(xPixel) == xPixel && fixY(yPixel) == yPixel) {
      int isub = imageView.toSubspectrumIndex(yPixel);
      String s = formatterX.format(imageView.toX(xPixel)) + " "
          + getSpectrum().getAxisLabel(true) + ",  " + get2DYLabel(isub, formatterX);
      pd.setToolTipText(pd.display1D ? s : "");
      pd.coordStr = s;
      return;
    }

    if (imageView != null && !pd.display1D) {
      pd.setToolTipText("");
      pd.coordStr = "";
      return;
    }
    double xPt = toX(fixX(xPixel));

    double yPt = (imageView != null && imageView.isXWithinRange(xPixel) ? imageView
        .toSubspectrumIndex(fixY(yPixel)) : toY(fixY(yPixel)));
    String xx = setCoordStr(xPt, yPt);
    if (!isInPlotRegion(xPixel, yPixel)) {
      yPt = Double.NaN;
    } else if (nSpectra == 1) {
      // I have no idea what I was thinking here...
      //        if (!getSpectrum().isHNMR()) {
      //          yPt = spectra[0].getPercentYValueAt(xPt);
      //          xx += ", " + formatterY.format(yPt);
      //        }
    } else if (getSpectrum().hasIntegral()) {
      yPt = getSpectrum().getIntegrationGraph().getPercentYValueAt(xPt);
      xx += ", " + pd.getFormatter("#0.0").format(yPt);
    }
    pd.setToolTipText(pendingMeasurement != null || iSelectedMeasurement >= 0 
    		? "Press ESC to delete " +
    				(pendingMeasurement == null ? "\"" + measurements.get(iSelectedMeasurement).text + "\"" 
    						: "measurement")	: Double.isNaN(yPt) ? null : xx);
  }
  
  private String setCoordStr(double xPt, double yPt) {
    String xx = formatterX.format(xPt);
    pd.coordStr = "(" + xx 
      + (haveSingleYScale || iSpectrumBold >= 0 ? ", " + formatterY.format(yPt) : "") + ")";
    return xx;
	}
	private boolean setStartupPinTip() {
    if (pd.startupPinTip == null)
      return false;
    pd.setToolTipText(pd.startupPinTip);
    pd.startupPinTip = null;
    return true;
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
      view.setXRange(getSpectrum());
    pd.notifySubSpectrumChange(i, getSpectrum());
  }

  String getSolutionColor() {
    JDXSpectrum spectrum = getSpectrum();
    return (spectrum.canShowSolutionColor() ? Visible.Colour(spectrum
        .getXYCoords(), spectrum.getYUnits()) : Visible.noColor);
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

	static PeakInfo selectPeakByFileIndex(GraphSet gs, List<GraphSet> graphSets,
			String filePath, String index) {
		System.out.println("GraphSet selectPeakByFileIndex with graphsets=" + graphSets);
		System.out.println("GraphSet selectPeakByFileIndex with gs=" + gs);
		System.out.println("GraphSet selectPeakByFileIndex with filepath/index=" + filePath + " " + index);
		PeakInfo pi = (gs == null ? null : gs.selectPeakByFileIndex(filePath, index));
		if (pi != null)
			for (int i = graphSets.size(); --i >= 0;)
				if ((pi = graphSets.get(i).selectPeakByFileIndex(filePath, index)) != null)
					return pi;
		return null;
	}
  
  boolean hasFileLoaded(String filePath) {
    for (int i = spectra.size(); --i >= 0;) {
      if (spectra.get(i).getFilePathForwardSlash().equals(filePath))
        return true;
    }
    return false;
  }
  
  PeakInfo findMatchingPeakInfo(PeakInfo pi) {
    PeakInfo pi2 = null;
    for (int i = 0; i < spectra.size(); i++)
      if (spectra.get(i) instanceof JDXSpectrum && (pi2 = (spectra.get(i)).findMatchingPeakInfo(pi)) != null)
        break;
    return pi2;
  }
  
	private boolean isOnSpectrum(int xPixel, int yPixel, int index, int cutoff) {
		setUserYFactor(index);
		int yOffset = (view.spectrumOffsets == null ? index
				* (int) (yPixels * (pd.yStackOffsetPercent / 100f)) : view.spectrumOffsets[index]);

		// ONLY getSpectrumAt(0).is1D();
		JDXSpectrum spec = spectra.get(index);
		Coordinate[] xyCoords = spec.xyCoords;
		if (spec.isContinuous()) {
			for (int i = view.startDataPointIndices[index]; i < view.endDataPointIndices[index]; i++) {
				Coordinate point1 = xyCoords[i];
				Coordinate point2 = xyCoords[i + 1];
				int x1 = toPixelX(point1.getXVal());
				int x2 = toPixelX(point2.getXVal());
				int y1 = (toPixelY(point1.getYVal()));
				int y2 = (toPixelY(point2.getYVal()));
				if (y1 == Integer.MIN_VALUE || y2 == Integer.MIN_VALUE)
					continue;
				y1 = yOffset + fixY(y1);
				y2 = yOffset + fixY(y2);
				if (isOnLine(xPixel, yPixel, x1, y1, x2, y2, cutoff))
					return true;
			}
		} else {
			for (int i = view.startDataPointIndices[index]; i <= view.endDataPointIndices[index]; i++) {
				Coordinate point = xyCoords[i];
				int y2 = toPixelY(point.getYVal());
				if (y2 == Integer.MIN_VALUE)
					continue;
				int x1 = toPixelX(point.getXVal());
				int y1 = toPixelY(Math.max(view.minYOnScale, 0));
				y1 = fixY(y1);
				y2 = fixY(y2);
				if (y1 == y2 && (y1 == yPixel0 || y1 == yPixel1))
					continue;
				if (isOnLine(xPixel, yPixel, x1, y1, x1, y2, cutoff))
					return true;
			}
		}
		return false;
	}
	private static boolean isOnLine(int xPixel, int yPixel, int x1, int y1, int x2,
			int y2, int cutoff) {
		// near a point
		int dx1 = Math.abs(x1 - xPixel);
		if (dx1 < cutoff && Math.abs(y1 - yPixel) < cutoff)
			return true;
		int dx2 = x2 - xPixel;
		if (Math.abs(dx2) < cutoff && Math.abs(y2 - yPixel) < cutoff)
			return true;
		// between points
		int dy12 = y1 - y2;
		if (Math.abs(dy12) > cutoff && (y1 < yPixel) == (y2 < yPixel))
			return false;
		int dx12 = x1 - x2;
		if (Math.abs(dx12) > cutoff && (x1 < xPixel) == (x2 < xPixel))
			return false;
		return (distance(dx1, y1 - yPixel) + distance(dx2, yPixel - y2)
				< distance(dx12, dy12) + cutoff);
	}

	private static double distance(int dx, int dy) {
		return Math.sqrt(dx * dx + dy * dy);
	}

	boolean checkSpectrumClickEvent(int xPixel, int yPixel) {
		if (checkArrowLeftRightClick(xPixel, yPixel))
			return true;
		if (pendingMeasurement != null || !showAllStacked
				|| !isInPlotRegion(xPixel, yPixel))
			return false;
		// in the stacked plot area
		stackSelected = false;
		for (int i = 0; i < nSpectra; i++) {
			if (!isOnSpectrum(xPixel, yPixel, i, 2))
				continue;
			stackSelected = true;
			boolean isNew = (i != iSpectrumSelected);
			setSpectrumClicked(iPreviousSpectrumClicked = i);
			return isNew;
		}
		// but not on a spectrum
		setSpectrumClicked(-1);
		return stackSelected = false;
	}
	
	@Override
	public String toString() {
		return "gs: " + nSpectra + " " + spectra + " " + spectra.get(0).getFilePath();
	}
	
	boolean haveSelectedSpectrum() {
		return haveSelectedSpectrum;
	}

	void setShowIntegration(Boolean tfToggle) {
		showIntegration = (tfToggle == null ? !showIntegration : tfToggle.booleanValue());
	}

	boolean getShowIntegration() {
		return showIntegration;
	}

}
