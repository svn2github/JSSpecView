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
import java.util.Map.Entry;

import jspecview.util.TextFormat;

/**
 * ScriptToken takes care of script command processing
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public enum ScriptToken {

  // null tip means DON'T SHOW
  UNKNOWN("?"),
  APPLETID("APPLETID"),
  APPLETREADYCALLBACKFUNCTIONNAME("APPLETREADYCALLBACKFUNCTIONNAME"),
  AUTOINTEGRATE("AUTOINTEGRATE", "TF"),
  BACKGROUNDCOLOR("BACKGROUNDCOLOR", "C"),
  CLOSE("CLOSE", "spectrumId or fileName or ALL"),
  COMPOUNDMENUON("COMPOUNDMENUON", "TF"),
  COORDCALLBACKFUNCTIONNAME("COORDCALLBACKFUNCTIONNAME"),
  COORDINATESCOLOR("COORDINATESCOLOR", "C"),
  COORDINATESON("COORDINATESON", "TF"),
  DEBUG("DEBUG", "TF"),
  DISPLAYFONTNAME("DISPLAYFONTNAME", "fontName"),
  ENABLEZOOM("ENABLEZOOM", "TF"),
  ENDINDEX("ENDINDEX"),
  EXPORT("EXPORT", "[JPG,PNG,XY,...] \"filename\""), 
  GETSOLUTIONCOLOR("GETSOLUTIONCOLOR", ""),
  GRIDCOLOR("GRIDCOLOR", "C"),
  GRIDON("GRIDON", "TF"),
  INTEGRATE("INTEGRATE", "ON/OFF/?/MARK ppm1-ppm2,ppm3-ppm4,..."),
  INTEGRALPLOTCOLOR("INTEGRALPLOTCOLOR"),
  INTEGRATIONRATIOS("INTEGRATIONRATIOS"),
  INTERFACE("INTERFACE"),
  IRMODE("IRMODE", "A or T or ?"),
  LABEL("LABEL", "x y [color and/or \"text\"]"),
  LOAD("LOAD", "[APPEND] \"fileName\" [first] [last]"),
  MENUON("MENUON"),
  OBSCURE("OBSCURE"),
  OVERLAY("OVERLAY", "spectrumID, spectrumID, ..."),
  PEAKCALLBACKFUNCTIONNAME("PEAKCALLBACKFUNCTIONNAME"),
  PLOTAREACOLOR("PLOTAREACOLOR", "C"),
  PLOTCOLOR("PLOTCOLOR", "C"),
  PLOTCOLORS("PLOTCOLORS"),
  REVERSEPLOT("REVERSEPLOT", "TF"),
  SCALECOLOR("SCALECOLOR", "C"),
  SPECTRUM("SPECTRUM", "spectrumID"),
  SPECTRUMNUMBER("SPECTRUMNUMBER"),
  STARTINDEX("STARTINDEX"),
  SYNCCALLBACKFUNCTIONNAME("SYNCCALLBACKFUNCTIONNAME"),
  SYNCID("SYNCID"),
  TITLEBOLDON("TITLEBOLDON", "TF"),
  TITLECOLOR("TITLECOLOR", "C"),
  TITLEFONTNAME("TITLEFONTNAME", "fontName"),
  UNITSCOLOR("UNITSCOLOR", "C"),
  VERSION("VERSION"),
  XSCALEON("XSCALEON", "TF"),
  XUNITSON("XUNITSON", "TF"),
  YSCALE("YSCALE", "[ALL] lowValue highValue"),
  YSCALEON("YSCALEON", "TF"),
  YUNITSON("YUNITSON", "TF"),
  ZOOM("ZOOM", "OUT or x1,x2 or x1,y1 x2,y2");

  private String tip;

  public String getTip() {
    return "  " + (tip == "TF" ? "TRUE or FALSE" : tip == "C" ? "<color>" : tip);
  }

  private ScriptToken(String name) {
  }

  private ScriptToken(String name, String tip) {
    this.tip = tip;
  }

  public static Map<String, ScriptToken> htParams;

  public static ScriptToken getScriptToken(String name) {
    if (htParams == null) {
      htParams = new Hashtable<String, ScriptToken>();
      for (ScriptToken item : values())
        htParams.put(item.name(), item);
    }
    ScriptToken st = htParams.get(name.toUpperCase());
    return (st == null ? UNKNOWN : st);
  }

  public static List<ScriptToken> getScriptTokenList(String name, boolean isExact) {
    name = name.toUpperCase();
    List<ScriptToken> list = new ArrayList<ScriptToken>();
    ScriptToken st = getScriptToken(name);
    if (isExact) {
      if (st != null)
        list.add(st);
    } else {
      for (Entry<String, ScriptToken> entry: htParams.entrySet())
         if (entry.getKey().startsWith(name) && entry.getValue().tip != null)
           list.add(entry.getValue());
    }
    return list;
  }

  /**
   * tweak command options depending upon special cases
   * 
   * @param st
   * @param params
   * @param cmd
   * @return
   */
  public static String getValue(ScriptToken st, StringTokenizer params, String cmd) {
    if (!params.hasMoreTokens())
      return "";
    switch (st) {
    default:
      return nextStringToken(params, true);
    case LABEL:
    case LOAD:
    case CLOSE:
    case EXPORT:
    case INTEGRATE:
    case YSCALE:
      // take full command
      return removeCommandName(cmd);
    case OVERLAY:
    case ZOOM:
      // commas to spaces
      return removeCommandName(cmd).replace(',',' ').trim();
    }
  }

  private static String removeCommandName(String cmd) {
    int pt = cmd.indexOf(" ");
    if (pt < 0)
      return "";
    return cmd.substring(pt).trim();
  }

  public static String nextStringToken(StringTokenizer params, boolean removeQuotes) {
    String s = params.nextToken();
    if (s.charAt(0) != '"') 
      return s;
    if (s.endsWith("\""))
      return (removeQuotes ? TextFormat.trimQuotes(s) : s);
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

  public static String getNameList(List<ScriptToken> list) {
    if (list.size() == 0)
      return "";
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < list.size(); i++)
      sb.append(",").append(list.get(i));
    return sb.toString().substring(1);
  }

}