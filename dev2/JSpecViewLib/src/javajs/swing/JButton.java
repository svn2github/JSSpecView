package javajs.swing;

import javajs.lang.StringBuilder;

public class JButton extends JComponent {

  public JButton() {
    super("btnJB");
  }
	@Override
	public String toHTML() {
		StringBuilder sb = new StringBuilder();
		sb.append("<input type=button id='" + id + "' class='JButton' style='" + getCSSstyle(0) + "' onclick='Jmol.Dialog.click(this)' value='"+ text + "'/>");
		return sb.toString();
	}


}
