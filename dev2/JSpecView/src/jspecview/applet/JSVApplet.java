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
import java.awt.Font;
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
import jspecview.common.JSVPanel;
import jspecview.common.OverlayLegendDialog;
import jspecview.common.Parameters;
import jspecview.common.PeakPickedEvent;
import jspecview.common.PeakPickedListener;
import jspecview.common.PrintLayoutDialog;
import jspecview.common.ScriptInterface;
import jspecview.common.ScriptParser;
import jspecview.common.ScriptToken;
import jspecview.common.Coordinate;
import jspecview.common.IntegrationRatio;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSpecViewUtils;
import jspecview.common.PeakInfo;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.export.Exporter;
import jspecview.source.JDXFileReader;
import jspecview.source.JDXSource;
import jspecview.util.Escape;
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

public class JSVApplet extends JApplet implements PeakPickedListener, ScriptInterface {

  public static final String APPLET_VERSION = "2.0.20120227-0500"; //
//  2.0.yyyymmdd-hhmm format - should be updated to keep track of the latest version (based on Jamaica time)
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Override
  public void finalize() {
    System.out.println("JSpecView " + this + " finalized");
  }

  /* --------------------set default-PARAMETERS -------------------------*/
  private String filePath;
  private String newFilePath = null;
  private String recentFileName = "";
  private String recentURL = "";

  private ArrayList<IntegrationRatio> integrationRatios = null; // Integration Ratio Annotations


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


  /*---------------------------------END PARAMETERS------------------------*/

  private boolean isSignedApplet = false;
  private boolean isStandalone = false;
  private JPanel statusPanel = new JPanel();
  private JLabel statusTextLabel = new JLabel();
  private JFileChooser jFileChooser;
  private JSVPanel selectedJSVPanel;
  private JSVAppletPopupMenu appletPopupMenu;

  @Override
  public void destroy() {
    System.out.println("JSVApplet " + this + " destroy 1");
    if (commandWatcherThread != null) {
      commandWatcherThread.interrupt();
      commandWatcherThread = null;
    }
    if (jsvPanels != null) {
      for (int i = jsvPanels.size(); --i >= 0;) {
        jsvPanels.get(i).destroy();
        jsvPanels.remove(i);
      }
    }
    System.out.println("JSVApplet " + this + " destroy 2");
  }

  private boolean newFile = false;

  /**
   * parameters from a javascript call
   */
  private String JSVparams;

  /**
   * The panes of a tabbed display
   */
  private JTabbedPane spectraPane = new JTabbedPane();

  /**
   * A list of </code>JDXSpectrum</code> instances
   */
  private List<JDXSpectrum> specs;

  /**
   * The <code>JSVPanel</code>s created for each </code>JDXSpectrum</code>
   */
  private List<JSVPanel> jsvPanels;

  /**
   * The <code>JDXSource</code> instance
   */
  private JDXSource source;

  /**
   * The Panel on which the applet contents are drawn
   */
  private JPanel appletPanel;

  /**
   * Frame constructed from applet panel when rising off web page
   */
  private JFrame frame;

  /**
   * The index of the <code>JDXSpectrum</code> that is is focus.
   */
  private int currentSpectrumIndex = 0;

