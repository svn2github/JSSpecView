/* Copyright (c) 2002-2010 The University of the West Indies
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

// CHANGES to 'JDXDecompressor.java' - 
// University of the West Indies, Mona Campus
//
// 23-08-2010 fix for DUP before DIF e.g. at start of line

package jspecview.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import jspecview.common.Coordinate;
import jspecview.common.JSpecViewUtils;


/**
 * JDXDecompressor contains static methods to decompress the data part of 
 * JCAMP-DX spectra that have been compressed using DIF, FIX, SQZ or PAC formats.
 * If you wish to parse the data from XY formats see
 * {@link jspecview.common.JSpecViewUtils#parseDSV(java.lang.String, double, double)}
 * @author Christopher Muir
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.export.JDXCompressor
 */
public class JDXDecompressor {


  /**
   * ASDF Compression
   */
  public static final int ASDF = 0;
  /**
   * AFFN Compression
   */
  public static final int AFFN = 1;


  /**
   * Error
   */
  public static final long ERROR_CODE = Long.MIN_VALUE;

  /**
   * The data
   */
  private String data;

  /**
   * The x compression factor
   */
  private double xFactor;

  /**
   * The y compression factor
   */
  private double yFactor;

  /**
   * The delta X value
   */
  private double deltaX;

  /**
   * All Delimiters in a JCAMP-DX compressed file
   */
  private static final String allDelim = "?+- %@ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrs\t\n";

  /**
   * The current line index
   */
  private int lineIndex;

  /**
   * Set to true whenever a SQZ character is encountered
   */
  //private boolean isSqz = false;

  /**
   * The line number of the dataset label in the source file
   */
  private int labelLineNo = 0;
  private int nPoints;


  /**
   * Initialises the <code>JDXDecompressor</code> from the compressed data,
   * the x factor, the y factor and the deltaX value
   * @param data the data to be decompressed
   * @param xFactor the x factor
   * @param yFactor the y factor
   * @param deltaX the delta X value
   * @param nPoints 
   */
  public JDXDecompressor(String data, double xFactor, double yFactor, double deltaX, int nPoints){
    this.data = data;
    this.xFactor = xFactor;
    this.yFactor = yFactor;
    this.deltaX = deltaX;
    this.nPoints = nPoints;
  }

  /**
   * Initialises the <code>JDXDecompressor</code> from the compressed data, the
   * x factor, the y factor and the deltaX value, the last X and first X values
   * in the source and the number of points
   * 
   * @param data
   *        compressed data
   * @param xFactor
   *        the x factor
   * @param yFactor
   *        the y factor
   * @param lastX
   *        the last x value
   * @param firstX
   *        the first x value
   * @param nPoints
   *        the number of points
   */
  public JDXDecompressor(String data, double xFactor, double yFactor,
      double lastX, double firstX, int nPoints){
    this.data = data;
    this.xFactor = xFactor;
    this.yFactor = yFactor;
    this.deltaX = JSpecViewUtils.deltaX(lastX, firstX, nPoints);
    this.nPoints = nPoints;
  }

  /**
   * Determines the type of compression, decompress the data and stores
   * coordinates in an array to be returned
   * 
   * @param errorLog
   * @return the array of <code>Coordinate</code>s
   */
  public Coordinate[] decompressData(StringBuffer errorLog) {
    int compressionType = getCompressionType();

    if (compressionType == -1)
      return null;

    switch (compressionType) {
    case JDXDecompressor.ASDF:
      return decompressASDF(errorLog);
    case JDXDecompressor.AFFN:
      return decompressAFFN(errorLog);
    default:
      return null;
    }
  }

  /**
   * Returns the compresssion type of the Source data
   * @return the compresssion type of the Source data
   */
  public int getCompressionType(){
    String dif = "%JKLMNOPQRjklmnopqr";
    String sqz = "@ABCDFGHIabcdfghi";
    String pac = " \t+-";
    String dsv = ",;";


    String line = data.substring(0,data.indexOf("\n",0));

    if (JSpecViewUtils.findOneOf(line,dif) != -1 ||
        JSpecViewUtils.findOneOf(line,sqz) != -1)
        return JDXDecompressor.ASDF;
    else if (JSpecViewUtils.findOneOf(line,dsv) == -1 &&
            JSpecViewUtils.findOneOf(line,pac) != -1)
        return JDXDecompressor.AFFN;

    return -1;
  }

