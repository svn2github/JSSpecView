/**
 * 
 */
package jspecview.common;

class Integral extends Measurement {

	Integral(JDXSpectrum spec, double value, double x1, double x2, double y1, double y2) {
  	super(spec, x1, y1, "", x2, y2);
    setValue(value);
  }
  
	
}