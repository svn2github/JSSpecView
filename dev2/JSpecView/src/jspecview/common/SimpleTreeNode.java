package jspecview.common;

import java.util.Enumeration;

import org.jmol.util.JmolList;

import jspecview.api.JSVTreeNode;
import jspecview.common.JSVPanelNode;

public class SimpleTreeNode implements JSVTreeNode {


	  private static final long serialVersionUID = 1L;
		
	  public JSVPanelNode panelNode;
		public int index;
	  SimpleTreeNode prevNode;
	  JmolList<SimpleTreeNode> children;

		private String text;

	  public SimpleTreeNode(String text, JSVPanelNode panelNode) {
	  	this.text = text;
	    this.panelNode = panelNode;
	    children = new JmolList<SimpleTreeNode>();
	   // System.out.println("adding " + text);
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

		public Enumeration<JSVTreeNode> children() {
			return new SimpleTreeEnumeration(this);
		}

		public int getChildCount() {
			return children.size();
		}

		public Object[] getPath() {
			JmolList<Object> o = new JmolList<Object>();
			SimpleTreeNode node = this;
			o.addLast(node);
			while ((node = node.prevNode) != null)
				o.add(0, node);
			return o.toArray();//new Object[o.size()]);
		}

		public boolean isLeaf() {
			return (prevNode != null && prevNode.prevNode != null);
		}

		@Override
		public String toString() {
			return text;
		}

}
