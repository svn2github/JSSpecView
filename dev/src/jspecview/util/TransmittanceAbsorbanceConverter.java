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

package jspecview.util;

import java.util.Vector;

import jspecview.common.JDXSpectrum;

/**
 * class <code>TransmittanceAbsorbanceConverter</code> converts a <code>
 * JDXSpectrum</code> from Transmittance to Absorbance and vice versa.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */

public class TransmittanceAbsorbanceConverter {

  /**
   * Converts and returns a converted spectrum. If original was Absorbance then
   * a Transmittance spectrum is returned and vice versa or null if spectrum was
   * neither Absorbance nor Transmittance then null is returned
   * @param spectrum the JDXSpectrum
   * @return the converted spectrum
   */
  public static JDXSpectrum convert(JDXSpectrum spectrum){
    JDXSpectrum convertedSpectrum = AbsorbancetoTransmittance(spectrum);

    return (convertedSpectrum != null ? convertedSpectrum :
            TransmittanceToAbsorbance(spectrum));
  }

  /**
   * Converts a spectrum from Absorbance to Transmittance
   * @param spectrum the JDXSpectrum
   * @return the converted spectrum
   */
  public static JDXSpectrum AbsorbancetoTransmittance(JDXSpectrum spectrum){
    JDXSpectrum convSpectrum = new JDXSpectrum();
    String yUnits = spectrum.getYUnits();
    Coordinate[] xyCoords = spectrum.getXYCoords();
    Vector<Coordinate> newXYCoords = new Vector<Coordinate>();
    double y;
    String newYInits;

    if(yUnits.toLowerCase().equals("absorbance") ||
            yUnits.toLowerCase().equals("a")){
      double min = 0;
      double max = 4;
      if(!isYInRange(spectrum, min, max)){
        xyCoords = normalise(spectrum, min, max);
      }
      for(int i = 0; i < xyCoords.length; i++){
        y = xyCoords[i].getYVal();
        y = toTransmittance(y);
        newXYCoords.addElement(new Coordinate(xyCoords[i].getXVal(), y));
      }

      newYInits = "TRANSMITTANCE";

      // Initialise convSpectrum
      convSpectrum = spectrum.copy();
      convSpectrum.setOrigin("JSpecView Converted");
      convSpectrum.setOwner("JSpecView Generated");
      convSpectrum.setXYCoords(
          (Coordinate[])newXYCoords.toArray(new Coordinate[newXYCoords.size()]));
      convSpectrum.setYUnits(newYInits);

      return convSpectrum;
    }
    else{
      return null;
    }
  }

  /**
   * Converts a spectrum from Transmittance to Absorbance
   * @param spectrum the JDXSpectrum
   * @return the converted spectrum
   */
  public static JDXSpectrum TransmittanceToAbsorbance(JDXSpectrum spectrum){
    JDXSpectrum convSpectrum = new JDXSpectrum();
    String yUnits = spectrum.getYUnits();
    Coordinate[] xyCoords = spectrum.getXYCoords();
    Vector<Coordinate> newXYCoords = new Vector<Coordinate>();
    double y;
    String newYInits;

    if(yUnits.toLowerCase().equals("transmittance") ||
       yUnits.toLowerCase().equals("t")){
      if(isYInRange(spectrum, -2, 2)){
        for(int i = 0; i < xyCoords.length; i++){
          y = xyCoords[i].getYVal();
          if(y <= 0)
            continue;
          y = TtoAbsorbance(y);
          newXYCoords.addElement(new Coordinate(xyCoords[i].getXVal(), y));
        }
      }
      else{ // is % Transmittance
        for(int i = 0; i < xyCoords.length; i++){
          y = xyCoords[i].getYVal();
          if(y <= 0 /*|| y > 100*/)
            continue;
          y = percentTtoAbsorbance(y);
          newXYCoords.addElement(new Coordinate(xyCoords[i].getXVal(), y));
        }
      }
      newYInits = "ABSORBANCE";

      // Initialise convSpectrum
      convSpectrum = spectrum.copy();
      convSpectrum.setOrigin("JSpecView Converted");
      convSpectrum.setOwner("JSpecView Generated");
      convSpectrum.setXYCoords(
          (Coordinate[])newXYCoords.toArray(new Coordinate[newXYCoords.size()]));
      convSpectrum.setYUnits(newYInits);

      return convSpectrum;
    }
    else{
      return null;
    }
  }

  /**
   * Converts a value in Transmittance to Absorbance
   * @param T the value in Transmittance
   * @return the value in Absorbance
   */
  public static double TtoAbsorbance(double T){
    return log10(1/T);
  }

  /**
   * Converts a value in percent Transmittance to Absorbance
   * @param percentT the value in % Transmittance
   * @return the value in Absorbance
   */
  public static double percentTtoAbsorbance(double percentT){
    return (2 - log10(percentT));
  }

  /**
   * Converts a value from Absorbance to Transmittance
   * @param A the value in Absorbance
   * @return the value in Transmittance
   */
  public static double toTransmittance(double A){
    return Math.pow(10, -A);
  }

  /**
   * Returns the log of a value to the base 10
   * @param value the input value
   * @return the log of a value to the base 10
   */
  public static double log10(double value) {
    return Math.log(value) / Math.log(10);
  }

  /**
   * Determines if a y values of a spectrum are in a certain range
   * @param spectrum the spectrum
   * @param minRange the minimum of the range
   * @param maxRange the maximum of the range
   * @return true is in range, otherwise false
   */
  private static boolean isYInRange(JDXSpectrum spectrum, double minRange, double maxRange){
    Coordinate[] xyCoords = spectrum.getXYCoords();
    double minY = JSpecViewUtils.getMinY(xyCoords);
    double maxY = JSpecViewUtils.getMaxY(xyCoords);

    if(minY >= minRange  && maxY <= maxRange)
      return true;
    return false;
  }

  /**
   * Normalises the y values of a spectrum to a certain range
   * @param spectrum the spectrum
   * @param min the minimum value of the range
   * @param max the maximum value of the range
   * @return array of normalised coordinates
   */
  private static Coordinate[] normalise(JDXSpectrum spectrum, double min, double max){
    Coordinate[] xyCoords = spectrum.getXYCoords();
    Coordinate[] newXYCoords = new Coordinate[xyCoords.length];
    double minY = JSpecViewUtils.getMinY(xyCoords);
    double maxY = JSpecViewUtils.getMaxY(xyCoords);
    double y, newY;

    double factor = (maxY - minY)/(max - min); // range = 0-5
    for(int i = 0; i < xyCoords.length; i++){
      y = xyCoords[i].getYVal();
      newY = ((y - minY) / factor) - min;
      newXYCoords[i] = new Coordinate(xyCoords[i].getXVal(), newY);
    }

    return newXYCoords;
  }

}
