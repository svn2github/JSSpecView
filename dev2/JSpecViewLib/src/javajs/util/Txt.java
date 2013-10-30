/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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

package javajs.util;

public class Txt {

	/**
	 * Does a clean replace of strFrom in str with strTo. This method has far
	 * faster performance than just String.replace() when str does not contain
	 * strFrom, but is about 15% slower when it does. (Note that
	 * String.replace(CharSeq, CharSeq) was introduced in Java 1.5. Finally
	 * getting around to using it in Jmol!)
	 * 
	 * @param str
	 * @param strFrom
	 * @param strTo
	 * @return replaced string
	 */
	public static String simpleReplace(String str, String strFrom, String strTo) {
	  if (str == null || strFrom.length() == 0 || str.indexOf(strFrom) < 0)
	    return str;
	  boolean isOnce = (strTo.indexOf(strFrom) >= 0);
	  do {
	    str = str.replace(strFrom, strTo);
	  } while (!isOnce && str.indexOf(strFrom) >= 0);
	  return str;
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
	public static String replaceAllCharacter(String str, String strFrom,
	                                          char chTo) {
	  if (str == null)
	    return null;
	  for (int i = strFrom.length(); --i >= 0;)
	    str = str.replace(strFrom.charAt(i), chTo);
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

	public static String trimQuotes(String value) {
	  return (value != null && value.length() > 1 && value.startsWith("\"")
	      && value.endsWith("\"") ? value.substring(1, value.length() - 1)
	      : value);
	}
}