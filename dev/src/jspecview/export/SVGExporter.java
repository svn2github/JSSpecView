/* Copyright (c) 2002-2010 The University of the West Indies
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

package jspecview.export;

import java.awt.Color;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Vector;

import jspecview.common.Coordinate;
import jspecview.common.Graph;
import jspecview.common.JSVPanel;
import jspecview.common.JSpecViewUtils;
import jspecview.application.PreferencesDialog;

/**
 * class <code>SVGExporter</code> contains static methods to export a Graph as
 * as SVG. Uses a template file called 'plot.vm'. So any changes in design should
 * be done in this file.
 * 
 * Modified 
 * 6 Oct 2010  added lastX for Inkscape SVG export so baseline could be printed
 * 
 * @see jspecview.common.Graph
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 */
public class SVGExporter extends FormExporter {

  /**
   * The width of the SVG
   */
  private static int svgWidth = 850;

  /**
   * The height of the SVG
   */
  private static int svgHeight = 400;

  /**
   * the left inset of the plot area of the SVG
   */
  private static int leftInset = 100;

  /**
   * The right inset of the plot area of the SVG
   */
  private static int rightInset = 200;

  /**
   * The bottom inset of the plot area of the SVG
   */
  private static int bottomInset = 80;

  /**
   * The top inset of the plot area of the SVG
   */
  private static int topInset = 20;

  /**
   * Export a Graph as SVG to a file given by fileName
   * @param path the file path
   * @param graph the Graph
   * @param startIndex
   * @param endIndex
   * @return data if fileName is null
   * @throws IOException
   */
  String exportAsSVG(String path, Graph graph, int startIndex, int endIndex)
      throws IOException {
    return exportAsSVG(path, graph.getXYCoords(), "", startIndex,
        endIndex, graph.getXUnits(), graph.getYUnits(), graph.isContinuous(),
        graph.isIncreasing(), Color.lightGray, Color.white, Color.black,
        Color.gray, Color.black, Color.black, Color.black);
  }

  /**
   * Exports the Graphs that are displayed by JSVPanel to a file given by fileName,
   * with the same Color Scheme as the JSVPanel. If graph is zoomed then export
   * the current view if doZoom is true
   * @param fileName the name of the file
   * @param jsvp the JSVPanel
   * @param specIndex the index of the graph to export
   * @param doZoom if true then if graph is zoomed then export the current view
   * @throws IOException
   */
  public void exportAsSVG(String fileName, JSVPanel jsvp, int specIndex,
                          boolean doZoom) throws IOException {
    Graph graph = (Graph) jsvp.getSpectrumAt(specIndex);
    int startIndex, endIndex;

    if (doZoom) {
      startIndex = jsvp.getStartDataPointIndices()[specIndex];
      endIndex = jsvp.getEndDataPointIndices()[specIndex];
    } else {
      startIndex = 0;
      endIndex = graph.getNumberOfPoints() - 1;
    }

    exportAsSVG(fileName, graph.getXYCoords(), graph.getTitle(), startIndex,
        endIndex, graph.getXUnits(), graph.getYUnits(), graph.isContinuous(),
        graph.isIncreasing(), jsvp.getPlotAreaColor(), jsvp.getBackground(),
        jsvp.getPlotColor(0), jsvp.getGridColor(), jsvp.getTitleColor(), jsvp
            .getScaleColor(), jsvp.getUnitsColor());
  }

