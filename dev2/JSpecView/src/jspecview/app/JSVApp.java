/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General private
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General private License for more details.
 *
 *  You should have received a copy of the GNU Lesser General private
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

// CHANGES to 'JSVApplet.java' - Web Application GUI
// University of the West Indies, Mona Campus
//
// 09-10-2007 commented out calls for exporting data
//            this was causing security issues with JRE 1.6.0_02 and 03
// 13-01-2008 in-line load JCAMP-DX file routine added
// 22-07-2008 reinstated calls for exporting since Ok with JRE 1.6.0_05
// 25-07-2008 added module to predict colour of solution
// 08-01-2010 need bugfix for protected static reverseplot
// 17-03-2010 fix for NMRShiftDB CML files
// 11-06-2011 fix for LINK files and reverseplot 
// 23-07-2011 jak - Added parameters for the visibility of x units, y units,
//            x scale, and y scale.  Added parameteres for the font,
//            title font, and integral plot color.  Added a method
//            to reset view from a javascript call.
// 24-09-2011 jak - Added parameter for integration ratio annotations.
// 08-10-2011 jak - Add a method to toggle integration from a javascript
//          call. Changed behaviour to remove integration after reset
//          view.

package jspecview.app;

import java.net.URL;

import java.util.Map;

import javajs.util.List;

import org.jmol.util.Logger;

import jspecview.api.AppletFrame;
import jspecview.api.JSVAppInterface;
import jspecview.api.JSVPanel;
import jspecview.api.JSVTreeNode;
import jspecview.api.PanelListener;

import jspecview.common.JSVFileManager;
import jspecview.common.PanelNode;
import jspecview.common.JSViewer;
import jspecview.common.Parameters;
import jspecview.common.PeakPickEvent;
import jspecview.common.ScriptTokenizer;
import jspecview.common.ScriptToken;
import jspecview.common.Coordinate;
import jspecview.common.SubSpecChangeEvent;
import jspecview.common.ZoomEvent;

import jspecview.source.FileReader;
import jspecview.source.JDXSource;
import jspecview.source.JDXSpectrum;
import jspecview.util.JSVEscape;


/**
 * JSpecView Applet class. For a list of parameters and scripting functionality
 * see the file JSpecView_Applet_Specification.html.
 * 
 * @author Bob Hanson
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A. D. Walters
 * @author Prof Robert J. Lancashire
 */

public class JSVApp implements PanelListener, JSVAppInterface {

	public JSVApp(AppletFrame appletFrame, boolean isJS) {
		this.appletFrame = appletFrame;
		initViewer(isJS);
		init();
	}

	private void initViewer(boolean isJS) {
		viewer = new JSViewer(this, true, isJS);
		appletFrame.setDropTargetListener(isSigned(), viewer);
		JSVFileManager.setDocumentBase(viewer, appletFrame.getDocumentBase());
	}

	protected AppletFrame appletFrame;

	private int fileCount;
	private int nViews;
	private int scriptLevelCount;

	boolean isNewWindow;

	// ------- settable parameters ------------

	private boolean allowCompoundMenu = true;
	private boolean allowMenu = true;
	private boolean autoIntegrate;
	private boolean interfaceOverlaid;
	private boolean loadImaginary = false;
	private Boolean obscureTitleFromUser;

	private int initialStartIndex = -1;
	private int initialEndIndex = -1;

	private String integrationRatios;

	private String appletReadyCallbackFunctionName;

	private String coordCallbackFunctionName;
	private String loadFileCallbackFunctionName;
	private String peakCallbackFunctionName;
	private String syncCallbackFunctionName;
	public JSViewer viewer;

	// ///// parameter set/get methods

	public boolean isPro() {
		return isSigned();
	}

	public boolean isSigned() {
		return false;
	}

	public void siSetCurrentSource(JDXSource source) {
		viewer.currentSource = source;
	}

	public int siGetFileCount() {
		return fileCount;
	}

	public void siSetFileCount(int n) {
		fileCount = n;
	}

