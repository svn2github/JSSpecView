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

// CHANGES to 'IntegrationRatio.java' - Integration Ratio Representation
// University of the West Indies, Mona Campus
// 24-09-2011 jak - Created class as an extension of the Coordinate class
//					to handle the integration ratio value.

package jspecview.common;

/**
 * The <code>Annotation</code> class stores the spectral x and pixel y values of an
 * annotation text along with its text
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class Annotation extends Coordinate {
  public String text = "";
  protected boolean isPixels;
  boolean is2D;
  int offsetX;
  int offsetY;
  JDXSpectrum spec;
  
  public enum AType { Integration, PeakList, Measurements, NONE }
  
  /**
   * Constructor -- note that x is spectral X value, but y is pixels above
   * baseline
   * @param x 
   * @param y 
   * 
   */
  protected Annotation(double x, double y) {
    super(x, y);
  }
  
  protected Annotation setAll(JDXSpectrum spec, String text, boolean isPixels, boolean is2D, int offsetX, int offsetY) {
    this.spec = spec;
    this.text = text;
    this.isPixels = isPixels;
    this.is2D = is2D;
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    return this;
  }

	Annotation setSpec(JDXSpectrum spec) {
		this.spec = spec;
		return this;
	}

	void addSpecShift(double dx) {
		setXVal(getXVal() + dx);
	}
	
  boolean isPixels() {
    return isPixels;
  }

  /**
   * Overrides Objects toString() method
   * 
   * @return the String representation of this coordinate
   */
  @Override
  public String toString() {
    return "[" + getXVal() + ", " + getYVal() + "," + text + "]";
  }


}
