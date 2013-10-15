package jspecview.api;

import jspecview.common.MeasurementData;
import jspecview.common.Parameters;
import jspecview.common.Annotation.AType;
import jspecview.source.JDXSpectrum;

public interface AnnotationData {

	void setSpecShift(double dx);

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
