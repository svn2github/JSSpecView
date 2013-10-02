package jspecview.common;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jspecview.common.Annotation.AType;
import jspecview.util.JSVTextFormat;

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
	  OFF, ON, TOGGLE, AUTO, LIST, MARK, MIN, UPDATE;
	  static IntMode getMode(String value) {
	    for (IntMode mode: values())
	      if (value.startsWith(mode.name()))
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
  private double percentRange;
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
		percentRange = integralRange;
    calculateIntegral();
	}

	public IntegralData(JDXSpectrum spec, Parameters p) {
		super(AType.Integration, spec);
		if (p == null) {
			autoIntegrate();
			return;
		}
		percentOffset = p.integralOffset;
		percentRange = p.integralRange;
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
		double percentRange0 = percentRange;
		if (integralRange <= 0 || integralRange == percentRange && integralOffset == percentOffset)
			return;
		percentOffset = integralOffset;
		percentRange = integralRange;
		checkRange();
		//for (int j = 0; j < size(); j++)
	  	//System.out.println(j + " " + get(j));
		double intRangeNew = integralRange / 100 / integralTotal;
		double offsetNew = integralOffset / 100;
		for (int i = 0; i < xyCoords.length; i++) {
			double y = xyCoords[i].getYVal();
			y = (y - offset) / intRange;
      xyCoords[i].setYVal(y * intRangeNew + offsetNew);			
		}

		if (normalizationFactor != 1)
      normalizationFactor *= percentRange0 / integralRange;
    if (haveRegions) {
  		for (int i = size(); --i >= 0;) {
  			Measurement ir = get(i);
	  		double y1 = getYValueAt(ir.getXVal());
	  		double y2 = getYValueAt(ir.getXVal2());
	  		ir.setYVal(y1);
	  		ir.setYVal2(y2);
	  		ir.setValue(Math.abs(y2 - y1) * 100 * normalizationFactor);
		  }
		}
    intRange = intRangeNew; 
    offset = offsetNew;		
	}

	boolean haveRegions;

	private Coordinate[] xyCoords;
	
  double getYValueAt(double x) {
    return Coordinate.getYValueAt(xyCoords, x);
  }

	public Integral addIntegralRegion(double x1, double x2) {
		if (Double.isNaN(x1)) {
			haveRegions = false;
			clear();
			return null;
		}
		if(x1 == x2)
			return null;
		double y1 = getYValueAt(x1);
		double y2 = getYValueAt(x2);
		haveRegions = true;
		Integral in = new Integral(spec, Math.abs(y2 - y1) * 100
				* normalizationFactor, x1, x2, y1, y2);
		clear(x1, x2);
		//if (in.getValue() < 0.1) -- no, need this for tiny integrals with a water peak, for instance
			//return null;
  	add(in);
		Collections.sort(this, c);
		return in;
	}

  private static Comparator<Measurement> c = new IntegralComparator();

	@Override
	public void addSpecShift(double dx) {
		Coordinate.shiftX(xyCoords, dx);
    for (int i = size(); --i >= 1;) {
      get(i).addSpecShift(dx);
    }		
	}

	/**
	 * INTEGRATION MARK list
	 * where list is a comma-separated list of ppm1-ppm2
	 * with  :x.x added to normalize one of them
	 * and starting with  0-0 clears the integration 
	 * @param ppms
	 */
	public void addMarks(String ppms) {
    //2-3,4-5,6-7...
    ppms = JSVTextFormat.simpleReplace(" " + ppms, ",", " ");
    ppms = JSVTextFormat.simpleReplace(ppms, " -"," #");
    ppms = JSVTextFormat.simpleReplace(ppms, "--","-#");
    ppms = ppms.replace('-','^');
    ppms = ppms.replace('#','-');
    List<String> tokens = ScriptToken.getTokens(ppms);
    for (int i = 0; i < tokens.size(); i++) {
      try {
        String s = tokens.get(i);
        double norm = 0;
        int pt = s.indexOf('^');
        if (pt < 0)
          continue;
      	int pt2 = s.indexOf(':');
      	if (pt2 > pt) {
      		norm = Double.valueOf(s.substring(pt2 + 1).trim()).doubleValue();
      		s = s.substring(0, pt2).trim();      		
      	}
        double x2 = Double.valueOf(s.substring(0, pt).trim()).doubleValue();
        double x1 = Double.valueOf(s.substring(pt + 1).trim()).doubleValue();
        if (x1 == 0 && x2 == 0) 
        	clear();
        if (x1 == x2)
        	continue;
        Measurement m = addIntegralRegion(Math.max(x1, x2), Math.min(x1, x2));
        if (m != null && norm > 0)
        	setSelectedIntegral(m, norm);
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
	    checkRange();
	    double minY = 1E100;
	    for (int i = 0; i < specXyCoords.length; i++) {
	      double y = specXyCoords[i].getYVal();
	      if (y < minY && y >= 0)
	        minY = y;
	    }

	    for (int i = 0; i < specXyCoords.length; i++) {
	      double y = specXyCoords[i].getYVal();
	      if (y > minYForIntegral)
	        integralTotal += (y - minY);
	    }
	    intRange = (percentRange / 100) / integralTotal; 
	    offset = (percentOffset / 100);

	    // Calculate Integral Graph as a scale from 0 to 1

	    double integral = 0;
	    for (int i = specXyCoords.length; --i >= 0;) {
	      double y = specXyCoords[i].getYVal();
	      if (y > minYForIntegral)
	        integral += (y - minY);
	      xyCoords[i] = new Coordinate(specXyCoords[i].getXVal(), integral
	          * intRange + offset);
	    }
	    return xyCoords;
	}

	private void checkRange() {
    percentOffset = Math.max(5, percentOffset);
    percentRange = Math.max(10, percentRange);
	}

	/**
	 * Parses integration ratios and x values from a string and returns them as
	 * <code>IntegrationRatio</code> objects
	 * @param spec 
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

  /**
   * @param x 
   * @return FRACTIONAL value * 100
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
		double factor = (val <= 0 ? 1/normalizationFactor : val / val0);
		factorAllIntegrals(factor, val <= 0);
	}

	private void factorAllIntegrals(double factor, boolean isReset) {
		for (int i = 0; i < size(); i++) {
			Measurement m = get(i);
			m.setValue(factor * m.getValue());
		}
		normalizationFactor = (isReset ? 1 : normalizationFactor * factor);
	}

	@Override
	public void clear() {
		super.clear();
	}
	
	@Override
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

	@Override
	public String[][] getMeasurementListArray(String units) {
		String[][] data = new String[size()][];
		for (int pt = 0, i = size(); --i >= 0;)
			data[pt++] = new String[] { "" + pt, JSVTextFormat.formatDecimal(get(i).getXVal(), 2), 
				JSVTextFormat.formatDecimal(get(i).getXVal2(), 2), get(i).getText() };
		return data;
	}

	@Override
	public double[][] getMeasurementListArrayReal(String units) {
		double[][] data = new double[size()][];
		for (int pt = 0, i = size(); --i >= 0; pt++)
			data[pt] = new double[] { get(i).getXVal(), get(i).getXVal2(), get(i).getValue() };
		return data;
	}

	private final static String[] HEADER = new String[] { "peak", "start/ppm", "end/ppm", "value" };

	@Override
	public String[] getDataHeader() {
		return HEADER;
	}

	public void shiftY(int yOld, int yNew, int yPixel0, int yPixels) {
		// yOld sign -1 indicates RANGE change
		double pt = (int) (100.0 * (yPixel0 + yPixels - yNew) / yPixels);
		if (yOld < 0)
			pt -=  percentOffset;
		if (yOld < 0) { // end point
			update(0, percentOffset, pt);
		} else {
			update(0, pt, percentRange);
		}
	}

	public void autoIntegrate() {
		if (xyCoords == null)
			calculateIntegral();
		if (xyCoords.length == 0)
			return;
		clear();
		int iStart = -1;
		double cutoff = 0.0001;
		int nCount = 0;
		int nMin = 20;
		double y0 = xyCoords[xyCoords.length - 1].getYVal();
		for (int i = xyCoords.length - 1; --i >= 0;) {
			double y = xyCoords[i].getYVal();
			nCount++;
			if ((y - y0) < cutoff && iStart < 0) {
				// not in peak and not increasing much
				if (y < y0) {
					// decreasing -- reset
					//System.out.println(" reset " + nCount + " " + iStart + " y0=" + y0 + " " + y);
					y0 = y;
					nCount = 0;
				}
				continue;
			}
			if (iStart < 0) {
				// but y - y0 >= cutoff
				iStart = i + Math.min(nCount, nMin);
				//System.out.println(" setting " + iStart + " y0=" + y0 + " " + y);
				y0 = y;
				nCount = 0;
				continue;
			}
			// in peak;
			if ((y - y0) < cutoff) {
				// and leveled off
				if (nCount == 1)
					y0 = y;
				if (nCount >= nMin) {
					//System.out.println(iStart + " " + i + " " + xyCoords[iStart] +  " "  + xyCoords[i]);
					addIntegralRegion(xyCoords[iStart].getXVal(), xyCoords[i].getXVal());
					iStart = -1;
					y0 = y;
					nCount = 0;
				}
			} else {
				// still rising
				nCount = 0;
				y0 = y;
				//System.out.println(i + " reset istart=" + iStart + " " + xyCoords[i] + "  " + y0);
			}
		}
		if (spec.nH > 0)
			factorAllIntegrals(spec.nH / percentRange, false);
	}

	@Override
	public void getInfo(Map<String, Object> info) {
		info.put("offset", Double.valueOf(myParams.integralOffset));
		info.put("range", Double.valueOf(myParams.integralRange));
		info.put("normalizationFactor", Double.valueOf(normalizationFactor));
		info.put("integralTotal", Double.valueOf(integralTotal));
		super.getInfo(info);
	}

	void setMinimumIntegral(double val) {
		for (int i = size(); --i >= 0;)
			if (get(i).getValue() < val)
				remove(i);
	}

}
