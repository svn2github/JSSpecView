package jspecview.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import jspecview.api.ScriptInterface;
import jspecview.java.AwtTree;
import jspecview.source.JDXSource;
import jspecview.util.JSVFileManager;

import org.jmol.api.ApiPlatform;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.Txt;

public class Temp {

	public static int openDataOrFile(ScriptInterface si, String data,
			String name, JmolList<JDXSpectrum> specs, String url, int firstSpec,
			int lastSpec, boolean isAppend) {
		if ("NONE".equals(name)) {
			Temp.close(si, "View*");
			return Temp.FILE_OPEN_OK;
		}
		si.writeStatus("");
		String filePath = null;
		String newPath = null;
		String fileName = null;
		File file = null;
		URL base = null;
		boolean isView = false;
		if (data != null) {
		} else if (specs != null) {
			isView = true;
			newPath = fileName = filePath = "View" + si.incrementViewCount(1);
		} else if (url != null) {
			try {
				base = JSVFileManager.appletDocumentBase;
				URL u = (base == null ? new URL(url) : new URL(base, url));
				filePath = u.toString();
				si.setRecentURL(filePath);
				fileName = JSVFileManager.getName(url);
			} catch (MalformedURLException e) {
				file = new File(url);
			}
		}
		if (file != null) {
			fileName = file.getName();
			newPath = filePath = file.getAbsolutePath();
			// recentJmolName = (url == null ? filePath.replace('\\', '/') : url);
			si.setRecentURL(null);
		}
		// TODO could check here for already-open view
		if (!isView)
			if (JSVPanelNode.isOpen(si.getPanelNodes(), filePath)
					|| JSVPanelNode.isOpen(si.getPanelNodes(), url)) {
				si.writeStatus(filePath + " is already open");
				return Temp.FILE_OPEN_ALREADY;
			}
		if (!isAppend && !isView)
			Temp.close(si, "all"); // with CHECK we may still need to do this
		si.setCursor(ApiPlatform.CURSOR_WAIT);
		try {
			si.setCurrentSource(isView ? JDXSource.createView(specs) : si
					.createSource(data, filePath, base, firstSpec, lastSpec));
		} catch (Exception e) {
			Logger.error(e.getMessage());
			si.writeStatus(e.getMessage());
			si.setCursor(ApiPlatform.CURSOR_DEFAULT);
			return Temp.FILE_OPEN_ERROR;
		}
		si.setCursor(ApiPlatform.CURSOR_DEFAULT);
		System.gc();
		JDXSource currentSource = si.getCurrentSource();
		if (newPath == null) {
			newPath = currentSource.getFilePath();
			if (newPath != null)
				fileName = newPath.substring(newPath.lastIndexOf("/") + 1);
		} else {
			currentSource.setFilePath(newPath);
		}
		si.setLoaded(fileName, newPath);
	
		JDXSpectrum spec = si.getCurrentSource().getJDXSpectrum(0);
		if (spec == null) {
			return Temp.FILE_OPEN_NO_DATA;
		}
	
		specs = currentSource.getSpectra();
		JDXSpectrum.process(specs, si.getIRMode());
	
		boolean autoOverlay = si.getAutoCombine()
				|| spec.isAutoOverlayFromJmolClick();
	
		boolean combine = isView || autoOverlay && currentSource.isCompoundSource;
		if (combine) {
			Temp.combineSpectra(si, (isView ? url : null));
		} else {
			AwtTree.splitSpectra(si);
		}
		if (!isView)
			si.updateRecentMenus(filePath);
		return Temp.FILE_OPEN_OK;
	}

