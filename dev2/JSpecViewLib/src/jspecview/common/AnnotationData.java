package jspecview.common;

import jspecview.common.Annotation.AType;

public interface AnnotationData {

	void addSpecShift(double dx);

	AType getAType();
	MeasurementData getData();
	String getKey();
	Parameters getParameters();
	JDXSpectrum getSpectrum();
	boolean getState();

	boolean isVisible();

	void setKey(String key);
	void setState(boolean b);

}
