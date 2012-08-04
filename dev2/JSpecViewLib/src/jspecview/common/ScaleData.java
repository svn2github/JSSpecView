package jspecview.common;

import java.text.DecimalFormat;
import java.util.Map;

import jspecview.util.TextFormat;

/**
 * Stores information
 * about scale and range that <code>JSVPanel</code> needs to to display a
 * graph with a single plot. (For graphs that require multiple plots to be
 * overlaid.
 */
public class ScaleData {

  private final static int[] NTICKS = { 2, 5, 10, 10 };
  private final static double[] LOGTICKS = { Math.log10(2), Math.log10(5), 0, 1 };

	static void fixScale(Map<Double, String> map) {
		if (map.isEmpty())
			return;
		while (true) {
			for (Map.Entry<Double, String> entry : map.entrySet()) {
				String s = entry.getValue();
				int pt = s.indexOf("E");
				if (pt >= 0)
					s = s.substring(0, pt);
				if (s.indexOf(".") < 0)
					return;
				if (!s.endsWith("0") && !s.endsWith("."))
					return;
			}
			for (Map.Entry<Double, String> entry : map.entrySet()) {
				String s = entry.getValue();
				int pt = s.indexOf("E");
				if (pt >= 0)
  				entry.setValue(s.substring(0, pt - 1) + s.substring(pt));
				else
  				entry.setValue(s.substring(0, s.length() - 1));
			}			
		}
	}



	/**
	 * for NMR, the adjustment in chemical shift applied using SHIFTX or SETPEAK or SETX
	 */
	
	double specShift;
  
  /**
   * The minimum X value in the list of coordinates of the graph
   */
  double minX;

  /**
   * The maximum X value in the list of coordinates of the graph
   */
  double maxX;

  /**
   * 
   * The precision (number of decimal places) of the X and Y values
   */
  public int hashNums[] = new int[2];


  /**
   * The formatter for the X and Y scales
   */
	public DecimalFormat[] formatters = new DecimalFormat[2];
	

  /**
   * The step values for the X and Y scales
   */
  public double[] steps = new double[2];

  /**
   * the minor tick counts for the X and Y scales
   */
	int[] minorTickCounts = new int[2];

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

  protected double xFactorForScale;
  protected double yFactorForScale;
  
  protected double spectrumScaleFactor = 1;
  protected double spectrumYRef = 0;
  protected double userYFactor = 1;
  
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
  int pointCount;
  
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
	 * @param isContinuous 
	 * @param isInverted 
	 * @returns an instance of <code>ScaleData</code>
	 */
	public ScaleData(Coordinate[] coords, int start, int end, boolean isContinuous, boolean isInverted) {
		minX = Coordinate.getMinX(coords, start, end);
		maxX = Coordinate.getMaxX(coords, start, end);
		minY = Coordinate.getMinY(coords, start, end);
		if (minY > 0 && !isContinuous)
			minY = 0; // assumed to be MS data -- we want 0 to be at the bottom
		maxY = Coordinate.getMaxY(coords, start, end);
		setScale(isContinuous, isInverted);
	}

  ScaleData() {
	}

	protected ScaleData(int iStart, int iEnd) {
		startDataPointIndex = iStart;
		endDataPointIndex = iEnd;
		pointCount = endDataPointIndex - startDataPointIndex + 1;
	}

	protected ScaleData setScale(boolean isContinuous, boolean isInverted) {

    setXScale();
    if (!isContinuous)
      maxXOnScale += steps[0] / 2; // MS should not end with line at end

    // Y Scale
    
    setYScale(minY, maxY, true, isInverted);
    return this;
  }

	protected void setXScale() {
    // X Scale
    double xStep = setScaleParams(minX, maxX, 0);
    firstX = Math.floor(minX / xStep) * xStep;
    if (Math.abs((minX - firstX) / xStep) > 0.0001)
      firstX += xStep;
    minXOnScale = minX;
    maxXOnScale = maxX;
  }

  boolean isYZeroOnScale() {
    return (minYOnScale < 0 && maxYOnScale > 0);
  }

