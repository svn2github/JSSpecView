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

package jspecview.common;

import java.util.List;


/**
 * class <code>IntegralGraph</code> implements the <code>Graph</code> interface.
 * It constructs an integral <code>Graph</code> from another <code>Graph</code>
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.common.Graph
 * @see jspecview.common.JDXSpectrum
 */
public class IntegralGraph implements Graph {

  enum IntMode {
    OFF, ON, TOGGLE, MARK;
    static IntMode getMode(String value) {
      for (IntMode mode: values())
        if (mode.name().equalsIgnoreCase(value))
          return mode;
      return OFF;
    }
  }
  
  /**
   * Calculates and initialises the <code>IntegralGraph</code> from an input <code>Graph</code>,
   *  the percentage Minimum Y value, the percent offset and the integral factor
   * @param graph the input graph
   * @param percentMinY percentage Minimum Y value
   * @param percentOffset the percent offset
   * @param integralFactor the integral factor
   */
  public IntegralGraph(JDXSpectrum spectrum, 
  		double percentMinY, double percentOffset, double factor, String xUnits, String yUnits) {
    this.spectrum = spectrum;
    id = new IntegralData(percentMinY, percentOffset, factor);
    xyCoords = id.calculateIntegral(this);
  }
  
  public void dispose() {
    id = null;
    spectrum = null;
  }

  JDXSpectrum spectrum;  
  
  private Coordinate xyCoords[];
  public Coordinate[] getXYCoords() {
    return xyCoords;
  }

	private IntegralData id;	
	IntegralData getIntegralData() {
		return id;
	}
	void setIntegralData(IntegralData id) {
		this.id = id;
	}
	
  public List<Measurement> getIntegralRegions() {
    return id.integralRegions;
  }

  public void addIntegralRegion(double x1, double x2, boolean isFinal) {
  	id.addIntegralRegion(this, x1, x2, isFinal);
  }
    
  public void addMarks(String ppms) {
  	id.addMarks(this, ppms);
  }

	public void scaleIntegrationBy(double factor) {
		id.scaleIntegrationBy(factor);
	}

  /**
   * Recalculates the integral normalized to [0 - 1]
   */
  public void recalculate(){
    xyCoords = id.calculateIntegral(this);
  }

  /**
   * returns FRACTIONAL value * 100
   */
  public double getPercentYValueAt(double x) {
 //   double y = getYValueAt(x);
 //   double y0 = xyCoords[xyCoords.length - 1].getYVal();
 //   double y1 = xyCoords[0].getYVal();
 //   return (y - y0) / (y1 - y0) * 100;
    return getYValueAt(x) * 100;
  }

  double getYValueAt(double x) {
    return Coordinate.getYValueAt(xyCoords, x);
  }

	public void addSpecShift(double dx) {
		Coordinate.shiftX(xyCoords, dx);
		id.addSpecShift(dx);
	}

}
