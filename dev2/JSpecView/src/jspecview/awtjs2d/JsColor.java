package jspecview.awtjs2d;

import jspecview.api.JSVColor;

public class JsColor implements JSVColor {

	public int argb;

	public int getRGB() {
		return argb & 0x00FFFFFF;
	}

	public int getOpacity255() {
		return ((argb >> 24) & 0xFF);
	}

	public void setOpacity255(int a) {
		argb = argb & 0xFFFFFF | ((a & 0xFF) << 24);
	}

	public static JSVColor get1(int rgb) {
		JsColor c = new JsColor();
		c.argb = rgb | 0xFF000000;
		return c;
	}

	public static JSVColor get3(int r, int g, int b) {
		return new JsColor().set4(r, g, b, 0xFF);
	}

	public static JSVColor get4(int r, int g, int b, int a) {
		return new JsColor().set4(r, g, b, a);
	}

	private JSVColor set4(int r, int g, int b, int a) {
		argb = ((a << 24) | (r << 16) | (g << 8) | b) & 0xFFFFFFFF;
		return this;
	}
}
