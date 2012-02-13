package jspecview.common;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import jspecview.exception.JSpecViewException;
import jspecview.util.Parser;

/**
 * could be a spectrum or a source
 * 
 * @author Bob Hanson
 *
 */
public class JDXObject {

  protected String title = "";
  protected String jcampdx = "5.01";
  protected String dataType = "";
  protected String dataClass = "";
  protected String origin = "";
  protected String owner = "PUBLIC DOMAIN";
  protected String longDate = "";
  protected String date = "";
  protected String time = "";
  protected String pathlength = "";

  // --------------------Spectral Parameters ------------------------------//
  protected double firstX = JDXSpectrum.ERROR;
  protected double lastX = JDXSpectrum.ERROR;
  protected int nPoints = -1;
    
  protected String xUnits = "";
  protected String yUnits = "";
  protected double xFactor = JDXSpectrum.ERROR;
  protected double yFactor = JDXSpectrum.ERROR;
  // For NMR Spectra
  protected double observedFreq = JDXSpectrum.ERROR;
  protected double offset = JDXSpectrum.ERROR;
  // Observed Frequency for NMR
  protected double obFreq = Graph.ERROR;
  // Shift Reference for NMR
  // shiftRef = 0, bruker = 1, varian = 2
  protected int shiftRefType = -1;
  protected int dataPointNum = -1;
  // Variables needed to create JDXSpectrum
  protected double deltaX = Graph.ERROR;
  protected boolean increasing = true;
  protected boolean continuous = true;
  
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

  /** Getter for property title.
   * @return Value of property title.
   */
  public String getTitle() {
      return title;
  }

  /** Getter for property jcampdx.
   * @return Value of property jcampdx.
   */
  public String getJcampdx() {
      return jcampdx;
  }

  /** Getter for property dataType.
   * @return Value of property dataType.
   */
  public String getDataType() {
      return dataType;
  }

  public int getNPoints() {
    return nPoints;
  }
  /** Getter for property origin.
   * @return Value of property origin.
   */
  public String getOrigin() {
      return origin;
  }

  /** Getter for property owner.
   * @return Value of property owner.
   */
  public String getOwner() {
      return owner;
  }

  /** Getter for property longDate.
    * @return Value of property longDate.
   */
  protected String getLongDate() {
    return longDate;
  }

  /** Getter for property date.
  * @return Value of property date.
  */
 protected String getDate() {
   return date;
 }

 /** Getter for property time.
  * @return Value of property time.
  */
 protected String getTime() {
   return time;
 }

 /** Getter for pathlength.
  * @return Value of pathlength.
  */
 protected String getPathlength() {
   return pathlength;
 }

