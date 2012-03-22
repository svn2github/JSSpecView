package jspecview.common;

public interface Panel {

  // static parameters
  static int minNumOfPointsForZoom = 3;


  GraphSet newGraphSet();


  int getNumberOfSpectraTotal();

}
