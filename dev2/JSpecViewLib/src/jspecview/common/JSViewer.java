package jspecview.common;

import org.jmol.api.ApiPlatform;
import org.jmol.util.JmolList;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import java.util.Map;

import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.SB;
import org.jmol.util.Txt;

import jspecview.api.JSVDialog;
import jspecview.api.JSVFileHelper;
import jspecview.api.JSVMainPanel;
import jspecview.api.JSVPanel;
import jspecview.api.JSVTree;
import jspecview.api.JSVTreeNode;
import jspecview.api.ScriptInterface;
import jspecview.common.Annotation.AType;
import jspecview.common.JDXSpectrum.IRMode;
import jspecview.common.PanelData.LinkMode;
import jspecview.source.JDXSource;
import jspecview.util.JSVEscape;
import jspecview.util.JSVFileManager;

/**
 * This class encapsulates all general functionality of applet and app. Most
 * methods include ScriptInterface parameter, which will be JSVAppletPrivate,
 * JSVAppletPrivatePro, or MainFrame.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class JSViewer {

  public final static String sourceLabel = "Original...";

  public final static int FILE_OPEN_OK = 0;
	public final static int FILE_OPEN_ALREADY = -1;
	// private final static int FILE_OPEN_URLERROR = -2;
	public final static int FILE_OPEN_ERROR = -3;
	public final static int FILE_OPEN_NO_DATA = -4;
	public static final int OVERLAY_DIALOG = -1;
	public static final int OVERLAY_OFFSET = 99;

	private static String testScript = "<PeakData  index=\"1\" title=\"\" model=\"~1.1\" type=\"1HNMR\" xMin=\"3.2915\" xMax=\"3.2965\" atoms=\"15,16,17,18,19,20\" multiplicity=\"\" integral=\"1\"> src=\"JPECVIEW\" file=\"http://SIMULATION/$caffeine\"";

	private final static int NLEVEL_MAX = 100;


	public ScriptInterface si;
	public JSVTree spectraTree;
	public JDXSource              currentSource;
  public JmolList<JSVPanelNode> panelNodes;  
	public ColorParameters        parameters;
	public RepaintManager         repaintManager;
	public JSVPanel               selectedPanel;
	public JSVMainPanel           viewPanel; // alias for spectrumPanel
	public Properties 						properties; // application only
	public JmolList<String>       scriptQueue;
	public JSVFileHelper fileHelper;

	public boolean isApplet;
	public boolean isJS;
	public boolean isSigned;

	public void setProperty(String key, String value) {
		if (properties != null)
			properties.setProperty(key, value);
	}


	/**
	 * @param si 
	 * @param isApplet  
	 * @param isJS 
	 */
	public JSViewer(ScriptInterface si, boolean isApplet, boolean isJS) {
		this.si = si;
		this.isApplet = isApplet;
		this.isJS = isApplet && isJS;
		this.isSigned = si.isSigned();
	}

	public boolean runScriptNow(String script) {
		si.siIncrementViewCount(1);
		if (script == null)
			script = "";
		String msg = null;
		script = script.trim();
		if (Logger.debugging)
			Logger.info("RUNSCRIPT " + script);
		JSVPanel jsvp = selectedPanel;
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
			Logger.info("KEY-> " + key + " VALUE-> " + value + " : " + st);
			try {
				switch (st) {
				case UNKNOWN:
					Logger.info("Unrecognized parameter: " + key);
					--nErrorsLeft;
					break;
				default:
					parameters.set(jsvp, st, value);
					si.siUpdateBoolean(st, Parameters.isTrue(value));
					break;
				case PEAKCALLBACKFUNCTIONNAME:
				case SYNCCALLBACKFUNCTIONNAME:
				case COORDCALLBACKFUNCTIONNAME:
				case LOADFILECALLBACKFUNCTIONNAME:
					si.siExecSetCallback(st, value);
					break;
				case AUTOINTEGRATE:
					si.siExecSetAutoIntegrate(Parameters.isTrue(value));
					break;
				case CLOSE:
					si.siExecClose(value, true);
					jsvp = selectedPanel;
					break;
				case DEBUG:
					Logger
							.setLogLevel(value.toLowerCase().equals("high") ? Logger.LEVEL_DEBUGHIGH
									: Parameters.isTrue(value) ? Logger.LEVEL_DEBUG
											: Logger.LEVEL_INFO);
					break;
				case EXPORT:
					msg = si.siExecExport(jsvp, value);
					return false;
				case FINDX:
					if (jsvp != null)
						jsvp.getPanelData().findX(null, Double.parseDouble(value));
					break;
				case GETPROPERTY:
					Map<String, Object> info = (jsvp == null ? null
							: getPropertyAsJavaObject(value));
					if (info != null)
						jsvp.showMessage(JSVEscape.toJSON(null, info, true), value);
					break;
				case GETSOLUTIONCOLOR:
					if (jsvp != null)
						showColorMessage();
					break;
				case HIDDEN:
					si.siExecHidden(Parameters.isTrue(value));
					break;
				case INTEGRALOFFSET:
				case INTEGRALRANGE:
					execSetIntegralParameter(st, Double.parseDouble(value));
					break;
				case INTEGRATION:
				case INTEGRATE:
					if (jsvp != null)
						execIntegrate(value);
					break;
				case INTEGRATIONRATIOS:
					si.siSetIntegrationRatios(value);
					if (jsvp != null)
						execIntegrate(null);
					break;
				case INTERFACE:
					si.siExecSetInterface(value);
					break;
				case IRMODE:
					if (jsvp == null)
						continue;
					execIRMode(value);
					break;
				case JMOL:
					si.syncToJmol(value);
					break;
				case JSV:
					syncScript(Txt.trimQuotes(value));
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
					msg = si.siExecLoad(value);
					jsvp = selectedPanel;
					break;
				case LOADIMAGINARY:
					si.siSetLoadImaginary(Parameters.isTrue(value));
					break;
				case OVERLAYSTACKED:
					if (jsvp != null)
						jsvp.getPanelData().splitStack(!Parameters.isTrue(value));
					break;
				case PEAK:
					execPeak(value);
					break;
				case PEAKLIST:
					execPeakList(value);
					break;
				case PRINT:
					if (jsvp == null)
						continue;
					si.siPrintPDF(value);
					break;
				case SCALEBY:
					scaleSelectedBy(panelNodes, value);
					break;
				case SCRIPT:
					String s = si.siSetFileAsString(value);
					if (s != null && si.siIncrementScriptLevelCount(0) < NLEVEL_MAX)
						runScriptNow(s);
					break;
				case SELECT:
					execSelect(value);
					break;
				case SETPEAK:
					// setpeak NONE Double.NaN, Double.MAX_VALUE
					// shiftx NONE Double.MAX_VALUE, Double.NaN
					// setpeak x.x Double.NaN, value
					// setx x.x Double.MIN_VALUE, value
					// shiftx x.x value, Double.NaN
					// setpeak ? Double.NaN, Double.MIN_VALUE
					if (jsvp != null)
						jsvp.getPanelData().shiftSpectrum(
								Double.NaN,
								value.equalsIgnoreCase("NONE") ? Double.MAX_VALUE : value
										.equalsIgnoreCase("?") ? Double.MIN_VALUE : Double
										.parseDouble(value));
					break;
				case SETX:
					if (jsvp != null)
						jsvp.getPanelData().shiftSpectrum(Double.MIN_VALUE,
								Double.parseDouble(value));
					break;
				case SHIFTX:
					if (jsvp != null)
						jsvp.getPanelData().shiftSpectrum(
								value.equalsIgnoreCase("NONE") ? Double.MAX_VALUE : Double
										.parseDouble(value), Double.NaN);
					break;
				case SHOWMEASUREMENTS:
					if (jsvp == null)
						break;
					jsvp.getPanelData().showAnnotation(AType.Measurements,
							Parameters.getTFToggle(value));
					break;
				case SHOWPEAKLIST:
					if (jsvp == null)
						break;
					jsvp.getPanelData().showAnnotation(AType.PeakList,
							Parameters.getTFToggle(value));
					break;
				case SHOWINTEGRATION:
					if (jsvp == null)
						break;
					jsvp.getPanelData().showAnnotation(AType.Integration,
							Parameters.getTFToggle(value));
					// execIntegrate(null);
					break;
				case SPECTRUM:
				case SPECTRUMNUMBER:
					jsvp = setSpectrum(value);
					if (jsvp == null)
						return false;
					break;
				case STACKOFFSETY:
					int offset = Parser.parseInt("" + Parser.parseFloat(value));
					if (jsvp != null && offset != Integer.MIN_VALUE)
						jsvp.getPanelData().setYStackOffsetPercent(offset);
					break;
				case TEST:
					si.siExecTest(value);
					break;
				case OVERLAY: // deprecated
				case VIEW:
					execView(value, true);
					jsvp = selectedPanel;
					break;
				case YSCALE:
					if (jsvp != null)
						setYScale(value, jsvp);
					break;
				case ZOOM:
					if (jsvp != null)
						isOK = execZoom(value, jsvp);
					break;
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
				Logger.error(e.getMessage());
				if (Logger.debugging)
					e.printStackTrace();
				isOK = false;
				--nErrorsLeft;
			}
		}
		si.siIncrementViewCount(-1);
		si.siExecScriptComplete(msg, true);
		// si.getSelectedPanel().requestFocusInWindow(); // could be CLOSE ALL
		return isOK;
	}

	private void execPeak(String value) {
		try {
			JmolList<String> tokens = ScriptToken.getTokens(value);
			value = " type=\"" + tokens.get(0).toUpperCase() + "\" _match=\""
					+ Txt.trimQuotes(tokens.get(1).toUpperCase()) + "\"";
			if (tokens.size() > 2 && tokens.get(2).equalsIgnoreCase("all"))
				value += " title=\"ALL\"";
			processPeakPickEvent(new PeakInfo(value), false); // false == true here
		} catch (Exception e) {
			// ignore
		}
	}

	private void execPeakList(String value) {
		JSVPanel jsvp = selectedPanel;
		ColorParameters p = parameters;
		Boolean b = Parameters.getTFToggle(value);
		if (value.indexOf("=") < 0) {
			if (jsvp != null)
				jsvp.getPanelData().getPeakListing(null, b);
		} else {
			JmolList<String> tokens = ScriptToken.getTokens(value);
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

	private boolean execZoom(String value, JSVPanel jsvp) {
		double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
		JmolList<String> tokens;
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

	private void scaleSelectedBy(JmolList<JSVPanelNode> nodes, String value) {
		try {
			double f = Double.parseDouble(value);
			for (int i = nodes.size(); --i >= 0;)
				nodes.get(i).jsvp.getPanelData().scaleSelectedBy(f);
		} catch (Exception e) {
		}
	}

	private void execSelect(String value) {
		JmolList<JSVPanelNode> nodes = panelNodes;
		for (int i = nodes.size(); --i >= 0;)
			nodes.get(i).jsvp.getPanelData().selectFromEntireSet(Integer.MIN_VALUE);
		JmolList<JDXSpectrum> speclist = new JmolList<JDXSpectrum>();
		fillSpecList(value, speclist, false);
	}

	public void execView(String value, boolean fromScript) {
		JmolList<JDXSpectrum> speclist = new JmolList<JDXSpectrum>();
		String strlist = fillSpecList(value, speclist, true);
		if (speclist.size() > 0)
			si.siOpenDataOrFile(null, strlist, speclist, strlist, -1, -1, false);
		if (!fromScript) {
			si.siValidateAndRepaint();
		}
	}

	private void execIRMode(String value) {
		IRMode mode = IRMode.getMode(value); // T, A, or TOGGLE
		PanelData pd = selectedPanel.getPanelData();
		JDXSpectrum spec = pd.getSpectrum();
		JDXSpectrum spec2 = JDXSpectrum.taConvert(spec, mode);
		if (spec2 == spec)
			return;
		pd.setSpectrum(spec2);
		si.siSetIRMode(mode);
		// jsvp.doRepaint();
	}

	private void execIntegrate(String value) {
		JSVPanel jsvp = selectedPanel;
		if (jsvp == null)
			return;
		jsvp.getPanelData().checkIntegral(parameters, value);
		String integrationRatios = si.siGetIntegrationRatios();
		if (integrationRatios != null)
			jsvp.getPanelData().setIntegrationRatios(integrationRatios);
		si.siSetIntegrationRatios(null); // one time only
		jsvp.doRepaint();
	}

	@SuppressWarnings("incomplete-switch")
	private void execSetIntegralParameter(ScriptToken st, double value) {
		ColorParameters p = parameters;
		switch (st) {
		case INTEGRALRANGE:
			p.integralRange = value;
			break;
		case INTEGRALOFFSET:
			p.integralOffset = value;
			break;
		}
		JSVPanel jsvp = selectedPanel;
		if (jsvp == null)
			return;
		jsvp.getPanelData().checkIntegral(parameters, "update");
	}

	private void setYScale(String value, JSVPanel jsvp) {
		JmolList<String> tokens = ScriptToken.getTokens(value);
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
				if (JDXSpectrum.areXScalesCompatible(spec, node.getSpectrum(), false,
						false))
					node.jsvp.getPanelData().setZoom(0, y1, 0, y2);
			}
		} else {
			jsvp.getPanelData().setZoom(0, y1, 0, y2);
		}
	}

	public void setOverlayLegendVisibility(JSVPanel jsvp, boolean showLegend) {
		JSVPanelNode node = JSVPanelNode.findNode(jsvp, panelNodes);
		for (int i = panelNodes.size(); --i >= 0;)
			showOverlayLegend(panelNodes.get(i), panelNodes.get(i) == node
					&& showLegend);
	}

	private void showOverlayLegend(JSVPanelNode node, boolean visible) {
		JSVDialog legend = node.legend;
		if (legend == null && visible) {
			legend = node.setLegend(node.jsvp.getPanelData()
					.getNumberOfSpectraInCurrentSet() > 1
					&& node.jsvp.getPanelData().getNumberOfGraphSets() == 1 ? si.siNewDialog("legend", node.jsvp) : null);
		}
		if (legend != null)
			legend.setVisible(visible);
	}

	// / from JavaScript

	public void addHighLight(double x1, double x2, int r, int g, int b, int a) {
		JSVPanel jsvp = selectedPanel;
		if (jsvp != null) {
			jsvp.getPanelData().addHighlight(null, x1, x2, null, r, g, b, a);
			jsvp.doRepaint();
		}
	}

	/**
	 * incoming script processing of <PeakAssignment file="" type="xxx"...> record
	 * from Jmol
	 * 
	 * @param peakScript
	 */

	public void syncScript(String peakScript) {
		if (peakScript.equals("TEST"))
			peakScript = testScript;
		Logger.info(Thread.currentThread() + "Jmol>JSV " + peakScript);
		if (peakScript.indexOf("<PeakData") < 0) {
			runScriptNow(peakScript);
			if (peakScript.indexOf("#SYNC_PEAKS") >= 0) {
				JDXSource source = currentSource;
				if (source == null)
					return;
				try {
					String file = "file=" + JSVEscape.eS(source.getFilePath());
					JmolList<PeakInfo> peaks = source.getSpectra().get(0).getPeakList();
					SB sb = new SB();
					sb.append("[");
					int n = peaks.size();
					for (int i = 0; i < n; i++) {
						String s = peaks.get(i).toString();
						s = s + " " + file;
						sb.append(JSVEscape.eS(s));
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
		peakScript = Txt.simpleReplace(peakScript, "\\\"", "");
		String file = Parser.getQuotedAttribute(peakScript, "file");
		System.out.println("file2=" + file);
		String index = Parser.getQuotedAttribute(peakScript, "index");
		if (file == null || index == null)
			return;
		String model = Parser.getQuotedAttribute(peakScript, "model");
		String jmolSource = Parser.getQuotedAttribute(peakScript, "src");
		String modelSent = (jmolSource != null && jmolSource.startsWith("Jmol") ? null
				: si.siGetReturnFromJmolModel());

		if (model != null && modelSent != null && !model.equals(modelSent)) {
			Logger.info("JSV ignoring model " + model + "; should be " + modelSent);
			return;
		}
		si.siSetReturnFromJmolModel(null);
		if (panelNodes.size() == 0 || !checkFileAlreadyLoaded(file)) {
			Logger.info("file " + file
					+ " not found -- JSViewer closing all and reopening");
			si.siSyncLoad(file);
		}
		// System.out.println(Thread.currentThread() + "syncscript jsvp=" +
		// si.getSelectedPanel() + " s0=" + si.getSelectedPanel().getSpectrum());
		PeakInfo pi = selectPanelByPeak(peakScript);
		// System.out.println(Thread.currentThread() + "syncscript pi=" + pi);
		JSVPanel jsvp = selectedPanel;
		// System.out.println(Thread.currentThread() + "syncscript jsvp=" + jsvp);
		String type = Parser.getQuotedAttribute(peakScript, "type");
		// System.out.println(Thread.currentThread() +
		// "syncscript --selectSpectrum2 " + pi + " " + type + " " + model + " s=" +
		// jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
		jsvp.getPanelData().selectSpectrum(file, type, model, true);
		// System.out.println(Thread.currentThread() +
		// "syncscript --selectSpectrum3 " + pi + " " + type + " " + model + " s=" +
		// jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
		si.siSendPanelChange(jsvp);
		// System.out.println(Thread.currentThread() +
		// "syncscript --selectSpectrum4 " + pi + " " + type + " " + model + " s=" +
		// jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
		jsvp.getPanelData().addPeakHighlight(pi);
		// System.out.println(Thread.currentThread() +
		// "syncscript --selectSpectrum5 " + pi + " " + type + " " + model + " s=" +
		// jsvp.getSpectrum() + " s0=" + jsvp.getSpectrumAt(0));
		jsvp.doRepaint();
		// round trip this so that Jmol highlights all equivalent atoms
		// and appropriately starts or clears vibration
		if (jmolSource == null || (pi != null && pi.getAtoms() != null))
			si.syncToJmol(jmolSelect(pi));
	}

	private boolean checkFileAlreadyLoaded(String fileName) {
		JSVPanel jsvp = selectedPanel;
		if (jsvp != null && jsvp.getPanelData().hasFileLoaded(fileName))
			return true;
		for (int i = panelNodes.size(); --i >= 0;)
			if (panelNodes.get(i).jsvp.getPanelData().hasFileLoaded(fileName)) {
				si.siSetSelectedPanel(panelNodes.get(i).jsvp);
				return true;
			}
		return false;
	}

	private PeakInfo selectPanelByPeak(String peakScript) {
		if (panelNodes == null)
			return null;
		String file = Parser.getQuotedAttribute(peakScript, "file");
		String index = Parser.getQuotedAttribute(peakScript, "index");
		PeakInfo pi = null;
		for (int i = panelNodes.size(); --i >= 0;)
			panelNodes.get(i).jsvp.getPanelData().addPeakHighlight(null);
		JSVPanel jsvp = selectedPanel;
		// System.out.println(Thread.currentThread() +
		// "JSViewer selectPanelByPeak looking for " + index + " " + file + " in " +
		// jsvp);
		pi = jsvp.getPanelData().selectPeakByFileIndex(file, index);
		System.out.println(Thread.currentThread()
				+ "JSViewer selectPanelByPeak pi = " + pi);
		if (pi != null) {
			// found in current panel
			si.siSetNode(JSVPanelNode.findNode(jsvp, panelNodes), false);
		} else {
			// must look elsewhere
			// System.out.println(Thread.currentThread() +
			// "JSViewer selectPanelByPeak did not find it");
			for (int i = panelNodes.size(); --i >= 0;) {
				JSVPanelNode node = panelNodes.get(i);
				// System.out.println(Thread.currentThread() +
				// "JSViewer selectPanelByPeak looking at node " + i + " " +
				// node.fileName);
				if ((pi = node.jsvp.getPanelData().selectPeakByFileIndex(file, index)) != null) {
					// System.out.println(Thread.currentThread() +
					// "JSViewer selectPanelByPeak setting node " + i + " pi=" + pi);
					si.siSetNode(node, false);
					// System.out.println(Thread.currentThread() +
					// "JSViewer selectPanelByPeak setting node " + i + " set node done");
					break;
				}
			}
		}
		// System.out.println(Thread.currentThread() +
		// "JSViewer selectPanelByPeak finally pi = " + pi);
		return pi;
	}

	/**
	 * this method is called as a result of the user clicking on a peak
	 * (eventObject instanceof PeakPickEvent) or from PEAK command execution
	 * 
	 * @param eventObj
	 * @param isApp
	 */
	public void processPeakPickEvent(Object eventObj, boolean isApp) {
		// trouble here is with round trip when peaks are clicked in rapid
		// succession.

		PeakInfo pi;
		if (eventObj instanceof PeakInfo) {
			// this is a call from the PEAK command, above.
			pi = (PeakInfo) eventObj;
			JSVPanel jsvp = selectedPanel;
			PeakInfo pi2 = jsvp.getPanelData().findMatchingPeakInfo(pi);
			if (pi2 == null) {
				if (!"ALL".equals(pi.getTitle()))
					return;
				JSVPanelNode node = null;
				for (int i = 0; i < panelNodes.size(); i++)
					if ((pi2 = panelNodes.get(i).jsvp.getPanelData()
							.findMatchingPeakInfo(pi)) != null) {
						node = panelNodes.get(i);
						break;
					}
				if (node == null)
					return;
				si.siSetNode(node, false);
			}
			pi = pi2;
		} else {
			PeakPickEvent e = ((PeakPickEvent) eventObj);
			si.siSetSelectedPanel((JSVPanel) e.getSource());
			pi = e.getPeakInfo();
		}
		selectedPanel.getPanelData().addPeakHighlight(pi);
		// the above line is what caused problems with GC/MS selection
		syncToJmol(pi);
		// System.out.println(Thread.currentThread() +
		// "processPeakEvent --selectSpectrum " + pi);
		if (pi.isClearAll()) // was not in app version??
			selectedPanel.doRepaint();
		else
			selectedPanel.getPanelData().selectSpectrum(pi.getFilePath(),
					pi.getType(), pi.getModel(), true);
		si.siCheckCallbacks(pi.getTitle());

	}

	private void syncToJmol(PeakInfo pi) {
		si.siSetReturnFromJmolModel(pi.getModel());
		si.syncToJmol(jmolSelect(pi));
	}

	public void sendPanelChange(JSVPanel jsvp) {
		PanelData pd = jsvp.getPanelData();
		JDXSpectrum spec = pd.getSpectrum();
		PeakInfo pi = spec.getSelectedPeak();
		if (pi == null)
			pi = spec.getModelPeakInfoForAutoSelectOnLoad();
		if (pi == null)
			pi = spec.getBasePeakInfo();
		pd.addPeakHighlight(pi);
		Logger.info(Thread.currentThread() + "JSViewer sendFrameChange " + jsvp);
		syncToJmol(pi);
	}

	private String jmolSelect(PeakInfo pi) {
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

	public Map<String, Object> getPropertyAsJavaObject(String key) {
		boolean isAll = false;
		if (key != null && key.toUpperCase().startsWith("ALL ")
				|| "all".equalsIgnoreCase(key)) {
			key = key.substring(3).trim();
			isAll = true;
		}
		if ("".equals(key))
			key = null;
		Map<String, Object> map = new Hashtable<String, Object>();
		Map<String, Object> map0 = selectedPanel.getPanelData().getInfo(
				true, key);
		if (!isAll && map0 != null)
			return map0;
		if (map0 != null)
			map.put("current", map0);
		JmolList<Map<String, Object>> info = new JmolList<Map<String, Object>>();
		for (int i = 0; i < panelNodes.size(); i++) {
			JSVPanel jsvp = panelNodes.get(i).jsvp;
			if (jsvp == null)
				continue;
			info.addLast(panelNodes.get(i).getInfo(key));
		}
		map.put("items", info);
		return map;
	}

	public String getCoordinate() {
		// important to use getSelectedPanel() here because it may be from MainFrame
		// in PRO
		if (selectedPanel != null) {
			Coordinate coord = selectedPanel.getPanelData()
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
	 * @param value
	 * @param speclist
	 * @param isView
	 * @return comma-separated list, for the title
	 */
	private String fillSpecList(String value, JmolList<JDXSpectrum> speclist,
			boolean isView) {

		String prefix = "1.";
		JmolList<String> list;
		JmolList<String> list0 = null;
		boolean isNone = (value.equalsIgnoreCase("NONE"));
		if (isNone || value.equalsIgnoreCase("all"))
			value = "*";
		if (value.indexOf("*") < 0) {
			// replace "3.1.1" with "3.1*1"
			String[] tokens = value.split(" ");
			SB sb = new SB();
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
			value = Txt.simpleReplace(value, "_", " _ ");
			value = Txt.simpleReplace(value, "-", " - ");
			list = ScriptToken.getTokens(value);
			list0 = ScriptToken.getTokens(JSVPanelNode
					.getSpectrumListAsString(panelNodes));
			if (list0.size() == 0)
				return null;
		}

		String id0 = (selectedPanel == null ? prefix : JSVPanelNode.findNode(
				selectedPanel, panelNodes).id);
		id0 = id0.substring(0, id0.indexOf(".") + 1);
		SB sb = new SB();
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
					speclist.addLast(JSVPanelNode.findNodeById(idLast = list0.get(pt++),
							panelNodes).jsvp.getPanelData().getSpectrumAt(0));
					sb.append(",").append(idLast);
				}
				continue;
			}
			JSVPanelNode node;
			if (id.startsWith("\"")) {
				id = Txt.trim(id, "\"");
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
				si.siSetNode(node, true);
				// possibility of a problem here -- we are not communicating with Jmol
				// our model changes.
				speclist.clear();
			}
		}
		return (isNone ? "NONE" : sb.length() > 0 ? sb.toString().substring(1)
				: null);
	}

	private void addSpecToList(PanelData pd, double userYFactor, int isubspec,
			JmolList<JDXSpectrum> list, boolean isView) {
		if (isView) {
			JDXSpectrum spec = pd.getSpectrumAt(0);
			spec.setUserYFactor(Double.isNaN(userYFactor) ? 1 : userYFactor);
			pd.addToList(isubspec - 1, list);
		} else {
			pd.selectFromEntireSet(isubspec - 1);
		}
	}

	private void zoomTo(JSVPanel jsvp, String value) {
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

	public void showColorMessage() {
		JSVPanel jsvp = selectedPanel;
		jsvp.showMessage(jsvp.getPanelData().getSolutionColorHtml(),
				"Predicted Colour");
	}

	public int openDataOrFile(String data, String name,
			JmolList<JDXSpectrum> specs, String url, int firstSpec, int lastSpec,
			boolean isAppend) {
		if ("NONE".equals(name)) {
			close("View*");
			return FILE_OPEN_OK;
		}
		si.writeStatus("");
		String filePath = null;
		String newPath = null;
		String fileName = null;
		File file = null;
		URL base = null;
		boolean isView = false;
		if (data != null) {
		} else if (specs != null) {
			isView = true;
			newPath = fileName = filePath = "View" + si.siIncrementViewCount(1);
		} else if (url != null) {
			try {
				base = JSVFileManager.appletDocumentBase;
				URL u = (base == null ? new URL(url) : new URL(base, url));
				filePath = u.toString();
				si.siSetRecentURL(filePath);
				fileName = JSVFileManager.getName(url);
			} catch (MalformedURLException e) {
				file = new File(url);
			}
		}
		if (file != null) {
			fileName = file.getName();
			newPath = filePath = file.getAbsolutePath();
			// recentJmolName = (url == null ? filePath.replace('\\', '/') : url);
			si.siSetRecentURL(null);
		}
		// TODO could check here for already-open view
		if (!isView)
			if (JSVPanelNode.isOpen(panelNodes, filePath)
					|| JSVPanelNode.isOpen(panelNodes, url)) {
				si.writeStatus(filePath + " is already open");
				return FILE_OPEN_ALREADY;
			}
		if (!isAppend && !isView)
			close("all"); // with CHECK we may still need to do this
		si.setCursor(ApiPlatform.CURSOR_WAIT);
		try {
			si.siSetCurrentSource(isView ? JDXSource.createView(specs) : si
					.siCreateSource(data, filePath, base, firstSpec, lastSpec));
		} catch (Exception e) {
			Logger.error(e.getMessage());
			si.writeStatus(e.getMessage());
			si.setCursor(ApiPlatform.CURSOR_DEFAULT);
			return FILE_OPEN_ERROR;
		}
		si.setCursor(ApiPlatform.CURSOR_DEFAULT);
		System.gc();
		if (newPath == null) {
			newPath = currentSource.getFilePath();
			if (newPath != null)
				fileName = newPath.substring(newPath.lastIndexOf("/") + 1);
		} else {
			currentSource.setFilePath(newPath);
		}
		si.siSetLoaded(fileName, newPath);

		JDXSpectrum spec = currentSource.getJDXSpectrum(0);
		if (spec == null) {
			return FILE_OPEN_NO_DATA;
		}

		specs = currentSource.getSpectra();
		JDXSpectrum.process(specs, si.siGetIRMode());

		boolean autoOverlay = si.siGetAutoCombine()
				|| spec.isAutoOverlayFromJmolClick();

		boolean combine = isView || autoOverlay && currentSource.isCompoundSource;
		if (combine) {
			combineSpectra((isView ? url : null));
		} else {
			splitSpectra();
		}
		if (!isView)
			si.siUpdateRecentMenus(filePath);
		return FILE_OPEN_OK;
	}

	public void close(String value) {
		if (value == null || value.equalsIgnoreCase("all") || value.equals("*")) {
			si.siCloseSource(null);
			return;
		}
		value = value.replace('\\', '/');
		if (value.endsWith("*")) {
			value = value.substring(0, value.length() - 1);
			for (int i = panelNodes.size(); --i >= 0;)
				if (i < panelNodes.size()
						&& panelNodes.get(i).fileName.startsWith(value))
					si.siCloseSource(panelNodes.get(i).source);
		} else if (value.equals("selected")) {
			JmolList<JDXSource> list = new JmolList<JDXSource>();
			JDXSource lastSource = null;
			for (int i = panelNodes.size(); --i >= 0;) {
				JDXSource source = panelNodes.get(i).source;
				if (panelNodes.get(i).isSelected
						&& (lastSource == null || lastSource != source))
					list.addLast(source);
				lastSource = source;
			}
			for (int i = list.size(); --i >= 0;)
				si.siCloseSource(list.get(i));
		} else {
			JDXSource source = (value.length() == 0 ? currentSource
					: JSVPanelNode.findSourceByNameOrId(value, panelNodes));
			if (source == null)
				return;
			si.siCloseSource(source);
		}
		if (selectedPanel == null && panelNodes.size() > 0)
			si.siSetSelectedPanel(JSVPanelNode.getLastFileFirstNode(panelNodes));
	}

	public void load(String value) {
		JmolList<String> tokens = ScriptToken.getTokens(value);
		String filename = tokens.get(0);
		int pt = 0;
		boolean isAppend = filename.equalsIgnoreCase("APPEND");
		boolean isCheck = filename.equalsIgnoreCase("CHECK");
		if (isAppend || isCheck)
			filename = tokens.get(++pt);
		boolean isSimulation = filename.equalsIgnoreCase("MOL");
		if (isSimulation)
			filename = JSVFileManager.SIMULATION_PROTOCOL + "MOL="
					+ Txt.trimQuotes(tokens.get(++pt));
		if (!isCheck && !isAppend) {
			if (filename.equals("\"\"") && currentSource != null)
				filename = currentSource.getFilePath();
			close("all");
		}
		filename = Txt.trimQuotes(filename);
		if (filename.startsWith("$")) {
			isSimulation = true;
			filename = JSVFileManager.SIMULATION_PROTOCOL + filename;
		}
		int firstSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
				.intValue() : -1);
		int lastSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
				.intValue() : firstSpec);
		si
				.siOpenDataOrFile(null, null, null, filename, firstSpec, lastSpec,
						isAppend);
	}

	public void combineSpectra(String name) {
		JDXSource source = currentSource;
		JmolList<JDXSpectrum> specs = source.getSpectra();
		JSVPanel jsvp = si.siGetNewJSVPanel2(specs);
		jsvp.setTitle(source.getTitle());
		if (jsvp.getTitle().equals("")) {
			jsvp.getPanelData().setViewTitle(source.getFilePath());
			jsvp.setTitle(name);
		}
		si.siSetPropertiesFromPreferences(jsvp, true);
		si.siCreateTree(source, new JSVPanel[] { jsvp }).getPanelNode().isView = true;
		JSVPanelNode node = JSVPanelNode.findNode(selectedPanel, panelNodes);
		node.setFrameTitle(name);
		node.isView = true;
		if (si.siGetAutoShowLegend()
				&& selectedPanel.getPanelData().getNumberOfGraphSets() == 1)
			node.setLegend(si.siNewDialog("legend", jsvp));
		si.siSetMenuEnables(node, false);
	}

	public void closeSource(JDXSource source) {
		// Remove nodes and dispose of frames
		JSVTreeNode rootNode = spectraTree.getRootNode();
		String fileName = (source == null ? null : source.getFilePath());
		JmolList<JSVTreeNode> toDelete = new JmolList<JSVTreeNode>();
		Enumeration<JSVTreeNode> enume = rootNode.children();
		while (enume.hasMoreElements()) {
			JSVTreeNode node = enume.nextElement();
			if (fileName == null
					|| node.getPanelNode().source.getFilePath().equals(fileName)) {
				for (Enumeration<JSVTreeNode> e = node.children(); e.hasMoreElements();) {
					JSVTreeNode childNode = e.nextElement();
					toDelete.addLast(childNode);
					panelNodes.remove(childNode.getPanelNode());
				}
				toDelete.addLast(node);
				if (fileName != null)
					break;
			}
		}
		spectraTree.deleteNodes(toDelete);
		if (source == null) {
			// jsvpPopupMenu.dispose();
			if (currentSource != null)
				currentSource.dispose();
			// jsvpPopupMenu.dispose();
			if (selectedPanel != null)
				selectedPanel.dispose();
		} else {
			// setFrameAndTreeNode(panelNodes.size() - 1);
		}

		if (currentSource == source) {
			si.siSetSelectedPanel(null);
			si.siSetCurrentSource(null);
		}

		int max = 0;
		for (int i = 0; i < panelNodes.size(); i++) {
			float f = Parser.parseFloat(panelNodes.get(i).id);
			if (f >= max + 1)
				max = (int) Math.floor(f);
		}
		si.siSetFileCount(max);
		System.gc();
		Logger.checkMemory();
	}

	public void setFrameAndTreeNode(int i) {
		if (panelNodes == null || i < 0 || i >= panelNodes.size())
			return;
		si.siSetNode(panelNodes.get(i), false);
	}

	public JSVPanelNode selectFrameNode(JSVPanel jsvp) {
		// Find Node in SpectraTree and select it
		JSVPanelNode node = JSVPanelNode.findNode(jsvp, panelNodes);
		if (node == null)
			return null;

		spectraTree.setPath(spectraTree.newTreePath(node.treeNode.getPath()));
		return si.siSetOverlayVisibility(node);
	}

	public JSVPanel setSpectrum(String value) {
		if (value.indexOf('.') >= 0) {
			JSVPanelNode node = JSVPanelNode.findNodeById(value, panelNodes);
			if (node == null)
				return null;
			si.siSetNode(node, false);
		} else {
			int n = Parser.parseInt(value);
			if (n <= 0) {
				checkOverlay();
				return null;
			}
			setFrameAndTreeNode(n - 1);
		}
		return selectedPanel;
	}

	public void splitSpectra() {
		JDXSource source = currentSource;
		JmolList<JDXSpectrum> specs = source.getSpectra();
		JSVPanel[] panels = new JSVPanel[specs.size()];
		JSVPanel jsvp = null;
		for (int i = 0; i < specs.size(); i++) {
			JDXSpectrum spec = specs.get(i);
			jsvp = si.siGetNewJSVPanel(spec);
			si.siSetPropertiesFromPreferences(jsvp, true);
			panels[i] = jsvp;
		}
		// arrange windows in ascending order
		si.siCreateTree(source, panels);
		si.siGetNewJSVPanel((JDXSpectrum) null); // end of operation
		JSVPanelNode node = JSVPanelNode.findNode(selectedPanel, panelNodes);
		si.siSetMenuEnables(node, true);
	}

	public void selectedTreeNode(JSVTreeNode node) {
		if (node == null) {
			return;
		}
		if (node.isLeaf()) {
			si.siSetNode(node.getPanelNode(), true);
		}
		si.siSetCurrentSource(node.getPanelNode().source);
	}

	public void removeAllHighlights() {
		JSVPanel jsvp = selectedPanel;
		if (jsvp != null) {
			jsvp.getPanelData().removeAllHighlights();
			jsvp.doRepaint();
		}
	}

	public void removeHighlight(double x1, double x2) {
		JSVPanel jsvp = selectedPanel;
		if (jsvp != null) {
			jsvp.getPanelData().removeHighlight(x1, x2);
			jsvp.doRepaint();
		}
	}

	public void dispose() {
		fileHelper = null;
		if (panelNodes != null)
			for (int i = panelNodes.size(); --i >= 0;) {
				panelNodes.get(i).dispose();
				panelNodes.remove(i);
			}
	}

	public void runScript(String script) {
		if (scriptQueue == null)
			si.siProcessCommand(script);
		else
			scriptQueue.addLast(script);
	}
	
	public void requestRepaint() {
		if (selectedPanel != null)
			repaintManager.refresh();
	}

	public void repaintDone() {
		repaintManager.repaintDone();
	}

	public PanelData getPanelData() {
		return selectedPanel.getPanelData();
	}

	public void showProperties() {
		selectedPanel.showProperties();
	}

	public void checkOverlay() {
		if (viewPanel != null)
      viewPanel.markSelectedPanels(panelNodes);
		si.siNewDialog("view", null);
	}


	public Object getDisplay() {
		return viewPanel;
	}


	public boolean getBooleanProperty(String name) {
		// TODO Auto-generated method stub
		return false;
	}
}
