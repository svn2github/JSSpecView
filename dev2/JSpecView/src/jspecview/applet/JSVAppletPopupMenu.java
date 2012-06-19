package jspecview.applet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import jspecview.common.JSVPanelPopupMenu;
import jspecview.common.JSVSpecNode;
import jspecview.source.JDXSource;

class JSVAppletPopupMenu extends JSVPanelPopupMenu {

  private JSVAppletPrivate applet;

  JSVAppletPopupMenu(JSVAppletPrivate applet, 
      boolean allowMenu, boolean enableZoom) {
    super(applet);
    isApplet = true;
    this.applet = applet;
    recentOverlay = "none";
    super.jbInit();
    if (!allowMenu) {
    	// all except About and Zoom disabled
      fileMenu.setEnabled(false);
      viewMenu.setEnabled(false);
      appletCompoundMenu.setEnabled(false);
      scriptMenuItem.setEnabled(false);
      appletAdvancedMenuItem.setEnabled(false);
      printMenuItem.setEnabled(false);
    	// about still allowed
    }
    zoomMenu.setEnabled(enableZoom);
  }

  private ActionListener exportActionListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      applet.exportSpectrumViaMenu(e.getActionCommand());
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

  /**
   * called by super.jbInit()
   * 
   */
  @Override
  protected void setMenu() {
    aboutMenu.setText("About");

    fileMenu.setText("File");
    //fileMenu.setEnabled(applet.isSigned()); ?
    fileMenu.add(saveAsMenu);
    if (applet.isSigned()) {
      appletExportAsMenu = new JMenu();
      fileMenu.add(appletExportAsMenu);
    }
    appletSaveAsJDXMenu = new JMenu();
    JSVPanelPopupMenu.setMenus(saveAsMenu, appletSaveAsJDXMenu, appletExportAsMenu, exportActionListener);

    viewMenu.setText("View");
    zoomMenu.setText("Zoom");
    
    headerMenuItem.setText("Show Header...");
    headerMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        applet.showHeader();
      }
    });

    windowMenuItem.setText("Window");
    windowMenuItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        applet.newWindow(e.getStateChange() == ItemEvent.SELECTED);
      }
    });
    overlayKeyMenuItem.setEnabled(false);
    overlayKeyMenuItem.setText("Show Overlay Key...");
    overlayKeyMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        overlayKeyMenuItem_actionPerformed();
      }
    });

    appletCompoundMenu = new JMenu();
    appletCompoundMenu.setEnabled(false);
    appletCompoundMenu.setText("Spectra");

    appletAdvancedMenuItem = new JMenuItem();
    appletAdvancedMenuItem.setText("Advanced...");
    appletAdvancedMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        applet.doAdvanced(applet.getSource().getFilePath());
      }
    });
    appletAdvancedMenuItem.setEnabled(applet.isPro());

    printMenuItem.setActionCommand("Print");
    printMenuItem.setEnabled(applet.isSigned());
    printMenuItem.setText("Print...");
    printMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        applet.print();
      }
    });
    
    versionMenuItem.setText("<html><h3>" + applet.getJsvApplet().getAppletInfo() + "</h3></html>");

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

    add(fileMenu);
    add(viewMenu);
    add(zoomMenu);
    add(appletCompoundMenu);
    addSeparator();
    add(scriptMenuItem);
    add(appletAdvancedMenuItem);
    addSeparator();
    add(printMenuItem);
    addSeparator();
    add(aboutMenu);

  }

  protected void overlayKeyMenuItem_actionPerformed() {
    //TODO: test this -- doesn't account for selected panels? 
    overlayKeyMenuItem.setSelected(!overlayKeyMenuItem.isSelected());
    boolean visible = overlayKeyMenuItem.isSelected();
    applet.showOverlayKey(visible);
  }

  public void setCompoundMenu(JDXSource currentSource,
                              int currentSpectrumIndex,
                              List<JSVSpecNode> specNodes, boolean allowSelection,
                              ActionListener compoundMenuSelectionListener,
                              ActionListener compoundMenuChooseListener) {
    appletCompoundMenu.removeAll();
    if (!allowSelection)
      return;

    appletCompoundMenu.add(overlayAllMenuItem);
    appletCompoundMenu.add(overlayNoneMenuItem);
    appletCompoundMenu.add(overlayMenuItem);
    appletCompoundMenu.add(overlayStackOffsetMenuItem);
    if (specNodes.size() <= 20) {
      // add Menus to navigate
      if (currentSource.isCompoundSource) {
        for (int i = 0; i < specNodes.size(); i++) {
          JCheckBoxMenuItem mi = new JCheckBoxMenuItem((i + 1) + "- "
              + specNodes.get(i).getSpectrum().getTitleLabel());
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
