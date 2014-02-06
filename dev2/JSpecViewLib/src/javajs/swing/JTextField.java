
package javajs.swing;

import javajs.util.SB;

/**
 * Note that in javajs.swing, JTextField extends AbstractButton
 * rather than JComponent. This is to reduce the duplication of 
 * actionListener-related business.
 * 
 * @author hansonr
 *
 */
public class JTextField extends JComponent {

	public JTextField(String value) {
		super("txtJT");
		text = value;
	}

	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("<input type=text id='" + id + "' class='JTextField' style='" + getCSSstyle(0) + "' value='"+ text + "' onkeyup	=SwingController.click(this,event)	>");
		return sb.toString();
	}

}
