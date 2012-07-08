/* Copyright (C) 2002-2012  The JSpecView Development Team
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

// CHANGES to 'MainFrame.java' - Main Application GUI
// University of the West Indies, Mona Campus
//
// 20-06-2005 kab - Implemented exporting JPG and PNG image files from the application
//                - Need to sort out JSpecViewFileFilters for Save dialog to include image file extensions
// 21-06-2005 kab - Adjusted export to not prompt for spectrum when exporting JPG/PNG
// 24-06-2005 rjl - Added JPG, PNG file filters to dialog
// 30-09-2005 kab - Added command-line support
// 30-09-2005 kab - Implementing Drag and Drop interface (new class)
// 10-03-2006 rjl - Added Locale overwrite to allow decimal points to be recognised correctly in Europe
// 25-06-2007 rjl - Close file now checks to see if any remaining files still open
//                - if not, then remove a number of menu options
// 05-07-2007 cw  - check menu options when changing the focus of panels
// 06-07-2007 rjl - close imported file closes spectrum and source and updates directory tree
// 06-11-2007 rjl - bug in reading displayschemes if folder name has a space in it
//                  use a default scheme if the file can't be found or read properly,
//                  but there will still be a problem if an attempt is made to
//                  write out a new scheme under these circumstances!
// 23-07-2011 jak - altered code to support drawing scales and units separately
// 21-02-2012 rmh - lots of additions  -  integrated into Jmol

package jspecview.application;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.jmol.api.JSVInterface;
import org.jmol.api.JmolSyncInterface;

import jspecview.applet.JSVAppletPrivatePro;
import jspecview.common.AwtPanel;
import jspecview.common.JSVAppletInterface;
import jspecview.common.JSVDialog;
import jspecview.common.JSVDropTargetListener;
import jspecview.common.JSVPanel;
import jspecview.common.JSVPanelNode;
import jspecview.common.ViewPanel;
import jspecview.common.JSVTree;
import jspecview.common.JSVTreeNode;
import jspecview.common.JSViewer;
import jspecview.common.ViewDialog;
import jspecview.common.PanelData;
import jspecview.common.Parameters;
import jspecview.common.JSVPopupMenu;
import jspecview.common.JSVFileFilter;
import jspecview.common.OverlayLegendDialog;
import jspecview.common.AwtParameters;
import jspecview.common.PanelListener;
import jspecview.common.PeakPickEvent;
import jspecview.common.PrintLayout;
import jspecview.common.PrintLayoutDialog;
import jspecview.common.ScriptInterface;
import jspecview.common.ScriptToken;
import jspecview.common.JDXSpectrum;
import jspecview.common.SubSpecChangeEvent;
import jspecview.common.ZoomEvent;
import jspecview.common.JDXSpectrum.IRMode;
import jspecview.export.Exporter;
import jspecview.source.FileReader;
import jspecview.source.JDXSource;
import jspecview.util.FileManager;
import jspecview.util.Logger;
import jspecview.util.TextFormat;

/**
 * The Main Class or Entry point of the JSpecView Application.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class MainFrame extends JFrame implements JmolSyncInterface,
		PanelListener, ScriptInterface, JSVAppletInterface {

	public static void main(String args[]) {
		JSpecView.main(args);
	}

	// ------------------------ Program Properties -------------------------

	/**
   * 
   */
	private final static long serialVersionUID = 1L;
	private final static int MAX_RECENT = 10;

	private boolean toolbarOn;
	private boolean sidePanelOn;
	private boolean statusbarOn;
	private boolean showExitDialog;

	private String defaultDisplaySchemeName;
	private boolean interfaceOverlaid;
	private boolean autoShowLegend;
	private boolean useDirLastOpened;
	private boolean useDirLastExported;
	private String dirLastOpened;
	private String dirLastExported;
	private String recentFileName;
	private String recentURL;
	private String integrationRatios;
	public void setIntegrationRatios(String value) {
		integrationRatios = value;
	}
	public String getIntegrationRatios() {
		return integrationRatios;
	}

	private IRMode irMode = IRMode.NO_CONVERT;
	public void setIRMode(IRMode mode) {
		irMode = mode;
	}
	private boolean autoIntegrate;

	private AwtParameters parameters = new AwtParameters("application");

	// ----------------------- Application Attributes ---------------------

	private JmolSyncInterface jmol;
	private List<JSVPanelNode> panelNodes = new ArrayList<JSVPanelNode>();
	private List<String> recentFilePaths = new ArrayList<String>(MAX_RECENT);
	private JDXSource currentSource;
	private Properties properties;
	private DisplaySchemesProcessor dsp;
	private String tempDS;

	// ----------------------------------------------------------------------

	private JSVPanel selectedPanel;

	private JSVPopupMenu jsvpPopupMenu = new JSVPopupMenu(this);

	public Object getPopupMenu() {
		return jsvpPopupMenu;
	}

	private AppMenu appMenu;
	private AppToolBar toolBar;

	private BorderLayout mainborderLayout = new BorderLayout();
	private JSplitPane mainSplitPane = new JSplitPane();
	private JSplitPane sideSplitPane = new JSplitPane();
	public DropTargetListener dtl;

	private JScrollPane spectraTreeScrollPane;
	private JPanel statusPanel = new JPanel();
	private JLabel statusLabel = new JLabel();
	private JTextField commandInput = new JTextField();

	private JFileChooser fc;

	private JSVInterface jmolOrAdvancedApplet;
	private JSVAppletPrivatePro advancedApplet;
	private Image iconImage;
	private CommandHistory commandHistory;
	private boolean svgForInkscape;
	private Component jmolDisplay;
	private Dimension jmolDimensionOld;
	private Container jmolFrame;
	private Dimension jmolDimensionNew = new Dimension(250, 200);

	/**
	 * Constructor
	 * 
	 * @param jmolOrAdvancedApplet
	 */
	public MainFrame(Component jmolDisplay, JSVInterface jmolOrAdvancedApplet) {
		this.jmolDisplay = jmolDisplay;
		if (jmolDisplay != null)
			jmolFrame = jmolDisplay.getParent();
		this.jmolOrAdvancedApplet = jmolOrAdvancedApplet;
		advancedApplet = (jmolOrAdvancedApplet instanceof JSVAppletPrivatePro ? (JSVAppletPrivatePro) jmolOrAdvancedApplet
				: null);

		onProgramStart();

	}

	void exitJSpecView(boolean withDialog) {
		jmolOrAdvancedApplet.saveProperties(properties);
		if (isEmbedded) {
			awaken(false);
			return;
		}
		dsp.getDisplaySchemes().remove("Current");
		jmolOrAdvancedApplet.exitJSpecView(withDialog && showExitDialog, this);
	}

	private JPanel nullPanel = new JPanel();
	private int splitPosition;
	private ViewPanel spectrumPanel;

	public void awaken(boolean visible) {
		if (jmolDisplay == null)
			return;
		try {
			if (visible) {
				jmolDimensionOld = new Dimension();
				jmolDisplay.getSize(jmolDimensionOld);
				jmolDisplay.setSize(jmolDimensionNew);
				jmolFrame.remove(jmolDisplay);
				jmolFrame.add(nullPanel);
				sideSplitPane.setBottomComponent(jmolDisplay);
				sideSplitPane.setDividerLocation(splitPosition);
				sideSplitPane.validate();
				jmolFrame.validate();
			} else {
				sideSplitPane.setBottomComponent(nullPanel);
				splitPosition = sideSplitPane.getDividerLocation();
				jmolFrame.add(jmolDisplay);
				jmolDisplay.getSize(jmolDimensionNew);
				jmolDisplay.setSize(jmolDimensionOld);
				sideSplitPane.validate();
				jmolFrame.validate();
			}
		} catch (Exception e) {
			// ignore
			e.printStackTrace();
		}
		setVisible(visible);
	}

	private void getIcons() {
		Class<? extends MainFrame> cl = getClass();
		URL iconURL = cl.getResource("icons/spec16.gif"); // imageIcon
		iconImage = Toolkit.getDefaultToolkit().getImage(iconURL);
		// frameIcon = new ImageIcon(iconURL);
	}

	/**
	 * Shows or hides certain GUI elements
	 */
	private void setApplicationElements() {
		appMenu.setSelections(sidePanelOn, toolbarOn, statusbarOn,
				getSelectedPanel());
		toolBar.setSelections(getSelectedPanel());
	}

	/**
	 * Task to do when program starts
	 */
	private void onProgramStart() {

		// initialise MainFrame as a target for the drag-and-drop action
		new DropTarget(this, getDropListener());

		getIcons();

		// Initalize application properties with defaults
		// and load properties from file
		properties = new Properties();
		// sets the list of recently opened files property to be initially empty
		properties.setProperty("recentFilePaths", "");
		properties.setProperty("confirmBeforeExit", "true");
		properties.setProperty("automaticallyOverlay", "false");
		properties.setProperty("automaticallyShowLegend", "false");
		properties.setProperty("useDirectoryLastOpenedFile", "true");
		properties.setProperty("useDirectoryLastExportedFile", "false");
		properties.setProperty("directoryLastOpenedFile", "");
		properties.setProperty("directoryLastExportedFile", "");
		properties.setProperty("showSidePanel", "true");
		properties.setProperty("showToolBar", "true");
		properties.setProperty("showStatusBar", "true");
		properties.setProperty("defaultDisplaySchemeName", "Default");
		properties.setProperty("showGrid", "false");
		properties.setProperty("showCoordinates", "false");
		properties.setProperty("showXScale", "true");
		properties.setProperty("showYScale", "true");
		properties.setProperty("svgForInkscape", "false");
		properties.setProperty("automaticTAConversion", "false");
		properties.setProperty("AtoTSeparateWindow", "false");
		properties.setProperty("automaticallyIntegrate", "false");
		properties.setProperty("integralMinY", "0.1");
		properties.setProperty("integralFactor", "50");
		properties.setProperty("integralOffset", "30");
		properties.setProperty("integralPlotColor", "#ff0000");

		jmolOrAdvancedApplet.setProperties(properties);

		dsp = new DisplaySchemesProcessor();

		// try loading display scheme from the file system otherwise load it from
		// the jar
		if (!dsp.load("displaySchemes.xml")) {
			if (!dsp.load(getClass().getResourceAsStream(
					"resources/displaySchemes.xml"))) {
				writeStatus("Problem loading Display Scheme");
			}
		}

		setApplicationProperties(true);
		tempDS = defaultDisplaySchemeName;
		fc = (Logger.debugging ? new JFileChooser("C:/temp")
				: useDirLastOpened ? new JFileChooser(dirLastOpened)
						: new JFileChooser());

		JSVFileFilter filter = new JSVFileFilter();

		filter = new JSVFileFilter();
		filter.addExtension("xml");
		filter.addExtension("aml");
		filter.addExtension("cml");
		filter.setDescription("CML/XML Files");
		fc.setFileFilter(filter);

		filter = new JSVFileFilter();
		filter.addExtension("jdx");
		filter.addExtension("dx");
		filter.setDescription("JCAMP-DX Files");
		fc.setFileFilter(filter);

		// initialise Spectra tree

		spectraTree = new JSVTree((ScriptInterface) this);
		spectraTree.setCellRenderer(new SpectraTreeCellRenderer());
		spectraTree.putClientProperty("JTree.lineStyle", "Angled");
		spectraTree.setShowsRootHandles(true);
		spectraTree.setEditable(false);
		spectraTree.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && getSelectedPanel() != null) {
					getSelectedPanel().getPanelData().setZoom(0, 0, 0, 0);
					repaint();
				}
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}

		});
		new DropTarget(spectraTree, getDropListener());

		// Initialise GUI Components
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}

		setApplicationElements();
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		// When application exits ...
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				windowClosing_actionPerformed();
			}
		});
		setSize(800, 500);

	}

	/**
	 * Sets the preferences or properties of the application that is loaded from a
	 * properties file.
	 */
	private void setApplicationProperties(
			boolean shouldApplySpectrumDisplaySettings) {

		String recentFilesString = properties.getProperty("recentFilePaths");
		recentFilePaths.clear();
		if (!recentFilesString.equals("")) {
			StringTokenizer st = new StringTokenizer(recentFilesString, ",");
			while (st.hasMoreTokens())
				recentFilePaths.add(st.nextToken().trim());
		}
		showExitDialog = Boolean.parseBoolean(properties
				.getProperty("confirmBeforeExit"));

		interfaceOverlaid = Boolean.parseBoolean(properties
				.getProperty("automaticallyOverlay"));
		autoShowLegend = Boolean.parseBoolean(properties
				.getProperty("automaticallyShowLegend"));

		useDirLastOpened = Boolean.parseBoolean(properties
				.getProperty("useDirectoryLastOpenedFile"));
		useDirLastExported = Boolean.parseBoolean(properties
				.getProperty("useDirectoryLastExportedFile"));
		dirLastOpened = properties.getProperty("directoryLastOpenedFile");
		dirLastExported = properties.getProperty("directoryLastExportedFile");

		sidePanelOn = Boolean.parseBoolean(properties.getProperty("showSidePanel"));
		toolbarOn = Boolean.parseBoolean(properties.getProperty("showToolBar"));
		statusbarOn = Boolean.parseBoolean(properties.getProperty("showStatusBar"));

		// Initialise DisplayProperties
		defaultDisplaySchemeName = properties
				.getProperty("defaultDisplaySchemeName");

		if (shouldApplySpectrumDisplaySettings) {
			parameters.setBoolean(ScriptToken.GRIDON, Parameters.isTrue(properties
					.getProperty("showGrid")));
			parameters.setBoolean(ScriptToken.COORDINATESON, Parameters
					.isTrue(properties.getProperty("showCoordinates")));
			parameters.setBoolean(ScriptToken.XSCALEON, Parameters.isTrue(properties
					.getProperty("showXScale")));
			parameters.setBoolean(ScriptToken.YSCALEON, Parameters.isTrue(properties
					.getProperty("showYScale")));
		}

		// TODO: Need to apply Properties to all panels that are opened
		// and update coordinates and grid CheckBoxMenuItems

		// Processing Properties
		String autoATConversion = properties.getProperty("automaticTAConversion");
		if (autoATConversion.equals("AtoT")) {
			irMode = IRMode.TO_TRANS;
		} else if (autoATConversion.equals("TtoA")) {
			irMode = IRMode.TO_ABS;
		}

		try {
			autoIntegrate = Boolean.parseBoolean(properties
					.getProperty("automaticallyIntegrate"));
			parameters.integralMinY = Double.parseDouble(properties
					.getProperty("integralMinY"));
			parameters.integralRange = Double.parseDouble(properties
					.getProperty("integralRange"));
			parameters.integralOffset = Double.parseDouble(properties
					.getProperty("integralOffset"));
			parameters.set(null, ScriptToken.INTEGRALPLOTCOLOR, properties
					.getProperty("integralPlotColor"));
		} catch (Exception e) {
			// bad property value
		}

		svgForInkscape = Boolean.parseBoolean(properties
				.getProperty("svgForInkscape"));

	}

	private DropTargetListener getDropListener() {		
		if (dtl == null)
			dtl = new JSVDropTargetListener(this);
		return dtl;
	}

	/**
	 * Initializes GUI components
	 * 
	 * @throws Exception
	 */
	private void jbInit() throws Exception {
		toolBar = new AppToolBar(this);
		appMenu = new AppMenu(this, jsvpPopupMenu);
		appMenu.setRecentMenu(recentFilePaths);
		setIconImage(iconImage);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setJMenuBar(appMenu);
		setTitle("JSpecView");
		getContentPane().setLayout(mainborderLayout);
		sideSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		sideSplitPane.setOneTouchExpandable(true);
		statusLabel.setToolTipText("");
		statusLabel.setHorizontalTextPosition(SwingConstants.LEADING);
		statusLabel.setText("  ");
		statusPanel.setBorder(BorderFactory.createEtchedBorder());
		BorderLayout bl = new BorderLayout();
		bl.setHgap(2);
		bl.setVgap(2);
		statusPanel.setLayout(bl);
		mainSplitPane.setOneTouchExpandable(true);
		mainSplitPane.setResizeWeight(0.3);
		getContentPane().add(statusPanel, BorderLayout.SOUTH);
		statusPanel.add(statusLabel, BorderLayout.NORTH);
		statusPanel.add(commandInput, BorderLayout.SOUTH);
		commandHistory = new CommandHistory(this, commandInput);
		commandInput.setFocusTraversalKeysEnabled(false);
		commandInput.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				commandHistory.keyPressed(e.getKeyCode());
				checkCommandLineForTip(e.getKeyChar());
				commandInput.requestFocusInWindow();
			}

			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			public void keyTyped(KeyEvent e) {
				// checkCommandLineForTip(e.getKeyChar());
			}

		});

		getContentPane().add(toolBar, BorderLayout.NORTH);
		getContentPane().add(mainSplitPane, BorderLayout.CENTER);

		spectraTreeScrollPane = new JScrollPane(spectraTree);
		if (jmolDisplay != null) {
			JSplitPane leftPanel = new JSplitPane();
			BorderLayout bl1 = new BorderLayout();
			leftPanel.setLayout(bl1);
			JPanel jmolDisplayPanel = new JPanel();
			jmolDisplayPanel.setBackground(Color.blue);
			leftPanel.add(jmolDisplayPanel, BorderLayout.SOUTH);
			leftPanel.add(spectraTreeScrollPane, BorderLayout.NORTH);
			sideSplitPane.setTopComponent(spectraTreeScrollPane);
			sideSplitPane.setDividerLocation(splitPosition = 200);
			awaken(true);
			mainSplitPane.setLeftComponent(sideSplitPane);
		} else {
			mainSplitPane.setLeftComponent(spectraTreeScrollPane);
		}
		spectrumPanel = new ViewPanel(new BorderLayout());
		mainSplitPane.setRightComponent(spectrumPanel);
	}

	protected void checkCommandLineForTip(char c) {
		if (c != '\t' && (c == '\n' || c < 32 || c > 126))
			return;
		String cmd = commandInput.getText()
				+ (Character.isISOControl(c) ? "" : "" + c);
		String tip;
		if (cmd.indexOf(";") >= 0)
			cmd = cmd.substring(cmd.lastIndexOf(";") + 1);
		while (cmd.startsWith(" "))
			cmd = cmd.substring(1);
		if (cmd.length() == 0) {
			tip = "Enter a command:";
		} else {
			List<String> tokens = ScriptToken.getTokens(cmd);
			if (tokens.size() == 0)
				return;
			boolean isExact = (cmd.endsWith(" ") || tokens.size() > 1);
			List<ScriptToken> list = ScriptToken.getScriptTokenList(tokens.get(0),
					isExact);
			switch (list.size()) {
			case 0:
				tip = "?";
				break;
			case 1:
				ScriptToken st = list.get(0);
				tip = st.getTip();
				if (tip.indexOf("TRUE") >= 0)
					tip = " (" + parameters.getBoolean(st) + ")";
				else if (st.name().indexOf("COLOR") >= 0)
					tip = " (" + AwtParameters.colorToHexString(parameters.getColor(st))
							+ ")";
				else
					tip = "";
				if (c == '\t' || isExact) {
					tip = st.name() + " " + st.getTip() + tip;
					if (c == '\t')
						commandInput.setText(st.name() + " ");
					break;
				}
				tip = st.name() + " " + tip;
				break;
			default:
				tip = ScriptToken.getNameList(list);
			}
		}
		writeStatus(tip);
	}

	/**
	 * Shows dialog to open a file
	 */
	void showFileOpenDialog() {
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			properties.setProperty("directoryLastOpenedFile", file.getParent());
			openFile(file.getAbsolutePath(), true);
		}
	}

	public void openDataOrFile(String data, String name, List<JDXSpectrum> specs,
			String url, int firstSpec, int lastSpec) {
		JSVTree.openDataOrFile((ScriptInterface) this, data, name, specs, url,
				firstSpec, lastSpec);
		validateAndRepaint();
	}

	public void setCurrentSource(JDXSource source) {
		currentSource = source;
		appMenu.setCloseMenuItem(source == null ? null : FileManager.getName(source
				.getFilePath()));
		boolean isError = (source != null && source.getErrorLog().length() > 0);
		setError(isError, (isError && source.getErrorLog().indexOf("Warning") >= 0));
	}

	private void setError(boolean isError, boolean isWarningOnly) {
		appMenu.setError(isError, isWarningOnly);
		toolBar.setError(isError, isWarningOnly);
	}

	/**
	 * Sets the display properties as specified from the preferences dialog or the
	 * properties file
	 * 
	 * @param jsvp
	 *          the display panel
	 */
	public void setPropertiesFromPreferences(JSVPanel jsvp,
			boolean includeMeasures) {
		Parameters ds = dsp.getDisplaySchemes().get(defaultDisplaySchemeName);
		jsvp.getPanelData().addListener(this);
		parameters.setFor(jsvp, (ds == null ? dsp.getDefaultScheme() : ds),
				includeMeasures);
		if (autoIntegrate)
			jsvp.getPanelData().integrateAll(parameters);
		jsvp.repaint();
	}

	private boolean isEmbedded;
	private boolean isHidden;

	/**
	 * Shows a dialog with the message "Not Yet Implemented"
	 */
	public void showNotImplementedOptionPane() {
		JOptionPane.showMessageDialog(this, "Not Yet Implemented",
				"Not Yet Implemented", JOptionPane.INFORMATION_MESSAGE);
	}

	/*  *//**
	 * Does the necessary actions and cleaning up when JSVFrame closes
	 * 
	 * @param frame
	 *          the JSVFrame
	 */
	/*
	 * private void doInternalFrameClosing(final JInternalFrame frame) {
	 * closeSource(currentSource); setCurrentSource(null); if (panelNodes.size()
	 * == 0) setMenuEnables(null, false); }
	 */
	public JSVPanel getSelectedPanel() {
		return selectedPanel;
	}

	public void processCommand(String script) {
		runScriptNow(script);
	}

	public boolean runScriptNow(String peakScript) {
		return JSViewer.runScriptNow(this, peakScript);
	}

	public void panelEvent(Object eventObj) {
		if (eventObj instanceof PeakPickEvent) {
			JSViewer.processPeakPickEvent(this, eventObj, true);
		} else if (eventObj instanceof ZoomEvent) {
			writeStatus("Double-Click highlighted spectrum in menu to zoom out; CTRL+/CTRL- to adjust Y scaling.");
		} else if (eventObj instanceof SubSpecChangeEvent) {
			SubSpecChangeEvent e = (SubSpecChangeEvent) eventObj;
			if (!e.isValid())
				advanceSpectrumBy(-e.getSubIndex());
		}
	}

	public void setSelectedPanel(JSVPanel jsvp) {
		spectrumPanel.setSelectedPanel(jsvp, panelNodes);
		selectedPanel = jsvp;
		spectraTree.setSelectedPanel((ScriptInterface) this, jsvp);
		validate();
		if (jsvp != null) {
      jsvp.setEnabled(true);
      jsvp.setFocusable(true);
		}
	}

	private JSVPanel prevPanel;

	public void sendPanelChange(JSVPanel jsvp) {
		if (jsvp == prevPanel)
			return;
		prevPanel = jsvp;
		JSViewer.sendPanelChange(this, jsvp);
	}

	// //////// MENU ACTIONS ///////////

	public void setSplitPane(boolean TF) {
		if (TF)
			mainSplitPane.setDividerLocation(200);
		else
			mainSplitPane.setDividerLocation(0);
	}

	public void enableToolbar(boolean isEnabled) {
		if (isEnabled)
			getContentPane().add(toolBar, BorderLayout.NORTH);
		else
			getContentPane().remove(toolBar);
		validate();
	}

	public void showPreferences() {
		PreferencesDialog pd = new PreferencesDialog(this, "Preferences", true,
				properties, dsp);
		properties = pd.getPreferences();
		boolean shouldApplySpectrumDisplaySetting = pd
				.shouldApplySpectrumDisplaySettingsNow();
		// Apply Properties where appropriate
		setApplicationProperties(shouldApplySpectrumDisplaySetting);

		for (int i = panelNodes.size(); --i >= 0;)
			setPropertiesFromPreferences(panelNodes.get(i).jsvp,
					shouldApplySpectrumDisplaySetting);

		setApplicationElements();

		dsp.getDisplaySchemes();
		if (defaultDisplaySchemeName.equals("Current")) {
			properties.setProperty("defaultDisplaySchemeName", tempDS);
		}
	}

	/**
	 * Export spectrum in a given format
	 * 
	 * @param command
	 *          the name of the format to export in
	 */
	void exportSpectrumViaMenu(String command) {
		final String type = command;
		JSVPanel jsvp = getSelectedPanel();
		if (jsvp == null)
			return;
		if (fc == null)
			return;

		if (Logger.debugging) {
			fc.setCurrentDirectory(new File("C:\\JCAMPDX"));
		} else if (useDirLastExported) {
			fc.setCurrentDirectory(new File(dirLastExported));
		}
		String msg = Exporter.exportSpectra(jsvp, this, fc, type,
				recentFileName, dirLastExported);
		jsvp.requestFocusInWindow();
		if (msg != null)
  		dirLastExported = msg;

	}

	protected void windowClosing_actionPerformed() {
		exitJSpecView(true);
	}

	/**
	 * Tree Cell Renderer for the Spectra Tree
	 */
	private class SpectraTreeCellRenderer extends DefaultTreeCellRenderer {
		/**
     * 
     */
		private final static long serialVersionUID = 1L;
		JSVTreeNode node;

		public SpectraTreeCellRenderer() {
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
					hasFocus);

			node = (JSVTreeNode) value;
			return this;
		}

		/**
		 * Returns a font depending on whether a frame is hidden
		 * 
		 * @return the tree node that is associated with an internal frame
		 */
		@Override
		public Font getFont() {
			return new Font("Dialog", (node == null || node.panelNode == null
					|| node.panelNode.jsvp == null ? Font.BOLD : Font.ITALIC), 12);
		}

	}

	private void advanceSpectrumBy(int n) {
		int i = panelNodes.size();
		for (; --i >= 0;)
			if (panelNodes.get(i).jsvp == getSelectedPanel())
				break;
		JSVTree.setFrameAndTreeNode(this, i + n);
		getSelectedPanel().requestFocusInWindow();
	}

	public Map<String, Object> getProperty(String key) {
		if ("".equals(key))
			key = null;
		List<Map<String, Object>> info = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < panelNodes.size(); i++) {
			JSVPanel jsvp = panelNodes.get(i).jsvp;
			if (jsvp == null)
				continue;
			info.add(jsvp.getPanelData().getInfo(jsvp == getSelectedPanel(), key));
		}
		Map<String, Object> map = new Hashtable<String, Object>();
		map.put("items", info);
		return map;
	}

	/**
	 * called by Jmol's StatusListener to register itself, indicating to JSpecView
	 * that it needs to synchronize with it
	 */
	public void register(String appletID, JmolSyncInterface jmolStatusListener) {
		jmol = jmolStatusListener;
		isEmbedded = true;
	}

	public synchronized void syncToJmol(String msg) {
		Logger.info("JSV>Jmol " + msg);
		System.out.println(Thread.currentThread() + "MainFrame sync JSV>Jmol 21"
				+ Thread.currentThread());
		if (jmol != null) { // MainFrame --> embedding application
			jmol.syncScript(msg);
			System.out.println(Thread.currentThread() + "MainFrame JSV>Jmol sync 22"
					+ Thread.currentThread());
			return;
		}
		if (jmolOrAdvancedApplet != null) // MainFrame --> embedding applet
			jmolOrAdvancedApplet.syncToJmol(msg);
	}

	public synchronized void syncScript(String peakScript) {
		System.out.println(Thread.currentThread() + "MainFrame Jmol>JSV sync 11"
				+ Thread.currentThread());
		spectraTree.setEnabled(false);
		JSViewer.syncScript(this, peakScript);
		spectraTree.setEnabled(true);
		System.out.println(Thread.currentThread() + "MainFrame Jmol>JSV sync 12"
				+ Thread.currentThread());
	}

	public void syncLoad(String filePath) {
		closeSource(null);
		openDataOrFile(null, null, null, filePath, -1, -1);
		if (currentSource == null)
			return;
		if (panelNodes.get(0).getSpectrum().isAutoOverlayFromJmolClick())
			JSViewer.execView(this, "*", false);
	}

	// //////////////////////// script commands from JSViewer /////////////////

	public JDXSource getCurrentSource() {
		return currentSource;
	}

	public Parameters getParameters() {
		return parameters;
	}

	public List<JSVPanelNode> getPanelNodes() {
		return panelNodes;
	}

	public void validateAndRepaint() {
		validate();
		repaint();
	}

	/**
	 * Allows Integration of an HNMR spectra
	 * 
	 */
	public void execIntegrate(JDXSpectrum spec) {
		// unnec
	}

	/**
	 * Calculates the predicted colour of the Spectrum
	 */
	public String setSolutionColor(boolean showMessage) {
		getSelectedPanel().showSolutionColor(this);
		return null;
	}

	public void execClose(String value, boolean fromScript) {
		JSVTree.close(this, TextFormat.trimQuotes(value));
		if (!fromScript) {
			validateAndRepaint();
		}
	}

	public void execHidden(boolean b) {
		isHidden = (jmol != null && b);
		setVisible(!isHidden);
	}

	public String execLoad(String value) {
		JSVTree.load((ScriptInterface) this, value);
		if (getSelectedPanel() == null)
			return null;
		if (!getSelectedPanel().getSpectrum().is1D()
				&& getSelectedPanel().getPanelData().getDisplay1D())
			return "Click on the spectrum and use UP or DOWN keys to see subspectra.";
		return null;
	}

	public String execExport(JSVPanel jsvp, String value) {
		return Exporter.exportCmd(jsvp, ScriptToken.getTokens(value),
				svgForInkscape);
	}

	public void execSetInterface(String value) {
		interfaceOverlaid = (value.equalsIgnoreCase("overlay"));
	}

	public void execScriptComplete(String msg, boolean isOK) {
		repaint();
		if (msg != null) {
			writeStatus(msg);
			if (msg.length() == 0)
				msg = null;
		}
		// if (msg == null) {
		// commandInput.requestFocus();
		// }
	}

	public JSVPanel execSetSpectrum(String value) {
		return JSVTree.setSpectrum((ScriptInterface) this, value);
	}

	public void execSetAutoIntegrate(boolean b) {
		autoIntegrate = b;
	}

	public PanelData getPanelData() {
		return getSelectedPanel().getPanelData();
	}

	public JSVDialog getOverlayLegend(JSVPanel jsvp) {
		return new OverlayLegendDialog(this, jsvp);
	}

	// these next patch into the advanced applet routines for JavaScript calls
	// via JSVAppletInterface

	public boolean isPro() {
		return true;
	}

	public boolean isSigned() {
		return true;
	}

	public void addHighlight(double x1, double x2, int r, int g, int b, int a) {
		advancedApplet.addHighlight(x1, x2, r, g, b, a);
	}

	public String exportSpectrum(String type, int n) {
		return advancedApplet.exportSpectrum(type, n);
	}

	public String getCoordinate() {
		return advancedApplet.getCoordinate();
	}

	public String getPropertyAsJSON(String key) {
		return advancedApplet.getPropertyAsJSON(key);
	}

	public Map<String, Object> getPropertyAsJavaObject(String key) {
		return advancedApplet.getPropertyAsJavaObject(key);
	}

	public String getSolnColour() {
		return advancedApplet.getSolnColour();
	}

	public void loadInline(String data) {
		openDataOrFile(data, null, null, null, -1, -1);
	}

	public void setFilePath(String tmpFilePath) {
		processCommand("load " + tmpFilePath);
	}

	/**
	 * ScriptInterface requires this. In the applet, this would be queued
	 */
	public void runScript(String script) {
		// if (advancedApplet != null)
		// advancedApplet.runScript(script);
		// else
		runScriptNow(script);
	}

	public void removeAllHighlights() {
		advancedApplet.removeAllHighlights();
	}

	public void removeHighlight(double x1, double x2) {
		advancedApplet.removeHighlight(x1, x2);
	}

	public void reversePlot() {
		advancedApplet.reversePlot();
	}

	public void setSpectrumNumber(int i) {
		advancedApplet.setSpectrumNumber(i);
	}

	public void toggleCoordinate() {
		advancedApplet.toggleCoordinate();
	}

	public void toggleGrid() {
		advancedApplet.toggleGrid();
	}

	public void toggleIntegration() {
		advancedApplet.toggleIntegration();
	}

	public void execSetCallback(ScriptToken st, String value) {
		if (advancedApplet != null)
			advancedApplet.execSetCallback(st, value);
	}

	/**
	 * Opens and displays a file
	 * 
	 * @param file
	 *          the file
	 */
	public void openFile(String fileName, boolean closeFirst) {
		if (closeFirst) { // drag/drop
			JDXSource source = JSVPanelNode.findSourceByNameOrId((new File(fileName))
					.getAbsolutePath(), panelNodes);
			if (source != null)
				closeSource(source);
		}
		openDataOrFile(null, null, null, fileName, -1, -1);
	}

	public void showProperties() {
		TextDialog.showProperties(this, getPanelData().getSpectrum());
	}

	public void updateBoolean(ScriptToken st, boolean TF) {
		JSVPanel jsvp = getSelectedPanel();
		if (jsvp == null)
			return;
		switch (st) {
		case COORDINATESON:
			toolBar.coordsToggleButton.setSelected(TF);
			break;
		case GRIDON:
			toolBar.gridToggleButton.setSelected(TF);
			break;
		}
	}

	public void enableStatus(boolean TF) {
		if (TF)
			getContentPane().add(statusPanel, BorderLayout.SOUTH);
		else
			getContentPane().remove(statusPanel);
		validate();
	}

	private String recentOpenURL = "http://";

	public void openURL() {
		String msg = (recentURL == null ? recentOpenURL : recentURL);
		String url = (String) JOptionPane.showInputDialog(null,
				"Enter the URL of a JCAMP-DX File", "Open URL",
				JOptionPane.PLAIN_MESSAGE, null, null, msg);
		if (url == null)
			return;
		recentOpenURL = url;
		openDataOrFile(null, null, null, url, -1, -1);
	}

	private PrintLayout lastPrintLayout;

	public void print() {
		JSVPanel jsvp = getSelectedPanel();
		PrintLayout pl;
		if (jsvp == null
				|| (pl = (new PrintLayoutDialog(this, lastPrintLayout))
						.getPrintLayout()) == null)
			return;
		lastPrintLayout = pl;
		((AwtPanel) jsvp).printSpectrum(pl);
	}

	public void toggleOverlayKey() {
		JSVPanel jsvp = getSelectedPanel();
		if (jsvp == null)
			return;
		// boolean showLegend = appMenu.toggleOverlayKeyMenuItem();
		JSViewer.setOverlayLegendVisibility(this, jsvp, true);

	}

	public void checkCallbacks(String title) {
		// setMainTitle(title);
	}

	// /// JSVPanelNode tree model methods (can be left unimplemented for Android)

	private JSVTree spectraTree;
	private int fileCount = 0;

	public int getFileCount() {
		return fileCount;
	}

	public void setFileCount(int n) {
		fileCount = n;
	}

	public Object getSpectraTree() {
		return spectraTree;
	}

	public JSVPanelNode setOverlayVisibility(JSVPanelNode node) {
		JSViewer.setOverlayLegendVisibility(this, node.jsvp,
				appMenu.overlayKeyMenuItem.isSelected());
		return node;
	}

	public void setNode(JSVPanelNode panelNode, boolean fromTree) {
		if (panelNode.jsvp != getSelectedPanel())
			setSelectedPanel(panelNode.jsvp);
		sendPanelChange(panelNode.jsvp);
		setMenuEnables(panelNode, false);
		if (getSelectedPanel().getPanelData().getShowIntegration())
			writeStatus("Use CTRL-LEFT-DRAG to measure an integration value.");
		else
			writeStatus("");
	}

	/**
	 * Closes the <code>JDXSource</code> specified by source
	 * 
	 * @param source
	 *          the <code>JDXSource</code>
	 */
	public void closeSource(JDXSource source) {
		JSVTree.closeSource(this, source);
		appMenu.clearSourceMenu(source);
		setError(false, false);
		setTitle("JSpecView");
	}

	private int nOverlays;

	public int incrementViewCount(int n) {
		return nOverlays += n;
	}

	public void setRecentURL(String filePath) {
		recentURL = filePath;
	}

	public void setRecentFileName(String fileName) {
		recentFileName = fileName;
	}

	/**
	 * Writes a message to the status bar
	 * 
	 * @param msg
	 *          the message
	 */
	public void writeStatus(String msg) {
		if (msg == null)
			msg = "Unexpected Error";
		if (msg.length() == 0)
			msg = "Enter a command:";
		statusLabel.setText(msg);
	}

	public void setLoaded(String fileName, String filePath) {
		appMenu.setCloseMenuItem(fileName);
		setTitle("JSpecView - " + filePath);
		appMenu.setSourceEnabled(true);
	}

	public boolean getAutoCombine() {
		return interfaceOverlaid;
	}

	public boolean getAutoShowLegend() {
		return autoShowLegend;
	}

	public void updateRecentMenus(String filePath) {

		// ADD TO RECENT FILE PATHS
		if (recentFilePaths.size() >= MAX_RECENT)
			recentFilePaths.remove(MAX_RECENT - 1);
		if (recentFilePaths.contains(filePath))
			recentFilePaths.remove(filePath);
		recentFilePaths.add(0, filePath);
		StringBuffer filePaths = new StringBuffer();
		int n = recentFilePaths.size();
		for (int index = 0; index < n; index++)
			filePaths.append(", ").append(recentFilePaths.get(index));
		properties.setProperty("recentFilePaths", (n == 0 ? "" : filePaths
				.substring(2)));
		appMenu.updateRecentMenus(recentFilePaths);
	}

	public void process(List<JDXSpectrum> specs) {
		JDXSpectrum.process(specs, irMode, parameters);
	}

	public void setMenuEnables(JSVPanelNode node, boolean isSplit) {
		appMenu.setMenuEnables(node);
		toolBar.setMenuEnables(node);
		// if (isSplit) // not sure why we care...
		// commandInput.requestFocusInWindow();
	}

	public JDXSource createSource(String data, String filePath, URL base,
			int firstSpec, int lastSpec) throws Exception {
		return FileReader.createJDXSource(FileManager
				.getBufferedReaderForString(data), filePath, null, false, firstSpec,
				lastSpec);
	}

	public URL getDocumentBase() {
		return null;
	}

	public JSVPanel getNewJSVPanel(List<JDXSpectrum> specs) {
		return AwtPanel.getJSVPanel(specs, 0, 0, jsvpPopupMenu);
	}

	public JSVPanel getNewJSVPanel(JDXSpectrum spec) {
		return (spec == null ? null : AwtPanel.getNewPanel(spec, jsvpPopupMenu));
	}

	public JSVPanelNode getNewPanelNode(String id, String fileName,
			JDXSource source, JSVPanel jsvp) {
		return new JSVPanelNode(id, fileName, source, jsvp);
	}

	public void checkOverlay() {
		if (spectrumPanel != null)
			spectrumPanel.markSelectedPanels(panelNodes);
		new ViewDialog(this, spectraTreeScrollPane, false);
	}

	private String returnFromJmolModel;

	public void setReturnFromJmolModel(String model) {
		returnFromJmolModel = model;
	}

	public String getReturnFromJmolModel() {
		return returnFromJmolModel;
	}

	// debugging

	public void execTest(String value) {
		syncScript("<PeakData file=\"c:/temp/crspectra.jdx\" index=\"23\" type=\"UV-VIS\" id=\"1\" title=\"Spin Forbidden Band ~694nm\" peakShape=\"singlet\" model=\"urea\" xMax=\"710\" xMin=\"670\"    />");
	}

}
