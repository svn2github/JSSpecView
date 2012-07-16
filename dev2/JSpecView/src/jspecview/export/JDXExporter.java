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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


import jspecview.common.Coordinate;
import jspecview.common.JDXDataObject;
import jspecview.common.JDXSpectrum;
import jspecview.source.FileReader;
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

public class JDXExporter {

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
  static String export(Exporter.Type type, String path, JDXSpectrum spectrum, int startIndex, int endIndex) throws IOException{
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
  private static String toStringAux(Exporter.Type type, JDXSpectrum spectrum,
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
    case XY:
      yCompFactor = 1;
      tmpDataClass = (spectrum.isContinuous() ?  "XYDATA" : "XYPOINTS");
      break;
    case PAC:
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
    case DIF:
    case DIFDUP:
      tabDataSet = JDXCompressor.compressDIF(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor, type == Exporter.Type.DIFDUP);
      break;
    case FIX:
      tabDataSet = JDXCompressor.compressFIX(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor);
      break;
    case PAC:
      tabDataSet = JDXCompressor.compressPAC(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor);
      break;
    case SQZ:
      tabDataSet = JDXCompressor.compressSQZ(newXYCoords, startIndex, endIndex, step, 
          xCompFactor, yCompFactor);
      break;
    case XY:
      tabDataSet = JDXCompressor.getXYList(newXYCoords, startIndex, endIndex, step);
      break;
		case AML:
			break;
		case CML:
			break;
		case JPG:
			break;
		case PNG:
			break;
		case SOURCE:
			break;
		case SVG:
			break;
		case SVGI:
			break;
		case UNK:
			break;
    }

    int index = Arrays.binarySearch(FileReader.VAR_LIST_TABLE[0],
        tmpDataClass);
    String varList = FileReader.VAR_LIST_TABLE[1][index];
    buffer.append(getHeaderString(spectrum, tmpDataClass, minY, maxY,
        xCompFactor, yCompFactor, startIndex, endIndex));
    buffer.append("##" + tmpDataClass + "= " + varList + TextFormat.newLine);
    buffer.append(tabDataSet);
    buffer.append("##END=");

    return buffer.toString();
  }

  /**
   * Returns the String for the header of the spectrum
   * @param spec 
   * 
   * @param tmpDataClass
   *        the dataclass
   * @param minY 
   * @param maxY 
   * @param tmpXFactor
   *        the x factor
   * @param tmpYFactor
   *        the y factor
   * @param startIndex
   *        the index of the starting coordinate
   * @param endIndex
   *        the index of the ending coordinate
   * @return the String for the header of the spectrum
   */
  private static String getHeaderString(JDXSpectrum spec, String tmpDataClass,
                                        double minY, double maxY,
                                        double tmpXFactor, double tmpYFactor,
                                        int startIndex, int endIndex) {

    //final String CORE_STR = "TITLE,ORIGIN,OWNER,DATE,TIME,DATATYPE,JCAMPDX";

    StringBuffer buffer = new StringBuffer();
    // start of header
    buffer.append("##TITLE= ").append(spec.getTitle()).append(
        TextFormat.newLine);
    buffer.append("##JCAMP-DX= 5.01").append(TextFormat.newLine); /*+ getJcampdx()*/
    buffer.append("##DATA TYPE= ").append(spec.getDataType()).append(
        TextFormat.newLine);
    buffer.append("##DATA CLASS= ").append(tmpDataClass).append(
        TextFormat.newLine);
    buffer.append("##ORIGIN= ").append(spec.getOrigin()).append(
        TextFormat.newLine);
    buffer.append("##OWNER= ").append(spec.getOwner()).append(
        TextFormat.newLine);
    String d = spec.getDate();
    String longdate = "";
    String currentTime = (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSS ZZZZ"))
        .format(Calendar.getInstance().getTime());
    if (spec.getLongDate().equals("") || d.length() != 8) {
      longdate = currentTime + " $$ export date from JSpecView";
    } else if (d.length() == 8) { // give a 50 year window; Y2K compliant
      longdate = (d.charAt(0) < '5' ? "20" : "19") + d + " " + spec.getTime();
    } else {
      longdate = spec.getLongDate();
    }
    buffer.append("##LONGDATE= ").append(longdate).append(TextFormat.newLine);

    // optional header
    List<String[]> headerTable = spec.getHeaderTable();
    for (int i = 0; i < headerTable.size(); i++) {
      String[] entry = headerTable.get(i);
      String label = entry[0];
      String dataSet = entry[1];
      String nl = (dataSet.startsWith("<") && dataSet.contains("</") ? TextFormat.newLine
          : "");
      buffer.append(label).append("= ").append(nl).append(dataSet).append(
          TextFormat.newLine);
    }
    double observedFreq = spec.getObservedFreq();
    if (!spec.is1D())
      buffer.append("##NUM DIM= ").append(spec.numDim).append(
          TextFormat.newLine);
    if (observedFreq != JDXDataObject.ERROR)
      buffer.append("##.OBSERVE FREQUENCY= ").append(observedFreq).append(
          TextFormat.newLine);
    if (spec.observedNucl != "")
      buffer.append("##.OBSERVE NUCLEUS= ").append(spec.observedNucl).append(
          TextFormat.newLine);
    //now need to put pathlength here

    // last part of header

    //boolean toHz = (observedFreq != JDXSpectrum.ERROR && !spec.getDataType()
      //  .toUpperCase().contains("FID"));
    buffer.append("##XUNITS= ").append(spec.isHZtoPPM() ? "HZ" : spec.getXUnits()).append(
        TextFormat.newLine);
    buffer.append("##YUNITS= ").append(spec.getYUnits()).append(
        TextFormat.newLine);
    buffer.append("##XFACTOR= ").append(TextFormat.fixExponentInt(tmpXFactor))
        .append(TextFormat.newLine);
    buffer.append("##YFACTOR= ").append(TextFormat.fixExponentInt(tmpYFactor))
        .append(TextFormat.newLine);
    double f = (spec.isHZtoPPM() ? observedFreq : 1);
    Coordinate[] xyCoords = spec.getXYCoords();
    buffer.append("##FIRSTX= ").append(
        TextFormat.fixExponentInt(xyCoords[startIndex].getXVal() * f)).append(
        TextFormat.newLine);
    buffer.append("##FIRSTY= ").append(
        TextFormat.fixExponentInt(xyCoords[startIndex].getYVal())).append(
        TextFormat.newLine);
    buffer.append("##LASTX= ").append(
        TextFormat.fixExponentInt(xyCoords[endIndex].getXVal() * f)).append(
        TextFormat.newLine);
    buffer.append("##NPOINTS= ").append((Math.abs(endIndex - startIndex) + 1))
        .append(TextFormat.newLine);
    buffer.append("##MINY= ").append(TextFormat.fixExponentInt(minY)).append(
        TextFormat.newLine);
    buffer.append("##MAXY= ").append(TextFormat.fixExponentInt(maxY)).append(
        TextFormat.newLine);
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
