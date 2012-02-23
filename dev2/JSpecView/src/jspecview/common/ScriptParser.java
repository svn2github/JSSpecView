package jspecview.common;

import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

public class ScriptParser {

  public enum ScriptToken {

    UNKNOWN("?"),
    APPLETID("APPLETID"),
    APPLETREADYCALLBACKFUNCTIONNAME("APPLETREADYCALLBACKFUNCTIONNAME"),
    BACKGROUNDCOLOR("BACKGROUNDCOLOR"),
    COMPOUNDMENUON("COMPOUNDMENUON"),
    COORDCALLBACKFUNCTIONNAME("COORDCALLBACKFUNCTIONNAME"),
    COORDINATESCOLOR("COORDINATESCOLOR"),
    COORDINATESON("COORDINATESON"),
    DEBUG("DEBUG"),
    DISPLAYFONTNAME("DISPLAYFONTNAME"),
    ENABLEZOOM("ENABLEZOOM"),
    ENDINDEX("ENDINDEX"),
    GRIDCOLOR("GRIDCOLOR"),
    GRIDON("GRIDON"),
    INTEGRALPLOTCOLOR("INTEGRALPLOTCOLOR"),
    INTEGRATIONRATIOS("INTEGRATIONRATIOS"),
    INTERFACE("INTERFACE"),
    IRMODE("IRMODE"),
    LOAD("LOAD"),
    MENUON("MENUON"),
    OBSCURE("OBSCURE"),
    PEAKCALLBACKFUNCTIONNAME("PEAKCALLBACKFUNCTIONNAME"),
    PLOTAREACOLOR("PLOTAREACOLOR"),
    PLOTCOLOR("PLOTCOLOR"),
    PLOTCOLORS("PLOTCOLORS"),
    REVERSEPLOT("REVERSEPLOT"),
    SCALECOLOR("SCALECOLOR"),
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
  }

  public static String getValue(ScriptToken st, StringTokenizer params) {
    switch (st) {
    default:
      return params.nextToken();
    case ZOOM:
      String x1 = params.nextToken();
      if (x1.equalsIgnoreCase("out"))
        return "0,0";
      String x2 = null;
      int pt = x1.indexOf(",");
      if (pt >= 0) {
        if (x1.endsWith(",")) {
          x1 = x1.substring(0, x1.length() - 1);
          x2 = params.nextToken();
        } else {
          x2 = x1.substring(pt + 1, x1.length());
          x1 = x1.substring(0, pt);
        }
      } else {
        x2 = params.nextToken();
        if (x2.equals(",")) {
          x2 = params.nextToken();
        } else if (x2.startsWith(",")) {
          x2 = x2.substring(1);
        }
      }
      return x1 + "," + x2;
    }
  }

  public static String getKey(StringTokenizer eachParam) {
    String key = eachParam.nextToken();
    if (key.equalsIgnoreCase("SET"))
      key = eachParam.nextToken();
    return key.toUpperCase();
  }
}
