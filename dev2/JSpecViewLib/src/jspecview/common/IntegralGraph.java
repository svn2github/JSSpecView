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

/**
 * class <code>IntegralGraph</code> implements the <code>Graph</code> interface.
 * It constructs an integral <code>Graph</code> from another <code>Graph</code>
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.common.Graph
 * @see jspecview.common.JDXSpectrum
 */
public class IntegralGraph extends IntegralData {

	private static final long serialVersionUID = 1L;

	/**
   * Calculates and initialises the <code>IntegralGraph</code> from an input <code>Graph</code>,
   *  the percentage Minimum Y value, the percent offset and the integral factor
   *  
   *  // old way -- left for android
   *  
   * @param graph the input graph
   * @param percentMinY percentage Minimum Y value
   * @param percentOffset the percent offset
   * @param integralRange the integral factor
   */
  public IntegralGraph(JDXSpectrum spectrum, 
  		double percentMinY, double percentOffset, double factor, String xUnits, String yUnits) {
  	super(percentMinY, percentOffset, factor, spectrum);
  }
  
}
