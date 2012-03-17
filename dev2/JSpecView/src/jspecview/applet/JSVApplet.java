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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.ArrayList;

import javax.swing.JApplet;
import javax.swing.JCheckBoxMenuItem;
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

import jspecview.common.AppUtils;
import jspecview.common.IntegralGraph;
import jspecview.common.JSV1DOverlayPanel;
import jspecview.common.JSVPanel;
import jspecview.common.OverlayLegendDialog;
import jspecview.common.Parameters;
import jspecview.common.PanelListener;
import jspecview.common.PeakPickedEvent;
import jspecview.common.PrintLayoutDialog;
import jspecview.common.ScriptInterface;
import jspecview.common.ScriptToken;
import jspecview.common.Coordinate;
import jspecview.common.Annotation;
import jspecview.common.JDXSpectrum;
import jspecview.common.PeakInfo;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.export.Exporter;
import jspecview.source.FileReader;
import jspecview.source.JDXSource;
import jspecview.util.Escape;
import jspecview.util.FileManager;
import jspecview.util.Logger;
import jspecview.util.Parser;
import jspecview.util.TextFormat;
import netscape.javascript.JSObject;

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

public class JSVApplet extends JApplet implements PanelListener, ScriptInterface {

  public static final String APPLET_VERSION = "2.0.20120316-0450"; //
//  2.0.yyyymmdd-hhmm format - should be updated to keep track of the latest version (based on Jamaica time)
  
  private static final long serialVersionUID = 1L;

  public boolean isPro() {
    return false;
  }
  
  /* -------------------- default PARAMETERS -------------------------*/

  private ArrayList<Annotation> integrationRatios = null; // Integration Ratio Annotations
  private int startIndex = -1;
  private int endIndex = -1;
  private int spectrumNumber = -1; // blockNumber or nTupleNumber
  private int irMode = JDXSpectrum.TA_NO_CONVERT;
  private boolean autoIntegrate;
  private int numberOfSpectra;

  private String theInterface = "single"; // either tab, tile, single, overlay
  private String coordCallbackFunctionName;
  private String peakCallbackFunctionName;
  private String syncCallbackFunctionName;
  private String appletReadyCallbackFunctionName;

  private String sltnclr = "255,255,255"; //Colour of Solution

  private Parameters parameters = new Parameters("applet");
  private Parameters tempParams = new Parameters("temp");
   

  /*---------------------------------END PARAMETERS------------------------*/

  private String appletID;
  private String syncID;
  private Thread commandWatcherThread;
  private boolean isSignedApplet;
  private boolean isStandalone;
  private Boolean obscureTitleFromUser;
  private JFileChooser jFileChooser;
  private JTabbedPane tabbedDisplayPane = new JTabbedPane();
  private JFrame offWindowFrame;
  private JPanel appletPanel;
  private JSVAppletPopupMenu appletPopupMenu;  
  private List<JSVPanel> jsvPanels;
  private List<JDXSpectrum> specs;
  private int currentSpectrumIndex;
  private String recentFileName = "";
  private String recentURL = "";
  private JSVPanel selectedJSVPanel;
  private JDXSource currentSource;
  private boolean isOverlaid;
   
  public boolean isSigned() {
    return isSignedApplet;
  }
  
  public JSVPanel getSelectedPanel() {
    return selectedJSVPanel;
  }
  
  JDXSource getSource() {
    return currentSource;
  }

  /////////////// public methods called from page or browser ////////////////
  //
  //
  // Notice that in all of these we use getSelectedPanel(), not selectedJSVPanel
  // That's because the methods aren't overridden in JSVAppletPro, and in that case
  // we want to select the panel from MainFrame, not here. Thus, when the Advanced...
  // tab is open, actions from outside of Jmol act on the MainFrame, not here. 
  //
  // BH - 8.3.2012
  
  @Override
  public void finalize() {
    System.out.println("JSpecView " + this + " finalized");
  }

  @Override
  public void destroy() {
    System.out.println("JSVApplet " + this + " destroy 1");
    if (commandWatcherThread != null) {
      commandWatcherThread.interrupt();
      commandWatcherThread = null;
    }
    if (jsvPanels != null) {
      for (int i = jsvPanels.size(); --i >= 0;) {
        jsvPanels.get(i).dispose();
        jsvPanels.remove(i);
      }
    }
    System.out.println("JSVApplet " + this + " destroy 2");
  }

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

  /**
   * Returns the current internal version of the Applet
   * 
   * @return String
   */
  public String getAppletVersion() {
    return JSVApplet.APPLET_VERSION;
  }

  /**
   * Get Applet information
   * 
   * @return the String "JSpecView Applet"
   */
  @Override
  public String getAppletInfo() {
    return "JSpecView Applet " + getAppletVersion();
  }
  
  /**
   * Calculates the predicted colour of the Spectrum
   */
  public void setSolutionColor(boolean showMessage) {
    sltnclr = getSelectedPanel().getSolutionColor();
    if (showMessage)
      JSVPanel.showSolutionColor((Component)this, sltnclr);
  }

  /**
   * Returns the calculated colour of a visible spectrum (Transmittance)
   * 
   * @return Color
   */

  public String getSolnColour() {
    return sltnclr;
  }

