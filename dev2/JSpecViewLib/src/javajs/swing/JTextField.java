
package javajs.swing;

import javajs.lang.StringBuffer;

public class JTextField extends JComponent {

	public JTextField(String value) {
		super("txtJT");
		text = value;
	}

	@Override
	public String toHTML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<input type=text id='" + id + "' class='JTextField' style='" + getCSSstyle(0) + "' value='"+ text + "' onkeyup	=Jmol.Dialog.click(this,event)	>");
		return sb.toString();
	}


}
