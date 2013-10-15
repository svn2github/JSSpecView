package jspecview.awtjs2d.swing;

import org.jmol.util.ArrayUtil;
import org.jmol.util.SB;

public class Grid {

	private int nrows;
	private int ncols;

	private Cell[][] grid;
	
	
	Grid(int rows, int cols) {
		grid = new Cell[0][0];
	}

	public void add(JComponent btn, GridBagConstraints c) {
		System.out.println(c.gridx + " " + ncols + " " + c.gridy + " " + nrows);
		if (c.gridx >= ncols) {
			ncols = c.gridx + 1;
			for (int i = 0; i < nrows; i++) {
				grid[i] = (Cell[]) ArrayUtil.ensureLength(grid[i], ncols * 2);
			}
		}
		if (c.gridy >= nrows) {
			Cell[][] g = new Cell[c.gridy * 2 + 1][];
			for (int i = 0; i < nrows; i++)
				g[i] = grid[i];
			for (int i = g.length; --i >= nrows;)
				g[i] = new Cell[ncols * 2 + 1];
			grid = g;
			nrows = c.gridy + 1;
		}
		Cell cell = grid[c.gridy][c.gridx] = new Cell(btn, c);
	}

	public String toHTML(String id) {
		SB sb = new SB();
		id += "_grid";
		sb.append("\n<table id='" + id + "_grid' class='Grid' style='width:100%;height:100%'>");		
		for (int i = 0; i < nrows; i++) {
			String rowid = id + "_" + i;
			sb.append("\n<tr id='" + rowid + "' style='height:100%'>");
			for (int j = 0; j < ncols; j++)
				if (grid[i][j] != null)
					sb.append(grid[i][j].toHTML(rowid + "_" + j));
			sb.append("</tr>");
		}
		sb.append("\n</table>\n");
		return sb.toString();
	}
}
