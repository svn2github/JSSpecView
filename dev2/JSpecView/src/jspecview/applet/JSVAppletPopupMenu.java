package jspecview.applet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import jspecview.common.AppUtils;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVPanelPopupMenu;
import jspecview.source.JDXSource;

class JSVAppletPopupMenu extends JSVPanelPopupMenu {

  private JSVApplet applet;

  JSVAppletPopupMenu(JSVApplet applet, 
      boolean allowMenu, boolean enableZoom) {
    super(applet);
    isApplet = true;
    this.applet = applet;
    recentOverlay = "none";
    super.jbInit();
    if (!allowMenu) {
      viewMenu.setEnabled(false);
      fileMenu.setEnabled(false);
      appletExportAsMenu.setEnabled(false);
      saveAsMenu.setEnabled(false);
    }
    zoomMenu.setEnabled(enableZoom);
  }

  private ActionListener exportActionListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      applet.exportSpectrum(e.getActionCommand());
    }
  };

  private static final long serialVersionUID = 1L;
  
  private JMenu aboutMenu = new JMenu();
  private JMenu fileMenu = new JMenu();
  private JMenuItem printMenuItem = new JMenuItem();
  private JMenu saveAsMenu = new JMenu();
  private JMenu viewMenu = new JMenu();
  private JMenu zoomMenu = new JMenu();
  private JMenuItem versionMenuItem = new JMenuItem();
  private JMenuItem headerMenuItem = new JMenuItem();
  JCheckBoxMenuItem windowMenuItem = new JCheckBoxMenuItem();

  protected void jbInit() {
    // handled later
  }

  protected void setMenu() {
    aboutMenu.setText("About");

    fileMenu.setText("File");
    printMenuItem.setActionCommand("Print");
    printMenuItem.setText("Print...");
    printMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        applet.printMenuItem_actionPerformed(e);
      }
    });
    viewMenu.setText("View");
    zoomMenu.setText("Zoom");
    
    headerMenuItem.setText("Show Header...");
    headerMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        applet.headerMenuItem_actionPerformed(e);
      }
    });

    windowMenuItem.setText("Window");
    windowMenuItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        applet.windowMenuItem_itemStateChanged(e);
      }
    });
    overlayKeyMenuItem = new JMenuItem();
    overlayKeyMenuItem.setEnabled(false);
    overlayKeyMenuItem.setText("Show Overlay Key...");
    overlayKeyMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        applet.overlayKeyMenuItem_actionPerformed(e);
      }
    });

    appletCompoundMenu = new JMenu();
    appletCompoundMenu.setEnabled(false);
    appletCompoundMenu.setText("Blocks");

    versionMenuItem.setText("<html><h3>" + applet.getAppletInfo() + "</h3></html>");

    add(fileMenu);
    add(viewMenu);
    add(zoomMenu);
    add(appletCompoundMenu);
    addSeparator();
    add(scriptMenuItem);
    if (applet.isPro()) {
      appletAdvancedMenuItem = new JMenuItem();
      appletAdvancedMenuItem.setText("Advanced...");
      appletAdvancedMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          applet.doAdvanced(applet.getSource().getFilePath());
        }
      });
      add(appletAdvancedMenuItem);
    }
    addSeparator();
    add(aboutMenu);
    fileMenu.add(saveAsMenu);
    if (applet.isSigned()) {
      appletExportAsMenu = new JMenu();
      fileMenu.add(appletExportAsMenu);
    }
    appletSaveAsJDXMenu = new JMenu();
    AppUtils.setMenus(saveAsMenu, appletSaveAsJDXMenu, appletExportAsMenu, exportActionListener);
    fileMenu.add(printMenuItem);

    viewMenu.add(gridCheckBoxMenuItem);
    viewMenu.add(coordsCheckBoxMenuItem);
    viewMenu.add(reversePlotCheckBoxMenuItem);
    viewMenu.addSeparator();
    viewMenu.add(headerMenuItem);
    viewMenu.add(overlayKeyMenuItem);
    viewMenu.addSeparator();
    setProcessingMenu(viewMenu);
    viewMenu.addSeparator();
    viewMenu.add(windowMenuItem);
    zoomMenu.add(nextMenuItem);
    zoomMenu.add(previousMenuItem);
    zoomMenu.add(resetMenuItem);
    zoomMenu.add(clearMenuItem);
    zoomMenu.add(userZoomMenuItem);
    aboutMenu.add(versionMenuItem);
  }

  public void setCompoundMenu(JDXSource currentSource,
                              int currentSpectrumIndex,
                              List<JDXSpectrum> specs, boolean allowSelection,
                              ActionListener compoundMenuSelectionListener,
                              ActionListener compoundMenuChooseListener) {
    appletCompoundMenu.removeAll();
    if (!allowSelection)
      return;

    appletCompoundMenu.add(overlayAllMenuItem);
    appletCompoundMenu.add(overlayNoneMenuItem);
    appletCompoundMenu.add(overlayMenuItem);
    if (specs.size() <= 20) {
      // add Menus to navigate
      if (currentSource.isCompoundSource) {
        for (int i = 0; i < specs.size(); i++) {
          JCheckBoxMenuItem mi = new JCheckBoxMenuItem((i + 1) + "- "
              + specs.get(i).getTitleLabel());
          mi.setSelected(i == currentSpectrumIndex);
          mi.addActionListener(compoundMenuSelectionListener);
          mi.setActionCommand("" + i);
          appletCompoundMenu.add(mi);
        }
        appletCompoundMenu.setText("Spectra");
      }
      // add compound menu to popup menu
      add(appletCompoundMenu, 3);
      appletCompoundMenu.setEnabled(true);
    } else {
      // open dialog box
      JMenuItem compoundMi = new JMenuItem("Choose Spectrum");
      compoundMi.addActionListener(compoundMenuChooseListener);
    }
  }
    
}
