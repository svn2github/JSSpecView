package javajs.swing;

import javajs.awt.Dimension;
import javajs.lang.StringBuffer;
import javajs.util.BitSet;
import jspecview.dialog.DialogManager;


public class JTable extends JComponent implements ListSelectionModel, ColumnSelectionModel {

	private AbstractTableModel tableModel;
	private BitSet bsSelectedCells;
	private BitSet bsSelectedRows;
	private boolean rowSelectionAllowed;
	private boolean cellSelectionEnabled;

	public JTable(AbstractTableModel tableModel) {
		super("JT");
		this.tableModel = tableModel;
		this.bsSelectedCells = new BitSet();
		this.bsSelectedRows = new BitSet();
	}

	public ListSelectionModel getSelectionModel() {
		return this;
	}

	public ColumnSelectionModel getColumnModel() {
		return this;
	}

	public void setPreferredScrollableViewportSize(Dimension dimension) {
		this.width = dimension.width;
		this.height = dimension.height;
	}

	public void clearSelection() {
		bsSelectedCells.clearAll();
		bsSelectedRows.clearAll();
	}

	public void setRowSelectionAllowed(boolean b) {
		rowSelectionAllowed = b;
	}

	public void setRowSelectionInterval(int i, int j) {
		bsSelectedRows.clearAll();
		bsSelectedRows.setBits(i, j);
		bsSelectedCells.clearAll();
	}

	public void setCellSelectionEnabled(boolean enabled) {
		cellSelectionEnabled = enabled;
	}

	public void addListSelectionListener(DialogManager manager) {
		dialogManager = manager;
	}

	public TableColumn getColumn(int i) {
		return tableModel.getColumn(i);
	}

	@Override
	public String toHTML() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n<table id='" + id + "_table' class='JTable' style='width:100%;height:100%'>");
		tableModel.toHTML(sb, id, bsSelectedRows);
		sb.append("\n</table>\n");
		return sb.toString();
	}
}
