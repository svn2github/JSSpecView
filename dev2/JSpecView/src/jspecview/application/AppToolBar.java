package jspecview.application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import jspecview.common.JSVPanel;
import jspecview.common.JSVSpecNode;
import jspecview.common.PanelData;
import jspecview.common.ScriptToken;

public class AppToolBar extends JToolBar {

  private static final long serialVersionUID = 1L;
  private MainFrame mainFrame;

  public AppToolBar(MainFrame mainFrame) {
    this.mainFrame = mainFrame;
    jbInit();
  }

  private JButton previousButton = new JButton();
  private JButton nextButton = new JButton();
  private JButton resetButton = new JButton();
  private JButton clearButton = new JButton();
  private JButton openButton = new JButton();
  private JButton propertiesButton = new JButton();
  private JButton errorLogButton = new JButton();
  JToggleButton gridToggleButton = new JToggleButton();
  JToggleButton coordsToggleButton = new JToggleButton();
  private JButton printButton = new JButton();
  private JToggleButton revPlotToggleButton = new JToggleButton();
  private JButton aboutButton = new JButton();
  private JButton overlaySplitButton = new JButton();
  private JButton overlayKeyButton = new JButton();

  private ImageIcon openIcon;
  private ImageIcon printIcon;
  private ImageIcon gridIcon;
  private ImageIcon coordinatesIcon;
  private ImageIcon reverseIcon;
  private ImageIcon previousIcon;
  private ImageIcon nextIcon;
  private ImageIcon resetIcon;
  private ImageIcon clearIcon;
  private ImageIcon informationIcon;
  private ImageIcon aboutIcon;
  private ImageIcon overlayIcon;
  private ImageIcon splitIcon;
  private ImageIcon overlayKeyIcon;
  private ImageIcon errorLogIcon;
  private ImageIcon errorLogYellowIcon;
  private ImageIcon errorLogRedIcon;

  private void getIcons() {
    Class<? extends AppToolBar> cl = getClass();
    openIcon = new ImageIcon(cl.getResource("icons/open24.gif"));
    printIcon = new ImageIcon(cl.getResource("icons/print24.gif"));
    gridIcon = new ImageIcon(cl.getResource("icons/grid24.gif"));
    coordinatesIcon = new ImageIcon(cl.getResource("icons/coords24.gif"));
    reverseIcon = new ImageIcon(cl.getResource("icons/reverse24.gif"));
    previousIcon = new ImageIcon(cl.getResource("icons/previous24.gif"));
    nextIcon = new ImageIcon(cl.getResource("icons/next24.gif"));
    resetIcon = new ImageIcon(cl.getResource("icons/reset24.gif"));
    clearIcon = new ImageIcon(cl.getResource("icons/clear24.gif"));
    informationIcon = new ImageIcon(cl.getResource("icons/information24.gif"));
    aboutIcon = new ImageIcon(cl.getResource("icons/about24.gif"));
    overlayIcon = new ImageIcon(cl.getResource("icons/overlay24.gif"));
    splitIcon = new ImageIcon(cl.getResource("icons/split24.gif"));
    overlayKeyIcon = new ImageIcon(cl.getResource("icons/overlayKey24.gif"));
    errorLogIcon = new ImageIcon(cl.getResource("icons/errorLog24.gif"));
    errorLogRedIcon = new ImageIcon(cl.getResource("icons/errorLogRed24.gif"));
    errorLogYellowIcon = new ImageIcon(cl
        .getResource("icons/errorLogYellow24.gif"));
  }


  public void setSelections(JSVPanel jsvp) {
    if (jsvp != null) {
      PanelData pd = jsvp.getPanelData();
      gridToggleButton.setSelected(pd.isGridOn());
      coordsToggleButton.setSelected(pd.isCoordinatesOn());
      revPlotToggleButton.setSelected(pd.isPlotReversed());
    }
  }

