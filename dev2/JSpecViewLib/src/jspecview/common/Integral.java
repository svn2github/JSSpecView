/**
 * 
 */
package jspecview.common;

class Integral extends Measurement {

	Integral(double x1, double y1) {
		super(x1, y1);
	}
	
	Integral setInt(JDXSpectrum spec, double value, double x2, double y2) {
		setAll(spec, "", false, false, 0, 6);
		setPt2(x2, y2);
    setValue(value);
		return this;
  }
  
	
}