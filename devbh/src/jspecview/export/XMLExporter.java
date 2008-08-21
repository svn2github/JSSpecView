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

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import jspecview.common.Coordinate;
import jspecview.common.JDXSpectrum;
import jspecview.util.FileManager;
import jspecview.util.Logger;

/**
 * The XMLExporter should be a totally generic exporter
 * 
 * AnIML and CML should be identical except for the templates. 
 * 
 * I just don't have time to finish this now.  - Bob Hanson
 *
 */
abstract class XMLExporter {

  Vector<Coordinate> newXYCoords = new Vector<Coordinate>();
  String template;
  VelocityContext context = new VelocityContext();
  String errMsg;
  String theFile;
  FileWriter writer;
  int startIndex;
  int endIndex;
  
  protected void setWriter(String fileName) throws IOException {
    writer = new FileWriter(fileName);
    theFile = fileName;
  }

  Calendar now;
  SimpleDateFormat formatter;
  String currentTime;

  Coordinate[] xyCoords;

  int npoints;

  String title;
  String xUnits;
  String yUnits;

  String unitLabel;
  String datatype;
  String owner;
  String origin;
  String spectypeInitials = "";
  String longdate;
  String date;
  String time;
  String vendor = "";
  String model = "";
  String resolution = "";
  String pathlength;
  String molform = "";
  String bp = "";
  String mp = "";
  String CASrn = "";
  String CASn = "";
  String ObNucleus = "";

  double ObFreq;
  double deltaX;

  String SolvRef = "";
  String SolvName = "";

  HashMap<String, String> specHead;
  String head;

  protected boolean setParameters(JDXSpectrum spec) {
    
    boolean continuous = spec.isContinuous();

    // no template ready for Peak Tables so exit
    if (!continuous)
      return false;


    Calendar now = Calendar.getInstance();
    SimpleDateFormat formatter = new SimpleDateFormat(
        "yyyy/MM/dd HH:mm:ss.SSSS ZZZZ");
    currentTime = formatter.format(now.getTime());
    
    
    xyCoords = spec.getXYCoords();
    npoints = endIndex - startIndex + 1;
    for (int i = startIndex; i <= endIndex; i++)
      newXYCoords.addElement(xyCoords[i]);
    
    title = spec.getTitle();
    xUnits = spec.getXUnits().toUpperCase();
    yUnits = spec.getYUnits().toUpperCase();

    unitLabel = (yUnits.equals("A") || yUnits.equals("ABS")
        || yUnits.equals("ABSORBANCE") || yUnits.equals("AU")
        || yUnits.equals("AUFS") || yUnits.equals("OPTICAL DENSITY") ? "Absorbance"
        : yUnits.equals("T") || yUnits.equals("TRANSMITTANCE") ? "Transmittance"
            : yUnits.equals("COUNTS") || yUnits.equals("CTS") ? "Counts"
                : "Arb. Units");

    
    datatype = spec.getDataType();
    owner = spec.getOwner();
    origin = spec.getOrigin();
    spectypeInitials = "";
    longdate = spec.getLongDate();
    date = spec.getDate();
    time = spec.getTime();

    if ((longdate.equals("")) || (date.equals("")))
      longdate = currentTime;
    if ((date.length() == 8) && (date.charAt(0) < '5'))
      longdate = "20" + date + " " + time;
    if ((date.length() == 8) && (date.charAt(0) > '5'))
      longdate = "19" + date + " " + time;

    vendor = "";
    model = "";
    resolution = "";
    pathlength = spec.getPathlength();
    molform = "";
    bp = "";
    mp = "";
    CASrn = "";
    CASn = "";
    ObNucleus = "";

    SolvRef = "";
    SolvName = "";

    HashMap<String, String> specHead = spec.getHeaderTable();
    head = specHead.toString();
    ObFreq = spec.getObservedFreq();
    deltaX = spec.getDeltaX();

    for (Iterator<String> iter = specHead.keySet().iterator(); iter.hasNext();) {
      String label = (String) iter.next();
      String dataSet = (String) specHead.get(label);
      if (label.equals("##RESOLUTION"))
        resolution = dataSet;
      if (label.contains("##SPECTROMETER"))
        model = dataSet;
      if (label.equals("##$MANUFACTURER"))
        vendor = dataSet;
      if (label.equals("##MOLFORM"))
        molform = dataSet;
      if (label.equals("##CASREGISTRYNO"))
        CASrn = dataSet;
      if (label.equals("##CASNAME"))
        CASn = dataSet;
      if (label.equals("##MP"))
        mp = dataSet;
      if (label.equals("##BP"))
        bp = dataSet;
      if (label.equals("##.OBSERVENUCLEUS"))
        ObNucleus = dataSet;
      if (label.equals("##.SOLVENTNAME"))
        SolvName = dataSet;
      if (label.equals("##.SOLVENTREFERENCE")) // should really try to parse info from SHIFTREFERENCE
        SolvRef = dataSet;
    }

    
    
    return true;
  }
  
  
  
  protected void setTemplate(String templateFile) {
    FileManager fm = new FileManager(null);
    template = fm.getResourceString(this, templateFile, true);
    if (template == null) {
      Logger.error(fm.getErrorMessage());
      return;
    }
    errMsg = context.setTemplate(template);
    if (errMsg != null) {
      Logger.error(errMsg);
      return;
    }
    
    context.put("file", theFile);

    context.put("title", title);
    context.put("xyCoords", newXYCoords);
    context.put("xUnits", xUnits);
    context.put("yUnits", yUnits);
    context.put("unitLabel", unitLabel);

    context.put("npoints", Integer.valueOf(npoints));
    context.put("xencode", "avs");
    context.put("yencode", "ivs");

    context.put("specinits", spectypeInitials);
    context.put("deltaX", new Double(deltaX));
    context.put("owner", owner);
    context.put("origin", origin);
    context.put("timestamp", longdate);
    context.put("DataType", datatype);
    context.put("currenttime", currentTime);
    context.put("resolution", resolution);
    context.put("pathlength", pathlength); //required for UV and IR
    context.put("molform", molform);
    context.put("CASrn", CASrn);
    context.put("CASn", CASn);
    context.put("mp", mp);
    context.put("bp", bp);
    context.put("ObFreq", new Double(ObFreq));
    context.put("ObNucleus", ObNucleus);
    context.put("SolvName", SolvName);
    context.put("SolvRef", SolvRef);


  }

  protected void writeXML() throws IOException {
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
