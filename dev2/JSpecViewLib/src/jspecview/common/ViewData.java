package jspecview.common;

import java.util.List;


/**
 * Stores information that <code>GraphSet</code> needs 
 * to display a view with one or more spectra. 
 * 
 */
class ViewData {

	private ScaleData[] scaleData;
  private int nSpectra;
  private int iThisScale;
	private ScaleData thisScale;
	
	/**
	 * in some cases, there is only one scaleData, but there are more than that number of spectra
	 * this is no problem -- we just use mod to set this to 0
	 * @param i
	 * @return starting point data index
	 */
  int getStartingPointIndex(int i) {
  	return scaleData[i % scaleData.length].startDataPointIndex;
  }
  
	/**
	 * in some cases, there is only one scaleData, but there are more than that number of spectra
	 * this is no problem -- we just use mod to set this to 0
	 * @param i
	 * @return ending point data index
	 */
  int getEndingPointIndex(int i) {
  	return scaleData[i % scaleData.length].endDataPointIndex;
  }
  

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
	 * @param is2D 
	 * @returns an instance of <code>MultiScaleData</code>
	 */
	ViewData(List<JDXSpectrum> spectra, double yPt1, double yPt2,
			int[] startList, int[] endList, boolean isContinuous, boolean is2D) {
		nSpectra = (is2D ? 1 : spectra.size());
		scaleData = new ScaleData[nSpectra];
		for (int j = 0; j < nSpectra; j++)
			scaleData[j] = new ScaleData(startList[j], endList[j]);
		init(spectra, yPt1, yPt2, isContinuous);
	}
  
	ViewData(List<JDXSpectrum> spectra, double yPt1, double yPt2, 
			boolean isContinuous) {
		// forced subsets
		nSpectra = spectra.size();
		int n = spectra.get(0).getXYCoords().length; // was - 1 
		scaleData = new ScaleData[1];
		scaleData[0] = new ScaleData(0, n - 1);
		init(spectra, yPt1, yPt2, isContinuous);
	}

  private void init(List<JDXSpectrum> spectra, 
  		double yPt1, double yPt2, boolean isContinuous) {
		thisScale = scaleData[iThisScale = 0];
		for (int i = 0; i < scaleData.length; i++) {
			scaleData[i].userYFactor = spectra.get(i).getUserYFactor();
		  scaleData[i].spectrumYRef = spectra.get(i).getYRef(); // 0 or 100
		}
		resetScaleFactors();
		double minX = Coordinate.getMinX(spectra, this);
		double maxX = Coordinate.getMaxX(spectra, this);
		double minY = Coordinate.getMinYUser(spectra, this);
		double maxY = Coordinate.getMaxYUser(spectra, this);
  	
    if (yPt1 != yPt2) {
      minY = yPt1;
      maxY = yPt2;
      if (minY > maxY) {
        double t = minY;
        minY = maxY;
        maxY = t;
      }
    }
    boolean isInverted = spectra.get(0).isInverted();
		for (int i = 0; i < scaleData.length; i++) {
			scaleData[i].setMinMax(minX, maxX, minY, maxY);
      scaleData[i].setScale(isContinuous, isInverted);
		}
  }

