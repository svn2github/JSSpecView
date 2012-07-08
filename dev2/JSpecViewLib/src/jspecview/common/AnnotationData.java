package jspecview.common;

import java.util.List;

import jspecview.common.Annotation.AType;

public interface AnnotationData {

	boolean getState();
	void setState(boolean b);
	List<Measurement> getMeasurements();
	void setMeasurements(List<Measurement> measurements);
	AType getType();
	Parameters getParameters();
	JDXSpectrum getSpectrum();
	AnnotationData getData();
	void setData(AnnotationData xyData);
	void addSpecShift(double dx);
	void setVisible(boolean b);
	void update(Coordinate clicked);
	

}
