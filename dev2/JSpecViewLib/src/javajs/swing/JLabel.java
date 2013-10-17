package javajs.swing;

import javajs.lang.StringBuffer;

public class JLabel extends JComponent {

	public JLabel(String text) {
		super("lblJL");
		this.text = text;
	}

	@Override
	public String toHTML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<span id='" + id + "' class='JLabel' style='" + getCSSstyle(0) + "'>");
		sb.append(text);
		sb.append("</span>");
		return sb.toString();
	}


}
