package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JScrollPane extends JComponent {

	private JComponent component;

	public JScrollPane(JComponent component) {
		super("JScP");
		this.component = component;
	}

	@Override
	public String toHTML() {
		if (component.width != 0)
			width = component.width;
		if (component.height != 0)
			height = component.height;
		SB sb = new SB();
		sb.append("\n<div id='" + id + "' class='JScrollPane' style='" + getCSSstyle(98) + "overflow:auto'>\n");
		sb.append(component.toHTML());
		sb.append("\n</div>\n");
		return sb.toString();
	}

}
