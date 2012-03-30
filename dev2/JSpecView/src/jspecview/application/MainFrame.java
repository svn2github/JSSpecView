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
import java.beans.PropertyVetoException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jmol.api.JSVInterface;
import org.jmol.api.JmolSyncInterface;

import jspecview.applet.JSVAppletPrivatePro;
import jspecview.common.AwtPanel;
import jspecview.common.JSVAppletInterface;
import jspecview.common.JSVDialog;
import jspecview.common.JSVDropTargetListener;
import jspecview.common.JSVPanel;
import jspecview.common.JSVSpecNode;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.Parameters;
import jspecview.common.JSVFrame;
import jspecview.common.JSVPanelPopupMenu;
import jspecview.common.JSVFileFilter;
import jspecview.common.OverlayLegendDialog;
import jspecview.common.AwtParameters;
import jspecview.common.PanelListener;
import jspecview.common.PeakPickEvent;
import jspecview.common.PrintLayoutDialog;
import jspecview.common.ScriptInterface;
import jspecview.common.ScriptToken;
import jspecview.common.JDXSpectrum;
import jspecview.common.SubSpecChangeEvent;
import jspecview.common.ZoomEvent;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.export.Exporter;
import jspecview.source.FileReader;
import jspecview.source.JDXSource;
import jspecview.util.FileManager;
import jspecview.util.Logger;
import jspecview.util.Parser;
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

  public class JSVTreeNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 1L;
    public JSVSpecNode specNode;

    public JSVTreeNode(String text, JSVSpecNode specNode) {
      super(text);
      this.specNode = specNode;
    }
  }

  public static void main(String args[]) {
    JSpecView.main(args);
  }

  //  ------------------------ Program Properties -------------------------

  /**
   * 
   */
  private final static long serialVersionUID = 1L;
  private final static int FILE_OPEN_OK = 0;
  private final static int FILE_OPEN_ALREADY = -1;
  //private final static int FILE_OPEN_URLERROR = -2;
  private final static int FILE_OPEN_ERROR = -3;
  private final static int FILE_OPEN_NO_DATA = -4;
  private final static int MAX_RECENT = 10;

  private boolean toolbarOn;
  private boolean sidePanelOn;
  private boolean statusbarOn;
  private boolean showExitDialog;

  private String defaultDisplaySchemeName;
  private boolean autoOverlay;
  private boolean autoShowLegend;
  private boolean useDirLastOpened;
  private boolean useDirLastExported;
  private String dirLastOpened;
  private String dirLastExported;
  private String recentFileName;
  private String recentURL;

  private int irMode = JDXSpectrum.TA_NO_CONVERT;
  private boolean autoIntegrate;

  private AwtParameters parameters = new AwtParameters("application");

  //  ----------------------- Application Attributes ---------------------

  private JmolSyncInterface jmol;
  private List<JSVSpecNode> specNodes = new ArrayList<JSVSpecNode>();
  private List<String> recentFilePaths = new ArrayList<String>(MAX_RECENT);
  private JDXSource currentSource;
  private Properties properties;
  private DisplaySchemesProcessor dsp;
  private String tempDS;

  //   ----------------------------------------------------------------------

  private JSVPanel selectedPanel;

  private JSVPanelPopupMenu jsvpPopupMenu = new JSVPanelPopupMenu(this);
  private AppMenu appMenu;  
  private AppToolBar toolBar;

  private BorderLayout mainborderLayout = new BorderLayout();
  private JSplitPane mainSplitPane = new JSplitPane();
  private JSplitPane sideSplitPane = new JSplitPane();
  private JScrollPane scrollPane = new JScrollPane();
  private ScrollableDesktopPane desktopPane = new ScrollableDesktopPane();
  public DropTargetListener dtl;


  private JSVTreeNode rootNode;
  private DefaultTreeModel spectraTreeModel;
  private JTree spectraTree;
  private JScrollPane spectraTreePane;
  private JPanel statusPanel = new JPanel();
  private JLabel statusLabel = new JLabel();
  private JTextField commandInput = new JTextField();
  
  private JFileChooser fc;

  private JSVInterface jmolOrAdvancedApplet;
  private JSVAppletPrivatePro advancedApplet;
  private Image iconImage;
  private ImageIcon frameIcon;
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
    this.jmolOrAdvancedApplet = jmolOrAdvancedApplet;
    advancedApplet = (jmolOrAdvancedApplet instanceof JSVAppletPrivatePro ? (JSVAppletPrivatePro) jmolOrAdvancedApplet : null);

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

  public void awaken(boolean visible) {
    if (jmolDisplay == null)
      return;
    try {
    if (visible) {
      jmolDimensionOld = new Dimension();
      jmolFrame = jmolDisplay.getParent();
      jmolDisplay.getSize(jmolDimensionOld);
      jmolDisplay.setSize(jmolDimensionNew);
      sideSplitPane.validate();
      jmolFrame.invalidate();
    } else {
      jmolFrame.add(jmolDisplay);
      jmolDisplay.getSize(jmolDimensionNew);
      jmolDisplay.setSize(jmolDimensionOld);
      jmolFrame.validate();
    }
    } catch(Exception e) {
      // ignore
      e.printStackTrace();
    }
    setVisible(visible);
  }

  private void getIcons() {
    Class<? extends MainFrame> cl = getClass();
    URL iconURL = cl.getResource("icons/spec16.gif"); //imageIcon
    iconImage = Toolkit.getDefaultToolkit().getImage(iconURL);
    frameIcon = new ImageIcon(iconURL);
  }

  /**
   * Shows or hides certain GUI elements
   */
  private void setApplicationElements() {
    appMenu.setSelections(sidePanelOn, toolbarOn, statusbarOn, getSelectedPanel());
    toolBar.setSelections(getSelectedPanel());
  }

  /**
   * Task to do when program starts
   */
  private void onProgramStart() {


    
    // initialise MainFrame as a target for the drag-and-drop action
    new DropTarget(this, getDropListener());

    getIcons();
//    // initialise Spectra tree
//    initSpectraTree();
//
//    // Initialise GUI Components
    try {
      //jbInit();
    } catch (Exception e) {
      e.printStackTrace();
    }

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

    // try loading display scheme from the file system otherwise load it from the jar
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
    initSpectraTree();

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

    autoOverlay = Boolean.parseBoolean(properties
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
      parameters.setBoolean(ScriptToken.GRIDON, properties
          .getProperty("showGrid"));
      parameters.setBoolean(ScriptToken.COORDINATESON, properties
          .getProperty("showCoordinates"));
      parameters.setBoolean(ScriptToken.XSCALEON, properties
          .getProperty("showXScale"));
      parameters.setBoolean(ScriptToken.YSCALEON, properties
          .getProperty("showYScale"));
    }

    // TODO: Need to apply Properties to all panels that are opened
    // and update coordinates and grid CheckBoxMenuItems

    // Processing Properties
    String autoATConversion = properties.getProperty("automaticTAConversion");
    if (autoATConversion.equals("AtoT")) {
      irMode = JDXSpectrum.TO_TRANS;
    } else if (autoATConversion.equals("TtoA")) {
      irMode = JDXSpectrum.TO_ABS;
    }

    try {
      autoIntegrate = Boolean.parseBoolean(properties
          .getProperty("automaticallyIntegrate"));
      parameters.integralMinY = Double.parseDouble(properties
          .getProperty("integralMinY"));
      parameters.integralFactor = Double.parseDouble(properties
          .getProperty("integralFactor"));
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

  /**
   * Creates tree representation of files that are opened
   */
  private void initSpectraTree() {
    currentSource = null;
    rootNode = new JSVTreeNode("Spectra", null);
    spectraTreeModel = new DefaultTreeModel(rootNode);
    spectraTree = new JTree(spectraTreeModel);
    spectraTree.getSelectionModel().setSelectionMode(
        TreeSelectionModel.SINGLE_TREE_SELECTION);
    spectraTree.setCellRenderer(new SpectraTreeCellRenderer());
    spectraTree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        JSVTreeNode node = (JSVTreeNode) spectraTree
            .getLastSelectedPathComponent();
        if (node == null) {
          return;
        }
        if (node.isLeaf()) {
          setFrame(node.specNode, true);
        }
        currentSource = node.specNode.source;
        appMenu.setCloseMenuItem(node.specNode.fileName);
      }
    });
    spectraTree.putClientProperty("JTree.lineStyle", "Angled");
    spectraTree.setShowsRootHandles(true);
    spectraTree.setEditable(false);
    spectraTree.setRootVisible(false);
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
  }

  private DropTargetListener getDropListener() {
    if (dtl == null)
      dtl = new JSVDropTargetListener(this, true);
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
        //        checkCommandLineForTip(e.getKeyChar());
      }

    });

    getContentPane().add(toolBar, BorderLayout.NORTH);
    getContentPane().add(mainSplitPane, BorderLayout.CENTER);

    spectraTreePane = new JScrollPane(spectraTree);
    if (jmolDisplay != null) {
      JSplitPane leftPanel = new JSplitPane();
      BorderLayout bl1 = new BorderLayout();
      leftPanel.setLayout(bl1);
      JPanel jmolDisplayPanel = new JPanel();
      jmolDisplayPanel.setBackground(Color.blue);
      leftPanel.add(jmolDisplayPanel, BorderLayout.SOUTH);
      leftPanel.add(spectraTreePane, BorderLayout.NORTH);
      sideSplitPane.setTopComponent(spectraTreePane);
      sideSplitPane.setBottomComponent(jmolDisplay);
      sideSplitPane.setDividerLocation(200);
      sideSplitPane.setResizeWeight(0.6);
      awaken(true);
      mainSplitPane.setLeftComponent(sideSplitPane);
    } else {
      mainSplitPane.setLeftComponent(spectraTreePane);
    }
    //mainSplitPane.setDividerLocation(200);
    scrollPane.getViewport().add(desktopPane);
    mainSplitPane.setRightComponent(scrollPane);

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

  /**
   * Opens and displays a file, either local or remote
   * 
   * @param fileOrURL
   */
  public void openFile(String fileOrURL, int firstSpec, int lastSpec) {
    openDataOrFile(null, null, null, fileOrURL, firstSpec, lastSpec);
  }

  private int nOverlay;


  private int openDataOrFile(String data, String name, List<JDXSpectrum> specs,
                             String url, int firstSpec, int lastSpec) {
    // name could be "NONE" here from overlay
    writeStatus("");
    String filePath = null;
    String fileName = null;
    File file = null;
    boolean isOverlay = false;
    if (data != null) {
    } else if (specs != null) {
      isOverlay = true;
      fileName = filePath = "Overlay" + (++nOverlay);
    } else if (url != null) {
      try {
        URL u = new URL(url);
        fileName = FileManager.getName(url);
        filePath = u.toString();
        //recentJmolName = filePath;
        recentURL = filePath;
      } catch (MalformedURLException e) {
        file = new File(url);
      }
    }
    if (file != null) {
      fileName = recentFileName = file.getName();
      filePath = file.getAbsolutePath();
      //recentJmolName = (url == null ? filePath.replace('\\', '/') : url);
      recentURL = null;
    }
    if (JSVSpecNode.isOpen(specNodes, filePath)) {
      writeStatus(filePath + " is already open");
      return FILE_OPEN_ALREADY;
    }
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    try {
      setCurrentSource(isOverlay ? JDXSource.createOverlay(url, specs)
          : FileReader.createJDXSource(FileManager
              .getBufferedReaderForString(data), filePath, null, false,
              firstSpec, lastSpec));
    } catch (Exception e) {
      e.printStackTrace();
      writeStatus(e.getMessage());
      setCursor(Cursor.getDefaultCursor());
      return FILE_OPEN_ERROR;
    }
    setCursor(Cursor.getDefaultCursor());
    System.gc();
    currentSource.setFilePath(filePath);
    appMenu.setCloseMenuItem(fileName);
    setTitle("JSpecView - " + filePath);

    // add calls to enable Menus that were greyed out until a file is opened.

    // if current spectrum is not a Peak Table then enable Menu to re-export

    appMenu.setSourceEnabled(true);

    JDXSpectrum spec = currentSource.getJDXSpectrum(0);
    if (spec == null) {
      return FILE_OPEN_NO_DATA;
    }

    boolean autoOverlay = this.autoOverlay || spec.isAutoOverlayFromJmolClick();

    specs = currentSource.getSpectra();
    boolean overlay = isOverlay || autoOverlay
        && currentSource.isCompoundSource;
    JDXSpectrum.process(specs, irMode, autoIntegrate, parameters);
    if (overlay) {
      overlay(currentSource, (isOverlay ? url : null));
    } else {
      splitSpectra(currentSource);
    }
    if (!isOverlay)
      updateRecentMenus(filePath);
    //setEnables(node);
    return FILE_OPEN_OK;
  }


  private void updateRecentMenus(String filePath) {

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
    properties.setProperty("recentFilePaths", (n == 0 ? "" : filePaths.substring(2)));
    appMenu.updateRecentMenus(recentFilePaths);
  }

  private void setCurrentSource(JDXSource source) {
    currentSource = source;
    boolean isError = (source != null && source.getErrorLog().length() > 0);
    setError(isError, (isError && source.getErrorLog().indexOf("Warning") >= 0));
  }

  private void setError(boolean isError, boolean isWarningOnly) {
    appMenu.setError(isError, isWarningOnly);
    toolBar.setError(isError, isWarningOnly);
  }

  /**
   * Checks to see if this is an XML file
   * 
   * @param file
   * @return true if anIML or CML
   */

  /**
   * Sets the display properties as specified from the preferences dialog or the
   * properties file
   * 
   * @param jsvp
   *        the display panel
   */
  public void setJSVPanelProperties(JSVPanel jsvp, boolean includeMeasures) {

    Parameters ds = dsp.getDisplaySchemes().get(defaultDisplaySchemeName);
    jsvp.getPanelData().addListener(this);
    parameters.setFor(jsvp, (ds == null ? dsp.getDefaultScheme() : ds),
        includeMeasures);
    jsvp.repaint();
  }

  /**
   * Overlays the spectra of the specified <code>JDXSource</code>
   * 
   * @param source
   *        the <code>JDXSource</code>
   * @throws ScalesIncompatibleException
   */
  private void overlay(JDXSource source, String name) {
    List<JDXSpectrum> specs = source.getSpectra();
    JSVPanel jsvp = AwtPanel.getJSVPanel(specs, 0, 0, jsvpPopupMenu);
    jsvp.setTitle(source.getTitle());

    setJSVPanelProperties(jsvp, true);

    JSVFrame frame = new JSVFrame((name == null ? source.getTitle() : name));
    frame.setFrameIcon(frameIcon);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.addInternalFrameListener(new JSVInternalFrameListener(source));
    frame.setMinimumSize(new Dimension(365, 200));
    frame.setPreferredSize(new Dimension(365, 200));
    frame.getContentPane().add((Component) jsvp);
    desktopPane.add(frame);
    frame.setSize(550, 350);
    try {
      frame.setMaximum(true);
    } catch (PropertyVetoException pve) {
    }
    frame.show();
    JSVFrame[] frames = new JSVFrame[] { frame };
    createTree(source, frames);
    validate();
    repaint();

    JSVSpecNode node = JSVSpecNode.findNode(getSelectedPanel(), specNodes);
    if (autoShowLegend
        && getSelectedPanel().getPanelData().getNumberOfGraphSets() == 1)
      node.setLegend(
          new OverlayLegendDialog(this, jsvp));
    setMenuEnables(node);
  }

  /**
   * Displays the spectrum of the <code>JDXSource</code> specified by source in
   * separate windows
   * 
   * @param source
   *        the <code>JDXSource</code>
   */
  private void splitSpectra(JDXSource source) {
    List<JDXSpectrum> specs = source.getSpectra();
    JSVFrame[] frames = new JSVFrame[specs.size()];
    for (int i = 0; i < specs.size(); i++) {
      JDXSpectrum spec = specs.get(i);
      JSVPanel jsvp = AwtPanel.getNewPanel(spec, jsvpPopupMenu);
      setJSVPanelProperties(jsvp, true);
      JSVFrame frame = new JSVFrame(spec.getTitleLabel());
      frame.setFrameIcon(frameIcon);
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      frame.setMinimumSize(new Dimension(365, 200));
      frame.setPreferredSize(new Dimension(365, 200));
      frame.getContentPane().add((Component) jsvp);
      frame.addInternalFrameListener(new JSVInternalFrameListener(source));
      frames[i] = frame;
      desktopPane.add(frame);
      frame.setVisible(true);
      frame.setSize(550, 350);
      try {
        frame.setMaximum(true);
      } catch (PropertyVetoException pve) {
      }
    }

    // arrange windows in ascending order
    for (int i = (specs.size() - 1); i >= 0; i--)
      frames[i].toFront();
    createTree(source, frames);
    JSVSpecNode node = JSVSpecNode.findNode(getSelectedPanel(), specNodes);
    setMenuEnables(node);
    commandInput.requestFocusInWindow();
  }

  /**
   * Closes the <code>JDXSource</code> specified by source
   * 
   * @param source
   *        the <code>JDXSource</code>
   */
  @SuppressWarnings("unchecked")
  public void closeSource(JDXSource source) {
    // Remove nodes and dispose of frames
    String fileName = (source == null ? null : source.getFilePath());
    List<JSVTreeNode> toDelete = new ArrayList<JSVTreeNode>();
    Enumeration<JSVTreeNode> enume = rootNode.children();
    while (enume.hasMoreElements()) {
      JSVTreeNode node = enume.nextElement();
      if (fileName == null
          || node.specNode.source.getFilePath().equals(fileName)) {
        for (Enumeration<JSVTreeNode> e = node.children(); e.hasMoreElements();) {
          JSVTreeNode childNode = e.nextElement();
          toDelete.add(childNode);
          childNode.specNode.frame.dispose();
          specNodes.remove(childNode.specNode);
        }
        toDelete.add(node);
        if (fileName != null)
          break;
      }
    }

    for (int i = 0; i < toDelete.size(); i++)
      spectraTreeModel.removeNodeFromParent(toDelete.get(i));
    //initSpectraTree();

    if (source == null) {
      jsvpPopupMenu.dispose();
      if (getSelectedPanel() != null)
        getSelectedPanel().dispose();
      if (currentSource != null)
        currentSource.dispose();
    }    
    appMenu.clearSourceMenu(source);
    setSelectedPanel(null);
    currentSource = null;
    System.gc();
    setError(false, false);
    setTitle("JSpecView");
    if (source != null) 
      setSpectrumNumberAndTreeNode(specNodes.size());
    setFileCount();
    System.gc();
    Logger.checkMemory();
  }

  private void setFileCount() {
    int max = 0;
    for (int i = 0; i < specNodes.size(); i++) {
      float f = Parser.parseFloat(specNodes.get(i).id);
      if (f >= max + 1)
        max = (int) Math.floor(f);
    }
    fileCount = max;
  }

  private int fileCount = 0;
  private boolean isEmbedded;
  private boolean isHidden;

  /**
   * Adds the <code>JDXSource</code> info specified by the
   * <code>fileName<code> to
   * the tree model for the side panel.
   * 
   * @param frame
   * @param specInfos2
   * 
   * @param fileName
   *        the name of the file
   * @param frames
   *        an array of JSVFrames
   */
  public void createTree(JDXSource source, JSVFrame[] frames) {
    String fileName = FileManager.getName(source.getFilePath());
    JSVSpecNode specNode = new JSVSpecNode(null, fileName, source, null, null);
    JSVTreeNode fileNode = new JSVTreeNode(fileName, specNode);
    specNode.setTreeNode(fileNode);
    spectraTreeModel.insertNodeInto(fileNode, rootNode, rootNode
        .getChildCount());
    spectraTree.scrollPathToVisible(new TreePath(fileNode.getPath()));

    fileCount++;
    for (int i = 0; i < frames.length; i++) {
      JSVPanel jsvp = getPanel0(frames[i]);
      String id = fileCount + "." + (i + 1);
      specNode = new JSVSpecNode(id, fileName, source, frames[i], jsvp);
      JSVTreeNode treeNode = new JSVTreeNode(specNode.toString(), specNode);
      specNode.setTreeNode(treeNode);
      specNodes.add(specNode);
      spectraTreeModel.insertNodeInto(treeNode, fileNode, fileNode
          .getChildCount());
      spectraTree.scrollPathToVisible(new TreePath(treeNode.getPath()));
    }
    selectFrameNode(frames[0]);
  }

  /**
   * Shows a dialog with the message "Not Yet Implemented"
   */
  public void showNotImplementedOptionPane() {
    JOptionPane.showMessageDialog(this, "Not Yet Implemented",
        "Not Yet Implemented", JOptionPane.INFORMATION_MESSAGE);
  }

  public JSVSpecNode selectFrameNode(JSVFrame frame) {
    // Find Node in SpectraTree and select it
    JSVSpecNode node = JSVSpecNode.findNode(frame, specNodes);
    if (node == null)
      return null;
    spectraTree.setSelectionPath(new TreePath(((JSVTreeNode) node.treeNode)
        .getPath()));
    JSViewer.setOverlayLegendVisibility(this, node.jsvp, appMenu.overlayKeyMenuItem.isSelected());
    return node;
  }

  /**
   * Does the necessary actions and cleaning up when JSVFrame closes
   * 
   * @param frame
   *        the JSVFrame
   */
  private void doInternalFrameClosing(final JInternalFrame frame) {

    closeSource(currentSource);
    setCurrentSource(null);
    if (specNodes.size() == 0)
      setMenuEnables(null);
  }

  private void setMenuEnables(JSVSpecNode node) {
      appMenu.setMenuEnables(node);
      toolBar.setMenuEnables(node);
  }

  public JSVPanel getSelectedPanel() {
    JSVFrame frame = (JSVFrame) desktopPane.getSelectedFrame();
    return (frame == null ? selectedPanel : getPanel0(frame));
  }

