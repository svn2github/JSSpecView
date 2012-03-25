/* Copyright (C) 2002-2012  The JSpecView Development Team
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
// 23-07-2011 jak - altered code to support drawing scales and units separately
// 21-02-2012 rmh - lots of additions  -  integrated into Jmol

package jspecview.application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import jspecview.common.JSVPanel;
import jspecview.common.JSVSpecNode;
import jspecview.common.PanelData;
import jspecview.common.JSVPanelPopupMenu;
import jspecview.common.ScriptToken;
import jspecview.common.JDXSpectrum;
import jspecview.source.JDXSource;
import jspecview.util.FileManager;

/**
 * The Main Class or Entry point of the JSpecView Application.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class AppMenu extends JMenuBar {

  private static final long serialVersionUID = 1L;
  private MainFrame mainFrame;
  private JSVPanelPopupMenu jsvpPopupMenu;

  public AppMenu(MainFrame si, JSVPanelPopupMenu popupMenu) throws Exception {
    this.mainFrame = si;
    jsvpPopupMenu = popupMenu;
    jbInit();
  }

  private JMenu fileMenu = new JMenu();
  private JMenuItem openMenuItem = new JMenuItem();
  private JMenuItem openURLMenuItem = new JMenuItem();
  private JMenuItem printMenuItem = new JMenuItem();
  private JMenuItem closeMenuItem = new JMenuItem();
  private JMenuItem closeAllMenuItem = new JMenuItem();
  private JMenu saveAsMenu = new JMenu();
  private JMenu saveAsJDXMenu = new JMenu();
  private JMenu exportAsMenu = new JMenu();
  private JMenuItem exitMenuItem = new JMenuItem();
  JMenu windowMenu = new JMenu();
  private JMenu helpMenu = new JMenu();
  private JMenu optionsMenu = new JMenu();
  private JMenu displayMenu = new JMenu();
  private JMenu zoomMenu = new JMenu();
  private JCheckBoxMenuItem gridCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem coordsCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem revPlotCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem scaleXCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem scaleYCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JMenuItem nextZoomMenuItem = new JMenuItem();
  private JMenuItem prevZoomMenuItem = new JMenuItem();
  private JMenuItem fullZoomMenuItem = new JMenuItem();
  private JMenuItem clearZoomMenuItem = new JMenuItem();
  private JMenuItem userZoomMenuItem = new JMenuItem();
  private JMenuItem preferencesMenuItem = new JMenuItem();
  private JMenuItem contentsMenuItem = new JMenuItem();
  private JMenuItem aboutMenuItem = new JMenuItem();
  private JMenu openRecentMenu = new JMenu();
  private JCheckBoxMenuItem toolbarCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem sidePanelCheckBoxMenuItem = new JCheckBoxMenuItem();
  private JCheckBoxMenuItem statusCheckBoxMenuItem = new JCheckBoxMenuItem();

  private JMenuItem splitMenuItem = new JMenuItem();
  private JMenuItem overlayAllMenuItem = new JMenuItem();
  private JMenuItem overlayMenuItem = new JMenuItem();
  private JMenuItem sourceMenuItem = new JMenuItem();
  private JMenuItem propertiesMenuItem = new JMenuItem();

  private JMenuItem scriptMenuItem = new JMenuItem();
  JMenu processingMenu = new JMenu();
  private JMenuItem errorLogMenuItem = new JMenuItem();
  JMenuItem overlayKeyMenuItem;

  /**
   * Initializes GUI components
   * 
   * @throws Exception
   */
  private void jbInit() throws Exception {
    fileMenu.setMnemonic('F');
    fileMenu.setText("File");

    JSVPanelPopupMenu.setMenuItem(openMenuItem, 'O', "Open...", 79,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.showFileOpenDialog();
          }
        });
    JSVPanelPopupMenu.setMenuItem(openURLMenuItem, 'U', "Open URL...", 85,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.openURL();
          }
        });
    JSVPanelPopupMenu.setMenuItem(printMenuItem, 'P', "Print...", 80,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.print();
          }
        });
    JSVPanelPopupMenu.setMenuItem(closeMenuItem, 'C', "Close", 115,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.runScript("CLOSE");
          }
        });
    JSVPanelPopupMenu.setMenuItem(closeAllMenuItem, 'L', "Close All", 0,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.runScript("CLOSE ALL");
          }
        });
    JSVPanelPopupMenu.setMenuItem(exitMenuItem, 'X', "Exit", 115,
        InputEvent.ALT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.exitJSpecView(false);
          }
        });

    helpMenu.setMnemonic('H');
    helpMenu.setText("Help");

    optionsMenu.setMnemonic('O');
    optionsMenu.setText("Options");

    displayMenu.setMnemonic('D');
    displayMenu.setText("Display");
    displayMenu.addMenuListener(new MenuListener() {
      public void menuSelected(MenuEvent e) {
        JSVPanel jsvp = mainFrame.getSelectedPanel();
        if (jsvp == null)
          return;
        gridCheckBoxMenuItem.setSelected(jsvp.getPanelData().isGridOn());
        coordsCheckBoxMenuItem.setSelected(jsvp.getPanelData()
            .isCoordinatesOn());
        revPlotCheckBoxMenuItem.setSelected(jsvp.getPanelData()
            .isPlotReversed());
        jsvpPopupMenu.setEnables(mainFrame.getSelectedPanel());
      }

      public void menuDeselected(MenuEvent e) {
      }

      public void menuCanceled(MenuEvent e) {
      }
    });
    zoomMenu.setMnemonic('Z');
    zoomMenu.setText("Zoom");

    JSVPanelPopupMenu.setMenuItem(gridCheckBoxMenuItem, 'G', "Grid", 71,
        InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.GRIDON, e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(coordsCheckBoxMenuItem, 'C', "Coordinates",
        67, InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.COORDINATESON, e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(revPlotCheckBoxMenuItem, 'R', "Reverse Plot",
        82, InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.REVERSEPLOT, e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(scaleXCheckBoxMenuItem, 'X', "X Scale", 88,
        InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.XSCALEON, e);
          }
        });
    JSVPanelPopupMenu.setMenuItem(scaleYCheckBoxMenuItem, 'Y', "Y Scale", 89,
        InputEvent.CTRL_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            setBoolean(ScriptToken.YSCALEON, e);
          };
        });
    JSVPanelPopupMenu.setMenuItem(nextZoomMenuItem, 'N', "Next View", 78,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.zoomTo(1);
          }
        });
    JSVPanelPopupMenu.setMenuItem(prevZoomMenuItem, 'P', "Previous View", 80,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.zoomTo(-1);
          }
        });
    JSVPanelPopupMenu.setMenuItem(fullZoomMenuItem, 'F', "Full View", 70,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.zoomTo(Integer.MAX_VALUE);
          }
        });
    JSVPanelPopupMenu.setMenuItem(clearZoomMenuItem, 'C', "Clear Views", 67,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.zoomTo(0);
          }
        });
    JSVPanelPopupMenu.setMenuItem(userZoomMenuItem, 'Z', "Set Zoom...", 90,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            jsvpPopupMenu.userZoom();
          }
        });
    JSVPanelPopupMenu.setMenuItem(scriptMenuItem, 'T', "Script...", 83,
        InputEvent.ALT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            jsvpPopupMenu.script();
          }
        });
    JSVPanelPopupMenu.setMenuItem(preferencesMenuItem, 'P', "Preferences...",
        0, 0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            showPreferencesDialog();
          }
        });
    JSVPanelPopupMenu.setMenuItem(contentsMenuItem, 'C', "Contents...", 112, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.showNotImplementedOptionPane();
          }
        });
    JSVPanelPopupMenu.setMenuItem(aboutMenuItem, 'A', "About", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            new AboutDialog(mainFrame);
          }
        });
    openRecentMenu.setActionCommand("OpenRecent");
    openRecentMenu.setMnemonic('R');
    openRecentMenu.setText("Open Recent");

    saveAsMenu.setMnemonic('A');
    saveAsJDXMenu.setMnemonic('J');
    exportAsMenu.setMnemonic('E');

    JSVPanelPopupMenu.setMenuItem(toolbarCheckBoxMenuItem, 'T', "Toolbar", 84,
        InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            mainFrame.enableToolbar(e.getStateChange() == ItemEvent.SELECTED);
          }
        });
    toolbarCheckBoxMenuItem.setSelected(true);

    JSVPanelPopupMenu.setMenuItem(sidePanelCheckBoxMenuItem, 'S', "Side Panel",
        83, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            mainFrame.setSplitPane(e.getStateChange() == ItemEvent.SELECTED);
          }
        });
    sidePanelCheckBoxMenuItem.setSelected(true);

    JSVPanelPopupMenu.setMenuItem(statusCheckBoxMenuItem, 'B', "Status Bar",
        66, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK, new ItemListener() {
          public void itemStateChanged(ItemEvent e) {
            mainFrame.enableStatus(e.getStateChange() == ItemEvent.SELECTED);
          }
        });
    statusCheckBoxMenuItem.setSelected(true);
    JSVPanelPopupMenu.setMenuItem(splitMenuItem, 'P', "Split", 83,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.runScript("CLOSE");
          }
        });
    JSVPanelPopupMenu.setMenuItem(overlayAllMenuItem, 'Y', "Overlay All", 0, 0,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.runScript("OVERLAY ALL");
          }
        });
    JSVPanelPopupMenu.setMenuItem(overlayMenuItem, 'O', "Overlay...", 79,
        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (overlayAllMenuItem.isEnabled()) 
              mainFrame.closeSource(mainFrame.getCurrentSource());
            jsvpPopupMenu.overlay(-1);
          }
        });
    //    JSVPanelPopupMenu.setMenuItem(hideMenuItem, 'H', "Hide", 0, 0,
    //        new ActionListener() {
    //          public void actionPerformed(ActionEvent e) {
    //            hideMenuItem_actionPerformed(e);
    //          }
    //        });
    //    JSVPanelPopupMenu.setMenuItem(hideAllMenuItem, 'L', "Hide All", 0, 0,
    //        new ActionListener() {
    //          public void actionPerformed(ActionEvent e) {
    //            hideAllMenuItem_actionPerformed(e);
    //          }
    //        });
    //    JSVPanelPopupMenu.setMenuItem(showMenuItem, 'S', "Show All", 0, 0,
    //        new ActionListener() {
    //          public void actionPerformed(ActionEvent e) {
    //            showMenuItem_actionPerformed(e);
    //          }
    //        });
    JSVPanelPopupMenu.setMenuItem(sourceMenuItem, 'S', "Source ...", 83,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            TextDialog.showSource(mainFrame);
          }
        });
    JSVPanelPopupMenu.setMenuItem(propertiesMenuItem, 'P', "Properties", 72,
        InputEvent.CTRL_MASK, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.showProperties();
          }
        });
    overlayKeyMenuItem = jsvpPopupMenu.overlayKeyMenuItem;
    JSVPanelPopupMenu.setMenuItem(overlayKeyMenuItem, '\0', "Overlay Key", 0,
        0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.toggleOverlayKey();
          }
        });

    JSVPanelPopupMenu.setMenuItem(errorLogMenuItem, '\0', "Error Log ...", 0,
        0, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            TextDialog.showError(mainFrame);
          }
        });

    processingMenu.setMnemonic('P');
    processingMenu.setText("Processing");
    processingMenu.addMenuListener(new MenuListener() {
      public void menuSelected(MenuEvent e) {
        jsvpPopupMenu.setEnables(mainFrame.getSelectedPanel());
      }

      public void menuDeselected(MenuEvent e) {
      }

      public void menuCanceled(MenuEvent e) {
      }
    });
    jsvpPopupMenu.setProcessingMenu(processingMenu);

    add(fileMenu);
    add(displayMenu).setEnabled(false);
    add(optionsMenu);
    add(processingMenu).setEnabled(false);
    add(windowMenu);
    add(helpMenu);
    fileMenu.add(openMenuItem);
    fileMenu.add(openURLMenuItem);
    fileMenu.add(openRecentMenu);
    fileMenu.addSeparator();
    fileMenu.add(closeMenuItem).setEnabled(false);
    fileMenu.add(closeAllMenuItem).setEnabled(false);
    fileMenu.addSeparator();
    fileMenu.add(scriptMenuItem);
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
    displayMenu.add(scaleXCheckBoxMenuItem);
    displayMenu.add(scaleYCheckBoxMenuItem);
    displayMenu.add(revPlotCheckBoxMenuItem);
    displayMenu.addSeparator();
    displayMenu.add(zoomMenu);
    displayMenu.addSeparator();
    displayMenu.add(propertiesMenuItem);
    displayMenu.add(overlayKeyMenuItem).setEnabled(false);
    zoomMenu.add(nextZoomMenuItem);
    zoomMenu.add(prevZoomMenuItem);
    zoomMenu.add(fullZoomMenuItem);
    zoomMenu.add(clearZoomMenuItem);
    zoomMenu.add(userZoomMenuItem);
    optionsMenu.add(preferencesMenuItem);
    optionsMenu.addSeparator();
    optionsMenu.add(toolbarCheckBoxMenuItem);
    optionsMenu.add(sidePanelCheckBoxMenuItem);
    optionsMenu.add(statusCheckBoxMenuItem);
    helpMenu.add(aboutMenuItem);
    JSVPanelPopupMenu.setMenus(saveAsMenu, saveAsJDXMenu, exportAsMenu,
        (new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.exportSpectrum(e.getActionCommand());
          }
        }));
    windowMenu.setMnemonic('W');
    windowMenu.setText("Window");
    windowMenu.add(splitMenuItem);
    windowMenu.add(overlayAllMenuItem);
    windowMenu.add(overlayMenuItem);
    //windowMenu.addSeparator();
    //windowMenu.add(hideMenuItem);
    //windowMenu.add(hideAllMenuItem);
    //windowMenu.add(showMenu);
    //windowMenu.add(showMenuItem);
    //windowMenu.addSeparator();

  }

  protected void setBoolean(ScriptToken st, ItemEvent e) {
    boolean isOn = (e.getStateChange() == ItemEvent.SELECTED);
    mainFrame.runScript(st + " " + isOn);
  }

  public void setSourceEnabled(boolean b) {
    closeAllMenuItem.setEnabled(b);
    displayMenu.setEnabled(b);
    windowMenu.setEnabled(b);
    processingMenu.setEnabled(b);
    printMenuItem.setEnabled(b);
    sourceMenuItem.setEnabled(b);
    errorLogMenuItem.setEnabled(b);
    exportAsMenu.setEnabled(b);
    saveAsMenu.setEnabled(b);
  }

  void setCloseMenuItem(String fileName) {
    closeMenuItem.setEnabled(fileName != null);
    closeMenuItem.setText(fileName == null ? "Close" : "Close " + fileName);
  }

  void setError(boolean isError, boolean isWarningOnly) {
    errorLogMenuItem.setEnabled(isError);
  }

  public void setMenuEnables(JSVSpecNode node) {
    if (node == null) {
      setCloseMenuItem(null);
      setSourceEnabled(false);
    } else {
      setSourceEnabled(true);
      JDXSpectrum spec = node.jsvp.getSpectrum();
      PanelData pd = node.jsvp.getPanelData();
      gridCheckBoxMenuItem.setSelected(pd.isGridOn());
      coordsCheckBoxMenuItem.setSelected(pd.isCoordinatesOn());
      revPlotCheckBoxMenuItem.setSelected(pd.isPlotReversed());
      scaleXCheckBoxMenuItem.setSelected(pd.isXScaleOn());
      scaleYCheckBoxMenuItem.setSelected(pd.isYScaleOn());
      overlayKeyMenuItem.setEnabled(pd.getNumberOfGraphSets() > 1);
      overlayAllMenuItem.setEnabled(!pd.isOverlaid());
      splitMenuItem.setEnabled(pd.isOverlaid());
      setCloseMenuItem(FileManager.getName(node.source.getFilePath()));
      exportAsMenu.setEnabled(true);
      saveAsMenu.setEnabled(true);
      saveAsJDXMenu.setEnabled(spec.canSaveAsJDX());
    }

  }

  public boolean toggleOverlayKeyMenuItem() {
    overlayKeyMenuItem.setSelected(overlayKeyMenuItem.isSelected());
    return overlayKeyMenuItem.isSelected();
  }

  ////////// MENU ACTIONS ///////////

  /**
   * Shows the preferences dialog
   * 
   * @param e
   *        the ActionEvent
   */
  void showPreferencesDialog() {
    mainFrame.showPreferences();
  }

  //  /**
  //   * Hides the selected JInternalFrane
  //   * 
  //   * @param e
  //   *        the ActionEvent
  //   */
  //  void hideMenuItem_actionPerformed(ActionEvent e) {
  //    JSVFrame frame = getCurrentFrame();
  //    try {
  //      if (frame != null) {
  //        frame.setVisible(false);
  //        frame.setSelected(false);
  //        //        spectraTree.validate();
  //        spectraTree.repaint();
  //      }
  //    } catch (PropertyVetoException pve) {
  //    }
  //    //doInternalFrameClosing(frame);
  //  }
  //
  //  /**
  //   * Hides all JInternalFranes
  //   * 
  //   * @param e
  //   *        the ActionEvent
  //   */
  //  void hideAllMenuItem_actionPerformed(ActionEvent e) {
  //    JInternalFrame[] frames = desktopPane.getAllFrames();
  //    try {
  //      for (int i = 0; i < frames.length; i++) {
  //        if (frames[i].isVisible()) {
  //          frames[i].setVisible(false);
  //          frames[i].setSelected(false);
  //          //         doInternalFrameClosing(frames[i]);
  //        }
  //      }
  //    } catch (PropertyVetoException pve) {
  //    }
  //  }
  //
  //  /**
  //   * Shows all JSVFrames
  //   * 
  //   * @param e
  //   *        the ActionEvent
  //   */
  //  protected void showMenuItem_actionPerformed(ActionEvent e) {
  //    JInternalFrame[] frames = desktopPane.getAllFrames();
  //    try {
  //      for (int i = 0; i < frames.length; i++) {
  //        frames[i].setVisible(true);
  //      }
  //      frames[0].setSelected(true);
  //    } catch (PropertyVetoException pve) {
  //    }
  //
  //    //showMenu.removeAll();
  //  }


  //  /**
  //   * Displays the spectrum of the current <code>JDXSource</code> in separate
  //   * windows
  //   * 
  //   * @param e
  //   *        the ActionEvent
  //   */
  //  protected void splitMenuItem_actionPerformed(ActionEvent e) {
  //    JDXSource source = currentSource;
  //    JSVPanel jsvp = mainFrame.getSelectedPanel();
  //    if (!source.isCompoundSource || jsvp == null
  //        || jsvp.getPanelData().getNumberOfGraphSets() == 1) {
  //      splitMenuItem.setSelected(false);
  //      return;
  //      // STATUS --> Can't Split
  //    }
  //    closeSource(source);
  //    if (!source.isOverlay())
  //      splitSpectra(source);
  //  }

  public void setSelections(boolean sidePanelOn, boolean toolbarOn,
                            boolean statusbarOn, JSVPanel jsvp) {
    // hide side panel if sidePanelOn property is false
    sidePanelCheckBoxMenuItem.setSelected(sidePanelOn);
    toolbarCheckBoxMenuItem.setSelected(toolbarOn);
    statusCheckBoxMenuItem.setSelected(statusbarOn);
    if (jsvp != null) {
      PanelData pd = jsvp.getPanelData();
      gridCheckBoxMenuItem.setSelected(pd.isGridOn());
      coordsCheckBoxMenuItem.setSelected(pd.isCoordinatesOn());
      revPlotCheckBoxMenuItem.setSelected(pd.isPlotReversed());
      scaleXCheckBoxMenuItem.setSelected(pd.isXScaleOn());
      scaleYCheckBoxMenuItem.setSelected(pd.isYScaleOn());
    }
  }

  public void setRecentMenu(List<String> recentFilePaths) {
    openRecentMenu.removeAll();
    for (int i = 0; i < recentFilePaths.size(); i++) {
      String path = recentFilePaths.get(i);
      JMenuItem menuItem;
      menuItem = new JMenuItem(path);
      openRecentMenu.add(menuItem);
      menuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          mainFrame.openFile(((JMenuItem) e.getSource()).getText(), true);
        }
      });
    }
  }

  public void updateRecentMenus(List<String> recentFilePaths) {
    JMenuItem menuItem;
    openRecentMenu.removeAll();
    for (int i = 0; i < recentFilePaths.size(); i++) {
      String path = recentFilePaths.get(i);
      menuItem = new JMenuItem(path);
      openRecentMenu.add(menuItem);
      menuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          mainFrame.openFile(((JMenuItem) e.getSource()).getText(), true);
        }
      });
    }
  }

  public void clearSourceMenu(JDXSource source) {
    if (source == null) {
      setMenuEnables(null);
    } else {
      //      List<JDXSpectrum> spectra = source.getSpectra();
      //      for (int i = 0; i < spectra.size(); i++) {
      //        String title = spectra.get(i).getTitleLabel();
      //        for (int j = 0; j < showMenu.getMenuComponentCount(); j++) {
      //          JMenuItem mi = (JMenuItem) showMenu.getMenuComponent(j);
      //          if (mi.getText().endsWith(title)) {
      //            showMenu.remove(mi);
      //          }
      //        }
      //      }
      saveAsJDXMenu.setEnabled(true);
      saveAsMenu.setEnabled(true);
    }
    setCloseMenuItem(null);

  }


}
