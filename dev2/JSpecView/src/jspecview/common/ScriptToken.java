/* Copyright (c) 2002-2012 The University of the West Indies
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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jspecview.util.TextFormat;

/**
 * ScriptToken takes care of script command processing
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public enum ScriptToken {

  UNKNOWN("?"),
  APPLETID("APPLETID"),
  APPLETREADYCALLBACKFUNCTIONNAME("APPLETREADYCALLBACKFUNCTIONNAME"),
  AUTOINTEGRATE("AUTOINTEGRATE"),
  BACKGROUNDCOLOR("BACKGROUNDCOLOR"),
  CLOSE("CLOSE"),   // id or file name or ALL
  COMPOUNDMENUON("COMPOUNDMENUON"),
  COORDCALLBACKFUNCTIONNAME("COORDCALLBACKFUNCTIONNAME"),
  COORDINATESCOLOR("COORDINATESCOLOR"),
  COORDINATESON("COORDINATESON"),
  DEBUG("DEBUG"),
  DISPLAYFONTNAME("DISPLAYFONTNAME"),
  ENABLEZOOM("ENABLEZOOM"),
  ENDINDEX("ENDINDEX"),
  GETSOLUTIONCOLOR("GETSOLUTIONCOLOR"),
  GRIDCOLOR("GRIDCOLOR"),
  GRIDON("GRIDON"),
  INTEGRATE("INTEGRATE"),
  INTEGRALPLOTCOLOR("INTEGRALPLOTCOLOR"),
  INTEGRATIONRATIOS("INTEGRATIONRATIOS"),
  INTERFACE("INTERFACE"),
  IRMODE("IRMODE"),
  LABEL("LABEL"),  // label x y color "text"
  LOAD("LOAD"),
  MENUON("MENUON"),
  OBSCURE("OBSCURE"),
  OVERLAY("OVERLAY"),   // overlay specno,specno,specno....
  PEAKCALLBACKFUNCTIONNAME("PEAKCALLBACKFUNCTIONNAME"),
  PLOTAREACOLOR("PLOTAREACOLOR"),
  PLOTCOLOR("PLOTCOLOR"),
  PLOTCOLORS("PLOTCOLORS"),
  REVERSEPLOT("REVERSEPLOT"),
  SCALECOLOR("SCALECOLOR"),
  SPECTRUM("SPECTRUM"),
  SPECTRUMNUMBER("SPECTRUMNUMBER"),
  STARTINDEX("STARTINDEX"),
  SYNCCALLBACKFUNCTIONNAME("SYNCCALLBACKFUNCTIONNAME"),
  SYNCID("SYNCID"),
  TITLEBOLDON("TITLEBOLDON"),
  TITLECOLOR("TITLECOLOR"),
  TITLEFONTNAME("TITLEFONTNAME"),
  UNITSCOLOR("UNITSCOLOR"),
  VERSION("VERSION"),
  XSCALEON("XSCALEON"),
  XUNITSON("XUNITSON"),
  YSCALEON("YSCALEON"),
  YUNITSON("YUNITSON"),
  ZOOM("ZOOM");

  private String name;

  private ScriptToken(String name) {
    this.name = name;
  }

  public static Map<String, ScriptToken> htParams;

  public static ScriptToken getScriptToken(String name) {
    if (htParams == null) {
      htParams = new Hashtable<String, ScriptToken>();
      for (ScriptToken item : values())
        htParams.put(item.name, item);
    }
    ScriptToken st = htParams.get(name.toUpperCase());
    return (st == null ? UNKNOWN : st);
  }

  /**
   * tweak command options depending upon special cases
   * 
   * @param st
   * @param params
   * @param token
   * @return
   */
  public static String getValue(ScriptToken st, StringTokenizer params, String token) {
    if (!params.hasMoreTokens())
      return "";
    int pt;
    switch (st) {
    default:
      return nextStringToken(params, true);
    case LABEL:
      // no trimming of quotes
      pt = token.indexOf(" ");
      if (pt < 0)
        return "";
      return token.substring(pt).trim();
    case CLOSE:
    case LOAD:
      // takes full command, possibly removing ""
      pt = token.indexOf(" ");
      if (pt < 0)
        return "";
      String s = token.substring(pt).trim();
      return (s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s);        
    case OVERLAY:
      // clean, with no spaces
      pt = token.indexOf(" ");
      if (pt < 0)
        return "";
      return TextFormat.simpleReplace(token.substring(pt).trim(), " ", "");      
    case ZOOM:
      // clean, with no spaces; possibly "out"
      String x1 = nextStringToken(params, true);
      pt = token.indexOf(" ");
      if (pt < 0 || x1.equalsIgnoreCase("out"))
        return "0,0";
      return TextFormat.simpleReplace(token.substring(pt).trim(), " ", "");
    }
  }

  public static String nextStringToken(StringTokenizer params, boolean removeQuotes) {
    String s = params.nextToken();
    if (s.charAt(0) != '"') 
      return s;
    if (s.endsWith("\""))
      return (removeQuotes ? s.substring(1, s.length() - 1) : s);
    StringBuffer sb = new StringBuffer(s.substring(1));
    s = null;
    while (params.hasMoreTokens() && !(s = params.nextToken()).endsWith("\"")) {
      sb.append(" ").append(s);
      s = null;
    }
    if (s != null)
      sb.append(s.substring(0, s.length() - 1));
    s = sb.toString();
    return (removeQuotes ? s : "\"" + s + "\"");
  }

  public static String getKey(StringTokenizer eachParam) {
    String key = eachParam.nextToken();
    if (key.equalsIgnoreCase("SET"))
      key = eachParam.nextToken();
    return key.toUpperCase();
  }

  public static List<String> getTokens(String value) {
    List<String>tokens = new ArrayList<String>();
    StringTokenizer st = new StringTokenizer(value);
    while (st.hasMoreElements())
      tokens.add(nextStringToken(st, false));
    return tokens;
  }
}