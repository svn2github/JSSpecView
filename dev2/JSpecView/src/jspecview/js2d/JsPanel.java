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

import java.io.IOException;
import java.io.OutputStream;

import org.jmol.api.Event;
import org.jmol.api.EventManager;
import org.jmol.api.JmolMouseInterface;
import org.jmol.util.JmolFont;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import jspecview.api.AnnotationDialog;
import jspecview.api.JSVApiPlatform;
import jspecview.api.JSVPanel;
import jspecview.common.GraphSet;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.ColorParameters;
import jspecview.common.PrintLayout;
import jspecview.common.ScriptToken;
import jspecview.common.Annotation.AType;
import jspecview.export.Exporter;
import jspecview.util.JSVColor;


/**
 * JSVPanel class represents a View combining one or more GraphSets, each with one or more JDXSpectra.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class JsPanel implements JSVPanel, EventManager {
//, MouseListener,  MouseMotionListener, KeyListener
  private static final long serialVersionUID = 1L;

  @Override
  public void finalize() {
    Logger.info("JSVPanel " + this + " finalized");
  }

  public PanelData pd;
	private JSVColor bgcolor;

	private JSViewer viewer;
  String name;

  public PanelData getPanelData() {
    return pd;
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

  public void setBackgroundColor(JSVColor color) {
  	bgcolor = color;
  }
  
  public JSVColor getBackgroundColor() {
  	return bgcolor;
  }
  
  /**
   * Constructs a new JSVPanel
   * @param viewer 
   * 
   * @param spectrum
   *        the spectrum
   * @return this
   */
  public JsPanel setOne(JSViewer viewer, JDXSpectrum spectrum) {
    // standard applet not overlaid and not showing range
    // standard application split spectra
    // removal of integration, taConvert
    // Preferences Dialog sample.jdx
//  	ToolTipManager.sharedInstance().setInitialDelay(0);
  	//toolTip = new AwtToolTip(this);
  	set(viewer);
    pd.initSingleSpectrum(spectrum);
    return this;
  }

  private void set(JSViewer viewer) {
  	this.viewer = viewer;
    pd = new PanelData(this);
    pd.BLACK = getColor1(0);
	}

  public static JsPanel getAwtPanel(JSViewer viewer, JDXSpectrum spectrum) {
    return new JsPanel().setOne(viewer, spectrum);
  }

  public static JsPanel getAwtPanel(JSViewer viewer, JmolList<JDXSpectrum> specs, int startIndex, int endIndex) {
    return new JsPanel().setMany(viewer, specs, startIndex, endIndex);
  }


  public JsPanel() {
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
  private JsPanel setMany(JSViewer viewer, JmolList<JDXSpectrum> spectra, int startIndex,
      int endIndex) {
    pd = new PanelData(this);
    this.viewer = viewer;
    apiPlatform = viewer.si.getApiPlatform();
  	//toolTip = new AwtToolTip(this);
    pd.initJSVPanel(spectra, startIndex, endIndex);
    return this;
  }

	public void setupPlatform() {
    //setBorder(BorderFactory.createLineBorder(Color.BLACK));
    mouse = apiPlatform.getMouseManager(this);
  }

	private JSVApiPlatform apiPlatform;
	public JSVApiPlatform getApiPlatform() {
		return apiPlatform;
	}
	
	private JmolMouseInterface mouse;

  public GraphSet getNewGraphSet() {
    return new GraphSet(this.pd);
  }

  public JSVColor getColor4(int r, int g, int b, int a) {
    return JsColor.get4(r, g, b, a);
  }
  
  public JSVColor getColor3(int r, int g, int b) {
    return JsColor.get3(r, g, b);
  }
  
  public JSVColor getColor1(int rgb) {
    return JsColor.get1(rgb);
  }
  
  /*----------------------- JSVPanel PAINTING METHODS ---------------------*/

  public void doRepaint() {
  	// to the system
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
 

//  /**
//   * Overrides paintComponent in class JPanel in order to draw the spectrum
//   * 
//   * @param g
//   *        the <code>Graphics</code> object
//   */
//  @Override
//  public void paintComponent(Graphics g) {
//  	
//  	// from the system, via update or applet/app repaint
//  	
//    if (viewer == null || pd == null || pd.graphSets == null || pd.isPrinting)
//      return;
//    
//    super.paintComponent(g); // paint background 
//    
//    pd.drawGraph(g, getWidth(), getHeight(), false);
//    viewer.repaintDone();
//  }

  
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
//		if (asThread)
//			SwingUtilities.invokeLater(new RequestThread());
//		else
//  		requestFocusInWindow();
    pd.dialogsToFront();
	}

//  public class RequestThread implements Runnable {
//		public void run() {
//			requestFocusInWindow();
//		}
//  }

  /*--------------mouse and keyboard interface -----------------------*/

	public boolean keyPressed(int keyCode, int modifiers) {
		return (!pd.isPrinting && pd.keyPressed(keyCode, modifiers));
	}

	public void keyReleased(int keyCode) {
		if (pd.isPrinting)
			return;
		pd.keyReleased(keyCode);
	}

  public boolean keyTyped(int ch, int modifiers) {
		return (!pd.isPrinting && pd.keyTyped(ch, modifiers));
  }

	public void mouseAction(int mode, long time, int x, int y, int count,
			int buttonMods) {
		if (pd.isPrinting)
			return;
		switch (mode) {
		case Event.PRESSED:
		case Event.RELEASED:
		case Event.DRAGGED:
	    break;
		case Event.MOVED:
	    getFocusNow(false);
	    break;
		case Event.CLICKED:
	    if (pd.checkMod(buttonMods, Event.MOUSE_RIGHT)) {
	    	viewer.showMenu(x, y);
	      return;
	    }
	    break;
		}
		pd.mouseAction(mode, time, x, y, count, buttonMods);
	}

	public void mouseEnterExit(long time, int x, int y, boolean isExit) {
		if (isExit) {
			pd.mouseEnterExit(time, x, y, isExit);
		} else {
	    getFocusNow(false);
		}			
	}

  /*-----------------GRAPHICS METHODS----------------------------------- */
	public void drawString(Object g, String text, int x, int y) {
//		((Graphics) g).drawString(text, x, y);
	}


	public void setGraphicsColor(Object g, JSVColor c) {
//		((Graphics) g).setColor((Color) c);
	}

	public void setGraphicsFont(Object g, JmolFont font) {
//		((Graphics) g).setFont((Font) font.font);
	}

	public void translateScale(Object g, double x, double y, double scale) {
		//??
	}

  /*----------------- METHODS IN INTERFACE Printable ---------------------- */

//  /**
//   * uses itext to create the document, either to a file or a byte stream
//   * @param os 
//   * @param pl 
//   */
//  private void createPdfDocument(OutputStream os, PrintLayout pl) {
//    PdfCreatorInterface pdfCreator = (PdfCreatorInterface) JSVInterface.getInterface("jspecview.java.AwtPdfCreator");
//  	if (pdfCreator == null)
//  		return;
//  	pdfCreator.createPdfDocument(this, pl, os);
//  }

	/**
	 * Send a print job of the spectrum to the default printer on the system
	 * 
	 * @param pl
	 *          the layout of the print job
	 * @param os
	 * @param title
	 */
	public void printPanel(PrintLayout pl, OutputStream os, String title) {

		// MediaSize size = MediaSize.getMediaSizeForName(pl.paper);

		pl.title = title;
//		pd.setPrint(pl, os == null ? pl.font : "Helvetica");

		/* Create a print job */
//	try {
//			PrinterJob pj = (os == null ? PrinterJob.getPrinterJob() : null);
//			if (pj != null) {
//				if (title.length() > 30)
//					title = title.substring(0, 30);
//				pj.setJobName(title);
//				pj.setPrintable(this);
//			}
//			if (pj == null || pj.printDialog()) {
//				try {
////					if (pj == null) {
////						createPdfDocument(os, pl);
////					} else {
////						PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
////						aset
////								.add(pl.layout.equals("landscape") ? OrientationRequested.LANDSCAPE
////										: OrientationRequested.PORTRAIT);
////						aset.add((Attribute) pl.paper);
////						pj.print(aset);
////					}
////				} catch (PrinterException ex) {
////					String s = ex.getMessage();
////					if (s == null)
////						return;
////					s = Txt.simpleReplace(s, "not accepting job.", "not accepting jobs.");
////					// not my fault -- Windows grammar error!
////					showMessage(s, "Printer Error");
////				}
////			}
//		} catch (Exception e) {
//			// too bad
//		} finally {
//			pd.setPrint(null, null);
//		}
	}


//  /**
//   * Implements method print in interface printable
//   * 
//   * @param g
//   *        the <code>Graphics</code> object
//   * @param pf
//   *        the <code>PageFormat</code> object
//   * @param pi
//   *        the page index -- -1 for PDF creation
//   * @return an int that depends on whether a print was successful
//   * @throws PrinterException
//   */
//  public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
//    if (pi == 0) {
//      Graphics2D g2D = (Graphics2D) g;
//      pd.isPrinting = true;
//
//      double height, width;
//      boolean addFilePath = false;
//      if (pd.printGraphPosition.equals("default")) {
//        g2D.translate(pf.getImageableX(), pf.getImageableY());
//        if (pf.getOrientation() == PageFormat.PORTRAIT) {
//          height = PanelData.defaultPrintHeight;
//          width = PanelData.defaultPrintWidth;
//        } else {
//          height = PanelData.defaultPrintWidth;
//          width = PanelData.defaultPrintHeight;
//        }
//      } else if (pd.printGraphPosition.equals("fit to page")) {
//        g2D.translate(pf.getImageableX(), pf.getImageableY());
//        addFilePath = true;
//        height = pf.getImageableHeight();
//        width = pf.getImageableWidth();
//      } else { // center
//        Paper paper = pf.getPaper();
//        double paperHeight = paper.getHeight();
//        double paperWidth = paper.getWidth();
//        int x, y;
//
//        if (pf.getOrientation() == PageFormat.PORTRAIT) {
//          height = PanelData.defaultPrintHeight;
//          width = PanelData.defaultPrintWidth;
//          x = (int) (paperWidth - width) / 2;
//          y = (int) (paperHeight - height) / 2;
//        } else {
//          height = PanelData.defaultPrintWidth;
//          width = PanelData.defaultPrintHeight;
//          y = (int) (paperWidth - PanelData.defaultPrintWidth) / 2;
//          x = (int) (paperHeight - PanelData.defaultPrintHeight) / 2;
//        }
//        g2D.translate(x, y);
//      }
//
//      g2D.scale(0.1, 0.1); // high resolution vector graphics for PDF
//      pd.drawGraph(g2D, (int) width, (int) height, addFilePath);
//
//      pd.isPrinting = false;
//      return Printable.PAGE_EXISTS;
//    }
//    pd.isPrinting = false;
//    return Printable.NO_SUCH_PAGE;
//  }

	public void draw2DImage(Object g, Object image2d, int destX, int destY,
			int destWidth, int destHeight, int srcX0, int srcY0, int srcX1, int srcY1) {		
//		((Graphics) g).drawImage((Image) image2d, destX, destY, destWidth, destHeight, srcX0, srcY0, srcX1, srcY1, null);
	}

	public Object newImage(int width, int height, int[] buffer) {
//		BufferedImage image2D = new BufferedImage(width, height,
//				BufferedImage.TYPE_BYTE_GRAY);
//		WritableRaster raster = image2D.getRaster();
//		raster.setSamples(0, 0, width, height, 0,
//				buffer);
//		return image2D;
		return null;
	}

	public void fillRect(Object g, int x, int y, int width, int height) {
//		((Graphics) g).fillRect(x, y, width, height);	
	}

	public void drawLine(Object g, int x0, int y0, int x1, int y1) {
//		((Graphics) g).drawLine(x0, y0, x1, y1);
	}

	public void drawRect(Object g, int x, int y, int xPixels,
			int yPixels) {
//		((Graphics) g).drawRect(x, y, xPixels, yPixels);
	}

	public int getFontHeight(Object g) {
//    return ((Graphics) g).getFontMetrics().getHeight();
		return 0;
	}

	public int getStringWidth(Object g, String s) {
//  	return (s == null ? 0 : ((Graphics) g).getFontMetrics().stringWidth(s));
		return 0;
	}

	public void drawOval(Object g, int x, int y, int width, int height) {
//		((Graphics) g).drawOval(x, y, width, height);
	}

	public void drawPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
//		((Graphics) g).drawPolygon(ayPoints, axPoints, nPoints);
	}

	public void fillOval(Object g, int x, int y, int width, int height) {
//		((Graphics) g).fillOval(x, y, width, height);
	}

	public void fillPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
