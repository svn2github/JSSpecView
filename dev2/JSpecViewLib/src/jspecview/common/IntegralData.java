package jspecview.common;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import jspecview.common.Annotation.AType;
import jspecview.util.TextFormat;

/**
 * 
 * from IntegralGraph
 * a data structure for integration settings
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 *
 */
public class IntegralData extends MeasurementData {

	public enum IntMode {
	  OFF, ON, TOGGLE, MARK, UPDATE;
	  static IntMode getMode(String value) {
	    for (IntMode mode: values())
	      if (mode.name().equalsIgnoreCase(value))
	        return mode;
	    return ON;
	  }
	}

	private static final long serialVersionUID = 1L;
	
	public final static double DEFAULT_OFFSET = 30;
	public final static double DEFAULT_RANGE = 50;
	public final static double DEFAULT_MINY = 0.1;

	private double percentMinY; // Android only?  not use in JSpecView	
  public double getPercentMinimumY() {
  	return percentMinY;
  }
  
	private double percentOffset;
  public double getPercentOffset() {
  	return percentOffset;
  }  

	private double intRange;
  public double getIntegralFactor() {
  	return intRange;
  }
  
  private double normalizationFactor = 1;
  private double range;
	private double offset;
	private double integralTotal;
	
	/**
	 * 
	 * @param integralMinY  not used
	 * @param integralOffset
	 * @param integralRange
	 * @param spec 
	 */
	public IntegralData(double integralMinY, double integralOffset, double integralRange, JDXSpectrum spec) {
		super(AType.Integration, spec);
    percentMinY = integralMinY; // not used.
		percentOffset = integralOffset;
		range = integralRange;
    calculateIntegral();
	}

	public IntegralData(JDXSpectrum spec, Parameters parameters) {
		super(AType.Integration, spec);
		percentOffset = parameters.integralOffset;
		range = parameters.integralRange;
    calculateIntegral();
	}

	public void update(Parameters parameters) {
		update(parameters.integralMinY, parameters.integralOffset, parameters.integralRange);
	}

	/**
	 * minY is ignored
	 * 
	 * @param integralMinY
	 * @param integralOffset
	 * @param integralRange
	 */
	public void update(double integralMinY, double integralOffset,
			double integralRange) {
		if (integralRange <= 0 || integralRange == range && integralOffset == percentOffset)
			return;
		for (int j = 0; j < size(); j++)
	  	System.out.println(j + " " + get(j));
		double intRangeNew = integralRange / 100 / integralTotal;
		double offsetNew = integralOffset / 100;
		for (int i = 0; i < xyCoords.length; i++) {
			double y = xyCoords[i].getYVal();
			y = (y - offset) / intRange;
      xyCoords[i].setYVal(y * intRangeNew + offsetNew);			
		}

		if (normalizationFactor != 1)
      normalizationFactor *= range / integralRange;
    if (haveRegions) {
  		for (int i = size(); --i >= 1;) { // 0 is pending only
  			Measurement ir = get(i);
	  		double y1 = getYValueAt(ir.getXVal());
	  		double y2 = getYValueAt(ir.getXVal2());
	  		ir.setYVal(y1);
	  		ir.setYVal2(y2);
	  		ir.setValue(Math.abs(y2 - y1) * 100 * normalizationFactor);
		  }
		}
		
		percentOffset = integralOffset;
		range = integralRange;
    intRange = intRangeNew; 
    offset = offsetNew;
	}

	public void scaleIntegrationBy(double f) {
		normalizationFactor *= f;
	}

	boolean haveRegions;

	private Coordinate[] xyCoords;
	
  double getYValueAt(double x) {
    return Coordinate.getYValueAt(xyCoords, x);
  }

	public void addIntegralRegion(double x1, double x2,
			boolean isFinal) {
		if (Double.isNaN(x1)) {
			haveRegions = false;
			clear();
			return;
		}
		double y1 = getYValueAt(x1);
		double y2 = getYValueAt(x2);
		haveRegions = true;
		Integral in = new Integral(spec, Math.abs(y2 - y1) * 100
				* normalizationFactor, x1, x2, y1, y2);
		clear(x1, x2);
		if (in.getValue() < 0.1)
			return;
  	add(in);
		Collections.sort(this, Integral.c);
	}

	public void addSpecShift(double dx) {
		Coordinate.shiftX(xyCoords, dx);
    for (int i = size(); --i >= 1;) {
      get(i).addSpecShift(dx);
    }		
	}

	public void addMarks(String ppms) {
    //2-3,4-5,6-7...
    ppms = TextFormat.simpleReplace(" " + ppms, ",", " ");
    ppms = TextFormat.simpleReplace(ppms, " -"," #");
    ppms = TextFormat.simpleReplace(ppms, "--","-#");
    ppms = ppms.replace('-','^');
    ppms = ppms.replace('#','-');
    List<String> tokens = ScriptToken.getTokens(ppms);
    addIntegralRegion(0, 0, false);
    for (int i = tokens.size(); --i >= 0;) {
      String s = tokens.get(i);
      int pt = s.indexOf('^');
      if (pt < 0)
        continue;
      try {
        double x2 = Double.valueOf(s.substring(0, pt).trim());
        double x1 = Double.valueOf(s.substring(pt + 1).trim());
        addIntegralRegion(x1, x2, true);
      } catch (Exception e) {
        continue;
      }
    }
	}

