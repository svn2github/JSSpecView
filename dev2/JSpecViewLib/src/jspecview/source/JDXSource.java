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

import java.util.ArrayList;
import java.util.List;

import jspecview.common.JDXHeader;
import jspecview.common.JDXSpectrum;

/**
 * <code>JDXSource</code> is representation of all the data in the JCAMP-DX file
 * or source. Note: All Jdx Source are viewed as having a set of Spectra
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 */
public class JDXSource extends JDXHeader {

  public final static int TYPE_OVERLAY = -2;
  
  public final static int TYPE_UNKNOWN = -1;
  /** Indicates a Simple Source */
  public final static int TYPE_SIMPLE = 0;
  /** Indicates a Block Source */
  public final static int TYPE_BLOCK = 1;
  /** Indicates a Ntuple Source */
  public final static int TYPE_NTUPLE = 2;

  public int type = TYPE_SIMPLE;
  public boolean isCompoundSource = false;
  
  // List of JDXSpectra
  protected List<JDXSpectrum> jdxSpectra;

  public int peakCount;
  
  
  public JDXSource(int type) {
    this.type = type;
    headerTable = new ArrayList<String[]>();
    jdxSpectra = new ArrayList<JDXSpectrum>();
    isCompoundSource = (type != TYPE_SIMPLE);
  }

  /**
   * Returns the Spectrum at a given index in the list
   * 
   * @param index
   *        the spectrum index
   * @return the Spectrum at a given index in the list
   */
  public JDXSpectrum getJDXSpectrum(int index) {
    return (jdxSpectra.size() <= index ? null : jdxSpectra.get(index));
  }

  /**
   * Adds a Spectrum to the list
   * 
   * @param spectrum
   *        the spectrum to be added
   */
  public void addJDXSpectrum(JDXSpectrum spectrum) {
    jdxSpectra.add(spectrum);
  }

  /**
   * Returns the number of Spectra in this Source
   * 
   * @return the number of Spectra in this Source
   */
  public int getNumberOfSpectra() {
    return jdxSpectra.size();
  }

  /**
   * Returns the Vector of Spectra
   * 
   * @return the Vector of Spectra
   */
  public List<JDXSpectrum> getSpectra() {
    return jdxSpectra;
  }

  // Errors
  String errors = "";

  /**
   * Returns the error log for this source
   * 
   * @return the error log for this source
   */
  public String getErrorLog() {
    return errors;
  }

  /**
   * Sets the error log for this source
   * 
   * @param errors
   *        error log for this source
   */
  public void setErrorLog(String errors) {
    this.errors = errors;
  }

  private String filePath;

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }
  
  public String getFilePath() {
    return filePath;
  }

  public static JDXSource createOverlay(String name, List<JDXSpectrum> specs) {
    JDXSource source = new JDXSource(TYPE_OVERLAY);
    for (int i = 0; i < specs.size(); i++)
      source.addJDXSpectrum(specs.get(i));
    return source;
  }

  public boolean isOverlay() {
    return type == TYPE_OVERLAY;
  }
}
