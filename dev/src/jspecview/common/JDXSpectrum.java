/* Copyright (c) 2002-2007 The University of the West Indies
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import jspecview.util.Coordinate;
import jspecview.util.JDXCompressor;
import jspecview.util.JSpecViewUtils;

/**
 * <code>JDXSpectrum</code> implements the Interface Spectrum for the display of JDX Files.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class JDXSpectrum implements Graph{
  //private JDXSource parentSource;
  /**
   * HashMap of optional header values
   */
  private HashMap headerTable;

  /**
   * Vector of x,y coordinates
   */
  private Coordinate[] xyCoords;

  /**
   * specifies whether the spectrum is increasing
   */
  private boolean increasing;

  /**
   * specifies whether the spectrum is continuous
   */
  private boolean continuous;


  /**
   * whether the x values were converted from HZ to PPM
   */
  private boolean isHZtoPPM = false;

   // -----------------------Core Fixed Header ----------------------------//

  private String title = "";
  private String jcampdx = "5.01";
  private String dataType = "";
  private String dataClass = "";
  private String origin = "";
  private String owner = "PUBLIC DOMAIN";
  private String longDate = "";
  private String date = "";
  private String time = "";
  private String pathlength = "";

  private Calendar now = Calendar.getInstance();
  SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSS ZZZZ");
  private String currentTime =  formatter.format(now.getTime());
  private String finalDate = "";

  // --------------------Spectral Parameters ------------------------------//
  private String xUnits = "Arbitrary Units";
  private String yUnits = "Arbitrary Units";
  private double xFactor = ERROR;
  private double yFactor = ERROR;
  //private double deltaX = ERROR;
  // For NMR Spectra
  private double observedFreq = ERROR;

  // -------Compression Types -----------------//
  private final int DIF = 0;
  private final int FIX = 1;
  private final int SQZ = 2;
  private final int PAC = 3;
  private final int XY  = 4;


  /**
   * Constructor
   */
  public JDXSpectrum(){
    headerTable = new HashMap();
    xyCoords = new Coordinate[0];
  }

  /**
   * Sets the header table
   * @param table a map of header labels and corresponding datasets
   */
  public void setHeaderTable(HashMap table){
    headerTable = table;
  }

  /**
   * Sets the array of coordinates
   * @param coords the array of Coordinates
   */
  public void setXYCoords(Coordinate[] coords){
    xyCoords = coords;
  }

  /**
   * Returns the table of headers
   * @return the table of headers
   */
  public HashMap getHeaderTable(){
    return headerTable;
  }

  /**
   * Returns the array of coordinates
   * @return the array of coordinates
   */
  public Coordinate[] getXYCoords(){
    return xyCoords;
  }

  /**
   * Return the parent Source of this Spectrum
   * @return the parent Source of this Spectrum
   */
 /* public JDXSource getParentSource(){
    return parentSource;
  }*/

  /**
   * Determines if the spectrum should be displayed with abscissa unit
   * of Part Per Million (PPM) instead of Hertz (HZ)
   * @return true if abscissa unit should be PPM
   */
  public boolean isHZtoPPM(){
    return isHZtoPPM;
  }

  /**
   * Sets the value to true if the spectrum should be displayed with abscissa unit
   * of Part Per Million (PPM) instead of Hertz (HZ)
   * @param val true or false
   */
  public void setHZtoPPM(boolean val){
    isHZtoPPM = val;
  }


  /**
   * Sets value to true if spectrum is increasing
   * @param val true if spectrum is increasing
   */
  public void setIncreasing(boolean val){
    increasing = val;
  }

  /**
   * Sets value to true if spectrum is continuous
   * @param val true if spectrum is continuous
   */
  public void setContinuous(boolean val){
    continuous = val;
  }


  /**
   * Sets the parent source of this spectrum
   * @param source the parent source
   */
  /*public void setParentSource(JDXSource source){
    parentSource = source;
  }*/


  /*---------------------SET CORE FIXED HEADER------------------------- */

  /**
   * Sets the title of the spectrum
   * @param title the spectrum title
   */
  public void setTitle(String title) {
      this.title = title;
  }

  /**
   * Sets the JCAMP-DX version number
   * @param versionNum the JCAMP-DX version number
   */
  public void setJcampdx(String versionNum) {
      this.jcampdx = versionNum;
  }

  /**
   * Sets the data type
   * @param dataType the data type
   */
  public void setDataType(String dataType) {
      this.dataType = dataType;
  }

  /**
   * Sets the data class
   * @param dataClass the data class
   */
  public void setDataClass(String dataClass) {
      this.dataClass = dataClass;
  }

  /**
   * Sets the origin of the JCAMP-DX spectrum
   * @param origin the origin
   */
  public void setOrigin(String origin) {
      this.origin = origin;
  }

  /**
   * Sets the owner
   * @param owner the owner
   */
  public void setOwner(String owner) {
      this.owner = owner;
  }

  /**
   * Sets the long date of when the file was created
   * @param longDate String
   */
  public void setLongDate(String longDate) {
    this.longDate = longDate;
  }

  /**
   * Sets the date the file was created
   * @param date String
   */
  public void setDate(String date) {
    this.date = date;
  }

  /**
   * Sets the time the file was created
   * @param time String
   */
  public void setTime(String time) {
    this.time = time;
  }

  /**
   * Sets the pathlength of the sample (required for AnIML IR/UV files)
   * @param pathlength String
   */
  public void setPathlength(String pathlength) {
    this.pathlength = pathlength;
  }



  /* ------------------------------------------------------------------- */


  /* -------------------SET SPECTRAL PARAMETERS -------------------------- */

  /**
   * Sets the units for the x axis
   * @param xUnits the x units
   */
  public void setXUnits(String xUnits) {
      this.xUnits = xUnits;
  }

  /**
   * Sets the units for the y axis
   * @param yUnits the y units
   */
  public void setYUnits(String yUnits) {
      this.yUnits = yUnits;
  }

  /**
   * Sets the original xfactor
   * @param xFactor the x factor
   */
  public void setXFactor(double xFactor){
    this.xFactor = xFactor;
  }

  /**
   * Sets the original y factor
   * @param yFactor the y factor
   */
  public void setYFactor(double yFactor){
    this.yFactor = yFactor;
  }

  /* --------------------------------------------------------------------- */


  /**
   * Sets the Observed Frequency (for NMR Spectra)
   * @param observedFreq the observed frequency
   */
  public void setObservedFreq(double observedFreq){
    this.observedFreq = observedFreq;
  }

   /**
   * Returns true if the spectrum is increasing
   * @return true if the spectrum is increasing
   */
  public boolean isIncreasing(){
    return increasing;
  }

 /**
  * Returns true if spectrum is continuous
  * @return true if spectrum is continuous
  */
  public boolean isContinuous(){
    return continuous;
  }


  /* --------------- GET CORE FIXED HEADER ----------------------------- */

  /**
   * Returns the title of the spectrum
   * @return the title of the spectrum
   */
  public String getTitle() {
      return title;
  }

  /**
   * Returns the JCAMP-DX version
   * @return the JCAMP-DX version
   */
  public String getJcampdx() {
      return jcampdx;
  }

  /**
   * Returns the data type of the spectrum
   * @return the data type of the spectrum
   */
  public String getDataType() {
      return dataType;
  }

  /**
   * Returns the data class
   * @return the data class
   */
  public String getDataClass() {
      return dataClass;
  }

  /**
   * Returns the origin of the spectrum
   * @return the origin of the spectrum
   */
  public String getOrigin() {
      return origin;
  }

  /**
   * Returns the owner of the spectrum
   * @return the owner of the spectrum
   */
  public String getOwner() {
      return owner;
  }

  /**
   * Returns the long date first created
   * @return the long date first created
   */
  public String getLongDate() {
    return longDate;
  }

  /**
   * Returns the date first created
   * @return the date first created
   */
  public String getDate() {
    return date;
  }

  /**
   * Returns the time first created
   * @return the time first created
   */
  public String getTime() {
  return time;
  }

  /**
   * Returns the pathlength of the sample (required for AnIML IR/UV files)
   * @return the pathlength
   */
  public String getPathlength() {
    return pathlength;
  }



  /*   ------------------------------------------------------------- */


 /*  ------------------- GET SPECTRAL PARAMETERS --------------------- */

  /**
   * Returns the units for x-axis when spectrum is displayed
   * @return the units for x-axis when spectrum is displayed
   */
  public String getXUnits() {
      return xUnits;
  }

  /**
   * Returns the units for y-axis when spectrum is displayed
   * @return the units for y-axis when spectrum is displayed
   */
  public String getYUnits() {
      return yUnits;
  }

  /**
   * Returns the first X value
   * @return the first X value
   */
  public double getFirstX() {
    //if(isIncreasing())
      return xyCoords[0].getXVal();
    //else
    //  return xyCoords[getNumberOfPoints() - 1].getXVal();
  }

  /**
   * Returns the first Y value
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
   * @return the last X value
   */
  public double getLastX(){
   // if(isIncreasing())
      return xyCoords[getNumberOfPoints() -1 ].getXVal();
   // else
   //   return xyCoords[0].getXVal();
  }

  /**
   * Returns the last Y value
   * @return the last Y value
   */
  public double getLastY() {
      return xyCoords[getNumberOfPoints() - 1].getYVal();
  }

  /**
   * Returns the number of points
   * @return the number of points
   */
  public int getNumberOfPoints() {
      return xyCoords.length;
  }

  /**
   * Calculates and returns the minimum x value in the list of coordinates
   * Fairly expensive operation
   * @return the minimum x value in the list of coordinates
   */
  public double getMinX(){
    return JSpecViewUtils.getMinX(xyCoords);
  }

  /**
   * Calculates and returns the minimum y value in the list of coordinates
   * Fairly expensive operation
   * @return the minimum x value in the list of coordinates
   */
  public double getMinY(){
    return JSpecViewUtils.getMinY(xyCoords);
  }

  /**
   * Calculates and returns the maximum x value in the list of coordinates
   * Fairly expensive operation
   * @return the maximum x value in the list of coordinates
   */
  public double getMaxX(){
    return JSpecViewUtils.getMaxX(xyCoords);
  }

  /**
   * Calculates and returns the maximum y value in the list of coordinates
   * Fairly expensive operation
   * @return the maximum y value in the list of coordinates
   */
  public double getMaxY(){
    return JSpecViewUtils.getMaxX(xyCoords);
  }

  /**
   * Returns the original x factor
   * @return the original x factor
   */
  public double getXFactor(){
    return xFactor;
  }

  /**
   * Returns the original y factor
   * @return the original y factor
   */
  public double getYFactor(){
    return yFactor;
  }

  /**
   * Returns the delta X
   * @return the delta X
   */
  public double getDeltaX(){
    return JSpecViewUtils.deltaX(getLastX(), getFirstX(), getNumberOfPoints());
  }



  /**
   * Returns the observed frequency (for NMR Spectra)
   * @return the observed frequency (for NMR Spectra)
   */
  public double getObservedFreq(){
    return observedFreq;
  }

  // ***************************** To String Methods ****************************

  /**
   * Returns the String for the header of the spectrum
   * @param tmpDataClass the dataclass
   * @param tmpXFactor the x factor
   * @param tmpYFactor the y factor
   * @param startIndex the index of the starting coordinate
   * @param endIndex the index of the ending coordinate
   * @return the String for the header of the spectrum
   */
  public String getHeaderString(String tmpDataClass, double tmpXFactor, double tmpYFactor, int startIndex, int endIndex){

    final String CORE_STR = "TITLE,ORIGIN,OWNER,DATE,TIME,DATATYPE,JCAMPDX";

    DecimalFormat varFormatter = new DecimalFormat("0.########", new DecimalFormatSymbols(java.util.Locale.US ));
    DecimalFormat sciFormatter = new DecimalFormat("0.########E0", new DecimalFormatSymbols(java.util.Locale.US ));

    StringBuffer buffer = new StringBuffer();
    String longdate="";
    // start of header
    buffer.append("##TITLE= " + getTitle() + JSpecViewUtils.newLine);
    buffer.append("##JCAMP-DX= 5.01" /*+ getJcampdx()*/ + JSpecViewUtils.newLine);
    buffer.append("##DATA TYPE= " + getDataType() + JSpecViewUtils.newLine);
    buffer.append("##DATA CLASS= " + tmpDataClass + JSpecViewUtils.newLine);
    buffer.append("##ORIGIN= " + getOrigin() + JSpecViewUtils.newLine);
    buffer.append("##OWNER= " + getOwner() + JSpecViewUtils.newLine);

    if ((getLongDate().equals("")) || (getDate().length() != 8) )
      longdate = currentTime + " $$ export date from JSpecView";

      // give a 50 year window
      // Y2K compliant
     if (getDate().length() == 8)  {
           if (getDate().charAt(0) < '5')
             longdate = "20" + getDate() + " " + getTime();
           else
             longdate = "19" + getDate() + " " + getTime();
     }
     if (!getLongDate().equals(""))
        longdate=getLongDate();

      buffer.append("##LONGDATE= " + longdate + JSpecViewUtils.newLine);

    // optional header
    for(Iterator iter = headerTable.keySet().iterator(); iter.hasNext();){
      String label = (String)iter.next();
      //System.out.println(label);
      String dataSet = (String)headerTable.get(label);
      buffer.append(label + "= " + dataSet + JSpecViewUtils.newLine);
    }
    if(getObservedFreq() != ERROR)
      buffer.append("##.OBSERVE FREQUENCY= " + getObservedFreq() + JSpecViewUtils.newLine);
    //now need to put pathlength here

    // last part of header

    if((getObservedFreq() != ERROR) && !(getDataType().toUpperCase().contains("FID")))
      buffer.append("##XUNITS= HZ" + JSpecViewUtils.newLine);
    else
      buffer.append("##XUNITS= " + getXUnits() + JSpecViewUtils.newLine);

    buffer.append("##YUNITS= " + getYUnits() + JSpecViewUtils.newLine);
    buffer.append("##XFACTOR= " + sciFormatter.format(tmpXFactor) + JSpecViewUtils.newLine);
    buffer.append("##YFACTOR= " + sciFormatter.format(tmpYFactor) + JSpecViewUtils.newLine);
    if(getObservedFreq() != ERROR)
      buffer.append("##FIRSTX= " + varFormatter.format(xyCoords[startIndex].getXVal() *getObservedFreq()) + JSpecViewUtils.newLine);
    else
      buffer.append("##FIRSTX= " + varFormatter.format(xyCoords[startIndex].getXVal()) + JSpecViewUtils.newLine);
    buffer.append("##FIRSTY= "+ varFormatter.format(xyCoords[startIndex].getYVal()) + JSpecViewUtils.newLine);
    if(getObservedFreq() != ERROR)
      buffer.append("##LASTX= " + varFormatter.format(xyCoords[endIndex].getXVal()*getObservedFreq()) + JSpecViewUtils.newLine);
    else
      buffer.append("##LASTX= " + varFormatter.format(xyCoords[endIndex].getXVal()) + JSpecViewUtils.newLine);
    buffer.append("##NPOINTS= " + (endIndex - startIndex + 1) + JSpecViewUtils.newLine);

    return buffer.toString();
  }

  /**
   * Returns the spectrum as a string with the data table in DIF compressed format
   * @return the spectrum as a string with the data table in DIF compressed format
   */
  public String toDIF(){
    return toStringAux(DIF);
  }

  /**
   * Returns the spectrum as a string with the data table in DIF compressed format
   * from startIndex to endIndex
   * @param startIndex the start index
   * @param endIndex the end index
   * @return the spectrum as a string with the data table in DIF compressed format
   */
  public String toDIF(int startIndex, int endIndex){
    return toStringAux(DIF, startIndex, endIndex);
  }

  /**
   * Returns the spectrum as a string with the data table in FIX compressed format
   * @return the spectrum as a string with the data table in FIX compressed format
   */
  public String toFIX(){
    return toStringAux(FIX);
  }

  /**
    * Returns the spectrum as a string with the data table in FIX compressed format
    * from startIndex to endIndex
    * @param startIndex the start index
    * @param endIndex the end index
    * @return the spectrum as a string with the data table in FIX compressed format
   */
  public String toFIX(int startIndex, int endIndex){
    return toStringAux(FIX, startIndex, endIndex);
  }


  /**
   * Returns the spectrum as a string with the data table in PAC compressed format
   * @return the spectrum as a string with the data table in PAC compressed format
   */
  public String toPAC(){
    return toStringAux(PAC);
  }

  /**
   * Returns the spectrum as a string with the data table in PAC compressed format
   * from startIndex to endIndex
   * @param startIndex the start index
   * @param endIndex the end index
   * @return the spectrum as a string with the data table in PAC compressed format
   */
  public String toPAC(int startIndex, int endIndex){
    return toStringAux(PAC, startIndex, endIndex);
  }

  /**
   * Returns the spectrum as a string with the data table in SQZ compressed format
   * @return the spectrum as a string with the data table in SQZ compressed format
   */
  public String toSQZ(){
    return toStringAux(SQZ);
  }


  /**
   * Returns the spectrum as a string with the data table in SQZ compressed format
   * from startIndex to endIndex
   * @param startIndex the start index
   * @param endIndex the end index
   * @return the spectrum as a string with the data table in SQZ compressed format
   */
  public String toSQZ(int startIndex, int endIndex){
    return toStringAux(SQZ, startIndex, endIndex);
  }

  /**
   * Returns the spectrum as a string with the data table un-compressed
   * @return the spectrum as a string with the data table un-compressed
   */
  public String toXY(){
    return toStringAux(XY);
  }


  /**
   * Returns the spectrum as a string with the data table in X,Y compressed format
   * from startIndex to endIndex
   * @param startIndex the start index
   * @param endIndex the end index
   * @return the spectrum as a string with the data table in X,Y compressed format
   */
  public String toXY(int startIndex, int endIndex){
    return toStringAux(XY, startIndex, endIndex);
  }

  /**
   * Auxiliary method to convert the data table in a format specified by
   * type
   * @param type the data table format
   * @return A string representation of the data table
   */
  private String toStringAux(int type){
    return toStringAux(type, 0, xyCoords.length - 1);
  }

  /**
   * Auxiliary function for the toString functions
   * @param type the type of compression
   * @param startIndex the start Coordinate Index
   * @param endIndex the end Coordinate Index
   * @return the spectrum string for the type of compression specified by <code>type</code>
   */
  private String toStringAux(int type, int startIndex, int endIndex){
    StringBuffer buffer = new StringBuffer();
    Coordinate[] newXYCoords = new Coordinate[xyCoords.length];
    String tabDataSet = "", tmpDataClass = "XYDATA";
    double xCompFactor, yCompFactor;

    if (isHZtoPPM){
      newXYCoords = JSpecViewUtils.copyCoordinateArray(xyCoords);
      JSpecViewUtils.applyScale(newXYCoords, getObservedFreq(), 1);
//       JSpecViewUtils.applyScale(newXYCoords, 1, 1);
    }
    else
      newXYCoords = getXYCoords();

    if(type != XY){
      xCompFactor = JSpecViewUtils.getXFactorForCompression(newXYCoords, startIndex, endIndex);
      yCompFactor = JSpecViewUtils.getYFactorForCompression(newXYCoords, startIndex, endIndex);
    }
    else{
      xCompFactor = yCompFactor = 1;
      tmpDataClass = "XYPOINTS";
    }

    switch(type){
      case DIF:  tabDataSet = JDXCompressor.compressDIF(newXYCoords, startIndex, endIndex, xCompFactor, yCompFactor);
                    break;
      case FIX:  tabDataSet = JDXCompressor.compressFIX(newXYCoords, startIndex, endIndex, xCompFactor, yCompFactor);
                    break;
      case PAC:  tabDataSet = JDXCompressor.compressPAC(newXYCoords, startIndex, endIndex, xCompFactor, yCompFactor);
                    break;
      case SQZ:  tabDataSet = JDXCompressor.compressSQZ(newXYCoords, startIndex, endIndex, xCompFactor, yCompFactor);
                    break;
      case XY:   tabDataSet = JSpecViewUtils.coordinatesToString(newXYCoords, startIndex, endIndex, 1);
                    break;
    }

    int index = Arrays.binarySearch(JDXSource.VAR_LIST_TABLE[0], tmpDataClass);
    String varList = JDXSource.VAR_LIST_TABLE[1][index];

    buffer.append(getHeaderString(tmpDataClass, xCompFactor, yCompFactor, startIndex, endIndex));
    buffer.append("##" + tmpDataClass + "= " + varList + JSpecViewUtils.newLine);
    buffer.append(tabDataSet);
    buffer.append("##END=");

    return buffer.toString();
  }

  /**
   * Returns a copy of this <code>JDXSpectrum</code>
   * @return a copy of this <code>JDXSpectrum</code>
   */
  public JDXSpectrum copy(){
    JDXSpectrum newSpectrum = new JDXSpectrum();

    newSpectrum.setContinuous(isContinuous());
    newSpectrum.setDataClass(getDataClass());
    newSpectrum.setDataType(getDataType());
    newSpectrum.setHeaderTable(getHeaderTable());
    newSpectrum.setHZtoPPM(isHZtoPPM());
    newSpectrum.setIncreasing(isIncreasing());
    newSpectrum.setJcampdx(getJcampdx());
    newSpectrum.setObservedFreq(getObservedFreq());
    newSpectrum.setOrigin(getOrigin());
    newSpectrum.setOwner(getOwner());
    newSpectrum.setTitle(getTitle());
    newSpectrum.setXFactor(getXFactor());
    newSpectrum.setXUnits(getXUnits());
    newSpectrum.setXYCoords(getXYCoords());
    newSpectrum.setYFactor(getYFactor());
    newSpectrum.setYUnits(getYUnits());
    newSpectrum.setPathlength(getPathlength());

    return newSpectrum;
  }
}
