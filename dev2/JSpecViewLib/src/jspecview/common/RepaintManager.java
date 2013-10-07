package jspecview.common;

public class RepaintManager {

	public RepaintManager(JSViewer viewer) {
		this.viewer = viewer;
	}
  /////////// thread management ///////////
  
  boolean repaintPending;
	private JSViewer viewer;

  private int n;
  public boolean refresh() {
  	n++;
    if (repaintPending) {
    	System.out.println("Repaint " + n + " skipped");
      return false;
    }
    repaintPending = true;
    	viewer.selectedPanel.repaint();
    return true;
  }

  synchronized public void repaintDone() {
    repaintPending = false;
      notify(); // to cancel any wait in requestRepaintAndWait()
  }
}
