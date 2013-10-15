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

// CHANGES to 'JSVPanel.java'
// University of the West Indies, Mona Campus
//
// 25-06-2007 rjl - bug in ReversePlot for non-continuous spectra fixed
//                - previously, one point less than npoints was displayed
// 25-06-2007 cw  - show/hide/close modified
// 10-02-2009 cw  - adjust for non zero baseline in North South plots
// 24-08-2010 rjl - check coord output is not Internationalised and uses decimal point not comma
// 31-10-2010 rjl - bug fix for drawZoomBox suggested by Tim te Beek
// 01-11-2010 rjl - bug fix for drawZoomBox
// 05-11-2010 rjl - colour the drawZoomBox area suggested by Valery Tkachenko
// 23-07-2011 jak - Added feature to draw the x scale, y scale, x units and y units
//					independently of each other. Added independent controls for the font,
//					title font, title bold, and integral plot color.
// 24-09-2011 jak - Altered drawGraph to fix bug related to reversed highlights. Added code to
//					draw integration ratio annotations
// 03-06-2012 rmh - Full overhaul; code simplification; added support for Jcamp 6 nD spectra

package jspecview.awtjs2d;

import jspecview.dialog.PlatformDialog;


public class JsDialog implements PlatformDialog {

	public Object addButton(String name, String title) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object addCheckBox(String name, String title, int level,
			boolean isSelected) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object addSelectOption(String name, String label, String[] info,
			int iPt, boolean visible) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object addTextField(String name, String label, String value,
			String units, String defaultValue, boolean visible) {
		// TODO Auto-generated method stub
		return null;
	}

	public void createTable(Object[][] data, String[] header, int[] widths) {
		// TODO Auto-generated method stub
		
	}

	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	public void endLayout() {
		// TODO Auto-generated method stub
		
	}

	public int getSelectedIndex(Object combo1) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Object getSelectedItem(Object combo) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getText(Object txt) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isSelected(Object chkbox) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isVisible() {
		// TODO Auto-generated method stub
		return false;
	}

	public void pack() {
		// TODO Auto-generated method stub
		
	}

	public void selectTableRow(int i) {
		// TODO Auto-generated method stub
		
	}

	public void setCellSelectionEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		
	}

	public void setEnabled(Object btn, boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void setIntLocation(int[] loc) {
		// TODO Auto-generated method stub
		
	}

	public void setPreferredSize(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	public void setSelected(Object chkbox, boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void setSelectedIndex(Object combo, int i) {
		// TODO Auto-generated method stub
		
	}

	public void setText(Object showHideButton, String label) {
		// TODO Auto-generated method stub
		
	}

	public void setTitle(String title) {
		// TODO Auto-generated method stub
		
	}

	public void setVisible(boolean visible) {
		// TODO Auto-generated method stub
		
	}

	public void startLayout() {
		// TODO Auto-generated method stub
		
	}

	public void repaint() {
		// TODO Auto-generated method stub
		
	}

}
