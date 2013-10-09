package jspecview.api;

import jspecview.common.JSVPanelNode;
import jspecview.common.JSViewer;

import org.jmol.util.JmolList;


public interface JSVPopupMenu {

	void dispose();

	void jpiShow(int x, int y);

	void setSelected(String key, boolean b);

	boolean getSelected(String key);

	void setCompoundMenu(JmolList<JSVPanelNode> panelNodes,
			boolean allowCompoundMenu);

	void setEnabled(boolean allowMenu, boolean zoomEnabled);

	void initialize(JSViewer viewer, String menu);

}
