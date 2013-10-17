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

import javajs.lang.SB;
import javajs.util.List;




import jspecview.common.Coordinate;
import jspecview.common.ExportType;
import jspecview.source.FileReader;
import jspecview.source.JDXDataObject;
import jspecview.source.JDXSpectrum;
import jspecview.util.JSVTxt;

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
  public static String export(ExportType type, String path, JDXSpectrum spectrum, int startIndex, int endIndex) throws IOException{
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
  private static String toStringAux(ExportType type, JDXSpectrum spectrum,
                                    int startIndex, int endIndex) {

    //String dataType = spectrum.getDataType();
    SB buffer = new SB();
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
          xCompFactor, yCompFactor, type == ExportType.DIFDUP);
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
    default:
			break;
    }

    int index = Arrays.binarySearch(FileReader.VAR_LIST_TABLE[0],
        tmpDataClass);
    String varList = FileReader.VAR_LIST_TABLE[1][index];
    buffer.append(getHeaderString(spectrum, tmpDataClass, minY, maxY,
        xCompFactor, yCompFactor, startIndex, endIndex));
    buffer.append("##" + tmpDataClass + "= " + varList + JSVTxt.newLine);
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

    SB buffer = new SB();
    // start of header
    buffer.append("##TITLE= ").append(spec.getTitle()).append(
        JSVTxt.newLine);
    buffer.append("##JCAMP-DX= 5.01").append(JSVTxt.newLine); /*+ getJcampdx()*/
    buffer.append("##DATA TYPE= ").append(spec.getDataType()).append(
        JSVTxt.newLine);
    buffer.append("##DATA CLASS= ").append(tmpDataClass).append(
        JSVTxt.newLine);
    buffer.append("##ORIGIN= ").append(spec.getOrigin()).append(
        JSVTxt.newLine);
    buffer.append("##OWNER= ").append(spec.getOwner()).append(
        JSVTxt.newLine);
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
    buffer.append("##LONGDATE= ").append(longdate).append(JSVTxt.newLine);

    // optional header
    List<String[]> headerTable = spec.getHeaderTable();
    for (int i = 0; i < headerTable.size(); i++) {
      String[] entry = headerTable.get(i);
      String label = entry[0];
      String dataSet = entry[1];
      String nl = (dataSet.startsWith("<") && dataSet.contains("</") ? JSVTxt.newLine
          : "");
      buffer.append(label).append("= ").append(nl).append(dataSet).append(
          JSVTxt.newLine);
    }
    double observedFreq = spec.getObservedFreq();
    if (!spec.is1D())
      buffer.append("##NUM DIM= ").appendI(spec.numDim).append(
          JSVTxt.newLine);
    if (observedFreq != JDXDataObject.ERROR)
      buffer.append("##.OBSERVE FREQUENCY= ").appendD(observedFreq).append(
          JSVTxt.newLine);
    if (spec.observedNucl != "")
      buffer.append("##.OBSERVE NUCLEUS= ").append(spec.observedNucl).append(
          JSVTxt.newLine);
    //now need to put pathlength here

    // last part of header

    //boolean toHz = (observedFreq != JDXSpectrum.ERROR && !spec.getDataType()
      //  .toUpperCase().contains("FID"));
    buffer.append("##XUNITS= ").append(spec.isHZtoPPM() ? "HZ" : spec.getXUnits()).append(
        JSVTxt.newLine);
    buffer.append("##YUNITS= ").append(spec.getYUnits()).append(
        JSVTxt.newLine);
    buffer.append("##XFACTOR= ").append(JSVTxt.fixExponentInt(tmpXFactor))
        .append(JSVTxt.newLine);
    buffer.append("##YFACTOR= ").append(JSVTxt.fixExponentInt(tmpYFactor))
        .append(JSVTxt.newLine);
    double f = (spec.isHZtoPPM() ? observedFreq : 1);
    Coordinate[] xyCoords = spec.getXYCoords();
    buffer.append("##FIRSTX= ").append(
        JSVTxt.fixExponentInt(xyCoords[startIndex].getXVal() * f)).append(
        JSVTxt.newLine);
    buffer.append("##FIRSTY= ").append(
        JSVTxt.fixExponentInt(xyCoords[startIndex].getYVal())).append(
        JSVTxt.newLine);
    buffer.append("##LASTX= ").append(
        JSVTxt.fixExponentInt(xyCoords[endIndex].getXVal() * f)).append(
        JSVTxt.newLine);
    buffer.append("##NPOINTS= ").appendI((Math.abs(endIndex - startIndex) + 1))
        .append(JSVTxt.newLine);
    buffer.append("##MINY= ").append(JSVTxt.fixExponentInt(minY)).append(
        JSVTxt.newLine);
    buffer.append("##MAXY= ").append(JSVTxt.fixExponentInt(maxY)).append(
        JSVTxt.newLine);
    return buffer.toString();
  }

  private static boolean areIntegers(Coordinate[] xyCoords, int startIndex,
                                     int endIndex, double factor, boolean isX) {
    for (int i = startIndex; i <= endIndex; i++) {
      double x = (isX ? xyCoords[i].getXVal() : xyCoords[i].getYVal()) / factor;
      if (JSVTxt.isAlmostInteger(x))
          return false;
    }
    return true;
  }
  
}
