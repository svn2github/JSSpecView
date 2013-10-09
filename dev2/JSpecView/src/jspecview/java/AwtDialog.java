package jspecview.java;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.JDialog;

import jspecview.api.JSVDialog;
import jspecview.common.DialogParams;

abstract public class AwtDialog extends JDialog implements JSVDialog {

	private static final long serialVersionUID = 1L;
	
	protected DialogParams params;

	private int[] loc;

  public AwtDialog(Frame frame, String title, boolean isModal) {
  	super(frame, title, isModal);
	}

	/**
	 * 
	 * @param opanel
	 *          a JPanel (Applet) or a JScrollPane (MainFrame)
	 * 
	 * @param posXY
	 *          static for a given dialog
	 */
	public void restoreDialogPosition(Object opanel, int[] posXY) {
		Component panel = (Component) opanel;
		if (panel != null) {
			if (posXY[0] == Integer.MIN_VALUE) {
				posXY[0] = 0;
				posXY[1] = -20;
			}
			Point pt = panel.getLocationOnScreen();
			int height = panel.getHeight();
			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			loc = new int[] { Math.min(d.width - 50, Math.max(0, pt.x + posXY[0])),
					Math.min(d.height - 50, Math.max(0, pt.y + height + posXY[1])) };
			setLocation(loc[0], loc[1]);
		}
	}
	
	public void saveDialogPosition(int[] posXY) {
		try {
			Point pt = getLocationOnScreen();
			posXY[0] += pt.x - loc[0];
			posXY[1] += pt.y - loc[1];
		} catch (Exception e) {
			// ignore
		}
	}
}
