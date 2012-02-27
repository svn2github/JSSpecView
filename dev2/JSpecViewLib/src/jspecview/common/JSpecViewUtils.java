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

// CHANGES to 'JSpecViewUtils.java' - Application Utility Class
// University of the West Indies, Mona Campus
//
// 23-07-2011 jak - Altered appletIntegrate to support specifying
//					the integration plot color.
// 24-09-2011 jak - Added method to parse integration ratio annotation
//					string. Altered appletIntegrate to support integration
//					ratio annotations.

package jspecview.common;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

//import jspecview.application.MainFrame;
import java.text.DecimalFormatSymbols;

//import jspecview.exception.ScalesIncompatibleException;

/**
 * <code>JSpecViewUtils</code> contains static methods used to calculate values
 * for display of spectra and other utility methods used in the application.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof. Robert J. Lancashire
 */
public class JSpecViewUtils {

  /**
   * Global variable used to specify that the application is in debug mode.
   * Allows the printing out of certain debug information to the standard output
   */
  public static boolean DEBUG = false;

  /**
   * The line separator for the system that the program is running on
   */
  public static final String newLine = System.getProperty("line.separator");

  /**
   * The factor divisor used in compressing spectral data in one of DIF, SQZ,
   * PAC and FIX formats
   */
  public static final double FACTOR_DIVISOR = 1000000;

  /**
   * If the applet is used for teaching, then we may need to obscure the title of
   * the spectrum
   */
  public static boolean obscure;
  public static double integralMinY = 0.1;
  public static double integralFactor = 50;
  public static double integralOffset = 30;

  public static DecimalFormat getDecimalFormat(String hash) {
    return new DecimalFormat(hash, new DecimalFormatSymbols(java.util.Locale.US));
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
    double tempMaxX, tempX;
    tempMaxX = coords[start].getXVal();
    for (int index = start + 1; index <= end; index++) {
      tempX = coords[index].getXVal();
      tempMaxX = Math.max(tempMaxX, tempX);
    }
    return tempMaxX;
  }

  /**
   * Returns the maximum x value of an array of <code>Coordinate</code>s
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
    double tempMinX, tempX;
    tempMinX = coords[start].getXVal();
    for (int index = start + 1; index <= end; index++) {
      tempX = coords[index].getXVal();
      tempMinX = Math.min(tempMinX, tempX);
    }

    return tempMinX;
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
  public static double getMinY(Coordinate[] coords, int start, int end) {
    double tempMinY, tempY;
    tempMinY = coords[start].getYVal();
    for (int index = start + 1; index <= end; index++) {
      tempY = coords[index].getYVal();
      tempMinY = Math.min(tempMinY, tempY);
    }

    return tempMinY;
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
    double tempMaxY, tempY;
    tempMaxY = coords[start].getYVal();
    for (int index = start + 1; index <= end; index++) {
      tempY = coords[index].getYVal();
      tempMaxY = Math.max(tempMaxY, tempY);
    }
    return tempMaxY;
  }

  /**
   * Returns the minimum x value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @return the minimum x value of an array of <code>Coordinate</code>s
   */
  public static double getMinX(Coordinate[] coords) {
    return JSpecViewUtils.getMinX(coords, 0, coords.length - 1);
  }

  /**
   * Returns the minimum y value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @return the minimum y value of an array of <code>Coordinate</code>s
   */
  public static double getMinY(Coordinate[] coords) {
    return JSpecViewUtils.getMinY(coords, 0, coords.length - 1);
  }

  /**
   * Returns the maximum x value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @return the maximum x value of an array of <code>Coordinate</code>s
   */
  public static double getMaxX(Coordinate[] coords) {
    return JSpecViewUtils.getMaxX(coords, 0, coords.length - 1);
  }

