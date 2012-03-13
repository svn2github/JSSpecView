package jspecview.common;

public class ImageScaleData {
  
  /*
   * Image Coordinates:
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
  public int xPixel0, yPixel0, imageWidth, imageHeight, xPixels, yPixels;
  public int xPixelZoom1, yPixelZoom1, xPixelZoom2, yPixelZoom2;
  public int xView1, yView1, xView2, yView2;
  public double minX = Double.NaN, maxX, minZ, maxZ;

  public ImageScaleData copy() {
    ImageScaleData isd = new ImageScaleData();
    isd.setXY0(xPixel0, yPixel0);
    isd.setImageSize(imageWidth, imageHeight);
    isd.setPixelWidthHeight(xPixels, yPixels);
    isd.minX = minX;
    isd.minZ = minZ;
    isd.maxX = maxX;
    isd.maxZ = maxZ;
    isd.resetZoom();
    isd.setView();
    return isd;
  }
  
  public void setScale(ScaleData scaleData) {
    if (Double.isNaN(minX)) {
      minX = scaleData.minX;
      maxX = scaleData.maxX;
    }
    minZ = scaleData.minY;
    maxZ = scaleData.maxY;
  }

  public void setZoom(int xPixel1, int yPixel1, int xPixel2, int yPixel2) {
    xPixelZoom1 = Math.min(xPixel1, xPixel2);
    yPixelZoom1 = Math.min(yPixel1, yPixel2);
    xPixelZoom2 = Math.max(xPixel1, xPixel2);
    yPixelZoom2 = Math.max(yPixel1, yPixel2);
    setView();
  }
  
  public void setImageSize(int width, int height) {
    this.imageWidth = xView2 = width;
    this.imageHeight = yView2 = height;
    
  }
  
  public void setXY0(int xPixel, int yPixel) {
    xPixel0 = xPixel;
    yPixel0 = yPixel;
  }
 
  public void setPixelWidthHeight (int xPixels, int yPixels) {
    this.xPixels = xPixels;
    this.yPixels = yPixels;   
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
    xPixelZoom2 = xPixel0 + xPixels;
    yPixelZoom2 = yPixel0 + yPixels;
  }

  public int fixX(int xPixel) {
    return (xPixel < xPixel0 ? xPixel0
        : xPixel >= xPixel0 + xPixels ? xPixel = xPixel0 + xPixels - 1
            : xPixel);
  }

  public int toImageX(double xPixel) {
    return xView1 + (int) Math.floor((xPixel - xPixel0) / xPixels * (xView2 - xView1));
  }

  public int toImageY(double yPixel) {
    return yView1 + (int) Math.floor((yPixel - yPixel0) / yPixels * (yView2 - yView1));
  }

  public boolean isXWithinRange(int xPixel) {
    return (xPixel >= xPixel0 - 5 && xPixel < xPixel0 + xPixels + 5);
  }
  
  public double toX(int xPixel) {
    return maxX + (minX - maxX) * toImageX(fixX(xPixel)) / imageWidth;
  }
  
  public int toSubSpectrumIndex(int yPixel) {
    return Math.min(imageHeight - 1, Math.max(0, imageHeight - 1 - toImageY(yPixel)));
  }

  public int toPixelX(int imageX) {
    return xPixel0 + (int) (xPixels *(1 - 1.0 *  imageX / imageWidth)); 
  }

  public int toPixelY(int subIndex) {
    double f = 1.0 * (subIndex - (imageHeight - yView1)) / (yView2 - yView1);
    int y = yPixel0 - (int) (f * yPixels);
    return y; 
  }

}