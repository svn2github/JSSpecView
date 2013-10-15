package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JContentPane extends JContainer {

	@Override
	public String toHTML() {
		String id = registerMe("JCP");
		SB sb = new SB();
		sb.append("\n<div id='" + id + "' class='JContentPane' style='" + getCSSstyle(true) + "'>\n");		
		for (int i = 0; i < list.size(); i++)
			sb.append(list.get(i).toHTML());
		sb.append("\n</div>\n");
		return sb.toString();
	}

}
