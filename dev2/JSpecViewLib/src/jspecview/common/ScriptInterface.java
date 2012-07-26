package jspecview.common;

import java.net.URL;
import java.util.List;

import jspecview.common.JDXSpectrum.IRMode;
import jspecview.source.JDXSource;

public interface ScriptInterface {

	public void checkCallbacks(String title);

	public void checkOverlay();

	public void closeSource(JDXSource source);

	public JDXSource createSource(String data, String filePath, URL base,
			int firstSpec, int lastSpec) throws Exception;

	public void execClose(String value, boolean fromScript);

	public String execExport(JSVPanel jsvp, String value);

	public void execHidden(boolean b);

	public String execLoad(String value);

	public void execScriptComplete(String msg, boolean isOK);

	public void execSetAutoIntegrate(boolean b);

	public void execSetCallback(ScriptToken st, String value);

	public void execSetInterface(String value);

	public void execTest(String value);

	public boolean getAutoCombine();

	public boolean getAutoShowLegend();

	public JDXSource getCurrentSource();

	public URL getDocumentBase();

	public int getFileCount();

	public String getIntegrationRatios();

	public IRMode getIRMode();
	
	public JSVPanel getNewJSVPanel(JDXSpectrum spec);

	public JSVPanel getNewJSVPanel(List<JDXSpectrum> specs);

	public JSVPanelNode getNewPanelNode(String id, String fileName,
			JDXSource source, JSVPanel jsvp);

	public JSVDialog getOverlayLegend(JSVPanel jsvp);

	public PanelData getPanelData();

	public List<JSVPanelNode> getPanelNodes();

	public Parameters getParameters();

	public Object getPopupMenu();

	public String getReturnFromJmolModel();

	public JSVPanel getSelectedPanel();

	public Object getSpectraTree();

	public int incrementViewCount(int i);

	public int incrementScriptLevelCount(int i);

	public void openDataOrFile(String data, String name, List<JDXSpectrum> specs,
			String url, int firstSpec, int lastSpec, boolean doCheck);

	/**
	 * @param pdfFileName
	 * @return "OK" if signedApplet or app; Base64-encoded string if unsigned applet or null if problem
	 */
	public String print(String pdfFileName);

	public void requestRepaint();

	public void repaint();

	public void repaintCompleted();

	public void runScript(String script);

	public void sendPanelChange(JSVPanel jsvp);

	public void setCurrentSource(JDXSource source);

	public void setCursorObject(Object predefinedCursor);

	public void setFileCount(int max);

	public void setIntegrationRatios(String value);

	public void setIRMode(IRMode iMode);

	public void setLoaded(String fileName, String filePath);

	public void setMenuEnables(JSVPanelNode node, boolean isSplit);

	public void setNode(JSVPanelNode node, boolean fromTree);

	public JSVPanelNode setOverlayVisibility(JSVPanelNode node);

	public void setPropertiesFromPreferences(JSVPanel jsvp, boolean b);

	public void setRecentURL(String filePath);

	public void setReturnFromJmolModel(String model);

	public void setSelectedPanel(JSVPanel jsvp);

	public JSVPanel setSpectrum(String value);

	public void showProperties();

	public void syncLoad(String fileName);

	public void syncToJmol(String value);

	public void updateBoolean(ScriptToken st, boolean TF);

	public void updateRecentMenus(String filePath);

	public void validateAndRepaint();

	public void writeStatus(String msg);

	public void setLoadImaginary(boolean TF);

	public void setProperty(String key, String value);

	public boolean isSigned();

	public String getFileAsString(String value);

}
