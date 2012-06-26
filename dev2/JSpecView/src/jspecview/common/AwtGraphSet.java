/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * AwtGraphSet class represents a set of overlaid spectra within some
 * subset of the main JSVPanel. See also GraphSet.java
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

class AwtGraphSet extends GraphSet {

  private AwtPanel jsvp;
  private BufferedImage image2D;
  private Color[] plotColors;

  @Override
  protected void disposeImage() {
    image2D = null;
    jsvp = null;
    pd = null;
    highlights = null;
    plotColors = null;
  }


  AwtGraphSet(AwtPanel jsvp, GraphSet superSet) {
    this.jsvp = jsvp;
    this.pd = jsvp.pd;
  }

  @Override
  protected void initGraphSet(int startIndex, int endIndex) {
    setPlotColors(AwtParameters.defaultPlotColors);
    super.initGraphSet(startIndex, endIndex);
  }

  void setPlotColors(Object oColors) {
    Color[] colors = (Color[]) oColors;
    if (colors.length > nSpectra) {
      Color[] tmpPlotColors = new Color[nSpectra];
      System.arraycopy(colors, 0, tmpPlotColors, 0, nSpectra);
      colors = tmpPlotColors;
    } else if (nSpectra > colors.length) {
      Color[] tmpPlotColors = new Color[nSpectra];
      int numAdditionColors = nSpectra - colors.length;
      System.arraycopy(colors, 0, tmpPlotColors, 0, colors.length);
      for (int i = 0, j = colors.length; i < numAdditionColors; i++, j++)
        tmpPlotColors[j] = generateRandomColor();
      colors = tmpPlotColors;
    }
    plotColors = colors;
  }

  private static Color generateRandomColor() {
    while (true) {
      int red = (int) (Math.random() * 255);
      int green = (int) (Math.random() * 255);
      int blue = (int) (Math.random() * 255);
      Color randomColor = new Color(red, green, blue);
      if (!randomColor.equals(Color.blue))
        return randomColor;
    }
  }

  void setPlotColor0(Object oColor) {
    plotColors[0] = (Color) oColor;
  }

  /**
   * Returns the color of the plot at a certain index
   * 
   * @param index
   *        the index
   * @return the color of the plot
   */
  Color getPlotColor(int index) {
    if (index >= plotColors.length)
      return null;
    return plotColors[index];
  }

  @Override
  protected void setColor(Object g, ScriptToken whatColor) {
    if (whatColor != null)
      ((Graphics) g)
          .setColor(whatColor == ScriptToken.PLOTCOLOR ? plotColors[0] : jsvp
              .getColor(whatColor));
  }

  /////////////// 2D image /////////////////

  protected void draw2DImage(Object g) {
    if (isd != null) {
      ((Graphics) g).drawImage(image2D, isd.xPixel0, isd.yPixel0, // destination 
          isd.xPixel0 + isd.xPixels - 1, // destination 
          isd.yPixel0 + isd.yPixels - 1, // destination 
          isd.xView1, isd.yView1, isd.xView2, isd.yView2, null); // source
    }
  }

  @Override
  protected boolean get2DImage() {
    isd = new ImageScaleData();
    isd.setScale(zoomInfoList.get(0));
    if (!update2dImage(false))
      return false;
    isd.resetZoom();
    sticky2Dcursor = true;
    return true;
  }

