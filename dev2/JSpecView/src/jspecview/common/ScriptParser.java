package jspecview.common;

import java.util.StringTokenizer;

public class ScriptParser {

  public static String getValue(ScriptToken st, StringTokenizer params) {
    if (!params.hasMoreTokens())
      return "";
    switch (st) {
    default:
      return nextStringToken(params);
    case ZOOM:
      String x1 = nextStringToken(params);
      if (x1.equalsIgnoreCase("out"))
        return "0,0";
      String x2 = null;
      int pt = x1.indexOf(",");
      if (pt >= 0) {
        if (x1.endsWith(",")) {
          x1 = x1.substring(0, x1.length() - 1);
          x2 = nextStringToken(params);
        } else {
          x2 = x1.substring(pt + 1, x1.length());
          x1 = x1.substring(0, pt);
        }
      } else {
        x2 = nextStringToken(params);
        if (x2.equals(",")) {
          x2 = nextStringToken(params);
        } else if (x2.startsWith(",")) {
          x2 = x2.substring(1);
        }
      }
      return x1 + "," + x2;
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
