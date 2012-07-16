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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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
	public void setMeasurements(@SuppressWarnings("unused") List<Measurement> measurements) {
		// won't happen
	}

	protected Parameters myParams = new Parameters("MeasurementData");
	public Parameters getParameters() {
		return myParams;
	}

	protected DecimalFormat df;

	private final static String[] HEADER = new String[] { "peak", "start", "end", "value" };
	public String[] getDataHeader() {
		return HEADER;
	}

	protected String units;
	
	public String[][] getMeasurementListArray(String units) {
		this.units = units;
		DecimalFormat dfx = TextFormat.getDecimalFormat(spec.isNMR() ? "#0.0000"
				: "#0.00");
		boolean toHz = spec.isNMR() && units.equalsIgnoreCase("HZ");
		DecimalFormat dfdx = TextFormat
				.getDecimalFormat(spec.isHNMR() && units.equals("ppm") ? "#0.0000" : "#0.00");
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

	public static boolean checkParameters(MeasurementData md, Parameters p) {
		if (md.size() == 0)
			return false;
		Parameters myParams = md.getParameters();
		switch (md.getAType()) {
		case Integration:
			break;
		case PeakList:
			return (
					p.peakListInterpolation.equals(myParams.peakListInterpolation)
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

  protected void clear(double x1, double x2) {
    // no overlapping regions. Ignore first, which is the temporary one
    for (int i = size(); --i >= 0;) {
      Measurement in = get(i);
      if (in.text.length() == 0 || in.overlaps(x1, x2)) {      	
        remove(i);
      }
    }
    
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

	public boolean isVisible() {
		return true;
	}

	public Map<String, Object> getParams() {
		Map<String, Object> info = new Hashtable<String, Object>();
		if (units != null)
			info.put("units", units);
		return info;
	}

	
}
