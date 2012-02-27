package jspecview.applet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jspecview.common.JSVPanel;
import jspecview.common.JSVPanelPopupMenu;

class JSVAppletPopupMenu extends JSVPanelPopupMenu {

  private boolean isSigned;

  private JSVApplet applet;

  JSVAppletPopupMenu(JSVApplet applet, boolean isSigned) {
    super(applet);
    isApplet = true;
    this.isSigned = isSigned;
    this.applet = applet;
    recentOverlay = "none";
    initialize();
  }

  private ActionListener actionListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      applet.exportSpectrum(e.getActionCommand());
    }
  };

  private static final long serialVersionUID = 1L;
  
  private JMenu aboutMenu = new JMenu();
  private JMenu fileMenu = new JMenu();
  private JMenuItem printMenuItem = new JMenuItem();
  private JMenu saveAsMenu = new JMenu();
  JMenu saveAsJDXMenu = new JMenu();
  JMenu exportAsMenu = new JMenu();
  private JMenu viewMenu = new JMenu();
  private JMenu zoomMenu = new JMenu();
  JMenuItem compoundMenu = new JMenu();
  private JMenuItem versionMenuItem = new JMenuItem();
  private JMenuItem headerMenuItem = new JMenuItem();
  JCheckBoxMenuItem windowMenuItem = new JCheckBoxMenuItem();
  JMenuItem overlayKeyMenuItem = new JMenuItem();

  protected void jbInit() {
    // handled later, by initialize();
  }

  void initialize() {
    super.jbInit();
  }
  public void enableMenus(boolean menuOn, boolean enableZoom) {
    if (!menuOn) {
      viewMenu.setEnabled(false);
      fileMenu.setEnabled(false);
      exportAsMenu.setEnabled(false);
      saveAsMenu.setEnabled(false);
    }
    zoomMenu.setEnabled(enableZoom);
  }
  
  protected void setMenu() {
    aboutMenu.setText("About");

    fileMenu.setText("File");
    printMenuItem.setActionCommand("Print");
    printMenuItem.setText("Print...");
    printMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        applet.printMenuItem_actionPerformed(e);
      }
    });
    viewMenu.setText("View");
    zoomMenu.setText("Zoom");
    
    headerMenuItem.setText("Show Header...");
    headerMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        applet.headerMenuItem_actionPerformed(e);
      }
    });

    windowMenuItem.setText("Window");
    windowMenuItem.addItemListener(new java.awt.event.ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        applet.windowMenuItem_itemStateChanged(e);
      }
    });
    overlayKeyMenuItem.setEnabled(false);
    overlayKeyMenuItem.setText("Show Overlay Key...");
    overlayKeyMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        applet.overlayKeyMenuItem_actionPerformed(e);
      }
    });

    compoundMenu.setEnabled(false);
    compoundMenu.setText("Blocks");

    versionMenuItem.setText("<html><h3>JSpecView Version " + JSVApplet.APPLET_VERSION
        + "</h3></html>");

    add(fileMenu);
    add(viewMenu);
    add(zoomMenu);
    add(compoundMenu);
    addSeparator();
    add(scriptMenuItem);
    addSeparator();
    add(aboutMenu);
    JSVPanel.setMenus(saveAsMenu, saveAsJDXMenu, exportAsMenu, actionListener);
    fileMenu.add(saveAsMenu);
    if (isSigned)
      fileMenu.add(exportAsMenu);
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
  
}
