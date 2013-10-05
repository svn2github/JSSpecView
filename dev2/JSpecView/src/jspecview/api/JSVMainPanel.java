package jspecview.api;

import jspecview.common.JSVPanel;
import jspecview.common.JSVPanelNode;

import org.jmol.util.JmolList;

public interface JSVMainPanel {

	void setSelectedPanel(JSVPanel jsvp, JmolList<JSVPanelNode> panelNodes);
	void markSelectedPanels(JmolList<JSVPanelNode> panelNodes);
	int getCurrentPanelIndex();

}