	void setYScale(double minY, double maxY, boolean setScaleMinMax,
			boolean isInverted) {
		if (minY == 0 && maxY == 0)
			maxY = 1;
		double yStep = setScaleParams(minY, maxY, 1);
		double dy = (isInverted ? yStep / 2 : yStep / 4);
		double dy2 = (isInverted ? yStep / 4 : yStep / 2);
		minYOnScale = (minY == 0 ? 0 : setScaleMinMax ? dy * Math.floor(minY / dy)
				: minY);
		maxYOnScale = (setScaleMinMax ? dy2 * Math.ceil(maxY * 1.05 / dy2) : maxY);
		firstY = (minY == 0 ? 0 : Math.floor(minY / dy) * dy);
		if (minYOnScale < 0 && maxYOnScale > 0) {
			// set a Y value to be 0;
			firstY = 0;
			while (firstY - yStep > minYOnScale)
				firstY -= yStep;
		} else if (minYOnScale != 0 && Math.abs((minY - firstY) / dy) > 0.0001) {
			firstY += dy;
		}

		if (setScaleMinMax) {
			initMinYOnScale = minYOnScale;
			initMaxYOnScale = maxYOnScale;
			initMinY = minY;
			initMaxY = maxY;
		}
	}

	protected void scale2D(double f) {
		double dy = maxY - minY;
		if (f == 1) {
			maxY = initMaxY;
			minY = initMinY;
			return;
		}
		maxY = minY + dy / f;
	}

  void setXRange(double x1, double x2) {
    minX = x1;
    maxX = x2;
    setXScale();
  }

  protected static int setXRange(int i, Coordinate[] xyCoords, double initX, double finalX, int iStart, int iEnd, int[] startIndices, int[] endIndices) {
    int index = 0;
    int ptCount = 0;
    for (index = iStart; index <= iEnd; index++) {
      double x = xyCoords[index].getXVal();
      if (x >= initX) {
        startIndices[i] = index;
        break;
      }
    }

    // determine endDataPointIndex
    for (; index <= iEnd; index++) {
      double x = xyCoords[index].getXVal();
      ptCount++;
      if (x >= finalX) {
        break;
      }
    }
    endIndices[i] = index - 1;
    return ptCount;
  }

  /**
   * sets hashNums, formatters, and steps 
   * @param min
   * @param max
   * @param i   0 for X; 1 for Y
   * @return  steps[i]
   */
	private double setScaleParams(double min, double max, int i) {
		// nDiv will be 14, which seems to work well
    double dx = (max == min ? 1 : Math.abs(max - min) / 14);
		double log = Math.log10(Math.abs(dx));
		
		int exp = (int) Math.floor(log);
		
		// set number of decimal places
		hashNums[i] = exp;

		// set number formatter
		String hash1 = "0.00000000";
		String hash = (
				exp <= 0 ? hash1.substring(0, Math.min(hash1.length(), Math.abs(exp) + 3))
				: exp > 3 ? "" 
				: "#");
		formatters[i] = TextFormat.getDecimalFormat(hash);
		
		// set step for numbers
    int j = 0;
    double dec = Math.pow(10, log - exp);
    while (dec > NTICKS[j]) {
      j++;
      if (j > 3)
      	System.out.println("OHOH");
    }
    steps[i] = Math.pow(10, exp) * NTICKS[j];
    
    // set minor ticks count
		log = Math.log10(Math.abs(steps[i] * 1.0001e5));
		double mantissa = log - Math.floor(log);
		int n = 0;
		for (j = 0; j < NTICKS.length; j++)
			if (Math.abs(mantissa - LOGTICKS[j]) < 0.001) {
				n = NTICKS[j];
				break;
			}
		minorTickCounts[i] = n;
		
		return steps[i];
		
  }

	/**
   * Determines if the x coordinate is within the range of coordinates in the
   * coordinate list
   * 
   * @param x
   * @return true if within range
   */
  boolean isInRangeX(double x) {
    return (x >= minX && x <= maxX);
  }

	public void addSpecShift(double dx) {
		specShift += dx;
		minX += dx;
		maxX += dx;
		minXOnScale += dx;
		maxXOnScale += dx;
		firstX += dx;
	}

  double minY2D, maxY2D;

//  void setMinMaxY2D(List<JDXSpectrum> subspectra) {
//    minY2D = Double.MAX_VALUE;
//    maxY2D = -Double.MAX_VALUE;
//    for (int i = subspectra.size(); --i >= 0; ) {
//      double d = subspectra.get(i).getY2D();
//      if (d < minY2D)
//        minY2D = d;
//      else if (d > maxY2D)
//        maxY2D = d;
//    }
//  }

