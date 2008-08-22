/* Copyright (c) 2007 The University of the West Indies
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

package jspecview.source;

//import java.util.Iterator;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.* ;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Vector;

import jspecview.common.Coordinate;
import jspecview.common.Graph;
import jspecview.common.JDXSource;
import jspecview.common.JDXSpectrum;

/**
 * Representation of a XML Source.
 * @author Craig Walters
 * @author Prof. Robert J. Lancashire
 */

abstract public class XMLSource extends JDXSource {
  
  public static XMLEventReader fer;

  public String tmpstr="START",attrList="", title="identifier not found", owner="UNKNOWN",origin = "UNKNOWN";
  public String tmpEnd="END", molForm="", techname="";
  private XMLEvent e;
  public int eventType=-1;
  public int npoints=-1, samplenum=-1;
  public double[] yaxisData;
  public double[] xaxisData;
  public String xaxisLabel = "",  xaxisUnit = "", xaxisType = "";
  public  String yaxisLabel = "",  yaxisUnit = "", yaxisType = "";
  public  String vendor = "na", modelType = "MODEL UNKNOWN", LongDate = "";
  public  String pathlength= "na", identifier="", plLabel="";
  public  String resolution="na", resLabel="", LocName="";
  public  String LocContact="", casName = "";
  public  String sampleowner="", obNucleus="", StrObFreq="";
  public  boolean increasing = false, continuous=false;
  public  int ivspoints, evspoints, sampleRefNum=0;
  public  double deltaX = Graph.ERROR;
  public  double xFactor = Graph.ERROR;
  public  double yFactor = Graph.ERROR;
  public  double firstX = Graph.ERROR;
  public  double lastX = Graph.ERROR;
  public  double firstY = Graph.ERROR;
  public  double obFreq= Graph.ERROR;
  public  double refPoint=Graph.ERROR;
  public String casRN = "";
  public String sampleID;
  public StringBuffer errorLog = new StringBuffer();
  protected XMLInputFactory factory;
  public String errorSeparator="________________________________________________________";

  
  protected void getFactory(InputStream in) throws XMLStreamException {
    if (factory == null)
      factory = XMLInputFactory.newInstance();
    XMLEventReader nofr = factory.createXMLEventReader(in);
    fer = factory.createFilteredReader(nofr, new EventFilter() {
      public boolean accept(XMLEvent event) {
        return ((event.isStartDocument()) || (event.isStartElement())
            || (event.isCharacters()) || (event.isEndElement()));
      }
    });
  }

  protected void populateVariables() {
    // end of import of CML document
    // now populate all the JSpecView spectrum variables.....

    HashMap<String, String> LDRTable = new HashMap<String, String>(20);
    JDXSpectrum spectrum = new JDXSpectrum();

    spectrum.setTitle(title);
    spectrum.setJcampdx("5.01");
    spectrum.setDataClass("XYDATA");
    spectrum.setDataType(techname);
    spectrum.setContinuous(continuous);
    spectrum.setIncreasing(increasing);
    spectrum.setXFactor(xFactor);
    spectrum.setYFactor(yFactor);
    spectrum.setLongDate(LongDate);
    spectrum.setOrigin(origin);
    spectrum.setOwner(owner);
    spectrum.setPathlength(pathlength);

    //  now fill in what we can of a HashMap with parameters from the CML file
    //  syntax is:
    //      LDRTable.put()
    //      Key kk = new Key;
    LDRTable.put("##PATHLENGTH", pathlength);
    LDRTable.put("##RESOLUTION", resolution);
    if (!StrObFreq.equals(""))
      LDRTable.put("##.OBSERVEFREQUENCY", StrObFreq);
    if (!obNucleus.equals(""))
      LDRTable.put("##.OBSERVENUCLEUS", obNucleus);
    LDRTable.put("##$MANUFACTURER", vendor);
    if (!casRN.equals(""))
      LDRTable.put("##CASREGISTRYNO", casRN);
    if (!molForm.equals(""))
      LDRTable.put("##MOLFORM", molForm);
    if (!modelType.equals(""))
      LDRTable.put("##SPECTROMETER/DATA SYSTEM", modelType);

    //etc etc.
    spectrum.setHeaderTable(LDRTable);

    double xScale = 1; // NMR data stored internally as ppm
    if (obFreq != Graph.ERROR) {
      spectrum.setObservedFreq(obFreq);
      if (xaxisUnit.toUpperCase().equals("HZ")) {
        xaxisUnit = "PPM";
        spectrum.setHZtoPPM(true);
        xScale = obFreq;
      }
    }

    Coordinate AMLpoint;
    Vector<Coordinate> xyCoords = new Vector<Coordinate>();

    //   for ease of plotting etc. all data is stored internally in increasing order
    for (int x = 0; x < npoints; x++) {
      AMLpoint = new Coordinate();
      AMLpoint.setXVal(xaxisData[x] / xScale);
      AMLpoint.setYVal(yaxisData[x]);
      if (increasing)
        xyCoords.addElement(AMLpoint);
      else
        xyCoords.insertElementAt(AMLpoint, 0);
    }

    spectrum.setXUnits(xaxisUnit);
    spectrum.setYUnits(yaxisUnit);

    Coordinate[] amlcoord = new Coordinate[npoints];
    xyCoords.toArray(amlcoord);
    spectrum.setXYCoords(amlcoord);    
    addJDXSpectrum(spectrum);
  }

