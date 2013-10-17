package javajs.swing;

import javajs.awt.Dimension;
import javajs.lang.StringBuilder;

public class JScrollPane extends JContainer {

	public JScrollPane(JComponent component) {
		super("JScP");
		add(component);
	}

	@Override
	public String toHTML() {
		JComponent c = list.get(0);
		StringBuilder sb = new StringBuilder();
		sb.append("\n<div id='" + id + "' class='JScrollPane' style='" + getCSSstyle(98) + "overflow:auto'>\n");
		sb.append(c.toHTML());
		sb.append("\n</div>\n");
		return sb.toString();
	}

	public void setMinimumSize(Dimension dimension) {
		// TODO Auto-generated method stub
		
	}

}
