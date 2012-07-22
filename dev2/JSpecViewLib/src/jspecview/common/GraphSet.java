package jspecview.common;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jspecview.common.Annotation.AType;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.util.Logger;
import jspecview.util.Parser;
import jspecview.util.TextFormat;

abstract class GraphSet {

	protected enum ArrowType {
		LEFT, RIGHT, UP, DOWN, RESET, HOME
	}

	abstract protected void disposeImage();

	abstract protected void draw2DImage(Object g);

	abstract protected void drawHandle(Object g, int x, int y, boolean outlineOnly);

	abstract protected void drawLine(Object g, int x0, int y0, int x1, int y1);

	abstract protected void drawRect(Object g, int xPixel02, int yPixel02,
			int xPixels2, int yPixels2);

	abstract protected void drawString(Object g, String s, int x, int y);

	abstract protected void drawTitle(Object g, int height, int width,
			String title);

	abstract protected void fillArrow(Object g, ArrowType type, int x, int y,
			boolean doFill);

	abstract protected void fillBox(Object g, int x0, int y0, int x1, int y1,
			ScriptToken whatColor);

	abstract protected void fillCircle(Object g, int x, int y, boolean doFill);

	abstract Annotation getAnnotation(double x, double y, String text,
			boolean isPixels, boolean is2D, int offsetX, int offsetY);

	abstract Annotation getAnnotation(List<String> args, Annotation lastAnnotation);

	abstract protected boolean get2DImage();

	abstract protected int getFontHeight(Object g);

	abstract protected int getStringWidth(Object g, String s);

	abstract protected void rotatePlot(Object g, int i, int j, int y);

	abstract protected void setAnnotationColor(Object g, Annotation note,
			ScriptToken whatColor);

	abstract protected void setColor(Object g, ScriptToken plotcolor);

	abstract protected void setCurrentBoxColor(Object g);

	abstract protected void setPlotColor(Object g, int i);

	abstract protected void setColor(Object g, int red, int green, int blue);

	abstract protected void setStrokeBold(Object g, boolean tf);

	abstract void setPlotColors(Object oColors);

	abstract void setPlotColor0(Object oColor);

	abstract boolean update2dImage(boolean forceNew, boolean isCreation);

	protected List<Highlight> highlights = new ArrayList<Highlight>();
	protected List<JDXSpectrum> spectra = new ArrayList<JDXSpectrum>(2);

	private boolean isSplittable = true;
	private boolean allowStacking = true; // not MS
	private int[] splitPointers = new int[] { 0 };

	private List<Annotation> annotations;
	private MeasurementData selectedSpectrumMeasurements;
	private MeasurementData selectedSpectrumIntegrals;
	private Annotation lastAnnotation;
	private Measurement pendingMeasurement;
	private Integral pendingIntegral;
	private List<JDXSpectrum> graphsTemp = new ArrayList<JDXSpectrum>();
	private PlotWidget[] widgets;
	private double userYFactor = 1;
	private double yRef = 0;

	final static double RT2 = Math.sqrt(2.0);
	private boolean haveSingleYScale;

	/**
	 * iSpectrumMovedTo
	 * 
	 * -- indicates spectrum moved to by user
	 * 
	 * -- originally 0
	 * 
	 * -- set in mouseMovedEvent only when nSpectra > 1: to iSpecBold if iSpecBold
	 * >= 0 or to -1 if showAllStacked or to getSplitPoint(yPixel)
	 * 
	 * -- used in doZoom to set spectrum number for the new View object int iSpec
	 * = (iSpecBold >= 0 ? iSpecBold : iSpectrumMovedTo);
	 * 
	 * -- used in drawAll to set the frame with the purple boundary int iSpec =
	 * (nSpectra == 1 ? 0 : !showAllStacked ? iSpectrumMovedTo : iSpecBold >= 0 ?
	 * iSpecBold : iSpectrumSelected);
	 * 
	 */
	/* very */private int iSpectrumMovedTo;

	private int setSpectrumMovedTo(int i) {
		return iSpectrumMovedTo = i;
	}

	/**
	 * iSpectrumClicked
	 * 
	 * -- indicates spectrum clicked on by user -- when set T/F, also sets
	 * iSpectrumSelected T/F
	 * 
	 * -- initially 0
	 * 
	 * -- set in checkSpectrumClickEvent from PanelData.setCurrentGraphSet when
	 * nSplit == 1 && showAllStacked && isClick to spectrum number if on spectrum
	 * to -1 if click is not on a spectrum
	 * 
	 * -- set in MouseClickEvent to previous spectrum clicked if it is a double
	 * click and the previous click was on a spectrum (also sets iSpectrumSelected
	 * in that case)
	 * 
	 * -- set in processPendingMeasurement to index of previous pendingMeasurement
	 * when clickCount == 1
	 * 
	 * -- used in mouseClickEvent if (iSpectrumClicked >= 0) {
	 * processPendingMeasurement(xPixel, yPixel, 2); }
	 * 
	 * -- used in processPendingMeasurement pendingMeasurement = new
	 * Measurement(this, spectra.get(iSpectrumClicked)...
	 * 
	 * 
	 */
	/* very */private int iSpectrumClicked;

	private void setSpectrumClicked(int i) {
		stackSelected = showAllStacked;
		if (i < 0 || iSpectrumClicked != i)
			lastClickX = Double.NaN;
		iSpectrumClicked = setSpectrumSelected(setSpectrumMovedTo(i));
	}

	/**
	 * iSpectrumSelected
	 * 
	 * -- indicates current spectrum index selected -- by clicking Left/Right
	 * arrow -- by clicking on a spectrum --
	 * 
	 * -- originally -1 -- [0,nSpectra) indicates selected by clicking or peak
	 * picking -- Integer.MIN_VALUE -- none selected (and display none)
	 * 
	 * -- set in PanelData.setCurrentGraphSet to currentSplitPoint when gs.nSplit
	 * > 1 && !gs.showAllStacked
	 * 
	 * -- set in checkArrowLeftRightClick to selected spectrum if LEFT or RIGHT,
	 * or to -1 if HOME circle
	 * 
	 * -- set in checkSpectrumClickEvent to spectrum clicked on, or to -1 if
	 * clicked off-spectrum
	 * 
	 * -- set in mouseClickEvent along with iSpectrumClicked to the previously
	 * clicked spectrum when there is a double click.
	 * 
	 * -- set in selectSpectrum based on filePath, type, and model to -1 if
	 * nSpectra == 1, or to the selected spectrum index if there is a match, or to
	 * Integer.MIN_VALUE if this isn't the current graph set and there is a
	 * selected spectrum already ??
	 * 
	 * -- used all over the place, in checkArrowLeftRightClick,
	 * checkArrowUpDownClick, checkSpectrum, doPlot, drawAll, drawPeakTabs,
	 * drawPlot, drawSpectrum, getFixedSelectedSpectrumIndex, isDrawNoSpectra, and
	 * selectSpectrum,
	 * 
	 * -- used in doPlot to return true when a split is to be shown, or when
	 * showAllStacked is true, or when no spectrum is selected, or when this is
	 * the spectrum selected
	 * 
	 */

	/* very */private int iSpectrumSelected = -1;

	int setSpectrumSelected(int i) {
		boolean isNew = (i != iSpectrumSelected);
		iSpectrumSelected = i;
		if (isNew)
			hideAllDialogsExceptCurrent();
		return iSpectrumSelected;
	}

	private boolean stackSelected = false;
	private BitSet bsSelected = new BitSet();

	// needed by PanelData

	ViewData viewData;
	boolean reversePlot;
	int nSplit = 1;
	int yStackOffsetPercent = 0;

	/**
	 * if nSplit > 1, then showAllStacked is false, but if nSplit == 1, then
	 * showAllStacked may be true or false
	 */
	boolean showAllStacked = true;

	// needed by AwtGraphSet

	protected List<ViewData> viewList;
	protected ImageView imageView;
	protected PanelData pd;
	protected boolean sticky2Dcursor;
	protected int nSpectra; // also needed by PanelData

	void close() {
		closeDialogs();
	}

	private void closeDialogs() {
		if (dialogs != null)
			for (Map.Entry<String, AnnotationData> e : dialogs.entrySet()) {
				AnnotationData ad = e.getValue();
				if (ad instanceof AnnotationDialog)
					((AnnotationDialog) ad).setVisible(false);
			}
	}

	void dispose() {
//		for (int i = 0; i < spectra.size(); i++)
//			spectra.get(i).dispose();
		spectra = null;
		viewData = null;
		viewList = null;
		annotations = null;
		lastAnnotation = null;
		pendingMeasurement = null;
		imageView = null;
		graphsTemp = null;
		widgets = null;
		disposeImage();
		if (dialogs != null)
			for (Map.Entry<String, AnnotationData> e : dialogs.entrySet()) {
				AnnotationData ad = e.getValue();
				if (ad instanceof AnnotationDialog)
					((AnnotationDialog) ad).dispose();
			}
		dialogs = null;
	}

	private double fracY = 1, fX0 = 0, fY0 = 0; // take up full screen

	private PlotWidget zoomBox1D, zoomBox2D, pin1Dx0, pin1Dx1, // ppm range --
			// horizontal bar
			// on 1D spectrum
			pin1Dy0, pin1Dy1, // y-scaling -- vertical bar on 1D spectrum and left of
			// 2D when no 1D
			pin1Dx01, pin1Dy01, // center pins for those
			pin2Dx0, pin2Dx1, // ppm range -- horizontal bar on 2D spectrum
			pin2Dy0, pin2Dy1, // subspectrum range -- vertical bar on 2D spectrum
			pin2Dx01, pin2Dy01, // center pins for those
			cur2Dx0, cur2Dx1, // 2D x cursors -- derived from pin1Dx0 and pin1Dx1
			// values
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
	private int iPreviousSpectrumClicked = -1;
	private boolean haveSelectedSpectrum;

	private boolean zoomEnabled;
	private int currentZoomIndex;

	private double lastClickX = Double.NaN;

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

	JDXSpectrum getSpectrum() {
		// could be a 2D spectrum or a set of mass spectra
		return getSpectrumAt(getFixedSelectedSpectrumIndex())
				.getCurrentSubSpectrum();
	}

	/**
	 * Returns the <code>Spectrum</code> at the specified index
	 * 
	 * @param index
	 *          the index of the <code>Spectrum</code>
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

	private void addSpec(JDXSpectrum spec) {
		spectra.add(spec);
		nSpectra++;
	}

	void splitStack(List<GraphSet> graphSets, boolean doSplit) {
		if (doSplit && isSplittable) {
			// nSplit = 0;
			// splitPointers = new int[nSpectra];
			// for (int i = 0; i < nSpectra; i++) {
			// splitPointers[nSplit] = i;
			// nSplit++;
			// }
			nSplit = nSpectra;
			showAllStacked = false;
			setSpectrumClicked(0);
		} else {
			nSplit = 1;
			splitPointers[0] = 0;
			showAllStacked = allowStacking && !doSplit;
			setSpectrumClicked(-1);
		}
		stackSelected = false;
		setFractionalPositions(graphSets);
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
				double widthRatio = (pd.display1D ? 1.0
						* (xPixels0 - imageView.xPixels) / xPixels0 : 1);
				xPixels = (int) Math.floor(widthRatio * xPixels0 * 0.8);
				xPixel1 = xPixel0 + xPixels - 1;
			} else {
				xPixels = 0;
				xPixel1 = imageView.xPixel0 - 30;
			}
		}
	}

	private boolean hasPoint(int xPixel, int yPixel) {
		return (xPixel >= xPixel00 && xPixel <= xPixel11 && yPixel >= yPixel000 && yPixel <= yPixel11
				* nSplit);
	}

	private boolean isInPlotRegion(int xPixel, int yPixel) {
		return (xPixel >= xPixel0 && xPixel <= xPixel1 && yPixel >= yPixel0 && yPixel <= yPixel1);
	}

	int getSplitPoint(int yPixel) {
		return Math.min(((yPixel - yPixel000) / (yPixel11 - yPixel00)),
				nSplit - 1);
	}

	private boolean isSplitWidget(int xPixel, int yPixel) {
		return (isSplittable && xPixel >= xPixel11 - 20 && yPixel >= yPixel00 + 1
				&& xPixel <= xPixel11 - 10 && yPixel <= yPixel00 + 11);
	}

	/**
	 * Initializes the graph set
	 * @param startIndex 
	 * @param endIndex 
	 * 
	 * @param spectra
	 *          the array of spectra
	 * @param startIndices
	 *          the index of the start data point
	 * @param endIndices
	 *          the index of the end data point
	 * @throws JSpecViewException
	 * @throws ScalesIncompatibleException
	 */

	void initGraphSet(int startIndex, int endIndex) {
		xAxisLeftToRight = getSpectrumAt(0).shouldDisplayXAxisIncreasing();
		setDrawXAxis();
		int[] startIndices = new int[nSpectra];
		int[] endIndices = new int[nSpectra];
		bsSelected.set(0, nSpectra);
		// null means use standard offset spectrumOffsets = new int[nSpectra];
		allowStackedYScale = true;
		if (endIndex <= 0)
			endIndex = Integer.MAX_VALUE;
		isSplittable = (nSpectra > 1);// for now, could be:
		// getSpectrumAt(0).isSplitable();
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
		getView(0, 0, 0, 0, startIndices, endIndices, null);
		viewList = new ArrayList<ViewData>();
		viewList.add(viewData);
	}

