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

package jspecview.common;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import jspecview.common.JDXSpectrum;
import jspecview.source.JDXSource;

/**
 * Popup Menu for JSVPanel.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.common.JSVPanel
 */
public class JSVPanelPopupMenu extends JPopupMenu {

  protected boolean isApplet;
  
  private static final long serialVersionUID = 1L;

  /**
   * Menu Item that allows user to navigate to the next view of a JSVPanel
   * that has been zoomed
   * @see jspecview.common.JSVPanel#nextView()
   */
  public JMenuItem nextMenuItem = new JMenuItem();
  /**
   * Menu Item for navigating to previous view
   * @see jspecview.common.JSVPanel#previousView()
   */
  public JMenuItem previousMenuItem = new JMenuItem();
  /**
   * Allows for all view to be cleared
   * @see jspecview.common.JSVPanel#clearViews()
   */
  public JMenuItem clearMenuItem = new JMenuItem();
  /**
   * Allows for the JSVPanel to be reset to it's original display
   * @see jspecview.common.JSVPanel#reset()
   */
  public JMenuItem resetMenuItem = new JMenuItem();
  /**
   * Allows for the viewing of the properties of the Spectrum that is
   * displayed on the <code>JSVPanel</code>
   */
  public JMenuItem properties = new JMenuItem();

  protected JMenuItem userZoomMenuItem = new JMenuItem();
  protected JMenuItem scriptMenuItem = new JMenuItem();
  public JMenuItem overlayMenuItem = new JMenuItem();

  public JCheckBoxMenuItem integrateCheckBoxMenuItem = new JCheckBoxMenuItem();
  public JCheckBoxMenuItem transAbsMenuItem = new JCheckBoxMenuItem();
  public JMenuItem solColMenuItem = new JMenuItem();
  
  private ScriptInterface scripter;
  
  public JSVPanelPopupMenu(ScriptInterface scripter) {
    super();
    this.scripter = scripter;
    jbInit();
  }

