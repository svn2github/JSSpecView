package jspecview.common;

import jspecview.common.Annotation.AType;

public interface AnnotationData {

	boolean isVisible();

	Parameters getParameters();
	JDXSpectrum getSpectrum();
	AType getAType();

	MeasurementData getData();

	String getKey();
	void setKey(String key);

	boolean getState();
	void setState(boolean b);

	void addSpecShift(double dx);
	
}
