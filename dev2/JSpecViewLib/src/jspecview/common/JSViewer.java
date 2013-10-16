package jspecview.common;

import org.jmol.api.ApiPlatform;
import org.jmol.api.Interface;
import org.jmol.api.JSmolInterface;
import org.jmol.api.PlatformViewer;
import org.jmol.util.JmolList;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import java.util.Map;

import org.jmol.util.Dimension;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.SB;
import org.jmol.util.Txt;

import jspecview.api.ExportInterface;
import jspecview.api.JSVApiPlatform;
import jspecview.api.JSVFileHelper;
import jspecview.api.JSVGraphics;
import jspecview.api.JSVMainPanel;
import jspecview.api.JSVPanel;
import jspecview.api.JSVPopupMenu;
import jspecview.api.JSVPrintDialog;
import jspecview.api.JSVTree;
import jspecview.api.JSVTreeNode;
import jspecview.api.ScriptInterface;
import jspecview.api.VisibleInterface;
import jspecview.common.Annotation.AType;
import jspecview.common.PanelData.LinkMode;
import jspecview.dialog.JSVDialog;
import jspecview.dialog.DialogManager;
import jspecview.source.JDXSource;
import jspecview.source.JDXSpectrum;
import jspecview.source.JDXSpectrum.IRMode;
import jspecview.tree.SimpleTree;
import jspecview.util.JSVEscape;

