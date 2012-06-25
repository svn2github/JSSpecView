package jspecview.common;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jspecview.source.JDXSource;
import jspecview.util.Logger;
import jspecview.util.Parser;
import jspecview.util.TextFormat;

/**
 * This static class encapsulates all general functionality of applet and app.
 * Most methods include ScriptInterface parameter, which will be
 * JSVAppletPrivate, JSVAppletPrivatePro, or MainFrame.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class JSViewer {

  public static boolean runScriptNow(ScriptInterface si, String script) {
    if (script == null)
      script = "";
    String msg = null;
    script = script.trim();
    if (Logger.debugging)
      Logger.info("RUNSCRIPT " + script);
    ScriptCommandTokenizer commandTokens = new ScriptCommandTokenizer(script, ";\n");
    JSVPanel jsvp = si.getSelectedPanel();
    boolean isOK = true;
    while (commandTokens.hasMoreTokens()) {
      String token = commandTokens.nextToken();
      // now split the key/value pair
      StringTokenizer eachParam = new StringTokenizer(token);
      String key = ScriptToken.getKey(eachParam);
      if (key == null)
        continue;
      ScriptToken st = ScriptToken.getScriptToken(key);
      String value = ScriptToken.getValue(st, eachParam, token);
      Logger.info("KEY-> " + key + " VALUE-> " + value + " : " + st);
      try {
        switch (st) {
        case UNKNOWN:
          Logger.info("Unrecognized parameter: " + key);
          break;
        default:
          si.getParameters().set(jsvp, st, value);
          si.updateBoolean(st, Parameters.isTrue(value));
          break;
        case PEAKCALLBACKFUNCTIONNAME:
        case SYNCCALLBACKFUNCTIONNAME:
        case COORDCALLBACKFUNCTIONNAME:
        case LOADFILECALLBACKFUNCTIONNAME:
          si.execSetCallback(st, value);
          break;
        case AUTOINTEGRATE:
          si.execSetAutoIntegrate(Parameters.isTrue(value));
          break;
        case CLOSE:
          si.execClose(value, true);
          jsvp = si.getSelectedPanel();
          break;
        case EXPORT:
          msg = si.execExport(jsvp, value);
          return false;
        case GETSOLUTIONCOLOR:
          if (jsvp != null)
            si.setSolutionColor(true);
          break;
        case HIDDEN:
          si.execHidden(Parameters.isTrue(value));
          break;
        case INTEGRATE:
          if (jsvp == null)
            continue;
          execIntegrate(si, value);
          break;
        case INTEGRATIONRATIOS:
          si.execSetIntegrationRatios(value);
          break;
        case INTERFACE:
          si.execSetInterface(value);
          break;
        case IRMODE:
          if (jsvp == null)
            continue;
          execTAConvert(si, value);
          break;
        case JMOL:
          si.syncToJmol(value);
          break;
        case LABEL:
          if (jsvp != null)
            jsvp.getPanelData().addAnnotation(ScriptToken.getTokens(value));
          break;
        case LOAD:
          msg = si.execLoad(value);
          jsvp = si.getSelectedPanel();
          break;
        case OVERLAY:
          execOverlay(si, value, true);
          break;
        case PEAK:
          try {
            List<String> tokens = ScriptToken.getTokens(value);
            value = " type=\"" + tokens.get(0).toUpperCase() + "\" _match=\""
                + TextFormat.trimQuotes(tokens.get(1).toUpperCase()) + "\"";
            if (tokens.size() > 2 && tokens.get(2).equalsIgnoreCase("all"))
              value += " title=\"ALL\"";
            processPeakPickEvent(si, new PeakInfo(value), false); // false == true here
          } catch (Exception e) {
            // ignore
          }
          break;
        case PRINT:
          if (jsvp == null)
            continue;
        	si.print();
        	break;
        case SPECTRUM:
        case SPECTRUMNUMBER:
          jsvp = si.execSetSpectrum(value);
          if (jsvp == null)
            return false;
          break;
        case STACKOFFSETY:
        	int offset = Parser.parseInt("" + Parser.parseFloat(value));
        	if (jsvp != null&& offset != Integer.MIN_VALUE)
        		jsvp.getPanelData().setYStackOffsetPercent(offset);
        	break;
        case TEST:
          si.execTest(value);
          break;
        case YSCALE:
          setYScale(value, si.getSpecNodes(), jsvp, si.getCurrentSource());
          break;
        }
      } catch (Exception e) {
        e.printStackTrace();
        // should we be returning?
        isOK = false;
      }
    }
    si.execScriptComplete(msg, true);
    return isOK;
  }

	public static void execOverlay(ScriptInterface si, String value, boolean fromScript) {
    List<JDXSpectrum> speclist = new ArrayList<JDXSpectrum>();
    String strlist = fillSpecList(si, si.getSpecNodes(), value, speclist,
        si.getSelectedPanel(), "1.");
    if (speclist.size() > 0)
      si.openDataOrFile(null, strlist, speclist, strlist, -1, -1);
    if (!fromScript) {
    	si.validateAndRepaint();
    }
	}

	private static void execTAConvert(ScriptInterface si, String value) {
    int mode = (value.toUpperCase().startsWith("T") ? JDXSpectrum.TO_TRANS
        : value.toUpperCase().startsWith("A") ? JDXSpectrum.TO_ABS
            : JDXSpectrum.IMPLIED);
    JSVPanel jsvp = si.getSelectedPanel();
    if (jsvp == null)
      return;
    JDXSpectrum spec = jsvp.getSpectrum();
    JDXSpectrum spec2 = JDXSpectrum.taConvert(spec, mode);
    if (spec2 == spec)
      return;
    jsvp.setSpectrum(spec2);
    si.execTAConvert(mode);
    jsvp.repaint();
  }

  private static void execIntegrate(ScriptInterface si, String value) {
    JSVPanel jsvp = si.getSelectedPanel();
    if (jsvp == null)
      return;
    JDXSpectrum spec = jsvp.getSpectrum();
    spec.checkIntegral(si.getParameters(), value);
    si.execIntegrate(spec);
    jsvp.repaint();
  }

  private static void setYScale(String value, List<JSVSpecNode> specNodes,
                                JSVPanel jsvp, JDXSource currentSource) {
    if (jsvp == null)
      return;
    List<String> tokens = ScriptToken.getTokens(value);
    int pt = 0;
    boolean isAll = false;
    if (tokens.size() > 1 && tokens.get(0).equalsIgnoreCase("ALL")) {
      isAll = true;
      pt++;
    }
    double y1 = Double.parseDouble(tokens.get(pt++));
    double y2 = Double.parseDouble(tokens.get(pt));
    if (isAll) {
      JDXSpectrum spec = jsvp.getSpectrum();
      for (int i = specNodes.size(); --i >= 0;) {
        JSVSpecNode node = specNodes.get(i);
        if (node.source != currentSource)
          continue;
        if (JDXSpectrum.areScalesCompatible(spec, node.getSpectrum(), false))
          node.jsvp.getPanelData().setZoom(Double.NaN, y1, Double.NaN, y2);
      }
    } else {
      jsvp.getPanelData().setZoom(Double.NaN, y1, Double.NaN, y2);
    }
  }

  public static void setOverlayLegendVisibility(ScriptInterface si,
                                                JSVPanel jsvp,
                                                boolean showLegend) {
    List<JSVSpecNode> specNodes = si.getSpecNodes();
    JSVSpecNode node = JSVSpecNode.findNode(jsvp, specNodes);
    for (int i = specNodes.size(); --i >= 0;)
      showOverlayLegend(si, specNodes.get(i), specNodes.get(i) == node
          && showLegend);
  }

  private static void showOverlayLegend(ScriptInterface si, JSVSpecNode node,
                                        boolean visible) {
    JSVDialog legend = (JSVDialog) node.legend;
    if (legend == null && visible) {
      legend = node.setLegend(node.jsvp.getPanelData()
          .getNumberOfSpectraInCurrentSet() > 1
          && node.jsvp.getPanelData().getNumberOfGraphSets() == 1 ? si
          .getOverlayLegend(node.jsvp) : null);
    }
    if (legend != null)
      legend.setVisible(visible);
  }

  /// from JavaScript

  public static void addHighLight(ScriptInterface si, double x1, double x2,
                                  int r, int g, int b, int a) {
    JSVPanel jsvp = si.getSelectedPanel();
    if (jsvp != null) {
      jsvp.getPanelData().addHighlight(x1, x2, r, g, b, a);
      jsvp.repaint();
    }
  }

  /**
   * incoming script processing of <PeakAssignment file="" type="xxx"...> record
   * from Jmol
   */

  public static void syncScript(ScriptInterface si, String peakScript) {
    Logger.info("Jmol>JSV " + peakScript);
    if (peakScript.indexOf("<PeakData") < 0) {
      runScriptNow(si, peakScript);
      return;
    }
    String file = Parser.getQuotedAttribute(peakScript, "file");
    String index = Parser.getQuotedAttribute(peakScript, "index");
    if (file == null || index == null)
      return;
    if (si.getSpecNodes().size() == 0 || !checkFileAlreadyLoaded(si, file)) {
      Logger.info("file " + file + " not found -- JSViewer closing all and reopening");
      si.syncLoad(file);
    }
    PeakInfo pi = selectPanelByPeak(si, peakScript, si.getSelectedPanel());
    JSVPanel jsvp = si.getSelectedPanel();
    String type = Parser.getQuotedAttribute(peakScript, "type");
    String model = Parser.getQuotedAttribute(peakScript, "model");
    jsvp.getPanelData().selectSpectrum(file, type, model);
    si.sendFrameChange(jsvp);
    jsvp.getPanelData().addPeakHighlight(pi);
    jsvp.repaint();
    // round trip this so that Jmol highlights all equivalent atoms
    // and appropriately starts or clears vibration
    si.syncToJmol(jmolSelect(pi));
  }

  private static boolean checkFileAlreadyLoaded(ScriptInterface si,
                                                String fileName) {
    if (si.getSelectedPanel().getPanelData().hasFileLoaded(fileName))
      return true;
    List<JSVSpecNode> specNodes = si.getSpecNodes();
    for (int i = specNodes.size(); --i >= 0;)
      if (specNodes.get(i).jsvp.getPanelData().hasFileLoaded(fileName)) {
        si.setSelectedPanel(specNodes.get(i).jsvp);
        return true;
      }
    return false;
  }

  private static PeakInfo selectPanelByPeak(ScriptInterface si,
                                            String peakScript, JSVPanel jsvp) {
    List<JSVSpecNode> specNodes = si.getSpecNodes();
    if (specNodes == null)
      return null;
    String file = Parser.getQuotedAttribute(peakScript, "file");
    String index = Parser.getQuotedAttribute(peakScript, "index");
    PeakInfo pi = null;
    for (int i = specNodes.size(); --i >= 0;)
      specNodes.get(i).jsvp.getPanelData().addPeakHighlight(null);
    if ((pi = jsvp.getPanelData().selectPeakByFileIndex(file, index)) != null) {
      si.setNode(JSVSpecNode.findNode(jsvp, specNodes), false);
    } else {
      for (int i = specNodes.size(); --i >= 0;) {
        JSVSpecNode node = specNodes.get(i);
        if ((pi = node.jsvp.getPanelData().selectPeakByFileIndex(file, index)) != null) {
          si.setNode(node, false);
          break;
        }
      }
    }
    return pi;
  }

  /**
   * this method is called as a result of the user clicking on a peak
   * (eventObject instanceof PeakPickEvent) or from PEAK command execution
   *  
   * @param si
   * @param eventObj
   * @param isApp
   */
  public static void processPeakPickEvent(ScriptInterface si, Object eventObj,
                                          boolean isApp) {
    PeakInfo pi;
    if (eventObj instanceof PeakInfo) {
      // this is a call from the PEAK command, above.
      pi = (PeakInfo) eventObj;
      JSVPanel jsvp = si.getSelectedPanel();
      PeakInfo pi2 = jsvp.getPanelData().findMatchingPeakInfo(pi);
      if (pi2 == null) {
        if (!"ALL".equals(pi.getTitle()))
          return;
        List<JSVSpecNode> specNodes = si.getSpecNodes();
        JSVSpecNode node = null;
        for (int i = 0; i < specNodes.size(); i++)
          if ((pi2 = specNodes.get(i).jsvp.getPanelData().findMatchingPeakInfo(
              pi)) != null) {
            node = specNodes.get(i);
            break;
          }
        if (node == null)
          return;
        si.setNode(node, false);
      }
      pi = pi2;
    } else {
      PeakPickEvent e = ((PeakPickEvent) eventObj);
      si.setSelectedPanel((JSVPanel) e.getSource());
      pi = e.getPeakInfo();
    }
    si.getSelectedPanel().getPanelData().addPeakHighlight(pi);
    si.syncToJmol(jmolSelect(pi));
    if (pi.isClearAll()) // was not in app version??
      si.getSelectedPanel().repaint();
    else
      si.getSelectedPanel().getPanelData().selectSpectrum(pi.getFilePath(),
          pi.getType(), pi.getModel());
    si.checkCallbacks(pi.getTitle());

  }

  public static void sendFrameChange(ScriptInterface si, JSVPanel jsvp) {
    PeakInfo pi = jsvp.getSpectrum().getSelectedPeak();
    if (pi == null)
      pi = jsvp.getSpectrum().getModelPeakInfoForAutoSelectOnLoad();
    if (pi == null)
      pi = jsvp.getSpectrum().getBasePeakInfo();
    si.getSelectedPanel().getPanelData().addPeakHighlight(pi);
    si.syncToJmol(jmolSelect(pi));
  }

  public static String jmolSelect(PeakInfo pi) {
    String script = null;
    if ("IR".equals(pi.getType()) || "RAMAN".equals(pi.getType())) {
      script = "vibration ON; selectionHalos OFF;";
    } else if (pi.getAtoms() != null) {
      script = "vibration OFF; selectionhalos ON;";
    } else {
      script = "vibration OFF; selectionhalos OFF;";
    }
    return "Select: " + pi + " script=\"" + script;
  }

  public static void removeAllHighlights(ScriptInterface si) {
    JSVPanel jsvp = si.getSelectedPanel();
    if (jsvp != null) {
      jsvp.getPanelData().removeAllHighlights();
      jsvp.repaint();
    }
  }

  public static void removeHighlights(ScriptInterface si, double x1, double x2) {
    JSVPanel jsvp = si.getSelectedPanel();
    if (jsvp != null) {
      jsvp.getPanelData().removeHighlight(x1, x2);
      jsvp.repaint();
    }
  }

  public static Map<String, Object> getPropertyAsJavaObject(ScriptInterface si,
                                                            String key) {
    if ("".equals(key))
      key = null;
    List<Map<String, Object>> info = new ArrayList<Map<String, Object>>();
    List<JSVSpecNode> specNodes = si.getSpecNodes();
    for (int i = 0; i < specNodes.size(); i++) {
      JSVPanel jsvp = specNodes.get(i).jsvp;
      if (jsvp == null)
        continue;
      info.add(jsvp.getPanelData().getInfo(true, key));
    }
    Map<String, Object> map = new Hashtable<String, Object>();
    map.put("items", info);
    return map;
  }

  public static String getCoordinate(ScriptInterface si) {
    // important to use getSelectedPanel() here because it may be from MainFrame in PRO
    if (si.getSelectedPanel() != null) {
      Coordinate coord = si.getSelectedPanel().getPanelData()
          .getClickedCoordinate();
      if (coord != null)
        return coord.getXVal() + " " + coord.getYVal();
    }
    return "";
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
  public static String fillSpecList(ScriptInterface si, List<JSVSpecNode> specNodes, String value,
                                    List<JDXSpectrum> speclist,
                                    JSVPanel selectedPanel, String prefix) {
    List<String> list;
    List<String> list0 = null;
    boolean isNone = (value.equalsIgnoreCase("NONE"));
    if (isNone || value.equalsIgnoreCase("all"))
      value = "*";
    value = TextFormat.simpleReplace(value, "*", " * ");
    if (value.equals(" * ")) {
      list = ScriptToken.getTokens(JSVSpecNode.getSpectrumListAsString(specNodes));
    } else if (value.startsWith("\"")) {
      list = ScriptToken.getTokens(value);
    } else {
      value = TextFormat.simpleReplace(value, "-", " - ");
      list = ScriptToken.getTokens(value);
      list0 = ScriptToken.getTokens(JSVSpecNode.getSpectrumListAsString(specNodes));
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
          speclist.add(JSVSpecNode.findNodeById(idLast = list0.get(pt++),
              specNodes).jsvp.getSpectrumAt(0));
          sb.append(",").append(idLast);
        }
        continue;
      }
      if (!id.contains("."))
        id = id0 + id;
      JSVSpecNode node = JSVSpecNode.findNodeById(id, specNodes);
      if (node == null)
        continue;
      JDXSpectrum spec = node.jsvp.getSpectrumAt(0);
      idLast = id;
      spec.setUserYFactor(userYFactor);
      speclist.add(spec);
      sb.append(",").append(id);
    }
    if (speclist.size() == 1) {
      	JSVSpecNode node = JSVSpecNode.findNodeById(idLast, specNodes);
      	if (node != null) {
      		si.setSelectedPanel(node.jsvp);
      		// possibility of a problem here -- we are not communicating with Jmol our model changes.
      		speclist.clear();
      	}
    }
    return (isNone ? "NONE" : sb.length() > 0 ? sb.toString().substring(1) : null);
  }
  
}
