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
import java.util.Arrays;
import java.util.Hashtable;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javajs.util.BS;
import javajs.util.List;
import javajs.util.SB;
import javajs.util.Txt;

import org.jmol.util.Logger;
import javajs.util.Parser;


import jspecview.api.JSVZipReader;
import jspecview.api.SourceReader;
import jspecview.common.Coordinate;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVFileManager;
import jspecview.common.JSViewer;
import jspecview.common.PeakInfo;
import jspecview.exception.JDXSourceException;
import jspecview.exception.JSpecViewException;
import jspecview.util.JSVEscape;

/**
 * <code>JDXFileReader</code> reads JDX data, including complex BLOCK files that
 * contain NTUPLE blocks or nested BLOCK data. 
 * 
 * In addition, this reader allows for simple concatenation -- no LINK record is 
 * required. This allows for testing simply by joining files. 
 * 
 * We also might be able to adapt this to reading a ZIP file collection.
 * 
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 * @author Bob Hanson, hansonr@stolaf.edu
 */
public class FileReader {

  /**
   * Labels for the exporter
   * 
   */
  public final static String[][] VAR_LIST_TABLE = {
      //NOTE: [0] MUST BE ALPHABETICAL ORDER BECAUSE EXPORTER USES BINARY SEARCH
      { "PEAKTABLE", "XYDATA", "XYPOINTS" },
      { "(XY..XY)", "(X++(Y..Y))", "(XY..XY)" } };

  final static String ERROR_SEPARATOR = "=====================\n";
  
  private final static String[] TABULAR_DATA_LABELS = { "##XYDATA",
      "##XYPOINTS", "##PEAKTABLE", "##DATATABLE", "##PEAKASSIGNMENTS" };
  static {
    Arrays.sort(TABULAR_DATA_LABELS);
  }
  private JDXSource source;
  private JDXSourceStreamTokenizer t;
  private SB errorLog;
  private boolean obscure;

  private boolean done;

  private boolean isZipFile;

  private String filePath;

  private boolean loadImaginary = true;
  
  private FileReader(String filePath, boolean obscure, boolean loadImaginary,
  		int iSpecFirst, int iSpecLast) {
  	System.out.println("FileReader filePath=" + filePath + "<<");
  	filePath = Txt.trimQuotes(filePath);
    this.filePath = (filePath != null && filePath.startsWith(JSVFileManager.SIMULATION_PROTOCOL + "MOL=") ? 
    		JSVFileManager.SIMULATION_PROTOCOL + "MOL=" + Math.abs(filePath.hashCode()) : filePath);
    this.obscure = obscure;
    firstSpec = iSpecFirst;
    lastSpec = iSpecLast;
    this.loadImaginary = loadImaginary;
  }
  
  
  /**
   * used only for preferences display and Android
   * 
   * @param in
   * @param obscure
   * @param loadImaginary 
   * @return source
   * @throws Exception
   */
  public static JDXSource createJDXSourceFromStream(InputStream in, boolean obscure, boolean loadImaginary)
      throws Exception {
    return createJDXSource(JSVFileManager.getBufferedReaderForInputStream(in),
        "stream", obscure, loadImaginary, -1, -1);
  }

