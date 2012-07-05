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

import java.awt.Container;
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
import jspecview.common.JDXSpectrum;
import jspecview.export.Exporter;
import jspecview.util.Logger;
import jspecview.util.Parser;

/**
 * Popup Menu for JSVPanel.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.common.JSVPanel
 */
public class JSVPopupMenu extends JPopupMenu {

  protected boolean isApplet;

  public enum EnumOverlay { DIALOG, OFFSETY };
  

  private static final long serialVersionUID = 1L;

  private ScriptInterface scripter;
  
  public void dispose() {
    pd = null;
    //scripter = null;
  }

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
   * @see jspecview.common.JSVPanel#resetView()
   */
  public JMenuItem clearMenuItem = new JMenuItem();
  /**
   * Allows for the JSVPanel to be reset to it's original display
   * @see jspecview.common.JSVPanel#clearViews()
   */
  public JMenuItem resetMenuItem = new JMenuItem();
  /**
   * Allows for the viewing of the properties of the Spectrum that is
   * displayed on the <code>JSVPanel</code>
   */
  public JMenuItem properties = new JMenuItem();

  protected JMenuItem userZoomMenuItem = new JMenuItem();
  protected JMenuItem scriptMenuItem = new JMenuItem();
  public JMenuItem overlayStackOffsetMenuItem = new JMenuItem();

  public JMenuItem integrateMenuItem = new JCheckBoxMenuItem();
  public JMenuItem integrateHideMenuItem = new JCheckBoxMenuItem();
  public JMenuItem transAbsMenuItem = new JMenuItem();
  public JMenuItem solColMenuItem = new JMenuItem();
  
  public JCheckBoxMenuItem gridCheckBoxMenuItem = new JCheckBoxMenuItem();
  public JCheckBoxMenuItem coordsCheckBoxMenuItem = new JCheckBoxMenuItem();
  public JCheckBoxMenuItem reversePlotCheckBoxMenuItem = new JCheckBoxMenuItem();


// applet only:
  
  protected JMenu appletSaveAsJDXMenu; // applet only
  protected JMenu appletExportAsMenu;  // applet only
  protected JMenuItem appletAdvancedMenuItem;
  protected JMenuItem spectraMenuItem = new JMenuItem();
  public JMenuItem overlayKeyMenuItem = new JMenuItem();
  
  public JSVPopupMenu(ScriptInterface scripter) {
    super();
    this.scripter = scripter;
    jbInit();
  }

