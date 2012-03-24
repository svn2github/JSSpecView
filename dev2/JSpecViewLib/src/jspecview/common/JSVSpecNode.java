/**
 * 
 */
package jspecview.common;

import java.util.ArrayList;
import java.util.List;

import jspecview.common.JSVContainer;
import jspecview.common.JSVDialog;
import jspecview.common.JSVPanel;
import jspecview.source.JDXSource;
import jspecview.util.TextFormat;

public class JSVSpecNode {

  public Object treeNode;
  public void setTreeNode(Object node) {
    treeNode = node;
  }
  public Object getTreeNode() {
    return treeNode;
  }
  
  public JDXSource source;
  public String fileName;
  public JSVContainer frame;
  public JSVPanel jsvp;
  public String id;
  public JSVDialog legend;

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

  public JSVSpecNode(String id, String fileName, JDXSource source, JSVContainer frame,
      JSVPanel jsvp) {
    this.id = id;
    this.source = source;
    this.fileName = fileName;
    this.frame = frame;
    this.jsvp = jsvp;
    if (jsvp != null)
      jsvp.getSpectrumAt(0).setId(id);
  }

  @Override
  public String toString() {
    return ((id == null ? "" : id + ": ") + (frame == null ? fileName : frame
        .getTitle()));
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
  public static JDXSpectrum findSpectrumById(String id, List<JSVSpecNode> specNodes) {
    for (int i = specNodes.size(); --i >= 0;)
      if (id.equals(specNodes.get(i).id))
        return specNodes.get(i).jsvp.getSpectrum();
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
  /**
   * Returns the tree node that is associated with an internal frame
   * 
   * @param frame
   *        the JSVFrame
   * @param specNodes TODO
   * @return the tree node that is associated with an internal frame
   */
  public static JSVSpecNode findNode(Object frame, List<JSVSpecNode> specNodes) {
    for (int i = specNodes.size(); --i >= 0;)
      if (specNodes.get(i).frame == frame)
        return specNodes.get(i);
    return null;
  }

  /**
   * originally in MainFrame, this method takes the OVERLAY command option and 
   * converts it to a list of spectra
   * 
   * @param si
   * @param specNodes
   * @param value
   * @param speclist
   * @param selectedPanel
   * @param prefix
   * @return      comma-separated list, for the title
   */
  public static String fillSpecList(List<JSVSpecNode> specNodes, String value,
                                    List<JDXSpectrum> speclist,
                                    JSVPanel selectedPanel, String prefix) {
    List<String> list;
    List<String> list0 = null;
    boolean isNone = (value.equalsIgnoreCase("NONE"));
    if (isNone || value.equalsIgnoreCase("all"))
      value = "*";
    value = TextFormat.simpleReplace(value, "*", " * ");
    if (value.equals(" * ")) {
      list = ScriptToken.getTokens(getSpectrumListAsString(specNodes));
    } else if (value.startsWith("\"")) {
      list = ScriptToken.getTokens(value);
    } else {
      value = TextFormat.simpleReplace(value, "-", " - ");
      list = ScriptToken.getTokens(value);
      list0 = ScriptToken.getTokens(getSpectrumListAsString(specNodes));
      if (list0.size() == 0)
        return null;
    }

    String id0 = (selectedPanel == null ? prefix : JSVSpecNode.findNode(
        selectedPanel, specNodes).id);
    id0 = id0.substring(0, id0.indexOf(".") + 1);
    StringBuffer sb = new StringBuffer();
    int n = list.size();
    String idLast = null;
    for (int i = 0; i < n; i++) {
      String id = list.get(i);
      double userYFactor = 1;
      if (i + 1 < n && list.get(i + 1).equals("*")) {
        i += 2;
        try {
          userYFactor = Double.parseDouble(list.get(i));
        } catch (NumberFormatException e) {
        }
      }
      if (id.equals("-")) {
        if (idLast == null)
          idLast = list0.get(0);
        id = (i + 1 == n ? list0.get(list0.size() - 1) : list.get(++i));
        if (!id.contains("."))
          id = id0 + id;
        int pt = 0;
        while (pt < list0.size() && !list0.get(pt).equals(idLast))
          pt++;
        pt++;
        while (pt < list0.size() && !idLast.equals(id)) {
          speclist.add(JSVSpecNode.findSpectrumById(idLast = list0.get(pt++),
              specNodes));
          sb.append(",").append(idLast);
        }
        continue;
      }
      if (!id.contains("."))
        id = id0 + id;
      JDXSpectrum spec = JSVSpecNode.findSpectrumById(id, specNodes);
      if (spec == null)
        continue;
      idLast = id;
      spec.setUserYFactor(userYFactor);
      speclist.add(spec);
      sb.append(",").append(id);
    }
    return (isNone ? "NONE" : sb.length() > 0 ? sb.toString().substring(1) : null);
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
        if (filePath.equals(specNodes.get(i).source.getFilePath()))
          return true;
    return false;
  }

}