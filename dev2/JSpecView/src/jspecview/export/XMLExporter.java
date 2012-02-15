/* Copyright (c) 2002-2008 The University of the West Indies
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

import java.io.IOException;
import java.util.Map;
import java.util.Vector;

import jspecview.common.Coordinate;
import jspecview.common.JDXSpectrum;

/**
 * The XMLExporter should be a totally generic exporter
 *
 * no longer uses Velocity.
 *
 * @author Bob Hanson, hansonr@stolaf.edu
 *
 */
abstract class XMLExporter extends FormExporter {

  boolean continuous;
  String title;
  String ident;
  String state;
  String xUnits;
  String yUnits;
  String xUnitFactor = "";
  String xUnitExponent = "1";
  String xUnitLabel;
  String yUnitLabel;
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
  String casRN = "";
  String casName = "";
  String obNucleus = "";

  double obFreq;
  double firstX;
  double lastX;
  double deltaX;

  String solvRef = "";
  String solvName = "";

  int startIndex;
  int endIndex;
  Coordinate[] xyCoords;
  int npoints;

  Vector<Coordinate> newXYCoords = new Vector<Coordinate>();

  public boolean exportAsXML(JDXSpectrum spec, String fileName, int startIndex,
                             int endIndex) {
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    initForm(fileName);
    return setParameters(spec);
  }

  protected boolean setParameters(JDXSpectrum spec) {

    continuous = spec.isContinuous();

    // no template ready for Peak Tables so exit
    if (!continuous)
      return false;

    xyCoords = spec.getXYCoords();
    npoints = endIndex - startIndex + 1;
    for (int i = startIndex; i <= endIndex; i++)
      newXYCoords.addElement(xyCoords[i]);

    title = spec.getTitle();

    // QUESTION: OK to set units UpperCase for both AnIML and CML?

    xUnits = spec.getXUnits().toUpperCase();
    yUnits = spec.getYUnits().toUpperCase();

    if (xUnits.equals("1/CM")) {
      xUnitLabel = "1/cm";
      xUnitFactor = "0.01";
      xUnitExponent = "-1";
    } else if (xUnits.equals("UM") || xUnits.equals("MICROMETERS")) {
      xUnitLabel = "um";
      xUnitFactor = "0.000001";
    } else if (xUnits.equals("NM") || xUnits.equals("NANOMETERS")
        || xUnits.equals("WAVELENGTH")) {
      xUnitLabel = "nm";
      xUnitFactor = "0.000000001";
    } else if (xUnits.equals("PM") || xUnits.equals("PICOMETERS")) {
      xUnitLabel = "pm";
      xUnitFactor = "0.000000000001";
    } else {
      xUnitLabel = "Arb. Units";
      xUnitFactor = "";
    }

    yUnitLabel = (yUnits.equals("A") || yUnits.equals("ABS")
        || yUnits.equals("ABSORBANCE") || yUnits.equals("AU")
        || yUnits.equals("AUFS") || yUnits.equals("OPTICAL DENSITY") ? "Absorbance"
        : yUnits.equals("T") || yUnits.equals("TRANSMITTANCE") ? "Transmittance"
            : yUnits.equals("COUNTS") || yUnits.equals("CTS") ? "Counts"
                : "Arb. Units");

    owner = spec.getOwner();
    origin = spec.getOrigin();
    time = spec.getTime();

    longdate = spec.getLongDate();
    date = spec.getDate();
    if ((longdate.equals("")) || (date.equals("")))
      longdate = currentTime;
    if ((date.length() == 8) && (date.charAt(0) < '5'))
      longdate = "20" + date + " " + time;
    if ((date.length() == 8) && (date.charAt(0) > '5'))
      longdate = "19" + date + " " + time;

    //pathlength = spec.getPathlength(); // ignored
    obFreq = spec.getObservedFreq();
    firstX = xyCoords[startIndex].getXVal();
    lastX = xyCoords[endIndex].getXVal();
    deltaX = spec.getDeltaX();
    datatype = spec.getDataType();
    if (datatype.contains("NMR")) {
      firstX *= obFreq; // NMR stored internally as ppm
      lastX *= obFreq;
      deltaX *= obFreq; // convert to Hz before exporting
    }

    // these may come back null, but context.put() turns that into ""
    // still, one must check for == null in tests here.

    Map<String, String> specHead = spec.getHeaderTable();
    state = specHead.get("##STATE");
    resolution = specHead.get("##RESOLUTION");
    model = specHead.get("##SPECTROMETER");
    vendor = specHead.get("##$MANUFACTURER");
    molform = specHead.get("##MOLFORM");
    casRN = specHead.get("##CASREGISTRYNO");
    casName = specHead.get("##CASNAME");
    mp = specHead.get("##MP");
    bp = specHead.get("##BP");
    obNucleus = specHead.get("##.OBSERVENUCLEUS");
    solvName = specHead.get("##.SOLVENTNAME");
    solvRef = specHead.get("##.SOLVENTREFERENCE"); // should really try to parse info from SHIFTREFERENCE
    return true;
  }

  protected void setContext() {
    context.put("continuous", Boolean.valueOf(continuous));
    context.put("file", outputFile);
    context.put("title", title);
    context.put("ident", ident);
    context.put("state", state);
    context.put("firstX", new Double(firstX));
    context.put("lastX", new Double(lastX));
    context.put("xyCoords", newXYCoords);
    context.put("xdata_type", "Float32");
    context.put("ydata_type", "Float32");
    context.put("npoints", Integer.valueOf(npoints));
    context.put("xencode", "avs");
    context.put("yencode", "ivs");
    context.put("xUnits", xUnits);
    context.put("yUnits", yUnits);
    context.put("xUnitLabel", xUnitLabel);
    context.put("yUnitLabel", yUnitLabel);
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
    context.put("CASrn", casRN);
    context.put("CASn", casName);
    context.put("mp", mp);
    context.put("bp", bp);
    context.put("ObFreq", new Double(obFreq));
    context.put("ObNucleus", obNucleus);
    context.put("SolvName", solvName);
    context.put("SolvRef", solvRef);
    context.put("vendor", vendor);
    context.put("model", model);
  }

  String writeFormType(String type) throws IOException {
    return writeForm(type + (datatype.contains("NMR") ? "_nmr" : "_tmp") + ".vm");
  }

}
