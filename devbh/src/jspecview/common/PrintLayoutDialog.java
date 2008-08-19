/* Copyright (c) 2002-2007 The University of the West Indies
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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

//import javax.print.PrintService;
import javax.print.attribute.standard.MediaSizeName;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
//import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

/**
 * Dialog to set print preferences for JSpecview.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class PrintLayoutDialog extends JDialog {
  private TitledBorder titledBorder1;
  private TitledBorder titledBorder2;
  private TitledBorder titledBorder3;
  private TitledBorder titledBorder4;
  private TitledBorder titledBorder5;

  private ImageIcon portraitIcon = new ImageIcon(
    PrintLayoutDialog.class.getClassLoader().getResource("icons/portrait.gif"));
  private ImageIcon landscapeIcon = new ImageIcon(
    PrintLayoutDialog.class.getClassLoader().getResource("icons/landscape.gif"));
  private ImageIcon previewPortraitCenterIcon = new ImageIcon(
    PrintLayoutDialog.class.getClassLoader().getResource("icons/portraitCenter.gif"));
  private ImageIcon previewPortraitDefaultIcon = new ImageIcon(
    PrintLayoutDialog.class.getClassLoader().getResource("icons/portraitDefault.gif"));
  private ImageIcon previewPortraitFitIcon = new ImageIcon(
    PrintLayoutDialog.class.getClassLoader().getResource("icons/portraitFit.gif"));
  private ImageIcon previewLandscapeCenterIcon = new ImageIcon(
    PrintLayoutDialog.class.getClassLoader().getResource("icons/landscapeCenter.gif"));
  private ImageIcon previewLandscapeDefaultIcon = new ImageIcon(
    PrintLayoutDialog.class.getClassLoader().getResource("icons/landscapeDefault.gif"));
  private ImageIcon previewLandscapeFitIcon = new ImageIcon(
    PrintLayoutDialog.class.getClassLoader().getResource("icons/landscapeFit.gif"));


  private ButtonGroup layoutButtonGroup = new ButtonGroup();
  private ButtonGroup fontButtonGroup = new ButtonGroup();
  private ButtonGroup positionButtonGroup = new ButtonGroup();

  private PrintLayout pl;
  private JPanel jPanel1 = new JPanel();
  private JButton cancelButton = new JButton();
  private JButton printButton = new JButton();
  private TitledBorder titledBorder6;
  private TitledBorder titledBorder7;
  private TitledBorder titledBorder8;

  private JPanel layoutPanel = new JPanel();
  private GridBagLayout gridBagLayout6 = new GridBagLayout();
  private GridBagLayout gridBagLayout5 = new GridBagLayout();
  private GridBagLayout gridBagLayout4 = new GridBagLayout();
  private GridBagLayout gridBagLayout3 = new GridBagLayout();
  private JRadioButton landscapeRadioButton = new JRadioButton();
  private GridBagLayout gridBagLayout2 = new GridBagLayout();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private JPanel positionPanel = new JPanel();
  private JPanel layoutContentPanel = new JPanel();
  private JCheckBox scaleCheckBox = new JCheckBox();
  private JPanel previewPanel = new JPanel();
  private JButton previewButton = new JButton();
  private JCheckBox gridCheckBox = new JCheckBox();
  private JRadioButton defaultPosRadioButton = new JRadioButton();
  private JRadioButton centerRadioButton = new JRadioButton();
  private JRadioButton portraitRadioButton = new JRadioButton();
  private JComboBox fontComboBox = new JComboBox();
  private JRadioButton fitToPageRadioButton = new JRadioButton();
  private JButton layoutButton = new JButton();
  private JRadioButton chooseFontRadioButton = new JRadioButton();
  private JRadioButton defaultFontRadioButton = new JRadioButton();
  private JPanel fontPanel = new JPanel();
  private JCheckBox titleCheckBox = new JCheckBox();
  private JPanel elementsPanel = new JPanel();
  private JPanel jPanel2 = new JPanel();
  private TitledBorder titledBorder9;
  private GridBagLayout gridBagLayout7 = new GridBagLayout();
  private JComboBox paperComboBox = new JComboBox();
  //private JComboBox printerNameComboBox = new JComboBox();

  /**
   * Initialises a <code>PrintLayoutDialog</code>
   * @param frame the parent frame
   * @param title the title
   * @param modal the modality
   */
  public PrintLayoutDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);

    // Intialise Paper Names
    paperComboBox.addItem(MediaSizeName.NA_LETTER);
    paperComboBox.addItem(MediaSizeName.NA_LEGAL);
    paperComboBox.addItem(MediaSizeName.ISO_A4);
    paperComboBox.addItem(MediaSizeName.ISO_B4);

    try {
      jbInit();
      setSize(320, 400);
      setResizable(false);

      // load names of fonts in fontComboBox
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      String allFontNames[] = ge.getAvailableFontFamilyNames();
      for(int i = 0; i < allFontNames.length; i++){
        fontComboBox.addItem(allFontNames[i]);
      }

      setVisible(true);
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Initialises a modal <code>PrintLayoutDialog</code> with a default title
   * of "Print Layout".
   * @param frame the parent frame
   */
  public PrintLayoutDialog(Frame frame) {
    this(frame, "Print Layout", true);
  }

  /**
   * Initalises the GUI components
   * @throws Exception
   */
  private void jbInit() throws Exception {
    titledBorder1 = new TitledBorder("");
    titledBorder2 = new TitledBorder("");
    titledBorder3 = new TitledBorder("");
    titledBorder4 = new TitledBorder("");
    titledBorder5 = new TitledBorder("");
    titledBorder6 = new TitledBorder("");
    titledBorder7 = new TitledBorder("");
    titledBorder8 = new TitledBorder("");
    titledBorder9 = new TitledBorder("");
    titledBorder1.setTitle("Layout");
    titledBorder1.setTitleJustification(2);
    titledBorder2.setTitle("Position");
    titledBorder2.setTitleJustification(2);
    titledBorder3.setTitle("Elements");
    titledBorder3.setTitleJustification(2);
    titledBorder4.setTitle("Font");
    titledBorder4.setTitleJustification(2);
    titledBorder5.setTitle("Preview");
    titledBorder5.setTitleJustification(2);
    cancelButton.setText("Cancel");
    cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelButton_actionPerformed(e);
      }
    });
    printButton.setToolTipText("");
    printButton.setText("Print");
    printButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        printButton_actionPerformed(e);
      }
    });
    titledBorder6.setTitle("Printers");
    titledBorder7.setTitle("Paper");
    titledBorder8.setTitle("Copies");
    layoutPanel.setBorder(titledBorder1);
    layoutPanel.setLayout(gridBagLayout2);
    landscapeRadioButton.setActionCommand("Landscape");
    landscapeRadioButton.setSelected(true);
    landscapeRadioButton.setText("Landscape");
    landscapeRadioButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        landscapeRadioButton_actionPerformed(e);
      }
    });
    positionPanel.setBorder(titledBorder2);
    positionPanel.setLayout(gridBagLayout3);
    layoutContentPanel.setLayout(gridBagLayout1);
    scaleCheckBox.setSelected(true);
    scaleCheckBox.setText("Scale");
    previewPanel.setBorder(titledBorder5);
    previewPanel.setLayout(gridBagLayout6);
    previewButton.setBorder(null);
    previewButton.setIcon(previewLandscapeDefaultIcon);
    gridCheckBox.setSelected(true);
    gridCheckBox.setText("Grid");
    defaultPosRadioButton.setActionCommand("Default");
    defaultPosRadioButton.setSelected(true);
    defaultPosRadioButton.setText("Default");
    defaultPosRadioButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        defaultPosRadioButton_actionPerformed(e);
      }
    });
    centerRadioButton.setActionCommand("Center");
    centerRadioButton.setText("Center");
    centerRadioButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        centerRadioButton_actionPerformed(e);
      }
    });
    portraitRadioButton.setActionCommand("Portrait");
    portraitRadioButton.setText("Portrait");
    portraitRadioButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        portraitRadioButton_actionPerformed(e);
      }
    });
    fitToPageRadioButton.setActionCommand("Fit To Page");
    fitToPageRadioButton.setText("Fit to Page");
    fitToPageRadioButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        fitToPageRadioButton_actionPerformed(e);
      }
    });
    layoutButton.setBorder(null);
    layoutButton.setIcon(portraitIcon);
    chooseFontRadioButton.setText("Choose font");
    defaultFontRadioButton.setSelected(true);
    defaultFontRadioButton.setText("Use default");
    fontPanel.setBorder(titledBorder4);
    fontPanel.setLayout(gridBagLayout5);
    titleCheckBox.setSelected(true);
    titleCheckBox.setText("Title");
    elementsPanel.setBorder(titledBorder3);
    elementsPanel.setLayout(gridBagLayout4);
    jPanel2.setBorder(titledBorder9);
    jPanel2.setLayout(gridBagLayout7);
    titledBorder9.setTitle("Paper");
    titledBorder9.setTitleJustification(2);
    this.getContentPane().add(jPanel1,  BorderLayout.SOUTH);
    jPanel1.add(printButton, null);
    jPanel1.add(cancelButton, null);
    this.getContentPane().add(layoutContentPanel,  BorderLayout.CENTER);
    layoutContentPanel.add(previewPanel,   new GridBagConstraints(0, 2, 2, 1, 0.0, 1.0
            ,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    previewPanel.add(previewButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    layoutContentPanel.add(layoutPanel,  new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    layoutPanel.add(portraitRadioButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutPanel.add(landscapeRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutPanel.add(layoutButton, new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutContentPanel.add(positionPanel,  new GridBagConstraints(1, 0, 2, 1, 0.5, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    positionPanel.add(centerRadioButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    positionPanel.add(fitToPageRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    positionPanel.add(defaultPosRadioButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutContentPanel.add(elementsPanel,            new GridBagConstraints(0, 1, 1, 1, 0.5, 1.0
            ,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    elementsPanel.add(gridCheckBox, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    elementsPanel.add(scaleCheckBox, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    elementsPanel.add(titleCheckBox, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutContentPanel.add(fontPanel,          new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    fontPanel.add(defaultFontRadioButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    fontPanel.add(chooseFontRadioButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    fontPanel.add(fontComboBox, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    layoutContentPanel.add(jPanel2,  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    jPanel2.add(paperComboBox,      new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
    layoutButtonGroup.add(portraitRadioButton);
    layoutButtonGroup.add(landscapeRadioButton);
    positionButtonGroup.add(centerRadioButton);
    positionButtonGroup.add(fitToPageRadioButton);
    positionButtonGroup.add(defaultPosRadioButton);
    fontButtonGroup.add(defaultFontRadioButton);
    fontButtonGroup.add(chooseFontRadioButton);
  }

  /**
   * Sets the layout to portrait and changes the preview icon according to the
   * position selected
   * @param e the ActionEvent
   */
  void portraitRadioButton_actionPerformed(ActionEvent e) {
    layoutButton.setIcon(portraitIcon);
    String comm = positionButtonGroup.getSelection().getActionCommand();
    if(comm.equals("Default"))
      previewButton.setIcon(previewPortraitDefaultIcon);
    else if(comm.equals("Fit To Page"))
      previewButton.setIcon(previewPortraitFitIcon);
    else if(comm.equals("Center"))
      previewButton.setIcon(previewPortraitCenterIcon);
  }

  /**
   * Sets the layout to landscape and changes the preview icon according to the
   * position selected
   * @param e the ActionEvent
   */
  void landscapeRadioButton_actionPerformed(ActionEvent e) {
    layoutButton.setIcon(landscapeIcon);
    String comm = positionButtonGroup.getSelection().getActionCommand();
    if(comm.equals("Default"))
      previewButton.setIcon(previewLandscapeDefaultIcon);
    else if(comm.equals("Fit To Page"))
      previewButton.setIcon(previewLandscapeFitIcon);
    else if(comm.equals("Center"))
      previewButton.setIcon(previewLandscapeCenterIcon);
  }

  /**
   * Sets the positon to center and changes the preview icon according to the
   * layout selected
   * @param e the ActionEvent
   */
  void centerRadioButton_actionPerformed(ActionEvent e) {
    String comm = layoutButtonGroup.getSelection().getActionCommand();
    if(comm.equals("Portrait"))
      previewButton.setIcon(previewPortraitCenterIcon);
    else if(comm.equals("Landscape"))
      previewButton.setIcon(previewLandscapeCenterIcon);
  }

  /**
   * Sets the positon to "fit to page" and changes the preview icon according to the
   * layout selected
   * @param e the ActionEvent
   */
  void fitToPageRadioButton_actionPerformed(ActionEvent e) {
    String comm = layoutButtonGroup.getSelection().getActionCommand();
    if(comm.equals("Portrait"))
      previewButton.setIcon(previewPortraitFitIcon);
    else if(comm.equals("Landscape"))
      previewButton.setIcon(previewLandscapeFitIcon);
  }

  /**
   * Sets the positon to default and changes the preview icon according to the
   * layout selected
   * @param e the ActionEvent
   */
  void defaultPosRadioButton_actionPerformed(ActionEvent e) {
    String comm = layoutButtonGroup.getSelection().getActionCommand();
    if(comm.equals("Portrait"))
      previewButton.setIcon(previewPortraitDefaultIcon);
    else if(comm.equals("Landscape"))
      previewButton.setIcon(previewLandscapeDefaultIcon);
  }

  /**
   * Stored all the layout Information the PrintLayout object and disposes the
   * dialog
   * @param e ActionEvent
   */
  void printButton_actionPerformed(ActionEvent e) {
    pl = new PrintLayout();
    pl.layout = layoutButtonGroup.getSelection().getActionCommand().toLowerCase();
    if(defaultFontRadioButton.isSelected())
      pl.font = null;
    else
      pl.font = (String)fontComboBox.getSelectedItem();
    pl.position = positionButtonGroup.getSelection().getActionCommand().toLowerCase();
    pl.showGrid = gridCheckBox.isSelected();
    pl.showScale = scaleCheckBox.isSelected();
    pl.showTitle = titleCheckBox.isSelected();
    pl.paper = (MediaSizeName)paperComboBox.getSelectedItem();
    //pl.printer = services[printerNameComboBox.getSelectedIndex()];
    //pl.numCopies = ((Integer)numCopiesSpinner.getValue()).intValue();

    dispose();
  }

  /**
   * Returns the PrintLayout object
   * @return the PrintLayout object
   */
  public PrintLayout getPrintLayout(){
    return pl;
  }

  /**
   * <code>PrintLayout</code> class stores all the information needed from the
   * <code>PrintLayoutDialog</code>
   */
  public class PrintLayout{

    /**
     * The paper orientation ("portrait" or "landscape")
     */
    public String layout;

    /**
     * The position of the graph on the paper
     * ("center", "default", "fit to page")
     */
    public String position;

    /**
     * whether or not the grid should be printed
     */
    public boolean showGrid;

    /**
     * whether or not the scale should be printed
     */
    public boolean showScale;

    /**
     * whether or not the title should be printed
     */
    public boolean showTitle;

    /**
     * The font of the elements
     */
    public String font;

    /**
     * The size of the paper to be printed on
     */
    public MediaSizeName paper;

    public PrintLayout(){ }
  }

  /**
   * set the <code>PrintLayout</code> object to null and disposes of the dialog
   * @param e the action (event)
   */
  void cancelButton_actionPerformed(ActionEvent e) {
    pl = null;
    dispose();
  }
}
