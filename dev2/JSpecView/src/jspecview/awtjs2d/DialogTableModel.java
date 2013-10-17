package jspecview.awtjs2d;


import javajs.api.GenericColor;
import javajs.lang.StringBuffer;
import javajs.swing.AbstractTableModel;
import javajs.swing.TableColumn;
import javajs.util.BitSet;
import jspecview.util.JSVColorUtil;


class DialogTableModel implements AbstractTableModel {

	private static final long serialVersionUID = 1L;
	String[] columnNames;
	Object[][] data;
	boolean asString;
	int[] widths;
	private int thisCol;
	private boolean tableCellAlignLeft;

	DialogTableModel(String[] columnNames, Object[][] data, boolean asString, boolean tableCellAlignLeft ) {
		this.columnNames = columnNames;
		this.data = data;
		this.asString = asString;
		this.widths = (data.length  == 0 ? new int[0] : new int[data[0].length]);
		this.tableCellAlignLeft = tableCellAlignLeft;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return data.length;
	}
	
	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		Object o = data[row][col];
		return (asString ? " " + o + " " : o);
	}

	public TableColumn getColumn(int i) {
		thisCol = i;
		return this;		
	}
	
	public void setPreferredWidth(int n) {
		widths[thisCol] = n;
	}

	public void toHTML(StringBuffer sb, String id, BitSet selectedRows) {
		if (data == null || data[0].length == 0)
			return;
		int nrows = data.length;
		int ncols = data[0].length;
		for (int j = 0; j < ncols; j++) 
			getCellHtml(sb, id + "_h" + j, -1, j, columnNames[j], false);		
		for (int i = 0; i < nrows; i++) {
			String rowid = id + "_" + i;
			sb.append("\n<tr id='" + rowid + "'>");
			for (int j = 0; j < ncols; j++) 
				getCellHtml(sb, rowid + "_" + j, i, j, data[i][j], selectedRows.get(i));
			sb.append("</tr>");
		}
	}
	
	private void getCellHtml(StringBuffer sb, String id, int iRow, int iCol, Object o, boolean isSelected) {
		String style = getCellStyle(id, iRow, iCol, o, isSelected);
		sb.append("<td id='" + id + "'" + style
				+ " onclick=Jmol.Dialog.click(this)>" + o + "</td>");
	}

	/**
	 * @param id
	 * @param iRow
	 * @param iCol
	 * @param o
	 * @return CSS style attribute
	 */
	private String getCellStyle(String id, int iRow, int iCol, Object o, boolean isSelected) {
		String style = ";padding:1px 1px 1px 1px;";
		if (iRow < 0) {
			style += ";font-weight:bold;";
		} else {
			if (o instanceof GenericColor) {
				style += "background-color:"
						+ JSVColorUtil.colorToCssString((GenericColor) o);
			} else {
				if (asString)
					o = " " + o + " ";
				style += "text-align:";
				if (tableCellAlignLeft)
					style += "left";
				else if (iCol == 0)
					style += "center";
				else
					style += "right";
				style += ";border:" + (isSelected ? 3 : 1) + "px solid #000";
			}
		}
		return " style='" + style + "'";
	}

}
