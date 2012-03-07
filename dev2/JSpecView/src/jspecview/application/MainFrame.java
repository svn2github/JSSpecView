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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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
import javax.swing.UIManager;
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

import org.jmol.api.JmolSyncInterface;

import jspecview.common.AppUtils;
import jspecview.common.CommandHistory;
import jspecview.common.DisplayScheme;
import jspecview.common.Graph;
import jspecview.common.JSVPanel;
import jspecview.common.JSVPanelPopupMenu;
import jspecview.common.JSpecViewFileFilter;
import jspecview.common.OverlayLegendDialog;
import jspecview.common.Parameters;
import jspecview.common.PeakPickedEvent;
import jspecview.common.PeakPickedListener;
import jspecview.common.PrintLayoutDialog;
import jspecview.common.ScriptInterface;
import jspecview.common.ScriptToken;
import jspecview.common.Coordinate;
import jspecview.common.JDXSpectrum;
import jspecview.common.PeakInfo;
import jspecview.common.ZoomListener;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.source.JDXFileReader;
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
public class MainFrame extends JFrame implements DropTargetListener,
    JmolSyncInterface, PeakPickedListener, ScriptInterface, ZoomListener {

  //  ------------------------ Program Properties -------------------------

  /**
   * 
   */
  private final static long serialVersionUID = 1L;
  private final static int FILE_OPEN_OK = 0;
  private final static int FILE_OPEN_ALREADY = -1;
  private final static int FILE_OPEN_URLERROR = -2;
  private final static int FILE_OPEN_ERROR = -3;
  private final static int FILE_OPEN_NO_DATA = -4;
  private final static int MAX_RECENT = 10;

  private String propertiesFileName = "jspecview.properties";
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
  private String recentJmolName;
  private String recentURL;

  private int irMode = JDXSpectrum.TA_NO_CONVERT;
  private boolean autoIntegrate;

  private Parameters parameters = new Parameters("application");

  //  ----------------------- Application Attributes ---------------------

  private JmolSyncInterface jmol;
  private List<JSVTreeNode> specNodes = new ArrayList<JSVTreeNode>();
  private List<String> recentFilePaths = new ArrayList<String>(MAX_RECENT);
  private JDXSource currentSelectedSource;
  private Properties properties;
  private DisplaySchemesProcessor dsp;
  private String tempDS, sltnclr;

  //   ----------------------------------------------------------------------

  private JSVPanel selectedJSVPanel;
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
  //private JTree errorTree = new JTree(new DefaultMutableTreeNode("Errors"));
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
  private JMenuItem overlayKeyMenuItem = new JMenuItem();
  private JButton overlayKeyButton = new JButton();
  //private OverlayLegendDialog legend;
  private JMenu processingMenu = new JMenu();
  private JMenuItem errorLogMenuItem = new JMenuItem();

  {
    // Does certain tasks once regardless of how many 
    // instances of the program are running  
    // [are you sure?? not static; runs every instance? -- BH]
    onProgramStart();
  }

  /**
   * Constructor
   */
  public MainFrame() {
    // initialise MainFrame as a target for the drag-and-drop action
    new DropTarget(this, this);

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
  }

  private void exitJSpecView(boolean withDialog) {
    try {
      onProgramExit();
    } catch (Exception e) {
    }
    if (jmol != null) {
      setVisible(false);
      return;
    }
    if (withDialog
        && showExitDialog
        && JOptionPane.showConfirmDialog(MainFrame.this, "Exit JSpecView?",
            "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
      return;
    System.exit(0);
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
  private ImageIcon errorLogRedIcon;
  private CommandHistory commandHistory;

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
      gridCheckBoxMenuItem.setSelected(jsvp.isGridOn());
      gridToggleButton.setSelected(jsvp.isGridOn());
      coordsCheckBoxMenuItem.setSelected(jsvp.isCoordinatesOn());
      coordsToggleButton.setSelected(jsvp.isCoordinatesOn());
      revPlotCheckBoxMenuItem.setSelected(jsvp.isPlotReversed());
      revPlotToggleButton.setSelected(jsvp.isPlotReversed());
      scaleXCheckBoxMenuItem.setSelected(jsvp.isXScaleOn());
      scaleYCheckBoxMenuItem.setSelected(jsvp.isYScaleOn());
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

    try {
      FileInputStream fileIn = new FileInputStream(propertiesFileName);
      properties.load(fileIn);
      //bgc = Visible.Colour();
    } catch (Exception e) {
    }

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

    JSpecViewFileFilter filter = new JSpecViewFileFilter();
    filter = new JSpecViewFileFilter();
    filter.addExtension("jdx");
    filter.addExtension("dx");
    filter.setDescription("JCAMP-DX Files");
    fc.setFileFilter(filter);

    filter = new JSpecViewFileFilter();
    filter.addExtension("xml");
    filter.addExtension("aml");
    filter.addExtension("cml");
    filter.setDescription("CML/XML Files");
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

    showExitDialog = Boolean.parseBoolean(
        properties.getProperty("confirmBeforeExit"));

    autoOverlay = Boolean.parseBoolean(
        properties.getProperty("automaticallyOverlay"));
    autoShowLegend = Boolean.parseBoolean(
        properties.getProperty("automaticallyShowLegend"));

    useDirLastOpened = Boolean.parseBoolean(
        properties.getProperty("useDirectoryLastOpenedFile"));
    useDirLastExported = Boolean.parseBoolean(
        properties.getProperty("useDirectoryLastExportedFile"));
    dirLastOpened = properties.getProperty("directoryLastOpenedFile");
    dirLastExported = properties.getProperty("directoryLastExportedFile");

    sidePanelOn = Boolean.parseBoolean(properties.getProperty("showSidePanel"))
        ;
    toolbarOn = Boolean.parseBoolean(properties.getProperty("showToolBar"))
        ;
    statusbarOn = Boolean.parseBoolean(properties.getProperty("showStatusBar"))
        ;

    // Initialise DisplayProperties
    defaultDisplaySchemeName = properties
        .getProperty("defaultDisplaySchemeName");

    if (shouldApplySpectrumDisplaySettings) {
      parameters.gridOn = Boolean.parseBoolean(properties.getProperty("showGrid"))
          ;
      parameters.coordinatesOn = Boolean.parseBoolean(
          properties.getProperty("showCoordinates"));
      parameters.xScaleOn = Boolean.parseBoolean(
          properties.getProperty("showXScale"));
      parameters.yScaleOn = Boolean.parseBoolean(
          properties.getProperty("showYScale"));
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
      autoIntegrate = Boolean.parseBoolean(
          properties.getProperty("automaticallyIntegrate"));
      parameters.integralMinY = Double.parseDouble(properties
          .getProperty("integralMinY"));
      parameters.integralFactor = Double.parseDouble(properties
          .getProperty("integralFactor"));
      parameters.integralOffset = Double.parseDouble(properties
          .getProperty("integralOffset"));
      parameters.set(null, 
          ScriptToken.INTEGRALPLOTCOLOR, 
          properties.getProperty("integralPlotColor"));
    } catch (Exception e) {
      // bad property value
    }

    //svgForInscape = Boolean.parseBoolean(properties.getProperty("svgForInscape"));

  }

  /**
   * Returns a <code>Color</code> instance from a parameter
   * 
   * @param key
   *        the parameter name
   * @param def
   *        the default value
   * @return a <code>Color</code> instance from a parameter
   */
  /*  private Color getColorProperty(String key, Color def) {
      String param = properties.getProperty(key);
      Color color = JSpecViewUtils.getColorFromString(param);
      return (color == null ? def : color);
    }
  */
  /**
   * Tasks to do when program exits
   * 
   * @throws Exception
   */
  void onProgramExit() throws Exception {
    // SET RECENT PATHS PROPERTY!!!
    // Write out current properties
    FileOutputStream fileOut = new FileOutputStream(propertiesFileName);
    properties.store(fileOut, "JSpecView Application Properties");
    dsp.getDisplaySchemes().remove("Current");
  }

  /**
   * Creates tree representation of files that are opened
   */
  private void initSpectraTree() {
    currentSelectedSource = null;
    rootNode = new JSVTreeNode("Spectra", null, null, null,null);
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
        if (node.isLeaf())
          setFrame(node, true);
        currentSelectedSource = node.source;
        setCloseMenuItem(node.fileName);
      }
    });
    spectraTree.putClientProperty("JTree.lineStyle", "Angled");
    spectraTree.setShowsRootHandles(true);
    spectraTreePane = new JScrollPane(spectraTree);
    spectraTree.setEditable(false);
    spectraTree.setRootVisible(false);
    spectraTree.addMouseListener(new MouseListener() {

      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && selectedJSVPanel != null) {
          selectedJSVPanel.setZoom(0,0,0,0);
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
  }

  /**
   * The main method
   * 
   * @param args
   *        program parameters, takes the name of the file to open
   */
  public static void main(String args[]) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
    }

    MainFrame frame = new MainFrame();
    frame.setSize(800, 500);

    if (args.length > 0) {
      // check for command-line arguments
      if (args.length == 2 && args[0].equalsIgnoreCase("-script"))
        frame.runScript(args[1]);
      else
        for (int i = 0; i < args.length; i++) {
          System.out.println("JSpecView is attempting to open " + args[i]);
          String filePath = args[i];
          File file = new File(filePath);
          if (file.exists()) {
            frame.openFile(file);
          } else {
            frame.writeStatus("File: " + filePath + " does not exist");
          }
        }
    }
    frame.setVisible(true);
    if (args.length == 0)
      frame.showFileOpenDialog();
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
        displayMenu_menuSelected(e);
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
        80, InputEvent.SHIFT_MASK, new ActionListener() {
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
    JSVPanelPopupMenu.setMenuItem(overlayAllMenuItem, 'Y', "Overlay All", 0,
        0, new ActionListener() {
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
    JSVPanel.setMenus(saveAsMenu, saveAsJDXMenu, exportAsMenu,
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
        if (c == '\t' || isExact) {
          tip = list.get(0).name() + " " + list.get(0).getTip();
          if (c == '\t')
            commandInput.setText(list.get(0).name() + " ");
          break;
        }
        // fall through
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
  private void showFileOpenDialog() {
    JSpecViewFileFilter filter = new JSpecViewFileFilter();
    filter.addExtension("jdx");
    filter.addExtension("dx");
    filter.setDescription("JCAMP-DX Files");
    fc.setFileFilter(filter);
    int returnVal = fc.showOpenDialog(this);

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      properties.setProperty("directoryLastOpenedFile", file.getParent());
      openFile(file);
    }
  }

  /**
   * Opens and displays a file
   * 
   * @param file
   *        the file
   */
  public void openFile(File file) {
    openFile(file, null, null, -1, -1);
  }

  /**
   * Opens and displays a file, either local or remote
   * 
   * @param fileOrURL
   */
  public void openFile(String fileOrURL, int firstSpec, int lastSpec) {
    if (FileManager.isURL(fileOrURL))
      openFile(null, fileOrURL, null, firstSpec, lastSpec);
    else
      openFile(new File(fileOrURL), null, null, firstSpec, lastSpec);
  }

  private int nOverlay;
  
  private int openFile(File file, String url, List<JDXSpectrum> specs, int firstSpec, int lastSpec) {
    writeStatus("");
    String filePath = null;
    String fileName = null;
    boolean isOverlay = false;
    if (specs != null) {
      isOverlay = true;
      fileName = filePath = "Overlay" + (++nOverlay);
    } else if (url != null) {
      try {
        URL u = new URL(url);
        fileName = FileManager.getName(url);
        filePath = u.toString();
        recentJmolName = filePath;
        recentURL = filePath;
      } catch (MalformedURLException e) {
        writeStatus(e.getMessage());
        return FILE_OPEN_URLERROR;
      }
    } else {
      fileName = recentFileName = file.getName();
      filePath = file.getAbsolutePath();
      recentJmolName = (url == null ? filePath.replace('\\', '/') : url);
      recentURL = null;
    }
    if (isOpen(filePath)) {
      writeStatus(filePath + " is already open");
      return FILE_OPEN_ALREADY;
    }
    try {
      setCurrentSource(isOverlay ? JDXSource.createOverlay(url, specs) 
          : JDXFileReader.createJDXSource(null, filePath, null, false, firstSpec, lastSpec));
    } catch (Exception e) {
      e.printStackTrace();
      writeStatus(e.getMessage());
      return FILE_OPEN_ERROR;
    }
    currentSelectedSource.setFilePath(filePath);
    setCloseMenuItem(fileName);
    setTitle("JSpecView - " + filePath);

    // add calls to enable Menus that were greyed out until a file is opened.

    // if current spectrum is not a Peak Table then enable Menu to re-export

    setSourceEnabled(true);

    JDXSpectrum spec = currentSelectedSource.getJDXSpectrum(0);
    if (spec == null) {
      return FILE_OPEN_NO_DATA;
    }

    setMenuEnables(spec);

    specs = currentSelectedSource.getSpectra();
    boolean overlay = isOverlay || autoOverlay && currentSelectedSource.isCompoundSource;
    overlay &= !JDXSpectrum.process(specs, irMode, !isOverlay && autoIntegrate, 
        parameters.integralMinY, parameters.integralOffset, parameters.integralFactor);
    if (overlay) {
      try {
        overlaySpectra(currentSelectedSource, (isOverlay ? url : null));
      } catch (ScalesIncompatibleException ex) {
        splitSpectra(currentSelectedSource);
      }
    } else {
      splitSpectra(currentSelectedSource);
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

  private boolean isOpen(String filePath) {
    for (int i = specNodes.size(); --i >= 0;)
      if (filePath.equals(specNodes.get(i).source.getFilePath()))
        return true;
    return false;
  }

  private void setCurrentSource(JDXSource source) {
    currentSelectedSource = source;
    boolean isError = (source != null && source.getErrorLog().length() > 0);
    setError(isError);
  }

  private void setError(boolean isError) {
    errorLogButton.setIcon(isError ? errorLogRedIcon : errorLogIcon);
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

    //    jsvp.setZoomEnabled(true);
    // update availability of Exporting JCAMP-DX file so that
    // if a Peak Table is the current spectrum, disable the menu.
    // need to check for XYPOINTS as well since not continuous either.... RL
    boolean continuous = spec.isContinuous();
    boolean isAbsTrans = (spec.isAbsorbance() || spec.isTransmittance());
    saveAsJDXMenu.setEnabled(spec.getDataClass().equals("XYDATA"));
    jsvpPopupMenu.integrateCheckBoxMenuItem.setEnabled(spec.canIntegrate());
    jsvpPopupMenu.integrateCheckBoxMenuItem.setSelected(spec.canIntegrate() && spec.getIntegrationGraph() != null);
    //  Can only convert from T <-> A  if Absorbance or Transmittance and continuous
    jsvpPopupMenu.transAbsMenuItem.setEnabled(continuous && isAbsTrans);
    Coordinate xyCoords[] = spec.getXYCoords();
    String Xunits = spec.getXUnits().toLowerCase();
    jsvpPopupMenu.solColMenuItem.setEnabled(isAbsTrans
        && (Xunits.equals("nanometers") || Xunits.equals("nm"))
        && xyCoords[0].getXVal() < 401
        && xyCoords[(xyCoords.length - 1)].getXVal() > 699);
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

    DisplayScheme ds = dsp.getDisplaySchemes().get(defaultDisplaySchemeName);
    parameters.setFor(jsvp, (ds == null ? dsp.getDefaultScheme() : ds),
        includeMeasures);

    jsvp.setXAxisDisplayedIncreasing((jsvp.getSpectrum())
        .shouldDisplayXAxisIncreasing());
    jsvp.setSource(currentSelectedSource);
    jsvp.setPopup(jsvpPopupMenu);

    jsvp.repaint();
  }

  private void showUnableToOverlayMessage() {
    writeStatus("Unable to Overlay, Scales are Incompatible");
    JOptionPane.showMessageDialog(this,
        "Unable to Overlay, Scales are Incompatible", "Overlay Error",
        JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Overlays the spectra of the specified <code>JDXSource</code>
   * 
   * @param source
   *        the <code>JDXSource</code>
   * @throws ScalesIncompatibleException
   */
  private void overlaySpectra(JDXSource source, String name)
      throws ScalesIncompatibleException {
    overlayAllMenuItem.setSelected(true);
    splitMenuItem.setSelected(false);
    List<JDXSpectrum> specs = source.getSpectra();
    JSVPanel jsvp;
    jsvp = new JSVPanel(specs);
    jsvp.setTitle(source.getTitle());

    setJSVPanelProperties(jsvp, true);

    JInternalFrame frame = new JInternalFrame((name == null ? source.getTitle() : name), true, true,
        true, true);
    frame.setFrameIcon(frameIcon);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.addInternalFrameListener(new JSVInternalFrameListener(source));
    frame.setMinimumSize(new Dimension(365, 200));
    frame.setPreferredSize(new Dimension(365, 200));
    frame.getContentPane().add(jsvp);
    desktopPane.add(frame);
    frame.setSize(550, 350);
    jsvpPopupMenu.transAbsMenuItem.setEnabled(false);
    jsvpPopupMenu.solColMenuItem.setEnabled(false);
    try {
      frame.setMaximum(true);
    } catch (PropertyVetoException pve) {
    }
    frame.show();
    JInternalFrame[] frames = new JInternalFrame[] { frame };
    createTree(source, frames);
    validate();
    repaint();

    //OverlayLegendDialog legend;
    if (autoShowLegend) {
      new OverlayLegendDialog(this, jsvp);
    }

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
    //JSVPanel[] panels = new JSVPanel[specs.size()];
    JInternalFrame[] frames = new JInternalFrame[specs.size()];
    for (int i = 0; i < specs.size(); i++) {
      JDXSpectrum spec = specs.get(i);
      JSVPanel jsvp = (spec.getIntegrationGraph() == null ? new JSVPanel(spec)
          : JSVPanel.getIntegralPanel(spec, null));
      jsvp.setIndex(i);
      jsvp.addPeakPickedListener(this);
      jsvp.addZoomListener(this);
      setJSVPanelProperties(jsvp, true);
      JInternalFrame frame = new JInternalFrame(spec.getTitleLabel(), true,
          true, true, true);
      frame.setFrameIcon(frameIcon);
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      frame.setMinimumSize(new Dimension(365, 200));
      frame.setPreferredSize(new Dimension(365, 200));
      frame.getContentPane().add(jsvp);
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
   * Allows Integration of an HNMR spectra
   * 
   */
  protected void integrate(String value) {
    JSVTreeNode node = findNode(selectedJSVPanel);
    JSVPanel jsvpNew = AppUtils.checkIntegral(selectedJSVPanel, node.frame,
        parameters, value);
    //if (integrationRatios != null)   applet only?
      //newJsvp.setIntegrationRatios(integrationRatios);
    if (jsvpNew == selectedJSVPanel)
      return;
    setJSVPanelProperties(jsvpNew, true);
    node.jsvp = selectedJSVPanel = jsvpNew;
    validate();
  }

  /**
   * Calculates the predicted colour of the Spectrum
   */
  private void setSolutionColor(boolean showMessage) {
    sltnclr = selectedJSVPanel.getSolutionColor();
    if (showMessage)
      JSVPanel.showSolutionColor((Component) this, sltnclr);
  }

  /**
   * Allows Transmittance to Absorbance conversion or vice versa depending on
   * the value of comm.
   * 
   * @param frame
   *        the selected JInternalFrame
   * @param comm
   *        the conversion command
   */
  private void taConvert(JInternalFrame frame, int comm) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    JSVTreeNode node = findNode(jsvp);
    node.jsvp = selectedJSVPanel = JSVPanel.taConvert(jsvp, comm);
    setJSVPanelProperties(node.jsvp, true);
    // Get from properties variable
    Container contentPane = (frame = node.frame).getContentPane();
    contentPane.remove(0);
    contentPane.invalidate();
    if (!(contentPane.getLayout() instanceof CardLayout))
      contentPane.setLayout(new CardLayout());
    contentPane.add(node.jsvp, "new");
    validate();
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
      if (fileName == null || node.source.getFilePath().equals(fileName)) {
        for (Enumeration<JSVTreeNode> e = node.children(); e.hasMoreElements();) {
          JSVTreeNode childNode = e.nextElement();
          toDelete.add(childNode);
          childNode.frame.dispose();
          specNodes.remove(childNode);
        }
        toDelete.add(node);
        if (fileName != null)
          break;
      }
    }

    for (int i = 0; i < toDelete.size(); i++)
      spectraTreeModel.removeNodeFromParent(toDelete.get(i));
    selectedJSVPanel = null;
    setError(false);
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
    if (source == null)
      setMenuEnables(null);
    System.gc();
  }

  private int fileCount = 0;
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
   *        an array of JInternalFrames
   */
  public void createTree(JDXSource source, JInternalFrame[] frames) {
    String fileName = FileManager.getName(source.getFilePath());
    JSVTreeNode fileNode = new JSVTreeNode(fileName, source, null,
        null, null);
    spectraTreeModel.insertNodeInto(fileNode, rootNode, rootNode
        .getChildCount());
    spectraTree.scrollPathToVisible(new TreePath(fileNode.getPath()));

    fileCount++;
    for (int i = 0; i < frames.length; i++) {
      JSVPanel jsvp = JSVPanel.getPanel0(frames[i]);
      JSVTreeNode specNode = new JSVTreeNode(fileName, source, frames[i], jsvp, fileCount + "." + (i + 1));
      specNodes.add(specNode);
      spectraTreeModel.insertNodeInto(specNode, fileNode, fileNode
          .getChildCount());
      spectraTree.scrollPathToVisible(new TreePath(specNode.getPath()));
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

  public void selectFrameNode(JInternalFrame frame) {
    // Find Node in SpectraTree and select it
    DefaultMutableTreeNode node = findNode(frame);
    if (node != null)
      spectraTree.setSelectionPath(new TreePath(node.getPath()));
  }

  /**
   * Returns the tree node that is associated with an internal frame
   * 
   * @param frame
   *        the JInternalFrame
   * @return the tree node that is associated with an internal frame
   */
  private JSVTreeNode findNode(JInternalFrame frame) {
    for (int i = specNodes.size(); --i >= 0;)
      if (specNodes.get(i).frame == frame)
        return specNodes.get(i);
    return null;
  }

  /**
   * Returns the tree node that is associated with a panel
   * 
   * @param frame
   *        the JInternalFrame
   * @return the tree node that is associated with a panel
   */
  private JSVTreeNode findNode(JSVPanel jsvp) {
    for (int i = specNodes.size(); --i >= 0;)
      if (specNodes.get(i).jsvp == jsvp)
        return specNodes.get(i);
    return null;
  }

  private JDXSpectrum findSpectrumById(String id) {
    for (int i = specNodes.size(); --i >= 0;)
      if (id.equals(specNodes.get(i).id))
        return specNodes.get(i).jsvp.getSpectrum();
     return null;
  }

  private JSVTreeNode findNodeById(String id) {
    for (int i = specNodes.size(); --i >= 0;)
      if (id.equals(specNodes.get(i).id))
        return specNodes.get(i);
     return null;
  }

  private JDXSource findSourceByNameOrId(String id) {
    for (int i = specNodes.size(); --i >= 0;) {
      JSVTreeNode node = specNodes.get(i);
      if (id.equals(node.id)
          || id.equalsIgnoreCase(node.source.getFilePath()))
        return node.source;
    }
    // only if that doesn't work -- check file name for exact case
    for (int i = specNodes.size(); --i >= 0;) {
      JSVTreeNode node = specNodes.get(i);
      if (id.equals(node.fileName))
        return node.source;
    }
    return null;
  }

  
  /**
   * Does the necessary actions and cleaning up when JInternalFrame closes
   * 
   * @param frame
   *        the JInternalFrame
   */
  private void doInternalFrameClosing(final JInternalFrame frame) {

    closeSource(currentSelectedSource);
    setCurrentSource(null);
    if (specNodes.size() == 0)
      setMenuEnables(null);
  }

  private JSVPanel getCurrentJSVPanel() {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    return (frame == null ? selectedJSVPanel : JSVPanel.getPanel0(frame));
  }

  private JInternalFrame getCurrentFrame() {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    return (frame != null ? frame : selectedJSVPanel == null ? null
        : findNode(selectedJSVPanel).frame);
  }

  //
  //   Abstract methods that are used to perform drag and drop operations
  //

  // Called when the user is dragging and enters this drop target.
  public void dragEnter(DropTargetDragEvent dtde) {
    // accept all drags
    dtde.acceptDrag(dtde.getSourceActions());
    // visually indicate that drop target is under drag
    //showUnderDrag(true);
  }

  // Called when the user is dragging and moves over this drop target.
  public void dragOver(DropTargetDragEvent dtde) {

  }

  // Called when the user is dragging and leaves this drop target.
  public void dragExit(DropTargetEvent dtde) {

  }

  // Called when the user changes the drag action between copy or move.
  public void dropActionChanged(DropTargetDragEvent dtde) {

  }

  // Called when the user finishes or cancels the drag operation.
  @SuppressWarnings("unchecked")
  public void drop(DropTargetDropEvent dtde) {
    try {
      Transferable t = dtde.getTransferable();

      if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        List list = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
        dtde.getDropTargetContext().dropComplete(true);
        File[] files = (File[]) list.toArray();
        for (int i = 0; i < list.size(); i++)
          openFile(files[i]);
      } else {
        dtde.rejectDrop();
      }
    }

    catch (IOException e) {
      dtde.rejectDrop();
    }

    catch (UnsupportedFlavorException e) {
      dtde.rejectDrop();
    }
  }

  /**
   * called by Jmol's StatusListener to register itself, indicating to JSpecView
   * that it needs to synchronize with it
   */
  public void register(String appletID, JmolSyncInterface jmolStatusListener) {
    jmol = jmolStatusListener;
  }

  /**
   * incoming script processing of <PeakAssignment file="" type="xxx"...> record
   * from Jmol
   */
  public void syncScript(String script) {
    if (script.indexOf("<PeakData") < 0) {
      runScript(script);
      return;
    }
    String file = Parser.getQuotedAttribute(script, "file");
    String index = Parser.getQuotedAttribute(script, "index");
    if (file == null || index == null)
      return;
    System.out.println("JSpecView MainFrame.syncScript: " + script);
    if (!file.equals(recentJmolName))
      openFile(new File(file));
    if (!selectPanelByPeak(index))
      script = null;
    selectedJSVPanel.processPeakSelect(script);
    sendFrameChange(selectedJSVPanel);
  }

  public void runScript(String params) {
    if (params == null)
      params = "";
    params = params.trim();
    String msg = null;
    System.out.println("CHECKSCRIPT " + params);
    StringTokenizer allParamTokens = new StringTokenizer(params, ";");
    if (Logger.debugging) {
      System.out.println("Running in DEBUG mode");
    }
    JSVPanel jsvp = selectedJSVPanel;
    List<String> tokens;
    int pt;
    while (allParamTokens.hasMoreTokens()) {
      String token = allParamTokens.nextToken();
      // now split the key/value pair
      StringTokenizer eachParam = new StringTokenizer(token);
      String key = ScriptToken.getKey(eachParam);
      ScriptToken st = ScriptToken.getScriptToken(key);
      String value = ScriptToken.getValue(st, eachParam, token);
      System.out.println("KEY-> " + key + " VALUE-> " + value + " : " + st);
      try {
        switch (st) {
        case UNKNOWN:
          System.out.println("Unrecognized parameter: " + key);
          break;
        default:
          parameters.set(jsvp, st, value);
          break;
        case YSCALE:
          if (jsvp == null)
            continue;
          tokens = ScriptToken.getTokens(value);
          pt = 0;
          boolean isAll = false;
          if (tokens.size() > 1 && tokens.get(0).equalsIgnoreCase("ALL")) {
            isAll = true;
            pt++;
          }
          double y1 = Double.parseDouble(tokens.get(pt++));
          double y2 = Double.parseDouble(tokens.get(pt));
          setYScale(y1, y2, isAll);
          break;          
        case LOAD:
          tokens = ScriptToken.getTokens(value);
          pt = 0;
          if (tokens.size() > 1 && tokens.get(0).equalsIgnoreCase("APPEND")) {
            pt++;
          } else {
            close("all");
          }
          String filename = TextFormat.trimQuotes(tokens.get(pt));
          int firstSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt)): -1); 
          int lastSpec = (pt + 1 < tokens.size() ? Integer.valueOf(tokens.get(++pt)) : firstSpec); 
          openFile(filename, firstSpec, lastSpec);
          jsvp = selectedJSVPanel;
          if (jsvp == null)
            return;
          if (!jsvp.getSpectrum().is1D())
            msg = "Click on the spectrum and use UP or DOWN keys to see subspectra.";
          break;
        case CLOSE:
          close(TextFormat.trimQuotes(value));
          break;
        case SPECTRUM:
        case SPECTRUMNUMBER:
          if (value.indexOf('.') >= 0) {
            JSVTreeNode node = findNodeById(value);
            if (node == null)
              return; // fail!
            setFrame(node);
          } else {
            setFrame(Integer.parseInt(value) - 1);
          }
          jsvp = selectedJSVPanel;
          break;
        case AUTOINTEGRATE:
          autoIntegrate = Parameters.parseBoolean(value);
          break;
        case IRMODE:
          irMode = JDXSpectrum.TA_NO_CONVERT;
          if (jsvp != null)
            taConvert(
                null,
                value.toUpperCase().startsWith("T") ? (irMode = JDXSpectrum.TO_TRANS)
                    : value.toUpperCase().startsWith("A") ? (irMode = JDXSpectrum.TO_ABS)
                        : JDXSpectrum.IMPLIED);
          break;
        case INTEGRATE:
          if (jsvp != null)
            integrate(value);
          break;
        case EXPORT:
          if (jsvp != null)
            msg = jsvp.export(ScriptToken.getTokens(value));
          break;
        case LABEL:
          if (jsvp != null)
            jsvp.addAnnotation(ScriptToken.getTokens(value));
          break;
        case OVERLAY:
          overlay(ScriptToken.getTokens(value));
          break;
        case GETSOLUTIONCOLOR:
          if (jsvp != null)
            setSolutionColor(true);
          break;
        case INTERFACE:
          if (value.equalsIgnoreCase("stack"))
            desktopPane.stackFrames();
          else if (value.equalsIgnoreCase("cascade"))
            desktopPane.cascadeFrames();
          else if (value.equalsIgnoreCase("tile"))
            desktopPane.tileFrames();
          break;
        case OBSCURE:
          //obscure = Boolean.parseBoolean(value);
          //JSpecViewUtils.setObscure(obscure);
          break;
        case INTEGRATIONRATIOS:
          // parse the string with a method in JSpecViewUtils
          // integrationRatios = JSpecViewUtils
          //  .getIntegrationRatiosFromString(value);
          break;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (msg == null)
      commandInput.requestFocusInWindow();
    else
      writeStatus(msg);
    repaint();
  }

  private void setYScale(double y1, double y2, boolean isAll) {
    if (isAll) {
      JDXSpectrum spec = selectedJSVPanel.getSpectrum();
      for (int i = specNodes.size(); --i >= 0;) {
        JSVTreeNode node = specNodes.get(i);
        if (node.source != currentSelectedSource)
          continue;
        if (JDXSpectrum.areScalesCompatible(spec, node.jsvp.getSpectrum()))
          node.jsvp.setZoom(Double.NaN, y1, Double.NaN, y2);
      }
    } else {
      selectedJSVPanel.setZoom(Double.NaN, y1, Double.NaN, y2);
    }
    
  }

  private void close(String value) {
    value = value.replace('\\', '/');
    if (value.equalsIgnoreCase("all")) {
      closeSource(null);
      return;
    }
    JDXSource source = (value.length() == 0 ? currentSelectedSource : findSourceByNameOrId(value));
    if (source == null)
      return;
    closeSource(source);
  }

  private void overlay(List<String> list) {
    List<JDXSpectrum> slist = new ArrayList<JDXSpectrum>();
    StringBuffer sb = new StringBuffer();
    String id0 = (selectedJSVPanel == null ? "1." : findNode(selectedJSVPanel).id);
    id0 = id0.substring(0, id0.indexOf(".") + 1);
    for (int i = 0; i < list.size(); i++) {
      String id = list.get(i);
      if (!id.contains("."))
        id = id0 + id;
      JDXSpectrum spec = findSpectrumById(id);
      if (spec == null)
        continue;
        slist.add(spec);
        sb.append(",").append(id);
    }
    if (slist.size() > 1 && JDXSpectrum.areScalesCompatible(slist))
      openFile(null, sb.toString().substring(1), slist, -1, -1);
  }

  private void setFrame(int i) {
    if (specNodes == null || i < 0 || i >= specNodes.size())
      return;
    setFrame(specNodes.get(i));
  }

  private void setFrame(JSVTreeNode node) {
    selectFrameNode(node.frame);
    setFrame(node, false);
  }

  private boolean selectPanelByPeak(String index) {
    for (int i = specNodes.size(); --i >= 0;) {
      JSVTreeNode node = specNodes.get(i);
      if ((node.jsvp.getSpectrum()).hasPeakIndex(index)) {
        setFrame(node, false);
        return true;
      }
    }
    return false;
  }

  private void setFrame(JSVTreeNode specNode, boolean fromTree) {
    JInternalFrame frame = specNode.frame;
    selectedJSVPanel = specNode.jsvp;
    frame.setVisible(true);
    frame.moveToFront();
    try {
      frame.setSelected(true);
    } catch (PropertyVetoException pve) {
    }
    if (fromTree && frame.isEnabled()) {
      selectedJSVPanel.setEnabled(true);
      sendFrameChange(specNode.jsvp);
      if (desktopPane.getStyle() == ScrollableDesktopPane.STYLE_STACK)
        desktopPane.setAllEnabled(false);
      selectedJSVPanel.setEnabled(true);
    }
    setMenuEnables(selectedJSVPanel.getSpectrum());
    if (selectedJSVPanel.getSpectrum().getIntegrationGraph() != null)
      writeStatus("Use CTRL-LEFT-DRAG to measure an integration value.");
    else 
      writeStatus("");
  }

  /**
   * This is the method Debbie needs to call from within JSpecView when a peak
   * is clicked.
   * 
   * @param peak
   */
  public void sendScript(String peak) {
    selectedJSVPanel.processPeakSelect(peak);
    String s = Escape.jmolSelect(peak, recentJmolName);
    System.out.println("JSpecView MainFrame sendScript: " + s);
    if (jmol == null)
      return;
    // outgoing <PeakData file="xxx" type="xxx"...> record to Jmol    
    jmol.syncScript(s);
  }

  public void peakPicked(PeakPickedEvent eventObj) {
    selectedJSVPanel = (JSVPanel) eventObj.getSource();
    String peaks = eventObj.getPeakInfo();
    sendScript(peaks);
  }

  private void sendFrameChange(JSVPanel jsvp) {
    PeakInfo pi = (jsvp.getSpectrum()).getSelectedPeak();
    sendScript(pi == null ? null : pi.getStringInfo());
  }

  ////////// MENU ACTIONS ///////////

  /**
   * Shows the legend or key for the overlayed spectra
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
      jsvp.printSpectrum(pl);
    }
  }

  /**
   * Shows the source file contents
   * 
   * @param e
   *        the ActionEvent
   */
  protected void sourceMenuItem_actionPerformed(ActionEvent e) {
    if (currentSelectedSource == null) {
      if (specNodes.size() > 0) {
        JOptionPane.showMessageDialog(this, "Please Select a Spectrum",
            "Select Spectrum", JOptionPane.ERROR_MESSAGE);
      }
      return;
    }
    try {
      new TextDialog(this, currentSelectedSource.getFilePath(), true);
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
      setJSVPanelProperties(JSVPanel.getPanel0(frames[i]),
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
      jsvp.setCoordinatesOn(true);
      coordsToggleButton.setSelected(true);
    } else {
      jsvp.setCoordinatesOn(false);
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
    jsvp.setReversePlot(e.getStateChange() == ItemEvent.SELECTED);
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
    jsvp.nextView();
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
    jsvp.previousView();
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
    jsvp.reset();
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
    jsvp.clearViews();
  }

  /**
   * Sets the status of the menuitems according to the properties of the current
   * selected JSVPanel
   * 
   * @param e
   *        the MenuEvent
   */
  void displayMenu_menuSelected(MenuEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    gridCheckBoxMenuItem.setSelected(jsvp.isGridOn());
    coordsCheckBoxMenuItem.setSelected(jsvp.isCoordinatesOn());
    revPlotCheckBoxMenuItem.setSelected(jsvp.isPlotReversed());
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

    dirLastExported = jsvp.exportSpectra(this, fc, type, recentFileName,
        dirLastExported);

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
    JInternalFrame frame = getCurrentFrame();
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
   * Shows all JInternalFrames
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
    if (currentSelectedSource == null) {
      JOptionPane.showMessageDialog(null, "Please Select a Spectrum",
          "Select Spectrum", JOptionPane.WARNING_MESSAGE);
      return;
    }
    String errorLog = currentSelectedSource.getErrorLog();
    if (errorLog != null)
      new TextDialog(this, currentSelectedSource.getFilePath(), errorLog, true);
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
    jsvp.setGridOn(((JToggleButton) e.getSource()).isSelected());
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
    parameters.coordinatesOn = selected;
    jsvp.setCoordinatesOn(selected);
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
    jsvp.setReversePlot(((JToggleButton) e.getSource()).isSelected());
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
   * Shows the legend or key for the overlayed spectra
   * 
   * @param e
   *        the ActionEvent
   */
  protected void overlayKeyMenuItem_actionPerformed(ActionEvent e) {
    JSVPanel jsvp = getCurrentJSVPanel();
    if (jsvp == null)
      return;
    if (jsvp.getNumberOfSpectra() > 1) {
      //legend = new OverlayLegendDialog(this, jsvp);
    }
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
    openFile(null, url.indexOf("://") < 0 ? "file:/" + url : url, null, -1, -1);
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
    JMenuItem menuItem = (JMenuItem) e.getSource();
    String filePath = menuItem.getText();
    File file = new File(filePath);
    openFile(file);
  }

  /**
   * Shows the current Source file as overlayed
   * 
   * @param e
   *        the ActionEvent
   */
  protected void overlayMenuItem_actionPerformed(ActionEvent e, boolean isAll) {
    if (isAll) {
      overlayAllMenuItem.setSelected(false);
      if (currentSelectedSource == null || selectedJSVPanel == null) {
        return;
      }
      if (!currentSelectedSource.isCompoundSource) {
        writeStatus("Unable to Overlay, Incompatible source type");
        return;
      }
      if (currentSelectedSource.isOverlay()) {
        closeSource(currentSelectedSource);
        return;
      }
      try {
        if (JDXSpectrum.areScalesCompatible(currentSelectedSource.getSpectra())) {
          closeSource(currentSelectedSource);
          overlaySpectra(currentSelectedSource, null);
        } else {
          showUnableToOverlayMessage();
        }
      } catch (Exception ex) {
        splitSpectra(currentSelectedSource);
      }
    } else {
      jsvpPopupMenu.overlay();
    }
  }

  /**
   * Closes the current JDXSource
   * 
   * @param e
   *        the ActionEvent
   */
  protected void closeMenuItem_actionPerformed(ActionEvent e) {
    closeSource(currentSelectedSource);
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
    JDXSource source = currentSelectedSource;
    JSVPanel jsvp = getCurrentJSVPanel();
    if (!source.isCompoundSource || jsvp == null
        || jsvp.getNumberOfSpectra() == 1) {
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
    jsvp.setGridOn(e.getStateChange() == ItemEvent.SELECTED);
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
    jsvp.setXScaleOn(e.getStateChange() == ItemEvent.SELECTED);
    jsvp.setXUnitsOn(e.getStateChange() == ItemEvent.SELECTED);
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
    jsvp.setYScaleOn(e.getStateChange() == ItemEvent.SELECTED);
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
    jsvpPopupMenu.setSelectedJSVPanel(jsvp);
    jsvpPopupMenu.setSource(currentSelectedSource);
    jsvpPopupMenu.properties_actionPerformed(e);

  }

  protected class JSVTreeNode extends DefaultMutableTreeNode {

    private final static long serialVersionUID = 1L;

    private JDXSource source;
    private String fileName;
    private JInternalFrame frame;
    private JSVPanel jsvp;
    private String id;

    private JSVTreeNode(String fileName, JDXSource source,
        JInternalFrame frame, JSVPanel jsvp, String id) {
      super(fileName);
      this.source = source;
      this.fileName = fileName;
      this.frame = frame;
      this.jsvp = jsvp;
      this.id = id;
    }

    @Override
    public String toString() {
      return ((id == null ? "" : id + ": ") + (frame == null ? fileName : frame.getTitle()));
    }

  }
  
  /**
   * Listener for a JInternalFrame
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
      JInternalFrame frame = e.getInternalFrame();
      if (!frame.isVisible())
        return;
      setCurrentSource(source);

      // Update the menu items for the display menu
      JSVPanel jsvp = JSVPanel.getPanel0(frame);
      JDXSpectrum spec = jsvp.getSpectrum();
      gridCheckBoxMenuItem.setSelected(jsvp.isGridOn());
      gridToggleButton.setSelected(jsvp.isGridOn());
      coordsCheckBoxMenuItem.setSelected(jsvp.isCoordinatesOn());
      coordsToggleButton.setSelected(jsvp.isCoordinatesOn());
      revPlotCheckBoxMenuItem.setSelected(jsvp.isPlotReversed());
      revPlotToggleButton.setSelected(jsvp.isPlotReversed());
      scaleXCheckBoxMenuItem.setSelected(jsvp.isXScaleOn());
      scaleYCheckBoxMenuItem.setSelected(jsvp.isYScaleOn());

      if (jsvp.getNumberOfSpectra() > 1) {
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
     * Called when <code>JInternalFrame</code> is closing
     * 
     * @param e
     *        the InternalFrameEvent
     */
    @Override
    public void internalFrameClosing(InternalFrameEvent e) {
      final JInternalFrame frame = e.getInternalFrame();

      doInternalFrameClosing(frame);
    }

    /**
     * Called when <code>JInternalFrame</code> has opened
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
      return new Font("Dialog", (node == null || node.frame == null
          || node.frame.isVisible() ? Font.BOLD : Font.ITALIC), 12);
    }

  }

  public void zoomed(double x1, double y1, double x2, double y2) {
    if (Double.isNaN(x2)) {
      // pass on to menu
      advanceSpectrumBy((int) x1);
      return;
    }
    if (x1 == x2)
      writeStatus("");
    else
      writeStatus("Double-Click highlighted spectrum in menu to zoom out.");
  }

  private void advanceSpectrumBy(int n) {
    int i = specNodes.size(); 
    for (; --i >= 0;)
      if (specNodes.get(i).jsvp == selectedJSVPanel)
        break;
    setFrame(i + n);
    selectedJSVPanel.requestFocusInWindow();
  }

}