  private Coordinate[] xyCoords;
  private int ipt;
  private void addPoint(Coordinate pt) {
    if (ipt == xyCoords.length) {
      Coordinate[] t = new Coordinate[ipt * 2];
      System.arraycopy(xyCoords, 0, t, 0, ipt);
      xyCoords = t;
    }
    xyCoords[ipt++] = pt;
  }

  /**
   * Decompresses DIF format
   * @param errorLog 
   * 
   * @return the array of <code>Coordinate</code>s
   */
  private Coordinate[] decompressASDF(StringBuffer errorLog) {
    char ch;
    int linenumber = labelLineNo;
    String line = null;
    int dupFactor, i;
    double xval = 0;
    double yval = 0;
    double difval = 0;
    String Dif = "JKLMNOPQR%jklmnopqr";
    String Dup = "STUVWXYZs";
    String Sqz = "ABCDEFGHI@abcdefghi";
    Coordinate point;
    xyCoords = new Coordinate[nPoints];

    double difMax = Math.abs(0.35 * deltaX);
    double dif14 = Math.abs(1.4 * deltaX);
    double dif06 = Math.abs(0.6 * deltaX);

    BufferedReader dataReader = new BufferedReader(new StringReader(data));
    try {
      line = dataReader.readLine();
    } catch (IOException ioe) {
    }
    lineIndex = 0;
    ipt = 0;

    while (line != null) {
      linenumber++;
      if (lineIndex <= line.length()) {
        point = new Coordinate();
        xval = getFirstXval(line);
        xval = (xval * xFactor);
        point.setXVal(xval);
        yval = getYvalDIF(line, linenumber);
        point.setYVal(yval * yFactor);
        if (ipt == 0) {
          addPoint(point); // first data line only
          continue;
        }
        Coordinate last_pt = xyCoords[ipt - 1];
        double xdif = Math.abs(last_pt.getXVal() - point.getXVal());
        // DIF Y checkpoint means X value does not advance at start
        // of new line. Remove last values and put in latest ones
        if (xdif < difMax) {
          Coordinate old_lastPt = xyCoords[ipt - 1] = point;
          //(Coordinate) xyCoords.set(xyCoords.size() - 1, point);
          // Check for Y checkpoint error - Y values should correspond
          double y = Math.abs(old_lastPt.getYVal());
          double y1 = Math.abs(point.getYVal());
          if (y1 < 0.6 * y || y1 > 1.4 * y)
            errorLog.append("ASDF Y Checkpoint Error! Line " + linenumber + " y1/y0=" + y1/y
                + " for y1=" + y1 + " y0=" + y + "\n");
        } else {
          addPoint(point);
          // Check for X checkpoint error
          // first point of new line should be deltaX away
          // ACD/Labs seem to have large rounding error so using between 0.6 and 1.4
          if (xdif > dif14 || xdif < dif06)
            errorLog.append("ASDF X Checkpoint Error! Line " + linenumber + " |x1-x0|="
                + xdif + " for x1=" + point.getXVal() + " x0=" + last_pt.getXVal() + "\n");
        }
      }
      while (lineIndex < line.length()) {
        ch = line.charAt(lineIndex);
        if (Dif.indexOf(ch) != -1) {
          point = new Coordinate();
          xval += deltaX;
          point.setXVal(xval);
          difval = getYvalDIF(line, linenumber);
          yval += difval;
          point.setYVal(yval * yFactor);
          addPoint(point);
        } else if (Dup.indexOf(ch) != -1) {
          dupFactor = getDUPVal(line, line.charAt(lineIndex));
          for (i = 1; i < dupFactor; i++) {
            point = new Coordinate();
            xval += deltaX;
            point.setXVal(xval);
            yval += difval;
            point.setYVal(yval * yFactor);
            addPoint(point);
          }
        } else if (Sqz.indexOf(ch) != -1) {
          point = new Coordinate();
          xval += deltaX;
          point.setXVal(xval);
          yval = getYvalDIF(line, linenumber);
          point.setYVal(yval * yFactor);
          addPoint(point);
        } else if (ch == '?') {
          // Check for missing points in file
          lineIndex++;
          xval += deltaX;
          errorLog.append("ASDF Error -- Invalid Data Symbol Found! Line " + linenumber
              + " ch=" + ch + "\n");
        }
        // check for spaces
        else if (ch == ' ') {
          lineIndex++;
        }
      }
      try {
        line = dataReader.readLine();
        difval = 0;
      } catch (IOException ioe) {
      }
      lineIndex = 0;
    }

    if (nPoints != ipt) {
      errorLog.append("ASDF decompressor did not find " + nPoints + " points -- instead " + ipt + "\n");
      Coordinate[] temp = new Coordinate[ipt];
      System.arraycopy(xyCoords, 0, temp, 0, ipt);
      xyCoords = temp;
    }
    return (deltaX > 0 ? xyCoords : reverse(xyCoords));
  }



