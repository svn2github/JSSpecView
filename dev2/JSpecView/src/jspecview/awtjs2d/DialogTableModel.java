package jspecview.awtjs2d;

import org.jmol.util.SB;

import jspecview.awtjs2d.swing.AbstractTableModel;
import jspecview.awtjs2d.swing.TableColumn;
import jspecview.util.JSVColor;
import jspecview.util.JSVColorUtil;


class DialogTableModel implements AbstractTableModel {

	private static final long serialVersionUID = 1L;
	String[] columnNames;
	Object[][] data;
	boolean asString;
	int[] widths;
	private int thisCol;

	DialogTableModel(String[] columnNames, Object[][] data, boolean asString) {
		this.columnNames = columnNames;
		this.data = data;
		this.asString = asString;
		this.widths = (data.length  == 0 ? new int[0] : new int[data[0].length]);
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

	public boolean isAsString() {
		return asString;
	}

	public void toHTML(SB sb, String id) {
		if (data == null || data[0].length == 0)
			return;
		int nrows = data.length;
		int ncols = data[0].length;
		for (int i = 0; i < nrows; i++) {
			String rowid = id + "_" + i;
			sb.append("\n<tr id='" + rowid + "'>");
			for (int j = 0; j < ncols; j++) 
				getCellHtml(sb, rowid + "_" + j, data[i][j]);
			sb.append("</tr>");
		}
	}

	private void getCellHtml(SB sb, String id, Object o) {
		String style = (o instanceof JSVColor ? ";background-color:" + JSVColorUtil.colorToCssString((JSVColor) o) : "");			
		String s = (style.length() > 0 ? "" : asString ? " " + o + " " : o.toString());
		if (style.length() > 0)
			style = " style='" + style + "'";
	  sb.append("<td id='" + id + "'" + style + " onclick=Jmol.Dialog.click(this)>" + s + "</td>");
	}

}
