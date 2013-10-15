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
//          call. Changed behaviour to remove integration after reset view.
// 19-06-2012 BH -changes to printing calls
// 23-06-2012 BH -Major change to Applet code to allow multiple file loads
// 28-06-2012 BH -Overlay/close/view spectrum dialog working SVN 961
// 02-07-2012 BH -show distances between peaks
// 04-07-2012 BH -Ctrl- PgUp/PgDn for 2D spectra 
// 06-07-2012 BH -Views menu implemented
// 08-07-2012 BH -refactoring of Graph, add new NMR dialog boxes
// 14-07-2012 BH -IR peak listing
// 17-07-2012 BH -getProperty key
// 18-07-2012 BH -MAC fix for repaint

package jspecview.appletjs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.Txt;

import jspecview.api.AppletFrame;
import jspecview.api.JSVApiPlatform;
import jspecview.api.JSVAppletInterface;
import jspecview.api.JSVMainPanel;
import jspecview.api.JSVPanel;
import jspecview.app.JSVApp;
import jspecview.awtjs2d.JsPanel;
import jspecview.awtjs2d.JsViewPanel;
import jspecview.awtjs2d.JsPlatform;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVersion;
import jspecview.common.JSViewer;
/**
 * 
 * Entry point for the web.
 * 
 * JSpecView Applet class. For a list of parameters and scripting functionality
 * see the file JSpecView_Applet_Specification.html.
 * 
 * @author Bob Hanson
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A. D. Walters
 * @author Prof Robert J. Lancashire
 * 
 *         http://blog.gorges.us/2009/03/how-to-enable-keywords-in-eclipse-and-
 *         subversion-svn/ $LastChangedRevision: 1097 $ $LastChangedDate:
 *         2012-07-23 11:10:30 -0500 (Mon, 23 Jul 2012) $
 */