  protected boolean checkPointCount() {
    //test to see if we have any contiuous data to plot
    //if not, then stop
    if (continuous && npoints < 5) {
      System.err.println("Insufficient points to plot");
      errorLog.append("Insufficient points to plot \n");
      setErrorLog(errorLog.toString());
      return false;
    }
    return true;
  }

  protected void nextTag() throws XMLStreamException {
    e = fer.nextTag();
  }
  
  protected int peek() throws XMLStreamException {
    e = fer.peek();
    return eventType = e.getEventType();    
  }
  
  protected int nextEvent() throws XMLStreamException {
    e = fer.nextEvent();
    return eventType = e.getEventType();    
  }
  
  protected void nextStartTag() throws XMLStreamException {
    e = fer.nextTag();
    while (!e.isStartElement())
      e = fer.nextTag();
  }

  protected String getTagName() {
    return e.asStartElement().getName().getLocalPart().toLowerCase().trim();
  }

  protected String getEndTag() {
    return e.asEndElement().getName().getLocalPart().toLowerCase().trim();
  }

  protected String getAttributeList() {
    return e.toString().toLowerCase();  
  }

  protected String nextValue() throws XMLStreamException {
    fer.nextTag();
    return fer.nextEvent().toString().trim();
  }

  protected String thisValue() throws XMLStreamException {
    return fer.nextEvent().toString().trim();
  }

  protected String getAttrValueLC(String key) {
    return getAttrValue(key).toLowerCase();
  }

  protected Attribute getAttr(String name) {
    return e.asStartElement().getAttributeByName(new QName(name));
  }
  protected String getAttrValue(String name) {
    Attribute a = getAttr(name);
    return (a == null ? "" : a.getValue());
  }

  protected String getFullAttribute(String name) {
    Attribute a = getAttr(name);
    return (a == null ? "" : a.toString().toLowerCase());
  }

  protected String getCharacters() throws XMLStreamException {
    StringBuffer sb = new StringBuffer();
    e = fer.peek();
    eventType = e.getEventType();

    while (eventType != XMLStreamConstants.CHARACTERS) {
      //                  System.out.println(e.toString()+ " "+ eventType);
      e = fer.nextEvent();
    }

    while (eventType == XMLStreamConstants.CHARACTERS) {
      e = fer.nextEvent();
      eventType = e.getEventType();
      if (eventType == XMLStreamConstants.CHARACTERS)
        sb.append(e.toString());
    }
    return sb.toString();
  }

  protected void processErrors(String type) {
    // for ease of processing later, return a source rather than a spectrum
    //    return XMLSource.getXMLInstance(spectrum);
    factory = null;
    if (errorLog.length() > 0) {
      errorLog.append("these errors were found in " + type + " \n");
    } else {
      errorLog.append("No Errors\n");
    }
    errorLog.append(errorSeparator);
    setErrorLog(errorLog.toString());
  }


}

