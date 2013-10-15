
package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JTextField extends JComponent {

	public JTextField(String value) {
		text = value;
	}

	@Override
	public String toHTML() {
		String id = registerMe("JTF");
		SB sb = new SB();
		sb.append("<input type=text id='" + id + "' class='JTextField' style='" + getCSSstyle(false) + "' value='"+ text + "'/>");
		return sb.toString();
	}


}
