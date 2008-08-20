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

package jspecview.export;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Vector;

import jspecview.common.Coordinate;
import jspecview.common.Graph;
import jspecview.common.JSVPanel;
import jspecview.common.JSpecViewUtils;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;


/**
 * class <code>SVGExporter</code> contains static methods to export a Graph as
 * as SVG. <code>SVGExporter</code> uses <a href="http://jakarta.apache.org/velocity/">Velocity</a>
 * to write to a template file called 'plot.vm'. So any changes in design should
 * be done in this file.
 * @see jspecview.common.Graph
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class SVGExporter {

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
   * @param fileName the name of the file
   * @param graph the Graph
   * @throws IOException
   */

  /**
   * Export a Graph as SVG to a Writer given by writer
   * @param graph the Graph
   * @param path the file path
   * @param startIndex 
   * @param endIndex 
   * @throws IOException
   */
  static void exportAsSVG(Graph graph, String path, int startIndex, int endIndex) throws IOException {
    exportAsSVG(new FileWriter(path), graph.getXYCoords(), "", startIndex, endIndex,
                graph.getXUnits(), graph.getYUnits(),
                graph.isContinuous(), graph.isIncreasing(),
                Color.lightGray, Color.white, Color.black,
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
  public static void exportAsSVG(String fileName,  JSVPanel jsvp, int specIndex,
                                 boolean doZoom) throws IOException{
    FileWriter writer;
    writer = new FileWriter(fileName);
    Graph graph = (Graph)jsvp.getSpectrumAt(specIndex);
    int startIndex, endIndex;

    if(doZoom){
      startIndex = jsvp.getStartDataPointIndices()[specIndex];
      endIndex = jsvp.getEndDataPointIndices()[specIndex];
    }
    else{
      startIndex = 0;
      endIndex = graph.getNumberOfPoints() -1;
    }

    exportAsSVG(writer, graph.getXYCoords(), graph.getTitle(),
                startIndex, endIndex,
                graph.getXUnits(), graph.getYUnits(),
                graph.isContinuous(), graph.isIncreasing(),
                jsvp.getPlotAreaColor(), jsvp.getBackground(),
                jsvp.getPlotColor(0), jsvp.getGridColor(),
                jsvp.getTitleColor(), jsvp.getScaleColor(), jsvp.getUnitsColor());
  }


  /**
   * Export a graph as SVG with specified Coordinates and Colors
   * @param writer the Writer
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
   */
  private static void exportAsSVG(Writer writer, Coordinate[] xyCoords, String title,
                                  int startDataPointIndex, int endDataPointIndex,
                                 String xUnits, String yUnits,
                                 boolean continuous, boolean increasing,
                                 Color plotAreaColor, Color backgroundColor,
                                 Color plotColor, Color gridColor,
                                 Color titleColor, Color scaleColor,
                                 Color unitsColor){

    //DecimalFormat formatter = new DecimalFormat("0.000000", new DecimalFormatSymbols(java.util.Locale.US ));
    DecimalFormat formatter2 = new DecimalFormat("0.######", new DecimalFormatSymbols(java.util.Locale.US ));

    JSpecViewUtils.ScaleData scaleData =
      JSpecViewUtils.generateScaleData(xyCoords, startDataPointIndex, endDataPointIndex, 10, 10);

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
    double xScaleFactor = (plotAreaWidth/(maxXOnScale - minXOnScale));
    double yScaleFactor = (plotAreaHeight/(maxYOnScale - minYOnScale));
    int leftPlotArea = leftInset;
    int rightPlotArea = leftInset + plotAreaWidth;
    int topPlotArea = topInset;
    int bottomPlotArea = topInset + plotAreaHeight;

    //BufferedWriter buffWriter = null;
    //buffWriter = new BufferedWriter(writer);

    Template template = null;
    VelocityContext context = new VelocityContext();

    double xPt, yPt;
    String xStr, yStr;


    try{
      Velocity.init();
    }
    catch(Exception e){
    }

    //Grid
    Vector<HashMap<String, String>> vertGridCoords = new Vector<HashMap<String, String>>();
    Vector<HashMap<String, String>> horizGridCoords = new Vector<HashMap<String, String>>();

    for(double i = minXOnScale; i < maxXOnScale + xStep/2 ; i += xStep){
      xPt = leftPlotArea + ((i - minXOnScale) * xScaleFactor);
      yPt = topPlotArea;
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);

      vertGridCoords.addElement(hash);
    }

    for(double i = minYOnScale; i < maxYOnScale + yStep/2; i += yStep){
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
      hashX = hash1.substring(0,Math.abs(hashNumX)+3);

    DecimalFormat displayXFormatter = new DecimalFormat(hashX, new DecimalFormatSymbols(java.util.Locale.US ));

    if (hashNumY <= 0)
      hashY = hash1.substring(0,Math.abs(hashNumY)+3);

    DecimalFormat displayYFormatter = new DecimalFormat(hashY, new DecimalFormatSymbols(java.util.Locale.US ));

    for(double i = minXOnScale; i < (maxXOnScale + xStep/2); i += xStep){
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

    for(double i = minXOnScale, j = maxXOnScale; i < (maxXOnScale + xStep/2); i += xStep, j -= xStep){
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


    for(double i = minYOnScale; (i < maxYOnScale + yStep/2); i += yStep){
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

    if(increasing){
      firstTranslateX = leftPlotArea;
      firstTranslateY = bottomPlotArea;
      scaleX = xScaleFactor;
      scaleY = -yScaleFactor;
      secondTranslateX = -1 *minXOnScale;
      secondTranslateY = -1 * minYOnScale;
    }
    else{
      firstTranslateX = rightPlotArea;
      firstTranslateY = bottomPlotArea;
      scaleX = -xScaleFactor;
      scaleY = -yScaleFactor;
      secondTranslateX = -minXOnScale;
      secondTranslateY = -minYOnScale;
    }

    context.put("plotAreaColor", JSpecViewUtils.colorToHexString(plotAreaColor));
    context.put("backgroundColor", JSpecViewUtils.colorToHexString(backgroundColor));
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


    Vector<Coordinate> newXYCoords = new Vector<Coordinate>();
    for(int i = startDataPointIndex; i <= endDataPointIndex; i++)
      newXYCoords.addElement(xyCoords[i]);

    context.put("title", title);
    context.put("xyCoords", newXYCoords);
    context.put("continuous", new Boolean(continuous));
    context.put("firstTranslateX", new Double(firstTranslateX));
    context.put("firstTranslateY", new Double(firstTranslateY));
    context.put("scaleX", new Double(scaleX));
    context.put("scaleY", new Double(scaleY));
    context.put("secondTranslateX", new Double(secondTranslateX));
    context.put("secondTranslateY", new Double(secondTranslateY));


    if(increasing){
      context.put("xScaleList", xScaleList);
      context.put("xScaleListReversed", xScaleListReversed);
    }
    else{
      context.put("xScaleList", xScaleListReversed);
      context.put("xScaleListReversed", xScaleList);
    }
    context.put("yScaleList", yScaleList);

    context.put("xUnits", xUnits);
    context.put("yUnits", yUnits);

    context.put("numDecimalPlacesX", new Integer(Math.abs(hashNumX)));
    context.put("numDecimalPlacesY", new Integer(Math.abs(hashNumY)));

    // Take out System.out.println's
    // Throw exception instead

    try
    {
      template = Velocity.getTemplate("plot.vm");
      template.merge( context, writer);
    }
    catch( ResourceNotFoundException rnfe )
    {
      // couldn't find the template
      System.out.println("couldn't find the template");
    }
    catch( ParseErrorException pee )
    {
      // syntax error : problem parsing the template
      System.out.println("syntax error : problem parsing the template");
      pee.printStackTrace();
    }
    catch( MethodInvocationException mie )
    {
      // something invoked in the template
      // threw an exception
      System.out.println("something invoked in the template threw an exception");
    }
    catch( Exception e )
    {
      System.out.println("exception!");
    }


    try{
      writer.flush();
      writer.close();
    }
    catch(IOException ioe){
    }

  }

  /**
   * Export an overlayed graph as SVG with specified Coordinates and Colors
   * @param writer the Writer
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
   */
  public static void exportAsSVG(Writer writer, Coordinate[][] xyCoordsList, String title,
                                  int[] startDataPointIndices, int[] endDataPointIndices,
                                 String xUnits, String yUnits,
                                 boolean continuous, boolean increasing,
                                 Color plotAreaColor, Color backgroundColor,
                                 Color plotColor, Color gridColor,
                                 Color titleColor, Color scaleColor,
                                 Color unitsColor){
    //DecimalFormat formatter = new DecimalFormat("0.000000", new DecimalFormatSymbols(java.util.Locale.US ));
    DecimalFormat formatter2 = new DecimalFormat("0.######", new DecimalFormatSymbols(java.util.Locale.US ));

    JSpecViewUtils.MultiScaleData scaleData =
      JSpecViewUtils.generateScaleData(xyCoordsList, startDataPointIndices, endDataPointIndices, 10, 10);

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
    double xScaleFactor = (plotAreaWidth/(maxXOnScale - minXOnScale));
    double yScaleFactor = (plotAreaHeight/(maxYOnScale - minYOnScale));
    int leftPlotArea = leftInset;
    int rightPlotArea = leftInset + plotAreaWidth;
    int topPlotArea = topInset;
    int bottomPlotArea = topInset + plotAreaHeight;

    //BufferedWriter buffWriter = null;
    //buffWriter = new BufferedWriter(writer);

    Template template = null;
    VelocityContext context = new VelocityContext();

    double xPt, yPt;
    String xStr, yStr;


    try{
      Velocity.init();
    }
    catch(Exception e){
    }

    //Grid
    Vector<HashMap<String, String>> vertGridCoords = new Vector<HashMap<String, String>>();
    Vector<HashMap<String, String>> horizGridCoords = new Vector<HashMap<String, String>>();

    for(double i = minXOnScale; i < maxXOnScale + xStep/2 ; i += xStep){
      xPt = leftPlotArea + ((i - minXOnScale) * xScaleFactor);
      yPt = topPlotArea;
      xStr = formatter2.format(xPt);
      yStr = formatter2.format(yPt);

      HashMap<String, String> hash = new HashMap<String, String>();
      hash.put("xVal", xStr);
      hash.put("yVal", yStr);

      vertGridCoords.addElement(hash);
    }

    for(double i = minYOnScale; i < maxYOnScale + yStep/2; i += yStep){
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
      hashX = hash1.substring(0,Math.abs(hashNumX)+3);

    DecimalFormat displayXFormatter = new DecimalFormat(hashX, new DecimalFormatSymbols(java.util.Locale.US ));

    if (hashNumY <= 0)
      hashY = hash1.substring(0,Math.abs(hashNumY)+3);

    DecimalFormat displayYFormatter = new DecimalFormat(hashY, new DecimalFormatSymbols(java.util.Locale.US ));




    for(double i = minXOnScale; i < (maxXOnScale + xStep/2); i += xStep){
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

    for(double i = minXOnScale, j = maxXOnScale; i < (maxXOnScale + xStep/2); i += xStep, j -= xStep){
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


    for(double i = minYOnScale; (i < maxYOnScale + yStep/2); i += yStep){
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

    if(increasing){
      firstTranslateX = leftPlotArea;
      firstTranslateY = bottomPlotArea;
      scaleX = xScaleFactor;
      scaleY = -yScaleFactor;
      secondTranslateX = -1 *minXOnScale;
      secondTranslateY = -1 * minYOnScale;
    }
    else{
      firstTranslateX = rightPlotArea;
      firstTranslateY = bottomPlotArea;
      scaleX = -xScaleFactor;
      scaleY = -yScaleFactor;
      secondTranslateX = -minXOnScale;
      secondTranslateY = -minYOnScale;
    }

    context.put("plotAreaColor", JSpecViewUtils.colorToHexString(plotAreaColor));
    context.put("backgroundColor", JSpecViewUtils.colorToHexString(backgroundColor));
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
    for(int i = 0; i < xyCoordsList.length; i++) {
      for(int j = startDataPointIndices[i]; j <= endDataPointIndices[i]; j++)
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


    if(increasing){
      context.put("xScaleList", xScaleList);
      context.put("xScaleListReversed", xScaleListReversed);
    }
    else{
      context.put("xScaleList", xScaleListReversed);
      context.put("xScaleListReversed", xScaleList);
    }
    context.put("yScaleList", yScaleList);

    context.put("xUnits", xUnits);
    context.put("yUnits", yUnits);

    context.put("numDecimalPlacesX", new Integer(Math.abs(hashNumX)));
    context.put("numDecimalPlacesY", new Integer(Math.abs(hashNumY)));

    // Take out System.out.println's
    // Throw exception instead

    try
    {
      template = Velocity.getTemplate("plot.vm");
      template.merge( context, writer);
    }
    catch( ResourceNotFoundException rnfe )
    {
      // couldn't find the template
      System.out.println("couldn't find the template");
    }
    catch( ParseErrorException pee )
    {
      // syntax error : problem parsing the template
      System.out.println("syntax error : problem parsing the template");
      pee.printStackTrace();
    }
    catch( MethodInvocationException mie )
    {
      // something invoked in the template
      // threw an exception
      System.out.println("something invoked in the template threw an exception");
    }
    catch( Exception e )
    {
      System.out.println("exception!");
    }


    try{
      writer.flush();
      writer.close();
    }
    catch(IOException ioe){
    }
 }
}
