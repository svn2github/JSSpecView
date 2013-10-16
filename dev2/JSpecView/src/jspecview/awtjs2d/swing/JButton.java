package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JButton extends JComponent {

  public JButton() {
    super("btnJB");
  }
	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("<input type=button id='" + id + "' class='JButton' style='" + getCSSstyle(0) + "' onclick='Jmol.Dialog.click(this)' value='"+ text + "'/>");
		return sb.toString();
	}


}
