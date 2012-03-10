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

import jspecview.common.Coordinate;
import jspecview.util.TextFormat;


/**
 * <code>JDXCompressor</code> takes an array of <code>Coordinates<code> and
 * compresses them into one of the JCAMP-DX compression formats: DIF, FIX, PAC
 * and SQZ.
 * @author Christopher Muir
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
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
    StringBuffer yStr = new StringBuffer();

    StringBuffer buffer = new StringBuffer();
    for (int i = startDataPointIndex; i < endDataPointIndex; i++) {
      buffer.append(TextFormat.fixIntNoExponent(xyCoords[i].getXVal() / xFactor));
      yStr.setLength(0);
      long y1 = (long) Math.round(xyCoords[i].getYVal() / yFactor);
      yStr.append(makeSQZ(y1));
      String lastDif = "";
      int nDif = 0;
      while (++i < endDataPointIndex - 1 && yStr.length() < 50) {
        // Print remaining Y values on a line
        long y2 = (long) Math.round(xyCoords[i].getYVal() / yFactor);
        // Calculate DIF value here
        String temp = makeDIF(y2 - y1);
        if (isDIFDUP && temp.equals(lastDif)) {
          nDif++;
        } else {
          lastDif = temp;
          if (nDif > 0) {
            yStr.append(makeDUP(nDif + 1));
            nDif = 0;
          }
          yStr.append(temp);
        }
        y1 = y2;
      }
      if (nDif > 0)
        yStr.append(makeDUP(nDif));
      // convert last digit of string to SQZ
      yStr.append(makeSQZ(xyCoords[i], yFactor));
      buffer.append(yStr).append(TextFormat.newLine);
    }
    // Get checksum line -- for an X-sequence check only
    buffer.append(TextFormat.fixExponentInt(xyCoords[endDataPointIndex].getXVal() / xFactor))
        .append(makeSQZ(xyCoords[endDataPointIndex], yFactor));
    buffer.append("  $$checkpoint").append(TextFormat.newLine);
    return buffer.toString();
  }

  final static String spaces = "                    ";


  /**
   * Compresses the <code>Coordinate<code>s into FIX format
   * 
   * @param xyCoords
   *        the array of <code>Coordinate</code>s
   * @param startDataPointIndex
   *        startDataPointIndex the start index of the array of Coordinates to
   *        be compressed
   * @param endDataPointIndex
   *        endDataPointIndex the end index of the array of Coordinates to be
   *        compressed
   * @param xFactor
   *        x factor for compression
   * @param yFactor
   *        y factor for compression
   * @return A String representing the compressed data
   */
  static String compressFIX(Coordinate[] xyCoords, int startDataPointIndex,
                            int endDataPointIndex, double xFactor,
                            double yFactor) {
    DecimalFormat formatter = TextFormat.getDecimalFormat("#");
    StringBuffer buffer = new StringBuffer();

    for (int i = startDataPointIndex; i <= endDataPointIndex; i++) {
      String xStr = TextFormat.fixIntNoExponent(xyCoords[i].getXVal( ) / xFactor);
      if (xStr.length() < 20)
        xStr += spaces.substring(0, (14 - xStr.length()));
      buffer.append(xStr).append(" ");
      format10(buffer, (long) Math.round(xyCoords[i].getYVal() / yFactor), formatter);
      for (int j = 0; j < 5 && ++i <= endDataPointIndex; j++)
        format10(buffer, (long) Math.round(xyCoords[i].getYVal() / yFactor), formatter);
      buffer.append(TextFormat.newLine);
    }

    return buffer.toString();
  }

  private static void format10(StringBuffer buffer, long y, DecimalFormat formatter) {
    String s = formatter.format(y);
    buffer.append(spaces.substring(0, (10 - s.length()))).append(s).append(" ");
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
    StringBuffer buffer = new StringBuffer();
    for (int i = startDataPointIndex; i < endDataPointIndex; i++) {
      buffer.append(TextFormat.fixIntNoExponent(xyCoords[i].getXVal()/ xFactor));
      yStr.setLength(0);
      yStr.append(makeSQZ(xyCoords[i], yFactor));
      while ((yStr.length() < 60) && i <= endDataPointIndex)
        yStr.append(makeSQZ(xyCoords[++i], yFactor));
      buffer.append(yStr).append(TextFormat.newLine);
    }
    return buffer.toString();
  }

  /**
   * Compresses the <code>Coordinate<code>s into PAC format
   * 
   * @param xyCoords
   *        the array of <code>Coordinate</code>s
   * @param startDataPointIndex
   *        startDataPointIndex the start index of the array of Coordinates to
   *        be compressed
   * @param endDataPointIndex
   *        endDataPointIndex the end index of the array of Coordinates to be
   *        compressed
   * @param xFactor
   *        x factor for compression
   * @param yFactor
   *        y factor for compression
   * @return A String representing the compressed data
   */
  static String compressPAC(Coordinate[] xyCoords, int startDataPointIndex,
                            int endDataPointIndex, double xFactor,
                            double yFactor) {
    StringBuffer buffer = new StringBuffer();
    for (int i = startDataPointIndex; i <= endDataPointIndex; i++) {
      buffer.append(TextFormat.fixIntNoExponent(xyCoords[i].getXVal() / xFactor))
      .append(fixPacY(xyCoords[i].getYVal() / yFactor));
      for (int j = 0; j < 4 && ++i <= endDataPointIndex; j++) {
        // Print remaining Y values on a line
        buffer.append(fixPacY(xyCoords[i].getYVal() / yFactor));
      }
      buffer.append(TextFormat.newLine);
    }
    return buffer.toString();
  }

  private static String fixPacY(double y) {
    return (y < 0 ? "" : " ") + TextFormat.fixIntNoExponent(y);
  }

  /**
   * Makes a SQZ Character for a y value
   * 
   * @param pt the input point
   * @param yFactor
   * @return the SQZ character
   */
  private static String makeSQZ(Coordinate pt, double yFactor) {
    return makeSQZ((long) Math.round(pt.getYVal() / yFactor));
  }
    
  /**
   * Makes a SQZ Character
   * 
   * @param y the input number
   * @return the SQZ character
   */
  private static String makeSQZ(long y) {
    return compress(y, "@ABCDEFGHI", "abcdefghi");
  }

  /**
   * Makes a DIF Character
   * 
   * @param y2
   *        the second y value
   * @param y1
   *        the first y value
   * @return the DIF Character
   */
  private static String makeDIF(long dy) {
    return compress(dy, "%JKLMNOPQR", "jklmnopqr");
  }

  /**
   * Makes a DUP Character
   * @param sNum the input number as a string
   * @return the DUP character
   */
  private static String makeDUP(long y){
    return compress(y, "0STUVWXYZs", "");
  }

  /**
   * replace first character and "-" sign with a letter
   * 
   * @param y
   * @param strPos
   * @param strNeg
   * @return
   */
  private static String compress(long y, String strPos, String strNeg) {
    boolean negative = false;
    String yStr = String.valueOf(y);
    char ch = yStr.charAt(0);
    if (ch == '-') {
      negative = true;
      yStr = yStr.substring(1);
      ch = yStr.charAt(0);
    }
    char[] yStrArray = yStr.toCharArray();
    yStrArray[0] = (negative ? strNeg.charAt(ch - '1') 
        : strPos.charAt(ch - '0'));
    return new String(yStrArray);
  }  
}