  /**
   * Returns the maximum y value of an array of <code>Coordinate</code>s
   * 
   * @param coords
   *        the coordinates
   * @return the maximum y value of an array of <code>Coordinate</code>s
   */
  public static double getMaxY(Coordinate[] coords) {
    return JSpecViewUtils.getMaxY(coords, 0, coords.length - 1);
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
  public static double getMinX(Coordinate[][] coordLists, int[] startList,
                               int[] endList) {
    double tmpMinX, tmp;

    tmpMinX = getMinX(coordLists[0], startList[0], endList[0]);
    for (int i = 1; i < coordLists.length; i++) {
      tmp = getMinX(coordLists[i], startList[i], endList[i]);
      if (tmp < tmpMinX)
        tmpMinX = tmp;
    }

    return tmpMinX;
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
  public static double getMinY(Coordinate[][] coordLists, int[] startList,
                               int[] endList) {
    double tmpMinY, tmp;

    tmpMinY = getMinY(coordLists[0], startList[0], endList[0]);
    for (int i = 1; i < coordLists.length; i++) {
      tmp = getMinY(coordLists[i], startList[i], endList[i]);
      if (tmp < tmpMinY)
        tmpMinY = tmp;
    }

    return tmpMinY;
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
  public static double getMaxX(Coordinate[][] coordLists, int[] startList,
                               int[] endList) {
    double tmpMaxX, tmp;

    tmpMaxX = getMaxX(coordLists[0], startList[0], endList[0]);
    for (int i = 1; i < coordLists.length; i++) {
      tmp = getMaxX(coordLists[i], startList[i], endList[i]);
      if (tmp > tmpMaxX)
        tmpMaxX = tmp;
    }

    return tmpMaxX;
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
  public static double getMaxY(Coordinate[][] coordLists, int[] startList,
                               int[] endList) {
    double tmpMaxY, tmp;

    tmpMaxY = getMaxY(coordLists[0], startList[0], endList[0]);
    for (int i = 1; i < coordLists.length; i++) {
      tmp = getMaxY(coordLists[i], startList[i], endList[i]);
      if (tmp > tmpMaxY)
        tmpMaxY = tmp;
    }

    return tmpMaxY;
  }

  /**
   * Returns the X Compression factor by finding the subtracting the min and max
   * x values and dividing by the factor divisor
   * 
   * @param xyCoords
   *        an array of coordinates
   * @param startDataPointIndex
   *        the start index
   * @param endDataPointIndex
   *        the end index
   * @param factorDivisor
   *        the factor divisor
   * @return the X Compression factor
   */
  public static double getXFactorForCompression(Coordinate[] xyCoords,
                                                int startDataPointIndex,
                                                int endDataPointIndex,
                                                double factorDivisor) {

    double maxX = JSpecViewUtils.getMaxX(xyCoords, startDataPointIndex,
        endDataPointIndex);
    double minX = JSpecViewUtils.getMinX(xyCoords, startDataPointIndex,
        endDataPointIndex);

    return (maxX - minX) / factorDivisor;
  }

  /**
   * Returns the Y Compression factor by finding the subtracting the min and max
   * y values and dividing by the factor divisor
   * 
   * @param xyCoords
   *        an array of coordinates
   * @param startDataPointIndex
   *        the start index
   * @param endDataPointIndex
   *        the end index
   * @param factorDivisor
   *        the factor divisor
   * @return the Y Compression factor
   */
  public static double getYFactorForCompression(Coordinate[] xyCoords,
                                                int startDataPointIndex,
                                                int endDataPointIndex,
                                                double factorDivisor) {

    double maxY = JSpecViewUtils.getMaxY(xyCoords, startDataPointIndex,
        endDataPointIndex);
    double minY = JSpecViewUtils.getMinY(xyCoords, startDataPointIndex,
        endDataPointIndex);

    return (maxY - minY) / factorDivisor;
  }

  /**
   * Returns the X Compression factor by finding the subtracting the min and max
   * x values and dividing by the default factor divisor
   * 
   * @param xyCoords
   *        an array of coordinates
   * @param startDataPointIndex
   *        the start index
   * @param endDataPointIndex
   *        the end index
   * @see JSpecViewUtils#FACTOR_DIVISOR
   * @return the X Compression factor
   */
  public static double getXFactorForCompression(Coordinate[] xyCoords,
                                                int startDataPointIndex,
                                                int endDataPointIndex) {
    return getXFactorForCompression(xyCoords, startDataPointIndex,
        endDataPointIndex, FACTOR_DIVISOR);
  }

  /**
   * Returns the Y Compression factor by finding the subtracting the min and max
   * y values and dividing by the default factor divisor
   * 
   * @param xyCoords
   *        an array of coordinates
   * @param startDataPointIndex
   *        the start index
   * @param endDataPointIndex
   *        the end index
   * @see JSpecViewUtils#FACTOR_DIVISOR
   * @return the Y Compression factor
   */
  public static double getYFactorForCompression(Coordinate[] xyCoords,
                                                int startDataPointIndex,
                                                int endDataPointIndex) {
    return getYFactorForCompression(xyCoords, startDataPointIndex,
        endDataPointIndex, FACTOR_DIVISOR);
  }

  /**
   * 
   * @param obscure
   *        boolean
   * @return boolean
   */
  public static boolean setObscure(boolean obscure2) {
    obscure = obscure2;
    return obscure;
  }

  /**
   * Calculates values that <code>JSVPanel</code> needs in order to render a
   * graph, (eg. scale, min and max values) and stores the values in the
   * class <code>ScaleData</code>. Note: This
   * method is not used in the application, instead the more general
   * {@link jspecview.common.MultiScaleData} is generated with
   * the
   * {@link jspecview.common.JSpecViewUtils#generateScaleData(jspecview.common.Coordinate[][], int[], int[], int, int)}
   * method
   * 
   * @param coords
   *        the array of coordinates
   * @param start
   *        the start index
   * @param end
   *        the end index
   * @param initNumXDivisions
   *        the initial number of X divisions for scale
   * @param initNumYDivisions
   *        the initial number of Y divisions for scale
   * @return returns an instance of <code>ScaleData</code>
   */
  public static ScaleData generateScaleData(Coordinate[] coords, int start,
                                            int end, int initNumXDivisions,
                                            int initNumYDivisions) {
    ScaleData data = new ScaleData();

    // max and min values divided by the number of divisions
    // ie. how much the scale would go up by (the step)
    double spanX, spanY;
    double[] units = { 1.5, 2.0, 2.5, 4.0, 5.0, 8.0, 10.0 };
    // formats the spanX and spanY values into scientific notation
    DecimalFormat sciFormatter = getDecimalFormat("0.###E0");
    // index of the letter 'E' in spanX and spanY values formatted  in sci notation
    // as a string
    int indexOfE;
    // number to the left of the 'E'
    double leftOfE;
    // integer to the right of 'E'
    int rightOfE;
    int i;

    // startDataPointIndex and endDataPointIndex
    data.startDataPointIndex = start;
    data.endDataPointIndex = end;
    data.numOfPoints = data.endDataPointIndex - data.startDataPointIndex + 1;
    data.numInitXdiv = initNumXDivisions;

    // X Scale
    data.minX = JSpecViewUtils.getMinX(coords, start, end);
    data.maxX = JSpecViewUtils.getMaxX(coords, start, end);

    // using 10 divisions
    spanX = (data.maxX - data.minX) / initNumXDivisions;
    // spanX in sci notation as a string
    String strSpanX = sciFormatter.format(spanX);
    strSpanX = strSpanX.toUpperCase();
    indexOfE = strSpanX.indexOf('E');
    leftOfE = Double.parseDouble(strSpanX.substring(0, indexOfE));
    rightOfE = Integer.parseInt(strSpanX.substring(indexOfE + 1));
    // the number of decimal places that the scale number should be formatted to
    data.hashNumX = rightOfE;

    i = 0;
    //make sure scale values are multiples or factor of one of the values in the units array
    while (leftOfE > units[i] && i <= 6) {
      i++;
    }

    // find the new span based on the unit found
    data.xStep = Math.pow(10, rightOfE) * units[i];
    // the minimum x value for the scale
    data.minXOnScale = data.xStep * Math.floor((data.minX) / data.xStep);
    // the minimum y value for the scale
    data.maxXOnScale = data.xStep * Math.ceil((data.maxX) / data.xStep);

    // the number of divisions with the new step
    data.numOfXDivisions = (int) Math
        .ceil((data.maxXOnScale - data.minXOnScale) / data.xStep);

    // Find min and max x and y

    // Y Scale
    // Do the same for Y scale
    data.minY = JSpecViewUtils.getMinY(coords, start, end);
    data.maxY = JSpecViewUtils.getMaxY(coords, start, end);
    data.numInitYdiv = initNumYDivisions;

    if (data.minY == 0 && data.maxY == 0) {
      data.maxY = 1;
    }

    spanY = (data.maxY - data.minY) / initNumYDivisions;
    String strSpanY = sciFormatter.format(spanY);
    strSpanY = strSpanY.toUpperCase();
    indexOfE = strSpanY.indexOf('E');
    leftOfE = Double.parseDouble(strSpanY.substring(0, indexOfE));
    rightOfE = Integer.parseInt(strSpanY.substring(indexOfE + 1));
    data.hashNumY = rightOfE;

    i = 0;
    while (leftOfE > units[i] && i <= 6) {
      i++;
    }

    data.yStep = Math.pow(10, rightOfE) * units[i];
    data.minYOnScale = data.yStep * Math.floor((data.minY) / data.yStep);
    data.maxYOnScale = data.yStep * Math.ceil((data.maxY) / data.yStep);
    data.numOfYDivisions = (int) Math
        .ceil((data.maxYOnScale - data.minYOnScale) / data.yStep);

    return data;
  }

  /**
   * Calculates values that <code>JSVPanel</code> needs in order to render a
   * graph, (eg. scale, min and max values) and stores the values in the
   * class <code>ScaleData</code>.
   * 
   * @param coordLists
   *        an array of arrays of coordinates
   * @param startList
   *        the start indices
   * @param endList
   *        the end indices
   * @param initNumXDivisions
   *        the initial number of X divisions for scale
   * @param initNumYDivisions
   *        the initial number of Y divisions for scale
   * @return returns an instance of <code>MultiScaleData</code>
   */
  public static MultiScaleData generateScaleData(Coordinate[][] coordLists,
                                                 int[] startList,
                                                 int[] endList,
                                                 int initNumXDivisions,
                                                 int initNumYDivisions) {

    MultiScaleData data = new MultiScaleData();

    double spanX, spanY;
    double[] units = { 1.5, 2.0, 2.5, 4.0, 5.0, 8.0, 10.0 };
    DecimalFormat sciFormatter = getDecimalFormat("0.###E0");

    int indexOfE;
    double leftOfE;
    int rightOfE;
    int i;

    // startDataPointIndex and endDataPointIndex
    data.startDataPointIndices = startList;
    data.endDataPointIndices = endList;
    data.numInitXdiv = initNumXDivisions;

    int[] tmpList = new int[startList.length];
    for (int j = 0; j < startList.length; j++) {
      tmpList[j] = endList[j] - startList[j] + 1;
    }
    data.numOfPointsList = tmpList;

    // X Scale
    data.minX = JSpecViewUtils.getMinX(coordLists, startList, endList);
    data.maxX = JSpecViewUtils.getMaxX(coordLists, startList, endList);
    spanX = (data.maxX - data.minX) / initNumXDivisions;
    String strSpanX = sciFormatter.format(spanX);
    strSpanX = strSpanX.toUpperCase();
    indexOfE = strSpanX.indexOf('E');
    leftOfE = Double.parseDouble(strSpanX.substring(0, indexOfE));
    rightOfE = Integer.parseInt(strSpanX.substring(indexOfE + 1));
    data.hashNumX = rightOfE;

    i = 0;
    while (leftOfE > units[i] && i <= 6) {
      i++;
    }

    data.xStep = Math.pow(10, rightOfE) * units[i];
    data.minXOnScale = data.xStep * Math.floor((data.minX) / data.xStep);
    data.maxXOnScale = data.xStep * Math.ceil((data.maxX) / data.xStep);
    data.numOfXDivisions = (int) Math
        .ceil((data.maxXOnScale - data.minXOnScale) / data.xStep);

    // Find min and max x and y

    // Y Scale
    data.minY = JSpecViewUtils.getMinY(coordLists, startList, endList);
    data.maxY = JSpecViewUtils.getMaxY(coordLists, startList, endList);
    data.numInitYdiv = initNumYDivisions;

    if (data.minY == 0 && data.maxY == 0) {
      data.maxY = 1;
    }

    spanY = (data.maxY - data.minY) / initNumYDivisions;
    String strSpanY = sciFormatter.format(spanY);
    strSpanY = strSpanY.toUpperCase();
    indexOfE = strSpanY.indexOf('E');
    leftOfE = Double.parseDouble(strSpanY.substring(0, indexOfE));
    rightOfE = Integer.parseInt(strSpanY.substring(indexOfE + 1));
    data.hashNumY = rightOfE;

    i = 0;
    while (leftOfE > units[i] && i <= 6) {
      i++;
    }

    data.yStep = Math.pow(10, rightOfE) * units[i];
    data.minYOnScale = data.yStep * Math.floor((data.minY) / data.yStep);
    data.maxYOnScale = data.yStep * Math.ceil((data.maxY) / data.yStep);
    data.numOfYDivisions = (int) Math
        .ceil((data.maxYOnScale - data.minYOnScale) / data.yStep);

    return data;
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

    DecimalFormat formatter = getDecimalFormat("0.000000");
    StringBuffer buffer = new StringBuffer();

    if (endDataPointIndex > startDataPointIndex) {
      for (int index = startDataPointIndex; index <= endDataPointIndex; index++) {
        Coordinate point = xyCoords[index];
        if (numPerLine > 0) {
          buffer.append(formatter.format(point.getXVal()) + ", "
              + formatter.format(point.getYVal()) + " ");
          if (((index + 1) % numPerLine) == 0)
            buffer.append(JSpecViewUtils.newLine);
        } else {
          buffer.append(formatter.format(point.getXVal()) + ", "
              + formatter.format(point.getYVal()));
          if (index < endDataPointIndex)
            buffer.append(JSpecViewUtils.newLine);
        }
      }
    } else {
      for (int index = startDataPointIndex; index <= endDataPointIndex; index--) {
        Coordinate point = xyCoords[index];
        if (numPerLine > 0) {
          buffer.append(formatter.format(point.getXVal()) + ", "
              + formatter.format(point.getYVal()) + " ");
          if (((index + 1) % numPerLine) == 0)
            buffer.append(JSpecViewUtils.newLine);
        } else {
          buffer.append(formatter.format(point.getXVal()) + ", "
              + formatter.format(point.getYVal()));
          if (index < endDataPointIndex)
            buffer.append(JSpecViewUtils.newLine);
        }
      }

    }
    return buffer.toString();
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

    if (DEBUG) {
      System.out.print("Started Parsing DSV at: ");
      System.out.println(Calendar.getInstance().getTime());
      time1 = Calendar.getInstance().getTimeInMillis();
    }

    while (st.hasMoreTokens()) {
      tmp1 = st.nextToken().trim();
      tmp2 = st.nextToken().trim();

      if (DEBUG) {
        System.out.println("tkn1: " + tmp1);
        System.out.println("tkn2: " + tmp2);
      }

      xval = Double.parseDouble(tmp1);
      yval = Double.parseDouble(tmp2);
      point = new Coordinate((xval * xFactor), (yval * yFactor));
      xyCoords.add(point);
    }

    if (DEBUG) {
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
   * Finds a character that is in one string in another and returns the index
   * 
   * @param str
   *        the string to search
   * @param delim
   *        the string from which to find the characters to search for
   * @return the index of the of the character found,
   */
  public static int findOneOf(String str, String delim) {
    int n = str.length();
    for (int i = 0; i < n; i++)
      if (delim.indexOf(str.charAt(i)) >= 0)
        return i;
    return  -1;
  }

  /**
   * Parses integration ratios and x values from a string and returns them as
   * <code>IntegrationRatio</code> objects
   * 
   * @param value
   * @return ArrayList<IntegrationRatio> object representing integration ratios
   */
  public static ArrayList<IntegrationRatio> getIntegrationRatiosFromString(
                                                                           String value) {
    // split input into x-value/integral-value pairs
    StringTokenizer allParamTokens = new StringTokenizer(value, ",");

    // create array list to return
    ArrayList<IntegrationRatio> inputRatios = new ArrayList<IntegrationRatio>();

    while (allParamTokens.hasMoreTokens()) {
      String token = allParamTokens.nextToken();
      // now split the x-value/integral-value pair
      StringTokenizer eachParam = new StringTokenizer(token, ":");
      IntegrationRatio inputRatio = new IntegrationRatio();
      inputRatio.setXVal(Double.parseDouble(eachParam.nextToken()));
      inputRatio.setYVal(0.0);
      inputRatio.setIntegralVal(Double.parseDouble(eachParam.nextToken()));
      inputRatios.add(inputRatio);
    }

    return inputRatios;
  }

}
