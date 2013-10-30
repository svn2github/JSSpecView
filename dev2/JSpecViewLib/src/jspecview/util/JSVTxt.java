/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package jspecview.util;

import javajs.util.DecimalFormat;
import javajs.util.Txt;



public class JSVTxt {

	public static final String newLine = System.getProperty("line.separator");

	public static String fixExponentInt(double x) {
	  return (x == Math.floor(x) ? String.valueOf((int) x) : Txt.simpleReplace(fixExponent(x), "E+00", ""));
	}

	public static String fixIntNoExponent(double x) {
	  return (x == Math.floor(x) ? String.valueOf((int) x) : DecimalFormat.formatDecimalTrimmed(x, 10));
	}

	public static boolean isAlmostInteger(double x) {
	  return (x != 0 && Math.abs(x - Math.floor(x)) / x > 1e-8);
	}

	/**
	 * JCAMP-DX requires 1.5E[+|-]nn or 1.5E[+|-]nnn only
	 * not Java's 1.5E3 or 1.5E-2
	 * 
	 * @param x
	 * @return exponent fixed
	 */
	private static String fixExponent(double x) {
	  String s = DecimalFormat.formatDecimalDbl(x, -7); // "0.000000"
	  int pt = s.indexOf("E");
	  if (pt < 0) {
	    return s;
	  }
	  switch (s.length() - pt) {
	  case 2:
	    s = s.substring(0, pt + 1) + "0" + s.substring(pt + 1);
	    break;
	  case 3:
	    // 4.3E-3
	    if (s.charAt(pt + 1) == '-')
	      s = s.substring(0, pt + 2) + "0" + s.substring(pt + 2);
	    break;
	  } 
	  if (s.indexOf("E-") < 0)
	    s = s.substring(0, pt + 1) + "+" + s.substring(pt + 1);
	  return s;
	}

}
