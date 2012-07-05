package jspecview.common;

public interface JSVPanel extends JSVContainer {

  void repaint();
  
  void doRequestFocusInWindow();
  void drawCoordinates(Object g, int height, int width);
  void drawTitle(Object g, int height, int width, String title);
  Object getColor(ScriptToken st);
  Object getColor(int r, int g, int b, int a);
  GraphSet getNewGraphSet(GraphSet gs);
  JSVPanel getNewPanel(JDXSpectrum spectrum);
  PanelData getPanelData();
  Object getPlotColor(int i);
  Object getPopup();
  JDXSpectrum getSpectrum();
  JDXSpectrum getSpectrumAt(int i);
  void setColor(ScriptToken st, Object color);
  void setColorOrFont(Parameters ds, ScriptToken st);
  void setFont(Object g, String string, int mode, int size);
  void setPlotColors(Object plotColors);
  void setSpectrum(JDXSpectrum spec);
  void setToolTipText(String s);
  void setupPlatform();
}
