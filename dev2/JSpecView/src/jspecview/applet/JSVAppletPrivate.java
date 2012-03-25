/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General private
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General private License for more details.
 *
 *  You should have received a copy of the GNU Lesser General private
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
//            x scale, and y scale.  Added parameteres for the font,
//            title font, and integral plot color.  Added a method
//            to reset view from a javascript call.
// 24-09-2011 jak - Added parameter for integration ratio annotations.
// 08-10-2011 jak - Add a method to toggle integration from a javascript
//          call. Changed behaviour to remove integration after reset
//          view.

package jspecview.applet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.ArrayList;

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

import jspecview.common.AwtPanel;
import jspecview.common.IntegralGraph;
import jspecview.common.JSVAppletInterface;
import jspecview.common.JSVContainer;
import jspecview.common.JSVDialog;
import jspecview.common.JSVDropTargetListener;
import jspecview.common.JSVPanel;
import jspecview.common.JSVSpecNode;
import jspecview.common.JSViewer;
import jspecview.common.OverlayLegendDialog;
import jspecview.common.AwtParameters;
import jspecview.common.PanelData;
import jspecview.common.PanelListener;
import jspecview.common.Parameters;
import jspecview.common.PeakPickEvent;
import jspecview.common.PrintLayoutDialog;
import jspecview.common.ScriptInterface;
import jspecview.common.ScriptToken;
import jspecview.common.Coordinate;
import jspecview.common.Annotation;
import jspecview.common.JDXSpectrum;
import jspecview.common.PeakInfo;
import jspecview.common.SubSpecChangeEvent;
import jspecview.common.ZoomEvent;
import jspecview.exception.JSpecViewException;
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

public class JSVAppletPrivate implements PanelListener, ScriptInterface, JSVAppletInterface {

  protected JSVApplet jsvApplet;

  JSVApplet getJsvApplet() {
    return jsvApplet;
  }

  int nOverlays;

  JSVAppletPrivate(JSVApplet jsvApplet) {
    this.jsvApplet = jsvApplet;
    try {
      jFileChooser = new JFileChooser();
      isSignedApplet = true;
    } catch (Exception e) {
      // just not a signed applet
    }
    init();
  }

  /////////////// applet panel ///////////////// 

  private class JSVAppletPanel extends JPanel implements JSVContainer {
    
    private static final long serialVersionUID = 1L;

    private JSVAppletPanel(BorderLayout borderLayout) {
      super(borderLayout);
    }

    public void dispose() {
    }

    public String getTitle() {
      // TODO Auto-generated method stub
      return null;
    }

    public void setTitle(String title) {
      // TODO Auto-generated method stub
      
    }
    
  }
  /* -------------------- default PARAMETERS -------------------------*/

  private JSVPanel prevPanel;
  private ArrayList<Annotation> integrationRatios; // Integration Ratio Annotations
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

  private AwtParameters parameters = new AwtParameters("applet");
  private AwtParameters tempParams = new AwtParameters("temp");
   

  /*---------------------------------END PARAMETERS------------------------*/

  boolean isStandalone;  // not changed 

  private String appletID;
  private String syncID;
  private Thread commandWatcherThread;
  private boolean isSignedApplet;
  private Boolean obscureTitleFromUser;
  private JFileChooser jFileChooser;
  private JTabbedPane tabbedDisplayPane = new JTabbedPane();
  private JFrame offWindowFrame;
  private JSVAppletPanel appletPanel;
  private JSVAppletPopupMenu appletPopupMenu;  
  private List<JSVSpecNode> specNodes;
  private List<JSVSpecNode> specNodesSaved;
  private int currentSpectrumIndex;
  private String recentFileName = "";
  private JSVPanel selectedPanel;
  private JDXSource currentSource;
  private boolean isOverlaid;

  public boolean isSigned() {
    return isSignedApplet;
  }
  
  public boolean isPro() {
    return isSigned();
  }

  JDXSource getSource() {
    return currentSource;
  }