  private static Coordinate[] reverse(Coordinate[] x) {
    int n = x.length;
    for (int i = 0; i < n; i++) {
      Coordinate v = x[i];
      x[i] = x[--n];
      x[n] = v;
    }
    return x;
  }

  /**
   * Decompresses AFFN format
   * 
   * @return the array of <code>Coordinate</code>s
   */
  private Coordinate[] decompressAFFN(StringBuffer errorLog) {
    char ch;
    int i;
    String line = null;
    int dupFactor;
    int linenumber = labelLineNo;
    Coordinate point;
    double xval = 0;
    double yval = 0;
    String Pac = "+-.0123456789";
    String Dup = "STUVWXYZs";

    lineIndex = 0;
    xyCoords = new Coordinate[nPoints];
    ipt = 0;

    double dx08 = Math.abs(.8 * deltaX);
    double dx12 = Math.abs(1.2 * deltaX);

    BufferedReader dataReader = new BufferedReader(new StringReader(data));
    try {
      line = dataReader.readLine();
    } catch (IOException ioe) {
    }
    while (line != null) {
      linenumber++;
      if (lineIndex <= line.length()) {
        point = new Coordinate();
        xval = getFirstXval(line) * xFactor;
        if (ipt > 0) {
          Coordinate last_pt = new Coordinate();
          last_pt = xyCoords[ipt - 1];
          //Check for X checkpoint error
          double xdif = Math.abs(xval - last_pt.getXVal());
          if (xdif < dx08 && xdif > dx12) {
            errorLog.append("AFFN X Checkpoint Error! Line " + linenumber + " |x1-x0|="
                + xdif
                + " for x1=" + xval + " x0=" + last_pt.getXVal() + "\n");
          }
        }
      }

      while (lineIndex < line.length()) {
        point = new Coordinate();
        point.setXVal(xval);
        ch = line.charAt(lineIndex);
        if (Pac.indexOf(ch) != -1) {
          yval = getYvalPAC(line, linenumber);
          point.setYVal(yval * yFactor);
          if (yval != ERROR_CODE)
            addPoint(point);
          xval += deltaX;
        } else if (Dup.indexOf(ch) != -1) {
          dupFactor = getDUPVal(line, line.charAt(lineIndex));
          for (i = 1; i < dupFactor; i++) {
            point = new Coordinate();
            point.setXVal(xval);
            point.setYVal(yval * yFactor);
            addPoint(point);
            xval += deltaX;
          }
        }
        else if (ch == '?') {        // Check for missing points in file
          lineIndex++;
          point.setXVal(point.getXVal() + deltaX);
          errorLog.append("AFFN Error: Invalid Data Symbol Found! Line " + linenumber
              + " ch=" + ch + "\n");
        } else
          lineIndex++;
      }
      try {
        line = dataReader.readLine();
      } catch (IOException ioe) {
      }
      lineIndex = 0;
    }
    if (nPoints != ipt) {
      errorLog.append("AFFN decompressor did not find " + nPoints + " points -- instead " + ipt + "\n");
      Coordinate[] temp = new Coordinate[ipt];
      System.arraycopy(xyCoords, 0, temp, 0, ipt);
      xyCoords = temp;
    }
    return (deltaX > 0 ? xyCoords : reverse(xyCoords));
  }


