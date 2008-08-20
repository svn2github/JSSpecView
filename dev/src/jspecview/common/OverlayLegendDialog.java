/* Copyright (c) 2002-2007 The University of the West Indies
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Dialog for showing the legend or key for overlayed plots in a <code>JSVPanel</code>.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class OverlayLegendDialog extends JDialog {

  JSVPanel jsvp;

  /**
   * Initialises the <code>OverlayLegendDialog</code>
   * @param frame the parent frame
   * @param title the title
   * @param modal the modality
   * @param jsvp the <code>JSVPanel</code>
   */
  public OverlayLegendDialog(Frame frame, String title, boolean modal, JSVPanel jsvp) {
    super(frame, title, modal);
    this.jsvp = jsvp;
    init();
    this.pack();
    this.setVisible(true);

  }

  /**
   * Initialises a non-modal <code>OverlayLegendDialog</code> with a default title of
   * "Legend: " + jsvp.getTitle() and null parent
   * @param jsvp the <code>JSVPanel</code>
   */
  public OverlayLegendDialog(JSVPanel jsvp) {
    this(null, ("Legend: " + jsvp.getTitle()), false, jsvp);
  }

  /**
   * Initialises a non-modal <code>OverlayLegendDialog</code> with a default title of
   * "Legend: " + jsvp.getTitle() and parent frame
   * @param frame the parent frame
   * @param jsvp the <code>JSVPanel</code>
   */
  public OverlayLegendDialog(Frame frame, JSVPanel jsvp) {
    this(frame, ("Legend: " + jsvp.getTitle()), false, jsvp);
  }

  /**
   * Initialises GUI Components
   */
  private void init(){

    LegendTableModel tableModel = new LegendTableModel();
    JTable table = new JTable(tableModel);
    table.setDefaultRenderer(Color.class, new ColorRenderer());
    table.setDefaultRenderer(String.class, new TitleRenderer());
    table.setPreferredScrollableViewportSize(new Dimension(350, 95));
    TableColumn column = null;
    column = table.getColumnModel().getColumn(0);
    column.setPreferredWidth(30);
    column = table.getColumnModel().getColumn(1);
    column.setPreferredWidth(60);
    column = table.getColumnModel().getColumn(2);
    column.setPreferredWidth(250);


    JScrollPane scrollPane = new JScrollPane(table);
    getContentPane().add(scrollPane, BorderLayout.CENTER);
  }

  /**
   * The Table Model for Legend
   */
  class LegendTableModel extends AbstractTableModel {
        String[] columnNames = {"No.", "Plot Color", "Title"};
        Object[][] data;

        public LegendTableModel(){
          init();
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }


        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        private void init(){
          Color plotColor;
          String title;
          JDXSpectrum spectrum;
          Object[] row;

          int numSpectra = jsvp.getNumberOfSpectra();
          data = new Object[numSpectra][];

          for(int index = 0; index < numSpectra; index++){
            row = new Object[3];

            spectrum = (JDXSpectrum)jsvp.getSpectrumAt(index);
            title = spectrum.getTitle();
            plotColor = jsvp.getPlotColor(index);

            row[0] = new Integer(index + 1);
            row[1] = plotColor;
            row[2] = title;

            data[index] = row;
          }
        }
    }

    /**
     * TableCellRenderer that allows the colors to be displayed in a JTable cell
     */
    class ColorRenderer extends JLabel
                        implements TableCellRenderer {

        public ColorRenderer() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(
                                JTable table, Object color,
                                boolean isSelected, boolean hasFocus,
                                int row, int column) {
            Border border;
            setBackground((Color)color);
            if (isSelected) {
              border = BorderFactory.createMatteBorder(2,5,2,5,
                                        table.getSelectionBackground());
              setBorder(border);
            }
            else {
              border = BorderFactory.createMatteBorder(2,5,2,5,
                                        table.getBackground());
              setBorder(border);
            }

            return this;
        }
    }

    /**
     * TableCellRenderer that aligns text in the center of a JTable Cell
     */
    class TitleRenderer extends JLabel
                        implements TableCellRenderer {
        public TitleRenderer(){
          setOpaque(true);
        }


        public Component getTableCellRendererComponent(
                                JTable table, Object title,
                                boolean isSelected, boolean hasFocus,
                                int row, int column) {
            setHorizontalAlignment(SwingConstants.CENTER);
            setText(title.toString());
            //setText("   " + title.toString());

            if(isSelected)
              setBackground(table.getSelectionBackground());
            else
              setBackground(table.getBackground());

            return this;
        }
    }
}
