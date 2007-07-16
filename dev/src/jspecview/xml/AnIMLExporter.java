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

package jspecview.xml;


import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Vector;

import jspecview.common.JDXSpectrum;
import jspecview.util.Coordinate;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import java.text.SimpleDateFormat;
import java.lang.Number;
import java.util.Calendar;
import java.util.Iterator;

/**
 * class <code>AnIMLExporter</code> contains static methods to export a Graph as
 * as AnIML. <code>AnIMLExporter</code> uses <a href="http://jakarta.apache.org/velocity/">Velocity</a>
 * to write to a template file called 'animl_tmp.vm' or 'animl_nmr.vm'. So any changes in design should
 * be done in these files.
 * @see jspecview.common.Graph
 * @author Prof Robert J. Lancashire
 */
public class AnIMLExporter {

  private static String amlFile;

  /**
   * Export full Spectrum as AnIML to a file given by fileName
   * @param spec the spectrum to export
   * @param fileName the name of the file
   * @throws IOException
   */

  public static void exportAsAnIML(JDXSpectrum spec, String fileName) throws IOException{
    FileWriter writer;
    writer = new FileWriter(fileName);
    amlFile= fileName;

    exportAsAnIML(spec, writer,  0, spec.getNumberOfPoints() -1);
  }

  /**
   * Export full Spectrum as AnIML to a Writer given by writer
   * @param spec the spectrum to export
   * @param writer the Writer
   * @throws IOException
   */
  public static void exportAsAnIML(JDXSpectrum spec, Writer writer) throws IOException{
    exportAsAnIML(spec, writer, 0, spec.getNumberOfPoints() -1);
  }

  /**
    * Exports the Spectrum that is displayed by JSVPanel to a file given by fileName
    * If display is zoomed then export the current view
    * @param spec the spectrum to export
    * @param fileName the name of the file
    * @param startIndex the starting point of the spectrum
    * @param endIndex the end point
    * @throws IOException
    */
   public static void exportAsAnIML(JDXSpectrum spec, String fileName, int startIndex, int endIndex) throws IOException{
     FileWriter writer;
     writer = new FileWriter(fileName);
     amlFile =fileName;
     exportAsAnIML(spec, writer, startIndex, endIndex);
   }

