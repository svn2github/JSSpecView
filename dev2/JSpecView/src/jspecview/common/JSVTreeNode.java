/**
 * 
 */
package jspecview.common;

import java.awt.Cursor;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import jspecview.common.JSVSpecNode;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.source.JDXSource;
import jspecview.util.FileManager;
import jspecview.util.Logger;
import jspecview.util.Parser;
import jspecview.util.TextFormat;

public class JSVTreeNode extends DefaultMutableTreeNode {

  public final static int FILE_OPEN_OK = 0;
  public final static int FILE_OPEN_ALREADY = -1;
  //private final static int FILE_OPEN_URLERROR = -2;
  public final static int FILE_OPEN_ERROR = -3;
  public final static int FILE_OPEN_NO_DATA = -4;

  private static final long serialVersionUID = 1L;
	
  public static final int OVERLAY_DIALOG = -1;
	public static final int OVERLAY_OFFSET = 99;
	
  public JSVSpecNode specNode;
	public int index;

  public JSVTreeNode(String text, JSVSpecNode specNode) {
    super(text);
    this.specNode = specNode;
  }

  @SuppressWarnings("unchecked")
	public static void closeSource(ScriptInterface si,
			JDXSource source) {
    // Remove nodes and dispose of frames
		JSVTreeNode rootNode = (JSVTreeNode) si.getRootNode();
		List<JSVSpecNode> specNodes = si.getSpecNodes();
		DefaultTreeModel spectraTreeModel = (DefaultTreeModel) si.getDefaultTreeModel();

    String fileName = (source == null ? null : source.getFilePath());
    List<JSVTreeNode> toDelete = new ArrayList<JSVTreeNode>();
    Enumeration<JSVTreeNode> enume = rootNode.children();
    while (enume.hasMoreElements()) {
      JSVTreeNode node = enume.nextElement();
      if (fileName == null
          || node.specNode.source.getFilePath().equals(fileName)) {
        for (Enumeration<JSVTreeNode> e = node.children(); e.hasMoreElements();) {
          JSVTreeNode childNode = e.nextElement();
          toDelete.add(childNode);
          specNodes.remove(childNode.specNode);
        }
        toDelete.add(node);
        if (fileName != null)
          break;
      }
    }
    for (int i = 0; i < toDelete.size(); i++)
      spectraTreeModel.removeNodeFromParent(toDelete.get(i));

    if (source == null) {
      JDXSource currentSource = si.getCurrentSource();
      //jsvpPopupMenu.dispose();
      if (currentSource != null)
        currentSource.dispose();
      //jsvpPopupMenu.dispose();
      if (si.getSelectedPanel() != null)
        si.getSelectedPanel().dispose();
    } else {
      setSpectrumNumberAndTreeNode(si, specNodes.size());
    }
    
    si.setSelectedPanel(null);
    si.setCurrentSource(null);

    int max = 0;
    for (int i = 0; i < specNodes.size(); i++) {
      float f = Parser.parseFloat(specNodes.get(i).id);
      if (f >= max + 1)
        max = (int) Math.floor(f);
    }
    si.setFileCount(max);
    System.gc();
    Logger.checkMemory();
	}

	public static void setSpectrumNumberAndTreeNode(ScriptInterface si, int n) {
    setFrameAndTreeNode(si, n - 1);
	}

	public static void setFrameAndTreeNode(ScriptInterface si, int i) {
    List<JSVSpecNode> specNodes = si.getSpecNodes();
		if (specNodes  == null || i < 0 || i >= specNodes.size())
      return;
    si.setNode(specNodes.get(i), false);
	}

	public static JSVSpecNode selectFrameNode(ScriptInterface si, JSVPanel jsvp) {
    // Find Node in SpectraTree and select it
    JSVSpecNode node = JSVSpecNode.findNode(jsvp, si.getSpecNodes());
    if (node == null)
      return null;

    JTree spectraTree = (JTree) si.getSpectraTree();
    spectraTree.setSelectionPath(new TreePath(((JSVTreeNode) node.treeNode)
        .getPath()));
    return si.setOverlayVisibility(node);
	}
	
	public static JSVPanel setSpectrum(ScriptInterface si,
			String value) {
    if (value.indexOf('.') >= 0) {
      JSVSpecNode node = JSVSpecNode.findNodeById(value, si.getSpecNodes());
      if (node == null)
        return null;
      si.setNode(node, false);
    } else {
      setSpectrumNumberAndTreeNode(si, Integer.parseInt(value));
    }
    return si.getSelectedPanel();
	}

