package jspecview.awt;

import jspecview.util.JSVColor;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import java.util.List;

import jspecview.common.ColorParameters;
import jspecview.g2d.AwtColor;

public class AwtParameters extends ColorParameters {

  public AwtParameters(String name) {
		super(name);
	}

	@Override
	protected boolean isValidFontName(String name) {
    GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
    List<String> fontList = Arrays.asList(g.getAvailableFontFamilyNames());
    for (String s : fontList)
      if (name.equalsIgnoreCase(s))
      	return true;
    return false;
	}
  
  @Override
	public JSVColor getColor1(int rgb) {
    return new AwtColor(rgb);
  }

	@Override
	protected JSVColor getColor3(int r, int g, int b) {
		return new AwtColor(r, g, b);
	}

  @Override
	public ColorParameters copy(String newName){
    return new AwtParameters(newName).setElementColors(this);
  }

}
