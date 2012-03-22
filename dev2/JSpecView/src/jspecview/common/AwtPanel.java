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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet; //import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.util.Logger;

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

public class AwtPanel extends JSVPanel implements Printable, MouseListener,
    MouseMotionListener, KeyListener {

  private static final long serialVersionUID = 1L;

  @Override
  public void finalize() {
    Logger.info("JSVPanel " + this + " finalized");
  }

  public void dispose() {
    super.dispose();
    removeKeyListener(this);
    removeMouseListener(this);
    removeMouseMotionListener(this);
  }

  public JSVPanelPopupMenu getPopup() {
    return popup;
  }

  @Override
  public void setTitle(String title) {
    super.setTitle(title);
    setName(title);
  }

  @Override
  protected void doRepaint() {
    repaint();    
  }

  @Override
  protected void doRequestFocusInWindow() {
    requestFocusInWindow();
  }

  ////////// settable colors //////////

  private Color coordinatesColor;
  private Color gridColor;
  private Color integralPlotColor;
  private Color plotAreaColor;
  private Color scaleColor;
  private Color titleColor;
  private Color unitsColor;

  public void setPlotColors(Color[] colors) {
    for (int i = graphSets.size(); --i >= 0;)
      graphSets.get(i).setPlotColors(colors);
  }

  // potentially settable; 

  private Color highlightColor = new Color(255, 0, 0, 200);

  public Color setHighlightColor(Color color) {
    return (highlightColor = color);
  }

  public Color getHighlightColor() {
    return highlightColor;
  }

  private Color zoomBoxColor = new Color(100, 100, 50, 130);

  public Color getZoomBoxColor() {
    return zoomBoxColor;
  }

  public void setColorOrFont(DisplayScheme ds, ScriptToken st) {
    if (st == null) {
      Map<ScriptToken, Color> colors = ds.getColors();
      for (Map.Entry<ScriptToken, Color> entry : colors.entrySet())
        setColorOrFont(ds, entry.getKey());
      setColorOrFont(ds, ScriptToken.DISPLAYFONTNAME);
      setColorOrFont(ds, ScriptToken.TITLEFONTNAME);
      return;
    }
    switch (st) {
    case DISPLAYFONTNAME:
      displayFontName = ds.getDisplayFont();
      if (displayFontName != null)
        options.put(st.name(), displayFontName);
      return;
    case TITLEFONTNAME:
      titleFontName = ds.getTitleFont();
      if (titleFontName != null)
        options.put(st.name(), titleFontName);
      return;
    }
    Color color = ds.getColor(st);
    if (color != null)
      options.put(st.name(), AppUtils.colorToHexString(color));
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
    case INTEGRALPLOTCOLOR:
      integralPlotColor = color;
      break;
    case PLOTCOLOR:
      for (int i = graphSets.size(); --i >= 0;)
        graphSets.get(i).setPlotColor0(color);
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
    default:
      Logger.warn("JSVPanel --- unrecognized DisplayScheme color: " + st);
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
  public AwtPanel(Graph spectrum, JSVPanelPopupMenu popup) {
    // standard applet not overlaid and not showing range
    // standard application split spectra
    // removal of integration, taConvert
    // Preferences Dialog sample.jdx

    this.popup = popup;
    initSingleSpectrum(spectrum);
  }

  public static AwtPanel getJSVPanel(List<JDXSpectrum> specs, int startIndex, int endIndex, JSVPanelPopupMenu popup) {
    List<Graph> graphs = new ArrayList<Graph>(specs.size());
    for (int i = 0; i < specs.size(); i++)
      graphs.add(specs.get(i));
    AwtPanel jsvp = new AwtPanel(graphs, startIndex, endIndex, popup);
    jsvp.isOverlaid = (specs.size() > 1);
    return jsvp;
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
  private AwtPanel(List<Graph> spectra, int startIndex,
      int endIndex, JSVPanelPopupMenu popup) {
    this.popup = popup;
    initJSVPanel(spectra, startIndex, endIndex);
  }

  public static AwtPanel getIntegralPanel(JDXSpectrum spectrum, Color color,
                                          JSVPanelPopupMenu popup) {
    Graph graph = spectrum.getIntegrationGraph();
    List<Graph> graphs = new ArrayList<Graph>();
    graphs.add(spectrum);
    graphs.add(graph);
    AwtPanel jsvp = new AwtPanel(graphs, 0, 0, popup);
    jsvp.setTitle(graph.getTitle());
    jsvp.setPlotColors(new Color[] { jsvp.getPlotColor(0), color });
    return jsvp;
  }

  public GraphSet newGraphSet() {
    return new AwtGraphSet(this);
  }

  /**
   * Returns the color of the plot at a certain index
   * 
   * @param index
   *        the index
   * @return the color of the plot
   */
  public Color getPlotColor(int index) {
    return ((AwtGraphSet) currentGraphSet).getPlotColor(index);
  }

  public Color getColor(ScriptToken whatColor) {
    switch (whatColor) {
    default:
      System.out.println("awtgraphset missing color " + whatColor);
      return Color.BLACK;
    case ZOOMBOXCOLOR:
      return zoomBoxColor;
    case HIGHLIGHTCOLOR:
      return highlightColor;
    case INTEGRALPLOTCOLOR:
      return integralPlotColor;
    case GRIDCOLOR:
      return gridColor;
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
    if (!isEnabled() || graphSets == null)
      return;
    super.paintComponent(g);
    drawGraph(g, getHeight(), getWidth());
  }

  @Override
  protected void setFont(Object g, String name, int mode, int size) {
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
  void drawTitle(Object og, int height, int width, String title) {
    Graphics g = (Graphics) og;
    setFont(g, width, isPrinting || titleBoldOn ? Font.BOLD : Font.PLAIN, 14,
        true);
    FontMetrics fm = g.getFontMetrics();
    g.setColor(titleColor);
    g.drawString(title, 5, (int) (height - fm
        .getHeight() / 2));
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
  protected void drawCoordinates(Object og, int height, int width) {
    Graphics g = (Graphics) og;
    g.setColor(coordinatesColor);
    setFont(g, width, Font.PLAIN, 12, true);
    g.drawString(coordStr, (int) ((plotAreaWidth + leftPlotAreaPos) * 0.85),
        (int) (topPlotAreaPos - 10));
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
      isPrinting = true;

      double height, width;

      if (printGraphPosition.equals("default")) {
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        if (pf.getOrientation() == PageFormat.PORTRAIT) {
          height = defaultHeight;
          width = defaultWidth;
        } else {
          height = defaultWidth;
          width = defaultHeight;
        }
      } else if (printGraphPosition.equals("fit to page")) {
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        height = pf.getImageableHeight();
        width = pf.getImageableWidth();
      } else { // center
        Paper paper = pf.getPaper();
        double paperHeight = paper.getHeight();
        double paperWidth = paper.getWidth();
        int x, y;

        if (pf.getOrientation() == PageFormat.PORTRAIT) {
          height = defaultHeight;
          width = defaultWidth;
          x = (int) (paperWidth - width) / 2;
          y = (int) (paperHeight - height) / 2;
        } else {
          height = defaultWidth;
          width = defaultHeight;
          y = (int) (paperWidth - defaultWidth) / 2;
          x = (int) (paperHeight - defaultHeight) / 2;
        }
        g2D.translate(x, y);
      }

      drawGraph(g2D, (int) height, (int) width);

      isPrinting = false;
      return Printable.PAGE_EXISTS;
    }
    isPrinting = false;
    return Printable.NO_SUCH_PAGE;
  }

  /*--------------------------------------------------------------------------*/

  /**
   * Send a print job of the spectrum to the default printer on the system
   * 
   * @param pl
   *        the layout of the print job
   */
  public void printSpectrum(PrintLayoutDialog.PrintLayout pl) {

    PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();

    if (pl.layout.equals("landscape"))
      aset.add(OrientationRequested.LANDSCAPE);
    else
      aset.add(OrientationRequested.PORTRAIT);

    aset.add(pl.paper);

    //MediaSize size = MediaSize.getMediaSizeForName(pl.paper);

    // Set Graph Properties
    printingFont = pl.font;
    printGrid = pl.showGrid;
    printTitle = pl.showTitle;
    printGraphPosition = pl.position;

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

  }


  /*--------------the rest are all mouse and keyboard interface -----------------------*/

  public void mousePressed(MouseEvent e) {
    if (e.getButton() != MouseEvent.BUTTON1)
      return;
    doMousePressed(e.getX(), e.getY(), e.isControlDown());
  }

  public void mouseMoved(MouseEvent e) {
    doMouseMoved(e.getX(), e.getY());
  }

  public void mouseDragged(MouseEvent e) {
    doMouseDragged(e.getX(), e.getY());
  }
  
  public void mouseReleased(MouseEvent e) {
    doMouseReleased(e.getButton() == MouseEvent.BUTTON1);
  }

  public void mouseClicked(MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON3) {
      popup.show(this, e.getX(), e.getY());
      return;
    }
    requestFocusInWindow();
    doMouseClicked(e.getX(), e.getY(), e.getClickCount(), e.isControlDown());
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
      currentGraphSet.escape();
      isIntegralDrag = false;
      repaint();
      e.consume();
      return;
    }
    if (e.getModifiers() != 0) {
      if (e.isControlDown()) {
        switch (e.getKeyCode()) {
        case 45: //'-'
          currentGraphSet.scaleYBy(0.5);
          e.consume();
          break;
        case 61: //'='
          currentGraphSet.scaleYBy(2);
          e.consume();
          break;
        }
      }
      return;
    }
    switch (e.getKeyCode()) {
    case KeyEvent.VK_LEFT:
      currentGraphSet.toPeak(-1);
      e.consume();
      break;
    case KeyEvent.VK_RIGHT:
      currentGraphSet.toPeak(1);
      e.consume();
      break;
    case KeyEvent.VK_DOWN:
    case KeyEvent.VK_UP:
      int dir = (e.getKeyCode() == KeyEvent.VK_DOWN ? -1 : 1);
      if (getSpectrumAt(0).getSubSpectra() == null) {
        notifySubSpectrumChange(dir, null);
      } else {
        currentGraphSet.advanceSubSpectrum(dir);
        repaint();
      }
      e.consume();
      break;
    }
  }

  public void keyReleased(KeyEvent e) {
  }

  public void keyTyped(KeyEvent e) {
    if (e.getKeyChar() == 'z') {
      currentGraphSet.previousView();
      return;
    }
    if (e.getKeyChar() == 'y') {
      currentGraphSet.nextView();
      return;
    }
  }

  @Override
  protected void setupPlatform() {
    setBorder(BorderFactory.createLineBorder(Color.lightGray));
    if (popup == null) {
      // preferences dialog
      coordStr = "(0,0)";
    } else {
      addKeyListener(this);
      addMouseListener(this);
      addMouseMotionListener(this);
    }
  }

}
