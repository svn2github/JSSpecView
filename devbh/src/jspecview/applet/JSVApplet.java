/* Copyright (c) 2002-2008 The University of the West Indies
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

// CHANGES to 'JSVApplet.java' - Web Application GUI
// University of the West Indies, Mona Campus
//
// 09-10-2007 commented out calls for exporting data
//            this was causing security issues with JRE 1.6.0_02 and 03
// 13-01-2008 in-line load JCAMP-DX file routine added
// 22-07-2008 reinstated calls for exporting since Ok with JRE 1.6.0_05
// 25-07-2008 added module to predict colour of solution

package jspecview.applet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JApplet;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jspecview.common.Coordinate;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVPanel;
import jspecview.common.JSpecViewUtils;
import jspecview.common.OverlayLegendDialog;
import jspecview.common.PrintLayoutDialog;
import jspecview.common.TransmittanceAbsorbanceConverter;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.export.Exporter;
import jspecview.source.BlockSource;
import jspecview.source.CompoundSource;
import jspecview.source.JDXSource;
import jspecview.source.NTupleSource;
import jspecview.util.Logger;
import netscape.javascript.JSObject;
import jspecview.common.Visible;

/**
 * JSpecView Applet class.
 * For a list of parameters and scripting functionality see the file
 * JSpecView_Applet_Specification.html.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig Walters
 * @author Prof Robert J. Lancashire
 */

public class JSVApplet extends JApplet {
  public static final String APPLET_VERSION = "1.0.20080804-2030";

  /* --------------------set default-PARAMETERS -------------------------*/
  String filePath, oldfilePath;
  String newFilePath = null;
  String recentFileName = "";
  String fileURL;

  boolean gridOn=true;
  boolean coordinatesOn=true;
  boolean reversePlot=false;
  boolean menuOn=true;
  boolean compoundMenuOn=false;
  boolean compoundMenuOn2=true;
  boolean enableZoom=true;

  int startIndex=-1;
  int endIndex=-1;
  int spectrumNumber=-1; // blockNumber or nTupleNumber
  int numberOfSpectra;

  String theInterface="single"; // either tab, tile, single, overlay
  String coordCallbackFunctionName=null; // = "coordCallBack";
  String peakCallbackFunctionName=null; // peakCallback
  String sltnclr="255,255,255"; //Colour of Solution

  Color titleColor=Color.BLACK;
  Color gridColor=Color.LIGHT_GRAY;
  Color unitsColor=Color.RED;
  Color scaleColor=Color.BLACK;
  Color coordinatesColor=Color.RED;
  Color plotAreaColor=Color.WHITE;
  Color backgroundColor=new Color(192,192,192);
  Color plotColor= Color.BLUE;


  Color[] plotColors= {Color.blue, Color.green, Color.red, Color.magenta, Color.yellow, Color.orange,  Color.pink, Color.cyan};
  String plotColorsStr;
  Color integralPlotColor = Color.red;

  final private int TO_TRANS = 0;
  final private int TO_ABS = 1;
  final private int IMPLIED = 2;

  /*---------------------------------END PARAMETERS------------------------*/


  boolean isStandalone=false;
  BorderLayout appletBorderLayout = new BorderLayout();
  JPanel statusPanel = new JPanel();
  JLabel statusTextLabel = new JLabel();
  JPopupMenu appletPopupMenu = new JPopupMenu();
  JMenu aboutMenu = new JMenu();
  JMenu fileMenu = new JMenu();
  JMenuItem printMenuItem = new JMenuItem();
  JMenu saveAsMenu = new JMenu();
  JMenu saveAsJDXMenu = new JMenu();
  JMenu exportAsMenu = new JMenu();
  JMenu viewMenu = new JMenu();
  JCheckBoxMenuItem gridMenuItem = new JCheckBoxMenuItem();
  JCheckBoxMenuItem coordinatesMenuItem = new JCheckBoxMenuItem();
  JCheckBoxMenuItem reversePlotMenuItem = new JCheckBoxMenuItem();
  JMenu zoomMenu = new JMenu();
  JMenuItem nextMenuItem = new JMenuItem();
  JMenuItem previousMenuItem = new JMenuItem();
  JMenuItem resetMenuItem = new JMenuItem();
  JMenuItem compoundMenu = new JMenu();
  JFileChooser jFileChooser;
  JSVPanel selectedJSVPanel;
  JMenuItem clearMenuItem = new JMenuItem();
  JCheckBoxMenuItem transAbsMenuItem = new JCheckBoxMenuItem();
  JCheckBoxMenuItem integrateMenuItem = new JCheckBoxMenuItem();
  JMenuItem solColMenuItem = new JMenuItem();
  JMenuItem versionMenuItem = new JMenuItem();
  JMenuItem headerMenuItem = new JMenuItem();
  JCheckBoxMenuItem windowMenuItem = new JCheckBoxMenuItem();
  JMenuItem overlayKeyMenuItem = new JMenuItem();
  JDXSpectrum xmlSpec;
  JSVPanel tempJSVP;

  private static final String[] params = {
     "LOAD", 
     "REVERSEPLOT", 
     "COORDINATESON", 
     "GRIDON", 
     "COORDCALLBACKFUNCTIONNAME", 
     "SPECTRUMNUMBER", 
     "INTERFACE", 
     "ENDINDEX", 
     "ENABLEZOOM", 
     "STARTINDEX", 
     "MENUON", 
     "COMPOUNDMENUON", 
     "BACKGROUNDCOLOR", 
     "COORDINATESCOLOR", 
     "GRIDCOLOR", 
     "PLOTAREACOLOR", 
     "PLOTCOLOR", 
     "SCALECOLOR", 
     "TITLECOLOR", 
     "UNITSCOLOR", 
     "PLOTCOLORS", 
     "VERSION", 
     "PEAKCALLBACKFUNCTIONNAME", 
     "IRMODE"
     };
  
  final private static int PARAM_LOAD = 0;
  final private static int PARAM_REVERSEPLOT = 1;
  final private static int PARAM_COORDINATESON = 2;
  final private static int PARAM_GRIDON = 3;
  final private static int PARAM_COORDCALLBACKFUNCTIONNAME = 4;
  final private static int PARAM_SPECTRUMNUMBER = 5;
  final private static int PARAM_INTERFACE = 6;
  final private static int PARAM_ENDINDEX = 7;
  final private static int PARAM_ENABLEZOOM = 8;
  final private static int PARAM_STARTINDEX = 9;
  final private static int PARAM_MENUON = 10;
  final private static int PARAM_COMPOUNDMENUON = 11;
  final private static int PARAM_BACKGROUNDCOLOR = 12;
  final private static int PARAM_COORDINATESCOLOR = 13;
  final private static int PARAM_GRIDCOLOR = 14;
  final private static int PARAM_PLOTAREACOLOR = 15;
  final private static int PARAM_PLOTCOLOR = 16;
  final private static int PARAM_SCALECOLOR = 17;
  final private static int PARAM_TITLECOLOR = 18;
  final private static int PARAM_UNITSCOLOR = 19;
  final private static int PARAM_PLOTCOLORS = 20;
  final private static int PARAM_VERSION = 21;
  final private static int PARAM_PEAKCALLBACKFUNCTIONNAME = 22;
  final private static int PARAM_IRMODE = 23;
  
  final private static Hashtable<String, Integer> htParams = new Hashtable<String, Integer>();
  {
    for (int i = 0; i < params.length; i++)
      htParams.put(params[i], new Integer(i));
  }
  //"ADDHIGHLIGHT", "REMOVEHIGHLIGHT", "REMOVEALLHIGHTLIGHTS"

 /**
   * Do we have new parameters passed from a javascript call?
   */
  public boolean newParams = false;

