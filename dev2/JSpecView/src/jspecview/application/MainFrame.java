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
import java.awt.CardLayout; //import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jmol.api.JSVInterface;
import org.jmol.api.JmolSyncInterface;

import jspecview.common.AwtPanel;
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
import jspecview.common.PeakInfo;
import jspecview.common.SubSpecChangeEvent;
import jspecview.common.ZoomEvent;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.export.Exporter;
import jspecview.source.FileReader;
import jspecview.source.JDXSource;
import jspecview.util.Escape;
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
    PanelListener, ScriptInterface {

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
  private AwtParameters tempParams = new AwtParameters("temp");

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

  public JSVPanel getSelectedPanel() {
    return selectedPanel;
  }

  private JMenuBar menuBar = new JMenuBar();
  private JMenu fileMenu = new JMenu();
  private JMenuItem openMenuItem = new JMenuItem();
  private JMenuItem openURLMenuItem = new JMenuItem();
  private JMenuItem printMenuItem = new JMenuItem();
  private JMenuItem closeMenuItem = new JMenuItem();
  private JMenuItem closeAllMenuItem = new JMenuItem();
  private JMenu saveAsMenu = new JMenu();
  private JMenu saveAsJDXMenu = new JMenu();
  private JMenu exportAsMenu = new JMenu();
  private JMenuItem exitMenuItem = new JMenuItem();
  //JMenu windowMenu = new JMenu();
  private JMenu helpMenu = new JMenu();
  private JMenu optionsMenu = new JMenu();
  private JMenu displayMenu = new JMenu();
  private JMenu zoomMenu = new JMenu();
  private JCheckBoxMenuItem gridCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem coordsCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem revPlotCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem scaleXCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem scaleYCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JMenuItem nextZoomMenuItem = new JMenuItem();
  private JMenuItem prevZoomMenuItem = new JMenuItem();
  private JMenuItem fullZoomMenuItem = new JMenuItem();
  private JMenuItem clearZoomMenuItem = new JMenuItem();
  private JMenuItem userZoomMenuItem = new JMenuItem();
  private JMenuItem preferencesMenuItem = new JMenuItem();
  private JMenuItem contentsMenuItem = new JMenuItem();
  private JMenuItem aboutMenuItem = new JMenuItem();
  private JMenu openRecentMenu = new JMenu();
  private JCheckBoxMenuItem toolbarCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem sidePanelCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem statusCheckBoxMenuItem = new JCheckBoxMenuItem();
  private BorderLayout mainborderLayout = new BorderLayout();
  private JSplitPane mainSplitPane = new JSplitPane();
  private JSplitPane sideSplitPane = new JSplitPane();

  private JScrollPane scrollPane = new JScrollPane();
  private ScrollableDesktopPane desktopPane = new ScrollableDesktopPane();
  private WindowMenu windowMenu = new WindowMenu(desktopPane);
  private JTree spectraTree;
  private JScrollPane spectraTreePane;
  private JPanel statusPanel = new JPanel();
  private JLabel statusLabel = new JLabel();
  private JTextField commandInput = new JTextField();

  private JSVPanelPopupMenu jsvpPopupMenu = new JSVPanelPopupMenu(this);
  private JCheckBoxMenuItem splitMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem overlayAllMenuItem = new JCheckBoxMenuItem();
  private JMenuItem overlayMenuItem = new JMenuItem();
  private JSVTreeNode rootNode;
  private DefaultTreeModel spectraTreeModel;
  private JMenuItem hideMenuItem = new JMenuItem();
  private JMenuItem hideAllMenuItem = new JMenuItem();
  private JMenu showMenu = new JMenu();
  private JMenuItem showMenuItem = new JMenuItem();
  private JMenuItem sourceMenuItem = new JMenuItem();
  private JMenuItem propertiesMenuItem = new JMenuItem();
  private BorderLayout borderLayout1 = new BorderLayout();

  private JFileChooser fc;
  private JToolBar jsvToolBar = new JToolBar();
  private JButton previousButton = new JButton();
  private JButton nextButton = new JButton();
  private JButton resetButton = new JButton();
  private JButton clearButton = new JButton();
  private JButton openButton = new JButton();
  private JButton propertiesButton = new JButton();
  private JButton errorLogButton = new JButton();
  private JToggleButton gridToggleButton = new JToggleButton();
  private JToggleButton coordsToggleButton = new JToggleButton();
  private JButton printButton = new JButton();
  private JToggleButton revPlotToggleButton = new JToggleButton();
  private JButton aboutButton = new JButton();
  private JButton overlaySplitButton = new JButton();
  private JMenuItem scriptMenuItem = new JMenuItem();
  private JButton overlayKeyButton = new JButton();
  private JMenu processingMenu = new JMenu();
  private JMenuItem errorLogMenuItem = new JMenuItem();
  private JSVInterface jsv;

  /**
   * Constructor
   * 
   * @param jsv
   */
  public MainFrame(JSVInterface jsv) {

    this.jsv = jsv;
    onProgramStart();

    // initialise MainFrame as a target for the drag-and-drop action
    new DropTarget(this, getDropListener());

    getIcons();
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

  private void exitJSpecView(boolean withDialog) {
    jsv.saveProperties(properties);
    if (isEmbedded) {
      setVisible(false);
      return;
    }
    dsp.getDisplaySchemes().remove("Current");
    jsv.exitJSpecView(withDialog && showExitDialog, this);
  }

  private Image icon;
  private ImageIcon frameIcon;
  private ImageIcon openIcon;
  private ImageIcon printIcon;
  private ImageIcon gridIcon;
  private ImageIcon coordinatesIcon;
  private ImageIcon reverseIcon;
  private ImageIcon previousIcon;
  private ImageIcon nextIcon;
  private ImageIcon resetIcon;
  private ImageIcon clearIcon;
  private ImageIcon informationIcon;
  private ImageIcon aboutIcon;
  private ImageIcon overlayIcon;
  private ImageIcon splitIcon;
  private ImageIcon overlayKeyIcon;
  private ImageIcon errorLogIcon;
  private ImageIcon errorLogYellowIcon;
  private ImageIcon errorLogRedIcon;
  private CommandHistory commandHistory;
  private boolean svgForInkscape;
  private JMenuItem overlayKeyMenuItem;

  private void getIcons() {
    Class<? extends MainFrame> cl = getClass();
    URL iconURL = cl.getResource("icons/spec16.gif"); //imageIcon
    icon = Toolkit.getDefaultToolkit().getImage(iconURL);
    frameIcon = new ImageIcon(iconURL);
    openIcon = new ImageIcon(cl.getResource("icons/open24.gif"));
    printIcon = new ImageIcon(cl.getResource("icons/print24.gif"));
    gridIcon = new ImageIcon(cl.getResource("icons/grid24.gif"));
    coordinatesIcon = new ImageIcon(cl.getResource("icons/coords24.gif"));
    reverseIcon = new ImageIcon(cl.getResource("icons/reverse24.gif"));
    previousIcon = new ImageIcon(cl.getResource("icons/previous24.gif"));
    nextIcon = new ImageIcon(cl.getResource("icons/next24.gif"));
    resetIcon = new ImageIcon(cl.getResource("icons/reset24.gif"));
    clearIcon = new ImageIcon(cl.getResource("icons/clear24.gif"));
    informationIcon = new ImageIcon(cl.getResource("icons/information24.gif"));
    aboutIcon = new ImageIcon(cl.getResource("icons/about24.gif"));
    overlayIcon = new ImageIcon(cl.getResource("icons/overlay24.gif"));
    splitIcon = new ImageIcon(cl.getResource("icons/split24.gif"));
    overlayKeyIcon = new ImageIcon(cl.getResource("icons/overlayKey24.gif"));
    errorLogIcon = new ImageIcon(cl.getResource("icons/errorLog24.gif"));
    errorLogRedIcon = new ImageIcon(cl.getResource("icons/errorLogRed24.gif"));
    errorLogYellowIcon = new ImageIcon(cl
        .getResource("icons/errorLogYellow24.gif"));
  }

  /**
   * Shows or hides certain GUI elements
   */
  private void setApplicationElements() {
    // hide side panel if sidePanelOn property is false
    sidePanelCheckBoxMenuItem.setSelected(sidePanelOn);
    toolbarCheckBoxMenuItem.setSelected(toolbarOn);
    statusCheckBoxMenuItem.setSelected(statusbarOn);

    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp != null) {
      PanelData pd = jsvp.getPanelData();
      gridCheckBoxMenuItem.setSelected(pd.isGridOn());
      gridToggleButton.setSelected(pd.isGridOn());
      coordsCheckBoxMenuItem.setSelected(pd.isCoordinatesOn());
      coordsToggleButton.setSelected(pd.isCoordinatesOn());
      revPlotCheckBoxMenuItem.setSelected(pd.isPlotReversed());
      revPlotToggleButton.setSelected(pd.isPlotReversed());
      scaleXCheckBoxMenuItem.setSelected(pd.isXScaleOn());
      scaleYCheckBoxMenuItem.setSelected(pd.isYScaleOn());
    }
  }

  /**
   * Task to do when program starts
   */
  private void onProgramStart() {

    //boolean loadedOk;
    //Set Default Properties

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

    jsv.setProperties(properties);

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

  }

  /**
   * Sets the preferences or properties of the application that is loaded from a
   * properties file.
   */
  private void setApplicationProperties(
                                        boolean shouldApplySpectrumDisplaySettings) {

    String recentFilesString = properties.getProperty("recentFilePaths");
    openRecentMenu.removeAll();
    recentFilePaths.clear();
    if (!recentFilesString.equals("")) {
      StringTokenizer st = new StringTokenizer(recentFilesString, ",");
      JMenuItem menuItem;
      String path;
      while (st.hasMoreTokens()) {
        path = st.nextToken().trim();
        recentFilePaths.add(path);
        menuItem = new JMenuItem(path);
        openRecentMenu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            openRecent_actionPerformed(ae);
          }
        });
      }
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
        setCloseMenuItem(node.specNode.fileName);
      }
    });
    spectraTree.putClientProperty("JTree.lineStyle", "Angled");
    spectraTree.setShowsRootHandles(true);
    spectraTree.setEditable(false);
    spectraTree.setRootVisible(false);
    spectraTree.addMouseListener(new MouseListener() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && selectedPanel != null) {
          selectedPanel.getPanelData().setZoom(0, 0, 0, 0);
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
    spectraTreePane = new JScrollPane(spectraTree);
    if (mainSplitPane != null)
      mainSplitPane.setLeftComponent(spectraTreePane);

  }

  public DropTargetListener dtl;

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
    setIconImage(icon);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setJMenuBar(menuBar);
    setTitle("JSpecView");
    getContentPane().setLayout(mainborderLayout);
    fileMenu.setMnemonic('F');
    fileMenu.setText("File");

    JSVPanelPopupMenu.setMenuItem(openMenuItem, 'O', "Open...", 79,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            open_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(openURLMenuItem, 'U', "Open URL...", 85,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            openURL_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(printMenuItem, 'P', "Print...", 80,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            printMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(closeMenuItem, 'C', "Close", 115,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            closeMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(closeAllMenuItem, 'L', "Close All", 0,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            closeAllMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(exitMenuItem, 'X', "Exit", 115,
        InputEvent.ALT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            exitMenuItem_actionPerformed(e);
          }
        });

    windowMenu.setMnemonic('W');
    windowMenu.setText("Window");

    helpMenu.setMnemonic('H');
    helpMenu.setText("Help");

    optionsMenu.setMnemonic('O');
    optionsMenu.setText("Options");

    displayMenu.setMnemonic('D');
    displayMenu.setText("Display");
    displayMenu.addMenuListener(new MenuListener() {
      public void menuSelected(MenuEvent e) {
        JSVPanel jsvp = getCurrentJSVPanel();
        if (jsvp == null)
          return;
        gridCheckBoxMenuItem.setSelected(jsvp.getPanelData().isGridOn());
        coordsCheckBoxMenuItem.setSelected(jsvp.getPanelData()
            .isCoordinatesOn());
        revPlotCheckBoxMenuItem.setSelected(jsvp.getPanelData()
            .isPlotReversed());
        jsvpPopupMenu.setEnables(selectedPanel);
      }

      public void menuDeselected(MenuEvent e) {
      }

      public void menuCanceled(MenuEvent e) {
      }
    });
    zoomMenu.setMnemonic('Z');
    zoomMenu.setText("Zoom");

    JSVPanelPopupMenu.setMenuItem(gridCheckBoxMenuItem, 'G', "Grid", 71,
        InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            gridCheckBoxMenuItem_itemStateChanged(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(coordsCheckBoxMenuItem, 'C', "Coordinates",
        67, InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            coordsCheckBoxMenuItem_itemStateChanged(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(revPlotCheckBoxMenuItem, 'R', "Reverse Plot",
        82, InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            revPlotCheckBoxMenuItem_itemStateChanged(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(scaleXCheckBoxMenuItem, 'X', "X Scale", 88,
        InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            scaleXCheckBoxMenuItem_itemStateChanged(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(scaleYCheckBoxMenuItem, 'Y', "Y Scale", 89,
        InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            scaleYCheckBoxMenuItem_itemStateChanged(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(nextZoomMenuItem, 'N', "Next View", 78,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            nextMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(prevZoomMenuItem, 'P', "Previous View", 80,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            prevMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(fullZoomMenuItem, 'F', "Full View", 70,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            fullMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(clearZoomMenuItem, 'C', "Clear Views", 67,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            clearMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(userZoomMenuItem, 'Z', "Set Zoom...", 90,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            userMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(scriptMenuItem, 'T', "Script...", 83,
        InputEvent.ALT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            scriptMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(preferencesMenuItem, 'P', "Preferences...",
        0, 0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            preferencesMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(contentsMenuItem, 'C', "Contents...", 112, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            contentsMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(aboutMenuItem, 'A', "About", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            aboutMenuItem_actionPerformed(e);
          }
        });
    openRecentMenu.setActionCommand("OpenRecent");
    openRecentMenu.setMnemonic('R');
    openRecentMenu.setText("Open Recent");

    saveAsMenu.setMnemonic('A');
    saveAsJDXMenu.setMnemonic('J');
    exportAsMenu.setMnemonic('E');

    JSVPanelPopupMenu.setMenuItem(toolbarCheckBoxMenuItem, 'T', "Toolbar", 84,
        InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            toolbarCheckBoxMenuItem_itemStateChanged(e);
          }
        });
    toolbarCheckBoxMenuItem.setSelected(true);

    JSVPanelPopupMenu.setMenuItem(sidePanelCheckBoxMenuItem, 'S', "Side Panel",
        83, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            sidePanelCheckBoxMenuItem_itemStateChanged(e);
          }
        });
    sidePanelCheckBoxMenuItem.setSelected(true);

    JSVPanelPopupMenu.setMenuItem(statusCheckBoxMenuItem, 'B', "Status Bar",
        66, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            statusCheckBoxMenuItem_itemStateChanged(e);
          }
        });
    statusCheckBoxMenuItem.setSelected(true);
    sideSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
    sideSplitPane.setOneTouchExpandable(true);
    statusLabel.setToolTipText("");
    statusLabel.setHorizontalTextPosition(SwingConstants.LEADING);
    statusLabel.setText("  ");
    statusPanel.setBorder(BorderFactory.createEtchedBorder());
    statusPanel.setLayout(borderLayout1);
    mainSplitPane.setOneTouchExpandable(true);
    borderLayout1.setHgap(2);
    borderLayout1.setVgap(2);

    JSVPanelPopupMenu.setMenuItem(splitMenuItem, 'P', "Split", 83,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            splitMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(overlayAllMenuItem, 'Y', "Overlay All", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            overlayMenuItem_actionPerformed(e, true);
          }
        });
    JSVPanelPopupMenu.setMenuItem(overlayMenuItem, 'O', "Overlay...", 79,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            overlayMenuItem_actionPerformed(e, false);
          }
        });
    JSVPanelPopupMenu.setMenuItem(hideMenuItem, 'H', "Hide", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            hideMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(hideAllMenuItem, 'L', "Hide All", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            hideAllMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(showMenuItem, 'S', "Show All", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            showMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(sourceMenuItem, 'S', "Source ...", 83,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            sourceMenuItem_actionPerformed(e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(propertiesMenuItem, 'P', "Properties", 72,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            propertiesMenuItem_actionPerformed(e);
          }
        });
    overlayKeyMenuItem = jsvpPopupMenu.overlayKeyMenuItem;
    JSVPanelPopupMenu.setMenuItem(overlayKeyMenuItem, '\0', "Overlay Key", 0,
        0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            overlayKeyMenuItem_actionPerformed(e);
          }
        });

    setButton(previousButton, "Previous View", previousIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            previousButton_actionPerformed(e);
          }
        });
    setButton(nextButton, "Next View", nextIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        nextButton_actionPerformed(e);
      }
    });
    setButton(resetButton, "Reset", resetIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        resetButton_actionPerformed(e);
      }
    });
    setButton(clearButton, "Clear Views", clearIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearButton_actionPerformed(e);
      }
    });

    setButton(openButton, "Open", openIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        openButton_actionPerformed(e);
      }
    });
    setButton(propertiesButton, "Properties", informationIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            propertiesButton_actionPerformed(e);
          }
        });
    setButton(errorLogButton, "Error Log", errorLogIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        errorLogMenuItem_actionPerformed(e);
      }
    });

    setButton(gridToggleButton, "Toggle Grid", gridIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        gridToggleButton_actionPerformed(e);
      }
    });
    setButton(coordsToggleButton, "Toggle Coordinates", coordinatesIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            coordsToggleButton_actionPerformed(e);
          }
        });
    setButton(printButton, "Print", printIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        printButton_actionPerformed(e);
      }
    });
    setButton(revPlotToggleButton, "Reverse Plot", reverseIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            revPlotToggleButton_actionPerformed(e);
          }
        });
    setButton(aboutButton, "About JSpecView", aboutIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        aboutButton_actionPerformed(e);
      }
    });
    setButton(overlaySplitButton, "Overlay Display", overlayIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            overlaySplitButton_actionPerformed(e);
          }
        });
    setButton(overlayKeyButton, "Display Key for Overlaid Spectra",
        overlayKeyIcon, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            overlayKeyButton_actionPerformed(e);
          }
        });
    overlayKeyButton.setEnabled(false);

    JSVPanelPopupMenu.setMenuItem(errorLogMenuItem, '\0', "Error Log ...", 0,
        0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            errorLogMenuItem_actionPerformed(e);
          }
        });

    processingMenu.setMnemonic('P');
    processingMenu.setText("Processing");
    processingMenu.addMenuListener(new MenuListener() {
      public void menuSelected(MenuEvent e) {
        jsvpPopupMenu.setEnables(selectedPanel);
      }

      public void menuDeselected(MenuEvent e) {
      }

      public void menuCanceled(MenuEvent e) {
      }
    });
    jsvpPopupMenu.setProcessingMenu(processingMenu);

    menuBar.add(fileMenu);
    menuBar.add(displayMenu).setEnabled(false);
    menuBar.add(optionsMenu);
    menuBar.add(windowMenu).setEnabled(false);
    menuBar.add(processingMenu).setEnabled(false);
    menuBar.add(helpMenu);
    fileMenu.add(openMenuItem);
    fileMenu.add(openURLMenuItem);
    fileMenu.add(openRecentMenu);
    fileMenu.addSeparator();
    fileMenu.add(closeMenuItem).setEnabled(false);
    fileMenu.add(closeAllMenuItem).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(scriptMenuItem);
    fileMenu.addSeparator();
    fileMenu.add(saveAsMenu).setEnabled(false);
    fileMenu.add(exportAsMenu).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(printMenuItem).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(sourceMenuItem).setEnabled(false);
    fileMenu.add(errorLogMenuItem).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(exitMenuItem);
    displayMenu.add(gridCheckBoxMenuItem);
    displayMenu.add(coordsCheckBoxMenuItem);
    displayMenu.add(scaleXCheckBoxMenuItem);
    displayMenu.add(scaleYCheckBoxMenuItem);
    displayMenu.add(revPlotCheckBoxMenuItem);
    displayMenu.addSeparator();
    displayMenu.add(zoomMenu);
    displayMenu.addSeparator();
    displayMenu.add(propertiesMenuItem);
    displayMenu.add(overlayKeyMenuItem).setEnabled(false);
    zoomMenu.add(nextZoomMenuItem);
    zoomMenu.add(prevZoomMenuItem);
    zoomMenu.add(fullZoomMenuItem);
    zoomMenu.add(clearZoomMenuItem);
    zoomMenu.add(userZoomMenuItem);
    optionsMenu.add(preferencesMenuItem);
    optionsMenu.addSeparator();
    optionsMenu.add(toolbarCheckBoxMenuItem);
    optionsMenu.add(sidePanelCheckBoxMenuItem);
    optionsMenu.add(statusCheckBoxMenuItem);
    helpMenu.add(aboutMenuItem);
    JSVPanelPopupMenu.setMenus(saveAsMenu, saveAsJDXMenu, exportAsMenu,
        (new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            exportSpectrum(e.getActionCommand());
          }
        }));
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

    getContentPane().add(jsvToolBar, BorderLayout.NORTH);
    jsvToolBar.add(openButton, null);
    jsvToolBar.add(printButton, null);
    jsvToolBar.addSeparator();
    jsvToolBar.add(gridToggleButton, null);
    jsvToolBar.add(coordsToggleButton, null);
    jsvToolBar.add(revPlotToggleButton, null);
    jsvToolBar.addSeparator();
    jsvToolBar.add(previousButton, null);
    jsvToolBar.add(nextButton, null);
    jsvToolBar.add(resetButton, null);
    jsvToolBar.add(clearButton, null);
    jsvToolBar.addSeparator();
    jsvToolBar.add(overlaySplitButton, null);
    jsvToolBar.add(overlayKeyButton, null);
    jsvToolBar.addSeparator();
    jsvToolBar.add(propertiesButton, null);
    jsvToolBar.add(errorLogButton, null);
    errorLogButton.setVisible(true);
    jsvToolBar.addSeparator();
    jsvToolBar.add(aboutButton, null);
    getContentPane().add(mainSplitPane, BorderLayout.CENTER);
    mainSplitPane.setLeftComponent(spectraTreePane);
    //sideSplitPane.setDividerLocation(350);
    mainSplitPane.setDividerLocation(200);
    scrollPane.getViewport().add(desktopPane);
    mainSplitPane.setRightComponent(scrollPane);
    //sideSplitPane.setTopComponent(spectraTreePane);
    //sideSplitPane.setBottomComponent(errorTree);
    windowMenu.add(splitMenuItem);
    windowMenu.add(overlayAllMenuItem);
    windowMenu.add(overlayMenuItem);
    windowMenu.addSeparator();
    windowMenu.add(hideMenuItem);
    windowMenu.add(hideAllMenuItem);
    //    windowMenu.add(showMenu);
    windowMenu.add(showMenuItem);
    windowMenu.addSeparator();

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

  private static void setButton(AbstractButton button, String tip,
                                ImageIcon icon, ActionListener actionListener) {
    button.setBorder(null);
    button.setToolTipText(tip);
    button.setIcon(icon);
    button.addActionListener(actionListener);
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

  /**
   * Opens and displays a file, either local or remote
   * 
   * @param fileOrURL
   */
  public void openFile(String fileOrURL, int firstSpec, int lastSpec) {
    openDataOrFile(null, null, null, fileOrURL, firstSpec, lastSpec);
  }

  private int nOverlay;

  public void loadInline(String data) {
    openDataOrFile(data, null, null, null, -1, -1);
  }

  private int openDataOrFile(String data, String name, List<JDXSpectrum> specs,
                             String url, int firstSpec, int lastSpec) {
    // name could be "NONE" here from overlay
    writeStatus("");
    String filePath = null;
    String fileName = null;
    boolean isOverlay = false;
    File file = null;
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
    setCloseMenuItem(fileName);
    setTitle("JSpecView - " + filePath);

    // add calls to enable Menus that were greyed out until a file is opened.

    // if current spectrum is not a Peak Table then enable Menu to re-export

    setSourceEnabled(true);

    JDXSpectrum spec = currentSource.getJDXSpectrum(0);
    if (spec == null) {
      return FILE_OPEN_NO_DATA;
    }

    setMenuEnables(spec);

    specs = currentSource.getSpectra();
    boolean overlay = isOverlay || autoOverlay
        && currentSource.isCompoundSource;
    overlay &= !JDXSpectrum.process(specs, irMode, !isOverlay && autoIntegrate,
        parameters.integralMinY, parameters.integralOffset,
        parameters.integralFactor);
    if (overlay) {
      overlay(currentSource, (isOverlay ? url : null));
    } else {
      splitSpectra(currentSource);
    }
    if (!isOverlay)
      updateRecentMenus(filePath);
    return FILE_OPEN_OK;
  }

  private void setSourceEnabled(boolean b) {
    closeAllMenuItem.setEnabled(b);
    displayMenu.setEnabled(b);
    windowMenu.setEnabled(b);
    processingMenu.setEnabled(b);
    printMenuItem.setEnabled(b);
    sourceMenuItem.setEnabled(b);
    errorLogMenuItem.setEnabled(b);
    exportAsMenu.setEnabled(b);
    saveAsMenu.setEnabled(b);

  }

  private void updateRecentMenus(String filePath) {

    // ADD TO RECENT FILE PATHS
    if (recentFilePaths.size() >= MAX_RECENT)
      recentFilePaths.remove(MAX_RECENT - 1);
    if (!recentFilePaths.contains(filePath))
      recentFilePaths.add(0, filePath);
    String filePaths = "";
    JMenuItem menuItem;
    openRecentMenu.removeAll();
    int index;
    for (index = 0; index < recentFilePaths.size() - 1; index++) {
      String path = recentFilePaths.get(index);
      filePaths += path + ", ";
      menuItem = new JMenuItem(path);
      openRecentMenu.add(menuItem);
      menuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          openRecent_actionPerformed(ae);
        }
      });
    }
    String path = recentFilePaths.get(index);
    filePaths += path;
    menuItem = new JMenuItem(path);
    openRecentMenu.add(menuItem);
    menuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        openRecent_actionPerformed(ae);
      }
    });
    properties.setProperty("recentFilePaths", filePaths);
  }

  private void setCloseMenuItem(String fileName) {
    closeMenuItem.setEnabled(fileName != null);
    closeMenuItem.setText(fileName == null ? "Close" : "Close " + fileName);
  }

  private void setCurrentSource(JDXSource source) {
    currentSource = source;
    boolean isError = (source != null && source.getErrorLog().length() > 0);
    setError(isError, (isError && source.getErrorLog().indexOf("Warning") >= 0));
  }

  private void setError(boolean isError, boolean isWarningOnly) {
    errorLogButton.setIcon(isWarningOnly ? errorLogYellowIcon
        : isError ? errorLogRedIcon : errorLogIcon);
    errorLogButton.setEnabled(isError);
    errorLogMenuItem.setEnabled(isError);
  }

  void setMenuEnables(JDXSpectrum spec) {

    if (spec == null) {
      setCloseMenuItem(null);
      setSourceEnabled(false);
      return;
    }

    exportAsMenu.setEnabled(true);
    saveAsMenu.setEnabled(true);
    saveAsJDXMenu.setEnabled(spec.canSaveAsJDX());
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
    overlayAllMenuItem.setSelected(true);
    splitMenuItem.setSelected(false);
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

    if (autoShowLegend
        && selectedPanel.getPanelData().getNumberOfGraphSets() == 1)
      JSVSpecNode.findNode(selectedPanel, specNodes).setLegend(
          new OverlayLegendDialog(this, jsvp));

    overlaySplitButton.setIcon(splitIcon);
    overlaySplitButton.setToolTipText("Split Display");
  }

  /**
   * Displays the spectrum of the <code>JDXSource</code> specified by source in
   * separate windows
   * 
   * @param source
   *        the <code>JDXSource</code>
   */
  private void splitSpectra(JDXSource source) {

    overlayAllMenuItem.setSelected(false);
    splitMenuItem.setSelected(true);

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

    overlaySplitButton.setIcon(overlayIcon);
    overlaySplitButton.setToolTipText("Overlay Display");
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
      if (selectedPanel != null)
        selectedPanel.dispose();
      if (currentSource != null)
        currentSource.dispose();
    }

    selectedPanel = null;
    currentSource = null;
    System.gc();

    setError(false, false);
    if (source != null) {
      List<JDXSpectrum> spectra = source.getSpectra();
      for (int i = 0; i < spectra.size(); i++) {
        String title = spectra.get(i).getTitleLabel();
        for (int j = 0; j < showMenu.getMenuComponentCount(); j++) {
          JMenuItem mi = (JMenuItem) showMenu.getMenuComponent(j);
          if (mi.getText().endsWith(title)) {
            showMenu.remove(mi);
          }
        }
      }
      saveAsJDXMenu.setEnabled(true);
      saveAsMenu.setEnabled(true);
    }
    setCloseMenuItem(null);
    setTitle("JSpecView");
    if (source == null) {
      setMenuEnables(null);
    } else {
      setSpectrumNumberAndTreeNode(specNodes.size());
    }
    //recentJmolName = null;
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

  public void selectFrameNode(JSVFrame frame) {
    // Find Node in SpectraTree and select it
    JSVSpecNode node = JSVSpecNode.findNode(frame, specNodes);
    if (node == null)
      return;
    spectraTree.setSelectionPath(new TreePath(((JSVTreeNode) node.treeNode)
        .getPath()));
    setOverlayKeys(node);
  }

  private void setOverlayKeys(JSVSpecNode node) {
    boolean showLegends = overlayKeyMenuItem.isSelected();
    for (int i = specNodes.size(); --i >= 0;)
      setLegendVisibility(specNodes.get(i), this, specNodes.get(i) == node
          && showLegends);
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

  private JSVPanel getCurrentJSVPanel() {
    JSVFrame frame = (JSVFrame) desktopPane.getSelectedFrame();
    return (frame == null ? selectedPanel : getPanel0(frame));
  }

  private JSVFrame getCurrentFrame() {
    JSVFrame frame = (JSVFrame) desktopPane.getSelectedFrame();
    return (frame != null ? frame : selectedPanel == null ? null
        : (JSVFrame) (JSVSpecNode.findNode(selectedPanel, specNodes).frame));
  }

  /**
   * ScriptInterface requires this. In the applet, this would be queued
   */
  public void runScript(String script) {
    if (jsv != null)
      jsv.runScript(script);
    else
      runScriptNow(script);
  }

  public void runScriptNow(String script) {
    JSViewer.runScriptNow(this, selectedPanel, script);
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

  public JSVPanel setSpectrum(int i) {
    if (specNodes != null && i >= 0 && i < specNodes.size())
      setFrame(specNodes.get(i), false);
    return selectedPanel;
  }

  private void setFrame(JSVSpecNode specNode, boolean fromTree) {
    System.out.println("setFrame" + specNode);
    JSVFrame frame = (JSVFrame) specNode.frame;
    selectedPanel = specNode.jsvp;
    frame.setVisible(true);
    frame.moveToFront();
    try {
      frame.setSelected(true);
    } catch (PropertyVetoException pve) {
    }
    if (fromTree && frame.isEnabled()) {
      selectedPanel.setEnabled(true);
      sendFrameChange(specNode.jsvp);
      if (desktopPane.getStyle() == ScrollableDesktopPane.STYLE_STACK)
        desktopPane.setAllEnabled(false);
      selectedPanel.setEnabled(true);
    }
    setMenuEnables(selectedPanel.getSpectrum());
    if (selectedPanel.getSpectrum().getIntegrationGraph() != null)
      writeStatus("Use CTRL-LEFT-DRAG to measure an integration value.");
    else
      writeStatus("");
  }

  public void panelEvent(Object eventObj) {
    System.out.println("panelEvent " + eventObj);
    if (eventObj instanceof PeakPickEvent) {
      PeakPickEvent e = ((PeakPickEvent) eventObj);
      PeakInfo pi = e.getPeakInfo();
      selectedPanel = (JSVPanel) e.getSource();
      selectedPanel.getPanelData().processPeakSelect(pi);
      sendScript(e.toString());
      setMainTitle(pi.getTitle());
      selectedPanel.getPanelData().selectSpectrum(pi.getFilePath(),
          pi.getType(), pi.getModel());
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

  private void setMainTitle(String title) {
    String t = selectedPanel.getSpectrum().getTitleLabel();
    desktopPane.getSelectedFrame().setTitle(
        title == null ? t : t + " - " + title);
  }

  public void sendFrameChange(JSVPanel jsvp) {
    if (jsvp.getPanelData().getNumberOfGraphSets() == 1)
      return;
    PeakInfo pi = jsvp.getSpectrum().getSelectedPeak();
    if (pi == null)
      pi = jsvp.getSpectrum().getModelPeakInfo();
    System.out.println("sendframechange2 " + pi);
    selectedPanel.getPanelData().processPeakSelect(
        pi == null ? PeakInfo.nullPeakInfo : pi);
    sendScript(pi == null ? null : pi.toString());
  }

  ////////// MENU ACTIONS ///////////

  /**
   * Shows the legend or key for the overlaid spectra
   * 
   * @param e
   *        the ActionEvent
   */
  protected void overlayKeyButton_actionPerformed(ActionEvent e) {
    overlayKeyMenuItem_actionPerformed(e);
  }

  /**
   * Prints the current Spectrum display
   * 
   * @param e
   *        the ActionEvent
   */
  protected void printMenuItem_actionPerformed(ActionEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;

    PrintLayoutDialog ppd = new PrintLayoutDialog(this);
    PrintLayoutDialog.PrintLayout pl = ppd.getPrintLayout();

    if (pl != null) {
      ((AwtPanel) jsvp).printSpectrum(pl);
    }
  }

  /**
   * Shows the source file contents
   * 
   * @param e
   *        the ActionEvent
   */
  protected void sourceMenuItem_actionPerformed(ActionEvent e) {
    if (currentSource == null) {
      if (specNodes.size() > 0) {
        JOptionPane.showMessageDialog(this, "Please Select a Spectrum",
            "Select Spectrum", JOptionPane.ERROR_MESSAGE);
      }
      return;
    }
    try {
      new TextDialog(this, currentSource.getFilePath(), true);
    } catch (IOException ex) {
      new TextDialog(this, "File Not Found", "File Not Found", true);
    }

  }

  /**
   * Exits the application
   * 
   * @param e
   *        the ActionEvent
   */
  protected void exitMenuItem_actionPerformed(ActionEvent e) {
    exitJSpecView(false);
  }

  /**
   * Shows the preferences dialog
   * 
   * @param e
   *        the ActionEvent
   */
  void preferencesMenuItem_actionPerformed(ActionEvent e) {
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
   * Shows the Help | Contents dialog
   * 
   * @param e
   *        the ActionEvent
   */
  void contentsMenuItem_actionPerformed(ActionEvent e) {
    showNotImplementedOptionPane();
  }

  /**
   * Toggles the Coordinates
   * 
   * @param e
   *        the ItemEvent
   */
  void coordsCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;

    if (e.getStateChange() == ItemEvent.SELECTED) {
      Parameters.setBoolean(jsvp, tempParams, ScriptToken.COORDINATESON, true);
      coordsToggleButton.setSelected(true);
    } else {
      Parameters.setBoolean(jsvp, tempParams, ScriptToken.COORDINATESON, false);
      coordsToggleButton.setSelected(false);
    }
    repaint();
  }

  /**
   * Reverses the plot
   * 
   * @param e
   *        the ItemEvent
   */
  void revPlotCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    jsvp.getPanelData()
        .setReversePlot(e.getStateChange() == ItemEvent.SELECTED);
    repaint();
  }

  /**
   * Shows the next zoomed view
   * 
   * @param e
   *        the ActionEvent
   */
  void nextMenuItem_actionPerformed(ActionEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    jsvp.getPanelData().nextView();
  }

  /**
   * Shows the previous zoomed view
   * 
   * @param e
   *        the ActionEvent
   */
  void prevMenuItem_actionPerformed(ActionEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    jsvp.getPanelData().previousView();
  }

  /**
   * Shows the full spectrum
   * 
   * @param e
   *        the ActionEvent
   */
  void fullMenuItem_actionPerformed(ActionEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    jsvp.getPanelData().clearViews();
  }

  /**
   * Clears all zoom views
   * 
   * @param e
   *        the ActionEvent
   */
  void clearMenuItem_actionPerformed(ActionEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    jsvp.getPanelData().clearViews();
  }

  /**
   * Export spectrum in a given format
   * 
   * @param command
   *        the name of the format to export in
   */
  void exportSpectrum(String command) {
    final String type = command;
    JSVPanel jsvp = getCurrentJSVPanel();
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

  /**
   * Shows the Help | About Dialog
   * 
   * @param e
   *        the ActionEvent
   */
  void aboutMenuItem_actionPerformed(ActionEvent e) {
    //JOptionPane.showMessageDialog(MainFrame.this, "<html><img src=MainFrame.class.getClassLoader().getResource(\"icons/spec16.gif\")> JSpecView version</img> " + JSVApplet.APPLET_VERSION + aboutJSpec, "About JSpecView", JOptionPane.PLAIN_MESSAGE);
    new AboutDialog(this);
  }

  /**
   * Hides the selected JInternalFrane
   * 
   * @param e
   *        the ActionEvent
   */
  void hideMenuItem_actionPerformed(ActionEvent e) {
    JSVFrame frame = getCurrentFrame();
    try {
      if (frame != null) {
        frame.setVisible(false);
        frame.setSelected(false);
        //        spectraTree.validate();
        spectraTree.repaint();
      }
    } catch (PropertyVetoException pve) {
    }
    //doInternalFrameClosing(frame);
  }

  /**
   * Hides all JInternalFranes
   * 
   * @param e
   *        the ActionEvent
   */
  void hideAllMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame[] frames = desktopPane.getAllFrames();
    try {
      for (int i = 0; i < frames.length; i++) {
        if (frames[i].isVisible()) {
          frames[i].setVisible(false);
          frames[i].setSelected(false);
          //         doInternalFrameClosing(frames[i]);
        }
      }
    } catch (PropertyVetoException pve) {
    }
  }

  /**
   * Shows all JSVFrames
   * 
   * @param e
   *        the ActionEvent
   */
  protected void showMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame[] frames = desktopPane.getAllFrames();
    try {
      for (int i = 0; i < frames.length; i++) {
        frames[i].setVisible(true);
      }
      frames[0].setSelected(true);
    } catch (PropertyVetoException pve) {
    }

    showMenu.removeAll();
  }

  /**
   * Shows the log of error in the source file
   * 
   * @param e
   *        the ActionEvent
   */
  protected void errorLogMenuItem_actionPerformed(ActionEvent e) {
    if (currentSource == null) {
      JOptionPane.showMessageDialog(null, "Please Select a Spectrum",
          "Select Spectrum", JOptionPane.WARNING_MESSAGE);
      return;
    }
    String errorLog = currentSource.getErrorLog();
    if (errorLog != null)
      new TextDialog(this, currentSource.getFilePath(), errorLog, true);
  }

  /**
   * Shows the File Open Dialog
   * 
   * @param e
   *        the ActionEvent
   */
  protected void openButton_actionPerformed(ActionEvent e) {
    showFileOpenDialog();
  }

  /**
   * Shows the print dialog
   * 
   * @param e
   *        the ActionEvent
   */
  protected void printButton_actionPerformed(ActionEvent e) {
    printMenuItem_actionPerformed(e);
  }

  /**
   * Toggles the grid
   * 
   * @param e
   *        the ActionEvent
   */
  protected void gridToggleButton_actionPerformed(ActionEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    Parameters.setBoolean(jsvp, tempParams, ScriptToken.GRIDON,
        ((JToggleButton) e.getSource()).isSelected());
    repaint();
  }

  /**
   * Toggles the coordinates
   * 
   * @param e
   *        the the ActionEvent
   */
  protected void coordsToggleButton_actionPerformed(ActionEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    setCoordinatesOn(jsvp, ((JToggleButton) e.getSource()).isSelected());
    repaint();
  }

  private void setCoordinatesOn(JSVPanel jsvp, boolean selected) {
    parameters.setBoolean(ScriptToken.COORDINATESON, selected);
    jsvp.getPanelData().setBoolean(parameters, ScriptToken.COORDINATESON);
  }

  /**
   * Reverses the plot
   * 
   * @param e
   *        the ActionEvent
   */
  protected void revPlotToggleButton_actionPerformed(ActionEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    jsvp.getPanelData().setReversePlot(
        ((JToggleButton) e.getSource()).isSelected());
    repaint();
  }

  /**
   * Shows the previous zoomed view
   * 
   * @param e
   *        the ActionEvent
   */
  protected void previousButton_actionPerformed(ActionEvent e) {
    prevMenuItem_actionPerformed(e);
  }

  /**
   * Shows the next zoomed view
   * 
   * @param e
   *        the ActionEvent
   */
  protected void nextButton_actionPerformed(ActionEvent e) {
    nextMenuItem_actionPerformed(e);
  }

  /**
   * Shows the full view of the spectrum display
   * 
   * @param e
   *        the ActionEvent
   */
  protected void resetButton_actionPerformed(ActionEvent e) {
    fullMenuItem_actionPerformed(e);
  }

  /**
   * Clears the zoomed views
   * 
   * @param e
   *        the ActionEvent
   */
  protected void clearButton_actionPerformed(ActionEvent e) {
    clearMenuItem_actionPerformed(e);
  }

  /**
   * Shows the properties or header information for the current spectrum
   * 
   * @param e
   *        the ActionEvent
   */
  protected void propertiesButton_actionPerformed(ActionEvent e) {
    propertiesMenuItem_actionPerformed(e);
  }

  /**
   * Shows the About dialog
   * 
   * @param e
   *        the ActionEvent
   */
  protected void aboutButton_actionPerformed(ActionEvent e) {
    aboutMenuItem_actionPerformed(e);
  }

  /**
   * Split or overlays the spectra
   * 
   * @param e
   *        the ActionEvent
   */
  protected void overlaySplitButton_actionPerformed(ActionEvent e) {
    if (((JButton) e.getSource()).getIcon() == overlayIcon) {
      overlayMenuItem_actionPerformed(e, true);
    } else {
      splitMenuItem_actionPerformed(e);
    }
  }

  /**
   * Shows or hides the toolbar
   * 
   * @param e
   *        the ItemEvent
   */
  protected void toolbarCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      getContentPane().add(jsvToolBar, BorderLayout.NORTH);
    } else {
      getContentPane().remove(jsvToolBar);
    }
    validate();
  }

  /**
   * Shows or hides the sidePanel
   * 
   * @param e
   *        the ItemEvent
   */
  protected void sidePanelCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      mainSplitPane.setDividerLocation(200);
    } else {
      mainSplitPane.setDividerLocation(0);
    }
  }

  /**
   * Shows or hides the status bar
   * 
   * @param e
   *        the ItemEvent
   */
  protected void statusCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      getContentPane().add(statusPanel, BorderLayout.SOUTH);
    } else {
      getContentPane().remove(statusPanel);
    }
    validate();
  }

  /**
   * Shows the legend or key for the overlaid spectra
   * 
   * @param e
   *        the ActionEvent
   */
  protected void overlayKeyMenuItem_actionPerformed(ActionEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    overlayKeyMenuItem.setSelected(!overlayKeyMenuItem.isSelected());
    setOverlayKeys(JSVSpecNode.findNode(jsvp, specNodes));
  }

  /**
   * Shows dialog to open a file
   * 
   * @param e
   *        the ActionEvent
   */
  protected void open_actionPerformed(ActionEvent e) {
    showFileOpenDialog();
  }

  private String recentOpenURL = "http://";

  protected void openURL_actionPerformed(ActionEvent e) {
    String msg = (recentURL == null ? recentOpenURL : recentURL);
    String url = (String) JOptionPane.showInputDialog(null,
        "Enter the URL of a JCAMP-DX File", "Open URL",
        JOptionPane.PLAIN_MESSAGE, null, null, msg);
    if (url == null)
      return;
    recentOpenURL = url;
    openDataOrFile(null, null, null, url, -1, -1);
  };

  protected void windowClosing_actionPerformed() {
    exitJSpecView(true);
  }

  protected void userMenuItem_actionPerformed(ActionEvent e) {
    jsvpPopupMenu.userZoom();
  }

  protected void scriptMenuItem_actionPerformed(ActionEvent e) {
    jsvpPopupMenu.script();
  }

  /**
   * Open a file listed in the open recent menu
   * 
   * @param e
   *        the ActionEvent
   */
  protected void openRecent_actionPerformed(ActionEvent e) {
    openFile(((JMenuItem) e.getSource()).getText(), true);
  }

  /**
   * Shows the current Source file as overlaid
   * 
   * @param e
   *        the ActionEvent
   */
  protected void overlayMenuItem_actionPerformed(ActionEvent e, boolean isAll) {
    if (isAll) {
      overlayAllMenuItem.setSelected(false);
      if (currentSource == null || selectedPanel == null) {
        return;
      }
      if (!currentSource.isCompoundSource) {
        writeStatus("Unable to Overlay, Incompatible source type");
        return;
      }
      if (currentSource.isOverlay()) {
        closeSource(currentSource);
        return;
      }
      try {
        JDXSource source = currentSource;
        closeSource(currentSource);
        overlay(source, null);
      } catch (Exception ex) {
        splitSpectra(currentSource);
      }
    } else {
      jsvpPopupMenu.overlay(-1);
    }
  }

  /**
   * Closes the current JDXSource
   * 
   * @param e
   *        the ActionEvent
   */
  protected void closeMenuItem_actionPerformed(ActionEvent e) {
    closeSource(currentSource);
    setCurrentSource(null);
    if (specNodes.size() == 0)
      setMenuEnables(null);
  }

  /**
   * Close all <code>JDXSource<code>s
   * 
   * @param e
   *        the ActionEvent
   */
  protected void closeAllMenuItem_actionPerformed(ActionEvent e) {
    closeSource(null);
  }

  /**
   * Displays the spectrum of the current <code>JDXSource</code> in separate
   * windows
   * 
   * @param e
   *        the ActionEvent
   */
  protected void splitMenuItem_actionPerformed(ActionEvent e) {
    JDXSource source = currentSource;
    JSVPanel jsvp = getCurrentJSVPanel();
    if (!source.isCompoundSource || jsvp == null
        || jsvp.getPanelData().getNumberOfGraphSets() == 1) {
      splitMenuItem.setSelected(false);
      return;
      // STATUS --> Can't Split
    }
    closeSource(source);
    if (!source.isOverlay())
      splitSpectra(source);
  }

  /**
   * Toggles the grid
   * 
   * @param e
   *        the ItemEvent
   */
  void gridCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    Parameters.setBoolean(jsvp, tempParams, ScriptToken.GRIDON, e
        .getStateChange() == ItemEvent.SELECTED);
    repaint();
  }

  /**
   * Toggles the X Scale
   * 
   * @param e
   *        the ItemEvent
   */
  void scaleXCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    boolean b = (e.getStateChange() == ItemEvent.SELECTED);
    Parameters.setBoolean(jsvp, tempParams, ScriptToken.XSCALEON, b);
    Parameters.setBoolean(jsvp, tempParams, ScriptToken.XUNITSON, b);
    repaint();
  }

  /**
   * Toggles the X Scale
   * 
   * @param e
   *        the ItemEvent
   */
  void scaleYCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    boolean b = (e.getStateChange() == ItemEvent.SELECTED);
    Parameters.setBoolean(jsvp, tempParams, ScriptToken.YSCALEON, b);
    Parameters.setBoolean(jsvp, tempParams, ScriptToken.YUNITSON, b);
    repaint();
  }

  /**
   * Shows the properties or header of a Spectrum
   * 
   * @param e
   *        the ActionEvent
   */
  protected void propertiesMenuItem_actionPerformed(ActionEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    jsvpPopupMenu.setEnables(jsvp);
    jsvpPopupMenu.properties_actionPerformed(e);

  }

  private void setLegendVisibility(JSVSpecNode node, Frame frame,
                                   boolean visible) {
    JSVDialog legend = (JSVDialog) node.legend;
    if (legend == null && visible) {
      node
          .setLegend(node.jsvp.getPanelData().getNumberOfSpectraInCurrentSet() > 1
              && node.jsvp.getPanelData().getNumberOfGraphSets() == 1 ? new OverlayLegendDialog(
              frame, node.jsvp)
              : null);
    }
    if (legend != null)
      legend.setVisible(visible);
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
      JSVPanel jsvp = getPanel0(frame);
      JDXSpectrum spec = jsvp.getSpectrum();
      PanelData pd = jsvp.getPanelData();
      gridCheckBoxMenuItem.setSelected(pd.isGridOn());
      gridToggleButton.setSelected(pd.isGridOn());
      coordsCheckBoxMenuItem.setSelected(pd.isCoordinatesOn());
      coordsToggleButton.setSelected(pd.isCoordinatesOn());
      revPlotCheckBoxMenuItem.setSelected(pd.isPlotReversed());
      revPlotToggleButton.setSelected(pd.isPlotReversed());
      scaleXCheckBoxMenuItem.setSelected(pd.isXScaleOn());
      scaleYCheckBoxMenuItem.setSelected(pd.isYScaleOn());

      if (pd.getNumberOfGraphSets() > 1) {
        overlaySplitButton.setIcon(splitIcon);
        overlaySplitButton.setToolTipText("Split Display");
        overlayKeyButton.setEnabled(true);
        overlayKeyMenuItem.setEnabled(true);
      } else {
        overlaySplitButton.setIcon(overlayIcon);
        overlaySplitButton.setToolTipText("Overlay Display");
        overlayKeyButton.setEnabled(false);
        overlayKeyMenuItem.setEnabled(false);
      }

      setMenuEnables(spec);
      setCloseMenuItem(FileManager.getName(source.getFilePath()));
      setTitle("JSpecView - " + source.getFilePath());
      selectFrameNode(frame);
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
      if (specNodes.get(i).jsvp == selectedPanel)
        break;
    setFrameAndTreeNode(i + n);
    selectedPanel.doRequestFocusInWindow();
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
      info.add(jsvp.getPanelData().getInfo(jsvp == selectedPanel, key));
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

  /**
   * outgoing method for return messages to Jmol
   * 
   * @param peak
   */
  public void sendScript(String peak) {
    String msg = Escape.jmolSelect(peak);
    syncToJmol(msg);
  }

  public void syncToJmol(String msg) {
    Logger.info("JSpecView MainFrame syncToJmol: " + msg);
    if (jmol != null) // MainFrame --> embedding application
      jmol.syncScript(msg);
    if (jsv != null) // MainFrame --> embedding applet
      jsv.syncToJmol(msg);
  }

  /**
   * incoming script processing of <PeakAssignment file="" type="xxx"...> record
   * from Jmol
   */
  public void syncScript(String peakScript) {
    if (peakScript.indexOf("<PeakData") < 0) {
      runScriptNow(peakScript);
      return;
    }
    String file = Parser.getQuotedAttribute(peakScript, "file");
    String index = Parser.getQuotedAttribute(peakScript, "index");
    if (file == null || index == null)
      return;
    Logger.info("JSpecView MainFrame.syncScript: " + peakScript);
    if (!selectMostRecentPanelByFileName(file)) {
      closeSource(null);
      openDataOrFile(null, null, null, file, -1, -1);
      System.out.println("doing overlay now");
      execOverlay("*");
    }
    PeakInfo pi = JSViewer.selectPanelByPeak(this, peakScript, specNodes,
        selectedPanel);
    selectedPanel.getPanelData().processPeakSelect(pi);
    JSViewer.selectSpectrumInPanel(this, selectedPanel, peakScript);
    selectedPanel.repaint();
  }

  private boolean selectMostRecentPanelByFileName(String fileName) {
    for (int i = specNodes.size(); --i >= 0;)
      if (specNodes.get(i).jsvp.getPanelData().hasFileLoaded(fileName)) {
        setFrame(specNodes.get(i), false);
        return true;
      }
    System.out.println("mainframe did not find " + fileName);
    return false;
  }

  ////////////////////////// script commands from JSViewer /////////////////

  public void execOverlay(String value) {
    List<JDXSpectrum> speclist = new ArrayList<JDXSpectrum>();
    String strlist = JSVSpecNode.fillSpecList(specNodes, value, speclist,
        selectedPanel, "1.");
    if (speclist.size() > 1)
      openDataOrFile(null, strlist, speclist, strlist, -1, -1);
  }

  /**
   * Allows Integration of an HNMR spectra
   * 
   */
  public void execIntegrate(String value) {
    JSVSpecNode node = JSVSpecNode.findNode(selectedPanel, specNodes);
    JSVPanel jsvpNew = (JSVPanel) PanelData.checkIntegral(selectedPanel,
        parameters, value);
    if (jsvpNew == selectedPanel)
      return;
    ((JSVFrame) node.frame).remove((AwtPanel) selectedPanel);
    ((JSVFrame) node.frame).add((AwtPanel) jsvpNew);
    setJSVPanelProperties(jsvpNew, true);
    node.jsvp = selectedPanel = jsvpNew;
    validate();
  }

  /**
   * Calculates the predicted colour of the Spectrum
   */
  public String setSolutionColor(boolean showMessage) {
    String msg = selectedPanel.getPanelData().getSolutionColorHtml();
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
    if (selectedPanel == null)
      return null;
    if (!selectedPanel.getSpectrum().is1D())
      return "Click on the spectrum and use UP or DOWN keys to see subspectra.";
    return null;
  }

  public String execExport(JSVPanel jsvp, String value) {
    return Exporter.exportCmd(jsvp, ScriptToken.getTokens(value),
        svgForInkscape);
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

  public void execSetCallback(ScriptToken st, String value) {
    // ignored
  }

  public void execSetIntegrationRatios(String value) {
    // ignored

  }

  /**
   * Allows Transmittance to Absorbance conversion or vice versa depending on
   * the value of comm.
   * 
   * @param frame
   *        the selected JSVFrame
   * @param comm
   *        the conversion command
   */
  public void execTAConvert(int comm) {
    irMode = JDXSpectrum.TA_NO_CONVERT;
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    JSVSpecNode node = JSVSpecNode.findNode(jsvp, specNodes);
    node.jsvp = selectedPanel = (JSVPanel) PanelData.taConvert(jsvp, comm);
    setJSVPanelProperties(node.jsvp, true);
    // Get from properties variable
    Container contentPane = ((JSVFrame) node.frame).getContentPane();
    contentPane.remove(0);
    contentPane.invalidate();
    if (!(contentPane.getLayout() instanceof CardLayout))
      contentPane.setLayout(new CardLayout());
    contentPane.add((Component) node.jsvp, "new");
    validate();
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
    return selectedPanel;
  }

  public void execSetAutoIntegrate(boolean b) {
    autoIntegrate = b;
  }

  public void execTest(String value) {
    syncScript("<PeakData file=\"c:/temp/t.jdx\" index=\"2\" type=\"MS\" id=\"2\" title=\"b-caryopholene (~93)\" peakShape=\"sharp\" model=\"caryoph\"  xMax=\"94\" xMin=\"92\"  yMax=\"100\" yMin=\"0\" />");
  }


}
