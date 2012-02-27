/**
 * 
 */
package jspecview.common;

import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import jspecview.util.TextFormat;

public enum ScriptToken {

  UNKNOWN("?"),
  APPLETID("APPLETID"),
  APPLETREADYCALLBACKFUNCTIONNAME("APPLETREADYCALLBACKFUNCTIONNAME"),
  AUTOINTEGRATE("AUTOINTEGRATE"),
  BACKGROUNDCOLOR("BACKGROUNDCOLOR"),
  CLOSE("CLOSE"),
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
  LOAD("LOAD"),
  MENUON("MENUON"),
  OBSCURE("OBSCURE"),
  OVERLAY("OVERLAY"),
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

  public static String getValue(ScriptToken st, StringTokenizer params, String token) {
    if (!params.hasMoreTokens())
      return "";
    int pt;
    switch (st) {
    default:
      return nextStringToken(params);
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
      String x1 = nextStringToken(params);
      pt = token.indexOf(" ");
      if (pt < 0 || x1.equalsIgnoreCase("out"))
        return "0,0";
      return TextFormat.simpleReplace(token.substring(pt).trim(), " ", "");
    }
  }

  static String nextStringToken(StringTokenizer params) {
    String s = params.nextToken();
    if (s.charAt(0) != '"') 
      return s;
    if (s.endsWith("\""))
      return s.substring(1, s.length() - 1);
    StringBuffer sb = new StringBuffer(s);
    s = null;
    while (params.hasMoreTokens() && !(s = params.nextToken()).endsWith("\"")) {
      sb.append(" ").append(s);
      s = null;
    }
    if (s != null)
      sb.append(s.substring(0, s.length() - 1));
    return sb.toString();
  }

  public static String getKey(StringTokenizer eachParam) {
    String key = eachParam.nextToken();
    if (key.equalsIgnoreCase("SET"))
      key = eachParam.nextToken();
    return key.toUpperCase();
  }
}