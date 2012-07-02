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
	JDXSpectrum spec;
	private double value;
	
  public Measurement(GraphSet gs, JDXSpectrum spec, double x, double y, String text, double x1, double y1) {
		super(x, y, text, false, false, 0, 6);
		this.spec = spec;
		setPt2(gs, x1, y1);
	}
  
  public Measurement(GraphSet gs, Measurement m) {
  	super(m.getXVal(), m.getYVal(), m.text, false, false, m.offsetX, m.offsetY);
  	this.spec = m.spec;
  	setPt2(gs, m.pt2.getXVal(), m.pt2.getYVal());
	}

	public void setPt2(GraphSet gs, double x, double y) {
		pt2.setXVal(x);
		pt2.setYVal(y);
		text = spec.setMeasurementText(gs, this);
  }
  
	public JDXSpectrum getSpectrum() {
		return spec;
	}
  
  public void setValue(double value) {
  	this.value = value;
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

	public Coordinate getPt2() {
		return pt2;
	}

}
