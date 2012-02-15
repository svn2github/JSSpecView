package jspecview.common;

import java.util.HashMap;
import java.util.Map;

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
   * spectrum only
   * @return the data class
   */
  public String getDataClass() {
    return dataClass;
  }

  // Table of header variables specific to the jdx source or spectrum
  protected Map<String, String> headerTable = new HashMap<String, String>();
  
  /**
   * Sets the headerTable for this Source or spectrum
   * 
   * @param table
   *        the header table
   */
  public void setHeaderTable(Map<String, String> table) {
    headerTable = table;
  }

  /**
   * Returns the table of headers
   * 
   * @return the table of headers
   */
  public Map<String, String> getHeaderTable() {
    return headerTable;
  }

  public Object[][] getHeaderRowDataAsArray(boolean addDataClass, int nMore) {
    
    Object[] headerLabels = headerTable.keySet().toArray();
    Object[] headerValues = headerTable.values().toArray();
    
    Object rowData[][] = new Object[(addDataClass ? 6 : 5) + headerLabels.length + nMore][];

    int i = 0;
    rowData[i++] = new Object[] { "##TITLE", title };
    rowData[i++] = new Object[] { "##JCAMP-DX", jcampdx };
    rowData[i++] = new Object[] { "##DATA TYPE", dataType };
    if (addDataClass)
      rowData[i++] = new Object[] { "##DATA CLASS", dataClass };      
    rowData[i++] = new Object[] { "##ORIGIN", origin };
    rowData[i++] = new Object[] { "##OWNER", owner };
    
    for(int j = 0; j < headerLabels.length; j++)
      rowData[i++] = new Object[] { headerLabels[j], headerValues[j] };
    
    return rowData;
  }

  
  
}