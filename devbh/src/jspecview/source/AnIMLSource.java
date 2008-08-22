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
import java.nio.*;
import javax.xml.stream.*;

import jspecview.common.Base64;

/**
 * Representation of a XML Source.
 * @author Craig Walters
 * @author Prof. Robert J. Lancashire
 */

public class AnIMLSource extends XMLSource {

  /**
   * Does the actual work of initializing the XMLSource
   * @param in an InputStream of the AnIML document
   * @return an instance of a AnIMLSource
   */
  public static AnIMLSource getAniMLInstance(InputStream in) {
    return (new AnIMLSource()).getXML(in);
  }

  private AnIMLSource getXML(InputStream in) {
    try {

      getFactory(in);

      nextEvent();

      processXML();

      if (!checkPointCount())
        return null;

      xFactor = 1;
      yFactor = 1;
      populateVariables();

    }

    catch (Exception pe) {

      System.err.println("That file may be empty...");
      errorLog.append("That file may be empty... \n");
    }

    processErrors("anIML");
    return this;
  }

  protected void processXML() throws Exception {
    while (haveMore()) {
      //System.out.println(eventType + " " + XMLStreamConstants.START_ELEMENT);
      if (nextEvent() == XMLStreamConstants.START_ELEMENT) {
        tmpstr = getTagName();
        attrList = getAttributeList();
        System.out.println(tmpstr);
        if (tmpstr.equals("sampleset")) {
          processSample();
        } else if (tmpstr.equals("experimentstepset")) {
          processMeasurement();
        } else if (tmpstr.equals("audittrail")) {
          processAudit();
        }
      }
    }
  }
  /**
   * Process the sample XML events
   *@throws Exception
   */
  public void processSample() throws Exception {
    try {
      while (haveMore() && !tmpEnd.equals("sampleset")) {
        if (nextEvent() != XMLStreamConstants.START_ELEMENT) {
          if (eventType == XMLStreamConstants.END_ELEMENT)
            tmpEnd = getEndTag();
          continue;
        }

        // with no way to distinguish sample from reference until the experiment step
        // this is not correct and values may get overwritten!!

        tmpstr = getTagName();
        if (tmpstr.equals("sample"))
          samplenum++;
        else if (tmpstr.equals("parameter")) {
          attrList = getAttrValueLC("name");
          if (attrList.equals("name")) {
            nextValue();
          } else if (attrList.equals("owner")) {
            nextValue();
          } else if (attrList.equals("molecular formula")) {
            molForm = nextValue();
          } else if (attrList.equals("cas registry number")) {
            casRN = nextValue();
          }
          //  continue as above to collect information like Temp, BP etc
        }
      }
    } catch (Exception ex) {
      System.err.println("error reading Sample section");
      errorLog.append("error reading Sample section\n");
    }
  } // end of processSample()

