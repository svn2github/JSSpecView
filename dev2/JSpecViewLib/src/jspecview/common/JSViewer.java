package jspecview.common;

import java.util.List;
import java.util.StringTokenizer;

import jspecview.source.JDXSource;
import jspecview.util.Logger;
import jspecview.util.Parser;
import jspecview.util.TextFormat;

public class JSViewer {

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
      ////System.out.println("KEY-> " + key + " VALUE-> " + value + " : " + st);
      try {
        switch (st) {
        case UNKNOWN:
          Logger.info("Unrecognized parameter: " + key);
          break;
        default:
          si.getParameters().set(jsvp, st, value);
          si.setBoolean(st, Parameters.isTrue(value));
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
        case PEAK:
          try {
          List<String> tokens = ScriptToken.getTokens(value);
          value = " type=\"" + tokens.get(0).toUpperCase() 
          + "\" _match=\"" + TextFormat.trimQuotes(tokens.get(1).toUpperCase()) + "\"";
          if (tokens.size() > 2 && tokens.get(2).equalsIgnoreCase("all"))
            value += " title=\"ALL\"";
          processPeakPickEvent(si, new PeakInfo(value), false); // false == true here
          } catch (Exception e) {
            // ignore
          }
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


  /// from JavaScript
  
  public static void addHighLight(ScriptInterface si, double x1,
                                  double x2, Object color) {
    JSVPanel jsvp = si.getSelectedPanel();
    if (jsvp != null)
      jsvp.getPanelData().addHighlight(x1, x2, color);
  }

  /**
   * incoming script processing of <PeakAssignment file="" type="xxx"...> record
   * from Jmol
   */

  public static void syncScript(ScriptInterface si, String peakScript) {
    System.out.println("Jmol>JSV " + peakScript);
    if (peakScript.indexOf("<PeakData") < 0) {
      runScriptNow(si, si.getSelectedPanel(), peakScript);
      return;
    }
    String file = Parser.getQuotedAttribute(peakScript, "file");
    String index = Parser.getQuotedAttribute(peakScript, "index");
    if (file == null || index == null)
      return;
    if (si.getSpecNodes().size() == 0 || !checkFileAlreadyLoaded(si, file)) {
      //System.out.println("JSViewer closing all and reopening");
      si.closeAllAndOpenFile(file);
      checkAutoOverlay(si);
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

  private static boolean checkFileAlreadyLoaded(
                                                        ScriptInterface si,
                                                        String fileName) {
    if (si.getSelectedPanel().getPanelData().hasFileLoaded(fileName))
      return true;
    List<JSVSpecNode> specNodes = si.getSpecNodes();
    for (int i = specNodes.size(); --i >= 0;)
      if (specNodes.get(i).jsvp.getPanelData().hasFileLoaded(fileName)) {
        si.setSelectedPanel(specNodes.get(i).jsvp);
        //si.sendFrameChange(specNodes.get(i).jsvp); !
        return true;
      }
    return false;
  }

  private static void checkAutoOverlay(ScriptInterface si) {
    List<JSVSpecNode> specNodes = si.getSpecNodes();
    if (specNodes.get(0).jsvp.getSpectrumAt(0).isAutoOverlayFromJmolClick())
      si.execOverlay("*");
  }

  private static PeakInfo selectPanelByPeak(ScriptInterface si, String peakScript,
                                           JSVPanel jsvp) {
    List<JSVSpecNode> specNodes = si.getSpecNodes();
    if (specNodes == null)
      return null;
    //System.out.println("JSViewer selectPanelByPeak " + peakScript);
    String file = Parser.getQuotedAttribute(peakScript, "file");
    String index = Parser.getQuotedAttribute(peakScript, "index");
    PeakInfo pi = null;
    for (int i = specNodes.size(); --i >= 0;) 
      specNodes.get(i).jsvp.getPanelData().addPeakHighlight(null);
    //System.out.println("JSViewer selected panelspec=" + jsvp.getSpectrum());
    if ((pi = jsvp.getPanelData().selectPeakByFileIndex(file, index)) != null) {
      //System.out.println("JSViewer found peak in this spectrum -- " + pi);
      si.setFrame(JSVSpecNode.findNode(jsvp, specNodes)); 
    } else {
      //System.out.println("JSViewer did not find a peak for " + file + " " + index);
      for (int i = specNodes.size(); --i >= 0;) {
        JSVSpecNode node = specNodes.get(i);
        //System.out.println("JSViewer looking at " + node );
        //System.out.println(file + " " + index );
        
        if ((pi = node.jsvp.getPanelData().selectPeakByFileIndex(file, index)) != null) {
          //System.out.println("JSViewer setting spectrum to  " + node);
          si.setFrame(node); 
          break;
        }
        //System.out.println("hmm");
      }
    }
    return pi;
  }

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
          if ((pi2 = specNodes.get(i).jsvp.getPanelData().findMatchingPeakInfo(pi)) != null) {
            node = specNodes.get(i);
            break;
          }
        if (node == null)
          return;
        si.setFrame(node);
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
      pi = PeakInfo.basePeakInfo;
    si.getSelectedPanel().getPanelData().addPeakHighlight(pi);
    si.syncToJmol(jmolSelect(pi));  
  }

  public static String jmolSelect(PeakInfo pi) {
    String script = null;
    if (pi == PeakInfo.basePeakInfo) {
      script = "vibration off; select none;selectionHalos off;model {basemodel}";
    } else if ("IR".equals(pi.getType()) || "RAMAN".equals(pi.getType())) {
      script = "vibration ON; selectionHalos OFF;";
    } else if (pi.getAtoms() != null) {
      script = "vibration OFF; selectionhalos ON;";
    } else {
      script = "vibration OFF; selectionhalos OFF;";
    }
    return "Select: " + pi + " script=\"" + script;
  }


}
