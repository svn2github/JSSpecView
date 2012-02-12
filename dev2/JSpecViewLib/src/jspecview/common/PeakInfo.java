package jspecview.common;

import jspecview.util.Parser;

public class PeakInfo {
	private double xMin, xMax, yMin, yMax;
	private String stringInfo;
	private String type;
  private String index;
	
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
	public void setStringInfo(String stringInfo) {
		this.stringInfo = stringInfo;
		type = Parser.getQuotedAttribute(stringInfo, "type");
    index = Parser.getQuotedAttribute(stringInfo, "index");
	}
	
	public String getStringInfo() {
		return stringInfo;
	}

  public String getIndex() {
    return index;
  }

}
