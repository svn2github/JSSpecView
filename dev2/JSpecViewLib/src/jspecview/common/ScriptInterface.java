package jspecview.common;

import java.awt.Cursor;
import java.net.URL;
import java.util.List;

import jspecview.source.JDXSource;


public interface ScriptInterface {
  
  public String execExport(JSVPanel jsvp, String value);
  public void execClose(String value, boolean fromScript);
  public void execHidden(boolean b);
  public void execIntegrate(JDXSpectrum spec);
  public String execLoad(String value);
  public void execScriptComplete(String msg, boolean isOK);
  public void execSetAutoIntegrate(boolean b);
  public void execSetCallback(ScriptToken st, String value);
  public JSVPanel execSetSpectrum(String value);
  public void execSetIntegrationRatios(String value);
  public void execSetInterface(String value);
  public void execTAConvert(int iMode);
  public void execTest(String value);

  public JDXSource getCurrentSource();
  public PanelData getPanelData();
  public Parameters getParameters();
  public JSVPanel getSelectedPanel();
  public List<JSVPanelNode> getPanelNodes();
  
  public void runScript(String script);
  public void sendPanelChange(JSVPanel jsvp);
  public String setSolutionColor(boolean b);
  public void syncToJmol(String value);
  public JSVDialog getOverlayLegend(JSVPanel jsvp);
  public void setSelectedPanel(JSVPanel jsvp);
  public void syncLoad(String fileName);
  public void showProperties();
  public void updateBoolean(ScriptToken st, boolean TF);
  public void checkCallbacks(String title);
	public void print();
	
  public void closeSource(JDXSource source);
	public Object getSpectraTree();
	public int getFileCount();
	public void setFileCount(int max);
  public void setNode(JSVPanelNode node, boolean fromTree);
	public JSVPanelNode setOverlayVisibility(JSVPanelNode node);
	public void setCurrentSource(JDXSource source);
	public int incrementViewCount(int i);
	public void setRecentURL(String filePath);
	public void setRecentFileName(String fileName);
	public void writeStatus(String msg);
	public void setCursor(Cursor predefinedCursor);
	public void setLoaded(String fileName, String filePath);
	public boolean getAutoCombine();
	public void updateRecentMenus(String filePath);
	public void process(List<JDXSpectrum> specs);
	public Object getPopupMenu();
	public void setPropertiesFromPreferences(JSVPanel jsvp, boolean b);
	public boolean getAutoShowLegend();
	public void setMenuEnables(JSVPanelNode node, boolean isSplit);
	public JDXSource createSource(String data, String filePath, URL base, int firstSpec,
			int lastSpec) throws Exception;
	public URL getDocumentBase();
	public JSVPanel getNewJSVPanel(List<JDXSpectrum> specs);
	public JSVPanelNode getNewPanelNode(String id, String fileName,
			JDXSource source, JSVPanel jsvp);
	public JSVPanel getNewJSVPanel(JDXSpectrum spec);
	public void openDataOrFile(String data, String name,
			List<JDXSpectrum> specs, String url, int firstSpec, int lastSpec);
	public void checkOverlay();
	public void validateAndRepaint();
	public void setReturnFromJmolModel(String model);
	public String getReturnFromJmolModel();
} 
