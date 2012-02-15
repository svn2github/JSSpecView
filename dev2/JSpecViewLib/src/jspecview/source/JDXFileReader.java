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
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import jspecview.common.JDXDataObject;
import jspecview.common.JDXHeader;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSpecViewUtils;
import jspecview.common.PeakInfo;
import jspecview.exception.JDXSourceException;
import jspecview.exception.JSpecViewException;
import jspecview.util.FileManager;
import jspecview.util.Parser;

/**
 * <code>JDXFileReader</code> reads JDX data, including
 * complex BLOCK files that contain NTUPLE blocks or more BLOCK files
 * BLOCK files containing subblocks are just read linearly, not as a hierarchy (for now)
 * (maybe!)
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 * @author Bob Hanson, hansonr@stolaf.edu
 */
public abstract class JDXFileReader {

  private final static String ERROR_SEPARATOR = "________________________________________________________";

  /**
   * The labels for various tabular data
   */
  private final static String[] TABULAR_DATA_LABELS = { "##XYDATA",
      "##XYPOINTS", "##PEAKTABLE", "##DATATABLE", "##PEAKASSIGNMENTS" };

  static {
    Arrays.sort(TABULAR_DATA_LABELS);
  }

  /**
   * Labels for the exporter
   *
   */
  public final static String[][] VAR_LIST_TABLE = {
      //NOTE: [0] MUST BE ALPHABETICAL ORDER BECAUSE EXPORTER USES BINARY SEARCH
      { "PEAKTABLE", "XYDATA", "XYPOINTS" },
      { "(XY..XY)", "(X++(Y..Y))", "(XY..XY)" } };

  public static JDXSource createJDXSource(InputStream in) throws IOException,
      JSpecViewException {
    return createJDXSource(getContentFromInputStream(in), null, null);
  }

  public static JDXSource createJDXSource(String sourceContents)
      throws IOException, JSpecViewException {
    return createJDXSource(sourceContents, null, null);
  }

