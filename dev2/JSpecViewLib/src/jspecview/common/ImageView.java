package jspecview.common;

import java.util.List;

public class ImageView {
  
  /*
   * The viewPort is related to two coordinate systems, image and screen.
   * 
   * Image Coordinates:
   * 
   *  Note that the last displayed image pixel is (xView2, yView2)
   * 
   *    (0,0)
   *     /---------------------------/
   *     /   (view1)                 /
   *     /      /-------/            /
   *     /      /////////            /
   *     /      /////////            /
   *     /      /-------/            /
   *     /            (view2)        /
   *     /                           /
   *     /---------------------------/
   *                                (width,height)
   *                                
   * Pixel(Screen) Coordinates:
   * 
   *  Note that the last displayed screen pixel is (yPixels - 1, xPixels - 1)
   *    
   *     /---------------------------/
   *     /   (pixel0)                /
   *     /      /-------/            /
   *     /      ///////// yPixels    /
   *     /      /////////            /
   *     /      /-------/            /
   *     /       xPixels             /
   *     /                           /
   *     /---------------------------/
   *                                 
   * 
   * 
   */
  public int xPixel0, yPixel0, xPixel1, yPixel1;
  public int imageWidth, imageHeight, xPixels, yPixels;
  public int xPixelZoom1, yPixelZoom1, xPixelZoom2, yPixelZoom2;
  public int xView1, yView1, xView2, yView2;
  public double minX = Double.NaN, maxX, minZ, maxZ;
  
  public void set(View view) {
    if (Double.isNaN(minX)) {
      minX = view.minX;
      maxX = view.maxX;
    }
    minZ = view.minY;
    maxZ = view.maxY;
  }

  public void setZoom(int xPixel1, int yPixel1, int xPixel2, int yPixel2) {
    xPixelZoom1 = Math.min(xPixel1, xPixel2);
    yPixelZoom1 = Math.min(yPixel1, yPixel2);
    xPixelZoom2 = Math.max(xPixel1, xPixel2);
    yPixelZoom2 = Math.max(yPixel1, yPixel2);
    setView();
  }
  
  public void setImageSize(int width, int height, boolean resetView) {
    this.imageWidth = width;
    this.imageHeight = height;
    if (resetView)
      resetView();
  }
  
  public void setXY0(int xPixel, int yPixel) {
    xPixel0 = xPixel;
    yPixel0 = yPixel;
    xPixel1 = xPixel0 + xPixels - 1;
    yPixel1 = yPixel0 + yPixels - 1;
  }
 
  public void setPixelWidthHeight (int xPixels, int yPixels) {
    this.xPixels = xPixels;
    this.yPixels = yPixels;
  }
  
  public void resetView() {
    xView1 = 0;
    yView1 = 0;
    xView2 = imageWidth - 1;
    yView2 = imageHeight - 1;
  }
  
  public void setView() {
    if (xPixelZoom1 == 0)
      resetZoom();
    int x1 = toImageX(xPixelZoom1);    
    int y1 = toImageY(yPixelZoom1);    
    int x2 = toImageX(xPixelZoom2);    
    int y2 = toImageY(yPixelZoom2); 
    xView1 = Math.min(x1, x2);
    yView1 = Math.min(y1, y2);
    xView2 = Math.max(x1, x2);
    yView2 = Math.max(y1, y2);

    resetZoom();
  }

  public void resetZoom() {
    xPixelZoom1 = xPixel0;
    yPixelZoom1 = yPixel0;
    xPixelZoom2 = xPixel1;
    yPixelZoom2 = yPixel1;
  }

  public int fixX(int xPixel) {
    return (xPixel < xPixel0 ? xPixel0 : xPixel > xPixel1 ? xPixel1 : xPixel);
  }

  public int toImageX(int xPixel) {
    return xView1 + (int) Math.floor((xPixel - xPixel0) / (xPixels - 1.0) * (xView2 - xView1));
  }

  public int toImageY(int yPixel) {
    return yView1 + (int) Math.floor((yPixel - yPixel0) / (yPixels - 1.0) * (yView2 - yView1));
  }

  public int toImageX0(int xPixel) {
    return Coordinate.intoRange((int) ((1.0 * xPixel - xPixel0) / (xPixels - 1) * (imageWidth - 1)), 0, imageWidth - 1);
  }

  public int toImageY0(int yPixel) {
    return Coordinate.intoRange((int) ((1.0 * yPixel - yPixel0) / (yPixels - 1) * (imageHeight - 1)), 0, imageHeight - 1);
  }

