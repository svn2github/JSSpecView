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

public class JSVTextFormat {

  private final static String[] formattingStrings = { "0", "0.0", "0.00", "0.000",
      "0.0000", "0.00000", "0.000000", "0.0000000", "0.00000000", "0.000000000" };

  private final static String zeros = "0000000000000000000000000000000000000000";

  private final static float[] formatAdds = { 0.5f, 0.05f, 0.005f, 0.0005f,
  	0.00005f, 0.000005f, 0.0000005f, 0.00000005f, 0.000000005f, 0.0000000005f };

  private final static Boolean[] useNumberLocalization = new Boolean[1];

  public static final String newLine = System.getProperty("line.separator");

	{
    useNumberLocalization[0] = Boolean.TRUE;
  }
  
  public static void setUseNumberLocalization(boolean TF) {
    useNumberLocalization[0] = (TF ? Boolean.TRUE : Boolean.FALSE);
  }

  /**
   * A simple alternative to DecimalFormat (which Java2Script does not have
   * and which is quite too complex for our use here).
   * 
   * Slightly different from Jmol's TextFormat.formatDecimal so as to 
   * better mimic DecimalFormat:
   * 
   * (a) -n means n digits AFTER the decimal place, not total # of digits
   * 
   * (b) no "+" after E
   * 
   * (c) to-even rounding of xx.xx5 
   * 
   * Limited support for scientific notation. 
   * Note that the last 0 in "0.00E00" is ignored.
   * 
   * @param value
   * @param decimalDigits
   * @return  formatted decimal
   */
  public static String formatDecimal(double value, int decimalDigits) {
    if (decimalDigits == Integer.MAX_VALUE 
        || value == Double.NEGATIVE_INFINITY || value == Double.POSITIVE_INFINITY || Double.isNaN(value))
      return "" + value;
    int n;
    if (decimalDigits < 0) {
      decimalDigits = 1 - decimalDigits;
      if (decimalDigits > formattingStrings.length)
        decimalDigits = formattingStrings.length;
      if (value == 0)
        return formattingStrings[decimalDigits] + "E0";
      //scientific notation
      n = 0;
      double d;
      if (Math.abs(value) < 1) {
        n = 10;
        d = value * 1e-10;
      } else {
        n = -10;
        d = value * 1e10;
      }
      String s = ("" + d).toUpperCase();
      int i = s.indexOf("E");
      n = JSVParser.parseInt(s.substring(i + 1)) + n;
      return (i < 0 ? "" + value : formatDecimal(JSVParser.parseFloat(s.substring(
          0, i)), decimalDigits - 1)
          + "E" + n);
    }

    if (decimalDigits >= formattingStrings.length)
      decimalDigits = formattingStrings.length - 1;
    String s1 = ("" + value).toUpperCase();
    boolean isNeg = s1.startsWith("-");
    if (isNeg)
      s1 = s1.substring(1);
    int pt = s1.indexOf(".");
    if (pt < 0)
      return s1 + formattingStrings[decimalDigits].substring(1);
    int pt1 = s1.indexOf("E-");
    if (pt1 > 0) {
      n = JSVParser.parseInt(s1.substring(pt1 + 1));
      // 3.567E-2
      // 0.03567
      s1 = "0." + zeros.substring(0, -n - 1) + s1.substring(0, 1) + s1.substring(2, pt1);
      pt = 1; 
    }

    pt1 = s1.indexOf("E");
    // 3.5678E+3
    // 3567.800000000
    // 1.234E10 %3.8f -> 12340000000.00000000
    if (pt1 > 0) {
      n = JSVParser.parseInt(s1.substring(pt1 + 1));
      s1 = s1.substring(0, 1) + s1.substring(2, pt1) + zeros;
      s1 = s1.substring(0, n + 1) + "." + s1.substring(n + 1);
      pt = s1.indexOf(".");
    } 
    // "234.345667  len == 10; pt = 3
    // "  0.0 "  decimalDigits = 1
    
    int len = s1.length();
    int pt2 = decimalDigits + pt + 1;
    char ch;
    if (pt2 < len && ((ch = s1.charAt(pt2)) > '5' || ch == '5' 
    	&& (pt2 > 0 && (0 + ((ch = s1.charAt(pt2 - 1)) == '.' ? s1.charAt(pt2 - 2) : ch) % 2 == 1)))) {
      return formatDecimal(
          value + (isNeg ? -1 : 1) * formatAdds[decimalDigits], decimalDigits);
    }

    JSVSB sb = JSVSB.newS(s1.substring(0, (decimalDigits == 0 ? pt
        : ++pt)));
    for (int i = 0; i < decimalDigits; i++, pt++) {
      if (pt < len)
        sb.appendC(s1.charAt(pt));
      else
        sb.appendC('0');
    }
    s1 = (isNeg ? "-" : "") + sb;
    return (Boolean.TRUE.equals(useNumberLocalization[0]) ? s1 : s1.replace(',',
        '.'));
  }

