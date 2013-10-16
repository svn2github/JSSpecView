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
		sb.append("<div id='" + id + "' class='JSplitPane' style='" + getCSSstyle(true) + "'>");
		if (isH) 
			sb.append("<div id='" + id + "_left' style='width:50%;height:100%;position:absolute;top:0%;left:0%'>");
		else
			sb.append("<div id='" + id + "_top' style='width:100%;height:50%;position:absolute;top:0%;left:0%'>");
		sb.append(left.toHTML());
		if (isH) 
			sb.append("</div><div id='" + id + "_right' style='width:50%;height:100%;position:absolute;top:0%;left:50%'>");
		else
			sb.append("</div><div id='" + id + "_bottom' style='width:100%;height:50%;position:absolute;top:50%;left:0%'>");
		sb.append(right.toHTML());
		sb.append("</div></div>\n");
		return sb.toString();
	}


}