  void dispose() {
    jFileChooser = null;
    try {
      if (commandWatcherThread != null) {
        commandWatcherThread.interrupt();
        commandWatcherThread = null;
      }
      if (specNodes != null)
        for (int i = specNodes.size(); --i >= 0;) {
          specNodes.get(i).dispose();
          specNodes.remove(i);
        }
      if (specNodesSaved != null)
        for (int i = specNodesSaved.size(); --i >= 0;) {
          specNodesSaved.get(i).dispose();
          specNodesSaved.remove(i);
        }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /////////////// private methods called from page or browser JavaScript calls ////////////////
  //
  //
  // Notice that in all of these we use getSelectedPanel(), not selectedJSVPanel
  // That's because the methods aren't overridden in JSVAppletPro, and in that case
  // we want to select the panel from MainFrame, not here. Thus, when the Advanced...
  // tab is open, actions from outside of Jmol act on the MainFrame, not here. 
  //
  // BH - 8.3.2012
  
  public Map<String, Object> getPropertyAsJavaObject(String key) {
    if ("".equals(key))
      key = null;
    List<Map<String, Object>> info = new ArrayList<Map<String, Object>>();
    for (int i = 0; i < specNodes.size(); i++) {
      JSVPanel jsvp = specNodes.get(i).jsvp;
      if (jsvp == null)
        continue;
      info.add(jsvp.getPanelData().getInfo(true, key));
    }
    Map<String, Object> map = new Hashtable<String, Object>();
    map.put("items", info);
    return map;
  }
  
  public String getPropertyAsJSON(String key) {
    Map<String, Object> map = getPropertyAsJavaObject(key);
    return Escape.toJSON(null, map);
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
      Coordinate coord = getSelectedPanel().getPanelData().getClickedCoordinate();
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
    jsvApplet.getContentPane().validate();
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
    return ((AwtPanel) getSelectedPanel()).export(type, n);
  }

  public void setFilePath(String tmpFilePath) {
    if (isSignedApplet)
      processCommand("load " + tmpFilePath);
    else
      closeAllAndOpenFile(tmpFilePath);
  }

  /**
   * Sets the spectrum to the specified block number
   * 
   * @param i
   */
  public void setSpectrumNumber(int i) {
    setSpectrumIndex(i - 1);
  }
  
  /**
   * Method that can be called from another applet or from javascript that
   * toggles the grid on a <code>JSVPanel</code>
   */
  public void toggleGrid() {
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp == null)
      return;
    Parameters.setBoolean(jsvp, tempParams, ScriptToken.GRIDON, !jsvp.getPanelData().isGridOn());
    jsvApplet.repaint();
  }

  /**
   * Method that can be called from another applet or from javascript that
   * toggles the coordinate on a <code>JSVPanel</code>
   */
  public void toggleCoordinate() {
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp == null)
      return;
    Parameters.setBoolean(jsvp, tempParams, ScriptToken.COORDINATESON, 
        !jsvp.getPanelData().isCoordinatesOn());
    jsvApplet.repaint();
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
    Color color = new Color(r, g, b, a);
    JSViewer.addHighLight(this, x1, x2, color);
    jsvApplet.repaint();
  }

