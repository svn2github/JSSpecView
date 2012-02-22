package jspecview.application.common;

import java.util.Hashtable;
import java.util.Map;

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
    YUNITSON("YUNITSON");

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

}
