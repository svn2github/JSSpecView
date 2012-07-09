/* Copyright (c) 2002-2012 The University of the West Indies
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

package jspecview.common;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import jspecview.common.Annotation.AType;
import jspecview.util.TextFormat;

/**
 * Dialog for managing the peak listing 
 * for a Spectrum within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

class AwtPeakListDialog extends AwtAnnotationDialog implements FocusListener {

	private static final long serialVersionUID = 1L;
	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};
	private JTextField txtThreshold;
//	private JTextField txtInclude;
//	private JTextField txtSkip;
	private JComboBox cbInterpolation;

	protected AwtPeakListDialog(String title, ScriptInterface si, JDXSpectrum spec, 
			JSVPanel jsvp, Map<String, Object> data) {
		super(title, si, spec, jsvp, data);
		thisType = AType.PeakList;
		setTitle("Peak Listing");
		setup();
	}

	@Override
	protected int[] getPosXY() {
		return posXY;
	}

	@Override
	protected void addControls() {
		txtThreshold = dialogHelper.addInputOption("Threshold", null, null, "",
				"", true);
		setThreshold();
//		txtInclude = dialogHelper.addInputOption("IncludeTop", "Include Top", null,
	//			"peaks", "10");
//		txtSkip = dialogHelper.addInputOption("SkipHighest", "Skip Highest", null,
//				"peaks", "0");
		cbInterpolation = dialogHelper.addSelectOption("Interpolation", null,
				new String[] { "parabolic", "none" }, 0, true);
//		txtThreshold.addFocusListener(this);
//		txtInclude.addFocusListener(this);
	}

	private void setThreshold() {
		Coordinate c = jsvp.getPanelData().getClickedCoordinate();
		double y = (c == null ? (jsvp.getPanelData().getView().maxYOnScale + jsvp.getPanelData().getView().maxYOnScale)/2
				: c.getYVal());
		String sy = TextFormat.getDecimalFormat(y < 1000 ? "#0.00" : "#0.0E0").format(y);
		txtThreshold.setText(" " + sy);
	}

	@Override
	protected void checkEnables() {
	}


	/*		String script = "PEAKLIST";
			String s = txtThreshold.getText();
			if (s.startsWith("("))
				script += " include=" + txtInclude.getText();
			else
				script += " threshold=" + s;
			script += " skip=" + txtSkip.getText();
			script += " interpolate=" + cbInterpolation.getSelectedItem().toString();
			System.out.println(script);
			JSViewer.runScriptNow(si, script);
	*/		

	@Override
	public void apply() {
		setThreshold();
		createData();
		super.apply();
	}

	@Override
	protected void setParams() {
		try {
//			String s = txtInclude.getText();
//			 myParams.peakListInclude = (s.startsWith("(") ? -1 :  Integer.valueOf(s));
			 String s = txtThreshold.getText();
			 myParams.peakListThreshold = (/*s.startsWith("(") ? -1 : */ Double.valueOf(s));
//			 myParams.peakListSkip = Integer.valueOf(txtSkip.getText());
			 myParams.peakListInterpolation = cbInterpolation.getSelectedItem().toString();
			 super.setParams();
		} catch (Exception e) {
			//
		}
	}

	protected void clear() {
		super.clear();
	}
		
	@Override
	protected void done() {
		super.done();
	}

	@Override
	protected void updateValues() {
		loadData();
	}

	public void focusGained(FocusEvent e) {
//		if (e.getSource() == txtThreshold) {
//			setEnabled(txtThreshold, txtInclude);
//		} else {
//			setEnabled(txtInclude, txtThreshold);
//		}
		// TODO Auto-generated method stub
		
	}

//	private static void setEnabled(JTextField txtEnabled, JTextField txtDisabled) {
//		txtEnabled.setText(cleanText(txtEnabled));
//		txtDisabled.setText("(" + cleanText(txtDisabled) + ")");
//		txtDisabled.setBackground(Color.lightGray);
//		txtEnabled.setBackground(Color.white);
//	}

//	private static String cleanText(JTextField t) {
//		return t.getText().replace('(',' ').replace(')', ' ').trim();
//	}

	public void focusLost(FocusEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void createData() {
		setParams();
		MeasurementData md = new MeasurementData(AType.PeakList, spec);
	  md.setPeakList(myParams, numberFormatter, jsvp.getPanelData().getView());
		xyData = md;
		loadData();
	}

	private void loadData() {
		if (xyData == null)
			createData();
		String[][] data = xyData.getPeakListArray();
		String[] header = (data.length == 0 ? new String[] {}
    : data[0].length == 3 ? new String[] { "peak", spec.getXUnits(), spec.getYUnits() }
    : new String[] { "peak", "shift/ppm", "intens" , "shift/hz", "diff/hz", "2-diff" });
		int[] widths = new int[] {40, 65, 50, 50, 50, 50};
		loadData(data, header, widths);
	}

	public synchronized void update(Coordinate clicked) {
		apply();
		if (xyData == null)
			return;
		int ipt = 0;
		double dx0 = 1e100;
		double xval = clicked.getXVal();
		MeasurementData md = (MeasurementData) xyData;
		for (int i = md.size(); --i >= 0;) {
			double dx = Math.abs(xval - md.get(i).getXVal());
			if (dx < dx0) {
				dx0 = dx;
				ipt = i;
			}
			if (dx0 < 0.1)
				dataTable.getSelectionModel().setSelectionInterval(md.size() - 2 - ipt,
						md.size() - 1 - ipt);
		}
	}

	public void tableRowSelectedEvent(int iRow) {
		String value = tableData[iRow][1];
		jsvp.getPanelData().findX(Double.parseDouble(value));
		jsvp.repaint();
	}
}
