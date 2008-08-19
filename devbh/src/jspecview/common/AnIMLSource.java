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
//import java.util.Iterator;
import javax.xml.stream.*;
import javax.xml.stream.events.* ;
import javax.xml.namespace.QName;

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

public class AnIMLSource extends JDXSource {
  public static String tmpstr="START",tempstr="", title="identifier not found", owner="UNKNOWN",origin = "UNKNOWN";
  public static String tmpEnd="END", MolForm="", techname="";
  public static StartElement se=null;
  public static XMLEvent e;
  public static XMLEventReader fer;
  public static int eventType=-1;
  public static int npoints=-1, samplenum=-1;
  public static double[] yaxisData;
  public static double[] xaxisData;
  public static String xaxisLabel = "",  xaxisUnit = "", xaxisType = "";
  public static  String yaxisLabel = "",  yaxisUnit = "", yaxisType = "";
  public static  String vendor = "na", modelType = "MODEL UNKNOWN", LongDate = "";
  public static  String pathlength= "na", identifier="", plLabel="";
  public static  String resolution="na", resLabel="", LocName="";
  public static  String LocContact="", CASrn="";
  public static  String sampleowner="", obNucleus="", StrObFreq="";
  public static  boolean increasing = false, continuous=false;
  public static  int ivspoints, evspoints, sampleRefNum=0;
  public static  double deltaX = Graph.ERROR;
  public static  double xFactor = Graph.ERROR;
  public static  double yFactor = Graph.ERROR;
  public static  double firstX = Graph.ERROR;
  public static  double lastX = Graph.ERROR;
  public static  double firstY = Graph.ERROR;
  public static  double obFreq= Graph.ERROR;
  public static  double refPoint=Graph.ERROR;
  public static String sampleID;
  public static StringBuffer errorLog = new StringBuffer();
  private static XMLInputFactory factory;
  public static String errorSeparator="________________________________________________________";

