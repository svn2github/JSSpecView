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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import jspecview.common.JDXDataObject;
import jspecview.common.JDXHeader;
import jspecview.common.JDXSpectrum;
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
public class JDXFileReader {

  /**
   * Labels for the exporter
   *
   */
  public final static String[][] VAR_LIST_TABLE = {
      //NOTE: [0] MUST BE ALPHABETICAL ORDER BECAUSE EXPORTER USES BINARY SEARCH
      { "PEAKTABLE", "XYDATA", "XYPOINTS" },
      { "(XY..XY)", "(X++(Y..Y))", "(XY..XY)" } };

  final static String ERROR_SEPARATOR = "________________________________________________________";
  private final static String[] TABULAR_DATA_LABELS = { "##XYDATA",
      "##XYPOINTS", "##PEAKTABLE", "##DATATABLE", "##PEAKASSIGNMENTS" };
  static {
    Arrays.sort(TABULAR_DATA_LABELS);
  }
  private JDXSource source;
  private JDXSourceStringTokenizer t;
  private StringBuffer errorLog;
  private boolean obscure;
  
  private JDXFileReader (boolean obscure) {
    this.obscure = obscure;
  }

  public static JDXSource createJDXSource(InputStream in, boolean obscure) throws IOException,
      JSpecViewException {
    return createJDXSource(getContentFromInputStream(in), null, null, obscure);
  }

  public static JDXSource createJDXSource(String sourceContents, boolean obscure)
      throws IOException, JSpecViewException {
    return createJDXSource(sourceContents, null, null, obscure);
  }

  public static JDXSource createJDXSource(String sourceContents,
                                          String filePath,
                                          URL appletDocumentBase,
                                          boolean obscure)
      throws IOException, JSpecViewException {

    try {
      if (filePath != null)
        sourceContents = getContentFromInputStream(FileManager.getInputStream(
            filePath, true, appletDocumentBase));

      if (!sourceContents.startsWith("#")) {
        JDXSource xmlSource = getXMLSource(sourceContents);
        if (xmlSource != null)
          return xmlSource;
        throw new JSpecViewException("File type not recognized");
      }
      return (new JDXFileReader(obscure)).getJDXSource(sourceContents);
    } catch (JSpecViewException e) {
      throw new JSpecViewException("Error reading JDX format: "
          + e.getMessage());
    }
  }

