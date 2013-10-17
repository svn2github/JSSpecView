package jspecview.api;

import javajs.util.List;
import jspecview.common.PanelNode;
import jspecview.common.JSViewer;



public interface JSVPopupMenu {

	void dispose();

	void jpiShow(int x, int y);

	void setSelected(String key, boolean b);

	boolean getSelected(String key);

	void setCompoundMenu(List<PanelNode> panelNodes,
			boolean allowCompoundMenu);

	void setEnabled(boolean allowMenu, boolean zoomEnabled);

	void initialize(JSViewer viewer, String menu);

}
