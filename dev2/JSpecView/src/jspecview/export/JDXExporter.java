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

package jspecview.export;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import jspecview.common.Coordinate;
import jspecview.common.JDXSpectrum;
import jspecview.source.JDXFileReader;
import jspecview.util.TextFormat;

/**
 * class <code>JDXExporter</code> contains static methods for exporting a
 * JCAMP-DX Spectrum in one of the compression formats DIF, FIX, PAC, SQZ or
 * as x, y values.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 */

class JDXExporter {

  /**
   * The factor divisor used in compressing spectral data in one of DIF, SQZ,
   * PAC and FIX formats
   */
  private static final double FACTOR_DIVISOR = 1000000;

  /**
   * Exports spectrum in X,Y format
   * @param type
   * @param path
   * @param spectrum the spectrum
   * @param startIndex
   * @param endIndex
   * @return data if path is null
   * @throws IOException
   */
  static String export(int type, String path, JDXSpectrum spectrum, int startIndex, int endIndex) throws IOException{
    String data = toStringAux(type, spectrum, startIndex, endIndex);
    if (path == null)
      return data;
    FileWriter writer = new FileWriter(path);
    writer.write(data);
    writer.close();
    return " (" + data.length() + " bytes)";
  }

  /**
   * Auxiliary function for the toString functions
   * 
   * @param type
   *        the type of compression
   * @param spectrum
   * @param startIndex
   *        the start Coordinate Index
   * @param endIndex
   *        the end Coordinate Index
   * @return the spectrum string for the type of compression specified by
   *         <code>type</code>
   */
  private static String toStringAux(int type, JDXSpectrum spectrum,
                                    int startIndex, int endIndex) {

    //String dataType = spectrum.getDataType();
    StringBuffer buffer = new StringBuffer();
    Coordinate[] newXYCoords = spectrum.getXYCoords();
    String tabDataSet = "", tmpDataClass = "XYDATA";

    if (spectrum.isHZtoPPM()) {
      // convert back to Hz.
      Coordinate[] xyCoords = newXYCoords;
      newXYCoords = new Coordinate[xyCoords.length];
      for (int i = 0; i < xyCoords.length; i++)
        newXYCoords[i] = xyCoords[i].copy();
      Coordinate.applyScale(newXYCoords, spectrum.getObservedFreq(), 1);
    }

    double xCompFactor = spectrum.getXFactor();
    boolean isIntegerX = areIntegers(newXYCoords, startIndex, endIndex, 1.0, true);
    if (!isIntegerX && !areIntegers(newXYCoords, startIndex, endIndex, xCompFactor, true))
      xCompFactor = 1;
    
    double minY = Coordinate.getMinY(newXYCoords, startIndex, endIndex);
    double maxY = Coordinate.getMaxY(newXYCoords, startIndex, endIndex);
    double yCompFactor = spectrum.getYFactor();

    switch (type) {
    case Exporter.XY:
      yCompFactor = 1;
      tmpDataClass = (spectrum.isContinuous() ?  "XYDATA" : "XYPOINTS");
      break;
    case Exporter.PAC:
      yCompFactor = 1;
      break;
    default:
      boolean isIntegerY = areIntegers(newXYCoords, startIndex, endIndex, 1.0, false);
      if (!isIntegerY && !areIntegers(newXYCoords, startIndex, endIndex, yCompFactor, false)) {
        yCompFactor = (maxY - minY) / FACTOR_DIVISOR;
      }
      break;
    }
    int step = 1;
    if (spectrum.isExportXAxisLeftToRight() != (spectrum.getFirstX() < spectrum.getLastX())) {
      int t = startIndex;
      startIndex = endIndex;
      endIndex = t;
      step = -1;
    }
    switch (type) {
    case Exporter.DIF:
    case Exporter.DIFDUP:
      tabDataSet = JDXCompressor.compressDIF(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor, type == Exporter.DIFDUP);
      break;
    case Exporter.FIX:
      tabDataSet = JDXCompressor.compressFIX(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor);
      break;
    case Exporter.PAC:
      tabDataSet = JDXCompressor.compressPAC(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor);
      break;
    case Exporter.SQZ:
      tabDataSet = JDXCompressor.compressSQZ(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor);
      break;
    case Exporter.XY:
      tabDataSet = JDXCompressor.getXYList(newXYCoords, startIndex, endIndex, step);
      break;
    }

    int index = Arrays.binarySearch(JDXFileReader.VAR_LIST_TABLE[0],
        tmpDataClass);
    String varList = JDXFileReader.VAR_LIST_TABLE[1][index];
    buffer.append(spectrum.getHeaderString(tmpDataClass, minY, maxY,
        xCompFactor, yCompFactor, startIndex, endIndex));
    buffer.append("##" + tmpDataClass + "= " + varList + TextFormat.newLine);
    buffer.append(tabDataSet);
    buffer.append("##END=");

    return buffer.toString();
  }

  private static boolean areIntegers(Coordinate[] xyCoords, int startIndex,
                                     int endIndex, double factor, boolean isX) {
    for (int i = startIndex; i <= endIndex; i++) {
      double x = (isX ? xyCoords[i].getXVal() : xyCoords[i].getYVal()) / factor;
      if (TextFormat.isAlmostInteger(x))
          return false;
    }
    return true;
  }
  
}
