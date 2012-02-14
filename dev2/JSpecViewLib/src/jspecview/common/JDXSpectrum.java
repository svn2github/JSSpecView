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
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import jspecview.exception.JSpecViewException;
import jspecview.source.JDXDecompressor;

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
   * HashMap of optional header values
   */
  private HashMap<String, String> headerTable;

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
    headerTable = new HashMap<String, String>();
    xyCoords = new Coordinate[0];
  }

  /**
   * Sets the header table
   * 
   * @param table
   *        a map of header labels and corresponding datasets
   */
  public void setHeaderTable(HashMap<String, String> table) {
    headerTable = table;
  }

  /**
   * Returns the table of headers
   * 
   * @return the table of headers
   */
  public HashMap<String, String> getHeaderTable() {
    return headerTable;
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
    return (Double.isNaN(minX) ? (minX = JSpecViewUtils.getMinX(xyCoords)) : minX);
  }

  /**
   * Calculates and returns the minimum y value in the list of coordinates
   * Fairly expensive operation
   * 
   * @return the minimum x value in the list of coordinates
   */
  public double getMinY() {
    return (Double.isNaN(minY) ? (minY = JSpecViewUtils.getMinY(xyCoords)) : minY);
  }

  /**
   * Calculates and returns the maximum x value in the list of coordinates
   * Fairly expensive operation
   * 
   * @return the maximum x value in the list of coordinates
   */
  public double getMaxX() {
    return (Double.isNaN(maxX) ? (maxX = JSpecViewUtils.getMaxX(xyCoords)) : maxX);
  }

  /**
   * Calculates and returns the maximum y value in the list of coordinates
   * Fairly expensive operation
   * 
   * @return the maximum y value in the list of coordinates
   */
  public double getMaxY() {
    return (Double.isNaN(maxY) ? (maxY = JSpecViewUtils.getMaxY(xyCoords)) : maxY);
  }

  /**
   * Returns the delta X
   * 
   * @return the delta X
   */
  public double getDeltaX() {
    return (Double.isNaN(deltaX) ? (deltaX = JSpecViewUtils.deltaX(getLastX(), getFirstX(), getNumberOfPoints())) : deltaX);
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

    DecimalFormat varFormatter = new DecimalFormat("0.########",
        new DecimalFormatSymbols(java.util.Locale.US));
    DecimalFormat sciFormatter = new DecimalFormat("0.########E0",
        new DecimalFormatSymbols(java.util.Locale.US));

    StringBuffer buffer = new StringBuffer();
    String longdate = "";
    // start of header
    buffer.append("##TITLE= " + getTitle() + JSpecViewUtils.newLine);
    buffer.append("##JCAMP-DX= 5.01" /*+ getJcampdx()*/
        + JSpecViewUtils.newLine);
    buffer.append("##DATA TYPE= " + getDataType() + JSpecViewUtils.newLine);
    buffer.append("##DATA CLASS= " + tmpDataClass + JSpecViewUtils.newLine);
    buffer.append("##ORIGIN= " + getOrigin() + JSpecViewUtils.newLine);
    buffer.append("##OWNER= " + getOwner() + JSpecViewUtils.newLine);

    if ((getLongDate().equals("")) || (getDate().length() != 8))
      longdate = currentTime + " $$ export date from JSpecView";

    // give a 50 year window
    // Y2K compliant
    if (getDate().length() == 8) {
      if (getDate().charAt(0) < '5')
        longdate = "20" + getDate() + " " + getTime();
      else
        longdate = "19" + getDate() + " " + getTime();
    }
    if (!getLongDate().equals(""))
      longdate = getLongDate();

    buffer.append("##LONGDATE= " + longdate + JSpecViewUtils.newLine);

    // optional header
    for (Iterator<String> iter = headerTable.keySet().iterator(); iter
        .hasNext();) {
      String label = (String) iter.next();
      String dataSet = (String) headerTable.get(label);
      String nl = (dataSet.startsWith("<") && dataSet.contains("</") ? JSpecViewUtils.newLine : "");
        
      buffer.append(label + "= " + nl + dataSet + JSpecViewUtils.newLine);
    }
    if (getObservedFreq() != ERROR)
      buffer.append("##.OBSERVE FREQUENCY= " + getObservedFreq()
          + JSpecViewUtils.newLine);
    //now need to put pathlength here

    // last part of header

    if ((getObservedFreq() != ERROR)
        && !(getDataType().toUpperCase().contains("FID")))
      buffer.append("##XUNITS= HZ" + JSpecViewUtils.newLine);
    else
      buffer.append("##XUNITS= " + getXUnits() + JSpecViewUtils.newLine);

    buffer.append("##YUNITS= " + getYUnits() + JSpecViewUtils.newLine);
    buffer.append("##XFACTOR= " + sciFormatter.format(tmpXFactor)
        + JSpecViewUtils.newLine);
    buffer.append("##YFACTOR= " + sciFormatter.format(tmpYFactor)
        + JSpecViewUtils.newLine);
    if (getObservedFreq() != ERROR)
      buffer.append("##FIRSTX= "
          + varFormatter.format(xyCoords[startIndex].getXVal()
              * getObservedFreq()) + JSpecViewUtils.newLine);
    else
      buffer.append("##FIRSTX= "
          + varFormatter.format(xyCoords[startIndex].getXVal())
          + JSpecViewUtils.newLine);
    buffer.append("##FIRSTY= "
        + varFormatter.format(xyCoords[startIndex].getYVal())
        + JSpecViewUtils.newLine);
    if (getObservedFreq() != ERROR)
      buffer.append("##LASTX= "
          + varFormatter.format(xyCoords[endIndex].getXVal()
              * getObservedFreq()) + JSpecViewUtils.newLine);
    else
      buffer.append("##LASTX= "
          + varFormatter.format(xyCoords[endIndex].getXVal())
          + JSpecViewUtils.newLine);
    buffer.append("##NPOINTS= " + (endIndex - startIndex + 1)
        + JSpecViewUtils.newLine);

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

  public void setPeakList(ArrayList<PeakInfo> list) {
    peakList = list;
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
                                    HashMap<String, String> table,
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
      if (dataClass.equals("PEAKTABLE"))
        continuous = false;
      // check if there is an x and y factor
      if (xFactor != ERROR && yFactor != ERROR)
        xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, xFactor, yFactor);
      else
        xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, 1, 1);

      double fileDeltaX = JSpecViewUtils.deltaX(xyCoords[xyCoords.length - 1]
          .getXVal(), xyCoords[0].getXVal(), xyCoords.length);
      increasing = (fileDeltaX > 0);
      return true;
    }
    return false;
  }

  public String getTabularData(String label, String value)
      throws JSpecViewException {

    if (label.equals("##PEAKASSIGNMENTS"))
      setDataClass("PEAKASSIGNMENTS");
    else if (label.equals("##PEAKTABLE"))
      setDataClass("PEAKTABLE");
    else if (label.equals("##XYDATA"))
      setDataClass("XYDATA");
    else if (label.equals("##XYPOINTS"))
      setDataClass("XYPOINTS");

    // Get CoordData
    String tmp = value;
    try {
      char chr;
      do {
        tmp = tmp.substring(tmp.indexOf("\n") + 1);
        chr = tmp.trim().charAt(0);
      } while (!Character.isDigit(chr) && chr != '+' && chr != '-'
          && chr != '.');
    } catch (IndexOutOfBoundsException iobe) {
      throw new JSpecViewException("Error Reading Data Set");
    }
    return tmp;
  }

  public boolean createXYCoords(HashMap<String, ArrayList<String>> nTupleTable,
                                String[] plotSymbols, String dataType,
                                String tabularSpecData, int tabDataLineNo,
                                StringBuffer errorLog) {
    ArrayList<String> list;
    if (dataClass.equals("XYDATA")) {
      // Get Label Values

      list = (ArrayList<String>) nTupleTable.get("##SYMBOL");
      int index1 = list.indexOf(plotSymbols[0]);
      int index2 = list.indexOf(plotSymbols[1]);

      list = (ArrayList<String>) nTupleTable.get("##FACTOR");
      xFactor = Double.parseDouble((String) list.get(index1));
      yFactor = Double.parseDouble((String) list.get(index2));

      list = (ArrayList<String>) nTupleTable.get("##LAST");
      fileLastX = Double.parseDouble((String) list.get(index1));

      list = (ArrayList<String>) nTupleTable.get("##FIRST");
      fileFirstX = Double.parseDouble((String) list.get(index1));
      //firstY = Double.parseDouble((String)list.get(index2));

      list = (ArrayList<String>) nTupleTable.get("##VARDIM");
      nPointsFile = Integer.parseInt((String) list.get(index1));

      list = (ArrayList<String>) nTupleTable.get("##UNITS");
      xUnits = (String) list.get(index1);
      yUnits = (String) list.get(index2);

      decompressData(tabularSpecData, tabDataLineNo, errorLog);
      return true;
    }
    if (dataClass.equals("PEAKTABLE") || dataClass.equals("XYPOINTS")) {
      if (dataClass.equals("PEAKTABLE"))
        continuous = false;
      list = (ArrayList<String>) nTupleTable.get("##SYMBOL");
      int index1 = list.indexOf(plotSymbols[0]);
      int index2 = list.indexOf(plotSymbols[1]);

      list = (ArrayList<String>) nTupleTable.get("##UNITS");
      xUnits = (String) list.get(index1);
      yUnits = (String) list.get(index2);
      xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, xFactor, yFactor);
      return true;
    }
    return false;
  }


  private void decompressData(String tabularSpecData, int tabDataLineNo,
                              StringBuffer errorLog) {

    double fileDeltaX = JSpecViewUtils.deltaX(fileLastX, fileFirstX, nPointsFile);
    increasing = (fileDeltaX > 0);

    JDXDecompressor decompressor = new JDXDecompressor(tabularSpecData,
        xFactor, yFactor, fileDeltaX);
    decompressor.setLabelLineNo(tabDataLineNo);

    xyCoords = decompressor.decompressData();

    if (xyCoords == null)
      xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, xFactor, yFactor);

    if (decompressor.getErrorLog().length() > 0) {
      errorLog.append(decompressor.getErrorLog() + "\n");
      errorLog.append("firstX: "
          + fileFirstX
          + " Found "
          + (increasing ? xyCoords[0] : xyCoords[xyCoords.length - 1])
              .getXVal() + "\n");
      errorLog.append("lastX from Header "
          + fileLastX
          + " Found "
          + (increasing ? xyCoords[xyCoords.length - 1] : xyCoords[0])
              .getXVal() + "\n");
      errorLog.append("deltaX from Header " + fileDeltaX + "\n");
      errorLog.append("Number of points in Header " + nPointsFile + " Found "
          + xyCoords.length + "\n");
    } else {
      errorLog.append("No Errors\n");
    }

    if (JSpecViewUtils.DEBUG) {
      System.err.println(errorLog.toString());
    }

    // apply offset
    if (offset != ERROR && observedFreq != ERROR
        && dataType.toUpperCase().contains("SPECTRUM")) {
      JSpecViewUtils.applyShiftReference(xyCoords, dataPointNum, fileFirstX,
          fileLastX, offset, observedFreq, shiftRefType);
    }

    if (observedFreq != ERROR && xUnits.toUpperCase().equals("HZ")) {
      double xScale = observedFreq;
      JSpecViewUtils.applyScale(xyCoords, (1 / xScale), 1);
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
  private boolean continuous = true;


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


}
