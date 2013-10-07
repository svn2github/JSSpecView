package jspecview.js2d;

import jspecview.util.JSVColor;

import jspecview.common.ColorParameters;

public class JsParameters extends ColorParameters {

  public JsParameters(String name) {
		super(name);
	}

	@Override
	protected boolean isValidFontName(String name) {
		// TODO
		return true;
	}
  
  @Override
	public JSVColor getColor1(int rgb) {
    return JsColor.get1(rgb);
  }

	@Override
	protected JSVColor getColor3(int r, int g, int b) {
		return JsColor.get3(r, g, b);
	}

  @Override
	public ColorParameters copy(String newName){
    return new JsParameters(newName).setElementColors(this);
  }

}
