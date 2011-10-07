/* Copyright (c) 2002-2008 The University of the West Indies
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

package jspecview.application;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jspecview.common.JSVPanel;
import jspecview.common.JSpecViewUtils;
import jspecview.exception.JSpecViewException;
import jspecview.source.JDXSource;

/**
 * Dialog to change the preferences for the application.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */

public class PreferencesDialog extends JDialog {
  JPanel contentpanel = new JPanel();
  BorderLayout contentBorderLayout = new BorderLayout();
  JTabbedPane preferencesTabbedPane = new JTabbedPane();
  JPanel generalPanel = new JPanel();
  JPanel displayPanel = new JPanel();
  BorderLayout generalBorderLayout = new BorderLayout();
  Box generalContentBox;
  TitledBorder fontTitledBorder;
  TitledBorder contentTitledBorder;
  JCheckBox confirmExitCheckBox = new JCheckBox();
  JCheckBox statusBarCheckBox = new JCheckBox();
  JCheckBox toolbarCheckBox = new JCheckBox();
  JCheckBox sidePanelCheckBox = new JCheckBox();
  JCheckBox exportDirCheckBox = new JCheckBox();
  JCheckBox openedDirCheckBox = new JCheckBox();
  JCheckBox legendCheckBox = new JCheckBox();
  JCheckBox overlayCheckBox = new JCheckBox();
  JButton clearRecentButton = new JButton();
  JButton cancelButton = new JButton();
  JPanel buttonPanel = new JPanel();
  JButton okButton = new JButton();
  private BorderLayout borderLayout6 = new BorderLayout();
  private DefaultListModel elModel = new DefaultListModel();
  private JPanel topPanel = new JPanel();
  private JPanel displayFontPanel = new JPanel();
  private JPanel colorSchemePanel = new JPanel();
  private JPanel elementPanel = new JPanel();
  private JLabel elementLabel = new JLabel();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private JCheckBox defaultFontCheckBox = new JCheckBox();
  private JComboBox fontComboBox = new JComboBox();
  private GridBagLayout gridBagLayout2 = new GridBagLayout();
  private JPanel colorPanel = new JPanel();
  private JComboBox schemeComboBox = new JComboBox();
  private GridLayout gridLayout1 = new GridLayout();
  private JCheckBox defaultColorCheckBox = new JCheckBox();
  private JButton customButton = new JButton();
  private GridBagLayout gridBagLayout3 = new GridBagLayout();
  private JScrollPane listScrollPane = new JScrollPane();
  JList elementList = new JList();
  private JButton colorButton8 = new JButton();
  private JButton colorButton7 = new JButton();
  private JButton colorButton6 = new JButton();
  private JButton colorButton5 = new JButton();
  private JButton colorButton4 = new JButton();
  private JButton colorButton3 = new JButton();
  private JButton colorButton2 = new JButton();
  private JButton colorButton1 = new JButton();
  JButton currentColorButton = new JButton();
  private JPanel processingPanel = new JPanel();
  private GridBagLayout gridBagLayout4 = new GridBagLayout();
  private JButton saveButton = new JButton();
  private GridBagLayout gridBagLayout5 = new GridBagLayout();
  private JPanel integrationPanel = new JPanel();
  private JPanel absTransPanel = new JPanel();
  private GridBagLayout gridBagLayout6 = new GridBagLayout();
  private TitledBorder integratinTitledBorder;
  private JLabel jLabel1 = new JLabel();
  private JTextField minYTextField = new JTextField();
  private JLabel jLabel2 = new JLabel();
  private JTextField integFactorTextField = new JTextField();
  private JLabel jLabel3 = new JLabel();
  private JLabel jLabel4 = new JLabel();
  private JButton processingCustomButton = new JButton();
  private JCheckBox autoIntegrateCheckBox = new JCheckBox();
  private JTextField integOffsetTextField = new JTextField();
  private TitledBorder absTransTitledBorder;
  private JCheckBox separateWindowCheckBox = new JCheckBox();
  //private ButtonGroup fontButtonGroup = new ButtonGroup();
  private JLabel jLabel5 = new JLabel();
  private JLabel jLabel6 = new JLabel();
  private JLabel jLabel7 = new JLabel();
  private GridBagLayout gridBagLayout7 = new GridBagLayout();
  private JRadioButton TtoARadioButton = new JRadioButton();
  private JRadioButton AtoTRadioButton = new JRadioButton();
  private ButtonGroup conversionButtonGroup = new ButtonGroup();

