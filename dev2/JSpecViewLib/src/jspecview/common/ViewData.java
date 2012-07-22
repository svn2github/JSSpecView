package jspecview.common;

import java.util.List;


/**
 * Stores information that <code>GraphSet</code> needs 
 * to display a view with one or more spectra. 
 * 
 */
class ViewData extends ScaleData {

  int[] startDataPointIndices;
  int[] endDataPointIndices;
  int[] numOfPointsList;
	double[] userYFactors;
	double[] spectrumYRefs; // 0 or 100
  int[] spectrumOffsets; // not yet implemented

  private int nSpec;
	private double[] spectrumScaleFactors;

	/**
	 * 
	 * @param spectra
	 *          an array of spectra
	 * @param yPt1
	 * @param yPt2
	 * @param startList
	 *          the start indices
	 * @param endList
	 *          the end indices
	 * @param isContinuous 
	 * @returns an instance of <code>MultiScaleData</code>
	 */
	ViewData(List<JDXSpectrum> spectra, double yPt1, double yPt2,
			int[] startList, int[] endList, boolean isContinuous, boolean isInverted) {
		super();
		nSpec = spectra.size();
		startDataPointIndices = startList;
		endDataPointIndices = endList;
		numOfPointsList = new int[nSpec];
		for (int j = 0; j < nSpec; j++)
			numOfPointsList[j] = endList[j] + 1 - startList[j];
		init(spectra, startList, endList, yPt1, yPt2, isContinuous, isInverted);
	}
  
	ViewData(List<JDXSpectrum> spectra, double yPt1, double yPt2, 
			boolean isContinuous, boolean isInverted) {
		// forced subsets
		super();
		nSpec = spectra.size();
		int n = spectra.get(0).getXYCoords().length; // was - 1 
		startDataPointIndices = new int[] { 0 };
		endDataPointIndices = new int[] { n - 1 };
		numOfPointsList = new int[] { n };
		init(spectra, null, null, yPt1, yPt2, isContinuous, isInverted);
	}

	private void setMinMax(List<JDXSpectrum> spectra, int[] startList,
			int[] endList) {
		minX = Coordinate.getMinX(spectra, startList, endList);
		maxX = Coordinate.getMaxX(spectra, startList, endList);
		minY = Coordinate.getMinYUser(spectra, startList, endList);
		maxY = Coordinate.getMaxYUser(spectra, startList, endList);
		spectrumYRefs = new double[nSpec];
		for (int i = nSpec; --i >= 0;)
			spectrumYRefs[i] = spectra.get(i).getYRef(); // 0 or 100
	}
	
	void setSpectrumYRef(int i, double yref) {
		spectrumYRefs[i] = yref;
	}

  private void init(List<JDXSpectrum> spectra, int[] startList, int[] endList, 
  		double yPt1, double yPt2, boolean isContinuous, boolean isInverted) {
  	
		spectrumScaleFactors = new double[nSpec];
		userYFactors = new double[nSpec];
		for (int i = 0; i < nSpec; i++)
			userYFactors[i] = spectra.get(i).getUserYFactor();
		resetScaleFactors();
		setMinMax(spectra, startList, endList);
  	
    if (yPt1 != yPt2) {
      minY = yPt1;
      maxY = yPt2;
      if (minY > maxY) {
        double t = minY;
        minY = maxY;
        maxY = t;
      }
    }
    setScale(isContinuous, isInverted);
  }

  /**
   * 
   * @param graphsTemp
   * @param initX
   * @param finalX
   * @param minPoints
   * @param startIndices  to fill
   * @param endIndices    to fill
   * @param useRange 
   * @param scaleData
   * @return true if OK
   */
  boolean setDataPointIndices(List<JDXSpectrum> graphsTemp,
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

  void setXRangeForSubSpectrum(Coordinate[] xyCoords) {
    int n = xyCoords.length - 1;
    startDataPointIndices[0] = 0;
    endDataPointIndices[0] = n;
    setXRange(0, xyCoords, minX, maxX, 0, n, startDataPointIndices, endDataPointIndices);
  }

	void resetScaleFactors() {
		for (int i = 0; i < nSpec; i++)
  	  spectrumScaleFactors[i] = 1;
	}
	
 void setScaleFactor(int i, double f) {
		if (f == 0 || i >= nSpec)
			return;
		if (i < 0) {
			resetScaleFactors();
			//setYScale(initMinY, initMaxY, true);
		} else {
			spectrumScaleFactors[i] = f;
		}
  }
  
	void scaleSpectrum(int i, double f) {
		if (f == 0 || i >= nSpec)
			return;
		if (i == -2) {
			scale2D(f);
			return;
		}
		if (i < 0)
			for (i = 0; i < nSpec; i++)
				spectrumScaleFactors[i] *= f;
		else
			spectrumScaleFactors[i] *= f;
  }

	double getSpectrumScaleFactor(int i) {
		return (i >= 0 && i < nSpec ? spectrumScaleFactors[i] : 1);
	}

	void copyScaleFactors(ViewData view) {
		System.arraycopy(view.spectrumScaleFactors, 0, spectrumScaleFactors, 0, nSpec);
		System.arraycopy(view.userYFactors, 0, userYFactors, 0, nSpec);
		System.arraycopy(view.spectrumYRefs, 0, spectrumYRefs, 0, nSpec);
		initMinYOnScale = view.initMinYOnScale;
		initMaxYOnScale = view.initMaxYOnScale;
		specShift = view.specShift;
		
	}

	void setAxisScaling(int i, int xPixels, int yPixels, boolean isInverted) {
		double f = spectrumScaleFactors[i];
		double yRef = spectrumYRefs[i];
		double minY = (f == 1 ? this.minY : initMinYOnScale);
		double maxY = (f == 1 ? this.maxY : initMaxYOnScale);
		if (f != 1 && yRef < minY)
			yRef = minY;
		if (f != 1 && yRef > maxY)
			yRef = maxY;
		setYScale((minY - yRef) / f + yRef, (maxY - yRef) / f + yRef, f == 1, isInverted);
		setScaleFactors(xPixels, yPixels);
	}
	
	public double unScaleY(int iSpec, double y) {
		return y * spectrumScaleFactors[iSpec];
	}

	boolean areYScalesSame(int i, int j) {
		return spectrumScaleFactors[i] == spectrumScaleFactors[j]
		  && spectrumYRefs[i] == spectrumYRefs[j] 
		  && userYFactors[i] == userYFactors[j];
	}

}