package jspecview.api;

import java.io.OutputStream;

import org.jmol.api.ApiPlatform;
import org.jmol.util.JmolFont;
import org.jmol.util.JmolList;

import jspecview.common.Annotation;
import jspecview.common.GraphSet;
import jspecview.common.JDXSpectrum;
import jspecview.common.PanelData;
import jspecview.common.PrintLayout;
import jspecview.common.Annotation.AType;
import jspecview.util.JSVColor;

public interface JSVPanel extends JSVViewPanel {

	public void repaint();

	public void doRepaint();
  
	JSVColor getColor1(int argb);
  JSVColor getColor4(int r, int g, int b, int a);

  void getFocusNow(boolean asThread);
  String getInput(String message, String title, String sval);
  GraphSet getNewGraphSet();
  PanelData getPanelData();

  boolean hasFocus();

  void setToolTipText(String s);
  void setupPlatform();
	AnnotationDialog showDialog(AType type);
	void showMessage(String msg, String title);

	ApiPlatform getApiPlatform();

	void setBackgroundColor(JSVColor color);

	JSVColor getBackgroundColor();

	void setGraphicsFont(Object og, JmolFont font);
	void setGraphicsColor(Object og, JSVColor c);

	JSVColor getColor3(int red, int green, int blue);
	
	void draw2DImage(Object og, Object image2D, int destX, int destY, int destWidth, int destHeight, int srcX0, int srcY0, int srcX1, int srcY1);

	Annotation getColoredAnnotation(JDXSpectrum spectrum, double x,
			double y, String text, JSVColor bLACK, boolean isPixels, boolean is2d,
			int offsetX, int offsetY);

	Annotation getNextAnnotation(JDXSpectrum spectrum, JmolList<String> args, Annotation lastAnnotation);

	Annotation newAnnotation(double x, double y,
			JDXSpectrum spec, String text, JSVColor color, boolean isPixels, boolean is2D,
			int offsetX, int offsetY);

	Object newImage(int width, int height, int[] buffer);

	void drawLine(Object g, int x0, int y0, int x1, int y1);

	void drawRect(Object g, int xPixel, int yPixel,
			int xPixels, int yPixels);

	void fillRect(Object og, int x, int y, int width, int height);

	void drawString(Object g, String s, int x, int y);

	void setStrokeBold(Object g, boolean tf);

	int getFontHeight(Object g);

	int getStringWidth(Object g, String s);

	void fillPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints);

	void drawPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints);

	void fillOval(Object g, int x, int y, int width, int height);

	void drawOval(Object g, int x, int y, int width, int height);

	void rotatePlot(Object g, int angle, int x, int y);

	int getFontFaceID(String name);

  int getOptionFromDialog(Object frame, String[] items,
			String dialogName, String labelName);

	Object[][] getOverlayLegendData();

  void saveImage(String type, Object file);

	public void printPanel(PrintLayout pl, OutputStream os, String printJobTitle);

	public String export(String type, int n);

}
