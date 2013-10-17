/**
 * 
 */
package jspecview.common;

import java.util.Map;

import javajs.lang.StringBuffer;

import org.jmol.util.JmolList;

import jspecview.api.JSVPanel;
import jspecview.api.JSVTreeNode;
import jspecview.dialog.JSVDialog;
import jspecview.source.JDXSource;
import jspecview.source.JDXSpectrum;

public class PanelNode {

	public PanelNode(String id, String fileName, JDXSource source, JSVPanel jsvp) {
    this.id = id;
    this.source = source;
    this.fileName = fileName;
    this.jsvp = jsvp;
    if (jsvp != null) {
      jsvp.getPanelData().getSpectrumAt(0).setId(id);
      frameTitle = jsvp.getTitle();
    }

  }

  public JSVTreeNode treeNode;
  public void setTreeNode(JSVTreeNode node) {
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
  public static JDXSource findSourceByNameOrId(String id, JmolList<PanelNode> panelNodes) {
    for (int i = panelNodes.size(); --i >= 0;) {
      PanelNode node = panelNodes.get(i);
      if (id.equals(node.id) || id.equalsIgnoreCase(node.source.getFilePath()))
        return node.source;
    }
    // only if that doesn't work -- check file name for exact case
    for (int i = panelNodes.size(); --i >= 0;) {
      PanelNode node = panelNodes.get(i);
      if (id.equals(node.fileName))
        return node.source;
    }
    return null;
  }
  
  public static PanelNode findNodeById(String id, JmolList<PanelNode> panelNodes) {
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
  static public PanelNode findNode(JSVPanel jsvp, JmolList<PanelNode> panelNodes) {
    for (int i = panelNodes.size(); --i >= 0;)
      if (panelNodes.get(i).jsvp == jsvp)
        return panelNodes.get(i);
    return null;
  }

	static public String getSpectrumListAsString(JmolList<PanelNode> panelNodes) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < panelNodes.size(); i++) {
      	String id = panelNodes.get(i).id;
        sb.append(" ").append(id);
      }
      return sb.toString().trim();
  }
  
//  static JmolList<JDXSpectrum> getSpecList(JmolList<JSVPanelNode> panelNodes) {
//    if (panelNodes == null || panelNodes.size() == 0)
//      return null;
//    JmolList<JDXSpectrum> specList = new JmolList<JDXSpectrum>();
//    for (int i = 0; i < panelNodes.size(); i++)
//      specList.add(panelNodes.get(i).getSpectrum());
//    return specList;
//  }
 
	public static boolean isOpen(JmolList<PanelNode> panelNodes, String filePath) {
    if (filePath != null)
      for (int i = panelNodes.size(); --i >= 0;) {
      	//System.out.println("JSVPanelNode " + filePath + " " + panelNodes.get(i).source.getFilePath());
        if (filePath.equals(panelNodes.get(i).source.getFilePath())
        		|| filePath.equals(panelNodes.get(i).frameTitle))
          return true;
      }
    return false;
  }
  
//  static int getNodeIndex(JmolList<JSVPanelNode> panelNodes, JSVPanelNode node) {
//    for (int i = panelNodes.size(); --i >= 0;)
//      if (node == panelNodes.get(i))
//        return i;
//    return -1;
//  }
  
  public void setFrameTitle(String name) {
		frameTitle = name;
	}
	public static JSVPanel getLastFileFirstNode(JmolList<PanelNode> panelNodes) {
		int n = panelNodes.size();
		PanelNode node = (n == 0 ? null : panelNodes.get(n - 1));
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