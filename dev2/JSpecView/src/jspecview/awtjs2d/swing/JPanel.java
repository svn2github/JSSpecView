package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JPanel extends JComponent {

	private LayoutManager layoutManager;

	private Grid grid;

	private int minWidth = 30;
	private int minHeight = 30;

	public JPanel(LayoutManager manager) {
		super("JP");
		this.layoutManager = manager;
		grid = new Grid(10,10);
	}

	public void add(JComponent btn, GridBagConstraints c) {
		grid.add(btn, c);	
	}

	public void setMinimumSize(Dimension d) {
		minWidth = d.width;
		minHeight = d.height;
	}
	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("\n<div id='" + id + "' class='JPanel' style='" + getCSSstyle(100) + "'>\n");
		sb.append("\n<span id='" + id + "_minimizer' style='width:"+minWidth+"px;height:"+minHeight+"px;'>");
		sb.append(grid.toHTML(id));
		sb.append("</span>");
		sb.append("\n</div>\n");
		return sb.toString();
	}


}
