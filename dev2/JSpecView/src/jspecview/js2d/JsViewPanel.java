/**
 * 
 */
package jspecview.js2d;

import javajs.util.List;

import jspecview.api.JSVMainPanel;
import jspecview.api.JSVPanel;
import jspecview.api.JSVViewPanel;
import jspecview.common.PanelNode;
import jspecview.common.Annotation.AType;

public class JsViewPanel implements JSVViewPanel, JSVMainPanel {

	private static final long serialVersionUID = 1L;
	private JSVPanel selectedPanel;
	private int currentPanelIndex;
	private String title;
	private boolean visible;
	private boolean focusable;
	private boolean enabled;
	public int getCurrentPanelIndex() {
		return currentPanelIndex;
	}

	public void dispose() {
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setSelectedPanel(JSVPanel jsvp, List<PanelNode> panelNodes) {
		if (jsvp != selectedPanel) {
			selectedPanel = jsvp;
		}
		for (int i = panelNodes.size(); --i >= 0;) {
			JSVPanel j = panelNodes.get(i).jsvp;
			if (j == jsvp) {
				currentPanelIndex = i;
			} else {
				j.setEnabled(false);
				j.setFocusable(false);
				j.getPanelData().closeAllDialogsExcept(AType.NONE);
			}
		}
		markSelectedPanels(panelNodes);
		visible = (jsvp != null);
	}

	public void markSelectedPanels(List<PanelNode> panelNodes) {
		for (int i = panelNodes.size(); --i >= 0;)
			panelNodes.get(i).isSelected = (currentPanelIndex == i);
	}

	public int getHeight() {
		return (selectedPanel == null ? 0 : selectedPanel.getHeight());
	}

	public int getWidth() {
		return (selectedPanel == null ? 0 : selectedPanel.getWidth());
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isFocusable() {
		return focusable;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setEnabled(boolean b) {
		enabled = b;
	}

	public void setFocusable(boolean b) {
		focusable = b;
	}

}