package jspecview.common;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jspecview.common.JDXSpectrum.IRMode;
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
        case DEBUG:
          Logger
              .setLogLevel(value.toLowerCase().equals("high") ? Logger.LEVEL_DEBUGHIGH
                  : Parameters.isTrue(value) ? Logger.LEVEL_DEBUG : Logger.LEVEL_INFO);
          break;
        case EXPORT:
          msg = si.execExport(jsvp, value);
          return false;
        case FINDX:
        	if (jsvp != null)
        		jsvp.getPanelData().findX(Double.parseDouble(value));
        	break;        	
        case GETSOLUTIONCOLOR:
          if (jsvp != null)
        		showColorMessage(si);
          break;
        case HIDDEN:
          si.execHidden(Parameters.isTrue(value));
          break;
        case INTEGRALOFFSET:
        case INTEGRALRANGE:
        	execSetIntegralParameter(si, st, Double.parseDouble(value));
        	break;
        case INTEGRATION:
        case INTEGRATE:
          if (jsvp != null)
            execIntegrate(si, value);
          break;
        case INTEGRATIONRATIOS:
          si.setIntegrationRatios(value);
          if (jsvp != null)
            execIntegrate(si, null);          
          break;
        case INTERFACE:
          si.execSetInterface(value);
          break;
        case IRMODE:
          if (jsvp == null)
            continue;
          execIRMode(si, value);
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
        case OVERLAYSTACKED:
          if (jsvp != null)
          	jsvp.getPanelData().splitStack(!Parameters.isTrue(value));
          break;
        case PEAK:
        	execPeak(si, value);
          break;
        case PEAKLIST:
        	execPeakList(si, value);
        	break;
        case PRINT:
          if (jsvp == null)
            continue;
        	si.print();
        	break;
        case SCALEBY:
        	scaleSelectedBy(si.getPanelNodes(), value);
        	break;
        case SELECT:
          execSelect(si, value);
          break;
        case SETPEAK:
        	if (jsvp != null)
        		jsvp.getPanelData().shiftSpectrum(Double.NaN, 
        				value.equalsIgnoreCase("NONE") ? Double.MAX_VALUE : Double.parseDouble(value));
        	break;
        case SETX:
        	if (jsvp != null)
        		jsvp.getPanelData().shiftSpectrum(Double.MIN_VALUE, Double.parseDouble(value));
        	break;
        case SHIFTX:
        	if (jsvp != null)
        		jsvp.getPanelData().shiftSpectrum(
        				value.equalsIgnoreCase("NONE") ? Double.MAX_VALUE 
        						: Double.parseDouble(value), Double.NaN);
        	break;        	
        case SHOWINTEGRATION:
        	if (jsvp == null)
        		break;
        	jsvp.getPanelData().showIntegration(Parameters.getTFToggle(value), si.getParameters());
        	execIntegrate(si, null);
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
        case OVERLAY: // deprecated
        case VIEW:
          execView(si, value, true);
          break;
        case YSCALE:
        	if (jsvp != null)
            setYScale(si, value, jsvp);
          break;
        case ZOOM:
        	if (jsvp != null)
          	isOK = setZoom(value, jsvp);
          break;
        }
      } catch (Exception e) {
        e.printStackTrace();
        // should we be returning?
        isOK = false;
      }
    }
    si.execScriptComplete(msg, true);
	  //si.getSelectedPanel().requestFocusInWindow(); // could be CLOSE ALL
    return isOK;
  }

	private static void execPeak(ScriptInterface si, String value) {
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
	}

	private static void execPeakList(ScriptInterface si, String value) {
		JSVPanel jsvp = si.getSelectedPanel();
		Parameters p = si.getParameters();
		Boolean b = Parameters.getTFToggle(value);
		if (b != null || value.equalsIgnoreCase("TOGGLE")) {
			if (jsvp != null)
				jsvp.getPanelData().getPeakListing(null, b);
		} else {
			List<String> tokens = ScriptToken.getTokens(value);
			for (int i = tokens.size(); --i >= 0;) {
				String token = tokens.get(i);
				int pt = tokens.indexOf("=");
				if (pt <= 0)
					continue;
				String key = token.substring(0, pt);
				value = token.substring(pt + 1);
				try {
					if (key.startsWith("thr")) {
						p.peakListThreshold = Double.valueOf(value);
						p.peakListInclude = -Math.abs(p.peakListInclude);
					} else if (key.startsWith("inc")) {
						p.peakListThreshold = Integer.valueOf(value);
						p.peakListThreshold = -Math.abs(p.peakListThreshold);
					} else if (key.startsWith("ski")) {
						p.peakListSkip = Integer.valueOf(value);
					} else if (key.startsWith("int")) {
						p.peakListInterpolation = (value.equalsIgnoreCase("none") ? "NONE"
								: "parabolic");
					} else if (key.equalsIgnoreCase("OFF"))
						if (jsvp != null)
							jsvp.getPanelData().getPeakListing(p, Boolean.TRUE);
				} catch (Exception e) {
					// ignore
				}
			}
		}

	}

	private static boolean setZoom(String value, JSVPanel jsvp) {
		double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
		List<String> tokens;
		tokens = ScriptToken.getTokens(value);
		switch (tokens.size()) {
		default:
			return false;
		case 1:
			if (!tokens.get(0).equalsIgnoreCase("out"))
				return false;
			break;
		case 2:
			x1 = Double.parseDouble(tokens.get(0));
			x2 = Double.parseDouble(tokens.get(1));
			break;
		case 4:
			x1 = Double.parseDouble(tokens.get(0));
			y1 = Double.parseDouble(tokens.get(1));
			x2 = Double.parseDouble(tokens.get(2));
			y2 = Double.parseDouble(tokens.get(3));
		}
		jsvp.getPanelData().setZoom(x1, y1, x2, y2);
		return true;
	}

	private static void scaleSelectedBy(List<JSVPanelNode> nodes, String value) {
		try {
			double f = Double.parseDouble(value);
	    for (int i = nodes.size(); --i >= 0;)
       	nodes.get(i).jsvp.getPanelData().scaleSelectedBy(f);
		} catch (Exception e) {
		}
	}

	public static void execSelect(ScriptInterface si, String value) {
    List<JSVPanelNode> nodes = si.getPanelNodes();
    for (int i = nodes.size(); --i >= 0;)
    	nodes.get(i).jsvp.getPanelData().selectFromEntireSet(Integer.MIN_VALUE);
    List<JDXSpectrum> speclist = new ArrayList<JDXSpectrum>();
    fillSpecList(si, si.getPanelNodes(), value, speclist,
        si.getSelectedPanel(), "1.", false);
	}

	public static void execView(ScriptInterface si, String value, boolean fromScript) {
    List<JDXSpectrum> speclist = new ArrayList<JDXSpectrum>();
    String strlist = fillSpecList(si, si.getPanelNodes(), value, speclist,
        si.getSelectedPanel(), "1.", true);
    if (speclist.size() > 0)
      si.openDataOrFile(null, strlist, speclist, strlist, -1, -1);
    if (!fromScript) {
    	si.validateAndRepaint();
    }
	}

	private static void execIRMode(ScriptInterface si, String value) {
    IRMode mode = IRMode.getMode(value); // T, A, or TOGGLE
    JSVPanel jsvp = si.getSelectedPanel();
    if (jsvp == null)
      return;
    JDXSpectrum spec = jsvp.getSpectrum();
    JDXSpectrum spec2 = JDXSpectrum.taConvert(spec, mode);
    if (spec2 == spec)
      return;
    jsvp.setSpectrum(spec2);
    si.setIRMode(mode);
    jsvp.repaint();
  }

  private static void execIntegrate(ScriptInterface si, String value) {
    JSVPanel jsvp = si.getSelectedPanel();
    if (jsvp == null)
      return;
    jsvp.getPanelData().checkIntegral(si.getParameters(), value);
    String integrationRatios = si.getIntegrationRatios();
		if (integrationRatios != null)
			jsvp.getPanelData().setIntegrationRatios(integrationRatios);
		si.setIntegrationRatios(null); // one time only
    jsvp.repaint();
  }

	private static void execSetIntegralParameter(ScriptInterface si, ScriptToken st, double value) {
		Parameters p = si.getParameters();
		switch (st) {
		case INTEGRALRANGE:
			p.integralRange = value;
			break;
		case INTEGRALOFFSET:
			p.integralOffset = value;
			break;
		}
    JSVPanel jsvp = si.getSelectedPanel();
    if (jsvp == null)
      return;
    jsvp.getPanelData().checkIntegral(si.getParameters(), "update");
	}

  private static void setYScale(ScriptInterface si, String value, 
                                JSVPanel jsvp) {
  	List<JSVPanelNode> panelNodes = si.getPanelNodes();
  	JDXSource currentSource = si.getCurrentSource();
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
      for (int i = panelNodes.size(); --i >= 0;) {
        JSVPanelNode node = panelNodes.get(i);
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
    List<JSVPanelNode> panelNodes = si.getPanelNodes();
    JSVPanelNode node = JSVPanelNode.findNode(jsvp, panelNodes);
    for (int i = panelNodes.size(); --i >= 0;)
      showOverlayLegend(si, panelNodes.get(i), panelNodes.get(i) == node
          && showLegend);
  }

  private static void showOverlayLegend(ScriptInterface si, JSVPanelNode node,
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
      jsvp.getPanelData().addHighlight(null, x1, x2, null, r, g, b, a);
      jsvp.repaint();
    }
  }

  /**
   * incoming script processing of <PeakAssignment file="" type="xxx"...> record
   * from Jmol
   */

  public static void syncScript(ScriptInterface si, String peakScript) {
    System.out.println(Thread.currentThread() + "Jmol>JSV " + peakScript);
    if (peakScript.indexOf("<PeakData") < 0) {
      runScriptNow(si, peakScript);
      return;
    }
    String file = Parser.getQuotedAttribute(peakScript, "file");
    String index = Parser.getQuotedAttribute(peakScript, "index");
    if (file == null || index == null)
      return;
    String model = Parser.getQuotedAttribute(peakScript, "model");
    String modelSent = si.getReturnFromJmolModel();
    if (model != null && modelSent != null && !model.equals(modelSent)) {
    	Logger.info("JSV ignoring model " + model + "; should be " + modelSent);
    	return;
    }
    si.setReturnFromJmolModel(null);
    if (si.getPanelNodes().size() == 0 || !checkFileAlreadyLoaded(si, file)) {
      Logger.info("file " + file + " not found -- JSViewer closing all and reopening");
      si.syncLoad(file);
    }
    System.out.println(Thread.currentThread() + "syncscript jsvp=" + si.getSelectedPanel() + " s0=" + si.getSelectedPanel().getSpectrum());
    PeakInfo pi = selectPanelByPeak(si, peakScript);
    System.out.println(Thread.currentThread() + "syncscript pi=" + pi);
    JSVPanel jsvp = si.getSelectedPanel();
    System.out.println(Thread.currentThread() + "syncscript jsvp=" + jsvp);
    String type = Parser.getQuotedAttribute(peakScript, "type");
    System.out.println(Thread.currentThread() + "syncscript --selectSpectrum2 "  + pi + " " + type + " "  + model + " s=" + jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
    jsvp.getPanelData().selectSpectrum(file, type, model, true);
    System.out.println(Thread.currentThread() + "syncscript --selectSpectrum3 "  + pi + " " + type + " "  + model + " s=" + jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
    si.sendPanelChange(jsvp);
    System.out.println(Thread.currentThread() + "syncscript --selectSpectrum4 "  + pi + " " + type + " "  + model + " s=" + jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
    jsvp.getPanelData().addPeakHighlight(pi);
    System.out.println(Thread.currentThread() + "syncscript --selectSpectrum5 "  + pi + " " + type + " "  + model + " s=" + jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
    jsvp.repaint();
    // round trip this so that Jmol highlights all equivalent atoms
    // and appropriately starts or clears vibration
    si.syncToJmol(jmolSelect(pi));
  }

  private static boolean checkFileAlreadyLoaded(ScriptInterface si,
                                                String fileName) {
  	JSVPanel jsvp = si.getSelectedPanel();
  	if (jsvp != null && jsvp.getPanelData().hasFileLoaded(fileName))
      return true;
    List<JSVPanelNode> panelNodes = si.getPanelNodes();
    for (int i = panelNodes.size(); --i >= 0;)
      if (panelNodes.get(i).jsvp.getPanelData().hasFileLoaded(fileName)) {
        si.setSelectedPanel(panelNodes.get(i).jsvp);
        return true;
      }
    return false;
  }

  private static PeakInfo selectPanelByPeak(ScriptInterface si,
                                            String peakScript) {
  	List<JSVPanelNode> panelNodes = si.getPanelNodes();
    if (panelNodes == null)
      return null;
    String file = Parser.getQuotedAttribute(peakScript, "file");
    String index = Parser.getQuotedAttribute(peakScript, "index");
    PeakInfo pi = null;
    for (int i = panelNodes.size(); --i >= 0;)
      panelNodes.get(i).jsvp.getPanelData().addPeakHighlight(null);
  	JSVPanel jsvp = si.getSelectedPanel();
    System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak looking for " + index + " " + file + " in " + jsvp);
    pi = jsvp.getPanelData().selectPeakByFileIndex(file, index);
    System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak pi = " + pi);
    if (pi != null) {
    	// found in current panel
      si.setNode(JSVPanelNode.findNode(jsvp, panelNodes), false);
    } else {
    	// must look elsewhere
    	System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak did not find it");
      for (int i = panelNodes.size(); --i >= 0;) {
        JSVPanelNode node = panelNodes.get(i);
      	System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak looking at node " + i + " " + node.fileName);
        if ((pi = node.jsvp.getPanelData().selectPeakByFileIndex(file, index)) != null) {
          System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak setting node " + i + " pi=" + pi);
          si.setNode(node, false);
          System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak setting node " + i + " set node done");
          break;
        }
      }
    }
    System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak finally pi = " + pi);
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
  	// trouble here is with round trip when peaks are clicked in rapid succession.
  	
    PeakInfo pi;
    if (eventObj instanceof PeakInfo) {
      // this is a call from the PEAK command, above.
      pi = (PeakInfo) eventObj;
      JSVPanel jsvp = si.getSelectedPanel();
      PeakInfo pi2 = jsvp.getPanelData().findMatchingPeakInfo(pi);
      if (pi2 == null) {
        if (!"ALL".equals(pi.getTitle()))
          return;
        List<JSVPanelNode> panelNodes = si.getPanelNodes();
        JSVPanelNode node = null;
        for (int i = 0; i < panelNodes.size(); i++)
          if ((pi2 = panelNodes.get(i).jsvp.getPanelData().findMatchingPeakInfo(
              pi)) != null) {
            node = panelNodes.get(i);
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
    // the above line is what caused problems with GC/MS selection 
    syncToJmol(si, pi);
    //System.out.println(Thread.currentThread() + "processPeakEvent --selectSpectrum "  + pi);
    if (pi.isClearAll()) // was not in app version??
      si.getSelectedPanel().repaint();
    else
      si.getSelectedPanel().getPanelData().selectSpectrum(pi.getFilePath(),
          pi.getType(), pi.getModel(), true);
    si.checkCallbacks(pi.getTitle());

  }

	private static void syncToJmol(ScriptInterface si, PeakInfo pi) {
  	si.setReturnFromJmolModel(pi.getModel());
  	si.syncToJmol(JSViewer.jmolSelect(pi));
	}

	public static void sendPanelChange(ScriptInterface si, JSVPanel jsvp) {
    PeakInfo pi = jsvp.getSpectrum().getSelectedPeak();
    if (pi == null)
      pi = jsvp.getSpectrum().getModelPeakInfoForAutoSelectOnLoad();
    if (pi == null)
      pi = jsvp.getSpectrum().getBasePeakInfo();
    si.getSelectedPanel().getPanelData().addPeakHighlight(pi);
    System.out.println(Thread.currentThread() + "JSViewer sendFrameChange "  + jsvp);
    syncToJmol(si, pi);
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
    List<JSVPanelNode> panelNodes = si.getPanelNodes();
    for (int i = 0; i < panelNodes.size(); i++) {
      JSVPanel jsvp = panelNodes.get(i).jsvp;
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
	 * @param panelNodes
	 * @param value
	 * @param speclist
	 * @param selectedPanel
	 * @param prefix
	 * @param isView
	 * @return comma-separated list, for the title
	 */
	private static String fillSpecList(ScriptInterface si,
			List<JSVPanelNode> panelNodes, String value, List<JDXSpectrum> speclist,
			JSVPanel selectedPanel, String prefix, boolean isView) {
		List<String> list;
		List<String> list0 = null;
		boolean isNone = (value.equalsIgnoreCase("NONE"));
		if (isNone || value.equalsIgnoreCase("all"))
			value = "*";
		if (value.indexOf("*") < 0 && !isView) {
			// replace "3.1.1" with "3.1*1"
			String[] tokens = value.split(" ");
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < tokens.length; i++) {
				int pt = tokens[i].indexOf('.'); 
				if (pt != tokens[i].lastIndexOf('.'))
					tokens[i] = tokens[i].substring(0, pt + 1) + tokens[i].substring(pt + 1).replace('.', '*');
				sb.append(tokens[i]).append(" ");
			}
			value = sb.toString().trim();
		}
		value = TextFormat.simpleReplace(value, "*", " * ");
		if (value.equals(" * ")) {
			list = ScriptToken.getTokens(JSVPanelNode.getSpectrumListAsString(
					panelNodes, !isView));
		} else if (value.startsWith("\"")) {
			list = ScriptToken.getTokens(value);
		} else {
			value = TextFormat.simpleReplace(value, "-", " - ");
			list = ScriptToken.getTokens(value);
			list0 = ScriptToken.getTokens(JSVPanelNode.getSpectrumListAsString(
					panelNodes, !isView));
			if (list0.size() == 0)
				return null;
		}

		String id0 = (selectedPanel == null ? prefix : JSVPanelNode.findNode(
				selectedPanel, panelNodes).id);
		id0 = id0.substring(0, id0.indexOf(".") + 1);
		StringBuffer sb = new StringBuffer();
		int n = list.size();
		String idLast = null;
		for (int i = 0; i < n; i++) {
			String id = list.get(i);
			double userYFactor = Double.NaN; // also subspectrum number
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
					speclist.add(JSVPanelNode.findNodeById(idLast = list0.get(pt++),
							panelNodes).jsvp.getSpectrumAt(0));
					sb.append(",").append(idLast);
				}
				continue;
			}
			JSVPanelNode node;
			if (id.startsWith("\"")) {
				id = TextFormat.trim(id, "\"");
		    for (int j = 0; j < panelNodes.size(); j++) {
		     node = panelNodes.get(j);
		      if (node.fileName != null && node.fileName.startsWith(id) 
		      		|| node.frameTitle != null && node.frameTitle.startsWith(id)) {
		      	addSpecToList(node, userYFactor, speclist, isView);
		      	sb.append(",").append(node.id);
		      }
		    }
		    continue;
			} else {
				if (!id.contains("."))
					id = id0 + id;
				node = JSVPanelNode.findNodeById(id, panelNodes);
			}
			if (node == null)
				continue;
			idLast = id;
    	addSpecToList(node, userYFactor, speclist, isView);
			sb.append(",").append(id);
		}
		if (isView && speclist.size() == 1) {
			JSVPanelNode node = JSVPanelNode.findNodeById(idLast, panelNodes);
			if (node != null) {
				si.setNode(node, true);
				// possibility of a problem here -- we are not communicating with Jmol
				// our model changes.
				speclist.clear();
			}
		}
		return (isNone ? "NONE" : sb.length() > 0 ? sb.toString().substring(1)
				: null);
	}
	
	private static void addSpecToList(JSVPanelNode node, double userYFactor,
			List<JDXSpectrum> speclist, boolean isView) {
		if (isView) {
			JDXSpectrum spec = node.jsvp.getSpectrumAt(0);
			spec.setUserYFactor(Double.isNaN(userYFactor) ? 1 : userYFactor);
			if (spec != null)
				speclist.add(spec);
		} else {
			node.jsvp.getPanelData().selectFromEntireSet(Double.isNaN(userYFactor) ? -1 : (int) userYFactor);
		}
	}

	public static void zoomTo(ScriptInterface si, int mode) {
		JSVPanel jsvp = si.getSelectedPanel();
		if (jsvp == null)
			return;
		PanelData pd = jsvp.getPanelData();
		switch (mode) {
		case 1:
			pd.nextView();
			break;
		case -1:
			pd.previousView();
			break;
		case Integer.MAX_VALUE:
			pd.resetView();
			break;
		default:
			pd.clearAllView();
			break;
		}
	}

	public static void showColorMessage(ScriptInterface si) {
		JSVPanel jsvp = si.getSelectedPanel();
		jsvp.showMessage(jsvp.getPanelData().getSolutionColorHtml(), "Predicted Colour");
	}

}
