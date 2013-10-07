package jspecview.applet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;


import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jspecview.app.JSVApp;
import jspecview.common.JSViewer;
import jspecview.java.AwtPopupMenu;

class AwtAppletPopupMenu extends AwtPopupMenu {

  JSVApp app;

  AwtAppletPopupMenu(JSViewer viewer, 
      boolean allowMenu, boolean enableZoom) {
    super(viewer, false);
    isApplet = true;
    app = (JSVApp) viewer.si;
    super.jbInit();
    if (!allowMenu) {
    	// all except About and Zoom disabled
      fileMenu.setEnabled(false);
      viewMenu.setEnabled(false);
      spectraMenuItem.setEnabled(false);
      scriptMenuItem.setEnabled(false);
      //appletAdvancedMenuItem.setEnabled(false);
      printMenuItem.setEnabled(false);
    	// about still allowed
    }
    zoomMenu.setEnabled(enableZoom);
  }

  private ActionListener exportActionListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      app.exportSpectrumViaMenu(e.getActionCommand());
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

  @Override
	protected void jbInit() {
    // handled later
  }

  /**
   * called by super.jbInit()
   * 
   */
  @Override
  protected void setPopupMenu() {
    aboutMenu.setText("About");
    fileMenu.setText("File");
    //fileMenu.setEnabled(applet.isSigned()); ?
    fileMenu.add(saveAsMenu);
    if (app.isSigned()) {
      appletExportAsMenu = new JMenu();
      fileMenu.add(appletExportAsMenu);
    }
    appletSaveAsJDXMenu = new JMenu();
    AwtPopupMenu.setMenus(saveAsMenu, appletSaveAsJDXMenu, appletExportAsMenu, exportActionListener);

    viewMenu.setText("View");
    zoomMenu.setText("Zoom");
    
    headerMenuItem.setText("Show Header...");
    headerMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        app.showHeader();
      }
    });

    windowMenuItem.setText("Window");
    windowMenuItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        app.newWindow(e.getStateChange() == ItemEvent.SELECTED, false);
      }
    });
    overlayKeyMenuItem.setEnabled(false);
    overlayKeyMenuItem.setText("Show Overlay Key...");
    overlayKeyMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        overlayKeyMenuItem_actionPerformed();
      }
    });

    setOverlayItems();

    //appletAdvancedMenuItem = new JMenuItem();
    //appletAdvancedMenuItem.setText("Advanced...");
    //appletAdvancedMenuItem.addActionListener(new ActionListener() {
    //  public void actionPerformed(ActionEvent e) {
    //    applet.doAdvanced(applet.getCurrentSource().getFilePath());
    //  }
    //});
    //appletAdvancedMenuItem.setEnabled(applet.isPro());

    printMenuItem.setActionCommand("Print");
    printMenuItem.setEnabled(app.isSigned());
    printMenuItem.setText("Print...");
    printMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        app.print();
      }
    });
    
    versionMenuItem.setText("<html><h3>" + app.getAppletFrame().getAppletInfo() + "</h3></html>");

    viewMenu.add(gridCheckBoxMenuItem);
    viewMenu.add(coordsCheckBoxMenuItem);
    viewMenu.add(reversePlotCheckBoxMenuItem);
    viewMenu.addSeparator();
    viewMenu.add(headerMenuItem);
    viewMenu.add(overlayKeyMenuItem);
    viewMenu.addSeparator();
    viewMenu.add(windowMenuItem);
    zoomMenu.add(nextMenuItem);
    zoomMenu.add(previousMenuItem);
    zoomMenu.add(resetMenuItem);
    zoomMenu.add(clearMenuItem);
    zoomMenu.add(userZoomMenuItem);
    aboutMenu.add(versionMenuItem);

    add(fileMenu);
    add(viewMenu);
    add(zoomMenu);
    add(spectraMenuItem);
    add(overlayStackOffsetMenuItem);
    addSeparator();
    setProcessingMenu(this);
    addSeparator();
    add(scriptMenuItem);
    //add(appletAdvancedMenuItem);
    addSeparator();
    add(printMenuItem);
    addSeparator();
    add(aboutMenu);

  }

  protected void overlayKeyMenuItem_actionPerformed() {
    //TODO: test this -- doesn't account for selected panels? 
    overlayKeyMenuItem.setSelected(!overlayKeyMenuItem.isSelected());
    boolean visible = overlayKeyMenuItem.isSelected();
    app.showOverlayKey(visible);
  }

}
