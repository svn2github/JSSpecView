package jspecview.export;

import java.io.IOException;

import jspecview.common.JDXSpectrum;

public class Exporter {

  static final int DIF = 0;
  static final int FIX = 1;
  static final int SQZ = 2;
  static final int PAC = 3;
  static final int XY = 4;

  public Exporter() {
  }

  public static String export(String type, String path, JDXSpectrum spec, int startIndex, int endIndex)
      throws IOException {
    if (type.equals("SVG")) {
      return (new SVGExporter()).exportAsSVG(path, spec, startIndex, endIndex);
    } else if (type.equals("CML")) {
      return (new CMLExporter()).exportAsCML(path, spec, startIndex, endIndex);
    } else if (type.equals("XML") || type.equals("AML") || type.equals("AnIML")) {
      return (new AMLExporter()).exportAsAnIML(path, spec, startIndex, endIndex);
    } else {
      int iType = (type.equals("XY") ? XY : type.equals("DIF") ? DIF : type
          .equals("FIX") ? FIX : type.equals("SQZ") ? SQZ
          : type.equals("PAC") ? PAC : -1);
      if (iType >= 0)
        return JDXExporter.export(iType, path, spec, startIndex, endIndex);
    }
    return null;
  }
}