	public static void close(ScriptInterface si, String value) {
		if (value == null || value.equalsIgnoreCase("all") || value.equals("*")) {
			si.closeSource(null);
			return;
		}
		JmolList<JSVPanelNode> panelNodes = si.getPanelNodes();
		value = value.replace('\\', '/');
		if (value.endsWith("*")) {
			value = value.substring(0, value.length() - 1);
			for (int i = panelNodes.size(); --i >= 0;)
				if (i < panelNodes.size() && panelNodes.get(i).fileName.startsWith(value))
					si.closeSource(panelNodes.get(i).source);
		} else if (value.equals("selected")) {
			JmolList<JDXSource> list = new JmolList<JDXSource>();
			JDXSource lastSource = null;
			for (int i = panelNodes.size(); --i >= 0;) {
				JDXSource source = panelNodes.get(i).source;
				if (panelNodes.get(i).isSelected 
						&& (lastSource == null || lastSource != source))
					list.addLast(source);
				lastSource = source;
			}
			for (int i = list.size(); --i >= 0;)
				si.closeSource(list.get(i));
		} else {
			JDXSource source = (value.length() == 0 ? si.getCurrentSource()
					: JSVPanelNode.findSourceByNameOrId(value, panelNodes));
			if (source == null)
				return;
			si.closeSource(source);
		}
		if (si.getSelectedPanel() == null && panelNodes.size() > 0)
			si.setSelectedPanel(JSVPanelNode.getLastFileFirstNode(panelNodes));
	}

	public static void load(ScriptInterface si, String value) {
		JmolList<String> tokens = ScriptToken.getTokens(value);
		String filename = tokens.get(0);
		int pt = 0;
		boolean isAppend = filename.equalsIgnoreCase("APPEND");
		boolean isCheck = filename.equalsIgnoreCase("CHECK");
		if (isAppend || isCheck)
			filename = tokens.get(++pt);
		boolean isSimulation = filename.equalsIgnoreCase("MOL");
		if (isSimulation)
			filename = JSVFileManager.SIMULATION_PROTOCOL + "MOL="
					+ Txt.trimQuotes(tokens.get(++pt));
		if (!isCheck && !isAppend) {
			if (filename.equals("\"\"") && si.getCurrentSource() != null)
				filename = si.getCurrentSource().getFilePath();
			close(si, "all");
		}
		filename = Txt.trimQuotes(filename);
		if (filename.startsWith("$")) {
			isSimulation = true;
			filename = JSVFileManager.SIMULATION_PROTOCOL + filename;
		}
		int firstSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
				.intValue() : -1);
		int lastSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
				.intValue() : firstSpec);
		si
				.openDataOrFile(null, null, null, filename, firstSpec, lastSpec,
						isAppend);
	}

	public static void combineSpectra(ScriptInterface si, String name) {
		JDXSource source = si.getCurrentSource();
	  JmolList<JDXSpectrum> specs = source.getSpectra();
	  JSVPanel jsvp = si.getNewJSVPanel(specs);
	  jsvp.setTitle(source.getTitle());
	  if (jsvp.getTitle().equals("")) {
	    jsvp.getPanelData().setViewTitle(source.getFilePath());
	  	jsvp.setTitle(name);
	  }
	  si.setPropertiesFromPreferences(jsvp, true);
	  AwtTree.createTree(si, source, new JSVPanel[] { jsvp }).getPanelNode().isView = true;
	  JSVPanelNode node = JSVPanelNode.findNode(si.getSelectedPanel(), si.getPanelNodes());
	  node.setFrameTitle(name);
	  node.isView = true;
	  if (si.getAutoShowLegend()
	      && si.getSelectedPanel().getPanelData().getNumberOfGraphSets() == 1)
	    node.setLegend(si.getOverlayLegend(jsvp));
	  si.setMenuEnables(node, false);
	}

	public final static int FILE_OPEN_OK = 0;
	public final static int FILE_OPEN_ALREADY = -1;
	//private final static int FILE_OPEN_URLERROR = -2;
	public final static int FILE_OPEN_ERROR = -3;
	public final static int FILE_OPEN_NO_DATA = -4;
	public static final int OVERLAY_DIALOG = -1;
	public static final int OVERLAY_OFFSET = 99;
}