	void newSpectrum(List<JDXSpectrum> spectra) {
		init(spectra, 0, 0, false);
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
      int iStart = (useRange ? getStartingPointIndex(i) : 0);
      int iEnd = (useRange ? getEndingPointIndex(i) : xyCoords.length - 1);
      if (ScaleData.setXRange(i, xyCoords, initX, finalX, iStart, iEnd, startIndices, endIndices) >= minPoints)
        nSpectraOK++;
    }
    return (nSpectraOK == nSpectra);
  }

  void setXRangeForSubSpectrum(Coordinate[] xyCoords) {
  	// forced subspectra only
    setXRange(0, xyCoords, scaleData[0].minX, scaleData[0].maxX, 0, xyCoords.length - 1);
  }

  private int setXRange(int i, Coordinate[] xyCoords, double initX, double finalX, int iStart, int iEnd) {
    int index = 0;
    int ptCount = 0;
    for (index = iStart; index <= iEnd; index++) {
      double x = xyCoords[index].getXVal();
      if (x >= initX) {
        scaleData[i % scaleData.length].startDataPointIndex = index;
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
    scaleData[i % scaleData.length].endDataPointIndex = index - 1;
    return ptCount;
  }


	void resetScaleFactors() {
		for (int i = 0; i < scaleData.length; i++)
  	  scaleData[i].spectrumScaleFactor = 1;
	}
	
	void setScaleFactor(int i, double f) {
		if (f == 0 || i >= nSpectra)
			return;
		if (i < 0) {
			resetScaleFactors();
			// setYScale(initMinY, initMaxY, true);
		} else {
			scaleData[i % scaleData.length].spectrumScaleFactor = f;
		}
	}
  
	void scaleSpectrum(int i, double f) {
		if (f == 0 || i >= nSpectra)
			return;
		if (i == -2) {
			thisScale.scale2D(f);
			return;
		}
		if (i < 0)
			for (i = 0; i < scaleData.length; i++)
				scaleData[i].spectrumScaleFactor *= f;
		else
      scaleData[i % scaleData.length].spectrumScaleFactor *= f;
  }

	double getSpectrumScaleFactor(int i) {
		return (i >= 0 && i < nSpectra ? scaleData[i % scaleData.length].spectrumScaleFactor : 1);
	}

	double getSpectrumYRef(int i) {
		return scaleData[i % scaleData.length].spectrumYRef;
	}
	
	double getUserYFactor(int i) {
		return scaleData[i % scaleData.length].userYFactor;
	}
	
	boolean areYScalesSame(int i, int j) {
		return getSpectrumScaleFactor(i) == getSpectrumScaleFactor(j)
		  && getSpectrumYRef(i) == getSpectrumYRef(j)
		  && getUserYFactor(i) == getUserYFactor(j);
	}

	void setScale(int i, int xPixels, int yPixels, boolean isInverted) {
		iThisScale = i % scaleData.length;
		thisScale = scaleData[iThisScale];
		System.out.println(i + "-0 " + thisScale.minY + " " + thisScale.maxY + " ? " + thisScale.minYOnScale + " " + thisScale.maxYOnScale);
		thisScale.setScale(xPixels, yPixels, isInverted);
		System.out.println(i + "-1 " + thisScale.minY + " " + thisScale.maxY + " ? " + thisScale.minYOnScale + " " + thisScale.maxYOnScale);
	}

	void copyScaleFactors(ViewData view0, boolean resetMinMaxY) {
		for (int i = 0; i < scaleData.length; i++) {
			scaleData[i].spectrumScaleFactor = view0.scaleData[i].spectrumScaleFactor;
			scaleData[i].spectrumYRef = view0.scaleData[i].spectrumYRef;
			scaleData[i].userYFactor = view0.scaleData[i].userYFactor;
			scaleData[i].specShift = view0.scaleData[i].specShift;
			if (resetMinMaxY) {
				System.out.println("resetminmax" + i + " " + scaleData[i].minYOnScale +" to " + view0.scaleData[i].minYOnScale);
				scaleData[i].initMinYOnScale = view0.scaleData[i].initMinYOnScale;
				scaleData[i].initMaxYOnScale = view0.scaleData[i].initMaxYOnScale;
				scaleData[i].minY = view0.scaleData[i].minY;
				scaleData[i].maxY = view0.scaleData[i].maxY;
			}
		}
	}
	
	double unScaleY(double y) {
		//TODO  this can't be good
		// it's important for NMR spectra, though.
		return y * thisScale.spectrumScaleFactor;
	}

	public ScaleData getScaleData() {
		return thisScale;
	}


}