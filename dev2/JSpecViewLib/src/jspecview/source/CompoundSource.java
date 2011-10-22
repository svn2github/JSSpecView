/* Copyright (c) 2002-2009 The University of the West Indies
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

package jspecview.source;


/**
 * Abstract base class form which JCAMP-DX Compound Source are extended.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 * @see jspecview.source.BlockSource
 * @see jspecview.source.NTupleSource
 */
public abstract class CompoundSource extends JDXSource {

  /** Holds value of property title. */
  private String title;

  /** Holds value of property jcampdx. */
  private String jcampdx;

  /** Holds value of property dataType. */
  private String dataType;

  /** Holds value of property origin. */
  private String origin;

  /** Holds value of property owner. */
  private String owner;

  /** Holds the value of property longDate. */
  private String longDate;

  /** Holds the value of property date. */
  private String date;

  /** Holds the value of property time. */
  private String time;

  /** Holds the value of the pathlength */
  private String pathlength;
  /**
   * Constructor
   */
  public CompoundSource() {
  }

  /** Getter for property title.
   * @return Value of property title.
   */
  public String getTitle() {
      return this.title;
  }

  /** Setter for property title.
   * @param title New value of property title.
   */
  protected void setTitle(String title) {
      this.title = title;
  }

  /** Getter for property jcampdx.
   * @return Value of property jcampdx.
   */
  public String getJcampdx() {
      return this.jcampdx;
  }

  /** Setter for property jcampdx.
   * @param jcampdx New value of property jcampdx.
   */
  protected void setJcampdx(String jcampdx) {
      this.jcampdx = jcampdx;
  }

  /** Getter for property dataType.
   * @return Value of property dataType.
   */
  public String getDataType() {
      return this.dataType;
  }

  /** Setter for property dataType.
   * @param dataType New value of property dataType.
   */
  protected void setDataType(String dataType) {
      this.dataType = dataType;
  }

  /** Getter for property origin.
   * @return Value of property origin.
   */
  public String getOrigin() {
      return this.origin;
  }

  /** Setter for property origin.
   * @param origin New value of property origin.
   */
  protected void setOrigin(String origin) {
      this.origin = origin;
  }

  /** Getter for property owner.
   * @return Value of property owner.
   */
  public String getOwner() {
      return this.owner;
  }

  /** Setter for property owner.
   * @param owner New value of property owner.
   */
  protected void setOwner(String owner) {
      this.owner = owner;
  }

  /** Getter for property longDate.
    * @return Value of property longDate.
   */
  protected String getLongDate() {
    return longDate;
  }

  /** Setter for property longDate.
  * @param longDate New value of property longDate.
  */
  protected void setLongDate(String longDate) {
    this.longDate = longDate;
  }

  /** Getter for property date.
  * @return Value of property date.
  */
 protected String getDate() {
   return date;
 }

 /** Setter for property date.
  * @param date New value of property date.
  */
 protected void setDate(String date) {
   this.date = date;
 }

 /** Getter for property time.
  * @return Value of property time.
  */
 protected String getTime() {
   return time;
 }

 /** Setter for property time.
  * @param time New value of property time.
  */
 protected void setTime(String time) {
   this.time = time;
 }

 /** Getter for pathlength.
  * @return Value of pathlength.
  */
 protected String getPathlength() {
   return pathlength;
 }

 /** Setter for pathlength
  * @param pathlength New value of pathlength.
  */
 protected void setPathlength(String pathlength) {
   this.pathlength = pathlength;
 }

}
