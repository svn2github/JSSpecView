/* Copyright (c) 2007-2009 The University of the West Indies
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import jspecview.util.Base64;
import jspecview.util.SimpleXmlReader;


/**
 * Representation of a XML Source.
 * @author Craig A.D. Walters
 * @author Prof. Robert J. Lancashire
 */

class AnIMLReader extends XMLReader {

  /**
   * Does the actual work of initializing the XMLSource
   * @param filePath 
   * @param br 
   * @param in an InputStream of the AnIML document
   * @return an instance of a AnIMLSource
   */
  static JDXSource getAniMLInstance(String filePath, BufferedReader br) {
    return (new AnIMLReader(filePath)).getXML(br);
  }
  
  AnIMLReader(String filePath) {
    super(filePath);
  }

   
  private JDXSource getXML(BufferedReader br) {
    try {

      source = new JDXSource(JDXSource.TYPE_SIMPLE, filePath);
      
      getSimpleXmlReader(br);

      reader.nextEvent();

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

    try {
      br.close();
    } catch (IOException e1) {
      //
    }

    return source;
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
    case AML_RESULT:
      inResult = true;
      return true;
    default:
      System.out.println("AnIMLSource not processing tag " + tagNames[tagId]
          + "!");
      // should not be here
      return false;
    }
  }

  @Override
  protected void processEndTag(int tagId) throws Exception {
    switch(tagId) {
    case AML_RESULT:
    case AML_EXPERIMENTSTEPSET:
      inResult = false;
      break;
    }
  }

  private void processAuditTrail() throws Exception {
    if (tagName.equals("user")) {
      reader.qualifiedValue();
    } else if (tagName.equals("timestamp")) {
      reader.qualifiedValue();
    }
  }

  private void processSampleSet() throws Exception {
    if (tagName.equals("sample"))
      samplenum++;
    else if (tagName.equals("parameter")) {
      attrList = reader.getAttrValueLC("name");
      if (attrList.equals("name")) {
        reader.qualifiedValue();
      } else if (attrList.equals("owner")) {
        reader.qualifiedValue();
      } else if (attrList.equals("molecular formula")) {
        molForm = reader.qualifiedValue();
      } else if (attrList.equals("cas registry number")) {
        casRN = reader.qualifiedValue();
      }
    }
  }

  private boolean inResult;
  
  private void processExperimentStepSet() throws Exception {
    System.out.println("AnIML experiment " + tagName);
    
    if (tagName.equals("result")) {
      inResult = true;
      
    } else if (tagName.equals("sampleref")) {
      if (reader.getAttrValueLC("role").contains("samplemeasurement"))
        sampleID = reader.getAttrValue("sampleID");
    } else if (tagName.equals("author")) {
      process(AML_AUTHOR, true);
    } else if (tagName.equals("timestamp")) {
      LongDate = reader.thisValue();
    } else if (tagName.equals("technique")) {
      techname = reader.getAttrValue("name").toUpperCase() + " SPECTRUM";
    } else if (tagName.equals("vectorset") || tagName.equals("seriesset") && inResult) {
      npoints = Integer.parseInt(reader.getAttrValue("length"));
      System.out.println("AnIML No. of points= " + npoints);
      xaxisData = new double[npoints];
      yaxisData = new double[npoints];
    } else if (tagName.equals("vector") || tagName.equals("series") && inResult) {
      String axisLabel = reader.getAttrValue("name");
      String dependency = reader.getAttrValueLC("dependency");
      if (dependency.equals("independent")) {
        xUnits = axisLabel;
        getXValues();
      } else if (dependency.equals("dependent")) {
        yUnits = axisLabel;
        getYValues();
      }
    } else if (tagName.equals("parameter")) {
      if ((attrList = reader.getAttrValueLC("name")).equals("identifier")) {
        title = reader.qualifiedValue();
      } else if (attrList.equals("nucleus")) {
        obNucleus = reader.qualifiedValue();
      } else if (attrList.equals("observefrequency")) {
        StrObFreq = reader.qualifiedValue();
        obFreq = Double.parseDouble(StrObFreq);
      } else if (attrList.equals("referencepoint")) {
        refPoint = Double.parseDouble(reader.qualifiedValue());
      } else if (attrList.equals("sample path length")) {
        pathlength = reader.qualifiedValue();
      } else if (attrList.equals("scanmode")) {
        reader.thisValue(); // ignore?
      } else if (attrList.equals("manufacturer")) {
        vendor = reader.thisValue();
      } else if (attrList.equals("model name")) {
        modelType = reader.thisValue();
      } else if (attrList.equals("resolution")) {
        resolution = reader.qualifiedValue();
      }
    }
  }

  private void getXValues() throws IOException {
    reader.nextTag();
    if (reader.getTagName().equals("autoincrementedvalueset")) {
      reader.nextTag();
      if (reader.getTagName().equals("startvalue"))
        firstX = Double.parseDouble(reader.qualifiedValue());
      nextStartTag();
      if (reader.getTagName().equals("increment"))
        deltaX = Double.parseDouble(reader.qualifiedValue());
    }
    if (!inResult) {
      nextStartTag();
      xUnits = reader.getAttrValue("label");
    }
    increasing = (deltaX > 0 ? true : false);
    continuous = true;
    for (int j = 0; j < npoints; j++)
      xaxisData[j] = firstX + (deltaX * j);
    lastX = xaxisData[npoints - 1];
  }

  private void nextStartTag() throws IOException {
    reader.nextStartTag();
    while (reader.getTagType() == SimpleXmlReader.COMMENT) {
      reader.nextStartTag();
    }
  }

  private void getYValues() throws IOException {
    String vectorType = reader.getAttrValueLC("type");
    if (vectorType.length() == 0)
      vectorType = reader.getAttrValueLC("vectorType");
    reader.nextTag();
    tagName = reader.getTagName();
    if (tagName.equals("individualvalueset")) {
      for (int ii = 0; ii < npoints; ii++)
        yaxisData[ii] = Double.parseDouble(reader.qualifiedValue());
      System.out.println(npoints + " individual Y values now read");
    } else if (tagName.equals("encodedvalueset")) {
      attrList = reader.getCharacters();
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
    reader.nextStartTag();
    tagName = reader.getTagName();
    yUnits = reader.getAttrValue("label");
    firstY = yaxisData[0];
  }

  private void processAuthor() throws IOException {
    if (tagName.equals("name"))
      owner = reader.thisValue();
    else if (tagName.contains("location"))
      origin = reader.thisValue();
  }


}
