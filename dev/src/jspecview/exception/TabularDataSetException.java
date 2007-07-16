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

package jspecview.exception;

/**
 * Exception thrown when there is an error in the Tabular data set that
 * prevents the JDXSpectrum from being displayed.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */

public class TabularDataSetException extends JDXSourceException {

  /**
   * Intialises a TabularDataSetException
   */
  public TabularDataSetException() {
    super();
  }

  /**
   * Intialises a TabularDataSetException with a message
   * @param message the message
   */
  public TabularDataSetException(String message) {
    super(message);
  }

  /**
   * Intialises a TabularDataSetException indicating the line number where
   * it was thrown
   * @param lineNum the line number
   */
  public TabularDataSetException(int lineNum) {
    super("Tabular Data Set Exception at line: " + lineNum);
  }
}