  protected static boolean checkCommon(JDXObject jdxObject, String label,
                                       String value, StringBuffer errorLog, 
                                       HashMap<String, String> table) {
    if (label.equals("##TITLE")) {
      jdxObject.setTitle(JSpecViewUtils.obscure || value == null
          || value.equals("") ? "Unknown" : value);
      return true;
    }
    if (label.equals("##JCAMPDX")) {
      jdxObject.setJcampdx(value);
      float version = Parser.parseFloat(value);
      if (version >= 6.0 || Float.isNaN(version)) {
        if (errorLog != null)
          errorLog
              .append("Warning: JCAMP-DX version may not be fully supported: "
                  + value + "\n");
      }
      return true;
    }

    if (label.equals("##ORIGIN")) {
      if (value != null && !value.equals(""))
        jdxObject.setOrigin(value);
      else
        jdxObject.setOrigin("Unknown");
      return true;
    }

    if (label.equals("##OWNER")) {
      if (value != null && !value.equals(""))
        jdxObject.setOwner(value);
      else
        jdxObject.setOwner("Unknown");
      return true;
    }

    if (label.equals("##DATATYPE")) {
      jdxObject.setDataType(value);
      return true;
    }

    if (label.equals("##LONGDATE")) {
      jdxObject.setLongDate(value);
      //longDateFound = true;
      return true;
    }

    if (label.equals("##DATE")) {
      //          notesLDRTable.put(label, value);
      jdxObject.setDate(value);
      return true;
    }

    if (label.equals("##TIME")) {
      //          notesLDRTable.put(label, value);
      jdxObject.setTime(value);
      return true;
    }

    if (label.equals("##PATHLENGTH")) {
      jdxObject.setPathlength(value);
      return true;
    }

    if(label.equals("##XLABEL")){
      jdxObject.setXUnits(value);
      return true;
    }

    if(label.equals("##XUNITS") && jdxObject.xUnits.equals("")){
      jdxObject.setXUnits(value != null && !value.equals("") ? value : "Arbitrary Units");
      return true;
    }

    if(label.equals("##YLABEL")){
      jdxObject.setYUnits(value);
      return true;
    }

    if(label.equals("##YUNITS") && jdxObject.yUnits.equals("")){
      jdxObject.setYUnits(value != null && !value.equals("") ? value : "Arbitrary Units");
      return true;
    }

    if(label.equals("##XFACTOR")){
      jdxObject.setXFactor(Double.parseDouble(value));
      return true;
    }

    if(label.equals("##YFACTOR")){
      jdxObject.setYFactor(Double.parseDouble(value));
      return true;
    }

    if (label.equals("##FIRSTX")) {
      jdxObject.firstX = Double.parseDouble(value);
      //spectrum.setFirstX(firstX);
      return true;
    }

    if (label.equals("##LASTX")) {
      jdxObject.lastX = Double.parseDouble(value);
      //spectrum.setLastX(lastX);
      return true;
    }

    if (label.equals("##NPOINTS")) {
      jdxObject.nPoints = Integer.parseInt(value);
      return true;
    }

    if(label.equals("##MINX") ||
        label.equals("##MINY") ||
        label.equals("##MAXX") ||
        label.equals("##MAXY") ||
        label.equals("##FIRSTY")||
        label.equals("##DELTAX") ||
        label.equals("##DATACLASS"))
        return true;

    if (label.equals("##$OFFSET") && jdxObject.shiftRefType != 0) {
      jdxObject.offset = Double.parseDouble(value);
      // bruker doesn't need dataPointNum
      jdxObject.dataPointNum = 1;
      // bruker type
      jdxObject.shiftRefType = 1;
      return true;
    }

    if ((label.equals("##$REFERENCEPOINT")) && (jdxObject.shiftRefType != 0)) {
      jdxObject.offset = Double.parseDouble(value);
      // varian doesn't need dataPointNum
      jdxObject.dataPointNum = 1;
      // varian type
      jdxObject.shiftRefType = 2;
    }

    else if (label.equals("##.SHIFTREFERENCE")) {
      if (!(jdxObject.dataType.toUpperCase().contains("SPECTRUM")))
        return true;
      StringTokenizer srt = new StringTokenizer(value, ",");
      if (srt.countTokens() != 4)
        return true;
      try {
        srt.nextToken();
        srt.nextToken();
        jdxObject.dataPointNum = Integer.parseInt(srt.nextToken().trim());
        jdxObject.offset = Double.parseDouble(srt.nextToken().trim());
      } catch (NumberFormatException nfe) {
        return true;
      } catch (NoSuchElementException nsee) {
        return true;
      }

      if (jdxObject.dataPointNum <= 0)
        jdxObject.dataPointNum = 1;
      jdxObject.shiftRefType = 0;
      return true;
    }

    if (label.equals("##.OBSERVEFREQUENCY")) {
      jdxObject.obFreq = Double.parseDouble(value);
      table.put(label, value);
      return true;
    }
    
    return false;
  }

  public void checkRequiredTokens() throws JSpecViewException {
    if (xFactor == Graph.ERROR)
      throw new JSpecViewException(
          "Error Reading Data Set: ##XFACTOR not found");

    if (yFactor == Graph.ERROR)
      throw new JSpecViewException(
          "Error Reading Data Set: ##YFACTOR not found");

    if (firstX == Graph.ERROR)
      throw new JSpecViewException(
          "Error Reading Data Set: ##FIRST not found");

    if (lastX == Graph.ERROR)
      throw new JSpecViewException(
          "Error Reading Data Set: ##LASTX not found");

    if (nPoints == -1)
      throw new JSpecViewException(
          "Error Reading Data Set: ##NPOINTS not found");

  }
}