  public static String formatNumber(double value, int width, int precision,
                              boolean alignLeft, boolean zeroPad) {
    return format(formatDecimal(value, precision), width, 0, alignLeft, zeroPad);
  }

  public static String format(String value, int width, int precision,
                              boolean alignLeft, boolean zeroPad) {
    if (value == null)
      return "";
    if (precision != Integer.MAX_VALUE && precision > 0
        && precision < value.length())
      value = value.substring(0, precision);
    else if (precision < 0 && -precision < value.length())
      value = value.substring(value.length() + precision);

    int padLength = width - value.length();
    if (padLength <= 0)
      return value;
    boolean isNeg = (zeroPad && !alignLeft && value.charAt(0) == '-');
    char padChar = (zeroPad ? '0' : ' ');
    char padChar0 = (isNeg ? '-' : padChar);

    JSVSB sb = new JSVSB();
    if (alignLeft)
      sb.append(value);
    sb.appendC(padChar0);
    for (int i = padLength; --i > 0;)
      // this is correct, not >= 0
      sb.appendC(padChar);
    if (!alignLeft)
      sb.append(isNeg ? padChar + value.substring(1) : value);
    return sb.toString();
  }
  
  public static String sprintf(String strFormat, Object[] values) {
    if (values == null)
      return strFormat;
    for (int o = 0; o < values.length; o++)
      if (values[o] != null) {
        if (values[o] instanceof String[]) {
          String[] sVal = (String[]) values[o];
          for (int i = 0; i < sVal.length; i++)
            strFormat = formatString(strFormat, "s", sVal[i], Float.NaN, true);
        } else if (values[o] instanceof float[]) {
          float[] fVal = (float[]) values[o];
          for (int i = 0; i < fVal.length; i++)
            strFormat = formatString(strFormat, "f", null, fVal[i], true);
        } else if (values[o] instanceof int[]) {
          int[] iVal = (int[]) values[o];
          for (int i = 0; i < iVal.length; i++)
            strFormat = formatString(strFormat, "d", "" + iVal[i], Float.NaN,
                true);
        }
      }
    return strFormat;
  }

  public static String sprintf(String strFormat, String[] sVal, float[] fVal) {
    return sprintf(strFormat, new Object[] {sVal, fVal});
  }
  
  public static String sprintf(String strFormat, String[] sVal, float[] fVal, 
                               int[] iVal) {
    return sprintf(strFormat, new Object[] {sVal, fVal, iVal});
  }

  public static String formatString(String strFormat, String key, String strT,
                                    float floatT) {
    return formatString(strFormat, key, strT, floatT, false);
  }

  /**
   * generic string formatter  based on formatLabel in Atom
   * 
   * 
   * @param strFormat   .... %width.precisionKEY....
   * @param key      any string to match
   * @param strT     replacement string or null
   * @param floatT   replacement float or Float.NaN
   * @param doOne    mimic sprintf    
   * @return         formatted string
   */

