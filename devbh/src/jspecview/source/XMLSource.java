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
//import javax.xml.namespace.QName;
//import javax.xml.stream.*;
//import javax.xml.stream.events.* ;

import java.io.IOException;
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

abstract class XMLSource extends JDXSource {

  //  protected XMLInputFactory factory;
  //  private XMLEventReader fer;
  //  private XMLEvent e;

  private SimpleXmlReader.Buffer fer;
  private SimpleXmlReaderXmlEvent e;

  protected String tagName = "START", attrList = "",
      title = "identifier not found", owner = "UNKNOWN", origin = "UNKNOWN";
  protected String tmpEnd = "END", molForm = "", techname = "";
  protected int eventType = -1;
  protected int npoints = -1, samplenum = -1;
  protected double[] yaxisData;
  protected double[] xaxisData;
  protected String xaxisLabel = "", xaxisUnit = "", xaxisType = "";
  protected String yaxisLabel = "", yaxisUnit = "", yaxisType = "";
  protected String vendor = "na", modelType = "MODEL UNKNOWN", LongDate = "";
  protected String pathlength = "na", identifier = "", plLabel = "";
  protected String resolution = "na", resLabel = "", LocName = "";
  protected String LocContact = "", casName = "";
  protected String sampleowner = "", obNucleus = "", StrObFreq = "";
  protected boolean increasing = false, continuous = false;
  protected int ivspoints, evspoints, sampleRefNum = 0;
  protected double deltaX = Graph.ERROR;
  protected double xFactor = Graph.ERROR;
  protected double yFactor = Graph.ERROR;
  protected double firstX = Graph.ERROR;
  protected double lastX = Graph.ERROR;
  protected double firstY = Graph.ERROR;
  protected double obFreq = Graph.ERROR;
  protected double refPoint = Graph.ERROR;
  protected String casRN = "";
  protected String sampleID;
  protected StringBuffer errorLog = new StringBuffer();
  protected String errorSeparator = "________________________________________________________";

  /*  
   protected void getFactory(InputStream in) throws XMLStreamException {
   factory = XMLInputFactory.newInstance();
   XMLEventReader nofr = factory.createXMLEventReader(in);
   fer = factory.createFilteredReader(nofr, new EventFilter() {
   public boolean accept(XMLEvent event) {
   return ((event.isStartDocument()) || (event.isStartElement())
   || (event.isCharacters()) || (event.isEndElement()));
   }
   });
   }
   */
  protected void getSimpleXmlReader(InputStream in) {
    SimpleXmlReader reader = new SimpleXmlReader();
    fer = reader.getBuffer(in);
  }

