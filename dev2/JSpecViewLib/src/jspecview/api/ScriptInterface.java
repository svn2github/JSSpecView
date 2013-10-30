package jspecview.api;

import javajs.util.List;

import jspecview.common.JDXSpectrum;
import jspecview.common.PanelNode;
import jspecview.common.JSViewer;
import jspecview.common.ScriptToken;
import jspecview.source.JDXSource;

public interface ScriptInterface {

	// java.awt.Component methods
	public void repaint();
	public void setCursor(int id);

	// from JSVAppletInterface or JSVInterface
	
	public boolean isSigned();
	public void runScript(String script);
	public boolean runScriptNow(String script);
	public void syncToJmol(String value);
	public void writeStatus(String msg);
	
	// JSpecView methods
	public void siCheckCallbacks(String title);

	public void siCloseSource(JDXSource source);

	public JDXSource siCreateSource(String data, String filePath, 
			int firstSpec, int lastSpec) throws Exception;

	public JSVTreeNode siCreateTree(JDXSource source, JSVPanel[] jsvPanels);

	public void siExecClose(String value);

	public void siExecHidden(boolean b);

	public String siExecLoad(String value);

	public void siExecScriptComplete(String msg, boolean isOK);

	public void siExecSetAutoIntegrate(boolean b);

	public void siExecSetCallback(ScriptToken st, String value);

	public void siExecSetInterface(String value);

	public void siExecTest(String value);

	public boolean siGetAutoCombine();

	public boolean siGetAutoShowLegend();

	public int siGetFileCount();

	public String siGetIntegrationRatios();

	public JSVPanel siGetNewJSVPanel(JDXSpectrum spec);

	public JSVPanel siGetNewJSVPanel2(List<JDXSpectrum> specs);

	public PanelNode siGetNewPanelNode(String id, String fileName,
			JDXSource source, JSVPanel jsvp);

	public String siGetReturnFromJmolModel();

	public JSViewer siGetViewer();

	public int siIncrementViewCount(int i);

	public int siIncrementScriptLevelCount(int i);

	public void siOpenDataOrFile(String data, String name, List<JDXSpectrum> specs,
			String url, int firstSpec, int lastSpec, boolean doCheck);

	public void siProcessCommand(String script);
	
	public void siSendPanelChange(JSVPanel jsvp);

	public void siSetCurrentSource(JDXSource source);

	public String siSetFileAsString(String value);

	public void siSetFileCount(int max);

	public void siSetIntegrationRatios(String value);

	public void siSetLoaded(String fileName, String filePath);

	public void siSetLoadImaginary(boolean TF);

	public void siSetMenuEnables(PanelNode node, boolean isSplit);

	public void siSetNode(PanelNode node, boolean fromTree);

	public void siSetPropertiesFromPreferences(JSVPanel jsvp, boolean b);

	public void siSetRecentURL(String filePath);

	public void siSetReturnFromJmolModel(String model);

	public void siSetSelectedPanel(JSVPanel jsvp);

	public void siSyncLoad(String fileName);

	public void siUpdateBoolean(ScriptToken st, boolean TF);

	public void siUpdateRecentMenus(String filePath);

	public void siValidateAndRepaint();
	
	public void siNewWindow(boolean isSelected, boolean fromFrame);
	
}
