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
    if (observedNucl.indexOf(nuc) >= 0) {
      freq = observedFreq;
    } else {
      double g1 = getGyroMagneticRatio(observedNucl);
      double g2 = getGyroMagneticRatio(nuc);
      freq = observedFreq * g2 / g1;
    }
    if (isX)
      freq2dX = freq;
    else
      freq2dY = freq;
    Logger.info("Freq for " + nuc + " = " + freq);
  }


  /**
   * source: http://www.mathworks.com/matlabcentral/fileexchange/11722-nmr-properties/content/isotopes.m
   * % REFERENCES 
% 1. "Nuclear spins, moments, and other data related to NMR spectroscopy" (9-93)
%    CRC Handbook of Chemistry and Physics, 83th Ed., CRC Press, Boca Raton, FL, 
%    2002. 
% 2. Stone, N. J., <www.nndc.bnl.gov/nndc/stone_moments/>
% 3. http://www.hbcpnetbase.com/
   */
  
  /* '*' indicates
   * from CODATA 2010 Physical Constants Task Group
1H      267.5222005        42.57748060
2H      41.0662791      6.53590131
13C     67.2828400      10.70839657
15N     -27.1261804     -4.31726570
19F     251.8148000        40.07757016
23Na    70.8084930      11.26952167
31P     108.3940000     17.25144090
   */

  private final static double[] gyroData = {
    1, 42.5774806,//*H   Hydrogen  1/2
    2, 6.53590131,//*H   Deuterium 1
    3, 45.4148 ,  // H   Tritium  1/2
    3, 32.436 ,   // He  Helium  1/2
    6, 6.2661 ,   // Li  Lithium 1
    7, 16.5483 ,  // Li  Lithium  3/2
    9, 5.9842 ,   // Be  Beryllium  3/2
    10, 4.5752 ,  // B   Boron 3
    11, 13.663 ,  // B   Boron  3/2
    13, 10.70839657,//*C Carbon  1/2
    14, 3.07770646,//*N  Nitrogen 1
    15, 4.31726570,//*N  Nitrogen  1/2
    17, 5.7742 ,  // O   Oxygen  5/2
    19, 40.07757016,//*F Fluorine  1/2
    21, 3.3631 ,  // Ne  Neon  3/2
    23, 11.26952167,//*Na Sodium  3/2
    25, 2.6083 ,  // Mg  Magnesium  5/2
    27, 11.1031 , // Al  Aluminum  5/2
    29, 8.4655 ,  // Si  Silicon  1/2
    31, 17.25144090,//*P Phosphorus  1/2
    33, 3.2717 ,  // S   Sulfur  3/2
    35, 4.1765 ,  // Cl  Chlorine  3/2
    37, 3.4765 ,  // Cl  Chlorine  3/2
    37, 5.819 ,   // Ar  Argone  3/2
    39, 3.46 ,    // Ar  Argone  7/2
    39, 1.9893 ,  // K   Potassium  3/2
    40, 2.4737 ,  // K   Potassium 4
    41, 1.0919 ,  // K   Potassium  3/2
    43, 2.8688 ,  // Ca  Calcium  7/2
    45, 10.3591 , // Sc  Scandium  7/2
    47, 2.4041 ,  // Ti  Titanium  5/2
    49, 2.4048 ,  // Ti  Titanium  7/2
    50, 4.2505 ,  // V   Vanadium 6
    51, 11.2133 , // V   Vanadium  7/2
    53, 2.4115 ,  // Cr  Chromium  3/2
    55, 10.5763 , // Mn  Manganese  5/2
    57, 1.3816 ,  // Fe  Iron  1/2
    59, 10.077 ,  // Co  Cobalt  7/2
    61, 3.8114 ,  // Ni  Nickel  3/2
    63, 11.2982 , // Cu  Copper  3/2
    65, 12.103 ,  // Cu  Copper  3/2
    67, 2.6694 ,  // Zn  Zinc  5/2
    69, 10.2478 , // Ga  Gallium  3/2
    71, 13.0208 , // Ga  Gallium  3/2
    73, 1.4897 ,  // Ge  Germanium  9/2
    75, 7.315 ,   // As  Arsenic  3/2
    77, 8.1571 ,  // Se  Selenium  1/2
    79, 10.7042 , // Br  Bromine  3/2
    81, 11.5384 , // Br  Bromine  3/2
    83, 1.6442 ,  // Kr  Krypton  9/2
    85, 4.1254 ,  // Rb  Rubidium  5/2
    87, 13.9811 , // Rb  Rubidium  3/2
    87, 1.8525 ,  // Sr  Strontium  9/2
    89, 2.0949 ,  // Y   Yttrium  1/2
    91, 3.9748 ,  // Zr  Zirconium  5/2
    93, 10.4523 , // Nb  Niobium  9/2
    95, 2.7874 ,  // Mo  Molybdenum  5/2
    97, 2.8463 ,  // Mo  Molybdenum  5/2
    99, 9.6294 ,  // Tc  Technetium  9/2
    99, 1.9553 ,  // Ru  Ruthenium  5/2
    101, 2.1916 , // Ru  Ruthenium  5/2
    103, 1.3477 , // Rh  Rhodium  1/2
    105, 1.957 ,  // Pd  Palladium  5/2
    107, 1.7331 , // Ag  Silver  1/2
    109, 1.9924 , // Ag  Silver  1/2
    111, 9.0692 , // Cd  Cadmium  1/2
    113, 9.4871 , // Cd  Cadmium  1/2
    113, 9.3655 , // In  Indium  9/2
    115, 9.3856 , // In  Indium  9/2
    115, 14.0077 ,// Sn  Tin  1/2
    117, 15.261 , // Sn  Tin  1/2
    119, 15.966 , // Sn  Tin  1/2
    121, 10.2551 ,// Sb  Antimony  5/2
    123, 5.5532 , // Sb  Antimony  7/2
    123, 11.2349 ,// Te  Tellurium  1/2
    125, 13.5454 ,// Te  Tellurium  1/2
    127, 8.5778 , // I   Iodine  5/2
    129, 11.8604 ,// Xe  Xenon  1/2
    131, 3.5159 , // Xe  Xenon  3/2
    133, 5.6234 , // Cs  Cesium  7/2
    135, 4.2582 , // Ba  Barium  3/2
    137, 4.7634 , // Ba  Barium  3/2
    138, 5.6615 , // La  Lanthanum 5
    139, 6.0612 , // La  Lanthanum  7/2
    137, 4.88 ,   // Ce  Cerium  3/2
    139, 5.39 ,   // Ce  Cerium  3/2
    141, 2.37 ,   // Ce  Cerium  7/2
    141, 13.0359 ,// Pr  Praseodymium  5/2
    143, 2.319 ,  // Nd  Neodymium  7/2
    145, 1.429 ,  // Nd  Neodymium  7/2
    143, 11.59 ,  // Pm  Promethium  5/2
    147, 5.62 ,   // Pm  Promethium  7/2
    147, 1.7748 , // Sm  Samarium  7/2
    149, 14631 ,  // Sm  Samarium  7/2
    151, 10.5856 ,// Eu  Europium  5/2
    153, 4.6745 , // Eu  Europium  5/2
    155, 1.312 ,  // Gd  Gadolinium  3/2
    157, 1.72 ,   // Gd  Gadolinium  3/2
    159, 10.23 ,  // Tb  Terbium  3/2
    161, 1.4654 , // Dy  Dysprosium  5/2
    163, 2.0508 , // Dy  Dysprosium  5/2
    165, 9.0883 , // Ho  Holmium  7/2
    167, 1.2281 , // Er  Erbium  7/2
    169, 3.531 ,  // Tm  Thulium  1/2
    171, 7.5261 , // Yb  Ytterbium  1/2
    173, 2.073 ,  // Yb  Ytterbium  5/2
    175, 4.8626 , // Lu  Lutetium  7/2
    176, 3.451 ,  // Lu  Lutetium 7
    177, 1.7282 , // Hf  Hafnium  7/2
    179, 1.0856 , // Hf  Hafnium  9/2
    180, 4.087 ,  // Ta  Tantalum 9
    181, 5.1627 , // Ta  Tantalum  7/2
    183, 1.7957 , // W   Tungsten  1/2
    185, 9.7176 , // Re  Rhenium  5/2
    187, 9.817 ,  // Re  Rhenium  5/2
    187, 0.9856 , // Os  Osmium  1/2
    189, 3.3536 , // Os  Osmium  3/2
    191, 0.7658 , // Ir  Iridium  3/2
    191, 0.8319 , // Ir  Iridium  3/2
    195, 9.2922 , // Pt  Platinum  1/2
    197, 0.7406 , // Au  Gold  3/2
    199, 7.7123 , // Hg  Mercury  1/2
    201, 2.8469 , // Hg  Mercury  3/2
    203, 24.7316 ,// Tl  Thallium  1/2
    205, 24.9749 ,// Tl  Thallium  1/2
    207, 9.034 ,  // Pb  Lead  1/2
    209, 6.963 ,  // Bi  Bismuth  9/2
    209, 11.7 ,   // Po  Polonium  1/2
    211, 9.16 ,   // Rn  Radon  1/2
    223, 5.95 ,   // Fr  Francium  3/2
    223, 1.3746 , // Ra  Radium  3/2
    225, 11.187 , // Ra  Radium  1/2
    227, 5.6 ,    // Ac  Actinium  3/2
    229, 1.4 ,    // Th  Thorium  5/2
    231, 10.2 ,   // Pa  Protactinium  3/2
    235, 0.83 ,   // U   Uranium  7/2
    237, 9.57 ,   // Np  Neptunium  5/2
    239, 3.09 ,   // Pu  Plutonium  1/2
    243, 4.6 ,    // Am  Americium  5/2
    1E100
  };

  private static double getGyroMagneticRatio(String nuc) {
    // will throw a caught exception if the string is not
    // of the proper form.
    int pt = 0;
    while (pt < nuc.length() && !Character.isDigit(nuc.charAt(pt)))
      pt++;
    pt = Parser.parseInt(nuc.substring(pt));
    int i = 0;
    for (; i < gyroData.length; i += 2)
      if (gyroData[i] >= pt)
        break;
    return (gyroData[i] == pt ? gyroData[i + 1] : Double.NaN);
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