  /**
   * Initialises GUI components
   * @throws Exception
   */
  protected void jbInit() {
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
    clearMenuItem.setText("Clear Views");
    clearMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearMenuItem_actionPerformed(e);
      }
    });
    resetMenuItem.setText("Reset View");
    resetMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        resetMenuItem_actionPerformed(e);
      }
    });
    overlayMenuItem.setText("Overlay...");
    overlayMenuItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         overlayMenuItem_actionPerformed(e);
       }
     });
    scriptMenuItem.setText("Script...");
    scriptMenuItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         scriptMenuItem_actionPerformed(e);
       }
     });
    userZoomMenuItem.setText("Set Zoom...");
    userZoomMenuItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         userMenuItem_actionPerformed(e);
       }
     });
    properties.setActionCommand("Properties");
    properties.setText("Properties");
    properties.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        properties_actionPerformed(e);
      }
    });
    gridCheckBoxMenuItem.setText("Show Grid");
    gridCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        gridCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    coordsCheckBoxMenuItem.setText("Show Coordinates");
    coordsCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        coordsCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    reversePlotCheckBoxMenuItem.setText("Reverse Plot");
    reversePlotCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        revPlotCheckBoxMenuItem_itemStateChanged(e);
      }
    });
    
    setMenu();
  }

  
  protected void scriptMenuItem_actionPerformed(ActionEvent e) {
    script();
  }
  
  protected void overlayMenuItem_actionPerformed(ActionEvent e) {
    overlay();
  }
  
  protected void userMenuItem_actionPerformed(ActionEvent e) {
    userZoom();
  }
  
  protected void setMenu() {
    add(gridCheckBoxMenuItem);
    add(coordsCheckBoxMenuItem);
    add(reversePlotCheckBoxMenuItem);
    addSeparator();
    add(nextMenuItem);
    add(previousMenuItem);
    add(clearMenuItem);
    add(resetMenuItem);
    add(userZoomMenuItem);
    addSeparator();
    add(overlayMenuItem);
    add(scriptMenuItem);
    addSeparator();
    add(properties);
  }

  /**
   * Action for nextMenuItem. Shows the next view of the JSVPanel
   * that has been zoomed
   * @param e the <code>ActionEvent</code>
   */
  void nextMenuItem_actionPerformed(ActionEvent e) {
    selectedJSVPanel.nextView();
  }

  /**
   * Action for the previousMenuItem. Shows the previous view of the JSVPanel
   * that has been zoomed
   * @param e the <code>ActionEvent</code>
   */
  void previousMenuItem_actionPerformed(ActionEvent e) {
    selectedJSVPanel.previousView();
  }

  /**
   * Action for the resetMenuItem. Resets the JSVpanel to it's original view
   * @param e the <code>ActionEvent</code>
   */
  void resetMenuItem_actionPerformed(ActionEvent e) {
    if (integrateCheckBoxMenuItem.isSelected() == true)
      integrateCheckBoxMenuItem.setSelected(false);
    selectedJSVPanel.reset();
  }

  /**
   * Action for clearMenuItem. Clears the a the views of the JSVPanel.
   * @param e the <code>ActionEvent</code>
   */
  void clearMenuItem_actionPerformed(ActionEvent e) {
    selectedJSVPanel.clearViews();
  }

  /**
   * Toogles the Grid on or off
   * @param e the <code>ItemEvent</code
   */
  void gridCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    scripter.runScript("GRIDON " + (e.getStateChange() == ItemEvent.SELECTED));
  }

  /**
   * Toggles the coordinates on or off
   * @param e the <code>ItemEvent</code
   */
  void coordsCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    scripter.runScript("COORDINATESON " + (e.getStateChange() == ItemEvent.SELECTED));
  }

  /**
   * Reverses the spectrum plot
   * @param e the <code>ItemEvent</code
   */
  void revPlotCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    selectedJSVPanel.setReversePlot((e.getStateChange() == ItemEvent.SELECTED));
    selectedJSVPanel.repaint();
  }

  /**
   * Shows the properties of the Spectrum displayed on the JSVPanel
   * 
   * @param e
   *        the <code>ActionEvent</code
   */
  public void properties_actionPerformed(ActionEvent e) {

    JDXSpectrum spectrum = selectedJSVPanel.getSpectrum();
    Object[][] rowData = spectrum.getHeaderRowDataAsArray();
    //if (source.isCompoundSource)
      //rowData = source.getHeaderRowDataAsArray(false, rowData);
    String[] columnNames = { "Label", "Description" };
    JTable table = new JTable(rowData, columnNames);
    table.setPreferredScrollableViewportSize(new Dimension(400, 195));
    JScrollPane scrollPane = new JScrollPane(table);
    JOptionPane.showMessageDialog(this, scrollPane, "Header Information",
        JOptionPane.PLAIN_MESSAGE);
  }

  protected JSVPanel selectedJSVPanel;
  protected JDXSource source;

  /**
   * Allows the grid to be toogled
   */
  public JCheckBoxMenuItem gridCheckBoxMenuItem = new JCheckBoxMenuItem();
  /**
   * Allows the coordinates to be toggled on or off
   */
  public JCheckBoxMenuItem coordsCheckBoxMenuItem = new JCheckBoxMenuItem();
  /**
   * Allows the plot to be reversed
   */
  public JCheckBoxMenuItem reversePlotCheckBoxMenuItem = new JCheckBoxMenuItem();


  /**
   * Sets the parent <code>JSVPanel</code> of the popupmenu
   * @param jsvp the <code>JSVPanel</code>
   */
  public void setSelectedJSVPanel(JSVPanel jsvp){
    selectedJSVPanel =  jsvp;
  }

  /**
   * Sets the source of the Spectrum of the JSVPanel
   * @param source the JDXSource
   */
  public void setSource(JDXSource source){
    this.source = source;
  }

  private String recentZoom;

  public void userZoom() {
    String zoom = (String) JOptionPane.showInputDialog(null,
        "Enter zoom range", "Zoom", JOptionPane.PLAIN_MESSAGE, null, null,
        (recentZoom == null ? "" : recentZoom));
    if (zoom == null)
      return;
    recentZoom = zoom;
    scripter.runScript("zoom " + zoom);
  }

  private String recentScript;

  public void script() {
    String script = (String) JOptionPane.showInputDialog(null,
        "Enter a JSpecView script", "Script", JOptionPane.PLAIN_MESSAGE, null,
        null, (recentScript == null ? "" : recentScript));
    if (script == null)
      return;
    recentScript = script;
    scripter.runScript(script);
  }

  public static void setMenuItem(JMenuItem item, char c, String text,
                           int accel, int mask, EventListener el) {
    if (c != '\0')
      item.setMnemonic(c);
    item.setText(text);
    if (accel > 0)
      item.setAccelerator(javax.swing.KeyStroke.getKeyStroke(accel,
          mask, false));
    if (el instanceof ActionListener)
      item.addActionListener((ActionListener) el);
    else if (el instanceof ItemListener)
      item.addItemListener((ItemListener) el);
  }

  public void setProcessingMenu(JMenu processingMenu) {
    processingMenu.add(integrateCheckBoxMenuItem).setEnabled(false);
    processingMenu.add(transAbsMenuItem).setEnabled(false);
    processingMenu.add(solColMenuItem).setEnabled(false);
        setMenuItem(
        integrateCheckBoxMenuItem, 'I', "Integrate HNMR", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            scripter.runScript("INTEGRATE ?");
          }
        });
        setMenuItem(
        transAbsMenuItem, '\0', "Transmittance/Absorbance", 0, 0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            scripter.runScript("IRMODE IMPLIED");
          }
        });
        setMenuItem(
        solColMenuItem, '\0', "Predicted Solution Colour", 0, 0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            scripter.runScript("GETSOLUTIONCOLOR");
          }
        });
  }

  protected String recentOverlay = "1.1,1.2";

  public void overlay() {
    String script = (String) JOptionPane.showInputDialog(null,
        "Enter a list of Spectrum IDs separated by commas", "Overlay", JOptionPane.PLAIN_MESSAGE, null,
        null, recentOverlay);
    if (script == null)
      return;
    recentOverlay = script;
    scripter.runScript("overlay " + script);
  }

}
