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

package jspecview.common;

import jspecview.util.Coordinate;

/**
 * Interface Graph provides methods that are used for the display by
 * the JSVPanel. Any object that should be displayed by the JSVPanel should
 * implement this interface.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 * @see jspecview.common.JSVPanel
 */
public interface Graph {

  /**
   * Error number that is returned when a min value is undefined
   */
  public static final double ERROR = Double.MAX_VALUE;

  /**
   * Specifies whether the Spectrum is increasing
   * @return if the Spectrum is increasing or not
   */
  public boolean isIncreasing();
  /**
   * Specifies whether the Spectrum is continuous
   * @return if the Spectrum is continuous or not
   */
  public boolean isContinuous();
  /**
   * Gets the Title of the Spectrum
   * @return the Title of the Spectrum
   */
  public String getTitle();
  /**
   * Gets the units of the X axis
   * @return the units of the X axis
   */
  public String getXUnits();
  /**
   * gets the units of the Y axis
   * @return the units of the Y axis
   */
  public String getYUnits();

  /**
   * Gets the number of points in the Spectrum
   * @return the number of points in the Spectrum
   */
  public int getNumberOfPoints();

  /**
   * Returns the array of coordinates
   * @return the array of coordinates
   */
  public Coordinate[] getXYCoords();
}
