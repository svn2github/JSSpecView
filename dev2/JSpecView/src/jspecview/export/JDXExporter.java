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
    return null;
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
    double xCompFactor = 1, yCompFactor = 1;

    if (spectrum.isHZtoPPM()) {
      Coordinate[] xyCoords = newXYCoords;
      newXYCoords = new Coordinate[xyCoords.length];
      for (int i = 0; i < xyCoords.length; i++)
        newXYCoords[i] = xyCoords[i].copy();
      Coordinate.applyScale(newXYCoords, spectrum.getObservedFreq(), 1);
    }

    if (type != Exporter.XY) {
      //xCompFactor = JSpecViewUtils.getXFactorForCompression(newXYCoords,
        //  startIndex, endIndex);
      if (type != Exporter.PAC)
        yCompFactor = getYFactorForCompression(newXYCoords,
          startIndex, endIndex, FACTOR_DIVISOR);
    } else {
      if (spectrum.isContinuous())
        tmpDataClass = "XYDATA";
      else
        tmpDataClass = "XYPOINTS";
    }

    switch (type) {
    case Exporter.DIF:
    case Exporter.DIFDUP:
      tabDataSet = JDXCompressor.compressDIF(newXYCoords, startIndex, endIndex,
          xCompFactor, yCompFactor, type == Exporter.DIFDUP);
      break;
    case Exporter.FIX:
      tabDataSet = JDXCompressor.compressFIX(newXYCoords, startIndex, endIndex,
          xCompFactor, yCompFactor);
      break;
    case Exporter.PAC:
      tabDataSet = JDXCompressor.compressPAC(newXYCoords, startIndex, endIndex,
          xCompFactor, yCompFactor);
      break;
    case Exporter.SQZ:
      tabDataSet = JDXCompressor.compressSQZ(newXYCoords, startIndex, endIndex,
          xCompFactor, yCompFactor);
      break;
    case Exporter.XY:
      tabDataSet = Coordinate.coordinatesToString(newXYCoords, startIndex,
          endIndex, 1);
      break;
    }

    int index = Arrays.binarySearch(JDXFileReader.VAR_LIST_TABLE[0],
        tmpDataClass);
    String varList = JDXFileReader.VAR_LIST_TABLE[1][index];

    buffer.append(spectrum.getHeaderString(tmpDataClass, xCompFactor,
        yCompFactor, startIndex, endIndex));
    buffer
        .append("##" + tmpDataClass + "= " + varList + TextFormat.newLine);
    buffer.append(tabDataSet);
    buffer.append("##END=");

    return buffer.toString();
  }

//  /**
//   * Returns the X Compression factor by finding the subtracting the min and max
//   * x values and dividing by the factor divisor
//   * 
//   * @param xyCoords
//   *        an array of coordinates
//   * @param startDataPointIndex
//   *        the start index
//   * @param endDataPointIndex
//   *        the end index
//   * @param factorDivisor
//   *        the factor divisor
//   * @return the X Compression factor
//   */
//  private static double getXFactorForCompression(Coordinate[] xyCoords,
//                                                int startDataPointIndex,
//                                                int endDataPointIndex,
//                                                double factorDivisor) {
//  
//    double maxX = Coordinate.getMaxX(xyCoords, startDataPointIndex,
//        endDataPointIndex);
//    double minX = Coordinate.getMinX(xyCoords, startDataPointIndex,
//        endDataPointIndex);
//  
//    return (maxX - minX) / factorDivisor;
//  }
  

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
   * @param factorDivisor 
   * @return the Y Compression factor
   */
  private static double getYFactorForCompression(Coordinate[] xyCoords,
                                                int startDataPointIndex,
                                                int endDataPointIndex, double factorDivisor) {
    int i = 0;
    double y;
    for (; i < xyCoords.length; i++) 
      if ((y = xyCoords[i].getYVal()) != Math.floor(y))
          break;
    if (i == xyCoords.length)
      return 1;
    double maxY = Coordinate.getMaxY(xyCoords, startDataPointIndex,
        endDataPointIndex);
    double minY = Coordinate.getMinY(xyCoords, startDataPointIndex,
        endDataPointIndex);
  
    return (maxY - minY) / factorDivisor;
  }



}