  /**
   * Constructs a new XMLSource from an AnIML or CML document
   * @throws JSpecViewException
   */
  protected AnIMLSource() throws JSpecViewException{
    super();
    try {
      jbInit();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Does the actual work of initializing the XMLSource
   * @param in an InputStream of the AnIML document
   * @return an instance of a AnIMLSource
   * @throws JSpecViewException
   */
  public static AnIMLSource getAniMLInstance(InputStream in) throws JSpecViewException {
       // The XMLSource Instance
//    StringBuffer errorLog = new StringBuffer();
    AnIMLSource xs = new AnIMLSource();
    HashMap<String, String> LDRTable = new HashMap<String, String>(20);
    Coordinate AMLpoint;
    Vector<Coordinate> xyCoords = new Vector<Coordinate>();
    JDXSpectrum spectrum = new JDXSpectrum();

    try {
//   First, get the factory instance.

     if (factory==null)
         factory = XMLInputFactory.newInstance();
     XMLEventReader nofr=factory.createXMLEventReader(in);
     fer= factory.createFilteredReader(nofr, new EventFilter() {
         public boolean accept (XMLEvent event) {
         return ((event.isStartDocument()) || (event.isStartElement()) || (event.isCharacters()) || (event.isEndElement()) );
       }
     });

//      e= fer.peek();
//      System.out.println(e.toString());
//      if (e.getEventType()!=XMLStreamConstants.START_DOCUMENT) {
//        System.err.println("Error, not an XML document?");
//        throw new IOException();
//      }

         e=fer.nextEvent();
//check if document is empty
        try {
            e= fer.peek();
        }catch (Exception e){
            System.err.println("Error, empty document?");
            return null;
        }



        do {
           e= fer.nextEvent();
           eventType = e.getEventType();
//           System.out.println(e.toString();
           if (eventType==XMLStreamConstants.START_ELEMENT){
               tmpstr = e.asStartElement().getName().getLocalPart().trim();
               tempstr= e.toString();
            }
              if (tmpstr.toLowerCase().equals("sampleset")){
                processSample();
               }else if (tmpstr.toLowerCase().equals("experimentstepset")){
                processMeasurement();
              }else if (tmpstr.toLowerCase().equals("audittrail")){
               processAudit();
              }
         } while (fer.hasNext());


// end of import of AnIML document
// now populate all the JSpecView spectrum variables.....

      spectrum.setTitle(title);
      spectrum.setJcampdx("5.01");
      spectrum.setDataClass("XYDATA");
      spectrum.setDataType(techname);
      spectrum.setContinuous(continuous);
      spectrum.setIncreasing(increasing);
      spectrum.setXFactor(1.0);
      spectrum.setYFactor(1.0);
      spectrum.setLongDate(LongDate);
      spectrum.setOrigin(origin);
      spectrum.setOwner(owner);
      spectrum.setPathlength(pathlength);

//  now fill in what we can of a HashMap with parameters from the AnIML file
//      LDRTable.put()
//      Key kk = new Key;
      LDRTable.put("##PATHLENGTH", pathlength);
      LDRTable.put("##RESOLUTION",resolution);
      if (!StrObFreq.equals(""))
        LDRTable.put("##.OBSERVEFREQUENCY",StrObFreq);
      if (!obNucleus.equals(""))
        LDRTable.put("##.OBSERVENUCLEUS",obNucleus);
        LDRTable.put("##$MANUFACTURER",vendor);
      if (!CASrn.equals("") )
        LDRTable.put("##CASREGISTRYNO",CASrn);
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
          xyCoords.insertElementAt(AMLpoint,0);
      }

      spectrum.setXUnits(xaxisUnit);
      spectrum.setYUnits(yaxisUnit);

      Coordinate[] amlcoord = new Coordinate[npoints];
      xyCoords.toArray(amlcoord);
      spectrum.setXYCoords(amlcoord);
    }

    catch (Exception pe) {
      System.err.println("That file may be empty...");
      errorLog.append("That file may be empty... \n");
      errorLog.append("these errors were found catch animl \n");
    }

// for ease of processing later, return a source rather than a spectrum
//    return XMLSource.getXMLInstance(spectrum);
if (errorLog.length()>0){
        errorLog.append("these errors were found animl \n");
    }else {
        errorLog.append("No Errors\n");
    }
    errorLog.append(errorSeparator);
    xs.setErrorLog(errorLog.toString());
    
    xs.addJDXSpectrum(spectrum);
    
    return xs;
  }

/**
 * Process the sample XML events
 *@throws Exception
 */
public static void processSample() throws Exception {
  try {
     while ((fer.hasNext()) && (!tmpEnd.toLowerCase().equals("sampleset")) ){
       e = fer.nextEvent();
//       System.out.println(e.toString());
       eventType = e.getEventType();
       if (eventType == XMLStreamConstants.END_ELEMENT)
         tmpEnd = e.asEndElement().getName().getLocalPart().trim();

       while (eventType != XMLStreamConstants.START_ELEMENT) {
         e = fer.nextTag();
         eventType = e.getEventType();
         if (eventType == XMLStreamConstants.END_ELEMENT)
           tmpEnd = e.asEndElement().getName().getLocalPart().trim();
       }

       if (eventType == XMLStreamConstants.START_ELEMENT) {
         se = e.asStartElement();
         tmpstr = se.getName().getLocalPart().trim();
       } //end of start element

// with no way to distinguish sample from reference until the experiment step
// this is not correct and values may get overwritten!!

        if (tmpstr.toLowerCase().equals("sample")) {
          samplenum ++;
           //Attribute sampleID = se.getAttributeByName(new QName("sampleID"));
 //          System.out.println(samplenum+ " "+sampleID.getName() + " = " + sampleID.getValue());
         }

         if (tmpstr.toLowerCase().equals("parameter")) {
           Attribute tempName = se.getAttributeByName(new QName("name"));
           if (tempName.getValue().toLowerCase().equals("name")) {
             //need to move through to <string> then get CHARACTERS
             e = fer.nextTag();
             e = fer.nextEvent();
//            System.out.println("sample name= " + e.toString().trim());
//             title = e.toString().trim();
           }
           else if (tempName.getValue().toLowerCase().equals("owner")) {
             //need to move through to <string> then get CHARACTERS
             e = fer.nextTag();
             e = fer.nextEvent();
 //          System.out.println("sample owner= " + e.toString().trim());
     //      owner=e.toString().trim();
           }
           else if (tempName.getValue().toLowerCase().equals("molecular formula")) {
             //need to move through to <string> then get CHARACTERS
             e = fer.nextTag();
             e = fer.nextEvent();
     //       System.out.println("molecular formula= " + e.toString().trim());
             MolForm = e.toString().trim();
           }
           else if (tempName.getValue().toLowerCase().equals("cas registry number")) {
             //need to move through to <string> then get CHARACTERS
             e = fer.nextTag();
             e = fer.nextEvent();
     //       System.out.println("CAS Reg No= " + e.toString().trim());
             CASrn = e.toString().trim();
           }
//  continue as above to collect information like Temp, BP etc
         } // end of parameters

    } // end of hasnext

  }  catch (Exception ex) {
     System.err.println("error reading Sample section");
     errorLog.append("error reading Sample section\n");
  }
}   // end of processSample()

/**
 * Process the ExperimentStepSet XML events
 *@throws Exception
 */
public static void processMeasurement() throws Exception {

  boolean bstart=false;

try {
    while ((fer.hasNext()) && (!tmpEnd.toLowerCase().equals("experimentstepset")) ) {
      e = fer.nextEvent();
      eventType = e.getEventType();

      if (eventType == XMLStreamConstants.END_ELEMENT)
        tmpEnd = e.asEndElement().getName().getLocalPart().trim();

      while (eventType != XMLStreamConstants.START_ELEMENT) {
        e = fer.nextEvent();
        eventType = e.getEventType();
        if (eventType == XMLStreamConstants.END_ELEMENT)
          tmpEnd = e.asEndElement().getName().getLocalPart().trim();
      }

      if (eventType == XMLStreamConstants.START_ELEMENT) {
        se = e.asStartElement();
        tmpstr = se.getName().getLocalPart().trim();
      }

      if (tmpstr.toLowerCase().equals("sampleref")) {
        Attribute sampleRef = se.getAttributeByName(new QName("sampleID"));
        Attribute role =se.getAttributeByName(new QName("role"));

        if (role.toString().toLowerCase().contains("samplemeasurement")) {
          sampleID=sampleRef.getValue();
//          System.out.println(sampleID);
        }
      }

        //now start on Author Info
        if (tmpstr.toLowerCase().equals("author")) {
          //Attribute authType = se.getAttributeByName(new QName("type"));

           do {
             e = fer.nextEvent();
             eventType = e.getEventType();

             if (eventType ==XMLStreamConstants.END_ELEMENT)
                tmpEnd = e.asEndElement().getName().getLocalPart().trim();

             while (eventType != XMLStreamConstants.START_ELEMENT) {
               e = fer.nextTag();
               eventType = e.getEventType();
               if (eventType == XMLStreamConstants.END_ELEMENT)
                 tmpEnd = e.asEndElement().getName().getLocalPart().trim();
             }

             if (e.isStartElement()) {
               se = e.asStartElement();
               tmpstr = se.getName().getLocalPart().trim();
             }
               if (tmpstr.toLowerCase().equals("name")) {
          //      System.out.println("Author Name= " + e.toString().trim());
                  e = fer.nextEvent();
                  owner = e.toString().trim();
               }
               if (tmpstr.toLowerCase().contains("location")) {
          //      System.out.println("Author Name= " + e.toString().trim());
                  e = fer.nextEvent();
                  origin = e.toString().trim();
               }

          } while (!tmpEnd.toLowerCase().equals("author"));

        } // end of <author>

        if (tmpstr.toLowerCase().equals("timestamp")) {
          e = fer.nextEvent();
          LongDate = e.toString();
        }

        if (tmpstr.toLowerCase().equals("technique")) {
          Attribute techType = se.getAttributeByName(new QName("name"));
          techname = techType.getValue().toString().toUpperCase() + " SPECTRUM";
        }

        if (tmpstr.toLowerCase().equals("vectorset")) {
          Attribute vectorSet = se.getAttributeByName(new QName("length"));
          npoints = Integer.parseInt(vectorSet.getValue());
//          System.out.println("No. of points= "+ npoints);
          xaxisData = new double[npoints];
          yaxisData = new double[npoints];
        }

        if (tmpstr.toLowerCase().equals("vector")) {
          Attribute AxisType = se.getAttributeByName(new QName("dependency"));
          Attribute AxisLabel = se.getAttributeByName(new QName("name"));
          Attribute VectorType = se.getAttributeByName(new QName("type"));

          if (AxisType.getValue().toLowerCase().equals("independent")) {
    //            System.out.println("X axis label= " + AxisLabel.getValue());
            xaxisLabel = AxisLabel.getValue();

            tmpstr = fer.nextTag().asStartElement().getName().getLocalPart();
            if (tmpstr.toLowerCase().equals("autoincrementedvalueset")) {
              tmpstr = fer.nextTag().asStartElement().getName().getLocalPart();
              if (tmpstr.toLowerCase().equals("startvalue")) {
                e = fer.nextTag();
                e = fer.nextEvent();
    //                   System.out.println("First X= "+e.toString().trim());
                firstX = Double.parseDouble(e.toString().trim());
              }
              e = fer.nextTag(); // end of X start int or float
              e = fer.nextTag(); // end of </startvalue>
              tmpstr = fer.nextTag().asStartElement().getName().getLocalPart();

              if (tmpstr.toLowerCase().equals("increment")) {
                e = fer.nextTag();
                e = fer.nextEvent();
                deltaX = Double.parseDouble(e.toString().trim());
              }
            }
            e = fer.nextTag();
            while (!e.isStartElement()) {
              e = fer.nextTag();
            }
            se = e.asStartElement();
            Attribute xunit = se.getAttributeByName(new QName("label"));
    //           System.out.println("X Unit= "+ xunit.getValue().toString() );
            xaxisUnit = xunit.getValue().toString();
    //           System.out.println("X value type= "+VectorType.getValue() );

            increasing = deltaX > 0 ? true : false;
            continuous = true;
            for (int j = 0; j < npoints; j++) {
              xaxisData[j] = firstX + (deltaX * j);
    //             System.out.println(xaxisData[j]);
            }
            lastX = xaxisData[npoints - 1];

    //        System.out.println("end of X value area");
          } // end of X information

          if (AxisType.getValue().toLowerCase().equals("dependent")) {

            yaxisLabel = AxisLabel.getValue();
            tmpstr = fer.nextTag().asStartElement().getName().getLocalPart();
            if (tmpstr.toLowerCase().equals("individualvalueset")) {
              for (int ii = 0; ii < npoints; ii++) {
                e = fer.nextTag();
                e = fer.nextEvent();
    //               System.out.println("Y value "+ii+" "+e.toString().trim());
                yaxisData[ii] = Double.parseDouble(e.toString().trim());
                e = fer.nextTag();
              }
    //             System.out.println("individual Y values now read");
            }
            if (tmpstr.toLowerCase().equals("encodedvalueset")) {

              String base64String = "";
              e=fer.peek();
              eventType = e.getEventType();
              while (eventType != XMLStreamConstants.CHARACTERS) {
//                    System.out.println(e.toString()+ " "+ eventType);
                e = fer.nextEvent();
              }

// to avoid problems when the base64 info has carriage returns inserted into the middle
// keep appending to the string while searching for the next non-CHARACTER tag

              while (eventType == XMLStreamConstants.CHARACTERS) {
                e = fer.nextEvent();
                eventType = e.getEventType();
                if (eventType == XMLStreamConstants.CHARACTERS)
                  base64String += e.toString();
              }
    //               System.out.println(base64String);

              byte[] dataArray = Base64.decode(base64String);
              int ij = 0;
              if (dataArray.length != 0) {
                ByteBuffer byte_buffer = ByteBuffer.wrap(dataArray).order(ByteOrder.
                    LITTLE_ENDIAN);

                // float64
                if (VectorType.getValue().toLowerCase().equals("float64")) {
                  DoubleBuffer double_buffer = byte_buffer.asDoubleBuffer();
                  for (ij = 0; double_buffer.remaining() > 0; ij++) {
                    yaxisData[ij] = double_buffer.get();
    //                      System.out.println(ij + " "+yaxisData[ij]);

                  }
                }
                // float32
                else if (VectorType.getValue().toLowerCase().equals("float32")) {
                  FloatBuffer float_buffer = byte_buffer.asFloatBuffer();
                  for (ij = 0; float_buffer.remaining() > 0; ij++) {
                    yaxisData[ij] = float_buffer.get();
                    //                   System.out.println(ij+ " " +yaxisData[ij]);
                  }
                }

              }
  //             System.out.println("encoded Y values now read");
              e = fer.nextTag();
            }  // end of encoded Y values

            bstart = e.isStartElement();
            while (!bstart) {
              e = fer.nextTag();
              bstart = e.isStartElement();
            }
            se = e.asStartElement();
            tmpstr = se.toString();
            Attribute yunit = se.getAttributeByName(new QName("label"));
    //               System.out.println("Y Unit= "+ yunit.getValue().toString() );
            yaxisUnit = yunit.getValue().toString();

          } // end of Y information
          firstY = yaxisData[0];
        }

        //now start on additional measurementparameters
        if (tmpstr.toLowerCase().equals("parameter")) {
          Attribute tempName = se.getAttributeByName(new QName("name"));
          if (tempName.getValue().toLowerCase().equals("identifier")) {
            //need to move through to <string> then get CHARACTERS
            e = fer.nextTag();
            e = fer.nextEvent();
            title = e.toString().trim();
    //         System.out.println("identifier= " + e.toString().trim());
          }
          else if (tempName.getValue().toLowerCase().equals("nucleus")) {
            //need to move through to <string> then get CHARACTERS
            e = fer.nextTag();
            e = fer.nextEvent();
    //         System.out.println("nucleus= " + e.toString().trim());
            obNucleus = e.toString().trim();
          }
          else if (tempName.getValue().toLowerCase().equals("observefrequency")) {
            //need to move through to <string> then get CHARACTERS
            e = fer.nextTag();
            e = fer.nextEvent();
            StrObFreq= e.toString().trim();
            obFreq = Double.parseDouble(StrObFreq);
//         System.out.println("observe frequency= " + StrObFreq);
          }
          else if (tempName.getValue().toLowerCase().equals("referencepoint")) {
            //need to move through to <string> then get CHARACTERS
            e = fer.nextTag();
            e = fer.nextEvent();
    //         System.out.println("reference point= " + e.toString().trim());
            refPoint = Double.parseDouble(e.toString().trim());
          }
          else if (tempName.getValue().toLowerCase().equals("sample path length")) {
            //need to move through to <string> then get CHARACTERS
            e = fer.nextTag();
            e = fer.nextEvent();
    //         System.out.println("pathlength= " + e.toString().trim());
            pathlength = e.toString().trim();
          }
          else if (tempName.getValue().toLowerCase().equals("scanmode")) {
            //need to move through to <string> then get CHARACTERS
            e = fer.nextTag();
            e = fer.nextEvent();
    //         System.out.println("scanmode= " + e.toString().trim());
          }
          else if (tempName.getValue().toLowerCase().equals("manufacturer")) {
            //need to move through to <string> then get CHARACTERS
            e = fer.nextTag();
            e = fer.nextEvent();
    //         System.out.println("vendor= " + e.toString().trim());
            vendor = e.toString().trim();
          }
          else if (tempName.getValue().toLowerCase().equals("model name")) {
            //need to move through to <string> then get CHARACTERS
            e = fer.nextTag();
            e = fer.nextEvent();
//           System.out.println("model= " + e.toString().trim());
            modelType = e.toString().trim();
          }
          else if (tempName.getValue().toLowerCase().equals("resolution")) {
            //need to move through to <string> then get CHARACTERS
            e = fer.nextTag();
            e = fer.nextEvent();
//           System.out.println("resolution= " + e.toString().trim());
            resolution = e.toString().trim();
          }

        } // end of parameters
//      } // end if not startelement
    } // end of hasNext

  }   catch (Exception ex) {
      System.err.println("error reading ExperimentStepSet section"+ ex.toString());
      errorLog.append("error reading ExperimentStepSet section\n");
   }

} // end of processMeasurement

/**
 * Process the audit XML events
  *@throws Exception
 */
public static void processAudit() throws Exception {
 try{
   while ((fer.hasNext()) && (!tmpEnd.toLowerCase().equals("audittrail")) ){
     e = fer.nextEvent();
     eventType = e.getEventType();

     if (eventType==XMLStreamConstants.END_ELEMENT)
       tmpEnd = e.asEndElement().getName().getLocalPart().trim();

     if (eventType == XMLStreamConstants.START_ELEMENT) {
      se = e.asStartElement();
      tmpstr = se.getName().getLocalPart().trim();
     }
      if (tmpstr.toLowerCase().equals("user")) {
          //Attribute tempName = se.getAttributeByName(new QName("type"));
         //need to move through to get CHARACTERS
          e = fer.nextTag();
          e = fer.nextEvent();
//          System.out.println("user name= " + e.toString().trim());
       }
       if (tmpstr.toLowerCase().equals("timestamp")) {
         e = fer.nextEvent();
//         System.out.println("timestamp= " + e.toString().trim());
//         timestamp=e.toString().trim();
       }
   } // end while
 } catch (Exception ex){
   System.err.println("error reading Audit section");
   errorLog.append("error reading Audit section\n");
   }
 } // end of processaudit

  private void jbInit() throws Exception {
  }
}  // end of class AnIMLImporter

