/* Copyright (c) 2002-2008 The University of the West Indies
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
 * <code>LDRNotFoundException</code> is thrown when a labled Data Record required
 * to display spectrum is not found.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class LDRNotFoundException extends JDXSourceException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor
   */
  public LDRNotFoundException() {
    super();
  }

  /**
   * Constructor
   * @param label the name of the label
   */
  public LDRNotFoundException(String label) {
    super("Label Data Record for " + label + " Not Found");
  }
}
