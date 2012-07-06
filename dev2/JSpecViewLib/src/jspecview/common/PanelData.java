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

	private Map<String, NumberFormat> htFormats = new Hashtable<String, NumberFormat>();

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
    htFormats = null;
    coordClicked = null;
    coordsClicked = null;
    thisWidget = null;
    options = null;
    listeners = null;
  }

  // plot parameters

  int defaultHeight = 450;
  int defaultWidth = 280;
  int leftPlotAreaPos = 80, topPlotAreaPos = 30;
  int plotAreaWidth, plotAreaHeight;
  int left = leftPlotAreaPos, right = 50, top = topPlotAreaPos,
      bottom = 50;

  // current values

  boolean drawXAxisLeftToRight;
  boolean xAxisLeftToRight = true;
  boolean isIntegralDrag;

  int nSpectra;
  int thisWidth;
  int thisPlotHeight;

  String coordStr = "";
  String startupPinTip = "Click to set.";
  String title;

  void setTitle(String title) {
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

  void setBoolean(Parameters parameters, ScriptToken st) {
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

  /////////// print parameters ///////////

  boolean isPrinting;
  String printingFont;
  String printGraphPosition = "default";
  boolean titleDrawn;
  boolean display1D;

  public boolean getDisplay1D() {
    return display1D;
  }


  ////// initialization - from AwtPanel
  
  void initSingleSpectrum(JDXSpectrum spectrum) {
    List<JDXSpectrum> spectra = new ArrayList<JDXSpectrum>();
    spectra.add(spectrum);
    initJSVPanel(spectra, 0, 0);
  }

  void initJSVPanel(List<JDXSpectrum> spectra, int startIndex, int endIndex) {
    owner.setupPlatform();
    nSpectra = spectra.size();
    graphSets = GraphSet.createGraphSets(owner, spectra, startIndex, endIndex);
    currentGraphSet = graphSets.get(0);
    //System.out.println("PanelData.initJSVPanel " + currentGraphSet);
    setTitle(getSpectrum().getTitleLabel());
  }

  public PeakInfo findMatchingPeakInfo(PeakInfo pi) {
    PeakInfo pi2 = null;
    for (int i = 0; i < graphSets.size(); i++)
      if ((pi2 = graphSets.get(i).findMatchingPeakInfo(pi)) != null)
        break;
    return pi2;
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
		PeakInfo pi = currentGraphSet.selectPeakByFileIndex(filePath, index);
		if (pi == null)
			for (int i = graphSets.size(); --i >= 0;)
				if (graphSets.get(i) != currentGraphSet
						&& (pi = graphSets.get(i).selectPeakByFileIndex(filePath, index)) != null)
					break;
		return pi;
	}

	public void selectSpectrum(String filePath, String type, String model, boolean andCurrent) {
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
   *        the <code>Graphics</code> object
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  void drawGraph(Object g, int height, int width) {

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

	/**
	 * sets bsSelected to the specified pointer
	 * from "select 3.1*1"
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

////// currentGraphSet methods
  
  void setCurrentGraphSet(GraphSet gs, int xPixel, int yPixel, int clickCount) {
  	int splitPoint = gs.getSplitPoint(yPixel);
  	boolean isNewSet = (currentGraphSet != gs);
  	boolean isNewSplitPoint = (isNewSet || currentSplitPoint != splitPoint);
    currentGraphSet = gs;
    currentSplitPoint = splitPoint;
//  	if (isNewSet)
//      System.out.println("setting currentGraphSet to " + gs);
//  	if (isNewSplitPoint)
//      System.out.println("setting currentSplitPoint to " + splitPoint);
  	if (!isNewSet) {
    	if (gs.nSplit == 1) {
    		if (clickCount == 0 || !gs.checkSpectrumClickedEvent(xPixel, yPixel, clickCount))
    			return;
    	} else if (!isNewSplitPoint) {
        return;
    	}
    }
  	
  	// new set (so also new split point)
  	// or nSplit > 1 and new split point
  	// or nSplit == 1 and showAllStacked and isClick and is a spectrum click)
  	
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
    return currentGraphSet.view.startDataPointIndices;
  }

  public int[] getEndDataPointIndices() {
    return currentGraphSet.view.endDataPointIndices;
  }

  public String getSolutionColor() {
    return currentGraphSet.getSolutionColor();
  }

	public boolean haveSelectedSpectrum() {
  	return currentGraphSet.haveSelectedSpectrum();
	}

	public boolean getShowIntegration() {
  	return currentGraphSet.getShowIntegration();
	}

	public void setShowIntegration(Boolean tfToggle) {
    currentGraphSet.setShowIntegration(tfToggle);
	}

  public void setYStackOffsetPercent(int offset) {
  	currentGraphSet.yStackOffsetPercent = offset;
  }

  public void setSpectrum(int iSpec, boolean isSplit) {
  	currentGraphSet.setSpectrum(iSpec, isSplit);
    refresh();
	}
  
	public JDXSpectrum getSpectrum() {
    return currentGraphSet.getSpectrum();
  }

  public void setSpectrum(JDXSpectrum spec) {
    currentGraphSet.setSpectrum(spec);
  }

  public boolean isOverlaid() {
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
    currentGraphSet.removeAllHighlights();
  }

  public void setZoom(double x1, double y1, double x2, double y2) {
    currentGraphSet.setZoom(x1, y1, x2, y2);
    thisWidth = 0;
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

  public int getSelectedIntegral() {
  	return currentGraphSet.getSelectedIntegral();
  }
  
  public String getSelectedIntegralText() {
  	return currentGraphSet.getSelectedIntegralText();
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

  String getSolutionColorHtml() {
    String color = currentGraphSet.getSolutionColor();
    return "<HTML><body bgcolor=rgb(" + color
        + ")><br />Predicted Solution Colour- RGB(" + color
        + ")<br /><br /></body></HTML>";
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
    return Coordinate.getPickedCoordinates(coordsClicked, coordClicked, coord,
        actualCoord);
  }

  // called by GraphSet
  
	boolean isCurrentGraphSet(GraphSet graphSet) {
		return graphSet == currentGraphSet;
	}

  final static DecimalFormat SCI_FORMATTER = TextFormat.getDecimalFormat("0.00E0");

  NumberFormat getFormatter(String hash) {
    NumberFormat formatter = htFormats.get(hash);
    if (formatter == null)
      htFormats.put(hash, formatter = (hash.equals("") ? 
      		SCI_FORMATTER : TextFormat.getDecimalFormat(hash)));
    return formatter;
  }


  void repaint() {
    owner.repaint();
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
    owner.setFont(g, (isPrinting ? printingFont : displayFontName), width, mode, size, isLabel);
  }

  // listeners to handle various events, from GraphSet or AwtPanel

  /**
   * 
   * @param isub
   *        -1 indicates direction if no subspectra or subspectrum index if
   *        subspectra
   * @param spec
   *        null indicates no subspectra
   */
  void notifySubSpectrumChange(int isub, JDXSpectrum spec) {
    notifyListeners(new SubSpecChangeEvent(isub, (spec == null ? null : spec
        .getTitleLabel())));
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
//		PeakInfo pi = p.getPeakInfo();
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

	void escapeKeyPressed() {
		currentGraphSet.escapeKeyPressed();
	}

  private enum Mouse {
    UP, DOWN;
  }
  
  private Mouse mouseState;
  boolean gridOn;
  boolean titleOn;
  
  boolean isMouseUp() {
    return (mouseState  == PanelData.Mouse.UP);
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

  void doMousePressed(int xPixel, int yPixel, boolean isControlDown, boolean isShiftDown) {
    mouseState = Mouse.DOWN;
    GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
    if (gs == null)
      return;
    isIntegralDrag = (isControlDown && gs.getSpectrum().hasIntegral());
    if (isControlDown && !isIntegralDrag)
      return;
    setCurrentGraphSet(gs, xPixel, yPixel, 0);
    gs.checkWidgetEvent(xPixel, yPixel, true, ++clickCount);
  }

  private int clickCount;
	boolean ctrlPressed;
  
  void doMouseDragged(int xPixel, int yPixel) {
    mouseState = Mouse.DOWN;
    if (GraphSet.findGraphSet(graphSets, xPixel, yPixel) != currentGraphSet)
      return;
    currentGraphSet.checkWidgetEvent(xPixel, yPixel, false, clickCount);
    currentGraphSet.mouseMovedEvent(xPixel, yPixel);
  }

  void doMouseReleased(boolean isButton1) {
    mouseState = Mouse.UP;
    if (thisWidget == null || !isButton1)
      return;
    currentGraphSet.mouseReleasedEvent();
    thisWidget = null;
  }

  void doMouseClicked(int xPixel, int yPixel, int clickCount,
                                boolean isControlDown) {
    GraphSet gs = GraphSet.findGraphSet(graphSets, xPixel, yPixel);
    if (gs == null)
      return;
    setCurrentGraphSet(gs, xPixel, yPixel, clickCount);
    gs.mouseClickedEvent(xPixel, yPixel, clickCount, isControlDown);
  }

}
