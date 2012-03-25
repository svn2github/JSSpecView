package jspecview.common;

import jspecview.util.Logger;
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
  public Graph spectrum;

  //private String atoms;

	
	public PeakInfo() {
	}

	public boolean isClearAll() {
	  return (spectrum == null);
	}
	
  public String getType() {
    return type;
  }
  
	public void setXMax(double xMax) {
		this.xMax = xMax;
	}
	public double getXMax() {
		return xMax;
	}
	public void setXMin(double xMin) {
		this.xMin = xMin;
	}
	public double getXMin() {
		return xMin;
	}
	public void setYMin(double yMin) {
		this.yMin = yMin;
	}
	public double getYMin() {
		return yMin;
	}
	public void setYMax(double yMax) {
		this.yMax = yMax;
	}
	public double getYMax() {
		return yMax;
	}
	
	public double getX() {
	  return (xMax + xMin) / 2;
	}
	
	public void setStringInfo(String stringInfo) {
	  Logger.info("JSpecView found " + stringInfo);
		this.stringInfo = stringInfo;
		type = Parser.getQuotedAttribute(stringInfo, "type").toUpperCase();
		type2 = fixType(type.substring(type.indexOf('/') + 1));
		if (type2.length() > 0)
		  type = fixType(type.substring(0, type.indexOf('/'))) + "/" + type2;
		else
		  type = fixType(type);
    index = Parser.getQuotedAttribute(stringInfo, "index");
    file = Parser.getQuotedAttribute(stringInfo, "file");
    model = Parser.getQuotedAttribute(stringInfo, "model");
   // atoms = Parser.getQuotedAttribute(stringInfo, "atoms");
    title = Parser.getQuotedAttribute(stringInfo, "title");
	}

	private static String fixType(String type) {
    return (type.equals("HNMR") ? "1HNMR" : type.equals("CNMR") ? "13CNMR" : type);
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

  public boolean checkTypeModel(String type, String model) {
    return (type != null && model != null 
        && (type.endsWith(this.type)) //   GC/MS matches MS
        && model.equals(this.model));
  }

  public String getModel() {
    return model;
  }

  public String getFilePath() {
    return file;
  }

  /**
   * a spectrum which, when loaded, should fire a message to load first peak
   * -- GC for now
   * 
   * @return
   */
  public boolean autoSelectOnLoad() {
    return (type.equalsIgnoreCase("GC"));
  }

}