  DisplayScheme currentDS = new DisplayScheme("Current");
  private DisplaySchemesProcessor dsp;
  private JSVPanel previewPanel = null;
  private String defaultDSName = "";
  private JLabel jLabel8 = new JLabel();
  private JLabel jLabel9 = new JLabel();
  //private JColorChooser cc = new JColorChooser();
  private JCheckBox AutoConvertCheckBox = new JCheckBox();
  //private boolean clearRecentFiles = false;
  JButton plotColorButton = new JButton();
  private JPanel colorPanel1 = new JPanel();
  private JButton procColorButton8 = new JButton();
  private JButton procColorButton7 = new JButton();
  private JButton procColorButton6 = new JButton();
  private JButton procColorButton5 = new JButton();
  private JButton procColorButton4 = new JButton();
  private JButton procColorButton3 = new JButton();
  private JButton procColorButton2 = new JButton();
  private GridLayout gridLayout2 = new GridLayout();
  private JButton procColorButton1 = new JButton();

  Properties preferences;
  private JCheckBox gridCheckBox = new JCheckBox();
  private JCheckBox coordinatesCheckBox = new JCheckBox();
  private JCheckBox scaleCheckBox = new JCheckBox();
  private JCheckBox svgCheckBox = new JCheckBox();
  public static boolean inkscape = false;

  /**
   * Initialises the <code>PreferencesDialog</code>
   * @param frame the the parent frame
   * @param title the title
   * @param modal the modality
   * @param prefs the initial preferences
   * @param dsp an instance of <code>DisplaySchemesProcessor</code>
   */
  public PreferencesDialog(Frame frame, String title, boolean modal,
                           Properties prefs, DisplaySchemesProcessor dsp) {
    super(frame, title, modal);

    preferences = prefs;
    this.dsp = dsp;

    elModel.addElement("Title");
    elModel.addElement("Plot");
    elModel.addElement("Scale");
    elModel.addElement("Units");
    elModel.addElement("Coordinates");
    elModel.addElement("PlotArea");
    elModel.addElement("Background");
    elModel.addElement("Grid");



    try {
      jbInit();
      setSize(480, 550);
      setResizable(false);
      if(dsp != null){
        initDisplayTab();
      }
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }

    initProcessingTab();
    initGeneralTab();

    colorButton1.addActionListener(new ColorPanelActionListener());
    colorButton2.addActionListener(new ColorPanelActionListener());
    colorButton3.addActionListener(new ColorPanelActionListener());
    colorButton4.addActionListener(new ColorPanelActionListener());
    colorButton5.addActionListener(new ColorPanelActionListener());
    colorButton6.addActionListener(new ColorPanelActionListener());
    colorButton7.addActionListener(new ColorPanelActionListener());
    colorButton8.addActionListener(new ColorPanelActionListener());

    procColorButton1.addActionListener(new ProcColorPanelActionListener());
    procColorButton2.addActionListener(new ProcColorPanelActionListener());
    procColorButton3.addActionListener(new ProcColorPanelActionListener());
    procColorButton4.addActionListener(new ProcColorPanelActionListener());
    procColorButton5.addActionListener(new ProcColorPanelActionListener());
    procColorButton6.addActionListener(new ProcColorPanelActionListener());
    procColorButton7.addActionListener(new ProcColorPanelActionListener());
    procColorButton8.addActionListener(new ProcColorPanelActionListener());

    elementList.addListSelectionListener(new ElementListSelectionListener());
    elementList.getSelectionModel().setSelectionInterval(0, 0);

    setVisible(true);
  }

  /**
   * Initialise the Display Tab, where display schemes are created or set
   */
  private void initDisplayTab(){
    TreeMap<String, DisplayScheme> displaySchemes = dsp.getDisplaySchemes();

    defaultDSName = preferences.getProperty("defaultDisplaySchemeName");

    // load names of schemes in schemeComboBox
    for (Iterator<String> i = displaySchemes.keySet().iterator(); i.hasNext(); ) {
      Object item = i.next();
      schemeComboBox.addItem(item);
    }

    schemeComboBox.setSelectedItem(defaultDSName);

    // load names of fonts in fontComboBox
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    String allFontNames[] = ge.getAvailableFontFamilyNames();
    for(int i = 0; i < allFontNames.length; i++){
      fontComboBox.addItem(allFontNames[i]);
    }

    // init preview panel
    try {
      JDXSource source = JDXSource.createJDXSource(null, "sample.jdx", null);

      previewPanel = new JSVPanel(source.getSpectra());
      previewPanel.setZoomEnabled(false);
      previewPanel.setCoordinatesOn(true);
      previewPanel.setGridOn(true);
    }
    catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }
    catch (JSpecViewException ex) {
      ex.printStackTrace();
    }

