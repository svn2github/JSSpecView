package jspecview.common;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jspecview.common.Annotation.AType;
import jspecview.common.JDXSpectrum.IRMode;
import jspecview.common.PanelData.LinkMode;
import jspecview.source.JDXSource;
import jspecview.util.JSVEscape;
import jspecview.util.JSVLogger;
import jspecview.util.JSVParser;
import jspecview.util.JSVSB;
import jspecview.util.JSVTextFormat;

/**
 * This static class encapsulates all general functionality of applet and app.
 * Most methods include ScriptInterface parameter, which will be
 * JSVAppletPrivate, JSVAppletPrivatePro, or MainFrame.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class JSViewer {

	// ALL STATIC METHODS
	
	private final static int NLEVEL_MAX = 100;
	
  public static boolean runScriptNow(ScriptInterface si, String script) {
		si.incrementViewCount(1);		
    if (script == null)
      script = "";
    String msg = null;
    script = script.trim();
    if (JSVLogger.debugging)
      JSVLogger.info("RUNSCRIPT " + script);
    JSVPanel jsvp = si.getSelectedPanel();
    boolean isOK = true;
    int nErrorsLeft = 10;
    ScriptTokenizer commandTokens = new ScriptTokenizer(script, true);
    while (commandTokens.hasMoreTokens() && nErrorsLeft > 0) {
      String token = commandTokens.nextToken();
      // now split the key/value pair
      ScriptTokenizer eachParam = new ScriptTokenizer(token, false);
      String key = ScriptToken.getKey(eachParam);
      if (key == null)
        continue;
      ScriptToken st = ScriptToken.getScriptToken(key);
      String value = ScriptToken.getValue(st, eachParam, token);
      JSVLogger.info("KEY-> " + key + " VALUE-> " + value + " : " + st);
      try {
        switch (st) {
        case UNKNOWN:
          JSVLogger.info("Unrecognized parameter: " + key);
          --nErrorsLeft;
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
          JSVLogger
              .setLogLevel(value.toLowerCase().equals("high") ? JSVLogger.LEVEL_DEBUGHIGH
                  : Parameters.isTrue(value) ? JSVLogger.LEVEL_DEBUG : JSVLogger.LEVEL_INFO);
          break;
        case EXPORT:
          msg = si.execExport(jsvp, value);
          return false;
        case FINDX:
        	if (jsvp != null)
        		jsvp.getPanelData().findX(null, Double.parseDouble(value));
        	break; 
        case GETPROPERTY:
        	Map<String, Object> info = (jsvp == null ? null : getPropertyAsJavaObject(si, value));
        	if (info != null)
        		jsvp.showMessage(JSVEscape.toJSON(null, info, true), value);
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
        case JSV:
        	syncScript(si, JSVTextFormat.trimQuotes(value));
        	break;
        case LABEL:
          if (jsvp != null)
            jsvp.getPanelData().addAnnotation(ScriptToken.getTokens(value));
          break;
        case LINK:
        	if (jsvp != null)
      			jsvp.getPanelData().linkSpectra(LinkMode.getMode(value));
          break;
        case LOAD:
          msg = si.execLoad(value);
          jsvp = si.getSelectedPanel();
          break;
        case LOADIMAGINARY:
        	si.setLoadImaginary(Parameters.isTrue(value));
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
        	si.print(value);
        	break;
        case SCALEBY:
        	scaleSelectedBy(si.getPanelNodes(), value);
        	break;
        case SCRIPT:
        	String s = si.getFileAsString(value);
        	if (s != null && si.incrementScriptLevelCount(0) < NLEVEL_MAX)
        		runScriptNow(si, s);
        	break;
        case SELECT:
          execSelect(si, value);
          break;
        case SETPEAK:
        	// setpeak NONE     Double.NaN,       Double.MAX_VALUE
        	// shiftx  NONE     Double.MAX_VALUE, Double.NaN
        	// setpeak x.x      Double.NaN,       value
        	// setx 	 x.x			Double.MIN_VALUE, value
        	// shiftx  x.x      value,            Double.NaN
        	// setpeak  ?       Double.NaN,       Double.MIN_VALUE
        	if (jsvp != null)
        		jsvp.getPanelData().shiftSpectrum(Double.NaN, 
        				value.equalsIgnoreCase("NONE") ? Double.MAX_VALUE 
        						: value.equalsIgnoreCase("?") ? Double.MIN_VALUE 
        								: Double.parseDouble(value));
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
        case SHOWMEASUREMENTS:
        	if (jsvp == null)
        		break;
        	jsvp.getPanelData().showAnnotation(AType.Measurements, Parameters.getTFToggle(value));
        	break;
        case SHOWPEAKLIST:
        	if (jsvp == null)
        		break;
        	jsvp.getPanelData().showAnnotation(AType.PeakList, Parameters.getTFToggle(value));
        	break;
        case SHOWINTEGRATION:
        	if (jsvp == null)
        		break;
        	jsvp.getPanelData().showAnnotation(AType.Integration, Parameters.getTFToggle(value));
        	//execIntegrate(si, null);
        	break;
        case SPECTRUM:
        case SPECTRUMNUMBER:
          jsvp = si.setSpectrum(value);
          if (jsvp == null)
            return false;
          break;
        case STACKOFFSETY:
        	int offset = JSVParser.parseInt("" + JSVParser.parseFloat(value));
        	if (jsvp != null&& offset != Integer.MIN_VALUE)
        		jsvp.getPanelData().setYStackOffsetPercent(offset);
        	break;
        case TEST:
          si.execTest(value);
          break;
        case OVERLAY: // deprecated
        case VIEW:
          execView(si, value, true);
          jsvp = si.getSelectedPanel();
          break;
        case YSCALE:
        	if (jsvp != null)
            setYScale(si, value, jsvp);
          break;
        case ZOOM:
        	if (jsvp != null)
          	isOK = execZoom(value, jsvp);
          break;
        }
      } catch (Exception e) {
      	System.out.println(e.getMessage());
        JSVLogger.error(e.getMessage());
        if (JSVLogger.debugging)
        	e.printStackTrace();
        isOK = false;
        --nErrorsLeft;
      }
    }
		si.incrementViewCount(-1);		
    si.execScriptComplete(msg, true);
	  //si.getSelectedPanel().requestFocusInWindow(); // could be CLOSE ALL
    return isOK;
  }

	private static void execPeak(ScriptInterface si, String value) {
    try {
      List<String> tokens = ScriptToken.getTokens(value);
      value = " type=\"" + tokens.get(0).toUpperCase() + "\" _match=\""
          + JSVTextFormat.trimQuotes(tokens.get(1).toUpperCase()) + "\"";
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
		if (value.indexOf("=") < 0) {
			if (jsvp != null)
				jsvp.getPanelData().getPeakListing(null, b);
		} else {
			List<String> tokens = ScriptToken.getTokens(value);
			for (int i = tokens.size(); --i >= 0;) {
				String token = tokens.get(i);
				int pt = token.indexOf("=");
				if (pt <= 0)
					continue;
				String key = token.substring(0, pt);
				value = token.substring(pt + 1);
				try {
					if (key.startsWith("thr")) {
						p.peakListThreshold = Double.valueOf(value).doubleValue();
					} else if (key.startsWith("int")) {
						p.peakListInterpolation = (value.equalsIgnoreCase("none") ? "NONE"
								: "parabolic");
					}
					if (jsvp != null)
						jsvp.getPanelData().getPeakListing(p, Boolean.TRUE);
				} catch (Exception e) {
					// ignore
				}
			}
		}

	}

	private static boolean execZoom(String value, JSVPanel jsvp) {
		double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
		List<String> tokens;
		tokens = ScriptToken.getTokens(value);
		switch (tokens.size()) {
		default:
			return false;
		case 1:
			zoomTo(jsvp, tokens.get(0));
			return true;
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

	private static void execSelect(ScriptInterface si, String value) {
    List<JSVPanelNode> nodes = si.getPanelNodes();
    for (int i = nodes.size(); --i >= 0;)
    	nodes.get(i).jsvp.getPanelData().selectFromEntireSet(Integer.MIN_VALUE);
    List<JDXSpectrum> speclist = new ArrayList<JDXSpectrum>();
    fillSpecList(si, value, speclist, false);
	}

	public static void execView(ScriptInterface si, String value, boolean fromScript) {
    List<JDXSpectrum> speclist = new ArrayList<JDXSpectrum>();
    String strlist = fillSpecList(si, value, speclist, true);
    if (speclist.size() > 0)
      si.openDataOrFile(null, strlist, speclist, strlist, -1, -1, false);
    if (!fromScript) {
    	si.validateAndRepaint();
    }
	}

	private static void execIRMode(ScriptInterface si, String value) {
    IRMode mode = IRMode.getMode(value); // T, A, or TOGGLE
    PanelData pd = si.getSelectedPanel().getPanelData();
    JDXSpectrum spec = pd.getSpectrum();
    JDXSpectrum spec2 = JDXSpectrum.taConvert(spec, mode);
    if (spec2 == spec)
      return;
    pd.setSpectrum(spec2);
    si.setIRMode(mode);
    //jsvp.doRepaint();
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
    jsvp.doRepaint();
  }

	@SuppressWarnings("incomplete-switch")
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
      JDXSpectrum spec = jsvp.getPanelData().getSpectrum();
      for (int i = panelNodes.size(); --i >= 0;) {
        JSVPanelNode node = panelNodes.get(i);
        if (node.source != currentSource)
          continue;
        if (JDXSpectrum.areXScalesCompatible(spec, node.getSpectrum(), false, false))
          node.jsvp.getPanelData().setZoom(0, y1, 0, y2);
      }
    } else {
      jsvp.getPanelData().setZoom(0, y1, 0, y2);
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
    JSVDialog legend = node.legend;
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
      jsvp.doRepaint();
    }
  }

  private static String testScript = "<PeakData  index=\"1\" title=\"\" model=\"~1.1\" type=\"1HNMR\" xMin=\"3.2915\" xMax=\"3.2965\" atoms=\"15,16,17,18,19,20\" multiplicity=\"\" integral=\"1\"> src=\"JPECVIEW\" file=\"http://SIMULATION/$caffeine\"";
  /**
   * incoming script processing of <PeakAssignment file="" type="xxx"...> record
   * from Jmol
   * @param si 
   * @param peakScript 
   */

  public static void syncScript(ScriptInterface si, String peakScript) {
  	if (peakScript.equals("TEST"))
  		peakScript = testScript;
    JSVLogger.info(Thread.currentThread() + "Jmol>JSV " + peakScript);
    if (peakScript.indexOf("<PeakData") < 0) {
      runScriptNow(si, peakScript);
      if (peakScript.indexOf("#SYNC_PEAKS") >= 0) {
      	JDXSource source = si.getCurrentSource();
      	if (source == null)
      		return;
      	try {
      	String file = "file=" + JSVEscape.escape(source.getFilePath());
      	ArrayList<PeakInfo> peaks = source.getSpectra().get(0).getPeakList();
      	JSVSB sb = new JSVSB();
      	sb.append("[");
      	int n = peaks.size();
      	for (int i = 0; i < n; i++) {
      		String s = peaks.get(i).toString();
      		s = s + " " + file;
      		sb.append(JSVEscape.escape(s));
      		if (i > 0)
      			sb.append(",");
      	}
      	sb.append("]");
      	si.syncToJmol("Peaks: " + sb);
      	} catch (Exception e) {
      		// ignore bad structures -- no spectrum
      	}
      }
      return;
    }
    // todo: why the quotes??
    peakScript = JSVTextFormat.simpleReplace(peakScript, "\\\"", "");
    String file = JSVParser.getQuotedAttribute(peakScript, "file");
    System.out.println("file2=" + file);
    String index = JSVParser.getQuotedAttribute(peakScript, "index");
    if (file == null || index == null)
      return;
    String model = JSVParser.getQuotedAttribute(peakScript, "model");
    String jmolSource = JSVParser.getQuotedAttribute(peakScript, "src");
    String modelSent = (jmolSource != null && jmolSource.startsWith("Jmol") ? null : si.getReturnFromJmolModel());
    
    if (model != null && modelSent != null && !model.equals(modelSent)) {
    	JSVLogger.info("JSV ignoring model " + model + "; should be " + modelSent);
    	return;
    }
    si.setReturnFromJmolModel(null);
    if (si.getPanelNodes().size() == 0 || !checkFileAlreadyLoaded(si, file)) {
      JSVLogger.info("file " + file + " not found -- JSViewer closing all and reopening");
      si.syncLoad(file);
    }
    //System.out.println(Thread.currentThread() + "syncscript jsvp=" + si.getSelectedPanel() + " s0=" + si.getSelectedPanel().getSpectrum());
    PeakInfo pi = selectPanelByPeak(si, peakScript);
    //System.out.println(Thread.currentThread() + "syncscript pi=" + pi);
    JSVPanel jsvp = si.getSelectedPanel();
    //System.out.println(Thread.currentThread() + "syncscript jsvp=" + jsvp);
    String type = JSVParser.getQuotedAttribute(peakScript, "type");
    //System.out.println(Thread.currentThread() + "syncscript --selectSpectrum2 "  + pi + " " + type + " "  + model + " s=" + jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
    jsvp.getPanelData().selectSpectrum(file, type, model, true);
    //System.out.println(Thread.currentThread() + "syncscript --selectSpectrum3 "  + pi + " " + type + " "  + model + " s=" + jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
    si.sendPanelChange(jsvp);
    //System.out.println(Thread.currentThread() + "syncscript --selectSpectrum4 "  + pi + " " + type + " "  + model + " s=" + jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
    jsvp.getPanelData().addPeakHighlight(pi);
    //System.out.println(Thread.currentThread() + "syncscript --selectSpectrum5 "  + pi + " " + type + " "  + model + " s=" + jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
    jsvp.doRepaint();
    // round trip this so that Jmol highlights all equivalent atoms
    // and appropriately starts or clears vibration
    if (jmolSource == null || (pi != null && pi.getAtoms() != null))
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
    String file = JSVParser.getQuotedAttribute(peakScript, "file");
    String index = JSVParser.getQuotedAttribute(peakScript, "index");
    PeakInfo pi = null;
    for (int i = panelNodes.size(); --i >= 0;)
      panelNodes.get(i).jsvp.getPanelData().addPeakHighlight(null);
  	JSVPanel jsvp = si.getSelectedPanel();
    //System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak looking for " + index + " " + file + " in " + jsvp);
    pi = jsvp.getPanelData().selectPeakByFileIndex(file, index);
    System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak pi = " + pi);
    if (pi != null) {
    	// found in current panel
      si.setNode(JSVPanelNode.findNode(jsvp, panelNodes), false);
    } else {
    	// must look elsewhere
    	//System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak did not find it");
      for (int i = panelNodes.size(); --i >= 0;) {
        JSVPanelNode node = panelNodes.get(i);
      	//System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak looking at node " + i + " " + node.fileName);
        if ((pi = node.jsvp.getPanelData().selectPeakByFileIndex(file, index)) != null) {
          //System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak setting node " + i + " pi=" + pi);
          si.setNode(node, false);
          //System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak setting node " + i + " set node done");
          break;
        }
      }
    }
    //System.out.println(Thread.currentThread() + "JSViewer selectPanelByPeak finally pi = " + pi);
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
      si.getSelectedPanel().doRepaint();
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
		PanelData pd = jsvp.getPanelData();
		JDXSpectrum spec = pd.getSpectrum();
    PeakInfo pi = spec.getSelectedPeak();
    if (pi == null)
      pi = spec.getModelPeakInfoForAutoSelectOnLoad();
    if (pi == null)
      pi = spec.getBasePeakInfo();
    pd.addPeakHighlight(pi);
    JSVLogger.info(Thread.currentThread() + "JSViewer sendFrameChange "  + jsvp);
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
    script = "Select: " + pi + " script=\"" + script;
    System.out.println("JSpecView jmolSelect " + script);
    return script;
  }

  public static void removeAllHighlights(ScriptInterface si) {
    JSVPanel jsvp = si.getSelectedPanel();
    if (jsvp != null) {
      jsvp.getPanelData().removeAllHighlights();
      jsvp.doRepaint();
    }
  }

  public static void removeHighlights(ScriptInterface si, double x1, double x2) {
    JSVPanel jsvp = si.getSelectedPanel();
    if (jsvp != null) {
      jsvp.getPanelData().removeHighlight(x1, x2);
      jsvp.doRepaint();
    }
  }

  public static Map<String, Object> getPropertyAsJavaObject(ScriptInterface si,
                                                            String key) {
  	boolean isAll = false;
  	if (key != null && key.toUpperCase().startsWith("ALL ") || "all".equalsIgnoreCase(key)) {
  		key = key.substring(3).trim();
  		isAll = true;
  	}
    if ("".equals(key))
      key = null;
    Map<String, Object>map = new Hashtable<String, Object>();
		Map<String, Object> map0 = si.getSelectedPanel().getPanelData().getInfo(true, key);
		if (!isAll && map0 != null)
			return map0;
		if (map0 != null)
		  map.put("current", map0);
   List<Map<String, Object>> info = new ArrayList<Map<String, Object>>();
    List<JSVPanelNode> panelNodes = si.getPanelNodes();
    for (int i = 0; i < panelNodes.size(); i++) {
      JSVPanel jsvp = panelNodes.get(i).jsvp;
      if (jsvp == null)
        continue;
			info.add(panelNodes.get(i).getInfo(key));      
    }
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
	 * @param value
	 * @param speclist
	 * @param isView
	 * @return comma-separated list, for the title
	 */
	private static String fillSpecList(ScriptInterface si, String value,
			List<JDXSpectrum> speclist, boolean isView) {

		List<JSVPanelNode> panelNodes = si.getPanelNodes();
		JSVPanel selectedPanel = si.getSelectedPanel();
		String prefix = "1.";
		List<String> list;
		List<String> list0 = null;
		boolean isNone = (value.equalsIgnoreCase("NONE"));
		if (isNone || value.equalsIgnoreCase("all"))
			value = "*";
		if (value.indexOf("*") < 0) {
			// replace "3.1.1" with "3.1*1"
			String[] tokens = value.split(" ");
			JSVSB sb = new JSVSB();
			for (int i = 0; i < tokens.length; i++) {
				int pt = tokens[i].indexOf('.');
				if (pt != tokens[i].lastIndexOf('.'))
					tokens[i] = tokens[i].substring(0, pt + 1)
							+ tokens[i].substring(pt + 1).replace('.', '_');
				sb.append(tokens[i]).append(" ");
			}
			value = sb.toString().trim();
		}
		if (value.equals("*")) {
			list = ScriptToken.getTokens(JSVPanelNode
					.getSpectrumListAsString(panelNodes));
		} else if (value.startsWith("\"")) {
			list = ScriptToken.getTokens(value);
		} else {
			value = JSVTextFormat.simpleReplace(value, "_", " _ ");
			value = JSVTextFormat.simpleReplace(value, "-", " - ");
			list = ScriptToken.getTokens(value);
			list0 = ScriptToken.getTokens(JSVPanelNode
					.getSpectrumListAsString(panelNodes));
			if (list0.size() == 0)
				return null;
		}

		String id0 = (selectedPanel == null ? prefix : JSVPanelNode.findNode(
				selectedPanel, panelNodes).id);
		id0 = id0.substring(0, id0.indexOf(".") + 1);
		JSVSB sb = new JSVSB();
		int n = list.size();
		String idLast = null;
		for (int i = 0; i < n; i++) {
			String id = list.get(i);
			double userYFactor = Double.NaN;
			int isubspec = -1;
			if (i + 1 < n && list.get(i + 1).equals("*")) {
				i += 2;
				userYFactor = Double.parseDouble(list.get(i));
			} else if (i + 1 < n && list.get(i + 1).equals("_")) {
				i += 2;
				isubspec = Integer.parseInt(list.get(i));
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
							panelNodes).jsvp.getPanelData().getSpectrumAt(0));
					sb.append(",").append(idLast);
				}
				continue;
			}
			JSVPanelNode node;
			if (id.startsWith("\"")) {
				id = JSVTextFormat.trim(id, "\"");
				for (int j = 0; j < panelNodes.size(); j++) {
					node = panelNodes.get(j);
					if (node.fileName != null && node.fileName.startsWith(id)
							|| node.frameTitle != null && node.frameTitle.startsWith(id)) {
						addSpecToList(node.jsvp.getPanelData(), userYFactor, -1, speclist,
								isView);
						sb.append(",").append(node.id);
					}
				}
				continue;
			}
			if (!id.contains("."))
				id = id0 + id;
			node = JSVPanelNode.findNodeById(id, panelNodes);
			if (node == null)
				continue;
			idLast = id;
			addSpecToList(node.jsvp.getPanelData(), userYFactor, isubspec, speclist,
					isView);
			sb.append(",").append(id);
			if (isubspec > 0)
				sb.append(".").appendI(isubspec);
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

	private static void addSpecToList(PanelData pd, double userYFactor, int isubspec,
			List<JDXSpectrum> list, boolean isView) {
		if (isView) {
			JDXSpectrum spec = pd.getSpectrumAt(0);
			spec.setUserYFactor(Double.isNaN(userYFactor) ? 1 : userYFactor);
			pd.addToList(isubspec - 1, list);
		} else {
			pd.selectFromEntireSet(isubspec - 1);
		}
	}

	private static void zoomTo(JSVPanel jsvp, String value) {
		PanelData pd = jsvp.getPanelData();
			if (value.equalsIgnoreCase("next")) {
				pd.nextView();
		} else if (value.toLowerCase().startsWith("prev")) {
			pd.previousView();
			
		} else if (value.equalsIgnoreCase("out")) {
			pd.resetView();
			
		} else if (value.equalsIgnoreCase("clear")) {
			pd.clearAllView();
		}
	}

	public static void showColorMessage(ScriptInterface si) {
		JSVPanel jsvp = si.getSelectedPanel();
		jsvp.showMessage(jsvp.getPanelData().getSolutionColorHtml(), "Predicted Colour");
	}

}