  @Override
  protected boolean update2dImage(boolean forceNew) {
    isd.setScale(multiScaleData);
    JDXSpectrum spec0 = getSpectrumAt(0);
    int[] buffer = spec0.get2dBuffer(jsvp.pd.thisWidth, jsvp.pd.thisPlotHeight, isd,
        forceNew);
    if (buffer == null) {
      image2D = null;
      isd = null;
      return false;
    }
    isd.setImageSize(spec0.getXYCoords().length, spec0.getSubSpectra().size(),
        !forceNew);
    image2D = new BufferedImage(isd.imageWidth, isd.imageHeight,
        BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster raster = image2D.getRaster();
    raster.setSamples(0, 0, isd.imageWidth, isd.imageHeight, 0, buffer);
    setImageWindow();
    return true;
  }

  @Override
  Annotation getAnnotation(double x, double y, String text, boolean isPixels,
                           boolean is2d, int offsetX, int offsetY) {
    return new ColoredAnnotation(x, y, text, Color.BLACK, isPixels, is2d,
        offsetX, offsetY);
  }

  @Override
  Annotation getAnnotation(List<String> args, Annotation lastAnnotation) {
    return ColoredAnnotation.getAnnotation(args,
        (ColoredAnnotation) lastAnnotation);
  }

  @Override
  protected String getInput(String message, String title, String sval) {
    return (String) JOptionPane.showInputDialog(null, message, title,
        JOptionPane.PLAIN_MESSAGE, null, null, sval);
  }

  @Override
  protected void fillBox(Object g, int x0, int y0, int x1, int y1,
                         ScriptToken whatColor) {
    setColor(g, whatColor);
    ((Graphics) g).fillRect(Math.min(x0, x1), Math.min(y0, y1), Math.abs(x0
        - x1), Math.abs(y0 - y1));
  }

  @Override
  protected void drawTitle(Object g, int height, int width, String title) {
    jsvp.drawTitle(g, height, width, title);
  }

  @Override
  protected void drawHandle(Object g, int x, int y, boolean outlineOnly) {
    if (outlineOnly)
      ((Graphics) g).drawRect(x - 2, y - 2, 4, 4);
    else
      ((Graphics) g).fillRect(x - 2, y - 2, 5, 5);
  }

  @Override
  protected void drawLine(Object g, int x0, int y0, int x1, int y1) {
    ((Graphics) g).drawLine(x0, y0, x1, y1);
  }

  @Override
  protected void setPlotColor(Object g, int i) {
    ((Graphics) g)
        .setColor(i < 0 ? jsvp.getColor(ScriptToken.INTEGRALPLOTCOLOR) : plotColors[i]);
  }

  @Override
  protected void drawRect(Object g, int x0, int y0, int x1, int y1) {
    ((Graphics) g).drawRect(x0, y0, x1, y1);
  }

  @Override
  protected void setCurrentBoxColor(Object g) {
    ((Graphics) g).setColor(Color.MAGENTA);
  }

  @Override
  protected void drawString(Object g, String s, int x, int y) {
    ((Graphics) g).drawString(s, x, y);
  }

  @Override
  protected int getFontHeight(Object g) {
    return ((Graphics) g).getFontMetrics().getHeight();
  }

  @Override
  protected int getStringWidth(Object g, String s) {
    return ((Graphics) g).getFontMetrics().stringWidth(s);
  }

  @Override
  protected void setAnnotationColor(Object g, Annotation note,
                                    ScriptToken whatColor) {
    if (whatColor != null) {
      setColor(g, whatColor);
      return;
    }
    Color color = null;
    if (note instanceof ColoredAnnotation)
      color = ((ColoredAnnotation) note).getColor();
    if (color == null)
      color = Color.BLACK;
    ((Graphics) g).setColor(color);
  }


  @Override
  protected void setColor(Object g, int red, int green, int blue) {
    ((Graphics) g).setColor(new Color(red, green, blue));
  }

  BasicStroke strokeBasic = new BasicStroke();
  BasicStroke strokeBold = new BasicStroke(2f);

	@Override
	protected void setStrokeBold(Object g, boolean tf) {
		((Graphics2D) g).setStroke(tf ? strokeBold : strokeBasic);
	}
	
	@Override
	protected void fillArrow(Object g, boolean isUp, int x, int y) {
		int f = (isUp ? -1 : 1);
		int[] axPoints = new int[] { x - 5,   x - 5, x + 5,   x + 5,   x + 8,        x, x - 8 }; 
		int[] ayPoints = new int[] { y + 5*f, y - f, y - f, y + 5*f, y + 5*f, y + 10*f, y + 5*f }; 
		((Graphics)g).fillPolygon(axPoints, ayPoints, 7);
	}

}
