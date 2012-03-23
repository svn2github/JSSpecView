package jspecview.common;

import java.util.List;

import jspecview.source.JDXSource;


public interface ScriptInterface {
  public void runScript(String script);
  public JSVPanel setSpectrum(int i);
  public void sendFrameChange(JSVPanel jsvp);
  public String execExport(JSVPanel jsvp, String value);
  public void execOverlay(String value);
  public void execIntegrate(String value);
  public List<JSVSpecNode> getSpecNodes();
  public JDXSource getCurrentSource();
  public void execSetIntegrationRatios(String value);
  public String setSolutionColor(boolean b);
  public void execTAConvert(int iMode) throws Exception;
  public void execSetCallback(ScriptToken st, String value);
  public Parameters getParameters();
  public void execClose(String value);
  public String execLoad(String value);
  public void execHidden(boolean b);
  public void execSetInterface(String value);
  public void execScriptComplete(String msg, boolean isOK);
  public JSVPanel execSetSpectrum(String value);
  public JSVPanel getSelectedPanel();
  public void execSetAutoIntegrate(boolean b);
  public void execTest(String value);
  public void syncToJmol(String value);

  //public void exportSpectrum(String actionCommand);
  //public void printView();
}
