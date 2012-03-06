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

package jspecview.common;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.Vector;

import jspecview.exception.JSpecViewException;
import jspecview.source.JDXDecompressor;
import jspecview.util.Logger;
import jspecview.util.TextFormat;

/**
 * <code>JDXSpectrum</code> implements the Interface Spectrum for the display of
 * JDX Files.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class JDXSpectrum extends JDXDataObject implements Graph {

  @Override
  public void finalize() {
    System.out.println("JDXSpectrum " + this + " finalized");
  }

  /**
   * Vector of x,y coordinates
   */
  private Coordinate[] xyCoords;

  /**
   * whether the x values were converted from HZ to PPM
   */
  private boolean isHZtoPPM = false;

  private String currentTime = (new SimpleDateFormat(
      "yyyy/MM/dd HH:mm:ss.SSSS ZZZZ"))
      .format(Calendar.getInstance().getTime());

  private ArrayList<PeakInfo> peakList = new ArrayList<PeakInfo>();

  /**
   * Constructor
   */
  public JDXSpectrum() {
    System.out.println("initialize JDXSpectrum " + this);
    headerTable = new Vector<String[]>();
    xyCoords = new Coordinate[0];
  }

  /*   ---------- Constructed data --------------------------------------------------- */

  /**
   * Sets the array of coordinates
   * 
   * @param coords
   *        the array of Coordinates
   */
  public void setXYCoords(Coordinate[] coords) {
    xyCoords = coords;
  }

  /**
   * Returns the array of coordinates
   * 
   * @return the array of coordinates
   */
  public Coordinate[] getXYCoords() {
    return xyCoords;
  }

  /**
   * Determines if the spectrum should be displayed with abscissa unit of Part
   * Per Million (PPM) instead of Hertz (HZ)
   * 
   * @return true if abscissa unit should be PPM
   */
  public boolean isHZtoPPM() {
    return isHZtoPPM;
  }

  /**
   * Sets the value to true if the spectrum should be displayed with abscissa
   * unit of Part Per Million (PPM) instead of Hertz (HZ)
   * 
   * @param val
   *        true or false
   */
  public void setHZtoPPM(boolean val) {
    isHZtoPPM = val;
  }

  /**
   * Returns the first X value
   * 
   * @return the first X value
   */
  public double getFirstX() {
    return xyCoords[0].getXVal();
  }

  /**
   * Returns the first Y value
   * 
   * @return the first Y value
   */
  public double getFirstY() {
    //if(isIncreasing())
    return xyCoords[0].getYVal();
    //else
    //  return xyCoords[getNumberOfPoints() - 1].getYVal();
  }

  /**
   * Returns the last X value
   * 
   * @return the last X value
   */
  public double getLastX() {
    // if(isIncreasing())
    return xyCoords[getNumberOfPoints() - 1].getXVal();
    // else
    //   return xyCoords[0].getXVal();
  }

  /**
   * Returns the last Y value
   * 
   * @return the last Y value
   */
  public double getLastY() {
    return xyCoords[getNumberOfPoints() - 1].getYVal();
  }

  /**
   * Returns the number of points
   * 
   * @return the number of points
   */
  public int getNumberOfPoints() {
    return xyCoords.length;
  }

  private double minX = Double.NaN, minY = Double.NaN;
  private double maxX = Double.NaN, maxY = Double.NaN;
  private double deltaX = Double.NaN;

  /**
   * Calculates and returns the minimum x value in the list of coordinates
   * Fairly expensive operation
   * 
   * @return the minimum x value in the list of coordinates
   */
  public double getMinX() {
    return (Double.isNaN(minX) ? (minX = Coordinate.getMinX(xyCoords)) : minX);
  }

  /**
   * Calculates and returns the minimum y value in the list of coordinates
   * Fairly expensive operation
   * 
   * @return the minimum x value in the list of coordinates
   */
  public double getMinY() {
    return (Double.isNaN(minY) ? (minY = Coordinate.getMinY(xyCoords)) : minY);
  }

  /**
   * Calculates and returns the maximum x value in the list of coordinates
   * Fairly expensive operation
   * 
   * @return the maximum x value in the list of coordinates
   */
  public double getMaxX() {
    return (Double.isNaN(maxX) ? (maxX = Coordinate.getMaxX(xyCoords)) : maxX);
  }

  /**
   * Calculates and returns the maximum y value in the list of coordinates
   * Fairly expensive operation
   * 
   * @return the maximum y value in the list of coordinates
   */
  public double getMaxY() {
    return (Double.isNaN(maxY) ? (maxY = Coordinate.getMaxY(xyCoords)) : maxY);
  }

  /**
   * Returns the delta X
   * 
   * @return the delta X
   */
  public double getDeltaX() {
    return (Double.isNaN(deltaX) ? (deltaX = Coordinate.deltaX(getLastX(), getFirstX(), getNumberOfPoints())) : deltaX);
  }

  /**
   * Returns the observed frequency (for NMR Spectra)
   * 
   * @return the observed frequency (for NMR Spectra)
   */
  public double getObservedFreq() {
    return observedFreq;
  }

  // ***************************** To String Methods ****************************

  /**
   * Returns the String for the header of the spectrum
   * 
   * @param tmpDataClass
   *        the dataclass
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
  public String getHeaderString(String tmpDataClass, double tmpXFactor,
                                double tmpYFactor, int startIndex, int endIndex) {

    //final String CORE_STR = "TITLE,ORIGIN,OWNER,DATE,TIME,DATATYPE,JCAMPDX";

    DecimalFormat varFormatter = TextFormat.getDecimalFormat("0.########");
    DecimalFormat sciFormatter = TextFormat.getDecimalFormat("0.########E0");

    StringBuffer buffer = new StringBuffer();
    // start of header
    buffer.append("##TITLE= ").append(getTitle())
        .append(TextFormat.newLine);
    buffer.append("##JCAMP-DX= 5.01").append(TextFormat.newLine); /*+ getJcampdx()*/
    buffer.append("##DATA TYPE= ").append(getDataType()).append(
        TextFormat.newLine);
    buffer.append("##DATA CLASS= ").append(tmpDataClass).append(
        TextFormat.newLine);
    buffer.append("##ORIGIN= ").append(getOrigin()).append(
        TextFormat.newLine);
    buffer.append("##OWNER= ").append(getOwner())
        .append(TextFormat.newLine);

    String d = getDate();
    String longdate = "";
    if (getLongDate().equals("") || d.length() != 8) {
      longdate = currentTime + " $$ export date from JSpecView";
    } else if (d.length() == 8) { // give a 50 year window; Y2K compliant
      longdate = (d.charAt(0) < '5' ? "20" : "19") + d + " " + getTime();
    } else {
      longdate = getLongDate();
    }
    buffer.append("##LONGDATE= ").append(longdate).append(
        TextFormat.newLine);

    // optional header
    for (int i = 0; i < headerTable.size(); i++) {
      String[] entry = headerTable.get(i);
      String label = entry[0];
      String dataSet = entry[1];
      String nl = (dataSet.startsWith("<") && dataSet.contains("</") ? TextFormat.newLine
          : "");
      buffer.append(label).append("= ").append(nl).append(dataSet).append(
          TextFormat.newLine);
    }
    if (getObservedFreq() != ERROR)
      buffer.append("##.OBSERVE FREQUENCY= ").append(getObservedFreq()).append(
          TextFormat.newLine);
    if (observedNucl != "")
      buffer.append("##.OBSERVE NUCLEUS= ").append(observedNucl).append(
          TextFormat.newLine);
    //now need to put pathlength here

    // last part of header

    buffer.append("##XUNITS= ").append(getObservedFreq() == ERROR
                || getDataType().toUpperCase().contains("FID") ? getXUnits()
                : "HZ").append(TextFormat.newLine);
    buffer.append("##YUNITS= ").append(getYUnits()).append(
        TextFormat.newLine);
    buffer.append("##XFACTOR= ").append(sciFormatter.format(tmpXFactor))
        .append(TextFormat.newLine);
    buffer.append("##YFACTOR= ").append(sciFormatter.format(tmpYFactor))
        .append(TextFormat.newLine);
    double f = (getObservedFreq() == ERROR ? 1 : getObservedFreq());
    buffer.append("##FIRSTX= ").append(
        varFormatter.format(xyCoords[startIndex].getXVal() * f)).append(
        TextFormat.newLine);
    buffer.append("##FIRSTY= ").append(
        varFormatter.format(xyCoords[startIndex].getYVal())).append(
        TextFormat.newLine);
    buffer.append("##LASTX= ").append(
        varFormatter.format(xyCoords[endIndex].getXVal() * f)).append(
        TextFormat.newLine);
    buffer.append("##NPOINTS= ").append((endIndex - startIndex + 1)).append(
        TextFormat.newLine);
    return buffer.toString();
  }

  /**
   * Returns a copy of this <code>JDXSpectrum</code>
   * 
   * @return a copy of this <code>JDXSpectrum</code>
   */
  public JDXSpectrum copy() {
    JDXSpectrum newSpectrum = new JDXSpectrum();

    newSpectrum.setTitle(getTitle());
    newSpectrum.setJcampdx(getJcampdx());
    newSpectrum.setOrigin(getOrigin());
    newSpectrum.setOwner(getOwner());
    newSpectrum.setDataClass(getDataClass());
    newSpectrum.setDataType(getDataType());
    newSpectrum.setHeaderTable(getHeaderTable());

    newSpectrum.setXFactor(getXFactor());
    newSpectrum.setYFactor(getYFactor());
    newSpectrum.setXUnits(getXUnits());
    newSpectrum.setYUnits(getYUnits());

    //newSpectrum.setPathlength(getPathlength());
    newSpectrum.setPeakList(getPeakList());
    newSpectrum.setXYCoords(getXYCoords());
    newSpectrum.setContinuous(isContinuous());
    newSpectrum.setIncreasing(isIncreasing());

    newSpectrum.observedFreq = observedFreq;
    newSpectrum.observedNucl = observedNucl;
    newSpectrum.offset = offset;
    newSpectrum.dataPointNum = dataPointNum;
    newSpectrum.shiftRefType = shiftRefType;
    newSpectrum.isHZtoPPM = isHZtoPPM;

    return newSpectrum;
  }

  public boolean isTransmittance() {
    String s = yUnits.toLowerCase();
    return (s.equals("transmittance") || s.contains("trans") || s.equals("t"));
  }

  public boolean isAbsorbance() {
    String s = yUnits.toLowerCase();
    return (s.equals("absorbance") || s.contains("abs") || s.equals("a"));
  }

  public ArrayList<PeakInfo> getPeakList() {
    return peakList;
  }

  public int setPeakList(ArrayList<PeakInfo> list) {
    peakList = list;
    return list.size();
  }

  public String getPeakType() {
    if (peakList == null || peakList.size() == 0)
      return null;
    return peakList.get(0).getType();
  }

  public boolean hasPeakIndex(String index) {
    if (peakList != null && peakList.size() > 0)
      for (int i = 0; i < peakList.size(); i++)
        if (index.equals(peakList.get(i).getIndex())) {
          selectedPeak = peakList.get(i);
          return true;
        }
    return false;
  }

  private PeakInfo selectedPeak;

  public void setSelectedPeak(PeakInfo peak) {
    selectedPeak = peak;
  }
  
  public PeakInfo getSelectedPeak() {
    return selectedPeak;
  }

  public String getAssociatedPeakInfo(Coordinate coord) {
    selectedPeak = null;
    if (peakList != null && peakList.size() > 0)
      for (int i = 0; i < peakList.size(); i++) {
        PeakInfo peak = peakList.get(i);
        double xVal = coord.getXVal();
        if (xVal >= peak.getXMin() && xVal <= peak.getXMax()) {
          return (selectedPeak = peak).getStringInfo();
        }
      }
    return null;
  }

  public String getPeakTitle() {
    return (selectedPeak == null ? getTitleLabel() : selectedPeak.getTitle());
  }

  public String getTitleLabel() {
    String type = (peakList == null || peakList.size() == 0 ? dataType
        : peakList.get(0).getType());
    return (type != null && type.length() > 0 ? type + " " : "") + title;
  }

  public boolean processTabularData(String tabularSpecData, int tabDataLineNo,
                                    List<String[]> table,
                                    StringBuffer errorLog)
      throws JSpecViewException {
    if (tabularSpecData == null)
      throw new JSpecViewException("Error Reading Data Set");

    if (dataClass.equals("PEAKASSIGNMENTS"))
      return true;

    setHeaderTable(table);

    if (dataClass.equals("XYDATA")) {
      checkRequiredTokens();      
      decompressData(tabularSpecData, tabDataLineNo, errorLog);
      return true;
    }
    if (dataClass.equals("PEAKTABLE") || dataClass.equals("XYPOINTS")) {
      continuous = dataClass.equals("XYPOINTS");
      // check if there is an x and y factor
      if (xFactor != ERROR && yFactor != ERROR)
        xyCoords = Coordinate.parseDSV(tabularSpecData, xFactor, yFactor);
      else
        xyCoords = Coordinate.parseDSV(tabularSpecData, 1, 1);

      double fileDeltaX = Coordinate.deltaX(xyCoords[xyCoords.length - 1]
          .getXVal(), xyCoords[0].getXVal(), xyCoords.length);
      increasing = (fileDeltaX > 0);
      return true;
    }
    return false;
  }

  public String getTabularData(String label, String value) {
    if (label.equals("##PEAKASSIGNMENTS"))
      setDataClass("PEAKASSIGNMENTS");
    else if (label.equals("##PEAKTABLE"))
      setDataClass("PEAKTABLE");
    else if (label.equals("##XYDATA"))
      setDataClass("XYDATA");
    else if (label.equals("##XYPOINTS"))
      setDataClass("XYPOINTS");
    // skip header lines
    int pt = value.indexOf('\n') + 1;
    while (pt < value.length() && "0123456789.-+".indexOf(value.charAt(pt)) < 0) 
      pt++;
    value = value.substring(pt);
    return value;
    
   
  }

  public boolean createXYCoords(Map<String, ArrayList<String>> nTupleTable,
                                String[] plotSymbols, String dataType,
                                String tabularSpecData, int tabDataLineNo,
                                StringBuffer errorLog) {
    ArrayList<String> list;
    if (dataClass.equals("XYDATA")) {
      // Get Label Values

      list = nTupleTable.get("##SYMBOL");
      int index1 = list.indexOf(plotSymbols[0]);
      int index2 = list.indexOf(plotSymbols[1]);

      list = nTupleTable.get("##FACTOR");
      xFactor = Double.parseDouble(list.get(index1));
      yFactor = Double.parseDouble(list.get(index2));

      list = nTupleTable.get("##LAST");
      fileLastX = Double.parseDouble(list.get(index1));

      list = nTupleTable.get("##FIRST");
      fileFirstX = Double.parseDouble(list.get(index1));
      //firstY = Double.parseDouble((String)list.get(index2));

      list = nTupleTable.get("##VARDIM");
      nPointsFile = Integer.parseInt(list.get(index1));

      list = nTupleTable.get("##UNITS");
      xUnits = list.get(index1);
      yUnits = list.get(index2);

      decompressData(tabularSpecData, tabDataLineNo, errorLog);
      return true;
    }
    if (dataClass.equals("PEAKTABLE") || dataClass.equals("XYPOINTS")) {
      continuous = dataClass.equals("XYPOINTS");
      list = nTupleTable.get("##SYMBOL");
      int index1 = list.indexOf(plotSymbols[0]);
      int index2 = list.indexOf(plotSymbols[1]);

      list = nTupleTable.get("##UNITS");
      xUnits = list.get(index1);
      yUnits = list.get(index2);
      xyCoords = Coordinate.parseDSV(tabularSpecData, xFactor, yFactor);
      return true;
    }
    return false;
  }

  private void decompressData(String tabularSpecData, int tabDataLineNo,
                              StringBuffer errorLog) {

    
    int errPt = errorLog.length();
    double fileDeltaX = Coordinate.deltaX(fileLastX, fileFirstX, nPointsFile);
    increasing = (fileDeltaX > 0);
    continuous = true;
    JDXDecompressor decompressor = new JDXDecompressor(tabularSpecData,
        fileFirstX, xFactor, yFactor, fileDeltaX, nPointsFile, tabDataLineNo);

    double[] firstLastX = new double[2];
    xyCoords = decompressor.decompressData(errorLog, firstLastX);

    if (xyCoords == null)
      xyCoords = Coordinate.parseDSV(tabularSpecData, xFactor, yFactor);

    if (errorLog.length() != errPt) {
      errorLog.append(getTitleLabel()).append("\n");
      errorLog.append("firstX: "
          + fileFirstX
          + " Found " + firstLastX[0] + "\n");
      errorLog.append("lastX from Header "
          + fileLastX
          + " Found " + firstLastX[1] + "\n");
      errorLog.append("deltaX from Header " + fileDeltaX + "\n");
      errorLog.append("Number of points in Header " + nPointsFile + " Found "
          + xyCoords.length + "\n");
    } else {
      //errorLog.append("No Errors decompressing data\n");
    }

    if (Logger.debugging) {
      System.err.println(errorLog.toString());
    }

    // apply offset
    if (offset != ERROR && observedFreq != ERROR
        && dataType.toUpperCase().contains("SPECTRUM")) {
      Coordinate.applyShiftReference(xyCoords, dataPointNum, fileFirstX,
          fileLastX, offset, observedFreq, shiftRefType);
    }

    if (observedFreq != ERROR && xUnits.toUpperCase().equals("HZ")) {
      double xScale = observedFreq;
      Coordinate.applyScale(xyCoords, (1 / xScale), 1);
      xUnits = "PPM";
      setHZtoPPM(true);
    }
  }

  double lastX = Double.NaN;
  
  public int setNextPeak(Coordinate coord, int istep) {
    if (peakList == null || peakList.size() == 0)
      return -1;
    double x0 = coord.getXVal() + istep * 0.000001;
    int ipt1 = -1;
    int ipt2 = -1;
    double dmin1 = Double.MAX_VALUE * istep;
    double dmin2 = 0;
    for (int i = peakList.size(); --i >= 0;) {
      double x = peakList.get(i).getX();
      if (istep > 0) {
        if (x > x0 && x < dmin1) {
          // nearest on right
          ipt1 = i;
          dmin1 = x;
        } else if (x < x0 && x - x0 < dmin2) {
          // farthest on left
          ipt2 = i;
          dmin2 = x - x0;
        }
      } else {
        if (x < x0 && x > dmin1) {
          // nearest on left
          ipt1 = i;
          dmin1 = x;
        } else if (x > x0 && x - x0 > dmin2) {
          // farthest on right
          ipt2 = i;
          dmin2 = x - x0;
        }
      }
    }

    if (ipt1 < 0) {
      if (ipt2 < 0)
        return -1;
      ipt1 = ipt2;
    }
    return ipt1;
  }
 
  private boolean increasing = true;
  private boolean continuous;

  /**
   * Sets value to true if spectrum is increasing
   * 
   * @param val
   *        true if spectrum is increasing
   */
  public void setIncreasing(boolean val) {
    increasing = val;
  }

  /**
   * Sets value to true if spectrum is continuous
   * 
   * @param val
   *        true if spectrum is continuous
   */
  public void setContinuous(boolean val) {
    continuous = val;
  }

  /**
   * Returns true if the spectrum is increasing
   * 
   * @return true if the spectrum is increasing
   */
  public boolean isIncreasing() {
    return increasing;
  }

  /**
   * Returns true if spectrum is continuous
   * 
   * @return true if spectrum is continuous
   */
  public boolean isContinuous() {
    return continuous;
  }

  public Object[][] getHeaderRowDataAsArray() {
    Object[][] rowData = getHeaderRowDataAsArray(true, 8);
    int i = rowData.length - 8;
    rowData[i++] = new Object[] { "##XUNITS", isHZtoPPM() ? "HZ" : getXUnits() };
    rowData[i++] = new Object[] { "##YUNITS", getYUnits() };
    double x = (isIncreasing() ? getFirstX() : getLastX());
    rowData[i++] = new Object[] { "##FIRSTX",
        String.valueOf(isHZtoPPM() ? x * getObservedFreq() : x) };
    x = (isIncreasing() ? getLastX() : getFirstX());
    rowData[i++] = new Object[] { "##FIRSTY",
        String.valueOf(isIncreasing() ? getFirstY() : getLastY()) };
    rowData[i++] = new Object[] { "##LASTX",
        String.valueOf(isHZtoPPM() ? x * getObservedFreq() : x) };
    rowData[i++] = new Object[] { "##XFACTOR", String.valueOf(getXFactor()) };
    rowData[i++] = new Object[] { "##YFACTOR", String.valueOf(getYFactor()) };
    rowData[i++] = new Object[] { "##NPOINTS",
        String.valueOf(getNumberOfPoints()) };
    return rowData;
  }

  /**
   * Determines if a spectrum is an HNMR spectrum
   * @param spectrum the JDXSpectrum
   * @return true if an HNMR, false otherwise
   */
  public boolean isHNMR() {
    return (dataType.toUpperCase().indexOf("NMR") >= 0 && observedNucl.toUpperCase().indexOf("H") >= 0);
  }

  /**
   * Determines if the plot should be displayed decreasing by default
   * @param spectrum
   */
  public boolean shouldDisplayXAxisIncreasing(){
  String datatype = getDataType();
  String xUnits = getXUnits();
      
    if (datatype.toUpperCase().contains("NMR") && !(datatype.toUpperCase().contains("FID"))) {
      return false;
    }else if(datatype.toUpperCase().contains("LINK") && xUnits.toUpperCase().contains("CM")){
      return false;    // I think this was because of a bug where BLOCK files kept type as LINK ?      
    }else if (datatype.toUpperCase().contains("INFRA") && xUnits.toUpperCase().contains("CM")) {
        return false;
    }else if (datatype.toUpperCase().contains("RAMAN") && xUnits.toUpperCase().contains("CM")) {
        return false;
    }
    else if(datatype.toUpperCase().contains("VIS") && xUnits.toUpperCase().contains("NANOMETERS")){
      return true;
    }
    
    return isIncreasing();
  }

  private IntegralGraph integration;

  public IntegralGraph getIntegrationGraph() {
    return integration;
  }
  
  public void setIntegrationGraph(IntegralGraph graph) {
    integration = graph;
  }

  private Comparator<Coordinate> c;
  
  public double getPercentYValueAt(double x) {
    if (!isContinuous())
      return Double.NaN;
    if (c == null)
      c = new CoordComparator();
    return Coordinate.getYValueAt(xyCoords, x, c);
  }

  private JDXSpectrum convertedSpectrum;

  public static final int TA_NO_CONVERT = 0;
  public static final int TO_ABS = 1;
  public static final int TO_TRANS = 2;
  public static final int IMPLIED = 3;
  public static final double MAXABS = 4; // maximum absorbance allowed

  public JDXSpectrum getConvertedSpectrum() {
    return convertedSpectrum;
  }
  
  public void setConvertedSpectrum(JDXSpectrum spectrum) {
    convertedSpectrum = spectrum;
  }

  public static JDXSpectrum taConvert(JDXSpectrum spectrum, int mode) {
    if (!spectrum.isContinuous())
      return spectrum;
    switch (mode) {
    case TO_ABS:
      if (!spectrum.isTransmittance())
        return spectrum;
      break;
    case TO_TRANS:
      if (!spectrum.isAbsorbance())
        return spectrum;
      break;
    case IMPLIED:
      break;
    default:
      return spectrum;
    }
    return convert(spectrum);
  }
  /**
   * Converts and returns a converted spectrum. If original was Absorbance then
   * a Transmittance spectrum is returned and vice versa if spectrum was neither
   * Absorbance nor Transmittance then null is returned
   * 
   * @param spectrum
   *        the JDXSpectrum
   * @return the converted spectrum
   */
  public static JDXSpectrum convert(JDXSpectrum spectrum) {
    JDXSpectrum spec = spectrum.getConvertedSpectrum();
    return (spec != null ? spec : spectrum.isAbsorbance() ? toT(spectrum) : toA(spectrum));
  }

  /**
   * Converts a spectrum from Absorbance to Transmittance
   * 
   * @param spectrum
   *        the JDXSpectrum
   * @return the converted spectrum
   */
  
  public static JDXSpectrum toT(JDXSpectrum spectrum) {
    if (!spectrum.isAbsorbance())
      return null;
    Coordinate[] xyCoords = spectrum.getXYCoords();
    Coordinate[] newXYCoords = new Coordinate[xyCoords.length];
    if (!isYInRange(xyCoords, 0, MAXABS))
      xyCoords = normalise(xyCoords, 0, MAXABS);
    for (int i = 0; i < xyCoords.length; i++)
      newXYCoords[i] = new Coordinate(xyCoords[i].getXVal(),
          toTransmittance(xyCoords[i].getYVal()));
    return newSpectrum(spectrum, newXYCoords, "TRANSMITTANCE");
  }

  /**
   * Converts a spectrum from Transmittance to Absorbance
   * 
   * @param spectrum
   *        the JDXSpectrum
   * @return the converted spectrum
   */
  public static JDXSpectrum toA(JDXSpectrum spectrum) {
    if (!spectrum.isTransmittance())
      return null;
    Coordinate[] xyCoords = spectrum.getXYCoords();
    Coordinate[] newXYCoords = new Coordinate[xyCoords.length];
    boolean isPercent = isYInRange(xyCoords, -2, 2);
    for (int i = 0; i < xyCoords.length; i++)
      newXYCoords[i] = new Coordinate(xyCoords[i].getXVal(), 
          toAbsorbance(xyCoords[i].getYVal(), isPercent));
    return newSpectrum(spectrum, newXYCoords, "ABSORBANCE");
  }

  /**
   * copy spectrum with new cooordinates
   * 
   * @param spectrum
   * @param newXYCoords
   * @param units
   * @return
   */
  public static JDXSpectrum newSpectrum(JDXSpectrum spectrum,
                                         Coordinate[] newXYCoords,
                                         String units) {
    JDXSpectrum convSpectrum = spectrum.copy();
    convSpectrum.setOrigin("JSpecView Converted");
    convSpectrum.setOwner("JSpecView Generated");
    convSpectrum.setXYCoords(newXYCoords);
    convSpectrum.setYUnits(units);
    spectrum.setConvertedSpectrum(convSpectrum);
    convSpectrum.setConvertedSpectrum(spectrum);
    return convSpectrum;
  }

  /**
   * Converts a value in Transmittance to Absorbance -- max of MAXABS (4)
   * 
   * 
   * @param x
   * @param isPercent
   * @return the value in Absorbance
   */
  public static double toAbsorbance(double x, boolean isPercent) {
    return (Math.min(MAXABS, isPercent ? 2 - log10(x) : -log10(x)));
  }

  /**
   * Converts a value from Absorbance to Transmittance
   * 
   * @param x
   * @return the value in Transmittance
   */
  public static double toTransmittance(double x) {
    return (x <= 0 ? 1 : Math.pow(10, -x));
  }

  /**
   * Returns the log of a value to the base 10
   * 
   * @param value
   *        the input value
   * @return the log of a value to the base 10
   */
  public static double log10(double value) {
    return Math.log(value) / Math.log(10);
  }

  /**
   * Determines if the y values of a spectrum are in a certain range
   * 
   * @param xyCoords
   * @param min
   * @param max
  * @return true is in range, otherwise false
   */
  public static boolean isYInRange(Coordinate[] xyCoords, double min,
                                    double max) {
    return (Coordinate.getMinY(xyCoords) >= min 
        && Coordinate.getMaxY(xyCoords) >= max);
  }

  /**
   * Normalises the y values of a spectrum to a certain range
   * 
   * @param xyCoords
   * @param min
   * @param max
   * @return array of normalised coordinates
   */
  public static Coordinate[] normalise(Coordinate[] xyCoords, double min,
                                        double max) {
    Coordinate[] newXYCoords = new Coordinate[xyCoords.length];
    double minY = Coordinate.getMinY(xyCoords);
    double maxY = Coordinate.getMaxY(xyCoords);
    double factor = (maxY - minY) / (max - min); // range = 0-5
    for (int i = 0; i < xyCoords.length; i++)
      newXYCoords[i] = new Coordinate(xyCoords[i].getXVal(), 
          ((xyCoords[i].getYVal() - minY) / factor) - min);
    return newXYCoords;
  }

  public IntegralGraph integrate(double minY, double offset, double factor) {
    if (!isHNMR())
      return null;
    IntegralGraph graph = new IntegralGraph(this, minY, offset, factor, xUnits,
        yUnits);
    setIntegrationGraph(graph);
    return graph;
  }

  public static boolean areScalesCompatible(List<JDXSpectrum> spectra) {
    JDXSpectrum[] specs = new JDXSpectrum[spectra.size()];
    for (int i = spectra.size(); --i >= 0;)
      specs[i] = spectra.get(i);
    return areScalesCompatible(specs);
  }

  public static boolean areScalesCompatible(Graph[] spectra) {
    String xUnit = spectra[0].getXUnits();
    String yUnit = spectra[0].getYUnits();
    int numOfSpectra = spectra.length;
  
    for (int i = 1; i < numOfSpectra; i++) {
      String tempXUnit, tempYUnit;
      tempXUnit = spectra[i].getXUnits();
      tempYUnit = spectra[i].getYUnits();
      if (!xUnit.equals(tempXUnit) || !yUnit.equals(tempYUnit)) {
        return false;
      }
      xUnit = tempXUnit;
      yUnit = tempYUnit;
    }
  
    return true;
  }

  public static boolean process(List<JDXSpectrum> specs, int irMode,
                             boolean autoIntegrate, 
                             double minY, double offset, double factor) {
    boolean haveIntegral = false;
    if (irMode == TO_ABS || irMode == TO_TRANS)
      for (int i = 0; i < specs.size(); i++)
        specs.set(i, taConvert(specs.get(i), irMode));
    if (autoIntegrate)
      for (int i = 0; i < specs.size(); i++)
        haveIntegral |= (specs.get(i).integrate(minY, offset, factor) != null);
    return haveIntegral;
  }

  public List<Integral> getIntegrals() {
   return (integration == null ? null : integration.getIntegrals());
  }
}
