package jspecview.common;

import java.util.StringTokenizer;

import jspecview.util.TextFormat;

public class ScriptParser {

  public static String getValue(ScriptToken st, StringTokenizer params, String token) {
    if (!params.hasMoreTokens())
      return "";
    int pt;
    switch (st) {
    default:
      return nextStringToken(params);
    case CLOSE:
    case LOAD:
      pt = token.indexOf(" ");
      if (pt < 0)
        return "";
      String s = token.substring(pt).trim();
      return (s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s);        
    case OVERLAY:
      pt = token.indexOf(" ");
      if (pt < 0)
        return "";
      return TextFormat.simpleReplace(token.substring(pt).trim(), " ", "");      
    case ZOOM:
      String x1 = nextStringToken(params);
      pt = token.indexOf(" ");
      if (pt < 0 || x1.equalsIgnoreCase("out"))
        return "0,0";
      return TextFormat.simpleReplace(token.substring(pt).trim(), " ", "");
    }
  }

  private static String nextStringToken(StringTokenizer params) {
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
