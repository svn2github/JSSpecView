package jspecview.common;

import java.text.DecimalFormat;

import jspecview.util.TextFormat;

/**
 * Stores information
 * about scale and range that <code>JSVPanel</code> needs to to display a
 * graph with a mutliple plots. You do not need to create an instance of this
 * class manually. Instead call the
 * {@link jspecview.common.MultiScaleData#generateScaleData(jspecview.common.Coordinate[][], int[], int[], int, int)}
 * .
 */
public class MultiScaleData extends ScaleData {

  /**
   * start inidices
   */
  public int[] startDataPointIndices;

  /**
   * end indices
   */
  public int[] endDataPointIndices;

  /**
   * number of points list
   */
  public int[] numOfPointsList;

  /**
   * Initialises a <code>MultiScaleData</code>
   */
  public MultiScaleData() {
    super();
  }

  /**
   * Initialises a <code>MultiScaleData</code> from another one
   * 
   * @param data
   *        the <code>MultiScaleData</code> to copy
   */
  public MultiScaleData(MultiScaleData data) {
    minX = data.minX;
    maxX = data.maxX;
    numInitXdiv = data.numInitXdiv;

    hashNumX = data.hashNumX;
    xStep = data.xStep;
    minXOnScale = data.minXOnScale;
    maxXOnScale = data.maxXOnScale;
    numOfXDivisions = data.numOfXDivisions;

    minY = data.minY;
    maxY = data.maxY;
    numInitYdiv = data.numInitYdiv;

    hashNumY = data.hashNumY;
    yStep = data.yStep;
    minYOnScale = data.minYOnScale;
    maxYOnScale = data.maxYOnScale;
    numOfYDivisions = data.numOfYDivisions;

    startDataPointIndices = data.startDataPointIndices;
    endDataPointIndices = data.endDataPointIndices;
    numOfPointsList = data.numOfPointsList;
  }

  /**
   * Calculates values that <code>JSVPanel</code> needs in order to render a
   * graph, (eg. scale, min and max values) and stores the values in the
   * class <code>ScaleData</code>.
   * 
   * @param coordLists
   *        an array of arrays of coordinates
   * @param startList
   *        the start indices
   * @param endList
   *        the end indices
   * @param initNumXDivisions
   *        the initial number of X divisions for scale
   * @param initNumYDivisions
   *        the initial number of Y divisions for scale
   * @return returns an instance of <code>MultiScaleData</code>
   */
  public static MultiScaleData generateScaleData(Coordinate[][] coordLists,
                                                 int[] startList,
                                                 int[] endList,
                                                 int initNumXDivisions,
                                                 int initNumYDivisions) {
  
    MultiScaleData data = new MultiScaleData();
  
    double spanX, spanY;
    double[] units = { 1.5, 2.0, 2.5, 4.0, 5.0, 8.0, 10.0 };
    DecimalFormat sciFormatter = TextFormat.getDecimalFormat("0.###E0");
  
    int indexOfE;
    double leftOfE;
    int rightOfE;
    int i;
  
    // startDataPointIndex and endDataPointIndex
    data.startDataPointIndices = startList;
    data.endDataPointIndices = endList;
    data.numInitXdiv = initNumXDivisions;
  
    int[] tmpList = new int[startList.length];
    for (int j = 0; j < startList.length; j++) {
      tmpList[j] = endList[j] - startList[j] + 1;
    }
    data.numOfPointsList = tmpList;
  
    // X Scale
    data.minX = Coordinate.getMinX(coordLists, startList, endList);
    data.maxX = Coordinate.getMaxX(coordLists, startList, endList);
    spanX = (data.maxX - data.minX) / initNumXDivisions;
    String strSpanX = sciFormatter.format(spanX);
    strSpanX = strSpanX.toUpperCase();
    indexOfE = strSpanX.indexOf('E');
    leftOfE = Double.parseDouble(strSpanX.substring(0, indexOfE));
    rightOfE = Integer.parseInt(strSpanX.substring(indexOfE + 1));
    data.hashNumX = rightOfE;
  
    i = 0;
    while (leftOfE > units[i] && i <= 6) {
      i++;
    }
  
    data.xStep = Math.pow(10, rightOfE) * units[i];
    data.minXOnScale = data.xStep * Math.floor((data.minX) / data.xStep);
    data.maxXOnScale = data.xStep * Math.ceil((data.maxX) / data.xStep);
    data.numOfXDivisions = (int) Math
        .ceil((data.maxXOnScale - data.minXOnScale) / data.xStep);
  
    // Find min and max x and y
  
    // Y Scale
    data.minY = Coordinate.getMinY(coordLists, startList, endList);
    data.maxY = Coordinate.getMaxY(coordLists, startList, endList);
    data.numInitYdiv = initNumYDivisions;
  
    if (data.minY == 0 && data.maxY == 0) {
      data.maxY = 1;
    }
  
    spanY = (data.maxY - data.minY) / initNumYDivisions;
    String strSpanY = sciFormatter.format(spanY);
    strSpanY = strSpanY.toUpperCase();
    indexOfE = strSpanY.indexOf('E');
    leftOfE = Double.parseDouble(strSpanY.substring(0, indexOfE));
    rightOfE = Integer.parseInt(strSpanY.substring(indexOfE + 1));
    data.hashNumY = rightOfE;
  
    i = 0;
    while (leftOfE > units[i] && i <= 6) {
      i++;
    }
  
    data.yStep = Math.pow(10, rightOfE) * units[i];
    data.minYOnScale = data.yStep * Math.floor((data.minY) / data.yStep);
    data.maxYOnScale = data.yStep * Math.ceil((data.maxY) / data.yStep);
    data.numOfYDivisions = (int) Math
        .ceil((data.maxYOnScale - data.minYOnScale) / data.yStep);
  
    return data;
  }
 }