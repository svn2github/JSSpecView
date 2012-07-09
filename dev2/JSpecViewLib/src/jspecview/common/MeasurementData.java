/* Copyright (c) 2002-2008 The University of the West Indies
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

// CHANGES to 'IntegrationRatio.java' - Integration Ratio Representation
// University of the West Indies, Mona Campus
// 24-09-2011 jak - Created class as an extension of the Coordinate class
//					to handle the integration ratio value.

package jspecview.common;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import jspecview.common.Annotation.AType;
import jspecview.util.TextFormat;

public class MeasurementData extends ArrayList<Measurement> implements AnnotationData {

	private static final long serialVersionUID = 1L;

	private AType type;
	public AType getAType() {
		return type;
	}
	
	protected JDXSpectrum spec;

	private boolean isON = true;

	public boolean getState() {
		return isON;
	}
	public void setState(boolean b) {
		isON = b;
	}
	
	public MeasurementData(AType type, JDXSpectrum spec) {
		this.type = type;
		this.spec = spec;
	}
	public List<Measurement> getMeasurements() {
		return this;
	}
	public void setMeasurements(List<Measurement> measurements) {
		// won't happen
	}

	private Parameters myParams = new Parameters("MeasurementData");
	public Parameters getParameters() {
		return myParams;
	}

	private DecimalFormat df;

	private double thresh;

	private double minY;

	private double maxY;
	public double getThresh() {
		return thresh;
	}
	
	public String[][] getPeakListArray() {
		String[][] data = new String[size()][];
		double[] last = new double[] {-1e100, 1e100};
		for (int pt = 0, i = size(); --i >= 0;) {
			data[pt++] = spec.getPeakListArray(pt, get(i), last, minY, maxY);
		}
		return data;
	}
	
	public String[][] getIntegralListArray() {
		DecimalFormat df2 = TextFormat.getDecimalFormat("#0.00");
		String[][] data = new String[size() - 1][];
		for (int pt = 0, i = size(); --i >= 1;) // [0] is the pending measurement
			data[pt++] = new String[] { "" + pt, df2.format(get(i).getXVal()), df2.format(get(i).getXVal2()), get(i).getText() };
		return data;
	}

	public String[][] getMeasurementListArray(String units) {
		DecimalFormat dfx = TextFormat.getDecimalFormat(spec.isNMR() ? "#0.0000"
				: "#0.00");
		boolean toHz = units.equalsIgnoreCase("HZ");
		DecimalFormat dfdx = TextFormat
				.getDecimalFormat(units.equals("ppm") ? "#0.0000" : "#0.00");
		String[][] data = new String[size()][];
		for (int pt = 0, i = size(); --i >= 0;) {
			double y = get(i).getValue();
			if (toHz)
				y *= spec.observedFreq;
			data[pt++] = new String[] { "" + pt, dfx.format(get(i).getXVal()),
					dfx.format(get(i).getXVal2()), dfdx.format(y) };
		}
		return data;
	}

	public void setPeakList(Parameters p, DecimalFormat formatter, ScaleData view) {
		if (formatter == null)
			formatter = TextFormat.getDecimalFormat(spec.getPeakPickHash());
		df = formatter;
		Coordinate[] xyCoords = spec.getXYCoords();
		if (xyCoords.length < 3)
			return;
		clear();
		myParams.peakListInclude = p.peakListInclude;
		myParams.peakListInterpolation = p.peakListInterpolation;
		myParams.peakListSkip = p.peakListSkip;
		myParams.peakListThreshold = p.peakListThreshold;
		boolean doInterpolate = (myParams.peakListInterpolation.equals("parabolic"));
		boolean isInverted = (spec.getYRef() != 0); // IR
		minY = view.minYOnScale;
		maxY = view.maxYOnScale;
		double minX = view.minXOnScale;
		double maxX = view.maxXOnScale;
		thresh = myParams.peakListThreshold;
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
	
	public static boolean checkParameters(MeasurementData md, Parameters p) {
		if (md.size() == 0)
			return false;
		Parameters myParams = md.getParameters();
		switch (md.getAType()) {
		case Integration:
			break;
		case PeakList:
			return (
					p.peakListInclude == myParams.peakListInclude
					&& p.peakListInterpolation.equals(myParams.peakListInterpolation)
					&& p.peakListSkip == myParams.peakListSkip
					&& p.peakListThreshold == myParams.peakListThreshold);
		case Measurements:
			break;
		}
		return false;
	}

	public JDXSpectrum getSpectrum() {
		return spec;
	}
	
	public MeasurementData getData() {
		return this;
	}

	public void addSpecShift(double dx) {
		for (int i = size(); --i >= 0;) {
			Measurement m = get(i);
			double x = m.getXVal() + dx;
			m.setXVal(x);
			m.setValue(x);
			m.text = df.format(x);
		}
	}

  private String key;
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;		
	}

}
