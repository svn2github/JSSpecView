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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

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
   * Process the XML events. 
   * Invoked for every start tag.
   * 
   * Invoked by the superclass method 
   *   XMLSource.process(tagId, requiresEndTag)
   *   
   * @param tagId 
   * @return true to continue looking for encapsulated tags
   *         false to process once only (no encapsulated tags of interest) 
   * @throws Exception 
   */
  @Override
  protected boolean processTag(int tagId) throws Exception {
    switch (tagId) {
    case AML_AUDITTRAIL:
      processAuditTrail();
      return true;
    case AML_EXPERIMENTSTEPSET:
      processExperimentStepSet();
      return true;
    case AML_SAMPLESET:
      processSampleSet();
      return true;
    case AML_AUTHOR:
      // AML_AUTHOR is processed via AML_EXPERIMENTSTEPSET 
      processAuthor();
      return true;
    default:
      System.out.println("AnIMLSource not processing tag " + tagNames[tagId]
          + "!");
      // should not be here
      return false;
    }
  }

  private void processAuditTrail() throws Exception {
    if (tagName.equals("user")) {
      qualifiedValue();
    } else if (tagName.equals("timestamp")) {
      qualifiedValue();
    }
  }

  private void processSampleSet() throws Exception {
    if (tagName.equals("sample"))
      samplenum++;
    else if (tagName.equals("parameter")) {
      attrList = getAttrValueLC("name");
      if (attrList.equals("name")) {
        qualifiedValue();
      } else if (attrList.equals("owner")) {
        qualifiedValue();
      } else if (attrList.equals("molecular formula")) {
        molForm = qualifiedValue();
      } else if (attrList.equals("cas registry number")) {
        casRN = qualifiedValue();
      }
    }
  }

  private void processExperimentStepSet() throws Exception {
    if (tagName.equals("sampleref")) {
      if (getFullAttribute("role").contains("samplemeasurement"))
        sampleID = getAttrValue("sampleID");
    } else if (tagName.equals("author")) {
      process(AML_AUTHOR, true);
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
      if (dependency.equals("independent")) {
        xaxisLabel = axisLabel;
        getXValues();
      } else if (dependency.equals("dependent")) {
        yaxisLabel = axisLabel;
        getYValues();
      }
    } else if (tagName.equals("parameter")) {
      if ((attrList = getAttrValueLC("name")).equals("identifier")) {
        title = qualifiedValue();
      } else if (attrList.equals("nucleus")) {
        obNucleus = qualifiedValue();
      } else if (attrList.equals("observefrequency")) {
        StrObFreq = qualifiedValue();
        obFreq = Double.parseDouble(StrObFreq);
      } else if (attrList.equals("referencepoint")) {
        refPoint = Double.parseDouble(qualifiedValue());
      } else if (attrList.equals("sample path length")) {
        pathlength = qualifiedValue();
      } else if (attrList.equals("scanmode")) {
        thisValue(); // ignore?
      } else if (attrList.equals("manufacturer")) {
        vendor = thisValue();
      } else if (attrList.equals("model name")) {
        modelType = thisValue();
      } else if (attrList.equals("resolution")) {
        resolution = qualifiedValue();
      }
    }
  }
  
  private void getXValues() throws IOException {
    nextTag();
    if (getTagName().equals("autoincrementedvalueset")) {
      nextTag();
      if (getTagName().equals("startvalue"))
        firstX = Double.parseDouble(qualifiedValue());
      nextStartTag();
      if (getTagName().equals("increment"))
        deltaX = Double.parseDouble(qualifiedValue());
    }
    nextStartTag();
    xaxisUnit = getAttrValue("label");
    increasing = (deltaX > 0 ? true : false);
    continuous = true;
    for (int j = 0; j < npoints; j++)
      xaxisData[j] = firstX + (deltaX * j);
    lastX = xaxisData[npoints - 1];
  }

  private void getYValues() throws IOException {
    String vectorType = getAttrValueLC("type");
    if (vectorType.length() == 0)
      vectorType = getAttrValueLC("vectorType");
    nextTag();
    tagName = getTagName();
    if (tagName.equals("individualvalueset")) {
      for (int ii = 0; ii < npoints; ii++)
        yaxisData[ii] = Double.parseDouble(qualifiedValue());
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
    } 
    nextStartTag();
    tagName = getTagName();
    yaxisUnit = getAttrValue("label");
    firstY = yaxisData[0];
  }

  private void processAuthor() throws IOException {
    if (tagName.equals("name"))
      owner = thisValue();
    else if (tagName.contains("location"))
      origin = thisValue();
  }


}
