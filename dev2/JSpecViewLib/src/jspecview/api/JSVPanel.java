package jspecview.api;

import java.io.IOException;
import java.io.OutputStream;

import org.jmol.api.ApiPlatform;

import jspecview.common.JDXSpectrum;
import jspecview.common.PanelData;
import jspecview.common.PrintLayout;
import jspecview.util.JSVColor;

public interface JSVPanel extends JSVViewPanel {

	public void repaint();

	public void doRepaint();
  
  void getFocusNow(boolean asThread);
  String getInput(String message, String title, String sval);
  PanelData getPanelData();

  boolean hasFocus();

  void setToolTipText(String s);

  void showMessage(String msg, String title);

	ApiPlatform getApiPlatform();

	void setBackgroundColor(JSVColor color);

	int getFontFaceID(String name);

  void saveImage(String type, Object file);

	public void printPanel(PrintLayout pl, OutputStream os, String printJobTitle);

	public String exportTheSpectrum(String type, String path, JDXSpectrum spec,
			int startIndex, int endIndex) throws IOException;

	public boolean handleOldJvm10Event(int id, int x, int y, int modifiers, long time);

	public void processTwoPointGesture(float[][][] touches);

}
