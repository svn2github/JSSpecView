package jspecview.java;

import org.jmol.util.JmolList;
import java.util.Enumeration;


import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jmol.util.Logger;
import org.jmol.util.Parser;

import jspecview.api.JSVTree;
import jspecview.api.JSVTreeNode;
import jspecview.api.ScriptInterface;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVPanel;
import jspecview.common.JSVPanelNode;
import jspecview.source.JDXSource;
import jspecview.util.JSVFileManager;

public class AwtTree extends JTree implements JSVTree {

  private static final long serialVersionUID = 1L;
	protected ScriptInterface si;
  private JSVTreeNode rootNode;
  private DefaultTreeModel spectraTreeModel;

	public JSVTreeNode getRootNode() {
		return rootNode;
	}

	public DefaultTreeModel getDefaultModel() {
		return spectraTreeModel;
	}

	public AwtTree(final ScriptInterface si) {
		super();
		this.si = si;
    rootNode = new AwtTreeNode("Spectra", null);
    spectraTreeModel = new DefaultTreeModel((TreeNode) rootNode);
    setModel(spectraTreeModel);
    getSelectionModel().setSelectionMode(
        TreeSelectionModel.SINGLE_TREE_SELECTION);
    addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        JSVTreeNode node = (JSVTreeNode) getLastSelectedPathComponent();
       if (node == null) {
          return;
        }
        if (node.isLeaf()) {
          si.setNode(node.getPanelNode(), true);
        }
        si.setCurrentSource(node.getPanelNode().source);
      }
    });
    setRootVisible(false);

	}
	
	public void setSelectedPanel(ScriptInterface si, JSVPanel jsvp) {
		if (jsvp != null) {
			JSVTreeNode treeNode = (JSVTreeNode) JSVPanelNode.findNode(jsvp, si
					.getPanelNodes()).treeNode;
			scrollPathToVisible(new TreePath(treeNode.getPath()));
			setSelectionPath(new TreePath(treeNode.getPath()));
		}
	}
	
	public static void closeSource(ScriptInterface si,
			JDXSource source) {
    // Remove nodes and dispose of frames
		JmolList<JSVPanelNode> panelNodes = si.getPanelNodes();
  	AwtTree tree = (AwtTree) si.getSpectraTree();
		JSVTreeNode rootNode = tree.getRootNode();
		DefaultTreeModel spectraTreeModel = tree.getDefaultModel();

    String fileName = (source == null ? null : source.getFilePath());
    JmolList<JSVTreeNode> toDelete = new JmolList<JSVTreeNode>();
    Enumeration<JSVTreeNode> enume = rootNode.children();
    while (enume.hasMoreElements()) {
      JSVTreeNode node = enume.nextElement();
      if (fileName == null
          || node.getPanelNode().source.getFilePath().equals(fileName)) {
        for (Enumeration<JSVTreeNode> e = node.children(); e.hasMoreElements();) {
          JSVTreeNode childNode = e.nextElement();
          toDelete.addLast(childNode);
          panelNodes.remove(childNode.getPanelNode());
        }
        toDelete.addLast(node);
        if (fileName != null)
          break;
      }
    }
    for (int i = 0; i < toDelete.size(); i++) {
      spectraTreeModel.removeNodeFromParent((MutableTreeNode) toDelete.get(i));
    }

    if (source == null) {
      JDXSource currentSource = si.getCurrentSource();
      //jsvpPopupMenu.dispose();
      if (currentSource != null)
        currentSource.dispose();
      //jsvpPopupMenu.dispose();
      if (si.getSelectedPanel() != null)
        si.getSelectedPanel().dispose();
    } else {
      //setFrameAndTreeNode(si, panelNodes.size() - 1);
    }
    
    if(si.getCurrentSource() == source) {
      si.setSelectedPanel(null);
      si.setCurrentSource(null);
    }

    int max = 0;
    for (int i = 0; i < panelNodes.size(); i++) {
      float f = Parser.parseFloat(panelNodes.get(i).id);
      if (f >= max + 1)
        max = (int) Math.floor(f);
    }
    si.setFileCount(max);
    System.gc();
    Logger.checkMemory();
	}

	public static void setFrameAndTreeNode(ScriptInterface si, int i) {
    JmolList<JSVPanelNode> panelNodes = si.getPanelNodes();
		if (panelNodes  == null || i < 0 || i >= panelNodes.size())
      return;
    si.setNode(panelNodes.get(i), false);
	}

	public static JSVPanelNode selectFrameNode(ScriptInterface si, JSVPanel jsvp) {
    // Find Node in SpectraTree and select it
    JSVPanelNode node = JSVPanelNode.findNode(jsvp, si.getPanelNodes());
    if (node == null)
      return null;

    JTree spectraTree = (JTree) si.getSpectraTree();
    spectraTree.setSelectionPath(new TreePath(((JSVTreeNode) node.treeNode)
        .getPath()));
    return si.setOverlayVisibility(node);
	}
	
	public static JSVPanel setSpectrum(ScriptInterface si, String value) {
		if (value.indexOf('.') >= 0) {
			JSVPanelNode node = JSVPanelNode.findNodeById(value, si.getPanelNodes());
			if (node == null)
				return null;
			si.setNode(node, false);
		} else {
			int n = Parser.parseInt(value);
			if (n <= 0) {
				si.checkOverlay();
				return null;
			}
      setFrameAndTreeNode(si, n - 1);
		}
		return si.getSelectedPanel();
	}

	public static JSVTreeNode createTree(ScriptInterface si,
			JDXSource source, JSVPanel[] panels) {

  	AwtTree tree = (AwtTree) si.getSpectraTree();
		JSVTreeNode rootNode = tree.getRootNode();
		DefaultTreeModel spectraTreeModel = tree.getDefaultModel();
    JmolList<JSVPanelNode> panelNodes = si.getPanelNodes();

    String fileName = JSVFileManager.getName(source.getFilePath());
    JSVPanelNode panelNode = new JSVPanelNode(null, fileName, source, null);
    JSVTreeNode fileNode = new AwtTreeNode(fileName, panelNode);
    panelNode.setTreeNode(fileNode);
		spectraTreeModel.insertNodeInto((MutableTreeNode) fileNode, (MutableTreeNode) rootNode, rootNode
        .getChildCount());
		tree.scrollPathToVisible(new TreePath(fileNode.getPath()));

		int fileCount = si.getFileCount() + 1;
    si.setFileCount(fileCount);
    for (int i = 0; i < panels.length; i++) {
      JSVPanel jsvp = panels[i];
      String id = fileCount + "." + (i + 1);
      panelNode = si.getNewPanelNode(id, fileName, source, jsvp);
      JSVTreeNode treeNode = new AwtTreeNode(panelNode.toString(), panelNode);
      panelNode.setTreeNode(treeNode);
			panelNodes.addLast(panelNode);
      spectraTreeModel.insertNodeInto((MutableTreeNode) treeNode, (MutableTreeNode) fileNode, fileNode
          .getChildCount());
      tree.scrollPathToVisible(new TreePath(treeNode.getPath()));
    }
    selectFrameNode(si, panels[0]);
    return fileNode;
	}

	public static void splitSpectra(ScriptInterface si) {
  	JDXSource source = si.getCurrentSource();
    JmolList<JDXSpectrum> specs = source.getSpectra();
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
    JSVPanelNode node = JSVPanelNode.findNode(si.getSelectedPanel(), si.getPanelNodes());
    si.setMenuEnables(node, true);
  }

}