  private static String formatString(String strFormat, String key, String strT,
                                    float floatT, boolean doOne) {
    if (strFormat == null)
      return null;
    if ("".equals(strFormat))
      return "";
    int len = key.length();
    if (strFormat.indexOf("%") < 0 || len == 0 || strFormat.indexOf(key) < 0)
      return strFormat;

    String strLabel = "";
    int ich, ichPercent, ichKey;
    for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) >= 0
        && (ichKey = strFormat.indexOf(key, ichPercent + 1)) >= 0;) {
      if (ich != ichPercent)
        strLabel += strFormat.substring(ich, ichPercent);
      ich = ichPercent + 1;
      if (ichKey > ichPercent + 6) {
        strLabel += '%';
        continue;//%12.10x
      }
      try {
        boolean alignLeft = false;
        if (strFormat.charAt(ich) == '-') {
          alignLeft = true;
          ++ich;
        }
        boolean zeroPad = false;
        if (strFormat.charAt(ich) == '0') {
          zeroPad = true;
          ++ich;
        }
        char ch;
        int width = 0;
        while ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
          width = (10 * width) + (ch - '0');
          ++ich;
        }
        int precision = Integer.MAX_VALUE;
        if (strFormat.charAt(ich) == '.') {
          ++ich;
          if ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
            precision = ch - '0';
            ++ich;
          }
        }
        String st = strFormat.substring(ich, ich + len);
        if (!st.equals(key)) {
          ich = ichPercent + 1;
          strLabel += '%';
          continue;
        }
        ich += len;
        if (!Float.isNaN(floatT))
          strLabel += formatNumber(floatT, width, precision, alignLeft,
              zeroPad);
        else if (strT != null)
          strLabel += format(strT, width, precision, alignLeft,
              zeroPad);
        if (doOne)
          break;
      } catch (IndexOutOfBoundsException ioobe) {
        ich = ichPercent;
        break;
      }
    }
    strLabel += strFormat.substring(ich);
    //if (strLabel.length() == 0)
      //return null;
    return strLabel;
  }

  /**
   * 
   *  proper splitting, even for Java 1.3 -- if the text ends in the run,
   *  no new line is appended.
   * 
   * @param text
   * @param run
   * @return  String array
   */
  public static String[] split(String text, String run) {
    int n = 1;
    int i = text.indexOf(run);
    String[] lines;
    int runLen = run.length();
    if (i < 0 || runLen == 0) {
      lines = new String[1];
      lines[0] = text;
      return lines;
    }
    int len = text.length() - runLen;
    for (; i >= 0 && i < len; n++)
      i = text.indexOf(run, i + runLen);
    lines = new String[n];
    i = 0;
    int ipt = 0;
    int pt = 0;
    for (; (ipt = text.indexOf(run, i)) >= 0 && pt + 1 < n;) {
      lines[pt++] = text.substring(i, ipt);
      i = ipt + runLen;
    }
    if (text.indexOf(run, len) != len)
      len += runLen;
    lines[pt] = text.substring(i, len);
    return lines;
  }

  /**
   * Does a clean replace of any of the characters in str with strTo
   * If strTo contains strFrom, then only a single pass is done.
   * Otherwise, multiple passes are made until no more replacements can be made.
   * 
   * @param str
   * @param strFrom
   * @param strTo
   * @return  replaced string
   */
  public static String replaceAllCharacters(String str, String strFrom,
                                            String strTo) {
    for (int i = strFrom.length(); --i >= 0;) {
      String chFrom = strFrom.substring(i, i + 1);
      str = simpleReplace(str, chFrom, strTo);
    }
    return str;
  }
  
  /**
   * Does a clean replace of any of the characters in str with chrTo
   * If strTo contains strFrom, then only a single pass is done.
   * Otherwise, multiple passes are made until no more replacements can be made.
   * 
   * @param str
   * @param strFrom
   * @param chTo
   * @return  replaced string
   */
  public static String replaceAllCharacters(String str, String strFrom,
                                            char chTo) {
    for (int i = strFrom.length(); --i >= 0;)
      str = str.replace(strFrom.charAt(i), chTo);
    return str;
  }
  
  /**
   * Does a clean replace of strFrom in str with strTo
   * If strTo contains strFrom, then only a single pass is done.
   * Otherwise, multiple passes are made until no more replacements can be made.
   * 
   * @param str
   * @param strFrom
   * @param strTo
   * @return  replaced string
   */
  public static String simpleReplace(String str, String strFrom, String strTo) {
    if (str == null || str.indexOf(strFrom) < 0 || strFrom.equals(strTo))
      return str;
    int fromLength = strFrom.length();
    if (fromLength == 0)
      return str;
    boolean isOnce = (strTo.indexOf(strFrom) >= 0);
    int ipt;
    while (str.indexOf(strFrom) >= 0) {
      JSVSB s = new JSVSB();
      int ipt0 = 0;
      while ((ipt = str.indexOf(strFrom, ipt0)) >= 0) {
        s.append(str.substring(ipt0, ipt)).append(strTo);
        ipt0 = ipt + fromLength;
      }
      s.append(str.substring(ipt0));
      str = s.toString();
      if (isOnce)
        break;
    }

    return str;
  }

  public static String trim(String str, String chars) {
    if (chars.length() == 0)
      return str.trim();
    int len = str.length();
    int k = 0;
    while (k < len && chars.indexOf(str.charAt(k)) >= 0)
      k++;
    int m = str.length() - 1;
    while (m > k && chars.indexOf(str.charAt(m)) >= 0)
      m--;
    return str.substring(k, m + 1);
  }

  public static String rtrim(String str, String chars) {
    if (chars.length() == 0)
      return str.trim();
    int m = str.length() - 1;
    while (m >= 0 && chars.indexOf(str.charAt(m)) >= 0)
      m--;
    return str.substring(0, m + 1);
  }

  public static String[] split(String text, char ch) {
    return split(text, "" + ch);
  }
  
  public static void leftJustify(JSVSB s, String sFill, String sVal) {
    s.append(sVal);
    int n = sFill.length() - sVal.length();
    if (n > 0)
      s.append(sFill.substring(0, n));
  }
  
  public static void rightJustify(JSVSB s, String sFill, String sVal) {
    int n = sFill.length() - sVal.length();
    if (n > 0)
      s.append(sFill.substring(0, n));
    s.append(sVal);
  }
  
  public static String safeTruncate(float f, int n) {
    if (f > -0.001 && f < 0.001)
      f = 0;
    return (f + "         ").substring(0,n);
  }

  public static boolean isWild(String s) {
    return s.indexOf("*") >= 0 || s.indexOf("?") >= 0;
  }

  public static boolean isMatch(String s, String strWildcard,
                                boolean checkStar, boolean allowInitialStar) {

    int ich = 0;
    int cchWildcard = strWildcard.length();
    int cchs = s.length();
    if (cchs == 0 || cchWildcard == 0)
      return false;
    boolean isStar0 = (checkStar && allowInitialStar ? strWildcard.charAt(0) == '*' : false);
    if (isStar0 && strWildcard.charAt(cchWildcard - 1) == '*')
      return (cchWildcard < 3 || s.indexOf(strWildcard.substring(1,
          cchWildcard - 1)) >= 0); 
    String qqq = "????";
    while (qqq.length() < s.length())
      qqq += qqq;
    if (checkStar) {
      if (allowInitialStar && isStar0)
        strWildcard = qqq + strWildcard.substring(1);
      if (strWildcard.charAt(ich = strWildcard.length() - 1) == '*')
        strWildcard = strWildcard.substring(0, ich) + qqq;
      cchWildcard = strWildcard.length();
    }

    if (cchWildcard < cchs)
      return false;

    ich = 0;

    // atom name variant (trimLeadingMarks == false)

    // -- each ? matches ONE character if not at end
    // -- extra ? at end ignored

    //group3 variant (trimLeadingMarks == true)

    // -- each ? matches ONE character if not at end
    // -- extra ? at beginning reduced to match length
    // -- extra ? at end ignored

    while (cchWildcard > cchs) {
      if (allowInitialStar && strWildcard.charAt(ich) == '?') {
        ++ich;
      } else if (strWildcard.charAt(ich + cchWildcard - 1) != '?') {
        return false;
      }
      --cchWildcard;
    }

    for (int i = cchs; --i >= 0;) {
      char charWild = strWildcard.charAt(ich + i);
      if (charWild == '?')
        continue;
      if (charWild != s.charAt(i))
        return false;
    }
    return true;
  }

