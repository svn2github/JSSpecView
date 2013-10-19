package jspecview.api;

import org.jmol.api.JmolPopupInterface;

import javajs.util.List;
import jspecview.common.PanelNode;


public interface JSVPopupMenu extends JmolPopupInterface {

	void jpiShow(int x, int y);

	void setSelected(String key, boolean b);

	boolean getSelected(String key);

	void setCompoundMenu(List<PanelNode> panelNodes,
			boolean allowCompoundMenu);

	void setEnabled(boolean allowMenu, boolean zoomEnabled);

}
