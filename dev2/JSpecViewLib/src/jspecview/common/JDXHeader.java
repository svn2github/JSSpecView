package jspecview.common;

/**
 * could be a spectrum or a source
 * 
 * @author Bob Hanson
 * 
 */
public class JDXHeader {

  public String title = "";
  public String jcampdx = "5.01";
  public String dataType = "";
  public String dataClass = "";
  public String origin = "";
  public String owner = "PUBLIC DOMAIN";
  public String longDate = "";
  public String date = "";
  public String time = "";


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
  public String getLongDate() {
    return longDate;
  }

  /**
   * Getter for property date.
   * 
   * @return Value of property date.
   */
  public String getDate() {
    return date;
  }

  /**
   * Getter for property time.
   * 
   * @return Value of property time.
   */
  public String getTime() {
    return time;
  }

  /**
   * Returns the data class
   * 
   * @return the data class
   */
  public String getDataClass() {
    return dataClass;
  }

}
