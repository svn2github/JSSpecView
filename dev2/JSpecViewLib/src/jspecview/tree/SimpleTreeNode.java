package jspecview.tree;

import java.util.Enumeration;

import javajs.util.List;


import jspecview.api.JSVTreeNode;
import jspecview.common.PanelNode;

public class SimpleTreeNode implements JSVTreeNode {


	  public PanelNode panelNode;
		public int index;
	  SimpleTreeNode prevNode;
	  List<SimpleTreeNode> children;

		private String text;

	  public SimpleTreeNode(String text, PanelNode panelNode) {
	  	this.text = text;
	    this.panelNode = panelNode;
	    children = new List<SimpleTreeNode>();
	   // System.out.println("adding " + text);
	  }

		public PanelNode getPanelNode() {
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
			List<Object> o = new List<Object>();
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