  public static JDXSource createJDXSource(String sourceContents,
                                          String filePath,
                                          URL appletDocumentBase)
      throws IOException, JSpecViewException {
    InputStream in = null;
    
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
      case JDXSource.TYPE_SIMPLE:
        return getSimpleInstance(null, null, sourceContents);
      case JDXSource.TYPE_BLOCK:
        return getBlockInstance(null, null, sourceContents, null, null);
      case JDXSource.TYPE_NTUPLE:
        return getNTupleInstance(null, null, sourceContents, null, null);
        // return RestrictedNTupleSource.getInstance(sourceContents, 128);
      case JDXSource.TYPE_UNKNOWN:
        throw new JSpecViewException("JDX Source Type not Recognized");
      default:
        throw new JSpecViewException("Programming error in JDXFileReader!");
      }
    } catch (JSpecViewException e) {
      throw new JSpecViewException("Error reading JDX format: "
          + e.getMessage());
    }
  }

  private static JDXSource getXMLSource(String source) {
    String xmlType = source.substring(0, 400).toLowerCase();

    if (xmlType.contains("<animl")) {
      return AnIMLReader.getAniMLInstance(new ByteArrayInputStream(source
          .getBytes()));
    } else if (xmlType.contains("xml-cml")) {
      return CMLReader.getCMLInstance(new ByteArrayInputStream(source
          .getBytes()));
    }
    return null;
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
    while (t.hasMoreTokens()) {
      t.nextToken();
      String label = JSpecViewUtils.cleanLabel(t.label);
      if (label.equals("##DATATYPE") && t.value.toUpperCase().equals("LINK")) {

        return JDXSource.TYPE_BLOCK;
      }
      if (label.equals("##DATACLASS")
          && t.value.toUpperCase().equals("NTUPLES")) {
        return JDXSource.TYPE_NTUPLE;
      }
      if (Arrays.binarySearch(TABULAR_DATA_LABELS, label) > 0)
        return JDXSource.TYPE_SIMPLE;
    }
    return JDXSource.TYPE_UNKNOWN;
  }

  private static ArrayList<PeakInfo> readPeakList(String peakList, int index) {
    ArrayList<PeakInfo> peakData = new ArrayList<PeakInfo>();
    BufferedReader reader = new BufferedReader(new StringReader(peakList));
    String line;
    try {
      line = discardLinesUntilContains(reader, "<Peaks");
      String type = Parser.getQuotedAttribute(line, "type");
      PeakInfo peak;
      while ((line = reader.readLine()) != null
          && !(line = line.trim()).startsWith("</Peaks>")) {
        if (line.startsWith("<PeakData")) {

          double xMax = Double.parseDouble(Parser.getQuotedAttribute(line,
              "xMax"));
          double xMin = Double.parseDouble(Parser.getQuotedAttribute(line,
              "xMin"));
          double yMax = Double.parseDouble(Parser.getQuotedAttribute(line,
              "yMax"));
          double yMin = Double.parseDouble(Parser.getQuotedAttribute(line,
              "yMin"));
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
    } catch (Exception e) {
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

  private static boolean readDataLabel(JDXDataObject spectrum, String label,
                                       String value, StringBuffer errorLog,
                                       Map<String, String> table) {

    if (readHeaderLabel(spectrum, label, value, errorLog))
      return true;

    //    if (label.equals("##PATHLENGTH")) {
    //      jdxObject.pathlength = value;
    //      return true;
    //    }

    // NOTE: returning TRUE for these means they are 
    // not included in the header map -- is that what we want?

    if (label.equals("##MINX") || label.equals("##MINY")
        || label.equals("##MAXX") || label.equals("##MAXY")
        || label.equals("##FIRSTY") || label.equals("##DELTAX")
        || label.equals("##DATACLASS"))
      return true;

    if (label.equals("##FIRSTX")) {
      spectrum.fileFirstX = Double.parseDouble(value);
      return true;
    }

    if (label.equals("##LASTX")) {
      spectrum.fileLastX = Double.parseDouble(value);
      return true;
    }

    if (label.equals("##NPOINTS")) {
      spectrum.nPointsFile = Integer.parseInt(value);
      return true;
    }

    if (label.equals("##XFACTOR")) {
      spectrum.xFactor = Double.parseDouble(value);
      return true;
    }

    if (label.equals("##YFACTOR")) {
      spectrum.yFactor = Double.parseDouble(value);
      return true;
    }

    if (label.equals("##XLABEL")) {
      spectrum.xUnits = value;
      return true;
    }

    if (label.equals("##XUNITS") && spectrum.xUnits.equals("")) {
      spectrum.xUnits = (value != null && !value.equals("") ? value
          : "Arbitrary Units");
      return true;
    }

    if (label.equals("##YLABEL")) {
      spectrum.yUnits = value;
      return true;
    }

    if (label.equals("##YUNITS") && spectrum.yUnits.equals("")) {
      spectrum.yUnits = (value != null && !value.equals("") ? value
          : "Arbitrary Units");
      return true;
    }

    // NMR variations: need observedFreq, offset, dataPointNum, and shiftRefType 

    if (label.equals("##.OBSERVEFREQUENCY")) {
      spectrum.observedFreq = Double.parseDouble(value);
      table.put(label, value);
      return true;
    }

    if (label.equals("##$OFFSET") && spectrum.shiftRefType != 0) {
      spectrum.offset = Double.parseDouble(value);
      // bruker doesn't need dataPointNum
      spectrum.dataPointNum = 1;
      // bruker type
      spectrum.shiftRefType = 1;
      return true;
    }

    if ((label.equals("##$REFERENCEPOINT")) && (spectrum.shiftRefType != 0)) {
      spectrum.offset = Double.parseDouble(value);
      // varian doesn't need dataPointNum
      spectrum.dataPointNum = 1;
      // varian type
      spectrum.shiftRefType = 2;
    }

    else if (label.equals("##.SHIFTREFERENCE")) {
      if (!(spectrum.dataType.toUpperCase().contains("SPECTRUM")))
        return true;
      StringTokenizer srt = new StringTokenizer(value, ",");
      if (srt.countTokens() != 4)
        return true;
      try {
        srt.nextToken();
        srt.nextToken();
        spectrum.dataPointNum = Integer.parseInt(srt.nextToken().trim());
        spectrum.offset = Double.parseDouble(srt.nextToken().trim());
      } catch (NumberFormatException nfe) {
        return true;
      } catch (NoSuchElementException nsee) {
        return true;
      }

      if (spectrum.dataPointNum <= 0)
        spectrum.dataPointNum = 1;
      spectrum.shiftRefType = 0;
      return true;
    }

    return false;
  }

  private static boolean readHeaderLabel(JDXHeader jdxHeader, String label,
                                         String value, StringBuffer errorLog) {
    if (label.equals("##TITLE")) {
      jdxHeader.title = (JSpecViewUtils.obscure || value == null
          || value.equals("") ? "Unknown" : value);
      return true;
    }
    if (label.equals("##JCAMPDX")) {
      jdxHeader.jcampdx = value;
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
      jdxHeader.origin = (value != null && !value.equals("") ? value
          : "Unknown");
      return true;
    }

    if (label.equals("##OWNER")) {
      jdxHeader.owner = (value != null && !value.equals("") ? value : "Unknown");
      return true;
    }

    if (label.equals("##DATATYPE")) {
      jdxHeader.dataType = value;
      return true;
    }

    if (label.equals("##LONGDATE")) {
      jdxHeader.longDate = value;
      return true;
    }

    if (label.equals("##DATE")) {
      jdxHeader.date = value;
      return true;
    }

    if (label.equals("##TIME")) {
      jdxHeader.time = value;
      return true;
    }

    return false;
  }

  //////////////////////////  SIMPLE FILE DATA //////////////////

  /**
   * Does the actual work of initializing the SimpleSource from the the contents
   * of the source
   * 
   * @param source
   *        TODO
   * @param sourceContents
   *        the contents of the source as a String
   * 
   * @return an instance of a SimpleSource
   * @throws JSpecViewException
   */
  private static JDXSource getSimpleInstance(JDXSource source, 
                                             JDXSourceStringTokenizer t, String sourceContents)
      throws JSpecViewException {

    // The SimpleSouce Instance
    if (source == null)
      source = new JDXSource(JDXSource.TYPE_SIMPLE);

    //Calendar now = Calendar.getInstance();
    //SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSS ZZZZ");
    //String currentTime =  formatter.format(now.getTime());

    JDXSpectrum spectrum = new JDXSpectrum();

    // The data Table
    String tabularSpecData = null;

    // Table for header information
    HashMap<String, String> notesLDRTable = new HashMap<String, String>(20);

    if (t == null) 
      t= new JDXSourceStringTokenizer(sourceContents);
    String label = "";
    int tabularDataLabelLineNo = 0;

    StringBuffer errorLog = new StringBuffer();

    while (t.hasMoreTokens() && t.nextToken()
        && !(label = JSpecViewUtils.cleanLabel(t.label)).equals("##END")) {

      if (JDXFileReader.readDataLabel(spectrum, label, t.value, errorLog,
          notesLDRTable))
        continue;

      if (Arrays.binarySearch(JDXFileReader.TABULAR_DATA_LABELS, label) > 0) {
        tabularDataLabelLineNo = t.labelLineNo;
        tabularSpecData = spectrum.getTabularData(label, t.value);
        continue;
      }

      notesLDRTable.put(t.label, t.value);

      if (label.equals("##$PEAKS")) {
        source.peakCount += spectrum.setPeakList(JDXFileReader.readPeakList(
            t.value, source.peakCount));
        continue;
      }

    }

    if (!label.equals("##END"))
      tabularSpecData = null;

    if (!spectrum.processTabularData(tabularSpecData, tabularDataLabelLineNo,
        notesLDRTable, errorLog))
      throw new JDXSourceException("Unable to read Simple Source");

    errorLog.append(JDXFileReader.ERROR_SEPARATOR);

    source.addJDXSpectrum(spectrum);
    return source;
  }

  //////////////////////////  BLOCK FILE DATA //////////////////

  /**
   * Does the work of initializing the BlockSource from the source String
   * 
   * @param sourceContents
   *        contents of the source as a String
   * @param sourceLDRTable
   * @return an instance of a BlockSource
   * @throws JSpecViewException
   */
  private static JDXSource getBlockInstance(
                                            JDXSource source,
                                            JDXSourceStringTokenizer t,
                                            String sourceContents,
                                            HashMap<String, String> sourceLDRTable,
                                            StringBuffer errorLog)
      throws JSpecViewException {

    if (source == null)
      source = new JDXSource(JDXSource.TYPE_BLOCK);

    HashMap<String, String> dataLDRTable;

    String label = "";

    if (sourceLDRTable == null)
      sourceLDRTable = new HashMap<String, String>();

    if (errorLog == null)
      errorLog = new StringBuffer();

    int tabDataLineNo = 0;
    String tabularSpecData = null;

    JDXSpectrum spectrum;

    if (t == null) {
      t = new JDXSourceStringTokenizer(sourceContents);
      // Get the LDRs up to the ##TITLE of the first block
      t.nextToken();
      label = JSpecViewUtils.cleanLabel(t.label);
      if (!label.equals("##TITLE"))
        throw new JSpecViewException("Error Reading Source");
      readHeaderLabel(source, label, t.value, errorLog);
      while (t.hasMoreTokens() && t.nextToken()
          && !(label = JSpecViewUtils.cleanLabel(t.label)).equals("##TITLE"))
        if (!readHeaderLabel(source, label, t.value, errorLog))
          sourceLDRTable.put(t.label, t.value);
      source.setHeaderTable(sourceLDRTable);
    } else {
      while (t.hasMoreTokens() && t.nextToken()
          && !(label = JSpecViewUtils.cleanLabel(t.label)).equals("##TITLE")) {
        // ignore rest of link when nested block (at least for now)
      }
    }

    // If ##TITLE not found throw Exception
    if (!label.equals("##TITLE"))
      throw new JSpecViewException("Unable to Read Block Source");

    spectrum = new JDXSpectrum();
    dataLDRTable = new HashMap<String, String>();

    readDataLabel(spectrum, label, t.value, errorLog, dataLDRTable);

    try {
      String tmp;
      while (t.hasMoreTokens()
          && t.nextToken()
          && (!(tmp = JSpecViewUtils.cleanLabel(t.label)).equals("##END") || !label
              .equals("##END"))) {
        label = tmp;

        if (spectrum.getDataType().equals("LINK")) {
          // oops -- just got a LINK -- this is a BLOCK file
          getBlockInstance(source, t, null, dataLDRTable, errorLog);
          spectrum = null;
          label = null;
        } else if (label.equals("##NTUPLES")) {
          getNTupleInstance(source, t, null, dataLDRTable, errorLog);
          spectrum = null;
          label = null;
        } else if (label.equals("##JCAMPCS")) {
          while (t.hasMoreTokens()
              && t.nextToken()
              && !(label = JSpecViewUtils.cleanLabel(t.label))
                  .equals("##TITLE")) {
            }
          spectrum = null;
          // label is not null -- will continue with TITLE
        }
        
        if (spectrum == null) {
          spectrum = new JDXSpectrum();
          dataLDRTable = new HashMap<String, String>();
          if (label == null)
            continue;
        }
          
        if (readDataLabel(spectrum, label, t.value, errorLog, dataLDRTable))
          continue;

        if (Arrays.binarySearch(TABULAR_DATA_LABELS, label) > 0) {
          tabDataLineNo = t.labelLineNo;
          tabularSpecData = spectrum.getTabularData(label, t.value);
          continue;
        }

        // Process Block
        if (label.equals("##END")) {

          if (!spectrum.processTabularData(tabularSpecData, tabDataLineNo,
              dataLDRTable, errorLog))
            throw new JDXSourceException("Unable to read Block Source");

          source.addJDXSpectrum(spectrum);

          tabularSpecData = null;
          spectrum = new JDXSpectrum();
          dataLDRTable = new HashMap<String, String>();
          continue;
        } // End Process Block

        dataLDRTable.put(t.label, t.value);

        if (label.equals("##$PEAKS")) {
          source.peakCount += spectrum.setPeakList(readPeakList(t.value,
              source.peakCount));
          continue;
        }

      } // End Source File
    } catch (NoSuchElementException nsee) {
      throw new JSpecViewException("Unable to Read Block Source");
    } catch (JSpecViewException jsve) {
      throw jsve;
    }
    errorLog.append(ERROR_SEPARATOR);
    source.setErrorLog(errorLog.toString());
    return source;
  }

  //////////////////////////  NTUPLE FILE DATA //////////////////

  /**
   * Does the actual work of initializing the Source instance
   * @param sourceContents
   *        the contents of the source as a String
   * @param sourceLDRTable TODO
   * 
   * @throws JSpecViewException
   * @return an instance of an NTupleSource
   */
  private static JDXSource getNTupleInstance(JDXSource source,
                                             JDXSourceStringTokenizer t,
                                             String sourceContents, 
                                             Map<String, String> sourceLDRTable,
                                             StringBuffer errorLog)
      throws JSpecViewException {

    JDXSpectrum spectrum = null;
    HashMap<String, String> LDRTable;
    if (sourceLDRTable == null)
      sourceLDRTable = new HashMap<String, String>();
    HashMap<String, ArrayList<String>> nTupleTable = new HashMap<String, ArrayList<String>>();

    JDXSpectrum spectrum0 = new JDXSpectrum();

    String page = "";
    String[] plotSymbols = new String[2];
    String tabularSpecData = null;
    int tabDataLineNo = 0;

    if (source == null)
      source = new JDXSource(JDXSource.TYPE_NTUPLE);

    if (errorLog == null)
      errorLog = new StringBuffer();    
    String label = "";

    if (t == null) {
      t = new JDXSourceStringTokenizer(sourceContents);

      // Read Source Specific Header
      while (t.hasMoreTokens() && t.nextToken()
          && !(label = JSpecViewUtils.cleanLabel(t.label)).equals("##NTUPLES"))
        if (!readDataLabel(spectrum0, label, t.value, errorLog, sourceLDRTable))
          sourceLDRTable.put(label, t.value);
      //Finished Pulling out the LDR Table Data

      source.setHeaderTable(sourceLDRTable);

    } else {
      label = "##NTUPLES";
    }

    /*--------------------------------------------*/
    /*------------- Fetch Page Data --------------*/

    if (!label.equals("##NTUPLES"))
      throw new JSpecViewException("Invalid NTuple Source");

    // Read NTuple Table
    while (t.hasMoreTokens() && t.nextToken()
        && !(label = JSpecViewUtils.cleanLabel(t.label)).equals("##PAGE")) {
      StringTokenizer st = new StringTokenizer(t.value, ",");
      ArrayList<String> attrList = new ArrayList<String>();
      while (st.hasMoreTokens())
        attrList.add(st.nextToken().trim());
      nTupleTable.put(label, attrList);
    }//Finished With Page Data
    if (!label.equals("##PAGE"))
      throw new JSpecViewException("Error Reading NTuple Source");
    page = t.value;

    /*--------------------------------------------*/
    /*-------- Gather Spectra Data From File -----*/

    while (t.hasMoreTokens() && t.nextToken()
        && !(label = JSpecViewUtils.cleanLabel(t.label)).equals("##ENDNTUPLES")) {

      if (label.equals("##PAGE")) {
        page = t.value;
        continue;
      }

      // Create and add Spectra
      if (spectrum == null) {
        spectrum = spectrum0.copy();
        spectrum.setTitle(spectrum0.getTitle() + " : " + page);
      }

      LDRTable = new HashMap<String, String>();
      while (!label.equals("##DATATABLE")) {
        LDRTable.put(t.label, t.value);
        t.nextToken();
        label = JSpecViewUtils.cleanLabel(t.label);
      }

      boolean continuous = true;
      tabDataLineNo = t.labelLineNo;
      try {
        BufferedReader reader = new BufferedReader(new StringReader(t.value));
        String line = reader.readLine();
        if (line.trim().indexOf("PEAKS") > 0)
          continuous = false;

        // parse variable list
        int index1 = line.indexOf('(');
        int index2 = line.lastIndexOf(')');
        if (index1 == -1 || index2 == -1)
          throw new JDXSourceException("Variable List not Found");
        String varList = line.substring(index1, index2 + 1);

        ArrayList<String> symbols = (ArrayList<String>) nTupleTable
            .get("##SYMBOL");
        int countSyms = 0;
        for (int i = 0; i < symbols.size(); i++) {
          String sym = ((String) symbols.get(i)).trim();
          if (varList.indexOf(sym) != -1) {
            plotSymbols[countSyms++] = sym;
          }
          if (countSyms == 2)
            break;
        }
      } catch (IOException ioe) {
      }

      tabularSpecData = spectrum.getTabularData("##"
          + (continuous ? "XYDATA" : "PEAKTABLE"), t.value);

      if (!spectrum.createXYCoords(nTupleTable, plotSymbols, spectrum
          .getDataType(), tabularSpecData, tabDataLineNo, errorLog))
        throw new JDXSourceException("Unable to read Ntuple Source");
      for (Iterator<String> iter = sourceLDRTable.keySet().iterator(); iter
          .hasNext();) {
        String key = iter.next();
        if (!key.equals("##TITLE") && !key.equals("##DATACLASS")
            && !key.equals("##NTUPLES"))
          LDRTable.put(key, sourceLDRTable.get(key));
      }
      spectrum.setHeaderTable(LDRTable);

      source.addJDXSpectrum(spectrum);
      spectrum = null;
    }
    errorLog.append(ERROR_SEPARATOR);
    source.setErrorLog(errorLog.toString());
    return source;
  }
}
