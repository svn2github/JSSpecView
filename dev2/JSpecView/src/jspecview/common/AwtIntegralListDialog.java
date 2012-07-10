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
import java.text.DecimalFormat;
import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import jspecview.common.Annotation.AType;
import jspecview.util.TextFormat;

/**
 * Dialog for managing the integral listing 
 * for a Spectrum within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

class AwtIntegralListDialog extends AwtAnnotationDialog {

	private static final long serialVersionUID = 1L;
	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};
	private JTextField txtRange;
	private JTextField txtOffset;
	//private JTextField txtNormalization;
	
	private AwtIntegralListDialog dialog;

	protected AwtIntegralListDialog(String title, ScriptInterface si, JDXSpectrum spec, 
			JSVPanel jsvp) {
		super(title, si, spec, jsvp);
		thisType = AType.Integration;
		setTitle("Integration Listing");
		setup();
		xyData = new IntegralData(spec, myParams);
		dialog = this;
	}

	@Override
	protected int[] getPosXY() {
		return posXY;
	}

	private int iSelected = -1;
	private JButton normalizeButton;
	
	protected double lastNorm = 1.0;
	//private JCheckBox chkResets;
	
	
	@Override
	protected void addControls() {
		txtRange = dialogHelper.addInputOption("Scale", "Scale", null, "%", ""
				+ si.getParameters().integralRange, true);
		txtOffset = dialogHelper.addInputOption("BaselineOffset", "Baseline Offset", null, "%",
				"" + si.getParameters().integralOffset, true);
		//chkResets = dialogHelper.addCheckBoxOption("BaselineResets", "Baseline Resets", true);
		normalizeButton = newJButton();
		normalizeButton.setText("Normalize");
		normalizeButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				normalize();
			}
		});
		dialogHelper.addButton(normalizeButton);
	}

	protected void normalize() {
		try {
			if (iSelected < 0) {
				JOptionPane.showMessageDialog(dialog,
						"Select a line on the table first, then click this button.");
				return;
			}
			String ret = (String) JOptionPane.showInputDialog(dialog,
					"Enter a normalization factor", "Normalize",
					JOptionPane.QUESTION_MESSAGE, null, null, "" + lastNorm);
			double val = Double.parseDouble(ret);
			if (val <= 0)
				return;
			lastNorm = val;
			((IntegralData) xyData).setSelectedIntegral(xyData.get(iSelected), val);
			apply();
			jsvp.repaint();
		} catch (Exception ee) {
			// ignore
		}
	}

	@Override
	protected void checkEnables() {
    boolean isShow = si.getSelectedPanel().getPanelData().getShowIntegration();
		showHideButton.setText(isShow ? "Hide" : "Show");				
	}

	@Override
	public void apply() {
		try {
			myParams.integralOffset = Double.valueOf(txtOffset.getText());
			myParams.integralRange = Double.valueOf(txtRange.getText());
			myParams.integralDrawAll = false;//chkResets.isSelected();
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

	public void update(Coordinate clicked) {
		updateValues();
		checkEnables();
	}

	@Override
	protected void updateValues() {
		loadData();
	}

	private void loadData() {
		if (xyData == null)
			createData();
		iSelected = -1;
		String[][] data = ((IntegralData) xyData).getIntegralListArray();
		String[] header = xyData.getDataHeader();
		int[] widths = new int[] {40, 65, 65, 50};
		loadData(data, header, widths);
	}

	public boolean checkParameters(Parameters p) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void createData() {
		xyData = new IntegralData(spec, myParams);
		iSelected = -1;
	}

	public void tableCellSelectedEvent(int iRow, int iCol) {
		DecimalFormat df2 = TextFormat.getDecimalFormat("#0.00");
		String value = tableData[iRow][1];
		for (int i = 0; i < xyData.size(); i++) 
			if (df2.format(xyData.get(i).getXVal()).equals(value)) {
				iSelected = i;
				jsvp.getPanelData().findX2(xyData.get(i).getXVal(), xyData.get(i).getXVal2());
				jsvp.repaint();
				break;
			}		
		checkEnables();
	}


}