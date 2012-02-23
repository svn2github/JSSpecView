package jspecview.common;

/**
 * Stores information
 * about scale and range that <code>JSVPanel</code> needs to to display a
 * graph with a mutliple plots. You do not need to create an instance of this
 * class manually. Instead call the
 * {@link jspecview.common.JSpecViewUtils#generateScaleData(jspecview.common.Coordinate[][], int[], int[], int, int)}
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

}