  public boolean newFile = false;

  /**
   * parameters from a javascript call
   */
  public String JSVparams;

  /**
   * The panes of a tabbed display
   */
  JTabbedPane spectraPane = new JTabbedPane();

  /**
   * A list of </code>JDXSpectrum</code> instances
   */
  Vector<JDXSpectrum> specs;

  /**
   * The <code>JSVPanel</code>s created for each </code>JDXSpectrum</code>
   */
  JSVPanel[] jsvPanels;

  /**
   * The <code>JDXSource</code> instance
   */
  JDXSource source;

  /**
   * The Panel on which the applet contents are drawn
   */
  JPanel appletPanel;

  /**
   * Frame constructed from applet panel when rising off web page
   */
  JFrame frame;

  /**
   * The index of the <code>JDXSpectrum</code> that is is focus.
   */
  public int currentSpectrumIndex=0;

  /**
   * Whether or not spectra should be overlayed
   */
  boolean overlay;

  
  String irMode;
  
  /**
   * Returns a parameter value
   * @param key the parameter name
   * @param def the default value. If param is not found then this is returned
   * @return a parameter value
   */
  public String getParameter(String key, String def) {
    return isStandalone ? System.getProperty(key, def) :
      (getParameter(key) != null ? getParameter(key) : def);
  }

  /**
   * Initialises applet with parameters and load the <code>JDXSource</code>
   */
  @Override
  public void init() {
	  init(null);
  }

  private void init(String data) {
    if (data != null) {

    } else if (!newFile) {
      JSVparams = getParameter("script");
      parseInitScript(JSVparams);
    } else {
      if (newFilePath != null)
        filePath = newFilePath;
    }
    // enable or disable menus
    if (!menuOn) {
      viewMenu.setEnabled(false);
      fileMenu.setEnabled(false);
      exportAsMenu.setEnabled(false);
      saveAsMenu.setEnabled(false);
    }
    zoomMenu.setEnabled(enableZoom);

    //setBackground(backgroundColor);

    try {
      jbInit();
    } catch (Exception e) {
      e.printStackTrace();
    }

    appletPanel = new JPanel(new BorderLayout());
    //appletPanel.add(statusPanel,  BorderLayout.SOUTH);
    Font statusFont = new Font(null, Font.PLAIN, 12);
    statusTextLabel.setFont(statusFont);
    statusTextLabel.setForeground(Color.darkGray);
    statusPanel.add(statusTextLabel, null);
    this.getContentPane().add(appletPanel);
    String fileName = null;
    boolean continuous = false;
    String Xunits = "", Yunits = "";
    double firstX = 0, lastX = 0;
    System.out.println("applet: " + filePath);
    URL base = null;
    if (data != null) {
    } else if (filePath != null) {
      URL url;
      try {
        url = new URL(getCodeBase(), filePath);
        System.out.println("applet: " + url);
        fileName = url.toString();
        recentFileName = url.getFile();
        base = getDocumentBase();
      } catch (MalformedURLException e) {
        System.out.println("problem: " + e.getMessage());
        fileName = filePath;
      }
    } else {
      writeStatus("Please set the 'filepath' or 'load file' parameter");
      return;
    }
    Object ret = JDXSource.createJDXSource(data, fileName, base);
    System.out.println(ret);
    if (ret instanceof String) {
      writeStatus((String) ret);
      return;
    }
    source = (JDXSource) ret;
    specs = source.getSpectra();
    continuous = source.getJDXSpectrum(0).isContinuous();
    if (!compoundMenuOn2)
      compoundMenuOn = false;
    else {
      compoundMenuOn = source instanceof CompoundSource;
    }
    Yunits = source.getJDXSpectrum(0).getYUnits();
    Xunits = source.getJDXSpectrum(0).getXUnits();
    firstX = source.getJDXSpectrum(0).getFirstX();
    lastX = source.getJDXSpectrum(0).getLastX();

    try {
      initPanels();
    } catch (JSpecViewException e1) {
      writeStatus(e1.getMessage());
      return;
    }
    initInterface();

    System.out.println("JSpecView vs: " + APPLET_VERSION + " File " + fileName
        + " Loaded Successfully");

    // if Transmittance, visible 400-700 and nm then calc colour
    if ((Xunits.toLowerCase().contains("nanometer"))
        & (firstX < 401)
        & (lastX > 699)
        & ((Yunits.toLowerCase().contains("trans")) || Yunits.toLowerCase()
            .contains("abs"))) {
      //         sltnclr = Visible.Colour(source);
      solColMenuItem.setEnabled(true);
    } else {
      solColMenuItem.setEnabled(false);
    }

    //  Can only integrate a continuous H NMR spectrum
    if (continuous
        && JSpecViewUtils.isHNMR((JDXSpectrum) selectedJSVPanel
            .getSpectrumAt(0)))
      integrateMenuItem.setEnabled(true);
    //  Can only convert from T <-> A  if Absorbance or Transmittance and continuous
    if ((continuous) && (Yunits.toLowerCase().contains("abs"))
        || (Yunits.toLowerCase().contains("trans")))
      transAbsMenuItem.setEnabled(true);
    else
      transAbsMenuItem.setEnabled(false);
    setStringParameter("irmode", irMode);
    exportAsMenu.setEnabled(true);
    saveAsJDXMenu.setEnabled(continuous);
    newFile = false;
  }