  /**
   * Initialises GUI components
   * @throws Exception
   */
  protected void jbInit() {
    final ScriptInterface scripter = this.scripter;
    nextMenuItem.setText("Next View");
    nextMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scripter.getPanelData().nextView();
        reboot();
      }
    });
    previousMenuItem.setText("Previous View");
    previousMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scripter.getPanelData().previousView();
        reboot();
      }
    });
    clearMenuItem.setText("Clear Views");
    clearMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scripter.getPanelData().clearViews();
      }
    });
    resetMenuItem.setText("Reset View");
    resetMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scripter.getPanelData().resetView();
      }
    });
    
    setOverlayItems();
    
    scriptMenuItem.setText("Script...");
    scriptMenuItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         script();
       }
     });
    userZoomMenuItem.setText("Set Zoom...");
    userZoomMenuItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         userZoom();
       }
     });
    properties.setActionCommand("Properties");
    properties.setText("Properties");
    properties.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        scripter.showProperties();
      }
    });
    gridCheckBoxMenuItem.setText("Show Grid");
    gridCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        runScript(scripter, "GRIDON " + (e.getStateChange() == ItemEvent.SELECTED));
        reboot();
      }
    });
    coordsCheckBoxMenuItem.setText("Show Coordinates");
    coordsCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        runScript(scripter, "COORDINATESON " + (e.getStateChange() == ItemEvent.SELECTED));
        reboot();
      }
    });
    reversePlotCheckBoxMenuItem.setText("Reverse Plot");
    reversePlotCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        runScript(scripter, "REVERSEPLOT " + (e.getStateChange() == ItemEvent.SELECTED));
        reboot();
      }
    });
    
    setPopupMenu();
  }

  protected void setOverlayItems() {
    spectraMenuItem.setText("Spectra...");
    spectraMenuItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         overlay(EnumOverlay.DIALOG);
       }
     });
    overlayStackOffsetMenuItem.setText("Overlay Offset...");
    overlayStackOffsetMenuItem.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent e) {
         overlay(EnumOverlay.OFFSETY);
       }
     });
	}

	/**
   * overridded in applet
   */
  protected void setPopupMenu() {
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
    add(spectraMenuItem);
    add(overlayStackOffsetMenuItem);
    add(scriptMenuItem);
    addSeparator();
    add(properties);
  }
  protected void reboot() {
    if (thisJsvp == null)
      return;
    thisJsvp.repaint();
    show((Container) thisJsvp, thisX, thisY);
  }

  private String recentZoom;

  public void userZoom() {
    String zoom = (String) JOptionPane.showInputDialog(null,
        "Enter zoom range", "Zoom", JOptionPane.PLAIN_MESSAGE, null, null,
        (recentZoom == null ? "" : recentZoom));
    if (zoom == null)
      return;
    recentZoom = zoom;
    runScript(scripter, "zoom " + zoom);
  }

  private String recentScript;

  public void script() {
    String script = (String) JOptionPane.showInputDialog(null,
        "Enter a JSpecView script", "Script", JOptionPane.PLAIN_MESSAGE, null,
        null, (recentScript == null ? "" : recentScript));
    if (script == null)
      return;
    recentScript = script;
    runScript(scripter, script);
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

  public void setProcessingMenu(JMenu menu) {
    final ScriptInterface scripter = this.scripter;
    setMenuItem(integrateMenuItem, 'I', "Integrate HNMR", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            runScript(scripter, "INTEGRATE TOGGLE");
          }
        });
    integrateHideMenuItem.setSelected(false);
    setMenuItem(integrateHideMenuItem, 'H', "Hide Integration", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            runScript(scripter, "SHOWINTEGRATION " + !((JMenuItem) e.getSource()).isSelected());
          }
        });
    setMenuItem(transAbsMenuItem, '\0', "Transmittance/Absorbance", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            runScript(scripter, "IRMODE IMPLIED");
          }
        });
    setMenuItem(solColMenuItem, 'C', "Predicted Solution Colour", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            runScript(scripter, "GETSOLUTIONCOLOR");
          }
        });
    menu.add(integrateMenuItem);
    menu.add(integrateHideMenuItem);
    menu.add(transAbsMenuItem);
    menu.add(solColMenuItem);
  }

  protected static void runScript(ScriptInterface scripter, String cmd) {
    if (scripter == null)
      Logger.error("scripter was null for " + cmd);
    else
      scripter.runScript(cmd);
  }

  protected String recentStackPercent = "5";

  private PanelData pd;

	public void overlay(EnumOverlay overlay) {
		switch (overlay) {
		case DIALOG:
			scripter.checkOverlay();
			break;
		case OFFSETY:
			String offset = (String) JOptionPane.showInputDialog(null,
					"Enter a vertical offset in percent for stacked plots",
					"Overlay", JOptionPane.PLAIN_MESSAGE, null, null,
					recentStackPercent);
			if (offset == null || Float.isNaN(Parser.parseFloat(offset)))
				return;
			recentStackPercent = offset;
			runScript(scripter, ScriptToken.STACKOFFSETY + " " + offset);
			break;
		}
	}


  private int thisX, thisY;
  private JSVPanel thisJsvp;
  
  public void show(JSVPanel jsvp, int x, int y) {
    setEnables(jsvp);
    thisX = x;
    thisY = y;
    thisJsvp = jsvp;
    super.show((Container) jsvp, x, y);
 }

  public void setEnables(JSVPanel jsvp) {
    pd = jsvp.getPanelData();
    JDXSpectrum spec0 = jsvp.getSpectrum();
    setSelected(gridCheckBoxMenuItem, pd.getBoolean(ScriptToken.GRIDON));
    setSelected(coordsCheckBoxMenuItem, pd.getBoolean(ScriptToken.COORDINATESON));
    setSelected(reversePlotCheckBoxMenuItem, pd.getBoolean(ScriptToken.REVERSEPLOT));

    boolean isOverlaid = pd.isOverlaid();
    boolean isSingle = pd.haveSelectedSpectrum();
    integrateMenuItem.setEnabled(isSingle && spec0.canIntegrate() || spec0.hasIntegral());
    integrateMenuItem.setSelected(pd.getSpectrum().hasIntegral());
    integrateHideMenuItem.setEnabled(integrateMenuItem.isEnabled() && integrateMenuItem.isSelected());
    integrateHideMenuItem.setSelected(!pd.getShowIntegration());
    solColMenuItem.setEnabled(isSingle && spec0.canShowSolutionColor());
    transAbsMenuItem.setEnabled(isSingle && spec0.canConvertTransAbs());
    overlayKeyMenuItem.setEnabled(isOverlaid && pd.getNumberOfGraphSets() == 1);
    // what about its selection???
    if (appletSaveAsJDXMenu != null)
      appletSaveAsJDXMenu.setEnabled(spec0.canSaveAsJDX());
    if (appletExportAsMenu != null)
      appletExportAsMenu.setEnabled(true);
    if (appletAdvancedMenuItem != null)
      appletAdvancedMenuItem.setEnabled(!isOverlaid);
//    if (appletCompoundMenu != null) 
  //    appletCompoundMenu.setEnabled(
    //      appletCompoundMenu.isEnabled() && appletCompoundMenu.getItemCount() > 3);
  }

  private void setSelected(JCheckBoxMenuItem item, boolean TF) {
    item.setEnabled(false);
    item.setSelected(TF);
    item.setEnabled(true);
  }

  static void addMenuItem(JMenu m, String key,
                                  ActionListener actionListener) {
    JMenuItem jmi = new JMenuItem();
    jmi.setMnemonic(key.charAt(0));
    jmi.setText(key);
    jmi.addActionListener(actionListener);
    m.add(jmi);
  }

  public static void setMenus(JMenu saveAsMenu, JMenu saveAsJDXMenu,
                              JMenu exportAsMenu, ActionListener actionListener) {
    saveAsMenu.setText("Save As");
    JSVPopupMenu.addMenuItem(saveAsMenu, Exporter.sourceLabel, actionListener);
    saveAsJDXMenu.setText("JDX");
    JSVPopupMenu.addMenuItem(saveAsJDXMenu, "XY", actionListener);
    JSVPopupMenu.addMenuItem(saveAsJDXMenu, "DIF", actionListener);
    JSVPopupMenu.addMenuItem(saveAsJDXMenu, "DIFDUP", actionListener);
    JSVPopupMenu.addMenuItem(saveAsJDXMenu, "FIX", actionListener);
    JSVPopupMenu.addMenuItem(saveAsJDXMenu, "PAC", actionListener);
    JSVPopupMenu.addMenuItem(saveAsJDXMenu, "SQZ", actionListener);
    saveAsMenu.add(saveAsJDXMenu);
    JSVPopupMenu.addMenuItem(saveAsMenu, "CML", actionListener);
    JSVPopupMenu.addMenuItem(saveAsMenu, "XML (AnIML)", actionListener);
    if (exportAsMenu != null) {
      exportAsMenu.setText("Export As");
      JSVPopupMenu.addMenuItem(exportAsMenu, "JPG", actionListener);
      JSVPopupMenu.addMenuItem(exportAsMenu, "PNG", actionListener);
      JSVPopupMenu.addMenuItem(exportAsMenu, "SVG", actionListener);
    }
  }

}