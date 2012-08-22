package jspecview.common;


class PeakPick extends Measurement {

	PeakPick(JDXSpectrum spec, double x, double y, String text, double value) {
		super(spec, x, y, text, value);
	}

	PeakPick(JDXSpectrum spec, double x, double y) {
		super(spec, x, y, false);
	}
}
