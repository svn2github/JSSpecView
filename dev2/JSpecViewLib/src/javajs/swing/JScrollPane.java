package javajs.swing;

import javajs.lang.StringBuffer;

public class JScrollPane extends JContainer {

	public JScrollPane(JComponent component) {
		super("JScP");
		add(component);
	}

	@Override
	public String toHTML() {
		JComponent c = list.get(0);
		StringBuffer sb = new StringBuffer();
		sb.append("\n<div id='" + id + "' class='JScrollPane' style='" + getCSSstyle(98) + "overflow:auto'>\n");
		sb.append(c.toHTML());
		sb.append("\n</div>\n");
		return sb.toString();
	}

	public void setMinimumSize(Dimension dimension) {
		// TODO Auto-generated method stub
		
	}

}
