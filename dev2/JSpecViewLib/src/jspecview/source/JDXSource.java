/* Copyright (c) 2002-2011 The University of the West Indies
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import jspecview.common.JDXObject;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSpecViewUtils;
import jspecview.common.PeakInfo;
import jspecview.exception.JSpecViewException;
import jspecview.util.FileManager;
import jspecview.util.Parser;

/**
 * <code>JDXSource</code> is representation of all the data in the JCAMP-DX file
 * or source. Note: All Jdx Source are viewed as having a set of Spectra
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 */
public abstract class JDXSource extends JDXObject {

  protected final static String ERROR_SEPARATOR = "________________________________________________________";

  public boolean isCompoundSource = false;
  
  /**
   * The labels for various tabular data
   */
  public static final String[] TABULAR_DATA_LABELS = { "##XYDATA",
      "##XYPOINTS", "##PEAKTABLE", "##DATATABLE", "##PEAKASSIGNMENTS" };

  /**
   * The variable list for the tabular data labels
   */
  public static final String[][] VAR_LIST_TABLE = {
      { "PEAKTABLE", "XYDATA", "XYPOINTS" },
      { "(XY..XY)", "(X++(Y..Y))", "(XY..XY)" } };

  // Table of header variables specific to the jdx source
  protected Map<String, String> headerTable;

  // List of JDXSpectra
  protected Vector<JDXSpectrum> jdxSpectra;

  // Source Listing
  protected String sourceListing;

  // Errors
  protected String errors;

  /**
   * Constructor
   */
  public JDXSource() {
    headerTable = new HashMap<String, String>();
    jdxSpectra = new Vector<JDXSpectrum>();
  }

  /**
   * Returns the Spectrum at a given index in the list
   * 
   * @param index
   *        the spectrum index
   * @return the Spectrum at a given index in the list
   */
  public JDXSpectrum getJDXSpectrum(int index) {
    return jdxSpectra.size() <= index ? null : (JDXSpectrum) jdxSpectra
        .elementAt(index);
  }

  private final static int TYPE_UNKNOWN = -1;
  /** Indicates a Simple Source */
  private final static int TYPE_SIMPLE = 0;
  /** Indicates a Block Source */
  private final static int TYPE_BLOCK = 1;
  /** Indicates a Ntuple Source */
  private final static int TYPE_NTUPLE = 2;

  private static JDXSource getXMLSource(String source) {
    String xmlType = source.substring(0, 400).toLowerCase();

    if (xmlType.contains("<animl")) {
      return AnIMLSource.getAniMLInstance(new ByteArrayInputStream(source
          .getBytes()));
    } else if (xmlType.contains("xml-cml")) {
      return CMLSource.getCMLInstance(new ByteArrayInputStream(source
          .getBytes()));
    }
    return null;
  }

  public static JDXSource createJDXSource(String sourceContents,
                                          String filePath,
                                          URL appletDocumentBase)
      throws IOException, JSpecViewException {
    InputStream in = null;
    System.out.println("createJDXSource " + filePath + " " + sourceContents
        + " " + appletDocumentBase);

    if (filePath != null) {
      in = FileManager.getInputStream(filePath, true, appletDocumentBase);
      sourceContents = getContentFromInputStream(in);

      JDXSource xmlSource = getXMLSource(sourceContents);
      if (xmlSource != null) {
        return xmlSource;
      }
    }

    try {
      switch (determineJDXSourceType(sourceContents)) {
      case TYPE_SIMPLE:
        return SimpleSource.getInstance(sourceContents);
      case TYPE_BLOCK:
        return BlockSource.getInstance(sourceContents);
      case TYPE_NTUPLE:
        return NTupleSource.getInstance(sourceContents);
        // return RestrictedNTupleSource.getInstance(sourceContents, 128);
      case TYPE_UNKNOWN:
        throw new JSpecViewException("JDX Source Type not Recognized");
      default:
        throw new JSpecViewException("Unknown or unrecognised JCAMP-DX format");
      }
    } catch (JSpecViewException e) {
      throw new JSpecViewException("Error reading JDX format: "
          + e.getMessage());
    }
  }

  public static JDXSource createJDXSource(InputStream in) throws IOException,
      JSpecViewException {

    String sourceContents = getContentFromInputStream(in);

    return createJDXSource(sourceContents, null, null);
  }

  public static JDXSource createJDXSource(String sourceContents)
      throws IOException, JSpecViewException {

    return createJDXSource(sourceContents, null, null);
  }

