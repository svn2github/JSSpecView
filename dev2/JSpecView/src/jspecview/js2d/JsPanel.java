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

package jspecview.js2d;

import java.io.OutputStream;

import javajs.api.GenericColor;
import javajs.api.GenericFileInterface;
import javajs.api.GenericMouseInterface;
import javajs.api.GenericPlatform;
import javajs.awt.Font;
import javajs.util.List;

import org.jmol.util.Logger;

import jspecview.api.JSVPanel;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSViewer;
import jspecview.common.PDFWriter;
import jspecview.common.PanelData;
import jspecview.common.ColorParameters;
import jspecview.common.PrintLayout;
import jspecview.common.ScriptToken;


/**
 * JSVPanel class represents a View combining one or more GraphSets, each with one or more JDXSpectra.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class JsPanel implements JSVPanel {
//, MouseListener,  MouseMotionListener, KeyListener
  private static final long serialVersionUID = 1L;

  @Override
  public void finalize() {
    Logger.info("JSVPanel " + this + " finalized");
  }

	private GenericPlatform apiPlatform;
	public GenericPlatform getApiPlatform() {
		return apiPlatform;
	}
	
  private PanelData pd;
  public PanelData getPanelData() {
    return pd;
  }

	private GenericMouseInterface mouse;
	private JSViewer viewer;

	String name;

  /**
   * Constructs a new JSVPanel
   * @param viewer 
   * 
   * @param spectrum
   *        the spectrum
   * @return this
   */
  public static JsPanel getPanelOne(JSViewer viewer, JDXSpectrum spectrum) {
    // standard applet not overlaid and not showing range
    // standard application split spectra
    // removal of integration, taConvert
    // Preferences Dialog sample.jdx
//  	ToolTipManager.sharedInstance().setInitialDelay(0);
  	JsPanel p = new JsPanel(viewer);
    p.pd.initOne(spectrum);
    return p;
  }

	/**
   * Constructs a <code>JSVPanel</code> with List of spectra and corresponding
   * start and end indices of data points that should be displayed
   * @param viewer 
   * 
   * @param spectra
   *        the List of <code>Graph</code> instances
   * @param startIndex
   *        the start index
   * @param endIndex
   *        the end index
   * @return this
   */
  public static JsPanel getPanelMany(JSViewer viewer, List<JDXSpectrum> spectra, int startIndex, int endIndex) {
  	JsPanel p = new JsPanel(viewer);
    p.pd.initMany(spectra, startIndex, endIndex);
    return p;
  }

  private JsPanel(JSViewer viewer) {
  	this.viewer = viewer;
    this.pd = new PanelData(this, viewer);
  	apiPlatform = viewer.apiPlatform;
    mouse = apiPlatform.getMouseManager(0, this);
//  setBorder(BorderFactory.createLineBorder(Color.BLACK));
	}

  public String getTitle() {
  	return pd.getTitle();
  }
  
  public void dispose() {
    //toolTip = null;
    if (pd != null)
      pd.dispose();
    pd = null;
    mouse.dispose();
  }

  public void setTitle(String title) {
    pd.title = title;
    this.name = title;
  }

	public void setColorOrFont(ColorParameters ds, ScriptToken st) {
  	pd.setColorOrFont(ds, st);
  }

  public void setBackgroundColor(GenericColor color) {
  
  	// unnecessary
  }
  
	///// threading and focus
	
  public String getInput(String message, String title, String sval) {
  	String ret = null;
  	/**
  	 * @j2sNative
  	 * 
  	 * ret = prompt(message, sval);
  	 */
  	{
  	}
    getFocusNow(true);
    return ret;
  }

	public void showMessage(String msg, String title) {
		Logger.info(msg);
		/**
		 * @j2sNative
		 * 
		 * alert(msg);
		 */
		{			
		}
		getFocusNow(true);
	}

	public void getFocusNow(boolean asThread) {
    pd.dialogsToFront();
	}

	public int getFontFaceID(String name) {
		return Font.getFontFaceID("SansSerif");
	}
	
  /*----------------------- JSVPanel PAINTING METHODS ---------------------*/

  public void doRepaint(boolean andTaintAll) {
  	pd.taintedAll |= andTaintAll;
  	// from dialogs, to the system
  	if (!pd.isPrinting)
      viewer.requestRepaint();
  }
  
//  @Override
//	public void update(Graphics g) {
//  	// from the system
//  	// System: Do not clear rectangle -- we are opaque and will take care of that.
//  	// seems unnecessary, but apparently for the Mac it is critical. Still not totally convinced!
//      paint(g);
//  }
 

  /**
   * Overrides paintComponent in class JPanel in order to draw the spectrum
   * 
   * @param context
   *        the canvas's context
   */
  public void paintComponent(Object context) {
  	
  	// from the system, via update or applet/app repaint
  	
    if (viewer == null || pd == null || pd.graphSets == null || pd.isPrinting)
      return;
    Object context2 = null;
    /**
     * @j2sNative
     * 
     * 
     * context2 = context.canvas.topLayer.getContext("2d");
     * 
     */
    {}
    pd.g2d = pd.g2d0;
    pd.drawGraph(context, context2, getWidth(), getHeight(), false);
    viewer.repaintDone();
  }

  /*----------------- METHODS IN INTERFACE Printable ---------------------- */

	/**
	 * Send a print job of the spectrum to the default printer on the system
	 * 
	 * @param pl
	 *          the layout of the print job
	 * @param os
	 * @param title
	 */
	public void printPanel(PrintLayout pl, OutputStream os, String title) {
		pl.title = title;
		pl.date = apiPlatform.getDateFormat(true);
		pd.setPrint(pl, "Helvetica");
		try {
			(new PDFWriter()).createPdfDocument(this, pl, os);
  	} catch (Exception ex) {
  		showMessage(ex.toString(), "creating PDF");
  	} finally {
			pd.setPrint(null, null);
  	}
	}

	public String saveImage(String type, GenericFileInterface file) {
		// handled in Export
		return null;
	}

	public boolean hasFocus() {
		// TODO Auto-generated method stub
		return false;
	}

	public void repaint() {
		// TODO Auto-generated method stub
		
	}

	public void setToolTipText(String s) {
		// TODO Auto-generated method stub
		
	}

	public int getHeight() {
		// TODO Auto-generated method stub
		return viewer.getHeight();
	}

	public int getWidth() {
		return viewer.getWidth();
	}

	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isFocusable() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isVisible() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setEnabled(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void setFocusable(boolean b) {
		// TODO Auto-generated method stub
		
	}

  @Override
  public String toString() {
    return pd.getSpectrumAt(0).toString();
  }

  /**
   * called only by JavaScript
   * 
   * @param id
   * @param x
   * @param y
   * @param modifiers
   * @param time
   * @return t/f
   */
  public boolean processMouseEvent(int id, int x, int y, int modifiers, long time) {
  	return mouse.processEvent(id, x, y, modifiers, time);
  }

	public void processTwoPointGesture(float[][][] touches) {
		mouse.processTwoPointGesture(touches);
	}

}
