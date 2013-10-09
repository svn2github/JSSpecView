package jspecview.api;

import jspecview.common.Coordinate;

public abstract interface AnnotationDialog extends AnnotationData {

	// from outside world (GraphSet)
	
	void update(Coordinate clicked);
	void setFields();
	void dispose();
	void setVisible(boolean b);
	
	
	// from DialogParams
	
	void applyFromFields();
	void checkEnables();
	void createTable(String[][] data, String[] header, int[] widths);
	int[] getPosXY();
	void loadDataFromFields();
	void restoreDialogPosition(Object jsvp, int[] posXY);
	void saveDialogPosition(int[] posXY);
	void setComboSelected(int i);
	void setParamsFromFields();
	void setTableSelectionEnabled(boolean enabled);
	void setTableSelectionInterval(int i, int j);
	void setThreshold(double nan);
	void showMessage(String msg);
}
