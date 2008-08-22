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

import java.io.*;
import javax.xml.stream.*;
import java.util.StringTokenizer;

/**
 * Representation of a XML Source.
 * @author Craig Walters
 * @author Prof. Robert J. Lancashire
 */

public class CMLSource extends XMLSource {
  private boolean specfound = false;

  /**
   * Does the actual work of initializing the CMLSource
   * @param in an InputStream of the CML document
   * @return an instance of a CMLSource
   */
  public static CMLSource getCMLInstance(InputStream in) {
    return (new CMLSource()).getXML(in);
  }

  private CMLSource getXML(InputStream in) {
    try {

      getFactory(in);

      if (!checkStart(this))
        return this;

      processXML();
      
      if (!checkPointCount())
        return null;
      
      populateVariables();
      
    } catch (Exception pe) {
      System.err.println("Error: " + pe.getMessage());
      errorLog.append("That file may be empty... \n");
    }

    processErrors("CML");
    return this;
  }

  private boolean checkStart(XMLSource xs) throws Exception {
    if (peek() != XMLStreamConstants.START_DOCUMENT) {
      System.err.println("Error, not an XML document?");
      errorLog.append("Error, not an XML document?");
      throw new IOException();
    }

    nextEvent();
    //check if document is empty
    try {
      peek();
    } catch (Exception e) {
      System.err.println("Error, empty document?");
      errorLog.append("Error, empty document?");
      xs.setErrorLog(errorLog.toString());
      xs.addJDXSpectrum(null);
      return false;
    }
    return true;
  }

  protected void processXML() throws Exception {
    //find beginning of <spectrum>
    tmpstr = "";
    while (fer.hasNext() && !tmpstr.equals("spectrum")) {
      if (nextEvent()!= XMLStreamConstants.START_ELEMENT) {
        if (eventType == XMLStreamConstants.END_ELEMENT)
          tmpEnd = getEndTag();
        continue;
      }
      tmpstr = getTagName();
      attrList = getAttributeList();
    }

    // title seems optional so check if it is present,
    // if not found, then set the JCAMP-DX title to ID
 

    if (attrList.contains("title")) 
      title = getAttrValue("title");
    else if(attrList.contains("id"))
      title = getAttrValue("id");

    // type is required so it should be present as well and no need to check if present
    // check anyway, to be safe.

    if (attrList.contains("type")) 
      techname = getAttrValue("type").toUpperCase() + " SPECTRUM";

    // now start to process the remainder of the document
    while (fer.hasNext()) {
      if (nextEvent() == XMLStreamConstants.START_ELEMENT) {
        tmpstr = getTagName();
        attrList = getAttributeList();
        if (tmpstr.equals("metadatalist")) {
          processMetadata();
        } else if (tmpstr.equals("conditionlist")) {
          processConditions();
        } else if (tmpstr.equals("parameterlist")) {
          processParameters();
        } else if (tmpstr.equals("sample")) {
          processSample();
        } else if (tmpstr.equals("spectrumdata")) {
          processSpectrum();
          // if a spectrum is found, ignore a peaklist if present as well
          // since without intervention it is not possible to guess
          // which display is required and the spectrum is probably the
          // more critical ?!?
        } else if ((tmpstr.equals("peaklist")) && (!specfound)) {
          processPeaks();
        }
      }
    }
  }

  /**
   * Process the metadata CML events
   *@throws Exception
   */
  public void processMetadata() throws Exception {
    //        StringBuffer errorLog = new StringBuffer();
    try {
      while ((fer.hasNext()) && (!tmpEnd.equals("metadatalist"))) {
        if (nextEvent() != XMLStreamConstants.START_ELEMENT) {
          if (eventType == XMLStreamConstants.END_ELEMENT)
            tmpEnd = getEndTag();
          continue;
        }
        tmpstr = getTagName();
        attrList = getAttributeList();
        if (tmpstr.equals("metadata")) {
          tmpstr = getAttrValueLC("name");
          if (tmpstr.contains(":origin")) {
            if (attrList.contains("content"))
              origin = getAttrValue("content");
            else
              origin = thisValue();
          } else if (tmpstr.contains(":owner")) {
            if (attrList.contains("content"))
              owner = getAttrValue("content");
            else
              owner = thisValue();
          }
        }
      }
    } catch (Exception ex) {
      System.err.println("error reading metadataList section");
      errorLog.append("error reading metadata List section\n");
    }
  } // end of processMetadata

