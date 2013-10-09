package jspecview.java;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import jspecview.api.JSVPanel;

/**
 * just a class I made to separate the construction of the AnnotationDialogs
 * from their use
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class AwtDialogHelper {

	private String thisKey;
	private ActionListener eventListener;
	private Map<String, Object> options;
	private JPanel leftPanel;
	private Insets buttonInsets = new Insets(5, 5, 5, 5);
	private Insets cbInsets = new Insets(0, 0, 2, 2);
	private int iRow;

	public AwtDialogHelper set(String thisKey, Map<String, Object> options,
			JPanel leftPanel, ActionListener eventListener) {
		this.thisKey = thisKey;
		if (options == null)
			options = new Hashtable<String, Object>();
		this.options = options;
		this.leftPanel = leftPanel;
		this.eventListener = eventListener;
		return this;
	}

	protected JButton addButton(String text, ActionListener a) {
		JButton	btn = new JButton();
		btn.setPreferredSize(new Dimension(120, 25));
		btn.setText(text);
		btn.addActionListener(a);
		leftPanel.add(btn, new GridBagConstraints(0, iRow++, 3, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets,
				0, 0));
		return btn;
	}
//
//	protected JCheckBox addCheckBoxOption(String name, String label,
//			boolean isSelected) {
//		JCheckBox obj = new JCheckBox();
//		obj.setText(label == null ? name : label);
//		obj.addActionListener(eventListener);
//		leftPanel.add(obj, new GridBagConstraints(1, iRow, 2, 1, 0.0, 0.0,
//				GridBagConstraints.WEST, GridBagConstraints.NONE, cbInsets, 0, 0));
//		iRow++;
//		return obj;
//	}

	protected JTextField addInputOption(String name, String label, String value,
			String units, String defaultValue, boolean visible) {
		String key = thisKey + "_" + name;
		if (value == null) {
			value = (String) options.get(key);
			if (value == null)
				options.put(key, (value = defaultValue));
		}
		JTextField obj = new JTextField(value);
		if (visible) {
			obj.setPreferredSize(new Dimension(75, 25));
			obj.addActionListener(eventListener);
  		addPanelLine(name, label, obj, units);
		}
		return obj;
	}

	protected JComboBox<String> addSelectOption(String name, String label, String[] info,
			int iPt, boolean visible) {
		JComboBox<String> obj = new JComboBox<String>(info);
		obj.setSelectedIndex(iPt);
		if (visible) {
			obj.setActionCommand(name);
			// obj.setPreferredSize(new Dimension(100, 25));
			obj.addActionListener(eventListener);
			addPanelLine(name, label, obj, null);
		}
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

	protected synchronized JTable getDataTable(AwtDialogAnnotation ad,
			String[][] data, String[] columnNames, int[] columnWidths, int height) {
		
		AwtDialogTableModel tableModel = new AwtDialogTableModel(columnNames, data, true);
		JTable table = new JTable(tableModel);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setDefaultRenderer(String.class, new TitleRenderer());
    table.setCellSelectionEnabled(true);
    table.getSelectionModel().addListSelectionListener(ad);
    ad.columnSelector = table.getColumnModel().getSelectionModel();
    ad.columnSelector.addListSelectionListener(ad);
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
          setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.RIGHT);
          setText(title.toString());
          if(isSelected)
            setBackground(table.getSelectionBackground());
          else
            setBackground(table.getBackground());

          return this;
      }
  }
	public int geOptionFromDialog(Object frame, String[] items,
			JSVPanel jsvp, String dialogName, String labelName) {
		final JDialog dialog = new JDialog((JFrame) frame, dialogName, true);
		dialog.setResizable(false);
		dialog.setSize(200, 100);
		Component panel = (Component) jsvp;
		dialog.setLocation((panel.getLocation().x + panel.getSize().width) / 2,
				(panel.getLocation().y + panel.getSize().height) / 2);
		final JComboBox<Object> cb = new JComboBox<Object>(items);
		Dimension d = new Dimension(120, 25);
		cb.setPreferredSize(d);
		cb.setMaximumSize(d);
		cb.setMinimumSize(d);
		JPanel p = new JPanel(new FlowLayout());
		JButton button = new JButton("OK");
		p.add(cb);
		p.add(button);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(
				new JLabel(labelName, SwingConstants.CENTER),
				BorderLayout.NORTH);
		dialog.getContentPane().add(p);
		final int ret[] = new int[] { Integer.MIN_VALUE };
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ret[0] = cb.getSelectedIndex();
				dialog.dispose();
			}
		});
		dialog.setVisible(true);
		dialog.dispose();
		return ret[0];
	}


}