  private static JDXSource getXMLSource(String source) {
    String xmlType = source.substring(0, 400).toLowerCase();

    if (xmlType.contains("<animl") || xmlType.contains("<!doctype technique")) {
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
   * starting point for reading all data
   * 
   * @param sourceContents
   *        the contents of the source as a String
   * 
   * @return source
   * @throws JSpecViewException
   */
  private JDXSource getJDXSource(String sourceContents)
      throws JSpecViewException {
    
    source = new JDXSource(JDXSource.TYPE_SIMPLE);
    t = new JDXSourceStringTokenizer(sourceContents);
    errorLog = new StringBuffer();

    String label = "";
    JDXSpectrum spectrum = new JDXSpectrum();

    // The data Table
    String tabularSpecData = null;
    int tabularDataLabelLineNo = 0;

    // Table for header information
    List<String[]> dataLDRTable = new ArrayList<String[]>(20);

    while (t.hasMoreTokens() && t.getNextToken()
        && !(label = cleanLabel(t.label)).equals("##END")) {
      if (label.equals("##DATATYPE") && t.value.toUpperCase().equals("LINK"))
        return getBlockSpectra(dataLDRTable);
      if (label.equals("##NTUPLES"))
        return getNTupleSpectra(dataLDRTable, spectrum);
      if (JDXFileReader.readDataLabel(spectrum, label, t.value, errorLog,
          dataLDRTable, obscure))
        continue;
      if (Arrays.binarySearch(JDXFileReader.TABULAR_DATA_LABELS, label) > 0) {
        tabularDataLabelLineNo = t.labelLineNo;
        tabularSpecData = spectrum.getTabularData(label, t.value);
        continue;
      }
      addHeader(dataLDRTable, t.label, t.value);
      if (label.equals("##$PEAKS")) {
        source.peakCount += spectrum.setPeakList(JDXFileReader.readPeakList(
            t.value, source.peakCount));
        continue;
      }
    }
    if (!label.equals("##END"))
      tabularSpecData = null;
    if (!spectrum.processTabularData(tabularSpecData, tabularDataLabelLineNo,
        dataLDRTable, errorLog))
      throw new JDXSourceException("Unable to read JDX file");
    source.setErrorLog(errorLog.toString());
    source.addJDXSpectrum(spectrum);
    return source;
  }

  public static void addHeader(List<String[]> table, String label,
                         String value) {
    String[] entry;
    for (int i = 0; i < table.size(); i++)
      if ((entry = table.get(i))[0].equals(label)) {
        entry[1] = value;
        return;
      }
    table.add(new String[] {label, value});
  }

  /**
   * reads BLOCK data
   * 
   * @param sourceLDRTable
   * @return source
   * @throws JSpecViewException
   */
  private JDXSource getBlockSpectra(List<String[]> sourceLDRTable)
      throws JSpecViewException {
    
    System.out.println("--JDX block start--");
    String label = "";
    boolean isNew = (source.type == JDXSource.TYPE_SIMPLE);
    while (t.hasMoreTokens() && t.getNextToken()
        && !(label = cleanLabel(t.label)).equals("##TITLE"))
      if (isNew && !readHeaderLabel(source, label, t.value, errorLog, obscure))
        addHeader(sourceLDRTable, t.label, t.value);

    // If ##TITLE not found throw Exception
    if (!label.equals("##TITLE"))
      throw new JSpecViewException("Unable to read block source");

    if (isNew)
      source.setHeaderTable(sourceLDRTable);
    source.type = JDXSource.TYPE_BLOCK;
    source.isCompoundSource = true;
    List<String[]> dataLDRTable;
    int tabDataLineNo = 0;
    String tabularSpecData = null;

    JDXSpectrum spectrum = new JDXSpectrum();
    dataLDRTable = new ArrayList<String[]>();
    readDataLabel(spectrum, label, t.value, errorLog, dataLDRTable, obscure);

    try {
      String tmp;
      while (t.hasMoreTokens()
          && t.getNextToken()
          && (!(tmp = cleanLabel(t.label)).equals("##END") || !label
              .equals("##END"))) {
        label = tmp;

        if (label.equals("##DATATYPE") && t.value.toUpperCase().equals("LINK")) {
          // embedded LINK 
          getBlockSpectra(dataLDRTable);
          spectrum = null;
          label = null;
        } else if (label.equals("##NTUPLES")) {
          getNTupleSpectra(dataLDRTable, spectrum);
          spectrum = null;
          label = null;
        } else if (label.equals("##JCAMPCS")) {
          while (t.hasMoreTokens()
              && t.getNextToken()
              && !(label = cleanLabel(t.label))
                  .equals("##TITLE")) {
          }
          spectrum = null;
          // label is not null -- will continue with TITLE
        }

        if (spectrum == null) {
          spectrum = new JDXSpectrum();
          dataLDRTable = new ArrayList<String[]>();
          if (label == null) {
            label = "##END";
            continue;
          }
        }

        if (readDataLabel(spectrum, label, t.value, errorLog, dataLDRTable, obscure))
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
          dataLDRTable = new ArrayList<String[]>();
          continue;
        } // End Process Block

        addHeader(dataLDRTable, t.label, t.value);

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
    if (errorLog.length() > 0)
      errorLog.append(ERROR_SEPARATOR);
    source.setErrorLog(errorLog.toString());
    System.out.println("--JDX block end--");
    return source;
  }

  /**
   * reads NTUPLE data
   * 
   * @param sourceLDRTable
   * @param spectrum0
   * 
   * @throws JSpecViewException
   * @return source
   */
  private JDXSource getNTupleSpectra(List<String[]> sourceLDRTable,
                                      JDXSpectrum spectrum0)
      throws JSpecViewException {

    String label = "";
    String page = "";

    String tabularSpecData = null;
    int tabDataLineNo = 0;

    Map<String, ArrayList<String>> nTupleTable = new Hashtable<String, ArrayList<String>>();
    String[] plotSymbols = new String[2];

    boolean isNew = (source.type == JDXSource.TYPE_SIMPLE);
    if (isNew) {
      source.type = JDXSource.TYPE_NTUPLE;
      source.isCompoundSource = true;
      source.setHeaderTable(sourceLDRTable);
    }

    // Read NTuple Table
    while (t.hasMoreTokens() && t.getNextToken()
        && !(label = cleanLabel(t.label)).equals("##PAGE")) {
      StringTokenizer st = new StringTokenizer(t.value, ",");
      ArrayList<String> attrList = new ArrayList<String>();
      while (st.hasMoreTokens())
        attrList.add(st.nextToken().trim());
      nTupleTable.put(label, attrList);
    }//Finished With Page Data
    if (!label.equals("##PAGE"))
      throw new JSpecViewException("Error Reading NTuple Source");
    page = t.value;

    /*-------- Gather Spectra Data From File -----*/

    JDXSpectrum spectrum = null;

    while (t.hasMoreTokens() && t.getNextToken()
        && !(label = cleanLabel(t.label)).equals("##ENDNTUPLES")) {

      if (label.equals("##PAGE")) {
        page = t.value;
        continue;
      }

      // Create and add Spectra
      if (spectrum == null) {
        spectrum = spectrum0.copy();
        spectrum.setTitle(spectrum0.getTitle() + " : " + page);
      }

      List<String[]> dataLDRTable = new ArrayList<String[]>();
      spectrum.setHeaderTable(dataLDRTable);

      while (!label.equals("##DATATABLE")) {
        addHeader(dataLDRTable, t.label, t.value);
        t.getNextToken();
        label = cleanLabel(t.label);
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
          String sym = symbols.get(i).trim();
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
      for (int i = 0; i < sourceLDRTable.size(); i++) {
        String[] entry = sourceLDRTable.get(i);
        String key = cleanLabel(entry[0]);
        if (!key.equals("##TITLE") && !key.equals("##DATACLASS")
            && !key.equals("##NTUPLES"))
          dataLDRTable.add(entry);
      }
      source.addJDXSpectrum(spectrum);
      spectrum = null;
    }
    if (errorLog.length() > 0)
      errorLog.append(ERROR_SEPARATOR);
    source.setErrorLog(errorLog.toString());
    return source;
  }
  
  /**
   * Extracts spaces, underscores etc. from the label
   * 
   * @param label
   *        the label to be cleaned
   * @return the new label
   */
  private static String cleanLabel(String label) {
    int i;
    StringBuffer str = new StringBuffer();

    for (i = 0; i < label.length(); i++) {
      switch (label.charAt(i)) {
      case '/':
      case '\\':
      case ' ':
      case '-':
      case '_':
        break;
      default:
        str.append(label.charAt(i));
        break;
      }
    }
    return str.toString().toUpperCase();
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
                                       List<String[]> table, boolean obscure) {

    if (readHeaderLabel(spectrum, label, value, errorLog, obscure))
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
      return true;
    }

    if (label.equals("##.OBSERVENUCLEUS")) {
      spectrum.observedNucl = value;
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
                                         String value, StringBuffer errorLog, 
                                         boolean obscure) {
    if (label.equals("##TITLE")) {
      jdxHeader.title = (obscure || value == null
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

}