  /**
   * Process the parameter CML events
   *@throws Exception
   */
  public void processParameters() throws Exception {
    try {
      while (fer.hasNext() && !tmpEnd.equals("parameterlist")) {
        if (nextEvent() != XMLStreamConstants.START_ELEMENT) {
          if (eventType == XMLStreamConstants.END_ELEMENT)
            tmpEnd = getEndTag();
          continue;
        }
        tmpstr = getTagName();
        attrList = getAttributeList();
        if (tmpstr.equals("parameter")) {
          String title = getAttrValueLC("title");
          if (title.equals("nmr.observe frequency")) {
            StrObFreq = nextValue();
            obFreq = Double.parseDouble(StrObFreq);
          } else if (title.equals("nmr.observe nucleus")) {
            obNucleus = thisValue();
          } else if (title.equals("spectrometer/data system")) {
            modelType = thisValue();
          } else if (title.equals("resolution")) {
            resolution = nextValue();
          }
        }
      }
    } catch (Exception ex) {
      System.err.println("error reading parameterList section");
      errorLog.append("error reading Parameters\n");
    }
  } 

  /**
   * Process the ConditionList CML events (found in NMRShiftDB)
   *@throws Exception
   */
  public void processConditions() throws Exception {
    try {
      while (fer.hasNext() && !tmpEnd.equals("conditionlist")) {
        if (nextEvent() != XMLStreamConstants.START_ELEMENT) {
          if (eventType == XMLStreamConstants.END_ELEMENT)
            tmpEnd = getEndTag();
          continue;
        }
        tmpstr = getTagName();
        attrList = getAttributeList();
        if (tmpstr.equals("scalar")) {
          String dictRef = getAttrValueLC("dictRef");
          if (dictRef.contains(":field")) {
            StrObFreq = thisValue();
            if (((int) StrObFreq.charAt(0) > 47)
                && ((int) StrObFreq.charAt(0) < 58))
              obFreq = Double.parseDouble(StrObFreq);
          }
        }
      } 
    } catch (Exception ex) {
      System.err.println("error reading conditionList section");
      errorLog.append("error reading Conditions section\n");
    }
  }

  /**
   * Process the sample CML events
   *@throws Exception
   */
  public void processSample() throws Exception {
    try {
      while ((fer.hasNext()) && (!tmpEnd.equals("sample"))) {
        if (nextEvent() != XMLStreamConstants.START_ELEMENT) {
          if (eventType == XMLStreamConstants.END_ELEMENT)
            tmpEnd = getEndTag();
          continue;
        }
        tmpstr = getTagName();
        attrList = getAttributeList();
        if (tmpstr.equals("formula")) {
          if (attrList.contains("concise"))
            molForm = getAttrValue("concise");
          else if (attrList.contains("inline"))
            molForm = getAttrValue("inline");
        } else if (tmpstr.equals("name")) {
          casName = thisValue();
        }
      }
    } catch (Exception ex) {
      System.err.println("error reading Sample section");
      errorLog.append("error reading sample section\n");
    }
  }

