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

package jspecview.common;

import java.io.*;
import java.nio.*;
import javax.xml.stream.*;
import javax.xml.stream.events.* ;
import javax.xml.namespace.QName;
import java.util.StringTokenizer;

import jspecview.common.Graph;
import jspecview.common.JDXSpectrum;
import jspecview.common.JDXSource;
import jspecview.exception.JSpecViewException;
import jspecview.util.Coordinate;
import java.util.HashMap;
import java.util.Vector;

/**
 * Representation of a XML Source.
 * @author Craig Walters
 * @author Prof. Robert J. Lancashire
 */

public class CMLSource extends JDXSource {
  public static boolean specfound = false, increasing = false, continuous = false;
  public static int ivspoints, evspoints;
  public static int eventType = -1;
  public static int npoints   = -1;
  public static double deltaX  =Graph.ERROR;
  public static double xFactor = 1.0;
  public static double yFactor = 1.0;
  public static double firstX  = Graph.ERROR;
  public static double lastX   = Graph.ERROR;
  public static double firstY  = Graph.ERROR;
  public static double obFreq  = Graph.ERROR;
  public static double refPoint= Graph.ERROR;
  public static String tmpstr="begin", title="", owner="", origin = "";
  public static String tmpEnd="finish", MolForm="", techname="", tempstr="";
  public static StartElement se = null;
  public static String xaxisLabel = ""  , xaxisUnit = "", xaxisType = "";
  public static String yaxisLabel = ""  , yaxisUnit = "ARBITRARY UNITS", yaxisType = "";
  public static String vendor     = "na", modelType = "na", LongDate  = "";
  public static String pathlength = "na", identifier= "", plLabel   = "";
  public static String resolution = "na", resLabel  = "", LocName   = "";
  public static String LocContact = ""  , CASNo     = "";
  public static String sampleowner= ""  , obNucleus = "", StrObFreq = "";
  public static double[] yaxisData;
  public static double[] xaxisData;
  public static XMLEvent e;
  public static XMLEventReader fer;

  /**
   * Constructs a new CMLSource from CML document
   * @throws JSpecViewException
   */
  protected CMLSource() throws JSpecViewException{
    super();
  }

