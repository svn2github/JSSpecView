package jspecview.api;

import org.jmol.util.JmolFont;

import jspecview.util.JSVColor;

public interface JSVGraphics {

	JSVColor getColor1(int argb);

	JSVColor getColor3(int red, int green, int blue);
	
	JSVColor getColor4(int r, int g, int b, int a);

	void setGraphicsFont(Object og, JmolFont font);
	
	void setGraphicsColor(Object og, JSVColor c);

	void draw2DImage(Object og, Object image2D, int destX, int destY, int destWidth, int destHeight, int srcX0, int srcY0, int srcX1, int srcY1);

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

	public void translateScale(Object g, double x, double y, double scale);

}
