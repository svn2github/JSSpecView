package jspecview.dialog;

import jspecview.api.JSVPanel;
import jspecview.common.Annotation;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSViewer;

abstract public class GenericDialog extends Annotation {

	protected DialogManager manager;
	protected DialogParams dialogParams;
	protected PlatformDialog dialog;
	private int[] loc;
	protected JSViewer viewer;
	protected JDXSpectrum spec;
	protected AType type;
	protected String title;

	public GenericDialog setParams(String title, JSViewer viewer, JDXSpectrum spec) {
		this.title = title;
		this.viewer = viewer;
		this.spec = spec;
		manager = viewer.getDialogManager();
		return this;
	}

	abstract public boolean callback(String id, String msg);

	/**
	 * 
	 * @param panel
	 *          a PPanel (Applet) or a JScrollPane (MainFrame)
	 * 
	 * @param posXY
	 *          static for a given dialog
	 */
	public void restoreDialogPosition(JSVPanel panel, int[] posXY) {
		if (panel != null) {
			if (posXY[0] == Integer.MIN_VALUE) {
				posXY[0] = 0;
				posXY[1] = -20;
			}
			int[] pt = manager.getLocationOnScreen(panel);
			int height = panel.getHeight();
			loc = new int[] { Math.max(0, pt[0] + posXY[0]), Math.max(0, pt[1] + height + posXY[1]) };
			dialog.setIntLocation(loc);
		}
	}
	
	public void saveDialogPosition(int[] posXY) {
		try {
			int[] pt = manager.getLocationOnScreen(dialog);
			posXY[0] += pt[0] - loc[0];
			posXY[1] += pt[1] - loc[1];
		} catch (Exception e) {
			// ignore
		}
	}
	
	/**
	 * @param id 
	 * @param msg  
	 * @return true if consumed
	 */
	
	public boolean callbackGD(String id, String msg) {
		if (id.equals("windowClosing")) {
			dialogParams.done();
			return true;
		}
		return false;
	}

	public void dispose() {
		dialog.dispose();		
	}

	public void setVisible(boolean visible) {
		dialog.setVisible(visible);		
	}

	public boolean isVisible() {
		return dialog.isVisible();
	}


}
