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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.text.NumberFormat;
import java.util.List;
import java.util.ArrayList;

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
  protected List<Highlight> highlights = new ArrayList<Highlight>();
  private Color[] plotColors;

  @Override
  protected void disposeImage() {
    image2D = null;
    jsvp = null;
    highlights = null;
    plotColors = null;
  }


  AwtGraphSet(AwtPanel jsvp) {
    this.jsvp = jsvp;
  }

  protected AwtGraphSet getGraphSet(Object jsvp) {
    return new AwtGraphSet((AwtPanel) jsvp);
  }

  @Override
  protected void initGraphSet(int startIndex, int endIndex) {
    setPlotColors(Parameters.defaultPlotColors);
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
        tmpPlotColors[j] = AppUtils.generateRandomColor();
      colors = tmpPlotColors;
    }
    plotColors = colors;
  }

  void setPlotColor0(Object oColor) {
    plotColors[0] = (Color) oColor;
  }

  /* -------------------Other methods ------------------------------------*/

  /**
   * Add information about a region of the displayed spectrum to be highlighted
   * 
   * @param x1
   *        the x value of the coordinate where the highlight should start
   * @param x2
   *        the x value of the coordinate where the highlight should end
   * @param spec
   * @param color
   *        the color of the highlight
   */
  void addHighlight(double x1, double x2, Graph spec, Object oColor) {
    if (spec == null)
      spec = getSpectrumAt(0);
    Highlight hl = new Highlight(x1, x2, spec, (oColor == null ? jsvp
        .getHighlightColor() : (Color) oColor));
    if (!highlights.contains(hl))
      highlights.add(hl);
  }

  /**
   * Remove the highlight at the specified index in the internal list of
   * highlights The index depends on the order in which the highlights were
   * added
   * 
   * @param index
   *        the index of the highlight in the list
   */
  void removeHighlight(int index) {
    highlights.remove(index);
  }

  /**
   * Remove the highlight specified by the starting and ending x value
   * 
   * @param x1
   *        the x value of the coordinate where the highlight started
   * @param x2
   *        the x value of the coordinate where the highlight ended
   */
  void removeHighlight(double x1, double x2) {
    for (int i = highlights.size(); --i >= 0;) {
      Highlight h = highlights.get(i);
      if (h.x1 == x1 && h.x2 == x2)
        highlights.remove(i);
    }
  }

  void removeAllHighlights(Graph spec) {
    if (spec == null)
      highlights.clear();
    else
      for (int i = highlights.size(); --i >= 0;)
        if (highlights.get(i).spectrum == spec)
          highlights.remove(i);
    jsvp.repaint();
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
    int[] buffer = spec0.get2dBuffer(jsvp.thisWidth, jsvp.thisPlotHeight, isd,
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
    setImageWindow(jsvp.display1D);
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
  void repaint() {
    jsvp.repaint();
  }

  @Override
  void refresh() {
    jsvp.refresh();
  }

  @Override
  protected void notifySubSpectrumChange(int i, JDXSpectrum spectrum) {
    jsvp.notifySubSpectrumChange(i, spectrum);
  }

  /**
   * Private class to represent a Highlighted region of the spectrum display
   * <p>
   * Title: JSpecView
   * </p>
   * <p>
   * Description: JSpecView is a graphical viewer for chemical spectra specified
   * in the JCAMP-DX format
   * </p>
   * <p>
   * Copyright: Copyright (c) 2002
   * </p>
   * <p>
   * Company: Dept. of Chemistry, University of the West Indies, Mona Campus,
   * Jamaica
   * </p>
   * 
   * @author Debbie-Ann Facey
   * @author Khari A. Bryan
   * @author Prof Robert.J. Lancashire
   * @version 1.0.017032006
   */
  private class Highlight {
    private double x1;
    private double x2;
    private Color color = new Color(255, 255, 0, 100);
    private Graph spectrum;

    /**
     * Constructor
     * 
     * @param x1
     *        starting x coordinate
     * @param x2
     *        ending x coordinate
     */
    Highlight(double x1, double x2) {
      this.x1 = x1;
      this.x2 = x2;
    }

    /**
     * Constructor
     * 
     * @param x1
     *        starting x coordinate
     * @param x2
     *        ending x coordinate
     * @param spec
     * @param color
     *        the color of the highlighted region
     */
    Highlight(double x1, double x2, Graph spec, Color color) {
      this(x1, x2);
      this.color = color;
      spectrum = spec;
    }

    /**
     * Returns the x coordinate where the highlighted region starts
     * 
     * @return the x coordinate where the highlighted region starts
     */
    double getStartX() {
      return x1;
    }

    /**
     * Returns the x coordinate where the highlighted region ends
     * 
     * @return the x coordinate where the highlighted region ends
     */
    double getEndX() {
      return x2;
    }

    /**
     * Returns the color of the highlighted region
     * 
     * @return the color of the highlighted region
     */
    Color getColor() {
      return color;
    }

    /**
     * Overides the equals method in class <code>Object</code>
     * 
     * @param obj
     *        the object that this <code>Highlight<code> is compared to
     * @return true if equal
     */
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Highlight))
        return false;
      Highlight hl = (Highlight) obj;

      return ((hl.x1 == this.x1) && (hl.x2 == this.x2));
    }
  }

  @Override
  protected String getCoordString() {
    return jsvp.coordStr;
  }

  @Override
  protected void notifyPeakPickedListeners() {
    jsvp.notifyPeakPickedListeners();
  }

  @Override
  protected Coordinate setCoordClicked(double x, double y) {
    if (Double.isNaN(x)) {
      jsvp.coordClicked = null;
      jsvp.coordsClicked = null;
      return null;
    }
    jsvp.coordClicked = new Coordinate(x, y);
    jsvp.coordsClicked = getSpectrum().getXYCoords();
    return jsvp.coordClicked;
  }

  @Override
  protected void setIntegralDrag(boolean b) {
    jsvp.isIntegralDrag = b;
  }

  @Override
  protected PlotWidget getThisWidget() {
    return jsvp.thisWidget;
  }

  @Override
  protected boolean isIntegralDrag() {
    return jsvp.isIntegralDrag;
  }

  @Override
  protected void setThisWidget(PlotWidget widget) {
    jsvp.thisWidget = widget;
  }

  @Override
  protected void notifyPeakListeners(PeakInfo peak) {
    jsvp.notifyListeners(new PeakPickEvent(jsvp, jsvp.coordClicked,
        peak == null ? PeakInfo.nullPeakInfo : peak));
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
  protected void setColor(Object g, ScriptToken whatColor) {
    if (whatColor != null)
      ((Graphics) g)
          .setColor(whatColor == ScriptToken.PLOTCOLOR ? plotColors[0] : jsvp
              .getColor(whatColor));
  }

  @Override
  protected void drawHighlights(Object g, Graph spec) {
    for (int i = 0; i < highlights.size(); i++) {
      Highlight hl = highlights.get(i);
      if (hl.spectrum == spec) {
        jsvp.setHighlightColor(hl.getColor());
        drawBar(g, hl.getStartX(), hl.getEndX(), ScriptToken.HIGHLIGHTCOLOR,
            true);
      }
    }
  }

  @Override
  protected void drawHandle(Object g, int x, int y) {
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
  protected boolean isCurrentGraphSet() {
    return (this == jsvp.currentGraphSet);
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
  protected NumberFormat getFormatter(String hash) {
    return jsvp.getFormatter(hash);
  }

  @Override
  protected void setFont(Object g, int width, int face, int size,
                         boolean isLabel) {
    jsvp.setFont(g, width, face, size, isLabel);
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
  protected boolean setStartupPinTip() {
    if (jsvp.startupPinTip == null)
      return false;
    jsvp.setToolTipText(jsvp.startupPinTip);
    jsvp.startupPinTip = null;
    return true;
  }

  @Override
  protected void setCoordStr(String string) {
    jsvp.coordStr = string;
  }

  @Override
  protected void setToolTipText(String s) {
    jsvp.setToolTipText(s);
  }

}
