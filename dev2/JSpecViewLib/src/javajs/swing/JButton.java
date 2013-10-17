package javajs.swing;

import javajs.lang.StringBuffer;

public class JButton extends JComponent {

  public JButton() {
    super("btnJB");
  }
	@Override
	public String toHTML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<input type=button id='" + id + "' class='JButton' style='" + getCSSstyle(0) + "' onclick='Jmol.Dialog.click(this)' value='"+ text + "'/>");
		return sb.toString();
	}


}