	/**
	 * general entrance method
	 * 
	 * @param br
	 * @param filePath
	 * @param obscure
	 * @param loadImaginary
	 * @param iSpecFirst
	 *          TODO
	 * @param iSpecLast
	 *          TODO
	 * @return source
	 * @throws Exception
	 */
	public static JDXSource createJDXSource(BufferedReader br, String filePath,
			boolean obscure, boolean loadImaginary,
			int iSpecFirst, int iSpecLast) throws Exception {

		try {
			if (br == null)
				br = JSVFileManager.getBufferedReaderFromName(filePath, "##TITLE");
			br.mark(400);
			char[] chs = new char[400];
			br.read(chs, 0, 400);
			br.reset();
			String header = new String(chs);
			int pt1 = header.indexOf('#');
			int pt2 = header.indexOf('<');
			if (pt1 < 0 || pt2 >= 0 && pt2 < pt1) {
				String xmlType = header.toLowerCase();
				if (xmlType.contains("404"))
					System.out.println(xmlType);
				xmlType = (xmlType.contains("<animl")
						|| xmlType.contains("<!doctype technique") ? "AnIML" : xmlType
						.contains("xml-cml") ? "CML" : null);
				JDXSource xmlSource = ((SourceReader) JSViewer
						.getInterface("jspecview.source." + xmlType + "Reader")).getSource(
						filePath, br);
				br.close();
				if (xmlSource == null)
					throw new JSpecViewException("File type not recognized");
				return xmlSource;
			}
			return (new FileReader(filePath, obscure, loadImaginary, iSpecFirst,
					iSpecLast)).getJDXSource(br);
		} catch (JSpecViewException e) {
			br.close();
			throw new Exception("Error reading JDX format: " + e);
		}
	}

  /**
   * The starting point for reading all data.
   * 
   * @param reader  a BufferedReader or a JSVZipFileSequentialReader
   * 
   * @return source
   * @throws JSpecViewException
   */
  private JDXSource getJDXSource(Object reader) throws JSpecViewException {

    source = new JDXSource(JDXSource.TYPE_SIMPLE, filePath);
    isZipFile = (reader instanceof JSVZipReader);
    t = new JDXSourceStreamTokenizer((BufferedReader) reader);
    errorLog = new SB();

    String label = null;

    while (!done && "##TITLE".equals(t.peakLabel())) {
      if (label != null && !isZipFile)
        errorLog.append("Warning - file is a concatenation without LINK record -- does not conform to IUPAC standards!\n");
      JDXSpectrum spectrum = new JDXSpectrum();
      List<String[]> dataLDRTable = new List<String[]>();
      while (!done && (label = t.getLabel()) != null && !isEnd(label)) {
        if (label.equals("##DATATYPE")
            && t.getValue().toUpperCase().equals("LINK")) {
          getBlockSpectra(dataLDRTable);
          spectrum = null;
          continue;
        }
        if (label.equals("##NTUPLES") || label.equals("##VARNAME")) {
          getNTupleSpectra(dataLDRTable, spectrum, label);
          spectrum = null;
          continue;
        }
        if (Arrays.binarySearch(TABULAR_DATA_LABELS, label) > 0) {
          setTabularDataType(spectrum, label);
          if (!processTabularData(spectrum, dataLDRTable))
            throw new JDXSourceException("Unable to read JDX file");
          addSpectrum(spectrum, false);
          spectrum = null;
          continue;
        }
        if (spectrum == null)
          spectrum = new JDXSpectrum();
        if (readDataLabel(spectrum, label, t, errorLog, obscure))
          continue;
        String value = t.getValue();
        addHeader(dataLDRTable, t.getRawLabel(), value);
        if (checkCustomTags(spectrum, label, value))
        	continue;
      }
    }
    source.setErrorLog(errorLog.toString());
    return source;
  }

  private boolean isEnd(String label) {
    if (!label.equals("##END"))
      return false;
    t.getValue();
    return true;
  }

  private int firstSpec = 0;
  private int lastSpec = 0;
  private int nSpec = 0;

  private double blockID;

  private String piUnitsX;

  private String piUnitsY;

	private String thisModelID;

	private int peakIndex;

  private boolean addSpectrum(JDXSpectrum spectrum, boolean forceSub) {
  	if (!loadImaginary && spectrum.isImaginary()) {
  		Logger.info("FileReader skipping imaginary spectrum -- use LOADIMAGINARY TRUE to load this spectrum.");
  		return true;
  	}
    nSpec++;
    if (firstSpec > 0 && nSpec < firstSpec)
      return true;
    if (lastSpec > 0 && nSpec > lastSpec)
      return !(done = true);
    spectrum.setBlockID(blockID);
    source.addJDXSpectrum(null, spectrum, forceSub);
    //System.out.println("Spectrum " + nSpec + " XYDATA: " + spectrum.getXYCoords().length);
    return true;
  }

