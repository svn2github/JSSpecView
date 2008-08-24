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
//import javax.xml.stream.*;
import java.util.StringTokenizer;

import jspecview.util.Logger;

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

      //getFactory(in);
      getSimpleXmlReader(in);
      if (!checkStart())
        return this;

      processXML(CML_0, CML_1);

      if (!checkPointCount())
        return null;

      populateVariables();

    } catch (Exception pe) {
      //System.out.println(getBufferData());
      System.err.println("Error: " + pe.getMessage());
      errorLog.append("That file may be empty... \n");
    }

    processErrors("CML");
    return this;
  }

  private boolean checkStart() throws Exception {
    if (peek() != SimpleXmlReader.START_ELEMENT) {
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
      setErrorLog(errorLog.toString());
      addJDXSpectrum(null);
      return false;
    }
    return true;
  }

  String Ydelim = "";

  /**
   * Process the audit XML events
   * @param tagId 
   */
  @Override
  protected void process(int tagId) {
    String thisTagName = tagNames[tagId];
    //System.out.println(thisTagName  + " "+ tagId);
    try {
      while (haveMore()) {
        switch (nextEvent()) {
        default:
          continue;
        case SimpleXmlReader.END_ELEMENT:
          //System.out.println("ending " +  getEndTag());
          if (getEndTag().equals(thisTagName))
            return;
          continue;
        case SimpleXmlReader.START_ELEMENT:
          break;
        }
        tagName = getTagName();
        attrList = getAttributeList();
        //System.out.println("  " + tagName + " " + attrList);
        switch (tagId) {
        case CML_SPECTRUM:
          processSpectrum();
          return; // once only
        case CML_SPECTRUMDATA:
          processSpectrumData();
          break;
        case CML_PEAKLIST:
          processPeaks();
          return; // only once
        case CML_SAMPLE:
          processSample();
          break;
        case CML_METADATALIST:
          processMetadataList();
          break;
        case CML_CONDITIONLIST:
          processConditionList();
          break;
        case CML_PARAMETERLIST:
          processParameterList();
          break;
        }
      }
    } catch (Exception e) {
      String msg = "error reading " + tagName + " section";
      Logger.error(msg);
      errorLog.append(msg + "\n");
    }
  }

  private void processSpectrum() throws Exception {
    // title OR id here
    if (attrList.contains("title"))
      title = getAttrValue("title");
    else if (attrList.contains("id"))
      title = getAttrValue("id");

    if (attrList.contains("type"))
      techname = getAttrValue("type").toUpperCase() + " SPECTRUM";
  }

  /**
   * Process the metadata CML events
   *@throws Exception
   */
  private void processMetadataList() throws Exception {
    if (tagName.equals("metadata")) {
      tagName = getAttrValueLC("name");
      if (tagName.contains(":origin")) {
        if (attrList.contains("content"))
          origin = getAttrValue("content");
        else
          origin = thisValue();
      } else if (tagName.contains(":owner")) {
        if (attrList.contains("content"))
          owner = getAttrValue("content");
        else
          owner = thisValue();
      }
    }
  }

  /**
   * Process the parameter CML events
   *@throws Exception
   */
  private void processParameterList() throws Exception {
    if (tagName.equals("parameter")) {
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

  /**
   * Process the ConditionList CML events (found in NMRShiftDB)
   *@throws Exception
   */
  private void processConditionList() throws Exception {
    if (tagName.equals("scalar")) {
      String dictRef = getAttrValueLC("dictRef");
      if (dictRef.contains(":field")) {
        StrObFreq = thisValue();
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
        molForm = getAttrValue("concise");
      else if (attrList.contains("inline"))
        molForm = getAttrValue("inline");
    } else if (tagName.equals("name")) {
      casName = thisValue();
    }
  }

  /**
   * Process the spectrumdata CML events
   *@throws Exception
   */
  private void processSpectrumData() throws Exception {
    if (tagName.equals("xaxis")) {
      if (attrList.contains("multipliertodata"))
        xFactor = Double.parseDouble(getAttrValue("multiplierToData"));
      nextTag();
      tagName = getTagName();
      attrList = getAttributeList();
      if (tagName.equals("array")) {
        xaxisUnit = getAttrValue("units");
        Integer pos = Integer.valueOf(xaxisUnit.indexOf(":"));
        xaxisUnit = xaxisUnit.substring(pos.intValue() + 1, xaxisUnit.length())
            .toUpperCase();
        if (xaxisUnit.toLowerCase().equals("cm-1"))
          xaxisUnit = "1/CM";
        else if (xaxisUnit.toLowerCase().equals("nm"))
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
          attrList = getCharacters().replace('\n', ' ').replace('\r', ' ')
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
        yFactor = Double.parseDouble(getAttrValue("multiplierToData"));
      nextTag();
      tagName = getTagName();
      attrList = getAttributeList();
      if (tagName.equals("array")) {
        yaxisUnit = getAttrValue("units");
        Integer pos = Integer.valueOf(yaxisUnit.indexOf(":"));
        yaxisUnit = yaxisUnit.substring(pos.intValue() + 1, yaxisUnit.length())
            .toUpperCase();
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
        attrList = getCharacters().replace('\n', ' ').replace('\r', ' ').trim();

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

  int nPeakData;

  /**
   * Process the peakList CML events
   *@throws Exception
   */
  private void processPeaks() throws Exception {
    
    // this method is run ONCE
    
    if (!specfound)
      return; // really????

    // don't know how many peaks to expect so set an arbitrary number of 100
    int nPeakData = -1;
    int arbsize = 100;
    xaxisData = new double[arbsize];
    yaxisData = new double[arbsize];

    process(CML_PEAKLIST2);

    // now that we have X,Y pairs set JCAMP-DX equivalencies
    // FIRSTX, FIRSTY, LASTX, NPOINTS
    // determine if the data is in increasing or decreasing order
    // since a PeakList the data is not continuous 

    npoints = nPeakData + 1;
    firstX = xaxisData[0];
    lastX = xaxisData[nPeakData];
    firstY = yaxisData[0];
    increasing = lastX > firstX ? true : false;
    continuous = false;

  } // end of processPeaks

  void processPeakList() {
    if (tagName.equals("peak")) {
      if (attrList.contains("xvalue")) {
        xaxisData[++nPeakData] = Double.parseDouble(getAttrValue("xValue"));
        if (attrList.contains("xunits")) {
          xaxisUnit = getAttrValue("xUnits");
          Integer pos = Integer.valueOf(xaxisUnit.indexOf(":"));
          xaxisUnit = xaxisUnit.substring(pos.intValue() + 1,
              xaxisUnit.length()).toUpperCase();
          if (xaxisUnit.toLowerCase().equals("moverz"))
            xaxisUnit = "M/Z";
        }
        if (attrList.contains("yvalue"))
          yaxisData[nPeakData] = Double.parseDouble(getAttrValue("yValue"));
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
          yaxisData[nPeakData] = 49 * (srt.countTokens());
        }
      } // end of peak

    }
  }
}
