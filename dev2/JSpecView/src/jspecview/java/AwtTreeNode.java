package jspecview.java;

import javax.swing.tree.DefaultMutableTreeNode;

import jspecview.common.JSVPanelNode;
import jspecview.common.JSVTreeNode;

@SuppressWarnings("unchecked")
public class AwtTreeNode extends
	DefaultMutableTreeNode implements JSVTreeNode {


	  private static final long serialVersionUID = 1L;
		
	  public JSVPanelNode panelNode;
		public int index;

	  public AwtTreeNode(String text, JSVPanelNode panelNode) {
	    super(text);
	    this.panelNode = panelNode;
	  }

		public JSVPanelNode getPanelNode() {
			return panelNode;
		}
		
		public int getIndex() {
			return index;
		}
		
		public void setIndex(int index) {
			this.index = index;
		}


}