  /**
   * Process the spectrumdata CML events
   *@throws Exception
   */
  public void processSpectrum() throws Exception {
    String Ydelim = "";
    try {
      while (fer.hasNext() && !tmpEnd.equals("spectrumdata")) {
        if (nextEvent() != XMLStreamConstants.START_ELEMENT) {
          if (eventType == XMLStreamConstants.END_ELEMENT)
            tmpEnd = getEndTag();
          continue;
        }
        tmpstr = getTagName();
        attrList = getAttributeList();
        if (tmpstr.equals("xaxis")) {
          if (attrList.contains("multipliertodata"))
            xFactor = Double.parseDouble(getAttrValue("multiplierToData"));
          nextTag();
          tmpstr = getTagName();
          attrList = getAttributeList();
          if (tmpstr.equals("array")) {
            xaxisUnit = getAttrValue("units");
            Integer pos = Integer.valueOf(xaxisUnit.indexOf(":"));
            xaxisUnit = xaxisUnit.substring(pos.intValue() + 1,
                xaxisUnit.length()).toUpperCase();
            if (xaxisUnit.toLowerCase().equals("cm-1"))
              xaxisUnit = "1/CM";
            if (xaxisUnit.toLowerCase().equals("nm"))
              xaxisUnit = "NANOMETERS";
            npoints = Integer.parseInt(getAttrValue("size"));
            xaxisData = new double[npoints];
            if (attrList.contains("start")) {
              firstX = Double.parseDouble(getAttrValue("start"));
              lastX = Double.parseDouble(getAttrValue("end"));
              deltaX = (lastX - firstX) / (npoints - 1);
              increasing = deltaX > 0 ? true : false;
              continuous = true;
              for (int j = 0; j < npoints; j++)
                xaxisData[j] = firstX + (deltaX * j);
            } else {
              int posDelim = 0;
              int jj = -1;
              String tempX = "";
              Ydelim = " ";
              attrList = getCharacters();

              // now that we have the full string should tokenise it to then process individual X values
              // for now using indexOf !!

              do {
                jj++;
                posDelim = attrList.indexOf(Ydelim);
                tempX = attrList.substring(0, posDelim);
                xaxisData[jj] = Double.parseDouble(tempX) * xFactor;
                //                   System.out.println(jj+" a "+xaxisData[jj] );
                attrList = attrList.substring(posDelim + 1, attrList.length())
                    .trim();
                posDelim = attrList.indexOf(Ydelim);
                while (posDelim > 0) {
                  jj++;
                  tempX = attrList.substring(0, posDelim);
                  xaxisData[jj] = Double.parseDouble(tempX) * xFactor;
                  //                       System.out.println(jj+" b "+xaxisData[jj] );
                  attrList = attrList.substring(posDelim + 1, attrList.length())
                      .trim();
                  posDelim = attrList.indexOf(Ydelim);
                }
                if (jj < npoints - 1) {
                  jj++;
                  xaxisData[jj] = Double.parseDouble(attrList) * xFactor;
                  //                     System.out.println(jj+" c "+xaxisData[jj] );
                }
              } while (jj < npoints - 1);
              firstX = xaxisData[0];
              lastX = xaxisData[npoints - 1];
              continuous = true;
            } // end of individual X values
          } // end of X array
          //          System.out.println("finished with X");
        } else if (tmpstr.equals("yaxis")) {
          if (attrList.contains("multipliertodata"))
            yFactor = Double.parseDouble(getAttrValue("multiplierToData"));
          nextTag();
          tmpstr = getTagName();
          attrList = getAttributeList();
          if (tmpstr.equals("array")) {
            yaxisUnit = getAttrValue("units");
            Integer pos = Integer.valueOf(yaxisUnit.indexOf(":"));
            yaxisUnit = yaxisUnit.substring(pos.intValue() + 1,
                yaxisUnit.length()).toUpperCase();
            if (yaxisUnit.toLowerCase().contains("arbitrary"))
              yaxisUnit = "ARBITRARY UNITS";
            Integer npointsY = Integer.valueOf(getAttrValue("size"));
            if (npoints != npointsY.intValue())
              System.err.println("npoints variation between X and Y arrays");
            yaxisData = new double[npoints];
            Ydelim = getAttrValue("delimeter");
            if (Ydelim.equals(""))
              Ydelim = " ";
            int posDelim = 0;
            int jj = -1;
            String tempY = "";
            attrList = getCharacters();

            // now that we have the full string should tokenise it to then process individual Y values
            // for now using indexOf !!

            do {
              jj++;
              posDelim = attrList.indexOf(Ydelim);
              tempY = attrList.substring(0, posDelim);
              yaxisData[jj] = Double.parseDouble(tempY) * yFactor;
              //             System.out.println(jj+" a "+xaxisData[jj]+" "+yaxisData[jj]);
              attrList = attrList.substring(posDelim + 1, attrList.length())
                  .trim();
              posDelim = attrList.indexOf(Ydelim);
              while (posDelim > 0) {
                jj++;
                tempY = attrList.substring(0, posDelim);
                yaxisData[jj] = Double.parseDouble(tempY) * yFactor;
                //                 System.out.println(jj+" b "+xaxisData[jj]+" "+yaxisData[jj]);
                //                 System.out.println(jj+" "+ tempstr);
                attrList = attrList.substring(posDelim + 1, attrList.length())
                    .trim();
                posDelim = attrList.indexOf(Ydelim);
              }
              if (jj < npoints - 1) {
                jj++;
                yaxisData[jj] = Double.parseDouble(attrList) * yFactor;
                //                 System.out.println(jj+" c "+xaxisData[jj]+" "+yaxisData[jj]);
              }
            } while (jj < npoints - 1);
          }
          firstY = yaxisData[0];
          //           System.out.println(firstY);
          //          System.out.println("finished with Y");
        } // end if not startelement
        specfound = true;
      } // end of hasNext

    } catch (Exception ex) {
      System.err
          .println("error reading SpectrumData section: " + ex.toString());
      errorLog.append("error reading Spectrum Data section\n");
    }

  } // end of processSpectrumData

