package jspecview.common;

import jspecview.exception.JSpecViewException;

/**
 * could be a spectrum or a source
 * 
 * @author Bob Hanson
 * 
 */
public class JDXObject {

  public String title = "";
  public String jcampdx = "5.01";
  public String dataType = "";
  public String dataClass = "";
  public String origin = "";
  public String owner = "PUBLIC DOMAIN";
  public String longDate = "";
  public String date = "";
  public String time = "";
  public String pathlength = "";

  // --------------------Spectral Parameters ------------------------------//
  public double firstX = JDXSpectrum.ERROR;
  public double lastX = JDXSpectrum.ERROR;
  public int nPoints = -1;

  public String xUnits = "";
  public String yUnits = "";
  public double xFactor = JDXSpectrum.ERROR;
  public double yFactor = JDXSpectrum.ERROR;
  // For NMR Spectra
  public double observedFreq = JDXSpectrum.ERROR;
  public double offset = JDXSpectrum.ERROR;
  // Observed Frequency for NMR
  public double obFreq = Graph.ERROR;
  // Shift Reference for NMR
  // shiftRef = 0, bruker = 1, varian = 2
  public int shiftRefType = -1;
  public int dataPointNum = -1;
  // Variables needed to create JDXSpectrum
  public double deltaX = Graph.ERROR;
  public boolean increasing = true;
  public boolean continuous = true;

  /*---------------------SET CORE FIXED HEADER------------------------- */

  /**
   * Sets the title of the spectrum
   * 
   * @param title
   *        the spectrum title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Sets the JCAMP-DX version number
   * 
   * @param versionNum
   *        the JCAMP-DX version number
   */
  public void setJcampdx(String versionNum) {
    this.jcampdx = versionNum;
  }

  /**
   * Sets the data type
   * 
   * @param dataType
   *        the data type
   */
  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  /**
   * Sets the data class
   * 
   * @param dataClass
   *        the data class
   */
  public void setDataClass(String dataClass) {
    this.dataClass = dataClass;
  }

  /**
   * Sets the origin of the JCAMP-DX spectrum
   * 
   * @param origin
   *        the origin
   */
  public void setOrigin(String origin) {
    this.origin = origin;
  }

  /**
   * Sets the owner
   * 
   * @param owner
   *        the owner
   */
  public void setOwner(String owner) {
    this.owner = owner;
  }

  /**
   * Sets the long date of when the file was created
   * 
   * @param longDate
   *        String
   */
  public void setLongDate(String longDate) {
    this.longDate = longDate;
  }

  /**
   * Sets the date the file was created
   * 
   * @param date
   *        String
   */
  public void setDate(String date) {
    this.date = date;
  }

  /**
   * Sets the time the file was created
   * 
   * @param time
   *        String
   */
  public void setTime(String time) {
    this.time = time;
  }

  /**
   * Sets the pathlength of the sample (required for AnIML IR/UV files)
   * 
   * @param pathlength
   *        String
   */
  public void setPathlength(String pathlength) {
    this.pathlength = pathlength;
  }

  /* -------------------SET SPECTRAL PARAMETERS -------------------------- */

  /**
   * Sets the units for the x axis
   * 
   * @param xUnits
   *        the x units
   */
  public void setXUnits(String xUnits) {
    this.xUnits = xUnits;
  }

  /**
   * Sets the units for the y axis
   * 
   * @param yUnits
   *        the y units
   */
  public void setYUnits(String yUnits) {
    this.yUnits = yUnits;
  }

  /**
   * Sets the original xfactor
   * 
   * @param xFactor
   *        the x factor
   */
  public void setXFactor(double xFactor) {
    this.xFactor = xFactor;
  }

  /**
   * Sets the original y factor
   * 
   * @param yFactor
   *        the y factor
   */
  public void setYFactor(double yFactor) {
    this.yFactor = yFactor;
  }

  /**
   * Getter for property title.
   * 
   * @return Value of property title.
   */
  public String getTitle() {
    return title;
  }

  /**
   * Getter for property jcampdx.
   * 
   * @return Value of property jcampdx.
   */
  public String getJcampdx() {
    return jcampdx;
  }

  /**
   * Getter for property dataType.
   * 
   * @return Value of property dataType.
   */
  public String getDataType() {
    return dataType;
  }

  public int getNPoints() {
    return nPoints;
  }

  /**
   * Getter for property origin.
   * 
   * @return Value of property origin.
   */
  public String getOrigin() {
    return origin;
  }

  /**
   * Getter for property owner.
   * 
   * @return Value of property owner.
   */
  public String getOwner() {
    return owner;
  }

  /**
   * Getter for property longDate.
   * 
   * @return Value of property longDate.
   */
  protected String getLongDate() {
    return longDate;
  }

  /**
   * Getter for property date.
   * 
   * @return Value of property date.
   */
  protected String getDate() {
    return date;
  }

  /**
   * Getter for property time.
   * 
   * @return Value of property time.
   */
  protected String getTime() {
    return time;
  }

  /**
   * Getter for pathlength.
   * 
   * @return Value of pathlength.
   */
  protected String getPathlength() {
    return pathlength;
  }

  public void checkRequiredTokens() throws JSpecViewException {
    if (xFactor == Graph.ERROR)
      throw new JSpecViewException(
          "Error Reading Data Set: ##XFACTOR not found");

    if (yFactor == Graph.ERROR)
      throw new JSpecViewException(
          "Error Reading Data Set: ##YFACTOR not found");

    if (firstX == Graph.ERROR)
      throw new JSpecViewException("Error Reading Data Set: ##FIRST not found");

    if (lastX == Graph.ERROR)
      throw new JSpecViewException("Error Reading Data Set: ##LASTX not found");

    if (nPoints == -1)
      throw new JSpecViewException(
          "Error Reading Data Set: ##NPOINTS not found");

  }
}