  /**
   * Whether or not spectra should be overlayed
   */
  private boolean overlay;
  private boolean obscure;

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
    JSVparams = getParameter("script");
    init(null);
    if (appletReadyCallbackFunctionName != null && fullName != null)
      callToJavaScript(appletReadyCallbackFunctionName, new Object[] {
          appletID, fullName, Boolean.TRUE });
  }

  private void init(String data) {
    // this is tricky
    String execScript = null;
    if (data != null) {
      // have string data
    } else if (!newFile) {
      parseInitScript(execScript = JSVparams);
    } else if (newFilePath != null) {
        filePath = newFilePath;
    }
    // enable or disable menus
    appletPopupMenu = new JSVAppletPopupMenu(this, isSignedApplet);
    appletPopupMenu.enableMenus(allowMenu, enableZoom);
    //setBackground(backgroundColor);

    try {
      jbInit();
    } catch (Exception e) {
      e.printStackTrace();
    }
    appletPanel = new JPanel(new BorderLayout());
    Font statusFont = new Font(null, Font.PLAIN, 12);
    statusTextLabel.setFont(statusFont);
    statusTextLabel.setForeground(Color.darkGray);
    statusPanel.add(statusTextLabel, null);
    this.getContentPane().add(appletPanel);
    if (execScript == null)
      openDataOrFile(data, null, null);
    else
      checkScriptNow(execScript);
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

    if (startIndex >= 0 && endIndex > startIndex)
      showRange = true;

    // Initialise JSVpanels

    if (overlay) {
      // overlay all spectra on a panel
      jsvPanels = new ArrayList<JSVPanel>();

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
        overlay = false;
        initPanels();
        return;
      }
      endIndex = startIndex = -1;
      jsvPanels.add(jsvp);
      initProperties(jsvp, 0);
      selectedJSVPanel = jsvp;
      jsvp.setIndex(currentSpectrumIndex = 0);
    } else {
      // initialise JSVPanels and add them to the array
      jsvPanels = new ArrayList<JSVPanel>();
      try {
        for (int i = 0; i < numberOfSpectra; i++) {
          JDXSpectrum spec = specs.get(i);
          if (spec.getIntegrationGraph() != null) {
            jsvp = JSVPanel.getIntegralPanel(spec, null);
          } else if (showRange) {
            jsvp = new JSVPanel(spec, startIndex,
                endIndex);
          } else {
            jsvp = new JSVPanel(spec);
          }
          jsvPanels.add(jsvp);
          initProperties(jsvp, i);
        }
      } catch (Exception e) {
        // TODO
      }
    }
  }

  private Parameters parameters = new Parameters("applet");
  
  private void initProperties(JSVPanel jsvp, int index) {
    // set JSVPanel properties from applet parameters
    jsvp.addPeakPickedListener(this);
    jsvp.setIndex(index);
    parameters.setFor(jsvp, null, true);
    jsvp.setXAxisDisplayedIncreasing((jsvp.getSpectrum()).shouldDisplayXAxisIncreasing());
    jsvp.setSource(source);
    jsvp.setPopup(appletPopupMenu);

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
        spectraPane.addTab(title, jsvPanels.get(i));
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
      if (overlay && source.isCompoundSource) {
        jsvPanels.get(spectrumIndex).setTitle(source.getTitle());
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
          if (source.isCompoundSource) {
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
                .setText(source.type == JDXSource.TYPE_OVERLAY ? "Spectra" 
                    : source.type == JDXSource.TYPE_BLOCK ? "Blocks" : "NTuples");
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
    JSVPanel jsvp = jsvPanels.get(index);
    appletPanel.remove(jsvPanels.get(currentSpectrumIndex));
    appletPanel.add(jsvp, BorderLayout.CENTER);
    currentSpectrumIndex = index;
    setSelectedPanel(jsvp);

    appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(selectedJSVPanel.getSpectrum().isHNMR());

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
  protected void headerMenuItem_actionPerformed(ActionEvent e) {
    
    JDXSpectrum spectrum = selectedJSVPanel.getSpectrum();
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
   */
  public void setSolutionColor(boolean showMessage) {
    sltnclr = selectedJSVPanel.getSolutionColor();
    if (showMessage)
      JSVPanel.showSolutionColor((Component)this, sltnclr);
  }

  /**
   * Opens the print dialog to enable printing
   * 
   * @param e
   *        ActionEvent
   */
  protected void printMenuItem_actionPerformed(ActionEvent e) {
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
  protected void windowMenuItem_itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      // disable some features when in Window mode
      appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(false);
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
          appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(true);
          //       if (compoundMenuOn)
          //             compoundMenu.setEnabled(true);
          frame.removeAll();
          frame.dispose();
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
      frame.removeAll();
      frame.dispose();
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
  protected void integrate(String value) {
    boolean showMessage = value.equals("?");
    int mode = (value.equals("?") ? JDXSpectrum.INTEGRATE_TOGGLE
        : value.equalsIgnoreCase("OFF") ? JDXSpectrum.INTEGRATE_OFF
        : JDXSpectrum.INTEGRATE_ON);
    JSVPanel jsvp = getCurrentPanel();
    JSVPanel jsvpNew = AppUtils.checkIntegral(jsvp, appletPanel, mode,
        showMessage, integrationRatios);
    if (jsvp == jsvpNew)
      return;
    initProperties(jsvpNew, currentSpectrumIndex);
    jsvpNew.repaint();
    integrationRatios = null;
    chooseContainer();
  }

  // check which mode the spectrum is in (windowed or applet)
  private void chooseContainer() {
    // check first if we have ever had a frame
    if (frame == null) {
      appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(true);
      if (compoundMenuOn)
        appletPopupMenu.compoundMenu.setEnabled(true);
      getContentPane().add(appletPanel);
      validate();
      repaint();
    } else {
      if (frame.getComponentCount() != 0) {
        appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(false);
        appletPopupMenu.compoundMenu.setEnabled(false);
        frame.add(appletPanel);
        frame.validate();
        frame.setVisible(true);
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
  private boolean enableZoom = true;
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
  protected void exportSpectrum(String command) {
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
      JDXSpectrum spec = selectedJSVPanel.getSpectrumAt(n);
      errMsg = Exporter.export(comm, (file == null ? null : file
          .getAbsolutePath()), spec, 0, spec.getXYCoords().length - 1);
    } catch (IOException ioe) {
      errMsg = "Error writing: " + file.getName();
    }
    return errMsg;
  }

  /**
   * Used to tile JSVPanel when the <i>interface</i> paramters is equal to
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

    ComponentListPair pair = new ComponentListPair();
    pair.bottom = bottom;
    pair.top = top;

    return pair;
  }

  /**
   * Representation of two array of components of equal or nearly equal size
   */
  private class ComponentListPair {

    /**
     * the first array
     */
    public List<JSVPanel> top;

    /**
     * the second array
     */
    public List<JSVPanel> bottom;

    /**
     * Constructor
     */
    public ComponentListPair() {
    }
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
      scriptQueue.add("load " + tmpFilePath);
    else
      setFilePathLocal(tmpFilePath);
  }

  /**
   * Loads a new file into the existing applet window
   * 
   * @param tmpFilePath
   *        String
   */
  private void setFilePathLocal(String tmpFilePath) {
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
    if (appletPopupMenu.integrateCheckBoxMenuItem.isSelected() == false)
      appletPopupMenu.integrateCheckBoxMenuItem.setSelected(true);
    else
      appletPopupMenu.integrateCheckBoxMenuItem.setSelected(false);
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
      ScriptToken st = ScriptToken.getScriptToken(key);
      String value = ScriptParser.getValue(st, eachParam, token);
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
          obscure = Boolean.parseBoolean(value);
          JSpecViewUtils.setObscure(obscure);
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
        case ENABLEZOOM:
          enableZoom = Boolean.parseBoolean(value);
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
        case SPECTRUM:
          spectrumNumber = Integer.parseInt(value);
          break;
        case AUTOINTEGRATE:
          autoIntegrate = Parameters.parseBoolean(value);
          break;
        case IRMODE:
          irMode = (value.toUpperCase().startsWith("T") ? JDXSpectrum.TO_TRANS
              : JDXSpectrum.TO_ABS);
       
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
  private List<String> scriptQueue = new ArrayList<String>();
  private Thread commandWatcherThread;

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
              if (scriptItem != null) {
                checkScriptNow(scriptItem);
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

  int nOverlays;
  private List<JDXSpectrum> specsSaved;

  /*
    private void interruptQueueThreads() {
      if (commandWatcherThread != null)
        commandWatcherThread.interrupt();
    }
  */
  private void openDataOrFile(String data, String name, List<JDXSpectrum> specs1) {
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
      source = (isOverlay ? JDXSource.createOverlay(fileName, specs)
          : JDXFileReader.createJDXSource(data, fileName, base));
    } catch (Exception e) {
      writeStatus(e.getMessage());
      e.printStackTrace();
      return;
    }

    specs = source.getSpectra();
    numberOfSpectra = specs.size();
    overlay = isOverlay && !name.equals("NONE") || (theInterface.equals("overlay") && numberOfSpectra > 1);
    overlay &= !JDXSpectrum.process(specs, irMode, !isOverlay && autoIntegrate);
    
    boolean continuous = source.getJDXSpectrum(0).isContinuous();
    String Yunits = source.getJDXSpectrum(0).getYUnits();
    String Xunits = source.getJDXSpectrum(0).getXUnits();
    double firstX = source.getJDXSpectrum(0).getFirstX();
    double lastX = source.getJDXSpectrum(0).getLastX();

    compoundMenuOn = allowCompoundMenu && source.isCompoundSource;

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
    if (continuous && selectedJSVPanel.getSpectrum().isHNMR())
      appletPopupMenu.integrateCheckBoxMenuItem.setEnabled(true);
    //Can only convert from T <-> A  if Absorbance or Transmittance and continuous
    if ((continuous) && (Yunits.toLowerCase().contains("abs"))
        || (Yunits.toLowerCase().contains("trans")))
      appletPopupMenu.transAbsMenuItem.setEnabled(true);
    else
      appletPopupMenu.transAbsMenuItem.setEnabled(false);
    appletPopupMenu.exportAsMenu.setEnabled(true);
    appletPopupMenu.saveAsJDXMenu.setEnabled(continuous);
    newFile = false;
  }

  /////////// simple sync functionality //////////

  public void checkScriptNow(String params) {
    if (params == null)
      params = "";
    params = params.trim();
    System.out.println("CHECKSCRIPT " + params);
    StringTokenizer allParamTokens = new StringTokenizer(params, ";");
    if (JSpecViewUtils.DEBUG) {
      System.out.println("Running in DEBUG mode");
    }
    JSVPanel jsvp = selectedJSVPanel;
    while (allParamTokens.hasMoreTokens()) {
      String token = allParamTokens.nextToken();
      // now split the key/value pair
      StringTokenizer eachParam = new StringTokenizer(token);
      String key = ScriptParser.getKey(eachParam);
      ScriptToken st = ScriptToken.getScriptToken(key);
      String value = ScriptParser.getValue(st, eachParam, token);
      System.out.println("KEY-> " + key + " VALUE-> " + value + " : " + st);
      try {
        switch (st) {
        case UNKNOWN:
          System.out.println("Unrecognized parameter: " + key);
          break;
        case LOAD:
          specsSaved = null;
          filePath = value;
          openDataOrFile(null, null, null);
          setSpectrumNumber(1);
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
        case SPECTRUMNUMBER:
        case SPECTRUM:
          setSpectrumNumber(Integer.parseInt(value));
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
          integrationRatios = JSpecViewUtils
              .getIntegrationRatiosFromString(value);
        case INTEGRATE:
          if (jsvp == null) 
            continue;
          integrate(value);
          break;
        case OVERLAY:
          overlay(TextFormat.split(value, ","));
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

  
  private void overlay(String[] ids) {
    if (specsSaved == null)
      specsSaved = specs;
    if (ids.length == 0 || ids.length == 1 && ids[0].equalsIgnoreCase("all")) {
      openDataOrFile(null, "", specsSaved);
      setSpectrumNumber(1);
      return;
    }
    if (ids.length == 1 && ids[0].equalsIgnoreCase("none")) {
      openDataOrFile(null, "NONE", specsSaved);
      setSpectrumNumber(1);
      return;
    }
    List<JDXSpectrum> list = new ArrayList<JDXSpectrum>();
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < ids.length; i++) {
      JDXSpectrum spec = findSpectrumById(ids[i]);
      if (spec == null)
        continue;
        list.add(spec);
        sb.append(",").append(ids[i]);
    }
    if (list.size() > 1 && JDXSpectrum.areScalesCompatible(list)) {
      openDataOrFile(null, sb.toString().substring(1), list);
      setSpectrumNumber(1);
    }
  }

  private JDXSpectrum findSpectrumById(String id) {
    int i = Parser.parseInt(id);
    return (i >= 0 && i < specsSaved.size() ? specsSaved.get(i) : null);
  }

  /**
   * preceed <Peaks here with full name of Jmol applet (including syncID)
   * 
   */
  public void syncScript(String script) {
    if (script.indexOf("<PeakData") < 0) {
      checkScript(script);
      return;
    }
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
    if (!f.equals(recentURL))
      setFilePathLocal(file);
    if (!selectPanel(index))
      script = null;
    selectedJSVPanel.processPeakSelect(script);
    sendFrameChange(selectedJSVPanel);
  }

  public void checkScript(String script) {
    scriptQueue.add(script);
  }
  
  private boolean selectPanel(String index) {
    // what if tabbed? 
    if (jsvPanels == null)
      return false;
    for (int i = 0; i < jsvPanels.size(); i++) {
      if ((jsvPanels.get(i).getSpectrum()).hasPeakIndex(index)) {
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

  public void peakPicked(PeakPickedEvent eventObj) {
    setSelectedPanel((JSVPanel) eventObj.getSource());
    currentSpectrumIndex = selectedJSVPanel.getIndex();
    sendScript(eventObj.getPeakInfo());
    checkCallbacks();
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

}
