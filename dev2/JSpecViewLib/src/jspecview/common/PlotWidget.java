package jspecview.common;

/**
 * Stores information about pins and zoom boxes,
 * which can be locked upon mousePressed events
 * and released upon mouseReleased events
 * We need actual x and y as well as pixel positions here
 * 
 * {@link jspecview.common.ScaleData#generateScaleData(jspecview.common.Coordinate[], int, int, int, int)}
 * .
 */
class PlotWidget extends Coordinate {

  int xPixel0;
  int yPixel0;
  int xPixel1;
  int yPixel1;
  boolean isPin;
  boolean isXtype;
  boolean is2D;
  private String name;
  
  @Override
  public String toString() {
    return name
        + (!isPin ? "" + xPixel0 + " " + yPixel0 + " / " + xPixel1 + " "
            + yPixel1 : " x=" + getXVal() + "/" + xPixel0 + " y=" + getYVal()
            + "/" + yPixel0);
  }
 
  public PlotWidget(String name) {
    this.name = name;
    isPin = (name.charAt(0) == 'p');
    isXtype = (name.indexOf("x") >= 0);
    is2D = (name.indexOf("2D") >= 0);
  }

  boolean selected(int xPixel, int yPixel) {
    return (Math.abs(xPixel - xPixel0) < 5 && Math.abs(yPixel - yPixel0) < 5);
  }

  public void setX(double x, int xPixel) {
    setXVal(x);
    xPixel0 = xPixel1 = xPixel;
  }

  public void setY(double y, int yPixel) {
    setYVal(y);
    yPixel0 = yPixel1 = yPixel;
  }

  public double getValue() {
    return (isXtype ? getXVal() : getYVal());
  }

}