	public static JSVTreeNode createTree(ScriptInterface si,
			JDXSource source, JSVPanel[] panels) {
		
    DefaultTreeModel spectraTreeModel = (DefaultTreeModel) si.getDefaultTreeModel();
		JSVTreeNode rootNode = (JSVTreeNode) si.getRootNode();
    List<JSVSpecNode> specNodes = si.getSpecNodes();
    JTree spectraTree = (JTree) si.getSpectraTree();

    String fileName = FileManager.getName(source.getFilePath());
    JSVSpecNode specNode = new JSVSpecNode(null, fileName, source, null);
    JSVTreeNode fileNode = new JSVTreeNode(fileName, specNode);
    specNode.setTreeNode(fileNode);
		spectraTreeModel.insertNodeInto(fileNode, rootNode, rootNode
        .getChildCount());
		spectraTree.scrollPathToVisible(new TreePath(fileNode.getPath()));

		int fileCount = si.getFileCount() + 1;
    si.setFileCount(fileCount);
    for (int i = 0; i < panels.length; i++) {
      JSVPanel jsvp = panels[i];
      String id = fileCount + "." + (i + 1);
      specNode = si.getNewSpecNode(id, fileName, source, jsvp);
      JSVTreeNode treeNode = new JSVTreeNode(specNode.toString(), specNode);
      specNode.setTreeNode(treeNode);
			specNodes.add(specNode);
      spectraTreeModel.insertNodeInto(treeNode, fileNode, fileNode
          .getChildCount());
      spectraTree.scrollPathToVisible(new TreePath(treeNode.getPath()));
    }
    selectFrameNode(si, panels[0]);
    return fileNode;
	}

	public static void close(ScriptInterface si, String value) {
		if (value == null || value.equalsIgnoreCase("all") || value.equals("*")) {
			si.closeSource(null);
			return;
		}
		List<JSVSpecNode> specNodes = si.getSpecNodes();
		value = value.replace('\\', '/');
		if (value.endsWith("*")) {
			value = value.substring(0, value.length() - 1);
			for (int i = specNodes.size(); --i >= 0;)
				if (i < specNodes.size() && specNodes.get(i).fileName.startsWith(value))
					si.closeSource(specNodes.get(i).source);
		} else if (value.equals("selected")) {
			List<JDXSource> list = new ArrayList<JDXSource>();
			JDXSource lastSource = null;
			for (int i = specNodes.size(); --i >= 0;) {
				JDXSource source = specNodes.get(i).source;
				if (specNodes.get(i).isSelected 
						&& (lastSource == null || lastSource != source))
					list.add(source);
				lastSource = source;
			}
			for (int i = list.size(); --i >= 0;)
				si.closeSource(list.get(i));
		} else {
			JDXSource source = (value.length() == 0 ? si.getCurrentSource()
					: JSVSpecNode.findSourceByNameOrId(value, specNodes));
			if (source == null)
				return;
			si.closeSource(source);
		}
		if (si.getSelectedPanel() == null && specNodes.size() > 0)
			si.setSelectedPanel(JSVSpecNode.getLastFileFirstNode(specNodes));
	}

