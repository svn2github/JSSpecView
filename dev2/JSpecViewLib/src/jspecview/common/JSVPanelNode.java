/**
 * 
 */
package jspecview.common;

import java.util.List;
import java.util.Map;

import org.jmol.util.SB;

import jspecview.common.JSVDialog;
import jspecview.common.JSVPanel;
import jspecview.source.JDXSource;

public class JSVPanelNode {

	public JSVPanelNode(String id, String fileName, JDXSource source, JSVPanel jsvp) {
    this.id = id;
    this.source = source;
    this.fileName = fileName;
    this.jsvp = jsvp;
    if (jsvp != null) {
      jsvp.getPanelData().getSpectrumAt(0).setId(id);
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
	public boolean isView;
  public String frameTitle;
  

  public void dispose() {
    source.dispose();
    if (jsvp != null)
    	jsvp.dispose();
    source = null;
    jsvp = null;
    legend = null;
  }
  
  public JDXSpectrum getSpectrum() {
    return jsvp.getPanelData().getSpectrum();
  }

  JSVDialog setLegend(JSVDialog legend) {
    if (this.legend != null)
      this.legend.dispose();
    this.legend = legend;
    return legend;
  }

  @Override
  public String toString() {
    return ((id == null ? "" : id + ": ") + (frameTitle == null ? fileName : frameTitle));
  }
  public static JDXSource findSourceByNameOrId(String id, List<JSVPanelNode> panelNodes) {
    for (int i = panelNodes.size(); --i >= 0;) {
      JSVPanelNode node = panelNodes.get(i);
      if (id.equals(node.id) || id.equalsIgnoreCase(node.source.getFilePath()))
        return node.source;
    }
    // only if that doesn't work -- check file name for exact case
    for (int i = panelNodes.size(); --i >= 0;) {
      JSVPanelNode node = panelNodes.get(i);
      if (id.equals(node.fileName))
        return node.source;
    }
    return null;
  }
  
  static JSVPanelNode findNodeById(String id, List<JSVPanelNode> panelNodes) {
    for (int i = panelNodes.size(); --i >= 0;)
      if (id.equals(panelNodes.get(i).id))
        return panelNodes.get(i);
    return null;
  }

  /**
   * Returns the tree node that is associated with a panel
   * @param jsvp 
   * @param panelNodes 
   * 
   * @return the tree node that is associated with a panel
   */
  static JSVPanelNode findNode(JSVPanel jsvp, List<JSVPanelNode> panelNodes) {
    for (int i = panelNodes.size(); --i >= 0;)
      if (panelNodes.get(i).jsvp == jsvp)
        return panelNodes.get(i);
    return null;
  }

	static String getSpectrumListAsString(List<JSVPanelNode> panelNodes) {
      SB sb = new SB();
      for (int i = 0; i < panelNodes.size(); i++) {
      	String id = panelNodes.get(i).id;
        sb.append(" ").append(id);
      }
      return sb.toString().trim();
  }
  
//  static List<JDXSpectrum> getSpecList(List<JSVPanelNode> panelNodes) {
//    if (panelNodes == null || panelNodes.size() == 0)
//      return null;
//    List<JDXSpectrum> specList = new ArrayList<JDXSpectrum>();
//    for (int i = 0; i < panelNodes.size(); i++)
//      specList.add(panelNodes.get(i).getSpectrum());
//    return specList;
//  }
 
  static boolean isOpen(List<JSVPanelNode> panelNodes, String filePath) {
    if (filePath != null)
      for (int i = panelNodes.size(); --i >= 0;) {
      	//System.out.println("JSVPanelNode " + filePath + " " + panelNodes.get(i).source.getFilePath());
        if (filePath.equals(panelNodes.get(i).source.getFilePath())
        		|| filePath.equals(panelNodes.get(i).frameTitle))
          return true;
      }
    return false;
  }
  
//  static int getNodeIndex(List<JSVPanelNode> panelNodes, JSVPanelNode node) {
//    for (int i = panelNodes.size(); --i >= 0;)
//      if (node == panelNodes.get(i))
//        return i;
//    return -1;
//  }
  
	void setFrameTitle(String name) {
		frameTitle = name;
	}
	static JSVPanel getLastFileFirstNode(List<JSVPanelNode> panelNodes) {
		int n = panelNodes.size();
		JSVPanelNode node = (n == 0 ? null : panelNodes.get(n - 1));
		// first in last file
		for (int i = n - 1; --i >= 0; ) {
			if (panelNodes.get(i).source != node.source)
				break;
			node = panelNodes.get(i);
		}
		return (node == null ? null : node.jsvp);
	}
	
  Map<String, Object> getInfo(String key) {
    Map<String, Object> info = jsvp.getPanelData().getInfo(false, key);
		Parameters.putInfo(key, info, "panelId", id);
		Parameters.putInfo(key, info, "panelFileName", fileName);
		Parameters.putInfo(key, info, "panelSource", source.getFilePath());
	  return info;
	}
	
}