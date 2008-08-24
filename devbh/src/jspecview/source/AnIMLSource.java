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
//import javax.xml.stream.*;

import jspecview.common.Base64;
import jspecview.util.Logger;

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

      //getFactory(in);

      getSimpleXmlReader(in);

      nextEvent();

      processXML(AML_0, AML_1);

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

  /**
   * Process the audit XML events
   * @param tagId 
   */
  @Override
  protected void process(int tagId) {
    String thisTagName = tagNames[tagId];
    try {
      while (haveMore()) {
        switch (nextEvent()) {
        default:
          continue;
        case SimpleXmlReader.END_ELEMENT:
          if (getEndTag().equals(thisTagName))
            return;
          continue;
        case SimpleXmlReader.START_ELEMENT:
          break;
        }
        tagName = getTagName();
        attrList = getAttributeList();
        switch (tagId) {
        case AML_AUDITTRAIL:
          processAuditTrail();
          break;
        case AML_AUTHOR:
          processAuthor();
          break;
        case AML_EXPERIMENTSTEPSET:
          processExperimentStepSet();
          break;
        case AML_SAMPLESET:
          processSampleSet();
          break;
        }
      }
    } catch (Exception e) {
      String msg = "error reading " + tagName + " section";
      Logger.error(msg);
      errorLog.append(msg + "\n");
    }
  }

  private void processAuthor() throws IOException {
    if (tagName.equals("name"))
      owner = thisValue();
    else if (tagName.contains("location"))
      origin = thisValue();
  }

  private void processAuditTrail() throws Exception {
    if (tagName.equals("user")) {
      nextValue();
    } else if (tagName.equals("timestamp")) {
      nextValue();
    }
  }

  private void processSampleSet() throws Exception {
    if (tagName.equals("sample"))
      samplenum++;
    else if (tagName.equals("parameter")) {
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
    }
  }

  private void processExperimentStepSet() throws Exception {
    if (tagName.equals("sampleref")) {
      if (getFullAttribute("role").contains("samplemeasurement"))
        sampleID = getAttrValue("sampleID");
    } else if (tagName.equals("author")) {
      process(AML_AUTHOR);
    } else if (tagName.equals("timestamp")) {
      LongDate = thisValue();
    } else if (tagName.equals("technique")) {
      techname = getAttrValue("name").toUpperCase() + " SPECTRUM";
    } else if (tagName.equals("vectorset")) {
      npoints = Integer.parseInt(getAttrValue("length"));
      System.out.println("AnIML No. of points= " + npoints);
      xaxisData = new double[npoints];
      yaxisData = new double[npoints];
    } else if (tagName.equals("vector")) {
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
        tagName = getTagName();
        if (tagName.equals("individualvalueset")) {
          for (int ii = 0; ii < npoints; ii++, nextTag())
            yaxisData[ii] = Double.parseDouble(nextValue());
          System.out.println(npoints + " individual Y values now read");
        } else if (tagName.equals("encodedvalueset")) {
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
        tagName = getTagName();
        yaxisUnit = getAttrValue("label");

      } // end of Y information
      firstY = yaxisData[0];
    } else if (tagName.equals("parameter")) {
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
}
