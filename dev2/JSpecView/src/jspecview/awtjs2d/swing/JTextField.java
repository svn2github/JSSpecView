
package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JTextField extends JComponent {

	public JTextField(String value) {
		super("txtJT");
		text = value;
	}

	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("<input type=text id='" + id + "' class='JTextField' style='" + getCSSstyle(0) + "' value='"+ text + "'/>");
		return sb.toString();
	}


}
