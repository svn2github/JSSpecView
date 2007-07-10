/* Copyright (c) 2002-2006 The University of the West Indies
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

package jspecview.util;

import java.io.IOException;
import java.io.Writer;

import jspecview.common.JDXSpectrum;

/**
 * class <code>JDXExporter</code> contains static methods for exporting a
 * JCAMP-DX Spectrum in one of the compression formats DIF, FIX, PAC, SQZ or
 * as x, y values.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */

public class JDXExporter {

  /**
   * Exports spectrum in DIF format starting from coordinate index startIndex
   * to endIndex
   * @param spectrum the spectrum
   * @param writer the writer to export to
   * @param startIndex the start coordinate index
   * @param endIndex the end coordinate index
   * @throws IOException
   */
  public static void exportDIF(JDXSpectrum spectrum, Writer writer,
                               int startIndex, int endIndex) throws IOException{
    writer.write(spectrum.toDIF(startIndex, endIndex));
    writer.close();
  }

  /**
   * Exports spectrum in DIF format
   * @param spectrum the spectrum
   * @param writer the writer to export to
   * @throws IOException
   */
  public static void exportDIF(JDXSpectrum spectrum, Writer writer)
      throws IOException{
    writer.write(spectrum.toDIF());
    writer.close();
  }

  /**
   * Exports spectrum in FIX format starting from coordinate index startIndex
   * to endIndex
   * @param spectrum the spectrum
   * @param writer the writer to export to
   * @param startIndex the start coordinate index
   * @param endIndex the end coordinate index
   * @throws IOException
   */
  public static void exportFIX(JDXSpectrum spectrum, Writer writer, int startIndex, int endIndex) throws IOException{
    writer.write(spectrum.toFIX(startIndex, endIndex));
    writer.close();
  }

  /**
   * Exports spectrum in FIX format
   * @param spectrum the spectrum
   * @param writer the writer to export to
   * @throws IOException
   */
  public static void exportFIX(JDXSpectrum spectrum, Writer writer) throws IOException{
    writer.write(spectrum.toFIX());
    writer.close();
  }


  /**
     * Exports spectrum in SQZ format starting from coordinate index startIndex
     * to endIndex
     * @param spectrum the spectrum
     * @param writer the writer to export to
     * @param startIndex the start coordinate index
     * @param endIndex the end coordinate index
     * @throws IOException
   */
  public static void exportSQZ(JDXSpectrum spectrum, Writer writer, int startIndex, int endIndex) throws IOException{
    writer.write(spectrum.toSQZ(startIndex, endIndex));
    writer.close();
  }

  /**
     * Exports spectrum in SQZ format
     * @param spectrum the spectrum
     * @param writer the writer to export to
     * @throws IOException
   */
  public static void exportSQZ(JDXSpectrum spectrum, Writer writer) throws IOException{
    writer.write(spectrum.toSQZ());
    writer.close();
  }

  /**
     * Exports spectrum in PAC format starting from coordinate index startIndex
     * to endIndex
     * @param spectrum the spectrum
     * @param writer the writer to export to
     * @param startIndex the start coordinate index
     * @param endIndex the end coordinate index
     * @throws IOException
   */
  public static void exportPAC(JDXSpectrum spectrum, Writer writer, int startIndex, int endIndex) throws IOException{
    writer.write(spectrum.toPAC(startIndex, endIndex));
    writer.close();
  }

  /**
   * Exports spectrum in PAC format
   * @param spectrum the spectrum
   * @param writer the writer to export to
   * @throws IOException
   */
  public static void exportPAC(JDXSpectrum spectrum, Writer writer) throws IOException{
    writer.write(spectrum.toPAC());
    writer.close();
  }

  /**
     * Exports spectrum in X,Y format starting from coordinate index startIndex
     * to endIndex
     * @param spectrum the spectrum
     * @param writer the writer to export to
     * @param startIndex the start coordinate index
     * @param endIndex the end coordinate index
     * @throws IOException
   */
  public static void exportXY(JDXSpectrum spectrum, Writer writer, int startIndex, int endIndex) throws IOException{
    writer.write(spectrum.toXY(startIndex, endIndex));
    writer.close();
  }


  /**
   * Exports spectrum in X,Y format
   * @param spectrum the spectrum
   * @param writer the writer to export to
   * @throws IOException
   */
  public static void exportXY(JDXSpectrum spectrum, Writer writer) throws IOException{
    writer.write(spectrum.toXY());
    writer.close();
  }

}
