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

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet; //import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;

import jspecview.common.Annotation.AType;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.export.Exporter;
import jspecview.util.Logger;

/**
 * JSVPanel class represents a View combining one or more GraphSets, each with one or more JDXSpectra.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class AwtPanel extends JPanel implements JSVPanel, Printable, MouseListener,
    MouseMotionListener, KeyListener {

  private static final long serialVersionUID = 1L;

  @Override
  public void finalize() {
    Logger.info("JSVPanel " + this + " finalized");
  }

  public PanelData pd;

  public PanelData getPanelData() {
    return pd;
  }

  JSVPopupMenu popup;
  
  public JSVPopupMenu getPopup() {
    return popup;
  }

  public void dispose() {
    if (popup != null) {
      popup.dispose();
      popup = null;
    }
    //toolTip = null;
    pd.dispose();
    pd = null;
    removeKeyListener(this);
    removeMouseListener(this);
    removeMouseMotionListener(this);
  }
//
//  class AwtToolTip extends JToolTip {
//
//		private static final long serialVersionUID = 1L;
//
//		AwtToolTip(AwtPanel panel) {
//			super();
//			setComponent(panel);
//			addKeyListener(panel);
//			setBackground(Color.blue);
//		}
//		
//  }
  
  public JDXSpectrum getSpectrum() {
    return pd.getSpectrum();
  }
  
  public void setSpectrum(JDXSpectrum spec) {
    pd.setSpectrum(spec);    
  }
  
  public JDXSpectrum getSpectrumAt(int i) {
    return pd.getSpectrumAt(i);
  }
  
  public void setTitle(String title) {
    pd.setTitle(title);
    setName(title);
  }

  protected void doRepaint() {
    repaint();    
  }

  ////////// settable colors //////////

  private Color coordinatesColor;
  private Color gridColor;
  private Color integralPlotColor;
  private Color peakTabColor;
  private Color plotAreaColor;
  private Color scaleColor;
  private Color titleColor;
  private Color unitsColor;
  // potentially settable; 

  private Color highlightColor = new Color(255, 0, 0, 200);
  private Color zoomBoxColor = new Color(100, 100, 50, 130);
	private String viewTitle;
	private static int MAC_COMMAND = InputEvent.BUTTON1_MASK + InputEvent.BUTTON3_MASK;

  public void setPlotColors(Object oColors) {
    Color[] colors = (Color[]) oColors;
    for (int i = pd.graphSets.size(); --i >= 0;)
      pd.graphSets.get(i).setPlotColors(colors);
  }


  public void setColorOrFont(Parameters ds, ScriptToken st) {
    if (st == null) {
      Map<ScriptToken, Object> colors = ds.getColors();
      for (Map.Entry<ScriptToken, Object> entry : colors.entrySet())
        setColorOrFont(ds, entry.getKey());
      setColorOrFont(ds, ScriptToken.DISPLAYFONTNAME);
      setColorOrFont(ds, ScriptToken.TITLEFONTNAME);
      return;
    }
    switch (st) {
    case DISPLAYFONTNAME:
      pd.setFontName(st, ds.getDisplayFont());
      return;
    case TITLEFONTNAME:
      pd.setFontName(st, ds.getTitleFont());
      return;
    }
    setColor(st, ds.getColor(st));
  }

  public void setColor(ScriptToken st, Object oColor) {
    Color color = (Color) oColor;
    if (color != null)
      pd.options.put(st, AwtParameters.colorToHexString(color));
    switch (st) {
    case BACKGROUNDCOLOR:
      setBackground(color);
      break;
    case COORDINATESCOLOR:
      coordinatesColor = color;
      break;
    case GRIDCOLOR:
      gridColor = color;
      break;
    case HIGHLIGHTCOLOR:
      highlightColor = color;
      break;
    case INTEGRALPLOTCOLOR:
      integralPlotColor = color;
      break;
    case PEAKTABCOLOR:
    	peakTabColor = color;
    	break;
    case PLOTCOLOR:
      for (int i = pd.graphSets.size(); --i >= 0;)
        pd.graphSets.get(i).setPlotColor0(color);
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
    default:
      Logger.warn("AwtPanel --- unrecognized color: " + st);
      break;
    }
  }

  /**
   * Constructs a new JSVPanel
   * 
   * @param spectrum
   *        the spectrum
   * @throws ScalesIncompatibleException
   */
  public AwtPanel(JDXSpectrum spectrum, JSVPopupMenu popup) {
    // standard applet not overlaid and not showing range
    // standard application split spectra
    // removal of integration, taConvert
    // Preferences Dialog sample.jdx
  	ToolTipManager.sharedInstance().setInitialDelay(0);
  	//toolTip = new AwtToolTip(this);
    pd = new PanelData(this);
    this.popup = popup;
    pd.initSingleSpectrum(spectrum);
  }

  public JSVPanel getNewPanel(JDXSpectrum spectrum) {
    return new AwtPanel(spectrum, popup);
  }

  public static AwtPanel getJSVPanel(List<JDXSpectrum> specs, int startIndex, int endIndex, JSVPopupMenu popup) {
    return new AwtPanel(specs, startIndex, endIndex, popup);
  }

  /**
   * Constructs a <code>JSVPanel</code> with List of spectra and corresponding
   * start and end indices of data points that should be displayed
   * 
   * @param spectra
   *        the List of <code>Graph</code> instances
   * @param startIndices
   *        the start indices
   * @param endIndices
   *        the end indices
   * @throws JSpecViewException
   * @throws ScalesIncompatibleException
   */
  private AwtPanel(List<JDXSpectrum> spectra, int startIndex,
      int endIndex, JSVPopupMenu popup) {
    pd = new PanelData(this);
    this.popup = popup;
  	//toolTip = new AwtToolTip(this);
    pd.initJSVPanel(spectra, startIndex, endIndex);
  }

  /**
   * generates a single panel or an integrated panel, as appropriate
   * 
   * @param spec
   * @param jsvpPopupMenu
   * @return
   */
  public static AwtPanel getNewPanel(JDXSpectrum spec,
                                     JSVPopupMenu jsvpPopupMenu) {
    return new AwtPanel(spec, jsvpPopupMenu);
  }

  public GraphSet getNewGraphSet(GraphSet superSet) {
    return new AwtGraphSet(this, superSet);
  }

  /**
   * Returns the color of the plot at a certain index
   * 
   * @param index
   *        the index
   * @return the color of the plot
   */
  public Color getPlotColor(int index) {
    return ((AwtGraphSet) pd.getCurrentGraphSet()).getPlotColor(index);
  }

  public Color getColor(int r, int g, int b, int a) {
    return new Color(r, g, b, a);
  }
  
  public Color getColor(ScriptToken whatColor) {
    switch (whatColor) {
    default:
      Logger.error("awtgraphset missing color " + whatColor);
      return Color.BLACK;
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

  /*----------------------- JSVPanel PAINTING METHODS ---------------------*/

  /**
   * Overides paintComponent in class JPanel in order to draw the spectrum
   * 
   * @param g
   *        the <code>Graphics</code> object
   */
  @Override
  public void paintComponent(Graphics g) {
    if (pd == null || pd.graphSets == null)
      return;
    super.paintComponent(g);
    pd.drawGraph(g, getHeight(), getWidth());
  }

  public void setFont(Object g, String name, int width, int mode, int size,  
  		boolean isLabel) {
    if (isLabel) {
      if (width < 400)
        size = (int) ((width * size) / 400);
    } else {
      if (width < 250)
        size = (int) ((width * size) / 250);
    }
    ((Graphics) g).setFont(new Font(name, mode, size));
  }

  /**
   * Draws Title
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  public void drawTitle(Object og, int height, int width, String title) {
  	title = title.replace('\n', ' ');
    Graphics g = (Graphics) og;
    pd.setFont(g, width, pd.isPrinting || pd.getBoolean(ScriptToken.TITLEBOLDON) ? Font.BOLD
        : Font.PLAIN, 14, true);
    FontMetrics fm = g.getFontMetrics();
    int nPixels = fm.stringWidth(title);
    if (nPixels > width) {
    	int size = (int) (14.0 * width / nPixels);
    	if (size < 10)
    		size = 10;
      pd.setFont(g, width, pd.isPrinting || pd.getBoolean(ScriptToken.TITLEBOLDON) ? Font.BOLD
          : Font.PLAIN, size, true);
      fm = g.getFontMetrics();
    }
    g.setColor(titleColor);
    g.drawString(title, 5, (int) (height - fm.getHeight() / 2));
  }

  /**
   * Draws the Coordinates
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param height
   *        the height to be drawn in pixels
   * @param width
   *        the width to be drawn in pixels
   */
  public void drawCoordinates(Object og, int height, int width) {
  	if (pd.coordStr == null)
  		return;
    Graphics g = (Graphics) og;
    g.setColor(coordinatesColor);
    pd.setFont(g, width, Font.PLAIN, 12, true);
    g.drawString(pd.coordStr, (int) ((pd.plotAreaWidth + pd.leftPlotAreaPos) * 0.85),
        (int) (pd.topPlotAreaPos - 10));
  }

  /*----------------- METHODS IN INTERFACE Printable ---------------------- */

  /**
   * Implements method print in interface printable
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param pf
   *        the <code>PageFormat</code> object
   * @param pi
   *        the page index
   * @return an int that depends on whether a print was successful
   * @throws PrinterException
   */
  public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
    if (pi == 0) {
      Graphics2D g2D = (Graphics2D) g;
      pd.isPrinting = true;

      double height, width;

      if (pd.printGraphPosition.equals("default")) {
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        if (pf.getOrientation() == PageFormat.PORTRAIT) {
          height = pd.defaultHeight;
          width = pd.defaultWidth;
        } else {
          height = pd.defaultWidth;
          width = pd.defaultHeight;
        }
      } else if (pd.printGraphPosition.equals("fit to page")) {
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        height = pf.getImageableHeight();
        width = pf.getImageableWidth();
      } else { // center
        Paper paper = pf.getPaper();
        double paperHeight = paper.getHeight();
        double paperWidth = paper.getWidth();
        int x, y;

        if (pf.getOrientation() == PageFormat.PORTRAIT) {
          height = pd.defaultHeight;
          width = pd.defaultWidth;
          x = (int) (paperWidth - width) / 2;
          y = (int) (paperHeight - height) / 2;
        } else {
          height = pd.defaultWidth;
          width = pd.defaultHeight;
          y = (int) (paperWidth - pd.defaultWidth) / 2;
          x = (int) (paperHeight - pd.defaultHeight) / 2;
        }
        g2D.translate(x, y);
      }

      pd.drawGraph(g2D, (int) height, (int) width);

      pd.isPrinting = false;
      return Printable.PAGE_EXISTS;
    }
    pd.isPrinting = false;
    return Printable.NO_SUCH_PAGE;
  }

  /*--------------------------------------------------------------------------*/

  /**
   * Send a print job of the spectrum to the default printer on the system
   * 
   * @param pl
   *        the layout of the print job
   */
  public void printSpectrum(PrintLayout pl) {

    PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();

    if (pl.layout.equals("landscape"))
      aset.add(OrientationRequested.LANDSCAPE);
    else
      aset.add(OrientationRequested.PORTRAIT);

    aset.add(pl.paper);

    //MediaSize size = MediaSize.getMediaSizeForName(pl.paper);

    // Set Graph Properties
    pd.printingFont = pl.font;
    pd.printGraphPosition = pl.position;

    // save original values
    
    boolean gridOn = pd.gridOn;
    boolean titleOn = pd.titleOn;
    boolean xScaleOn = pd.getBoolean(ScriptToken.XSCALEON); 
    boolean xUnitsOn = pd.getBoolean(ScriptToken.XUNITSON); 
    boolean yScaleOn = pd.getBoolean(ScriptToken.YSCALEON); 
    
    pd.gridOn = pl.showGrid;
    pd.titleOn = pl.showTitle;
    pd.setBoolean(ScriptToken.XSCALEON, pl.showXScale);
    pd.setBoolean(ScriptToken.XUNITSON, pl.showXScale);
    pd.setBoolean(ScriptToken.YSCALEON, pl.showYScale);

    /* Create a print job */
    PrinterJob pj = PrinterJob.getPrinterJob();
    //    PageFormat pf = pj.defaultPage();
    //    pf.setOrientation(PageFormat.LANDSCAPE);
    //    pf = pj.pageDialog(pf);
    //
    //    PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
    //
    //    if(pf.getOrientation() == pf.LANDSCAPE){
    //      aset.add(OrientationRequested.LANDSCAPE);
    //    }else{
    //      aset.add(OrientationRequested.PORTRAIT);
    //    }

    pj.setPrintable(this);

    if (pj.printDialog()) {
      try {
        pj.print(aset);
      } catch (PrinterException ex) {
        ex.printStackTrace();
      }
    }

    // restore original values
    
    pd.gridOn = gridOn;
    pd.titleOn = titleOn;
    pd.setBoolean(ScriptToken.XSCALEON, xScaleOn); 
    pd.setBoolean(ScriptToken.XUNITSON, xUnitsOn); 
    pd.setBoolean(ScriptToken.YSCALEON, yScaleOn);
    
  }


  /*--------------the rest are all mouse and keyboard interface -----------------------*/

  public void mousePressed(MouseEvent e) {
  	System.out.println("mousePress " + isControlDown(e) + " " + e);
    if (e.getButton() != MouseEvent.BUTTON1)
      return;
    pd.doMousePressed(e.getX(), e.getY(), isControlDown(e), e.isShiftDown());
  }

  private boolean isControlDown(InputEvent e) {
  	// Mac does not allow Ctrl-drag. The CMD key is indicated using code 157 
  	return pd.ctrlPressed |= e.isControlDown() || (e.getModifiers() & MAC_COMMAND ) == MAC_COMMAND;
	}

	public void mouseMoved(MouseEvent e) {
    getFocusNow();
		if (e.getButton() != 0) {
			mouseDragged(e);
			return;
		}
  	System.out.println("mouseMoved " + isControlDown(e) + " " + e);
    pd.doMouseMoved(e.getX(), e.getY());
  }

  public void mouseDragged(MouseEvent e) {
  	System.out.println("mouseDragged " + isControlDown(e) + " " + e);
    pd.doMouseDragged(e.getX(), e.getY());
  }
  
  public void mouseReleased(MouseEvent e) {
    pd.doMouseReleased(e.getButton() == MouseEvent.BUTTON1);
  }

  public void mouseClicked(MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON3) {
      popup.show((JSVPanel) this, e.getX(), e.getY());
      return;
    }
    pd.doMouseClicked(e.getX(), e.getY(), e.getClickCount(), e.isControlDown());
  }

  public void mouseEntered(MouseEvent e) {
    getFocusNow();
  }

  public void mouseExited(MouseEvent e) {
  }

	public void keyPressed(KeyEvent e) {
		checkControl(e, true);
		
    if (!pd.ctrlPressed)
      System.out.println("awtpanel keypress " + e);

		// should be only in panel region, though.
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE
				|| e.getKeyCode() == KeyEvent.VK_DELETE) {
			pd.escapeKeyPressed(e.getKeyCode() == KeyEvent.VK_DELETE);
			pd.isIntegralDrag = false;
			repaint();
			e.consume();
			return;
		}
		int code = e.getKeyCode();
		if (e.getModifiers() != 0) {
			if (e.isControlDown()) {
				switch (code) {
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_UP:
				case 45: // '-'
				case 61: // '=/+'
					pd.scaleYBy(code == 61 || code == KeyEvent.VK_UP ? GraphSet.RT2 : 1/GraphSet.RT2);
					e.consume();
					break;
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_RIGHT:
					pd.toPeak(code == KeyEvent.VK_RIGHT ? 1 :-1);
					e.consume();
					break;
				}
			}
			return;
		}
		switch (code) {
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_RIGHT:
			pd.doMouseMoved((code == KeyEvent.VK_RIGHT ? ++pd.mouseX : --pd.mouseX), pd.mouseY);
			e.consume();
			repaint();
			break;
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_UP:
			int dir = (code == KeyEvent.VK_DOWN ? -1 : 1);
			if (pd.getSpectrumAt(0).getSubSpectra() == null) {
				pd.notifySubSpectrumChange(dir, null);
			} else {
				pd.advanceSubSpectrum(dir);
				repaint();
			}
			e.consume();
			break;
		}

	}

	public void keyReleased(KeyEvent e) {
		checkControl(e, false);
  }

  private void checkControl(KeyEvent e, boolean isPressed) {
  	switch(e.getKeyCode()) {
  	case KeyEvent.VK_CONTROL:
  	case KeyEvent.VK_META:
    	pd.ctrlPressed = isPressed;
    	break;
    default:
    	pd.ctrlPressed = isControlDown(e);
  	}
	}

  public void keyTyped(KeyEvent e) {
  	if (e.getKeyChar() == 'n') {
  		if (pd.getSelectedIntegral() != null)
  			normalizeIntegral();
  		e.consume();
  		return;
  	}
    if (e.getKeyChar() == 'z') {
      pd.previousView();
      e.consume();
      return;
    }
    if (e.getKeyChar() == 'y') {
      pd.nextView();
      e.consume();
      return;
    }
  }

	private void normalizeIntegral() {
    String sValue = pd.getSelectedIntegralText();
    if (sValue.length() == 0)
    	return;
		String newValue = getInput("Enter a new value for this integral", 
				"Normalize Integral", sValue);
		double val;
		try {
			val = Double.parseDouble(newValue);
		} catch (Exception e) {
			return;
		}
		if (val <= 0)
			return;
    pd.setSelectedIntegral(val);
    repaint();
	}

  public void setupPlatform() {
    setBorder(BorderFactory.createLineBorder(Color.lightGray));
    if (popup == null) {
      // preferences dialog
      pd.coordStr = "(0,0)";
    } else {
      addKeyListener(this);
      addMouseListener(this);
      addMouseMotionListener(this);
    }
  }

  public String getTitle() {
    return pd.getTitle();
  }

  public void paint(Object graphics) {
    super.paint((Graphics) graphics);
  }

  public String export(String type, int n) {
    if (type == null)
      type = "XY";
    if (n < -1 || pd.getNumberOfSpectraInCurrentSet() <= n)
      return "only " + pd.getNumberOfSpectraInCurrentSet()
          + " spectra available.";
    try {
      JDXSpectrum spec = (n < 0 ? getSpectrum() : getSpectrumAt(n));
      return Exporter.exportTheSpectrum(Exporter.Type.getType(type), null, spec, 0, spec.getXYCoords().length - 1);
    } catch (IOException ioe) {
      // not possible
    }
    return null;
  }
  
  @Override
  public String toString() {
    return getSpectrumAt(0).toString();
  }

	public void setViewTitle(String title) {
		viewTitle = title;
	}
	
	public String getViewTitle() {
		return (viewTitle == null ? getTitle() : viewTitle);
	}

  public String getInput(String message, String title, String sval) {
    String ret = (String) JOptionPane.showInputDialog(this, message, title,
        JOptionPane.QUESTION_MESSAGE, null, null, sval);
    getFocusNow();
    return ret;
  }

	public void showHeader(Object jsvApplet) {
		JDXSpectrum spectrum = pd.getSpectrum();
		String[][] rowData = spectrum.getHeaderRowDataAsArray();
		String[] columnNames = { "Label", "Description" };
		JTable table = new JTable(rowData, columnNames);
		table.setPreferredScrollableViewportSize(new Dimension(400, 195));
		JScrollPane scrollPane = new JScrollPane(table);
		JOptionPane.showMessageDialog((Container) jsvApplet, scrollPane, "Header Information",
				JOptionPane.PLAIN_MESSAGE);
		getFocusNow();
	}

	public void showDialog(ScriptInterface si, AType type) {
		AwtAnnotationDialog dialog = null; 
		AnnotationData ad = pd.getDialog(type);
		if (ad != null && ad instanceof AwtAnnotationDialog) {
			((AwtAnnotationDialog) ad).reEnable();
			return;
		}
		
		int iSpec = pd.getCurrentSpectrumIndex();
		if (iSpec < 0) {
			showMessage("To enable " + type + " first select a spectrum by clicking on it.", "" + type);
			return;
		}
		JDXSpectrum spec = getSpectrum();
		switch (type) {
		case Integration:
			dialog = new AwtIntegralListDialog("Integration for " + spec, si, spec, this, null);
			break;
		case Measurements:
			dialog = new AwtMeasurementListDialog("Measurements for " + spec, si, spec, this, null);
			break;
		case PeakList:
			dialog = new AwtPeakListDialog("Peak List for " + spec, si, spec, this, null);
			break;
		}
		if (ad != null)
			dialog.setData(ad);
		pd.addDialog(iSpec, type, dialog);
		dialog.reEnable();
	}

	public void showMessage(String msg, String title) {
		JOptionPane.showMessageDialog(this, msg, title, (msg.startsWith("<html>") ? JOptionPane.INFORMATION_MESSAGE 
				: JOptionPane.PLAIN_MESSAGE));	
		getFocusNow();
	}

	public boolean getFocusNow() {
		return requestFocusInWindow();
	}

}
