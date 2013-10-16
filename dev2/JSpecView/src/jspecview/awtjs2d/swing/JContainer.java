package jspecview.awtjs2d.swing;

import org.jmol.util.JmolList;

public class JContainer extends JComponent {

	protected JmolList<JComponent> list;
	
	public JContainer(String type) {
		super(type);
		list = new JmolList<JComponent>();
	}
	public void removeAll() {
		list.clear();
	}

	public JComponent add(JComponent component) {
		list.addLast(component);
		return component;
	}

	@Override
	protected int getSubcomponentWidth() {
		return (list.size() == 1 ? list.get(0).getSubcomponentWidth() : 0);
	}
	
	@Override
	protected int getSubcomponentHeight() {
		return (list.size() == 1 ? list.get(0).getSubcomponentHeight() : 0);
	}
	@Override
	public String toHTML() {
		return null;
	}


}
