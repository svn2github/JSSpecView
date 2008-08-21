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

package jspecview.export;


import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import jspecview.common.Coordinate;
import jspecview.common.JDXSpectrum;
import jspecview.util.FileManager;
import jspecview.util.Logger;

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
class CMLExporter {

  private String cmlFile;

  /**
   * Exports the Spectrum that is displayed by JSVPanel to a file given by fileName
   * If display is zoomed then export the current view
   * @param spec the spectrum to export
   * @param fileName the name of the file
   * @param startIndex the starting point of the spectrum
   * @param endIndex the end point
   * @throws IOException
   */
  void exportAsCML(JDXSpectrum spec, String fileName, int startIndex,
                   int endIndex) throws IOException {
    String CMLtemplate = "cml_tmp.vm";
    cmlFile = fileName;
    FileWriter writer = new FileWriter(fileName);

    Coordinate[] xyCoords = spec.getXYCoords();

    double deltaX = spec.getDeltaX();
    double cmlFirstX = xyCoords[startIndex].getXVal();
    double cmlLastX = xyCoords[endIndex].getXVal();

    int npoints = endIndex - startIndex + 1;
    //boolean increasing= spec.isIncreasing();
    String title = spec.getTitle();
    String xUnits = spec.getXUnits();
    String yUnits = spec.getYUnits();
    String datatype = spec.getDataType();
    String owner = spec.getOwner();
    String origin = spec.getOrigin();
    String spectypeInitials = "";
    String longdate = spec.getLongDate();
    String date = spec.getDate();
    String time = spec.getTime();
    //String vendor="";
    String ident = "";
    String model = "unknown";
    String resolution = "";
    //String pathlength=spec.getPathlength();
    String molform = "";
    //String bp="";
    //String mp="";
    String CASrn = "";
    //String CASn="";
    String ObNucleus = "";
    double ObFreq = spec.getObservedFreq();
    //double xMult=1.0;
    //String SolvRef="";
    //String SolvName="";

    Calendar now = Calendar.getInstance();
    SimpleDateFormat formatter = new SimpleDateFormat(
        "yyyy/MM/dd HH:mm:ss.SSSS ZZZZ");
    String currentTime = formatter.format(now.getTime());

    HashMap<String, String> specHead = spec.getHeaderTable();
    //String amlHead=specHead.toString();

    for (Iterator<String> iter = specHead.keySet().iterator(); iter.hasNext();) {
      String label = (String) iter.next();
      String dataSet = (String) specHead.get(label);
      if (label.equals("##RESOLUTION"))
        resolution = dataSet;
      if (label.contains("##SPECTROMETER"))
        model = dataSet;
      if (label.equals("##$MANUFACTURER")) {
      }//vendor=dataSet;
      if (label.equals("##MOLFORM"))
        molform = dataSet;
      if (label.equals("##CASREGISTRYNO"))
        CASrn = dataSet;
      if (label.equals("##CASNAME")) {
      }//CASn=dataSet;
      if (label.equals("##MP")) {
      }//mp=dataSet;
      if (label.equals("##BP")) {
      }//bp=dataSet;
      if (label.equals("##.OBSERVENUCLEUS"))
        ObNucleus = dataSet;
      if (label.equals("##.SOLVENTNAME")) {
      }//SolvName=dataSet;
      if (label.equals("##.SOLVENTREFERENCE")) // should really try to parse info from SHIFTREFERENCE
      {
      }//SolvRef=dataSet;
    }

    if (datatype.contains("MASS"))
      spectypeInitials = "massSpectrum";
    if (datatype.contains("INFRARED")) {
      spectypeInitials = "infrared";
      //         if (xUnits.toLowerCase().contains("cm") )
      //            xUnits="cm-1";
    }
    if (datatype.contains("UV") || (datatype.contains("VIS")))
      spectypeInitials = "UV/VIS";
    if (datatype.contains("NMR")) {
      cmlFirstX = cmlFirstX * ObFreq; // NMR stored internally as ppm
      cmlLastX = cmlLastX * ObFreq;
      deltaX = deltaX * ObFreq; // convert to Hz before exporting
      CMLtemplate = "cml_nmr.vm";
      spectypeInitials = "NMR";
    }
    int IDlen = title.length();
    if (IDlen > 10)
      IDlen = 10;
    ident = spectypeInitials + "_" + title.substring(0, IDlen);

    Vector<Coordinate> newXYCoords = new Vector<Coordinate>();
    for (int i = startIndex; i <= endIndex; i++)
      newXYCoords.addElement(xyCoords[i]);

    // load template

    FileManager fm = new FileManager(null);
    VelocityContext context = new VelocityContext();
    String template = fm.getResourceString(this, CMLtemplate, true);
    if (template == null) {
      Logger.error(fm.getErrorMessage());
      return;
    }
    String errMsg = context.setTemplate(template);
    if (errMsg != null) {
      Logger.error(errMsg);
      return;
    }
    context.put("file", cmlFile);
    context.put("title", title);
    context.put("ident", ident);
    context.put("xyCoords", newXYCoords);
    if (xUnits.toLowerCase().equals("m/z"))
      xUnits = "moverz";
    if (xUnits.toLowerCase().equals("1/cm"))
      xUnits = "cm-1";
    if (xUnits.toLowerCase().equals("nanometers"))
      xUnits = "nm";
    context.put("xUnits", xUnits.toLowerCase());
    context.put("yUnits", yUnits.toLowerCase());

    if ((longdate.equals("")) || (date.equals("")))
      longdate = currentTime;
    if ((date.length() == 8) && (date.charAt(0) < '5'))
      longdate = "20" + date + " " + time;
    if ((date.length() == 8) && (date.charAt(0) > '5'))
      longdate = "19" + date + " " + time;

    context.put("firstX", new Double(cmlFirstX));
    context.put("lastX", new Double(cmlLastX));
    context.put("npoints", Integer.valueOf(npoints));
    context.put("continuous", Boolean.valueOf(spec.isContinuous()));

    context.put("specinits", spectypeInitials);
    //    context.put("deltaX",deltaX);
    context.put("owner", owner);
    context.put("origin", origin);
    //    context.put("timestamp",longdate);
    context.put("DataType", datatype);
    //    context.put("currenttime",currentTime);
    context.put("resolution", resolution);
    //    context.put("pathlength",pathlength);
    context.put("molform", molform);
    context.put("CASrn", CASrn);
    context.put("model", model);
    //    context.put("CASn",CASn);
    //    context.put("mp",mp);
    //    context.put("bp",bp);
    context.put("ObFreq", new Double(ObFreq));
    context.put("ObNucleus", ObNucleus);
    //    context.put("SolvName",SolvName);
    //    context.put("SolvRef",SolvRef);

    errMsg = context.merge(writer);
    if (errMsg != null) {
      Logger.error(errMsg);
      throw new IOException(errMsg);
    }
    try {
      writer.flush();
      writer.close();
    } catch (IOException ioe) {
    }
  }
}