//		((Graphics) g).fillPolygon(ayPoints, axPoints, nPoints);
	}

	public void rotatePlot(Object g, int angle, int x, int y) {
//  	((Graphics2D) g).rotate(Math.PI * angle / 180.0, x, y);
  }

  
//	BasicStroke strokeBasic = new BasicStroke();
//  BasicStroke strokeBold = new BasicStroke(2f);

	
	public void setStrokeBold(Object g, boolean tf) {
//		((Graphics2D) g).setStroke(tf ? strokeBold : strokeBasic);
	}

	public int getOptionFromDialog(Object frame, String[] items,
			String dialogName, String labelName) {
	final int ret[] = new int[] { Integer.MIN_VALUE };
//		final JDialog dialog = new JDialog((JFrame) frame, dialogName, true);
//		dialog.setResizable(false);
//		dialog.setSize(200, 100);
//		dialog.setLocation(getLocation().x + getSize().width / 2,
//				getLocation().y + getSize().height / 2);
//		// Q: why (x + w)/2, (y + h)/2?? 
//		final JComboBox<Object> cb = new JComboBox<Object>(items);
//		Dimension d = new Dimension(120, 25);
//		cb.setPreferredSize(d);
//		cb.setMaximumSize(d);
//		cb.setMinimumSize(d);
//		JPanel p = new JPanel(new FlowLayout());
//		JButton button = new JButton("OK");
//		p.add(cb);
//		p.add(button);
//		dialog.getContentPane().setLayout(new BorderLayout());
//		dialog.getContentPane().add(
//				new JLabel(labelName, SwingConstants.CENTER),
//				BorderLayout.NORTH);
//		dialog.getContentPane().add(p);
//		button.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				ret[0] = cb.getSelectedIndex();
//				dialog.dispose();
//			}
//		});
//		dialog.setVisible(true);
//		dialog.dispose();
		return ret[0];
	}

	public void saveImage(String type, Object file) {
//    Image image = createImage(getWidth(), getHeight());
//    paint(image.getGraphics());
//    try {
//			ImageIO.write((RenderedImage) image, type, (File) file);
//		} catch (IOException e) {
//			showMessage(e.getMessage(), "Error Saving Image");
//		}
	}

	public String exportTheSpectrum(String type, String path, JDXSpectrum spec,
			int startIndex, int endIndex) throws IOException {
		return Exporter.exportTheSpectrum(type, path, spec, startIndex, endIndex);
	}

	public int getFontFaceID(String name) {
		return JmolFont.getFontFaceID("SansSerif");
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
		return 0;
	}

	public int getWidth() {
		// TODO Auto-generated method stub
		return 0;
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

	public AnnotationDialog getDialog(AType type, JDXSpectrum spec) {
		switch (type) {
//		case Integration:
//			return new JsDialogIntegrals("Integration for " + spec, viewer, spec);
//		case Measurements:
//			return new JsDialogMeasurements("Measurements for " + spec, viewer, spec);
//			break;
//		case PeakList:
//			return new JsDialogPeakList("Peak List for " + spec, viewer, spec);
//			break;
		default:
			return null;
		}
	}

  @Override
  public String toString() {
    return pd.getSpectrumAt(0).toString();
  }


}