  /**
   * Initialises the applet's GUI components
   * @throws Exception
   */
  private void jbInit() throws Exception {
    try {
      jFileChooser = new JFileChooser();
    } catch (SecurityException se) {
      //System.err.println("Export menu disabled: You need the signed Applet to export files");
    }

    statusTextLabel.setText("Loading...");
    aboutMenu.setText("About");

    fileMenu.setText("File");
    printMenuItem.setActionCommand("Print");
    printMenuItem.setText("Print...");
    printMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        printMenuItem_actionPerformed(e);
      }
    });
    viewMenu.setText("View");
    gridMenuItem.setText("Show Grid");
    gridMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        gridMenuItem_itemStateChanged(e);
      }
    });
    coordinatesMenuItem.setText("Show Coordinates");
    coordinatesMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        coordinatesMenuItem_itemStateChanged(e);
      }
    });
    reversePlotMenuItem.setText("Reverse Plot");
    reversePlotMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        reversePlotMenuItem_itemStateChanged(e);
      }
    });
    zoomMenu.setText("Zoom");
    nextMenuItem.setText("Next View");
    nextMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        nextMenuItem_actionPerformed(e);
      }
    });
    previousMenuItem.setText("Previous View");
    previousMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        previousMenuItem_actionPerformed(e);
      }
    });
    resetMenuItem.setText("Reset View");
    resetMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        resetMenuItem_actionPerformed(e);
      }
    });
    clearMenuItem.setText("Clear Views");
    clearMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearMenuItem_actionPerformed(e);
      }
    });
    headerMenuItem.setText("Show Header...");
    headerMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        headerMenuItem_actionPerformed(e);
      }
    });

    solColMenuItem.setEnabled(false);
    solColMenuItem.setText("Predict Solution Colour...");
    solColMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        solColMenuItem_actionPerformed(e);
      }
    });

    windowMenuItem.setText("Window");
    windowMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        windowMenuItem_itemStateChanged(e);
      }
    });
    overlayKeyMenuItem.setEnabled(false);
    overlayKeyMenuItem.setText("Show Overlay Key...");
    overlayKeyMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        overlayKeyMenuItem_actionPerformed(e);
      }
    });
    transAbsMenuItem.setEnabled(true);
    transAbsMenuItem.setText("Transmittance/Absorbance");
    transAbsMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        transAbsMenuItem_itemStateChanged(e);
      }
    });

    integrateMenuItem.setEnabled(false);
    integrateMenuItem.setText("Integrate");
    integrateMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        integrateMenuItem_itemStateChanged(e);
      }
    });

    compoundMenu.setEnabled(false);
    compoundMenu.setText("Blocks");

    versionMenuItem.setText("<html><h3>JSpecView Version " + APPLET_VERSION + "</h3></html>");

    appletPopupMenu.add(fileMenu);
    appletPopupMenu.add(viewMenu);
    appletPopupMenu.add(zoomMenu);
    appletPopupMenu.add(compoundMenu);
    appletPopupMenu.addSeparator();
    appletPopupMenu.add(aboutMenu);
    JSVPanel.setMenus(saveAsMenu, saveAsJDXMenu, exportAsMenu, actionListener);
    fileMenu.add(saveAsMenu);
    if(jFileChooser != null)
      fileMenu.add(exportAsMenu);
    fileMenu.add(printMenuItem);

    viewMenu.add(gridMenuItem);
    viewMenu.add(coordinatesMenuItem);
    viewMenu.add(reversePlotMenuItem);
    viewMenu.addSeparator();
    viewMenu.add(headerMenuItem);
    viewMenu.add(overlayKeyMenuItem);
    viewMenu.addSeparator();
    viewMenu.add(transAbsMenuItem);
    viewMenu.add(solColMenuItem);
    viewMenu.add(integrateMenuItem);
    viewMenu.addSeparator();
    viewMenu.add(windowMenuItem);
    zoomMenu.add(nextMenuItem);
    zoomMenu.add(previousMenuItem);
    zoomMenu.add(resetMenuItem);
    zoomMenu.add(clearMenuItem);
    aboutMenu.add(versionMenuItem);
  }

  private ActionListener actionListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      exportSpectrum(e.getActionCommand());
    }
  };
  
  /**
   * Get Applet information
   * @return the String "JSpecView Applet"
   */
  @Override
  public String getAppletInfo() {
    return "JSpecView Applet " + APPLET_VERSION;
  }

  /**
   * Returns null
   * @return null
   */
  @Override
  public String[][] getParameterInfo() {
    return null;
  }

  /**
   * Initalises the <code>JSVPanels</code> and adds them to the jsvPanels array
   * @throws JSpecViewException
   */
  private void initPanels() throws JSpecViewException {
    boolean showRange = false;
    JSVPanel jsvp;

      numberOfSpectra = specs.size();
      overlay = theInterface.equals("overlay") && numberOfSpectra > 1;

    if (startIndex != -1 && endIndex != -1)
      showRange = true;

    // Initialise JSVpanels

    if (overlay) {
      // overlay all spectra on a panel
      jsvPanels = new JSVPanel[1];

      try {
        if (showRange) {
          int[] startIndices = new int[numberOfSpectra];
          int[] endIndices = new int[numberOfSpectra];

          Arrays.fill(startIndices, startIndex);
          Arrays.fill(endIndices, endIndex);

          jsvp = new JSVPanel(specs, startIndices, endIndices);
        } else
          jsvp = new JSVPanel(specs);
      } catch (ScalesIncompatibleException sie) {
        theInterface = "single";
        initPanels();
        return;
      }

      jsvPanels[0] = jsvp;

      initProperties(jsvp);

      // add listeners
      jsvp.addMouseListener(new JSVPanelMouseListener());
      selectedJSVPanel = jsvp;

    } else {
      // initalise JSVPanels and add them to the array
      jsvPanels = new JSVPanel[numberOfSpectra];
      try {
        for (int i = 0; i < numberOfSpectra; i++) {
          if (showRange)
            jsvp = new JSVPanel((JDXSpectrum) specs.elementAt(i), startIndex,
                endIndex);
          else
            jsvp = new JSVPanel((JDXSpectrum) specs.elementAt(i));
          jsvPanels[i] = jsvp;

          initProperties(jsvp);

          // add listeners
          jsvp.addMouseListener(new JSVPanelMouseListener());
        }
      } catch (Exception e) {
        // TODO
      }
    }
  }

  public void initProperties(JSVPanel jsvp) {
      // set JSVPanel properties from applet parameters
        jsvp.setGridOn(gridOn);
        jsvp.setCoordinatesOn(coordinatesOn);
        jsvp.setReversePlot(reversePlot);
        jsvp.setZoomEnabled(enableZoom);

        // other JSVPanel properties
        // Need to add to applet Parameters
        jsvp.setPlotAreaColor(plotAreaColor);
        jsvp.setGridColor(gridColor);
        jsvp.setTitleColor(titleColor);
        jsvp.setUnitsColor(unitsColor);
        jsvp.setScaleColor(scaleColor);
        jsvp.setcoordinatesColor(coordinatesColor);
        //jsvp.setPlotColor(plotColor);
        jsvp.setIntegralPlotColor(integralPlotColor);
        jsvp.setBackground(backgroundColor);
        getPlotColors();  //<======= Kind of sloppy
        if(plotColors != null)
          jsvp.setPlotColors(plotColors);
  }

  /**
   * Initialises the interface of the applet depending on the value of the
   * <i>interface</i> parameter
   */
  private void initInterface(){
    final int numberOfPanels = jsvPanels.length;
    boolean canDoTile = (numberOfPanels >=2 && numberOfPanels <=10);
    boolean moreThanOnePanel = numberOfPanels > 1;
    boolean showSpectrumNumber = spectrumNumber != -1 &&
                                 spectrumNumber <= numberOfPanels;

    //appletPanel.setBackground(backgroundColor);

    if(theInterface.equals("tab") && moreThanOnePanel){
      spectraPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
      appletPanel.add(new JLabel(((CompoundSource)source).getTitle(), SwingConstants.CENTER), BorderLayout.NORTH);
      appletPanel.add(spectraPane,  BorderLayout.CENTER);

      for(int i = 0; i < numberOfPanels; i++){
        String title = ((JDXSpectrum)specs.elementAt(i)).getTitle();
        if(source instanceof NTupleSource)
          title = title.substring(title.indexOf(':') + 1);
        else if (source instanceof BlockSource)
          //title = "block " + (i + 1);
          title = title.substring(0, (title.length() >= 10 ? 10 : title.length())) +
                        "... : ";
        spectraPane.addTab(title, jsvPanels[i]);
      }

      // Show the spectrum specified by the spectrumnumber parameter
      if(showSpectrumNumber){
        spectraPane.setSelectedIndex(spectrumNumber - 1);
      }

      selectedJSVPanel = (JSVPanel)spectraPane.getSelectedComponent();
      spectraPane.addChangeListener(new ChangeListener(){
        public void stateChanged(ChangeEvent e){
          selectedJSVPanel = (JSVPanel)spectraPane.getSelectedComponent();
        }
      });
    }
    else if(theInterface.equals("tile") && canDoTile){
      appletPanel.add(new JLabel(((CompoundSource)source).getTitle(), SwingConstants.CENTER), BorderLayout.NORTH);

      for(int i = 0; i < numberOfPanels; i++){
        jsvPanels[i].setMinimumSize(new Dimension(250, 150));
        jsvPanels[i].setPreferredSize(new Dimension(300, 200));
      }
      JSplitPane splitPane = createSplitPane(jsvPanels);
      appletPanel.add(splitPane,  BorderLayout.CENTER);
      //splitPane.setBackground(backgroundColor);
    }

    else{ // Single or overlay
//      compoundMenuOn = true;
      int spectrumIndex;
      String title;

      if(showSpectrumNumber)
        spectrumIndex = spectrumNumber - 1;
      else
        spectrumIndex = 0;

      selectedJSVPanel = jsvPanels[spectrumIndex];

      // Global variable for single interface
      currentSpectrumIndex = spectrumIndex;
      if(overlay && source instanceof CompoundSource){
        title = ((CompoundSource)source).getTitle();
        jsvPanels[spectrumIndex].setTitle(title);
        overlayKeyMenuItem.setEnabled(true);
      }
      appletPanel.add(jsvPanels[spectrumIndex], BorderLayout.CENTER);

      // else interface = single
      if(moreThanOnePanel && compoundMenuOn){
          if(numberOfPanels <= 20){

            // add Menus to navigate
            JCheckBoxMenuItem mi;

          if(source instanceof NTupleSource){
            for(int i = 0; i < numberOfPanels; i++){
              title = (i+1) + "- " + ((JDXSpectrum) specs.elementAt(i)).getTitle();
              mi = new JCheckBoxMenuItem(title);
              if (i == currentSpectrumIndex) mi.setSelected(true);

              mi.addItemListener(new java.awt.event.ItemListener() {
                public void itemStateChanged(ItemEvent e) {

                  if (e.getStateChange() == ItemEvent.SELECTED) {
                    // deselects the previously selected menu item
                    JCheckBoxMenuItem deselectedMenu =
                        (JCheckBoxMenuItem)((JCheckBoxMenuItem)e.getSource()).getParent().getComponent(currentSpectrumIndex);
                    deselectedMenu.setSelected(false);

                    compoundMenu_itemStateChanged(e);
                  }
                }
              });

              mi.setActionCommand(""+i);
              compoundMenu.add(mi);
            }
            compoundMenu.setText("NTuples");
          }
          else if(source instanceof BlockSource){
              for(int i = 0; i < numberOfPanels; i++){
              title = (i+1) + "- " + ((JDXSpectrum)specs.elementAt(i)).getTitle();
              mi = new JCheckBoxMenuItem(title);
              if (i == currentSpectrumIndex) mi.setSelected(true);

              mi.addItemListener(new java.awt.event.ItemListener() {
                public void itemStateChanged(ItemEvent e) {

                  if (e.getStateChange() == ItemEvent.SELECTED) {
                   // deselects the previously selected menu item
                   JCheckBoxMenuItem deselectedMenu =
                       (JCheckBoxMenuItem)((JCheckBoxMenuItem)e.getSource()).getParent().getComponent(currentSpectrumIndex);
                   deselectedMenu.setSelected(false);

                  compoundMenu_itemStateChanged(e);
                }
              }

              });

              mi.setActionCommand(""+i);
              compoundMenu.add(mi);
            }

            compoundMenu.setText("Blocks");
          }

          // add compound menu to popup menu
          appletPopupMenu.add(compoundMenu, 3);
          if (compoundMenuOn)
            compoundMenu.setEnabled(true);
        }
        else{
          // open dialog box
          JMenuItem compoundMi = new JMenuItem("Choose Spectrum");
          compoundMi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              StringBuffer msgStrBuffer = new StringBuffer();
              msgStrBuffer.append("Choose a number between 1 and ");
              msgStrBuffer.append(numberOfPanels);
              msgStrBuffer.append(" to display another spectrum");

              String str = JOptionPane.showInputDialog(JSVApplet.this,
                              msgStrBuffer.toString(),
                              "Spectrum Chooser",
                              JOptionPane.PLAIN_MESSAGE);
              if(str != null){
                int index = 0;
                try{
                  index = Integer.parseInt(str) - 1;
                }
                catch(NumberFormatException nfe){}

                if(index > 0 && index < numberOfPanels){
                  showSpectrum(index);
                  writeStatus(" ");
                }
                else
                  writeStatus("Invalid Spectrum Number");
              }
            }
          });
        }
      }
    }
  }

 /**
   * display from a drop-down menu
   * @param e the ItemEvent
   */
  void compoundMenu_itemStateChanged(ItemEvent e) {

    // gets the newly selected title
    JCheckBoxMenuItem selectedMenu = (JCheckBoxMenuItem) e.getSource();
    String txt = selectedMenu.getText();

    // shows the newly selected block
    int index = Integer.parseInt(txt.substring(0, txt.indexOf("-")));
    //sltnclr = Visible.Colour(source);
    showSpectrum(index - 1);
    }

  /**
   * Shows the </code>JSVPanel</code> at a certain index
   * @param index the index
   */
  void showSpectrum(int index){
    JSVPanel jsvp = jsvPanels[index];
    appletPanel.remove(jsvPanels[currentSpectrumIndex]);
    appletPanel.add(jsvp, BorderLayout.CENTER);
    currentSpectrumIndex = index;
    selectedJSVPanel = jsvp;

    if (JSpecViewUtils.isHNMR((JDXSpectrum)selectedJSVPanel.getSpectrumAt(0)))
      integrateMenuItem.setEnabled(true);
   else
      integrateMenuItem.setEnabled(false);

    chooseContainer();
    this.validate();
    repaint();
  }

  /**
   * Writes a message to the status label
   * @param msg the message
   */
  public void writeStatus(String msg){
    statusTextLabel.setText(msg);
  }

  /**
   * A Mouse Listener for the JSVPanel
   */
  class JSVPanelMouseListener extends MouseAdapter {

    /**
     * The number of mouse clicks
     */
    int clickCount = 0;

    /**
     * If mouse is clicked with the plot area of the <code>JSVPanel</code>
     * and coordinate call back is enabled then the value of the coordinate
     * clicked is send to a javascript function specified by the
     * coordcallbackfunctionname parameter
     * @param e the MouseEvent
     */
    @Override
    public void mouseClicked(MouseEvent e){
      JSVPanel jsvPanel = (JSVPanel)e.getSource();
      selectedJSVPanel = jsvPanel;

      if (peakCallbackFunctionName != null) {
        Coordinate coord = jsvPanel.getClickedCoordinate();
        int store = 0;
        double xPt = coord.getXVal();

        if(coord != null) {

          JDXSpectrum spectrum = (JDXSpectrum)selectedJSVPanel.getSpectrumAt(0);
          for (int i = 0; i < spectrum.getXYCoords().length; i++) {
            if (spectrum.getXYCoords()[i].getXVal() > xPt) {
              store = i;
              break;
            }
          }

          double actualXPt = spectrum.getXYCoords()[store].getXVal();
          double actualYPt = spectrum.getXYCoords()[store].getYVal();

          DecimalFormat displayXFormatter = new DecimalFormat("0.000000", new DecimalFormatSymbols(java.util.Locale.US ));
          DecimalFormat displayYFormatter = new DecimalFormat("0.000000", new DecimalFormatSymbols(java.util.Locale.US ));

          String actualXCoordStr = displayXFormatter.format(actualXPt);
          String actualYCoordStr = displayYFormatter.format(actualYPt);
          Coordinate actualCoord = new Coordinate(Double.parseDouble(actualXCoordStr), Double.parseDouble(actualYCoordStr) );

          callToJavaScript(peakCallbackFunctionName, coord.getXVal() + ", " + coord.getYVal() + ", " + actualCoord.getXVal() + ", " + actualCoord.getYVal() + ", " + (currentSpectrumIndex + 1));
        }
      }
      else if(coordCallbackFunctionName != null){
        Coordinate coord = jsvPanel.getClickedCoordinate();
        if(coord != null)
          callToJavaScript(coordCallbackFunctionName, coord.getXVal() + ", " + coord.getYVal());
      }
    }

    /**
     * Shows a popup menu when the right mouse button is clicked
     * @param e MouseEvent
     */
    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    /**
     * Shows a popup menu when the right mouse button is clicked
     * @param e MouseEvent
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    /**
     * If the right mouse button is clicked then show the popup menu
     * and set status of <code>JCheckBoxMenuItem</code>s according to the
     * <code>JSVPanel</code> that is clicked
     * @param e MouseEvent
     */
    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
          appletPopupMenu.show(e.getComponent(),
                     e.getX(), e.getY());
          JSVPanel jsvPanel = (JSVPanel)e.getSource();
          selectedJSVPanel = jsvPanel;

          gridMenuItem.setSelected(jsvPanel.isGridOn());
          coordinatesMenuItem.setSelected(jsvPanel.isCoordinatesOn());

          reversePlotMenuItem.setSelected(jsvPanel.isPlotReversed());
      }
    }
  }

  /**
   * Toggles the grid
   * @param e the ItemEvent
   */
  void gridMenuItem_itemStateChanged(ItemEvent e) {
    selectedJSVPanel.setGridOn((e.getStateChange() == ItemEvent.SELECTED));
    selectedJSVPanel.repaint();
  }

  /**
     * Toggles the coordinates
     * @param e the ItemEvent
   */
  void coordinatesMenuItem_itemStateChanged(ItemEvent e) {
    selectedJSVPanel.setCoordinatesOn((e.getStateChange() == ItemEvent.SELECTED));
    selectedJSVPanel.repaint();
  }

  /**
   * Reverses the plot
   * @param e the ItemEvent
   */
  void reversePlotMenuItem_itemStateChanged(ItemEvent e) {
    selectedJSVPanel.setReversePlot((e.getStateChange() == ItemEvent.SELECTED));
    selectedJSVPanel.repaint();
  }

  /**
   * Shows the header information for the Spectrum
   * @param e the ActionEvent
   */
  void headerMenuItem_actionPerformed(ActionEvent e) {
    if(overlay){
      // Show header of Source
      HashMap<String, String> header = (HashMap<String, String>)((CompoundSource)source).getHeaderTable();
      Object[] headerLabels = header.keySet().toArray();
      Object[] headerValues = header.values().toArray();

      int coreHeaderSize = 5;

      String[] columnNames = {"Label", "Description"};
      int headerSize = header.size() + coreHeaderSize;

      Object rowData[][] = new Object[headerSize][];
      Object[] tmp;
      int i = 0;

      // add core header
      tmp = new Object[2];
      tmp[0] = "##TITLE";
      tmp[1] = ((CompoundSource)source).getTitle();
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##JCAMP-DX";
      tmp[1] = ((CompoundSource)source).getJcampdx();
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##DATA TYPE";
      tmp[1] = ((CompoundSource)source).getDataType();
      rowData[i++] = tmp;

      /*
      tmp = new Object[2];
      tmp[0] = "##DATA CLASS";
      tmp[1] = ((CompoundSource)source).getDataClass();
      rowData[i++] = tmp;
      */

      tmp = new Object[2];
      tmp[0] = "##ORIGIN";
      tmp[1] = ((CompoundSource)source).getOrigin();
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##OWNER";
      tmp[1] = ((CompoundSource)source).getOwner();
      rowData[i++] = tmp;

      for(int j = 0; i < headerSize ; i++, j++){
        tmp = new Object[2];
        tmp[0] = headerLabels[j];
        tmp[1] = headerValues[j];
        rowData[i] = tmp;
      }

      JTable table = new JTable(rowData, columnNames);
      JScrollPane scrollPane = new JScrollPane(table);

      JOptionPane.showMessageDialog(this, scrollPane, "Header Information",
                                    JOptionPane.PLAIN_MESSAGE);

    }
    else{
      JDXSpectrum spectrum = (JDXSpectrum)selectedJSVPanel.getSpectrumAt(0);

      HashMap<String, String> header = spectrum.getHeaderTable();
      Object[] headerLabels = header.keySet().toArray();
      Object[] headerValues = header.values().toArray();

      int coreHeaderSize = 6;
      int specParamsSize = 8;

      String[] columnNames = {"Label", "Description"};
      int headerSize = header.size() + coreHeaderSize + specParamsSize;

      Object rowData[][] = new Object[headerSize][];
      Object[] tmp;
      int i = 0;

      // add core header
      tmp = new Object[2];
      tmp[0] = "##TITLE";
      tmp[1] = spectrum.getTitle();
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##JCAMP-DX";
      tmp[1] = spectrum.getJcampdx();
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##DATA TYPE";
      tmp[1] = spectrum.getDataType();
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##DATA CLASS";
      tmp[1] = spectrum.getDataClass();
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##ORIGIN";
      tmp[1] = spectrum.getOrigin();
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##OWNER";
      tmp[1] = spectrum.getOwner();
      rowData[i++] = tmp;

      for(int j = 0; i < (headerSize - specParamsSize); i++, j++){
        tmp = new Object[2];
        tmp[0] = headerLabels[j];
        tmp[1] = headerValues[j];
        rowData[i] = tmp;
      }

      // add spectral parameters
      tmp = new Object[2];
      tmp[0] = "##XUNITS";
      tmp[1] = spectrum.isHZtoPPM() ? "HZ" : spectrum.getXUnits();
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##YUNITS";
      tmp[1] = spectrum.getYUnits();
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##FIRSTX";
      double firstX;
      if(spectrum.isIncreasing())
        firstX = spectrum.getFirstX();
      else
        firstX = spectrum.getLastX();

      tmp[1] = String.valueOf( spectrum.isHZtoPPM() ?
                               firstX * spectrum.getObservedFreq() :
                               firstX);

      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##LASTX";
      double lastX;
      if(spectrum.isIncreasing())
        lastX = spectrum.getLastX();
      else
        lastX = spectrum.getFirstX();
      tmp[1] = String.valueOf(spectrum.isHZtoPPM() ?
                              lastX * spectrum.getObservedFreq() :
                              lastX);
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##XFACTOR";
      tmp[1] = String.valueOf(spectrum.getXFactor());
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##YFACTOR";
      tmp[1] = String.valueOf(spectrum.getYFactor());
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##NPOINTS";
      tmp[1] = String.valueOf(spectrum.getNumberOfPoints());
      rowData[i++] = tmp;

      tmp = new Object[2];
      tmp[0] = "##FIRSTY";
      if(spectrum.isIncreasing())
        tmp[1] = String.valueOf(spectrum.getFirstY());
      else
        tmp[1] = String.valueOf(spectrum.getLastY());
      rowData[i++] = tmp;

      JTable table = new JTable(rowData, columnNames);
      JScrollPane scrollPane = new JScrollPane(table);

      JOptionPane.showMessageDialog(this, scrollPane, "Header Information",
                                    JOptionPane.PLAIN_MESSAGE);
    }
  }

  /**
   * Calculates the predicted colour of the Spectrum
   * @param e the ActionEvent
   */
  void solColMenuItem_actionPerformed(ActionEvent e) {

    JDXSpectrum spectrum = (JDXSpectrum)selectedJSVPanel.getSpectrumAt(0);
    String Yunits = spectrum.getYUnits();
//    System.out.println(spectrum.getTitle());
    sltnclr = Visible.Colour(spectrum.getXYCoords(),Yunits);

    //JScrollPane scrollPane = new JScrollPane();

    JOptionPane.showMessageDialog(this, "<HTML><body bgcolor=rgb("+sltnclr+")><br />Predicted Solution Colour- RGB("+sltnclr+")<br /><br /></body></HTML>"
                                  , "Predicted Colour",
                                    JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Opens the print dialog to enable printing
   * @param e ActionEvent
   */
  void printMenuItem_actionPerformed(ActionEvent e) {
    if(frame == null) {
      System.err.println("Use the View/Window menu to lift the spectrum off the page first.");
      return;
    }

    JSVPanel jsvp = selectedJSVPanel;

    PrintLayoutDialog ppd = new PrintLayoutDialog(frame);
    PrintLayoutDialog.PrintLayout pl = ppd.getPrintLayout();

    if(pl != null)
      jsvp.printSpectrum(pl);
  }

  /**
   * Shows the next view when <code>JDXSpectrum</code> has been zoomed
   * @param e the ActionEvent
   */
  void nextMenuItem_actionPerformed(ActionEvent e) {
    selectedJSVPanel.nextView();
  }

  /**
   * Shows the previous view when <code>JDXSpectrum</code> has been zoomed
   * @param e the ActionEvent
   */
  void previousMenuItem_actionPerformed(ActionEvent e) {
    selectedJSVPanel.previousView();
  }

  /**
     * Resets the view when <code>JDXSpectrum</code> has been zoomed
     * @param e the ActionEvent
   */
  void resetMenuItem_actionPerformed(ActionEvent e) {
    selectedJSVPanel.reset();
  }

  /**
   * Clears all zoomed views
   * @param e the ActionEvent
   */
  void clearMenuItem_actionPerformed(ActionEvent e) {
    selectedJSVPanel.clearViews();
  }

  /**
   * Shows the applet in a Frame
   * @param e the ActionEvent
   */
  void windowMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      // disable some features when in Window mode
      integrateMenuItem.setEnabled(false);
      compoundMenu.setEnabled(false);
      transAbsMenuItem.setEnabled(false);
      frame = new JFrame("JSpecView");
      frame.setSize(getSize());
      final Dimension d;
      d = appletPanel.getSize();
      frame.add(appletPanel);
      frame.validate();
      frame.setVisible(true);
      remove(appletPanel);
      validate();
      repaint();
      frame.addMouseListener(new JSVPanelMouseListener());
      frame.addWindowListener(
          new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          appletPanel.setSize(d);
          getContentPane().add(appletPanel);
          setVisible(true);
          validate();
          repaint();
          integrateMenuItem.setEnabled(true);
   //       if (compoundMenuOn)
//             compoundMenu.setEnabled(true);
          frame.removeAll();
          frame.dispose();
          windowMenuItem.setSelected(false);
        }
      }
      );
    }
    else {
      // re-enable features that were disabled in Window mode
      integrateMenuItem.setEnabled(true);
      transAbsMenuItem.setEnabled(true);
      if (compoundMenuOn)
        compoundMenu.setEnabled(true);

      getContentPane().add(appletPanel);
      validate();
      repaint();
      frame.removeAll();
      frame.dispose();
    }
  }

  public boolean setStringParameter(String key, String value) {
    if (key == null)
      return false;
    try {
    if (key.equalsIgnoreCase("irMode")) {
      if ("transmittance".equalsIgnoreCase(value))
        TAConvert(TO_TRANS);
      else if ("absorbance".equalsIgnoreCase(value))
        TAConvert(TO_ABS);
      else if ("switch".equalsIgnoreCase(value))
        TAConvert(IMPLIED);
      else
        return false;
      return true;
    }
    } catch (Exception jsve) {
      // 
    }
    return false;
  }
  
  /**
   * Allows conversion between TRANSMITTANCE and ABSORBANCE
   * @param e ItemEvent
   */

  void transAbsMenuItem_itemStateChanged(ItemEvent e) {
    // for some reason, at the the St. Olaf site, this is triggering twice
    // when the user clicks the menu item. Why?
    try {
    System.out.println("ta event " + e.getID() + " " + e.getStateChange() + " " + ItemEvent.SELECTED + " " + ItemEvent.DESELECTED + " " + e.toString());
    if (e.getStateChange() == ItemEvent.SELECTED)
       TAConvert(IMPLIED);
    else
       TAConvert(IMPLIED);
    } catch (Exception jsve) {
      // ignore? 
    }
  }


  private long msTrigger = -1;
  
  /**
   * Allows Transmittance to Absorbance conversion or vice versa
   * depending on the value of comm.
   * @param comm the conversion command
   * @throws Exception 
   */

  
  private void TAConvert(int comm) throws Exception {
    long time = System.currentTimeMillis();
    System.out.println(time + " " + msTrigger + " " + (time-msTrigger));
    if (msTrigger > 0 && time - msTrigger < 100)
      return;
    msTrigger = time;
    JSVPanel jsvp = (JSVPanel) appletPanel.getComponent(0);
    if (jsvp.getNumberOfSpectra() > 1)
      return;
    JDXSpectrum spectrum = (JDXSpectrum) jsvp.getSpectrumAt(0);
    if (!spectrum.isContinuous())
      return;
    switch (comm) {
    case TO_ABS:
      if (!spectrum.isTransmittance())
        return;
      break;
    case TO_TRANS:
      if (!spectrum.isAbsorbance())
        return;
      break;
    case IMPLIED:
      break;
    default:
      return;
    }

      
      //  if successful, newSpec has the converted info
      JDXSpectrum newSpec = TransmittanceAbsorbanceConverter.convert(spectrum);

      //  if not Abs or Trans data or if there is a problem, return null
      if (newSpec == null)
        return;

      jsvp = new JSVPanel(newSpec);

      //  not working after loading a second file
      //  if grid turned off or zoom done first
      //  this returns full spectrum anyway

      appletPanel.remove(0);
      appletPanel.add(jsvp);

      initProperties(jsvp);
      solColMenuItem.setEnabled(true);
      jsvp.addMouseListener(new JSVPanelMouseListener());
      jsvp.repaint();

      //  now need to validate and repaint
      validate();
      repaint();

  }

  /**
     * Allows Integration of an HNMR spectrum
     * @param e the ItemEvent
     */
  void integrateMenuItem_itemStateChanged(ItemEvent e) {

      if (e.getStateChange() == ItemEvent.SELECTED) {
        tempJSVP = JSpecViewUtils.appletIntegrate(appletPanel, true);
      }
      else {
        if (JSpecViewUtils.hasIntegration((JSVPanel)appletPanel.getComponent(0))) {
          appletPanel.remove((JSVPanel)appletPanel.getComponent(0));
          tempJSVP = jsvPanels[currentSpectrumIndex];
          tempJSVP.setZoomEnabled(true);
          appletPanel.add(tempJSVP);
        }
      }

      initProperties(tempJSVP);
      tempJSVP.repaint();
      tempJSVP.addMouseListener(new JSVPanelMouseListener());

      chooseContainer();
    }

  // check which mode the spectrum is in (windowed or applet)
  private void chooseContainer() {
    // check first if we have ever had a frame
    if (frame == null) {
      integrateMenuItem.setEnabled(true);
      if (compoundMenuOn)
        compoundMenu.setEnabled(true);
      getContentPane().add(appletPanel);
      validate();
      repaint();
    } else {
      if (frame.getComponentCount() != 0) {
        integrateMenuItem.setEnabled(false);
        compoundMenu.setEnabled(false);
        frame.add(appletPanel);
        frame.validate();
        frame.setVisible(true);
      } else {
        integrateMenuItem.setEnabled(true);
        if (compoundMenuOn)
          compoundMenu.setEnabled(true);
        getContentPane().add(appletPanel);
        validate();
        repaint();
      }
    }
  }

  /**
   * Overlays the Spectra
   * @param e the ActionEvent
   */
  void overlayKeyMenuItem_actionPerformed(ActionEvent e) {
    new OverlayLegendDialog(selectedJSVPanel);
  }


  String dirLastExported;
  /**
   * Export spectrum in a given format
   * @param command the name of the format to export in
   */
  void exportSpectrum(String command) {
    final String comm = command;
    if (jFileChooser == null) {
      System.out.println(export(0, comm, null));
      // for now -- just send to output
      writeStatus("output sent to Java console");
      return;
    }
    dirLastExported = selectedJSVPanel.exportSpectra(frame, jFileChooser, comm, recentFileName, dirLastExported);

  }


  private String export(int n, String comm, File file) {
    if (n < 0 || selectedJSVPanel.getNumberOfSpectra() <= n)
      return "only " + selectedJSVPanel.getNumberOfSpectra() + " spectra available.";
    String errMsg = null;
    try{
      JDXSpectrum spec = (JDXSpectrum)selectedJSVPanel.getSpectrumAt(n);
      errMsg = Exporter.export(comm, (file == null ? null : file.getAbsolutePath()), spec, 
          0, spec.getXYCoords().length - 1);
    }
    catch(IOException ioe){
      errMsg = "Error writing: " + file.getName();
    }
    return errMsg;
  }

  /**
   * Shows an About dialog
   * @param e the ActionEvent
   */
  void versionMenuItem_actionPerformed(ActionEvent e) {

    //AboutDialog ab = new AboutDialog(null, "", false);
  }

  /**
   * Used to tile JSVPanel when the <i>interface</i> paramters is equal to "tile"
   * @param comps An array of components to tile
   * @return a <code>JSplitPane</code> with components tiled
   */
  public JSplitPane createSplitPane(JComponent[] comps){
    ComponentListPair pair = createPair(comps);
    return createSplitPaneAux(pair);
  }

  /**
   * Auxiliary method for creating a tiled interface
   * @param pair the <code>ComponentListPair</code>
   * @return a <code>JSplitPane</code> with components tiled
   */
  public JSplitPane createSplitPaneAux(ComponentListPair pair){
    int numTop = pair.top.length;
    int numBottom = pair.bottom.length;
    JSplitPane splitPane;

    if(numBottom == 1 && numTop == 1){
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      splitPane.setLeftComponent(pair.top[0]);
      splitPane.setRightComponent(pair.bottom[0]);

    }

    else if(numBottom == 1 && numTop == 2){
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      JSplitPane newSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      newSplitPane.setLeftComponent(pair.top[0]);
      newSplitPane.setRightComponent(pair.top[1]);
      splitPane.setLeftComponent(newSplitPane);
      splitPane.setRightComponent(pair.bottom[0]);
    }
    else{
      splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      splitPane.setTopComponent(createSplitPaneAux(createPair(pair.top)));
      splitPane.setBottomComponent(createSplitPaneAux(createPair(pair.bottom)));
    }
    return splitPane;
  }

  /**
   * Spits the comps array in 2 equal or nearly equal arrays an encapsulates them
   * in a <code>ComponentListPair</code> instance
   * @param comps an array of components
   * @return a <code>ComponentListPair</code>
   */
  public ComponentListPair createPair(JComponent[] comps){
    int numBottom = (int)(comps.length/2);
    int numTop = numBottom + (comps.length % 2);

    JComponent[] top = new JComponent[numTop];
    JComponent[] bottom = new JComponent[numBottom];

    int i;
    for(i = 0; i < numTop; i++){
      top[i] = comps[i];
    }

    for(int j = 0; i < comps.length; i++, j++){
      bottom[j] = comps[i];
    }

    ComponentListPair pair = new ComponentListPair();
    pair.bottom = bottom;
    pair.top = top;

    return pair;
  }

  /**
   * Representation of two array of components of equal or nearly equal size
   */
  class ComponentListPair{

    /**
     * the first array
     */
    public JComponent[] top;

    /**
     * the second array
     */
    public JComponent[] bottom;

    /**
     * Constructor
     */
    public ComponentListPair(){}
  }

  /**
   * Returns a <code>Color</color> object from a parameter value
   * @param key the parameter name
   * @param def the default value
   * @return a <code>Color</color> object from a parameter value
   */
