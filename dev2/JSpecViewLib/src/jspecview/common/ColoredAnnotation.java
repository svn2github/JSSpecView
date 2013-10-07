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

package jspecview.common;

import jspecview.util.JSVColor;

import jspecview.common.Annotation;
import jspecview.common.JDXSpectrum;


/**
 * ColoredAnnotation is a label on the spectrum; not an integralRegion
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class ColoredAnnotation extends Annotation {

  private JSVColor color;

  public JSVColor getColor() {
    return color;
  }

  public ColoredAnnotation(double x, double y) {
  	super(x, y);
  }
  
  public ColoredAnnotation set(
	JDXSpectrum spec, String text, JSVColor color,
      boolean isPixels, boolean is2D, int offsetX, int offsetY) {
    setAll(spec, text, isPixels, is2D, offsetX, offsetY);
    this.color = color;
    return this;
  }

}
