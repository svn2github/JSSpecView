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

package jspecview.java;

import jspecview.util.JSVColor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet; //import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import org.jmol.api.Event;
import org.jmol.api.EventManager;
import org.jmol.api.JmolMouseInterface;
import org.jmol.util.JmolFont;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.Txt;

import jspecview.api.AnnotationData;
import jspecview.api.AnnotationDialog;
import jspecview.api.JSVApiPlatform;
import jspecview.api.JSVPanel;
import jspecview.api.PdfCreatorInterface;
import jspecview.common.Annotation;
import jspecview.common.ColoredAnnotation;
import jspecview.common.GraphSet;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVInterface;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.ColorParameters;
import jspecview.common.PrintLayout;
import jspecview.common.ScriptToken;
import jspecview.common.Annotation.AType;
import jspecview.export.Exporter;

/**
 * JSVPanel class represents a View combining one or more GraphSets, each with one or more JDXSpectra.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class AwtPanel extends JPanel implements JSVPanel, Printable, EventManager {
//, MouseListener,  MouseMotionListener, KeyListener
  private static final long serialVersionUID = 1L;

  @Override
  public void finalize() {
    Logger.info("JSVPanel " + this + " finalized");
  }

  public PanelData pd;
	private JSVColor bgcolor;

	private JSViewer viewer;

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
    setName(title);
  }

	public void setColorOrFont(ColorParameters ds, ScriptToken st) {
  	pd.setColorOrFont(ds, st);
  }

  public void setBackgroundColor(JSVColor color) {
  	setBackground((Color) (bgcolor = color));
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
  public AwtPanel setOne(JSViewer viewer, JDXSpectrum spectrum) {
    // standard applet not overlaid and not showing range
    // standard application split spectra
    // removal of integration, taConvert
    // Preferences Dialog sample.jdx
  	ToolTipManager.sharedInstance().setInitialDelay(0);
  	//toolTip = new AwtToolTip(this);
  	set(viewer);
    pd.initSingleSpectrum(spectrum);
    return this;
  }

  private void set(JSViewer viewer) {
  	this.viewer = viewer;
    pd = new PanelData(this);
    pd.BLACK = new AwtColor(0);
	}

  public static AwtPanel getAwtPanel(JSViewer viewer, JDXSpectrum spectrum) {
    return new AwtPanel().setOne(viewer, spectrum);
  }

  public static AwtPanel getAwtPanel(JSViewer viewer, JmolList<JDXSpectrum> specs, int startIndex, int endIndex) {
    return new AwtPanel().setMany(viewer, specs, startIndex, endIndex);
  }


  public AwtPanel() {
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
  private AwtPanel setMany(JSViewer viewer, JmolList<JDXSpectrum> spectra, int startIndex,
      int endIndex) {
    pd = new PanelData(this);
    this.viewer = viewer;
    this.apiPlatform = viewer.si.getApiPlatform();
  	//toolTip = new AwtToolTip(this);
    pd.initJSVPanel(spectra, startIndex, endIndex);
    return this;
  }

  public GraphSet getNewGraphSet() {
    return new GraphSet(this.pd);
  }

  public JSVColor getColor4(int r, int g, int b, int a) {
    return new AwtColor(r, g, b, a);
  }
  
  public JSVColor getColor3(int r, int g, int b) {
    return new AwtColor(r, g, b);
  }
  
  public JSVColor getColor1(int rgb) {
    return new AwtColor(rgb);
  }
  
  /*----------------------- JSVPanel PAINTING METHODS ---------------------*/

  public void doRepaint() {
  	// to the system
  	if (!pd.isPrinting)
      viewer.requestRepaint();
  }
  
  @Override
	public void update(Graphics g) {
  	// from the system
  	// System: Do not clear rectangle -- we are opaque and will take care of that.
  	// seems unnecessary, but apparently for the Mac it is critical. Still not totally convinced!
      paint(g);
  }
 

  /**
   * Overrides paintComponent in class JPanel in order to draw the spectrum
   * 
   * @param g
   *        the <code>Graphics</code> object
   */
  @Override
  public void paintComponent(Graphics g) {
  	
  	// from the system, via update or applet/app repaint
  	
    if (viewer == null || pd == null || pd.graphSets == null || pd.isPrinting)
      return;
    
    super.paintComponent(g); // paint background 
    
    pd.drawGraph(g, getWidth(), getHeight(), false);
    viewer.repaintDone();
  }

  
	private JSVApiPlatform apiPlatform;
	public JSVApiPlatform getApiPlatform() {
		return apiPlatform;
	}
	
	private JmolMouseInterface mouse;

	public void setupPlatform() {
  	this.apiPlatform = viewer.si.getApiPlatform();
    setBorder(BorderFactory.createLineBorder(Color.BLACK));
    mouse = apiPlatform.getMouseManager(this);
  }

  public String export(String type, int n) {
    if (type == null)
      type = "XY";
    if (n < -1 || pd.getNumberOfSpectraInCurrentSet() <= n)
      return "only " + pd.getNumberOfSpectraInCurrentSet()
          + " spectra available.";
    try {
      JDXSpectrum spec = (n < 0 ? pd.getSpectrum() : pd.getSpectrumAt(n));
      return Exporter.exportTheSpectrum(type, null, spec, 0, spec.getXYCoords().length - 1);
    } catch (IOException ioe) {
      // not possible
    }
    return null;
  }
  
  @Override
  public String toString() {
    return pd.getSpectrumAt(0).toString();
  }

  public String getInput(String message, String title, String sval) {
    String ret = (String) JOptionPane.showInputDialog(this, message, title,
        JOptionPane.QUESTION_MESSAGE, null, null, sval);
    getFocusNow(true);
    return ret;
  }

	public void showMessage(String msg, String title) {
		Logger.info(msg);
		JOptionPane.showMessageDialog(this, msg, title, (msg.startsWith("<html>") ? JOptionPane.INFORMATION_MESSAGE 
				: JOptionPane.PLAIN_MESSAGE));	
		getFocusNow(true);
	}

	public AnnotationDialog showDialog(AType type) {
		AwtDialogAnnotation dialog = null; 
		AnnotationData ad = pd.getDialog(type);
		pd.closeAllDialogsExcept(type);
		if (ad != null && ad instanceof AwtDialogAnnotation) {
			((AwtDialogAnnotation) ad).params.reEnable();
			return (AnnotationDialog) ad;
		}
		
		int iSpec = pd.getCurrentSpectrumIndex();
		if (iSpec < 0) {
			showMessage("To enable " + type + " first select a spectrum by clicking on it.", "" + type);
			return null;
		}
		JDXSpectrum spec = pd.getSpectrum();
		switch (type) {
		case Integration:
			dialog = new AwtDialogIntegrals("Integration for " + spec, viewer, spec);
			break;
		case Measurements:
			dialog = new AwtDialogMeasurements("Measurements for " + spec, viewer, spec);
			break;
		case PeakList:
			dialog = new AwtDialogPeakList("Peak List for " + spec, viewer, spec);
			break;
		case NONE:
		}
		if (ad != null)
			dialog.params.setData(ad);
		pd.addDialog(iSpec, type, dialog);
		dialog.params.reEnable();
		return dialog;
	}

	public void getFocusNow(boolean asThread) {
		if (asThread)
			SwingUtilities.invokeLater(new RequestThread());
		else
  		requestFocusInWindow();
    pd.dialogsToFront();
	}

  public class RequestThread implements Runnable {
		public void run() {
			requestFocusInWindow();
		}
  }

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
		((Graphics) g).drawString(text, x, y);
	}


	public void setGraphicsColor(Object g, JSVColor c) {
		((Graphics) g).setColor((Color) c);
	}

	public void setGraphicsFont(Object g, JmolFont font) {
		((Graphics) g).setFont((Font) font.font);
	}

  public void setPlotColors(JSVColor[] colors) {
  	pd.setPlotColors(colors);
  }

  /*----------------- METHODS IN INTERFACE Printable ---------------------- */

  /**
   * uses itext to create the document, either to a file or a byte stream
   * @param os 
   * @param pl 
   */
  private void createPdfDocument(OutputStream os, PrintLayout pl) {
  	PdfCreatorInterface pdfCreator = (PdfCreatorInterface) JSVInterface.getInterface("jspecview.java.AwtPdfCreator");
  	if (pdfCreator == null)
  		return;
  	pdfCreator.createPdfDocument(this, pl, os);
  }

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
		pd.setPrint(pl, os == null ? pl.font : "Helvetica");

		/* Create a print job */

		try {
			PrinterJob pj = (os == null ? PrinterJob.getPrinterJob() : null);
			if (pj != null) {
				if (title.length() > 30)
					title = title.substring(0, 30);
				pj.setJobName(title);
				pj.setPrintable(this);
			}
			if (pj == null || pj.printDialog()) {
				try {
					if (pj == null) {
						createPdfDocument(os, pl);
					} else {
						PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
						aset
								.add(pl.layout.equals("landscape") ? OrientationRequested.LANDSCAPE
										: OrientationRequested.PORTRAIT);
						aset.add((Attribute) pl.paper);
						pj.print(aset);
					}
				} catch (PrinterException ex) {
					String s = ex.getMessage();
					if (s == null)
						return;
					s = Txt.simpleReplace(s, "not accepting job.", "not accepting jobs.");
					// not my fault -- Windows grammar error!
					showMessage(s, "Printer Error");
				}
			}
		} catch (Exception e) {
			// too bad
		} finally {
			pd.setPrint(null, null);
		}
	}


  /**
   * Implements method print in interface printable
   * 
   * @param g
   *        the <code>Graphics</code> object
   * @param pf
   *        the <code>PageFormat</code> object
   * @param pi
   *        the page index -- -1 for PDF creation
   * @return an int that depends on whether a print was successful
   * @throws PrinterException
   */
  public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
    if (pi == 0) {
      Graphics2D g2D = (Graphics2D) g;
      pd.isPrinting = true;

      double height, width;
      boolean addFilePath = false;
      if (pd.printGraphPosition.equals("default")) {
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        if (pf.getOrientation() == PageFormat.PORTRAIT) {
          height = PanelData.defaultPrintHeight;
          width = PanelData.defaultPrintWidth;
        } else {
          height = PanelData.defaultPrintWidth;
          width = PanelData.defaultPrintHeight;
        }
      } else if (pd.printGraphPosition.equals("fit to page")) {
        g2D.translate(pf.getImageableX(), pf.getImageableY());
        addFilePath = true;
        height = pf.getImageableHeight();
        width = pf.getImageableWidth();
      } else { // center
        Paper paper = pf.getPaper();
        double paperHeight = paper.getHeight();
        double paperWidth = paper.getWidth();
        int x, y;

        if (pf.getOrientation() == PageFormat.PORTRAIT) {
          height = PanelData.defaultPrintHeight;
          width = PanelData.defaultPrintWidth;
          x = (int) (paperWidth - width) / 2;
          y = (int) (paperHeight - height) / 2;
        } else {
          height = PanelData.defaultPrintWidth;
          width = PanelData.defaultPrintHeight;
          y = (int) (paperWidth - PanelData.defaultPrintWidth) / 2;
          x = (int) (paperHeight - PanelData.defaultPrintHeight) / 2;
        }
        g2D.translate(x, y);
      }

      g2D.scale(0.1, 0.1); // high resolution vector graphics for PDF
      pd.drawGraph(g2D, (int) width, (int) height, addFilePath);

      pd.isPrinting = false;
      return Printable.PAGE_EXISTS;
    }
    pd.isPrinting = false;
    return Printable.NO_SUCH_PAGE;
  }

	public void draw2DImage(Object g, Object image2d, int destX, int destY,
			int destWidth, int destHeight, int srcX0, int srcY0, int srcX1, int srcY1) {		
		((Graphics) g).drawImage((Image) image2d, destX, destY, destWidth, destHeight, srcX0, srcY0, srcX1, srcY1, null);
	}

	public Annotation getColoredAnnotation(JDXSpectrum spectrum, double x,
			double y, String text, JSVColor c, boolean isPixels, boolean is2d,
			int offsetX, int offsetY) {
		return new ColoredAnnotation(x, y).set(spectrum, text, c, isPixels,
				is2d, offsetX, offsetY);
	}

	public Annotation getNextAnnotation(JDXSpectrum spectrum,
			JmolList<String> args, Annotation lastAnnotation) {
		return Annotation.getColoredAnnotation(this, spectrum, args, lastAnnotation);
	}
	
	public Annotation newAnnotation(double x, double y, JDXSpectrum spec,
			String text, JSVColor color, boolean isPixels, boolean is2D, int offsetX,
			int offsetY) {
		return new ColoredAnnotation(x, y).set(spec, text, color, isPixels,
				is2D, offsetX, offsetY);
	}

	public Object newImage(int width, int height, int[] buffer) {
		BufferedImage image2D = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = image2D.getRaster();
		raster.setSamples(0, 0, width, height, 0,
				buffer);
		return image2D;
	}

	public void fillRect(Object g, int x, int y, int width, int height) {
		((Graphics) g).fillRect(x, y, width, height);	
	}

	public void drawLine(Object g, int x0, int y0, int x1, int y1) {
		((Graphics) g).drawLine(x0, y0, x1, y1);
	}

	public void drawRect(Object g, int x, int y, int xPixels,
			int yPixels) {
		((Graphics) g).drawRect(x, y, xPixels, yPixels);
	}

	public int getFontHeight(Object g) {
    return ((Graphics) g).getFontMetrics().getHeight();
	}

	public int getStringWidth(Object g, String s) {
  	return (s == null ? 0 : ((Graphics) g).getFontMetrics().stringWidth(s));
	}

	public void drawOval(Object g, int x, int y, int width, int height) {
		((Graphics) g).drawOval(x, y, width, height);
	}

	public void drawPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
		((Graphics) g).drawPolygon(ayPoints, axPoints, nPoints);
	}

	public void fillOval(Object g, int x, int y, int width, int height) {
		((Graphics) g).fillOval(x, y, width, height);
	}

	public void fillPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
		((Graphics) g).fillPolygon(ayPoints, axPoints, nPoints);
	}

	public void rotatePlot(Object g, int angle, int x, int y) {
  	((Graphics2D) g).rotate(Math.PI * angle / 180.0, x, y);
  }

  
	BasicStroke strokeBasic = new BasicStroke();
  BasicStroke strokeBold = new BasicStroke(2f);

	
	public void setStrokeBold(Object g, boolean tf) {
		((Graphics2D) g).setStroke(tf ? strokeBold : strokeBasic);
	}

	public int getFontFaceID(String name) {
		return JmolFont.getFontFaceID("SansSerif");
	}
	
	public int getOptionFromDialog(Object frame, String[] items,
			String dialogName, String labelName) {
		final JDialog dialog = new JDialog((JFrame) frame, dialogName, true);
		dialog.setResizable(false);
		dialog.setSize(200, 100);
		dialog.setLocation(getLocation().x + getSize().width / 2,
				getLocation().y + getSize().height / 2);
		// Q: why (x + w)/2, (y + h)/2?? 
		final JComboBox<Object> cb = new JComboBox<Object>(items);
		Dimension d = new Dimension(120, 25);
		cb.setPreferredSize(d);
		cb.setMaximumSize(d);
		cb.setMinimumSize(d);
		JPanel p = new JPanel(new FlowLayout());
		JButton button = new JButton("OK");
		p.add(cb);
		p.add(button);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(
				new JLabel(labelName, SwingConstants.CENTER),
				BorderLayout.NORTH);
		dialog.getContentPane().add(p);
		final int ret[] = new int[] { Integer.MIN_VALUE };
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ret[0] = cb.getSelectedIndex();
				dialog.dispose();
			}
		});
		dialog.setVisible(true);
		dialog.dispose();
		return ret[0];
	}

	public Object[][] getOverlayLegendData() {
	  return pd.getOverlayLegendData();
	}

	public void saveImage(String type, Object file) {
    Image image = createImage(getWidth(), getHeight());
    paint(image.getGraphics());
    try {
			ImageIO.write((RenderedImage) image, type, (File) file);
		} catch (IOException e) {
			showMessage(e.getMessage(), "Error Saving Image");
		}
	}

	public Object getDisplay() {
		return this;
	}

}