  /**
   * Method that can be called from another applet or from javascript that
   * removes all highlights from the plot area of a <code>JSVPanel</code>
   */
  public void removeAllHighlights() {
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp != null) {
      jsvp.getPanelData().removeAllHighlights();
      jsvApplet.repaint();
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
      jsvp.getPanelData().removeHighlight(x1, x2);
      jsvApplet.repaint();
    }
  }

  /**
   * Method that can be called from another applet or from javascript that
   * toggles reversing the plot on a <code>JSVPanel</code>
   */
  public void reversePlot() {
    JSVPanel jsvp = getSelectedPanel();
    if (jsvp != null) {
      jsvp.getPanelData().setReversePlot(!jsvp.getPanelData().isPlotReversed());
      jsvApplet.repaint();
    }
  }

  public void syncScript(String peakScript) {
    JSViewer.syncScript(this, peakScript);
  }

  /**
   * Writes a message to the status label
   * 
   * @param msg
   *        the message
   */
  public void writeStatus(String msg) {
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
  private void init() {
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
    
    initParams(jsvApplet.getParameter("script"));
    if (appletReadyCallbackFunctionName != null && fullName != null)
      callToJavaScript(appletReadyCallbackFunctionName, new Object[] {
          appletID, fullName, Boolean.TRUE });
    
    if (isSignedApplet) {
      new DropTarget(jsvApplet, getDropListener());
    }
  }

  private DropTargetListener dtl;
  private DropTargetListener getDropListener() {
    if (dtl == null)
      dtl = new JSVDropTargetListener(this, false);
    return dtl;
  }

  /**
   * starts or restarts applet display from scratch or from a JSVApplet.script()
   * JavaScript command
   * 
   * Involves a two-pass sequence through parsing the parameters, because order
   * is not important in this sort of call.
   * 
   * To call a script and have commands execute in order, use
   * 
   * JSVApplet.runScript(script)
   * 
   * instead
   * 
   * @param params
   */
  void initParams(String params) {
    parseInitScript(params);
    newAppletPanel();
    appletPopupMenu = new JSVAppletPopupMenu(this, allowMenu, parameters
        .getBoolean(ScriptToken.ENABLEZOOM));
    runScriptNow(params);
  }
  
  private void newAppletPanel() {
    System.out.println("\n\n----newAppletPanel----\n\n");
    jsvApplet.getContentPane().removeAll();
    appletPanel = new JSVAppletPanel(new BorderLayout());
    jsvApplet.getContentPane().add(appletPanel);
  }

  /**
   * Initalizes the <code>specNodes</code> and adds them to the specNodes array
   * 
   * @throws JSpecViewException
   */
  private void initPanels(List<JDXSpectrum> specs) throws JSpecViewException {
    AwtPanel jsvp;

    // Initialise specNodes

    if (isOverlaid && startIndex != endIndex) {
      theInterface = "single";
      isOverlaid = false;
    }
    specNodes = new ArrayList<JSVSpecNode>();
    if (isOverlaid) {
      // overlay all spectra on a panel
      jsvp = AwtPanel.getJSVPanel(specs, startIndex, endIndex, appletPopupMenu);
      endIndex = startIndex = -1;
      initProperties(jsvp, -1);
      specNodes.add(new JSVSpecNode("specs", currentSource.getFilePath(), currentSource, appletPanel, jsvp));
    } else {
      // initialise specNodes and add them to the array
      try {
        for (int i = 0; i < numberOfSpectra; i++) {
          JDXSpectrum spec = specs.get(i);
          if (spec.getIntegrationGraph() != null) {
            jsvp = AwtPanel.getNewPanel(spec, appletPopupMenu);
          } else {
            List<JDXSpectrum> list = new ArrayList<JDXSpectrum>();
            list.add(spec);
            jsvp = AwtPanel.getJSVPanel(list, startIndex, endIndex, appletPopupMenu);
          }
          specNodes.add(new JSVSpecNode("" + (i + 1), currentSource.getFilePath(), currentSource, appletPanel, jsvp));
          initProperties(jsvp, i);
        }
      } catch (Exception e) {
        // TODO
      }
    }
  }

  private void initProperties(JSVPanel jsvp, int oldIndex) {
    jsvp.getPanelData().addListener(this);
    parameters.setFor(jsvp, null, true);
    setSelectedPanel(jsvp);
  }

  /**
   * Initializes the interface of the applet depending on the value of the
   * <i>interface</i> parameter
   */
  private void initInterface() {
    final int numberOfPanels = specNodes.size();
    boolean canDoTile = (numberOfPanels >= 2 && numberOfPanels <= 10);
    boolean moreThanOnePanel = (numberOfPanels > 1);
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
        String title = specNodes.get(i).getSpectrum().getTitleLabel();
        if (currentSource.type == JDXSource.TYPE_NTUPLE)
          title = title.substring(title.indexOf(':') + 1);
        else if (currentSource.type == JDXSource.TYPE_BLOCK)
          //title = "block " + (i + 1);
          title = title.substring(0, (title.length() >= 10 ? 10 : title
              .length()))
              + "... : ";
        tabbedDisplayPane.addTab(title, (AwtPanel)specNodes.get(i).jsvp);
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
        JSVPanel jsvp = specNodes.get(i).jsvp;
        ((Component) jsvp).setMinimumSize(new Dimension(250, 150));
        ((Component) jsvp).setPreferredSize(new Dimension(300, 200));
      }
      JSplitPane splitPane = createSplitPane(specNodes);
      appletPanel.add(splitPane, BorderLayout.CENTER);
      //splitPane.setBackground(backgroundColor);
    } else { // Single or overlay
      //      compoundMenuOn = true;
      int spectrumIndex = (showSpectrumNumber ? spectrumNumber - 1 : 0);
            
      setSelectedPanel(spectrumIndex >= specNodes.size() ? null : specNodes.get(spectrumIndex).jsvp);

      // Global variable for single interface

      if (isOverlaid && currentSource.isCompoundSource)
        specNodes.get(spectrumIndex).jsvp.setTitle(currentSource.getTitle());
      appletPanel.add((AwtPanel)specNodes.get(spectrumIndex).jsvp, BorderLayout.CENTER);      
      appletPopupMenu.setCompoundMenu(currentSource, spectrumIndex, 
          (specNodesSaved == null ? specNodes : specNodesSaved), 
          compoundMenuOn && currentSource.isCompoundSource, 
          compoundMenuSelectionListener, compoundMenuChooseListener);  
    }
  }

  private ActionListener compoundMenuChooseListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      resetSaved();
      StringBuffer msgStrBuffer = new StringBuffer();
      msgStrBuffer.append("Choose a number between 1 and ");
      msgStrBuffer.append(specNodes.size());
      msgStrBuffer.append(" to display another spectrum");

      String str = JOptionPane.showInputDialog(jsvApplet,
          msgStrBuffer.toString(), "Spectrum Chooser",
          JOptionPane.PLAIN_MESSAGE);
      if (str != null) {
        int index = 0;
        try {
          index = Integer.parseInt(str) - 1;
        } catch (NumberFormatException nfe) {
        }
        if (index > 0 && index < specNodes.size()) {
          showSpectrum(index);
          writeStatus(" ");
        } else
          writeStatus("Invalid Spectrum Number");
      }
    }
  };

  private ActionListener compoundMenuSelectionListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        compoundMenu_itemStateChanged(e);
    }
  };
   
  private void compoundMenu_itemStateChanged(ActionEvent e) {
    String txt = ((JMenuItem) e.getSource()).getText();
    resetSaved();
    showSpectrum(Parser.parseInt(txt) - 1);
  }

  /**
   * Shows the </code>JSVPanel</code> at a certain index
   * 
   * @param index
   *        the index
   */
  private void showSpectrum(int index) {
    JSVPanel jsvp = specNodes.get(index).jsvp;
    if (jsvp != getSelectedPanel())
      setSelectedPanel(jsvp);
    sendFrameChange(jsvp);
  }

  public void sendFrameChange(JSVPanel jsvp) {
    if (jsvp == prevPanel)
      return;
    prevPanel = jsvp;
    PeakInfo pi = jsvp.getSpectrum().getSelectedPeak();
    if (pi == null)
      pi = PeakInfo.nullPeakInfo;
    getSelectedPanel().getPanelData().addPeakHighlight(pi);
    sendScript(pi.toString());
  }

  private long msTrigger = -1;

  private JSVPanel getCurrentPanel() {
    return (JSVPanel) appletPanel.getComponent(0);
  }

  private void addPanelToFrame() {
    if (offWindowFrame == null || offWindowFrame.getComponentCount() == 0) {
      jsvApplet.getContentPane().add(appletPanel);
      jsvApplet.validate();
      jsvApplet.repaint();
    } else {
      offWindowFrame.add(appletPanel);
      offWindowFrame.validate();
      offWindowFrame.setVisible(true);
    }
  }

  /**
   * Shows a floating overlay key if possible
   * 
   * @param e
   *        the ActionEvent
   */
  protected void showOverlayKey(boolean visible) {
    JSViewer.setOverlayLegendVisibility(this, getSelectedPanel(), visible);
  }

  private String fullName;
  private boolean allowMenu = true;
  private boolean compoundMenuOn;
  private boolean allowCompoundMenu = true;
  private String dirLastExported;
  
  
  ////////////// JSVAppletPopupMenu calls
  
  
  /**
   * Shows the header information for the Spectrum
   */
  void showHeader() {
    
    JDXSpectrum spectrum = getSelectedPanel().getSpectrum();
    String[][] rowData = (isOverlaid ? currentSource
        .getHeaderRowDataAsArray(false, 0) : spectrum
        .getHeaderRowDataAsArray());
    String[] columnNames = { "Label", "Description" };
    JTable table = new JTable(rowData, columnNames);
    table.setPreferredScrollableViewportSize(new Dimension(400, 195));
    JScrollPane scrollPane = new JScrollPane(table);
    JOptionPane.showMessageDialog(jsvApplet, scrollPane, "Header Information",
        JOptionPane.PLAIN_MESSAGE);
  }

  /**
   * Opens the print dialog to enable printing
   */
  void print() {
    if (offWindowFrame == null) {
      System.err
          .println("Use the View/Window menu to lift the spectrum off the page first.");
      return;
    }

    JSVPanel jsvp = getSelectedPanel();

    PrintLayoutDialog ppd = new PrintLayoutDialog(offWindowFrame);
    PrintLayoutDialog.PrintLayout pl = ppd.getPrintLayout();

    if (pl != null)
      ((AwtPanel) jsvp).printSpectrum(pl);
  }

  /**
   * Shows the applet in a Frame
   */
  void newWindow(boolean isSelected) {
    if (isSelected) {
      offWindowFrame = new JFrame("JSpecView");
      offWindowFrame.setSize(jsvApplet.getSize());
      final Dimension d;
      d = appletPanel.getSize();
      offWindowFrame.add(appletPanel);
      offWindowFrame.validate();
      offWindowFrame.setVisible(true);
      jsvApplet.remove(appletPanel);
      jsvApplet.validate();
      jsvApplet.repaint();
      offWindowFrame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          appletPanel.setSize(d);
          jsvApplet.getContentPane().add(appletPanel);
          jsvApplet.setVisible(true);
          jsvApplet.validate();
          jsvApplet.repaint();
          offWindowFrame.removeAll();
          offWindowFrame.dispose();
          appletPopupMenu.windowMenuItem.setSelected(false);
        }
      });
    } else {
      jsvApplet.getContentPane().add(appletPanel);
      jsvApplet.validate();
      jsvApplet.repaint();
      offWindowFrame.removeAll();
      offWindowFrame.dispose();
    }
  }

  /**
   * Export spectrum in a given format
   * 
   * @param command
   *        the name of the format to export in
   */
  void exportSpectrumViaMenu(String type) {
    if (isSignedApplet)
      dirLastExported = Exporter.exportSpectra(getSelectedPanel(),
          offWindowFrame, jFileChooser, type, recentFileName, dirLastExported);
    else
      Logger.info(export(type, -1));
  }

  /**
   * Used to tile JSVPanel when the <i>interface</i> parameters is equal to
   * "tile"
   * 
   * @param specNodes
   *        An array of components to tile
   * @return a <code>JSplitPane</code> with components tiled
   */
  private JSplitPane createSplitPane(List<JSVSpecNode> specNodes) {
    ComponentListPair pair = createPair(specNodes);
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
      splitPane.setLeftComponent((Component) pair.top.get(0).jsvp);
      splitPane.setRightComponent((Component) pair.bottom.get(0).jsvp);

    }

    else if (numBottom == 1 && numTop == 2) {
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      JSplitPane newSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      newSplitPane.setLeftComponent((Component) pair.top.get(0).jsvp);
      newSplitPane.setRightComponent((Component) pair.top.get(1).jsvp);
      splitPane.setLeftComponent(newSplitPane);
      splitPane.setRightComponent((Component) pair.bottom.get(0).jsvp);
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
   * @param specNodes
   *        an array of components
   * @return a <code>ComponentListPair</code>
   */
  private ComponentListPair createPair(List<JSVSpecNode> specNodes) {
    int numBottom = (int) (specNodes.size() / 2);
    int numTop = numBottom + (specNodes.size() % 2);
    List<JSVSpecNode> top = new ArrayList<JSVSpecNode>();
    List<JSVSpecNode> bottom = new ArrayList<JSVSpecNode>();
    int i;
    for (i = 0; i < numTop; i++)
      top.add(specNodes.get(i));
    for (; i < specNodes.size(); i++)
      bottom.add(specNodes.get(i));
    return new ComponentListPair(top, bottom);
  }

  /**
   * Representation of array[2] of components of equal or nearly equal size
   * 
   */
  private class ComponentListPair {
    
    List<JSVSpecNode> top; 
    List<JSVSpecNode> bottom;

    private ComponentListPair(List<JSVSpecNode> top, List<JSVSpecNode> bottom) {
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
  public void closeAllAndOpenFile(String filePath) {
    newAppletPanel();
    openDataOrFile(null, null, null, filePath);
    jsvApplet.getContentPane().validate();
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
      JSObject.getWindow(jsvApplet).call(function, params);
    } catch (Exception npe) {
      Logger.warn("EXCEPTION-> " + npe.getMessage());
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
      Logger.info("Running in DEBUG mode");
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
      if (Logger.debugging)
        Logger.info("KEY-> " + key + " VALUE-> " + value + " : " + st);
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
          autoIntegrate = AwtParameters.isTrue(value);
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

  /*
    private void interruptQueueThreads() {
      if (commandWatcherThread != null)
        commandWatcherThread.interrupt();
    }
  */
  private void openDataOrFile(String data, String name,
                              List<JDXSpectrum> specs, String filePath) {
    appletPanel.removeAll();
    String fileName = null;
    URL base = null;
    boolean isOverlay = false;
    if (specs != null) {
      fileName = "Overlay" + (++nOverlays);
      isOverlay = true;
    } else if (data != null) {
    } else if (filePath != null) {
      URL url;
      try {
        url = new URL(jsvApplet.getCodeBase(), filePath);
        fileName = url.toString();
        recentFileName = url.getFile();
        base = jsvApplet.getDocumentBase();
      } catch (MalformedURLException e) {
        Logger.warn("problem: " + e.getMessage());
        fileName = filePath;
      }
    } else {
      writeStatus("Please set the 'filepath' or 'load file' parameter");
      return;
    }

    try {
      currentSource = (isOverlay ? 
          JDXSource.createOverlay(fileName, specs)
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

    compoundMenuOn = allowCompoundMenu && currentSource.isCompoundSource;

    try {
      initPanels(specs);
    } catch (JSpecViewException e1) {
      writeStatus(e1.getMessage());
      return;
    }

    initInterface();

    Logger.info(jsvApplet.getAppletInfo() + " File " + fileName
        + " Loaded Successfully");

  }

  protected void processCommand(String script) {
    runScriptNow(script);
  }

  /////////// simple sync functionality //////////

  private void runScriptNow(String params) {
    JSViewer.runScriptNow(this, getSelectedPanel(), params);
  }

  private void setSaved(boolean isOverlay) {
    if (isOverlay) {
      if (specNodesSaved == null) {
        specNodesSaved = specNodes;
      }
    } else {
      specNodesSaved = null;
    }
  }
  
  private void resetSaved() {
    if (specNodesSaved != null) {
      specNodes = specNodesSaved;
    }
  }
  

  /**
   *  call when a peak is clicked.
   * 
   * @param peak
   */
  public void sendScript(String peak) {
    if (syncCallbackFunctionName == null)
      return;
    peak = Escape.jmolSelect(peak);
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
    if (!getSelectedPanel().getPanelData().getPickedCoordinates(coord, actualCoord))
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

  public void setSelectedPanel(JSVPanel jsvp) {
    System.out.println("Applet setting selected to " + jsvp.getSpectrum());
    if (jsvp != selectedPanel) {
      if (selectedPanel != null) {
        appletPanel.remove((AwtPanel)selectedPanel);
        jsvApplet.removeKeyListener((AwtPanel)selectedPanel);
      }
      appletPanel.add((AwtPanel)jsvp, BorderLayout.CENTER);
      jsvApplet.addKeyListener((AwtPanel)jsvp);
      selectedPanel = jsvp;
    }
    for (int i = specNodes.size(); --i >= 0; )
      if (specNodes.get(i) == jsvp)
        currentSpectrumIndex = i;
      else 
        specNodes.get(i).jsvp.setEnabled(false);
    jsvp.setEnabled(true);
    System.out.println("Applet panel vis " + appletPanel.isVisible() + " " + jsvp.isVisible());
  }

  ///////////// MISC methods from interfaces /////////////
  
  
  // called by Pro's popup window Advanced...
  void doAdvanced(String filePath) {
    // only for JSVAppletPro
  }

  ////////////////// PanelEventInterface
  
  /**
   * called by notifyPeakPickedListeners in JSVPanel
   */
  public void panelEvent(Object eventObj) {
    if (eventObj instanceof PeakPickEvent) {
      PeakPickEvent e = (PeakPickEvent) eventObj;
      PeakInfo pi = e.getPeakInfo();
      System.out.println("panel event " + pi + " on source " + e.getSource() + " " + ((JSVPanel) e.getSource()).getSpectrum());
      setSelectedPanel((JSVPanel) e.getSource());
      getSelectedPanel().getPanelData().addPeakHighlight(pi);
      sendScript(pi.toString());
      if (!pi.isClearAll())
        getSelectedPanel().getPanelData().selectSpectrum(pi.getFilePath(), pi.getType(), pi.getModel());
      checkCallbacks();
    } else if (eventObj instanceof ZoomEvent) {
    } else if (eventObj instanceof SubSpecChangeEvent) {
    }
  }  
  
  /////////////// ScriptInterface execution from JSViewer.runScriptNow and menus
  
  public JSVPanel getSelectedPanel() {
    return selectedPanel;
  }
  
  public void runScript(String script) {
    //System.out.println(getPropertyAsJSON("type"));
    if (scriptQueue == null)
      processCommand(script);
    else
      scriptQueue.add(script);
  }

  public String execExport(JSVPanel jsvp, String value) {
    if (jsvp != null && isPro())
      writeStatus(Exporter.exportCmd(jsvp, ScriptToken.getTokens(value),
          false));
    return null;
  }

  public JDXSource getCurrentSource() {
    return currentSource;
  }

  public List<JSVSpecNode> getSpecNodes() {
    return specNodes;
  }

  public void execSetIntegrationRatios(String value) {
    // parse the string with a method in JSpecViewUtils
    //System.out.println("Integration Ratio Parameter: " + value);
    integrationRatios = IntegralGraph
        .getIntegrationRatiosFromString(value);
  }

  /**
   * Allows Integration of an HNMR spectrum
   * 
   */
  public void execIntegrate(String value) {
    JSVPanel jsvp = getCurrentPanel();
    JSVPanel jsvpNew = (JSVPanel)PanelData.checkIntegral(jsvp,
        parameters, value);
    if (jsvp == jsvpNew) {
      integrationRatios = null;
      return;
    }
    appletPanel.remove((AwtPanel)jsvp);
    appletPanel.add((AwtPanel)jsvpNew);

    initProperties(jsvpNew, currentSpectrumIndex);
    if (integrationRatios != null)
      jsvpNew.getPanelData().setIntegrationRatios(integrationRatios);
    integrationRatios = null; // first time only
    jsvpNew.repaint();
    addPanelToFrame();
  }

  /**
* Allows Transmittance to Absorbance conversion or vice versa depending on
   * the value of comm.
   * 
   * @param comm
   *        the conversion command
   * @throws Exception
   */

  public void execTAConvert(int comm) throws Exception {
    long time = System.currentTimeMillis();
    if (msTrigger > 0 && time - msTrigger < 100)
      return;
    msTrigger = time;
    JSVPanel jsvp = (JSVPanel) PanelData.taConvert(getCurrentPanel(), comm);
    if (jsvp == null)
      return;
    initProperties(jsvp, currentSpectrumIndex);
    jsvp.repaint();

    //  now need to validate and repaint
    jsvApplet.validate();
    jsvApplet.repaint();

  }

  public void execSetCallback(ScriptToken st, String value) {
    switch (st) {
    case PEAKCALLBACKFUNCTIONNAME:
      peakCallbackFunctionName = value;
      break;
    case SYNCCALLBACKFUNCTIONNAME:
      syncCallbackFunctionName = value;
      break;
    case COORDCALLBACKFUNCTIONNAME:
      coordCallbackFunctionName = value;
      break;
    }
  }

  /**
   * Returns the calculated colour of a visible spectrum (Transmittance)
   * 
   * @return Color
   */

  public String getSolnColour() {
    return getSelectedPanel().getPanelData().getSolutionColor();
  }

  /**
   * Calculates the predicted colour of the Spectrum
   */
  public String setSolutionColor(boolean showMessage) {
    if (showMessage) {
      String msg = getSelectedPanel().getPanelData().getSolutionColorHtml();
      JOptionPane.showMessageDialog(jsvApplet, msg, "Predicted Colour",
          JOptionPane.INFORMATION_MESSAGE);
    }
    return getSelectedPanel().getPanelData().getSolutionColor(); 
  }

  public Parameters getParameters() {
    return parameters;
  }

  public void execClose(String value) {
    setSaved(false);
  }

  public String execLoad(String value) {
    // no APPEND here
    setSaved(false);
    openDataOrFile(null, null, null, TextFormat.trimQuotes(value));
    setSpectrumIndex(0);
    return null; 
  }

  public void execHidden(boolean b) {
    // ignored
  }

  public void execSetInterface(String value) {
    // ignored
  }

  public void execScriptComplete(String msg, boolean isOK) {
    jsvApplet.repaint();
  }

  public JSVPanel execSetSpectrum(String value) {
     return setSpectrumIndex(Parser.parseInt(value) - 1);
  }

  public void execSetAutoIntegrate(boolean b) {
    autoIntegrate = b;
  }

  public void execTest(String value) {
    // TODO Auto-generated method stub
    
  }

  public PanelData getPanelData() {
    return getSelectedPanel().getPanelData();  
  }

  public JSVDialog getOverlayLegend(JSVPanel jsvp) {
    return  new OverlayLegendDialog(null, getSelectedPanel());
  }

  public JSVPanel setSpectrumIndex(int i) {
    System.out.println("jsvappletprivate setting spectrum to " + i);
    if (i < 0 || i > specNodes.size())
      return null;
    if (getSelectedPanel() != null) {
      if (theInterface.equals("single")) {
        showSpectrum(i);
        appletPanel.validate();
      } else {
        tabbedDisplayPane.setSelectedIndex(i);
      }
      jsvApplet.repaint();
    }
    return getSelectedPanel();
  }

  public void execOverlay(String value) {
    setSaved(true);
    List<JDXSpectrum> list = new ArrayList<JDXSpectrum>();
    String s = JSVSpecNode.fillSpecList(specNodesSaved, value, list, null, "");
    if (list.size() > 1) {
      openDataOrFile(null, s, list, null);
      setSpectrumIndex(0);
    }
  }

  /**
   * @param msg
   */
  public void syncToJmol(String msg) {
    System.out.println("JSV>Jmol " + msg);
    callToJavaScript(syncCallbackFunctionName, new Object[] { fullName, msg });    
  }

  public void setVisible(boolean b) {
    System.out.println("jsvappletpriv setVisible " + b);
    appletPanel.setVisible(b);    
  }

  public void setFrame(JSVSpecNode node) {
    setSpectrumIndex(JSVSpecNode.getNodeIndex(specNodes, node));
  }

}
