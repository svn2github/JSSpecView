/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javajs.util.PT;
import javajs.util.SB;



public class JSVEscape {

  private final static String escapable = "\\\\\tt\rr\nn\"\""; 

  public static String eS(String str) {
    if (str == null)
      return "\"\"";
    boolean haveEscape = false;
    int i = 0;
    for (; i < escapable.length(); i += 2)
      if (str.indexOf(escapable.charAt(i)) >= 0) {
        haveEscape = true;
        break;
      }
    if (haveEscape)
      while (i < escapable.length()) {
        int pt = -1;
        char ch = escapable.charAt(i++);
        char ch2 = escapable.charAt(i++);
        SB sb = new SB();
        int pt0 = 0;
        while ((pt = str.indexOf(ch, pt + 1)) >= 0) {
          sb.append(str.substring(pt0, pt)).appendC('\\').appendC(ch2);
          pt0 = pt + 1;
        }
        sb.append(str.substring(pt0, str.length()));
        str = sb.toString();
      }
    for (i = str.length(); --i >= 0;)
      if (str.charAt(i) > 0x7F)
        str = str.substring(0, i) + unicode(str.charAt(i))
            + str.substring(i + 1);
    return "\"" + str + "\"";
  }

  private static String unicode(char c) {
    String s = "0000" + Integer.toHexString(c);
    return "\\u" + s.substring(s.length() - 4);
  }
  
  public static String getHexColorFromRGB(int rgb) {
    if (rgb == 0)
      return "000000";
    String r  = "00" + Integer.toHexString((rgb >> 16) & 0xFF);
    r = r.substring(r.length() - 2);
    String g  = "00" + Integer.toHexString((rgb >> 8) & 0xFF);
    g = g.substring(g.length() - 2);
    String b  = "00" + Integer.toHexString(rgb & 0xFF);
    b = b.substring(b.length() - 2);
    return r + g + b;
  }

  @SuppressWarnings("unchecked")
  public static String toJSON(String infoType, Object info, boolean addCR) {

    //Logger.debug(infoType+" -- "+info);

    SB sb = new SB();
    String sep = "";
    if (info == null)
      return packageJSON(infoType, (String) null, addCR);
    if (info instanceof Integer || info instanceof Float || info instanceof Double)
      return packageJSON(infoType, info.toString(), addCR);
    if (info instanceof String)
      return packageJSON(infoType, fixString((String) info), addCR);
    if (info instanceof String[]) {
      sb.append("[");
      int imax = ((String[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(fixString(((String[]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb, addCR);
    }
    if (info instanceof int[]) {
      sb.append("[");
      int imax = ((int[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).appendI(((int[]) info)[i]);
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb, addCR);
    }
    if (info instanceof float[]) {
      sb.append("[");
      int imax = ((float[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).appendF(((float[]) info)[i]);
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb, addCR);
    }
    if (info instanceof double[]) {
      sb.append("[");
      int imax = ((double[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).appendD(((double[]) info)[i]);
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb, addCR);
    }
    if (info instanceof String[][]) {
      sb.append("[");
      if (addCR)
      	sb.append("\n");
      int imax = ((String[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((String[][]) info)[i], addCR));
        if (addCR)
        	sb.append("\n");
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb, addCR);
    }
    if (info instanceof int[][]) {
      sb.append("[");
      int imax = ((int[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((int[][]) info)[i], addCR));
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb, addCR);
    }
    if (info instanceof float[][]) {
      sb.append("[");
      int imax = ((float[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((float[][]) info)[i], addCR));
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb, addCR);
    }
    if (info instanceof double[][]) {
      sb.append("[");
      int imax = ((double[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((double[][]) info)[i], addCR));
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb, addCR);
    }
    if (info instanceof float[][][]) {
      sb.append("[");
      int imax = ((float[][][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((float[][][]) info)[i], addCR));
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb, addCR);
    }
    if (info instanceof Object[]) {
      sb.append("[");
      int imax = ((Object[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((Object[]) info)[i], addCR));
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb, addCR);
    }
    if (info instanceof List) {
      sb.append("[ ");
      int imax = ((List<?>) info).size();
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((List<?>) info).get(i), addCR));
        sep = ",";
      }
      sb.append(" ]");
      return packageJSON(infoType, sb, addCR);
    }
    if (info instanceof Map) {
      sb.append("{ ");
      Iterator<String> e = ((Map<String, ?>) info).keySet().iterator();
      while (e.hasNext()) {
        String key = e.next();
        sb.append(sep)
            .append(packageJSON(key, toJSON(null, ((Map<?, ?>) info).get(key), addCR), addCR));
        sep = ",";
      }
      sb.append(" }");
      return packageJSON(infoType, sb, addCR);
    }
    return packageJSON(infoType, fixString(info.toString()), addCR);
  }

  private static String packageJSON(String infoType, SB sb, boolean addCR) {
    return packageJSON(infoType, sb.toString(), addCR);
  }

  private static String packageJSON(String infoType, String info, boolean addCR) {
    if (infoType == null)
      return info;
    return "\"" + infoType + "\": " + info + (addCR ? "\n" : "");
  }

  private static String fixString(String s) {
    if (s == null || s.indexOf("{\"") == 0) //don't doubly fix JSON strings when retrieving status
      return s;
    s = PT.simpleReplace(s, "\"", "''");
    s = PT.simpleReplace(s, "\n", " | ");
    return "\"" + s + "\"";
  }

  public static String escapeUrl(String url) {
    url = PT.simpleReplace(url, "\n", "");
    url = PT.simpleReplace(url, "%", "%25");
    url = PT.simpleReplace(url, "[", "%5B");
    url = PT.simpleReplace(url, "]", "%5D");
    url = PT.simpleReplace(url, " ", "%20");
    url = PT.simpleReplace(url, "?", "%3F");
    return url;
  }


}
