
package javajs.swing;

import javajs.lang.StringBuilder;

public class JTextField extends JComponent {

	public JTextField(String value) {
		super("txtJT");
		text = value;
	}

	@Override
	public String toHTML() {
		StringBuilder sb = new StringBuilder();
		sb.append("<input type=text id='" + id + "' class='JTextField' style='" + getCSSstyle(0) + "' value='"+ text + "' onkeyup	=Jmol.Dialog.click(this,event)	>");
		return sb.toString();
	}


}
