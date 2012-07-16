package jspecview.common;


public class PeakPick extends Measurement {

	public PeakPick(JDXSpectrum spec, double x, double y, String text, double value) {
		super(spec, x, y, text, value);
	}

	public PeakPick(JDXSpectrum spec, double x, double y) {
		super(spec, x, y, false);
	}
}
