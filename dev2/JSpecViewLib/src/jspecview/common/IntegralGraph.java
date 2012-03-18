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
import java.util.List;
import java.util.Map;
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

  /**
   * The input graph
   */
  private Graph graph;

  /**
   * The minimum percent of Y values to use in calculating the integral
   */
  private double percentMinY;

  /**
   * The percentage offset from the baseline of the input graph where the
   * <code>IntegralGraph</code> will be drawn
   */
  private double percentOffset;

  /**
   * The y factor of input graph by which the <code>IntegralGraph</code>
   * will be drawn
   */
  private double integralFactor;

  /**
   * The array of the <code>IntegralGraph</code> coordinates
   */
  private Coordinate xyCoords[];

  /**
   * value of the x Units. Necessary for the implementation of the
   * Graph interface
   * @see jspecview.Graph
   */
  private String xUnits = "Arbitrary Units";

  /**
   * value of the y Units. Necessary for the implementation of the
   * Graph interface
   * @see jspecview.Graph
   */
  private String yUnits = "Arbitrary Units";

  /**
   * Calculates and initialises the <code>IntegralGraph</code> from an input <code>Graph</code>,
   *  the percentage Minimum Y value, the percent offset and the integral factor
   * @param graph the input graph
   * @param percentMinY percentage Minimum Y value
   * @param percentOffset the percent offset
   * @param integralFactor the integral factor
   */
  public IntegralGraph(Graph graph, double percentMinY, double percentOffset,
                       double integralFactor, String xUnits, String yUnits) {
    this.graph = graph;
    this.percentMinY = percentMinY;
    this.percentOffset = percentOffset;
    this.integralFactor = integralFactor;
    this.xUnits = xUnits;
    this.yUnits = yUnits;
    xyCoords = calculateIntegral();
  }

  /**
   * Sets the percent minimum y value
   * @param minY the percent minimum y value
   */
  public void setPercentMinimumY(double minY){
    percentMinY = minY;
  }

  /**
   * Sets the percent offset
   * @param offset the percent offset
   */
  public void setPercentOffset(double offset){
    percentOffset = offset;
  }

  /**
   * Sets the integral factor
   * @param factor the integral factor
   */
  public void setIntegralFactor(double factor){
    integralFactor = factor;
  }

  /**
   * Returns the percent minimum y value
   * @return the percent minimum y value
   */
  public double getPercentMinimumY(){
    return percentMinY;
  }

  /**
   * Returns the percent offset value
   * @return the percent offset value
   */
  public double getPercentOffset(){
    return percentOffset;
  }

  /**
   * Returns the integral factor
   * @return the integral factor
   */
  public double getIntegralFactor(){
    return integralFactor;
  }

  /**
   * Method from the <code>Graph</code> Interface
   * Determines if the the <code>IntegralGraph</code> is increasing
   * Depends on whether the input <code>Graph</code> is increasing
   * @return true is increasing, false otherwise
   * @see jspecview.common.Graph#isIncreasing()
   */
  public boolean isIncreasing() {
    return graph.isIncreasing();
  }

  /**
   * Method from the <code>Graph</code> Interface
   * Determines if the <code>IntegralGraph</code> is continuous
   * @return true
   * @see jspecview.common.Graph#isContinuous()
   */
  public boolean isContinuous() {
    return true;
  }

  /**
   * Method from the <code>Graph</code> Interface.
   * Returns the title of the <code>IntegralGraph</code>. The title is the
   * concatenation of the string "Integral of: " and the title of the input
   * graph.
   * @return the title of the <code>IntegralGraph</code>
   */
  public String getTitle() {
    return "Integral of: " + graph.getTitle();
  }

  public String getTitleLabel() {
    return getTitle();
  }

  /**
   * Method from the <code>Graph</code> Interface.
   * Returns the x units of the <code>IntegralGragh</code>.
   * @return returns the string "Arbitrary Units"
   * @see jspecview.common.Graph#getXUnits()
   */
  public String getXUnits() {
    return xUnits;
  }

  /**
   * Method from the <code>Graph</code> Interface.
   * Returns the y units of the <code>IntegralGragh</code>.
   * @return returns the string "Arbitrary Units"
   * @see jspecview.common.Graph#getYUnits()
   */
  public String getYUnits() {
    return yUnits;
  }

  /**
   * Method from the <code>Graph</code> Interface. Returns the number of
   * coordinates of the <code>IntegralGraph</code>.
   * @return the number of coordinates of the <code>IntegralGraph</code.
   * @see jspecview.common.Graph#getNumberOfPoints()
   */
  public int getNumberOfPoints() {
    return xyCoords.length;
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
   * Sets the x units
   * @param units the x units
   */
  public void setXUnits(String units){
    xUnits = units;
  }

  /**
   * Sets the y units
   * @param units the y units
   */
  public void setYUnits(String units){
    yUnits = units;
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
    Coordinate[] xyCoords = graph.getXYCoords();
    Coordinate[] integralCoords = new Coordinate[xyCoords.length];

    double maxY = Coordinate.getMaxY(xyCoords);
    double minYForIntegral = percentMinY / 100 * maxY; // 0.1%
    double integral = 0;
    for (int i = 0; i < xyCoords.length; i++) {
      double y = xyCoords[i].getYVal();
      if (y > minYForIntegral)
        integral += y;
    }

    double totalIntegralScalefactor = maxY / integral;
    double factor = (integralFactor / 100) * totalIntegralScalefactor; // 50%
    double offset = (percentOffset / 100) * maxY;

    // Calculate Integral Graph

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
    double y0 = xyCoords[xyCoords.length - 1].getYVal();
    double y1 = xyCoords[0].getYVal();
    return (y - y0) / (y1 - y0) * 100;
  }

  public double getYValueAt(double x) {
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
      Annotation ratio = new Annotation(Double
          .parseDouble(eachParam.nextToken()), 0.0, eachParam.nextToken(), true, false, 0, 0);
      ratios.add(ratio);
    }
    return ratios;
  }

  private List<Integral> integrals;
  public static final int INTEGRATE_MARK = 4;
  public static final int INTEGRATE_TOGGLE = 3;
  public static final int INTEGRATE_ON = 2;
  public static final int INTEGRATE_OFF = 1;  

  public void addIntegral(double x1, double x2, boolean isFinal) {
    if (Double.isNaN(x1)) {
      integrals = null;
      return;
    }
    double intVal = Math.abs(getPercentYValueAt(x2) - getPercentYValueAt(x1));
    if (isFinal) {
      integrals.get(0).value = 0;
      if (intVal == 0)
        return;
    }
    if (integrals == null)
      integrals = new ArrayList<Integral>();
    Integral in = new Integral(intVal, x1, x2, getYValueAt(x1), getYValueAt(x2));
    if (isFinal || integrals.size() == 0) {
      integrals.add(in);
    } else {
      integrals.set(0, in);
    }
  }

  public List<Integral> getIntegrals() {
    return integrals;
  }

  public static int getMode(String value) {
    return (value.equals("?") ? INTEGRATE_TOGGLE : value
        .equalsIgnoreCase("OFF") ? INTEGRATE_OFF : value
        .toUpperCase().startsWith("MARK") ? INTEGRATE_MARK
        : INTEGRATE_ON);
  }

  public void addMarks(String ppms) {
    //2-3,4-5,6-7...
    ppms = TextFormat.simpleReplace(" " + ppms, ",", " ");
    ppms = TextFormat.simpleReplace(ppms, " -"," #");
    ppms = TextFormat.simpleReplace(ppms, "--","-#");
    ppms = ppms.replace('-','^');
    ppms = ppms.replace('#','-');
    List<String> tokens = ScriptToken.getTokens(ppms);
    addIntegral(0, 0, false);
    for (int i = tokens.size(); --i >= 0;) {
      String s = tokens.get(i);
      int pt = s.indexOf('^');
      if (pt < 0)
        continue;
      try {
        double x2 = Double.valueOf(s.substring(0, pt).trim());
        double x1 = Double.valueOf(s.substring(pt + 1).trim());
        addIntegral(x1, x2, true);
      } catch (Exception e) {
        continue;
      }
    }
  }

  public double getUserYFactor() {
    return 1;
  }

  public Map<String, Object> getInfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("type", "integration");
    info.put("percentMinY", Double.valueOf(percentMinY));
    info.put("percentOffset", Double.valueOf(percentOffset));
    info.put("integralFactor", Double.valueOf(integralFactor));
    //TODO annotations
    return info;
  }

}
