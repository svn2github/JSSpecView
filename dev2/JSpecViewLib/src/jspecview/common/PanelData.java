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
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javajs.api.GenericColor;
import javajs.util.List;

import jspecview.api.AnnotationData;
import jspecview.api.JSVGraphics;
import jspecview.api.JSVPanel;
import jspecview.api.PanelListener;
import jspecview.common.Annotation.AType;
import jspecview.dialog.JSVDialog;
import jspecview.util.JSVColorUtil;

import org.jmol.api.Event;
import org.jmol.api.EventManager;
import org.jmol.util.JmolFont;
import org.jmol.util.Logger;

/**
 * JSVPanel class draws a plot from the data contained a instance of a
 * <code>Graph</code>.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class PanelData implements EventManager {

	JSVGraphics g2d;
	private JSViewer viewer;

	public PanelData(JSVPanel panel, JSViewer viewer) {
		this.viewer = viewer;
		this.jsvp = panel;
		this.g2d = viewer.g2d;
    BLACK = g2d.getColor1(0);
		highlightColor = g2d.getColor4(255, 0, 0, 200);
		zoomBoxColor = g2d.getColor4(150, 150, 100, 130);
		zoomBoxColor2 = g2d.getColor4(150, 100, 100, 130);
	}

	// Critical fields

	private List<PanelListener> listeners = new List<PanelListener>();

	public void addListener(PanelListener listener) {
		if (!listeners.contains(listener)) {
			listeners.addLast(listener);
		}
	}

	private GraphSet currentGraphSet;

	public GraphSet getCurrentGraphSet() {
		return currentGraphSet;
	}

	public Hashtable<ScriptToken, Object> options = new Hashtable<ScriptToken, Object>();
	public JSVPanel jsvp;
	public List<GraphSet> graphSets;
	public int currentSplitPoint;
	public PlotWidget thisWidget;
	public Coordinate coordClicked;
	public Coordinate[] coordsClicked;

	public void dispose() {
		jsvp = null;
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

	public static final int defaultPrintHeight = 450, defaultPrintWidth = 280;
	public static final int topMargin = 40, bottomMargin = 50, leftMargin = 60,
			rightMargin = 50;

	public boolean ctrlPressed;
	public boolean shiftPressed;
	public boolean drawXAxisLeftToRight;
	public boolean isIntegralDrag;
	public boolean xAxisLeftToRight = true;

	public int scalingFactor = 1; // will be 10 for printing
	public int integralShiftMode;
	public int left = leftMargin;
	public int right = rightMargin;

	public String coordStr = "";
	public String startupPinTip = "Click to set.";
	public String title;

	int clickCount;
	private int nSpectra;
	public int thisWidth;
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
		sets = new List<Map<String, Object>>();
		for (int i = graphSets.size(); --i >= 0;)
			sets.addLast(graphSets.get(i).getInfo(key, -1));
		info.put("sets", sets);
		return info;
	}

	public void setBooleans(Parameters parameters, ScriptToken st) {
		if (st == null) {
			Map<ScriptToken, Boolean> booleans = parameters.getBooleans();
			for (Map.Entry<ScriptToken, Boolean> entry : booleans.entrySet())
				setBooleans(parameters, entry.getKey());
			return;
		}
		setBoolean(st, parameters.getBoolean(st));
	}

	@SuppressWarnings("incomplete-switch")
	public void setBoolean(ScriptToken st, boolean isTrue) {
		taintedAll = true;
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
		if (options == null)
			return false;
		Object b = options.get(st);
		return (b != null && (b instanceof Boolean) && ((Boolean) b) == Boolean.TRUE);
	}

	// //////// settable colors and fonts //////////

	private String displayFontName;
	@SuppressWarnings("unused")
	private String titleFontName;

	@SuppressWarnings("incomplete-switch")
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
			options.put(st, fontName);
	}

	// ///////// print parameters ///////////

	public boolean isPrinting;

	private boolean doReset = true;

	public String printingFontName;
	public String printGraphPosition = "default";
	public boolean titleDrawn;
	public boolean display1D;
	public boolean isLinked;
	public String printJobTitle;

	public boolean getDisplay1D() {
		return display1D;
	}

	// //// initialization - from AwtPanel

	public List<JDXSpectrum> spectra;
	public boolean taintedAll = true;

	public void initOne(JDXSpectrum spectrum) {
		spectra = new List<JDXSpectrum>();
		spectra.addLast(spectrum);
		initMany(spectra, 0, 0);
	}

	public enum LinkMode {

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

	public void initMany(List<JDXSpectrum> spectra, int startIndex,
			int endIndex) {
		this.startIndex = startIndex;
		this.endIndex = endIndex;
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
		graphSets = GraphSet.createGraphSetsAndSetLinkMode(this, jsvp, spectra,
				startIndex, endIndex, linkMode);
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

	public void integrateAll(ColorParameters parameters) {
		for (int i = graphSets.size(); --i >= 0;)
			graphSets.get(i).integrate(-1, parameters);
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

	public void setPlotColors(GenericColor[] colors) {
		for (int i = graphSets.size(); --i >= 0;)
			graphSets.get(i).setPlotColors(colors);
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
			graphSets.get(i).clearViews();
	}

	/*----------------------- JSVPanel PAINTING METHODS ---------------------*/
	/**
	 * Draws the Spectrum to the panel
	 * 
	 * @param gMain
	 *          the main <code>Graphics</code> object
	 * @param gTop
	 *          the <code>Graphics</code> object for top-object writing
	 * @param width
	 *          the width to be drawn in pixels
	 * @param height
	 *          the height to be drawn in pixels
	 * @param addFilePath
	 */
	public synchronized void drawGraph(Object gMain, Object gTop, int width, int height,
			boolean addFilePath) {
		boolean withCoords;
		display1D = !isLinked && getBoolean(ScriptToken.DISPLAY1D);
		int top = topMargin;
		int bottom = bottomMargin;
		boolean isResized = (isPrinting || doReset || thisWidth != width || thisHeight != height);
		if (isResized)
			taintedAll = true;
		if (taintedAll)
			g2d.fillBackground(gMain, bgcolor);
		if (gTop != gMain)
			g2d.fillBackground(gTop, null);
		if (isPrinting) {
			top *= 3; // for three-hole punching
			bottom *= 3; // for file name
			scalingFactor = 10; // for high resolution (zooming in within PDF)
			withCoords = false;
		} else {
			scalingFactor = 1;
			withCoords = getBoolean(ScriptToken.COORDINATESON);
			titleOn = getBoolean(ScriptToken.TITLEON);
			gridOn = getBoolean(ScriptToken.GRIDON);
		}
		doReset = false;
		titleDrawn = false;
		thisWidth = width;
		thisHeight = height;
		for (int i = graphSets.size(); --i >= 0;)
			graphSets.get(i).drawGraphSet(gMain, gTop, width, height, left, right, top, bottom,
					isResized, taintedAll);
		if (titleOn && !titleDrawn && taintedAll)
			drawTitle(gMain, height * scalingFactor, width * scalingFactor,
					getDrawTitle(isPrinting));
		if (withCoords && coordStr != null)
			drawCoordinates(gTop, top, thisWidth - right, top - 20);
		if (addFilePath && taintedAll) {
			String s = (commonFilePath != null ? commonFilePath
					: graphSets.size() == 1 && currentGraphSet.getTitle(true) != null ? getSpectrum()
							.getFilePath()
							: null);
			if (s != null) {
				printFilePath(gMain, left, height, s);
			}
		}
		if (isPrinting) {
			printVersion(gMain, height);
		}
		taintedAll = (isPrinting || gMain == gTop);
	}

	/**
	 * @param g
	 * @param top
	 * @param x
	 * @param y
	 */
	public void drawCoordinates(Object g, int top, int x, int y) {
		g2d.setGraphicsColor(g, coordinatesColor);
		JmolFont font = setFont(g, jsvp.getWidth(), JmolFont.FONT_STYLE_PLAIN, 12,
				true);
		g2d.drawString(g, coordStr, x - font.stringWidth(coordStr), y);
	}

	public JmolFont setFont(Object g, int width, int style, float size,
			boolean isLabel) {
		JmolFont font = getFont(g, width, style, size, isLabel);
		g2d.setGraphicsFont(g, font);
		return font;
	}

	public void printFilePath(Object g, int x, int y, String s) {
		x *= scalingFactor;
		y *= scalingFactor;
		if (s.indexOf("?") > 0)
			s = s.substring(s.indexOf("?") + 1);
		s = s.substring(s.lastIndexOf("/") + 1);
		s = s.substring(s.lastIndexOf("\\") + 1);
		g2d.setGraphicsColor(g, BLACK);
		JmolFont font = setFont(g, 1000, JmolFont.FONT_STYLE_PLAIN, 9, true);
		if (x != left * scalingFactor)
			x -= font.stringWidth(s);
		g2d.drawString(g, s, x, y - font.getHeight());
	}

	public void printVersion(Object g, int pageHeight) {
		g2d.setGraphicsColor(g, BLACK);
		JmolFont font = setFont(g, 100, JmolFont.FONT_STYLE_PLAIN, 9, true);
		String s = jsvp.getApiPlatform().getDateFormat() + " JSpecView "
				+ JSVersion.VERSION_SHORT;
		int w = font.stringWidth(s);
		g2d.drawString(g, s, (thisWidth - right) * scalingFactor - w, pageHeight
				* scalingFactor - font.getHeight());
	}

	/**
	 * Draws Title
	 * 
	 * @param g
	 * 
	 * @param pageHeight
	 *          the height to be drawn in pixels -- after scaling
	 * @param pageWidth
	 *          the width to be drawn in pixels -- after scaling
	 * @param title
	 */
	public void drawTitle(Object g, int pageHeight, int pageWidth, String title) {
		title = title.replace('\n', ' ');
		JmolFont font = getFont(g, pageWidth, isPrinting
				|| getBoolean(ScriptToken.TITLEBOLDON) ? JmolFont.FONT_STYLE_BOLD
				: JmolFont.FONT_STYLE_PLAIN, 14, true);
		int nPixels = font.stringWidth(title);
		if (nPixels > pageWidth) {
			int size = (int) (14.0 * pageWidth / nPixels);
			if (size < 10)
				size = 10;
			font = getFont(g, pageWidth, isPrinting
					|| getBoolean(ScriptToken.TITLEBOLDON) ? JmolFont.FONT_STYLE_BOLD
					: JmolFont.FONT_STYLE_PLAIN, size, true);
		}
		g2d.setGraphicsColor(g, titleColor);
		g2d.setGraphicsFont(g, font);
		g2d.drawString(g, title, (isPrinting ? left * scalingFactor : 5),
				pageHeight - (int) (font.getHeight() * (isPrinting ? 2 : 0.5)));
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
				if (iSpec < 0 || iSpec == pt)
					graphSets.get(i).setSelected(j);
		}
	}

	public void addToList(int iSpec, List<JDXSpectrum> list) {
		for (int i = 0; i < spectra.size(); i++)
			if (iSpec < 0 || i == iSpec)
				list.addLast(spectra.get(i));
	}

	public void scaleSelectedBy(double f) {
		for (int i = graphSets.size(); --i >= 0;)
			graphSets.get(i).scaleSelectedBy(f);

	}

	// //// currentGraphSet methods

	public void setCurrentGraphSet(GraphSet gs, int yPixel, int clickCount) {
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

	public int getStartingPointIndex(int index) {
		return currentGraphSet.viewData.getStartingPointIndex(index);
	}

	public int getEndingPointIndex(int index) {
		return currentGraphSet.viewData.getEndingPointIndex(index);
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
		JDXSpectrum spec0 = currentGraphSet.getSpectrum();
		currentGraphSet.setSpectrumJDX(spec);
		for (int i = 0; i < spectra.size(); i++)
			if (spectra.get(i) == spec0)
				spectra.set(i, spec);
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
	 * @param a
	 * @param b
	 * @param g
	 */
	public void addHighlight(GraphSet gs, double x1, double x2, JDXSpectrum spec,
			int r, int g, int b, int a) {
		(gs == null ? currentGraphSet : gs).addHighlight(x1, x2, spec, g2d
				.getColor4(r, g, b, a));
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
		taintedAll = true;
		notifyListeners(new ZoomEvent(x1, y1, x2, y2));
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

	public void setXPointers(JDXSpectrum spec, double x1, JDXSpectrum spec2,
			double x2) {
		currentGraphSet.setXPointer(spec, x1);
		currentGraphSet.setXPointer2(spec2, x2);
	}

	// called by GraphSet

	public boolean isCurrentGraphSet(GraphSet graphSet) {
		return graphSet == currentGraphSet;
	}

	public void repaint() {
		jsvp.doRepaint();
	}

	public void setToolTipText(String s) {
		jsvp.setToolTipText(s);
	}

	public void setHighlightColor(GenericColor color) {
		jsvp.getPanelData().setColor(ScriptToken.HIGHLIGHTCOLOR, color);
	}

	String getInput(String message, String title, String sval) {
		return jsvp.getInput(message, title, sval);
	}

	private JmolFont getFont(Object g, int width, int style, float size,
			boolean isLabel) {
		size *= scalingFactor;
		if (isLabel) {
			if (width < 400)
				size = ((width * size) / 400);
		} else {
			if (width < 250)
				size = ((width * size) / 250);
		}
		int face = jsvp.getFontFaceID(isPrinting ? printingFontName
				: displayFontName);
		return JmolFont.createFont3D(face, style, size, size, jsvp.getApiPlatform(), g);
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
	public void notifySubSpectrumChange(int isub, JDXSpectrum spec) {
		notifyListeners(new SubSpecChangeEvent(isub, (spec == null ? null : spec
				.getTitleLabel())));
	}

	/**
	 * Notifies CoordinatePickedListeners
	 * 
	 * @param p
	 * 
	 */
	public void notifyPeakPickedListeners(PeakPickEvent p) {
		if (p == null) {
			p = new PeakPickEvent(jsvp, coordClicked, getSpectrum()
					.getAssociatedPeakInfo(xPixelClicked, coordClicked));
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

	public void notifyListeners(Object eventObj) {
		for (int i = 0; i < listeners.size(); i++)
			if (listeners.get(i) != null)
				listeners.get(i).panelEvent(eventObj);
	}

	/*--------------the rest are all mouse and keyboard interface -----------------------*/

	public void escapeKeyPressed(boolean isDEL) {
		currentGraphSet.escapeKeyPressed(isDEL);
	}

	private enum Mouse {
		UP, DOWN;
	}

	private Mouse mouseState;
	public boolean gridOn;
	public boolean titleOn;

	public boolean hasFocus() {
		return jsvp.hasFocus();
	}

	public boolean isMouseUp() {
		return (mouseState == Mouse.UP);
	}

	public int mouseX, mouseY;

	public void doMouseMoved(int xPixel, int yPixel) {
		mouseX = xPixel;
		mouseY = yPixel;
		mouseState = Mouse.UP;
		clickCount = 0;
		GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
		if (gs == null)
			return;
		gs.mouseMovedEvent(xPixel, yPixel);
	}

	public void doMousePressed(int xPixel, int yPixel) {
		mouseState = Mouse.DOWN;
		GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
		if (gs == null)
			return;
		setCurrentGraphSet(gs, yPixel, 0);
		clickCount = (++clickCount % 3);
		gs.checkWidgetEvent(xPixel, yPixel, true);
	}

	public void doMouseDragged(int xPixel, int yPixel) {
		isIntegralDrag |= ctrlPressed;
		mouseState = Mouse.DOWN;
		if (GraphSet.findGraphSet(graphSets, xPixel, yPixel) != currentGraphSet)
			return;
		currentGraphSet.checkWidgetEvent(xPixel, yPixel, false);
		currentGraphSet.mouseMovedEvent(xPixel, yPixel);
	}

	public void doMouseReleased(int xPixel, int yPixel, boolean isButton1) {
		mouseState = Mouse.UP;
		if (thisWidget == null && currentGraphSet.pendingMeasurement == null || !isButton1)
			return;
		currentGraphSet.mouseReleasedEvent(xPixel, yPixel);
		thisWidget = null;
		isIntegralDrag = false;
		integralShiftMode = 0;
		// repaint();
	}

	public void doMouseClicked(int xPixel, int yPixel,
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

	public void removeDialog(JSVDialog dialog) {
		currentGraphSet.removeDialog(dialog);
	}

	public void normalizeIntegral() {
		Integral integral = getSelectedIntegral();
		if (integral == null)
			return;
		String sValue = integral.text;
		if (sValue.length() == 0)
			sValue = "" + integral.getValue();
		String newValue = getInput("Enter a new value for this integral",
				"Normalize Integral", sValue);
		try {
			setSelectedIntegral(Double.parseDouble(newValue));
		} catch (Exception e) {
		}
	}

	public String getDrawTitle(boolean isPrinting) {
		String title = null;
		if (isPrinting)
			title = printJobTitle;
		else if (nSpectra == 1) {
			title = getSpectrum().getPeakTitle();
		} else if (viewTitle != null) {
			if (currentGraphSet.getTitle(false) != null) // check if common title
				title = getSpectrum().getPeakTitle();
			if (title == null)
				title = viewTitle; // "View 1"
		} else {
			title = jsvp.getTitle().trim();
		}
		if (title.indexOf("\n") >= 0)
			title = title.substring(0, title.indexOf("\n")).trim();
		return title;
	}

	public String getPrintJobTitle(boolean isPrinting) {
		String title = null;
		if (nSpectra == 1) {
			title = getSpectrum().getTitle();
		} else if (viewTitle != null) {
			if (graphSets.size() == 1)
				title = currentGraphSet.getTitle(isPrinting);
			if (title == null)
				title = viewTitle; // "View 1"
		} else {
			title = jsvp.getTitle().trim();
		}
		if (title.indexOf("\n") >= 0)
			title = title.substring(0, title.indexOf("\n")).trim();
		return title;
	}

	public synchronized void linkSpectra(LinkMode mode) {
		if (mode == LinkMode.ALL)
			mode = (nSpectra == 2 ? LinkMode.AB : nSpectra == 3 ? LinkMode.ABC
					: LinkMode.NONE);
		if (mode != LinkMode.NONE && mode.toString().length() != nSpectra)
			return;
		setGraphSets(mode);
	}

	private boolean linking;
	public int xPixelClicked;

	public void doZoomLinked(GraphSet graphSet, double initX, double finalX,
			boolean addZoom, boolean checkRange, boolean is1d) {
		if (linking)
			return;
		linking = true;
		JDXSpectrum spec = graphSet.getSpectrumAt(0);
		for (int i = graphSets.size(); --i >= 0;) {
			GraphSet gs = graphSets.get(i);
			if (gs != graphSet
					&& JDXSpectrum.areXScalesCompatible(spec, graphSets.get(i)
							.getSpectrumAt(0), false, true))
				gs.doZoom(initX, 0, finalX, 0, is1d, false, checkRange, false, addZoom);
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
			if (gs != graphSet
					&& JDXSpectrum.areXScalesCompatible(spec, graphSets.get(i)
							.getSpectrum(), false, true))
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
			if (gs != graphSet
					&& JDXSpectrum.areXScalesCompatible(spec, graphSets.get(i)
							.getSpectrum(), false, true)) {
				if (gs.imageView == null)
					if (isX2) {
						gs.setXPixelMovedTo(Double.MAX_VALUE, x, 0, 0);
					} else {
						gs.setXPixelMovedTo(x, Double.MAX_VALUE, 0, 0);
					}
			}
		}
		linking = false;
	}

	public void set2DCrossHairsLinked(GraphSet graphSet, double x, double y,
			boolean isLocked) {
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

	// //////// settable colors //////////

	public GenericColor coordinatesColor;
	public GenericColor gridColor;
	public GenericColor integralPlotColor;
	public GenericColor peakTabColor;
	public GenericColor plotAreaColor;
	public GenericColor scaleColor;
	public GenericColor titleColor;
	public GenericColor unitsColor;
	// potentially settable;

	public GenericColor highlightColor;
	public GenericColor zoomBoxColor;
	public GenericColor zoomBoxColor2;
	public GenericColor BLACK;
	public GenericColor bgcolor;

	public void setColor(ScriptToken st, GenericColor color) {
		if (color != null)
			options.put(st, JSVColorUtil.colorToHexString(color));
		switch (st) {
		case BACKGROUNDCOLOR:
			jsvp.setBackgroundColor(bgcolor = color);
			break;
		case COORDINATESCOLOR:
			coordinatesColor = color;
			break;
		case GRIDCOLOR:
			gridColor = color;
			break;
		case HIGHLIGHTCOLOR:
			highlightColor = color;
			if (highlightColor.getOpacity255() == 255)
				highlightColor.setOpacity255(150);
			break;
		case INTEGRALPLOTCOLOR:
			integralPlotColor = color;
			break;
		case PEAKTABCOLOR:
			peakTabColor = color;
			break;
		case PLOTCOLOR:
			for (int i = graphSets.size(); --i >= 0;)
				graphSets.get(i).setPlotColor0(color);
			break;
		case PLOTAREACOLOR:
			plotAreaColor = color;
			break;
		case SCALECOLOR:
			scaleColor = color;
			break;
		case TITLECOLOR:
			titleColor = color;
			break;
		case UNITSCOLOR:
			unitsColor = color;
			break;
		case ZOOMBOXCOLOR:
			zoomBoxColor = color;
			break;
		case ZOOMBOXCOLOR2:
			zoomBoxColor2 = color;
			break;
		default:
			Logger.warn("AwtPanel --- unrecognized color: " + st);
			break;
		}
	}

	public GenericColor getColor(ScriptToken whatColor) {
		switch (whatColor) {
		default:
			Logger.error("awtgraphset missing color " + whatColor);
			return BLACK;
		case ZOOMBOXCOLOR2:
			return zoomBoxColor2;
		case ZOOMBOXCOLOR:
			return zoomBoxColor;
		case HIGHLIGHTCOLOR:
			return highlightColor;
		case INTEGRALPLOTCOLOR:
			return integralPlotColor;
		case GRIDCOLOR:
			return gridColor;
		case PEAKTABCOLOR:
			return peakTabColor;
		case PLOTAREACOLOR:
			return plotAreaColor;
		case SCALECOLOR:
			return scaleColor;
		case TITLECOLOR:
			return titleColor;
		case UNITSCOLOR:
			return unitsColor;
		}
	}

	public Object[][] getOverlayLegendData() {
		int numSpectra = currentGraphSet.nSpectra;
		Object[][] data = new Object[numSpectra][];
		String f1 = getSpectrumAt(0).getFilePath();
		String f2 = getSpectrumAt(numSpectra - 1).getFilePath();
		boolean useFileName = !f1.equals(f2);

		for (int index = 0; index < numSpectra; index++) {
			Object[] cols = new Object[3];
			JDXSpectrum spectrum = getSpectrumAt(index);
			title = spectrum.getTitle();
			if (useFileName)
				title = JSVFileManager.getName(spectrum.getFilePath()) + " - " + title;
			GenericColor plotColor = getCurrentPlotColor(index);
			cols[0] = new Integer(index + 1);
			cols[1] = plotColor;
			cols[2] = " " + title;
			data[index] = cols;
		}
		return data;
	}

	@SuppressWarnings("incomplete-switch")
	public void setColorOrFont(ColorParameters ds, ScriptToken st) {
		if (st == null) {
			Map<ScriptToken, GenericColor> colors = ds.elementColors;
			for (Map.Entry<ScriptToken, GenericColor> entry : colors.entrySet())
				setColorOrFont(ds, entry.getKey());
			setColorOrFont(ds, ScriptToken.DISPLAYFONTNAME);
			setColorOrFont(ds, ScriptToken.TITLEFONTNAME);
			return;
		}
		switch (st) {
		case DISPLAYFONTNAME:
			setFontName(st, ds.displayFontName);
			return;
		case TITLEFONTNAME:
			setFontName(st, ds.titleFontName);
			return;
		}
		setColor(st, ds.getElementColor(st));
	}

	public GenericColor getCurrentPlotColor(int i) {
		return currentGraphSet.getPlotColor(i);
	}

	public Hashtable<ScriptToken, Object> optionsSaved;

	public void setPrint(PrintLayout pl, String fontName) {
		if (pl == null) {
			options.putAll(optionsSaved);
			optionsSaved = null;
			return;
		}
		printJobTitle = pl.title;
		// Set Graph Properties
		printingFontName = fontName;
		printGraphPosition = pl.position;

		optionsSaved = new Hashtable<ScriptToken, Object>();
		optionsSaved.putAll(options);
		
		gridOn = pl.showGrid;
		titleOn = pl.showTitle;
		setBoolean(ScriptToken.XSCALEON, pl.showXScale);
		setBoolean(ScriptToken.XUNITSON, pl.showXScale);
		setBoolean(ScriptToken.YSCALEON, pl.showYScale);
		setBoolean(ScriptToken.YUNITSON, pl.showYScale);


		// TODO Auto-generated method stub
		
	}

	public JSVDialog showDialog(AType type) {
		AnnotationData ad = getDialog(type);
		closeAllDialogsExcept(type);
		if (ad != null && ad instanceof JSVDialog)
			return ((JSVDialog) ad).reEnable();
		int iSpec = getCurrentSpectrumIndex();
		if (iSpec < 0) {
			jsvp.showMessage("To enable " + type + " first select a spectrum by clicking on it.", "" + type);
			return null;
		}
		JDXSpectrum spec = getSpectrum();
		JSVDialog dialog = viewer.getDialog(type, spec);
		if (ad == null && type == AType.Measurements)
			ad = new MeasurementData(AType.Measurements, spec);
		if (ad != null)
			dialog.setData(ad);
		addDialog(iSpec, type, dialog);
		dialog.reEnable();
		return dialog;
	}

	public int print(Object g, double height, double width,
			double x, double y, double paperHeight, double paperWidth, 
			boolean isPortrait, int pi) {
    if (pi == 0) {
      isPrinting = true;
      boolean addFilePath = false;
      if (printGraphPosition.equals("default")) {
        if (isPortrait) {
          height = defaultPrintHeight;
          width = defaultPrintWidth;
        } else {
          height = defaultPrintWidth;
          width = defaultPrintHeight;
        }
      } else if (printGraphPosition.equals("fit to page")) {
        addFilePath = true;
      } else { // center
        if (isPortrait) {
          height = defaultPrintHeight;
          width = defaultPrintWidth;
          x = (int) (paperWidth - width) / 2;
          y = (int) (paperHeight - height) / 2;
        } else {
          height = defaultPrintWidth;
          width = defaultPrintHeight;
          y = (int) (paperWidth - defaultPrintWidth) / 2;
          x = (int) (paperHeight - defaultPrintHeight) / 2;
        }
      }
      g2d.translateScale(g, x, y, 0.1);
      taintedAll = true;
      drawGraph(g, g, (int) width, (int) height, addFilePath);
      isPrinting = false;
      return JSViewer.PAGE_EXISTS;
    }
    isPrinting = false;
    return JSViewer.NO_SUCH_PAGE;
	}

  /*--------------mouse and keyboard interface -----------------------*/

	public boolean keyPressed(int code, int modifiers) {
		if (isPrinting)
			return false;
		checkKeyControl(code, true);
		// should be only in panel region, though.
		switch (code) {
		case Event.VK_ESCAPE:
		case Event.VK_DELETE:
		case Event.VK_BACK_SPACE: // Mac
			escapeKeyPressed(code != Event.VK_ESCAPE);
			isIntegralDrag = false;
			repaint();
			return true;
		}
		double scaleFactor = 0;
		boolean doConsume = false;
		if (modifiers == 0) {
			switch (code) {
			case Event.VK_LEFT:
			case Event.VK_RIGHT:
				doMouseMoved((code == Event.VK_RIGHT ? ++mouseX : --mouseX), mouseY);
				repaint();
				doConsume = true;
				break;
			case Event.VK_PAGE_UP:
			case Event.VK_PAGE_DOWN:
				scaleFactor = (code == Event.VK_PAGE_UP ? GraphSet.RT2
						: 1 / GraphSet.RT2);
				doConsume = true;
				break;
			case Event.VK_DOWN:
			case Event.VK_UP:
				int dir = (code == Event.VK_DOWN ? -1 : 1);
				if (getSpectrumAt(0).getSubSpectra() == null) {
					notifySubSpectrumChange(dir, null);
				} else {
					advanceSubSpectrum(dir);
					repaint();
				}
				doConsume = true;
				break;
			}
		} else if (checkMod(code, Event.CTRL_MASK)) {
			switch (code) {
			case Event.VK_DOWN:
			case Event.VK_UP:
			case 45: // '-'
			case 61: // '=/+'
				scaleFactor = (code == 61 || code == Event.VK_UP ? GraphSet.RT2
						: 1 / GraphSet.RT2);
				doConsume = true;
				break;
			case Event.VK_LEFT:
			case Event.VK_RIGHT:
				toPeak(code == Event.VK_RIGHT ? 1 : -1);
				doConsume = true;
				break;
			}
		}
		if (scaleFactor != 0) {
			scaleYBy(scaleFactor);
			repaint();
		}
		return doConsume;
	}

	public void keyReleased(int keyCode) {
		if (isPrinting)
			return;
		checkKeyControl(keyCode, false);
	}

	public boolean keyTyped(int ch, int mods) {
		if (isPrinting)
			return false;
		switch (ch) {
		case 'n':
			if (mods != 0)
				break;
			normalizeIntegral();
			return true;
		case 26: // ctrl-Z
			if (mods != Event.CTRL_MASK)
				break;
			previousView();
			repaint();
			return true;
		case 25: // ctrl-y
			if (mods != Event.CTRL_MASK)
				break;
			nextView();
			repaint();
			return true;
		}
		return false;
	}

	public void mouseAction(int mode, long time, int x, int y, int countIgnored,
			int buttonMods) {
		if (isPrinting)
			return;
		switch (mode) {
		case Event.PRESSED:
			if (!checkMod(buttonMods, Event.MOUSE_LEFT))
				return;
			doMousePressed(x, y);
			break;
		case Event.RELEASED:
			doMouseReleased(x, y, checkMod(buttonMods, Event.MOUSE_LEFT));
			repaint();
			break;
		case Event.DRAGGED:
			doMouseDragged(x, y);
			repaint();
			break;
		case Event.MOVED:
	    jsvp.getFocusNow(false);
			if ((buttonMods & Event.BUTTON_MASK) != 0) {
				doMouseDragged(x, y);
				repaint();
				return;
			}
			doMouseMoved(x, y);
			if (coordStr != null)
				repaint();
			break;
		case Event.CLICKED:
	    if (checkMod(buttonMods, Event.MOUSE_RIGHT)) {
	    	viewer.showMenu(x, y);
	      return;
	    }
			ctrlPressed = false;
			doMouseClicked(x, y, updateControlPressed(buttonMods));
			break;
		}
	}

	public boolean checkMod(int buttonMods, int mask) {
		return ((buttonMods & mask) == mask);
	}

	public void checkKeyControl(int keyCode, boolean isPressed) {
		switch (keyCode) {
		case Event.VK_CONTROL:
		case Event.VK_META:
			ctrlPressed = isPressed;
			break;
		case Event.VK_SHIFT:
			shiftPressed = isPressed;
			break;
		default:
			ctrlPressed = updateControlPressed(keyCode);
			shiftPressed = checkMod(keyCode, Event.SHIFT_MASK);
		}
	}

	public boolean updateControlPressed(int mods) {
		// Mac does not allow Ctrl-drag. The CMD key is indicated using code 157
		return (ctrlPressed |= checkMod(mods, Event.CTRL_MASK)
				|| checkMod(mods, Event.MAC_COMMAND));
	}

	public void mouseEnterExit(long time, int x, int y, boolean isExit) {
		if (isExit) {
			thisWidget = null;
			isIntegralDrag = false;
			integralShiftMode = 0;
		} else {
	    jsvp.getFocusNow(false);
		}			
	}


}
