package jspecview.api;

import jspecview.common.JSVPanelNode;

import org.jmol.util.JmolList;


public interface JSVPopupMenu {

	void dispose();

	void show(JSVPanel jsvPanel, int x, int y);

	void setSelected(String key, boolean b);

	boolean getSelected(String key);

	void setCompoundMenu(JmolList<JSVPanelNode> panelNodes,
			boolean allowCompoundMenu);

}
