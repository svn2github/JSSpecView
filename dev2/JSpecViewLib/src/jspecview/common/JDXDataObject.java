package jspecview.common;

import jspecview.exception.JSpecViewException;
import jspecview.util.Logger;
import jspecview.util.Parser;

/**
 * spectrum data AS READ FROM FILE
 * 
 * @author Bob Hanson
 * 
 */
public class JDXDataObject extends JDXHeader {

  // --------------------Required Spectral Parameters ------------------------------//
  public double fileFirstX = JDXSpectrum.ERROR;
  public double fileLastX = JDXSpectrum.ERROR;
  public int nPointsFile = -1;
  public double xFactor = JDXSpectrum.ERROR;
  public double yFactor = JDXSpectrum.ERROR;

  public void checkRequiredTokens() throws JSpecViewException {
    if (fileFirstX == Graph.ERROR)
      throw new JSpecViewException("Error Reading Data Set: ##FIRST not found");
    if (fileLastX == Graph.ERROR)
      throw new JSpecViewException("Error Reading Data Set: ##LASTX not found");
    if (nPointsFile == -1)
      throw new JSpecViewException(
          "Error Reading Data Set: ##NPOINTS not found");
    if (xFactor == Graph.ERROR)
      throw new JSpecViewException(
          "Error Reading Data Set: ##XFACTOR not found");
    if (yFactor == Graph.ERROR)
      throw new JSpecViewException(
          "Error Reading Data Set: ##YFACTOR not found");
  }

  // --------------------Optional Spectral Parameters ------------------------------//

  public String xUnits = "";
  public String yUnits = "";
  
  // For NMR Spectra:
  public String observedNucl = "";
  public double observedFreq = JDXSpectrum.ERROR;
  public double offset = JDXSpectrum.ERROR; // Shift Reference
  public int shiftRefType = -1; // shiftRef = 0, bruker = 1, varian = 2
  public int dataPointNum = -1;
  public int numDim = 1;
  
  
  // 2D nucleus calc
  public String nucleusX, nucleusY = "?";
  public double freq2dX = Double.NaN;
  public double freq2dY = Double.NaN;

  protected void setNucleus(String nuc, boolean isX) {
    if (isX)
      nucleusX = nuc;
    else
      nucleusY = nuc;
    double freq;
    try {
      if (observedNucl.indexOf(nuc) >= 0) {
        freq = observedFreq;
      } else {
        double g1 = getGyroMagneticRatio(observedNucl);
        double g2 = getGyroMagneticRatio(nuc);
        freq = observedFreq * g2 / g1;
      }
    } catch (Exception e) {
      return;
    }
    if (isX)
      freq2dX = freq;
     else  
      freq2dY = freq;
    Logger.info("Freq for " + nuc + " = " + freq);
  }

  /*
   * from CODATA 2010 Physical Constants Task Group
1H      267.5222005        42.57748060
2H      41.0662791      6.53590131
13C     67.2828400      10.70839657
15N     -27.1261804     -4.31726570
19F     251.8148000        40.07757016
23Na    70.8084930      11.26952167
31P     108.3940000     17.25144090
   */
  private static double getGyroMagneticRatio(String nuc) {
    // will throw a caught exception if the string is not
    // of the proper form.
    int pt = 0;
    while (!Character.isDigit(nuc.charAt(pt)))
      pt++;
    switch (Parser.parseInt(nuc.substring(pt))) {
    default:
      return Double.NaN;
    case 1: //1H
      return 42.57774806;
    case 2: //2H
      return 6.53590131;
    case 13: //13C
      return 10.70838657;
    case 14: //14N
      return 3.07770646;
    case 15: //15N
      // absolute value is all that is needed here.
      return 4.31726570;
    case 19: //19F
      return 40.07757016;
    case 23: //23Na
      return 11.26952167;
    case 31: //31P
      return 17.2514409;
    }
  }


  /**
   * Sets the units for the x axis
   * 
   * @param xUnits
   *        the x units
   */
  public void setXUnits(String xUnits) {
    this.xUnits = xUnits;
  }

  /**
   * Sets the units for the y axis
   * 
   * @param yUnits
   *        the y units
   */
  public void setYUnits(String yUnits) {
    this.yUnits = yUnits;
  }

  /**
   * Sets the original xfactor
   * 
   * @param xFactor
   *        the x factor
   */
  public void setXFactor(double xFactor) {
    this.xFactor = xFactor;
  }

  /**
   * Sets the original y factor
   * 
   * @param yFactor
   *        the y factor
   */
  public void setYFactor(double yFactor) {
    this.yFactor = yFactor;
  }

  /**
   * Sets the Observed Frequency (for NMR Spectra)
   * 
   * @param observedFreq
   *        the observed frequency
   */
  public void setObservedFreq(double observedFreq) {
    this.observedFreq = observedFreq;
  }

  /**
   * Returns the units for x-axis when spectrum is displayed
   * 
   * @return the units for x-axis when spectrum is displayed
   */
  public String getXUnits() {
    return xUnits;
  }

  /**
   * Returns the units for y-axis when spectrum is displayed
   * 
   * @return the units for y-axis when spectrum is displayed
   */
  public String getYUnits() {
    return yUnits;
  }

  /**
   * Returns the original x factor
   * 
   * @return the original x factor
   */
  public double getXFactor() {
    return xFactor;
  }

  /**
   * Returns the original y factor
   * 
   * @return the original y factor
   */
  public double getYFactor() {
    return yFactor;
  }


  // For AnIML IR/UV files:
  
  // public double pathlength
  
//  /**
//   * Returns the pathlength of the sample (required for AnIML IR/UV files)
//   * 
//   * @return the pathlength
//   */
//  public String getPathlength() {
//    return pathlength;
//  }


  
}