  /**
   * Get first X-Val for a (X++(Y..Y)) data set
   * @param line a line of data
   * @return the first x value
   */
  private double getFirstXval(String line)
  {
    String temp = null;
    int pos;
    int disp = checkForExp(line);

    // Check if first character is +/- which are delimiters
    if ((line.charAt(0) == '-') || (line.charAt(0) == '+')){
      if (disp != -1)
        pos = JSpecViewUtils.findOneOf(line.substring(disp),allDelim) + 1 + disp;
      else
        pos = JSpecViewUtils.findOneOf(line.substring(1),allDelim) + 1;
    }
    else{
      if (disp != -1)
        pos = JSpecViewUtils.findOneOf(line.substring(disp), allDelim) + disp;
      else
        pos = JSpecViewUtils.findOneOf(line,allDelim);
    }

    try{
      temp = line.substring(0,pos);
      lineIndex = pos;
      return ((Double.valueOf(temp)).doubleValue());
    }
    catch(NumberFormatException nfe){
      return 0; // Return Error number
    }
  }


  /**
   * Convert a DIF character to corresponding string of integers
   * @param temp the DIF character
   * @return the DIF character as number as a string
   */
  private static String convDifChar (int temp)
  {
    int num =0;
    if ((temp >= '@') && (temp <= 'I')){
      num = temp - '@';
      //isSqz = true;
    }
    else if ((temp >= 'a') && (temp <= 'i')){
            num = -(temp - '`');
            //isSqz = true;
    }
    else if ((temp >= 'J') && (temp <= 'R'))
            num = temp - 'I';
    else if ((temp >= 'j') && (temp <= 'r'))
            num = -(temp - 'i');
    else if (temp == '%')
            num = 0;

      return (String.valueOf(num));
  }


  /**
   * Get Y-Value for a DIFed or SQZed data set
   * @param line a line of data
   * @param lineNo the line number
   * @return the y value
   */
  private double getYvalDIF(String line, int lineNo){
    String temp = new String();
    int pos, num;

    num = line.charAt(lineIndex);
    temp = convDifChar(num);
    lineIndex++;
    pos = JSpecViewUtils.findOneOf(line.substring(lineIndex),allDelim);
    if (pos != -1)
    {
            temp += line.substring(lineIndex,lineIndex+pos);
            lineIndex += pos;
    }
    else
    {
            temp += line.substring(lineIndex);
            lineIndex = line.length();
    }
    return ((Double.valueOf(temp)).doubleValue());
  }


  /**
   * Get a duplicate factor
   * @param line a line of the data
   * @param dup_char duplicated character
   * @return the the y value
   */
  private int getDUPVal (String line, int dup_char){
    String temp = new String();
    int ch,pos;

    lineIndex++;
    if ((dup_char >= 'S') && (dup_char <= 'Z'))
    {
            ch = (dup_char - 'R');
            temp = String.valueOf(ch);
    }
    else if (dup_char == 's')
             temp = "9";

    pos = JSpecViewUtils.findOneOf(line.substring(lineIndex),allDelim);

    if (pos != -1){
      temp += line.substring(lineIndex,lineIndex+pos);
      lineIndex += pos;
    }
    else{
      temp += line.substring(lineIndex);
      lineIndex = line.length();
    }

    return ((Integer.valueOf(temp)).intValue());
  }

  /**
   * Get Y-Value for a PACked or FIXed data set
   * @param line a line of data
   * @param lineNo the line number
   * @return the y value
   */
    private double getYvalPAC(String line, int lineNo){
      String temp = new String();
      int pos;
      String PACDelim = "?+- \n";

      if (line.charAt(lineIndex) == '-')
      {
        temp = "-";
        lineIndex++;
      }
      else if (line.charAt(lineIndex) == '+')
        lineIndex++;

      // Check if numbers are written in exponential notation
      int displacement = checkForExp(line.substring(lineIndex) );

      if (displacement != -1)
        pos = JSpecViewUtils.findOneOf(line.substring(displacement), PACDelim);
      else
        pos = JSpecViewUtils.findOneOf(line.substring(lineIndex), PACDelim);

      if (pos != -1){
        temp += line.substring(lineIndex,lineIndex+pos);
        lineIndex += pos;
      }
      else{
        temp += line.substring(lineIndex);
        lineIndex = line.length();
      }
      return ((Double.valueOf(temp)).doubleValue());
    }

    /**
     * Sets the line number for the dataset label in the source
     * @param lineNo the line number
     */
    public void setLabelLineNo(int lineNo){
      labelLineNo = lineNo;
    }

    // Check if numbers are written in exponential notation
    private static int checkForExp(String line) {
      if (line.indexOf("E-") != -1) {
        return line.indexOf("E-") + 2;
      }
      else if (line.indexOf("E+") != -1) {
        return line.indexOf("E+") + 2;
      }
      return -1;
    }

}
