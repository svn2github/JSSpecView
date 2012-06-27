package jspecview.common;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

public class JSVTree extends JTree {

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

	public JSVTree(final ScriptInterface si) {
		super();
		this.si = si;
    rootNode = new JSVTreeNode("Spectra", null);
    spectraTreeModel = new DefaultTreeModel(rootNode);
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
          si.setNode(node.panelNode, true);
        }
        si.setCurrentSource(node.panelNode.source);
      }
    });
    setRootVisible(false);

	}

}