	/**
	 * reads BLOCK data
	 * 
	 * @param sourceLDRTable
	 * @return source
	 * @throws JSpecViewException
	 */
	@SuppressWarnings("null")
	private JDXSource getBlockSpectra(List<String[]> sourceLDRTable)
			throws JSpecViewException {

		Logger.debug("--JDX block start--");
		String label = "";
		boolean isNew = (source.type == JDXSource.TYPE_SIMPLE);
		boolean forceSub = false;
		while ((label = t.getLabel()) != null && !label.equals("##TITLE")) {
			if (isNew) {
				if (!readHeaderLabel(source, label, t, errorLog, obscure))
					addHeader(sourceLDRTable, t.getRawLabel(), t.getValue());
			} else {
				t.getValue();
			}
			if (label.equals("##BLOCKS")) {
				int nBlocks = Parser.parseInt(t.getValue());
				if (nBlocks > 100 && firstSpec <= 0)
					forceSub = true;
			}
		}

		// If ##TITLE not found throw Exception
		if (!"##TITLE".equals(label))
			throw new JSpecViewException("Unable to read block source");

		if (isNew)
			source.setHeaderTable(sourceLDRTable);
		source.type = JDXSource.TYPE_BLOCK;
		source.isCompoundSource = true;
		List<String[]> dataLDRTable;
		JDXSpectrum spectrum = new JDXSpectrum();
		dataLDRTable = new List<String[]>();
		readDataLabel(spectrum, label, t, errorLog, obscure);

		try {
			String tmp;
			while ((tmp = t.getLabel()) != null) {
				if ("##END".equals(label) && isEnd(tmp)) {
					Logger.debug("##END= " + t.getValue());
					break;
				}
				label = tmp;
				if (Arrays.binarySearch(TABULAR_DATA_LABELS, label) > 0) {
					setTabularDataType(spectrum, label);
					if (!processTabularData(spectrum, dataLDRTable))
						throw new JDXSourceException("Unable to read Block Source");
					continue;
				}

				if (label.equals("##DATATYPE")
						&& t.getValue().toUpperCase().equals("LINK")) {
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
					dataLDRTable = new List<String[]>();
					if (label == "")
						continue;
					if (label == null) {
						label = "##END";
						continue;
					}
				}

				if (readDataLabel(spectrum, label, t, errorLog, obscure))
					continue;

				// Process Block
				if (isEnd(label)) {
					if (spectrum.getXYCoords().length > 0
							&& !addSpectrum(spectrum, forceSub))
						return source;
					spectrum = new JDXSpectrum();
					dataLDRTable = new List<String[]>();
					t.getValue();
					continue;
				} // End Process Block

				String value = t.getValue();
				addHeader(dataLDRTable, t.getRawLabel(), value);
				if (checkCustomTags(spectrum, label, value))
					continue;
			} // End Source File
		} catch (NoSuchElementException nsee) {
			throw new JSpecViewException("Unable to Read Block Source");
		} catch (JSpecViewException jsve) {
			throw jsve;
		}
		addErrorLogSeparator();
		source.setErrorLog(errorLog.toString());
		Logger.debug("--JDX block end--");
		return source;
	}

	private boolean checkCustomTags(JDXSpectrum spectrum, String label,
			String value) {
		int pt = "##$MODELS ##$PEAKS  ##$SIGNALS".indexOf(label);
		switch (pt) {
		default:
			return false;
		case 0:
			thisModelID = Parser.getQuotedAttribute(value, "id");
			return true;
		case 10:
		case 20:
			peakIndex = source.peakCount;
			source.peakCount += readPeaks(spectrum, value, pt == 20);
			return true;
		}
	}


	private void addErrorLogSeparator() {
    if (errorLog.length() > 0
        && errorLog.lastIndexOf(ERROR_SEPARATOR) != errorLog.length()
            - ERROR_SEPARATOR.length())
      errorLog.append(ERROR_SEPARATOR);
  }