public class JSVApplet implements JSVAppletInterface,
		AppletFrame {

	protected JSVApp app;
	public JSViewer viewer;

	private boolean isStandalone = false;
	protected Map<String, Object> viewerOptions;
	private Map<String, Object> htParams;
	private JSVMainPanel spectrumPanel;

  public JSVApplet(Map<String, Object>viewerOptions) {
  	if (viewerOptions == null)
  		viewerOptions = new Hashtable<String, Object>();
  	this.viewerOptions = viewerOptions;
  	htParams = new Hashtable<String, Object>();
  	for (Map.Entry<String, Object> entry : viewerOptions.entrySet())
  		htParams.put(entry.getKey().toLowerCase(), entry.getValue());
    init();
  }

	/**
	 * 
	 * Initializes applet with parameters and load the <code>JDXSource</code>
	 * called by the browser
	 * 
	 */
	public void init() {
		app = new JSVApp(this);
		initViewer();
	}

	protected void initViewer() {
		viewer = app.viewer;
    setLogging();
    viewerOptions.remove("debug");
    Object o = viewerOptions.get("display");
    /**
     * @j2sNative
     * 
     *            o = document.getElementById(o);
     */
    {}
    viewer.setDisplay(o);
     
//	  app.viewer.scriptQueue = new JmolList<String>();
//		commandWatcherThread = new Thread(new CommandWatcher());
//		commandWatcherThread.setName("CommmandWatcherThread");
//		commandWatcherThread.start();
		Logger.info(getAppletInfo());
	}

  private void setLogging() {
    int iLevel = (getValue("logLevel", (getBooleanValue("debug", false) ? "5"
        : "4"))).charAt(0) - '0';
    if (iLevel != 4)
      System.out.println("setting logLevel=" + iLevel
          + " -- To change, use script \"set logLevel [0-5]\"");
    Logger.setLogLevel(iLevel);
  }

  public String getParameter(String paramName) {
    Object o = htParams.get(paramName.toLowerCase());
    return (o == null ? null : new String(o.toString()));
  }

  private boolean getBooleanValue(String propertyName, boolean defaultValue) {
    String value = getValue(propertyName, defaultValue ? "true" : "");
    return (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on") || value
        .equalsIgnoreCase("yes"));
  }

  private String getValue(String propertyName, String defaultValue) {
    String stringValue = getParameter(propertyName);
    System.out.println("getValue " + propertyName + " = " + stringValue);
    if (stringValue != null)
      return stringValue;
    return defaultValue;
  }

	private static final long serialVersionUID = 1L;

	public boolean isPro() {
		return app.isPro();
	}

	public boolean isSigned() {
		return app.isSigned();
	}

	// /////////// public methods called from page or browser ////////////////
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

	@Override
	public void finalize() {
		System.out.println("JSpecView " + this + " finalized");
	}

	public void destroy() {
//		if (commandWatcherThread != null) {
//			commandWatcherThread.interrupt();
//			commandWatcherThread = null;
//		}
		app.dispose();
		app = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#getParameter(java.lang.String,
	 * java.lang.String)
	 * 
	 * not used
	 */
	public String getParameter(String key, String def) {
		return isStandalone ? System.getProperty(key, def)
				: (getParameter(key) != null ? getParameter(key) : def);
	}

	/**
	 * Get Applet information
	 * 
	 * @return the String "JSpecView Applet"
	 */
	public String getAppletInfo() {
		return "JSpecView Applet " + JSVersion.VERSION;
	}

	// /////////////// JSpecView JavaScript calls ///////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#getSolnColour()
	 */

	public String getSolnColour() {
		return app.getSolnColour();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#getCoordinate()
	 */
	public String getCoordinate() {
		return app.getCoordinate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#loadInline(java.lang.String)
	 */
	public void loadInline(String data) {
		app.loadInline(data);
	}

	@Deprecated
	public String export(String type, int n) {
		return app.exportSpectrum(type, n);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#exportSpectrum(java.lang.String,
	 * int)
	 */
	public String exportSpectrum(String type, int n) {
		return app.exportSpectrum(type, n);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#setFilePath(java.lang.String)
	 */
	public void setFilePath(String tmpFilePath) {
		app.setFilePath(tmpFilePath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#setSpectrumNumber(int)
	 */
	public void setSpectrumNumber(int i) {
		app.setSpectrumNumber(i);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#toggleGrid()
	 */
	public void toggleGrid() {
		app.toggleGrid();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#toggleCoordinate()
	 */
	public void toggleCoordinate() {
		app.toggleCoordinate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#toggleIntegration()
	 */
	public void toggleIntegration() {
		app.toggleIntegration();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#addHighlight(double, double, int,
	 * int, int, int)
	 */
	public void addHighlight(double x1, double x2, int r, int g, int b, int a) {
		app.addHighlight(x1, x2, r, g, b, a);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#removeAllHighlights()
	 */
	public void removeAllHighlights() {
		app.removeAllHighlights();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#removeHighlight(double, double)
	 */
	public void removeHighlight(double x1, double x2) {
		app.removeHighlight(x1, x2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#reversePlot()
	 */
	public void reversePlot() {
		app.reversePlot();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#script(java.lang.String)
	 */
	@Deprecated
	public void script(String script) {
		app.initParams(script);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#runScript(java.lang.String)
	 */
	public void runScript(String script) {
		app.runScript(script);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#syncScript(java.lang.String)
	 */
	public void syncScript(String peakScript) {
		app.syncScript(peakScript);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jspecview.applet.JSVAppletInterface#writeStatus(java.lang.String)
	 */
	public void writeStatus(String msg) {
		app.writeStatus(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jspecview.applet.JSVAppletInterface#getPropertyAsJavaObject(java.lang.String
	 * )
	 */
	public Map<String, Object> getPropertyAsJavaObject(String key) {
		return app.getPropertyAsJavaObject(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jspecview.applet.JSVAppletInterface#getPropertyAsJSON(java.lang.String)
	 */
	public String getPropertyAsJSON(String key) {
		return app.getPropertyAsJSON(key);
	}

	public boolean runScriptNow(String script) {
		return app.runScriptNow(script);
	}

//	private DropTargetListener dtl;
//	private Component spectrumPanel;
//	private JFrame offWindowFrame;

	public void setDropTargetListener(boolean isSigned, JSViewer viewer) {
//		if (dtl == null && isSigned)
//			dtl = new JsDropTargetListener(viewer);
	}

	public void validateContent(int mode) {
//		if ((mode & 1) == 1)
//			getContentPane().validate();
//		if ((mode & 2) == 2)
//			spectrumPanel.validate();
	}

	public void addNewPanel(JSViewer viewer) {
		viewer.viewPanel = spectrumPanel = new JsViewPanel();
	}


	public void newWindow(boolean isSelected) {
//		if (isSelected) {
//			offWindowFrame = new JFrame("JSpecView");
//			offWindowFrame.setSize(getSize());
//			final Dimension d = spectrumPanel.getSize();
//			offWindowFrame.add(spectrumPanel);
//			offWindowFrame.validate();
//			offWindowFrame.setVisible(true);
//			remove(spectrumPanel);
//			app.siValidateAndRepaint();
//			offWindowFrame.addWindowListener(new WindowAdapter() {
//				@Override
//				public void windowClosing(WindowEvent e) {
//					windowClosingEvent(d);
//				}
//			});
//		} else {
//			getContentPane().add(spectrumPanel);
//			app.siValidateAndRepaint();
//			offWindowFrame.removeAll();
//			offWindowFrame.dispose();
//			offWindowFrame = null;
//		}
	}

//	protected void windowClosingEvent(Dimension d) {
//		spectrumPanel.setSize(d);
//		getContentPane().add(spectrumPanel);
//		setVisible(true);
//		app.siValidateAndRepaint();
//		offWindowFrame.removeAll();
//		offWindowFrame.dispose();
//		app.newWindow(false, true);
//	}

	/**
	 * Calls a javascript function given by the function name passing to it the
	 * string parameters as arguments
	 * 
	 * @param callback
	 * @param data
	 * 
	 */
	public void callToJavaScript(String callback, Object[] data) {
 	 String[] tokens = Txt.split(callback, ".");
	 	/**
	 	 * @j2sNative
	 	 * 
	 	 * try{
	 	 *   var o = window[tokens[0]]
	 	 *   for (i = 1; i < tokens.length; i++){
	 	 *     o = o[tokens[i]]
	 	 *   }
	 	 *   return o(data[0],data[1],data[2],data[3],data[4],data[5],data[6],data[7],data[8],data[9]);
	 	 * } catch (e) {
	 	 *	 System.out.println(callback + " failed " + e);
	 	 * }
	 	 */
	 	{
	 		System.out.println(tokens + " " + data);
	 	}
	}

	public void setPanelVisible(boolean b) {
//		spectrumPanel.setVisible(b);
	}

	public JSVPanel getJSVPanel(JSViewer viewer, JmolList<JDXSpectrum> specs,
			int initialStartIndex, int initialEndIndex) {
		return JsPanel.getPanelMany(viewer, specs, initialStartIndex,
				initialEndIndex);
	}

	public void setVisible(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public URL getDocumentBase() {
		String base = (String) viewerOptions.get("documentBase");
		try {
			return new URL((URL) null, base, null);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public void repaint() {
		// TODO Auto-generated method stub
		
	}

	public void validate() {
		// TODO Auto-generated method stub
		
	}

	public JSVApiPlatform getApiPlatform() {
		return new JsPlatform();
	}

	public void doExitJmol() {
		// ignore
	}

	public JSVApp getApp() {
		return app;
	}

}