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

import javajs.api.GenericColor;
import javajs.awt.Color;
import javajs.awt.ColorUtil;

import org.jmol.util.JmolFont;

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

public class JsG2D implements JSVGraphics {
  private static final long serialVersionUID = 1L;
  
	private int windowWidth;

	private int windowHeight;

	private JmolFont currentFont;

  public JsG2D() {
	}

  public GenericColor getColor4(int r, int g, int b, int a) {
    return Color.get4(r, g, b, a);
  }
  
  public GenericColor getColor3(int r, int g, int b) {
    return Color.get3(r, g, b);
  }
  
  public GenericColor getColor1(int rgb) {
    return Color.get1(rgb);
  }

	public void draw2DImage(Object g, Object image2d, int destX, int destY,
			int destWidth, int destHeight, int srcX0, int srcY0, int srcX1, int srcY1) {
		// TODO Auto-generated method stub
		
	}

	public void drawLine(Object g, int x0, int y0, int x1, int y1) {
		/**
		 * @j2sNative
		 * 
		 *            if (!this.inPath) g.beginPath(); 
		 *            g.moveTo(x0, y0); 
		 *            g.lineTo(x1, y1); 
		 *            if (!this.inPath) g.stroke();
		 * 
		 */
		{}
	}

	public void drawCircle(Object g, int x, int y, int diameter) {
		/**
		 * @j2sNative
		 * 
		 *    var r = diameter/2;
		 * 		g.beginPath();
		 *    g.arc(x + r, y + r, r, 0, 2 * Math.PI, false);
		 *    g.stroke();
		 */
		{	
		}
		
	}

	public void drawPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
		doPoly(g, ayPoints, axPoints, nPoints, false);
	}

	/**
	 * @param g 
	 * @param axPoints  
	 * @param ayPoints 
	 * @param nPoints 
	 * @param doFill 
	 */
	private void doPoly(Object g, int[] axPoints, int[] ayPoints, int nPoints,
			boolean doFill) {
		/**
		 * @j2sNative
		 * 
		 * g.beginPath();
		 * g.moveTo(axPoints[0], ayPoints[0]);
		 * 
		 * for (var i = 1; i < nPoints; i++)
		 *   g.lineTo(axPoints[i], ayPoints[i]);
     * if (doFill)
     *   g.fill();
     * else
     *   g.stroke();
		 * 
		 */
		{
		}
	}

	public void drawRect(Object g, int x, int y, int width,
			int height) {
		/**
		 * @j2sNative
		 * 
		 * g.beginPath();
     * g.rect(x ,y, width, height);
     * g.stroke();
		 * 
		 */
		{
		}
	}

	public void drawString(Object g, String s, int x, int y) {
		/**
		 * @j2sNative
		 * 
		 * g.fillText(s,x,y);
		 */
		{
			
		}
	}

	public void fillBackground(Object g, GenericColor bgcolor) {
		if (bgcolor == null) {
			/**
			 * @j2sNative
			 * 
			 * g.clearRect(0,0, this.windowWidth, this.windowHeight);
			 * return;
			 * 
			 */
			{				
			}
		}
		setGraphicsColor(g, bgcolor);
		fillRect(g, 0, 0, windowWidth, windowHeight);
	}

	public void fillCircle(Object g, int x, int y, int diameter) {
		/**
		 * @j2sNative
		 * 
		 * 		g.beginPath();
		 *    g.arc(x, y, diameter/2, 0, 2 * Math.PI, false);
		 *    g.fill();
		 */
		{	
		}
		
	}

	public void fillPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
		doPoly(g, ayPoints, axPoints, nPoints, true);
	}

	public void fillRect(Object g, int x, int y, int width, int height) {
		/**
		 * @j2sNative
		 * 
		 * g.fillRect(x, y, width, height);
		 * 
		 */
		{
		}
	}

	public int getFontHeight(Object g) {
		return currentFont.getAscent();
	}

	public int getStringWidth(Object g, String s) {
		return currentFont.stringWidth(s);
	}

	public Object newImage(int width, int height, int[] buffer) {
		// TODO Auto-generated method stub
		return null;
	}

	public void rotatePlot(Object g, int angle, int x, int y) {
		// TODO Auto-generated method stub
		
	}

	public void setGraphicsColor(Object g, GenericColor c) {
		String s = ColorUtil.toCSSString(c);
		/**
		 * @j2sNative
		 * 
		 * g.fillStyle = g.strokeStyle = s;
		 */
		{
			System.out.println(s);
		}
	}

	public void setGraphicsFont(Object g, JmolFont font) {
		currentFont = font;
		String s = font.getInfo();
		int pt = s.indexOf(" ");
		s = s.substring(0, pt) + "px" + s.substring(pt);
		/**
		 * @j2sNative
		 * 
		 * g.font = s;
		 */
		{
		}
	}

	public void setStrokeBold(Object g, boolean tf) {
		/**
		 * @j2sNative
		 *
		 * g.lineWidth = (tf ? 2 : 1);
		 * 
		 */
		{

		}

	}

	public void setWindowParameters(int width, int height) {
		windowWidth = width;
		windowHeight = height;
	}

	public void translateScale(Object g, double x, double y, double scale) {
		// TODO Auto-generated method stub
		
	}

	public boolean canDoLineTo() {
		return true;
	}

	boolean inPath;
	
	public void doStroke(Object g, boolean isBegin) {
		/**
		 * 
		 *  reduce antialiasing, thank you, http://www.rgraph.net/docs/howto-get-crisp-lines-with-no-antialias.html
		 *  
		 * @j2sNative
		 * 
		 * this.inPath = isBegin;
		 * if (isBegin) {
		 * g.translate(0.5, 0.5);
		 * 	g.beginPath();
		 * }
		 * else
		 *  g.stroke();
		 * 
		 */
		{}
	}

	public void lineTo(Object g, int x2, int y2) {
		/**
		 * @j2sNative
		 * 
		 * g.lineTo(x2, y2);
		 * 
		 */
		{}
	}

}
