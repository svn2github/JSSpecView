package jspecview.common;

import jspecview.common.Annotation.AType;

public interface AnnotationData {

	Parameters getParameters();
	JDXSpectrum getSpectrum();
	AType getType();

	MeasurementData getData();

	String getKey();
	void setKey(String key);

	boolean getState();
	void setState(boolean b);

	void addSpecShift(double dx);
	
}
