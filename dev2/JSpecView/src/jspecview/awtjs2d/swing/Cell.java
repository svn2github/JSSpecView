package jspecview.awtjs2d.swing;

public class Cell {

	private JComponent component;
	private int colspan;
	private int rowspan;
	int textAlign;
	private GridBagConstraints c;

	public Cell(JComponent btn, GridBagConstraints c) {
		this.component = btn;
		colspan = c.gridwidth;
		rowspan = c.gridheight;  // ignoring for now
		this.c = c;
	}

	public String toHTML(String id) {
		String style = c.insets.getStyle(c.ipadx, c.ipady);
		return "<td><span id='" + id +"' " + (colspan < 2 ? "" : " colspan='" + colspan + "' style='") 
			+ style + "'>" + component.toHTML() + "</span></td>";
	}

}
