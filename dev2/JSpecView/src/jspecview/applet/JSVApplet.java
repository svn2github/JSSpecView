/* Copyright (c) 2002-2012 The University of the West Indies
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
// 08-01-2010 need bugfix for protected static reverseplot
// 17-03-2010 fix for NMRShiftDB CML files
// 11-06-2011 fix for LINK files and reverseplot 
// 23-07-2011 jak - Added parameters for the visibility of x units, y units,
//			  		x scale, and y scale.  Added parameteres for the font,
//			  		title font, and integral plot color.  Added a method
//			  		to reset view from a javascript call.
// 24-09-2011 jak - Added parameter for integration ratio annotations.
// 08-10-2011 jak - Add a method to toggle integration from a javascript
//					call. Changed behaviour to remove integration after reset
//					view.

package jspecview.applet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.ArrayList;

import javax.swing.JApplet;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jspecview.application.common.AppUtils;
import jspecview.application.common.JSVPanel;
import jspecview.application.common.OverlayLegendDialog;
import jspecview.application.common.PeakPickedEvent;
import jspecview.application.common.PeakPickedListener;
import jspecview.application.common.PrintLayoutDialog;
import jspecview.common.Coordinate;
import jspecview.common.Graph;
import jspecview.common.IntegrationRatio;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSpecViewUtils;
import jspecview.common.PeakInfo;
import jspecview.common.TransmittanceAbsorbanceConverter;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.export.Exporter;
import jspecview.source.JDXFileReader;
import jspecview.source.JDXSource;
import jspecview.util.Escape;
import jspecview.util.Logger;
import jspecview.util.Parser;
import netscape.javascript.JSObject;
import jspecview.common.Visible;

/**
 * JSpecView Applet class. For a list of parameters and scripting functionality
 * see the file JSpecView_Applet_Specification.html.
 * 
 * @author Bob Hanson
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A. D. Walters
 * @author Prof Robert J. Lancashire
 */

public class JSVApplet extends JApplet implements PeakPickedListener {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Override
  public void finalize() {
    System.out.println("JSpecView " + this + " finalized");
  }

  public static final String APPLET_VERSION = "1.0.20120216-0500";

  /* --------------------set default-PARAMETERS -------------------------*/
  String filePath, oldfilePath;
  String newFilePath = null;
  private String recentFileName = "";
  private String recentURL = "";

  boolean gridOn = true;
  boolean coordinatesOn = true;
  boolean reversePlot = false;
  boolean menuOn = true;
  boolean compoundMenuOn = true;
  boolean compoundMenuOn2 = true;
  boolean enableZoom = true;
  boolean xScaleOn = true;
  boolean yScaleOn = true;
  boolean xUnitsOn = true;
  boolean yUnitsOn = true;
  boolean titleBoldOn = false;

  int startIndex = -1;
  int endIndex = -1;
  int spectrumNumber = -1; // blockNumber or nTupleNumber
  int numberOfSpectra;

  String theInterface = "single"; // either tab, tile, single, overlay
  private String coordCallbackFunctionName;
  private String peakCallbackFunctionName;
  private String syncCallbackFunctionName;
  private String appletReadyCallbackFunctionName;

  String sltnclr = "255,255,255"; //Colour of Solution
  String titleFontName = null; // Title Font
  String displayFontName = null; // Display Font

  ArrayList<IntegrationRatio> integrationRatios = null; // Integration Ratio Annotations

  Color titleColor = Color.BLACK;
  Color gridColor = Color.LIGHT_GRAY;
  Color unitsColor = Color.RED;
  Color scaleColor = Color.BLACK;
  Color coordinatesColor = Color.RED;
  Color plotAreaColor = Color.WHITE;
  Color backgroundColor = new Color(192, 192, 192);
  Color plotColor = Color.BLUE;

  Color[] plotColors = { Color.blue, Color.green, Color.red, Color.magenta,
      Color.yellow, Color.orange, Color.pink, Color.cyan };
  String plotColorsStr;
  Color integralPlotColor = Color.red;

  final private int TO_TRANS = 0;
  final private int TO_ABS = 1;
  final private int IMPLIED = 2;

  /*---------------------------------END PARAMETERS------------------------*/

  boolean isSignedApplet = false;
  boolean isStandalone = false;
  BorderLayout appletBorderLayout = new BorderLayout();
  JPanel statusPanel = new JPanel();
  JLabel statusTextLabel = new JLabel();
  JFileChooser jFileChooser;
  JSVPanel selectedJSVPanel;
  JSVAppletPopupMenu appletPopupMenu;
  JDXSpectrum xmlSpec;
  JSVPanel tempJSVP;

  private static final String[] params = { "LOAD", "REVERSEPLOT",
      "COORDINATESON", "GRIDON", "COORDCALLBACKFUNCTIONNAME", "SPECTRUMNUMBER",
      "INTERFACE", "ENDINDEX", "ENABLEZOOM", "STARTINDEX", "MENUON",
      "COMPOUNDMENUON", "BACKGROUNDCOLOR", "COORDINATESCOLOR", "GRIDCOLOR",
      "PLOTAREACOLOR", "PLOTCOLOR", "SCALECOLOR", "TITLECOLOR", "UNITSCOLOR",
      "PLOTCOLORS", "VERSION", "PEAKCALLBACKFUNCTIONNAME", "IRMODE", "OBSCURE",
      "XSCALEON", "YSCALEON", "XUNITSON", "YUNITSON", "INTEGRALPLOTCOLOR",
      "TITLEFONTNAME", "TITLEBOLDON", "DISPLAYFONTNAME", "INTEGRATIONRATIOS",
      "APPLETREADYCALLBACKFUNCTIONNAME", "APPLETID", "SYNCID",
      "SYNCCALLBACKFUNCTIONNAME" };

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
  final private static int PARAM_OBSCURE = 24;
  final private static int PARAM_XSCALEON = 25;
  final private static int PARAM_YSCALEON = 26;
  final private static int PARAM_XUNITSON = 27;
  final private static int PARAM_YUNITSON = 28;
  final private static int PARAM_INTEGRALPLOTCOLOR = 29;
  final private static int PARAM_TITLEFONTNAME = 30;
  final private static int PARAM_TITLEBOLDON = 31;
  final private static int PARAM_DISPLAYFONTNAME = 32;
  final private static int PARAM_INTEGRATIONRATIOS = 33;
  final private static int PARAM_APPLETREADYCALLBACKFUNCTIONNAME = 34;
  final private static int PARAM_APPLETID = 35;
  final private static int PARAM_SYNCID = 36;
  final private static int PARAM_SYNCCALLBACKFUNCTIONNAME = 37;

