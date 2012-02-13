/* Copyright (c) 2002-2010 The University of the West Indies
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

import java.util.Arrays;
import java.util.HashMap;

import jspecview.common.JDXSpectrum;
import jspecview.common.JSpecViewUtils;
import jspecview.exception.JDXSourceException;
import jspecview.exception.JSpecViewException;

/**
 * Representation of a JDX Simple Source.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof. Robert J. Lancashire
 */

public class SimpleSource extends JDXSource {

  /**
   * Constructs a new SimpleSource
   */
  protected SimpleSource() {
    super();
  }

  /**
   * Does the actual work of initializing the SimpleSource from the the contents
   * of the source
   * 
   * @param sourceContents
   *        the contents of the source as a String
   * @return an instance of a SimpleSource
   * @throws JSpecViewException
   */
  public static SimpleSource getInstance(String sourceContents)
      throws JSpecViewException {

    // The SimpleSouce Instance
    SimpleSource ss = new SimpleSource();

    //Calendar now = Calendar.getInstance();
    //SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSS ZZZZ");
    //String currentTime =  formatter.format(now.getTime());

    JDXSpectrum spectrum = new JDXSpectrum();

    // The data Table
    String tabularSpecData = null;

    // Table for header information
    HashMap<String, String> notesLDRTable = new HashMap<String, String>(20);

    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(sourceContents);
    String label = "";
    int tabularDataLabelLineNo = 0;

    StringBuffer errorLog = new StringBuffer();

    while (t.hasMoreTokens() && t.nextToken()
        && !(label = JSpecViewUtils.cleanLabel(t.label)).equals("##END")) {

      if (checkCommon(ss, spectrum, label, t.value, errorLog, notesLDRTable))
        continue;

      if (Arrays.binarySearch(TABULAR_DATA_LABELS, label) > 0) {
        tabularDataLabelLineNo = t.labelLineNo;
        tabularSpecData = spectrum.getTabularSpecData(label, t.value);
        continue;
      }
      
      notesLDRTable.put(label, t.value);
    }

    if (!label.equals("##END"))
      tabularSpecData = null;

    if (!spectrum.createXYCoords(tabularSpecData, tabularDataLabelLineNo,
        notesLDRTable, errorLog))
      throw new JDXSourceException("Unable to read Simple Source");

    errorLog.append(ERROR_SEPARATOR);

    ss.addJDXSpectrum(spectrum);
    return ss;
  }
}
