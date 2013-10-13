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

import org.jmol.util.Txt;


public class JSVTxt {

	public static final String newLine = System.getProperty("line.separator");

	public static String fixExponentInt(double x) {
	  return (x == Math.floor(x) ? String.valueOf((int) x) : Txt.simpleReplace(JSVTxt.fixExponent(x), "E+00", ""));
	}

	public static String fixIntNoExponent(double x) {
	  return (x == Math.floor(x) ? String.valueOf((int) x) : JSVTxt.formatDecimalTrimmed(x, 10));
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
	  String s = Txt.formatDecimalDbl(x, -7); // "0.000000"
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

	public static String formatDecimalTrimmed(double x, int precision) {
	  return JSVTxt.rtrim(Txt.formatDecimalDbl(x, precision), "0"); // 0.##...
	}

	public static String rtrim(String str, String chars) {
	  if (chars.length() == 0)
	    return str.trim();
	  int m = str.length() - 1;
	  while (m >= 0 && chars.indexOf(str.charAt(m)) >= 0)
	    m--;
	  return str.substring(0, m + 1);
	}

//static {
//DecimalFormat df =  new DecimalFormat("0.00E0", new DecimalFormatSymbols(java.util.Locale.US));
//System.out.println(df.format(-1.0) + " " + formatDecimal(-1.0, -2));
//System.out.println(df.format(-1.3467E-10) + " " + formatDecimal(-1.3467E-10, -2));
//System.out.println(df.format(-1.3467E10) + " " + formatDecimal(-1.3467E10, -2));
//System.out.println(df.format(-1.3467) + " " + formatDecimal(-1.3467, -2));
//df = new DecimalFormat("#0.00", new DecimalFormatSymbols(java.util.Locale.US));
//System.out.println(df.format(-1.3467) + " " + formatDecimal(-1.3467, 2));
//System.out.println(df.format(-1.456) + " " + formatDecimal(-1.456, 2));
//System.out.println(df.format(-1.535) + " " + formatDecimal(-1.535, 2));
//System.out.println(df.format(-1.545) + " " + formatDecimal(-1.545, 2));
//System.out.println(df.format(0) + " " + formatDecimal(0, 2));
//df = new DecimalFormat("#", new DecimalFormatSymbols(java.util.Locale.US));
//System.out.println(df.format(-1.3467) + " " + formatDecimal(-1.3467, 0));
//System.out.println(df.format(-1.45) + " " + formatDecimal(-1.45, 0));
//System.out.println(df.format(-1.5) + " " + formatDecimal(-1.5, 0));
//System.out.println(df.format(-1.1) + " " + formatDecimal(-1.1, 0));
//System.out.println(df.format(0) + " " + formatDecimal(0, 0));
//
////-1.00E0 -1.00E0
////-1.35E-10 -1.35E-10
////-1.35E10 -1.35E10
////-1.35E0 -1.35E0
////-1.35 -1.35
////-1.46 -1.46
////-1.54 -1.54
////-1.54 -1.54
////0.00 0.00
////-1 -1
////-1 -1
////-2 -2
////-1 -1
////0 0
//}

//static {
//System.out.println("TEST TextFormat.java");
//System.out.println((long) 1.23456789E10);   
//System.out.println((long) Math.round(1.2345678999999E10));   
//System.out.println((long) 1.2345679000001E+10);   
//System.out.println((long) 1.2345679005001E10);   
//System.out.println((long) Math.round(1.2345679004999E10));   
//System.out.println((long) 1.234567890123E15);   
//System.out.println((long) 9.999999666666666665E17);   
//////////////////////////////12345678901234567
//System.out.println((long) 1.2345678901234567E15);   
//
//}
}
