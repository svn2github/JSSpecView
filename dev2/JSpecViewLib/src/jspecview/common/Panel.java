package jspecview.common;

interface Panel {

  boolean requestFocusInWindow();
  void repaint();

  Object getColor(ScriptToken st);
  Panel getNewPanel(JDXSpectrum spectrum);
  public Panel getIntegralPanel(JDXSpectrum spectrum);
  GraphSet newGraphSet();
  void setupPlatform();
  void setFont(Object g, String string, int mode, int size);
  void drawCoordinates(Object g, int height, int width);
  void drawTitle(Object g, int height, int width, String title);
  public void setColorOrFont(Parameters ds, ScriptToken st);
  JDXSpectrum getSpectrum();
  JDXSpectrum getSpectrumAt(int i);
  PanelData getPanelData();
  Object getPlotColor(int i);
  void setPlotColors(Object plotColors);
  Object getPopup();
  void setToolTipText(String s);
  void setColor(ScriptToken st, Object color);
}