//	private static Map<String, DecimalFormat> htFormats = new Hashtable<String, DecimalFormat>();
  
//	public static DecimalFormat getDecimalFormat(String hash) {
//		DecimalFormat df = htFormats.get(hash);
//		if (df == null) {
//	  	System.out.println("JSVTextFormat using DF " + hash);
//			if (hash.length() == 0)
//				hash = "0.00E0";
//			htFormats.put(hash, df = new DecimalFormat(hash, new DecimalFormatSymbols(java.util.Locale.US)));
//		}
//		return df;
//	}
  
  public static String trimQuotes(String value) {
    return (value.length() > 1 && value.startsWith("\"")
        && value.endsWith("\"") ? value.substring(1, value.length() - 1)
        : value);
  }

	public static String formatDecimalTrimmed(double x, int precision) {
	  return JSVTextFormat.rtrim(JSVTextFormat.formatDecimal(x, precision), "0"); // 0.##...
	}  

  public static String fixExponentInt(double x) {
    return (x == Math.floor(x) ? String.valueOf((int) x) : simpleReplace(fixExponent(x), "E+00", ""));
  }

  public static String fixIntNoExponent(double x) {
    return (x == Math.floor(x) ? String.valueOf((int) x) : formatDecimalTrimmed(x, 10));
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
    String s = formatDecimal(x, -6);
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

//	static {
//  	DecimalFormat df =  new DecimalFormat("0.00E0", new DecimalFormatSymbols(java.util.Locale.US));
//  	System.out.println(df.format(-1.0) + " " + formatDecimal(-1.0, -2));
//  	System.out.println(df.format(-1.3467E-10) + " " + formatDecimal(-1.3467E-10, -2));
//  	System.out.println(df.format(-1.3467E10) + " " + formatDecimal(-1.3467E10, -2));
//  	System.out.println(df.format(-1.3467) + " " + formatDecimal(-1.3467, -2));
//		df = new DecimalFormat("#0.00", new DecimalFormatSymbols(java.util.Locale.US));
//		System.out.println(df.format(-1.3467) + " " + formatDecimal(-1.3467, 2));
//		System.out.println(df.format(-1.456) + " " + formatDecimal(-1.456, 2));
//		System.out.println(df.format(-1.535) + " " + formatDecimal(-1.535, 2));
//		System.out.println(df.format(-1.545) + " " + formatDecimal(-1.545, 2));
//		System.out.println(df.format(0) + " " + formatDecimal(0, 2));
//		df = new DecimalFormat("#", new DecimalFormatSymbols(java.util.Locale.US));
//		System.out.println(df.format(-1.3467) + " " + formatDecimal(-1.3467, 0));
//		System.out.println(df.format(-1.45) + " " + formatDecimal(-1.45, 0));
//		System.out.println(df.format(-1.5) + " " + formatDecimal(-1.5, 0));
//		System.out.println(df.format(-1.1) + " " + formatDecimal(-1.1, 0));
//		System.out.println(df.format(0) + " " + formatDecimal(0, 0));
//		
////		-1.00E0 -1.00E0
////		-1.35E-10 -1.35E-10
////		-1.35E10 -1.35E10
////		-1.35E0 -1.35E0
////		-1.35 -1.35
////		-1.46 -1.46
////		-1.54 -1.54
////		-1.54 -1.54
////		0.00 0.00
////		-1 -1
////		-1 -1
////		-2 -2
////		-1 -1
////		0 0
//	}
  
//  static {
//    System.out.println("TEST TextFormat.java");
//    System.out.println((long) 1.23456789E10);   
//    System.out.println((long) Math.round(1.2345678999999E10));   
//    System.out.println((long) 1.2345679000001E+10);   
//    System.out.println((long) 1.2345679005001E10);   
//    System.out.println((long) Math.round(1.2345679004999E10));   
//    System.out.println((long) 1.234567890123E15);   
//    System.out.println((long) 9.999999666666666665E17);   
//    ////////////////////////////12345678901234567
//    System.out.println((long) 1.2345678901234567E15);   
//    
//  }

}
