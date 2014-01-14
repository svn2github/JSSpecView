/**
 * 
 */
package jspecview.java;

import java.awt.BorderLayout;


import javajs.util.List;

import javax.swing.JPanel;


import jspecview.api.JSVMainPanel;
import jspecview.api.JSVPanel;
import jspecview.common.JSViewer;
import jspecview.common.PanelNode;

public class AwtMainPanel extends JPanel implements JSVMainPanel {

	private static final long serialVersionUID = 1L;
	private JSVPanel selectedPanel;
	private int currentPanelIndex;
	public int getCurrentPanelIndex() {
		return currentPanelIndex;
	}

	public AwtMainPanel(BorderLayout borderLayout) {
		super(borderLayout);
	}

	public void dispose() {
	}

	public String getTitle() {
		return null;
	}

	public void setTitle(String title) {
	}

	public void setSelectedPanel(JSViewer viewer, JSVPanel jsvp, List<PanelNode> panelNodes) {
		if (jsvp != selectedPanel) {
			if (selectedPanel != null)
				remove((AwtPanel) selectedPanel);
			if (jsvp != null)
				add((AwtPanel) jsvp, BorderLayout.CENTER);
			selectedPanel = jsvp;
		}
		int i = viewer.selectPanel(jsvp, panelNodes);
		if (i >= 0)
			currentPanelIndex = i;		
		setVisible(true);
	}


}