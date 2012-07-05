/**
 * 
 */
package jspecview.common;

import java.util.Comparator;

public class Integral extends Measurement {

  public static Comparator<Measurement> c = new IntegralComparator();

	Integral(JDXSpectrum spec, double value, double x1, double x2, double y1, double y2) {
  	super(spec, x1, y1, "", x2, y2);
    setValue(value);
  }
  
	
}