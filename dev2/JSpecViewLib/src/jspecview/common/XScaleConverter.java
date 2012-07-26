package jspecview.common;

interface XScaleConverter {
	
  int fixX(int xPixel);
  double toX(int xPixel);  
  int toPixelX(double x);
	int getXPixels();

}