  /** Decodes Base64 data from encoded AnIML Y values.
   **
   ** @author Peter J. Linstrom
   **/
  class Base64  extends Object {

 /** Table used for encoding data.
 **/
    protected static final char[] aEncodeChars = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '+', '/',
        '='
    };

    /** Table used for decoding data.
     **/
    protected static final int[] aDecodeValues = {
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
        0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
        0xFF, 0xFF, 0xFF, 0x3E, 0xFF, 0xFF, 0xFF, 0x3F,
        0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B,
        0x3C, 0x3D, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
        0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
        0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
        0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
        0x17, 0x18, 0x19, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
        0xFF, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20,
        0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28,
        0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30,
        0x31, 0x32, 0x33, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
    };

    static byte[] decode(String sText) {
      // Check for a bad argument.
      if ( (sText == null) || (sText.length() == 0)) {
        return null;
      }

      // Compute the number of characters to use.
      int iUseChars = 0;

      int iTextLength = sText.length();
      int iTextIndex;

      for (iTextIndex = 0; iTextIndex < iTextLength; iTextIndex++) {
        char cChar = sText.charAt(iTextIndex);

        if ( (cChar < 0x7F) && (aDecodeValues[cChar] < 0x40)) {
          iUseChars++;
        }
      }

      // Make sure we have data to use.
      if (iUseChars == 0) {
        return null;
      }

      // Compute the length of the data array.
      int iDataLength = (iUseChars / 4) * 3;
      int iExtraChars = (iUseChars % 4);

      if (iExtraChars > 1) {
        iDataLength += (iExtraChars - 1);
      }

      // Allocate memory.
      byte aData[] = new byte[iDataLength];

      // Initialize the decoding state.
      int iDataIndex = 0;
      int iBitsUsed = 0;
      byte bNext = (byte) 0;

      // Loop through the string
      for (iTextIndex = 0; iTextIndex < iTextLength; iTextIndex++) {
        // Get the next character.
        char cChar = sText.charAt(iTextIndex);

        // Skip over non-ascii characters.
        if ( (cChar & 0x80) != 0) {
          continue;
        }

        // Decode and check the character.
        int iData = aDecodeValues[cChar];

        if (iData >= 0x40) {
          continue;
        }

        // Add bits.
        int iShift = 2 - iBitsUsed;

        if (iShift > 0) {
          bNext |= (byte) (iData << iShift);
        }
        else {
          bNext |= (byte) (iData >> ( -iShift));
        }

        // Increment the number of bits used.
        iBitsUsed += 6;

        // See if there is more to add.
        if (iBitsUsed >= 8) {
          // Advance to the next position in the data array.
          aData[iDataIndex] = bNext;

          iDataIndex++;

          // Adjust the bit count for the next byte.
          iBitsUsed -= 8;

          // If appropriate, add additional bits.
          if (iBitsUsed > 0) {
            bNext = (byte) (iData << (8 - iBitsUsed));
          }
          else {
            bNext = (byte) 0;
          }
        }
      }

      // Done, return the data array.
      return aData;
    }

