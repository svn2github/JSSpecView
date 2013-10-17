package javajs.swing;

import javajs.lang.StringBuffer;

public class JContentPane extends JContainer {

	public JContentPane() {
		super("JCP");
	}

	@Override
	public String toHTML() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n<div id='" + id + "' class='JContentPane' style='" + getCSSstyle(100) + "'>\n");		
		for (int i = 0; i < list.size(); i++)
			sb.append(list.get(i).toHTML());
		sb.append("\n</div>\n");
		return sb.toString();
	}

}