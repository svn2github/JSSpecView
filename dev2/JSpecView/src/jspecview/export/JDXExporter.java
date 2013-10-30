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

import java.io.IOException;
import java.util.Arrays;

import org.jmol.io.JmolOutputChannel;

import javajs.util.List;

import jspecview.api.JSVExporter;
import jspecview.common.Coordinate;
import jspecview.common.ExportType;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.source.FileReader;
import jspecview.source.JDXDataObject;
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

public class JDXExporter implements JSVExporter {

	public static final String newLine = System.getProperty("line.separator");
	private JmolOutputChannel out;
	private ExportType type;
	private JDXSpectrum spectrum;
	private JSViewer viewer;

	public JDXExporter() {
		
	}
  /**
   * The factor divisor used in compressing spectral data in one of DIF, SQZ,
   * PAC and FIX formats
   */
  private static final double FACTOR_DIVISOR = 1000000;

  /**
   * Exports spectrum in one of several formats
   * @param type
   * @param out
   * @param spectrum the spectrum
   * @param startIndex
   * @param endIndex
   * @return data if path is null
   * @throws IOException
   */
  public String exportTheSpectrum(JSViewer viewer, ExportType type, JmolOutputChannel out, JDXSpectrum spectrum, int startIndex, int endIndex, PanelData pd) throws IOException{
  	this.out = out;
  	this.type = type;
  	this.spectrum = spectrum;
  	this.viewer = viewer;
    toStringAux(startIndex, endIndex);
    out.closeChannel();
    return " (" + out.getByteCount() + " bytes)";
  }

  /**
   * Auxiliary function for the toString functions
   * 
   * @param startIndex
   *        the start Coordinate Index
   * @param endIndex
   *        the end Coordinate Index
   */
  private void toStringAux(int startIndex, int endIndex) {

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
    out.append(getHeaderString(tmpDataClass, minY, maxY,
        xCompFactor, yCompFactor, startIndex, endIndex));
    out.append("##" + tmpDataClass + "= " + varList + newLine);
    out.append(tabDataSet);
    out.append("##END=");
  }

  /**
   * Returns the String for the header of the spectrum
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
  private String getHeaderString(String tmpDataClass,
                                        double minY, double maxY,
                                        double tmpXFactor, double tmpYFactor,
                                        int startIndex, int endIndex) {

    //final String CORE_STR = "TITLE,ORIGIN,OWNER,DATE,TIME,DATATYPE,JCAMPDX";

    // start of header
    out.append("##TITLE= ").append(spectrum.getTitle()).append(
        newLine);
    out.append("##JCAMP-DX= 5.01").append(newLine); /*+ getJcampdx()*/
    out.append("##DATA TYPE= ").append(spectrum.getDataType()).append(
        newLine);
    out.append("##DATA CLASS= ").append(tmpDataClass).append(
        newLine);
    out.append("##ORIGIN= ").append(spectrum.getOrigin()).append(
        newLine);
    out.append("##OWNER= ").append(spectrum.getOwner()).append(
        newLine);
    String d = spectrum.getDate();
    String longdate = "";
    String currentTime = viewer.apiPlatform.getDateFormat(false);
    if (spectrum.getLongDate().equals("") || d.length() != 8) {
      longdate = currentTime + " $$ export date from JSpecView";
    } else if (d.length() == 8) { // give a 50 year window; Y2K compliant
      longdate = (d.charAt(0) < '5' ? "20" : "19") + d + " " + spectrum.getTime();
    } else {
      longdate = spectrum.getLongDate();
    }
    out.append("##LONGDATE= ").append(longdate).append(newLine);

    // optional header
    List<String[]> headerTable = spectrum.getHeaderTable();
    for (int i = 0; i < headerTable.size(); i++) {
      String[] entry = headerTable.get(i);
      String label = entry[0];
      String dataSet = entry[1];
      String nl = (dataSet.startsWith("<") && dataSet.contains("</") ? newLine
          : "");
      out.append(label).append("= ").append(nl).append(dataSet).append(
          newLine);
    }
    double observedFreq = spectrum.getObservedFreq();
    if (!spectrum.is1D())
      out.append("##NUM DIM= ").append("" + spectrum.numDim).append(
          newLine);
    if (observedFreq != JDXDataObject.ERROR)
      out.append("##.OBSERVE FREQUENCY= ").append("" + observedFreq).append(
          newLine);
    if (spectrum.observedNucl != "")
      out.append("##.OBSERVE NUCLEUS= ").append(spectrum.observedNucl).append(
          newLine);
    //now need to put pathlength here

    // last part of header

    //boolean toHz = (observedFreq != JDXSpectrum.ERROR && !spec.getDataType()
      //  .toUpperCase().contains("FID"));
    out.append("##XUNITS= ").append(spectrum.isHZtoPPM() ? "HZ" : spectrum.getXUnits()).append(
        newLine);
    out.append("##YUNITS= ").append(spectrum.getYUnits()).append(
        newLine);
    out.append("##XFACTOR= ").append(JSVTxt.fixExponentInt(tmpXFactor))
        .append(newLine);
    out.append("##YFACTOR= ").append(JSVTxt.fixExponentInt(tmpYFactor))
        .append(newLine);
    double f = (spectrum.isHZtoPPM() ? observedFreq : 1);
    Coordinate[] xyCoords = spectrum.getXYCoords();
    out.append("##FIRSTX= ").append(
        JSVTxt.fixExponentInt(xyCoords[startIndex].getXVal() * f)).append(
        newLine);
    out.append("##FIRSTY= ").append(
        JSVTxt.fixExponentInt(xyCoords[startIndex].getYVal())).append(
        newLine);
    out.append("##LASTX= ").append(
        JSVTxt.fixExponentInt(xyCoords[endIndex].getXVal() * f)).append(
        newLine);
    out.append("##NPOINTS= ").append("" + (Math.abs(endIndex - startIndex) + 1))
        .append(newLine);
    out.append("##MINY= ").append(JSVTxt.fixExponentInt(minY)).append(
        newLine);
    out.append("##MAXY= ").append(JSVTxt.fixExponentInt(maxY)).append(
        newLine);
    return out.toString();
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
