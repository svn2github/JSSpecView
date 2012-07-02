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
import java.util.List;
import java.util.StringTokenizer;

import jspecview.common.Integral;
import jspecview.util.TextFormat;


/**
 * class <code>IntegralGraph</code> implements the <code>Graph</code> interface.
 * It constructs an integral <code>Graph</code> from another <code>Graph</code>
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.common.Graph
 * @see jspecview.common.JDXSpectrum
 */
public class IntegralGraph implements Graph {

  public final static double DEFAULT_MINY = 0.1;
  public final static double DEFAULT_FACTOR = 50;
  public final static double DEFAULT_OFFSET = 30;

  private List<Integral> integralRegions;
  private JDXSpectrum spectrum;

  public void dispose() {
    integralRegions = null;
    spectrum = null;
  }

  /**
   * The minimum percent of Y values to use in calculating the integral
   */
  private double percentMinY;

  public double getPercentMinimumY() {
    return percentMinY;
  }

  /**
   * The percentage offset from the baseline of the input graph where the
   * <code>IntegralGraph</code> will be drawn
   */
  private double percentOffset;

  public double getPercentOffset() {
    return percentOffset;
  }


  /**
   * The y factor of input graph by which the <code>IntegralGraph</code>
   * will be drawn
   */
  private double integralFactor;

  public double getIntegralFactor() {
    return integralFactor;
  }

  /**
   * The array of the <code>IntegralGraph</code> coordinates
   */
  private Coordinate xyCoords[];

  /**
   * Calculates and initialises the <code>IntegralGraph</code> from an input <code>Graph</code>,
   *  the percentage Minimum Y value, the percent offset and the integral factor
   * @param graph the input graph
   * @param percentMinY percentage Minimum Y value
   * @param percentOffset the percent offset
   * @param integralFactor the integral factor
   */
  public IntegralGraph(JDXSpectrum spectrum, Parameters parameters, String xUnits, String yUnits) {
    this.spectrum = spectrum;
    this.percentMinY = parameters.integralMinY;
    this.percentOffset = parameters.integralOffset;
    this.integralFactor = parameters.integralFactor;
    xyCoords = calculateIntegral();
  }
  
  /**
   * Method from the <code>Graph</code> Interface. Returns an array of
   * Coordinates of the <code>IntegralGraph</code>.
   * @return an array of the Coordinates of the <code>IntegralGraph</code.
   * @see jspecview.common.Graph#getXYCoords()
   */
  public Coordinate[] getXYCoords() {
    return xyCoords;
  }

  /**
   * Recalculates the integral
   */
  public void recalculate(){
    xyCoords = calculateIntegral();
  }

  /**
   * Calculates the integral from the input <code>Graph</code>
   * 
   * @return the array of coordinates of the Integral
   * @see jspecview.IntegralGraph#recalculate()
   */
  private Coordinate[] calculateIntegral() {
    Coordinate[] xyCoords = spectrum.getXYCoords();
    Coordinate[] integralCoords = new Coordinate[xyCoords.length];

    double maxY = Coordinate.getMaxY(xyCoords, 0, xyCoords.length);
    double minYForIntegral = percentMinY / 100 * maxY; // 0.1%
    double integral = 0;
    for (int i = 0; i < xyCoords.length; i++) {
      double y = xyCoords[i].getYVal();
      if (y > minYForIntegral)
        integral += y;
    }

    double factor = (integralFactor / 100) / integral; 
    double offset = (percentOffset / 100);

    // Calculate Integral Graph as a scale from 0 to 1

    integral = 0;
    for (int i = xyCoords.length, j = 0; --i >= 0; j++) {
      double y = xyCoords[i].getYVal();
      if (y > minYForIntegral)
        integral += y;
      integralCoords[i] = new Coordinate(xyCoords[i].getXVal(), integral
          * factor + offset);
    }
    return integralCoords;
  }

  private static Comparator<Coordinate> c;
  
  /**
   * returns FRACTIONAL value * 100
   */
  public double getPercentYValueAt(double x) {
    double y = getYValueAt(x);
 //   double y0 = xyCoords[xyCoords.length - 1].getYVal();
 //   double y1 = xyCoords[0].getYVal();
 //   return (y - y0) / (y1 - y0) * 100;
    return y * 100;
  }

