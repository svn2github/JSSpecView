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

package jspecview.export;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import jspecview.common.Coordinate;
import jspecview.common.JSpecViewUtils;


/**
 * <code>JDXCompressor</code> takes an array of <code>Coordinates<code> and
 * compresses them into one of the JCAMP-DX compression formats: DIF, FIX, PAC
 * and SQZ.
 * @author Christopher Muir
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.common.Coordinate
 * @see jspecview.source.JDXDecompressor
 */

class JDXCompressor {

  /**
   * Compresses the <code>Coordinate<code>s into DIF format
   * 
   * @param xyCoords
   *        the array of <code>Coordinate</code>s
   * @param startDataPointIndex
   *        the start index of the array of Coordinates to be compressed
   * @param endDataPointIndex
   *        the end index of the array of Coordinates to be compressed
   * @param xFactor
   *        x factor for compression
   * @param yFactor
   *        y factor for compression
   * @param isDIFDUP
   * @return A String representing the compressed data
   */
  static String compressDIF(Coordinate[] xyCoords, int startDataPointIndex,
                            int endDataPointIndex, double xFactor,
                            double yFactor, boolean isDIFDUP) {
    String temp;
    StringBuffer yStr = new StringBuffer();
    Coordinate curXY;

    int y1, y2;
    double x1;
    StringBuffer buffer = new StringBuffer();

    int i = startDataPointIndex;
    while (i < endDataPointIndex) {
      curXY = xyCoords[i];

      // Get first X value on line
      x1 = curXY.getXVal() / xFactor;//(int) Math.round(curXY.getXVal() / xFactor);

      // Get First Y value on line
      y1 = (int) Math.round(curXY.getYVal() / yFactor);

      temp = String.valueOf(y1);
      // convert 1st digit of string to SQZ
      temp = makeSQZ(temp);
      yStr.append(temp);
      String lastDif = "";
      int nDif = 0;
      i++;
      while (yStr.length() < 60 && i < endDataPointIndex - 1) {
        // Print remaining Y values on a line
        curXY = xyCoords[i];
        y2 = (int) Math.round(curXY.getYVal() / yFactor);

        // Calculate DIF value here
        temp = makeDIF(y2, y1);
        if (isDIFDUP && temp.equals(lastDif)) {
          nDif++;
        } else {
          lastDif = temp;
          if (nDif > 0) {
            yStr.append(makeDUP(String.valueOf(nDif + 1)));
            nDif = 0;
          }
          yStr.append(temp);
        }
        y1 = y2;
        i++;
      }
      if (nDif > 0)
        yStr.append(makeDUP(String.valueOf(nDif + 1)));
      curXY = xyCoords[i];
      y2 = (int) Math.round(curXY.getYVal() / yFactor);
      // convert last digit of string to SQZ
      temp = makeSQZ(String.valueOf(y2));

      yStr.append(temp);
      i++;

      buffer.append(fixExponent(x1)).append(yStr).append(JSpecViewUtils.newLine);
      yStr.setLength(0);
    }

    if (i == endDataPointIndex) {
      curXY = xyCoords[i];

      // Get first X value on line
      x1 = curXY.getXVal() / xFactor;

      // Get First Y value on line
      y1 = (int) Math.round(curXY.getYVal() / yFactor);
      temp = String.valueOf(y1);
      // convert 1st digit of string to SQZ
      temp = makeSQZ(temp);
      buffer.append(fixExponent(x1)).append(yStr).append(temp);
      buffer.append("  $$checkpoint" + JSpecViewUtils.newLine);
    }

    return buffer.toString();
  }

