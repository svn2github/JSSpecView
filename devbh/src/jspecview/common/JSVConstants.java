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

/**
 * Constants collected here
 * @author Bob Hanson, hansonr@stolaf.edu
 */
public class JSVConstants {
  
  public final static int CALLBACK_APPLETREADY = 0;
  public final static int CALLBACK_LOADSPECTRUM = 1;
  public final static int CALLBACK_PEAK = 2;
  public final static int CALLBACK_COORD = 3;
  public final static int CALLBACK_RESIZE = 4;
  public final static int CALLBACK_COUNT = 5;
  
  private final static String[] callbackNames = {
    "appletReadyCallback",
    "loadSpectrumCallback",
    "peakCallback", 
    "coordCallback",
    "resizeCallback", 
  };
  
  public static String getCallbackName(int i) {
    return callbackNames[i];
  }

  public static String getCallbackName(String callbackName) {
    for (int i = 0; i < CALLBACK_COUNT; i++)
      if (callbackNames[i].equalsIgnoreCase(callbackName))
        return callbackNames[i];
    return null;
  }
}


