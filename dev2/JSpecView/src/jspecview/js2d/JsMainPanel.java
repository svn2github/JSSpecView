/**
 * 
 */
package jspecview.js2d;

import javajs.util.List;

import jspecview.api.JSVMainPanel;
import jspecview.api.JSVPanel;
import jspecview.common.JSViewer;
import jspecview.common.PanelNode;

public class JsMainPanel implements JSVMainPanel {

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

	public void setSelectedPanel(JSViewer viewer, JSVPanel jsvp, List<PanelNode> panelNodes) {
		if (jsvp != selectedPanel)
			selectedPanel = jsvp;
    int i = viewer.selectPanel(jsvp, panelNodes);
    if (i >= 0)
      currentPanelIndex = i;    
		visible = true;
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