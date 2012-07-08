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

import java.util.Map;

import jspecview.common.Annotation.AType;

/**
 * Dialog for managing the measurement list
 * for a Spectrum within a GraphSet

 * @author Bob Hanson hansonr@stolaf.edu
 */

class AwtMeasurementListDialog extends AwtAnnotationDialog {

	private static final long serialVersionUID = 1L;
	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};

	protected AwtMeasurementListDialog(String title, ScriptInterface si, 
			JDXSpectrum spec, JSVPanel jsvp, Map<String, Object> data) {
		super(title, si, spec, jsvp, data);
		thisType = AType.Measurements;
		setTitle("Measurement Listing");
		setup();
	}

	@Override
	protected int[] getPosXY() {
		return posXY;
	}

	@Override
	protected void addControls() {
		// none required
	}

	@Override
	protected void checkEnables() {
	}

	@Override
	public void apply() {
		super.apply();
  }

	@Override
	protected void done() {
		super.done();
	}

	@Override
	protected void updateValues() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void createData() {
		// TODO Auto-generated method stub
		
	}

	public void tableRowSelectedEvent(int minSelectionIndex) {
		// TODO Auto-generated method stub
		
	}

	public void update(Coordinate clicked) {
		// TODO Auto-generated method stub
		
	}


}