  private static String getContentFromInputStream(InputStream in)
      throws IOException {
    StringBuffer sb = new StringBuffer();
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    String line;
    while ((line = br.readLine()) != null)
      sb.append(line).append("\n");
    br.close();
    return sb.toString();
  }

  /**
   * Determines the type of JDX Source
   * 
   * @param sourceContents
   *        the contents of the source
   * @return the JDX source type
   */
  private static int determineJDXSourceType(String sourceContents) {
    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(sourceContents);
    String label;
    while (t.hasMoreTokens()) {
      t.nextToken();
      label = JSpecViewUtils.cleanLabel(t.label);
      if (label.equals("##DATATYPE") && t.value.toUpperCase().equals("LINK")) {

        return TYPE_BLOCK;
      }
      if (label.equals("##DATACLASS")
          && t.value.toUpperCase().equals("NTUPLES")) {
        return TYPE_NTUPLE;
      }
      Arrays.sort(JDXSource.TABULAR_DATA_LABELS);
      if (Arrays.binarySearch(JDXSource.TABULAR_DATA_LABELS, label) > 0)
        return TYPE_SIMPLE;
    }
    return TYPE_UNKNOWN;
  }

  /**
   * Adds a Spectrum to the list
   * 
   * @param spectrum
   *        the spectrum to be added
   */
  public void addJDXSpectrum(JDXSpectrum spectrum) {
    jdxSpectra.addElement(spectrum);
  }

  /**
   * Returns the number of Spectra in this Source
   * 
   * @return the number of Spectra in this Source
   */
  public int getNumberOfSpectra() {
    return jdxSpectra.size();
  }

  /**
   * Returns the Vector of Spectra
   * 
   * @return the Vector of Spectra
   */
  public Vector<JDXSpectrum> getSpectra() {
    return jdxSpectra;
  }

  /**
   * Returns the header table of the JDXSource
   * 
   * @return the header table of the JDXSource
   */
  public Map<String, String> getHeaderTable() {
    return headerTable;
  }

  /**
   * Sets the headertable for this Source
   * 
   * @param table
   *        the header table
   */
  public void setHeaderTable(Map<String, String> table) {
    headerTable = table;
  }

  /**
   * Returns the error log for this source
   * 
   * @return the error log for this source
   */
  public String getErrorLog() {
    return errors;
  }

  /**
   * Sets the error log for this source
   * 
   * @param errors
   *        error log for this source
   */
  public void setErrorLog(String errors) {
    this.errors = errors;
  }

  private int index;

  public ArrayList<PeakInfo> readPeakList(String peakList) throws Exception {
    ArrayList<PeakInfo> peakData = new ArrayList<PeakInfo>();
    BufferedReader reader = new BufferedReader(new StringReader(peakList));
    String line = discardLinesUntilContains(reader, "<Peaks");
    String type = Parser.getQuotedAttribute(line, "type");
    PeakInfo peak;
    while ((line = reader.readLine()) != null
        && !(line = line.trim()).startsWith("</Peaks>")) {
      if (line.startsWith("<PeakData")) {

        double xMax = Double.parseDouble(Parser
            .getQuotedAttribute(line, "xMax"));
        double xMin = Double.parseDouble(Parser
            .getQuotedAttribute(line, "xMin"));
        double yMax = Double.parseDouble(Parser
            .getQuotedAttribute(line, "yMax"));
        double yMin = Double.parseDouble(Parser
            .getQuotedAttribute(line, "yMin"));
        peak = new PeakInfo();
        peak.setXMax(xMax);
        peak.setXMin(xMin);
        peak.setYMax(yMax);
        peak.setYMin(yMin);
        peak.setStringInfo("<PeakData file=\"\" index=\"" + (++index)
            + "\" type=\"" + type + "\" " + line.substring(9).trim());
        peakData.add(peak);
      }
    }

    return peakData;
  }

  private static String discardLinesUntilContains(BufferedReader reader,
                                             String containsMatch)
      throws Exception {
    String line = reader.readLine();
    while (line != null && line.indexOf(containsMatch) < 0) {
    }
    return line;
  }

