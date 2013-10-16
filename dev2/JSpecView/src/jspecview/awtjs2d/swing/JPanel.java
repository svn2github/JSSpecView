package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JPanel extends JComponent {

	private LayoutManager layoutManager;

	private Grid grid;

	public JPanel(LayoutManager manager) {
		super("JP");
		this.layoutManager = manager;
		grid = new Grid(10,10);
	}

	public void add(JComponent btn, GridBagConstraints c) {
		grid.add(btn, c);	
	}

	public void setMinimumSize(Dimension d) {
		//width = d.width;
		//height = d.height;
	}
	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("\n<div id='" + id + "' class='JPanel' style='" + getCSSstyle(100) + "'>\n");
		sb.append("\n<span id='" + id + "_minimizer' style='width:30px;height:30px;'>");
		sb.append(grid.toHTML(id));
		sb.append("</span>");
		sb.append("\n</div>\n");
		return sb.toString();
	}


}
