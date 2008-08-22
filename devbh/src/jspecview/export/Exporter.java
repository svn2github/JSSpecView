package jspecview.export;

import java.io.IOException;

import jspecview.api.ExporterInterface;
import jspecview.common.JDXSpectrum;

public class Exporter implements ExporterInterface {

  static final int DIF = 0;
  static final int FIX = 1;
  static final int SQZ = 2;
  static final int PAC = 3;
  static final int XY = 4;

  public Exporter() {
    // implemented via ExporterInterface
  }

  public String export(String type, String path, JDXSpectrum spec, int startIndex, int endIndex)
      throws IOException {
    if (type.equals("SVG")) {
      (new SVGExporter()).exportAsSVG(path, spec, startIndex, endIndex);
    } else if (type.equals("CML")) {
      (new CMLExporter()).exportAsCML(path, spec, startIndex, endIndex);
    } else if (type.equals("AML")) {
      (new AMLExporter()).exportAsAnIML(path, spec, startIndex, endIndex);
    } else {
      int iType = (type.equals("XY") ? XY : type.equals("DIF") ? DIF : type
          .equals("FIX") ? FIX : type.equals("SQZ") ? SQZ
          : type.equals("PAC") ? PAC : -1);
      if (iType >= 0)
        JDXExporter.export(iType, path, spec, startIndex, endIndex);
    }
    return null;
  }
}
