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
   * @param xyCoords the array of <code>Coordinate</code>s
   * @param startDataPointIndex the start index of the array of Coordinates to
   *        be compressed
   * @param endDataPointIndex the end index of the array of Coordinates to
   *        be compressed
   * @param xFactor x factor for compression
   * @param yFactor y factor for compression
   * @return A String representing the compressed data
   */
  static String compressDIF(Coordinate[] xyCoords, int startDataPointIndex,
                                   int endDataPointIndex, double xFactor, double yFactor){
    String yStr = "", temp;
    Coordinate curXY;

    int y1, y2, x1;
    StringBuffer buffer = new StringBuffer();

    int i = startDataPointIndex;
    while( i < endDataPointIndex)
    {
      curXY = xyCoords[i];

      // Get first X value on line
      x1 = (int)Math.round(curXY.getXVal()/ xFactor);

      // Get First Y value on line
      y1 = (int)Math.round(curXY.getYVal()/ yFactor);

      temp = String.valueOf(y1);
      // convert 1st digit of string to SQZ
      temp = makeSQZ(temp);
      yStr += temp;

      i++;
      while ((yStr.length() < 60) && i < endDataPointIndex - 1)
      {
        // Print remaining Y values on a line
        curXY = xyCoords[i];
        y2 = (int)Math.round(curXY.getYVal() / yFactor);

        // Calculate DIF value here
        temp = makeDIF(y2, y1);
        yStr += temp;
        y1 = y2;
        i++;
      }

      curXY = xyCoords[i];
      y2 = (int)Math.round(curXY.getYVal() / yFactor);
      // convert last digit of string to SQZ
      temp = makeSQZ(String.valueOf(y2));

      yStr += temp;
      i++;

      buffer.append(x1 + yStr + JSpecViewUtils.newLine);
      yStr = "";
    }


    if(i == endDataPointIndex){
      curXY = xyCoords[i];

      // Get first X value on line
      x1 = (int)Math.round(curXY.getXVal()/ xFactor);

      // Get First Y value on line
      y1 = (int)Math.round(curXY.getYVal()/ yFactor);
      temp = String.valueOf(y1);
      // convert 1st digit of string to SQZ
      temp = makeSQZ(temp);
      yStr += temp;

      buffer.append(x1 + yStr);
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
    String yStr = "", xStr, temp;
    Coordinate curXY;
    String tempYStr;
    String spaces = "          ";

    int y1, y2, x1;
    StringBuffer buffer = new StringBuffer();

    int i = startDataPointIndex;
    while( i <= endDataPointIndex)
    {
      ij = 1;
      curXY = xyCoords[i];

      x1 = (int) Math.round(curXY.getXVal()/xFactor);
      xStr = formatter.format(x1);

      xStr = xStr + spaces.substring(0, (10 - xStr.length()));
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

        temp = spaces.substring(0, (10 - temp.length())) + temp + " ";
        temp += " ";

        yStr += temp;
        ij ++;
        i++;
      }
      buffer.append(xStr + tempYStr + yStr + JSpecViewUtils.newLine);

      yStr = "";
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
    String yStr = "", temp;
    Coordinate curXY;

    int y1, y2, x1;
    StringBuffer buffer = new StringBuffer();

    int i = startDataPointIndex;

    while( i < endDataPointIndex)
    {
      curXY = xyCoords[i];

      // Get first X value on line
      x1 = (int)Math.round(curXY.getXVal()/ xFactor);

      // Get First Y value on line
      y1 = (int)Math.round(curXY.getYVal()/ yFactor);
      temp = String.valueOf(y1);
      // convert 1st digit of string to SQZ
      temp = makeSQZ(temp);
      yStr += temp;

      i++;
      while ((yStr.length() < 60) && i <= endDataPointIndex)
      {
        // Print remaining Y values on a line
        curXY = xyCoords[i];
        y2 = (int)Math.round(curXY.getYVal() / yFactor);
        temp = String.valueOf(y2);
        // Calculate DIF value here
        temp = makeSQZ(temp);
        yStr += temp;
        i++;
      }
      buffer.append(x1 + yStr + JSpecViewUtils.newLine);
      yStr = "";

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
    DecimalFormat formatter = new DecimalFormat("#", new DecimalFormatSymbols(java.util.Locale.US ));
    int ij;
    String yStr = "", temp;
    Coordinate curXY;

    int y1, y2, x1;
    StringBuffer buffer = new StringBuffer();

    int i = startDataPointIndex;
    while( i <= endDataPointIndex)
    {
      ij = 1;
      curXY = xyCoords[i];

      // Get first X value on line
      x1 = (int) Math.round(curXY.getXVal()/xFactor);

      // Get First Y value on line
      y1 = (int) Math.round(curXY.getYVal()/yFactor);

      i++;
      while ((ij <= 5) && i <= endDataPointIndex)
      {
        // Print remaining Y values on a line
        curXY = xyCoords[i];
        y2 = (int) Math.round(curXY.getYVal()/yFactor);
        temp = formatter.format(y2)+" ";
        yStr += temp;
        ij ++;
        i++;
      }
      buffer.append(formatter.format(x1) + " " + formatter.format(y1) + " " + yStr + JSpecViewUtils.newLine);

      yStr = "";
    }

    return buffer.toString();
  }

  /**
   * Makes a SQZ Character
   * @param yStr the input number as a string
   * @return the SQZ character
   */
  private static String makeSQZ(String yStr){
    boolean negative = false;

    yStr.trim();
    if (yStr.charAt(0) == '-'){
      negative = true;
      yStr = yStr.substring(1);
    }

    char[] yStrArray = yStr.toCharArray();

    switch (yStr.charAt(0)){
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
}