	public Map<String, Object> getInfo(Map<String, Object> info) {
		info.put("specShift", Double.valueOf(specShift));
		info.put("minX", Double.valueOf(minX));
		info.put("maxX", Double.valueOf(maxX));
		info.put("minXOnScale", Double.valueOf(minXOnScale));
		info.put("maxXOnScale", Double.valueOf(maxXOnScale));
		info.put("minY", Double.valueOf(minY));
		info.put("maxY", Double.valueOf(maxY));
		info.put("minYOnScale", Double.valueOf(minYOnScale));
		info.put("maxYOnScale", Double.valueOf(maxYOnScale));
		info.put("minorTickCountX", Integer.valueOf(minorTickCounts[0]));
		info.put("xStep", Double.valueOf(steps[0]));
		return info;
	}

	public void setMinMax(double minX, double maxX, double minY, double maxY) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
	}
	
	double toY(int yPixel, int yPixel0) {		
		return maxYOnScale + (yPixel0 - yPixel) * yFactorForScale;
	}

	double toY0(int yPixel, int yPixel0, int yPixel1) {
		double factor = (maxYOnScale - minYOnScale) / (yPixel1 - yPixel0);
		double y = maxYOnScale + (yPixel0 - yPixel) * factor;
		return Math.max(minY, Math.min(y, maxY));
	}

	int toPixelY(double yVal, int yPixel1) {
		return (Double.isNaN(yVal) ? Integer.MIN_VALUE
				: yPixel1
						- (int) (((yVal - spectrumYRef) * userYFactor + spectrumYRef - minYOnScale) / yFactorForScale));
	}

	int toPixelY0(double y, int yPixel0, int yPixel1) {
		double factor = (maxYOnScale - minYOnScale) / (yPixel1 - yPixel0);
		return (int) (yPixel0 + (maxYOnScale - y) / factor);
	}

	public void setScale(int xPixels, int yPixels, boolean isInverted) {
		double yRef = spectrumYRef;
		double f = spectrumScaleFactor;
		double minY = (f == 1 ? this.minY : initMinYOnScale);
		double maxY = (f == 1 ? this.maxY : initMaxYOnScale);
		if (f != 1 && yRef < minY)
			yRef = minY;
		if (f != 1 && yRef > maxY)
			yRef = maxY;
		setYScale((minY - yRef) / f + yRef, (maxY - yRef) / f + yRef, f == 1, isInverted);
  	xFactorForScale = (maxXOnScale - minXOnScale) / (xPixels - 1);
  	yFactorForScale = (maxYOnScale - minYOnScale) / (yPixels - 1);
  	System.out.println("scaledata yfactor max min " + yFactorForScale +" " + maxYOnScale +" " +  minYOnScale);
	}

  double toX(int xPixel, int xPixel1, boolean drawXAxisLeftToRight) {
		return toX(xPixel, xPixel1, drawXAxisLeftToRight, xFactorForScale);
	}

	double toX0(int xPixel, int xPixel0, int xPixel1, boolean drawXAxisLeftToRight) {
		return toX(xPixel, xPixel1, drawXAxisLeftToRight,
				(maxXOnScale - minXOnScale) / (xPixel1 - xPixel0));
	}

	private double toX(int xPixel, int xPixel1, boolean drawXAxisLeftToRight,
			double factor) {
		return (
				drawXAxisLeftToRight ? maxXOnScale - (xPixel1 - xPixel) * factor 
						                 : minXOnScale + (xPixel1 - xPixel) * factor
						);
	}

	int toPixelX(double dx, int xPixel0, int xPixel1,
			boolean drawXAxisLeftToRight) {
		return toPixelX(dx, xPixel0, xPixel1, drawXAxisLeftToRight, xFactorForScale);
	}

	int toPixelX0(double dx, int xPixel0, int xPixel1, boolean drawXAxisLeftToRight) {
		return toPixelX(dx, xPixel0, xPixel1, drawXAxisLeftToRight, (maxXOnScale - minXOnScale) / (xPixel1 - xPixel0));
	}

	private int toPixelX(double dx, int xPixel0, int xPixel1, boolean drawXAxisLeftToRight,
			double factor) {
		int x = (int) ((dx - minXOnScale) / factor);
		return (drawXAxisLeftToRight ? xPixel0 + x : xPixel1 - x);
	}





}