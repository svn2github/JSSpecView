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
class PlotWidget {

  double x, y;
  int xPixel0;
  int yPixel0;
  int xPixel1;
  int yPixel1;
  boolean isPin;

  PlotWidget(boolean isPin) {
    this.isPin = isPin;
  }

  boolean selected(int xPixel, int yPixel) {
    return (Math.abs(xPixel - xPixel0) < 5 && Math.abs(yPixel - yPixel0) < 5);
  }

  public void setX(double x, int xPixel) {
    this.x = x;
    xPixel0 = xPixel1 = xPixel;
  }

  public void setY(double y, int yPixel) {
    this.y = y;
    yPixel0 = yPixel1 = yPixel;
  }

}