/* Copyright (c) 2006-2007 The University of the West Indies
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

package jspecview.export;


import java.io.IOException;
import jspecview.common.JDXSpectrum;

/**
 * class <code>CMLExporter</code> contains static methods to export a Graph as
 * as CIML. <code>CMLExporter</code> uses <a href="http://jakarta.apache.org/velocity/">Velocity</a>
 * to write to a template file called 'cml_tmp.vm' or 'cml_nmr.vm'. So any changes in design should
 * be done in these files.
 * @see jspecview.common.Graph
 * @author Prof Robert J. Lancashire
 */
class CMLExporter extends XMLExporter {

  /**
   * Exports the Spectrum that is displayed by JSVPanel to a file given by fileName
   * If display is zoomed then export the current view
   * @param spec the spectrum to export
   * @param fileName the name of the file
   * @param startIndex the starting point of the spectrum
   * @param endIndex the end point
   * @throws IOException
   */
  void exportAsCML(JDXSpectrum spec, String fileName, int startIndex,
                   int endIndex) throws IOException {

    this.startIndex = startIndex;
    this.endIndex = endIndex;

    if (!setParameters(spec))
      return;


    setWriter(fileName);

    double cmlFirstX = xyCoords[startIndex].getXVal();
    double cmlLastX = xyCoords[endIndex].getXVal();

    if (model.equals(""))
      model = "unknown";

    if (xUnits.toLowerCase().equals("m/z"))
      xUnits = "moverz";
    if (xUnits.toLowerCase().equals("1/cm"))
      xUnits = "cm-1";
    if (xUnits.toLowerCase().equals("nanometers"))
      xUnits = "nm";

    if (datatype.contains("MASS"))
      spectypeInitials = "massSpectrum";
    if (datatype.contains("INFRARED")) {
      spectypeInitials = "infrared";
      //         if (xUnits.toLowerCase().contains("cm") )
      //            xUnits="cm-1";
    }
    if (datatype.contains("UV") || (datatype.contains("VIS")))
      spectypeInitials = "UV/VIS";

    String CMLtemplate = "cml_tmp.vm";
    if (datatype.contains("NMR")) {
      cmlFirstX *= ObFreq; // NMR stored internally as ppm
      cmlLastX *= ObFreq;
      deltaX *= ObFreq; // convert to Hz before exporting
      CMLtemplate = "cml_nmr.vm";
      spectypeInitials = "NMR";
    }

    setTemplate(CMLtemplate);

    String ident = spectypeInitials + "_"
        + title.substring(0, Math.min(10, title.length()));
    context.put("ident", ident);
    context.put("firstX", new Double(cmlFirstX));
    context.put("lastX", new Double(cmlLastX));
    context.put("continuous", Boolean.valueOf(spec.isContinuous()));

    writeXML();
  }
}
