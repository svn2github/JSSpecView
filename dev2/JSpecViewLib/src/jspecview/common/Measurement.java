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
 * The <code>Measurement</code> class stores an annotation that is a measurement
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class Measurement extends Annotation {
  
	private Coordinate pt2 = new Coordinate();
	private double value;

	private boolean isPending;
	
  public Measurement(JDXSpectrum spec, double x, double y) {
		super(spec, x, y, "", false, false, 0, 6);
		setPt2(x, y);
	}
  
  public Measurement(JDXSpectrum spec, double x, double y, String text, double x1, double y1) {
		super(spec, x, y, text, false, false, 0, 6);
		setPt2(x1, y1);
	}
  
  public Measurement(Measurement m) {
  	super(m.spec, m.getXVal(), m.getYVal(), m.text, false, false, m.offsetX, m.offsetY);
  	setPt2(m.pt2.getXVal(), m.pt2.getYVal());
	}

	public Measurement(JDXSpectrum spec, double x, double y, String text, double value) {
		// peak picking
		super(spec, x, y, text, false, false, 0, 6);
		this.value = value;
		pt2.setXVal(x);
		pt2.setYVal(y);
	}

	public Measurement(JDXSpectrum spec, double x, double y, boolean b) {
		 super(spec, x, y, b);
	}

	public void setPt2(double x, double y) {
		pt2.setXVal(x);
		pt2.setYVal(y);
		value = Math.abs(x - getXVal());
		text = spec.setMeasurementText(this);
  }
  
	public JDXSpectrum getSpectrum() {
		return spec;
	}
  
  public void setValue(double value) {
  	this.value = value;
		text = spec.setMeasurementText(this);
  }

  public double getValue() {
  	return value;
  }
  
  /**
   * Overrides Objects toString() method
   * 
   * @return the String representation of this coordinate
   */
  @Override
  public String toString() {
    return "[" + getXVal() + "-" + pt2.getXVal() + "]";
  }

	public double getXVal2() {
		return pt2.getXVal();
	}

	public double getYVal2() {
		return pt2.getYVal();
	}

	public void addSpecShift(double dx) {
    setXVal(getXVal() + dx);
    pt2.setXVal(pt2.getXVal() + dx);
	}

	public void setYVal2(double y2) {
		pt2.setYVal(y2);
	}

}
