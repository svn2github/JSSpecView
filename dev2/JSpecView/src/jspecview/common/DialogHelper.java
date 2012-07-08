package jspecview.common;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * just a class I made to separate the construction of the AnnotationDialogs
 * from their use
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class DialogHelper {

	private String thisKey;
	private ActionListener eventListener;
	private Map<String, Object> options;
	private JPanel leftPanel;
	private Insets buttonInsets = new Insets(5, 5, 5, 5);
	private Insets cbInsets = new Insets(0, 0, 2, 2);
	private int iRow;

	public DialogHelper() {
	}

	public DialogHelper(String thisKey, Map<String, Object> options,
			JPanel leftPanel, ActionListener eventListener) {
		this.thisKey = thisKey;
		this.options = options;
		this.leftPanel = leftPanel;
		this.eventListener = eventListener;
	}

	protected void addButton(JButton selectAllButton) {
		leftPanel.add(selectAllButton, new GridBagConstraints(0, iRow++, 3, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets,
				0, 0));
	}

	protected JCheckBox addCheckBoxOption(String name, String label,
			boolean isSelected) {
		JCheckBox obj = new JCheckBox();
		obj.setText(label == null ? name : label);
		obj.addActionListener(eventListener);
		leftPanel.add(obj, new GridBagConstraints(1, iRow, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, cbInsets, 0, 0));
		iRow++;
		return obj;
	}

	protected JTextField addInputOption(String name, String label, String value,
			String units, String defaultValue) {
		String key = thisKey + "_" + name;
		if (value == null) {
			value = (String) options.get(key);
			if (value == null)
				options.put(key, (value = defaultValue));
		}
		JTextField obj = new JTextField((String) value);
		obj.setPreferredSize(new Dimension(50, 25));
		obj.addActionListener(eventListener);
		addPanelLine(name, label, obj, units);
		return obj;
	}

	protected JComboBox addSelectOption(String name, String label, String[] info,
			int iPt) {
		JComboBox obj = new JComboBox(info);
		obj.setSelectedIndex(iPt);
		obj.setActionCommand(name);
		// obj.setPreferredSize(new Dimension(100, 25));
		obj.addActionListener(eventListener);
		addPanelLine(name, label, obj, null);
		return obj;
	}

	private void addPanelLine(String name, String label, JComponent obj,
			String units) {
		leftPanel.add(new JLabel(label == null ? name : label),
				new GridBagConstraints(0, iRow, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE, cbInsets, 0, 0));
		if (units == null) {
			leftPanel.add(obj, new GridBagConstraints(1, iRow, 2, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE, cbInsets, 0, 0));
		} else {
			leftPanel.add(obj, new GridBagConstraints(1, iRow, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, cbInsets, 0, 0));
			leftPanel.add(new JLabel(units), new GridBagConstraints(2, iRow, 1, 1,
					0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, cbInsets,
					0, 0));
		}
		iRow++;
	}

	protected synchronized JTable getDataTable(JSVPanel jsvp0, String[] columnNames,
			String[][] data) {
		
		final JSVPanel jsvp = jsvp0;
		final String[][] d = data;
		
		LegendTableModel tableModel = new LegendTableModel(columnNames, data);
		JTable table = new JTable(tableModel);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setDefaultRenderer(String.class, new TitleRenderer());

    ListSelectionModel specSelection = table.getSelectionModel();
		specSelection.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				try {
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				String value = d[lsm.getMinSelectionIndex()][1];
				jsvp.getPanelData().findX(Double.parseDouble(value));
				jsvp.repaint();
				} catch (Exception ee) {
					//ignore
				}
			}
		});
		table.setPreferredScrollableViewportSize(new Dimension(350, 95));
		TableColumn c = table.getColumnModel().getColumn(0);
		c.setPreferredWidth(30);
		c = table.getColumnModel().getColumn(1);
		c.setPreferredWidth(60);
		c = table.getColumnModel().getColumn(2);
		c.setPreferredWidth(60);
		c = table.getColumnModel().getColumn(3);
		c.setPreferredWidth(60);
		c = table.getColumnModel().getColumn(4);
		c.setPreferredWidth(60);
		return table;
	}

  /**
   * TableCellRenderer that aligns text in the center of a JTable Cell
   */
  class TitleRenderer extends JLabel
                      implements TableCellRenderer {
      /**
     * 
     */
    private static final long serialVersionUID = 1L;


      public TitleRenderer(){
        setOpaque(true);
      }


      public Component getTableCellRendererComponent(
                              JTable table, Object title,
                              boolean isSelected, boolean hasFocus,
                              int row, int column) {
          setHorizontalAlignment(SwingConstants.RIGHT);
          setText(title.toString());
          if(isSelected)
            setBackground(table.getSelectionBackground());
          else
            setBackground(table.getBackground());

          return this;
      }
  }
	/**
	 * The Table Model for Legend
	 */
	class LegendTableModel extends AbstractTableModel {
		/**
     * 
     */
		private static final long serialVersionUID = 1L;
		String[] columnNames;
		Object[][] data;

		public LegendTableModel(String[] columnNames, String[][] data) {
			this.columnNames = columnNames;
			this.data = data;
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

	}

}
