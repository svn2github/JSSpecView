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

import org.jmol.util.JmolList;

import java.util.Map;

import org.jmol.util.Txt;

import jspecview.api.AnnotationData;
import jspecview.common.Annotation.AType;

public class MeasurementData extends JmolList<Measurement> implements
		AnnotationData {

	private static final long serialVersionUID = 1L;

	private AType type;
	protected JDXSpectrum spec;
	protected String units;
	protected int precision;
	protected Parameters myParams;

	private boolean isON = true;
	private String key;

	private final static String[] HEADER = new String[] { "peak", "start", "end",
			"value" };

	MeasurementData(AType type, JDXSpectrum spec) {
		this.type = type;
		this.spec = spec;
		myParams = new Parameters().setName("MeasurementData");
	}

	JmolList<Measurement> getMeasurements() {
		return this;
	}

	public AType getAType() {
		return type;
	}

	public boolean getState() {
		return isON;
	}

	public void setState(boolean b) {
		isON = b;
	}

	void setMeasurements(
			@SuppressWarnings("unused") JmolList<Measurement> measurements) {
		// won't happen
	}

	public Parameters getParameters() {
		return myParams;
	}

	public String[] getDataHeader() {
		return HEADER;
	}

	public String[][] getMeasurementListArray(String units) {
		this.units = units;
		double[][] ddata = getMeasurementListArrayReal(units);
		int precisionX = (spec.isNMR() ? 4 : 2);
		int precisionDX = (spec.isHNMR() && units.equals("ppm") ? 4 : 2);
		String[][] data = new String[size()][];
		for (int i = size(); --i >= 0;)
			data[i] = new String[] { "" + (i + 1),
					Txt.formatDecimalDbl(ddata[i][0], precisionX),
					Txt.formatDecimalDbl(ddata[i][1], precisionX),
					Txt.formatDecimalDbl(ddata[i][2], precisionDX) };
		return data;
	}

	double[][] getMeasurementListArrayReal(String units) {
		boolean toHz = spec.isNMR() && units.equalsIgnoreCase("HZ");
		double[][] data = new double[size()][];
		for (int pt = 0, i = size(); --i >= 0;) {
			double y = get(i).getValue();
			if (toHz)
				y *= spec.observedFreq;
			data[pt++] = new double[] { get(i).getXVal(), get(i).getXVal2(), y };
		}
		return data;
	}

	static boolean checkParameters(MeasurementData md, ColorParameters p) {
		if (md.size() == 0)
			return false;
		Parameters myParams = md.getParameters();
		switch (md.getAType()) {
		case Integration:
			break;
		case PeakList:
			return (p.peakListInterpolation.equals(myParams.peakListInterpolation) && p.peakListThreshold == myParams.peakListThreshold);
		case Measurements:
			break;
		case NONE:
		}
		return false;
	}

	public JDXSpectrum getSpectrum() {
		return spec;
	}

	public MeasurementData getData() {
		return this;
	}

	protected void clear(double x1, double x2) {
		// no overlapping regions. Ignore first, which is the temporary one
		for (int i = size(); --i >= 0;) {
			Measurement in = get(i);
			if (in.text.length() == 0 || in.overlaps(x1, x2)) {
				remove(i);
			}
		}

	}

	public void setSpecShift(double dx) {
		for (int i = size(); --i >= 0;) {
			Measurement m = get(i);
			double x = m.getXVal() + dx;
			m.setXVal(x);
			m.setValue(x);
			m.text = Txt.formatDecimalDbl(x, precision);
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public boolean isVisible() {
		return true;
	}

	void getInfo(Map<String, Object> info) {
		info.put("header", getDataHeader());
		info.put("table", getMeasurementListArrayReal("ppm"));
		if (units != null)
			info.put("units", units);
	}

}