  /**
   * Process the ExperimentStepSet XML events
   *@throws Exception
   */
public void processMeasurement() throws Exception {
    try {
      while (haveMore() && !tmpEnd.equals("experimentstepset")) {
        if (nextEvent()!= XMLStreamConstants.START_ELEMENT) {
          if (eventType == XMLStreamConstants.END_ELEMENT)
            tmpEnd = getEndTag();
          continue;
        }
        tmpstr = getTagName();
        if (tmpstr.equals("sampleref")) {
          if (getFullAttribute("role").contains("samplemeasurement"))
            sampleID = getAttrValue("sampleID");
        } else if (tmpstr.equals("author")) {
          
          
          while (!tmpEnd.equals("author")) {                        
            if (nextEvent()!= XMLStreamConstants.START_ELEMENT) {
              if (eventType == XMLStreamConstants.END_ELEMENT) {
                tmpEnd = getEndTag();
              }
              continue;
            }
            tmpstr = getTagName();
            if (tmpstr.equals("name"))
              owner = thisValue();
            else if (tmpstr.contains("location"))
              origin = thisValue();
          } 
        } else if (tmpstr.equals("timestamp")) {
          LongDate = thisValue();
        } else if (tmpstr.equals("technique")) {
          techname = getAttrValue("name").toUpperCase() + " SPECTRUM";
        } else if (tmpstr.equals("vectorset")) {
          npoints = Integer.parseInt(getAttrValue("length"));
          System.out.println("AnIML No. of points= " + npoints);
          xaxisData = new double[npoints];
          yaxisData = new double[npoints];
        } else if (tmpstr.equals("vector")) {
          String axisLabel = getAttrValue("name");
          String dependency = getAttrValueLC("dependency");
          String vectorType = getAttrValueLC("type");
          if (vectorType.length() == 0)
            vectorType = getAttrValueLC("vectorType");

          if (dependency.equals("independent")) {
            xaxisLabel = axisLabel;
            nextTag();
            if (getTagName().equals("autoincrementedvalueset")) {
              nextTag();
              if (getTagName().equals("startvalue"))
                firstX = Double.parseDouble(nextValue());
              nextStartTag();
              if (getTagName().equals("increment"))
                deltaX = Double.parseDouble(nextValue());
            }
            nextStartTag();
            xaxisUnit = getAttrValue("label");
            increasing = (deltaX > 0 ? true : false);
            continuous = true;
            for (int j = 0; j < npoints; j++)
              xaxisData[j] = firstX + (deltaX * j);
            lastX = xaxisData[npoints - 1];
          } else if (dependency.equals("dependent")) {
            yaxisLabel = axisLabel;
            nextTag();
            tmpstr = getTagName();
            if (tmpstr.equals("individualvalueset")) {
              for (int ii = 0; ii < npoints; ii++, nextTag())
                yaxisData[ii] = Double.parseDouble(nextValue());
              System.out.println(npoints + " individual Y values now read");
            } else if (tmpstr.equals("encodedvalueset")) {
              attrList = getCharacters();
              byte[] dataArray = Base64.decodeBase64(attrList);
              int ij = 0;
              if (dataArray.length != 0) {
                ByteBuffer byte_buffer = ByteBuffer.wrap(dataArray).order(
                    ByteOrder.LITTLE_ENDIAN);
                // float64
                if (vectorType.equals("float64")) {
                  DoubleBuffer double_buffer = byte_buffer.asDoubleBuffer();
                  for (ij = 0; double_buffer.remaining() > 0; ij++)
                    yaxisData[ij] = double_buffer.get();
                }
                // float32
                else if (vectorType.equals("float32")) {
                  FloatBuffer float_buffer = byte_buffer.asFloatBuffer();
                  for (ij = 0; float_buffer.remaining() > 0; ij++)
                    yaxisData[ij] = float_buffer.get();
                }
              }
            } // end of encoded Y values

            nextStartTag();
            tmpstr = getTagName();
            yaxisUnit = getAttrValue("label");

          } // end of Y information
          firstY = yaxisData[0];
        } else if (tmpstr.equals("parameter")) {
          if ((attrList = getAttrValueLC("name")).equals("identifier")) {
            title = nextValue();
          } else if (attrList.equals("nucleus")) {
            obNucleus = nextValue();
          } else if (attrList.equals("observefrequency")) {
            StrObFreq = nextValue();
            obFreq = Double.parseDouble(StrObFreq);
          } else if (attrList.equals("referencepoint")) {
            refPoint = Double.parseDouble(nextValue());
          } else if (attrList.equals("sample path length")) {
            pathlength = nextValue();
          } else if (attrList.equals("scanmode")) {
            thisValue(); // ignore?
          } else if (attrList.equals("manufacturer")) {
            vendor = thisValue();
          } else if (attrList.equals("model name")) {
            modelType = thisValue();
          } else if (attrList.equals("resolution")) {
            resolution = nextValue();
          }
        }
      }
    } catch (Exception ex) {
      System.err.println("error reading ExperimentStepSet section"
          + ex.toString());
      errorLog.append("error reading ExperimentStepSet section\n");
    }

  } // end of processMeasurement

  /**
   * Process the audit XML events
   *@throws Exception
   */
  public void processAudit() throws Exception {
    try {
      while (haveMore() && !tmpEnd.equals("audittrail")) {
        if (nextEvent()!= XMLStreamConstants.START_ELEMENT) {
          if (eventType == XMLStreamConstants.END_ELEMENT)
            tmpEnd = getEndTag();
          continue;
        }
        tmpstr = getTagName();
        if (tmpstr.equals("user")) {
          nextValue();
        } else if (tmpstr.equals("timestamp")) {
          nextValue();
        }
      }
    } catch (Exception ex) {
      System.err.println("error reading Audit section");
      errorLog.append("error reading Audit section\n");
    }
  }
}