//  private JSVFrame getCurrentFrame() {
//    JSVFrame frame = (JSVFrame) desktopPane.getSelectedFrame();
//    return (frame != null ? frame : getSelectedPanel() == null ? null
//        : (JSVFrame) (JSVSpecNode.findNode(getSelectedPanel(), specNodes).frame));
//  }

  public void processCommand(String script) {
    runScriptNow(script);
  }
  
  public void runScriptNow(String peakScript) {
    JSViewer.runScriptNow(this, peakScript);
  }

  private void close(String value) {
    if (value == null || value.equalsIgnoreCase("all")) {
      closeSource(null);
      return;
    }
    value = value.replace('\\', '/');
    if (value.endsWith("*")) {
      value = value.substring(0, value.length() - 1);
      for (int i = specNodes.size(); --i >= 0;)
        if (specNodes.get(i).fileName.startsWith(value))
          closeSource(specNodes.get(i).source);
      return;
    }
    JDXSource source = (value.length() == 0 ? currentSource : JSVSpecNode
        .findSourceByNameOrId(value, specNodes));
    if (source == null)
      return;
    closeSource(source);
  }

  public void setSpectrumNumberAndTreeNode(int n) {
    setFrameAndTreeNode(n - 1);
  }

  private void setFrameAndTreeNode(int i) {
    if (specNodes == null || i < 0 || i >= specNodes.size())
      return;
    setFrameAndTreeNode(specNodes.get(i));
  }

  private void setFrameAndTreeNode(JSVSpecNode node) {
    selectFrameNode((JSVFrame) node.frame);
    setFrame(node, false);
  }

  public JSVPanel setSpectrumIndex(int i) {
    if (specNodes != null && i >= 0 && i < specNodes.size())
      setFrame(specNodes.get(i), false);
    return getSelectedPanel();
  }

  private void setFrame(JSVSpecNode specNode, boolean fromTree) {
    JSVFrame frame = (JSVFrame) specNode.frame;
    setSelectedPanel(specNode.jsvp);
    frame.setVisible(true);
    frame.moveToFront();
    try {
      frame.setSelected(true);
    } catch (PropertyVetoException pve) {
    }
    if (fromTree && frame.isEnabled()) {
      getSelectedPanel().setEnabled(true);
      sendFrameChange(specNode.jsvp);
      if (desktopPane.getStyle() == ScrollableDesktopPane.STYLE_STACK)
        desktopPane.setAllEnabled(false);
      getSelectedPanel().setEnabled(true);
    }
    setMenuEnables(specNode);
    if (getSelectedPanel().getSpectrum().hasIntegral())
      writeStatus("Use CTRL-LEFT-DRAG to measure an integration value.");
    else
      writeStatus("");
  }

  public void panelEvent(Object eventObj) {
    if (eventObj instanceof PeakPickEvent) {
      JSViewer.processPeakPickEvent(this, eventObj, true);
    } else if (eventObj instanceof ZoomEvent) {
      writeStatus("Double-Click highlighted spectrum in menu to zoom out; CTRL+/CTRL- to adjust Y scaling.");
    } else if (eventObj instanceof SubSpecChangeEvent) {
      SubSpecChangeEvent e = (SubSpecChangeEvent) eventObj;
      if (e.isValid())
        setMainTitle(e.toString());
      else
        // pass on to menu
        advanceSpectrumBy(-e.getSubIndex());
    }
  }

  public void setSelectedPanel(JSVPanel jsvp) {
    selectedPanel = jsvp;
  }

  private void setMainTitle(String title) {
    String t = getSelectedPanel().getSpectrum().getTitleLabel();
    desktopPane.getSelectedFrame().setTitle(
        title == null ? t : t + " - " + title);
  }

  public void sendFrameChange(JSVPanel jsvp) {
    JSViewer.sendFrameChange(this, jsvp);
  }

  ////////// MENU ACTIONS ///////////

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

    JInternalFrame[] frames = desktopPane.getAllFrames();
    for (int i = 0; i < frames.length; i++)
      setJSVPanelProperties(getPanel0((JSVFrame) frames[i]),
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
   *        the name of the format to export in
   */
  void exportSpectrum(String command) {
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

    dirLastExported = Exporter.exportSpectra(jsvp, this, fc, type,
        recentFileName, dirLastExported);

  }

  protected void windowClosing_actionPerformed() {
    exitJSpecView(true);
  }

  /**
   * Listener for a JSVFrame
   */
  private class JSVInternalFrameListener extends InternalFrameAdapter {

    JDXSource source;

    /**
     * Initialises a <code>JSVInternalFrameListener</code>
     * 
     * @param file
     *        the name of the selected file
     * @param source
     *        current the JDXSource of the file
     */
    public JSVInternalFrameListener(JDXSource source) {
      this.source = source;
    }

    /**
     * Gets the selected JSVPanel and updates menus and button according to the
     * panel's properties. Also sets the frame title to the current file name.
     * 
     * @param e
     *        the InternalFrameEvent
     */
    @Override
    public void internalFrameActivated(InternalFrameEvent e) {
      JSVFrame frame = (JSVFrame) e.getInternalFrame();
      if (!frame.isVisible())
        return;
      setCurrentSource(source);
      
      // Update the menu items for the display menu
      setTitle("JSpecView - " + source.getFilePath());
      selectFrameNode(frame);
      setMenuEnables(selectFrameNode(frame));
    }

    /**
     * Called when <code>JSVFrame</code> is closing
     * 
     * @param e
     *        the InternalFrameEvent
     */
    @Override
    public void internalFrameClosing(InternalFrameEvent e) {
      final JSVFrame frame = (JSVFrame) e.getInternalFrame();

      doInternalFrameClosing(frame);
    }

    /**
     * Called when <code>JSVFrame</code> has opened
     * 
     * @param e
     *        the InternalFrameEvent
     */
    @Override
    public void internalFrameOpened(InternalFrameEvent e) {

      spectraTree.validate();
      spectraTree.repaint();
    }
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
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {

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
      return new Font("Dialog",
          (node == null || node.specNode == null || node.specNode.frame == null
              || node.specNode.frame.isVisible() ? Font.BOLD : Font.ITALIC), 12);
    }

  }

  private void advanceSpectrumBy(int n) {
    int i = specNodes.size();
    for (; --i >= 0;)
      if (specNodes.get(i).jsvp == getSelectedPanel())
        break;
    setFrameAndTreeNode(i + n);
    getSelectedPanel().doRequestFocusInWindow();
  }

  static JSVPanel getPanel0(JSVFrame frame) {
    return ((JSVPanel) frame.getContentPane().getComponent(0));
  }

  public Map<String, Object> getProperty(String key) {
    if ("".equals(key))
      key = null;
    List<Map<String, Object>> info = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < specNodes.size(); i++) {
      JSVPanel jsvp = specNodes.get(i).jsvp;
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

  public boolean syncToJmol(String msg) {
    Logger.info("JSV>Jmol " + msg);
    if (jmol != null) { // MainFrame --> embedding application
      jmol.syncScript(msg);
      return true;
    }
    if (jmolOrAdvancedApplet != null) // MainFrame --> embedding applet
      return jmolOrAdvancedApplet.syncToJmol(msg);
    return false;
  }

  public void syncScript(String peakScript) {
    JSViewer.syncScript(this, peakScript);
  }

  public void syncLoad(String filePath) {
    closeSource(null);
    openDataOrFile(null, null, null, filePath, -1, -1);
    if (currentSource == null)
      return;
    if (specNodes.get(0).getSpectrum().isAutoOverlayFromJmolClick())
      execOverlay("*");
  }

  ////////////////////////// script commands from JSViewer /////////////////

  public void setFrame(JSVSpecNode specNode) {
    setFrame(specNode, false);
  }
  
  public JDXSource getCurrentSource() {
    return currentSource;
  }

  public Parameters getParameters() {
    return parameters;
  }

  public List<JSVSpecNode> getSpecNodes() {
    return specNodes;
  }

  public void execOverlay(String value) {
    List<JDXSpectrum> speclist = new ArrayList<JDXSpectrum>();
    String strlist = JSVSpecNode.fillSpecList(specNodes, value, speclist,
        getSelectedPanel(), "1.");
    if (speclist.size() > 1)
      openDataOrFile(null, strlist, speclist, strlist, -1, -1);
  }

  /**
   * Allows Integration of an HNMR spectra
   * 
   */
  public void execIntegrate(JDXSpectrum spec) {
    //unnec
  }

  /**
   * Calculates the predicted colour of the Spectrum
   */
  public String setSolutionColor(boolean showMessage) {
    String msg = getSelectedPanel().getPanelData().getSolutionColorHtml();
    JOptionPane.showMessageDialog(this, msg, "Predicted Colour",
        JOptionPane.INFORMATION_MESSAGE);
    return null;
  }

  public void execClose(String value) {
    close(TextFormat.trimQuotes(value));
  }

  public void execHidden(boolean b) {
    isHidden = (jmol != null && b);
    setVisible(!isHidden);
  }

  public String execLoad(String value) {
    List<String> tokens = ScriptToken.getTokens(value);
    String filename = tokens.get(0);
    int pt = 0;
    if (filename.equalsIgnoreCase("APPEND")) {
      filename = tokens.get(++pt);
    } else {
      if (filename.equals("\"\"") && currentSource != null)
        filename = currentSource.getFilePath();
      close("all");
    }
    filename = TextFormat.trimQuotes(filename);
    int firstSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
        : -1);
    int lastSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt))
        : firstSpec);
    openFile(filename, firstSpec, lastSpec);
    if (getSelectedPanel() == null)
      return null;
    if (!getSelectedPanel().getSpectrum().is1D())
      return "Click on the spectrum and use UP or DOWN keys to see subspectra.";
    return null;
  }

  public String execExport(JSVPanel jsvp, String value) {
    return Exporter.exportCmd(jsvp, ScriptToken.getTokens(value),
        svgForInkscape);
  }

  public void execSetIntegrationRatios(String value) {
    // ignored

  }

  public void execTAConvert(int mode) {
    irMode = JDXSpectrum.TA_NO_CONVERT;
  }

  public void execSetInterface(String value) {
    if (value.equalsIgnoreCase("stack"))
      desktopPane.stackFrames();
    else if (value.equalsIgnoreCase("cascade"))
      desktopPane.cascadeFrames();
    else if (value.equalsIgnoreCase("tile"))
      desktopPane.tileFrames();
  }

  public void execScriptComplete(String msg, boolean isOK) {
    repaint();
    if (msg != null) {
      writeStatus(msg);
      if (msg.length() == 0)
        msg = null;
    }
    if (msg == null) {
      commandInput.requestFocus();
    }
  }

  public JSVPanel execSetSpectrum(String value) {
    if (value.indexOf('.') >= 0) {
      JSVSpecNode node = JSVSpecNode.findNodeById(value, specNodes);
      if (node == null)
        return null;
      setFrameAndTreeNode(node);
    } else {
      setSpectrumNumberAndTreeNode(Integer.parseInt(value));
    }
    return getSelectedPanel();
  }

  public void execSetAutoIntegrate(boolean b) {
    autoIntegrate = b;
  }

  public void execTest(String value) {
    syncScript("<PeakData file=\"c:/temp/t.jdx\" index=\"2\" type=\"MS\" id=\"2\" title=\"b-caryopholene (~93)\" peakShape=\"sharp\" model=\"caryoph\"  xMax=\"94\" xMin=\"92\"  yMax=\"100\" yMin=\"0\" />");
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

  public String export(String type, int n) {
    return advancedApplet.export(type, n);
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
    //if (advancedApplet != null)
    //  advancedApplet.runScript(script);
    //else
      runScriptNow(script);
  }

  /**
   * Writes a message to the status bar
   * 
   * @param msg
   *        the message
   */
  public void writeStatus(String msg) {
    if (msg == null)
      msg = "Unexpected Error";
    if (msg.length() == 0)
      msg = "Enter a command:";
    statusLabel.setText(msg);
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
   *        the file
   */
  public void openFile(String fileName, boolean closeFirst) {
    if (closeFirst) { // drag/drop
      JDXSource source = JSVSpecNode.findSourceByNameOrId((new File(fileName))
          .getAbsolutePath(), specNodes);
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
    switch(st) {
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

  public void print() {
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp == null)
      return;
    PrintLayoutDialog.PrintLayout pl = (new PrintLayoutDialog(this)).getPrintLayout();
    if (pl != null) {
      ((AwtPanel) jsvp).printSpectrum(pl);
    }
  }

  public void toggleOverlayKey() {
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp == null)
      return;
    //boolean showLegend = appMenu.toggleOverlayKeyMenuItem();
    JSViewer.setOverlayLegendVisibility(this, jsvp, true);

  }
  
  public void zoomTo(int mode) {
    JSVPanel jsvp = getSelectedPanel();
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
      pd.fullView();
      break;
    default:
      pd.resetView();
      break;
    }
  }

  public void checkCallbacks(String title) {
    setMainTitle(title);
  }
}
