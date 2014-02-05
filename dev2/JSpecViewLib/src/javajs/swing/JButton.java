package javajs.swing;

import javajs.api.SC;
import javajs.util.SB;

public class JButton extends AbstractButton {

  public JButton() {
    super("btnJB");
  }
	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("<input type=button id='" + id + "' class='JButton' style='" + getCSSstyle(0) + "' onclick='SwingController.click(this)' value='"+ text + "'/>");
		return sb.toString();
	}
}
