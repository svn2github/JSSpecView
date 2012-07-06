package jspecview.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import jspecview.util.TextFormat;

public class IntegralData {

  List<Measurement> integralRegions;
  double normalizationFactor = 1;
  double percentMinY;
	double percentOffset;
	double factor;
	public final static double DEFAULT_MINY = 0.1;
	public final static double DEFAULT_FACTOR = 50;
	public final static double DEFAULT_OFFSET = 30;

	IntegralData(double percentMinY, double percentOffset, double factor) {
		this.percentMinY = percentMinY;
		this.percentOffset = percentOffset;
		this.factor = factor;
	}

	public void scaleIntegrationBy(double f) {
		normalizationFactor *= f;
	}

	public void addIntegralRegion(IntegralGraph ig, double x1, double x2,
			boolean isFinal) {
    if (Double.isNaN(x1)) {
      integralRegions = null;
      return;
    }
    double intVal = Math.abs(ig.getPercentYValueAt(x2) - ig.getPercentYValueAt(x1));
    if (isFinal) {
      integralRegions.get(0).setValue(0);
      if (intVal == 0)
        return;
    }
    if (integralRegions == null)
      integralRegions = new ArrayList<Measurement>();
    Integral in = new Integral(ig.spectrum, intVal * normalizationFactor, x1, x2, 
    		ig.getYValueAt(x1), ig.getYValueAt(x2));
    clearIntegralRegions(x1, x2);
    if (isFinal || integralRegions.size() == 0) {
      integralRegions.add(in);
    } else {
      integralRegions.set(0, in);
    }
		Collections.sort(integralRegions, Integral.c);
	}

  private void clearIntegralRegions(double x1, double x2) {
    // no overlapping integrals. Ignore first, which is the temporary one
    
    for (int i = integralRegions.size(); --i >= 1;) {
      Measurement in = integralRegions.get(i);
      if (Math.min(in.getXVal(), in.getXVal2()) < Math.max(x1, x2) 
          && Math.max(in.getXVal(), in.getXVal2()) > Math.min(x1, x2))
        integralRegions.remove(i);
    }
    
  }

	public void addMarks(IntegralGraph ig, String ppms) {
    //2-3,4-5,6-7...
    ppms = TextFormat.simpleReplace(" " + ppms, ",", " ");
    ppms = TextFormat.simpleReplace(ppms, " -"," #");
    ppms = TextFormat.simpleReplace(ppms, "--","-#");
    ppms = ppms.replace('-','^');
    ppms = ppms.replace('#','-');
    List<String> tokens = ScriptToken.getTokens(ppms);
    addIntegralRegion(ig, 0, 0, false);
    for (int i = tokens.size(); --i >= 0;) {
      String s = tokens.get(i);
      int pt = s.indexOf('^');
      if (pt < 0)
        continue;
      try {
        double x2 = Double.valueOf(s.substring(0, pt).trim());
        double x1 = Double.valueOf(s.substring(pt + 1).trim());
        addIntegralRegion(ig, x1, x2, true);
      } catch (Exception e) {
        continue;
      }
    }
	}

	public Coordinate[] calculateIntegral(IntegralGraph ig) {
	    Coordinate[] xyCoords = ig.spectrum.getXYCoords();
	    Coordinate[] integralCoords = new Coordinate[xyCoords.length];

	    //double maxY = Coordinate.getMaxY(xyCoords, 0, xyCoords.length);
	    
	    // this was setting a minimum point, not allowing the integral to 
	    // register negative values
	    double minYForIntegral = -Double.MAX_VALUE;//percentMinY / 100 * maxY; // 0.1%
	    double integral = 0;
	    for (int i = 0; i < xyCoords.length; i++) {
	      double y = xyCoords[i].getYVal();
	      if (y > minYForIntegral)
	        integral += y;
	    }

	    double intFactor = (factor / 100) / integral; 
	    double offset = (percentOffset / 100);

	    // Calculate Integral Graph as a scale from 0 to 1

	    integral = 0;
	    for (int i = xyCoords.length, j = 0; --i >= 0; j++) {
	      double y = xyCoords[i].getYVal();
	      if (y > minYForIntegral)
	        integral += y;
	      integralCoords[i] = new Coordinate(xyCoords[i].getXVal(), integral
	          * intFactor + offset);
	    }
	    return integralCoords;
	}

	/**
	 * Parses integration ratios and x values from a string and returns them as
	 * <code>IntegrationRatio</code> objects
	 * 
	 * @param value
	 * @return ArrayList<IntegrationRatio> object representing integration ratios
	 */
	public static ArrayList<Annotation> getIntegrationRatiosFromString(
			String value) {
		ArrayList<Annotation> ratios = new ArrayList<Annotation>();
		// split input into x-value/integral-value pairs
		StringTokenizer allParamTokens = new StringTokenizer(value, ",");
		while (allParamTokens.hasMoreTokens()) {
			String token = allParamTokens.nextToken();
			// now split the x-value/integral-value pair
			StringTokenizer eachParam = new StringTokenizer(token, ":");
			Annotation ratio = new Annotation(Double.parseDouble(eachParam
					.nextToken()), 0.0, eachParam.nextToken(), true, false, 0, 0);
			ratios.add(ratio);
		}
		return ratios;
	}

	//
//public Map<String, Object> getInfo(String key) {
//  Map<String, Object> info = new Hashtable<String, Object>();
//  JDXSpectrum.putInfo(key, info, "type", "integration");
//  JDXSpectrum.putInfo(key, info, "percentMinY", Double.valueOf(percentMinY));
//  JDXSpectrum.putInfo(key, info, "percentOffset", Double.valueOf(percentOffset));
//  JDXSpectrum.putInfo(key, info, "integralFactor", Double.valueOf(integralFactor));
//  //TODO annotations
//  return info;
//}


}
