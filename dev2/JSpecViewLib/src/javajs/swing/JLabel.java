package javajs.swing;

import javajs.lang.StringBuilder;

public class JLabel extends JComponent {

	public JLabel(String text) {
		super("lblJL");
		this.text = text;
	}

	@Override
	public String toHTML() {
		StringBuilder sb = new StringBuilder();
		sb.append("<span id='" + id + "' class='JLabel' style='" + getCSSstyle(0) + "'>");
		sb.append(text);
		sb.append("</span>");
		return sb.toString();
	}


}
