package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JLabel extends JComponent {

	public JLabel(String text) {
		this.text = text;
	}

	@Override
	public String toHTML() {
		String id = registerMe("JL");
		SB sb = new SB();
		sb.append("<span id='" + id + "' class='JLabel' style='" + getCSSstyle(false) + "'>");
		sb.append(text);
		sb.append("</span>");
		return sb.toString();
	}


}
