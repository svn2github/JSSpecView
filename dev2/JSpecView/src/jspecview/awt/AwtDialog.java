package jspecview.awt;

import java.awt.BorderLayout;
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

import jspecview.common.Annotation.AType;
import jspecview.dialog.DialogManager;
import jspecview.dialog.DialogParams;
import jspecview.dialog.PlatformDialog;
import jspecview.util.JSVColor;

/**
 * just a class I made to separate the construction of the AnnotationDialogs
 * from their use
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class AwtDialog extends JDialog implements PlatformDialog {

	protected String thisKey;
	protected String thisID;
	protected Map<String, Object> options;
	protected DialogManager manager;

	private AType type;
	private DialogParams params;

  protected JPanel leftPanel;
	private JSplitPane mainSplitPane;
	private JPanel rightPanel;
	private JPanel thisPanel;

	private JTable dataTable;
	private int iRow;
	private boolean haveColors;
	protected boolean tableCellAlignLeft;
	private boolean haveTwoPanels = true;


	public AwtDialog(DialogManager manager, DialogParams params, String thisID) {
  	this.manager = manager;
  	this.params = params;
		this.thisKey = params.thisKey;
		this.type = params.thisType;
		this.thisID = thisID;
	}

	public void startLayout() {
		setPreferredSize(new Dimension(600, 370)); // golden ratio
    getContentPane().removeAll();
		thisPanel = rightPanel = new JPanel();
		switch (type) {
		case Integration:
		case Measurements:
		case PeakList:
			options = params.options;
			if (options == null)
				options = new Hashtable<String, Object>();
			break;
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

	public void endLayout() {
		getContentPane().removeAll();
		getContentPane().add(mainSplitPane);
		pack();
	}
	
	private Insets buttonInsets = new Insets(5, 5, 5, 5);
	private Insets panelInsets = new Insets(0, 0, 2, 2);
	private int defaultHeight = 350;
	
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

	public Object addButton(String name, String text) {
		JButton	btn = new JButton();
		btn.setPreferredSize(new Dimension(120, 25));
		btn.setText(text);
		btn.setName(thisID + "/" + name);
		btn.addActionListener((AwtDialogManager) manager);
		thisPanel.add(btn, new GridBagConstraints(0, iRow++, 3, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets,
				0, 0));
		return btn;
	}

	public Object addTextField(String name, String label, String value,
			String units, String defaultValue, boolean visible) {
		String key = thisKey + "_" + name;
		if (value == null) {
			value = (String) options.get(key);
			if (value == null)
				options.put(key, (value = defaultValue));
		}
		JTextField obj = new JTextField(value);
		obj.setName(thisID + "/" + name);
		if (visible) {
			obj.setPreferredSize(new Dimension(75, 25));
			obj.addActionListener((AwtDialogManager)manager);
  		addPanelLine(name, label, obj, units);
		}
		return obj;
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

	//// get/set methods ////
	
	public void setPreferredSize(int width, int height) {
		setPreferredSize(new Dimension(width, height));
	}

	public void setEnabled(Object btn, boolean b) {
		((Component) btn).setEnabled(b);
	}

	public boolean isSelected(Object chkbox) {
		return ((JCheckBox) chkbox).isSelected();
	}

	public void setSelected(Object chkbox, boolean b) {
		((JCheckBox) chkbox).setSelected(b);
	}

	public int getSelectedIndex(Object c) {
		return ((JComboBox<?>) c).getSelectedIndex();
	}

	public void setSelectedIndex(Object combo, int i) {
		((JComboBox<?>) combo).setSelectedIndex(i);
	}

	public Object getSelectedItem(Object combo) {
		return ((JComboBox<?>)combo).getSelectedItem();
	}

	
	public String getText(Object o) {
		return (o instanceof JTextComponent ? ((JTextComponent) o).getText()
				: ((AbstractButton) o).getText());	
	}
	
	public void setText(Object o, String text) {
	  if (o instanceof JTextComponent)
	  	((JTextComponent) o).setText(text);
	  else
	  	((AbstractButton) o).setText(text);
	}

	public void setIntLocation(int[] loc) {
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		loc[0] = Math.min(d.width - 50, loc[0]);
		loc[1] = Math.min(d.height - 50, loc[1]);
		Point pt = new Point(loc[0], loc[1]);
		setLocation(pt);
	}

	public void createTable(Object[][] data, String[] header, int[] widths) {
		try {
			JScrollPane scrollPane = new JScrollPane(
					dataTable = getDataTable(data, header,
							widths, (leftPanel == null ? defaultHeight  : leftPanel.getHeight() - 50)));
			if (mainSplitPane == null) {
				getContentPane().add(scrollPane, BorderLayout.CENTER);				 
			} else {
				mainSplitPane.setRightComponent(scrollPane);
			}
		} catch (Exception e) {
			// not perfect.
		}
		validate();
		repaint();
	}

  //// Table-related methods ////
	
	
	public void setCellSelectionEnabled(boolean enabled) {
		dataTable.setCellSelectionEnabled(enabled);
	}

	protected int selectedRow = -1;
	public void selectTableRow(int i) {
		selectedRow = i;
		dataTable.clearSelection();
	}

	private synchronized JTable getDataTable(Object[][] data, String[] columnNames, int[] columnWidths, int height) {
    selectedRow = -1;		
		AwtDialogTableModel tableModel = new AwtDialogTableModel(columnNames, data, !haveColors);
		JTable table = new JTable(tableModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		if (haveColors)
			table.setDefaultRenderer(JSVColor.class, new ColorRenderer());
    table.setDefaultRenderer(String.class, new TitleRenderer());
    table.setCellSelectionEnabled(true);
    ListSelectionModel selector = table.getSelectionModel();
    selector.addListSelectionListener((AwtDialogManager) manager);
    manager.registerSelector(thisID + "/ROW", selector);
    selector = table.getColumnModel().getSelectionModel();
    selector.addListSelectionListener((AwtDialogManager) manager);
    manager.registerSelector(thisID + "/COLUMN", selector);
		int n = 0;
		for (int i = 0; i < columnNames.length; i++) {
			table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
			n += columnWidths[i];
		}
		table.setPreferredScrollableViewportSize(new Dimension(n, height));
		return table;
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
			setHorizontalAlignment(tableCellAlignLeft ? SwingConstants.LEFT : column == 0 ? SwingConstants.CENTER
					: SwingConstants.RIGHT);
			setText(title.toString());
			// ignore selection model
			isSelected = (row == selectedRow);
			setBackground(isSelected ? table.getSelectionBackground() : table
					.getBackground());
			return this;
		}
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
}
