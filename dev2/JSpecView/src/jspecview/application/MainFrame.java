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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
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
import org.jmol.util.JmolList;

import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
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
import org.jmol.util.Logger;
import org.jmol.util.SB;
import org.jmol.util.Txt;

import jspecview.api.JSVAppInterface;
import jspecview.api.JSVDialog;
import jspecview.api.JSVPanel;
import jspecview.api.JSVPopupMenu;
import jspecview.api.JSVTreeNode;
import jspecview.api.PanelListener;
import jspecview.app.JSVAppPro;
import jspecview.awt.AwtPanel;
import jspecview.awt.AwtParameters;
import jspecview.awt.Platform;
import jspecview.common.JSVPanelNode;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.ColorParameters;
import jspecview.common.Parameters;
import jspecview.common.PeakPickEvent;
import jspecview.common.PrintLayout;
import jspecview.common.ScriptToken;
import jspecview.common.JDXSpectrum;
import jspecview.common.SubSpecChangeEvent;
import jspecview.common.ZoomEvent;
import jspecview.common.JDXSpectrum.IRMode;
import jspecview.export.Exporter;
import jspecview.g2d.G2D;
import jspecview.java.AwtDialogOverlayLegend;
import jspecview.java.AwtDialogPrint;
import jspecview.java.AwtDialogText;
import jspecview.java.AwtDialogView;
import jspecview.java.AwtDropTargetListener;
import jspecview.java.AwtFileHelper;
import jspecview.java.AwtViewPanel;
import jspecview.source.FileReader;
import jspecview.source.JDXSource;
import jspecview.util.JSVColorUtil;
import jspecview.util.JSVEscape;
import jspecview.util.JSVFileManager;

/**
 * The Main Class or Entry point of the JSpecView Application.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class MainFrame extends JFrame implements JmolSyncInterface,
		PanelListener, JSVAppInterface {

	public static void main(String args[]) {
		JSpecView.main(args);
	}

	// ------------------------ Program Properties -------------------------

	/**
   * 
   */
	private final static long serialVersionUID = 1L;
	private final static int MAX_RECENT = 10;

	// ----------------------------------------------------------------------

	public JSViewer      viewer;
	private AppMenu         appMenu;
	private AppToolBar      toolBar;
	private JTextField      commandInput = new JTextField();
	private BorderLayout    mainborderLayout = new BorderLayout();
	private JSplitPane      mainSplitPane = new JSplitPane();
	private final JPanel    nullPanel = new JPanel();
	private JSplitPane      sideSplitPane = new JSplitPane();
	
