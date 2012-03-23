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
//			  		x scale, and y scale.  Added parameteres for the font,
//			  		title font, and integral plot color.  Added a method
//			  		to reset view from a javascript call.
// 24-09-2011 jak - Added parameter for integration ratio annotations.
// 08-10-2011 jak - Add a method to toggle integration from a javascript
//					call. Changed behaviour to remove integration after reset
//					view.

package jspecview.applet;

import java.util.Properties;

import jspecview.application.MainFrame;
import jspecview.common.JSVPanel;

import org.jmol.api.JSVInterface;



 /** A signed applet that has an Advanced... menu item that pulls up a MainFrame
 * 
 * @author Bob Hanson St. Olaf College hansonr@stolaf.edu
 */

public class JSVAppletPro extends JSVApplet implements JSVInterface {

  /*  class interactions:
   * 
      //           JSVAppletPro    JSpecView
      //             /   |    \       /
      //     [extension] |  [instantiation]
      //           /     |      \   /
      //      JSVApplet  |    MainFrame
      //           \     |        \
      //         [JavaScript]   [interface]
      //             \   |          \
      //           JmolApplet       Jmol
      // 
   * 
   * JSVAppletPro and JSpecView can create a MainFrame
   * MainFrame can interface with Jmol via JmolSyncInterface and JSVInterface
   * JSVAppletPro and JSVApplet can interact with JmolApplet via JavaScript callbacks
   * 
   */
  
  private static final long serialVersionUID = 1L;

  private MainFrame mainFrame;

  @Override
  public void init() {
    super.init();
  }

  @Override
  public boolean isPro() {
    return true;
  }
  
  @Override
  public String getAppletInfo() {
    return super.getAppletInfo() + " (PRO)";
  }
  
  /**
   * JSVAppletPro uses "script()" for executing a real script, 
   * not a parameter initialization. "runScript()" will also work
   * 
   */
  
  @Override
  public void script(String script) {
    runScript(script);
  }

  @Override
  public void loadInline(String data) {
    if (mainFrame != null && mainFrame.isVisible())
      mainFrame.loadInline(data);
    else
      super.loadInline(data);      
  }

  @Override
  public void setSpectrumNumber(int n) {
    if (mainFrame != null && mainFrame.isVisible())
      mainFrame.setSpectrumNumberAndTreeNode(n);
    else
      super.setSpectrumNumber(n);      
  }

  @Override
  public void syncScript(String script) {
    if (mainFrame != null && mainFrame.isVisible())
      mainFrame.syncScript(script);
    else
      super.syncScript(script);      
  }

  @Override
  public void writeStatus(String msg) {
    if (mainFrame != null && mainFrame.isVisible())
      mainFrame.writeStatus(msg);
    else
      super.writeStatus(msg);
  }

  public JSVPanel getSelectedPanel() {
    return (mainFrame != null && mainFrame.isVisible() ? mainFrame.getSelectedPanel() 
        : super.getSelectedPanel());
  }
  
  /**
   * executed only by the command processor and menu actions
   * present only so that processed commands are sent to the right place
   * 
   */
  @Override
  protected void processCommand(String script) {
    if (mainFrame != null && mainFrame.isVisible())
      mainFrame.runScriptNow(script);
    else
      super.processCommand(script);
  } 
   
  /////////// JSVInterface ////////////
  
  void doAdvanced(String filePath) {
    if (mainFrame == null) {
      mainFrame = new MainFrame(this);
    }
    mainFrame.runScript("load \"" + filePath + "\"");
    mainFrame.setVisible(true);
  }

  public void exitJSpecView(boolean withDialog, Object frame) {
    mainFrame.setVisible(false);
  }

  public void saveProperties(Properties properties) {
  }

  public void setProperties(Properties properties) {
  }

  public void syncToJmol(String msg) {
    super.syncToJmol(msg);
  }

}