	public static void load(ScriptInterface si, String value) {
    List<String> tokens = ScriptToken.getTokens(value);
    String filename = tokens.get(0);
    int pt = 0;
    if (filename.equalsIgnoreCase("APPEND")) {
      filename = tokens.get(++pt);
    } else {
      if (filename.equals("\"\"") && si.getCurrentSource() != null)
        filename = si.getCurrentSource().getFilePath();
      close(si, "all");
    }
    filename = TextFormat.trimQuotes(filename);
    int firstSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
        : -1);
    int lastSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
        : firstSpec);
    si.openDataOrFile(null, null, null, filename, firstSpec, lastSpec);
	}

	public static int openDataOrFile(ScriptInterface si,
			String data, String name, List<JDXSpectrum> specs, String url,
			int firstSpec, int lastSpec) {
		if ("NONE".equals(name)) {
			close(si, "Overlay*");
			return FILE_OPEN_OK;
		}
    si.writeStatus("");
    String filePath = null;
    String fileName = null;
    File file = null;
		URL base = null;
    boolean isOverlay = false;
    if (data != null) {
    } else if (specs != null) {
      isOverlay = true;
      fileName = filePath = "Overlay" + si.incrementOverlay(1);
    } else if (url != null) {
      try {
      	base = si.getDocumentBase();
        URL u = (base == null ? new URL(url) : new URL(base, url));
        filePath = u.toString();
        si.setRecentURL(filePath);
        fileName = FileManager.getName(url);
        if (base != null)
        	si.setRecentFileName(fileName);
      } catch (MalformedURLException e) {
        file = new File(url);
      }
    }
    if (file != null) {
      fileName = file.getName();
      si.setRecentFileName(fileName);
      filePath = file.getAbsolutePath();
      //recentJmolName = (url == null ? filePath.replace('\\', '/') : url);
      si.setRecentURL(null);
    }
    // TODO could check here for already-open overlay 
    if (JSVSpecNode.isOpen(si.getSpecNodes(), filePath) || JSVSpecNode.isOpen(si.getSpecNodes(), url)) {
      si.writeStatus(filePath + " is already open");
      if (isOverlay)
      	 si.incrementOverlay(-1);
      return FILE_OPEN_ALREADY;
    }
    si.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    try {
      si.setCurrentSource(isOverlay ? JDXSource.createOverlay(specs)
          : si.createSource(data, filePath, base, firstSpec, lastSpec));
    } catch (Exception e) {
      e.printStackTrace();
      si.writeStatus(e.getMessage());
      si.setCursor(Cursor.getDefaultCursor());
      return FILE_OPEN_ERROR;
    }
    si.setCursor(Cursor.getDefaultCursor());
    System.gc();
    JDXSource currentSource = si.getCurrentSource();
    currentSource.setFilePath(filePath);
    JDXSpectrum spec = si.getCurrentSource().getJDXSpectrum(0);

    si.setLoaded(fileName, filePath);

    if (spec == null) {
      return FILE_OPEN_NO_DATA;
    }

    specs = currentSource.getSpectra();
    si.process(specs);
    
    boolean autoOverlay = si.getAutoOverlay() || spec.isAutoOverlayFromJmolClick();

    
  //boolean isOverlaid = (isOverlay && !name.equals("NONE"));
    
    boolean overlay = isOverlay || autoOverlay
        && currentSource.isCompoundSource;
    if (overlay) {
      overlay(si, (isOverlay ? url : null));
    } else {
      splitSpectra(si);
    }
    if (!isOverlay)
      si.updateRecentMenus(filePath);
    return FILE_OPEN_OK;
	}

  /**
   * Overlays the spectra of the specified <code>JDXSource</code>
   * 
   * @param source
   *        the <code>JDXSource</code>
   * @throws ScalesIncompatibleException
   */
  private static void overlay(ScriptInterface si, String name) {
  	JDXSource source = si.getCurrentSource();
    List<JDXSpectrum> specs = source.getSpectra();
    JSVPanel jsvp = si.getNewJSVPanel(specs);
    jsvp.setTitle(source.getTitle());
    if (jsvp.getTitle().equals(""))
    	jsvp.setTitle(name);
    si.setPropertiesFromPreferences(jsvp, true);
    createTree(si, source, new JSVPanel[] { jsvp }).specNode.isOverlay = true;
    JSVSpecNode node = JSVSpecNode.findNode(si.getSelectedPanel(), si.getSpecNodes());
    node.setFrameTitle(name);
    node.isOverlay = true;
    if (si.getAutoShowLegend()
        && si.getSelectedPanel().getPanelData().getNumberOfGraphSets() == 1)
      node.setLegend(si.getOverlayLegend(jsvp));
    si.setMenuEnables(node, false);
  }

  /**
   * Displays the spectrum of the <code>JDXSource</code> specified by source in
   * separate windows
   * 
   * @param source
   *        the <code>JDXSource</code>
   */
  private static void splitSpectra(ScriptInterface si) {
  	JDXSource source = si.getCurrentSource();
    List<JDXSpectrum> specs = source.getSpectra();
    JSVPanel[] panels = new JSVPanel[specs.size()];
    JSVPanel jsvp = null;
    for (int i = 0; i < specs.size(); i++) {
      JDXSpectrum spec = specs.get(i);
      jsvp = si.getNewJSVPanel(spec);
      si.setPropertiesFromPreferences(jsvp, true);
      panels[i] = jsvp;
    }
    // arrange windows in ascending order
    createTree(si, source, panels);
    si.getNewJSVPanel((JDXSpectrum) null); // end of operation
    JSVSpecNode node = JSVSpecNode.findNode(si.getSelectedPanel(), si.getSpecNodes());
    si.setMenuEnables(node, true);
  }

}