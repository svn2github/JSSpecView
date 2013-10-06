package jspecview.api;

import org.jmol.util.JmolList;

import jspecview.api.ScriptInterface;

public interface JSVTree {
	
	public void setSelectedPanel(ScriptInterface si, JSVPanel jsvp);

	public JSVTreeNode getRootNode();

	public void setPath(JSVTreePath newTreePath);

	public JSVTreePath newTreePath(Object[] path);

	public void deleteNodes(JmolList<JSVTreeNode> toDelete);

}
