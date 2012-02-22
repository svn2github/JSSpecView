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
import java.awt.event.ItemEvent;

import javax.swing.JCheckBoxMenuItem;
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

  public JCheckBoxMenuItem integrateMenuItem = new JCheckBoxMenuItem();

  public JSVPanelPopupMenu() {
    super();
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

  
  protected void setMenu() {
    add(gridCheckBoxMenuItem);
    add(coordsCheckBoxMenuItem);
    add(reversePlotCheckBoxMenuItem);
    addSeparator();
    add(nextMenuItem);
    add(previousMenuItem);
    add(clearMenuItem);
    add(resetMenuItem);
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
    if (integrateMenuItem.isSelected() == true)
      integrateMenuItem.setSelected(false);
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
    selectedJSVPanel.setGridOn((e.getStateChange() == ItemEvent.SELECTED));
    selectedJSVPanel.repaint();
  }

  /**
   * Toggles the coordinates on or off
   * @param e the <code>ItemEvent</code
   */
  void coordsCheckBoxMenuItem_itemStateChanged(ItemEvent e) {
    selectedJSVPanel.setCoordinatesOn((e.getStateChange() == ItemEvent.SELECTED));
    selectedJSVPanel.repaint();
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

    JDXSpectrum spectrum = selectedJSVPanel.getSpectrumAt(0);
    Object[][] rowData = (source.isCompoundSource ? source
        .getHeaderRowDataAsArray(false, 0) : spectrum
        .getHeaderRowDataAsArray());
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



}