//	public JSVTree spectraTree;
//	public JDXSource              currentSource;
//  public JmolList<JSVPanelNode> panelNodes;  
//	public ColorParameters        parameters;
//	public RepaintManager         repaintManager;
//	public JSVPanel               selectedPanel;
//	public JSVMainPanel           viewPanel; // alias for spectrumPanel

	public JSVDialog              viewDialog;
	public JSVDialog              overlayLegendDialog;

	private JSVAppPro     					advancedApplet;
	private CommandHistory          commandHistory;
	private DisplaySchemesProcessor dsp;
	private JmolSyncInterface       jmol;
	private Component               jmolDisplay;
	private Dimension               jmolDimensionOld;
	private Container               jmolFrame;
	private Dimension               jmolDimensionNew = new Dimension(250, 200);
	private JSVInterface            jmolOrAdvancedApplet;
	private JSVPopupMenu            jsvpPopupMenu;
	private JSVPanel                prevPanel;
	private JmolList<String>        recentFilePaths = new JmolList<String>();
	private JScrollPane             spectraTreeScrollPane;
	private Component               spectrumPanel;
	private JPanel                  statusPanel = new JPanel();
	private JLabel                  statusLabel = new JLabel();

	private AwtTree tree; // alias for spectraTree, because here it is visible

	private IRMode irMode = IRMode.NO_CONVERT;

	private boolean autoIntegrate;
	private boolean autoShowLegend;
	private boolean isEmbedded;
	private boolean isHidden;
	private boolean interfaceOverlaid;
	private boolean loadImaginary = false;
	private boolean sidePanelOn;
	private boolean showExitDialog;
	private boolean statusbarOn;
	private boolean svgForInkscape = false;
	private boolean toolbarOn;


  private int mainSplitPosition = 200;
	private int fileCount;
	private int nViews;
	private int scriptLevelCount;
	private int splitPosition;

	private String tempDS;
	private String returnFromJmolModel;
	private String recentOpenURL = "http://";
	private String defaultDisplaySchemeName;
	private String recentURL;
	private String integrationRatios;
	
	
	
	////////////////////// get/set methods
	
	public boolean isPro() {
		return true;
	}

	public boolean isSigned() {
		return true;
	}

	public boolean siGetAutoCombine() {
		return interfaceOverlaid;
	}

	public boolean siGetAutoShowLegend() {
		return autoShowLegend;
	}

	public int siGetFileCount() {
		return fileCount;
	}

	public void siSetFileCount(int n) {
		fileCount = n;
	}

	public String siGetIntegrationRatios() {
		return integrationRatios;
	}

	public void siSetIntegrationRatios(String value) {
		integrationRatios = value;
	}
	
	public IRMode siGetIRMode() {
		return irMode;
	}
	
	public void siSetIRMode(IRMode mode) {
		irMode = mode;
	}
	
	public void siSetLoadImaginary(boolean TF) {
		loadImaginary  = TF;
	}

	public void siSetReturnFromJmolModel(String model) {
		returnFromJmolModel = model;
	}

	public String siGetReturnFromJmolModel() {
		return returnFromJmolModel;
	}

	public int siIncrementScriptLevelCount(int n) {
		return scriptLevelCount += n;
	}
	
	public int siIncrementViewCount(int n) {
		return nViews += n;
	}

	/**
	 * Constructor
	 * @param jmolDisplay 
	 * 
	 * @param jmolOrAdvancedApplet
	 */
	public MainFrame(Component jmolDisplay, JSVInterface jmolOrAdvancedApplet) {
		Platform apiPlatform = new Platform();
		viewer = new JSViewer(this, false, false, new G2D(apiPlatform));
		apiPlatform.setViewer(viewer, null);		
		viewer.apiPlatform = apiPlatform;
		jsvpPopupMenu = apiPlatform.getJSVMenuPopup("appMenu");//new AwtPopupMenuOld();
		jsvpPopupMenu.initialize(viewer, "appMenu");
		viewer.jsvpPopupMenu = jsvpPopupMenu; 
		setPlatformFields();
		this.jmolDisplay = jmolDisplay;
		if (jmolDisplay != null)
			jmolFrame = jmolDisplay.getParent();
		this.jmolOrAdvancedApplet = jmolOrAdvancedApplet;
		advancedApplet = (jmolOrAdvancedApplet instanceof JSVAppPro ? (JSVAppPro) jmolOrAdvancedApplet
				: null);
		init();
	}

	private void setPlatformFields() {
		viewer.spectraTree = new AwtTree(viewer);
		viewer.parameters = new AwtParameters("applet");
		viewer.fileHelper = new AwtFileHelper(viewer);
	}

	void exitJSpecView(boolean withDialog) {
		jmolOrAdvancedApplet.saveProperties(viewer.properties);
		if (isEmbedded) {
			awaken(false);
			return;
		}
		dsp.getDisplaySchemes().remove("Current");
		jmolOrAdvancedApplet.exitJSpecView(withDialog && showExitDialog, this);
	}

	private boolean isAwake;
	
	public void awaken(boolean visible) {
		System.out.println("MAINFRAME visible/awake" + visible + " " + isAwake + " " + jmolDisplay);
		if (jmolDisplay == null || isAwake == visible)
			return;
		try {
      isAwake = visible;
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
				System.out.println("awakened");
			} else {
				sideSplitPane.setBottomComponent(nullPanel);
				splitPosition = sideSplitPane.getDividerLocation();
				jmolFrame.add(jmolDisplay);
				jmolDisplay.getSize(jmolDimensionNew);
				jmolDisplay.setSize(jmolDimensionOld);
				sideSplitPane.validate();
				jmolFrame.validate();
        System.out.println("sleeping");
			}
		} catch (Exception e) {
			// ignore
			e.printStackTrace();
		}
		setVisible(visible);
	}

	/**
	 * Task to do when program starts
	 */
	private void init() {

		// initialise MainFrame as a target for the drag-and-drop action
		DropTargetListener dtl = new AwtDropTargetListener(viewer);
		new DropTarget(this, dtl);
		Class<? extends MainFrame> cl = getClass();
		URL iconURL = cl.getResource("icons/spec16.gif"); // imageIcon
		setIconImage(Toolkit.getDefaultToolkit().getImage(iconURL));

		// Initalize application properties with defaults
		// and load properties from file
		Properties properties = viewer.properties = new Properties();
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
		// initialise Spectra tree
		viewer.spectraTree = tree = new AwtTree(viewer);
		tree.setCellRenderer(new SpectraTreeCellRenderer());
		tree.putClientProperty("JTree.lineStyle", "Angled");
		tree.setShowsRootHandles(true);
		tree.setEditable(false);
		tree.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && viewer.selectedPanel != null) {
					viewer.selectedPanel.getPanelData().setZoom(0, 0, 0, 0);
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
		new DropTarget(tree, dtl);

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
	 * Shows or hides certain GUI elements
	 */
	private void setApplicationElements() {
		appMenu.setSelections(sidePanelOn, toolbarOn, statusbarOn,
				viewer.selectedPanel);
		toolBar.setSelections(viewer.selectedPanel);
	}

	/**
	 * Sets the preferences or properties of the application that is loaded from a
	 * properties file.
	 * @param shouldApplySpectrumDisplaySettings 
	 */
	private void setApplicationProperties(
			boolean shouldApplySpectrumDisplaySettings) {

		Properties properties = viewer.properties;
		String recentFilesString = properties.getProperty("recentFilePaths");
		recentFilePaths.clear();
		if (!recentFilesString.equals("")) {
			StringTokenizer st = new StringTokenizer(recentFilesString, ",");
			while (st.hasMoreTokens()) {
				String file = st.nextToken().trim();
				if (file.length() < 100)
					recentFilePaths.addLast(file);
			}
		}
		showExitDialog = Boolean.parseBoolean(properties
				.getProperty("confirmBeforeExit"));

		interfaceOverlaid = Boolean.parseBoolean(properties
				.getProperty("automaticallyOverlay"));
		autoShowLegend = Boolean.parseBoolean(properties
				.getProperty("automaticallyShowLegend"));
		AwtFileHelper fh = (AwtFileHelper) viewer.fileHelper; 
		fh.useDirLastOpened = Boolean.parseBoolean(properties
				.getProperty("useDirectoryLastOpenedFile"));
		fh.useDirLastExported = Boolean.parseBoolean(properties
				.getProperty("useDirectoryLastExportedFile"));
		fh.dirLastOpened = properties.getProperty("directoryLastOpenedFile");
		fh.dirLastExported = properties.getProperty("directoryLastExportedFile");

		sidePanelOn = Boolean.parseBoolean(properties.getProperty("showSidePanel"));
		toolbarOn = Boolean.parseBoolean(properties.getProperty("showToolBar"));
		statusbarOn = Boolean.parseBoolean(properties.getProperty("showStatusBar"));

		// Initialise DisplayProperties
		defaultDisplaySchemeName = properties
				.getProperty("defaultDisplaySchemeName");

		if (shouldApplySpectrumDisplaySettings) {
			viewer.parameters.setBoolean(ScriptToken.GRIDON, Parameters.isTrue(properties
					.getProperty("showGrid")));
			viewer.parameters.setBoolean(ScriptToken.COORDINATESON, Parameters
					.isTrue(properties.getProperty("showCoordinates")));
			viewer.parameters.setBoolean(ScriptToken.XSCALEON, Parameters.isTrue(properties
					.getProperty("showXScale")));
			viewer.parameters.setBoolean(ScriptToken.YSCALEON, Parameters.isTrue(properties
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
			viewer.parameters.integralMinY = Double.parseDouble(properties
					.getProperty("integralMinY"));
			viewer.parameters.integralRange = Double.parseDouble(properties
					.getProperty("integralRange"));
			viewer.parameters.integralOffset = Double.parseDouble(properties
					.getProperty("integralOffset"));
			viewer.parameters.set(null, ScriptToken.INTEGRALPLOTCOLOR, properties
					.getProperty("integralPlotColor"));
		} catch (Exception e) {
			// bad property value
		}

		svgForInkscape = Boolean.parseBoolean(properties
				.getProperty("svgForInkscape"));

	}

	/**
	 * Initializes GUI components
	 * 
	 * @throws Exception
	 */
	private void jbInit() throws Exception {
		toolBar = new AppToolBar(this);
		appMenu = new AppMenu(this);
		appMenu.setRecentMenu(recentFilePaths);
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
		commandHistory = new CommandHistory(viewer, commandInput);
		commandInput.setFocusTraversalKeysEnabled(false);
		commandInput.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				keyPressedEvent(e.getKeyCode(), e.getKeyChar());
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyTyped(KeyEvent e) {
			}

		});

		getContentPane().add(toolBar, BorderLayout.NORTH);
		getContentPane().add(mainSplitPane, BorderLayout.CENTER);

		spectraTreeScrollPane = new JScrollPane(tree);
		if (jmolDisplay != null) {
			JSplitPane leftPanel = new JSplitPane();
			BorderLayout bl1 = new BorderLayout();
			leftPanel.setLayout(bl1);
			JPanel jmolDisplayPanel = new JPanel();
			jmolDisplayPanel.setBackground(Color.BLACK);
			leftPanel.add(jmolDisplayPanel, BorderLayout.SOUTH);
			leftPanel.add(spectraTreeScrollPane, BorderLayout.NORTH);
			sideSplitPane.setTopComponent(spectraTreeScrollPane);
			sideSplitPane.setDividerLocation(splitPosition = 200);
			awaken(true);
			mainSplitPane.setLeftComponent(sideSplitPane);
		} else {
			mainSplitPane.setLeftComponent(spectraTreeScrollPane);
		}
		spectrumPanel = (Component) (viewer.viewPanel = new AwtViewPanel(new BorderLayout()));
		mainSplitPane.setRightComponent(spectrumPanel);
	}

	protected void keyPressedEvent(int keyCode, char keyChar) {
		commandHistory.keyPressed(keyCode);	
		checkCommandLineForTip(keyChar);
		commandInput.requestFocusInWindow();
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
			JmolList<String> tokens = ScriptToken.getTokens(cmd);
			if (tokens.size() == 0)
				return;
			boolean isExact = (cmd.endsWith(" ") || tokens.size() > 1);
			JmolList<ScriptToken> list = ScriptToken.getScriptTokenList(tokens.get(0),
					isExact);
			switch (list.size()) {
			case 0:
				tip = "?";
				break;
			case 1:
				ScriptToken st = list.get(0);
				tip = st.getTip();
				if (tip.indexOf("TRUE") >= 0)
					tip = " (" + viewer.parameters.getBoolean(st) + ")";
				else if (st.name().indexOf("COLOR") >= 0)
					tip = " (" + JSVColorUtil.colorToHexString(viewer.parameters.getElementColor(st))
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
		File file = ((AwtFileHelper) viewer.fileHelper).showFileOpenDialog(this);
		if (file != null)
			openFile(file.getAbsolutePath(), true);
	}

	public void siOpenDataOrFile(String data, String name, JmolList<JDXSpectrum> specs,
			String url, int firstSpec, int lastSpec, boolean isAppend) {
		viewer.openDataOrFile(data, name, specs, url,
				firstSpec, lastSpec, isAppend);
		siValidateAndRepaint();
	}

	public void siSetCurrentSource(JDXSource source) {
		viewer.currentSource = source;
		if (source != null)
		  appMenu.setCloseMenuItem(JSVFileManager.getName(source.getFilePath()));
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
	public void siSetPropertiesFromPreferences(JSVPanel jsvp,
			boolean includeMeasures) {
		ColorParameters ds = dsp.getDisplaySchemes().get(defaultDisplaySchemeName);
		jsvp.getPanelData().addListener(this);
		viewer.parameters.setFor(jsvp, (ds == null ? dsp.getDefaultScheme() : ds),
				includeMeasures);
		if (autoIntegrate)
			jsvp.getPanelData().integrateAll(viewer.parameters);
		jsvp.doRepaint();
	}


	/**
	 * Shows a dialog with the message "Not Yet Implemented"
	 */
	public void showNotImplementedOptionPane() {
		JOptionPane.showMessageDialog(this, "Not Yet Implemented",
				"Not Yet Implemented", JOptionPane.INFORMATION_MESSAGE);
	}

	public void siProcessCommand(String script) {
		runScriptNow(script);
	}

	public boolean runScriptNow(String peakScript) {
		return viewer.runScriptNow(peakScript);
	}

	public void panelEvent(Object eventObj) {
		if (eventObj instanceof PeakPickEvent) {
			viewer.processPeakPickEvent(eventObj, true);
		} else if (eventObj instanceof ZoomEvent) {
			writeStatus("Double-Click highlighted spectrum in menu to zoom out; CTRL+/CTRL- to adjust Y scaling.");
		} else if (eventObj instanceof SubSpecChangeEvent) {
			SubSpecChangeEvent e = (SubSpecChangeEvent) eventObj;
			if (!e.isValid())
				advanceSpectrumBy(-e.getSubIndex());
		}
	}


	public void siSetSelectedPanel(JSVPanel jsvp) {
		if (viewer.selectedPanel != null)
      mainSplitPosition = mainSplitPane.getDividerLocation();
		viewer.viewPanel.setSelectedPanel(jsvp, viewer.panelNodes);
		viewer.selectedPanel = jsvp;
		viewer.spectraTree.setSelectedPanel(this, jsvp);
		validate();
		if (jsvp != null) {
      jsvp.setEnabled(true);
      jsvp.setFocusable(true);
		}
		if (mainSplitPosition != 0)
  		mainSplitPane.setDividerLocation(mainSplitPosition);
	}


	public void siSendPanelChange(JSVPanel jsvp) {
		if (jsvp == prevPanel)
			return;
		prevPanel = jsvp;
		viewer.sendPanelChange(jsvp);
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
		PreferencesDialog pd = new PreferencesDialog(this, viewer, "Preferences", true, dsp);
		viewer.properties = pd.getPreferences();
		boolean shouldApplySpectrumDisplaySetting = pd
				.shouldApplySpectrumDisplaySettingsNow();
		// Apply Properties where appropriate
		setApplicationProperties(shouldApplySpectrumDisplaySetting);

		for (int i = viewer.panelNodes.size(); --i >= 0;)
			siSetPropertiesFromPreferences(viewer.panelNodes.get(i).jsvp,
					shouldApplySpectrumDisplaySetting);

		setApplicationElements();

		dsp.getDisplaySchemes();
		if (defaultDisplaySchemeName.equals("Current")) {
			viewer.setProperty("defaultDisplaySchemeName", tempDS);
		}
	}

	/**
	 * Export spectrum in a given format
	 * 
	 * @param command
	 *          the name of the format to export in
	 */
	void exportSpectrumViaMenu(String command) {
		Exporter.exportSpectrum(viewer, command);
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
			return new Font("Dialog", (node == null || node.getPanelNode() == null
					|| node.getPanelNode().jsvp == null ? Font.BOLD : Font.ITALIC), 12);
		}

	}

	private void advanceSpectrumBy(int n) {
		int i = viewer.panelNodes.size();
		for (; --i >= 0;)
			if (viewer.panelNodes.get(i).jsvp == viewer.selectedPanel)
				break;
		viewer.setFrameAndTreeNode(i + n);
		viewer.selectedPanel.getFocusNow(false);
	}

	public Map<String, Object> getProperty(String key) {
		return viewer.getPropertyAsJavaObject(key);
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
		//System.out.println(Thread.currentThread() + "MainFrame sync JSV>Jmol 21"
			//	+ Thread.currentThread());
		if (jmol != null) { // MainFrame --> embedding application
			jmol.syncScript(msg);
			//System.out.println(Thread.currentThread() + "MainFrame JSV>Jmol sync 22"
				//	+ Thread.currentThread());
			return;
		}
		if (jmolOrAdvancedApplet != null) // MainFrame --> embedding applet
			jmolOrAdvancedApplet.syncToJmol(msg);
	}

	public synchronized void syncScript(String peakScript) {
		//System.out.println(Thread.currentThread() + "MainFrame Jmol>JSV sync 11"
			//	+ Thread.currentThread());
		tree.setEnabled(false);
		viewer.syncScript(peakScript);
		tree.setEnabled(true);
		//System.out.println(Thread.currentThread() + "MainFrame Jmol>JSV sync 12"
			//	+ Thread.currentThread());
	}

	public void siSyncLoad(String filePath) {
		siCloseSource(null);
		siOpenDataOrFile(null, null, null, filePath, -1, -1, false);
		if (viewer.currentSource == null)
			return;
		if (viewer.panelNodes.get(0).getSpectrum().isAutoOverlayFromJmolClick())
			viewer.execView("*", false);
	}

	// //////////////////////// script commands from JSViewer /////////////////

	public void siValidateAndRepaint() {
		validate();
		viewer.requestRepaint();
	}

	public void siExecClose(String value, boolean fromScript) {
		viewer.close(Txt.trimQuotes(value));
		if (!fromScript || viewer.panelNodes.size() == 0) {
			validate();
			repaint();
		}
	}

	public void siExecHidden(boolean b) {
		isHidden = (jmol != null && b);
		setVisible(!isHidden);
	}

	public String siExecLoad(String value) {
		viewer.load(value);
		if (viewer.selectedPanel == null)
			return null;
		PanelData pd = viewer.selectedPanel.getPanelData();
		if (!pd.getSpectrum().is1D() && pd.getDisplay1D())
			return "Click on the spectrum and use UP or DOWN keys to see subspectra.";
		return null;
	}

	public String siExecExport(JSVPanel jsvp, String value) {
		return Exporter.exportCmd(jsvp, ScriptToken.getTokens(value),
				svgForInkscape);
	}

	public void siExecSetInterface(String value) {
		interfaceOverlaid = (value.equalsIgnoreCase("overlay"));
	}

	public void siExecScriptComplete(String msg, boolean isOK) {
		viewer.requestRepaint();
		if (msg != null) {
			writeStatus(msg);
			if (msg.length() == 0)
				msg = null;
		}
		// if (msg == null) {
		// commandInput.requestFocus();
		// }
	}

	public void siExecSetAutoIntegrate(boolean b) {
		autoIntegrate = b;
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
		siOpenDataOrFile(data, null, null, null, -1, -1, true);
	}

	public void setFilePath(String tmpFilePath) {
		siProcessCommand("load " + tmpFilePath);
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

	public void siExecSetCallback(ScriptToken st, String value) {
		if (advancedApplet != null)
			advancedApplet.siExecSetCallback(st, value);
	}

	/**
	 * Opens and displays a file
	 * @param fileName 
	 * @param closeFirst 
	 * 
	 */
	public void openFile(String fileName, boolean closeFirst) {
		if (closeFirst) { // drag/drop
			JDXSource source = JSVPanelNode.findSourceByNameOrId((new File(fileName))
					.getAbsolutePath(), viewer.panelNodes);
			if (source != null)
				siCloseSource(source);
		}
		siOpenDataOrFile(null, null, null, fileName, -1, -1, true);
	}

	@SuppressWarnings("incomplete-switch")
	public void siUpdateBoolean(ScriptToken st, boolean TF) {
		JSVPanel jsvp = viewer.selectedPanel;
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

	public void openURL() {
		String msg = (recentURL == null ? recentOpenURL : recentURL);
		String url = (String) JOptionPane.showInputDialog(null,
				"Enter the URL of a JCAMP-DX File", "Open URL",
				JOptionPane.PLAIN_MESSAGE, null, null, msg);
		if (url == null)
			return;
		recentOpenURL = url;
		siOpenDataOrFile(null, null, null, url, -1, -1, false);
	}

	public void simulate() {
		String msg = "";
		String name = (String) JOptionPane.showInputDialog(null,
				"Enter the name of a compound", "Simulate",
				JOptionPane.PLAIN_MESSAGE, null, null, msg);
		if (name == null)
			return;
		//recentOpenURL = url;
		siOpenDataOrFile(null, null, null, JSVFileManager.SIMULATION_PROTOCOL + "$" + name, -1, -1, true);
	}

	public String siPrintPDF(String fileName) {
		return Exporter.printPDF(viewer, fileName);
	}
	
	public void siCheckCallbacks(String title) {
		// setMainTitle(title);
	}

	// /// JSVPanelNode tree model methods (can be left unimplemented for Android)

	public void siSetNode(JSVPanelNode panelNode, boolean fromTree) {
		if (panelNode.jsvp != viewer.selectedPanel)
			siSetSelectedPanel(panelNode.jsvp);
		siSendPanelChange(panelNode.jsvp);
		siSetMenuEnables(panelNode, false);
		writeStatus("");
	}

	/**
	 * Closes the <code>JDXSource</code> specified by source
	 * 
	 * @param source
	 *          the <code>JDXSource</code>
	 */
	public void siCloseSource(JDXSource source) {
		viewer.closeSource(source);
		appMenu.clearSourceMenu(source);
		setError(false, false);
		setTitle("JSpecView");
		siValidateAndRepaint();			
	}

	public void siSetRecentURL(String filePath) {
		recentURL = filePath;
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

	public void siSetLoaded(String fileName, String filePath) {
		appMenu.setCloseMenuItem(fileName);
		setTitle("JSpecView - " + (filePath.startsWith(JSVFileManager.SIMULATION_PROTOCOL) ? "SIMULATION" : filePath));
		appMenu.setSourceEnabled(true);
	}

	public void siUpdateRecentMenus(String filePath) {

		// ADD TO RECENT FILE PATHS
		if (filePath.length() > 100)
			return;
		if (recentFilePaths.size() >= MAX_RECENT)
			recentFilePaths.remove(MAX_RECENT - 1);
		if (recentFilePaths.contains(filePath))
			recentFilePaths.remove(filePath);
		recentFilePaths.add(0, filePath);
		SB filePaths = new SB();
		int n = recentFilePaths.size();
		for (int index = 0; index < n; index++)
			filePaths.append(", ").append(recentFilePaths.get(index));
		viewer.setProperty("recentFilePaths", (n == 0 ? "" : filePaths
				.substring(2)));
		appMenu.updateRecentMenus(recentFilePaths);
	}

	public void siSetMenuEnables(JSVPanelNode node, boolean isSplit) {
		appMenu.setMenuEnables(node);
		toolBar.setMenuEnables(node);
		// if (isSplit) // not sure why we care...
		// commandInput.requestFocusInWindow();
	}

	public JDXSource siCreateSource(String data, String filePath, URL base,
			int firstSpec, int lastSpec) throws Exception {
		return FileReader.createJDXSource(JSVFileManager
				.getBufferedReaderForString(data), filePath, null, false, loadImaginary , firstSpec,
				lastSpec);
	}

	public JSVPanel siGetNewJSVPanel2(JmolList<JDXSpectrum> specs) {
		return AwtPanel.getPanelMany(viewer, specs, 0, 0);
	}

	public JSVPanel siGetNewJSVPanel(JDXSpectrum spec) {
		return (spec == null ? null : AwtPanel.getPanelOne(viewer, spec));
	}

	public JSVPanelNode siGetNewPanelNode(String id, String fileName,
			JDXSource source, JSVPanel jsvp) {
		return new JSVPanelNode(id, fileName, source, jsvp);
	}

	public void setCursorObject(Object c) {
		setCursor((Cursor) c);
	}


	// debugging

	public void siExecTest(String value) {
		System.out.println(JSVEscape.toJSON(null, viewer.getPropertyAsJavaObject(value), false));
		//syncScript("Jmol sending to JSpecView: jmolApplet_object__5768809713073075__JSpecView: <PeakData file=\"file:/C:/jmol-dev/workspace/Jmol-documentation/script_documentation/examples-12/jspecview/acetophenone.jdx\" index=\"31\" type=\"13CNMR\" id=\"6\" title=\"carbonyl ~200\" peakShape=\"multiplet\" model=\"acetophenone\" atoms=\"1\" xMax=\"199\" xMin=\"197\"  yMax=\"10000\" yMin=\"0\" />");
	}

	public String siSetFileAsString(String value) {
		return JSVFileManager.getFileAsString(value, null);
	}

	private PrintLayout lastPrintLayout;
  public PrintLayout siGetPrintLayout(boolean isJob) {
		PrintLayout pl = new AwtDialogPrint(this, lastPrintLayout, isJob).getPrintLayout();
		if (pl != null)
			lastPrintLayout = pl;
		return pl;
	}
  
	public JSVTreeNode siCreateTree(JDXSource source, JSVPanel[] jsvPanels) {
		return tree.createTree(this, source, jsvPanels);
	}

	public JSViewer siGetViewer() {
		return viewer;
	}

	public void siNewWindow(boolean isSelected, boolean fromFrame) {
		// not implemented for MainFrame
	}

	public JSVDialog siNewDialog(String type, JSVPanel jsvp) {
		if (type.equals("legend"))
			return new AwtDialogOverlayLegend(this, jsvp);
		if (type.equals("view"))
			return new AwtDialogView(viewer, spectraTreeScrollPane, false);
		return null;
	}

	public JmolList<String> getScriptQueue() {
  // applet only
		return null;
	}

	public void siShow(String what) {
		if (what.equals("properties")) {
			AwtDialogText.showProperties(this, viewer.getPanelData().getSpectrum());
		} else if (what.equals("errors")) {
			AwtDialogText.showError(this, viewer.currentSource);
		} else if (what.equals("source")) {
			if (viewer.currentSource == null) {
				if (viewer.panelNodes.size() > 0) {
					JOptionPane.showMessageDialog(this, "Please Select a Spectrum",
							"Select Spectrum", JOptionPane.ERROR_MESSAGE);
				}
				return;
			}
			AwtDialogText.showSource(this, viewer.currentSource);
		}
	}

}