	public Coordinate[] calculateIntegral() {
	    Coordinate[] specXyCoords = spec.getXYCoords();
	    xyCoords = new Coordinate[specXyCoords.length];

	    //double maxY = Coordinate.getMaxY(xyCoords, 0, xyCoords.length);
	    
	    // this was setting a minimum point, not allowing the integral to 
	    // register negative values
	    double minYForIntegral = -Double.MAX_VALUE;//percentMinY / 100 * maxY; // 0.1%
	    integralTotal = 0;
	    for (int i = 0; i < specXyCoords.length; i++) {
	      double y = specXyCoords[i].getYVal();
	      if (y > minYForIntegral)
	        integralTotal += y;
	    }
	    if (range == 0)
	    	range = 10;
	    intRange = (range / 100) / integralTotal; 
	    offset = (percentOffset / 100);

	    // Calculate Integral Graph as a scale from 0 to 1

	    double integral = 0;
	    for (int i = specXyCoords.length; --i >= 0;) {
	      double y = specXyCoords[i].getYVal();
	      if (y > minYForIntegral)
	        integral += y;
	      xyCoords[i] = new Coordinate(specXyCoords[i].getXVal(), integral
	          * intRange + offset);
	    }
	    return xyCoords;
	}

	/**
	 * Parses integration ratios and x values from a string and returns them as
	 * <code>IntegrationRatio</code> objects
	 * 
	 * @param value
	 * @return ArrayList<IntegrationRatio> object representing integration ratios
	 */
	public static ArrayList<Annotation> getIntegrationRatiosFromString(
			JDXSpectrum spec, String value) {
		ArrayList<Annotation> ratios = new ArrayList<Annotation>();
		// split input into x-value/integral-value pairs
		StringTokenizer allParamTokens = new StringTokenizer(value, ",");
		while (allParamTokens.hasMoreTokens()) {
			String token = allParamTokens.nextToken();
			// now split the x-value/integral-value pair
			StringTokenizer eachParam = new StringTokenizer(token, ":");
			Annotation ratio = new Annotation(spec, Double.parseDouble(eachParam
					.nextToken()), 0.0, eachParam.nextToken(), true, false, 0, 0);
			ratios.add(ratio);
		}
		return ratios;
	}

	public Coordinate[] getXYCoords() {
		return xyCoords;
	}

	//
//public Map<String, Object> getInfo(String key) {
//  Map<String, Object> info = new Hashtable<String, Object>();
//  JDXSpectrum.putInfo(key, info, "type", "integration");
//  JDXSpectrum.putInfo(key, info, "percentMinY", Double.valueOf(percentMinY));
//  JDXSpectrum.putInfo(key, info, "percentOffset", Double.valueOf(percentOffset));
//  JDXSpectrum.putInfo(key, info, "integralRange", Double.valueOf(integralRange));
//  //TODO annotations
//  return info;
//}

  /**
   * returns FRACTIONAL value * 100
   */
  public double getPercentYValueAt(double x) {
    return getYValueAt(x) * 100;
  }

	public void dispose() {
		spec = null;
		xyCoords = null;
	}

	public void setSelectedIntegral(Measurement integral, double val) {
		double val0 = integral.getValue();
		double factor = val / val0;
		for (int i = 0; i < size(); i++) {
			Measurement m = get(i);
  		m.setValue(factor * m.getValue());
		}
		scaleIntegrationBy(factor);
	}

	public void clear() {
		super.clear();
	}
	
	public Measurement remove(int i) {
		return super.remove(i);
	}

	public BitSet getBitSet() {
		BitSet bs = new BitSet(xyCoords.length);
		if (size() == 0) {
  		bs.set(0, xyCoords.length);
  		return bs;
		}
		for (int i = size(); --i >= 0;) {
		  Measurement m = get(i);
		  int x1 = Coordinate.getNearestIndexForX(xyCoords, m.getXVal());
		  int x2 = Coordinate.getNearestIndexForX(xyCoords, m.getXVal2());
		  bs.set(Math.min(x1, x2), Math.max(x1, x2));
		}
		return bs;
	}

	public String[][] getIntegralListArray() {
		DecimalFormat df2 = TextFormat.getDecimalFormat("#0.00");
		String[][] data = new String[size()][];
		for (int pt = 0, i = size(); --i >= 0;)
			data[pt++] = new String[] { "" + pt, df2.format(get(i).getXVal()), df2.format(get(i).getXVal2()), get(i).getText() };
		return data;
	}

	private final static String[] HEADER = new String[] { "peak", "start/ppm", "end/ppm", "value" };

	public String[] getDataHeader() {
		return HEADER;
	}

}
