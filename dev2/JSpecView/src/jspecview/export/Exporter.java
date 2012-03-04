package jspecview.export;

import java.io.IOException;

import jspecview.common.JDXSpectrum;

public class Exporter {

  static final int UNK = -1;
  static final int DIF = 0;
  static final int FIX = 1;
  static final int SQZ = 2;
  static final int PAC = 3;
  static final int XY = 4;
  static final int DIFDUP = 5;
  static final int PNG = 6;
  static final int JPG = 7;
  static final int SVG = 8;
  static final int CML = 9;
  static final int AML = 10;

  private static final String TYPES = " DIF      " // 0
                                     +" FIX      " // 10
                                     +" SQZ      " // 20
                                     +" PAC      " // 30
                                     +" XY       " // 40
                                     +" DIFDUP   " // 50
                                     +" PNG      " // 60
                                     +" JPG      " // 70
                                     +" SVG      " // 80
                                     +" CML      " // 90
                                     +" AML      " //100
                                     +" AnIML    " //110
                                     +" XML      ";//120
  
  public Exporter() {
  }

  public static int getType(String type) {
    int pt = TYPES.indexOf(" " + type.toUpperCase() + " ");
    if (pt < 0)
      return UNK;
    return Math.min(pt / 10, AML);
  }
  
  public static String export(String type, String path, JDXSpectrum spec, int startIndex, int endIndex)
      throws IOException {
    int iType = getType(type);
    switch (iType) {
    case XY:
    case DIF:
    case DIFDUP:
    case FIX:
    case PAC:
    case SQZ:
      return JDXExporter.export(iType, path, spec, startIndex, endIndex);      
    case SVG:
      return (new SVGExporter()).exportAsSVG(path, spec, startIndex, endIndex);
    case CML:
      return (new CMLExporter()).exportAsCML(path, spec, startIndex, endIndex);
    case AML:
      return (new AMLExporter()).exportAsAnIML(path, spec, startIndex, endIndex);
    default:
      return null;
    }
  }

  public static boolean isExportMode(String ext) {
    return (getType(ext) >= 0);
  }
}
