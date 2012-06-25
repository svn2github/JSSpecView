/**
 * 
 */
package jspecview.common;

import java.util.ArrayList;
import java.util.List;

import jspecview.common.JSVDialog;
import jspecview.common.JSVPanel;
import jspecview.source.JDXSource;

public class JSVSpecNode {

  private String frameTitle;
	public JSVSpecNode(String id, String fileName, JDXSource source, JSVPanel jsvp) {
    this.id = id;
    this.source = source;
    this.fileName = fileName;
    this.jsvp = jsvp;
    if (jsvp != null) {
      jsvp.getSpectrumAt(0).setId(id);
      frameTitle = jsvp.getTitle();
    }

  }

  public Object treeNode;
  public void setTreeNode(Object node) {
    treeNode = node;
  }
  public Object getTreeNode() {
    return treeNode;
  }
  
  public JDXSource source;
  public String fileName;
  public JSVPanel jsvp;
  public String id;
  public JSVDialog legend;
	public boolean isSelected;
	public boolean isOverlay;

  public void dispose() {
    source.dispose();
    jsvp.dispose();
    source = null;
    jsvp = null;
    legend = null;
  }
  
  public JDXSpectrum getSpectrum() {
    return jsvp.getSpectrum();
  }

  public JSVDialog setLegend(JSVDialog legend) {
    if (this.legend != null)
      this.legend.dispose();
    this.legend = legend;
    return legend;
  }

  @Override
  public String toString() {
    return ((id == null ? "" : id + ": ") + (frameTitle == null ? fileName : frameTitle));
  }
  public static JDXSource findSourceByNameOrId(String id, List<JSVSpecNode> specNodes) {
    for (int i = specNodes.size(); --i >= 0;) {
      JSVSpecNode node = specNodes.get(i);
      if (id.equals(node.id) || id.equalsIgnoreCase(node.source.getFilePath()))
        return node.source;
    }
    // only if that doesn't work -- check file name for exact case
    for (int i = specNodes.size(); --i >= 0;) {
      JSVSpecNode node = specNodes.get(i);
      if (id.equals(node.fileName))
        return node.source;
    }
    return null;
  }
  public static JSVSpecNode findNodeById(String id, List<JSVSpecNode> specNodes) {
    for (int i = specNodes.size(); --i >= 0;)
      if (id.equals(specNodes.get(i).id))
        return specNodes.get(i);
    return null;
  }

  /**
   * Returns the tree node that is associated with a panel
   * @param specNodes TODO
   * @param frame
   *        the JSVFrame
   * 
   * @return the tree node that is associated with a panel
   */
  public static JSVSpecNode findNode(JSVPanel jsvp, List<JSVSpecNode> specNodes) {
    for (int i = specNodes.size(); --i >= 0;)
      if (specNodes.get(i).jsvp == jsvp)
        return specNodes.get(i);
    return null;
  }

	public static String getSpectrumListAsString(List<JSVSpecNode> specNodes) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < specNodes.size(); i++)
        sb.append(" ").append(specNodes.get(i).id);
      return sb.toString().trim();
  }
  
  public static List<JDXSpectrum> getSpecList(List<JSVSpecNode> specNodes) {
    if (specNodes == null || specNodes.size() == 0)
      return null;
    List<JDXSpectrum> specList = new ArrayList<JDXSpectrum>();
    for (int i = 0; i < specNodes.size(); i++)
      specList.add(specNodes.get(i).getSpectrum());
    return specList;
  }
 
  public static boolean isOpen(List<JSVSpecNode> specNodes, String filePath) {
    if (filePath != null)
      for (int i = specNodes.size(); --i >= 0;)
        if (filePath.equals(specNodes.get(i).source.getFilePath())
        		|| filePath.equals(specNodes.get(i).frameTitle))
          return true;
    return false;
  }
  public static int getNodeIndex(List<JSVSpecNode> specNodes, JSVSpecNode node) {
    for (int i = specNodes.size(); --i >= 0;)
      if (node == specNodes.get(i))
        return i;
    return -1;
  }
  
	public void setFrameTitle(String name) {
		frameTitle = name;
	}
	public static JSVPanel getLastFileFirstNode(List<JSVSpecNode> specNodes) {
		int n = specNodes.size();
		JSVSpecNode node = (n == 0 ? null : specNodes.get(n - 1));
		// first in last file
		for (int i = specNodes.size(); --i >= 0; ) {
			if (specNodes.get(i).jsvp == null)
				break;
			node = specNodes.get(i);
		}
		return (node == null ? null : node.jsvp);
	}
	
}