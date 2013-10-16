package jspecview.awtjs2d.swing;

import org.jmol.util.JmolList;

abstract public class JContainer extends JComponent {

	protected JmolList<JComponent> list;
	
	public JContainer(String type) {
		super(type);
		list = new JmolList<JComponent>();
	}
	public void removeAll() {
		list.clear();
	}

	public void add(JComponent component) {
		list.addLast(component);
	}



}
