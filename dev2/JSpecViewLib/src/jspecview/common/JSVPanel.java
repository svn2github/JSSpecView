package jspecview.common;

import org.jmol.api.ApiPlatform;
import org.jmol.util.JmolFont;
import org.jmol.util.JmolList;

import jspecview.api.ScriptInterface;
import jspecview.common.Annotation.AType;
import jspecview.util.JSVColor;

public interface JSVPanel extends JSVContainer {

	public void doRepaint();
  
  JSVColor getColor(ScriptToken st);
  JSVColor getColor4(int r, int g, int b, int a);
  void getFocusNow(boolean asThread);
  String getInput(String message, String title, String sval);
  GraphSet getNewGraphSet();
  JSVPanel getNewPanel(ScriptInterface si, JDXSpectrum spectrum);
  PanelData getPanelData();

  boolean hasFocus();

  JSVColor getPlotColor(int i);
	void setColor(ScriptToken st, JSVColor color);
  void setColorOrFont(ColorParameters ds, ScriptToken st);
  void setPlotColors(JSVColor[] plotColors);
  
  void setToolTipText(String s);
  void setupPlatform();
	void showHeader(Object jsvApplet);
	AnnotationDialog showDialog(AType type);
	void showMessage(String msg, String title);

	ApiPlatform getApiPlatform();

	void setBackgroundColor(JSVColor color);

	JSVColor getBackgroundColor();

	void setGraphicsFont(Object og, JmolFont font);
	void setGraphicsColor(Object og, JSVColor c);

	JSVColor getColor3(int red, int green, int blue);
	
	void draw2DImage(Object og, Object image2D, int destX, int destY, int destWidth, int destHeight, int srcX0, int srcY0, int srcX1, int srcY1);

	public Annotation getColoredAnnotation(JDXSpectrum spectrum, double x,
			double y, String text, JSVColor bLACK, boolean isPixels, boolean is2d,
			int offsetX, int offsetY);

	public Annotation getNextAnnotation(JDXSpectrum spectrum, JmolList<String> args, Annotation lastAnnotation);

	public Object newImage(int width, int height, int[] buffer);

	void drawLine(Object g, int x0, int y0, int x1, int y1);

	void drawRect(Object g, int xPixel, int yPixel,
			int xPixels, int yPixels);

	public void fillRect(Object og, int x, int y, int width, int height);

	void drawString(Object g, String s, int x, int y);

	void setStrokeBold(Object g, boolean tf);

	int getFontHeight(Object g);

	public int getStringWidth(Object g, String s);

	public void fillPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints);

	public void drawPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints);

	public void fillOval(Object g, int x, int y, int width, int height);

	public void drawOval(Object g, int x, int y, int width, int height);

	public void rotatePlot(Object g, int angle, int x, int y);

	public int getFontFaceID(String name);

}
