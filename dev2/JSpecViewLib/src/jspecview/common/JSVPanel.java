package jspecview.common;

import java.awt.Container;

public interface JSVPanel extends JSVContainer {

  void repaint();
  
  void drawCoordinates(Object g, int height, int width);
  void drawTitle(Object g, int height, int width, String title);
  Object getColor(ScriptToken st);
  Object getColor(int r, int g, int b, int a);
  String getInput(String message, String title, String sval);
  GraphSet getNewGraphSet(GraphSet gs);
  JSVPanel getNewPanel(JDXSpectrum spectrum);
  PanelData getPanelData();
  Object getPlotColor(int i);
  Object getPopup();
  JDXSpectrum getSpectrum();
  JDXSpectrum getSpectrumAt(int i);
  String getViewTitle();
  void setColor(ScriptToken st, Object color);
  void setColorOrFont(Parameters ds, ScriptToken st);
  void setFont(Object g, String string, int width, int mode, int size, boolean isLabel);
  void setPlotColors(Object plotColors);
  void setSpectrum(JDXSpectrum spec);
  void setToolTipText(String s);
	void setViewTitle(String filePath);
  void setupPlatform();
	void showSolutionColor(Object container);
	void showHeader(Container jsvApplet);


}
