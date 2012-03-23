/**
 * 
 */
package jspecview.common;

public class Integral {
  double value;
  double x1;
  double x2;
  double y1;
  double y2;

  Integral(double value, double x1, double x2, double y1, double y2) {
    this.value = value;
    this.x1 = x1;
    this.x2 = x2;
    this.y1 = y1;
    this.y2 = y2;
  }
  
  @Override
  public String toString() {
    return "integral val=" + value + " " + x1 + " " + x2;
  }
}