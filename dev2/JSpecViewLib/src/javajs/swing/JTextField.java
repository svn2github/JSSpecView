
package javajs.swing;

import javajs.lang.SB;

public class JTextField extends JComponent {

	public JTextField(String value) {
		super("txtJT");
		text = value;
	}

	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("<input type=text id='" + id + "' class='JTextField' style='" + getCSSstyle(0) + "' value='"+ text + "' onkeyup	=Jmol.Dialog.click(this,event)	>");
		return sb.toString();
	}


}
