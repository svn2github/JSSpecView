/* Copyright (c) 2006-2007 The University of the West Indies
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
import java.util.Calendar;
import java.util.Iterator;

/**
 * class <code>CMLExporter</code> contains static methods to export a Graph as
 * as CIML. <code>CMLExporter</code> uses <a href="http://jakarta.apache.org/velocity/">Velocity</a>
 * to write to a template file called 'cml_tmp.vm' or 'cml_nmr.vm'. So any changes in design should
 * be done in these files.
 * @see jspecview.common.Graph
 * @author Prof Robert J. Lancashire
 */
public class CMLExporter {

  private static String cmlFile;

  /**
   * Export full Spectrum as AnIML to a file given by fileName
   * @param spec the spectrum to export
   * @param fileName the name of the file
   * @throws IOException
   */

  public static void exportAsCML(JDXSpectrum spec, String fileName) throws IOException{
    FileWriter writer;
    writer = new FileWriter(fileName);
    cmlFile= fileName;

    exportAsCML(spec, writer,  0, spec.getNumberOfPoints() -1);
  }

  /**
   * Export full Spectrum as CML to a Writer given by writer
   * @param spec the spectrum to export
   * @param writer the Writer
   * @throws IOException
   */
  public static void exportAsCML(JDXSpectrum spec, Writer writer) throws IOException{
    exportAsCML(spec, writer, 0, spec.getNumberOfPoints() -1);
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
   public static void exportAsCML(JDXSpectrum spec, String fileName, int startIndex, int endIndex) throws IOException{
     FileWriter writer;
     writer = new FileWriter(fileName);
     cmlFile =fileName;
     exportAsCML(spec, writer, startIndex, endIndex);
   }

  /**
   * Export a Spectrum as CML with specified Coordinates
   * @param spec the spectrum to export
   * @param writer the Writer
   * @param startDataPointIndex the start index of the coordinates
   * @param endDataPointIndex the end index of the coordinates
   */
  public static void exportAsCML(JDXSpectrum spec, Writer writer, int startDataPointIndex, int endDataPointIndex) {

   boolean continuous=spec.isContinuous();
   Template template = null;
   VelocityContext context = new VelocityContext();

   Coordinate[] xyCoords=spec.getXYCoords();

    double deltaX= spec.getDeltaX();
    double cmlFirstX= xyCoords[startDataPointIndex].getXVal();
    double cmlLastX= xyCoords[endDataPointIndex].getXVal();

    int npoints=endDataPointIndex-startDataPointIndex+1;
    //boolean increasing= spec.isIncreasing();
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
    //String vendor="";
    String ident="";
    String model="unknown";
    String resolution="";
    //String pathlength=spec.getPathlength();
    String molform="";
    //String bp="";
    //String mp="";
    String CASrn="";
    //String CASn="";
    String ObNucleus="";
    double ObFreq=spec.getObservedFreq();
    //double xMult=1.0;
    //String SolvRef="";
    //String SolvName="";
    String CMLtemplate="cml_tmp.vm";

    Calendar now = Calendar.getInstance();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSS ZZZZ");
    String currentTime =  formatter.format(now.getTime());

    HashMap<String, String> specHead= spec.getHeaderTable();
    //String amlHead=specHead.toString();

    for(Iterator<String> iter = specHead.keySet().iterator(); iter.hasNext();){
      String label = (String)iter.next();
      String dataSet = (String)specHead.get(label);
      if (label.equals("##RESOLUTION") )
        resolution = dataSet;
      if (label.contains("##SPECTROMETER") )
        model=dataSet;
      if (label.equals("##$MANUFACTURER"))
        {}//vendor=dataSet;
      if (label.equals("##MOLFORM"))
        molform=dataSet;
      if (label.equals("##CASREGISTRYNO"))
        CASrn=dataSet;
      if (label.equals("##CASNAME"))
        {}//CASn=dataSet;
      if (label.equals("##MP"))
        {}//mp=dataSet;
      if (label.equals("##BP"))
        {}//bp=dataSet;
      if (label.equals("##.OBSERVENUCLEUS"))
        ObNucleus=dataSet;
      if (label.equals("##.SOLVENTNAME"))
        {}//SolvName=dataSet;
      if (label.equals("##.SOLVENTREFERENCE"))    // should really try to parse info from SHIFTREFERENCE
        {}//SolvRef=dataSet;
    }

    try{
         Velocity.init();
    }
    catch(Exception e){
    }

    Vector<Coordinate> newXYCoords = new Vector<Coordinate>();

    if (datatype.contains("MASS"))
         spectypeInitials="massSpectrum";
    if (datatype.contains("INFRARED")) {
      spectypeInitials = "infrared";
//         if (xUnits.toLowerCase().contains("cm") )
//            xUnits="cm-1";
    }
    if (datatype.contains("UV")||(datatype.contains("VIS")))
         spectypeInitials="UV/VIS";
    if (datatype.contains("NMR")) {
       cmlFirstX= cmlFirstX * ObFreq;  // NMR stored internally as ppm
       cmlLastX= cmlLastX * ObFreq;
       deltaX= deltaX * ObFreq;        // convert to Hz before exporting
       CMLtemplate="cml_nmr.vm";
       spectypeInitials = "NMR";
     }
     int IDlen=title.length();
     if (IDlen > 10)
         IDlen=10;
     ident=spectypeInitials+"_"+title.substring(0,IDlen);

    for(int i = startDataPointIndex; i <= endDataPointIndex; i++){
      Coordinate pt;
      pt = xyCoords[i].copy();
      newXYCoords.addElement(pt);
    }

    context.put("file", cmlFile);
    context.put("title", title);
    context.put("ident",ident);
    context.put("xyCoords", newXYCoords);
    if (xUnits.toLowerCase().equals("m/z"))
        xUnits="moverz";
    if (xUnits.toLowerCase().equals("1/cm"))
        xUnits="cm-1";
    if (xUnits.toLowerCase().equals("nanometers"))
          xUnits="nm";
    context.put("xUnits", xUnits.toLowerCase());
    context.put("yUnits", yUnits.toLowerCase());

    if ( (longdate.equals("") ) || (date.equals("") ) )
         longdate=currentTime;
    if ( (date.length() == 8) && (date.charAt(0) < '5'))
         longdate= "20" + date + " " + time;
    if ( (date.length() == 8) && (date.charAt(0) > '5'))
         longdate = "19" + date + " " + time;

    context.put("firstX",new Double(cmlFirstX));
    context.put("lastX",new Double(cmlLastX));
    context.put("npoints",Integer.valueOf(npoints));
    context.put("continuous",Boolean.valueOf(continuous));

    context.put("specinits",spectypeInitials);
//    context.put("deltaX",deltaX);
    context.put("owner",owner);
    context.put("origin",origin);
//    context.put("timestamp",longdate);
    context.put("DataType",datatype);
//    context.put("currenttime",currentTime);
    context.put("resolution",resolution);
//    context.put("pathlength",pathlength);
    context.put("molform",molform);
    context.put("CASrn",CASrn);
    context.put("model",model);
//    context.put("CASn",CASn);
//    context.put("mp",mp);
//    context.put("bp",bp);
    context.put("ObFreq",new Double(ObFreq));
    context.put("ObNucleus",ObNucleus);
//    context.put("SolvName",SolvName);
//    context.put("SolvRef",SolvRef);


    // TODO Take out System.err.println's
    // Throw exception instead
    // set up a template type so that for
    // each SpecType a different template is filled in

    try
    {
      template = Velocity.getTemplate(CMLtemplate);
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
