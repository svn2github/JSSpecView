package jspecview.common;

public abstract interface AnnotationDialog extends AnnotationData {

	// from outside world (GraphSet)
	
	void update(Coordinate clicked);
	void setFields();
	void dispose();
	void setVisible(boolean b);
	
	
	// from DialogParams
	
	int[] getPosXY();
	void restoreDialogPosition(Object jsvp, int[] posXY);
	void checkEnables();
	void applyFromFields();
	void setParamsFromFields();
	void loadDataFromFields();
	void showMessage(String msg);
	void setThreshold(double nan);
	void setComboSelected(int i);
	void createTable(String[][] data, String[] header, int[] widths);
	void setTableSelectionEnabled(boolean enabled);
	void setTableSelectionInterval(int i, int j);
}
