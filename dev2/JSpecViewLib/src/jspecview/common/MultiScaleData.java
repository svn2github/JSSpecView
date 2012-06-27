package jspecview.common;

import java.util.List;


/**
 * Stores information
 * about scale and range that <code>JSVPanel</code> needs to to display a
 * graph with a mutliple plots. You do not need to create an instance of this
 * class manually. Instead call the
 * {@link jspecview.common.MultiScaleData#generateScaleData(jspecview.common.Coordinate[][], int[], int[], int, int)}
 * .
 */
public class MultiScaleData extends ScaleData {

  public int[] startDataPointIndices;
  public int[] endDataPointIndices;
  public int[] numOfPointsList;
  public double minY2D, maxY2D;
	private float[] spectrumScaleFactors;
	private int nSpec;
 
  
  /**
   * Never utilized?? 
   * Initialises a <code>MultiScaleData</code> from another one
   * 
   * @param data
   *        the <code>MultiScaleData</code> to copy
   */
  public MultiScaleData(MultiScaleData data) {
    super(data);    
    startDataPointIndices = data.startDataPointIndices;
    endDataPointIndices = data.endDataPointIndices;
    spectrumScaleFactors = new float[data.spectrumScaleFactors.length];
    System.arraycopy(data.spectrumScaleFactors, 0, spectrumScaleFactors, 0, data.spectrumScaleFactors.length);
    numOfPointsList = data.numOfPointsList;
    minY2D = data.minY2D;
    maxY2D = data.maxY2D;
  }

	/**
	 * Calculates values that <code>JSVPanel</code> needs in order to render a
	 * graph, (eg. scale, min and max values) and stores the values in the class
	 * <code>ScaleData</code>.
	 * 
	 * @param spectra
	 *          an array of spectra
	 * @param yPt1
	 * @param yPt2
	 * @param startList
	 *          the start indices
	 * @param endList
	 *          the end indices
	 * @param initNumXDivisions
	 *          the initial number of X divisions for scale
	 * @param initNumYDivisions
	 *          the initial number of Y divisions for scale
	 * @return returns an instance of <code>MultiScaleData</code>
	 */
	public MultiScaleData(List<JDXSpectrum> spectra, double yPt1, double yPt2,
			int[] startList, int[] endList, int initNumXDivisions,
			int initNumYDivisions, boolean isContinuous) {
		super();
		nSpec = spectra.size();
		startDataPointIndices = startList;
		endDataPointIndices = endList;
		numOfPointsList = new int[nSpec];
		for (int j = 0; j < nSpec; j++)
			numOfPointsList[j] = endList[j] + 1 - startList[j];
		spectrumScaleFactors = new float[nSpec];
		resetScaleFactors();
		setMinMax(spectra, startList, endList);
		init(yPt1, yPt2, initNumXDivisions, initNumYDivisions, isContinuous);
	}
  
	public MultiScaleData(List<JDXSpectrum> spectra, double yPt1, double yPt2,
			int initNumXDivisions, int initNumYDivisions, boolean isContinuous) {
		// forced subsets
		super();
		nSpec = spectra.size();
		int n = spectra.get(0).getXYCoords().length; // was - 1 
		startDataPointIndices = new int[] { 0 };
		endDataPointIndices = new int[] { n - 1 };
		numOfPointsList = new int[] { n };
		spectrumScaleFactors = new float[nSpec];
		resetScaleFactors();
		setMinMax(spectra, null, null);
		init(yPt1, yPt2, initNumXDivisions, initNumYDivisions, isContinuous);
	}

	private void setMinMax(List<JDXSpectrum> spectra, int[] startList,
			int[] endList) {
		minX = Coordinate.getMinX(spectra, startList, endList);
		maxX = Coordinate.getMaxX(spectra, startList, endList);
		minY = Coordinate.getMinYUser(spectra, startList, endList);
		maxY = Coordinate.getMaxYUser(spectra, startList, endList);
	}

  private void init(double yPt1, double yPt2,
                    int initNumXDivisions, int initNumYDivisions, boolean isContinuous) {
    if (yPt1 != yPt2) {
      minY = yPt1;
      maxY = yPt2;
      if (minY > maxY) {
        double t = minY;
        minY = maxY;
        maxY = t;
      }
    }
    setScale(initNumXDivisions, initNumYDivisions, isContinuous);
  }

  /**
   * 
   * @param graphsTemp
   * @param scaleData
   * @param initX
   * @param finalX
   * @param minPoints
   * @param startIndices  to fill
   * @param endIndices    to fill
   * @return
   */
  public boolean setDataPointIndices(List<JDXSpectrum> graphsTemp,
                                            double initX,
                                            double finalX, int minPoints,
                                            int[] startIndices,
                                            int[] endIndices, boolean useRange) {
    int nSpectraOK = 0;
    int nSpectra = graphsTemp.size();
    for (int i = 0; i < nSpectra; i++) {
      Coordinate[] xyCoords = graphsTemp.get(i).getXYCoords();
      int iStart = (useRange ? startDataPointIndices[i] : 0);
      int iEnd = (useRange ? endDataPointIndices[i] : xyCoords.length - 1);
      if (setXRange(i, xyCoords, initX, finalX, iStart, iEnd, startIndices, endIndices)
          >= minPoints)
        nSpectraOK++;
    }
    return (nSpectraOK == nSpectra);
  }

  private static int setXRange(int i, Coordinate[] xyCoords, double initX, double finalX, int iStart, int iEnd, int[] startIndices, int[] endIndices) {
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

  public void setXRange(Graph graph) {
    int n = graph.getXYCoords().length - 1;
    startDataPointIndices[0] = 0;
    endDataPointIndices[0] = n;
    setXRange(0, graph.getXYCoords(), minX, maxX, 0, n, startDataPointIndices, endDataPointIndices);
  }

  public void setXRange(double x1, double x2, int initNumXDivisions) {
    // TODO Auto-generated method stub
    minX = x1;
    maxX = x2;
    setXScale(initNumXDivisions);
  }

  public boolean isYZeroOnScale() {
    return (minYOnScale < 0 && maxYOnScale > 0);
  }

  public void setMinMaxY2D(List<JDXSpectrum> subspectra) {
    minY2D = Double.MAX_VALUE;
    maxY2D = -Double.MAX_VALUE;
    for (int i = subspectra.size(); --i >= 0; ) {
      double d = subspectra.get(i).getY2D();
      if (d < minY2D)
        minY2D = d;
      else if (d > maxY2D)
        maxY2D = d;
    }
  }

	public void resetScaleFactors() {
		for (int i = 0; i < nSpec; i++)
  	  spectrumScaleFactors[i] = 1;
	}
	
 public void setScaleFactor(int i, float f) {
		if (f == 0 || i >= nSpec)
			return;
		if (i < 0)
			resetScaleFactors();
		else
			spectrumScaleFactors[i] = f;
  }
  
	public void scaleSpectrum(int i, float f) {
		if (f == 0 || i >= nSpec)
			return;
		if (i < 0)
			for (i = 0; i < nSpec; i++)
				spectrumScaleFactors[i] *= f;
		else
			spectrumScaleFactors[i] *= f;
  }

	public double getSpectrumScaleFactor(int i) {
		return (i >= 0 && i < nSpec ? spectrumScaleFactors[i] : 1);
	}

	public void setScaleFactors(MultiScaleData msd) {
		for (int i = 0; i < nSpec; i++)
			spectrumScaleFactors[i] = msd.spectrumScaleFactors[i];
	}
}