  final private static Hashtable<String, Integer> htParams = new Hashtable<String, Integer>();
  static {
    for (int i = 0; i < params.length; i++)
      htParams.put(params[i], new Integer(i));
  }

  //"ADDHIGHLIGHT", "REMOVEHIGHLIGHT", "REMOVEALLHIGHTLIGHTS"

  @Override
  public void destroy() {
    System.out.println("JSVApplet " + this + " destroy 1");
    if (commandWatcherThread != null) {
      commandWatcherThread.interrupt();
      commandWatcherThread = null;
    }
    if (jsvPanels != null) {
      for (int i = 0; i < jsvPanels.length; i++)
        if (jsvPanels[i] != null) {
          jsvPanels[i].destroy();
          jsvPanels[i] = null;
        }
    }
    if (tempJSVP != null)
      tempJSVP.destroy();
    tempJSVP = null;
    System.out.println("JSVApplet " + this + " destroy 2");
  }

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
  public int currentSpectrumIndex = 0;

  /**
   * Whether or not spectra should be overlayed
   */
  boolean overlay;
  boolean obscure;

  String irMode;

  /**
   * Returns a parameter value
   * 
   * @param key
   *        the parameter name
   * @param def
   *        the default value. If param is not found then this is returned
   * @return a parameter value
   */
  public String getParameter(String key, String def) {
    return isStandalone ? System.getProperty(key, def)
        : (getParameter(key) != null ? getParameter(key) : def);
  }

  private String appletID;
  private String syncID;

  /**
   * Initializes applet with parameters and load the <code>JDXSource</code>
   */
  @Override
  public void init() {
    init(null);
    if (appletReadyCallbackFunctionName != null && fullName != null)
      callToJavaScript(appletReadyCallbackFunctionName, new Object[] {
          appletID, fullName, Boolean.TRUE });
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
    appletPopupMenu = new JSVAppletPopupMenu(this, isSignedApplet);
    appletPopupMenu.enableMenus(menuOn, enableZoom);
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
    openDataOrFile(data);
  }

  /**
   * Initializes the applet's GUI components
   * 
   * @throws Exception
   */
  private void jbInit() throws Exception {
    try {
      jFileChooser = new JFileChooser();
      isSignedApplet = true;
      startCommandThread();
    } catch (SecurityException se) {
      //System.err.println("Export menu disabled:
      //You need the signed Applet to export files");
    }

    statusTextLabel.setText("Loading...");

  }

  /**
   * Get Applet information
   * 
   * @return the String "JSpecView Applet"
   */
  @Override
  public String getAppletInfo() {
    return "JSpecView Applet " + APPLET_VERSION;
  }

  /**
   * Returns null
   * 
   * @return null
   */
  @Override
  public String[][] getParameterInfo() {
    return null;
  }