  private double getYValueAt(double x) {
    if (c == null)
      c = new CoordComparator();
    return Coordinate.getYValueAt(xyCoords, x, c);
  }

	/**
	 * Parses integration ratios and x values from a string and returns them as
	 * <code>IntegrationRatio</code> objects
	 * 
	 * @param value
	 * @return ArrayList<IntegrationRatio> object representing integration ratios
	 */
	public static ArrayList<Annotation> getIntegrationRatiosFromString(
			String value) {
		ArrayList<Annotation> ratios = new ArrayList<Annotation>();
		// split input into x-value/integral-value pairs
		StringTokenizer allParamTokens = new StringTokenizer(value, ",");
		while (allParamTokens.hasMoreTokens()) {
			String token = allParamTokens.nextToken();
			// now split the x-value/integral-value pair
			StringTokenizer eachParam = new StringTokenizer(token, ":");
			Annotation ratio = new Annotation(Double.parseDouble(eachParam
					.nextToken()), 0.0, eachParam.nextToken(), true, false, 0, 0);
			ratios.add(ratio);
		}
		return ratios;
	}

  public static final int INTEGRATE_MARK = 4;
  public static final int INTEGRATE_TOGGLE = 3;
  public static final int INTEGRATE_ON = 2;
  public static final int INTEGRATE_OFF = 1;  

  public void addIntegralRegion(double x1, double x2, boolean isFinal) {
    if (Double.isNaN(x1)) {
      integralRegions = null;
      return;
    }
    double intVal = Math.abs(getPercentYValueAt(x2) - getPercentYValueAt(x1));
    if (isFinal) {
      integralRegions.get(0).value = 0;
      if (intVal == 0)
        return;
    }
    if (integralRegions == null)
      integralRegions = new ArrayList<Integral>();
    Integral in = new Integral(intVal, x1, x2, getYValueAt(x1), getYValueAt(x2));
    clearIntegralRegions(x1, x2);
    if (isFinal || integralRegions.size() == 0) {
      integralRegions.add(in);
    } else {
      integralRegions.set(0, in);
    }
  }

  private void clearIntegralRegions(double x1, double x2) {
    // no overlapping integrals. Ignore first, which is the temporary one
    
    for (int i = integralRegions.size(); --i >= 1;) {
      Integral in = integralRegions.get(i);
      if (Math.min(in.x1, in.x2) < Math.max(x1, x2) 
          && Math.max(in.x1, in.x2) > Math.min(x1, x2))
        integralRegions.remove(i);
    }
    
  }

  public List<Integral> getIntegralRegions() {
    return integralRegions;
  }

  enum IntMode {
    OFF, ON, TOGGLE, MARK;
    static IntMode getMode(String value) {
      for (IntMode mode: values())
        if (mode.name().equalsIgnoreCase(value))
          return mode;
      return OFF;
    }
  }
  
  public void addMarks(String ppms) {
    //2-3,4-5,6-7...
    ppms = TextFormat.simpleReplace(" " + ppms, ",", " ");
    ppms = TextFormat.simpleReplace(ppms, " -"," #");
    ppms = TextFormat.simpleReplace(ppms, "--","-#");
    ppms = ppms.replace('-','^');
    ppms = ppms.replace('#','-');
    List<String> tokens = ScriptToken.getTokens(ppms);
    addIntegralRegion(0, 0, false);
    for (int i = tokens.size(); --i >= 0;) {
      String s = tokens.get(i);
      int pt = s.indexOf('^');
      if (pt < 0)
        continue;
      try {
        double x2 = Double.valueOf(s.substring(0, pt).trim());
        double x1 = Double.valueOf(s.substring(pt + 1).trim());
        addIntegralRegion(x1, x2, true);
      } catch (Exception e) {
        continue;
      }
    }
  }
//
//  public Map<String, Object> getInfo(String key) {
//    Map<String, Object> info = new Hashtable<String, Object>();
//    JDXSpectrum.putInfo(key, info, "type", "integration");
//    JDXSpectrum.putInfo(key, info, "percentMinY", Double.valueOf(percentMinY));
//    JDXSpectrum.putInfo(key, info, "percentOffset", Double.valueOf(percentOffset));
//    JDXSpectrum.putInfo(key, info, "integralFactor", Double.valueOf(integralFactor));
//    //TODO annotations
//    return info;
//  }

}