/*  private Color getColorParameter(String key, Color def){
    String param = getParameter(key);
    int r, g, b;

    if(param == null){
      return def;
    }

    param = param.trim();
    if(param.startsWith("#")){
      try{
        return new Color(Integer.parseInt(param.substring(1), 16));
      }
      catch(NumberFormatException nfe){
        return def;
      }
    }
    StringTokenizer st = new StringTokenizer(param, ",;.- ");
    try{
      r = Integer.parseInt(st.nextToken().trim());
      g = Integer.parseInt(st.nextToken().trim());
      b = Integer.parseInt(st.nextToken().trim());

      return new Color(r, g, b);
    }
    catch(NoSuchElementException nsee){
      return def;
    }
    catch(NumberFormatException nfe){
      return def;
    }
  }

*/
  
  /**
   * Intialises the <code>plotColors</code> array from the
   * <i>plotColorsStr</i> variable
   */
  public void getPlotColors(){
    if(plotColorsStr != null){
      StringTokenizer st = new StringTokenizer(plotColorsStr, ",;.- ");
      int r, g, b;
      Vector<Color> colors = new Vector<Color>();

      try{
        while(st.hasMoreTokens()){

          String token = st.nextToken();
          if(token.startsWith("#")){
              colors.addElement(new Color(Integer.parseInt(token.substring(1), 16)));
          }
          else{
            r = Integer.parseInt(token.trim());
            g = Integer.parseInt(st.nextToken().trim());
            b = Integer.parseInt(st.nextToken().trim());
            colors.addElement(new Color(r, g, b));
          }
        }
      }
      catch(NoSuchElementException nsee){
        return;
      }
      catch(NumberFormatException nfe){
        return;
      }

      plotColors = (Color[])colors.toArray(new Color[colors.size()]);
    }
    else{
//      plotColors = new Color[specs.size()];
//      for(int i = 0; i < specs.size(); i++){
        plotColors[0] = plotColor;
//        System.out.println(i+" "+plotColors[i]);
      }
//    }
  }

  /**
   * Returns the current internal version of the Applet
   * @return String
   */
  public String getAppletVersion() {
    return JSVApplet.APPLET_VERSION;
  }

  /**
   * Returns the calculated colour of a visible spectrum (Transmittance)
   * @return Color
   */

  public String getSolnColour(){
    return this.sltnclr;
  }


  /**
   * Method that can be called from another applet or from javascript to
   * return the coordinate of clicked point in the plot area of the <code>
   * JSVPanel</code>
   * @return A String representation of the coordinate
   */
  public String getCoordinate(){
    if (selectedJSVPanel != null){
      Coordinate coord = selectedJSVPanel.getClickedCoordinate();

      if(coord != null)
        return coord.getXVal() + " " + coord.getYVal();
    }

    return "";
  }

  /**
   * Method that can be called from another applet or from javascript
   * that toggles the grid on a <code>JSVPanel</code>
   */
  public void toggleGrid(){
    if (selectedJSVPanel != null){
      selectedJSVPanel.setGridOn(!selectedJSVPanel.isGridOn());
      repaint();
    }
  }

  /**
   * Method that can be called from another applet or from javascript
   * that toggles the coordinate on a <code>JSVPanel</code>
   */
  public void toggleCoordinate(){
    if (selectedJSVPanel != null){
      selectedJSVPanel.setCoordinatesOn(!selectedJSVPanel.isCoordinatesOn());
      repaint();
    }
  }

  /**
     * Method that can be called from another applet or from javascript
     * that toggles reverses the plot on a <code>JSVPanel</code>
   */
  public void reversePlot(){
    if (selectedJSVPanel != null){
      selectedJSVPanel.setReversePlot(!selectedJSVPanel.isPlotReversed());
      repaint();
    }
  }

  /**
   * Gets a new set of parameters from a javascript call
   * @param newJSVparams String
   */
  public void script(String newJSVparams) {
    JSVparams = newJSVparams;
    newParams = true;
    init();
  }

  /**
   * Delivers spectrum coded as desired: XY, SQZ, PAC, DIF, AML, CML, SVG
   * @param type 
   * @param n 
   * @return data
   * 
   */
  public String export(String type, int n) {
    if (type == null)
      type = "XY";
    return export(n, type.toUpperCase(), null);
  }
  
  /**
    * Returns the spectrum at the specified block number
    * @param block int
  */
  public void setSpectrumNumber(int block){
    if (selectedJSVPanel != null){
      if (theInterface.equals("single")) {
        showSpectrum(block - 1);
        repaint();
      }
      else {
        spectraPane.setSelectedIndex(block - 1);
        repaint();
      }
    }
 }

  public void setFilePath(String tmpFilePath){
    executeCommandAsThread("load " + tmpFilePath);
  }
  
  /**
   * Loads a new file into the existing applet window
   * @param tmpFilePath String
  */
  void setFilePathLocal(String tmpFilePath){
     getContentPane().removeAll();
     appletPanel.removeAll();
     newFilePath = tmpFilePath;
     newFile = true;
     init();
     getContentPane().validate();
     appletPanel.validate();
  }


  /**
   * Loads in-line JCAMP-DX data into the existing applet window
   * @param data String
  */
  public void loadInline(String data){
     getContentPane().removeAll();
     appletPanel.removeAll();
     newFilePath = null;
     newFile = false;
     init(data);
     getContentPane().validate();
     appletPanel.validate();
  }

  /**
   * Method that can be called from another applet or from javascript
   * that adds a highlight to a portion of the plot area of a <code>JSVPanel</code>
   * @param x1 the starting x value
   * @param x2 the ending x value
   * @param r the red portion of the highlight color
   * @param g the green portion of the highlight color
   * @param b the blue portion of the highlight color
   * @param a the alpha portion of the highlight color
   */
  public void addHighlight(double x1, double x2, int r, int g, int b, int a){
    if (selectedJSVPanel != null){
      selectedJSVPanel.setHighlightOn(true);
      Color color = new Color(r, g, b, a);
      selectedJSVPanel.addHighlight(x1, x2, color);
      repaint();
    }
  }

  /**
   * Method that can be called from another applet or from javascript
   * that removes a highlight from the plot area of a <code>JSVPanel</code>
   * @param x1 the starting x value
   * @param x2 the ending x value
   */
  public void removeHighlight(double x1, double x2){
    if (selectedJSVPanel != null){
      selectedJSVPanel.removeHighlight(x1, x2);
      repaint();
    }
  }

  /**
   * Method that can be called from another applet or from javascript
   * that removes all highlights from the plot area of a <code>JSVPanel</code>
   */
  public void removeAllHighlights(){
    if (selectedJSVPanel != null){
      selectedJSVPanel.removeAllHighlights();
      repaint();
    }
  }

  /**
   * Calls a javascript function given by the function name
   * passing to it the string prarmeters as arguments
   * @param function the javascript function name
   * @param parameters the function arguments as a string in the form "x, y, z..."
   */
  public void callToJavaScript(String function, String parameters) {
    try {
      JSObject win = JSObject.getWindow(this);
      win.eval(function + "(" + parameters + ")");
    }
    catch (Exception npe) {
      System.out.println("EXCEPTION-> " + npe.getMessage());
    }
  }

  /**
   * Parses the javascript call parameters and executes them accordingly
   * @param params String
   */
  public void parseInitScript(String params) {
    StringTokenizer allParamTokens = new StringTokenizer(params, ";");

    if (JSpecViewUtils.DEBUG) {
      System.out.println("Running in DEBUG mode");
    }

    while (allParamTokens.hasMoreTokens()) {
      String token = allParamTokens.nextToken();

      // now split the key/value pair
      StringTokenizer eachParam = new StringTokenizer(token);

      String key = eachParam.nextToken();
      if (key.equalsIgnoreCase("SET"))
        key = eachParam.nextToken();
      key = key.toUpperCase();
      String value = eachParam.nextToken();

      if (JSpecViewUtils.DEBUG) {
        System.out.println("KEY-> " + key + " VALUE-> " + value);
      }

      Integer iparam = (Integer) htParams.get(key);
      try {
        switch (iparam == null ? -1 : iparam.intValue()) {
        case -1:
          break;
        case PARAM_LOAD:
          filePath = value;
          break;
        case PARAM_REVERSEPLOT:
          reversePlot = Boolean.parseBoolean(value);
          break;
        case PARAM_COORDINATESON:
          coordinatesOn = Boolean.parseBoolean(value);
          break;
        case PARAM_GRIDON:
          gridOn = Boolean.parseBoolean(value);
          break;
        case PARAM_COORDCALLBACKFUNCTIONNAME:
          coordCallbackFunctionName = value;
          break;

        case PARAM_SPECTRUMNUMBER:
          spectrumNumber = Integer.parseInt(value);
        case PARAM_INTERFACE:
          theInterface = value;
          if (!theInterface.equals("tab") && !theInterface.equals("tile")
              && !theInterface.equals("single")
              && !theInterface.equals("overlay"))
            theInterface = "single";
          break;
        case PARAM_ENDINDEX:
          endIndex = Integer.parseInt(value);
          break;
        case PARAM_ENABLEZOOM:
          enableZoom = Boolean.parseBoolean(value);
          break;
        case PARAM_STARTINDEX:
          startIndex = Integer.parseInt(value);
          break;
        case PARAM_MENUON:
          menuOn = Boolean.parseBoolean(value);
          break;
        case PARAM_COMPOUNDMENUON:
          compoundMenuOn2 = Boolean.parseBoolean(value);
          break;
        case PARAM_BACKGROUNDCOLOR:
          backgroundColor = JSpecViewUtils.getColorFromString(value);
          break;
        case PARAM_COORDINATESCOLOR:
          coordinatesColor = JSpecViewUtils.getColorFromString(value);
        case 175:
        case PARAM_GRIDCOLOR:
          gridColor = JSpecViewUtils.getColorFromString(value);
          break;
        case PARAM_PLOTAREACOLOR:
          plotAreaColor = JSpecViewUtils.getColorFromString(value);
          break;
        case PARAM_PLOTCOLOR:
          plotColor = JSpecViewUtils.getColorFromString(value);
          break;
        case PARAM_SCALECOLOR:
          scaleColor = JSpecViewUtils.getColorFromString(value);
          break;
        case PARAM_TITLECOLOR:
          titleColor = JSpecViewUtils.getColorFromString(value);
          break;
        case PARAM_UNITSCOLOR:
          unitsColor = JSpecViewUtils.getColorFromString(value);
          break;
        case PARAM_PEAKCALLBACKFUNCTIONNAME:
          peakCallbackFunctionName = value;
          break;
        case PARAM_PLOTCOLORS:
        case PARAM_VERSION:
          break;
        case PARAM_IRMODE:
          irMode = value;
          break;
        }
      } catch (Exception e) {
      }
    }

  }

  class ExecuteCommandThread extends Thread {

    String strCommand;
    ExecuteCommandThread (String command) {
      strCommand = command;
      this.setName("ExecuteCommandThread");
    }
    
    @Override
    public void run() {
      System.out.println("command thread: " + strCommand);
      try {
        if (strCommand.startsWith("load "))
        setFilePathLocal(strCommand.substring(5));
      } catch (Exception ie) {
        Logger.error("execution command interrupted!",ie);
      }
    }
  }
   
  ExecuteCommandThread execThread;
  void executeCommandAsThread(String strCommand){ 
    if (strCommand.length() > 0) {
      execThread = new ExecuteCommandThread(strCommand);
      execThread.start();
      //can't do this: 
      //SwingUtilities.invokeLater(execThread);
      //because then the thread runs from the event queue, and that 
      //causes PAUSE to hang the application on refresh()
    }
  }


}
