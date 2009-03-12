/* Copyright (C) 2002-2009  The JSpecView Development Team
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
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

import jspecview.common.Coordinate;
import jspecview.common.Graph;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVPanel;
import jspecview.common.JSVPanelPopupListener;
import jspecview.common.JSVPanelPopupMenu;
import jspecview.common.JSpecViewFileFilter;
import jspecview.common.JSpecViewUtils;
import jspecview.common.OverlayLegendDialog;
import jspecview.common.PrintLayoutDialog;
import jspecview.common.TransmittanceAbsorbanceConverter;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.source.CompoundSource;
import jspecview.source.JDXSource;
import mdidesktop.ScrollableDesktopPane;
import mdidesktop.WindowMenu;
import jspecview.common.Visible;

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
  String recentFileName;

  boolean autoIntegrate;

  boolean AtoTSeparateWindow;
  String autoATConversion;

//   ------------------------ Display Properties -------------------------
  Color bgc= Color.WHITE;
  boolean gridOn;
  boolean coordinatesOn;
  boolean scaleOn;
  boolean obscure;
  //  ----------------------- Application Attributes ---------------------

  Vector<JDXSource> jdxSources = new Vector<JDXSource>();
  Vector<File> jdxSourceFiles = new Vector<File>();
  int numRecent = 10;
  Vector<String> recentFilePaths = new Vector<String>(numRecent);
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
  JMenuBar menuBar = new JMenuBar();
  JMenu fileMenu = new JMenu();
  JMenuItem openMenuItem = new JMenuItem();
  JMenuItem printMenuItem = new JMenuItem();
  JMenuItem closeMenuItem = new JMenuItem();
  JMenuItem closeAllMenuItem = new JMenuItem();
  JMenu saveAsMenu = new JMenu();
  JMenu saveAsJDXMenu = new JMenu();
  JMenu exportAsMenu = new JMenu();
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
  JCheckBoxMenuItem toolbarCheckBoxMenuItem = new JCheckBoxMenuItem();
  JCheckBoxMenuItem sidePanelCheckBoxMenuItem = new JCheckBoxMenuItem();
  JCheckBoxMenuItem statusCheckBoxMenuItem = new JCheckBoxMenuItem();
  BorderLayout mainborderLayout = new BorderLayout();
  JSplitPane mainSplitPane = new JSplitPane();
  JSplitPane sideSplitPane = new JSplitPane();


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
  JMenuItem overlayKeyMenuItem = new JMenuItem();
  JButton overlayKeyButton = new JButton();
  OverlayLegendDialog legend;
  JMenu processingMenu = new JMenu();
  JMenuItem integrateMenuItem = new JMenuItem();
  JMenuItem transAbsMenuItem = new JMenuItem();
  JMenuItem solColMenuItem = new JMenuItem();
  JMenuItem errorLogMenuItem = new JMenuItem();

  //private String aboutJSpec = "\nJSpecView is a graphical viewer for JCAMP-DX Spectra\nCopyright (c) 2008\nUniversity of the West Indies, Mona ";

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

    getIcons();
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
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    // When application exits ...
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent we) {
        try {
          onProgramExit();
        }
        catch (Exception e) {
        }

        int option;
        if (showExitDialog) {
          option = JOptionPane.showConfirmDialog(MainFrame.this,
                                                 "Exit JSpecView? ", "Exit",
                                                 JOptionPane.YES_NO_OPTION,
                                                 JOptionPane.QUESTION_MESSAGE);

          if (option == JOptionPane.YES_OPTION) {
            System.exit(0);
          }
        }
        else {
          System.exit(0);
        }
      }
    });

  }

  Image icon;
  ImageIcon frameIcon;
  ImageIcon openIcon;
  ImageIcon printIcon;
  ImageIcon gridIcon;
  ImageIcon coordinatesIcon;
  ImageIcon reverseIcon;
  ImageIcon previousIcon;
  ImageIcon nextIcon;
  ImageIcon resetIcon;
  ImageIcon clearIcon;
  ImageIcon informationIcon;
  ImageIcon aboutIcon;
  ImageIcon overlayIcon;
  ImageIcon splitIcon;
  ImageIcon overlayKeyIcon;

  private void getIcons() {
    Class cl = getClass();
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

    //boolean loadedOk;
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
    String fname = new File("displaySchemes.xml").getAbsolutePath();
    try {
      MainFrame.class.getClassLoader().getResource("displaySchemes.xml");
      try {
        MainFrame.class.getResourceAsStream("displaySchemes.xml");
      }
      catch (Exception jEX) {
        System.err.println("could not find displayschemes?");
      }
      dsp.load(fname);
      System.out.println("Display scheme loaded from " + fname);
    }
    catch (Exception ex) {
      dsp.loadDefault("missingDS.xml");
      System.err.println("Warning, display scheme file "  + fname + " was not found or not properly loaded -- using Default settings");
    }

    setApplicationProperties();
    tempDS = defaultDisplaySchemeName;
    fc = (JSpecViewUtils.DEBUG ? new JFileChooser("C:/temp")
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
/*  private Color getColorProperty(String key, Color def) {
    String param = properties.getProperty(key);
    Color color = JSpecViewUtils.getColorFromString(param);
    return (color == null ? def : color);
  }
*/
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
    setIconImage(icon);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setJMenuBar(menuBar);
    setTitle("JSpecView");
    getContentPane().setLayout(mainborderLayout);
    fileMenu.setMnemonic('F');
    fileMenu.setText("File");
    openMenuItem.setActionCommand("Open");
    openMenuItem.setMnemonic('O');
    openMenuItem.setText("Open...");
    openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(79,
        InputEvent.CTRL_MASK, false));
    openMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        open_actionPerformed(e);
      }
    });
    printMenuItem.setMnemonic('P');
    printMenuItem.setText("Print...");
    printMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(80,
        InputEvent.CTRL_MASK, false));
    printMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        printMenuItem_actionPerformed(e);
      }
    });
    closeMenuItem.setMnemonic('C');
    closeMenuItem.setText("Close");
    closeMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(115,
        InputEvent.CTRL_MASK, false));
    closeMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeMenuItem_actionPerformed(e);
      }
    });
    closeAllMenuItem.setMnemonic('L');
    closeAllMenuItem.setText("Close All");
    closeAllMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeAllMenuItem_actionPerformed(e);
      }
    });
    exitMenuItem.setMnemonic('X');
    exitMenuItem.setText("Exit");
    exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(115,
        InputEvent.ALT_MASK, false));
    exitMenuItem.addActionListener(new ActionListener() {
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
    gridCheckBoxMenuItem.setMnemonic('G');
    gridCheckBoxMenuItem.setText("Grid");
    gridCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(71,
        InputEvent.CTRL_MASK, false));
    gridCheckBoxMenuItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        gridCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    coordsCheckBoxMenuItem.setMnemonic('C');
    coordsCheckBoxMenuItem.setText("Coordinates");
    coordsCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(67,
        InputEvent.CTRL_MASK, false));
    coordsCheckBoxMenuItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        coordsCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    revPlotCheckBoxMenuItem.setMnemonic('R');
    revPlotCheckBoxMenuItem.setText("Reverse Plot");
    revPlotCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
        82, InputEvent.CTRL_MASK, false));
    revPlotCheckBoxMenuItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        revPlotCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    nextMenuItem.setMnemonic('N');
    nextMenuItem.setText("Next View");
    nextMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(78,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, false));
    nextMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        nextMenuItem_actionPerformed(e);
      }
    });
    prevMenuItem.setMnemonic('P');
    prevMenuItem.setText("Previous View");
    prevMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(80,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, false));
    prevMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        prevMenuItem_actionPerformed(e);
      }
    });
    fullMenuItem.setMnemonic('F');
    fullMenuItem.setText("Full View");
    fullMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(70,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, false));
    fullMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fullMenuItem_actionPerformed(e);
      }
    });
    clearMenuItem.setMnemonic('C');
    clearMenuItem.setText("Clear Views");
    clearMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(67,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, false));
    clearMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearMenuItem_actionPerformed(e);
      }
    });
    preferencesMenuItem.setActionCommand("Preferences");
    preferencesMenuItem.setMnemonic('P');
    preferencesMenuItem.setText("Preferences...");
    preferencesMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(80,
        InputEvent.SHIFT_MASK, false));
    preferencesMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        preferencesMenuItem_actionPerformed(e);
      }
    });
    contentsMenuItem.setActionCommand("Contents");
    contentsMenuItem.setMnemonic('C');
    contentsMenuItem.setText("Contents...");
    contentsMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(112, 0, false));
    contentsMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        contentsMenuItem_actionPerformed(e);
      }
    });
    aboutMenuItem.setMnemonic('A');
    aboutMenuItem.setText("About");
    aboutMenuItem.addActionListener(new ActionListener() {
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

    toolbarCheckBoxMenuItem.setMnemonic('T');
    toolbarCheckBoxMenuItem.setSelected(true);
    toolbarCheckBoxMenuItem.setText("Toolbar");
    toolbarCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
        84,
        InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, false));
    toolbarCheckBoxMenuItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        toolbarCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    sidePanelCheckBoxMenuItem.setMnemonic('S');
    sidePanelCheckBoxMenuItem.setSelected(true);
    sidePanelCheckBoxMenuItem.setText("Side Panel");
    sidePanelCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
        83,
        InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, false));
    sidePanelCheckBoxMenuItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        sidePanelCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    statusCheckBoxMenuItem.setMnemonic('B');
    statusCheckBoxMenuItem.setSelected(true);
    statusCheckBoxMenuItem.setText("Status Bar");
    statusCheckBoxMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(66,
        InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, false));
    statusCheckBoxMenuItem.addItemListener(new ItemListener() {
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
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, false));
    splitMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        splitMenuItem_actionPerformed(e);
      }
    });
    overlayMenuItem.setMnemonic('O');
    overlayMenuItem.setText("Overlay");
    overlayMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(79,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, false));
    overlayMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        overlayMenuItem_actionPerformed(e);
      }
    });
    hideMenuItem.setMnemonic('H');
    hideMenuItem.setText("Hide");
    hideMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        hideMenuItem_actionPerformed(e);
      }
    });
    hideAllMenuItem.setMnemonic('L');
    hideAllMenuItem.setText("Hide All");
    hideAllMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        hideAllMenuItem_actionPerformed(e);
      }
    });
    showMenuItem.setMnemonic('S');
    showMenuItem.setText("Show All");