	private synchronized void getView(double x1, double x2, double y1, double y2,
			int[] startIndices, int[] endIndices, ViewData view0) {
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
			viewData = new ViewData(subspecs, y1, y2, getSpectrum().isContinuous(), getSpectrum().isInverted());
			graphs = null;
		}
		if (graphs != null) {
			viewData = new ViewData(graphs, y1, y2, startIndices, endIndices,
					getSpectrumAt(0).isContinuous(), getSpectrumAt(0).isInverted());
			if (x1 != x2)
				viewData.setXRange(x1, x2);
		}
		if (view0 != null)
			viewData.copyScaleFactors(view0);
	}

	private boolean isNearby(Coordinate a1, Coordinate a2, ImageView imageView,
			int range) {
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
	 *          true or false
	 */
	void setReversePlot(boolean val) {
		reversePlot = val;
		if (reversePlot)
			closeDialogs();
		setDrawXAxis();
	}

	private void setDrawXAxis() {
		drawXAxisLeftToRight = xAxisLeftToRight ^ reversePlot;
		for (int i = 0; i < spectra.size(); i++)
			(spectra.get(i)).setExportXAxisDirection(drawXAxisLeftToRight);
	}

	private int fixX(int xPixel) {
		return Coordinate.intoRange(xPixel, xPixel0, xPixel1);
	}

	private int toPixelX(double dx) {
		int x = (int) ((dx - viewData.minXOnScale) / viewData.xFactorForScale);
		return (drawXAxisLeftToRight ? xPixel0 + x : xPixel1 - x);
	}

	private boolean isInTopBar(int xPixel, int yPixel) {
		return (xPixel == fixX(xPixel) && yPixel > pin1Dx0.yPixel0 - 2 && yPixel < pin1Dx0.yPixel1);
	}

	private boolean isInTopBar2D(int xPixel, int yPixel) {
		return (imageView != null && xPixel == imageView.fixX(xPixel)
				&& yPixel > pin2Dx0.yPixel0 - 2 && yPixel < pin2Dx0.yPixel1);
	}

	// private boolean isInRightBar(int xPixel, int yPixel) {
	// return (yPixel == fixY(yPixel) && xPixel > pin1Dy0.xPixel1 && xPixel <
	// pin1Dy0.xPixel0 + 2);
	// }

	private boolean isInRightBar2D(int xPixel, int yPixel) {
		return (imageView != null && yPixel == fixY(yPixel)
				&& xPixel > pin2Dy0.xPixel1 && xPixel < pin2Dy0.xPixel0 + 2);
	}

	private double toX(int xPixel) {
		if (imageView != null && imageView.isXWithinRange(xPixel))
			return imageView.toX(xPixel);
		xPixel = fixX(xPixel);
		return (drawXAxisLeftToRight ? viewData.maxXOnScale - (xPixel1 - xPixel)
				* viewData.xFactorForScale : viewData.minXOnScale + (xPixel1 - xPixel)
				* viewData.xFactorForScale);
	}

	private double toX0(int xPixel) {
		xPixel = fixX(xPixel);
		ViewData view = viewList.get(0);
		double factor = (view.maxXOnScale - view.minXOnScale) / xPixels;
		return (drawXAxisLeftToRight ? view.maxXOnScale - (xPixel1 - xPixel)
				* factor : view.minXOnScale + (xPixel1 - xPixel) * factor);
	}

	private int toPixelX0(double x) {
		ViewData view = viewList.get(0);
		double factor = (view.maxXOnScale - view.minXOnScale) / xPixels;
		return (int) (drawXAxisLeftToRight ? xPixel1 - (view.maxXOnScale - x)
				/ factor : xPixel1 - (x - view.minXOnScale) / factor);
	}

	private int fixY(int yPixel) {
		return Coordinate.intoRange(yPixel, yPixel0, yPixel1);
	}

	private int toPixelY(double yVal) {
		return (Double.isNaN(yVal) ? Integer.MIN_VALUE
				: yPixel1
						- (int) (((yVal - yRef) * userYFactor + yRef - viewData.minYOnScale) / viewData.yFactorForScale));
	}

	private int toPixelYint(double yVal) {
		return yPixel1
				- (int) (Double.isNaN(yVal) ? Integer.MIN_VALUE : yPixels * yVal);
	}

	private int toPixelY0(double y) {
		ViewData view = viewList.get(0);
		double factor = (view.maxYOnScale - view.minYOnScale) / yPixels;
		return fixY((int) (yPixel0 + (view.maxYOnScale - y) / factor));
	}

	private double toY(int yPixel) {
		return viewData.maxYOnScale + (yPixel0 - yPixel) * viewData.yFactorForScale;
	}

	private double toY0(int yPixel) {
		yPixel = fixY(yPixel);
		ViewData view = viewList.get(0);
		double factor = (view.maxYOnScale - view.minYOnScale) / yPixels;
		double y = view.maxYOnScale + (yPixel0 - yPixel) * factor;
		return Math.max(view.minY, Math.min(y, view.maxY));
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
			if (annotation.is2D ? isNearby(annotations.get(i), annotation, imageView,
					10) : annotation.equals(annotations.get(i))) {
				removed = true;
				annotations.remove(i);
			}
		if (annotation.getText().length() > 0 && (!removed || !isToggle))
			annotations.add(annotation);
	}

	protected void setImageWindow() {
		imageView.setPixelWidthHeight((int) ((pd.display1D ? 0.6 : 1) * xPixels0),
				yPixels);
		imageView.setXY0((int) Math.floor(xPixel10 - imageView.xPixels), yPixel0);
	}

	private Measurement selectedMeasurement;
	private Integral selectedIntegral;

	private double lastXMax = Double.NaN;
	private int lastSpecClicked = -1;

//	private double getPeakCenter() {
//		if (nSpectra > 1 && iSpectrumClicked < 0 || Double.isNaN(lastClickX))
//			return Double.NaN;
//		return getSpectrum().findXForPeakNearest(lastClickX);
//	}

	private boolean findNearestMaxMin() {
		if (nSpectra > 1 && iSpectrumClicked < 0)
			return false;
		xValueMovedTo = getSpectrum().findXForPeakNearest(xValueMovedTo);
		setXPixelMovedTo(xValueMovedTo);
		return true;
	}

	private void setXPixelMovedTo(double x) {
		xPixelMovedTo = toPixelX(x);
		if (fixX(xPixelMovedTo) != xPixelMovedTo)
			xPixelMovedTo = -1;
		xPixelMovedTo2 = -1;
		if (x != 1e10)
			setSpectrumClicked(getFixedSelectedSpectrumIndex());
	}

	private void processPendingMeasurement(int xPixel, int yPixel, int clickCount) {
		if (!isInPlotRegion(xPixel, yPixel)) {
			pendingMeasurement = null;
			return;
		}
		double x = toX(xPixel);
		double y = toY(yPixel);
		double x0 = x;
		Measurement m;
		switch (clickCount) {
		case 0: // move
			pendingMeasurement.setPt2(toX(xPixel), toY(yPixel));
			break;
		case 3: // ctrl-click
		case 2: // 1st double-click
			JDXSpectrum spec = spectra.get(iSpectrumClicked);
			setUserYFactor(iSpectrumClicked);
			if (clickCount == 3) {
			} else {
				m = findMeasurement(selectedSpectrumMeasurements, xPixel, yPixel, 1);
				if (m != null) {
					x = m.getXVal();
					y = m.getYVal();
				} else if ((m = findMeasurement(selectedSpectrumMeasurements, xPixel,
						yPixel, 2)) != null) {
					x = m.getXVal2();
					y = m.getYVal2();
				} else {
					x = getNearestPeak(spec, x, y);
				}
			}
			pendingMeasurement = new Measurement(spec, x, y);
			pendingMeasurement.setPt2(x0, y);
			break;
		case 1: // single click -- save and continue
		case -2: // second double-click -- save and quit
		case -3: // second ctrl-click
			setUserYFactor(getSpectrumIndex(pendingMeasurement.spec));
			if (clickCount != 3 && findNearestMaxMin()) {
				xPixel = xPixelMovedTo;
			}
			x = toX(xPixel);
			y = toY(yPixel);
			pendingMeasurement.setPt2(x, y);
			if (pendingMeasurement.text.length() == 0) {
				pendingMeasurement = null;
			} else {
				setMeasurement(pendingMeasurement);
				if (clickCount == 1) {
					setSpectrumClicked(getSpectrumIndex(pendingMeasurement.spec));
					pendingMeasurement = new Measurement(pendingMeasurement.spec, x, y);
				} else {
					pendingMeasurement = null;
				}
			}
			// pd.repaint();
			break;
		case 5: // (old) control-click
			if (findNearestMaxMin()) {
				int iSpec = getFixedSelectedSpectrumIndex();
				if (Double.isNaN(lastXMax) || lastSpecClicked != iSpec
						|| pendingMeasurement == null) {
					lastXMax = xValueMovedTo;
					lastSpecClicked = iSpec;
					pendingMeasurement = new Measurement(spectra.get(iSpec),
							xValueMovedTo, yValueMovedTo);
				} else {
					pendingMeasurement.setPt2(xValueMovedTo, yValueMovedTo);
					if (pendingMeasurement.text.length() > 0)
						setMeasurement(pendingMeasurement);
					pendingMeasurement = null;
					lastXMax = Double.NaN;
				}
				// pd.repaint();
			} else {
				lastXMax = Double.NaN;
			}
			break;
		}
	}

	private boolean checkIntegralNormalizationClick(int xPixel, int yPixel) {
		if (selectedSpectrumIntegrals == null)
			return false;
		Integral integral = (Integral) findMeasurement(selectedSpectrumIntegrals,
				xPixel, yPixel, -5);
		if (integral == null)
			return false;
		selectedIntegral = integral;
		pd.normalizeIntegral();
		updateDialog(AType.Integration, -1);
		setSpectrumClicked(getSpectrumIndex(integral.spec));
		return true;
	}

	/**
	 * search for the nearest peak above/below the given y value
	 * 
	 * @param spec
	 * 
	 * @param x
	 * @param y
	 * @param x2
	 * @return  nearest x value
	 */
	private double getNearestPeak(JDXSpectrum spec, double x, double y) {
		double x0 = Coordinate.getNearestXWithYAbove(spec.getXYCoords(), x, y, spec
				.isInverted(), false);
		double x1 = Coordinate.getNearestXWithYAbove(spec.getXYCoords(), x, y, spec
				.isInverted(), true);
		return (Double.isNaN(x0) ? x1 : Double.isNaN(x1) ? x0
				: Math.abs(x0 - x) < Math.abs(x1 - x) ? x0 : x1);
	}

	private Measurement findMeasurement(MeasurementData measurements, int xPixel,
			int yPixel, int iPt) {
		if (measurements == null || measurements.size() == 0)
			return null;
		if (iPt == 0) {
			Measurement m = findMeasurement(measurements, xPixel, yPixel, -1);
			if (m != null || measurements.get(0) instanceof Integral)
				return m;
			return findMeasurement(measurements, xPixel, yPixel, -2); // lower bar,
			// near baseline
		}
		for (int i = measurements.size(); --i >= 0;) {
			Measurement m = measurements.get(i);
			int x1, x2, y1, y2;
			if (m instanceof Integral) {
				x1 = x2 = toPixelX(m.getXVal2());
				y1 = toPixelYint(m.getYVal());
				y2 = toPixelYint(m.getYVal2());
			} else {
				x1 = toPixelX(m.getXVal());
				x2 = toPixelX(m.getXVal2());
				y1 = y2 = (iPt == -2 ? yPixel1 - 2 : toPixelY(m.getYVal()));
			}
			switch (iPt) {
			case 1:
				if (Math.abs(xPixel - x1) + Math.abs(yPixel - y1) < 4)
					return m;
				break;
			case 2:
				if (Math.abs(xPixel - x2) + Math.abs(yPixel - y2) < 4)
					return m;
				break;
			case -5: // label for integral
				y1 = y2 = (y1 + y2) / 2;
				x2 = x1 + 20; // estimate only
				//$FALL-THROUGH$
			default:
				if (isOnLine(xPixel, yPixel, x1, y1, x2, y2))
					return m;
				break;
			}

		}
		return null;
	}

	private void setMeasurement(Measurement m) {
		int iSpec = getSpectrumIndex(m.spec);
		AnnotationData ad = getDialog(AType.Measurements, iSpec);
		if (ad == null)
			addDialog(iSpec, AType.Measurements, ad = new MeasurementData(
					AType.Measurements, m.spec));
		ad.getData().add(new Measurement(m));
		updateDialog(AType.Measurements, -1);
	}

	private boolean checkArrowUpDownClick(int xPixel, int yPixel) {
		boolean ok = false;
		double f = (isArrowClick(xPixel, yPixel, ArrowType.UP) ? RT2
				: isArrowClick(xPixel, yPixel, ArrowType.DOWN) ? 1 / RT2 : 0);
		if (f != 0) {
			if (nSplit > 1)
				setSpectrumSelected(iSpectrumMovedTo);
			if ((nSpectra == 1 || iSpectrumSelected >= 0)
					&& spectra.get(getFixedSelectedSpectrumIndex()).isTransmittance())
				f = 1 / f;
			viewData.scaleSpectrum(imageView == null ? iSpectrumSelected : -2, f);
			ok = true;
		} else if (isArrowClick(xPixel, yPixel, ArrowType.RESET)) {
			clearViews();
			if (showAllStacked && !stackSelected)
  			closeDialogs();
			viewData.setScaleFactor(-1, 1);
			// did not work: view.setScaleFactor(iSpectrumSelected, 1);
			updateDialogs();
			ok = true;
		}

		if (ok) {
			if (imageView != null) {
				update2dImage(true, false);
				resetPinsFromView();
			}
			// pd.repaint();
		}
		return ok;
	}

	private boolean checkArrowLeftRightClick(int xPixel, int yPixel) {
		if (haveLeftRightArrows) {
			int dx = (isArrowClick(xPixel, yPixel, ArrowType.LEFT) ? -1
					: isArrowClick(xPixel, yPixel, ArrowType.RIGHT) ? 1 : 0);
			if (dx != 0) {
				setSpectrumClicked((iSpectrumSelected + dx) % nSpectra);
				return true;
			}
			if (isArrowClick(xPixel, yPixel, ArrowType.HOME)) {
				if (showAllStacked) {
					showAllStacked = false;
					setSpectrumClicked(getFixedSelectedSpectrumIndex());
				} else {
					showAllStacked = allowStacking;
					setSpectrumSelected(-1);
					stackSelected = false;
					// if (showAllStacked && iSpectrumSelected >= 0)
					// stackSelected = true;
				}
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
			pt = (yPixel00 + yPixel11) / 2
					+ (type == ArrowType.UP ? -1 : type == ArrowType.DOWN ? 1 : 0) * 15;
			return (Math.abs(xPixel11 - 25 - xPixel) < 10 && Math.abs(pt - yPixel) < 10);
		case LEFT:
		case RIGHT:
		case HOME:
			pt = xPixel00 + 32
					+ (type == ArrowType.LEFT ? -1 : type == ArrowType.RIGHT ? 1 : 0)
					* 15;
			return (Math.abs(pt - xPixel) < 10 && Math.abs(yPixel11 - 10 - yPixel) < 10);
		}
		return false;
	}

	private static final int MIN_DRAG_PIXELS = 5;// fewer than this means no zoom
	// or reset

	private boolean inPlotMove;
	private int xPixelMovedTo = -1;
	private int xPixelMovedTo2 = -1;
	private double yValueMovedTo;
	private double xValueMovedTo;
	private boolean haveLeftRightArrows;
	private int xPixelPlot1;
	private int xPixelPlot0;
	private int yPixelPlot0;
	private int yPixelPlot1;
	private boolean nextClickForSetPeak;

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
		sval = pd.getInput("New value?", "Set Slider", sval);
		if (sval == null)
			return;
		sval = sval.trim();
		try {
			if (pw == pin1Dx01 || pw == pin1Dy01 || pw == pin2Dx01 || pw == pin2Dy01) {
				int pt = sval.indexOf("-", 1);
				if (pt < 0)
					return;
				double val1 = Double.valueOf(sval.substring(0, pt)).doubleValue();
				double val2 = Double.valueOf(sval.substring(pt + 1)).doubleValue();
				if (pw == pin1Dx01) {
					doZoom(val1, pin1Dy0.getYVal(), val2, pin1Dy1.getYVal(), true, false,
							true);
				} else if (pw == pin1Dy01) { // actually for 2D Z
					doZoom(pin1Dx0.getXVal(), val1, pin1Dx1.getXVal(), val2, true, false,
							false);
				} else if (pw == pin2Dx01) {
					imageView.setView0(imageView.toPixelX0(val1), pin2Dy0.yPixel0,
							imageView.toPixelX0(val2), pin2Dy1.yPixel0);
					doZoom(val1, pin1Dy0.getYVal(), val2, pin1Dy1.getYVal(), true, false,
							false);
				} else if (pw == pin2Dy01) {
					imageView.setView0(pin2Dx0.xPixel0, imageView.toPixelY0(val1),
							pin2Dx1.xPixel0, imageView.toPixelY0(val2));
					doZoom(imageView.toX(imageView.xPixel0), viewData.minY, imageView
							.toX(imageView.xPixel0 + imageView.xPixels - 1), viewData.maxY,
							true, false, false);
				}
			} else {
				double val = Double.valueOf(sval).doubleValue();
				if (pw.isXtype) {
					double val2 = (pw == pin1Dx0 || pw == cur2Dx0 || pw == pin2Dx0 ? pin1Dx1
							.getXVal()
							: pin1Dx0.getXVal());
					doZoom(val, pin1Dy0.getYVal(), val2, pin1Dy1.getYVal(), true, false,
							true);
				} else if (pw == cur2Dy) {
					setCurrentSubSpectrum((int) val);
					// pd.repaint();
				} else if (pw == pin2Dy0 || pw == pin2Dy1) {
					int val2 = (pw == pin2Dy0 ? pin2Dy1.yPixel0 : pin2Dy0.yPixel0);
					imageView.setView0(pin2Dx0.xPixel0, imageView.toPixelY((int) val),
							pin2Dx1.xPixel0, val2);
					// pd.repaint();
				} else {
					double val2 = (pw == pin1Dy0 ? pin1Dy1.getYVal() : pin1Dy0.getYVal());
					doZoom(pin1Dx0.getXVal(), val, pin1Dx1.getXVal(), val2, true, false,
							false);
				}
			}
		} catch (Exception e) {
		}
	}

	private void removeAllHighlights(JDXSpectrum spec) {
		if (spec == null)
			highlights.clear();
		else
			for (int i = highlights.size(); --i >= 0;)
				if (highlights.get(i).spectrum == spec)
					highlights.remove(i);
	}

	private Coordinate setCoordClicked(double x, double y) {
		if (y == 0)
			nextClickForSetPeak = false;
		if (Double.isNaN(x)) {
			pd.coordClicked = null;
			pd.coordsClicked = null;
			return null;
		}
		pd.coordClicked = new Coordinate(lastClickX = x, y);
		pd.coordsClicked = getSpectrum().getXYCoords();
		return pd.coordClicked;
	}

	/**
	 * PlotWidgets are zoom boxes and slider points that are draggable. Some are
	 * derived from others (center points and the 2D subIndex pointer). The first
	 * time through, we have to create new pins. When the frame is resized, we
	 * need to reset their positions along the slider based on their values, and
	 * we need to also move the sliders to the right place.
	 * @param needNewPins 
	 * 
	 * @param isResized
	 * @param subIndex
	 * @param doDraw1DObjects 
	 */
	private void setWidgets(boolean needNewPins, int subIndex,
			boolean doDraw1DObjects) {
		if (needNewPins) {
			if (zoomBox1D == null)
				newPins();
			else
				resetPinPositions();
		}
		setDerivedPins(subIndex);
		setPinSliderPositions(doDraw1DObjects);
	}

	/**
	 * Create new pins and set their default values. Note that we are making a
	 * distinction between view.minY and view.minYOnScale. For X these are now the
	 * same, but for Y they are not. This produces a nicer grid, but also an odd
	 * jumpiness in the Y slider that is not totally predictable.
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
		pin1Dx0.setX(viewData.minX, toPixelX0(viewData.minX));
		pin1Dx1.setX(viewData.maxX, toPixelX0(viewData.maxX));
		pin1Dy0.setY(viewData.minY, toPixelY0(viewData.minY));
		pin1Dy1.setY(viewData.maxY, toPixelY0(viewData.maxY));
		widgets = new PlotWidget[] { zoomBox1D, zoomBox2D, pin1Dx0, pin1Dx01,
				pin1Dx1, pin1Dy0, pin1Dy01, pin1Dy1, pin2Dx0, pin2Dx01, pin2Dx1,
				pin2Dy0, pin2Dy01, pin2Dy1, cur2Dx0, cur2Dx1, cur2Dy };
	}

	private void resetPinsFromView() {
		if (pin1Dx0 == null)
			return;
		pin1Dx0.setX(viewData.minXOnScale, toPixelX0(viewData.minXOnScale));
		pin1Dx1.setX(viewData.maxXOnScale, toPixelX0(viewData.maxXOnScale));
		pin1Dy0.setY(viewData.minY, toPixelY0(viewData.minY));
		pin1Dy1.setY(viewData.maxY, toPixelY0(viewData.maxY));
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
	 * @param doDraw1DObjects
	 * 
	 */
	private void setPinSliderPositions(boolean doDraw1DObjects) {
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
		// in a stacked set, this will be wrong. Perhaps no showing this pin1Dy01
		// then?
		pin1Dy01.setEnabled(Math.min(pin1Dy0.yPixel0, pin1Dy1.yPixel0) > Math.min(
				toPixelY(viewData.minY), toPixelY(viewData.maxY))
				|| Math.max(pin1Dy0.yPixel0, pin1Dy1.yPixel0) < Math.max(
						toPixelY(viewData.minY), toPixelY(viewData.maxY)));
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

	/**
	 * Zooms the spectrum between two coordinates
	 * 
	 * @param initX
	 *          TODO
	 * @param initX
	 *          the X start coordinate of the zoom area
	 * @param initY
	 *          TODO
	 * @param initY
	 *          the Y start coordinate of the zoom area
	 * @param finalX
	 *          TODO
	 * @param finalX
	 *          the X end coordinate of the zoom area
	 * @param finalY
	 *          TODO
	 * @param finalY
	 *          the Y end coordinate of the zoom area
	 * @param addZoom 
	 * @param checkRange 
	 * @param is1D
	 *          TODO
	 */
	private synchronized void doZoom(double initX, double initY, double finalX,
			double finalY, boolean addZoom, boolean checkRange, boolean is1D) {
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

		boolean is2DGrayScaleChange = (imageView != null && (imageView.minZ != initY || imageView.maxZ != finalY));

		if (!zoomEnabled && !is2DGrayScaleChange)
			return;

		// determine if the range of the area selected for zooming is within the
		// plot
		// area and if not ensure that it is

		ViewData view0 = viewData;
		if (checkRange) {
			if (!viewData.isInRangeX(initX)
					&& !viewData.isInRangeX(finalX))
				return;
			if (!viewData.isInRangeX(initX)) {
				initX = viewData.minX;
			} else if (!viewData.isInRangeX(finalX)) {
				finalX = viewData.maxX;
			}
		} else {
			viewData = viewList.get(0);
		}
		int[] startIndices = new int[nSpectra];
		int[] endIndices = new int[nSpectra];
		graphsTemp.clear();
		List<JDXSpectrum> subspecs = getSpectrumAt(0).getSubSpectra();
		boolean dontUseSubspecs = (subspecs == null || subspecs.size() == 2);
		// NMR real/imaginary
		boolean is2D = !getSpectrumAt(0).is1D();
		if (!is2D && !dontUseSubspecs) {
			graphsTemp.add(getSpectrum());
			if (!viewData.setDataPointIndices(graphsTemp, initX, finalX,
					minNumOfPointsForZoom, startIndices, endIndices, false))
				return;
		} else {
			if (!viewData.setDataPointIndices(spectra, initX, finalX,
					minNumOfPointsForZoom, startIndices, endIndices, false))
				return;
		}
		double f = (!is2DGrayScaleChange && is1D ? f = viewData
				.getSpectrumScaleFactor(iSpectrumSelected) : 1);
		double y1 = initY;
		double y2 = finalY;
		if (f != 1) {
			// not clear this is right for IR spec or some other cases...
			y1 = viewData.unScaleY(iSpectrumSelected, initY);
			y2 = viewData.unScaleY(iSpectrumSelected, finalY);
		}
		getView(initX, finalX, y1, y2, startIndices, endIndices, view0);
		setXPixelMovedTo(1E10);
		pin1Dx0.setX(initX, toPixelX0(initX));
		pin1Dx1.setX(finalX, toPixelX0(finalX));
		pin1Dy0.setY(initY, toPixelY0(initY));
		pin1Dy1.setY(finalY, toPixelY0(finalY));
		if (imageView == null) {
			updateDialogs();
		} else {
			int isub = getSpectrumAt(0).getSubIndex();
			int ifix = imageView.fixSubIndex(isub);
			if (ifix != isub)
				setCurrentSubSpectrum(ifix);
			if (is2DGrayScaleChange)
				update2dImage(true, false);
		}
		if (addZoom)
			addCurrentZoom();
		// if (doRepaint)
		// pd.repaint();
	}

	private void updateDialogs() {
		updateDialog(AType.PeakList, -1);
		updateDialog(AType.Measurements, -1);
	}

	private void setCurrentSubSpectrum(int i) {
		JDXSpectrum spec0 = getSpectrumAt(0);
		i = spec0.setCurrentSubSpectrum(i);
		if (spec0.isForcedSubset())
			viewData.setXRangeForSubSpectrum(getSpectrum().getXYCoords());
		pd.notifySubSpectrumChange(i, getSpectrum());
	}

	private void addCurrentZoom() {
		// add to and clean the zoom list
		if (viewList.size() > currentZoomIndex + 1)
			for (int i = viewList.size() - 1; i > currentZoomIndex; i--)
				viewList.remove(i);
		viewList.add(viewData);
		currentZoomIndex++;
	}

	private void setZoomTo(int i) {
		imageView = null;
		currentZoomIndex = i;
		viewData = viewList.get(i);
		resetPinsFromView();
		//pd.refresh();
	}

	/**
	 * Clears all views in the zoom list
	 */
	private void clearViews() {
		setZoomTo(0);
		// leave first zoom
		for (int i = viewList.size(); --i >= 1;)
			viewList.remove(i);
	}

	private void drawAll(Object g, int subIndex, int iSplit, boolean needNewPins) {
		if (imageView != null)
			draw2DImage(g);
		draw2DUnits(g, width, subIndex);

		int iSelected = (stackSelected || !showAllStacked ? iSpectrumSelected : -1);
		boolean doYScale = (!showAllStacked || nSpectra == 1 || iSelected >= 0);
		boolean doDraw1DObjects = (imageView == null || pd.display1D);
		int n = (iSelected >= 0 ? 1 : 0);
		int iSpectrumForScale = getFixedSelectedSpectrumIndex();
		if (doDraw1DObjects) {
			fillBox(g, xPixel0, yPixel0, xPixel1, yPixel1, ScriptToken.PLOTAREACOLOR);
			if (iSelected < 0) {
				doYScale = true;
				for (int i = 0; i < nSpectra; i++)
					if (doPlot(i, iSplit)) {
						if (n++ == 0)
							continue;
						if (doYScale && !viewData.areYScalesSame(i - 1, i))
							doYScale = false;
					}
			}
		}
		int iSpecForFrame = (nSpectra == 1 ? 0 : !showAllStacked ? iSpectrumMovedTo
				: iSpectrumSelected);
		boolean addCurrentBox = ((fracY != 1 || isSplittable) && (!isSplittable
				|| nSplit == 1 || pd.currentSplitPoint == iSplit));
		boolean drawUpDownArrows = (pd.isCurrentGraphSet(this) && zoomEnabled
				&& spectra.get(0).isScalable() && (addCurrentBox || nSpectra == 1)
				&& (nSplit == 1 || pd.currentSplitPoint == iSpectrumMovedTo) && !isDrawNoSpectra());
		boolean addSplitBox = isSplittable;
		drawFrame(g, iSpecForFrame, addCurrentBox, addSplitBox,
				drawUpDownArrows);
		if (pd.isCurrentGraphSet(this) // is current set
				&& iSplit == pd.currentSplitPoint && (n < 2 // just one spectrum to show
				|| iSpectrumSelected >= 0 // stacked and selected
				))
			haveSelectedSpectrum = true;
		haveSingleYScale = (showAllStacked && nSpectra > 1 ? allowStackedYScale
				&& doYScale : true);
		if (doDraw1DObjects) {
			int yOffsetPixels = (int) (yPixels * (yStackOffsetPercent / 100f));
			haveLeftRightArrows = false;
			for (int i = 0, offset = 0; i < nSpectra; i++)
				if (doPlot(i, iSplit)) {
					boolean isGrey = (stackSelected && iSpectrumSelected >= 0 && iSpectrumSelected != i);
					IntegralData ig = (!reversePlot
							&& getShowAnnotation(AType.Integration, i)
							&& (!showAllStacked || iSpectrumSelected == i) ? (IntegralData) getDialog(
							AType.Integration, i).getData()
							: null);
					setUserYFactor(i);
					JDXSpectrum spec = spectra.get(i);
					if (nSplit > 1) {
						iSpectrumForScale = i;
					}
					if (nSplit == 1 || showAllStacked || iSpectrumSelected == iSplit)
						drawWidgets(g, subIndex, needNewPins, doDraw1DObjects);
					if (haveSingleYScale && i == iSpectrumForScale)
						drawGrid(g);
					if (haveSingleYScale && !isDrawNoSpectra() && i == iSpectrumForScale
							&& (nSpectra == 1 || iSpectrumSelected >= 0))
						drawHighlightsAndPeakTabs(g, i);
					if (n == 1 && iSpectrumSelected < 0 || iSpectrumSelected == i
							&& pd.isCurrentGraphSet(this)) {
						if (pd.titleOn && !pd.titleDrawn) {
							drawTitle(g, height, width, (pd.isPrinting ? spec.getTitle()
									: spec.getPeakTitle()));
							pd.titleDrawn = true;
						}
					}
					if (haveSingleYScale && i == iSpectrumForScale) {
						if (pd.getBoolean(ScriptToken.YSCALEON))
							drawYScale(g);
						if (pd.getBoolean(ScriptToken.YUNITSON))
							drawYUnits(g);
					}
					drawSpectrum(g, i, viewData.spectrumOffsets == null ? offset
							: viewData.spectrumOffsets[i], isGrey, ig);
					if ((nSplit > 1 ? i == iSpectrumMovedTo : i == iSpectrumForScale)
							&& !pd.isPrinting && xPixelMovedTo >= 0 && spec.isContinuous()) {
						drawSpectrumPointer(g, spec, ig);
					}
					if (nSpectra > 1 && nSplit == 1 && pd.isCurrentGraphSet(this)) {
						haveLeftRightArrows = true;
						if (!pd.isPrinting) {
							setUserYFactor(0);
							iSpecForFrame = (iSpectrumSelected);
							if (nSpectra != 2) {
								setPlotColor(g, (iSpecForFrame + nSpectra - 1) % nSpectra);
								fillArrow(g, ArrowType.LEFT, yPixel11 - 10, xPixel00 + 23, true);
								setCurrentBoxColor(g);
								fillArrow(g, ArrowType.LEFT, yPixel11 - 10, xPixel00 + 23,
										false);
							}
							if (iSpecForFrame >= 0) {
								setPlotColor(g, iSpecForFrame);
								fillCircle(g, xPixel00 + 32, yPixel11 - 10, true);
							}
							setCurrentBoxColor(g);
							fillCircle(g, xPixel00 + 32, yPixel11 - 10, false);
							setPlotColor(g, (iSpecForFrame + 1) % nSpectra);
							fillArrow(g, ArrowType.RIGHT, yPixel11 - 10, xPixel00 + 41, true);
							setCurrentBoxColor(g);
							fillArrow(g, ArrowType.RIGHT, yPixel11 - 10, xPixel00 + 41, false);
						}
					}
					offset -= yOffsetPixels;
				}
			if (pd.getBoolean(ScriptToken.XSCALEON))
				drawXScale(g);
			if (pd.getBoolean(ScriptToken.XUNITSON))
				drawXUnits(g);
		} else {
			drawWidgets(g, subIndex, needNewPins, doDraw1DObjects);
		}
		if (annotations != null)
			drawAnnotations(g, annotations, null);
	}

	private void hideAllDialogsExceptCurrent() {
		if (dialogs == null)
			return;
		boolean getInt = false;
		boolean getMeas = false;
		boolean getPeak = false;
		AnnotationData ad;

		for (Map.Entry<String, AnnotationData> e : dialogs.entrySet()) {
			ad = e.getValue();
			if (isVisible(ad)) {
				// ((AnnotationDialog) ad).setVisible(false);
				switch (ad.getAType()) {
				case Integration:
					getInt = true;
					break;
				case Measurements:
					getMeas = true;
					break;
				case PeakList:
					getPeak = true;
					break;
				}
			}
		}
		if (getInt)
			ad = pd.owner.showDialog(AType.Integration);
		if (getMeas)
			ad = pd.owner.showDialog(AType.Measurements);
		if (getPeak)
			ad = pd.owner.showDialog(AType.PeakList);

	}

	private void drawSpectrumPointer(Object g, JDXSpectrum spec,
			IntegralData ig) {
		setColor(g, ScriptToken.PEAKTABCOLOR);
		int iHandle = pd.integralShiftMode;
		if (ig != null) {
			if ((!pd.ctrlPressed || pd.isIntegralDrag)
					&& !isOnSpectrum(pd.mouseX, pd.mouseY, -1))
				ig = null;
			else if (iHandle == 0)
				iHandle = getShiftMode(pd.mouseX, pd.mouseY);
		}
		double y0 = yValueMovedTo;
		yValueMovedTo = (ig == null ? spec.getYValueAt(xValueMovedTo) : ig
				.getPercentYValueAt(xValueMovedTo));
		setCoordStr(xValueMovedTo, yValueMovedTo);
		if (iHandle != 0) {
			setPlotColor(g, 0);
			int x = (iHandle < 0 ? xPixelPlot1 : xPixelPlot0);
			int y = (iHandle < 0 ? yPixelPlot0 : yPixelPlot1);
			drawHandle(g, x, y, false);
			return;
		}
		if (ig != null)
			setStrokeBold(g, true);
		if (Double.isNaN(y0) || pendingMeasurement != null) {
			drawLine(g, xPixelMovedTo, yPixel0, xPixelMovedTo, yPixel1);
			if (xPixelMovedTo2 >= 0)
				drawLine(g, xPixelMovedTo2, yPixel0, xPixelMovedTo2, yPixel1);
			yValueMovedTo = Double.NaN;
		} else {
			int y = (ig == null ? toPixelY(yValueMovedTo)
					: toPixelYint(yValueMovedTo / 100));
			if (y == fixY(y))
				drawLine(g, xPixelMovedTo, y - 10, xPixelMovedTo, y + 10);
		}
		if (ig != null)
			setStrokeBold(g, false);
	}

	private void setUserYFactor(int i) {
		userYFactor = viewData.userYFactors[i];
		yRef = viewData.spectrumYRefs[i];
		viewData.setAxisScaling(i, xPixels, yPixels, spectra.get(i).isInverted());
	}

	private boolean doPlot(int i, int iSplit) {
		return (nSplit > 1 ? i == iSplit : showAllStacked
				|| iSpectrumSelected == -1 || iSpectrumSelected == i);
	}

	private void draw2DUnits(Object g, int width, int subIndex) {
		if (subIndex >= 0 && imageView != null) {
			setColor(g, ScriptToken.PLOTCOLOR);
			drawUnits(g, width, nucleusX, imageView.xPixel1 + 5, yPixel1, 1, 1.0);
			drawUnits(g, width, nucleusY, imageView.xPixel0 - 5, yPixel0, 1, 0);
		}
	}

	private void drawPeakTabs(Object g, JDXSpectrum spec) {
		List<PeakInfo> list = (nSpectra == 1 || iSpectrumSelected >= 0 ? spec
				.getPeakList() : null);
		if (list != null && list.size() > 0) {
			if (piMouseOver != null && piMouseOver.spectrum == spec && pd.isMouseUp()) {
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
	 * @param needNewPins 
	 * @param doDraw1DObjects
	 */
	private void drawWidgets(Object g, int subIndex, boolean needNewPins,
			boolean doDraw1DObjects) {
		if (pd.isPrinting)
			return;
		// top/side slider bar backgrounds
		setWidgets(needNewPins, subIndex, doDraw1DObjects);
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
			if (pw == null || !pw.isPinOrCursor && !zoomEnabled)
				continue;
			if (pw.is2D) {
				if (pw == cur2Dx0 && !doDraw1DObjects)// || pw.is2Donly &&
					// doDraw1DObjects)
					continue;
			} else if (doDraw1DObjects == (pw == pin1Dy0 || pw == pin1Dy1 || pw == pin1Dy01)
					|| pw == zoomBox1D
					&& (pd.isIntegralDrag || pd.integralShiftMode != 0)) {
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
	 *          units
	 * @param endX
	 *          units
	 * @param whatColor 
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
	 *          the <code>Graphics</code> object
	 * @param index
	 *          the index of the Spectrum to draw
	 * @param yOffset 
	 * @param isGrey 
	 * @param ig 
	 * @param height
	 *          the height to be drawn in pixels
	 * @param width
	 *          the width to be drawn in pixels
	 */
	private void drawSpectrum(Object g, int index, int yOffset, boolean isGrey,
			IntegralData ig) {
		// Check if specInfo in null or xyCoords is null
		JDXSpectrum spec = spectra.get(index);
		drawPlot(g, index, spec.getXYCoords(), spec.isContinuous(), yOffset,
				isGrey, null);
		if (ig != null) {
			if (haveIntegralDisplayed(index))
				drawPlot(g, index, getIntegrationGraph(index).getXYCoords(), true,
						yOffset, false, ig);
			drawIntegralValues(g, index, yOffset);
		}
		if (getIntegrationRatios(index) != null)
			drawAnnotations(g, getIntegrationRatios(index),
					ScriptToken.INTEGRALPLOTCOLOR);
		if (pendingMeasurement != null && pendingMeasurement.spec == spec)
			drawMeasurement(g, pendingMeasurement);
		drawMeasurements(g, index);

	}

	private MeasurementData getMeasurements(AType type, int iSpec) {
		AnnotationData ad = getDialog(type, iSpec);
		return (ad == null || ad.getData().size() == 0 || !ad.getState() ? null : ad.getData());
	}

	private void drawPlot(Object g, int index, Coordinate[] xyCoords,
			boolean isContinuous, int yOffset, boolean isGrey, IntegralData ig) {
		boolean isIntegral = (ig != null);
		// if (isIntegral)
		// return;
		BitSet bsDraw = (ig == null ? null : ig.getBitSet());
		boolean fillPeaks = (!isIntegral && !isGrey && !isIntegral
				&& pendingIntegral != null && pendingIntegral.spec == spectra
				.get(index));
		// boolean drawAll = (ig == null || !ig.getParameters().integralDrawAll);
		int iColor = (isGrey ? -2 : isIntegral ? -1 : !allowStacking ? 0 : index);
		setPlotColor(g, iColor);
		boolean plotOn = true;
		int y0 = toPixelY(0);
		fillPeaks &= (y0 == fixY(y0));
		int iFirst = viewData.startDataPointIndices[index];
		int iLast = viewData.endDataPointIndices[index];
		if (isContinuous) {
			iLast--;
			for (int i = iFirst; i <= iLast; i++) {
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
				y1 = fixY(yOffset + y1);
				y2 = fixY(yOffset + y2);
				if (isIntegral) {
					if (i == iFirst) {
						xPixelPlot1 = x1;
						yPixelPlot0 = y1;
					}
					xPixelPlot0 = x2;
					yPixelPlot1 = y2;
				}
				if (x2 == x1 && y1 == y2)
					continue;
				if (fillPeaks
						&& pendingIntegral.overlaps(point1.getXVal(), point2.getXVal())) {
					setColor(g, ScriptToken.INTEGRALPLOTCOLOR);
					drawLine(g, x1, y0, x1, y1);
					setPlotColor(g, iColor);
					continue;
				}
				if (y1 == y2 && (y1 == yPixel0 || y1 == yPixel1))
					continue;
				if (bsDraw != null && bsDraw.get(i) != plotOn) {
					plotOn = bsDraw.get(i);
					if (!pd.isPrinting && pd.integralShiftMode != 0)
						setPlotColor(g, 0);
					else if (plotOn)
						setColor(g, ScriptToken.INTEGRALPLOTCOLOR);
					else
						setPlotColor(g, -3);
				}
				if (pd.isPrinting && !plotOn)
					continue;
				drawLine(g, x1, y1, x2, y2);
			}
		} else {
			for (int i = iFirst; i <= iLast; i++) {
				Coordinate point = xyCoords[i];
				int y2 = toPixelY(point.getYVal());
				if (y2 == Integer.MIN_VALUE)
					continue;
				int x1 = toPixelX(point.getXVal());
				int y1 = toPixelY(Math.max(viewData.minYOnScale, 0));
				y1 = fixY(yOffset + y1);
				y2 = fixY(yOffset + y2);
				if (y1 == y2 && (y1 == yPixel0 || y1 == yPixel1))
					continue;
				drawLine(g, x1, y1, x1, y2);
			}
			if (viewData.isYZeroOnScale()) {
				int y = yOffset + toPixelY(0);
				if (y == fixY(y))
					drawLine(g, xPixel1, y, xPixel0, y);
			}
		}
	}

	/**
	 * 
	 * @param g
	 * @param iSpec 
	 * @param addCurrentBox 
	 * @param addSplitBox 
	 * @param drawUpDownArrows 
	 * @param height
	 * @param width
	 */
	private void drawFrame(Object g, int iSpec,
			boolean addCurrentBox, boolean addSplitBox, boolean drawUpDownArrows) {
		if (!pd.gridOn || pd.isPrinting) {
			setColor(g, ScriptToken.GRIDCOLOR);
			drawRect(g, xPixel0, yPixel0, xPixels, yPixels);
			if (pd.isPrinting)
				return;
		}
		setCurrentBoxColor(g);
		if (drawUpDownArrows) {
			if (iSpec >= 0) {
				setPlotColor(g, iSpec);
				fillArrow(g, ArrowType.UP, xPixel11 - 25,
						(yPixel00 + yPixel11) / 2 - 9, true);
				fillArrow(g, ArrowType.DOWN, xPixel11 - 25,
						(yPixel00 + yPixel11) / 2 + 9, true);
				setCurrentBoxColor(g);
			}
			fillArrow(g, ArrowType.UP, xPixel11 - 25, (yPixel00 + yPixel11) / 2 - 9,
					false);
			fillCircle(g, xPixel11 - 25, (yPixel00 + yPixel11) / 2, false);
			fillArrow(g, ArrowType.DOWN, xPixel11 - 25,
					(yPixel00 + yPixel11) / 2 + 9, false);
		}

		if (imageView != null)
			return;
		if (addCurrentBox) {
			drawRect(g, xPixel00 + 10, yPixel00 + 1, xPixel11 - 20 - xPixel00,
					yPixel11 - 2 - yPixel00);
			if (addSplitBox) {
				fillBox(g, xPixel11 - 20, yPixel00 + 1, xPixel11 - 10, yPixel00 + 11,
						null);
			}
		}
	}

	/**
	 * Draws the grid on the Panel
	 * 
	 * @param g
	 *          the <code>Graphics</code> object
	 * @param height
	 *          the height to be drawn in pixels
	 * @param width
	 *          the width to be drawn in pixels
	 */
	private void drawGrid(Object g) {
		if (!pd.gridOn || imageView != null)
			return;
		setColor(g, ScriptToken.GRIDCOLOR);
		double lastX;
		if (Double.isNaN(viewData.firstX)) {
			lastX = viewData.maxXOnScale + viewData.steps[0] / 2;
			for (double val = viewData.minXOnScale; val < lastX; val += viewData.steps[0]) {
				int x = toPixelX(val);
				drawLine(g, x, yPixel0, x, yPixel1);
			}
		} else {
			lastX = viewData.maxXOnScale * 1.0001;
			for (double val = viewData.firstX; val <= lastX; val += viewData.steps[0]) {
				int x = toPixelX(val);
				drawLine(g, x, yPixel0, x, yPixel1);
			}
		}
		for (double val = viewData.firstY; val < viewData.maxYOnScale
				+ viewData.steps[1] / 2; val += viewData.steps[1]) {
			int y = toPixelY(val);
			if (y == fixY(y))
				drawLine(g, xPixel0, y, xPixel1, y);
		}
	}

	Map<Double, String> mapX = new Hashtable<Double, String>();

	/**
	 * Draws the x Scale
	 * 
	 * @param g
	 *          the <code>Graphics</code> object
	 * @param height
	 *          the height to be drawn in pixels
	 * @param width
	 *          the width to be drawn in pixels
	 */
	private void drawXScale(Object g) {

		DecimalFormat formatter = viewData.formatters[0];
		pd.setFont(g, width, FONT_PLAIN, 12, false);
		int y1 = yPixel1;
		int y2 = yPixel1 + 4;
		int y3 = yPixel1 + 2;

		int h = getFontHeight(g);
		double maxWidth = Math.abs((toPixelX(viewData.steps[0]) - toPixelX(0)) * 0.95);
		// if (Double.isNaN(viewData.firstX)) {
		// lastX = viewData.maxXOnScale + viewData.steps[0] / 2;
		// for (double val = viewData.minXOnScale, vald = viewData.maxXOnScale; val
		// < lastX; val += viewData.steps[0], vald -= viewData.steps[0]) {
		// int x = (int) (xPixel0 + (((drawXAxisLeftToRight ? val : vald) -
		// viewData.minXOnScale) / viewData.xFactorForScale));
		// setColor(g, ScriptToken.SCALECOLOR);
		// drawLine(g, x, y1, x, y2);
		// setColor(g, ScriptToken.SCALECOLOR);
		// String s = formatter.format(val);
		// int w = getStringWidth(g, s);
		// drawString(g, s, x - w / 2, y2 + h);
		// }
		// } else {

		// we go overboard for ticks
		double firstX = viewData.firstX - viewData.steps[0];
		double lastX = (viewData.maxXOnScale + viewData.steps[0]) * 1.0001;
		setColor(g, ScriptToken.SCALECOLOR);
		for (int pass = 0; pass < 2; pass++) {
			if (pass == 1)
				ScaleData.fixScale(mapX);
			double prevX = 1e10;
			for (double val = firstX; val <= lastX; val += viewData.steps[0]) {
				int x = toPixelX(val);
				Double d = Double.valueOf(val);
				String s;
				switch (pass) {
				case 0:
					s = formatter.format(val);
					mapX.put(d, s);
					drawTick(g, x, y1, y2);
					double dx = Math.abs(prevX - val);
					int ntick = viewData.minorTickCounts[0];
					if (ntick != 0) {
						double step = dx / ntick;
						for (int i = 1; i < ntick; i++)
							drawTick(g, toPixelX(val - i * step), y1, y3);
					}
					prevX = val;
					continue;
				case 1:
					if (x != fixX(x))
						continue;
					s = mapX.get(d);
					int w = getStringWidth(g, s);
					int n = (x + w / 2 == fixX(x + w / 2) ? 2 : 0);
					if (n > 0)
						drawString(g, s, x - w / n, y2 + h);
					val += Math.floor(w / maxWidth) * viewData.steps[0];
					break;
				}
			}
		}
		mapX.clear();
	}

	private void drawTick(Object g, int x, int y1, int y2) {
		if (x == fixX(x))
			drawLine(g, x, y1, x, y2);
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

		DecimalFormat formatter = viewData.formatters[1];
		pd.setFont(g, width, FONT_PLAIN, 12, false);
		int h = getFontHeight(g);
		double max = viewData.maxYOnScale + viewData.steps[1] / 2;
		int yLast = Integer.MIN_VALUE;
		setColor(g, ScriptToken.SCALECOLOR);
		for (int pass = 0; pass < 2; pass++) {
			if (pass == 1)
				ScaleData.fixScale(mapX);
			for (double val = viewData.firstY; val < max; val += viewData.steps[1]) {
				Double d = Double.valueOf(val);
				int x1 = xPixel0;
				int y = toPixelY(val);
				if (y != fixY(y))
					continue;
				String s;
				if (pass == 0)
					drawLine(g, x1, y, x1 - 3, y);
				if (Math.abs(y - yLast) <= h)
					continue;
				yLast = y;
				switch (pass) {
				case 0:
					s = formatter.format(val);
					mapX.put(d, s);
					break;
				case 1:
					s = mapX.get(d);
					if (s.startsWith("0") && s.contains("E"))
						s = "0";
					drawString(g, s, (x1 - 4 - getStringWidth(g, s)), y + h / 3);
					break;
				}
			}
		}
		mapX.clear();
	}

	/**
	 * Draws the X Units
	 * 
	 * @param g
	 *          the <code>Graphics</code> object
	 * @param imageHeight
	 *          the height to be drawn in pixels
	 * @param width
	 *          the width to be drawn in pixels
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
	 *          the <code>Graphics</code> object
	 * @param imageHeight
	 *          the height to be drawn in pixels
	 */
	private void drawYUnits(Object g) {
		String units = spectra.get(0).getAxisLabel(false);
		if (units != null)
			drawUnits(g, width, units, 5, yPixel0, 0, -1);
	}

	private void drawHighlightsAndPeakTabs(Object g, int iSpec) {
		MeasurementData md = getMeasurements(AType.PeakList, iSpec);
		JDXSpectrum spec = spectra.get(iSpec);
		if (pd.isPrinting) {
			if (md != null) {
				setColor(g, ScriptToken.PEAKTABCOLOR);
				printPeakList(g, spec, (PeakData) md);
			}
			return;
		}
		if (md == null) {
			for (int i = 0; i < highlights.size(); i++) {
				Highlight hl = highlights.get(i);
				if (hl.spectrum == spec) {
					pd.setHighlightColor(hl.color);
					drawBar(g, hl.x1, hl.x2, ScriptToken.HIGHLIGHTCOLOR, true);
				}
			}
			drawPeakTabs(g, spec);
		}
		int y;
		if (md != null) {
			y = (spec.isInverted() ? yPixel1 - 10 : yPixel0);
			setColor(g, ScriptToken.PEAKTABCOLOR);
			for (int i = md.size(); --i >= 0;) {
				Measurement m = md.get(i);
				int x = toPixelX(m.getXVal());
				drawLine(g, x, y, x, y + 10);
			}
			if (isVisible(getDialog(AType.PeakList, iSpec))) {
				y = toPixelY(((PeakData) md).getThresh());
				if (y == fixY(y) && !pd.isPrinting)
					drawLine(g, xPixel0, y, xPixel1, y);
			}
		}
	}

	private void printPeakList(Object g, JDXSpectrum spec, PeakData data) {
		String[][] sdata = data.getMeasurementListArray(null);
		if (sdata.length == 0)
			return;
		pd.setFont(g, width, FONT_PLAIN, 8, false);
		int h = getFontHeight(g);
		int[] xs = new int[data.size()];
		int[] xs0 = new int[data.size()];
		int dx = 0;
		for (int i = 0; i < sdata.length; i++) {
			xs0[i] = toPixelX(Double.parseDouble(sdata[i][1]));
			if (i == 0) {
				xs[i] = xs0[i];
				continue;
			}
			xs[i] = Math.max(xs[i - 1] + h, xs0[i] + h);
			dx += (xs[i] - xs0[i]);
		}
		dx /= 2 * sdata.length;
		if (xs[0] - dx < xPixel0 + 25)
			dx = xs[0] - (xPixel0 + 25);
		for (int i = 0; i < sdata.length; i++)
			xs[i] -= dx;

		boolean inverted = spec.isInverted();
		int y4 = getStringWidth(g, "99.9999");
		int y2 = (sdata[0].length >= 6 ? getStringWidth(g, "99.99") : 0);
		int f = (inverted ? -1 : 1);
		
		int y = (inverted ? yPixel1 : yPixel0) + f * (y2 + y4 + 15);
		for (int i = 0; i < sdata.length; i++) {
			drawLine(g, xs[i], y, xs[i], y + 5 * f);
			drawLine(g, xs[i], y + 5 * f, xs0[i], y + 10 * f);
			drawLine(g, xs0[i], y + 10 * f, xs0[i], y + 15 * f);
			if (y2 > 0 && sdata[i][4].length() > 0)
				drawLine(g, (xs[i] + xs[i - 1]) / 2, y - y4 + 5,
						(xs[i] + xs[i - 1]) / 2, y - y4 - 5);
		}

		y -= f * 2;

		if (y2 > 0) {
  		drawStringRotated(g, 90, xs[0] - 15, y, "  ppm");
	  	drawStringRotated(g, 90, xs[0] - 15, y - y4 - 5, " Hz");
		}
		for (int i = data.size(); --i >= 0;) {
			drawStringRotated(g, 90 * f, xs[i] + f * h / 3, y, sdata[i][1]);
			if (y2 > 0 && sdata[i][4].length() > 0) {
				int x = (xs[i] + xs[i - 1]) / 2 + h / 3;
				drawStringRotated(g, 90, x, y - y4 - 5, sdata[i][4]);
			}
		}

		// TODO Auto-generated method stub

	}

	private void drawStringRotated(Object g, int angle, int x, int y, String s) {
		rotatePlot(g, -angle, x, y);
		drawString(g, s, x, y);
		rotatePlot(g, angle, x, y);
	}

	// determine whether there are any ratio annotations to draw
	private void drawAnnotations(Object g, List<Annotation> annotations,
			ScriptToken whatColor) {
		pd.setFont(g, width, FONT_BOLD, 12, false);
		for (int i = annotations.size(); --i >= 0;) {
			Annotation note = annotations.get(i);
			setAnnotationColor(g, note, whatColor);
			int x = (note.is2D ? imageView.toPixelX(note.getXVal()) : toPixelX(note
					.getXVal()));
			int y = (note.isPixels() ? (int) (yPixel0 + 10 - note.getYVal())
					: note.is2D ? imageView.toPixelY((int) note.getYVal())
							: toPixelY(note.getYVal()));
			drawString(g, note.getText(), x + note.offsetX, y - note.offsetY);
		}
	}

	private void drawIntegralValues(Object g, int iSpec, int yOffset) {
		MeasurementData integrals = getMeasurements(AType.Integration, iSpec);
		if (integrals != null) {
			if (pd.isPrinting)
				pd.setFont(g, width, FONT_PLAIN, 8, false);
			else
				pd.setFont(g, width, FONT_BOLD, 12, false);
			setColor(g, ScriptToken.INTEGRALPLOTCOLOR);
			int h = getFontHeight(g);
			setStrokeBold(g, true);
			for (int i = integrals.size(); --i >= 0;) {
				Measurement in = integrals.get(i);
				if (in.getValue() == 0)
					continue;
				int x = toPixelX(in.getXVal2());
				int y1 = yOffset + toPixelYint(in.getYVal());
				int y2 = yOffset + toPixelYint(in.getYVal2());
				if (x != fixX(x) || y1 != fixY(y1) || y2 != fixY(y2))
					continue;

				if (!pd.isPrinting)
					drawLine(g, x, y1, x, y2);
				String s = "  " + in.getText();
				drawString(g, s, x, (y1 + y2) / 2 + h / 3);
			}
			setStrokeBold(g, false);
		}
		if (iSpec == getFixedSelectedSpectrumIndex())
			selectedSpectrumIntegrals = integrals;
	}

	private void drawMeasurements(Object g, int iSpec) {
		MeasurementData md = getMeasurements(AType.Measurements, iSpec);
		if (md != null)
			for (int i = md.size(); --i >= 0;)
				drawMeasurement(g, md.get(i));
		if (iSpec == getFixedSelectedSpectrumIndex())
			selectedSpectrumMeasurements = md;
	}

	private void drawMeasurement(Object g, Measurement m) {
		if (m.text.length() == 0 && m != pendingMeasurement)
			return;
		pd.setFont(g, width, FONT_BOLD, 12, false);
		setColor(g, 0, 0, 0); // black
		int x1 = toPixelX(m.getXVal());
		int y1 = toPixelY(m.getYVal());
		int x2 = toPixelX(m.getXVal2());
		if (x1 != fixX(x1) || x2 != fixX(x2))
			return;
		boolean drawString = (Math.abs((m.getXVal() - m.getXVal2())
				/ viewData.xFactorForScale) >= 2);
		boolean drawBaseLine = viewData.isYZeroOnScale() && m.spec.isHNMR();
		int x = (x1 + x2) / 2;
		setStrokeBold(g, true);
		if (drawString)
			drawLine(g, x1, y1, x2, y1);
		if (drawBaseLine)
			drawLine(g, x1 + 1, yPixel1 - 1, x2, yPixel1 - 1);
		setStrokeBold(g, false);
		if (drawString)
			drawString(g, m.getText(), x + m.offsetX, y1 - m.offsetY);
		if (drawBaseLine) {
			drawLine(g, x1, yPixel1, x1, yPixel1 - 6);
			drawLine(g, x2, yPixel1, x2, yPixel1 - 6);
		}
	}

	private PlotWidget getPinSelected(int xPixel, int yPixel) {
		if (widgets != null)
			for (int i = 0; i < widgets.length; i++) {
				if (widgets[i] != null && widgets[i].isPinOrCursor
						&& widgets[i].selected(xPixel, yPixel)) {
					return widgets[i];
				}
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
			imageView.setView0(imageView.xPixel0, pin2Dy0.yPixel0, imageView.xPixel1,
					pin2Dy1.yPixel0);
			doZoom(imageView.minX, viewData.minY, imageView.maxX, viewData.maxY,
					true, false, true);
		} else {
			imageView.setView0(pin2Dx0.xPixel0, imageView.yPixel0, pin2Dx1.xPixel0,
					imageView.yPixel1);
			// pd.repaint();
		}
	}

	private boolean setAnnotationText(Annotation a) {
		String sval = pd.getInput("New text?", "Set Label", a.getText());
		if (sval == null)
			return false;
		if (sval.length() == 0)
			annotations.remove(a);
		else
			a.setText(sval);
		return true;
	}

	private void checkIntegral(double x1, double x2, boolean isFinal) {
		AnnotationData ad = getDialog(AType.Integration, -1);
		if (ad == null)
			return;
		Integral integral = ((IntegralData) ad.getData()).addIntegralRegion(x1, x2);
		if (isFinal && ad instanceof AnnotationDialog)
			((AnnotationDialog) ad).update(null);
		selectedSpectrumIntegrals = null;
		pendingIntegral = (isFinal ? null : integral);
	}

	private void setToolTipForPixels(int xPixel, int yPixel) {
		PlotWidget pw = getPinSelected(xPixel, yPixel);
		NumberFormat formatterX = viewData.formatters[0];
		NumberFormat formatterY = viewData.formatters[1];
		if (pw != null) {
			if (setStartupPinTip())
				return;
			String s;
			if (pw == pin1Dx01 || pw == pin2Dx01) {
				s = formatterX .format(Math.min(pin1Dx0.getXVal(), pin1Dx1.getXVal()))
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

		double yPt;
		if (imageView != null) {
			if (imageView.fixX(xPixel) == xPixel && fixY(yPixel) == yPixel) {

				int isub = imageView.toSubspectrumIndex(yPixel);
				String s = formatterX.format(imageView.toX(xPixel)) + " "
						+ getSpectrum().getAxisLabel(true) + ",  "
						+ get2DYLabel(isub, formatterX);
				pd.setToolTipText(pd.display1D ? s : "");
				pd.coordStr = s;
				return;
			}
			if (!pd.display1D) {
				pd.setToolTipText("");
				pd.coordStr = "";
				return;
			}
		}
		double xPt = toX(fixX(xPixel));
		yPt = (imageView != null && imageView.isXWithinRange(xPixel) ? imageView
				.toSubspectrumIndex(fixY(yPixel)) : toY(fixY(yPixel)));
		String xx = setCoordStr(xPt, yPt);
		int iSpec = getFixedSelectedSpectrumIndex();
		if (!isInPlotRegion(xPixel, yPixel)) {
			yPt = Double.NaN;
		} else if (nSpectra == 1) {
			// I have no idea what I was thinking here...
			// if (!getSpectrum().isHNMR()) {
			// yPt = spectra[0].getPercentYValueAt(xPt);
			// xx += ", " + formatterY.format(yPt);
			// }
		} else if (haveIntegralDisplayed(iSpec)) {
			yPt = getIntegrationGraph(iSpec).getPercentYValueAt(xPt);
			xx += ", " + TextFormat.getDecimalFormat("#0.0").format(yPt);
		}
		pd.setToolTipText(
						(pendingMeasurement != null || selectedMeasurement != null || selectedIntegral != null ? 
								(pd.hasFocus() ? "Press ESC to delete "	
										+ (selectedIntegral != null ? "integral, DEL to delete all visible, or N to normalize" 
													: pendingMeasurement == null ? "\"" + selectedMeasurement.text + "\" or DEL to delete all visible" 
															: "measurement")
						: "")
						: Double.isNaN(yPt) ? null : xx)

				// + " :" + iSpectrumSelected + " :" + iSpectrumMovedTo

				);
	}

	private String setCoordStr(double xPt, double yPt) {
		String xx = viewData.formatters[0].format(xPt);
		pd.coordStr = "("
				+ xx
				+ (haveSingleYScale || iSpectrumSelected >= 0 ? ", "
						+ viewData.formatters[1].format(yPt) : "") + ")";
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

	private boolean isOnSpectrum(int xPixel, int yPixel, int index) {
		Coordinate[] xyCoords = null;
		boolean isContinuous = true;
		boolean isIntegral = (index < 0);

		// ONLY getSpectrumAt(0).is1D();
		if (isIntegral) {
			AnnotationData ad = getDialog(AType.Integration, -1);
			if (ad == null)
				return false;
			xyCoords = ((IntegralData) ad.getData()).getXYCoords();
			index = getFixedSelectedSpectrumIndex();
		} else {
			setUserYFactor(index);
			JDXSpectrum spec = spectra.get(index);
			xyCoords = spec.xyCoords;
			isContinuous = spec.isContinuous();
		}
		int yOffset = (viewData.spectrumOffsets == null ? index
				* (int) (yPixels * (yStackOffsetPercent / 100f))
				: viewData.spectrumOffsets[index]);

		if (isContinuous) {
			for (int i = viewData.startDataPointIndices[index]; i < viewData.endDataPointIndices[index]; i++) {
				Coordinate point1 = xyCoords[i];
				Coordinate point2 = xyCoords[i + 1];
				int x1 = toPixelX(point1.getXVal());
				int x2 = toPixelX(point2.getXVal());
				int y1 = (isIntegral ? toPixelYint(point1.getYVal()) : toPixelY(point1
						.getYVal()));
				int y2 = (isIntegral ? toPixelYint(point2.getYVal()) : toPixelY(point2
						.getYVal()));
				if (y1 == Integer.MIN_VALUE || y2 == Integer.MIN_VALUE)
					continue;
				y1 = yOffset + fixY(y1);
				y2 = yOffset + fixY(y2);
				if (isOnLine(xPixel, yPixel, x1, y1, x2, y2))
					return true;
			}
		} else {
			for (int i = viewData.startDataPointIndices[index]; i <= viewData.endDataPointIndices[index]; i++) {
				Coordinate point = xyCoords[i];
				int y2 = toPixelY(point.getYVal());
				if (y2 == Integer.MIN_VALUE)
					continue;
				int x1 = toPixelX(point.getXVal());
				int y1 = toPixelY(Math.max(viewData.minYOnScale, 0));
				y1 = fixY(y1);
				y2 = fixY(y2);
				if (y1 == y2 && (y1 == yPixel0 || y1 == yPixel1))
					continue;
				if (isOnLine(xPixel, yPixel, x1, y1, x1, y2))
					return true;
			}
		}
		return false;
	}

	// static methods

	private static double distance(int dx, int dy) {
		return Math.sqrt(dx * dx + dy * dy);
	}

	private static GraphSet findCompatibleGraphSet(List<GraphSet> graphSets,
			JDXSpectrum spec) {
		for (int i = 0; i < graphSets.size(); i++)
			if (JDXSpectrum.areScalesCompatible(spec, graphSets.get(i).getSpectrum(),
					false))
				return graphSets.get(i);
		return null;
	}

	private static boolean isGoodEvent(PlotWidget zOrP, PlotWidget p, boolean asX) {
		return (p == null ? (Math.abs(zOrP.xPixel1 - zOrP.xPixel0) > MIN_DRAG_PIXELS && Math
				.abs(zOrP.yPixel1 - zOrP.yPixel0) > MIN_DRAG_PIXELS)
				: asX ? Math.abs(zOrP.xPixel0 - p.xPixel0) > MIN_DRAG_PIXELS : Math
						.abs(zOrP.yPixel0 - p.yPixel0) > MIN_DRAG_PIXELS);
	}

	private final static int ONLINE_CUTOFF = 2;

	private static boolean isOnLine(int xPixel, int yPixel, int x1, int y1,
			int x2, int y2) {
		// near a point
		int dx1 = Math.abs(x1 - xPixel);
		if (dx1 < ONLINE_CUTOFF && Math.abs(y1 - yPixel) < ONLINE_CUTOFF)
			return true;
		int dx2 = x2 - xPixel;
		if (Math.abs(dx2) < ONLINE_CUTOFF && Math.abs(y2 - yPixel) < ONLINE_CUTOFF)
			return true;
		// between points
		int dy12 = y1 - y2;
		if (Math.abs(dy12) > ONLINE_CUTOFF && (y1 < yPixel) == (y2 < yPixel))
			return false;
		int dx12 = x1 - x2;
		if (Math.abs(dx12) > ONLINE_CUTOFF && (x1 < xPixel) == (x2 < xPixel))
			return false;
		return (distance(dx1, y1 - yPixel) + distance(dx2, yPixel - y2) < distance(
				dx12, dy12)
				+ ONLINE_CUTOFF);
	}

	private static void setFractionalPositions(List<GraphSet> graphSets) {
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

	// highlight class

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
		 *          the object that this <code>Highlight<code> is compared to
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

	// called only by PanelData

	String addAnnotation(List<String> args, String title) {
		if (args.size() == 0 || args.size() == 1
				&& args.get(0).equalsIgnoreCase("none")) {
			annotations = null;
			lastAnnotation = null;
			return null;
		}
		if (args.size() < 4 && lastAnnotation == null)
			lastAnnotation = getAnnotation(
					(viewData.maxXOnScale + viewData.minXOnScale) / 2,
					(viewData.maxYOnScale + viewData.minYOnScale) / 2, title, false,
					false, 0, 0);
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

	/**
	 * Add information about a region of the displayed spectrum to be highlighted
	 * 
	 * @param x1
	 *          the x value of the coordinate where the highlight should start
	 * @param x2
	 *          the x value of the coordinate where the highlight should end
	 * @param spec
	 * @param oColor 
	 *          the color of the highlight
	 */
	void addHighlight(double x1, double x2, JDXSpectrum spec, Object oColor) {
		if (spec == null)
			spec = getSpectrumAt(0);
		Highlight hl = new Highlight(x1, x2, spec, (oColor == null ? pd
				.getHighlightColor() : oColor));
		if (!highlights.contains(hl))
			highlights.add(hl);
	}

	void addPeakHighlight(PeakInfo peakInfo) {
		for (int i = spectra.size(); --i >= 0;) {
			JDXSpectrum spec = spectra.get(i);
			removeAllHighlights(spec);
			if (peakInfo == null || peakInfo.isClearAll()
					|| spec != peakInfo.spectrum)
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
			pd.addHighlight(this, x1, x2, spec, 200, 200, 200, 200);
			if (viewData.isInRangeX(x1)
					|| viewData.isInRangeX(x2) || x1 < viewData.minX
					&& viewData.maxX < x2) {
				// pd.repaint();
			} else {
				setZoomTo(0);
			}
		}
	}

	void advanceSubSpectrum(int dir) {
		JDXSpectrum spec0 = getSpectrumAt(0);
		int i = spec0.advanceSubSpectrum(dir);
		if (spec0.isForcedSubset())
			viewData.setXRangeForSubSpectrum(getSpectrum().getXYCoords());
		pd.notifySubSpectrumChange(i, getSpectrum());
	}

	boolean checkSpectrumClickedEvent(int xPixel, int yPixel, int clickCount) {
		if (nextClickForSetPeak)
			return false;
		if (clickCount > 0 && checkArrowLeftRightClick(xPixel, yPixel))
			return true;
		if (clickCount > 1 || pendingMeasurement != null
				|| !isInPlotRegion(xPixel, yPixel))
			return false;
		// in the plot area

		if (clickCount == 0) {
			// pressed

			boolean isOnIntegral = isOnSpectrum(xPixel, yPixel, -1);
			pd.integralShiftMode = (isOnIntegral ? getShiftMode(xPixel, yPixel) : 0);
			pd.isIntegralDrag = (pd.integralShiftMode == 0 && (isOnIntegral || haveIntegralDisplayed(-1)
					&& findMeasurement(getIntegrationGraph(-1), xPixel, yPixel, 0) != null));
			if (pd.integralShiftMode != 0)
				// pd.repaint();
				return false;
		}

		if (!showAllStacked)
			return false;
		// in the stacked plot area

		stackSelected = false;
		for (int i = 0; i < nSpectra; i++) {
			if (!isOnSpectrum(xPixel, yPixel, i))
				continue;
			boolean isNew = (i != iSpectrumSelected);
			setSpectrumClicked(iPreviousSpectrumClicked = i);
			return isNew;
		}
		// but not on a spectrum
		if (isDialogOpen())
			return false;
		setSpectrumClicked(-1);
		return stackSelected = false;
	}

	private int getShiftMode(int xPixel, int yPixel) {
		return (isStartEndIntegral(xPixel, false) ? yPixel : isStartEndIntegral(
				xPixel, true) ? -yPixel : 0);
	}

	private boolean isDialogOpen() {
		return (isVisible(getDialog(AType.Integration, -1))
				|| isVisible(getDialog(AType.Measurements, -1)) || isVisible(getDialog(
				AType.PeakList, -1)));
	}

	private boolean isStartEndIntegral(int xPixel, boolean isEnd) {
		return (isEnd ? xPixelPlot1 - xPixel < 20 : xPixel - xPixelPlot0 < 20);
	}

	boolean checkWidgetEvent(int xPixel, int yPixel, boolean isPress) {
		if (!zoomEnabled)
			return false;
		PlotWidget widget;
		if (isPress) {
			widget = getPinSelected(xPixel, yPixel);
			if (widget == null) {
				yPixel = fixY(yPixel);
				if (xPixel < xPixel1) {
					xPixel = fixX(xPixel);
					zoomBox1D.setX(toX(xPixel), xPixel);
					zoomBox1D.yPixel0 = yPixel;
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
		nextClickForSetPeak = false;
		widget = pd.thisWidget;
		if (widget == null)
			return false;

		// mouse drag with widget
		if (widget == zoomBox1D) {
			zoomBox1D.xPixel1 = fixX(xPixel);
			zoomBox1D.yPixel1 = fixY(yPixel);
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
			// 2D x zoom change
			doZoom(cur2Dx0.getXVal(), viewData.minY, cur2Dx1.getXVal(),
					viewData.maxY, false, false, false);
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
			// 1D x zoom change
			doZoom(pin1Dx0.getXVal(), viewData.minY, pin1Dx1.getXVal(),
					viewData.maxY, false, false, false);
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
				if (Math.min(y0, y1) == viewData.minY
						|| Math.max(y0, y1) == viewData.maxY)
					return true;
				pin1Dy0.setY(y0, yPixel);
				pin1Dy1.setY(y1, yPixel1);
			}
			// actually only used for 2D intensity change now
			doZoom(viewData.minXOnScale, pin1Dy0.getYVal(), viewData.maxXOnScale,
					pin1Dy1.getYVal(), false, false, false);
			return true;
		}
		if (widget == pin2Dx0 || widget == pin2Dx1 || widget == pin2Dx01) {
			xPixel = imageView.fixX(xPixel);
			widget.setX(imageView.toX0(xPixel), xPixel);
			if (widget == pin2Dx01) {
				int dp = xPixel - (pin2Dx0.xPixel0 + pin2Dx1.xPixel0) / 2 + 1;
				xPixel = pin2Dx0.xPixel0 + dp;
				int xPixel1 = pin2Dx1.xPixel0 + dp;
				if (imageView.fixX(xPixel) != xPixel
						|| imageView.fixX(xPixel1) != xPixel1)
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
			// 2D x zoom
			doZoom(pin2Dx0.getXVal(), viewData.minY, pin2Dx1.getXVal(),
					viewData.maxY, false, false, false);
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

	void clearAllViews() {
		clearViews();
	}

	void clearIntegrals() {
		checkIntegral(Double.NaN, 0, false);
		// pd.repaint();
	}

	void clearMeasurements() {
		removeDialog(getFixedSelectedSpectrumIndex(), AType.Measurements);
	}

	static List<GraphSet> createGraphSets(JSVPanel jsvp,
			List<JDXSpectrum> spectra, int startIndex, int endIndex) {
		List<GraphSet> graphSets = new ArrayList<GraphSet>();
		for (int i = 0; i < spectra.size(); i++) {
			JDXSpectrum spec = spectra.get(i);
			GraphSet graphSet = findCompatibleGraphSet(graphSets, spec);
			if (graphSet == null)
				graphSets.add(graphSet = jsvp.getNewGraphSet());
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

	/**
	 * 
	 * @param og 
	 * @param g
	 * @param withGrid
	 * @param withXUnits
	 * @param withYUnits
	 * @param withXScale
	 * @param withYScale
	 * @param drawY0
	 * @param height
	 * @param width
	 * @param left 
	 * @param right 
	 * @param top 
	 * @param bottom 
	 * @param plotAreaInsets
	 * @param isResized
	 */
	void drawGraph(Object og, int height, int width, int left, int right,
			int top, int bottom, boolean isResized) {
		JDXSpectrum spec0 = getSpectrumAt(0);
		int subIndex = spec0.getSubIndex();
		nucleusX = spec0.nucleusX;
		nucleusY = spec0.nucleusY;
		zoomEnabled = pd.getBoolean(ScriptToken.ENABLEZOOM);
		this.height = height;
		this.width = width;
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
		// yValueMovedTo = Double.NaN;
		is1D2DSplit = (!spec0.is1D() && pd.getBoolean(ScriptToken.DISPLAY2D) && (imageView != null || get2DImage()));
		haveSelectedSpectrum = false;
		selectedSpectrumIntegrals = null;
		selectedSpectrumMeasurements = null;
		for (int iSplit = 0; iSplit < nSplit; iSplit++) {
			// for now, at least, we only allow one 2D image
			setPositionForFrame(iSplit);
			drawAll(og, subIndex, iSplit, isResized || nSplit > 1);
		}
		setPositionForFrame(nSplit > 1 ? pd.currentSplitPoint : 0);
		if (pd.isPrinting)
			return;
	}

	synchronized void escapeKeyPressed(boolean isDEL) {
		if (zoomBox1D != null)
			zoomBox1D.xPixel0 = zoomBox1D.xPixel1 = 0;
		if (zoomBox2D != null)
			zoomBox2D.xPixel0 = zoomBox2D.xPixel1 = 0;
		if (!inPlotMove)
			return;
		if (pendingMeasurement != null) {
			pendingMeasurement = null;
			return;
		}
		pd.thisWidget = null;
		pendingMeasurement = null;
		if (selectedSpectrumMeasurements != null && selectedMeasurement != null) {
			if (isDEL)
				selectedSpectrumMeasurements.clear(viewData.minXOnScale,
						viewData.maxXOnScale);
			else
				selectedSpectrumMeasurements.remove(selectedMeasurement);
			selectedMeasurement = null;
			updateDialog(AType.Measurements, -1);
		}
		if (selectedSpectrumIntegrals != null && selectedIntegral != null) {
			if (isDEL)
				selectedSpectrumIntegrals.clear(viewData.minXOnScale,
						viewData.maxXOnScale);
			else
				selectedSpectrumIntegrals.remove(selectedIntegral);
			selectedIntegral = null;
			updateDialog(AType.Integration, -1);
		}
	}

	static GraphSet findGraphSet(List<GraphSet> graphSets, int xPixel, int yPixel) {
		for (int i = graphSets.size(); --i >= 0;)
			if (graphSets.get(i).hasPoint(xPixel, yPixel))
				return graphSets.get(i);
		return null;
	}

	PeakInfo findMatchingPeakInfo(PeakInfo pi) {
		PeakInfo pi2 = null;
		for (int i = 0; i < spectra.size(); i++)
			if ((pi2 = (spectra.get(i)).findMatchingPeakInfo(pi)) != null)
				break;
		return pi2;
	}

	int getCurrentSpectrumIndex() {
		return (nSpectra == 1 ? 0 : iSpectrumSelected);
	}

	Integral getSelectedIntegral() {
		return selectedIntegral;
	}

	boolean getShowAnnotation(AType type, int i) {
		AnnotationData id = getDialog(type, i);
		return (id != null && id.getState());
	}

	String getSolutionColor() {
		JDXSpectrum spectrum = getSpectrum();
		return (spectrum.canShowSolutionColor() ? Visible.Colour(spectrum
				.getXYCoords(), spectrum.getYUnits()) : Visible.noColor);
	}

	boolean hasFileLoaded(String filePath) {
		for (int i = spectra.size(); --i >= 0;)
			if (spectra.get(i).getFilePathForwardSlash().equals(filePath))
				return true;
		return false;
	}

	boolean haveSelectedSpectrum() {
		return haveSelectedSpectrum;
	}

	synchronized void mouseClickedEvent(int xPixel, int yPixel, int clickCount,
			boolean isControlDown) {
		selectedMeasurement = null;
		selectedIntegral = null;
		boolean isNextClick = nextClickForSetPeak;
		nextClickForSetPeak = false;
		if (checkArrowUpDownClick(xPixel, yPixel))
			return;
		lastClickX = Double.NaN;
		if (isSplitWidget(xPixel, yPixel)) {
			splitStack(pd.graphSets, nSplit == 1);
			// pd.repaint();
			return;
		}
		PlotWidget pw = getPinSelected(xPixel, yPixel);
		if (pw != null) {
			setWidgetValueByUser(pw);
			return;
		}
		boolean is2D = (imageView != null && xPixel == imageView.fixX(xPixel) && yPixel == fixY(yPixel));
		if (clickCount == 2 && iSpectrumClicked == -1
				&& iPreviousSpectrumClicked >= 0) {
			setSpectrumClicked(iPreviousSpectrumClicked);
		}
		if (!is2D && isControlDown) {
			setSpectrumClicked(iPreviousSpectrumClicked);
			if (pendingMeasurement != null) {
				processPendingMeasurement(xPixel, yPixel, -3);
			} else if (iSpectrumClicked >= 0) {
				processPendingMeasurement(xPixel, yPixel, 3);
			}
			return;
		}
		lastXMax = Double.NaN; // TODO: was for "is2D || !isControlDown
		if (clickCount == 2) {
			if (is2D) {
				if (sticky2Dcursor) {
					addAnnotation(getAnnotation(imageView.toX(xPixel), imageView
							.toSubspectrumIndex(yPixel), pd.coordStr, false, true, 5, 5),
							true);
				}
				sticky2Dcursor = !sticky2Dcursor;
				set2DCrossHairs(xPixel, yPixel);
				// pd.repaint();
				return;
			}

			// 1D double-click

			if (isInTopBar(xPixel, yPixel)) {
				// 1D x zoom reset to original
				doZoom(toX0(xPixel0), viewData.minY, toX0(xPixel1), viewData.maxY,
						true, false, true);
				// } else if (isInRightBar(xPixel, yPixel)) {
				// // no longer possible
				// doZoom(view.minXOnScale, viewList.get(0).minY,
				// view.maxXOnScale, viewList.get(0).maxY, true, true,
				// false);
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
					// pd.repaint();
					return;
				}
			}
			sticky2Dcursor = false;
			set2DCrossHairs(xPixel, yPixel);
			// pd.repaint();
			return;
		}

		// 1D single click

		if (isInPlotRegion(xPixel, yPixel)) {
			if (selectedSpectrumIntegrals != null
					&& checkIntegralNormalizationClick(xPixel, yPixel))
				return;
			if (pendingMeasurement != null) {
				processPendingMeasurement(xPixel, yPixel, 1);
				return;
			}
			setCoordClicked(toX(xPixel), toY(yPixel));
			updateDialog(AType.PeakList, -1);
			if (isNextClick) {
				shiftSpectrum(Double.NaN, Double.NaN);
				return;
			}
		} else {
			setCoordClicked(Double.NaN, 0);
		}
		pd.notifyPeakPickedListeners(null);
	}

	private void updateDialog(AType type, int iSpec) {
		AnnotationData ad = getDialog(type, iSpec);
		if (ad != null && ad.getState() && isVisible(ad))
			((AnnotationDialog) ad).update(pd.coordClicked);
	}

	private boolean isVisible(AnnotationData ad) {
		return (ad instanceof AnnotationDialog && ad.isVisible());
	}

	synchronized void mouseReleasedEvent() {
		if (pd.integralShiftMode != 0) {
			pd.integralShiftMode = 0;
			zoomBox1D.xPixel1 = zoomBox1D.xPixel0;
			// pd.repaint();
			return;
		}
		if (iSpectrumMovedTo >= 0)
			setUserYFactor(iSpectrumMovedTo);
		PlotWidget thisWidget = pd.thisWidget;
		if (pd.isIntegralDrag) {
			if (isGoodEvent(zoomBox1D, null, true)) {
				checkIntegral(toX(zoomBox1D.xPixel0), toX(zoomBox1D.xPixel1), true);
			}
			zoomBox1D.xPixel1 = zoomBox1D.xPixel0 = 0;
			pendingIntegral = null;
			pd.isIntegralDrag = false;
			// pd.repaint();
		} else if (thisWidget == zoomBox2D) {
			if (!isGoodEvent(zoomBox2D, null, true))
				return;
			imageView.setZoom(zoomBox2D.xPixel0, zoomBox2D.yPixel0,
					zoomBox2D.xPixel1, zoomBox2D.yPixel1);
			zoomBox2D.xPixel1 = zoomBox2D.xPixel0;
			// 2D xy zoom
			doZoom(imageView.toX(imageView.xPixel0), viewData.minY, imageView
					.toX(imageView.xPixel0 + imageView.xPixels - 1), viewData.maxY, true,
					false, false);
		} else if (thisWidget == zoomBox1D) {
			if (!isGoodEvent(zoomBox1D, null, true))
				return;
			int x1 = zoomBox1D.xPixel1;
			// 1D xy zoom
			doZoom(toX(zoomBox1D.xPixel0), toY(zoomBox1D.yPixel0), toX(x1),
					toY(zoomBox1D.yPixel1), true, false, true);
			zoomBox1D.xPixel1 = zoomBox1D.xPixel0;
		} else if (thisWidget == pin1Dx0 || thisWidget == pin1Dx1
				|| thisWidget == cur2Dx0 || thisWidget == cur2Dx1) {
			addCurrentZoom();
		}
	}

	synchronized void mouseMovedEvent(int xPixel, int yPixel) {
		if (nSpectra > 1) {
			int iFrame = getSplitPoint(yPixel);
			setPositionForFrame(iFrame);
			setSpectrumMovedTo(nSplit > 1 ? iFrame : iSpectrumSelected);
			if (iSpectrumMovedTo >= 0)
				setUserYFactor(iSpectrumMovedTo);
		}
		inPlotMove = isInPlotRegion(xPixel, yPixel);
		xPixelMovedTo = (inPlotMove ? xPixel : -1);
		xPixelMovedTo2 = -1;
		if (inPlotMove) {
			xValueMovedTo = toX(xPixelMovedTo);
			yValueMovedTo = getSpectrum().getYValueAt(xValueMovedTo);
		}
		if (pd.integralShiftMode != 0) {
			AnnotationData ad = getDialog(AType.Integration, -1);
			Coordinate[] xy = ((IntegralData) ad.getData()).getXYCoords();
			double y = xy[pd.integralShiftMode > 0 ? xy.length - 1 : 0].getYVal();

			((IntegralData) ad.getData()).shiftY(pd.integralShiftMode, toPixelYint(y) + yPixel
					- (pd.integralShiftMode > 0 ? yPixelPlot1 : yPixelPlot0), yPixel0,
					yPixels);
		} else if (pd.isIntegralDrag) {
		} else if (pendingMeasurement != null) {
			processPendingMeasurement(xPixel, yPixel, 0);
			setToolTipForPixels(xPixel, yPixel);
		} else {
			selectedMeasurement = (inPlotMove && selectedSpectrumMeasurements != null ? findMeasurement(
					selectedSpectrumMeasurements, xPixel, yPixel, 0)
					: null);
			selectedIntegral = null;
			if (inPlotMove && selectedSpectrumIntegrals != null
					&& selectedMeasurement == null) {
				selectedIntegral = (Integral) findMeasurement(
						selectedSpectrumIntegrals, xPixel, yPixel, 0);
				if (selectedIntegral == null)
					selectedIntegral = (Integral) findMeasurement(
							selectedSpectrumIntegrals, xPixel, yPixel, -5);
			}
			setToolTipForPixels(xPixel, yPixel);
			if (imageView == null) {
				piMouseOver = null;
				int iSpec = (nSplit > 1 ? iSpectrumMovedTo : iSpectrumClicked);
				if (!isDrawNoSpectra() && iSpec >= 0) {
					JDXSpectrum spec = spectra.get(iSpec);
					if (spec.getPeakList() != null) {
						coordTemp.setXVal(toX(xPixel));
						coordTemp.setYVal(toY(yPixel));
						piMouseOver = spec.findPeakByCoord(coordTemp);
					}
				}
			} else {
				if (!pd.display1D && sticky2Dcursor)
					set2DCrossHairs(xPixel, yPixel);
			}
		}
		// pd.repaint();
	}

	/**
	 * Displays the next view zoomed
	 */
	void nextView() {
		if (currentZoomIndex + 1 < viewList.size())
			setZoomTo(currentZoomIndex + 1);
	}

	/**
	 * Displays the previous view zoomed
	 */
	void previousView() {
		if (currentZoomIndex > 0)
			setZoomTo(currentZoomIndex - 1);
	}

	/**
	 * Resets the spectrum to it's original view
	 */
	void resetView() {
		setZoomTo(0);
	}

	void removeAllHighlights() {
		removeAllHighlights(null);
	}

	/**
	 * Remove the highlight at the specified index in the internal list of
	 * highlights The index depends on the order in which the highlights were
	 * added
	 * 
	 * @param index
	 *          the index of the highlight in the list
	 */
	void removeHighlight(int index) {
		highlights.remove(index);
	}

	/**
	 * Remove the highlight specified by the starting and ending x value
	 * 
	 * @param x1
	 *          the x value of the coordinate where the highlight started
	 * @param x2
	 *          the x value of the coordinate where the highlight ended
	 */
	void removeHighlight(double x1, double x2) {
		for (int i = highlights.size(); --i >= 0;) {
			Highlight h = highlights.get(i);
			if (h.x1 == x1 && h.x2 == x2)
				highlights.remove(i);
		}
	}

	void scaleYBy(double factor) {
		if (imageView == null && !zoomEnabled)
			return;
		// from CTRL +/-
		viewData.scaleSpectrum(imageView == null ? iSpectrumSelected : -2, factor);
		if (imageView != null) {
			update2dImage(true, false);
			resetPinsFromView();
		}
		pd.refresh();
		// view.scaleSpectrum(-1, factor);
	}

	/**
	 * here we are selecting a spectrum based on a message from Jmol matching type
	 * and model
	 * 
	 * @param filePath
	 * @param type
	 * @param model
	 * @return haveFound
	 */
	boolean selectSpectrum(String filePath, String type, String model) {
		boolean haveFound = false;
		for (int i = spectra.size(); --i >= 0;)
			if ((filePath == null || getSpectrumAt(i).getFilePathForwardSlash()
					.equals(filePath))
					&& (getSpectrumAt(i).matchesPeakTypeModel(type, model))) {
				setSpectrumSelected(i);
				if (nSplit > 1)
					splitStack(pd.graphSets, true);
				haveFound = true;
			}
		if (nSpectra > 1 && !haveFound && iSpectrumSelected >= 0
				&& !pd.isCurrentGraphSet(this))
			setSpectrumSelected(Integer.MIN_VALUE); // no plots in that case
		return haveFound;
	}

	PeakInfo selectPeakByFileIndex(String filePath, String index) {
		PeakInfo pi;
		for (int i = spectra.size(); --i >= 0;)
			if ((pi = getSpectrumAt(i).selectPeakByFileIndex(filePath, index)) != null)
				return pi;
		return null;
	}

	void setSelected(int i) {
		if (i < 0) {
			bsSelected.clear();
			setSpectrumClicked(-1);
			return;
		}
		bsSelected.set(i);
		setSpectrumClicked((bsSelected.cardinality() == 1 ? i : -1));
		if (nSplit > 1 && i >= 0)
			pd.currentSplitPoint = i;
	}

	void setSelectedIntegral(double val) {
		JDXSpectrum spec = selectedIntegral.getSpectrum();
		getIntegrationGraph(getSpectrumIndex(spec)).setSelectedIntegral(
				selectedIntegral, val);
	}

	void setShowAnnotation(AType type, Boolean tfToggle) {
		AnnotationData id = getDialog(type, -1);
		if (id == null) {
			if (tfToggle != null && tfToggle != Boolean.TRUE)
				return; // does not exist and "OFF" -- ignore
			// does not exist, and TOGGLE or ON
			if (type == AType.PeakList)
				pd.owner.showDialog(type);
			return;
		}
		if (tfToggle == null) {
			// exists and "TOGGLE"
			if (id instanceof AnnotationDialog)
				((AnnotationDialog) id).setVisible(tfToggle != null
						&& ((AnnotationDialog) id).isVisible());
			else
				id.setState(!id.getState());
			return;
		}
		// exists and "ON" or "OFF"
		id.setState(tfToggle.booleanValue());

		// if (type == AType.Integration)
		// checkIntegral(parameters, "UPDATE");
		// id.setState(tfToggle == null ? !id.getState() : tfToggle.booleanValue());
	}

	boolean checkIntegral(Parameters parameters, String value) {
		JDXSpectrum spec = getSpectrum();
		if (!spec.canIntegrate() || reversePlot)
			return false;
		int iSpec = getFixedSelectedSpectrumIndex();
		AnnotationData ad = getDialog(AType.Integration, -1);
		if (value == null && ad != null)
			return true;
		switch (IntegralData.IntMode.getMode(value.toUpperCase())) {
		case ON:
			integrate(iSpec, parameters);
			break;
		case OFF:
			integrate(iSpec, null);
			break;
		case TOGGLE:
			integrate(iSpec, ad == null ? parameters : null);
			break;
		case AUTO:
			if (ad == null) {
				checkIntegral(parameters, "ON");
				ad = getDialog(AType.Integration, -1);
			}
			if (ad != null)
				((IntegralData) ad.getData()).autoIntegrate();
			break;
		case MARK:
			if (ad == null) {
				checkIntegral(parameters, "ON");
				ad = getDialog(AType.Integration, -1);
			}
			if (ad != null)
				((IntegralData) ad.getData()).addMarks(value.substring(4).trim());
			break;
		case UPDATE:
			if (ad != null)
				((IntegralData) ad.getData()).update(parameters);
		}
		updateDialog(AType.Integration, -1);
		return true;
	}

	void setSpectrum(int iSpec, boolean fromSplit) {
		if (fromSplit) {
			if (nSplit > 1)
				setSpectrumClicked(iSpec);
		} else {
			setSpectrumClicked(iSpec);
			stackSelected = false;
			showAllStacked = false;
		}
	}

	void setSpectrum(JDXSpectrum spec) {
		// T/A conversion for IR
		clearViews();
		int pt = getFixedSelectedSpectrumIndex();
		spectra.remove(pt);
		spectra.add(pt, spec);
		viewData.setSpectrumYRef(pt, spec.getYRef());
		pendingMeasurement = null;
	}

	void setZoom(double x1, double y1, double x2, double y2) {
		// called by
		// 1. double-clicking on a tree node in the application to reset (0,0,0,0)
		// 2. the YSCALE command (NaN,y1,NaN,y2)
		// 3. the ZOOM command (0,0,0,0) or (x1, 0, x2, 0) or (x1, y1, x2, y2)
		setZoomTo(0);
		if (Double.isNaN(x1)) {
			// y zoom only
			x1 = viewData.minX;
			x2 = viewData.maxX;
			imageView = null;
		}
		if (x1 == 0 && x2 == 0) {
			newPins();
		} else {
			doZoom(x1, y1, x2, y2, true, false, true);
			return;
		}
		imageView = null;
	}

	/**
	 * result depends upon the values of dx and x1: x1 NaN --> just use dx dx NaN
	 * --> use value as x1 and nearest peak to last click as x0 dx MIN_VALUE -->
	 * use value as x1 and last click exactly as x0 dx or x1 MAX_VALUE --> reset
	 * to original (apply dx = -specShift)
	 * 
	 * @param dx
	 * @param x1
	 * @return true if accomplished
	 */
	boolean shiftSpectrum(double dx, double x1) {
  	// setpeak NONE     Double.NaN,       Double.MAX_VALUE
  	// shiftx  NONE     Double.MAX_VALUE, Double.NaN

		// setpeak  ?       Double.NaN,       Double.MIN_VALUE

		// setpeak x.x      Double.NaN,       value
  	// setx    x.x			Double.MIN_VALUE, value
		// setpeak          Double.NaN,       Double.NaN
    // setx       			Double.MIN_VALUE, Double.NaN

		// shiftx  x.x      value,            Double.NaN

		JDXSpectrum spec = getSpectrum();
		if (!spec.isNMR() || !spec.is1D())
			return false;
		if (x1 == Double.MAX_VALUE || dx == Double.MAX_VALUE) {
			// setPeak NONE or setX NONE
			dx = -spec.addSpecShift(0);
		} else if (x1 == Double.MIN_VALUE) {
			// setpeak ? -- set for setpeak Double.NaN,       Double.NaN after click
			nextClickForSetPeak = true;
			pd.owner.showMessage("Click on or beside a peak to set its chemical shift.", "Set Reference");
			return false;
		} else if (Double.isNaN(dx) || dx == Double.MIN_VALUE) {
			// setpeak     or setx
			// setpeak x.x or setx x.x
			double x0 = (dx == Double.MIN_VALUE ? lastClickX : getNearestPeak(spec, lastClickX, toY(pd.mouseY)));
 			if (Double.isNaN(x0))
				return false;
			if (Double.isNaN(x1))
				try {
					String s = pd.getInput("New chemical shift (set blank to reset)",
							"Set Reference", "" + x0).trim();
					if (s.length() == 0)
						x1 = x0 - spec.addSpecShift(0);
					else		
					  x1 = Double.parseDouble(s);
				} catch (Exception e) {
					return false;
				}
			dx = x1 - x0;
		}
		if (dx == 0)
			return false;
		spec.addSpecShift(dx);
		if (annotations != null)
			for (int i = annotations.size(); --i >= 0;)
				if (annotations.get(i).spec == spec)
					annotations.get(i).addSpecShift(dx);
		if (dialogs != null)
			for (Map.Entry<String, AnnotationData> e : dialogs.entrySet())
				if (e.getValue().getSpectrum() == spec)
					e.getValue().addSpecShift(dx);
		double dx0 = viewData.specShift;
		for (int i = viewList.size(); --i >= 0;)
			viewList.get(i).addSpecShift(dx);
		if (viewData.specShift == dx0)
			viewData.addSpecShift(dx);
		if (!Double.isNaN(lastClickX))
			lastClickX += dx;
		updateDialogs();
		doZoom(viewData.minXOnScale, viewData.minYOnScale, viewData.maxXOnScale,
				viewData.maxYOnScale, false, false, true);
		pd.repaint();
		return true;
	}

	void toPeak(int istep) {
		istep *= (drawXAxisLeftToRight ? 1 : -1);
		if (Double.isNaN(lastClickX))
			lastClickX = 0;
		JDXSpectrum spec = getSpectrum();
		Coordinate coord = setCoordClicked(lastClickX, 0);
		int iPeak = spec.setNextPeak(coord, istep);
		if (iPeak < 0)
			return;
		PeakInfo peak = spec.getPeakList().get(iPeak);
		spec.setSelectedPeak(peak);
		setCoordClicked(peak.getX(), 0);
		pd.notifyPeakPickedListeners(new PeakPickEvent(pd.owner, pd.coordClicked,
				peak));
	}

	// methods that only act on SELECTED spectra

	void scaleSelectedBy(double f) {
		for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
				.nextSetBit(i + 1))
			viewData.scaleSpectrum(i, f);
	}

	// overridden methods

	@Override
	public String toString() {
		return "gs: " + nSpectra + " " + spectra + " "
				+ spectra.get(0).getFilePath();
	}

	void setXPointer(JDXSpectrum spec, double x) {
		if (spec != null)
			setSpectrumClicked(getSpectrumIndex(spec));
		xValueMovedTo = lastClickX = x;
		setXPixelMovedTo(x);
		yValueMovedTo = Double.NaN;
	}

	void setXPointer2(JDXSpectrum spec, double x) {
		if (spec != null)
			setSpectrumClicked(getSpectrumIndex(spec));
		xPixelMovedTo2 = toPixelX(x);
	}

	boolean hasCurrentMeasurement(AType type) {
		return ((type == AType.Integration ? selectedSpectrumIntegrals
				: selectedSpectrumMeasurements) != null);
	}

	private Map<String, AnnotationData> dialogs;
	private Object[] aIntegrationRatios;

	AnnotationData getDialog(AType type, int iSpec) {
		if (iSpec == -1)
			iSpec = getCurrentSpectrumIndex();
		if (dialogs == null || iSpec < 0)
			return null;
		return dialogs.get(type + "_" + iSpec);
	}

	void removeDialog(int iSpec, AType type) {
		if (dialogs == null || iSpec < 0)
			return;
		dialogs.remove(type + "_" + iSpec);
	}

	void addDialog(int iSpec, AType type, AnnotationData dialog) {
		if (iSpec < 0) {
			iSpec = getSpectrumIndex(dialog.getSpectrum());
			dialog = null;
		}
		if (dialogs == null)
			dialogs = new Hashtable<String, AnnotationData>();
		String key = type + "_" + iSpec;
		dialog.setKey(key);
		dialogs.put(key, dialog);
	}

	void removeDialog(AnnotationDialog dialog) {
		String key = dialog.getKey();
		dialogs.remove(key);
		AnnotationData data = dialog.getData();
		if (data != null)
			dialogs.put(key, data);
	}

	public MeasurementData getPeakListing(int iSpec, Parameters p, boolean forceNew) {
		if (iSpec < 0)
			iSpec = getCurrentSpectrumIndex();
		if (iSpec < 0)
			return null;
		AnnotationData dialog = getDialog(AType.PeakList, -1);
		if (dialog == null) {
			if (!forceNew)
				return null;
			addDialog(iSpec, AType.PeakList, dialog = new PeakData(AType.PeakList,
					getSpectrum()));
		}
		((PeakData) dialog.getData()).setPeakList(p, null, viewData);
		if (dialog instanceof AnnotationDialog)
			((AnnotationDialog) dialog).setFields();
		return dialog.getData();
	}

	public void setPeakListing(Boolean tfToggle) {
		AnnotationData dialog = getDialog(AType.PeakList, -1);
		AnnotationDialog ad = (dialog instanceof AnnotationDialog ? (AnnotationDialog) dialog : null);
		boolean isON = (tfToggle == null ? ad == null || !ad.isVisible() : tfToggle.booleanValue());
		if (isON) {
			pd.owner.showDialog(AType.PeakList);
		} else {
			if (dialog instanceof AnnotationDialog)
				((AnnotationDialog) dialog).setVisible(false);
		}
	}

	boolean haveIntegralDisplayed(int i) {
		AnnotationData ad = getDialog(AType.Integration, i);
		return (ad != null && ad.getState());
	}

	IntegralData getIntegrationGraph(int i) {
		AnnotationData ad = getDialog(AType.Integration, i);
		return (ad == null ? null : (IntegralData) ad.getData());
	}

	public void setIntegrationRatios(String value) {
		int iSpec = getFixedSelectedSpectrumIndex();
		if (aIntegrationRatios == null)
			aIntegrationRatios = new Object[nSpectra];
		aIntegrationRatios[iSpec] = IntegralData.getIntegrationRatiosFromString(
				getSpectrum(), value);
	}

	/**
	 * deprecated -- or at least not compatible with multiple spectra
	 * 
	 * @param i
	 * @return list 
	 */
	@SuppressWarnings("unchecked")
	ArrayList<Annotation> getIntegrationRatios(int i) {
		return (ArrayList<Annotation>) (aIntegrationRatios == null ? null
				: aIntegrationRatios[i]);
	}

	boolean integrate(int iSpec, Parameters parameters) {
		JDXSpectrum spec = getSpectrumAt(iSpec);
		if (parameters == null || !spec.canIntegrate()) {
			removeDialog(iSpec, AType.Integration);
			return false;
		}
		addDialog(iSpec, AType.Integration, new IntegralData(spec, parameters));
		return true;
	}

	public IntegralData getIntegration(int iSpec, Parameters p, boolean forceNew) {
		if (iSpec < 0)
			iSpec = getCurrentSpectrumIndex();
		if (iSpec < 0)
			return null;
		AnnotationData dialog = getDialog(AType.Integration, -1);
		if (dialog == null) {
			if (!forceNew)
				return null;
			addDialog(iSpec, AType.Integration, new IntegralData(getSpectrum(), p));
		}
		return (IntegralData) dialog.getData();
	}

	public Map<String, Object> getMeasurementInfo(AType type, int iSpec) {
	  MeasurementData md;	
		switch (type) {
		case PeakList:
			md = getPeakListing(iSpec, null, false);
  		break;
		case Integration:
			md = getIntegration(iSpec, null, false);
			break;
		default:
			return null;
		}
		if (md == null)
			return null;
		Map<String, Object> info = new Hashtable<String, Object>();
		md.getInfo(info);
		return info;
	}
	
	Map<String, Object> getInfo(String key, int iSpec) {
		Map<String, Object> spectraInfo = new Hashtable<String, Object>();
		if ("viewInfo".equalsIgnoreCase(key))
			return viewData.getInfo(spectraInfo);
		List<Map<String, Object>> specInfo = new ArrayList<Map<String, Object>>();		
		spectraInfo.put("spectra", specInfo);
		for (int i = 0; i < nSpectra; i++) {
			if (iSpec >= 0 && i != iSpec)
				continue;
			JDXSpectrum spec = spectra.get(i);
			Map<String, Object> info = spec.getInfo(key);
			if (iSpec >= 0 && key != null 
					&& (info.size() == 2 || key.equalsIgnoreCase("id"))) {
				if (info.size() == 2)
					info.remove("id");
				return info;
			}
			Parameters.putInfo(key, info, "type", spec.getDataType());
			Parameters.putInfo(key, info, "titleLabel", spec.getTitleLabel());
			Parameters.putInfo(key, info, "filePath", spec.getFilePath().replace('\\', '/'));
			Parameters.putInfo(key, info, "PeakList", (Parameters.isMatch(key,
					"PeakList") ? getMeasurementInfo(AType.PeakList, i) : null));
			Parameters.putInfo(key, info, "Integration", (Parameters.isMatch(key,
					"Integration") ? getMeasurementInfo(AType.Integration, i) : null));
			if (iSpec >= 0)
				return info;
			specInfo.add(info);
		}
		return spectraInfo;
	}


}
