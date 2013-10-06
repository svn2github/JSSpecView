package jspecview.java;

import org.jmol.util.JmolList;


import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;


import jspecview.api.JSVPanel;
import jspecview.api.JSVTree;
import jspecview.api.JSVTreeNode;
import jspecview.api.JSVTreePath;
import jspecview.api.ScriptInterface;
import jspecview.common.JSVPanelNode;
import jspecview.common.JSViewer;
import jspecview.source.JDXSource;
import jspecview.util.JSVFileManager;

public class AwtTree extends JTree implements JSVTree {

  private static final long serialVersionUID = 1L;
	protected ScriptInterface si;
  private JSVTreeNode rootNode;
  private DefaultTreeModel spectraTreeModel;
	protected JSViewer viewer;

	public JSVTreeNode getRootNode() {
		return rootNode;
	}

	public AwtTree(JSViewer viewer) {
		super();
		final JSViewer v = this.viewer = viewer;
    rootNode = new AwtTreeNode("Spectra", null);
    spectraTreeModel = new DefaultTreeModel((TreeNode) rootNode);
    setModel(spectraTreeModel);
    getSelectionModel().setSelectionMode(
        TreeSelectionModel.SINGLE_TREE_SELECTION);
    addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
      	v.selectedTreeNode((JSVTreeNode) getLastSelectedPathComponent());
      }
    });
    setRootVisible(false);

	}
	
	public void setSelectedPanel(ScriptInterface si, JSVPanel jsvp) {
		if (jsvp != null) {
			JSVTreeNode treeNode = JSVPanelNode.findNode(jsvp, si
					.getPanelNodes()).treeNode;
			JSVTree t = si.getSpectraTree();
			scrollPathToVisible((TreePath) t.newTreePath(treeNode.getPath()));
			setSelectionPath((TreePath) t.newTreePath(treeNode.getPath()));
		}
	}
	
	public JSVTreeNode createTree(ScriptInterface si,
			JDXSource source, JSVPanel[] panels) {
  	AwtTree tree = (AwtTree) si.getSpectraTree();
		JSVTreeNode rootNode = tree.getRootNode();
    JmolList<JSVPanelNode> panelNodes = si.getPanelNodes();

    String fileName = JSVFileManager.getName(source.getFilePath());
    JSVPanelNode panelNode = new JSVPanelNode(null, fileName, source, null);
    JSVTreeNode fileNode = new AwtTreeNode(fileName, panelNode);
    panelNode.setTreeNode(fileNode);
		tree.spectraTreeModel.insertNodeInto((MutableTreeNode) fileNode, (MutableTreeNode) rootNode, rootNode
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
      tree.spectraTreeModel.insertNodeInto((MutableTreeNode) treeNode, (MutableTreeNode) fileNode, fileNode
          .getChildCount());
      tree.scrollPathToVisible(new TreePath(treeNode.getPath()));
    }
    viewer.selectFrameNode(panels[0]);
    return fileNode;
	}

	public void setPath(JSVTreePath path) {
		setSelectionPath((TreePath) path);
	}

	public JSVTreePath newTreePath(Object[] path) {
		return new AwtTreePath(path);
	}

	public void deleteNodes(JmolList<JSVTreeNode> toDelete) {
	  for (int i = 0; i < toDelete.size(); i++) {
	  	spectraTreeModel.removeNodeFromParent((MutableTreeNode) toDelete.get(i));
	  }
	
	}

}
