/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.List;

import jspecview.source.JDXSourceStreamTokenizer;
import jspecview.util.Logger;

/**
 * <code>JDXSpectrum</code> implements the Interface Spectrum for the display of
 * JDX Files.
 * 
 * @author Bob Hanson
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class JDXSpectrum extends JDXDataObject implements Graph {

  @Override
  public void finalize() {
    System.out.println("JDXSpectrum " + this + " finalized " + title);
  }

  /**
   * spectra that can never be displayed independently, or at least not by default
   * 2D slices, for example.
   */
  private List<JDXSpectrum> subSpectra;
  private ArrayList<PeakInfo> peakList = new ArrayList<PeakInfo>();
  private JDXSpectrum parent;
  private int[] buf2d;
  private IntegralGraph integration;
  private PeakInfo selectedPeak;

  public void dispose() {
    buf2d = null;
    integration = null;
    if (subSpectra != null)
    for (int i = 0; i < subSpectra.size(); i++)
      if (subSpectra.get(i) != this)
        subSpectra.get(i).dispose();
    subSpectra = null;
    parent = null;
    peakList = null;
    selectedPeak = null;
  }

  private int thisWidth,thisHeight;
  private int currentSubSpectrumIndex;
  private double grayFactorLast;
  private boolean isForcedSubset;
  
  public boolean isForcedSubset() {
    return isForcedSubset;
  }

  private String id = "";
  public void setId(String id) {
    this.id = id;
  }
  
  /**
   * Constructor
   */
  public JDXSpectrum() {
    //System.out.println("initialize JDXSpectrum " + this);
    headerTable = new ArrayList<String[]>();
    xyCoords = new Coordinate[0];
  }

  /**
   * Returns a copy of this <code>JDXSpectrum</code>
   * 
   * @return a copy of this <code>JDXSpectrum</code>
   */
  public JDXSpectrum copy() {
    JDXSpectrum newSpectrum = new JDXSpectrum();
    copyTo(newSpectrum);
    newSpectrum.setPeakList(getPeakList());
    return newSpectrum;
  }

  /**
   * Returns the array of coordinates
   * 
   * @return the array of coordinates
   */
  public Coordinate[] getXYCoords() {
    return getCurrentSubSpectrum().xyCoords;
  }

  
  public ArrayList<PeakInfo> getPeakList() {
    return peakList;
  }

  public int setPeakList(ArrayList<PeakInfo> list) {
    peakList = list;
    for (int i = list.size(); --i >= 0; )
      peakList.get(i).spectrum = this;
    if (Logger.debugging)
      Logger.info("Spectrum " + getTitle() + " peaks: " + list.size());
    return list.size();
  }

  public PeakInfo findPeakByFileIndex(String filePath, String index) {
    if (peakList != null && peakList.size() > 0)
      for (int i = 0; i < peakList.size(); i++)
        if (peakList.get(i).checkFileIndex(filePath, index)) {
          return (selectedPeak = peakList.get(i));
        }
    return null;
  }

  public boolean matchesPeakTypeModel(String type, String model) {
    if (peakList != null && peakList.size() > 0)
      for (int i = 0; i < peakList.size(); i++)
        if (peakList.get(i).checkTypeModel(type, model))
          return true;
    return false;
  }


  public void setSelectedPeak(PeakInfo peak) {
    selectedPeak = peak;
  }
  
  public PeakInfo getSelectedPeak() {
    return selectedPeak;
  }

  public PeakInfo getModelPeakInfo() {
    for (int i = 0; i < peakList.size(); i++)
      if (peakList.get(i).autoSelectOnLoad())
        return peakList.get(i);
    return null;
  }
  
  public PeakInfo getAssociatedPeakInfo(Coordinate coord) {
    selectedPeak = null;
    if (coord != null && peakList != null && peakList.size() > 0)
      for (int i = 0; i < peakList.size(); i++) {
        PeakInfo peak = peakList.get(i);
        double xVal = coord.getXVal();
        if (xVal >= peak.getXMin() && xVal <= peak.getXMax())
          return (selectedPeak = peak);
      }
    return null;
  }

  public String getPeakTitle() {
    return (selectedPeak == null ? getTitleLabel() : selectedPeak.getTitle());
  }

  public String getTitleLabel() {
    String type = (peakList == null || peakList.size() == 0 ? dataType
        : peakList.get(0).getType());
    return (type != null && type.length() > 0 ? type + " " : "") 
    + getTitle() 
    + (parent == null ? "" : " (" + parent.subSpectra.size() + ")");
  }

  public int setNextPeak(Coordinate coord, int istep) {
    if (peakList == null || peakList.size() == 0)
      return -1;
    double x0 = coord.getXVal() + istep * 0.000001;
    int ipt1 = -1;
    int ipt2 = -1;
    double dmin1 = Double.MAX_VALUE * istep;
    double dmin2 = 0;
    for (int i = peakList.size(); --i >= 0;) {
      double x = peakList.get(i).getX();
      if (istep > 0) {
        if (x > x0 && x < dmin1) {
          // nearest on right
          ipt1 = i;
          dmin1 = x;
        } else if (x < x0 && x - x0 < dmin2) {
          // farthest on left
          ipt2 = i;
          dmin2 = x - x0;
        }
      } else {
        if (x < x0 && x > dmin1) {
          // nearest on left
          ipt1 = i;
          dmin1 = x;
        } else if (x > x0 && x - x0 > dmin2) {
          // farthest on right
          ipt2 = i;
          dmin2 = x - x0;
        }
      }
    }

    if (ipt1 < 0) {
      if (ipt2 < 0)
        return -1;
      ipt1 = ipt2;
    }
    return ipt1;
  }
 
  public IntegralGraph getIntegrationGraph() {
    return integration;
  }
  
  public void setIntegrationGraph(IntegralGraph graph) {
    integration = graph;
  }

  private Comparator<Coordinate> c;
  
  public double getPercentYValueAt(double x) {
    if (!isContinuous())
      return Double.NaN;
    return getYValueAt(xyCoords, x);
  }

  public double getYValueAt(Coordinate[] xyCoords, double x) {
    if (c == null)
      c = new CoordComparator();
    return Coordinate.getYValueAt(xyCoords, x, c);
  }

  private JDXSpectrum convertedSpectrum;

  /**
   * when the user click VK_UP or VK_DOWN
   * 
   */
  private double userYFactor = 1;
  public void setUserYFactor(double userYFactor) {
    this.userYFactor = userYFactor;
  }

  public double getUserYFactor() {
    return userYFactor;
  }

  public static final int TA_NO_CONVERT = 0;
  public static final int TO_ABS = 1;
  public static final int TO_TRANS = 2;
  public static final int IMPLIED = 3;
  public static final double MAXABS = 4; // maximum absorbance allowed

  public JDXSpectrum getConvertedSpectrum() {
    return convertedSpectrum;
  }
  
  public void setConvertedSpectrum(JDXSpectrum spectrum) {
    convertedSpectrum = spectrum;
  }

  public static JDXSpectrum taConvert(JDXSpectrum spectrum, int mode) {
    if (!spectrum.isContinuous())
      return spectrum;
    switch (mode) {
    case TO_ABS:
      if (!spectrum.isTransmittance())
        return spectrum;
      break;
    case TO_TRANS:
      if (!spectrum.isAbsorbance())
        return spectrum;
      break;
    case IMPLIED:
      break;
    default:
      return spectrum;
    }
    JDXSpectrum spec = spectrum.getConvertedSpectrum();
    return (spec != null ? spec : spectrum.isAbsorbance() ? toT(spectrum) : toA(spectrum));
  }

  /**
   * Converts a spectrum from Absorbance to Transmittance
   * 
   * @param spectrum
   *        the JDXSpectrum
   * @return the converted spectrum
   */
  
  private static JDXSpectrum toT(JDXSpectrum spectrum) {
    if (!spectrum.isAbsorbance())
      return null;
    Coordinate[] xyCoords = spectrum.getXYCoords();
    Coordinate[] newXYCoords = new Coordinate[xyCoords.length];
    if (!Coordinate.isYInRange(xyCoords, 0, MAXABS))
      xyCoords = Coordinate.normalise(xyCoords, 0, MAXABS);
    for (int i = 0; i < xyCoords.length; i++)
      newXYCoords[i] = new Coordinate(xyCoords[i].getXVal(),
          toTransmittance(xyCoords[i].getYVal()));
    return newSpectrum(spectrum, newXYCoords, "TRANSMITTANCE");
  }

  /**
   * Converts a spectrum from Transmittance to Absorbance
   * 
   * @param spectrum
   *        the JDXSpectrum
   * @return the converted spectrum
   */
  private static JDXSpectrum toA(JDXSpectrum spectrum) {
    if (!spectrum.isTransmittance())
      return null;
    Coordinate[] xyCoords = spectrum.getXYCoords();
    Coordinate[] newXYCoords = new Coordinate[xyCoords.length];
    boolean isPercent = Coordinate.isYInRange(xyCoords, -2, 2);
    for (int i = 0; i < xyCoords.length; i++)
      newXYCoords[i] = new Coordinate(xyCoords[i].getXVal(), 
          toAbsorbance(xyCoords[i].getYVal(), isPercent));
    return newSpectrum(spectrum, newXYCoords, "ABSORBANCE");
  }

  /**
   * copy spectrum with new coordinates
   * 
   * @param spectrum
   * @param newXYCoords
   * @param units
   * @return
   */
  public static JDXSpectrum newSpectrum(JDXSpectrum spectrum,
                                         Coordinate[] newXYCoords,
                                         String units) {
    JDXSpectrum specNew = spectrum.copy();
    specNew.setOrigin("JSpecView Converted");
    specNew.setOwner("JSpecView Generated");
    specNew.setXYCoords(newXYCoords);
    specNew.setYUnits(units);
    spectrum.setConvertedSpectrum(specNew);
    specNew.setConvertedSpectrum(spectrum);
    return specNew;
  }

  /**
   * Converts a value in Transmittance to Absorbance -- max of MAXABS (4)
   * 
   * 
   * @param x
   * @param isPercent
   * @return the value in Absorbance
   */
  private static double toAbsorbance(double x, boolean isPercent) {
    return (Math.min(MAXABS, isPercent ? 2 - log10(x) : -log10(x)));
  }

  /**
   * Converts a value from Absorbance to Transmittance
   * 
   * @param x
   * @return the value in Transmittance
   */
  private static double toTransmittance(double x) {
    return (x <= 0 ? 1 : Math.pow(10, -x));
  }

  /**
   * Returns the log of a value to the base 10
   * 
   * @param value
   *        the input value
   * @return the log of a value to the base 10
   */
  private static double log10(double value) {
    return Math.log(value) / Math.log(10);
  }

  public IntegralGraph integrate(double minY, double offset, double factor) {
    if (!canIntegrate())
      return null;
    IntegralGraph graph = new IntegralGraph(this, minY, offset, factor, xUnits,
        yUnits);
    setIntegrationGraph(graph);
    return graph;
  }

  public static boolean areScalesCompatible(Graph s1, Graph s2,
                                            boolean allow2D2D) {
    if (!((allow2D2D ? s1.is1D() == s2.is1D() : s1.is1D() && s2.is1D()) 
        && s1.getXUnits().equalsIgnoreCase(s2.getXUnits())))
      return false;
    if (!(s1 instanceof JDXSpectrum) || !(s2 instanceof JDXSpectrum))
      return true;
    JDXSpectrum spec1 = (JDXSpectrum) s1;
    JDXSpectrum spec2 = (JDXSpectrum) s2;
    if (spec1.isHNMR() != spec2.isHNMR())
      return false;
    return true;
    
  }

  public static boolean process(List<JDXSpectrum> specs, int irMode,
                             boolean autoIntegrate, 
                             double minY, double offset, double factor) {
    boolean haveIntegral = false;
    if (irMode == TO_ABS || irMode == TO_TRANS)
      for (int i = 0; i < specs.size(); i++)
        specs.set(i, taConvert(specs.get(i), irMode));
    if (autoIntegrate)
      for (int i = 0; i < specs.size(); i++)
        haveIntegral |= (specs.get(i).integrate(minY, offset, factor) != null);
    return haveIntegral;
  }

  public List<Integral> getIntegrals() {
   return (integration == null ? null : integration.getIntegrals());
  }

  public List<JDXSpectrum> getSubSpectra() {
    return subSpectra;
  }
  
  public JDXSpectrum getCurrentSubSpectrum() {
    return (subSpectra == null ? this : subSpectra.get(currentSubSpectrumIndex));
  }

  public int advanceSubSpectrum(int dir) {
    return setCurrentSubSpectrum(currentSubSpectrumIndex + dir);
  }
  
  public int setCurrentSubSpectrum(int n) {
    return (currentSubSpectrumIndex = Coordinate.intoRange(n, 0, subSpectra.size() - 1));
  }

  /**
   * adds an nD subspectrum and titles it "Subspectrum <n>"
   * These spectra can be iterated over using the UP and DOWN keys.
   * 
   * @param spectrum
   * @return
   */
  public boolean addSubSpectrum(JDXSpectrum spectrum, boolean forceSub) {
    if (!forceSub && (numDim < 2 || blockID != spectrum.blockID)
        || !areScalesCompatible(this, spectrum, true))
      return false;
    isForcedSubset = forceSub; // too many blocks (>100)
    if (subSpectra == null) {
      subSpectra = new ArrayList<JDXSpectrum>();
      addSubSpectrum(this, true);
    }
    subSpectra.add(spectrum);
    spectrum.parent = this;
    spectrum.setTitle(subSpectra.size() + ": " + spectrum.title);
    //System.out.println("Added subspectrum " + subSpectra.size() + ": " + spectrum.y2D);
    return true;
  }
  
  public int getSubIndex() {
    return (subSpectra == null ? -1 : currentSubSpectrumIndex);
  }

  private boolean exportXAxisLeftToRight;

  public void setExportXAxisDirection(boolean leftToRight) {
    exportXAxisLeftToRight = leftToRight;
  }
  public boolean isExportXAxisLeftToRight() {
    return exportXAxisLeftToRight;
  }

  /**
   * 
   * @param width
   * @param height
   * @param isd
   * @return
   */
  public int[] get2dBuffer(int width, int height, ImageScaleData isd, boolean forceNew) {
    if (subSpectra == null || !subSpectra.get(0).isContinuous())
      return null;
    if (!forceNew && thisWidth == width && thisHeight == height)
      return buf2d;
    int nSpec = subSpectra.size();
    thisWidth = width = xyCoords.length;
    thisHeight = height = nSpec;
    double grayFactor = 255 / (isd.maxZ - isd.minZ);
    if (!forceNew && buf2d != null && grayFactor == grayFactorLast)
      return buf2d;
    grayFactorLast = grayFactor;
    int pt = width * height;
    int[] buf = new int[pt];
    for (int i = 0; i < nSpec; i++) {
      Coordinate[] points = subSpectra.get(i).xyCoords;
      if (points.length != xyCoords.length)
        return null;
      double f = subSpectra.get(i).getUserYFactor();
      for (int j = 0; j < xyCoords.length; j++)
        buf[--pt] = 255 - Coordinate.intoRange((int) ((points[j].getYVal()* f - isd.minZ) * grayFactor), 0, 255);
    }
    return (buf2d = buf);
  }

  public Map<String, Object> getInfo(String key) {
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("id", id);
    boolean justHeader = ("header" == key);
    Map<String, Object> head = new Hashtable<String, Object>();
    String[][] list = getHeaderRowDataAsArray();
    for (int i = 0; i < list.length; i++) {
      String label = JDXSourceStreamTokenizer.cleanLabel(list[i][0]);
      if (key != null && !justHeader && !label.equals(key))
        continue;
      Object val = fixInfoValue(list[i][1]);
      if (key == null) {
        Map<String, Object> data = new Hashtable<String, Object>();
        data.put("value", val);
        data.put("index", Integer.valueOf(i + 1));
        info.put(label, data);
      } else {
        info.put(label, val);
      }
    }
    info.put("header", head);
    if (!justHeader) {
      putInfo(key, info, "type", getDataType());
      putInfo(key, info, "isHZToPPM", Boolean.valueOf(isHZtoPPM));
      putInfo(key, info, "subSpectrumCount", Integer
          .valueOf(subSpectra == null ? 0 : subSpectra.size()));
    }
    return info;
  }

  private static Object fixInfoValue(String info) {
    try { return (Integer.valueOf(info)); } catch (Exception e) {}
    try { return (Double.valueOf(info)); } catch (Exception e) {}
    return info;
  }

  public static void putInfo(String match, Map<String, Object> info,
                             String key, Object value) {
    if (match == null || key.equalsIgnoreCase(match))
      info.put(key, value);
  }
  
  @Override
  public String toString() {
    return getTitleLabel();
  }

  public boolean isAutoOverlayFromJmolClick() {
    return (dataType.equalsIgnoreCase("GC") || dataType.startsWith("VIS"));
  }


}