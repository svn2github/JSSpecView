/* Copyright (c) 2007-2008 The University of the West Indies
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

import java.io.InputStream;
import jspecview.util.Parser;

/**
 * Representation of a XML Source.
 * @author Craig Walters
 * @author Prof. Robert J. Lancashire
 */

public class CMLSource extends XMLSource {
  private boolean specfound = false;
  private int nPeakData = -1;

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

      getSimpleXmlReader(in);
      processXML(CML_0, CML_1);

      if (!checkPointCount())
        return null;

      populateVariables();

    } catch (Exception pe) {
      //System.out.println(getBufferData());
      System.err.println("Error: " + pe.getMessage());
    }

    processErrors("CML");
    return this;
  }

  String Ydelim = "";

  /**
   * Process the XML events. The while() loop here
   * iterates through XML tags until a </xxxx> tag
   * is found.
   *
   *
   *
   * @param tagId
   * @return true to continue with encapsulated tags
   * @throws Exception
   */
  @Override
  protected boolean processTag(int tagId) throws Exception {
    //System.out.println(tagId + " " + tagNames[tagId]);
    switch (tagId) {
    case CML_SPECTRUM:
      processSpectrum();
      return false; // once only
    case CML_SPECTRUMDATA:
      processSpectrumData();
      return true;
    case CML_PEAKLIST:
      processPeaks();
      return false; // only once
    case CML_SAMPLE:
      processSample();
      return true;
    case CML_METADATALIST:
      processMetadataList();
      return true;
    case CML_CONDITIONLIST:
      processConditionList();
      return true;
    case CML_PARAMETERLIST:
      processParameterList();
      return true;
    case CML_PEAKLIST2:
      // via CML_PEAKLIST
      processPeakList();
      return true;
    default:
      System.out.println("CMLSource not processing tag " + tagNames[tagId] + "!");
      // should not get here
      return false;
    }
  }

  private void processSpectrum() throws Exception {
    // title OR id here
    if (attrList.contains("title"))
      title = reader.getAttrValue("title");
    else if (attrList.contains("id"))
      title = reader.getAttrValue("id");

    // "type" is a required tag
    if (attrList.contains("type"))
      techname = reader.getAttrValue("type").toUpperCase() + " SPECTRUM";
  }

  /**
   * Process the metadata CML events
   *@throws Exception
   */
  private void processMetadataList() throws Exception {
    if (tagName.equals("metadata")) {
      tagName = reader.getAttrValueLC("name");
      if (tagName.contains(":origin")) {
        if (attrList.contains("content"))
          origin = reader.getAttrValue("content");
        else
          origin = reader.thisValue();
      } else if (tagName.contains(":owner")) {
        if (attrList.contains("content"))
          owner = reader.getAttrValue("content");
        else
          owner = reader.thisValue();
      }
    }
  }

  /**
   * Process the parameter CML events
   *@throws Exception
   */
  private void processParameterList() throws Exception {
    if (tagName.equals("parameter")) {
      String title = reader.getAttrValueLC("title");
      if (title.equals("nmr.observe frequency")) {
        StrObFreq = reader.qualifiedValue();
        obFreq = Double.parseDouble(StrObFreq);
      } else if (title.equals("nmr.observe nucleus")) {
        obNucleus = reader.getAttrValue("value");
      } else if (title.equals("spectrometer/data system")) {
        modelType = reader.getAttrValue("value");
      } else if (title.equals("resolution")) {
        resolution = reader.qualifiedValue();
      }
    }
  }

  /**
   * Process the ConditionList CML events (found in NMRShiftDB)
   *@throws Exception
   */
  private void processConditionList() throws Exception {
    if (tagName.equals("scalar")) {
      String dictRef = reader.getAttrValueLC("dictRef");
      if (dictRef.contains(":field")) {
        StrObFreq = reader.thisValue();
        if (StrObFreq.charAt(0) > 47
            && StrObFreq.charAt(0) < 58)
          obFreq = Double.parseDouble(StrObFreq);
      }
    }
  }

  /**
   * Process the sample CML events
   *@throws Exception
   */
  private void processSample() throws Exception {
    if (tagName.equals("formula")) {
      if (attrList.contains("concise"))
        molForm = reader.getAttrValue("concise");
      else if (attrList.contains("inline"))
        molForm = reader.getAttrValue("inline");
    } else if (tagName.equals("name")) {
      casName = reader.thisValue();
    }
  }

  /**
   * Process the spectrumdata CML events
   *@throws Exception
   */
  private void processSpectrumData() throws Exception {
    if (tagName.equals("xaxis")) {
      if (attrList.contains("multipliertodata"))
        xFactor = Double.parseDouble(reader.getAttrValue("multiplierToData"));
      reader.nextTag();
      tagName = reader.getTagName();
      attrList = reader.getAttributeList();
      if (tagName.equals("array")) {
        xaxisUnit = reader.getAttrValue("units");
        Integer pos = Integer.valueOf(xaxisUnit.indexOf(":"));
        xaxisUnit = xaxisUnit.substring(pos.intValue() + 1, xaxisUnit.length())
            .toUpperCase();
        if (xaxisUnit.toLowerCase().equals("cm-1"))
          xaxisUnit = "1/CM";
        else if (xaxisUnit.toLowerCase().equals("nm"))
          xaxisUnit = "NANOMETERS";
        npoints = Integer.parseInt(reader.getAttrValue("size"));
        xaxisData = new double[npoints];
        if (attrList.contains("start")) {
          firstX = Double.parseDouble(reader.getAttrValue("start"));
          lastX = Double.parseDouble(reader.getAttrValue("end"));
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
          attrList = reader.getCharacters().replace('\n', ' ').replace('\r', ' ')
              .trim();

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
    } else if (tagName.equals("yaxis")) {
      if (attrList.contains("multipliertodata"))
        yFactor = Double.parseDouble(reader.getAttrValue("multiplierToData"));
      reader.nextTag();
      tagName = reader.getTagName();
      attrList = reader.getAttributeList();
      if (tagName.equals("array")) {
        yaxisUnit = reader.getAttrValue("units");
        Integer pos = Integer.valueOf(yaxisUnit.indexOf(":"));
        yaxisUnit = yaxisUnit.substring(pos.intValue() + 1, yaxisUnit.length())
            .toUpperCase();
        if (yaxisUnit.toLowerCase().contains("arbitrary"))
          yaxisUnit = "ARBITRARY UNITS";
        Integer npointsY = Integer.valueOf(reader.getAttrValue("size"));
        if (npoints != npointsY.intValue())
          System.err.println("npoints variation between X and Y arrays");
        yaxisData = new double[npoints];
        Ydelim = reader.getAttrValue("delimeter");
        if (Ydelim.equals(""))
          Ydelim = " ";
        int posDelim = 0;
        int jj = -1;
        String tempY = "";
        attrList = reader.getCharacters().replace('\n', ' ').replace('\r', ' ').trim();

        // now that we have the full string should tokenise it to then process individual Y values
        // for now using indexOf !!

        do {
          jj++;
          posDelim = attrList.indexOf(Ydelim);
          tempY = attrList.substring(0, posDelim);
          yaxisData[jj] = Double.parseDouble(tempY) * yFactor;
          attrList = attrList.substring(posDelim + 1, attrList.length()).trim();
          posDelim = attrList.indexOf(Ydelim);
          while (posDelim > 0) {
            jj++;
            tempY = attrList.substring(0, posDelim);
            yaxisData[jj] = Double.parseDouble(tempY) * yFactor;
            attrList = attrList.substring(posDelim + 1, attrList.length())
                .trim();
            posDelim = attrList.indexOf(Ydelim);
          }
          if (jj < npoints - 1) {
            jj++;
            yaxisData[jj] = Double.parseDouble(attrList) * yFactor;
          }
        } while (jj < npoints - 1);
      }
      firstY = yaxisData[0];
      specfound = true;
    }
  }

  /**
   * Process the peakList CML events
   *@throws Exception
   */
 private void processPeaks() throws Exception {

    // this method is run ONCE

    // if a spectrum is found, ignore a peaklist if present as well
    // since without intervention it is not possible to guess
    // which display is required and the spectrum is probably the
    // more critical ?!?

    if (specfound)
      return;

    // don't know how many peaks to expect so set an arbitrary number of 100
    int arbsize = 100;
    xaxisData = new double[arbsize];
    yaxisData = new double[arbsize];

    process(CML_PEAKLIST2, true);

    // now that we have X,Y pairs set JCAMP-DX equivalencies
    // FIRSTX, FIRSTY, LASTX, NPOINTS
    // determine if the data is in increasing or decreasing order
    // since for a PeakList the data is not continuous

    npoints = nPeakData + 1;
    firstX = xaxisData[0];
    lastX = xaxisData[nPeakData];
    firstY = yaxisData[0];
    increasing = lastX > firstX ? true : false;
    continuous = false;

  } // end of processPeaks

 private void processPeakList() {
    if (tagName.equals("peak")) {
      if (attrList.contains("xvalue")) {
        xaxisData[++nPeakData] = Double.parseDouble(reader.getAttrValue("xValue"));
        if (attrList.contains("xunits")) {
          xaxisUnit = reader.getAttrValue("xUnits");
          Integer pos = Integer.valueOf(xaxisUnit.indexOf(":"));
          xaxisUnit = xaxisUnit.substring(pos.intValue() + 1,
              xaxisUnit.length()).toUpperCase();
          if (xaxisUnit.toLowerCase().equals("moverz"))
            xaxisUnit = "M/Z";
        }
        if (attrList.contains("yvalue"))
          yaxisData[nPeakData] = Double.parseDouble(reader.getAttrValue("yValue"));
        if (attrList.contains("yunits")) {
          yaxisUnit = reader.getAttrValue("yUnits");
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
          String[] tokens = Parser.getTokens(reader.getAttrValue("atomRefs"));
          yaxisData[nPeakData] = 49 * (tokens.length);
        }
      }

    }
  }  // end of processPeakList
}