/**
 * This class encapsulates all general functionality of applet and app. Most
 * methods include ScriptInterface parameter, which will be JSVAppletPrivate,
 * JSVAppletPrivatePro, or MainFrame.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class JSViewer implements PlatformViewer, JSmolInterface {

	public final static String sourceLabel = "Original...";

	public final static int FILE_OPEN_OK = 0;
	public final static int FILE_OPEN_ALREADY = -1;
	// private final static int FILE_OPEN_URLERROR = -2;
	public final static int FILE_OPEN_ERROR = -3;
	public final static int FILE_OPEN_NO_DATA = -4;
	public static final int OVERLAY_DIALOG = -1;
	public static final int OVERLAY_OFFSET = 99;
	public static final int PORTRAIT = 1; // Printable
	public static final int PAGE_EXISTS = 0;
	public static final int NO_SUCH_PAGE = 1;

	private final static String noColor = "255,255,255";

	private static String testScript = "<PeakData  index=\"1\" title=\"\" model=\"~1.1\" type=\"1HNMR\" xMin=\"3.2915\" xMax=\"3.2965\" atoms=\"15,16,17,18,19,20\" multiplicity=\"\" integral=\"1\"> src=\"JPECVIEW\" file=\"http://SIMULATION/$caffeine\"";

	private final static int NLEVEL_MAX = 100;

	public ScriptInterface si;
	public JSVGraphics g2d;
	public JSVTree spectraTree;
	public JDXSource currentSource;
	public JmolList<PanelNode> panelNodes;
	public ColorParameters parameters;
	public RepaintManager repaintManager;
	public JSVPanel selectedPanel;
	public JSVMainPanel viewPanel; // alias for spectrumPanel
	public Properties properties; // application only
	public JmolList<String> scriptQueue;
	public JSVFileHelper fileHelper;
	public JSVPopupMenu jsvpPopupMenu;
	private DialogManager dialogManager;
	private JSVDialog viewDialog;
	private JSVDialog overlayLegendDialog;

	private IRMode irMode = IRMode.NO_CONVERT;

	public boolean isSingleThreaded;
	public boolean isApplet;
	public boolean isJS;
	public boolean isSigned;

	private String recentScript = "";

	public String appletID;
	public String fullName;
	public String syncID;
	public Object applet; // will be an JavaScript object if this is JavaScript

	public Object display;
	private int maximumSize = Integer.MAX_VALUE;
	final Dimension dimScreen = new Dimension();

	public JSVApiPlatform apiPlatform;

	public void setProperty(String key, String value) {
		if (properties != null)
			properties.setProperty(key, value);
	}

	public void setNode(PanelNode node, boolean fromTree) {
		si.siSetNode(node, fromTree);
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
		apiPlatform = (JSVApiPlatform) getAwtInterface("Platform");
		apiPlatform.setViewer(this, this.display);
		g2d = (JSVGraphics) getAwtInterface("G2D");
		spectraTree = new SimpleTree(this);
		parameters = (ColorParameters) getAwtInterface("Parameters");
		parameters.setName("applet");
		fileHelper = ((JSVFileHelper) getAwtInterface("FileHelper")).set(this);
		isSingleThreaded = apiPlatform.isSingleThreaded();
		panelNodes = new JmolList<PanelNode>();
		repaintManager = new RepaintManager(this);
		if (!isApplet)
			setPopupMenu(true, true);
	}

	public void setPopupMenu(boolean allowMenu, boolean zoomEnabled) {
		try {
			jsvpPopupMenu = (JSVPopupMenu) getAwtInterface("Popup");
			jsvpPopupMenu.initialize(this, isApplet ? "appletMenu" : "appMenu");
			jsvpPopupMenu.setEnabled(allowMenu, zoomEnabled);
		} catch (Exception e) {
			System.out.println(e + " initializing popup menu");
		}
	}

	public boolean runScriptNow(String script) {
		si.siIncrementViewCount(1);
		if (script == null)
			script = "";
		script = script.trim();
		System.out.println("RUNSCRIPT " + script);
		boolean isOK = true;
		int nErrorsLeft = 10;
		ScriptTokenizer commandTokens = new ScriptTokenizer(script, true);
		String msg = null;
		while (commandTokens.hasMoreTokens() && nErrorsLeft > 0 && isOK) {
			String token = commandTokens.nextToken();
			// now split the key/value pair

			ScriptTokenizer eachParam = new ScriptTokenizer(token, false);
			String key = ScriptToken.getKey(eachParam);
			if (key == null)
				continue;
			ScriptToken st = ScriptToken.getScriptToken(key);
			String value = ScriptToken.getValue(st, eachParam, token);
			System.out.println("KEY-> " + key + " VALUE-> " + value + " : " + st);
			try {
				switch (st) {
				case UNKNOWN:
					Logger.info("Unrecognized parameter: " + key);
					--nErrorsLeft;
					break;
				default:
					if (selectedPanel == null)
						break;// probably a startup option for the applet
					parameters.set(pd(), st, value);
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
					si.siExecClose(value);
					break;
				case DEBUG:
					Logger
							.setLogLevel(value.toLowerCase().equals("high") ? Logger.LEVEL_DEBUGHIGH
									: Parameters.isTrue(value) ? Logger.LEVEL_DEBUG
											: Logger.LEVEL_INFO);
					break;
				case EXPORT:
					msg = execExport(value);
					return false;
				case GETPROPERTY:
					Map<String, Object> info = (selectedPanel == null ? null
							: getPropertyAsJavaObject(value));
					if (info != null)
						selectedPanel
								.showMessage(JSVEscape.toJSON(null, info, true), value);
					break;
				case HIDDEN:
					si.siExecHidden(Parameters.isTrue(value));
					break;
				case INTEGRATIONRATIOS:
					si.siSetIntegrationRatios(value);
					if (selectedPanel != null)
						execIntegrate(null);
					break;
				case INTERFACE:
					si.siExecSetInterface(value);
					break;
				case INTEGRALOFFSET:
				case INTEGRALRANGE:
					execSetIntegralParameter(st, Double.parseDouble(value));
					break;
				case JMOL:
					si.syncToJmol(value);
					break;
				case JSV:
					syncScript(Txt.trimQuotes(value));
					break;
				case LOAD:
					msg = si.siExecLoad(value);
					break;
				case LOADIMAGINARY:
					si.siSetLoadImaginary(Parameters.isTrue(value));
					break;
				case PEAK:
					execPeak(value);
					break;
				case PEAKLIST:
					execPeakList(value);
					break;
				case SCALEBY:
					scaleSelectedBy(panelNodes, value);
					break;
				case SCRIPT:
					if (value.equals("") || value.toLowerCase().startsWith("inline")) {
						execScriptInline(value);
					} else {
						String s = si.siSetFileAsString(value);
						if (s != null && si.siIncrementScriptLevelCount(0) < NLEVEL_MAX)
							runScriptNow(s);
					}
					break;
				case SELECT:
					execSelect(value);
					break;
				case SPECTRUM:
				case SPECTRUMNUMBER:
					if (!setSpectrum(value))
						isOK = false;
					break;
				case STACKOFFSETY:
					execOverlayOffsetY(Parser.parseInt("" + Parser.parseFloat(value)));
					break;
				case TEST:
					si.siExecTest(value);
					break;
				case OVERLAY: // deprecated
				case VIEW:
					execView(value, true);
					break;
				case FINDX:
				case GETSOLUTIONCOLOR:
				case INTEGRATION:
				case INTEGRATE:
				case IRMODE:
				case LABEL:
				case LINK:
				case OVERLAYSTACKED:
				case PRINT:
				case SETPEAK:
				case SETX:
				case SHIFTX:
				case SHOWERRORS:
				case SHOWMEASUREMENTS:
				case SHOWMENU:
				case SHOWKEY:
				case SHOWPEAKLIST:
				case SHOWINTEGRATION:
				case SHOWPROPERTIES:
				case SHOWSOURCE:
				case YSCALE:
				case ZOOM:
					if (selectedPanel == null) {
						isOK = false;
						break;
					}
					switch (st) {
					default:
						break;
					case FINDX:
						pd().findX(null, Double.parseDouble(value));
						break;
					case GETSOLUTIONCOLOR:
						selectedPanel.showMessage(getSolutionColorHtml(),
								"Predicted Colour");
						break;
					case INTEGRATION:
					case INTEGRATE:
						execIntegrate(value);
						break;
					case IRMODE:
						execIRMode(value);
						break;
					case LABEL:
						pd().addAnnotation(ScriptToken.getTokens(value));
						break;
					case LINK:
						pd().linkSpectra(LinkMode.getMode(value));
						break;
					case OVERLAYSTACKED:
						pd().splitStack(!Parameters.isTrue(value));
						break;
					case PRINT:
						msg = printPDF(value);
						break;
					case SETPEAK:
						// setpeak NONE Double.NaN, Double.MAX_VALUE
						// shiftx NONE Double.MAX_VALUE, Double.NaN
						// setpeak x.x Double.NaN, value
						// setx x.x Double.MIN_VALUE, value
						// shiftx x.x value, Double.NaN
						// setpeak ? Double.NaN, Double.MIN_VALUE
						pd().shiftSpectrum(
								Double.NaN,
								value.equalsIgnoreCase("NONE") ? Double.MAX_VALUE : value
										.equalsIgnoreCase("?") ? Double.MIN_VALUE : Double
										.parseDouble(value));
						break;
					case SETX:
						pd().shiftSpectrum(Double.MIN_VALUE, Double.parseDouble(value));
						break;
					case SHIFTX:
						pd().shiftSpectrum(
								value.equalsIgnoreCase("NONE") ? Double.MAX_VALUE : Double
										.parseDouble(value), Double.NaN);
						break;
					case SHOWERRORS:
						show("errors");
						break;
					case SHOWINTEGRATION:
						pd().showAnnotation(AType.Integration,
								Parameters.getTFToggle(value));
						// execIntegrate(null);
						break;
					case SHOWKEY:
						setOverlayLegendVisibility(Parameters.getTFToggle(value), true);
						break;
					case SHOWMEASUREMENTS:
						pd().showAnnotation(AType.Measurements,
								Parameters.getTFToggle(value));
						break;
					case SHOWMENU:
						showMenu(Integer.MIN_VALUE, 0);
						break;
					case SHOWPEAKLIST:
						pd().showAnnotation(AType.PeakList, Parameters.getTFToggle(value));
						break;
					case SHOWPROPERTIES:
						show("properties");
						break;
					case SHOWSOURCE:
						show("source");
						break;
					case YSCALE:
						setYScale(value);
						break;
					case WINDOW:
						si.siNewWindow(Parameters.isTrue(value), false);
						break;
					case ZOOM:
						isOK = execZoom(value);
						break;
					}
					break;
				}
			} catch (Exception e) {
				/**
				 * @j2sNative
				 * 
				 *            alert(e + "\n" + Clazz.getStackTrace())
				 */
				{
					System.out.println(e.getMessage());
					Logger.error(e.getMessage());

					if (Logger.debugging)
						e.printStackTrace();
				}
				isOK = false;
				--nErrorsLeft;
			}
		}
		si.siIncrementViewCount(-1);
		si.siExecScriptComplete(msg, true);
		return isOK;
	}

	private PanelData pd() {
		return selectedPanel.getPanelData();
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

	private boolean execZoom(String value) {
		double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
		JmolList<String> tokens;
		tokens = ScriptToken.getTokens(value);
		switch (tokens.size()) {
		default:
			return false;
		case 1:
			zoomTo(tokens.get(0));
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
		pd().setZoom(x1, y1, x2, y2);
		return true;
	}

	// private String recentZoom = "";
	// doesn't work

	private void zoomTo(String value) {
		PanelData pd = pd();
		// if (value.equals("")) {
		// value = selectedPanel.getInput("Enter zoom range y1 y2", "Zoom",
		// recentZoom);
		// if (value == null)
		// return;
		// recentZoom = value;
		// runScriptNow("zoom " + value);
		// return;
		// }
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

	private void scaleSelectedBy(JmolList<PanelNode> nodes, String value) {
		try {
			double f = Double.parseDouble(value);
			for (int i = nodes.size(); --i >= 0;)
				nodes.get(i).jsvp.getPanelData().scaleSelectedBy(f);
		} catch (Exception e) {
		}
	}

	private void execSelect(String value) {
		JmolList<PanelNode> nodes = panelNodes;
		for (int i = nodes.size(); --i >= 0;)
			nodes.get(i).jsvp.getPanelData().selectFromEntireSet(Integer.MIN_VALUE);
		JmolList<JDXSpectrum> speclist = new JmolList<JDXSpectrum>();
		fillSpecList(value, speclist, false);
	}

	public void execView(String value, boolean fromScript) {
		if (value.equals("")) {
			checkOverlay();
			return;
		}
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
		PanelData pd = pd();
		JDXSpectrum spec = pd.getSpectrum();
		JDXSpectrum spec2 = JDXSpectrum.taConvert(spec, mode);
		if (spec2 == spec)
			return;
		pd.setSpectrum(spec2);
		setIRmode(value);
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

	private void setYScale(String value) {
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
			JDXSpectrum spec = pd().getSpectrum();
			for (int i = panelNodes.size(); --i >= 0;) {
				PanelNode node = panelNodes.get(i);
				if (node.source != currentSource)
					continue;
				if (JDXSpectrum.areXScalesCompatible(spec, node.getSpectrum(), false,
						false))
					node.jsvp.getPanelData().setZoom(0, y1, 0, y2);
			}
		} else {
			pd().setZoom(0, y1, 0, y2);
		}
	}

	private boolean overlayLegendVisible;

	private void setOverlayLegendVisibility(Boolean tftoggle,
			boolean doSet) {
		if (doSet)
			overlayLegendVisible = (tftoggle == null ? !overlayLegendVisible
					: tftoggle == Boolean.TRUE);
		PanelNode node = PanelNode.findNode(selectedPanel, panelNodes);
		for (int i = panelNodes.size(); --i >= 0;)
			showOverlayLegend(panelNodes.get(i), panelNodes.get(i) == node
					&& overlayLegendVisible);
	}

	private void showOverlayLegend(PanelNode node, boolean visible) {
		JSVDialog legend = node.legend;
		if (legend == null && visible) {
			legend = node.setLegend(node.jsvp.getPanelData()
					.getNumberOfSpectraInCurrentSet() > 1
					&& node.jsvp.getPanelData().getNumberOfGraphSets() == 1 ? getDialog(
					AType.OverlayLegend, null) : null);
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
			setNode(PanelNode.findNode(jsvp, panelNodes), false);
		} else {
			// must look elsewhere
			// System.out.println(Thread.currentThread() +
			// "JSViewer selectPanelByPeak did not find it");
			for (int i = panelNodes.size(); --i >= 0;) {
				PanelNode node = panelNodes.get(i);
				// System.out.println(Thread.currentThread() +
				// "JSViewer selectPanelByPeak looking at node " + i + " " +
				// node.fileName);
				if ((pi = node.jsvp.getPanelData().selectPeakByFileIndex(file, index)) != null) {
					// System.out.println(Thread.currentThread() +
					// "JSViewer selectPanelByPeak setting node " + i + " pi=" + pi);
					setNode(node, false);
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
				PanelNode node = null;
				for (int i = 0; i < panelNodes.size(); i++)
					if ((pi2 = panelNodes.get(i).jsvp.getPanelData()
							.findMatchingPeakInfo(pi)) != null) {
						node = panelNodes.get(i);
						break;
					}
				if (node == null)
					return;
				setNode(node, false);
			}
			pi = pi2;
		} else {
			PeakPickEvent e = ((PeakPickEvent) eventObj);
			si.siSetSelectedPanel((JSVPanel) e.getSource());
			pi = e.getPeakInfo();
		}
		pd().addPeakHighlight(pi);
		// the above line is what caused problems with GC/MS selection
		syncToJmol(pi);
		// System.out.println(Thread.currentThread() +
		// "processPeakEvent --selectSpectrum " + pi);
		if (pi.isClearAll()) // was not in app version??
			selectedPanel.doRepaint();
		else
			pd().selectSpectrum(pi.getFilePath(), pi.getType(), pi.getModel(), true);
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
		Map<String, Object> map0 = pd().getInfo(true, key);
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
			Coordinate coord = pd().getClickedCoordinate();
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
			list = ScriptToken.getTokens(PanelNode
					.getSpectrumListAsString(panelNodes));
		} else if (value.startsWith("\"")) {
			list = ScriptToken.getTokens(value);
		} else {
			value = Txt.simpleReplace(value, "_", " _ ");
			value = Txt.simpleReplace(value, "-", " - ");
			list = ScriptToken.getTokens(value);
			list0 = ScriptToken.getTokens(PanelNode
					.getSpectrumListAsString(panelNodes));
			if (list0.size() == 0)
				return null;
		}

		String id0 = (selectedPanel == null ? prefix : PanelNode.findNode(
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
					speclist.addLast(PanelNode.findNodeById(idLast = list0.get(pt++),
							panelNodes).jsvp.getPanelData().getSpectrumAt(0));
					sb.append(",").append(idLast);
				}
				continue;
			}
			PanelNode node;
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
			node = PanelNode.findNodeById(id, panelNodes);
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
			PanelNode node = PanelNode.findNodeById(idLast, panelNodes);
			if (node != null) {
				setNode(node, true);
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

	public String getSolutionColor() {
		JDXSpectrum spectrum = pd().getSpectrum();
		return (spectrum.canShowSolutionColor() ? ((VisibleInterface) Interface
				.getInterface("jspecview.common.Visible")).getColour(spectrum
				.getXYCoords(), spectrum.getYUnits()) : noColor);
	}

	public String getSolutionColorHtml() {
		String color = getSolutionColor();
		return "<html><body style='background-color:rgb(" + color
				+ ")'><br />Predicted Solution Colour- RGB(" + color
				+ ")<br /><br /></body></html>";
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
				URL u = new URL(base, url, null);
				filePath = u.toString();
				si.siSetRecentURL(filePath);
				fileName = JSVFileManager.getName(filePath);
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
			if (PanelNode.isOpen(panelNodes, filePath)
					|| PanelNode.isOpen(panelNodes, url)) {
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
			/**
			 * @j2sNative
			 * 
			 *            alert(e + "\n" + Clazz.getStackTrace())
			 */
			{
				Logger.error(e.getMessage());
				si.writeStatus(e.getMessage());
			}
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
		JDXSpectrum.process(specs, irMode);

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
			JDXSource source = (value.length() == 0 ? currentSource : PanelNode
					.findSourceByNameOrId(value, panelNodes));
			if (source == null)
				return;
			si.siCloseSource(source);
		}
		if (selectedPanel == null && panelNodes.size() > 0)
			si.siSetSelectedPanel(PanelNode.getLastFileFirstNode(panelNodes));
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
		si.siOpenDataOrFile(null, null, null, filename, firstSpec, lastSpec,
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
		PanelNode node = PanelNode.findNode(selectedPanel, panelNodes);
		node.setFrameTitle(name);
		node.isView = true;
		if (si.siGetAutoShowLegend() && pd().getNumberOfGraphSets() == 1)
			node.setLegend(getDialog(AType.OverlayLegend, null));
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
		setNode(panelNodes.get(i), false);
	}

	public PanelNode selectFrameNode(JSVPanel jsvp) {
		// Find Node in SpectraTree and select it
		PanelNode node = PanelNode.findNode(jsvp, panelNodes);
		if (node == null)
			return null;
		spectraTree.setPath(spectraTree.newTreePath(node.treeNode.getPath()));
		setOverlayLegendVisibility(null, false);
		return node;
	}

	private boolean setSpectrum(String value) {
		if (value.indexOf('.') >= 0) {
			PanelNode node = PanelNode.findNodeById(value, panelNodes);
			if (node == null)
				return false;
			setNode(node, false);
		} else {
			int n = Parser.parseInt(value);
			if (n <= 0) {
				checkOverlay();
				return false;
			}
			setFrameAndTreeNode(n - 1);
		}
		return true;
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
		si.siGetNewJSVPanel(null); // end of operation
		PanelNode node = PanelNode.findNode(selectedPanel, panelNodes);
		si.siSetMenuEnables(node, true);
	}

	public void selectedTreeNode(JSVTreeNode node) {
		if (node == null) {
			return;
		}
		if (node.isLeaf()) {
			setNode(node.getPanelNode(), true);
		} else {
			System.out.println("not a leaf");
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
		if (viewDialog != null)
			viewDialog.dispose();
		viewDialog = null;
		if (overlayLegendDialog != null)
			overlayLegendDialog.dispose();
		overlayLegendDialog = null;

		if (jsvpPopupMenu != null) {
			jsvpPopupMenu.dispose();
			jsvpPopupMenu = null;
		}
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

	public void checkOverlay() {
		if (viewPanel != null)
			viewPanel.markSelectedPanels(panelNodes);
		viewDialog = getDialog(AType.Views, null);
	}

	private int recentStackPercent = 5;

	private void execOverlayOffsetY(int offset) {
		if (selectedPanel == null)
			return;
		if (offset == Integer.MIN_VALUE) {
			String soffset = selectedPanel.getInput(
					"Enter a vertical offset in percent for stacked plots", "Overlay", ""
							+ recentStackPercent);
			float f = Parser.parseFloat(soffset);
			if (Float.isNaN(f))
				return;
			offset = (int) f;
		}
		recentStackPercent = offset;
		pd().setYStackOffsetPercent(offset);
	}

	private void execScriptInline(String script) {
		if (script.length() > 0)
			script = script.substring(6).trim();
		if (script.length() == 0)
			script = selectedPanel.getInput("Enter a JSpecView script", "Script",
					recentScript);
		if (script == null)
			return;
		recentScript = script;
		runScriptNow(script);
	}

	public void showMenu(int x, int y) {
		if (jsvpPopupMenu != null)
			jsvpPopupMenu.jpiShow(x, y);
	}

	// / called by JSmol JavaScript

	public void setDisplay(Object canvas) {
		// used by JSmol/HTML5 when a canvas is resized
		apiPlatform.setViewer(this, display = canvas);
		int[] wh = new int[2];
		apiPlatform.getFullScreenDimensions(canvas, wh);
		setScreenDimension(wh[0], wh[1]);
	}

	public void setScreenDimension(int width, int height) {
		// There is a bug in Netscape 4.7*+MacOS 9 when comparing dimension objects
		// so don't try dim1.equals(dim2)
		height = Math.min(height, maximumSize);
		width = Math.min(width, maximumSize);
		if (dimScreen.width == width && dimScreen.height == height)
			return;
		// System.out.println("HMM " + width + " " + height + " " + maximumSize);
		resizeImage(width, height);
	}

	void resizeImage(int width, int height) {
		if (width > 0) {
			dimScreen.width = width;
			dimScreen.height = height;
		} else {
			width = (dimScreen.width == 0 ? dimScreen.width = 500 : dimScreen.width);
			height = (dimScreen.height == 0 ? dimScreen.height = 500
					: dimScreen.height);
		}
		g2d.setWindowParameters(width, height);
	}

	/**
	 * for JavaScript only; this is the call to draw the spectrum
	 * 
	 * @param width
	 * @param height
	 */
	public void updateJS(int width, int height) {
		if (selectedPanel == null)
			return;
		/**
		 * @j2sNative
		 * 
		 *            this.selectedPanel.paintComponent(this.apiPlatform.context);
		 * 
		 */
		{
		}
	}

	/**
	 * called by JSmol.js mouse event
	 * 
	 * @param id
	 * @param x
	 * @param y
	 * @param modifiers
	 * @param time
	 * @return t/f
	 */
	public boolean handleOldJvm10Event(int id, int x, int y, int modifiers,
			long time) {
		return (selectedPanel != null && selectedPanel.handleOldJvm10Event(id, x,
				y, modifiers, time));
	}

	public void processTwoPointGesture(float[][][] touches) {
		if (selectedPanel != null)
			selectedPanel.processTwoPointGesture(touches);
	}

	public Object getApplet() {
		return applet;
	}

	public void startHoverWatcher(boolean enable) {
		// n/a?
	}

	public int cacheFileByName(String fileName, boolean isAdd) {
		// n/a
		return 0;
	}

	public void cachePut(String key, Object data) {
		// n/a
	}

	public void openFileAsyncPDB(String fileName, boolean pdbCartoons) {
		// n/a
	}

	public int getHeight() {
		return dimScreen.height;
	}

	public int getWidth() {
		return dimScreen.width;
	}

	public Object getAwtInterface(String type) {
		return Interface.getInterface("jspecview.awt" + (isJS ? "js2d.Js" : ".Awt")
				+ type);
	}

	public DialogManager getDialogManager() {
		if (dialogManager != null)
			return dialogManager;
		dialogManager = (DialogManager) Interface.getInterface("jspecview.awtjs2d.JsDialogManager");
		//    getAwtInterface("DialogManager");
		return dialogManager.set(this);
	}

	public JSVDialog getDialog(AType type, JDXSpectrum spec) {
		String root = "jspecview.dialog.";
		switch (type) {
		case Integration:
			return ((JSVDialog) Interface.getInterface(root + "IntegrationDialog"))
					.setParams("Integration for " + spec, this, spec);
		case Measurements:
			return ((JSVDialog) Interface.getInterface(root + "MeasurementsDialog"))
					.setParams("Measurements for " + spec, this, spec);
		case PeakList:
			return ((JSVDialog) Interface.getInterface(root + "PeakListDialog"))
					.setParams("Peak List for " + spec, this, spec);
		case OverlayLegend:
			return overlayLegendDialog = ((JSVDialog) Interface.getInterface(root
					+ "OverlayLegendDialog")).setParams(pd().getViewTitle(), this, null);
		case Views:
			return viewDialog = ((JSVDialog) Interface.getInterface(root
					+ "ViewsDialog")).setParams("View/Combine/Close Spectra", this, null);
		default:
			return null;
		}
	}

	private void show(String what) {
		getDialogManager();
		if (what.equals("properties")) {
			dialogManager.showProperties(null, pd().getSpectrum());
		} else if (what.equals("errors")) {
			dialogManager.showSourceErrors(null, currentSource);
		} else if (what.equals("source")) {
			if (currentSource == null) {
				if (panelNodes.size() > 0)
					dialogManager.showMessageDialog(null, "Please Select a Spectrum",
							"Select Spectrum", DialogManager.ERROR_MESSAGE);
				return;
			}
			dialogManager.showSource(this, currentSource);
		}
	}

	private PrintLayout lastPrintLayout;
	private Object offWindowFrame;

	public PrintLayout getDialogPrint(boolean isJob) {
		try {
			PrintLayout pl = ((JSVPrintDialog) getAwtInterface("PrintDialog")).set(
					offWindowFrame, lastPrintLayout, isJob).getPrintLayout();
			if (pl != null)
				lastPrintLayout = pl;
			return pl;
		} catch (Exception e) {
			return null;
		}
	}

	public void setIRmode(String mode) {
		if (mode.equals("AtoT")) {
			irMode = IRMode.TO_TRANS;
		} else if (mode.equals("TtoA")) {
			irMode = IRMode.TO_ABS;
		} else {
			irMode = IRMode.getMode(mode);
		}
	}

	public int getOptionFromDialog(String[] items, String title, String label) {
		getDialogManager().getOptionFromDialog(null, items, selectedPanel, title,
				label);
		// TODO Auto-generated method stub
		return 0;
	}

	private String execExport(String value) {
		String msg = ((ExportInterface) Interface
				.getInterface("jspecview.export.Exporter")).exportCmd(selectedPanel,
				ScriptToken.getTokens(value), false);
		si.writeStatus(msg);
		return msg;
	}

	/**
	 * @param fileName
	 * @return "OK" if signedApplet or app; Base64-encoded string if unsigned
	 *         applet or null if problem
	 */
	public String printPDF(String fileName) {
		boolean needWindow = false; // !isNewWindow;
		// not sure what this is about. The applet prints fine
		if (needWindow)
			si.siNewWindow(true, false);
		String s = ((ExportInterface) Interface
				.getInterface("jspecview.export.Exporter")).printPDF(this, fileName);
		if (needWindow)
			si.siNewWindow(false, false);
		return s;
	}

	public String export(String type, int n) {
		if (type == null)
			type = "XY";
		PanelData pd = pd();
		int nMax = pd.getNumberOfSpectraInCurrentSet();
		if (n < -1 || n >= nMax)
			return "Maximum spectrum index (0-based) is " + (nMax - 1) + ".";
		JDXSpectrum spec = (n < 0 ? pd.getSpectrum() : pd.getSpectrumAt(n));
		try {
			return ((ExportInterface) Interface
					.getInterface("jspecview.export.Exporter")).exportTheSpectrum(type,
					null, spec, 0, spec.getXYCoords().length - 1);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}

}
