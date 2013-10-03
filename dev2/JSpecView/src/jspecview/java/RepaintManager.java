package jspecview.java;

import javax.swing.JPanel;

import jspecview.api.ScriptInterface;

public class RepaintManager {

	public RepaintManager(ScriptInterface si) {
		this.si = si;
	}
  /////////// thread management ///////////
  
  boolean repaintPending;
	private ScriptInterface si;

  private int n;
  public boolean refresh() {
  	n++;
    if (repaintPending) {
    	System.out.println("Repaint " + n + " skipped");
      return false;
    }
    repaintPending = true;
    	((JPanel) si.getSelectedPanel()).repaint();
    return true;
  }

  synchronized public void repaintDone() {
    repaintPending = false;
      notify(); // to cancel any wait in requestRepaintAndWait()
  }
}
