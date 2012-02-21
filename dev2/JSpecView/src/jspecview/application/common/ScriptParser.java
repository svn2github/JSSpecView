package jspecview.application.common;

import java.util.Hashtable;

public class ScriptParser {
  
  public static final String[] params = { "LOAD", "REVERSEPLOT",
      "COORDINATESON", "GRIDON", "COORDCALLBACKFUNCTIONNAME", "SPECTRUMNUMBER",
      "INTERFACE", "ENDINDEX", "ENABLEZOOM", "STARTINDEX", "MENUON",
      "COMPOUNDMENUON", "BACKGROUNDCOLOR", "COORDINATESCOLOR", "GRIDCOLOR",
      "PLOTAREACOLOR", "PLOTCOLOR", "SCALECOLOR", "TITLECOLOR", "UNITSCOLOR",
      "PLOTCOLORS", "VERSION", "PEAKCALLBACKFUNCTIONNAME", "IRMODE", "OBSCURE",
      "XSCALEON", "YSCALEON", "XUNITSON", "YUNITSON", "INTEGRALPLOTCOLOR",
      "TITLEFONTNAME", "TITLEBOLDON", "DISPLAYFONTNAME", "INTEGRATIONRATIOS",
      "APPLETREADYCALLBACKFUNCTIONNAME", "APPLETID", "SYNCID",
      "SYNCCALLBACKFUNCTIONNAME" };

  final public static int PARAM_LOAD = 0;
  final public static int PARAM_REVERSEPLOT = 1;
  final public static int PARAM_COORDINATESON = 2;
  final public static int PARAM_GRIDON = 3;
  final public static int PARAM_COORDCALLBACKFUNCTIONNAME = 4;
  final public static int PARAM_SPECTRUMNUMBER = 5;
  final public static int PARAM_INTERFACE = 6;
  final public static int PARAM_ENDINDEX = 7;
  final public static int PARAM_ENABLEZOOM = 8;
  final public static int PARAM_STARTINDEX = 9;
  final public static int PARAM_MENUON = 10;
  final public static int PARAM_COMPOUNDMENUON = 11;
  final public static int PARAM_BACKGROUNDCOLOR = 12;
  final public static int PARAM_COORDINATESCOLOR = 13;
  final public static int PARAM_GRIDCOLOR = 14;
  final public static int PARAM_PLOTAREACOLOR = 15;
  final public static int PARAM_PLOTCOLOR = 16;
  final public static int PARAM_SCALECOLOR = 17;
  final public static int PARAM_TITLECOLOR = 18;
  final public static int PARAM_UNITSCOLOR = 19;
  final public static int PARAM_PLOTCOLORS = 20;
  final public static int PARAM_VERSION = 21;
  final public static int PARAM_PEAKCALLBACKFUNCTIONNAME = 22;
  final public static int PARAM_IRMODE = 23;
  final public static int PARAM_OBSCURE = 24;
  final public static int PARAM_XSCALEON = 25;
  final public static int PARAM_YSCALEON = 26;
  final public static int PARAM_XUNITSON = 27;
  final public static int PARAM_YUNITSON = 28;
  final public static int PARAM_INTEGRALPLOTCOLOR = 29;
  final public static int PARAM_TITLEFONTNAME = 30;
  final public static int PARAM_TITLEBOLDON = 31;
  final public static int PARAM_DISPLAYFONTNAME = 32;
  final public static int PARAM_INTEGRATIONRATIOS = 33;
  final public static int PARAM_APPLETREADYCALLBACKFUNCTIONNAME = 34;
  final public static int PARAM_APPLETID = 35;
  final public static int PARAM_SYNCID = 36;
  final public static int PARAM_SYNCCALLBACKFUNCTIONNAME = 37;

  final public static Hashtable<String, Integer> htParams = new Hashtable<String, Integer>();
  static {
    for (int i = 0; i < params.length; i++)
      htParams.put(params[i], new Integer(i));
  }

  //"ADDHIGHLIGHT", "REMOVEHIGHLIGHT", "REMOVEALLHIGHTLIGHTS"

}
