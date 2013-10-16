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

package jspecview.awt;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import org.jmol.util.JmolFont;

import jspecview.api.JSVColor;
import jspecview.api.JSVGraphics;

/**
 * JSVPanel class represents a View combining one or more GraphSets, each with one or more JDXSpectra.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class AwtG2D implements JSVGraphics {
  private static final long serialVersionUID = 1L;
  
	BasicStroke strokeBasic = new BasicStroke();
  BasicStroke strokeBold = new BasicStroke(2f);

  public AwtG2D() {
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

	public void draw2DImage(Object g, Object image2d, int destX, int destY,
			int destWidth, int destHeight, int srcX0, int srcY0, int srcX1, int srcY1) {		
		((Graphics) g).drawImage((Image) image2d, destX, destY, destWidth, destHeight, srcX0, srcY0, srcX1, srcY1, null);
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
		if (path == null) {
			((Graphics) g).drawLine(x0, y0, x1, y1);
		} else {
			path.moveTo(x0, y0);
			path.lineTo(x1, y1);
		}			
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

	public void drawCircle(Object g, int x, int y, int diameter) {
		((Graphics) g).drawOval(x, y, diameter, diameter);
	}

	public void drawPolygon(Object g, int[] axPoints, int[] ayPoints, int nPoints) {
		((Graphics) g).drawPolygon(axPoints, ayPoints, nPoints);
	}

	public void fillCircle(Object g, int x, int y, int diameter) {
		((Graphics) g).fillOval(x, y, diameter, diameter);
	}

	public void fillPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
		((Graphics) g).fillPolygon(ayPoints, axPoints, nPoints);
	}

	public void rotatePlot(Object g, int angle, int x, int y) {
  	((Graphics2D) g).rotate(Math.PI * angle / 180.0, x, y);
  }

	public void translateScale(Object g, double x, double y, double scale) {
		((Graphics2D) g).translate(x, y);
		((Graphics2D) g).scale(scale, scale);
	}
  
	public void setStrokeBold(Object g, boolean tf) {
		((Graphics2D) g).setStroke(tf ? strokeBold : strokeBasic);
	}

	public void fillBackground(Object g, JSVColor bgcolor) {
		// not necessary
	}

	public void setWindowParameters(int width, int height) {
		// not necessary
	}

	public boolean canDoLineTo() {
		return true;
	}

	private GeneralPath path;
	
	public void doStroke(Object g, boolean isBegin) {
		if (isBegin) {
			path = new GeneralPath();
		} else {
			((Graphics2D) g).draw(path);
			path = null;
		}
	}

	public void lineTo(Object g, int x2, int y2) {
		path.lineTo(x2, y2);
	}


}
