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
  public ImageScaleData isd;
 
  
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
   *        an array of spectra
   * @param yPt1 
   * @param yPt2 
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
  public MultiScaleData(Graph[] spectra, double yPt1,
      double yPt2, int[] startList, int[] endList,
      int initNumXDivisions, int initNumYDivisions, boolean isContinuous) {
    super(Coordinate.getMinX(spectra, startList, endList), 
        Coordinate.getMaxX(spectra, startList, endList), 
        Coordinate.getMinYUser(spectra, startList, endList), 
        Coordinate.getMaxYUser(spectra, startList, endList));
    startDataPointIndices = startList;
    endDataPointIndices = endList;
    numOfPointsList = new int[startList.length];
    for (int j = 0; j < startList.length; j++)
      numOfPointsList[j] = endList[j] - startList[j] + 1;
    init(yPt1, yPt2, initNumXDivisions, initNumYDivisions, isContinuous);
  }
  
  public MultiScaleData(List<JDXSpectrum> spectra, double yPt1, double yPt2,
      int initNumXDivisions, int initNumYDivisions,
      boolean isContinuous) {
    // forced subsets
    super(Coordinate.getMinX(spectra), 
        Coordinate.getMaxX(spectra),
        Coordinate.getMinYUser(spectra), 
        Coordinate.getMaxYUser(spectra));
    int n =  spectra.get(0).getXYCoords().length - 1;
    startDataPointIndices = new int[] { 0 };
    endDataPointIndices = new int[] { n - 1 };
    numOfPointsList = new int[] { n };
    init(yPt1, yPt2, initNumXDivisions, initNumYDivisions, isContinuous);
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
   * @param spectra
   * @param scaleData
   * @param initX
   * @param finalX
   * @param minPoints
   * @param startIndices  to fill
   * @param endIndices    to fill
   * @return
   */
  public boolean setDataPointIndices(Graph[] spectra,
                                            double initX,
                                            double finalX, int minPoints,
                                            int[] startIndices,
                                            int[] endIndices, boolean useRange) {
    int nSpectraOK = 0;
    int nSpectra = spectra.length;
    for (int i = 0; i < nSpectra; i++) {
      Coordinate[] xyCoords = spectra[i].getXYCoords();
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
}