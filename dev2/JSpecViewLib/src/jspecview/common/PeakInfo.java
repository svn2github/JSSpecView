package jspecview.common;

//import jspecview.util.Logger;
import jspecview.util.Parser;

public class PeakInfo {
  public final static PeakInfo nullPeakInfo = new PeakInfo();

  private double xMin, xMax, yMin, yMax;
  private String stringInfo;
  private String type;
  private String type2;
  private String index;
  private String file;
  private String title;
  private String model;
  private String atoms;
  public JDXSpectrum spectrum;

  private String _match;


  public PeakInfo() {
  }

  public PeakInfo(String s) {
    stringInfo = s;
    type = Parser.getQuotedAttribute(s, "type");
    if (type == null)
      type = "";
    type = type.toUpperCase();
    int pt = type.indexOf('/');
    type2 = (pt < 0 ? "" : fixType(type.substring(type.indexOf('/') + 1)));
    if (pt >= 0)
      type = fixType(type.substring(0, pt)) + "/" + type2;
    else
      type = fixType(type);
    index = Parser.getQuotedAttribute(s, "index");
    file = Parser.getQuotedAttribute(s, "file");
    model = Parser.getQuotedAttribute(s, "model");
    boolean isBaseModel = s.contains("baseModel=\"\"");
    if (!isBaseModel)
      atoms = Parser.getQuotedAttribute(s, "atoms");
    title = Parser.getQuotedAttribute(s, "title");
    _match = Parser.getQuotedAttribute(s, "_match"); // PEAK command creates this
    xMax = Parser.parseFloat(Parser.getQuotedAttribute(s, "xMax"));
    xMin = Parser.parseFloat(Parser.getQuotedAttribute(s, "xMin"));
    yMax = Parser.parseFloat(Parser.getQuotedAttribute(s, "yMax"));
    yMin = Parser.parseFloat(Parser.getQuotedAttribute(s, "yMin"));
  }

  public boolean isClearAll() {
    return (spectrum == null);
  }

  public String getType() {
    return type;
  }

  public String getAtoms() {
    return atoms;
  }

  public double getXMax() {
    return xMax;
  }

  public double getXMin() {
    return xMin;
  }

  public double getYMin() {
    return yMin;
  }

  public double getYMax() {
    return yMax;
  }

  public double getX() {
    return (xMax + xMin) / 2;
  }

  public String getMatch() {
    return _match;
  }
  
  private static String fixType(String type) {
    return (type.equals("HNMR") ? "1HNMR" : type.equals("CNMR") ? "13CNMR"
        : type);
  }

  @Override
  public String toString() {
    return stringInfo;
  }

  public String getIndex() {
    return index;
  }

  public String getTitle() {
    return title;
  }

  public boolean checkFileIndex(String filePath, String sIndex) {
    return (sIndex.equals(index) && filePath.equals(file));
  }

  /**
   * type checks true for MS in GC/MS; reverse of checkType
   * 
   * @param filePath
   * @param type
   * @param model
   * @return true for MS in GC/MS; reverse of checkType
   */
	public boolean checkFileTypeModel(String filePath, 
			String type, String model) {
		return filePath.equals(file) && checkModel(model)
		  && this.type.endsWith(type);
	}

  public boolean checkTypeModel(String type, String model) {
    return checkType(type) //   GC/MS matches MS
        && checkModel(model);
  }

  private boolean checkModel(String model) {
    return (model != null && model.equals(this.model));
  }

  private boolean checkType(String type) {
    return (type.endsWith(this.type));
  }

  public boolean checkTypeMatch(PeakInfo pi) {
    return (checkType(pi.type) 
        && (checkModel(pi._match) || title.toUpperCase().indexOf(pi._match) >= 0));
  }

  public String getModel() {
    return model;
  }

  public String getFilePath() {
    return file;
  }

  /**
   * a spectrum which, when loaded, should fire a message to load first peak --
   * GC for now
   * 
   * @return can autoselect on loading
   */
  public boolean autoSelectOnLoad() {
    return (type.startsWith("GC"));
  }

}