  /**
   * Export a Spectrum as AnIML with specified Coordinates
   * @param spec the spectrum to export
   * @param writer the Writer
   * @param startDataPointIndex the start index of the coordinates
   * @param endDataPointIndex the end index of the coordinates
   */
  public static void exportAsAnIML(JDXSpectrum spec, Writer writer, int startDataPointIndex, int endDataPointIndex) {

   boolean continuous=spec.isContinuous();

// no template ready for Peak Tables so exit
   if (!continuous)
      return;

  Template template = null;
  VelocityContext context = new VelocityContext();

  Coordinate[] xyCoords=spec.getXYCoords();

  int npoints=endDataPointIndex-startDataPointIndex+1;
    boolean increasing= spec.isIncreasing();
    String xdata_type="float32";
    String ydata_type="float32";
    String title = spec.getTitle();
    String xUnits= spec.getXUnits();
    String yUnits= spec.getYUnits();
    String datatype= spec.getDataType();
    String owner = spec.getOwner();
    String origin = spec.getOrigin();
    String spectypeInitials="";
    String longdate= spec.getLongDate();
    String date=spec.getDate();
    String time=spec.getTime();
    String vendor="";
    String model="";
    String resolution="";
    String pathlength=spec.getPathlength();
    String molform="";
    String bp="";
    String mp="";
    String CASrn="";
    String CASn="";
    String ObNucleus="";

    double ObFreq=spec.getObservedFreq();
    double deltaX= spec.getDeltaX();
    double amlFirstX= xyCoords[startDataPointIndex].getXVal();
    
    String SolvRef="";
    String SolvName="";
    String AnIMLtemplate="animl_tmp.vm";

    Calendar now = Calendar.getInstance();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSS ZZZZ");
    String currentTime =  formatter.format(now.getTime());

    HashMap specHead= spec.getHeaderTable();
    String amlHead=specHead.toString();

    for(Iterator iter = specHead.keySet().iterator(); iter.hasNext();){
      String label = (String)iter.next();
      String dataSet = (String)specHead.get(label);
      if (label.equals("##RESOLUTION") )
        resolution = dataSet;
      if (label.contains("##SPECTROMETER") )
        model=dataSet;
      if (label.equals("##$MANUFACTURER"))
        vendor=dataSet;
      if (label.equals("##MOLFORM"))
        molform=dataSet;
      if (label.equals("##CASREGISTRYNO"))
        CASrn=dataSet;
      if (label.equals("##CASNAME"))
        CASn=dataSet;
      if (label.equals("##MP"))
        mp=dataSet;
      if (label.equals("##BP"))
        bp=dataSet;
      if (label.equals("##.OBSERVENUCLEUS"))
        ObNucleus=dataSet;
      if (label.equals("##.SOLVENTNAME"))
        SolvName=dataSet;
      if (label.equals("##.SOLVENTREFERENCE"))    // should really try to parse info from SHIFTREFERENCE
        SolvRef=dataSet;
    }

    Coordinate coord;
    double xPt, yPt;

    try{
         Velocity.init();
    }
    catch(Exception e){
    }

    Vector<Coordinate> newXYCoords = new Vector<Coordinate>();
    for(int i = startDataPointIndex; i <= endDataPointIndex; i++){
      Coordinate pt;
      pt = xyCoords[i].copy();
      newXYCoords.addElement(pt);
    }

    context.put("file", amlFile);
    context.put("xdata_type",xdata_type);
    context.put("ydata_type",ydata_type);

    context.put("title", title);
    context.put("xyCoords", newXYCoords);
    context.put("amlHead",amlHead);
    context.put("xUnits", xUnits.toUpperCase());
    context.put("yUnits", yUnits.toUpperCase());


    if ( (longdate.equals("") ) || (date.equals("") ) )
         longdate=currentTime;
    if ( (date.length() == 8) && (date.charAt(0) < '5'))
         longdate= "20" + date + " " + time;
    if ( (date.length() == 8) && (date.charAt(0) > '5'))
         longdate = "19" + date + " " + time;

    if (datatype.contains("MASS"))
         spectypeInitials="MS";
    if (datatype.contains("INFRARED"))
         spectypeInitials="IR";
    if (datatype.contains("UV")||(datatype.contains("VIS")))
         spectypeInitials="UV";
    if (datatype.contains("NMR")) {
         amlFirstX= amlFirstX * ObFreq;  // NMR stored internally as ppm
         deltaX= deltaX * ObFreq;        // convert to Hz before exporting
         AnIMLtemplate="animl_nmr.vm";
         spectypeInitials = "NMR";
    }

    context.put("firstX",amlFirstX);
    context.put("npoints",Integer.valueOf(npoints));
    context.put("xencode","avs");
    context.put("yencode","ivs");

    context.put("specinits",spectypeInitials);
    context.put("deltaX",deltaX);
    context.put("owner",owner);
    context.put("origin",origin);
    context.put("timestamp",longdate);
    context.put("DataType",datatype);
    context.put("currenttime",currentTime);
    context.put("resolution",resolution);
    context.put("pathlength",pathlength);   //required for UV and IR
    context.put("molform",molform);
    context.put("CASrn",CASrn);
    context.put("CASn",CASn);
    context.put("mp",mp);
    context.put("bp",bp);
    context.put("ObFreq",ObFreq);
    context.put("ObNucleus",ObNucleus);
    context.put("SolvName",SolvName);
    context.put("SolvRef",SolvRef);

    if (vendor.equals(""))
       vendor="not available from JCAMP-DX file";
     if (model.equals(""))
        model="not available from JCAMP-DX file";

    context.put("vendor",vendor);
    context.put("model",model);


    // TODO Take out System.err.println's
    // Throw exception instead
    // set up a template type so that for
    // each SpecType a different template is filled in

    try
    {
      template = Velocity.getTemplate(AnIMLtemplate);
      template.merge( context, writer);
    }
    catch( ResourceNotFoundException rnfe )
    {
      // couldn't find the template
      System.err.println("couldn't find the template");
    }
    catch( ParseErrorException pee )
    {
      // syntax error : problem parsing the template
      System.err.println("syntax error : problem parsing the template");
      pee.printStackTrace();
    }
    catch( MethodInvocationException mie )
    {
      // something invoked in the template
      // threw an exception
      System.err.println("something invoked in the template threw an exception");
    }
    catch( Exception e )
    {
      System.err.println("exception!");
    }

    try{
      writer.flush();
      writer.close();
    }
    catch(IOException ioe){
    }
  }
}
