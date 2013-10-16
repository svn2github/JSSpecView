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

	public void toHTML(SB sb, String id) {
		if (data == null || data[0].length == 0)
			return;
		int nrows = data.length;
		int ncols = data[0].length;
		for (int j = 0; j < ncols; j++) 
			getCellHtml(sb, id + "_h" + j, -1, j, columnNames[j]);		
		for (int i = 0; i < nrows; i++) {
			String rowid = id + "_" + i;
			sb.append("\n<tr id='" + rowid + "'>");
			for (int j = 0; j < ncols; j++) 
				getCellHtml(sb, rowid + "_" + j, i, j, data[i][j]);
			sb.append("</tr>");
		}
	}
	
	private void getCellHtml(SB sb, String id, int iRow, int iCol, Object o) {
		String style = getCellStyle(id, iRow, iCol, o);
		sb.append("<td cellpadding=0 cellspacing=0 id='" + id + "'" + style
				+ " onclick=Jmol.Dialog.click(this)>" + o + "</td>");
	}

	/**
	 * @param id  
	 * @param iRow 
	 * @param iCol 
	 * @param o 
	 * @return CSS style attribute
	 */
	private String getCellStyle(String id, int iRow, int iCol, Object o) {
		String style;
		if (iRow < 0) {
			style = ";font-weight:bold;";
		} else if (o instanceof JSVColor) {
			style = ";background-color:"
					+ JSVColorUtil.colorToCssString((JSVColor) o);
		} else {
			if (asString)
				o = " " + o + " ";
			style = "text-align:";
			if (tableCellAlignLeft)
				style += "left";
			else if (iCol == 0)
				style += "center";
			else
				style += "right";
		}
		return " style='" + style + "'";
	}

}
