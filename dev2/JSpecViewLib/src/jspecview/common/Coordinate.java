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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Comparator;


/**
 * The <code>Coordinate</code> class stores the x and y values of a coordinate.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class Coordinate {
  /** the x value */
  private double xVal = 0;
  /** the y value */
  private double yVal = 0;

  /** the format of the string returned by getXString() and getYString() */
  private static DecimalFormat formatter = new DecimalFormat("0.########",
      new DecimalFormatSymbols(java.util.Locale.US));

  /**
   * Constructor
   */
  public Coordinate() {
  }

  /**
   * Constructor
   * 
   * @param x
   *        the x value
   * @param y
   *        the y value
   */
  public Coordinate(double x, double y) {
    xVal = x;
    yVal = y;
  }

  /**
   * Returns the x value of the coordinate
   * 
   * @return the x value of the coordinate
   */
  public double getXVal() {
    return xVal;
  }

  /**
   * Returns the y value of the coordinate
   * 
   * @return the y value of the coordinate
   */
  public double getYVal() {
    return yVal;
  }

  /**
   * Returns the x value of the coordinate formatted to a maximum of eight
   * decimal places
   * 
   * @return Returns the x value of the coordinate formatted to a maximum of
   *         eight decimal places
   */
  public String getXString() {
    return formatter.format(xVal);
  }

  /**
   * Returns the y value of the coordinate formatted to a maximum of eight
   * decimal places
   * 
   * @return Returns the y value of the coordinate formatted to a maximum of
   *         eight decimal places
   */
  public String getYString() {
    return formatter.format(yVal);
  }

  /**
   * Sets the x value of the coordinate
   * 
   * @param val
   *        the x value
   */
  public void setXVal(double val) {
    xVal = val;
  }

  /**
   * Sets the y value of the coordinate
   * 
   * @param val
   *        the y value
   */
  public void setYVal(double val) {
    yVal = val;
  }

  /**
   * Returns a new coordinate that has the same x and y values of this
   * coordinate
   * 
   * @return Returns a new coordinate that has the same x and y values of this
   *         coordinate
   */
  public Coordinate copy() {
    return new Coordinate(xVal, yVal);
  }

  /**
   * Indicates whether some other Coordinate is equal to this one
   * 
   * @param coord
   *        the reference coordinate
   * @return true if the coordinates are equal, false otherwise
   */
  public boolean equals(Coordinate coord) {
    return (coord.xVal == xVal && coord.yVal == yVal);
  }

  /**
   * Overides Objects toString() method
   * 
   * @return the String representation of this coordinate
   */
  @Override
  public String toString() {
    return "[" + xVal + ", " + yVal + "]";
  }
  
  public static double getYValueAt(Coordinate[] xyCoords, double xPt,
                                   Comparator<Coordinate> c) {
    Coordinate x = new Coordinate(xPt, 0);
    int i = Arrays.binarySearch(xyCoords, x, c);
    if (i < 0) i = -1 - i;
    //System.out.println(i);
    if (i <= 0 || i >= xyCoords.length)
      return Double.NaN;
    double x1 = xyCoords[i].getXVal();
    double x0 = xyCoords[i - 1].getXVal();
    double y1 = xyCoords[i].getYVal();
    double y0 = xyCoords[i - 1].getYVal();
    //System.out.println(x0 + " " + xPt + " " + x1);
    if (x1 == x0)
      return y1; 
    return y0 + (y1 - y0) / (x1 - x0) * (xPt - x0);
  }
}
