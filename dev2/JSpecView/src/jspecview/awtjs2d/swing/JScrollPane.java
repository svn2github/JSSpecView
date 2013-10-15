package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JScrollPane extends JComponent {

	private JComponent component;

	public JScrollPane(JComponent component) {
		this.component = component;
	}

	@Override
	public String toHTML() {
		String id = registerMe("JScP");
		SB sb = new SB();
		sb.append("\n<div id='" + id + "' class='JScrollPane' style='" + getCSSstyle(true) + ";background-color:green;overflow:auto'>\n");
		sb.append(component.toHTML());
		sb.append("\n</div>\n");
		return sb.toString();
	}

}
