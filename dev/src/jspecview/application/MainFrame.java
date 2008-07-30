/* Copyright (C) 2002-2008  The JSpecView Development Team
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

package jspecview.application;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
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
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.RenderedImage;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.io.BufferedInputStream;
import java.io.DataInputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
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
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jspecview.common.CompoundSource;
import jspecview.common.Graph;
import jspecview.common.JDXSource;
import jspecview.common.CMLSource;
import jspecview.common.AnIMLSource;
import jspecview.common.JDXSourceFactory;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVPanel;
import jspecview.common.JSVPanelPopupListener;
import jspecview.common.JSVPanelPopupMenu;
import jspecview.common.OverlayLegendDialog;
import jspecview.common.PrintLayoutDialog;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.xml.AnIMLExporter;
import jspecview.xml.CMLExporter;
import jspecview.xml.SVGExporter;
import jspecview.util.DisplayScheme;
import jspecview.util.DisplaySchemesProcessor;
import jspecview.util.JDXExporter;
import jspecview.util.JSpecViewUtils;
import jspecview.util.TransmittanceAbsorbanceConverter;
import mdidesktop.ScrollableDesktopPane;
import mdidesktop.WindowMenu;
import jspecview.common.Visible;
import jspecview.util.Coordinate;

/**
 * The Main Class or Entry point of the JSpecView Application.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class MainFrame
    extends JFrame implements DropTargetListener {

//  ------------------------ Program Properties -------------------------

  String propertiesFileName = "jspecview.properties";
  boolean toolbarOn;
  boolean sidePanelOn;
  boolean statusbarOn;
  boolean showExitDialog;

  String defaultDisplaySchemeName;
  boolean autoOverlay;
  boolean autoShowLegend;
  boolean useDirLastOpened;
  boolean useDirLastExported;
  String dirLastOpened;
  String dirLastExported;

  boolean autoIntegrate;

  boolean AtoTSeparateWindow;
  String autoATConversion;

//   ------------------------ Display Properties -------------------------
  Color bgc= Color.WHITE;
  boolean gridOn;
  boolean coordinatesOn;
  boolean scaleOn;
  boolean continuous;
  //  ----------------------- Application Attributes ---------------------

  Vector<JDXSource> jdxSources = new Vector<JDXSource> ();
  Vector<File> jdxSourceFiles = new Vector<File> ();
  int numRecent = 10;
  Vector<String> recentFilePaths = new Vector<String> (numRecent);
  JDXSource currentSelectedSource = null;
  Properties properties;
  DisplaySchemesProcessor dsp;
  String tempDS,sltnclr;

//   -------------------------- GUI Components  -------------------------

  final private int TO_TRANS = 0;
  final private int TO_ABS = 1;
  final private int IMPLIED = 2;

//   ----------------------------------------------------------------------

  JSVPanel selectedJSVPanel;
  URL iconURL;
  {
    iconURL = MainFrame.class.getClassLoader().getResource("icons/spec16.gif");
  }

  Image icon = Toolkit.getDefaultToolkit().getImage(iconURL);

  ImageIcon imageIcon = new ImageIcon(iconURL);
  JMenuBar menuBar = new JMenuBar();
  JMenu fileMenu = new JMenu();
  JMenuItem openMenuItem = new JMenuItem();
  JMenuItem printMenuItem = new JMenuItem();
  JMenuItem closeMenuItem = new JMenuItem();
  JMenuItem closeAllMenuItem = new JMenuItem();
  JMenuItem exitMenuItem = new JMenuItem();
  //JMenu windowMenu = new JMenu();
  JMenu helpMenu = new JMenu();
  JMenu optionsMenu = new JMenu();
  JMenu displayMenu = new JMenu();
  JMenu zoomMenu = new JMenu();
  JCheckBoxMenuItem gridCheckBoxMenuItem = new JCheckBoxMenuItem();
  JCheckBoxMenuItem coordsCheckBoxMenuItem = new JCheckBoxMenuItem();
  JCheckBoxMenuItem revPlotCheckBoxMenuItem = new JCheckBoxMenuItem();
  JMenuItem nextMenuItem = new JMenuItem();
  JMenuItem prevMenuItem = new JMenuItem();
  JMenuItem fullMenuItem = new JMenuItem();
  JMenuItem clearMenuItem = new JMenuItem();
  JMenuItem preferencesMenuItem = new JMenuItem();
  JMenuItem contentsMenuItem = new JMenuItem();
  JMenuItem aboutMenuItem = new JMenuItem();
  JMenu openRecentMenu = new JMenu();
  JMenuItem importAnIML = new JMenuItem();
  JMenu exportJcampMenu = new JMenu();
  JMenuItem xyMenuItem = new JMenuItem();
  JMenuItem fixMenuItem = new JMenuItem();
  JMenuItem sqzMenuItem = new JMenuItem();
  JMenuItem pacMenuItem = new JMenuItem();
  JMenuItem difMenuItem = new JMenuItem();
  JMenu exportAsMenu = new JMenu();
  JMenuItem svgMenuItem = new JMenuItem();
  JMenuItem animlMenuItem = new JMenuItem();
  JMenuItem cmlMenuItem = new JMenuItem();
  JMenuItem pngMenuItem = new JMenuItem();
  JMenuItem jpgMenuItem = new JMenuItem();
  JCheckBoxMenuItem toolbarCheckBoxMenuItem = new JCheckBoxMenuItem();
  JCheckBoxMenuItem sidePanelCheckBoxMenuItem = new JCheckBoxMenuItem();
  JCheckBoxMenuItem statusCheckBoxMenuItem = new JCheckBoxMenuItem();
  BorderLayout mainborderLayout = new BorderLayout();
  JSplitPane mainSplitPane = new JSplitPane();
  JSplitPane sideSplitPane = new JSplitPane();

  JSpecViewFileFilter filter = new JSpecViewFileFilter();
  JSpecViewFileFilter filter2 = new JSpecViewFileFilter();
  JSpecViewFileFilter filter3 = new JSpecViewFileFilter();

  JScrollPane scrollPane = new JScrollPane();
  ScrollableDesktopPane desktopPane = new ScrollableDesktopPane();
  WindowMenu windowMenu = new WindowMenu(desktopPane);
  JTree spectraTree;
  JScrollPane spectraTreePane;
  JTree errorTree = new JTree(new DefaultMutableTreeNode("Errors"));
  JPanel statusPanel = new JPanel();
  JLabel statusLabel = new JLabel();

  JSVPanelPopupMenu jsvpPopupMenu = new JSVPanelPopupMenu();
  JMenuItem splitMenuItem = new JMenuItem();
  JMenuItem overlayMenuItem = new JMenuItem();
  DefaultMutableTreeNode rootNode;
  DefaultTreeModel spectraTreeModel;
  JMenuItem hideMenuItem = new JMenuItem();
  JMenuItem hideAllMenuItem = new JMenuItem();
  JMenu showMenu = new JMenu();
  JMenuItem showMenuItem = new JMenuItem();
  JMenuItem sourceMenuItem = new JMenuItem();
  JMenuItem propertiesMenuItem = new JMenuItem();
  BorderLayout borderLayout1 = new BorderLayout();

  JFileChooser fc;
  JToolBar jsvToolBar = new JToolBar();
  JButton previousButton = new JButton();
  JButton nextButton = new JButton();
  JButton resetButton = new JButton();
  JButton clearButton = new JButton();
  JButton openButton = new JButton();
  JButton propertiesButton = new JButton();
  JToggleButton gridToggleButton = new JToggleButton();
  JToggleButton coordsToggleButton = new JToggleButton();
  JButton printButton = new JButton();
  JToggleButton revPlotToggleButton = new JToggleButton();
  JButton aboutButton = new JButton();
  JButton overlaySplitButton = new JButton();

  {
    iconURL = MainFrame.class.getClassLoader().getResource("icons/spec16.gif");
  }

  ImageIcon openIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/open24.gif"));
  ImageIcon printIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/print24.gif"));
  ImageIcon gridIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/grid24.gif"));
  ImageIcon coordinatesIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/coords24.gif"));
  ImageIcon reverseIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/reverse24.gif"));
  ImageIcon previousIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/previous24.gif"));
  ImageIcon nextIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/next24.gif"));
  ImageIcon resetIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/reset24.gif"));
  ImageIcon clearIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/clear24.gif"));
  ImageIcon informationIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/information24.gif"));
  ImageIcon aboutIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/about24.gif"));
  ImageIcon overlayIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/overlay24.gif"));
  ImageIcon splitIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/split24.gif"));
  ImageIcon overlayKeyIcon = new ImageIcon(
      MainFrame.class.getClassLoader().getResource("icons/overlayKey24.gif"));

  JMenuItem overlayKeyMenuItem = new JMenuItem();
  JButton overlayKeyButton = new JButton();
  OverlayLegendDialog legend;
  JMenu processingMenu = new JMenu();
  private JMenuItem integrateMenuItem = new JMenuItem();
  private JMenuItem transAbsMenuItem = new JMenuItem();
  private JMenuItem solColMenuItem = new JMenuItem();
  private JMenuItem errorLogMenuItem = new JMenuItem();

  private String aboutJSpec = "\nJSpecView is a graphical viewer for JCAMP-DX Spectra\nCopyright (c) 2008\nUniversity of the West Indies, Mona ";

  // Does certain tasks once regardless of how many instances of the program
  // are running
  {
    onProgramStart();
  }

  /**
   * Constructor
   */
  public MainFrame() {
    // initialise MainFrame as a target for the drag-and-drop action
    new DropTarget(this, this);

    // initialise Spectra tree
    initSpectraTree();

    // Initialise GUI Components
    try {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    setApplicationElements();

    // When application exits ...
    this.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        try {
          onProgramExit();
        }
        catch (Exception e) {
        }

        int option;
        if (showExitDialog) {
          option = JOptionPane.showConfirmDialog(MainFrame.this,
                                                 "Are you sure?", "Exit",
                                                 JOptionPane.OK_CANCEL_OPTION,
                                                 JOptionPane.QUESTION_MESSAGE);

          if (option == JOptionPane.OK_OPTION) {
            System.exit(0);
          }
        }
        else {
          System.exit(0);
        }
      }
    });

  }

  /**
   * Shows or hides certain GUI elements
   */
  private void setApplicationElements() {
    // hide side panel if sidePanelOn property is false
    sidePanelCheckBoxMenuItem.setSelected(sidePanelOn);
    toolbarCheckBoxMenuItem.setSelected(toolbarOn);
    statusCheckBoxMenuItem.setSelected(statusbarOn);
  }

  /**
   * Task to do when program starts
   */
  void onProgramStart() {

    boolean loadedOk;
    //Set Default Properties

    // Initalise application properties with defaults
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
    properties.setProperty("showScale", "true");
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
    }
    catch (Exception e) {
    }