    static char[] encode(byte[] aData) {
      // Make sure we have data.
      if ( (aData == null) || (aData.length == 0)) {
        return null;
      }

      // Get the data length.
      int iDataLength = aData.length;

      // Compute the encoded length.
      int iTextLength = ( (iDataLength + 2) / 3) * 4;

      // Allocate memory.
      char aText[] = new char[iTextLength];

      // Loop through the data.
      int iDataIndex = 0;
      int iTextIndex = 0;

      while (iDataIndex < iDataLength) {
        // Get the first byte.
        byte bData = aData[iDataIndex];

        iDataIndex++;

        // Initialize the output.
        int iOne = (bData & 0xFC) >> 2;
        int iTwo = (bData & 0x3) << 4;
        int iThree = 64;
        int iFour = 64;

        // If appropriate, process the second byte.
        if (iDataIndex < iDataLength) {
          // Get the second byte.
          bData = aData[iDataIndex];

          iDataIndex++;

          // Add to the output.
          iTwo |= (bData & 0xF0) >> 4;
          iThree = (bData & 0xF) << 2;
        }

        // If appropriate, process the third byte.
        if (iDataIndex < iDataLength) {
          // Get the third byte.
          bData = aData[iDataIndex];

          iDataIndex++;

          // Add to the output.
          iThree |= (bData & 0xC0) >> 6;
          iFour = (bData & 0x3F);
        }

        // Write out the output string.
        aText[iTextIndex] = aEncodeChars[iOne];
        aText[iTextIndex + 1] = aEncodeChars[iTwo];
        aText[iTextIndex + 2] = aEncodeChars[iThree];
        aText[iTextIndex + 3] = aEncodeChars[iFour];

        // Advance the text index.
        iTextIndex += 4;
      }

      // Return the encoded string.
      return aText;
    }    
// if you reached this far then hopefully no errors?    

  }