  public boolean isXWithinRange(int xPixel) {
    return (xPixel >= xPixel0 - 5 && xPixel < xPixel0 + xPixels + 5);
  }
  
  public double toX(int xPixel) {
    return maxX + (minX - maxX) * toImageX(fixX(xPixel)) / (imageWidth - 1);
  }
  
  public double toX0(int xPixel) {
    return maxX + (minX - maxX) * (fixX(xPixel) - xPixel0) / (xPixels - 1);
  }
  
  public int toPixelX(double x) {
    double x0 = toX(xPixel0);
    double x1 = toX(xPixel1);
    return xPixel0 + (int) ((x - x0) / (x1 - x0) * (xPixels - 1));
  }
  
  public int toPixelX0(double x) {
    //TODO -- assumes reverse axis
    return xPixel1 - (int) ((x - minX) / (maxX - minX) * (xPixels - 1));
  }
  
  public int toSubspectrumIndex(int yPixel) {
    return Coordinate.intoRange(imageHeight - 1 - toImageY(yPixel), 0, imageHeight - 1);
  }

  public int toPixelY0(double ysub) {
    return yPixel1 - (int) (ysub / (imageHeight - 1) * (yPixels - 1));
  }

  public int toPixelX(int imageX) {
    return xPixel0 + (int) ((xPixels - 1) *(1 - 1.0 *  imageX / (imageWidth - 1))); 
  }

  public int toPixelY(int subIndex) {
    // yView2 > yView1, but these are imageHeight - 1 - subIndex
    
    double f = 1.0 * (imageHeight - 1 - subIndex - yView1) / (yView2 - yView1);
    int y = yPixel0 + (int) (f * (yPixels - 1));
    return y; 
  }

  public int fixSubIndex(int subIndex) {
    return Coordinate.intoRange(subIndex, imageHeight - 1 - yView2, imageHeight - 1 - yView1);
  }

  public void setView0(int xp1, int yp1, int xp2, int yp2) {
    int x1 = toImageX0(xp1);
    int y1 = toImageY0(yp1);
    int x2 = toImageX0(xp2);
    int y2 = toImageY0(yp2);
    xView1 = Math.min(x1, x2);
    yView1 = Math.min(y1, y2);
    xView2 = Math.max(x1, x2);
    yView2 = Math.max(y1, y2);
    resetZoom();
  }
  
  private int[] buf2d;
  private int thisWidth,thisHeight;
  private double grayFactorLast;
	private double averageGray;
  /**
   * 
   * @param width
   * @param height
   * @param imageView
   * @return
   */
  public int[] get2dBuffer(int width, int height, JDXSpectrum spec, boolean forceNew) {
    List<JDXSpectrum> subSpectra = spec.getSubSpectra();
    if (subSpectra == null || !subSpectra.get(0).isContinuous())
      return null;
    if (!forceNew && thisWidth == width && thisHeight == height)
      return buf2d;
    Coordinate[] xyCoords = spec.getXYCoords();
    int nSpec = subSpectra.size();
    thisWidth = width = xyCoords.length;
    thisHeight = height = nSpec;
    double grayFactor = 255 / (maxZ - minZ);
    if (!forceNew && buf2d != null && grayFactor == grayFactorLast)
      return buf2d;
    grayFactorLast = grayFactor;
    int pt = width * height;
    int[] buf = new int[pt];
    double totalGray = 0;
    for (int i = 0; i < nSpec; i++) {
      Coordinate[] points = subSpectra.get(i).xyCoords;
      if (points.length != xyCoords.length)
        return null;
      double f = subSpectra.get(i).getUserYFactor();
      for (int j = 0; j < xyCoords.length; j++) {
        double y = points[j].getYVal();
        int gray = 255 - Coordinate.intoRange((int) ((y* f - minZ) * grayFactor), 0, 255); 
        buf[--pt] = gray;
        totalGray += gray;
      }
    }
    averageGray = (1 - totalGray / (width * height) / 255);
    System.out.println ("Average gray = " + averageGray);
    return (buf2d = buf);
  }
  
  int[] adjustView (JDXSpectrum spec, View view, double minGray, double maxGray) {
  	//double minGray = 0.05;
  	//double maxGray = 0.20;
  	int i = 0;
  	boolean isLow = false;
  	while (((isLow = (averageGray < minGray)) || averageGray > maxGray) && i++ < 10) {
      view.scaleSpectrum(-2, isLow ? 2 : 0.5);
      set(view);
      get2dBuffer(thisWidth, thisHeight, spec, true);   
      System.out.println("ImageView adjustView " + i + " " + minZ + "  " + maxZ );
  	} 
  	return buf2d;
  }

}