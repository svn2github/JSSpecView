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

package jspecview.java;

import jspecview.api.ScriptInterface;
import jspecview.common.JDXSpectrum;
import jspecview.common.Annotation.AType;

/**
 * Dialog for managing the peak listing 
 * for a Spectrum within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class AwtDialogPeakList extends AwtAnnotationDialog {

	private static final long serialVersionUID = 1L;
	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};

	protected AwtDialogPeakList(String title, ScriptInterface si, JDXSpectrum spec) {
		super(title, si, spec, AType.PeakList);
	}

	public int[] getPosXY() {
		return posXY;
	}

	@Override
	public void addUniqueControls(AwtDialogHelper dialogHelper) {
		txt1 = dialogHelper.addInputOption("Threshold", null, null, "",
				"", true);
		setThreshold(Double.NaN);
		chkbox1 = dialogHelper.addSelectOption("Interpolation", null,
				new String[] { "parabolic", "none" }, 0, true);
	}

	// from DialogParams:

	@Override
	public void setParamsFromFields() {
		params.setParams(new Object[] {txt1.getText(), chkbox1.getSelectedItem().toString()});
	}

}