  /**
   * Does the actual work of initializing the CMLSource
   * @param in an InputStream of the CML document
   * @return an instance of a CMLSource
   * @throws JSpecViewException
   */
  public static CMLSource getCMLInstance(InputStream in) throws JSpecViewException {
       // The CMLSource Instance
    StringBuffer errorLog = new StringBuffer();
    CMLSource xs = new CMLSource();
    HashMap<String,String> LDRTable = new HashMap<String,String>(20);
    Coordinate AMLpoint;
    Vector<Coordinate> xyCoords = new Vector<Coordinate>();
    JDXSpectrum spectrum = new JDXSpectrum();
    String tempstr = "";

    try {
//   First off, get the factory instance.
     XMLInputFactory factory = XMLInputFactory.newInstance();
//   System.out.println("Factory= "+factory);
     XMLEventReader nofr=factory.createXMLEventReader(in);
     fer= factory.createFilteredReader(nofr, new EventFilter() {
         public boolean accept (XMLEvent event) {
         return ((event.isStartDocument()) || (event.isStartElement()) || (event.isEndElement()) ||(event.isCharacters()));
       }
     });

      e= fer.peek();
//      System.out.println(e.toString());
      if (e.getEventType()!= XMLStreamConstants.START_DOCUMENT) {
        System.err.println("Error, not an XML document?");
        throw new IOException();
      }

         e=fer.nextEvent();
//check if document is empty
        try {
            e= fer.peek();
        }catch (Exception e){
            System.err.println("Error, empty document?");
            return null;
        }
//find beginning of <spectrum>
         do {
                e= fer.nextEvent();
//                System.out.println(e.getEventType());
                eventType = e.getEventType();
//                System.out.println(e.toString());
                if (eventType==XMLStreamConstants.START_ELEMENT) {
                  tmpstr = e.asStartElement().getName().getLocalPart().trim();
                  tempstr= e.toString();
                }
              } while ((fer.hasNext()) && (!tmpstr.toLowerCase().equals("spectrum")) );

// found spectrum tag (?) so can now read info
              se = e.asStartElement();

// id is a required value so it should be there, and should not need to check if present,
// but will check anyway.

              if (tempstr.toLowerCase().contains("id")) {
                 Attribute specID = se.getAttributeByName(new QName("id"));
                 title = specID.getValue();
              }
// title seems optional so check if it is present,
// if not found, then set the JCAMP-DX title to ID
              if (tempstr.toLowerCase().contains("title")) {
                 Attribute tempName = se.getAttributeByName(new QName("title"));
                 title = tempName.getValue();
              }
// type is required so it should be present as well and no need to check if present
// check anyway, to be safe.

              if (tempstr.toLowerCase().contains("type")) {
                 Attribute techType = se.getAttributeByName(new QName("type"));
                 techname = techType.getValue().toString().toUpperCase() + " SPECTRUM";
              }
// now start to process the remainder of the document
         do {
              e = fer.nextEvent();
              eventType = e.getEventType();
              if (eventType==XMLStreamConstants.START_ELEMENT) {
                  tmpstr = e.asStartElement().getName().getLocalPart().trim();
                  tempstr= e.toString();
                }
              if (tmpstr.toLowerCase().equals("metadatalist")){
                processMetadata();
              }else if (tmpstr.toLowerCase().equals("conditionlist")){
                processConditions();
              }else if (tmpstr.toLowerCase().equals("parameterlist")){
                processParameters();
              }else if (tmpstr.toLowerCase().equals("sample")){
                processSample();
              }else if (tmpstr.toLowerCase().equals("spectrumdata")){
                processSpectrum();
         // if a spectrum is found ignore a peaklist if present as well
         // since without intervention it is not possible to guess
         // which display is required and the spectrum is probably the
         // more critical ?!?
              }else if ((tmpstr.toLowerCase().equals("peaklist"))&&(!specfound)){
                processPeaks();
              }

           } while (fer.hasNext());

//test to see if we have any contiuous data to plot
//if not then stop
        if (continuous && npoints < 5 )
          return null;

// end of import of CML document
// now populate all the JSpecView spectrum variables.....

      spectrum.setTitle(title);
      spectrum.setJcampdx("5.01");
      spectrum.setDataClass("XYDATA");
      spectrum.setDataType(techname);
      spectrum.setContinuous(continuous);
 //     spectrum.setzoomEnabled(true);
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
      LDRTable.put("##RESOLUTION",resolution);
      if (!StrObFreq.equals(""))
      LDRTable.put("##.OBSERVEFREQUENCY",StrObFreq);
      if (!obNucleus.equals(""))
       LDRTable.put("##.OBSERVENUCLEUS",obNucleus);
       LDRTable.put("##$MANUFACTURER",vendor);
      if (!CASNo.equals("") )
        LDRTable.put("##CASREGISTRYNO",CASNo);
      if (!MolForm.equals(""))
        LDRTable.put("##MOLFORM",MolForm);
      if (!modelType.equals("") )
        LDRTable.put("##SPECTROMETER/DATA SYSTEM",modelType);

//etc etc.
      spectrum.setHeaderTable(LDRTable);

      double xScale=1;   // NMR data stored internally as ppm
      if(obFreq != Graph.ERROR) {
        spectrum.setObservedFreq(obFreq);
        if (xaxisUnit.toUpperCase().equals("HZ")) {
          xaxisUnit="PPM";
          spectrum.setHZtoPPM(true);
          xScale = obFreq;
        }
      }

//   for ease of plotting etc. all data is stored internally in increasing order
      for(int x=0; x<npoints; x++)  {
        AMLpoint = new Coordinate();
        AMLpoint.setXVal(xaxisData[x]/xScale);
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
    }

    catch (Exception pe) {
      System.err.println("Error: " + pe.getMessage());
      pe.printStackTrace();
      System.exit( -1);
    }
// if you reached this far then hopefully no errors?    
    errorLog.append("No Errors");
    xs.setErrorLog(errorLog.toString());
    
    xs.addJDXSpectrum(spectrum);
    
    return xs;
// for ease of processing later, return a source rather than a spectrum
//    return XMLSource.getXMLInstance(spectrum);
  }

/**
 * Process the metadata CML events
 *@throws Exception
 */
public static void processMetadata() throws Exception {
 try{
   while ((fer.hasNext()) && (!tmpEnd.toLowerCase().equals("metadatalist")) ){
     e = fer.nextEvent();
     eventType = e.getEventType();

     if (eventType==XMLStreamConstants.END_ELEMENT)
       tmpEnd = e.asEndElement().getName().getLocalPart().trim();

     if (eventType == XMLStreamConstants.START_ELEMENT) {
      se = e.asStartElement();
      tmpstr = se.getName().getLocalPart().trim();
      String tempstr=se.toString();
      if (tmpstr.toLowerCase().equals("metadata")) {
        Attribute tempName = se.getAttributeByName(new QName("name"));
        if (tempName.getValue().toLowerCase().contains(":origin")) {
             if (tempstr.toLowerCase().contains("content")) {
               tempName = se.getAttributeByName(new QName("content"));
               origin = tempName.getValue();
             } else {
               e = fer.nextEvent();
               origin= e.toString().trim();
             }
//          System.out.println("origin= " + origin.trim());
        }
       else if (tempName.getValue().toLowerCase().contains(":owner")) {
         if (tempstr.toLowerCase().contains("content")) {
           tempName = se.getAttributeByName(new QName("content"));
           owner = tempName.getValue();
         } else {
           e = fer.nextEvent();
           owner= e.toString().trim();
         }
//          System.out.println("owner= " + owner.trim());
        }
      }
    }  // end if startelement
   } // end while
 } catch (Exception ex){
   System.err.println("error reading metadataList section");
   }
 } // end of processMetadata

/**
  * Process the parameter CML events
   *@throws Exception
  */
public static void processParameters() throws Exception {
  try{
    while ((fer.hasNext()) && (!tmpEnd.toLowerCase().equals("parameterlist")) ){
      e = fer.nextEvent();
      eventType = e.getEventType();

      if (eventType==XMLStreamConstants.END_ELEMENT)
        tmpEnd = e.asEndElement().getName().getLocalPart().trim();

      if (eventType == XMLStreamConstants.START_ELEMENT) {
        se = e.asStartElement();
        tmpstr = se.getName().getLocalPart().trim();
        String tempstr = se.toString();

        if (tmpstr.toLowerCase().equals("parameter")) {
          Attribute tempName = se.getAttributeByName(new QName("title"));

          if (tempName.getValue().toLowerCase().equals("nmr.observe frequency")) {
      //      tempName = se.getAttributeByName(new QName("content"));
              e = fer.nextTag();
              e = fer.nextEvent();
              StrObFreq= e.toString().trim();
              obFreq = Double.parseDouble(StrObFreq);
      //      System.out.println("obFreq= " + obFreq);
          }
          else if (tempName.getValue().toLowerCase().equals("nmr.observe nucleus")) {
//          tempName = se.getAttributeByName(new QName("content"));
            e = fer.nextEvent();
            obNucleus=e.toString().trim();
//          System.out.println("obNucleus= " + obNucleus.trim());
          }
          else if (tempName.getValue().toLowerCase().equals("spectrometer/data system")) {
             e = fer.nextEvent();
            modelType = e.toString().trim();
 //           System.out.println("modelType= " + modelType);
         }
         else if (tempName.getValue().toLowerCase().equals("resolution")) {
    //      tempName = se.getAttributeByName(new QName("content"));
            e = fer.nextTag();
            e = fer.nextEvent();
            resolution = e.toString().trim();
 //         System.out.println("Resolution= " + resolution);
           }

        }
      }
    } // end while
  } catch (Exception ex){
    System.err.println("error reading parameterList section");
    }
  } // end of processParameters

/**
  * Process the ConditionList CML events (found in NMRShiftDB)
   *@throws Exception
  */
public static void processConditions() throws Exception {
  try{
    while ((fer.hasNext()) && (!tmpEnd.toLowerCase().equals("conditionlist")) ){
      e = fer.nextEvent();
      eventType = e.getEventType();

      if (eventType==XMLStreamConstants.END_ELEMENT)
        tmpEnd = e.asEndElement().getName().getLocalPart().trim();

      if (eventType == XMLStreamConstants.START_ELEMENT) {
        se = e.asStartElement();
        tmpstr = se.getName().getLocalPart().trim();
        String tempstr = se.toString();

        if (tmpstr.toLowerCase().equals("scalar")) {
          Attribute tempName = se.getAttributeByName(new QName("dictRef"));

          if (tempName.getValue().toLowerCase().contains(":field")) {
              e = fer.nextEvent();
              StrObFreq= e.toString().trim();
//              System.out.println(StrObFreq);

              if ( ((int) StrObFreq.charAt(0) > 47) && ( (int) StrObFreq.charAt(0) < 58) )
                  obFreq = Double.parseDouble(StrObFreq);
//               else
//                   throw new JSpecViewException("Error reading ObFreq");

          }
        }
      }
    } // end while
  } catch (Exception ex){
    System.err.println("error reading conditionList section");
    }
  } // end of processConditions

/**
 * Process the sample CML events
 *@throws Exception
 */
public static void processSample() throws Exception {
  try {
     while ((fer.hasNext()) && (!tmpEnd.toLowerCase().equals("sample")) ){
       e = fer.nextEvent();
       eventType = e.getEventType();
       if (eventType == XMLStreamConstants.END_ELEMENT)
         tmpEnd = e.asEndElement().getName().getLocalPart().trim();

       if (eventType == XMLStreamConstants.START_ELEMENT) {
         se = e.asStartElement();
         tmpstr = se.getName().getLocalPart().trim();

         if (tmpstr.toLowerCase().equals("formula")) {
           String tempstr=se.toString();

// attempting to read an attribute that is absent causes a throw,
// so need to check for its presence before reading

           if (tempstr.toLowerCase().contains("concise")) {
             Attribute tempName = se.getAttributeByName(new QName("concise"));
             MolForm = tempName.getValue();
           }
           else if (tempstr.toLowerCase().contains("inline")) {
              Attribute tempName = se.getAttributeByName(new QName("inline"));
              MolForm = tempName.getValue();
           }
//           System.out.println("MolForm= " + MolForm);
         }
         if (tmpstr.toLowerCase().equals("name")) {
             Attribute tempName = se.getAttributeByName(new QName("convention"));
             e = fer.nextEvent();
             CASNo=e.toString().trim();
//             System.out.println("CASNo= " + CASNo);
           }
       } // end of start_element
    } //end of while fer.hasNext
  }  catch (Exception ex) {
     System.err.println("error reading Sample section");
  }
}   // end of processSample()

/**
 * Process the spectrumdata CML events
 *@throws Exception
 */
public static void processSpectrum() throws Exception {
     String Ydelim="";

try {
    while ((fer.hasNext()) && (!tmpEnd.toLowerCase().equals("spectrumdata")) ) {
      e = fer.nextEvent();
      eventType = e.getEventType();
      if (eventType == XMLStreamConstants.END_ELEMENT)
        tmpEnd = e.asEndElement().getName().getLocalPart().trim();

      if (eventType == XMLStreamConstants.START_ELEMENT) {
        se = e.asStartElement();
        tmpstr = se.getName().getLocalPart().trim();
//        System.out.println(" tmpstr= " + tmpstr);

         if (tmpstr.toLowerCase().equals("xaxis")) {
           String tempstr=se.toString();

// attempting to read an attribute that is absent causes a throw,
// so need to check for its presence before reading

           if (tempstr.toLowerCase().contains("multipliertodata")) {
             Attribute AxisMultiplier = se.getAttributeByName(new QName("multiplierToData"));
             tmpstr = AxisMultiplier.getValue().toString().trim();
             xFactor = Double.parseDouble(AxisMultiplier.getValue().toString().trim());
//             System.out.println("xFactor= " + xFactor);
           }
           e=fer.nextTag();
           tmpstr =e.asStartElement().getName().getLocalPart();
           se = e.asStartElement();
           tempstr=e.toString();

          if (tmpstr.toLowerCase().equals("array")) {

            Attribute xunit = se.getAttributeByName(new QName("units"));
            xaxisUnit = xunit.getValue().toString();
            Integer pos= Integer.valueOf(xaxisUnit.indexOf(":"));
            xaxisUnit=xaxisUnit.substring(pos.intValue()+1,xaxisUnit.length()).toUpperCase();
            if (xaxisUnit.toLowerCase().equals("cm-1"))
                xaxisUnit="1/CM";
            if (xaxisUnit.toLowerCase().equals("nm"))
                xaxisUnit="NANOMETERS";
//          System.out.println("X value type= "+ xaxisUnit );


            Attribute size = se.getAttributeByName(new QName("size"));
            npoints = Integer.parseInt(size.getValue().toString());
//            System.out.println("npoints= "+ npoints );

            xaxisData = new double[npoints];

            if (tempstr.toLowerCase().contains("start")) {
              Attribute startX = se.getAttributeByName(new QName("start"));
              firstX = Double.parseDouble(startX.getValue().toString());
//              System.out.println("firstX= " + firstX);

              Attribute endX = se.getAttributeByName(new QName("end"));
              lastX = Double.parseDouble(endX.getValue().toString());
 //             System.out.println("lastX= " + lastX);

              deltaX=(lastX-firstX)/(npoints-1);
              increasing = deltaX > 0 ? true : false;
              continuous = true;
              for (int j = 0; j < npoints; j++) {
                xaxisData[j] = firstX + (deltaX * j);
//                System.out.println(xaxisData[j]);
              }
            } //autoincremented X values
            else {
                 int posDelim=0;
                 int jj=-1;
                 String tempX="";
                 Ydelim=" ";

                  e=fer.peek();
                  eventType = e.getEventType();

                  while (eventType != XMLStreamConstants.CHARACTERS) {
 //                  System.out.println(e.toString()+ " "+ eventType);
                    e = fer.nextEvent();
                  }

                  tempstr="";

                  do {
                    e = fer.nextEvent();
                    eventType = e.getEventType();
                    if (eventType == XMLStreamConstants.CHARACTERS)
                         tempstr+=e.toString();
                  } while (eventType == XMLStreamConstants.CHARACTERS) ;

// now that we have the full string should tokenise it to then process individual X values
// for now using indexOf !!

                 do {
                    jj ++;
                    posDelim= tempstr.indexOf(Ydelim);
                    tempX=tempstr.substring(0,posDelim);
                    xaxisData[jj] = Double.parseDouble(tempX)* xFactor;
 //                   System.out.println(jj+" a "+xaxisData[jj] );
                    tempstr= tempstr.substring(posDelim+1, tempstr.length()).trim();
                    posDelim= tempstr.indexOf(Ydelim);
                    while (posDelim > 0 ){
                       jj++ ;
                       tempX=tempstr.substring(0,posDelim);
                       xaxisData[jj] = Double.parseDouble(tempX)* xFactor;
//                       System.out.println(jj+" b "+xaxisData[jj] );
                       tempstr= tempstr.substring(posDelim+1, tempstr.length()).trim();
                       posDelim= tempstr.indexOf(Ydelim);
                    }
                    if (jj < npoints-1) {
                       jj ++;
                       xaxisData[jj] = Double.parseDouble(tempstr)* xFactor;
//                     System.out.println(jj+" c "+xaxisData[jj] );
                    }
            } while (jj < npoints-1);
            firstX= xaxisData[0];
            lastX = xaxisData[npoints - 1];
            continuous = true;
         } // end of individual X values
        } // end of X array
//          System.out.println("finished with X");
       } // end of xaxis

        if (tmpstr.toLowerCase().equals("yaxis")) {
//          System.out.println("in Y axis");
           tempstr=se.toString();
//           System.out.println(tempstr);
          if (tempstr.toLowerCase().contains("multipliertodata")) {
            Attribute AxisMultiplier = se.getAttributeByName(new QName("multiplierToData"));
            tmpstr = AxisMultiplier.getValue().toString().trim();
            yFactor = Double.parseDouble(AxisMultiplier.getValue().toString().trim());
//            System.out.println("yFactor= " + yFactor);
          }

           e=fer.nextTag();
           tempstr=e.toString();
           tmpstr =e.asStartElement().getName().getLocalPart();
           se = e.asStartElement();

         if (tmpstr.toLowerCase().equals("array")) {
           Attribute yunit = se.getAttributeByName(new QName("units"));
           yaxisUnit = yunit.getValue().toString().trim();
//           System.out.println(yaxisUnit);
           Integer pos= Integer.valueOf(yaxisUnit.indexOf(":"));
           yaxisUnit=yaxisUnit.substring(pos.intValue()+1,yaxisUnit.length()).toUpperCase();
           if (yaxisUnit.toLowerCase().contains("arbitrary"))
              yaxisUnit="ARBITRARY UNITS";
//           System.out.println("Y value type= "+ yaxisUnit );

           Attribute size = se.getAttributeByName(new QName("size"));
           Integer npointsY = Integer.valueOf(size.getValue().toString());
//           System.out.println("npointsY= "+ npointsY + " " + npoints );
           if (npoints != npointsY.intValue())
             System.err.println("npoints variation between X and Y arrays");
           yaxisData = new double[npoints];

           if (tempstr.toLowerCase().contains("delimiter")) {
            Attribute Ydelimiter =se.getAttributeByName(new QName("delimiter"));
            Ydelim= Ydelimiter.getValue().toString();
           } else
             Ydelim=" ";

           int posDelim=0;
           int jj=-1;
           String tempY="";

            e=fer.peek();
            eventType = e.getEventType();

            while (eventType != XMLStreamConstants.CHARACTERS) {
              e = fer.nextEvent();
              eventType = e.getEventType();
//              System.out.println(eventType);
            }
            tempstr="";

            do {
              e = fer.nextEvent();
              eventType = e.getEventType();
              if (eventType == XMLStreamConstants.CHARACTERS)
                tempstr += e.toString();
            } while (eventType == XMLStreamConstants.CHARACTERS) ;

// now that we have the full string should tokenise it to then process individual Y values
// for now using indexOf !!

           do {
              jj ++;
              posDelim = tempstr.indexOf(Ydelim);
              tempY = tempstr.substring(0,posDelim);
              yaxisData[jj] = Double.parseDouble(tempY)* yFactor;
 //             System.out.println(jj+" a "+xaxisData[jj]+" "+yaxisData[jj]);
              tempstr = tempstr.substring(posDelim+1, tempstr.length()).trim();
              posDelim = tempstr.indexOf(Ydelim);
              while (posDelim > 0 ){
                 jj++ ;
                 tempY = tempstr.substring(0,posDelim);
                 yaxisData[jj] = Double.parseDouble(tempY)* yFactor;
//                 System.out.println(jj+" b "+xaxisData[jj]+" "+yaxisData[jj]);
//                 System.out.println(jj+" "+ tempstr);
                 tempstr = tempstr.substring(posDelim+1, tempstr.length()).trim();
                 posDelim = tempstr.indexOf(Ydelim);
              }
              if (jj < npoints-1) {
                 jj ++;
                 yaxisData[jj] = Double.parseDouble(tempstr)* yFactor;
//                 System.out.println(jj+" c "+xaxisData[jj]+" "+yaxisData[jj]);
              }
            } while (jj < npoints-1);
          }
           firstY = yaxisData[0];
//           System.out.println(firstY);
        }
//          System.out.println("finished with Y");
      } // end if not startelement
      specfound = true;
    } // end of hasNext

  } catch (Exception ex) {
      System.err.println("error reading SpectrumData section: "+ ex.toString());
   }

} // end of processSpectrumData

 /**
  * Process the peakList CML events
  *@throws Exception
  */
public static void processPeaks() throws Exception {
   // don't know how many peaks to expect so set an arbitrary number of 100
      int jj=-1;
      int arbsize = 100;
      xaxisData = new double[arbsize];
      yaxisData = new double[arbsize];

 try {
     while ((fer.hasNext()) && (!tmpEnd.toLowerCase().equals("peaklist")) ) {
       e = fer.nextEvent();
       eventType = e.getEventType();
       if (eventType == XMLStreamConstants.END_ELEMENT)
         tmpEnd = e.asEndElement().getName().getLocalPart().trim();

       if (eventType == XMLStreamConstants.START_ELEMENT) {
         se = e.asStartElement();
         tmpstr = se.getName().getLocalPart().trim();

          if (tmpstr.toLowerCase().equals("peak")) {
            String tempstr=se.toString();

// attempting to read an attribute that is absent causes a throw,
// so need to check for its presence before reading

            if (tempstr.toLowerCase().contains("xvalue")) {
              jj ++;
              Attribute xValue = se.getAttributeByName(new QName("xValue"));
              xaxisData[jj]=Double.parseDouble(xValue.getValue());;
//              System.out.println(" xValue= " + xaxisData[jj]);
            }

            if (tempstr.toLowerCase().contains("xunits")) {
              Attribute xunit = se.getAttributeByName(new QName("xUnits"));
              xaxisUnit = xunit.getValue().toString().trim();
              Integer pos= Integer.valueOf(xaxisUnit.indexOf(":"));
              xaxisUnit=xaxisUnit.substring(pos.intValue()+1, xaxisUnit.length()).toUpperCase();
//              System.out.println("xUnit= " + xaxisUnit);
              if (xaxisUnit.toLowerCase().equals("moverz"))
            	  xaxisUnit="M/Z";
            }

            if (tempstr.toLowerCase().contains("yvalue")) {
              Attribute yValue = se.getAttributeByName(new QName("yValue"));
              yaxisData[jj]=Double.parseDouble(yValue.getValue());;
//              System.out.println(" yValue= " + yaxisData[jj]);
            }

            if (tempstr.toLowerCase().contains("yunits")) {
              Attribute yunit = se.getAttributeByName(new QName("yUnits"));
              yaxisUnit = yunit.getValue().toString().trim();
              Integer pos= Integer.valueOf(yaxisUnit.indexOf(":"));
              yaxisUnit=yaxisUnit.substring(pos.intValue()+1, yaxisUnit.length()).toUpperCase();
              if (yaxisUnit.toLowerCase().equals("relabundance"))
                  yaxisUnit="RELATIVE ABUNDANCE";
              if (yaxisUnit.toLowerCase().contains("arbitrary"))
                 yaxisUnit="ARBITRARY UNITS";
//              System.out.println("yUnit= " + yaxisUnit);
            }

// for CML exports from NMRShiftDB there are no Y values or Y units
// given in the Peaks, just XValues
// to use the JCAMP-DX plot routines we assign a Yvalue
// of 50 for every atom referenced

            if (tempstr.toLowerCase().contains("atomrefs")) {
              Attribute atomRefs = se.getAttributeByName(new QName("atomRefs"));
              String atomrefs = atomRefs.getValue().toString();
              StringTokenizer srt = new StringTokenizer(atomrefs);
//            System.out.println(atomrefs+" "+srt.countTokens());
              yaxisData[jj] = 49 * (srt.countTokens());
            }
          }  // end of peak
       } // end if not startelement
     } // end of hasNext

// now that we have X,Y pairs set JCAMP-DX equivalencies
// FIRSTX, FIRSTY, LASTX, NPOINTS
// determine if the data is in increasing or decreasing order
// since a PeakList the data is not continuous

   npoints=jj+1;
   firstX= xaxisData[0];
   lastX = xaxisData[npoints - 1];
   firstY= yaxisData[0];
   increasing = lastX > firstX ? true : false;
   continuous = false;

   } catch (Exception ex) {
       System.err.println("error reading PeakList section: "+ ex.toString());
    }

 } // end of processPeaks

  }
