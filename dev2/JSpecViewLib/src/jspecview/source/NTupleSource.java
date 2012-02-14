/* Copyright (c) 2002-2009 The University of the West Indies
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
import java.io.StringReader;
import java.util.ArrayList;
//import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import jspecview.common.JDXSpectrum;
import jspecview.common.JSpecViewUtils;
import jspecview.exception.JDXSourceException;
import jspecview.exception.JSpecViewException;

/**
 * Representation of an JCAMP-DX NTuple Source.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 */
public class NTupleSource extends JDXSource {

  /**
   * Constructs a new NTupleSource from the source Listing
   */
  protected NTupleSource() {
    super();
    isCompoundSource = true;
  }

  /**
   * Does the actual work of initializing the Source instance
   * 
   * @param sourceContents
   *        the contents of the source as a String
   * @throws JSpecViewException
   * @return an instance of an NTupleSource
   */
  public static NTupleSource getInstance(String sourceContents)
      throws JSpecViewException {

    JDXSpectrum spectrum = null;
    HashMap<String, String> LDRTable;
    HashMap<String, String> sourceLDRTable = new HashMap<String, String>();
    HashMap<String, ArrayList<String>> nTupleTable = new HashMap<String, ArrayList<String>>();

    JDXSpectrum spectrum0 = new JDXSpectrum();

    String page = "";
    String[] plotSymbols = new String[2];
    String tabularSpecData = null;
    int tabDataLineNo = 0;

    NTupleSource ns = new NTupleSource();

    StringBuffer errorLog = new StringBuffer();
    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(sourceContents);

    // Read Source Specific Header
    String label = "";
    while (t.hasMoreTokens() && t.nextToken()
        && !(label = JSpecViewUtils.cleanLabel(t.label)).equals("##NTUPLES"))
      if (!readDataLabel(ns, spectrum0, label, t.value, errorLog, sourceLDRTable))
        sourceLDRTable.put(label, t.value);
    //Finished Pulling out the LDR Table Data

    ns.setHeaderTable(sourceLDRTable);

    /*--------------------------------------------*/
    /*------------- Fetch Page Data --------------*/

    if (!label.equals("##NTUPLES"))
      throw new JSpecViewException("Invalid NTuple Source");

    // Read NTuple Table
    while (t.hasMoreTokens() && t.nextToken() && !(label = JSpecViewUtils.cleanLabel(t.label)).equals("##PAGE")) {
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

      if (!spectrum.createXYCoords(nTupleTable, plotSymbols, spectrum.getDataType(),
          tabularSpecData, tabDataLineNo, errorLog))
        throw new JDXSourceException("Unable to read Ntuple Source");
      for (Iterator<String> iter = sourceLDRTable.keySet().iterator(); iter
          .hasNext();) {
        String key = iter.next();
        if (!key.equals("##TITLE") && !key.equals("##DATACLASS")
            && !key.equals("##NTUPLES"))
          LDRTable.put(key, sourceLDRTable.get(key));
      }
      spectrum.setHeaderTable(LDRTable);

      ns.addJDXSpectrum(spectrum);
      spectrum = null;
    }
    errorLog.append(ERROR_SEPARATOR);
    ns.setErrorLog(errorLog.toString());
    return ns;
  }

}