// the thought was to have a set of displayschemes stored internally in JSVApp.jar
// this is not yet functioning properly and getResource does not seem to find them

    dsp = new DisplaySchemesProcessor();
    try {
      URL url = MainFrame.class.getClassLoader().getResource("displaySchemes.xml");
      try {
          InputStream in = (InputStream) (MainFrame.class.getResourceAsStream("displaySchemes.xml"));
        }
        catch (Exception jEX) {
          System.err.println("could not find displayschemes?");
        }
       loadedOk = dsp.load("displaySchemes.xml");
    }
    catch (Exception ex) {
      loadedOk = dsp.loadDefault("missingDS.xml");
      System.err.println("Warning, display schemes not properly loaded, using Default settings!");
    }

    setApplicationProperties();
    tempDS = defaultDisplaySchemeName;

    if (JSpecViewUtils.DEBUG) {
      fc = new JFileChooser("C:/temp");
    }
    else {
      if (useDirLastOpened) {
        fc = new JFileChooser(dirLastOpened);
      }
      else {
        fc = new JFileChooser();
      }
    }

    filter2.addExtension("jpg");
    filter2.addExtension("png");
    filter2.setDescription("Image Files");
    fc.setFileFilter(filter2);
    filter.addExtension("jdx");
    filter.addExtension("dx");
    filter.setDescription("JCAMP-DX Files");
    fc.setFileFilter(filter);
  }

  /**
   * Sets the perferences or properties of the application that is loaded
   * from a properties file.
   */
  private void setApplicationProperties() {

    String recentFilesString = properties.getProperty("recentFilePaths");
    openRecentMenu.removeAll();
    recentFilePaths.removeAllElements();
    if (!recentFilesString.equals("")) {
      StringTokenizer st = new StringTokenizer(recentFilesString, ",");
      JMenuItem menuItem;
      String path;
      while (st.hasMoreTokens()) {
        path = st.nextToken().trim();
        recentFilePaths.addElement(path);
        menuItem = new JMenuItem(path);
        openRecentMenu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            openRecent_actionPerformed(ae);
          }
        });
      }
    }

    showExitDialog = Boolean.valueOf(properties.getProperty("confirmBeforeExit")).
        booleanValue();

    autoOverlay = Boolean.valueOf(properties.getProperty("automaticallyOverlay")).
        booleanValue();
    autoShowLegend = Boolean.valueOf(properties.getProperty(
        "automaticallyShowLegend")).booleanValue();

    useDirLastOpened = Boolean.valueOf(properties.getProperty(
        "useDirectoryLastOpenedFile")).booleanValue();
    useDirLastExported = Boolean.valueOf(properties.getProperty(
        "useDirectoryLastExportedFile")).booleanValue();
    dirLastOpened = properties.getProperty("directoryLastOpenedFile");
    dirLastExported = properties.getProperty("directoryLastExportedFile");

    sidePanelOn = Boolean.valueOf(properties.getProperty("showSidePanel")).
        booleanValue();
    toolbarOn = Boolean.valueOf(properties.getProperty("showToolBar")).
        booleanValue();
    statusbarOn = Boolean.valueOf(properties.getProperty("showStatusBar")).
        booleanValue();

    // Initialise DisplayProperties
    defaultDisplaySchemeName = properties.getProperty(
        "defaultDisplaySchemeName");
    gridOn = Boolean.valueOf(properties.getProperty("showGrid")).booleanValue();
    coordinatesOn = Boolean.valueOf(properties.getProperty("showCoordinates")).
        booleanValue();
    scaleOn = Boolean.valueOf(properties.getProperty("showScale")).booleanValue();
    // Need to apply Properties to all panels that are opened
    // and update coordinates and grid CheckBoxMenuItems


    // Processing Properties
    autoATConversion = properties.getProperty("automaticTAConversion");
    AtoTSeparateWindow = Boolean.valueOf(properties.getProperty(
        "AtoTSeparateWindow")).booleanValue();

    autoIntegrate = Boolean.valueOf(properties.getProperty(
        "automaticallyIntegrate")).booleanValue();
    JSpecViewUtils.integralMinY = properties.getProperty("integralMinY");
    JSpecViewUtils.integralFactor = properties.getProperty("integralFactor");
    JSpecViewUtils.integralOffset = properties.getProperty("integralOffset");
    JSpecViewUtils.integralPlotColor = JSpecViewUtils.getColorFromString(
        properties.getProperty("integralPlotColor"));

  }

  /**
   * Returns a <code>Color</code> instance from a parameter
   * @param key the parameter name
   * @param def the default value
   * @return a <code>Color</code> instance from a parameter
   */
  private Color getColorProperty(String key, Color def) {
    String param = properties.getProperty(key);
    Color color = JSpecViewUtils.getColorFromString(param);
    if (color == null) {
      return def;
    }
    else {
      return color;
    }
  }

  /**
   * Tasks to do when program exits
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
    rootNode = new DefaultMutableTreeNode("Spectra");
    spectraTreeModel = new DefaultTreeModel(rootNode);
    spectraTree = new JTree(spectraTreeModel);
    spectraTree.getSelectionModel().setSelectionMode(
        TreeSelectionModel.SINGLE_TREE_SELECTION);
    spectraTree.setCellRenderer(new SpectraTreeCellRenderer());
    spectraTree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
            spectraTree.getLastSelectedPathComponent();

        if (node == null) {
          return;
        }

        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
          SpecInfo specInfo = (SpecInfo) nodeInfo;
          JInternalFrame frame = specInfo.frame;
          frame.moveToFront();
          try {
            frame.setSelected(true);
          }
          catch (PropertyVetoException pve) {
          }
        }
      }
    });
    spectraTree.putClientProperty("JTree.lineStyle", "Angled");
    spectraTree.setShowsRootHandles(true);
    spectraTreePane = new JScrollPane(spectraTree);
    spectraTree.setEditable(false);
    spectraTree.setRootVisible(false);
  }

  /**
   * The main method
   * @param args program parameters, takes the name of the file to open
   */
  public static void main(String args[]) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {}

    MainFrame frame = new MainFrame();
    frame.setSize(800, 500);
    //frame.pack();
    frame.setVisible(true);

    // check for command-line arguments
    if (args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        System.out.println("JSpecView is attempting to open " + args[i]);
        String filePath = args[i];
        File file = new File(filePath);
        if (file.exists()) {
          frame.openFile(file);
        }
        else {
          frame.writeStatus("File: " + filePath + " does not exist");
        }
      }
    }
    else {
      frame.showFileOpenDialog();
    }
  }

  /**
   * Intialises GUI components
   * @throws Exception
   */
  private void jbInit() throws Exception {
    this.setIconImage(icon);
    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.setJMenuBar(menuBar);
    this.setTitle("JSpecView");
    this.getContentPane().setLayout(mainborderLayout);
    fileMenu.setMnemonic('F');
    fileMenu.setText("File");
    openMenuItem.setActionCommand("Open");
    openMenuItem.setMnemonic('O');
    openMenuItem.setText("Open...");
    openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(79,
        java.awt.event.KeyEvent.CTRL_MASK, false));
    openMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        open_actionPerformed(e);
      }
    });
    printMenuItem.setMnemonic('P');
    printMenuItem.setText("Print...");
    printMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(80,
        java.awt.event.KeyEvent.CTRL_MASK, false));
    printMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        printMenuItem_actionPerformed(e);
      }
    });
    closeMenuItem.setMnemonic('C');
    closeMenuItem.setText("Close");
    closeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(115,
        java.awt.event.KeyEvent.CTRL_MASK, false));
    closeMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeMenuItem_actionPerformed(e);
      }
    });
    closeAllMenuItem.setMnemonic('A');
    closeAllMenuItem.setText("Close All");
    closeAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeAllMenuItem_actionPerformed(e);
      }
    });
    exitMenuItem.setMnemonic('X');
    exitMenuItem.setText("Exit");
    exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(115,
        java.awt.event.KeyEvent.ALT_MASK, false));
    exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
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
    displayMenu.addMenuListener(new javax.swing.event.MenuListener() {
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
    gridCheckBoxMenuItem.setMnemonic('G');
    gridCheckBoxMenuItem.setText("Grid");
    gridCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(71,
        java.awt.event.KeyEvent.CTRL_MASK, false));
    gridCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        gridCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    coordsCheckBoxMenuItem.setMnemonic('C');
    coordsCheckBoxMenuItem.setText("Coordinates");
    coordsCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(67,
        java.awt.event.KeyEvent.CTRL_MASK, false));
    coordsCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        coordsCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    revPlotCheckBoxMenuItem.setMnemonic('R');
    revPlotCheckBoxMenuItem.setText("Reverse Plot");
    revPlotCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
        82, java.awt.event.KeyEvent.CTRL_MASK, false));
    revPlotCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        revPlotCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    nextMenuItem.setMnemonic('N');
    nextMenuItem.setText("Next View");
    nextMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(78,
        java.awt.event.KeyEvent.CTRL_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
    nextMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        nextMenuItem_actionPerformed(e);
      }
    });
    prevMenuItem.setMnemonic('P');
    prevMenuItem.setText("Previous View");
    prevMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(80,
        java.awt.event.KeyEvent.CTRL_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
    prevMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        prevMenuItem_actionPerformed(e);
      }
    });
    fullMenuItem.setMnemonic('F');
    fullMenuItem.setText("Full View");
    fullMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(70,
        java.awt.event.KeyEvent.CTRL_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
    fullMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fullMenuItem_actionPerformed(e);
      }
    });
    clearMenuItem.setMnemonic('C');
    clearMenuItem.setText("Clear Views");
    clearMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(67,
        java.awt.event.KeyEvent.CTRL_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
    clearMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearMenuItem_actionPerformed(e);
      }
    });
    preferencesMenuItem.setActionCommand("Preferences");
    preferencesMenuItem.setMnemonic('P');
    preferencesMenuItem.setText("Preferences...");
    preferencesMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(80,
        java.awt.event.KeyEvent.SHIFT_MASK, false));
    preferencesMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        preferencesMenuItem_actionPerformed(e);
      }
    });
    contentsMenuItem.setActionCommand("Contents");
    contentsMenuItem.setMnemonic('C');
    contentsMenuItem.setText("Contents...");
    contentsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(112, 0, false));
    contentsMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        contentsMenuItem_actionPerformed(e);
      }
    });
    aboutMenuItem.setMnemonic('A');
    aboutMenuItem.setText("About");
    aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        aboutMenuItem_actionPerformed(e);
      }
    });
    openRecentMenu.setActionCommand("OpenRecent");
    openRecentMenu.setMnemonic('R');
    openRecentMenu.setText("Open Recent");
    importAnIML.setActionCommand("ImportAnIML");
    importAnIML.setMnemonic('I');
    importAnIML.setAccelerator(javax.swing.KeyStroke.getKeyStroke(73,
        java.awt.event.KeyEvent.CTRL_MASK, false));
    importAnIML.setText("Import AnIML/CML...");
    importAnIML.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        import_actionPerformed(e);
      }
    });
    exportJcampMenu.setMnemonic('J');
    exportJcampMenu.setText("Export JCAMP-DX ");
    xyMenuItem.setMnemonic('X');
    xyMenuItem.setText("XY");
    xyMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        xyMenuItem_actionPerformed(e);
      }
    });
    fixMenuItem.setMnemonic('F');
    fixMenuItem.setText("FIX");
    fixMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fixMenuItem_actionPerformed(e);
      }
    });
    pacMenuItem.setMnemonic('P');
    pacMenuItem.setText("PAC");
    pacMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pacMenuItem_actionPerformed(e);
      }
    });
    sqzMenuItem.setMnemonic('S');
    sqzMenuItem.setText("SQZ");
    sqzMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        sqzMenuItem_actionPerformed(e);
      }
    });
    difMenuItem.setMnemonic('D');
    difMenuItem.setText("DIF");
    difMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        difMenuItem_actionPerformed(e);
      }
    });
    exportAsMenu.setMnemonic('E');
    exportAsMenu.setText("Export As");
    animlMenuItem.setMnemonic('X');
    animlMenuItem.setText("AnIML");
    animlMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        animlMenuItem_actionPerformed(e);
      }
    });
    cmlMenuItem.setText("CML");
    cmlMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        animlMenuItem_actionPerformed(e);
      }
    });
    svgMenuItem.setMnemonic('S');
    svgMenuItem.setText("SVG");
    svgMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        svgMenuItem_actionPerformed(e);
      }
    });
    pngMenuItem.setMnemonic('P');
    pngMenuItem.setText("PNG");
    pngMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pngMenuItem_actionPerformed(e);
      }
    });
    jpgMenuItem.setMnemonic('J');
    jpgMenuItem.setText("JPG");
    jpgMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        jpgMenuItem_actionPerformed(e);
      }
    });
    toolbarCheckBoxMenuItem.setMnemonic('T');
    toolbarCheckBoxMenuItem.setSelected(true);
    toolbarCheckBoxMenuItem.setText("Toolbar");
    toolbarCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
        84,
        java.awt.event.KeyEvent.ALT_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
    toolbarCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        toolbarCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    sidePanelCheckBoxMenuItem.setMnemonic('S');
    sidePanelCheckBoxMenuItem.setSelected(true);
    sidePanelCheckBoxMenuItem.setText("Side Panel");
    sidePanelCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
        83,
        java.awt.event.KeyEvent.ALT_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
    sidePanelCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        sidePanelCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    statusCheckBoxMenuItem.setMnemonic('B');
    statusCheckBoxMenuItem.setSelected(true);
    statusCheckBoxMenuItem.setText("Status Bar");
    statusCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(66,
        java.awt.event.KeyEvent.ALT_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
    statusCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        statusCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    sideSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
    sideSplitPane.setOneTouchExpandable(true);
    statusLabel.setToolTipText("");
    statusLabel.setHorizontalTextPosition(SwingConstants.LEADING);
    statusLabel.setText("  ");
    statusPanel.setBorder(BorderFactory.createEtchedBorder());
    statusPanel.setLayout(borderLayout1);
    splitMenuItem.setMnemonic('P');
    splitMenuItem.setText("Split");
    splitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(83,
        java.awt.event.KeyEvent.CTRL_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
    splitMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        splitMenuItem_actionPerformed(e);
      }
    });
    overlayMenuItem.setMnemonic('O');
    overlayMenuItem.setText("Overlay");
    overlayMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(79,
        java.awt.event.KeyEvent.CTRL_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
    overlayMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        overlayMenuItem_actionPerformed(e);
      }
    });
    hideMenuItem.setMnemonic('H');
    hideMenuItem.setText("Hide");
    hideMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        hideMenuItem_actionPerformed(e);
      }
    });
    hideAllMenuItem.setMnemonic('L');
    hideAllMenuItem.setText("Hide All");
    hideAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        hideAllMenuItem_actionPerformed(e);
      }
    });
    showMenuItem.setMnemonic('S');
    showMenuItem.setText("Show All");
