/**
 * 
 */
package jspecview.api;

import java.util.Enumeration;

import jspecview.common.JSVPanelNode;

public interface JSVTreeNode {

	int getChildCount();

	Object[] getPath();

	boolean isLeaf();

	JSVPanelNode getPanelNode();

	Enumeration<JSVTreeNode> children();

	int getIndex();

	void setIndex(int index);

}