  private void jbInit() {
    getIcons();
    setButton(previousButton, "Previous View", previousIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.zoomTo(-1);
          }
        });
    setButton(nextButton, "Next View", nextIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mainFrame.zoomTo(1);
      }
    });
    setButton(resetButton, "Reset", resetIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mainFrame.zoomTo(Integer.MAX_VALUE);
      }
    });
    setButton(clearButton, "Clear Views", clearIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mainFrame.zoomTo(0);
      }
    });

    setButton(openButton, "Open", openIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mainFrame.showFileOpenDialog();
      }
    });
    setButton(propertiesButton, "Properties", informationIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.showProperties();
          }
        });
    setButton(errorLogButton, "Error Log", errorLogIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TextDialog.showError(mainFrame);
      }
    });

    setButton(gridToggleButton, "Toggle Grid", gridIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setBoolean(ScriptToken.GRIDON, e);
      }
    });
    setButton(coordsToggleButton, "Toggle Coordinates", coordinatesIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            setBoolean(ScriptToken.COORDINATESON, e);
          }
        });
    setButton(printButton, "Print", printIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mainFrame.print();
      }
    });
    setButton(revPlotToggleButton, "Reverse Plot", reverseIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            setBoolean(ScriptToken.REVERSEPLOT, e);
          }
        });
    setButton(aboutButton, "About JSpecView", aboutIcon, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        new AboutDialog(mainFrame);
      }
    });
    setButton(overlaySplitButton, "Overlay Display", overlayIcon,
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            overlaySplitButton_actionPerformed(e);
          }
        });
    setButton(overlayKeyButton, "Display Key for Overlaid Spectra",
        overlayKeyIcon, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            mainFrame.toggleOverlayKey();
          }
        });
    overlayKeyButton.setEnabled(false);

    add(openButton, null);
    add(printButton, null);
    addSeparator();
    add(gridToggleButton, null);
    add(coordsToggleButton, null);
    add(revPlotToggleButton, null);
    addSeparator();
    add(previousButton, null);
    add(nextButton, null);
    add(resetButton, null);
    add(clearButton, null);
    addSeparator();
    add(overlaySplitButton, null);
    add(overlayKeyButton, null);
    addSeparator();
    add(propertiesButton, null);
    add(errorLogButton, null);
    errorLogButton.setVisible(true);
    addSeparator();
    add(aboutButton, null);

  }

  private static void setButton(AbstractButton button, String tip,
                                ImageIcon icon, ActionListener actionListener) {
    button.setBorder(null);
    button.setToolTipText(tip);
    button.setIcon(icon);
    button.addActionListener(actionListener);
  }

  void setError(boolean isError, boolean isWarningOnly) {
    errorLogButton.setIcon(isWarningOnly ? errorLogYellowIcon
        : isError ? errorLogRedIcon : errorLogIcon);
    errorLogButton.setEnabled(isError);
  }

  protected void setBoolean(ScriptToken st, ActionEvent e) {
    boolean isOn = ((JToggleButton) e.getSource()).isSelected();
    mainFrame.runScript(st + " " + isOn);
  }

  public void setOverlay(boolean b) {
    if (b) {
      overlaySplitButton.setIcon(splitIcon);
      overlaySplitButton.setToolTipText("Split Display");
    } else {
      overlaySplitButton.setIcon(overlayIcon);
      overlaySplitButton.setToolTipText("Overlay Display");
    }
  }

  public void setMenuEnables(JSVSpecNode node) {
    if (node == null)
      return;
    PanelData pd = node.jsvp.getPanelData();
    gridToggleButton.setSelected(pd.isGridOn());
    coordsToggleButton.setSelected(pd.isCoordinatesOn());
    revPlotToggleButton.setSelected(pd.isPlotReversed());
    setOverlay(pd.isOverlaid());
  }   
  
  protected void overlaySplitButton_actionPerformed(ActionEvent e) {
    if (((JButton) e.getSource()).getIcon() == overlayIcon) {
      mainFrame.runScript("OVERLAY ALL");
    } else {
      mainFrame.runScript("CLOSE");
    }
  }

}