  /**
   * Export a graph as SVG with specified Coordinates and Colors
   * @param fileName
   * @param xyCoords an array of Coordinates
   * @param title the title of the graph
   * @param startDataPointIndex the start index of the coordinates
   * @param endDataPointIndex the end index of the coordinates
   * @param xUnits the units of the x axis
   * @param yUnits the units of the y axis
   * @param continuous true if the graph is continuous, otherwise false
   * @param increasing true is the graph is increasing, otherwise false
   * @param plotAreaColor the color of the plot area
   * @param backgroundColor the color of the background
   * @param plotColor the color of the plot
   * @param gridColor the color of the grid
   * @param titleColor the color of the title
   * @param scaleColor the color of the scales
   * @param unitsColor the color of the units
   * @return data if fileName is null
   * @throws IOException
   */
  private String exportAsSVG(String fileName, Coordinate[] xyCoords, String title,
                           int startDataPointIndex, int endDataPointIndex,
                           String xUnits, String yUnits, boolean continuous,
                           boolean increasing, Color plotAreaColor,
                           Color backgroundColor, Color plotColor,
                           Color gridColor, Color titleColor, Color scaleColor,
                           Color unitsColor) throws IOException {

    initForm(fileName);

    //DecimalFormat formatter = new DecimalFormat("0.000000", new DecimalFormatSymbols(java.util.Locale.US ));
    DecimalFormat formatter2 = new DecimalFormat("0.######",
        new DecimalFormatSymbols(java.util.Locale.US));

    JSpecViewUtils.ScaleData scaleData = JSpecViewUtils.generateScaleData(
        xyCoords, startDataPointIndex, endDataPointIndex, 10, 10);

    double maxXOnScale = scaleData.maxXOnScale;
    double minXOnScale = scaleData.minXOnScale;
    double maxYOnScale = scaleData.maxYOnScale;
    double minYOnScale = scaleData.minYOnScale;
    double xStep = scaleData.xStep;
    double yStep = scaleData.yStep;
    int hashNumX = scaleData.hashNumX;
    int hashNumY = scaleData.hashNumY;

    int plotAreaWidth = svgWidth - leftInset - rightInset;
    int plotAreaHeight = svgHeight - topInset - bottomInset;
    double xScaleFactor = (plotAreaWidth / (maxXOnScale - minXOnScale));
    double yScaleFactor = (plotAreaHeight / (maxYOnScale - minYOnScale));
    int leftPlotArea = leftInset;
    int rightPlotArea = leftInset + plotAreaWidth;
    int topPlotArea = topInset;
    int bottomPlotArea = topInset + plotAreaHeight;
    int titlePosition = bottomPlotArea + 60;
    context.put("titlePosition", new Integer(titlePosition));


    double xPt, yPt;
    String xStr, yStr;

    //Grid
    Vector<HashMap<String, String>> vertGridCoords = new Vector<HashMap<String, String>>();
    Vector<HashMap<String, String>> horizGridCoords = new Vector<HashMap<String, String>>();

    for (double i = minXOnScale; i < maxXOnScale + xStep / 2; i += xStep) {
      xPt = leftPlotArea + ((i - minXOnScale) * xScaleFactor);
      yPt = topPlotArea;
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);
      vertGridCoords.addElement(hash);
    }

    for (double i = minYOnScale; i < maxYOnScale + yStep / 2; i += yStep) {
      xPt = leftPlotArea;
      yPt = topPlotArea + ((i - minYOnScale) * yScaleFactor);
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);

