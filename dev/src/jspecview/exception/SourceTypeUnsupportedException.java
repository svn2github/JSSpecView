/* Copyright (c) 2002-2006 The University of the West Indies
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
 * Exception thrown when the source is not in JCAMP-DX format or the format is
 * unsupported.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */

public class SourceTypeUnsupportedException extends JSpecViewException {

  /**
   * Initialises a SourceTypeUnsupportedException
   */
  public SourceTypeUnsupportedException() {
    super();
  }

  /**
   * Initialises a SourceTypeUnsupportedException with a message
   * @param message the message
   */
  public SourceTypeUnsupportedException(String message) {
    super(message);
  }
}
