package jspecview.tree;

import org.jmol.util.JmolList;

import jspecview.api.JSVPanel;
import jspecview.api.JSVTree;
import jspecview.api.JSVTreeNode;
import jspecview.api.JSVTreePath;
import jspecview.api.ScriptInterface;
import jspecview.common.JSVFileManager;
import jspecview.common.JSVPanelNode;
import jspecview.common.JSViewer;
import jspecview.source.JDXSource;

public class SimpleTree implements JSVTree {

  private static final long serialVersionUID = 1L;
	protected ScriptInterface si;
  private JSVTreeNode rootNode;
  private SimpleTreeModel spectraTreeModel;
	protected JSViewer viewer;
	private SimpleTreePath selectedPath;

	public JSVTreeNode getRootNode() {
		return rootNode;
	}

	public SimpleTree(JSViewer viewer) {
		this.viewer = viewer;
    rootNode = new SimpleTreeNode("Spectra", null);
    spectraTreeModel = new SimpleTreeModel(rootNode);
	}
	
  public void valueChanged() {
  	viewer.selectedTreeNode(getLastSelectedPathComponent());
  }

  private JSVTreeNode getLastSelectedPathComponent() {
  	return (JSVTreeNode) (selectedPath == null ? null : selectedPath.getLastPathComponent());
	}

	public void setSelectedPanel(ScriptInterface si, JSVPanel jsvp) {
		if (jsvp != null) {
			JSVTreeNode treeNode = JSVPanelNode.findNode(jsvp, viewer.panelNodes).treeNode;
			setSelectionPath(viewer.spectraTree.newTreePath(treeNode.getPath()));
		}
	}
	
	private void setSelectionPath(JSVTreePath newTreePath) {
		selectedPath = (SimpleTreePath) newTreePath;
		valueChanged();
	}

	public JSVTreeNode createTree(ScriptInterface si,
			JDXSource source, JSVPanel[] panels) {
  	SimpleTree tree = (SimpleTree) viewer.spectraTree;
		JSVTreeNode rootNode = tree.getRootNode();
    JmolList<JSVPanelNode> panelNodes = viewer.panelNodes;

    String fileName = JSVFileManager.getName(source.getFilePath());
    JSVPanelNode panelNode = new JSVPanelNode(null, fileName, source, null);
    JSVTreeNode fileNode = new SimpleTreeNode(fileName, panelNode);
    panelNode.setTreeNode(fileNode);
		tree.spectraTreeModel.insertNodeInto(fileNode, rootNode, rootNode
        .getChildCount());
		//tree.scrollPathToVisible(new JsTreePath(fileNode.getPath()));

		int fileCount = si.siGetFileCount() + 1;
    si.siSetFileCount(fileCount);
    for (int i = 0; i < panels.length; i++) {
      JSVPanel jsvp = panels[i];
      String id = fileCount + "." + (i + 1);
      panelNode = si.siGetNewPanelNode(id, fileName, source, jsvp);
      JSVTreeNode treeNode = new SimpleTreeNode(panelNode.toString(), panelNode);
      panelNode.setTreeNode(treeNode);
			panelNodes.addLast(panelNode);
      tree.spectraTreeModel.insertNodeInto(treeNode, fileNode, fileNode
          .getChildCount());
      //tree.scrollPathToVisible(new TreePath(treeNode.getPath()));
    }
    viewer.selectFrameNode(panels[0]);
    return fileNode;
	}

	public void setPath(JSVTreePath path) {
		setSelectionPath(path);
	}

	public JSVTreePath newTreePath(Object[] path) {
		return new SimpleTreePath(path);
	}

	public void deleteNodes(JmolList<JSVTreeNode> toDelete) {
	  for (int i = 0; i < toDelete.size(); i++) {
	  	spectraTreeModel.removeNodeFromParent(toDelete.get(i));
	  }
	
	}

}
