package jspecview.common;

import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Map;

import jspecview.common.Annotation.AType;
import jspecview.util.TextFormat;

/**
 * 
 * a data structure for peak lists
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 *
 */
public class PeakData extends MeasurementData {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PeakData(AType type, JDXSpectrum spec) {
		super(type, spec);
	}

	private double thresh;

	private double minY;

	private double maxY;
	public double getThresh() {
		return thresh;
	}
	
	private final static String[] HNMR_HEADER = new String[] { "peak", "shift/ppm", "intens" , "shift/hz", "diff/hz", "2-diff", "3-diff" };

	@Override
	public String[] getDataHeader() {
		return (spec.isHNMR() ? HNMR_HEADER : new String[] { "peak", spec.getXUnits(), spec.getYUnits() }); 		
	}

	@Override
	public String[][] getMeasurementListArray(String units) {
		String[][] data = new String[size()][];
		double[] last = new double[] {-1e100, 1e100, 1e100};
		for (int pt = 0, i = size(); --i >= 0;) {
			data[pt++] = spec.getPeakListArray(pt, get(i), last, maxY);
		}
		return data;
	}
	
	@Override
	public Map<String, Object> getParams() {
		Map<String, Object> info = new Hashtable<String, Object>();
		info.put("interpolation", myParams.peakListInterpolation);
		info.put("threshold", Double.valueOf(myParams.peakListThreshold));
		return info;
	}

	public void setPeakList(Parameters p, DecimalFormat formatter, ScaleData view) {
		if (formatter == null)
			formatter = TextFormat.getDecimalFormat(spec.getPeakPickHash());
		df = formatter;
		Coordinate[] xyCoords = spec.getXYCoords();
		if (xyCoords.length < 3)
			return;
		clear();
		if (p != null) {
  		myParams.peakListInterpolation = p.peakListInterpolation;
	  	myParams.peakListThreshold = p.peakListThreshold;
		}
		boolean doInterpolate = (myParams.peakListInterpolation.equals("parabolic"));
		boolean isInverted = spec.isInverted();
		minY = view.minYOnScale;
		maxY = view.maxYOnScale;
		double minX = view.minXOnScale;
		double maxX = view.maxXOnScale;
		thresh = myParams.peakListThreshold;
		if (Double.isNaN(thresh))
			thresh = myParams.peakListThreshold = (minY + maxY) / 2;
		double yLast = 0;
		double[] y3 = new double[] { xyCoords[0].getYVal(),
				yLast = xyCoords[1].getYVal(), 0 };
		int n = 0;
		int n2 = 0;
		if (isInverted)
			for (int i = 2; i < xyCoords.length; i++) {
				double y = y3[i % 3] = xyCoords[i].getYVal();
				if (yLast < thresh && y3[(i - 2) % 3] > yLast && yLast < y) {
					double x = (doInterpolate ? Coordinate.parabolicInterpolation(
							xyCoords, i - 1) : xyCoords[i - 1].getXVal());
					if (x >= minX || x <= maxX) {
						PeakPick m = new PeakPick(spec, x, y);
						add(m);
						if (++n == 100)
							break;
					}
				}
				yLast = y;
			}
		else
			for (int i = 2; i < xyCoords.length; i++) {
				double y = y3[i % 3] = xyCoords[i].getYVal();
				if (yLast > thresh && y3[(i - 2) % 3] < yLast && yLast > y) {
					n2++;
					double x = (doInterpolate ? Coordinate.parabolicInterpolation(
							xyCoords, i - 1) : xyCoords[i - 1].getXVal());
					if (x >= minX && x <= maxX) {
						PeakPick m = new PeakPick(spec, x, y, formatter.format(x), x);
						add(m);
						if (++n == 100)
							break;
					}
				}
				yLast = y;
			}
	}
	


}
