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

import java.awt.BorderLayout;
import jspecview.util.JSVColor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;

import jspecview.common.JSVPanel;

/**
 * Dialog for showing the legend or key for overlaid plots in a
 * <code>JSVPanel</code>.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class AwtDialogOverlayLegend extends AwtDialog {

	private static final long serialVersionUID = 1L;
	protected JSVPanel jsvp;

	/**
	 * Initialises a non-modal <code>OverlayLegendDialog</code> with a default
	 * title of "Legend: " + jsvp.getTitle() and parent frame
	 * 
	 * @param frame
	 *          the parent frame
	 * @param jsvp
	 *          the <code>JSVPanel</code>
	 */
	public AwtDialogOverlayLegend(Frame frame, JSVPanel jsvp) {
		super(frame, jsvp.getPanelData().getViewTitle(), false);
		this.jsvp = jsvp;
		initDialog();
		pack();
		setVisible(false);
	}

	private void initDialog() {
		AwtDialogTableModel tableModel = new AwtDialogTableModel(new String[] {
				"No.", "Plot Color", "Title" }, jsvp.getOverlayLegendData(), false);
		JTable table = new JTable(tableModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ListSelectionModel specSelection = table.getSelectionModel();
		specSelection.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				jsvp.getPanelData().setSpectrum(lsm.getMinSelectionIndex(), false);
			}
		});
		table.setDefaultRenderer(JSVColor.class, new ColorRenderer());
		table.setDefaultRenderer(String.class, new TitleRenderer());
		table.setPreferredScrollableViewportSize(new Dimension(350, 95));
		setColWidth(table, 0, 30);
		setColWidth(table, 1, 60);
		setColWidth(table, 2, 250);
		JScrollPane scrollPane = new JScrollPane(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
	}

	private static void setColWidth(JTable table, int i, int width) {
		table.getColumnModel().getColumn(i).setPreferredWidth(width);
	}

	/**
	 * TableCellRenderer that allows the colors to be displayed in a JTable cell
	 */
	class ColorRenderer extends JLabel implements TableCellRenderer {

		private static final long serialVersionUID = 1L;

		public ColorRenderer() {
			setOpaque(true);
		}

		public Component getTableCellRendererComponent(JTable table, Object color,
				boolean isSelected, boolean hasFocus, int row, int column) {
			Border border;
			setBackground((Color) color);
			if (isSelected) {
				border = BorderFactory.createMatteBorder(2, 5, 2, 5, table
						.getSelectionBackground());
				setBorder(border);
			} else {
				border = BorderFactory.createMatteBorder(2, 5, 2, 5, table
						.getBackground());
				setBorder(border);
			}
			return this;
		}
	}

	/**
	 * TableCellRenderer that aligns text in the center of a JTable Cell
	 */
	class TitleRenderer extends JLabel implements TableCellRenderer {

		private static final long serialVersionUID = 1L;

		public TitleRenderer() {
			setOpaque(true);
		}

		public Component getTableCellRendererComponent(JTable table, Object title,
				boolean isSelected, boolean hasFocus, int row, int column) {
			setHorizontalAlignment(SwingConstants.LEFT);
			setText(title.toString());
			setBackground(isSelected ? table.getSelectionBackground() : table
					.getBackground());
			return this;
		}
	}

}