	public void siSetIntegrationRatios(String value) {
		integrationRatios = value;
	}

	public String siGetIntegrationRatios() {
		return integrationRatios;
	}

	public AppletFrame getAppletFrame() {
		return appletFrame;
	}

	public void siSetLoadImaginary(boolean TF) {
		loadImaginary = TF;
	}

	public int siIncrementScriptLevelCount(int n) {
		return scriptLevelCount += n;
	}

	public int siIncrementViewCount(int n) {
		return nViews += n;
	}

	// ///////////////////////////////////////

	public void dispose() {
		try {
			viewer.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ///////////// private methods called from page or browser JavaScript calls
	// ////////////////
	//
	//
	// Notice that in all of these we use getSelectedPanel(), not selectedJSVPanel
	// That's because the methods aren't overridden in JSVAppletPro, and in that
	// case
	// we want to select the panel from MainFrame, not here. Thus, when the
	// Advanced...
	// tab is open, actions from outside of Jmol act on the MainFrame, not here.
	//
	// BH - 8.3.2012

	public Map<String, Object> getPropertyAsJavaObject(String key) {
		return viewer.getPropertyAsJavaObject(key);
	}

	public String getPropertyAsJSON(String key) {
		return JSVEscape.toJSON(null, getPropertyAsJavaObject(key), false);
	}

	/**
	 * Method that can be called from another applet or from javascript to return
	 * the coordinate of clicked point in the plot area of the <code>
   * JSVPanel</code>
	 * 
	 * @return A String representation of the coordinate
	 */
	public String getCoordinate() {
		return viewer.getCoordinate();
	}

	/**
	 * Loads in-line JCAMP-DX data into the existing applet window
	 * 
	 * @param data
	 *          String
	 */
	public void loadInline(String data) {
		// newAppletPanel();
		siOpenDataOrFile(data, null, null, null, -1, -1, true);
		appletFrame.validateContent(3);
	}

	/**
	 * Delivers spectrum coded as desired: XY, SQZ, PAC, DIF, DIFDUP, FIX, AML,
	 * CML
	 * 
	 * @param type
	 * @param n
	 * @return data
	 * 
	 */
	public String exportSpectrum(String type, int n) {
		return viewer.export(type, n);
	}

	public void setFilePath(String tmpFilePath) {
		runScript("load " + JSVEscape.eS(tmpFilePath));
	}

	/**
	 * Sets the spectrum to the specified block number
	 * 
	 * @param n
	 */
	public void setSpectrumNumber(int n) {
		runScript(ScriptToken.SPECTRUMNUMBER + " " + n);
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * toggles reversing the plot on a <code>JSVPanel</code>
	 */
	public void reversePlot() {
		toggle(ScriptToken.REVERSEPLOT);
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * toggles the grid on a <code>JSVPanel</code>
	 */
	public void toggleGrid() {
		toggle(ScriptToken.GRIDON);
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * toggles the coordinate on a <code>JSVPanel</code>
	 */
	public void toggleCoordinate() {
		toggle(ScriptToken.COORDINATESON);
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * toggles the integration graph of a <code>JSVPanel</code>.
	 */
	public void toggleIntegration() {
		toggle(ScriptToken.INTEGRATE);
	}

	private void toggle(ScriptToken st) {
		JSVPanel jsvp = viewer.selectedPanel;
		if (jsvp != null)
			runScript(st + " TOGGLE");
	}

	/**
	 * Method that can be called from another applet or from javascript that adds
	 * a highlight to a portion of the plot area of a <code>JSVPanel</code>
	 * 
	 * @param x1
	 *          the starting x value
	 * @param x2
	 *          the ending x value
	 * @param r
	 *          the red portion of the highlight color
	 * @param g
	 *          the green portion of the highlight color
	 * @param b
	 *          the blue portion of the highlight color
	 * @param a
	 *          the alpha portion of the highlight color
	 */
	public void addHighlight(double x1, double x2, int r, int g, int b, int a) {
		viewer.addHighLight(x1, x2, r, g, b, a);
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * removes all highlights from the plot area of a <code>JSVPanel</code>
	 */
	public void removeAllHighlights() {
		viewer.removeAllHighlights();
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * removes a highlight from the plot area of a <code>JSVPanel</code>
	 * 
	 * @param x1
	 *          the starting x value
	 * @param x2
	 *          the ending x value
	 */
	public void removeHighlight(double x1, double x2) {
		viewer.removeHighlight(x1, x2);
	}

	public void syncScript(String peakScript) {
		viewer.syncScript(peakScript);
	}

	/**
	 * Writes a message to the status label
	 * 
	 * @param msg
	 *          the message
	 */
	public void writeStatus(String msg) {
		Logger.info(msg);
		// statusTextLabel.setText(msg);
	}

	// //////////////////////// PRIVATE or SEMIPRIVATE METHODS
	// ////////////////////
	// ///////////
	// ///////////
	// ///////////
	// ///////////
	// ///////////

	/**
	 * Initializes applet with parameters and load the <code>JDXSource</code>
	 * called by the browser
	 * 
	 */
	private void init() {

		initParams(appletFrame.getParameter("script"));
		if (appletReadyCallbackFunctionName != null && viewer.fullName != null)
			appletFrame.callToJavaScript(appletReadyCallbackFunctionName, new Object[] {
					viewer.appletID, viewer.fullName, Boolean.TRUE, appletFrame });

	}

	/**
	 * starts or restarts applet display from scratch or from a JSVApplet.script()
	 * JavaScript command
	 * 
	 * Involves a two-pass sequence through parsing the parameters, because order
	 * is not important in this sort of call.
	 * 
	 * To call a script and have commands execute in order, use
	 * 
	 * JSVApplet.runScript(script)
	 * 
	 * instead
	 * 
	 * @param params
	 */
	public void initParams(String params) {
		parseInitScript(params);
		newAppletPanel();
		viewer.setPopupMenu(allowMenu, viewer.parameters
				.getBoolean(ScriptToken.ENABLEZOOM));
		runScriptNow(params);
	}

	private void newAppletPanel() {
		Logger.info("newAppletPanel");
		appletFrame.addNewPanel(viewer);
	}

	private JSVPanel prevPanel;

	public void siSendPanelChange(JSVPanel jsvp) {
		if (jsvp == prevPanel)
			return;
		prevPanel = jsvp;
		viewer.sendPanelChange(jsvp);
	}

	// //////////// JSVAppletPopupMenu calls

	/**
	 * Shows the applet in a Frame
	 * 
	 * @param isSelected
	 */
	public void siNewWindow(boolean isSelected, boolean fromFrame) {
		isNewWindow = isSelected;
		if (fromFrame) {
			if (viewer.jsvpPopupMenu != null)
				viewer.jsvpPopupMenu.setSelected("Window", false);
		} else {
			appletFrame.newWindow(isSelected);
		}
	}

	public void repaint() {
		
    /**
     * Jmol._repaint(applet,asNewThread)
     * 
     * should invoke 
     * 
     *   setTimeout(applet._applet.viewer.updateJS(width, height)) // may be 0,0
     *   
     * when it is ready to do so.
     * 
     * @j2sNative
     * 
     * if (typeof Jmol != "undefined" && Jmol._repaint && this.viewer.applet)
     *   Jmol._repaint(this.viewer.applet,true);
     * 
     */
		{
			appletFrame.repaint();
		}
	}
	
	/**
	 * 
	 * @param width
	 * @param height
	 */
	public void updateJS(int width, int height) {
		
	}

	public void siValidateAndRepaint() {
		appletFrame.validate();
		repaint();
	}

	/**
	 * Loads a new file into the existing applet window
	 * 
	 * @param filePath
	 */
	public void siSyncLoad(String filePath) {
		newAppletPanel();
		Logger.info("JSVP syncLoad reading " + filePath);
		siOpenDataOrFile(null, null, null, filePath, -1, -1, false);
		appletFrame.validateContent(3);
	}

	/**
	 * Parses the javascript call parameters and executes them accordingly
	 * 
	 * @param params
	 *          String
	 */
	private void parseInitScript(String params) {
		if (params == null)
			params = "";
		ScriptTokenizer allParamTokens = new ScriptTokenizer(params, true);
		if (Logger.debugging) {
			Logger.info("Running in DEBUG mode");
		}
		while (allParamTokens.hasMoreTokens()) {
			String token = allParamTokens.nextToken();
			// now split the key/value pair
			ScriptTokenizer eachParam = new ScriptTokenizer(token, false);
			String key = eachParam.nextToken();
			if (key.equalsIgnoreCase("SET"))
				key = eachParam.nextToken();
			key = key.toUpperCase();
			ScriptToken st = ScriptToken.getScriptToken(key);
			String value = ScriptToken.getValue(st, eachParam, token);
			//if (Logger.debugging)
				Logger.info("KEY-> " + key + " VALUE-> " + value + " : " + st);
			try {
				switch (st) {
				default:
					viewer.parameters.set(null, st, value);
					break;
				case UNKNOWN:
					break;
				case APPLETID:
					viewer.appletID = value;
					viewer.fullName = viewer.appletID + "__" + viewer.syncID + "__";
					/**
					 * @j2sNative
					 * 
					 *            if(typeof Jmol != "undefined") this.viewer.applet =
					 *            Jmol._applets[value];
					 * 
					 * 
					 */
					{
					}

					break;
				case APPLETREADYCALLBACKFUNCTIONNAME:
					appletReadyCallbackFunctionName = value;
					break;
				case AUTOINTEGRATE:
					autoIntegrate = Parameters.isTrue(value);
					break;
				case COMPOUNDMENUON:
					allowCompoundMenu = Boolean.parseBoolean(value);
					break;
				case COORDCALLBACKFUNCTIONNAME:
				case LOADFILECALLBACKFUNCTIONNAME:
				case PEAKCALLBACKFUNCTIONNAME:
				case SYNCCALLBACKFUNCTIONNAME:
					siExecSetCallback(st, value);
					break;
				case ENDINDEX:
					initialEndIndex = Integer.parseInt(value);
					break;
				case INTERFACE:
					siExecSetInterface(value);
					break;
				case IRMODE:
					viewer.setIRmode(value);
					break;
				case MENUON:
					allowMenu = Boolean.parseBoolean(value);
					break;
				case OBSCURE:
					if (obscureTitleFromUser == null) // once only
						obscureTitleFromUser = Boolean.valueOf(value);
					break;
				case STARTINDEX:
					initialStartIndex = Integer.parseInt(value);
					break;
				// case SPECTRUMNUMBER:
				// initialSpectrumNumber = Integer.parseInt(value);
				// break;
				case SYNCID:
					viewer.syncID = value;
					viewer.fullName = viewer.appletID + "__" + viewer.syncID + "__";
					break;
				}
			} catch (Exception e) {
			}
		}
	}

	/*
	 * private void interruptQueueThreads() { if (commandWatcherThread != null)
	 * commandWatcherThread.interrupt(); }
	 */
	public void siOpenDataOrFile(String data, String name,
			List<JDXSpectrum> specs, String url, int firstSpec, int lastSpec,
			boolean isAppend) {
		int status = viewer.openDataOrFile(data, name, specs, url, firstSpec,
				lastSpec, isAppend);
		if (status == JSViewer.FILE_OPEN_ALREADY)
			return;
		if (status != JSViewer.FILE_OPEN_OK) {
			siSetSelectedPanel(null);
			return;
		}

		if (viewer.jsvpPopupMenu != null)
			viewer.jsvpPopupMenu.setCompoundMenu(viewer.panelNodes, allowCompoundMenu);

		Logger.info(appletFrame.getAppletInfo() + " File "
				+ viewer.currentSource.getFilePath() + " Loaded Successfully");

	}

	// ///////// simple sync functionality //////////

	/**
	 * overloaded in JSVAppletPro
	 * 
	 * @param scriptItem
	 */
	public void siProcessCommand(String scriptItem) {
		viewer.runScriptNow(scriptItem);
	}

	public boolean runScriptNow(String params) {
		return viewer.runScriptNow(params);
	}

	/**
	 * fires peakCallback ONLY if there is a peak found fires coordCallback ONLY
	 * if there is no peak found or no peakCallback active
	 * 
	 * if (peakFound && havePeakCallback) { do the peakCallback } else { do the
	 * coordCallback }
	 * 
	 * Is that what we want?
	 * 
	 */
	private void checkCallbacks() {
		if (coordCallbackFunctionName == null && peakCallbackFunctionName == null)
			return;
		Coordinate coord = new Coordinate();
		Coordinate actualCoord = (peakCallbackFunctionName == null ? null
				: new Coordinate());
		// will return true if actualcoord is null (just doing coordCallback)
		if (!viewer.selectedPanel.getPanelData().getPickedCoordinates(coord,
				actualCoord))
			return;
		int iSpec = viewer.viewPanel.getCurrentPanelIndex();
		if (actualCoord == null)
			appletFrame.callToJavaScript(coordCallbackFunctionName, new Object[] {
					Double.valueOf(coord.getXVal()), Double.valueOf(coord.getYVal()),
					Integer.valueOf(iSpec + 1) });
		else
			appletFrame.callToJavaScript(peakCallbackFunctionName, new Object[] {
					Double.valueOf(coord.getXVal()), Double.valueOf(coord.getYVal()),
					Double.valueOf(actualCoord.getXVal()),
					Double.valueOf(actualCoord.getYVal()), Integer.valueOf(iSpec + 1) });
	}

	public void siSetSelectedPanel(JSVPanel jsvp) {
		viewer.viewPanel.setSelectedPanel(jsvp, viewer.panelNodes);
		viewer.selectedPanel = jsvp;
		viewer.spectraTree.setSelectedPanel(this, jsvp);
		appletFrame.validate();
		if (jsvp != null) {
			jsvp.setEnabled(true);
			jsvp.setFocusable(true);
		}
	}

	// /////////// MISC methods from interfaces /////////////

	/**
	 * called by Pro's popup window Advanced...
	 * 
	 * @param filePath
	 */
	public void doAdvanced(String filePath) {
		// only for JSVAppletPro
	}

	// //////////////// PanelEventInterface

	/**
	 * called by notifyPeakPickedListeners in JSVPanel
	 */
	public void panelEvent(Object eventObj) {
		if (eventObj instanceof PeakPickEvent) {
			viewer.processPeakPickEvent(eventObj, false);
		} else if (eventObj instanceof ZoomEvent) {
		} else if (eventObj instanceof SubSpecChangeEvent) {
		}
	}

	// ///////////// ScriptInterface execution from JSViewer.runScriptNow and
	// menus

	@SuppressWarnings("incomplete-switch")
	public void siExecSetCallback(ScriptToken st, String value) {
		switch (st) {
		case LOADFILECALLBACKFUNCTIONNAME:
			loadFileCallbackFunctionName = value;
			break;
		case PEAKCALLBACKFUNCTIONNAME:
			peakCallbackFunctionName = value;
			break;
		case SYNCCALLBACKFUNCTIONNAME:
			syncCallbackFunctionName = value;
			break;
		case COORDCALLBACKFUNCTIONNAME:
			coordCallbackFunctionName = value;
			break;
		}
	}

	/**
	 * Returns the calculated colour of a visible spectrum (Transmittance)
	 * 
	 * @return Color
	 */

	public String getSolnColour() {
		return viewer.getSolutionColor();
	}

	public void siExecClose(String value) {
		boolean fromScript = (!value.startsWith("!"));
		if (fromScript)
			value = value.substring(1);		
		viewer.close(value);
		if (!fromScript)
			siValidateAndRepaint();
	}

	public String siExecLoad(String value) {
		// int nSpec = panelNodes.size();
		viewer.load(value);
		if (viewer.selectedPanel == null)
			return null;
		// probably unnecessary:
		// setSpectrumIndex(nSpec, "execLoad");
		if (loadFileCallbackFunctionName != null)
			appletFrame.callToJavaScript(loadFileCallbackFunctionName, new Object[] { viewer.appletID,
					value });
		return null;
	}

	public void siExecHidden(boolean b) {
		// ignored
	}

	public void siExecSetInterface(String value) {
		interfaceOverlaid = (value.equalsIgnoreCase("single") || value
				.equalsIgnoreCase("overlay"));
	}

	public void siExecScriptComplete(String msg, boolean isOK) {
		siValidateAndRepaint();
	}

	public void siExecSetAutoIntegrate(boolean b) {
		autoIntegrate = b;
	}

	/**
	 * @param msg
	 */
	public synchronized void syncToJmol(String msg) {
		if (syncCallbackFunctionName == null)
			return;
		Logger.info("JSV>Jmol " + msg);
		appletFrame.callToJavaScript(syncCallbackFunctionName, new Object[] { viewer.fullName, msg });
	}

	public void setVisible(boolean b) {
		appletFrame.setPanelVisible(b);
	}

	public void siUpdateBoolean(ScriptToken st, boolean TF) {
		// ignored -- this is for setting buttons and menu items
	}

	public void siCheckCallbacks(String title) {
		checkCallbacks();
	}

	// /////// multiple source changes ////////

	public void siSetNode(PanelNode panelNode, boolean fromTree) {
		if (panelNode.jsvp != viewer.selectedPanel)
			siSetSelectedPanel(panelNode.jsvp);
		siSendPanelChange(panelNode.jsvp);
		appletFrame.validateContent(2);
		siValidateAndRepaint(); // app does not do repaint here
	}

	public void siCloseSource(JDXSource source) {
		viewer.closeSource(source);
	}

	public void setCursor(int id) {
		viewer.apiPlatform.setCursor(id, appletFrame);
	}

	public boolean siGetAutoCombine() {
		return interfaceOverlaid;
	}

	public JDXSource siCreateSource(String data, String filePath, URL base,
			int firstSpec, int lastSpec) throws Exception {
		return FileReader.createJDXSource(JSVFileManager
				.getBufferedReaderForString(data), filePath, base,
				obscureTitleFromUser == Boolean.TRUE, loadImaginary, -1, -1);
	}

	public JSVPanel siGetNewJSVPanel2(List<JDXSpectrum> specs) {
		JSVPanel jsvp = appletFrame.getJSVPanel(viewer, specs, initialStartIndex,
				initialEndIndex);
		initialEndIndex = initialStartIndex = -1;
		jsvp.getPanelData().addListener(this);
		viewer.parameters.setFor(jsvp, null, true);
		return jsvp;
	}

	public JSVPanel siGetNewJSVPanel(JDXSpectrum spec) {
		if (spec == null) {
			initialEndIndex = initialStartIndex = -1;
			return null;
		}
		List<JDXSpectrum> specs = new List<JDXSpectrum>();
		specs.addLast(spec);
		JSVPanel jsvp = appletFrame.getJSVPanel(viewer, specs, initialStartIndex,
				initialEndIndex);
		jsvp.getPanelData().addListener(this);
		viewer.parameters.setFor(jsvp, null, true);
		return jsvp;
	}

	public PanelNode siGetNewPanelNode(String id, String fileName,
			JDXSource source, JSVPanel jsvp) {
		return new PanelNode(id, fileName, source, jsvp);
	}

	// not implemented for applet

	public boolean siGetAutoShowLegend() {
		return false; // option?
	}

	private String returnFromJmolModel;

	public void siSetReturnFromJmolModel(String model) {
		returnFromJmolModel = model;
	}

	public String siGetReturnFromJmolModel() {
		return returnFromJmolModel;
	}

	public void siSetPropertiesFromPreferences(JSVPanel jsvp,
			boolean includeMeasures) {
		if (autoIntegrate)
			jsvp.getPanelData().integrateAll(viewer.parameters);
	}

	// not applicable to applet:

	public void siSetLoaded(String fileName, String filePath) {
	}

	public void siSetMenuEnables(PanelNode node, boolean isSplit) {
	}

	public void siSetRecentURL(String filePath) {
	}

	public void siUpdateRecentMenus(String filePath) {
	}

	// debugging

	public void siExecTest(String value) {
		String data = "##TITLE= Acetophenone\n##JCAMP-DX= 5.01\n##DATA TYPE= MASS SPECTRUM\n##DATA CLASS= XYPOINTS\n##ORIGIN= UWI, Mona, JAMAICA\n##OWNER= public domain\n##LONGDATE= 2012/02/19 22:20:06.0416 -0600 $$ export date from JSpecView\n##BLOCK_ID= 4\n##$URL= http://wwwchem.uwimona.edu.jm/spectra\n##SPECTROMETER/DATA SYSTEM= Finnigan\n##.INSTRUMENT PARAMETERS= LOW RESOLUTION\n##.SPECTROMETER TYPE= TRAP\n##.INLET= GC\n##.IONIZATION MODE= EI+\n##MOLFORM= C 8 H 8 O\n##$MODELS= \n<Models>\n<ModelData id=\"acetophenone\" type=\"MOL\">\nacetophenone\nDSViewer          3D                             0\n\n17 17  0  0  0  0  0  0  0  0999 V2000\n-1.6931    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  1\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  2\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  3\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  4\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  5\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  6\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  7\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  8\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  9\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0 10\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0 11\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0 12\n-2.3403    1.0639    0.0008 O   0  0  0  0  0  0  0  0  0 13\n-2.3832   -1.3197   -0.0010 C   0  0  0  0  0  0  0  0  0 14\n-2.0973   -1.8988    0.9105 H   0  0  0  0  0  0  0  0  0 15\n-2.0899   -1.9018   -0.9082 H   0  0  0  0  0  0  0  0  0 16\n-3.4920   -1.1799   -0.0059 H   0  0  0  0  0  0  0  0  0 17\n1  2  1  0  0  0\n2  5  4  0  0  0\n2  4  4  0  0  0\n3 12  1  0  0  0\n4  7  4  0  0  0\n5  6  4  0  0  0\n6 10  1  0  0  0\n6  3  4  0  0  0\n7  3  4  0  0  0\n7 11  1  0  0  0\n8  4  1  0  0  0\n9  5  1  0  0  0\n13  1  2  0  0  0\n14 16  1  0  0  0\n14  1  1  0  0  0\n14 15  1  0  0  0\n17 14  1  0  0  0\nM  END\n</ModelData>\n<ModelData id=\"2\" type=\"MOL\">\nacetophenone m/z 120\nDSViewer          3D                             0\n\n17 17  0  0  0  0  0  0  0  0999 V2000\n-1.6931    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  1\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  2\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  3\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  4\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  5\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  6\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  7\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  8\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  9\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0 10\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0 11\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0 12\n-2.3403    1.0639    0.0008 O   0  0  0  0  0  0  0  0  0 13\n-2.3832   -1.3197   -0.0010 C   0  0  0  0  0  0  0  0  0 14\n-2.0973   -1.8988    0.9105 H   0  0  0  0  0  0  0  0  0 15\n-2.0899   -1.9018   -0.9082 H   0  0  0  0  0  0  0  0  0 16\n-3.4920   -1.1799   -0.0059 H   0  0  0  0  0  0  0  0  0 17\n1  2  1  0  0  0\n2  5  4  0  0  0\n2  4  4  0  0  0\n3 12  1  0  0  0\n4  7  4  0  0  0\n5  6  4  0  0  0\n6 10  1  0  0  0\n6  3  4  0  0  0\n7  3  4  0  0  0\n7 11  1  0  0  0\n8  4  1  0  0  0\n9  5  1  0  0  0\n13  1  2  0  0  0\n14 16  1  0  0  0\n14  1  1  0  0  0\n14 15  1  0  0  0\n17 14  1  0  0  0\nM  END\nacetophenone m/z 105\n\ncreated with ArgusLab version 4.0.1\n13 13  0  0  0  0  0  0  0  0  0 V2000\n-1.6931    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n-2.3403    1.0639    0.0008 O   0  0  0  0  0  0  0  0  0  0  0  0\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0  0  0  0\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0  0  0  0\n1  2  1  0  0  0  0\n1  8  2  0  0  0  0\n2  4  4  0  0  0  0\n2  5  4  0  0  0  0\n3  6  4  0  0  0  0\n3  7  4  0  0  0  0\n3 13  1  0  0  0  0\n4  7  4  0  0  0  0\n4  9  1  0  0  0  0\n5  6  4  0  0  0  0\n5 10  1  0  0  0  0\n6 11  1  0  0  0  0\n7 12  1  0  0  0  0\nM  END\nacetophenone m/z 77\n\ncreated with ArgusLab version 4.0.1\n11 11  0  0  0  0  0  0  0  0  0 V2000\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0  0  0  0\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0  0  0  0\n1  3  4  0  0  0  0\n1  4  4  0  0  0  0\n2  5  4  0  0  0  0\n2  6  4  0  0  0  0\n2 11  1  0  0  0  0\n3  6  4  0  0  0  0\n3  7  1  0  0  0  0\n4  5  4  0  0  0  0\n4  8  1  0  0  0  0\n5  9  1  0  0  0  0\n6 10  1  0  0  0  0\nM  END\n</ModelData>\n</Models>\n##$PEAKS= \n<Peaks type=\"MS\" xUnits=\"M/Z\" yUnits=\"RELATIVE ABUNDANCE\" >\n<PeakData id=\"1\" title=\"molecular ion (~120)\" peakShape=\"sharp\" model=\"2.1\"  xMax=\"121\" xMin=\"119\"  yMax=\"100\" yMin=\"0\" />\n<PeakData id=\"2\" title=\"fragment 1 (~105)\" peakShape=\"sharp\" model=\"2.2\"  xMax=\"106\" xMin=\"104\"  yMax=\"100\" yMin=\"0\" />\n<PeakData id=\"3\" title=\"fragment 2 (~77)\" peakShape=\"sharp\" model=\"2.3\"  xMax=\"78\" xMin=\"76\"  yMax=\"100\" yMin=\"0\" />\n</Peaks>\n##XUNITS= M/Z\n##YUNITS= RELATIVE ABUNDANCE\n##XFACTOR= 1E0\n##YFACTOR= 1E0\n##FIRSTX= 0\n##FIRSTY= 0\n##LASTX= 121\n##NPOINTS= 19\n##XYPOINTS= (XY..XY)\n0.000000, 0.000000 \n38.000000, 5.200000 \n39.000000, 8.000000 \n43.000000, 21.900000 \n50.000000, 20.200000 \n51.000000, 41.900000 \n52.000000, 4.000000 \n63.000000, 3.800000 \n74.000000, 6.600000 \n75.000000, 3.700000 \n76.000000, 4.600000 \n77.000000, 100.000000 \n78.000000, 10.400000 \n89.000000, 1.000000 \n91.000000, 1.000000 \n105.000000, 80.800000 \n106.000000, 6.000000 \n120.000000, 23.100000 \n121.000000, 2.000000 \n##END=";
		loadInline(data);
	}

	public String siSetFileAsString(String value) {
		return JSVFileManager.getFileAsString(value, appletFrame.getDocumentBase());
	}

	public JSVTreeNode siCreateTree(JDXSource source, JSVPanel[] jsvPanels) {
		return viewer.spectraTree.createTree(this, source, jsvPanels);
	}

	public JSViewer siGetViewer() {
		return viewer;
	}

	public void runScript(String script) {
		viewer.runScript(script);
	}

	public List<String> getScriptQueue() {
		return viewer.scriptQueue;
	}
	
}