  /**
   * Initalizes the <code>JSVPanels</code> and adds them to the jsvPanels array
   * 
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
      initProperties(jsvp, 0);
      selectedJSVPanel = jsvp;
      jsvp.setIndex(currentSpectrumIndex = 0);
    } else {
      // initalise JSVPanels and add them to the array
      jsvPanels = new JSVPanel[numberOfSpectra];
      try {
        for (int i = 0; i < numberOfSpectra; i++) {
          if (showRange)
            jsvp = new JSVPanel(specs.get(i), startIndex,
                endIndex);
          else
            jsvp = new JSVPanel(specs.get(i));
          jsvPanels[i] = jsvp;
          initProperties(jsvp, i);
        }
      } catch (Exception e) {
        // TODO
      }
    }
  }

  private void initProperties(JSVPanel jsvp, int index) {
    // set JSVPanel properties from applet parameters
    jsvp.addPeakPickedListener(this);
    jsvp.setIndex(index);
    jsvp.setGridOn(gridOn);
    jsvp.setCoordinatesOn(coordinatesOn);
    jsvp.setXScaleOn(xScaleOn);
    jsvp.setYScaleOn(yScaleOn);
    jsvp.setXUnitsOn(xUnitsOn);
    jsvp.setYUnitsOn(yUnitsOn);
    jsvp.setTitleBoldOn(titleBoldOn);
    jsvp.setTitleFontName(titleFontName);
    jsvp.setDisplayFontName(displayFontName);
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
    getPlotColors(); //<======= Kind of sloppy
    //if(plotColors == null)
    //  jsvp.setPlotColors(plotColors);
    jsvp.setXAxisDisplayedIncreasing(((JDXSpectrum) jsvp.getSpectrumAt(0)).shouldDisplayXAxisIncreasing());
    jsvp.setSource(source);
    jsvp.setPopup(appletPopupMenu);

  }

  /**
   * Initializes the interface of the applet depending on the value of the
   * <i>interface</i> parameter
   */
  private void initInterface() {
    final int numberOfPanels = jsvPanels.length;
    boolean canDoTile = (numberOfPanels >= 2 && numberOfPanels <= 10);
    boolean moreThanOnePanel = numberOfPanels > 1;
    boolean showSpectrumNumber = spectrumNumber != -1
        && spectrumNumber <= numberOfPanels;
    //appletPanel.setBackground(backgroundColor);
    if (theInterface.equals("tab") && moreThanOnePanel) {
      spectraPane = new JTabbedPane(SwingConstants.TOP,
          JTabbedPane.SCROLL_TAB_LAYOUT);
      appletPanel.add(new JLabel(source.getTitle(),
          SwingConstants.CENTER), BorderLayout.NORTH);
      appletPanel.add(spectraPane, BorderLayout.CENTER);

      for (int i = 0; i < numberOfPanels; i++) {
        String title = specs.get(i).getTitleLabel();
        if (source.type == JDXSource.TYPE_NTUPLE)
          title = title.substring(title.indexOf(':') + 1);
        else if (source.type == JDXSource.TYPE_BLOCK)
          //title = "block " + (i + 1);
          title = title.substring(0, (title.length() >= 10 ? 10 : title
              .length()))
              + "... : ";
        spectraPane.addTab(title, jsvPanels[i]);
      }
      // Show the spectrum specified by the spectrumnumber parameter
      if (showSpectrumNumber) {
        spectraPane.setSelectedIndex(spectrumNumber - 1);
      }
      setSelectedPanel((JSVPanel) spectraPane.getSelectedComponent());
      spectraPane.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          setSelectedPanel((JSVPanel) spectraPane.getSelectedComponent());
        }
      });
    } else if (theInterface.equals("tile") && canDoTile) {
      appletPanel.add(new JLabel(source.getTitle(),
          SwingConstants.CENTER), BorderLayout.NORTH);

      for (int i = 0; i < numberOfPanels; i++) {
        jsvPanels[i].setMinimumSize(new Dimension(250, 150));
        jsvPanels[i].setPreferredSize(new Dimension(300, 200));
      }
      JSplitPane splitPane = createSplitPane(jsvPanels);
      appletPanel.add(splitPane, BorderLayout.CENTER);
      //splitPane.setBackground(backgroundColor);
    } else { // Single or overlay
      //      compoundMenuOn = true;
      int spectrumIndex;
      String title;
      if (showSpectrumNumber)
        spectrumIndex = spectrumNumber - 1;
      else
        spectrumIndex = 0;
      setSelectedPanel(jsvPanels[spectrumIndex]);

      // Global variable for single interface
      currentSpectrumIndex = spectrumIndex;
      if (overlay && source.isCompoundSource) {
        title = source.getTitle();
        jsvPanels[spectrumIndex].setTitle(title);
        appletPopupMenu.overlayKeyMenuItem.setEnabled(true);
      }
      appletPanel.add(jsvPanels[spectrumIndex], BorderLayout.CENTER);
      // else interface = single
      if (moreThanOnePanel && compoundMenuOn) {
        if (numberOfPanels <= 20) {
          // add Menus to navigate
          JCheckBoxMenuItem mi;
          if (source.isCompoundSource) {
            for (int i = 0; i < numberOfPanels; i++) {
              title = (i + 1) + "- " + specs.get(i).getTitleLabel();
              mi = new JCheckBoxMenuItem(title);
              mi.setSelected(i == currentSpectrumIndex);
              mi.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                  if (e.getStateChange() == ItemEvent.SELECTED) {
                    // deselects the previously selected menu item
                    JCheckBoxMenuItem deselectedMenu = (JCheckBoxMenuItem) ((JCheckBoxMenuItem) e
                        .getSource()).getParent().getComponent(
                        currentSpectrumIndex);
                    deselectedMenu.setSelected(false);
                    compoundMenu_itemStateChanged(e);
                  }
                }
              });
              mi.setActionCommand("" + i);
              appletPopupMenu.compoundMenu.add(mi);
            }
            appletPopupMenu.compoundMenu
                .setText(source.type == JDXSource.TYPE_BLOCK ? "Blocks" : "NTuples");
          }
          // add compound menu to popup menu
          appletPopupMenu.add(appletPopupMenu.compoundMenu, 3);
          if (compoundMenuOn)
            appletPopupMenu.compoundMenu.setEnabled(true);
        } else {
          // open dialog box
          JMenuItem compoundMi = new JMenuItem("Choose Spectrum");
          compoundMi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              StringBuffer msgStrBuffer = new StringBuffer();
              msgStrBuffer.append("Choose a number between 1 and ");
              msgStrBuffer.append(numberOfPanels);
              msgStrBuffer.append(" to display another spectrum");

              String str = JOptionPane.showInputDialog(JSVApplet.this,
                  msgStrBuffer.toString(), "Spectrum Chooser",
                  JOptionPane.PLAIN_MESSAGE);
              if (str != null) {
                int index = 0;
                try {
                  index = Integer.parseInt(str) - 1;
                } catch (NumberFormatException nfe) {
                }

                if (index > 0 && index < numberOfPanels) {
                  showSpectrum(index, true);
                  writeStatus(" ");
                } else
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
   * 
   * @param e
   *        the ItemEvent
   */
  void compoundMenu_itemStateChanged(ItemEvent e) {

    // gets the newly selected title
    JCheckBoxMenuItem selectedMenu = (JCheckBoxMenuItem) e.getSource();
    String txt = selectedMenu.getText();

    // shows the newly selected block
    int index = Integer.parseInt(txt.substring(0, txt.indexOf("-")));
    //sltnclr = Visible.Colour(source);
    showSpectrum(index - 1, true);
  }

  /**
   * Shows the </code>JSVPanel</code> at a certain index
   * 
   * @param index
   *        the index
   */
  void showSpectrum(int index, boolean fromMenu) {
    JSVPanel jsvp = jsvPanels[index];
    appletPanel.remove(jsvPanels[currentSpectrumIndex]);
    appletPanel.add(jsvp, BorderLayout.CENTER);
    currentSpectrumIndex = index;
    setSelectedPanel(jsvp);

    if (((JDXSpectrum) selectedJSVPanel.getSpectrumAt(0)).isHNMR())
      appletPopupMenu.integrateMenuItem.setEnabled(true);
    else
      appletPopupMenu.integrateMenuItem.setEnabled(false);

    chooseContainer();
    this.validate();
    repaint();

    if (fromMenu)
      sendFrameChange(jsvp);

  }

  /**
   * Writes a message to the status label
   * 
   * @param msg
   *        the message
   */
  public void writeStatus(String msg) {
    statusTextLabel.setText(msg);
  }

  /**
   * Shows the header information for the Spectrum
   * 
   * @param e
   *        the ActionEvent
   */
  void headerMenuItem_actionPerformed(ActionEvent e) {
    
    JDXSpectrum spectrum = (JDXSpectrum) selectedJSVPanel.getSpectrumAt(0);
    Object[][] rowData = (overlay ? source
        .getHeaderRowDataAsArray(false, 0) : spectrum
        .getHeaderRowDataAsArray());
    String[] columnNames = { "Label", "Description" };
    JTable table = new JTable(rowData, columnNames);
    table.setPreferredScrollableViewportSize(new Dimension(400, 195));
    JScrollPane scrollPane = new JScrollPane(table);
    JOptionPane.showMessageDialog(this, scrollPane, "Header Information",
        JOptionPane.PLAIN_MESSAGE);
  }

  /**
   * Calculates the predicted colour of the Spectrum
   * 
   * @param e
   *        the ActionEvent
   */
  void solColMenuItem_actionPerformed(ActionEvent e) {

    Graph spectrum = selectedJSVPanel.getSpectrumAt(0);
    String Yunits = spectrum.getYUnits();
    //    System.out.println(spectrum.getTitle());
    sltnclr = Visible.Colour(spectrum.getXYCoords(), Yunits);

    //JScrollPane scrollPane = new JScrollPane();

    JOptionPane.showMessageDialog(this, "<HTML><body bgcolor=rgb(" + sltnclr
        + ")><br />Predicted Solution Colour- RGB(" + sltnclr
        + ")<br /><br /></body></HTML>", "Predicted Colour",
        JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Opens the print dialog to enable printing
   * 
   * @param e
   *        ActionEvent
   */
  void printMenuItem_actionPerformed(ActionEvent e) {
    if (frame == null) {
      System.err
          .println("Use the View/Window menu to lift the spectrum off the page first.");
      return;
    }

    JSVPanel jsvp = selectedJSVPanel;

    PrintLayoutDialog ppd = new PrintLayoutDialog(frame);
    PrintLayoutDialog.PrintLayout pl = ppd.getPrintLayout();

    if (pl != null)
      jsvp.printSpectrum(pl);
  }

  /**
   * Clears all zoomed views
   * 
   * @param e
   *        the ActionEvent
   */
  /*  void clearMenuItem_actionPerformed(ActionEvent e) {
      selectedJSVPanel.clearViews();
    }
  */
  /**
   * Shows the applet in a Frame
   * 
   * @param e
   *        the ActionEvent
   */
  void windowMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      // disable some features when in Window mode
      appletPopupMenu.integrateMenuItem.setEnabled(false);
      appletPopupMenu.compoundMenu.setEnabled(false);
      appletPopupMenu.transAbsMenuItem.setEnabled(false);
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
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          appletPanel.setSize(d);
          getContentPane().add(appletPanel);
          setVisible(true);
          validate();
          repaint();
          appletPopupMenu.integrateMenuItem.setEnabled(true);
          //       if (compoundMenuOn)
          //             compoundMenu.setEnabled(true);
          frame.removeAll();
          frame.dispose();
          appletPopupMenu.windowMenuItem.setSelected(false);
        }
      });
    } else {
      // re-enable features that were disabled in Window mode
      appletPopupMenu.integrateMenuItem.setEnabled(true);
      appletPopupMenu.transAbsMenuItem.setEnabled(true);
      if (compoundMenuOn)
        appletPopupMenu.compoundMenu.setEnabled(true);

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
   * 
   * @param e
   *        ItemEvent
   */

  void transAbsMenuItem_itemStateChanged(ItemEvent e) {
    // for some reason, at the the St. Olaf site, this is triggering twice
    // when the user clicks the menu item. Why?
    try {
      System.out.println("ta event " + e.getID() + " " + e.getStateChange()
          + " " + ItemEvent.SELECTED + " " + ItemEvent.DESELECTED + " "
          + e.toString());
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
   * Allows Transmittance to Absorbance conversion or vice versa depending on
   * the value of comm.
   * 
   * @param comm
   *        the conversion command
   * @throws Exception
   */

  private void TAConvert(int comm) throws Exception {
    long time = System.currentTimeMillis();
    System.out.println(time + " " + msTrigger + " " + (time - msTrigger));
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

    initProperties(jsvp, 0);
    appletPopupMenu.solColMenuItem.setEnabled(true);
    jsvp.repaint();

    //  now need to validate and repaint
    validate();
    repaint();

  }

  /**
   * Allows Integration of an HNMR spectrum
   * 
   * @param e
   *        the ItemEvent
   */
  void integrateMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      tempJSVP = AppUtils.appletIntegrate(appletPanel, true, integrationRatios);
    } else {
      if (AppUtils.hasIntegration((JSVPanel) appletPanel.getComponent(0))) {
        appletPanel.remove((JSVPanel) appletPanel.getComponent(0));
        tempJSVP = jsvPanels[currentSpectrumIndex];
        tempJSVP.setZoomEnabled(true);
        appletPanel.add(tempJSVP);
      }
    }

    initProperties(tempJSVP, currentSpectrumIndex);
    tempJSVP.repaint();
    chooseContainer();
  }

  // check which mode the spectrum is in (windowed or applet)
  private void chooseContainer() {
    // check first if we have ever had a frame
    if (frame == null) {
      appletPopupMenu.integrateMenuItem.setEnabled(true);
      if (compoundMenuOn)
        appletPopupMenu.compoundMenu.setEnabled(true);
      getContentPane().add(appletPanel);
      validate();
      repaint();
    } else {
      if (frame.getComponentCount() != 0) {
        appletPopupMenu.integrateMenuItem.setEnabled(false);
        appletPopupMenu.compoundMenu.setEnabled(false);
        frame.add(appletPanel);
        frame.validate();
        frame.setVisible(true);
      } else {
        appletPopupMenu.integrateMenuItem.setEnabled(true);
        if (compoundMenuOn)
          appletPopupMenu.compoundMenu.setEnabled(true);
        getContentPane().add(appletPanel);
        validate();
        repaint();
      }
    }
  }

  /**
   * Overlays the Spectra
   * 
   * @param e
   *        the ActionEvent
   */
  void overlayKeyMenuItem_actionPerformed(ActionEvent e) {
    new OverlayLegendDialog(selectedJSVPanel);
  }

  private String dirLastExported;

  private String fullName;

  /**
   * Export spectrum in a given format
   * 
   * @param command
   *        the name of the format to export in
   */
  void exportSpectrum(String command) {
    final String comm = command;
    if (!isSignedApplet) {
      System.out.println(export(0, comm, null));
      // for now -- just send to output
      writeStatus("output sent to Java console");
      return;
    }
    dirLastExported = selectedJSVPanel.exportSpectra(frame, jFileChooser, comm,
        recentFileName, dirLastExported);
  }

  private String export(int n, String comm, File file) {
    if (n < 0 || selectedJSVPanel.getNumberOfSpectra() <= n)
      return "only " + selectedJSVPanel.getNumberOfSpectra()
          + " spectra available.";
    String errMsg = null;
    try {
      JDXSpectrum spec = (JDXSpectrum) selectedJSVPanel.getSpectrumAt(n);
      errMsg = Exporter.export(comm, (file == null ? null : file
          .getAbsolutePath()), spec, 0, spec.getXYCoords().length - 1);
    } catch (IOException ioe) {
      errMsg = "Error writing: " + file.getName();
    }
    return errMsg;
  }

  /**
   * Shows an About dialog
   * 
   * @param e
   *        the ActionEvent
   */
  void versionMenuItem_actionPerformed(ActionEvent e) {

    //AboutDialog ab = new AboutDialog(null, "", false);
  }

  /**
   * Used to tile JSVPanel when the <i>interface</i> paramters is equal to
   * "tile"
   * 
   * @param comps
   *        An array of components to tile
   * @return a <code>JSplitPane</code> with components tiled
   */
  public JSplitPane createSplitPane(JComponent[] comps) {
    ComponentListPair pair = createPair(comps);
    return createSplitPaneAux(pair);
  }

  /**
   * Auxiliary method for creating a tiled interface
   * 
   * @param pair
   *        the <code>ComponentListPair</code>
   * @return a <code>JSplitPane</code> with components tiled
   */
  public JSplitPane createSplitPaneAux(ComponentListPair pair) {
    int numTop = pair.top.length;
    int numBottom = pair.bottom.length;
    JSplitPane splitPane;

    if (numBottom == 1 && numTop == 1) {
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      splitPane.setLeftComponent(pair.top[0]);
      splitPane.setRightComponent(pair.bottom[0]);

    }

    else if (numBottom == 1 && numTop == 2) {
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      JSplitPane newSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      newSplitPane.setLeftComponent(pair.top[0]);
      newSplitPane.setRightComponent(pair.top[1]);
      splitPane.setLeftComponent(newSplitPane);
      splitPane.setRightComponent(pair.bottom[0]);
    } else {
      splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      splitPane.setTopComponent(createSplitPaneAux(createPair(pair.top)));
      splitPane.setBottomComponent(createSplitPaneAux(createPair(pair.bottom)));
    }
    return splitPane;
  }

  /**
   * Splits the components array in 2 equal or nearly equal arrays and
   * encapsulates them in a <code>ComponentListPair</code> instance
   * 
   * @param comps
   *        an array of components
   * @return a <code>ComponentListPair</code>
   */
  public ComponentListPair createPair(JComponent[] comps) {
    int numBottom = (int) (comps.length / 2);
    int numTop = numBottom + (comps.length % 2);

    JComponent[] top = new JComponent[numTop];
    JComponent[] bottom = new JComponent[numBottom];

    int i;
    for (i = 0; i < numTop; i++) {
      top[i] = comps[i];
    }

    for (int j = 0; i < comps.length; i++, j++) {
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
  class ComponentListPair {

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
    public ComponentListPair() {
    }
  }

  /**
   * Returns a <code>Color</color> object from a parameter value
   * 
   * @param key
   *        the parameter name
   * @param def
   *        the default value
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
   * Intialises the <code>plotColors</code> array from the <i>plotColorsStr</i>
   * variable
   */
  public void getPlotColors() {
    if (plotColorsStr != null) {
      StringTokenizer st = new StringTokenizer(plotColorsStr, ",;.- ");
      int r, g, b;
      Vector<Color> colors = new Vector<Color>();

      try {
        while (st.hasMoreTokens()) {

          String token = st.nextToken();
          if (token.startsWith("#")) {
            colors.addElement(new Color(Integer
                .parseInt(token.substring(1), 16)));
          } else {
            r = Integer.parseInt(token.trim());
            g = Integer.parseInt(st.nextToken().trim());
            b = Integer.parseInt(st.nextToken().trim());
            colors.addElement(new Color(r, g, b));
          }
        }
      } catch (NoSuchElementException nsee) {
        return;
      } catch (NumberFormatException nfe) {
        return;
      }

      plotColors = (Color[]) colors.toArray(new Color[colors.size()]);
    } else {
      //      plotColors = new Color[specs.size()];
      //      for(int i = 0; i < specs.size(); i++){
      plotColors[0] = plotColor;
      //        System.out.println(i+" "+plotColors[i]);
    }
    //    }
  }

  /**
   * Returns the current internal version of the Applet
   * 
   * @return String
   */
  public String getAppletVersion() {
    return JSVApplet.APPLET_VERSION;
  }

  /**
   * Returns the calculated colour of a visible spectrum (Transmittance)
   * 
   * @return Color
   */

  public String getSolnColour() {
    return this.sltnclr;
  }

  /**
   * Method that can be called from another applet or from javascript to return
   * the coordinate of clicked point in the plot area of the <code>
   * JSVPanel</code>
   * 
   * @return A String representation of the coordinate
   */
  public String getCoordinate() {
    if (selectedJSVPanel != null) {
      Coordinate coord = selectedJSVPanel.getClickedCoordinate();

      if (coord != null)
        return coord.getXVal() + " " + coord.getYVal();
    }

    return "";
  }

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the grid on a <code>JSVPanel</code>
   */
  public void toggleGrid() {
    if (selectedJSVPanel != null) {
      selectedJSVPanel.setGridOn(!selectedJSVPanel.isGridOn());
      repaint();
    }
  }

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the coordinate on a <code>JSVPanel</code>
   */
  public void toggleCoordinate() {
    if (selectedJSVPanel != null) {
      selectedJSVPanel.setCoordinatesOn(!selectedJSVPanel.isCoordinatesOn());
      repaint();
    }
  }

  /**
   * Method that can be called from another applet or from javascript that
   * toggles reversing the plot on a <code>JSVPanel</code>
   */
  public void reversePlot() {
    if (selectedJSVPanel != null) {
      selectedJSVPanel.setReversePlot(!selectedJSVPanel.isPlotReversed());
      repaint();
    }
  }

  /**
   * Gets a new set of parameters from a javascript call
   * 
   * @param newJSVparams
   *        String
   */
  public void script(String newJSVparams) {
    JSVparams = newJSVparams;
    newParams = true;
    init(null);
  }

  /**
   * Delivers spectrum coded as desired: XY, SQZ, PAC, DIF, AML, CML, SVG
   * 
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
   * 
   * @param block
   *        int
   */
  public void setSpectrumNumber(int block) {
    if (selectedJSVPanel != null) {
      if (theInterface.equals("single")) {
        showSpectrum(block - 1, false);
      } else {
        spectraPane.setSelectedIndex(block - 1);
      }
      repaint();
    }
  }

  public void setFilePath(String tmpFilePath) {
    if (isSignedApplet)
      scriptQueue.add(tmpFilePath);
    else
      setFilePathLocal(tmpFilePath);
  }

  /**
   * Loads a new file into the existing applet window
   * 
   * @param tmpFilePath
   *        String
   */
  void setFilePathLocal(String tmpFilePath) {
    getContentPane().removeAll();
    appletPanel.removeAll();
    newFilePath = tmpFilePath;
    newFile = true;
    init(null);
    getContentPane().validate();
    appletPanel.validate();
  }

  /**
   * Loads in-line JCAMP-DX data into the existing applet window
   * 
   * @param data
   *        String
   */
  public void loadInline(String data) {
    getContentPane().removeAll();
    appletPanel.removeAll();
    newFilePath = null;
    newFile = false;
    init(data);
    getContentPane().validate();
    appletPanel.validate();
  }

  /**
   * Method that can be called from another applet or from javascript that adds
   * a highlight to a portion of the plot area of a <code>JSVPanel</code>
   * 
   * @param x1
   *        the starting x value
   * @param x2
   *        the ending x value
   * @param r
   *        the red portion of the highlight color
   * @param g
   *        the green portion of the highlight color
   * @param b
   *        the blue portion of the highlight color
   * @param a
   *        the alpha portion of the highlight color
   */
  public void addHighlight(double x1, double x2, int r, int g, int b, int a) {
    if (selectedJSVPanel != null) {
      selectedJSVPanel.setHighlightOn(true);
      Color color = new Color(r, g, b, a);
      selectedJSVPanel.addHighlight(x1, x2, color);
      repaint();
    }
  }

  /**
   * Method that can be called from another applet or from javascript that
   * removes a highlight from the plot area of a <code>JSVPanel</code>
   * 
   * @param x1
   *        the starting x value
   * @param x2
   *        the ending x value
   */
  public void removeHighlight(double x1, double x2) {
    if (selectedJSVPanel != null) {
      selectedJSVPanel.removeHighlight(x1, x2);
      repaint();
    }
  }

  /**
   * Method that can be called from another applet or from javascript that
   * removes all highlights from the plot area of a <code>JSVPanel</code>
   */
  public void removeAllHighlights() {
    if (selectedJSVPanel != null) {
      selectedJSVPanel.removeAllHighlights();
      repaint();
    }
  }

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the integration graph of a <code>JSVPanel</code>.
   */
  public void toggleIntegration() {
    if (appletPopupMenu.integrateMenuItem.isSelected() == false)
      appletPopupMenu.integrateMenuItem.setSelected(true);
    else
      appletPopupMenu.integrateMenuItem.setSelected(false);
  }

  /**
   * Calls a javascript function given by the function name passing to it the
   * string parameters as arguments
   * 
   * @param function
   *        the javascript function name
   * @param parameters
   *        the function arguments as a string in the form "x, y, z..."
   */
  public void callToJavaScript(String function, Object[] params) {
    try {
      JSObject.getWindow(this).call(function, params);
    } catch (Exception npe) {
      System.out.println("EXCEPTION-> " + npe.getMessage());
    }
  }

  /**
   * Parses the javascript call parameters and executes them accordingly
   * 
   * @param params
   *        String
   */
  public void parseInitScript(String params) {
    if (params == null)
      params = "";
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
        case PARAM_SYNCID:
          syncID = value;
          fullName = appletID + "__" + syncID + "__";
          break;
        case PARAM_APPLETID:
          appletID = value;
          fullName = appletID + "__" + syncID + "__";
          break;
        case PARAM_SYNCCALLBACKFUNCTIONNAME:
          syncCallbackFunctionName = value;
          break;
        case PARAM_APPLETREADYCALLBACKFUNCTIONNAME:
          appletReadyCallbackFunctionName = value;
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
          backgroundColor = AppUtils.getColorFromString(value);
          break;
        case PARAM_COORDINATESCOLOR:
          coordinatesColor = AppUtils.getColorFromString(value);
          break;
        case PARAM_GRIDCOLOR:
          gridColor = AppUtils.getColorFromString(value);
          break;
        case PARAM_PLOTAREACOLOR:
          plotAreaColor = AppUtils.getColorFromString(value);
          break;
        case PARAM_PLOTCOLOR:
          plotColor = AppUtils.getColorFromString(value);
          break;
        case PARAM_SCALECOLOR:
          scaleColor = AppUtils.getColorFromString(value);
          break;
        case PARAM_TITLECOLOR:
          titleColor = AppUtils.getColorFromString(value);
          break;
        case PARAM_UNITSCOLOR:
          unitsColor = AppUtils.getColorFromString(value);
          break;
        case PARAM_PEAKCALLBACKFUNCTIONNAME:
          peakCallbackFunctionName = value;
          break;
        case PARAM_PLOTCOLORS:
          plotColorsStr = value;
          break;
        case PARAM_VERSION:
          break;
        case PARAM_IRMODE:
          irMode = value;
          break;
        case PARAM_OBSCURE:
          obscure = Boolean.parseBoolean(value);
          JSpecViewUtils.setObscure(obscure);
          break;
        case PARAM_XSCALEON:
          xScaleOn = Boolean.parseBoolean(value);
          break;
        case PARAM_YSCALEON:
          yScaleOn = Boolean.parseBoolean(value);
          break;
        case PARAM_XUNITSON:
          xUnitsOn = Boolean.parseBoolean(value);
          break;
        case PARAM_YUNITSON:
          yUnitsOn = Boolean.parseBoolean(value);
          break;
        case PARAM_INTEGRALPLOTCOLOR:
          integralPlotColor = AppUtils.getColorFromString(value);
          break;
        case PARAM_TITLEFONTNAME:
          GraphicsEnvironment g = GraphicsEnvironment
              .getLocalGraphicsEnvironment();
          List<String> fontList = Arrays
              .asList(g.getAvailableFontFamilyNames());
          for (String s : fontList)
            if (value.equalsIgnoreCase(s)) {
              titleFontName = value;
              break;
            }
          break;
        case PARAM_TITLEBOLDON:
          titleBoldOn = Boolean.parseBoolean(value);
          break;
        case PARAM_DISPLAYFONTNAME:
          GraphicsEnvironment g2 = GraphicsEnvironment
              .getLocalGraphicsEnvironment();
          List<String> fontList2 = Arrays.asList(g2
              .getAvailableFontFamilyNames());
          for (String s2 : fontList2)
            if (value.equalsIgnoreCase(s2)) {
              displayFontName = value;
              break;
            }
          break;
        case PARAM_INTEGRATIONRATIOS:
          // parse the string with a method in JSpecViewUtils
          System.out.println("Integration Ratio Parameter: " + value);
          integrationRatios = JSpecViewUtils
              .getIntegrationRatiosFromString(value);
          break;
        }
      } catch (Exception e) {
      }
    }
  }

  private void startCommandThread() {
    commandWatcherThread = new Thread(new CommandWatcher());
    commandWatcherThread.setName("CommmandWatcherThread");
    commandWatcherThread.start();
  }

  // for the signed applet to load a remote file, it must
  // be using a thread started by the initiating thread;
  Vector<String> scriptQueue = new Vector<String>();
  Thread commandWatcherThread;

  class CommandWatcher implements Runnable {
    public void run() {
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      int commandDelay = 200;
      while (commandWatcherThread != null) {
        try {
          Thread.sleep(commandDelay);
          if (commandWatcherThread != null) {
            if (scriptQueue.size() > 0) {
              String scriptItem = scriptQueue.remove(0);
              System.out.println("executing " + scriptItem);
              if (scriptItem != null) {
                setFilePathLocal(scriptItem);
              }
            }
          }
        } catch (InterruptedException ie) {
          Logger.info("CommandWatcher InterruptedException!");
          break;
        } catch (Exception ie) {
          String s = "script processing ERROR:\n\n" + ie.toString();
          for (int i = 0; i < ie.getStackTrace().length; i++) {
            s += "\n" + ie.getStackTrace()[i].toString();
          }
          Logger.info("CommandWatcher Exception! " + s);
          break;
        }
      }
      commandWatcherThread = null;
    }
  }

  void interruptQueueThreads() {
    if (commandWatcherThread != null)
      commandWatcherThread.interrupt();
  }

  private void openDataOrFile(String data) {
    String fileName = null;
    URL base = null;
    if (data != null) {
    } else if (filePath != null) {
      URL url;
      try {
        url = new URL(getCodeBase(), filePath);
        fileName = url.toString();
        recentFileName = url.getFile();
        recentURL = url.toString();
        base = getDocumentBase();
      } catch (MalformedURLException e) {
        System.out.println("problem: " + e.getMessage());
        fileName = filePath;
      }
    } else {
      writeStatus("Please set the 'filepath' or 'load file' parameter");
      return;
    }

    try {
      source = JDXFileReader.createJDXSource(data, fileName, base);
    } catch (Exception e) {
      writeStatus(e.getMessage());
      e.printStackTrace();
      return;
    }

    specs = source.getSpectra();
    boolean continuous = source.getJDXSpectrum(0).isContinuous();
    if (!compoundMenuOn2)
      compoundMenuOn = false;
    else {
      compoundMenuOn = source.isCompoundSource;
    }

    String Yunits = source.getJDXSpectrum(0).getYUnits();
    String Xunits = source.getJDXSpectrum(0).getXUnits();
    double firstX = source.getJDXSpectrum(0).getFirstX();
    double lastX = source.getJDXSpectrum(0).getLastX();

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
      appletPopupMenu.solColMenuItem.setEnabled(true);
    } else {
      appletPopupMenu.solColMenuItem.setEnabled(false);
    }
    //  Can only integrate a continuous H NMR spectrum
    if (continuous
        && ((JDXSpectrum) selectedJSVPanel
            .getSpectrumAt(0)).isHNMR())
      appletPopupMenu.integrateMenuItem.setEnabled(true);
    //Can only convert from T <-> A  if Absorbance or Transmittance and continuous
    if ((continuous) && (Yunits.toLowerCase().contains("abs"))
        || (Yunits.toLowerCase().contains("trans")))
      appletPopupMenu.transAbsMenuItem.setEnabled(true);
    else
      appletPopupMenu.transAbsMenuItem.setEnabled(false);
    setStringParameter("irmode", irMode);
    appletPopupMenu.exportAsMenu.setEnabled(true);
    appletPopupMenu.saveAsJDXMenu.setEnabled(continuous);
    newFile = false;
  }

  /////////// simple sync functionality //////////

  /**
   * preceed <Peaks here with full name of Jmol applet (including syncID)
   * 
   */
  public void syncScript(String script) {
    System.out.println("JSpecView applet syncScript: " + script);
    String file = Parser.getQuotedAttribute(script, "file");
    String index = Parser.getQuotedAttribute(script, "index");
    if (file == null || index == null)
      return;
    URL url = null;
    try {
      url = new URL(getCodeBase(), file);
    } catch (MalformedURLException e) {
      System.out.println("Trouble with URL for " + file);
      return;
    }
    String f = url.toString();
    System.out.println(f);
    System.out.println(recentURL);
    if (!f.equals(recentURL))
      setFilePathLocal(file);
    if (!selectPanel(index))
      script = null;
    selectedJSVPanel.processPeakSelect(script);
    sendFrameChange(selectedJSVPanel);
  }

  private boolean selectPanel(String index) {
    // what if tabbed? 
    if (jsvPanels == null)
      return false;
    for (int i = 0; i < jsvPanels.length; i++) {
      if (((JDXSpectrum) jsvPanels[i].getSpectrumAt(0)).hasPeakIndex(index)) {
        setSpectrumNumber(i + 1);
        return true;
      }
    }
    return false;
  }

  /**
   * This is the method Debbie needs to call from within JSpecView when a peak
   * is clicked.
   * 
   * @param peak
   */
  public void sendScript(String peak) {
    selectedJSVPanel.processPeakSelect(peak);
    if (syncCallbackFunctionName == null)
      return;
    callToJavaScript(syncCallbackFunctionName, new Object[] { fullName,
        Escape.jmolSelect(peak, recentURL) });
  }

  public void checkCallbacks() {
    if (coordCallbackFunctionName == null && peakCallbackFunctionName == null)
      return;
    Coordinate coord = new Coordinate();
    Coordinate actualCoord = (peakCallbackFunctionName == null ? null
        : new Coordinate());
    if (!selectedJSVPanel.getPickedCoordinates(coord, actualCoord))
      return;
    if (actualCoord == null)
      callToJavaScript(coordCallbackFunctionName, new Object[] {
          Double.valueOf(coord.getXVal()), Double.valueOf(coord.getYVal()),
          Integer.valueOf(currentSpectrumIndex + 1) });
    else
      callToJavaScript(peakCallbackFunctionName, new Object[] {
          Double.valueOf(coord.getXVal()), Double.valueOf(coord.getYVal()),
          Double.valueOf(actualCoord.getXVal()),
          Double.valueOf(actualCoord.getYVal()),
          Integer.valueOf(currentSpectrumIndex + 1) });
  }

  public void peakPicked(PeakPickedEvent eventObj) {
    setSelectedPanel((JSVPanel) eventObj.getSource());
    currentSpectrumIndex = selectedJSVPanel.getIndex();
    checkCallbacks();
    sendScript(eventObj.getPeakInfo());
  }

  private void setSelectedPanel(JSVPanel source) {
    removeKeyListener(selectedJSVPanel);
    selectedJSVPanel = source;
    addKeyListener(selectedJSVPanel);
  }

  private void sendFrameChange(JSVPanel jsvp) {
    PeakInfo pi = ((JDXSpectrum)jsvp.getSpectrumAt(0)).getSelectedPeak();
    sendScript(pi == null ? null : pi.getStringInfo());
  }

}
