package jspecview.api;

import javajs.util.List;
import jspecview.common.PanelNode;


public interface JSVMainPanel {

	void setSelectedPanel(JSVPanel jsvp, List<PanelNode> panelNodes);
	void markSelectedPanels(List<PanelNode> panelNodes);
	int getCurrentPanelIndex();

}
