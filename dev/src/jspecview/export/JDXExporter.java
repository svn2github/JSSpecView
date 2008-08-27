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

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import jspecview.common.Coordinate;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSpecViewUtils;
import jspecview.source.JDXSource;

/**
 * class <code>JDXExporter</code> contains static methods for exporting a
 * JCAMP-DX Spectrum in one of the compression formats DIF, FIX, PAC, SQZ or
 * as x, y values.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */

class JDXExporter {

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
   * @param type the type of compression
   * @param spectrum
   * @param startIndex the start Coordinate Index
   * @param endIndex the end Coordinate Index
   * @return the spectrum string for the type of compression specified by <code>type</code>
   */
  private static String toStringAux(int type, JDXSpectrum spectrum,
                                    int startIndex, int endIndex) {
    StringBuffer buffer = new StringBuffer();
    Coordinate[] newXYCoords = spectrum.getXYCoords();
    String tabDataSet = "", tmpDataClass = "XYDATA";
    double xCompFactor, yCompFactor;

    if (spectrum.isHZtoPPM()) {
      Coordinate[] xyCoords = newXYCoords;
      newXYCoords = new Coordinate[xyCoords.length];
      for (int i = 0; i < xyCoords.length; i++)
        newXYCoords[i] = xyCoords[i].copy();
      JSpecViewUtils.applyScale(newXYCoords, spectrum.getObservedFreq(), 1);
    }

    if (type != Exporter.XY) {
      xCompFactor = JSpecViewUtils.getXFactorForCompression(newXYCoords,
          startIndex, endIndex);
      yCompFactor = JSpecViewUtils.getYFactorForCompression(newXYCoords,
          startIndex, endIndex);
    } else {
      xCompFactor = yCompFactor = 1;
      tmpDataClass = "XYPOINTS";
    }

    switch (type) {
    case Exporter.DIF:
      tabDataSet = JDXCompressor.compressDIF(newXYCoords, startIndex, endIndex,
          xCompFactor, yCompFactor);
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
      tabDataSet = JSpecViewUtils.coordinatesToString(newXYCoords, startIndex,
          endIndex, 1);
      break;
    }

    int index = Arrays.binarySearch(JDXSource.VAR_LIST_TABLE[0], tmpDataClass);
    String varList = JDXSource.VAR_LIST_TABLE[1][index];

    buffer.append(spectrum.getHeaderString(tmpDataClass, xCompFactor,
        yCompFactor, startIndex, endIndex));
    buffer
        .append("##" + tmpDataClass + "= " + varList + JSpecViewUtils.newLine);
    buffer.append(tabDataSet);
    buffer.append("##END=");

    return buffer.toString();
  }


}