  protected static boolean checkCommon(JDXSource source, JDXObject jdxObject, String label,
                                       String value, StringBuffer errorLog, 
                                       HashMap<String, String> table) {
    if (label.equals("##TITLE")) {
      jdxObject.title = (JSpecViewUtils.obscure || value == null
          || value.equals("") ? "Unknown" : value);
      return true;
    }
    if (label.equals("##JCAMPDX")) {
      jdxObject.jcampdx = value;
      float version = Parser.parseFloat(value);
      if (version >= 6.0 || Float.isNaN(version)) {
        if (errorLog != null)
          errorLog
              .append("Warning: JCAMP-DX version may not be fully supported: "
                  + value + "\n");
      }
      return true;
    }

    if (label.equals("##ORIGIN")) {
      jdxObject.origin = (value != null && !value.equals("") ? value : "Unknown");
      return true;
    }

    if (label.equals("##OWNER")) {
      jdxObject.owner = (value != null && !value.equals("") ? value : "Unknown");
      return true;
    }

    if (label.equals("##DATATYPE")) {
      jdxObject.dataType = value;
      return true;
    }

    if (label.equals("##LONGDATE")) {
      jdxObject.longDate = value;
      return true;
    }

    if (label.equals("##DATE")) {
      jdxObject.date = value;
      return true;
    }

    if (label.equals("##TIME")) {
      jdxObject.time = value;
      return true;
    }

    if (label.equals("##PATHLENGTH")) {
      jdxObject.pathlength = value;
      return true;
    }

    if (label.equals("##$PEAKS")) {
      try {
        ((JDXSpectrum) jdxObject).setPeakList(source.readPeakList(value));
      } catch (Exception e) {

      }
      return true;
    }

    if(label.equals("##XLABEL")){
      jdxObject.xUnits = value;
      return true;
    }

    if(label.equals("##XUNITS") && jdxObject.xUnits.equals("")){
      jdxObject.xUnits = (value != null && !value.equals("") ? value : "Arbitrary Units");
      return true;
    }

    if(label.equals("##YLABEL")){
      jdxObject.yUnits = value;
      return true;
    }

    if(label.equals("##YUNITS") && jdxObject.yUnits.equals("")){
      jdxObject.yUnits = (value != null && !value.equals("") ? value : "Arbitrary Units");
      return true;
    }

    if(label.equals("##XFACTOR")){
      jdxObject.xFactor = Double.parseDouble(value);
      return true;
    }

    if(label.equals("##YFACTOR")){
      jdxObject.yFactor = Double.parseDouble(value);
      return true;
    }

    if (label.equals("##FIRSTX")) {
      jdxObject.firstX = Double.parseDouble(value);
      return true;
    }

    if (label.equals("##LASTX")) {
      jdxObject.lastX = Double.parseDouble(value);
      return true;
    }

    if (label.equals("##NPOINTS")) {
      jdxObject.nPoints = Integer.parseInt(value);
      return true;
    }

    if(label.equals("##MINX") ||
        label.equals("##MINY") ||
        label.equals("##MAXX") ||
        label.equals("##MAXY") ||
        label.equals("##FIRSTY")||
        label.equals("##DELTAX") ||
        label.equals("##DATACLASS"))
        return true;

    if (label.equals("##$OFFSET") && jdxObject.shiftRefType != 0) {
      jdxObject.offset = Double.parseDouble(value);
      // bruker doesn't need dataPointNum
      jdxObject.dataPointNum = 1;
      // bruker type
      jdxObject.shiftRefType = 1;
      return true;
    }

    if ((label.equals("##$REFERENCEPOINT")) && (jdxObject.shiftRefType != 0)) {
      jdxObject.offset = Double.parseDouble(value);
      // varian doesn't need dataPointNum
      jdxObject.dataPointNum = 1;
      // varian type
      jdxObject.shiftRefType = 2;
    }

    else if (label.equals("##.SHIFTREFERENCE")) {
      if (!(jdxObject.dataType.toUpperCase().contains("SPECTRUM")))
        return true;
      StringTokenizer srt = new StringTokenizer(value, ",");
      if (srt.countTokens() != 4)
        return true;
      try {
        srt.nextToken();
        srt.nextToken();
        jdxObject.dataPointNum = Integer.parseInt(srt.nextToken().trim());
        jdxObject.offset = Double.parseDouble(srt.nextToken().trim());
      } catch (NumberFormatException nfe) {
        return true;
      } catch (NoSuchElementException nsee) {
        return true;
      }

      if (jdxObject.dataPointNum <= 0)
        jdxObject.dataPointNum = 1;
      jdxObject.shiftRefType = 0;
      return true;
    }

    if (label.equals("##.OBSERVEFREQUENCY")) {
      jdxObject.obFreq = Double.parseDouble(value);
      table.put(label, value);
      return true;
    }
    
    return false;
  }


}