//    showAllMenuItem.setMnemonic('A');
//    showAllMenuItem.setText("Show All");
    showMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showMenuItem_actionPerformed(e);
      }
    });
    sourceMenuItem.setActionCommand("Source  ");
    sourceMenuItem.setMnemonic('S');
    sourceMenuItem.setText("Source ...");
    sourceMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(83,
        java.awt.event.KeyEvent.CTRL_MASK, false));
    sourceMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        sourceMenuItem_actionPerformed(e);
      }
    });
    propertiesMenuItem.setMnemonic('P');
    propertiesMenuItem.setText("Properties");
    propertiesMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(72,
        java.awt.event.KeyEvent.CTRL_MASK, false));
    propertiesMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        propertiesMenuItem_actionPerformed(e);
      }
    });
    mainSplitPane.setOneTouchExpandable(true);
    borderLayout1.setHgap(2);
    borderLayout1.setVgap(2);

    clearButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearButton_actionPerformed(e);
      }
    });
    previousButton.setBorder(null);
    previousButton.setToolTipText("Previous View");
    previousButton.setIcon(previousIcon);
    previousButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        previousButton_actionPerformed(e);
      }
    });
    nextButton.setBorder(null);
    nextButton.setToolTipText("Next View");
    nextButton.setIcon(nextIcon);
    nextButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        nextButton_actionPerformed(e);
      }
    });
    resetButton.setBorder(null);
    resetButton.setToolTipText("Reset ");
    resetButton.setIcon(resetIcon);
    resetButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        resetButton_actionPerformed(e);
      }
    });
    clearButton.setBorder(null);
    clearButton.setToolTipText("Clear Views");
    clearButton.setIcon(clearIcon);
    openButton.setBorder(null);
    openButton.setToolTipText("Open");
    openButton.setIcon(openIcon);
    openButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        openButton_actionPerformed(e);
      }
    });
    propertiesButton.setBorder(null);
    propertiesButton.setToolTipText("Properties");
    propertiesButton.setIcon(informationIcon);
    propertiesButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        propertiesButton_actionPerformed(e);
      }
    });
    gridToggleButton.setBorder(null);
    gridToggleButton.setToolTipText("Toggle Grid");
    gridToggleButton.setIcon(gridIcon);
    gridToggleButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        gridToggleButton_actionPerformed(e);
      }
    });
    coordsToggleButton.setBorder(null);
    coordsToggleButton.setToolTipText("Toggle Coordinates");
    coordsToggleButton.setIcon(coordinatesIcon);
    coordsToggleButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        coordsToggleButton_actionPerformed(e);
      }
    });
    printButton.setBorder(null);
    printButton.setToolTipText("Print");
    printButton.setIcon(printIcon);
    printButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        printButton_actionPerformed(e);
      }
    });
    revPlotToggleButton.setBorder(null);
    revPlotToggleButton.setToolTipText("Reverse Plot");
    revPlotToggleButton.setIcon(reverseIcon);
    revPlotToggleButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        revPlotToggleButton_actionPerformed(e);
      }
    });
    aboutButton.setBorder(null);
    aboutButton.setToolTipText("About JSpecView");
    aboutButton.setIcon(aboutIcon);
    aboutButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        aboutButton_actionPerformed(e);
      }
    });
    overlaySplitButton.setBorder(null);
    overlaySplitButton.setIcon(overlayIcon);
    overlaySplitButton.setToolTipText("Overlay Display");
    overlaySplitButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        overlaySplitButton_actionPerformed(e);
      }
    });
    overlayKeyMenuItem.setEnabled(false);
    overlayKeyMenuItem.setText("Overlay Key");
    overlayKeyMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        overlayKeyMenuItem_actionPerformed(e);
      }
    });
    overlayKeyButton.setEnabled(false);
    overlayKeyButton.setBorder(null);
    overlayKeyButton.setToolTipText("Display Key for Overlaid Spectra");
    overlayKeyButton.setIcon(overlayKeyIcon);
    overlayKeyButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        overlayKeyButton_actionPerformed(e);
      }
    });
    processingMenu.setMnemonic('P');
    processingMenu.setText("Processing");
    integrateMenuItem.setMnemonic('I');
    integrateMenuItem.setText("Integrate HNMR");
    integrateMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        integrateMenuItem_actionPerformed(e);
      }
    });
    transAbsMenuItem.setText("Transmittance/Absorbance");
    transAbsMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        transAbsMenuItem_actionPerformed(e);
      }
    });
    solColMenuItem.setText("Predicted Solution Colour");
    solColMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        solColMenuItem_actionPerformed(e);
      }
    });
    errorLogMenuItem.setText("Error Log ...");
    errorLogMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        errorLogMenuItem_actionPerformed(e);
      }
    });
    menuBar.add(fileMenu);
    menuBar.add(displayMenu).setEnabled(false);
    menuBar.add(optionsMenu);
    menuBar.add(windowMenu).setEnabled(false);
    menuBar.add(processingMenu).setEnabled(false);
    menuBar.add(helpMenu);
    fileMenu.add(openMenuItem);
    fileMenu.add(openRecentMenu);
    fileMenu.addSeparator();
    fileMenu.add(closeMenuItem).setEnabled(false);
    fileMenu.add(closeAllMenuItem).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(importAnIML);
    fileMenu.add(exportJcampMenu).setEnabled(false);
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
    displayMenu.add(revPlotCheckBoxMenuItem);
    displayMenu.addSeparator();
    displayMenu.add(zoomMenu);
    displayMenu.addSeparator();
    displayMenu.add(propertiesMenuItem);
    displayMenu.add(overlayKeyMenuItem);
    zoomMenu.add(nextMenuItem);
    zoomMenu.add(prevMenuItem);
    zoomMenu.add(fullMenuItem);
    zoomMenu.add(clearMenuItem);
    optionsMenu.add(preferencesMenuItem);
    optionsMenu.addSeparator();
    optionsMenu.add(toolbarCheckBoxMenuItem);
    optionsMenu.add(sidePanelCheckBoxMenuItem);
    optionsMenu.add(statusCheckBoxMenuItem);
    helpMenu.add(contentsMenuItem);
    helpMenu.add(aboutMenuItem);
    exportJcampMenu.add(xyMenuItem);
    exportJcampMenu.add(fixMenuItem);
    exportJcampMenu.add(pacMenuItem);
    exportJcampMenu.add(sqzMenuItem);
    exportJcampMenu.add(difMenuItem);
    exportAsMenu.add(animlMenuItem);
    exportAsMenu.add(cmlMenuItem);
    exportAsMenu.add(svgMenuItem);
    exportAsMenu.add(pngMenuItem);
    exportAsMenu.add(jpgMenuItem);
    //this.getContentPane().add(toolBar, BorderLayout.NORTH);
    this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
    statusPanel.add(statusLabel, BorderLayout.SOUTH);
    this.getContentPane().add(jsvToolBar, BorderLayout.NORTH);
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
    jsvToolBar.addSeparator();
    jsvToolBar.add(aboutButton, null);
    this.getContentPane().add(mainSplitPane, BorderLayout.CENTER);
    mainSplitPane.setLeftComponent(spectraTreePane);
    //sideSplitPane.setDividerLocation(350);
    mainSplitPane.setDividerLocation(200);
    scrollPane.getViewport().add(desktopPane);
    mainSplitPane.setRightComponent(scrollPane);
    //sideSplitPane.setTopComponent(spectraTreePane);
    //sideSplitPane.setBottomComponent(errorTree);
    windowMenu.add(splitMenuItem);
    windowMenu.add(overlayMenuItem);
    windowMenu.addSeparator();
    windowMenu.add(hideMenuItem);
    windowMenu.add(hideAllMenuItem);
