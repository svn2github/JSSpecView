/* Copyright (c) 2002-2007 The University of the West Indies
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

import java.util.Vector;

import jspecview.util.Coordinate;
import jspecview.util.JSpecViewUtils;


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
 /**
   * The input graph
   */
  private Graph graph;

  /**
   * The minimum percent of Y values to use in calulating the integral
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
  private Coordinate coordinates[];

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
   * Calculates and intialises the <code>IntegralGraph</code> from an input <code>Graph</code>,
   *  the percentage Minimum Y value, the percent offset and the integral factor
   * @param graph the input graph
   * @param percentMinY percentage Minimum Y value
   * @param percentOffset the percent offset
   * @param integralFactor the integral factor
   */
  public IntegralGraph(Graph graph, double percentMinY, double percentOffset,
                       double integralFactor) {
    this.graph = graph;
    this.percentMinY = percentMinY;
    this.percentOffset = percentOffset;
    this.integralFactor = integralFactor;
    coordinates = calculateIntegral();
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
    return coordinates.length;
  }

  /**
   * Method from the <code>Graph</code> Interface. Returns an array of
   * Coordinates of the <code>IntegralGraph</code>.
   * @return an array of the Coordinates of the <code>IntegralGraph</code.
   * @see jspecview.common.Graph#getXYCoords()
   */
  public Coordinate[] getXYCoords() {
    return coordinates;
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
   * Recalutes the intregral
   */
  public void recalculate(){
    coordinates = calculateIntegral();
  }

  /**
   * Calulates the integral from the input <code>Graph</code>
   * @return the array of coordinates of the Integral
   * @see jspecview.IntegralGraph#recalculate()
   */
  private Coordinate[] calculateIntegral(){
    double totalIntegral = 0;
    double integral = 0;
    Coordinate[] xyCoords = graph.getXYCoords();
    double minY = JSpecViewUtils.getMinY(xyCoords);
    double maxY = JSpecViewUtils.getMaxY(xyCoords);
    double minYForIntegral = percentMinY / 100 * maxY; // 0.1%
    Vector<Coordinate> integralCoords = new Vector<Coordinate>();


    // Find total integral
    for(int i = 0; i < xyCoords.length; i++){
      double y = xyCoords[i].getYVal();
      if(y > minYForIntegral){
       totalIntegral += y;
      }
    }

    double totalIntegralScalefactor = maxY / totalIntegral;
    double factor = (integralFactor / 100) * totalIntegralScalefactor;  // 50%
    double offset = (percentOffset/100) * maxY;

    // Calculate Integral Graph

    for(int i = xyCoords.length-1; i >= 0; i--){
      double y = xyCoords[i].getYVal();
      if(y > minYForIntegral){
       integral += y;
      }else{
        integral += 0;
      }

      double newY = integral * factor + offset; // + offset
      integralCoords.insertElementAt(new Coordinate(xyCoords[i].getXVal(), newY), 0);
    }




    Coordinate[] integralCoordsArray;
    Coordinate[] tempCoords = new Coordinate[integralCoords.size()];
    integralCoordsArray = (Coordinate[])integralCoords.toArray(tempCoords);

    return integralCoordsArray;
  }

}
