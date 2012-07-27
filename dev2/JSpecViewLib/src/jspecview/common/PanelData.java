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

import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import jspecview.common.Annotation.AType;

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
	}

	// Critical fields

	private ArrayList<PanelListener> listeners = new ArrayList<PanelListener>();

	public void addListener(PanelListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	private GraphSet currentGraphSet;

	GraphSet getCurrentGraphSet() {
		return currentGraphSet;
	}

	Hashtable<ScriptToken, Object> options = new Hashtable<ScriptToken, Object>();
	JSVPanel owner;
	List<GraphSet> graphSets;
	int currentSplitPoint;
	PlotWidget thisWidget;
	Coordinate coordClicked;
	Coordinate[] coordsClicked;

	public void dispose() {
		owner = null;
		for (int i = 0; i < graphSets.size(); i++)
			graphSets.get(i).dispose();
		graphSets = null;
		currentGraphSet = null;
		coordClicked = null;
		coordsClicked = null;
		thisWidget = null;
		options = null;
		listeners = null;
	}

	// plot parameters

	static final int defaultPrintHeight = 450, defaultPrintWidth = 280;
	static final int topMargin = 40, bottomMargin = 50, leftMargin = 60,
			rightMargin = 50;

	boolean ctrlPressed;
	boolean drawXAxisLeftToRight;
	boolean isIntegralDrag;
	boolean xAxisLeftToRight = true;

	int integralShiftMode;
	int top = topMargin;
	int bottom = bottomMargin;
	int left = leftMargin;
	int right = rightMargin;

	String coordStr = "";
	String startupPinTip = "Click to set.";
	String title;
	
	private int clickCount;
	private int nSpectra;
	int thisWidth;
	private int thisHeight;
	private int startIndex, endIndex;

	
	private String commonFilePath;
	private String viewTitle;


	public void setViewTitle(String title) {
		viewTitle = title;
	}

	public String getViewTitle() {
		return (viewTitle == null ? getTitle() : viewTitle);
	}

	public Map<String, Object> getInfo(boolean selectedOnly, String key) {
		Map<String, Object> info = new Hashtable<String, Object>();
		List<Map<String, Object>> sets = null;
		if (selectedOnly)
			return currentGraphSet.getInfo(key, getCurrentSpectrumIndex());
		Set<Entry<ScriptToken, Object>> entries = options.entrySet();
		for (Entry<ScriptToken, Object> entry : entries)
			Parameters.putInfo(key, info, entry.getKey().name(), entry.getValue());
		Parameters.putInfo(key, info, "type", getSpectrumAt(0).getDataType());
		Parameters.putInfo(key, info, "title", title);
		Parameters.putInfo(key, info, "nSets", Integer.valueOf(graphSets.size()));
		sets = new ArrayList<Map<String, Object>>();
		for (int i = graphSets.size(); --i >= 0;)
			sets.add(graphSets.get(i).getInfo(key, -1));
		info.put("sets", sets);
		return info;
	}

	void setBoolean(Parameters parameters, ScriptToken st) {
		if (st == null) {
			Map<ScriptToken, Boolean> booleans = parameters.getBooleans();
			for (Map.Entry<ScriptToken, Boolean> entry : booleans.entrySet())
				setBoolean(parameters, entry.getKey());
			return;
		}
		setBoolean(st, parameters.getBoolean(st));
	}

	@SuppressWarnings("incomplete-switch")
	public void setBoolean(ScriptToken st, boolean isTrue) {
		if (st == ScriptToken.REVERSEPLOT) {
			currentGraphSet.setReversePlot(isTrue);
			return;
		}
		options.put(st, Boolean.valueOf(isTrue));
		switch (st) {
		case DISPLAY1D:
		case DISPLAY2D:
			doReset = true;
			break;
		}
	}

	public boolean getBoolean(ScriptToken st) {
		if (st == ScriptToken.REVERSEPLOT)
			return currentGraphSet.reversePlot;
		Object b = options.get(st);
		return (b != null && (b instanceof Boolean) && ((Boolean) b) == Boolean.TRUE);
	}

	// //////// settable colors and fonts //////////

	private String displayFontName;
	private String titleFontName;

	@SuppressWarnings("incomplete-switch")
	void setFontName(ScriptToken st, String fontName) {
		switch (st) {
		case DISPLAYFONTNAME:
			displayFontName = fontName;
			break;
		case TITLEFONTNAME:
			titleFontName = fontName;
			break;
		}
		if (fontName != null)
			options.put(st, fontName);
	}

	// ///////// print parameters ///////////

	boolean isPrinting;
	
	private boolean doReset = true;
	
	String printingFont;
	String printGraphPosition = "default";
	boolean titleDrawn;
	boolean display1D;
	boolean isLinked;
	String printJobTitle;

	public boolean getDisplay1D() {
		return display1D;
	}

	// //// initialization - from AwtPanel

	List<JDXSpectrum> spectra;
	
	void initSingleSpectrum(JDXSpectrum spectrum) {
		spectra = new ArrayList<JDXSpectrum>();
		spectra.add(spectrum);
		initJSVPanel(spectra, 0, 0);
	}

	enum LinkMode {	
		
		ALL, NONE, AB, ABC;
		
		public static LinkMode getMode(String abc) {
			if (abc.equals("*"))
				return ALL;
			for (LinkMode mode : values())
				if (mode.name().equalsIgnoreCase(abc))
					return mode;
			return NONE;
		}    
	}
		
	void initJSVPanel(List<JDXSpectrum> spectra, int startIndex, int endIndex) {
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		owner.setupPlatform();
		nSpectra = spectra.size();
		this.spectra = spectra;
		commonFilePath = spectra.get(0).getFilePath();
		for (int i = 0; i < nSpectra; i++)
			if (!commonFilePath.equalsIgnoreCase(spectra.get(i).getFilePath())) {
				commonFilePath = null;
				break;
			}
		setGraphSets(LinkMode.NONE);
	}

	private void setGraphSets(LinkMode linkMode) {
		graphSets = GraphSet.createGraphSetsAndSetLinkMode(this, owner, spectra, startIndex, endIndex, linkMode);
		currentGraphSet = graphSets.get(0);
		title = getSpectrum().getTitleLabel();
	}

	public PeakInfo findMatchingPeakInfo(PeakInfo pi) {
		PeakInfo pi2 = null;
		for (int i = 0; i < graphSets.size(); i++)
			if ((pi2 = graphSets.get(i).findMatchingPeakInfo(pi)) != null)
				break;
		return pi2;
	}

	public void integrateAll(Parameters parameters) {
		for (int i = graphSets.size(); --i >= 0;)
			graphSets.get(i).integrate(-1, parameters);
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

	/**
	 * Returns the title displayed on the graph
	 * 
	 * @return the title displayed on the graph
	 */
	public String getTitle() {
		return title;
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

	public void refresh() {
		doReset = true;
	}

	public void addAnnotation(List<String> tokens) {
		String title = currentGraphSet.addAnnotation(tokens, getTitle());
		if (title != null)
			this.title = title;
	}

	public void addPeakHighlight(PeakInfo peakInfo) {
		for (int i = 0; i < graphSets.size(); i++)
			graphSets.get(i).addPeakHighlight(peakInfo);
	}

	public PeakInfo selectPeakByFileIndex(String filePath, String index) {
		PeakInfo pi = currentGraphSet.selectPeakByFileIndex(filePath, index);
		if (pi == null)
			for (int i = graphSets.size(); --i >= 0;)
				if (graphSets.get(i) != currentGraphSet
						&& (pi = graphSets.get(i).selectPeakByFileIndex(filePath, index)) != null)
					break;
		return pi;
	}

	public void selectSpectrum(String filePath, String type, String model,
			boolean andCurrent) {
		if (andCurrent)
			currentGraphSet.selectSpectrum(filePath, type, model);
		for (int i = 0; i < graphSets.size(); i += 1)
			if (graphSets.get(i) != currentGraphSet)
				graphSets.get(i).selectSpectrum(filePath, type, model);
	}

	public boolean hasFileLoaded(String filePath) {
		for (int i = graphSets.size(); --i >= 0;)
			if (graphSets.get(i).hasFileLoaded(filePath))
				return true;
		return false;
	}

	/**
	 * Clears all views in the zoom list
	 */
	public void clearAllView() {
		for (int i = graphSets.size(); --i >= 0;)
			graphSets.get(i).clearAllViews();
	}

	/*----------------------- JSVPanel PAINTING METHODS ---------------------*/
	/**
	 * Draws the Spectrum to the panel
	 * 
	 * @param g
	 *          the <code>Graphics</code> object
	 * @param width
	 *          the width to be drawn in pixels
	 * @param height
	 *          the height to be drawn in pixels
	 * @param addFilePath
	 */
	synchronized void drawGraph(Object g, int width, int height,
			boolean addFilePath) {
		boolean withCoords;
		display1D = !isLinked && getBoolean(ScriptToken.DISPLAY1D);
		top = topMargin;
		bottom = bottomMargin;
		if (isPrinting) {
			top *= 2; // for three-hole punching
			bottom *= 2;
			withCoords = false;
		} else {
			withCoords = getBoolean(ScriptToken.COORDINATESON);
			titleOn = getBoolean(ScriptToken.TITLEON);
			gridOn = getBoolean(ScriptToken.GRIDON);
		}
		boolean isResized = (isPrinting || doReset || thisWidth != width || thisHeight != height);
		doReset = false;
		titleDrawn = false;
		thisWidth = width;
		thisHeight = height;
		for (int i = graphSets.size(); --i >= 0;)
			graphSets.get(i).drawGraphSet(g, width, height, left, right, top, bottom,
					isResized);
		if (titleOn && !titleDrawn)
			owner.drawTitle(g, height, width, getDrawTitle(isPrinting));
		if (withCoords)
			owner.drawCoordinates(g);
		if (addFilePath) {
			String s = (commonFilePath != null ? commonFilePath
					: graphSets.size() == 1 && currentGraphSet.getTitle(true) != null ? getSpectrum()
							.getFilePath()
							: null);
			if (s != null) {
				owner.printFilePath(g, left, height, s);
			}
		}
		if (isPrinting)
			owner.printVersion(g, height);

	}

	/**
	 * sets bsSelected to the specified pointer from "select 3.1*1"
	 * 
	 * @param iSpec
	 */
	public void selectFromEntireSet(int iSpec) {
		// note that iSpec is over the entire set
		for (int i = 0, pt = 0; i < graphSets.size(); i++) {
			if (iSpec == Integer.MIN_VALUE) {
				graphSets.get(i).setSelected(-1);
				continue;
			}
			List<JDXSpectrum> specs = graphSets.get(i).spectra;
			for (int j = 0; j < specs.size(); j++, pt++)
				if (iSpec == -1 || iSpec == pt + 1)
					graphSets.get(i).setSelected(j);
		}
	}

	public void scaleSelectedBy(double f) {
		for (int i = graphSets.size(); --i >= 0;)
			graphSets.get(i).scaleSelectedBy(f);

	}

	// //// currentGraphSet methods

	void setCurrentGraphSet(GraphSet gs, int yPixel, int clickCount) {
		int splitPoint = gs.getSplitPoint(yPixel);
		boolean isNewSet = (currentGraphSet != gs);
		boolean isNewSplitPoint = (isNewSet || currentSplitPoint != splitPoint);
		currentGraphSet = gs;
		currentSplitPoint = splitPoint;
		if (isNewSet || gs.nSplit > 1 && isNewSplitPoint)
			setSpectrum(currentSplitPoint, true);
		if (!isNewSet) {
			isNewSet = gs.checkSpectrumClickedEvent(mouseX, mouseY, clickCount);
			if (!isNewSet)
				return;
		}

		// new set (so also new split point)
		// or nSplit > 1 and new split point
		// or nSplit == 1 and showAllStacked and isClick and is a spectrum click)

		if (isNewSet || gs.nSplit > 1 && isNewSplitPoint)
			setSpectrum(currentSplitPoint, true);
		JDXSpectrum spec = gs.getSpectrum();
		notifySubSpectrumChange(spec.getSubIndex(), spec);
	}

	public void splitStack(boolean doSplit) {
		currentGraphSet.splitStack(graphSets, doSplit);
	}

	public int getNumberOfSpectraInCurrentSet() {
		return currentGraphSet.nSpectra;
	}

	public int[] getStartDataPointIndices() {
		return currentGraphSet.viewData.startDataPointIndices;
	}

	public int[] getEndDataPointIndices() {
		return currentGraphSet.viewData.endDataPointIndices;
	}

	public String getSolutionColor() {
		return currentGraphSet.getSolutionColor();
	}

	public boolean haveSelectedSpectrum() {
		return currentGraphSet.haveSelectedSpectrum();
	}

	public boolean getShowAnnotation(AType type) {
		return currentGraphSet.getShowAnnotation(type, -1);
	}

	public void showAnnotation(AType type, Boolean tfToggle) {
		currentGraphSet.setShowAnnotation(type, tfToggle);
	}

	public void setYStackOffsetPercent(int offset) {
		currentGraphSet.yStackOffsetPercent = offset;
	}

	public void setSpectrum(int iSpec, boolean isSplit) {
		currentGraphSet.setSpectrum(iSpec, isSplit);
		// repaint();
	}

	public JDXSpectrum getSpectrum() {
		return currentGraphSet.getSpectrum();
	}

	public void setSpectrum(JDXSpectrum spec) {
		currentGraphSet.setSpectrum(spec);
	}

	public boolean isShowAllStacked() {
		return currentGraphSet.showAllStacked;
	}

	public int getCurrentSpectrumIndex() {
		return currentGraphSet.getCurrentSpectrumIndex();
	}

	public JDXSpectrum getSpectrumAt(int index) {
		return currentGraphSet.getSpectrumAt(index);
	}

	/**
	 * Add information about a region of the displayed spectrum to be highlighted
	 * applet only right now
	 * 
	 * @param gs
	 * 
	 * @param x1
	 *          the x value of the coordinate where the highlight should start
	 * @param x2
	 *          the x value of the coordinate where the highlight should end
	 * @param spec
	 * @param r
	 * @param color
	 *          the color of the highlight
	 * @param a
	 * @param b
	 * @param g
	 */
	public void addHighlight(GraphSet gs, double x1, double x2, JDXSpectrum spec,
			int r, int g, int b, int a) {
		(gs == null ? currentGraphSet : gs).addHighlight(x1, x2, spec, owner
				.getColor(r, g, b, a));
	}

	/**
	 * Remove the highlight at the specified index in the internal list of
	 * highlights The index depends on the order in which the highlights were
	 * added
	 * 
	 * @param index
	 *          the index of the highlight in the list
	 */
	public void removeHighlight(int index) {
		currentGraphSet.removeHighlight(index);
	}

	/**
	 * Remove the highlight specified by the starting and ending x value
	 * 
	 * @param x1
	 *          the x value of the coordinate where the highlight started
	 * @param x2
	 *          the x value of the coordinate where the highlight ended
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

	public void setZoom(double x1, double y1, double x2, double y2) {
		currentGraphSet.setZoom(x1, y1, x2, y2);
		doReset = true;
		notifyListeners(new ZoomEvent(x1, y1, x2, y2));
	}

	public void clearIntegrals() {
		currentGraphSet.clearIntegrals();
	}

	public void clearMeasurements() {
		currentGraphSet.clearMeasurements();
	}

	/**
	 * Resets the spectrum to it's original view
	 */
	public void resetView() {
		currentGraphSet.resetView();
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

	public Integral getSelectedIntegral() {
		return currentGraphSet.getSelectedIntegral();
	}

	public void advanceSubSpectrum(int dir) {
		currentGraphSet.advanceSubSpectrum(dir);
	}

	public void setSelectedIntegral(double val) {
		currentGraphSet.setSelectedIntegral(val);
	}

	public void scaleYBy(double f) {
		currentGraphSet.scaleYBy(f);
	}

	public void toPeak(int i) {
		currentGraphSet.toPeak(i);
	}

	public String getSolutionColorHtml() {
		String color = currentGraphSet.getSolutionColor();
		return "<html><body bgcolor=rgb(" + color
				+ ")><br />Predicted Solution Colour- RGB(" + color
				+ ")<br /><br /></body></html>";
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
	 * click event processing
	 * 
	 * @param coord
	 * @param actualCoord
	 * @return true if a coordinate was picked and fills in coord and actualCoord
	 */
	public boolean getPickedCoordinates(Coordinate coord, Coordinate actualCoord) {
		return Coordinate.getPickedCoordinates(coordsClicked, coordClicked, coord,
				actualCoord);
	}

	/**
	 * shifts xyCoords for a spectrum by the specified amount
	 * 
	 * @param dx
	 *          NaN to determine from x0, x1
	 * @param x1
	 *          NaN to query for new value
	 * @return true if successful
	 */
	public boolean shiftSpectrum(double dx, double x1) {
		return currentGraphSet.shiftSpectrum(dx, x1);

	}

	public void findX(JDXSpectrum spec, double d) {
		currentGraphSet.setXPointer(spec, d);
		// repaint();
	}

	public void findX2(JDXSpectrum spec, double d, JDXSpectrum spec2, double d2) {
		currentGraphSet.setXPointer(spec, d);
		currentGraphSet.setXPointer2(spec2, d2);
		// repaint();
	}

	// called by GraphSet

	boolean isCurrentGraphSet(GraphSet graphSet) {
		return graphSet == currentGraphSet;
	}

	void repaint() {
		owner.doRepaint();
	}

	void setToolTipText(String s) {
		owner.setToolTipText(s);
	}

	Object getHighlightColor() {
		return owner.getColor(ScriptToken.HIGHLIGHTCOLOR);
	}

	void setHighlightColor(Object color) {
		owner.setColor(ScriptToken.HIGHLIGHTCOLOR, color);
	}

	String getInput(String message, String title, String sval) {
		return owner.getInput(message, title, sval);
	}

	void setFont(Object g, int width, int mode, int size, boolean isLabel) {
		owner.setFont(g, (isPrinting ? printingFont : displayFontName), width,
				mode, size, isLabel);
	}

	// listeners to handle various events, from GraphSet or AwtPanel

	/**
	 * 
	 * @param isub
	 *          -1 indicates direction if no subspectra or subspectrum index if
	 *          subspectra
	 * @param spec
	 *          null indicates no subspectra
	 */
	void notifySubSpectrumChange(int isub, JDXSpectrum spec) {
		notifyListeners(new SubSpecChangeEvent(isub, (spec == null ? null : spec
				.getTitleLabel())));
	}

	/**
	 * Notifies CoordinatePickedListeners
	 * 
	 * @param p
	 * 
	 */
	void notifyPeakPickedListeners(PeakPickEvent p) {
		if (p == null) {
			p = new PeakPickEvent(owner, coordClicked, getSpectrum()
					.getAssociatedPeakInfo(coordClicked));
		}
		// PeakInfo pi = p.getPeakInfo();
		// if (pi.getAtoms() == null) {
		// // find matching file/type/model in other panels
		// String filePath = pi.getFilePath();
		// String type = pi.getType();
		// String model = pi.getModel();
		// for (int i = 0; i < graphSets.size(); i++) {
		// if (graphSets.get(i) == currentGraphSet)
		// continue;
		// // just first spectrum for now -- presumed to be GC/MS
		// PeakInfo pi2 = graphSets.get(i).getSpectrumAt(0)
		// .selectPeakByFilePathTypeModel(filePath, type, model);
		// if (pi2 != null)
		// graphSets.get(i).addPeakHighlight(pi2);
		// }
		// }
		// problem is that you cannot have two highlights for the same model.
		// only problem is gc/ms, where we want to select a GC peak based on
		// an MS peak.

		notifyListeners(p);
	}

	void notifyListeners(Object eventObj) {
		for (int i = 0; i < listeners.size(); i++)
			if (listeners.get(i) != null)
				listeners.get(i).panelEvent(eventObj);
	}

	/*--------------the rest are all mouse and keyboard interface -----------------------*/

	void escapeKeyPressed(boolean isDEL) {
		currentGraphSet.escapeKeyPressed(isDEL);
	}

	private enum Mouse {
		UP, DOWN;
	}

	private Mouse mouseState;
	boolean gridOn;
	boolean titleOn;

	boolean hasFocus() {
		return owner.hasFocus();
	}

	boolean isMouseUp() {
		return (mouseState == PanelData.Mouse.UP);
	}

	int mouseX, mouseY;

	void doMouseMoved(int xPixel, int yPixel) {
		mouseX = xPixel;
		mouseY = yPixel;
		mouseState = Mouse.UP;
		clickCount = 0;
		GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
		if (gs == null)
			return;
		gs.mouseMovedEvent(xPixel, yPixel);
	}

	void doMousePressed(int xPixel, int yPixel) {
		mouseState = Mouse.DOWN;
		GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
		if (gs == null)
			return;
		setCurrentGraphSet(gs, yPixel, 0);
		++clickCount;
		gs.checkWidgetEvent(xPixel, yPixel, true);
	}

	void doMouseDragged(int xPixel, int yPixel) {
		isIntegralDrag |= ctrlPressed;
		mouseState = Mouse.DOWN;
		if (GraphSet.findGraphSet(graphSets, xPixel, yPixel) != currentGraphSet)
			return;
		currentGraphSet.checkWidgetEvent(xPixel, yPixel, false);
		currentGraphSet.mouseMovedEvent(xPixel, yPixel);
	}

	void doMouseReleased(boolean isButton1) {
		mouseState = Mouse.UP;
		if (thisWidget == null || !isButton1)
			return;
		currentGraphSet.mouseReleasedEvent();
		thisWidget = null;
		isIntegralDrag = false;
		integralShiftMode = 0;
		// repaint();
	}

	void doMouseClicked(int xPixel, int yPixel, int clickCount,
			boolean isControlDown) {
		GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
		if (gs == null)
			return;
		setCurrentGraphSet(gs, yPixel, clickCount);
		gs.mouseClickedEvent(xPixel, yPixel, clickCount, isControlDown);
		repaint();
	}

	public boolean hasCurrentMeasurements(AType type) {
		return currentGraphSet.hasCurrentMeasurement(type);
	}

	public AnnotationData getDialog(AType type) {
		return currentGraphSet.getDialog(type, -1);
	}

	public void addDialog(int iSpec, AType type, AnnotationData dialog) {
		currentGraphSet.addDialog(iSpec, type, dialog);
	}

	public void getPeakListing(Parameters p, Boolean tfToggle) {
		if (p != null)
			currentGraphSet.getPeakListing(-1, p, true);
		currentGraphSet.setPeakListing(tfToggle);
	}

	public void checkIntegral(Parameters parameters, String value) {
		currentGraphSet.checkIntegral(parameters, value);
	}

	/**
	 * DEPRECATED
	 * 
	 * Sets the integration ratios that will be displayed
	 * 
	 * @param value
	 * 
	 * 
	 * @param ratios
	 *          array of the integration ratios
	 */
	public void setIntegrationRatios(String value) {
		currentGraphSet.setIntegrationRatios(value);
	}

	public ScaleData getView() {
		return currentGraphSet.getCurrentView();
	}

	public void closeAllDialogsExcept(AType type) {
		for (int i = graphSets.size(); --i >= 0;)
			graphSets.get(i).closeDialogsExcept(type);
	}

	public void removeDialog(AnnotationDialog dialog) {
		currentGraphSet.removeDialog(dialog);
	}

	void normalizeIntegral() {
		Integral integral = getSelectedIntegral();
		if (integral == null)
			return;
		String sValue = integral.getText();
		if (sValue.length() == 0)
			sValue = "" + integral.getValue();
		String newValue = getInput("Enter a new value for this integral",
				"Normalize Integral", sValue);
		try {
			setSelectedIntegral(Double.parseDouble(newValue));
		} catch (Exception e) {
		}
	}

	String getDrawTitle(boolean isPrinting) {
		String title = null;
		if (isPrinting)
			title = printJobTitle;
		else if (getNumberOfSpectraTotal() == 1) {
			title = getSpectrum().getPeakTitle();
		} else if (viewTitle != null) {
			if (currentGraphSet.getTitle(false) != null) // check if common title
				title = getSpectrum().getPeakTitle();
			if (title == null)
				title = viewTitle; // "View 1"
		} else {
			title = owner.getTitle().trim();
		}
		if (title.indexOf("\n") >= 0)
			title = title.substring(0, title.indexOf("\n")).trim();
		return title;
	}

	public String getPrintJobTitle(boolean isPrinting) {
		String title = null;
		if (getNumberOfSpectraTotal() == 1) {
			title = getSpectrum().getTitle();
		} else if (viewTitle != null) {
			if (graphSets.size() == 1)
				title = currentGraphSet.getTitle(isPrinting);
			if (title == null)
				title = viewTitle; // "View 1"
		} else {
			title = owner.getTitle().trim();
		}
		if (title.indexOf("\n") >= 0)
			title = title.substring(0, title.indexOf("\n")).trim();
		return title;
	}

	public synchronized void linkSpectra(LinkMode mode) {
		if (mode == LinkMode.ALL)
			mode = (nSpectra == 2 ? LinkMode.AB : nSpectra == 3 ? LinkMode.ABC : LinkMode.NONE);
		if (mode != LinkMode.NONE && mode.toString().length() != nSpectra)
			return;
		setGraphSets(mode);
	}

	private boolean linking;
	public void doZoomLinked(GraphSet graphSet, double initX,
			double finalX, boolean addZoom, boolean checkRange,
			boolean is1d) {
		if (linking)
			return;
		linking = true;
		JDXSpectrum spec = graphSet.getSpectrum();
		for (int i = graphSets.size(); --i >= 0;) {
			GraphSet gs = graphSets.get(i);
			if (gs != graphSet && JDXSpectrum.areXScalesCompatible(spec, graphSets.get(i).getSpectrum(),
					false, false))
				gs.doZoom(initX, 0, finalX, 0, addZoom, checkRange, is1d, false);
		}
		linking = false;
	}

	public void clearLinkViews(GraphSet graphSet) {
		if (linking)
			return;
		linking = true;
		JDXSpectrum spec = graphSet.getSpectrum();
		for (int i = graphSets.size(); --i >= 0;) {
			GraphSet gs = graphSets.get(i);
			if (gs != graphSet && JDXSpectrum.areXScalesCompatible(spec, graphSets.get(i).getSpectrum(),
					false, false))
				gs.clearViews();
		}
		linking = false;
	}

	public void setlinkedXMove(GraphSet graphSet, double x, boolean isX2) {
		if (linking)
			return;
		linking = true;
		JDXSpectrum spec = graphSet.getSpectrum();
		for (int i = graphSets.size(); --i >= 0;) {
			GraphSet gs = graphSets.get(i);
			if (gs != graphSet && JDXSpectrum.areXScalesCompatible(spec, graphSets.get(i).getSpectrum(),
					false, true)) {
				if (isX2) {
					gs.setXPixelMovedTo(Double.MAX_VALUE, x, 0, 0);
				} else {
					gs.setXPixelMovedTo(x, Double.MAX_VALUE, 0, 0);
				}
			}
		}
		linking = false;
	}

	public void set2DCrossHairsLinked(GraphSet graphSet, double x, double y, boolean isLocked) {
		if (Math.abs(x - y) < 0.1)
			x = y = Double.MAX_VALUE;
		for (int i = graphSets.size(); --i >= 0;) {
			GraphSet gs = graphSets.get(i);
			if (gs != graphSet)
				gs.set2DXY(x, y, isLocked);
		}
	}

	public void dialogsToFront() {
		currentGraphSet.dialogsToFront();		
	}
}
