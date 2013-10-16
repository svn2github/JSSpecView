package jspecview.awtjs2d.swing;

import jspecview.dialog.DialogManager;

import org.jmol.util.BS;
import org.jmol.util.SB;

public class JTable extends JComponent implements ListSelectionModel, ColumnSelectionModel {

	private AbstractTableModel tableModel;
	private BS bsSelectedCells;
	private BS bsSelectedRows;
	private boolean rowSelectionAllowed;
	private boolean cellSelectionEnabled;

	public JTable(AbstractTableModel tableModel) {
		this.tableModel = tableModel;
		this.bsSelectedCells = new BS();
		this.bsSelectedRows = new BS();
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
		SB sb = new SB();
		String id = registerMe("JT");
		sb.append("\n<table id='" + id + "_table' class='JTable' style='width:100%;height:100%'>");
		tableModel.toHTML(sb, id);
		sb.append("\n</table>\n");
		return sb.toString();
	}
}
