package jspecview.java;

import java.awt.Component;
import java.awt.Frame;

import javax.swing.JDialog;

import jspecview.api.JSVDialog;
import jspecview.common.DialogParams;

abstract public class AwtDialog extends JDialog implements JSVDialog {

	private static final long serialVersionUID = 1L;
	
	protected DialogParams params;
	
  public AwtDialog(Frame frame, String title, boolean isModal) {
  	super(frame, title, isModal);
	}

  /**
   *   
   * @param opanel
   * 							a JPanel (Applet) or a JScrollPane (MainFrame)
   * 
   * @param posXY
   * 							static for a given dialog
   */
	public void restoreDialogPosition(Object opanel, int[] posXY) {
		Component panel = (Component) opanel;
		if (panel != null) {
			if (posXY[0] == Integer.MIN_VALUE) {
				posXY[0] = panel.getLocationOnScreen().x;
				posXY[1] = panel.getLocationOnScreen().y + panel.getHeight() - 20;
			}
			setLocation(posXY[0], posXY[1]);
		}
	}
}