  /**
   * Process the peakList CML events
   *@throws Exception
   */
  public void processPeaks() throws Exception {
    // don't know how many peaks to expect so set an arbitrary number of 100
    int jj = -1;
    int arbsize = 100;
    xaxisData = new double[arbsize];
    yaxisData = new double[arbsize];

    try {
      while (fer.hasNext() && !tmpEnd.equals("peaklist")) {
        if (nextEvent() != XMLStreamConstants.START_ELEMENT) {
          if (eventType == XMLStreamConstants.END_ELEMENT)
            tmpEnd = getEndTag();
          continue;
        }
        tmpstr = getTagName();
        attrList = getAttributeList();
          if (tmpstr.equals("peak")) {
            if (attrList.contains("xvalue")) {
              xaxisData[++jj] = Double.parseDouble(getAttrValue("xValue"));
            if (attrList.contains("xunits")) {
              xaxisUnit = getAttrValue("xUnits");
              Integer pos = Integer.valueOf(xaxisUnit.indexOf(":"));
              xaxisUnit = xaxisUnit.substring(pos.intValue() + 1,
                  xaxisUnit.length()).toUpperCase();
              if (xaxisUnit.toLowerCase().equals("moverz"))
                xaxisUnit = "M/Z";
            }
            if (attrList.contains("yvalue"))
              yaxisData[jj] = Double.parseDouble(getAttrValue("yValue"));
            if (attrList.contains("yunits")) {
              yaxisUnit = getAttrValue("yUnits");
              Integer pos = Integer.valueOf(yaxisUnit.indexOf(":"));
              yaxisUnit = yaxisUnit.substring(pos.intValue() + 1,
                  yaxisUnit.length()).toUpperCase();
              if (yaxisUnit.toLowerCase().equals("relabundance"))
                yaxisUnit = "RELATIVE ABUNDANCE";
              if (yaxisUnit.toLowerCase().contains("arbitrary"))
                yaxisUnit = "ARBITRARY UNITS";
            }

            // for CML exports from NMRShiftDB there are no Y values or Y units
            // given in the Peaks, just XValues
            // to use the JCAMP-DX plot routines we assign a Yvalue
            // of 50 for every atom referenced

            if (attrList.contains("atomrefs")) {
              StringTokenizer srt = new StringTokenizer(getAttrValue("atomRefs"));
              yaxisData[jj] = 49 * (srt.countTokens());
            }
          } // end of peak
        } // end if not startelement
      } // end of hasNext

      // now that we have X,Y pairs set JCAMP-DX equivalencies
      // FIRSTX, FIRSTY, LASTX, NPOINTS
      // determine if the data is in increasing or decreasing order
      // since a PeakList the data is not continuous

      npoints = jj + 1;
      firstX = xaxisData[0];
      lastX = xaxisData[jj];
      firstY = yaxisData[0];
      increasing = lastX > firstX ? true : false;
      continuous = false;

    } catch (Exception ex) {
      System.err.println("error reading PeakList section: " + ex.toString());
      errorLog.append("error reading Peak List section\n");
    }

  } // end of processPeaks

}
