package jspecview.common;

import java.util.List;
import java.util.StringTokenizer;

import jspecview.source.JDXSource;
import jspecview.util.Logger;
import jspecview.util.Parser;

public class JSViewer {

  public static PeakInfo selectPanelByPeak(ScriptInterface si, String peakScript,
                                           List<JSVSpecNode> specNodes,
                                           JSVPanel selectedPanel) {
    System.out.println("JSViewer selectPanelByPeak " + peakScript);
    if (specNodes == null)
      return null;
    String file = Parser.getQuotedAttribute(peakScript, "file");
    String index = Parser.getQuotedAttribute(peakScript, "index");
    PeakInfo pi = null;
    for (int i = specNodes.size(); --i >= 0;) 
      specNodes.get(i).jsvp.getPanelData().processPeakSelect(null);
    if ((pi = selectedPanel.getPanelData().findPeak(file, index)) == null)
      for (int i = specNodes.size(); --i >= 0;) {
        JSVSpecNode node = specNodes.get(i);
        if ((pi = node.jsvp.getPanelData().findPeak(file, index)) != null) {
          System.out.println("JSViewer setting spectrum to  " + node);
          si.setSpectrum(i);
          break;
        }
      }
    return pi;
  }

  public static void selectSpectrumInPanel(ScriptInterface si, JSVPanel selectedPanel, 
                                           String peakScript) {
    String file = Parser.getQuotedAttribute(peakScript, "file");
    String type = Parser.getQuotedAttribute(peakScript, "type");
    String model = Parser.getQuotedAttribute(peakScript, "model");
    selectedPanel.getPanelData().selectSpectrum(file, type, model);
    si.sendFrameChange(selectedPanel);
  }

  public static void setYScale(String value, 
                               List<JSVSpecNode> specNodes, 
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
        if (JDXSpectrum.areScalesCompatible(spec, node.getSpectrum(),
            false))
          node.jsvp.getPanelData().setZoom(Double.NaN, y1, Double.NaN, y2);
      }
    } else {
      jsvp.getPanelData().setZoom(Double.NaN, y1, Double.NaN, y2);
    }
  }

  public static boolean runScriptNow(ScriptInterface si, JSVPanel jsvp,
                                     String script) {
    if (script == null)
      script = "";
    String msg = null;
    script = script.trim();
    if (Logger.debugging)
      Logger.info("RUNSCRIPT " + script);
    StringTokenizer allParamTokens = new StringTokenizer(script, ";");
    while (allParamTokens.hasMoreTokens()) {
      String token = allParamTokens.nextToken().trim();
      // now split the key/value pair
      StringTokenizer eachParam = new StringTokenizer(token);
      String key = ScriptToken.getKey(eachParam);
      ScriptToken st = ScriptToken.getScriptToken(key);
      String value = ScriptToken.getValue(st, eachParam, token);
      //System.out.println("KEY-> " + key + " VALUE-> " + value + " : " + st);
      try {
        switch (st) {
        case UNKNOWN:
          Logger.info("Unrecognized parameter: " + key);
          break;
        default:
          si.getParameters().set(jsvp, st, value);
          break;
        case PEAKCALLBACKFUNCTIONNAME:
        case SYNCCALLBACKFUNCTIONNAME:
        case COORDCALLBACKFUNCTIONNAME:
          si.execSetCallback(st, value);
          break;
        case AUTOINTEGRATE:
          si.execSetAutoIntegrate(Parameters.isTrue(value));
          break;
        case CLOSE:
          si.execClose(value);
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
          si.execIntegrate(value);
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
          si.execTAConvert(value.toUpperCase().startsWith("T") ? JDXSpectrum.TO_TRANS
              : value.toUpperCase().startsWith("A") ? JDXSpectrum.TO_ABS
                  : JDXSpectrum.IMPLIED);
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
          si.execOverlay(value);
          break;
        case SPECTRUM:
        case SPECTRUMNUMBER:
          jsvp = si.execSetSpectrum(value);
          if (jsvp == null)
            return false;
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
      }
    }
    si.execScriptComplete(msg, true);
    return true;
  }

  public static void setOverlayLegendVisibility(ScriptInterface si,
                                                JSVPanel jsvp, boolean showLegend) {
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

  public static void checkAutoOverlay(ScriptInterface si,
                                      List<JSVSpecNode> specNodes) {
    if (specNodes.get(0).jsvp.getSpectrumAt(0).isAutoOverlayFromJmolClick())
      si.execOverlay("*");
  }
  
}