  protected String getBufferData() {
    return (fer == null ? null : fer.data.substring(0, fer.ptr));
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

  protected int peek() throws IOException {
    e = fer.peek();
    return eventType = e.getEventType();
  }

  protected boolean haveMore() {
    return fer.hasNext();
  }

  protected void nextTag() throws IOException {
    e = fer.nextTag();
  }

  protected int nextEvent() throws IOException {
    e = fer.nextEvent();
    return eventType = e.getEventType();
  }

  protected void nextStartTag() throws IOException {
    e = fer.nextTag();
    while (!e.isStartElement())
      e = fer.nextTag();
  }

  protected String getTagName() {
    return e.getTagName();
  }

  protected String getEndTag() {
    return e.getTagName();
  }

  protected String getAttributeList() {
    return e.toString().toLowerCase();
  }

  protected String thisValue() throws IOException {
    return fer.nextEvent().toString().trim();
  }

  protected String nextValue() throws IOException {
    fer.nextTag();
    return fer.nextEvent().toString().trim();
  }

  protected String getAttrValueLC(String key) {
    return getAttrValue(key).toLowerCase();
  }

  protected String getAttrValue(String name) {
    SimpleXmlReaderAttribute a = e.getAttributeByName(name);
    return (a == null ? "" : a.getValue());
  }

  protected String getFullAttribute(String name) {
    SimpleXmlReaderAttribute a = e.getAttributeByName(name);
    return (a == null ? "" : a.toString().toLowerCase());
  }

  protected String getCharacters() throws IOException {
    StringBuffer sb = new StringBuffer();
    e = fer.peek();
    eventType = e.getEventType();

    while (eventType != SimpleXmlReader.CHARACTERS) {
      e = fer.nextEvent();
      eventType = e.getEventType();
    }

    while (eventType == SimpleXmlReader.CHARACTERS) {
      e = fer.nextEvent();
      eventType = e.getEventType();
      if (eventType == SimpleXmlReader.CHARACTERS)
        sb.append(e.toString());
    }
    return sb.toString();
  }

  /*  protected int peek() throws XMLStreamException {
   e = fer.peek();
   return eventType = e.getEventType();    
   }
   
   protected boolean haveMore() {
   return fer.hasNext();
   }
   
   protected void nextTag() throws XMLStreamException {
   e = fer.nextTag();
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

   protected String thisValue() throws XMLStreamException {
   return fer.nextEvent().toString().trim();
   }

   protected String nextValue() throws XMLStreamException {
   fer.nextTag();
   return fer.nextEvent().toString().trim();
   }

   protected String getAttrValueLC(String key) {
   return getAttrValue(key).toLowerCase();
   }

   protected SimpleXmlReaderAttribute getAttr(String name) {
   return e.asStartElement().getAttributeByName(new QName(name));
   }
   protected String getAttrValue(String name) {
   SimpleXmlReaderAttribute a = getAttr(name);
   return (a == null ? "" : a.getValue());
   }

   protected String getFullAttribute(String name) {
   SimpleXmlReaderAttribute a = getAttr(name);
   return (a == null ? "" : a.toString().toLowerCase());
   }

   protected String getCharacters() throws XMLStreamException {
   StringBuffer sb = new StringBuffer();
   e = fer.peek();
   eventType = e.getEventType();

   while (eventType != XMLStreamConstants.CHARACTERS) { 
   e = fer.nextEvent();
   eventType = e.getEventType();     
   }

   while (eventType == XMLStreamConstants.CHARACTERS) {
   e = fer.nextEvent();
   eventType = e.getEventType();
   if (eventType == XMLStreamConstants.CHARACTERS)
   sb.append(e.toString());
   }
   return sb.toString();
   }
   */
  protected void processErrors(String type) {
    // for ease of processing later, return a source rather than a spectrum
    //    return XMLSource.getXMLInstance(spectrum);
    //factory = null;
    fer = null;
    if (errorLog.length() > 0) {
      errorLog.append("these errors were found in " + type + " \n");
    } else {
      errorLog.append("No Errors\n");
    }
    errorLog.append(errorSeparator);
    setErrorLog(errorLog.toString());
  }

  
  final static String[] tagNames = {
    // aml:
    "audittrail", 
    "author",
    "experimentstepset", 
    "sampleset",
    // cml:  
    "spectrum",
    "metadatalist",
    "conditionlist",
    "parameterlist",
    "sample",
    "spectrumdata",
    "peaklist",
    // cml, but not here:
    "peaklist"
  };

  final static int AML_0 = 0;
  final static int AML_AUDITTRAIL = 0;
  final static int AML_AUTHOR = 1;
  final static int AML_EXPERIMENTSTEPSET = 2;
  final static int AML_SAMPLESET = 3;
  final static int AML_1 = 4;
  
  final static int CML_0 = 4;
  final static int CML_SPECTRUM = 4;
  final static int CML_METADATALIST = 5;
  final static int CML_CONDITIONLIST = 6;
  final static int CML_PARAMETERLIST = 7;
  final static int CML_SAMPLE = 8;
  final static int CML_SPECTRUMDATA = 9;
  final static int CML_PEAKLIST = 10;
  final static int CML_1 = 11;

  final static int CML_PEAKLIST2 = 11;

  protected void processXML(int i0, int i1) throws Exception {
    while (haveMore()) {
      if (nextEvent() != SimpleXmlReader.START_ELEMENT) 
        continue;
        String theTag = getTagName();
        //System.out.println(theTag);
        for (int i = i0; i < i1; i++)
          if (theTag.equals(tagNames[i])) {
            process(i);
            break;
          }
      }
  }

  protected void process(int tagId) {
    // overridden
  }

}
