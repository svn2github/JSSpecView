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
//          call. Changed behaviour to remove integration after reset
//          view.

package jspecview.applet;

import java.util.Map;

import javax.swing.JApplet;

import jspecview.common.JSVAppletInterface;

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

public class JSVApplet extends JApplet implements JSVAppletInterface {

  protected JSVAppletInterface appletPrivate;
  private boolean isStandalone = false;

  /**
   * Initializes applet with parameters and load the <code>JDXSource</code> 
   * called by the browser
   * 
   */
  @Override
  public void init() {
    appletPrivate = new JSVAppletPrivate(this);
  }

  public static final String APPLET_VERSION = "2.0.20120329-0500"; //
//  2.0.yyyymmdd-hhmm format - should be updated to keep track of the latest version (based on Jamaica time)
  
  private static final long serialVersionUID = 1L;

  public boolean isPro() {
    return appletPrivate.isPro();
  }
  
  public boolean isSigned() {
    return appletPrivate.isSigned();
  }
  
///////////// public methods called from page or browser ////////////////
  //
  //
  // Notice that in all of these we use getSelectedPanel(), not selectedJSVPanel
  // That's because the methods aren't overridden in JSVAppletPro, and in that case
  // we want to select the panel from MainFrame, not here. Thus, when the Advanced...
  // tab is open, actions from outside of Jmol act on the MainFrame, not here. 
  //
  // BH - 8.3.2012
  
  @Override
  public void finalize() {
    System.out.println("JSpecView " + this + " finalized");
  }

  @Override
  public void destroy() {
    ((JSVAppletPrivate) appletPrivate).dispose();
    appletPrivate = null;
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#getParameter(java.lang.String, java.lang.String)
   */
  public String getParameter(String key, String def) {
    return isStandalone ? System.getProperty(key, def)
        : (getParameter(key) != null ? getParameter(key) : def);
  }

  /**
   * Returns the current internal version of the Applet
   * 
   * @return String
   */
  public String getAppletVersion() {
    return JSVApplet.APPLET_VERSION;
  }

  /**
   * Get Applet information
   * 
   * @return the String "JSpecView Applet"
   */
  @Override
  public String getAppletInfo() {
    return "JSpecView Applet " + getAppletVersion();
  }
  
  ///////////////// JSpecView JavaScript calls ///////////////////
  
  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#getSolnColour()
   */

  public String getSolnColour() {
    return appletPrivate.getSolnColour();
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#getCoordinate()
   */
  public String getCoordinate() {
    return appletPrivate.getCoordinate();
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#loadInline(java.lang.String)
   */
  public void loadInline(String data) {
    appletPrivate.loadInline(data);
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#export(java.lang.String, int)
   */
  public String export(String type, int n) {
    return appletPrivate.export(type, n);
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#setFilePath(java.lang.String)
   */
  public void setFilePath(String tmpFilePath) {
    appletPrivate.setFilePath(tmpFilePath);
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#setSpectrumNumber(int)
   */
  public void setSpectrumNumber(int i) {
    appletPrivate.setSpectrumNumber(i);
  }
  
  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#toggleGrid()
   */
  public void toggleGrid() {
    appletPrivate.toggleGrid();
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#toggleCoordinate()
   */
  public void toggleCoordinate() {
    appletPrivate.toggleCoordinate();
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#toggleIntegration()
   */
  public void toggleIntegration() {
    appletPrivate.toggleIntegration();
  }


  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#addHighlight(double, double, int, int, int, int)
   */
  public void addHighlight(double x1, double x2, int r, int g, int b, int a) {
    appletPrivate.addHighlight(x1, x2, r, g, b, a);
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#removeAllHighlights()
   */
  public void removeAllHighlights() {
    appletPrivate.removeAllHighlights();
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#removeHighlight(double, double)
   */
  public void removeHighlight(double x1, double x2) {
    appletPrivate.removeHighlight(x1, x2);
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#reversePlot()
   */
  public void reversePlot() {
    appletPrivate.reversePlot();
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#script(java.lang.String)
   */
  @Deprecated
  public void script(String script) {
    ((JSVAppletPrivate) appletPrivate).initParams(script);
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#runScript(java.lang.String)
   */
  public void runScript(String script) {
    appletPrivate.runScript(script);
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#syncScript(java.lang.String)
   */
  public void syncScript(String peakScript) {
    appletPrivate.syncScript(peakScript);
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#writeStatus(java.lang.String)
   */
  public void writeStatus(String msg) {
    appletPrivate.writeStatus(msg);
  }

  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#getPropertyAsJavaObject(java.lang.String)
   */
  public Map<String, Object> getPropertyAsJavaObject(String key) {
    return appletPrivate.getPropertyAsJavaObject(key);
  }
  
  /* (non-Javadoc)
   * @see jspecview.applet.JSVAppletInterface#getPropertyAsJSON(java.lang.String)
   */
  public String getPropertyAsJSON(String key) {
    return appletPrivate.getPropertyAsJSON(key);
  }

  public void runScriptNow(String script) {
    appletPrivate.runScriptNow(script);    
  }

}
