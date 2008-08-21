/* Copyright (c) 2002-2007 The University of the West Indies
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
 * class <code>AnIMLExporter</code> contains static methods to export a Graph as
 * as AnIML. <code>AnIMLExporter</code> uses <a href="http://jakarta.apache.org/velocity/">Velocity</a>
 * to write to a template file called 'animl_tmp.vm' or 'animl_nmr.vm'. So any changes in design should
 * be done in these files.
 * @see jspecview.common.Graph
 * @author Prof Robert J. Lancashire
 */
class AnIMLExporter extends XMLExporter {

  /**
   * Exports the Spectrum that is displayed by JSVPanel to a file given by fileName
   * If display is zoomed then export the current view
   * @param spec the spectrum to export
   * @param fileName the name of the file
   * @param startIndex the starting point of the spectrum
   * @param endIndex the end point
   * @throws IOException
   */
  void exportAsAnIML(JDXSpectrum spec, String fileName, int startIndex,
                     int endIndex) throws IOException {

    this.startIndex = startIndex;
    this.endIndex = endIndex;

    if (!setParameters(spec))
      return;
    
    setWriter(fileName);

    double amlFirstX = xyCoords[startIndex].getXVal();

    if (SolvName.equals(""))
      SolvName = "unknown";

    String AnIMLtemplate = "animl_tmp.vm";
    if (datatype.contains("MASS")) {
      spectypeInitials = "MS";
    } else if (datatype.contains("INFRARED")) {
      spectypeInitials = "IR";
    } else if (datatype.contains("UV") || (datatype.contains("VIS"))) {
      spectypeInitials = "UV";
    } else if (datatype.contains("NMR")) {
      amlFirstX = amlFirstX * ObFreq; // NMR stored internally as ppm
      deltaX = deltaX * ObFreq; // convert to Hz before exporting
      AnIMLtemplate = "animl_nmr.vm";
      spectypeInitials = "NMR";
    }

    pathlength = (pathlength.equals("") && spectypeInitials.equals("UV")? "1.0" : "-1");

    setTemplate(AnIMLtemplate);

    context.put("amlHead", head);
    context.put("firstX", new Double(amlFirstX));

    context.put("xdata_type", "Float32");
    context.put("ydata_type", "Float32");


    if (vendor.equals(""))
      vendor = "not available from JCAMP-DX file";
    if (model.equals(""))
      model = "not available from JCAMP-DX file";

    context.put("vendor", vendor);
    context.put("model", model);

    writeXML();
  }

}
