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
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import jspecview.common.JDXSpectrum;
import jspecview.common.JSpecViewUtils;
import jspecview.exception.JSpecViewException;
import jspecview.util.FileManager;

/**
 * <code>JDXSource</code> is representation of all the data in the JCAMP-DX file
 * or source.
 * Note: All Jdx Source are viewed as having a set of Spectra
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 */
public abstract class JDXSource {


  /**
   * The labels for various tabular data
   */
  public static final String[] TABULAR_DATA_LABELS = {"##XYDATA", "##XYPOINTS",
                                                "##PEAKTABLE", "##DATATABLE",
                                                "##PEAKASSIGNMENTS"};

  /**
   * The variable list for the tabular data labels
   */
  public static final String[][] VAR_LIST_TABLE =
    {
      {"PEAKTABLE", "XYDATA", "XYPOINTS"},
      {"(XY..XY)", "(X++(Y..Y))", "(XY..XY)"}
    };

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
  public JDXSource(){
    headerTable = new HashMap<String, String>();
    jdxSpectra = new Vector<JDXSpectrum>();
  }

  /**
   * Returns the Spectrum at a given index in the list
   * @param index the spectrum index
   * @return the Spectrum at a given index in the list
   */
  public JDXSpectrum getJDXSpectrum(int index){
    return jdxSpectra.size() <= index ? null : (JDXSpectrum)jdxSpectra.elementAt(index);
  }

  /** Indicates a Simple Source */
  public final static int SIMPLE = 0;
  /** Indicates a Block Source */
  public final static int BLOCK = 1;
  /** Indicates a Ntuple Source */
  public final static int NTUPLE = 2;

  private static JDXSource getXMLSource(String source) {
    String xmlType = source.substring(0,400).toLowerCase();

    if (xmlType.contains("<animl")) {
      return AnIMLSource.getAniMLInstance(new ByteArrayInputStream(source.getBytes()));
    }
    else if (xmlType.contains("xml-cml")) {
      return CMLSource.getCMLInstance(new ByteArrayInputStream(source.getBytes()));
    }
    return null;
  }

  public static JDXSource createJDXSource(String sourceContents, String filePath,
                                       URL appletDocumentBase)
    throws IOException, JSpecViewException
  {
    InputStream in = null;
    System.out.println("createJDXSource " + filePath + " " + sourceContents + " " +
                       appletDocumentBase);
 
    if (filePath != null) {
      in = FileManager.getInputStream(filePath, true, appletDocumentBase);
      sourceContents = getContentFromInputStream(in); 

      JDXSource xmlSource = getXMLSource(sourceContents);
      if (xmlSource != null) {
        return xmlSource;
      }
    }

    int sourceType = determineJDXSourceType(sourceContents);
    
    if (sourceType == -1) {
      throw new JSpecViewException("JDX Source Type not Recognized");
    }
    try {
      switch (sourceType) {
      case SIMPLE:
        return SimpleSource.getInstance(sourceContents);
      case BLOCK:
        return BlockSource.getInstance(sourceContents);
      case NTUPLE:
        return NTupleSource.getInstance(sourceContents);
      // return RestrictedNTupleSource.getInstance(sourceContents, 128);
      default:
        throw new JSpecViewException("Unknown or unrecognised JCAMP-DX format");
      }
    } catch (JSpecViewException e) {
      throw new JSpecViewException("Error reading JDX format: " + e.getMessage());
    }
  }
  

  public static JDXSource createJDXSource(InputStream in)
    throws IOException, JSpecViewException{
	  
	  String sourceContents = getContentFromInputStream(in); 
	  
	  return createJDXSource(sourceContents, null, null);
  }

  public static JDXSource createJDXSource(String sourceContents)
  throws IOException, JSpecViewException{
	  	 	  
	 return createJDXSource(sourceContents, null, null);
}
  
  private static String getContentFromInputStream(InputStream in)
    throws IOException
  {
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
   * @param sourceContents the contents of the source
   * @return the JDX source type
   */
  private static int determineJDXSourceType(String sourceContents){
    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(sourceContents);
    String label;
    while(t.hasMoreTokens()){
      t.nextToken();
      label = JSpecViewUtils.cleanLabel(t.label);
      if (label.equals("##DATATYPE") && t.value.toUpperCase().equals("LINK")){

        return BLOCK;
      }
      if (label.equals("##DATACLASS") && t.value.toUpperCase().equals("NTUPLES")) {
          return NTUPLE;
      }
      Arrays.sort(JDXSource.TABULAR_DATA_LABELS);
      if(Arrays.binarySearch(JDXSource.TABULAR_DATA_LABELS, label) > 0)
        return SIMPLE;
    }
    return -1;
  }

  /**
   * Adds a Spectrum to the list
   * @param spectrum the spectrum to be added
   */
  public void addJDXSpectrum(JDXSpectrum spectrum){
    jdxSpectra.addElement(spectrum);
  }

  /**
   * Returns the number of Spectra in this Source
   * @return the number of Spectra in this Source
   */
  public int getNumberOfSpectra(){
    return jdxSpectra.size();
  }


  /**
   * Returns the Vector of Spectra
   * @return the Vector of Spectra
   */
  public Vector<JDXSpectrum> getSpectra(){
    return jdxSpectra;
  }

  /**
   * Returns the header table of the JDXSource
   * @return the header table of the JDXSource
   */
  public Map<String, String> getHeaderTable(){
    return headerTable;
  }


  /**
   * Sets the headertable for this Source
   * @param table the header table
   */
  public void setHeaderTable(Map<String, String> table){
    headerTable = table;
  }

  /**
   * Returns the error log for this source
   * @return the error log for this source
   */
  public String getErrorLog(){
    return errors;
  }

  /**
   * Sets the error log for this source
   * @param errors error log for this source
   */
  public void setErrorLog(String errors){
    this.errors = errors;
  }
}
