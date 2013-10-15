/**
 * 
 */
package jspecview.common;

import jspecview.source.JDXSpectrum;

public class Integral extends Measurement {

	public Integral setInt(double x1, double y1, JDXSpectrum spec, double value, double x2, double y2) {
		setA(x1, y1, spec, "", false, false, 0, 6);
		setPt2(x2, y2);
    setValue(value);
		return this;
  }
  
	
}