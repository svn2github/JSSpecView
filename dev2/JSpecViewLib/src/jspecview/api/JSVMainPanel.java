package jspecview.api;

import jspecview.common.PanelNode;

import org.jmol.util.JmolList;

public interface JSVMainPanel {

	void setSelectedPanel(JSVPanel jsvp, JmolList<PanelNode> panelNodes);
	void markSelectedPanels(JmolList<PanelNode> panelNodes);
	int getCurrentPanelIndex();

}
