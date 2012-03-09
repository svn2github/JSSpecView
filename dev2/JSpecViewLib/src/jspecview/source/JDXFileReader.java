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
import java.io.IOException;
import java.io.InputStream;
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
 * <code>JDXFileReader</code> reads JDX data, including complex BLOCK files that
 * contain NTUPLE blocks or more BLOCK files BLOCK files containing subblocks
 * are just read linearly, not as a hierarchy (for now) (maybe!)
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
  private JDXSourceStreamTokenizer t;
  private StringBuffer errorLog;
  private boolean obscure;

  private boolean done;

  private double[] minMaxY = new double[] { Double.MAX_VALUE, Double.MIN_VALUE };
  private JDXFileReader(boolean obscure, int iSpecFirst, int iSpecLast) {
    this.obscure = obscure;
    firstSpec = iSpecFirst;
    lastSpec = iSpecLast;
  }
  
  
  /**
   * used only for preferences display
   * 
   * @param in
   * @param obscure
   * @return
   * @throws IOException
   * @throws JSpecViewException
   */
  public static JDXSource createJDXSource(InputStream in, boolean obscure)
      throws IOException, JSpecViewException {
    return createJDXSource(FileManager.getBufferedReaderForInputStream(in),
        null, null, obscure, -1, -1);
  }

  /**
   * general entrance method
   * 
   * @param br
   * @param filePath
   * @param appletDocumentBase
   * @param obscure
   * @param iSpecFirst TODO
   * @param iSpecLast TODO
   * @return
   * @throws IOException
   * @throws JSpecViewException
   */
  public static JDXSource createJDXSource(BufferedReader br,
                                          String filePath,
                                          URL appletDocumentBase,
                                          boolean obscure, int iSpecFirst, int iSpecLast) throws IOException,
      JSpecViewException {
    
    try {
      if (filePath != null)
        br = FileManager.getBufferedReaderFromName(filePath, appletDocumentBase);
      br.mark(400);
      char[] chs = new char[400];
      br.read(chs);
      br.reset();
      String header = new String(chs);
      int pt1 = header.indexOf('#');
      int pt2 = header.indexOf('<');
      if (pt1 < 0 || pt2 >= 0 && pt2 < pt1) {
        JDXSource xmlSource = getXMLSource(header, br);
        br.close();
        if (xmlSource != null)
          return xmlSource;
        throw new JSpecViewException("File type not recognized");
      }
      return (new JDXFileReader(obscure, iSpecFirst, iSpecLast)).getJDXSource(br);
    } catch (JSpecViewException e) {
      br.close();
      throw new JSpecViewException("Error reading JDX format: "
          + e.getMessage());
    }
  }

  private static JDXSource getXMLSource(String header, BufferedReader br) {
    String xmlType = header.toLowerCase();
    if (xmlType.contains("<animl") || xmlType.contains("<!doctype technique")) {
      return AnIMLReader.getAniMLInstance(br);
    } else if (xmlType.contains("xml-cml")) {
      return CMLReader.getCMLInstance(br);
    }
    return null;
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
  private JDXSource getJDXSource(BufferedReader br)
      throws JSpecViewException {

    source = new JDXSource(JDXSource.TYPE_SIMPLE);
    t = new JDXSourceStreamTokenizer(br);
    errorLog = new StringBuffer();

    String label = "";
    JDXSpectrum spectrum = new JDXSpectrum();

    // Table for header information
    List<String[]> dataLDRTable = new ArrayList<String[]>(20);
    while (!done && (label = t.getLabel()) != null
        && !label.equals("##END")) {
      if (label.equals("##DATATYPE") && t.getValue().toUpperCase().equals("LINK"))
        return getBlockSpectra(dataLDRTable);
      if (label.equals("##NTUPLES") || label.equals("##VARNAME"))
        return getNTupleSpectra(dataLDRTable, spectrum, label);
      if (Arrays.binarySearch(TABULAR_DATA_LABELS, label) > 0) {
        setTabularDataType(spectrum, label);
        if (!spectrum.processTabularData(t, dataLDRTable, minMaxY, errorLog))
          throw new JDXSourceException("Unable to read JDX file");
        continue;
      }
      if (readDataLabel(spectrum, label, t, errorLog,
          dataLDRTable, obscure))
        continue;
      String value = t.getValue();
      addHeader(dataLDRTable, t.getRawLabel(), value);
      if (label.equals("##$PEAKS")) {
        source.peakCount += spectrum.setPeakList(readPeakList(
            value, source.peakCount));
        continue;
      }
    }
    source.setErrorLog(errorLog.toString());
    addSpectrum(spectrum, false);
    return source;
  }

  private int firstSpec = 0;
  private int lastSpec = 0;
  private int nSpec = 0;

  private double blockID;

  private boolean addSpectrum(JDXSpectrum spectrum, boolean forceSub) {
    nSpec++;
    if (firstSpec > 0 && nSpec < firstSpec)
      return true;
    if (lastSpec > 0 && nSpec > lastSpec)
      return !(done = true);
    spectrum.setBlockID(blockID);
    source.addJDXSpectrum(spectrum, forceSub);
    System.out.println("Spectrum " + nSpec + " XYDATA: " + spectrum.getXYCoords().length);
    return true;
  }

  public static void addHeader(List<String[]> table, String label, String value) {
    String[] entry;
    for (int i = 0; i < table.size(); i++)
      if ((entry = table.get(i))[0].equals(label)) {
        entry[1] = value;
        return;
      }
    table.add(new String[] { label, value });
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
    boolean forceSub = false;
    while ((label = t.getLabel()) != null
        && !label.equals("##TITLE")) {
      if (isNew) {
        if (!readHeaderLabel(source, label, t, errorLog, obscure))
          addHeader(sourceLDRTable, t.getRawLabel(), t.getValue());
      } else {
        t.getValue();
      }
      if (label.equals("##BLOCKS")) {
        int nBlocks = Parser.parseInt(t.getValue());
        if (nBlocks > 100 && firstSpec <=0)
          forceSub = true;
      }
    }

    // If ##TITLE not found throw Exception
    if (!label.equals("##TITLE"))
      throw new JSpecViewException("Unable to read block source");

    if (isNew)
      source.setHeaderTable(sourceLDRTable);
    source.type = JDXSource.TYPE_BLOCK;
    source.isCompoundSource = true;
    List<String[]> dataLDRTable;
    JDXSpectrum spectrum = new JDXSpectrum();
    dataLDRTable = new ArrayList<String[]>();
    readDataLabel(spectrum, label, t, errorLog, dataLDRTable, obscure);

    try {
      String tmp;
      while ((tmp = t.getLabel()) != null) {
          if (tmp.equals("##END") && label.equals("##END")) {
            System.out.println("##END= " + t.getValue());
            break;
          }
        label = tmp;
        if (Arrays.binarySearch(TABULAR_DATA_LABELS, label) > 0) {
          setTabularDataType(spectrum, label);
          if (!spectrum.processTabularData(t, dataLDRTable, minMaxY, errorLog))
            throw new JDXSourceException("Unable to read Block Source");
          continue;
        }

        if (label.equals("##DATATYPE") && t.getValue().toUpperCase().equals("LINK")) {
          // embedded LINK 
          getBlockSpectra(dataLDRTable);
          spectrum = null;
          label = null;
        } else if (label.equals("##NTUPLES") || label.equals("##VARNAME")) {
            getNTupleSpectra(dataLDRTable, spectrum, label);
          spectrum = null;
          label = "";
        } else if (label.equals("##JCAMPCS")) {
          while (!(label = t.getLabel()).equals("##TITLE")) {
            t.getValue();
          }
          spectrum = null;
          // label is not null -- will continue with TITLE
        } else {
          t.getValue();
        }
        if (done)
          break;
        if (spectrum == null) {
          spectrum = new JDXSpectrum();
          dataLDRTable = new ArrayList<String[]>();
          if (label == "") 
            continue;
          if (label == null) {
            label = "##END";
            continue;
          }
        }

        if (readDataLabel(spectrum, label, t, errorLog, dataLDRTable,
            obscure))
          continue;

        // Process Block
        if (label.equals("##END")) {
          if (spectrum.getXYCoords().length > 0 && !addSpectrum(spectrum, forceSub))
            return source;
          spectrum = new JDXSpectrum();
          dataLDRTable = new ArrayList<String[]>();
          t.getValue();
          continue;
        } // End Process Block

        String value = t.getValue();
        addHeader(dataLDRTable, t.getRawLabel(), value);

        if (label.equals("##$PEAKS")) {
          source.peakCount += spectrum.setPeakList(readPeakList(value,
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
   * @param haveVarLabel 
   * 
   * @throws JSpecViewException
   * @return source
   */
  private JDXSource getNTupleSpectra(List<String[]> sourceLDRTable,
                                     JDXSpectrum spectrum0, String label)
      throws JSpecViewException {
    double[] minMaxY = new double[] {Double.MAX_VALUE, Double.MIN_VALUE};
    blockID = Math.random();
    boolean isOK = true;//(spectrum0.is1D() || firstSpec > 0);
    if (firstSpec > 0)
      spectrum0.numDim = 1; // don't display in 2D if only loading some spectra
    
    boolean haveFirstLabel = !label.equals("##NTUPLES");
    if (!haveFirstLabel) {
      label = "";
      t.getValue();
    }
    Map<String, ArrayList<String>> nTupleTable = new Hashtable<String, ArrayList<String>>();
    String[] plotSymbols = new String[2];

    boolean isNew = (source.type == JDXSource.TYPE_SIMPLE);
    if (isNew) {
      source.type = JDXSource.TYPE_NTUPLE;
      source.isCompoundSource = true;
      source.setHeaderTable(sourceLDRTable);
    }

    // Read NTuple Table
    while (!(label = (haveFirstLabel ? label : t.getLabel())).equals("##PAGE")) {
      haveFirstLabel = false;
      StringTokenizer st = new StringTokenizer(t.getValue(), ",");
      ArrayList<String> attrList = new ArrayList<String>();
      while (st.hasMoreTokens())
        attrList.add(st.nextToken().trim());
      nTupleTable.put(label, attrList);
    }//Finished With Page Data
    if (!label.equals("##PAGE"))
      throw new JSpecViewException("Error Reading NTuple Source");
    String page = t.getValue();
    /*
     * 7.3.1 ##PAGE= (STRING).
This LDR indicates the start of a PAGE which contains tabular data. It may have no
argument, or it may be omitted when the data consists of one PAGE. When the Data Table
represents a property like a spectrum or a particular fraction, or at a particular time, or at a
specific location in two or three dimensional space, the appropriate PAGE VARIABLE
values will be given as arguments of the ##PAGE= LDR, as in the following examples:
##PAGE= N=l $$ Spectrum of first fraction of GCIR run
##PAGE= T=10:21 $$ Spectrum of product stream at time: 10:21
##PAGE= X=5.2, Y=7.23 $$ Spectrum of known containing 5.2 % X and 7.23% Y
     */
    
    
    JDXSpectrum spectrum = null;
    while (!done) {
      if ((label = t.getLabel()).equals("##ENDNTUPLES")) {
        t.getValue();
        break;
      }

      if (label.equals("##PAGE")) {
        page = t.getValue();
        continue;
      }

      // Create and add Spectra
      if (spectrum == null) {
        spectrum = spectrum0.copy();
        spectrum.setTitle(spectrum0.getTitle() + " : " + page);
        if (!spectrum.is1D()) {
          setSpectrumY2(spectrum, page);
        }
      }

      List<String[]> dataLDRTable = new ArrayList<String[]>();
      spectrum.setHeaderTable(dataLDRTable);

      while (!label.equals("##DATATABLE")) {
        addHeader(dataLDRTable, t.getRawLabel(), t.getValue());
        label = t.getLabel();
      }

      boolean continuous = true;
      String line = t.flushLine();
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

      setTabularDataType(spectrum, "##" + (continuous ? "XYDATA" : "PEAKTABLE"));

      if (!spectrum.readNTUPLECoords(nTupleTable, plotSymbols, spectrum
          .getDataType(), t, minMaxY, errorLog))
        throw new JDXSourceException("Unable to read Ntuple Source");
      spectrum0.nucleusX = spectrum.nucleusX; 
      for (int i = 0; i < sourceLDRTable.size(); i++) {
        String[] entry = sourceLDRTable.get(i);
        String key = JDXSourceStreamTokenizer.cleanLabel(entry[0]);
        if (!key.equals("##TITLE") && !key.equals("##DATACLASS")
            && !key.equals("##NTUPLES"))
          dataLDRTable.add(entry);
      }
      if (isOK)
        addSpectrum(spectrum, true);
      spectrum = null;
    }
    if (errorLog.length() > 0)
      errorLog.append(ERROR_SEPARATOR);
    source.setErrorLog(errorLog.toString());
    System.out.println("NTUPLE MIN/MAX Y = " + minMaxY[0] + " " + minMaxY[1]);
    return source;
  }

  private void setSpectrumY2(JDXSpectrum spectrum, String page) {
    int pt = page.indexOf('=');
    if (pt >= 0)
      try {
        spectrum.setY2D(Double.parseDouble(page.substring(pt + 1).trim())); 
      } catch (NumberFormatException e) {
        //we tried.            
      }
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
                                       JDXSourceStreamTokenizer t, StringBuffer errorLog,
                                       List<String[]> table, boolean obscure) {

    if (readHeaderLabel(spectrum, label, t, errorLog, obscure))
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
        || label.equals("##DATACLASS")) {
      t.getValue();
      return true;
    }
    if (label.equals("##FIRSTX")) {
      spectrum.fileFirstX = Double.parseDouble(t.getValue());
      return true;
    }

    if (label.equals("##LASTX")) {
      spectrum.fileLastX = Double.parseDouble(t.getValue());
      return true;
    }

    if (label.equals("##NPOINTS")) {
      spectrum.nPointsFile = Integer.parseInt(t.getValue());
      return true;
    }

    if (label.equals("##XFACTOR")) {
      spectrum.xFactor = Double.parseDouble(t.getValue());
      return true;
    }

    if (label.equals("##YFACTOR")) {
      spectrum.yFactor = Double.parseDouble(t.getValue());
      return true;
    }

    if (label.equals("##XLABEL")) {
      spectrum.xUnits = t.getValue();
      return true;
    }

    if (label.equals("##XUNITS") && spectrum.xUnits.equals("")) {
      String value = t.getValue();
      spectrum.xUnits = (value != null && !value.equals("") ? value
          : "Arbitrary Units");
      return true;
    }

    if (label.equals("##YLABEL")) {
      spectrum.yUnits = t.getValue();
      return true;
    }

    if (label.equals("##YUNITS") && spectrum.yUnits.equals("")) {
      String value = t.getValue();
      spectrum.yUnits = (value != null && !value.equals("") ? value
          : "Arbitrary Units");
      return true;
    }

    // NMR variations: need observedFreq, offset, dataPointNum, and shiftRefType 

    if (label.equals("##NUMDIM")) {
      spectrum.numDim = Integer.parseInt(t.getValue());
      return true;
    }

    if (label.equals("##.OBSERVEFREQUENCY")) {
      spectrum.observedFreq = Double.parseDouble(t.getValue());
      return true;
    }

    if (label.equals("##.OBSERVENUCLEUS")) {
      spectrum.observedNucl = t.getValue();
      return true;
    }

    if (label.equals("##$OFFSET") && spectrum.shiftRefType != 0) {
      spectrum.offset = Double.parseDouble(t.getValue());
      // bruker doesn't need dataPointNum
      spectrum.dataPointNum = 1;
      // bruker type
      spectrum.shiftRefType = 1;
      return true;
    }

    if ((label.equals("##$REFERENCEPOINT")) && (spectrum.shiftRefType != 0)) {
      spectrum.offset = Double.parseDouble(t.getValue());
      // varian doesn't need dataPointNum
      spectrum.dataPointNum = 1;
      // varian type
      spectrum.shiftRefType = 2;
    }

    else if (label.equals("##.SHIFTREFERENCE")) {
      String val = t.getValue();
      if (!(spectrum.dataType.toUpperCase().contains("SPECTRUM")))
        return true;
      StringTokenizer srt = new StringTokenizer(val, ",");
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
                                         JDXSourceStreamTokenizer t, StringBuffer errorLog,
                                         boolean obscure) {
    if (label.equals("##TITLE")) {
      String value = t.getValue();
      jdxHeader.title = (obscure || value == null || value.equals("") ? "Unknown"
          : value);
      return true;
    }
    if (label.equals("##JCAMPDX")) {
      String value = t.getValue();
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
      String value = t.getValue();
      jdxHeader.origin = (value != null && !value.equals("") ? value
          : "Unknown");
      return true;
    }

    if (label.equals("##OWNER")) {
      String value = t.getValue();
      jdxHeader.owner = (value != null && !value.equals("") ? value : "Unknown");
      return true;
    }

    if (label.equals("##DATATYPE")) {
      jdxHeader.dataType = t.getValue();
      return true;
    }

    if (label.equals("##LONGDATE")) {
      jdxHeader.longDate = t.getValue();
      return true;
    }

    if (label.equals("##DATE")) {
      jdxHeader.date = t.getValue();
      return true;
    }

    if (label.equals("##TIME")) {
      jdxHeader.time = t.getValue();
      return true;
    }

    return false;
  }

  public void setTabularDataType(JDXSpectrum spectrum, String label) {
    if (label.equals("##PEAKASSIGNMENTS"))
      spectrum.setDataClass("PEAKASSIGNMENTS");
    else if (label.equals("##PEAKTABLE"))
      spectrum.setDataClass("PEAKTABLE");
    else if (label.equals("##XYDATA"))
      spectrum.setDataClass("XYDATA");
    else if (label.equals("##XYPOINTS"))
      spectrum.setDataClass("XYPOINTS");
//    try {
//      t.readLineTrimmed();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
  }
  
}
