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

import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;

import jspecview.common.JDXSpectrum;
import jspecview.common.JSpecViewUtils;
import jspecview.exception.JDXSourceException;
import jspecview.exception.JSpecViewException;
/**
 * <code>BlockSource</code> class is a representation of a JCAMP-DX Block file.
 * This class is not intialised directly. Instead <code>JDXSourceFactory</code>
 * is used to determine the type of JCAMP-DX source from a stream and returns
 * an instance of the appropriate class
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @see jspecview.source.CompoundSource
 * @see jspecview.source.NTupleSource
 */
public class BlockSource extends JDXSource {

  /**
   * Creates a new Block Source
   */
  protected BlockSource() {
    super();
    isCompoundSource = true;
  }

  /**
   * Does the work of initializing the BlockSource from the source String
   * 
   * @param sourceContents
   *        contents of the source as a String
   * @return an instance of a BlockSource
   * @throws JSpecViewException
   */
  public static BlockSource getInstance(String sourceContents)
      throws JSpecViewException {

    HashMap<String, String> LDRTable;
    HashMap<String, String> sourceLDRTable = new HashMap<String, String>();
    String label, tmp;

    StringBuffer errorLog = new StringBuffer();

    BlockSource bs = new BlockSource();

    int tabDataLineNo = 0;
    String tabularSpecData = null;

    JDXSpectrum spectrum;

    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(sourceContents);

    // Get the LDRs up to the ##TITLE of the first block
    t.nextToken();
    label = JSpecViewUtils.cleanLabel(t.label);
    if (!label.equals("##TITLE")) {
      throw new JSpecViewException("Error Reading Source");
    }

    checkCommon(bs, bs, label, t.value, errorLog, sourceLDRTable);

    while (t.hasMoreTokens() && t.nextToken()
        && !(label = JSpecViewUtils.cleanLabel(t.label)).equals("##TITLE")) {
      if (!checkCommon(bs, bs, label, t.value, errorLog, sourceLDRTable))
        sourceLDRTable.put(t.label, t.value);
    }

    bs.setHeaderTable(sourceLDRTable);

    // If ##TITLE not found throw Exception
    if (!label.equals("##TITLE")) {
      throw new JSpecViewException("Unable to Read Block Source");
    }
    spectrum = new JDXSpectrum();
    LDRTable = new HashMap<String, String>();

    checkCommon(bs, spectrum, label, t.value, errorLog, LDRTable);

    try {
      while (t.hasMoreTokens()
          && t.nextToken()
          && (!(tmp = JSpecViewUtils.cleanLabel(t.label)).equals("##END") || !label
              .equals("##END"))) {
        label = tmp;
        System.out.println(label);
        if (label.equals("##JCAMPCS")) {
          do {
            t.nextToken();
            label = JSpecViewUtils.cleanLabel(t.label);
          } while (!label.equals("##TITLE"));
          spectrum = new JDXSpectrum();
          continue;
        }

        if (checkCommon(bs, spectrum, label, t.value, errorLog, LDRTable))
          continue;

        if (Arrays.binarySearch(TABULAR_DATA_LABELS, label) > 0) {
          tabDataLineNo = t.labelLineNo;
          tabularSpecData = spectrum.getTabularSpecData(label, t.value);
          continue;
        }

        // Process Block
        if (label.equals("##END")) {

          if (!spectrum.createXYCoords(tabularSpecData, tabDataLineNo,
              LDRTable, errorLog))
            throw new JDXSourceException("Unable to read Block Source");

          bs.addJDXSpectrum(spectrum);

          tabularSpecData = null;
          spectrum = new JDXSpectrum();
          LDRTable = new HashMap<String, String>();
          continue;
        } // End Process Block

        LDRTable.put(label, t.value);

      } // End Source File
    } catch (NoSuchElementException nsee) {
      throw new JSpecViewException("Unable to Read Block Source");
    } catch (JSpecViewException jsve) {
      throw jsve;
    }
    errorLog.append(ERROR_SEPARATOR);
    bs.setErrorLog(errorLog.toString());
    return bs;
  }

}
