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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import jspecview.util.Logger;
import jspecview.util.TextFormat;


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
  private static DecimalFormat formatter = TextFormat.getDecimalFormat("0.########");

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
  
  /**
   * Parses data stored in x, y format
   * 
   * @param dataPoints
   *        the data as string
   * @param xFactor
   *        the factor to apply to x values
   * @param yFactor
   *        the factor to apply to y values
   * @return an array of <code>Coordinate</code>s
   */
  public static Coordinate[] parseDSV(String dataPoints, double xFactor,
                                      double yFactor) {
    // for debugging;
    long time1, time2, totalTime;
    time1 = 0;
    // for debugging
  
    //int linenumber = 0;
    Coordinate point;
    double xval = 0;
    double yval = 0;
    ArrayList<Coordinate> xyCoords = new ArrayList<Coordinate>(1024);
  
    String delim = " \t\n\r\f,;";
    StringTokenizer st = new StringTokenizer(dataPoints, delim);
    String tmp1, tmp2;
  
    if (Logger.debugging) {
      System.out.print("Started Parsing DSV at: ");
      System.out.println(Calendar.getInstance().getTime());
      time1 = Calendar.getInstance().getTimeInMillis();
    }
  
    while (st.hasMoreTokens()) {
      tmp1 = st.nextToken().trim();
      tmp2 = st.nextToken().trim();
  
      if (Logger.debugging) {
        System.out.println("tkn1: " + tmp1);
        System.out.println("tkn2: " + tmp2);
      }
  
      xval = Double.parseDouble(tmp1);
      yval = Double.parseDouble(tmp2);
      point = new Coordinate((xval * xFactor), (yval * yFactor));
      xyCoords.add(point);
    }
  
    if (Logger.debugging) {
      System.out.print("Finished Parsing DSV at: ");
      System.out.println(Calendar.getInstance().getTime());
      time2 = Calendar.getInstance().getTimeInMillis();
      totalTime = time2 - time1;
      System.out.println("Total time = " + totalTime + "ms or "
          + ((double) totalTime / 1000) + "s");
    }
  
    Coordinate[] coord = new Coordinate[xyCoords.size()];
    return (Coordinate[]) xyCoords.toArray(coord);
  }

  /**
   * Converts and returns the list of Coordinates as a string with the number of
   * coordinate per line specified by numPerLine argument
   * 
   * @param xyCoords
   *        the array of coordinates
   * @param startDataPointIndex
   *        that start index
   * @param endDataPointIndex
   *        the end index
   * @param numPerLine
   *        number of coordinates per line
   * @return returns the list of Coordinates as a string
   */
  public static String coordinatesToString(Coordinate[] xyCoords,
                                           int startDataPointIndex,
                                           int endDataPointIndex, int numPerLine) {
  
    DecimalFormat formatter = TextFormat.getDecimalFormat("0.000000");
    StringBuffer buffer = new StringBuffer();
  
    if (endDataPointIndex > startDataPointIndex) {
      for (int index = startDataPointIndex; index <= endDataPointIndex; index++) {
        Coordinate point = xyCoords[index];
        if (numPerLine > 0) {
          buffer.append(formatter.format(point.getXVal()) + ", "
              + formatter.format(point.getYVal()) + " ");
          if (((index + 1) % numPerLine) == 0)
            buffer.append(TextFormat.newLine);
        } else {
          buffer.append(formatter.format(point.getXVal()) + ", "
              + formatter.format(point.getYVal()));
          if (index < endDataPointIndex)
            buffer.append(TextFormat.newLine);
        }
      }
    } else {
      for (int index = startDataPointIndex; index <= endDataPointIndex; index--) {
        Coordinate point = xyCoords[index];
        if (numPerLine > 0) {
          buffer.append(formatter.format(point.getXVal()) + ", "
              + formatter.format(point.getYVal()) + " ");
          if (((index + 1) % numPerLine) == 0)
            buffer.append(TextFormat.newLine);
        } else {
          buffer.append(formatter.format(point.getXVal()) + ", "
              + formatter.format(point.getYVal()));
          if (index < endDataPointIndex)
            buffer.append(TextFormat.newLine);
        }
      }
  
    }
    return buffer.toString();
  }

  /**
   * Returns the Delta X value
   * 
   * @param last
   *        the last x value
   * @param first
   *        the first x value
   * @param numPoints
   *        the number of data points
   * @return the Delta X value
   */
  public static double deltaX(double last, double first, int numPoints) {
    double test = (last - first) / (numPoints - 1);
    return test;
  }

  /**
   * Removes the scale factor from the coordinates
   * 
   * @param xyCoords
   *        the array of coordinates
   * @param xScale
   *        the scale for the x values
   * @param yScale
   *        the scale for the y values
   */
  public static void removeScale(Coordinate[] xyCoords, double xScale,
                                 double yScale) {
    applyScale(xyCoords, (1 / xScale), (1 / yScale));
  }

  /**
   * Apply the scale factor to the coordinates
   * 
   * @param xyCoords
   *        the array of coordinates
   * @param xScale
   *        the scale for the x values
   * @param yScale
   *        the scale for the y values
   */
  public static void applyScale(Coordinate[] xyCoords, double xScale,
                                double yScale) {
    if (xScale != 1 || yScale != 1) {
      for (int i = 0; i < xyCoords.length; i++) {
        xyCoords[i].setXVal(xyCoords[i].getXVal() * xScale);
        xyCoords[i].setYVal(xyCoords[i].getYVal() * yScale);
      }
    }
  }

  /**
   * Applies the shift reference to all coordinates
   * 
   * @param xyCoords
   *        an array of coordinates
   * @param dataPointNum
   *        the number of the data point in the the spectrum, indexed from 1
   * @param firstX
   *        the first X value
   * @param lastX
   *        the last X value
   * @param offset
   *        the offset value
   * @param observedFreq
   *        the observed frequency
   * @param shiftRefType
   *        the type of shift
   * @throws IndexOutOfBoundsException
   */
  public static void applyShiftReference(Coordinate[] xyCoords,
                                         int dataPointNum, double firstX,
                                         double lastX, double offset,
                                         double observedFreq, int shiftRefType)
      throws IndexOutOfBoundsException {
  
    if (dataPointNum > xyCoords.length || dataPointNum < 0)
      //throw new IndexOutOfBoundsException();
      return;
  
    Coordinate coord;
    switch (shiftRefType) {
    case 0:
      //double deltaX = JSpecViewUtils.deltaX(xyCoords[xyCoords.length - 1].getXVal(), xyCoords[0].getXVal(), xyCoords.length);     
      offset = xyCoords[xyCoords.length - dataPointNum].getXVal() - offset * observedFreq;
      break;
    case 1:
      offset = firstX - offset * observedFreq;
      break;
    case 2:
      offset = lastX + offset;
      break;
    }
  
    for (int index = 0; index < xyCoords.length; index++) {
      coord = xyCoords[index];
      coord.setXVal(coord.getXVal() - offset);
      xyCoords[index] = coord;
    }
  
    firstX -= offset;
    lastX -= offset;
  }

  /**
   * Returns the minimum x value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @param start
   *        the starting index
   * @param end
   *        the ending index
   * @return the maximum x value of an array of <code>Coordinate</code>s
   */
  public static double getMinX(Coordinate[] coords, int start, int end) {   
    double min = Double.MAX_VALUE;
    for (int index = start; index <= end; index++) {
      double tmp = coords[index].getXVal();
      if (tmp < min)
      min = tmp;
    }
    return min;
  }

  /**
   * Returns the minimum x value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @return the minimum x value of an array of <code>Coordinate</code>s
   */
  public static double getMinX(Coordinate[] coords) {
    return getMinX(coords, 0, coords.length - 1);
  }

  /**
   * Returns the minimum x value value from an array of arrays of
   * <code>Coordinate</code>s.
   * 
   * @param coordLists
   *        the 2d coordinate array
   * @param startList
   *        the start indices
   * @param endList
   *        the end indices
   * @return the minimum x value value from an array of arrays of
   *         <code>Coordinate</code>s
   */
  public static double getMinX(Graph[] spectra, int[] startList,
                               int[] endList) {
    double min = Double.MAX_VALUE;
    for (int i = 0; i < spectra.length; i++) {
      double tmp = getMinX(spectra[i].getXYCoords(), startList[i], endList[i]);
      if (tmp < min)
        min = tmp;
    }
    return min;
  }

  public static double getMinX(List<JDXSpectrum> spectra, int start, int end) {
    double min = Double.MAX_VALUE;
    for (int i = 0; i < spectra.size(); i++) {
      double tmp = getMinX(spectra.get(i).getXYCoords(), start, end);
      if (tmp < min)
        min = tmp;
    }  
    return min;
  }

  /**
   * Returns the minimum x value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @param start
   *        the starting index
   * @param end
   *        the ending index
   * @return the minimum x value of an array of <code>Coordinate</code>s
   */
  public static double getMaxX(Coordinate[] coords, int start, int end) {
    double max = -Double.MAX_VALUE;
    for (int index = start; index <= end; index++) {
      double tmp = coords[index].getXVal();
      if (tmp > max)
        max = tmp;
    }
    return max;
  }

  /**
   * Returns the maximum x value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @return the maximum x value of an array of <code>Coordinate</code>s
   */
  public static double getMaxX(Coordinate[] coords) {
    return getMaxX(coords, 0, coords.length - 1);
  }

  /**
   * Returns the maximum x value value from an array of arrays of
   * <code>Coordinate</code>s.
   * 
   * @param coordLists
   *        the 2d coordinate array
   * @param startList
   *        the start indices
   * @param endList
   *        the end indices
   * @return the maximum x value value from an array of arrays of
   *         <code>Coordinate</code>s
   */
  public static double getMaxX(Graph[] spectra, int[] startList,
                               int[] endList) {
    double max = -Double.MAX_VALUE;
    for (int i = 0; i < spectra.length; i++) {
      double tmp = getMaxX(spectra[i].getXYCoords(), startList[i], endList[i]);
      if (tmp > max)
        max = tmp;
    }
    return max;
  }

  public static double getMaxX(List<JDXSpectrum> spectra, int start, int end) {
    double max = -Double.MAX_VALUE;
    for (int i = 0; i < spectra.size(); i++) {
      double tmp = getMaxX(spectra.get(i).getXYCoords(), start, end);
      if (tmp > max)
        max = tmp;
    }  
    return max;
  }

  /**
   * Returns the minimum y value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @param start
   *        the starting index
   * @param end
   *        the ending index
   * @return the minimum y value of an array of <code>Coordinate</code>s
   */
  public static double getMinY(Coordinate[] coords, int start, int end) {
    double min = Double.MAX_VALUE;
    for (int index = start; index <= end; index++) {
      double tmp = coords[index].getYVal();
      if (tmp < min)
      min = tmp;
    }
    return min;
  }

  
  /**
   * Returns the minimum y value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @return the minimum y value of an array of <code>Coordinate</code>s
   */
  public static double getMinY(Coordinate[] coords) {
    return getMinY(coords, 0, coords.length - 1);
  }

  /**
   * Returns the minimum y value value from an array of arrays of
   * <code>Coordinate</code>s.
   * 
   * @param coordLists
   *        the 2d coordinate array
   * @param startList
   *        the start indices
   * @param endList
   *        the end indices
   * @return the minimum y value value from an array of arrays of
   *         <code>Coordinate</code>s
   */
  public static double getMinYUser(Graph[] spectra, int[] startList,
                               int[] endList) {
    double min = Double.MAX_VALUE;
    for (int i = 0; i < spectra.length; i++) {
      double tmp = getMinY(spectra[i].getXYCoords(), startList[i], endList[i]) * spectra[i].getUserYFactor();
      if (tmp < min)
        min = tmp;
    }  
    return min;
  }

  public static double getMinYUser(List<JDXSpectrum> spectra) {
    double min = Double.MAX_VALUE;
    for (int i = spectra.size(); --i >= 0; ) {
      double tmp = getMinY(spectra.get(i).getXYCoords()) * spectra.get(i).getUserYFactor();
      if (tmp < min)
        min = tmp;
    }  
    return min;
  }

  /**
   * Returns the maximum y value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @param start
   *        the starting index
   * @param end
   *        the ending index
   * @return the maximum y value of an array of <code>Coordinate</code>s
   */
  public static double getMaxY(Coordinate[] coords, int start, int end) {
    double max = -Double.MAX_VALUE;
    for (int index = start; index <= end; index++) {
      double tmp = coords[index].getYVal();
      if (tmp > max)
        max = tmp;
    }
    return max;
  }

  /**
   * Returns the maximum y value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @return the maximum y value of an array of <code>Coordinate</code>s
   */
  public static double getMaxY(Coordinate[] coords) {
    return getMaxY(coords, 0, coords.length - 1);
  }

  /**
   * Returns the maximum y value value from an array of arrays of
   * <code>Coordinate</code>s.
   * 
   * @param coordLists
   *        the 2d coordinate array
   * @param startList
   *        the start indices
   * @param endList
   *        the end indices
   * @return the maximum y value value from an array of arrays of
   *         <code>Coordinate</code>s
   */
  public static double getMaxYUser(Graph[] spectra, int[] startList,
                               int[] endList) {
    double max = -Double.MAX_VALUE;
    for (int i = 0; i < spectra.length; i++) {
      double tmp = getMaxY(spectra[i].getXYCoords(), startList[i], endList[i]) * spectra[i].getUserYFactor();
      if (tmp > max)
        max = tmp;
    }
    return max;
  }

  public static double getMaxYUser(List<JDXSpectrum> spectra) {
    double max = -Double.MAX_VALUE;
    for (int i = spectra.size(); --i >= 0; ) {
      double tmp = getMaxY(spectra.get(i).getXYCoords()) * spectra.get(i).getUserYFactor();
      if (tmp > max)
        max = tmp;
    }  
    return max;
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
