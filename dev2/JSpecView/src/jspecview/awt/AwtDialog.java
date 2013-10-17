package jspecview.awt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.Hashtable;
import java.util.Map;

import javajs.api.GenericColor;

import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;

import jspecview.api.PlatformDialog;
import jspecview.common.Annotation.AType;
import jspecview.dialog.DialogManager;
import jspecview.dialog.JSVDialog;

/**
 * AwtDialog extends JDialog, interpreting DialogManager's requests in terms of Swing and AWT.
 *  
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class AwtDialog extends JDialog implements PlatformDialog {

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
		/**
     * 
     */
		private static final long serialVersionUID = 1L;

		public TitleRenderer() {
			setOpaque(true);
		}

		public Component getTableCellRendererComponent(JTable table, Object title,
				boolean isSelected, boolean hasFocus, int row, int column) {
			setHorizontalAlignment(getColumnCentering(column));
			setText(title.toString());
			// ignore selection model
			isSelected = (row == selectedRow);
			setBackground(isSelected ? table.getSelectionBackground() : table
					.getBackground());
			return this;
		}
	}
	protected String optionKey;
	protected String registryKey;

	protected Map<String, Object> options;
	protected DialogManager manager;

  private AType type;
	protected JPanel leftPanel;
	private JSplitPane mainSplitPane;

	private JPanel rightPanel;
	private JPanel thisPanel;
	private JTable dataTable;
	private int iRow;
	private boolean haveColors;


	protected boolean tableCellAlignLeft;

	private boolean haveTwoPanels = true;

	private Insets buttonInsets = new Insets(5, 5, 5, 5);
	
	private Insets panelInsets = new Insets(0, 0, 2, 2);
	private int defaultHeight = 350;
	protected int selectedRow = -1;
	
	public AwtDialog(DialogManager manager, JSVDialog jsvDialog, String registryKey) {
  	this.manager = manager;
		this.registryKey = registryKey;
		type = jsvDialog.getAType();
		optionKey = jsvDialog.optionKey;
		options = jsvDialog.options;
		if (options == null)
			options = new Hashtable<String, Object>();
	}

	protected int getColumnCentering(int column) {
		return tableCellAlignLeft ? SwingConstants.LEFT : column == 0 ? SwingConstants.CENTER
				: SwingConstants.RIGHT;
	}

	public Object addButton(String name, String text) {
		JButton	btn = new JButton();
		btn.setPreferredSize(new Dimension(120, 25));
		btn.setText(text);
		btn.setName(registryKey + "/" + name);
		btn.addActionListener((AwtDialogManager) manager);
		thisPanel.add(btn, new GridBagConstraints(0, iRow++, 3, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets,
				0, 0));
		return btn;
	}

	public Object addCheckBox(String name, String title, int level,
			boolean isSelected) {
		if (name == null) {
			// reset row counter
			iRow = 0;
			thisPanel = rightPanel;
			return null;
		}
  	JCheckBox cb = new JCheckBox();
  	cb.setSelected(isSelected);
  	cb.setText(title);
  	cb.setName(name);
    Insets insets = new Insets(0, 20 * level, 2, 2);
    thisPanel.add(cb, new GridBagConstraints(0, iRow++, 1, 1, 0.0, 0.0,
    		GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
    return cb;
	}

	private void addPanelLine(String name, String label, JComponent obj,
			String units) {
		thisPanel.add(new JLabel(label == null ? name : label),
				new GridBagConstraints(0, iRow, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE, panelInsets, 0, 0));
		if (units == null) {
			thisPanel.add(obj, new GridBagConstraints(1, iRow, 2, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE, panelInsets, 0, 0));
		} else {
			thisPanel.add(obj, new GridBagConstraints(1, iRow, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, panelInsets, 0, 0));
			thisPanel.add(new JLabel(units), new GridBagConstraints(2, iRow, 1, 1,
					0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, panelInsets,
					0, 0));
		}
		iRow++;
	}

	public Object addSelectOption(String name, String label,
			String[] info, int iPt, boolean visible) {
		JComboBox<String> combo = new JComboBox<String>(info);
		combo.setSelectedIndex(iPt);
		combo.setName(name);
		if (visible) {
			combo.addActionListener((AwtDialogManager) manager);
			addPanelLine(name, label, combo, null);
	  }
		return combo;
	}

	//// get/set methods ////
	
	public Object addTextField(String name, String label, String value,
			String units, String defaultValue, boolean visible) {
		String key = optionKey + "_" + name;
		if (value == null) {
			value = (String) options.get(key);
			if (value == null)
				options.put(key, (value = defaultValue));
		}
		JTextField obj = new JTextField(value);
		obj.setName(registryKey + "/" + name);
		if (visible) {
			obj.setPreferredSize(new Dimension(75, 25));
			obj.addActionListener((AwtDialogManager)manager);
  		addPanelLine(name, label, obj, units);
		}
		return obj;
	}

	public void createTable(Object[][] data, String[] header, int[] widths) {
		try {
			JScrollPane scrollPane = new JScrollPane(
					dataTable = getDataTable(data, header,
							widths, (leftPanel == null ? defaultHeight  : leftPanel.getHeight() - 50)));
			if (mainSplitPane == null) {
				getContentPane().add(scrollPane);				 
			} else {
				mainSplitPane.setRightComponent(scrollPane);
			}
		} catch (Exception e) {
			// not perfect.
		}
		validate();
		repaint();
	}

	public void endLayout() {
		getContentPane().removeAll();
		getContentPane().add(mainSplitPane);
		pack();
	}

	private synchronized JTable getDataTable(Object[][] data, String[] columnNames, int[] columnWidths, int height) {
    selectedRow = -1;		
		DialogTableModel tableModel = new DialogTableModel(columnNames, data, !haveColors);
		JTable table = new JTable(tableModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		if (haveColors)
			table.setDefaultRenderer(GenericColor.class, new ColorRenderer());
    table.setDefaultRenderer(String.class, new TitleRenderer());
    table.setCellSelectionEnabled(true);
    ListSelectionModel selector = table.getSelectionModel();
    selector.addListSelectionListener((AwtDialogManager) manager);
    manager.registerSelector(registryKey + "/ROW", selector);
    selector = table.getColumnModel().getSelectionModel();
    selector.addListSelectionListener((AwtDialogManager) manager);
    manager.registerSelector(registryKey + "/COL", selector);
		int n = 0;
		for (int i = 0; i < columnNames.length; i++) {
			table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
			n += columnWidths[i];
		}
		table.setPreferredScrollableViewportSize(new Dimension(n, height));
		return table;
	}

	public int getSelectedIndex(Object c) {
		return ((JComboBox<?>) c).getSelectedIndex();
	}

	public Object getSelectedItem(Object combo) {
		return ((JComboBox<?>)combo).getSelectedItem();
	}

	public String getText(Object o) {
		return (o instanceof JTextComponent ? ((JTextComponent) o).getText()
				: ((AbstractButton) o).getText());	
	}

	
	public boolean isSelected(Object chkbox) {
		return ((JCheckBox) chkbox).isSelected();
	}
	
	@Override
	public void repaint() {
		if (dataTable != null) {
			dataTable.clearSelection();
			if (selectedRow >= 0) {
				dataTable.setRowSelectionAllowed(true);
				dataTable.setRowSelectionInterval(selectedRow, selectedRow + 1);
			}
		}
		
	}

	public void selectTableRow(int i) {
		selectedRow = i;
		dataTable.clearSelection();
	}

	public void setCellSelectionEnabled(boolean enabled) {
		dataTable.setCellSelectionEnabled(enabled);
	}

  //// Table-related methods ////
	
	
	public void setEnabled(Object btn, boolean b) {
		((Component) btn).setEnabled(b);
	}

	public void setIntLocation(int[] loc) {
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		loc[0] = Math.min(d.width - 50, loc[0]);
		loc[1] = Math.min(d.height - 50, loc[1]);
		Point pt = new Point(loc[0], loc[1]);
		setLocation(pt);
	}
	public void setPreferredSize(int width, int height) {
		setPreferredSize(new Dimension(width, height));
	}

	public void setSelected(Object chkbox, boolean b) {
		((JCheckBox) chkbox).setSelected(b);
	}

	public void setSelectedIndex(Object combo, int i) {
		((JComboBox<?>) combo).setSelectedIndex(i);
	}

	public void setText(Object o, String text) {
	  if (o instanceof JTextComponent)
	  	((JTextComponent) o).setText(text);
	  else
	  	((AbstractButton) o).setText(text);
	}

	public void startLayout() {
		setPreferredSize(new Dimension(600, 370)); // golden ratio
    getContentPane().removeAll();
		thisPanel = rightPanel = new JPanel();
		switch (type) {
		case Integration:
		case Measurements:
		case PeakList:
		case NONE:
			break;
		case OverlayLegend:
			tableCellAlignLeft = true;
			haveColors = true;
			haveTwoPanels = false;
			break;
		case Views:
			rightPanel = new JPanel(new GridBagLayout());
		}
		if (haveTwoPanels) {
			thisPanel = leftPanel = new JPanel(new GridBagLayout());
			leftPanel.setMinimumSize(new Dimension(200, 300));
			mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			mainSplitPane.setOneTouchExpandable(true);
			mainSplitPane.setResizeWeight(0);
			mainSplitPane.setLeftComponent(leftPanel);
			mainSplitPane.setRightComponent(new JScrollPane(rightPanel));
		}
			
	}
}
