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

package jspecview.applet;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import java.util.Map;

import org.jmol.api.ApiPlatform;
import org.jmol.util.JmolList;

import javax.swing.JFrame;

import org.jmol.util.Logger;
import org.jmol.util.Txt;

import jspecview.api.JSVAppletInterface;
import jspecview.api.JSVDialog;
import jspecview.api.JSVMainPanel;
import jspecview.api.JSVPanel;
import jspecview.api.JSVTree;
import jspecview.api.JSVTreeNode;
import jspecview.api.PanelListener;
import jspecview.api.ScriptInterface;
import jspecview.application.TextDialog;
import jspecview.awt.Platform;
import jspecview.common.JSVPanelNode;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.ColorParameters;
import jspecview.common.Parameters;
import jspecview.common.PeakPickEvent;
import jspecview.common.PrintLayout;
import jspecview.common.RepaintManager;
import jspecview.common.ScriptTokenizer;
import jspecview.common.ScriptToken;
import jspecview.common.Coordinate;
import jspecview.common.JDXSpectrum;
import jspecview.common.SubSpecChangeEvent;
import jspecview.common.ZoomEvent;
import jspecview.common.JDXSpectrum.IRMode;
import jspecview.export.Exporter;
import jspecview.java.AwtDialogOverlayLegend;
import jspecview.java.AwtDialogPrint;
import jspecview.java.AwtPanel;
import jspecview.java.AwtParameters;
import jspecview.java.AwtDropTargetListener;
import jspecview.java.AwtTree;
import jspecview.java.AwtDialogView;
import jspecview.java.AwtViewPanel;
import jspecview.java.AwtFileHelper;
import jspecview.source.FileReader;
import jspecview.source.JDXSource;
import jspecview.util.JSVEscape;
import jspecview.util.JSVFileManager;
import netscape.javascript.JSObject;

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

