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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import jspecview.util.TextFormat;

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

  JSVPanel owner;
  private ArrayList<PanelListener> listeners = new ArrayList<PanelListener>();
  List<GraphSet> graphSets;
  GraphSet currentGraphSet;
	int currentSplitPoint;
  private Map<String, NumberFormat> htFormats = new Hashtable<String, NumberFormat>();
  PlotWidget thisWidget;
  Coordinate coordClicked;
  Coordinate[] coordsClicked;
  Hashtable<ScriptToken, Object> options = new Hashtable<ScriptToken, Object>();

  public void dispose() {
    owner = null;
    for (int i = 0; i < graphSets.size(); i++)
      graphSets.get(i).dispose();
    graphSets = null;
    currentGraphSet = null;
    htFormats = null;
    coordClicked = null;
    coordsClicked = null;
    thisWidget = null;
    options = null;
    listeners = null;
  }

  // plot parameters

  protected int defaultHeight = 450;
  protected int defaultWidth = 280;
  protected int leftPlotAreaPos = 80, topPlotAreaPos = 30;
  protected int plotAreaWidth, plotAreaHeight;
  protected int left = leftPlotAreaPos, right = 50, top = topPlotAreaPos,
      bottom = 50;

  // current values

  protected boolean drawXAxisLeftToRight;
  protected boolean xAxisLeftToRight = true;
  boolean isIntegralDrag;
  protected boolean isOverlaid;

  public boolean isOverlaid() {
    return isOverlaid;
  }

  protected int nSpectra;
  int thisWidth;
  int thisPlotHeight;

  String coordStr = "";
  String startupPinTip = "Click to set.";
  protected String title;

  public void setTitle(String title) {
    this.title = title;
  }

  public Map<String, Object> getInfo(boolean isSelected, String key) {
    Map<String, Object> info = new Hashtable<String, Object>();
    Set<Entry<ScriptToken, Object>> entries = options.entrySet();
    for (Entry<ScriptToken, Object> entry : entries)
      JDXSpectrum.putInfo(key, info, entry.getKey().name(), entry.getValue());
    JDXSpectrum.putInfo(key, info, "selected", Boolean.valueOf(isSelected));
    JDXSpectrum.putInfo(key, info, "type", getSpectrumAt(0).getDataType());
    JDXSpectrum.putInfo(key, info, "title", title);
    JDXSpectrum.putInfo(key, info, "nSets", Integer.valueOf(graphSets.size()));
    List<Map<String, Object>> sets = new ArrayList<Map<String, Object>>();
    for (int i = graphSets.size(); --i >= 0;)
      sets.add(graphSets.get(i).getInfo(key,
          isSelected && graphSets.get(i) == currentGraphSet));
    info.put("sets", sets);
    return info;
  }

  public void setBoolean(Parameters parameters, ScriptToken st) {
    if (st == null) {
      Map<ScriptToken, Boolean> booleans = parameters.getBooleans();
      for (Map.Entry<ScriptToken, Boolean> entry : booleans.entrySet())
        setBoolean(parameters, entry.getKey());
      return;
    }
    Boolean isTrue = parameters.getBoolean(st);
    setBoolean(st, isTrue);
  }

  public void setBoolean(ScriptToken st, Boolean isTrue) {
    if (st == ScriptToken.REVERSEPLOT) {
      currentGraphSet.setReversePlot(isTrue);
      return;
    }
    options.put(st, isTrue);
    switch (st) {
    case DISPLAY1D:
    case DISPLAY2D:
      thisWidth = 0;
      break;
    }
  }

  public boolean getBoolean(ScriptToken st) {
    if (st == ScriptToken.REVERSEPLOT)
      return currentGraphSet.reversePlot;
    Object b = options.get(st);
    return (b != null && (b instanceof Boolean) && ((Boolean) b) == Boolean.TRUE);
  }

  ////////// settable colors and fonts //////////

  private String displayFontName;
  private String titleFontName;


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

  /////////// print parameters ///////////

  boolean isPrinting;
  String printingFont;
  protected String printGraphPosition = "default";
  boolean titleDrawn;
  boolean display1D;
  int yStackOffsetPercent;
  
  public void setYStackOffsetPercent(int offset) {
	yStackOffsetPercent = offset;
  }
  
  public boolean getDisplay1D() {
    return display1D;
  }


  // listeners to handle various events

  protected void initSingleSpectrum(JDXSpectrum spectrum) {
    List<JDXSpectrum> spectra = new ArrayList<JDXSpectrum>();
    spectra.add(spectrum);
    initJSVPanel(spectra, 0, 0);
  }

  protected void initJSVPanel(List<JDXSpectrum> spectra, int startIndex, int endIndex) {
    owner.setupPlatform();
    nSpectra = spectra.size();
    graphSets = GraphSet.getGraphSets(owner, spectra, startIndex, endIndex);
    currentGraphSet = graphSets.get(0);
    System.out.println("PanelData.initJSVPanel " + currentGraphSet);
    setTitle(getSpectrum().getTitleLabel());
  }

  protected void setCurrentGraphSet(GraphSet gs, int xPixel, int yPixel, boolean isClick) {
  	int splitPoint = gs.getSplitPoint(yPixel);
  	boolean isNewSet = (currentGraphSet != gs);
  	boolean isNewSplitPoint = (isNewSet || currentSplitPoint != splitPoint);
    currentGraphSet = gs;
    currentSplitPoint = splitPoint;
  	if (isNewSet)
      System.out.println("setting currentGraphSet to " + gs);
  	if (isNewSplitPoint)
      System.out.println("setting currentSplitPoint to " + splitPoint);
  	if (!isNewSet) {
    	if (gs.nSplit == 1) {
    		if (!isClick || !gs.checkSpectrumClickEvent(xPixel, yPixel))
    			return;
    	} else if (!isNewSplitPoint) {
        return;
    	}
    }
  	
  	// new set (so also new split point)
  	// or nSplit > 1 and new split point
  	// or nSplit == 1 and showAllStacked and isClick and is a spectrum click)
  	
    if (gs.nSplit > 1)
    	gs.setSpectrumSelected(currentSplitPoint);
    JDXSpectrum spec = gs.getSpectrum();
    notifySubSpectrumChange(spec.getSubIndex(), spec);
    refresh();
  }

  public JDXSpectrum getSpectrum() {
    return currentGraphSet.getSpectrum();
  }

  public void setSpectrum(JDXSpectrum spec) {
    currentGraphSet.setSpectrum(spec);
  }


  /**
   * Returns the <code>Spectrum</code> at the specified index
   * 
   * @param index
   *        the index of the <code>Spectrum</code>
   * @return the <code>Spectrum</code> at the specified index
   */
  public JDXSpectrum getSpectrumAt(int index) {
    return currentGraphSet.getSpectrumAt(index);
  }

  /* -------------------Other methods ------------------------------------*/

  /**
   * Add information about a region of the displayed spectrum to be highlighted
   * applet only right now
   * 
   * @param x1
   *        the x value of the coordinate where the highlight should start
   * @param x2
   *        the x value of the coordinate where the highlight should end
   * @param color
   *        the color of the highlight
   * @param a 
   * @param b 
   * @param g 
   */
  public void addHighlight(GraphSet gs, double x1, double x2, JDXSpectrum spec, int r, int g, int b, int a) {
    (gs == null ? currentGraphSet : gs).addHighlight(x1, x2, spec, owner.getColor(r, g, b, a));
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
    currentGraphSet.removeHighlight(index);
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
    currentGraphSet.removeHighlight(x1, x2);
  }

  /**
   * Removes all highlights from the display
   */
  public void removeAllHighlights() {
    currentGraphSet.removeAllHighlights(null);
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

  public int getNumberOfSpectraInCurrentSet() {
    return currentGraphSet.nSpectra;
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
   * Return the start indices of the Scaledata
   * 
   * @return the start indices of the Scaledata
   */
  public int[] getStartDataPointIndices() {
    return currentGraphSet.view.startDataPointIndices;
  }

  /**
   * Return the end indices of the Scaledata
   * 
   * @return the end indices of the Scaledata
   */
  public int[] getEndDataPointIndices() {
    return currentGraphSet.view.endDataPointIndices;
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

  /*----------------------- JSVPanel PAINTING METHODS ---------------------*/
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
  protected void drawGraph(Object g, int height, int width) {

    boolean withCoords;
    display1D = getBoolean(ScriptToken.DISPLAY1D);
    if (isPrinting) {
      withCoords = false;
    } else {
      withCoords = getBoolean(ScriptToken.COORDINATESON);
      titleOn = getBoolean(ScriptToken.TITLEON);
      gridOn = getBoolean(ScriptToken.GRIDON);
    }
    plotAreaWidth = width - (right + left);
    plotAreaHeight = height - (top + bottom);
    boolean isResized = (thisWidth != width || thisPlotHeight != plotAreaHeight);
    if (isResized) {
      // this seems unnecessary, and it prevents focus returning to the script command line
      //owner.doRequestFocusInWindow();
    }
    thisWidth = width;
    thisPlotHeight = plotAreaHeight;
    titleDrawn = false;
    for (int i = graphSets.size(); --i >= 0;)
      graphSets.get(i)
          .drawGraph(g, height, width, left,
              right, top, bottom, isResized);
    if (titleOn && !titleDrawn)
      owner.drawTitle(g, height, width, getSpectrum().getPeakTitle());
    if (withCoords)
      owner.drawCoordinates(g, height, width);
  }

  final static DecimalFormat SCI_FORMATTER = TextFormat.getDecimalFormat("0.00E0");

  NumberFormat getFormatter(String hash) {
    NumberFormat formatter = htFormats.get(hash);
    if (formatter == null)
      htFormats.put(hash, formatter = (hash.equals("") ? 
      		SCI_FORMATTER : TextFormat.getDecimalFormat(hash)));
    return formatter;
  }

  void setFont(Object g, int width, int mode, int size, boolean isLabel) {
    if (isLabel) {
      if (width < 400)
        size = (int) ((width * size) / 400);
    } else {
      if (width < 250)
        size = (int) ((width * size) / 250);
    }
    owner.setFont(g, (isPrinting ? printingFont : displayFontName), mode, size);
  }

  /*-------------------- METHODS FOR SCALING AND ZOOM --------------------------*/

  public void setZoom(double x1, double y1, double x2, double y2) {
    currentGraphSet.setZoom(x1, y1, x2, y2);
    thisWidth = 0;
    notifyZoomListeners(x1, y1, x2, y2);
  }

  /**
   * Resets the spectrum to it's original view
   */
  public void clearViews() {
    currentGraphSet.reset();
  }

  /**
   * Clears all views in the zoom list
   */
  public void resetView() {
    for (int i = graphSets.size(); --i >= 0;)
      graphSets.get(i).clearViews();
  }

  public void fullView() {
    currentGraphSet.setZoomTo(0);
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
   * moving panel click event processing to JSVPanel from applet
   * 
   * @param coord
   * @param actualCoord
   * @return
   */
  public boolean getPickedCoordinates(Coordinate coord, Coordinate actualCoord) {
    return GraphSet.getPickedCoordinates(coordsClicked, coordClicked, coord,
        actualCoord);
  }

  public String getSolutionColorHtml() {
    String color = currentGraphSet.getSolutionColor();
    return "<HTML><body bgcolor=rgb(" + color
        + ")><br />Predicted Solution Colour- RGB(" + color
        + ")<br /><br /></body></HTML>";
  }



  public void refresh() {
    thisWidth = 0;
    owner.repaint();
  }

  public void addAnnotation(List<String> tokens) {
    String title = currentGraphSet.addAnnotation(tokens, getTitle());
    if (title != null)
      setTitle(title);
  }

  public void addPeakHighlight(PeakInfo peakInfo) {
    for (int i = 0; i < graphSets.size(); i++)
      graphSets.get(i).addPeakHighlight(peakInfo);
  }

	public PeakInfo selectPeakByFileIndex(String filePath, String index) {
		PeakInfo pi = (currentGraphSet == null ? null : 
			currentGraphSet.selectPeakByFileIndex(filePath, index));
		if (pi == null)
			for (int i = graphSets.size(); --i >= 0;)
				if (graphSets.get(i) != currentGraphSet 
						&& (pi = graphSets.get(i).selectPeakByFileIndex(filePath, index)) != null)
					break;
		return pi;
	}

	public void selectSpectrum(String filePath, String type, String model, boolean andCurrent) {
		if (andCurrent && currentGraphSet != null)
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
   * Adds a PanelListener
   * 
   * @param listener
   */
  public void addListener(PanelListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /**
   * 
   * @param isub
   *        -1 indicates direction if no subspectra or subspectrum index if
   *        subspectra
   * @param spec
   *        null indicates no subspectra
   */
  public void notifySubSpectrumChange(int isub, JDXSpectrum spec) {
    notifyListeners(new SubSpecChangeEvent(isub, (spec == null ? null : spec
        .getTitleLabel())));
  }

  /**
   * Notifies CoordinatePickedListeners
   * 
   * @param coord
   */
  public void notifyZoomListeners(double x1, double y1, double x2, double y2) {
    notifyListeners(new ZoomEvent(x1, y1, x2, y2));
  }

	/**
	 * Notifies CoordinatePickedListeners
	 * 
	 * @param coord
	 */
	void notifyPeakPickedListeners(PeakPickEvent p) {
		if (p == null) {
			p = new PeakPickEvent(owner, coordClicked, getSpectrum()
					.getAssociatedPeakInfo(coordClicked));
		}
		PeakInfo pi = p.getPeakInfo();
//		if (pi.getAtoms() == null) {
//			// find matching file/type/model in other panels
//			String filePath = pi.getFilePath();
//			String type = pi.getType();
//			String model = pi.getModel();
//			for (int i = 0; i < graphSets.size(); i++) {
//				if (graphSets.get(i) == currentGraphSet)
//					continue;
//				// just first spectrum for now -- presumed to be GC/MS
//				PeakInfo pi2 = graphSets.get(i).getSpectrumAt(0)
//						.selectPeakByFilePathTypeModel(filePath, type, model);
//				if (pi2 != null)
//					graphSets.get(i).addPeakHighlight(pi2);
//			}
//		}
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

  private enum Mouse {
    UP, DOWN;
  }
  
  private Mouse mouseState;
  public boolean gridOn;
  public boolean titleOn;
  
  boolean isMouseUp() {
    return (mouseState  == PanelData.Mouse.UP);
  }
  
  protected int mouseX, mouseY;
  protected void doMouseMoved(int xPixel, int yPixel) {
  	mouseX = xPixel;
  	mouseY = yPixel;
    mouseState = Mouse.UP;
    clickCount = 0;
    GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
    if (gs == null)
      return;
    gs.mouseMovedEvent(xPixel, yPixel);
  }

  protected void doMousePressed(int xPixel, int yPixel, boolean isControlDown, boolean isShiftDown) {
    mouseState = Mouse.DOWN;
    GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
    if (gs == null)
      return;
    isIntegralDrag = (isControlDown && gs.getSpectrum().hasIntegral());
    if (isControlDown && !isIntegralDrag)
      return;
    setCurrentGraphSet(gs, xPixel, yPixel, false);
    gs.checkWidgetEvent(xPixel, yPixel, true, ++clickCount);
  }

  int clickCount;
	boolean ctrlPressed;
  
  protected void doMouseDragged(int xPixel, int yPixel) {
    mouseState = Mouse.DOWN;
    if (GraphSet.findGraphSet(graphSets, xPixel, yPixel) != currentGraphSet)
      return;
    currentGraphSet.checkWidgetEvent(xPixel, yPixel, false, clickCount);
    currentGraphSet.mouseMovedEvent(xPixel, yPixel);
  }

  protected void doMouseReleased(boolean isButton1) {
    System.out.println("doMouseReleased " + this.clickCount);
    mouseState = Mouse.UP;
    if (thisWidget == null || !isButton1)
      return;
    currentGraphSet.mouseReleasedEvent();
    thisWidget = null;
  }

  protected void doMouseClicked(int xPixel, int yPixel, int clickCount,
                                boolean isControlDown) {
    GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
    if (gs == null)
      return;
    setCurrentGraphSet(gs, xPixel, yPixel, clickCount == 1);
    gs.mouseClickEvent(xPixel, yPixel, clickCount, isControlDown);
  }
  
  public void repaint() {
    owner.repaint();
  }

  public void setToolTipText(String s) {
    owner.setToolTipText(s);
  }

  public Object getHighlightColor() {
    return owner.getColor(ScriptToken.HIGHLIGHTCOLOR);
  }

  public void setHighlightColor(Object color) {
    owner.setColor(ScriptToken.HIGHLIGHTCOLOR, color);
  }

  public String getSolutionColor() {
    return currentGraphSet.getSolutionColor();
  }

  public PeakInfo findMatchingPeakInfo(PeakInfo pi) {
    PeakInfo pi2 = null;
    for (int i = 0; i < graphSets.size(); i++)
      if ((pi2 = graphSets.get(i).findMatchingPeakInfo(pi)) != null)
        break;
    return pi2;
  }

	public void splitStack(boolean doSplit) {
  	if (currentGraphSet != null)
    	currentGraphSet.splitStack(graphSets, doSplit);
	}

	public boolean haveSelectedSpectrum() {
  	if (currentGraphSet == null)
  		return false;
  	return currentGraphSet.haveSelectedSpectrum();
	}

	public boolean getShowIntegration() {
  	if (currentGraphSet == null)
  		return false;
  	return currentGraphSet.getShowIntegration();
	}

	public void setShowIntegration(Boolean tfToggle) {
  	if (currentGraphSet != null)
    	currentGraphSet.setShowIntegration(tfToggle);
	}

}
