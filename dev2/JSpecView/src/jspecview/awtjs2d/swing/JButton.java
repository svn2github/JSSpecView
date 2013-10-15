package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JButton extends JComponent {

	@Override
	public String toHTML() {
		String id = registerMe("JB");
		SB sb = new SB();
		sb.append("<input type=button id='" + id + "' class='JButton' style='" + getCSSstyle(false) + "' onclick='Jmol.Dialog.click(this)' value='"+ text + "'/>");
		return sb.toString();
	}


}