    if(previewPanel != null)
      displayPanel.add(previewPanel, BorderLayout.CENTER);
    else{
      displayPanel.add(new JButton("Error Loading Sample File!"), BorderLayout.CENTER);
    }

    schemeComboBox.setSelectedItem(currentDS.getName());
    fontComboBox.setSelectedItem(currentDS.getFont());

    repaint();
  }

  /**
   * Initalises the precesing tab, where integration properties and
   * transmittance/Adsorbance properties are set
   */
  private void initProcessingTab(){

    minYTextField.setText(preferences.getProperty("integralMinY"));
    integFactorTextField.setText(preferences.getProperty("integralFactor"));
    integOffsetTextField.setText(preferences.getProperty("integralOffset"));
    plotColorButton.setBackground(
        JSpecViewUtils.getColorFromString(preferences.getProperty("integralPlotColor")));
    autoIntegrateCheckBox.setSelected(
       Boolean.valueOf(preferences.getProperty("automaticallyIntegrate")).booleanValue());
    String autoConvert =
        preferences.getProperty("automaticTAConversion");
    if(autoConvert.equals("TtoA")){
      TtoARadioButton.setSelected(true);
      autoIntegrateCheckBox.setSelected(true);
    }else if(autoConvert.equals("AtoT")){
      AtoTRadioButton.setSelected(true);
      autoIntegrateCheckBox.setSelected(true);
    }else{
      autoIntegrateCheckBox.setSelected(false);
    }

    separateWindowCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("AtoTSeparateWindow")).booleanValue());

  }

  /**
   * Intialises the general tab, where general properties of the application
   * are set
   */
  private void initGeneralTab(){

    confirmExitCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("confirmBeforeExit")).booleanValue());
    overlayCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("automaticallyOverlay")).booleanValue());
    legendCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("automaticallyShowLegend")).booleanValue());
    openedDirCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("useDirectoryLastOpenedFile")).booleanValue());
    exportDirCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("useDirectoryLastExportedFile")).booleanValue());
    sidePanelCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("showSidePanel")).booleanValue());
    toolbarCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("showToolBar")).booleanValue());
    gridCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("showGrid")).booleanValue());
    coordinatesCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("showCoordinates")).booleanValue());
    scaleCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("showScale")).booleanValue());
    svgCheckBox.setSelected(
        Boolean.valueOf(preferences.getProperty("svgExport")).booleanValue());

  }

  /**
   * class <code>ColorPanelActionListener</code> is the <code>ActionListener</code>
   * for the panel of color buttons
   */
  class ColorPanelActionListener implements ActionListener{

    /**
     * Sets the color of the selected element on the current color button
     * @param ae the ActionEvent
     */
    public void actionPerformed(ActionEvent ae){
      JButton button = (JButton)ae.getSource();
      Color color = button.getBackground();
      currentColorButton.setBackground(color);
      String element = (String)elementList.getSelectedValue();
      currentDS.setColor(element.toLowerCase(), color);
      // kludge
      currentDS.setName("Current");
      updatePreviewPanel();
    }
  }

  /**
   * Listener for the element list
   */
  class ElementListSelectionListener implements ListSelectionListener{

    /**
     * Sets the color of the currentColorButton to the color of the selected
     * element in the list
     * @param lse the ListSelectionEvent
     */
    public void valueChanged(ListSelectionEvent lse){
      JList list = (JList)lse.getSource();
      String element = (String)list.getSelectedValue();
      Color color = currentDS.getColor(element.toLowerCase());
      currentColorButton.setBackground(color);
    }
  }

  /**
   * Initialises GUI components
   * @throws Exception
   */
  void jbInit() throws Exception {
    generalContentBox = Box.createVerticalBox();
    fontTitledBorder = new TitledBorder("");
    contentTitledBorder = new TitledBorder("");
    integratinTitledBorder = new TitledBorder("");
    absTransTitledBorder = new TitledBorder("");
    generalContentBox.setAlignmentY(Component.LEFT_ALIGNMENT);
    contentpanel.setLayout(contentBorderLayout);
    generalPanel.setLayout(generalBorderLayout);
    fontTitledBorder.setTitle("Font");
    fontTitledBorder.setTitleJustification(2);
    contentTitledBorder.setTitle("Content");
    contentTitledBorder.setTitleJustification(2);
    generalPanel.setBorder(BorderFactory.createEtchedBorder());
    confirmExitCheckBox.setText("Confirm before exiting");
    statusBarCheckBox.setText("Show status bar");
    toolbarCheckBox.setText("Show toolbar");
    sidePanelCheckBox.setText("Show SidePanel");
    exportDirCheckBox.setText("Remember directory of last exported file");
    openedDirCheckBox.setText("Remember directory of last opened file");
    legendCheckBox.setText("Automatically show legend when spectra are overlayed");
    overlayCheckBox.setText("Show Compound Files as overlayed if possible");
    clearRecentButton.setText("Clear Recent Files");
    clearRecentButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearRecentButton_actionPerformed(e);
      }
    });
    cancelButton.setText("CANCEL");
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    okButton.setText("OK");
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });
    displayPanel.setLayout(borderLayout6);
    topPanel.setLayout(gridBagLayout4);
    elementPanel.setBorder(BorderFactory.createEtchedBorder());
    elementPanel.setLayout(gridBagLayout1);
    colorSchemePanel.setBorder(BorderFactory.createEtchedBorder());
    colorSchemePanel.setLayout(gridBagLayout3);
    displayFontPanel.setBorder(BorderFactory.createEtchedBorder());
    displayFontPanel.setLayout(gridBagLayout2);
    elementLabel.setText("Element:");
    defaultFontCheckBox.setText("Use Default");
    defaultFontCheckBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        defaultFontCheckBox_actionPerformed(e);
      }
    });
    colorPanel.setLayout(gridLayout1);
    schemeComboBox.setMaximumSize(new Dimension(200, 21));
    schemeComboBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        schemeComboBox_actionPerformed(e);
      }
    });
    gridLayout1.setHgap(2);
    gridLayout1.setRows(2);
    gridLayout1.setVgap(2);
    defaultColorCheckBox.setText("Use Default");
    defaultColorCheckBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        defaultColorCheckBox_actionPerformed(e);
      }
    });
    customButton.setText("Custom...");
    customButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        customButton_actionPerformed(e);
      }
    });
    elementList.setToolTipText("");
    elementList.setModel(elModel);
    elementList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    elementList.setVisibleRowCount(4);
    listScrollPane.setMinimumSize(new Dimension(125, 110));
    listScrollPane.setPreferredSize(new Dimension(125, 110));
    colorButton8.setBackground(Color.magenta);
    colorButton8.setBorder(BorderFactory.createLoweredBevelBorder());
    colorButton7.setBackground(new Color(0, 92, 0));
    colorButton7.setBorder(BorderFactory.createLoweredBevelBorder());
    colorButton6.setBackground(new Color(0, 0, 64));
    colorButton6.setBorder(BorderFactory.createLoweredBevelBorder());
    colorButton5.setBackground(Color.red);
    colorButton5.setBorder(BorderFactory.createLoweredBevelBorder());
    colorButton4.setBackground(Color.blue);
    colorButton4.setBorder(BorderFactory.createLoweredBevelBorder());
    colorButton3.setBackground(Color.gray);
    colorButton3.setBorder(BorderFactory.createLoweredBevelBorder());
    colorButton3.setText(" ");
    colorButton2.setBackground(Color.white);
    colorButton2.setBorder(BorderFactory.createLoweredBevelBorder());
    colorButton1.setBackground(Color.black);
    colorButton1.setBorder(BorderFactory.createLoweredBevelBorder());
    currentColorButton.setBorder(BorderFactory.createLoweredBevelBorder());
    currentColorButton.setMaximumSize(new Dimension(50, 11));
    currentColorButton.setMinimumSize(new Dimension(50, 11));
    currentColorButton.setPreferredSize(new Dimension(50, 11));
    currentColorButton.setMnemonic('0');
    saveButton.setText("Save Scheme");
    saveButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        saveButton_actionPerformed(e);
      }
    });
    processingPanel.setLayout(gridBagLayout5);
    integrationPanel.setLayout(gridBagLayout6);
    integrationPanel.setBorder(integratinTitledBorder);
    integratinTitledBorder.setTitle("Integration");
    integratinTitledBorder.setTitleJustification(2);
    jLabel1.setText("Integral Factor");
    jLabel2.setText("Minimum Y");
    jLabel3.setText("Integral Offset");
    jLabel4.setText("Plot Color");
    processingCustomButton.setPreferredSize(new Dimension(87, 21));
    processingCustomButton.setText("Custom...");
    processingCustomButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        processingCustomButton_actionPerformed(e);
      }
    });
    autoIntegrateCheckBox.setText("Automatically Integrate HNMR Spectra");
    minYTextField.setMinimumSize(new Dimension(40, 21));
    minYTextField.setPreferredSize(new Dimension(40, 21));
    integFactorTextField.setMinimumSize(new Dimension(40, 21));
    integFactorTextField.setPreferredSize(new Dimension(40, 21));
    integOffsetTextField.setMinimumSize(new Dimension(40, 21));
    integOffsetTextField.setPreferredSize(new Dimension(40, 21));
    absTransPanel.setBorder(absTransTitledBorder);
    absTransPanel.setLayout(gridBagLayout7);
    absTransTitledBorder.setTitle("Absorbance/Transmittance");
    absTransTitledBorder.setTitleJustification(2);
    separateWindowCheckBox.setEnabled(false);
    separateWindowCheckBox.setText("Show converted Spectrum in a separate window");
    jLabel5.setText("%");
    jLabel6.setText("%");
    jLabel7.setText("%");
    TtoARadioButton.setSelected(true);
    TtoARadioButton.setText("Transmittance to Absorbance");
    AtoTRadioButton.setText("Absorbance to Transmittance");
    colorPanel.setBorder(BorderFactory.createRaisedBevelBorder());
    jLabel8.setText("Color Scheme:");
    jLabel9.setText("Font:");
    fontComboBox.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fontComboBox_actionPerformed(e);
      }
    });
    AutoConvertCheckBox.setToolTipText("");
    AutoConvertCheckBox.setText("Automatically Convert");
    plotColorButton.setBackground(Color.green);
    plotColorButton.setBorder(null);
    plotColorButton.setPreferredSize(new Dimension(30, 21));
    colorPanel1.setBorder(BorderFactory.createRaisedBevelBorder());
    colorPanel1.setLayout(gridLayout2);
    procColorButton8.setBackground(Color.magenta);
    procColorButton8.setBorder(BorderFactory.createLoweredBevelBorder());
    procColorButton7.setBackground(new Color(0, 92, 0));
    procColorButton7.setBorder(BorderFactory.createLoweredBevelBorder());
    procColorButton6.setBackground(new Color(0, 0, 64));
    procColorButton6.setBorder(BorderFactory.createLoweredBevelBorder());
    procColorButton5.setBackground(Color.red);
    procColorButton5.setBorder(BorderFactory.createLoweredBevelBorder());
    procColorButton4.setBackground(Color.blue);
    procColorButton4.setBorder(BorderFactory.createLoweredBevelBorder());
    procColorButton3.setBackground(Color.gray);
    procColorButton3.setBorder(BorderFactory.createLoweredBevelBorder());
    procColorButton3.setText(" ");
    procColorButton2.setBackground(Color.white);
    procColorButton2.setBorder(BorderFactory.createLoweredBevelBorder());
    gridLayout2.setHgap(2);
    gridLayout2.setRows(2);
    gridLayout2.setVgap(2);
    procColorButton1.setBackground(Color.black);
    procColorButton1.setBorder(BorderFactory.createLoweredBevelBorder());
    procColorButton1.setMaximumSize(new Dimension(20, 20));
    procColorButton1.setMinimumSize(new Dimension(20, 20));
    procColorButton1.setPreferredSize(new Dimension(20, 20));
    gridCheckBox.setToolTipText("");
    gridCheckBox.setText("Show Grid");
    coordinatesCheckBox.setText("Show Coordinates");
    scaleCheckBox.setText("Show Scale");
    svgCheckBox.setText("SVG export for Inkscape");
    displayFontPanel.add(fontComboBox,                    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    displayFontPanel.add(defaultFontCheckBox,                  new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0));
    displayFontPanel.add(jLabel9,      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 5));
    getContentPane().add(contentpanel);
    contentpanel.add(preferencesTabbedPane, BorderLayout.CENTER);
    preferencesTabbedPane.add(generalPanel,   "General");
    preferencesTabbedPane.add(displayPanel,     "Display Scheme");
    displayPanel.add(topPanel,  BorderLayout.NORTH);
    topPanel.add(elementPanel,    new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 13, 0));
    elementPanel.add(listScrollPane,          new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTH, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
    listScrollPane.getViewport().add(elementList, null);
    elementPanel.add(elementLabel,           new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
    colorSchemePanel.add(schemeComboBox,          new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    colorSchemePanel.add(defaultColorCheckBox,  new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    colorSchemePanel.add(customButton,         new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    colorSchemePanel.add(colorPanel,         new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    colorPanel.add(colorButton1, null);
    colorPanel.add(colorButton2, null);
    colorPanel.add(colorButton3, null);
    colorPanel.add(colorButton4, null);
    colorPanel.add(colorButton5, null);
    colorPanel.add(colorButton6, null);
    colorPanel.add(colorButton7, null);
    colorPanel.add(colorButton8, null);
    colorSchemePanel.add(currentColorButton,        new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), -16, 0));
    colorSchemePanel.add(jLabel8,    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 5));
    topPanel.add(colorSchemePanel,    new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 8, 17));
    topPanel.add(displayFontPanel,    new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 10, 0, 1), 8, 36));
    preferencesTabbedPane.add(processingPanel,  "Processing");
    processingPanel.add(integrationPanel,            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 10, 0), 0, 30));
    contentpanel.add(buttonPanel,  BorderLayout.SOUTH);
    buttonPanel.add(okButton, null);
    buttonPanel.add(cancelButton, null);
    generalPanel.add(generalContentBox, BorderLayout.CENTER);
    generalContentBox.add(confirmExitCheckBox, null);
    generalContentBox.add(overlayCheckBox, null);
    generalContentBox.add(legendCheckBox, null);
    generalContentBox.add(openedDirCheckBox, null);
    generalContentBox.add(exportDirCheckBox, null);
    generalContentBox.add(sidePanelCheckBox, null);
    generalContentBox.add(toolbarCheckBox, null);
    generalContentBox.add(statusBarCheckBox, null);
    generalContentBox.add(gridCheckBox, null);
    generalContentBox.add(coordinatesCheckBox, null);
    generalContentBox.add(scaleCheckBox, null);
    generalContentBox.add(svgCheckBox, null);
    generalContentBox.add(clearRecentButton, null);
    topPanel.add(saveButton,     new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));
    processingPanel.add(absTransPanel,      new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    integrationPanel.add(jLabel2,                              new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 10, 0), 0, 0));
    integrationPanel.add(jLabel1,                  new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 10, 0), 0, 0));
    integrationPanel.add(autoIntegrateCheckBox,                   new GridBagConstraints(0, 5, 4, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(10, 5, 0, 0), 0, 0));
    integrationPanel.add(minYTextField,                     new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 50, 0, 0), 0, 0));
    integrationPanel.add(integFactorTextField,              new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 50, 0, 0), 0, 0));
    integrationPanel.add(jLabel3,           new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    integrationPanel.add(integOffsetTextField,            new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 50, 0, 0), 0, 0));
    integrationPanel.add(jLabel5,         new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    integrationPanel.add(jLabel6,        new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    integrationPanel.add(jLabel7,        new GridBagConstraints(2, 2, 1, 2, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    integrationPanel.add(colorPanel1,            new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    colorPanel1.add(procColorButton1, null);
    colorPanel1.add(procColorButton2, null);
    colorPanel1.add(procColorButton3, null);
    colorPanel1.add(procColorButton4, null);
    colorPanel1.add(procColorButton5, null);
    colorPanel1.add(procColorButton6, null);
    colorPanel1.add(procColorButton7, null);
    colorPanel1.add(procColorButton8, null);
    integrationPanel.add(jLabel4,    new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
    integrationPanel.add(processingCustomButton,        new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    integrationPanel.add(plotColorButton,                new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 50, 0, 0), 0, 0));
    absTransPanel.add(separateWindowCheckBox,        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 147, 0));
    absTransPanel.add(TtoARadioButton,     new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
    absTransPanel.add(AtoTRadioButton,      new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));
    absTransPanel.add(AutoConvertCheckBox,   new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
    conversionButtonGroup.add(TtoARadioButton);
    conversionButtonGroup.add(AtoTRadioButton);
  }

  /**
   * disposes the dialog when the cancel button is pressed
   * @param e the ActionEvent
   */
  void cancelButton_actionPerformed(ActionEvent e) {
    dispose();
  }

  /**
   * Updates the preferences that were set to the <code>Properties</code> passed
   * in the constructor
   * @param e the ActionEvent
   */
  void okButton_actionPerformed(ActionEvent e) {
    //preferences = new Properties();

    // rewrite and call setProperty method instead of the put method

    // General tab
    preferences.setProperty(
        "confirmBeforeExit", Boolean.toString(confirmExitCheckBox.isSelected()));
    preferences.setProperty(
        "automaticallyOverlay", Boolean.toString(overlayCheckBox.isSelected()));
    preferences.setProperty(
        "automaticallyShowLegend", Boolean.toString(legendCheckBox.isSelected()));
    preferences.setProperty(
        "useDirectoryLastOpenedFile", Boolean.toString(openedDirCheckBox.isSelected()));
    preferences.setProperty(
        "useDirectoryLastExportedFile", Boolean.toString(exportDirCheckBox.isSelected()));
    preferences.setProperty(
        "showSidePanel", Boolean.toString(sidePanelCheckBox.isSelected()));
    preferences.setProperty(
        "showToolBar", Boolean.toString(toolbarCheckBox.isSelected()));
    preferences.setProperty(
        "showStatusBar", Boolean.toString(toolbarCheckBox.isSelected()));
    preferences.setProperty(
        "showGrid", Boolean.toString(gridCheckBox.isSelected()));
    preferences.setProperty(
        "showCoordinates", Boolean.toString(coordinatesCheckBox.isSelected()));
    preferences.setProperty(
        "showScale", Boolean.toString(scaleCheckBox.isSelected()));
    preferences.setProperty(
        "svgExport", Boolean.toString(svgCheckBox.isSelected()));
    inkscape = Boolean.valueOf(preferences.getProperty("svgExport")).booleanValue();

    // Processing tab
    preferences.setProperty("automaticallyIntegrate", Boolean.toString(autoIntegrateCheckBox.isSelected()));

    boolean autoTACovert = Boolean.valueOf(AutoConvertCheckBox.isSelected()).booleanValue();
    if(autoTACovert){
      if(TtoARadioButton.isSelected())
        preferences.setProperty("automaticTAConversion", "TtoA");
      else
        preferences.setProperty("automaticTAConversion", "AtoT");
    }else{
      preferences.setProperty("automaticTAConversion", "false");
    }

    preferences.setProperty(
        "AtoTSeparateWindow", Boolean.toString(separateWindowCheckBox.isSelected()));
    preferences.setProperty("integralMinY", minYTextField.getText());
    preferences.setProperty("integralFactor", integFactorTextField.getText());
    preferences.setProperty("integralOffset", integOffsetTextField.getText());
    preferences.setProperty("integralPlotColor",
                           JSpecViewUtils.colorToHexString(plotColorButton.getBackground()));

    // Display Schemes Tab
    preferences.setProperty("defaultDisplaySchemeName", currentDS.getName());
//    System.out.println(currentDS.getName());

    //TreeMap<String,DisplayScheme> dispSchemes;
    if(currentDS.getName().equals("Current")){
      //@SuppressWarnings("unchecked")
      TreeMap<String, DisplayScheme> dispSchemes = dsp.getDisplaySchemes();
      dispSchemes.put("Current", currentDS);
    }

    dispose();
  }

  /**
   * Returns the preferences (<code>Properties</code> Object)
   * @return the preferences (<code>Properties</code> Object)
   */
  public Properties getPreferences(){
    return preferences;
  }

  /**
   * Shows a Color Dialog and updates the currentColorButton and the preview
   * panel accordingly
   * @param e the ActionEvent
   */
  void customButton_actionPerformed(ActionEvent e) {
    Color color = JColorChooser.showDialog(this, "Choose Color", Color.black);
    if(color != null){
      currentColorButton.setBackground(color);
      String element = (String)elementList.getSelectedValue();
      currentDS.setColor(element.toLowerCase(), color);
      // kludge
      currentDS.setName("Current");
      updatePreviewPanel();
    }
  }

  /**
   * Saves a new DisplayScheme to file
   * @param e the ActionEvent
   */
  void saveButton_actionPerformed(ActionEvent e) {
    // Prompt for Scheme Name
    String input = "";
    while(input.equals("")){
      input = JOptionPane.showInputDialog(this, "Enter the Name of the Display Scheme",
                                "Display Scheme Name", JOptionPane.PLAIN_MESSAGE);
    }

    if(input == null)
      return;

    // set Name
    currentDS.setName(input);

    // get font Info
    boolean isdefault = defaultFontCheckBox.isSelected();
    if(!isdefault){
      String fontName = (String)fontComboBox.getSelectedItem();
      currentDS.setFont(fontName);
    }

    TreeMap<String, DisplayScheme> dispSchemes = dsp.getDisplaySchemes();

    dispSchemes.put(input, currentDS);

    try {
      dsp.store();
      boolean found = false;
      // add if not already in combobox
      for (int i=0; i < schemeComboBox.getItemCount(); i++) {
        String item = (String)schemeComboBox.getItemAt(i);
        if(item.equals(input)){
          found = true;
          break;
        }
      }

      if(!found)
        schemeComboBox.addItem(input);
      schemeComboBox.setSelectedItem(input);
    }
    catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "There was an error saving the Display Scheme",
                                    "Error Saving Scheme", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Updates the preview panel and the font panel with the DisplayScheme
   * chosen
   * @param e the ActionEvent
   */
  void schemeComboBox_actionPerformed(ActionEvent e) {
    JComboBox schemeCB = (JComboBox)e.getSource();
    String schemeName = (String)schemeCB.getSelectedItem();
    DisplayScheme ds = null;

    TreeMap<String, DisplayScheme> schemes = dsp.getDisplaySchemes();
    for (Iterator<String> i = schemes.keySet().iterator(); i.hasNext(); ) {
      Object item = i.next();
      ds = (DisplayScheme)schemes.get(item);
      if(ds.getName().equals(schemeName)){
        currentDS = ds.copy();
        break;
      }
    }
    elementList.getSelectionModel().setSelectionInterval(0, 0);

    // Update Selected Font
    String fontName = currentDS.getFont();
    fontComboBox.setSelectedItem(fontName);

    // kludge
    currentDS.setName(ds.getName());

    updatePreviewPanel();
  }

  /**
   * Updates the preview panel with the values chosen in the dialog
   */
  void updatePreviewPanel(){
    if(previewPanel != null){
      previewPanel.setTitleColor(currentDS.getColor("title"));
      previewPanel.setPlotColor(currentDS.getColor("plot"));
      previewPanel.setScaleColor(currentDS.getColor("scale"));
      previewPanel.setcoordinatesColor(currentDS.getColor("coordinates"));
      previewPanel.setUnitsColor(currentDS.getColor("units"));
      previewPanel.setPlotAreaColor(currentDS.getColor("plotarea"));
      previewPanel.setBackground(currentDS.getColor("background"));
      previewPanel.setGridColor(currentDS.getColor("grid"));
      previewPanel.setDisplayFontName(currentDS.getFont());
      repaint();
    }
  }

  /**
   * Changes the font of the current DisplayScheme
   * @param e the ActionEvent
   */
  void fontComboBox_actionPerformed(ActionEvent e) {
    String fontName = (String)((JComboBox)e.getSource()).getSelectedItem();
    currentDS.setFont(fontName);
    // kludge
    currentDS.setName("Current");
    updatePreviewPanel();
  }

  /**
   * Sets the font of the current DisplayScheme to the the system default
   * @param e ActionEvent
   */
  void defaultFontCheckBox_actionPerformed(ActionEvent e) {
    JCheckBox cb = (JCheckBox)e.getSource();
    if(cb.isSelected()){
      fontComboBox.setSelectedItem("Default");
      fontComboBox.setEnabled(false);
      currentDS.setFont("Default");
      // kludge
      currentDS.setName("Current");
      updatePreviewPanel();
    }else{
      fontComboBox.setEnabled(true);
    }
  }

  /**
   * Sets the current DisplayScheme to the default
   * @param e the ActionEvent
   */
  void defaultColorCheckBox_actionPerformed(ActionEvent e) {
    JCheckBox cb = (JCheckBox)e.getSource();
    if(cb.isSelected()){
      schemeComboBox.setSelectedItem("Default");
      schemeComboBox.setEnabled(false);
      customButton.setEnabled(false);
      saveButton.setEnabled(false);

      colorButton1.setEnabled(false);
      colorButton2.setEnabled(false);
      colorButton3.setEnabled(false);
      colorButton4.setEnabled(false);
      colorButton5.setEnabled(false);
      colorButton6.setEnabled(false);
      colorButton7.setEnabled(false);
      colorButton8.setEnabled(false);

      updatePreviewPanel();
    }else{
      schemeComboBox.setEnabled(true);
      colorPanel.setEnabled(true);
      customButton.setEnabled(true);
      saveButton.setEnabled(true);

      colorButton1.setEnabled(true);
      colorButton2.setEnabled(true);
      colorButton3.setEnabled(true);
      colorButton4.setEnabled(true);
      colorButton5.setEnabled(true);
      colorButton6.setEnabled(true);
      colorButton7.setEnabled(true);
      colorButton8.setEnabled(true);
    }
  }


  /* ---------------------------------------------------------------------*/

  /**
   * Retruns the current Display Scheme
   * @return the current Display Scheme
   */
  public DisplayScheme getSelectedDisplayScheme(){
    return currentDS;
  }

  /**
   * Clears the list of recently opened files
   * @param e the ActionEvent
   */
  void clearRecentButton_actionPerformed(ActionEvent e) {
    int option = JOptionPane.showConfirmDialog(this, "Recent File Paths will be cleared!",
                                  "Warning", JOptionPane.OK_CANCEL_OPTION,
                                  JOptionPane.WARNING_MESSAGE);
    if(option == JOptionPane.OK_OPTION)
      preferences.setProperty("recentFilePaths", "");
  }

  /**
   * Shows Color dialog to set the color of the integral plot
   * @param e ActionEvent
   */
  void processingCustomButton_actionPerformed(ActionEvent e) {
    Color color = JColorChooser.showDialog(this, "Choose Color", Color.black);
    if(color != null)
      plotColorButton.setBackground(color);
  }

  /**
   * Listener for the Panel of Color button of the processing tab
   */
  class ProcColorPanelActionListener implements ActionListener{

    /**
     * Sets the color plotColorButton to the color of the button pressed
     * @param ae the ActionEvent
     */
    public void actionPerformed(ActionEvent ae){
      JButton button = (JButton)ae.getSource();
      Color color = button.getBackground();
      plotColorButton.setBackground(color);
    }
  }


}