  /**
   * Compresses the <code>Coordinate<code>s into FIX format
   * @param xyCoords the array of <code>Coordinate</code>s
   * @param startDataPointIndex startDataPointIndex the start index of the array of Coordinates to
   *        be compressed
   * @param endDataPointIndex endDataPointIndex the end index of the array of Coordinates to
   *        be compressed
   * @param xFactor x factor for compression
   * @param yFactor y factor for compression
   * @return A String representing the compressed data
   */
  static String compressFIX(Coordinate[] xyCoords, int startDataPointIndex, int endDataPointIndex, double xFactor, double yFactor){
    DecimalFormat formatter = new DecimalFormat("#", new DecimalFormatSymbols(java.util.Locale.US ));
    int ij;
    StringBuffer yStr = new StringBuffer();
    String xStr, temp;
    Coordinate curXY;
    String tempYStr;
    String spaces = "                    ";

    int y1, y2;
    double x1;
    StringBuffer buffer = new StringBuffer();

    int i = startDataPointIndex;
    while( i <= endDataPointIndex)
    {
      ij = 1;
      curXY = xyCoords[i];

      x1 = curXY.getXVal()/xFactor;
      xStr = fixExponent(x1);

      if (xStr.length() < 20)
        xStr = spaces.substring(0, (20 - xStr.length()));
      xStr += " ";

      // Get First Y value on line
      y1 = (int) Math.round(curXY.getYVal()/yFactor);
      tempYStr = formatter.format(y1);


      tempYStr = spaces.substring(0, (10 - tempYStr.length())) + tempYStr + " ";
      tempYStr += " ";

      i++;
      while ((ij <= 5) && i <= endDataPointIndex)
      {
        // Print remaining Y values on a line
        curXY = xyCoords[i];
        y2 = (int) Math.round(curXY.getYVal()/yFactor);
        temp = formatter.format(y2);
        yStr.append(spaces.substring(0, (10 - temp.length())))
        .append(temp).append(" ");
        ij++;
        i++;
      }
      buffer.append(xStr).append(tempYStr).append(yStr).append(JSpecViewUtils.newLine);
      yStr.setLength(0);
    }

    return buffer.toString();
  }

  /**
   * Compresses the <code>Coordinate<code>s into SQZ format
   * @param xyCoords the array of <code>Coordinate</code>s
   * @param startDataPointIndex startDataPointIndex the start index of the array of Coordinates to
   *        be compressed
   * @param endDataPointIndex endDataPointIndex the end index of the array of Coordinates to
   *        be compressed
   * @param xFactor x factor for compression
   * @param yFactor y factor for compression
   * @return A String representing the compressed data
   */
  static String compressSQZ(Coordinate[] xyCoords, int startDataPointIndex, int endDataPointIndex, double xFactor, double yFactor){
    StringBuffer yStr = new StringBuffer();
    String temp;
    Coordinate curXY;

    int y1, y2;
    double x1;
    StringBuffer buffer = new StringBuffer();

    int i = startDataPointIndex;

    while( i < endDataPointIndex)
    {
      curXY = xyCoords[i];

      // Get first X value on line
      x1 = curXY.getXVal()/ xFactor;

      // Get First Y value on line
      y1 = (int)Math.round(curXY.getYVal()/ yFactor);
      temp = String.valueOf(y1);
      // convert 1st digit of string to SQZ
      temp = makeSQZ(temp);
      yStr.append(temp);

      i++;
      while ((yStr.length() < 60) && i <= endDataPointIndex)
      {
        // Print remaining Y values on a line
        curXY = xyCoords[i];
        y2 = (int)Math.round(curXY.getYVal() / yFactor);
        temp = String.valueOf(y2);
        // Calculate DIF value here
        temp = makeSQZ(temp);
        yStr.append(temp);
        i++;
      }
      buffer.append(fixExponent(x1)).append(yStr).append(JSpecViewUtils.newLine);
      yStr.setLength(0);

    }

    return buffer.toString();
  }

  /**
   * Compresses the <code>Coordinate<code>s into PAC format
   * @param xyCoords the array of <code>Coordinate</code>s
   * @param startDataPointIndex startDataPointIndex the start index of the array of Coordinates to
   *        be compressed
   * @param endDataPointIndex endDataPointIndex the end index of the array of Coordinates to
   *        be compressed
   * @param xFactor x factor for compression
   * @param yFactor y factor for compression
   * @return A String representing the compressed data
   */
  static String compressPAC(Coordinate[] xyCoords, int startDataPointIndex, int endDataPointIndex, double xFactor, double yFactor){
    int ij;
    StringBuffer yStr = new StringBuffer();
    String temp;
    Coordinate curXY;

    double x1, y1, y2;
    StringBuffer buffer = new StringBuffer();

    int i = startDataPointIndex;
    while( i <= endDataPointIndex)
    {
      ij = 1;
      curXY = xyCoords[i];

      // Get first X value on line
      x1 = curXY.getXVal() / xFactor;

      // Get First Y value on line
      y1 = curXY.getYVal() / yFactor ;

      i++;
      while ((ij <= 5) && i <= endDataPointIndex)
      {
        // Print remaining Y values on a line
        curXY = xyCoords[i];
        y2 = curXY.getYVal() / yFactor;
        temp = y2 +" ";
        yStr.append(temp);
        ij ++;
        i++;
      }
      buffer.append(fixExponent(x1))
          .append(" ").append(fixExponent(y1)).append(" ")
          .append(yStr).append(JSpecViewUtils.newLine);

      yStr.setLength(0);
    }

    return buffer.toString();
  }

