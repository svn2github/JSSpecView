package jspecview.awt;

import javax.swing.table.AbstractTableModel;

/**
 * The Table Model for Legend
 */
class AwtDialogTableModel extends AbstractTableModel {
	/**
   * 
   */
	private static final long serialVersionUID = 1L;
	String[] columnNames;
	Object[][] data;
	boolean asString;

	public AwtDialogTableModel(String[] columnNames, Object[][] data, boolean asString) {
		this.columnNames = columnNames;
		this.data = data;
		this.asString = asString;
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
		Object o = data[row][col];
		return (asString ? " " + o + " " : o);
	}
	
  @Override
  public Class<?> getColumnClass(int c) {
      return getValueAt(0, c).getClass();
  }

}