//    showAllMenuItem.setMnemonic('A');
//    showAllMenuItem.setText("Show All");
    showMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showMenuItem_actionPerformed(e);
      }
    });
    sourceMenuItem.setActionCommand("Source  ");
    sourceMenuItem.setMnemonic('S');
    sourceMenuItem.setText("Source ...");
    sourceMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(83,
        InputEvent.CTRL_MASK, false));
    sourceMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        sourceMenuItem_actionPerformed(e);
      }
    });
    propertiesMenuItem.setMnemonic('P');
    propertiesMenuItem.setText("Properties");
    propertiesMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(72,
        InputEvent.CTRL_MASK, false));
    propertiesMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        propertiesMenuItem_actionPerformed(e);
      }
    });
    mainSplitPane.setOneTouchExpandable(true);
    borderLayout1.setHgap(2);
    borderLayout1.setVgap(2);

    clearButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearButton_actionPerformed(e);
      }
    });
    previousButton.setBorder(null);
    previousButton.setToolTipText("Previous View");
    previousButton.setIcon(previousIcon);
    previousButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        previousButton_actionPerformed(e);
      }
    });
    nextButton.setBorder(null);
    nextButton.setToolTipText("Next View");
    nextButton.setIcon(nextIcon);
    nextButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        nextButton_actionPerformed(e);
      }
    });
    resetButton.setBorder(null);
    resetButton.setToolTipText("Reset ");
    resetButton.setIcon(resetIcon);
    resetButton.addActionListener(new ActionListener() {
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
    openButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        openButton_actionPerformed(e);
      }
    });
    propertiesButton.setBorder(null);
    propertiesButton.setToolTipText("Properties");
    propertiesButton.setIcon(informationIcon);
    propertiesButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        propertiesButton_actionPerformed(e);
      }
    });
    gridToggleButton.setBorder(null);
    gridToggleButton.setToolTipText("Toggle Grid");
    gridToggleButton.setIcon(gridIcon);
    gridToggleButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        gridToggleButton_actionPerformed(e);
      }
    });
    coordsToggleButton.setBorder(null);
    coordsToggleButton.setToolTipText("Toggle Coordinates");
    coordsToggleButton.setIcon(coordinatesIcon);
    coordsToggleButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        coordsToggleButton_actionPerformed(e);
      }
    });
    printButton.setBorder(null);
    printButton.setToolTipText("Print");
    printButton.setIcon(printIcon);
    printButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        printButton_actionPerformed(e);
      }
    });
    revPlotToggleButton.setBorder(null);
    revPlotToggleButton.setToolTipText("Reverse Plot");
    revPlotToggleButton.setIcon(reverseIcon);
    revPlotToggleButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        revPlotToggleButton_actionPerformed(e);
      }
    });
    aboutButton.setBorder(null);
    aboutButton.setToolTipText("About JSpecView");
    aboutButton.setIcon(aboutIcon);
    aboutButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        aboutButton_actionPerformed(e);
      }
    });
    overlaySplitButton.setBorder(null);
    overlaySplitButton.setIcon(overlayIcon);
    overlaySplitButton.setToolTipText("Overlay Display");
    overlaySplitButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        overlaySplitButton_actionPerformed(e);
      }
    });
    overlayKeyMenuItem.setEnabled(false);
    overlayKeyMenuItem.setText("Overlay Key");
    overlayKeyMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        overlayKeyMenuItem_actionPerformed(e);
      }
    });
    overlayKeyButton.setEnabled(false);
    overlayKeyButton.setBorder(null);
    overlayKeyButton.setToolTipText("Display Key for Overlaid Spectra");
    overlayKeyButton.setIcon(overlayKeyIcon);
    overlayKeyButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        overlayKeyButton_actionPerformed(e);
      }
    });
    processingMenu.setMnemonic('P');
    processingMenu.setText("Processing");
    integrateMenuItem.setMnemonic('I');
    integrateMenuItem.setText("Integrate HNMR");
    integrateMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        integrateMenuItem_actionPerformed(e);
      }
    });
    transAbsMenuItem.setText("Transmittance/Absorbance");
    transAbsMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        transAbsMenuItem_actionPerformed(e);
      }
    });
    solColMenuItem.setText("Predicted Solution Colour");
    solColMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        solColMenuItem_actionPerformed(e);
      }
    });
    errorLogMenuItem.setText("Error Log ...");
    errorLogMenuItem.addActionListener(new ActionListener() {
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
    JSVPanel.setMenus(saveAsMenu, saveAsJDXMenu, exportAsMenu, actionListener);
    //getContentPane().add(toolBar, BorderLayout.NORTH);
    getContentPane().add(statusPanel, BorderLayout.SOUTH);
    statusPanel.add(statusLabel, BorderLayout.SOUTH);
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

  private ActionListener actionListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      exportSpectrum(e.getActionCommand());
    }
  };

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
    writeStatus(" ");
    String fileName = recentFileName = file.getName();
    String filePath = file.getAbsolutePath();
    if (jdxSourceFiles.contains(file)) {
      writeStatus("File: '" + filePath + "' is already opened");
      return;
    }
    Object source = source = JDXSource.createJDXSource(null, filePath, null);
    if (source instanceof String) {
      writeStatus((String) source);
      return;
    }
    currentSelectedSource = (JDXSource) source;
    jdxSources.addElement(currentSelectedSource);
    jdxSourceFiles.addElement(file);
    closeMenuItem.setEnabled(true);
    closeMenuItem.setText("Close '" + fileName + "'");
    setTitle("JSpecView - " + file.getAbsolutePath());

    // add calls to enable Menus that were greyed out until a file is opened.

    // if current spectrum is not a Peak Table then enable Menu to re-export

    closeAllMenuItem.setEnabled(true);
    displayMenu.setEnabled(true);
    windowMenu.setEnabled(true);
    processingMenu.setEnabled(true);
    printMenuItem.setEnabled(true);
    sourceMenuItem.setEnabled(true);
    errorLogMenuItem.setEnabled(true);

    JDXSpectrum spec = currentSelectedSource.getJDXSpectrum(0);
    if (spec == null) {
      return;
    }

    setMenuEnables(spec);

    if (autoOverlay && source instanceof CompoundSource) {
      try {
        overlaySpectra(currentSelectedSource);
      } catch (ScalesIncompatibleException ex) {
        splitSpectra(currentSelectedSource);
      }
    } else {
      splitSpectra(currentSelectedSource);
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
  }

  void setMenuEnables(JDXSpectrum spec) {

    if (spec == null) {
      saveAsMenu.setEnabled(false);
      exportAsMenu.setEnabled(false);
      closeMenuItem.setEnabled(false);
      closeAllMenuItem.setEnabled(false);
      displayMenu.setEnabled(false);
      windowMenu.setEnabled(false);
      processingMenu.setEnabled(false);
      printMenuItem.setEnabled(false);
      sourceMenuItem.setEnabled(false);
      errorLogMenuItem.setEnabled(false);
      return;
    }

    saveAsMenu.setEnabled(true);
    exportAsMenu.setEnabled(true);

    //    jsvp.setZoomEnabled(true);
    // update availability of Exporting JCAMP-DX file so that
    // if a Peak Table is the current spectrum, disable the menu.
    boolean continuous = spec.isContinuous();
    saveAsJDXMenu.setEnabled(continuous);
    integrateMenuItem.setEnabled(JSpecViewUtils.isHNMR(spec) && continuous);
    //  Can only convert from T <-> A  if Absorbance or Transmittance and continuous
    boolean isAbsTrans = (spec.isAbsorbance() || spec.isTransmittance());
    transAbsMenuItem.setEnabled(continuous && isAbsTrans);
    Coordinate xyCoords[] = spec.getXYCoords();
    String Xunits = spec.getXUnits().toLowerCase();
    solColMenuItem.setEnabled(isAbsTrans
        && (Xunits.equals("nanometers") || Xunits.equals("nm"))
        && xyCoords[0].getXVal() < 401
        && xyCoords[(xyCoords.length - 1)].getXVal() > 699);
  }

  /**
   * Checks to see if this is an XML file
   * @param file
   * @return true if anIML or CML
   */

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
    //Color tmpcolour;
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
    Vector<JDXSpectrum> specs = source.getSpectra();
    JSVPanel jsvp;
    jsvp = new JSVPanel(specs);
    jsvp.addMouseListener(new JSVPanelPopupListener(jsvpPopupMenu, jsvp, source));
    jsvp.setTitle( ( (CompoundSource) source).getTitle());

    setJSVPanelProperties(jsvp);

    JInternalFrame frame = new JInternalFrame(
        ( (CompoundSource) source).getTitle(),
        true, true, true, true);
    frame.setFrameIcon(frameIcon);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.addInternalFrameListener(new JSVInternalFrameListener(file, source));
    frame.setMinimumSize(new Dimension(365, 200));
    frame.setPreferredSize(new Dimension(365, 200));
    frame.getContentPane().add(jsvp);
    desktopPane.add(frame);
    frame.setSize(550, 350);
    System.out.println("inside overlay");
    transAbsMenuItem.setEnabled(false);
    solColMenuItem.setEnabled(false);
    try {
      frame.setMaximum(true);
    }
    catch (PropertyVetoException pve) {}
    frame.show();
    createTree(file.getName(), new JInternalFrame[] {frame});
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
   * Closes the current JDXSource
   * @param e the ActionEvent
   */
  void closeMenuItem_actionPerformed(ActionEvent e) {
    closeSource(currentSelectedSource);
    removeSource(currentSelectedSource);

    if (jdxSources.size() < 1)
      setMenuEnables(null);
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

    setMenuEnables(null);

    removeAllSources();

    closeMenuItem.setText("Close");
  }

  /**
   * Closes the <code>JDXSource</code> specified by source
   * @param source the <code>JDXSource</code>
   */
  @SuppressWarnings("unchecked")
  public void closeSource(JDXSource source) {
    // Remove nodes and dispose of frames
    Enumeration enume = rootNode.children();
    SpecInfo nodeInfo;
    DefaultMutableTreeNode childNode;
    while (enume.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) enume
          .nextElement();
      String fileName = getFileNameForSource(source);
      if (((String) node.getUserObject()).equals(fileName)) {
        for (Enumeration e = node.children(); e.hasMoreElements();) {
          childNode = (DefaultMutableTreeNode) e.nextElement();
          nodeInfo = (SpecInfo) childNode.getUserObject();
          nodeInfo.frame.dispose();
        }
        spectraTreeModel.removeNodeFromParent(node);
        break;
      }
    }

    if (source != null) {
      Vector<JDXSpectrum> spectra = source.getSpectra();
      for (int i = 0; i < spectra.size(); i++) {
        String title = ((Graph) spectra.elementAt(i)).getTitle();
        for (int j = 0; j < showMenu.getMenuComponentCount(); j++) {
          JMenuItem mi = (JMenuItem) showMenu.getMenuComponent(j);
          if (mi.getText().equals(title)) {
            showMenu.remove(mi);
          }
        }
      }
      saveAsJDXMenu.setEnabled(true);
      saveAsMenu.setEnabled(true);
    }

    // TODO: need to check that any remaining file on display is still continuous
    //

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

    Vector<JDXSpectrum> specs = source.getSpectra();
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
        frame.setFrameIcon(frameIcon);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
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
    @Override
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
      currentSelectedSource = source;
    }

    /**
     * Gets the selected JSVPanel and updates menus and button according to the
     * panel's properties. Also sets the frame title to the current file name.
     * @param e the InternalFrameEvent
     */
    @Override
    public void internalFrameActivated(InternalFrameEvent e) {
      JInternalFrame frame = e.getInternalFrame();
      JDXSpectrum spec = currentSelectedSource.getJDXSpectrum(0);

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

      setMenuEnables(spec);

      // Update file|Close Menu
      closeMenuItem.setText("Close '" + file.getName() + "'");
      setTitle("JSpecView - " + file.getAbsolutePath());

      // Find Node in SpectraTree and select it
      DefaultMutableTreeNode node = getNodeForInternalFrame(frame, rootNode);
      if (node != null)
        spectraTree.setSelectionPath(new TreePath(node.getPath()));
    }

    /**
     * Called when <code>JInternalFrame</code> is closing
     * @param e the InternalFrameEvent
     */
    @Override
    public void internalFrameClosing(InternalFrameEvent e) {
      final JInternalFrame frame = e.getInternalFrame();

      doInternalFrameClosing(frame);
    }

    /**
     * Called when <code>JInternalFrame</code> has opened
     * @param e the InternalFrameEvent
     */
    @Override
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
    @SuppressWarnings("unchecked")
    public DefaultMutableTreeNode getNodeForInternalFrame(JInternalFrame frame,
        DefaultMutableTreeNode parent) {
      Enumeration enume = parent.breadthFirstEnumeration();

      while (enume.hasMoreElements()) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) enume.nextElement();
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

    @Override
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
    @Override
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
        //JInternalFrame frame = ( (SpecInfo) nodeInfo).frame;
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
        new TextDialog(this, file.getAbsolutePath(), file, true);
      }
      catch (IOException ex) {
        new TextDialog(this, "File Not Found",
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
    System.exit(0);
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
    dsp.getDisplaySchemes();
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
   * Export spectrum in a given format
   * @param command the name of the format to export in
   */
  void exportSpectrum(String command) {
    final String type = command;
    JInternalFrame frame = desktopPane.getSelectedFrame();
    if (frame == null) {
      return;
    }
    final JSVPanel jsvp = (JSVPanel) frame.getContentPane().getComponent(0);
    if (fc == null)
      return;

    if (JSpecViewUtils.DEBUG) {
      fc.setCurrentDirectory(new File("C:\\JCAMPDX"));
    } else if (useDirLastExported) {
      fc.setCurrentDirectory(new File(dirLastExported));
    }

    dirLastExported = jsvp.exportSpectra(this, fc, type, recentFileName, dirLastExported);

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
    new AboutDialog(this);
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
  void doInternalFrameClosing(final JInternalFrame frame) {

    closeSource(currentSelectedSource);
    removeSource(currentSelectedSource);

    if (jdxSources.size() < 1) {
      saveAsMenu.setEnabled(false);
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
    if (e.getStateChange() == ItemEvent.SELECTED) {
      getContentPane().add(jsvToolBar, BorderLayout.NORTH);
    }
    else {
      getContentPane().remove(jsvToolBar);
    }
    validate();
  }

  /**
   * Shows or hides the sidePanel
   * @param e the ItemEvent
   */
  void sidePanelCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
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
    if (e.getStateChange() == ItemEvent.SELECTED) {
      getContentPane().add(statusPanel, BorderLayout.SOUTH);
    }
    else {
      getContentPane().remove(statusPanel);
    }
    validate();
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
      Object errMsg = JSpecViewUtils.removeIntegration(frame.getContentPane());
      if (errMsg != null) {
        writeStatus((String) errMsg);
      } else {
        newJsvPanel = (JSVPanel)frame.getContentPane().getComponent(0);
      }
    } else {
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
    String Yunits = currentSelectedSource.getJDXSpectrum(0).getYUnits();
    String Yunits0 = Yunits;

    JInternalFrame frame = desktopPane.getSelectedFrame();
    Container contentPane = frame.getContentPane();


    if (frame != null) {
      //JSVPanel panel = (JSVPanel) frame.getContentPane().getComponent(0);
      JSVPanel jsvp = (JSVPanel) contentPane.getComponent(0);
      int numcomp=contentPane.getComponentCount();
       if ( (numcomp> 1)& Yunits.toLowerCase().contains("trans")) {
         Yunits0 = "abs";
       }
       if ( (numcomp> 1)& Yunits.toLowerCase().contains("abs")) {
         Yunits0 = "trans";
       }
      JDXSpectrum spectrum = (JDXSpectrum) jsvp.getSpectrumAt(0);
      //jsvpPopupMenu.setSelectedJSVPanel(panel);
      //jsvpPopupMenu.setSource(currentSelectedSource);
      //jsvpPopupMenu.properties_actionPerformed(e);
      //Coordinate[] source;
      //source = currentSelectedSource.getJDXSpectrum(0).getXYCoords();
      //JDXSpectrum spectrum = (JDXSpectrum)selectedJSVPanel.getSpectrumAt(0);
      sltnclr = Visible.Colour(spectrum.getXYCoords(),Yunits0);
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
          new TextDialog(this, file.getAbsolutePath(), errorLog, true);
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
  @SuppressWarnings("unchecked")
  public void drop(DropTargetDropEvent dtde) {
    try {
      Transferable t = dtde.getTransferable();

      if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        List list = (List) t.getTransferData(DataFlavor.
            javaFileListFlavor);
        dtde.getDropTargetContext().dropComplete(true);
        File[] files = (File[]) list.toArray();
        for (int i = 0; i < list.size(); i++) {
          openFile( files[i]);
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