  /**
   * Method that can be called from another applet or from javascript to return
   * the coordinate of clicked point in the plot area of the <code>
   * JSVPanel</code>
   * 
   * @return A String representation of the coordinate
   */
  public String getCoordinate() {
    // important to use getSelectedPanel() here because it may be from MainFrame in PRO
    if (getSelectedPanel() != null) {
      Coordinate coord = getSelectedPanel().getClickedCoordinate();
      if (coord != null)
        return coord.getXVal() + " " + coord.getYVal();
    }
    return "";
  }

  /**
   * Loads in-line JCAMP-DX data into the existing applet window
   * 
   * @param data
   *        String
   */
  public void loadInline(String data) {
    newAppletPanel();
    openDataOrFile(data, null, null, null);
    getContentPane().validate();
    appletPanel.validate();
  }

  /**
   * Delivers spectrum coded as desired: XY, SQZ, PAC, DIF, DIFDUP, FIX, AML, CML
   * 
   * @param type
   * @param n
   * @return data
   * 
   */
  public String export(String type, int n) {
    if (type == null)
      type = "XY";
    JSVPanel jsvp = getSelectedPanel();
    if (n < -1 || jsvp.getNumberOfSpectra() <= n)
      return "only " + jsvp.getNumberOfSpectra()
          + " spectra available.";
    try {
      JDXSpectrum spec = (n < 0 ? jsvp.getSpectrum() : jsvp.getSpectrumAt(n));
      return Exporter.exportTheSpectrum(Exporter.Type.getType(type), null, spec, 0, spec.getXYCoords().length - 1);
    } catch (IOException ioe) {
      // not possible
    }
    return null;
  }

  public void setFilePath(String tmpFilePath) {
    if (isSignedApplet)
      processCommand("load " + tmpFilePath);
    else
      setFilePathLocal(tmpFilePath);
  }

  /**
   * Sets the spectrum to the specified block number
   * 
   * @param i
   */
  public void setSpectrumNumber(int i) {
    setSpectrum(i);
  }
  