      horizGridCoords.addElement(hash);
    }

    // Scale

    Vector<HashMap<String, String>> xScaleList = new Vector<HashMap<String, String>>();
    Vector<HashMap<String, String>> xScaleListReversed = new Vector<HashMap<String, String>>();
    Vector<HashMap<String, String>> yScaleList = new Vector<HashMap<String, String>>();

    String hashX = "#";
    String hashY = "#";
    String hash1 = "0.00000000";

    if (hashNumX <= 0)
      hashX = hash1.substring(0, Math.abs(hashNumX) + 3);

    DecimalFormat displayXFormatter = new DecimalFormat(hashX,
        new DecimalFormatSymbols(java.util.Locale.US));

    if (hashNumY <= 0)
      hashY = hash1.substring(0, Math.abs(hashNumY) + 3);

    DecimalFormat displayYFormatter = new DecimalFormat(hashY,
        new DecimalFormatSymbols(java.util.Locale.US));

    for (double i = minXOnScale; i < (maxXOnScale + xStep / 2); i += xStep) {
      xPt = leftPlotArea + ((i - minXOnScale) * xScaleFactor);
      xPt -= 10; // shift to left by 10
      yPt = bottomPlotArea + 15; // shift down by 15
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);
      String iStr = displayXFormatter.format(i);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);
      hash.put("number", iStr);
      xScaleList.addElement(hash);
    }

    for (double i = minXOnScale, j = maxXOnScale; i < (maxXOnScale + xStep / 2); i += xStep, j -= xStep) {
      xPt = leftPlotArea + ((j - minXOnScale) * xScaleFactor);
      xPt -= 10;
      yPt = bottomPlotArea + 15; // shift down by 15
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);
      String iStr = displayXFormatter.format(i);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);
      hash.put("number", iStr);
      xScaleListReversed.addElement(hash);

    }

    for (double i = minYOnScale; (i < maxYOnScale + yStep / 2); i += yStep) {
      xPt = leftPlotArea - 55;
      yPt = bottomPlotArea - ((i - minYOnScale) * yScaleFactor);
      yPt += 3; // shift down by three
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);
      String iStr = displayYFormatter.format(i);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);
      hash.put("number", iStr);
      yScaleList.addElement(hash);
    }

    double firstTranslateX, firstTranslateY, secondTranslateX, secondTranslateY;
    double scaleX, scaleY;

    if (increasing) {
      firstTranslateX = leftPlotArea;
      firstTranslateY = bottomPlotArea;
      scaleX = xScaleFactor;
      scaleY = -yScaleFactor;
      secondTranslateX = -1 * minXOnScale;
      secondTranslateY = -1 * minYOnScale;
    } else {
      firstTranslateX = rightPlotArea;
      firstTranslateY = bottomPlotArea;
      scaleX = -xScaleFactor;
      scaleY = -yScaleFactor;
      secondTranslateX = -minXOnScale;
      secondTranslateY = -minYOnScale;
    }

 double yTickA= minYOnScale -(yStep/2);
 double yTickB= yStep/5;
 
    context
        .put("plotAreaColor", JSpecViewUtils.colorToHexString(plotAreaColor));
    context.put("backgroundColor", JSpecViewUtils
        .colorToHexString(backgroundColor));
    context.put("plotColor", JSpecViewUtils.colorToHexString(plotColor));
    context.put("gridColor", JSpecViewUtils.colorToHexString(gridColor));
    context.put("titleColor", JSpecViewUtils.colorToHexString(titleColor));
    context.put("scaleColor", JSpecViewUtils.colorToHexString(scaleColor));
    context.put("unitsColor", JSpecViewUtils.colorToHexString(unitsColor));

    context.put("svgHeight", new Integer(svgHeight));
    context.put("svgWidth", new Integer(svgWidth));
    context.put("leftPlotArea", new Integer(leftPlotArea));
    context.put("rightPlotArea", new Integer(rightPlotArea));
    context.put("topPlotArea", new Integer(topPlotArea));
    context.put("bottomPlotArea", new Integer(bottomPlotArea));
    context.put("plotAreaHeight", new Integer(plotAreaHeight));
    context.put("plotAreaWidth", new Integer(plotAreaWidth));

    context.put("minXOnScale", new Double(minXOnScale));
    context.put("maxXOnScale", new Double(maxXOnScale));
    context.put("minYOnScale", new Double(minYOnScale));
    context.put("maxYOnScale", new Double(maxYOnScale));
    context.put("yTickA", new Double(yTickA));
    context.put("yTickB", new Double(yTickB));
    context.put("xScaleFactor", new Double(xScaleFactor));
    context.put("yScaleFactor", new Double(yScaleFactor));

    context.put("increasing", new Boolean(increasing));

    context.put("verticalGridCoords", vertGridCoords);
    context.put("horizontalGridCoords", horizGridCoords);

    Vector<Coordinate> newXYCoords = new Vector<Coordinate>();
    for (int i = startDataPointIndex; i <= endDataPointIndex; i++)
      newXYCoords.addElement(xyCoords[i]);

    double firstX, firstY, lastX;
    firstX=xyCoords[startDataPointIndex].getXVal();
    firstY=xyCoords[startDataPointIndex].getYVal();
    lastX=xyCoords[endDataPointIndex].getXVal();
    
    context.put("title", title);
    context.put("xyCoords", newXYCoords);
    context.put("continuous", new Boolean(continuous));
    context.put("firstTranslateX", new Double(firstTranslateX));
    context.put("firstTranslateY", new Double(firstTranslateY));
    context.put("scaleX", new Double(scaleX));
    context.put("scaleY", new Double(scaleY));
    context.put("secondTranslateX", new Double(secondTranslateX));
    context.put("secondTranslateY", new Double(secondTranslateY));

    if (increasing) {
      context.put("xScaleList", xScaleList);
      context.put("xScaleListReversed", xScaleListReversed);
    } else {
      context.put("xScaleList", xScaleListReversed);
      context.put("xScaleListReversed", xScaleList);
    }
    context.put("yScaleList", yScaleList);

    context.put("xUnits", xUnits);
    context.put("yUnits", yUnits);
    context.put("firstX", firstX);
    context.put("firstY", firstY);
    context.put("lastX", lastX);

    int xUnitLabelX = rightPlotArea - 50;
    int xUnitLabelY = bottomPlotArea + 30;
    int yUnitLabelX = leftPlotArea - 80;
    int yUnitLabelY = bottomPlotArea / 2;
    int tempX = yUnitLabelX;
    yUnitLabelX = -yUnitLabelY;
    yUnitLabelY = tempX;
    context.put("xUnitLabelX", "" + xUnitLabelX);
    context.put("xUnitLabelY", "" + xUnitLabelY);
    context.put("yUnitLabelX", "" + yUnitLabelX);
    context.put("yUnitLabelY", "" + yUnitLabelY);

    context.put("numDecimalPlacesX", new Integer(Math.abs(hashNumX)));
    context.put("numDecimalPlacesY", new Integer(Math.abs(hashNumY)));

    if (PreferencesDialog.inkscape){
        System.out.println("inkscape is true ");
        return writeForm("plot_ink.vm");
    }else {
        System.out.println("inkscape is false ");
        return writeForm("plot.vm");
    }
  }

  /**
   * Export an overlayed graph as SVG with specified Coordinates and Colors
   * @param fileName
   * @param xyCoordsList an array of arrays of Coordinates
   * @param title the title of the graph
   * @param startDataPointIndices the start indices of the coordinates
   * @param endDataPointIndices the end indices of the coordinates
   * @param xUnits the units of the x axis
   * @param yUnits the units of the y axis
   * @param continuous true if the graph is continuous, otherwise false
   * @param increasing true is the graph is increasing, otherwise false
   * @param plotAreaColor the color of the plot area
   * @param backgroundColor the color of the background
   * @param plotColor the color of the plot
   * @param gridColor the color of the grid
   * @param titleColor the color of the title
   * @param scaleColor the color of the scales
   * @param unitsColor the color of the units
   * @return data if fileName is null
   * @throws IOException
   */
  public String exportAsSVG(String fileName, Coordinate[][] xyCoordsList,
                                 String title, int[] startDataPointIndices,
                                 int[] endDataPointIndices, String xUnits,
                                 String yUnits, boolean continuous,
                                 boolean increasing, Color plotAreaColor,
                                 Color backgroundColor, Color plotColor,
                                 Color gridColor, Color titleColor,
                                 Color scaleColor, Color unitsColor) throws IOException {
    initForm(fileName);
    //DecimalFormat formatter = new DecimalFormat("0.000000", new DecimalFormatSymbols(java.util.Locale.US ));
    DecimalFormat formatter2 = new DecimalFormat("0.######",
        new DecimalFormatSymbols(java.util.Locale.US));

    JSpecViewUtils.MultiScaleData scaleData = JSpecViewUtils.generateScaleData(
        xyCoordsList, startDataPointIndices, endDataPointIndices, 10, 10);

    double maxXOnScale = scaleData.maxXOnScale;
    double minXOnScale = scaleData.minXOnScale;
    double maxYOnScale = scaleData.maxYOnScale;
    double minYOnScale = scaleData.minYOnScale;
    double xStep = scaleData.xStep;
    double yStep = scaleData.yStep;
    int hashNumX = scaleData.hashNumX;
    int hashNumY = scaleData.hashNumY;

    int plotAreaWidth = svgWidth - leftInset - rightInset;
    int plotAreaHeight = svgHeight - topInset - bottomInset;
    double xScaleFactor = (plotAreaWidth / (maxXOnScale - minXOnScale));
    double yScaleFactor = (plotAreaHeight / (maxYOnScale - minYOnScale));
    int leftPlotArea = leftInset;
    int rightPlotArea = leftInset + plotAreaWidth;
    int topPlotArea = topInset;
    int bottomPlotArea = topInset + plotAreaHeight;

    //BufferedWriter buffWriter = null;
    //buffWriter = new BufferedWriter(writer);

    double xPt, yPt;
    String xStr, yStr;

    //Grid
    Vector<HashMap<String, String>> vertGridCoords = new Vector<HashMap<String, String>>();
    Vector<HashMap<String, String>> horizGridCoords = new Vector<HashMap<String, String>>();

    for (double i = minXOnScale; i < maxXOnScale + xStep / 2; i += xStep) {
      xPt = leftPlotArea + ((i - minXOnScale) * xScaleFactor);
      yPt = topPlotArea;
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);

      vertGridCoords.addElement(hash);
    }

    for (double i = minYOnScale; i < maxYOnScale + yStep / 2; i += yStep) {
      xPt = leftPlotArea;
      yPt = topPlotArea + ((i - minYOnScale) * yScaleFactor);
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);

      horizGridCoords.addElement(hash);
    }

    // Scale

    Vector<HashMap<String, String>> xScaleList = new Vector<HashMap<String, String>>();
    Vector<HashMap<String, String>> xScaleListReversed = new Vector<HashMap<String, String>>();
    Vector<HashMap<String, String>> yScaleList = new Vector<HashMap<String, String>>();

    String hashX = "#";
    String hashY = "#";
    String hash1 = "0.00000000";

    if (hashNumX <= 0)
      hashX = hash1.substring(0, Math.abs(hashNumX) + 3);

    DecimalFormat displayXFormatter = new DecimalFormat(hashX,
        new DecimalFormatSymbols(java.util.Locale.US));

    if (hashNumY <= 0)
      hashY = hash1.substring(0, Math.abs(hashNumY) + 3);

    DecimalFormat displayYFormatter = new DecimalFormat(hashY,
        new DecimalFormatSymbols(java.util.Locale.US));

    for (double i = minXOnScale; i < (maxXOnScale + xStep / 2); i += xStep) {
      xPt = leftPlotArea + ((i - minXOnScale) * xScaleFactor);
      xPt -= 10; // shift to left by 10
      yPt = bottomPlotArea + 15; // shift down by 15
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);
      String iStr = displayXFormatter.format(i);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);
      hash.put("number", iStr);
      xScaleList.addElement(hash);
    }

    for (double i = minXOnScale, j = maxXOnScale; i < (maxXOnScale + xStep / 2); i += xStep, j -= xStep) {
      xPt = leftPlotArea + ((j - minXOnScale) * xScaleFactor);
      xPt -= 10;
      yPt = bottomPlotArea + 15; // shift down by 15
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);
      String iStr = displayXFormatter.format(i);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);
      hash.put("number", iStr);
      xScaleListReversed.addElement(hash);

    }

    for (double i = minYOnScale; (i < maxYOnScale + yStep / 2); i += yStep) {
      xPt = leftPlotArea - 55;
      yPt = bottomPlotArea - ((i - minYOnScale) * yScaleFactor);
      yPt += 3; // shift down by three
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);
      String iStr = displayYFormatter.format(i);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);
      hash.put("number", iStr);
      yScaleList.addElement(hash);
    }

    double firstTranslateX, firstTranslateY, secondTranslateX, secondTranslateY;
    double scaleX, scaleY;

    if (increasing) {
      firstTranslateX = leftPlotArea;
      firstTranslateY = bottomPlotArea;
      scaleX = xScaleFactor;
      scaleY = -yScaleFactor;
      secondTranslateX = -1 * minXOnScale;
      secondTranslateY = -1 * minYOnScale;
    } else {
      firstTranslateX = rightPlotArea;
      firstTranslateY = bottomPlotArea;
      scaleX = -xScaleFactor;
      scaleY = -yScaleFactor;
      secondTranslateX = -minXOnScale;
      secondTranslateY = -minYOnScale;
    }

    context.put("plotAreaColor", JSpecViewUtils.colorToHexString(plotAreaColor));
    context.put("backgroundColor", JSpecViewUtils
        .colorToHexString(backgroundColor));
    context.put("plotColor", JSpecViewUtils.colorToHexString(plotColor));
    context.put("gridColor", JSpecViewUtils.colorToHexString(gridColor));
    context.put("titleColor", JSpecViewUtils.colorToHexString(titleColor));
    context.put("scaleColor", JSpecViewUtils.colorToHexString(scaleColor));
    context.put("unitsColor", JSpecViewUtils.colorToHexString(unitsColor));

    context.put("svgHeight", new Integer(svgHeight));
    context.put("svgWidth", new Integer(svgWidth));
    context.put("leftPlotArea", new Integer(leftPlotArea));
    context.put("rightPlotArea", new Integer(rightPlotArea));
    context.put("topPlotArea", new Integer(topPlotArea));
    context.put("bottomPlotArea", new Integer(bottomPlotArea));
    context.put("plotAreaHeight", new Integer(plotAreaHeight));
    context.put("plotAreaWidth", new Integer(plotAreaWidth));

    context.put("minXOnScale", new Double(minXOnScale));
    context.put("maxXOnScale", new Double(maxXOnScale));
    context.put("minYOnScale", new Double(minYOnScale));
    context.put("maxYOnScale", new Double(maxYOnScale));
    context.put("xScaleFactor", new Double(xScaleFactor));
    context.put("yScaleFactor", new Double(yScaleFactor));

    context.put("increasing", new Boolean(increasing));

    context.put("verticalGridCoords", vertGridCoords);
    context.put("horizontalGridCoords", horizGridCoords);

    Vector<Vector<Coordinate>> newXYCoordsList = new Vector<Vector<Coordinate>>();
    Vector<Coordinate> coords = new Vector<Coordinate>();
    for (int i = 0; i < xyCoordsList.length; i++) {
      for (int j = startDataPointIndices[i]; j <= endDataPointIndices[i]; j++)
        coords.addElement(xyCoordsList[i][j]);
      newXYCoordsList.addElement(coords);
    }

    context.put("overlaid", new Boolean(true));
    context.put("title", title);
    context.put("xyCoords", newXYCoordsList);
    context.put("continuous", new Boolean(continuous));
    context.put("firstTranslateX", new Double(firstTranslateX));
    context.put("firstTranslateY", new Double(firstTranslateY));
    context.put("scaleX", new Double(scaleX));
    context.put("scaleY", new Double(scaleY));
    context.put("secondTranslateX", new Double(secondTranslateX));
    context.put("secondTranslateY", new Double(secondTranslateY));

    if (increasing) {
      context.put("xScaleList", xScaleList);
      context.put("xScaleListReversed", xScaleListReversed);
    } else {
      context.put("xScaleList", xScaleListReversed);
      context.put("xScaleListReversed", xScaleList);
    }
    context.put("yScaleList", yScaleList);

    context.put("xUnits", xUnits);
    context.put("yUnits", yUnits);

    context.put("numDecimalPlacesX", new Integer(Math.abs(hashNumX)));
    context.put("numDecimalPlacesY", new Integer(Math.abs(hashNumY)));

    if (PreferencesDialog.inkscape){
        System.out.println("inkscape is true ");
        return writeForm("plot_ink.vm");
    }else {
        System.out.println("inkscape is false ");
        return writeForm("plot.vm");
    }
  }
}