  /**
   * reads NTUPLE data
   * 
   * @param sourceLDRTable
   * @param spectrum0
   * @param label 
   * 
   * @throws JSpecViewException
   * @return source
   */
  @SuppressWarnings("null")
	private JDXSource getNTupleSpectra(List<String[]> sourceLDRTable,
                                     JDXDataObject spectrum0, String label)
      throws JSpecViewException {
    double[] minMaxY = new double[] { Double.MAX_VALUE, Double.MIN_VALUE };
    blockID = Math.random();
    boolean isOK = true;//(spectrum0.is1D() || firstSpec > 0);
    if (firstSpec > 0)
      spectrum0.numDim = 1; // don't display in 2D if only loading some spectra

    boolean isVARNAME = label.equals("##VARNAME");
    if (!isVARNAME) {
      label = "";
      t.getValue();
    }
    Map<String, List<String>> nTupleTable = new Hashtable<String, List<String>>();
    String[] plotSymbols = new String[2];

    boolean isNew = (source.type == JDXSource.TYPE_SIMPLE);
    if (isNew) {
      source.type = JDXSource.TYPE_NTUPLE;
      source.isCompoundSource = true;
      source.setHeaderTable(sourceLDRTable);
    }

    // Read NTuple Table
    while (!(label = (isVARNAME ? label : t.getLabel())).equals("##PAGE")) {
      isVARNAME = false;
      StringTokenizer st = new StringTokenizer(t.getValue(), ",");
      List<String> attrList = new List<String>();
      while (st.hasMoreTokens())
        attrList.addLast(st.nextToken().trim());
      nTupleTable.put(label, attrList);
    }//Finished With Page Data
    List<String> symbols = nTupleTable.get("##SYMBOL");
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
    boolean isFirst = true;
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
        spectrum = new JDXSpectrum();
        spectrum0.copyTo(spectrum);
        spectrum.setTitle(spectrum0.getTitle());
        if (!spectrum.is1D()) {
          int pt = page.indexOf('=');
          if (pt >= 0)
            try {
              spectrum
                  .setY2D(Double.parseDouble(page.substring(pt + 1).trim()));
              String y2dUnits = page.substring(0, pt).trim();
              int i = symbols.indexOf(y2dUnits);
              if (i >= 0)
                spectrum.setY2DUnits(nTupleTable.get("##UNITS").get(i));
            } catch (Exception e) {
              //we tried.            
            }
        }
      }

      List<String[]> dataLDRTable = new List<String[]>();
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

