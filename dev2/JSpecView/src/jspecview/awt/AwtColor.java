package jspecview.awt;

import java.awt.Color;

import jspecview.util.JSVColor;
import jspecview.util.JSVColorUtil;

public class AwtColor extends Color implements JSVColor {

	public JSVColor get4(int r, int g, int b, int a) {
		return new AwtColor(r, g, b, a);
	}

	public JSVColor get3(int r, int g, int b) {
		return new AwtColor(r, g, b);
	}

	public AwtColor(int rgb) {
		super(rgb | 0xFF000000);
	}
	
	public AwtColor(int r, int g, int b) {
		super(r, g, b);
	}

	public AwtColor(int r, int g, int b, int a) {
		super(r, g, b, a);
	}
	
	public String getCSS() {
		return "#" + JSVColorUtil.colorToHexString(this);
	}
}
