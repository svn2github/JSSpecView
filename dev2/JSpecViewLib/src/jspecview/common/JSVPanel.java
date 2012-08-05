package jspecview.common;

import jspecview.common.Annotation.AType;

public interface JSVPanel extends JSVContainer {

	public void doRepaint();
  
  void drawCoordinates(Object g, int top);
  void drawTitle(Object g, int pageHeight, int pageWidth, String title);
	
  Object getColor(ScriptToken st);
  Object getColor(int r, int g, int b, int a);
  void getFocusNow(boolean asThread);
  String getInput(String message, String title, String sval);
  GraphSet getNewGraphSet();
  JSVPanel getNewPanel(ScriptInterface si, JDXSpectrum spectrum);
  PanelData getPanelData();
  Object getPlotColor(int i);
  Object getPopup();

  boolean hasFocus();

  void printFilePath(Object g, int x, int y, String filePath);
	void printVersion(Object g, int height);

	void setColor(ScriptToken st, Object color);
  void setColorOrFont(Parameters ds, ScriptToken st);
  void setFont(Object g, String string, int width, int mode, int size, boolean isLabel);
  void setPlotColors(Object plotColors);
  void setToolTipText(String s);
  void setupPlatform();
	void showHeader(Object jsvApplet);
	AnnotationDialog showDialog(AType type);
	void showMessage(String msg, String title);



}
