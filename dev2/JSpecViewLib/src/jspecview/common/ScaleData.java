package jspecview.common;

import java.text.DecimalFormat;

import jspecview.util.TextFormat;

/**
 * Stores information
 * about scale and range that <code>JSVPanel</code> needs to to display a
 * graph with a single plot. (For graphs that require multiple plots to be
 * overlaid.
 */
public class ScaleData {

  private final static double[] UNITS = { 1.5, 2.0, 2.5, 4.0, 5.0, 8.0, 10.0 };
  private final static DecimalFormat SCI_FORMATTER = TextFormat.getDecimalFormat("0.###E0");

  // X variables
  /**
   * The minimum X value in the list of coordinates of the graph
   */
  double minX;

  /**
   * The maximum X value in the list of coordinates of the graph
   */
  double maxX;

  /**
   * The preferred number of X division for the scale
   */
  protected int numInitXdiv;

  /**
   * never set -- always 0
   * 
   * The precision (number of decimal places) of the X and Y values
   */
  public int hashNums[] = new int[2];

  /**
   * The step value of X axis of the scale
   */
  public double xStep;

  /**
   * First grid x value
   */
  double firstX = Double.NaN;
  

  /**
   * the minimum X value on the scale
   */
  public double minXOnScale;

  /**
   * the maximim X value on the scale
   */
  public double maxXOnScale;

  // Y variables
  /**
   * The minimum Y value in the list of coordinates of the graph
   */
  double minY;

  /**
   * The maximum Y value in the list of coordinates of the graph
   */
  double maxY;

  double xFactorForScale;
  double yFactorForScale;
  
  /**
   * The preferred number of Y division for the scale  -- always 10
   */
  protected int numInitYdiv = 10;

  /**
   * The step value of Y axis of the scale
   */
  public double yStep;

  /**
   * the minimum Y value on the scale
   */
  public double minYOnScale;

  /**
   * the maximim Y value on the scale
   */
  public double maxYOnScale;

  // Other
  /**
   * The start index
   */
  int startDataPointIndex;

  /**
   * the end index
   */
  int endDataPointIndex;

  /**
   * The number of points
   */
  int numOfPoints;
	protected double initMinYOnScale;
	protected double initMaxYOnScale;
	protected double initMinY;
	protected double initMaxY;
	double firstY;

	/**
	 * Calculates values that <code>JSVPanel</code> needs in order to render a
	 * graph, (eg. scale, min and max values) and stores the values in the class
	 * <code>ScaleData</code>. Note: This method is not used in the application,
	 * instead the more general {@link jspecview.common.ViewData} is
	 * generated with the
	 * {@link jspecview.common.ViewData#generateScaleData(jspecview.common.Coordinate[][], int[], int[], int, int)}
	 * method
	 * 
	 * @param coords
	 *          the array of coordinates
	 * @param start
	 *          the start index
	 * @param end
	 *          the end index
	 * @param initNumXDivisions
	 *          the initial number of X divisions for scale
	 * @param initNumYDivisions
	 *          the initial number of Y divisions for scale
	 * @return returns an instance of <code>ScaleData</code>
	 */
	public ScaleData(Coordinate[] coords, int start, int end,
			int initNumXDivisions, int initNumYDivisions, boolean isContinuous) {
		minX = Coordinate.getMinX(coords, start, end);
		maxX = Coordinate.getMaxX(coords, start, end);
		minY = Coordinate.getMinY(coords, start, end);
		maxY = Coordinate.getMaxY(coords, start, end);
		numInitXdiv = initNumXDivisions;
		numInitYdiv = initNumYDivisions;
		setScale(isContinuous);
	}

//  /**
//   * never used or tested
//   * 
//   * Initialises a <code>ScaleData</code> from another one
//   * 
//   * @param data
//   *        the <code>ScaleData</code> to copy
//   */
//  ScaleData(ScaleData data) {
//    minX = data.minX;
//    maxX = data.maxX;
//    //numInitXdiv = data.numInitXdiv;
//
//    hashNums[0] = data.hashNums[0];
//    hashNums[1] = data.hashNums[1];
//    xStep = data.xStep;
//    firstX = data.firstX;
//    minXOnScale = data.minXOnScale;
//    maxXOnScale = data.maxXOnScale;
//    //numOfXDivisions = data.numOfXDivisions;
//
//    minY = data.minY;
//    maxY = data.maxY;
//   // numInitYdiv = data.numInitYdiv;
//
//    yStep = data.yStep;
//    minYOnScale = data.minYOnScale;
//    maxYOnScale = data.maxYOnScale;
//    //numOfYDivisions = data.numOfYDivisions;
//
//    startDataPointIndex = data.startDataPointIndex;
//    endDataPointIndex = data.endDataPointIndex;
//    numOfPoints = data.numOfPoints;
//  }

  ScaleData() {
	}

	protected ScaleData setScale(boolean isContinuous) {

    setXScale();
    if (!isContinuous)
      maxXOnScale += xStep / 2; // MS should not end with line at end

    // Y Scale
    
    setYScale(minY, maxY, true);
    return this;
  }

	protected void setXScale() {
    // X Scale
    xStep = getStep(minX, maxX, numInitXdiv, hashNums, 0);
    firstX = Math.floor(minX / xStep) * xStep;
    if (Math.abs((minX - firstX) / xStep) > 0.0001)
      firstX += xStep;
    minXOnScale = minX;
    maxXOnScale = maxX;
  }

  void setYScale(double minY, double maxY, boolean setScaleMinMax) {
    if (minY == 0 && maxY == 0)
      maxY = 1;
    yStep = getStep(minY, maxY, numInitYdiv, hashNums, 1);
    minYOnScale = (setScaleMinMax ? yStep * Math.floor(minY / yStep) : minY);
    maxYOnScale = (setScaleMinMax ? yStep * Math.ceil(maxY / yStep) : maxY);
    firstY = Math.floor(minY / yStep) * yStep;
    if (Math.abs((minY - firstY) / yStep) > 0.0001)
      firstY += yStep;
    if (setScaleMinMax) {
    	initMinYOnScale = minYOnScale;
    	initMaxYOnScale = maxYOnScale;
    	initMinY = minY;
    	initMaxY = maxY;
    }
	}

	void setScaleFactors(int xPixels, int yPixels) {
  	xFactorForScale = (maxXOnScale - minXOnScale) / xPixels;
	  yFactorForScale = (maxYOnScale - minYOnScale) / yPixels;
	}

	private static double getStep(double min, double max, int nDiv, int[] hashNums, int i) {
    double spanX = (max - min) / nDiv;
    String strSpanX = SCI_FORMATTER.format(spanX);
    strSpanX = strSpanX.toUpperCase();
    int indexOfE = strSpanX.indexOf('E');
    return getStepFromExponent(Double.parseDouble(strSpanX.substring(0, indexOfE)),
        (hashNums[i] = Integer.parseInt(strSpanX.substring(indexOfE + 1))));
  }

  private static double getStepFromExponent(double leftOfE, int rightOfE) {    
    int i = 0;
    while (leftOfE > UNITS[i] && i <= 6)
      i++;
    return Math.pow(10, rightOfE) * UNITS[i];
  }


  /**
   * Determines if the x coordinate is within the range of coordinates in the
   * coordinate list
   * 
   * @param x
   *        the x coodinate
   * @param scaleData TODO
   * @return true if within range
   */
  static boolean isWithinRange(double x, ScaleData scaleData) {
    if (x >= scaleData.minX && x <= scaleData.maxX)
      return true;
    return false;
  }

}