/* Copyright (c) 2002-2008 The University of the West Indies
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

package jspecview.java;

import jspecview.util.JSVColor;
import jspecview.util.JSVColorUtil;

import jspecview.common.Annotation;
import jspecview.common.ColoredAnnotation;
import jspecview.common.JDXSpectrum;


import org.jmol.util.JmolList;

/**
 * ColoredAnnotation is a label on the spectrum; not an integralRegion
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class AwtColoredAnnotation extends Annotation implements ColoredAnnotation {

  private JSVColor color;

  public JSVColor getColor() {
    return color;
  }

  public AwtColoredAnnotation(double x, double y) {
  	super(x, y);
  }
  
  public AwtColoredAnnotation set(
	JDXSpectrum spec, String text, JSVColor color,
      boolean isPixels, boolean is2D, int offsetX, int offsetY) {
    setAll(spec, text, isPixels, is2D, offsetX, offsetY);
    this.color = color;
    return this;
  }

	public static AwtColoredAnnotation getAnnotation(JDXSpectrum spec, JmolList<String> args,
                                                Annotation lastAnnotation) {
  	String arg;
    int xPt = 0;
    int yPt = 1;
    int colorPt = 2;
    int textPt = 3;
    int nArgs = args.size();
    try {
      switch (nArgs) {
      default:
        return null;
      case 1:
        arg = args.get(0);
        xPt = yPt = -1;
        if (arg.charAt(0) == '\"') {
          textPt = 0;
          colorPt = -1;
        } else {
          colorPt = 0;
          textPt = -1;
        }
        break;
      case 2:
        xPt = yPt = -1;
        arg = args.get(0);
        if (arg.charAt(0) == '\"') {
          textPt = 0;
          colorPt = 1;
        } else {
          colorPt = 0;
          textPt = 1;
        }
        break;
      case 3:
      case 4:
        // x y "text" or x y color
        // x y color "text" or x y "text" color
        arg = args.get(2);
        if (arg.charAt(0) == '\"') {
          textPt = 2;
          colorPt = (nArgs == 4 ? 3 : -1);
        } else {
          colorPt = 2;
          textPt = (nArgs == 4 ? 3 : -1);
        }
        arg = args.get(2);
        if (arg.charAt(0) == '\"') {
          textPt = 2;
          colorPt = -1;
        } else {
          colorPt = 2;
          textPt = -1;
        }
      }
      if (lastAnnotation == null && 
          (xPt < 0 || yPt < 0 || textPt < 0 || colorPt < 0))
        return null;
      double x = (xPt < 0 ? lastAnnotation.getXVal() : Double.valueOf(args.get(xPt)).doubleValue());
      double y = (yPt < 0 ? lastAnnotation.getYVal() : Double.valueOf(args.get(yPt)).doubleValue());
      JSVColor color =(colorPt < 0 ? ((ColoredAnnotation) lastAnnotation).getColor() : 
      	new AwtColor(JSVColorUtil.getArgbFromString(args.get(colorPt))));
      String text;
      if (textPt < 0) {
        text = lastAnnotation.text;
      } else {
        text = args.get(textPt);
        if (text.charAt(0) == '\"')
          text = text.substring(1, text.length() - 1);
      }
      return new AwtColoredAnnotation(x, y).set(spec, text, color, false, false, 0, 0);
    } catch (Exception e) {
      return null;
    }
  }

}
