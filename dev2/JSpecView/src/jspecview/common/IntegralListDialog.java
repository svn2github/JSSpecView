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

import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import jspecview.common.Annotation.AType;

/**
 * Dialog for managing the integral listing 
 * for a Spectrum within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

class IntegralListDialog extends AnnotationDialog {

	private static final long serialVersionUID = 1L;
	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};
	private JTextField txtRange;
	private JTextField txtOffset;
	private JTextField txtNormalization;
	
	private JCheckBox chkResets; // not yet implemented

	protected IntegralListDialog(String title, ScriptInterface si, JDXSpectrum spec, 
			JSVPanel jsvp, Map<String, Object> data) {
		super(title, si, spec, jsvp, data);
		thisType = AType.Integration;
		setTitle("Integration Listing");
		setup();
		
	}

	@Override
	protected int[] getPosXY() {
		return posXY;
	}

	private int iSelected = -1;
	private JButton normalizeButton;
	
	@Override
	protected void addControls() {
		txtRange = dialogHelper.addInputOption("Scale", "Scale", null, "%", ""
				+ si.getParameters().integralRange);
		txtOffset = dialogHelper.addInputOption("BaselineOffset", "Baseline Offset", null, "%",
				"" + si.getParameters().integralOffset);
		chkResets = dialogHelper.addCheckBoxOption("BaselineResets", "Baseline Resets", true);
		txtNormalization = dialogHelper.addInputOption("NormalizationFactor",
				"Normalization Factor", null, null, "1.0");
		normalizeButton = newJButton();
		normalizeButton.setText("Normalize");
		normalizeButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				si.runScript("normalize " + iSelected + " "
						+ txtNormalization.getText());
			}
		});
		dialogHelper.addButton(normalizeButton);
	}

	@Override
	protected void checkEnables() {
		normalizeButton.setEnabled(iSelected >= 0);
    boolean isShow = si.getSelectedPanel().getPanelData().getShowIntegration();
		showHideButton.setText(isShow ? "Hide" : "Show");				
	}

	@Override
	public void apply() {
		try {
			myParams.integralOffset = Double.valueOf(txtOffset.getText());
			myParams.integralRange = Double.valueOf(txtRange.getText());
			((IntegralData) getData()).update(myParams);
			jsvp.repaint();
			super.apply();
		} catch (Exception e) {
			// ignore?
		}
		//JSViewer.runScriptNow(si, "integralOffset " + txtOffset.getText() 
			//	+ ";integralRange " + txtRange.getText() + ";showIntegration");
	}
	
	@Override
	protected void done() {
		super.done();
	}

	@Override
	protected void updateValues() {
		// TODO Auto-generated method stub
		
	}

	protected void clear() {
		jsvp.getPanelData().checkIntegral(si.getParameters(), "off");
		super.clear();
	}
		
	public boolean checkParameters(Parameters p) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected AnnotationData createData() {
		IntegralData data = new IntegralData(spec, myParams);
		setMeasurements(data);
		return data;
	}

}