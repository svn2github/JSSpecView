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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import jspecview.application.TextDialog;
import jspecview.common.AwtPanel;
import jspecview.common.IntegralGraph;
import jspecview.common.JSVAppletInterface;
import jspecview.common.JSVDialog;
import jspecview.common.JSVDropTargetListener;
import jspecview.common.JSVPanel;
import jspecview.common.JSVPanelNode;
import jspecview.common.ViewPanel;
import jspecview.common.JSVTree;
import jspecview.common.JSViewer;
import jspecview.common.ViewDialog;
import jspecview.common.OverlayLegendDialog;
import jspecview.common.AwtParameters;
import jspecview.common.PanelData;
import jspecview.common.PanelListener;
import jspecview.common.Parameters;
import jspecview.common.PeakPickEvent;
import jspecview.common.PrintLayout;
import jspecview.common.PrintLayoutDialog;
import jspecview.common.ScriptCommandTokenizer;
import jspecview.common.ScriptInterface;
import jspecview.common.ScriptToken;
import jspecview.common.Coordinate;
import jspecview.common.Annotation;
import jspecview.common.JDXSpectrum;
import jspecview.common.SubSpecChangeEvent;
import jspecview.common.ZoomEvent;
import jspecview.export.Exporter;
import jspecview.source.FileReader;
import jspecview.source.JDXSource;
import jspecview.util.Escape;
import jspecview.util.FileManager;
import jspecview.util.Logger;
import jspecview.util.TextFormat;
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

	protected JSVApplet jsvApplet;

	JSVApplet getJsvApplet() {
		return jsvApplet;
	}

	int nViews;

	JSVAppletPrivate(JSVApplet jsvApplet) {
		this.jsvApplet = jsvApplet;
		init();
	}

	private ArrayList<Annotation> integrationRatios; // Integration Ratio
																										// Annotations
	private int initialStartIndex = -1;
	private int initialEndIndex = -1;
	private int irMode = JDXSpectrum.TA_NO_CONVERT;
	private boolean autoIntegrate;

	private String coordCallbackFunctionName;
	private String loadFileCallbackFunctionName;
	private String peakCallbackFunctionName;
	private String syncCallbackFunctionName;
	private String appletReadyCallbackFunctionName;

	private AwtParameters parameters = new AwtParameters("applet");

	/*---------------------------------END PARAMETERS------------------------*/

	boolean isStandalone; // not changed

	private ViewDialog viewDialog;
	private OverlayLegendDialog overlayLegendDialog;

	private String appletID;
	private String syncID;
	private Thread commandWatcherThread;
	private Boolean obscureTitleFromUser;
	private JFileChooser jFileChooser;
	private JFrame offWindowFrame;
	private ViewPanel spectrumPanel;
	private JSVAppletPopupMenu appletPopupMenu;
	public Object getPopupMenu() {
		return appletPopupMenu;
	}

  private List<JSVPanelNode> panelNodes = new ArrayList<JSVPanelNode>();
	public List<JSVPanelNode> getPanelNodes() {
		return panelNodes;
	}
	private String recentFileName = "";
	private JSVPanel selectedPanel;

	private JDXSource currentSource;
	public JDXSource getCurrentSource() {
		return currentSource;
	}
	public void setCurrentSource(JDXSource source) {
		currentSource = source;
	}

	public boolean isSigned() {
		return false;
	}

	public boolean isPro() {
		return isSigned();
	}

	void dispose() {
		jFileChooser = null;
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
		return JSViewer.getPropertyAsJavaObject(this, key);
	}

	public String getPropertyAsJSON(String key) {
		Map<String, Object> map = getPropertyAsJavaObject(key);
		return Escape.toJSON(null, map);
	}

	/**
	 * Method that can be called from another applet or from javascript to return
	 * the coordinate of clicked point in the plot area of the <code>
   * JSVPanel</code>
	 * 
	 * @return A String representation of the coordinate
	 */
	public String getCoordinate() {
		return JSViewer.getCoordinate(this);
	}

	/**
	 * Loads in-line JCAMP-DX data into the existing applet window
	 * 
	 * @param data
	 *          String
	 */
	public void loadInline(String data) {
		newAppletPanel();
		openDataOrFile(data, null, null, null, -1, -1);
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
		runScript("load " + Escape.escape(tmpFilePath));
	}

	/**
	 * Sets the spectrum to the specified block number
	 * 
	 * @param i
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
		JSViewer.addHighLight(this, x1, x2, r, g, b, a);
	}

	/**
	 * Method that can be called from another applet or from javascript that
	 * removes all highlights from the plot area of a <code>JSVPanel</code>
	 */
	public void removeAllHighlights() {
		JSViewer.removeAllHighlights(this);
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
		JSViewer.removeHighlights(this, x1, x2);
	}

	public void syncScript(String peakScript) {
		JSViewer.syncScript(this, peakScript);
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

		spectraTree = new JSVTree((ScriptInterface) this);
		scriptQueue = new ArrayList<String>();
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
			dtl = new JSVDropTargetListener(this, false);
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
		System.out.println("newAppletPanel");
		jsvApplet.getContentPane().removeAll();
		spectrumPanel = new ViewPanel(new BorderLayout());
		jsvApplet.getContentPane().add(spectrumPanel);
	}

	private JSVPanel prevPanel;
	public void sendPanelChange(JSVPanel jsvp) {
		if (jsvp == prevPanel)
			return;
		prevPanel = jsvp;
		JSViewer.sendPanelChange(this, jsvp);
	}

	/**
	 * Shows a floating overlay key if possible
	 * 
	 * @param e
	 *          the ActionEvent
	 */
	protected void showOverlayKey(boolean visible) {
		JSViewer.setOverlayLegendVisibility(this, getSelectedPanel(), visible);
	}

	private String fullName;
	private boolean allowMenu = true;
	private boolean compoundMenuOn;
	private boolean allowCompoundMenu = true;
	private String dirLastExported;
	private boolean interfaceOverlaid;

	// //////////// JSVAppletPopupMenu calls

	/**
	 * Shows the header information for the Spectrum
	 */
	void showHeader() {

		JDXSpectrum spectrum = getSelectedPanel().getSpectrum();
		String[][] rowData = spectrum.getHeaderRowDataAsArray();
		String[] columnNames = { "Label", "Description" };
		JTable table = new JTable(rowData, columnNames);
		table.setPreferredScrollableViewportSize(new Dimension(400, 195));
		JScrollPane scrollPane = new JScrollPane(table);
		JOptionPane.showMessageDialog(jsvApplet, scrollPane, "Header Information",
				JOptionPane.PLAIN_MESSAGE);
	}

	private PrintLayout lastPrintLayout;

	/**
	 * Opens the print dialog to enable printing
	 */
	public void print() {
		if (!isSigned())
			return;
		boolean needWindow = false; // !isNewWindow;
		// not sure what this is about. The applet prints fine
		if (needWindow)
			newWindow(true);
		JSVPanel jsvp = getSelectedPanel();
		PrintLayout pl;
		if ((pl = (new PrintLayoutDialog(offWindowFrame, lastPrintLayout))
				.getPrintLayout()) == null)
			return;
		lastPrintLayout = pl;
		((AwtPanel) jsvp).printSpectrum(pl);
		if (needWindow)
			newWindow(false);
	}

	boolean isNewWindow;

	/**
	 * Shows the applet in a Frame
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
					spectrumPanel.setSize(d);
					jsvApplet.getContentPane().add(spectrumPanel);
					jsvApplet.setVisible(true);
					validateAndRepaint();
					offWindowFrame.removeAll();
					offWindowFrame.dispose();
					appletPopupMenu.windowMenuItem.setSelected(false);
					isNewWindow = false;
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

	public void validateAndRepaint() {
		jsvApplet.validate();
		jsvApplet.repaint();
	}
	
	/**
	 * Export spectrum in a given format
	 * 
	 * @param command
	 *          the name of the format to export in
	 */
	void exportSpectrumViaMenu(String type) {
		if (!isSigned()) {
			Logger.info(exportSpectrum(type, -1));
			return;
		}
		String msg = Exporter.exportSpectra(getSelectedPanel(), offWindowFrame,
				jFileChooser, type, recentFileName, dirLastExported);
		if (msg != null)
			dirLastExported = msg;
	}

	/**
	 * Loads a new file into the existing applet window
	 * 
	 * @param tmpFilePath
	 *          String
	 */
	public void syncLoad(String filePath) {
		newAppletPanel();
		System.out.println("syncLoad reading " + filePath);
		openDataOrFile(null, null, null, filePath, -1, -1);
		jsvApplet.getContentPane().validate();
		spectrumPanel.validate();
	}

	/**
	 * Calls a javascript function given by the function name passing to it the
	 * string parameters as arguments
	 * 
	 * @param function
	 *          the javascript function name
	 * @param parameters
	 *          the function arguments as a string in the form "x, y, z..."
	 */
	private void callToJavaScript(String callback, Object[] params) {
		try {
			JSObject jso = JSObject.getWindow(jsvApplet);
			if (callback.length() > 0) {
				if (callback.indexOf(".") > 0) {
					String[] mods = TextFormat.split(callback, '.');
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
		ScriptCommandTokenizer allParamTokens = new ScriptCommandTokenizer(params,
				";\n");
		if (Logger.debugging) {
			Logger.info("Running in DEBUG mode");
		}
		while (allParamTokens.hasMoreTokens()) {
			String token = allParamTokens.nextToken();
			// now split the key/value pair
			StringTokenizer eachParam = new StringTokenizer(token);
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
					autoIntegrate = AwtParameters.isTrue(value);
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
					irMode = (value.toUpperCase().startsWith("T") ? JDXSpectrum.TO_TRANS
							: JDXSpectrum.TO_ABS);
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
				case VERSION:
					break;
				}
			} catch (Exception e) {
			}
		}
	}

	// for the signed applet to load a remote file, it must
	// be using a thread started by the initiating thread;
	private List<String> scriptQueue;

	private class CommandWatcher implements Runnable {
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
			List<JDXSpectrum> specs, String url, int firstSpec, int lastSpec) {
  	int status = JSVTree.openDataOrFile((ScriptInterface) this, data, name, specs, url, firstSpec, lastSpec);
  	if (status == JSVTree.FILE_OPEN_ALREADY)
  		return;
    if (status != JSVTree.FILE_OPEN_OK) {
    	setSelectedPanel(null);
    	return;
    }
    compoundMenuOn = allowCompoundMenu;

		appletPopupMenu.setCompoundMenu(getSelectedPanel(), panelNodes, compoundMenuOn, 
				null, null);//compoundMenuSelectionListener, compoundMenuChooseListener);

		Logger.info(jsvApplet.getAppletInfo() + " File " + currentSource.getFilePath()
				+ " Loaded Successfully");
		
	}

	
	protected void processCommand(String script) {
		runScriptNow(script);
	}

	// ///////// simple sync functionality //////////

	public boolean runScriptNow(String params) {
		return JSViewer.runScriptNow(this, params);
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
		int iSpec = spectrumPanel.getCurrentSpectrumIndex();
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
		spectrumPanel.setSelectedPanel(jsvp, panelNodes);
  	selectedPanel = jsvp;
		spectraTree.setSelectedPanel((ScriptInterface) this, jsvp);
    jsvApplet.validate();
		if (jsvp != null) {
      jsvp.setEnabled(true);
      jsvp.setFocusable(true);
		}
	}

	// /////////// MISC methods from interfaces /////////////

	// called by Pro's popup window Advanced...
	void doAdvanced(String filePath) {
		// only for JSVAppletPro
	}

	// //////////////// PanelEventInterface

	/**
	 * called by notifyPeakPickedListeners in JSVPanel
	 */
	public void panelEvent(Object eventObj) {
		if (eventObj instanceof PeakPickEvent) {
			JSViewer.processPeakPickEvent(this, eventObj, false);
		} else if (eventObj instanceof ZoomEvent) {
		} else if (eventObj instanceof SubSpecChangeEvent) {
		}
	}

	// ///////////// ScriptInterface execution from JSViewer.runScriptNow and
	// menus

	public JSVPanel getSelectedPanel() {
		return selectedPanel;
	}

	public void runScript(String script) {
		if (scriptQueue == null)
			processCommand(script);
		else
			scriptQueue.add(script);
	}

	public String execExport(JSVPanel jsvp, String value) {
		if (jsvp != null && isPro())
			writeStatus(Exporter.exportCmd(jsvp, ScriptToken.getTokens(value), false));
		return null;
	}

	public void execSetIntegrationRatios(String value) {
		// parse the string with a method in JSpecViewUtils
		integrationRatios = IntegralGraph.getIntegrationRatiosFromString(value);
	}

	/**
	 * Allows Integration of an HNMR spectrum
	 * 
	 */
	public void execIntegrate(JDXSpectrum spec) {
		if (spec.hasIntegral() && integrationRatios != null)
			spec.setIntegrationRatios(integrationRatios);
		integrationRatios = null; // first time only
	}

	/**
	 * Allows Transmittance to Absorbance conversion or vice versa depending on
	 * the value of comm.
	 * 
	 * @param comm
	 *          the conversion command
	 * @throws Exception
	 */

	public void execTAConvert(int comm) {
		// unnec
	}

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

	/**
	 * Calculates the predicted colour of the Spectrum
	 */
	public String setSolutionColor(boolean showMessage) {
		if (showMessage) {
			String msg = getSelectedPanel().getPanelData().getSolutionColorHtml();
			JOptionPane.showMessageDialog(jsvApplet, msg, "Predicted Colour",
					JOptionPane.INFORMATION_MESSAGE);
		}
		return getSelectedPanel().getPanelData().getSolutionColor();
	}

	public Parameters getParameters() {
		return parameters;
	}

	public void execClose(String value, boolean fromScript) {
		JSVTree.close((ScriptInterface) this, value);
    if (!fromScript)
    	validateAndRepaint();
	}

  public String execLoad(String value) {
  	//int nSpec = panelNodes.size();
  	JSVTree.load((ScriptInterface) this, value);
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

	public JSVPanel execSetSpectrum(String value) {
  	return JSVTree.setSpectrum((ScriptInterface) this, value);
	}

	public void execSetAutoIntegrate(boolean b) {
		autoIntegrate = b;
	}

	public PanelData getPanelData() {
		return getSelectedPanel().getPanelData();
	}

	public JSVDialog getOverlayLegend(JSVPanel jsvp) {
		return overlayLegendDialog = new OverlayLegendDialog(null, getSelectedPanel());
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
	
	
  private JSVTree spectraTree;
	private int fileCount;
	public int getFileCount() {
		return fileCount;
	}
	public void setFileCount(int n) {
		fileCount = n;
	}

  
  public Object getSpectraTree() {
  	return spectraTree;
  }
  
	public JSVPanelNode setOverlayVisibility(JSVPanelNode node) {
		JSViewer.setOverlayLegendVisibility(this, getSelectedPanel(),
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
  	JSVTree.closeSource(this, source);
	}

	public int incrementViewCount(int n) {
		return nViews += n;
	}
	
	public void process(List<JDXSpectrum> specs) {
    JDXSpectrum.process(specs, irMode, autoIntegrate, parameters);
	}
	
	public void setCursor(Cursor c) {
		jsvApplet.setCursor(c);
	}

	public void setRecentFileName(String fileName) {
		recentFileName = fileName;
	}

	public boolean getAutoCombine() {
		return interfaceOverlaid;
	}
	
	public URL getDocumentBase() {
		return jsvApplet.getDocumentBase();
	}

	public JDXSource createSource(String data, String filePath, URL base,
			int firstSpec, int lastSpec) throws Exception {
		return FileReader.createJDXSource(FileManager
				.getBufferedReaderForString(data), filePath, base,
				obscureTitleFromUser == Boolean.TRUE, -1, -1);
	}

	public JSVPanel getNewJSVPanel(List<JDXSpectrum> specs) {
		JSVPanel jsvp = AwtPanel.getJSVPanel(specs, initialStartIndex, initialEndIndex, appletPopupMenu);
		initialEndIndex = initialStartIndex = -1;
		jsvp.getPanelData().addListener(this);
		parameters.setFor(jsvp, null, true);
		return jsvp;
	}

	public JSVPanel getNewJSVPanel(JDXSpectrum spec) {
		System.out.println("getNewJSVPanel " + spec);
		if (spec == null) {
			initialEndIndex = initialStartIndex = -1;
			return null;
		}
		List<JDXSpectrum> specs = new ArrayList<JDXSpectrum>();
		specs.add(spec);
		JSVPanel jsvp = AwtPanel.getJSVPanel(specs, initialStartIndex, initialEndIndex, appletPopupMenu);
		jsvp.getPanelData().addListener(this);
		parameters.setFor(jsvp, null, true);
		return jsvp;
	}

	public JSVPanelNode getNewPanelNode(String id, String fileName, JDXSource source, JSVPanel jsvp) {
		System.out.println("getNewPanelNode " + fileName + " " + jsvp);
		return new JSVPanelNode(id, fileName, source, jsvp);	
	}

	// not implemented for applet
	
	public boolean getAutoShowLegend() {
		return false; //option?
	}

	public void checkOverlay() {
		if (spectrumPanel != null)
      spectrumPanel.markSelectedPanels(panelNodes);
		viewDialog = new ViewDialog(this, spectrumPanel, false);
	}

	private String returnFromJmolModel;

	public void setReturnFromJmolModel(String model) {
    returnFromJmolModel = model;		
	}

	public String getReturnFromJmolModel() {
    return returnFromJmolModel;		
	}

	// not applicable to applet:
	
	public void setLoaded(String fileName, String filePath) {} 
	public void setMenuEnables(JSVPanelNode node, boolean isSplit) {}
	public void setRecentURL(String filePath) {}
	public void setPropertiesFromPreferences(JSVPanel jsvp, boolean includeMeasures) {}
	public void updateRecentMenus(String filePath) {}

	// debugging

	public void execTest(String value) {
	}
}