      if (!readNTUPLECoords(spectrum, nTupleTable, plotSymbols, minMaxY))
        throw new JDXSourceException("Unable to read Ntuple Source");
      if (!spectrum.nucleusX.equals("?"))
        spectrum0.nucleusX = spectrum.nucleusX;
      spectrum0.nucleusY = spectrum.nucleusY;
      spectrum0.freq2dX = spectrum.freq2dX;
      spectrum0.freq2dY = spectrum.freq2dY;
      spectrum0.y2DUnits = spectrum.y2DUnits;
      for (int i = 0; i < sourceLDRTable.size(); i++) {
        String[] entry = sourceLDRTable.get(i);
        String key = JDXSourceStreamTokenizer.cleanLabel(entry[0]);
        if (!key.equals("##TITLE") && !key.equals("##DATACLASS")
            && !key.equals("##NTUPLES"))
          dataLDRTable.addLast(entry);
      }
      if (isOK)
        addSpectrum(spectrum, !isFirst);
      isFirst = false;
      spectrum = null;
    }
    addErrorLogSeparator();
    source.setErrorLog(errorLog.toString());
    Logger.info("NTUPLE MIN/MAX Y = " + minMaxY[0] + " " + minMaxY[1]);
    return source;
  }

	/**
	 * * read a <Peaks> or <Signals> block See similar method in
	 * Jmol/src/adapter/more/JcampdxReader.java
	 * 
	 * @param spectrum
	 * 
	 * 
	 * @param peakList
	 * @param isSignals
	 * @return new # of peaks
	 */
	private int readPeaks(JDXSpectrum spectrum, String peakList, boolean isSignals) {
		List<PeakInfo> peakData = new List<PeakInfo>();
		BufferedReader reader = new BufferedReader(new StringReader(peakList));
		try {
			int offset = (isSignals ? 1 : 0);
			System.out.println("offset is " + offset + " isSignals=" + isSignals);
			String tag1 = (isSignals ? "Signals" : "Peaks");
			String tag2 = (isSignals ? "<Signal" : "<PeakData");
			String line = discardUntil(reader, tag1);
			if (line.indexOf("<" + tag1) < 0)
				line = discardUntil(reader, "<" + tag1);
			if (line.indexOf("<" + tag1) < 0)
				return 0;

			String file = getPeakFilePath();
			String model = getQuotedAttribute(line, "model");
			model = " model=" + escape(model == null ? thisModelID : model);
			String type = getQuotedAttribute(line, "type");
			if ("HNMR".equals(type))
				type = "1HNMR";
			else if ("CNMR".equals(type))
				type = "13CNMR";
			type = (type == null ? "" : " type=" + escape(type));
			piUnitsX = getQuotedAttribute(line, "xLabel");
			piUnitsY = getQuotedAttribute(line, "yLabel");
			Map<String, Object[]> htSets = new Hashtable<String, Object[]>();
			List<Object[]> list = new List<Object[]>();
			while ((line = reader.readLine()) != null
					&& !(line = line.trim()).startsWith("</" + tag1)) {
				if (line.startsWith(tag2)) {
					info(line);
					String title = getQuotedAttribute(line, "title");
          if (title == null) {
            title = (type == "1HNMR" ? "atom%S%: %ATOMS%; integration: %NATOMS%" : "");
            title = " title=" + escape(title);
          } else {
            title = "";
          }
					String stringInfo = "<PeakData "
							+ file
							+ " index=\"%INDEX%\""
							+ title
							+ type
							+ (getQuotedAttribute(line, "model") == null ? model
									: "") + " " + line.substring(tag2.length()).trim();
					String atoms = getQuotedAttribute(stringInfo, "atoms");
					if (atoms != null)
						stringInfo = simpleReplace(stringInfo, "atoms=\""
								+ atoms + "\"", "atoms=\"%ATOMS%\"");
					String key = ((int) (parseFloatStr(getQuotedAttribute(line, "xMin")) * 100))
							+ "_"
							+ ((int) (parseFloatStr(getQuotedAttribute(line,
									"xMax")) * 100));
					Object[] o = htSets.get(key);
					if (o == null) {
						o = new Object[] { stringInfo,
								(atoms == null ? null : new BS()) };
						htSets.put(key, o);
						list.addLast(o);
					}
					BS bs = (BS) o[1];
					if (atoms != null && bs != null) {
						atoms = atoms.replace(',', ' ');
						bs.or(unescapeBitSet("({" + atoms + "})"));
						System.out.println("bs is  " + bs);
					}
				}
			}
			int nH = 0;
			int n = list.size();
			for (int i = 0; i < n; i++) {
				Object[] o = list.get(i);
				String stringInfo = (String) o[0];
				stringInfo = simpleReplace(stringInfo, "%INDEX%", ""
						+ getPeakIndex());
				BS bs = (BS) o[1];
				if (bs != null) {
					System.out.println("bs " + i + " is " + bs);
					String s = "";
					for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
						s += "," + (j + offset);
					int na = bs.cardinality();
					nH += na;
					stringInfo = simpleReplace(stringInfo, "%ATOMS%", s
							.substring(1));
					stringInfo = simpleReplace(stringInfo, "%S%",
							(na == 1 ? "" : "s"));
					stringInfo = simpleReplace(stringInfo, "%NATOMS%", ""
							+ na);
				}
				info("JSpecView using " + stringInfo);
				add(peakData, stringInfo);
			}
			setSpectrumPeaks(spectrum, peakData, nH);
			return n;
		} catch (Exception e) {
			return 0;
		}
	}

  private void add(List<PeakInfo> peakData, String info) {
  	peakData.addLast(new PeakInfo(info));
	}

	private void info(String s) {
  	Logger.info(s);
	}

	private javajs.util.BS unescapeBitSet(String str) {
    char ch;
    int len;
    if (str == null || (len = (str = str.trim()).length()) < 4
        || str.equalsIgnoreCase("({null})") 
        || (ch = str.charAt(0)) != '(' && ch != '[' 
        || str.charAt(len - 1) != (ch == '(' ? ')' : ']')
        || str.charAt(1) != '{' || str.indexOf('}') != len - 2)
      return null;
    len -= 2;
    for (int i = len; --i >= 2;)
      if (!Character.isDigit(ch = str.charAt(i)) && ch != ' ' && ch != '\t'
          && ch != ':')
        return null;
    int lastN = len;
    while (Character.isDigit(str.charAt(--lastN))) {
      // loop
    }
    if (++lastN == len)
      lastN = 0;
    else
      try {
        lastN = Integer.parseInt(str.substring(lastN, len));
      } catch (NumberFormatException e) {
        return null;
      }
    BS bs = BS.newN(lastN);
    lastN = -1;
    int iPrev = -1;
    int iThis = -2;
    for (int i = 2; i <= len; i++) {
      switch (ch = str.charAt(i)) {
      case '\t':
      case ' ':
      case '}':
        if (iThis < 0)
          break;
        if (iThis < lastN)
          return null;
        lastN = iThis;
        if (iPrev < 0)
          iPrev = iThis;
        bs.setBits(iPrev, iThis + 1);
        iPrev = -1;
        iThis = -2;
        break;
      case ':':
        iPrev = lastN = iThis;
        iThis = -2;
        break;
      default:
        if (Character.isDigit(ch)) {
          if (iThis < 0)
            iThis = 0;
          iThis = (iThis * 10) + (ch - 48);
        }
      }
    }
    return (iPrev >= 0 ? null : bs);
	}

	private float parseFloatStr(String s) {
  	return Parser.parseFloat(s);
  }

	private String simpleReplace(String s, String sfrom, String sto) {
		return Txt.simpleReplace(s, sfrom, sto);
	}

	private String escape(String s) {
		return JSVEscape.eS(s);
	}

	private String getQuotedAttribute(String s, String attr) {
		return Parser.getQuotedAttribute(s, attr);
	}

	private String getPeakFilePath() {
				return " file=" + JSVEscape.eS(Txt.trimQuotes(filePath).replace('\\', '/'));
	}


	private void setSpectrumPeaks(JDXSpectrum spectrum,
			List<PeakInfo> peakData, int nH) {
		spectrum.setPeakList(peakData, piUnitsX, piUnitsY);
		spectrum.setNHydrogens(nH);
	}

	private String discardUntil(BufferedReader reader, String tag) 
    throws Exception {
      String line;
      while ((line = reader.readLine()) != null && line.indexOf("<" + tag) < 0 && line.indexOf("##") < 0) {
      }
      return line;
	}
  
	private int getPeakIndex() {
  	return ++peakIndex;
	}

  /**
   * 
   * @param spectrum
   * @param label
   * @param t
   * @param errorLog
   * @param obscure
   * @return  true to skip saving this key in the spectrum headerTable
   */
  private static boolean readDataLabel(JDXDataObject spectrum, String label,
                                       JDXSourceStreamTokenizer t,
                                       SB errorLog, boolean obscure) {

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

    if (label.equals("##XUNITS")) {
      spectrum.setXUnits(t.getValue());
      return true;
    }

    if (label.equals("##YUNITS")) {
      spectrum.setYUnits(t.getValue());
      return true;
    }

    if (label.equals("##XLABEL")) {
      spectrum.setXLabel(t.getValue());
      return false; // store in hashtable
    }

    if (label.equals("##YLABEL")) {
      spectrum.setYLabel(t.getValue());
      return false; // store in hashtable
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
      spectrum.setObservedNucleus(t.getValue());
      return true;
    }

    if (label.equals("##$OFFSET") && spectrum.shiftRefType != 0) {
    	if (spectrum.offset == JDXDataObject.ERROR)
        spectrum.offset = Double.parseDouble(t.getValue());
      // bruker doesn't need dataPointNum
      spectrum.dataPointNum = 1;
      // bruker type
      spectrum.shiftRefType = 1;
      return false;
    }

    if ((label.equals("##$REFERENCEPOINT")) && (spectrum.shiftRefType != 0)) {
      spectrum.offset = Double.parseDouble(t.getValue());
      // varian doesn't need dataPointNum
      spectrum.dataPointNum = 1;
      // varian type
      spectrum.shiftRefType = 2;
      return false; // save in file  
    }
    
    if (label.equals("##.SHIFTREFERENCE")) {
      //TODO: don't save in file??
      String val = t.getValue();
      if (!(spectrum.dataType.toUpperCase().contains("SPECTRUM")))
        return true;
      StringTokenizer srt =   new StringTokenizer(val, ",");
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
                                         JDXSourceStreamTokenizer t, SB errorLog,
                                         boolean obscure) {
    if (label.equals("##TITLE")) {
      String value = t.getValue();
      jdxHeader.setTitle(obscure || value == null || value.equals("") ? "Unknown"
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

  private void setTabularDataType(JDXDataObject spectrum, String label) {
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

  private boolean processTabularData(JDXDataObject spec, 
                                            List<String[]> table)
      throws JSpecViewException {
    if (spec.dataClass.equals("PEAKASSIGNMENTS"))
      return true;

    spec.setHeaderTable(table);

    if (spec.dataClass.equals("XYDATA")) {
      spec.checkRequiredTokens();
      decompressData(spec, null);
      return true;
    }
    if (spec.dataClass.equals("PEAKTABLE") || spec.dataClass.equals("XYPOINTS")) {
      spec.setContinuous(spec.dataClass.equals("XYPOINTS"));
      // check if there is an x and y factor
      try {
        t.readLineTrimmed();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      Coordinate[] xyCoords;
      
      if (spec.xFactor != JDXDataObject.ERROR && spec.yFactor != JDXDataObject.ERROR)
        xyCoords = Coordinate.parseDSV(t.getValue(), spec.xFactor, spec.yFactor);
      else
        xyCoords = Coordinate.parseDSV(t.getValue(), 1, 1);
      spec.setXYCoords(xyCoords);
      double fileDeltaX = Coordinate.deltaX(xyCoords[xyCoords.length - 1]
          .getXVal(), xyCoords[0].getXVal(), xyCoords.length);
      spec.setIncreasing(fileDeltaX > 0);
      return true;
    }
    return false;
  }

  private boolean readNTUPLECoords(JDXDataObject spec, 
                                          Map<String, List<String>> nTupleTable,
                                          String[] plotSymbols,
                                          double[] minMaxY) {
    List<String> list;
    if (spec.dataClass.equals("XYDATA")) {
      // Get Label Values

      list = nTupleTable.get("##SYMBOL");
      int index1 = list.indexOf(plotSymbols[0]);
      int index2 = list.indexOf(plotSymbols[1]);

      list = nTupleTable.get("##VARNAME");
      spec.varName = list.get(index2).toUpperCase();

      list = nTupleTable.get("##FACTOR");
      spec.xFactor = Double.parseDouble(list.get(index1));
      spec.yFactor = Double.parseDouble(list.get(index2));

      list = nTupleTable.get("##LAST");
      spec.fileLastX = Double.parseDouble(list.get(index1));

      list = nTupleTable.get("##FIRST");
      spec.fileFirstX = Double.parseDouble(list.get(index1));
      //firstY = Double.parseDouble((String)list.get(index2));

      list = nTupleTable.get("##VARDIM");
      spec.nPointsFile = Integer.parseInt(list.get(index1));

      list = nTupleTable.get("##UNITS");
      spec.setXUnits(list.get(index1));
      spec.setYUnits(list.get(index2));

      if (spec.nucleusX == null && (list = nTupleTable.get("##.NUCLEUS")) != null) {
        spec.setNucleus(list.get(0), false);
        spec.setNucleus(list.get(index1), true);
      } else {
      	if (spec.nucleusX == null)
          spec.nucleusX = "?";
      }

      decompressData(spec, minMaxY);
      return true;
    }
    if (spec.dataClass.equals("PEAKTABLE") || spec.dataClass.equals("XYPOINTS")) {
      spec.setContinuous(spec.dataClass.equals("XYPOINTS"));
      list = nTupleTable.get("##SYMBOL");
      int index1 = list.indexOf(plotSymbols[0]);
      int index2 = list.indexOf(plotSymbols[1]);

      list = nTupleTable.get("##UNITS");
      spec.setXUnits(list.get(index1));
      spec.setYUnits(list.get(index2));
      spec.setXYCoords(Coordinate.parseDSV(t.getValue(), spec.xFactor, spec.yFactor));
      return true;
    }
    return false;
  }

  private void decompressData(JDXDataObject spec, double[] minMaxY) {

    int errPt = errorLog.length();
    double fileDeltaX = Coordinate.deltaX(spec.fileLastX, spec.fileFirstX,
        spec.nPointsFile);
    spec.setIncreasing(fileDeltaX > 0);
    spec.setContinuous(true);
    JDXDecompressor decompressor = new JDXDecompressor(t, spec.fileFirstX,
        spec.xFactor, spec.yFactor, fileDeltaX, spec.nPointsFile);

    double[] firstLastX = new double[2];
    long t = System.currentTimeMillis();
    Coordinate[] xyCoords = decompressor.decompressData(errorLog, firstLastX);
    System.out.println("decompression time = " + (System.currentTimeMillis() - t) + " ms");
    spec.setXYCoords(xyCoords);
    double d = decompressor.getMinY();
    if (minMaxY != null) {
      if (d < minMaxY[0])
        minMaxY[0] = d;
      d = decompressor.getMaxY();
      if (d > minMaxY[1])
        minMaxY[1] = d;
    }
    double freq = (Double.isNaN(spec.freq2dX) ? spec.observedFreq
        : spec.freq2dX);
    // apply offset
    if (spec.offset != JDXDataObject.ERROR && freq != JDXDataObject.ERROR
        && spec.dataType.toUpperCase().contains("SPECTRUM")) {
      Coordinate
          .applyShiftReference(xyCoords, spec.dataPointNum, spec.fileFirstX,
              spec.fileLastX, spec.offset, freq, spec.shiftRefType);
    }

    if (freq != JDXDataObject.ERROR && spec.getXUnits().toUpperCase().equals("HZ")) {
      double xScale = freq;
      Coordinate.applyScale(xyCoords, (1 / xScale), 1);
      spec.setXUnits("PPM");
      spec.setHZtoPPM(true);
    }
    if (errorLog.length() != errPt) {
      errorLog.append(spec.getTitle()).append("\n");
      errorLog.append("firstX: " + spec.fileFirstX + " Found " + firstLastX[0]
          + "\n");
      errorLog.append("lastX from Header " + spec.fileLastX + " Found "
          + firstLastX[1] + "\n");
      errorLog.append("deltaX from Header " + fileDeltaX + "\n");
      errorLog.append("Number of points in Header " + spec.nPointsFile
          + " Found " + xyCoords.length + "\n");
    } else {
      //errorLog.append("No Errors decompressing data\n");
    }

    if (Logger.debugging) {
      System.err.println(errorLog.toString());
    }

  }

  public static void addHeader(List<String[]> table, String label, String value) {
    String[] entry;
    for (int i = 0; i < table.size(); i++)
      if ((entry = table.get(i))[0].equals(label)) {
        entry[1] = value;
        return;
      }
    table.addLast(new String[] { label, value, JDXSourceStreamTokenizer.cleanLabel(label) });
  }

}
