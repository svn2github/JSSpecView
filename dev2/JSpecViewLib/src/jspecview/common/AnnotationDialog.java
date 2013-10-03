package jspecview.common;

public abstract interface AnnotationDialog extends AnnotationData {

	void setData(AnnotationData xyData);
	void setVisible(boolean b);
	void update(Coordinate clicked);
	void setFields();
	void dispose();
}
