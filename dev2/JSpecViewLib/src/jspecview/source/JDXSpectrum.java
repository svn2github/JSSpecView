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

package jspecview.source;

import org.jmol.util.JmolList;
import java.util.Hashtable;
import java.util.Map;


import org.jmol.util.Logger;

import jspecview.common.Coordinate;
import jspecview.common.Parameters;
import jspecview.common.PeakInfo;

/**
 * <code>JDXSpectrum</code> implements the Interface Spectrum for the display of
 * JDX Files.
 * 
 * @author Bob Hanson
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class JDXSpectrum extends JDXDataObject {

  public enum IRMode {
    NO_CONVERT, TO_TRANS, TO_ABS, TOGGLE;
    public static IRMode getMode(String value) {
    	switch (value == null ? 'I' : value.toUpperCase().charAt(0)) {
    	case 'A':
    		return TO_ABS;
    	case 'T':
    		return TO_TRANS;
    	case 'N':
    		return NO_CONVERT;
    	default:
    		return TOGGLE;
    	}
    }
  }
  
  @Override
  public void finalize() {
    System.out.println("JDXSpectrum " + this + " finalized " + title);
  }

  /**
   * spectra that can never be displayed independently, or at least not by default
   * 2D slices, for example.
   */
  private JmolList<JDXSpectrum> subSpectra;
  private JmolList<PeakInfo> peakList = new JmolList<PeakInfo>();
  private String piUnitsX, piUnitsY;
  private PeakInfo selectedPeak, highlightedPeak;
  private double specShift = 0;
  

  public void dispose() {
  	// NO! NEVER DO THIS!!! 
  	// Just because a spectrum is no longer needed by a graphSet does not mean it 
  	// is gone. 
//   if (subSpectra != null)
//    for (int i = 0; i < subSpectra.size(); i++)
//      if (subSpectra.get(i) != this)
//        subSpectra.get(i).dispose();
//    subSpectra = null;
//    parent = null;
//    peakList = null;
//    selectedPeak = null;
  }

  private int currentSubSpectrumIndex;
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
    headerTable = new JmolList<String[]>();
    xyCoords = new Coordinate[0];
    parent = this;
  }

  /**
   * specifically for Abs/Trans conversion. Note that we do NOT carry over piUnitsY
   * 
   * @return a copy of this <code>JDXSpectrum</code>
   */
  public JDXSpectrum copy() {
    JDXSpectrum newSpectrum = new JDXSpectrum();
    copyTo(newSpectrum);
    newSpectrum.setPeakList(peakList, piUnitsX, null);
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

  
  public JmolList<PeakInfo> getPeakList() {
    return peakList;
  }

  public int setPeakList(JmolList<PeakInfo> list, String piUnitsX, String piUnitsY) {
    peakList = list;
    System.out.println("setting peaklist for " + this + " to " + list);
    if (peakList == null)
    	System.out.println("ohoh");
    this.piUnitsX = piUnitsX;
    this.piUnitsY = piUnitsY;
    for (int i = list.size(); --i >= 0; )
      peakList.get(i).spectrum = this;
    if (Logger.debugging)
      Logger.info("Spectrum " + getTitle() + " peaks: " + list.size());
    return list.size();
  }

  public PeakInfo selectPeakByFileIndex(String filePath, String index) {
    if (peakList != null && peakList.size() > 0)
      for (int i = 0; i < peakList.size(); i++)
        if (peakList.get(i).checkFileIndex(filePath, index)) {
          System.out.println("selecting peak in " + this + " " + peakList.get(i));
          return (selectedPeak = peakList.get(i));
        }
    return null;
  }

  public PeakInfo selectPeakByFilePathTypeModel(String filePath, String type, String model) {
    if (peakList != null && peakList.size() > 0)
      for (int i = 0; i < peakList.size(); i++)
        if (peakList.get(i).checkFileTypeModel(filePath, type, model)) {
          System.out.println("selecting peak in " + this + " " + peakList.get(i));
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
  
  public void setHighlightedPeak(PeakInfo peak) {
    highlightedPeak = peak;
  }
  
  public PeakInfo getSelectedPeak() {
    return selectedPeak;
  }

  public PeakInfo getModelPeakInfoForAutoSelectOnLoad() {
  	if (peakList != null)
	    for (int i = 0; i < peakList.size(); i++)
	      if (peakList.get(i).autoSelectOnLoad())
	        return peakList.get(i);
    return null;
  }
  
  
  public PeakInfo getAssociatedPeakInfo(int xPixel, Coordinate coord) {
    selectedPeak = findPeakByCoord(xPixel, coord);
    return (selectedPeak == null ? getBasePeakInfo() : selectedPeak);
  }

  public PeakInfo findPeakByCoord(int xPixel, Coordinate coord) {
    if (coord != null && peakList != null && peakList.size() > 0) {
      double xVal = coord.getXVal();
      int iBest = -1;
      double dBest = 1e100;
      for (int i = 0; i < peakList.size(); i++) {
        double d = peakList.get(i).checkRange(xPixel, xVal);
        if (d < dBest) {
        	dBest = d;
        	iBest = i;
        }
      }
      if (iBest >= 0)
      	return peakList.get(iBest);
    }
    return null;   
  }

  public String getPeakTitle() {
    return (selectedPeak != null ? selectedPeak.getTitle() : highlightedPeak != null ? highlightedPeak.getTitle() : getTitleLabel());
  }

  public String getTitleLabel() {
    String type = (peakList == null || peakList.size() == 0 ? 
    		getQualifiedDataType() : peakList.get(0).getType());
    return (type != null && type.length() > 0 ? type + " " : "") 
    + getTitle();
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
 
  public double getPercentYValueAt(double x) {
    if (!isContinuous())
      return Double.NaN;
    return getYValueAt(x);
  }

  public double getYValueAt(double x) {
    return Coordinate.getYValueAt(xyCoords, x);
  }

  private JDXSpectrum convertedSpectrum;

  /**
   * based YFactor used in View command
   * 
   */
  private double userYFactor = 1;
  public void setUserYFactor(double userYFactor) {
    this.userYFactor = userYFactor;
  }

  public double getUserYFactor() {
    return userYFactor;
  }

  public static final double MAXABS = 4; // maximum absorbance allowed

  public JDXSpectrum getConvertedSpectrum() {
    return convertedSpectrum;
  }
  
  public void setConvertedSpectrum(JDXSpectrum spectrum) {
    convertedSpectrum = spectrum;
  }

  public static JDXSpectrum taConvert(JDXSpectrum spectrum, IRMode mode) {
    if (!spectrum.isContinuous())
      return spectrum;
    switch (mode) {
    case NO_CONVERT:
      return spectrum;
    case TO_ABS:
      if (!spectrum.isTransmittance())
        return spectrum;
      break;
    case TO_TRANS:
      if (!spectrum.isAbsorbance())
        return spectrum;
      break;
    case TOGGLE:
      break;
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
      newXYCoords[i] = new Coordinate().set(xyCoords[i].getXVal(),
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
      newXYCoords[i] = new Coordinate().set(xyCoords[i].getXVal(), 
          toAbsorbance(xyCoords[i].getYVal(), isPercent));
    return newSpectrum(spectrum, newXYCoords, "ABSORBANCE");
  }

  /**
   * copy spectrum with new coordinates
   * 
   * @param spectrum
   * @param newXYCoords
   * @param units
   * @return new spectrum
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

  public static boolean process(JmolList<JDXSpectrum> specs, IRMode irMode) {
    if (irMode == IRMode.TO_ABS || irMode == IRMode.TO_TRANS)
      for (int i = 0; i < specs.size(); i++)
        specs.set(i, taConvert(specs.get(i), irMode));
    return true;
  }

  public JmolList<JDXSpectrum> getSubSpectra() {
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
   * @param forceSub 
   * @return true if was possible
   */
  public boolean addSubSpectrum(JDXSpectrum spectrum, boolean forceSub) {
    if (!forceSub && (numDim < 2 || blockID != spectrum.blockID)
        || !allowSubSpec(this, spectrum))
      return false;
    isForcedSubset = forceSub; // too many blocks (>100)
    if (subSpectra == null) {
      subSpectra = new JmolList<JDXSpectrum>();
      addSubSpectrum(this, true);
    }
    subSpectra.addLast(spectrum);
    spectrum.parent = this;
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

  public Map<String, Object> getInfo(String key) {
    Map<String, Object> info = new Hashtable<String, Object>();
    if ("id".equalsIgnoreCase(key)) {
    	info.put(key, id);
    	return info;
    }
    info.put("id", id);
    Parameters.putInfo(key, info, "specShift", Double.valueOf(specShift));
    boolean justHeader = ("header".equals(key));
    if (!justHeader && key != null) {
      for (int i = headerTable.size(); --i >= 0;) {
      	String[] entry = headerTable.get(i);
      	if (entry[0].equalsIgnoreCase(key) || entry[2].equalsIgnoreCase(key)) {
      		info.put(key, entry[1]);
      		return info;
      	}      		
      }
    }
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
    if (head.size() > 0)
      info.put("header", head);
    if (!justHeader) {
      Parameters.putInfo(key, info, "titleLabel", getTitleLabel());
      Parameters.putInfo(key, info, "type", getDataType());
      Parameters.putInfo(key, info, "isHZToPPM", Boolean.valueOf(isHZtoPPM));
      Parameters.putInfo(key, info, "subSpectrumCount", Integer
          .valueOf(subSpectra == null ? 0 : subSpectra.size()));
    }
    return info;
  }

  private static Object fixInfoValue(String info) {
    try { return (Integer.valueOf(info)); } catch (Exception e) {}
    try { return (Double.valueOf(info)); } catch (Exception e) {}
    return info;
  }

  @Override
  public String toString() {
    return getTitleLabel();
  }

  public PeakInfo findMatchingPeakInfo(PeakInfo pi) {
    for (int i = 0; i < peakList.size(); i++)
      if (peakList.get(i).checkTypeMatch(pi))
        return peakList.get(i);
    return null;
  }

  public PeakInfo getBasePeakInfo() {
    return (peakList.size() == 0 ? new PeakInfo() : 
      new PeakInfo(" baseModel=\"\" " + peakList.get(0)));
  }

  /**
   * checks in order: (1) Peaks tag attribute xUnits/yUnits, 
   * then (2) ##XLABEL/##YLABEL, 
   * then (3) ##XUNITS/##YUNITS 
   * @param isX
   * @return  suitable label or ""
   */
  public String getAxisLabel(boolean isX) {
    String units = (isX ? piUnitsX : piUnitsY);
    if (units == null)
      units = (isX ? xLabel : yLabel);
    if (units == null)
      units = (isX ? xUnits : yUnits);
    return (units == null ? "" 
    		: units.equalsIgnoreCase("WAVENUMBERS") ? "1/cm"
    	  : units.equalsIgnoreCase("nanometers") ? "nm"
   			: units);
  }

	public double findXForPeakNearest(double x) {
		return Coordinate.findXForPeakNearest(xyCoords, x, isInverted());
	}

	public double addSpecShift(double dx) {
		if (dx != 0) {
			specShift += dx;
			Coordinate.shiftX(xyCoords, dx);
			if (subSpectra != null)
				for (int i = subSpectra.size(); --i >= 0;) {
					JDXSpectrum spec = subSpectra.get(i); 
					if (spec != this && spec != parent)
						spec.addSpecShift(dx);
				}
		}
		return specShift;
	}

	public static boolean allowSubSpec(JDXSpectrum s1, JDXSpectrum s2) {
		return (s1.is1D() == s2.is1D() 
				&& s1.xUnits.equalsIgnoreCase(s2.xUnits)
				&& s1.isHNMR() == s2.isHNMR());
	}

	public static boolean areXScalesCompatible(JDXSpectrum s1, JDXSpectrum s2,
			boolean isSubspecCheck, boolean isLinkCheck) {
		boolean isNMR1 = s1.isNMR();
		// must be both NMR or both not NMR
		if (isNMR1 != s2.isNMR())
			return false;
		// must have same xUnits if not a link check
		if (!isLinkCheck && !s1.xUnits.equalsIgnoreCase(s2.xUnits))
			return false;
		if (isSubspecCheck) {
			// must both be 1D (or both be 2D?) for adding subspectra
			if (s1.is1D() != s2.is1D())
				return false;
		} else if (isLinkCheck) {
			if (!isNMR1)
				return true;
			// we allow 1D/2D here
		} else if (!s1.is1D() || !s2.is1D()) {
			// otherwise we don't want to consider any 2D spectra
			return false;
		}
		// done if this is not NMR comparison
		// or check same nuclei // for now not going 1D-->2D
		return (!isNMR1 || s2.is1D() && s1.parent.nucleusX.equals(s2.parent.nucleusX));
	}

	public static boolean areLinkableX(JDXSpectrum s1, JDXSpectrum s2) {
		return (s1.isNMR() && s2.isNMR() && s1.nucleusX.equals(s2.nucleusX));
	}

	public static boolean areLinkableY(JDXSpectrum s1, JDXSpectrum s2) {
		return (s1.isNMR() && s2.isNMR() && s1.nucleusX.equals(s2.nucleusY));
	}

	public void setNHydrogens(int nH) {
		this.nH = nH;
	}

}