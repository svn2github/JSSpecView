/**
 * 
 */
package jspecview.common;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;

import jspecview.common.JSVContainer;

public class JSVSpectrumPanel extends JPanel implements JSVContainer {

	private static final long serialVersionUID = 1L;
	private JSVPanel selectedPanel;
	private int currentPanelIndex;
	public int getCurrentSpectrumIndex() {
		return currentPanelIndex;
	}

	public JSVSpectrumPanel(BorderLayout borderLayout) {
		super(borderLayout);
	}

	public void dispose() {
	}

	public String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setTitle(String title) {
		// TODO Auto-generated method stub

	}

	public void setSelectedPanel(JSVPanel jsvp, List<JSVSpecNode> specNodes) {
		if (jsvp != null && jsvp != selectedPanel) {
			if (selectedPanel != null) {
				remove((AwtPanel) selectedPanel);
				//jsvApplet.removeKeyListener((AwtPanel) selectedPanel);
			}
			add((AwtPanel) jsvp, BorderLayout.CENTER);
			//jsvApplet.addKeyListener((AwtPanel) jsvp);
			selectedPanel = jsvp;
		}
		for (int i = specNodes.size(); --i >= 0;)
			if (specNodes.get(i).jsvp == jsvp) {
				currentPanelIndex = i;
				jsvp.setEnabled(true);
			} else {
				specNodes.get(i).jsvp.setEnabled(false);
			}
		markSelectedPanels(specNodes);
	}

	void markSelectedPanels(List<JSVSpecNode> specNodes) {
		for (int i = specNodes.size(); --i >= 0;)
			specNodes.get(i).isSelected = (currentPanelIndex == i);
	}

}