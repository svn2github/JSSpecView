package jspecview.common;


class PeakPick extends Measurement {

	PeakPick(double x, double y) {
		super(x, y);
	} 
	
	public PeakPick setValue(JDXSpectrum spec, String text, double value) {
		// peak picking
		setAll(spec, text, false, false, 0, 6);
		this.value = value;
		setPt2(getXVal(), getYVal());
		return this;
	}

	PeakPick setNoValue(JDXSpectrum spec) {
		setPt2(spec, false);
		return this;
	}
}
