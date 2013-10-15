package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JSplitPane extends JComponent {

	public static final int HORIZONTAL_SPLIT = 1;
	private int split;
	private JComponent right;
	private JComponent left;

	public JSplitPane(int split) {
		this.split = split;
	}

	public void setRightComponent(JComponent r) {
		this.right = r;
	}

	public void setLeftComponent(JComponent l) {
		this.left = l;
	}

	@Override
	public String toHTML() {
		String id = registerMe("JSpP");
		SB sb = new SB();
		boolean isH = (split == HORIZONTAL_SPLIT); 
		sb.append("<div id='" + id + "' class='JSplitPane' style='" + getCSSstyle(true) + "'><table border=1 style='width:100%;height:100%'>"
				+"<tr><td style='width:"+(isH?"50%" : "100%")+"'>");
		sb.append(left.toHTML());
		sb.append(isH ? "</td><td style='width:50%'>" : "</td></tr><tr><td style='width:100%'>");
		sb.append(right.toHTML());
		sb.append("</td></tr></table>\n</div>\n");
		return sb.toString();
	}


}
