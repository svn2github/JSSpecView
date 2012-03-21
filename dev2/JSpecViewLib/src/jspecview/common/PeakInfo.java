package jspecview.common;

import jspecview.util.Logger;
import jspecview.util.Parser;

public class PeakInfo {
	private double xMin, xMax, yMin, yMax;
	private String stringInfo;
	private String type;
  private String index;
	private String file;
	
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
		type = Parser.getQuotedAttribute(stringInfo, "type");
    index = Parser.getQuotedAttribute(stringInfo, "index");
    file = Parser.getQuotedAttribute(stringInfo, "file");
	}

	@Override
	public String toString() {
		return stringInfo;
	}

  public String getIndex() {
    return index;
  }

  String title;

  public String getTitle() {
    if (title == null)
      title = Parser.getQuotedAttribute(stringInfo, "title");
    return title;
  }

  public boolean check(String filePath, String sIndex) {
    return (index.equals(sIndex) && file.equals(filePath));
  }

}
