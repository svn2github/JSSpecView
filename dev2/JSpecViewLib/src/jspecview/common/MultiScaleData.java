package jspecview.common;


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
  }

  /**
   * Calculates values that <code>JSVPanel</code> needs in order to render a
   * graph, (eg. scale, min and max values) and stores the values in the class
   * <code>ScaleData</code>.
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
  public MultiScaleData(Coordinate[][] coordLists, int[] startList,
      int[] endList, int initNumXDivisions, int initNumYDivisions,
      boolean isContinuous) {
    super(Coordinate.getMinX(coordLists, startList, endList), 
        Coordinate.getMaxX(coordLists, startList, endList), 
        Coordinate.getMinY(coordLists, startList, endList), 
        Coordinate.getMaxY(coordLists,startList, endList));
    startDataPointIndices = startList;
    endDataPointIndices = endList;
    numOfPointsList = new int[startList.length];
    for (int j = 0; j < startList.length; j++)
      numOfPointsList[j] = endList[j] - startList[j] + 1;
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
                                            int[] endIndices) {
    int ptCount = 0;
    int nSpectraOK = 0;
    int nSpectra = startIndices.length;
    int index = 0;
    for (int i = 0; i < nSpectra; i++) {
      Coordinate[] xyCoords = spectra[i].getXYCoords();
      int iStart = startDataPointIndices[i];
      int iEnd = endDataPointIndices[i];
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

      if (ptCount >= minPoints) {
        nSpectraOK++;
      }
      ptCount = 0;
      endIndices[i] = index - 1;
    }

    return (nSpectraOK == nSpectra);
  }

 }