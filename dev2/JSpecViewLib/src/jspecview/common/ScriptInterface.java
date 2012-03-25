package jspecview.common;

import java.util.List;

import jspecview.source.JDXSource;


public interface ScriptInterface {
  
  public String execExport(JSVPanel jsvp, String value);
  public void execClose(String value);
  public void execHidden(boolean b);
  public void execIntegrate(String value);
  public String execLoad(String value);
  public void execOverlay(String value);
  public void execScriptComplete(String msg, boolean isOK);
  public void execSetAutoIntegrate(boolean b);
  public void execSetCallback(ScriptToken st, String value);
  public JSVPanel execSetSpectrum(String value);
  public void execSetIntegrationRatios(String value);
  public void execSetInterface(String value);
  public void execTAConvert(int iMode) throws Exception;
  public void execTest(String value);

  public JDXSource getCurrentSource();
  public PanelData getPanelData();
  public Parameters getParameters();
  public JSVPanel getSelectedPanel();
  public List<JSVSpecNode> getSpecNodes();
  
  public void runScript(String script);
  public void sendFrameChange(JSVPanel jsvp);
  public JSVPanel setSpectrumIndex(int i);
  public String setSolutionColor(boolean b);
  public void syncToJmol(String value);
  public JSVDialog getOverlayLegend(JSVPanel jsvp);
  public void setSelectedPanel(JSVPanel jsvp);
  public void sendScript(String peakScript);
  public void closeAllAndOpenFile(String fileName);
  public void setFrame(JSVSpecNode findNode);
}