  /**
   * Makes a SQZ Character
   * @param sNum the input number as a string
   * @return the SQZ character
   */
  private static String makeSQZ(String sNum){
    boolean negative = false;

    sNum.trim();
    if (sNum.charAt(0) == '-'){
      negative = true;
      sNum = sNum.substring(1);
    }

    char[] yStrArray = sNum.toCharArray();

    switch (sNum.charAt(0)){
      case '0' : yStrArray[0] = '@';break;
      case '1' : if (negative) yStrArray[0] = 'a';else yStrArray[0] = 'A';break;
      case '2' : if (negative) yStrArray[0] = 'b';else yStrArray[0] = 'B';break;
      case '3' : if (negative) yStrArray[0] = 'c';else yStrArray[0] = 'C';break;
      case '4' : if (negative) yStrArray[0] = 'd';else yStrArray[0] = 'D';break;
      case '5' : if (negative) yStrArray[0] = 'e';else yStrArray[0] = 'E';break;
      case '6' : if (negative) yStrArray[0] = 'f';else yStrArray[0] = 'F';break;
      case '7' : if (negative) yStrArray[0] = 'g';else yStrArray[0] = 'G';break;
      case '8' : if (negative) yStrArray[0] = 'h';else yStrArray[0] = 'H';break;
      case '9' : if (negative) yStrArray[0] = 'i';else yStrArray[0] = 'I';break;
    }
    return (new String(yStrArray));
  }

    /**
   * Makes a DUP Character
   * @param sNum the input number as a string
   * @return the DUP character
   */
  private static String makeDUP(String sNum){
    char[] yStrArray = sNum.toCharArray();
    yStrArray[0] = "STUVWXYZs".charAt(yStrArray[0] - '1');
    return (new String(yStrArray));
  }

  /**
   * Makes a DIF Character
   * @param y1 the first y value
   * @param y2 the second y value
   * @return the DIF Character
   */
  private static String makeDIF(int y1, int y2){
    boolean negative = false;
    String yStr;

    int dif = y1 - y2;
    yStr = String.valueOf(dif);
    yStr.trim();
    if (yStr.charAt(0) == '-'){
      negative = true;
      yStr = yStr.substring(1);
    }

    char[] yStrArray = yStr.toCharArray();
    switch (yStr.charAt(0))
    {
      case '0' : yStrArray[0] = '%';break;
      case '1' : if (negative) yStrArray[0] = 'j';else yStrArray[0] = 'J';break;
      case '2' : if (negative) yStrArray[0] = 'k';else yStrArray[0] = 'K';break;
      case '3' : if (negative) yStrArray[0] = 'l';else yStrArray[0] = 'L';break;
      case '4' : if (negative) yStrArray[0] = 'm';else yStrArray[0] = 'M';break;
      case '5' : if (negative) yStrArray[0] = 'n';else yStrArray[0] = 'N';break;
      case '6' : if (negative) yStrArray[0] = 'o';else yStrArray[0] = 'O';break;
      case '7' : if (negative) yStrArray[0] = 'p';else yStrArray[0] = 'P';break;
      case '8' : if (negative) yStrArray[0] = 'q';else yStrArray[0] = 'Q';break;
      case '9' : if (negative) yStrArray[0] = 'r';else yStrArray[0] = 'R';break;
    }
    return (new String(yStrArray));
  }

  private static String fixExponent(double x) {
    // JCAMP-DX requires 1.5E[+|-]nn or 1.5E[+|-]nnn only
    // not Java's 1.5E3 or 1.5E-2
    String s = "" + x;
    int pt = s.indexOf("E");
    if (pt < 0)
      return s;
    switch (s.length() - pt) {
    case 2:
      s = s.substring(0, pt + 1) + "0" + s.substring(pt + 1);
      break;
    case 3:
      // 4.3E-3
      if (s.charAt(pt + 1) == '-')
        s = s.substring(0, pt + 2) + "0" + s.substring(pt + 2);
      break;
    } 
    if (s.indexOf("E-") < 0)
      s = s.substring(0, pt + 1) + "+" + s.substring(pt + 1);
    return s;
  }  
}