  /**
   * Method that can be called from another applet or from javascript that
   * toggles the grid on a <code>JSVPanel</code>
   */
  public void toggleGrid() {
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp == null)
      return;
    AppUtils.setBoolean(jsvp, tempParams, ScriptToken.GRIDON, !jsvp.isGridOn());
    repaint();
  }

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the coordinate on a <code>JSVPanel</code>
   */
  public void toggleCoordinate() {
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp == null)
      return;
    AppUtils.setBoolean(jsvp, tempParams, ScriptToken.COORDINATESON, 
        !jsvp.isCoordinatesOn());
    repaint();
  }

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the integration graph of a <code>JSVPanel</code>.
   */
  public void toggleIntegration() {
    runScript("integrate ?");
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
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp != null) {
      Color color = new Color(r, g, b, a);
      jsvp.addHighlight(x1, x2, color);
      repaint();
    }
  }

  /**
   * Method that can be called from another applet or from javascript that
   * removes all highlights from the plot area of a <code>JSVPanel</code>
   */
  public void removeAllHighlights() {
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp != null) {
      jsvp.removeAllHighlights();
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
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp != null) {
      jsvp.removeHighlight(x1, x2);
      repaint();
    }
  }

  /**
   * Method that can be called from another applet or from javascript that
   * toggles reversing the plot on a <code>JSVPanel</code>
   */
  public void reversePlot() {
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp != null) {
      jsvp.setReversePlot(!jsvp.isPlotReversed());
      repaint();
    }
  }

  public void runScript(String script) {
    if (scriptQueue == null)
      processCommand(script);
    else
      scriptQueue.add(script);
  }

  /**
   * precede <Peaks here with full name of Jmol applet (including syncID)
   * 
   */
  public void syncScript(String script) {
    System.out.println("jsvapplet syncScript " + script);
    if (script.indexOf("<PeakData") < 0) {
      runScript(script);
      return;
    }
    String fileName = Parser.getQuotedAttribute(script, "file");
    String index = Parser.getQuotedAttribute(script, "index");
    System.out.println("jsvapplet syncScript file/index " + fileName + " " + index);
    if (fileName == null || index == null)
      return;
    URL url = null;
    try {
      url = new URL(getCodeBase(), fileName);
    } catch (MalformedURLException e) {
      System.out.println("Trouble with URL for " + fileName);
      return;
    }
    String f = url.toString();
    if (!f.equals(recentURL))
      setFilePathLocal(fileName);
    if (!selectPanel(index))
      script = null;
    selectedJSVPanel.processPeakSelect(script);
    sendFrameChange(selectedJSVPanel);
  }

  /**
   * Writes a message to the status label
   * 
   * @param msg
   *        the message
   */
  protected void writeStatus(String msg) {
    Logger.info(msg);
    //statusTextLabel.setText(msg);
  }

  //////////////////////////  PRIVATE or SEMIPRIVATE METHODS ////////////////////
  /////////////
  /////////////
  /////////////
  /////////////
  /////////////
  
  /**
   * Initializes applet with parameters and load the <code>JDXSource</code> 
   * called by the browser
   * 
   */
  @Override
  public void init() {
    try {
      jFileChooser = new JFileChooser();
      isSignedApplet = true;
    } catch (Exception e) {
      // just not a signed applet
      //e.printStackTrace();
    }
    scriptQueue = new ArrayList<String>();
    commandWatcherThread = new Thread(new CommandWatcher());
    commandWatcherThread.setName("CommmandWatcherThread");
    commandWatcherThread.start();
    
    initParams(getParameter("script"));
    if (appletReadyCallbackFunctionName != null && fullName != null)
      callToJavaScript(appletReadyCallbackFunctionName, new Object[] {
          appletID, fullName, Boolean.TRUE });
  }

  /**
   * starts or restarts applet display from scratch
   * or from a JSVApplet.script() JavaScript command
   * 
   * Involves a two-pass sequence through parsing the 
   * parameters, because order is not important in this
   * sort of call. 
   * 
   * To call a script and have commands execute in order, use 
   * 
   *   JSVApplet.runScript(script)
   * 
   * instead
   * 
   * @param params
   */
  private void initParams(String params) {
    parseInitScript(params);
    newAppletPanel();
    appletPopupMenu = new JSVAppletPopupMenu(this, allowMenu, parameters.getBoolean(ScriptToken.ENABLEZOOM));
    runScriptNow(params);
  }
  
  private void newAppletPanel() {
    getContentPane().removeAll();
    appletPanel = new JPanel(new BorderLayout());
    getContentPane().add(appletPanel);
  }

  /**
   * Initalizes the <code>JSVPanels</code> and adds them to the jsvPanels array
   * 
   * @throws JSpecViewException
   */
  private void initPanels() throws JSpecViewException {
    boolean showRange = false;
    JSVPanel jsvp;

    if (startIndex >= 0 && endIndex > startIndex)
      showRange = true;

    // Initialise JSVpanels

    if (isOverlaid) {
      // overlay all spectra on a panel
      jsvPanels = new ArrayList<JSVPanel>();

      try {
        int[] startIndices = null;
        int[] endIndices = null;
        if (showRange) {
          startIndices = new int[numberOfSpectra];
          endIndices = new int[numberOfSpectra];
          Arrays.fill(startIndices, startIndex);
          Arrays.fill(endIndices, endIndex);
        }
        jsvp = new JSV1DOverlayPanel(specs, startIndices, endIndices);
      } catch (ScalesIncompatibleException sie) {
        theInterface = "single";
        isOverlaid = false;
        initPanels();
        return;
      }
      endIndex = startIndex = -1;
      jsvPanels.add(jsvp);
      initProperties(jsvp, 0);
      selectedJSVPanel = jsvp;
      jsvp.setIndex(currentSpectrumIndex = 0);
    } else {
      // not overlaid
      // initialise JSVPanels and add them to the array
      jsvPanels = new ArrayList<JSVPanel>();
      try {
        for (int i = 0; i < numberOfSpectra; i++) {
          JDXSpectrum spec = specs.get(i);
          if (spec.getIntegrationGraph() != null) {
            jsvp = JSV1DOverlayPanel.getIntegralPanel(spec, null);
          } else if (showRange) {
            jsvp = new JSVPanel(spec, startIndex,
                endIndex, currentSource, appletPopupMenu);
          } else {
            jsvp = new JSVPanel(spec, currentSource, appletPopupMenu);
          }
          jsvPanels.add(jsvp);
          initProperties(jsvp, i);
        }
      } catch (Exception e) {
        // TODO
      }
    }
  }

  private void initProperties(JSVPanel jsvp, int index) {
    // set JSVPanel properties from applet parameters
    jsvp.addListener(this);
    jsvp.setIndex(index);
    parameters.setFor(jsvp, null, true);
  }

  /**
   * Initializes the interface of the applet depending on the value of the
   * <i>interface</i> parameter
   */
  private void initInterface() {
    final int numberOfPanels = jsvPanels.size();
    boolean canDoTile = (numberOfPanels >= 2 && numberOfPanels <= 10);
    boolean moreThanOnePanel = numberOfPanels > 1;
    boolean showSpectrumNumber = spectrumNumber != -1
        && spectrumNumber <= numberOfPanels;
    //appletPanel.setBackground(backgroundColor);
    if (theInterface.equals("tab") && moreThanOnePanel) {
      tabbedDisplayPane = new JTabbedPane(SwingConstants.TOP,
          JTabbedPane.SCROLL_TAB_LAYOUT);
      appletPanel.add(new JLabel(currentSource.getTitle(),
          SwingConstants.CENTER), BorderLayout.NORTH);
      appletPanel.add(tabbedDisplayPane, BorderLayout.CENTER);

      for (int i = 0; i < numberOfPanels; i++) {
        String title = specs.get(i).getTitleLabel();
        if (currentSource.type == JDXSource.TYPE_NTUPLE)
          title = title.substring(title.indexOf(':') + 1);
        else if (currentSource.type == JDXSource.TYPE_BLOCK)
          //title = "block " + (i + 1);
          title = title.substring(0, (title.length() >= 10 ? 10 : title
              .length()))
              + "... : ";
        tabbedDisplayPane.addTab(title, jsvPanels.get(i));
      }
      // Show the spectrum specified by the spectrumnumber parameter
      if (showSpectrumNumber) {
        tabbedDisplayPane.setSelectedIndex(spectrumNumber - 1);
      }
      setSelectedPanel((JSVPanel) tabbedDisplayPane.getSelectedComponent());
      tabbedDisplayPane.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          setSelectedPanel((JSVPanel) tabbedDisplayPane.getSelectedComponent());
        }
      });
    } else if (theInterface.equals("tile") && canDoTile) {
      appletPanel.add(new JLabel(currentSource.getTitle(),
          SwingConstants.CENTER), BorderLayout.NORTH);

      for (int i = 0; i < numberOfPanels; i++) {
        jsvPanels.get(i).setMinimumSize(new Dimension(250, 150));
        jsvPanels.get(i).setPreferredSize(new Dimension(300, 200));
      }
      JSplitPane splitPane = createSplitPane(jsvPanels);
      appletPanel.add(splitPane, BorderLayout.CENTER);
      //splitPane.setBackground(backgroundColor);
    } else { // Single or overlay
      //      compoundMenuOn = true;
      int spectrumIndex = (showSpectrumNumber ? spectrumNumber - 1 : 0);
            
      setSelectedPanel(spectrumIndex >= jsvPanels.size() ? null : jsvPanels.get(spectrumIndex));

      // Global variable for single interface
      currentSpectrumIndex = spectrumIndex;
      if (isOverlaid && currentSource.isCompoundSource) {
        jsvPanels.get(spectrumIndex).setTitle(currentSource.getTitle());
        appletPopupMenu.overlayKeyMenuItem.setEnabled(true);
      }
      appletPanel.add(jsvPanels.get(spectrumIndex), BorderLayout.CENTER);
      // else interface = single
      if (moreThanOnePanel && compoundMenuOn) {
        appletPopupMenu.compoundMenu.removeAll();
        appletPopupMenu.compoundMenu.add(appletPopupMenu.overlayMenuItem);
        if (jsvPanels.size() <= 20) {
          // add Menus to navigate
          JCheckBoxMenuItem mi;
          if (currentSource.isCompoundSource) {
            for (int i = 0; i < numberOfPanels; i++) {
              mi = new JCheckBoxMenuItem((i + 1) + "- " + specs.get(i).getTitleLabel());
              mi.setSelected(i == currentSpectrumIndex);
              mi.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                  if (e.getStateChange() == ItemEvent.SELECTED) {
                    // deselects the previously selected menu item
                    JCheckBoxMenuItem deselectedMenu = (JCheckBoxMenuItem) ((JCheckBoxMenuItem) e
                        .getSource()).getParent().getComponent(
                        currentSpectrumIndex + 1);
                    deselectedMenu.setSelected(false);
                    compoundMenu_itemStateChanged(e);
                  }
                }
              });
              mi.setActionCommand("" + i);
              appletPopupMenu.compoundMenu.add(mi);
            }
            appletPopupMenu.compoundMenu
                .setText(currentSource.type == JDXSource.TYPE_OVERLAY ? "Spectra" 
                    : currentSource.type == JDXSource.TYPE_BLOCK ? "Blocks" : "NTuples");
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
  private void compoundMenu_itemStateChanged(ItemEvent e) {

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
  private void showSpectrum(int index, boolean fromMenu) {
    System.out.println("showSpectrum " + index);
    JSVPanel jsvp = jsvPanels.get(index);
    appletPanel.remove(jsvPanels.get(currentSpectrumIndex));
    appletPanel.add(jsvp, BorderLayout.CENTER);
    currentSpectrumIndex = index;
    setSelectedPanel(jsvp);

    appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(selectedJSVPanel.getSpectrum().canIntegrate());

    chooseContainer();
    this.validate();
    repaint();

    if (fromMenu)
      sendFrameChange(jsvp);

  }

  /**
   * Shows the header information for the Spectrum
   * 
   * @param e
   *        the ActionEvent
   */
  protected void headerMenuItem_actionPerformed(ActionEvent e) {
    
    JDXSpectrum spectrum = selectedJSVPanel.getSpectrum();
    Object[][] rowData = (isOverlaid ? currentSource
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
   * Opens the print dialog to enable printing
   * 
   * @param e
   *        ActionEvent
   */
  protected void printMenuItem_actionPerformed(ActionEvent e) {
    if (offWindowFrame == null) {
      System.err
          .println("Use the View/Window menu to lift the spectrum off the page first.");
      return;
    }

    JSVPanel jsvp = selectedJSVPanel;

    PrintLayoutDialog ppd = new PrintLayoutDialog(offWindowFrame);
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
  protected void windowMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      // disable some features when in Window mode
      appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(false);
      appletPopupMenu.compoundMenu.setEnabled(false);
      appletPopupMenu.transAbsMenuItem.setEnabled(false);
      offWindowFrame = new JFrame("JSpecView");
      offWindowFrame.setSize(getSize());
      final Dimension d;
      d = appletPanel.getSize();
      offWindowFrame.add(appletPanel);
      offWindowFrame.validate();
      offWindowFrame.setVisible(true);
      remove(appletPanel);
      validate();
      repaint();
      offWindowFrame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          appletPanel.setSize(d);
          getContentPane().add(appletPanel);
          setVisible(true);
          validate();
          repaint();
          appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(true);
          //       if (compoundMenuOn)
          //             compoundMenu.setEnabled(true);
          offWindowFrame.removeAll();
          offWindowFrame.dispose();
          appletPopupMenu.windowMenuItem.setSelected(false);
        }
      });
    } else {
      // re-enable features that were disabled in Window mode
      appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(true);
      appletPopupMenu.transAbsMenuItem.setEnabled(true);
      if (compoundMenuOn)
        appletPopupMenu.compoundMenu.setEnabled(true);

      getContentPane().add(appletPanel);
      validate();
      repaint();
      offWindowFrame.removeAll();
      offWindowFrame.dispose();
    }
  }

  /**
   * Allows conversion between TRANSMITTANCE and ABSORBANCE
   * 
   * @param e
   *        ItemEvent
   */

  protected void transAbsMenuItem_itemStateChanged(ItemEvent e) {
    // for some reason, at the the St. Olaf site, this is triggering twice
    // when the user clicks the menu item. Why?
    try {
      System.out.println("ta event " + e.getID() + " " + e.getStateChange()
          + " " + ItemEvent.SELECTED + " " + ItemEvent.DESELECTED + " "
          + e.toString());
      if (e.getStateChange() == ItemEvent.SELECTED)
        taConvert(JDXSpectrum.IMPLIED);
      else
        taConvert(JDXSpectrum.IMPLIED);
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

  private void taConvert(int comm) throws Exception {
    long time = System.currentTimeMillis();
    if (msTrigger > 0 && time - msTrigger < 100)
      return;
    msTrigger = time;
    JSVPanel jsvp = JSVPanel.taConvert(getCurrentPanel(), comm);
    if (jsvp == null)
      return;

    appletPanel.remove(0);
    appletPanel.add(jsvp);

    initProperties(jsvp, 0);
    appletPopupMenu.solColMenuItem.setEnabled(true);
    jsvp.repaint();

    //  now need to validate and repaint
    validate();
    repaint();

  }

  private JSVPanel getCurrentPanel() {
    return (JSVPanel) appletPanel.getComponent(0);
  }

  /**
   * Allows Integration of an HNMR spectrum
   * 
   */
  private void integrate(String value) {
    JSVPanel jsvp = getCurrentPanel();
    JSVPanel jsvpNew = AppUtils.checkIntegral(jsvp, appletPanel,
        parameters, value);
    if (jsvp == jsvpNew) {
      integrationRatios = null;
      return;
    }
    initProperties(jsvpNew, currentSpectrumIndex);
    if (integrationRatios != null)
      jsvpNew.setIntegrationRatios(integrationRatios);
    integrationRatios = null; // first time only
    jsvpNew.repaint();
    chooseContainer();
  }

  // check which mode the spectrum is in (windowed or applet)
  private void chooseContainer() {
    // check first if we have ever had a frame
    if (offWindowFrame == null) {
      appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(true);
      if (compoundMenuOn)
        appletPopupMenu.compoundMenu.setEnabled(true);
      getContentPane().add(appletPanel);
      validate();
      repaint();
    } else {
      if (offWindowFrame.getComponentCount() != 0) {
        appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(false);
        appletPopupMenu.compoundMenu.setEnabled(false);
        offWindowFrame.add(appletPanel);
        offWindowFrame.validate();
        offWindowFrame.setVisible(true);
      } else {
        appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(true);
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
  protected void overlayKeyMenuItem_actionPerformed(ActionEvent e) {
    new OverlayLegendDialog(selectedJSVPanel);
  }

  private String fullName;
  private boolean allowMenu = true;
  private boolean compoundMenuOn;
  private boolean allowCompoundMenu = true;
  private String dirLastExported;

  /**
   * Export spectrum in a given format
   * 
   * @param command
   *        the name of the format to export in
   */
  void exportSpectrum(String type) {
    if (isSignedApplet)
      dirLastExported = Exporter.exportSpectra(selectedJSVPanel,
          offWindowFrame, jFileChooser, type, recentFileName, dirLastExported);
    else
      Logger.info(export(type, -1));
  }

  /**
   * Used to tile JSVPanel when the <i>interface</i> parameters is equal to
   * "tile"
   * 
   * @param comps
   *        An array of components to tile
   * @return a <code>JSplitPane</code> with components tiled
   */
  private JSplitPane createSplitPane(List<JSVPanel> comps) {
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
  private JSplitPane createSplitPaneAux(ComponentListPair pair) {
    int numTop = pair.top.size();
    int numBottom = pair.bottom.size();
    JSplitPane splitPane;

    if (numBottom == 1 && numTop == 1) {
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      splitPane.setLeftComponent(pair.top.get(0));
      splitPane.setRightComponent(pair.bottom.get(0));

    }

    else if (numBottom == 1 && numTop == 2) {
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      JSplitPane newSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      newSplitPane.setLeftComponent(pair.top.get(0));
      newSplitPane.setRightComponent(pair.top.get(1));
      splitPane.setLeftComponent(newSplitPane);
      splitPane.setRightComponent(pair.bottom.get(0));
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
  private ComponentListPair createPair(List<JSVPanel> comps) {
    int numBottom = (int) (comps.size() / 2);
    int numTop = numBottom + (comps.size() % 2);
    List<JSVPanel> top = new ArrayList<JSVPanel>();
    List<JSVPanel> bottom = new ArrayList<JSVPanel>();
    int i;
    for (i = 0; i < numTop; i++)
      top.add(comps.get(i));
    for (; i < comps.size(); i++)
      bottom.add(comps.get(i));
    return new ComponentListPair(top, bottom);
  }

  /**
   * Representation of array[2] of components of equal or nearly equal size
   * 
   */
  private class ComponentListPair {
    
    List<JSVPanel> top; 
    List<JSVPanel> bottom;

    public ComponentListPair(List<JSVPanel> top, List<JSVPanel> bottom) {
      this.top = top;
      this.bottom = bottom;
    }
  }

  /**
   * Loads a new file into the existing applet window
   * 
   * @param tmpFilePath
   *        String
   */
  private void setFilePathLocal(String filePath) {
    newAppletPanel();
    openDataOrFile(null, null, null, filePath);
    getContentPane().validate();
    appletPanel.validate();
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
  private void callToJavaScript(String function, Object[] params) {
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
  private void parseInitScript(String params) {
    if (params == null)
      params = "";
    StringTokenizer allParamTokens = new StringTokenizer(params, ";");
    if (Logger.debugging) {
      System.out.println("Running in DEBUG mode");
    }
    while (allParamTokens.hasMoreTokens()) {
      String token = allParamTokens.nextToken().trim();
      // now split the key/value pair
      StringTokenizer eachParam = new StringTokenizer(token);
      String key = eachParam.nextToken();
      if (key.equalsIgnoreCase("SET"))
        key = eachParam.nextToken();
      key = key.toUpperCase();
      ScriptToken st = ScriptToken.getScriptToken(key);
      String value = ScriptToken.getValue(st, eachParam, token);
      System.out.println("KEY-> " + key + " VALUE-> " + value + " : " + st);
      try {
        switch (st) {
        default:
          parameters.set(null, st, value);
          break;
        case UNKNOWN:
          break;
        case VERSION:
          break;
        case OBSCURE:
          if (obscureTitleFromUser == null) // once only 
            obscureTitleFromUser = Boolean.valueOf(value);
          break;
        case SYNCID:
          syncID = value;
          fullName = appletID + "__" + syncID + "__";
          break;
        case APPLETID:
          appletID = value;
          fullName = appletID + "__" + syncID + "__";
          break;
        case APPLETREADYCALLBACKFUNCTIONNAME:
          appletReadyCallbackFunctionName = value;
          break;
        case MENUON:
          allowMenu = Boolean.parseBoolean(value);
          break;
        case COMPOUNDMENUON:
          allowCompoundMenu = Boolean.parseBoolean(value);
          break;
        case INTERFACE:
          theInterface = value;
          if (!theInterface.equals("tab") && !theInterface.equals("tile")
              && !theInterface.equals("single")
              && !theInterface.equals("overlay"))
            theInterface = "single";
          break;
        case ENDINDEX:
          endIndex = Integer.parseInt(value);
          break;
        case STARTINDEX:
          startIndex = Integer.parseInt(value);
          break;
        case SPECTRUMNUMBER:
          spectrumNumber = Integer.parseInt(value);
          break;
        case AUTOINTEGRATE:
          autoIntegrate = Parameters.isTrue(value);
          break;
        case IRMODE:
          irMode = (value.toUpperCase().startsWith("T") ? JDXSpectrum.TO_TRANS
              : JDXSpectrum.TO_ABS);
       
        }
      } catch (Exception e) {
      }
    }
  }

  // for the signed applet to load a remote file, it must
  // be using a thread started by the initiating thread;
  private List<String> scriptQueue;

  private class CommandWatcher implements Runnable {
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
              if (scriptItem != null)
                processCommand(scriptItem);
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

  int nOverlays;
  private List<JDXSpectrum> specsSaved;

  /*
    private void interruptQueueThreads() {
      if (commandWatcherThread != null)
        commandWatcherThread.interrupt();
    }
  */
  private void openDataOrFile(String data, String name,
                              List<JDXSpectrum> specs1, String filePath) {
    appletPanel.removeAll();
    String fileName = null;
    URL base = null;
    boolean isOverlay = false;
    if (specs1 != null) {
      fileName = "Overlay" + (++nOverlays);
      isOverlay = true;
      specs = specs1;
    } else if (data != null) {
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
      currentSource = (isOverlay ? JDXSource.createOverlay(fileName, specs)
          : FileReader.createJDXSource(FileManager
              .getBufferedReaderForString(data), fileName, base,
              obscureTitleFromUser == Boolean.TRUE, -1, -1));
      currentSource.setFilePath(fileName);
    } catch (Exception e) {
      writeStatus(e.getMessage());
      e.printStackTrace();
      return;
    }

    specs = currentSource.getSpectra();
    numberOfSpectra = specs.size();
    isOverlaid = isOverlay && !name.equals("NONE")
        || (theInterface.equals("overlay") && numberOfSpectra > 1);
    isOverlaid &= !JDXSpectrum.process(specs, irMode, !isOverlay && autoIntegrate,
        parameters.integralMinY, parameters.integralOffset,
        parameters.integralFactor);

    boolean continuous = currentSource.getJDXSpectrum(0).isContinuous();
    String Yunits = currentSource.getJDXSpectrum(0).getYUnits();
    String Xunits = currentSource.getJDXSpectrum(0).getXUnits();
    double firstX = currentSource.getJDXSpectrum(0).getFirstX();
    double lastX = currentSource.getJDXSpectrum(0).getLastX();

    compoundMenuOn = allowCompoundMenu && currentSource.isCompoundSource;

    try {
      initPanels();
    } catch (JSpecViewException e1) {
      writeStatus(e1.getMessage());
      return;
    }

    initInterface();

    System.out.println(getAppletInfo() + " File " + fileName
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
    if (continuous && selectedJSVPanel.getSpectrum().canIntegrate())
      appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(true);
    //Can only convert from T <-> A  if Absorbance or Transmittance and continuous
    if ((continuous) && (Yunits.toLowerCase().contains("abs"))
        || (Yunits.toLowerCase().contains("trans")))
      appletPopupMenu.transAbsMenuItem.setEnabled(true);
    else
      appletPopupMenu.transAbsMenuItem.setEnabled(false);
    if (appletPopupMenu.exportAsMenu != null)
      appletPopupMenu.exportAsMenu.setEnabled(true);
    appletPopupMenu.saveAsJDXMenu.setEnabled(continuous);
  }

  protected void processCommand(String script) {
    runScriptNow(script);
  }

  /////////// simple sync functionality //////////

  private void runScriptNow(String params) {
    if (params == null)
      params = "";
    params = params.trim();
    System.out.println("RUNSCRIPT " + params);
    StringTokenizer allParamTokens = new StringTokenizer(params, ";");
    if (Logger.debugging) {
      System.out.println("Running in DEBUG mode");
    }
    JSVPanel jsvp = selectedJSVPanel;
    while (allParamTokens.hasMoreTokens()) {
      String token = allParamTokens.nextToken().trim();
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
        case LOAD:
          // no APPEND here
          specsSaved = null;
          openDataOrFile(null, null, null, TextFormat.trimQuotes(value));
          setSpectrum(1);
          break;
        default:
          parameters.set(jsvp, st, value);
          break;
        case PEAKCALLBACKFUNCTIONNAME:
          peakCallbackFunctionName = value;
          break;
        case SYNCCALLBACKFUNCTIONNAME:
          syncCallbackFunctionName = value;
          break;
        case COORDCALLBACKFUNCTIONNAME:
          coordCallbackFunctionName = value;
          break;
        case SPECTRUM:
        case SPECTRUMNUMBER:
          setSpectrum(Integer.parseInt(value));
          jsvp = selectedJSVPanel;
          break;
        //          case INTERFACE:
        //            if (value.equalsIgnoreCase("stack"))
        //              desktopPane.stackFrames();
        //            else if (value.equalsIgnoreCase("cascade"))
        //              desktopPane.cascadeFrames();
        //            else if(value.equalsIgnoreCase("tile"))
        //              desktopPane.tileFrames();            
        //            break;
        case IRMODE:
          if (jsvp == null) 
            continue;
          taConvert(value.toUpperCase().startsWith("T") ? JDXSpectrum.TO_TRANS
              : value.toUpperCase().startsWith("A") ? JDXSpectrum.TO_ABS
                  : JDXSpectrum.IMPLIED);
          break;
        case INTEGRATIONRATIOS:
          // parse the string with a method in JSpecViewUtils
          System.out.println("Integration Ratio Parameter: " + value);
          integrationRatios = IntegralGraph
              .getIntegrationRatiosFromString(value);
        case INTEGRATE:
          if (jsvp == null) 
            continue;
          integrate(value);
          break;
        case EXPORT:
          if (jsvp != null && isPro())
            writeStatus(Exporter.exportCmd(jsvp, ScriptToken.getTokens(value), false));
          return;
        case OVERLAY:
          overlay(ScriptToken.getTokens(TextFormat.simpleReplace(value, "*", " * ")));
          break;
        case YSCALE:
          if (jsvp == null)
            continue;
          List<String> tokens = ScriptToken.getTokens(value);
          int pt = 0;
          boolean isAll = false;
          if (tokens.size() > 1 && tokens.get(0).equalsIgnoreCase("ALL")) {
            isAll = true;
            pt++;
          }
          double y1 = Double.parseDouble(tokens.get(pt++));
          double y2 = Double.parseDouble(tokens.get(pt));
          setYScale(y1, y2, isAll);
          break;          

        case GETSOLUTIONCOLOR:
          if (jsvp == null) 
            continue;
          setSolutionColor(true);
          break;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    repaint();
  }

  private void setSpectrum(int i) {
    if (selectedJSVPanel != null) {
      if (theInterface.equals("single")) {
        showSpectrum(i - 1, false);
      } else {
        tabbedDisplayPane.setSelectedIndex(i - 1);
      }
      repaint();
    }
  }

  private void setYScale(double y1, double y2, boolean isAll) {
    if (isAll) {
      JDXSpectrum spec = selectedJSVPanel.getSpectrum();
      for (int i = jsvPanels.size(); --i >= 0; )
        if (JDXSpectrum.areScalesCompatible(spec, jsvPanels.get(i).getSpectrum()))
          jsvPanels.get(i).setZoom(Double.NaN, y1, Double.NaN, y2);
    } else {
      selectedJSVPanel.setZoom(Double.NaN, y1, Double.NaN, y2);
    }        
  }

  private void overlay(List<String> list) {
    if (specsSaved == null)
      specsSaved = specs;
    if (list.size() == 0 || list.size() == 1
        && list.get(0).equalsIgnoreCase("all")) {
      openDataOrFile(null, "", specsSaved, null);
      setSpectrum(1);
      return;
    }
    if (list.size() == 1 && list.get(0).equalsIgnoreCase("none")) {
      openDataOrFile(null, "NONE", specsSaved, null);
      setSpectrum(1);
      return;
    }
    List<JDXSpectrum> slist = new ArrayList<JDXSpectrum>();
    StringBuffer sb = new StringBuffer();
    int n = list.size();
    for (int i = 0; i < n; i++) {
      String id = list.get(i);
      double userYFactor = 1;
      if (i + 1 < n && list.get(i + 1).equals("*")) {
        i += 2;
        try {
          userYFactor = Double.parseDouble(list.get(i));
        } catch (NumberFormatException e) {
        }
      }
      JDXSpectrum spec = findSpectrumById(id);
      if (spec == null)
        continue;
      spec.setUserYFactor(userYFactor);
      slist.add(spec);
      sb.append(",").append(id);
    }
    String s = sb.toString().substring(1);
    if (list.size() > 1 && JDXSpectrum.areScalesCompatible(slist)) {
      openDataOrFile(null, s, slist, null);
      setSpectrum(1);
    } else {
      writeStatus("Spectra cannot be overlaid: " + s);
    }
  }

  private JDXSpectrum findSpectrumById(String id) {
    int i = Parser.parseInt(id);
    return (i > 0 && i <= specsSaved.size() ? specsSaved.get(i - 1) : null);
  }

  private boolean selectPanel(String index) {
    // what if tabbed? 
    if (jsvPanels == null)
      return false;
    for (int i = 0; i < jsvPanels.size(); i++) {
      System.out.println("jsvapplet selectPanel " + index);

      if ((jsvPanels.get(i).getSpectrum()).hasPeakIndex(index)) {
        setSpectrum(i + 1);
        return true;
      }
    }
    return false;
  }

  /**
   *  call when a peak is clicked.
   * 
   * @param peak
   */
  private void sendScript(String peak) {
    selectedJSVPanel.processPeakSelect(peak);
    if (syncCallbackFunctionName == null)
      return;
    peak = Escape.jmolSelect(peak, recentURL);
    syncToJmol(peak);
  }

  /**
   * fires peakCallback ONLY if there is a peak found
   * fires coordCallback ONLY if there is no peak found or no peakCallback active
   * 
   * if (peakFound && havePeakCallback) { do the peakCallback } else { do the coordCallback }
   * 
   * Is that what we want? 
   * 
   */
  private void checkCallbacks() {
    if (coordCallbackFunctionName == null && peakCallbackFunctionName == null)
      return;
    Coordinate coord = new Coordinate();
    Coordinate actualCoord = (peakCallbackFunctionName == null ? null
        : new Coordinate());
    // will return true if actualcoord is null (just doing coordCallback)
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

  private void setSelectedPanel(JSVPanel jsvp) {
    removeKeyListener(selectedJSVPanel);
    selectedJSVPanel = jsvp;
    addKeyListener(selectedJSVPanel);
    for (int i = jsvPanels.size(); --i >= 0; )
      jsvPanels.get(i).setEnabled(jsvPanels.get(i) == jsvp);
  }

  private void sendFrameChange(JSVPanel jsvp) {
    PeakInfo pi = ((JDXSpectrum)jsvp.getSpectrum()).getSelectedPeak();
    sendScript(pi == null ? null : pi.getStringInfo());
  }

  ///////////// MISC methods from interfaces /////////////
  
  
  // called by Pro's popup window Advanced...
  void doAdvanced(String filePath) {
    // only for JSVAppletPro
  }

  
  /**
   * called from here or from MainFrame
   * @param msg
   */
  protected void syncToJmol(String msg) {
    callToJavaScript(syncCallbackFunctionName, new Object[] { fullName, msg });    
  }

  /**
   * called by notifyPeakPickedListeners in JSVPanel
   */
  public void panelEvent(Object eventObj) {
    if (eventObj instanceof PeakPickedEvent) {
      PeakPickedEvent e = (PeakPickedEvent) eventObj;
      setSelectedPanel((JSVPanel) e.getSource());
      currentSpectrumIndex = selectedJSVPanel.getIndex();
      sendScript(e.getPeakInfo());
      checkCallbacks();
    }
  }  
  
  /**
   * Legacy JavaScript call -- does NOT carry out process in order 
   * -- all parameters are set, then the file is loaded
   * -- DO NOT CALL FROM JSpecView !!!!!
   * 
   * @param newJSVparams
   *        String
   */
  public void script(String params) {
    initParams(params);
  }

}