public class JSVAppletPrivate implements PanelListener, ScriptInterface,
		JSVAppletInterface {

	private ApiPlatform apiPlatform;

	JSVAppletPrivate(JSVApplet jsvApplet) {
		this.jsvApplet = jsvApplet;
		repaintManager = new RepaintManager(this);
		fileHelper = new AwtFileHelper(this);
		JSVFileManager.setDocumentBase(jsvApplet.getDocumentBase());
		apiPlatform = new Platform();
		init();
	}

	private JSVAppletPopupMenu     appletPopupMenu;
	protected Thread               commandWatcherThread;
	private JDXSource              currentSource;
	private AwtFileHelper             fileHelper;
	protected JSVApplet            jsvApplet;
	private JFrame                 offWindowFrame;
	private Component              spectrumPanel;
  private JmolList<JSVPanelNode>     panelNodes = new JmolList<JSVPanelNode>();  
	private ColorParameters        parameters = new AwtParameters("applet");
	private RepaintManager         repaintManager;
	private JSVPanel               selectedPanel;
  private JSVTree                spectraTree;
	private JSVMainPanel           viewPanel; // alias for spectrumPanel
  private JSVDialog              viewDialog;
	private JSVDialog              overlayLegendDialog;

	private String appletID;
	private String fullName;
	private String syncID;
	
	private int fileCount;
	private int nViews;
	private int scriptLevelCount;
	
	boolean isNewWindow;


	//------- settable parameters ------------

	private IRMode irMode = IRMode.NO_CONVERT;

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
	private JSViewer viewer;

	/////// parameter set/get methods
	
	public boolean isPro() {
		return isSigned();
	}

	public boolean isSigned() {
		return false;
	}

	public JDXSource getCurrentSource() {
		return currentSource;
	}
	public void setCurrentSource(JDXSource source) {
		currentSource = source;
	}

	public int getFileCount() {
		return fileCount;
	}
	public void setFileCount(int n) {
		fileCount = n;
	}

	public void setIntegrationRatios(String value) {
		integrationRatios = value;
	}
	public String getIntegrationRatios() {
		return integrationRatios;
	}

															
	public void setIRMode(IRMode mode) {
		irMode = mode;
	}
	public IRMode getIRMode() {
		return irMode;
	}

	JSVApplet getJsvApplet() {
		return jsvApplet;
	}

	public void setLoadImaginary(boolean TF) {
		loadImaginary = TF;
	}

	public JmolList<JSVPanelNode> getPanelNodes() {
		return panelNodes;
	}
	
	public ColorParameters getParameters() {
		return parameters;
	}

	public Object getPopupMenu() {
		return appletPopupMenu;
	}

	public int incrementScriptLevelCount(int n) {
		return scriptLevelCount += n;
	}
	
	public JSVPanel getSelectedPanel() {
		return selectedPanel;
	}

  public JSVTree getSpectraTree() {
  	return spectraTree;
  }
  

	public int incrementViewCount(int n) {
		return nViews += n;
	}
	
	/////////////////////////////////////////
	
	void dispose() {
		fileHelper = null;
		try {
			if (viewDialog != null)
	  		viewDialog.dispose();
			viewDialog = null;
			if (overlayLegendDialog != null)
				overlayLegendDialog.dispose();
			overlayLegendDialog = null;
			if (commandWatcherThread != null) {
				commandWatcherThread.interrupt();
				commandWatcherThread = null;
			}
			if (panelNodes != null)
				for (int i = panelNodes.size(); --i >= 0;) {
					panelNodes.get(i).dispose();
					panelNodes.remove(i);
				}
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
		//newAppletPanel();
		openDataOrFile(data, null, null, null, -1, -1, true);
		jsvApplet.getContentPane().validate();
		spectrumPanel.validate();
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
		return ((AwtPanel) getSelectedPanel()).export(type, n);
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
		JSVPanel jsvp = getSelectedPanel();
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

		viewer = new JSViewer(this);
		spectraTree = new AwtTree(viewer);
		scriptQueue = new JmolList<String>();
		commandWatcherThread = new Thread(new CommandWatcher());
		commandWatcherThread.setName("CommmandWatcherThread");
		commandWatcherThread.start();

		initParams(jsvApplet.getParameter("script"));
		if (appletReadyCallbackFunctionName != null && fullName != null)
			callToJavaScript(appletReadyCallbackFunctionName, new Object[] {
					appletID, fullName, Boolean.TRUE, jsvApplet });

		if (isSigned()) {
			new DropTarget(jsvApplet, getDropListener());
		}
	}

	private DropTargetListener dtl;

	private DropTargetListener getDropListener() {
		if (dtl == null)
			dtl = new AwtDropTargetListener(this);
		return dtl;
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
	void initParams(String params) {
		parseInitScript(params);
		newAppletPanel();
		appletPopupMenu = new JSVAppletPopupMenu(this, allowMenu, parameters
				.getBoolean(ScriptToken.ENABLEZOOM));
		runScriptNow(params);
	}

	private void newAppletPanel() {
		Logger.info("newAppletPanel");
		jsvApplet.getContentPane().removeAll();
		spectrumPanel = (Component) (viewPanel = new AwtViewPanel(new BorderLayout()));
		jsvApplet.getContentPane().add(spectrumPanel);
	}

	private JSVPanel prevPanel;
	public void sendPanelChange(JSVPanel jsvp) {
		if (jsvp == prevPanel)
			return;
		prevPanel = jsvp;
		viewer.sendPanelChange(jsvp);
	}

	/**
	 * Shows a floating overlay key if possible
	 * @param visible 
	 * 
	 */
	protected void showOverlayKey(boolean visible) {
		viewer.setOverlayLegendVisibility(getSelectedPanel(), visible);
	}

	// //////////// JSVAppletPopupMenu calls

	/**
	 * Shows the header information for the Spectrum
	 */
	void showHeader() {
    getSelectedPanel().showHeader(jsvApplet);
	}

	/**
	 * Opens the print dialog to enable printing
	 */
	public void print() {
	  print("");
	}

		/**
	 * Opens the print dialog to enable printing
	 */
	public String print(String pdfFileName) {
		boolean needWindow = false; // !isNewWindow;
		// not sure what this is about. The applet prints fine
		if (needWindow)
			newWindow(true);
		String s = Exporter.print(this, fileHelper, pdfFileName);
		if (needWindow)
			newWindow(false);
		return s;
	}

	/**
	 * Shows the applet in a Frame
	 * @param isSelected 
	 */
	void newWindow(boolean isSelected) {
		isNewWindow = isSelected;
		if (isSelected) {
			offWindowFrame = new JFrame("JSpecView");
			offWindowFrame.setSize(jsvApplet.getSize());
			final Dimension d;
			d = spectrumPanel.getSize();
			offWindowFrame.add(spectrumPanel);
			offWindowFrame.validate();
			offWindowFrame.setVisible(true);
			jsvApplet.remove(spectrumPanel);
			validateAndRepaint();
			offWindowFrame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					windowClosingEvent(d);
				}
			});
		} else {
			jsvApplet.getContentPane().add(spectrumPanel);
			validateAndRepaint();
			offWindowFrame.removeAll();
			offWindowFrame.dispose();
			offWindowFrame = null;
		}
	}

	protected void windowClosingEvent(Dimension d) {
		spectrumPanel.setSize(d);
		jsvApplet.getContentPane().add(spectrumPanel);
		jsvApplet.setVisible(true);
		validateAndRepaint();
		offWindowFrame.removeAll();
		offWindowFrame.dispose();
		appletPopupMenu.windowMenuItem.setSelected(false);
		isNewWindow = false;
	}

	public void repaint() {
		jsvApplet.repaint();
	}
	
	public void validateAndRepaint() {
		jsvApplet.validate();
		jsvApplet.repaint();
	}
	
	/**
	 * Export spectrum in a given format
	 * @param type 
	 * 
	 */
	void exportSpectrumViaMenu(String type) {
		if (!isSigned()) {
			Logger.info(exportSpectrum(type, -1));
			return;
		}
		Exporter.exportSpectrum(this, fileHelper, type);
		getSelectedPanel().getFocusNow(true);
	}

	/**
	 * Loads a new file into the existing applet window
	 * 
	 * @param filePath
	 */
	public void syncLoad(String filePath) {
		newAppletPanel();
		Logger.info("JSVP syncLoad reading " + filePath);
		openDataOrFile(null, null, null, filePath, -1, -1, false);
		jsvApplet.getContentPane().validate();
		spectrumPanel.validate();
	}

	/**
	 * Calls a javascript function given by the function name passing to it the
	 * string parameters as arguments
	 * @param callback 
	 * @param params 
	 * 
	 */
	private void callToJavaScript(String callback, Object[] params) {
		try {
			JSObject jso = JSObject.getWindow(jsvApplet);
			if (callback.length() > 0) {
				if (callback.indexOf(".") > 0) {
					String[] mods = Txt.split(callback, ".");
					for (int i = 0; i < mods.length - 1; i++) {
						jso = (JSObject) jso.getMember(mods[i]);
					}
					callback = mods[mods.length - 1];
				}
				Logger.info("JSVApplet calling " + jso + " " + callback);
				jso.call(callback, params);
			}

		} catch (Exception npe) {
			Logger.warn("EXCEPTION-> " + npe.getMessage());
		}
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
		ScriptTokenizer allParamTokens = new ScriptTokenizer(params,
				true);
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
			if (Logger.debugging)
				Logger.info("KEY-> " + key + " VALUE-> " + value + " : " + st);
			try {
				switch (st) {
				default:
					parameters.set(null, st, value);
					break;
				case UNKNOWN:
					break;
				case APPLETID:
					appletID = value;
					fullName = appletID + "__" + syncID + "__";
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
					execSetCallback(st, value);
					break;
				case ENDINDEX:
					initialEndIndex = Integer.parseInt(value);
					break;
				case INTERFACE:
					execSetInterface(value);
					break;
				case IRMODE:
					irMode = IRMode.getMode(value);
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
				//case SPECTRUMNUMBER:
					//initialSpectrumNumber = Integer.parseInt(value);
					//break;
				case SYNCID:
					syncID = value;
					fullName = appletID + "__" + syncID + "__";
					break;
				}
			} catch (Exception e) {
			}
		}
	}

	// for the signed applet to load a remote file, it must
	// be using a thread started by the initiating thread;
	JmolList<String> scriptQueue;

	class CommandWatcher implements Runnable {
		public void run() {
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			int commandDelay = 200;
			while (commandWatcherThread != null) {
				try {
					Thread.sleep(commandDelay);
					if (commandWatcherThread != null) {
						if (scriptQueue.size() > 0) {
							String scriptItem = scriptQueue.remove(0);
							if (scriptItem != null)
								processCommand(scriptItem);
						}
					}
				} catch (InterruptedException ie) {
					Logger.info("CommandWatcher InterruptedException!");
					break;
				} catch (Exception ie) {
					String s = "script processing ERROR:\n\n" + ie.toString();
					for (int i = 0; i < ie.getStackTrace().length; i++) {
						s += "\n" + ie.getStackTrace()[i].toString();
					}
					Logger.info("CommandWatcher Exception! " + s);
					break;
				}
			}
			commandWatcherThread = null;
		}
	}

	/*
	 * private void interruptQueueThreads() { if (commandWatcherThread != null)
	 * commandWatcherThread.interrupt(); }
	 */
	public void openDataOrFile(String data, String name,
			JmolList<JDXSpectrum> specs, String url, int firstSpec, int lastSpec, boolean isAppend) {
  	int status = viewer.openDataOrFile(data, name, specs, url, firstSpec, lastSpec, isAppend);
  	if (status == JSViewer.FILE_OPEN_ALREADY)
  		return;
    if (status != JSViewer.FILE_OPEN_OK) {
    	setSelectedPanel(null);
    	return;
    }

    appletPopupMenu.setCompoundMenu(panelNodes, allowCompoundMenu);

		Logger.info(jsvApplet.getAppletInfo() + " File " + currentSource.getFilePath()
				+ " Loaded Successfully");
		
	}

	
	protected void processCommand(String script) {
		runScriptNow(script);
	}

	// ///////// simple sync functionality //////////

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
		if (!getSelectedPanel().getPanelData().getPickedCoordinates(coord,
				actualCoord))
			return;
		int iSpec = viewPanel.getCurrentPanelIndex();
		if (actualCoord == null)
			callToJavaScript(coordCallbackFunctionName, new Object[] {
					Double.valueOf(coord.getXVal()), Double.valueOf(coord.getYVal()),
					Integer.valueOf(iSpec + 1) });
		else
			callToJavaScript(peakCallbackFunctionName, new Object[] {
					Double.valueOf(coord.getXVal()), Double.valueOf(coord.getYVal()),
					Double.valueOf(actualCoord.getXVal()),
					Double.valueOf(actualCoord.getYVal()),
					Integer.valueOf(iSpec + 1) });
	}

	public void setSelectedPanel(JSVPanel jsvp) {
		viewPanel.setSelectedPanel(jsvp, panelNodes);
  	selectedPanel = jsvp;
		spectraTree.setSelectedPanel(this, jsvp);
    jsvApplet.validate();
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
	void doAdvanced(String filePath) {
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


	public void runScript(String script) {
		if (scriptQueue == null)
			processCommand(script);
		else
			scriptQueue.addLast(script);
	}

	public String execExport(JSVPanel jsvp, String value) {
		if (jsvp != null && isPro())
			writeStatus(Exporter.exportCmd(jsvp, ScriptToken.getTokens(value), false));
		return null;
	}
	
	@SuppressWarnings("incomplete-switch")
	public void execSetCallback(ScriptToken st, String value) {
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
		return getSelectedPanel().getPanelData().getSolutionColor();
	}

	public void execClose(String value, boolean fromScript) {
		viewer.close(value);
    if (!fromScript)
    	validateAndRepaint();
	}

  public String execLoad(String value) {
  	//int nSpec = panelNodes.size();
  	viewer.load(value);
    if (getSelectedPanel() == null)
      return null;
    // probably unnecessary:
		//setSpectrumIndex(nSpec, "execLoad");
		if (loadFileCallbackFunctionName != null)
			callToJavaScript(loadFileCallbackFunctionName, new Object[] { appletID,
					value });
    return null;
  }

	public void execHidden(boolean b) {
		// ignored
	}

	public void execSetInterface(String value) {
		interfaceOverlaid = (value.equalsIgnoreCase("single")
				|| value.equalsIgnoreCase("overlay"));
	}

	public void execScriptComplete(String msg, boolean isOK) {
		validateAndRepaint();
	}

	public void execSetAutoIntegrate(boolean b) {
		autoIntegrate = b;
	}

	public PanelData getPanelData() {
		return getSelectedPanel().getPanelData();
	}

	public JSVDialog getOverlayLegend(JSVPanel jsvp) {
		return overlayLegendDialog = new AwtDialogOverlayLegend(null, getSelectedPanel());
	}

	/**
	 * @param msg
	 */
	public synchronized void syncToJmol(String msg) {
		if (syncCallbackFunctionName == null)
			return;
		Logger.info("JSV>Jmol " + msg);
		callToJavaScript(syncCallbackFunctionName, new Object[] { fullName, msg });
	}
	
	public void setVisible(boolean b) {
		spectrumPanel.setVisible(b);
	}

	public void showProperties() {
		TextDialog.showProperties(jsvApplet, getPanelData().getSpectrum());
	}

	public void updateBoolean(ScriptToken st, boolean TF) {
		// ignored -- this is for setting buttons and menu items
	}

	public void checkCallbacks(String title) {
		checkCallbacks();
	}

	///////// multiple source changes ////////
	
	
	public JSVPanelNode setOverlayVisibility(JSVPanelNode node) {
		viewer.setOverlayLegendVisibility(getSelectedPanel(),
				appletPopupMenu.overlayKeyMenuItem.isSelected());
		return node;
	}

	public void setNode(JSVPanelNode panelNode, boolean fromTree) {
		if (panelNode.jsvp != getSelectedPanel())
			setSelectedPanel(panelNode.jsvp);
		sendPanelChange(panelNode.jsvp);
		spectrumPanel.validate();
		validateAndRepaint(); // app does not do repaint here
	}

	public void closeSource(JDXSource source) {
  	viewer.closeSource(source);
	}

	public void process(JmolList<JDXSpectrum> specs) {
    JDXSpectrum.process(specs, irMode);
	}
	
	public void setCursor(int id) {
		apiPlatform.setCursor(id, jsvApplet);
	}

	public boolean getAutoCombine() {
		return interfaceOverlaid;
	}
	
	public JDXSource createSource(String data, String filePath, URL base,
			int firstSpec, int lastSpec) throws Exception {
		return FileReader.createJDXSource(JSVFileManager
				.getBufferedReaderForString(data), filePath, base,
				obscureTitleFromUser == Boolean.TRUE, loadImaginary, -1, -1);
	}

	public JSVPanel getNewJSVPanel(JmolList<JDXSpectrum> specs) {
		JSVPanel jsvp = AwtPanel.getJSVPanel(this, specs, initialStartIndex, initialEndIndex, appletPopupMenu);
		initialEndIndex = initialStartIndex = -1;
		jsvp.getPanelData().addListener(this);
		parameters.setFor(jsvp, null, true);
		return jsvp;
	}

	public JSVPanel getNewJSVPanel(JDXSpectrum spec) {
		if (spec == null) {
			initialEndIndex = initialStartIndex = -1;
			return null;
		}
		JmolList<JDXSpectrum> specs = new JmolList<JDXSpectrum>();
		specs.addLast(spec);
		JSVPanel jsvp = AwtPanel.getJSVPanel(this, specs, initialStartIndex, initialEndIndex, appletPopupMenu);
		jsvp.getPanelData().addListener(this);
		parameters.setFor(jsvp, null, true);
		return jsvp;
	}

	public JSVPanelNode getNewPanelNode(String id, String fileName, JDXSource source, JSVPanel jsvp) {
		return new JSVPanelNode(id, fileName, source, jsvp);	
	}

	// not implemented for applet
	
	public boolean getAutoShowLegend() {
		return false; //option?
	}

	public void checkOverlay() {
		if (viewPanel != null)
      viewPanel.markSelectedPanels(panelNodes);
		viewDialog = new AwtDialogView(this, spectrumPanel, false);
	}

	private String returnFromJmolModel;

	public void setReturnFromJmolModel(String model) {
    returnFromJmolModel = model;		
	}

	public String getReturnFromJmolModel() {
    return returnFromJmolModel;		
	}

	public void setPropertiesFromPreferences(JSVPanel jsvp, boolean includeMeasures) {
		if (autoIntegrate)
			jsvp.getPanelData().integrateAll(parameters);
	}

	public void requestRepaint() {
		if (getSelectedPanel() != null)
  		repaintManager.refresh();
	}

	public void repaintCompleted() {
			repaintManager.repaintDone();
	}

	// not applicable to applet:
	
	public void setLoaded(String fileName, String filePath) {} 
	public void setMenuEnables(JSVPanelNode node, boolean isSplit) {}
	public void setRecentURL(String filePath) {}
	public void updateRecentMenus(String filePath) {}

	// debugging

	public void execTest(String value) {
		String data = "##TITLE= Acetophenone\n##JCAMP-DX= 5.01\n##DATA TYPE= MASS SPECTRUM\n##DATA CLASS= XYPOINTS\n##ORIGIN= UWI, Mona, JAMAICA\n##OWNER= public domain\n##LONGDATE= 2012/02/19 22:20:06.0416 -0600 $$ export date from JSpecView\n##BLOCK_ID= 4\n##$URL= http://wwwchem.uwimona.edu.jm/spectra\n##SPECTROMETER/DATA SYSTEM= Finnigan\n##.INSTRUMENT PARAMETERS= LOW RESOLUTION\n##.SPECTROMETER TYPE= TRAP\n##.INLET= GC\n##.IONIZATION MODE= EI+\n##MOLFORM= C 8 H 8 O\n##$MODELS= \n<Models>\n<ModelData id=\"acetophenone\" type=\"MOL\">\nacetophenone\nDSViewer          3D                             0\n\n17 17  0  0  0  0  0  0  0  0999 V2000\n-1.6931    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  1\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  2\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  3\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  4\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  5\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  6\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  7\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  8\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  9\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0 10\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0 11\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0 12\n-2.3403    1.0639    0.0008 O   0  0  0  0  0  0  0  0  0 13\n-2.3832   -1.3197   -0.0010 C   0  0  0  0  0  0  0  0  0 14\n-2.0973   -1.8988    0.9105 H   0  0  0  0  0  0  0  0  0 15\n-2.0899   -1.9018   -0.9082 H   0  0  0  0  0  0  0  0  0 16\n-3.4920   -1.1799   -0.0059 H   0  0  0  0  0  0  0  0  0 17\n1  2  1  0  0  0\n2  5  4  0  0  0\n2  4  4  0  0  0\n3 12  1  0  0  0\n4  7  4  0  0  0\n5  6  4  0  0  0\n6 10  1  0  0  0\n6  3  4  0  0  0\n7  3  4  0  0  0\n7 11  1  0  0  0\n8  4  1  0  0  0\n9  5  1  0  0  0\n13  1  2  0  0  0\n14 16  1  0  0  0\n14  1  1  0  0  0\n14 15  1  0  0  0\n17 14  1  0  0  0\nM  END\n</ModelData>\n<ModelData id=\"2\" type=\"MOL\">\nacetophenone m/z 120\nDSViewer          3D                             0\n\n17 17  0  0  0  0  0  0  0  0999 V2000\n-1.6931    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  1\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  2\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  3\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  4\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  5\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  6\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  7\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  8\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  9\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0 10\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0 11\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0 12\n-2.3403    1.0639    0.0008 O   0  0  0  0  0  0  0  0  0 13\n-2.3832   -1.3197   -0.0010 C   0  0  0  0  0  0  0  0  0 14\n-2.0973   -1.8988    0.9105 H   0  0  0  0  0  0  0  0  0 15\n-2.0899   -1.9018   -0.9082 H   0  0  0  0  0  0  0  0  0 16\n-3.4920   -1.1799   -0.0059 H   0  0  0  0  0  0  0  0  0 17\n1  2  1  0  0  0\n2  5  4  0  0  0\n2  4  4  0  0  0\n3 12  1  0  0  0\n4  7  4  0  0  0\n5  6  4  0  0  0\n6 10  1  0  0  0\n6  3  4  0  0  0\n7  3  4  0  0  0\n7 11  1  0  0  0\n8  4  1  0  0  0\n9  5  1  0  0  0\n13  1  2  0  0  0\n14 16  1  0  0  0\n14  1  1  0  0  0\n14 15  1  0  0  0\n17 14  1  0  0  0\nM  END\nacetophenone m/z 105\n\ncreated with ArgusLab version 4.0.1\n13 13  0  0  0  0  0  0  0  0  0 V2000\n-1.6931    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n-2.3403    1.0639    0.0008 O   0  0  0  0  0  0  0  0  0  0  0  0\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0  0  0  0\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0  0  0  0\n1  2  1  0  0  0  0\n1  8  2  0  0  0  0\n2  4  4  0  0  0  0\n2  5  4  0  0  0  0\n3  6  4  0  0  0  0\n3  7  4  0  0  0  0\n3 13  1  0  0  0  0\n4  7  4  0  0  0  0\n4  9  1  0  0  0  0\n5  6  4  0  0  0  0\n5 10  1  0  0  0  0\n6 11  1  0  0  0  0\n7 12  1  0  0  0  0\nM  END\nacetophenone m/z 77\n\ncreated with ArgusLab version 4.0.1\n11 11  0  0  0  0  0  0  0  0  0 V2000\n-0.2141    0.0078    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n2.5839    0.0872    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.4615    1.2373   -0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n0.5257   -1.1809    0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.9188   -1.1393    0.0005 C   0  0  0  0  0  0  0  0  0  0  0  0\n1.8539    1.2756   -0.0001 C   0  0  0  0  0  0  0  0  0  0  0  0\n-0.1262    2.1703   -0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n0.0144   -2.1556    0.0002 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.4947   -2.0764    0.0009 H   0  0  0  0  0  0  0  0  0  0  0  0\n2.3756    2.2439   -0.0001 H   0  0  0  0  0  0  0  0  0  0  0  0\n3.6838    0.1161    0.0003 H   0  0  0  0  0  0  0  0  0  0  0  0\n1  3  4  0  0  0  0\n1  4  4  0  0  0  0\n2  5  4  0  0  0  0\n2  6  4  0  0  0  0\n2 11  1  0  0  0  0\n3  6  4  0  0  0  0\n3  7  1  0  0  0  0\n4  5  4  0  0  0  0\n4  8  1  0  0  0  0\n5  9  1  0  0  0  0\n6 10  1  0  0  0  0\nM  END\n</ModelData>\n</Models>\n##$PEAKS= \n<Peaks type=\"MS\" xUnits=\"M/Z\" yUnits=\"RELATIVE ABUNDANCE\" >\n<PeakData id=\"1\" title=\"molecular ion (~120)\" peakShape=\"sharp\" model=\"2.1\"  xMax=\"121\" xMin=\"119\"  yMax=\"100\" yMin=\"0\" />\n<PeakData id=\"2\" title=\"fragment 1 (~105)\" peakShape=\"sharp\" model=\"2.2\"  xMax=\"106\" xMin=\"104\"  yMax=\"100\" yMin=\"0\" />\n<PeakData id=\"3\" title=\"fragment 2 (~77)\" peakShape=\"sharp\" model=\"2.3\"  xMax=\"78\" xMin=\"76\"  yMax=\"100\" yMin=\"0\" />\n</Peaks>\n##XUNITS= M/Z\n##YUNITS= RELATIVE ABUNDANCE\n##XFACTOR= 1E0\n##YFACTOR= 1E0\n##FIRSTX= 0\n##FIRSTY= 0\n##LASTX= 121\n##NPOINTS= 19\n##XYPOINTS= (XY..XY)\n0.000000, 0.000000 \n38.000000, 5.200000 \n39.000000, 8.000000 \n43.000000, 21.900000 \n50.000000, 20.200000 \n51.000000, 41.900000 \n52.000000, 4.000000 \n63.000000, 3.800000 \n74.000000, 6.600000 \n75.000000, 3.700000 \n76.000000, 4.600000 \n77.000000, 100.000000 \n78.000000, 10.400000 \n89.000000, 1.000000 \n91.000000, 1.000000 \n105.000000, 80.800000 \n106.000000, 6.000000 \n120.000000, 23.100000 \n121.000000, 2.000000 \n##END=";
		loadInline(data);
	}
	
	public void setProperty(String key, String value) {
		// n/a
	}

	public String getFileAsString(String value) {
		return JSVFileManager.getFileAsString(value, jsvApplet.getDocumentBase());
	}

	private PrintLayout lastPrintLayout;

	public Object getPrintLayout(boolean isJob) {
		PrintLayout pl = new AwtDialogPrint(offWindowFrame, lastPrintLayout, isJob).getPrintLayout();
		if (pl != null)
			lastPrintLayout = pl;
		return pl;
	}

	public JSVTreeNode createTree(JDXSource source, JSVPanel[] jsvPanels) {
		return ((AwtTree) spectraTree).createTree(this, source, jsvPanels);
	}

	public JSViewer getViewer() {
		return viewer;
	}

}