//    windowMenu.add(showMenu);
    windowMenu.add(showMenuItem);
    processingMenu.add(integrateMenuItem).setEnabled(false);
    processingMenu.add(transAbsMenuItem).setEnabled(false);
    processingMenu.add(solColMenuItem).setEnabled(false);
    windowMenu.addSeparator();
  }

  /**
   * Shows dialog to open a file
   * @param e the ActionEvent
   */
  void open_actionPerformed(ActionEvent e) {
    showFileOpenDialog();
  }

  /**
   * Shows dialog to open a file
   */
  public void showFileOpenDialog() {
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
   * Shows dialog to import a file
   * @param e the ActionEvent
   */
  void import_actionPerformed(ActionEvent e) {
    showFileImportDialog();
  }

  /**
   * Shows dialog to import a file
   */
  public void showFileImportDialog() {
    String Yunits,Xunits;
    boolean continuous;

    byte[] infile = new byte[400];

    JDXSpectrum xmlSpec;
    JDXSource xmlSource;

    filter3.addExtension("xml");
    filter3.addExtension("aml");
    filter3.addExtension("cml");
    filter3.setDescription("XML Files");
    fc.setFileFilter(filter3);
    int returnVal = fc.showOpenDialog(this);

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      String fileName = file.getName();

      // ADD TO RECENT FILE PATHS, but no point for XML documents???
//       if(recentFilePaths.size() >= numRecent){
//         recentFilePaths.removeElementAt(numRecent - 1);
//       }
//       if(!recentFilePaths.contains(filePath)){
//         recentFilePaths.insertElementAt(filePath, 0);
//       }

      try {
        InputStream in = new FileInputStream(file);

//  read the first 400 characters from the XML document
//  but not sure what resources this is using and how to release it afterwards

        DataInputStream dis = new DataInputStream(new BufferedInputStream(new
            FileInputStream(file)));
        int filelength = dis.read(infile);

        String filecheck = new String(infile, 0, 400);
//         System.out.println("FILE START = " + filecheck);

// check for "<animl" in the first 400 chararcters
// if present then assume it is an AnIML document
// otherwise check for "xml-cml" in namespace in first 400 characters
// if present use cml routine else return nothing since don't know the file type

        if (filecheck.toLowerCase().contains("<animl")) {
          xmlSource = AnIMLSource.getAniMLInstance(in);
        }
        else if (filecheck.toLowerCase().contains("xml-cml")) {
          xmlSource = CMLSource.getCMLInstance(in);
        }
        else {
          System.err.println("not a recognizable XML Document");
          return;
        }
        in.close();
        currentSelectedSource = xmlSource;
        xmlSpec = xmlSource.getJDXSpectrum(0);
        JSVPanel jsvp = new JSVPanel(xmlSpec);
        jsvp.addMouseListener(new JSVPanelPopupListener(jsvpPopupMenu, jsvp, null));
        setJSVPanelProperties(jsvp);
        JInternalFrame frame = new JInternalFrame(xmlSpec.getTitle(), true, true, true, true);
        frame.setFrameIcon(imageIcon);
        frame.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(365, 200));
        frame.setPreferredSize(new Dimension(365, 200));
        frame.getContentPane().add(jsvp);
        frame.addInternalFrameListener(new JSVInternalFrameListener(file,xmlSource));

        jdxSources.addElement(xmlSource);
        jdxSourceFiles.addElement(file);
        closeMenuItem.setEnabled(true);
        closeMenuItem.setText("Close '" + fileName + "'");
        setTitle("JSpecView - " + file.getAbsolutePath());

        // add calls to enable Menus that were greyed out until a file is opened.

        exportAsMenu.setEnabled(true);
        closeAllMenuItem.setEnabled(true);
        displayMenu.setEnabled(true);
        windowMenu.setEnabled(true);
        processingMenu.setEnabled(true);
        printMenuItem.setEnabled(true);
        sourceMenuItem.setEnabled(true);
        errorLogMenuItem.setEnabled(true);
        exportJcampMenu.setEnabled(true);

        if (autoATConversion.equals("AtoT")) {
          TAConvert(frame, TO_TRANS);
        }
        else if (autoATConversion.equals("TtoA")) {
          TAConvert(frame, TO_ABS);
        }

        if (autoIntegrate) {
          JSpecViewUtils.integrate(this, frame, false);
        }

        //  Can only integrate a continuous H NMR spectrum
        continuous = xmlSpec.isContinuous();
        if (continuous && JSpecViewUtils.isHNMR(xmlSpec)) {
          integrateMenuItem.setEnabled(true);
        }
        else {
          integrateMenuItem.setEnabled(false);
        }

        //  Can only convert from T <-> A  if continuous and Absorbance or Transmittance
        Yunits = currentSelectedSource.getJDXSpectrum(0).getYUnits();
        if (continuous &&
            (Yunits.toLowerCase().contains("abs") ||
             Yunits.toLowerCase().contains("trans"))) {
          transAbsMenuItem.setEnabled(true);
        }
        else {
          transAbsMenuItem.setEnabled(false);
        }
        Xunits = currentSelectedSource.getJDXSpectrum(0).getXUnits();
        if(Yunits.toLowerCase().contains("trans") &
           Xunits.toLowerCase().contains("nanometer")){
          solColMenuItem.setEnabled(true);
        }else{
          solColMenuItem.setEnabled(false);
        }

        splitSpectra(xmlSource);
      }
      catch (IOException ex) {
        // STATUS --> write FileInputStream error message
        writeStatus(ex.getMessage());
      }
      catch (JSpecViewException jsve) {
        //STATUS --> write message
        writeStatus(jsve.getMessage());
      }
    }
  }

  /**
   * Open a file listed in the open recent menu
   * @param e the ActionEvent
   */
  public void openRecent_actionPerformed(ActionEvent e) {
    JMenuItem menuItem = (JMenuItem) e.getSource();
    String filePath = menuItem.getText();
    File file = new File(filePath);
    openFile(file);
  }

  /**
   * Opens and displays a file
   * @param file the file
   */
  public void openFile(File file) {
    String fileName = file.getName();
    String filePath = file.getAbsolutePath();
    String Yunits,Xunits;

    writeStatus(" ");
    if (jdxSourceFiles.contains(file)) {
      writeStatus("File: '" + filePath + "' is already opened");
      return;
    }

    // ADD TO RECENT FILE PATHS
    if (recentFilePaths.size() >= numRecent) {
      recentFilePaths.removeElementAt(numRecent - 1);
    }
    if (!recentFilePaths.contains(filePath)) {
      recentFilePaths.insertElementAt(filePath, 0);
    }

    String filePaths = "";
    JMenuItem menuItem;
    openRecentMenu.removeAll();
    int index;
    for (index = 0; index < recentFilePaths.size() - 1; index++) {
      String path = (String) recentFilePaths.elementAt(index);
      filePaths += path + ", ";
      menuItem = new JMenuItem(path);
      openRecentMenu.add(menuItem);
      menuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          openRecent_actionPerformed(ae);
        }
      });
    }
    String path = (String) recentFilePaths.elementAt(index);
    filePaths += path;
    menuItem = new JMenuItem(path);
    openRecentMenu.add(menuItem);
    menuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        openRecent_actionPerformed(ae);
      }
    });

    properties.setProperty("recentFilePaths", filePaths);

    try {
      InputStream in = new FileInputStream(file);
      JDXSourceFactory factory = new JDXSourceFactory(in);
      JDXSource source = factory.createJDXSource();
      currentSelectedSource = source;
      in.close();
      jdxSources.addElement(source);
      jdxSourceFiles.addElement(file);
      closeMenuItem.setEnabled(true);
      closeMenuItem.setText("Close '" + fileName + "'");
      setTitle("JSpecView - " + file.getAbsolutePath());

      // add calls to enable Menus that were greyed out until a file is opened.

      exportAsMenu.setEnabled(true);
      closeAllMenuItem.setEnabled(true);
      displayMenu.setEnabled(true);
      windowMenu.setEnabled(true);
      processingMenu.setEnabled(true);
      printMenuItem.setEnabled(true);
      sourceMenuItem.setEnabled(true);
      errorLogMenuItem.setEnabled(true);

      // if current spectrum is not a Peak Table then enable Menu to re-export

      if (currentSelectedSource.getJDXSpectrum(0).isContinuous()) {
        exportJcampMenu.setEnabled(true);
      }
      else {
        exportJcampMenu.setEnabled(false);
      }

      if (JSpecViewUtils.isHNMR(currentSelectedSource.getJDXSpectrum(0))) {
        integrateMenuItem.setEnabled(true);
      }
      else {
        integrateMenuItem.setEnabled(false);
      }

      Yunits = currentSelectedSource.getJDXSpectrum(0).getYUnits();
      //  Can only convert from T <-> A  if Absorbance or Transmittance and continuous
      if ( (currentSelectedSource.getJDXSpectrum(0).isContinuous()) &&
          (Yunits.toLowerCase().contains("abs") ||
           Yunits.toLowerCase().contains("trans"))) {
        transAbsMenuItem.setEnabled(true);
      }
      else {
        transAbsMenuItem.setEnabled(false);
      }
      Xunits = currentSelectedSource.getJDXSpectrum(0).getXUnits();
      if(Yunits.toLowerCase().contains("trans") &
         Xunits.toLowerCase().contains("nanometer")){
        solColMenuItem.setEnabled(true);
      }else{
        solColMenuItem.setEnabled(false);
      }
      if (autoOverlay && source instanceof CompoundSource) {
        try {
          overlaySpectra(source);
        }
        catch (ScalesIncompatibleException ex) {
          splitSpectra(source);
        }
      }
      else {
        splitSpectra(source);
      }
    }
    catch (IOException ex) {
    }

    catch (JSpecViewException ex) {
      writeStatus(ex.getMessage());
    }
  }

  /**
   * Sets the display properties as specified from the preferences dialog
   * or the properties file
   * @param jsvp the display panel
   */
  public void setJSVPanelProperties(JSVPanel jsvp) {

    DisplayScheme ds = (DisplayScheme) dsp.getDisplaySchemes().get(
        defaultDisplaySchemeName);

    jsvp.setCoordinatesOn(coordinatesOn);
    coordsCheckBoxMenuItem.setSelected(coordinatesOn);
    jsvp.setGridOn(gridOn);
    gridCheckBoxMenuItem.setSelected(gridOn);
    Color tmpcolour;
    jsvp.setScaleOn(scaleOn);
    jsvp.setUnitsOn(scaleOn);
    jsvp.setTitleColor(ds.getColor("title"));
    jsvp.setUnitsColor(ds.getColor("units"));
    jsvp.setScaleColor(ds.getColor("scale"));
    jsvp.setcoordinatesColor(ds.getColor("coordinates"));
    jsvp.setGridColor(ds.getColor("grid"));
//   jsvp.setPlotColor(ds.getColor("plot"));
//    jsvp.setPlotAreaColor(ds.getColor("plotarea"));
    jsvp.setPlotAreaColor(bgc);
    jsvp.setBackground(ds.getColor("background"));

    jsvp.setDisplayFontName(ds.getFont());
    jsvp.repaint();
  }

  /**
   * Shows the current Source file as overlayed
   * @param e the ActionEvent
   */
  void overlayMenuItem_actionPerformed(ActionEvent e) {
    JDXSource source = currentSelectedSource;
    if (source == null) {
      return;
    }
    if (! (source instanceof CompoundSource)) {
      return;
      // STATUS --> Can't overlay
    }
    try {
      closeSource(source);
      overlaySpectra(source);
    }
    catch (ScalesIncompatibleException ex) {
      writeStatus("Unable to Overlay, Scales are Incompatible");
      JOptionPane.showMessageDialog(this,
                                    "Unable to Overlay, Scales are Incompatible",
                                    "Overlay Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Overlays the spectra of the specified <code>JDXSource</code>
   * @param source the <code>JDXSource</code>
   * @throws ScalesIncompatibleException
   */
  private void overlaySpectra(JDXSource source) throws
      ScalesIncompatibleException {

    File file = getFileForSource(source);
    Vector specs = source.getSpectra();
    JSVPanel jsvp;
    jsvp = new JSVPanel(specs);
    jsvp.addMouseListener(new JSVPanelPopupListener(jsvpPopupMenu, jsvp, source));
    jsvp.setTitle( ( (CompoundSource) source).getTitle());

    setJSVPanelProperties(jsvp);

    JInternalFrame frame = new JInternalFrame(
        ( (CompoundSource) source).getTitle(),
        true, true, true, true);
    frame.setFrameIcon(imageIcon);
    frame.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
    frame.addInternalFrameListener(new JSVInternalFrameListener(file, source));
    frame.setMinimumSize(new Dimension(365, 200));
    frame.setPreferredSize(new Dimension(365, 200));
    frame.getContentPane().add(jsvp);
    desktopPane.add(frame);
    frame.setSize(550, 350);
    System.out.println("inside overlay");
    transAbsMenuItem.setEnabled(false);
    try {
      frame.setMaximum(true);
    }
    catch (PropertyVetoException pve) {}
    frame.show();
    createTree(file.getName(), new JInternalFrame[] {frame});
    validate();
    repaint();

    OverlayLegendDialog legend;
    if (autoShowLegend) {
      legend = new OverlayLegendDialog(this, jsvp);
    }

    overlaySplitButton.setIcon(splitIcon);
    overlaySplitButton.setToolTipText("Split Display");
  }

  /**
   * Closes the current JDXSource
   * @param e the ActionEvent
   */
  void closeMenuItem_actionPerformed(ActionEvent e) {
    closeSource(currentSelectedSource);
    removeSource(currentSelectedSource);

    if (jdxSources.size() < 1) {
      exportJcampMenu.setEnabled(false);
      exportAsMenu.setEnabled(false);
      closeMenuItem.setEnabled(false);
      closeAllMenuItem.setEnabled(false);
      displayMenu.setEnabled(false);
      windowMenu.setEnabled(false);
      processingMenu.setEnabled(false);
      printMenuItem.setEnabled(false);
      sourceMenuItem.setEnabled(false);
      errorLogMenuItem.setEnabled(false);
    }
  }

  /**
   * Close all <code>JDXSource<code>s
   * @param e the ActionEvent
   */
  void closeAllMenuItem_actionPerformed(ActionEvent e) {

    for (int i = 0; i < jdxSources.size(); i++) {
      JDXSource source = (JDXSource) jdxSources.elementAt(i);
      closeSource(source);
    }
    // add calls to disable Menus while files not open.

    exportJcampMenu.setEnabled(false);
    exportAsMenu.setEnabled(false);
    closeMenuItem.setEnabled(false);
    closeAllMenuItem.setEnabled(false);
    displayMenu.setEnabled(false);
    windowMenu.setEnabled(false);
    processingMenu.setEnabled(false);
    printMenuItem.setEnabled(false);
    sourceMenuItem.setEnabled(false);
    errorLogMenuItem.setEnabled(false);

    removeAllSources();

    closeMenuItem.setText("Close");
  }

  /**
   * Closes the <code>JDXSource</code> specified by source
   * @param source the <code>JDXSource</code>
   */
  public void closeSource(JDXSource source) {
    // Remove nodes and dispose of frames
    Enumeration enume = rootNode.children();
    SpecInfo nodeInfo;
    DefaultMutableTreeNode childNode;
    while (enume.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) enume.nextElement();
      String fileName = getFileNameForSource(source);
      if ( ( (String) node.getUserObject()).equals(fileName)) {
        for (Enumeration e = node.children(); e.hasMoreElements(); ) {
          childNode = (DefaultMutableTreeNode) e.nextElement();
          nodeInfo = (SpecInfo) childNode.getUserObject();
          nodeInfo.frame.dispose();
        }
        spectraTreeModel.removeNodeFromParent(node);
        break;
      }
    }

    Vector spectra = source.getSpectra();
    for (int i = 0; i < spectra.size(); i++) {
      String title = ( (Graph) spectra.elementAt(i)).getTitle();
      for (int j = 0; j < showMenu.getMenuComponentCount(); j++) {
        JMenuItem mi = (JMenuItem) showMenu.getMenuComponent(j);
        if (mi.getText().equals(title)) {
          showMenu.remove(mi);
        }
      }
    }

// TODO: need to check that any remaining file on display is still continuous
//

    exportJcampMenu.setEnabled(true);

    closeMenuItem.setText("Close");
    setTitle("JSpecView");
    // suggest now would be a good time for garbage collection
    System.gc();
  }

  /**
   * Displays the spectrum of the current <code>JDXSource</code> in separate windows
   * @param e the ActionEvent
   */
  void splitMenuItem_actionPerformed(ActionEvent e) {
    JDXSource source = currentSelectedSource;
    if (! (source instanceof CompoundSource)) {
      return;
      // STATUS --> Can't Split
    }

    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }

    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    if (jsvp.getNumberOfSpectra() == 1) {
      return;
    }

    closeSource(source);
    splitSpectra(source);
  }

  /**
   * Displays the spectrum of the <code>JDXSource</code> specified by source in
   * separate windows
   * @param source the <code>JDXSource</code>
   */
  private void splitSpectra(JDXSource source) {

    File file = getFileForSource(source);

    Vector specs = source.getSpectra();
    //JSVPanel[] panels = new JSVPanel[specs.size()];
    JInternalFrame[] frames = new JInternalFrame[specs.size()];
    JSVPanel jsvp;
    JInternalFrame frame;

    try {
      for (int i = 0; i < specs.size(); i++) {
        JDXSpectrum spec = (JDXSpectrum) specs.elementAt(i);
        jsvp = new JSVPanel(spec);
        jsvp.addMouseListener(new JSVPanelPopupListener(jsvpPopupMenu, jsvp,
            source));
        setJSVPanelProperties(jsvp);
        frame = new JInternalFrame(spec.getTitle(), true, true,
                                   true, true);
        frame.setFrameIcon(imageIcon);
        frame.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(365, 200));
        frame.setPreferredSize(new Dimension(365, 200));
        frame.getContentPane().add(jsvp);
        frame.addInternalFrameListener(new JSVInternalFrameListener(file,
            source));
        frames[i] = frame;

        if (autoATConversion.equals("AtoT")) {
          TAConvert(frame, TO_TRANS);
        }
        else if (autoATConversion.equals("TtoA")) {
          TAConvert(frame, TO_ABS);
        }

        if (autoIntegrate) {
          JSpecViewUtils.integrate(this, frame, false);
        }

        desktopPane.add(frame);
        frame.setVisible(true);
        frame.setSize(550, 350);
        try {
          frame.setMaximum(true);
        }
        catch (PropertyVetoException pve) {}
      }

      // arrange windows in ascending order
      for (int i = (specs.size() - 1); i >= 0; i--) {
        frames[i].toFront();
      }

      createTree(file.getName(), frames);

      overlaySplitButton.setIcon(overlayIcon);
      overlaySplitButton.setToolTipText("Overlay Display");
    }
    catch (JSpecViewException jsve) {
      //STATUS --> write message
    }
  }

  /**
   * Adds the <code>JDXSource</code> info specified by the <code>fileName<code> to
   * the tree model for the side panel.
   * @param fileName the name of the file
   * @param frames an array of JInternalFrames
   */
  public void createTree(String fileName, JInternalFrame[] frames) {
    DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(fileName);
    spectraTreeModel.insertNodeInto(fileNode, rootNode,
                                    fileNode.getChildCount());
    spectraTree.scrollPathToVisible(new TreePath(fileNode.getPath()));

    DefaultMutableTreeNode specNode;

    for (int i = 0; i < frames.length; i++) {
      specNode = new DefaultMutableTreeNode(
          new SpecInfo(frames[i]));

      spectraTreeModel.insertNodeInto(specNode, fileNode,
                                      fileNode.getChildCount());
      spectraTree.scrollPathToVisible(new TreePath(specNode.getPath()));
    }

  }

  /**
   * Class <code>SpecInfo</code> is Information for a node in the tree
   */
  private class SpecInfo {
    public JInternalFrame frame;

    /**
     * Initialises a <code>SpecInfo</code> with the a JInternalFrame
     * @param frame the JInternalFrame
     */
    public SpecInfo(JInternalFrame frame) {
      this.frame = frame;
    }

    /**
     * String representation of the class
     * @return the string representation
     */
    public String toString() {
      return frame.getTitle();
    }
  }

  /**
   * Toggles the grid
   * @param e the ItemEvent
   */
  void gridCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    if (e.getStateChange() == ItemEvent.SELECTED) {
      jsvp.setGridOn(true);
    }
    else {
      jsvp.setGridOn(false);
    }
    repaint();
  }

  /**
   * Shows the properties or header of a Spectrum
   * @param e the ActionEvent
   */
  void propertiesMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame != null) {
      JSVPanel panel = (JSVPanel) frame.getContentPane().getComponent(0);
      jsvpPopupMenu.setSelectedJSVPanel(panel);
      jsvpPopupMenu.setSource(currentSelectedSource);
      jsvpPopupMenu.properties_actionPerformed(e);
    }
  }

  /**
   * Listener for a JInternalFrame
   */
  private class JSVInternalFrameListener
      extends InternalFrameAdapter {

    File file;
    JDXSource source;

    /**
     * Initialises a <code>JSVInternalFrameListener</code>
     * @param file the name of the selected file
     * @param source current the JDXSource of the file
     */
    public JSVInternalFrameListener(File file, JDXSource source) {
      this.file = file;
      this.source = source;

    }

    /**
     * Gets the selected JSVPanel and updates menus and button according to the
     * panel's properties. Also sets the frame title to the current file name.
     * @param e the InternalFrameEvent
     */
    public void internalFrameActivated(InternalFrameEvent e) {
      String Yunits = "";
      JInternalFrame frame = e.getInternalFrame();
      // Set Current Selected Source
      currentSelectedSource = source;

      // Update the menu items for the display menu
      JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
      gridCheckBoxMenuItem.setSelected(jsvp.isGridOn());
      gridToggleButton.setSelected(jsvp.isGridOn());
      coordsCheckBoxMenuItem.setSelected(jsvp.isCoordinatesOn());
      coordsToggleButton.setSelected(jsvp.isCoordinatesOn());
      revPlotCheckBoxMenuItem.setSelected(jsvp.isPlotReversed());
      revPlotToggleButton.setSelected(jsvp.isPlotReversed());

      if (jsvp.getNumberOfSpectra() > 1) {
        overlaySplitButton.setIcon(splitIcon);
        overlaySplitButton.setToolTipText("Split Display");
        overlayKeyButton.setEnabled(true);
        overlayKeyMenuItem.setEnabled(true);
      }
      else {
        overlaySplitButton.setIcon(overlayIcon);
        overlaySplitButton.setToolTipText("Overlay Display");
        overlayKeyButton.setEnabled(false);
        overlayKeyMenuItem.setEnabled(false);
      }

//      jsvp.setZoomEnabled(true);
      // update availability of Exporting JCAMP-DX file so that
      // if a Peak Table is the current spectrum, disable the menu.
      continuous = currentSelectedSource.getJDXSpectrum(0).isContinuous();
      if (continuous) {
        exportJcampMenu.setEnabled(true);
      }
      else {
        exportJcampMenu.setEnabled(false);
      }

      Yunits = currentSelectedSource.getJDXSpectrum(0).getYUnits();
//        System.out.println(Yunits);

      if (continuous &&
          JSpecViewUtils.isHNMR(currentSelectedSource.getJDXSpectrum(0))) {
        integrateMenuItem.setEnabled(true);
      }
      else {
        integrateMenuItem.setEnabled(false);
      }
      //  Can only convert from T <-> A  if Absorbance or Transmittance and continuous
      if ( (continuous) &&
          (Yunits.toLowerCase().contains("abs") ||
           Yunits.toLowerCase().contains("trans"))) {
        transAbsMenuItem.setEnabled(true);
      }
      else {
        transAbsMenuItem.setEnabled(false);
      }

      // Update file|Close Menu
      closeMenuItem.setText("Close '" + file.getName() + "'");
      MainFrame.this.setTitle("JSpecView - " + file.getAbsolutePath());

      // Find Node in SpectraTree and select it
      DefaultMutableTreeNode node = getNodeForInternalFrame(frame, rootNode);
      if (node != null) {
        spectraTree.setSelectionPath(new TreePath(node.getPath()));
      }
    }

    /**
     * Called when <code>JInternalFrame</code> is closing
     * @param e the InternalFrameEvent
     */
    public void internalFrameClosing(InternalFrameEvent e) {
      final JInternalFrame frame = e.getInternalFrame();

      doInternalFrameClosing(frame);
    }

    /**
     * Called when <code>JInternalFrame</code> has opened
     * @param e the InternalFrameEvent
     */
    public void internalFrameOpened(InternalFrameEvent e) {

      spectraTree.validate();
      spectraTree.repaint();
    }

    /**
     * Returns the tree node that is associated with an internal frame
     * @param frame the JInternalFrame
     * @param parent the parent node
     * @return the tree node that is associated with an internal frame
     */
    public DefaultMutableTreeNode getNodeForInternalFrame(JInternalFrame frame,
        DefaultMutableTreeNode parent) {
      Enumeration enume = parent.breadthFirstEnumeration();

      while (enume.hasMoreElements()) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) enume.
            nextElement();
        if (node.isLeaf()) {
          Object nodeInfo = node.getUserObject();
          if (nodeInfo instanceof SpecInfo) {
            if ( ( (SpecInfo) nodeInfo).frame == frame) {
              return node;
            }
          }
        }
        else {
          continue;
        }
      }

      return null;
    }
  }

  /**
   * Tree Cell Renderer for the Spectra Tree
   */
  private class SpectraTreeCellRenderer
      extends DefaultTreeCellRenderer {
    Object value;
    boolean leaf;

    public SpectraTreeCellRenderer() {
    }

    public Component getTreeCellRendererComponent(
        JTree tree,
        Object value,
        boolean sel,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus) {

      super.getTreeCellRendererComponent(
          tree, value, sel,
          expanded, leaf, row,
          hasFocus);

      this.value = value;
      this.leaf = leaf;

      return this;
    }

    /**
     * Returns a font depending on whether a frame is hidden
     * @return the tree node that is associated with an internal frame
     */
    public Font getFont() {
      if (leaf && isFrameHidden(value)) {
        return new Font("Dialog", Font.ITALIC, 12);
      }
      return new Font("Dialog", Font.BOLD, 12);
    }

    protected boolean isFrameHidden(Object value) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
      Object nodeInfo = node.getUserObject();
      if (nodeInfo instanceof SpecInfo) {
        JInternalFrame frame = ( (SpecInfo) nodeInfo).frame;
        if (! ( (SpecInfo) nodeInfo).frame.isVisible()) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Shows a dialog with the message "Not Yet Implemented"
   */
  public void showNotImplementedOptionPane() {
    JOptionPane.showMessageDialog(this, "Not Yet Implemented",
                                  "Not Yet Implemented",
                                  JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Returns the name of the file associated with a JDXSource
   * @param source the JDXSource
   * @return the name of the file associated with a JDXSource
   */
  public String getFileNameForSource(JDXSource source) {
    File file = getFileForSource(source);
    if (file != null) {
      return file.getName();
    }
    return null;
  }

  /**
   * Returns the  file associated with a JDXSource
   * @param source the JDXSource
   * @return the file associated with a JDXSource
   */
  public File getFileForSource(JDXSource source) {
    int index = jdxSources.indexOf(source);
    if (index != -1) {
      return (File) jdxSourceFiles.elementAt(index);
    }

    return null;
  }

  /**
   * Returns the JDXSource associated with a file given by name fileName
   * @param fileName the name of the file
   * @return the JDXSource associated with a file given by name fileName
   */
  public JDXSource getSourceForFileName(String fileName) {
    int index = -1;
    for (int i = 0; i < jdxSourceFiles.size(); i++) {
      File file = (File) jdxSourceFiles.elementAt(i);
      if (file.getName().equals(fileName)) {
        index = i;
      }
    }
    if (index != -1) {
      return (JDXSource) jdxSources.elementAt(index);
    }

    return null;
  }

  /**
   * Removes a JDXSource object from the application
   * @param source the JDXSource
   */
  public void removeSource(JDXSource source) {
    File file = getFileForSource(source);
    jdxSourceFiles.removeElement(file);
    jdxSources.removeElement(source);
    currentSelectedSource = null;
  }

  /**
   * Removes all JDXSource objects from the application
   */
  public void removeAllSources() {
    jdxSourceFiles.clear();
    jdxSources.clear();
  }

  /**
   * Prints the current Spectrum display
   * @param e the ActionEvent
   */
  void printMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }

    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);

    PrintLayoutDialog ppd = new PrintLayoutDialog(this);
    PrintLayoutDialog.PrintLayout pl = ppd.getPrintLayout();

    if (pl != null) {
      jsvp.printSpectrum(pl);
    }
  }

  /**
   * Shows the source file contents
   * @param e the ActionEvent
   */
  void sourceMenuItem_actionPerformed(ActionEvent e) {
    File file;
    if (currentSelectedSource != null) {
      file = getFileForSource(currentSelectedSource);
      try {
        TextDialog source = new TextDialog(this, file.getAbsolutePath(), file, true);
      }
      catch (IOException ex) {
        TextDialog source = new TextDialog(this, "File Not Found",
                                           "File Not Found", true);
      }
    }
    else {
      if (jdxSources.size() > 0) {
        JOptionPane.showMessageDialog(this, "Please Select a Spectrum",
                                      "Select Spectrum",
                                      JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * Exits the application
   * @param e the ActionEvent
   */
  void exitMenuItem_actionPerformed(ActionEvent e) {
    int option = JOptionPane.showConfirmDialog(this, "Are you sure?", "Exit",
                                               JOptionPane.OK_CANCEL_OPTION,
                                               JOptionPane.QUESTION_MESSAGE);

    if (option == JOptionPane.OK_OPTION) {
      System.exit(0);
    }
  }

  /**
   * Shows the preferences dialog
   * @param e the ActionEvent
   */
  void preferencesMenuItem_actionPerformed(ActionEvent e) {
    PreferencesDialog pd = new PreferencesDialog(this, "Preferences", true,
                                                 properties, dsp);
    properties = pd.getPreferences();
    // Apply Properties where appropriate
    setApplicationProperties();
    setApplicationElements();

    JInternalFrame[] frames = desktopPane.getAllFrames();
    for (int i = 0; i < frames.length; i++) {
      JSVPanel jsvp = (JSVPanel) frames[i].getContentPane().getComponent(0);
      setJSVPanelProperties(jsvp);
    }
    TreeMap dispSchemes = dsp.getDisplaySchemes();
    if (defaultDisplaySchemeName.equals("Current")) {
      properties.setProperty("defaultDisplaySchemeName", tempDS);
    }
  }

  /**
   * Shows the Help | Contents dialog
   * @param e the ActionEvent
   */
  void contentsMenuItem_actionPerformed(ActionEvent e) {
    showNotImplementedOptionPane();
  }

  /**
   * Toggles the Coordinates
   * @param e the ItemEvent
   */
  void coordsCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    if (e.getStateChange() == ItemEvent.SELECTED) {
      jsvp.setCoordinatesOn(true);
    }
    else {
      jsvp.setCoordinatesOn(false);
    }
    repaint();
  }

  /**
   * Reverses the plot
   * @param e the ItemEvent
   */
  void revPlotCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    if (e.getStateChange() == ItemEvent.SELECTED) {
      jsvp.setReversePlot(true);
    }
    else {
      jsvp.setReversePlot(false);
    }
    repaint();
  }

  /**
   * Shows the next zoomed view
   * @param e the ActionEvent
   */
  void nextMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    jsvp.nextView();
  }

  /**
   * Shows the previous zoomed view
   * @param e the ActionEvent
   */
  void prevMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    jsvp.previousView();
  }

  /**
   * Shows the full spectrum
   * @param e the ActionEvent
   */
  void fullMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    jsvp.reset();
  }

  /**
   * Clears all zoom views
   * @param e the ActionEvent
   */
  void clearMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    jsvp.clearViews();
  }

  /**
   * Sets the status of the menuitems according to the properties of the current
   * selected JSVPanel
   * @param e the MenuEvent
   */
  void displayMenu_menuSelected(MenuEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    gridCheckBoxMenuItem.setSelected(jsvp.isGridOn());
    coordsCheckBoxMenuItem.setSelected(jsvp.isCoordinatesOn());
    revPlotCheckBoxMenuItem.setSelected(jsvp.isPlotReversed());
  }

  /**
   * Exports the spectrum in X,Y format
   * @param e the ActionEvent
   */
  void xyMenuItem_actionPerformed(ActionEvent e) {
    exportSpectrum(e.getActionCommand());
  }

  /**
   * Exports the spectrum in FIX format
   * @param e the ActionEvent
   */
  void fixMenuItem_actionPerformed(ActionEvent e) {
    exportSpectrum(e.getActionCommand());
  }

  /**
   * Exports the spectrum in SQZ format
   * @param e the ActionEvent
   */
  void sqzMenuItem_actionPerformed(ActionEvent e) {
    exportSpectrum(e.getActionCommand());
  }

  /**
   * Exports the spectrum in PAC format
   * @param e the ActionEvent
   */
  void pacMenuItem_actionPerformed(ActionEvent e) {
    exportSpectrum(e.getActionCommand());
  }

  /**
   * Exports the spectrum in DIF format
   * @param e the ActionEvent
   */
  void difMenuItem_actionPerformed(ActionEvent e) {
    exportSpectrum(e.getActionCommand());
  }

  /**
   * Export spectrum in a given format
   * @param command the name of the format to export in
   */
  private void exportSpectrum(String command) {
    final String comm = command;
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    final JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);

    if (fc != null) {
      if (JSpecViewUtils.DEBUG) {
        fc.setCurrentDirectory(new File("C:\\JCAMPDX"));
      }
      else if (useDirLastExported) {
        fc.setCurrentDirectory(new File(dirLastExported));
      }

      // if JSVPanel has more than one spectrum...Choose which one to export
      int numOfSpectra = jsvp.getNumberOfSpectra();
      if (numOfSpectra > 1 && (comm != "JPG" && comm != "PNG")) {
        String[] items = new String[numOfSpectra];
        for (int i = 0; i < numOfSpectra; i++) {
          JDXSpectrum spectrum = (JDXSpectrum) jsvp.getSpectrumAt(i);
          items[i] = spectrum.getTitle();
        }

        final JDialog dialog = new JDialog(this, "Choose Spectrum", true);
        dialog.setResizable(false);
        dialog.setSize(200, 100);
        dialog.setLocation( (getLocation().x + getSize().width) / 2,
                           (getLocation().y + getSize().height) / 2);
        final JComboBox cb = new JComboBox(items);
        Dimension d = new Dimension(120, 25);
        cb.setPreferredSize(d);
        cb.setMaximumSize(d);
        cb.setMinimumSize(d);
        JPanel panel = new JPanel(new FlowLayout());
        JButton button = new JButton("OK");
        panel.add(cb);
        panel.add(button);
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(new JLabel("Choose Spectrum to export",
                                               JLabel.CENTER),
                                    BorderLayout.NORTH);
        dialog.getContentPane().add(panel);
        button.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            int index = cb.getSelectedIndex();
            JDXSpectrum spec = (JDXSpectrum) jsvp.getSpectrumAt(index);
            dialog.dispose();
            exportSpectrum_aux(spec, jsvp, comm, index);
          }
        });
        dialog.setVisible(true);
      }
      else {
        JDXSpectrum spec = (JDXSpectrum) jsvp.getSpectrumAt(0);
        exportSpectrum_aux(spec, jsvp, comm, 0);
      }
    }
  }

  /**
   * Auxiliary Export method
   * @param spec the spectrum to export
   * @param jsvp the JSVPanel that displays the spectrum
   * @param comm the format to export in
   * @param index the index of the spectrum
   */
  private void exportSpectrum_aux(JDXSpectrum spec, JSVPanel jsvp, String comm,
                                  int index) {
    filter3.addExtension("xml");
    filter3.addExtension("aml");
    filter3.setDescription("AnIML Files");
    fc.setFileFilter(filter3);

    filter.addExtension("jdx");
    filter.addExtension("dx");
    filter.setDescription("JCAMP-DX Files");
    fc.setFileFilter(filter);

    int returnVal = fc.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      dirLastExported = file.getParent();

      int option = -1;
      int startIndex, endIndex;

      if (file.exists()) {
        option = JOptionPane.showConfirmDialog(this, "Overwrite file?",
                                               "Confirm Overwrite Existing File",
                                               JOptionPane.YES_NO_OPTION,
                                               JOptionPane.QUESTION_MESSAGE);
      }

      if (option != -1) {
        if (option == JOptionPane.NO_OPTION) {
          exportSpectrum_aux(spec, jsvp, comm, index);
          return;
        }
      }

      startIndex = jsvp.getStartDataPointIndices()[index];
      endIndex = jsvp.getEndDataPointIndices()[index];

      try {
        if (comm.equals("XY")) {
          JDXExporter.exportXY(spec, new FileWriter(file.getAbsolutePath()),
                               startIndex, endIndex);
        }
        else if (comm.equals("DIF")) {
          JDXExporter.exportDIF(spec, new FileWriter(file.getAbsolutePath()),
                                startIndex, endIndex);
        }
        else if (comm.equals("FIX")) {
          JDXExporter.exportFIX(spec, new FileWriter(file.getAbsolutePath()),
                                startIndex, endIndex);
        }
        else if (comm.equals("SQZ")) {
          JDXExporter.exportSQZ(spec, new FileWriter(file.getAbsolutePath()),
                                startIndex, endIndex);
        }
        else if (comm.equals("PAC")) {
          JDXExporter.exportPAC(spec, new FileWriter(file.getAbsolutePath()),
                                startIndex, endIndex);
        }
        else if (comm.equals("AnIML")) {
          AnIMLExporter.exportAsAnIML(spec, file.getAbsolutePath(), startIndex,
                                      endIndex);
        }
        else if (comm.equals("CML")) {
          CMLExporter.exportAsCML(spec, file.getAbsolutePath(), startIndex,
                                  endIndex);
        }
        else if (comm.equals("SVG")) {
          SVGExporter.exportAsSVG(file.getAbsolutePath(), jsvp, index, true);
        }
        else if (comm.equals("PNG")) {
          try {
            Rectangle r = jsvp.getBounds();
            Image image = jsvp.createImage(r.width, r.height);
            Graphics g = image.getGraphics();
            jsvp.paint(g);
            ImageIO.write( (RenderedImage) image, "png",
                          new File(file.getAbsolutePath()));
          }
          catch (IOException ioe) {
            ioe.printStackTrace();
          }
        }
        else if (comm.equals("JPG")) {
          try {
            Rectangle r = jsvp.getBounds();
            Image image = jsvp.createImage(r.width, r.height);
            Graphics g = image.getGraphics();
            jsvp.paint(g);
            ImageIO.write( (RenderedImage) image, "jpg",
                          new File(file.getAbsolutePath()));
          }
          catch (IOException ioe) {
            ioe.printStackTrace();
          }
        }
      }
      catch (IOException ioe) {
        // STATUS --> "Error writing: " + file.getName()
      }
    }
  }

  /**
   * Show dialog when there is an attempt to export overlaid spectra
   */
  private void showCannotExportOverlaidOptionPane() {
    JOptionPane.showMessageDialog(this, "Can't Export Overlaid Spectra.\n" +
                                  "Split Display then Export",
                                  "Can't Export",
                                  JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Writes a message to the status bar
   * @param msg the message
   */
  public void writeStatus(String msg) {
    statusLabel.setText(msg);
  }

  /**
   * Shows the Help | About Dialog
   * @param e the ActionEvent
   */
  void aboutMenuItem_actionPerformed(ActionEvent e) {
    //JOptionPane.showMessageDialog(MainFrame.this, "<html><img src=MainFrame.class.getClassLoader().getResource(\"icons/spec16.gif\")> JSpecView version</img> " + JSVApplet.APPLET_VERSION + aboutJSpec, "About JSpecView", JOptionPane.PLAIN_MESSAGE);
    AboutDialog ad = new AboutDialog(this);
  }

  /**
   * Hides the selected JInternalFrane
   * @param e the ActionEvent
   */
  void hideMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    try {
      if (frame != null) {
        frame.setVisible(false);
        frame.setSelected(false);
//        spectraTree.validate();
        spectraTree.repaint();
      }
    }
    catch (PropertyVetoException pve) {
    }
    //doInternalFrameClosing(frame);
  }

  /**
   * Hides all JInternalFranes
   * @param e the ActionEvent
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
    }
    catch (PropertyVetoException pve) {
    }
  }

  /**
   * Shows all JInternalFrames
   * @param e the ActionEvent
   */
  void showMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame[] frames = desktopPane.getAllFrames();
    try {
      for (int i = 0; i < frames.length; i++) {
        frames[i].setVisible(true);
      }
      frames[0].setSelected(true);
    }
    catch (PropertyVetoException pve) {
    }

    showMenu.removeAll();
  }

  /**
   * Does the necessary actions and cleaning up when  JInternalFrame closes
   * @param frame the JInternalFrame
   */
  private void doInternalFrameClosing(final JInternalFrame frame) {

    closeSource(currentSelectedSource);
    removeSource(currentSelectedSource);

    if (jdxSources.size() < 1) {
      exportJcampMenu.setEnabled(false);
      exportAsMenu.setEnabled(false);
      closeMenuItem.setEnabled(false);
      closeAllMenuItem.setEnabled(false);
      displayMenu.setEnabled(false);
      windowMenu.setEnabled(false);
      processingMenu.setEnabled(false);
      printMenuItem.setEnabled(false);
      sourceMenuItem.setEnabled(false);
      errorLogMenuItem.setEnabled(false);
    }

    /**
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        spectraTree.validate();
        spectraTree.repaint();

        // Add Title of internal frame to the Window|Show menu
        JMenuItem menuItem = new JMenuItem(frame.getTitle());
        showMenu.add(menuItem);
        menuItem.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e){
            frame.setVisible(true);
            frame.setSize(550, 350);
            try{
              frame.setSelected(true);
              frame.setMaximum(true);
            }
            catch(PropertyVetoException pve){}
            spectraTree.validate();
            spectraTree.repaint();
            showMenu.remove((JMenuItem)e.getSource());
          }
        });
     */

  }

  /**
   * Exports the spectrum in AnIML format
   * @param e the ActionEvent
   */
  void animlMenuItem_actionPerformed(ActionEvent e) {
    exportSpectrum(e.getActionCommand());
  }

  /**
   * Exports the current spectrum as SVG
   * @param e the ActionEvent
   */
  void svgMenuItem_actionPerformed(ActionEvent e) {
    exportSpectrum(e.getActionCommand());
  }

  /**
   * Exports spectrum as PNG image.
   * @param e the ActionEvent
   */
  void pngMenuItem_actionPerformed(ActionEvent e) {
    exportSpectrum(e.getActionCommand());
    //showNotImplementedOptionPane();
  }

  /**
   * Exports spectrum as JPG image.
   * @param e the ActionEvent
   */
  void jpgMenuItem_actionPerformed(ActionEvent e) {
    exportSpectrum(e.getActionCommand());
  }

  /**
   * Shows the File Open Dialog
   * @param e the ActionEvent
   */
  void openButton_actionPerformed(ActionEvent e) {
    showFileOpenDialog();
  }

  /**
   * Shows the print dialog
   * @param e the ActionEvent
   */
  void printButton_actionPerformed(ActionEvent e) {
    printMenuItem_actionPerformed(e);
  }

  /**
   * Toggles the grid
   * @param e the ActionEvent
   */
  void gridToggleButton_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    if ( ( (JToggleButton) e.getSource()).isSelected()) {
      jsvp.setGridOn(true);
    }
    else {
      jsvp.setGridOn(false);
    }
    repaint();
  }

  /**
   * Toggles the coordinates
   * @param e the the ActionEvent
   */
  void coordsToggleButton_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    if ( ( (JToggleButton) e.getSource()).isSelected()) {
      jsvp.setCoordinatesOn(true);
      coordinatesOn = true;
    }
    else {
      jsvp.setCoordinatesOn(false);
      coordinatesOn = false;
    }

    repaint();
  }

  /**
   * Reverses the plot
   * @param e the ActionEvent
   */
  void revPlotToggleButton_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    if ( ( (JToggleButton) e.getSource()).isSelected()) {
      jsvp.setReversePlot(true);
    }
    else {
      jsvp.setReversePlot(false);
    }
    repaint();
  }

  /**
   * Shows the previous zoomed view
   * @param e the ActionEvent
   */
  void previousButton_actionPerformed(ActionEvent e) {
    prevMenuItem_actionPerformed(e);
  }

  /**
   * Shows the next zoomed view
   * @param e the ActionEvent
   */
  void nextButton_actionPerformed(ActionEvent e) {
    nextMenuItem_actionPerformed(e);
  }

  /**
   * Shows the full view of the spectrum display
   * @param e the ActionEvent
   */
  void resetButton_actionPerformed(ActionEvent e) {
    fullMenuItem_actionPerformed(e);
  }

  /**
   * Clears the zoomed views
   * @param e the ActionEvent
   */
  void clearButton_actionPerformed(ActionEvent e) {
    clearMenuItem_actionPerformed(e);
  }

  /**
   * Shows the properties or header information for the current spectrum
   * @param e the ActionEvent
   */
  void propertiesButton_actionPerformed(ActionEvent e) {
    propertiesMenuItem_actionPerformed(e);
  }

  /**
   * Shows the About dialog
   * @param e the ActionEvent
   */
  void aboutButton_actionPerformed(ActionEvent e) {
    aboutMenuItem_actionPerformed(e);
  }

  /**
   * Split or overlays the spectra
   * @param e the ActionEvent
   */
  void overlaySplitButton_actionPerformed(ActionEvent e) {
    if ( ( (JButton) e.getSource()).getIcon() == overlayIcon) {
      overlayMenuItem_actionPerformed(e);
    }
    else {
      splitMenuItem_actionPerformed(e);
    }
  }

  /**
   * Shows or hides the toolbar
   * @param e the ItemEvent
   */
  void toolbarCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == e.SELECTED) {
      this.getContentPane().add(jsvToolBar, BorderLayout.NORTH);
    }
    else {
      this.getContentPane().remove(jsvToolBar);
    }
    this.validate();
  }

  /**
   * Shows or hides the sidePanel
   * @param e the ItemEvent
   */
  void sidePanelCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == e.SELECTED) {
      mainSplitPane.setDividerLocation(200);
    }
    else {
      mainSplitPane.setDividerLocation(0);
    }
  }

  /**
   * Shows or hides the status bar
   * @param e the ItemEvent
   */
  void statusCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == e.SELECTED) {
      this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
    }
    else {
      this.getContentPane().remove(statusPanel);
    }
    this.validate();
  }

  /**
   * Shows the legend or key for the overlayed spectra
   * @param e the ActionEvent
   */
  void overlayKeyMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    if (jsvp.getNumberOfSpectra() > 1) {
      legend = new OverlayLegendDialog(this, jsvp);
    }
  }

  /**
   * Shows the legend or key for the overlayed spectra
   * @param e the ActionEvent
   */
  void overlayKeyButton_actionPerformed(ActionEvent e) {
    overlayKeyMenuItem_actionPerformed(e);
  }

  /**
   * Allows Integration of an HNMR spectra
   * @param e the ActionEvent
   */
  void integrateMenuItem_actionPerformed(ActionEvent e) {
    JInternalFrame frame = desktopPane.getSelectedFrame();
    JSVPanel newJsvPanel = (JSVPanel) frame.getContentPane().getComponent(0);

    if (JSpecViewUtils.hasIntegration(newJsvPanel)) {
      newJsvPanel = JSpecViewUtils.removeIntegration(this, frame, true);
    }
    else {
      newJsvPanel = JSpecViewUtils.integrate(this, frame, true);
    }

    newJsvPanel.addMouseListener(new JSVPanelPopupListener(jsvpPopupMenu,
        newJsvPanel, currentSelectedSource));
    setJSVPanelProperties(newJsvPanel);
    validate();
  }

  /**
   * Allows Transmittance to Absorbance conversion and vice versa
   * @param e the ActionEvent
   */
  void transAbsMenuItem_actionPerformed(ActionEvent e) {

    JInternalFrame frame = desktopPane.getSelectedFrame();
    TAConvert(frame, IMPLIED);

  }

  /**
   * Predicts the colour of a solution containing the compound
   * @param e the ActionEvent
   */
  void solColMenuItem_actionPerformed(ActionEvent e) {

    JInternalFrame frame = desktopPane.getSelectedFrame();
    Container contentPane = frame.getContentPane();
    if (frame != null) {
      //JSVPanel panel = (JSVPanel) frame.getContentPane().getComponent(0);
      JSVPanel jsvp = (JSVPanel) contentPane.getComponent(0);
      JDXSpectrum spectrum = (JDXSpectrum) jsvp.getSpectrumAt(0);
      //jsvpPopupMenu.setSelectedJSVPanel(panel);
      //jsvpPopupMenu.setSource(currentSelectedSource);
      //jsvpPopupMenu.properties_actionPerformed(e);
      //Coordinate[] source;
      //source = currentSelectedSource.getJDXSpectrum(0).getXYCoords();
      //JDXSpectrum spectrum = (JDXSpectrum)selectedJSVPanel.getSpectrumAt(0);
      sltnclr = Visible.Colour(spectrum.getXYCoords());
      JOptionPane.showMessageDialog(this, "<HTML><body bgcolor=rgb("+sltnclr+")><br />Predicted Solution Colour RGB("+sltnclr+")<br /><br /></body></HTML>"
                                    , "Predicted Colour",
                                    JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * Allows Transmittance to Absorbance conversion or vice versa
   * depending on the value of comm.
   * @param frame the selected JInternalFrame
   * @param comm the conversion command
   */
  private void TAConvert(JInternalFrame frame, int comm) {
    if (frame == null) {
      return;
    }

    Container contentPane = frame.getContentPane();
    if (contentPane.getComponentCount() == 2) {
      frame.remove(contentPane.getComponent(0));
      JSVPanel jsvp = (JSVPanel) contentPane.getComponent(0);
      jsvp.reset();
      validate();
      return;
    }

    JSVPanel jsvp = (JSVPanel) contentPane.getComponent(0);
    if (jsvp.getNumberOfSpectra() > 1) {
      return;
    }

    JDXSpectrum spectrum = (JDXSpectrum) jsvp.getSpectrumAt(0);
    JDXSpectrum newSpec;
    try {
      switch (comm) {
        case TO_TRANS:
          newSpec = TransmittanceAbsorbanceConverter.AbsorbancetoTransmittance(
              spectrum);
          break;
        case TO_ABS:
          newSpec = TransmittanceAbsorbanceConverter.TransmittanceToAbsorbance(
              spectrum);
          break;
        case IMPLIED:
          newSpec = TransmittanceAbsorbanceConverter.convert(spectrum);
          break;
        default:
          newSpec = null;
      }
      if (newSpec == null) {
        return;
      }
      JSVPanel newJsvp = new JSVPanel(newSpec);
      newJsvp.setOverlayIncreasing(spectrum.isIncreasing());
      newJsvp.addMouseListener(new JSVPanelPopupListener(jsvpPopupMenu, newJsvp,
          currentSelectedSource));
      setJSVPanelProperties(newJsvp);

      // Get from properties variable
      contentPane.remove(jsvp);
      contentPane.invalidate();
      if (! (contentPane.getLayout() instanceof CardLayout)) {
        contentPane.setLayout(new CardLayout());
      }
      contentPane.add(newJsvp, "new");
      contentPane.add(jsvp, "old");
      validate();
    }
    catch (JSpecViewException ex) {
    }
  }

  /**
   * Shows the log of error in the source file
   * @param e the ActionEvent
   */
  void errorLogMenuItem_actionPerformed(ActionEvent e) {
      if(currentSelectedSource==null){
        JOptionPane.showMessageDialog(null, "Please Select a Spectrum",
                                      "Select Spectrum",
                                      JOptionPane.WARNING_MESSAGE);
             return;}

    System.out.println(currentSelectedSource.getErrorLog().length());
      if (currentSelectedSource.getErrorLog().length()>0) {
          String errorLog = currentSelectedSource.getErrorLog();
          File file = getFileForSource(currentSelectedSource);
          TextDialog source = new TextDialog(this, file.getAbsolutePath(), errorLog, true);
    }
/*    else {
      if (jdxSources.size() > 0) {
        JOptionPane.showMessageDialog(this, "Please Select a Spectrum",
                                      "Select Spectrum",
                                      JOptionPane.WARNING_MESSAGE);
      }
    }*/
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
  public void drop(DropTargetDropEvent dtde) {
    try {
      Transferable t = dtde.getTransferable();

      if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        java.util.List list = (java.util.List) t.getTransferData(DataFlavor.
            javaFileListFlavor);
        dtde.getDropTargetContext().dropComplete(true);

        for (int i = 0; i < list.size(); i++) {
          openFile( (File) list.toArray()[i]);
        }
      }
      else {
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

}
